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
  private final List<List<Card>> playerHands = new ArrayList<>();
  private final List<Card> dealerHand = new ArrayList<>();
  private final Random random = new Random();
  private GamePhase phase = GamePhase.WAITING;
  private boolean doubledDown = false;
  private boolean hasSplit = false;
  private int currentHandIndex = 0;

  // ─────────────── Serialization ───────────────

  @Override
  public void serialize(ValueOutput output) {
    output.putString("phase", phase.name());
    output.putString("doubledDown", String.valueOf(doubledDown));
    output.putString("hasSplit", String.valueOf(hasSplit));
    output.putString("currentHandIndex", String.valueOf(currentHandIndex));

    ValueOutput.ValueOutputList handsListOutput = output.childrenList("playerHands");
    for (List<Card> hand : playerHands) {
      ValueOutput handOutput = handsListOutput.addChild();
      ValueOutput.ValueOutputList cardsList = handOutput.childrenList("cards");
      for (Card card : hand) {
        cardsList.addChild().putString("card", card.getCardName());
      }
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
    input.getString("hasSplit").ifPresent(s -> this.hasSplit = Boolean.parseBoolean(s));
    input
        .getString("currentHandIndex")
        .ifPresent(
            s -> {
              try {
                this.currentHandIndex = Integer.parseInt(s);
              } catch (NumberFormatException ignored) {
              }
            });

    playerHands.clear();
    input
        .childrenList("playerHands")
        .ifPresent(
            handsList ->
                handsList.forEach(
                    handInput -> {
                      List<Card> hand = new ArrayList<>();
                      handInput
                          .childrenList("cards")
                          .ifPresent(
                              cardsList ->
                                  cardsList.forEach(
                                      cardInput ->
                                          cardInput
                                              .getString("card")
                                              .ifPresent(name -> hand.add(Card.fromName(name)))));
                      playerHands.add(hand);
                    }));

    // Backward compatibility: check for old single hand format
    if (playerHands.isEmpty()) {
      input
          .childrenList("playerHand")
          .ifPresent(
              list -> {
                List<Card> hand = new ArrayList<>();
                list.forEach(
                    child ->
                        child.getString("card").ifPresent(name -> hand.add(Card.fromName(name))));
                if (!hand.isEmpty()) {
                  playerHands.add(hand);
                }
              });
    }

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
    playerHands.clear();
    playerHands.add(new ArrayList<>()); // Initialize first hand
    dealerHand.clear();
    phase = GamePhase.PLAYER_TURN;
    doubledDown = false;
    hasSplit = false;
    currentHandIndex = 0;

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
    playerHands.clear();
    dealerHand.clear();
    phase = GamePhase.WAITING;
    doubledDown = false;
    hasSplit = false;
    currentHandIndex = 0;
  }

  public void stand() {
    if (phase == GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Player stands on hand {}", currentHandIndex);

      if (hasSplit && currentHandIndex < playerHands.size() - 1) {
        // Move to next hand
        currentHandIndex++;
        CasinoMod.LOGGER.debug("[BlackjackGame] Moving to hand {}", currentHandIndex);
      } else {
        // All hands completed, move to dealer
        phase = GamePhase.DEALER_TURN;
      }
    } else {
      CasinoMod.LOGGER.warn("Cannot stand: Phase is {}", phase);
    }
  }

  // ─────────────── Split Pairs ───────────────

  public boolean canSplit() {
    if (phase != GamePhase.PLAYER_TURN) return false;
    if (hasSplit) return false; // Can only split once per game
    if (playerHands.size() != 1) return false; // Must be first hand

    List<Card> firstHand = playerHands.get(0);
    if (firstHand.size() != 2) return false; // Must have exactly 2 cards

    // Check if both cards have the same rank
    return firstHand.get(0).getValue() == firstHand.get(1).getValue();
  }

  public void splitPairs() {
    if (!canSplit()) {
      CasinoMod.LOGGER.warn("Cannot split: Invalid conditions");
      return;
    }

    List<Card> originalHand = playerHands.get(0);
    Card firstCard = originalHand.get(0);
    Card secondCard = originalHand.get(1);

    CasinoMod.LOGGER.debug("[BlackjackGame] Splitting pair of {}", firstCard.getValue());

    // Create two new hands
    List<Card> hand1 = new ArrayList<>();
    hand1.add(firstCard);
    List<Card> hand2 = new ArrayList<>();
    hand2.add(secondCard);

    // Replace the original hand with the two split hands
    playerHands.clear();
    playerHands.add(hand1);
    playerHands.add(hand2);

    // Deal one card to each hand
    playerHands.get(0).add(draw());
    playerHands.get(1).add(draw());

    hasSplit = true;
    currentHandIndex = 0;

    CasinoMod.LOGGER.debug(
        "[BlackjackGame] Split complete. Hand 1: {}, Hand 2: {}",
        formatHand(playerHands.get(0)),
        formatHand(playerHands.get(1)));
  }

  public boolean hasSplit() {
    return hasSplit;
  }

  public int getCurrentHandIndex() {
    return currentHandIndex;
  }

  public int getHandCount() {
    return playerHands.size();
  }

  // ─────────────── Player Actions ───────────────

  public void hitPlayer() {
    if (phase != GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.warn("Cannot hit: Phase is {}", phase);
      return;
    }

    if (playerHands.isEmpty() || currentHandIndex >= playerHands.size()) {
      CasinoMod.LOGGER.warn("Cannot hit: Invalid hand index {}", currentHandIndex);
      return;
    }

    List<Card> currentHand = playerHands.get(currentHandIndex);
    Card drawn = draw();
    currentHand.add(drawn);
    CasinoMod.LOGGER.debug(
        "[BlackjackGame] Player hits hand {} and draws {}", currentHandIndex, drawn);

    if (isBusted(currentHand)) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Hand {} busted!", currentHandIndex);

      if (hasSplit && currentHandIndex < playerHands.size() - 1) {
        // Move to next hand
        currentHandIndex++;
        CasinoMod.LOGGER.debug("[BlackjackGame] Moving to hand {} after bust", currentHandIndex);
      } else {
        // All hands completed (or single hand busted)
        phase = GamePhase.FINISHED;
      }
    }
  }

  public void doubleDown() {
    if (phase != GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.warn("Cannot double down: Phase is {}", phase);
      return;
    }

    if (!canDoubleDown()) {
      List<Card> currentHand = getCurrentHand();
      CasinoMod.LOGGER.warn(
          "Cannot double down: Hand {} has {} cards",
          currentHandIndex,
          currentHand != null ? currentHand.size() : 0);
      return;
    }

    if (playerHands.isEmpty() || currentHandIndex >= playerHands.size()) {
      CasinoMod.LOGGER.warn("Cannot double down: Invalid hand index {}", currentHandIndex);
      return;
    }

    List<Card> currentHand = playerHands.get(currentHandIndex);
    doubledDown = true;
    Card drawn = draw();
    currentHand.add(drawn);
    CasinoMod.LOGGER.debug(
        "[BlackjackGame] Player doubles down on hand {} and draws {}", currentHandIndex, drawn);

    // Player's turn ends immediately after double down
    if (isBusted(currentHand)) {
      CasinoMod.LOGGER.debug("[BlackjackGame] Hand {} busted after double down!", currentHandIndex);

      if (hasSplit && currentHandIndex < playerHands.size() - 1) {
        // Move to next hand
        currentHandIndex++;
        CasinoMod.LOGGER.debug(
            "[BlackjackGame] Moving to hand {} after double down bust", currentHandIndex);
      } else {
        // All hands completed
        phase = GamePhase.FINISHED;
      }
    } else {
      if (hasSplit && currentHandIndex < playerHands.size() - 1) {
        // Move to next hand
        currentHandIndex++;
        CasinoMod.LOGGER.debug(
            "[BlackjackGame] Moving to hand {} after double down", currentHandIndex);
      } else {
        // All hands completed, move to dealer
        phase = GamePhase.DEALER_TURN;
      }
    }
  }

  public boolean canDoubleDown() {
    if (phase != GamePhase.PLAYER_TURN) return false;
    List<Card> currentHand = getCurrentHand();
    return currentHand != null && currentHand.size() == 2;
  }

  public List<Card> getCurrentHand() {
    if (playerHands.isEmpty() || currentHandIndex >= playerHands.size()) {
      return null;
    }
    return playerHands.get(currentHandIndex);
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
   *
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
      CasinoMod.LOGGER.debug(
          "[BlackjackGame] Dealer stands at {} (soft 17 rule: {})", dealerValue, !dealerHitsSoft17);
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
   *
   * @return true if dealer should stand, false if dealer should hit
   */
  private boolean shouldDealerStandOn17() {
    return shouldDealerStandOn17(Config.DEALER_HITS_SOFT_17.get());
  }

  /**
   * Determines if the dealer should stand on 17 based on the provided soft 17 configuration. This
   * method is package-private for testing purposes.
   *
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

    // For split hands, this method returns the overall result
    // Individual hand results should be checked with determineResult(handIndex)
    if (hasSplit) {
      boolean hasWin = false;
      boolean hasLose = false;

      for (int i = 0; i < playerHands.size(); i++) {
        Result handResult = determineResult(i);
        if (handResult == Result.WIN) hasWin = true;
        if (handResult == Result.LOSE) hasLose = true;
      }

      if (hasWin && !hasLose) return Result.WIN;
      if (!hasWin && hasLose) return Result.LOSE;
      return Result.DRAW; // Mixed results or all draws
    }

    // Single hand logic
    if (playerHands.isEmpty()) return Result.LOSE;
    List<Card> playerHand = playerHands.get(0);
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

  public Result determineResult(int handIndex) {
    if (phase != GamePhase.FINISHED) {
      return Result.UNFINISHED;
    }

    if (handIndex < 0 || handIndex >= playerHands.size()) {
      return Result.LOSE;
    }

    List<Card> playerHand = playerHands.get(handIndex);
    int playerScore = getHandValue(playerHand);
    int dealerScore = getHandValue(dealerHand);

    if (playerScore > 21) return Result.LOSE;
    if (dealerScore > 21) return Result.WIN;
    if (playerScore > dealerScore) return Result.WIN;
    if (playerScore < dealerScore) return Result.LOSE;
    return Result.DRAW;
  }

  public boolean isBlackjack() {
    // For backward compatibility, check the first hand or current hand
    List<Card> hand = getCurrentHand();
    if (hand == null && !playerHands.isEmpty()) {
      hand = playerHands.get(0);
    }
    return hand != null && hand.size() == 2 && getHandValue(hand) == 21;
  }

  public boolean isBlackjack(int handIndex) {
    if (handIndex < 0 || handIndex >= playerHands.size()) {
      return false;
    }

    // Hands resulting from splits cannot be blackjack (traditional blackjack rule)
    if (hasSplit) {
      return false;
    }

    List<Card> hand = playerHands.get(handIndex);
    return hand.size() == 2 && getHandValue(hand) == 21;
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

  /** Deals one card to the current player's hand, ensuring a hand exists. */
  public void dealToPlayer() {
    List<Card> hand = getCurrentHand();
    if (hand == null) {
      if (playerHands.isEmpty()) {
        playerHands.add(new ArrayList<>());
      }
      hand = playerHands.get(0);
    }
    hand.add(draw());
  }

  /** Deals one card to the dealer's hand. */
  public void dealToDealer() {
    dealerHand.add(draw());
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
   * Checks if a hand is a "soft 17" - total of 17 with at least one Ace counted as 11. Examples:
   * A-6, A-2-4, A-A-5, etc.
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
    // For backward compatibility, return the current hand or first hand if available
    if (playerHands.isEmpty()) {
      return new ArrayList<>(); // Empty hand if no hands exist
    }

    if (hasSplit) {
      // In split scenarios, return the current hand
      List<Card> currentHand = getCurrentHand();
      return currentHand != null
          ? new ArrayList<>(currentHand)
          : new ArrayList<>(playerHands.get(0));
    } else {
      // In single-hand scenarios, return the first hand
      return new ArrayList<>(playerHands.get(0));
    }
  }

  public List<Card> getPlayerHand(int handIndex) {
    if (handIndex < 0 || handIndex >= playerHands.size()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(playerHands.get(handIndex));
  }

  public List<List<Card>> getAllPlayerHands() {
    List<List<Card>> result = new ArrayList<>();
    for (List<Card> hand : playerHands) {
      result.add(new ArrayList<>(hand));
    }
    return result;
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
   * Gets the actual player hand list for testing purposes. WARNING: This breaks encapsulation and
   * should only be used in tests!
   */
  public List<Card> getPlayerHandDirect() {
    // For backward compatibility, return the first hand (or current hand in split scenarios)
    if (playerHands.isEmpty()) {
      // If no hands exist, create one for backward compatibility
      playerHands.add(new ArrayList<>());
    }

    if (hasSplit) {
      // In split scenarios, return the current hand
      List<Card> currentHand = getCurrentHand();
      return currentHand != null ? currentHand : playerHands.get(0);
    } else {
      // In single-hand scenarios, always return the first hand
      return playerHands.get(0);
    }
  }

  /**
   * Gets the actual player hand list by index for testing purposes. WARNING: This breaks
   * encapsulation and should only be used in tests!
   */
  public List<Card> getPlayerHandDirect(int handIndex) {
    if (handIndex < 0 || handIndex >= playerHands.size()) {
      return new ArrayList<>();
    }
    return playerHands.get(handIndex);
  }

  /**
   * Gets all actual player hands for testing purposes. WARNING: This breaks encapsulation and
   * should only be used in tests!
   */
  public List<List<Card>> getAllPlayerHandsDirect() {
    return playerHands;
  }

  /**
   * Gets the actual dealer hand list for testing purposes. WARNING: This breaks encapsulation and
   * should only be used in tests!
   */
  public List<Card> getDealerHandDirect() {
    return dealerHand;
  }
}
