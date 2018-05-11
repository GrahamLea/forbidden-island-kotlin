package com.grahamlea.forbiddenisland

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GameEvent.allPossibleEvents")
class AllPossibleEventsTest {
    @Test
    fun `contains all possible events`() {
        assertThat(GameEvent.allPossibleEvents.joinToString("\n"))
            .isEqualTo(this::class.java.getResource("AllPossibleEvents.txt").readText())
    }
}