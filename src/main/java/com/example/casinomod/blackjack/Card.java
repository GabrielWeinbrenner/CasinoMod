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
          case 1 -> "A";
          case 11 -> "J";
          case 12 -> "Q";
          case 13 -> "K";
          default -> String.valueOf(value);
        };
    return name + "_" + suit.name().toLowerCase();
  }

  public int getBlackjackValue() {
    return (value >= 10) ? 10 : value;
  }

  public boolean isAce() {
    return value == 1;
  }
}
