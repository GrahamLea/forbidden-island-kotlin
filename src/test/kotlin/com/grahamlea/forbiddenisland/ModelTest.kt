package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Location.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Model tests")
class ModelTest {
    @Test
    fun `starting locations are correct for each adventurer`() {
        assertThat(Diver.startingLocation).isEqualTo(IronGate)
        assertThat(Engineer.startingLocation).isEqualTo(BronzeGate)
        assertThat(Explorer.startingLocation).isEqualTo(CopperGate)
        assertThat(Messenger.startingLocation).isEqualTo(SilverGate)
        assertThat(Navigator.startingLocation).isEqualTo(GoldGate)
        assertThat(Pilot.startingLocation).isEqualTo(FoolsLanding)
    }

    @Test
    fun `toString of both locations sinking names both locations`() {
        assertThat(BothPickupLocationsSankBeforeCollectingTreasure(Treasure.CrystalOfFire).toString())
            .isEqualTo(
                "BothPickupLocationsSankBeforeCollectingTreasure: CaveOfEmbers^ and CaveOfShadows^ sank before CrystalOfFire was collected"
            )
    }

    @Test
    fun `toString of player drowning names the adventurer`() {
        assertThat(PlayerDrowned(Messenger).toString())
            .isEqualTo("PlayerDrowned: Messenger was on an island that sank and couldn't swim to an adjacent one")
    }

    @Test
    fun `can get the next FloodLevel`() {
        assertThat(FloodLevel.TWO.next()).isEqualTo(FloodLevel.THREE)
        assertThat(FloodLevel.NINE.next()).isEqualTo(FloodLevel.DEAD)
        assertThrows<IllegalStateException> { FloodLevel.DEAD.next() }
    }
}