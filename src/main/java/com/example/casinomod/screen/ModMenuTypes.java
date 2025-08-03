package com.example.casinomod.screen;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.screen.custom.DealerMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, CasinoMod.MODID);

  public static final DeferredHolder<MenuType<?>, MenuType<DealerMenu>> DEALER_MENU =
      registerMenuType("dealer_menu", DealerMenu::new);

  private static <T extends AbstractContainerMenu>
      DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
          String name, IContainerFactory<T> factory) {
    return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
  }

  public static void register(IEventBus eventBus) {
    MENUS.register(eventBus);
  }
}
