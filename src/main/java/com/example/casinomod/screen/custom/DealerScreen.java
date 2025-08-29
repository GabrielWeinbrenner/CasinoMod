package com.example.casinomod.screen.custom;

import java.util.List;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.Card;
import com.example.casinomod.network.DealerButtonPacket;
import com.example.casinomod.network.DealerButtonPacket.Action;

import org.jetbrains.annotations.NotNull;

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

  private static final int BASE_GUI_SIZE = 200; // nominal base for scaling
  private static final int BASE_CARD_WIDTH = 37;
  private static final int BASE_CARD_HEIGHT = 52;
  private static final int BASE_CARD_SPACING = 40;

  public DealerScreen(DealerMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = BASE_GUI_SIZE;
    this.imageHeight = BASE_GUI_SIZE;
  }

  @Override
  protected void init() {
    super.init();

    float scaleFactor =
        Math.min(this.width / (float) BASE_GUI_SIZE, this.height / (float) BASE_GUI_SIZE);
    int scaledImageWidth = (int) (BASE_GUI_SIZE * scaleFactor);
    int scaledImageHeight = (int) (BASE_GUI_SIZE * scaleFactor);
    int guiLeft = (this.width - scaledImageWidth) / 2;

    int centerX = guiLeft + scaledImageWidth / 2;
    int buttonWidth = (int) (40 * scaleFactor);
    int spacing = (int) (10 * scaleFactor);
    int totalWidth = 3 * buttonWidth + 2 * spacing;
    int startX = centerX - (totalWidth / 2);
    int buttonY =
        (this.height - scaledImageHeight) / 2 + scaledImageHeight - (int) (30 * scaleFactor);

    this.addRenderableWidget(
        Button.builder(
                Component.literal("Deal"),
                b ->
                    ClientPacketDistributor.sendToServer(
                        new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.DEAL)))
            .bounds(startX, buttonY, buttonWidth, (int) (20 * scaleFactor))
            .build());

    this.addRenderableWidget(
        Button.builder(
                Component.literal("Hit"),
                b ->
                    ClientPacketDistributor.sendToServer(
                        new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.HIT)))
            .bounds(
                startX + (buttonWidth + spacing), buttonY, buttonWidth, (int) (20 * scaleFactor))
            .build());

    this.addRenderableWidget(
        Button.builder(
                Component.literal("Stand"),
                b ->
                    ClientPacketDistributor.sendToServer(
                        new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.STAND)))
            .bounds(
                startX + 2 * (buttonWidth + spacing),
                buttonY,
                buttonWidth,
                (int) (20 * scaleFactor))
            .build());
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);

    BlackjackGame.Result result = menu.blockEntity.getGame().getResult();
    if (result != null) {
      String message =
          switch (result) {
            case WIN -> "You Win!";
            case LOSE -> "You Lose!";
            case DRAW -> "It's a Tie!";
            default -> "";
          };

      int color =
          switch (result) {
            case WIN -> 0xFFFFFFFF; // Green
            case LOSE -> 0xFFFFFFFF; // Red
            case DRAW -> 0xFFFFFFFF; // Yellow
            default -> 0xFFFFFFFF; // White
          };
      // Center on the whole screen
      int x = (this.width - this.font.width(message)) / 2;
      int y = this.height / 2 - this.font.lineHeight / 2;
      guiGraphics.drawString(this.font, message, x, y, color, false);
    }
  }

  @Override
  protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    float scaleFactor =
        Math.min(this.width / (float) BASE_GUI_SIZE, this.height / (float) BASE_GUI_SIZE);
    int scaledImageWidth = (int) (BASE_GUI_SIZE * scaleFactor);
    int scaledImageHeight = (int) (BASE_GUI_SIZE * scaleFactor);
    int guiLeft = (this.width - scaledImageWidth) / 2;
    int guiTop = (this.height - scaledImageHeight) / 2;

    // Draw background texture
    guiGraphics.blit(
        RenderPipelines.GUI_TEXTURED,
        GUI_TEXTURE,
        guiLeft,
        guiTop,
        0,
        0,
        scaledImageWidth,
        scaledImageHeight,
        1024,
        1024,
        1024,
        1024);

    BlackjackGame game = menu.blockEntity.getGame();

    float cardScale = 0.75f; // smaller cards
    int cardSpacing = (int) (BASE_CARD_SPACING * scaleFactor * cardScale);
    int cardWidth = (int) (BASE_CARD_WIDTH * scaleFactor * cardScale);
    int cardHeight = (int) (BASE_CARD_HEIGHT * scaleFactor * cardScale);

    int centerX = guiLeft + scaledImageWidth / 2;

    // Dealer cards
    int dealerY = guiTop + (int) (50 * scaleFactor);
    int dealerStartX = centerX - (game.getDealerHand().size() * cardSpacing / 2);
    renderHand(
        guiGraphics,
        game.getDealerHand(),
        dealerStartX,
        dealerY,
        true,
        cardSpacing,
        cardWidth,
        cardHeight);

    // Player cards
    int playerY = guiTop + (int) (130 * scaleFactor);
    int playerStartX = centerX - (game.getPlayerHand().size() * cardSpacing / 2);
    renderHand(
        guiGraphics,
        game.getPlayerHand(),
        playerStartX,
        playerY,
        false,
        cardSpacing,
        cardWidth,
        cardHeight);
    BlackjackGame.Result result = game.getResult();
  }

  private void renderHand(
      GuiGraphics guiGraphics,
      List<Card> hand,
      int startX,
      int startY,
      boolean isDealer,
      int cardSpacing,
      int cardWidth,
      int cardHeight) {

    BlackjackGame.GamePhase phase = menu.blockEntity.getGame().getPhase();

    for (int i = 0; i < hand.size(); i++) {
      Card card = hand.get(i);
      boolean isSecondDealerCard = isDealer && i == 1;
      boolean showBack = isSecondDealerCard && phase == BlackjackGame.GamePhase.PLAYER_TURN;

      String cardName = showBack ? "back" : card.getCardName();
      ResourceLocation texture = getCardTexture(cardName);

      guiGraphics.blit(
          RenderPipelines.GUI_TEXTURED,
          texture,
          startX + i * cardSpacing,
          startY,
          0f,
          0f,
          cardWidth,
          cardHeight,
          cardWidth,
          cardHeight,
          cardWidth,
          cardHeight);
    }
  }

  @Override
  protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    return;
  }

  private static ResourceLocation getCardTexture(String cardName) {
    String path = "textures/gui/dealer_block/cards/" + cardName + ".png";
    return ResourceLocation.fromNamespaceAndPath(CasinoMod.MODID, path);
  }
}
