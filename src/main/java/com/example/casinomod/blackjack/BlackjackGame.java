package com.example.casinomod.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BlackjackGame {
  public enum GamePhase {
    WAITING,
    PLAYER_TURN,
    DEALER_TURN,
    FINISHED
  }

  private final List<Card> deck = new ArrayList<>();
  private final List<Card> playerHand = new ArrayList<>();
  private final List<Card> dealerHand = new ArrayList<>();
  private GamePhase phase = GamePhase.WAITING;
  private final Random random = new Random();

  public void startGame() {
    deck.clear();
    playerHand.clear();
    dealerHand.clear();
    phase = GamePhase.PLAYER_TURN;

    // Generate and shuffle deck
    for (int i = 1; i <= 13; i++) {
      for (Suit suit : Suit.values()) {
        deck.add(new Card(i, suit));
      }
    }
    Collections.shuffle(deck, random);

    // Deal initial cards
    playerHand.add(draw());
    dealerHand.add(draw());
    playerHand.add(draw());
    dealerHand.add(draw());
  }

  public void hitPlayer() {
    if (phase == GamePhase.PLAYER_TURN) {
      playerHand.add(draw());
      if (isBusted(playerHand)) {
        phase = GamePhase.FINISHED;
      }
    }
  }

  public void stand() {
    if (phase == GamePhase.PLAYER_TURN) {
      phase = GamePhase.DEALER_TURN;

      while (getHandValue(dealerHand) < 17) {
        dealerHand.add(draw());
      }
      phase = GamePhase.FINISHED;
    }
  }

  public Result determineResult() {
    if (phase != GamePhase.FINISHED) return Result.UNFINISHED;

    int playerScore = getHandValue(playerHand);
    int dealerScore = getHandValue(dealerHand);

    if (playerScore > 21) return Result.LOSE;
    if (dealerScore > 21) return Result.WIN;
    if (playerScore > dealerScore) return Result.WIN;
    if (playerScore < dealerScore) return Result.LOSE;
    return Result.DRAW;
  }

  public enum Result {
    WIN,
    LOSE,
    DRAW,
    UNFINISHED
  }

  // ─────────────────────────────────────────────────────────────

  private Card draw() {
    return deck.remove(0);
  }

  private int getHandValue(List<Card> hand) {
    int total = 0;
    int aceCount = 0;

    for (Card card : hand) {
      total += card.getBlackjackValue();
      if (card.isAce()) aceCount++;
    }

    while (aceCount > 0 && total + 10 <= 21) {
      total += 10;
      aceCount--;
    }

    return total;
  }

  private boolean isBusted(List<Card> hand) {
    return getHandValue(hand) > 21;
  }

  // ────────────────────── Accessors ─────────────────────────────

  public List<Card> getPlayerHand() {
    return playerHand;
  }

  public List<Card> getDealerHand() {
    return dealerHand;
  }

  public GamePhase getPhase() {
    return phase;
  }
}
