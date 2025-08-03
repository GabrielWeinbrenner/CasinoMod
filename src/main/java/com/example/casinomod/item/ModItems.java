package com.example.casinomod.item;

import com.example.casinomod.CasinoMod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CasinoMod.MODID);

  public static void register(IEventBus eventBus) {
    ITEMS.register(eventBus);
  }
}
