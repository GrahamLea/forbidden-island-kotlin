@file:Suppress("UNCHECKED_CAST")

package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Diver
import com.grahamlea.forbiddenisland.Adventurer.Messenger
import com.grahamlea.forbiddenisland.FloodLevel.*
import com.grahamlea.forbiddenisland.GameSetup.Companion.newRandomGameSetupFor
import com.grahamlea.forbiddenisland.LocationFloodState.Flooded
import com.grahamlea.forbiddenisland.LocationFloodState.Unflooded
import com.grahamlea.forbiddenisland.StartingFloodLevel.*
import com.grahamlea.forbiddenisland.Treasure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Game initialisation")
class GameInitialisationTest {

    @Test
    fun `new game has correct initial flood level`() {
        assertThat(
            listOf(Novice, Normal, Elite, Legendary).map {
                Game.newRandomGameFor(newRandomGameSetupFor(4), startingFloodLevel = it).gameState.floodLevel
            }).isEqualTo(listOf(ONE, TWO, THREE, FOUR))
    }

    @Test
    fun `new game has unique Adventurer for each player`() {
        assertThat(Game.newRandomGameFor(2).gameSetup.players.distinct().size).isEqualTo(2)
        assertThat(Game.newRandomGameFor(3).gameSetup.players.distinct().size).isEqualTo(3)
        assertThat(Game.newRandomGameFor(4).gameSetup.players.distinct().size).isEqualTo(4)
    }

    @Test
    fun `new game has 6 flooded locations`() {
        val floodStateCount = Game.newRandomGameFor(4).gameState.locationFloodStates.values.groupingBy { it }.eachCount()
        assertThat(floodStateCount).isEqualTo(mapOf(Unflooded to 18, Flooded to 6 /*, Sunken to 0*/))
    }

    @Test
    fun `flooded locations in a new game appear in the flood deck discard pile`() {
        val game = Game.newRandomGameFor(4)
        val floodedLocations = game.gameState.locationFloodStates.filter { (_, v) -> v == Flooded }.keys
        assertThat(game.gameState.floodDeckDiscard.toSet()).isEqualTo(floodedLocations.toSet())
    }

    @Test
    fun `flooded locations in a new game are not in the flood deck`() {
        val game = Game.newRandomGameFor(4)
        val floodedLocations = game.gameState.locationFloodStates.filter { (_, v) -> v == Flooded }.keys
        val floodDeck = GameState::class.getPrivateFieldValue("floodDeck", game.gameState) as ImmutableList<Location>
        assertThat(floodDeck intersect floodedLocations).isEmpty()
    }

    @Test
    fun `flood deck in new game contains all unflooded locations`() {
        val game = Game.newRandomGameFor(4)
        val unfloodedLocations = game.gameState.locationFloodStates.filter { (_, v) -> v == Unflooded }.keys
        val floodDeck = GameState::class.getPrivateFieldValue("floodDeck", game.gameState) as ImmutableList<Location>
        assertThat(unfloodedLocations.toSet()).isEqualTo(floodDeck.toSet())
        assertThat(unfloodedLocations.size).isEqualTo(floodDeck.size)
    }

    @Test
    fun `players start on the starting map site for their Adventurer`() {
        val checksPerAdventurer = Adventurer.values().associate { it to 0 }.toMutableMap()
        while (checksPerAdventurer.values.any { it < 2 }) {
            val game = Game.newRandomGameFor(4)
            for (player in game.gameSetup.players) {
                val expectedStartingLocation = Location.values().first { it.startingLocationForAdventurer == player }
                assertThat(game.gameSetup.map.locationAt(game.gameState.playerPositions[player]!!)).isEqualTo(expectedStartingLocation)
                checksPerAdventurer[player] = checksPerAdventurer[player]!! + 1
            }
        }
    }

    @Test
    fun `new game has 4 treasures not collected`() {
        assertThat(Game.newRandomGameFor(4).gameState.treasuresCollected)
            .isEqualTo(mapOf(CrystalOfFire to false, EarthStone to false, OceansChalice to false, StatueOfTheWind to false).imm())
    }

    @Test
    fun `each player starts with 2 treasure deck cards`() {
        assertThat(Game.newRandomGameFor(4).gameState.playerCards.values.map { it.size }.distinct()).isEqualTo(listOf(2))
    }

    @Test
    fun `treasure deck cards dealt to players are removed from the treasure deck`() {
        val game = Game.newRandomGameFor(4)
        val dealtCards = game.gameState.playerCards.flatMap { it.value }
        val remainingCards = GameState::class.getPrivateFieldValue("treasureDeck", game.gameState) as ImmutableList<HoldableCard>
        assertThat((dealtCards + remainingCards).map { it.displayName }.groupingBy { it }.eachCount().toSortedMap())
            .isEqualTo(TreasureDeck.newShuffledDeck().map { it.displayName }.groupingBy { it }.eachCount().toSortedMap())
    }

    @Test
    fun `new game has empty treasure deck discard pile`() {
        assertThat(Game.newRandomGameFor(4).gameState.treasureDeckDiscard).isEmpty()
    }

    @Test
    fun `new game has empty previous event list`() {
        assertThat(Game.newRandomGameFor(4).gameState.previousEvents).isEmpty()
    }

    @Test
    fun `Waters Rise! cards are not dealt to players but shuffled back into the Treasure Deck`() {
        val treasureDeckWithoutWatersRise = TreasureDeck.newShuffledDeck().filterNot { it === WatersRiseCard }
        val initialTreasureDeck = listOf(WatersRiseCard, WatersRiseCard, WatersRiseCard) + treasureDeckWithoutWatersRise
        assertThat(initialTreasureDeck.take(4).count { it == WatersRiseCard }).isEqualTo(3)

        val game = Game.newRandomGameFor(
                newRandomGameSetupFor(2),
                treasureDeck = initialTreasureDeck.imm()
        )
        val allDealtCards = game.gameState.playerCards.flatMap { (_, v) -> v }
        assertThat(allDealtCards.size).isEqualTo(4)
        assertThat(allDealtCards.count { it == WatersRiseCard }).isEqualTo(0)
        assertThat(allDealtCards.toSet()).isEqualTo(treasureDeckWithoutWatersRise.take(4).toSet())

        val treasureDeck = GameState::class.getPrivateFieldValue("treasureDeck", game.gameState) as ImmutableList<HoldableCard>
        assertThat(treasureDeck.count { it === WatersRiseCard }).isEqualTo(3)
        assertThat(treasureDeck.dropLast(3)).isNotEqualTo(initialTreasureDeck.drop(7))
    }

    @Test()
    fun `new game phase is waiting for first player action out of 3`() {
        val game = Game.newRandomGameFor(listOf(Diver, Messenger).imm())
        assertThat(game.gameState.phase).isEqualTo(AwaitingPlayerAction(Diver, 3) as GamePhase)
    }

    @Test()
    fun `new game has no result`() {
        assertThat(Game.newRandomGameFor(4).gameState.result).isNull()
    }

    @Test
    fun `can't create game with less than 2 players`() {
        assertThrows(IllegalArgumentException::class.java) { Game.newRandomGameFor(numberOfPlayers = 1) }
    }

    @Test
    fun `can't create game with more than 4 players`() {
        assertThrows(IllegalArgumentException::class.java) { Game.newRandomGameFor(numberOfPlayers = 5) }
    }
}
