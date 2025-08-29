package com.example.casinomod.screen.custom;

import com.example.casinomod.block.ModBlocks;
import com.example.casinomod.block.custom.DealerBlockEntity;
import com.example.casinomod.screen.ModMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class DealerMenu extends AbstractContainerMenu {
  public final DealerBlockEntity blockEntity;
  private final Level level;

  public DealerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
    this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
  }

  public DealerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
    super(ModMenuTypes.DEALER_MENU.get(), containerId);
    this.blockEntity = ((DealerBlockEntity) blockEntity);
    this.level = inv.player.level();

    // Hotbar at bottom (nominal coordinates)
    addPlayerHotbar(inv, 160);

    // Wager slot centered for nominal 200×200 GUI
    this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 0, 100 - 9, 80));
  }

  private static final int HOTBAR_SLOT_COUNT = 9;
  private static final int VANILLA_FIRST_SLOT_INDEX = 0;
  private static final int TE_INVENTORY_FIRST_SLOT_INDEX =
      VANILLA_FIRST_SLOT_INDEX + HOTBAR_SLOT_COUNT;
  private static final int TE_INVENTORY_SLOT_COUNT = 1;

  @Override
  public ItemStack quickMoveStack(Player playerIn, int pIndex) {
    Slot sourceSlot = slots.get(pIndex);
    if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

    ItemStack sourceStack = sourceSlot.getItem();
    ItemStack copyOfSourceStack = sourceStack.copy();

    if (pIndex < VANILLA_FIRST_SLOT_INDEX + HOTBAR_SLOT_COUNT) {
      if (!moveItemStackTo(
          sourceStack,
          TE_INVENTORY_FIRST_SLOT_INDEX,
          TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT,
          false)) {
        return ItemStack.EMPTY;
      }
    } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
      if (!moveItemStackTo(
          sourceStack,
          VANILLA_FIRST_SLOT_INDEX,
          VANILLA_FIRST_SLOT_INDEX + HOTBAR_SLOT_COUNT,
          false)) {
        return ItemStack.EMPTY;
      }
    } else {
      System.out.println("Invalid slotIndex:" + pIndex);
      return ItemStack.EMPTY;
    }

    if (sourceStack.isEmpty()) {
      sourceSlot.set(ItemStack.EMPTY);
    } else {
      sourceSlot.setChanged();
    }
    sourceSlot.onTake(playerIn, sourceStack);
    return copyOfSourceStack;
  }

  @Override
  public boolean stillValid(Player player) {
    return stillValid(
        ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
        player,
        ModBlocks.DEALER_BLOCK.get());
  }

  private void addPlayerHotbar(Inventory playerInventory, int offsetY) {
    int startX = 8;
    for (int i = 0; i < 9; ++i) {
      this.addSlot(new Slot(playerInventory, i, startX + i * 18, offsetY));
    }
  }
}
