package com.example.casinomod.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.GameRecord;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;

class AuditNetworkTest {

  @Test
  void testGameRecordSerialization() {
    GameRecord original = new GameRecord();
    original.startEpochMs = 1693123456789L;
    original.endEpochMs = 1693123556789L;
    original.result = BlackjackGame.Result.WIN;
    original.betCount = 15;
    original.payoutCount = 30;
    original.doubledDown = true;
    original.split = false;
    original.dealerScore = 18;
    original.playerScores.add(20);
    original.playerScores.add(19);

    ByteBuf buf = Unpooled.buffer();
    GameRecord.STREAM_CODEC.encode(buf, original);
    GameRecord decoded = GameRecord.STREAM_CODEC.decode(buf);
    buf.release();

    assertEquals(original.startEpochMs, decoded.startEpochMs);
    assertEquals(original.endEpochMs, decoded.endEpochMs);
    assertEquals(original.result, decoded.result);
    assertEquals(original.betCount, decoded.betCount);
    assertEquals(original.payoutCount, decoded.payoutCount);
    assertEquals(original.doubledDown, decoded.doubledDown);
    assertEquals(original.split, decoded.split);
    assertEquals(original.dealerScore, decoded.dealerScore);
    assertEquals(original.playerScores, decoded.playerScores);
  }

  @Test
  void testGameRecordSerializationWithEmptyPlayerScores() {
    GameRecord original = new GameRecord();
    original.startEpochMs = 1693123456789L;
    original.endEpochMs = 1693123556789L;
    original.result = BlackjackGame.Result.LOSE;
    original.betCount = 10;
    original.payoutCount = 0;
    original.doubledDown = false;
    original.split = false;
    original.dealerScore = 21;
    // No player scores added

    ByteBuf buf = Unpooled.buffer();
    GameRecord.STREAM_CODEC.encode(buf, original);
    GameRecord decoded = GameRecord.STREAM_CODEC.decode(buf);
    buf.release();

    assertEquals(original.playerScores.size(), decoded.playerScores.size());
    assertTrue(decoded.playerScores.isEmpty());
  }

  @Test
  void testAuditRequestPacketSerialization() {
    BlockPos pos = new BlockPos(100, 64, -50);
    AuditRequestPacket original = new AuditRequestPacket(pos, 2, 10);

    ByteBuf buf = Unpooled.buffer();
    AuditRequestPacket.STREAM_CODEC.encode(buf, original);
    AuditRequestPacket decoded = AuditRequestPacket.STREAM_CODEC.decode(buf);
    buf.release();

    assertEquals(original.pos(), decoded.pos());
    assertEquals(original.page(), decoded.page());
    assertEquals(original.pageSize(), decoded.pageSize());
  }

  @Test
  void testAuditPagePacketSerialization() {
    BlockPos pos = new BlockPos(0, 0, 0);

    GameRecord record1 = new GameRecord();
    record1.startEpochMs = 1693123456789L;
    record1.result = BlackjackGame.Result.WIN;
    record1.betCount = 5;
    record1.payoutCount = 10;
    record1.playerScores.add(21);

    GameRecord record2 = new GameRecord();
    record2.startEpochMs = 1693123556789L;
    record2.result = BlackjackGame.Result.LOSE;
    record2.betCount = 8;
    record2.payoutCount = 0;
    record2.playerScores.add(22);

    List<GameRecord> records = List.of(record1, record2);
    AuditPagePacket original = new AuditPagePacket(pos, 0, 10, 25, records);

    ByteBuf buf = Unpooled.buffer();
    AuditPagePacket.STREAM_CODEC.encode(buf, original);
    AuditPagePacket decoded = AuditPagePacket.STREAM_CODEC.decode(buf);
    buf.release();

    assertEquals(original.pos(), decoded.pos());
    assertEquals(original.page(), decoded.page());
    assertEquals(original.pageSize(), decoded.pageSize());
    assertEquals(original.total(), decoded.total());
    assertEquals(original.records().size(), decoded.records().size());

    assertEquals(BlackjackGame.Result.WIN, decoded.records().get(0).result);
    assertEquals(BlackjackGame.Result.LOSE, decoded.records().get(1).result);
  }

  @Test
  void testAuditPageClientHandlerCaching() {
    BlockPos pos1 = new BlockPos(10, 64, 20);
    BlockPos pos2 = new BlockPos(30, 64, 40);

    GameRecord record = new GameRecord();
    record.result = BlackjackGame.Result.DRAW;
    record.betCount = 12;

    AuditPagePacket packet1 = new AuditPagePacket(pos1, 0, 10, 1, List.of(record));
    AuditPagePacket packet2 = new AuditPagePacket(pos2, 1, 10, 5, List.of());

    AuditPageClientHandler.handle(packet1);
    AuditPageClientHandler.handle(packet2);

    var data1 = AuditPageClientHandler.get(pos1);
    var data2 = AuditPageClientHandler.get(pos2);
    var data3 = AuditPageClientHandler.get(new BlockPos(0, 0, 0)); // Non-existent

    assertNotNull(data1);
    assertNotNull(data2);
    assertNull(data3);

    assertEquals(0, data1.page());
    assertEquals(1, data1.total());
    assertEquals(1, data1.records().size());

    assertEquals(1, data2.page());
    assertEquals(5, data2.total());
    assertEquals(0, data2.records().size());
  }

  @Test
  void testGameRecordWithAllResults() {
    for (BlackjackGame.Result result : BlackjackGame.Result.values()) {
      GameRecord record = new GameRecord();
      record.result = result;

      ByteBuf buf = Unpooled.buffer();
      GameRecord.STREAM_CODEC.encode(buf, record);
      GameRecord decoded = GameRecord.STREAM_CODEC.decode(buf);
      buf.release();

      assertEquals(result, decoded.result);
    }
  }

  @Test
  void testGameRecordWithMaxPlayerScores() {
    GameRecord original = new GameRecord();
    original.result = BlackjackGame.Result.WIN;

    // Add many player scores (split scenario)
    for (int i = 1; i <= 10; i++) {
      original.playerScores.add(i * 2);
    }

    ByteBuf buf = Unpooled.buffer();
    GameRecord.STREAM_CODEC.encode(buf, original);
    GameRecord decoded = GameRecord.STREAM_CODEC.decode(buf);
    buf.release();

    assertEquals(10, decoded.playerScores.size());
    for (int i = 0; i < 10; i++) {
      assertEquals((i + 1) * 2, decoded.playerScores.get(i));
    }
  }

  @Test
  void testAuditPagePacketWithEmptyRecords() {
    BlockPos pos = new BlockPos(0, 0, 0);
    AuditPagePacket original = new AuditPagePacket(pos, 5, 10, 100, List.of());

    ByteBuf buf = Unpooled.buffer();
    AuditPagePacket.STREAM_CODEC.encode(buf, original);
    AuditPagePacket decoded = AuditPagePacket.STREAM_CODEC.decode(buf);
    buf.release();

    assertEquals(5, decoded.page());
    assertEquals(100, decoded.total());
    assertTrue(decoded.records().isEmpty());
  }

  @Test
  void testAuditRequestPacketBoundaryValues() {
    // Test with boundary values
    AuditRequestPacket packet1 = new AuditRequestPacket(BlockPos.ZERO, 0, 1);
    AuditRequestPacket packet2 =
        new AuditRequestPacket(
            new BlockPos(Integer.MAX_VALUE, 319, Integer.MAX_VALUE), Integer.MAX_VALUE, 50);

    ByteBuf buf1 = Unpooled.buffer();
    AuditRequestPacket.STREAM_CODEC.encode(buf1, packet1);
    AuditRequestPacket decoded1 = AuditRequestPacket.STREAM_CODEC.decode(buf1);
    buf1.release();

    ByteBuf buf2 = Unpooled.buffer();
    AuditRequestPacket.STREAM_CODEC.encode(buf2, packet2);
    AuditRequestPacket decoded2 = AuditRequestPacket.STREAM_CODEC.decode(buf2);
    buf2.release();

    assertEquals(0, decoded1.page());
    assertEquals(1, decoded1.pageSize());
    assertEquals(Integer.MAX_VALUE, decoded2.page());
    assertEquals(50, decoded2.pageSize());
  }
}
