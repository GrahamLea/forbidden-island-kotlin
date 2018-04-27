package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Direction.*
import java.util.*

data class GameMap(val mapSites: ImmutableList<MapSite>) {

    init {
        require(mapSites.map { it.position }.toSet().containsAll(positions)) { "mapSites must include all valid Positions" }
        require(mapSites.map { it.location }.toSet().containsAll(Location.values().toList())) { "mapSites must include all Locations" }
    }

    fun mapSiteAt(position: Position): MapSite = mapSites.first { it.position == position }

    fun mapSiteOf(location: Location): MapSite = mapSites.first { it.location == location }

    fun positionOf(location: Location): Position = mapSiteOf(location).position

    fun locationAt(position: Position): Location = mapSiteAt(position).location

    fun adjacentSites(position: Position, includeDiagonals: Boolean = false): List<MapSite> =
        when (includeDiagonals) {
            false -> listOf(North, South, East, West)
            true -> Direction.values().toList()
        }.mapNotNull { position.neighbour(it) }.map { mapSiteAt(it) }

    companion object {
        private val positions =
            listOf(      3, 4      ).map { Position(it, 1) } +
            listOf(   2, 3, 4, 5   ).map { Position(it, 2) } +
            listOf(1, 2, 3, 4, 5, 6).map { Position(it, 3) } +
            listOf(1, 2, 3, 4, 5, 6).map { Position(it, 4) }  +
            listOf(   2, 3, 4, 5   ).map { Position(it, 5) }  +
            listOf(      3, 4      ).map { Position(it, 6) }

        fun newShuffledMap(random: Random = Random()) =
            GameMap(ImmutableList(positions.zip(shuffled<Location>(random)) { p, l -> MapSite(p, l) }))
    }
}

data class Position(val x: Int, val y: Int): Comparable<Position> {
    init {
        require(isValid(x, y)) { "$this is not a valid position" }
    }

    fun neighbour(direction: Direction): Position? {
        val newX = x + direction.xTravel
        val newY = y + direction.yTravel
        return if (isValid(newX, newY)) Position(newX, newY) else null
    }

    override fun compareTo(other: Position): Int = when {
        this.y < other.y -> -1
        this.y > other.y -> 1
        this.x < other.x -> -1
        this.x > other.x -> 1
        else -> 0
    }

    override fun toString() = "($x,$y)"

    companion object {
        private val unfilledPositions = listOf(
            Pair(1, 1), Pair(2, 1), /* ... */ Pair(5, 1), Pair(6, 1),
            Pair(1, 2),             /* ... */             Pair(6, 2),
                                    /* ... */
            Pair(1, 5),             /* ... */             Pair(6, 5),
            Pair(1, 6), Pair(2, 6), /* ... */ Pair(5, 6), Pair(6, 6)
        )

        fun isValid(x: Int, y: Int) = x in 1..6 && y in 1..6 && Pair(x, y) !in unfilledPositions
    }
}

data class MapSite(val position: Position, val location: Location): Comparable<MapSite> {

    override fun compareTo(other: MapSite): Int = this.position.compareTo(other.position)

    override fun toString() = "$position~$location"
}

enum class Direction(val xTravel: Int = 0, val yTravel: Int = 0) {
    North(yTravel = -1), East(xTravel = 1), South(yTravel = 1), West(xTravel = -1),
    NorthEast(1, -1), SouthEast(1, 1), SouthWest(-1, 1), NorthWest(-1, -1);
}