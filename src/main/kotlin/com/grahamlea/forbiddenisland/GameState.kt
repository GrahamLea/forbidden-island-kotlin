package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.FloodLevel.DEAD
import com.grahamlea.forbiddenisland.Location.FoolsLanding
import com.grahamlea.forbiddenisland.LocationFloodState.*
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
        val previousActions: ImmutableList<GameAction> = immListOf()
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
            isSunken(FoolsLanding) -> FoolsLandingSank
            previousActions.lastOrNull() is HelicopterLiftOffIsland -> AdventurersWon
            else -> null
        }
    }

    private fun uncollectedTreasures(): Set<Treasure> = treasuresCollected.filterValues { !it }.keys

    private fun unreachableTreasures(): Set<Treasure> =
            Location.values()
                .filter { it.pickupLocationForTreasure != null }
                .groupBy { it.pickupLocationForTreasure!! }
                .filterValues { it.all(::isSunken) }
                .keys

    private fun drownedPlayers() =
            playerPositions
                    .filterKeys { it != Diver && it != Pilot }
                    .filterValues(::isSunken)
                    .filter { (player, position) ->
                        gameSetup.map.adjacentSites(position, includeDiagonals = (player == Explorer)).all(::isSunken)
                    }.keys

    val availableActions: List<GameAction> by lazy {
        when (phase) {
            is AwaitingPlayerAction ->
                positionOf(phase.player).let { playerPosition ->
                    availableMoveAndFlyActions(phase.player, playerPosition) +
                        availableShoreUpActions(phase.player, playerPosition) +
                        availableGiveTreasureCardActions(phase.player, playerPosition) +
                        availableCaptureTreasureActions(phase.player, playerPosition) +
                        DrawFromTreasureDeck(phase.player)
                }
            is AwaitingTreasureDeckDraw -> listOf(DrawFromTreasureDeck(phase.player))
            is AwaitingFloodDeckDraw -> listOf(DrawFromFloodDeck(phase.player))
            is AwaitingPlayerToDiscardExtraCard ->
                playerCards.getValue(phase.playerWithTooManyCards).distinct().map { DiscardCard(phase.playerWithTooManyCards, it) }
            is AwaitingPlayerToSwimToSafety -> availableSwimToSafetyActions(phase.player)
            is GameOver -> emptyList()
        } + ((allHelicopterLiftActions() + allSandbagActions() + helicopterLiftOffIslandIfAvailable()).let { actions ->
            when (phase) {
                is AwaitingPlayerToSwimToSafety, is GameOver -> emptyList()
                is AwaitingPlayerToDiscardExtraCard -> actions.filter { (it as CardDiscardingAction).playerDiscardingCard == phase.playerWithTooManyCards }
                else -> actions
        }})
    }

    private fun availableMoveAndFlyActions(player: Adventurer, playerPosition: Position): List<GameAction> {
        val moves = accessiblePositionsAdjacentTo(playerPosition, includeDiagonals = player == Explorer).toMovesFor(player)
        return moves + when (player) {
            Diver -> diverSwimPositionsFrom(gameSetup.mapSiteAt(playerPosition)).toMovesFor(Diver)
            Navigator -> (gameSetup.players - Navigator).flatMap { sitesNavigatorCanSend(it).toMovesFor(it) }
            Pilot -> if (!pilotHasAlreadyUsedAFlightThisTurn()) availablePilotFlights(moves, playerPosition, player) else emptyList()
            else -> emptyList<GameAction>()
        }
    }

    private fun Iterable<MapSite>.toMovesFor(player: Adventurer) = this.map { Move(player, it.position) }

    private fun accessiblePositionsAdjacentTo(p: Position, includeDiagonals: Boolean = false, includeSunkenLocations: Boolean = false): List<MapSite> =
        gameSetup.map.adjacentSites(p, includeDiagonals).filter { !isSunken(it) || includeSunkenLocations }

    private fun sitesNavigatorCanSend(player: Adventurer): List<MapSite> =
        positionOf(player).let { playersCurrentPosition ->
            accessiblePositionsAdjacentTo(
                playersCurrentPosition,
                includeDiagonals = player == Explorer,
                includeSunkenLocations = player == Diver
            ).flatMap { immediateNeighbourSite ->
                accessiblePositionsAdjacentTo(
                    immediateNeighbourSite.position,
                    includeDiagonals = player == Explorer,
                    includeSunkenLocations = false
                ) + immediateNeighbourSite
            }.distinct() - gameSetup.mapSiteAt(playersCurrentPosition)
        }

    private fun pilotHasAlreadyUsedAFlightThisTurn() =
        previousActions.takeLastWhile { it !is DrawFromFloodDeck }.any { it is Fly }

    private fun availablePilotFlights(pilotAvailableMoves: List<Move>, playerPosition: Position, player: Adventurer) =
        (gameSetup.map.mapSites
            .filterNot(::isSunken)
            .map(MapSite::position)
            - pilotAvailableMoves.map(Move::position)
            - playerPosition)
            .map { Fly(player, it) }

    private fun diverSwimPositionsFrom(playersCurrentSite: MapSite): List<MapSite> {
        tailrec fun findPositions(startingPoints: List<MapSite>, reachable: MutableList<MapSite>): List<MapSite> {
            val neighbours = startingPoints.flatMap { accessiblePositionsAdjacentTo(it.position, includeSunkenLocations = true) }
            val floodedAndSunkenNeighbours = neighbours.filterNot { locationFloodStates.getValue(it.location) == Unflooded }
            val newStartingPoints = floodedAndSunkenNeighbours - reachable
            reachable += neighbours
            return if (newStartingPoints.any()) findPositions(newStartingPoints, reachable) else reachable
        }

        return findPositions(listOf(playersCurrentSite), mutableListOf()).filterNot(::isSunken) - playersCurrentSite
    }

    private fun availableShoreUpActions(player: Adventurer, playerPosition: Position): List<GameAction> {
        val floodedPositions = (gameSetup.map.adjacentSites(playerPosition, includeDiagonals = player == Explorer)
            + gameSetup.mapSiteAt(playerPosition))
            .filter { locationFloodStates[it.location] == Flooded }
            .map(MapSite::position)
        return floodedPositions.map { ShoreUp(player, it) } +
            if (player != Engineer) emptyList()
            else floodedPositions
                    .flatMap { p1 -> floodedPositions.mapNotNull { p2 -> if (p1 < p2) ShoreUp(player, p1, p2) else null } }
    }

    private fun availableGiveTreasureCardActions(player: Adventurer, playerPosition: Position): List<GameAction> =
        (if (player == Messenger) gameSetup.players else playerPositions.filter { it.value == playerPosition }.keys)
            .filter { it != player }
            .flatMap { colocatedPlayer ->
                playerCards.getValue(player).mapNotNull { it as? TreasureCard }.distinct()
                    .map { GiveTreasureCard(player, colocatedPlayer, it) }
            }

    private fun availableCaptureTreasureActions(player: Adventurer, playerPosition: Position): List<GameAction> =
        gameSetup.locationAt(playerPosition).pickupLocationForTreasure?.let { treasureAtLocation ->
            if (!treasuresCollected.getValue(treasureAtLocation) &&
                playerCards.getValue(player).count { it is TreasureCard && it.treasure == treasureAtLocation } >= 4)
                listOf(CaptureTreasure(player, treasureAtLocation))
            else null
        } ?: listOf()

    private fun allHelicopterLiftActions(): List<GameAction> =
        playerCards.filterValues { HelicopterLiftCard in it }.keys.flatMap { playerWithCard ->
            locationFloodStates.filterValues { it != Sunken }.keys.map(gameSetup::positionOf).let { accessiblePositions ->
                playerPositions.toCombinations().mapValues { accessiblePositions - it.value }.flatMap { (otherPlayers, destinations) ->
                    destinations.map { HelicopterLift(playerWithCard, otherPlayers, it) }
                }
            }
        }

    private fun allSandbagActions(): List<GameAction> =
        playerCards.filterValues { SandbagsCard in it }.keys.flatMap { playerWithCard ->
            locationsWithState(Flooded).map(gameSetup::positionOf).let { floodedPositions ->
                floodedPositions.map { Sandbag(playerWithCard, it) }
            }
        }

    private fun availableSwimToSafetyActions(player: Adventurer): List<GameAction> =
        when (player) {
            Pilot -> locationFloodStates.filterValues { it != Sunken }.keys.map(gameSetup::positionOf)
            Diver -> diverSwimToSafetyPositions()
            else -> positionOf(player).adjacentPositions(includeDiagonals = player == Explorer).filterNot(::isSunken)
        }.map { SwimToSafety(player, it) }

    private fun diverSwimToSafetyPositions(): List<Position> {
        tailrec fun closestUnsunkenPositions(positions: List<MapSite>): List<Position> {
            val allAdjacentSites = positions.flatMap { gameSetup.map.adjacentSites(it.position, false) }
            val unsunkenAdjacentSites = allAdjacentSites.filterNot(::isSunken)
            return if (unsunkenAdjacentSites.any()) unsunkenAdjacentSites.map(MapSite::position)
                else closestUnsunkenPositions(allAdjacentSites)
        }

        return closestUnsunkenPositions(listOf(gameSetup.mapSiteAt(positionOf(Diver))))
    }

    private fun helicopterLiftOffIslandIfAvailable(): List<GameAction> =
        if (treasuresCollected.values.all { it == true } &&
            listOf(gameSetup.positionOf(FoolsLanding)) == playerPositions.values.distinct())
            playerCards.filterValues { HelicopterLiftCard in it }.keys.map(::HelicopterLiftOffIsland)
        else
            emptyList()

    fun nextStateAfter(action: GameAction, random: Random): GameState {
        require(action in availableActions) { "'$action' is not an available action in this state" }
        return with(if (action is CardDiscardingAction) action.playerDiscardingCard discards action.discardedCards else this) {
            when (action) {
                is DiscardCard, is HelicopterLiftOffIsland -> this
                is PlayerMovingAction -> copy(playerPositions = playerPositions + (action.player to action.position))
                is ShoreUp -> copy(locationFloodStates = locationFloodStates + (gameSetup.locationAt(action.position) to Unflooded))
                is CaptureTreasure -> copy(treasuresCollected = treasuresCollected + (action.treasure to true))
                is HelicopterLift -> copy(playerPositions = (playerPositions + action.playersBeingMoved.map { (it to action.position) }).imm())
                is Sandbag -> copy(locationFloodStates = locationFloodStates + (gameSetup.locationAt(action.position) to Unflooded))
                is GiveTreasureCard -> copy(playerCards =
                    playerCards + (action.player   to playerCards.getValue(action.player)  .subtract(listOf(action.card))) +
                                  (action.receiver to playerCards.getValue(action.receiver).plus(    listOf(action.card)))
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
                            playerCards = playerCards + (action.player to playerCards.getValue(action.player).plus(drawnCard)),
                            treasureDeck = treasureDeck.drop(1).imm()
                        )
                    }.let {
                        if (it.treasureDeck.any()) it
                        else it.copy(treasureDeck = it.treasureDeckDiscard.shuffled(random).imm(), treasureDeckDiscard = cards())
                    }
                }
                is DrawFromFloodDeck -> floodDeck.first().let { floodedLocation ->
                    locationFloodStates.getValue(floodedLocation).flooded().let { newFloodState ->
                        copy(
                            floodDeck = floodDeck.drop(1).imm(),
                            locationFloodStates = locationFloodStates + (floodedLocation to newFloodState),
                            floodDeckDiscard = if (newFloodState == Sunken) floodDeckDiscard else floodDeckDiscard + floodedLocation
                        )
                    }.let {
                        if (it.floodDeck.any()) it
                        else it.copy(floodDeck = it.floodDeckDiscard.shuffled().imm(), floodDeckDiscard = immListOf())
                    }
                }
                else -> throw IllegalStateException("Unhandled action: $action")
            }.let { newState -> newState.copy(
                phase = phase.phaseAfter(action, newState),
                previousActions = previousActions + action
            )}
        }
    }

    private infix fun Adventurer.discards(cardList: Collection<HoldableCard>) =
            copy(
                    playerCards = playerCards + (this to playerCards.getValue(this).subtract(cardList)),
                    treasureDeckDiscard = treasureDeckDiscard + cardList
            )

    val sunkPlayers: Set<Adventurer> by lazy { playerPositions.filter { isSunken(it.value) }.keys }

    fun locationsWithState(state: LocationFloodState) = locationFloodStates.filterValues { it == state }.keys

    val playerCardCounts: Map<Adventurer, Int> = playerCards.mapValues { it.value.size }

    fun positionOf(player: Adventurer): Position = playerPositions.getValue(player)

    fun locationOf(player: Adventurer): Location = gameSetup.locationAt(positionOf(player))

    fun isSunken(location: Location): Boolean = locationFloodStates.getValue(location) == Sunken

    fun isSunken(position: Position): Boolean = isSunken(gameSetup.locationAt(position))

    fun isSunken(mapSite: MapSite): Boolean = isSunken(mapSite.location)
}

private fun ImmutableMap<Adventurer, Position>.toCombinations(): Map<ImmutableSet<Adventurer>, Position> {
    return this.transpose().flatMap { (position, players) ->
        players.toCombinations().map { it to position }
    }.toMap()
}

private fun ImmutableSet<Adventurer>.toCombinations(): Collection<ImmutableSet<Adventurer>> =
    if (this.size == 1) listOf(immSetOf(first()))
    else listOf(this) + (this.flatMap { (this - it).toSortedSet().imm().toCombinations() }.distinct())

private fun <K: Comparable<K>, V> ImmutableMap<K, V>.transpose(): Map<V, ImmutableSet<K>> =
    this.values.toSet().associate { v -> v to this.filterValues { it == v }.keys.toSortedSet().imm() }
