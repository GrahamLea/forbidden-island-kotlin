package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Location.MistyMarsh
import com.grahamlea.forbiddenisland.Treasure.EarthStone
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as is_

class GamePhaseTest {

    private val mapSite = MapSite(Position(3, 3), MistyMarsh)

    @Test
    fun `awaiting player action + move = awaiting player action with one less action`() {
        checkPhaseTransition(
            firstPhase = AwaitingPlayerAction(Engineer, 3),
            event = Move(Engineer, mapSite),
            expectedPhase = AwaitingPlayerAction(Engineer, 2)
        )
    }

    @Test
    fun `awaiting player action + shore up = awaiting player action with one less action`() {
        checkPhaseTransition(
            firstPhase = AwaitingPlayerAction(Engineer, 2),
            event = ShoreUp(Engineer, mapSite),
            expectedPhase = AwaitingPlayerAction(Engineer, 1)
        )
    }

    @Test
    fun `awaiting player action + capture treasure = awaiting player action with one less action`() {
        checkPhaseTransition(
            firstPhase = AwaitingPlayerAction(Engineer, 3),
            event = CaptureTreasure(Engineer, EarthStone),
            expectedPhase = AwaitingPlayerAction(Engineer, 2)
        )
    }

    @Test
    fun `awaiting player action + give treasure card = awaiting player action with one less action`() {
        checkPhaseTransition(
            firstPhase = AwaitingPlayerAction(Engineer, 3),
            event = GiveTreasureCard(Engineer, Messenger, TreasureCard(EarthStone)),
            expectedPhase = AwaitingPlayerAction(Engineer, 2)
        )
    }

    @Test
    fun `awaiting final player action + move = awaiting draw 2 treasure cards for same player`() {
        checkPhaseTransition(
            firstPhase = AwaitingPlayerAction(Engineer, 1),
            event = Move(Engineer, mapSite),
            expectedPhase = AwaitingTreasureDeckDraw(Engineer, 2)
        )
    }

    @Test
    fun `awaiting final player action + early treasure deck draw = awaiting draw 1 treasure cards for same player`() {
        checkPhaseTransition(
            firstPhase = AwaitingPlayerAction(Engineer, 1),
            event = DrawFromTreasureDeck(Engineer),
            expectedPhase = AwaitingTreasureDeckDraw(Engineer, 1)
        )
    }

    @Test
    fun `helicopter lift or sandbag or swim on any phase = no phase change`() {
        val phases = listOf(
            AwaitingPlayerAction(Engineer, 2),
            AwaitingPlayerToDiscardExtraCards(Engineer, 1), // Hmmmm... might need to deal with this especially?
            AwaitingTreasureDeckDraw(Engineer, 3),
            AwaitingFloodDeckDraw(Engineer, 3)
        )

        val events = listOf(
            HelicopterLift(Engineer, Diver, mapSite),
            Sandbag(Engineer, mapSite),
            Swim(Engineer, mapSite)
        )

        for (phase in phases) {
            for (event in events) {
                assertThat(phase.phaseAfter(event), is_(phase))
            }
        }
    }
}

private fun checkPhaseTransition(firstPhase: GamePhase, event: GameEvent, expectedPhase: GamePhase) {
    assertThat(firstPhase.phaseAfter(event), is_(expectedPhase))
}
