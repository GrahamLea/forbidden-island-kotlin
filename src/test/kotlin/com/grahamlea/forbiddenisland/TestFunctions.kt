package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.LocationFloodState.Sunken
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

fun Game.withPlayerCards(playerCards: ImmutableMap<Adventurer, ImmutableList<HoldableCard>>): Game {
    if (!gameState.treasureDeckDiscard.isEmpty()) throw IllegalStateException("If you need to manipulate the treasureDeckDiscard, do it after setting the player cards")
    return copy(
        gameState.copy(
            playerCards = playerCards,
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
