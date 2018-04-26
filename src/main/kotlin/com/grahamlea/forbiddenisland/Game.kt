package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Companion.randomListOfPlayers
import com.grahamlea.forbiddenisland.GamePhase.Companion.maxActionsPerPlayerTurn
import com.grahamlea.forbiddenisland.LocationFloodState.*
import java.util.*

class Game(val gameSetup: GameSetup, gameState: GameState, val random: Random = Random()) {

    var gameState: GameState = gameState
        private set

    fun copy(
            gameState: GameState = this.gameState,
            gameSetup: GameSetup = this.gameSetup,
            random: Random = this.random
    ): Game = Game(gameSetup, gameState, random)

    override fun toString() = "Game($gameSetup, $gameState)"

    companion object {

        private const val numberOfInitialTreasureCardsPerPlayer = 2
        private const val numberOfInitialLocationsToFlood = 6

        fun newRandomGameFor(numberOfPlayers: Int, random: Random = Random()) =
                newRandomGameFor(randomListOfPlayers(numberOfPlayers, random), GameMap.newShuffledMap(random), random)

        fun newRandomGameFor(adventurers: ImmutableList<Adventurer>, map: GameMap, random: Random = Random()) =
                newRandomGameFor(GameSetup(adventurers, map), random)

        fun newRandomGameFor(
                gameSetup: GameSetup,
                random: Random = Random(),
                startingFloodLevel: StartingFloodLevel = StartingFloodLevel.Normal,
                treasureDeck: ImmutableList<HoldableCard> = TreasureDeck.newShuffledDeck(random),
                floodDeck: ImmutableList<Location> = shuffled(random)
        ): Game {

            val initialFloodDeckDiscard = floodDeck.take(numberOfInitialLocationsToFlood)
            val initialFloodDeck = floodDeck.drop(numberOfInitialLocationsToFlood)
            val initialLocationFloodStates =
                    initialFloodDeck.associate { it to Unflooded } + initialFloodDeckDiscard.associate { it to Flooded }

            val mutableTreasureDeck = treasureDeck.toMutableList()

            val initialPlayerCards = mutableMapOf<Adventurer, MutableList<HoldableCard>>()
            for (adventurer in gameSetup.adventurers) {
                initialPlayerCards.put(adventurer, mutableListOf())
            }
            var watersRiseCardsRemoved = 0
            for (n in 1..numberOfInitialTreasureCardsPerPlayer) {
                for (adventurer in gameSetup.adventurers) {
                    var card = mutableTreasureDeck.removeAt(0)
                    while (card is WatersRiseCard) {
                        watersRiseCardsRemoved++
                        card = mutableTreasureDeck.removeAt(0)
                    }
                    initialPlayerCards[adventurer]!!.add(card)
                }
            }
            if (watersRiseCardsRemoved != 0) {
                (1..watersRiseCardsRemoved).forEach { mutableTreasureDeck.add(WatersRiseCard) }
                mutableTreasureDeck.shuffle(random)
            }

            val initialPlayerPositions = gameSetup.adventurers.associate { adventurer ->
                adventurer to
                        (Location.values().first { it.startingLocationForAdventurer == adventurer }
                                .let { location -> gameSetup.map.mapSites.first { it.location == location } })
            }

            val gameState = GameState(
                    gameSetup = gameSetup,
                    floodLevel = startingFloodLevel.floodLevel,
                    treasureDeck = mutableTreasureDeck.immutable(),
                    treasureDeckDiscard = emptyList<HoldableCard>().immutable(),
                    floodDeck = initialFloodDeck.immutable(),
                    floodDeckDiscard = initialFloodDeckDiscard.immutable(),
                    treasuresCollected = Treasure.values().associate { Pair(it, false) }.immutable(),
                    locationFloodStates = initialLocationFloodStates.immutable(),
                    playerCards = initialPlayerCards.mapValues { (_, v) -> v.immutable() }.immutable(),
                    playerPositions = initialPlayerPositions.immutable(),
                    phase = AwaitingPlayerAction(gameSetup.adventurers.first(), actionsRemaining = maxActionsPerPlayerTurn)
            )

            return Game(gameSetup, gameState, random)
        }

    }
}

data class GameSetup(val adventurers: ImmutableList<Adventurer>, val map: GameMap) {
    init {
        require(adventurers.size in 2..4) { "The game must have 2, 3 or 4 adventurers." }
        require(adventurers.distinct().size == adventurers.size) { "Each Adventurer can only be played by one player." }
    }

    companion object {
        fun newRandomGameSetupFor(numberOfPlayers: Int, random: Random = Random()) =
                GameSetup(shuffled<Adventurer>(random, numberOfPlayers), GameMap.newShuffledMap(random))
    }
}