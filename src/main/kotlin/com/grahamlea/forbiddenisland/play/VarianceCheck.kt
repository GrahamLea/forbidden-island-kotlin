package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.AdventurersWon
import com.grahamlea.forbiddenisland.StartingFloodLevel
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max

object VarianceCheck {
    @JvmStatic
    fun main(args: Array<String>) {
        val gamePlayer = RandomGamePlayer()
        val batchesPerBatchSize = 5
        val batchSizeLimit = 33_000
        printVarianceCheckResults(runVarianceCheck(gamePlayer, batchesPerBatchSize, batchSizeLimit))
    }
}

typealias BatchSize = Int
typealias ActionCount = Int
typealias WinRatio = Float
typealias BatchResult = Pair<ActionCount, WinRatio>

fun runVarianceCheck(gamePlayer: RandomGamePlayer, batchesPerBatchSize: Int, batchSizeLimit: Int): Map<BatchSize, List<BatchResult>> {
    val numberOfThreads = max(1, Runtime.getRuntime().availableProcessors() - 1)
    val fixedThreadPool = Executors.newFixedThreadPool(numberOfThreads)
    val results = fixedThreadPool.invokeAll(
        generateSequence(1) { it * 2 }.takeWhile { it <= batchSizeLimit }.toList().reversed().map { limit ->
            Callable {
                println("Testing batches of $limit")
                val random = Random(88)
                limit to (1..batchesPerBatchSize).map { getAverageActionsAndWinRate(limit, gamePlayer, random) }
            }
        }
    )
    fixedThreadPool.shutdown()
    return results.map { it.get() }.reversed().toMap()
}

fun printVarianceCheckResults(results: Map<BatchSize, List<BatchResult>>) {
    val batchNumbers = 1 .. results.values.first().size
    println("Games per batch,${batchNumbers.map { "Batch $it avg. actions" }.joinToString(",")},${batchNumbers.map { "Batch $it win rate" }.joinToString(",")}")
    for ((limit, actionCountsAndWinRatios) in results) {
        val actionCountsList = actionCountsAndWinRatios.map { it.first }.joinToString(",")
        val winRatiosList = actionCountsAndWinRatios.map { "%4.1f%%".format(it.second) }.joinToString(",")
        println("${"%4d".format(limit)},$actionCountsList,$winRatiosList")
    }
}

private fun getAverageActionsAndWinRate(limit: Int, gamePlayer: GamePlayer, random: Random): BatchResult {
    var totalActions = 0
    var totalWins = 0
    var runs = 0
    while (runs < limit) {
        runs++
        val gameState = playGame(gamePlayer, 4, StartingFloodLevel.Novice, random).gameState
        totalActions += gameState.previousActions.size
        totalWins += if (gameState.result == AdventurersWon) 1 else 0
    }
    val averageActions = totalActions / runs
    val winRate = totalWins * 100f / runs
    return BatchResult(averageActions, winRate)
}