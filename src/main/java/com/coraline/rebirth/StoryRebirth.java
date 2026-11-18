package com.coraline.rebirth;

import com.coraline.feast.*;
import com.coraline.parents.*;
import com.coraline.palace.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import static com.coraline.rebirth.StoryRun.Phase.*;

/** Production scene adapter. Ritual commands cannot fake completion, kill arbitrary mobs or grant rewards. */
@Mod.EventBusSubscriber(modid="coraline")
public final class StoryRebirth {
    public static final int PORTRAIT=84,CIRCLE=85,FIRST_LIFE=86,FINAL_LIFE=87;
    private static final Map<UUID,StoryRun> ACTIVE=new HashMap<>();
    private static final Map<UUID,Consent> CONSENT=new HashMap<>();
    private static final Map<UUID,String> ERRORS=new HashMap<>();
    private record Consent(UUID nonce,UUID portrait,long expires,boolean second,long heldSince,long lastPulse){}
    public static boolean kind(int kind){return kind>=PORTRAIT&&kind<=FINAL_LIFE;}
    public static void reconcileProp(CinematicPropEntity prop){
        if(prop.owner==null)return;
        try{if(!StoryRunStore.ownsProp(prop.getServer(),prop.owner,prop.getUUID()))prop.discard();}catch(IOException corrupt){/* Preserve ambiguous scenery for review. */}
    }
    public static long run(ServerPlayer p){try{return StoryRunStore.status(p.server,p.getUUID()).run();}catch(IOException bad){return -1;}}
    public static boolean active(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
    private static void error(ServerPlayer p,String reason){if(!reason.equals(ERRORS.put(p.getUUID(),reason)))p.sendSystemMessage(Component.literal("Last Thread paused: "+reason));}
    private static void save(ServerPlayer p,StoryRun run)throws IOException{StoryRunStore.save(p.server,run);if(run.active())ACTIVE.put(p.getUUID(),run);else ACTIVE.remove(p.getUUID());}
    public static boolean canonicalCompletion(VisitData.Scene s){
        if(s==null||!PalaceJourney.prerequisites(s).ready())return false;
        var root=s.loop.story.getCompound("PinkPalaceJourney");
        var well=PalaceWellProgress.load(s.owner,s.door,root.getCompound("Well"));
        var key=PalaceKeyTransaction.load(s.owner,s.door,root.getCompound("KeyCustody"));
        return root.getInt("Version")==1&&root.hasUUID("Owner")&&s.owner.equals(root.getUUID("Owner"))
            &&root.hasUUID("Door")&&s.door.equals(root.getUUID("Door"))&&well.blocker().isEmpty()
            &&well.phase()==PalaceWellProgress.Phase.COMPLETE&&key.blocker().isEmpty()
            &&key.stage()==PalaceKeyTransaction.Stage.DISPOSED&&!root.contains("KeyCustodyBlocker")
            &&root.getCompound("Well").hasUUID("BundleReceipt")&&key.receipt().equals(root.getCompound("Well").getUUID("BundleReceipt"));
    }
    /** Personal replay credit never fabricates a well/key completion witness. */
    public static boolean personalCompletion(ServerPlayer p,VisitData.Scene s){
        if(s==null||!p.getUUID().equals(s.owner))return false;
        if(com.coraline.feast.GhostEscapeEnding.alternativeComplete(s))return true;
        if(canonicalCompletion(s))return true;
        if(!PalaceJourney.prerequisites(s).ready())return false;
        var root=s.loop.story.getCompound("PinkPalaceJourney");var proof=root.getCompound("PersonalFarewell");
        if(root.contains("Well")||root.getInt("Version")!=1||!root.hasUUID("Owner")||!s.owner.equals(root.getUUID("Owner"))||!root.hasUUID("Door")||!s.door.equals(root.getUUID("Door"))||!proof.hasUUID("Owner")||!s.owner.equals(proof.getUUID("Owner"))
            ||!proof.hasUUID("Door")||!s.door.equals(proof.getUUID("Door")))return false;
        try{long run=StoryRunStore.status(p.server,p.getUUID()).run();
            return proof.getLong("Run")==run&&(run>1||proof.getBoolean("WorldHandComplete")&&root.getBoolean("MapIssued"));
        }catch(IOException unavailable){return false;}
    }
    public static void ending(ServerPlayer p,VisitData.Scene scene){
        if(scene==null||!p.getUUID().equals(scene.owner))return;
        com.coraline.postgame.InfiniteWorldUnlock.record(p,scene);
        try{var run=StoryRunStore.get(p.server,p.getUUID());if(run.finish(personalCompletion(p,scene)))save(p,run);}
        catch(IOException bad){error(p,"Ending reward checkpoint: "+bad.getMessage());}
    }
    private static FamilyData.Family family(ServerPlayer p,StoryRun run){
        if(run.active()){var f=FamilyData.get(p.server).families.get(run.id("Family"));return f!=null&&p.getUUID().equals(f.owner)?f:null;}
        return ParentAccess.family(p.server,p.getUUID());
    }
    private static boolean quiet(ServerPlayer p,StoryRun run){
        var s=OtherMotherLoop.scene(p);
        boolean alternate=s!=null&&com.coraline.feast.GhostEscapeEnding.alternativeComplete(s);
        return p.isAlive()&&!p.isSpectator()&&!p.isSleeping()&&!p.isPassenger()&&p.serverLevel()==p.server.overworld()
            &&s!=null&&(s.loop.phase==OtherMotherLoopState.Phase.NONE||s.loop.phase==OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL&&personalCompletion(p,s)||alternate)
            &&(personalCompletion(p,s)||p.hasPermissions(2)&&run.details().getBoolean("DebugCompletion"))&&!ShowPlayback.started(s)&&!com.coraline.bobinsky.BobinskyVisit.pending(p);
    }
    private static boolean eligible(ServerPlayer p,StoryRun run){
        var f=family(p,run);return !com.coraline.postgame.TrueHeartBattle.recoveryPending(p)&&!com.coraline.postgame.TrueHeartBattle.encounterActive(p.server)&&!run.active()&&run.complete()&&run.claimed()&&quiet(p,run)&&f!=null
            &&f.rescued&&!f.dead&&!f.storyMissing&&f.mom!=null&&f.dad!=null&&f.bed!=null&&f.homeDim.equals("minecraft:overworld")
            &&p.server.overworld().getBlockEntity(f.bed) instanceof ParentsBedEntity bed&&f.id.equals(bed.familyId);
    }
    private static boolean roomReserved(ServerPlayer p,StoryRun run)throws IOException{
        for(var other:FamilyData.get(p.server).families.values())if(other.owner!=null&&!other.owner.equals(p.getUUID())){
            var otherRun=StoryRunStore.get(p.server,other.owner);
            if(otherRun.active()&&otherRun.details().contains("Center")&&center(otherRun).distanceToSqr(center(run))<64)return true;
        }return false;
    }
    private static void refreshHome(ServerPlayer p,StoryRun run)throws IOException{
        if(run.active()||!run.details().contains("Center"))return;var f=family(p,run);
        if(f!=null&&f.bed!=null&&f.homeDim.equals("minecraft:overworld")&&f.bed.distSqr(BlockPos.containing(center(run)))<=16)return;
        var old=existing(p,run,"Portrait");var d=run.details();
        for(String key:new String[]{"Center","PortraitPosition","Portrait","Circle","FirstLife","FinalLife"})d.remove(key);
        run.details(d);save(p,run);if(old!=null)old.discard();
    }
    private static boolean checkpointInventory(ServerPlayer p)throws IOException{
        p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);
        var n=StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));
        return PalaceKeyCheckpoint.inventory(n,p.getInventory().save(new ListTag()));
    }
    private static void reward(ServerPlayer p,StoryRun run)throws IOException{
        if(!run.rewardPending())return;
        // Player receipt is saved alongside the actual inventory BEFORE the journal is acknowledged.
        // No drop/overflow path: a full inventory leaves the same receipt pending.
        var persisted=p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        var previousReceipt=persisted.get("LastVoidHeartReceipt");if(previousReceipt!=null)previousReceipt=previousReceipt.copy();int allocatedSlot=-1;
        boolean received=persisted.hasUUID("LastVoidHeartReceipt")&&run.receipt().equals(persisted.getUUID("LastVoidHeartReceipt"));
        if(!received){
            int slot=p.getInventory().getFreeSlot();if(slot<0){p.displayClientMessage(Component.literal("Your Void Heart is reserved. Free one inventory slot."),true);return;}
            allocatedSlot=slot;
            var heart=new ItemStack(RebirthItems.VOID_HEART.get());heart.getOrCreateTag().putUUID("StoryRewardReceipt",run.receipt());
            p.getInventory().setItem(slot,heart);persisted.putUUID("LastVoidHeartReceipt",run.receipt());
            p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,persisted);
        }
        try{
            if(!checkpointInventory(p))throw new IOException("Void Heart inventory checkpoint could not be verified");
            var file=StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));
            var actual=file.getCompound("ForgeData").getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
            if(!actual.hasUUID("LastVoidHeartReceipt")||!run.receipt().equals(actual.getUUID("LastVoidHeartReceipt")))throw new IOException("Void Heart receipt read-back failed");
        }catch(IOException|RuntimeException failed){
            // Server-thread transaction: no inventory input can interleave this insertion/read-back.
            // Roll back an unverified new item before returning control, rather than leaving it spendable.
            if(allocatedSlot>=0){
                var item=p.getInventory().getItem(allocatedSlot);
                if(item.is(RebirthItems.VOID_HEART.get())&&item.hasTag()&&item.getTag().hasUUID("StoryRewardReceipt")&&run.receipt().equals(item.getTag().getUUID("StoryRewardReceipt")))p.getInventory().setItem(allocatedSlot,ItemStack.EMPTY);
                var restore=p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
                if(previousReceipt==null)restore.remove("LastVoidHeartReceipt");else restore.put("LastVoidHeartReceipt",previousReceipt);
                p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,restore);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
            }
            throw failed;
        }
        run.delivered(run.receipt());save(p,run);com.coraline.tycoon.TycoonWorld.rewardEntrance(p,run.run());p.sendSystemMessage(Component.literal("Story complete — received one Void Heart. You may leave the threads untouched."));
    }
    private static Vec3 center(StoryRun run){return Vec3.atBottomCenterOf(BlockPos.of(run.details().getLong("Center")));}
    private static Vec3 spot(StoryRun run,boolean mother){return center(run).add(mother?-1.05:1.05,0,0);}
    private static ParentEntity parent(ServerPlayer p,FamilyData.Family f,StoryRun run,boolean mother){
        var id=run.active()?run.id(mother?"Mother":"Father"):mother?f.mom:f.dad;
        String dimension=mother?f.momDim:f.dadDim;var level=FamilyData.level(p.server,dimension);var at=mother?f.momPos:f.dadPos;
        if(level==null||id==null)return null;
        level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(at),2,at);level.getChunkAt(at);
        for(var world:p.server.getAllLevels())if(world.getEntity(id) instanceof ParentEntity entity&&Objects.equals(entity.familyId,f.id)
            &&!entity.isOtherParent()&&entity.isMom()==mother){
                if(mother){f.momPos=entity.blockPosition();f.momDim=world.dimension().location().toString();}
                else{f.dadPos=entity.blockPosition();f.dadDim=world.dimension().location().toString();}
                FamilyData.get(p.server).setDirty();return entity;
            }
        return null;
    }
    private static boolean safe(ServerLevel level,Entity entity,Vec3 at){
        var pos=BlockPos.containing(at);return level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)
            &&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)
            &&level.noCollision(entity,entity.getDimensions(entity.getPose()).makeBoundingBox(at))
            &&!level.containsAnyLiquid(entity.getDimensions(entity.getPose()).makeBoundingBox(at));
    }
    private static boolean unseen(ServerPlayer owner,Entity entity,Vec3 target){
        for(var viewer:owner.server.getPlayerList().getPlayers())if(viewer.level()==entity.level()&&viewer.distanceToSqr(entity)<128*128
            &&viewer.getLookAngle().dot(entity.position().add(0,1,0).subtract(viewer.getEyePosition()).normalize())>0
            &&viewer.hasLineOfSight(entity))return false;
        for(var viewer:owner.server.getPlayerList().getPlayers())if(viewer.serverLevel()==owner.server.overworld()&&viewer.distanceToSqr(target)<128*128
            &&viewer.getLookAngle().dot(target.add(0,1,0).subtract(viewer.getEyePosition()).normalize())>0
            &&owner.server.overworld().clip(new ClipContext(viewer.getEyePosition(),target.add(0,1,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,viewer)).getType()==HitResult.Type.MISS)return false;
        return true;
    }
    private static CinematicPropEntity prop(ServerPlayer p,StoryRun run,String key,int kind,Vec3 at)throws IOException{
        var d=run.details();var level=p.server.overworld();UUID id=d.hasUUID(key)?d.getUUID(key):UUID.nameUUIDFromBytes((p.getUUID()+":"+run.run()+":"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        level.getChunkAt(BlockPos.containing(at));
        var existing=level.getEntity(id);if(existing!=null){
            if(!(existing instanceof CinematicPropEntity e)||!p.getUUID().equals(e.owner)||e.kind()!=kind)return null;
            if(!d.hasUUID(key)){d.putUUID(key,id);run.details(d);save(p,run);}return e;
        }
        // A recorded unloaded entity is not evidence of deletion. It must load before any replacement is considered.
        if(d.hasUUID(key))return null;
        var scene=OtherMotherLoop.scene(p);if(scene==null)return null;var e=CinematicPropEntity.TYPE.get().create(level);if(e==null)return null;
        e.setUUID(id);e.configure(scene,kind,0);e.setPos(at);e.setInvulnerable(true);
        if(!level.addFreshEntity(e))return null;d.putUUID(key,id);run.details(d);save(p,run);return e;
    }
    private static CinematicPropEntity existing(ServerPlayer p,StoryRun run,String key){
        var n=run.details();if(!n.hasUUID(key))return null;var e=p.server.overworld().getEntity(n.getUUID(key));
        return e instanceof CinematicPropEntity prop&&p.getUUID().equals(prop.owner)?prop:null;
    }
    private static void portrait(ServerPlayer p,StoryRun run)throws IOException{
        var f=family(p,run);if(f==null||f.bed==null)return;var n=run.details();
        if(!n.contains("Center")){
            // The established home is the player's placed Parents' Bed, not a guessed global house coordinate.
            var bed=f.bed;var probe=com.coraline.registry.ModEntities.MOM.get().create(p.server.overworld());if(probe==null)return;
            found: for(int dz=-3;dz<=3;dz++)for(int dx=-3;dx<=3;dx++){
                var c=bed.offset(dx,0,dz);Vec3 at=Vec3.atBottomCenterOf(c);
                if(c.distSqr(bed)>16||!safe(p.server.overworld(),probe,at.add(-1.05,0,0))||!safe(p.server.overworld(),probe,at.add(1.05,0,0)))continue;
                var picture=c.north(2).above();if(!p.server.overworld().getBlockState(picture).isAir()||!p.server.overworld().getBlockState(picture.above()).isAir()
                    ||!p.server.overworld().getBlockState(picture.north()).isSolidRender(p.server.overworld(),picture.north()))continue;
                n.putLong("Center",c.asLong());n.putLong("PortraitPosition",picture.asLong());run.details(n);save(p,run);break found;
            }
        }
        if(!run.details().contains("Center"))return;
        var at=Vec3.atBottomCenterOf(BlockPos.of(run.details().getLong("PortraitPosition"))).add(0,0,-.35);
        var portrait=prop(p,run,"Portrait",PORTRAIT,at);if(portrait==null)return;
        portrait.frame(run.active()?run.phase().ordinal():0,(int)Math.min(Integer.MAX_VALUE,run.ticks()),1);
        if(p.distanceToSqr(portrait)<36&&!run.active())p.displayClientMessage(Component.literal("The Ritual of the Last Thread"),true);
    }
    public static boolean interact(ServerPlayer p,CinematicPropEntity prop){
        if(!kind(prop.kind()))return false;
        if(!p.getUUID().equals(prop.owner)||p.distanceToSqr(prop)>16||!p.hasLineOfSight(prop)||!p.isAlive()||p.isSpectator())return true;
        try{
            var run=StoryRunStore.get(p.server,p.getUUID());
            if(prop.kind()==PORTRAIT&&eligible(p,run)&&run.details().hasUUID("Portrait")&&run.details().getUUID("Portrait").equals(prop.getUUID())){
                var c=new Consent(UUID.randomUUID(),prop.getUUID(),p.server.overworld().getGameTime()+1200,false,-1,-1);CONSENT.put(p.getUUID(),c);show(p,c);return true;
            }
            boolean first=prop.kind()==FIRST_LIFE;String key=first?"FirstLife":"FinalLife";
            var tool=p.getMainHandItem();var tag=tool.getTag();
            if(run.active()&&run.details().hasUUID(key)&&run.details().getUUID(key).equals(prop.getUUID())&&tool.is(RebirthItems.CUTTER.get())
                &&tag!=null&&tag.hasUUID("RitualOwner")&&p.getUUID().equals(tag.getUUID("RitualOwner"))&&tag.hasUUID("RitualSession")&&run.session().equals(tag.getUUID("RitualSession"))
                &&run.cut(p.getUUID(),run.session(),first)){
                save(p,run);sound(p,SoundEvents.WOOL_BREAK,.35F,.55F);p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
            }
        }catch(IOException|RuntimeException bad){error(p,bad.getMessage());}return true;
    }
    private static void show(ServerPlayer p,Consent c){var n=new CompoundTag();n.putString("Mode",c.second?"confirm":"explain");n.putUUID("Nonce",c.nonce);RebirthNetwork.send(p,n);}
    public static void input(ServerPlayer p,UUID nonce,String action){
        var c=CONSENT.get(p.getUUID());long now=p.server.overworld().getGameTime();
        if(c==null||!c.nonce.equals(nonce)||now>c.expires)return;
        if(action.equals("cancel")){CONSENT.remove(p.getUUID());return;}
        try{
            var run=StoryRunStore.get(p.server,p.getUUID());var portrait=existing(p,run,"Portrait");
            if(!eligible(p,run)||portrait==null||!portrait.getUUID().equals(c.portrait)||p.distanceToSqr(portrait)>16||!p.hasLineOfSight(portrait)){CONSENT.remove(p.getUUID());return;}
            if(action.equals("begin")&&!c.second){c=new Consent(UUID.randomUUID(),c.portrait,now+1200,true,-1,-1);CONSENT.put(p.getUUID(),c);show(p,c);return;}
            if(!c.second)return;
            if(action.equals("release")){CONSENT.put(p.getUUID(),new Consent(c.nonce,c.portrait,c.expires,true,-1,-1));return;}
            if(action.equals("hold")){
                long since=c.heldSince<0||now-c.lastPulse>8?now:c.heldSince;
                c=new Consent(c.nonce,c.portrait,c.expires,true,since,now);CONSENT.put(p.getUUID(),c);
                if(now-since<60)return;
                var f=family(p,run);var mom=parent(p,f,run,true);var dad=parent(p,f,run,false);
                if(roomReserved(p,run)){p.displayClientMessage(Component.literal("Another family's ritual currently occupies this room."),true);return;}
                if(mom==null||dad==null||!mom.isAlive()||!dad.isAlive())return;
                if(!safe(p.server.overworld(),mom,spot(run,true))||!safe(p.server.overworld(),dad,spot(run,false)))return;
                var retained=MainStoryRebirth.capture(p,OtherMotherLoop.scene(p));
                if(!run.commit(f.id,mom.getUUID(),dad.getUUID(),OtherMotherLoop.scene(p).door,retained))return;
                var d=run.details();d.putInt("MomSkin",mom.skin());d.putInt("DadSkin",dad.skin());run.details(d);
                save(p,run);CONSENT.remove(p.getUUID());p.closeContainer();
                for(var parent:List.of(mom,dad))parent.getPersistentData().putUUID("LastThreadSession",run.session());
                var n=new CompoundTag();n.putString("Mode","close");RebirthNetwork.send(p,n);
            }
        }catch(IOException|RuntimeException bad){error(p,bad.getMessage());}
    }
    private static void sound(ServerPlayer p,SoundEvent sound,float volume,float pitch){p.playNotifySound(sound,SoundSource.AMBIENT,volume,pitch);}
    private static void go(ServerPlayer p,StoryRun run,StoryRun.Phase next)throws IOException{
        if(!run.advance(run.session(),run.phase(),next))throw new IllegalStateException("Invalid ritual transition");save(p,run);
    }
    private static boolean summon(ServerPlayer p,StoryRun run,ParentEntity parent,boolean mother){
        if(parent==null||!parent.isAlive())return false;var at=spot(run,mother);
        if(parent.level()==p.level()&&parent.distanceToSqr(at)<.25){parent.getNavigation().stop();return true;}
        if(parent.level()==p.level())parent.getNavigation().moveTo(at.x,at.y,at.z,.65);
        if(run.ticks()>=200&&safe(p.server.overworld(),parent,at)&&unseen(p,parent,at)){
            if(parent.level()!=p.level()){ParentEvents.transferNear(parent,p.server.overworld(),BlockPos.containing(at));return false;}
            parent.teleportTo(at.x,at.y,at.z);
        }
        return false;
    }
    private static void tool(ServerPlayer p,StoryRun run){
        for(var item:p.getInventory().items)if(item.is(RebirthItems.CUTTER.get())&&item.hasTag()&&run.session().equals(item.getTag().getUUID("RitualSession")))return;
        int slot=p.getInventory().getFreeSlot();if(slot<0){if(p.tickCount%100==0)p.sendSystemMessage(Component.literal("Free one inventory slot for the ritual thread cutter. The threads will wait."));return;}var item=new ItemStack(RebirthItems.CUTTER.get());
        item.getOrCreateTag().putUUID("RitualOwner",p.getUUID());item.getTag().putUUID("RitualSession",run.session());p.getInventory().setItem(slot,item);p.inventoryMenu.broadcastChanges();
    }
    public static final Map<String,String> DEBUG_ACTIONS=java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(Map.ofEntries(
        Map.entry("state","Run, completions, earned Hearts, receipt and current phase"),
        Map.entry("dry_run","List exact cleared and preserved progression categories"),
        Map.entry("validate_flags","Audit scheduled reset flags without changing them"),
        Map.entry("validate_preserved","Audit inventory, Hearts, XP and site data preservation"),
        Map.entry("force_completion","DEBUG ONLY: mark this run complete and reserve its one Heart"),
        Map.entry("give_heart","DEBUG ONLY: give an extra Heart, not an earned-run reward"),
        Map.entry("eligible","DEBUG ONLY: completion plus rescued benefits on this existing family"),
        Map.entry("portrait","Place/preview the existing eligible bedroom portrait"),
        Map.entry("location","Teleport to the already recorded safe ritual location"),
        Map.entry("confirmation","Open ordinary two-stage owner consent at the actual portrait"),
        Map.entry("summon","Resume the committed ritual's actual parent navigation"),
        Map.entry("binding","Preview both real parents' thread-binding poses without death"),
        Map.entry("first_thread","Test the actual nearby first thread interaction with the bound cutter"),
        Map.entry("final_thread","Test the actual nearby final thread interaction with the bound cutter"),
        Map.entry("death_animation","Preview collapse pose only; does not record or kill anyone"),
        Map.entry("rewind_preview","Preview local rewind only; no state reset"),
        Map.entry("execute_reset","Roll forward only a committed, both-dead RESETTING transaction"),
        Map.entry("next_run","Resume only the existing RESTORING/RETURNING checkpoint"),
        Map.entry("recovery","Reload the exact persisted checkpoint; no new actors/rewards allocated"),
        Map.entry("unique_sites","Read the existing unique Palace landmark receipt"),
        Map.entry("parents","Inspect original/rescued task and availability state"),
        Map.entry("heart_retained","Inspect current inventory Hearts and lifetime earned count")
    )));
    public static String debug(ServerPlayer p,String action){
        if(!p.hasPermissions(2))return "Operator/developer only.";
        try{
            var run=StoryRunStore.get(p.server,p.getUUID());var f=family(p,run);
            switch(action){
                case "dry_run","validate_flags","validate_preserved"->{
                    for(String line:MainStoryRebirth.report())p.sendSystemMessage(Component.literal(line));
                    p.sendSystemMessage(Component.literal("Exact story keys: "+MainStoryRebirth.STORY_KEYS));return "Dry-run only; no data changed.";
                }
                case "force_completion","eligible"->{
                    if(run.active())return "A committed ritual cannot be replaced.";
                    if(action.equals("eligible")){
                        if(f==null||f.dead||f.storyMissing)return "A living canonical claimed family is required; no duplicates spawned.";
                        f.rescued=true;FamilyData.get(p.server).setDirty();
                    }
                    var d=run.details();d.putBoolean("DebugCompletion",true);run.details(d);run.finish(true);save(p,run);reward(p,run);
                    return "DEBUG completion recorded, clearly separate from real ending verification. Main story was NOT played through.";
                }
                case "give_heart"->{int slot=p.getInventory().getFreeSlot();if(slot<0)return "Free an inventory slot.";var heart=new ItemStack(RebirthItems.VOID_HEART.get());heart.getOrCreateTag().putBoolean("DebugGranted",true);p.getInventory().setItem(slot,heart);p.inventoryMenu.broadcastChanges();return "DEBUG Heart granted; lifetime earned/completed counters unchanged.";}
                case "portrait"->{if(!eligible(p,run))return "Complete the ending, have living rescued parents/home, and stop other cinematics first.";portrait(p,run);return run.details().hasUUID("Portrait")?"Portrait identity recorded.":"No safe bedroom wall/circle space; no blocks were changed.";}
                case "location"->{if(!run.details().contains("Center"))return "No recorded ritual location.";var probe=com.coraline.registry.ModEntities.MOM.get().create(p.server.overworld());var at=center(run).add(0,0,1.2);if(probe==null||!safe(p.server.overworld(),probe,at))return "Ritual arrival is obstructed.";p.teleportTo(p.server.overworld(),at.x,at.y,at.z,p.getYRot(),0);return "At recorded family bedroom.";}
                case "confirmation"->{var portrait=existing(p,run,"Portrait");if(portrait==null)return "Preview the portrait first.";interact(p,portrait);return "Ordinary nearby-owner consent attempted; no sacrifice committed by debug.";}
                case "first_thread","final_thread"->{var thread=existing(p,run,action.equals("first_thread")?"FirstLife":"FinalLife");if(thread==null)return "Actual committed life thread is not present.";interact(p,thread);return "Actual owner/reach/LOS/tool/session interaction attempted.";}
                case "summon","execute_reset","next_run"->{if(!run.active())return "No committed transaction. Use ordinary consent first.";if(action.equals("execute_reset")&&run.phase()!=RESETTING)return "Both explicit sacrifices and rewind must finish first.";scene(p,run);return "Existing checkpoint resumed; no phase was forced.";}
                case "binding","death_animation"->{if(f==null)return "No canonical family.";for(boolean mother:new boolean[]{true,false}){var parent=parent(p,f,run,mother);if(parent!=null&&parent.isAlive())parent.familyGesture(action.equals("binding")?80:82,80);}return "Pose preview only; no deaths or reset recorded.";}
                case "rewind_preview"->{var n=new CompoundTag();n.putString("Mode","scene");n.putString("Phase","REWINDING");n.putLong("Ticks",40);RebirthNetwork.send(p,n);return "Local rewind preview, no main-story mutation.";}
                case "recovery"->{if(run.active())ACTIVE.put(p.getUUID(),run);return "Persisted phase "+run.phase()+", run "+run.run()+"; reconnect will resume this same transaction.";}
                case "unique_sites"->{return "Existing global sites only: "+PalaceSiteData.get(p.server).save(new CompoundTag()).getAllKeys()+". No generation or relocation requested.";}
                case "parents"->{return f==null?"No claimed family.":"Family "+f.id+" rescued="+f.rescued+" busy="+f.busy+" tasks="+f.momTask+"/"+f.dadTask+" original IDs="+f.mom+"/"+f.dad;}
                case "heart_retained"->{int count=0;for(var item:p.getInventory().items)if(item.is(RebirthItems.VOID_HEART.get()))count+=item.getCount();return "Inventory Hearts="+count+", lifetime earned="+run.earned()+"; spent/stored Hearts need not be in inventory for eligibility.";}
                default->{return "Story Run "+run.run()+" | completed "+run.completed()+" | earned "+run.earned()+" | reward "+run.receipt()+" | pending "+run.rewardPending()+" | claimed "+run.claimed()+" | phase "+run.phase()+" | "+ERRORS.getOrDefault(p.getUUID(),"no blocker");}
            }
        }catch(IOException|RuntimeException bad){return "Blocked: "+bad.getMessage();}
    }
    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("coraline-rebirth").requires(s->s.hasPermission(2))
            .then(net.minecraft.commands.Commands.argument("action",com.mojang.brigadier.arguments.StringArgumentType.word())
            .suggests((c,b)->{DEBUG_ACTIONS.keySet().stream().sorted().forEach(b::suggest);return b.buildFuture();})
            .executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(debug(p,com.mojang.brigadier.arguments.StringArgumentType.getString(c,"action"))));return 1;})));
    }
    private static void sacrifice(ServerPlayer p,StoryRun run,ParentEntity parent,boolean mother)throws IOException{
        var d=run.details();String key=mother?"MotherDead":"FatherDead";
        if(!d.getBoolean(key)){
            if(parent==null)return; // Unloaded is not dead; never kill a substitute.
            parent.getPersistentData().putUUID("LastThreadSacrifice",run.session());
            parent.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);parent.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            parent.finishScriptedDeath(); // Genuine vanilla health-zero/death; bypasses ordinary paired-parent death sequence.
            if(parent.isAlive())return;
            run.recordDeath(run.session(),mother);save(p,run);
            p.sendSystemMessage(Component.literal(mother?"Mother's thread falls silent.":"Father's thread falls silent."));
        }
    }
    private static UUID restoredId(StoryRun run,boolean mother){return UUID.nameUUIDFromBytes((run.owner()+":"+run.session()+":"+(mother?"mother":"father")).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static boolean restoreParent(ServerPlayer p,StoryRun run,FamilyData.Family f,boolean mother){
        var at=spot(run,mother);var id=restoredId(run,mother);p.server.overworld().getChunkAt(BlockPos.containing(at));
        ParentEntity parent=null;for(var level:p.server.getAllLevels())if(level.getEntity(id) instanceof ParentEntity found)parent=found;
        if(parent==null){
            // Deterministic next-timeline IDs, saved staging chunk, bounded load grace. Never reconstitutes the sacrificed UUID.
            if(run.ticks()<60)return false;
            parent=(mother?com.coraline.registry.ModEntities.MOM:com.coraline.registry.ModEntities.DAD).get().create(p.server.overworld());if(parent==null||!safe(p.server.overworld(),parent,at))return false;
            var n=new CompoundTag();parent.saveWithoutId(n);n.putUUID("UUID",id);n.putUUID("Family",f.id);n.putInt("Skin",run.details().getInt(mother?"MomSkin":"DadSkin"));
            n.putBoolean("OtherParent",false);n.putBoolean("DeathFrozen",false);n.putFloat("Health",parent.getMaxHealth());parent.load(n);parent.setPos(at);
            if(!p.server.overworld().addFreshEntity(parent))return false;
        }
        if(parent.isOtherParent()||!f.id.equals(parent.familyId)||parent.isMom()!=mother||!parent.isAlive())return false;
        if(mother){f.mom=id;f.momPos=parent.blockPosition();f.momDim="minecraft:overworld";}else{f.dad=id;f.dadPos=parent.blockPosition();f.dadDim="minecraft:overworld";}
        return true;
    }
    private static void scene(ServerPlayer p,StoryRun run)throws IOException{
        var f=family(p,run);if(f==null)throw new IOException("Canonical family identity unavailable");
        if(p.serverLevel()!=p.server.overworld()||p.distanceToSqr(center(run))>12*12){
            p.displayClientMessage(Component.literal("The committed ritual waits in your parents' bedroom."),true);return;
        }
        for(int i=0;i<20;i++)run.tick();
        var mother=parent(p,f,run,true);var father=parent(p,f,run,false);
        var circle=existing(p,run,"Circle");if(run.phase().ordinal()<RESETTING.ordinal()){
            circle=prop(p,run,"Circle",CIRCLE,center(run).add(0,.015,0));if(circle!=null)circle.frame(run.phase().ordinal(),(int)run.ticks(),1);
            portrait(p,run);
        }
        switch(run.phase()){
            case SUMMONING->{
                if(run.ticks()==20){p.sendSystemMessage(Component.literal("Mother: Sweetheart? What's happening?"));sound(p,SoundEvents.AMETHYST_BLOCK_RESONATE,.18F,.5F);}
                if(summon(p,run,mother,true)&summon(p,run,father,false))go(p,run,BINDING);
                else if(run.ticks()%100==0)p.displayClientMessage(Component.literal("Your parents are approaching. Look away briefly if their route is blocked."),true);
            }
            case BINDING->{
                if(mother==null||father==null||!mother.isAlive()||!father.isAlive())return;
                mother.familyGesture(80,25);father.familyGesture(80,25);
                if(run.ticks()==40)p.sendSystemMessage(Component.literal("Father: Is something wrong? You can tell us."));
                if(run.ticks()==100)p.sendSystemMessage(Component.literal("Mother: We're here for you. You know that, don't you?"));
                if(run.ticks()>=160){tool(p,run);go(p,run,FIRST_THREAD);}
            }
            case FIRST_THREAD->{
                tool(p,run);var thread=prop(p,run,"FirstLife",FIRST_LIFE,spot(run,true).add(0,.7,.55));
                if(thread!=null)thread.frame(0,(int)run.ticks(),1);p.displayClientMessage(Component.literal("Sever the first thread."),true);
            }
            case FIRST_DEATH->{
                var thread=existing(p,run,"FirstLife");if(thread!=null)thread.frame(1,(int)run.ticks(),Math.max(0,1-run.ticks()/40F));
                if(mother!=null)mother.familyGesture(82,25);if(run.ticks()>=40)sacrifice(p,run,mother,true);
                if(run.details().getBoolean("MotherDead")&&run.ticks()>=100){p.sendSystemMessage(Component.literal("Father: Please. You don't have to do this."));go(p,run,FINAL_THREAD);}
            }
            case FINAL_THREAD->{tool(p,run);if(father!=null)father.familyGesture(81,25);var thread=prop(p,run,"FinalLife",FINAL_LIFE,spot(run,false).add(0,.7,.55));if(thread!=null)thread.frame(0,(int)run.ticks(),1);p.displayClientMessage(Component.literal("Sever the final thread."),true);}
            case FINAL_DEATH->{
                var thread=existing(p,run,"FinalLife");if(thread!=null)thread.frame(1,(int)run.ticks(),Math.max(0,1-run.ticks()/40F));
                if(father!=null)father.familyGesture(82,25);if(run.ticks()>=40)sacrifice(p,run,father,false);
                if(run.details().getBoolean("MotherDead")&&run.details().getBoolean("FatherDead")&&run.ticks()>=100)go(p,run,ABSORBING);
            }
            case ABSORBING->{if(run.ticks()>=100)go(p,run,REWINDING);}
            case REWINDING->{if(run.ticks()==20)sound(p,SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),.18F,.5F);if(run.ticks()>=140)go(p,run,RESETTING);}
            case RESETTING->{
                if(!run.details().getBoolean("MotherDead")||!run.details().getBoolean("FatherDead"))throw new IOException("Both death receipts required before rewind");
                MainStoryRebirth.reset(p,run);if(!MainStoryRebirth.verified(p,run))throw new IOException("Story reset checkpoint could not be verified");go(p,run,RESTORING);
            }
            case RESTORING->{
                if(!MainStoryRebirth.verified(p,run)){MainStoryRebirth.reset(p,run);if(!MainStoryRebirth.verified(p,run))break;}
                if(!restoreParent(p,run,f,true)||!restoreParent(p,run,f,false))break;
                f.dead=false;f.storyMissing=false;f.rescued=false;f.busy=true;f.availabilityChangesAt=0;f.deathPhase=0;f.deathStartedAt=0;
                f.momTask=f.dadTask=f.gardenPlots=0;f.momDelivery=f.dadDelivery=f.dialogueUntil=0;
                FamilyData.get(p.server).setDirty();p.server.saveEverything(true,false,true);
                var familyFile=StoryRunStore.read(p.server.getWorldPath(LevelResource.ROOT).resolve("data/coraline_families.dat"));boolean matched=false;
                for(var entry:familyFile.getCompound("data").getList("Families",10))if(f.save().equals(entry))matched=true;
                if(!matched)throw new IOException("Restored family checkpoint could not be verified");
                run.nextRun(run.session());save(p,run);
            }
            case RETURNING->{
                // A crash after the family record but before an entity-region write must still roll forward.
                if(!restoreParent(p,run,f,true)||!restoreParent(p,run,f,false))return;
                FamilyData.get(p.server).setDirty();
                var probe=com.coraline.registry.ModEntities.MOM.get().create(p.server.overworld());var at=center(run).add(0,0,1.2);
                if(probe==null||!safe(p.server.overworld(),probe,at))return;
                p.server.saveEverything(true,false,true); // Flush any next-timeline entity recovered during RETURNING before clearing the journal phase.
                p.stopRiding();p.setCamera(p);p.teleportTo(p.server.overworld(),at.x,at.y,at.z,p.getYRot(),0);
                for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.is(RebirthItems.CUTTER.get())&&item.hasTag()&&run.session().equals(item.getTag().getUUID("RitualSession")))p.getInventory().setItem(i,ItemStack.EMPTY);}
                for(String key:new String[]{"Portrait","Circle","FirstLife","FinalLife"}){var e=existing(p,run,key);if(e!=null)e.discard();}
                if(!checkpointInventory(p))return;var session=run.session();run.returned(session);save(p,run);
                p.sendSystemMessage(Component.literal("The threads have been rewoven."));p.sendSystemMessage(Component.literal("A new story begins."));
                p.sendSystemMessage(Component.literal("Story Run: "+run.run()+" — use the Dowsing Rod to begin the original Wybie introduction."));
                var n=new CompoundTag();n.putString("Mode","clear");RebirthNetwork.send(p,n);
            }
            default->{}
        }
        if(run.active()){
            save(p,run);var n=new CompoundTag();n.putString("Mode","scene");n.putString("Phase",run.phase().name());n.putLong("Ticks",run.ticks());n.putLong("Center",run.details().getLong("Center"));RebirthNetwork.send(p,n);
        }
    }
    /** Parent tick hook takes exclusive movement/animation ownership, before normal chores or paired death. */
    public static boolean controlParent(ParentEntity parent){
        var f=parent.family();if(f==null||f.owner==null||parent.isOtherParent())return false;var run=ACTIVE.get(f.owner);
        if(run==null)try{if(StoryRunStore.status(parent.getServer(),f.owner).active()){var saved=StoryRunStore.get(parent.getServer(),f.owner);ACTIVE.put(f.owner,saved);run=saved;}}catch(IOException unavailable){return parent.getPersistentData().hasUUID("LastThreadSession");}
        if(run==null||!f.id.equals(run.id("Family"))||!parent.getUUID().equals(run.id(parent.isMom()?"Mother":"Father")))return false;
        if(run.phase()==SUMMONING)return true;
        parent.getNavigation().stop();parent.setNoAi(true);parent.setInvulnerable(true);parent.setSilent(true);parent.setDeltaMovement(Vec3.ZERO);
        if(run.phase().ordinal()>=BINDING.ordinal()&&run.phase().ordinal()<RESETTING.ordinal()){
            parent.familyGesture(run.phase()==FINAL_THREAD&&!parent.isMom()?81:run.phase()==FIRST_DEATH&&parent.isMom()||run.phase()==FINAL_DEATH&&!parent.isMom()?82:80,25);
            var p=parent.getServer().getPlayerList().getPlayer(f.owner);if(p!=null)parent.lookAtStoryPlayer(p);
        }return true;
    }
    public static boolean protectedParent(ParentEntity parent){
        var f=parent.family();if(f==null||f.owner==null||parent.isOtherParent())return false;
        try{return StoryRunStore.ritualParent(parent.getServer(),f.owner,f.id,parent.getUUID(),parent.isMom());}
        catch(IOException bad){return parent.getPersistentData().hasUUID("LastThreadSession");}
    }
    /** A stale entity chunk cannot bring a sacrificed previous-timeline parent back to life. */
    public static boolean retireOldParent(ParentEntity parent){
        var f=parent.family();if(f==null||f.owner==null||parent.isOtherParent())return false;
        try{
            if(!StoryRunStore.tombstoned(parent.getServer(),f.owner,parent.getUUID()))return false;
            var run=StoryRunStore.get(parent.getServer(),f.owner);
            if(run.active()&&(parent.getUUID().equals(run.id("Mother"))||parent.getUUID().equals(run.id("Father")))){
                parent.getPersistentData().putUUID("LastThreadSacrifice",run.session());if(parent.isAlive())parent.finishScriptedDeath();return true;
            }
            parent.discard();return true;
        }catch(IOException bad){return parent.getPersistentData().hasUUID("LastThreadSacrifice");}
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0||com.coraline.postgame.TrueHeartBattle.recoveryPending(p))return;
        try{
            var status=StoryRunStore.status(p.server,p.getUUID());var current=OtherMotherLoop.scene(p);
            if(!status.active()&&!status.complete()&&!status.pending()&&!personalCompletion(p,current))return;
            var run=StoryRunStore.get(p.server,p.getUUID());if(run.active()){ACTIVE.put(p.getUUID(),run);if(p.isAlive())scene(p,run);return;}
            var s=OtherMotherLoop.scene(p);if(run.finish(personalCompletion(p,s)))save(p,run);reward(p,run);
            refreshHome(p,run);
            if(eligible(p,run))portrait(p,run);
        }catch(IOException|RuntimeException bad){error(p,bad.getMessage()==null?bad.getClass().getSimpleName():bad.getMessage());}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void drops(LivingDropsEvent e){if(e.getEntity().getPersistentData().hasUUID("LastThreadSacrifice"))e.setCanceled(true);}
    @SubscribeEvent public static void xp(LivingExperienceDropEvent e){if(e.getEntity().getPersistentData().hasUUID("LastThreadSacrifice"))e.setDroppedExperience(0);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){CONSENT.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){ACTIVE.clear();CONSENT.clear();ERRORS.clear();}
    private StoryRebirth(){}
}
