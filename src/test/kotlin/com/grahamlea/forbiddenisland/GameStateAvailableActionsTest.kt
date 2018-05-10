package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*


class GameStateAvailableActionsTest {

    private val random = Random()
    private val earth = TreasureCard(Treasure.EarthStone)
    private val ocean = TreasureCard(Treasure.OceansChalice)
    private val fire = TreasureCard(Treasure.CrystalOfFire)

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

    private fun Game.availableMoves(player: Adventurer): List<Position> =
        availableMoves().filter { it.player == player }.map(Move::position)

    private fun Game.availableMoves(): List<Move> = availableActions<Move>()

    private inline fun <reified T: GameEvent> Game.availableActions(): List<T> =
        gameState.availableActions.mapNotNull { it as? T }
}