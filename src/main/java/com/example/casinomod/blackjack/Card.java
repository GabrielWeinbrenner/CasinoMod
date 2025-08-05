package com.example.casinomod.blackjack;

public class Card {
  private final int value;
  private final Suit suit;

  public Card(int value, Suit suit) {
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
    String[] parts = name.split("_");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid card name: " + name);
    }

    String suitStr = parts[0];
    String rankStr = parts[1];

    Suit suit = Suit.valueOf(suitStr.toUpperCase());

    int value =
        switch (rankStr) {
          case "ace" -> 1;
          case "j" -> 11;
          case "q" -> 12;
          case "k" -> 13;
          default -> Integer.parseInt(rankStr);
        };

    return new Card(value, suit);
  }

  public int getBlackjackValue() {
    return (value >= 10) ? 10 : value;
  }

  public boolean isAce() {
    return value == 1;
  }
}
