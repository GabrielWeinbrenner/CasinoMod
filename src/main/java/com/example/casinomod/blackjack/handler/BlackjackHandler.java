package com.example.casinomod.blackjack.handler;

import java.util.List;
import java.util.Objects;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.block.custom.DealerBlockEntity;
import com.example.casinomod.util.ServerTaskScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlackjackHandler {

  // ─────────────── Start Game With Suspense ───────────────

  public static void startGameWithDelay(
      ServerPlayer player,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe,
      BlackjackGame game) {
    if (!(level instanceof ServerLevel serverLevel)) return;

    game.startGame(); // Shuffle and reset

    List<Runnable> drawSteps =
        List.of(
            () -> dealPlayerCard(game, "Player (1st)", level, pos, dealerBe),
            () -> dealDealerCard(game, "Dealer (1st)", level, pos, dealerBe),
            () -> dealPlayerCard(game, "Player (2nd)", level, pos, dealerBe),
            () -> dealDealerCard(game, "Dealer (2nd)", level, pos, dealerBe),
            () -> {
              // Check for dealer blackjack after all initial cards are dealt
              if (game.isDealerBlackjack()) {
                CasinoMod.LOGGER.debug("[BlackjackHandler] Dealer has blackjack!");
                game.setPhase(BlackjackGame.GamePhase.FINISHED);
                BlackjackGame.Result result = game.determineResult();
                handleResult(result, player, level, pos, dealerBe, game);
              } else {
                game.setPhase(BlackjackGame.GamePhase.PLAYER_TURN);
                CasinoMod.LOGGER.debug("[BlackjackHandler] All cards dealt. Player turn begins.");
              }
            });

    scheduleDrawSteps(drawSteps, serverLevel, 0);
  }

  private static void dealPlayerCard(
      BlackjackGame game, String who, Level level, BlockPos pos, DealerBlockEntity dealerBe) {
    game.dealToPlayer();
    CasinoMod.LOGGER.debug("[BlackjackHandler] {} draws (player)", who);
    updateBlock(level, pos, dealerBe);
  }

  private static void dealDealerCard(
      BlackjackGame game, String who, Level level, BlockPos pos, DealerBlockEntity dealerBe) {
    game.dealToDealer();
    CasinoMod.LOGGER.debug("[BlackjackHandler] {} draws (dealer)", who);
    updateBlock(level, pos, dealerBe);
  }

  private static void scheduleDrawSteps(List<Runnable> steps, ServerLevel serverLevel, int index) {
    if (index >= steps.size()) return;

    steps.get(index).run();
    ServerTaskScheduler.schedule(
        serverLevel.getServer(), () -> scheduleDrawSteps(steps, serverLevel, index + 1), 10);
  }

  // ─────────────── Dealer AI Turn ───────────────

  public static void simulateDealerTurn(
      ServerPlayer player,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe,
      BlackjackGame game) {

    if (!(level instanceof ServerLevel serverLevel)) return;

    ServerTaskScheduler.schedule(
        serverLevel.getServer(),
        () -> {
          updateBlock(level, pos, dealerBe);
          CasinoMod.LOGGER.debug("[DealerTurn] Hole card revealed.");

          runDealerDrawLoop(player, level, pos, dealerBe, game, serverLevel);
        },
        20);
  }

  private static void runDealerDrawLoop(
      ServerPlayer player,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe,
      BlackjackGame game,
      ServerLevel serverLevel) {

    boolean dealerContinues = game.hitDealer();
    updateBlock(level, pos, dealerBe);

    if (dealerContinues) {
      ServerTaskScheduler.schedule(
          serverLevel.getServer(),
          () -> runDealerDrawLoop(player, level, pos, dealerBe, game, serverLevel),
          20);
    } else {
      BlackjackGame.Result result = game.determineResult();
      handleResult(result, player, level, pos, dealerBe, game);
      // Note: handleResult will schedule the reset - no need to duplicate it here
    }
  }

  // ─────────────── Results + Rewards ───────────────

  private static void handleResult(
      BlackjackGame.Result result,
      ServerPlayer player,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe,
      BlackjackGame game) {
    switch (result) {
      case WIN -> handleWin(player, level, pos, dealerBe, game);
      case LOSE -> handleLoss(player, level, pos, dealerBe);
      case DRAW -> handleDraw(player, level, pos, dealerBe);
    }
    ServerTaskScheduler.schedule(
        Objects.requireNonNull(level.getServer()),
        () -> {
          game.reset();
          updateBlock(level, pos, dealerBe);
          CasinoMod.LOGGER.debug("[BlackjackHandler] Game reset after handling results.");
        },
        60);
  }

  private static void handleWin(
      ServerPlayer player,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe,
      BlackjackGame game) {
    // Extract the wager from inventory and calculate payout
    ItemStack wager = dealerBe.inventory.getStackInSlot(0);
    if (!wager.isEmpty()) {
      dealerBe.inventory.setStackInSlot(0, ItemStack.EMPTY);

      // Calculate effective wager (doubled if player doubled down)
      int effectiveWager = game.hasDoubledDown() ? wager.getCount() * 2 : wager.getCount();

      // Blackjack pays 3:2 (1.5x payout), regular win pays 1:1 (1x payout)
      // Note: Blackjack bonus doesn't apply when doubled down
      int totalReturnCount;
      if (game.isBlackjack() && !game.hasDoubledDown()) {
        totalReturnCount =
            effectiveWager + (wager.getCount() * 3 / 2); // 1.5x payout on original wager
      } else {
        totalReturnCount = effectiveWager * 2; // 1x payout on effective wager
      }

      ItemStack reward = wager.copyWithCount(totalReturnCount);
      if (!player.getInventory().add(reward)) {
        player.drop(reward, false);
      }

      String message;
      if (game.isBlackjack() && !game.hasDoubledDown()) {
        message = "Blackjack! 1.5x payout!";
      } else if (game.hasDoubledDown()) {
        message = "You win! Double down payout!";
      } else {
        message = "You win! Wager returned with payout.";
      }
      player.sendSystemMessage(Component.literal(message));
      playFeedback(level, pos, SoundEvents.PLAYER_LEVELUP, ParticleTypes.HAPPY_VILLAGER);
    }
  }

  private static void handleLoss(
      ServerPlayer player, Level level, BlockPos pos, DealerBlockEntity dealerBe) {
    // Extract the wager from inventory since player lost
    ItemStack wager = dealerBe.inventory.getStackInSlot(0);
    if (!wager.isEmpty()) {
      dealerBe.inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    player.sendSystemMessage(Component.literal("You lost your wager."));
    playFeedback(level, pos, SoundEvents.VILLAGER_NO, ParticleTypes.SMOKE);
  }

  private static void handleDraw(
      ServerPlayer player, Level level, BlockPos pos, DealerBlockEntity dealerBe) {
    // Extract the wager from inventory and return it to player
    ItemStack wager = dealerBe.inventory.getStackInSlot(0);
    if (!wager.isEmpty()) {
      dealerBe.inventory.setStackInSlot(0, ItemStack.EMPTY);
      if (!player.getInventory().add(wager)) {
        player.drop(wager, false);
      }
      player.sendSystemMessage(Component.literal("It's a draw. Your wager has been returned."));
      playFeedback(level, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, ParticleTypes.NOTE);
    }
  }

  // ─────────────── Helpers ───────────────

  private static void playFeedback(
      Level level, BlockPos pos, SoundEvent sound, ParticleOptions particle) {
    level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    if (level instanceof ServerLevel serverLevel) {
      serverLevel.sendParticles(
          particle, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.01);
    }
  }

  private static void updateBlock(Level level, BlockPos pos, DealerBlockEntity dealerBe) {
    dealerBe.setChanged();
    level.sendBlockUpdated(pos, dealerBe.getBlockState(), dealerBe.getBlockState(), 3);
  }
}
