package com.coraline.clothing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** Harvests a short forward arc, consuming a matching seed before each automatic replant. */
public final class GardenMantisArmItem extends Item {
    public GardenMantisArmItem(){super(new Item.Properties().durability(384));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.sidedSuccess(stack,true);
        var center=player.blockPosition();var direction=player.getLookAngle();int harvested=0;
        for(int dy=-1;dy<=1;dy++)for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
            if(dx*dx+dz*dz>16||dx*direction.x+dz*direction.z<1)continue;
            BlockPos pos=center.offset(dx,dy,dz);if(!level.hasChunkAt(pos)||!level.mayInteract(player,pos))continue;
            var state=level.getBlockState(pos);
            if(!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state))continue;
            Item seed=state.is(Blocks.WHEAT)?Items.WHEAT_SEEDS:state.is(Blocks.CARROTS)?Items.CARROT:state.is(Blocks.POTATOES)?Items.POTATO:state.is(Blocks.BEETROOTS)?Items.BEETROOT_SEEDS:null;
            int slot=-1;
            if(seed!=null)for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(seed)){slot=i;break;}
            if(level.destroyBlock(pos,true,player)){
                harvested++;
                if(slot>=0){player.getInventory().getItem(slot).shrink(1);level.setBlock(pos,crop.getStateForAge(0),3);}
                if(!player.getAbilities().instabuild)stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
                if(stack.isEmpty()||harvested>=64)break;
            }
        }
        if(harvested==0)return InteractionResultHolder.fail(stack);
        player.swing(hand);return InteractionResultHolder.success(stack);
    }
}
