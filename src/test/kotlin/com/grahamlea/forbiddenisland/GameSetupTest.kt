package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GameSetupTest {

    @Test
    fun `can't setup game with less than 2 players`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            GameSetup(listOf(Diver).imm(), GameMap.newShuffledMap())
        }
    }

    @Test
    fun `can't setup game with more than 4 players`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            GameSetup(listOf(Diver, Engineer, Explorer, Messenger, Navigator).imm(), GameMap.newShuffledMap())
        }
    }

    @Test
    fun `can't setup game with same Adventurer for two players`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            GameSetup(listOf(Diver, Diver).imm(), GameMap.newShuffledMap())
        }
    }
}