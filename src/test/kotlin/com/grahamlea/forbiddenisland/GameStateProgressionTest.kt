package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Location.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import com.grahamlea.forbiddenisland.Treasure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("GameState progressions following actions")
class GameStateProgressionTest {

    private val random = Random()
    private val earth = TreasureCard(EarthStone)
    private val ocean = TreasureCard(OceansChalice)
    private val fire = TreasureCard(CrystalOfFire)

    @Test
    fun `actions on game state are recorded in previous actions`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withPositionFloodStates(Flooded, Position(2, 3))

        val action1 = Move(Engineer, Position(4, 3))
        val action2 = Move(Engineer, Position(3, 3))
        val action3 = ShoreUp(Engineer, Position(2, 3))

        after (listOf(action1, action2, action3) playedOn game) {
            assertThat(previousActions).isEqualTo(immListOf<GameAction>(action1, action2, action3))
        }
    }

    @Test
    fun `move played on game changes position of the one player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerPosition(Engineer, Position(4, 4))

        val engineerOriginalPosition = game.gameState.positionOf(Engineer)
        val messengerOriginalPosition = game.gameState.positionOf(Messenger)

        assertThat(game.gameState.playerPositions).isEqualTo(immMapOf(Engineer to engineerOriginalPosition, Messenger to messengerOriginalPosition))

        val engineerNewPosition = Position(4, 3)
        after (Move(Engineer, engineerNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(Engineer to engineerNewPosition, Messenger to messengerOriginalPosition))
        }
    }

    @Test
    fun `fly played on game changes position of the Pilot`() {
        val pilotOriginalPosition = Position(4, 4)
        val game = Game.newRandomGameFor(immListOf(Pilot, Messenger), GameMap.newRandomMap())
                .withPlayerPosition(Pilot, pilotOriginalPosition)

        val messengerOriginalPosition = game.gameState.positionOf(Messenger)

        val pilotNewPosition = Position(1, 3)
        after (Fly(Pilot, pilotNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(Pilot to pilotNewPosition, Messenger to messengerOriginalPosition))
        }
    }

    @Test
    fun `swim action played on game changes position of the one player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger),
                    GameMap.newRandomMap().withLocationNotAtAnyOf(FoolsLanding, listOf(Position(4, 4))
                ))
                .withPlayerPosition(Engineer, Position(4, 4))
                .withPositionFloodStates(Sunken, Position(4, 4))
                .withGamePhase(AwaitingPlayerToSwimToSafety(Engineer, AwaitingPlayerAction(Messenger, 2)))

        val engineerOriginalPosition = game.gameState.positionOf(Engineer)
        val messengerOriginalPosition = game.gameState.positionOf(Messenger)

        assertThat(game.gameState.playerPositions).isEqualTo(immMapOf(Engineer to engineerOriginalPosition, Messenger to messengerOriginalPosition))

        val engineerNewPosition = Position(4, 3)
        after (SwimToSafety(Engineer, engineerNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(Engineer to engineerNewPosition, Messenger to messengerOriginalPosition))
        }
    }

    @Test
    fun `shore up played on game changes location flood state`() {
        val positionToShoreUp = Position(3, 4)
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withPositionFloodStates(Flooded, positionToShoreUp)

        val mapSiteToShoreUp = game.gameSetup.mapSiteAt(positionToShoreUp)

        assertThat(game.gameState.locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Flooded)

        after (ShoreUp(Engineer, positionToShoreUp) playedOn game) {
            assertThat(locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Unflooded)
        }
    }

    @Test
    fun `shore up with two positions played on game changes location flood state of both positions`() {
        val positionToShoreUp = Position(3, 4)
        val position2ToShoreUp = Position(5, 4)
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withPositionFloodStates(Flooded, positionToShoreUp, position2ToShoreUp)

        val mapSiteToShoreUp = game.gameSetup.mapSiteAt(positionToShoreUp)
        val mapSite2ToShoreUp = game.gameSetup.mapSiteAt(position2ToShoreUp)

        assertThat(game.gameState.locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Flooded)
        assertThat(game.gameState.locationFloodStates[mapSite2ToShoreUp.location]).isEqualTo(Flooded)

        after (ShoreUp(Engineer, positionToShoreUp, position2ToShoreUp) playedOn game) {
            assertThat(locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Unflooded)
            assertThat(locationFloodStates[mapSite2ToShoreUp.location]).isEqualTo(Unflooded)
        }
    }

    @Test
    fun `give treasure card action on game state changes player cards`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newRandomMap())
                .withPlayerCards(Messenger to cards(earth, earth), Engineer to cards(earth, earth))

        after (GiveTreasureCard(Messenger, Engineer, earth) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(earth), Engineer to cards(earth, earth, earth)))
        }
    }

    @Test
    fun `capture treasure action captures treasure, and discards treasure cards`() {
        val gameSetup = GameSetup(immListOf(Messenger, Engineer), GameMap.newRandomMap())
        val game = Game.newRandomGameFor(gameSetup)
                .withPlayerLocation(Messenger, TempleOfTheSun)
                .withPlayerCards(
                    immMapOf(Messenger to cards(earth, earth, earth, earth, ocean),
                             Engineer to cards(ocean, ocean)))
                .withTreasureDeckDiscard(cards(ocean))
                .withTreasuresCollected(OceansChalice)

        assertThat(game.gameState.treasuresCollected).containsAllEntriesOf(mapOf(
            OceansChalice to true,
            EarthStone to false,
            StatueOfTheWind to false,
            CrystalOfFire to false)
        )

        after (CaptureTreasure(Messenger, EarthStone) playedOn game) {
            assertThat(treasuresCollected).containsAllEntriesOf(mapOf(
                OceansChalice to true,
                EarthStone to true,
                StatueOfTheWind to false,
                CrystalOfFire to false)
            )
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(ocean), Engineer to cards(ocean, ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, earth, earth, earth, earth))
        }
    }

    @Test
    fun `helicopter lift played on game changes position of the players and discards the card`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer, Explorer, Diver), GameMap.newRandomMap())
                .withPlayerPosition(Engineer, Position(2, 2))
                .withPlayerPosition(Explorer, Position(2, 2))
                .withPlayerPosition(Diver, Position(2, 2))
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard, earth),
                    Engineer to cards(ocean),
                    Explorer to cards(earth),
                    Diver to cards(fire)
                )
                .withTreasureDeckDiscard(cards(ocean))

        val messengerOriginalPosition = game.gameState.positionOf(Messenger)
        val engineerOriginalPosition = game.gameState.positionOf(Engineer)
        val explorerOriginalPosition = game.gameState.positionOf(Explorer)
        val diverOriginalPosition = game.gameState.positionOf(Diver)

        assertThat(game.gameState.playerPositions).isEqualTo(immMapOf(
            Messenger to messengerOriginalPosition,
            Engineer to engineerOriginalPosition,
            Explorer to explorerOriginalPosition,
            Diver to diverOriginalPosition
        ))

        val engineerAndExplorerNewPosition = Position(5, 5)
        after (HelicopterLift(Messenger, immSetOf(Engineer, Explorer), engineerAndExplorerNewPosition) playedOn game) {
            assertThat(playerPositions).isEqualTo(immMapOf(
                Messenger to messengerOriginalPosition,
                Engineer to engineerAndExplorerNewPosition,
                Explorer to engineerAndExplorerNewPosition,
                Diver to diverOriginalPosition
            ))
            assertThat(playerCards).isEqualTo(immMapOf(
                Messenger to cards(earth),
                Engineer to cards(ocean),
                Explorer to cards(earth),
                Diver to cards(fire)
            ))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, HelicopterLiftCard))
        }
    }

    @Test
    fun `sandbag played on game shores up location and discards the card`() {
        val positionToShoreUp = Position(2, 2)
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerPosition(Engineer, Position(4, 4))
                .withPlayerPosition(Messenger, Position(3, 4))
                .withPositionFloodStates(Flooded, positionToShoreUp)
                .withPlayerCards(Engineer to cards(earth, SandbagsCard), Messenger to cards(ocean))
                .withTreasureDeckDiscard(cards(earth))

        val mapSiteToShoreUp = game.gameSetup.mapSiteAt(positionToShoreUp)

        assertThat(game.gameState.locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Flooded)

        after (Sandbag(Engineer, positionToShoreUp) playedOn game) {
            assertThat(locationFloodStates[mapSiteToShoreUp.location]).isEqualTo(Unflooded)
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(earth, SandbagsCard))
        }
    }

    @Test
    fun `discard card on game changes just discards the card`() {
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), GameMap.newRandomMap())
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard, earth, earth, earth, earth, ocean),
                    Engineer to cards(ocean)
                )
                .withTreasureDeckDiscard(cards(ocean))
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Engineer, 1)))

        after (DiscardCard(Messenger, earth) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(
                Messenger to cards(HelicopterLiftCard, earth, earth, earth, ocean),
                Engineer to cards(ocean)
            ))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, earth))
            assertThat(playerPositions).isEqualTo(game.gameState.playerPositions)
            assertThat(treasureDeck).isEqualTo(game.gameState.treasureDeck)
        }
    }

    @Test
    fun `draw treasure card from treasure deck moves card from treasure deck to player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerCards(Engineer to cards(earth), Messenger to cards(ocean))
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
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerCards(Engineer to cards(earth), Messenger to cards(ocean))
                .withTopOfTreasureDeck(SandbagsCard)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth, SandbagsCard), Messenger to cards(ocean)))
        }
    }

    @Test
    fun `draw Helicopter Lift from treasure deck moves card from treasure deck to player`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerCards(Engineer to cards(earth), Messenger to cards(ocean))
                .withTopOfTreasureDeck(HelicopterLiftCard)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth, HelicopterLiftCard), Messenger to cards(ocean)))
        }
    }

    @Test
    fun `drawing last card from treasure deck shuffles treasure discard back to deck`() {
        val treasureDeckDiscardBeforeAction = TreasureDeck.newShuffledDeck().subtract(listOf(earth))
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerCards(Engineer to cards(), Messenger to cards())
                .withTreasureDeckDiscard(treasureDeckDiscardBeforeAction)

        assertThat(game.gameState.treasureDeck).isEqualTo(cards(earth))

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards()))
            assertThat(treasureDeck)
                .hasSize(TreasureDeck.newShuffledDeck().size - 1) // One card dealt to Engineer
                .isNotEqualTo(treasureDeckDiscardBeforeAction)
            assertThat(treasureDeckDiscard).isEqualTo(cards())
        }
    }

    @Test
    fun `drawing waters rise card from treasure deck raises flood level, discards card, and shuffles flood deck discard back to flood deck`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerCards(Engineer to cards(earth), Messenger to cards(ocean))
                .withTopOfTreasureDeck(WatersRiseCard)

        val floodDeckBeforeAction = game.gameState.floodDeck
        val floodDeckDiscardBeforeAction = game.gameState.floodDeckDiscard

        assertThat(game.gameState.floodLevel).isEqualTo(FloodLevel.TWO)
        assertThat(floodDeckDiscardBeforeAction).hasSize(6)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(floodLevel).isEqualTo(FloodLevel.THREE)
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(WatersRiseCard))
            assertThat(floodDeckDiscard).isEmpty()
            assertThat(floodDeck.take(6))
                .containsOnlyElementsOf(floodDeckDiscardBeforeAction)
                .isNotEqualTo(floodDeckDiscardBeforeAction)
            assertThat(floodDeck.drop(6)).isEqualTo(floodDeckBeforeAction)
        }
    }

    @Test
    fun `drawing waters rise card as last card from treasure deck does all the expected things from the two test cases above`() {
        val treasureDeckDiscardBeforeAction = TreasureDeck.newShuffledDeck().subtract(listOf(earth, ocean, WatersRiseCard))
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), GameMap.newRandomMap())
                .withPlayerCards(Engineer to cards(earth), Messenger to cards(ocean))
                .withTreasureDeckDiscard(treasureDeckDiscardBeforeAction)

        val floodDeckBeforeAction = game.gameState.floodDeck
        val floodDeckDiscardBeforeAction = game.gameState.floodDeckDiscard

        assertThat(game.gameState.floodLevel).isEqualTo(FloodLevel.TWO)
        assertThat(floodDeckDiscardBeforeAction).hasSize(6)

        after (DrawFromTreasureDeck(Engineer) playedOn game) {
            assertThat(floodLevel).isEqualTo(FloodLevel.THREE)
            assertThat(playerCards).isEqualTo(immMapOf(Engineer to cards(earth), Messenger to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards())
            assertThat(floodDeckDiscard).isEmpty()
            assertThat(floodDeck.take(6))
                .containsOnlyElementsOf(floodDeckDiscardBeforeAction)
                .isNotEqualTo(floodDeckDiscardBeforeAction)
            assertThat(floodDeck.drop(6)).isEqualTo(floodDeckBeforeAction)
            assertThat(treasureDeck)
                .hasSize(TreasureDeck.newShuffledDeck().size - 2) // Players have two cards
                .isNotEqualTo(treasureDeckDiscardBeforeAction)
            assertThat(treasureDeckDiscard).isEqualTo(cards())
        }
    }

    @Test
    fun `draw location from flood deck floods or sinks location and discards card`() {
        val map = GameMap.newRandomMap()
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), map)
                .withPositionFloodStates(Unflooded, Position.allPositions)
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
        val floodDeckDiscardBeforeAction = shuffled<Location>().subtract(listOf(MistyMarsh)) - Observatory
        val map = GameMap.newRandomMap()
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger), map)
                    .withPositionFloodStates(Unflooded, Position.allPositions)
                    .withLocationFloodStates(Flooded, DunesOfDeception)
                    .withLocationFloodStates(Sunken, Observatory)
                    .withFloodDeckDiscard(floodDeckDiscardBeforeAction.imm())
                    .withGamePhase(AwaitingFloodDeckDraw(Engineer, 3))

        assertThat(game.gameState.floodDeck).isEqualTo(immListOf(MistyMarsh))
        assertThat(game.gameState.floodDeckDiscard).hasSize(Location.values().size - 2) // Observatory is out bc its sunken

        after (DrawFromFloodDeck(Engineer) playedOn game) {
            assertThat(floodDeck).containsOnlyElementsOf(Location.values().toList() - Observatory)
            assertThat(floodDeck.dropLast(1)).isNotEqualTo(floodDeckDiscardBeforeAction)
            assertThat(floodDeckDiscard).isEmpty()
        }
    }

    @Test
    fun `helicopter lift off island played on game goes into treasure discard pile and players stay put`() {
        val map = GameMap.newRandomMap()
        val game = Game.newRandomGameFor(immListOf(Messenger, Engineer), map)
                .withPlayerPosition(Messenger, map.positionOf(FoolsLanding))
                .withPlayerPosition(Engineer, map.positionOf(FoolsLanding))
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard, earth),
                    Engineer to cards(ocean))
                .withTreasureDeckDiscard(cards(ocean))
                .withTreasuresCollected(*Treasure.values())

        val messengerOriginalPosition = game.gameState.positionOf(Messenger)
        val engineerOriginalPosition = game.gameState.positionOf(Engineer)

        assertThat(game.gameState.playerPositions).isEqualTo(
            immMapOf(Messenger to messengerOriginalPosition, Engineer to engineerOriginalPosition)
        )

        after (HelicopterLiftOffIsland(Messenger) playedOn game) {
            assertThat(playerPositions).isEqualTo(game.gameState.playerPositions)
            assertThat(playerCards).isEqualTo(immMapOf(Messenger to cards(earth), Engineer to cards(ocean)))
            assertThat(treasureDeckDiscard).isEqualTo(cards(ocean, HelicopterLiftCard))
        }
    }

    private inline fun <T, R> after(receiver: T, block: T.() -> R): R = with(receiver, block)

    private infix fun GameAction.playedOn(game: Game): GameState = game.gameState.nextStateAfter(this, Random())
    private infix fun List<GameAction>.playedOn(game: Game): GameState = this.fold(game.gameState) { state, action -> state.nextStateAfter(action, random) }
}
