package com.example.casinomod.blackjack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class AuditLogTest {

  @Test
  void testGameRecordDefaults() {
    GameRecord record = new GameRecord();

    assertEquals(0, record.startEpochMs);
    assertEquals(0, record.endEpochMs);
    assertNull(record.result);
    assertEquals(0, record.betCount);
    assertEquals(0, record.payoutCount);
    assertFalse(record.doubledDown);
    assertFalse(record.split);
    assertEquals(0, record.dealerScore);
    assertNotNull(record.playerScores);
    assertTrue(record.playerScores.isEmpty());
  }

  @Test
  void testGameRecordDataPopulation() {
    GameRecord record = new GameRecord();
    record.startEpochMs = 1693123456789L;
    record.endEpochMs = 1693123556789L;
    record.result = BlackjackGame.Result.WIN;
    record.betCount = 10;
    record.payoutCount = 25;
    record.doubledDown = false;
    record.split = false;
    record.dealerScore = 19;
    record.playerScores.add(20);

    assertEquals(1693123456789L, record.startEpochMs);
    assertEquals(1693123556789L, record.endEpochMs);
    assertEquals(BlackjackGame.Result.WIN, record.result);
    assertEquals(10, record.betCount);
    assertEquals(25, record.payoutCount);
    assertFalse(record.doubledDown);
    assertFalse(record.split);
    assertEquals(19, record.dealerScore);
    assertEquals(List.of(20), record.playerScores);
  }

  @Test
  void testGameRecordWithSplitData() {
    GameRecord record = new GameRecord();
    record.result = BlackjackGame.Result.WIN;
    record.betCount = 20;
    record.payoutCount = 80;
    record.doubledDown = false;
    record.split = true;
    record.dealerScore = 17;
    record.playerScores.add(19);
    record.playerScores.add(21);

    assertTrue(record.split);
    assertEquals(2, record.playerScores.size());
    assertEquals(19, record.playerScores.get(0));
    assertEquals(21, record.playerScores.get(1));
  }

  @Test
  void testGameRecordWithDoubleDownData() {
    GameRecord record = new GameRecord();
    record.result = BlackjackGame.Result.WIN;
    record.betCount = 10;
    record.payoutCount = 40;
    record.doubledDown = true;
    record.split = false;
    record.dealerScore = 18;
    record.playerScores.add(20);

    assertTrue(record.doubledDown);
    assertFalse(record.split);
    assertEquals(40, record.payoutCount);
  }

  @Test
  void testGameRecordAllResults() {
    for (BlackjackGame.Result result : BlackjackGame.Result.values()) {
      GameRecord record = new GameRecord();
      record.result = result;
      assertEquals(result, record.result);
    }
  }

  @Test
  void testGameRecordTimestampValidation() {
    GameRecord record = new GameRecord();
    long startTime = System.currentTimeMillis();
    record.startEpochMs = startTime;
    record.endEpochMs = startTime + 30000; // 30 seconds later

    assertTrue(record.endEpochMs > record.startEpochMs);
    assertEquals(30000, record.endEpochMs - record.startEpochMs);
  }
}
