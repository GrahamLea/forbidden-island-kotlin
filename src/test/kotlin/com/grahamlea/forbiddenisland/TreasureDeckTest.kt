package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Treasure.*
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class TreasureDeckTest {
    @Test
    fun `has 3 helicopter lift cards`() {
        assertThat(TreasureDeck.newShuffledDeck().filter { it is HelicopterLiftCard }.size, is_(3))
        assertThat(TreasureDeck.newShuffledDeck().filter { it is HelicopterLiftCard }.size, is_(3))
    }

    @Test
    fun `has 2 sandbags cards`() {
        assertThat(TreasureDeck.newShuffledDeck().filter { it is SandbagsCard }.size, is_(2))
        assertThat(TreasureDeck.newShuffledDeck().filter { it is SandbagsCard }.size, is_(2))
    }

    @Test
    fun `has 3 Waters Rise! cards`() {
        assertThat(TreasureDeck.newShuffledDeck().filter { it is WatersRiseCard }.size, is_(3))
        assertThat(TreasureDeck.newShuffledDeck().filter { it is WatersRiseCard }.size, is_(3))
    }

    @Test
    fun `has 5 of each treasure card`() {
        val treasureCardCounts = TreasureDeck.newShuffledDeck().filter { it is TreasureCard }.groupingBy { (it as TreasureCard).treasure }.eachCount()
        assertThat(treasureCardCounts, is_(mapOf(CrystalOfFire to 5, EarthStone to 5, OceansChalice to 5, StatueOfTheWind to 5)))
    }
}