package com.grahamlea.forbiddenisland

import java.util.*

sealed class HoldableCard(val displayName: String): Comparable<HoldableCard> {

    override fun compareTo(other: HoldableCard) = this.displayName.compareTo(other.displayName)

    override fun toString() = displayName
}

object HelicopterLiftCard : HoldableCard("Helicopter Lift")

object SandbagsCard : HoldableCard("Sandbags")

object WatersRiseCard : HoldableCard("Waters Rise!")

data class TreasureCard(val treasure: Treasure) : HoldableCard(treasure.displayName) {
    override fun toString() = super.toString() // because 'data class'
}

object TreasureDeck {
    private const val numberOfHelicopterLiftCards = 3
    private const val numberOfSandbagsCards = 2
    private const val numberOfWatersRiseCards = 3
    private const val numberOfEachTreasureCard = 5
    val totalCardCounts = newShuffledDeck().groupingBy { it }.eachCount()

    fun newShuffledDeck(random: Random = Random()): ImmutableList<HoldableCard> =
        (HelicopterLiftCard * numberOfHelicopterLiftCards +
         SandbagsCard * numberOfSandbagsCards +
         WatersRiseCard * numberOfWatersRiseCards +
         Treasure.values().flatMap { TreasureCard(it) * numberOfEachTreasureCard })
        .shuffled(random)
        .immutable()
}

infix operator fun HoldableCard.times(n: Int) = List(n, {this})

