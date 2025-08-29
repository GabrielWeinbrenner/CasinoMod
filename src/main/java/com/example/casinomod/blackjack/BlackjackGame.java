package com.example.casinomod.blackjack;

import java.util.*;

import javax.annotation.Nullable;

import com.example.casinomod.CasinoMod;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

public class BlackjackGame implements ValueIOSerializable {

  // ────────────────────── Enums ──────────────────────

  public enum GamePhase {
    WAITING,
    PLAYER_TURN,
    DEALER_TURN,
    FINISHED
  }

  public enum Result {
    WIN,
    LOSE,
    DRAW,
    UNFINISHED
  }

  // ────────────────────── Fields ──────────────────────

  private final List<Card> deck = new ArrayList<>();
  private final List<Card> playerHand = new ArrayList<>();
  private final List<Card> dealerHand = new ArrayList<>();
  private final Random random = new Random();
  private GamePhase phase = GamePhase.WAITING;

  // ─────────────── Serialization ───────────────

  @Override
  public void serialize(ValueOutput output) {
    output.putString("phase", phase.name());

    ValueOutput.ValueOutputList playerList = output.childrenList("playerHand");
    for (Card card : playerHand) {
      playerList.addChild().putString("card", card.getCardName());
    }

    ValueOutput.ValueOutputList dealerList = output.childrenList("dealerHand");
    for (Card card : dealerHand) {
      dealerList.addChild().putString("card", card.getCardName());
    }
  }

  @Override
  public void deserialize(ValueInput input) {
    input
        .getString("phase")
        .ifPresent(
            s -> {
              try {
                this.phase = GamePhase.valueOf(s);
              } catch (IllegalArgumentException ignored) {
              }
            });

    playerHand.clear();
    input
        .childrenList("playerHand")
        .ifPresent(
            list ->
                list.forEach(
                    child ->
                        child
                            .getString("card")
                            .ifPresent(name -> playerHand.add(Card.fromName(name)))));

    dealerHand.clear();
    input
        .childrenList("dealerHand")
        .ifPresent(
            list ->
                list.forEach(
                    child ->
                        child
                            .getString("card")
                            .ifPresent(name -> dealerHand.add(Card.fromName(name)))));
  }

  // ─────────────── Game Lifecycle ───────────────

  public void startGame() {
    CasinoMod.LOGGER.debug("[BlackjackGame] Initializing game (deck only)");
    deck.clear();
    playerHand.clear();
    dealerHand.clear();
    phase = GamePhase.PLAYER_TURN;

    for (int i = 1; i <= 13; i++) {
      for (Suit suit : Suit.values()) {
        deck.add(new Card(i, suit));
      }
    }

    Collections.shuffle(deck, random);
    CasinoMod.LOGGER.debug("[BlackjackGame] Deck shuffled with {} cards", deck.size());
  }

  public void reset() {
    deck.clear();
    playerHand.clear();
    dealerHand.clear();
    phase = GamePhase.WAITING;
  }

  public void stand() {
    if (phase == GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Player stands");
      phase = GamePhase.DEALER_TURN;
    } else {
      CasinoMod.LOGGER.warn("Cannot stand: Phase is {}", phase);
    }
  }

  // ─────────────── Player Actions ───────────────

  public void hitPlayer() {
    if (phase != GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.warn("Cannot hit: Phase is {}", phase);
      return;
    }

    Card drawn = draw();
    playerHand.add(drawn);
    CasinoMod.LOGGER.debug("[BlackjackGame] Player hits and draws {}", drawn);

    if (isBusted(playerHand)) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Player busted!");
      phase = GamePhase.FINISHED;
    }
  }

  // ─────────────── Dealer AI ───────────────

  public boolean hitDealer() {
    if (phase != GamePhase.DEALER_TURN) {
      CasinoMod.LOGGER.warn("[BlackjackGame] Cannot hit dealer: Phase is {}", phase);
      return false;
    }

    int dealerValue = getHandValue(dealerHand);
    if (dealerValue >= 17) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Dealer stands at {}", dealerValue);
      phase = GamePhase.FINISHED;
      return false;
    }

    Card drawn = draw();
    dealerHand.add(drawn);
    CasinoMod.LOGGER.debug("[BlackjackGame] Dealer hits and draws {}", drawn);

    if (getHandValue(dealerHand) >= 17) {
      phase = GamePhase.FINISHED;
      return false;
    }

    return true;
  }

  // ─────────────── Result Evaluation ───────────────

  @Nullable
  public Result getResult() {
    Result result = (phase == GamePhase.FINISHED) ? determineResult() : null;
    CasinoMod.LOGGER.trace(
        "[BlackjackGame] getResult() → Phase: {}, Result: {}",
        phase.name(),
        (result != null) ? result.name() : "null");
    return result;
  }

  public Result determineResult() {
    if (phase != GamePhase.FINISHED) {
      CasinoMod.LOGGER.warn("[BlackjackGame] Result requested before game finished.");
      return Result.UNFINISHED;
    }

    int playerScore = getHandValue(playerHand);
    int dealerScore = getHandValue(dealerHand);

    CasinoMod.LOGGER.debug(
        "[BlackjackGame] Final scores → Player: {}, Dealer: {}", playerScore, dealerScore);

    if (playerScore > 21) return Result.LOSE;
    if (dealerScore > 21) return Result.WIN;
    if (playerScore > dealerScore) return Result.WIN;
    if (playerScore < dealerScore) return Result.LOSE;
    return Result.DRAW;
  }

  public boolean isBlackjack() {
    return playerHand.size() == 2 && getHandValue(playerHand) == 21;
  }

  public boolean isDealerBlackjack() {
    return dealerHand.size() == 2 && getHandValue(dealerHand) == 21;
  }

  // ─────────────── Helpers ───────────────

  public Card draw() {
    if (deck.isEmpty()) {
      CasinoMod.LOGGER.error("[BlackjackGame] Attempted to draw from empty deck! Reshuffling...");
      // Emergency reshuffle - create new deck and shuffle
      for (int i = 1; i <= 13; i++) {
        for (Suit suit : Suit.values()) {
          deck.add(new Card(i, suit));
        }
      }
      Collections.shuffle(deck, random);
      CasinoMod.LOGGER.info("[BlackjackGame] Emergency reshuffled deck with {} cards", deck.size());
    }

    Card card = deck.removeFirst();
    CasinoMod.LOGGER.trace("[BlackjackGame] Drawing card: {}", card);
    return card;
  }

  private boolean isBusted(List<Card> hand) {
    return getHandValue(hand) > 21;
  }

  public int getHandValue(List<Card> hand) {
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

  private String formatHand(List<Card> hand) {
    return hand.stream().map(Card::toString).reduce((a, b) -> a + ", " + b).orElse("(empty)");
  }

  // ─────────────── Accessors ───────────────

  public List<Card> getPlayerHand() {
    return playerHand;
  }

  public List<Card> getDealerHand() {
    return dealerHand;
  }

  public GamePhase getPhase() {
    return phase;
  }

  public void setPhase(GamePhase phase) {
    this.phase = phase;
  }
}
