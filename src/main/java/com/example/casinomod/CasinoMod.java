package com.example.casinomod;

import com.example.casinomod.block.ModBlocks;
import com.example.casinomod.block.entity.ModBlockEntities;
import com.example.casinomod.item.ModItems;
import com.example.casinomod.screen.ModMenuTypes;
import com.example.casinomod.screen.custom.DealerScreen;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CasinoMod.MODID)
public class CasinoMod {
  // Define mod id in a common place for everything to reference
  public static final String MODID = "casinomod";
  // Directly reference a slf4j logger
  public static final Logger LOGGER = LogUtils.getLogger();
  // Create a Deferred Register to hold Blocks which will all be registered under the "casinomod"
  // namespace
  // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the
  // "casinomod" namespace
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

  // Creates a creative tab with the id "casinomod:example_tab" for the example item, that is placed
  // after the combat tab
  //    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
  // CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
  //            .title(Component.translatable("itemGroup.casinomod")) //The language key for the
  // title of your CreativeModeTab
  //            .withTabsBefore(CreativeModeTabs.COMBAT)
  //            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
  //            .displayItems((parameters, output) -> {
  //                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your
  // own tabs, this method is preferred over the event
  //            }).build());

  // The constructor for the mod class is the first code that is run when your mod is loaded.
  // FML will recognize some parameter types like IEventBus or ModContainer and pass them in
  // automatically.
  public CasinoMod(IEventBus modEventBus, ModContainer modContainer) {
    // Register the commonSetup method for modloading
    modEventBus.addListener(this::commonSetup);

    // Register the Deferred Register to the mod event bus so blocks get registered
    ModBlocks.register(modEventBus);
    // Register the Deferred Register to the mod event bus so items get registered
    ModItems.register(modEventBus);
    ModMenuTypes.register(modEventBus);
    ModBlockEntities.register(modEventBus);
    // Register the Deferred Register to the mod event bus so tabs get registered
    //        CREATIVE_MODE_TABS.register(modEventBus);

    // Register ourselves for server and other game events we are interested in.
    // Note that this is necessary if and only if we want *this* class (CasinoMod) to respond
    // directly to events.
    // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like
    // onServerStarting() below.
    NeoForge.EVENT_BUS.register(this);

    // Register the item to a creative tab
    modEventBus.addListener(this::addCreative);

    // Register our mod's ModConfigSpec so that FML can create and load the config file for us
    modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    // Some common setup code
    LOGGER.info("HELLO FROM COMMON SETUP");

    if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
      LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
    }

    LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

    Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
  }

  // Add the example block item to the building blocks tab
  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    //        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
    //            event.accept(EXAMPLE_BLOCK_ITEM);
    //        }
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {}

  @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      LOGGER.info("HELLO FROM CLIENT SETUP");
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
      event.register(ModMenuTypes.DEALER_MENU.get(), DealerScreen::new);
    }
  }
}
