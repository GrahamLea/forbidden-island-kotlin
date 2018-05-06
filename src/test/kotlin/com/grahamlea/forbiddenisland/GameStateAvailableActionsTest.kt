package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import java.util.*


class GameStateAvailableActionsTest {

    private val random = Random()
    private val earth = TreasureCard(Treasure.EarthStone)
    private val ocean = TreasureCard(Treasure.OceansChalice)
    private val fire = TreasureCard(Treasure.CrystalOfFire)

    @Test
    fun `Engineer, Messenger and Diver can walk to any adjacent positions`() {
        val game = game(Engineer, Messenger, Diver).withLocationFloodStates(Unflooded, *Location.values())

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
        val floodedPosition = Position(3, 4)
        val sunkenPosition = Position(5, 4)
        testFloodRelatedMoves(floodedPosition, sunkenPosition, Diver, Engineer, Explorer)
        testFloodRelatedMoves(floodedPosition, sunkenPosition, Messenger, Navigator, Pilot)
    }

    private fun testFloodRelatedMoves(floodedPosition: Position, sunkenPosition: Position, vararg players: Adventurer) {
        val game = game(*players)
            .withLocationFloodStates(Flooded, floodedPosition)
            .withLocationFloodStates(Sunken, sunkenPosition)

        for (player in game.gameSetup.players) {
            val availableActions =
                game.withPlayerPosition(player, Position(4, 4))
                    .withGamePhase(AwaitingPlayerAction(player, 3))
                    .gameState.availableActions

            assertThat(availableActions)
                .contains(Move(player, floodedPosition))
                .doesNotContain(Move(player, sunkenPosition))
        }
    }

    @Ignore
    @Test
    fun `Explorer can walk to diagonal positions as well as adjacent`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Pilot can move to any tile`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Pilot cannot move to any tile more than once per turn`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Navigator can move other players up to 2 tiles, including changing directions`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Navigator themself can only move to adjacent tiles`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Navigator can move the Explorer diagonally`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Navigator can move the Diver through 1 flooded tile`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Navigator cannot land the Diver on flooded tiles`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Navigator cannot move non-Diver players through flooded tiles`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Diver can move through adjacent flooded and sunken tiles`() {
        TODO()
    }
}