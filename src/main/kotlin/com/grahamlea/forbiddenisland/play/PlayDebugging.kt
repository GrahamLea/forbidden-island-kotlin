package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.*
import java.util.*

typealias Selector = String

/**
 * An extension of [GamePlayer] for implementations that wish to be able to be [stepped through][stepThroughGame] or
 * [conditionally debugged][debugGame].
 */
interface ExplainingGamePlayer: GamePlayer {

    override fun newContext(game: Game, deterministicRandomForGamePlayerDecisions: Random): ExplainingGamePlayContext

    abstract class ExplainingGamePlayContext: GamePlayer.GamePlayContext {
        /**
         * Selects and returns the next [action][GameAction] to be played on the [Game] that was given to this context
         * at [the time of its creation][newContext], along with a short description of the condition that led to the
         * action being selected.
         */
        abstract fun selectNextActionWithSelector(): Pair<GameAction, Selector>

        final override fun selectNextAction() = selectNextActionWithSelector().first
    }
}

/**
 * Step through [game] as played by [gamePlayer], printing the game state to [System.out] at each step and requiring
 * a newline to be typed in order for play to progress.
 */
fun stepThroughGame(game: Game, gamePlayer: ExplainingGamePlayer, random: Random = Random()) {
    val gamePlayContext = gamePlayer.newContext(game, random)
    val scanner = Scanner(System.`in`)
    while (game.gameState.result == null) {
        val (action, selector) = gamePlayContext.selectNextActionWithSelector()
        printGameState(game, action, selector)
        print(">")
        scanner.nextLine()
        game.process(action)
    }
    println(game.gameState)
    println("Game Result: ${game.gameState.result}")
}

/**
 * A predicate used to determine whether an
 * [action selection][ExplainingGamePlayer.ExplainingGamePlayContext.selectNextActionWithSelector]
 * should result in the GameState being debugged.
 *
 * @see debugGame
 */
typealias DebugPredicate =
    (
        gameState: GameState,
        selectedAction: GameAction,
        selector: Selector,
        previousAction: GameAction?,
        previousSelector: Selector?
    ) -> Boolean

/**
 * Run [game] as played by [gamePlayer], printing the game state to [System.out] whenever [debugPredicate] returns true.
 */
fun debugGame(game: Game, gamePlayer: ExplainingGamePlayer, debugPredicate: DebugPredicate, random: Random = Random()) {
    val gamePlayContext = gamePlayer.newContext(game, random)
    var previousAction: GameAction? = null
    var previousSelector: Selector? = null
    while (game.gameState.result == null) {
        val (action, selector) = gamePlayContext.selectNextActionWithSelector()
        if (debugPredicate(game.gameState, action, selector, previousAction, previousSelector)) {
            printGameState(game, action, selector)
        }
        game.process(action)
        previousAction = action
        previousSelector = selector
    }
    println(game.gameState)
    println("Game Result: ${game.gameState.result}")
}

private fun printGameState(game: Game, selectedAction: GameAction, selector: Selector) {
    synchronized(System.out) {
        println()
        println(game.gameState)
        println("Available actions: ${game.gameState.availableActions.filterNot { it is HelicopterLift || it is Sandbag }}")
        game.gameState.availableActions.filter { it is HelicopterLift }.takeIf { it.any() }?.let {
            println("    + ${it.size} HelicopterLifts")
        }
        game.gameState.availableActions.filter { it is Sandbag }.takeIf { it.any() }?.let {
            println("    + ${it.size} Sandbags")
        }
        println()
        println("Selected action: $selectedAction")
        println("Selector: ${selector}")
        println("=".repeat(90))
    }
}
