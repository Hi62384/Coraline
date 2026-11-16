package com.coraline.feast;

import java.util.UUID;

import com.coraline.entity.CharacterEntity;
import com.coraline.palace.PalaceWybieIdentity;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Keeps the original real Wybie through scene cleanup, save/reload and either
 * well outcome. Stored entities are dormant, invisible and non-interacting;
 * DEAD is a story tombstone, not an instruction to discard/recreate the NPC. */
@Mod.EventBusSubscriber(modid="coraline")
public final class CanonicalRealWybie {
    private static final String KEY="CanonicalRealWybie",OWNER="CanonicalRealWybieOwner";
    public static boolean recorded(VisitData.Scene s){return s!=null&&s.loop.story.contains(KEY);}
    /** The only cross-timeline exception to DEAD. Same stored entity, never a reconstruction. */
    public static void rebindTimeline(ServerPlayer p,VisitData.Scene s,com.coraline.rebirth.StoryRun run){
        if(run.run()<2&&!run.completeEndingTimeline()||!s.owner.equals(run.owner()))return;
        var ritual=run.save().getCompound("PreviousRitual");if((!ritual.getBoolean("MotherDead")||!ritual.getBoolean("FatherDead"))&&(!run.save().hasUUID("CompleteReset")||!ritual.hasUUID("CompleteReset")||!run.save().getUUID("CompleteReset").equals(ritual.getUUID("CompleteReset"))))return;
        var old=ritual.getCompound("Retained").getCompound("Wybie");
        if(!old.hasUUID("Entity")||!old.hasUUID("Door"))return;
        com.coraline.postgame.WybieRescue.prepareStoryLoan(p,old.getUUID("Entity"));
        var level=com.coraline.parents.FamilyData.level(p.server,old.getString("Dimension"));if(level==null)return;
        LoopProps.hold(level,net.minecraft.core.BlockPos.of(old.getLong("Position")));
        for(var world:p.server.getAllLevels())if(world.getEntity(old.getUUID("Entity")) instanceof CharacterEntity actor
                &&actor.characterId().equals("wybie")&&tagged(actor)&&p.getUUID().equals(actor.getPersistentData().getUUID(OWNER))){
            if(actor.getPersistentData().hasUUID("CanonicalRealWybieDoor")&&s.door.equals(actor.getPersistentData().getUUID("CanonicalRealWybieDoor")))return;
            if(com.coraline.postgame.WybieRescue.reserved(actor)&&!com.coraline.postgame.WybieRescue.beginStoryLoan(p,s,actor))return;
            actor.getPersistentData().putUUID("CanonicalRealWybieDoor",s.door);
            actor.getPersistentData().putBoolean("CanonicalWybieStoryDead",false);
            if(run.completeEndingTimeline()||!"LOST".equals(old.getString("Fate"))){actor.setHealth(actor.getMaxHealth());actor.deathTime=0;}
            var state=identity(s);if(state!=null){dormant(actor,state);write(s,state,actor);}return;
        }
    }
    /** Same-body evacuation before a Complete Ending retires its dimensions.
     * The saved plan names both source and destination before native travel.
     * An unloaded/missing/ambiguous actor pauses the reset instead of spawning. */
    public static java.util.Optional<CompoundTag> archiveForCompleteEnding(net.minecraft.server.MinecraftServer server,UUID owner,UUID reset){
        var book=CompleteEndingArchive.get(server,reset);if(!book.ready())return java.util.Optional.empty();var row=book.row(owner);var scene=VisitData.get(server).scenes.get(owner);PalaceWybieIdentity state=null;
        if(row!=null){var n=row.getCompound("Identity");state=PalaceWybieIdentity.load(owner,n.getUUID("Door"),n);}else{
            state=scene==null?null:identity(scene);
            if(state==null){var leased=com.coraline.postgame.WybieRescue.endingIdentity(server,owner);if(leased.isEmpty())return java.util.Optional.empty();var n=leased.orElseThrow();
                if(n.isEmpty())try{n=com.coraline.rebirth.StoryRunStore.get(server,owner).save().getCompound("PreviousRitual").getCompound("Retained").getCompound("Wybie");}catch(java.io.IOException ambiguous){return java.util.Optional.empty();}
                if(!n.isEmpty()){if(!n.hasUUID("Door"))return java.util.Optional.empty();state=PalaceWybieIdentity.load(owner,n.getUUID("Door"),n);}
            }
        }
        if(state==null){var witness=com.coraline.postgame.InfiniteWorldData.get(server).completionWitness(owner);return witness.hasUUID("Wybie")?java.util.Optional.empty():java.util.Optional.of(new CompoundTag());}
        if(!state.blocker().isEmpty())return java.util.Optional.empty();
        if(scene!=null&&identity(scene)!=null&&(!identity(scene).blocker().isEmpty()||!identity(scene).matches(state.entity())))return java.util.Optional.empty();
        com.coraline.postgame.WybieRescue.endingOriginal(server,owner);var source=com.coraline.parents.FamilyData.level(server,state.dimension());if(source!=null)LoopProps.hold(source,state.position());
        if(row!=null)LoopProps.hold(server.overworld(),net.minecraft.core.BlockPos.of(row.getLong("Destination")));
        CharacterEntity original=null;for(var world:server.getAllLevels())if(world.getEntity(state.entity()) instanceof CharacterEntity actor){if(original!=null)return java.util.Optional.empty();original=actor;}
        if(original==null||!original.characterId().equals("wybie")||!tagged(original)||!owner.equals(original.getPersistentData().getUUID(OWNER)))return java.util.Optional.empty();
        var palace=com.coraline.palace.PalaceSiteData.get(server).position(com.coraline.palace.PalaceSitePlan.Kind.PALACE).orElse(null);if(palace==null)return java.util.Optional.empty();LoopProps.hold(server.overworld(),palace);
        var destination=row!=null?net.minecraft.core.BlockPos.of(row.getLong("Destination")):com.coraline.tycoon.TycoonWorld.landing(server.overworld(),palace.east(4));if(destination==null||!com.coraline.tycoon.TycoonWorld.safeFeet(server.overworld(),destination))return java.util.Optional.empty();
        if(row!=null&&row.contains("Archived",Tag.TAG_COMPOUND)&&original.level()==server.overworld()&&original.blockPosition().equals(destination)&&original.isNoAi()&&original.isInvisible()&&original.isNoGravity()&&original.noPhysics&&original.getPersistentData().getBoolean("CanonicalWybieStored")&&!original.getPersistentData().contains("CoralinePostgameWybieLease"))return java.util.Optional.of(row.getCompound("Archived").copy());
        if(row==null){state.remember(original.getUUID(),original.level().dimension().location().toString(),original.blockPosition());if(!book.admit(server,owner,state.save(),destination))return java.util.Optional.empty();}
        original.getNavigation().stop();original.stopRiding();net.minecraft.world.entity.Entity moved=original;
        if(original.level()!=server.overworld())moved=original.changeDimension(server.overworld(),new net.minecraftforge.common.util.ITeleporter(){@Override public net.minecraft.world.entity.Entity placeEntity(net.minecraft.world.entity.Entity entity,net.minecraft.server.level.ServerLevel current,net.minecraft.server.level.ServerLevel target,float yaw,java.util.function.Function<Boolean,net.minecraft.world.entity.Entity> reposition){var result=reposition.apply(false);if(result!=null)result.setPos(Vec3.atBottomCenterOf(destination));return result;}});
        if(!(moved instanceof CharacterEntity actor)||!actor.getUUID().equals(state.entity()))return java.util.Optional.empty();actor.setPos(Vec3.atBottomCenterOf(destination));actor.setDeltaMovement(Vec3.ZERO);actor.fallDistance=0;
        if(!com.coraline.postgame.WybieRescue.archiveEndingLease(actor))return java.util.Optional.empty();state.remember(actor.getUUID(),"minecraft:overworld",destination);state.store(actor.getUUID());if(scene!=null)detach(scene,actor);for(String key:new String[]{"ShowActorReturn","PalaceWybieReturn","ShowTravelOwner","ShowActorOwner","ShowActorSerial"})actor.getPersistentData().remove(key);dormant(actor,state);
        if(scene!=null&&identity(scene)!=null&&identity(scene).matches(actor.getUUID()))write(scene,state,actor);if(!server.saveEverything(true,true,true))return java.util.Optional.empty();if(!book.archived(server,owner,state.save()))return java.util.Optional.empty();return java.util.Optional.of(state.save());
    }
    public static PalaceWybieIdentity identity(VisitData.Scene s){
        if(!recorded(s))return null;
        return PalaceWybieIdentity.load(s.owner,s.door,s.loop.story.contains(KEY,Tag.TAG_COMPOUND)?s.loop.story.getCompound(KEY):new CompoundTag());
    }
    public static boolean tagged(CharacterEntity actor){return actor.getPersistentData().hasUUID(OWNER);}
    public static boolean dormant(CharacterEntity actor){return tagged(actor)&&actor.getPersistentData().getBoolean("CanonicalWybieStored");}
    /** Retire an old noncanonical postgame lease into its recorded dormant
     * original. No health/fate change, replacement entity or new location. */
    public static boolean retireLegacyPostgame(net.minecraft.server.MinecraftServer server,UUID owner,UUID door,CharacterEntity actor){
        if(actor==null||!actor.characterId().equals("wybie")||!tagged(actor)||!owner.equals(actor.getPersistentData().getUUID(OWNER))||!actor.getPersistentData().hasUUID("CanonicalRealWybieDoor")||!door.equals(actor.getPersistentData().getUUID("CanonicalRealWybieDoor")))return false;
        var scene=VisitData.get(server).scenes.get(owner);PalaceWybieIdentity state;
        if(scene!=null){if(!scene.door.equals(door))return false;state=identity(scene);}
        else try{var run=com.coraline.rebirth.StoryRunStore.get(server,owner);if(run.phase()!=com.coraline.rebirth.StoryRun.Phase.IDLE)return false;state=PalaceWybieIdentity.load(owner,door,run.save().getCompound("PreviousRitual").getCompound("Retained").getCompound("Wybie"));}catch(java.io.IOException|RuntimeException unavailable){return false;}
        if(state==null||!state.blocker().isEmpty()||!state.matches(actor.getUUID())||!state.stored())return false;
        var target=com.coraline.parents.FamilyData.level(server,state.dimension());if(target==null||!target.getWorldBorder().isWithinBounds(state.position()))return false;LoopProps.hold(target,state.position());var at=Vec3.atBottomCenterOf(state.position());
        actor.getNavigation().stop();actor.stopRiding();dormant(actor,state);net.minecraft.world.entity.Entity moved=actor;
        if(actor.level()!=target)moved=actor.changeDimension(target,new net.minecraftforge.common.util.ITeleporter(){@Override public net.minecraft.world.entity.Entity placeEntity(net.minecraft.world.entity.Entity entity,net.minecraft.server.level.ServerLevel current,net.minecraft.server.level.ServerLevel destination,float yaw,java.util.function.Function<Boolean,net.minecraft.world.entity.Entity> reposition){var original=reposition.apply(false);if(original!=null)original.setPos(at);return original;}});
        if(!(moved instanceof CharacterEntity original)||!state.matches(original.getUUID()))return false;original.setPos(at);original.setDeltaMovement(Vec3.ZERO);dormant(original,state);if(scene!=null){detach(scene,original);write(scene,state,original);VisitData.get(server).setDirty();}return true;
    }
    private static VisitData.Scene scene(CharacterEntity actor){return actor.getServer()==null||!tagged(actor)?null:VisitData.get(actor.getServer()).scenes.get(actor.getPersistentData().getUUID(OWNER));}
    private static void write(VisitData.Scene s,PalaceWybieIdentity state,CharacterEntity actor){
        state.remember(actor.getUUID(),actor.level().dimension().location().toString(),actor.blockPosition());s.loop.story.put(KEY,state.save());
        VisitData.get(actor.getServer()).setDirty();
    }
    public static boolean bind(ServerPlayer p,VisitData.Scene s,CharacterEntity actor){
        if(com.coraline.postgame.WybieRescue.reserved(actor))return false;
        if(!s.owner.equals(p.getUUID())||!actor.characterId().equals("wybie"))return false;
        var tag=actor.getPersistentData();if(tag.hasUUID(OWNER)&&!s.owner.equals(tag.getUUID(OWNER)))return false;
        var state=identity(s);
        if(state==null)state=PalaceWybieIdentity.create(s.owner,s.door,actor.getUUID(),actor.level().dimension().location().toString(),actor.blockPosition());
        if(!state.activate(actor.getUUID()))return false;
        tag.putUUID(OWNER,s.owner);tag.putUUID("CanonicalRealWybieDoor",s.door);tag.putBoolean("CanonicalWybieStored",false);
        actor.setPersistenceRequired();actor.setInvisible(false);actor.setSilent(false);write(s,state,actor);return true;
    }
    /** Lookup only: the saved chunk may be unloaded. Never instantiate a UUID. */
    public static CharacterEntity find(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return null;return find(p.server,s);
    }
    /** Native identity lookup for world-owned postgame work; no simulated owner. */
    public static CharacterEntity find(net.minecraft.server.MinecraftServer server,VisitData.Scene s){
        var state=identity(s);if(state==null||!state.blocker().isEmpty())return null;
        var level=com.coraline.parents.FamilyData.level(server,state.dimension());if(level==null)return null;
        // Request only the recorded original's chunk, not a scan of all chunks.
        LoopProps.hold(level,state.position());
        for(var world:server.getAllLevels())if(world.getEntity(state.entity()) instanceof CharacterEntity actor
                &&actor.characterId().equals("wybie")&&tagged(actor)&&s.owner.equals(actor.getPersistentData().getUUID(OWNER))
                &&actor.getPersistentData().hasUUID("CanonicalRealWybieDoor")&&s.door.equals(actor.getPersistentData().getUUID("CanonicalRealWybieDoor")))return actor;
        return null;
    }
    /** Lookup between ordinary story scenes. Retained metadata is authority to
     * locate the same stored body only; it is never an entity spawn recipe. */
    public record RetainedOriginal(UUID door,PalaceWybieIdentity identity,CharacterEntity actor){}
    public static RetainedOriginal betweenScenes(ServerPlayer player){return betweenScenes(player.server,player.getUUID());}
    public static RetainedOriginal betweenScenes(net.minecraft.server.MinecraftServer server,UUID owner){
        if(VisitData.get(server).scenes.containsKey(owner))return null;
        try{var run=com.coraline.rebirth.StoryRunStore.get(server,owner);
            if(run.phase()!=com.coraline.rebirth.StoryRun.Phase.IDLE)return null;
            var saved=run.save().getCompound("PreviousRitual").getCompound("Retained").getCompound("Wybie");
            if(!saved.hasUUID("Door"))return null;var door=saved.getUUID("Door");
            var state=PalaceWybieIdentity.load(owner,door,saved);
            if(!state.blocker().isEmpty()||!state.stored()||state.fate()==PalaceWybieIdentity.Fate.DEAD)return null;
            var source=com.coraline.parents.FamilyData.level(server,state.dimension());if(source==null)return null;LoopProps.hold(source,state.position());
            CharacterEntity found=null;for(var world:server.getAllLevels())if(world.getEntity(state.entity()) instanceof CharacterEntity actor){if(found!=null)return null;found=actor;}
            if(found==null)return new RetainedOriginal(door,state,null);
            if(!found.isAlive()||!found.characterId().equals("wybie")||!dormant(found)||found.getPersistentData().getBoolean("CanonicalWybieStoryDead")||!owner.equals(found.getPersistentData().getUUID(OWNER))||!found.getPersistentData().hasUUID("CanonicalRealWybieDoor")||!door.equals(found.getPersistentData().getUUID("CanonicalRealWybieDoor")))return null;
            return new RetainedOriginal(door,state,found);
        }catch(java.io.IOException|RuntimeException ambiguous){return null;}
    }
    public static void remember(CharacterEntity actor){
        if(com.coraline.postgame.WybieRescue.remember(actor))return;
        var s=scene(actor);if(s==null){if(dormant(actor)){actor.getNavigation().stop();actor.stopRiding();actor.setTarget(null);actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;actor.setDeltaMovement(Vec3.ZERO);actor.setInvulnerable(true);actor.setInvisible(true);actor.setSilent(true);}return;}var state=identity(s);if(state==null||!state.matches(actor.getUUID()))return;
        if(state.stored())dormant(actor,state);
        if(actor.tickCount%20==0)write(s,state,actor);
    }
    private static void dormant(CharacterEntity actor,PalaceWybieIdentity state){
        actor.getNavigation().stop();actor.stopRiding();actor.setTarget(null);actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;
        actor.setDeltaMovement(Vec3.ZERO);actor.setInvulnerable(true);actor.setInvisible(true);actor.setSilent(true);actor.gameGesture(0,0);
        actor.getPersistentData().putBoolean("CanonicalWybieStored",true);actor.getPersistentData().putBoolean("CanonicalWybieStoryDead",state.fate()==PalaceWybieIdentity.Fate.DEAD);
        actor.setPersistenceRequired();
    }
    private static void detach(VisitData.Scene s,CharacterEntity actor){
        var id=actor.getUUID();var tag=actor.getPersistentData();
        for(String key:new String[]{"ShowActorReturn","PalaceWybieReturn"}){s.loop.event.getCompound("ShowTravelGuests").remove(key+":"+id);tag.remove(key);}
        tag.remove("ShowTravelOwner");
        var lease=s.loop.event.getCompound("ShowActors").getCompound("wybie");if(lease.hasUUID("Entity")&&id.equals(lease.getUUID("Entity")))s.loop.event.getCompound("ShowActors").remove("wybie");
        tag.remove("ShowActorOwner");tag.remove("ShowActorSerial");if(id.equals(s.wybieActor))s.wybieActor=null;
    }
    /** Call after an actual offscreen departure. Bicycle cleanup is independent. */
    public static boolean park(ServerPlayer p,VisitData.Scene s,CharacterEntity actor){
        return park(p,s,actor,false);
    }
    /** Explicit operator skip may retire a visible actor, but never deletes or clones him. */
    public static boolean debugPark(ServerPlayer p,VisitData.Scene s,CharacterEntity actor){return p.hasPermissions(2)&&park(p,s,actor,true);}
    /** The completed spoken goodbye cannot stall forever on player terrain or
     * a viewer following him. Retire the SAME actor, never kill/delete/clone. */
    static boolean finishBlockedDeparture(ServerPlayer p,VisitData.Scene s,CharacterEntity actor){var n=FirstEscapeStory.data(s);return FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.BICYCLE_DEPARTURE&&n.getInt("BicycleDepartureTicks")>=60&&park(p,s,actor,true);}
    private static boolean park(ServerPlayer p,VisitData.Scene s,CharacterEntity actor,boolean debug){
        if(com.coraline.postgame.WybieRescue.reserved(actor))return false;
        if(!s.owner.equals(p.getUUID())||!debug&&ShowActors.visibleToAnyone(actor,p))return false;
        var state=identity(s);if(state==null&&!bind(p,s,actor))return false;state=identity(s);
        if(state==null||!state.store(actor.getUUID()))return false;
        detach(s,actor);
        dormant(actor,state);write(s,state,actor);snapshot(s,actor);OtherMotherLoop.changed(p);return true;
    }
    private static void snapshot(VisitData.Scene s,CharacterEntity actor){
        // Evidence/backup only. There is deliberately no load/spawn from this.
        var n=new CompoundTag();if(actor.save(n))s.loop.story.put("CanonicalRealWybieSnapshot",n);
    }
    /** Archive the same physical entity and permanently record this outcome.
     * Cancelling vanilla death prevents its 20-tick corpse-removal path. */
    public static boolean archiveDeath(CharacterEntity actor){
        if(com.coraline.postgame.WybieRescue.reserved(actor))return false;
        com.coraline.postgame.WybieRescue.loanDeath(actor);
        var s=scene(actor);var state=s==null?null:identity(s);if(state==null||!state.recordDeath(actor.getUUID()))return false;
        actor.setHealth(Math.max(1,actor.getHealth()));actor.deathTime=0;detach(s,actor);dormant(actor,state);write(s,state,actor);snapshot(s,actor);return true;
    }
    /** Admit only the actual living original at the end of this owner's well
     * fall. The same entity is retained for later rescue; a DEAD tombstone is
     * never silently reclassified by a missing actor or a caller's cue name. */
    static boolean archiveWellFall(ServerPlayer p,VisitData.Scene s,com.coraline.palace.PalaceWellProgress progress,CharacterEntity actor){
        if(!p.getUUID().equals(s.owner)||!actor.isAlive()||actor.level()!=p.server.overworld()||!progress.blocker().isEmpty()
            ||progress.phase()!=com.coraline.palace.PalaceWellProgress.Phase.BOSS||progress.rescueTicks()<=0
            ||progress.wybieLife()!=com.coraline.palace.PalaceWellProgress.WybieLife.LOST_AT_WELL
            ||progress.originalWybie().filter(actor.getUUID()::equals).isEmpty()||find(p,s)!=actor)return false;
        var well=com.coraline.palace.PalaceSiteData.get(p.server).position(com.coraline.palace.PalaceSitePlan.Kind.WELL).orElse(null);if(well==null)return false;
        var bottom=Vec3.atBottomCenterOf(well).add(0,-25,0);if(actor.position().distanceToSqr(bottom)>2.25)return false;
        var state=identity(s);if(state==null||!state.recordWellFall(actor.getUUID(),progress.session()))return false;
        detach(s,actor);dormant(actor,state);write(s,state,actor);snapshot(s,actor);OtherMotherLoop.changed(p);return true;
    }
    /** Safety net for older optional-visit/general cast cleanup. Chunk unload
     * and dimension transfer are NOT deletion and must keep vanilla semantics. */
    public static boolean preserveCleanup(CharacterEntity actor,boolean killed){
        if(com.coraline.postgame.WybieRescue.reserved(actor))return true;
        if(actor.level().isClientSide||!tagged(actor))return false;
        if(com.coraline.postgame.WybieRescue.loaned(actor))killed=false;
        var s=scene(actor);var state=s==null?null:identity(s);
        // Generic scene retirement cannot turn a stored rescue subject into a
        // death. Actual LivingDeathEvent still records genuine actor deaths.
        if(state!=null&&state.fate()==PalaceWybieIdentity.Fate.LOST&&state.stored()&&state.matches(actor.getUUID())){detach(s,actor);dormant(actor,state);write(s,state,actor);snapshot(s,actor);return true;}
        if(killed&&archiveDeath(actor))return true;
        if(state!=null&&state.store(actor.getUUID())){detach(s,actor);dormant(actor,state);write(s,state,actor);snapshot(s,actor);}
        // A malformed/foreign record is not authority to erase the original.
        // Leave it persistent and untouched for compatibility review.
        actor.setPersistenceRequired();return true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(event.getEntity() instanceof CharacterEntity leased&&com.coraline.postgame.WybieRescue.reserved(leased)){com.coraline.postgame.WybieRescue.archiveDeath(leased);event.setCanceled(true);return;}
        if(event.getEntity() instanceof CharacterEntity actor&&tagged(actor)&&archiveDeath(actor))event.setCanceled(true);
    }
    private CanonicalRealWybie(){}
}
