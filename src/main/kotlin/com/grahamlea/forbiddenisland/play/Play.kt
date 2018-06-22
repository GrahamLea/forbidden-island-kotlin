package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.*
import com.grahamlea.forbiddenisland.StartingFloodLevel.Novice
import java.util.*

interface GamePlayer {

    fun newContext(game: Game, deterministicRandomForGamePlayerDecisions: Random): GamePlayContext

    interface GamePlayContext {
        fun selectNextAction(): GameAction
    }
}

interface Logger {
    fun info(s: String)
    fun detail(s: String)
}

class ConsoleLogger(val printDetail: Boolean = false): Logger {
    override fun info(s: String) {
        println(s)
    }

    override fun detail(s: String) {
        if (printDetail) {
            synchronized(System.out) {
                print("   ")
                println(s)
            }
        }
    }
}

fun playGame(
    gamePlayer: GamePlayer,
    numberOfPlayers: Int? = null,
    startingFloodLevel: StartingFloodLevel = Novice,
    random: Random = Random(),
    logger: Logger? = null
): Game {

    val playerCount = numberOfPlayers ?: 2 + random.nextInt(3)
    logger?.detail("numberOfPlayers = ${numberOfPlayers}")

    val gameContextRandom = Random(random.nextLong())

    val game = Game.newRandomGameFor(
        GameSetup.newRandomGameSetupFor(playerCount, random),
        startingFloodLevel = startingFloodLevel,
        random = random
    )
    logger?.detail("game:\n ${GamePrinter.toString(game)}")

    val gamePlayContext = gamePlayer.newContext(game, gameContextRandom)

    var numberOfActions = 0

    while (game.gameState.result == null) {
        logger?.detail(game.gameState.phase.toString())

        val action = gamePlayContext.selectNextAction()
        logger?.detail("    $action")

        game.process(action)
        numberOfActions++
    }
    logger?.detail("numberOfActions = ${numberOfActions}")
    logger?.info("result = ${game.gameState.result}")

    return game
}
