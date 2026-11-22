package com.coraline.feast;

import java.util.*;
import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.registry.ModBlocks;
import com.coraline.secrets.Reconciliation;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One owner-persisted continuation, using the existing cast lease and acknowledged audio clock. */
@Mod.EventBusSubscriber(modid="coraline")
public final class MissingParentsCat {
    public static final String EVENT="MISSING_PARENTS_CAT_VISIT";
    public static final String INTRO_MUSIC="missing_parents_music";
    static int spawnCue(int duration){return 45*20;}
    static boolean arrivalMusic(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(n.getBoolean("ArrivalMusicComplete"))return true;
        if(!ShowPlayback.data(s).getString("Sound").equals(INTRO_MUSIC)&&!ShowPlayback.start(p,s,INTRO_MUSIC,true))return false;
        ShowPlayback.tick(p,s);
        if(ShowPlayback.started(s)&&ShowPlayback.audibleElapsedTicks(s)>=spawnCue(ShowRecordings.duration(INTRO_MUSIC))||ShowPlayback.finished(s)&&ShowPlayback.data(s).getBoolean("SafetySkipped")){
            n.putBoolean("ArrivalMusicComplete",true);OtherMotherLoop.changed(p);return true;
        }return false;
    }
    public enum Phase { PENDING,NOTICE,APPROACH,A,FOLLOW_MIRROR,B,WAIT_BREAK,C,SHATTER,D,FOLLOW_BED,E,F,DEPART,COMPLETE }
    public static CompoundTag data(VisitData.Scene s){if(s==null)return new CompoundTag();if(!s.loop.story.contains("MissingParentsCat"))s.loop.story.put("MissingParentsCat",new CompoundTag());return s.loop.story.getCompound("MissingParentsCat");}
    public static Phase phase(VisitData.Scene s){try{return Phase.valueOf(data(s).getString("Phase"));}catch(IllegalArgumentException e){return Phase.PENDING;}}
    public static boolean eligible(VisitData.Scene s){
        if(s==null||AcceptedButtonEyes.active(s))return false;var root=FirstEscapeStory.data(s);var tea=MissingParentsTea.data(s);
        return root.getBoolean("Complete")&&root.getBoolean("WybieDeparted")&&root.getBoolean("OtherWorldSealed")
            &&FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.ESCAPE_COMPLETE
            &&tea.getBoolean("Completed")&&!tea.getBoolean("Active")
            &&s.loop.story.getBoolean("SeeingStoneReceipt")&&!s.loop.story.getBoolean("SeeingStonePending")
            &&!data(s).getBoolean("Complete")&&!ParentRescueStory.started(s);
    }
    public static boolean tunnelRestored(VisitData.Scene s){return s!=null&&s.loop.story.getCompound("MissingParentsCat").getBoolean("TunnelRestored");}
    public static String voice(Phase phase){return phase.name().length()==1?"missing_parents_cat_"+phase.name().toLowerCase(Locale.ROOT):"";}
    static void go(ServerPlayer p,VisitData.Scene s,Phase next){
        ShowPlayback.transition(p,s,voice(next));var n=data(s);n.putString("Phase",next.name());n.putInt("Ticks",0);n.remove("Audio");n.remove("Blocker");
        n.remove("MoveTarget");n.remove("MoveBest");n.remove("StuckTicks");n.remove("LostTicks");n.remove("GuideRequested");n.remove("GuideResolved");n.remove("GuideRetry");
        n.remove("FloorGuide");n.remove("GuideFloor");
        OtherMotherLoop.changed(p);props(p,s);p.server.overworld().getDataStorage().save();
    }
    static boolean owns(ServerPlayer p,VisitData.Scene s){var n=data(s);return s!=null&&!AcceptedButtonEyes.active(s)&&s.owner.equals(p.getUUID())&&(!n.hasUUID("Owner")||s.owner.equals(n.getUUID("Owner")))&&(!n.hasUUID("Door")||s.door.equals(n.getUUID("Door")));}
    private static CharacterEntity cat(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(n.contains("CatAt"))LoopProps.hold(p.server.overworld(),BlockPos.of(n.getLong("CatAt")));
        return ShowActors.actor(p,s,"cat") instanceof CharacterEntity c?c:null;
    }
    static boolean walk(Mob actor,Vec3 destination,double speed){
        if(actor==null)return false;actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;actor.setMaxUpStep(1.05F);
        if(actor.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}
        if(arrived(actor,destination)){actor.getNavigation().stop();return true;}
        if(actor.getNavigation().isDone()||actor.tickCount%20==0)actor.getNavigation().moveTo(destination.x,destination.y,destination.z,speed);
        if(actor.horizontalCollision&&actor.onGround()&&actor.tickCount%20==0)actor.getJumpControl().jump();return false;
    }
    static boolean arrived(Mob actor,Vec3 destination){
        var delta=actor.position().subtract(destination);
        if(delta.horizontalDistanceSqr()>1.44||Math.abs(delta.y)>1.1)return false;
        // Adjacent grass/floor coverings and an occupied centre tile are not a
        // failed arrival, but a close point behind a wall still is.
        return actor.level().clip(new net.minecraft.world.level.ClipContext(actor.position().add(0,.5,0),destination.add(0,.5,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,actor)).getType()==HitResult.Type.MISS;
    }
    private static void look(Mob actor,Vec3 point){actor.getNavigation().stop();actor.getLookControl().setLookAt(point.x,point.y,point.z,30,25);}
    static boolean seen(ServerPlayer p,Entity e){return p.level()==e.level()&&p.distanceToSqr(e)<144&&p.hasLineOfSight(e)&&p.getLookAngle().dot(e.getEyePosition().subtract(p.getEyePosition()).normalize())>.80;}
    private static boolean pointVisible(ServerPlayer p,Vec3 point){
        if(p.distanceToSqr(point)>128*128||p.getLookAngle().dot(point.subtract(p.getEyePosition()).normalize())<.15)return false;
        return p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),point,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;
    }
    private static boolean pointVisibleToAnyone(ServerPlayer p,Vec3 point){
        if(pointVisible(p,point))return true;for(var other:p.server.getPlayerList().getPlayers())if(other!=p&&other.level()==p.level()&&pointVisible(other,point))return true;return false;
    }
    private static boolean walkable(ServerPlayer p,BlockPos at){
        return !p.serverLevel().isOutsideBuildHeight(at)&&p.level().isEmptyBlock(at)&&p.level().isEmptyBlock(at.above())
            &&p.level().getBlockState(at.below()).isFaceSturdy(p.level(),at.below(),Direction.UP);
    }
    private static BlockPos nearParentsBed(ServerPlayer p,BlockPos bed){
        BlockPos best=null;double score=Double.MAX_VALUE;
        for(int radius=3;radius<=6;radius++)for(var direction:Direction.Plane.HORIZONTAL)for(int dy:new int[]{0,1,-1}){
            var at=bed.relative(direction,radius).above(dy);if(!p.serverLevel().hasChunkAt(at)||!walkable(p,at))continue;
            double cost=at.distSqr(bed)+(pointVisibleToAnyone(p,at.getCenter())?100:0);if(cost<score){score=cost;best=at;}
        }return best;
    }
    private static BlockPos nearby(ServerPlayer p,boolean hidden){
        for(int i=0;i<12;i++){double angle=p.getRandom().nextDouble()*Math.PI*2;int radius=7+p.getRandom().nextInt(6);
            var around=p.blockPosition().offset((int)Math.round(Math.cos(angle)*radius),0,(int)Math.round(Math.sin(angle)*radius));
            var at=OtherWorld.safePlayer(p.serverLevel(),around);
            if(at!=null&&Math.abs(at.getY()-p.getY())<=4&&at.distSqr(p.blockPosition())>=25&&at.distSqr(p.blockPosition())<=256&&walkable(p,at)
                &&(!hidden||!pointVisibleToAnyone(p,Vec3.atBottomCenterOf(at).add(0,.5,0))))return at;
        }return null;
    }
    /** Never reset a recording because the player wandered away or a doorway blocked navigation. */
    static boolean guide(ServerPlayer p,VisitData.Scene s,CharacterEntity actor,BlockPos target,double speed){
        var n=data(s);actor.gameGesture(32,10);
        if(!n.contains("GuideRequested")||n.getLong("GuideRequested")!=target.asLong()||n.getInt("StuckTicks")>=40&&n.getLong("GuideRetry")<=p.serverLevel().getGameTime()){
            n.putLong("GuideRequested",target.asLong());var resolved=reachableNear(p,actor,target);
            if(resolved!=null)n.putLong("GuideResolved",resolved.asLong());else n.remove("GuideResolved");
            n.putLong("GuideRetry",p.serverLevel().getGameTime()+40);
        }
        if(n.contains("GuideResolved"))target=BlockPos.of(n.getLong("GuideResolved"));
        if(phase(s)!=Phase.APPROACH&&actor.distanceToSqr(p)>100){
            look(actor,p.getEyePosition());int lost=n.getInt("LostTicks")+1;n.putInt("LostTicks",lost);
            if(lost>=100&&!ShowActors.visibleToAnyone(actor,p)){var at=nearby(p,true);if(at!=null){actor.moveTo(Vec3.atBottomCenterOf(at));n.putLong("CatAt",at.asLong());n.putInt("LostTicks",0);}}
            FirstEscapeStory.hint(p,"The Cat is waiting for you.");return false;
        }
        n.putInt("LostTicks",0);
        double distance=actor.distanceToSqr(Vec3.atBottomCenterOf(target));
        if(!n.contains("MoveTarget")||n.getLong("MoveTarget")!=target.asLong()){n.putLong("MoveTarget",target.asLong());n.putDouble("MoveBest",distance);n.putInt("StuckTicks",0);}
        if(distance<n.getDouble("MoveBest")-.08){n.putDouble("MoveBest",distance);n.putInt("StuckTicks",0);}
        else n.putInt("StuckTicks",n.getInt("StuckTicks")+1);
        // Vanilla navigation can report an unreachable partial path beside a
        // bed and endlessly retry its final node. After a short real stall,
        // walk the same Cat through a bounded, collision-checked floor route.
        // These are small physical steps, never a warp through furniture.
        if(n.getInt("StuckTicks")>=40)n.putBoolean("FloorGuide",true);
        if(n.getBoolean("FloorGuide")){
            if(StoryFloorRoute.move(actor,n,"GuideFloor",target,p.distanceToSqr(actor)>36?.085:.12))return true;
        }else if(walk(actor,Vec3.atBottomCenterOf(target),p.distanceToSqr(actor)>36?speed*.7:speed))return true;
        if(n.getInt("StuckTicks")>=160&&!ShowActors.visibleToAnyone(actor,p)){
            var at=reachableNear(p,actor,target);if(at!=null){actor.getNavigation().stop();actor.moveTo(Vec3.atBottomCenterOf(at));n.putLong("CatAt",at.asLong());n.putInt("StuckTicks",0);}
        }
        if(n.getInt("StuckTicks")>=240)FirstEscapeStory.hint(p,"Clear a path through the doorway for the Cat.");
        return false;
    }
    static BlockPos bedFoot(ServerPlayer p,BlockPos bed){
        var state=p.level().getBlockState(bed);
        if(state.getBlock() instanceof com.coraline.parents.ParentsBedBlock)return com.coraline.parents.ParentsBedBlock.anchorPos(bed,state).relative(state.getValue(com.coraline.parents.ParentsBedBlock.FACING).getOpposite());
        return state.getBlock() instanceof BedBlock&&state.getValue(BedBlock.PART)==BedPart.HEAD?bed.relative(state.getValue(BedBlock.FACING).getOpposite()):bed;
    }
    private static BlockPos reachableNear(ServerPlayer p,Mob actor,BlockPos target){
        // Recover toward the intended destination, not a new random place near
        // the player. Keep the same target and never jump through a solid wall.
        for(int radius=0;radius<=2;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int dy:new int[]{0,1,-1}){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;var at=target.offset(x,dy,z);
            if(!StoryFloorRoute.fits(actor,at))continue;var feet=StoryFloorRoute.feet(p.serverLevel(),at);
            if(arrived(actor,feet))return at;
            var route=actor.getNavigation().createPath(at,0);if(route!=null&&route.canReach())return at;
        }return null;
    }
    static Direction bedFront(ServerPlayer p,BlockPos bed){var state=p.level().getBlockState(bed);return state.getBlock() instanceof com.coraline.parents.ParentsBedBlock?state.getValue(com.coraline.parents.ParentsBedBlock.FACING).getOpposite():state.getBlock() instanceof BedBlock?state.getValue(BedBlock.FACING).getOpposite():Direction.SOUTH;}
    static BlockPos bedApproach(ServerPlayer p,Mob actor,BlockPos foot,Direction front){
        BlockPos best=null;double score=Double.MAX_VALUE;
        // The first clear reachable edge wins, not the bed's solid mattress or a
        // fixed point two blocks out that might be a wall/chair in a custom home.
        for(int depth=1;depth<=2;depth++)for(int side:new int[]{0,1,-1})for(int dy:new int[]{0,1,-1}){
            var cell=foot.relative(front,depth).relative(front.getClockWise(),side).above(dy);
            if(!StoryFloorRoute.fits(actor,cell))continue;
            var path=actor.getNavigation().createPath(cell,0);
            if(!arrived(actor,StoryFloorRoute.feet(p.serverLevel(),cell))&&(path==null||!path.canReach()))continue;
            double cost=(path==null?0:path.getNodeCount())+depth*2+Math.abs(side)+Math.abs(dy);
            if(cost<score){best=cell;score=cost;}
        }return best;
    }
    static boolean available(ServerPlayer p,VisitData.Scene s){
        if(Reconciliation.active(p))return false;var audio=ShowPlayback.data(s);
        if(!audio.getString("Sound").isEmpty()&&!audio.getBoolean("Finished")&&!audio.getString("Sound").startsWith("missing_parents_cat_")&&!audio.getString("Sound").equals(INTRO_MUSIC))return false;
        return true;
    }
    /** A vanished bed may retire a started clue only while its original Cat still exists.
     * An unstarted clue needs the original family bed for its physical mirror scene. */
    static boolean missingBedRecoveryReady(boolean started,boolean originalCatPresent,int missingTicks){
        return started&&originalCatPresent&&missingTicks>=200;
    }
    /** The mirror/doll watchdog cannot replace the original Cat by skipping
     * ahead. Its UUID must remain present throughout every started scene. */
    static boolean catWatchdogReady(boolean started,Phase state,boolean originalPresent){
        return !started||state.ordinal()<Phase.NOTICE.ordinal()
                ||state==Phase.COMPLETE||originalPresent;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){
        if(!eligible(s)||!owns(p,s)||!p.isAlive()||p.serverLevel()!=p.server.overworld()||!available(p,s))return;
        var n=data(s);var family=ParentAccess.family(p.server,s.owner);
        if(family==null||!family.storyMissing)return;
        if(family.bed==null){
            if(!n.getBoolean("Started")){
                n.putString("Blocker","Place your family's original parents' bed to begin the Cat's mirror scene.");
                FirstEscapeStory.hint(p,n.getString("Blocker"));
                return;
            }
            int missing=Math.min(200,n.getInt("MissingBedTicks")+1);
            n.putInt("MissingBedTicks",missing);
            // Without the original Cat, an apparently successful tunnel handoff
            // would strand the later collapse, final fight and farewell.
            var originalCat=cat(p,s);
            if(originalCat==null){
                n.putString("Blocker","Waiting for the original Cat to load before restoring the missing-bed passage.");
                FirstEscapeStory.hint(p,n.getString("Blocker"));
                return;
            }
            if(missingBedRecoveryReady(n.getBoolean("Started"),originalCat!=null,missing)){
                if(!StoryTunnel.restore(p,s)){
                    FirstEscapeStory.hint(p,"Restoring the original Little Door passage...");
                    return;
                }
                n.putBoolean("DollsRetrieved",true);
                s.loop.story.putBoolean("ParentDollsDiscovered",true);
                n.putBoolean("Complete",true);
                n.putBoolean("TunnelRestored",true);
                n.putString("Phase",Phase.COMPLETE.name());
                n.remove("Blocker");
                ShowPlayback.stop(p,s);
                ShowActors.releaseHidden(p,s,"cat");
                props(p,s);
                OtherMotherLoop.changed(p);
                FirstEscapeStory.hint(p,"The Cat reveals the way back. Investigate the Little Door.");
            }
            return;
        }
        if(n.contains("MissingBedTicks"))n.remove("MissingBedTicks");
        if(n.getString("Blocker").startsWith("Place your family's original parents' bed")
                ||n.getString("Blocker").startsWith("Waiting for the original Cat to load before restoring"))
            n.remove("Blocker");
        if(phase(s)==Phase.PENDING&&p.distanceToSqr(family.bed.getCenter())>64){
            FirstEscapeStory.hint(p,"Return to your parents' bed for the Cat's mirror scene.");
            return;
        }
        if(!n.contains("BedFoot")&&phase(s).ordinal()>=Phase.E.ordinal()){
            n.putLong("BedFoot",bedFoot(p,family.bed).asLong());n.putString("BedFront",bedFront(p,family.bed).getName());
        }
        if(!n.hasUUID("Owner")){
            n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putString("Event",EVENT);
            if(!n.contains("Delay"))n.putInt("Delay",120+p.getRandom().nextInt(301));
            n.putLong("Bed",family.bed.asLong());n.putInt("MomSkin",Math.max(0,family.momSkin));n.putInt("DadSkin",Math.max(0,family.dadSkin));
        }
        if(phase(s)==Phase.PENDING){
            if(!arrivalMusic(p,s))return;
            var door=DoorData.get(p.server).doors.get(s.door);
            if(!tunnelRestored(s)&&(door==null||!s.owner.equals(door.owner)||!door.placed||!door.exists||!FirstEscapeStory.sealed(p.server,s.owner))){
                n.putString("Blocker","Waiting for this family's sealed Little Door.");return;
            }
            if(!tunnelRestored(s)){
                var source=FamilyData.level(p.server,door.dimension);if(source!=p.server.overworld())return;source.getChunkAt(door.pos);
                var doorState=source.getBlockState(door.pos);
                if(!doorState.is(ModBlocks.TINY_DOOR.get())||doorState.getValue(com.coraline.block.TinyDoorBlock.PORTAL)
                    ||!source.getBlockState(door.pos.relative(doorState.getValue(com.coraline.block.TinyDoorBlock.FACING).getOpposite())).is(Blocks.BRICKS))return;
            }
            var at=nearParentsBed(p,family.bed);if(at==null)return;
            var actor=ShowActors.acquire(p,s,"cat",p.serverLevel(),Vec3.atBottomCenterOf(at));
            if(actor==null){n.putString("Blocker","Waiting for the canonical Cat lease; no duplicate created.");return;}
            actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;
            n.putLong("CatAt",actor.blockPosition().asLong());n.putBoolean("Started",true);s.loop.story.putString("MajorStoryPhase",EVENT);go(p,s,Phase.NOTICE);
        }
        var cat=cat(p,s);if(cat==null){n.putString("Blocker","Waiting for the saved Cat to load; no duplicate created.");return;}
        n.putLong("CatAt",cat.blockPosition().asLong());cat.setInvulnerable(true);cat.setNoAi(false);cat.setNoGravity(false);cat.noPhysics=false;
        if(n.getBoolean("Recover")){n.remove("Recover");n.remove("Audio");ShowPlayback.stop(p,s);if(phase(s)==Phase.B)n.putBoolean("BFinished",false);}
        var state=phase(s);String sound=voice(state);boolean done=false;int t=n.getInt("Ticks");
        if(!sound.isEmpty()){
            if(!n.getBoolean("Audio")){if(!ShowPlayback.start(p,s,sound,false))return;n.putBoolean("Audio",true);}
            done=ShowPlayback.tick(p,s);t=ShowPlayback.position(s);n.putInt("Ticks",t);
            if(!ShowPlayback.data(s).getString("Blocker").isEmpty()){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));look(cat,p.getEyePosition());return;}
        }else n.putInt("Ticks",++t);
        cat.gameGesture(31,10);
        switch(state){
            case NOTICE->{
                cat.gameGesture(30,10);look(cat,p.getEyePosition());
                if(seen(p,cat))go(p,s,Phase.APPROACH);
                else{if(t%200==120)cat.playSound(net.minecraft.sounds.SoundEvents.CAT_AMBIENT,.35F,1);if(t>=300&&!ShowActors.visibleToAnyone(cat,p)){var at=nearby(p,true);if(at!=null){cat.moveTo(Vec3.atBottomCenterOf(at));n.putInt("Ticks",0);}}}
            }
            case APPROACH->{if(cat.distanceToSqr(p)<=4&&p.hasLineOfSight(cat)){look(cat,p.getEyePosition());go(p,s,Phase.A);}else guide(p,s,cat,p.blockPosition(),1.05);}
            case A->{look(cat,p.getEyePosition());if(done){if(mirror(p,s,family.bed))go(p,s,Phase.FOLLOW_MIRROR);else FirstEscapeStory.hint(p,"Clear a space beside your parents' bed for the mirror.");}}
            case FOLLOW_MIRROR->{
                var target=BlockPos.of(n.getLong("MirrorApproach"));
                if(guide(p,s,cat,target,.95)&&p.distanceToSqr(target.getCenter())<25){look(cat,BlockPos.of(n.getLong("Mirror")).getCenter());go(p,s,Phase.B);}
                FirstEscapeStory.hint(p,"Follow the Cat.");
            }
            case B->{
                look(cat,BlockPos.of(n.getLong("Mirror")).getCenter());
                if(ShowPlayback.started(s))n.putInt("Writing",Math.min(MissingParentsCatTimeline.WRITE_TICKS,n.getInt("Writing")+1));
                if(done)n.putBoolean("BFinished",true);
                if(n.getBoolean("BFinished")&&n.getInt("Writing")>=MissingParentsCatTimeline.WRITE_TICKS){
                    n.putInt("ReadTicks",n.getInt("ReadTicks")+1);
                    if(n.getInt("ReadTicks")>=MissingParentsCatTimeline.READ_TICKS){n.putBoolean("HelpUsComplete",true);go(p,s,Phase.WAIT_BREAK);}
                }
            }
            case WAIT_BREAK->{props(p,s);if(!n.hasUUID("MirrorActor")||!(p.serverLevel().getEntity(n.getUUID("MirrorActor")) instanceof CinematicPropEntity)){if(t>=100){n.putBoolean("MirrorBroken",true);go(p,s,Phase.D);}break;}look(cat,BlockPos.of(n.getLong("Mirror")).getCenter());FirstEscapeStory.hint(p,"Break the mirror. Right-click it.");}
            case C->{look(cat,BlockPos.of(n.getLong("Mirror")).getCenter());if(Math.max(t,ShowPlayback.audiblePosition(s))>=MissingParentsCatTimeline.shatterCue(ShowRecordings.duration(sound)))shatter(p,s);if(done){shatter(p,s);go(p,s,Phase.SHATTER);}}
            case SHATTER->{look(cat,BlockPos.of(n.getLong("Mirror")).getCenter());cat.gameGesture(34,10);if(n.getInt("ShatterAge")>=MissingParentsCatTimeline.SHATTER_TICKS)go(p,s,Phase.D);}
            case D->{look(cat,p.getEyePosition());if(done)go(p,s,Phase.FOLLOW_BED);}
            case FOLLOW_BED->{
                retireShatteredMirror(p,s);
                if(!(p.level().getBlockState(family.bed).getBlock() instanceof BedBlock)&&!p.level().getBlockState(family.bed).is(ModBlocks.PARENTS_BED.get())){n.putString("Blocker","The family's bed was removed. Restore or update the canonical parents' bed.");FirstEscapeStory.hint(p,"Find your parents' bed.");look(cat,p.getEyePosition());break;}
                n.remove("Blocker");var foot=bedFoot(p,family.bed);var front=bedFront(p,family.bed);
                n.putLong("Bed",family.bed.asLong());n.putLong("BedFoot",foot.asLong());n.putString("BedFront",front.getName());
                if(!n.contains("BedApproach")||!StoryFloorRoute.fits(cat,BlockPos.of(n.getLong("BedApproach")))||n.getInt("StuckTicks")>=60){
                    if(n.getLong("BedRetry")<=p.serverLevel().getGameTime()){
                        var resolved=bedApproach(p,cat,foot,front);n.putLong("BedRetry",p.serverLevel().getGameTime()+20);
                        if(resolved!=null){n.putLong("BedApproach",resolved.asLong());n.remove("GuideRequested");n.putInt("StuckTicks",0);}else n.remove("BedApproach");
                    }
                }
                if(!n.contains("BedApproach")){look(cat,foot.getCenter());FirstEscapeStory.hint(p,"Clear a space at the foot of your parents' bed.");break;}
                var target=BlockPos.of(n.getLong("BedApproach"));
                if(guide(p,s,cat,target,.9)&&p.distanceToSqr(foot.getCenter())<36){look(cat,foot.getCenter());go(p,s,Phase.E);}
                FirstEscapeStory.hint(p,"Follow the Cat.");
            }
            case E->{
                var bed=BlockPos.of(n.getLong("BedFoot"));look(cat,bed.getCenter());cat.gameGesture(33,10);
                n.putFloat("DollsPull",MissingParentsCatTimeline.dollPull(t,ShowRecordings.duration(sound)));
                if(done){n.putFloat("DollsPull",1);n.putBoolean("DollsRetrieved",true);s.loop.story.putBoolean("ParentDollsDiscovered",true);go(p,s,Phase.F);}
            }
            case F->{look(cat,p.getEyePosition());if(done){
                if(!StoryTunnel.restore(p,s)){FirstEscapeStory.hint(p,"Preparing the Little Door passage...");break;}
                n.putBoolean("FFinished",true);n.putBoolean("TunnelRestored",true);s.loop.story.putString("MajorStoryPhase","MISSING_PARENTS_CAT_COMPLETE");
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Investigate the Little Door."));go(p,s,Phase.DEPART);
            }}
            case DEPART->{
                FirstEscapeStory.hint(p,"Investigate the Little Door.");
                cat.gameGesture(32,10);
                var door=DoorData.get(p.server).doors.get(s.door);
                if(door!=null&&s.owner.equals(door.owner)){
                    var target=Vec3.atCenterOf(door.pos).add(0,-.5,0);
                    boolean reached=walk(cat,target,1.1);
                    if((reached||t>=200)&&parkInTunnel(p,s)){n.putBoolean("Complete",true);go(p,s,Phase.COMPLETE);}
                }
            }
            default->{}
        }
        if(n.getBoolean("MirrorBroken"))n.putInt("ShatterAge",Math.min(MissingParentsCatTimeline.SHATTER_TICKS+1,n.getInt("ShatterAge")+1));
        if(!n.getBoolean("Complete")&&p.tickCount%5==0)ShowPlaybackNetwork.pose(cat,cat.gameGesture(),n.getInt("Ticks"),n.getFloat("DollsPull"));
        props(p,s);if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        if(p.serverLevel()!=p.server.overworld()||!owns(p,s))return;var n=data(s);var state=phase(s);String sound=voice(state);
        boolean movement=state==Phase.APPROACH||state==Phase.SHATTER;
        if(state==Phase.FOLLOW_MIRROR&&n.contains("MirrorApproach")){
            var cat=cat(p,s);movement=p.distanceToSqr(BlockPos.of(n.getLong("MirrorApproach")).getCenter())<36
                ||cat!=null&&cat.level()==p.level()&&p.distanceToSqr(cat)<144;
        }
        if(state==Phase.FOLLOW_BED&&n.contains("Bed")){
            var cat=cat(p,s);movement=p.distanceToSqr(BlockPos.of(n.getLong("Bed")).getCenter())<49
                ||cat!=null&&cat.level()==p.level()&&p.distanceToSqr(cat)<144;
        }
        if(sound.isEmpty()&&!movement)return;
        // Normal story ticking already pauses while the original Cat is
        // unloaded. The independent safety watchdog must respect that pause,
        // or it can award mirror/doll dialogue without ever seeing the Cat.
        if(!catWatchdogReady(n.getBoolean("Started"),state,cat(p,s)!=null)){
            if(n.getBoolean("Audio")){ShowPlayback.stop(p,s);n.remove("Audio");n.putInt("Ticks",0);}
            n.putString("Blocker","Waiting for the original Cat before advancing the mirror scene.");
            FirstEscapeStory.hint(p,n.getString("Blocker"));
            StoryCutsceneWatchdog.retry(s,"post_escape_cat");
            return;
        }
        // An unavailable staging surface is not a successfully skipped action.
        // Resolve it before the watchdog announces that it continued the scene.
        if(state==Phase.A||state==Phase.FOLLOW_MIRROR||state==Phase.B||state==Phase.WAIT_BREAK){var family=ParentAccess.family(p.server,s.owner);if(family==null||family.bed==null||!mirror(p,s,family.bed)){int failed=n.getInt("MirrorStageFailures")+1;n.putInt("MirrorStageFailures",failed);if(failed>=200){n.putBoolean("MirrorBroken",true);go(p,s,Phase.D);}else FirstEscapeStory.hint(p,"Preparing the Cat's mirror.");return;}}
        // Resolve destination preparation before announcing a successful recovery.
        if(state==Phase.F&&cat(p,s)==null){
            n.putString("Blocker","Waiting for the original Cat before the tunnel can be restored.");
            FirstEscapeStory.hint(p,n.getString("Blocker"));
            return;
        }
        if(state==Phase.F&&!StoryTunnel.restore(p,s)){FirstEscapeStory.hint(p,"Preparing the Little Door passage...");return;}
        int safetyBudget=sound.isEmpty()?600:StoryCutsceneWatchdog.voiceBudget(sound);
        // If the player is following the Cat and its route has made no useful
        // progress for eight seconds, recover that action promptly instead of
        // making them watch the full generic cutscene timeout. Ordinary
        // movement still keeps the longer, authored pacing window.
        if((state==Phase.FOLLOW_BED||state==Phase.FOLLOW_MIRROR)&&n.getInt("StuckTicks")>=160)safetyBudget=160;
        if(!StoryCutsceneWatchdog.overdue(p,s,"post_escape_cat",state.name(),safetyBudget))return;
        ShowPlayback.safetyFinish(p,s,"Cat visit action exceeded its safety window");
        switch(state){
            case APPROACH->go(p,s,Phase.A);
            case A->go(p,s,Phase.FOLLOW_MIRROR);
            case FOLLOW_MIRROR->{props(p,s);if(!n.hasUUID("MirrorActor")||!(p.serverLevel().getEntity(n.getUUID("MirrorActor")) instanceof CinematicPropEntity)){n.putString("LastSafetySkip","Unavailable mirror presentation");go(p,s,Phase.D);}else go(p,s,Phase.B);}
            case B->{n.putInt("Writing",MissingParentsCatTimeline.WRITE_TICKS);n.putBoolean("HelpUsComplete",true);go(p,s,Phase.WAIT_BREAK);}
            case C->{shatter(p,s);go(p,s,Phase.SHATTER);}
            case SHATTER->go(p,s,Phase.D);
            case D->go(p,s,Phase.FOLLOW_BED);
            case FOLLOW_BED->{
                retireShatteredMirror(p,s);
                var actor=cat(p,s);
                if(actor==null){
                    n.putString("Blocker","Waiting for the original Cat before retrieving the parent dolls.");
                    FirstEscapeStory.hint(p,n.getString("Blocker"));
                    StoryCutsceneWatchdog.retry(s,"post_escape_cat");
                    return;
                }
                var family=ParentAccess.family(p.server,s.owner);
                if(family==null||family.bed==null){
                    n.putBoolean("DollsRetrieved",true);
                    s.loop.story.putBoolean("ParentDollsDiscovered",true);
                    go(p,s,Phase.F);
                    return;
                }
                var foot=bedFoot(p,family.bed);
                var front=bedFront(p,family.bed);
                var at=bedApproach(p,actor,foot,front);
                if(at==null){
                    n.putBoolean("DollsRetrieved",true);
                    s.loop.story.putBoolean("ParentDollsDiscovered",true);
                    go(p,s,Phase.F);
                    return;
                }
                actor.setPos(Vec3.atBottomCenterOf(at));
                n.putLong("BedFoot",foot.asLong());n.putString("BedFront",front.getName());
                go(p,s,Phase.E);
            }
            case E->{
                if(cat(p,s)==null){
                    n.putString("Blocker","Waiting for the original Cat before completing the parent-dolls clue.");
                    StoryCutsceneWatchdog.retry(s,"post_escape_cat");
                    return;
                }
                n.putFloat("DollsPull",1);
                n.putBoolean("DollsRetrieved",true);
                s.loop.story.putBoolean("ParentDollsDiscovered",true);
                go(p,s,Phase.F);
            }
            case F->{
                if(cat(p,s)==null){
                    n.putString("Blocker","Waiting for the original Cat before ending its scene.");
                    StoryCutsceneWatchdog.retry(s,"post_escape_cat");
                }else if(StoryTunnel.restore(p,s)){
                    n.putBoolean("FFinished",true);
                    n.putBoolean("TunnelRestored",true);
                    s.loop.story.putString("MajorStoryPhase","MISSING_PARENTS_CAT_COMPLETE");
                    go(p,s,Phase.DEPART);
                }else StoryCutsceneWatchdog.retry(s,"post_escape_cat");
            }
            default->{}
        }
    }
    /** Keep the original protected Cat in the saved crawl for an unlimited return delay. */
    static boolean parkInTunnel(ServerPlayer p,VisitData.Scene s){
        var level=p.server.getLevel(OtherWorld.KEY);if(level==null)return false;
        FirstEscapeStory.ensureDecayedTunnel(p,s);
        var at=Vec3.atBottomCenterOf(FirstEscapeStory.tunnelCell(s,0,0,1));
        var actor=ShowActors.acquire(p,s,"cat",level,at);if(actor==null)return false;
        actor.getNavigation().stop();actor.setPos(at);actor.setDeltaMovement(Vec3.ZERO);
        actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;actor.setInvulnerable(true);actor.setInvisible(true);
        data(s).putBoolean("CatWaitingInTunnel",true);OtherMotherLoop.changed(p);return true;
    }
    static boolean finishForReturn(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(!owns(p,s)||!n.getBoolean("FFinished")||!tunnelRestored(s))return false;
        // Offscreen Cat retirement is presentation, not a lock on the restored passage.
        if(!parkInTunnel(p,s))return false;n.putBoolean("Complete",true);go(p,s,Phase.COMPLETE);return true;
    }
    static boolean mirror(ServerPlayer p,VisitData.Scene s,BlockPos bed){
        var n=data(s);
        if(n.contains("Mirror")&&n.contains("MirrorApproach")&&walkable(p,BlockPos.of(n.getLong("MirrorApproach"))))return true;
        long now=p.serverLevel().getGameTime();if(n.getLong("MirrorRetryAt")>now)return false;n.putLong("MirrorRetryAt",now+20);
        // Old saves may contain a surface selected before its approach was validated.
        n.remove("Mirror");n.remove("MirrorFacing");n.remove("MirrorApproach");
        var candidates=new ArrayList<BlockPos>();
        for(var cell:BlockPos.betweenClosed(bed.offset(-12,0,-12),bed.offset(12,4,12)))
            if(p.serverLevel().hasChunkAt(cell)&&p.level().getBlockState(cell).is(ModBlocks.ROSEWOOD_MIRROR.get()))candidates.add(cell.immutable());
        candidates.sort(Comparator.comparingDouble(cell->cell.distSqr(bed)));
        for(var chosen:candidates){var state=p.level().getBlockState(chosen);var facing=state.hasProperty(HorizontalDirectionalBlock.FACING)?state.getValue(HorizontalDirectionalBlock.FACING):Direction.SOUTH;
            var approach=mirrorApproach(p,chosen,facing,bed.getY());if(approach!=null)return commitMirror(n,chosen,facing,approach);
        }
        // Prefer a wall-backed presentation, but never require the player to
        // build a house: the same entity mirror can stand on a clear bedside
        // floor. No wall/floor/player block is placed or replaced here.
        for(boolean wallRequired:new boolean[]{true,false})for(int radius=2;radius<=8;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int dy:new int[]{0,1,-1}){var chosen=bed.offset(dx,dy,dz);if(!p.serverLevel().hasChunkAt(chosen))continue;
                for(var facing:Direction.Plane.HORIZONTAL){
                    if(wallRequired&&!p.level().getBlockState(chosen.relative(facing.getOpposite()).above()).isSolidRender(p.level(),chosen.relative(facing.getOpposite()).above()))continue;
                    if(!mirrorSpace(p,chosen,facing))continue;var approach=mirrorApproach(p,chosen,facing,bed.getY());
                    if(approach!=null)return commitMirror(n,chosen,facing,approach);
                }
            }
        }
        n.putString("Blocker","A clear floor space beside the parents' bed is needed for the mirror; existing builds are preserved.");return false;
    }
    private static boolean commitMirror(CompoundTag n,BlockPos mirror,Direction facing,BlockPos approach){n.putLong("Mirror",mirror.asLong());n.putString("MirrorFacing",facing.getName());n.putLong("MirrorApproach",approach.asLong());n.remove("MirrorRetryAt");n.remove("Blocker");return true;}
    private static boolean mirrorSpace(ServerPlayer p,BlockPos at,Direction facing){
        var c=Vec3.atBottomCenterOf(at);double x=facing.getAxis()==Direction.Axis.Z?1.2:.25,z=facing.getAxis()==Direction.Axis.Z?.25:1.2;var bounds=new AABB(c.x-x,c.y+.01,c.z-z,c.x+x,c.y+2.95,c.z+z);
        if(!p.serverLevel().hasChunkAt(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ))||!p.serverLevel().hasChunkAt(BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ)))return false;
        return p.level().getBlockState(at.below()).isFaceSturdy(p.level(),at.below(),Direction.UP)&&!p.level().containsAnyLiquid(bounds)&&!p.level().getBlockCollisions(null,bounds).iterator().hasNext();
    }
    private static BlockPos mirrorApproach(ServerPlayer p,BlockPos mirror,Direction facing,int bedY){
        if(Math.abs(mirror.getY()-bedY)>2)return null;
        for(int depth:new int[]{2,3})for(int side:new int[]{0,1,-1})for(int dy:new int[]{0,1,-1}){var at=mirror.relative(facing,depth).relative(facing.getClockWise(),side).atY(bedY+dy);if(p.serverLevel().hasChunkAt(at)&&walkable(p,at))return at;}
        return null;
    }

    static void shatter(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(n.getBoolean("MirrorBroken")||!n.contains("Mirror"))return;n.putBoolean("MirrorBroken",true);n.putInt("ShatterAge",0);
        // The presentation may use a pre-existing Rosewood Mirror in the player's
        // home. Shatter the story prop, not an unrelated permanent block.
        p.serverLevel().playSound(null,at,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,.7F,.8F);
        p.serverLevel().sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,Blocks.GLASS.defaultBlockState()),at.getX()+.5,at.getY()+1.2,at.getZ()+.5,24,.7,.9,.12,.06);
        OtherMotherLoop.changed(p);
    }
    private static CinematicPropEntity prop(ServerPlayer p,VisitData.Scene s,String key,int kind,Vec3 at){
        var n=data(s);if(n.hasUUID(key)){if(n.contains(key+"At"))LoopProps.hold(p.serverLevel(),BlockPos.of(n.getLong(key+"At")));return p.serverLevel().getEntity(n.getUUID(key)) instanceof CinematicPropEntity e&&e.owned(s)?e:null;}
        var e=CinematicPropEntity.TYPE.get().create(p.serverLevel());if(e==null)return null;e.configure(s,kind,(n.getInt("MomSkin")&127)|((n.getInt("DadSkin")&127)<<7));e.setPos(at);
        if(!p.serverLevel().addFreshEntity(e))return null;n.putUUID(key,e.getUUID());n.putLong(key+"At",e.blockPosition().asLong());OtherMotherLoop.changed(p);return e;
    }
    static void props(ServerPlayer p,VisitData.Scene s){
        if(p.serverLevel()!=p.server.overworld())return;var n=data(s);
        if(dollsFinished(s)&&n.hasUUID("MirrorActor")){
            if(n.contains("MirrorActorAt"))LoopProps.hold(p.serverLevel(),BlockPos.of(n.getLong("MirrorActorAt")));
            if(p.serverLevel().getEntity(n.getUUID("MirrorActor")) instanceof CinematicPropEntity mirror&&mirror.owned(s))mirror.discard();
        }
        if(dollsFinished(s)&&n.hasUUID("ParentDolls")){
            if(n.contains("ParentDollsAt"))LoopProps.hold(p.serverLevel(),BlockPos.of(n.getLong("ParentDollsAt")));
            if(p.serverLevel().getEntity(n.getUUID("ParentDolls")) instanceof CinematicPropEntity dolls&&dolls.owned(s))dolls.discard();
        }
        if(!dollsFinished(s)&&phase(s).ordinal()<=Phase.SHATTER.ordinal()&&n.contains("Mirror")){
            var mirror=prop(p,s,"MirrorActor",CinematicPropEntity.PARENT_MIRROR,Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Mirror"))));
            if(mirror!=null){var face=Direction.byName(n.getString("MirrorFacing"));mirror.setYRot(180+(face==null?Direction.SOUTH:face).toYRot());
                mirror.setPos(Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Mirror")))); // Same entity follows a safely re-resolved checkpoint.
                int cue=n.getBoolean("MirrorBroken")?3:phase(s)==Phase.C?2:phase(s).ordinal()>=Phase.B.ordinal()?1:0;
                mirror.frame(cue,cue==3?n.getInt("ShatterAge"):n.getInt("Ticks"),cue==2?MissingParentsCatTimeline.progress(n.getInt("Ticks"),ShowRecordings.duration(voice(Phase.C))):n.getInt("Writing")/(float)MissingParentsCatTimeline.WRITE_TICKS);
            }
        }
        if(!dollsFinished(s)&&n.contains("BedFoot")&&(phase(s).ordinal()>=Phase.E.ordinal()||n.getBoolean("DollsRetrieved"))){
            var foot=BlockPos.of(n.getLong("BedFoot"));var face=Direction.byName(n.getString("BedFront"));if(face==null)face=Direction.SOUTH;
            var base=Vec3.atBottomCenterOf(foot).add(face.getStepX()*.95,.12,face.getStepZ()*.95);
            var dolls=prop(p,s,"ParentDolls",CinematicPropEntity.PARENT_DOLLS,base);
            if(dolls!=null){dolls.setPos(base);dolls.setYRot(face.toYRot());dolls.frame(0,n.getInt("Ticks"),n.getBoolean("DollsRetrieved")?1:n.getFloat("DollsPull"));}
        }
    }
    private static boolean dollsFinished(VisitData.Scene s){return data(s).getBoolean("Complete")||phase(s).ordinal()>=Phase.DEPART.ordinal();}
    private static void retireShatteredMirror(ServerPlayer p,VisitData.Scene s){var n=data(s);if(n.getBoolean("MirrorBroken")&&n.hasUUID("MirrorActor")&&p.serverLevel().getEntity(n.getUUID("MirrorActor")) instanceof CinematicPropEntity e&&e.owned(s))e.discard();}
    static boolean retiredMirror(CinematicPropEntity prop,VisitData.Scene s){return prop.kind()==CinematicPropEntity.PARENT_MIRROR&&dollsFinished(s)&&data(s).hasUUID("MirrorActor")&&prop.getUUID().equals(data(s).getUUID("MirrorActor"));}
    static boolean retiredDolls(CinematicPropEntity prop,VisitData.Scene s){return prop.kind()==CinematicPropEntity.PARENT_DOLLS&&dollsFinished(s)&&data(s).hasUUID("ParentDolls")&&prop.getUUID().equals(data(s).getUUID("ParentDolls"));}
    public static boolean block(ServerPlayer p,BlockPos at){
        var s=OtherMotherLoop.scene(p);if(!eligible(s)||!owns(p,s)||p.serverLevel()!=p.server.overworld()||p.distanceToSqr(at.getCenter())>16)return false;
        var n=data(s);if(n.contains("Mirror")&&BlockPos.of(n.getLong("Mirror")).equals(at)&&phase(s)==Phase.WAIT_BREAK){p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);go(p,s,Phase.C);return true;}return false;
    }
    public static boolean interact(ServerPlayer p,CinematicPropEntity prop){
        var s=OtherMotherLoop.scene(p);if(!eligible(s)||!owns(p,s)||!prop.owned(s)||prop.kind()!=CinematicPropEntity.PARENT_MIRROR||p.distanceToSqr(prop)>16)return false;
        if(phase(s)==Phase.WAIT_BREAK){p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);go(p,s,Phase.C);}return true;
    }
    private static void recover(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null&&owns(p,s)&&data(s).getBoolean("Started")&&!data(s).getBoolean("Complete")){data(s).putBoolean("Recover",true);OtherMotherLoop.changed(p);}}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null&&data(s).getBoolean("Started")&&!data(s).getBoolean("Complete")){ShowPlayback.stop(p,s);recover(p);p.server.overworld().getDataStorage().save();}}}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)recover(p);}
    @SubscribeEvent public static void respawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)recover(p);}
    @SubscribeEvent public static void protectMirror(net.minecraftforge.event.level.BlockEvent.BreakEvent e){
        if(!(e.getPlayer() instanceof ServerPlayer p))return;
        for(var s:VisitData.get(p.server).scenes.values()){var n=data(s);if(n.getBoolean("Started")&&!n.getBoolean("MirrorBroken")&&n.contains("Mirror")&&e.getPos().asLong()==n.getLong("Mirror")&&p.level()==p.server.overworld()){e.setCanceled(true);return;}}
    }
    private MissingParentsCat(){}
}
