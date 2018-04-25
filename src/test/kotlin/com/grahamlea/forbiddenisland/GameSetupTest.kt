package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import org.junit.Test

class GameSetupTest {

    @Test(expected = IllegalArgumentException::class)
    fun `can't setup game with less than 2 players`() {
        GameSetup(listOf(Diver).immutable(), GameMap.newShuffledMap())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `can't setup game with more than 4 players`() {
        GameSetup(listOf(Diver, Engineer, Explorer, Messenger, Navigator).immutable(), GameMap.newShuffledMap())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `can't setup game with same Adventurer for two players`() {
        GameSetup(listOf(Diver, Diver).immutable(), GameMap.newShuffledMap())
    }


}