package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Game progression")
class GameProgressionTest {

    @Test
    fun `action processed on Game reults in progression of GameState`() {
        val originalGameState: GameState = mock()

        val game = Game(GameSetup(immListOf(Engineer, Messenger), GameMap.newShuffledMap()), originalGameState)

        val moveEvent = Move(Engineer, Position(4, 3))

        val nextGameState: GameState = mock()
        whenever(originalGameState.after(moveEvent, game.random)) doReturn nextGameState

        game.process(moveEvent)

        assertThat(game.gameState).isEqualTo(nextGameState)
    }

    @Test
    fun `game phase is updated during event with player card counts of new state`() {

        val map = GameMap.newShuffledMap()

        val moveEvent = Move(Engineer, Position(4, 3))
        val drawEvent = DrawFromTreasureDeck(Engineer)

        val gamePhase1: GamePhase = mock()
        val gamePhase2: GamePhase = mock()
        val gamePhase3: GamePhase = mock()

        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), map)
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(LocationFloodState.Flooded, Position(2, 3))
                .withTopOfTreasureDeck(TreasureCard(Treasure.EarthStone))
                .withGamePhase(gamePhase1)

        val cardCountsBeforeDraw = mapOf(Engineer to 2, Messenger to 2)
        val cardCountsAfterDraw = mapOf(Engineer to 3, Messenger to 2)
        whenever(gamePhase1.phaseAfter(same(moveEvent), argWhere { it.playerCardCounts == cardCountsBeforeDraw })) doReturn gamePhase2
        whenever(gamePhase2.phaseAfter(same(drawEvent), argWhere { it.playerCardCounts == cardCountsAfterDraw })) doReturn gamePhase3

        after (moveEvent playedOn game) {
            assertThat(phase).isEqualTo(gamePhase2)
        }

        after (listOf(moveEvent, drawEvent) playedOn game) {
            assertThat(phase).isEqualTo(gamePhase3)
        }
    }

    private inline fun <T, R> after(receiver: T, block: T.() -> R): R = with(receiver, block)

    private infix fun GameEvent.playedOn(game: Game): GameState = game.gameState.after(this, game.random)
    private infix fun List<GameEvent>.playedOn(game: Game): GameState = this.fold(game.gameState) { state, event -> state.after(event, game.random) }

}