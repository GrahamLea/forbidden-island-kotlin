package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.AdventurersWon
import com.grahamlea.forbiddenisland.GameResult
import com.grahamlea.forbiddenisland.PlayerObligationAction
import com.grahamlea.forbiddenisland.StartingFloodLevel
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

private const val gamePlayerTestSeedGeneratorSeed = 78345763246952L

private val startingSeedsByCategory = Random(gamePlayerTestSeedGeneratorSeed).let { seedGenerator ->
    StartingFloodLevel.values().flatMap { startingFloodLevel ->
        (2..4).map { numberOfPlayers -> Pair(startingFloodLevel, numberOfPlayers) to seedGenerator.nextLong() }
    }.toMap()
}

/**
 * Interface for a [GamePlayer] that wants to be notified when testing has completed.
 */
interface TestingAware {
    fun testingComplete() { }
}

/**
 * Tests [gamePlayer] by running many games and returning a summary of the results.
 *
 * Tests are run in multiple threads to increase throughput.
 * Expect your CPU to get smashed when invoking this function with large numbers of tests.
 *
 * The total number of tests run is `numbersOfPlayers.size * startingFloodLevels.size * gamesPerCategory`.
 *
 * Results should be deterministic on the same machine and across multiple machines with the same number of
 * [available processors][Runtime.availableProcessors].
 *
 * @param gamesPerCategory The number of games to play for every numberOfPlayers/StartingFloodLevel combination
 * @param numbersOfPlayers A list of numbers of players for which to test the [gamePlayer]
 * @param startingFloodLevels A list of starting flood levels for which to test the [gamePlayer]
 */
fun testGamePlayer(
    gamePlayer: GamePlayer,
    gamesPerCategory: Int = 1000,
    numbersOfPlayers: Iterable<Int> = (2..4),
    startingFloodLevels: Iterable<StartingFloodLevel> = StartingFloodLevel.values().toList()
): GamePlayerTestResult {
    val gamesPerTask = 100
    val numberOfThreads = max(1, Runtime.getRuntime().availableProcessors() - 1)
    println("Testing ${gamePlayer::class.qualifiedName?.split('.')?.takeLast(2)?.joinToString(".")} with $numberOfThreads threads")
    val fixedThreadPool = Executors.newFixedThreadPool(numberOfThreads)
    val tasks = startingFloodLevels.flatMap { startingFloodLevel ->
        numbersOfPlayers.flatMap { numberOfPlayers ->
            val categorySeedGenerator = Random(startingSeedsByCategory.getValue(Pair(startingFloodLevel, numberOfPlayers)))
            (0 until gamesPerCategory step gamesPerTask).map { firstGameNumber ->
                val deterministicRandomForTask = Random(categorySeedGenerator.nextLong())
                val games = min(gamesPerTask, gamesPerCategory - firstGameNumber)
                Callable {
                    runGamePlayerTest(startingFloodLevel, numberOfPlayers, games, gamePlayer, deterministicRandomForTask)
                }
            }
        }
    }
    val results = fixedThreadPool.invokeAll(tasks)
    fixedThreadPool.shutdown()
    (gamePlayer as? TestingAware)?.testingComplete()
    return GamePlayerTestResult(gamesPerCategory,
        results.map { it.get() }.groupingBy { it.first }.aggregate { _, acc, it, first ->
            if (first) it.second else acc!! + it.second
        }
    )
}

private fun runGamePlayerTest(
    startingFloodLevel: StartingFloodLevel,
    numberOfPlayers: Int,
    gamesPerCategory: Int,
    gamePlayer: GamePlayer,
    gameSeedGenerator: Random
): Pair<GamePlayerTestResult.GameTestCategory, List<GamePlayerTestResult.GameSummary>> {
    return GamePlayerTestResult.GameTestCategory(startingFloodLevel, numberOfPlayers) to
        (1..gamesPerCategory).asSequence().map {
            playGame(gamePlayer, numberOfPlayers, startingFloodLevel, Random(gameSeedGenerator.nextLong())).let { game ->
                GamePlayerTestResult.GameSummary(
                    game.gameState.result!!,
                    game.gameState.previousActions.filter { it !is PlayerObligationAction }.size,
                    game.gameState.treasuresCollected.count { it.value == true }
                )
            }
        }.toList()
}

class GamePlayerTestResult(val gamesPerCategory: Int, gameResults: Map<GameTestCategory, List<GameSummary>>) {

    val gameResults: Map<GameTestCategory, GameSummaries> = gameResults.mapValues { GameSummaries(it.value) }

    val numbersOfPlayers = gameResults.keys.map { it.numberOfPlayers }.distinct().sorted()
    val startingFloodLevels = gameResults.keys.map { it.startingFloodLevel }.distinct().sorted()

    val gamesPerFloodLevel = gamesPerCategory * numbersOfPlayers.size

    data class GameTestCategory(val startingFloodLevel: StartingFloodLevel, val numberOfPlayers: Int): Comparable<GameTestCategory> {
        override fun compareTo(other: GameTestCategory): Int {
            return this.startingFloodLevel.compareTo(other.startingFloodLevel).let {
                if (it != 0) it
                else this.numberOfPlayers.compareTo(other.numberOfPlayers)
            }
        }
    }

    operator fun get(startingFloodLevel: StartingFloodLevel, numberOfPlayers: Int): GameSummaries =
        gameResults.getValue(GameTestCategory(startingFloodLevel, numberOfPlayers))

    operator fun get(startingFloodLevel: StartingFloodLevel): GameSummaries =
        GameSummaries(gameResults.filter { it.key.startingFloodLevel == startingFloodLevel }.flatMap { it.value.summaries })

    data class GameSummary(val result: GameResult, val actions: Int, val treasuresCaptured: Int)

    inner class GameSummaries(val summaries: List<GameSummary>) {
        fun gamesWonRatio() = summaries.count { it.result == AdventurersWon } / gamesPerCategory.toFloat()
        fun totalActions() = summaries.sumBy { it.actions }
        fun totalTreasuresCaptured() = summaries.sumBy { it.treasuresCaptured }
    }
}

/**
 * Print a summary of the results of a [game player test][testGamePlayer] to [System.out].
 */
fun printGamePlayerTestResult(result: GamePlayerTestResult) {
    println("| |${result.startingFloodLevels.joinToString("|")}|")
    println("|---|${"---:|".repeat(result.startingFloodLevels.size)}")
    printGameWonRatioPerPlayerNumber(result)
    printGameResultBreakdown(result)
    printAverageTreasuresCaptured(result)
    printAverageActions(result)
}

private fun printGameWonRatioPerPlayerNumber(result: GamePlayerTestResult) {
    val percentFormat = DecimalFormat("0.00%")
    result.numbersOfPlayers.forEach { numberOfPlayers ->
        val results = result.startingFloodLevels.map { result[it, numberOfPlayers].let { (it.gamesWonRatio()) } }
        println("|$numberOfPlayers players win rate|${results.joinToString("|") { percentFormat.format(it) }}|")
    }
}

private fun printGameResultBreakdown(result: GamePlayerTestResult) {
    val percentFormat = DecimalFormat("0.0%")
    val resultTypes = result.gameResults.flatMap { it.value.summaries.map { it.result::class } }.toSortedSet(compareBy { it.simpleName })
    for (resultType in resultTypes) {
        result.startingFloodLevels
            .map { level -> result[level].summaries.count { resultType.isInstance(it.result) } }
            .map { it.toFloat() / result.gamesPerFloodLevel }
            .let { println("|${resultType.simpleName}|${it.joinToString("|") { percentFormat.format(it) }}|") }
    }
}

private fun printAverageTreasuresCaptured(result: GamePlayerTestResult) {
    val decimalFormat = DecimalFormat("0.00")
    result.startingFloodLevels
        .map { level -> result[level].totalTreasuresCaptured() }
        .map { it.toFloat() / result.gamesPerFloodLevel }
        .let { println("|Avg. Treasures|${it.joinToString("|") { decimalFormat.format(it) }}|") }
}

private fun printAverageActions(result: GamePlayerTestResult) {
    val intFormat = NumberFormat.getIntegerInstance()
    result.startingFloodLevels
        .map { level -> result[level].totalActions() }
        .map { it / result.gamesPerFloodLevel }
        .let { println("|Avg. Actions|${it.joinToString("|") { intFormat.format(it) }}|") }
}
