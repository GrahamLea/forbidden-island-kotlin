package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.LocationFloodState.Sunken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

fun game(vararg players: Adventurer) = Game.newRandomGameFor(immListOf(*players), GameMap.newShuffledMap())

fun Game.withLocationFloodStates(floodState: LocationFloodState, vararg positions: Position): Game {
    val locations = this.gameSetup.map.mapSites.filter { it.position in positions }.map { it.location }
    return withLocationFloodStates(floodState,  *locations.toTypedArray())
}

fun Game.withLocationFloodStates(floodState: LocationFloodState, vararg locations: Location): Game {
    return copy(
        gameState.copy(locationFloodStates = (gameState.locationFloodStates + locations.associate { it to floodState }).imm())
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
                        MapSite(currentPositionOfLocationToSwapIn, locationToEvict)).imm())
    }
}

fun Game.withPlayerPosition(player: Adventurer, newPosition: Position): Game {
    return copy(
        gameState.copy(
            playerPositions = gameState.playerPositions + (player to newPosition)
        )
    )
}

fun Game.withPlayerLocation(player: Adventurer, newLocation: Location): Game {
    return copy(
        gameState.copy(
            playerPositions = gameState.playerPositions + (player to gameSetup.map.positionOf(newLocation))
        )
    )
}

fun Game.withPlayerCards(playerCards: Map<Adventurer, ImmutableList<HoldableCard>>): Game {
    if (!gameState.treasureDeckDiscard.isEmpty()) throw IllegalStateException("If you need to manipulate the treasureDeckDiscard, do it after setting the player cards")
    return copy(
        gameState.copy(
            playerCards = playerCards.imm(),
            treasureDeck = TreasureDeck.newShuffledDeck().subtract(playerCards.values.flatten())
        )
    )
}

fun Game.withTopOfTreasureDeck(vararg cards: HoldableCard): Game {
    return copy(
        gameState.copy(
            treasureDeck = immListOf(*cards) + gameState.treasureDeck.subtract(cards.toList())
        )
    )
}

fun Game.withTreasureDeckDiscard(discardedCards: ImmutableList<HoldableCard>): Game {
    return copy(
        gameState.copy(
            treasureDeckDiscard = discardedCards,
            treasureDeck = gameState.treasureDeck.subtract(discardedCards)
        )
    )
}

fun Game.withTopOfFloodDeck(vararg locations: Location): Game {
    return copy(
            gameState.copy(
                    floodDeck = immListOf(*locations) + gameState.floodDeck.subtract(locations.toList())
            )
    )
}

fun Game.withFloodDeckDiscard(discardedCards: ImmutableList<Location>): Game {
    return copy(
        gameState.copy(
            floodDeckDiscard = discardedCards,
            floodDeck = (shuffled<Location>() - discardedCards - gameState.locationsWithState(Sunken)).imm()
        )
    )
}

fun Game.withPreviousEvents(vararg events: GameEvent): Game {
    return copy(
        gameState.copy(
            previousEvents = immListOf(*events)
        )
    )
}

fun Game.withTreasuresCollected(vararg treasures: Treasure): Game {
    return copy(
        gameState.copy(
             treasuresCollected = (gameState.treasuresCollected + treasures.map { it to true }).imm()
        )
    )
}

fun Game.withGamePhase(phase: GamePhase): Game = copy(gameState.copy(phase = phase))

@Suppress("UNCHECKED_CAST")
val GameState.treasureDeck: ImmutableList<HoldableCard>
    get() = GameState::class.getPrivateFieldValue("treasureDeck", this) as ImmutableList<HoldableCard>

@Suppress("UNCHECKED_CAST")
val GameState.floodDeck: ImmutableList<Location>
    get() = GameState::class.getPrivateFieldValue("floodDeck", this) as ImmutableList<Location>

fun <C: Any> KClass<C>.getPrivateFieldValue(fieldName: String, target: C): Any? {
    val field = this.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(target)
}

fun <T> time(block: () -> T): T {
    val start = System.currentTimeMillis()
    try {
        return block()
    } finally {
        val end = System.currentTimeMillis()
        val time = end - start
        val caller = Thread.currentThread().stackTrace[2]
        System.err.println("(${time}ms) ${caller.lineNumber}: ${caller.methodName}")
    }
}

/**
 * Translates a visual map in a string to a List<Position>.
 *
 * For example, the following string represents positions (2,2), (2,3), (2,4) and (3,4):
 * ```
 *   ..
 *  o...
 * .o....
 * .oo...
 *  ....
 *   ..
 * ```
 */
fun positionsFromMap(map: String): List<Position> {
    map.split("\n").map { it.trim() }.filterNot { it.isEmpty() }.let { lines ->
        require(lines.map { it.length } == listOf(2, 4, 6, 6, 4, 2)) { "Map string must have 6 non-empty lines with lengths of 2, 4, 6, 6, 4, and 2" }
        require(lines.all { it.all { it == '.' || it == 'o' } }) { "Map string must only contain '.' and 'o' characters" }
        return lines.mapIndexed { y, line ->
            when (y) {
                0, 5 -> "  $line"
                1, 4 -> " $line"
                else -> line
            }.mapIndexedNotNull { x, c -> if (c == 'o') Position(x + 1, y + 1) else null }
        }.flatten()
    }
}

class TestFunctionsTest {
    @Test
    fun `positionsFromMap translates a map string into a map`() {
        val points = listOf(
            listOf(      3         ).map { Position(it, 1) },
            listOf(   2, 3, 4, 5   ).map { Position(it, 2) },
            listOf(1, 2,    4, 5, 6).map { Position(it, 3) },
            listOf(1,          5, 6).map { Position(it, 4) },
            listOf(   2,    4, 5   ).map { Position(it, 5) },
            listOf(      3, 4      ).map { Position(it, 6) }
        ).flatten()

        assertThat(positionsFromMap("""
  o.
 oooo
oo.ooo
o...oo
 o.oo
  oo
""")).isEqualTo(points)
    }
}
