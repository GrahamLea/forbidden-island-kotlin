package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameProgressionTest {

    @Test
    fun `move played on game affects state`() {
        val game = Game.newRandomGameFor(immutableListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val move = Move(Engineer, game.gameSetup.map.mapSiteAt(Position(4, 3)))
        game.process(move)

        assertThat(game.gameState.playerPositions[Engineer]?.position, is_(Position(4, 3)))
        assertThat(game.gameState.previousEvents, is_(immutableListOf<GameEvent>(move)))
    }
}