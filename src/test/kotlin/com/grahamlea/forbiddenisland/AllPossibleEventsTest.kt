package com.grahamlea.forbiddenisland

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class AllPossibleEventsTest {
    @Test
    fun `all possible events`() {
        assertThat(GameEvent.allPossibleEvents.joinToString("\n"),
                `is`(this::class.java.getResource("AllPossibleEvents.txt").readText()))
    }
}