package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.FloodLevel.DEAD
import com.grahamlea.forbiddenisland.Location.FoolsLanding
import com.grahamlea.forbiddenisland.LocationFloodState.Sunken
import com.grahamlea.forbiddenisland.LocationFloodState.Unflooded
import java.util.*

data class GameState(
        val gameSetup: GameSetup,
        val floodLevel: FloodLevel,
        private val treasureDeck: ImmutableList<HoldableCard>,
        val treasureDeckDiscard: ImmutableList<HoldableCard>,
        private val floodDeck: ImmutableList<Location>,
        val floodDeckDiscard: ImmutableList<Location>,
        val treasuresCollected: ImmutableMap<Treasure, Boolean>,
        val locationFloodStates: ImmutableMap<Location, LocationFloodState>,
        val playerPositions: ImmutableMap<Adventurer, Position>,
        val playerCards: ImmutableMap<Adventurer, ImmutableList<HoldableCard>>,
        val phase: GamePhase,
        val previousEvents: ImmutableList<GameEvent> = immListOf()
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

        // Cards in flood decks must make a full deck excluding sunken locations
        require((Location.values().subtract(locationsWithState(Sunken)))
                    .all { (it in floodDeck) xor (it in floodDeckDiscard ) }) {
            "Every location that has not sunk must appear exactly once in either the floodDeck or the floodDeckDiscard"
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
            previousEvents.lastOrNull() is HelicopterLiftOffIsland -> AdventurersWon
            else -> null
        }
    }

    private fun uncollectedTreasures(): Set<Treasure> = treasuresCollected.filterValues { !it }.keys

    private fun unreachableTreasures(): Set<Treasure> =
            locationFloodStates
                    .filterKeys { it.pickupLocationForTreasure != null }
                    .toList()
                    .groupBy { it.first.pickupLocationForTreasure!! }
                    .filterValues { it.all { it.second == Sunken } }
                    .keys

    private fun drownedPlayers() =
            playerPositions
                    .filterKeys { it != Diver && it != Pilot }
                    .filter { locationFloodStates.getValue(gameSetup.map.locationAt(it.value)) == Sunken }
                    .filter { gameSetup.map.adjacentSites(it.value, includeDiagonals = (it.key == Explorer))
                            .all { locationFloodStates.getValue(it.location) == Sunken } }
                    .keys

    val availableActions: List<GameEvent> by lazy {
        when (phase) {
            is AwaitingPlayerAction -> possibleMoveAndFlyActions(phase.player)
            else -> emptyList()
        }
    }

    private fun possibleMoveAndFlyActions(player: Adventurer): List<GameEvent> {
        val playerPosition = playerPositions.getValue(player)
        val playerSite = gameSetup.map.mapSiteAt(playerPosition)
        val moves = accessiblePositionsAdjacentTo(playerPosition, includeDiagonals = player == Explorer).map { Move(player, it) }
        val flights = if (player == Pilot && !previousEvents.takeLastWhile { it !is DrawFromFloodDeck }.any { it is Fly }) {
            (gameSetup.map.mapSites.map(MapSite::position) - moves.map(Move::position) - playerSite.position).map { Fly(player, it) }
        } else emptyList()
        val navigatorAssists = if (player == Navigator) {
            gameSetup.players.filterNot { it == Navigator }
                .associate { otherPlayer -> otherPlayer to positionsNavigatorCanSendPlayerFrom(playerPositions.getValue(otherPlayer), otherPlayer) }
                .flatMap { (otherPlayer, positions) -> positions.map { Move(otherPlayer, it) } }
        } else emptyList()
        val diverSwims = if (player == Diver) {
            diverSwimPositionsFrom(playerPosition).map { Move(Diver, it) }
        } else emptyList()
        return moves + flights + navigatorAssists + diverSwims
    }

    private fun accessiblePositionsAdjacentTo(p: Position, includeDiagonals: Boolean = false, includeSunkenTiles: Boolean = false): List<Position> =
        gameSetup.map.adjacentSites(p, includeDiagonals)
            .filterNot { locationFloodStates.getValue(it.location) == Sunken && !includeSunkenTiles }
            .map { it.position }

    private fun positionsNavigatorCanSendPlayerFrom(p: Position, player: Adventurer): List<Position> =
        accessiblePositionsAdjacentTo(p, includeDiagonals = player == Explorer, includeSunkenTiles = player == Diver).flatMap {
            accessiblePositionsAdjacentTo(it, includeDiagonals = player == Explorer).map { it } + it
        }.distinct() - p

    private fun diverSwimPositionsFrom(playersCurrentPosition: Position): List<Position> {
        tailrec fun positions(startingPoints: List<Position>, reachable: MutableList<Position>): List<Position> {
            val neighbours = startingPoints.flatMap { accessiblePositionsAdjacentTo(it, includeSunkenTiles = true) }
            val floodedAndSunkenNeighbours = neighbours.filterNot { locationFloodStates.getValue(gameSetup.map.locationAt(it)) == Unflooded }
            val newStartingPoints = floodedAndSunkenNeighbours - reachable
//            println("startingPoints = ${startingPoints}")
//            println("neighbours = ${neighbours}")
//            println("floodedAndSunkenNeighbours = ${floodedAndSunkenNeighbours}")
//            println("reachable = ${reachable}")
//            println("newStartingPoints = ${newStartingPoints}")
//            println("------------------------------------------------------------------------------------------------")
            reachable += neighbours
            return if (newStartingPoints.any()) positions(newStartingPoints, reachable) else reachable
        }

        return positions(listOf(playersCurrentPosition), mutableListOf()).filterNot {
            locationFloodStates.getValue(gameSetup.map.locationAt(it)) == Sunken
        } - playersCurrentPosition
    }

    fun after(event: GameEvent, random: Random): GameState {
        // TODO Check that event is in list of possible events
        return when (event) {
                is Move -> copy(playerPositions = playerPositions + (event.player to event.position))
                is Fly -> copy(playerPositions = playerPositions + (event.player to event.position))
                is SwimToSafety -> copy(playerPositions = playerPositions + (event.strandedPlayer to event.position))
                is ShoreUp -> copy(locationFloodStates = locationFloodStates + (gameSetup.map.locationAt(event.position) to Unflooded))
                is GiveTreasureCard -> copy(playerCards =
                    playerCards + (event.player   to playerCards.getValue(event.player)  .subtract(listOf(event.card))) +
                                  (event.receiver to playerCards.getValue(event.receiver).plus(    listOf(event.card)))
                )
                is CaptureTreasure -> event.player.discards(TreasureCard(event.treasure) * 4).copy(
                    treasuresCollected = treasuresCollected + (event.treasure to true)
                )
                is HelicopterLift -> (event.playerWithCard discards HelicopterLiftCard).copy(
                    playerPositions = playerPositions + (event.playerBeingMoved to event.position)
                )
                is Sandbag -> (event.player discards SandbagsCard).copy(
                    locationFloodStates = locationFloodStates + (gameSetup.map.locationAt(event.position) to Unflooded)
                )
                is DrawFromTreasureDeck -> treasureDeck.first().let { drawnCard ->
                    if (drawnCard == WatersRiseCard) {
                        copy(
                            floodLevel = floodLevel.next(),
                            treasureDeck = treasureDeck.drop(1).imm(),
                            treasureDeckDiscard = treasureDeckDiscard + WatersRiseCard,
                            floodDeck = (floodDeckDiscard.shuffled() + floodDeck).imm(),
                            floodDeckDiscard = immListOf()
                        )
                    } else {
                        copy(
                            playerCards = playerCards + (event.player to playerCards.getValue(event.player).plus(drawnCard)),
                            treasureDeck = treasureDeck.drop(1).imm()
                        )
                    }.let { if (it.treasureDeck.isEmpty()) it.copy(treasureDeck = it.treasureDeckDiscard.shuffled(random).imm(), treasureDeckDiscard = cards()) else it }
                }
                is DrawFromFloodDeck -> floodDeck.first().let { floodedLocation ->
                    locationFloodStates.getValue(floodedLocation).flooded().let { newFloodState ->
                        copy(
                            floodDeck = floodDeck.drop(1).imm(),
                            locationFloodStates = locationFloodStates + (floodedLocation to newFloodState),
                            floodDeckDiscard = if (newFloodState == Sunken) floodDeckDiscard else floodDeckDiscard + floodedLocation
                        )
                    }.let { if (it.floodDeck.isEmpty()) it.copy(floodDeck = it.floodDeckDiscard.shuffled().imm(), floodDeckDiscard = immListOf()) else it }
                }
                is DiscardCard -> (event.player discards event.card)
                is HelicopterLiftOffIsland -> (event.player discards HelicopterLiftCard)
            }.let { newState -> newState.copy(
                phase = phase.phaseAfter(event, newState),
                previousEvents = previousEvents + event
            )}
    }

    private infix fun Adventurer.discards(card: HoldableCard) = this.discards(immListOf(card))

    private fun Adventurer.discards(cardList: Collection<HoldableCard>) =
            copy(
                    playerCards = playerCards + (this to playerCards.getValue(this).subtract(cardList)),
                    treasureDeckDiscard = treasureDeckDiscard + cardList
            )

    val sunkPlayers: Set<Adventurer> by lazy { playerPositions.filter { locationFloodStates.getValue(gameSetup.map.locationAt(it.value)) == Sunken }.keys }

    fun locationsWithState(state: LocationFloodState) = locationFloodStates.filterValues { it == state }.keys

    val playerCardCounts: Map<Adventurer, Int> = playerCards.mapValues { it.value.size }
}
