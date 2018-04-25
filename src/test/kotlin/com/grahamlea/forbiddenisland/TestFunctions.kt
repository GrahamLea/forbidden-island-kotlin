package com.grahamlea.forbiddenisland

import kotlin.reflect.KClass

fun Game.withLocationFloodStates(floodState: LocationFloodState, positions: List<Position>): Game {
    val locations = this.gameSetup.map.mapSites.filter { it.position in positions }.map { it.location }
    return copy(
            gameState.copy(locationFloodStates = (gameState.locationFloodStates + locations.associate { it to floodState }).immutable())
    )
}

fun GameMap.withLocationNotAtAnyOf(locationToEvict: Location, positions: List<Position>): GameMap {
    val locationsAtPositions = mapSites.filter { it.position in positions }.map { it.location }
    return if (locationToEvict !in locationsAtPositions) {
        this
    } else {
        val locationToSwapIn = Location.values().first { it !in locationsAtPositions }
        val currentPositionOfLocationToBeEvicted = positionOf(locationToEvict)
        val currentPositionOfLocationToSwapIn = positionOf(locationToSwapIn)
        val locationsToSwap = listOf(locationToEvict, locationToSwapIn)
        GameMap(
                (mapSites.filterNot { it.location in locationsToSwap } +
                        MapSite(currentPositionOfLocationToBeEvicted, locationToSwapIn) +
                        MapSite(currentPositionOfLocationToSwapIn, locationToEvict)).immutable())
    }
}

fun Game.withPlayerPosition(player: Adventurer, newPosition: Position): Game {
    return copy(
            gameState.copy(
                    playerPositions = gameState.playerPositions + (player to gameSetup.map.mapSites.first { it.position == newPosition })
            )
    )
}

@Suppress("UNCHECKED_CAST")
val GameState.treasureDeck: ImmutableList<HoldableCard>
    get() = GameState::class.getPrivateFieldValue("treasureDeck", this) as ImmutableList<HoldableCard>

fun <C: Any> KClass<C>.getPrivateFieldValue(fieldName: String, target: C): Any? {
    val field = this.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(target)
}
