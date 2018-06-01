package com.grahamlea.forbiddenisland

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GameAction.allPossibleActions")
class AllPossibleActionsTest {
    @Test
    fun `contains all possible actions`() {
        assertThat(GameAction.ALL_POSSIBLE_ACTIONS.joinToString("\n"))
            .isEqualTo(this::class.java.getResource("AllPossibleActions.txt").readText())
    }
}