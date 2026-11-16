package com.coraline.feast;

import com.coraline.mixin.ActivityGuardianAccess;
import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** All progression lives in the existing family activity ledger. Guardians never deal damage. */
@Mod.EventBusSubscriber(modid="coraline")
public final class OceanMonumentActivity {
    static final String OWNER="CoralineOceanOwner";
    static final int CHARGE_TICKS=80;
    private static final String AIR_BACKUP="CoralineOceanBreathing";
    // 0 speech, 1 swimming, 2 treasure available, 3 failed, 4 claimed.
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(1880);}
    static CompoundTag state(VisitData.Scene s){return s.loop.ocean;}
    static boolean exitReady(VisitData.Scene s){return state(s).getInt("Phase")>=3;}
    static BlockPos exitPos(BlockPos c,VisitData.Scene s){return state(s).getInt("Phase")==4?c.offset(0,8,-287):c.south(5);}
    static boolean owns(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY&&s.loop.activity==OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT&&ActivityWorld.isActivity(p.level());}
    private static boolean owned(Entity e,VisitData.Scene s){return e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER));}
    private static void mark(Entity e,VisitData.Scene s){e.getPersistentData().putUUID(OWNER,s.owner);e.getPersistentData().putBoolean("CoralineDecoration",true);e.setInvulnerable(true);}
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        var c=center(p,s);clean(l,s,c);s.loop.ocean=new CompoundTag();
        state(s).putLong("VoiceUntil",OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(s.loop.activity)));
        prepare(p,s,l,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        LoopProps.hold(l,c);OceanMonumentCourse.build(l,c);
        if(l.getEntitiesOfClass(ParentEntity.class,new AABB(c).inflate(12),e->owned(e,s)).isEmpty()){
            var realm=RealmData.get(p.server).realms.get(s.door);var dad=ModEntities.DAD.get().create(l);
            if(realm!=null&&dad!=null){var f=ParentAccess.family(p.server,s.owner);dad.makeOtherParent(f==null?0:Math.max(0,f.dadSkin));
                dad.setNoAi(true);mark(dad,s);dad.getPersistentData().putUUID("RealmDoor",s.door);dad.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
                dad.setPos(c.getX()+2.5,c.getY(),c.getZ()+.5);dad.setYRot(90);dad.setYBodyRot(90);dad.setYHeadRot(90);l.addFreshEntity(dad);}
        }
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){
        for(var e:l.getEntitiesOfClass(Entity.class,OceanMonumentCourse.bounds(c).inflate(3),e->owned(e,s)||e instanceof ScenePropEntity prop&&s.owner.equals(prop.owner)&&s.door.equals(prop.storyDoor)))e.discard();
    }
    static void removeBuild(ServerLevel l,BlockPos c){OceanMonumentCourse.remove(l,c);}
    private static void stopGuardians(ServerLevel l,VisitData.Scene s,BlockPos c){
        for(var g:l.getEntitiesOfClass(Guardian.class,OceanMonumentCourse.bounds(c),e->owned(e,s))){beam(g,0);g.discard();}
    }
    static void restoreBreathing(ServerPlayer p,VisitData.Scene s){
        if(p.getPersistentData().contains(AIR_BACKUP)){restoreNativeBreathing(p);return;}if(!p.getUUID().equals(s.owner))return;
        var n=state(s);if(!n.getBoolean("Breathing"))return;
        p.removeEffect(MobEffects.WATER_BREATHING);n.putBoolean("Breathing",false);
        if(n.contains("OldBreathing")){
            var previous=MobEffectInstance.load(n.getCompound("OldBreathing"));
            if(previous!=null){int remaining=previous.isInfiniteDuration()?-1:previous.getDuration()-(int)Math.max(0,OtherMotherLoop.now(p)-n.getLong("BreathingAt"));
                if(remaining==-1||remaining>0)p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,remaining,previous.getAmplifier(),previous.isAmbient(),previous.isVisible(),previous.showIcon()));}
            n.remove("OldBreathing");
        }
        p.setAirSupply(p.getMaxAirSupply());
    }
    public static void restoreNativeBreathing(ServerPlayer p){
        var backup=p.getPersistentData().getCompound(AIR_BACKUP);if(!backup.hasUUID("Player")||!backup.getUUID("Player").equals(p.getUUID())||!backup.contains("StartedAt",net.minecraft.nbt.Tag.TAG_LONG))return;
        var current=p.getEffect(MobEffects.WATER_BREATHING);if(current!=null&&current.getAmplifier()==0&&!current.isInfiniteDuration()&&current.getDuration()<=220&&!current.isVisible()&&!current.showIcon())p.removeEffect(MobEffects.WATER_BREATHING);
        if(backup.contains("OldBreathing")){var old=MobEffectInstance.load(backup.getCompound("OldBreathing"));if(old!=null){int remaining=old.isInfiniteDuration()?-1:old.getDuration()-(int)Math.min(Integer.MAX_VALUE,Math.max(0,OtherMotherLoop.now(p)-backup.getLong("StartedAt")));var kept=p.getEffect(MobEffects.WATER_BREATHING);if((remaining==-1||remaining>0)&&(kept==null||kept.getAmplifier()<old.getAmplifier()||kept.getAmplifier()==old.getAmplifier()&&!kept.isInfiniteDuration()&&(remaining==-1||kept.getDuration()<remaining)))p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,remaining,old.getAmplifier(),old.isAmbient(),old.isVisible(),old.showIcon()));}}
        p.getPersistentData().remove(AIR_BACKUP);
    }
    @SubscribeEvent public static void recoverAir(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0||!p.getPersistentData().contains(AIR_BACKUP))return;var scene=OtherMotherLoop.active(p);if(scene==null||scene.loop.activity!=OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT||state(scene).getInt("Phase")!=1||!ActivityWorld.isActivity(p.level()))restoreNativeBreathing(p);
    }
    private static void breathing(ServerPlayer p,VisitData.Scene s){
        if(com.coraline.multiplayer.StorySessionData.get(p.server).groupActivity(s)){
            if(!p.getPersistentData().contains(AIR_BACKUP)){var backup=new CompoundTag();backup.putUUID("Player",p.getUUID());backup.putUUID("Owner",s.owner);backup.putUUID("Door",s.door);backup.putInt("Generation",s.loop.generation);backup.putLong("StartedAt",OtherMotherLoop.now(p));var old=p.getEffect(MobEffects.WATER_BREATHING);if(old!=null)backup.put("OldBreathing",old.save(new CompoundTag()));p.getPersistentData().put(AIR_BACKUP,backup);}
            if(p.tickCount%100==0||!p.hasEffect(MobEffects.WATER_BREATHING))p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,220,0,false,false,false));p.setAirSupply(p.getMaxAirSupply());return;
        }
        var n=state(s);if(!n.getBoolean("Breathing")){
            var old=p.getEffect(MobEffects.WATER_BREATHING);if(old!=null)n.put("OldBreathing",old.save(new CompoundTag()));
            n.putBoolean("Breathing",true);n.putLong("BreathingAt",OtherMotherLoop.now(p));
        }
        if(p.tickCount%100==0||!p.hasEffect(MobEffects.WATER_BREATHING))p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,220,0,false,false,false));
        p.setAirSupply(p.getMaxAirSupply());
    }
    private static void title(ServerPlayer p,String main,String sub){
        p.connection.send(new ClientboundSetTitlesAnimationPacket(8,55,15));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(main)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
    }
    private static void move(ServerPlayer p,BlockPos c,double x,double y,double z){p.stopRiding();p.teleportTo(p.serverLevel(),c.getX()+x,c.getY()+y,c.getZ()+z,p.getYRot(),p.getXRot());p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(!owns(p,s))return;
        var n=state(s);int phase=n.getInt("Phase"),section=n.getInt("Section");var l=p.serverLevel();
        if(phase==0){
            if(p.distanceToSqr(Vec3.atBottomCenterOf(c))>64)move(p,c,.5,0,.5);
            if(OtherMotherLoop.now(p)<n.getLong("VoiceUntil"))return;
            n.putInt("Phase",1);n.putLong("Started",OtherMotherLoop.now(p));n.putInt("Section",0);
            for(var member:CooperativeActivities.playing(p,s))breathing(member,s);move(p,c,.5,1,-13.5);CooperativeActivities.regroup(p,s);title(p,"OTHER OCEAN MONUMENT","Break the Guardians' line of sight.");OtherMotherLoop.changed(p);return;
        }
        if(phase>=2){
            if(p.getY()<c.getY()-5||phase==3&&p.getZ()<c.getZ()-10||!OceanMonumentCourse.bounds(c).inflate(2).contains(p.position()))move(p,c,.5,phase==3?0:8,phase==3?.5:-280.5);
            if(phase==2&&p.tickCount%20==0)ensurePrize(p,s,c);
            return;
        }
        for(var member:CooperativeActivities.playing(p,s)){
            breathing(member,s);if(member.getY()<c.getY()||member.getY()>c.getY()+17||Math.abs(member.getX()-c.getX())>13.8){lose(member,p,s,c);continue;}
            double z=member.getZ()-c.getZ(),y=member.getY()-c.getY();int boundary=OceanMonumentCourse.start(section)-OceanMonumentCourse.LENGTH+1;
            if(z<boundary+.8){if(z<boundary-3||y<OceanMonumentCourse.exitHeight(section)-.2){lose(member,p,s,c);continue;}if(section==5){finish(p,s,c,true);return;}n.putInt("Section",++section);stopGuardians(l,s,c);member.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.45F,1.3F);OtherMotherLoop.changed(p);break;}
        }
        if(n.getInt("Phase")!=1)return;
        if(p.tickCount%20==0){ensureGuardians(l,s,c,section);for(var member:CooperativeActivities.playing(p,s)){LoopProps.hold(l,member.blockPosition());member.displayClientMessage(Component.literal("Section "+(section+1)+" / 6"),true);}}
        if(section==4&&p.tickCount%4==0)OceanMonumentCourse.cyclingCover(l,c,OtherMotherLoop.now(p));
        final int activeSection=section;
        for(var g:l.getEntitiesOfClass(Guardian.class,OceanMonumentCourse.bounds(c),e->owned(e,s))){
            var data=g.getPersistentData();int slot=data.getInt("Slot"),gs=data.getInt("Section");g.noPhysics=true;g.setDeltaMovement(Vec3.ZERO);g.setPos(OceanMonumentCourse.guardian(gs,slot).add(Vec3.atLowerCornerOf(c)));
            var target=CooperativeActivities.playing(p,s).stream().filter(member->gs==activeSection&&g.distanceToSqr(member)<24*24&&clearSight(l,g.getEyePosition(),member.getEyePosition(),g)&&clearSight(l,g.getEyePosition(),member.position().add(0,.6,0),g)).min(java.util.Comparator.comparingDouble(g::distanceToSqr)).orElse(null);
            if(target==null){data.putInt("Charge",0);data.remove("BeamPlayer");beam(g,0);continue;}
            if(!data.hasUUID("BeamPlayer")||!data.getUUID("BeamPlayer").equals(target.getUUID())){data.putInt("Charge",0);data.putUUID("BeamPlayer",target.getUUID());}
            int charge=nextCharge(data.getInt("Charge"),true);data.putInt("Charge",charge);if(charge==1){beam(g,target.getId());l.broadcastEntityEvent(g,(byte)21);}g.getLookControl().setLookAt(target,60,60);
            if(charge%20==0)target.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(),SoundSource.HOSTILE,.18F,.6F+charge/100F);
            if(charge>=CHARGE_TICKS){lose(target,p,s,c);data.putInt("Charge",0);data.remove("BeamPlayer");beam(g,0);if(n.getInt("Phase")!=1)return;}
        }
    }
    private static void lose(ServerPlayer participant,ServerPlayer director,VisitData.Scene scene,BlockPos c){
        if(com.coraline.multiplayer.StorySessionData.get(director.server).groupActivity(scene)){if(CooperativeActivities.failed(participant,scene)){restoreBreathing(participant,scene);title(participant,"LASERED","Watch the group finish the monument.");}return;}finish(director,scene,c,false);
    }
    static int nextCharge(int previous,boolean visible){return visible?previous+1:0;}
    static boolean clearSight(ServerLevel l,Vec3 from,Vec3 to,Entity source){return l.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,source)).getType()==HitResult.Type.MISS;}
    static void beam(Guardian g,int target){((ActivityGuardianAccess)g).coraline$beamTarget(target);}
    private static void ensureGuardians(ServerLevel l,VisitData.Scene s,BlockPos c,int section){
        var existing=l.getEntitiesOfClass(Guardian.class,OceanMonumentCourse.bounds(c),e->owned(e,s));
        for(var g:existing)if(g.getPersistentData().getInt("Section")!=section)g.discard();
        for(int slot=0;slot<OceanMonumentCourse.guardians(section);slot++){
            final int key=slot;if(existing.stream().anyMatch(g->g.isAlive()&&g.getPersistentData().getInt("Section")==section&&g.getPersistentData().getInt("Slot")==key))continue;
            Guardian g=EntityType.GUARDIAN.create(l);if(g==null)continue;
            mark(g,s);g.setNoAi(true);g.setNoGravity(true);g.setPersistenceRequired();g.setSilent(true);
            g.getPersistentData().putInt("Section",section);g.getPersistentData().putInt("Slot",slot);
            g.setPos(OceanMonumentCourse.guardian(section,slot).add(Vec3.atLowerCornerOf(c)));l.getChunkAt(g.blockPosition());l.addFreshEntity(g);
        }
    }
    static void finish(ServerPlayer p,VisitData.Scene s,BlockPos c,boolean success){
        if(!owns(p,s)||state(s).getInt("Phase")!=1)return;
        state(s).putInt("Phase",success?2:3);stopGuardians(p.serverLevel(),s,c);for(var member:com.coraline.multiplayer.StorySessionData.get(p.server).audience(p))restoreBreathing(member,s);
        OceanMonumentCourse.cyclingCover(p.serverLevel(),c,0);ActivitySoundscape.stop(p,s);
        p.displayClientMessage(Component.empty(),true);
        if(success)OceanMonumentCourse.openVault(p.serverLevel(),c);
        else move(p,c,.5,0,.5);
        title(p,success?"MONUMENT CLEARED":"LASERED",success?"Your treasure awaits.":"The return portal is open.");
        p.playNotifySound(success?SoundEvents.UI_TOAST_CHALLENGE_COMPLETE:SoundEvents.GUARDIAN_ATTACK,SoundSource.PLAYERS,.8F,success?1:.65F);
        p.serverLevel().sendParticles(success?ParticleTypes.END_ROD:ParticleTypes.SPLASH,p.getX(),p.getY()+1,p.getZ(),35,1,1,1,.05);
        if(success)ensurePrize(p,s,c);OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
    }
    private static void ensurePrize(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var at=c.offset(0,9,-281);LoopProps.hold(p.serverLevel(),at);
        if(LoopProps.nearby(p.serverLevel(),s,at).stream().noneMatch(e->e.slot==610&&e.kind()==ScenePropEntity.PRIZE_PLATE)){
            var prop=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.PRIZE_PLATE,Vec3.atBottomCenterOf(at),610);
            prop.food(0,new ItemStack(Items.DIAMOND));prop.setCustomName(Component.literal("Claim monument treasure"));prop.setCustomNameVisible(true);mark(prop,s);
        }
    }
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(prop.kind()!=ScenePropEntity.PRIZE_PLATE||prop.slot!=610)return false;
        if(!com.coraline.multiplayer.StoryCoop.canAct(p,s)||!ActivityWorld.isActivity(p.level())||s.loop.activity!=OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT||!owned(prop,s)||state(s).getInt("Phase")!=2||p.distanceToSqr(prop)>25)return true;
        if(!takePrize(s,s.owner))return true;
        OtherMotherLoop.changed(p);prop.food(0,ItemStack.EMPTY);prop.discard();
        // Existing top activity tier, with the storyline's food/treasure theme preserved.
        var prizes=new java.util.ArrayList<ItemStack>();if(s.loop.activityLoot()==OtherMotherDialogue.Loot.FOOD){for(int i=0;i<4;i++)prizes.add(FeastContent.serving(p.getRandom(),i));prizes.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));}else{prizes.add(new ItemStack(Items.DIAMOND,4+p.getRandom().nextInt(3)));prizes.add(new ItemStack(Items.GOLD_INGOT,8));prizes.add(new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get(),2));}
        if(!com.coraline.multiplayer.StoryRewardDelivery.award(p,s,"ocean_final",prizes))for(var prize:prizes)ActivityTokens.give(p,prize);
        ActivityWorld.ensureExit(p,s);return true;
    }
    static boolean takePrize(VisitData.Scene s,UUID player){
        if(!s.owner.equals(player)||state(s).getInt("Phase")!=2)return false;
        state(s).putInt("Phase",4);return true;
    }
    public static void abort(ServerPlayer p,VisitData.Scene s){
        if(s.loop.activity!=OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT||!s.owner.equals(p.getUUID()))return;
        restoreBreathing(p,s);state(s).putInt("Phase",3);
        if(ActivityWorld.isActivity(p.level()))ActivitySoundscape.stop(p,s);
        var l=p.server.getLevel(ActivityWorld.KEY);if(l!=null)stopGuardians(l,s,center(p,s));
        OtherMotherLoop.changed(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null)abort(p,s);}}
    private OceanMonumentActivity(){}
}
