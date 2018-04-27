package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Engineer
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameStateProgressionTest {

    private val earth = TreasureCard(Treasure.EarthStone)
    private val ocean = TreasureCard(Treasure.OceansChalice)

    @Test
    fun `events on game state are recorded in previous events`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(LocationFloodState.Flooded, listOf(Position(2, 3)))

        val event1 = Move(Engineer, game.gameSetup.map.mapSiteAt(Position(4, 3)))
        val event2 = Move(Engineer, game.gameSetup.map.mapSiteAt(Position(3, 3)))
        val event3 = ShoreUp(Engineer, game.gameSetup.map.mapSiteAt(Position(2, 3)))

        after (listOf(event1, event2, event3) playedOn game) {
            assertThat(previousEvents, is_(immListOf<GameEvent>(event1, event2, event3)))
        }
    }

    @Test
    fun `move played on game changes map site of the one player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val engineerOriginalSite = game.gameState.playerPositions.getValue(Engineer)
        val messengerOriginalSite = game.gameState.playerPositions.getValue(Messenger)

        assertThat(game.gameState.playerPositions, is_(immMapOf(Engineer to engineerOriginalSite, Messenger to messengerOriginalSite)))

        val engineerNewSite = game.gameSetup.map.mapSiteAt(Position(4, 3))
        after (Move(Engineer, engineerNewSite) playedOn game) {
            assertThat(playerPositions, is_(immMapOf(Engineer to engineerNewSite, Messenger to messengerOriginalSite)))
        }
    }

    @Test
    fun `shore up played on game changes location flood state`() {
        val positionToShoreUp = Position(3, 4)
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(LocationFloodState.Flooded, listOf(positionToShoreUp))

        val mapSiteToShoreUp = game.gameSetup.map.mapSiteAt(positionToShoreUp)

        assertThat(game.gameState.locationFloodStates[mapSiteToShoreUp.location], is_(LocationFloodState.Flooded))

        after (ShoreUp(Engineer, mapSiteToShoreUp) playedOn game) {
            assertThat(locationFloodStates[mapSiteToShoreUp.location], is_(LocationFloodState.Unflooded))
        }
    }

    @Test
    fun `give treasure card event on game state changes player cards`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to cards(earth, earth), Engineer to cards(earth, earth)))

        after (GiveTreasureCard(Messenger, Engineer, immListOf(earth)) playedOn game) {
            assertThat(playerCards, is_(immMapOf(Messenger to cards(earth), Engineer to cards(earth, earth, earth))))
        }
    }

    @Test
    fun `give multiple treasure cards event on game state changes player cards`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to cards(earth, earth), Engineer to cards(earth, earth)))

        after (GiveTreasureCard(Messenger, Engineer, immListOf(earth, earth)) playedOn game) {
            assertThat(playerCards, is_(immMapOf(Messenger to cards(), Engineer to cards(earth, earth, earth, earth))))
        }
    }

    @Test
    fun `capture treasure event captures treasure, and discards treasure cards`() {
        val gameSetup = GameSetup(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
        val game = Game.newRandomGameFor(gameSetup)
                .withPlayerPosition(Messenger, gameSetup.map.positionOf(Location.TempleOfTheSun))
                .withPlayerCards(
                    immMapOf(Messenger to cards(earth, earth, earth, earth, ocean),
                             Engineer to cards(ocean, ocean)))
                .withTreasureDeckDiscard(cards(ocean))

        assertThat(game.gameState.treasuresCollected, is_(Treasure.values().associate { it to false }.imm()))

        after (CaptureTreasure(Messenger, Treasure.EarthStone) playedOn game) {
            assertThat(treasuresCollected[Treasure.EarthStone], is_(true))
            assertThat(treasuresCollected.filterKeys { it != Treasure.EarthStone }.values.toSet(), is_(setOf(false)))
            assertThat(playerCards, is_(immMapOf(Messenger to cards(ocean), Engineer to cards(ocean, ocean))))
            assertThat(treasureDeckDiscard, is_(cards(ocean, earth, earth, earth, earth)))
        }
    }
}

private inline fun <T, R> after(receiver: T, block: T.() -> R): R = with(receiver, block)

private infix fun GameEvent.playedOn(game: Game): GameState = game.gameState.after(this)
private infix fun List<GameEvent>.playedOn(game: Game): GameState = this.fold(game.gameState) { state, event -> state.after(event) }
