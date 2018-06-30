package com.grahamlea.forbiddenisland.play.grahamlea

import com.grahamlea.forbiddenisland.*
import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Adventurer.Companion.randomListOfPlayers
import com.grahamlea.forbiddenisland.Game.Companion.newRandomGameFor
import com.grahamlea.forbiddenisland.Location.FoolsLanding
import com.grahamlea.forbiddenisland.LocationFloodState.Flooded
import com.grahamlea.forbiddenisland.LocationFloodState.Sunken
import com.grahamlea.forbiddenisland.play.*
import java.util.*
import kotlin.reflect.KFunction1
import kotlin.system.measureTimeMillis

private val stepThrough = false

private val debug: DebugPredicate? =
    null
//{ gameState, selectedAction, selector, previousAction, previousSelector ->
//    if (selectedAction is PlayerMovingAction) {
//        if (previousAction is PlayerMovingAction
//            && previousAction.player == selectedAction.player
//            && previousMoveStartingPosition == selectedAction.position) {
//            println("${selectedAction.player} moving back to $previousMoveStartingPosition because of $selector straight after moving off it with $previousSelector")
//                true
//        } else {
//            false
//        }.also {
//            previousMoveStartingPosition = gameState.playerPositions[selectedAction.player]
//        }
//    } else {
//        previousMoveStartingPosition = null
//        false
//    }
//}

private var previousMoveStartingPosition: Position? = null

fun main(args: Array<String>) {
    val seedGenerator = Random(90)
    if (stepThrough) {
        val game = newRandomGameFor(randomListOfPlayers(4, seedGenerator), Random(seedGenerator.nextLong()))
        stepThroughGame(game, RuleBasedGamePlayer(), Random(seedGenerator.nextLong()))
    } else if (debug != null) {
        val games = 200
        (0..games).forEach {
            val gameSeed = seedGenerator.nextLong()
            val gameRandom = Random(gameSeed)
            val game = newRandomGameFor(randomListOfPlayers(seedGenerator.nextInt(3) + 2, seedGenerator), gameRandom)
            debugGame(game, RuleBasedGamePlayer(), debug, gameRandom)
//            println(game.gameState)
//            println("Game Result: ${game.gameState.result}")
        }
    } else {
        measureTimeMillis {
            printGamePlayerTestResult(testGamePlayer(RuleBasedGamePlayer(), 100_000/*, startingFloodLevels = listOf(StartingFloodLevel.Novice)*/))
        }.also {
            println("\nTime: ${"%.1f".format(it / 1000f)}s")
        }
    }
}

private typealias ActionSelector = (KFunction1<@ParameterName(name = "withState") RuleBasedGamePlayer.GameStateContext, GameAction?>)

class RuleBasedGamePlayer: ExplainingGamePlayer() {

    override fun newContext(game: Game, deterministicRandomForGamePlayerDecisions: Random): ExplainingGamePlayContext = Context(game)

    private val rules: Sequence<ActionSelector> = sequenceOf(
        // High-priority stuff
        ::selectSingleAction,
        ::captureTreasure,
        ::headForFoolsLandingWithAllTreasures,
        ::finishTheGame,

        ::givePlayerTheFourthTreasureCard,

        // Avoid losing the game
        ::shoreUpFoolsLanding,
        ::stayOnFoolsLandingWithAllTreasures,
        ::moveToShoreUpFoolsLandingIfClose,
        ::sandbagFoolsLandingIfCloseToSinking,
        ::helicopterLiftNextPlayerToHaveTurnToFoolsLandingIfCloseToSinking,
        ::shoreUpOrphanedTreasureCollectionSites,
        ::moveToShoreUpOrphanedTreasureCollectionSiteIfClose,
        ::sandbagOrphanedTreasureCollectionSitesIfCloseToSinking,
        ::helicopterLiftNextPlayerToHaveTurnToOrphanedTreasureCollectionSiteCloseToSinking,
        ::shoreUpUncollectedTreasureSites,
        ::helicopterLiftStrandedPlayers,

        // Discard the right Treasure Cards
        ::discardTreasureCardsOfCollectedTreasures,
        ::discardTreasureCardsThatAnotherPlayerHasFourOf,
        ::discardTreasureCardsWhenTwoOrMoreInDiscard,
        ::useSandbagRatherThanDiscardOneOfFourTreasureCards,
        ::helicopterLiftToTreasurePickupSiteRatherThanDiscardOneOfFourTreasureCards,
        ::useHelicopterLiftRatherThanDiscardOneOfFourTreasureCards,
        ::useSandbagRatherThanDiscardTreasureCard,
        ::discardOddTreasureCardWhenPlayerHasThreeOfSomething,
        ::discardSandbagRatherThanOneOfFourTreasureCards,
        ::discardRarestTreasureCards,

        // Trade cards if beneficial
        ::giveTreasureCardsToPlayersWhoNeedThem,

        // Move somewhere useful
        ::moveOntoTreasureCollectionPointIfPlayerHasFourMatchingCards,
        ::movePlayersTowardsOtherPlayersTheyCanGiveCardsTo,

        // Do basic stuff
        ::moveIfMovingCouldShoreUpTwoThings,
        ::shoreStuffUp,
        ::moveTowardsTreasureCollectionPointIfPlayerHasThreeMatchingCards,
        ::moveToShoreUpCorePositions,
        ::moveToShoreSomethingUp,
        ::moveTowardsTheMiddle,

        // Obligatory stuff
        ::drawFromTreasureDeck,
        ::drawFromFloodDeck,
        ::swimToSafety,

        // Can't find anything better to do
        ::endTurnEarly
    )

    private inner class Context(val game: Game) : ExplainingGamePlayContext() {

        override fun selectNextActionWithSelector(): Pair<GameAction, Selector> {
            GameStateContext(game).let { stateContext ->
                return rules.mapNotNull { selector ->
                    selector(stateContext)?.let { selectedAction -> Pair(selectedAction, selector.name) }
                }.first()
            }
        }
    }

    internal inner class GameStateContext(game: Game) {

        operator fun <R> invoke(f: GameStateContext.() -> R): R = with(this) { f() }

        val state = game.gameState
        val map = state.gameSetup.map
        val setup = state.gameSetup
        val actions = state.availableActions
        val cards = state.playerCards
        val phase = state.phase
        val foolsLandingPosition = state.gameSetup.positionOf(FoolsLanding)
        val foolsLandingNeighbours by lazy { map.adjacentSites(foolsLandingPosition, true) }
        val collectedTreasures by lazy { state.treasuresCollected.filterValues { it }.keys }
        val uncollectedTreasures by lazy { state.treasuresCollected.filterValues { !it }.keys }
        val uncollectedTreasurePositions by lazy { uncollectedTreasurePickupSitesByTreasure.flatMap { it.value.map { it.position } } }
        val cardsPerPlayer: Map<Pair<Adventurer, HoldableCard>, Int> by lazy { cards.flatMap { (p, cards) -> cards.map { p to it } }.groupingBy { it }.eachCount() }

        val positionsOfAndAdjacentToPlayers: Map<Adventurer, List<Position>> by lazy {
            state.playerPositions.map { it.key to map.adjacentSites(it.value, it.key == Explorer).map(MapSite::position) + it.value }.toMap()
        }

        val middlePositionClosestToFoolsLanding by lazy {
            when (Pair(foolsLandingPosition.x >= 4, foolsLandingPosition.y >= 4)) {
                Pair(false, false) -> Position(3, 3)
                Pair(true, false) -> Position(4, 3)
                Pair(false, true) -> Position(3, 4)
                Pair(true, true) -> Position(4, 4)
                else -> throw IllegalStateException()
            }
        }

        val treasurePickupSitesByTreasure: Map<Treasure, List<MapSite>> by lazy {
            map.mapSites.mapNotNull { site -> site.location.pickupLocationForTreasure?.let { site } }
                .groupBy { it.location.pickupLocationForTreasure!! }
        }

        val uncollectedTreasurePickupSitesByTreasure: Map<Treasure, List<MapSite>>
            by lazy { treasurePickupSitesByTreasure.filterKeys { it in uncollectedTreasures } }

        val orphanedTreasureCollectionSites: List<MapSite>
            by lazy {
                uncollectedTreasurePickupSitesByTreasure.values.mapNotNull { it.singleOrNull { !state.isSunken(it) } }
            }

        val orphanedTreasureCollectionPositions: List<Position>
            by lazy { orphanedTreasureCollectionSites.map { it.position } }

        val almostCollectableTreasuresByPlayer: Map<Adventurer, Treasure>
            by lazy {
                cardsPerPlayer
                    .filterValues { it >= 3 }
                    .keys.mapNotNull { (player, card) -> (card as? TreasureCard)?.let { (player to it.treasure) } }
                    .filter { it.second in uncollectedTreasures }
                    .toMap()
            }

        val collectableTreasuresByPlayer: Map<Adventurer, Treasure>
            by lazy {
                cardsPerPlayer
                    .filterValues { it >= 4 }
                    .keys.mapNotNull { (player, card) -> (card as? TreasureCard)?.let { (player to it.treasure) } }
                    .filter { it.second in uncollectedTreasures }
                    .toMap()
            }

        val nextPlayerToHaveATurn by lazy { nextPlayerToHaveATurn(phase, false) }

        val playersToSoonHaveATurn by lazy {
            setOf(nextPlayerToHaveATurn, nextPlayerToHaveATurn(phase, true))
        }

        private tailrec fun nextPlayerToHaveATurn(phase: GamePhase, skipOneAction: Boolean): Adventurer {
            return when (phase) {
                is AwaitingPlayerAction ->
                    if (!skipOneAction) phase.player
                    else nextPlayerToHaveATurn(phase.phaseAfter(Move(phase.player, foolsLandingPosition), state), false)
                is AwaitingTreasureDeckDraw -> setup.players[(setup.players.indexOf(phase.player) + 1) % setup.players.size]
                is AwaitingFloodDeckDraw -> setup.players[(setup.players.indexOf(phase.player) + 1) % setup.players.size]
                is AwaitingPlayerToDiscardExtraCard -> nextPlayerToHaveATurn(phase.returningToPhase, skipOneAction)
                is AwaitingPlayerToSwimToSafety -> nextPlayerToHaveATurn(phase.returningToPhase, skipOneAction)
                is GameOver -> throw IllegalStateException()
            }
        }

        val desiredCardTradesToPlayers: Map<Adventurer, List<Triple<Adventurer, TreasureCard, Int>>>
            by lazy {
                val desiredCards = cardsPerPlayer.filterValues { it in 2..3 }
                val availableCards = cardsPerPlayer.filterValues { it in 1..2 }
                desiredCards.map { (pc, count) ->
                    pc.let { (desiringPlayer, desiredCard) ->
                        desiringPlayer to
                            availableCards
                                .filterKeys { it.first != desiringPlayer }
                                .filterKeys { it.second is TreasureCard && it.second == desiredCard }
                                .map { (k, v) -> Triple(k.first, k.second as TreasureCard, Math.min(v, 4 - count)) }
                    }
                }.toMap()
            }

        val strandedPlayers: Set<Adventurer> by lazy {
            setup.players.filterNot { it == Diver || it == Pilot }
                .associate { it to setup.mapSiteAt(state.playerPositions.getValue(it)) }
                .filterValues { floodState(it.location) == Flooded }
                .filter {
                    setup.map.adjacentSites(it.value.position, it.key == Explorer).map {
                        floodState(it.location)
                    }.all { it == Sunken }
                }.keys
        }

        fun floodState(location: Location) = state.locationFloodStates.getValue(location)
        fun floodState(position: Position) = floodState(map.locationAt(position))
        fun isFlooded(position: Position) = floodState(map.locationAt(position)) == Flooded

        @Suppress("UNCHECKED_CAST")
        inline fun <reified P : GamePhase> inPhase(f: (P) -> GameAction?): GameAction? =
            whenever(P::class.isInstance(phase), { f(phase as P) })

        fun positionOf(player: Adventurer) = state.playerPositions.getValue(player)

        fun playerIsOnCollectLocationFor(player: Adventurer, treasure: Treasure) =
            map.locationAt(positionOf(player)).pickupLocationForTreasure == treasure

        fun walkingDistance(position: Position, treasure: Treasure, includeDiagonals: Boolean): Int? {
            return uncollectedTreasurePickupSitesByTreasure.getValue(treasure)
                .filterNot { state.isSunken(it) }
                .mapNotNull { walkingDistance(position, it.position, includeDiagonals) }
                .min()
        }

        fun walkingDistance(fromPosition: Position, toPosition: Position, includeDiagonals: Boolean): Int? {
            tailrec fun distance(startingSites: List<MapSite>, visitedSites: List<MapSite> = listOf(), distance: Int = 0): Int? {
                val newSites = startingSites
                    .flatMap { map.adjacentSites(it.position, includeDiagonals) }
                    .filterNot(visitedSites::contains)
                    .filterNot(state::isSunken)
                if (newSites.isEmpty()) return null
                if (newSites.any { it.position == toPosition }) return distance + 1
                return distance(newSites, visitedSites + newSites, distance + 1)
            }
            return if (fromPosition == toPosition) 0 else distance(listOf(map.mapSiteAt(fromPosition)))
        }
    }

    private fun selectSingleAction(withState: GameStateContext): GameAction? = withState { actions.singleOrNull() }

    private fun finishTheGame(withState: GameStateContext): GameAction? =
        withState {
            actions.firstOfType<HelicopterLiftOffIsland> { true }
        }

    private fun shoreUpFoolsLanding(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfTypeWithPreference<ShoreUp>(
                    { foolsLandingPosition in listOf(it.position, it.position2) },
                    { it.position2 != null })
            }
        }

    private fun moveToShoreUpFoolsLandingIfClose(withState: GameStateContext): GameAction? =
        withState {
            whenever(phase is AwaitingPlayerToSwimToSafety || (phase is AwaitingPlayerAction && phase.actionsRemaining != 1)) {
                whenever(floodState(FoolsLanding) == Flooded && state.playerCards.values.flatten().none { it is SandbagsCard }) {
                    actions.firstOfType<PlayerMovingAction> { action ->
                        action.player in playersToSoonHaveATurn &&
                        FoolsLanding in map.adjacentSites(action.position).map(MapSite::location)
                            && positionOf(action.player) != foolsLandingPosition
                            && foolsLandingPosition !in map.adjacentSites(positionOf(action.player), action.player == Explorer).map(MapSite::position)
                            && foolsLandingPosition !in positionsOfAndAdjacentToPlayers.filterKeys { it != action.player }.values.flatten()
                    } as GameAction?
                }
            }
        }

    private fun sandbagFoolsLandingIfCloseToSinking(withState: GameStateContext): GameAction? =
        withState {
            whenever(floodState(FoolsLanding) == Flooded && FoolsLanding !in state.floodDeckDiscard) {
                actions.firstOfType<Sandbag> { it.position == foolsLandingPosition }
            }
        }

    private fun helicopterLiftNextPlayerToHaveTurnToFoolsLandingIfCloseToSinking(withState: GameStateContext): GameAction? =
        withState {
            whenever(floodState(FoolsLanding) == Flooded && FoolsLanding !in state.floodDeckDiscard) {
                actions.firstOfType<HelicopterLift> {
                    it.position == foolsLandingPosition
                        && it.playersBeingMoved.singleOrNull() == nextPlayerToHaveATurn
                }
            }
        }

    private fun shoreUpOrphanedTreasureCollectionSites(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfTypeWithPreferenceRank<ShoreUp>(
                    { it.position in orphanedTreasureCollectionPositions || it.position2 in orphanedTreasureCollectionPositions },
                    {
                        listOf(it.position, it.position2).let { positions ->
                            when {
                                orphanedTreasureCollectionPositions.containsAll(positions) -> 0
                                positions.any { it in uncollectedTreasurePositions } -> 1
                                else -> 2
                            }
                        }
                    }
                )
            }
        }

    private fun moveToShoreUpOrphanedTreasureCollectionSiteIfClose(withState: GameStateContext): GameAction? =
        withState {
            whenever(phase is AwaitingPlayerAction || phase is AwaitingPlayerToSwimToSafety) {
                val player = (phase as? AwaitingPlayerAction)?.player ?: (phase as AwaitingPlayerToSwimToSafety).player
                val targetSites = orphanedTreasureCollectionSites.filter { floodState(it.location) == Flooded }
                actions.firstOfType<PlayerMovingAction>
                    {
                        val fixableSitesAtDestination = targetSites intersect map.adjacentSites(it.position)
                        it.player == player &&
                        fixableSitesAtDestination.any()
                            && fixableSitesAtDestination.size >
                                (targetSites intersect map.adjacentSites(state.playerPositions.getValue(it.player))).size
                    }

            } as GameAction?
        }

    private fun sandbagOrphanedTreasureCollectionSitesIfCloseToSinking(withState: GameStateContext): GameAction? =
        withState {
            orphanedTreasureCollectionSites.mapNotNull { orphanSite ->
                whenever(floodState(orphanSite.location) == Flooded && orphanSite.location !in state.floodDeckDiscard) {
                    actions.firstOfType<Sandbag> { it.position == orphanSite.position }
                }
            }.firstOrNull()
        }

    private fun helicopterLiftNextPlayerToHaveTurnToOrphanedTreasureCollectionSiteCloseToSinking(withState: GameStateContext): GameAction? =
        withState {
            orphanedTreasureCollectionSites.mapNotNull { orphanSite ->
                whenever(floodState(orphanSite.location) == Flooded && orphanSite.location !in state.floodDeckDiscard) {
                    actions.firstOfType<HelicopterLift> {
                        it.position == orphanSite.position && it.playersBeingMoved.singleOrNull() == nextPlayerToHaveATurn
                    }
                }
            }.firstOrNull()
        }

    private fun shoreUpUncollectedTreasureSites(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfTypeWithPreferenceRank<ShoreUp>(
                    { it.position in uncollectedTreasurePositions || it.position2 in uncollectedTreasurePositions },
                    {
                        when {
                            setup.players.size != 4 -> 0
                            uncollectedTreasurePositions.containsAll(listOf(it.position, it.position2)) -> 0
                            else -> 1
                        }
                    }
                )
            }
        }

    private fun helicopterLiftStrandedPlayers(withState: GameStateContext): GameAction? =
        withState {
            whenever(strandedPlayers.any()) {
                actions.firstOfTypeWithPreferenceRank<HelicopterLift>(
                    { (strandedPlayers intersect it.playersBeingMoved).any() && it.position == foolsLandingPosition },
                    { -it.playersBeingMoved.size }
                )
            }
        }

    private fun captureTreasure(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfType<CaptureTreasure> { true }
            }
        }

    private fun headForFoolsLandingWithAllTreasures(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                whenever(uncollectedTreasures.isEmpty()) {
                    actions.firstOfTypeWithPreferenceRank<PlayerMovingAction>({ positionOf(it.player) != foolsLandingPosition }) {
                        walkingDistance(it.position, foolsLandingPosition, it.player == Explorer)
                    }
                }
            }
        }

    private fun stayOnFoolsLandingWithAllTreasures(withState: GameStateContext): GameAction? =
        withState {
            whenever(uncollectedTreasures.isEmpty()) {
                inPhase<AwaitingPlayerAction> {
                    actions.firstOfType<DrawFromTreasureDeck> { true }
                }
            }
        }

    private fun discardTreasureCardsOfCollectedTreasures(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                actions.firstOfType<DiscardCard> { (it.card as? TreasureCard)?.treasure in collectedTreasures }
            }
        }

    private fun discardTreasureCardsThatAnotherPlayerHasFourOf(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                actions.firstOfType<DiscardCard> { (player, card) ->
                    card is TreasureCard
                        && cardsPerPlayer.filterKeys { it.second == card && it.first != player }
                        .values.any { it >= 4 }
                }
            }
        }

    private fun discardTreasureCardsWhenTwoOrMoreInDiscard(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                actions.firstOfType<DiscardCard> { (_, card) ->
                    card is TreasureCard && state.treasureDeckDiscard.count { it == card } >= 2
                }
            }
        }

    private fun useSandbagRatherThanDiscardOneOfFourTreasureCards(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                whenever (state.playerCards.getValue(it.playerWithTooManyCards)
                        .filter { it is TreasureCard }
                        .distinct()
                        .size == 1) {
                    actions.firstOfTypeWithPreferenceRank<Sandbag>({ true }) {
                        when {
                            it.position == foolsLandingPosition -> 0
                            it.position in orphanedTreasureCollectionPositions -> 1
                            it.position in uncollectedTreasurePositions -> 2
                            it.position in foolsLandingPosition.adjacentPositions(true) -> 3
                            it.position in orphanedTreasureCollectionPositions.flatMap { it.adjacentPositions(true) } -> 4
                            else -> 5
                        }
                    }
                }
            }
        }

    private fun useSandbagRatherThanDiscardTreasureCard(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                actions.firstOfTypeWithPreferenceRank<Sandbag>({ true }) {
                    when {
                        it.position == foolsLandingPosition -> 0
                        it.position in orphanedTreasureCollectionPositions -> 1
                        it.position in uncollectedTreasurePositions -> 2
                        it.position in foolsLandingPosition.adjacentPositions(true) -> 3
                        it.position in orphanedTreasureCollectionPositions.flatMap { it.adjacentPositions(true) } -> 4
                        it.position in uncollectedTreasurePositions.flatMap { it.adjacentPositions(true) } -> 5
                        else -> 6
                    }
                }
            }
        }
    private fun discardSandbagRatherThanOneOfFourTreasureCards(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                whenever (state.playerCards.getValue(it.playerWithTooManyCards)
                        .filter { it is TreasureCard }
                        .distinct()
                        .size == 1) {
                    actions.firstOfType<DiscardCard> { it.card is SandbagsCard }
                }
            }
        }

    private fun helicopterLiftToTreasurePickupSiteRatherThanDiscardOneOfFourTreasureCards(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                val distinctTreasureCards = state.playerCards.getValue(it.playerWithTooManyCards)
                    .mapNotNull { it as? TreasureCard }
                    .distinct()
                whenever (distinctTreasureCards.size == 1) {
                    uncollectedTreasurePickupSitesByTreasure[distinctTreasureCards.first().treasure]
                        ?.let { positions ->
                            actions.firstOfType<HelicopterLift> { it.position in positions.map(MapSite::position) }
                        }
                }
            }
        }

    private fun useHelicopterLiftRatherThanDiscardOneOfFourTreasureCards(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                val distinctTreasureCards = state.playerCards.getValue(it.playerWithTooManyCards)
                    .mapNotNull { it as? TreasureCard }
                    .distinct()
                whenever (distinctTreasureCards.size == 1) {
                    actions.firstOfTypeWithPreferenceRank<HelicopterLift>({ true }) {
                        when {
                            it.position == foolsLandingPosition -> 0
                            it.position in orphanedTreasureCollectionPositions -> 2
                            it.position in uncollectedTreasurePositions -> 4
                            it.position in foolsLandingPosition.adjacentPositions(true) -> 6
                            it.position in orphanedTreasureCollectionPositions.flatMap { it.adjacentPositions(true) } -> 8
                            else -> 10
                        } - (if (floodState(it.position) == Flooded) 11 else 0)
                    }
                }
            }
        }

    private fun discardOddTreasureCardWhenPlayerHasThreeOfSomething(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> { phase ->
                val treasureCardPlayerHasFourOf: Treasure? =
                    (cardsPerPlayer.filterKeys { it.second is TreasureCard }.toList().firstOrNull { it.first.first == phase.playerWithTooManyCards && it.second >= 3 }
                        ?.first?.second as TreasureCard?)?.treasure
                whenever(treasureCardPlayerHasFourOf != null) {
                    actions.firstOfType<DiscardCard> { it.card is TreasureCard && it.card.treasure != treasureCardPlayerHasFourOf }
                }
            }
        }

    private fun discardRarestTreasureCards(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToDiscardExtraCard> {
                val totalCardCounts = cards.values.flatten().mapNotNull { it as? TreasureCard }.groupingBy { it.treasure }.eachCount()
                actions.firstOfTypeWithPreferenceRank<DiscardCard>(
                    { it.card is TreasureCard },
                    { totalCardCounts.getValue(treasure(it)) }
                )
            }
        }

    private fun treasure(event: DiscardCard) = (event.card as TreasureCard).treasure

    private fun givePlayerTheFourthTreasureCard(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfType<GiveTreasureCard> {
                    cardsPerPlayer[Pair(it.receiver, it.card)] == 3 && it.card.treasure in uncollectedTreasures
                }
            }
        }

    private fun giveTreasureCardsToPlayersWhoNeedThem(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfTypeWithPreferenceRank<GiveTreasureCard>(
                    { action -> desiredCardTradesToPlayers[action.receiver]?.any { it.first == action.player } ?: false },
                    { - (cardsPerPlayer[Pair(it.receiver, it.card)] ?: 0) }
                )
            }
        }

    private fun moveTowardsTreasureCollectionPointIfPlayerHasThreeMatchingCards(withState: GameStateContext): GameAction? =
        withState {
            whenever(phase is AwaitingPlayerAction || phase is AwaitingPlayerToSwimToSafety) {
                val player = (phase as? AwaitingPlayerAction)?.player ?: (phase as AwaitingPlayerToSwimToSafety).player
                actions.firstOfTypeWithPreferenceRank<PlayerMovingAction>(
                    {
                        it.player == player &&
                        it.player in almostCollectableTreasuresByPlayer.keys &&
                            !playerIsOnCollectLocationFor(it.player, almostCollectableTreasuresByPlayer.getValue(it.player))
                    },
                    { walkingDistance(it.position, almostCollectableTreasuresByPlayer.getValue(it.player), it.player == Explorer) })
            }
        }

    private fun moveOntoTreasureCollectionPointIfPlayerHasFourMatchingCards(withState: GameStateContext): GameAction? =
        withState {
            whenever(phase is AwaitingPlayerAction || phase is AwaitingPlayerToSwimToSafety) {
                actions.firstOfTypeWithPreferenceRank<PlayerMovingAction>(
                    {
                        it.player in collectableTreasuresByPlayer.keys &&
                            !playerIsOnCollectLocationFor(it.player, collectableTreasuresByPlayer.getValue(it.player))
                    },
                    { walkingDistance(it.position, collectableTreasuresByPlayer.getValue(it.player), it.player == Explorer) })
            }
        }

    private fun movePlayersTowardsOtherPlayersTheyCanGiveCardsTo(withState: GameStateContext): GameAction? =
        withState {
            whenever(phase is AwaitingPlayerAction || phase is AwaitingPlayerToSwimToSafety) {
                val player = (phase as? AwaitingPlayerAction)?.player ?: (phase as AwaitingPlayerToSwimToSafety).player
                val treasure = almostCollectableTreasuresByPlayer[player]
                whenever(treasure == null || !playerIsOnCollectLocationFor(player, treasure)) {
                    actions.firstOfTypeWithPreferenceRank<PlayerMovingAction>(
                        { action ->
                            (action.player in desiredCardTradesToPlayers.keys
                                || action.player in desiredCardTradesToPlayers.values.flatMap { it.map(Triple<Adventurer, TreasureCard, Int>::first) })
                            && !playerAlreadyOnGoodTileForTrading(withState, action.player)
                            && !(collectableTreasuresByPlayer[action.player]?.let {
                                    playerIsOnCollectLocationFor(action.player, it)
                                } ?: false)
                        },
                        {
                            val movingPlayer = it.player
                            val moveDestination = it.position

                            val tradesTheMovingPlayerCanMake: Map<Adventurer, List<Triple<Adventurer, TreasureCard, Int>>> =
                                desiredCardTradesToPlayers
                                    .mapValues { it.value.filter { (givingPlayer, _, _) -> givingPlayer == movingPlayer } }
                                    .filter { it.key == movingPlayer || it.value.any() }

                            tradesTheMovingPlayerCanMake.flatMap { (receivingPlayer, trades) ->
                                trades.map { (sendingPlayer, _, cardsToTrade) ->
                                    val otherPlayer = if (sendingPlayer == movingPlayer) receivingPlayer else sendingPlayer
                                    // Shorter walking distance is better
                                    walkingDistance(moveDestination, positionOf(otherPlayer), includeDiagonals = movingPlayer == Explorer)
                                        ?.plus((if (cardsToTrade > 1) 0 else 20)) // Having 2 cards to trade is better
                                }
                            }.filterNotNull().min()

                        }
                    )
                }
            }
        }

    private fun playerAlreadyOnGoodTileForTrading(withState: GameStateContext, player: Adventurer): Boolean =
        withState {
            positionOf(player) in
                ((desiredCardTradesToPlayers[player]?.map { positionOf(it.first) } ?: emptyList())
                + desiredCardTradesToPlayers.filterValues { it.any { it.first == player } }.keys.map(::positionOf))
        }

    private fun moveToShoreUpCorePositions(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> { phase ->
                whenever(phase.actionsRemaining > 1) {
                    actions.firstOfTypeWithPreferenceRank<Move>(
                        { it.player == phase.player && it.position in corePositions && isFlooded(it.position) },
                        { -map.adjacentSites(it.position, it.player == Explorer).map(MapSite::position).filter { it in corePositions }.count { floodState(it) == Flooded } }
                    )
                }
            }
        }

    private fun moveIfMovingCouldShoreUpTwoThings(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> { phase ->
                val treasure = almostCollectableTreasuresByPlayer[phase.player]
                whenever(phase.actionsRemaining > 1 && (treasure == null || !playerIsOnCollectLocationFor(phase.player, treasure))) {
                    actions.firstOfTypeWithPreferenceRank<Move>(
                        { it.player == phase.player
                            && map.adjacentSites(positionOf(it.player), it.player == Explorer).map(MapSite::position).count(::isFlooded) < 2
                            && map.adjacentSites(it.position, it.player == Explorer).map(MapSite::position).count(::isFlooded) >= 2
                            && !playerAlreadyOnGoodTileForTrading(withState, it.player)
                        },
                        { -map.adjacentSites(it.position, it.player == Explorer).map(MapSite::position).count(::isFlooded) }
                    )
                }
            }
        }

    private fun shoreStuffUp(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfTypeWithPreferenceRank<ShoreUp>({ true }) {
                    val positions = listOf(it.position, it.position2)
                    when {
                        (positions intersect foolsLandingNeighbours.map(MapSite::position)).any() -> 0
                        (positions intersect
                            uncollectedTreasurePositions.flatMap { map.adjacentSites(it) }.map(MapSite::position))
                            .any() -> 2
                        (positions intersect
                            state.playerPositions.values.flatMap { map.adjacentSites(it) }.map(MapSite::position))
                            .any() -> 4
                        (positions intersect corePositions).any() -> 6
                        (positions intersect mapEdgePositions).none() -> 8
                        else -> 10
                    }.minus(if (it.position2 != null) 3 else 0)
                }
            }
        }

    private fun moveTowardsTheMiddle(withState: GameStateContext): GameAction? =
        withState {
            whenever(phase is AwaitingPlayerAction || phase is AwaitingPlayerToSwimToSafety) {
                val player = (phase as? AwaitingPlayerAction)?.player ?: (phase as AwaitingPlayerToSwimToSafety).player
                whenever(almostCollectableTreasuresByPlayer[player] == null) {
                    actions.firstOfTypeWithPreferenceRank<Move>(
                        { positionOf(it.player) != middlePositionClosestToFoolsLanding && it.player == player },
                        { walkingDistance(it.position, middlePositionClosestToFoolsLanding, it.player == Explorer) }
                    )
                }
            }
        }

    private fun moveToShoreSomethingUp(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> { phase ->
                whenever(phase.actionsRemaining > 1) {
                    actions.firstOfTypeWithPreferenceRank<Move>(
                        {
                            map.adjacentSites(it.position, it.player == Explorer).map(MapSite::position).any { floodState(it) == Flooded }
                                && it.position !in mapEdgePositions
                                && it.player == phase.player
                        },
                        { -map.adjacentSites(it.position, it.player == Explorer).map(MapSite::position).count { floodState(it) == Flooded } }
                    )
                }
            }
        }

    private fun drawFromTreasureDeck(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingTreasureDeckDraw> {
                actions.firstOfType<DrawFromTreasureDeck> { true }
            }
        }

    private fun drawFromFloodDeck(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingFloodDeckDraw> {
                actions.firstOfType<DrawFromFloodDeck> { true }
            }
        }

    private fun swimToSafety(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerToSwimToSafety> {
                actions.firstOfTypeWithPreferenceRank<SwimToSafety>({ true },
                    {
                        when {
                            it.position == foolsLandingPosition -> 0
                            it.position in orphanedTreasureCollectionPositions -> 1
                            it.position in uncollectedTreasurePositions -> 2
                            it.position in foolsLandingPosition.adjacentPositions(true) -> 3
                            it.position in orphanedTreasureCollectionPositions.flatMap { it.adjacentPositions(true) } -> 4
                            it.position in uncollectedTreasurePositions.flatMap { it.adjacentPositions(true) } -> 5
                            it.position in corePositions -> 6
                            else -> 7
                        }
                    })
            }
        }

    private fun endTurnEarly(withState: GameStateContext): GameAction? =
        withState {
            inPhase<AwaitingPlayerAction> {
                actions.firstOfType<DrawFromTreasureDeck> { true }
            }
        }
}

private val mapEdgePositions = listOf(
    Position(3, 1), Position(4, 1), Position(3, 6), Position(4, 6),
    Position(1, 3), Position(1, 4), Position(6, 3), Position(6, 4)
)

private val corePositions = listOf(
    listOf(   3, 4   ).map { Position(it, 2) },
    listOf(2, 3, 4, 5).map { Position(it, 3) },
    listOf(2, 3, 4, 5).map { Position(it, 4) },
    listOf(   3, 4   ).map { Position(it, 5) }
).flatten()

private inline fun <reified T: Any> List<GameAction>.candidates(predicate: (T) -> Boolean) =
    this.mapNotNull { it as? T }.filter(predicate)

private inline fun <reified T: Any> List<GameAction>.firstOfType(predicate: (T) -> Boolean) =
    candidates(predicate).firstOrNull()

private inline fun <reified T: Any> List<GameAction>.firstOfTypeWithPreference(
    predicate: (T) -> Boolean,
    preferred: (T) -> Boolean
): GameAction? = firstOfTypeWithPreferenceRank(predicate, { if (preferred(it)) 0 else 1 })

private inline fun <reified T: Any> List<GameAction>.firstOfTypeWithPreferenceRank(
    predicate: (T) -> Boolean,
    preferenceRank: (T) -> Int?
): GameAction? {
    return candidates(predicate).toList()
        .mapNotNull { action -> preferenceRank(action)?.let { action to it }  }
        .sortedBy { it.second }
        .firstOrNull()?.first as GameAction?
}

private inline fun <reified T: Any> whenever(predicate: Boolean, f: () -> T?): T? = if (predicate) f() else null
