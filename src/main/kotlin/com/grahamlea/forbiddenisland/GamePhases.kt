package com.grahamlea.forbiddenisland

sealed class GamePhase {

    open fun phaseAfter(event: GameEvent, nextGameState: GameState): GamePhase =
            when {
                this is AwaitingPlayerToDiscardExtraCard -> calculateNextPhase(event, nextGameState)
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

data class AwaitingPlayerToDiscardExtraCard(val playerWithTooManyCards: Adventurer, val followingPhase: GamePhase): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        if (event !is CardDiscardingEvent)
            throw IllegalStateException("$event does not discard a card")
        else if (event.playerDiscardingCard != playerWithTooManyCards)
            throw IllegalStateException("$playerWithTooManyCards must discard a card, not ${event.playerDiscardingCard}")
        else
            return followingPhase
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
        return when (event) {
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
    }
}

data class AwaitingPlayerToSwim(val player: Adventurer): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        TODO() // TODO
    }
}

object GameOver: GamePhase() {
    override fun phaseAfter(event: GameEvent, nextGameState: GameState): GamePhase { invalidEventInPhase(event) }
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase { throw IllegalStateException() }
}
