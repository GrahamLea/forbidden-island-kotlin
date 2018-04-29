package com.grahamlea.forbiddenisland

sealed class GamePhase {

    abstract fun phaseAfter(event: GameEvent): GamePhase

    companion object {
        const val maxActionsPerPlayerTurn = 3
        const val treasureDeckCardsDrawnPerTurn = 2
    }
}

data class AwaitingPlayerAction(val player: Adventurer, val actionsRemaining: Int): GamePhase() {
    override fun phaseAfter(event: GameEvent): GamePhase {
        return this // TODO: Implement properly
    }
}

data class AwaitingPlayerToDiscardExtraCards(val player: Adventurer, val cardsRemainingToBeDiscarded: Int): GamePhase() {
    override fun phaseAfter(event: GameEvent): GamePhase {
        return this // TODO: Implement properly
    }
}

data class AwaitingTreasureDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun phaseAfter(event: GameEvent): GamePhase {
        return this // TODO: Implement properly
    }
}

data class AwaitingFloodDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun phaseAfter(event: GameEvent): GamePhase {
        return this // TODO: Implement properly
    }
}

object GameOver: GamePhase() {
    override fun phaseAfter(event: GameEvent): GamePhase {
        return this // TODO: Implement properly
    }
}
