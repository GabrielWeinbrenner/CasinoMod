package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CardValidationTest {

  @Test
  void testValidCardConstruction() {
    // Test all valid values
    for (int value = 1; value <= 13; value++) {
      for (Suit suit : Suit.values()) {
        Card card = new Card(value, suit);
        assertEquals(value, card.getValue());
        assertEquals(suit, card.getSuit());
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -10, 14, 15, 100})
  void testInvalidCardValues(int invalidValue) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new Card(invalidValue, Suit.HEARTS));
    assertTrue(exception.getMessage().contains("Invalid card value"));
    assertTrue(exception.getMessage().contains("Must be between 1 and 13"));
  }

  @Test
  void testNullSuit() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Card(1, null));
    assertEquals("Suit cannot be null", exception.getMessage());
  }

  @Test
  void testValidFromName() {
    // Test standard numeric cards
    Card card2 = Card.fromName("hearts_2");
    assertEquals(2, card2.getValue());
    assertEquals(Suit.HEARTS, card2.getSuit());

    Card card10 = Card.fromName("spades_10");
    assertEquals(10, card10.getValue());
    assertEquals(Suit.SPADES, card10.getSuit());

    // Test face cards
    Card ace = Card.fromName("clubs_ace");
    assertEquals(1, ace.getValue());
    assertEquals(Suit.CLUBS, ace.getSuit());

    Card jack = Card.fromName("diamonds_j");
    assertEquals(11, jack.getValue());
    assertEquals(Suit.DIAMONDS, jack.getSuit());

    Card queen = Card.fromName("hearts_q");
    assertEquals(12, queen.getValue());
    assertEquals(Suit.HEARTS, queen.getSuit());

    Card king = Card.fromName("spades_k");
    assertEquals(13, king.getValue());
    assertEquals(Suit.SPADES, king.getSuit());
  }

  @Test
  void testCaseInsensitiveFromName() {
    Card card1 = Card.fromName("HEARTS_ACE");
    assertEquals(1, card1.getValue());
    assertEquals(Suit.HEARTS, card1.getSuit());

    Card card2 = Card.fromName("clubs_J");
    assertEquals(11, card2.getValue());
    assertEquals(Suit.CLUBS, card2.getSuit());
  }

  @Test
  void testInvalidFromNameFormat() {
    // Null or empty names
    assertThrows(IllegalArgumentException.class, () -> Card.fromName(null));
    assertThrows(IllegalArgumentException.class, () -> Card.fromName(""));
    assertThrows(IllegalArgumentException.class, () -> Card.fromName("   "));

    // Wrong format
    assertThrows(IllegalArgumentException.class, () -> Card.fromName("hearts"));
    assertThrows(IllegalArgumentException.class, () -> Card.fromName("hearts_ace_extra"));
    assertThrows(IllegalArgumentException.class, () -> Card.fromName("_ace"));
    assertThrows(IllegalArgumentException.class, () -> Card.fromName("hearts_"));
  }

  @Test
  void testInvalidSuitFromName() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> Card.fromName("invalid_ace"));
    assertTrue(exception.getMessage().contains("Invalid suit: invalid"));
    assertTrue(exception.getMessage().contains("Valid suits: hearts, diamonds, clubs, spades"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"hearts_0", "hearts_1", "hearts_14", "hearts_15"})
  void testInvalidNumericRankFromName(String invalidName) {
    assertThrows(IllegalArgumentException.class, () -> Card.fromName(invalidName));
  }

  @Test
  void testInvalidRankFromName() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> Card.fromName("hearts_invalid"));
    assertTrue(exception.getMessage().contains("Invalid rank: invalid"));
    assertTrue(exception.getMessage().contains("Must be ace, j, q, k, or 2-10"));
  }

  @Test
  void testFromNameRoundTrip() {
    // Test that getCardName() and fromName() are consistent
    for (int value = 1; value <= 13; value++) {
      for (Suit suit : Suit.values()) {
        Card original = new Card(value, suit);
        String name = original.getCardName();
        Card reconstructed = Card.fromName(name);

        assertEquals(original.getValue(), reconstructed.getValue());
        assertEquals(original.getSuit(), reconstructed.getSuit());
        assertEquals(original.getCardName(), reconstructed.getCardName());
      }
    }
  }
}