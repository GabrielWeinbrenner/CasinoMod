package com.example.casinomod.network;

import com.example.casinomod.CasinoMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AuditRequestPacket(BlockPos pos, int page, int pageSize)
    implements CustomPacketPayload {
  public static final CustomPacketPayload.Type<AuditRequestPacket> TYPE =
      new CustomPacketPayload.Type<>(
          ResourceLocation.fromNamespaceAndPath(CasinoMod.MODID, "audit_request"));

  public static final StreamCodec<ByteBuf, AuditRequestPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          AuditRequestPacket::pos,
          ByteBufCodecs.VAR_INT,
          AuditRequestPacket::page,
          ByteBufCodecs.VAR_INT,
          AuditRequestPacket::pageSize,
          AuditRequestPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
