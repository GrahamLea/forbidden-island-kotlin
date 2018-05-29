package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Adventurer.Companion.randomListOfPlayers
import com.grahamlea.forbiddenisland.FloodLevel.DEAD
import com.grahamlea.forbiddenisland.Location.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import com.grahamlea.forbiddenisland.Treasure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest

@DisplayName("GameState result")
class GameStateResultTest {

    private val allLocationsUnflooded = Location.values().associate { it to Unflooded }

    @Nested
    @DisplayName("game is won")
    inner class GameWonTests {
        @RepeatedTest(100)
        fun `when HelicopterLiftOffIsland event has been played`() {
            val game = Game.newRandomGameFor(randomListOfPlayers(2)).let { game ->
                game.copy(gameState = game.gameState.copy(
                    previousEvents = immListOf(HelicopterLiftOffIsland(game.gameSetup.players[0]))
                ))
            }

            assertThat(game.gameState.result).isEqualTo(AdventurersWon)
        }

        @Nested
        @DisplayName("game is NOT won")
        inner class GameNotWonTests {
            @RepeatedTest(100)
            fun `when all four treasures are collected and all adventurers are on fool's landing and helicopter lift was most recent card played`() {
                val players = randomListOfPlayers(2)
                val game = Game.newRandomGameFor(players).let { game ->
                    game.copy(gameState = game.gameState.copy(
                        treasuresCollected = Treasure.values().associate { it to true }.imm(),
                        treasureDeck = (TreasureDeck.newShuffledDeck() - TreasureCard(EarthStone) - HelicopterLiftCard).imm(),
                        treasureDeckDiscard = immListOf(TreasureCard(EarthStone), HelicopterLiftCard),
                        playerCards = players.associate { it to immListOf<HoldableCard>() }.imm()
                    ))
                        .withPlayerLocation(players[0], FoolsLanding)
                        .withPlayerLocation(players[1], FoolsLanding)
                }

                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `just when all four treasures are collected`() {
                val players = randomListOfPlayers(2)
                val game = Game.newRandomGameFor(players).let { game ->
                    game.copy(gameState = game.gameState.copy(
                        treasuresCollected = Treasure.values().associate { it to true }.imm()))
                }

                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `when all four treasures are collected and all adventurers are on fool's landing`() {
                val players = randomListOfPlayers(2)
                val game = Game.newRandomGameFor(players).let { game ->
                    game.copy(gameState = game.gameState.copy(
                        treasuresCollected = Treasure.values().associate { it to true }.imm()))
                        .withPlayerLocation(players[0], FoolsLanding)
                        .withPlayerLocation(players[1], FoolsLanding)
                }

                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `when all four treasures are collected and one adventurers is on fool's landing and helicopter lift has been played`() {
                val players = randomListOfPlayers(2)
                val game = Game.newRandomGameFor(players).let { game ->
                    game.copy(gameState = game.gameState.copy(
                        treasuresCollected = Treasure.values().associate { it to true }.imm(),
                        treasureDeck = (TreasureDeck.newShuffledDeck() - TreasureCard(EarthStone) - HelicopterLiftCard).imm(),
                        treasureDeckDiscard = immListOf(TreasureCard(EarthStone), HelicopterLiftCard),
                        playerCards = players.associate { it to immListOf<HoldableCard>() }.imm()
                    ))
                        .withPlayerLocation(players[0], FoolsLanding)
                        .withPlayerLocation(players[1], TempleOfTheSun)
                }

                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `when three treasures are collected and all adventurers are on fool's landing and helicopter lift has been played`() {
                val players = randomListOfPlayers(2)
                val game = Game.newRandomGameFor(players).let { game ->
                    game.copy(gameState = game.gameState.copy(
                        treasuresCollected = (Treasure.values().take(3).associate { it to true } + (Treasure.values().last() to false)).imm(),
                        treasureDeck = (TreasureDeck.newShuffledDeck() - TreasureCard(EarthStone) - HelicopterLiftCard).imm(),
                        treasureDeckDiscard = immListOf(TreasureCard(EarthStone), HelicopterLiftCard),
                        playerCards = players.associate { it to immListOf<HoldableCard>() }.imm()
                    ))
                        .withPlayerLocation(players[0], FoolsLanding)
                        .withPlayerLocation(players[1], FoolsLanding)
                }

                assertThat(game.gameState.result).isNull()
            }
        }
    }

    @Nested
    @DisplayName("game is lost")
    inner class GameLostTests {
        @RepeatedTest(100)
        fun `when Fool's Landing sinks`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (FoolsLanding to Sunken)).imm())

            assertThat(gameState.result).isEqualTo(FoolsLandingSank)
        }

        @RepeatedTest(100)
        fun `when flood level reaches 10 (dead)`() {
            val gameState = Game.newRandomGameFor(4).gameState.copy(floodLevel = DEAD)

            assertThat(gameState.result).isEqualTo(MaximumWaterLevelReached)
        }

        @RepeatedTest(100)
        fun `when both pickup locations for Crystal of Fire sink`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (CaveOfEmbers to Sunken) + (CaveOfShadows to Sunken)).imm())

            assertThat(gameState.result)
                .isEqualTo(BothPickupLocationsSankBeforeCollectingTreasure(CrystalOfFire, CaveOfEmbers to CaveOfShadows))
        }

        @RepeatedTest(100)
        fun `when both pickup locations for Ocean's Chalice sink`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (CoralPalace to Sunken) + (TidalPalace to Sunken)).imm())

            assertThat(gameState.result)
                .isEqualTo(BothPickupLocationsSankBeforeCollectingTreasure(OceansChalice, CoralPalace to TidalPalace))
        }

        @RepeatedTest(100)
        fun `when both pickup locations for Statue of the Wind sink`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (HowlingGarden to Sunken) + (WhisperingGarden to Sunken)).imm())

            assertThat(gameState.result)
                .isEqualTo(BothPickupLocationsSankBeforeCollectingTreasure(StatueOfTheWind, HowlingGarden to WhisperingGarden))
        }

        @RepeatedTest(100)
        fun `when both pickup locations for Earth Stone sink`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Sunken) + (TempleOfTheSun to Sunken)).imm())

            assertThat(gameState.result)
                .isEqualTo(BothPickupLocationsSankBeforeCollectingTreasure(EarthStone, TempleOfTheMoon to TempleOfTheSun))
        }

        @RepeatedTest(100)
        fun `game is not lost when one pickup locations for a Treasure has sunken`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Sunken)).imm())

            assertThat(gameState.result).isNull()
        }

        @RepeatedTest(100)
        fun `game is not lost when one pickup locations for a Treasure has sunken and the other is flooded`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Sunken) + (TempleOfTheSun to Flooded)).imm())

            assertThat(gameState.result).isNull()
        }

        @RepeatedTest(100)
        fun `game is not lost when both pickup locations for a Treasure are flooded`() {
            val gameState = Game.newRandomGameFor(4).gameState
                .copy(locationFloodStates = (allLocationsUnflooded + (TempleOfTheMoon to Flooded) + (TempleOfTheSun to Flooded)).imm())

            assertThat(gameState.result).isNull()
        }

        @RepeatedTest(100)
        fun `when the Engineer is on a sunken location and can't swim to an adjacent one`() {
            val game = setupStrandedGameFor(Engineer, Navigator, sinkDiagonals = false)
            assertThat(game.gameState.result).isEqualTo(PlayerDrowned(Engineer))
        }

        @RepeatedTest(100)
        fun `when the Navigator is on a sunken location and can't swim to an adjacent one`() {
            val game = setupStrandedGameFor(Navigator, Engineer, sinkDiagonals = false)
            assertThat(game.gameState.result).isEqualTo(PlayerDrowned(Navigator))
        }

        @RepeatedTest(100)
        fun `when the Messenger is on a sunken location and can't swim to an adjacent one`() {
            val game = setupStrandedGameFor(Messenger, Engineer, sinkDiagonals = false)
            assertThat(game.gameState.result).isEqualTo(PlayerDrowned(Messenger))
        }

        @RepeatedTest(100)
        fun `when the Explorer is on a sunken location with all adjacent and diagonal ones sunken`() {
            val game = setupStrandedGameFor(Explorer, Navigator, sinkDiagonals = true)
            assertThat(game.gameState.result).isEqualTo(PlayerDrowned(Explorer))
        }

        @Nested
        @DisplayName("game is NOT lost")
        inner class GameNotLostTests {
            @RepeatedTest(100)
            fun `game is NOT lost when the Explorer is on a sunken location with only adjacent ones sunken`() {
                val game = setupStrandedGameFor(Explorer, Navigator, sinkDiagonals = false)
                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `game is NOT lost when the Explorer is on a sunken location with all adjacent and SOME diagonal ones sunken`() {
                val sunkPositionWithSunkSurroundings = Position(3, 3)
                val safePosition = Position(5, 5)
                val positionsToSink: List<Position> = positionsFromMap("""
                  ..
                     oo..
                    .ooo..
                    .ooo..
                     ....
                      ..
                """)

                val game = createGameForSunkPlayerScenario(sunkPositionWithSunkSurroundings, Explorer, positionsToSink, Navigator, safePosition)

                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `game is NOT lost when the Diver is on a sunken location with all adjacent ones sunken`() {
                val game = setupStrandedGameFor(Diver, Navigator, sinkDiagonals = false)
                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `game is NOT lost when the Pilot is on a sunken location with all adjacent ones sunken`() {
                val game = setupStrandedGameFor(Pilot, Navigator, sinkDiagonals = false)
                assertThat(game.gameState.result).isNull()
            }

            @RepeatedTest(100)
            fun `game is NOT lost when a player is on a sunken location and CAN swim to at least one adjacent one`() {
                val sunkPositionWithSunkSurroundings = Position(3, 3)
                val safePosition = Position(5, 5)
                val positionsToSink = positionsFromMap("""
              ..
             .o..
            .oo...
            ..o...
             ....
              ..
        """)

                val game = createGameForSunkPlayerScenario(sunkPositionWithSunkSurroundings, Engineer, positionsToSink, Navigator, safePosition)

                assertThat(game.gameState.result).isNull()
            }
        }
    }

    private fun setupStrandedGameFor(playerToStrand: Adventurer, otherPlayer: Adventurer, sinkDiagonals: Boolean): Game {
        val sunkPositionWithSunkSurroundings = Position(3, 3)
        val safePosition = Position(5, 5)
        val positionsToSink: List<Position> = if (sinkDiagonals) {
            positionsFromMap("""
                  ..
                 ooo.
                .ooo..
                .ooo..
                 ....
                  ..
            """)
        } else {
            positionsFromMap("""
                  ..
                 .o..
                .ooo..
                ..o...
                 ....
                  ..
            """)
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
