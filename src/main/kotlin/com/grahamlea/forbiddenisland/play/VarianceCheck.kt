package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.AdventurersWon
import com.grahamlea.forbiddenisland.StartingFloodLevel
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max

object VarianceCheck {

    val gamePlayer = RandomGamePlayer()

    @JvmStatic
    fun main(args: Array<String>) {
        val batchNumbers = 1..5
        val numberOfThreads = max(1, Runtime.getRuntime().availableProcessors() - 1)
        val fixedThreadPool = Executors.newFixedThreadPool(numberOfThreads)
        val results = fixedThreadPool.invokeAll(
            generateSequence(1) { it * 2 }.takeWhile { it < 40_000 }.toList().reversed().map { limit ->
                Callable {
                    println("Testing batches of $limit")
                    val random = Random(88)
                    limit to batchNumbers.map { getAverageActionsAndWinRate(limit, gamePlayer, random) }
                }
            }
        )
        fixedThreadPool.shutdown()
        println("Games per batch,${batchNumbers.map { "Batch $it avg. actions" }.joinToString(",")},${batchNumbers.map { "Batch $it win rate" }.joinToString(",")}")
        for (result in results.reversed()) {
            val (limit, actionCountsAndWinRatios) = result.get()
            val actionCountsList = actionCountsAndWinRatios.map { it.first }.joinToString(",")
            val winRatiosList = actionCountsAndWinRatios.map { "%4.1f%%".format(it.second) }.joinToString(",")
            println("${"%4d".format(limit)},$actionCountsList,$winRatiosList")
        }
    }

    private fun getAverageActionsAndWinRate(limit: Int, gamePlayer: GamePlayer, random: Random): Pair<Int, Float> {
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
        return Pair(averageActions, winRate)
    }
}