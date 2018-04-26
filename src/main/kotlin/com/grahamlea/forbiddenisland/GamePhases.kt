package com.grahamlea.forbiddenisland

sealed class GamePhase {
    companion object {
        const val maxActionsPerPlayerTurn = 3
        const val treasureDeckCardsDrawnPerTurn = 2
    }
}

data class AwaitingPlayerAction(val player: Adventurer, val actionsRemaining: Int): GamePhase()
data class AwaitingPlayerToDiscardExtraCards(val player: Adventurer, val cardsRemainingToBeDiscarded: Int): GamePhase()
data class AwaitingTreasureDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase()
data class AwaitingFloodDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase()
