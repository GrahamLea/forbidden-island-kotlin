package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.LocationFloodState.*
import java.util.*

/**
 * Represents the result of a [Game] once it has finished.
 *
 * @see GameState.result
 * @see AdventurersWon
 * @see FoolsLandingSank
 * @see MaximumWaterLevelReached
 * @see BothPickupLocationsSankBeforeCollectingTreasure
 * @see PlayerDrowned
 */
sealed class GameResult() {
    protected open fun detail(): String? = null
    override fun toString() = this::class.simpleName!! + (detail()?.let { ": $it" } ?: "")
}

object AdventurersWon: GameResult()

object FoolsLandingSank: GameResult()

object MaximumWaterLevelReached: GameResult()

data class BothPickupLocationsSankBeforeCollectingTreasure(val treasure: Treasure): GameResult() {
    private val locations: Pair<Location, Location> =
        Location.values().filter { it.pickupLocationForTreasure == treasure }.let { Pair(it[0], it[1]) }
    override fun detail() = "${locations.first} and ${locations.second} sank before $treasure was collected"
    override fun toString() = super.toString() // because data class
}

data class PlayerDrowned(val player: Adventurer): GameResult() {
    override fun detail(): String? = "$player was on an island that sank and couldn't swim to an adjacent one"
    override fun toString() = super.toString() // because data class
}

/** The treasures which must be collected in order to win the game of Forbidden Island. */
enum class Treasure(val displayName: String) {
    CrystalOfFire("The Crystal of Fire"),
    EarthStone("The Earth Stone"),
    OceansChalice("The Ocean's Chalice"),
    StatueOfTheWind("The Statue of the Wind")
}

/**
 * The different roles played by the "Adventurers" (players) in the game.
 *
 * Each player is randomly assigned one Adventurer role, and only one player can have each role per game.
 */
enum class Adventurer {
    Diver, Engineer, Explorer, Messenger, Navigator, Pilot;

    val startingLocation get() = Location.values().first { it.startingLocationForAdventurer == this }

    companion object {
        fun randomListOfPlayers(numberOfPlayers: Int, random: Random = Random()): ImmutableList<Adventurer> = shuffled(random, numberOfPlayers)
    }
}

/**
 * The current "Flood Level" of the game. This determines how many cards are dealt from the
 * [flood deck][GameState.floodDeck] during each turn, and rises over the course of the game whenever a
 * [Waters Rise!][WatersRiseCard] card is dealt, causing the [Location]s on the [GameMap] to flood quicker as the game
 * progresses. Reaching the [top flood level][FloodLevel.DEAD] is one way that the game ends.
 */
enum class FloodLevel(val tilesFloodingPerTurn: Int) {
    ONE(2), TWO(2), THREE(3), FOUR(3), FIVE(3), SIX(4), SEVEN(4), EIGHT(5), NINE(5), DEAD(0);

    /**
     * Returns the next highest flood level to this one
     *
     * @throws IllegalStateException if called on the [DEAD] level
     */
    fun next(): FloodLevel {
        if (this == DEAD) throw IllegalStateException("There is no next flood level after dead")
        return FloodLevel.values()[this.ordinal + 1]
    }
}

/**
 * A list of named starting points for the [FloodLevel].
 */
enum class StartingFloodLevel(val floodLevel: FloodLevel) {
    Novice(FloodLevel.ONE),
    Normal(FloodLevel.TWO),
    Elite(FloodLevel.THREE),
    Legendary(FloodLevel.FOUR)
}

/**
 * Represents whether a [Location] is [Unflooded], [Flooded] or [Sunken]. Locations become flooded when they are dealt
 * from the [flood deck][GameState.floodDeck]. Flooded locations can be unflooded by being [shored up][ShoreUp] or
 * [sandbagged][Sandbag]. If an already-flooded location is dealt from the flood deck again, it is sunken and becomes
 * inaccessible for the remained of the game.
 */
enum class LocationFloodState {
    Unflooded, Flooded, Sunken;

    fun flooded(): LocationFloodState = when (this) {
        Unflooded -> Flooded
        Flooded -> Sunken
        Sunken -> throw IllegalStateException("A sunken location should never be flooded again")
    }
}

/**
 * All the locations that appear on the map. There is one [starting location][startingLocationForAdventurer] for each
 * [Adventurer] and two [pickup locations][pickupLocationForTreasure] for each [Treasure].
 */
enum class Location(
        val displayName: String,
        val startingLocationForAdventurer: Adventurer? = null,
        val pickupLocationForTreasure: Treasure? = null) {

    FoolsLanding("Fool's Landing", startingLocationForAdventurer = Adventurer.Pilot),

    BronzeGate("Bronze Gate", startingLocationForAdventurer = Adventurer.Engineer),
    CopperGate("Copper Gate", startingLocationForAdventurer = Adventurer.Explorer),
    GoldGate("Gold Gate", startingLocationForAdventurer = Adventurer.Navigator),
    IronGate("Iron Gate", startingLocationForAdventurer = Adventurer.Diver),
    SilverGate("Silver Gate", startingLocationForAdventurer = Adventurer.Messenger),

    CaveOfEmbers("Cave of Embers", pickupLocationForTreasure = Treasure.CrystalOfFire),
    CaveOfShadows("Cave of Shadows", pickupLocationForTreasure = Treasure.CrystalOfFire),
    CoralPalace("Coral Palace", pickupLocationForTreasure = Treasure.OceansChalice),
    TidalPalace("Tidal Palace", pickupLocationForTreasure = Treasure.OceansChalice),
    HowlingGarden("Howling Garden", pickupLocationForTreasure = Treasure.StatueOfTheWind),
    WhisperingGarden("Whispering Garden", pickupLocationForTreasure = Treasure.StatueOfTheWind),
    TempleOfTheMoon("Temple of the Moon", pickupLocationForTreasure = Treasure.EarthStone),
    TempleOfTheSun("Temple of the Sun", pickupLocationForTreasure = Treasure.EarthStone),

    Watchtower("Watchtower"),
    MistyMarsh("Misty Marsh"),
    PhantomRock("Phantom Rock"),
    BreakersBridge("Breakers Bridge"),
    CrimsonForest("Crimson Forest"),
    Observatory("Observatory"),
    TwilightHollow("TwilightHollow"),
    CliffsOfAbandon("Cliffs of Abandon"),
    DunesOfDeception("Dunes of Deception"),
    LostLagoon("Lost Lagoon");

    override fun toString() = displayName + if (pickupLocationForTreasure != null) "^" else ""

    companion object {
        val allLocationsSet = values().toSet()
    }
}
