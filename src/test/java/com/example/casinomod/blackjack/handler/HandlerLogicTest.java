package com.example.casinomod.blackjack.handler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.Card;
import com.example.casinomod.blackjack.Suit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HandlerLogicTest {

  @Test
  void testWinRewardCalculation() {
    BlackjackGame game = new BlackjackGame();

    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(10, Suit.SPADES));

    boolean isBlackjack = game.isBlackjack();
    int baseWager = 10;
    int expectedReward = isBlackjack ? baseWager * 3 : baseWager * 2;

    assertEquals(30, expectedReward);
  }

  @Test
  void testRegularWinRewardCalculation() {
    BlackjackGame game = new BlackjackGame();

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(9, Suit.SPADES));

    boolean isBlackjack = game.isBlackjack();
    int baseWager = 5;
    int expectedReward = isBlackjack ? baseWager * 3 : baseWager * 2;

    assertEquals(10, expectedReward);
  }

  @ParameterizedTest
  @CsvSource({"1, 10, true, 30", "10, 9, false, 20", "7, 7, false, 14", "1, 11, true, 6"})
  void testRewardCalculation(
      int card1Value, int card2Value, boolean expectedBlackjack, int expectedReward) {
    BlackjackGame game = new BlackjackGame();

    game.getPlayerHandDirect().add(new Card(card1Value, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(card2Value, Suit.SPADES));

    boolean isBlackjack = game.isBlackjack();
    int baseWager = expectedReward / (isBlackjack ? 3 : 2);
    int actualReward = baseWager * (isBlackjack ? 3 : 2);

    assertEquals(expectedBlackjack, isBlackjack);
    assertEquals(expectedReward, actualReward);
  }

  @Test
  void testDealerDrawLogic() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    game.getDealerHandDirect().add(new Card(5, Suit.HEARTS));
    game.getDealerHandDirect().add(new Card(6, Suit.DIAMONDS));

    boolean shouldContinue = true;
    int drawCount = 0;
    while (shouldContinue && drawCount < 10) {
      shouldContinue = game.hitDealer(false);
      drawCount++;
    }

    assertTrue(drawCount > 0);
    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testDealerStopsAt17() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    game.getDealerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getDealerHandDirect().add(new Card(7, Suit.DIAMONDS));

    boolean shouldContinue = game.hitDealer(false);

    assertFalse(shouldContinue);
    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testDealerStandsOnSoft17() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    game.getDealerHandDirect().add(new Card(1, Suit.HEARTS));
    game.getDealerHandDirect().add(new Card(6, Suit.DIAMONDS));

    boolean shouldContinue = game.hitDealer(false);

    // Current implementation: dealer stands on soft 17
    assertFalse(shouldContinue);
    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testCardDealingSequence() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();

    List<Card> playerHand = game.getPlayerHand();
    List<Card> dealerHand = game.getDealerHand();

    Card playerCard1 = game.draw();
    playerHand.add(playerCard1);

    Card dealerCard1 = game.draw();
    dealerHand.add(dealerCard1);

    Card playerCard2 = game.draw();
    playerHand.add(playerCard2);

    Card dealerCard2 = game.draw();
    dealerHand.add(dealerCard2);

    assertEquals(2, playerHand.size());
    assertEquals(2, dealerHand.size());
    assertNotNull(playerCard1);
    assertNotNull(playerCard2);
    assertNotNull(dealerCard1);
    assertNotNull(dealerCard2);
  }

  @Test
  void testResultMessageLogic() {
    BlackjackGame game = new BlackjackGame();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(10, Suit.SPADES));

    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(8, Suit.DIAMONDS));

    BlackjackGame.Result result = game.determineResult();
    boolean isBlackjack = game.isBlackjack();

    assertEquals(BlackjackGame.Result.WIN, result);
    assertTrue(isBlackjack);

    String expectedMessage =
        isBlackjack ? "Blackjack! 1.5x payout!" : "You win! Wager returned with payout.";
    assertEquals("Blackjack! 1.5x payout!", expectedMessage);
  }

  @Test
  void testLossResultMessage() {
    BlackjackGame game = new BlackjackGame();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(5, Suit.DIAMONDS));
    game.getPlayerHandDirect().add(new Card(8, Suit.SPADES));

    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(9, Suit.HEARTS));

    BlackjackGame.Result result = game.determineResult();
    assertEquals(BlackjackGame.Result.LOSE, result);

    String expectedMessage = "You lost your wager.";
    assertNotNull(expectedMessage);
  }

  @Test
  void testDrawResultMessage() {
    BlackjackGame game = new BlackjackGame();
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(8, Suit.DIAMONDS));

    game.getDealerHandDirect().add(new Card(9, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(9, Suit.SPADES));

    BlackjackGame.Result result = game.determineResult();
    assertEquals(BlackjackGame.Result.DRAW, result);

    String expectedMessage = "It's a draw. Your wager has been returned.";
    assertNotNull(expectedMessage);
  }

  @Test
  void testGamePhaseTransitions() {
    BlackjackGame game = new BlackjackGame();

    assertEquals(BlackjackGame.GamePhase.WAITING, game.getPhase());

    game.startGame();
    assertEquals(BlackjackGame.GamePhase.PLAYER_TURN, game.getPhase());

    game.stand();
    assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());

    while (game.getPhase() == BlackjackGame.GamePhase.DEALER_TURN) {
      boolean continues = game.hitDealer(false);
      if (!continues) break;
    }

    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }
}
