package com.example.casinomod.network;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.BlackjackGame.GamePhase;
import com.example.casinomod.blackjack.handler.BlackjackHandler;
import com.example.casinomod.block.custom.DealerBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
      case DOUBLE_DOWN -> handleDoubleDown(game, dealerBe, level, pos, player);
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
    if (game.getPhase() != GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.warn("Cannot hit. Current phase: {}", game.getPhase());
      return;
    }

    game.hitPlayer();
    updateBlock(level, pos, dealerBe);
    proceedToNextPhaseIfNeeded(game, player, level, pos, dealerBe);
  }

  private static void handleStand(
      BlackjackGame game,
      DealerBlockEntity dealerBe,
      Level level,
      BlockPos pos,
      ServerPlayer player) {
    if (game.getPhase() != GamePhase.PLAYER_TURN) {
      CasinoMod.LOGGER.warn("Cannot stand. Current phase: {}", game.getPhase());
      return;
    }

    game.stand();
    updateBlock(level, pos, dealerBe);
    proceedToNextPhaseIfNeeded(game, player, level, pos, dealerBe);
  }

  private static void handleDoubleDown(
      BlackjackGame game,
      DealerBlockEntity dealerBe,
      Level level,
      BlockPos pos,
      ServerPlayer player) {
    if (!game.canDoubleDown()) {
      CasinoMod.LOGGER.warn("Cannot double down. Current phase: {}, Hand size: {}", 
          game.getPhase(), game.getPlayerHand().size());
      return;
    }

    // Get the current wager to double it
    ItemStack currentWager = dealerBe.inventory.getStackInSlot(0);
    if (currentWager.isEmpty()) {
      CasinoMod.LOGGER.warn("Cannot double down - no wager found");
      return;
    }

    // Try to extract matching wager from player's inventory for the double down
    ItemStack additionalWager = new ItemStack(currentWager.getItem(), currentWager.getCount());
    boolean extracted = false;
    
    // Try to remove the additional wager from player's inventory
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
      ItemStack slot = player.getInventory().getItem(i);
      if (ItemStack.isSameItemSameComponents(slot, additionalWager) && slot.getCount() >= additionalWager.getCount()) {
        slot.shrink(additionalWager.getCount());
        extracted = true;
        break;
      }
    }

    if (!extracted) {
      player.sendSystemMessage(Component.literal("Cannot double down - insufficient matching items in inventory"));
      CasinoMod.LOGGER.warn("Player {} cannot double down - insufficient matching items", player.getName().getString());
      return;
    }

    // Add the additional wager to the dealer inventory to double the bet
    ItemStack doubledWager = currentWager.copy();
    doubledWager.setCount(currentWager.getCount() * 2);
    dealerBe.inventory.setStackInSlot(0, doubledWager);
    
    // Update the stored wager amount
    dealerBe.setLastWager(doubledWager.copy());

    game.doubleDown();
    updateBlock(level, pos, dealerBe);
    proceedToNextPhaseIfNeeded(game, player, level, pos, dealerBe);
    
    CasinoMod.LOGGER.info("Player {} doubled down, wager doubled from {} to {}", 
        player.getName().getString(), currentWager.getCount(), doubledWager.getCount());
  }

  private static void proceedToNextPhaseIfNeeded(
      BlackjackGame game,
      ServerPlayer player,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe) {
    // Proceed to dealer turn if game phase changed to dealer turn or finished
    if ((game.getPhase() == GamePhase.DEALER_TURN || game.getPhase() == GamePhase.FINISHED) 
        && level.getServer() != null) {
      BlackjackHandler.simulateDealerTurn(player, level, pos, dealerBe, game);
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
