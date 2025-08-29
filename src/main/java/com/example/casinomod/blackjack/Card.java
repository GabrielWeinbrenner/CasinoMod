package com.example.casinomod.blackjack;

public class Card {
  private final int value;
  private final Suit suit;

  public Card(int value, Suit suit) {
    if (value < 1 || value > 13) {
      throw new IllegalArgumentException("Invalid card value: " + value + ". Must be between 1 and 13.");
    }
    if (suit == null) {
      throw new IllegalArgumentException("Suit cannot be null");
    }
    this.value = value;
    this.suit = suit;
  }

  public int getValue() {
    return value;
  }

  public Suit getSuit() {
    return suit;
  }

  public String getCardName() {
    String name =
        switch (value) {
          case 1 -> "ace";
          case 11 -> "j";
          case 12 -> "q";
          case 13 -> "k";
          default -> String.valueOf(value);
        };
    return suit.name().toLowerCase() + "_" + name;
  }

  @Override
  public String toString() {
    return getCardName();
  }

  public static Card fromName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Card name cannot be null or empty");
    }

    String[] parts = name.split("_");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid card name format: " + name + ". Expected format: suit_rank");
    }

    String suitStr = parts[0];
    String rankStr = parts[1];

    if (suitStr.trim().isEmpty() || rankStr.trim().isEmpty()) {
      throw new IllegalArgumentException("Invalid card name: " + name + ". Suit and rank cannot be empty");
    }

    Suit suit;
    try {
      suit = Suit.valueOf(suitStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid suit: " + suitStr + ". Valid suits: hearts, diamonds, clubs, spades");
    }

    int value;
    try {
      value =
          switch (rankStr.toLowerCase()) {
            case "ace" -> 1;
            case "j" -> 11;
            case "q" -> 12;
            case "k" -> 13;
            default -> {
              int parsedValue = Integer.parseInt(rankStr);
              if (parsedValue < 2 || parsedValue > 10) {
                throw new IllegalArgumentException("Invalid numeric rank: " + rankStr + ". Must be 2-10");
              }
              yield parsedValue;
            }
          };
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid rank: " + rankStr + ". Must be ace, j, q, k, or 2-10");
    }

    return new Card(value, suit);
  }

  public int getBlackjackValue() {
    return (value >= 10) ? 10 : value;
  }

  public boolean isAce() {
    return value == 1;
  }
}
