package com.example.casinomod.block.custom;

import javax.annotation.Nullable;

import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.block.entity.ModBlockEntities;
import com.example.casinomod.screen.custom.DealerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DealerBlockEntity extends BlockEntity implements MenuProvider {
  private final BlackjackGame blackjackGame = new BlackjackGame();
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

  private ItemStack lastWager = ItemStack.EMPTY;

  public void setLastWager(ItemStack wager) {
    this.lastWager = wager.copy();
  }

  public ItemStack getLastWager() {
    return lastWager;
  }

  public DealerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.DEALER_BE.get(), pos, state);
  }

  public final BlackjackGame getGame() {
    return blackjackGame;
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

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
    return this.saveWithoutMetadata(provider);
  }

  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public void saveAdditional(ValueOutput output) {
    blackjackGame.serialize(output.child("game"));
  }

  @Override
  public void loadAdditional(ValueInput input) {
    input.child("game").ifPresent(blackjackGame::deserialize);
  }
}
