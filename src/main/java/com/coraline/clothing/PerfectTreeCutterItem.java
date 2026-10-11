package com.coraline.clothing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayDeque;
import java.util.HashSet;

/** Fells a bounded, leaf-bearing connected trunk without consuming player-built log walls. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PerfectTreeCutterItem extends AxeItem {
    private static final ThreadLocal<Boolean> ACTIVE=ThreadLocal.withInitial(()->false);
    public PerfectTreeCutterItem(){super(OtherMaterials.PERFECT,5,-3F,new Item.Properties());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void fell(BlockEvent.BreakEvent event){
        if(event.isCanceled()||ACTIVE.get()||!(event.getLevel() instanceof ServerLevel level)||!(event.getPlayer() instanceof ServerPlayer player)||!player.getMainHandItem().is(ArtifactItems.PERFECT_TREE_CUTTER.get())||!event.getState().is(BlockTags.LOGS))return;
        var start=event.getPos();var queue=new ArrayDeque<BlockPos>();var logs=new HashSet<BlockPos>();queue.add(start);boolean leaves=false;
        while(!queue.isEmpty()&&logs.size()<192){
            var pos=queue.removeFirst();if(!logs.add(pos))continue;
            for(var direction:net.minecraft.core.Direction.values()){
                var next=pos.relative(direction);if(!level.hasChunkAt(next))continue;
                var state=level.getBlockState(next);
                if(state.is(BlockTags.LEAVES))leaves=true;
                if(state.is(BlockTags.LOGS)&&!logs.contains(next)&&next.distManhattan(start)<28)queue.add(next);
            }
        }
        if(!leaves)return;
        ACTIVE.set(true);
        try{for(var pos:logs){if(pos.equals(start)||player.getMainHandItem().isEmpty())continue;if(level.mayInteract(player,pos))player.gameMode.destroyBlock(pos);}}
        finally{ACTIVE.set(false);}
    }
}
