package com.grahamlea.forbiddenisland

sealed class GamePhase {

    open fun phaseAfter(event: GameEvent, nextGameState: GameState): GamePhase =
            when {
                this is ForcedOutOfTurnPhase -> calculateNextPhase(event, nextGameState)
                event is OutOfTurnEvent -> this
                else -> calculateNextPhase(event, nextGameState)
            }

    protected abstract fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase

    protected fun invalidEventInPhase(event: GameEvent): Nothing {
        throw IllegalStateException("Not expecting event '$event' during phase '$this'")
    }

    companion object {
        const val maxActionsPerPlayerTurn = 3
        const val treasureDeckCardsDrawnPerTurn = 2
    }
}

data class AwaitingPlayerAction(val player: Adventurer, val actionsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        val nextPhase = when (event) {
            is PlayerActionEvent -> when (actionsRemaining) {
                1 -> AwaitingTreasureDeckDraw(player, treasureDeckCardsDrawnPerTurn)
                else -> AwaitingPlayerAction(player, actionsRemaining - 1)
            }
            is DrawFromTreasureDeck -> AwaitingTreasureDeckDraw(player, treasureDeckCardsDrawnPerTurn - 1)
            else -> invalidEventInPhase(event)
        }

        return nextGameState.playerCardCounts.entries.firstOrNull { it.value > Game.maxCardsHoldablePerPlayer }
                ?.let { AwaitingPlayerToDiscardExtraCard(it.key, nextPhase) }
                ?: nextPhase
    }
}

data class AwaitingPlayerToDiscardExtraCard(val playerWithTooManyCards: Adventurer, val returningToPhase: GamePhase): ForcedOutOfTurnPhase() { // TODO: Move this down
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        if (event !is CardDiscardingEvent || event.playerDiscardingCard != playerWithTooManyCards)
            invalidEventInPhase(event)
        else
            return returningToPhase
    }
}

data class AwaitingTreasureDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        val nextPhase = when (event) {
            is DrawFromTreasureDeck -> when {
                drawsRemaining > 1 -> AwaitingTreasureDeckDraw(player, drawsRemaining - 1)
                else -> AwaitingFloodDeckDraw(player, nextGameState.floodLevel.tilesFloodingPerTurn)
            }
            else -> invalidEventInPhase(event)
        }

        return nextGameState.playerCardCounts.entries.firstOrNull { it.value > Game.maxCardsHoldablePerPlayer }
                ?.let { AwaitingPlayerToDiscardExtraCard(it.key, nextPhase) }
                ?: nextPhase

    }
}

data class AwaitingFloodDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        val nextPhase = when (event) {
            is DrawFromFloodDeck -> when {
                drawsRemaining > 1 -> AwaitingFloodDeckDraw(player, drawsRemaining - 1)
                else -> {
                    val nextPlayer =
                            if (nextGameState.gameSetup.players.last() == player) nextGameState.gameSetup.players.first()
                            else nextGameState.gameSetup.players[nextGameState.gameSetup.players.indexOf(player) + 1]
                    AwaitingPlayerAction(nextPlayer, maxActionsPerPlayerTurn)
                }
            }
            else -> invalidEventInPhase(event)
        }

        val sunkPlayers = nextGameState.sunkPlayers.sorted() // Sorting just introduces determinism for the test
        return if (sunkPlayers.isEmpty()) nextPhase
        else sunkPlayers.fold(initial = nextPhase) { phase, player -> AwaitingPlayerToSwimToSafety(player, phase) }
    }
}

sealed class ForcedOutOfTurnPhase: GamePhase()

data class AwaitingPlayerToSwimToSafety(val player: Adventurer, val returningToPhase: GamePhase): ForcedOutOfTurnPhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        if (event !is SwimToSafety || event.strandedPlayer != player)
            invalidEventInPhase(event)
        else
            return returningToPhase
    }
}

object GameOver: GamePhase() {
    override fun phaseAfter(event: GameEvent, nextGameState: GameState): GamePhase { invalidEventInPhase(event) }
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase { throw IllegalStateException() }
}
