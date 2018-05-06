package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.LocationFloodState.Unflooded
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*


class GameStateAvailableActionsTest {

    private val random = Random()
    private val earth = TreasureCard(Treasure.EarthStone)
    private val ocean = TreasureCard(Treasure.OceansChalice)
    private val fire = TreasureCard(Treasure.CrystalOfFire)

    @Test
    fun `Engineer, Messenger and Diver can walk to any adjacent positions`() {
        val game = Game.newRandomGameFor(immListOf(Engineer, Messenger, Diver), GameMap.newShuffledMap())
                .withLocationFloodStates(Unflooded, *Location.values())

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

}