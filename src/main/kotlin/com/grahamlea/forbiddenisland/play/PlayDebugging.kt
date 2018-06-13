package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.*
import java.util.*

typealias Selector = String

interface ExplainingGamePlayer: GamePlayer {

    override fun newContext(game: Game, random: Random): ExplainingGamePlayContext

    abstract class ExplainingGamePlayContext: GamePlayer.GamePlayContext {
        abstract fun selectNextActionWithSelector(): Pair<GameAction, Selector>
        final override fun selectNextAction() = selectNextActionWithSelector().first
    }
}

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

typealias DebugPredicate =
    (
        gameState: GameState,
        selectedAction: GameAction,
        selector: Selector,
        previousAction: GameAction?,
        previousSelector: Selector?
    ) -> Boolean

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
