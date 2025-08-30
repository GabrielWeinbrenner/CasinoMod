package com.example.casinomod.network;

import java.util.ArrayList;
import java.util.List;

import com.example.casinomod.blackjack.GameRecord;
import com.example.casinomod.block.custom.DealerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class AuditRequestHandler {
  public static void handle(AuditRequestPacket packet, ServerPlayer player) {
    Level level = player.level();
    BlockPos pos = packet.pos();
    if (!level.isLoaded(pos)) return;
    var be = level.getBlockEntity(pos);
    if (!(be instanceof DealerBlockEntity dealer)) return;

    List<GameRecord> all = dealer.getAudit();
    int total = all.size();
    int page = Math.max(0, packet.page());
    int size = Math.max(1, Math.min(50, packet.pageSize()));

    // Most recent first
    int start = Math.max(0, total - (page + 1) * size);
    int end = Math.min(total, total - page * size);
    List<GameRecord> slice = new ArrayList<>();
    for (int i = end - 1; i >= start; i--) {
      slice.add(all.get(i));
    }

    AuditPagePacket response = new AuditPagePacket(pos, page, size, total, slice);
    com.example.casinomod.CasinoMod.LOGGER.debug(
        "Sending audit page {} ({} items of total {})", page, slice.size(), total);
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, response);
  }
}
