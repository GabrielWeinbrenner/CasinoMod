package com.example.casinomod.blackjack;

import java.util.*;

import javax.annotation.Nullable;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.Config;

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
  private boolean doubledDown = false;

  // ─────────────── Serialization ───────────────

  @Override
  public void serialize(ValueOutput output) {
    output.putString("phase", phase.name());
    output.putString("doubledDown", String.valueOf(doubledDown));

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

    input.getString("doubledDown").ifPresent(s -> this.doubledDown = Boolean.parseBoolean(s));

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
    doubledDown = false;
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

  public void doubleDown() {
    if (phase != GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.warn("Cannot double down: Phase is {}", phase);
      return;
    }

    if (!canDoubleDown()) {
      CasinoMod.LOGGER.warn("Cannot double down: Player has {} cards", playerHand.size());
      return;
    }

    doubledDown = true;
    Card drawn = draw();
    playerHand.add(drawn);
    CasinoMod.LOGGER.debug("[BlackjackGame] Player doubles down and draws {}", drawn);

    // Player's turn ends immediately after double down
    if (isBusted(playerHand)) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Player busted after double down!");
      phase = GamePhase.FINISHED;
    } else {
      phase = GamePhase.DEALER_TURN;
    }
  }

  public boolean canDoubleDown() {
    return phase == GamePhase.PLAYER_TURN && playerHand.size() == 2;
  }

  public boolean hasDoubledDown() {
    return doubledDown;
  }

  // ─────────────── Dealer AI ───────────────

  public boolean hitDealer() {
    return hitDealer(Config.DEALER_HITS_SOFT_17.get());
  }

  /**
   * Dealer AI logic with configurable soft 17 rule. Public for testing.
   * @param dealerHitsSoft17 true if dealer should hit soft 17, false if dealer should stand
   * @return true if dealer took another card, false if dealer stands or game ended
   */
  public boolean hitDealer(boolean dealerHitsSoft17) {
    if (phase != GamePhase.DEALER_TURN) {
      CasinoMod.LOGGER.warn("[BlackjackGame] Cannot hit dealer: Phase is {}", phase);
      return false;
    }

    int dealerValue = getHandValue(dealerHand);
    
    // Check if dealer should stand based on soft 17 rule
    if (dealerValue > 17 || (dealerValue == 17 && shouldDealerStandOn17(dealerHitsSoft17))) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Dealer stands at {} (soft 17 rule: {})", 
          dealerValue, !dealerHitsSoft17);
      phase = GamePhase.FINISHED;
      return false;
    }

    Card drawn = draw();
    dealerHand.add(drawn);
    CasinoMod.LOGGER.debug("[BlackjackGame] Dealer hits and draws {}", drawn);

    // Check again after drawing
    dealerValue = getHandValue(dealerHand);
    if (dealerValue > 17 || (dealerValue == 17 && shouldDealerStandOn17(dealerHitsSoft17))) {
      phase = GamePhase.FINISHED;
      return false;
    }

    return true;
  }

  /**
   * Determines if the dealer should stand on 17 based on the soft 17 configuration.
   * @return true if dealer should stand, false if dealer should hit
   */
  private boolean shouldDealerStandOn17() {
    return shouldDealerStandOn17(Config.DEALER_HITS_SOFT_17.get());
  }

  /**
   * Determines if the dealer should stand on 17 based on the provided soft 17 configuration.
   * This method is package-private for testing purposes.
   * @param dealerHitsSoft17 true if dealer should hit soft 17, false if dealer should stand
   * @return true if dealer should stand, false if dealer should hit
   */
  boolean shouldDealerStandOn17(boolean dealerHitsSoft17) {
    // If dealer hits soft 17 is enabled, only stand on hard 17
    if (dealerHitsSoft17) {
      return !isSoftSeventeen(dealerHand);
    } else {
      // Dealer stands on all 17s (soft and hard)
      return true;
    }
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

  /**
   * Checks if a hand is a "soft 17" - total of 17 with at least one Ace counted as 11.
   * Examples: A-6, A-2-4, A-A-5, etc.
   */
  public boolean isSoftSeventeen(List<Card> hand) {
    if (getHandValue(hand) != 17) {
      return false;
    }
    
    // Count aces and calculate if any ace is being counted as 11
    int aceCount = 0;
    int totalWithoutAces = 0;
    
    for (Card card : hand) {
      if (card.isAce()) {
        aceCount++;
      } else {
        totalWithoutAces += card.getBlackjackValue();
      }
    }
    
    // If we have aces and the total is 17, check if any ace is counted as 11
    if (aceCount > 0) {
      // Try counting all aces as 1 first
      int minTotal = totalWithoutAces + aceCount;
      
      // If we can add 10 to make exactly 17, then we have a soft 17
      return minTotal + 10 == 17;
    }
    
    return false;
  }

  private String formatHand(List<Card> hand) {
    return hand.stream().map(Card::toString).reduce((a, b) -> a + ", " + b).orElse("(empty)");
  }

  // ─────────────── Accessors ───────────────

  public List<Card> getPlayerHand() {
    return new ArrayList<>(playerHand);
  }

  public List<Card> getDealerHand() {
    return new ArrayList<>(dealerHand);
  }

  public GamePhase getPhase() {
    return phase;
  }

  public void setPhase(GamePhase phase) {
    this.phase = phase;
  }

  // ─────────────── Testing Helper Methods ───────────────
  // These methods provide direct access for testing purposes only
  // They should NOT be used in production code

  /**
   * Gets the actual player hand list for testing purposes.
   * WARNING: This breaks encapsulation and should only be used in tests!
   */
  public List<Card> getPlayerHandDirect() {
    return playerHand;
  }

  /**
   * Gets the actual dealer hand list for testing purposes.
   * WARNING: This breaks encapsulation and should only be used in tests!
   */
  public List<Card> getDealerHandDirect() {
    return dealerHand;
  }
}
