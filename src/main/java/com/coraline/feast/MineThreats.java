package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Nine capped, owner-only actors; no vanilla attacks or destructive explosions. */
final class MineThreats {
    private static final int[] CAPS={2,3,4,6}, INTERVALS={360,280,200,140};
    static int limit(int depth){return CAPS[Math.max(0,Math.min(3,depth))];}
    static int spawnInterval(int depth){return INTERVALS[Math.max(0,Math.min(3,depth))];}
    static final int[][] POOLS={{0,1},{0,1,2,4},{1,2,4,5,6,7},{2,3,5,6,7,8}};
    static final EntityType<?>[] TYPES={EntityType.ZOMBIE,EntityType.SPIDER,EntityType.SKELETON,EntityType.ENDERMAN,EntityType.HUSK,EntityType.STRAY,EntityType.CAVE_SPIDER,EntityType.CREEPER,EntityType.WITHER_SKELETON};
    static final String[] NAMES={"Stitched Miner","Thread Skitter","Shard Watcher","Deep Stalker","Tunnel Charger","Frost Archer","Web Pouncer","Fuse Phantom","Vault Reaper"};
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        if(p.tickCount%5!=0)return;
        var n=ExpeditionActivities.state(s);var l=p.serverLevel();int depth=Math.max(0,Math.min(3,(c.getY()-p.blockPosition().getY()+5)/14));
        var actors=l.getEntitiesOfClass(Mob.class,ExpeditionActivities.bounds(c,s.loop.activity),e->ActivityScenery.owned(e,s)&&e.getPersistentData().getBoolean("MineEnemy"));
        // Returning upstairs must also reduce an already accumulated deep crowd.
        actors.sort(java.util.Comparator.comparingDouble(e->e.distanceToSqr(p)));
        while(actors.size()>limit(depth))actors.remove(actors.size()-1).discard();
        boolean safe=p.distanceToSqr(Vec3.atBottomCenterOf(c))<225;
        if(!safe&&actors.size()<limit(depth)&&now>=n.getLong("NextSpawn")){
            n.putLong("NextSpawn",now+spawnInterval(depth));
            for(int attempt=0;attempt<30;attempt++){
                int dx=p.getRandom().nextInt(25)-12,dz=p.getRandom().nextInt(25)-12;if(dx*dx+dz*dz<49)continue;
                var at=p.blockPosition().offset(dx,0,dz);int[] pool=POOLS[depth];int kind=pool[p.getRandom().nextInt(pool.length)];
                if(!safeSpot(p,at,kind==3||kind==8?3:2))continue;
                var mob=(Mob)TYPES[kind].create(l);if(mob==null)break;
                mob.goalSelector.removeAllGoals(g->true);mob.targetSelector.removeAllGoals(g->true);
                mob.setPersistenceRequired();mob.setSilent(true);ActivityScenery.mark(mob,s);
                var d=mob.getPersistentData();d.putBoolean("MineEnemy",true);d.putInt("Generation",s.loop.generation);d.putInt("MineKind",kind);d.putLong("Born",now);
                mob.setCustomName(Component.literal("Button-Eyed "+NAMES[kind]));
                if(kind==2||kind==5)mob.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
                if(kind==8)mob.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
                mob.setPos(Vec3.atBottomCenterOf(at));l.addFreshEntity(mob);
                p.playNotifySound(kind==1||kind==6?SoundEvents.SPIDER_AMBIENT:kind==2||kind==5||kind==8?SoundEvents.SKELETON_AMBIENT:kind==3?SoundEvents.ENDERMAN_AMBIENT:SoundEvents.ZOMBIE_AMBIENT,SoundSource.HOSTILE,.35F,.8F);break;
            }
        }
        for(var mob:actors){
            var d=mob.getPersistentData();int kind=d.getInt("MineKind");double distance=mob.distanceToSqr(p);
            if(distance>1600||safe||mob.getY()<c.getY()-50||now-d.getLong("Born")>1600){mob.discard();continue;}
            mob.setTarget(null);mob.setRemainingFireTicks(0);mob.getLookControl().setLookAt(p,30,30);
            boolean lamp=p.getMainHandItem().is(com.coraline.clothing.UtilityItems.MOONLIT_LANTERN.get());
            if(lamp&&distance<16&&now>=d.getLong("RepelAt")){
                d.putLong("RepelAt",now+100);d.putLong("Stunned",now+25);d.remove("AttackAt");
                mob.setDeltaMovement(mob.position().subtract(p.position()).normalize().scale(.5).add(0,.15,0));
                l.sendParticles(ParticleTypes.END_ROD,mob.getX(),mob.getY()+1,mob.getZ(),4,.2,.2,.2,.01);
            }
            if(now<d.getLong("Stunned")){mob.getNavigation().stop();continue;}
            boolean ranged=kind==2||kind==5;
            if(p.tickCount%20==0){
                mob.goalSelector.removeAllGoals(g->true);mob.targetSelector.removeAllGoals(g->true);
                if(ranged&&distance<121&&mob.hasLineOfSight(p)){
                    mob.getNavigation().stop();if(distance<16){var back=mob.position().add(mob.position().subtract(p.position()).normalize().scale(3));mob.getNavigation().moveTo(back.x,back.y,back.z,1.2);}
                }else if(!d.contains("AttackAt"))mob.getNavigation().moveTo(p.getX(),p.getY(),p.getZ(),kind==1||kind==6?1.5:kind==3?1.3:1.05);
            }
            if(ranged&&distance<196&&mob.hasLineOfSight(p)){
                if(!d.contains("AttackAt")&&now>=d.getLong("ReadyAt")){
                    d.putLong("AttackAt",now+25);d.putDouble("AimX",p.getX());d.putDouble("AimY",p.getY()+.9);d.putDouble("AimZ",p.getZ());
                    p.playNotifySound(SoundEvents.CROSSBOW_LOADING_START,SoundSource.HOSTILE,.45F,1.4F);
                }
                if(d.contains("AttackAt")&&now>=d.getLong("AttackAt")){
                    var start=mob.getEyePosition();var aim=new Vec3(d.getDouble("AimX"),d.getDouble("AimY"),d.getDouble("AimZ"));
                    var wall=l.clip(new ClipContext(start,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mob));var end=wall.getType()==HitResult.Type.MISS?aim:wall.getLocation();
                    for(int i=0;i<20;i++){var q=start.lerp(end,i/19.0);l.sendParticles(kind==5?ParticleTypes.SNOWFLAKE:ParticleTypes.CRIT,q.x,q.y,q.z,1,0,0,0,0);}
                    p.playNotifySound(SoundEvents.SKELETON_SHOOT,SoundSource.HOSTILE,.6F,1);
                    if(p.getBoundingBox().inflate(.12).clip(start,end).isPresent()){hit(p,s,now,1);if(kind==5)n.putLong("ChilledUntil",now+35);}
                    d.remove("AttackAt");d.putLong("ReadyAt",now+(kind==5?65:90));
                }
            }else if((kind==1||kind==6)&&distance>4&&distance<49&&mob.onGround()&&now>=d.getLong("ReadyAt")){
                d.putLong("ReadyAt",now+65);mob.setDeltaMovement(p.position().subtract(mob.position()).normalize().scale(kind==6?.8:.65).add(0,.35,0));
                p.playNotifySound(SoundEvents.SPIDER_STEP,SoundSource.HOSTILE,.6F,.7F);
            }else if(kind==3&&distance>36&&distance<324&&now>=d.getLong("ReadyAt")){
                d.putLong("ReadyAt",now+120);
                for(int i=0;i<12;i++){var at=p.blockPosition().offset(p.getRandom().nextInt(9)-4,0,p.getRandom().nextInt(9)-4);if(at.distSqr(p.blockPosition())<9||!safeSpot(p,at,3))continue;
                    mob.setPos(Vec3.atBottomCenterOf(at));d.putLong("Stunned",now+20);l.sendParticles(ParticleTypes.PORTAL,mob.getX(),mob.getY()+1,mob.getZ(),20,.3,.8,.3,.02);p.playNotifySound(SoundEvents.ENDERMAN_TELEPORT,SoundSource.HOSTILE,.6F,.8F);break;}
            }else if((kind==4||kind==7||kind==8)&&distance<(kind==4?49:16)&&mob.hasLineOfSight(p)){
                if(!d.contains("AttackAt")&&now>=d.getLong("ReadyAt")){
                    d.putLong("AttackAt",now+(kind==7?35:25));d.putDouble("AimX",p.getX());d.putDouble("AimZ",p.getZ());
                    p.playNotifySound(kind==7?SoundEvents.CREEPER_PRIMED:SoundEvents.IRON_GOLEM_ATTACK,SoundSource.HOSTILE,.6F,.8F);
                }
                if(d.contains("AttackAt")){
                    mob.getNavigation().stop();l.sendParticles(kind==7?ParticleTypes.FLAME:ParticleTypes.ANGRY_VILLAGER,mob.getX(),mob.getY()+1,mob.getZ(),3,.3,.3,.3,0);
                    if(now>=d.getLong("AttackAt")){
                        d.remove("AttackAt");d.putLong("ReadyAt",now+90);
                        if(kind==4)mob.setDeltaMovement(new Vec3(d.getDouble("AimX")-mob.getX(),0,d.getDouble("AimZ")-mob.getZ()).normalize().scale(.95).add(0,.1,0));
                        else{if(distance<(kind==7?9:6)&&mob.hasLineOfSight(p))hit(p,s,now,kind==8?2:1);
                            if(kind==7){l.sendParticles(ParticleTypes.EXPLOSION,mob.getX(),mob.getY()+.5,mob.getZ(),1,0,0,0,0);p.playNotifySound(SoundEvents.GENERIC_EXPLODE,SoundSource.HOSTILE,.6F,1.3F);mob.discard();}}
                    }
                }
            }
            if(!ranged&&kind!=7&&kind!=8&&distance<2.3&&mob.hasLineOfSight(p)){hit(p,s,now,1);if(kind==6)n.putLong("ChilledUntil",now+25);}
            if(n.getInt("Phase")!=1)return;
        }
        if(now<n.getLong("ChilledUntil")){var v=p.getDeltaMovement();p.setDeltaMovement(v.x*.6,v.y,v.z*.6);p.hurtMarked=true;l.sendParticles(ParticleTypes.SNOWFLAKE,p.getX(),p.getY()+.2,p.getZ(),3,.25,.15,.25,0);}
    }
    private static boolean safeSpot(ServerPlayer p,BlockPos at,int height){var l=p.serverLevel();if(!l.getBlockState(at.below()).isFaceSturdy(l,at.below(),net.minecraft.core.Direction.UP))return false;for(int y=0;y<height;y++)if(!l.isEmptyBlock(at.above(y)))return false;return true;}
    private static void hit(ServerPlayer p,VisitData.Scene s,long now,int damage){var n=ExpeditionActivities.state(s);if(now<n.getLong("MineHitAt"))return;n.putLong("MineHitAt",now+35);n.putInt("Hearts",Math.max(0,n.getInt("Hearts")-damage));OtherMotherLoop.changed(p);ExpeditionNetwork.mine(p,n.getInt("Hearts"));p.playNotifySound(SoundEvents.PLAYER_HURT,SoundSource.PLAYERS,.7F,.8F);if(n.getInt("Hearts")==0)MineshaftActivity.fail(p,s,"All five hearts lost.");}
    private MineThreats(){}
}
