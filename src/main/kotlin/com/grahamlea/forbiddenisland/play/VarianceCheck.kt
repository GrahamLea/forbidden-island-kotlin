package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.AdventurersWon
import com.grahamlea.forbiddenisland.StartingFloodLevel
import java.util.*

object VarianceCheck {

    val gamePlayer = RandomGamePlayer()

    @JvmStatic
    fun main(args: Array<String>) {
        val batchNumbers = 1..5
        println("Games per batch,${batchNumbers.map { "Batch $it avg. actions" }.joinToString(",")},${batchNumbers.map { "Batch $it win rate" }.joinToString(",")}")
        generateSequence(1) { it * 2 }.takeWhile { it < 5000 }.forEach { limit ->
            val random = Random(88)
            val results = batchNumbers.map { getAverageActionsAndWinRate(limit, gamePlayer, random) }
            println("${"%4d".format(limit)},${results.map { it.first }.joinToString(",")},${results.map { "%4.1f%%".format(it.second) }.joinToString(",")}")
        }
    }

    private fun getAverageActionsAndWinRate(limit: Int, gamePlayer: GamePlayer, random: Random): Pair<Int, Float> {
        var runs = 0
        var totalActions = 0
        var totalWins = 0
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