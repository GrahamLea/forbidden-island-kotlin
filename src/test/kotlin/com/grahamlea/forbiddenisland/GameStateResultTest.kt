package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Adventurer.Companion.randomListOfPlayers
import com.grahamlea.forbiddenisland.FloodLevel.DEAD
import com.grahamlea.forbiddenisland.Location.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import com.grahamlea.forbiddenisland.Treasure.*
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.hamcrest.CoreMatchers.`is` as is_

@RunWith(Parameterized::class)
class GameStateResultTest {

    companion object {
        // Some of these tests setup fairly complex scenarios on top of random data, so we run them 100 times each
        // to ensure that the implementation passes for all cases
        private const val numberOfTimesToRunTests = 100

        @JvmStatic @Parameterized.Parameters
        fun data(): Array<Array<Any>> {
            return Array(numberOfTimesToRunTests) { Array<Any>(0) {} }
        }
    }

    private val allLocationsUnflooded = Location.values().associate { it to Unflooded }

    @Test
    fun `game is won when HelicopterLiftOffIsland event has been played`() {
        val game = Game.newRandomGameFor(randomListOfPlayers(2)).let { game ->
            game.copy(gameState = game.gameState.copy(
                    previousEvents = immListOf(HelicopterLiftOffIsland(game.gameSetup.players[0]))
            ))
        }

        assertThat(game.gameState.result, is_(AdventurersWon as GameResult))
    }

    @Test
    fun `game is not won when all four treasures are collected and all adventurers are on fool's landing and helicopter lift was most recent card played`() {
        val players = randomListOfPlayers(2)
        val game = Game.newRandomGameFor(players).let { game ->
            game.copy(gameState = game.gameState.copy(
                    treasuresCollected = Treasure.values().associate { it to true }.imm(),
                    treasureDeck = (TreasureDeck.newShuffledDeck() - TreasureCard(EarthStone) - HelicopterLiftCard).imm(),
                    treasureDeckDiscard = immListOf(TreasureCard(EarthStone), HelicopterLiftCard),
                    playerCards = players.associate { it to immListOf<HoldableCard>() }.imm()
            ))
                .withPlayerPosition(players[0], game.gameSetup.map.positionOf(FoolsLanding))
                .withPlayerPosition(players[1], game.gameSetup.map.positionOf(FoolsLanding))
        }

        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is not won just when all four treasures are collected`() {
        val players = randomListOfPlayers(2)
        val game = Game.newRandomGameFor(players).let { game ->
            game.copy(gameState = game.gameState.copy(
                    treasuresCollected = Treasure.values().associate { it to true }.imm()))
        }

        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is not won when all four treasures are collected and all adventurers are on fool's landing`() {
        val players = randomListOfPlayers(2)
        val game = Game.newRandomGameFor(players).let { game ->
            game.copy(gameState = game.gameState.copy(
                    treasuresCollected = Treasure.values().associate { it to true }.imm()))
                .withPlayerPosition(players[0], game.gameSetup.map.positionOf(FoolsLanding))
                .withPlayerPosition(players[1], game.gameSetup.map.positionOf(FoolsLanding))
        }

        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is not won when all four treasures are collected and one adventurers is on fool's landing and helicopter lift has been played`() {
        val players = randomListOfPlayers(2)
        val game = Game.newRandomGameFor(players).let { game ->
            game.copy(gameState = game.gameState.copy(
                    treasuresCollected = Treasure.values().associate { it to true }.imm(),
                    treasureDeck = (TreasureDeck.newShuffledDeck() - TreasureCard(EarthStone) - HelicopterLiftCard).imm(),
                    treasureDeckDiscard = immListOf(TreasureCard(EarthStone), HelicopterLiftCard),
                    playerCards = players.associate { it to immListOf<HoldableCard>() }.imm()
            ))
                .withPlayerPosition(players[0], game.gameSetup.map.positionOf(FoolsLanding))
                .withPlayerPosition(players[1], game.gameSetup.map.positionOf(TempleOfTheSun))
        }

        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is not won when three treasures are collected and all adventurers are on fool's landing and helicopter lift has been played`() {
        val players = randomListOfPlayers(2)
        val game = Game.newRandomGameFor(players).let { game ->
            game.copy(gameState = game.gameState.copy(
                    treasuresCollected = (Treasure.values().take(3).associate { it to true } + (Treasure.values().last() to false)).imm(),
                    treasureDeck = (TreasureDeck.newShuffledDeck() - TreasureCard(EarthStone) - HelicopterLiftCard).imm(),
                    treasureDeckDiscard = immListOf(TreasureCard(EarthStone), HelicopterLiftCard),
                    playerCards = players.associate { it to immListOf<HoldableCard>() }.imm()
            ))
                .withPlayerPosition(players[0], game.gameSetup.map.positionOf(FoolsLanding))
                .withPlayerPosition(players[1], game.gameSetup.map.positionOf(FoolsLanding))
        }

        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is lost when Fool's Landing sinks`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (FoolsLanding to Sunken)).imm())

        assertThat(gameState.result, is_(FoolsLandingSank as GameResult))
    }

    @Test
    fun `game is lost when flood level reaches 10 (dead)`() {
        val gameState = Game.newRandomGameFor(4).gameState.copy(floodLevel = DEAD)

        assertThat(gameState.result, is_(MaximumWaterLevelReached as GameResult))
    }

    @Test
    fun `game is lost when both pickup locations for Crystal of Fire sink`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (CaveOfEmbers to Sunken) + (CaveOfShadows to Sunken)).imm())

        assertThat(gameState.result,
                is_(BothPickupLocationsSankBeforeCollectingTreasure(CrystalOfFire, CaveOfEmbers to CaveOfShadows) as GameResult))
    }

    @Test
    fun `game is lost when both pickup locations for Ocean's Chalice sink`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (CoralPalace to Sunken) + (TidalPalace to Sunken)).imm())

        assertThat(gameState.result,
                is_(BothPickupLocationsSankBeforeCollectingTreasure(OceansChalice, CoralPalace to TidalPalace) as GameResult))
    }

    @Test
    fun `game is lost when both pickup locations for Statue of the Wind sink`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (HowlingGarden to Sunken) + (WhisperingGarden to Sunken)).imm())

        assertThat(gameState.result,
                is_(BothPickupLocationsSankBeforeCollectingTreasure(StatueOfTheWind, HowlingGarden to WhisperingGarden) as GameResult))
    }

    @Test
    fun `game is lost when both pickup locations for Earth Stone sink`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Sunken) + (TempleOfTheSun to Sunken)).imm())

        assertThat(gameState.result,
                is_(BothPickupLocationsSankBeforeCollectingTreasure(EarthStone, TempleOfTheMoon to TempleOfTheSun) as GameResult))
    }

    @Test
    fun `game is not lost when one pickup locations for a Treasure has sunken`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Sunken)).imm())

        assertThat(gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is not lost when one pickup locations for a Treasure has sunken and the other is flooded`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Sunken) + (TempleOfTheSun to Flooded)).imm())

        assertThat(gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is not lost when both pickup locations for a Treasure are flooded`() {
        val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Flooded) + (TempleOfTheSun to Flooded)).imm())

        assertThat(gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is lost when the Engineer is on a sunken location and can't swim to an adjacent one`() {
        val game = setupStrandedGameFor(Engineer, Navigator, sinkDiagonals = false)
        assertThat(game.gameState.result, is_(PlayerDrowned(Engineer) as GameResult))
    }

    @Test
    fun `game is lost when the Navigator is on a sunken location and can't swim to an adjacent one`() {
        val game = setupStrandedGameFor(Navigator, Engineer, sinkDiagonals = false)
        assertThat(game.gameState.result, is_(PlayerDrowned(Navigator) as GameResult))
    }

    @Test
    fun `game is lost when the Messenger is on a sunken location and can't swim to an adjacent one`() {
        val game = setupStrandedGameFor(Messenger, Engineer, sinkDiagonals = false)
        assertThat(game.gameState.result, is_(PlayerDrowned(Messenger) as GameResult))
    }

    @Test
    fun `game is lost when the Explorer is on a sunken location with all adjacent and diagonal ones sunken`() {
        val game = setupStrandedGameFor(Explorer, Navigator, sinkDiagonals = true)
        assertThat(game.gameState.result, is_(PlayerDrowned(Explorer) as GameResult))
    }

    @Test
    fun `game is NOT lost when the Explorer is on a sunken location with only adjacent ones sunken`() {
        val game = setupStrandedGameFor(Explorer, Navigator, sinkDiagonals = false)
        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is NOT lost when the Explorer is on a sunken location with all adjacent and SOME diagonal ones sunken`() {
        val sunkPositionWithSunkSurroundings = Position(3, 3)
        val safePosition = Position(5, 5)
        val positionsToSink: List<Position> =
            listOf(
                    Position(2, 2), Position(3, 2),
                    Position(2, 3), Position(3, 3), Position(4, 3),
                    Position(2, 4), Position(3, 4), Position(4, 4)
            )

        val game = createGameForSunkPlayerScenario(sunkPositionWithSunkSurroundings, Explorer, positionsToSink, Navigator, safePosition)

        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is NOT lost when the Diver is on a sunken location with all adjacent ones sunken`() {
        val game = setupStrandedGameFor(Diver, Navigator, sinkDiagonals = false)
        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is NOT lost when the Pilot is on a sunken location with all adjacent ones sunken`() {
        val game = setupStrandedGameFor(Pilot, Navigator, sinkDiagonals = false)
        assertThat(game.gameState.result, is_(nullValue()))
    }

    @Test
    fun `game is NOT lost when a player is on a sunken location and CAN swim to at least one adjacent one`() {
        val sunkPositionWithSunkSurroundings = Position(3, 3)
        val safePosition = Position(5, 5)
        val positionsToSink = listOf(
                                Position(3, 2),
                Position(2, 3), Position(3, 3),
                                Position(3, 4)
        )

        val game = createGameForSunkPlayerScenario(sunkPositionWithSunkSurroundings, Engineer, positionsToSink, Navigator, safePosition)

        assertThat(game.gameState.result, is_(nullValue()))
    }

    private fun setupStrandedGameFor(playerToStrand: Adventurer, otherPlayer: Adventurer, sinkDiagonals: Boolean): Game {
        val sunkPositionWithSunkSurroundings = Position(3, 3)
        val safePosition = Position(5, 5)
        val positionsToSink: List<Position> = if (sinkDiagonals) {
            listOf(
                    Position(2, 2), Position(3, 2), Position(4, 2),
                    Position(2, 3), Position(3, 3), Position(4, 3),
                    Position(2, 4), Position(3, 4), Position(4, 4)
            )
        } else {
            listOf(
                    Position(3, 2),
                    Position(2, 3), Position(3, 3), Position(4, 3),
                    Position(3, 4)
            )
        }

        return createGameForSunkPlayerScenario(sunkPositionWithSunkSurroundings, playerToStrand, positionsToSink, otherPlayer, safePosition)
    }

    private fun createGameForSunkPlayerScenario(sunkPositionWithSunkSurroundings: Position, playerToStrand: Adventurer, positionsToSink: List<Position>, otherPlayer: Adventurer, safePosition: Position): Game {
        var game: Game
        do {
            val gameMap = GameMap.newShuffledMap().withLocationNotAtAnyOf(FoolsLanding, positionsToSink)

            game = Game.newRandomGameFor(immListOf(playerToStrand, otherPlayer), gameMap)
                    .withPlayerPosition(playerToStrand, sunkPositionWithSunkSurroundings)
                    .withPlayerPosition(otherPlayer, safePosition)
                    .withLocationFloodStates(Sunken, *positionsToSink.toTypedArray())

            // Sinking 5 Locations means we regularly trip this other end-game rule, so we iterate 'til we don't:
        } while (game.gameState.result is BothPickupLocationsSankBeforeCollectingTreasure)
        return game
    }

}
