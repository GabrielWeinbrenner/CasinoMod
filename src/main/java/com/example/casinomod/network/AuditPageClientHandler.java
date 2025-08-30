package com.example.casinomod.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.casinomod.blackjack.GameRecord;

import net.minecraft.core.BlockPos;

/** Simple client-side cache for received audit pages per block position. */
public class AuditPageClientHandler {
  public record PageData(int page, int pageSize, int total, List<GameRecord> records) {}

  private static final Map<BlockPos, PageData> CACHE = new HashMap<>();

  public static void handle(AuditPagePacket packet) {
    CACHE.put(
        packet.pos(),
        new PageData(packet.page(), packet.pageSize(), packet.total(), packet.records()));
  }

  public static PageData get(BlockPos pos) {
    return CACHE.get(pos);
  }
}
