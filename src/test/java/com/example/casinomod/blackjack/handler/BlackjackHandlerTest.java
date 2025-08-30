package com.example.casinomod.blackjack.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.casinomod.blackjack.BlackjackGame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlackjackHandlerTest {

  @Test
  void testBlackjackGameIntegration() {
    BlackjackGame game = new BlackjackGame();

    game.startGame();
    assertEquals(BlackjackGame.GamePhase.PLAYER_TURN, game.getPhase());

    game.hitPlayer();
    assertTrue(game.getPlayerHand().size() > 0);

    game.stand();
    assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());
  }

  @Test
  void testDealerLogicIntegration() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();
    game.setPhase(BlackjackGame.GamePhase.DEALER_TURN);

    game.getDealerHandDirect().add(game.draw());
    game.getDealerHandDirect().add(game.draw());

    int initialHandSize = game.getDealerHand().size();
    boolean dealerContinues = game.hitDealer(false);

    assertTrue(game.getDealerHand().size() >= initialHandSize);

    if (dealerContinues) {
      assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());
    } else {
      assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
    }
  }

  @Test
  void testGameFlow() {
    BlackjackGame game = new BlackjackGame();

    assertEquals(BlackjackGame.GamePhase.WAITING, game.getPhase());

    game.startGame();
    assertEquals(BlackjackGame.GamePhase.PLAYER_TURN, game.getPhase());

    game.hitPlayer();
    game.hitPlayer();

    game.stand();
    assertEquals(BlackjackGame.GamePhase.DEALER_TURN, game.getPhase());

    while (game.getPhase() == BlackjackGame.GamePhase.DEALER_TURN) {
      boolean dealerContinues = game.hitDealer(false);
      if (!dealerContinues) {
        break;
      }
    }

    assertEquals(BlackjackGame.GamePhase.FINISHED, game.getPhase());
    assertNotNull(game.determineResult());
  }

  @Test
  void testBlackjackScenario() {
    BlackjackGame game = new BlackjackGame();
    game.startGame();

    game.getPlayerHandDirect().clear();
    game.getPlayerHandDirect()
        .add(
            new com.example.casinomod.blackjack.Card(
                1, com.example.casinomod.blackjack.Suit.HEARTS));
    game.getPlayerHandDirect()
        .add(
            new com.example.casinomod.blackjack.Card(
                10, com.example.casinomod.blackjack.Suit.SPADES));

    assertTrue(game.isBlackjack());

    game.setPhase(BlackjackGame.GamePhase.FINISHED);
    game.getDealerHandDirect().clear();
    game.getDealerHand()
        .add(
            new com.example.casinomod.blackjack.Card(
                10, com.example.casinomod.blackjack.Suit.CLUBS));
    game.getDealerHand()
        .add(
            new com.example.casinomod.blackjack.Card(
                9, com.example.casinomod.blackjack.Suit.DIAMONDS));

    assertEquals(BlackjackGame.Result.WIN, game.determineResult());
  }
}
