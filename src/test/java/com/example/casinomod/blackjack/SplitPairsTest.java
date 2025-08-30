package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SplitPairsTest {

  private BlackjackGame game;

  @BeforeEach
  void setUp() {
    game = new BlackjackGame();
    game.startGame();
  }

  @Test
  void testCanSplitWithMatchingPair() {
    // Set up a pair of 8s
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(8, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(8, Suit.SPADES));

    assertTrue(game.canSplit());
  }

  @Test
  void testCannotSplitWithDifferentCards() {
    // Set up non-matching cards
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(8, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(7, Suit.SPADES));

    assertFalse(game.canSplit());
  }

  @Test
  void testCannotSplitWithThreeCards() {
    // Set up hand with 3 cards
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(8, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(8, Suit.SPADES));
    game.getPlayerHandDirect(0).add(new Card(5, Suit.CLUBS));

    assertFalse(game.canSplit());
  }

  @Test
  void testCannotSplitWhenNotPlayerTurn() {
    // Set up a pair but wrong phase
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(8, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(8, Suit.SPADES));
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    assertFalse(game.canSplit());
  }

  @Test
  void testCannotSplitAfterAlreadySplit() {
    // Set up initial pair and split
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(8, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(8, Suit.SPADES));

    game.splitPairs();

    // Can no longer split
    assertFalse(game.canSplit());
    assertTrue(game.hasSplit());
  }

  @Test
  void testSplitPairsCreatesTransactions() {
    // Set up a pair of Kings
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(13, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(13, Suit.CLUBS));

    assertEquals(1, game.getHandCount());

    game.splitPairs();

    // Should now have 2 hands
    assertEquals(2, game.getHandCount());
    assertTrue(game.hasSplit());
    assertEquals(0, game.getCurrentHandIndex());

    // Each hand should have 2 cards after split (original + new card)
    assertEquals(2, game.getPlayerHand(0).size());
    assertEquals(2, game.getPlayerHand(1).size());

    // First cards should be the original split cards
    assertEquals(13, game.getPlayerHand(0).get(0).getValue());
    assertEquals(13, game.getPlayerHand(1).get(0).getValue());
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1", // Aces
    "2, 2", // Twos
    "10, 10", // Tens
    "11, 11", // Jacks
    "12, 12", // Queens
    "13, 13" // Kings
  })
  void testSplitPairsWithDifferentRanks(int rank1, int rank2) {
    // Set up matching pair
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(rank1, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(rank2, Suit.SPADES));

    assertTrue(game.canSplit());

    game.splitPairs();

    assertTrue(game.hasSplit());
    assertEquals(2, game.getHandCount());
    assertEquals(rank1, game.getPlayerHand(0).get(0).getValue());
    assertEquals(rank2, game.getPlayerHand(1).get(0).getValue());
  }

  @Test
  void testSplitPairsGameFlow() {
    // Set up a pair of 2s (low cards to avoid busting when hit)
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(2, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(2, Suit.DIAMONDS));

    game.splitPairs();

    // Should start with first hand
    assertEquals(0, game.getCurrentHandIndex());

    // Hit first hand - manually add a small card to avoid bust
    int handSizeBefore = game.getPlayerHand(0).size();
    game.getPlayerHandDirect(0)
        .add(new Card(3, Suit.CLUBS)); // Manually add instead of hitPlayer to control outcome

    // Move to next hand manually to test hand progression
    assertEquals(0, game.getCurrentHandIndex()); // Should still be on first hand

    // Stand on first hand
    game.stand();
    assertEquals(1, game.getCurrentHandIndex()); // Move to second hand

    // Add card to second hand manually
    game.getPlayerHandDirect(1).add(new Card(4, Suit.SPADES));
    assertEquals(3, game.getPlayerHand(1).size());

    // Stand on second hand
    game.stand();
    assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());
  }

  @Test
  void testSplitPairsHandProgression() {
    // Set up a pair
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(6, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(6, Suit.SPADES));

    game.splitPairs();

    // Bust first hand
    game.getPlayerHandDirect(0).add(new Card(10, Suit.CLUBS)); // 6 + new card + 10 = likely bust
    game.getPlayerHandDirect(0).add(new Card(10, Suit.DIAMONDS)); // Force over 21

    game.hitPlayer(); // This should trigger bust logic and move to next hand

    // Should move to hand 1 or finish game depending on whether hand busted
    assertTrue(
        game.getCurrentHandIndex() >= 1 || game.getPhase() == BlackjackGame.GamePhase.FINISHED);
  }

  @Test
  void testSplitPairsDoubleDown() {
    // Set up a pair suitable for double down
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(5, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(5, Suit.SPADES));

    game.splitPairs();

    // Should be able to double down on first hand (has exactly 2 cards)
    assertTrue(game.canDoubleDown());

    game.doubleDown();

    // Should move to second hand
    assertEquals(1, game.getCurrentHandIndex());

    // First hand should have 3 cards now
    assertEquals(3, game.getPlayerHand(0).size());
  }

  @Test
  void testSplitPairsResultEvaluation() {
    // Set up a pair
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(9, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(9, Suit.SPADES));

    // Set up dealer hand
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(7, Suit.DIAMONDS)); // Dealer 17

    game.splitPairs();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    // Manually set hand values for testing
    // Hand 0: 9 + 10 = 19 (should win against dealer 17)
    // Hand 1: 9 + 8 = 17 (should tie against dealer 17)
    game.getPlayerHandDirect(0).set(1, new Card(10, Suit.HEARTS)); // 19 total
    game.getPlayerHandDirect(1).set(1, new Card(8, Suit.CLUBS)); // 17 total

    // Individual hand results
    assertEquals(BlackjackGame.Result.WIN, game.determineResult(0)); // 19 vs 17
    assertEquals(BlackjackGame.Result.DRAW, game.determineResult(1)); // 17 vs 17

    // Overall result should be WIN (one hand wins, one draws, no losses)
    assertEquals(BlackjackGame.Result.WIN, game.determineResult());
  }

  @Test
  void testSplitPairsBlackjackNoLongerBlackjack() {
    // Set up Ace pair (would be blackjack but not after split)
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect(0).add(new Card(1, Suit.SPADES)); // Ace

    // Not blackjack because it's 2 aces (value 12)
    assertFalse(game.isBlackjack());

    game.splitPairs();

    // Even after split, individual hands with Ace + new card are not blackjack
    // because they result from a split
    assertFalse(game.isBlackjack(0));
    assertFalse(game.isBlackjack(1));
  }

  @Test
  void testSplitPairsReset() {
    // Set up and perform split
    game.getAllPlayerHandsDirect().clear();
    game.getAllPlayerHandsDirect().add(new java.util.ArrayList<>());
    game.getPlayerHandDirect(0).add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect(0).add(new Card(10, Suit.SPADES));

    game.splitPairs();
    assertTrue(game.hasSplit());
    assertEquals(2, game.getHandCount());

    // Reset should clear split state
    game.reset();
    assertFalse(game.hasSplit());
    assertEquals(0, game.getHandCount());
    assertEquals(0, game.getCurrentHandIndex());
  }
}
