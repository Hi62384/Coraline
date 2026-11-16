package com.coraline.postgame;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Conversion keeps the actual vanilla entity, UUID, species, health, coat,
 * wool, equipment and ownership properties. Only this marker/goal is added. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ColonyInfectedMobs {
    private static final String KEY="CoralineColonyInfection";
    private static final Map<Mob,Goal> GOALS=new WeakHashMap<>();
    private static final Set<EntityType<?>> SUPPORTED=Set.of(EntityType.COW,EntityType.SHEEP,EntityType.PIG,EntityType.WOLF,EntityType.ZOMBIE,EntityType.SKELETON);
    private static CompoundTag marker(Entity actor){if(!(actor instanceof Mob)||!SUPPORTED.contains(actor.getType())||!actor.getPersistentData().contains(KEY,Tag.TAG_COMPOUND))return null;var n=actor.getPersistentData().getCompound(KEY);return n.contains("Version",Tag.TAG_INT)&&n.getInt("Version")==1&&n.hasUUID("Generation")&&n.hasUUID("Colony")&&n.hasUUID("Entity")&&n.getUUID("Entity").equals(actor.getUUID())&&n.contains("Type",Tag.TAG_STRING)&&n.getString("Type").equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(actor.getType()).toString())?n:null;}
    public static boolean active(Entity actor){if(actor.getServer()!=null&&TrueHeartBattle.resetting(actor.getServer()))return false;var n=marker(actor);return n!=null&&!protectedMob((Mob)actor)&&((Mob)actor).getPersistentData().getAllKeys().stream().allMatch(KEY::equals)&&actor.level() instanceof ServerLevel level&&InfiniteWorldData.get(level.getServer()).matchesGeneration(n.getUUID("Generation"))&&InfectionColonies.ownsMob(level,n.getUUID("Colony"),actor.getUUID());}
    private static boolean protectedMob(Mob mob){return mob.hasCustomName()||mob.isBaby()||mob.isPersistenceRequired()||mob.isLeashed()||mob.isVehicle()||mob instanceof TamableAnimal tame&&(tame.isTame()||tame.getOwnerUUID()!=null)||mob instanceof Pig pig&&pig.isSaddled()||mob instanceof NeutralMob neutral&&neutral.getRemainingPersistentAngerTime()>0;}
    private static boolean eligible(Mob mob){return SUPPORTED.contains(mob.getType())&&mob.isAlive()&&!protectedMob(mob)&&mob.getPersistentData().isEmpty();}
    private static void install(Mob mob){if(GOALS.containsKey(mob))return;var goal=new Swarm(mob);GOALS.put(mob,goal);((com.coraline.mixin.InfectedGoalAccess)mob).coraline$infectionGoals().addGoal(-1,goal);}
    private static void cure(Mob mob){var goal=GOALS.remove(mob);if(goal!=null)((com.coraline.mixin.InfectedGoalAccess)mob).coraline$infectionGoals().removeGoal(goal);mob.getNavigation().stop();mob.setTarget(null);mob.getPersistentData().remove(KEY);ColonyMobNetwork.send(mob,false);}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof Mob mob)||marker(mob)==null)return;Runnable action=()->{if(active(mob))install(mob);else cure(mob);};if(level.getServer().isSameThread())action.run();else level.getServer().execute(action);}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){if(event.getLevel().isClientSide||!(event.getEntity() instanceof Mob mob))return;var goal=GOALS.remove(mob);if(goal!=null)((com.coraline.mixin.InfectedGoalAccess)mob).coraline$infectionGoals().removeGoal(goal);}
    @SubscribeEvent public static void living(LivingEvent.LivingTickEvent event){if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)||mob.tickCount%20!=0||marker(mob)==null||mob.isDeadOrDying())return;if(!active(mob)||protectedMob(mob)||mob.getPersistentData().getAllKeys().stream().anyMatch(k->!k.equals(KEY))){cure(mob);return;}install(mob);level.sendParticles(net.minecraft.core.particles.ParticleTypes.WARPED_SPORE,mob.getX(),mob.getY()+.8,mob.getZ(),3,.3,.4,.3,0);if(mob.tickCount%100==0)ColonyMobNetwork.send(mob,true);}
    @SubscribeEvent public static void convert(TickEvent.ServerTickEvent event){var server=event.getServer();if(event.phase!=TickEvent.Phase.END||server.getTickCount()%100!=0)return;var view=BreachWorldData.get(server).view();if(TrueHeartBattle.resetting(server)||!view.active()||!view.blocker().isEmpty()||view.millionths()<1000000)return;if(view.millionths()<1500000&&server.getTickCount()%200!=0)return;var epoch=InfiniteWorldData.get(server).generation();if(epoch.isEmpty())return;var level=server.overworld();for(var source:InfectionColonies.sources(level)){int examined=0,admitted=0;for(var mob:level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(source.heart()).inflate(12))){if(examined++>=16||admitted>=2)break;if(!eligible(mob)||!InfectionColonies.admitMob(level,source.id(),mob))continue;var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Generation",epoch.orElseThrow());n.putUUID("Colony",source.id());n.putUUID("Entity",mob.getUUID());n.putString("Type",net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString());mob.getPersistentData().put(KEY,n);install(mob);ColonyMobNetwork.send(mob,true);admitted++;}}}
    @SubscribeEvent public static void tracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p&&event.getTarget() instanceof Mob mob&&SUPPORTED.contains(mob.getType()))ColonyMobNetwork.send(p,mob,active(mob));}
    private static final class Swarm extends Goal {private final Mob mob;private Player target;private int path,hit;Swarm(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}@Override public boolean canUse(){if(!active(mob))return false;target=mob.level().getNearestPlayer(mob,24);return target!=null&&!target.isCreative()&&!target.isSpectator();}@Override public boolean canContinueToUse(){return active(mob)&&target!=null&&target.isAlive()&&!target.isCreative()&&!target.isSpectator()&&mob.distanceToSqr(target)<32*32;}@Override public void tick(){if(mob instanceof net.minecraft.world.entity.monster.Monster)mob.setTarget(target);mob.getLookControl().setLookAt(target,30,30);if(mob.tickCount>=path){path=mob.tickCount+10;mob.getNavigation().moveTo(target,BreachWorldData.get(mob.getServer()).view().millionths()>=1500000?1.25:1.1);}if(mob.tickCount>=hit&&mob.distanceToSqr(target)<5&&mob.hasLineOfSight(target)){hit=mob.tickCount+25;mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);target.hurt(mob.damageSources().mobAttack(mob),(mob instanceof Wolf?4:3)+(BreachWorldData.get(mob.getServer()).view().millionths()>=1500000?1:0));}}@Override public void stop(){mob.getNavigation().stop();mob.setTarget(null);}}
    private ColonyInfectedMobs(){}
}
