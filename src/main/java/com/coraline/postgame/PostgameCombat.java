package com.coraline.postgame;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Bounded native movement/attacks for the remaining artificial creatures.
 * These do not impersonate, replace or mutate original story actors. */
final class PostgameCombat {
    private final OtherWorldHostile mob;
    private final net.minecraft.server.level.ServerBossEvent boss;
    private int quakeAt=-1,nextQuake;
    PostgameCombat(OtherWorldHostile mob){this.mob=mob;boss=mob.kind()==PostgameHostiles.Kind.RIFT_GUARDIAN?new ServerBossEvent(mob.getDisplayName(),net.minecraft.world.BossEvent.BossBarColor.PURPLE,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS):null;}
    void startTracking(ServerPlayer p){if(boss!=null)boss.addPlayer(p);}
    void stopTracking(ServerPlayer p){if(boss!=null)boss.removePlayer(p);}
    void removed(){if(boss!=null)boss.removeAllPlayers();}
    void tick(){
        var target=mob.getTarget();
        switch(mob.kind()){
            case ROOTBOUND_HORROR->{
                if(target!=null||mob.getLastHurtByMob()!=null)mob.reveal();
                mob.setInvisible(!mob.revealed());if(!mob.revealed())mob.getNavigation().stop();
            }
            case FALSE_VILLAGER->{if(target!=null||mob.getLastHurtByMob()!=null)mob.reveal();}
            case PAPER_WRAITH->mob.setNoGravity(true);
            case RIFT_GUARDIAN->{
                boss.setProgress(Math.max(0,mob.getHealth()/mob.getMaxHealth()));boss.setVisible(mob.isAlive());
                if(quakeAt<0&&mob.tickCount>=nextQuake&&target!=null&&mob.distanceToSqr(target)<=49&&mob.hasLineOfSight(target)&&mob.onGround()){
                    quakeAt=mob.tickCount+24;mob.charging(true);mob.level().playSound(null,mob.blockPosition(),net.minecraft.sounds.SoundEvents.IRON_GOLEM_REPAIR,net.minecraft.sounds.SoundSource.HOSTILE,1,.55F);
                }
                if(quakeAt>=0){
                    mob.getNavigation().stop();
                    if(mob.tickCount>=quakeAt){
                        quakeAt=-1;nextQuake=mob.tickCount+120;mob.charging(false);
                        for(var victim:mob.level().getEntitiesOfClass(Player.class,mob.getBoundingBox().inflate(4),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.onGround()&&mob.hasLineOfSight(p)))if(victim.hurt(mob.damageSources().mobAttack(mob),7))victim.knockback(.7,mob.getX()-victim.getX(),mob.getZ()-victim.getZ());
                        ((ServerLevel)mob.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,mob.getX(),mob.getY()+.2,mob.getZ(),35,2,.1,2,.025);
                    }
                }
            }
            default->{}
        }
    }
    static final class FlightGoal extends Goal {
        private final OtherWorldHostile mob;private int nextAttack,nextPoint;private Vec3 destination;
        FlightGoal(OtherWorldHostile mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return true;}
        @Override public void tick(){
            var target=mob.getTarget();
            if(target!=null&&target.isAlive()){
                destination=target.getEyePosition().add(0,.3+Math.sin(mob.tickCount*.06),0);mob.getLookControl().setLookAt(target,30,30);
                if(mob.distanceToSqr(target)<5&&mob.hasLineOfSight(target)&&mob.tickCount>=nextAttack){nextAttack=mob.tickCount+40;mob.doHurtTarget(target);}
            }else if(destination==null||mob.tickCount>=nextPoint||mob.position().distanceToSqr(destination)<2){
                nextPoint=mob.tickCount+60;destination=mob.position().add((mob.getRandom().nextDouble()-.5)*10,(mob.getRandom().nextDouble()-.4)*4,(mob.getRandom().nextDouble()-.5)*10);
            }
            if(mob.horizontalCollision)destination=mob.position().add((mob.getRandom().nextDouble()-.5)*5,2,(mob.getRandom().nextDouble()-.5)*5);
            if(destination.y<mob.level().getMinBuildHeight()+2||destination.y>mob.level().getMaxBuildHeight()-2)destination=mob.position().add(0,destination.y<mob.getY()?2:-2,0);
            mob.getMoveControl().setWantedPosition(destination.x,destination.y,destination.z,.85);
        }
    }
    static final class ThreadGoal extends Goal {
        private final OtherWorldHostile mob;private int next;
        ThreadGoal(OtherWorldHostile mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){var target=mob.getTarget();return target!=null&&target.isAlive()&&mob.distanceToSqr(target)>16;}
        @Override public void tick(){
            var target=mob.getTarget();if(target==null)return;mob.getLookControl().setLookAt(target,30,30);
            if(mob.distanceToSqr(target)>100||!mob.hasLineOfSight(target)){mob.getNavigation().moveTo(target,1);return;}
            mob.getNavigation().stop();if(mob.tickCount<next)return;next=mob.tickCount+80;
            if(target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,80,1),mob)){
                var from=mob.getEyePosition();var delta=target.getEyePosition().subtract(from);
                var dust=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.85F,.8F,.94F),.7F);
                for(int i=1;i<=12;i++){var point=from.add(delta.scale(i/12.));((ServerLevel)mob.level()).sendParticles(dust,point.x,point.y,point.z,1,0,0,0,0);}
                mob.level().playSound(null,mob.blockPosition(),net.minecraft.sounds.SoundEvents.SPIDER_STEP,net.minecraft.sounds.SoundSource.HOSTILE,.7F,.6F);
            }
        }
    }
}
