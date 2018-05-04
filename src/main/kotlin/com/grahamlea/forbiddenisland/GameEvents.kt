package com.grahamlea.forbiddenisland

sealed class GameEvent {
    companion object {
        val allPossibleEvents: List<GameEvent> by lazy {
            val players = Adventurer.values().sorted()
            val positions = Position.allPositions.sorted()
            val treasures = Treasure.values().sorted()
            val cardTypes = HoldableCard.allCardTypes.sorted()

            listOf(
                generateAll(players, positions) { Move(first, second) },
                generateAll(players, positions) { ShoreUp(first, second) },
                generateAll(positions, positions)
                    { ShoreUp(Adventurer.Engineer, first, second) }.filterNot { it.position >= it.position2!! },
                generateAll(players, players, treasures)
                    { GiveTreasureCard(first, second, TreasureCard(third)) }.filterNot { it.player == it.receiver },
                generateAll(players, players, positions) { HelicopterLift(first, second, third) },
                generateAll(players, positions) { Sandbag(first, second) },
                generateAll(players, positions) { SwimToSafety(first, second) },
                generateAll(players, cardTypes.filterNot { it === WatersRiseCard }) { DiscardCard(first, second) },
                players.map { HelicopterLiftOffIsland(it) },
                players.map { DrawFromTreasureDeck(it) },
                players.map { DrawFromFloodDeck(it) }
            ).flatten()
        }

        private fun <A, B, T: GameEvent> generateAll(o1s: Iterable<A>, o2s: Iterable<B>, function: Pair<A, B>.() -> T): List<T> =
            o1s.flatMap { o1 -> o2s.map { o2 -> with(Pair(o1, o2), function) } }

        private fun <A, B, C, T: GameEvent> generateAll(o1s: Iterable<A>, o2s: Iterable<B>, o3s: Iterable<C>, function: Triple<A, B, C>.() -> T): List<T> =
            o1s.flatMap { o1 -> o2s.flatMap { o2 -> o3s.map { o3 -> with(Triple(o1, o2, o3), function) } } }

        @JvmStatic
        fun main(args: Array<String>) {
            println("allPossibleEvents:\n${allPossibleEvents.joinToString("\n")}")
        }
    }
}

sealed class PlayerActionEvent: GameEvent()

interface CardDiscardingEvent {
    val playerDiscardingCard: Adventurer
}

data class Move(val player: Adventurer, val position: Position): PlayerActionEvent() {
    override fun toString() = "$player moves to $position"
}

data class ShoreUp(val player: Adventurer, val position: Position, val position2: Position? = null): PlayerActionEvent() {
    override fun toString() =
        "$position ${if (position2 == null) "is" else "and $position2 are"} shored up by $player"
}

data class GiveTreasureCard(val player: Adventurer, val receiver: Adventurer, val card: TreasureCard): PlayerActionEvent() {
    override fun toString() = "$player gives one '$card' card to $receiver"
}

data class CaptureTreasure(val player:Adventurer, val treasure: Treasure): PlayerActionEvent() {
    override fun toString() = "$treasure is captured by $player"
}

sealed class OutOfTurnEvent: GameEvent()

sealed class PlayerSpecialActionEvent: OutOfTurnEvent()

data class HelicopterLift(val playerWithCard: Adventurer, val playerBeingMoved: Adventurer, val position: Position):
        PlayerSpecialActionEvent(), CardDiscardingEvent {
    override val playerDiscardingCard = playerWithCard
    override fun toString() = "$playerBeingMoved is helicopter lifted to $position by $playerWithCard"
}

data class Sandbag(val player: Adventurer, val position: Position): PlayerSpecialActionEvent(), CardDiscardingEvent {
    override val playerDiscardingCard = player
    override fun toString() = "$position is sand bagged by $player"
}

data class SwimToSafety(val strandedPlayer: Adventurer, val position: Position): OutOfTurnEvent() {
    override fun toString() = "$strandedPlayer swims to safety at $position"
}

data class DiscardCard(val player: Adventurer, val card: HoldableCard): OutOfTurnEvent(), CardDiscardingEvent {
    override val playerDiscardingCard = player
    override fun toString() = "$player discards $card"
}

data class HelicopterLiftOffIsland(val player: Adventurer): PlayerSpecialActionEvent() {
    override fun toString() = "All players are lifted off the island by $player"
}

sealed class PlayerObligationEvent: GameEvent()

data class DrawFromTreasureDeck(val player: Adventurer): PlayerObligationEvent() {
    override fun toString() = "$player draws from the Treasure Deck"
}

data class DrawFromFloodDeck(val player: Adventurer): PlayerObligationEvent() {
    override fun toString() = "$player draws from the Flood Deck"
}

