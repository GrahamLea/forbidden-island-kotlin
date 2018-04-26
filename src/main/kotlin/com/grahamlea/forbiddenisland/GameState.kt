package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.FloodLevel.DEAD
import com.grahamlea.forbiddenisland.Location.FoolsLanding
import com.grahamlea.forbiddenisland.LocationFloodState.Sunken

data class GameState(
        val gameSetup: GameSetup,
        val floodLevel: FloodLevel,
        private val treasureDeck: ImmutableList<HoldableCard>,
        val treasureDeckDiscard: ImmutableList<HoldableCard>,
        private val floodDeck: ImmutableList<Location>,
        val floodDeckDiscard: ImmutableList<Location>,
        val treasuresCollected: ImmutableMap<Treasure, Boolean>,
        val locationFloodStates: ImmutableMap<Location, LocationFloodState>,
        val playerPositions: ImmutableMap<Adventurer, MapSite>,
        val playerCards: ImmutableMap<Adventurer, ImmutableList<HoldableCard>>,
        val phase: GamePhase,
        val previousEvents: ImmutableList<GameEvent> = immutableListOf()
    ) {

    init {
        // Cards in treasure decks and hands must make a full deck
        require((treasureDeck + treasureDeckDiscard + playerCards.values.flatten()).groupingBy { it }.eachCount() == TreasureDeck.totalCardCounts) {
            listOf("There must be a full Treasure Deck between the treasureDeck, the treasureDeckDiscard and playerCards.",
                    "Expected: ${TreasureDeck.totalCardCounts.toSortedMap()}",
                    "But was:  ${(treasureDeck + treasureDeckDiscard + playerCards.values.flatten()).groupingBy { it }.eachCount().toSortedMap()}",
                    "treasureDeck: $treasureDeck",
                    "treasureDeckDiscard: $treasureDeckDiscard",
                    "playerCards: $playerCards"
            ).joinToString(separator = "\n")
        }

        // Cards in flood decks must make a full deck
        require(Location.values().all { (it in floodDeck) xor (it in floodDeckDiscard ) }) {
            // TODO: Once locations start being sunk, we remove them from the game entirely
            "Every location must appear exactly once in either the floodDeck or the floodDeckDiscard"
        }

        // All treasures must have a collected flag
        require(treasuresCollected.size == 4) {
            "treasuresCollected must contain a value for all treasures, but only contains ${treasuresCollected.keys}"
        }

        // Location flood states must contain all locations
        require(locationFloodStates.keys == Location.allLocationsSet) {
            "A FloodState must be provided for each Location. Missing: ${Location.allLocationsSet - locationFloodStates.keys}"
        }

        // Players with positions must match those in the game setup
        require(playerPositions.keys == gameSetup.players.toSet()) {
            "Adventurers with positions ${playerPositions.keys} must match the players in the game ${gameSetup.players.toSet()}"
        }

        // MapSites of playerPositions must match GameSetup
        require(gameSetup.map.mapSites.containsAll(playerPositions.values)) {
            "All MapSites of playerPositions must match a MapSite in the gameSetup.map"
        }

        // Players with cards must match those in the game setup
        require(playerCards.keys == gameSetup.players.toSet()) {
            "Adventurers with cards ${playerCards.keys} must match the players in the game ${gameSetup.players.toSet()}"
        }
    }

    val result: GameResult? by lazy {
        val lostTreasures = unreachableTreasures() intersect uncollectedTreasures()
        val drownedPlayers = drownedPlayers()
        when {
            floodLevel == DEAD -> MaximumWaterLevelReached
            drownedPlayers.any() -> PlayerDrowned(drownedPlayers.first())
            lostTreasures.any() -> BothPickupLocationsSankBeforeCollectingTreasure(lostTreasures.first())
            locationFloodStates[FoolsLanding] == Sunken -> FoolsLandingSank
            treasuresCollected.all { it.value } &&
                    playerPositions.values.all { it.location == FoolsLanding } &&
                    treasureDeckDiscard.lastOrNull() == HelicopterLiftCard
            -> AdventurersWon
            else -> null
        }
    }

    fun after(event: GameEvent): GameState {
        // TODO Check that event is in list of possible events
        return this.copy(previousEvents = previousEvents + event).let {
            when (event) {
                is Move -> it.copy(playerPositions = playerPositions + (event.player to event.mapSite))
                else -> throw IllegalArgumentException("Event type ${event::class} isn't currently handled")
            }
        }
    }

    private fun uncollectedTreasures(): Set<Treasure> = treasuresCollected.filterValues { !it }.keys

    private fun drownedPlayers() =
        playerPositions
            .filterKeys { it != Diver && it != Pilot }
            .filter { locationFloodStates.getValue(it.value.location) == Sunken }
            .filter { gameSetup.map.adjacentSites(it.value.position, includeDiagonals = (it.key == Explorer))
            .all { locationFloodStates.getValue(it.location) == Sunken } }
            .keys

    private fun unreachableTreasures(): Set<Treasure> =
        locationFloodStates
            .filterKeys { it.pickupLocationForTreasure != null }
            .toList()
            .groupBy { it.first.pickupLocationForTreasure!! }
            .filterValues { it.all { it.second == Sunken } }
            .keys

}
