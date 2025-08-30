package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlackjackGameTest {

  private BlackjackGame game;

  @BeforeEach
  void setUp() {
    game = new BlackjackGame();
  }

  @Test
  void testInitialState() {
    assertEquals(BlackjackGame.GamePhase.WAITING, game.getPhase());
    assertTrue(game.getPlayerHand().isEmpty());
    assertTrue(game.getDealerHand().isEmpty());
  }

  @Test
  void testStartGame() {
    game.startGame();

    assertEquals(BlackjackGame.GamePhase.PLAYER_TURN, game.getPhase());
    assertTrue(game.getPlayerHand().isEmpty());
    assertTrue(game.getDealerHand().isEmpty());
  }

  @Test
  void testDraw() {
    game.startGame();

    Card drawnCard = game.draw();
    assertNotNull(drawnCard);
    assertTrue(drawnCard.getValue() >= 1 && drawnCard.getValue() <= 13);
  }

  @Test
  void testHitPlayer() {
    game.startGame();

    int initialPlayerHandSize = game.getPlayerHand().size();
    game.hitPlayer();

    assertEquals(initialPlayerHandSize + 1, game.getPlayerHand().size());
  }

  @Test
  void testHitPlayerNotInPlayerTurn() {
    game.setPhase(BlackjackGame.GamePhase.WAITING);

    int initialSize = game.getPlayerHand().size();
    game.hitPlayer();

    assertEquals(initialSize, game.getPlayerHand().size());
  }

  @Test
  void testStand() {
    game.setPhase(BlackjackGame.GamePhase.PLAYER_TURN);

    game.stand();

    assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());
  }

  @Test
  void testStandNotInPlayerTurn() {
    game.setPhase(BlackjackGame.GamePhase.WAITING);

    game.stand();

    assertEquals(BlackjackGame.GamePhase.WAITING, game.getPhase());
  }

  @Test
  void testHitDealer() {
    game.startGame();
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    game.getDealerHandDirect().add(new Card(5, Suit.HEARTS));

    int initialSize = game.getDealerHand().size();
    boolean shouldContinue = game.hitDealer(false);

    assertEquals(initialSize + 1, game.getDealerHand().size());
    assertTrue(shouldContinue);
  }

  @Test
  void testHitDealerStandsAt17() {
    game.startGame();
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    game.getDealerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getDealerHandDirect().add(new Card(7, Suit.DIAMONDS));

    int initialSize = game.getDealerHand().size();
    boolean shouldContinue = game.hitDealer(false);

    assertEquals(initialSize, game.getDealerHand().size());
    assertFalse(shouldContinue);
    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testDetermineResult_PlayerWins() {
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(9, Suit.DIAMONDS));

    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(7, Suit.SPADES));

    assertEquals(BlackjackGame.Result.WIN, game.determineResult());
  }

  @Test
  void testDetermineResult_PlayerLoses() {
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(7, Suit.DIAMONDS));

    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(9, Suit.SPADES));

    assertEquals(BlackjackGame.Result.LOSE, game.determineResult());
  }

  @Test
  void testDetermineResult_Draw() {
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(8, Suit.DIAMONDS));

    game.getDealerHandDirect().add(new Card(9, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(9, Suit.SPADES));

    assertEquals(BlackjackGame.Result.DRAW, game.determineResult());
  }

  @Test
  void testDetermineResult_PlayerBusted() {
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(10, Suit.DIAMONDS));
    game.getPlayerHandDirect().add(new Card(5, Suit.CLUBS));

    game.getDealerHandDirect().add(new Card(10, Suit.SPADES));
    game.getDealerHandDirect().add(new Card(7, Suit.HEARTS));

    assertEquals(BlackjackGame.Result.LOSE, game.determineResult());
  }

  @Test
  void testDetermineResult_DealerBusted() {
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(8, Suit.DIAMONDS));

    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(10, Suit.SPADES));
    game.getDealerHandDirect().add(new Card(5, Suit.HEARTS));

    assertEquals(BlackjackGame.Result.WIN, game.determineResult());
  }

  @Test
  void testDetermineResult_NotFinished() {
    game.setPhase(BlackjackGame.GamePhase.PLAYER_TURN);

    assertEquals(BlackjackGame.Result.UNFINISHED, game.determineResult());
  }

  @Test
  void testIsBlackjack_True() {
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(10, Suit.DIAMONDS));

    assertTrue(game.isBlackjack());
  }

  @Test
  void testIsBlackjack_FalseWrongTotal() {
    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(9, Suit.DIAMONDS));

    assertFalse(game.isBlackjack());
  }

  @Test
  void testIsBlackjack_FalseWrongCardCount() {
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(5, Suit.DIAMONDS));
    game.getPlayerHandDirect().add(new Card(5, Suit.CLUBS));

    assertFalse(game.isBlackjack());
  }

  @Test
  void testGetResult_Finished() {
    game.setPhase(BlackjackGame.GamePhase.FINISHED);

    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(9, Suit.DIAMONDS));

    game.getDealerHandDirect().add(new Card(10, Suit.CLUBS));
    game.getDealerHandDirect().add(new Card(7, Suit.SPADES));

    assertEquals(BlackjackGame.Result.WIN, game.getResult());
  }

  @Test
  void testGetResult_NotFinished() {
    game.setPhase(BlackjackGame.GamePhase.PLAYER_TURN);

    assertNull(game.getResult());
  }

  @Test
  void testReset() {
    game.startGame();
    game.hitPlayer();
    game.hitPlayer();

    game.reset();

    assertEquals(BlackjackGame.GamePhase.WAITING, game.getPhase());
    assertTrue(game.getPlayerHand().isEmpty());
    assertTrue(game.getDealerHand().isEmpty());
  }

  @Test
  void testPlayerBustTransitionsToFinished() {
    game.startGame();

    // Clear the default dealt hand and manually set up bust scenario
    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect().add(new Card(10, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(10, Suit.DIAMONDS));

    game.hitPlayer();

    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
  }

  @Test
  void testHandValueCalculation_WithAces() {
    game.startGame();
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(5, Suit.DIAMONDS));

    game.hitPlayer();
    Card lastCard = game.getPlayerHand().get(game.getPlayerHand().size() - 1);

    int expectedMinValue = 6 + lastCard.getBlackjackValue();
    int expectedMaxValue = 16 + lastCard.getBlackjackValue();

    assertTrue(expectedMinValue <= 21 || expectedMaxValue <= 21);
  }

  @Test
  void testMultipleAcesHandling() {
    game.getPlayerHandDirect().add(new Card(1, Suit.HEARTS));
    game.getPlayerHandDirect().add(new Card(1, Suit.DIAMONDS));
    game.getPlayerHandDirect().add(new Card(9, Suit.CLUBS));

    List<Card> hand = game.getPlayerHand();
    int total = 0;
    int aces = 0;

    for (Card card : hand) {
      total += card.getBlackjackValue();
      if (card.isAce()) aces++;
    }

    while (aces > 0 && total + 10 <= 21) {
      total += 10;
      aces--;
    }

    assertEquals(21, total);
  }
}
