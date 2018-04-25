package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Game.Companion.newRandomGameFor
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class GamePrinterTest {
    @Test
    fun printMap() {
        val seed = 1524652901387L
        val random = Random(seed)
        val game = newRandomGameFor(4, random)
                .withPlayerPosition(Adventurer.Messenger, Position(5, 5))
                .withPlayerPosition(Adventurer.Engineer, Position(5, 5))
        assertThat("\n" + GamePrinter.toString(game), `is`(
"""
                                                 (3,1):Observatory*          (4,1):GoldGate       """+"""
                         (2,2):CrimsonForest     (3,2):CaveOfShadows^*     P>(4,2):FoolsLanding        (5,2):CoralPalace^    """+"""
(1,3):LostLagoon*        (2,3):TempleOfTheSun^   (3,3):DunesOfDeception      (4,3):CaveOfEmbers^*      (5,3):BronzeGate*      (6,3):BreakersBridge  """+"""
(1,4):CliffsOfAbaondon   (2,4):TwilightHollow    (3,4):PhantomRock           (4,4):IronGate            (5,4):HowlingGarden^*  (6,4):MistyMarsh      """+"""
                         (2,5):TidalPalace^      (3,5):TempleOfTheMoon^      (4,5):Watchtower      EMX>(5,5):CopperGate      """+"""
                                                 (3,6):WhisperingGarden^     (4,6):SilverGate     """ + "\n"))
    }
}