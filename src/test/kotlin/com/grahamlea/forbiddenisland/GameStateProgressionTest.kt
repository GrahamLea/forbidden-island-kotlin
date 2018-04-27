package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameStateProgressionTest {

    private val earthCard = TreasureCard(Treasure.EarthStone)
    private val oceanCard = TreasureCard(Treasure.OceansChalice)
    private val emptyCardList = immListOf<HoldableCard>()

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
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to emptyCardList + earthCard + earthCard, Engineer to emptyCardList + earthCard + earthCard))

        val event = GiveTreasureCard(Messenger, Engineer, immListOf(earthCard))
        val nextGameState = game.gameState.after(event)
        assertThat(nextGameState.playerCards, is_(immMapOf(Messenger to emptyCardList + earthCard, Engineer to emptyCardList + earthCard + earthCard + earthCard)))
    }

    @Test
    fun `give multiple treasure cards event on game state changes player cards`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to emptyCardList + earthCard + earthCard, Engineer to emptyCardList + earthCard + earthCard))

        val event = GiveTreasureCard(Messenger, Engineer, immListOf(earthCard, earthCard))
        val nextGameState = game.gameState.after(event)
        assertThat(nextGameState.playerCards, is_(immMapOf(Messenger to emptyCardList, Engineer to emptyCardList + earthCard + earthCard + earthCard + earthCard)))
    }

    @Test
    fun `capture treasure event captures treasure, and discards treasure cards`() {
        val gameSetup = GameSetup(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
        val game = Game.newRandomGameFor(gameSetup)
                .withPlayerPosition(Messenger, gameSetup.map.positionOf(Location.TempleOfTheSun))
                .withPlayerCards(
                    immMapOf(Messenger to emptyCardList + earthCard + earthCard + earthCard + earthCard + oceanCard,
                             Engineer to emptyCardList + oceanCard + oceanCard))
                .withTreasureDeckDiscard(emptyCardList + oceanCard)

        assertThat(game.gameState.treasuresCollected, is_(Treasure.values().associate { it to false }.imm()))
        assertThat(game.gameState.treasureDeckDiscard, is_(emptyCardList + oceanCard))

        val event = CaptureTreasure(Messenger, Treasure.EarthStone)
        val nextGameState = game.gameState.after(event)
        assertThat(nextGameState.treasuresCollected, is_(Treasure.values().associate { it to false }.imm() + (Treasure.EarthStone to true)))
        assertThat(nextGameState.playerCards, is_(immMapOf(Messenger to emptyCardList + oceanCard, Engineer to emptyCardList + oceanCard + oceanCard)))
        assertThat(nextGameState.treasureDeckDiscard, is_(emptyCardList + oceanCard + earthCard + earthCard + earthCard + earthCard))
    }

}