package com.grahamlea.forbiddenisland

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AllPossibleEventsTest {
    @Test
    fun `all possible events`() {
        assertThat(GameEvent.allPossibleEvents.joinToString("\n"))
            .isEqualTo(this::class.java.getResource("AllPossibleEvents.txt").readText())
    }
}