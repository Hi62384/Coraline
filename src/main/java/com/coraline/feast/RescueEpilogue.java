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
    /** Persisted ParentsStaged is only a hint: both real actors must still be
     * in this reunion world and near their original Little Door checkpoint. */
    static boolean parentsOnReunionStage(boolean staged,boolean motherInWorld,boolean fatherInWorld,double motherDistanceSq,double fatherDistanceSq){
        return staged&&motherInWorld&&fatherInWorld
                &&Double.isFinite(motherDistanceSq)&&Double.isFinite(fatherDistanceSq)
                &&motherDistanceSq<=36*36&&fatherDistanceSq<=36*36;
    }
    /** The viewer must be able to see the reunion, not merely be in a saved phase. */
    static boolean reunionViewerNear(boolean correctDimension,double distanceSquared){
        return correctDimension&&Double.isFinite(distanceSquared)&&distanceSquared<=64*64;
    }
    private static boolean pauseReunion(ServerPlayer p,VisitData.Scene s,String reason){
        var n=RescueChapter.data(s);
        if(n.getBoolean("Audio")){ShowPlayback.stop(p,s);n.remove("Audio");n.remove("AudioTail");n.putInt("Ticks",0);}
        n.putString("Blocker",reason);
        return false;
    }
    static boolean requiresEpilogueStage(RescueChapter.Phase phase){
        return phase==REUNION_A||phase==REUNION_B||phase==CAT_NOTICE
                ||phase==FAREWELL_A||phase==FAREWELL_B;
    }
    /** Return the SAME saved Cat to a safe visible stage after interrupted
     * dimension travel; never mint a stand-in or advance absent-Cat dialogue. */
    private static boolean stageFarewellCat(ServerPlayer p,VisitData.Scene s,DoorData.Door door,ServerLevel world){
        var n=RescueChapter.data(s);
        var cat=ShowActors.actor(p,s,"cat");
        boolean staged=n.getBoolean("FarewellCatPlaced")&&cat!=null&&cat.isAlive()
                &&cat.level()==world&&cat.distanceToSqr(door.pos.getCenter())<=36*36
                &&world.noCollision(cat,cat.getBoundingBox());
        if(staged)return true;
        Vec3 candidate=null;
        for(var offset:java.util.List.of(door.pos.offset(6,0,5),door.pos.offset(-5,0,4),
                door.pos.south(4),door.pos.north(4))){
            var safe=nearReturn(world,p,Pose.STANDING,offset,2);
            if(safe==null||safe.distanceToSqr(p.position())<2)continue;
            var hit=world.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),safe.add(0,.55,0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS){candidate=safe;break;}
        }
        if(candidate==null){n.putString("Blocker","Clear a visible path near the original Little Door for the Cat.");return false;}
        cat=ShowActors.recoverCat(p,s,world,candidate);
        if(cat==null||cat.level()!=world||!cat.isAlive()){
            n.putString("Blocker","Waiting for the original Cat before the farewell.");
            return false;
        }
        cat.stopRiding();
        cat.setDeltaMovement(Vec3.ZERO);
        cat.setPos(candidate);
        cat.setInvisible(false);
        n.putInt("Cat",cat.getId());
        n.putBoolean("FarewellCatPlaced",true);
        OtherMotherLoop.changed(p);
        return true;
    }
    /** Both ordinary story ticks AND the independent watchdog use this gate.
     * An offworld farewell must never save an unusable dream-return dimension. */
    static boolean presentationReady(ServerPlayer p,VisitData.Scene s){
        var state=RescueChapter.phase(s);
        if(!requiresEpilogueStage(state))return true;
        if(!reunionReady(s))return pauseReunion(p,s,"Waiting for the original sealed-door and three-eye reunion receipt.");
        var door=source(p,s);var family=ParentAccess.family(p.server,s.owner);
        var world=door==null?null:FamilyData.level(p.server,door.dimension);
        if(door==null||family==null||world==null||world!=p.server.overworld())
            return pauseReunion(p,s,"Waiting for the saved real-world Little Door and family.");
        if(p.serverLevel()!=world&&!RescueChapter.transfer(p,world,door.pos))
            return pauseReunion(p,s,"Your reunion return is blocked. Clear a safe landing near the original Little Door.");
        if(!reunionViewerNear(p.serverLevel()==world,p.distanceToSqr(door.pos.getCenter())))
            return pauseReunion(p,s,"Return near the original Little Door to see your parents.");
        if(state==REUNION_A||state==REUNION_B){
            if(!stageParents(p,s,family,door)){
                var blocker=RescueChapter.data(s).getString("Blocker");
                return pauseReunion(p,s,blocker.isEmpty()?"Waiting for both original parents to reach the reunion.":blocker);
            }
        }else{
            if(!RescueChapter.data(s).getBoolean("ParentsReturned"))
                return pauseReunion(p,s,"Waiting for the original parents to finish their reunion.");
            if(!stageFarewellCat(p,s,door,world)){
                var blocker=RescueChapter.data(s).getString("Blocker");
                return pauseReunion(p,s,blocker.isEmpty()?"Waiting for the original Cat to reach the farewell.":blocker);
            }
        }
        RescueChapter.data(s).remove("Blocker");
        return true;
    }
    public static boolean begin(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);var d=source(p,s);if(d==null||p.serverLevel()!=FamilyData.level(p.server,d.dimension)||!n.getBoolean("PermanentlySealed")||!n.getBoolean("BangingDone")||!RescueEyes.all(s))return false;
        var family=ParentAccess.family(p.server,s.owner);if(family==null||parent(p,family,true)==null||parent(p,family,false)==null){n.putString("Blocker","Waiting for both saved real parents to load; no replacements created.");return false;}
        if(!stageParents(p,s,family,d))return false;n.remove("Blocker");RescueChapter.go(p,s,REUNION_A);return true;
    }
    private static boolean stageParents(ServerPlayer p,VisitData.Scene s,FamilyData.Family family,DoorData.Door door){
        var n=RescueChapter.data(s);var target=FamilyData.level(p.server,door.dimension);if(target==null)return false;
        var mom=parent(p,family,true);var dad=parent(p,family,false);if(mom==null||dad==null)return false;
        if(parentsOnReunionStage(n.getBoolean("ParentsStaged"),mom.level()==target,dad.level()==target,
                mom.blockPosition().distSqr(door.pos),dad.blockPosition().distSqr(door.pos)))return true;
        n.remove("ParentsStaged");
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
        if(protectedVision(s)&&!n.getBoolean("DreamDialogueComplete")&&!n.getBoolean("VisionRestored")){
            if(!n.contains("VisionReturn"))saveVision(p,s);
            holdVision(p,s);
            if(p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY)&&state!=EYES_CLOSE){
                enterVision(p,s);
                return;
            }
        }
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
                FirstEscapeStory.hint(p,"Approach the Cat and look at it.");
                var level=p.serverLevel();
                var cat=ShowActors.actor(p,s,"cat");
                if(cat==null||cat.level()!=level||!n.getBoolean("FarewellCatPlaced")
                        ||!level.noCollision(cat,cat.getBoundingBox())
                        ||p.tickCount%40==0&&p.distanceToSqr(cat)<100&&!p.hasLineOfSight(cat)){
                    Vec3 candidate=null;
                    for(var offset:java.util.List.of(d.pos.offset(6,0,5),d.pos.offset(-5,0,4),d.pos.south(4),d.pos.north(4))){
                        var safe=nearReturn(level,p,Pose.STANDING,offset,2);
                        if(safe==null||safe.distanceToSqr(p.position())<2)continue;
                        var hit=level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),safe.add(0,.55,0),
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
                        if(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS){candidate=safe;break;}
                    }
                    if(candidate==null){FirstEscapeStory.hint(p,"Move into a clear area near the Little Door so the Cat can approach.");return;}
                    cat=ShowActors.recoverCat(p,s,level,candidate);
                    if(cat==null||cat.level()!=level){FirstEscapeStory.hint(p,"Waiting for the original Cat to return.");return;}
                    cat.setPos(candidate);
                    n.putBoolean("FarewellCatPlaced",true);OtherMotherLoop.changed(p);
                }
                cat.setInvisible(false);ShowActors.position(cat,cat.position(),cat.getYRot(),30,t,1);n.putInt("Cat",cat.getId());
                if(p.distanceToSqr(cat)<64&&RescueEncounters.gaze(p,cat.getEyePosition(),8,.55))RescueChapter.go(p,s,FAREWELL_A);
            }
            case FAREWELL_A->{if(done)RescueChapter.go(p,s,FAREWELL_B);}
            case FAREWELL_B->{
                if(ShowPlayback.audiblePosition(s)>=460&&!n.getBoolean("FarewellEyesClosing")){saveVision(p,s);n.putBoolean("FarewellEyesClosing",true);OtherMotherLoop.changed(p);}
                if(done){saveVision(p,s);RescueChapter.go(p,s,EYES_CLOSE);}
            }
            case EYES_CLOSE->{
                if(t>=40&&enterVision(p,s))RescueChapter.go(p,s,CELESTIAL_OPEN);
            }
            case CELESTIAL_OPEN->{if(t>=40)RescueChapter.go(p,s,ORBS);}
            case ORBS->{
                var actors=visionActors(p,s);
                if(actors==null){n.putInt("Ticks",0);return;}
                for(var actor:actors){
                    actor.setInvisible(t<60);
                    actor.frame(3,t,Math.max(0,Math.min(1,(t-60)/40F)));
                }
                if(t>=100)RescueChapter.go(p,s,FAREWELL_C);
            }
            case FAREWELL_C->{
                n.putBoolean("GhostsSad",t>=240);n.putFloat("VisionBlur",Math.max(0,(t-(ShowRecordings.duration("farewell_c")-100))/100F));
                if(!n.getBoolean("VisionRestored")&&!n.getBoolean("DreamDialogueComplete")){
                    var actors=visionActors(p,s);
                    if(actors==null)return;
                    for(var actor:actors){actor.setInvisible(false);actor.frame(t>=240?5:4,t,1);}
                    if(done){
                        n.putBoolean("DreamDialogueComplete",true);
                        OtherMotherLoop.changed(p);
                    }
                }
                if(n.getBoolean("DreamDialogueComplete")||n.getBoolean("VisionRestored")){
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
    private static boolean enterVision(ServerPlayer p,VisitData.Scene s){
        var c=visionCenter(p,s);
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null)return false;
        LoopProps.hold(level,c);
        p.stopRiding();
        p.setForcedPose(null);
        p.setPose(Pose.STANDING);
        p.teleportTo(level,c.getX()+.5,c.getY(),c.getZ()+.5,0,0);
        if(p.serverLevel()!=level||p.distanceToSqr(Vec3.atBottomCenterOf(c))>.25)return false;
        RescueChapter.data(s).putLong("VisionAt",c.asLong());
        return true;
    }
    private static void holdVision(ServerPlayer p,VisitData.Scene s){p.setInvulnerable(true);p.setNoGravity(true);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);var n=RescueChapter.data(s);if(p.serverLevel()==p.server.getLevel(ActivityWorld.KEY)&&n.contains("VisionAt")&&RescueChapter.phase(s)!=EYES_CLOSE){var at=BlockPos.of(n.getLong("VisionAt"));if(p.distanceToSqr(at.getCenter())>1)p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());}}
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
    static CinematicPropEntity[] visionActors(ServerPlayer p,VisitData.Scene s){
        var actors=new CinematicPropEntity[3];
        boolean ready=true;
        for(int i=0;i<actors.length;i++){
            actors[i]=ghost(p,s,i);
            ready&=actors[i]!=null;
        }
        var n=RescueChapter.data(s);
        if(ready){n.remove("MissingVisionActors");return actors;}
        n.putBoolean("MissingVisionActors",true);
        for(var actor:actors)if(actor!=null)actor.setInvisible(true);
        return null;
    }
    private static CinematicPropEntity ghost(ServerPlayer p,VisitData.Scene s,int index){
        var stage=p.server.getLevel(ActivityWorld.KEY);
        if(stage==null)return null;
        String key="FarewellProjection"+index;
        var context=FirstEscapeScene.context(s);
        var at=Vec3.atBottomCenterOf(visionCenter(p,s)).add((index-1)*2,0,4);
        var chunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(at));
        LoopProps.hold(stage,BlockPos.containing(at));
        var old=findGhost(p,s,index);
        var projection=FirstEscapeScene.prop(p,s,key);
        if(projection!=null&&(projection.level()!=stage||projection.kind()!=CinematicPropEntity.GHOST_CHILD
                ||projection.slot()!=index||projection.canonicalGhost(s)))return null;
        if(old==null){
            if(projection!=null){
                projection.setPos(at);
                return projection;
            }
            if(context.hasUUID(key)){
                var savedAt=context.contains(key+"Position")?BlockPos.of(context.getLong(key+"Position")):BlockPos.containing(at);
                if(savedAt.distSqr(BlockPos.containing(at))>16||!stage.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(savedAt).toLong()))return null;
                for(var level:p.server.getAllLevels())if(level.getEntity(context.getUUID(key))!=null)return null;
                context.remove(key);
                context.remove(key+"Position");
                OtherMotherLoop.changed(p);
            }
            if(!stage.areEntitiesLoaded(chunk.toLong()))return null;
            return FirstEscapeScene.ensure(p,s,key,CinematicPropEntity.GHOST_CHILD,index,stage,at);
        }
        if(projection!=null)projection.discard();
        var l=stage;Entity result=old;if(old.level()!=l)result=old.changeDimension(l,new net.minecraftforge.common.util.ITeleporter(){@Override public Entity placeEntity(Entity entity,ServerLevel current,ServerLevel destination,float yaw,java.util.function.Function<Boolean,Entity> reposition){var moved=reposition.apply(false);if(moved!=null)moved.setPos(at);return moved;}});if(!(result instanceof CinematicPropEntity ghost))return null;if(!ghost.getUUID().equals(old.getUUID()))throw new IllegalStateException("Ghost identity changed during farewell");LoopProps.hold(l,BlockPos.containing(at));ghost.setPos(at);FirstEscapeScene.context(s).putLong("Ghost"+index+"Position",ghost.blockPosition().asLong());return ghost;}
    private static boolean restoreVision(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        if(n.getBoolean("VisionRestored")&&p.serverLevel()==p.server.overworld())return true;
        if(n.getBoolean("VisionLandingPending")&&p.serverLevel()==p.server.overworld())return verifyVisionLanding(p,n);
        var saved=n.getCompound("VisionReturn");
        var l=p.server.overworld();
        var original=new Vec3(saved.getDouble("X"),saved.getDouble("Y"),saved.getDouble("Z"));
        boolean originalOverworld=saved.getString("Dimension").equals(l.dimension().location().toString());
        boolean finiteOriginal=Double.isFinite(original.x)&&Double.isFinite(original.y)&&Double.isFinite(original.z);
        Pose pose;try{pose=Pose.valueOf(saved.getString("Pose"));}catch(IllegalArgumentException bad){pose=Pose.STANDING;}
        // Older interrupted farewells may have captured their return while in
        // the wrong dimension. The recorded foreign position is never used as
        // an Overworld coordinate; only the authentic owned door is a fallback.
        Vec3 target=originalOverworld&&finiteOriginal
                ?(safeReturn(l,p,pose,original)?original:nearReturn(l,p,pose,BlockPos.containing(original),4))
                :null;
        var door=DoorData.get(p.server).doors.get(s.door);
        if(target==null&&door!=null&&door.placed&&s.owner.equals(door.owner)&&FamilyData.level(p.server,door.dimension)==l){
            target=nearReturn(l,p,pose,door.pos,6);
            if(target==null){
                var family=ParentAccess.family(p.server,s.owner);
                if(family!=null&&family.bed!=null&&FamilyData.level(p.server,family.homeDim)==l
                        &&family.bed.distSqr(door.pos)<=32*32)target=nearReturn(l,p,pose,family.bed,6);
            }
        }
        if(target==null){
            n.putString("Blocker","Clear safe footing and standing space near your original Little Door.");
            return false;
        }
        if(!originalOverworld||!finiteOriginal)n.putBoolean("VisionReturnUsedDoorFallback",true);
        p.teleportTo(l,target.x,target.y,target.z,saved.getFloat("Yaw"),saved.getFloat("Pitch"));
        if(p.serverLevel()!=l||p.position().distanceToSqr(target)>.25||!safeReturn(l,p,pose,p.position())){
            n.putString("Blocker","Your saved return is still being prepared. Your dream state is preserved and will retry safely.");OtherMotherLoop.changed(p);return false;
        }
        p.setInvulnerable(saved.getBoolean("Invulnerable"));p.setNoGravity(saved.getBoolean("NoGravity"));p.setHealth(Math.min(p.getMaxHealth(),saved.getFloat("Health")));p.getFoodData().setFoodLevel(saved.getInt("Food"));p.getFoodData().setSaturation(saved.getFloat("Saturation"));p.setForcedPose(null);if(saved.contains("ForcedPose"))try{p.setForcedPose(Pose.valueOf(saved.getString("ForcedPose")));}catch(IllegalArgumentException ignored){}p.setPose(pose);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        n.putBoolean("VisionLandingPending",true);
        OtherMotherLoop.changed(p);
        return verifyVisionLanding(p,n);
    }
    private static boolean verifyVisionLanding(ServerPlayer p,CompoundTag n){
        long now=p.server.overworld().getGameTime();
        if(n.contains("VisionSaveAfter")&&now<n.getLong("VisionSaveAfter")&&n.getLong("VisionSaveAfter")-now<=20)return false;
        try{
            ((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);
            var world=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR);
            var saved=PalaceKeyCustody.read(world.resolve(p.getUUID()+".dat"),16L*1024*1024);
            var position=saved.getList("Pos",Tag.TAG_DOUBLE);
            if(!saved.getString("Dimension").equals(p.server.overworld().dimension().location().toString())
                    ||position.size()!=3||p.distanceToSqr(position.getDouble(0),position.getDouble(1),position.getDouble(2))>.25)
                throw new java.io.IOException("Return position was not confirmed");
        }catch(java.io.IOException|RuntimeException unavailable){
            n.putLong("VisionSaveAfter",now+20);
            n.putString("Blocker","Waiting for storage to confirm your safe return.");
            OtherMotherLoop.changed(p);
            return false;
        }
        n.remove("VisionAt");
        n.remove("VisionSaveAfter");
        n.remove("VisionLandingPending");
        n.remove("Blocker");
        n.putBoolean("VisionRestored",true);
        OtherMotherLoop.changed(p);
        return true;
    }
    static Vec3 nearReturn(ServerLevel level,ServerPlayer player,Pose pose,BlockPos anchor,int radius){
        LoopProps.hold(level,anchor);
        for(int r=0;r<=radius;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            for(int dy:new int[]{0,1,-1,2,-2,3,-3}){
                var at=Vec3.atBottomCenterOf(anchor.offset(dx,dy,dz));
                if(safeReturn(level,player,pose,at))return at;
            }
        }
        return null;
    }
    static boolean safeReturn(ServerLevel level,ServerPlayer player,Pose pose,Vec3 at){
        var body=player.getDimensions(pose).makeBoundingBox(at);
        var low=BlockPos.containing(body.minX,body.minY-.10,body.minZ);
        var high=BlockPos.containing(body.maxX,body.maxY,body.maxZ);
        if(low.getY()<level.getMinBuildHeight()||high.getY()>=level.getMaxBuildHeight()
                ||!level.getWorldBorder().isWithinBounds(low)||!level.getWorldBorder().isWithinBounds(high)
                ||!level.hasChunksAt(low,high)||!level.noCollision(player,body)||level.containsAnyLiquid(body))return false;
        for(var pos:BlockPos.betweenClosed(low,high)){
            var state=level.getBlockState(pos);
            if(state.is(net.minecraft.tags.BlockTags.FIRE)||state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                    ||state.is(net.minecraft.world.level.block.Blocks.CACTUS)||state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                    ||state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)||state.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)
                    ||state.is(net.minecraft.tags.BlockTags.CAMPFIRES))return false;
        }
        var support=new AABB(body.minX,body.minY-.10,body.minZ,body.maxX,body.minY,body.maxZ);
        return level.getBlockCollisions(player,support).iterator().hasNext();
    }
    public static void recoverVision(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        if(!RescueChapter.owns(p,s)||!n.contains("VisionReturn")||n.getBoolean("VisionRestored")||n.getBoolean("DreamDialogueComplete"))return;
        holdVision(p,s);
        n.remove("Audio");
        n.remove("AudioTail");
        if(RescueChapter.phase(s)!=EYES_CLOSE&&p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY))enterVision(p,s);
    }
    private RescueEpilogue(){}
}
