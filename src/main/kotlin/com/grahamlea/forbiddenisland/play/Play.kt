package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.*
import com.grahamlea.forbiddenisland.StartingFloodLevel.Novice
import java.util.*

/**
 * Interface for objects capable of playing [Game]s of Forbidden Island.
 *
 * GamePlayer implementations should only make deterministic decisions or, when random decisions are preferred, use the
 * [random provided when the context is created][newContext] to ensure deterministic randomness across executions.
 */
interface GamePlayer {

    /**
     * Create a new context for an individual game.
     * The [GamePlayContext] should retain a reference to the [game].
     *
     * @param deterministicRandomForGamePlayerDecisions a random to be used in the event that the implementation wants
     * to make a randomised decision.
     */
    fun newContext(game: Game, deterministicRandomForGamePlayerDecisions: Random): GamePlayContext

    /**
     * Interface for an object that selects the next action to be played on a [Game] of Forbidden Island.
     *
     * @see [newContext]
     */
    interface GamePlayContext {
        /**
         * Selects and returns the next [action][GameAction] to be played on the [Game] that was given to this context
         * at [the time of its creation][newContext].
         */
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

/**
 * Plays one [Game] of Forbidden Island using [gamePlayer].
 */
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
