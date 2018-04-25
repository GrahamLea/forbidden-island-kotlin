package com.grahamlea.forbiddenisland

import org.hamcrest.CoreMatchers.`is` as is_
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ModelTest {
    @Test
    fun `starting locations are correct for each adventurer`() {
        assertThat(Adventurer.Diver.startingLocation, is_(Location.IronGate))
        assertThat(Adventurer.Engineer.startingLocation, is_(Location.BronzeGate))
        assertThat(Adventurer.Explorer.startingLocation, is_(Location.CopperGate))
        assertThat(Adventurer.Messenger.startingLocation, is_(Location.SilverGate))
        assertThat(Adventurer.Navigator.startingLocation, is_(Location.GoldGate))
        assertThat(Adventurer.Pilot.startingLocation, is_(Location.FoolsLanding))
    }
}