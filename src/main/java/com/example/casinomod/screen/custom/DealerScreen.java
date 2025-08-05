package com.example.casinomod.screen.custom;

import java.util.List;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.Card;
import com.example.casinomod.network.DealerButtonPacket;
import com.example.casinomod.network.DealerButtonPacket.Action;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

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
    int centerX = (this.width - this.imageWidth) / 2;
    int centerY = (this.height - this.imageHeight) / 2;

    // Log game state
    BlackjackGame.GamePhase phase = menu.blockEntity.getGame().getPhase();
    CasinoMod.LOGGER.debug("Opening DealerScreen with game phase: {}", phase);

    // Center-aligned buttons
    int buttonWidth = 60;
    int spacing = 10;
    int totalWidth = 3 * buttonWidth + 2 * spacing;
    int startX = centerX + (this.imageWidth - totalWidth) / 2;

    this.addRenderableWidget(
        Button.builder(
                Component.literal("Deal"),
                b -> {
                  CasinoMod.LOGGER.info("Deal button clicked");
                  ClientPacketDistributor.sendToServer(
                      new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.DEAL));
                })
            .bounds(startX, centerY + 200, buttonWidth, 20)
            .build());

    this.addRenderableWidget(
        Button.builder(
                Component.literal("Hit"),
                b -> {
                  CasinoMod.LOGGER.info("Hit button clicked");
                  ClientPacketDistributor.sendToServer(
                      new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.HIT));
                })
            .bounds(startX + (buttonWidth + spacing), centerY + 200, buttonWidth, 20)
            .build());

    this.addRenderableWidget(
        Button.builder(
                Component.literal("Stand"),
                b -> {
                  CasinoMod.LOGGER.info("Stand button clicked");
                  ClientPacketDistributor.sendToServer(
                      new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.STAND));
                })
            .bounds(startX + 2 * (buttonWidth + spacing), centerY + 200, buttonWidth, 20)
            .build());
  }

  @Override
  protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
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

    BlackjackGame game = menu.blockEntity.getGame();
    BlackjackGame.Result result = game.getResult();
    renderHand(guiGraphics, game.getPlayerHand(), x + 20, y + 170, false);
    renderHand(guiGraphics, game.getDealerHand(), x + 20, y + 40, true);

    if (result != null) {
      String message =
          switch (result) {
            case WIN -> "You Win!";
            case LOSE -> "You Lose!";
            case DRAW -> "It's a Tie!";
            default -> "";
          };

      guiGraphics.drawCenteredString(this.font, message, this.width / 2, y + 10, 0xFFFFFF);
    }
  }

  private void renderHand(
      GuiGraphics guiGraphics, List<Card> hand, int startX, int startY, boolean isDealer) {
    int spacing = 40;
    BlackjackGame.GamePhase phase = menu.blockEntity.getGame().getPhase();
    for (int i = 0; i < hand.size(); i++) {
      Card card = hand.get(i);
      boolean isSecondDealerCard = isDealer && i == 1;
      boolean showBack = isSecondDealerCard && phase == BlackjackGame.GamePhase.PLAYER_TURN;

      String cardName = showBack ? "back" : card.getCardName();
      CasinoMod.LOGGER.trace(
          "{} card {} being rendered at {},{}",
          isDealer ? "Dealer" : "Player",
          cardName,
          startX + i * spacing,
          startY);

      ResourceLocation texture = getCardTexture(cardName);
      guiGraphics.blit(
          RenderPipelines.GUI_TEXTURED,
          texture,
          startX + i * spacing,
          startY,
          0f,
          0f,
          37,
          52,
          37,
          52,
          37,
          52);
    }
  }

  private static ResourceLocation getCardTexture(String cardName) {
    String path = "textures/gui/dealer_block/cards/" + cardName + ".png";
    CasinoMod.LOGGER.trace("Trying to load card texture: {}", path);
    return ResourceLocation.fromNamespaceAndPath(CasinoMod.MODID, path);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    this.renderTooltip(guiGraphics, mouseX, mouseY);
  }
}
