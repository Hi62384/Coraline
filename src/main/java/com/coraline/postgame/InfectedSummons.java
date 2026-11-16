package com.coraline.postgame;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The Avatar's finite temporary infected reinforcements. Their native UUID,
 * drops and species remain real; no player-owned/named animal is substituted. */
@Mod.EventBusSubscriber(modid="coraline")
public final class InfectedSummons {
    private static final String KEY="CoralineAvatarInfection";
    private static final Map<Mob,Goal> GOALS=new WeakHashMap<>();
    static boolean configure(Mob mob,WhiteVoidAvatarEntity avatar){
        if(mob.hasCustomName()||!avatar.authentic())return false;
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Entity",mob.getUUID());n.putUUID("Avatar",avatar.getUUID());n.putUUID("Generation",avatar.generation());n.putLong("Born",avatar.getServer().overworld().getGameTime());
        String name="Button-Eyed "+mob.getType().getDescription().getString();n.putString("Name",name);mob.getPersistentData().put(KEY,n);mob.getPersistentData().putBoolean("CoralineInfected",true);mob.setCustomName(net.minecraft.network.chat.Component.literal(name));mob.setCustomNameVisible(false);mob.setPersistenceRequired();install(mob);return true;
    }
    private static CompoundTag marker(LivingEntity mob){
        var root=mob.getPersistentData();if(!root.contains(KEY,Tag.TAG_COMPOUND))return null;var n=root.getCompound(KEY);
        return n.getInt("Version")==1&&n.hasUUID("Entity")&&n.getUUID("Entity").equals(mob.getUUID())&&n.hasUUID("Avatar")&&n.hasUUID("Generation")&&n.contains("Born",Tag.TAG_LONG)?n:null;
    }
    public static boolean active(LivingEntity mob){var n=marker(mob);return n!=null&&mob.level() instanceof ServerLevel l&&BreachWorldData.get(l.getServer()).ownsSummon(mob.getUUID(),n.getUUID("Avatar"),n.getUUID("Generation"));}
    private static void install(Mob mob){if(GOALS.containsKey(mob))return;var goal=new SwarmGoal(mob);GOALS.put(mob,goal);((com.coraline.mixin.InfectedGoalAccess)mob).coraline$infectionGoals().addGoal(-1,goal);}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof Mob mob&&marker(mob)!=null)install(mob);}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof Mob mob))return;var goal=GOALS.remove(mob);if(goal!=null)((com.coraline.mixin.InfectedGoalAccess)mob).coraline$infectionGoals().removeGoal(goal);
        var n=marker(mob);if(n!=null&&(mob.getRemovalReason()==Entity.RemovalReason.KILLED||mob.getRemovalReason()==Entity.RemovalReason.DISCARDED))BreachWorldData.get(((ServerLevel)event.getLevel()).getServer()).releaseSummon(mob.getUUID(),n.getUUID("Avatar"));
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level))return;var n=marker(mob);if(n==null)return;
        var data=BreachWorldData.get(level.getServer());if(!active(mob)||level.getServer().overworld().getGameTime()-n.getLong("Born")>2400){
            data.releaseSummon(mob.getUUID(),n.getUUID("Avatar"));level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,mob.getX(),mob.getY()+.8,mob.getZ(),12,.3,.6,.3,.02);mob.discard();return;
        }
        if(mob.tickCount%20==0)level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.3F,.65F,.55F),.6F),mob.getX(),mob.getY()+1,mob.getZ(),2,.3,.5,.3,.01);
    }
    private static final class SwarmGoal extends Goal {
        private final Mob mob;private Player target;private int attack,path;
        SwarmGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){if(!active(mob))return false;target=mob.level().getNearestPlayer(mob,32);return target!=null&&!target.isCreative()&&!target.isSpectator();}
        @Override public boolean canContinueToUse(){return active(mob)&&target!=null&&target.isAlive()&&!target.isCreative()&&!target.isSpectator()&&mob.distanceToSqr(target)<48*48;}
        @Override public void tick(){
            mob.getLookControl().setLookAt(target,30,30);if(mob.tickCount>=path){path=mob.tickCount+10;
                var delta=target.position().subtract(mob.position()).multiply(1,0,1);var side=new net.minecraft.world.phys.Vec3(-delta.z,0,delta.x).normalize().scale(mob.getId()%2==0?2:-2);var to=target.position().add(mob.distanceToSqr(target)>16?side:net.minecraft.world.phys.Vec3.ZERO);
                if(!mob.getNavigation().moveTo(to.x,to.y,to.z,1.15))mob.getNavigation().moveTo(target,1.15);
            }
            if(mob.tickCount>=attack&&mob.distanceToSqr(target)<4&&mob.hasLineOfSight(target)){attack=mob.tickCount+30;mob.doHurtTarget(target);}
        }
        @Override public void stop(){mob.getNavigation().stop();target=null;}
    }
    private InfectedSummons(){}
}
