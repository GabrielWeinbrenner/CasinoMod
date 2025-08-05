package com.example.casinomod.network;

import com.example.casinomod.CasinoMod;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {
  public static void register(IEventBus eventBus) {
    eventBus.addListener(ModMessages::registerMessages);
  }

  private static void registerMessages(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(CasinoMod.MODID);

    registrar.playToServer(
        DealerButtonPacket.TYPE,
        DealerButtonPacket.STREAM_CODEC,
        (packet, context) -> {
          if (context.player() instanceof ServerPlayer player) {
            DealerButtonPacket.handle(packet, player);
          }
        });
  }
}
