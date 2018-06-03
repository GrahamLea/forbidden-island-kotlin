package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.*
import com.grahamlea.forbiddenisland.StartingFloodLevel.Novice
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

interface GamePlayer {

    fun newContext(game: Game): GamePlayContext

    interface GamePlayContext {
        fun selectNextAction(): GameAction
    }
}

class RandomGamePlayer(private val random: Random = Random(27697235L)): GamePlayer {

    override fun newContext(game: Game): GamePlayer.GamePlayContext {
        return object: GamePlayer.GamePlayContext {
            override fun selectNextAction(): GameAction =
                game.gameState.availableActions.let { availableActions ->
                    availableActions[random.nextInt(availableActions.size)]
                }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            printGamePlayerTestResult(testGamePlayer(RandomGamePlayer(), 100))
        }
    }
}

class GameTestResult(val gamesPerCategory: Int, gameResults: Map<GameTestCategory, List<GameSummary>>) {

    val gameResults: Map<GameTestCategory, GameSummaries> = gameResults.mapValues { GameSummaries(it.value) }

    val gamesPerFloodLevel = gamesPerCategory * 3 // 2, 3 and 4 players

    data class GameTestCategory(val startingFloodLevel: StartingFloodLevel, val numberOfPlayers: Int): Comparable<GameTestCategory> {
        override fun compareTo(other: GameTestCategory): Int {
            return this.startingFloodLevel.compareTo(other.startingFloodLevel).let {
                if (it != 0) it
                else this.numberOfPlayers.compareTo(other.numberOfPlayers)
            }
        }
    }

    operator fun get(startingFloodLevel: StartingFloodLevel, numberOfPlayers: Int) =
        gameResults.getValue(GameTestCategory(startingFloodLevel, numberOfPlayers))

    operator fun get(startingFloodLevel: StartingFloodLevel) =
        gameResults.filter { it.key.startingFloodLevel == startingFloodLevel }.flatMap { it.value.summaries }

    data class GameSummary(val result: GameResult, val actions: Int)

    inner class GameSummaries(val summaries: List<GameSummary>) {
        fun gamesWonRatio() = summaries.count { it.result == AdventurersWon } / gamesPerCategory
        fun totalActions() = summaries.sumBy { it.actions }
    }
}

fun testGamePlayer(gamePlayer: GamePlayer, gamesPerCategory: Int = 1000): GameTestResult =
    Random(78345763246952L).let { random ->
        StartingFloodLevel.values().flatMap { startingFloodLevel ->
            (2..4).map { numberOfPlayers ->
                GameTestResult.GameTestCategory(startingFloodLevel, numberOfPlayers) to
                    (1..gamesPerCategory).asSequence().map {
                        playGame(gamePlayer, numberOfPlayers, startingFloodLevel, random).let { game ->
                            GameTestResult.GameSummary(game.gameState.result!!, game.gameState.previousActions.size)
                        }
                    }.toList()
            }
        }
    }.let { GameTestResult(gamesPerCategory, it.toMap()) }

fun printGamePlayerTestResult(result: GameTestResult) {
    println("| |${StartingFloodLevel.values().joinToString("|")}|")
    println("|---|---:|---:|---:|---:|")
    printGameWonRatioPerPlayerNumber(result)
    printGameResultBreakdown(result)
    printAverageActions(result)
}

private fun printGameWonRatioPerPlayerNumber(result: GameTestResult) {
    val percentFormat = DecimalFormat("0.0%")
    (2..4).forEach { numberOfPlayers ->
        val results = StartingFloodLevel.values().map { result[it, numberOfPlayers].let { (it.gamesWonRatio()) } }
        println("|$numberOfPlayers players win rate|${results.joinToString("|") { percentFormat.format(it) }}|")
    }
}

private fun printGameResultBreakdown(result: GameTestResult) {
    val percentFormat = DecimalFormat("0.0%")
    val resultTypes = result.gameResults.flatMap { it.value.summaries.map { it.result::class } }.toSortedSet(compareBy { it.simpleName })
    for (resultType in resultTypes) {
        StartingFloodLevel.values()
            .map { level -> result[level].count { resultType.isInstance(it.result) } }
            .map { it.toFloat() / result.gamesPerFloodLevel }
            .let { println("|${resultType.simpleName}|${it.joinToString("|") { percentFormat.format(it) }}|") }
    }
}

private fun printAverageActions(result: GameTestResult) {
    val intFormat = NumberFormat.getIntegerInstance()
    StartingFloodLevel.values()
        .map { level -> (2..4).sumBy { result[level, it].totalActions() } }
        .map { it / StartingFloodLevel.values().count() / result.gamesPerCategory }
        .let { println("|Avg. Actions|${it.joinToString("|") { intFormat.format(it) }}|") }
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
        GameSetup.newRandomGameSetupFor(playerCount, random),
        startingFloodLevel = startingFloodLevel,
        random = random
    )
    logger?.detail("game:\n ${GamePrinter.toString(game)}")

    val gamePlayContext = gamePlayer.newContext(game)

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
