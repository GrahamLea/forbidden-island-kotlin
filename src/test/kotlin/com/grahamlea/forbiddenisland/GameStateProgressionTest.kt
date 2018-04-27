package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameStateProgressionTest {

    @Test
    fun `events on game state are recorded in previous events`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(LocationFloodState.Flooded, listOf(Position(2, 3)))

        val event1 = Move(Engineer, game.gameSetup.map.mapSiteAt(Position(4, 3)))
        val event2 = Move(Engineer, game.gameSetup.map.mapSiteAt(Position(3, 3)))
        val event3 = ShoreUp(Engineer, game.gameSetup.map.mapSiteAt(Position(2, 3)))

        val nextGameState = game.gameState.after(event1).after(event2).after(event3)

        assertThat(nextGameState.previousEvents, is_(immListOf<GameEvent>(event1, event2, event3)))
    }

    @Test
    fun `move played on game changes map site of the one player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val engineerOriginalSite = game.gameState.playerPositions.getValue(Engineer)
        val messengerOriginalSite = game.gameState.playerPositions.getValue(Messenger)

        assertThat(game.gameState.playerPositions, is_(immMapOf(Engineer to engineerOriginalSite, Messenger to messengerOriginalSite)))

        val engineerNewSite = game.gameSetup.map.mapSiteAt(Position(4, 3))
        val event = Move(Engineer, engineerNewSite)
        val nextGameState = game.gameState.after(event)
        assertThat(nextGameState.playerPositions, is_(immMapOf(Engineer to engineerNewSite, Messenger to messengerOriginalSite)))
    }

    @Test
    fun `shore up played on game changes location flood state`() {
        val positionToShoreUp = Position(3, 4)
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(LocationFloodState.Flooded, listOf(positionToShoreUp))

        val mapSiteToShoreUp = game.gameSetup.map.mapSiteAt(positionToShoreUp)

        assertThat(game.gameState.locationFloodStates[mapSiteToShoreUp.location], is_(LocationFloodState.Flooded))

        val event = ShoreUp(Engineer, mapSiteToShoreUp)
        val nextGameState = game.gameState.after(event)
        assertThat(nextGameState.locationFloodStates[mapSiteToShoreUp.location], is_(LocationFloodState.Unflooded))
    }

    @Test
    fun `give treasure card event on game state changes player cards`() {
        val card = TreasureCard(Treasure.EarthStone)
        val emptyCardList = immListOf<HoldableCard>()
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to emptyCardList + card + card, Engineer to emptyCardList + card + card))

        val event = GiveTreasureCard(Messenger, Engineer, immListOf(card))
        val nextGameState = game.gameState.after(event)
        assertThat(nextGameState.playerCards, is_(immMapOf(Messenger to emptyCardList + card, Engineer to emptyCardList + card + card + card)))
    }

    @Test
    fun `give multiple treasure cards event on game state changes player cards`() {
        val card = TreasureCard(Treasure.EarthStone)
        val emptyCardList = immListOf<HoldableCard>()
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to emptyCardList + card + card, Engineer to emptyCardList + card + card))

        val event = GiveTreasureCard(Messenger, Engineer, immListOf(card, card))
        val nextGameState = game.gameState.after(event)
        assertThat(nextGameState.playerCards, is_(immMapOf(Messenger to emptyCardList, Engineer to emptyCardList + card + card + card + card)))
    }

}