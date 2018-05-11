package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Game.Companion.newRandomGameFor
import com.grahamlea.forbiddenisland.LocationFloodState.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*

@DisplayName("GameState available actions")
class GameStateAvailableActionsTest {

    private val random = Random()
    private val earth = TreasureCard(Treasure.EarthStone)
    private val ocean = TreasureCard(Treasure.OceansChalice)
    private val fire = TreasureCard(Treasure.CrystalOfFire)

    @Nested
    @DisplayName("Move actions")
    inner class MoveTests {

        @Nested
        inner class General {
            @Test
            fun `Engineer, Messenger, Diver and Pilot can walk to any adjacent positions`() {
                val game = game(Engineer, Messenger, Diver, Pilot).withLocationFloodStates(Unflooded, *Location.values())

                for (player in game.gameSetup.players) {
                    val availableMoves =
                        game.withPlayerPosition(player, Position(4, 4))
                            .withGamePhase(AwaitingPlayerAction(player, 3))
                            .availableMoves()

                    assertThat(availableMoves).containsOnlyElementsOf(positionsFromMap("""
                          ..
                         ....
                        ...o..
                        ..o.o.
                         ..o.
                          ..
                        """).map { Move(player, it) }
                    )
                }
            }

            @Test
            fun `Any player can move to flooded locations, but no player can move to sunken`() {
                val playerPosition = Position(4, 4)
                val floodedPosition = Position(3, 4)
                val sunkenPosition = Position(5, 4)
                testFloodRelatedMoves(playerPosition, floodedPosition, sunkenPosition, Diver, Engineer, Explorer)
                testFloodRelatedMoves(playerPosition, floodedPosition, sunkenPosition, Messenger, Navigator, Pilot)
            }

            private fun testFloodRelatedMoves(playerPosition: Position, floodedPosition: Position, sunkenPosition: Position, vararg players: Adventurer) {
                val game = game(*players)
                    .withLocationFloodStates(Flooded, floodedPosition)
                    .withLocationFloodStates(Sunken, sunkenPosition)

                for (player in game.gameSetup.players) {
                    val availableMoves =
                        game.withPlayerPosition(player, playerPosition)
                            .withGamePhase(AwaitingPlayerAction(player, 3))
                            .availableMoves()

                    assertThat(availableMoves)
                        .contains(Move(player, floodedPosition))
                        .doesNotContain(Move(player, sunkenPosition))
                }
            }
        }

        @Nested
        @DisplayName("Explorer")
        inner class ExplorerTests {
            @Test
            fun `Explorer can walk to diagonal positions as well as adjacent`() {
                val game = game(Explorer, Pilot)
                    .withPlayerPosition(Explorer, Position(4, 4))

                assertThat(game.availableMoves()).containsOnlyElementsOf(positionsFromMap("""
                      ..
                     ....
                    ..ooo.
                    ..o.o.
                     .ooo
                      ..
                    """).map { Move(Explorer, it) }
                )
            }

            @Test
            fun `Explorer can move to flooded diagonal location but not sunken`() {
                val floodedPosition = Position(3, 5)
                val sunkenPosition = Position(5, 3)
                val game = game(Explorer, Messenger)
                    .withPlayerPosition(Explorer, Position(4, 4))
                    .withLocationFloodStates(Flooded, floodedPosition)
                    .withLocationFloodStates(Sunken, sunkenPosition)

                assertThat(game.gameState.availableActions)
                    .contains(Move(Explorer, floodedPosition))
                    .doesNotContain(Move(Explorer, sunkenPosition))
            }
        }

        @Nested
        @DisplayName("Navigator")
        inner class NavigatorTests {
            @Test
            fun `Navigator can move other players up to 2 tiles, including changing directions`() {
                val game = game(Navigator, Engineer)
                    .withPlayerPosition(Navigator, Position(4, 4))
                    .withPlayerPosition(Engineer, Position(3, 3))

                assertThat(game.availableMoves(Engineer)).containsOnlyElementsOf(positionsFromMap("""
                      o.
                     ooo.
                    oo.oo.
                    .ooo..
                     .o..
                      ..
                """)
                )
            }

            @Test
            fun `Navigator themselves can only move to adjacent tiles`() {
                val game = game(Navigator, Messenger)
                    .withPlayerPosition(Navigator, Position(4, 4))

                assertThat(game.availableMoves(Navigator)).containsOnlyElementsOf(positionsFromMap("""
                      ..
                     ....
                    ...o..
                    ..o.o.
                     ..o.
                      ..
                    """)
                )
            }

            @Test
            fun `Navigator can move the Explorer diagonally`() {
                val game = game(Navigator, Explorer)
                    .withPlayerPosition(Explorer, Position(3, 3))

                val expectedExplorerOptions = positionsFromMap("""
                      oo
                     oooo
                    oo.oo.
                    ooooo.
                     oooo
                      ..
                """)

                assertThat(game.availableMoves(Explorer)).containsOnlyElementsOf(expectedExplorerOptions)
            }

            @Test
            fun `Navigator can move the Diver through 1 sunken tile`() {
                val game = game(Navigator, Diver)
                    .withPlayerPosition(Diver, Position(3, 3))
                    .withLocationFloodStates(Sunken, Position(4, 3))

                assertThat(game.gameState.availableActions).contains(Move(Diver, Position(5, 3)))
            }

            @Test
            fun `Navigator cannot move the Diver through more than 1 sunken tile`() {
                val game = game(Navigator, Diver)
                    .withPlayerPosition(Diver, Position(3, 3))
                    .withLocationFloodStates(Sunken, Position(4, 3), Position(5, 3))

                assertThat(game.gameState.availableActions).doesNotContain(Move(Diver, Position(6, 3)))
            }

            @Test
            fun `Navigator cannot land the anyone (incl the Diver) on flooded tiles`() {
                val game = game(Navigator, Diver)
                    .withPlayerPosition(Diver, Position(3, 3))
                    .withLocationFloodStates(Sunken, Position(5, 3))

                assertThat(game.gameState.availableActions).doesNotContain(Move(Diver, Position(5, 3)))
            }

            @Test
            fun `Navigator cannot move non-Diver players through flooded tiles`() {
                val game = game(Navigator, Engineer)
                    .withPlayerPosition(Engineer, Position(3, 3))
                    .withLocationFloodStates(Sunken, Position(4, 3))

                assertThat(game.gameState.availableActions).doesNotContain(Move(Engineer, Position(5, 3)))
            }
        }

        @Nested
        @DisplayName("Diver")
        inner class DiverTests {
            @Test
            fun `Diver can move through adjacent flooded and sunken tiles in one action, but can't land on sunken tiles`() {

                val game = game(Diver, Explorer)
                    .withPlayerPosition(Diver, Position(1, 4))
                    .withLocationFloodStates(Unflooded, *Position.allPositions.toTypedArray())
                    .withLocationFloodStates(Sunken, Position(2, 4), Position(4, 4))
                    .withLocationFloodStates(Flooded, Position(3, 4), Position(5, 4))

                assertThat(game.availableMoves())
                    .contains(Move(Diver, Position(3, 4)))
                    .contains(Move(Diver, Position(5, 4)))
                    .contains(Move(Diver, Position(6, 4)))
                    .doesNotContain(Move(Diver, Position(2, 4)))
                    .doesNotContain(Move(Diver, Position(4, 4)))
                    .containsAll((1..5).map { Move(Diver, Position(it, 3)) }) // Almost everything in the row above/**/...
                    .containsAll((2..5).map { Move(Diver, Position(it, 5)) }) // ... and the row below
                    .doesNotContain(Move(Diver, Position(1, 4)))
            }

            @Test
            fun `Diver can change direction while moving through adjacent flooded and sunken tiles`() {

                val diverPosition = Position(1, 4) // bottom-left

                val floodedPositions = positionsFromMap("""
                      o.
                     oo..
                    oo....
                    ......
                     ....
                      ..
                """)

                val game = game(Diver, Explorer)
                    .withPlayerPosition(Diver, diverPosition)
                    .withLocationFloodStates(Flooded, *floodedPositions.toTypedArray())

                assertThat(game.availableMoves()).contains(Move(Diver, Position(4, 1))) // top-right
            }
        }
    }

    @Nested
    @DisplayName("Fly actions")
    inner class FlyTests {
        @Test
        fun `Pilot can fly to any tile that's not adjacent`() {
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))

            val allNonAdjacentPositions = positionsFromMap("""
                  oo
                 oooo
                ooo.oo
                oo...o
                 oo.o
                  oo
            """)

            assertThat(game.availableActions<Fly>()).containsOnlyElementsOf(
                allNonAdjacentPositions.map { Fly(Pilot, it) }
            )
        }

        @Test
        fun `Pilot cannot fly to any tile a second time in the same turn`() {
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))
                .withPreviousEvents(Fly(Pilot, Position(4, 4)))

            assertThat(game.availableActions<Fly>()).isEmpty()
        }

        @Test
        fun `Pilot can fly to any tile on the turn after having moved to any tile`() {
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))
                .withPreviousEvents(
                    Fly(Pilot, Position(3, 4)), DrawFromTreasureDeck(Pilot), DrawFromFloodDeck(Pilot),
                    Move(Explorer, Position(4, 4)), DrawFromTreasureDeck(Explorer), DrawFromFloodDeck(Explorer),
                    Move(Pilot, Position(4, 4)))

            val allNonAdjacentPositions = positionsFromMap("""
                  oo
                 oooo
                ooo.oo
                oo...o
                 oo.o
                  oo
            """)

            assertThat(game.availableActions<Fly>()).containsOnlyElementsOf(
                allNonAdjacentPositions.map { Fly(Pilot, it) }
            )
        }

        @Test
        fun `pilot can't fly to positions that have sunk`() {
            val sunkenPosition = Position(3, 1)
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))
                .withLocationFloodStates(Sunken, sunkenPosition)

            assertThat(game.availableActions<Fly>()).doesNotContain(Fly(Pilot, sunkenPosition))
        }
    }

    @Nested
    @DisplayName("Shore Up actions")
    inner class ShoreUpTests {
        @Test
        fun `Navigator, Messenger, Diver and Pilot can shore up their own position or flooded positions adjacent to them`() {
            val game = game(Navigator, Messenger, Diver, Pilot)
                            .withLocationFloodStates(Flooded, *Position.allPositions.toTypedArray())

            for (player in game.gameSetup.players) {
                val availableMoves =
                    game.withPlayerPosition(player, Position(4, 4))
                        .withGamePhase(AwaitingPlayerAction(player, 3))
                        .availableActions<ShoreUp>()

                assertThat(availableMoves).containsOnlyElementsOf(positionsFromMap("""
                  ..
                 ....
                ...o..
                ..ooo.
                 ..o.
                  ..
                """).map { ShoreUp(player, it) })
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["Diver", "Engineer", "Explorer", "Messenger", "Navigator", "Pilot"])
        fun `Unflooded or Sunken positions cannot be shored up`(playerName: String) {
            val player1 = Adventurer.valueOf(playerName)
            val player2 = (Adventurer.values().toList() - player1).shuffled().first()
            val game = newRandomGameFor(immListOf(player1, player2))
                            .withPlayerPosition(player1, Position(4, 4))
                            .withLocationFloodStates(Unflooded, *Position.allPositions.toTypedArray())
                            .withLocationFloodStates(Sunken, Position(3, 4), Position(4, 5))

            assertThat(game.availableActions<ShoreUp>()).isEmpty()
        }

        @Test
        fun `Explorer can shore up flooded positions adjacent and diagonal to them`() {
            val game = game(Explorer, Messenger)
                        .withLocationFloodStates(Flooded, *Position.allPositions.toTypedArray())
                        .withPlayerPosition(Explorer, Position(4, 4))

                assertThat(game.availableActions<ShoreUp>()).containsOnlyElementsOf(positionsFromMap("""
                  ..
                 ....
                ..ooo.
                ..ooo.
                 .ooo
                  ..
                """).map { ShoreUp(Explorer, it) })
        }

        @Test
        fun `Engineer can shore up one or two flooded positions`() {
            val game = game(Engineer, Messenger)
                        .withLocationFloodStates(Flooded, *Position.allPositions.toTypedArray())
                        .withPlayerPosition(Engineer, Position(4, 4))

            val validPositions = positionsFromMap("""
              ..
             ....
            ...o..
            ..ooo.
             ..o.
              ..
            """)

            val validCombinations = Pair(Engineer, validPositions).let { (p, vp) -> listOf(
                ShoreUp(p, vp[0], vp[1]), ShoreUp(p, vp[0], vp[2]), ShoreUp(p, vp[0], vp[3]), ShoreUp(p, vp[0], vp[4]),
                ShoreUp(p, vp[1], vp[2]), ShoreUp(p, vp[1], vp[3]), ShoreUp(p, vp[1], vp[4]),
                ShoreUp(p, vp[2], vp[3]), ShoreUp(p, vp[2], vp[4]),
                ShoreUp(p, vp[3], vp[4])
            )}

            assertThat(game.availableActions<ShoreUp>()).containsOnlyElementsOf(
                validPositions.map { ShoreUp(Engineer, it) } + validCombinations
            )
        }

    }
}

private fun Game.availableMoves(player: Adventurer): List<Position> =
    availableMoves().filter { it.player == player }.map(Move::position)

private fun Game.availableMoves(): List<Move> = availableActions<Move>()

private inline fun <reified T: GameEvent> Game.availableActions(): List<T> =
    gameState.availableActions.mapNotNull { it as? T }
