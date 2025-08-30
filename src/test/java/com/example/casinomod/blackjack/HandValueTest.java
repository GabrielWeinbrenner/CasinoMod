package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HandValueTest {

  @Test
  void testBasicHandValues() {
    BlackjackGame game = new BlackjackGame();

    // Test simple numeric cards
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(5, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(7, Suit.SPADES));
    assertEquals(12, game.getHandValue(game.getPlayerHand()));

    // Test face cards
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(11, Suit.CLUBS)); // Jack
    game.getPlayerHandDirect().add(new Card(12, Suit.DIAMONDS)); // Queen
    assertEquals(20, game.getHandValue(game.getPlayerHand()));

    // Test King
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(13, Suit.HEARTS)); // King
    game.getPlayerHandDirect().add(new Card(5, Suit.SPADES));
    assertEquals(15, game.getHandValue(game.getPlayerHand()));
  }

  @Test
  void testAceHandling() {
    BlackjackGame game = new BlackjackGame();

    // Ace counted as 11 (soft hand)
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(6, Suit.SPADES));
    assertEquals(17, game.getHandValue(game.getPlayerHand())); // 11 + 6 = 17

    // Ace counted as 1 (hard hand - would bust if 11)
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(7, Suit.SPADES));
    game.getPlayerHandDirect().add(new Card(8, Suit.CLUBS));
    assertEquals(16, game.getHandValue(game.getPlayerHand())); // 1 + 7 + 8 = 16

    // Blackjack (Ace + 10-value card)
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(10, Suit.SPADES));
    assertEquals(21, game.getHandValue(game.getPlayerHand())); // 11 + 10 = 21
  }

  @Test
  void testMultipleAces() {
    BlackjackGame game = new BlackjackGame();

    // Two aces - one as 11, one as 1
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(1, Suit.SPADES)); // Ace
    assertEquals(12, game.getHandValue(game.getPlayerHand())); // 11 + 1 = 12

    // Three aces - all as 1
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(1, Suit.SPADES)); // Ace
    game.getPlayerHandDirect().add(new Card(1, Suit.CLUBS)); // Ace
    assertEquals(13, game.getHandValue(game.getPlayerHand())); // 1 + 1 + 11 = 13

    // Four aces
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(1, Suit.SPADES)); // Ace
    game.getPlayerHandDirect().add(new Card(1, Suit.CLUBS)); // Ace
    game.getPlayerHandDirect().add(new Card(1, Suit.DIAMONDS)); // Ace
    assertEquals(14, game.getHandValue(game.getPlayerHand())); // 1 + 1 + 1 + 11 = 14
  }

  @ParameterizedTest
  @CsvSource({
    "1, 10, 21", // Ace + 10
    "1, 11, 21", // Ace + Jack
    "1, 12, 21", // Ace + Queen
    "1, 13, 21", // Ace + King
    "10, 10, 20", // 10 + 10
    "5, 6, 11", // 5 + 6
    "2, 3, 5" // 2 + 3
  })
  void testTwoCardHands(int card1Value, int card2Value, int expectedTotal) {
    BlackjackGame game = new BlackjackGame();
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(card1Value, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(card2Value, Suit.SPADES));

    assertEquals(expectedTotal, game.getHandValue(game.getPlayerHand()));
  }

  @Test
  void testBustScenarios() {
    BlackjackGame game = new BlackjackGame();

    // Simple bust
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(10, Suit.SPADES));
    game.getPlayerHandDirect().add(new Card(5, Suit.CLUBS));
    assertEquals(25, game.getHandValue(game.getPlayerHand()));
    assertTrue(game.getHandValue(game.getPlayerHand()) > 21);

    // Ace adjustment prevents bust
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace (starts as 11)
    game.getPlayerHandDirect().add(new Card(6, Suit.SPADES)); // 11 + 6 = 17
    game.getPlayerHandDirect()
        .add(
            new Card(
                7, Suit.CLUBS)); // Would be 11 + 6 + 7 = 24, but ace becomes 1, so 1 + 6 + 7 = 14
    assertEquals(14, game.getHandValue(game.getPlayerHand()));
    assertFalse(game.getHandValue(game.getPlayerHand()) > 21);
  }

  @Test
  void testDealerHandValues() {
    BlackjackGame game = new BlackjackGame();

    // Test dealer hand calculation works the same
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(6, Suit.SPADES));
    assertEquals(17, game.getHandValue(game.getDealerHand()));

    // Test dealer blackjack
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.CLUBS)); // Ace
    game.getDealerHandDirect().add(new Card(10, Suit.DIAMONDS));
    assertEquals(21, game.getHandValue(game.getDealerHand()));
    assertTrue(game.isDealerBlackjack());
  }

  @Test
  void testEmptyHandValue() {
    BlackjackGame game = new BlackjackGame();

    // Empty hands should have value 0
    game.getPlayerHandDirect().clear();
    game.getDealerHandDirect().clear();
    assertEquals(0, game.getHandValue(game.getPlayerHand()));
    assertEquals(0, game.getHandValue(game.getDealerHand()));
  }

  @Test
  void testSingleCardValues() {
    BlackjackGame game = new BlackjackGame();

    // Test all single card values
    for (int value = 1; value <= 13; value++) {
      game.getPlayerHandDirect().clear();
      game.getPlayerHandDirect().add(new Card(value, Suit.HEARTS));

      int expectedValue;
      if (value == 1) {
        expectedValue = 11; // Ace counted as 11 when possible
      } else if (value >= 10) {
        expectedValue = 10; // Face cards worth 10
      } else {
        expectedValue = value;
      }

      assertEquals(
          expectedValue,
          game.getHandValue(game.getPlayerHand()),
          "Card value " + value + " should have hand value " + expectedValue);
    }
  }

  @Test
  void testComplexAceScenarios() {
    BlackjackGame game = new BlackjackGame();

    // Soft 18 (Ace + 7)
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(7, Suit.SPADES));
    assertEquals(18, game.getHandValue(game.getPlayerHand()));

    // Hit soft 18 with a 5 -> becomes hard 13
    game.getPlayerHandDirect().add(new Card(5, Suit.CLUBS));
    assertEquals(13, game.getHandValue(game.getPlayerHand())); // 1 + 7 + 5 = 13

    // Soft 19 (Ace + 8)
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.DIAMONDS)); // Ace
    game.getPlayerHandDirect().add(new Card(8, Suit.HEARTS));
    assertEquals(19, game.getHandValue(game.getPlayerHand()));
  }
}
