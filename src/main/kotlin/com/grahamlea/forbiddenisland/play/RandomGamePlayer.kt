package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.Game
import com.grahamlea.forbiddenisland.GameAction
import java.util.*

class RandomGamePlayer : GamePlayer {

    override fun newContext(game: Game, random: Random): GamePlayer.GamePlayContext = Context(game, random)

    inner class Context(private val game: Game, private val random: Random) : GamePlayer.GamePlayContext {
        override fun selectNextAction(): GameAction =
            game.gameState.availableActions.let { availableActions ->
                availableActions[random.nextInt(availableActions.size)]
            }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            printGamePlayerTestResult(testGamePlayer(RandomGamePlayer()))
        }
    }
}