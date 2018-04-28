package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.sameInstance
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameProgressionTest {

    @Test
    fun `action processed on Game affects GameState`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val originalGameState = game.gameState

        val move = Move(Engineer, game.gameSetup.map.mapSiteAt(Position(4, 3)))

        val expectedState = originalGameState.after(move, game.random)

        game.process(move)

        assertThat(game.gameState, is_(expectedState))
        assertThat(game.gameState, is_(not(sameInstance(expectedState))))
        assertThat(game.gameState, is_(not(originalGameState)))
    }
}