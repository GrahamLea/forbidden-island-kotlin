package com.grahamlea.forbiddenisland

sealed class GameEvent

sealed class PlayerActionEvent: GameEvent()

interface CardDiscardingEvent {
    val playerDiscardingCard: Adventurer
}

data class Move(val player: Adventurer, val position: Position): PlayerActionEvent() {
    override fun toString() = "$player moves to $position"
}

data class ShoreUp(val player: Adventurer, val position: Position, val position2: Position? = null): PlayerActionEvent() {
    override fun toString() =
        "$position ${if (position2 == null) "is" else "and $position2 are "} shored up by $player"
}

data class GiveTreasureCard(val player: Adventurer, val receiver: Adventurer, val card: TreasureCard): PlayerActionEvent() {
    override fun toString() = "$player gives $card to $receiver"
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
    override fun toString() = "$strandedPlayer swims to $position"
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

