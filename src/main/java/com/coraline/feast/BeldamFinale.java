package com.coraline.feast;

import static com.coraline.feast.RescueChapter.Phase.*;
import com.coraline.block.TinyDoorBlock;
import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Audio-ended gates, physical Cat/Stone arcs, hearing-only web and manual final lock. */
public final class BeldamFinale {
    /** Direct operator checkpoint, including the original exit/reunion dialogue. */
    public static String debugAfterFinalBoss(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);
        if(!p.hasPermissions(2))return "Operator permission required. In singleplayer, enable cheats before using this debug item.";
        if(s==null){String initialized=FirstVisit.debugComplete(p);if(OtherMotherLoop.scene(p)==null)return initialized;return debugAfterFinalBoss(p);}
        if(!p.getUUID().equals(s.owner))return "This is another player's story session. Return to your own story before using this item.";
        if(p.serverLevel()!=p.server.overworld())return "Use this item in the Overworld.";
        if(AcceptedButtonEyes.active(s))return "Finish or reset the active button-eye storyline before changing branches.";
        if(RescueChapter.finished(s)||RescueChapter.phase(s).ordinal()>=REUNION_A.ordinal())return "The post-boss storyline has already started; no items duplicated.";
        var d=DoorData.get(p.server).doors.get(s.door);var realm=RealmData.get(p.server).realms.get(s.door);
        if(d==null||!s.owner.equals(d.owner))return "Your story has no matching owned Little Door. Use the door originally given by your parents.";
        if(realm==null||!realm.ready||realm.house==null){
            if(!OtherWorld.prepareDebugStory(p,d)){p.getPersistentData().putBoolean("CoralineDebugAfterFinalPending",true);return "Preparing your story world automatically. The post-boss checkpoint will start when it is ready.";}
        }
        p.serverLevel().getChunkAt(d.pos);var door=p.level().getBlockState(d.pos);
        if(!door.is(ModBlocks.TINY_DOOR.get()))return "Restore the original Little Door before using this checkpoint.";
        if(p.getInventory().getFreeSlot()<0)return "Make space for the recovered key and ghost eyes first.";
        boolean key=false;for(var stack:p.getInventory().items)key|=ParentRescueStory.key(stack,s);
        if(!key){
            if(!StoryItemCustody.held(s,"Key").isEmpty()){if(!StoryItemCustody.giveBack(p,s,"Key"))return "Free a slot for your original key.";}
            else if(s.loop.story.getBoolean("LittleDoorKeyIssued")){
                if(!StoryItemCustody.capture(p,s,"Key",stack->ParentRescueStory.key(stack,s))&&!StoryItemCustody.recoverSwallowedKey(p,s))return "Retrieve your existing Little Door Key; a duplicate will not be created.";
                if(!StoryItemCustody.giveBack(p,s,"Key"))return "Free a slot for your original key.";
            }else if(!ParentRescueStory.grantKey(p,s))return "Free a slot for the Little Door Key.";
        }
        for(int i=0;i<3;i++){var eye=RescueEyes.eye(s,i);eye.putBoolean("Discovered",true);eye.putBoolean("Defeated",true);if(!RescueEyes.claim(p,s,i))return "Free inventory space for the three ghost eyes, then use again.";}
        if(!StoryItemCustody.held(s,"Stone").isEmpty()&&!StoryItemCustody.giveBack(p,s,"Stone"))return "Free a slot for your Seeing Stone.";
        if(!RescueChapter.transfer(p,p.server.overworld(),d.pos))return "Clear a safe landing beside your Little Door.";
        ShowPlayback.stop(p,s);VisitAudio.stop(p);RescueBosses.hideBar(p);p.stopUsingItem();
        s.stage=VisitData.Stage.DONE;var first=FirstEscapeStory.data(s);first.putBoolean("Started",true);first.putBoolean("Complete",true);first.putString("Phase",FirstEscapeTimeline.Phase.ESCAPE_COMPLETE.name());first.remove("Recover");MissingParentsCat.data(s).putBoolean("Complete",true);MissingParentsCat.data(s).putBoolean("TunnelRestored",true);
        var rescue=ParentRescueStory.data(s);rescue.putBoolean("Started",true);rescue.putBoolean("Complete",true);rescue.putBoolean("Seated",false);
        var scenery=ParentRescueStory.scenery(s);scenery.putLong("House",realm.house.asLong());scenery.putLong("LittleDoor",realm.doorPos.asLong());scenery.putLong("Prison",realm.house.above(80).asLong());
        var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,handWaitingPosition(p.serverLevel(),d.pos));
        if(hand==null)return "The original Hand could not be staged; retry the checkpoint before the farewell.";
        parkHand(p,s,hand,d.pos);
        var sealed=door.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false);
        // setBlock returns false when the door is already in the requested state.
        // That is a successful checkpoint, not a reason to abandon the epilogue.
        if(!door.equals(sealed))p.serverLevel().setBlock(d.pos,sealed,3);
        if(!p.serverLevel().getBlockState(d.pos).equals(sealed))return "The Little Door could not be sealed; retry the checkpoint.";
        var n=RescueChapter.data(s);n.putBoolean("Started",true);n.putBoolean("DebugFinalFightSkipped",true);n.putBoolean("KeyRecovered",true);n.putBoolean("CrawlReachedOverworld",true);n.putBoolean("PermanentlySealed",true);n.putBoolean("BangingDone",true);n.remove("Recover");
        n.putBoolean("HandSevered",true);
        rescue.putBoolean("DoorLocked",true);d.activeUntil=0;DoorData.get(p.server).setDirty();
        if(!RescueEpilogue.begin(p,s)){RescueChapter.go(p,s,BANGING);n.putInt("Ticks",120);}
        OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();return "Final boss completed. The Little Door exit and reunion dialogue is starting.";
    }
    /** Explicit operator shortcut: skip the fight, not the original key/door or epilogue. */
    public static String debugSkipFight(ServerPlayer p,VisitData.Scene s){
        if(!p.hasPermissions(2)||s==null||!p.getUUID().equals(s.owner))return "Owner/operator authorization required.";
        if(!RescueChapter.owns(p,s)||RescueChapter.phase(s).ordinal()<FINAL_A.ordinal()){
            s.loop.story.putBoolean("DebugSkipFinalFightPending",true);OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();
            return "Boss skip armed. Continue the remaining cutscenes and eye quests; the final fight will be skipped automatically when it begins.";
        }
        var state=RescueChapter.phase(s);
        if(state.ordinal()<FINAL_A.ordinal())return "Reach the final Other Mother encounter first; the three eye quests are not skipped.";
        if(state.ordinal()>=LOCK_DOOR.ordinal())return "The final fight is already over. No progress or items duplicated.";
        if(!RescueEyes.all(s))return "Collect your three original ghost eyes before skipping the final encounter.";
        var d=DoorData.get(p.server).doors.get(s.door);var level=d==null?null:FamilyData.level(p.server,d.dimension);
        if(d==null||!s.owner.equals(d.owner)||level==null)return "Your original Little Door is unavailable; nothing skipped.";
        level.getChunkAt(d.pos);if(!level.getBlockState(d.pos).is(ModBlocks.TINY_DOOR.get()))return "Restore your original Little Door before skipping.";
        boolean key=false;for(int i=0;i<p.getInventory().getContainerSize();i++)key|=ParentRescueStory.key(p.getInventory().getItem(i),s);
        if(!key){if(!StoryItemCustody.recoverSwallowedKey(p,s)||!StoryItemCustody.giveBack(p,s,"Key"))return "Retrieve the original key or free an inventory slot; no duplicate key created.";}
        if(!StoryItemCustody.held(s,"Stone").isEmpty()&&!StoryItemCustody.giveBack(p,s,"Stone"))return "Free an inventory slot for the original Seeing Stone before skipping.";
        if(!RescueChapter.transfer(p,level,d.pos))return "Clear a safe landing beside the original Little Door, then retry.";
        ShowPlayback.stop(p,s);VisitAudio.stop(p);p.stopUsingItem();RescueBosses.hideBar(p);
        // The skipped door struggle normally severs the original Hand. Keep
        // that physical actor and its saved identity for the later well chase.
        var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,handWaitingPosition(p.serverLevel(),d.pos));
        if(hand==null)return "The original Hand could not be staged; the skip has not completed.";
        var n=RescueChapter.data(s);s.loop.story.remove("DebugSkipFinalFightPending");n.putBoolean("DebugFinalFightSkipped",true);n.putBoolean("KeyRecovered",true);n.putBoolean("CrawlReachedOverworld",true);n.remove("CatHeld");n.remove("CatFlying");n.remove("Recover");n.remove("Press");
        parkHand(p,s,hand,d.pos);n.putBoolean("HandSevered",true);
        s.loop.story.putString("LittleDoorKeyHolder","PLAYER");
        var mother=ParentRescueStory.mother(p,s);if(mother!=null){mother.getNavigation().stop();mother.setDeltaMovement(Vec3.ZERO);mother.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);mother.setInvisible(true);}
        RescueChapter.go(p,s,LOCK_DOOR);RescueSiteCleanup.tick(p,s);OtherMotherLoop.changed(p);
        return "Final encounter skipped. Use your recovered key to lock the Little Door; the reunion continues normally.";
    }
    static Vec3 handWaitingPosition(net.minecraft.server.level.ServerLevel level,BlockPos door){
        var state=level.getBlockState(door);
        if(state.is(ModBlocks.TINY_DOOR.get())){
            var front=state.getValue(TinyDoorBlock.FACING);
            for(var dir:new net.minecraft.core.Direction[]{front,front.getClockWise(),front.getCounterClockWise()}){
                for(int dy:new int[]{0,-1,1}){
                    var at=door.relative(dir).above(dy);
                    if(level.hasChunkAt(at)&&com.coraline.tycoon.TycoonWorld.safeFeet(level,at))return Vec3.atBottomCenterOf(at);
                }
            }
        }
        return Vec3.atBottomCenterOf(door);
    }
    static void parkHand(ServerPlayer p,VisitData.Scene s,CinematicPropEntity hand,BlockPos door){
        if(hand.level()==p.level()){
            hand.setPos(handWaitingPosition(p.serverLevel(),door));
            var state=p.level().getBlockState(door);
            if(state.is(ModBlocks.TINY_DOOR.get()))hand.setYRot(state.getValue(TinyDoorBlock.FACING).toYRot());
        }
        hand.frame(RescueProps.HAND_WAITING_CUE,0,0);
        hand.setInvisible(true);
        hand.setDeltaMovement(Vec3.ZERO);
        var scenery=ParentRescueStory.scenery(s);
        scenery.putLong("RescueSeveredHandPosition",hand.blockPosition().asLong());
        scenery.putString("RescueSeveredHandDimension",hand.level().dimension().location().toString());
        RescueChapter.data(s).putBoolean("HandParked",true);
        OtherMotherLoop.changed(p);
    }
    static void reconcileHand(ServerPlayer p,VisitData.Scene s){
        var story=RescueChapter.data(s);
        if(!story.getBoolean("HandSevered")||p.tickCount%20!=0||PalaceWellEncounter.control(s)!=null)return;
        var source=DoorData.get(p.server).doors.get(s.door);
        if(source==null||!s.owner.equals(source.owner)||FamilyData.level(p.server,source.dimension)!=p.level())return;
        var hand=RescueProps.find(p,s,"SeveredHand");
        if(hand!=null&&hand.cue()<10&&(!story.getBoolean("HandParked")||!hand.isInvisible()))parkHand(p,s,hand,source.pos);
    }

    private static BlockPos little(VisitData.Scene s){return BlockPos.of(ParentRescueStory.scenery(s).getLong("LittleDoor"));}
    private static Vec3 hearth(VisitData.Scene s){return Vec3.atBottomCenterOf(FirstEscapeScene.room(s)).add(-2,.7,5);}
    static float face(Vec3 from,Vec3 to){return (float)Math.toDegrees(Math.atan2(from.x-to.x,to.z-from.z));}
    /** Exported rig button line: 1.501 - (9 - 19 - 7) / 16, not vanilla parent's chest-height eyes. */
    public static Vec3 facePoint(Entity mother){double yaw=Math.toRadians(mother.getYRot());return mother.position().add(-Math.sin(yaw)*.14,2.5635,Math.cos(yaw)*.14);}
    public static AABB faceBox(Entity mother){var at=facePoint(mother);return new AABB(at.x-.27,at.y-.29,at.z-.27,at.x+.27,at.y+.29,at.z+.27);}
    public static boolean catHitBeforeBlock(Entity mother,Vec3 from,Vec3 to,HitResult block){var hit=faceBox(mother).clip(from,to);return hit.isPresent()&&(block.getType()==HitResult.Type.MISS||from.distanceToSqr(hit.get())<from.distanceToSqr(block.getLocation()));}
    public static Vec3 catThrowVelocity(Vec3 from,Vec3 aim){var delta=aim.subtract(from);double ticks=Math.max(1,delta.horizontalDistance()/1.2);return new Vec3(delta.x/ticks,delta.y/ticks+.045*(ticks-1)/2,delta.z/ticks);}
    static Vec3 catThrowOrigin(ServerPlayer p){var look=p.getLookAngle().normalize();var eye=p.getEyePosition();var hand=eye.add(look.scale(.65)).add(0,-.45,0);var trace=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(eye,hand,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));return trace.getType()==HitResult.Type.MISS?hand:trace.getLocation().subtract(look.scale(.08));}
    private static void motherAt(ParentEntity m,Vec3 at,ServerPlayer p,int gesture){if(m==null)return;m.stopRiding();m.getNavigation().stop();m.setNoAi(true);m.setNoGravity(true);m.noPhysics=true;m.setInvisible(false);m.setPos(at);m.setYRot(face(at,p.position()));m.setYBodyRot(m.getYRot());m.setYHeadRot(m.getYRot());m.familyGesture(gesture,5);}
    static boolean roomLocked(VisitData.Scene s){
        if(!RescueChapter.started(s))return false;
        var n=RescueChapter.data(s);
        var phase=RescueChapter.phase(s);
        return n.getBoolean("RoomLocked")&&phase.ordinal()>=ROOM_LOCKED.ordinal()
                &&phase.ordinal()<=FINAL_O.ordinal()&&!n.getBoolean("WebEntryReady");
    }
    static boolean lockedDoor(VisitData.Scene s,BlockPos at){
        if(!roomLocked(s)||!ParentRescueStory.scenery(s).contains("LittleDoor",net.minecraft.nbt.Tag.TAG_LONG))return false;
        var entrance=FirstEscapeScene.entrance(s);
        var little=little(s);
        return at.equals(entrance)||at.equals(entrance.above())||at.equals(entrance.south())
                ||at.equals(entrance.south().above())||at.equals(little)||at.equals(little.above());
    }
    static boolean keepPlayerInRoom(ServerPlayer p,VisitData.Scene s){
        if(!roomLocked(s)||RescueChapter.data(s).getBoolean("FloorOpened"))return true;
        var level=p.server.getLevel(OtherWorld.KEY);
        if(level==null)return false;
        if(p.serverLevel()==level&&FirstEscapeScene.fullyInRoom(s,p.getBoundingBox()))return true;
        var center=FirstEscapeScene.room(s);
        LoopProps.hold(level,center);
        for(int radius=0;radius<=5;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            var at=Vec3.atBottomCenterOf(center.offset(dx,0,dz));
            if(!FirstEscapeScene.fullyInRoom(s,p.getDimensions(Pose.STANDING).makeBoundingBox(at))
                    ||!RescueEpilogue.safeReturn(level,p,Pose.STANDING,at))continue;
            p.stopRiding();
            p.setForcedPose(null);
            p.setPose(Pose.STANDING);
            p.teleportTo(level,at.x,at.y,at.z,p.getYRot(),p.getXRot());
            p.fallDistance=0;
            p.setDeltaMovement(Vec3.ZERO);
            return p.serverLevel()==level&&p.position().distanceToSqr(at)<.25;
        }
        RescueChapter.data(s).putString("Blocker","Clear a standing space in the locked confrontation room to resume.");
        return false;
    }
    private static final class StageFloor {
        private static final java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> BLUEPRINT=create();
        private static java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> create(){
            var footprint=new java.util.HashSet<BlockPos>();
            for(int x=14;x<=30;x++)for(int z=37;z<=51;z++)footprint.add(com.coraline.secrets.HouseCabinets.at(BlockPos.ZERO,x,5,z));
            var result=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(var cell:RosewoodHouse.palaceCells())if(footprint.contains(cell.offset()))result.put(cell.offset(),cell.state());
            return java.util.Map.copyOf(result);
        }
    }
    static boolean prepareFall(ServerPlayer p,VisitData.Scene s){
        var level=p.server.getLevel(OtherWorld.KEY);
        if(level==null)return false;
        var n=RescueChapter.data(s);
        var journal=FirstEscapeScene.context(s).getCompound("Environment");
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        var cells=new java.util.ArrayList<BlockPos>(17*15);
        for(int x=14;x<=30;x++)for(int z=37;z<=51;z++){
            var pos=FirstEscapeScene.at(s,x,5,z);
            cells.add(pos);
            level.getChunkAt(pos);
            var block=level.getBlockState(pos);
            if(block.isAir())continue;
            var existing=journal.getCompound(Long.toString(pos.asLong()));
            boolean authored=block.equals(StageFloor.BLUEPRINT.get(pos.subtract(RescueEncounters.house(s))));
            boolean journaled=existing.contains("Placed")&&block.equals(net.minecraft.nbt.NbtUtils.readBlockState(lookup,existing.getCompound("Placed")));
            if(level.getBlockEntity(pos)!=null||!authored&&!journaled){
                n.remove("FloorOpened");
                n.putString("Blocker","The stage floor at "+pos.toShortString()+" contains an obstruction; move it before continuing.");
                return false;
            }
        }
        for(var pos:cells)if(!level.getBlockState(pos).isAir())FirstEscapeScene.place(p,s,pos,Blocks.AIR.defaultBlockState());
        for(var pos:cells)if(!level.getBlockState(pos).isAir()){
            n.remove("FloorOpened");
            n.putString("Blocker","The floor has not physically opened at "+pos.toShortString()+".");
            return false;
        }
        var room=FirstEscapeScene.room(s);
        var feet=Vec3.atBottomCenterOf(room.below(2));
        var clearance=p.getDimensions(Pose.STANDING).makeBoundingBox(feet).expandTowards(0,2,0);
        if(level.getBlockCollisions(p,clearance).iterator().hasNext()){
            n.remove("FloorOpened");
            n.putString("Blocker","Clear the falling space underneath the confrontation room.");
            return false;
        }
        n.putBoolean("FloorOpened",true);
        return true;
    }
    static boolean openedFloorFallRecoveryDue(int ticks,int duration){
        return ticks>=Math.max(120,duration+80);
    }
    private static boolean guideOpenedFloorFall(ServerPlayer p,VisitData.Scene s,int t,int duration){
        var n=RescueChapter.data(s);
        var level=p.server.getLevel(OtherWorld.KEY);
        var room=FirstEscapeScene.room(s);
        if(!n.getBoolean("FloorOpened")||level==null||p.serverLevel()!=level
                ||p.getY()<room.getY()-2)return false;
        if(!openedFloorFallRecoveryDue(t,duration)){
            RescueChapter.hint(p,"The floor has opened. Step into the falling center.");
            return false;
        }
        var landing=Vec3.atBottomCenterOf(room);
        var body=p.getDimensions(Pose.STANDING).makeBoundingBox(landing);
        if(!level.getWorldBorder().isWithinBounds(body)
                ||!level.getBlockState(room.below()).isAir()
                ||level.containsAnyLiquid(body)
                ||level.getBlockCollisions(p,body).iterator().hasNext()){
            n.putString("Blocker","The center of the opened floor is obstructed. Clear it to continue the fall.");
            return false;
        }
        // Do not run this as an unconditional per-tick teleport: a previous
        // staged fall needs time to complete, and saves may resume mid-drop.
        long now=level.getGameTime();
        if(n.contains("FallGuideRetryAt")&&now<n.getLong("FallGuideRetryAt"))return false;
        n.putLong("FallGuideRetryAt",now+80);
        p.stopRiding();p.setForcedPose(null);p.setPose(Pose.STANDING);
        p.teleportTo(level,landing.x,landing.y,landing.z,p.getYRot(),p.getXRot());
        if(p.serverLevel()!=level||p.position().distanceToSqr(landing)>.25)return false;
        p.fallDistance=0;
        n.remove("Blocker");OtherMotherLoop.changed(p);
        return true;
    }
    static boolean requiresOriginalExit(RescueChapter.Phase state){
        return state==LOCK_DOOR||state==BANGING;
    }
    /** Do not strand a post-crawl save in the Activity World. Check the real
     * owner-bound Overworld door before lock/banging or resume the real crawl
     * when an older checkpoint never committed the exit receipt. */
    /** A saved seal alone is not enough: the owner's original door must still
     * physically exist with its own block entity and closed, inactive portal. */
    static boolean originalDoorSealed(ServerLevel home,DoorData.Door source,java.util.UUID doorId){
        var block=home.getBlockState(source.pos);
        return block.is(ModBlocks.TINY_DOOR.get())
                &&!block.getValue(TinyDoorBlock.OPEN)&&!block.getValue(TinyDoorBlock.PORTAL)
                &&home.getBlockEntity(source.pos) instanceof DoorBlockEntity tile
                &&doorId.equals(tile.doorId);
    }
    static boolean originalExitReady(ServerPlayer p,VisitData.Scene s){
        var state=RescueChapter.phase(s);
        if(!requiresOriginalExit(state))return true;
        var n=RescueChapter.data(s);
        var source=DoorData.get(p.server).doors.get(s.door);
        var home=source==null?null:FamilyData.level(p.server,source.dimension);
        if(source==null||!s.owner.equals(source.owner)||home!=p.server.overworld()){
            n.putString("Blocker","Waiting for the original owner-bound Overworld Little Door.");
            return false;
        }
        if(state==BANGING){
            home.getChunkAt(source.pos);
            if(!originalDoorSealed(home,source,s.door)){
                var block=home.getBlockState(source.pos);
                if(!block.is(ModBlocks.TINY_DOOR.get())
                        ||!(home.getBlockEntity(source.pos) instanceof DoorBlockEntity tile)
                        ||!s.door.equals(tile.doorId)){
                    n.putString("Blocker","Restore the original owner-bound Little Door before the post-boss reunion.");
                    return false;
                }
                // Reloading a saved seal after the real portal reopened must
                // resume the *manual* key lock, not silently complete the ending.
                n.putBoolean("PermanentlySealed",false);
                n.putBoolean("BangingDone",false);
                ParentRescueStory.data(s).putBoolean("DoorLocked",false);
                RescueChapter.go(p,s,LOCK_DOOR);
                return false;
            }
        }
        if(state==LOCK_DOOR&&!n.getBoolean("CrawlReachedOverworld")){
            // A phase marker alone is not proof that the 52-block crawl was
            // completed. Re-enter its verified physical passage instead.
            if(n.getBoolean("HandSevered")&&n.getBoolean("KeyRecovered")
                    &&RescuePassage.enter(p,s))RescueChapter.go(p,s,TUNNEL_CHASE);
            else n.putString("Blocker","Waiting for the original verified escape crawl and recovered key.");
            return false;
        }
        if(p.serverLevel()!=home&&!RescueChapter.transfer(p,home,source.pos)){
            n.putString("Blocker","Clear a safe return landing near your original Little Door.");
            return false;
        }
        if(state==BANGING&&!n.getBoolean("PermanentlySealed")){
            // An interrupted door transaction must be completed by the
            // original key interaction, not by the dialogue watchdog.
            RescueChapter.go(p,s,LOCK_DOOR);
            return false;
        }
        n.remove("Blocker");
        return true;
    }
    /** All voiced final-room scenes need the SAME canonical Other Mother.
     * The Cat swat also needs the original leased Cat until its actual escape
     * receipt is committed; recording timeouts must never substitute for it. */
    static boolean finaleCastReady(RescueChapter.Phase state,boolean motherPresent,boolean catPresent,boolean catEscaped){
        return state.ordinal()<FINAL_A.ordinal()||state.ordinal()>FINAL_N.ordinal()
                ||motherPresent&&(state!=FINAL_N||catEscaped||catPresent);
    }
    static boolean originalFinaleCastReady(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state){
        if(state.ordinal()<FINAL_A.ordinal()||state.ordinal()>FINAL_N.ordinal())return true;
        var n=RescueChapter.data(s);
        var world=p.server.getLevel(OtherWorld.KEY);
        if(world==null||p.serverLevel()!=world){
            n.putBoolean("Recover",true);
            n.putString("Blocker","Return to the original Other World confrontation room before continuing the finale.");
            return false;
        }
        var mother=ParentRescueStory.mother(p,s);
        // On interrupted world transfers bring back ONLY the recorded Mother,
        // never spawn a replacement or allow a cast actor in another dimension.
        if(mother!=null&&mother.isAlive()&&mother.level()!=world&&s.mom.equals(mother.getUUID())){
            var returned=ShowTravel.guest(mother,p,s,world,Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)),true,"BeldamFinaleReturn");
            if(returned instanceof ParentEntity original&&original.getUUID().equals(s.mom))mother=original;
        }
        boolean motherReady=mother!=null&&mother.isAlive()&&mother.level()==world&&s.mom.equals(mother.getUUID());
        boolean catReady=false;
        if(state==FINAL_N&&!n.getBoolean("CatEscaped")){
            var cat=ShowActors.actor(p,s,"cat");
            if(cat==null||cat.level()!=world){
                Vec3 at=n.contains("CatSwatX")?new Vec3(n.getDouble("CatSwatX"),n.getDouble("CatSwatY"),n.getDouble("CatSwatZ"))
                        :motherReady?facePoint(mother):Vec3.atBottomCenterOf(FirstEscapeScene.room(s));
                // Recovering a lease can move the SAME UUID between dimensions;
                // a missing or foreign Cat never licenses a newly-created one.
                cat=ShowActors.recoverCat(p,s,world,at);
            }
            catReady=cat instanceof CharacterEntity original&&original.isAlive()&&original.level()==world&&ShowActors.storyCat(original);
        }
        if(finaleCastReady(state,motherReady,catReady,n.getBoolean("CatEscaped")))return true;
        if(n.getBoolean("Audio")){
            ShowPlayback.stop(p,s);
            n.remove("Audio");n.remove("AudioTail");n.putInt("Ticks",0);
        }
        n.putString("Blocker",!motherReady?"Waiting for the original Other Mother to return to the finale."
                :"Waiting for the original Cat before the final escape cutscene.");
        return false;
    }
    public static boolean preflight(ServerPlayer p,VisitData.Scene s){
        var state=RescueChapter.phase(s);var n=RescueChapter.data(s);
        if(state.ordinal()>=FINAL_I.ordinal()&&state.ordinal()<=FINAL_N.ordinal()&&!n.getBoolean("KeyRecovered")&&!StoryItemCustody.recoverSwallowedKey(p,s)){n.putString("Blocker","The swallowed key cannot be recovered: restore its saved custody record or bring the surviving owner-bound original out of storage.");return false;}
        if(state==FINAL_E&&!n.getBoolean("StoneTaken")){if(!StoryItemCustody.capture(p,s,"Stone",stack->StoryItemCustody.stone(stack,s))){n.putString("Blocker","Bring your original Seeing Stone out of storage. Unrelated stones are not substitutes.");return false;}p.stopUsingItem();n.putBoolean("StoneTaken",true);}
        if(!keepPlayerInRoom(p,s)||!originalFinaleCastReady(p,s,state))return false;
        if(state.ordinal()>=FINAL_P.ordinal()&&state.ordinal()<=WEB_STEALTH.ordinal()
                &&!enterWeb(p,s,ParentRescueStory.mother(p,s)))return false;
        if(state==TUNNEL_CHASE&&!n.getBoolean("PassageEntered")&&!RescuePassage.enter(p,s))return false;
        n.remove("Blocker");return true;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state,int t,boolean done){
        var n=RescueChapter.data(s);var m=ParentRescueStory.mother(p,s);
        if(state.ordinal()<=FINAL_N.ordinal()&&m==null){n.putString("Blocker","Waiting for the saved Other Mother, not creating another Beldam.");return;}
        if(state.ordinal()<=FINAL_N.ordinal()){m.setInvisible(false);m.setNoAi(true);m.getNavigation().stop();m.lookAtStoryPlayer(p);}
        if(state.ordinal()<=FINAL_O.ordinal())RescueWebSite.prepare(p,s);
        switch(state){
            case FINAL_A->{motherAt(m,Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)),p,66);if(done)RescueChapter.go(p,s,FINAL_B);}
            case FINAL_B->{m.familyGesture(67,5);if(done)RescueChapter.go(p,s,FINAL_C);}
            case FINAL_C->{m.familyGesture(66,5);if(done)RescueChapter.go(p,s,SHOW_EYES);}
            case SHOW_EYES->RescueChapter.hint(p,"Show the three ghost eyes.");
            case FINAL_D->{showEyes(p,s,t);m.familyGesture(66,5);if(done)RescueChapter.go(p,s,FINAL_E);}
            case FINAL_E->{m.familyGesture(64,5);m.setItemSlot(EquipmentSlot.MAINHAND,StoryItemCustody.held(s,"Stone"));if(done)RescueChapter.go(p,s,FINAL_F);}
            case FINAL_F->{
                m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);var target=hearth(s);var start=m.position().add(0,1.65,-.4);double u=Math.min(1,t/40.0);var point=start.lerp(target,u).add(0,Math.sin(u*Math.PI)*1.3,0);var stone=RescueProps.ensure(p,s,"ThrownStone",RescueProps.STONE,0,point);if(stone!=null){stone.setPos(point);stone.frame(0,t,1);}
                if(u>=1&&!n.getBoolean("StoneDestroyed")){StoryItemCustody.destroy(s,"Stone");n.putBoolean("StoneDestroyed",true);RescueProps.hide(p,s,"ThrownStone");p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,target.x,target.y,target.z,18,.2,.3,.2,.025);OtherMotherLoop.changed(p);}
                if(done&&n.getBoolean("StoneDestroyed"))RescueChapter.go(p,s,FINAL_G);
            }
            case FINAL_G->{m.familyGesture(66,5);if(done)RescueChapter.go(p,s,HIDING_CHOICE);}
            case HIDING_CHOICE->RescueChapter.hint(p,"Where have you hidden them?");
            case FINAL_H,WARDROBE_ASIDE->{
                if(t>=40||state==WARDROBE_ASIDE)moveWardrobe(p,s,m);
                if((done||state==WARDROBE_ASIDE)&&n.getBoolean("WardrobeClear"))RescueChapter.go(p,s,FINAL_I);
                else if(done)RescueChapter.go(p,s,WARDROBE_ASIDE);
            }
            case FINAL_I->{m.familyGesture(65,5);if(t>=20){var key=StoryItemCustody.held(s,"Key");if(key.isEmpty())return;m.setItemSlot(EquipmentSlot.MAINHAND,key);n.putBoolean("KeyCoughed",true);}if(done&&n.getBoolean("KeyCoughed"))RescueChapter.go(p,s,FINAL_J);}
            case FINAL_J->{m.familyGesture(64,5);m.setItemSlot(EquipmentSlot.MAINHAND,StoryItemCustody.held(s,"Key"));if(done)RescueChapter.go(p,s,FINAL_K);}
            case FINAL_K->{
                var at=Vec3.atBottomCenterOf(little(s).east());moveMother(m,at,.045,p,64);
                if(t>=Math.max(20,ShowRecordings.duration("finale_k")-45)&&m.distanceToSqr(at)<1){var frame=p.serverLevel().getBlockState(little(s));if(frame.is(ModBlocks.TINY_DOOR.get())&&!StoryItemCustody.held(s,"Key").isEmpty()){p.serverLevel().setBlock(little(s),frame.setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,false),3);n.putBoolean("DoorUnlocked",true);ParentRescueStory.data(s).putBoolean("DoorLocked",false);}}
                if(done&&n.getBoolean("DoorUnlocked"))RescueChapter.go(p,s,FIGHT_CHOICE);
            }
            case FIGHT_CHOICE->RescueChapter.hint(p,"Fight back.");
            case FINAL_M->{catAtMantel(p,s);if(done)RescueChapter.go(p,s,CAT_THROW);}
            case CAT_THROW->{
                var cat=catAtMantel(p,s);
                if(cat==null){n.putInt("Ticks",Math.max(0,t-1));n.putString("Blocker","Waiting for the original Cat before the confrontation can resume.");return;}
                int beat=Math.floorMod(t,100);
                boolean advancing=beat>=44&&beat<77&&t>=n.getInt("CatRecoverUntil");
                if(advancing)moveMother(m,p.position(),.044,p,67);
                else motherAt(m,m.position(),p,beat<44?66:67);
                if(beat==40)p.serverLevel().playSound(null,m.blockPosition(),net.minecraft.sounds.SoundEvents.SPIDER_AMBIENT,net.minecraft.sounds.SoundSource.HOSTILE,.8F,.85F);
                if(n.getBoolean("CatFlying"))catFlight(p,s,cat,m);
                else if(n.getBoolean("CatHeld")){
                    cat.setPos(p.getEyePosition().add(p.getLookAngle().scale(.65)).add(0,-.45,0));
                    RescueChapter.hint(p,advancing?"She's coming. Take aim at her face!":"Aim at Other Mother's face and left-click to throw the Cat.");
                }else RescueChapter.hint(p,"Right-click the Cat on the mantel, then aim and throw.");
                if(advancing&&!n.getBoolean("CatFlying")&&RescueChapter.phase(s)==CAT_THROW&&m.distanceToSqr(p)<1.8)
                    AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS);
            }
            case FINAL_N->{
                int duration=ShowRecordings.duration("finale_n");m.familyGesture(67,5);var cat=ShowActors.actor(p,s,"cat");
                if(RescueWebLayout.blindAt(t,duration))n.putBoolean("BeldamBlind",true);
                if(RescueWebLayout.swatAt(t,duration)&&cat!=null){if(!n.contains("CatSwatX")){n.putDouble("CatSwatX",cat.getX());n.putDouble("CatSwatY",cat.getY());n.putDouble("CatSwatZ",cat.getZ());}double u=Math.min(1,(t-(duration-40))/25.0);var from=new Vec3(n.getDouble("CatSwatX"),n.getDouble("CatSwatY"),n.getDouble("CatSwatZ"));var land=Vec3.atBottomCenterOf(FirstEscapeScene.entrance(s).west(3));cat.setPos(from.lerp(land,u).add(0,Math.sin(u*Math.PI)*1.3,0));ShowPlaybackNetwork.pose(cat,34,t,1);if(u>=1)n.putBoolean("CatEscaped",true);}
                if(t>=duration-50&&!n.getBoolean("KeyRecovered")){m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);if(StoryItemCustody.giveBack(p,s,"Key")){n.putBoolean("KeyRecovered",true);s.loop.story.putString("LittleDoorKeyHolder","PLAYER");}else RescueChapter.hint(p,"Make an inventory space to recover your original key.");}
                if(done&&n.getBoolean("CatEscaped")&&n.getBoolean("KeyRecovered"))RescueChapter.go(p,s,FINAL_O);
            }
            case FINAL_O->{
                if(t<30||!RescueWebSite.prepare(p,s)||webLanding(p,s)==null||!prepareFall(p,s))return;
                // The floor can open while the player stands on its surviving
                // perimeter. Do not leave the voiced fall permanently waiting
                // for an accidental step: after a full cue and grace window,
                // move the original player into the already verified air gap.
                // The web transfer and the rest of the boss remain physical.
                guideOpenedFloorFall(p,s,t,ShowRecordings.duration("finale_o"));
                if((p.getY()<FirstEscapeScene.room(s).getY()-2||p.serverLevel()==p.server.getLevel(ActivityWorld.KEY))
                        &&enterWeb(p,s,m)&&done)RescueChapter.go(p,s,FINAL_P);
            }
            case FINAL_P->{n.putFloat("BeldamMorph",Math.min(1,t/(float)Math.max(1,ShowRecordings.duration("finale_p"))));if(done)RescueChapter.go(p,s,FINAL_Q);}
            case FINAL_Q->{if(m!=null){m.setInvisible(t<120);motherAt(m,Vec3.atBottomCenterOf(RescueWebSite.center(p,s)),p,67);m.setInvisible(t<120);}if(done){n.remove("NoiseX");rememberPosition(p,n);RescueChapter.go(p,s,WEB_STEALTH);}}
            case WEB_STEALTH->web(p,s,m,t);
            case DOOR_KICK->{RescueChapter.hint(p,"Kick the door open!");if(t==1)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,net.minecraft.sounds.SoundSource.BLOCKS,.8F,.8F);if(t>=20){n.putFloat("Struggle",0);RescueChapter.go(p,s,DOOR_STRUGGLE);}}
            case DOOR_STRUGGLE->{
                long now=p.serverLevel().getGameTime();
                boolean pulling=n.contains("PressUntil")&&now<=n.getLong("PressUntil")&&n.getLong("PressUntil")-now<=6;
                float progress=n.getFloat("Struggle");
                if(!n.getBoolean("HandCutPending")){
                    progress=Math.max(0,Math.min(1,progress+(pulling?(n.getBoolean("AccessibleStruggle")?.017F:.012F):-.002F)));
                    n.putFloat("Struggle",progress);
                    if(RescueWebLayout.struggleFinished(t,progress))n.putBoolean("HandCutPending",true);
                }
                var door=RescueProps.find(p,s,"WebDoor");
                if(door!=null)door.frame(2,t,progress);
                if(pulling&&t%16==0)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,net.minecraft.sounds.SoundSource.BLOCKS,.45F,.65F+progress*.5F);
                RescueChapter.hint(p,"Hold to pull the door shut — "+Math.round(progress*100)+"%");
                if(n.getBoolean("HandCutPending")){
                    var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,RescueWebSite.at(p,s,1,1).add(0,.15,-.7));
                    if(hand==null){n.putString("Blocker","Waiting to stage the severed Hand.");return;}
                    hand.frame(1,0,1);
                    p.serverLevel().playSound(null,hand.blockPosition(),net.minecraft.sounds.SoundEvents.WOOD_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1.1F,.72F);
                    n.remove("PressUntil");
                    n.remove("HandCutPending");
                    n.putBoolean("HandSevered",true);
                    RescueChapter.go(p,s,HAND_FALL);
                }else if(t>=240)retry(p,s,DOOR_STRUGGLE,"You lost your grip. Try closing the door again.");
            }
            case HAND_FALL->{
                RescuePassage.prepare(p,s);
                var hand=RescueProps.find(p,s,"SeveredHand");
                if(hand==null||hand.kind()!=RescueProps.HAND||hand.level()!=p.level()){
                    n.putInt("Ticks",Math.max(0,t-1));
                    n.putString("Blocker","Waiting for the original severed Hand.");
                    return;
                }
                hand.frame(1,t,1);
                if(t<20)hand.setPos(hand.position().add(0,-.015,0));
                if(t>=60&&RescuePassage.prepare(p,s)){n.remove("PassageEntered");RescueChapter.go(p,s,TUNNEL_CHASE);}
            }
            case TUNNEL_CHASE->tunnel(p,s,t,done);
            case LOCK_DOOR->RescueChapter.hint(p,"Use your recovered key to lock the Little Door.");
            case BANGING->{if(t<=120&&t%20==0)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,net.minecraft.sounds.SoundSource.BLOCKS,(1-t/140F)*.8F,.7F);if(t>=120){n.putBoolean("BangingDone",true);RescueEpilogue.begin(p,s);}}
            default->{}
        }
        if(n.getBoolean("WebEntered")&&state.ordinal()>=FINAL_O.ordinal()&&state.ordinal()<=WEB_STEALTH.ordinal()&&p.getY()<RescueWebSite.center(p,s).getY()-6)retry(p,s,WEB_STEALTH,"You slipped off the safe strands. Retry from the web entrance.");
    }
    private static void showEyes(ServerPlayer p,VisitData.Scene s,int t){for(int i=0;i<3;i++){var at=p.position().add(p.getLookAngle().multiply(.8,0,.8)).add((i-1)*.45,1.1,0);var e=RescueProps.ensure(p,s,"PresentedEye"+i,RescueProps.EYE,i,at);if(e!=null){e.setPos(at);e.frame(1,t,1);}}}
    private static boolean physicalEyes(ServerPlayer p,VisitData.Scene s){for(int i=0;i<3;i++){boolean found=false;for(int slot=0;slot<p.getInventory().getContainerSize();slot++)found|=RescueEyes.authentic(p.getInventory().getItem(slot),s,i);if(!found)return false;}return RescueEyes.all(s);}
    private static boolean recoverPresentationEyes(ServerPlayer p,VisitData.Scene s){
        for(int i=0;i<3;i++){boolean found=false;for(int slot=0;slot<p.getInventory().getContainerSize();slot++)found|=RescueEyes.authentic(p.getInventory().getItem(slot),s,i);
            if(!found&&!com.coraline.multiplayer.QuestItemRecovery.recall(p,s,"eye_"+i))return false;}
        return physicalEyes(p,s);
    }
    private static void moveMother(ParentEntity m,Vec3 target,double speed,ServerPlayer p,int gesture){if(m==null)return;var delta=target.subtract(m.position()).multiply(1,0,1);var next=m.position().add(delta.length()<=speed?delta:delta.normalize().scale(speed));motherAt(m,next,p,gesture);}
    private static void moveWardrobe(ServerPlayer p,VisitData.Scene s,ParentEntity m){
        var n=RescueChapter.data(s);var wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");if(wardrobe==null)return;var door=little(s);var goal=Vec3.atBottomCenterOf(door).add(0,0,4);moveMother(m,wardrobe.position().add(1.5,0,0),.04,p,64);if(m.distanceToSqr(wardrobe)>12)return;
        var delta=goal.subtract(wardrobe.position());wardrobe.setPos(wardrobe.position().add(delta.length()<.055?delta:delta.normalize().scale(.055)));wardrobe.frame(1,n.getInt("Ticks"),1);
        var portal=new AABB(door).inflate(.3,.1,.3);if(!wardrobe.getBoundingBox().intersects(portal)&&wardrobe.distanceToSqr(goal)<.1){n.putBoolean("WardrobeClear",true);ParentRescueStory.data(s).putBoolean("WardrobeBlocking",false);}
    }
    static Mob catAtMantel(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        var at=hearth(s).add(0,.7,0);
        var cat=ShowActors.recoverCat(p,s,p.serverLevel(),at);
        if(cat==null)return null;
        var state=RescueChapter.phase(s);
        // A saved CatMantel flag is not proof the original Cat is still here.
        // Re-stage the SAME actor at pre-fight checkpoints only. In CAT_THROW,
        // never reset a carried or airborne Cat just because it moved away.
        boolean preFight=state==HOUSE_SAFE||state==ROOM_LOCKED;
        if(!n.getBoolean("CatMantel")||preFight&&cat.distanceToSqr(at)>.25){
            cat.stopRiding();cat.setNoAi(true);cat.setNoGravity(true);
            cat.setDeltaMovement(Vec3.ZERO);cat.setPos(at);n.putBoolean("CatMantel",true);
        }
        cat.setInvisible(false);
        n.putInt("Cat",cat.getId());LoopProps.hold(p.serverLevel(),cat.blockPosition());
        return cat;
    }
    private static void catFlight(ServerPlayer p,VisitData.Scene s,Mob cat,ParentEntity mother){
        var n=RescueChapter.data(s);
        int ticks=n.getInt("CatFlightTick")+1;
        n.putInt("CatFlightTick",ticks);
        var from=cat.position();
        var velocity=new Vec3(n.getDouble("CatVX"),n.getDouble("CatVY"),n.getDouble("CatVZ"));
        var next=from.add(velocity);
        var trace=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(from,next,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,cat));
        if(catHitBeforeBlock(mother,from,next,trace)){
            var face=facePoint(mother);
            cat.setPos(face);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,face.x,face.y,face.z,16,.2,.2,.2,.05);
            p.serverLevel().playSound(null,mother.blockPosition(),net.minecraft.sounds.SoundEvents.CAT_HISS,net.minecraft.sounds.SoundSource.HOSTILE,1.1F,.9F);
            n.remove("CatFlying");
            n.remove("CatHeld");
            n.putBoolean("CatHit",true);
            RescueChapter.go(p,s,FINAL_N);
            return;
        }
        if(trace.getType()!=HitResult.Type.MISS||ticks>45){
            n.remove("CatFlying");
            n.remove("CatHeld");
            n.remove("CatMantel");
            n.putInt("CatRecoverUntil",n.getInt("Ticks")+42);
            catAtMantel(p,s);
            p.serverLevel().playSound(null,cat.blockPosition(),net.minecraft.sounds.SoundEvents.CAT_AMBIENT,net.minecraft.sounds.SoundSource.NEUTRAL,.8F,1.1F);
            RescueChapter.hint(p,"The Cat missed. Pick him up and aim at her face again.");
            return;
        }
        cat.setPos(next);
        n.putDouble("CatVY",velocity.y-.045);
        ShowPlaybackNetwork.pose(cat,34,ticks,1);
    }
    private static Vec3 webLanding(ServerPlayer p,VisitData.Scene s){
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null||!RescueWebSite.prepare(p,s))return null;
        var landing=RescueWebSite.at(p,s,1,0);
        var block=BlockPos.containing(landing);
        LoopProps.hold(level,block);
        for(var support:new BlockPos[]{block.below(),block}){
            var shape=level.getBlockState(support).getCollisionShape(level,support);
            if(shape.isEmpty())continue;
            var feet=Vec3.atBottomCenterOf(block).add(0,support.getY()-block.getY()+shape.max(net.minecraft.core.Direction.Axis.Y),0);
            if(RescueEpilogue.safeReturn(level,p,Pose.STANDING,feet))return feet;
        }
        return null;
    }
    static boolean enterWeb(ServerPlayer p,VisitData.Scene s,ParentEntity mother){
        var n=RescueChapter.data(s);
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null)return false;
        if(n.getBoolean("WebEntryReady")&&p.serverLevel()==level&&mother!=null&&mother.level()==level
                &&mother.getUUID().equals(s.mom))return true;
        n.remove("WebEntryReady");
        n.remove("WebEntered");
        // A new entry/retry is a physical checkpoint, not just an old
        // WebReady boolean. Verify the journalled web in bounded batches.
        if(!n.getBoolean("WebEntryRecheck")){
            n.putBoolean("WebEntryRecheck",true);
            n.remove("WebVerified");
            n.putInt("WebVerify",0);
        }
        if(mother==null||!mother.getUUID().equals(s.mom)||!mother.isAlive()){
            n.putString("Blocker","Waiting for the original Other Mother before entering the web.");
            return false;
        }
        var landing=webLanding(p,s);
        var motherAt=Vec3.atBottomCenterOf(RescueWebSite.center(p,s));
        LoopProps.hold(level,BlockPos.containing(motherAt));
        var motherBody=mother.getDimensions(Pose.STANDING).makeBoundingBox(motherAt);
        if(landing==null){
            if(n.getString("Blocker").isEmpty())
                n.putString("Blocker","Waiting for the complete original web and safe player landing.");
            return false;
        }
        if(level.getBlockCollisions(mother,motherBody).iterator().hasNext()||level.containsAnyLiquid(motherBody)){
            n.putString("Blocker","The original Other Mother web landing is obstructed.");
            return false;
        }
        n.putUUID("WebEntryMother",mother.getUUID());
        if(p.serverLevel()!=level||p.position().distanceToSqr(landing)>.25){
            // Same-world retries can leave the player far below or beyond the
            // start. Restore only the real verified web entrance, not midair.
            p.stopRiding();
            p.setForcedPose(null);
            p.setPose(Pose.STANDING);
            p.teleportTo(level,landing.x,landing.y,landing.z,90,0);
            if(p.serverLevel()!=level||p.position().distanceToSqr(landing)>.25)return false;
        }
        p.fallDistance=0;
        var web=RescueProps.ensure(p,s,"Web",RescueProps.WEB,0,Vec3.atLowerCornerOf(RescueWebSite.center(p,s)));
        if(web==null||web.level()!=level)return false;
        web.frame(0,0,1);
        if(mother.level()!=level||mother.distanceToSqr(motherAt)>1){
            var moved=ShowTravel.guest(mother,p,s,level,motherAt,true,"BeldamFinaleReturn");
            if(!(moved instanceof ParentEntity original)||original.level()!=level||!original.getUUID().equals(n.getUUID("WebEntryMother"))
                    ||original.distanceToSqr(motherAt)>1){
                n.putString("Blocker","Waiting for the original Other Mother to reach the web.");
                return false;
            }
            mother=original;
        }
        n.putLong("BeldamAt",mother.blockPosition().asLong());
        n.putBoolean("WebEntered",true);
        n.putBoolean("WebEntryReady",true);
        n.remove("WebEntryRecheck");
        if(RescueChapter.phase(s)==WEB_STEALTH){
            // A recovery teleport is not a player footstep. Do not let the
            // hearing AI interpret it as a deafening first movement packet.
            n.remove("NoiseX");
            rememberPosition(p,n);
            RescueWebHearing.reset(p);
        }
        n.remove("Blocker");
        OtherMotherLoop.changed(p);
        return true;
    }
    private static void rememberPosition(ServerPlayer p,CompoundTag n){n.putDouble("PreviousX",p.getX());n.putDouble("PreviousY",p.getY());n.putDouble("PreviousZ",p.getZ());n.putBoolean("PreviousGround",p.onGround());}
    private static void web(ServerPlayer p,VisitData.Scene s,ParentEntity mother,int t){
        var n=RescueChapter.data(s);var center=RescueWebSite.center(p,s);double moved=Math.hypot(p.getX()-n.getDouble("PreviousX"),p.getZ()-n.getDouble("PreviousZ"));boolean sticky=false;double best=3;for(int lane=0;lane<3;lane++)for(int sample=0;sample<=RescueWebLayout.SAMPLES;sample+=4){var point=RescueWebSite.at(p,s,lane,sample/(double)RescueWebLayout.SAMPLES);double d=p.position().distanceToSqr(point);if(d<best){best=d;sticky=RescueWebLayout.sticky(lane,sample);}}
        if(sticky&&p.onGround())p.makeStuckInBlock(ModBlocks.STORY_WEB.get().defaultBlockState(),new Vec3(.65,1,.65));
        boolean landed=RescueWebLayout.audibleLanding(n.getDouble("PreviousY"),p.getY(),p.onGround()&&!n.getBoolean("PreviousGround"));
        if(t<=1){for(int i=0;i<5;i++)n.remove("WebTravel"+i);n.remove("WebTravelSamples");n.putInt("HearingNoiseEvents",0);}
        var movement=RescueWebHearing.consume(p,moved);double step=movement.loudest(),travel=movement.distance();int samples=movement.packets();
        boolean noise=RescueChallengeMath.hears(step,p.isCrouching(),p.isSprinting(),landed,sticky);n.putDouble("LastWebTravel",travel);n.putDouble("LastWebStep",step);n.putInt("LastWebPackets",samples);
        n.putDouble("LastWebMoved",moved);n.putBoolean("LastWebSticky",sticky);n.putBoolean("LastWebLanded",landed);
        if(noise){n.putString("LastNoiseReason",landed?"LANDING":sticky?"STICKY":p.isSprinting()?"SPRINT":p.isCrouching()?"FAST_CROUCH":"WALK");n.putDouble("LastNoiseMoved",step);n.putDouble("LastNoiseTravel",travel);n.putInt("LastNoiseSamples",samples);int events=n.getInt("HearingNoiseEvents");n.putInt("HearingNoiseEvents",events+1);if(events<12)org.slf4j.LoggerFactory.getLogger(BeldamFinale.class).debug("Web noise t={} displacement={} loudestStep={} acceptedTravel={} packets={} crouching={} sprint={} landing={} sticky={}",t,moved,step,travel,samples,p.isCrouching(),p.isSprinting(),landed,sticky);n.putDouble("NoiseX",p.getX());n.putDouble("NoiseY",p.getY());n.putDouble("NoiseZ",p.getZ());n.putInt("NoiseAge",0);if(t%6==0)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SPIDER_STEP,net.minecraft.sounds.SoundSource.BLOCKS,.3F,1.4F);}else n.putInt("NoiseAge",n.getInt("NoiseAge")+1);
        // Pursuit target is only the most recently heard position, never p.position().
        if(mother!=null){mother.setInvisible(false);mother.setNoAi(true);mother.setNoGravity(true);mother.noPhysics=true;if(n.contains("NoiseX")&&n.getInt("NoiseAge")<160){var heard=new Vec3(n.getDouble("NoiseX"),n.getDouble("NoiseY")-.5,n.getDouble("NoiseZ"));var delta=heard.subtract(mother.position());mother.setPos(mother.position().add(delta.length()<.095?delta:delta.normalize().scale(.095)));mother.setYRot(face(mother.position(),heard));mother.setYBodyRot(mother.getYRot());}n.putLong("BeldamAt",mother.blockPosition().asLong());if(mother.getBoundingBox().inflate(.35).intersects(p.getBoundingBox())){AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS);return;}}
        rememberPosition(p,n);var end=RescueWebSite.at(p,s,1,1);var door=RescueProps.ensure(p,s,"WebDoor",RescueProps.PURSUING_DOOR,0,end.add(0,0,-1));if(door!=null)door.frame(0,t,1);RescueChapter.hint(p,sticky?"Sticky strand — keep still to stop making noise.":"Crouch and follow the thick strands. She follows sound, not sight.");if(p.distanceToSqr(end)<4&&p.getY()>end.y-.6)RescueChapter.go(p,s,DOOR_KICK);
    }
    private static void tunnel(ServerPlayer p,VisitData.Scene s,int t,boolean done){
        var n=RescueChapter.data(s);p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);p.fallDistance=0;
        double progress=RescuePassage.progress(p,s),pursuit=RescueWebLayout.crawlPursuit(t);var origin=RescuePassage.origin(p,s);
        var door=RescueProps.ensure(p,s,"PursuingDoor",RescueProps.PURSUING_DOOR,0,Vec3.atBottomCenterOf(origin).add(pursuit,0,0));
        if(door!=null){door.setPos(Vec3.atBottomCenterOf(origin).add(pursuit,0,0));door.setYRot(90);door.frame(2,t,1);}
        RescueChapter.hint(p,"CRAWL! Dodge the rubble and reach the real door.");
        if(progress<pursuit+1.1||progress<-.5||Math.abs(p.getZ()-origin.getZ()-.5)>2){retry(p,s,TUNNEL_CHASE,"The door caught up. Retry the crawl with your recovered key.");return;}
        if(progress<RescuePassage.LENGTH||!done)return;
        var source=DoorData.get(p.server).doors.get(s.door);if(source==null||!s.owner.equals(source.owner))return;
        var level=FamilyData.level(p.server,source.dimension);if(level==null||level==p.server.getLevel(ActivityWorld.KEY))return;
        if(!RescueChapter.transfer(p,level,source.pos))return;
        n.putBoolean("CrawlReachedOverworld",true);RescueChapter.go(p,s,LOCK_DOOR);
    }
    public static void input(ServerPlayer p,VisitData.Scene s,String action){
        var state=RescueChapter.phase(s);var n=RescueChapter.data(s);
        if(roomLocked(s)&&(p.serverLevel()!=p.server.getLevel(OtherWorld.KEY)||!FirstEscapeScene.fullyInRoom(s,p.getBoundingBox())))return;
        if(state==SHOW_EYES&&action.equals("show_eyes")){if(recoverPresentationEyes(p,s))RescueChapter.go(p,s,FINAL_D);else p.displayClientMessage(net.minecraft.network.chat.Component.literal("Retrieve the original eyes from your helper or make room in your inventory. You can close this screen and try again."),false);}
        else if(state==SHOW_EYES&&action.equals("close_eyes"))RescueChapter.go(p,s,FINAL_C);
        else if(state==HIDING_CHOICE&&action.equals("behind_little_door"))RescueChapter.go(p,s,FINAL_H);
        else if(state==FIGHT_CHOICE&&action.equals("fight"))RescueChapter.go(p,s,FINAL_M);
        else if(state==DOOR_STRUGGLE&&action.equals("struggle")&&p.serverLevel()==p.server.getLevel(ActivityWorld.KEY)
                &&p.distanceToSqr(RescueWebSite.at(p,s,1,1))<9)n.putLong("PressUntil",p.serverLevel().getGameTime()+6);
        else if(state==CAT_THROW&&action.equals("throw_cat")&&n.getBoolean("CatHeld")&&!n.getBoolean("CatFlying")){
            var cat=ShowActors.actor(p,s,"cat");if(cat==null)return;
            var look=p.getLookAngle().normalize();var from=catThrowOrigin(p);var v=look.scale(1.25).add(0,.18,0);var mother=ParentRescueStory.mother(p,s);
            if(mother!=null){var hit=faceBox(mother).clip(p.getEyePosition(),p.getEyePosition().add(look.scale(24)));if(hit.isPresent())v=catThrowVelocity(from,hit.get());}
            cat.setPos(from);n.putDouble("CatVX",v.x);n.putDouble("CatVY",v.y);n.putDouble("CatVZ",v.z);n.putInt("CatFlightTick",0);n.putBoolean("CatFlying",true);
        }
    }
    public static boolean interact(ServerPlayer p,VisitData.Scene s,Entity target){var n=RescueChapter.data(s);if(RescueChapter.phase(s)==CAT_THROW&&target==ShowActors.actor(p,s,"cat")&&!n.getBoolean("CatFlying")){n.putBoolean("CatHeld",true);return true;}return false;}
    public static boolean block(ServerPlayer p,BlockPos at){var s=OtherMotherLoop.scene(p);if(!RescueChapter.owns(p,s)||RescueChapter.phase(s)!=LOCK_DOOR)return false;var n=RescueChapter.data(s);var d=DoorData.get(p.server).doors.get(s.door);if(d==null||!s.owner.equals(d.owner)||p.serverLevel()!=FamilyData.level(p.server,d.dimension)||!at.equals(d.pos)||p.distanceToSqr(at.getCenter())>16)return false;boolean key=false;for(int i=0;i<p.getInventory().getContainerSize();i++)key|=ParentRescueStory.key(p.getInventory().getItem(i),s);if(!key||!n.getBoolean("CrawlReachedOverworld")||!n.getBoolean("KeyRecovered"))return true;var frame=p.serverLevel().getBlockState(at);if(!frame.is(ModBlocks.TINY_DOOR.get())||!(p.serverLevel().getBlockEntity(at) instanceof DoorBlockEntity tile)||!s.door.equals(tile.doorId))return true;
        var closed=frame.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false);
        p.serverLevel().setBlock(at,closed,3);
        if(!p.serverLevel().getBlockState(at).equals(closed))return true;
        n.putBoolean("PermanentlySealed",true);ParentRescueStory.data(s).putBoolean("DoorLocked",true);d.activeUntil=0;DoorData.get(p.server).setDirty();RescueChapter.go(p,s,BANGING);return true;}
    public static void retry(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state,String reason){
        var n=RescueChapter.data(s);n.putString("LastRetryReason",reason);n.putString("LastRetryPhase",state.name());n.putInt("RetryCount",n.getInt("RetryCount")+1);org.slf4j.LoggerFactory.getLogger(BeldamFinale.class).debug("Rescue retry phase={} reason={} feet={} crouching={} moved={} sticky={} landed={} noiseAge={} lastNoise={} lastNoiseMoved={}",state,reason,p.position(),p.isCrouching(),n.getDouble("LastWebMoved"),n.getBoolean("LastWebSticky"),n.getBoolean("LastWebLanded"),n.getInt("NoiseAge"),n.getString("LastNoiseReason"),n.getDouble("LastNoiseMoved"));RescueChapter.hint(p,reason);p.setHealth(p.getMaxHealth());p.clearFire();p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        if(state==CAT_THROW){
            if(!RescueChapter.transfer(p,p.server.getLevel(OtherWorld.KEY),FirstEscapeScene.room(s)))return;
            n.remove("CatFlying");n.remove("CatHeld");n.remove("CatMantel");var mother=ParentRescueStory.mother(p,s);if(mother!=null)motherAt(mother,Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)),p,67);catAtMantel(p,s);
        }else if(state==WEB_STEALTH){
            // Avoid teleporting directly onto an unverified, possibly missing
            // web after a saved-world interruption. The next preflight checks
            // the actual journal, then enterWeb places both original actors.
            n.remove("WebEntryReady");n.remove("WebEntered");n.remove("WebEntryRecheck");
            n.remove("NoiseX");n.remove("WebVerified");n.putInt("WebVerify",0);
            RescueWebHearing.reset(p);
        }else if(state==DOOR_STRUGGLE){
            // An interrupted door struggle must not drop the player onto a
            // possibly absent web bridge. Keep this retry pending until the
            // original physical journal has been verified in bounded batches.
            if(!n.getBoolean("WebDoorRetryRecheck")){
                n.putBoolean("WebDoorRetryRecheck",true);
                n.remove("WebVerified");n.putInt("WebVerify",0);
            }
            if(!RescueWebSite.prepare(p,s)){n.putBoolean("Recover",true);return;}
            var at=RescueWebSite.at(p,s,1,1);
            if(!RescueChapter.transfer(p,p.server.getLevel(ActivityWorld.KEY),BlockPos.containing(at).above())){
                n.putBoolean("Recover",true);return;
            }
            n.remove("WebDoorRetryRecheck");
            if(!n.getBoolean("HandCutPending"))n.putFloat("Struggle",0);
            n.remove("PressUntil");
        }else if(state==TUNNEL_CHASE){n.remove("PassageEntered");if(!RescuePassage.enter(p,s))return;}
        RescueChapter.go(p,s,state);
    }
    private BeldamFinale(){}
}
