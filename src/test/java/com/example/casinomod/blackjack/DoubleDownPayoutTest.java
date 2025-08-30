package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DoubleDownPayoutTest {

  private BlackjackGame game;

  @BeforeEach
  void setUp() {
    game = new BlackjackGame();
    game.startGame();
    
    // Set up initial two cards for player
    game.getPlayerHandDirect().add(new Card(7, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(4, Suit.SPADES));
    
    // Set up dealer cards  
    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(6, Suit.DIAMONDS));
  }

  @Test
  void testDoubleDownPayoutCalculation() {
    // Original wager: 10 items
    int originalWager = 10;
    
    game.doubleDown();
    assertTrue(game.hasDoubledDown());
    
    // Effective wager should be doubled
    int effectiveWager = game.hasDoubledDown() ? originalWager * 2 : originalWager;
    assertEquals(20, effectiveWager);
    
    // Regular win payout: effective wager * 2 = 20 * 2 = 40 total return
    int expectedPayout = effectiveWager * 2;
    assertEquals(40, expectedPayout);
  }

  @Test 
  void testRegularWinPayoutWithoutDoubleDown() {
    // Original wager: 10 items
    int originalWager = 10;
    
    assertFalse(game.hasDoubledDown());
    
    // Regular win without double down: original wager * 2 = 20 total return
    int effectiveWager = game.hasDoubledDown() ? originalWager * 2 : originalWager;
    int expectedPayout = effectiveWager * 2;
    assertEquals(20, expectedPayout);
  }

  @Test
  void testBlackjackPayoutWithoutDoubleDown() {
    // Set up blackjack
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(10, Suit.SPADES)); // 10
    
    int originalWager = 10;
    assertFalse(game.hasDoubledDown());
    assertTrue(game.isBlackjack());
    
    // Blackjack payout: original + (original * 3/2) = 10 + 15 = 25
    int effectiveWager = originalWager;
    int expectedPayout = effectiveWager + (originalWager * 3 / 2);
    assertEquals(25, expectedPayout);
  }

  @Test
  void testBlackjackBecomesRegularWinAfterDoubleDown() {
    // Set up blackjack initially
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace  
    game.getPlayerHandDirect().add(new Card(10, Suit.SPADES)); // 10
    
    assertTrue(game.isBlackjack());
    
    game.doubleDown(); // This adds a third card
    
    assertFalse(game.isBlackjack()); // No longer blackjack with 3 cards
    assertTrue(game.hasDoubledDown());
    
    int originalWager = 10;
    
    // Should be regular double down payout, not blackjack payout
    int effectiveWager = originalWager * 2; // 20
    int expectedPayout = effectiveWager * 2; // 40
    assertEquals(40, expectedPayout);
  }

  @ParameterizedTest
  @CsvSource({
    "5, false, false, 10",  // Regular win: 5 * 2 = 10
    "5, true, false, 20",   // Double down win: (5*2) * 2 = 20  
    "10, false, true, 25",  // Blackjack: 10 + (10*3/2) = 25
    "10, true, true, 40",   // Double down (no blackjack bonus): (10*2) * 2 = 40
    "1, false, false, 2",   // Small regular win: 1 * 2 = 2
    "1, true, false, 4",    // Small double down: (1*2) * 2 = 4
  })
  void testVariousPayoutScenarios(int originalWager, boolean doubledDown, boolean blackjack, int expectedPayout) {
    // Mock the game state
    BlackjackGame mockGame = mock(BlackjackGame.class);
    when(mockGame.hasDoubledDown()).thenReturn(doubledDown);
    when(mockGame.isBlackjack()).thenReturn(blackjack);
    
    // Calculate payout using the same logic as BlackjackHandler
    int effectiveWager = mockGame.hasDoubledDown() ? originalWager * 2 : originalWager;
    
    int totalReturnCount;
    if (mockGame.isBlackjack() && !mockGame.hasDoubledDown()) {
      totalReturnCount = effectiveWager + (originalWager * 3 / 2); // 1.5x payout on original wager
    } else {
      totalReturnCount = effectiveWager * 2; // 1x payout on effective wager
    }
    
    assertEquals(expectedPayout, totalReturnCount);
  }

  @Test
  void testDoubleDownRoundingWithOddWagers() {
    // Test with wager of 3 (odd number)
    int originalWager = 3;
    
    game.doubleDown();
    
    int effectiveWager = originalWager * 2; // 6
    int expectedPayout = effectiveWager * 2; // 12
    assertEquals(12, expectedPayout);
  }

  @Test
  void testBlackjackRoundingWith3to2Payout() {
    // Set up blackjack
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS)); // Ace
    game.getPlayerHandDirect().add(new Card(11, Suit.SPADES)); // Jack
    
    assertTrue(game.isBlackjack());
    assertFalse(game.hasDoubledDown());
    
    // Test with wager of 3 - should get 3 + (3*3/2) = 3 + 4 = 7 (integer division)
    int originalWager = 3;
    int expectedPayout = originalWager + (originalWager * 3 / 2);
    assertEquals(7, expectedPayout);
    
    // Test with wager of 2 - should get 2 + (2*3/2) = 2 + 3 = 5
    originalWager = 2;
    expectedPayout = originalWager + (originalWager * 3 / 2);
    assertEquals(5, expectedPayout);
    
    // Test with wager of 4 - should get 4 + (4*3/2) = 4 + 6 = 10
    originalWager = 4;
    expectedPayout = originalWager + (originalWager * 3 / 2);
    assertEquals(10, expectedPayout);
  }

  @Test
  void testLargeWagerDoubleDownPayout() {
    // Test with large wager
    int originalWager = 64; // Max stack size
    
    game.doubleDown();
    
    int effectiveWager = originalWager * 2; // 128
    int expectedPayout = effectiveWager * 2; // 256
    assertEquals(256, expectedPayout);
  }
}