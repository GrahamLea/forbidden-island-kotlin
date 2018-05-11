package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Location.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("GameState progressions following events")
class GameStateProgressionTest {

    private val random = Random()
    private val earth = TreasureCard(Treasure.EarthStone)
    private val ocean = TreasureCard(Treasure.OceansChalice)
    private val fire = TreasureCard(Treasure.CrystalOfFire)

    @Test
    fun `events on game state are recorded in previous events`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(Flooded, Position(2, 3))

        val event1 = Move(Engineer, Position(4, 3))
        val event2 = Move(Engineer, Position(3, 3))
        val event3 = ShoreUp(Engineer, Position(2, 3))

        after (listOf(event1, event2, event3) playedOn game) {
            assertThat(previousEvents).isEqualTo(immListOf<GameEvent>(event1, event2, event3))
        }
    }

    @Test
    fun `move played on game changes position of the one player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val engineerOriginalPosition = game.gameState.playerPositions.getValue(Engineer)
        val messengerOriginalPosition = game.gameState.playerPositions.getValue(Messenger)

        assertThat(game.gameState.playerPositions).isEqualTo(immMapOf(Engineer to engineerOriginalPosition, Messenger to messengerOriginalPosition))

        val engineerNewPosition = Position(4, 3)
        after (Move(Engineer, engineerNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(Engineer to engineerNewPosition, Messenger to messengerOriginalPosition))
        }
    }

    @Test
    fun `fly played on game changes position of the Pilot`() {
        val pilotOriginalPosition = Position(4, 4)
        val game = Game.newRandomGameFor(immListOf(Pilot, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Pilot, pilotOriginalPosition)

        val messengerOriginalPosition = game.gameState.playerPositions.getValue(Messenger)

        val pilotNewPosition = Position(1, 3)
        after (Fly(Pilot, pilotNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(Pilot to pilotNewPosition, Messenger to messengerOriginalPosition))
        }
    }

    @Test
    fun `swim event played on game changes position of the one player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val engineerOriginalPosition = game.gameState.playerPositions.getValue(Engineer)
        val messengerOriginalPosition = game.gameState.playerPositions.getValue(Messenger)

        assertThat(game.gameState.playerPositions).isEqualTo(immMapOf(Engineer to engineerOriginalPosition, Messenger to messengerOriginalPosition))

        val engineerNewPosition = Position(4, 3)
        after (SwimToSafety(Engineer, engineerNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(Engineer to engineerNewPosition, Messenger to messengerOriginalPosition))
        }
    }

    @Test
    fun `shore up played on game changes location flood state`() {
        val positionToShoreUp = Position(3, 4)
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withLocationFloodStates(Flooded, positionToShoreUp)

        val mapSiteToShoreUp = game.gameSetup.map.mapSiteAt(positionToShoreUp)

        assertThat(game.gameState.locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Flooded)

        after (ShoreUp(Engineer, positionToShoreUp) playedOn game) {
            assertThat(locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Unflooded)
        }
    }

    @Test
    fun `give treasure card event on game state changes player cards`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to cards(earth, earth), Engineer to cards(earth, earth)))

        after (GiveTreasureCard(Messenger, Engineer, earth) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(earth), Engineer to cards(earth, earth, earth)))
        }
    }

    @Test
    fun `capture treasure event captures treasure, and discards treasure cards`() {
        val gameSetup = GameSetup(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
        val game = Game.newRandomGameFor(gameSetup)
                .withPlayerPosition(Messenger, gameSetup.map.positionOf(TempleOfTheSun))
                .withPlayerCards(
                    immMapOf(Messenger to cards(earth, earth, earth, earth, ocean),
                             Engineer to cards(ocean, ocean)))
                .withTreasureDeckDiscard(cards(ocean))

        assertThat(game.gameState.treasuresCollected).isEqualTo(Treasure.values().associate { it to false }.imm())

        after (CaptureTreasure(Messenger, Treasure.EarthStone) playedOn game) {
            assertThat(treasuresCollected[Treasure.EarthStone]).isEqualTo(true)
            assertThat(treasuresCollected.filterKeys { it != Treasure.EarthStone }.values.toSet()).isEqualTo(setOf(false))
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(ocean), Engineer to cards(ocean, ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, earth, earth, earth, earth))
        }
    }

    @Test
    fun `helicopter lift played on game changes position of the one player and discards the card`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(2, 2))
                .withPlayerCards(immMapOf(Messenger to cards(HelicopterLiftCard, earth), Engineer to cards(ocean)))
                .withTreasureDeckDiscard(cards(ocean))

        val messengerOriginalPosition = game.gameState.playerPositions.getValue(Messenger)
        val engineerOriginalPosition = game.gameState.playerPositions.getValue(Engineer)

        assertThat(game.gameState.playerPositions).isEqualTo(immMapOf(Messenger to messengerOriginalPosition, Engineer to engineerOriginalPosition))

        val engineerNewPosition = Position(5, 5)
        after (HelicopterLift(Messenger, Engineer, engineerNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(Messenger to messengerOriginalPosition, Engineer to engineerNewPosition))
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(earth), Engineer to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, HelicopterLiftCard))
        }
    }


    @Test
    fun `sandbag played on game shores up location and discards the card`() {
        val positionToShoreUp = Position(2, 2)
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withPlayerPosition(Messenger, Position(3, 4))
                .withLocationFloodStates(Flooded, positionToShoreUp)
                .withPlayerCards(immMapOf(Engineer to cards(earth, SandbagsCard), Messenger to cards(ocean)))
                .withTreasureDeckDiscard(cards(earth))

        val mapSiteToShoreUp = game.gameSetup.map.mapSiteAt(positionToShoreUp)

        assertThat(game.gameState.locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Flooded)

        after (Sandbag(Engineer, positionToShoreUp) playedOn game) {
            assertThat(locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Unflooded)
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(earth, SandbagsCard))
        }
    }

    @Test
    fun `discard card on game changes just discards the card`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Messenger to cards(HelicopterLiftCard, earth, ocean), Engineer to cards(ocean)))
                .withTreasureDeckDiscard(cards(ocean))

        after (DiscardCard(Messenger, earth) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(HelicopterLiftCard, ocean), Engineer to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, earth))
            assertThat(playerPositions).isEqualTo(game.gameState.playerPositions)
            assertThat(treasureDeck).isEqualTo(game.gameState.treasureDeck)
        }
    }

    @Test
    fun `draw treasure card from treasure deck moves card from treasure deck to player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
                .withTopOfTreasureDeck(fire, ocean, earth)

        val draw = DrawFromTreasureDeck(Engineer)
        after (draw playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth, fire), Messenger to cards(ocean)))
        }

        after (listOf(draw, draw) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth, fire, ocean), Messenger to cards(ocean)))
        }
    }

    @Test
    fun `draw Sandbags from treasure deck moves card from treasure deck to player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
                .withTopOfTreasureDeck(SandbagsCard)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth, SandbagsCard), Messenger to cards(ocean)))
        }
    }

    @Test
    fun `draw Helicopter Lift from treasure deck moves card from treasure deck to player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
                .withTopOfTreasureDeck(HelicopterLiftCard)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth, HelicopterLiftCard), Messenger to cards(ocean)))
        }
    }

    @Test
    fun `drawing last card from treasure deck shuffles treasure discard back to deck`() {
        val treasureDeckDiscardBeforeEvent = TreasureDeck.newShuffledDeck().subtract(listOf(earth))
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Engineer to cards(), Messenger to cards()))
                .withTreasureDeckDiscard(treasureDeckDiscardBeforeEvent)

        assertThat(game.gameState.treasureDeck).isEqualTo(cards(earth))

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards()))
            assertThat(treasureDeck.size).isEqualTo(TreasureDeck.newShuffledDeck().size - 1) // One card dealt to Engineer
            assertThat(treasureDeck).isNotEqualTo(treasureDeckDiscardBeforeEvent)
            assertThat(treasureDeckDiscard).isEqualTo(cards())
        }
    }

    @Test
    fun `drawing waters rise card from treasure deck raises flood level, discards card, and shuffles flood deck discard back to flood deck`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
                .withTopOfTreasureDeck(WatersRiseCard)

        val floodDeckBeforeEvent = game.gameState.floodDeck
        val floodDeckDiscardBeforeEvent = game.gameState.floodDeckDiscard

        assertThat(game.gameState.floodLevel).isEqualTo(FloodLevel.TWO)
        assertThat(floodDeckDiscardBeforeEvent.size).isEqualTo(6)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(floodLevel).isEqualTo(FloodLevel.THREE)
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(WatersRiseCard))
            assertThat(floodDeckDiscard).isEmpty()
            assertThat(floodDeck.take(6).toSortedSet()).isEqualTo(floodDeckDiscardBeforeEvent.toSortedSet())
            assertThat(floodDeck.take(6)).isNotEqualTo(floodDeckDiscardBeforeEvent as List<Location>)
            assertThat(floodDeck.drop(6)).isEqualTo(floodDeckBeforeEvent as List<Location>)
        }
    }

    @Test
    fun `drawing waters rise card as last card from treasure deck does all the expected things from the two test cases above`() {
        val treasureDeckDiscardBeforeEvent = TreasureDeck.newShuffledDeck().subtract(listOf(earth, ocean, WatersRiseCard))
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newShuffledMap())
                .withPlayerCards(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
                .withTreasureDeckDiscard(treasureDeckDiscardBeforeEvent)

        val floodDeckBeforeEvent = game.gameState.floodDeck
        val floodDeckDiscardBeforeEvent = game.gameState.floodDeckDiscard

        assertThat(game.gameState.floodLevel).isEqualTo(FloodLevel.TWO)
        assertThat(floodDeckDiscardBeforeEvent.size).isEqualTo(6)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(floodLevel).isEqualTo(FloodLevel.THREE)
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards())
            assertThat(floodDeckDiscard).isEmpty()
            assertThat(floodDeck.take(6).toSortedSet()).isEqualTo(floodDeckDiscardBeforeEvent.toSortedSet())
            assertThat(floodDeck.take(6)).isNotEqualTo(floodDeckDiscardBeforeEvent as List<Location>)
            assertThat(floodDeck.drop(6)).isEqualTo(floodDeckBeforeEvent as List<Location>)
            assertThat(treasureDeck.size).isEqualTo(TreasureDeck.newShuffledDeck().size - 2) // Players have two cards
            assertThat(treasureDeck).isNotEqualTo(treasureDeckDiscardBeforeEvent)
            assertThat(treasureDeckDiscard).isEqualTo(cards())
        }
    }

    @Test
    fun `draw location from flood deck floods or sinks location and discards card`() {
        val map = GameMap.newShuffledMap()
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), map)
                .withLocationFloodStates(Unflooded, *Position.allPositions.toTypedArray())
                .withLocationFloodStates(Flooded, MistyMarsh, Observatory)
                .withFloodDeckDiscard(immListOf(MistyMarsh))
                .withTopOfFloodDeck(CrimsonForest, Observatory)
                .withGamePhase(AwaitingFloodDeckDraw(Engineer, 3))

        val draw = DrawFromFloodDeck(Engineer)

        after (draw playedOn game) {
            assertThat(locationFloodStates[MistyMarsh]).isEqualTo(Flooded)
            assertThat(locationFloodStates[Observatory]).isEqualTo(Flooded)
            assertThat(locationFloodStates[CrimsonForest]).isEqualTo(Flooded)
            assertThat(locationFloodStates.values.count { it == Unflooded }).isEqualTo(Location.allLocationsSet.size - 3)
            assertThat(floodDeckDiscard).isEqualTo(immListOf(MistyMarsh, CrimsonForest))
        }

        after (listOf(draw, draw) playedOn game) {
            assertThat(locationFloodStates[MistyMarsh]).isEqualTo(Flooded)
            assertThat(locationFloodStates[Observatory]).isEqualTo(Sunken)
            assertThat(locationFloodStates[CrimsonForest]).isEqualTo(Flooded)
            assertThat(locationFloodStates.values.count { it == Unflooded }).isEqualTo(Location.allLocationsSet.size - 3)
            assertThat(floodDeckDiscard).isEqualTo(immListOf(MistyMarsh, CrimsonForest)) // Sunken Locations are removed from the Flood Deck
        }
    }

    @Test
    fun `drawing last card from flood deck shuffles flood discard back to deck without sunken locations`() {
        val floodDeckDiscardBeforeEvent = shuffled<Location>().subtract(listOf(MistyMarsh)) - Observatory
        val map = GameMap.newShuffledMap()
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), map)
                    .withLocationFloodStates(Unflooded, *Position.allPositions.toTypedArray())
                    .withLocationFloodStates(Flooded, DunesOfDeception)
                    .withLocationFloodStates(Sunken, Observatory)
                    .withFloodDeckDiscard(floodDeckDiscardBeforeEvent.imm())
                    .withGamePhase(AwaitingFloodDeckDraw(Engineer, 3))

        assertThat(game.gameState.floodDeck).isEqualTo(immListOf(MistyMarsh))
        assertThat(game.gameState.floodDeckDiscard.size).isEqualTo(Location.values().size - 2) // Observatory is out bc its sunken

        after (DrawFromFloodDeck(Engineer) playedOn game) {
            assertThat(floodDeck.toSortedSet()).isEqualTo(Location.values().subtract(listOf(Observatory)).toSortedSet())
            assertThat(floodDeck.dropLast(1)).isNotEqualTo(floodDeckDiscardBeforeEvent)
            assertThat(floodDeckDiscard).isEmpty()
        }
    }

    @Test
    fun `helicopter lift off island played on game goes into treasure discard pile and players stay put`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newShuffledMap())
                .withPlayerPosition(Engineer, Position(2, 2))
                .withPlayerCards(immMapOf(Messenger to cards(HelicopterLiftCard, earth), Engineer to cards(ocean)))
                .withTreasureDeckDiscard(cards(ocean))

        val messengerOriginalPosition = game.gameState.playerPositions.getValue(Messenger)
        val engineerOriginalPosition = game.gameState.playerPositions.getValue(Engineer)

        assertThat(game.gameState.playerPositions).isEqualTo(immMapOf(Messenger to messengerOriginalPosition, Engineer to engineerOriginalPosition))

        after (HelicopterLiftOffIsland(Messenger) playedOn game) {
            assertThat(playerPositions).isEqualTo(game.gameState.playerPositions)
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(earth), Engineer to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, HelicopterLiftCard))
        }
    }

    private inline fun <T, R> after(receiver: T, block: T.() -> R): R = with(receiver, block)

    private infix fun GameEvent.playedOn(game: Game): GameState = game.gameState.after(this, game.random)
    private infix fun List<GameEvent>.playedOn(game: Game): GameState = this.fold(game.gameState) { state, event -> state.after(event, random) }
}
