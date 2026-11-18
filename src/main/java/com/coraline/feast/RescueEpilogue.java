package com.coraline.feast;

import static com.coraline.feast.RescueChapter.Phase.*;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Canonical-family reunion and reversible, damage-protected original-ghost farewell. */
public final class RescueEpilogue {
    public static boolean protectedVision(VisitData.Scene s){if(!RescueChapter.started(s))return false;var state=RescueChapter.phase(s);return state.ordinal()>=EYES_CLOSE.ordinal()&&state!=RELEASED_SPIRITS&&state!=COMPLETE;}
    static boolean beginAlternate(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        if(!GhostVictimMirrors.owned(p,s,ghost))return false;
        var ending=GhostEscapeEndingProgress.existing(ghost,s.owner,s.door);var d=source(p,s);
        if(s.loop.story.contains("ParentRescue")&&!s.loop.story.contains("ParentRescue",Tag.TAG_COMPOUND))return false;
        var root=ParentRescueStory.data(s);if(root.contains("Continuation")&&!root.contains("Continuation",Tag.TAG_COMPOUND))return false;
        var n=RescueChapter.data(s);
        if(ending==null||!ending.getString("Stage").equals(GhostEscapeEndingProgress.Stage.DOOR_SEALED.name())||!ending.getBoolean("SealedByOwner")
            ||d==null||p.serverLevel()!=FamilyData.level(p.server,d.dimension)||!RescueEyes.all(s))return false;
        var state=p.serverLevel().getBlockState(d.pos);
        if(!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())||state.getValue(com.coraline.block.TinyDoorBlock.OPEN)||state.getValue(com.coraline.block.TinyDoorBlock.PORTAL)
            ||!(p.serverLevel().getBlockEntity(d.pos) instanceof DoorBlockEntity tile)||!s.door.equals(tile.doorId))return false;
        if(n.contains("AlternateGhostAttempt")&&(!n.hasUUID("AlternateGhostAttempt")||!ending.getUUID("Attempt").equals(n.getUUID("AlternateGhostAttempt")))){ending.putString("Blocker","The saved epilogue belongs to another rescue attempt.");return false;}
        var rescue=ParentRescueStory.data(s);if(rescue.contains("Scenery")&&!rescue.contains("Scenery",Tag.TAG_COMPOUND)){ending.putString("Blocker","The saved rescue scenery is incompatible; existing references were preserved.");return false;}
        FirstEscapeScene.reconcileOriginalGhostReferences(p,s);
        var current=FirstEscapeScene.context(s);var scenery=ParentRescueStory.scenery(s);
        for(String key:java.util.List.of("House","LittleDoor","Prison","Ghost0","Ghost1","Ghost2","Ghost0Position","Ghost1Position","Ghost2Position"))if(!scenery.contains(key)&&current.contains(key))scenery.put(key,current.get(key).copy());
        var family=ParentAccess.family(p.server,s.owner);var mom=family==null?null:parent(p,family,true);var dad=family==null?null:parent(p,family,false);
        if(family==null||mom==null||dad==null||!family.id.equals(mom.familyId)||!family.id.equals(dad.familyId)){
            ending.putString("Blocker","Waiting for both original real parents; no replacement parents created.");return false;
        }
        var bond=com.coraline.doll.DollBondData.get(p.server).bonds.get(s.owner);
        boolean captured=family.storyMissing||bond!=null&&bond.storyMissing||mom.getPersistentData().contains("FirstEscapeHidden")||dad.getPersistentData().contains("FirstEscapeHidden");
        if(captured){
            if(!n.getBoolean("AlternateParentsPrepared")){n.remove("ParentsStaged");n.putBoolean("AlternateParentsPrepared",true);}
            if(!stageParents(p,s,family,d))return false;
        }else if(mom.isInvisible()||dad.isInvisible()||mom.level()!=FamilyData.level(p.server,d.dimension)||dad.level()!=FamilyData.level(p.server,d.dimension)){
            ending.putString("Blocker","Waiting for the original parents' return to the real world.");return false;
        }
        var firstPhase=captured?REUNION_A:RescueEyes.ledger(s).getBoolean("Released")?RELEASED_SPIRITS:CAT_NOTICE;
        if(!GhostEscapeEndingProgress.beginEpilogue(ghost,s.owner,s.door,n,family.id,mom.getUUID(),dad.getUUID(),true,true,firstPhase))return false;
        n.remove("Recover");n.remove("Audio");n.remove("AudioTail");RescueBosses.hideBar(p);ending.remove("Blocker");
        if(!captured)n.putBoolean("ParentsReturned",true);RescueChapter.go(p,s,firstPhase);
        OtherMotherLoop.changed(p);return true;
    }
    private static boolean reunionReady(VisitData.Scene s){return RescueChapter.data(s).getBoolean("PermanentlySealed")&&RescueEyes.all(s)
        &&(RescueChapter.data(s).getBoolean("BangingDone")||GhostEscapeEnding.epilogueActive(s));}
    private static DoorData.Door source(ServerPlayer p,VisitData.Scene s){var d=DoorData.get(p.server).doors.get(s.door);return d!=null&&s.owner.equals(d.owner)?d:null;}
    private static ParentEntity parent(ServerPlayer p,FamilyData.Family f,boolean mom){
        var id=mom?f.mom:f.dad;if(id==null)return null;var l=FamilyData.level(p.server,mom?f.momDim:f.dadDim);if(l==null)return null;LoopProps.hold(l,mom?f.momPos:f.dadPos);return l.getEntity(id) instanceof ParentEntity entity&&!entity.isOtherParent()?entity:null;
    }
    public static boolean begin(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);var d=source(p,s);if(d==null||p.serverLevel()!=FamilyData.level(p.server,d.dimension)||!n.getBoolean("PermanentlySealed")||!n.getBoolean("BangingDone")||!RescueEyes.all(s))return false;
        var family=ParentAccess.family(p.server,s.owner);if(family==null||parent(p,family,true)==null||parent(p,family,false)==null){n.putString("Blocker","Waiting for both saved real parents to load; no replacements created.");return false;}
        if(!stageParents(p,s,family,d))return false;n.remove("Blocker");RescueChapter.go(p,s,REUNION_A);return true;
    }
    private static boolean stageParents(ServerPlayer p,VisitData.Scene s,FamilyData.Family family,DoorData.Door door){
        var n=RescueChapter.data(s);if(n.getBoolean("ParentsStaged"))return true;var target=FamilyData.level(p.server,door.dimension);if(target==null)return false;
        var mom=parent(p,family,true);var dad=parent(p,family,false);if(mom==null||dad==null)return false;
        var momAt=OtherWorld.safePlayer(target,door.pos.offset(-2,0,2));var dadAt=OtherWorld.safePlayer(target,door.pos.offset(2,0,2));if(momAt==null||dadAt==null){n.putString("Blocker","Make room for both real parents near your Little Door.");return false;}
        for(var actor:java.util.List.of(mom,dad)){var saved=actor.getPersistentData();if(!saved.contains("ReunionFlags")){var flags=saved.contains("FirstEscapeHidden")?saved.getCompound("FirstEscapeHidden").copy():new CompoundTag();if(!saved.contains("FirstEscapeHidden")){flags.putBoolean("Invisible",actor.isInvisible());flags.putBoolean("NoAI",actor.isNoAi());flags.putBoolean("Silent",actor.isSilent());}saved.put("ReunionFlags",flags);}saved.remove("FirstEscapeHidden");}
        var movedMom=ShowTravel.guest(mom,p,s,target,Vec3.atBottomCenterOf(momAt),false,"Reunion");var movedDad=ShowTravel.guest(dad,p,s,target,Vec3.atBottomCenterOf(dadAt),false,"Reunion");
        // Preserve successful partial movement for a safe retry if the second
        // transfer fails, instead of leaving the first UUID at a stale anchor.
        if(movedMom instanceof ParentEntity a&&a.getUUID().equals(family.mom)){family.momPos=a.blockPosition();family.momDim=target.dimension().location().toString();}
        if(movedDad instanceof ParentEntity b&&b.getUUID().equals(family.dad)){family.dadPos=b.blockPosition();family.dadDim=target.dimension().location().toString();}FamilyData.get(p.server).setDirty();
        if(!(movedMom instanceof ParentEntity a)||!(movedDad instanceof ParentEntity b)||!a.getUUID().equals(family.mom)||!b.getUUID().equals(family.dad)){n.putString("Blocker","A saved parent could not reach the reunion; no replacement created.");return false;}
        family.momPos=a.blockPosition();family.dadPos=b.blockPosition();family.momDim=family.dadDim=target.dimension().location().toString();family.storyMissing=false;n.putBoolean("ParentsStaged",true);
        for(var actor:java.util.List.of(a,b)){var flags=actor.getPersistentData().getCompound("ReunionFlags");if(!flags.contains("PhysicalFlags")){flags.putBoolean("PhysicalFlags",true);flags.putBoolean("NoGravity",actor.isNoGravity());flags.putBoolean("NoPhysics",actor.noPhysics);}actor.setInvisible(false);actor.setSilent(true);actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;actor.reunionFrost(1);actor.lookAtStoryPlayer(p);target.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,actor.getX(),actor.getY()+1.2,actor.getZ(),8,.25,.4,.25,.012);}FamilyData.get(p.server).setDirty();OtherMotherLoop.changed(p);return true;
    }
    /** Real family actors stay present during A/B, then resume their own saved flags/jobs. */
    public static boolean controlParent(ParentEntity actor,FamilyData.Family family){
        if(actor.level().isClientSide||family.owner==null)return false;var s=VisitData.get(actor.getServer()).scenes.get(family.owner);if(!RescueChapter.started(s))return false;var state=RescueChapter.phase(s);if(state!=REUNION_A&&state!=REUNION_B)return false;if(!actor.getUUID().equals(family.mom)&&!actor.getUUID().equals(family.dad))return false;
        if(GhostEscapeEnding.epilogueActive(s)&&!GhostEscapeEndingProgress.parentsMatch(GhostVictimEntry.data(s),s.owner,s.door,family.id,family.mom,family.dad))return false;
        var flags=actor.getPersistentData().getCompound("ReunionFlags");if(!flags.contains("PhysicalFlags")){flags.putBoolean("PhysicalFlags",true);flags.putBoolean("NoGravity",actor.isNoGravity());flags.putBoolean("NoPhysics",actor.noPhysics);actor.getPersistentData().put("ReunionFlags",flags);}
        if(!flags.contains("NavigationFlags")){flags.putBoolean("NavigationFlags",true);flags.putBoolean("OpenDoors",actor.getNavigation().getNodeEvaluator().canOpenDoors());flags.putBoolean("PassDoors",actor.getNavigation().getNodeEvaluator().canPassDoors());}
        actor.setInvisible(false);actor.setNoAi(false);actor.setSilent(true);actor.setNoGravity(false);actor.noPhysics=false;var n=RescueChapter.data(s);float melt=state==REUNION_B?Math.min(1,n.getInt("Ticks")/(float)Math.max(1,ShowRecordings.duration("reunion_b"))):0;actor.reunionFrost(1-melt);
        if(actor.tickCount%20==0){if(actor.getUUID().equals(family.mom)){family.momPos=actor.blockPosition();family.momDim=actor.level().dimension().location().toString();}else{family.dadPos=actor.blockPosition();family.dadDim=actor.level().dimension().location().toString();}FamilyData.get(actor.getServer()).setDirty();}
        var player=actor.getServer().getPlayerList().getPlayer(family.owner);
        if(player==null||player.level()!=actor.level()){actor.getNavigation().stop();return true;}
        // Two separate approach anchors. Navigation owns movement; no recurring
        // teleport, through-wall velocity or forced body-facing camera lock.
        if(actor.distanceToSqr(player)>20.25){if(actor.tickCount%10==0){var forward=player.getLookAngle().multiply(1,0,1).normalize();var side=new Vec3(-forward.z,0,forward.x).scale(actor.isMom()?-1.8:1.8);var at=player.position().add(side).add(forward.scale(.7));if(actor.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}actor.getNavigation().moveTo(at.x,at.y,at.z,.8);}}else actor.getNavigation().stop();
        if(actor.tickCount%20<10)actor.getLookControl().setLookAt(player,12,12);actor.familyGesture(actor.isMom()==(state==REUNION_A)?69:68,5);
        var level=(ServerLevel)actor.level();if(actor.tickCount%40==0&&actor.reunionFrost()>.05F){level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,actor.getX(),actor.getY()+1.5,actor.getZ(),2,.23,.2,.23,.006);var breath=actor.getEyePosition().add(actor.getLookAngle().scale(.22));level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,breath.x,breath.y-.08,breath.z,1,.025,.015,.025,.004);}
        if(state==REUNION_B&&melt>.1F&&melt<.95F&&actor.tickCount%20==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER,actor.getX(),actor.getY()+1.35,actor.getZ(),2,.2,.15,.17,0);
        return true;
    }
    private static void releaseParents(ServerPlayer p,FamilyData.Family family){for(boolean mom:new boolean[]{true,false}){var actor=parent(p,family,mom);if(actor==null)continue;actor.reunionFrost(0);actor.familyGesture(0,0);actor.getNavigation().stop();var tag=actor.getPersistentData();if(!tag.contains("ReunionFlags"))continue;var flags=tag.getCompound("ReunionFlags");actor.setInvisible(flags.getBoolean("Invisible"));actor.setNoAi(flags.getBoolean("NoAI"));actor.setSilent(flags.getBoolean("Silent"));if(flags.getBoolean("PhysicalFlags")){actor.setNoGravity(flags.getBoolean("NoGravity"));actor.noPhysics=flags.getBoolean("NoPhysics");}if(flags.getBoolean("NavigationFlags")){actor.getNavigation().getNodeEvaluator().setCanOpenDoors(flags.getBoolean("OpenDoors"));actor.getNavigation().getNodeEvaluator().setCanPassDoors(flags.getBoolean("PassDoors"));}tag.remove("ReunionFlags");}}
    public static void tick(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state,int t,boolean done){
        var n=RescueChapter.data(s);var d=source(p,s);if(d==null&&(state==REUNION_A||state==REUNION_B||state==CAT_NOTICE))return;
        if(protectedVision(s)){if(!n.contains("VisionReturn"))saveVision(p,s);holdVision(p,s);if(p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY)&&state!=EYES_CLOSE){recoverVision(p,s);return;}}
        switch(state){
            case REUNION_A,REUNION_B->{
                if(!reunionReady(s))return;
                var family=ParentAccess.family(p.server,s.owner);if(family==null)return;var mom=parent(p,family,true);var dad=parent(p,family,false);if(mom==null||dad==null){n.putString("Blocker","Waiting for your real parents to load.");return;}
                if(GhostEscapeEnding.epilogueActive(s)&&!GhostEscapeEndingProgress.parentsMatch(GhostVictimEntry.data(s),s.owner,s.door,family.id,family.mom,family.dad)){n.putString("Blocker","The saved reunion belongs to the original parent pair.");return;}
                family.storyMissing=false;FamilyData.get(p.server).setDirty();com.coraline.doll.DollBondData.get(p.server).destroyAfterBeldam(s.owner);
                RescueProps.retire(p,s,"SnowGlobe");
                if(done){if(state==REUNION_A)RescueChapter.go(p,s,REUNION_B);else{releaseParents(p,family);RescuedParentBenefits.apply(p.server,family);n.putBoolean("ParentsReturned",true);RescueProps.hide(p,s,"SnowGlobe");RescueChapter.go(p,s,RescueEyes.ledger(s).getBoolean("Released")?RELEASED_SPIRITS:CAT_NOTICE);}}
            }
            case RELEASED_SPIRITS->{
                if(!n.getBoolean("ParentsReturned")&&!n.getBoolean("FarewellComplete"))return;
                if(n.contains("VisionReturn")&&!n.getBoolean("VisionRestored")&&!restoreVision(p,s))return;
                if(!n.contains("VisionReturn"))n.putBoolean("VisionRestored",true);
                n.putBoolean("FarewellComplete",true);
                if(!n.getBoolean("FarewellCatDeparted"))n.putBoolean("FarewellCatCleanupPending",true);
                departAfterWaking(p,s);
                if(RescueEyes.release(p,s))completeFarewell(p,s);

            }
            case CAT_NOTICE->{
                var safe=OtherWorld.safePlayer(p.serverLevel(),d.pos.offset(6,0,5));if(safe==null)return;var cat=ShowActors.acquire(p,s,"cat",p.serverLevel(),Vec3.atBottomCenterOf(safe));if(cat==null)return;
                if(!n.getBoolean("FarewellCatPlaced")){cat.setPos(Vec3.atBottomCenterOf(safe));n.putBoolean("FarewellCatPlaced",true);}cat.setInvisible(false);ShowActors.position(cat,cat.position(),cat.getYRot(),30,t,1);n.putInt("Cat",cat.getId());
                if(p.distanceToSqr(cat)<25&&RescueEncounters.gaze(p,cat.getEyePosition(),8,.8))RescueChapter.go(p,s,FAREWELL_A);
            }
            case FAREWELL_A->{if(done)RescueChapter.go(p,s,FAREWELL_B);}
            case FAREWELL_B->{
                if(ShowPlayback.audiblePosition(s)>=460&&!n.getBoolean("FarewellEyesClosing")){saveVision(p,s);n.putBoolean("FarewellEyesClosing",true);OtherMotherLoop.changed(p);}
                if(done){saveVision(p,s);if(p.server.getLevel(ActivityWorld.KEY)!=null){enterVision(p,s);RescueChapter.go(p,s,CELESTIAL_OPEN);}else RescueChapter.go(p,s,EYES_CLOSE);}
            }
            case EYES_CLOSE->{if(t>=40&&(visionReady(p,s)||t>=200)){if(p.server.getLevel(ActivityWorld.KEY)!=null){enterVision(p,s);RescueChapter.go(p,s,CELESTIAL_OPEN);}else if(restoreVision(p,s))RescueChapter.go(p,s,RELEASED_SPIRITS);}}
            case CELESTIAL_OPEN->{if(t>=40)RescueChapter.go(p,s,ORBS);}
            case ORBS->{for(int i=0;i<3;i++){var ghost=ghost(p,s,i);if(ghost==null){n.putBoolean("MissingVisionActors",true);continue;}ghost.setInvisible(t<60);ghost.frame(3,t,Math.max(0,Math.min(1,(t-60)/40F)));}if(t>=100)RescueChapter.go(p,s,FAREWELL_C);}
            case FAREWELL_C->{
                n.putBoolean("GhostsSad",t>=240);n.putFloat("VisionBlur",Math.max(0,(t-(ShowRecordings.duration("farewell_c")-100))/100F));
                for(int i=0;i<3;i++){var ghost=ghost(p,s,i);if(ghost!=null){ghost.setInvisible(false);ghost.frame(t>=240?5:4,t,1);}}
                if(done){
                    if(!restoreVision(p,s))return;
                    for(int i=0;i<3;i++){
                        var ghost=findGhost(p,s,i);
                        if(ghost!=null){ghost.setInvisible(true);ghost.frame(5,t,0);}
                        var projection=FirstEscapeScene.prop(p,s,"FarewellProjection"+i);
                        if(projection!=null)projection.discard();
                    }
                    n.putBoolean("FarewellComplete",true);n.putFloat("VisionBlur",0);
                    if(!n.getBoolean("FarewellCatDeparted"))n.putBoolean("FarewellCatCleanupPending",true);
                    departAfterWaking(p,s);
                    if(RescueEyes.release(p,s))completeFarewell(p,s);
                    else RescueChapter.go(p,s,RELEASED_SPIRITS);
                }
            }
            default->{}
        }
        if(state==FAREWELL_A||state==FAREWELL_B){var cat=ShowActors.actor(p,s,"cat");if(cat!=null){cat.setInvisible(false);ShowPlaybackNetwork.pose(cat,31,t,1);}}
    }
    private static void completeFarewell(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);n.putBoolean("FarewellComplete",true);
        if(!n.getBoolean("FarewellCatDeparted"))n.putBoolean("FarewellCatCleanupPending",true);
        RescueChapter.go(p,s,COMPLETE);
        departAfterWaking(p,s);
        PalaceJourney.afterWake(p,s);
    }
    static void departAfterWaking(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        if(n.getBoolean("FarewellCatDeparted"))return;
        var visible=ShowActors.actor(p,s,"cat");
        if(visible!=null)visible.setInvisible(true);
        if(ShowActors.departHiddenCat(p,s)){
            n.putBoolean("FarewellCatDeparted",true);n.remove("FarewellCatCleanupPending");OtherMotherLoop.changed(p);
            return;
        }
    }
    private static void saveVision(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);if(n.contains("VisionReturn"))return;var saved=new CompoundTag();saved.putString("Dimension",p.level().dimension().location().toString());saved.putDouble("X",p.getX());saved.putDouble("Y",p.getY());saved.putDouble("Z",p.getZ());saved.putFloat("Yaw",p.getYRot());saved.putFloat("Pitch",p.getXRot());saved.putString("Pose",p.getPose().name());saved.putFloat("Health",p.getHealth());saved.putInt("Food",p.getFoodData().getFoodLevel());saved.putFloat("Saturation",p.getFoodData().getSaturationLevel());saved.putBoolean("Invulnerable",p.isInvulnerable());saved.putBoolean("NoGravity",p.isNoGravity());n.put("VisionReturn",saved);OtherMotherLoop.changed(p);
        // Set before the first eyelid frame. Survival remains Survival; no spectator shortcut.
        if(p.getForcedPose()!=null)n.getCompound("VisionReturn").putString("ForcedPose",p.getForcedPose().name());
        p.setInvulnerable(true);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
    }
    private static BlockPos visionCenter(ServerPlayer p,VisitData.Scene s){return ShowSiteData.get(p.server).center(s.owner,false).offset(150,90,100);}
    private static boolean visionReady(ServerPlayer p,VisitData.Scene s){var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return false;var c=visionCenter(p,s);l.getChunkAt(c);for(int i=0;i<3;i++)if(findGhost(p,s,i)==null)return false;return true;}
    private static void enterVision(ServerPlayer p,VisitData.Scene s){var c=visionCenter(p,s);var l=p.server.getLevel(ActivityWorld.KEY);l.getChunkAt(c);p.stopRiding();p.setForcedPose(null);p.setPose(Pose.STANDING);LoopProps.hold(l,c);p.teleportTo(l,c.getX()+.5,c.getY(),c.getZ()+.5,0,0);RescueChapter.data(s).putLong("VisionAt",c.asLong());}
    private static void holdVision(ServerPlayer p,VisitData.Scene s){p.setInvulnerable(true);p.setNoGravity(true);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);var n=RescueChapter.data(s);n.putBoolean("VisionRestored",false);if(p.serverLevel()==p.server.getLevel(ActivityWorld.KEY)&&n.contains("VisionAt")&&RescueChapter.phase(s)!=EYES_CLOSE){var at=BlockPos.of(n.getLong("VisionAt"));if(p.distanceToSqr(at.getCenter())>1)p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());}}
    static java.util.UUID savedGhostReference(CompoundTag original,CompoundTag current,int index){
        if(index<0||index>2)return null;String key="Ghost"+index;
        return original.hasUUID(key)?original.getUUID(key):current.hasUUID(key)?current.getUUID(key):null;
    }
    private static CinematicPropEntity findGhost(ServerPlayer p,VisitData.Scene s,int index){
        var original=FirstEscapeStory.data(s);var current=FirstEscapeScene.context(s);var id=savedGhostReference(original,current,index);if(id==null)return null;
        var other=p.server.getLevel(OtherWorld.KEY);if(other!=null)for(var scope:java.util.List.of(original,current)){
            if(scope.contains("Prison"))LoopProps.hold(other,BlockPos.of(scope.getLong("Prison")));
            if(scope.contains("Ghost"+index+"Position"))LoopProps.hold(other,BlockPos.of(scope.getLong("Ghost"+index+"Position")));
        }
        for(var level:p.server.getAllLevels())if(level.getEntity(id) instanceof CinematicPropEntity entity&&entity.canonicalGhost(s))return entity;
        return null;
    }
    private static CinematicPropEntity ghost(ServerPlayer p,VisitData.Scene s,int index){var old=findGhost(p,s,index);
        if(old==null){
            // A legacy saved UUID can outlive its entity. Project the vision
            // without replacing that canonical identity or its quest ledger.
            var stage=p.server.getLevel(ActivityWorld.KEY);
            if(stage==null)return null;
            var at=Vec3.atBottomCenterOf(visionCenter(p,s)).add((index-1)*1.45,0,4);
            return FirstEscapeScene.ensure(p,s,"FarewellProjection"+index,CinematicPropEntity.GHOST_CHILD,index,stage,at);
        }

        if(old==null)return null;var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return null;var c=visionCenter(p,s);var at=Vec3.atBottomCenterOf(c).add((index-1)*2,0,4);Entity result=old;if(old.level()!=l)result=old.changeDimension(l,new net.minecraftforge.common.util.ITeleporter(){@Override public Entity placeEntity(Entity entity,ServerLevel current,ServerLevel destination,float yaw,java.util.function.Function<Boolean,Entity> reposition){var moved=reposition.apply(false);if(moved!=null)moved.setPos(at);return moved;}});if(!(result instanceof CinematicPropEntity ghost))return null;if(!ghost.getUUID().equals(old.getUUID()))throw new IllegalStateException("Ghost identity changed during farewell");LoopProps.hold(l,BlockPos.containing(at));ghost.setPos(at);FirstEscapeScene.context(s).putLong("Ghost"+index+"Position",ghost.blockPosition().asLong());return ghost;}
    private static boolean restoreVision(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);var saved=n.getCompound("VisionReturn");var l=FamilyData.level(p.server,saved.getString("Dimension"));if(l==null)return false;
        var original=new Vec3(saved.getDouble("X"),saved.getDouble("Y"),saved.getDouble("Z"));if(!Double.isFinite(original.x)||!Double.isFinite(original.y)||!Double.isFinite(original.z)){n.putString("Blocker","The saved farewell return anchor is invalid.");return false;}
        Pose pose;try{pose=Pose.valueOf(saved.getString("Pose"));}catch(IllegalArgumentException bad){pose=Pose.STANDING;}
        l.getChunkAt(BlockPos.containing(original));Vec3 target=original;
        if(!safeReturn(l,p,pose,target)){var safe=OtherWorld.safePlayer(l,BlockPos.containing(original));if(safe==null||!safeReturn(l,p,pose,Vec3.atBottomCenterOf(safe))){n.putString("Blocker","Your saved farewell return has insufficient body clearance or safe footing.");return false;}target=Vec3.atBottomCenterOf(safe);}
        p.teleportTo(l,target.x,target.y,target.z,saved.getFloat("Yaw"),saved.getFloat("Pitch"));
        if(p.serverLevel()!=l||p.position().distanceToSqr(target)>.25||!safeReturn(l,p,pose,p.position())){
            n.putString("Blocker","Your saved return is still being prepared. Your dream state is preserved and will retry safely.");OtherMotherLoop.changed(p);return false;
        }
        p.setInvulnerable(saved.getBoolean("Invulnerable"));p.setNoGravity(saved.getBoolean("NoGravity"));p.setHealth(Math.min(p.getMaxHealth(),saved.getFloat("Health")));p.getFoodData().setFoodLevel(saved.getInt("Food"));p.getFoodData().setSaturation(saved.getFloat("Saturation"));p.setForcedPose(null);if(saved.contains("ForcedPose"))try{p.setForcedPose(Pose.valueOf(saved.getString("ForcedPose")));}catch(IllegalArgumentException ignored){}p.setPose(pose);n.remove("VisionAt");n.putBoolean("VisionRestored",true);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);OtherMotherLoop.changed(p);return true;
    }
    private static boolean safeReturn(ServerLevel level,ServerPlayer player,Pose pose,Vec3 at){var body=player.getDimensions(pose).makeBoundingBox(at);return level.noCollision(player,body)&&level.getBlockCollisions(player,new AABB(body.minX,body.minY-.10,body.minZ,body.maxX,body.minY,body.maxZ)).iterator().hasNext();}
    public static void recoverVision(ServerPlayer p,VisitData.Scene s){if(!RescueChapter.owns(p,s)||!RescueChapter.data(s).contains("VisionReturn"))return;holdVision(p,s);if(restoreVision(p,s))RescueChapter.go(p,s,EYES_CLOSE);}
    private RescueEpilogue(){}
}
