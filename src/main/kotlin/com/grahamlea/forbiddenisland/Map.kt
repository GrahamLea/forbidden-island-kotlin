package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Direction.*
import java.util.*

/**
 * Represents the randomised map generated for a [Game] to be played on.
 *
 * The map does not change during the course of the [Game], however the [flood states][GameState.locationFloodStates]
 * do, and [Location]s may be rendered inaccessible once they are [sunken][LocationFloodState.Sunken]. (In the
 * physical game, tiles are removed from the map once a location sinks, but in this digital version [MapSite]s are not
 * removed from the [GameMap].)
 *
 * The order of the [mapSites] is inconsequential.
 *
 * @throws IllegalArgumentException if [mapSites] does not contain every [Position] and every [Location] exactly once.
 */
data class GameMap(val mapSites: ImmutableList<MapSite>) {

    init {
        require(mapSites.map { it.position }.toSet().containsAll(Position.allPositions)) { "mapSites must include all valid Positions" }
        require(mapSites.map { it.location }.toSet().containsAll(Location.values().toList())) { "mapSites must include all Locations" }
        Position.allPositions.size.let { positionCount ->
            require(mapSites.size == positionCount) { "mapSites must have exactly $positionCount entries" }
        }
    }

    private val sitesByPosition = mapSites.associateBy(MapSite::position)
    private val sitesByLocation = mapSites.associateBy(MapSite::location)

    fun mapSiteAt(position: Position): MapSite = sitesByPosition.getValue(position)

    fun mapSiteOf(location: Location): MapSite = sitesByLocation.getValue(location)

    fun positionOf(location: Location): Position = mapSiteOf(location).position

    fun locationAt(position: Position): Location = mapSiteAt(position).location

    /**
     * Returns the [MapSite]s adjacent to the provided [position].
     *
     * "Adjacent" strictly means up, down, left and right, but you can ask to get the diagonals here as well.
     *
     * @param includeDiagonals whether or not to include diagonally adjacent sites
     */
    fun adjacentSites(position: Position, includeDiagonals: Boolean = false): List<MapSite> =
        position.adjacentPositions(includeDiagonals).map { mapSiteAt(it) }

    companion object {
        /**
         * Returns a new random [GameMap].
         *
         * For determinism, the map can be generated by specifying the [random] to use.
         */
        fun newRandomMap(random: Random = Random()): GameMap =
            GameMap(ImmutableList(Position.allPositions.zip(shuffled<Location>(random)) { p, l -> MapSite(p, l) }))
    }
}

/**
 * Represents a position on the [GameMap].
 *
 * Valid positons are generally in the range (1, 1) to (6, 6), but the 3 positions closest to each corner are not
 * valid, e.g. (1, 1),  (2, 1) and (1, 2).
 *
 * @throws IllegalArgumentException if the given (x, y) co-ordinate is not a valid position.
 */
data class Position(val x: Int, val y: Int): Comparable<Position> {
    init {
        require(isValid(x, y)) { "$this is not a valid position" }
    }

    /**
     * Gives the [Position] one step in the given direction __if that is a valid position__, otherwise null.
     */
    fun neighbour(direction: Direction): Position? =
        Pair(x + direction.xTravel, y + direction.yTravel).let { (newX, newY) ->
            if (isValid(newX, newY)) Position(newX, newY) else null
        }

    /**
     * Returns the [Position]s adjacent to this position.
     *
     * "Adjacent" strictly means up, down, left and right, but you can ask to get the diagonals here as well.
     *
     * @param includeDiagonals whether or not to include diagonally adjacent sites
     */
    fun adjacentPositions(includeDiagonals: Boolean = false): List<Position> = when (includeDiagonals) {
        false -> listOf(North, South, East, West)
        true -> values().toList()
    }.mapNotNull { this.neighbour(it) }

    override fun compareTo(other: Position): Int = when {
        this.y < other.y -> -1
        this.y > other.y -> 1
        this.x < other.x -> -1
        this.x > other.x -> 1
        else -> 0
    }

    override fun toString() = "($x,$y)"

    companion object {
        val allPositions: List<Position> by lazy {
            listOf(      3, 4      ).map { Position(it, 1) } +
            listOf(   2, 3, 4, 5   ).map { Position(it, 2) } +
            listOf(1, 2, 3, 4, 5, 6).map { Position(it, 3) } +
            listOf(1, 2, 3, 4, 5, 6).map { Position(it, 4) } +
            listOf(   2, 3, 4, 5   ).map { Position(it, 5) } +
            listOf(      3, 4      ).map { Position(it, 6) }
        }

        private val unfilledPositions = listOf(
            Pair(1, 1), Pair(2, 1), /* ... */ Pair(5, 1), Pair(6, 1),
            Pair(1, 2),             /* ... */             Pair(6, 2),
                                    /* ... */
            Pair(1, 5),             /* ... */             Pair(6, 5),
            Pair(1, 6), Pair(2, 6), /* ... */ Pair(5, 6), Pair(6, 6)
        )

        /** Returns whether a pair of coordinates constitute a valid Forbidden Island [Position]. */
        fun isValid(x: Int, y: Int): Boolean = x in 1..6 && y in 1..6 && Pair(x, y) !in unfilledPositions

    }
}

/** The placement of a [location] at a [position], instances of which form the [GameMap]. */
data class MapSite(val position: Position, val location: Location): Comparable<MapSite> {

    override fun compareTo(other: MapSite): Int = this.position.compareTo(other.position)

    override fun toString() = "$position~$location"
}

/** A direction in which a player may travel on the [GameMap]. */
enum class Direction(val xTravel: Int = 0, val yTravel: Int = 0) {
    /** Towards the top of the map */
    North(yTravel = -1),
    /** Towards the right of the map */
    East(xTravel = 1),
    /** Towards the bottom of the map */
    South(yTravel = 1),
    /** Towards the left of the map */
    West(xTravel = -1),
    /** Towards the top-right of the map */
    NorthEast(1, -1),
    /** Towards the bottom-right of the map */
    SouthEast(1, 1),
    /** Towards the bottom-left of the map */
    SouthWest(-1, 1),
    /** Towards the top-right of the map */
    NorthWest(-1, -1)
}