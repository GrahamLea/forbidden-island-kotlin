package com.grahamlea.forbiddenisland

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("Collections utils")
class CollectionsTest {

    @Test
    fun `shuffled enum lists produce random results`() {
        // These can fail randomly, but only 1 in 24! times on average
        assertThat(shuffled<Location>()).isNotEqualTo(Location.values().toList())
        assertThat(shuffled<Location>()).isNotEqualTo(shuffled<Location>())
    }

    @Test
    fun `shuffled enum lists can be restricted to a specified size`() {
        assertThat(shuffled<Location>(count = 10)).hasSize(10)
        assertThat(shuffled<Location>(count = 20)).hasSize(20)
        assertThat(shuffled<Location>(count = 10)).isNotEqualTo(Location.values().take(10).toList())
    }

    @Test
    fun `shuffled enum lists are deterministic based off the Random passed in`() {
        assertThat(shuffled<Location>(random = Random(1)))
            .isEqualTo(shuffled<Location>(random = Random(1)))
    }
}