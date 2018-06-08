package com.grahamlea.forbiddenisland.play

import com.grahamlea.forbiddenisland.Game
import com.grahamlea.forbiddenisland.GameAction
import java.util.*

class RandomGamePlayer(private val random: Random = Random(27697235L)): GamePlayer {

    override fun newContext(game: Game): GamePlayer.GamePlayContext = Context(game)

    inner class Context(private val game: Game) : GamePlayer.GamePlayContext {
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