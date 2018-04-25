package com.grahamlea.forbiddenisland

import java.util.*

sealed class GameResult(val detail: String? = null) {
    override fun toString() = this::class.simpleName!! + (detail?.let { " $it" } ?: "")
}

object AdventurersWon: GameResult(null)

object FoolsLandingSank: GameResult()

object MaximumWaterLevelReached: GameResult()

data class BothPickupLocationsSankBeforeCollectingTreasure(
    val treasure: Treasure,
    val locations: Pair<Location, Location> =
            Location.values().filter { it.pickupLocationForTreasure == treasure }.let { Pair(it[0], it[1]) }):
    GameResult("${locations.first} and ${locations.second} sank before $treasure was collected")

data class PlayerDrowned(val player: Adventurer): GameResult("$player was on an island that sank and couldn't swim to an adjacent one")

enum class Treasure(val displayName: String) {
    CrystalOfFire("The Crystal of Fire"),
    EarthStone("The Earth Stone"),
    OceansChalice("The Ocean's Chalice"),
    StatueOfTheWind("The Statue of the Wind")
}

enum class Adventurer {
    Diver, Engineer, Explorer, Messenger, Navigator, Pilot;

    val startingLocation get() = Location.values().first { it.startingLocationForAdventurer == this }

    companion object {
        fun randomListOfPlayers(numberOfPlayers: Int, random: Random = Random()): ImmutableList<Adventurer> = shuffled(random, numberOfPlayers)
    }
}

enum class FloodLevel(val tilesFloodingPerTurn: Int) {
    ONE(2), TWO(2), THREE(3), FOUR(3), FIVE(3), SIX(4), SEVEN(4), EIGHT(5), NINE(5), DEAD(0)
}

enum class StartingFloodLevel(val floodLevel: FloodLevel) {
    Novice(FloodLevel.ONE),
    Normal(FloodLevel.TWO),
    Elite(FloodLevel.THREE),
    Legendary(FloodLevel.FOUR)
}

enum class LocationFloodState {
    Unflooded, Flooded, Sunken
}

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
    CliffsOfAbaondon("Cliffs of Abaondon"),
    DunesOfDeception("Dunes of Deception"),
    LostLagoon("Lost Lagoon");

    override fun toString() = name + if (pickupLocationForTreasure != null) "^" else ""

    companion object {
        val allLocationsSet = values().toSet()
    }
}