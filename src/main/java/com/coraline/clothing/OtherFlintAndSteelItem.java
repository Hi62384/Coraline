package com.coraline.clothing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="coraline")
public final class OtherFlintAndSteelItem extends FlintAndSteelItem {
    private record Lava(BlockPos pos,long expires){}
    private static final class Sources extends net.minecraft.world.level.saveddata.SavedData {
        final Map<UUID,Lava> active=new HashMap<>();
        static Sources get(net.minecraft.server.level.ServerLevel level){return level.getDataStorage().computeIfAbsent(Sources::load,Sources::new,"coraline_temporary_lava");}
        static Sources load(net.minecraft.nbt.CompoundTag tag){
            var data=new Sources();
            for(var raw:tag.getList("Sources",10)){var n=(net.minecraft.nbt.CompoundTag)raw;
                data.active.put(n.getUUID("Owner"),new Lava(BlockPos.of(n.getLong("Pos")),n.getLong("Expires")));}
            return data;
        }
        @Override public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag){
            var list=new net.minecraft.nbt.ListTag();active.forEach((owner,lava)->{
                var n=new net.minecraft.nbt.CompoundTag();n.putUUID("Owner",owner);n.putLong("Pos",lava.pos.asLong());n.putLong("Expires",lava.expires);list.add(n);
            });tag.put("Sources",list);return tag;
        }
    }
    private final boolean potent;
    public OtherFlintAndSteelItem(boolean potent){super(new Item.Properties().durability(320));this.potent=potent;}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!potent)return super.useOn(context);
        var player=context.getPlayer();if(player==null)return InteractionResult.FAIL;
        return activate(context.getLevel(),player,context.getHand(),context.getClickedPos().relative(context.getClickedFace()),context.getClickedFace());
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!potent)return super.use(level,player,hand);
        BlockPos pos=BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(3)));
        var result=activate(level,player,hand,pos,Direction.UP);
        return new InteractionResultHolder<>(result,player.getItemInHand(hand));
    }
    private InteractionResult activate(Level level,Player player,InteractionHand hand,BlockPos pos,Direction face){
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var server=(net.minecraft.server.level.ServerLevel)level;
        boolean removed=false;
        for(var dimension:server.getServer().getAllLevels()){
            var saved=Sources.get(dimension);Lava old=saved.active.remove(player.getUUID());
            if(old!=null){removed|=clear(dimension,old);saved.setDirty();}
        }
        if(removed)return InteractionResult.CONSUME;
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).canBeReplaced()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,face,player.getItemInHand(hand)))return InteractionResult.FAIL;
        if(!level.getFluidState(pos).isEmpty()||!level.setBlock(pos,Blocks.LAVA.defaultBlockState(),3))return InteractionResult.FAIL;
        var saved=Sources.get(server);saved.active.put(player.getUUID(),new Lava(pos.immutable(),level.getGameTime()+20));saved.setDirty();
        if(!player.getAbilities().instabuild)player.getItemInHand(hand).hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
        player.swing(hand);return InteractionResult.CONSUME;
    }
    private static boolean clear(net.minecraft.server.level.ServerLevel level,Lava lava){
        // Load only the recorded source chunk if necessary; an unloaded chunk must
        // not leave permanent lava behind. Never erase a replacement solid block.
        level.getChunkAt(lava.pos);
        if(!level.getBlockState(lava.pos).is(Blocks.LAVA)||!level.getFluidState(lava.pos).isSource())return false;
        return level.setBlock(lava.pos,Blocks.AIR.defaultBlockState(),3);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.level.isClientSide())return;
        var level=(net.minecraft.server.level.ServerLevel)event.level;var saved=Sources.get(level);
        boolean changed=saved.active.entrySet().removeIf(entry->{var lava=entry.getValue();if(lava.expires>level.getGameTime())return false;clear(level,lava);return true;});
        if(changed)saved.setDirty();
    }
}
