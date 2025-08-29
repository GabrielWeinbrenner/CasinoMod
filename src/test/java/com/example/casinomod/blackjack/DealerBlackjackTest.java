package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DealerBlackjackTest {

  @Test
  void testDealerBlackjackDetection() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();

    // Clear hands and set up dealer blackjack
    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHand().add(new Card(10, Suit.SPADES)); // 10

    assertTrue(game.isDealerBlackjack());
  }

  @Test
  void testDealerNonBlackjack21() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();

    // 21 but not blackjack (more than 2 cards)
    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(7, Suit.HEARTS));
    game.getDealerHand().add(new Card(7, Suit.DIAMONDS));
    game.getDealerHand().add(new Card(7, Suit.CLUBS));

    assertFalse(game.isDealerBlackjack());
  }

  @Test
  void testDealerBlackjackVariations() {
    BlackjackGame game = new BlackjackGame();

    // Test Ace + Jack
    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(1, Suit.CLUBS));
    game.getDealerHand().add(new Card(11, Suit.HEARTS));
    assertTrue(game.isDealerBlackjack());

    // Test Ace + Queen
    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(1, Suit.DIAMONDS));
    game.getDealerHand().add(new Card(12, Suit.SPADES));
    assertTrue(game.isDealerBlackjack());

    // Test Ace + King
    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(1, Suit.SPADES));
    game.getDealerHand().add(new Card(13, Suit.CLUBS));
    assertTrue(game.isDealerBlackjack());
  }

  @Test
  void testPlayerVsDealerBlackjack() {
    BlackjackGame game = new BlackjackGame();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    // Both have blackjack - should be a draw
    game.getPlayerHand().clear();
    game.getPlayerHand().add(new Card(1, Suit.HEARTS));
    game.getPlayerHand().add(new Card(10, Suit.CLUBS));

    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(1, Suit.SPADES));
    game.getDealerHand().add(new Card(11, Suit.DIAMONDS));

    assertTrue(game.isBlackjack());
    assertTrue(game.isDealerBlackjack());
    assertEquals(BlackjackGame.Result.DRAW, game.determineResult());
  }

  @Test
  void testPlayerBlackjackVsDealerRegular() {
    BlackjackGame game = new BlackjackGame();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    // Player has blackjack, dealer has regular 20
    game.getPlayerHand().clear();
    game.getPlayerHand().add(new Card(1, Suit.HEARTS));
    game.getPlayerHand().add(new Card(10, Suit.CLUBS));

    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(10, Suit.SPADES));
    game.getDealerHand().add(new Card(10, Suit.DIAMONDS));

    assertTrue(game.isBlackjack());
    assertFalse(game.isDealerBlackjack());
    assertEquals(BlackjackGame.Result.WIN, game.determineResult());
  }

  @Test
  void testDealerBlackjackVsPlayerRegular() {
    BlackjackGame game = new BlackjackGame();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    // Dealer has blackjack, player has regular 20
    game.getPlayerHand().clear();
    game.getPlayerHand().add(new Card(10, Suit.HEARTS));
    game.getPlayerHand().add(new Card(10, Suit.CLUBS));

    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(1, Suit.SPADES));
    game.getDealerHand().add(new Card(13, Suit.DIAMONDS));

    assertFalse(game.isBlackjack());
    assertTrue(game.isDealerBlackjack());
    assertEquals(BlackjackGame.Result.LOSE, game.determineResult());
  }

  @Test
  void testNoBlackjacksRegularComparison() {
    BlackjackGame game = new BlackjackGame();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    // Neither has blackjack, player wins with 20 vs 19
    game.getPlayerHand().clear();
    game.getPlayerHand().add(new Card(10, Suit.HEARTS));
    game.getPlayerHand().add(new Card(10, Suit.CLUBS));

    game.getDealerHand().clear();
    game.getDealerHand().add(new Card(9, Suit.SPADES));
    game.getDealerHand().add(new Card(10, Suit.DIAMONDS));

    assertFalse(game.isBlackjack());
    assertFalse(game.isDealerBlackjack());
    assertEquals(BlackjackGame.Result.WIN, game.determineResult());
  }
}
