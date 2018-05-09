package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
            val availableActions =
                game.withPlayerPosition(player, Position(4, 4))
                    .withGamePhase(AwaitingPlayerAction(player, 3))
                    .gameState.availableActions

            assertThat(availableActions.filter { it is Move }).containsOnlyElementsOf(
                listOf(
                                    Position(4, 3),
                    Position(3, 4),                 Position(5, 4),
                                    Position(4, 5)
                ).map { Move(player, it) as GameEvent }
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
            val availableActions =
                game.withPlayerPosition(player, playerPosition)
                    .withGamePhase(AwaitingPlayerAction(player, 3))
                    .gameState.availableActions

            assertThat(availableActions)
                .contains(Move(player, floodedPosition))
                .doesNotContain(Move(player, sunkenPosition))
        }
    }

    @Test
    fun `Explorer can walk to diagonal positions as well as adjacent`() {
        val game = game(Explorer, Pilot)
            .withPlayerPosition(Explorer, Position(4, 4))

        assertThat(game.gameState.availableActions.filter { it is Move }).containsOnlyElementsOf(
            listOf(
                Position(3, 3), Position(4, 3), Position(5, 3),
                Position(3, 4),                 Position(5, 4),
                Position(3, 5), Position(4, 5), Position(5, 5)
            ).map { Move(Explorer, it) as GameEvent }
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

        val allNonAdjacentPositions = listOf(
            listOf(      3, 4      ).map { Position(it, 1) },
            listOf(   2, 3, 4, 5   ).map { Position(it, 2) },
            listOf(1, 2, 3,    5, 6).map { Position(it, 3) },
            listOf(1, 2,  /*@*/   6).map { Position(it, 4) },
            listOf(   2, 3,    5   ).map { Position(it, 5) },
            listOf(      3, 4      ).map { Position(it, 6) }
        ).flatten()

        assertThat(game.gameState.availableActions.filter { it is Fly }).containsOnlyElementsOf(
            allNonAdjacentPositions.map { Fly(Pilot, it) as GameEvent }
        )
    }

    @Test
    fun `Pilot cannot fly to any tile a second time in the same turn`() {
        val game = game(Pilot, Explorer)
            .withPlayerPosition(Pilot, Position(4, 4))
            .withPreviousEvents(Fly(Pilot, Position(4, 4)))

        assertThat(game.gameState.availableActions.filter { it is Fly }).isEmpty()
    }

    @Test
    fun `Pilot can fly to any tile one the turn after having moved to any tile`() {
        val game = game(Pilot, Explorer)
            .withPlayerPosition(Pilot, Position(4, 4))
            .withPreviousEvents(
                Fly(Pilot, Position(3, 4)), DrawFromTreasureDeck(Pilot), DrawFromFloodDeck(Pilot),
                Move(Explorer, Position(4, 4)), DrawFromTreasureDeck(Explorer), DrawFromFloodDeck(Explorer),
                Move(Pilot, Position(4, 4)))

        val allNonAdjacentPositions = listOf(
            listOf(      3, 4      ).map { Position(it, 1) },
            listOf(   2, 3, 4, 5   ).map { Position(it, 2) },
            listOf(1, 2, 3,    5, 6).map { Position(it, 3) },
            listOf(1, 2,  /*@*/   6).map { Position(it, 4) },
            listOf(   2, 3,    5   ).map { Position(it, 5) },
            listOf(      3, 4      ).map { Position(it, 6) }
        ).flatten()

        assertThat(game.gameState.availableActions.filter { it is Fly }).containsOnlyElementsOf(
            allNonAdjacentPositions.map { Fly(Pilot, it) as GameEvent }
        )
    }

    @Test
    fun `Navigator can move other players up to 2 tiles, including changing directions`() {
        val game = game(Navigator, Engineer)
            .withPlayerPosition(Navigator, Position(4, 4))
            .withPlayerPosition(Engineer, Position(3, 3))


        assertThat(game.gameState.availableActions.mapNotNull {
            if (it is Move && it.player == Engineer) it.position else null
        }).containsOnlyElementsOf(
            listOf(
                                                Position(3, 1),
                                Position(2, 2), Position(3, 2), Position(4, 2),
                Position(1, 3), Position(2, 3), /*     @     */ Position(4, 3), Position(5, 3),
                                Position(2, 4), Position(3, 4), Position(4, 4),
                                                Position(3, 5)
            )
        )
    }

    @Test
    fun `Navigator themselves can only move to adjacent tiles`() {
        val game = game(Navigator, Messenger)
                    .withPlayerPosition(Navigator, Position(4, 4))

        assertThat(game.gameState.availableActions.filter { it is Move && it.player == Navigator }).containsOnlyElementsOf(
            listOf(
                                Position(4, 3),
                Position(3, 4),                 Position(5, 4),
                                Position(4, 5)
            ).map { Move(Navigator, it) as GameEvent }
        )
    }

    @Test
    fun `Navigator can move the Explorer diagonally`() {
        val game = game(Navigator, Explorer)
            .withPlayerPosition(Explorer, Position(3, 3))

        val expectedExplorerOptions = listOf(
            listOf(      3, 4   ).map { Position(it, 1) },
            listOf(   2, 3, 4, 5).map { Position(it, 2) },
            listOf(1, 2,    4, 5).map { Position(it, 3) },
            listOf(1, 2, 3, 4, 5).map { Position(it, 4) },
            listOf(   2, 3, 4, 5).map { Position(it, 5) }
        ).flatten()

        assertThat(game.gameState.availableActions.mapNotNull { if (it is Move && it.player == Explorer) it.position else null })
            .containsOnlyElementsOf(expectedExplorerOptions)
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
                    .withLocationFloodStates(Sunken, Position(2, 4), Position(4, 4))
                    .withLocationFloodStates(Flooded, Position(3, 4), Position(5, 4))

        assertThat(game.gameState.availableActions.filter { it is Move })
            .contains(Move(Diver, Position(3, 4)))
            .contains(Move(Diver, Position(5, 4)))
            .contains(Move(Diver, Position(6, 4)))
    }

    @Test
    fun `Diver can change direction while moving through adjacent flooded and sunken tiles`() {

        val diverPosition = Position(1, 4)

        val floodedPositions = listOf(
            listOf(      3).map { Position(it, 1) },
            listOf(   2, 3).map { Position(it, 2) },
            listOf(1, 2   ).map { Position(it, 3) }
        ).flatten()

        val game = game(Diver, Explorer)
                    .withPlayerPosition(Diver, diverPosition)
                    .withLocationFloodStates(Flooded, *floodedPositions.toTypedArray())

        assertThat(game.gameState.availableActions.filter { it is Move })
            .contains(Move(Diver, Position(4, 1)))
    }

}