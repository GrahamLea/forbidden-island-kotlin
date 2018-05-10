package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Location.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
}