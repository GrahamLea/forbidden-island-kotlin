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

        fun newRandomGameFor(players: ImmutableList<Adventurer>, map: GameMap, random: Random = Random()) =
                newRandomGameFor(GameSetup(players, map), random)

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
            for (player in gameSetup.players) {
                initialPlayerCards.put(player, mutableListOf())
            }
            var watersRiseCardsRemoved = 0
            for (n in 1..numberOfInitialTreasureCardsPerPlayer) {
                for (player in gameSetup.players) {
                    var card = mutableTreasureDeck.removeAt(0)
                    while (card is WatersRiseCard) {
                        watersRiseCardsRemoved++
                        card = mutableTreasureDeck.removeAt(0)
                    }
                    initialPlayerCards[player]!!.add(card)
                }
            }
            if (watersRiseCardsRemoved != 0) {
                (1..watersRiseCardsRemoved).forEach { mutableTreasureDeck.add(WatersRiseCard) }
                mutableTreasureDeck.shuffle(random)
            }

            val initialPlayerPositions = gameSetup.players.associate { player ->
                player to
                        (Location.values().first { it.startingLocationForAdventurer == player }
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
                    phase = AwaitingPlayerAction(gameSetup.players.first(), actionsRemaining = maxActionsPerPlayerTurn)
            )

            return Game(gameSetup, gameState, random)
        }

    }
}

data class GameSetup(val players: ImmutableList<Adventurer>, val map: GameMap) {
    init {
        require(players.size in 2..4) { "The game must have 2, 3 or 4 players." }
        require(players.distinct().size == players.size) { "Each Adventurer can only be played by one player." }
    }

    companion object {
        fun newRandomGameSetupFor(numberOfPlayers: Int, random: Random = Random()) =
                GameSetup(shuffled<Adventurer>(random, numberOfPlayers), GameMap.newShuffledMap(random))
    }
}