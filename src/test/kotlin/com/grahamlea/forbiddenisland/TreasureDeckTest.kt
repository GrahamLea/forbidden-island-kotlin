package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Treasure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("treasure deck")
class TreasureDeckTest {
    @Test
    fun `has 3 helicopter lift cards`() {
        assertThat(TreasureDeck.newShuffledDeck().filter { it is HelicopterLiftCard }).hasSize(3)
        assertThat(TreasureDeck.newShuffledDeck().filter { it is HelicopterLiftCard }).hasSize(3)
    }

    @Test
    fun `has 2 sandbags cards`() {
        assertThat(TreasureDeck.newShuffledDeck().filter { it is SandbagsCard }).hasSize(2)
        assertThat(TreasureDeck.newShuffledDeck().filter { it is SandbagsCard }).hasSize(2)
    }

    @Test
    fun `has 3 Waters Rise! cards`() {
        assertThat(TreasureDeck.newShuffledDeck().filter { it is WatersRiseCard }).hasSize(3)
        assertThat(TreasureDeck.newShuffledDeck().filter { it is WatersRiseCard }).hasSize(3)
    }

    @Test
    fun `has 5 of each treasure card`() {
        val treasureCardCounts = TreasureDeck.newShuffledDeck().filter { it is TreasureCard }.groupingBy { (it as TreasureCard).treasure }.eachCount()
        assertThat(treasureCardCounts).isEqualTo(mapOf(CrystalOfFire to 5, EarthStone to 5, OceansChalice to 5, StatueOfTheWind to 5))
    }
}