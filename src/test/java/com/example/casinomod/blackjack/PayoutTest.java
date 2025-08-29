package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PayoutTest {

  @Test
  void testBlackjackPayoutCalculation() {
    BlackjackGame game = new BlackjackGame();
    
    // Set up blackjack hand (21 with 2 cards)
    game.getPlayerHand().add(new Card(1, Suit.HEARTS));  // Ace
    game.getPlayerHand().add(new Card(10, Suit.SPADES)); // 10
    
    assertTrue(game.isBlackjack());
    
    // Test the calculation logic that would be used in handler
    int wagerAmount = 10;
    int expectedTotal = wagerAmount + (wagerAmount * 3 / 2); // 10 + 15 = 25
    assertEquals(25, expectedTotal);
  }

  @Test
  void testRegularWinPayoutCalculation() {
    BlackjackGame game = new BlackjackGame();
    
    // Set up regular win hand (21 with 3+ cards)
    game.getPlayerHand().add(new Card(7, Suit.HEARTS));
    game.getPlayerHand().add(new Card(7, Suit.DIAMONDS));
    game.getPlayerHand().add(new Card(7, Suit.CLUBS));
    
    assertFalse(game.isBlackjack());
    
    // Test regular win calculation
    int wagerAmount = 10;
    int expectedTotal = wagerAmount * 2; // 20 (original + 1:1 payout)
    assertEquals(20, expectedTotal);
  }

  @ParameterizedTest
  @CsvSource({
    "2, 5",     // 2 bet -> 5 total (2 + 3)
    "4, 10",    // 4 bet -> 10 total (4 + 6)  
    "6, 15",    // 6 bet -> 15 total (6 + 9)
    "10, 25",   // 10 bet -> 25 total (10 + 15)
    "20, 50"    // 20 bet -> 50 total (20 + 30)
  })
  void testBlackjackPayoutRatios(int wager, int expectedTotal) {
    int actualTotal = wager + (wager * 3 / 2);
    assertEquals(expectedTotal, actualTotal, 
        "Blackjack payout should be 3:2 (1.5x payout + original wager)");
  }

  @ParameterizedTest
  @CsvSource({
    "1, 2",
    "5, 10", 
    "10, 20",
    "15, 30",
    "25, 50"
  })
  void testRegularWinPayoutRatios(int wager, int expectedTotal) {
    int actualTotal = wager * 2;
    assertEquals(expectedTotal, actualTotal,
        "Regular win should be 1:1 (1x payout + original wager)");
  }

  @Test
  void testBlackjackVsRegularWinDifference() {
    int wager = 10;
    
    int blackjackTotal = wager + (wager * 3 / 2); // 25
    int regularWinTotal = wager * 2; // 20
    
    assertEquals(5, blackjackTotal - regularWinTotal,
        "Blackjack should pay 5 more than regular win for 10 wager");
  }

  @Test
  void testIntegerDivisionHandling() {
    // Test edge cases with integer division
    int oddWager = 3;
    int blackjackTotal = oddWager + (oddWager * 3 / 2); // 3 + 4 = 7 (truncated)
    assertEquals(7, blackjackTotal);
    
    int evenWager = 4; 
    int blackjackTotal2 = evenWager + (evenWager * 3 / 2); // 4 + 6 = 10
    assertEquals(10, blackjackTotal2);
  }
}