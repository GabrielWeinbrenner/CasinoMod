package com.example.casinomod.blackjack;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Minimal audit record for a completed blackjack game. */
public class GameRecord {
  public long startEpochMs;
  public long endEpochMs;
  public BlackjackGame.Result result;
  public int betCount;
  public int payoutCount;
  public boolean doubledDown;
  public boolean split;
  public int dealerScore;
  public List<Integer> playerScores = new ArrayList<>();

  public GameRecord() {}

  public static final StreamCodec<ByteBuf, GameRecord> STREAM_CODEC =
      new StreamCodec<>() {
        @Override
        public GameRecord decode(ByteBuf buf) {
          GameRecord r = new GameRecord();
          r.startEpochMs = buf.readLong();
          r.endEpochMs = buf.readLong();
          r.result = BlackjackGame.Result.values()[ByteBufCodecs.VAR_INT.decode(buf)];
          r.betCount = ByteBufCodecs.VAR_INT.decode(buf);
          r.payoutCount = ByteBufCodecs.VAR_INT.decode(buf);
          r.doubledDown = buf.readBoolean();
          r.split = buf.readBoolean();
          r.dealerScore = ByteBufCodecs.VAR_INT.decode(buf);
          int n = ByteBufCodecs.VAR_INT.decode(buf);
          for (int i = 0; i < n; i++) {
            r.playerScores.add(ByteBufCodecs.VAR_INT.decode(buf));
          }
          return r;
        }

        @Override
        public void encode(ByteBuf buf, GameRecord r) {
          buf.writeLong(r.startEpochMs);
          buf.writeLong(r.endEpochMs);
          ByteBufCodecs.VAR_INT.encode(buf, r.result.ordinal());
          ByteBufCodecs.VAR_INT.encode(buf, r.betCount);
          ByteBufCodecs.VAR_INT.encode(buf, r.payoutCount);
          buf.writeBoolean(r.doubledDown);
          buf.writeBoolean(r.split);
          ByteBufCodecs.VAR_INT.encode(buf, r.dealerScore);
          ByteBufCodecs.VAR_INT.encode(buf, r.playerScores.size());
          for (int v : r.playerScores) {
            ByteBufCodecs.VAR_INT.encode(buf, v);
          }
        }
      };
}
