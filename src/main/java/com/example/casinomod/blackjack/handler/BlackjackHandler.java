package com.example.casinomod.blackjack.handler;

import java.util.List;
import java.util.Objects;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.Card;
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
            () -> drawCard(game, game.getPlayerHand(), "Player (1st)", level, pos, dealerBe),
            () -> drawCard(game, game.getDealerHand(), "Dealer (1st)", level, pos, dealerBe),
            () -> drawCard(game, game.getPlayerHand(), "Player (2nd)", level, pos, dealerBe),
            () -> drawCard(game, game.getDealerHand(), "Dealer (2nd)", level, pos, dealerBe),
            () -> {
              game.setPhase(BlackjackGame.GamePhase.PLAYER_TURN);
              CasinoMod.LOGGER.debug("[BlackjackHandler] All cards dealt. Player turn begins.");
            });

    scheduleDrawSteps(drawSteps, serverLevel, 0);
  }

  private static void drawCard(
      BlackjackGame game,
      List<Card> hand,
      String who,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe) {
    Card card = game.draw();
    hand.add(card);
    CasinoMod.LOGGER.debug("[BlackjackHandler] {} draws {}", who, card);
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

      ServerTaskScheduler.schedule(
          serverLevel.getServer(),
          () -> {
            dealerBe.inventory.setStackInSlot(0, ItemStack.EMPTY);
            game.reset();
            updateBlock(level, pos, dealerBe);
            CasinoMod.LOGGER.debug("[DealerTurn] Game reset after dealer finishes.");
          },
          100);
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
      case LOSE -> handleLoss(player, level, pos);
      case DRAW -> handleDraw(player, level, pos, dealerBe);
    }
    ServerTaskScheduler.schedule(
        Objects.requireNonNull(level.getServer()),
        () -> {
          game.reset();
          dealerBe.inventory.setStackInSlot(0, ItemStack.EMPTY);
          updateBlock(level, pos, dealerBe);
        },
        60);
  }

  private static void handleWin(
      ServerPlayer player,
      Level level,
      BlockPos pos,
      DealerBlockEntity dealerBe,
      BlackjackGame game) {
    ItemStack wager = dealerBe.getLastWager();
    if (!wager.isEmpty()) {
      int multiplier = game.isBlackjack() ? 3 : 2;
      int rewardCount = wager.getCount() * multiplier;

      ItemStack reward = wager.copyWithCount(rewardCount);
      if (!player.getInventory().add(reward)) {
        player.drop(reward, false);
      }

      player.sendSystemMessage(
          Component.literal(
              game.isBlackjack()
                  ? "Blackjack! 1.5x payout!"
                  : "You win! Wager returned with payout."));
      playFeedback(level, pos, SoundEvents.PLAYER_LEVELUP, ParticleTypes.HAPPY_VILLAGER);
    }
  }

  private static void handleLoss(ServerPlayer player, Level level, BlockPos pos) {
    player.sendSystemMessage(Component.literal("You lost your wager."));
    playFeedback(level, pos, SoundEvents.VILLAGER_NO, ParticleTypes.SMOKE);
  }

  private static void handleDraw(
      ServerPlayer player, Level level, BlockPos pos, DealerBlockEntity dealerBe) {
    ItemStack wager = dealerBe.getLastWager();
    if (!wager.isEmpty()) {
      if (!player.getInventory().add(wager.copy())) {
        player.drop(wager.copy(), false);
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
