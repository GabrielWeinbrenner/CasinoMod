package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MultiDeckTest {

  private BlackjackGame game;

  @BeforeEach
  void setUp() {
    game = new BlackjackGame();
  }

  @Test
  void testSingleDeckByDefault() {
    game.startGame();
    assertEquals(1, game.getNumberOfDecks());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 4, 6, 8})
  void testMultipleDeckSupport(int deckCount) {
    game.startGame(deckCount);
    assertEquals(deckCount, game.getNumberOfDecks());

    // A full deck has 52 cards, so multiple decks should have multiples of 52
    int expectedTotalCards = 52 * deckCount;
    int actualTotalCards = 0;

    // Count all cards by trying to draw them all
    try {
      while (true) {
        game.draw();
        actualTotalCards++;
      }
    } catch (RuntimeException e) {
      // When deck is empty, draw() will reshuffle and we can draw again
      // But since we reshuffled, we should have the right number of cards
    }

    assertTrue(
        actualTotalCards >= expectedTotalCards,
        "Should have at least " + expectedTotalCards + " cards but only drew " + actualTotalCards);
  }

  @Test
  void testDeckCountBoundaries() {
    // Test minimum boundary
    game.startGame(0);
    assertEquals(1, game.getNumberOfDecks()); // Should clamp to 1

    game.startGame(-5);
    assertEquals(1, game.getNumberOfDecks()); // Should clamp to 1

    // Test maximum boundary
    game.startGame(10);
    assertEquals(8, game.getNumberOfDecks()); // Should clamp to 8

    game.startGame(100);
    assertEquals(8, game.getNumberOfDecks()); // Should clamp to 8
  }

  @Test
  void testSetNumberOfDecks() {
    game.setNumberOfDecks(4);
    assertEquals(4, game.getNumberOfDecks());

    // Test boundaries
    game.setNumberOfDecks(0);
    assertEquals(1, game.getNumberOfDecks());

    game.setNumberOfDecks(12);
    assertEquals(8, game.getNumberOfDecks());
  }

  @Test
  void testReshuffleWithMultipleDecks() {
    game.startGame(3); // Start with 3 decks
    assertEquals(3, game.getNumberOfDecks());

    // Draw all cards to force reshuffle
    int cardsDrawn = 0;
    for (int i = 0; i < 156; i++) { // 3 decks * 52 cards = 156 cards
      game.draw();
      cardsDrawn++;
    }

    // The next draw should trigger reshuffle
    Card cardAfterReshuffle = game.draw();
    assertNotNull(cardAfterReshuffle);

    // After reshuffle, should still have 3 decks worth of cards
    assertEquals(3, game.getNumberOfDecks());
  }

  @Test
  void testMultiDeckGettersSetters() {
    game.setNumberOfDecks(6);
    assertEquals(6, game.getNumberOfDecks());

    // Test that starting with a different count updates the internal state
    game.startGame(3);
    assertEquals(3, game.getNumberOfDecks());
  }
}
