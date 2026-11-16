package com.coraline.postgame;

import java.util.*;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reuses the existing pin screen, with an independent owner/target nonce and
 * physical/tool validation on every click. No personal-story knowledge grant,
 * quest completion, loot roll or client-provided price is accepted here. */
@Mod.EventBusSubscriber(modid="coraline")
public final class SiteLockpicking {
    private record Attempt(UUID nonce,UUID site,ResourceKey<Level> dimension,BlockPos target,InteractionHand hand,long start,long last,int pins,int done,int cost,double speed,double window){}
    private static final Map<UUID,Attempt> ACTIVE=new HashMap<>();
    private static SiteLockTarget target(ServerPlayer p,BlockPos at,InteractionHand hand){
        if(!p.isAlive()||p.isSpectator()||p.distanceToSqr(at.getCenter())>25||!p.serverLevel().hasChunkAt(at)||!p.serverLevel().mayInteract(p,at)
            ||!(p.getItemInHand(hand).getItem() instanceof OtherIronLockpick)||!p.mayUseItemAt(at,net.minecraft.core.Direction.UP,p.getItemInHand(hand)))return null;
        var ray=p.level().clip(new ClipContext(p.getEyePosition(),at.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(ray.getType()!=HitResult.Type.MISS&&!ray.getBlockPos().equals(at))return null;
        return p.level().getBlockEntity(at) instanceof SiteLockTarget cache&&cache.locked()&&cache.authentic()&&cache.canPick()?cache:null;
    }
    public static void open(ServerPlayer p,BlockPos at,InteractionHand hand){
        var cache=target(p,at,hand);if(cache==null){p.displayClientMessage(Component.translatable("message.coraline.lockpick_unavailable"),true);return;}
        int index=cache.tier()-1,cost=InfiltrationConfig.COSTS.get(index).get();var tool=p.getItemInHand(hand);
        if(tool.getMaxDamage()-tool.getDamageValue()<cost){p.displayClientMessage(Component.translatable("message.coraline.lockpick_worn",cost),true);return;}
        // Starting a different target retires the previous nonce, including an
        // old personal lock screen; no old room/session is unlocked by this.
        SecretLocks.clear(p.getUUID());p.closeContainer();long now=p.serverLevel().getGameTime();
        var a=new Attempt(UUID.randomUUID(),cache.site(),p.level().dimension(),at.immutable(),hand,now,now-6,InfiltrationConfig.PINS.get(index).get(),0,cost,InfiltrationConfig.SPEEDS.get(index).get(),InfiltrationConfig.WINDOWS.get(index).get());
        ACTIVE.put(p.getUUID(),a);p.swing(hand,true);send(p,a);
    }
    private static void send(ServerPlayer p,Attempt a){
        var cache=p.level().getBlockEntity(a.target) instanceof SiteLockTarget c?c:null;if(cache==null)return;
        var n=new net.minecraft.nbt.CompoundTag();n.putBoolean("Lock",true);n.putBoolean("SiteLock",true);n.putUUID("Nonce",a.nonce);n.putLong("Start",a.start);n.putInt("Pins",a.pins);n.putInt("Completed",a.done);n.putInt("Cost",a.cost);n.putDouble("Speed",a.speed);n.putDouble("Window",a.window);n.putDouble("Target",SiteLockRules.target(cache.tier(),a.done));SecretsNetwork.send(p,n);
    }
    /** True means this nonce belongs to this player's independent site attempt. */
    public static boolean pin(ServerPlayer p,UUID nonce){
        var a=ACTIVE.get(p.getUUID());if(a==null||!a.nonce.equals(nonce))return false;
        long now=p.serverLevel().getGameTime();
        if(!p.level().dimension().equals(a.dimension)||p.containerMenu!=p.inventoryMenu||now<a.start||now-a.start>1200){ACTIVE.remove(p.getUUID());return true;}
        var cache=target(p,a.target,a.hand);
        if(cache==null||!a.site.equals(cache.site())){ACTIVE.remove(p.getUUID());return true;}
        var tool=p.getItemInHand(a.hand);
        if(tool.getMaxDamage()-tool.getDamageValue()<a.cost){ACTIVE.remove(p.getUUID());return true;}
        var outcome=SiteLockRules.pin(cache.tier(),a.pins,a.done,a.start,a.last,now,a.speed,a.window);
        if(outcome==SiteLockRules.Outcome.INVALID){ACTIVE.remove(p.getUUID());return true;}
        if(outcome==SiteLockRules.Outcome.WAIT)return true;
        boolean hit=outcome==SiteLockRules.Outcome.HIT;
        int done=hit?a.done+1:Math.max(0,a.done-1);p.swing(a.hand,true);
        p.playNotifySound(hit?net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get():net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.get(),net.minecraft.sounds.SoundSource.BLOCKS,.45F,hit?1.3F:.7F);
        if(done>=a.pins){
            ACTIVE.remove(p.getUUID());
            var permission=net.minecraftforge.common.ForgeHooks.onRightClickBlock(p,a.hand,a.target,new BlockHitResult(a.target.getCenter(),net.minecraft.core.Direction.UP,a.target,false));
            if(permission.isCanceled()||permission.getUseBlock()==net.minecraftforge.eventbus.api.Event.Result.DENY||permission.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.DENY){p.closeContainer();return true;}
            if(!p.level().dimension().equals(a.dimension)||p.getItemInHand(a.hand)!=tool||tool.getMaxDamage()-tool.getDamageValue()<a.cost||target(p,a.target,a.hand)!=cache){p.closeContainer();return true;}
            // The server processes attempts serially. A competing unlock can
            // succeed once; it never charges a second player's tool afterward.
            if(cache.unlock(a.site)){tool.hurtAndBreak(a.cost,p,who->who.broadcastBreakEvent(a.hand));p.displayClientMessage(Component.translatable(cache.unlockFeedback()),true);}
            p.closeContainer();
        }else{var next=new Attempt(a.nonce,a.site,a.dimension,a.target,a.hand,a.start,now,a.pins,done,a.cost,a.speed,a.window);ACTIVE.put(p.getUUID(),next);send(p,next);}
        return true;
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){ACTIVE.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){ACTIVE.clear();}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        var a=ACTIVE.get(p.getUUID());if(a!=null&&(!p.isAlive()||!p.level().dimension().equals(a.dimension)||p.serverLevel().getGameTime()<a.start||p.serverLevel().getGameTime()-a.start>1200))ACTIVE.remove(p.getUUID());
    }
    private SiteLockpicking(){}
}
