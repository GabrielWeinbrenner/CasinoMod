package com.example.casinomod.network;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.block.custom.DealerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SettingsHandler {
  private static final SettingsHandler INSTANCE = new SettingsHandler();

  public static SettingsHandler getInstance() {
    return INSTANCE;
  }

  public void handle(final SettingsPacket data, final IPayloadContext context) {
    // This code runs on the receiving side - the server when sent from client
    context.enqueueWork(
        () -> {
          if (context.flow().isServerbound()) {
            var player = context.player();
            var level = player.level();

            CasinoMod.LOGGER.debug(
                "[SettingsHandler] Received settings update for block at {}", data.blockPos());

            BlockEntity blockEntity = level.getBlockEntity(data.blockPos());
            if (blockEntity instanceof DealerBlockEntity dealerEntity) {
              // Validate the player has permission to change settings
              double distance =
                  player
                      .position()
                      .distanceToSqr(
                          data.blockPos().getX() + 0.5,
                          data.blockPos().getY() + 0.5,
                          data.blockPos().getZ() + 0.5);

              if (distance > 64) { // 8 block max distance
                CasinoMod.LOGGER.warn(
                    "[SettingsHandler] Player {} too far from block to change settings",
                    player.getName().getString());
                return;
              }

              // Update settings
              dealerEntity.setSurrenderAllowed(data.surrenderAllowed());
              dealerEntity.setDealerHitsSoft17(data.dealerHitsSoft17());
              dealerEntity.setNumberOfDecks(data.numberOfDecks());
              dealerEntity.setMinBet(data.minBet());
              dealerEntity.setMaxBet(data.maxBet());
              
              // Sync changes to client
              level.sendBlockUpdated(data.blockPos(), level.getBlockState(data.blockPos()), level.getBlockState(data.blockPos()), 3);

              CasinoMod.LOGGER.info(
                  "[SettingsHandler] Updated settings for dealer block at {}: surrender={}, soft17={}, decks={}, betLimits={}-{}",
                  data.blockPos(),
                  data.surrenderAllowed(),
                  data.dealerHitsSoft17(),
                  data.numberOfDecks(),
                  data.minBet(),
                  data.maxBet());
            } else {
              CasinoMod.LOGGER.error(
                  "[SettingsHandler] Block entity at {} is not a DealerBlockEntity",
                  data.blockPos());
            }
          }
        });
  }
}
