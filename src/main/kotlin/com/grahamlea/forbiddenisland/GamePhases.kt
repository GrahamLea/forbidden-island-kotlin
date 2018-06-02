package com.grahamlea.forbiddenisland

/**
 * Represents the phase of play that the current game is in, i.e. whether a player should be taking an action next,
 * or a card needs to be drawn, etc.
 *
 * @see AwaitingPlayerAction
 * @see AwaitingTreasureDeckDraw
 * @see AwaitingFloodDeckDraw
 * @see AwaitingPlayerToDiscardExtraCard
 * @see AwaitingPlayerToSwimToSafety
 * @see GameOver
 */
sealed class GamePhase {

    /**
     * Calculates the next [GamePhase] after this phase based on the given [action] having just been applied to the
     * current [GameState], which resulted in the [nextGameState].
     *
     * @param nextGameState the next GameState that is about to be updated in the [Game], calculated by applying the
     * [action] to the current GameState, with the exception that [GameState.phase] has obviously not been updated.
     */
    open fun phaseAfter(action: GameAction, nextGameState: GameState): GamePhase =
            when {
                this is ForcedOutOfTurnPhase -> calculateNextPhase(action, nextGameState)
                action is OutOfTurnAction -> this
                else -> calculateNextPhase(action, nextGameState)
            }

    protected abstract fun calculateNextPhase(action: GameAction, nextGameState: GameState): GamePhase

    protected fun invalidActionInPhase(action: GameAction): Nothing {
        throw IllegalStateException("Not expecting action '$action' during phase '$this'")
    }

    companion object {
        const val maxActionsPerPlayerTurn = 3
        const val treasureDeckCardsDrawnPerTurn = 2
    }
}

data class AwaitingPlayerAction(val player: Adventurer, val actionsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(action: GameAction, nextGameState: GameState): GamePhase {
        val nextPhase = when (action) {
            is PlayerAction -> when (actionsRemaining) {
                1 -> AwaitingTreasureDeckDraw(player, treasureDeckCardsDrawnPerTurn)
                else -> AwaitingPlayerAction(player, actionsRemaining - 1)
            }
            is DrawFromTreasureDeck -> AwaitingTreasureDeckDraw(player, treasureDeckCardsDrawnPerTurn - 1)
            else -> invalidActionInPhase(action)
        }

        return nextGameState.playerCardCounts.entries.firstOrNull { it.value > Game.maxCardsHoldablePerPlayer }
                ?.let { AwaitingPlayerToDiscardExtraCard(it.key, nextPhase) }
                ?: nextPhase
    }
}

data class AwaitingTreasureDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(action: GameAction, nextGameState: GameState): GamePhase {
        val nextPhase = when (action) {
            is DrawFromTreasureDeck -> when {
                drawsRemaining > 1 -> AwaitingTreasureDeckDraw(player, drawsRemaining - 1)
                else -> AwaitingFloodDeckDraw(player, nextGameState.floodLevel.tilesFloodingPerTurn)
            }
            else -> invalidActionInPhase(action)
        }

        return nextGameState.playerCardCounts.entries.firstOrNull { it.value > Game.maxCardsHoldablePerPlayer }
                ?.let { AwaitingPlayerToDiscardExtraCard(it.key, nextPhase) }
                ?: nextPhase

    }
}

data class AwaitingFloodDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(action: GameAction, nextGameState: GameState): GamePhase {
        val nextPhase = when (action) {
            is DrawFromFloodDeck -> when {
                drawsRemaining > 1 -> AwaitingFloodDeckDraw(player, drawsRemaining - 1)
                else -> {
                    val nextPlayer =
                            if (nextGameState.gameSetup.players.last() == player) nextGameState.gameSetup.players.first()
                            else nextGameState.gameSetup.players[nextGameState.gameSetup.players.indexOf(player) + 1]
                    AwaitingPlayerAction(nextPlayer, maxActionsPerPlayerTurn)
                }
            }
            else -> invalidActionInPhase(action)
        }

        val sunkPlayers = nextGameState.sunkPlayers.sorted() // Sorting just introduces determinism for the test
        return if (sunkPlayers.isEmpty()) nextPhase
        else sunkPlayers.fold(initial = nextPhase) { phase, player -> AwaitingPlayerToSwimToSafety(player, phase) }
    }
}

sealed class ForcedOutOfTurnPhase: GamePhase()

data class AwaitingPlayerToDiscardExtraCard(val playerWithTooManyCards: Adventurer, val returningToPhase: GamePhase): ForcedOutOfTurnPhase() {
    override fun calculateNextPhase(action: GameAction, nextGameState: GameState): GamePhase {
        if (action !is CardDiscardingAction || action.playerDiscardingCard != playerWithTooManyCards)
            invalidActionInPhase(action)
        else
            return returningToPhase
    }
}

data class AwaitingPlayerToSwimToSafety(val player: Adventurer, val returningToPhase: GamePhase): ForcedOutOfTurnPhase() {
    override fun calculateNextPhase(action: GameAction, nextGameState: GameState): GamePhase {
        if (action !is SwimToSafety || action.player != player)
            invalidActionInPhase(action)
        else
            return returningToPhase
    }
}

object GameOver: GamePhase() {
    override fun phaseAfter(action: GameAction, nextGameState: GameState): GamePhase { invalidActionInPhase(action) }
    override fun calculateNextPhase(action: GameAction, nextGameState: GameState): GamePhase { throw IllegalStateException() }
    override fun toString() = "GameOver"
}
