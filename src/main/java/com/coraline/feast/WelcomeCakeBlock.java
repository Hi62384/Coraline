package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** Normal cake eating, but the block item remembers exactly how many slices remain. */
public final class WelcomeCakeBlock extends CakeBlock {
    public WelcomeCakeBlock(){super(BlockBehaviour.Properties.copy(Blocks.CAKE).strength(.5F));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        if(!c.getLevel().dimension().equals(Level.OVERWORLD))return null;
        int bites=c.getItemInHand().getOrCreateTag().getInt("CakeBites");
        return defaultBlockState().setValue(BITES,Math.max(0,Math.min(6,bites)));
    }
    public static ItemStack stack(int bites){var s=new ItemStack(com.coraline.registry.ModItems.WELCOME_CAKE.get());s.getOrCreateTag().putInt("CakeBites",bites);return s;}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(stack(state.getValue(BITES)));}
    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state){return stack(state.getValue(BITES));}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!p.canEat(false))return InteractionResult.PASS;
        if(!level.isClientSide){p.getFoodData().eat(2,.1F);int bites=state.getValue(BITES);if(bites<6)level.setBlock(pos,state.setValue(BITES,bites+1),3);else level.removeBlock(pos,false);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
