package com.coraline.postgame;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Only the shrine's explicitly admitted temporary attackers. Natural hostile
 * mobs and story/NPC actors never enter this lifecycle or drop-suppression path. */
@Mod.EventBusSubscriber(modid="coraline")
public final class BoundaryRaiders {
    static final String KEY="CoralineBoundaryRaider";
    private static final Map<Mob,Goal> GOALS=new WeakHashMap<>();
    private static CompoundTag marker(Mob mob){if(mob.getType()!=PostgameHostiles.TYPES.get(PostgameHostiles.Kind.RIFT_SOLDIER).get())return null;var data=mob.getPersistentData();if(!data.contains(KEY,Tag.TAG_COMPOUND))return null;var n=data.getCompound(KEY);return n.contains("Version",Tag.TAG_INT)&&n.getInt("Version")==1&&n.hasUUID("Generation")&&n.hasUUID("Site")&&n.hasUUID("Session")&&n.contains("Beacon",Tag.TAG_LONG)?n:null;}
    private static BoundaryDefenseEntity root(Mob mob,CompoundTag marker){if(!(mob.level() instanceof ServerLevel level))return null;var at=net.minecraft.core.BlockPos.of(marker.getLong("Beacon"));return level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof BoundaryDefenseEntity root&&root.member(mob,marker)?root:null;}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(event.getLevel().isClientSide||!(event.getEntity() instanceof Mob mob)||marker(mob)==null||GOALS.containsKey(mob))return;var goal=new RaidGoal(mob);GOALS.put(mob,goal);((com.coraline.mixin.InfectedGoalAccess)mob).coraline$infectionGoals().addGoal(-1,goal);}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof Mob mob))return;var goal=GOALS.remove(mob);if(goal!=null)((com.coraline.mixin.InfectedGoalAccess)mob).coraline$infectionGoals().removeGoal(goal);
        var marker=marker(mob);if(marker!=null&&mob.getRemovalReason()==Entity.RemovalReason.KILLED&&mob.getHealth()<=0){var root=root(mob,marker);if(root!=null)root.killed(mob,marker);}
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)||mob.isDeadOrDying())return;var marker=marker(mob);if(marker==null)return;if(root(mob,marker)==null){level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,mob.getX(),mob.getY()+1,mob.getZ(),10,.3,.7,.3,.02);mob.discard();}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void drops(LivingDropsEvent event){if(event.getEntity() instanceof Mob mob&&marker(mob)!=null)event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void xp(LivingExperienceDropEvent event){if(event.getEntity() instanceof Mob mob&&marker(mob)!=null)event.setDroppedExperience(0);}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){GOALS.clear();}
    private static final class RaidGoal extends Goal {
        private final Mob mob;private int path,attack,beacon;
        RaidGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){var n=marker(mob);return n!=null&&root(mob,n)!=null;}
        @Override public void tick(){var n=marker(mob);if(n==null)return;var root=root(mob,n);if(root==null)return;var at=root.getBlockPos();var player=mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(3),p->p.isAlive()&&(p instanceof Player user&&!user.isCreative()&&!user.isSpectator()||p instanceof OtherworldResident ally&&ally.role()==3&&ally.authentic())).stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            if(player!=null&&mob.hasLineOfSight(player)){mob.getLookControl().setLookAt(player,30,30);if(mob.tickCount>=attack&&mob.distanceToSqr(player)<7){attack=mob.tickCount+30;mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);mob.doHurtTarget(player);}}
            else mob.getLookControl().setLookAt(at.getX()+.5,at.getY()+.5,at.getZ()+.5);
            if(mob.tickCount>=path){path=mob.tickCount+10;mob.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()-.5,1.05);}
            if(mob.tickCount>=beacon&&mob.distanceToSqr(at.getCenter())<5){var hit=mob.level().clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),at.getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));if(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(at)){beacon=mob.tickCount+60;root.hit();}}
        }
        @Override public void stop(){mob.getNavigation().stop();}
    }
    private BoundaryRaiders(){}
}
