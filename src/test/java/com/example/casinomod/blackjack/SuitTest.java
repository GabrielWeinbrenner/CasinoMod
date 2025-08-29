package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SuitTest {

  @Test
  void testSuitValues() {
    assertEquals(4, Suit.values().length);

    assertEquals(Suit.HEARTS, Suit.valueOf("HEARTS"));
    assertEquals(Suit.DIAMONDS, Suit.valueOf("DIAMONDS"));
    assertEquals(Suit.CLUBS, Suit.valueOf("CLUBS"));
    assertEquals(Suit.SPADES, Suit.valueOf("SPADES"));
  }

  @Test
  void testSuitNames() {
    assertEquals("HEARTS", Suit.HEARTS.name());
    assertEquals("DIAMONDS", Suit.DIAMONDS.name());
    assertEquals("CLUBS", Suit.CLUBS.name());
    assertEquals("SPADES", Suit.SPADES.name());
  }

  @Test
  void testInvalidSuit() {
    assertThrows(IllegalArgumentException.class, () -> Suit.valueOf("INVALID"));
  }
}
