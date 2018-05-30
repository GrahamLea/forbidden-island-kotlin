package com.grahamlea.forbiddenisland

import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

@DisplayName("Full games")
class FullGameTest {

    @RepeatedTest(1000)
    fun `play a full game until the end`() {
        val debug = false

        val seed = System.currentTimeMillis() xor System.nanoTime()
        println("seed = ${seed}")

        val random = Random(seed)

        assertTimeout(Duration.ofSeconds(1), {
            `play a random game`(random, debug)
        })
    }

    @Disabled
    @Test
    fun `can random moves win a game?`() {
        val random = Random()

        val availableProcessors = Runtime.getRuntime().availableProcessors()
        println("availableProcessors = ${availableProcessors}")

        val games = AtomicLong(0)
        val gamesWon = AtomicLong(0)

        // TODO Use an ExecutorService so the test runs until the threads return...

        (0 until (availableProcessors - 1)).forEach {
            thread {
                println("Started thread ${it + 1}")
                while (gamesWon.get() < 2) {
                    val game = `play a random game`(random, false)
                    val gamesDone = games.incrementAndGet()
                    if (game.gameState.result == AdventurersWon) {
                        gamesWon.incrementAndGet()
                        println(GamePrinter.toString(game))
                        println("Won a game! Have won $gamesWon/$games so far")
                    }
                    if (gamesDone  % 50000L == 0L) {
                        println("Played $games games so far")
                    }
                }
            }
        }

        Thread.sleep(1000L * 60 * 60 * 24) // Because JUnit kills the process once the test returns, regardless of non-daemon threads

    }

    private fun `play a random game`(random: Random, debug: Boolean): Game {

        val numberOfPlayers = 2 + (Math.random() * 3).toInt()
        if (debug) println("numberOfPlayers = ${numberOfPlayers}")

        val game = Game.newRandomGameFor(numberOfPlayers, random)
        if (debug) println(GamePrinter.toString(game))

        printGameOnFailure(game) {
            var actions = 0

            while (game.gameState.result == null) {
                if (debug) println(game.gameState.phase)

                val availableActions = game.gameState.availableActions
                val action = availableActions[random.nextInt(availableActions.size)]
                if (debug) {
                    print("    "); println(action)
                }

                game.process(action)
                actions++
            }
            if (debug) println("actions = ${actions}")
            if (debug) println("result = ${game.gameState.result}")
        }

        return game
    }

}

