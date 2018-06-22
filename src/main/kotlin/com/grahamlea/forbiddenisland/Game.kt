package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.Companion.randomListOfPlayers
import com.grahamlea.forbiddenisland.Game.Companion.newRandomGameFor
import com.grahamlea.forbiddenisland.GamePhase.Companion.maxActionsPerPlayerTurn
import com.grahamlea.forbiddenisland.LocationFloodState.Flooded
import com.grahamlea.forbiddenisland.LocationFloodState.Unflooded
import java.util.*

/**
 * Represents a game of Forbidden Island that progresses from its beginning to its conclusion.
 *
 * New Games are most easily created using one of the [newRandomGameFor] functions.
 */
class Game(val gameSetup: GameSetup, gameState: GameState, private val random: Random = Random()) {

    var gameState: GameState = gameState
        private set

    fun copy(
            gameState: GameState = this.gameState,
            gameSetup: GameSetup = this.gameSetup,
            random: Random = this.random
    ): Game = Game(gameSetup, gameState, random)

    override fun toString() = "Game($gameSetup, $gameState)"

    /**
     * Process the provided [GameAction] on the current [GameState] and progress to the resulting state.
     *
     * This is the only mutating function in all of the game code.
     */
    fun process(action: GameAction) {
        gameState = gameState.nextStateAfter(action, random)
    }

    companion object {

        const val maxCardsHoldablePerPlayer = 5

        private const val numberOfInitialTreasureCardsPerPlayer = 2
        private const val numberOfInitialLocationsToFlood = 6

        fun newRandomGameFor(numberOfPlayers: Int, random: Random = Random()) =
                newRandomGameFor(randomListOfPlayers(numberOfPlayers, random), random)

        fun newRandomGameFor(players: ImmutableList<Adventurer>, random: Random = Random()) =
                newRandomGameFor(GameSetup(players, GameMap.newRandomMap(random)), random)

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
                initialPlayerCards[player] = mutableListOf()
            }
            var watersRiseCardsRemoved = 0
            for (n in 1..numberOfInitialTreasureCardsPerPlayer) {
                for (player in gameSetup.players) {
                    var card = mutableTreasureDeck.removeAt(0)
                    while (card === WatersRiseCard) {
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
                player to gameSetup.positionOf(player.startingLocation)
            }

            val gameState = GameState(
                    gameSetup = gameSetup,
                    floodLevel = startingFloodLevel.floodLevel,
                    treasureDeck = mutableTreasureDeck.imm(),
                    treasureDeckDiscard = immListOf(),
                    floodDeck = initialFloodDeck.imm(),
                    floodDeckDiscard = initialFloodDeckDiscard.imm(),
                    treasuresCollected = Treasure.values().associate { Pair(it, false) }.imm(),
                    locationFloodStates = initialLocationFloodStates.imm(),
                    playerCards = initialPlayerCards.mapValues { (_, v) -> v.imm() }.imm(),
                    playerPositions = initialPlayerPositions.imm(),
                    phase = AwaitingPlayerAction(gameSetup.players.first(), actionsRemaining = maxActionsPerPlayerTurn)
            )

            return Game(gameSetup, gameState, random)
        }

    }
}

/**
 * Represents the two elements of a [Game] that are set up at the beginning and do not change thereafter:
 * * the [Adventurer]s played by each player and the order in which they take their turns
 * * the position of each location on the [GameMap]
 */
data class GameSetup(val players: ImmutableList<Adventurer>, val map: GameMap) {
    init {
        require(players.size in 2..4) { "The game must have 2, 3 or 4 players." }
        require(players.distinct().size == players.size) { "Each Adventurer can only be played by one player." }
    }

    fun mapSiteAt(position: Position): MapSite = map.mapSiteAt(position)

    fun mapSiteOf(location: Location): MapSite = map.mapSiteOf(location)

    fun positionOf(location: Location): Position = map.positionOf(location)

    fun locationAt(position: Position): Location = map.locationAt(position)

    companion object {
        @Suppress("RemoveExplicitTypeArguments")
        fun newRandomGameSetupFor(numberOfPlayers: Int, random: Random = Random()): GameSetup =
                GameSetup(shuffled<Adventurer>(random, numberOfPlayers), GameMap.newRandomMap(random))
    }
}