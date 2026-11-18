package com.coraline.postgame;

import java.util.*;
import com.coraline.entity.CharacterEntity;
import com.coraline.feast.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Native original-actor transfer and physical escort. Missing actors wait for
 * their recorded chunk; no NBT backup is ever instantiated as a replacement. */
@Mod.EventBusSubscriber(modid="coraline")
public final class WybieRescue {
    private static final String LEASE="CoralinePostgameWybieLease",CORALINE="CoralinePostgameGuide";
    private static Optional<UUID> generation(MinecraftServer server){return InfiniteWorldData.get(server).generation();}
    private static WybieRescueData data(MinecraftServer server){return generation(server).map(id->WybieRescueData.get(server,id)).orElse(null);}
    private static CompoundTag lease(CharacterEntity actor){var n=actor.getPersistentData();return n.contains(LEASE,Tag.TAG_COMPOUND)?n.getCompound(LEASE):new CompoundTag();}
    public static boolean guide(CharacterEntity actor){return actor.getPersistentData().hasUUID(CORALINE);}
    public static boolean reserved(CharacterEntity actor){if(!actor.getPersistentData().contains(LEASE))return false;var old=lease(actor);if(actor.getServer()!=null&&old.hasUUID("Generation")&&old.hasUUID("Owner")){var archived=WybieRescueData.get(actor.getServer(),old.getUUID("Generation")).row(old.getUUID("Owner"));if(archived!=null&&archived.getString("Stage").equals("ARCHIVED")&&archived.getUUID("Actor").equals(actor.getUUID())&&CanonicalRealWybie.dormant(actor))return false;}var n=record(actor);return n==null||!n.getString("Stage").equals("STORY_LOAN")||!n.hasUUID("LoanDoor")||!actor.getPersistentData().hasUUID("CanonicalRealWybieDoor")||!n.getUUID("LoanDoor").equals(actor.getPersistentData().getUUID("CanonicalRealWybieDoor"));}
    public static boolean loaned(CharacterEntity actor){var n=record(actor);return n!=null&&n.getString("Stage").equals("STORY_LOAN");}
    private static CompoundTag record(CharacterEntity actor){var tag=lease(actor);if(!tag.contains("Version",Tag.TAG_INT)||tag.getInt("Version")!=1||!tag.hasUUID("Generation")||!tag.hasUUID("Owner")||actor.getServer()==null||!InfiniteWorldData.get(actor.getServer()).matchesGeneration(tag.getUUID("Generation")))return null;var d=WybieRescueData.get(actor.getServer(),tag.getUUID("Generation"));var n=d.row(tag.getUUID("Owner"));return n!=null&&n==d.shared(actor.getServer())&&n.hasUUID("Actor")&&n.getUUID("Actor").equals(actor.getUUID())&&(n.getString("Stage").equals("MOVING")||n.getString("Stage").equals("STORY_LOAN")||n.getString("Dimension").equals(actor.level().dimension().location().toString()))?n:null;}
    public static boolean remember(CharacterEntity actor){if(!actor.getPersistentData().contains(LEASE))return false;var n=record(actor);if(n==null){actor.getNavigation().stop();return true;}if(actor.tickCount%20==0&&!n.getString("Stage").equals("MOVING")){n.putString("Dimension",actor.level().dimension().location().toString());n.putLong("Position",actor.blockPosition().asLong());data(actor.getServer()).setDirty();}return !n.getString("Stage").equals("STORY_LOAN");}
    public static void archiveDeath(CharacterEntity actor){var n=record(actor);if(n!=null){n.putString("Stage","FAILED");data(actor.getServer()).setDirty();}actor.getNavigation().stop();actor.setHealth(1);actor.deathTime=0;actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;actor.setInvisible(true);actor.setInvulnerable(true);actor.getPersistentData().putBoolean("CanonicalWybieStored",true);actor.getPersistentData().putBoolean("CanonicalWybieStoryDead",true);}
    /** Complete-ending evacuation never reconstructs an original. */
    public static CharacterEntity endingOriginal(MinecraftServer server,UUID owner){var d=data(server);var n=d==null?null:d.row(owner);if(n==null)return null;requestOriginal(server,n);return find(server,n);}
    public static java.util.Optional<CompoundTag> endingIdentity(MinecraftServer server,UUID owner){
        var d=data(server);var n=d==null?null:d.row(owner);if(n==null)return java.util.Optional.of(new CompoundTag());requestOriginal(server,n);var actor=find(server,n);if(actor==null||!actor.characterId().equals("wybie")||!actor.getPersistentData().hasUUID("CanonicalRealWybieOwner")||!actor.getPersistentData().getUUID("CanonicalRealWybieOwner").equals(owner))return java.util.Optional.empty();
        var identity=com.coraline.palace.PalaceWybieIdentity.create(owner,n.getUUID("Door"),actor.getUUID(),actor.level().dimension().location().toString(),actor.blockPosition());
        if(actor.getPersistentData().getBoolean("CanonicalWybieStoryDead"))identity.recordDeath(actor.getUUID());else if((n.getString("Stage").equals("SEARCH")||n.getString("Stage").startsWith("TRAIL_")||n.getString("Stage").equals("CAGE"))&&n.hasUUID("WellSession"))identity.recordWellFall(actor.getUUID(),n.getUUID("WellSession"));identity.store(actor.getUUID());return java.util.Optional.of(identity.save());
    }
    public static boolean archiveEndingLease(CharacterEntity actor){
        if(!actor.getPersistentData().contains(LEASE))return true;var tag=lease(actor);if(!tag.hasUUID("Generation")||!tag.hasUUID("Owner")||actor.getServer()==null)return false;
        var d=WybieRescueData.get(actor.getServer(),tag.getUUID("Generation"));var n=d.row(tag.getUUID("Owner"));if(n==null||!n.getUUID("Actor").equals(actor.getUUID()))return false;
        n.putString("Dimension",actor.level().dimension().location().toString());n.putLong("Position",actor.blockPosition().asLong());n.putString("Stage","ARCHIVED");if(!d.checkpoint(actor.getServer()))return false;actor.getPersistentData().remove(LEASE);return true;
    }
    static void retireGuides(MinecraftServer server,UUID generation){var d=WybieRescueData.get(server,generation);var guide=d.coraline();if(guide!=null&&server.overworld().getEntity(guide.getUUID("Actor")) instanceof CharacterEntity actor&&actor.getPersistentData().hasUUID(CORALINE)&&actor.getPersistentData().getUUID(CORALINE).equals(generation))actor.discard();}
    @SubscribeEvent public static void retiredGuide(net.minecraftforge.event.entity.EntityJoinLevelEvent event){if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof CharacterEntity actor)||!actor.getPersistentData().hasUUID(CORALINE))return;var generation=actor.getPersistentData().getUUID(CORALINE);if(!InfiniteWorldData.get(level.getServer()).state().retired(generation))return;var guide=WybieRescueData.get(level.getServer(),generation).coraline();if(guide!=null&&guide.getUUID("Actor").equals(actor.getUUID())){event.setCanceled(true);actor.discard();}}
    public static boolean rescued(MinecraftServer server,UUID owner,UUID actor){var d=data(server);var n=d==null?null:d.row(owner);return n!=null&&n.getUUID("Actor").equals(actor)&&n.hasUUID("Completion")&&(n.getString("Stage").equals("COMPLETE")||n.getString("Stage").equals("STORY_LOAN")||n.getString("Stage").equals("MOVING")&&n.getString("AfterMove").equals("COMPLETE"));}
    public static void prepareStoryLoan(ServerPlayer p,UUID actor){var d=data(p.server);var n=d==null?null:d.row(p.getUUID());if(n!=null&&n.getUUID("Actor").equals(actor)&&(n.getString("Stage").equals("COMPLETE")||n.getString("Stage").equals("STORY_LOAN")))requestOriginal(p.server,n);}
    public static boolean beginStoryLoan(ServerPlayer p,VisitData.Scene scene,CharacterEntity actor){var d=data(p.server);var n=d==null?null:d.row(p.getUUID());if(n==null||!scene.owner.equals(p.getUUID())||!n.getUUID("Actor").equals(actor.getUUID())||!actor.isAlive()||find(p.server,n)!=actor)return false;if(n.getString("Stage").equals("STORY_LOAN"))return n.hasUUID("LoanDoor")&&n.getUUID("LoanDoor").equals(scene.door);if(!n.getString("Stage").equals("COMPLETE")||!n.hasUUID("Completion"))return false;n.putString("Stage","STORY_LOAN");n.putUUID("LoanDoor",scene.door);n.remove("LoanFinished");return d.checkpoint(p.server);}
    public static void loanDeath(CharacterEntity actor){var n=record(actor);if(n!=null&&n.getString("Stage").equals("STORY_LOAN")){n.putString("Stage","FAILED");data(actor.getServer()).setDirty();}}
    static void finishStoryLoan(MinecraftServer server,InfiniteWorldState.Witness witness){
        var d=data(server);var n=d==null?null:d.row(witness.owner());if(n==null||!n.getString("Stage").equals("STORY_LOAN")||!n.hasUUID("LoanDoor")||!n.getUUID("LoanDoor").equals(witness.door())||witness.wybie()==null||!n.getUUID("Actor").equals(witness.wybie()))return;
        if(witness.wybieLife()==com.coraline.palace.PalaceWellProgress.WybieLife.LOST_AT_WELL){
            var scene=VisitData.get(server).scenes.get(witness.owner());var original=scene==null?null:CanonicalRealWybie.identity(scene);
            if(scene==null||!scene.door.equals(witness.door())||original==null||!original.blocker().isEmpty()||!original.matches(witness.wybie())||original.fate()!=com.coraline.palace.PalaceWybieIdentity.Fate.LOST||!original.stored()||original.lostWellSession().filter(witness.wellSession()::equals).isEmpty())return;
            // Preserve the original reward receipt, but require a fresh native
            // trail, cage and escort after an actual new fall in a replay.
            if(n.getString("Origin").equals("SURVIVED")){n.remove("Origin");n.remove("Completion");n.remove("RewardIssued");n.remove("RewardRecipient");}
            n.putString("Stage","SEARCH");n.putUUID("Door",witness.door());n.putUUID("WellSession",witness.wellSession());n.putString("Dimension",original.dimension());n.putLong("Position",original.position().asLong());
            for(String key:List.of("LoanDoor","LoanFinished","Site","Seed","AnchorX","AnchorZ","Generation","PreviousDimension","PreviousPosition"))n.remove(key);
            d.checkpoint(server);return;
        }
        if(witness.wybieLife()!=com.coraline.palace.PalaceWellProgress.WybieLife.ALIVE)return;
        n.putBoolean("LoanFinished",true);if(!d.checkpoint(server))return;var p=server.getPlayerList().getPlayer(witness.owner());if(p!=null)returnLoan(p,d,n);
    }
    private static void returnLoan(ServerPlayer p,WybieRescueData d,CompoundTag n){var actor=find(p.server,n);var guide=d.coraline();if(actor==null||guide==null||!actor.isAlive()||actor.getPersistentData().getBoolean("CanonicalWybieStoryDead"))return;if(!actor.getPersistentData().getBoolean("CanonicalWybieStored")){var scene=VisitData.get(p.server).scenes.get(p.getUUID());if(scene==null||!scene.door.equals(n.getUUID("LoanDoor"))||!CanonicalRealWybie.park(p,scene,actor))return;}var home=BlockPos.of(guide.getLong("Home"));var safe=com.coraline.tycoon.TycoonWorld.landing(p.server.overworld(),home.east(2));if(safe!=null)move(p,d,n,actor,p.server.overworld(),safe,"COMPLETE");}
    private static void say(ServerPlayer p,String key){p.sendSystemMessage(Component.translatable("message.coraline.rescue_"+key));}
    private static CharacterEntity find(MinecraftServer server,CompoundTag n){CharacterEntity found=null;for(var world:server.getAllLevels())if(world.getEntity(n.getUUID("Actor")) instanceof CharacterEntity actor){if(found!=null)return null;found=actor;}return found;}
    private static void requestOriginal(MinecraftServer server,CompoundTag n){if(n.contains("PreviousDimension",Tag.TAG_STRING)&&n.contains("PreviousPosition",Tag.TAG_LONG)){var previous=com.coraline.parents.FamilyData.level(server,n.getString("PreviousDimension"));if(previous!=null)LoopProps.hold(previous,BlockPos.of(n.getLong("PreviousPosition")));}var level=com.coraline.parents.FamilyData.level(server,n.getString("Dimension"));if(level!=null)LoopProps.hold(level,BlockPos.of(n.getLong("Position")));if(n.getString("Stage").equals("MOVING")){var target=com.coraline.parents.FamilyData.level(server,n.getString("TargetDimension"));if(target!=null)LoopProps.hold(target,BlockPos.of(n.getLong("TargetPosition")));}}
    private static boolean eligible(ServerPlayer p){return !TrueHeartBattle.recoveryPending(p)&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&InfiniteWorldData.get(p.server).accessible();}
    private static InfiniteStructurePlan.Plan plan(CompoundTag n){return n!=null&&n.hasUUID("Site")?FalsePalaceLayout.read(n).filter(p->p.site().equals(n.getUUID("Site"))).orElse(null):null;}
    private static BlockPos cage(InfiniteStructurePlan.Plan p){return new BlockPos(p.x(),p.y()+9,p.z()+6);}
    private static void attach(CharacterEntity actor,UUID owner,UUID generation){var tag=new CompoundTag();tag.putInt("Version",1);tag.putUUID("Owner",owner);tag.putUUID("Generation",generation);actor.getPersistentData().put(LEASE,tag);}
    /** A witnessed survivor joins the Palace as the same native actor. This is
     * arrival, not a rescue: it creates no rescue payout or containment credit. */
    private static void welcomeSurvivor(ServerPlayer p,WybieRescueData d){
        if(!eligible(p)||d.shared(p.server)!=null||d.coraline()==null||p.level()!=p.server.overworld())return;
        var home=BlockPos.of(d.coraline().getLong("Home"));if(p.distanceToSqr(home.getCenter())>128*128)return;
        var witness=InfiniteWorldData.get(p.server).canonicalCompletion();if(!witness.hasUUID("Owner"))return;var canonicalOwner=witness.getUUID("Owner");
        if(!witness.getString("WybieLife").equals("ALIVE")||!witness.hasUUID("Wybie")||!witness.hasUUID("WellSession"))return;
        var scene=VisitData.get(p.server).scenes.get(canonicalOwner);var retained=scene==null?CanonicalRealWybie.betweenScenes(p.server,canonicalOwner):null;
        var identity=scene==null?(retained==null?null:retained.identity()):CanonicalRealWybie.identity(scene);var door=scene==null?(retained==null?null:retained.door()):scene.door;
        if(!witness.hasUUID("Door")||!witness.getUUID("Door").equals(door)||identity==null||!identity.blocker().isEmpty()||identity.fate()!=com.coraline.palace.PalaceWybieIdentity.Fate.ALIVE)return;
        var original=scene==null?(retained==null?null:retained.actor()):CanonicalRealWybie.find(p.server,scene);if(original==null||!original.isAlive()||!original.getUUID().equals(witness.getUUID("Wybie"))||reserved(original)||original.getPersistentData().getBoolean("CanonicalWybieStoryDead"))return;
        // Ordinary offscreen departure rules apply before the postgame lease.
        if(!identity.stored()){var actualOwner=p.server.getPlayerList().getPlayer(canonicalOwner);if(scene==null||actualOwner==null||!CanonicalRealWybie.park(actualOwner,scene,original))return;}
        var safe=com.coraline.tycoon.TycoonWorld.landing(p.server.overworld(),home.east(2));if(safe==null)return;
        var row=new CompoundTag();row.putUUID("Actor",original.getUUID());row.putUUID("Door",door);row.putUUID("WellSession",witness.getUUID("WellSession"));row.putString("Origin","SURVIVED");row.putUUID("Completion",UUID.randomUUID());row.putBoolean("RewardIssued",true);row.putString("Stage","MOVING");row.putString("Dimension",original.level().dimension().location().toString());row.putLong("Position",original.blockPosition().asLong());row.putString("PreviousDimension",original.level().dimension().location().toString());row.putLong("PreviousPosition",original.blockPosition().asLong());row.putString("TargetDimension","minecraft:overworld");row.putLong("TargetPosition",safe.asLong());row.putString("AfterMove","COMPLETE");
        row.putUUID("Driver",p.getUUID());if(!d.claim(p.server,canonicalOwner,row))return;attach(original,canonicalOwner,generation(p.server).orElseThrow());move(p,d,d.shared(p.server),original,p.server.overworld(),safe,"COMPLETE");
    }
    private static void start(ServerPlayer p){
        var d=data(p.server);if(d==null||!d.problem().isEmpty())return;var existing=d.shared(p.server);if(existing!=null){say(p,existing.getString("Stage").equals("COMPLETE")?"complete":"status_"+existing.getString("Stage").toLowerCase(Locale.ROOT));return;}
        var witness=InfiniteWorldData.get(p.server).canonicalCompletion();if(!witness.hasUUID("Owner")||!witness.hasUUID("Wybie"))return;var canonicalOwner=witness.getUUID("Owner");
        var scene=VisitData.get(p.server).scenes.get(canonicalOwner);var retained=scene==null?CanonicalRealWybie.betweenScenes(p.server,canonicalOwner):null;
        var identity=scene==null?(retained==null?null:retained.identity()):CanonicalRealWybie.identity(scene);var door=scene==null?(retained==null?null:retained.door()):scene.door;
        if(identity==null||!identity.blocker().isEmpty()||identity.fate()!=com.coraline.palace.PalaceWybieIdentity.Fate.LOST||!identity.stored()||identity.lostWellSession().isEmpty()){say(p,"no_living_witness");return;}
        var original=scene==null?retained.actor():CanonicalRealWybie.find(p.server,scene);if(original==null){say(p,"loading_original");return;}if(!original.isAlive()||reserved(original)){say(p,"original_unavailable");return;}
        var row=new CompoundTag();row.putUUID("Actor",original.getUUID());row.putUUID("Door",door);row.putUUID("WellSession",identity.lostWellSession().orElseThrow());row.putString("Stage","SEARCH");row.putString("Dimension",original.level().dimension().location().toString());row.putLong("Position",original.blockPosition().asLong());
        row.putUUID("Driver",p.getUUID());if(!d.claim(p.server,canonicalOwner,row))return;attach(original,canonicalOwner,generation(p.server).orElseThrow());say(p,"begins");
    }
    /** Only actual discovery of native structure pieces assigns a site. */
    static void discover(ServerPlayer p,InfiniteStructurePlan.Plan plan){if(!eligible(p)||plan.family()!=InfiniteStructurePlan.Family.FALSE_PINK_PALACE)return;var d=data(p.server);var n=d==null?null:d.shared(p.server);if(n==null||!n.getString("Stage").equals("SEARCH"))return;for(int i=0;i<3;i++){var at=WellScarLayout.clue(plan,i);if(!p.level().hasChunkAt(at)||!(p.level().getBlockEntity(at) instanceof WellScarClueEntity clue)||!clue.matches(plan,i))return;}n.putUUID("Site",plan.site());n.putLong("Seed",plan.seed());n.putUUID("Generation",plan.generation());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putString("Stage","TRAIL_ONE");if(d.checkpoint(p.server))say(p,"scar");}
    static void clue(ServerPlayer p,InfiniteStructurePlan.Plan plan,int clue){if(!eligible(p))return;var d=data(p.server);var n=d==null?null:d.shared(p.server);if(n==null||!n.hasUUID("Site")||!plan.site().equals(n.getUUID("Site")))return;if(clue==3){say(p,"false_trail");return;}String[] stages={"TRAIL_ONE","TRAIL_TWO","TRAIL_THREE","CAGE"};if(!n.getString("Stage").equals(stages[clue])){say(p,"trail_order");return;}n.putString("Stage",stages[clue+1]);if(d.checkpoint(p.server))say(p,"clue_"+clue);}
    private static boolean move(ServerPlayer p,WybieRescueData d,CompoundTag n,CharacterEntity actor,ServerLevel target,BlockPos feet,String after){
        if(actor==null||!actor.isAlive()||!actor.getUUID().equals(n.getUUID("Actor"))||!target.hasChunkAt(feet)||!target.getWorldBorder().isWithinBounds(feet)||!target.noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(feet)))||!target.getFluidState(feet).isEmpty())return false;
        if(!n.getString("Stage").equals("MOVING")){n.putString("PreviousDimension",actor.level().dimension().location().toString());n.putLong("PreviousPosition",actor.blockPosition().asLong());n.putString("Stage","MOVING");n.putString("TargetDimension",target.dimension().location().toString());n.putLong("TargetPosition",feet.asLong());n.putString("AfterMove",after);if(!d.checkpoint(p.server))return false;}
        actor.getNavigation().stop();actor.stopRiding();Entity moved=actor;
        if(actor.level()!=target)moved=actor.changeDimension(target,new net.minecraftforge.common.util.ITeleporter(){@Override public Entity placeEntity(Entity entity,ServerLevel current,ServerLevel destination,float yaw,java.util.function.Function<Boolean,Entity> reposition){var result=reposition.apply(false);if(result!=null)result.setPos(Vec3.atBottomCenterOf(feet));return result;}});
        if(!(moved instanceof CharacterEntity original)||!original.getUUID().equals(n.getUUID("Actor")))return false;
        original.setPos(Vec3.atBottomCenterOf(feet));original.setDeltaMovement(Vec3.ZERO);original.fallDistance=0;attach(original,d.sharedOwner(p.server),generation(p.server).orElseThrow());n.putUUID("Driver",p.getUUID());n.putString("Dimension",target.dimension().location().toString());n.putLong("Position",feet.asLong());n.putString("Stage",after);n.remove("TargetDimension");n.remove("TargetPosition");n.remove("AfterMove");return d.checkpoint(p.server);
    }
    /** Called with the actual safe portal destination before player travel. */
    static void portalReturn(ServerPlayer p,ServerLevel destination,BlockPos feet){var d=data(p.server);var n=d==null?null:d.shared(p.server);if(n==null||!n.getString("Stage").equals("ESCORT"))return;var actor=find(p.server,n);if(actor==null||actor.level()!=p.level()||actor.distanceToSqr(p)>16)return;var safe=com.coraline.tycoon.TycoonWorld.landing(destination,feet.east(2));if(safe!=null)move(p,d,n,actor,destination,safe,"RETURNING");}
    private static void tick(ServerPlayer p,WybieRescueData d,CompoundTag n){
        if(!eligible(p)||!d.problem().isEmpty())return;var stage=n.getString("Stage");if(stage.equals("STORY_LOAN")){if(n.getBoolean("LoanFinished")){requestOriginal(p.server,n);returnLoan(p,d,n);}return;}if(stage.equals("ARCHIVED")||stage.equals("FAILED")||stage.equals("SEARCH")||stage.startsWith("TRAIL_"))return;if(stage.equals("COMPLETE")&&(p.level()!=p.server.overworld()||p.distanceToSqr(BlockPos.of(n.getLong("Position")).getCenter())>128*128))return;requestOriginal(p.server,n);var actor=find(p.server,n);if(actor==null)return;if(!reserved(actor))attach(actor,d.sharedOwner(p.server),generation(p.server).orElseThrow());if(record(actor)!=n)return;
        if(stage.equals("MOVING")){var target=com.coraline.parents.FamilyData.level(p.server,n.getString("TargetDimension"));if(target!=null)move(p,d,n,actor,target,BlockPos.of(n.getLong("TargetPosition")),n.getString("AfterMove"));return;}
        var plan=plan(n);if(stage.equals("CAGE")&&plan!=null&&p.serverLevel().getChunkSource().getGenerator() instanceof InfiniteChunkGenerator gen&&gen.structureGeneration().id().equals(plan.generation())&&p.distanceToSqr(cage(plan).getCenter())<16*16&&actor.level()!=p.level()){move(p,d,n,actor,p.serverLevel(),cage(plan),"CAGE");return;}
        boolean captive=stage.equals("CAGE")&&InfiniteRealm.isRealm(actor.level()),escort=stage.equals("ESCORT")||stage.equals("RETURNING"),complete=stage.equals("COMPLETE");
        if(!captive&&!escort&&!complete)return;if(stage.equals("RETURNING")&&actor.level()!=p.server.overworld())return;actor.getPersistentData().putBoolean("CanonicalWybieStored",false);actor.setInvisible(false);actor.setSilent(false);actor.setInvulnerable(true);actor.noPhysics=false;actor.setNoGravity(false);actor.setNoAi(captive);actor.partyDoorDuck(true);
        if(escort){if(actor.level()==p.level()){actor.getLookControl().setLookAt(p,30,30);if(actor.distanceToSqr(p)>6.25)actor.getNavigation().moveTo(p,actor.distanceToSqr(p)>64?1.5:1.15);else actor.getNavigation().stop();if(actor.tickCount%200==0&&actor.distanceToSqr(p)>64&&actor.getNavigation().isDone())say(p,"clear_route");}else actor.getNavigation().stop();}
        if(complete){var coraline=d.coraline();if(coraline!=null){var home=BlockPos.of(coraline.getLong("Home"));actor.restrictTo(home,12);if(actor.distanceToSqr(home.getCenter())>144)actor.getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);}}
    }
    private static void coraline(MinecraftServer server,WybieRescueData data){
        if(!data.problem().isEmpty())return;
        var generation=generation(server).orElse(null);
        if(generation==null)return;
        var level=server.overworld();
        var record=data.coraline();
        BlockPos home;
        UUID actorId;
        if(record!=null){
            home=BlockPos.of(record.getLong("Home"));
            actorId=record.getUUID("Actor");
            var existing=level.getEntity(actorId);
            if(existing instanceof CharacterEntity actor&&guide(actor)&&generation.equals(actor.getPersistentData().getUUID(CORALINE))){
                if((!record.contains("Pending")||record.getBoolean("Pending"))&&(!server.saveEverything(true,true,true)||!data.finishCoraline(server)))return;
                actor.setNoAi(false);
                actor.setNoGravity(false);
                actor.restrictTo(home,12);
                return;
            }
            if(existing!=null||record.contains("Pending")&&!record.getBoolean("Pending"))return;
            if(!record.getBoolean("Pending")){
                if(!level.hasChunkAt(home)||!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(home).toLong()))return;
                if(com.coraline.rebirth.LegacyActorRecovery.check(server,actorId)!=com.coraline.rebirth.LegacyActorRecovery.Result.ABSENT
                        ||!data.reopenCoraline(server,actorId))return;
            }
            for(var world:server.getAllLevels())if(world.getEntity(actorId)!=null)return;
        }else{
            var drive=BreachWorldData.get(server).drivePosition().orElse(null);
            if(drive==null||!level.hasChunkAt(drive))return;
            home=com.coraline.tycoon.TycoonWorld.landing(level,drive.east(3));
            if(home==null)return;
            actorId=UUID.randomUUID();
        }
        if(!level.hasChunkAt(home)||!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(home).toLong())
                ||!com.coraline.tycoon.TycoonWorld.safeFeet(level,home))return;
        var actor=com.coraline.registry.CharacterCast.ENTITIES.get("coraline").get().create(level);
        if(actor==null)return;
        actor.setUUID(actorId);
        actor.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,180,0);
        if(!level.noCollision(actor)||record==null&&!data.admitCoraline(server,actorId,home))return;
        actor.getPersistentData().putUUID(CORALINE,generation);
        actor.setNoAi(true);
        actor.setNoGravity(true);
        actor.setInvulnerable(true);
        actor.setPersistenceRequired();
        if(level.addFreshEntity(actor)&&server.saveEverything(true,true,true)&&data.finishCoraline(server)){
            actor.setNoAi(false);
            actor.setNoGravity(false);
            actor.restrictTo(home,12);
        }
    }
    private static ServerPlayer driver(MinecraftServer server,WybieRescueData d,CompoundTag n){
        if(n.getString("Stage").equals("STORY_LOAN")){var original=server.getPlayerList().getPlayer(d.sharedOwner(server));return original!=null&&eligible(original)?original:null;}
        var actual=n.hasUUID("Driver")?server.getPlayerList().getPlayer(n.getUUID("Driver")):null;if(actual!=null&&eligible(actual))return actual;
        var actor=find(server,n);var candidate=server.getPlayerList().getPlayers().stream().filter(WybieRescue::eligible).filter(p->actor!=null&&actor.level()==p.level()&&actor.distanceToSqr(p)<128*128||n.getString("Stage").equals("SEARCH")||n.getString("Stage").startsWith("TRAIL_")).min(java.util.Comparator.comparingDouble(p->actor!=null&&actor.level()==p.level()?actor.distanceToSqr(p):Double.MAX_VALUE)).orElse(null);
        if(candidate!=null){n.putUUID("Driver",candidate.getUUID());d.setDirty();}else if(actor!=null)actor.getNavigation().stop();return candidate;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||event.getServer().getTickCount()%20!=0)return;var d=data(event.getServer());if(d==null||!d.migrateLegacy(event.getServer()))return;coraline(event.getServer(),d);for(var p:event.getServer().getPlayerList().getPlayers())welcomeSurvivor(p,d);var n=d.shared(event.getServer());if(n!=null){var driver=driver(event.getServer(),d,n);if(driver!=null)tick(driver,d,n);}}
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event){if(!(event.getEntity() instanceof ServerPlayer p)||event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!(event.getTarget() instanceof CharacterEntity actor)||TrueHeartBattle.recoveryPending(p)||!p.isAlive()||p.isCreative()||p.isSpectator()||!InfiniteWorldData.get(p.server).accessible()||p.distanceToSqr(actor)>36)return;var d=data(p.server);if(d==null)return;var guide=d.coraline();if(guide!=null&&actor.getUUID().equals(guide.getUUID("Actor"))&&actor.getPersistentData().hasUUID(CORALINE)&&InfiniteWorldData.get(p.server).matchesGeneration(actor.getPersistentData().getUUID(CORALINE))){if(eligible(p)){welcomeSurvivor(p,d);var rescue=d.shared(p.server);if(rescue==null)start(p);else if(rescue.getString("Stage").equals("RETURNING"))acknowledge(p,d);else if(rescue.getString("Stage").equals("COMPLETE"))reward(p,d,rescue);}PostgameQuests.coraline(p);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);return;}var shared=record(actor);if(shared!=null&&shared.getString("Stage").equals("COMPLETE")){PostgameQuests.wybie(p);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);return;}var n=d.shared(p.server);if(n!=null&&n.getUUID("Actor").equals(actor.getUUID())&&record(actor)==n){if(n.getString("Stage").equals("COMPLETE")){PostgameQuests.wybie(p);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);return;}var plan=plan(n);if(n.getString("Stage").equals("ESCORT")||n.getString("Stage").equals("RETURNING")){n.putUUID("Driver",p.getUUID());d.setDirty();}if(n.getString("Stage").equals("CAGE")&&plan!=null&&FalsePalaceLayout.open(actor.level(),plan)){n.putUUID("Driver",p.getUUID());n.putString("Stage","ESCORT");if(d.checkpoint(p.server))say(p,"escort");}else say(p,"status_"+n.getString("Stage").toLowerCase(Locale.ROOT));event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}}
    private static void acknowledge(ServerPlayer p,WybieRescueData d){
        var n=d.shared(p.server);
        if(n==null||!n.getString("Stage").equals("RETURNING")||p.level()!=p.server.overworld())return;
        var actor=find(p.server,n);
        var guide=d.coraline();
        if(actor==null||guide==null||actor.level()!=p.level()||actor.distanceToSqr(BlockPos.of(guide.getLong("Home")).getCenter())>144||actor.distanceToSqr(p)>144)return;
        n.putString("Stage","COMPLETE");
        if(!n.getBoolean("RewardIssued")){
            n.putUUID("Completion",UUID.randomUUID());
            n.putUUID("RewardRecipient",p.getUUID());
            n.putBoolean("RewardIssued",false);
        }
        if(!d.checkpoint(p.server))return;
        say(p,"complete");
        reward(p,d,n);
    }
    private static void reward(ServerPlayer p,WybieRescueData d,CompoundTag n){
        if(!n.hasUUID("Completion"))return;
        var receipt=n.getUUID("Completion");
        String key=WybieRewardClaim.KEY;
        String persistedKey=WybieRewardClaim.PERSISTED;
        var persisted=p.getPersistentData().getCompound(persistedKey);
        var receipts=persisted.getCompound(key);
        var action=WybieRewardClaim.action(n,receipts,p.getUUID());
        if(action==WybieRewardClaim.Action.NONE)return;

        var realm=generation(p.server).map(id->p.server.getLevel(InfiniteRealm.key(id))).orElse(null);
        if(realm!=null)BreachWorldData.get(p.server).resolveWybieRescue(realm,p);
        if(action==WybieRewardClaim.Action.COMPLETE)return;
        var path=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");
        var nativeReceipt=WybieRewardClaim.nativeReceipt(path,p.getUUID(),receipt);
        if(action==WybieRewardClaim.Action.LEGACY_UNVERIFIED){
            if(nativeReceipt!=WybieRewardClaim.NativeReceipt.PRESENT)
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("This older rescue save recorded the reward as issued without a verifiable delivery receipt."));
            return;
        }
        if(nativeReceipt==WybieRewardClaim.NativeReceipt.PRESENT){
            n.putBoolean("RewardIssued",true);
            d.checkpoint(p.server);
            return;
        }
        if(action==WybieRewardClaim.Action.DELIVER){
            if(nativeReceipt!=WybieRewardClaim.NativeReceipt.ABSENT){
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("The rescue reward is pending while your player save is checked."));
                return;
            }
            var inventory=p.getInventory();
            var snapshot=inventory.save(new ListTag());
            var moonstone=new net.minecraft.world.item.ItemStack(com.coraline.clothing.OtherMaterials.MOONSTONE.get(),2);
            var soulglass=new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.SOULGLASS.get(),3);
            inventory.add(moonstone);
            inventory.add(soulglass);
            if(!moonstone.isEmpty()||!soulglass.isEmpty()){
                inventory.load(snapshot);
                p.inventoryMenu.broadcastChanges();
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Make room for the rescue reward, then speak to Coraline again."));
                return;
            }
            receipts.putBoolean(receipt.toString(),true);
            persisted.put(key,receipts);
            p.getPersistentData().put(persistedKey,persisted);
            inventory.setChanged();
            p.inventoryMenu.broadcastChanges();
        }
        try{
            if(java.nio.file.Files.isSymbolicLink(path)||java.nio.file.Files.isSymbolicLink(path.getParent()))return;
            ((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);
            var disk=com.coraline.rebirth.StoryRunStore.read(path);
            if(!disk.getCompound("ForgeData").getCompound(persistedKey).getCompound(key).getBoolean(receipt.toString()))return;
            n.putBoolean("RewardIssued",true);
            d.checkpoint(p.server);
        }catch(java.io.IOException|RuntimeException unavailable){
            // Inventory and its receipt are serialized together by the next save.
        }
    }
    static Optional<UUID> completion(ServerPlayer p){var d=data(p.server);var n=d==null?null:d.shared(p.server);if(n==null||n.getString("Origin").equals("SURVIVED")||!n.getString("Stage").equals("COMPLETE")||!n.hasUUID("Completion"))return Optional.empty();var actor=find(p.server,n);return actor!=null&&record(actor)==n&&actor.level()==p.server.overworld()&&actor.distanceToSqr(p)<=144?Optional.of(n.getUUID("Completion")):Optional.empty();}
    static boolean nearReturnedWybie(ServerPlayer p){
        if(!InfiniteWorldData.get(p.server).accessible())return false;
        // Research is personal, but the returned original is a shared native
        // character. A guest does not need a fabricated personal rescue row.
        for(var actor:p.serverLevel().getEntitiesOfClass(CharacterEntity.class,p.getBoundingBox().inflate(6),a->a.isAlive()&&a.distanceToSqr(p)<=36)){
            var n=record(actor);if(n!=null&&n.getString("Stage").equals("COMPLETE"))return true;
        }return false;
    }
    static UUID objective(ServerPlayer p){var d=data(p.server);var n=d==null?null:d.shared(p.server);return n==null?null:n.getUUID("Actor");}
    public static final class EscortGoal extends Goal {private final CharacterEntity actor;public EscortGoal(CharacterEntity actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}@Override public boolean canUse(){return reserved(actor);}@Override public void tick(){var n=record(actor);if(n==null)actor.getNavigation().stop();}@Override public void stop(){actor.getNavigation().stop();}}
    public static final class GuideGoal extends Goal {private final CharacterEntity actor;private int next;public GuideGoal(CharacterEntity actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}@Override public boolean canUse(){return guide(actor);}@Override public void tick(){if(actor.tickCount<next)return;next=actor.tickCount+200;var d=data(actor.getServer());var record=d==null?null:d.coraline();if(record==null||!record.getUUID("Actor").equals(actor.getUUID())||actor.level()!=actor.getServer().overworld()||!InfiniteWorldData.get(actor.getServer()).matchesGeneration(actor.getPersistentData().getUUID(CORALINE))){actor.getNavigation().stop();return;}var home=BlockPos.of(record.getLong("Home"));int beat=(int)Math.floorMod(actor.level().getDayTime()/4000,3);var at=home.offset(new int[]{-3,0,3}[beat],0,new int[]{0,-3,2}[beat]);if(actor.level().hasChunkAt(at)&&com.coraline.tycoon.TycoonWorld.safeFeet((ServerLevel)actor.level(),at))actor.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.85);}@Override public void stop(){actor.getNavigation().stop();}}
    private WybieRescue(){}
}
