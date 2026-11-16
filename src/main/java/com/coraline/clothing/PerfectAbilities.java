package com.coraline.clothing;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline")
public final class PerfectAbilities {
    public static final ThreadLocal<Boolean> MINING=ThreadLocal.withInitial(()->false);
    private static final String LAST="CoralinePerfectAbilityAt",BARRIER="CoralinePerfectBarrierAt";
    private static final DustParticleOptions GOLD=new DustParticleOptions(new org.joml.Vector3f(1,.77F,.15F),.9F);
    private static final Map<MinecraftServer,List<Slash>> SLASHES=new WeakHashMap<>();
    private static final class Slash {
        final ServerPlayer player;final net.minecraft.server.level.ServerLevel level;final Vec3 direction;final Set<UUID> hit=new HashSet<>();Vec3 at;int ticks;
        Slash(ServerPlayer p){player=p;level=p.serverLevel();at=p.getEyePosition();direction=p.getLookAngle().scale(.75);}
    }
    private static boolean ready(ServerPlayer p,String key,int cooldown){
        if(!PerfectPower.powered(p))return false;long now=p.server.overworld().getGameTime();var n=p.getPersistentData();
        return !n.contains(key)||now<n.getLong(key)||now-n.getLong(key)>=cooldown;
    }
    private static void paid(ServerPlayer p,String key){p.getPersistentData().putLong(key,p.server.overworld().getGameTime());p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.65F,1.4F);}
    private static boolean target(ServerPlayer p,LivingEntity victim){return victim.isAlive()&&victim instanceof Mob&&(victim instanceof Enemy||victim.getPersistentData().getBoolean("CoralineInfected"))&&!p.isAlliedTo(victim);}
    private static boolean hurt(ServerPlayer p,LivingEntity victim,float damage){
        return target(p,victim)&&!MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(p,victim))&&victim.hurt(p.damageSources().playerAttack(p),damage);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void air(PlayerInteractEvent.RightClickItem event){
        if(event.isCanceled()||event.getHand()!=InteractionHand.MAIN_HAND||!(event.getEntity() instanceof ServerPlayer p)||!PerfectPower.equipment(event.getItemStack())||!ready(p,LAST,PerfectPowerConfig.COOLDOWN.get()))return;
        var item=event.getItemStack().getItem();boolean activated=false;
        if(item instanceof SwordItem){
            var from=p.getEyePosition();var end=from.add(p.getLookAngle().scale(.75));
            if(p.serverLevel().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS&&PerfectPower.consume(p,1)){
                SLASHES.computeIfAbsent(p.server,s->new ArrayList<>()).add(new Slash(p));activated=true;
            }
        }else if(item instanceof AxeItem){
            var victims=p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(3),v->target(p,v)&&p.hasLineOfSight(v)&&v.position().subtract(p.position()).normalize().dot(p.getLookAngle())>.1);
            if(!victims.isEmpty()&&PerfectPower.consume(p,1)){
                for(var victim:victims)activated|=hurt(p,victim,10);
                if(!activated)refund(p);else p.serverLevel().sendParticles(GOLD,p.getX(),p.getY()+1,p.getZ(),24,1,.4,1,.1);
            }
        }
        if(activated){paid(p,LAST);event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);}
    }
    private static boolean eligible(ItemStack tool,net.minecraft.world.level.block.state.BlockState state){
        if(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock)return false;
        if(tool.getItem() instanceof PerfectExcavatorItem)return PerfectExcavatorItem.basic(state);
        return tool.getItem() instanceof PickaxeItem?state.is(BlockTags.MINEABLE_WITH_PICKAXE):tool.getItem() instanceof ShovelItem&&state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void block(PlayerInteractEvent.RightClickBlock event){
        if(event.isCanceled()||event.getUseBlock()==Event.Result.DENY||event.getUseItem()==Event.Result.DENY||event.getHand()!=InteractionHand.MAIN_HAND||!(event.getEntity() instanceof ServerPlayer p)||!p.isShiftKeyDown()||!PerfectPower.equipment(event.getItemStack())||!ready(p,LAST,PerfectPowerConfig.COOLDOWN.get()))return;
        var tool=p.getMainHandItem();var item=tool.getItem();if(!(item instanceof PickaxeItem||item instanceof ShovelItem||item instanceof HoeItem))return;
        var center=event.getPos();if(p.getEyePosition().distanceToSqr(center.getCenter())>36||p.serverLevel().getBlockEntity(center)!=null)return;
        var face=event.getFace();if(face==null)return;var level=p.serverLevel();boolean paid=false;MINING.set(true);
        try{
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
                if(p.getMainHandItem()!=tool||tool.isEmpty())break;
                var at=item instanceof HoeItem?center.offset(a,0,b):switch(face.getAxis()){case Y->center.offset(a,0,b);case X->center.offset(0,a,b);case Z->center.offset(a,b,0);};
                if(!level.hasChunkAt(at)||!level.mayInteract(p,at)||!p.mayUseItemAt(at,face,tool))continue;var state=level.getBlockState(at);
                boolean crop=item instanceof HoeItem&&state.getBlock() instanceof CropBlock plant&&plant.isMaxAge(state);
                if(!crop&&(!eligible(tool,state)||state.getDestroySpeed(level,at)<0||!tool.isCorrectToolForDrops(state)))continue;
                if(!paid&&!PerfectPower.powered(p))break;
                // Survival gameMode performs normal harvesting, Forge break
                // protection, drops and tool wear; never synthesize extra loot.
                boolean reserved=!paid;if(reserved&&!PerfectPower.consume(p,1))break;
                boolean destroyed=false;
                try{destroyed=p.gameMode.destroyBlock(at);}finally{if(reserved&&!destroyed)refund(p);}
                if(!destroyed)continue;
                if(!paid){paid=true;paid(p,LAST);}
                if(crop){
                    tool.hurtAndBreak(1,p,owner->owner.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                    var seed=state.getBlock().asItem();
                    if(level.getBlockState(at).isAir()&&level.getBlockState(at.below()).is(Blocks.FARMLAND))for(var stack:p.getInventory().items)if(stack.is(seed)){
                        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,at);
                        if(level.setBlock(at,((CropBlock)state.getBlock()).getStateForAge(0),3)){
                            if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(p,snapshot,Direction.UP))snapshot.restore(true,false);else stack.shrink(1);
                        }break;
                    }
                }
                level.sendParticles(GOLD,at.getX()+.5,at.getY()+.5,at.getZ()+.5,3,.2,.2,.2,.02);
            }
        }finally{MINING.set(false);}
        if(paid){event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);}
    }
    public static void barrier(ServerPlayer p){
        if(!ArmorSets.full(p,ArmorSets.Set.PERFECT)||!ready(p,BARRIER,PerfectPowerConfig.BARRIER_COOLDOWN.get())||!PerfectPower.consume(p,1))return;
        if(!p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,PerfectPowerConfig.BARRIER_DURATION.get(),1,false,true))){refund(p);return;}paid(p,BARRIER);
        p.serverLevel().sendParticles(GOLD,p.getX(),p.getY()+1,p.getZ(),40,.6,.9,.6,.05);
    }
    private static void refund(ServerPlayer p){p.getInventory().placeItemBackInInventory(new ItemStack(com.coraline.registry.ModItems.PERFECT_ESSENCE.get()));}
    static boolean extraToolReady(ServerPlayer p){return ready(p,LAST,PerfectPowerConfig.COOLDOWN.get());}
    static void extraToolPaid(ServerPlayer p){paid(p,LAST);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(var entry:SLASHES.entrySet())entry.getValue().removeIf(s->{
            if(!s.player.isAlive()||s.player.hasDisconnected()||s.player.level()!=s.level||++s.ticks>12)return true;
            var end=s.at.add(s.direction);var block=s.level.clip(new ClipContext(s.at,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.player));
            if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
            var box=new AABB(s.at,end).inflate(.4);var from=s.at;var to=end;
            for(var victim:s.level.getEntitiesOfClass(LivingEntity.class,box,v->target(s.player,v)&&!s.hit.contains(v.getUUID())))if(victim.getBoundingBox().inflate(.3).clip(from,to).isPresent()||victim.getBoundingBox().inflate(.3).contains(from)){s.hit.add(victim.getUUID());hurt(s.player,victim,9);}
            s.level.sendParticles(GOLD,end.x,end.y,end.z,6,.2,.2,.2,.02);s.at=end;return block.getType()!=HitResult.Type.MISS;
        });
    }
    private PerfectAbilities(){}
}
