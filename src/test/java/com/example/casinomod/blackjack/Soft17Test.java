package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Soft17Test {

  private BlackjackGame game;

  @BeforeEach
  void setUp() {
    game = new BlackjackGame();
    game.startGame();

    // Set game to dealer turn for testing dealer logic
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);
  }

  @Test
  void testIsSoftSeventeen_AceSix() {
    // A-6 = Soft 17
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(6, Suit.SPADES)); // 6

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertTrue(game.isSoftSeventeen(game.getDealerHand()));
  }

  @Test
  void testIsSoftSeventeen_AceTwoFour() {
    // A-2-4 = Soft 17
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(2, Suit.SPADES)); // 2
    game.getDealerHandDirect().add(new Card(4, Suit.CLUBS)); // 4

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertTrue(game.isSoftSeventeen(game.getDealerHand()));
  }

  @Test
  void testIsSoftSeventeen_AceAceFive() {
    // A-A-5 = Soft 17 (one ace as 11, one as 1)
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(1, Suit.SPADES)); // Ace
    game.getDealerHandDirect().add(new Card(5, Suit.CLUBS)); // 5

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertTrue(game.isSoftSeventeen(game.getDealerHand()));
  }

  @Test
  void testIsSoftSeventeen_HardSeventeen() {
    // 10-7 = Hard 17 (no aces)
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(10, Suit.HEARTS)); // 10
    game.getDealerHandDirect().add(new Card(7, Suit.SPADES)); // 7

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertFalse(game.isSoftSeventeen(game.getDealerHand()));
  }

  @Test
  void testIsSoftSeventeen_AcesSeven() {
    // A-A-A-A-A-A-A-10 = Hard 17 (all aces as 1)
    game.getDealerHandDirect().clear();
    for (int i = 0; i < 6; i++) {
      game.getDealerHandDirect().add(new Card(1, Suit.values()[i % 4])); // 6 Aces
    }
    game.getDealerHandDirect().add(new Card(10, Suit.HEARTS)); // 10

    // Should be 6 + 10 = 16, not 17
    assertEquals(16, game.getHandValue(game.getDealerHand()));
    assertFalse(game.isSoftSeventeen(game.getDealerHand()));
  }

  @Test
  void testIsSoftSeventeen_NotSeventeen() {
    // A-5 = 16, not 17
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(5, Suit.SPADES)); // 5

    assertEquals(16, game.getHandValue(game.getDealerHand()));
    assertFalse(game.isSoftSeventeen(game.getDealerHand()));
  }

  @ParameterizedTest
  @CsvSource({
    "1, 6, true", // A-6 = Soft 17
    "1, 2, false", // A-2 = 13, not 17
    "1, 7, false", // A-7 = 18, not 17
    "10, 7, false", // 10-7 = Hard 17
    "9, 8, false", // 9-8 = Hard 17
    "5, 5, false" // 5-5-7 would need third card
  })
  void testIsSoftSeventeenTwoCards(int card1Value, int card2Value, boolean expectedSoft17) {
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(card1Value, Suit.HEARTS));
    game.getDealerHandDirect().add(new Card(card2Value, Suit.SPADES));

    if (card1Value == 1 && card2Value == 6) {
      assertEquals(17, game.getHandValue(game.getDealerHand()));
    }

    assertEquals(expectedSoft17, game.isSoftSeventeen(game.getDealerHand()));
  }

  @Test
  void testDealerStandsOnSoft17_ConfigDisabled() {
    // Set up soft 17
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(6, Suit.SPADES)); // 6

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertTrue(game.isSoftSeventeen(game.getDealerHand()));

    // Dealer should stand (not hit) when config is disabled (false)
    boolean shouldHit = game.hitDealer(false);
    assertFalse(shouldHit);
    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testDealerHitsOnSoft17_ConfigEnabled() {
    // Set up soft 17
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(6, Suit.SPADES)); // 6

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertTrue(game.isSoftSeventeen(game.getDealerHand()));

    // Dealer should hit on soft 17 when config is enabled (true)
    int originalSize = game.getDealerHand().size();
    game.hitDealer(true);
    int newSize = game.getDealerHand().size();

    // Verify a card was drawn
    assertEquals(3, newSize, "Dealer should have drawn a card");
    assertTrue(newSize > originalSize, "Dealer hand size should have increased");
  }

  @Test
  void testDealerStandsOnHard17_ConfigEnabled() {
    // Set up hard 17
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(10, Suit.HEARTS)); // 10
    game.getDealerHandDirect().add(new Card(7, Suit.SPADES)); // 7

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertFalse(game.isSoftSeventeen(game.getDealerHand()));

    // Dealer should still stand on hard 17 even when config is enabled
    boolean shouldHit = game.hitDealer(true);
    assertFalse(shouldHit);
    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testDealerAlwaysStandsOn18Plus() {
    // Set up 18
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(7, Suit.SPADES)); // 7

    assertEquals(18, game.getHandValue(game.getDealerHand()));

    // Dealer should stand on 18 regardless of config
    boolean shouldHit = game.hitDealer(true);
    assertFalse(shouldHit);
    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testComplexSoft17Scenario() {
    // A-2-2-2 = Soft 17
    game.getDealerHandDirect().clear();
    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getDealerHandDirect().add(new Card(2, Suit.SPADES)); // 2
    game.getDealerHandDirect().add(new Card(2, Suit.CLUBS)); // 2
    game.getDealerHandDirect().add(new Card(2, Suit.DIAMONDS)); // 2

    assertEquals(17, game.getHandValue(game.getDealerHand()));
    assertTrue(game.isSoftSeventeen(game.getDealerHand()));
  }
}
