package com.coraline.clothing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.WebBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.coraline.registry.ModBlocks;
import com.coraline.otherworld.OtherWorld;

@Mod.EventBusSubscriber(modid="coraline")
public final class SpiderArmor {
    public static boolean hasWebBonus(Player p){return ArmorSets.full(p,ArmorSets.Set.SPIDERVISION)||superSet(p);}
    public static boolean superSet(Player p){return ArmorSets.full(p,ArmorSets.Set.SUPER_SPIDERVISION);}
    public static boolean placeWeb(Player p,BlockPos pos){
        if(!(p.level() instanceof ServerLevel level)||level.dimension().equals(OtherWorld.KEY)||p.isSpectator()||!p.mayBuild())return false;
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isAir()||!level.getWorldBorder().isWithinBounds(pos))return false;
        if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)||!level.mayInteract(p,pos))return false;
        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(p,snapshot,net.minecraft.core.Direction.UP))return false;
        return level.setBlock(pos,ModBlocks.FADING_COBWEB.get().defaultBlockState(),3);
    }
    public static boolean inWeb(Player p){
        var box=p.getBoundingBox().deflate(.01);
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(p.level().getBlockState(pos).getBlock() instanceof WebBlock)return true;
        return false;
    }
    @SubscribeEvent public static void movement(TickEvent.PlayerTickEvent event){
        var p=event.player;if(event.phase!=TickEvent.Phase.END)return;
        var tag=p.getPersistentData();
        if(!p.isAlive()||!superSet(p)||p.isSpectator()||p.getAbilities().flying||p.isFallFlying()||p.isPassenger()) {tag.remove("CoralineCeilingCling");return;}
        boolean jump=tag.getBoolean("CoralineSpiderJump")&&p.tickCount-tag.getInt("CoralineSpiderInputTick")<30;
        boolean down=p.isShiftKeyDown();var motion=p.getDeltaMovement();
        boolean ceiling=!p.level().noCollision(p,p.getBoundingBox().deflate(.03).move(0,.16,0));
        boolean cling=ceiling&&!down&&(jump||tag.getBoolean("CoralineCeilingCling"));
        tag.putBoolean("CoralineCeilingCling",cling);
        if(cling){p.setDeltaMovement(motion.x,.08,motion.z);p.fallDistance=0;}
        else if(inWeb(p)){
            double y=jump?ClothingConfig.CLIMB_SPEED.get():down?-ClothingConfig.CLIMB_SPEED.get():0;
            p.setDeltaMovement(motion.x,y,motion.z);p.fallDistance=0;
        }else if(p.horizontalCollision&&!down){p.setDeltaMovement(motion.x,Math.max(motion.y,ClothingConfig.CLIMB_SPEED.get()),motion.z);p.fallDistance=0;}
    }
    @SubscribeEvent public static void attacked(LivingDamageEvent event){
        if(!(event.getEntity() instanceof Player p)||!(p.level() instanceof ServerLevel level)||event.getAmount()<=0||!hasWebBonus(p)||!(event.getSource().getEntity() instanceof Mob))return;
        if(level.dimension().equals(OtherWorld.KEY)||p.isSpectator()||!p.mayBuild())return;
        var tag=p.getPersistentData();long now=level.getGameTime();
        if(tag.contains("CoralineWebLast")&&now-tag.getLong("CoralineWebLast")<ClothingConfig.WEB_COOLDOWN.get())return;
        if(p.getRandom().nextDouble()>=ClothingConfig.WEB_CHANCE.get())return;
        tag.putLong("CoralineWebLast",now);
        int placed=0;
        for(int i=0;i<12&&placed<3;i++){
            var pos=p.blockPosition().offset(p.getRandom().nextInt(5)-2,0,p.getRandom().nextInt(5)-2);
            if(!pos.equals(p.blockPosition())&&placeWeb(p,pos))placed++;
        }
    }
}
