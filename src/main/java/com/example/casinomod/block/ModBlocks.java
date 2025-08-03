package com.example.casinomod.block;

import java.util.function.Function;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.block.custom.DealerBlock;
import com.example.casinomod.item.ModItems;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
  public static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks(CasinoMod.MODID);

  public static final DeferredBlock<Block> DEALER_BLOCK =
      registerBlock(
          "dealer_block",
          (properties) ->
              new DealerBlock(
                  properties.strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

  private static <T extends Block> DeferredBlock<T> registerBlock(
      String name, Function<BlockBehaviour.Properties, T> function) {
    DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
    registerBlockItem(name, toReturn);
    return toReturn;
  }

  private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
    ModItems.ITEMS.registerItem(
        name, (properties) -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
  }

  public static void register(IEventBus eventBus) {
    BLOCKS.register(eventBus);
  }
}
