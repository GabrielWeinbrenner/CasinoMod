package com.example.casinomod.network;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.BlackjackGame.GamePhase;
import com.example.casinomod.blackjack.handler.BlackjackHandler;
import com.example.casinomod.block.custom.DealerBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record DealerButtonPacket(BlockPos pos, Action action) implements CustomPacketPayload {

  // ──────────────── Enums ────────────────

  public enum Action {
    DEAL,
    HIT,
    STAND,
    DOUBLE_DOWN;

    public static final Action[] VALUES = values();

    public static Action byId(int id) {
      return id >= 0 && id < VALUES.length ? VALUES[id] : DEAL;
    }

    public int getId() {
      return this.ordinal();
    }
  }

  // ──────────────── Codec ────────────────

  public static final CustomPacketPayload.Type<DealerButtonPacket> TYPE =
      new CustomPacketPayload.Type<>(
          ResourceLocation.fromNamespaceAndPath(CasinoMod.MODID, "dealer_button_packet"));

  public static final StreamCodec<ByteBuf, DealerButtonPacket> STREAM_CODEC =
      StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          DealerButtonPacket::pos,
          ByteBufCodecs.idMapper(Action::byId, Action::getId),
          DealerButtonPacket::action,
          DealerButtonPacket::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  // ──────────────── Main Handler ────────────────

  public static void handle(DealerButtonPacket packet, ServerPlayer player) {
    Level level = player.level();
    BlockPos pos = packet.pos();

    logPacket(packet, player);

    if (!level.isLoaded(pos)) {
      CasinoMod.LOGGER.warn("Block position {} not loaded!", pos);
      return;
    }

    BlockEntity be = level.getBlockEntity(pos);
    if (!(be instanceof DealerBlockEntity dealerBe)) {
      CasinoMod.LOGGER.warn("BlockEntity at {} is not a DealerBlockEntity", pos);
      return;
    }

    BlackjackGame game = dealerBe.getGame();
    GamePhase phase = game.getPhase();

    switch (packet.action()) {
      case DEAL -> handleDeal(player, game, dealerBe, level, pos);
      case HIT -> handleHit(game, dealerBe, level, pos, player);
      case STAND -> handleStand(game, dealerBe, level, pos, player);
    }
  }

  // ──────────────── Actions ────────────────

  private static void handleDeal(
      ServerPlayer player,
      BlackjackGame game,
      DealerBlockEntity dealerBe,
      Level level,
      BlockPos pos) {
    if (game.getPhase() != GamePhase.WAITING) {
      CasinoMod.LOGGER.warn("Cannot deal. Current phase: {}", game.getPhase());
      return;
    }

    if (!dealerBe.inventory.getStackInSlot(0).isEmpty()) {
      // Store wager info but don't extract yet - keep it visible during game
      ItemStack wager = dealerBe.inventory.getStackInSlot(0).copy();
      dealerBe.setLastWager(wager);

      if (level.getServer() != null) {
        BlackjackHandler.startGameWithDelay(player, level, pos, dealerBe, game);
      }
    } else {
      CasinoMod.LOGGER.warn("No item found in dealer inventory slot.");
    }
  }

  private static void handleHit(
      BlackjackGame game,
      DealerBlockEntity dealerBe,
      Level level,
      BlockPos pos,
      ServerPlayer player) {
    if (game.getPhase() == GamePhase.PLAYER_TURN) {
      game.hitPlayer();
      updateBlock(level, pos, dealerBe);

      if (game.getPhase() == GamePhase.FINISHED) {
        // If player busts, dealer doesn't play, go straight to result
        BlackjackHandler.simulateDealerTurn(player, level, pos, dealerBe, game);
      }
    } else {
      CasinoMod.LOGGER.warn("Cannot hit. Current phase: {}", game.getPhase());
    }
  }

  private static void handleStand(
      BlackjackGame game,
      DealerBlockEntity dealerBe,
      Level level,
      BlockPos pos,
      ServerPlayer player) {
    if (game.getPhase() == GamePhase.PLAYER_TURN) {
      game.stand();
      updateBlock(level, pos, dealerBe);

      if (level.getServer() != null) {
        BlackjackHandler.simulateDealerTurn(player, level, pos, dealerBe, game);
      }
    } else {
      CasinoMod.LOGGER.warn("Cannot stand. Current phase: {}", game.getPhase());
    }
  }

  // ──────────────── Utilities ────────────────

  private static void updateBlock(Level level, BlockPos pos, DealerBlockEntity dealerBe) {
    dealerBe.setChanged();
    level.sendBlockUpdated(pos, dealerBe.getBlockState(), dealerBe.getBlockState(), 3);
  }

  private static void logPacket(DealerButtonPacket packet, ServerPlayer player) {
    CasinoMod.LOGGER.info(
        "Received DealerButtonPacket: {} from {}", packet.action(), player.getName().getString());
  }
}
