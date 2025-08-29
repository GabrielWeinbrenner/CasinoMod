package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmptyDeckTest {

  @Test
  void testEmptyDeckHandling() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();
    
    // Draw all 52 cards to empty the deck
    for (int i = 0; i < 52; i++) {
      Card card = game.draw();
      assertNotNull(card);
      assertTrue(card.getValue() >= 1 && card.getValue() <= 13);
    }
    
    // The 53rd draw should trigger reshuffle, not crash
    Card extraCard = game.draw();
    assertNotNull(extraCard);
    assertTrue(extraCard.getValue() >= 1 && extraCard.getValue() <= 13);
  }

  @Test
  void testEmptyDeckMultipleDraws() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();
    
    // Draw all cards plus several more
    for (int i = 0; i < 60; i++) {
      Card card = game.draw();
      assertNotNull(card);
    }
    
    // Should still be able to draw without crashes
    Card finalCard = game.draw();
    assertNotNull(finalCard);
  }

  @Test
  void testEmptyDeckAfterReset() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();
    
    // Draw some cards
    for (int i = 0; i < 10; i++) {
      game.draw();
    }
    
    game.reset();
    game.startGame();
    
    // Should be able to draw normally after reset
    Card card = game.draw();
    assertNotNull(card);
    assertTrue(card.getValue() >= 1 && card.getValue() <= 13);
  }
}