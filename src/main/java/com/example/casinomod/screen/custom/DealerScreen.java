package com.example.casinomod.screen.custom;

import com.example.casinomod.CasinoMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DealerScreen extends AbstractContainerScreen<DealerMenu> {
  private static final ResourceLocation GUI_TEXTURE =
      ResourceLocation.fromNamespaceAndPath(
          CasinoMod.MODID, "textures/gui/dealer_block/dealer_gui.png");

  public DealerScreen(DealerMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = 256;
    this.imageHeight = 256;
  }

  @Override
  protected void init() {
    super.init();

    int x = (this.width - this.imageWidth) / 2;
    int y = (this.height - this.imageHeight) / 2;

    this.addRenderableWidget(
        Button.builder(
                Component.literal("Deal"),
                btn -> {
                  CasinoMod.LOGGER.info("DEAL");
                })
            .bounds(x + 10, y + 10, 60, 20)
            .build());
  }

  @Override
  protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
    int x = (width - imageWidth) / 2;
    int y = (height - imageHeight) / 2;

    guiGraphics.blit(
        RenderPipelines.GUI_TEXTURED,
        GUI_TEXTURE,
        x,
        y,
        0,
        0,
        imageWidth,
        imageHeight,
        1024,
        1024,
        1024,
        1024);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    this.renderTooltip(guiGraphics, mouseX, mouseY);
  }
}
