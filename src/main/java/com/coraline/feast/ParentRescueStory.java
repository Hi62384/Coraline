package com.coraline.feast;

import java.util.*;
import com.coraline.block.TinyDoorBlock;
import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.registry.ModItems;
import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One-way Missing Parents return. The final checkpoint deliberately does not resume ordinary events. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ParentRescueStory {
    public enum Phase { A,TUNNEL_EXIT,B,C,D,FATHER_ENTRY,KEY_SCENE,E,F,G,KITCHEN,WAIT_KITCHEN_SEAT,H,I,J,K,SERVE,L,M,LOOK_AWAY,WAIT_LOOK_BACK,N,PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED }
    public static CompoundTag data(VisitData.Scene s){if(s==null)return new CompoundTag();if(!s.loop.story.contains("ParentRescue"))s.loop.story.put("ParentRescue",new CompoundTag());return s.loop.story.getCompound("ParentRescue");}
    public static boolean started(VisitData.Scene s){return s!=null&&!AcceptedButtonEyes.active(s)&&s.loop.story.getCompound("ParentRescue").getBoolean("Started");}
    public static CompoundTag scenery(VisitData.Scene s){var n=data(s);if(!n.contains("Scenery"))n.put("Scenery",new CompoundTag());return n.getCompound("Scenery");}
    public static Phase phase(VisitData.Scene s){try{return Phase.valueOf(data(s).getString("Phase"));}catch(IllegalArgumentException e){return Phase.A;}}
    private static String voice(Phase phase){return phase.name().length()==1?"parent_rescue_"+phase.name().toLowerCase(Locale.ROOT):"";}
    private static void go(ServerPlayer p,VisitData.Scene s,Phase next){ShowPlayback.transition(p,s,voice(next));var n=data(s);if(next==Phase.B&&!n.getBoolean("ArrivalMusicStarted"))n.putLong("ArrivalMusicAt",p.serverLevel().getGameTime()+80);if(next==Phase.H&&n.getBoolean("KitchenSeated")&&!n.getBoolean("KitchenMusicStarted")){VisitAudio.play(p,"other_mother_game_proposal_music",true);n.putBoolean("KitchenMusicStarted",true);}n.putString("Phase",next.name());n.putInt("Ticks",0);n.remove("Audio");OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();sync(p,s);}
    public static boolean key(ItemStack stack,VisitData.Scene s){return stack.is(ModItems.LITTLE_DOOR_STORY_KEY.get())&&stack.hasTag()&&stack.getTag().hasUUID("StoryKeyOwner")&&s.owner.equals(stack.getTag().getUUID("StoryKeyOwner"))&&stack.getTag().hasUUID("StoryKeyDoor")&&s.door.equals(stack.getTag().getUUID("StoryKeyDoor"));}
    static ItemStack boundKey(VisitData.Scene s){var stack=new ItemStack(ModItems.LITTLE_DOOR_STORY_KEY.get());stack.getOrCreateTag().putUUID("StoryKeyOwner",s.owner);stack.getOrCreateTag().putUUID("StoryKeyDoor",s.door);return stack;}
    public static boolean grantKey(ServerPlayer p,VisitData.Scene s){if(s==null||!s.owner.equals(p.getUUID()))return false;var story=s.loop.story;if(story.getString("LittleDoorKeyHolder").equals("EVIL_OTHER_MOTHER"))return false;for(var stack:p.getInventory().items)if(key(stack,s))return true;if(story.getBoolean("LittleDoorKeyIssued")||p.getInventory().getFreeSlot()<0)return false;var stack=boundKey(s);SecretTransactions.give(p,stack);if(!stack.isEmpty())return false;story.putBoolean("LittleDoorKeyIssued",true);story.putInt("LittleDoorKeyCustodyVersion",1);story.putString("LittleDoorKeyHolder","PLAYER");p.sendSystemMessage(net.minecraft.network.chat.Component.literal("You take the Little Door Key for your return."));OtherMotherLoop.changed(p);return true;}
    public static boolean begin(ServerPlayer p,VisitData.Scene s){if(started(s))return true;if(!s.owner.equals(p.getUUID())||!MissingParentsCat.data(s).getBoolean("Complete")||!MissingParentsCat.tunnelRestored(s)||p.serverLevel()!=p.server.overworld())return false;boolean has=false;for(int i=0;i<p.getInventory().getContainerSize();i++)has|=key(p.getInventory().getItem(i),s);if(!has&&!grantKey(p,s)){FirstEscapeStory.hint(p,"Bring the Little Door Key.");return false;}var n=data(s);n.putBoolean("Started",true);var scene=new CompoundTag();var old=FirstEscapeStory.data(s);for(String k:List.of("House","LittleDoor","Prison"))scene.putLong(k,old.getLong(k));n.put("Scenery",scene);s.loop.story.putString("MajorStoryPhase","PARENT_RESCUE_RETURN");p.closeContainer();p.stopRiding();VisitAudio.stop(p);prepareReturnRoom(p,s);StoryTunnel.crawl(p,s,2);go(p,s,Phase.A);return true;}
    public static ParentEntity mother(ServerPlayer p,VisitData.Scene s){
        if(s==null||s.mom==null)return null;var n=data(s);var other=p.server.getLevel(OtherWorld.KEY);
        if(other!=null){LoopProps.hold(other,BlockPos.of(scenery(s).getLong("House")));if(n.contains("MotherAt"))LoopProps.hold(other,BlockPos.of(n.getLong("MotherAt")));if(!s.loop.motherPos.equals(BlockPos.ZERO))LoopProps.hold(other,s.loop.motherPos);}
        var continuation=n.getCompound("Continuation");var activity=p.server.getLevel(ActivityWorld.KEY);if(activity!=null&&continuation.contains("BeldamAt"))LoopProps.hold(activity,BlockPos.of(continuation.getLong("BeldamAt")));
        for(var level:p.server.getAllLevels())if(level.getEntity(s.mom) instanceof ParentEntity mother&&mother.isOtherParent()){LoopProps.hold(level,mother.blockPosition());return mother;}
        return null; // An unloaded canonical UUID is never permission to spawn another mother.
    }
    private static CharacterEntity father(ServerPlayer p,VisitData.Scene s){if(data(s).contains("FatherAt"))p.serverLevel().getChunkAt(BlockPos.of(data(s).getLong("FatherAt")));var raw=ShowActors.actor(p,s,"evil_other_father");return raw instanceof CharacterEntity c?c:null;}
    private static CharacterEntity cat(ServerPlayer p,VisitData.Scene s){var n=data(s);var c=ShowActors.actor(p,s,"cat");if((c==null||c.level()!=p.level())&&!n.getBoolean("CatLeft")){var at=FirstEscapeStory.tunnelCell(s,0,0,1);c=ShowActors.acquire(p,s,"cat",p.server.getLevel(OtherWorld.KEY),Vec3.atBottomCenterOf(at));}return c instanceof CharacterEntity e?e:null;}
    static void guideTunnelCat(ServerPlayer p,VisitData.Scene s,CharacterEntity cat,double playerProgress){
        if(cat.level()!=p.level())return;cat.setInvisible(false);cat.setInvulnerable(true);cat.storyCatCrawl(true);cat.getNavigation().stop();cat.setNoAi(true);cat.setNoGravity(true);cat.noPhysics=false;cat.setDeltaMovement(Vec3.ZERO);
        var origin=Vec3.atBottomCenterOf(BlockPos.of(FirstEscapeStory.data(s).getLong("EscapeTunnel")));var forward=Vec3.atLowerCornerOf(FirstEscapeStory.escapeDirection(s).getNormal());
        // Follow the traveller instead of occupying the remaining crawl passage.
        // Relocate immediately if an older save left the Cat ahead of the player.
        double progress=cat.position().subtract(origin).dot(forward),goal=Math.max(1,playerProgress-2.5);
        var destination=origin.add(forward.scale(goal));var delta=destination.subtract(cat.position());
        var step=delta.lengthSqr()<.0001?Vec3.ZERO:delta.normalize().scale(Math.min(.28,delta.length()));
        if(progress>playerProgress-1||delta.lengthSqr()>16)cat.setPos(destination);
        else if(!p.serverLevel().getBlockCollisions(cat,cat.getBoundingBox().expandTowards(step).deflate(.001)).iterator().hasNext())cat.setPos(cat.position().add(step));
        float yaw=FirstEscapeStory.escapeDirection(s).toYRot();cat.setYRot(yaw);cat.setYBodyRot(yaw);cat.setYHeadRot(yaw);cat.gameGesture(step.lengthSqr()>.0001?32:31,10);
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){
        if(!started(s)||!s.owner.equals(p.getUUID())||!p.isAlive())return;var n=data(s);
        if(RescueChapter.started(s)||n.getBoolean("Complete")&&phase(s)==Phase.PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED){RescueChapter.tick(p,s);return;}
        p.fallDistance=0;if(p.level()!=p.server.getLevel(OtherWorld.KEY))return;
        if(n.getBoolean("Recover")){n.remove("Recover");n.remove("Audio");ShowPlayback.stop(p,s);}
        if(n.contains("ArrivalMusicAt")&&!n.getBoolean("ArrivalMusicStarted")&&p.serverLevel().getGameTime()>=n.getLong("ArrivalMusicAt")){
            VisitAudio.play(p,"parents_rescue_arrival_music",true);n.putBoolean("ArrivalMusicStarted",true);n.remove("ArrivalMusicAt");
        }
        var state=phase(s);var m=mother(p,s);if(m==null&&state!=Phase.A&&state!=Phase.TUNNEL_EXIT){n.putString("Blocker","Waiting for the canonical Other Mother to load; no duplicate created.");return;}
        var family=ParentAccess.family(p.server,s.owner);if(m!=null){if(family!=null){m.makeOtherParent(Math.max(0,family.momSkin));if(!family.storyMissing){family.storyMissing=true;FamilyData.get(p.server).setDirty();}}n.putLong("MotherAt",m.blockPosition().asLong());LoopProps.hold(p.serverLevel(),m.blockPosition());}
        var savedFather=father(p,s);if(savedFather!=null){n.putLong("FatherAt",savedFather.blockPosition().asLong());LoopProps.hold(p.serverLevel(),savedFather.blockPosition());}for(String key:scenery(s).getAllKeys())if(key.endsWith("Position"))LoopProps.hold(p.serverLevel(),BlockPos.of(scenery(s).getLong(key)));
        String sound=voice(state);int t=n.getInt("Ticks");boolean done=false;if(!sound.isEmpty()){if(!n.getBoolean("Audio")){if(!ShowPlayback.start(p,s,sound,false)){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));return;}n.putBoolean("Audio",true);}done=ShowPlayback.tick(p,s);t=ShowPlayback.position(s);n.putInt("Ticks",t);if(!ShowPlayback.data(s).getString("Blocker").isEmpty()){sync(p,s);return;}}else n.putInt("Ticks",++t);
        if(n.getBoolean("DoorLocked")){var little=BlockPos.of(scenery(s).getLong("LittleDoor"));var stateAtDoor=p.level().getBlockState(little);if(stateAtDoor.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())&&stateAtDoor.getValue(TinyDoorBlock.OPEN))p.serverLevel().setBlock(little,stateAtDoor.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false),3);}
        if(n.getBoolean("Seated")){var chair=FirstEscapeScene.prop(p,s,n.getBoolean("KitchenSeated")?"KitchenSeat":"BugChair");if(chair!=null&&p.getVehicle()!=chair)p.startRiding(chair,true);}
        switch(state){
            case A,TUNNEL_EXIT->{p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);var origin=Vec3.atBottomCenterOf(BlockPos.of(FirstEscapeStory.data(s).getLong("EscapeTunnel")));var forward=FirstEscapeStory.escapeDirection(s);double progress=p.position().subtract(origin).dot(Vec3.atLowerCornerOf(forward.getNormal()));var c=cat(p,s);if(c!=null){guideTunnelCat(p,s,c,progress);}if(state==Phase.A&&done){n.putBoolean("TunnelSpeechFinished",true);go(p,s,Phase.TUNNEL_EXIT);}if(n.getBoolean("TunnelSpeechFinished")){FirstEscapeScene.place(p,s,FirstEscapeStory.tunnelCell(s,0,0,22),Blocks.AIR.defaultBlockState());if(progress>=20&&emerge(p,s)){n.putBoolean("CatLeft",true);ShowActors.restore(p,s);if(m!=null){m.stopRiding();m.setInvisible(false);m.setPos(Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)));m.lookAtStoryPlayer(p);}go(p,s,Phase.B);}}}
            case B->{m.lookAtStoryPlayer(p);if(done){FirstEscapeScene.prepareRoom(p,s);FirstEscapeScene.roomProps(p,s);go(p,s,Phase.C);}}
            case C->{float u=Math.min(1,t/(float)Math.max(1,ShowRecordings.duration(sound)));n.putFloat("Morph",u);parlor(p,s,u,t);m.lookAtStoryPlayer(p);if(done){n.putBoolean("Evil",true);go(p,s,Phase.D);}}
            case D->{m.lookAtStoryPlayer(p);if(done)go(p,s,Phase.FATHER_ENTRY);}
            case FATHER_ENTRY->fatherEntry(p,s,t);
            case KEY_SCENE->keyScene(p,s,m,t);
            case E,F->{m.lookAtStoryPlayer(p);var dad=father(p,s);if(dad!=null)ShowPlaybackNetwork.pose(dad,state==Phase.F?8:0,t,1);if(done)go(p,s,state==Phase.E?Phase.F:Phase.G);}
            case G->{var outside=FirstEscapeScene.entrance(s).east(3);var dad=father(p,s);MissingParentsCat.walk(m,Vec3.atBottomCenterOf(outside),.9);if(dad!=null){dad.partyDoorDuck(true);MissingParentsCat.walk(dad,m.position().add(-.6,0,0),.9);ShowPlaybackNetwork.pose(dad,6,t,1);}if(!FirstEscapeScene.inRoom(s,m.blockPosition())&&dad!=null&&!FirstEscapeScene.inRoom(s,dad.blockPosition())){int elapsed=n.getInt("OutsideTicks")+1;n.putInt("OutsideTicks",elapsed);if(elapsed>=40)release(p,s);}if(done)n.putBoolean("GDone",true);if(n.getBoolean("GDone")&&!n.getBoolean("Seated")){if(dad!=null){dad.setInvisible(true);dad.getNavigation().stop();}go(p,s,Phase.KITCHEN);}}
            case KITCHEN->tickKitchenArrival(p,s,m);
            case WAIT_KITCHEN_SEAT->{if(walkKitchenMother(p,s,m))m.familyGesture(60,5);FirstEscapeStory.hint(p,"Right-click a kitchen chair to sit.");}
            case H,I,J,K->{walkKitchenMother(p,s,m);var stoveAt=BlockPos.of(n.getLong("Stove"));FirstEscapeScene.ensure(p,s,"CookingPot",CinematicPropEntity.RESCUE_POT,0,p.serverLevel(),Vec3.atBottomCenterOf(stoveAt).add(0,.9,0));if(state!=Phase.I&&t%12==0)p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,stoveAt.getX()+.5,stoveAt.getY()+1.25,stoveAt.getZ()+.5,1,.12,.05,.12,.005);m.familyGesture(state==Phase.I?0:state==Phase.K&&t>=20?61:60,5);if(done){if(state==Phase.H)go(p,s,Phase.I);else if(state==Phase.I)go(p,s,Phase.J);else if(state==Phase.J)go(p,s,Phase.K);else go(p,s,Phase.SERVE);}}
            case SERVE->{retireKitchenGift(p,s);var plate=FirstEscapeScene.ensure(p,s,"Meal",CinematicPropEntity.RESCUE_MEAL,0,p.serverLevel(),m.position().add(0,1,.4));var target=BlockPos.of(n.getLong("KitchenTable"));if(plate!=null)plate.setPos(m.position().add(0,1,.4));m.familyGesture(62,5);if(MissingParentsCat.walk(m,Vec3.atBottomCenterOf(target.west()),.65)){if(plate!=null)plate.setPos(Vec3.atBottomCenterOf(target).add(0,.9,0));n.putBoolean("MealPlaced",true);go(p,s,Phase.L);}}
            case L->{retireKitchenGift(p,s);m.lookAtStoryPlayer(p);if(done)go(p,s,Phase.M);}
            case M->{retireKitchenGift(p,s);m.lookAtStoryPlayer(p);m.familyGesture(63,5);if(done)go(p,s,Phase.LOOK_AWAY);}
            case LOOK_AWAY->{m.lookAtStoryPlayer(p);FirstEscapeStory.hint(p,"Look away from Other Mother.");if(!gaze(p,m.position().add(0,1.5,0),.20))n.putInt("AwayTicks",n.getInt("AwayTicks")+1);else n.putInt("AwayTicks",0);if(n.getInt("AwayTicks")>=12){n.putLong("MotherVanishedAt",m.blockPosition().asLong());m.setInvisible(true);m.getNavigation().stop();n.putBoolean("MotherAbsent",true);go(p,s,Phase.WAIT_LOOK_BACK);}}
            case WAIT_LOOK_BACK->{if(gaze(p,BlockPos.of(n.getLong("MotherVanishedAt")).getCenter().add(0,1,0),.80))go(p,s,Phase.N);}
            case N->{m.setInvisible(true);if(done){release(p,s);s.loop.story.putString("MajorStoryPhase",Phase.PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED.name());n.putBoolean("Complete",true);go(p,s,Phase.PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED);}}
            case PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED->{m.setInvisible(true);m.getNavigation().stop();}
        }
        if(p.tickCount%10==0)sync(p,s);if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        if(!OtherWorld.isOther(p.level()))return;var state=phase(s);var n=data(s);String sound=voice(state);
        boolean motion=state==Phase.FATHER_ENTRY||state==Phase.KEY_SCENE||state==Phase.SERVE;
        if(sound.isEmpty()&&!motion)return;
        // Fetching a key from storage is a player objective, not a failed animation.
        if(state==Phase.KEY_SCENE&&n.getInt("KeyStep")==0&&!java.util.stream.IntStream.range(0,p.getInventory().getContainerSize()).anyMatch(i->key(p.getInventory().getItem(i),s)))return;
        if(!StoryCutsceneWatchdog.overdue(p,s,"parent_rescue",state.name()+":"+n.getInt("KeyStep"),sound.isEmpty()?600:StoryCutsceneWatchdog.voiceBudget(sound)))return;
        ShowPlayback.safetyFinish(p,s,"Parent rescue action exceeded its safety window");
        switch(state){
            case A->{n.putBoolean("TunnelSpeechFinished",true);go(p,s,Phase.TUNNEL_EXIT);}
            case B->{FirstEscapeScene.prepareRoom(p,s);FirstEscapeScene.roomProps(p,s);go(p,s,Phase.C);}
            case C->{n.putFloat("Morph",1);n.putBoolean("Evil",true);parlor(p,s,1,200);go(p,s,Phase.D);}
            case D->go(p,s,Phase.FATHER_ENTRY);
            case FATHER_ENTRY->{release(p,s);n.putBoolean("ChairPlaced",true);go(p,s,Phase.KEY_SCENE);}
            case KEY_SCENE->{
                if(!n.getBoolean("KeyTaken")&&!StoryItemCustody.capture(p,s,"Key",stack->key(stack,s))){release(p,s);StoryCutsceneWatchdog.retry(s,"parent_rescue");return;}
                n.putBoolean("KeyTaken",true);n.putBoolean("KeySwallowed",true);s.loop.story.putString("LittleDoorKeyHolder","EVIL_OTHER_MOTHER");
                var little=BlockPos.of(scenery(s).getLong("LittleDoor"));FirstEscapeStory.repairLittleDoor(p,s);var door=p.serverLevel().getBlockState(little);if(door.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))p.serverLevel().setBlock(little,door.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false),3);
                n.putBoolean("DoorLocked",true);var wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");
                if(wardrobe!=null){wardrobe.setInvisible(false);var facing=door.hasProperty(TinyDoorBlock.FACING)?door.getValue(TinyDoorBlock.FACING):Direction.EAST;wardrobe.setPos(Vec3.atBottomCenterOf(little).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.1)));n.putBoolean("WardrobeBlocking",true);}
                var mom=mother(p,s);if(mom!=null)mom.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);go(p,s,Phase.E);
            }
            case E->go(p,s,Phase.F);case F->go(p,s,Phase.G);
            case G->{release(p,s);go(p,s,Phase.KITCHEN);}
            case H->go(p,s,Phase.I);case I->go(p,s,Phase.J);case J->go(p,s,Phase.K);case K->go(p,s,Phase.SERVE);
            case SERVE->{retireKitchenGift(p,s);var plate=FirstEscapeScene.prop(p,s,"Meal");if(plate!=null&&n.contains("KitchenTable"))plate.setPos(Vec3.atBottomCenterOf(BlockPos.of(n.getLong("KitchenTable"))).add(0,.9,0));n.putBoolean("MealPlaced",true);go(p,s,Phase.L);}
            case L->go(p,s,Phase.M);case M->go(p,s,Phase.LOOK_AWAY);
            case N->{release(p,s);n.putBoolean("Complete",true);s.loop.story.putString("MajorStoryPhase",Phase.PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED.name());go(p,s,Phase.PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED);}
            default->{}
        }
    }
    /** Build the finished set while the owner is still in the source world/crawl. */
    static void prepareReturnRoom(ServerPlayer p,VisitData.Scene s){
        FirstEscapeScene.prepareRoom(p,s);FirstEscapeScene.roomProps(p,s);
        for(String key:List.of("Wardrobe","BugLamp","BugChair","BugCouch","BugTable","Chandelier","Parlor")){
            var prop=FirstEscapeScene.prop(p,s,key);if(prop!=null)prop.frame(1,200,1);
        }
    }
    private static void parlor(ServerPlayer p,VisitData.Scene s,float u,int time){FirstEscapeScene.light(p,s,FirstEscapeScene.room(s).above(3),(int)(u*12));}
    /** Arrive at the actual threshold, facing into the room, rather than its center. */
    static boolean emerge(ServerPlayer p,VisitData.Scene s){
        prepareReturnRoom(p,s);
        var little=BlockPos.of(scenery(s).getLong("LittleDoor"));var state=p.level().getBlockState(little);
        if(!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){FirstEscapeStory.repairLittleDoor(p,s);state=p.level().getBlockState(little);}
        if(!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))return false;
        var facing=state.getValue(TinyDoorBlock.FACING);
        for(int distance=1;distance<=2;distance++){var cell=little.relative(facing,distance);if(!FirstEscapeCarry.fits(p,cell))continue;
            var at=FirstEscapeCarry.feet(p,cell);p.setForcedPose(null);p.setPose(Pose.STANDING);p.setDeltaMovement(Vec3.ZERO);
            p.teleportTo(p.serverLevel(),at.x,at.y,at.z,facing.toYRot(),0);data(s).putBoolean("EmergedAtDoor",true);StoryTunnel.retireCrawl(p,s);return true;
        }
        FirstEscapeStory.hint(p,"The Little Door exit is obstructed. Clear its threshold.");return false;
    }
    private static void fatherEntry(ServerPlayer p,VisitData.Scene s,int ticks){
        var n=data(s);for(int z:new int[]{41,42})FirstEscapeStory.setDoor(p,FirstEscapeScene.at(s,31,6,z),true);
        var dad=father(p,s);if(dad==null)dad=(CharacterEntity)ShowActors.acquire(p,s,"evil_other_father",p.serverLevel(),Vec3.atBottomCenterOf(FirstEscapeScene.entrance(s).east(2)));
        var chair=FirstEscapeScene.prop(p,s,"BugChair");if(chair==null){FirstEscapeScene.roomProps(p,s);chair=FirstEscapeScene.prop(p,s,"BugChair");}if(dad==null||chair==null)return;
        dad.partyDoorDuck(true);
        if(!n.getBoolean("ChairPlaced")){
            // The destination is beside the player, not the player's occupied collision box.
            if(!n.contains("FatherStand")){var cell=StoryFloorRoute.beside(dad,new Vec3(p.getX(),FirstEscapeScene.room(s).getY(),p.getZ()),1.1,2.5);if(cell!=null)n.putLong("FatherStand",cell.asLong());}
            if(!n.contains("FatherStand")){stageBlocked(p,s,"There is no clear place beside the chair.");return;}
            chair.setPos(dad.position().add(0,.7,.6));chair.frame(2,ticks,1);
            if(!StoryFloorRoute.move(dad,n,"FatherRoute",BlockPos.of(n.getLong("FatherStand")),.22)){stageBlocked(p,s,"Other Father's route into the room is obstructed.");return;}
            n.putBoolean("ChairPlaced",true);var forward=p.getLookAngle().multiply(1,0,1).normalize();
            chair.setPos(p.position().add(forward.scale(.6)));chair.setYRot(p.getYRot());chair.frame(0,0,1);
        }
        // No walk-away gate: he stays beside the chair while it captures the player.
        dad.getNavigation().stop();dad.setJumping(false);dad.setDeltaMovement(Vec3.ZERO);dad.setNoAi(true);
        if(!p.startRiding(chair,true)){stageBlocked(p,s,"The chair is occupied; waiting for a safe seat.");return;}
        n.remove("StageStall");n.remove("Blocker");n.putBoolean("Seated",true);go(p,s,Phase.KEY_SCENE);
    }
    private static void stageBlocked(ServerPlayer p,VisitData.Scene s,String message){
        var n=data(s);n.putString("Blocker",message);int stalled=n.getInt("StageStall")+1;n.putInt("StageStall",stalled);
        if(stalled%40==0)for(String key:List.of("FatherStand","KeyTakeStand","KeyLockStand"))n.remove(key);
        // A damaged/custom furnished room must not permanently imprison the owner.
        if(stalled>=100){release(p,s);FirstEscapeStory.hint(p,message);}
    }
    private static boolean approach(ServerPlayer p,VisitData.Scene s,ParentEntity m,String key,Vec3 point,double reach){
        var n=data(s);var horizontal=m.position().subtract(point).multiply(1,0,1);
        if(horizontal.lengthSqr()<=reach*reach&&Math.abs(m.getY()-point.y)<1.6){m.getNavigation().stop();n.remove("StageStall");n.remove("Blocker");return true;}
        if(!n.contains(key)){var cell=StoryFloorRoute.beside(m,point,.9,reach);if(cell!=null)n.putLong(key,cell.asLong());}
        if(n.contains(key)&&StoryFloorRoute.move(m,n,key+"Route",BlockPos.of(n.getLong(key)),.18)){n.remove("StageStall");n.remove("Blocker");return true;}
        stageBlocked(p,s,"Other Mother's path is blocked. Clear the space beside the chair.");return false;
    }
    private static void retireKitchenGift(ServerPlayer p,VisitData.Scene s){
        var gift=FirstEscapeScene.prop(p,s,"KitchenGift");if(gift!=null)gift.discard();
    }
    private static void clearKeyPerformance(ServerPlayer p,VisitData.Scene s){
        if(data(s).getBoolean("KeySwallowed"))return;
        // The blockade belongs after the lock-and-swallow performance, not in its sightline.
        var wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");
        if(wardrobe!=null){wardrobe.setInvisible(true);wardrobe.setPos(Vec3.atBottomCenterOf(FirstEscapeScene.room(s).offset(4,0,3)));}
    }
    static void keyScene(ServerPlayer p,VisitData.Scene s,ParentEntity m,int ticks){
        clearKeyPerformance(p,s);var n=data(s);int step=n.getInt("KeyStep");var little=BlockPos.of(scenery(s).getLong("LittleDoor"));
        var floorPlayer=new Vec3(p.getX(),FirstEscapeScene.room(s).getY(),p.getZ());
        if(step==0){
            if(!approach(p,s,m,"KeyTakeStand",floorPlayer,1.9))return;
            if(!StoryItemCustody.capture(p,s,"Key",stack->key(stack,s))){release(p,s);n.putString("Blocker","Bring the original Little Door Key out of storage before continuing.");FirstEscapeStory.hint(p,n.getString("Blocker"));return;}
            n.remove("Blocker");m.setItemSlot(EquipmentSlot.MAINHAND,StoryItemCustody.held(s,"Key"));n.putBoolean("KeyTaken",true);p.getInventory().setChanged();p.containerMenu.broadcastChanges();s.loop.story.putString("LittleDoorKeyHolder","EVIL_OTHER_MOTHER");n.putInt("KeyStep",1);n.putInt("KeyTicks",0);return;
        }
        int t=n.getInt("KeyTicks")+1;n.putInt("KeyTicks",t);
        if(step==1){
            // Hold the captured original in view before walking to the lock.
            m.getNavigation().stop();m.lookAtStoryPlayer(p);m.familyGesture(64,5);
            FirstEscapeStory.hint(p,"Other Mother has your Little Door Key.");
            if(t>=50){n.putInt("KeyStep",2);n.putInt("KeyTicks",0);}return;
        }
        if(step==2){
            m.familyGesture(0,5);
            if(!approach(p,s,m,"KeyLockStand",Vec3.atBottomCenterOf(little),1.8))return;
            n.putInt("KeyStep",3);n.putInt("KeyTicks",0);return;
        }
        if(step==3){
            m.getNavigation().stop();m.getLookControl().setLookAt(little.getX()+.5,little.getY()+.55,little.getZ()+.5);
            float yaw=(float)Math.toDegrees(Math.atan2(-(little.getX()+.5-m.getX()),little.getZ()+.5-m.getZ()));m.setYRot(yaw);m.setYBodyRot(yaw);m.setYHeadRot(yaw);m.familyGesture(68,5);
            FirstEscapeStory.hint(p,"She locks the Little Door with your key.");
            if(t==20||t>20&&!n.getBoolean("DoorLocked")){
                FirstEscapeStory.repairLittleDoor(p,s);var state=p.level().getBlockState(little);
                if(!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){stageBlocked(p,s,"Restore the Little Door's original threshold.");return;}
                p.serverLevel().setBlock(little,state.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false),3);
                p.serverLevel().playSound(null,little,net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.65F,1.15F);n.putBoolean("DoorLocked",true);
            }
            if(t>=55){n.putInt("KeyStep",4);n.putInt("KeyTicks",0);}return;
        }
        m.getNavigation().stop();m.lookAtStoryPlayer(p);
        if(step==4){
            m.familyGesture(64,5);FirstEscapeStory.hint(p,"She shows you the key to the locked door.");
            if(t>=45){n.putInt("KeyStep",5);n.putInt("KeyTicks",0);}return;
        }
        m.familyGesture(65,5);FirstEscapeStory.hint(p,t<35?"She brings the key to her mouth.":"She swallows your Little Door Key.");
        if(t==35)m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        if(t>=70){
            m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);n.putBoolean("KeySwallowed",true);
            var wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");if(wardrobe==null){FirstEscapeScene.roomProps(p,s);wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");}
            if(wardrobe==null){stageBlocked(p,s,"Waiting for the original wardrobe to load.");return;}
            var doorState=p.level().getBlockState(little);var facing=doorState.hasProperty(TinyDoorBlock.FACING)?doorState.getValue(TinyDoorBlock.FACING):Direction.EAST;
            wardrobe.setInvisible(false);var target=Vec3.atBottomCenterOf(little).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.1));var delta=target.subtract(wardrobe.position());
            wardrobe.setPos(wardrobe.position().add(delta.length()<.10?delta:delta.normalize().scale(.10)));wardrobe.frame(1,t,1);
            if(wardrobe.distanceToSqr(target)<.04){n.putBoolean("WardrobeBlocking",true);n.remove("Blocker");go(p,s,Phase.E);}
        }
    }
    public static BlockPos cookingStand(net.minecraft.server.level.ServerLevel level,BlockPos stove){var state=level.getBlockState(stove);var facing=state.hasProperty(HorizontalDirectionalBlock.FACING)?state.getValue(HorizontalDirectionalBlock.FACING):Direction.NORTH;return stove.relative(facing);}
    static void tickKitchenArrival(ServerPlayer p,VisitData.Scene s,ParentEntity mother){
        if(!kitchen(p,s))return;walkKitchenMother(p,s,mother);FirstEscapeStory.hint(p,"Go to the kitchen.");
        // Player arrival and Mother's stove approach are independent. A delayed
        // NPC must not suppress the chair prompt for a player already here.
        if(p.distanceToSqr(BlockPos.of(data(s).getLong("Kitchen")).getCenter())<64)go(p,s,Phase.WAIT_KITCHEN_SEAT);
    }
    private static boolean walkKitchenMother(ServerPlayer p,VisitData.Scene s,ParentEntity mother){
        var n=data(s);if(!n.contains("Stove"))return false;
        mother.stopRiding();var goal=cookingStand(p.serverLevel(),BlockPos.of(n.getLong("Stove")));
        if(MissingParentsCat.walk(mother,Vec3.atBottomCenterOf(goal),.95)){n.putBoolean("MotherAtStove",true);return true;}
        n.putBoolean("MotherAtStove",false);return false;
    }
    private static boolean kitchen(ServerPlayer p,VisitData.Scene s){var n=data(s);if(n.contains("Kitchen"))return true;var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null)return false;var center=HouseCabinets.at(realm.house,43,6,60);n.putLong("Kitchen",center.asLong());BlockPos stove=null;for(var at:BlockPos.betweenClosed(center.offset(-8,0,-8),center.offset(8,2,8))){var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(p.level().getBlockState(at).getBlock()).getPath();if(id.contains("furnace")||id.contains("oven")||id.contains("stove")){stove=at.immutable();break;}}if(stove==null){n.remove("Kitchen");n.putString("Blocker","The existing kitchen stove must be loaded before continuing.");return false;}n.putLong("Stove",stove.asLong());n.putLong("KitchenTable",center.asLong());var gift=FirstEscapeScene.ensure(p,s,"KitchenGift",CinematicPropEntity.BUTTON_GIFT,0,p.serverLevel(),Vec3.atBottomCenterOf(center).add(0,.9,0));if(gift!=null)gift.frame(0,0,0);return true;}
    public static boolean block(ServerPlayer p,BlockPos at){var s=OtherMotherLoop.scene(p);if(!started(s))return false;var n=data(s);if(n.getBoolean("DoorLocked")&&at.asLong()==scenery(s).getLong("LittleDoor"))return true;if(phase(s)==Phase.WAIT_KITCHEN_SEAT&&n.contains("Kitchen")&&at.distSqr(BlockPos.of(n.getLong("Kitchen")))<64){var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(p.level().getBlockState(at).getBlock()).getPath();if(id.contains("chair")){var seat=FirstEscapeScene.ensure(p,s,"KitchenSeat",CinematicPropEntity.STUNT_CARRIER,0,p.serverLevel(),Vec3.atBottomCenterOf(at).add(0,.3,0));if(seat!=null&&p.startRiding(seat,true)){n.putBoolean("KitchenSeated",true);n.putBoolean("Seated",true);go(p,s,Phase.H);}return true;}}return false;}
    private static boolean gaze(ServerPlayer p,Vec3 point,double dot){var delta=point.subtract(p.getEyePosition());return p.getLookAngle().dot(delta.normalize())>dot&&p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),point,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    static Vec3 kitchenExit(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(!n.contains("Kitchen"))return null;
        var origin=BlockPos.of(n.getLong("Kitchen"));Vec3 best=null;double distance=Double.MAX_VALUE;
        // Search horizontally on this floor; a clear basement is never an exit.
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            var cell=origin.offset(x,0,z);if(!p.serverLevel().hasChunkAt(cell)||!p.level().getFluidState(cell).isEmpty())continue;
            var feet=StoryFloorRoute.feet(p.serverLevel(),cell);var box=p.getDimensions(Pose.STANDING).makeBoundingBox(feet).deflate(.001);
            if(!p.level().getBlockState(cell.below()).isFaceSturdy(p.level(),cell.below(),Direction.UP)||!p.level().noCollision(p,box))continue;
            double cost=feet.distanceToSqr(p.position());if(cost<distance){distance=cost;best=feet;}
        }return best;
    }
    public static Vec3 kitchenDismount(ServerPlayer p,CinematicPropEntity seat){
        var s=OtherMotherLoop.scene(p);if(!started(s)||!seat.owned(s))return null;
        var n=data(s);return scenery(s).hasUUID("KitchenSeat")&&seat.getUUID().equals(scenery(s).getUUID("KitchenSeat"))?kitchenExit(p,s):null;
    }
    private static void release(ServerPlayer p,VisitData.Scene s){
        Vec3 exit=p.getVehicle() instanceof CinematicPropEntity seat?kitchenDismount(p,seat):null;
        data(s).putBoolean("Seated",false);p.stopRiding();
        if(exit!=null){p.teleportTo(p.serverLevel(),exit.x,exit.y,exit.z,p.getYRot(),p.getXRot());p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}
    }
    public static boolean parentControl(ParentEntity parent){if(!parent.isOtherParent()||!parent.getPersistentData().hasUUID("RealmDoor"))return false;for(var s:VisitData.get(parent.getServer()).scenes.values())if(started(s)&&s.door.equals(parent.getPersistentData().getUUID("RealmDoor")))return true;return false;}
    private static void sync(ServerPlayer p,VisitData.Scene s){var n=data(s);var packet=new CompoundTag();packet.putBoolean("ParentRescue",true);packet.putString("Phase",phase(s).name());packet.putInt("Ticks",n.getInt("Ticks"));packet.putInt("Duration",ShowRecordings.duration(voice(phase(s))));packet.putString("Sound",voice(phase(s)));var m=mother(p,s);packet.putInt("Mother",m==null?-1:m.getId());packet.putBoolean("Evil",n.getBoolean("Evil"));packet.putBoolean("Absent",n.getBoolean("MotherAbsent"));packet.putFloat("Morph",n.getFloat("Morph"));var tunnel=FirstEscapeStory.data(s);if(tunnel.contains("EscapeTunnel")){packet.putLong("EscapeTunnel",tunnel.getLong("EscapeTunnel"));packet.putString("EscapeDirection",tunnel.getString("EscapeDirection"));}SecretsNetwork.send(p,packet);}
    @SubscribeEvent public static void dismount(net.minecraftforge.event.entity.EntityMountEvent e){if(e.isDismounting()&&e.getEntityMounting() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(started(s)&&data(s).getBoolean("Seated")&&e.getEntityBeingMounted() instanceof CinematicPropEntity prop&&prop.owned(s))e.setCanceled(true);}}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(started(s)){data(s).putBoolean("Recover",true);ShowPlayback.stop(p,s);OtherMotherLoop.changed(p);}}}
    private ParentRescueStory(){}
}
