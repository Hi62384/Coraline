package com.coraline.secrets;

import com.coraline.entity.CharacterEntity;
import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Owner-scoped outdoor vignette. Never carves terrain or replaces a missing actor. */
@Mod.EventBusSubscriber(modid="coraline")
public final class CatEncounter {
    private static final String KEY="CatEncounter";
    public enum Phase { APPROACH,A,B,C,D,EDGE,E,F,G,H,I,DEPART }
    private CatEncounter(){}
    static CompoundTag data(ServerPlayer p){var s=OtherMotherLoop.scene(p);return s==null?new CompoundTag():s.loop.secrets.world.getCompound(KEY);}
    public static boolean active(ServerPlayer p){return data(p).getBoolean("Active");}
    public static boolean guidedVoid(ServerPlayer p){var n=data(p);return n.getBoolean("Active")&&Set.of("EDGE","E").contains(n.getString("Phase"));}
    static Vec3 center(RealmData.Realm r){return Vec3.atBottomCenterOf(r.sourceDoor==null?r.doorPos:r.sourceDoor.offset(r.dx,0,r.dz));}
    public static double marginRadius(int radius){return Math.max(8,radius-30);}
    static double radial(ServerPlayer p,RealmData.Realm r){return p.position().subtract(center(r)).multiply(1,0,1).length();}
    private static boolean eligible(ServerPlayer p){
        var s=OtherMotherLoop.active(p);var r=SecretWorld.realm(p);
        return s!=null&&r!=null&&r.ready&&r.house!=null&&p.isAlive()&&!p.isSpectator()&&OtherWorld.isOther(p.level())&&!p.getPersistentData().getBoolean("CoralineInTunnel")
            &&(s.loop.phase==OtherMotherLoopState.Phase.EXPLORING||s.loop.phase==OtherMotherLoopState.Phase.FREE_TIME)
            &&s.loop.secrets.visit==s.loop.visits&&!s.loop.secrets.searching()&&!SecretDialogue.busy(p)&&!Reconciliation.active(p)&&p.getVehicle()==null
            &&p.serverLevel().canSeeSky(p.blockPosition())&&OtherWorldEstate.outsideHouse(r.house,p.blockPosition())&&radial(p,r)<r.radius+5;
    }
    /** Null means no safe loaded ground; never construct a platform to make a spawn fit. */
    static BlockPos ground(ServerLevel level,BlockPos near){
        if(!level.hasChunkAt(near))return null;
        for(int step=0;step<=48;step++){
            int dy=step==0?0:((step+1)/2)*(step%2==1?1:-1);var at=near.offset(0,dy,0);var floor=level.getBlockState(at.below());
            if(at.getY()<level.getMinBuildHeight()+3||at.getY()>level.getMaxBuildHeight()-3||floor.is(net.minecraft.tags.BlockTags.LEAVES)||floor.is(net.minecraft.tags.BlockTags.LOGS))continue;
            if(floor.isFaceSturdy(level,at.below(),Direction.UP)&&level.getFluidState(at).isEmpty()&&level.noCollision(new AABB(at.getX()+.2,at.getY(),at.getZ()+.2,at.getX()+.8,at.getY()+1.8,at.getZ()+.8)))return at;
        }return null;
    }
    public static boolean begin(ServerPlayer p){
        if(active(p)||!eligible(p))return false;var r=SecretWorld.realm(p);BlockPos spawn=null;
        for(int i=0;i<24&&spawn==null;i++){double angle=p.getRandom().nextDouble()*Math.PI*2;spawn=ground(p.serverLevel(),p.blockPosition().offset((int)(Math.cos(angle)*9),0,(int)(Math.sin(angle)*9)));}
        if(spawn==null)return false;var cat=CharacterCast.ENTITIES.get("cat").get().create(p.serverLevel());if(cat==null)return false;
        var n=new CompoundTag();n.putBoolean("Active",true);n.putUUID("Run",UUID.randomUUID());n.putUUID("Actor",cat.getUUID());n.putInt("Visit",OtherMotherLoop.scene(p).loop.secrets.visit);n.putLong("Started",OtherMotherLoop.now(p));
        n.putString("Phase",Phase.APPROACH.name());n.putLong("LastPos",spawn.asLong());
        var direction=p.position().subtract(center(r)).multiply(1,0,1);if(direction.lengthSqr()<1)direction=new Vec3(1,0,0);direction=direction.normalize();n.putDouble("Dx",direction.x);n.putDouble("Dz",direction.z);
        mark(cat,p,n);cat.setInvulnerable(true);cat.setMaxUpStep(1.05F);cat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(64);cat.getNavigation().setMaxVisitedNodesMultiplier(4);
        cat.moveTo(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,p.getYRot(),0);if(!p.serverLevel().addFreshEntity(cat))return false;
        OtherMotherLoop.scene(p).loop.secrets.world.put(KEY,n);VisitAudio.stopVoice(p);SecretSpies.suspend(p);clearWhiteout(p);holdMothers(p);sync(p,n,cat);OtherMotherLoop.changed(p);return true;
    }
    private static void mark(Entity actor,ServerPlayer p,CompoundTag n){var tag=actor.getPersistentData();tag.putBoolean("CoralineDecoration",true);tag.putUUID("CatEncounterOwner",p.getUUID());tag.putUUID("CatEncounterRun",n.getUUID("Run"));}
    private static boolean owns(Entity e,ServerPlayer p,CompoundTag n){var t=e.getPersistentData();return t.hasUUID("CatEncounterOwner")&&t.getUUID("CatEncounterOwner").equals(p.getUUID())&&n.hasUUID("Run")&&t.hasUUID("CatEncounterRun")&&t.getUUID("CatEncounterRun").equals(n.getUUID("Run"));}
    private static CharacterEntity cat(ServerPlayer p,CompoundTag n){return n.hasUUID("Actor")&&p.serverLevel().getEntity(n.getUUID("Actor")) instanceof CharacterEntity c&&owns(c,p,n)?c:null;}
    private static MouseEntity rat(ServerPlayer p,CompoundTag n){return n.hasUUID("Rat")&&p.serverLevel().getEntity(n.getUUID("Rat")) instanceof MouseEntity m&&owns(m,p,n)?m:null;}
    private static Phase phase(CompoundTag n){try{return Phase.valueOf(n.getString("Phase"));}catch(IllegalArgumentException bad){return Phase.DEPART;}}
    public static boolean voiced(Phase phase){return phase.ordinal()>=Phase.A.ordinal()&&phase.ordinal()<=Phase.I.ordinal()&&phase!=Phase.EDGE;}
    private static String sound(Phase phase){return "cat_encounter_"+phase.name().toLowerCase(Locale.ROOT);}
    private static void next(ServerPlayer p,CompoundTag n,Phase phase){n.putString("Phase",phase.name());n.putInt("Ticks",0);n.remove("Finished");n.remove("Burrow");if(voiced(phase))VisitAudio.play(p,sound(phase),false);OtherMotherLoop.changed(p);}
    public static void voiceFinished(ServerPlayer p,String name){var n=data(p);var phase=phase(n);if(active(p)&&voiced(phase)&&sound(phase).equals(name)&&n.getInt("Ticks")>=VisitAudio.length(name)-10)n.putBoolean("Finished",true);}
    static boolean finished(CompoundTag n,Phase phase){return n.getBoolean("Finished")||n.getInt("Ticks")>=VisitAudio.length(sound(phase))+40;}
    private static void clearWhiteout(ServerPlayer p){for(String key:List.of("CoralineWhiteoutStart","CoralineWhiteoutMoved","CoralineWhiteoutAnchor"))p.getPersistentData().remove(key);}
    public static void tick(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);if(s==null)return;var n=data(p);
        if(!n.getBoolean("Active")){
            if(!eligible(p)||n.contains("CompletedVisit")&&n.getInt("CompletedVisit")==s.loop.secrets.visit||OtherMotherLoop.now(p)<n.getLong("NextTry"))return;
            n.putLong("NextTry",OtherMotherLoop.now(p)+600+p.getRandom().nextInt(1200));s.loop.secrets.world.put(KEY,n);
            if(p.getRandom().nextInt(3)==0)begin(p);return;
        }
        if(!p.isAlive()||!OtherWorld.isOther(p.level())||p.getPersistentData().getBoolean("CoralineInTunnel")||OtherMotherLoop.now(p)-n.getLong("Started")>12000){cancel(p,false);return;}
        holdMothers(p);if(p.tickCount%20==0)SecretSpies.suspend(p);
        var cat=cat(p,n);if(cat==null){var at=BlockPos.of(n.getLong("LastPos"));p.serverLevel().getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(at),2,at);int missing=n.getInt("Missing")+1;n.putInt("Missing",missing);if(missing>100)cancel(p,false);return;}n.remove("Missing");
        if(p.tickCount%20==0){n.putLong("LastPos",cat.blockPosition().asLong());sync(p,n,cat);OtherMotherLoop.changed(p);}
        var phase=phase(n);int ticks=n.getInt("Ticks")+1;n.putInt("Ticks",ticks);var r=SecretWorld.realm(p);if(r==null){cancel(p,false);return;}
        // Deliberately leaving is an abandoned encounter, not a permanent safe mode.
        // Allow a generous grace period for corners, terrain and a missed turn.
        if(Set.of(Phase.APPROACH,Phase.A,Phase.B,Phase.C,Phase.D,Phase.EDGE).contains(phase)){
            int apart=cat.distanceToSqr(p)>24*24?n.getInt("ApartTicks")+1:0;n.putInt("ApartTicks",apart);
            if(apart>=100){p.displayClientMessage(Component.literal("You left the cat behind. Perhaps he'll find you another time."),false);cancel(p,false);return;}
        }
        switch(phase){
            case APPROACH->{walk(cat,p.position(),1.25);cat.getLookControl().setLookAt(p,30,30);if(cat.distanceToSqr(p)<12&&cat.hasLineOfSight(p)){cat.getNavigation().stop();next(p,n,Phase.A);}else if(ticks>1200)cancel(p,false);}
            case A->{stare(cat,p);if(finished(n,phase))next(p,n,Phase.B);}
            case B->{lead(cat,p,n,r,false);burrow(p,cat,n);if(finished(n,phase)){cat.setInvisible(false);cat.noPhysics=false;next(p,n,Phase.C);}}
            case C->{stare(cat,p);if(finished(n,phase))next(p,n,Phase.D);}
            case D->{lead(cat,p,n,r,false);if(finished(n,phase))next(p,n,Phase.EDGE);}
            case EDGE->{lead(cat,p,n,r,true);if(radial(p,r)>=r.radius-4)next(p,n,Phase.E);}
            case E->{lead(cat,p,n,r,true);if(finished(n,phase)&&radial(p,r)>=r.radius+18){if(returnNearHouse(p,cat,n,r)){next(p,n,Phase.F);sync(p,n,cat);}else cancel(p,false);}}
            case F->{stare(cat,p);if(finished(n,phase)){if(spawnRat(p,cat,n))next(p,n,Phase.G);else cancel(p,false);}}
            case G->{var rat=rat(p,n);if(rat==null){cancel(p,false);return;}runRat(rat,cat,p,n);if(finished(n,phase)){next(p,n,Phase.H);cat.gameGesture(20,VisitAudio.length(sound(Phase.H))+60);holdRat(cat,rat,2,0);}}
            case H->{var rat=rat(p,n);if(rat==null){cancel(p,false);return;}cat.getNavigation().stop();boolean biting=ticks<Math.min(60,VisitAudio.length(sound(phase))/2);if(!biting&&cat.gameGesture()==20)cat.gameGesture(21,VisitAudio.length(sound(Phase.I))+240);holdRat(cat,rat,biting?2:3,0);if(finished(n,phase)){cat.gameGesture(21,VisitAudio.length(sound(Phase.I))+240);next(p,n,Phase.I);}}
            case I->{var rat=rat(p,n);if(rat==null){cancel(p,false);return;}float progress=Math.min(1,ticks/(float)Math.max(1,VisitAudio.length(sound(phase))*.7F));holdRat(cat,rat,3,progress);if(ticks%3==0)p.serverLevel().sendParticles(p,new BlockParticleOption(ParticleTypes.FALLING_DUST,Blocks.SAND.defaultBlockState()),true,rat.getX(),rat.getY(),rat.getZ(),5,.12,.06,.12,.02);if(finished(n,phase))next(p,n,Phase.DEPART);}
            case DEPART->{var away=cat.position().subtract(p.position()).multiply(1,0,1);if(away.lengthSqr()<1)away=new Vec3(n.getDouble("Dx"),0,n.getDouble("Dz"));var to=ground(p.serverLevel(),BlockPos.containing(cat.position().add(away.normalize().scale(8))));if(to!=null)walk(cat,Vec3.atBottomCenterOf(to),1.9);var rat=rat(p,n);if(rat!=null)holdRat(cat,rat,3,1);if(ticks>=120||ticks>40&&!p.hasLineOfSight(cat))cancel(p,true);}
        }
        if(active(p)&&p.tickCount%40==0&&Set.of(Phase.B,Phase.D,Phase.EDGE,Phase.E).contains(phase))p.displayClientMessage(Component.literal("Follow the cat"),true);
    }
    private static void walk(CharacterEntity c,Vec3 to,double speed){if(c.tickCount%10==0){var target=c.getNavigation().getTargetPos();if(target==null||c.getNavigation().isDone()||target.distSqr(BlockPos.containing(to))>4)c.getNavigation().moveTo(to.x,to.y,to.z,speed);}}
    private static void stare(CharacterEntity c,ServerPlayer p){c.getNavigation().stop();c.getLookControl().setLookAt(p,30,30);}
    private static void lead(CharacterEntity c,ServerPlayer p,CompoundTag n,RealmData.Realm r,boolean edge){
        if(c.isInvisible()||c.noPhysics)return;if(c.distanceToSqr(p)>196){stare(c,p);return;}
        if(n.getBoolean("WaterRoute")){
            var target=new Vec3(n.getDouble("WaterX"),n.getDouble("WaterY"),n.getDouble("WaterZ"));var delta=target.subtract(c.position());
            c.setNoGravity(true);c.getNavigation().stop();c.getLookControl().setLookAt(target.x,target.y+.2,target.z,30,20);
            c.move(MoverType.SELF,new Vec3(delta.x,0,delta.z).normalize().scale(Math.min(.32,delta.horizontalDistance())).add(0,net.minecraft.util.Mth.clamp(delta.y,-.12,.12),0));
            int waterTicks=n.getInt("WaterTicks")+1;n.putInt("WaterTicks",waterTicks);
            if(delta.lengthSqr()<.5||waterTicks>80)n.remove("WaterRoute");return;
        }
        var origin=center(r);var direction=new Vec3(n.getDouble("Dx"),0,n.getDouble("Dz"));Vec3 wanted;
        double distance=c.position().subtract(origin).multiply(1,0,1).length();
        if(!edge&&distance>=marginRadius(r.radius)-3){double angle=Math.atan2(direction.z,direction.x)+.12*(n.getInt("Ticks")/60+1);wanted=origin.add(Math.cos(angle)*marginRadius(r.radius),0,Math.sin(angle)*marginRadius(r.radius));}
        else wanted=c.position().add(direction.scale(7));
        if(!edge){var delta=wanted.subtract(origin).multiply(1,0,1);if(delta.length()>marginRadius(r.radius)){var clamped=origin.add(delta.normalize().scale(marginRadius(r.radius)));wanted=new Vec3(clamped.x,wanted.y,clamped.z);}}
        if(edge)OtherWorldEstate.extendWhite(p.serverLevel(),r,BlockPos.containing(wanted.x,p.getY(),wanted.z));
        if(c.tickCount%10!=0)return;
        for(int degrees:new int[]{0,30,-30,60,-60,90,-90}){var heading=wanted.subtract(c.position()).multiply(1,0,1).normalize().yRot((float)Math.toRadians(degrees));var candidate=c.position().add(heading.scale(degrees==0?7:5));if(!edge&&candidate.subtract(origin).multiply(1,0,1).length()>marginRadius(r.radius))continue;var at=ground(p.serverLevel(),BlockPos.containing(candidate.x,c.getY(),candidate.z));if(at==null)continue;var path=c.getNavigation().createPath(at,0);if(path!=null&&path.canReach()){c.setNoGravity(false);c.getNavigation().moveTo(path,2.0);return;}}
        // Water is not dry ground. The magical Cat can skim its surface without
        // placing blocks, moving the player, or tunnelling through solid terrain.
        if(!p.serverLevel().hasChunkAt(BlockPos.containing(wanted))){stare(c,p);return;}
        var surface=p.serverLevel().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,BlockPos.containing(wanted.x,c.getY(),wanted.z));
        if(p.serverLevel().hasChunkAt(surface)&&p.serverLevel().getFluidState(surface.below()).is(net.minecraft.tags.FluidTags.WATER)&&Math.abs(surface.getY()-c.getY())<6){
            var target=Vec3.atBottomCenterOf(surface);var hit=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(c.position().add(0,.4,0),target.add(0,.4,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,c));
            if(hit.getType()==HitResult.Type.MISS){n.putBoolean("WaterRoute",true);n.putDouble("WaterX",target.x);n.putDouble("WaterY",target.y);n.putDouble("WaterZ",target.z);n.putInt("WaterTicks",0);return;}
        }stare(c,p);
    }
    private static void burrow(ServerPlayer p,CharacterEntity cat,CompoundTag n){
        int remaining=VisitAudio.length(sound(Phase.B))-n.getInt("Ticks");int step=n.getInt("Burrow");
        if(remaining<=40&&step==0){var exit=ground(p.serverLevel(),cat.blockPosition().offset(4,0,3));if(exit==null)exit=ground(p.serverLevel(),cat.blockPosition().offset(-4,0,-3));if(exit==null)return;n.putLong("Emerge",exit.asLong());n.putInt("Burrow",1);cat.getNavigation().stop();cat.noPhysics=true;}
        if(n.getInt("Burrow")==1){cat.setPos(cat.getX(),cat.getY()-.025,cat.getZ());if(remaining<=20){cat.setInvisible(true);n.putInt("Burrow",2);}}
        if(n.getInt("Burrow")==2&&remaining<=6){var at=BlockPos.of(n.getLong("Emerge"));cat.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);cat.setInvisible(false);cat.noPhysics=false;n.putInt("Burrow",3);p.serverLevel().sendParticles(p,ParticleTypes.POOF,true,cat.getX(),cat.getY()+.2,cat.getZ(),8,.2,.1,.2,.01);}
    }
    private static boolean returnNearHouse(ServerPlayer p,CharacterEntity cat,CompoundTag n,RealmData.Realm r){
        BlockPos at=null;var porch=r.porch==null?r.doorPos:r.porch;
        outer:for(int dx:new int[]{24,-24,32,-32})for(int dz:new int[]{12,-12,24,-24}){var near=porch.offset(dx,0,dz);p.serverLevel().getChunkAt(near);var candidate=ground(p.serverLevel(),near);if(candidate!=null&&OtherWorldEstate.outsideHouse(r.house,candidate)&&candidate.distSqr(porch)>400){at=candidate;break outer;}}
        if(at==null)return false;p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);p.fallDistance=0;var spot=ground(p.serverLevel(),at.offset(2,0,1));if(spot==null)spot=at;cat.teleportTo(spot.getX()+.5,spot.getY(),spot.getZ()+.5);n.putLong("FadeAt",OtherMotherLoop.now(p));return true;
    }
    private static boolean spawnRat(ServerPlayer p,CharacterEntity cat,CompoundTag n){
        var at=ground(p.serverLevel(),cat.blockPosition().offset(3,0,1));if(at==null)return false;var rat=ModEntities.MOUSE.get().create(p.serverLevel());if(rat==null)return false;mark(rat,p,n);rat.circusStyle(4);rat.catPose(1,0);rat.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);if(!p.serverLevel().addFreshEntity(rat))return false;n.putUUID("Rat",rat.getUUID());return true;
    }
    private static void runRat(MouseEntity rat,CharacterEntity cat,ServerPlayer p,CompoundTag n){
        var away=rat.position().subtract(cat.position()).multiply(1,0,1);if(away.lengthSqr()<1)away=new Vec3(n.getDouble("Dz"),0,-n.getDouble("Dx"));var delta=away.normalize().scale(.12);var target=ground(p.serverLevel(),BlockPos.containing(rat.position().add(delta.scale(8))));
        if(rat.distanceToSqr(p)<81&&target!=null){rat.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));rat.setYBodyRot(rat.getYRot());rat.move(MoverType.SELF,new Vec3(delta.x,Math.max(-.2,Math.min(.2,target.getY()-rat.getY())),delta.z));}
        if(cat.distanceToSqr(rat)>3)walk(cat,rat.position(),1.5);else cat.getNavigation().stop();cat.getLookControl().setLookAt(rat,30,30);
    }
    private static void holdRat(CharacterEntity cat,MouseEntity rat,int pose,float ugly){var forward=new Vec3(-Math.sin(Math.toRadians(cat.getYRot())),0,Math.cos(Math.toRadians(cat.getYRot())));rat.noPhysics=true;rat.setNoGravity(true);rat.catPose(pose,ugly);rat.setDeltaMovement(Vec3.ZERO);var at=cat.position().add(forward.scale(.43)).add(0,.44,0);rat.setPos(at.x,at.y,at.z);rat.setYRot(cat.getYRot()+90);rat.setXRot(pose==2?(float)Math.sin(cat.tickCount*1.2)*30:75);}
    private static void sync(ServerPlayer p,CompoundTag n,CharacterEntity cat){var packet=new CompoundTag();packet.putBoolean("CatEncounter",true);packet.putBoolean("Active",n.getBoolean("Active"));packet.putInt("Actor",cat.getId());packet.putString("Phase",n.getString("Phase"));packet.putLong("FadeAge",n.contains("FadeAt")?OtherMotherLoop.now(p)-n.getLong("FadeAt"):-1);SecretsNetwork.send(p,packet);}
    private static void holdMothers(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s==null||s.mom==null)return;for(var l:p.server.getAllLevels())if(l.getEntity(s.mom) instanceof ParentEntity m){var t=m.getPersistentData();if(!t.hasUUID("CatHeldOwner")){t.putUUID("CatHeldOwner",p.getUUID());t.putBoolean("CatWasInvisible",m.isInvisible());}m.setInvisible(true);m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);}}
    public static boolean holdMother(ParentEntity m){var t=m.getPersistentData();if(!t.hasUUID("CatHeldOwner")){if(!t.hasUUID("LoopOwner"))return false;var owner=m.getServer().getPlayerList().getPlayer(t.getUUID("LoopOwner"));if(owner==null||!active(owner))return false;t.putUUID("CatHeldOwner",owner.getUUID());t.putBoolean("CatWasInvisible",m.isInvisible());}var p=m.getServer().getPlayerList().getPlayer(t.getUUID("CatHeldOwner"));if(p!=null&&active(p)){m.setInvisible(true);m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);return true;}m.setInvisible(t.getBoolean("CatWasInvisible"));t.remove("CatHeldOwner");t.remove("CatWasInvisible");return false;}
    public static void cancel(ServerPlayer p,boolean completed){var n=data(p);if(!n.getBoolean("Active"))return;n.putBoolean("Active",false);for(var l:p.server.getAllLevels()){for(String key:List.of("Actor","Rat"))if(n.hasUUID(key)){var e=l.getEntity(n.getUUID(key));if(e!=null&&owns(e,p,n))e.discard();}var s=OtherMotherLoop.scene(p);if(s!=null&&s.mom!=null&&l.getEntity(s.mom) instanceof ParentEntity m)holdMother(m);}
        var scene=OtherMotherLoop.scene(p);if(scene!=null){if(completed&&phase(n)==Phase.DEPART){n.putInt("CompletedVisit",scene.loop.secrets.visit);awardSecret(p,scene.loop.secrets);}n.putLong("NextTry",OtherMotherLoop.now(p)+2400);}clearWhiteout(p);for(Phase phase:Phase.values())if(voiced(phase))VisitAudio.stopVoice(p,sound(phase));var packet=new CompoundTag();packet.putBoolean("CatEncounter",true);packet.putBoolean("Active",false);SecretsNetwork.send(p,packet);OtherMotherLoop.changed(p);
    }
    /** Never downgrade to common loot or duplicate evidence the player already owns. */
    static String rewardSecret(SecretsState state){
        return SecretRegistry.ALL.values().stream().filter(secret->secret.tier()==2&&!state.journal.contains(secret.id())&&!state.issued.containsValue(secret.id())&&!state.blackmail.values().stream().anyMatch(ids->ids.contains(secret.id())))
            .max(Comparator.comparingDouble(SecretRegistry.Secret::severity)).map(SecretRegistry.Secret::id).orElse("");
    }
    private static void awardSecret(ServerPlayer p,SecretsState state){
        String id=rewardSecret(state);
        if(id.isEmpty()){p.displayClientMessage(Component.literal("The cat has nothing more to reveal: you already hold every Exceptional secret."),false);return;}
        SecretTransactions.issue(p,id);
        p.displayClientMessage(Component.literal("The cat left you an Exceptional secret: "+SecretRegistry.ALL.get(id).name()+". Hold it opposite your journal and right-click the journal to record it."),false);
    }
    public static void skip(ServerPlayer p){var n=data(p);if(guidedVoid(p)){var r=SecretWorld.realm(p);var cat=cat(p,n);if(r!=null&&cat!=null&&!returnNearHouse(p,cat,n,r))QuestRecovery.recoverToPorch(p);}cancel(p,false);}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void playerTick(TickEvent.PlayerTickEvent e){if(e.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p)tick(p);}
    @SubscribeEvent public static void motherTick(LivingEvent.LivingTickEvent e){if(!e.getEntity().level().isClientSide&&e.getEntity() instanceof ParentEntity m)holdMother(m);}
    @SubscribeEvent public static void damage(net.minecraftforge.event.entity.living.LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p&&active(p))e.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p,false);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p,false);}
    public static final class ControlGoal extends Goal {
        public ControlGoal(CharacterEntity actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}private final CharacterEntity actor;
        @Override public boolean canUse(){return actor.getPersistentData().hasUUID("CatEncounterOwner");}
    }
}
