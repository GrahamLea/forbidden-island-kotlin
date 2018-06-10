package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.LocationFloodState.Sunken
import com.grahamlea.forbiddenisland.Treasure.EarthStone
import com.grahamlea.forbiddenisland.Treasure.OceansChalice
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GamePhase transitions")
class GamePhaseTest {

    private val position33 = Position(3, 3)
    private val randomNewGame = Game.newRandomGameFor(immListOf(Engineer, Messenger, Diver, Explorer))
    private val randomNewGameState = randomNewGame.gameState

    private val engineerHasSixCards = Game.newRandomGameFor(immListOf(Engineer, Messenger, Diver, Explorer))
            .withPlayerCards(
                    Engineer to (TreasureCard(OceansChalice) * 5 + TreasureCard(EarthStone)).imm(),
                    Messenger to cards(),
                    Diver to cards(),
                    Explorer to cards()
            )

    @Nested @DisplayName("awaiting player action")
    inner class AwaitingPlayerActionTests {
        @Test
        fun `+ move = awaiting player action with one less action`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerAction(Engineer, 3),
                action = Move(Engineer, position33),
                expectedPhase = AwaitingPlayerAction(Engineer, 2)
            )
        }

        @Test
        fun `+ fly = awaiting player action with one less action`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerAction(Pilot, 3),
                action = Fly(Pilot, position33),
                expectedPhase = AwaitingPlayerAction(Pilot, 2)
            )
        }

        @Test
        fun `+ shore up = awaiting player action with one less action`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerAction(Engineer, 2),
                action = ShoreUp(Engineer, position33),
                expectedPhase = AwaitingPlayerAction(Engineer, 1)
            )
        }

        @Test
        fun `+ capture treasure = awaiting player action with one less action`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerAction(Engineer, 3),
                action = CaptureTreasure(Engineer, EarthStone),
                expectedPhase = AwaitingPlayerAction(Engineer, 2)
            )
        }

        @Test
        fun `+ give treasure card = awaiting player action with one less action`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerAction(Engineer, 3),
                action = GiveTreasureCard(Engineer, Messenger, TreasureCard(EarthStone)),
                expectedPhase = AwaitingPlayerAction(Engineer, 2)
            )
        }

        @Test
        fun `+ give treasure card to player with 5 cards = awaiting receiving player to discard card, then back to this player`() {
            val messengerHasSixCards = Game.newRandomGameFor(immListOf(Engineer, Messenger, Diver, Explorer))
                .withPlayerCards(
                    Engineer to cards(),
                    Messenger to (TreasureCard(OceansChalice) * 5 + TreasureCard(EarthStone)).imm(),
                    Diver to cards(),
                    Explorer to cards()
                )

            checkPhaseTransition(
                firstPhase = AwaitingPlayerAction(Engineer, 3),
                action = GiveTreasureCard(Engineer, Messenger, TreasureCard(EarthStone)),
                gameStateAfterActionProcessed = messengerHasSixCards.gameState,
                expectedPhase = AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Engineer, 2))
            )
        }

        @Test
        fun `+ early treasure deck draw with 5 cards = awaiting player to discard card, then continuing to draw 1 more treasure card`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerAction(Engineer, 2),
                action = DrawFromTreasureDeck(Engineer),
                gameStateAfterActionProcessed = engineerHasSixCards.gameState,
                expectedPhase = AwaitingPlayerToDiscardExtraCard(Engineer, AwaitingTreasureDeckDraw(Engineer, 1))
            )
        }

        @Nested @DisplayName("LAST action")
        inner class LastActionTests {
            @Test
            fun `+ move = awaiting draw 2 treasure cards for same player`() {
                checkPhaseTransition(
                    firstPhase = AwaitingPlayerAction(Engineer, 1),
                    action = Move(Engineer, position33),
                    expectedPhase = AwaitingTreasureDeckDraw(Engineer, 2)
                )
            }

            @Test
            fun `+ early treasure deck draw = awaiting draw 1 treasure cards for same player`() {
                checkPhaseTransition(
                    firstPhase = AwaitingPlayerAction(Engineer, 1),
                    action = DrawFromTreasureDeck(Engineer),
                    expectedPhase = AwaitingTreasureDeckDraw(Engineer, 1)
                )
            }
        }
    }

    @Nested @DisplayName("awaiting treasure deck draw")
    inner class AwaitingTreasureDeckDrawTests {
        @Test
        fun `+ treasure deck draw = awaiting one less treasure deck draw`() {
            checkPhaseTransition(
                firstPhase = AwaitingTreasureDeckDraw(Engineer, 2),
                action = DrawFromTreasureDeck(Engineer),
                expectedPhase = AwaitingTreasureDeckDraw(Engineer, 1)
            )
        }

        @Test
        fun `+ treasure deck draw to have 6 cards = awaiting player to discard card, then continuing to draw 1 more treasure card`() {
            checkPhaseTransition(
                firstPhase = AwaitingTreasureDeckDraw(Engineer, 2),
                action = DrawFromTreasureDeck(Engineer),
                gameStateAfterActionProcessed = engineerHasSixCards.gameState,
                expectedPhase = AwaitingPlayerToDiscardExtraCard(Engineer, AwaitingTreasureDeckDraw(Engineer, 1))
            )
        }

        @Nested @DisplayName("LAST draw")
        inner class LastDrawTests {
            @Test
            fun `awaiting LAST treasure deck draw + treasure deck draw = awaiting flood deck draw corresponding to flood level`() {
                checkPhaseTransition(
                    firstPhase = AwaitingTreasureDeckDraw(Engineer, 1),
                    action = DrawFromTreasureDeck(Engineer),
                    gameStateAfterActionProcessed = randomNewGameState.copy(floodLevel = FloodLevel.SEVEN),
                    expectedPhase = AwaitingFloodDeckDraw(Engineer, 4)
                )
            }

            @Test
            fun `awaiting LAST treasure deck draw + treasure deck draw to have 6 cards = awaiting player to discard card, then continuing to awaiting flood deck draw corresponding to flood level`() {
                checkPhaseTransition(
                    firstPhase = AwaitingTreasureDeckDraw(Engineer, 1),
                    action = DrawFromTreasureDeck(Engineer),
                    gameStateAfterActionProcessed = engineerHasSixCards.gameState.copy(floodLevel = FloodLevel.SEVEN),
                    expectedPhase = AwaitingPlayerToDiscardExtraCard(Engineer, AwaitingFloodDeckDraw(Engineer, 4))
                )
            }
        }
    }

    @Nested @DisplayName("awaiting flood deck draw")
    inner class AwaitingFloodDeckDrawTests {
        @Test
        fun `+ flood deck draw = awaiting one less flood deck draw`() {
            checkPhaseTransition(
                firstPhase = AwaitingFloodDeckDraw(Engineer, 3),
                action = DrawFromFloodDeck(Engineer),
                expectedPhase = AwaitingFloodDeckDraw(Engineer, 2)
            )
        }

        @Nested @DisplayName("LAST draw")
        inner class LastDrawTests {
            @Test
            fun `+ flood deck draw = awaiting NEXT player action`() {
                checkPhaseTransition(
                    firstPhase = AwaitingFloodDeckDraw(Engineer, 1),
                    action = DrawFromFloodDeck(Engineer),
                    expectedPhase = AwaitingPlayerAction(Messenger, 3)
                )
            }

            @Test
            fun `of LAST player + flood deck draw = awaiting FIRST player action`() {
                checkPhaseTransition(
                    firstPhase = AwaitingFloodDeckDraw(Explorer, 1),
                    action = DrawFromFloodDeck(Explorer),
                    expectedPhase = AwaitingPlayerAction(Engineer, 3)
                )
            }
        }

        @Nested @DisplayName("draw causes player/s to sink")
        inner class PlayersSinkTests {
            @Test
            fun `first draw + any player sinks = awaiting swim then next treasure draw`() {
                val sunkPosition = Position(3, 3)
                val otherPosition = Position(3, 1)
                val messengerIsSunk = randomNewGame
                    .withPlayerPosition(Engineer, otherPosition)
                    .withPlayerPosition(Messenger, sunkPosition)
                    .withPlayerPosition(Diver, otherPosition)
                    .withPlayerPosition(Explorer, otherPosition)
                    .withPositionFloodStates(Sunken, sunkPosition)

                checkPhaseTransition(
                    firstPhase = AwaitingFloodDeckDraw(Engineer, 3),
                    action = DrawFromFloodDeck(Engineer),
                    gameStateAfterActionProcessed = messengerIsSunk.gameState,
                    expectedPhase = AwaitingPlayerToSwimToSafety(Messenger, AwaitingFloodDeckDraw(Engineer, 2))
                )
            }

            @Test
            fun `multiple players sunk = multiple players needing to swim before progressing`() {
                val sunkPosition = Position(3, 3)
                val otherPosition = Position(3, 1)
                val messengerAndExplorerAreSunk = randomNewGame
                    .withPlayerPosition(Engineer, otherPosition)
                    .withPlayerPosition(Messenger, sunkPosition)
                    .withPlayerPosition(Diver, otherPosition)
                    .withPlayerPosition(Explorer, sunkPosition)
                    .withPositionFloodStates(Sunken, sunkPosition)

                checkPhaseTransition(
                    firstPhase = AwaitingFloodDeckDraw(Engineer, 3),
                    action = DrawFromFloodDeck(Engineer),
                    gameStateAfterActionProcessed = messengerAndExplorerAreSunk.gameState,
                    expectedPhase = AwaitingPlayerToSwimToSafety(Messenger, AwaitingPlayerToSwimToSafety(Explorer, AwaitingFloodDeckDraw(Engineer, 2)))
                )
            }

            @Test
            fun `LAST draw of LAST player + player sinks = awaiting swim then awaiting FIRST player action`() {
                val sunkPosition = Position(3, 3)
                val otherPosition = Position(3, 1)
                val messengerIsSunk = randomNewGame
                    .withPlayerPosition(Engineer, otherPosition)
                    .withPlayerPosition(Messenger, sunkPosition)
                    .withPlayerPosition(Diver, otherPosition)
                    .withPlayerPosition(Explorer, otherPosition)
                    .withPositionFloodStates(Sunken, sunkPosition)

                checkPhaseTransition(
                    firstPhase = AwaitingFloodDeckDraw(Explorer, 1),
                    action = DrawFromFloodDeck(Explorer),
                    gameStateAfterActionProcessed = messengerIsSunk.gameState,
                    expectedPhase = AwaitingPlayerToSwimToSafety(Messenger, AwaitingPlayerAction(Engineer, 3))
                )
            }
        }
    }

    @Nested @DisplayName("awaiting discard card")
    inner class AwaitingDiscardCardTests {
        @Test
        fun `+ discard card = back to play of previous phase`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Engineer, 2)),
                action = DiscardCard(Messenger, TreasureCard(EarthStone)),
                expectedPhase = AwaitingPlayerAction(Engineer, 2)
            )
        }

        @Test
        fun `+ player sandbags card = back to play of previous phase`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Diver, 3)),
                action = Sandbag(Messenger, position33),
                expectedPhase = AwaitingPlayerAction(Diver, 3)
            )
        }

        @Test
        fun `+ player helicopter lift card = back to play of previous phase`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Engineer, 1)),
                action = HelicopterLift(Messenger, immSetOf(Pilot), position33),
                expectedPhase = AwaitingPlayerAction(Engineer, 1)
            )
        }

        @Test
        fun `+ non-card-discarding action is illegal`() {
            Assertions.assertThrows(IllegalStateException::class.java) {
                AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Engineer, 1))
                    .phaseAfter(action = DrawFromTreasureDeck(Engineer), nextGameState = randomNewGameState)
            }
        }

        @Test
        fun `+ card-discarding action for another player is illegal`() {
            Assertions.assertThrows(IllegalStateException::class.java) {
                AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Engineer, 1))
                    .phaseAfter(action = DiscardCard(Engineer, TreasureCard(EarthStone)), nextGameState = randomNewGameState)
            }
        }
    }

    @Nested @DisplayName("awaiting swim to safety")
    inner class AwaitingSwimToSafetyTests {
        @Test
        fun `+ swim = back to play of next phase`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerToSwimToSafety(Messenger, AwaitingFloodDeckDraw(Engineer, 1)),
                action = SwimToSafety(Messenger, position33),
                expectedPhase = AwaitingFloodDeckDraw(Engineer, 1)
            )
        }

        @Test
        fun `+ non-swim action is illegal`() {
            Assertions.assertThrows(IllegalStateException::class.java) {
                AwaitingPlayerToSwimToSafety(Messenger, AwaitingPlayerAction(Engineer, 1))
                    .phaseAfter(action = DrawFromTreasureDeck(Engineer), nextGameState = randomNewGameState)
            }
        }

        @Test
        fun `+ swim action for another player is illegal`() {
            Assertions.assertThrows(IllegalStateException::class.java) {
                AwaitingPlayerToSwimToSafety(Messenger, AwaitingPlayerAction(Engineer, 1))
                    .phaseAfter(action = SwimToSafety(Engineer, position33), nextGameState = randomNewGameState)
            }
        }

        @Test
        fun `nested awaiting to swims are unfolded sequentially`() {
            checkPhaseTransition(
                firstPhase = AwaitingPlayerToSwimToSafety(Messenger, AwaitingPlayerToSwimToSafety(Explorer, AwaitingFloodDeckDraw(Engineer, 1))),
                action = SwimToSafety(Messenger, position33),
                expectedPhase = AwaitingPlayerToSwimToSafety(Explorer, AwaitingFloodDeckDraw(Engineer, 1))
            )
        }
    }

    @Test
    fun `helicopter lift or sandbag or swim on most phases = no phase change`() {
        val phases = listOf(
            AwaitingPlayerAction(Engineer, 2),
            AwaitingTreasureDeckDraw(Engineer, 3),
            AwaitingFloodDeckDraw(Engineer, 3)
            // AwaitingPlayerToDiscardExtraCard and AwaitingPlayerToSwimToSafety behave differently
        )

        val actions = listOf<GameAction>(
            HelicopterLift(Engineer, immSetOf(Diver), position33),
            Sandbag(Engineer, position33),
            SwimToSafety(Engineer, position33)
        )

        for (phase in phases) {
            for (action in actions) {
                assertThat(phase.phaseAfter(action, randomNewGameState)).isEqualTo(phase)
            }
        }
    }

    @Test
    fun `game is over when next state has result`() {
        val phases = listOf(
            AwaitingPlayerAction(Engineer, 2),
            AwaitingTreasureDeckDraw(Engineer, 3),
            AwaitingFloodDeckDraw(Engineer, 3),
            AwaitingPlayerToDiscardExtraCard(Navigator, AwaitingPlayerAction(Navigator, 2)),
            AwaitingPlayerToSwimToSafety(Navigator, AwaitingPlayerAction(Navigator, 3))
        )

        val gameWithResult = randomNewGame.withLocationFloodStates(Sunken, Location.FoolsLanding).gameState

        for (phase in phases) {
            assertThat(phase.phaseAfter(Sandbag(Engineer, position33), gameWithResult)).isEqualTo(GameOver)
        }
    }

    @Test
    fun `any action played on game over is an error`() {
        val actions = listOf(
                Move(Engineer, position33),
                ShoreUp(Engineer, position33),
                GiveTreasureCard(Engineer, Messenger, TreasureCard(EarthStone)),
                CaptureTreasure(Engineer, EarthStone),
                HelicopterLift(Engineer, immSetOf(Diver), position33),
                Sandbag(Engineer, position33),
                SwimToSafety(Engineer, position33),
                DrawFromTreasureDeck(Engineer),
                DrawFromFloodDeck(Engineer),
                HelicopterLiftOffIsland(Engineer)
        )

        for (action in actions) {
            try {
                GameOver.phaseAfter(action, randomNewGameState)
                fail("Expected IllegalStateException")
            } catch (e: IllegalStateException) {
                // expected
            }
        }
    }

    private fun checkPhaseTransition(firstPhase: GamePhase, action: GameAction, gameStateAfterActionProcessed: GameState = randomNewGameState, expectedPhase: GamePhase) {
        assertThat(firstPhase.phaseAfter(action, gameStateAfterActionProcessed)).isEqualTo(expectedPhase)
    }
}

