package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.StartingFloodLevel
import java.util.*

object VarianceCheck {
    @JvmStatic
    fun main(args: Array<String>) {
        generateSequence(1) { it * 2 }.takeWhile { it < 3000 }.forEach { limit ->
            val gamePlayer = RandomGamePlayer()
            println("$limit,${getAverageActions(limit, gamePlayer)},${getAverageActions(limit, gamePlayer)},${getAverageActions(limit, gamePlayer)},${getAverageActions(limit, gamePlayer)},${getAverageActions(limit, gamePlayer)}")
        }
    }

    private fun getAverageActions(limit: Int, gamePlayer: RandomGamePlayer): Int {
        var runs = 0
        var totalActions = 0
        while (runs < limit) {
            runs++
            totalActions += playGame(gamePlayer, 4, StartingFloodLevel.Novice, Random(88)).gameState.previousActions.size
        }
        val averageActions = totalActions / runs
        return averageActions
    }
}