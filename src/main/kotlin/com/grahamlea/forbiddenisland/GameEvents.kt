package com.grahamlea.forbiddenisland

sealed class GameEvent

sealed class PlayerActionEvent: GameEvent()

data class Move(val player: Adventurer, val mapSite: MapSite): PlayerActionEvent() {
    override fun toString() = "$player moves to $mapSite"
}

data class ShoreUp(val player: Adventurer, val mapSite: MapSite, val mapSite2: MapSite? = null): PlayerActionEvent() {
    override fun toString() =
        "$mapSite ${if (mapSite2 == null) "is" else "and $mapSite2 are "} shored up by $player"
}

data class GiveTreasureCard(val player: Adventurer, val receiver: Adventurer, val cards: List<TreasureCard>): PlayerActionEvent() {
    override fun toString() = "$player gives ${cards} to $receiver"
}

data class CaptureTreasure(val treasure: Treasure): PlayerActionEvent() {
    override fun toString() = "$treasure is captured"
}

sealed class PlayerSpecialActionEvent: GameEvent()

data class HelicopterLift(val player: Adventurer, val mapSite: MapSite): PlayerSpecialActionEvent() {
    override fun toString() = "$player is helicopter lifted to $mapSite"
}

data class Sandbag(val player: Adventurer, val mapSite: MapSite): PlayerSpecialActionEvent() {
    override fun toString() = "$mapSite is sand bagged by $player"
}

object HelicopterLiftOffIsland: PlayerSpecialActionEvent() {
    override fun toString() = "All players are lifted off the island"
}

sealed class PlayerObligationEvent: GameEvent()

class DrawFromTreasureDeck(val player: Adventurer): PlayerObligationEvent()

object DrawFromFloodDeck: PlayerObligationEvent()

data class Swim(val strandedPlayer: Adventurer, val mapSite: MapSite): PlayerObligationEvent()
