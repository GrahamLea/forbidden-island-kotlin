package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameStateProgressionTest {

    @Test
    fun `events on game state are recorded in previous events`() {
        val game = Game.newRandomGameFor(immutableListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val move1 = game.moveEvent(Engineer, Position(4, 3))
        val move2 = game.moveEvent(Engineer, Position(3, 3))
        val move3 = game.moveEvent(Engineer, Position(2, 3))

        val nextGameState = game.gameState.after(move1).after(move2).after(move3)

        assertThat(nextGameState.previousEvents, is_(immutableListOf<GameEvent>(move1, move2, move3)))
    }

    @Test
    fun `move played on game changes map site`() {
        val game = Game.newRandomGameFor(immutableListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val move = game.moveEvent(Engineer, Position(4, 3))

        val nextGameState = game.gameState.after(move)

        assertThat(nextGameState.playerPositions[Engineer]?.position, is_(Position(4, 3)))
    }

    private fun Game.moveEvent(player: Adventurer, position: Position) = Move(player, gameSetup.map.mapSiteAt(position))
}