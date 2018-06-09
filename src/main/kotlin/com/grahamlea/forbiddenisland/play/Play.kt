package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.*
import com.grahamlea.forbiddenisland.StartingFloodLevel.Novice
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max

interface GamePlayer {

    fun newContext(game: Game, random: Random): GamePlayContext
    fun done() { }

    interface GamePlayContext {
        fun selectNextAction(): GameAction
    }
}

private const val gamePlayerTestSeedGeneratorSeed = 78345763246952L

fun testGamePlayer(gamePlayer: GamePlayer, gamesPerCategory: Int = 250): GameTestResult {
    val seedGenerator = Random(gamePlayerTestSeedGeneratorSeed)
    val numberOfThreads = max(1, Runtime.getRuntime().availableProcessors() - 1)
    println("Testing ${gamePlayer::class.qualifiedName?.split('.')?.takeLast(2)?.joinToString(".")} with $numberOfThreads threads")
    val fixedThreadPool = Executors.newFixedThreadPool(numberOfThreads)
    val tasks = StartingFloodLevel.values().flatMap { startingFloodLevel ->
        (2..4).map { numberOfPlayers ->
            val deterministicRandomForThread = Random(seedGenerator.nextLong())
            Callable {
                runGamePlayerTest(startingFloodLevel, numberOfPlayers, gamesPerCategory, gamePlayer, deterministicRandomForThread)
            }
        }
    }
    val results = fixedThreadPool.invokeAll(tasks)
    fixedThreadPool.shutdown()
    gamePlayer.done()
    return GameTestResult(gamesPerCategory, results.map { it.get() }.toMap())
}

private fun runGamePlayerTest(
    startingFloodLevel: StartingFloodLevel,
    numberOfPlayers: Int,
    gamesPerCategory: Int,
    gamePlayer: GamePlayer,
    random: Random
): Pair<GameTestResult.GameTestCategory, List<GameTestResult.GameSummary>> {
    return GameTestResult.GameTestCategory(startingFloodLevel, numberOfPlayers) to
        (1..gamesPerCategory).asSequence().map {
            playGame(gamePlayer, numberOfPlayers, startingFloodLevel, random).let { game ->
                GameTestResult.GameSummary(
                    game.gameState.result!!,
                    game.gameState.previousActions.size,
                    game.gameState.treasuresCollected.count { it.value == true }
                )
            }
        }.toList()
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

    val gamePlayContext = gamePlayer.newContext(game, random)

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

    data class GameSummary(val result: GameResult, val actions: Int, val treasuresCaptured: Int)

    inner class GameSummaries(val summaries: List<GameSummary>) {
        fun gamesWonRatio() = summaries.count { it.result == AdventurersWon } / gamesPerCategory
        fun totalActions() = summaries.sumBy { it.actions }
        fun totalTreasuresCaptured() = summaries.sumBy { it.treasuresCaptured }
    }
}

fun printGamePlayerTestResult(result: GameTestResult) {
    println("| |${StartingFloodLevel.values().joinToString("|")}|")
    println("|---|---:|---:|---:|---:|")
    printGameWonRatioPerPlayerNumber(result)
    printGameResultBreakdown(result)
    printAverageTreasuresCaptured(result)
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

private fun printAverageTreasuresCaptured(result: GameTestResult) {
    val decimalFormat = DecimalFormat("0.00")
    StartingFloodLevel.values()
        .map { level -> (2..4).sumBy { result[level, it].totalTreasuresCaptured() } }
        .map { it.toFloat() / (result.gamesPerCategory * 3) }
        .let { println("|Avg. Treasures|${it.joinToString("|") { decimalFormat.format(it) }}|") }
}

private fun printAverageActions(result: GameTestResult) {
    val intFormat = NumberFormat.getIntegerInstance()
    StartingFloodLevel.values()
        .map { level -> (2..4).sumBy { result[level, it].totalActions() } }
        .map { it / (result.gamesPerCategory * 3) }
        .let { println("|Avg. Actions|${it.joinToString("|") { intFormat.format(it) }}|") }
}
