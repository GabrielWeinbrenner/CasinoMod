package com.example.casinomod.block.custom;

import javax.annotation.Nullable;

import com.example.casinomod.CasinoMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DealerBlock extends BaseEntityBlock {
  public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
  public static final MapCodec<DealerBlock> CODEC = simpleCodec(DealerBlock::new);

  public DealerBlock(Properties properties) {
    super(properties);
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  /* BLOCK ENTITY */

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
    return new DealerBlockEntity(blockPos, blockState);
  }

  @Override
  protected InteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level pLevel,
      BlockPos pPos,
      Player pPlayer,
      InteractionHand hand,
      BlockHitResult hitResult) {
    CasinoMod.LOGGER.info("useItemOn called at position: {}", pPos);
    CasinoMod.LOGGER.info(
        "Player: {} | Hand: {} | Item in hand: {}",
        pPlayer.getName().getString(),
        hand,
        stack.getItem());

    if (!pLevel.isClientSide()) {
      BlockEntity entity = pLevel.getBlockEntity(pPos);
      if (entity instanceof DealerBlockEntity dealerBlockEntity) {
        CasinoMod.LOGGER.info("DealerBlockEntity found. Opening screen...");
        ((ServerPlayer) pPlayer)
            .openMenu(
                new SimpleMenuProvider(dealerBlockEntity, Component.literal("Dealer Block")), pPos);
      } else {
        CasinoMod.LOGGER.error("BlockEntity at {} is not a DealerBlockEntity!", pPos);
        throw new IllegalStateException("Our Container provider is missing!");
      }
    } else {
      CasinoMod.LOGGER.info("useItemOn called on client side, not opening screen.");
    }

    return InteractionResult.SUCCESS;
  }
}
