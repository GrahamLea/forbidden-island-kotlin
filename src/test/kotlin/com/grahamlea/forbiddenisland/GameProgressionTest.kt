package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameProgressionTest {

    @Test
    fun `action processed on Game reults in progression of GameState`() {
        val originalGameState: GameState = mock()

        val game = Game(GameSetup(immListOf(Engineer, Messenger), GameMap.newShuffledMap()), originalGameState)

        val moveEvent = Move(Engineer, game.gameSetup.map.mapSiteAt(Position(4, 3)))

        val nextGameState: GameState = mock()
        whenever(originalGameState.after(moveEvent, game.random)) doReturn nextGameState

        game.process(moveEvent)

        assertThat(game.gameState, is_(nextGameState))
    }

    @Test
    fun `game phase is updated during event`() {

        val map = GameMap.newShuffledMap()

        val event1 = Move(Engineer, map.mapSiteAt(Position(4, 3)))
        val event2 = Move(Engineer, map.mapSiteAt(Position(3, 3)))

        val gamePhase1: GamePhase = mock()
        val gamePhase2: GamePhase = mock()
        val gamePhase3: GamePhase = mock()

        whenever(gamePhase1.phaseAfter(event1)) doReturn gamePhase2
        whenever(gamePhase2.phaseAfter(event2)) doReturn gamePhase3

        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), map)
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(LocationFloodState.Flooded, Position(2, 3))
                .withGamePhase(gamePhase1)

        after (event1 playedOn game) {
            assertThat(phase, is_(gamePhase2))
        }

        after (listOf(event1, event2) playedOn game) {
            assertThat(phase, is_(gamePhase3))
        }
    }

    private inline fun <T, R> after(receiver: T, block: T.() -> R): R = with(receiver, block)

    private infix fun GameEvent.playedOn(game: Game): GameState = game.gameState.after(this, game.random)
    private infix fun List<GameEvent>.playedOn(game: Game): GameState = this.fold(game.gameState) { state, event -> state.after(event, game.random) }

}