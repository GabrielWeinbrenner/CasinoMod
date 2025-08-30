package com.example.casinomod.network;

import java.util.ArrayList;
import java.util.List;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.GameRecord;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AuditPagePacket(
    BlockPos pos, int page, int pageSize, int total, List<GameRecord> records)
    implements CustomPacketPayload {
  public static final CustomPacketPayload.Type<AuditPagePacket> TYPE =
      new CustomPacketPayload.Type<>(
          ResourceLocation.fromNamespaceAndPath(CasinoMod.MODID, "audit_page"));

  public static final StreamCodec<ByteBuf, AuditPagePacket> STREAM_CODEC =
      new StreamCodec<>() {
        @Override
        public AuditPagePacket decode(ByteBuf buf) {
          BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
          int page = ByteBufCodecs.VAR_INT.decode(buf);
          int pageSize = ByteBufCodecs.VAR_INT.decode(buf);
          int total = ByteBufCodecs.VAR_INT.decode(buf);
          int n = ByteBufCodecs.VAR_INT.decode(buf);
          List<GameRecord> recs = new ArrayList<>(n);
          for (int i = 0; i < n; i++) {
            recs.add(GameRecord.STREAM_CODEC.decode(buf));
          }
          return new AuditPagePacket(pos, page, pageSize, total, recs);
        }

        @Override
        public void encode(ByteBuf buf, AuditPagePacket p) {
          BlockPos.STREAM_CODEC.encode(buf, p.pos);
          ByteBufCodecs.VAR_INT.encode(buf, p.page);
          ByteBufCodecs.VAR_INT.encode(buf, p.pageSize);
          ByteBufCodecs.VAR_INT.encode(buf, p.total);
          ByteBufCodecs.VAR_INT.encode(buf, p.records.size());
          for (GameRecord r : p.records) {
            GameRecord.STREAM_CODEC.encode(buf, r);
          }
        }
      };

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
