package com.example.casinomod.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SettingsPacket(
    BlockPos blockPos,
    boolean surrenderAllowed,
    boolean dealerHitsSoft17,
    int numberOfDecks,
    int minBet,
    int maxBet)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SettingsPacket> TYPE =
      new CustomPacketPayload.Type<>(
          ResourceLocation.fromNamespaceAndPath("casinomod", "settings"));

  public static final StreamCodec<FriendlyByteBuf, SettingsPacket> CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          SettingsPacket::blockPos,
          ByteBufCodecs.BOOL,
          SettingsPacket::surrenderAllowed,
          ByteBufCodecs.BOOL,
          SettingsPacket::dealerHitsSoft17,
          ByteBufCodecs.VAR_INT,
          SettingsPacket::numberOfDecks,
          ByteBufCodecs.VAR_INT,
          SettingsPacket::minBet,
          ByteBufCodecs.VAR_INT,
          SettingsPacket::maxBet,
          SettingsPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
