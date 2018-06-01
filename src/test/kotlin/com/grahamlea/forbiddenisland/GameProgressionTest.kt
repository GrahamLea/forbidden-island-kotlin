package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Game progression")
class GameProgressionTest {

    @Test
    fun `action processed on Game reults in progression of GameState`() {
        val game = Game.newRandomGameFor(4)
        val originalGameState = game.gameState
        game.process(game.gameState.availableActions.first { it is Move })
        assertThat(game.gameState).isNotEqualTo(originalGameState)
    }

    @Test
    fun `game phase is updated during action`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
        assertThat(game.gameState.phase).isEqualTo(AwaitingPlayerAction(Engineer, 3))

        game.process(game.gameState.availableActions.first { it is Move })
        assertThat(game.gameState.phase).isEqualTo(AwaitingPlayerAction(Engineer, 2))

        game.process(game.gameState.availableActions.first { it is Move })
        assertThat(game.gameState.phase).isEqualTo(AwaitingPlayerAction(Engineer, 1))
    }
}