package com.coraline.clothing;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="coraline")
public final class UtilityEffects {
    private static final UUID REACH_ID=UUID.fromString("ee8da23e-f691-4d5a-91c6-0b7e866a9995");
    private static final Map<UUID,Direction> FACES=new HashMap<>();
    private static final ThreadLocal<Boolean> EXCAVATING=ThreadLocal.withInitial(()->false);
    private static boolean umbrella(Player player){return player.getMainHandItem().is(UtilityItems.MAGIC_UMBRELLA.get())||player.getOffhandItem().is(UtilityItems.MAGIC_UMBRELLA.get());}

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var player=event.player;
        if(!player.level().isClientSide){
            var reach=player.getAttribute(ForgeMod.BLOCK_REACH.get());
            if(reach!=null){
                double bonus=player.isAlive()&&player.getOffhandItem().is(UtilityItems.BUILDERS_DREAM.get())?2:0;
                var old=reach.getModifier(REACH_ID);
                if(old!=null&&old.getAmount()!=bonus)reach.removeModifier(REACH_ID);
                if(bonus>0&&reach.getModifier(REACH_ID)==null)reach.addTransientModifier(new AttributeModifier(REACH_ID,"Builder's Dream block reach",bonus,AttributeModifier.Operation.ADDITION));
            }
            if(player.isAlive()&&umbrella(player)&&player.tickCount%20==0&&player.level().isRainingAt(player.blockPosition())&&player.level().canSeeSky(player.blockPosition()))player.heal(1F);
            if(player.level() instanceof ServerLevel server){
                if(player.tickCount%10==0&&(player.getMainHandItem().is(UtilityItems.MOONLIT_LANTERN.get())||(offhandLightAllowed(player)&&player.getOffhandItem().is(UtilityItems.MOONLIT_LANTERN.get())))){
                    double spread=ClothingEffects.lanternNight(server)?1.8:.45;
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,player.getX(),player.getY()+1,player.getZ(),3,spread,.4,spread,.003);
                }
            }
        }
        if(player.isAlive()&&umbrella(player)&&!player.onGround()&&!player.isInWaterOrBubble()&&!player.isFallFlying()&&!player.getAbilities().flying&&player.getDeltaMovement().y<-.14){
            var motion=player.getDeltaMovement();player.setDeltaMovement(motion.x,-.14,motion.z);player.fallDistance=0;
        }
    }
    private static boolean offhandLightAllowed(Player p){
        if(!(p instanceof ServerPlayer sp))return true;
        var s=com.coraline.feast.OtherMotherLoop.active(sp);
        return s==null||s.loop.activity!=com.coraline.feast.OtherMotherActivities.Activity.OTHER_MINESHAFT||!com.coraline.feast.ActivityWorld.isActivity(p.level());
    }
    // Held lantern illumination is supplied by the cached client light field.
    // Do not place moving invisible blocks: they force vanilla chunk relighting.
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        FACES.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void fall(LivingFallEvent event){
        if(event.getEntity() instanceof Player player&&umbrella(player)){event.setDamageMultiplier(0);event.setCanceled(true);}
    }
    @SubscribeEvent public static void face(PlayerInteractEvent.LeftClickBlock event){
        if(event.getEntity() instanceof ServerPlayer player&&event.getFace()!=null)FACES.put(player.getUUID(),event.getFace());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void excavate(BlockEvent.BreakEvent event){
        if(EXCAVATING.get()||!(event.getLevel() instanceof ServerLevel level)||!(event.getPlayer() instanceof ServerPlayer player))return;
        if(!player.getMainHandItem().is(UtilityItems.EXCAVATOR.get())||!PerfectExcavatorItem.basic(event.getState()))return;
        Direction face=FACES.remove(player.getUUID());if(face==null)face=Direction.UP;
        var origin=event.getPos();EXCAVATING.set(true);
        try{
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
                if(a==0&&b==0)continue;
                var pos=switch(face.getAxis()){
                    case Y->origin.offset(a,0,b);
                    case X->origin.offset(0,a,b);
                    case Z->origin.offset(a,b,0);
                };
                if(!level.hasChunkAt(pos)||player.getMainHandItem().isEmpty())continue;
                BlockState state=level.getBlockState(pos);
                if(!PerfectExcavatorItem.basic(state))continue;
                player.gameMode.destroyBlock(pos);
            }
        }finally{EXCAVATING.set(false);}
    }
}
