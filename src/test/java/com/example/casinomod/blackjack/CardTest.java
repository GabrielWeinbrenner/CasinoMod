package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CardTest {

  @Test
  void testCardConstructor() {
    Card card = new Card(10, Suit.HEARTS);
    assertEquals(10, card.getValue());
    assertEquals(Suit.HEARTS, card.getSuit());
  }

  @ParameterizedTest
  @CsvSource({
    "1, HEARTS, hearts_ace",
    "2, DIAMONDS, diamonds_2",
    "10, CLUBS, clubs_10",
    "11, SPADES, spades_j",
    "12, HEARTS, hearts_q",
    "13, DIAMONDS, diamonds_k"
  })
  void testGetCardName(int value, Suit suit, String expectedName) {
    Card card = new Card(value, suit);
    assertEquals(expectedName, card.getCardName());
  }

  @Test
  void testToString() {
    Card card = new Card(1, Suit.HEARTS);
    assertEquals("hearts_ace", card.toString());
  }

  @ParameterizedTest
  @CsvSource({
    "hearts_ace, 1, HEARTS",
    "diamonds_2, 2, DIAMONDS",
    "clubs_10, 10, CLUBS",
    "spades_j, 11, SPADES",
    "hearts_q, 12, HEARTS",
    "diamonds_k, 13, DIAMONDS"
  })
  void testFromName(String cardName, int expectedValue, Suit expectedSuit) {
    Card card = Card.fromName(cardName);
    assertEquals(expectedValue, card.getValue());
    assertEquals(expectedSuit, card.getSuit());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "hearts", "hearts_ace_extra", ""})
  void testFromNameInvalidInput(String invalidName) {
    assertThrows(IllegalArgumentException.class, () -> Card.fromName(invalidName));
  }

  @ParameterizedTest
  @CsvSource({"1, 1", "2, 2", "9, 9", "10, 10", "11, 10", "12, 10", "13, 10"})
  void testGetBlackjackValue(int cardValue, int expectedBlackjackValue) {
    Card card = new Card(cardValue, Suit.HEARTS);
    assertEquals(expectedBlackjackValue, card.getBlackjackValue());
  }

  @ParameterizedTest
  @ValueSource(ints = {1})
  void testIsAce_True(int value) {
    Card card = new Card(value, Suit.HEARTS);
    assertTrue(card.isAce());
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13})
  void testIsAce_False(int value) {
    Card card = new Card(value, Suit.HEARTS);
    assertFalse(card.isAce());
  }

  @Test
  void testCardNameRoundTrip() {
    Card original = new Card(7, Suit.SPADES);
    String cardName = original.getCardName();
    Card reconstructed = Card.fromName(cardName);

    assertEquals(original.getValue(), reconstructed.getValue());
    assertEquals(original.getSuit(), reconstructed.getSuit());
  }
}
