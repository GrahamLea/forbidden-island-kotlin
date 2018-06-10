package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Pilot

/**
 * Represents an _action_ that can occur in a [Game], being a user-driven change to the game.
 *
 * Note that:
 * * the Forbidden Island rules often use 'action' to mean a more specific subset of actions, being those a player
 * takes during their turn.
 * * some actions made by physical players in the real game are not represented here as actions but instead are made
 * automatically by the [GameState] while processing a [GameAction], e.g. discarding cards that have been used,
 * shuffling discard decks back to draw decks upon depletion.
 *
 * @see Game.process
 * @see Move
 * @see Fly
 * @see ShoreUp
 * @see GiveTreasureCard
 * @see CaptureTreasure
 * @see DrawFromTreasureDeck
 * @see DrawFromFloodDeck
 * @see HelicopterLift
 * @see Sandbag
 * @see SwimToSafety
 * @see DiscardCard
 * @see HelicopterLiftOffIsland
 */
sealed class GameAction {
    companion object {

        private val players = Adventurer.values().sorted()

        /**
         * Returns a list of every single action that could ever happen in any game.
         */
        val ALL_POSSIBLE_ACTIONS: List<GameAction> by lazy {
            val allPlayerCombinations: List<ImmutableSet<Adventurer>> = (1..4).flatMap { setsOfPlayersOfLength(it) }
            val positions = Position.allPositions.sorted()
            val treasures = Treasure.values().sorted()
            val cardTypes = HoldableCard.allCardTypes.sorted()

            listOf(
                generateAll(players, positions) { Move(first, second) },
                generateAll(players, positions) { ShoreUp(first, second) },
                generateAll(positions, positions)
                    { ShoreUp(Engineer, first, second) }.filterNot { it.position >= it.position2!! },
                generateAll(players, players, treasures)
                    { GiveTreasureCard(first, second, TreasureCard(third)) }.filterNot { it.player == it.receiver },
                generateAll(players, allPlayerCombinations, positions) { HelicopterLift(first, second, third) },
                generateAll(players, positions) { Sandbag(first, second) },
                generateAll(players, positions) { SwimToSafety(first, second) },
                generateAll(players, cardTypes.filterNot { it === WatersRiseCard }) { DiscardCard(first, second) },
                players.map { HelicopterLiftOffIsland(it) },
                players.map { DrawFromTreasureDeck(it) },
                players.map { DrawFromFloodDeck(it) }
            ).flatten()
        }

        private fun setsOfPlayersOfLength(length: Int): List<ImmutableSet<Adventurer>> {
            return (1..63).map { it.toString(2).padStart(6, '0') }.filter { it.count { it == '1' } == length }.reversed()
                .map { it.mapIndexedNotNull { i, c -> if (c == '1') i else null }.map { players[it] }.toSortedSet().imm() }
        }

        private fun <A, B, T: GameAction> generateAll(o1s: Iterable<A>, o2s: Iterable<B>, function: Pair<A, B>.() -> T): List<T> =
            o1s.flatMap { o1 -> o2s.map { o2 -> with(Pair(o1, o2), function) } }

        private fun <A, B, C, T: GameAction> generateAll(o1s: Iterable<A>, o2s: Iterable<B>, o3s: Iterable<C>, function: Triple<A, B, C>.() -> T): List<T> =
            o1s.flatMap { o1 -> o2s.flatMap { o2 -> o3s.map { o3 -> with(Triple(o1, o2, o3), function) } } }
    }
}

sealed class PlayerAction: GameAction()

interface PlayerMovingAction {
    val player: Adventurer
    val position: Position
}

interface CardDiscardingAction {
    val playerDiscardingCard: Adventurer
    val discardedCards: ImmutableList<HoldableCard>
}

data class Move(override val player: Adventurer, override val position: Position): PlayerAction(), PlayerMovingAction {
    override fun toString() = "$player moves to $position"
}

data class Fly(override val player: Adventurer, override val position: Position): PlayerAction(), PlayerMovingAction {
    init { require(player == Pilot) { "Only the Pilot is able to fly" } }
    override fun toString() = "$player flies to $position"
}

data class ShoreUp(val player: Adventurer, val position: Position, val position2: Position? = null): PlayerAction() {
    override fun toString() =
        "$position ${if (position2 == null) "is" else "and $position2 are"} shored up by $player"
}

data class GiveTreasureCard(val player: Adventurer, val receiver: Adventurer, val card: TreasureCard): PlayerAction() {
    override fun toString() = "$player gives one '$card' card to $receiver"
}

data class CaptureTreasure(val player:Adventurer, val treasure: Treasure): PlayerAction(), CardDiscardingAction {
    override val playerDiscardingCard = player
    override val discardedCards = (TreasureCard(treasure) * 4).imm()
    override fun toString() = "$treasure is captured by $player"
}

sealed class PlayerObligationAction: GameAction()

data class DrawFromTreasureDeck(val player: Adventurer): PlayerObligationAction() {
    override fun toString() = "$player draws from the Treasure Deck"
}

data class DrawFromFloodDeck(val player: Adventurer): PlayerObligationAction() {
    override fun toString() = "$player draws from the Flood Deck"
}

interface OutOfTurnAction

data class DiscardCard(val player: Adventurer, val card: HoldableCard): PlayerObligationAction(), OutOfTurnAction, CardDiscardingAction {
    override val playerDiscardingCard = player
    override val discardedCards = immListOf(card)
    override fun toString() = "$player discards $card"
}

data class SwimToSafety(override val player: Adventurer, override val position: Position): PlayerObligationAction(), OutOfTurnAction, PlayerMovingAction {
    override fun toString() = "$player swims to safety at $position"
}

sealed class PlayerSpecialAction: GameAction(), OutOfTurnAction

data class HelicopterLift(val playerWithCard: Adventurer, val playersBeingMoved: Set<Adventurer>, val position: Position):
        PlayerSpecialAction(), CardDiscardingAction {

    override val playerDiscardingCard = playerWithCard
    override val discardedCards = immListOf(HelicopterLiftCard)
    override fun toString() =
        (if (playersBeingMoved.size == 1) "${playersBeingMoved.first()} is" else "${playersBeingMoved.joinToString(" and ")} are") +
            " helicopter lifted to $position by $playerWithCard"
}

data class Sandbag(val player: Adventurer, val position: Position): PlayerSpecialAction(), CardDiscardingAction {
    override val playerDiscardingCard = player
    override val discardedCards = immListOf(SandbagsCard)
    override fun toString() = "$position is sand bagged by $player"
}

data class HelicopterLiftOffIsland(val player: Adventurer): PlayerObligationAction(), CardDiscardingAction {
    override val playerDiscardingCard = player
    override val discardedCards = immListOf(HelicopterLiftCard)
    override fun toString() = "All players are lifted off the island by $player"
}
