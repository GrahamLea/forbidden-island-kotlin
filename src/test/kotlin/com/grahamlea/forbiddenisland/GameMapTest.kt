package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Direction.*
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GameMapTest {
    @Test
    fun `new random map should have 24 distinct locations`() {
        assertThat(GameMap.newShuffledMap().mapSites.map { it.location }.distinct().size, is_(Location.values().size))
    }

    @Test
    fun `Positions can return their adjacent positions`() {
        val p = Position(3, 3)
        assertThat(p.neighbour(North), is_(Position(3, 2)))
        assertThat(p.neighbour(South), is_(Position(3, 4)))
        assertThat(p.neighbour(East), is_(Position(4, 3)))
        assertThat(p.neighbour(West), is_(Position(2, 3)))
        assertThat(p.neighbour(NorthEast), is_(Position(4, 2)))
        assertThat(p.neighbour(SouthEast), is_(Position(4, 4)))
        assertThat(p.neighbour(NorthWest), is_(Position(2, 2)))
        assertThat(p.neighbour(SouthWest), is_(Position(2, 4)))
    }

    @Test
    fun `Invalid positions are not returned as adjacent positions`() {
        assertThat(Position(3, 1).neighbour(North), is_(nullValue()))
        assertThat(Position(6, 4).neighbour(East), is_(nullValue()))
        assertThat(Position(1, 3).neighbour(West), is_(nullValue()))
        assertThat(Position(4, 6).neighbour(South), is_(nullValue()))
        assertThat(Position(2, 2).neighbour(NorthWest), is_(nullValue()))
    }

    @Test
    fun `GameMap can return adjacent sites`() {
        val map = GameMap.newShuffledMap()
        val position = Position(3, 4)
        val neighbours = listOf(
                            Position(2, 4),
            Position(3, 3),                 Position(3, 5),
                            Position(4, 4)
        )
        val expectedSites = map.mapSites.filter { it.position in neighbours }
        assertThat(map.adjacentSites(position).toSet(), is_(expectedSites.toSet()))
    }

    @Test
    fun `GameMap can return adjacent sites including diagonals`() {
        val map = GameMap.newShuffledMap()
        val position = Position(4, 3)
        val neighbours = listOf(
                Position(3, 2), Position(4, 2), Position(5, 2),
                Position(3, 3),                 Position(5, 3),
                Position(3, 4), Position(4, 4), Position(5, 4)
        )
        val expectedSites = map.mapSites.filter { it.position in neighbours }
        assertThat(map.adjacentSites(position, includeDiagonals = true).toSet(), is_(expectedSites.toSet()))
    }

    @Test
    fun `GameMap doesn't return invalid sites adjacent to edge sites`() {
        val map = GameMap.newShuffledMap()
        val position = Position(5, 2)
        val neighbours = listOf(
                Position(4, 1),
                Position(4, 2),
                Position(4, 3), Position(5, 3), Position(6, 3)
        )
        val expectedSites = map.mapSites.filter { it.position in neighbours }
        assertThat(map.adjacentSites(position, includeDiagonals = true).toSet(), is_(expectedSites.toSet()))
    }
}