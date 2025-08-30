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

    registrar.playToServer(
        AuditRequestPacket.TYPE,
        AuditRequestPacket.STREAM_CODEC,
        (packet, context) -> {
          if (context.player() instanceof ServerPlayer player) {
            AuditRequestHandler.handle(packet, player);
          }
        });

    registrar.playToClient(
        AuditPagePacket.TYPE,
        AuditPagePacket.STREAM_CODEC,
        (packet, context) -> AuditPageClientHandler.handle(packet));

    registrar.playToServer(
        SettingsPacket.TYPE,
        SettingsPacket.CODEC,
        (packet, context) -> SettingsHandler.getInstance().handle(packet, context));
  }
}
