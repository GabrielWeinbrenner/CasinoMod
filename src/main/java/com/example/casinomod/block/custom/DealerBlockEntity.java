package com.example.casinomod.block.custom;

import javax.annotation.Nullable;

import com.example.casinomod.blackjack.BlackjackGame;
import com.example.casinomod.blackjack.GameRecord;
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
          return 64; // Allow stacking for larger bets
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
          // Prevent extraction during active games (except when dealing)
          if (blackjackGame.getPhase() != BlackjackGame.GamePhase.WAITING) {
            return ItemStack.EMPTY;
          }
          return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
          if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
          }
        }
      };

  private ItemStack lastWager = ItemStack.EMPTY;
  private final java.util.List<GameRecord> audit = new java.util.ArrayList<>();
  private GameRecord currentRecord = null;

  public void setLastWager(ItemStack wager) {
    this.lastWager = wager.copy();
  }

  public ItemStack getLastWager() {
    return lastWager;
  }

  public void startAuditRecord() {
    currentRecord = new GameRecord();
    currentRecord.startEpochMs = System.currentTimeMillis();
  }

  public void finalizeAuditRecord(GameRecord record) {
    audit.add(record);
    currentRecord = null;
  }

  public GameRecord getOrCreateCurrentRecord() {
    if (currentRecord == null) {
      startAuditRecord();
    }
    return currentRecord;
  }

  public java.util.List<GameRecord> getAudit() {
    return audit;
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
    // Note: lastWager will be synced through inventory updates instead
    // Persist only a recent slice of audit for client preview (pagination handles the rest).
    var list = output.childrenList("auditPreview");
    int from = Math.max(0, audit.size() - 50);
    for (int i = from; i < audit.size(); i++) {
      GameRecord r = audit.get(i);
      var child = list.addChild();
      child.putLong("s", r.startEpochMs);
      child.putLong("e", r.endEpochMs);
      child.putInt("res", r.result.ordinal());
      child.putInt("bet", r.betCount);
      child.putInt("pay", r.payoutCount);
      child.putString("dd", String.valueOf(r.doubledDown));
      child.putString("sp", String.valueOf(r.split));
      child.putInt("ds", r.dealerScore);
      var ps = child.childrenList("ps");
      for (int v : r.playerScores) ps.addChild().putInt("v", v);
    }
    output.putInt("auditTotal", audit.size());
  }

  @Override
  public void loadAdditional(ValueInput input) {
    input.child("game").ifPresent(blackjackGame::deserialize);
    // Note: lastWager will be synced through inventory updates instead
    // Load preview slice for client-side display; full history remains server-side in memory.
    audit.clear();
    input
        .childrenList("auditPreview")
        .ifPresent(
            list ->
                list.forEach(
                    child -> {
                      GameRecord r = new GameRecord();
                      child.getLong("s").ifPresent(v -> r.startEpochMs = v);
                      child.getLong("e").ifPresent(v -> r.endEpochMs = v);
                      child
                          .getInt("res")
                          .ifPresent(v -> r.result = BlackjackGame.Result.values()[v]);
                      child.getInt("bet").ifPresent(v -> r.betCount = v);
                      child.getInt("pay").ifPresent(v -> r.payoutCount = v);
                      child.getString("dd").ifPresent(s -> r.doubledDown = Boolean.parseBoolean(s));
                      child.getString("sp").ifPresent(s -> r.split = Boolean.parseBoolean(s));
                      child.getInt("ds").ifPresent(v -> r.dealerScore = v);
                      child
                          .childrenList("ps")
                          .ifPresent(
                              ps ->
                                  ps.forEach(
                                      p ->
                                          p.getInt("v").ifPresent(val -> r.playerScores.add(val))));
                      audit.add(r);
                    }));
    // auditTotal is not used client-side directly; pagination packets provide authoritative totals.
  }
}
