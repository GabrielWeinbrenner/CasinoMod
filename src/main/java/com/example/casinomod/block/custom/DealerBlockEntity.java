package com.example.casinomod.block.custom;

import javax.annotation.Nullable;

import com.example.casinomod.block.entity.ModBlockEntities;
import com.example.casinomod.screen.custom.DealerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DealerBlockEntity extends BlockEntity implements MenuProvider {
  public final ItemStackHandler inventory =
      new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
          return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
          if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
          }
        }
      };

  public DealerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.DEALER_BE.get(), pos, state);
  }

  @Override
  public Component getDisplayName() {
    return Component.literal("Dealer Table");
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
    return new DealerMenu(id, playerInventory, this);
  }
}
