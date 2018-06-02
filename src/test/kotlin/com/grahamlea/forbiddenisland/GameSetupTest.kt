package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("invalid GameSetup scenarios")
class GameSetupTest {

    @Test
    fun `can't setup game with less than 2 players`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameSetup(listOf(Diver).imm(), GameMap.newRandomMap())
        }
    }

    @Test
    fun `can setup game 2, 3 or 4 players`() {
        for (i in 2..4) {
            GameSetup(shuffled(count = i), GameMap.newRandomMap())
        }
    }

    @Test
    fun `can't setup game with more than 4 players`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameSetup(listOf(Diver, Engineer, Explorer, Messenger, Navigator).imm(), GameMap.newRandomMap())
        }
    }

    @Test
    fun `can't setup game with same Adventurer for two players`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameSetup(listOf(Diver, Diver).imm(), GameMap.newRandomMap())
        }
    }

    @Test
    fun `new random game setup returns different, random results on each call`() {
        val gameSetup1 = GameSetup.newRandomGameSetupFor(4)
        var gameSetup2 = GameSetup.newRandomGameSetupFor(4)
        if (gameSetup1.players == gameSetup2.players) // Statistically occurs once in 720 invocations
            gameSetup2 = GameSetup.newRandomGameSetupFor(4)

        assertThat(gameSetup1.players).isNotEqualTo(gameSetup2.players) // May fail once per 1/2 million runs
        assertThat(gameSetup1.map.mapSites).isNotEqualTo(gameSetup2.map.mapSites) // May fail once per 1/2 million runs
    }

    @Test
    fun `new random game setup returns deterministic results if given the same random seed`() {
        val gameSetup1 = GameSetup.newRandomGameSetupFor(4, Random(1))
        val gameSetup2 = GameSetup.newRandomGameSetupFor(4, Random(1))
        assertThat(gameSetup1.players).isEqualTo(gameSetup2.players)
        assertThat(gameSetup1.map.mapSites).isEqualTo(gameSetup2.map.mapSites)
    }
}