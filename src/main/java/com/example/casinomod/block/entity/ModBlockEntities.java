package com.example.casinomod.block.entity;

import java.util.function.Supplier;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.block.ModBlocks;
import com.example.casinomod.block.custom.DealerBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CasinoMod.MODID);

  public static final Supplier<BlockEntityType<DealerBlockEntity>> DEALER_BE =
      BLOCK_ENTITIES.register(
          "pedestal_be",
          () -> new BlockEntityType<>(DealerBlockEntity::new, ModBlocks.DEALER_BLOCK.get()));

  public static void register(IEventBus eventBus) {
    BLOCK_ENTITIES.register(eventBus);
  }
}
