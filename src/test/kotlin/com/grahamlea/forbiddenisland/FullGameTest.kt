package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.play.ConsoleLogger
import com.grahamlea.forbiddenisland.play.RandomGamePlayer
import com.grahamlea.forbiddenisland.play.playGame
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.text.NumberFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@DisplayName("Full games")
class FullGameTest {

    @RepeatedTest(1000)
    fun `play a full game until the end`() {
        val debug = false

        val seed = System.currentTimeMillis() xor System.nanoTime()
        if (debug) println("seed = ${seed}")

        val random = Random(seed)

        val randomGamePlayer = RandomGamePlayer(random)

        assertTimeout(Duration.ofSeconds(1), {
            playGame(randomGamePlayer, random = random, logger = ConsoleLogger(debug))
        })
    }

    @Disabled
    @Test
    fun `can random moves win a game?`() {
        val format = NumberFormat.getIntegerInstance()
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        println("availableProcessors = ${availableProcessors}")

        val games = AtomicLong(0)
        val gamesWon = AtomicLong(0)

        val executorService = Executors.newFixedThreadPool(7)

        (0 until (availableProcessors - 1)).forEach {
            executorService.submit({
                println("Started thread ${it + 1}")
                val random = Random()
                val randomGamePlayer = RandomGamePlayer(random)
                while (gamesWon.get() < 2) {
                    val game = playGame(randomGamePlayer)
                    val gamesDone = games.incrementAndGet()
                    if (game.gameState.result == AdventurersWon) {
                        gamesWon.incrementAndGet()
                        println(GamePrinter.toString(game))
                        println("Won a game! Have won $gamesWon/$gamesDone so far")
                    }
                    if (gamesDone  % 100000L == 0L) {
                        println("Played ${format.format(gamesDone)} games so far")
                    }
                }
            })
        }

        executorService.shutdown()
        executorService.awaitTermination(1, TimeUnit.HOURS)
    }

}

