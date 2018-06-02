package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Direction.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("GameMap")
class GameMapTest {
    @Test
    fun `invalid Positions can't be created`() {
        assertThrows<IllegalArgumentException> { Position(1, 1) }
        assertThrows<IllegalArgumentException> { Position(2, 6) }
        assertThrows<IllegalArgumentException> { Position(6, 5) }
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