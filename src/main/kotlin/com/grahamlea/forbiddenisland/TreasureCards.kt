package com.grahamlea.forbiddenisland

import java.util.*

/** Represents a card that can be held by a [player][Adventurer]. */
sealed class HoldableCard(val displayName: String): Comparable<HoldableCard> {

    override fun compareTo(other: HoldableCard) = this.displayName.compareTo(other.displayName)

    override fun toString() = displayName

    companion object {
        val allCardTypes by lazy { TreasureDeck.newShuffledDeck().toSet() }
    }
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

    val totalCardCounts by lazy { newShuffledDeck().groupingBy { it }.eachCount() }

    /**
     * Returns a new shuffled "Treasure Deck", which includes all [TreasureCard]s, [HelicopterLiftCard]s,
     * [SandbagsCard]s and [WatersRiseCard]s.
     */
    fun newShuffledDeck(random: Random = Random()): ImmutableList<HoldableCard> =
        (HelicopterLiftCard * numberOfHelicopterLiftCards +
         SandbagsCard * numberOfSandbagsCards +
         WatersRiseCard * numberOfWatersRiseCards +
         Treasure.values().flatMap { TreasureCard(it) * numberOfEachTreasureCard })
        .shuffled(random)
        .imm()
}

/** Helper method that returns an [ImmutableList] of the provided [cards] */
fun cards(vararg cards: HoldableCard): ImmutableList<HoldableCard> = immListOf(*cards)

/** Helper method that returns a list containing [n] instances the card that is the receiver. */
infix operator fun HoldableCard.times(n: Int) = List(n, {this})
