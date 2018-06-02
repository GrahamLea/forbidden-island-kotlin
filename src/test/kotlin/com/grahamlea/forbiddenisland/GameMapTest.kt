package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Direction.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("GameMap")
class GameMapTest {
    @Test
    fun `the validity of Positions can be tested`() {
        assertThat(Position.isValid(1, 1)).isFalse()
        assertThat(Position.isValid(2, 1)).isFalse()
        assertThat(Position.isValid(1, 2)).isFalse()
        assertThat(Position.isValid(2, 2)).isTrue()
        assertThat(Position.isValid(3, 1)).isTrue()
        assertThat(Position.isValid(1, 3)).isTrue()
        assertThat(Position.isValid(3, 3)).isTrue()
        assertThat(Position.isValid(4, 4)).isTrue()
        assertThat(Position.isValid(5, 5)).isTrue()
        assertThat(Position.isValid(6, 6)).isFalse()
    }

    @Test
    fun `invalid Positions can't be created`() {
        assertThrows<IllegalArgumentException> { Position(1, 1) }
        assertThrows<IllegalArgumentException> { Position(2, 6) }
        assertThrows<IllegalArgumentException> { Position(6, 5) }
    }

    @Test
    fun `allPositions contains them all (and no more)`() {
        assertThat(Position.allPositions).containsExactlyInAnyOrder(
            Position(3, 1), Position(4, 1),
            Position(2, 2), Position(3, 2), Position(4, 2), Position(5, 2),
            Position(1, 3), Position(2, 3), Position(3, 3), Position(4, 3), Position(5, 3), Position(6, 3),
            Position(1, 4), Position(2, 4), Position(3, 4), Position(4, 4), Position(5, 4), Position(6, 4),
            Position(2, 5), Position(3, 5), Position(4, 5), Position(5, 5),
            Position(3, 6), Position(4, 6)
        )
    }

    @Test
    fun `new random map should have 24 distinct locations`() {
        assertThat(GameMap.newRandomMap().mapSites.map { it.location }.distinct()).hasSize(Location.values().size)
    }

    @Test
    fun `Positions can return their adjacent positions`() {
        val p = Position(3, 3)
        assertThat(p.neighbour(North)).isEqualTo(Position(3, 2))
        assertThat(p.neighbour(South)).isEqualTo(Position(3, 4))
        assertThat(p.neighbour(East)).isEqualTo(Position(4, 3))
        assertThat(p.neighbour(West)).isEqualTo(Position(2, 3))
        assertThat(p.neighbour(NorthEast)).isEqualTo(Position(4, 2))
        assertThat(p.neighbour(SouthEast)).isEqualTo(Position(4, 4))
        assertThat(p.neighbour(NorthWest)).isEqualTo(Position(2, 2))
        assertThat(p.neighbour(SouthWest)).isEqualTo(Position(2, 4))
    }

    @Test
    fun `Invalid positions are not returned as adjacent positions`() {
        assertThat(Position(3, 1).neighbour(North)).isNull()
        assertThat(Position(6, 4).neighbour(East)).isNull()
        assertThat(Position(1, 3).neighbour(West)).isNull()
        assertThat(Position(4, 6).neighbour(South)).isNull()
        assertThat(Position(2, 2).neighbour(NorthWest)).isNull()
    }

    @Test
    fun `GameMap can return adjacent positions and sites`() {
        val map = GameMap.newRandomMap()
        val position = Position(3, 4)
        val neighbours = positionsFromMap("""
              ..
             ....
            ..o...
            .o.o..
             .o..
              ..
        """)
        val expectedSites = map.mapSites.filter { it.position in neighbours }
        assertThat(position.adjacentPositions()).containsOnlyElementsOf(expectedSites.map(MapSite::position))
        assertThat(map.adjacentSites(position)).containsOnlyElementsOf(expectedSites)
    }

    @Test
    fun `GameMap can return adjacent positions and sites including diagonals`() {
        val map = GameMap.newRandomMap()
        val position = Position(4, 3)
        val neighbours = positionsFromMap("""
              ..
             .ooo
            ..o.o.
            ..ooo.
             ....
              ..
        """)
        val expectedSites = map.mapSites.filter { it.position in neighbours }
        assertThat(position.adjacentPositions(includeDiagonals = true)).containsOnlyElementsOf(expectedSites.map(MapSite::position))
        assertThat(map.adjacentSites(position, includeDiagonals = true)).containsOnlyElementsOf(expectedSites)
    }

    @Test
    fun `GameMap doesn't return invalid positions or sites adjacent to edge positions`() {
        val map = GameMap.newRandomMap()
        val position = Position(5, 2)
        val neighbours = positionsFromMap("""
              .o
             ..o.
            ...ooo
            ......
             ....
              ..
        """)
        val expectedSites = map.mapSites.filter { it.position in neighbours }
        assertThat(position.adjacentPositions(includeDiagonals = true)).containsOnlyElementsOf(expectedSites.map(MapSite::position))
        assertThat(map.adjacentSites(position, includeDiagonals = true).toSet()).isEqualTo(expectedSites.toSet())
    }
}