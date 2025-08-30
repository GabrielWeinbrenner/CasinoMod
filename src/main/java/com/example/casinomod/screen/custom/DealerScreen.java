package com.example.casinomod.screen.custom;

import java.util.List;

import com.example.casinomod.CasinoMod;
import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.Card;
import com.example.casinomod.network.DealerButtonPacket;
import com.example.casinomod.network.DealerButtonPacket.Action;
import com.example.casinomod.network.SettingsPacket;

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
  private Button gearButton;
  private Button auditButton;

  // Audit panel state
  private boolean showAudit = false;
  private int auditPage = 0;
  private static final int AUDIT_PAGE_SIZE = 10;

  // Settings panel state
  private boolean showSettings = false;

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

    // Settings button (top-left)
    gearButton =
        Button.builder(
                Component.literal("⚙"),
                b -> {
                  showSettings = !showSettings;
                  if (showSettings) {
                    showAudit = false; // Close audit if open
                  }
                })
            .bounds(
                guiLeft + (int) (8 * scaleFactor),
                (this.height - scaledImageHeight) / 2 + (int) (8 * scaleFactor),
                (int) (20 * scaleFactor),
                (int) (20 * scaleFactor))
            .build();
    this.addRenderableWidget(gearButton);

    // Audit button (top-right)
    auditButton =
        Button.builder(
                Component.literal("≡"),
                b -> {
                  showAudit = !showAudit;
                  if (showAudit) {
                    auditPage = 0;
                    requestAuditPage();
                    showSettings = false; // Close settings if open
                  }
                })
            .bounds(
                guiLeft + scaledImageWidth - (int) (8 * scaleFactor) - (int) (20 * scaleFactor),
                (this.height - scaledImageHeight) / 2 + (int) (8 * scaleFactor),
                (int) (20 * scaleFactor),
                (int) (20 * scaleFactor))
            .build();
    this.addRenderableWidget(auditButton);

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

    if (showAudit) {
      renderAuditPanel(guiGraphics);
    }

    if (showSettings) {
      renderSettingsPanel(guiGraphics);
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // Handle settings panel clicks
    if (showSettings) {
      if (handleSettingsClick(mouseX, mouseY)) {
        return true;
      }
    }

    // Handle audit panel clicks
    if (showAudit) {
      if (handleAuditClick(mouseX, mouseY)) {
        return true;
      }
    }

    if (!showAudit && !showSettings) return super.mouseClicked(mouseX, mouseY, button);

    return false;
  }

  private boolean handleAuditClick(double mouseX, double mouseY) {
    float scaleFactor =
        Math.min(this.width / (float) BASE_GUI_SIZE, this.height / (float) BASE_GUI_SIZE);
    int scaledImageWidth = (int) (BASE_GUI_SIZE * scaleFactor);
    int scaledImageHeight = (int) (BASE_GUI_SIZE * scaleFactor);
    int guiLeft = (this.width - scaledImageWidth) / 2;
    int guiTop = (this.height - scaledImageHeight) / 2;
    int panelX = guiLeft - (int) (60 * scaleFactor);
    int panelY = guiTop + (int) (20 * scaleFactor);
    int panelW = scaledImageWidth + (int) (120 * scaleFactor);
    int panelH = scaledImageHeight - (int) (40 * scaleFactor);

    // Close button click region
    String closeBtn = "[X]";
    int closeBtnX = panelX + panelW - this.font.width(closeBtn) - 6;
    if (mouseX >= closeBtnX
        && mouseX <= closeBtnX + this.font.width(closeBtn)
        && mouseY >= panelY + 6
        && mouseY <= panelY + 6 + this.font.lineHeight) {
      showAudit = false;
      return true;
    }

    var pd =
        com.example.casinomod.network.AuditPageClientHandler.get(menu.blockEntity.getBlockPos());
    if (pd != null) {
      int total = pd.total();
      int totalPages = (total + AUDIT_PAGE_SIZE - 1) / AUDIT_PAGE_SIZE;
      boolean canPrev = auditPage > 0;
      boolean canNext = auditPage + 1 < totalPages;

      int navY = panelY + panelH - 18;
      int prevW = this.font.width("< Prev");
      int nextW = this.font.width("Next >");
      int gap = this.font.width("   ");
      int navTotalW = prevW + gap + nextW;
      int navX = panelX + panelW - navTotalW - 8;

      // Click regions for prev/next
      if (canPrev
          && mouseX >= navX
          && mouseX <= navX + prevW
          && mouseY >= navY
          && mouseY <= navY + this.font.lineHeight) {
        auditPage--;
        requestAuditPage();
        return true;
      }

      int nextX = navX + prevW + gap;
      if (canNext
          && mouseX >= nextX
          && mouseX <= nextX + nextW
          && mouseY >= navY
          && mouseY <= navY + this.font.lineHeight) {
        auditPage++;
        requestAuditPage();
        return true;
      }
    }

    return false;
  }

  private boolean handleSettingsClick(double mouseX, double mouseY) {
    float scaleFactor =
        Math.min(this.width / (float) BASE_GUI_SIZE, this.height / (float) BASE_GUI_SIZE);
    int scaledImageWidth = (int) (BASE_GUI_SIZE * scaleFactor);
    int scaledImageHeight = (int) (BASE_GUI_SIZE * scaleFactor);
    int guiLeft = (this.width - scaledImageWidth) / 2;
    int guiTop = (this.height - scaledImageHeight) / 2;
    int panelX = guiLeft - (int) (60 * scaleFactor);
    int panelY = guiTop + (int) (20 * scaleFactor);
    int panelW = scaledImageWidth + (int) (120 * scaleFactor);
    int panelH = scaledImageHeight - (int) (40 * scaleFactor);

    // Close button click region
    String closeBtn = "[X]";
    int closeBtnX = panelX + panelW - this.font.width(closeBtn) - 6;
    if (mouseX >= closeBtnX
        && mouseX <= closeBtnX + this.font.width(closeBtn)
        && mouseY >= panelY + 6
        && mouseY <= panelY + 6 + this.font.lineHeight) {
      showSettings = false;
      return true;
    }

    // Handle settings panel clicks
    int settingsY = panelY + 6 + this.font.lineHeight + 10 + this.font.lineHeight + 5;

    // Surrender checkbox - click anywhere on the line
    String surrenderText =
        (menu.blockEntity.isSurrenderAllowed() ? "[✓]" : "[ ]") + " Surrender allowed";
    if (mouseX >= panelX + 6
        && mouseX <= panelX + 6 + this.font.width(surrenderText)
        && mouseY >= settingsY
        && mouseY <= settingsY + this.font.lineHeight) {
      sendSettingsUpdate(
          !menu.blockEntity.isSurrenderAllowed(),
          menu.blockEntity.isDealerHitsSoft17(),
          menu.blockEntity.getNumberOfDecks(),
          menu.blockEntity.getMinBet(),
          menu.blockEntity.getMaxBet());
      return true;
    }
    settingsY += this.font.lineHeight + 2;

    // Soft 17 checkbox - click anywhere on the line
    String soft17Text =
        (menu.blockEntity.isDealerHitsSoft17() ? "[✓]" : "[ ]") + " Dealer hits soft 17";
    if (mouseX >= panelX + 6
        && mouseX <= panelX + 6 + this.font.width(soft17Text)
        && mouseY >= settingsY
        && mouseY <= settingsY + this.font.lineHeight) {
      sendSettingsUpdate(
          menu.blockEntity.isSurrenderAllowed(),
          !menu.blockEntity.isDealerHitsSoft17(),
          menu.blockEntity.getNumberOfDecks(),
          menu.blockEntity.getMinBet(),
          menu.blockEntity.getMaxBet());
      return true;
    }
    settingsY += this.font.lineHeight + 2;

    // Deck count controls
    int decksX = panelX + 6 + this.font.width("Number of decks: ");
    int countX = decksX + this.font.width("[-] ");
    int plusX = countX + this.font.width(String.valueOf(menu.blockEntity.getNumberOfDecks())) + 2;

    // [-] button
    if (mouseX >= decksX
        && mouseX <= decksX + this.font.width("[-]")
        && mouseY >= settingsY
        && mouseY <= settingsY + this.font.lineHeight) {
      int newDecks = Math.max(1, menu.blockEntity.getNumberOfDecks() - 1);
      sendSettingsUpdate(
          menu.blockEntity.isSurrenderAllowed(),
          menu.blockEntity.isDealerHitsSoft17(),
          newDecks,
          menu.blockEntity.getMinBet(),
          menu.blockEntity.getMaxBet());
      return true;
    }

    // [+] button
    if (mouseX >= plusX
        && mouseX <= plusX + this.font.width("[+]")
        && mouseY >= settingsY
        && mouseY <= settingsY + this.font.lineHeight) {
      int newDecks = Math.min(8, menu.blockEntity.getNumberOfDecks() + 1);
      sendSettingsUpdate(
          menu.blockEntity.isSurrenderAllowed(),
          menu.blockEntity.isDealerHitsSoft17(),
          newDecks,
          menu.blockEntity.getMinBet(),
          menu.blockEntity.getMaxBet());
      return true;
    }

    return false;
  }

  private void sendSettingsUpdate(
      boolean surrenderAllowed,
      boolean dealerHitsSoft17,
      int numberOfDecks,
      int minBet,
      int maxBet) {
    SettingsPacket packet =
        new SettingsPacket(
            menu.blockEntity.getBlockPos(),
            surrenderAllowed,
            dealerHitsSoft17,
            numberOfDecks,
            minBet,
            maxBet);
    ClientPacketDistributor.sendToServer(packet);
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

  private void requestAuditPage() {
    ClientPacketDistributor.sendToServer(
        new com.example.casinomod.network.AuditRequestPacket(
            menu.blockEntity.getBlockPos(), auditPage, AUDIT_PAGE_SIZE));
  }

  private void renderAuditPanel(GuiGraphics g) {
    float scaleFactor =
        Math.min(this.width / (float) BASE_GUI_SIZE, this.height / (float) BASE_GUI_SIZE);
    int scaledImageWidth = (int) (BASE_GUI_SIZE * scaleFactor);
    int scaledImageHeight = (int) (BASE_GUI_SIZE * scaleFactor);
    int guiLeft = (this.width - scaledImageWidth) / 2;
    int guiTop = (this.height - scaledImageHeight) / 2;

    int panelX = guiLeft - (int) (60 * scaleFactor);
    int panelY = guiTop + (int) (20 * scaleFactor);
    int panelW = scaledImageWidth + (int) (120 * scaleFactor);
    int panelH = scaledImageHeight - (int) (40 * scaleFactor);

    g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xAA000000);

    // Header with close button
    String header = "Audit Log (page " + (auditPage + 1) + ")";
    g.drawString(this.font, header, panelX + 6, panelY + 6, 0xFFFFFFFF, false);

    // Close button (X) in top-right of panel
    String closeBtn = "[X]";
    int closeBtnX = panelX + panelW - this.font.width(closeBtn) - 6;
    g.drawString(this.font, closeBtn, closeBtnX, panelY + 6, 0xFFFF5555, false);

    // Fetch latest page from client cache
    var pd =
        com.example.casinomod.network.AuditPageClientHandler.get(menu.blockEntity.getBlockPos());
    int y = panelY + 20;
    int lineH = this.font.lineHeight + 2;
    java.time.format.DateTimeFormatter fmt =
        java.time.format.DateTimeFormatter.ofPattern("MM-dd-yy HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault());
    int logsEndY = y; // Track where logs end
    if (pd != null && pd.records() != null) {
      int idx = 0;
      for (com.example.casinomod.blackjack.GameRecord r : pd.records()) {
        // Reserve space for stats (4 lines * lineH + some padding)
        int statsSpace = 4 * lineH + 20;
        if (panelY + panelH - (y + idx * lineH) < statsSpace + 50) break;

        String ts = fmt.format(java.time.Instant.ofEpochMilli(r.startEpochMs));
        String res = r.result.name();
        String bet = String.valueOf(r.betCount);
        String pay = String.valueOf(r.payoutCount);
        String flags = (r.doubledDown ? "DD " : "") + (r.split ? "SP" : "");
        String scores = "P:" + r.playerScores.toString() + " D:" + r.dealerScore;
        String line =
            ts + " | " + res + " | bet " + bet + " → " + pay + " | " + flags + " | " + scores;

        g.drawString(this.font, line, panelX + 6, y + idx * lineH, 0xFFE0E0E0, false);
        idx++;
        logsEndY = y + idx * lineH;
      }

      int total = pd.total();
      int totalPages = (total + AUDIT_PAGE_SIZE - 1) / AUDIT_PAGE_SIZE;
      boolean canPrev = auditPage > 0;
      boolean canNext = auditPage + 1 < totalPages;
      // Calculate stats from ALL audit records (not just current page)
      int wins = 0, losses = 0, draws = 0;
      int totalWagered = 0, totalWon = 0;
      int ddGames = 0, splitGames = 0;

      // Request full audit data for stats calculation
      var fullAudit = menu.blockEntity.getAudit();
      for (var record : fullAudit) {
        switch (record.result) {
          case WIN -> wins++;
          case LOSE -> losses++;
          case DRAW -> draws++;
        }
        totalWagered += record.betCount;
        totalWon += record.payoutCount;
        if (record.doubledDown) ddGames++;
        if (record.split) splitGames++;
      }

      // Stats section - position dynamically after logs
      int statsY = Math.max(logsEndY + 10, panelY + panelH - 90);
      g.drawString(this.font, "--- Stats ---", panelX + 6, statsY, 0xFFFFFF00, false);
      statsY += this.font.lineHeight + 2;

      String winRate = total > 0 ? String.format("%.1f%%", (wins * 100.0 / total)) : "0%";
      g.drawString(
          this.font,
          "W:" + wins + " L:" + losses + " D:" + draws + " (" + winRate + ")",
          panelX + 6,
          statsY,
          0xFFAAAAFF,
          false);
      statsY += this.font.lineHeight;

      int netProfit = totalWon - totalWagered;
      String profitColor = netProfit >= 0 ? "0xFF55FF55" : "0xFFFF5555";
      g.drawString(
          this.font,
          "Net: "
              + (netProfit >= 0 ? "+" : "")
              + netProfit
              + " (Wagered:"
              + totalWagered
              + " Won:"
              + totalWon
              + ")",
          panelX + 6,
          statsY,
          Integer.parseUnsignedInt(profitColor.substring(2), 16),
          false);
      statsY += this.font.lineHeight;

      g.drawString(
          this.font,
          "DD:" + ddGames + " Split:" + splitGames,
          panelX + 6,
          statsY,
          0xFFCCCCCC,
          false);

      // Show results count and navigation with range display
      int startRange = total == 0 ? 0 : Math.max(1, total - (auditPage + 1) * AUDIT_PAGE_SIZE + 1);
      int endRange = Math.min(total, total - auditPage * AUDIT_PAGE_SIZE);
      String resultsInfo;
      if (total == 0) {
        resultsInfo = "No results";
      } else {
        resultsInfo = "Showing " + startRange + "-" + endRange + " of " + total + " total results";
      }
      g.drawString(this.font, resultsInfo, panelX + 6, panelY + panelH - 30, 0xFFCCCCCC, false);

      String nav = (canPrev ? "< Prev" : "     ") + "   " + (canNext ? "Next >" : "     ");
      int navY = panelY + panelH - 18;
      int navX = panelX + panelW - this.font.width(nav) - 8;
      g.drawString(this.font, nav, navX, navY, 0xFFFFFFFF, false);
    } else {
      g.drawString(this.font, "Loading...", panelX + 6, y, 0xFFFFFFFF, false);
    }
  }

  private void renderSettingsPanel(GuiGraphics g) {
    float scaleFactor =
        Math.min(this.width / (float) BASE_GUI_SIZE, this.height / (float) BASE_GUI_SIZE);
    int scaledImageWidth = (int) (BASE_GUI_SIZE * scaleFactor);
    int scaledImageHeight = (int) (BASE_GUI_SIZE * scaleFactor);
    int guiLeft = (this.width - scaledImageWidth) / 2;
    int guiTop = (this.height - scaledImageHeight) / 2;
    int panelX = guiLeft - (int) (60 * scaleFactor);
    int panelY = guiTop + (int) (20 * scaleFactor);
    int panelW = scaledImageWidth + (int) (120 * scaleFactor);
    int panelH = scaledImageHeight - (int) (40 * scaleFactor);

    // Semi-transparent dark background
    g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);

    // Border
    g.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF555555);
    g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF555555);
    g.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF555555);
    g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF555555);

    // Title and close button
    g.drawString(this.font, "Blackjack Settings", panelX + 6, panelY + 6, 0xFFFFFFFF, false);
    String closeBtn = "[X]";
    int closeBtnX = panelX + panelW - this.font.width(closeBtn) - 6;
    g.drawString(this.font, closeBtn, closeBtnX, panelY + 6, 0xFFFF5555, false);

    int y = panelY + 6 + this.font.lineHeight + 10;

    // Game settings controls
    g.drawString(this.font, "Game Settings:", panelX + 6, y, 0xFFFFFF00, false);
    y += this.font.lineHeight + 5;

    // Surrender setting
    String surrenderCheck = menu.blockEntity.isSurrenderAllowed() ? "[✓]" : "[ ]";
    g.drawString(
        this.font, surrenderCheck + " Surrender allowed", panelX + 6, y, 0xFFAAAAAA, false);
    y += this.font.lineHeight + 2;

    // Soft 17 setting
    String soft17Check = menu.blockEntity.isDealerHitsSoft17() ? "[✓]" : "[ ]";
    g.drawString(this.font, soft17Check + " Dealer hits soft 17", panelX + 6, y, 0xFFAAAAAA, false);
    y += this.font.lineHeight + 2;

    // Number of decks with +/- controls
    g.drawString(this.font, "Number of decks: ", panelX + 6, y, 0xFFAAAAAA, false);
    int decksX = panelX + 6 + this.font.width("Number of decks: ");
    g.drawString(this.font, "[-]", decksX, y, 0xFF5555FF, false);
    int countX = decksX + this.font.width("[-] ");
    g.drawString(
        this.font,
        String.valueOf(menu.blockEntity.getNumberOfDecks()),
        countX,
        y,
        0xFFFFFFFF,
        false);
    int plusX = countX + this.font.width(String.valueOf(menu.blockEntity.getNumberOfDecks())) + 2;
    g.drawString(this.font, "[+]", plusX, y, 0xFF5555FF, false);
    y += this.font.lineHeight + 2;

    // Bet limits
    g.drawString(
        this.font,
        "Bet limits: " + menu.blockEntity.getMinBet() + "-" + menu.blockEntity.getMaxBet(),
        panelX + 6,
        y,
        0xFFAAAAAA,
        false);
  }
}
