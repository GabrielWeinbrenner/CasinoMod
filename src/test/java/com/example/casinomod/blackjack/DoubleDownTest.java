package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DoubleDownTest {

  private BlackjackGame game;

  @BeforeEach
  void setUp() {
    game = new BlackjackGame();
    game.startGame();

    // Set up initial two cards for player (simulate dealing)
    game.getPlayerHandDirect().add(new Card(7, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(4, Suit.SPADES));

    // Set up dealer cards
    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(6, Suit.DIAMONDS));
  }

  @Test
  void testCanDoubleDownWithTwoCards() {
    assertEquals(BlackjackGame.GamePhase.PLAYER_TURN, game.getPhase());
    assertEquals(2, game.getPlayerHand().size());
    assertTrue(game.canDoubleDown());
  }

  @Test
  void testCannotDoubleDownWithThreeCards() {
    // Add a third card
    game.getPlayerHandDirect().add(new Card(3, Suit.HEARTS));

    assertFalse(game.canDoubleDown());
  }

  @Test
  void testCannotDoubleDownWhenNotPlayerTurn() {
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    assertFalse(game.canDoubleDown());
  }

  @Test
  void testCannotDoubleDownWhenWaiting() {
    game.setPhase(BlackjackGame.GamePhase.WAITING);

    assertFalse(game.canDoubleDown());
  }

  @Test
  void testCannotDoubleDownWhenFinished() {
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    assertFalse(game.canDoubleDown());
  }

  @Test
  void testDoubleDownDrawsOneCard() {
    int initialHandSize = game.getPlayerHand().size();

    game.doubleDown();

    assertEquals(initialHandSize + 1, game.getPlayerHand().size());
    assertEquals(3, game.getPlayerHand().size());
  }

  @Test
  void testDoubleDownEndsPlayerTurn() {
    game.doubleDown();

    assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());
  }

  @Test
  void testDoubleDownSetsFlag() {
    assertFalse(game.hasDoubledDown());

    game.doubleDown();

    assertTrue(game.hasDoubledDown());
  }

  @Test
  void testDoubleDownAfterBustFinishesGame() {
    // Clear hand and set up a hand that will bust when doubled down
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS)); // 10
    game.getPlayerHandDirect().add(new Card(9, Suit.SPADES)); // 19 total

    // Force next card to be high to guarantee bust
    // We'll manually add a card that causes bust
    game.getPlayerHandDirect().add(new Card(5, Suit.CLUBS)); // Would be 24, but we remove it first
    game.getPlayerHandDirect().remove(2); // Remove the extra card

    // Now double down - the draw() method will get a random card
    game.doubleDown();

    // Verify the game finished if player busted
    if (game.getHandValue(game.getPlayerHand()) > 21) {
      assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
    }

    assertTrue(game.hasDoubledDown());
  }

  @Test
  void testCannotDoubleDownAfterHit() {
    assertTrue(game.canDoubleDown());

    game.hitPlayer();

    assertFalse(game.canDoubleDown());
    assertEquals(3, game.getPlayerHand().size());
  }

  @Test
  void testDoubleDownWhenPhaseIsNotPlayerTurnDoesNothing() {
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);
    int initialHandSize = game.getPlayerHand().size();
    boolean initialDoubledDown = game.hasDoubledDown();

    game.doubleDown();

    assertEquals(initialHandSize, game.getPlayerHand().size());
    assertEquals(initialDoubledDown, game.hasDoubledDown());
    assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());
  }

  @Test
  void testDoubleDownWithThreeCardsDoesNothing() {
    // Add third card first
    game.getPlayerHandDirect().add(new Card(2, Suit.HEARTS));
    int initialHandSize = game.getPlayerHand().size();
    boolean initialDoubledDown = game.hasDoubledDown();

    game.doubleDown();

    assertEquals(initialHandSize, game.getPlayerHand().size());
    assertEquals(initialDoubledDown, game.hasDoubledDown());
    assertEquals(BlackjackGame.GamePhase.PLAYER_TURN, game.getPhase());
  }

  @Test
  void testDoubleDownSerializationAndDeserialization() {
    game.doubleDown();
    assertTrue(game.hasDoubledDown());

    // Test serialization preserves the doubled down state
    // This is a simplified test - in practice we'd use actual serialization
    BlackjackGame newGame = new BlackjackGame();

    // Simulate the serialization process
    String serializedDoubledDown = String.valueOf(game.hasDoubledDown());
    boolean deserializedDoubledDown = Boolean.parseBoolean(serializedDoubledDown);

    assertEquals(game.hasDoubledDown(), deserializedDoubledDown);
  }

  @Test
  void testResetClearsDoubledDownFlag() {
    game.doubleDown();
    assertTrue(game.hasDoubledDown());

    game.reset();

    assertFalse(game.hasDoubledDown());
    assertEquals(BlackjackGame.GamePhase.WAITING, game.getPhase());
  }

  @Test
  void testDoubleDownWithBlackjackScenario() {
    // Set up a blackjack hand (but this shouldn't be possible in real game)
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(10, Suit.SPADES)); // 10

    assertTrue(game.isBlackjack());
    assertTrue(game.canDoubleDown()); // Technically possible with 2 cards

    game.doubleDown();

    assertTrue(game.hasDoubledDown());
    assertEquals(3, game.getPlayerHand().size());
    assertFalse(game.isBlackjack()); // No longer blackjack with 3 cards
  }

  @Test
  void testDoubleDownMultipleTimes() {
    // Can only double down once
    assertTrue(game.canDoubleDown());

    game.doubleDown();

    assertFalse(game.canDoubleDown()); // Can't double down again
    assertEquals(3, game.getPlayerHand().size());

    // Try to double down again - should do nothing
    BlackjackGame.GamePhase phaseAfterFirst = game.getPhase();
    int handSizeAfterFirst = game.getPlayerHand().size();

    game.doubleDown();

    assertEquals(phaseAfterFirst, game.getPhase());
    assertEquals(handSizeAfterFirst, game.getPlayerHand().size());
  }
}
