package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.*
import com.grahamlea.forbiddenisland.StartingFloodLevel.Novice
import java.text.DecimalFormat
import java.util.*

interface GamePlayer {
    fun selectAction(game: Game): GameAction
}

class RandomGamePlayer(private val random: Random = Random()): GamePlayer {
    override fun selectAction(game: Game): GameAction =
        game.gameState.availableActions.let { availableActions ->
            availableActions[random.nextInt(availableActions.size)]
        }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            printGamePlayerTestResult(testGamePlayer(RandomGamePlayer(), 20))
        }
    }
}

fun testGamePlayer(gamePlayer: GamePlayer, gamesPerCategory: Int = 1000): Map<Pair<Int, StartingFloodLevel>, Pair<Int, Int>> =
    (2..4).flatMap { numberOfPlayers -> StartingFloodLevel.values().map { startingFloodLevel ->
        Pair(numberOfPlayers, startingFloodLevel) to
            Pair(
                (1..gamesPerCategory).asSequence().map {
                    playGame(gamePlayer, numberOfPlayers, startingFloodLevel, Random(78345763246952L)).gameState.result
                }.count { it == AdventurersWon },
                gamesPerCategory
            )
    } }.toMap()

fun printGamePlayerTestResult(result: Map<Pair<Int, StartingFloodLevel>, Pair<Int, Int>>) {
    val format = DecimalFormat("0.0%")
    println("|Number of Players|${StartingFloodLevel.values().joinToString("|")}|")
    println("|---|---:|---:|---:|---:|")
    (2..4).forEach { numberOfPlayers ->
        val results = StartingFloodLevel.values().map { result.getValue(Pair(numberOfPlayers, it)).let { (it.first.toFloat() / it.second) } }
        println("|$numberOfPlayers|${results.joinToString("|") { format.format(it) } }|")
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

    val game = Game.newRandomGameFor(
        GameSetup.newRandomGameSetupFor(playerCount),
        startingFloodLevel = startingFloodLevel,
        random = random
    )
    logger?.detail("game:\n ${GamePrinter.toString(game)}")

    var numberOfActions = 0

    while (game.gameState.result == null) {
        logger?.detail(game.gameState.phase.toString())

        val action = gamePlayer.selectAction(game)
        logger?.detail("    $action")

        game.process(action)
        numberOfActions++
    }
    logger?.detail("numberOfActions = ${numberOfActions}")
    logger?.info("result = ${game.gameState.result}")

    return game
}
