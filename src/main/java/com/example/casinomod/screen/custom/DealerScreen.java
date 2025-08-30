package com.example.casinomod.screen.custom;

import java.util.List;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.Card;
import com.example.casinomod.network.DealerButtonPacket;
import com.example.casinomod.network.DealerButtonPacket.Action;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class DealerScreen extends AbstractContainerScreen<DealerMenu> {

  private static final ResourceLocation GUI_TEXTURE =
      ResourceLocation.fromNamespaceAndPath(
          CasinoMod.MODID, "textures/gui/dealer_block/dealer_gui.png");

  private static final int BASE_GUI_SIZE = 200; // nominal base for scaling
  private static final int BASE_CARD_WIDTH = 37;
  private static final int BASE_CARD_HEIGHT = 52;
  private static final int BASE_CARD_SPACING = 40;

  // Button references for state management
  private Button dealButton;
  private Button hitButton;
  private Button standButton;
  private Button doubleDownButton;
  private Button splitButton;

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
    int buttonWidth = (int) (30 * scaleFactor); // Smaller to fit 5 buttons
    int spacing = (int) (6 * scaleFactor); // Reduced spacing
    int totalWidth = 5 * buttonWidth + 4 * spacing;
    int startX = centerX - (totalWidth / 2);
    int buttonY =
        (this.height - scaledImageHeight) / 2 + scaledImageHeight - (int) (30 * scaleFactor);

    dealButton =
        Button.builder(
                Component.literal("Deal"),
                b ->
                    ClientPacketDistributor.sendToServer(
                        new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.DEAL)))
            .bounds(startX, buttonY, buttonWidth, (int) (20 * scaleFactor))
            .build();
    this.addRenderableWidget(dealButton);

    hitButton =
        Button.builder(
                Component.literal("Hit"),
                b ->
                    ClientPacketDistributor.sendToServer(
                        new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.HIT)))
            .bounds(
                startX + (buttonWidth + spacing), buttonY, buttonWidth, (int) (20 * scaleFactor))
            .build();
    this.addRenderableWidget(hitButton);

    standButton =
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
            .build();
    this.addRenderableWidget(standButton);

    doubleDownButton =
        Button.builder(
                Component.literal("Double"),
                b ->
                    ClientPacketDistributor.sendToServer(
                        new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.DOUBLE_DOWN)))
            .bounds(
                startX + 3 * (buttonWidth + spacing),
                buttonY,
                buttonWidth,
                (int) (20 * scaleFactor))
            .build();
    this.addRenderableWidget(doubleDownButton);

    splitButton =
        Button.builder(
                Component.literal("Split"),
                b ->
                    ClientPacketDistributor.sendToServer(
                        new DealerButtonPacket(menu.blockEntity.getBlockPos(), Action.SPLIT)))
            .bounds(
                startX + 4 * (buttonWidth + spacing),
                buttonY,
                buttonWidth,
                (int) (20 * scaleFactor))
            .build();
    this.addRenderableWidget(splitButton);

    // Set initial button states
    updateButtonStates();
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    // Update button states based on current game state
    updateButtonStates();

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

    // Display hand values and bet information when cards are dealt
    BlackjackGame game = menu.blockEntity.getGame();
    int playerHandSize = game.getPlayerHand().size();
    int dealerHandSize = game.getDealerHand().size();
    if (playerHandSize > 0 || dealerHandSize > 0) {
      // Player hand value(s) - show all hands if split
      String playerText;
      if (game.hasSplit()) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < game.getHandCount(); i++) {
          if (i > 0) sb.append("  ");
          List<Card> hand = game.getPlayerHand(i);
          int handValue = game.getHandValue(hand);
          sb.append("Hand ").append(i + 1).append(": ").append(handValue);
          if (handValue > 21) {
            sb.append(" (BUST)");
          }
          if (i == game.getCurrentHandIndex()) {
            sb.append(" *"); // Mark active hand
          }
        }
        playerText = sb.toString();
      } else {
        int playerValue = game.getHandValue(game.getPlayerHand());
        playerText = "Player: " + playerValue;
        if (playerValue > 21) {
          playerText += " (BUST)";
        }
      }

      // Dealer hand value (show partial during player turn)
      String dealerText;
      if (game.getPhase() == BlackjackGame.GamePhase.PLAYER_TURN
          && game.getDealerHand().size() >= 2) {
        int firstCardValue = game.getDealerHand().get(0).getBlackjackValue();
        if (game.getDealerHand().get(0).isAce()) {
          dealerText = "Dealer: " + firstCardValue + "/11 + ?";
        } else {
          dealerText = "Dealer: " + firstCardValue + " + ?";
        }
      } else {
        int dealerValue = game.getHandValue(game.getDealerHand());
        dealerText = "Dealer: " + dealerValue;
        if (dealerValue > 21) {
          dealerText += " (BUST)";
        }
      }

      // Position hand values in corners to avoid card overlap
      // Dealer text in top-left corner
      int dealerX = 10;
      int dealerY = 20;
      guiGraphics.drawString(this.font, dealerText, dealerX, dealerY, 0xFFFFFFFF, false);

      // Player text in bottom-left corner
      int playerX = 10;
      int playerY = this.height - 30;
      guiGraphics.drawString(this.font, playerText, playerX, playerY, 0xFFFFFFFF, false);
    }

    // Show current bet amount - use the inventory slot instead of lastWager for now
    ItemStack currentBet = menu.blockEntity.inventory.getStackInSlot(0);
    if (!currentBet.isEmpty()) {
      String betText =
          "Bet: " + currentBet.getCount() + " " + currentBet.getHoverName().getString();
      // Position bet in top-right corner
      int betX = this.width - this.font.width(betText) - 10;
      int betY = 20;
      guiGraphics.drawString(this.font, betText, betX, betY, 0xFFFFFFFF, false);
    } else {
      // Try to show lastWager if inventory is empty but wager was placed
      ItemStack lastWager = menu.blockEntity.getLastWager();
      if (!lastWager.isEmpty()) {
        String betText =
            "Last Bet: " + lastWager.getCount() + " " + lastWager.getHoverName().getString();
        int betX = this.width - this.font.width(betText) - 10;
        int betY = 20;
        guiGraphics.drawString(this.font, betText, betX, betY, 0xFFFFFFFF, false);
      } else {
        // Show "No Bet" message in top-right corner when no bet is placed
        String noBetText = "No Bet";
        int noBetX = this.width - this.font.width(noBetText) - 10;
        int noBetY = 20;
        guiGraphics.drawString(this.font, noBetText, noBetX, noBetY, 0xFFFFFFFF, false);
      }
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

    // Player cards - render all hands if split
    int playerY = guiTop + (int) (130 * scaleFactor);
    if (game.hasSplit()) {
      // Render multiple hands side by side
      int totalHandsWidth = 0;
      for (int i = 0; i < game.getHandCount(); i++) {
        List<Card> hand = game.getPlayerHand(i);
        totalHandsWidth += hand.size() * cardSpacing;
      }
      totalHandsWidth += (game.getHandCount() - 1) * cardSpacing * 2; // Extra spacing between hands

      int currentX = centerX - (totalHandsWidth / 2);

      for (int i = 0; i < game.getHandCount(); i++) {
        List<Card> hand = game.getPlayerHand(i);

        // Highlight active hand
        boolean isActiveHand = (i == game.getCurrentHandIndex());
        if (isActiveHand && game.getPhase() == BlackjackGame.GamePhase.PLAYER_TURN) {
          // Draw a subtle background highlight for the active hand
          int highlightWidth = hand.size() * cardSpacing + 10;
          guiGraphics.fill(
              currentX - 5,
              playerY - 5,
              currentX + highlightWidth,
              playerY + cardHeight + 5,
              0x30FFFFFF); // Semi-transparent white
        }

        renderHand(guiGraphics, hand, currentX, playerY, false, cardSpacing, cardWidth, cardHeight);

        currentX += hand.size() * cardSpacing + cardSpacing * 2; // Move to next hand position
      }
    } else {
      // Single hand rendering
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
    }

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

  private void updateButtonStates() {
    BlackjackGame game = menu.blockEntity.getGame();
    BlackjackGame.GamePhase phase = game.getPhase();
    ItemStack wager = menu.blockEntity.inventory.getStackInSlot(0);

    // Deal button: enabled when waiting for game and wager is placed
    dealButton.active = (phase == BlackjackGame.GamePhase.WAITING && !wager.isEmpty());

    // Hit button: enabled during player turn only
    hitButton.active = (phase == BlackjackGame.GamePhase.PLAYER_TURN);

    // Stand button: enabled during player turn only
    standButton.active = (phase == BlackjackGame.GamePhase.PLAYER_TURN);

    // Double Down button: enabled during player turn, can double down, and player has sufficient
    // matching items
    boolean canDoubleDown = game.canDoubleDown();
    boolean hasMatchingItems = false;

    if (canDoubleDown && !wager.isEmpty()) {
      // Check if player has matching items for the additional wager
      ItemStack additionalWager = new ItemStack(wager.getItem(), wager.getCount());

      Inventory playerInv = Minecraft.getInstance().player.getInventory();
      for (int i = 0; i < playerInv.getContainerSize(); i++) {
        ItemStack slot = playerInv.getItem(i);
        if (ItemStack.isSameItemSameComponents(slot, additionalWager)
            && slot.getCount() >= additionalWager.getCount()) {
          hasMatchingItems = true;
          break;
        }
      }
    }

    doubleDownButton.active = canDoubleDown && hasMatchingItems;

    // Split button: enabled during player turn, can split, and player has sufficient matching items
    boolean canSplit = game.canSplit();
    boolean hasMatchingItemsForSplit = false;

    if (canSplit && !wager.isEmpty()) {
      // Check if player has matching items for the additional wager
      ItemStack additionalWager = new ItemStack(wager.getItem(), wager.getCount());

      Inventory playerInv = Minecraft.getInstance().player.getInventory();
      for (int i = 0; i < playerInv.getContainerSize(); i++) {
        ItemStack slot = playerInv.getItem(i);
        if (ItemStack.isSameItemSameComponents(slot, additionalWager)
            && slot.getCount() >= additionalWager.getCount()) {
          hasMatchingItemsForSplit = true;
          break;
        }
      }
    }

    splitButton.active = canSplit && hasMatchingItemsForSplit;
  }

  private static ResourceLocation getCardTexture(String cardName) {
    String path = "textures/gui/dealer_block/cards/" + cardName + ".png";
    return ResourceLocation.fromNamespaceAndPath(CasinoMod.MODID, path);
  }
}
