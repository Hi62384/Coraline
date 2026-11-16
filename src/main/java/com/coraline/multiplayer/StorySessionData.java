package com.coraline.multiplayer;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import com.coraline.feast.VisitData;

/** Ownership journal only: a participant never receives a second VisitData.Scene.
 * All mutations run on the server thread. Copies returned to adapters cannot
 * change membership, custody, balances or a sealed reward cohort. */
public final class StorySessionData extends SavedData {
    private static final String NAME="coraline_story_sessions";
    private final Map<UUID,CompoundTag> sessions=new LinkedHashMap<>();
    private final Map<UUID,UUID> membership=new HashMap<>();
    private final Map<UUID,UUID> currentSessions=new HashMap<>();
    private final Map<UUID,List<CompoundTag>> rewardIndex=new HashMap<>();
    private final Set<UUID> durableRewards=new HashSet<>();
    private CompoundTag current(UUID owner){var id=currentSessions.get(owner);return id==null?null:sessions.get(id);}
    private CompoundTag memberSession(UUID player){var id=membership.get(player);return id==null?null:sessions.get(id);}
    private CompoundTag memberOrCurrent(UUID player){var n=memberSession(player);return n==null?current(player):n;}
    private boolean blocked;
    private StorySessionData(){}
    public static StorySessionData get(MinecraftServer server){
        var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/"+NAME+".dat");
        return server.overworld().getDataStorage().computeIfAbsent(StorySessionData::load,()->{
            var d=new StorySessionData();d.blocked=!java.nio.file.Files.notExists(file,java.nio.file.LinkOption.NOFOLLOW_LINKS);return d;
        },NAME);
    }
    private static boolean uuidKey(String key){try{return UUID.fromString(key).toString().equals(key);}catch(IllegalArgumentException bad){return false;}}
    private static StorySessionData load(CompoundTag root){
        var d=new StorySessionData();
        if((root.getInt("Version")!=1&&root.getInt("Version")!=2)||root.getBoolean("Blocked")||!root.contains("Sessions",Tag.TAG_LIST)){d.blocked=true;return d;}
        var rows=root.getList("Sessions",Tag.TAG_COMPOUND);
        if(rows.size()!=((ListTag)root.get("Sessions")).size()||rows.size()>65536){d.blocked=true;return d;}
        try{for(var raw:rows){
            var n=((CompoundTag)raw).copy();
            if(!n.hasUUID("Id")||!n.hasUUID("Owner")||!n.hasUUID("Door")||n.getLong("Revision")<0||!n.contains("Members",Tag.TAG_COMPOUND))throw new IllegalArgumentException();
            UUID owner=n.getUUID("Owner");var members=n.getCompound("Members");
            if(d.sessions.containsKey(n.getUUID("Id"))||!n.getBoolean("Archived")&&d.currentSessions.containsKey(owner)||members.size()>64||!members.contains(owner.toString(),Tag.TAG_COMPOUND))throw new IllegalArgumentException();
            for(String player:members.getAllKeys()){
                if(!uuidKey(player)||!members.contains(player,Tag.TAG_COMPOUND))throw new IllegalArgumentException();
                if(!player.equals(owner.toString())&&d.membership.putIfAbsent(UUID.fromString(player),n.getUUID("Id"))!=null)throw new IllegalArgumentException();
                var member=members.getCompound(player);String mode=member.getString("State");
                if(!Set.of("ACTIVE","PENDING","SPECTATOR","RETURNING").contains(mode))throw new IllegalArgumentException();
                if(!player.equals(owner.toString())&&(!member.contains("Return",Tag.TAG_COMPOUND)||!validReturn(member.getCompound("Return"))))throw new IllegalArgumentException();
            }
            for(String collection:List.of("Permits","Resources","Custody","Rewards"))if(n.contains(collection)&&!n.contains(collection,Tag.TAG_COMPOUND))throw new IllegalArgumentException();
            if(n.getCompound("Permits").size()>4096||n.getCompound("Resources").size()>64||n.getCompound("Custody").size()>64||n.getCompound("Rewards").size()>65536)throw new IllegalArgumentException();
            for(String player:n.getCompound("Permits").getAllKeys())if(!uuidKey(player))throw new IllegalArgumentException();
            for(String resource:n.getCompound("Resources").getAllKeys())if(!n.getCompound("Resources").contains(resource,Tag.TAG_LONG)||n.getCompound("Resources").getLong(resource)<0)throw new IllegalArgumentException();
            if(!n.getBoolean("Paused")){n.remove("ClockShift");if(n.contains("LastDirectorAt",Tag.TAG_LONG))n.putLong("PausedAt",n.getLong("LastDirectorAt"));}
            n.putBoolean("Paused",true);d.sessions.put(n.getUUID("Id"),n);if(!n.getBoolean("Archived"))d.currentSessions.put(owner,n.getUUID("Id"));
        }}catch(IllegalArgumentException malformed){d.blocked=true;d.sessions.clear();d.membership.clear();d.currentSessions.clear();}
        if(!d.blocked)for(var session:d.sessions.values())for(String key:session.getCompound("Rewards").getAllKeys()){
            var reward=session.getCompound("Rewards").getCompound(key);if(reward.hasUUID("Player")&&reward.contains("Items",Tag.TAG_LIST)&&reward.hasUUID("Id")){d.rewardIndex.computeIfAbsent(reward.getUUID("Player"),ignored->new ArrayList<>()).add(reward);d.durableRewards.add(reward.getUUID("Id"));}
        }
        return d;
    }
    private static boolean validReturn(CompoundTag n){return net.minecraft.resources.ResourceLocation.tryParse(n.getString("Dimension"))!=null&&n.contains("X",Tag.TAG_DOUBLE)&&n.contains("Y",Tag.TAG_DOUBLE)&&n.contains("Z",Tag.TAG_DOUBLE)&&Double.isFinite(n.getDouble("X"))&&Double.isFinite(n.getDouble("Y"))&&Double.isFinite(n.getDouble("Z"))&&n.getInt("Mode")>=0&&n.getInt("Mode")<=3;}
    private boolean thread(MinecraftServer server){return !blocked&&server.isSameThread();}
    public boolean blocked(){return blocked;}
    public boolean guest(UUID player){return !blocked&&membership.containsKey(player);}
    /** One packet per actual client, using the owner's existing audio pipeline.
     * Guest mixer acknowledgments cannot advance the director. */
    public List<ServerPlayer> audience(ServerPlayer owner){
        if(!thread(owner.server)||guest(owner.getUUID()))return List.of(owner);
        var n=current(owner.getUUID());if(n==null||n.getBoolean("Paused"))return List.of(owner);
        var out=new ArrayList<ServerPlayer>();out.add(owner);
        for(String id:n.getCompound("Members").getAllKeys()){
            if(id.equals(owner.getUUID().toString())||!n.getCompound("Permits").getBoolean(id))continue;
            String state=n.getCompound("Members").getCompound(id).getString("State");if(!state.equals("ACTIVE")&&!state.equals("SPECTATOR"))continue;
            var guest=owner.server.getPlayerList().getPlayer(UUID.fromString(id));if(guest!=null&&guest.serverLevel()==owner.serverLevel())out.add(guest);
        }return List.copyOf(out);
    }

    public Optional<UUID> owner(UUID player){var n=memberOrCurrent(player);return blocked||n==null?Optional.empty():Optional.of(n.getUUID("Owner"));}
    public CompoundTag snapshot(UUID owner){var n=current(owner);return blocked||n==null?new CompoundTag():n.copy();}
    /** Debug presentation deliberately excludes saved inventories and return coordinates. */
    public CompoundTag debugView(MinecraftServer server,UUID player){
        var out=new CompoundTag();out.putBoolean("Blocked",blocked);var n=memberOrCurrent(player);if(n==null||blocked)return out;
        for(String key:List.of("Id","Owner","Door","Phase","ActivityId"))if(n.hasUUID(key))out.putUUID(key,n.getUUID(key));out.putLong("Revision",n.getLong("Revision"));out.putString("Checkpoint",n.getString("Checkpoint"));out.putBoolean("Paused",n.getBoolean("Paused"));out.putBoolean("Archived",n.getBoolean("Archived"));out.putInt("Permits",n.getCompound("Permits").size());
        var rows=new ListTag();for(String id:n.getCompound("Members").getAllKeys()){
            var member=n.getCompound("Members").getCompound(id);var row=new CompoundTag();UUID uuid=UUID.fromString(id);row.putUUID("Player",uuid);row.putString("State",member.getString("State"));var online=server.getPlayerList().getPlayer(uuid);row.putBoolean("Online",online!=null);if(online!=null)row.putString("Name",online.getGameProfile().getName());row.putBoolean("Escrow",member.getCompound("AcceptedInventory").getBoolean("EscrowStored")&&!member.getCompound("AcceptedInventory").getBoolean("EscrowRestored"));row.putBoolean("ModeHeld",member.hasUUID("SpectatorActivity"));rows.add(row);
        }out.put("Members",rows);int queued=0;for(String id:n.getCompound("Rewards").getAllKeys())if(!n.getCompound("Rewards").getCompound(id).getString("State").equals("DELIVERED"))queued++;out.putInt("QueuedRewards",queued);return out;
    }
    private CompoundTag own(ServerPlayer p){return thread(p.server)?current(p.getUUID()):null;}
    /** Observe the existing director; never create an alternate director for a guest. */
    public boolean observe(ServerPlayer p,VisitData.Scene scene){
        if(!thread(p.server)||scene==null||!p.getUUID().equals(scene.owner))return false;
        if(membership.containsKey(p.getUUID()))return false;
        var n=current(p.getUUID());
        if(n==null){if(sessions.size()>=65536)return false;n=new CompoundTag();n.putUUID("Id",UUID.randomUUID());n.putUUID("Owner",p.getUUID());n.putUUID("Door",scene.door);n.putLong("Revision",0);var members=new CompoundTag();var owner=new CompoundTag();owner.putString("State","ACTIVE");members.put(p.getUUID().toString(),owner);n.put("Members",members);sessions.put(n.getUUID("Id"),n);currentSessions.put(p.getUUID(),n.getUUID("Id"));setDirty();}
        if(!scene.door.equals(n.getUUID("Door"))){
            n.putBoolean("Archived",true);n.putBoolean("Paused",true);for(String id:n.getCompound("Members").getAllKeys())if(!id.equals(p.getUUID().toString()))n.getCompound("Members").getCompound(id).putString("State","RETURNING");currentSessions.remove(p.getUUID());setDirty();return observe(p,scene);
        }
        if(n.getBoolean("DebugPaused"))return false;
        String checkpoint=scene.stage.name()+":"+scene.loop.phase.name()+":"+scene.loop.generation+":"+scene.loop.activity.name()+":"+scene.loop.event.getInt("Serial")+":"+scene.loop.story.getCompound("FirstEscape").getString("Phase")+":"+scene.loop.story.getCompound("AcceptedButtonEyes").getString("Phase")+":"+scene.loop.story.getCompound("AcceptedButtonEyes").getCompound(com.coraline.feast.GhostVictimProgress.KEY).getString("Phase");
        if(!checkpoint.equals(n.getString("Checkpoint"))){
            if(n.getLong("Revision")==Long.MAX_VALUE)return false;
            n.putString("Checkpoint",checkpoint);n.putLong("Revision",n.getLong("Revision")+1);n.putUUID("Phase",UUID.randomUUID());n.putLong("CheckpointAt",p.server.overworld().getGameTime());
            var cohort=new CompoundTag();for(String id:n.getCompound("Members").getAllKeys()){var m=n.getCompound("Members").getCompound(id);if(m.getString("State").equals("ACTIVE"))cohort.putBoolean(id,true);}n.put("RewardCohort",cohort);setDirty();
        }
        if(n.getBoolean("Paused")){
            if(n.contains("PausedAt",Tag.TAG_LONG)){
                if(!n.contains("ClockShift",Tag.TAG_COMPOUND)){var shift=new CompoundTag();shift.putUUID("Id",UUID.randomUUID());shift.putLong("Delta",Math.max(0,p.server.overworld().getGameTime()-n.getLong("PausedAt")));n.put("ClockShift",shift);setDirty();if(!checkpoint(p.server))return false;}
                var shift=n.getCompound("ClockShift");if(!shift.hasUUID("Id")||shift.getLong("Delta")<0)return false;
                StoryDirectorClock.apply(scene,shift.getUUID("Id"),shift.getLong("Delta"));StoryDirectorClock.applyMembers(n,shift.getUUID("Id"),shift.getLong("Delta"));VisitData.get(p.server).setDirty();if(!checkpoint(p.server)||!StoryDirectorClock.saved(p,scene,shift.getUUID("Id")))return false;
            }
            n.putBoolean("Paused",false);setDirty();
        }
        n.putLong("LastDirectorAt",p.server.overworld().getGameTime());setDirty();return true;
    }
    public boolean permit(ServerPlayer owner,UUID guest,boolean allow){
        var n=own(owner);if(n==null||guest==null||guest.equals(owner.getUUID()))return false;
        if(!n.contains("Permits"))n.put("Permits",new CompoundTag());var permits=n.getCompound("Permits");
        if(allow){if(!permits.contains(guest.toString())&&permits.size()>=4096)return false;permits.putBoolean(guest.toString(),true);}else{var actualGuest=owner.server.getPlayerList().getPlayer(guest);if(n.getCompound("Members").contains(guest.toString())&&(actualGuest!=null?!QuestItemRecovery.capture(actualGuest):!QuestItemRecovery.captureOffline(owner.server,guest,VisitData.get(owner.server).scenes.get(owner.getUUID()))))return false;permits.remove(guest.toString());var members=n.getCompound("Members");if(members.contains(guest.toString()))members.getCompound(guest.toString()).putString("State","RETURNING");}
        setDirty();return checkpoint(owner.server);
    }
    /** Called by the verified entrance adapter before transport. The supplied
     * door must be the native entrance actually touched by the requesting player. */
    public boolean request(ServerPlayer guest,ServerPlayer owner,UUID touchedDoor){
        if(com.coraline.feast.AcceptedButtonEyesInventory.locked(guest)||!thread(guest.server)||guest.server!=owner.server||guest==owner||membership.containsKey(guest.getUUID())||!guest.isAlive()||guest.isSpectator()||guest.isPassenger()||guest.getCamera()!=guest)return false;
        var personal=VisitData.get(guest.server).scenes.get(guest.getUUID());if(personal!=null&&(personal.stage!=VisitData.Stage.DONE||personal.loop.active()))return false;
        var hosted=current(guest.getUUID());if(hosted!=null&&hosted.getCompound("Members").size()>1)return false;
        var door=com.coraline.otherworld.DoorData.get(guest.server).doors.get(touchedDoor);
        if(door==null||door.revoked||!door.exists||!door.placed||!owner.getUUID().equals(door.owner)||!guest.level().dimension().location().toString().equals(door.dimension)||!guest.serverLevel().hasChunkAt(door.pos)||guest.distanceToSqr(door.pos.getCenter())>25||!(guest.level().getBlockEntity(door.pos) instanceof com.coraline.otherworld.DoorBlockEntity nativeDoor)||!touchedDoor.equals(nativeDoor.doorId))return false;
        if(!com.coraline.otherworld.OtherWorld.isOther(owner.level())&&!com.coraline.feast.ActivityWorld.isActivity(owner.level()))return false;
        if(!owner.getPersistentData().hasUUID("CoralineVisitDoor")||!owner.getPersistentData().getUUID("CoralineVisitDoor").equals(touchedDoor))return false;
        var n=own(owner);if(n==null||n.getBoolean("Paused")||!n.getUUID("Door").equals(touchedDoor)||!n.getCompound("Permits").getBoolean(guest.getUUID().toString())||n.getCompound("Members").size()>=64)return false;
        var back=new CompoundTag();back.putString("Dimension",guest.level().dimension().location().toString());back.putDouble("X",guest.getX());back.putDouble("Y",guest.getY());back.putDouble("Z",guest.getZ());back.putFloat("Yaw",guest.getYRot());back.putFloat("Pitch",guest.getXRot());back.putInt("Mode",guest.gameMode.getGameModeForPlayer().getId());
        if(!validReturn(back))return false;var member=new CompoundTag();member.putString("State","PENDING");member.put("Return",back);if(guest.getPersistentData().hasUUID("CoralineVisitDoor"))member.putUUID("OriginalDoor",guest.getPersistentData().getUUID("CoralineVisitDoor"));member.putLong("RequestedAt",guest.server.overworld().getGameTime());n.getCompound("Members").put(guest.getUUID().toString(),member);membership.put(guest.getUUID(),n.getUUID("Id"));setDirty();return checkpoint(guest.server);
    }
    public boolean activate(MinecraftServer server,UUID owner,UUID guest,UUID phase){
        if(!thread(server))return false;var n=current(owner);if(n==null||n.getBoolean("Paused")||!n.hasUUID("Phase")||!n.getUUID("Phase").equals(phase)||!n.getCompound("Permits").getBoolean(guest.toString()))return false;
        var m=n.getCompound("Members").getCompound(guest.toString());if(!m.getString("State").equals("PENDING"))return false;
        var scene=VisitData.get(server).scenes.get(owner);boolean cohortAdded=false;
        if(scene!=null&&scene.loop.phase==com.coraline.feast.OtherMotherLoopState.Phase.ACTIVITY&&n.hasUUID("ActivityId")&&n.getInt("ActivityGeneration")==scene.loop.generation&&n.getString("ActivityKind").equals(scene.loop.activity.name())){
            var cohort=n.getCompound("ActivityCohort");if(!cohort.contains(guest.toString())){if(cohort.size()>=64)return false;cohort.putBoolean(guest.toString(),true);cohortAdded=true;}
        }
        m.putString("State","ACTIVE");m.putUUID("JoinedPhase",phase);setDirty();if(checkpoint(server))return true;
        m.putString("State","PENDING");m.remove("JoinedPhase");if(cohortAdded)n.getCompound("ActivityCohort").remove(guest.toString());setDirty();return false;
    }
    public boolean mayAct(ServerPlayer p,UUID session,UUID phase){
        if(!thread(p.server)||!p.isAlive()||p.isSpectator())return false;var n=memberOrCurrent(p.getUUID());var owner=n==null?null:n.getUUID("Owner");
        return n!=null&&p.server.getPlayerList().getPlayer(owner)!=null&&!n.getBoolean("Paused")&&n.getUUID("Id").equals(session)&&n.hasUUID("Phase")&&n.getUUID("Phase").equals(phase)&&n.getCompound("Members").getCompound(p.getUUID().toString()).getString("State").equals("ACTIVE")&&(owner.equals(p.getUUID())||n.getCompound("Permits").getBoolean(p.getUUID().toString()));
    }
    /** Atomic server-thread pool update. Purchasers remain the actual players. */
    public boolean resource(ServerPlayer p,UUID session,UUID phase,String key,long delta){
        if(!mayAct(p,session,phase)||!key.matches("[a-z_]{1,40}"))return false;var n=memberOrCurrent(p.getUUID());if(!n.contains("Resources"))n.put("Resources",new CompoundTag());var pools=n.getCompound("Resources");if(!pools.contains(key)&&pools.size()>=64)return false;long old=pools.getLong(key),next;try{next=Math.addExact(old,delta);}catch(ArithmeticException overflow){return false;}if(next<0)return false;pools.putLong(key,next);setDirty();return true;
    }
    /** A reward reservation is durable before its existing inventory transaction
     * runs. Ambiguous RESERVED records block retries instead of duplicating loot. */
    public Optional<UUID> reserveReward(ServerPlayer p,UUID session,UUID phase,String reward){
        if(!mayAct(p,session,phase)||!reward.matches("[a-z_]{1,40}"))return Optional.empty();var n=memberOrCurrent(p.getUUID());if(!n.getCompound("RewardCohort").getBoolean(p.getUUID().toString()))return Optional.empty();
        if(!n.contains("Rewards"))n.put("Rewards",new CompoundTag());var rewards=n.getCompound("Rewards");String key=phase+":"+p.getUUID()+":"+reward;if(rewards.contains(key)||rewards.size()>=65536)return Optional.empty();var receipt=new CompoundTag();UUID id=UUID.randomUUID();receipt.putUUID("Id",id);receipt.putUUID("Player",p.getUUID());receipt.putString("State","RESERVED");rewards.put(key,receipt);setDirty();return checkpoint(p.server)?Optional.of(id):Optional.empty();
    }
    public boolean checkpoint(MinecraftServer server){
        if(!thread(server))return false;try{var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/"+NAME+".dat");if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;setDirty();server.overworld().getDataStorage().save();return com.coraline.rebirth.StoryRunStore.read(file).getCompound("data").equals(save(new CompoundTag()));}catch(java.io.IOException|RuntimeException failure){return false;}
    }
    public CompoundTag guestInventory(ServerPlayer p){
        if(!thread(p.server))return null;var n=memberSession(p.getUUID());if(n==null)return null;var m=n.getCompound("Members").getCompound(p.getUUID().toString());if(!m.contains("AcceptedInventory",Tag.TAG_COMPOUND))m.put("AcceptedInventory",new CompoundTag());return m.getCompound("AcceptedInventory");
    }
    public CompoundTag inventoryWitness(ServerPlayer p){var n=memberSession(p.getUUID());return !thread(p.server)||n==null?new CompoundTag():n.getCompound("InventoryReturnWitness").copy();}
    public void authorizeInventoryReturn(MinecraftServer server,VisitData.Scene scene){
        if(!thread(server)||scene==null||VisitData.get(server).scenes.get(scene.owner)!=scene)return;
        var n=current(scene.owner);if(n==null||!n.getUUID("Door").equals(scene.door))return;
        var accepted=com.coraline.feast.AcceptedButtonEyesProgress.data(scene);var ghost=com.coraline.feast.GhostVictimEntry.data(scene);var escape=ghost.getCompound(com.coraline.feast.GhostEscapeProgress.KEY);var snapshot=accepted.getCompound("InventoryEscrow");
        if(!com.coraline.feast.GhostEscapeProgress.owns(ghost,escape)||!escape.getBoolean("WagerWon")||!escape.getBoolean("PlayerEyesHeld")||!escape.hasUUID("OriginalEscrow")||!snapshot.hasUUID("Transaction")||!escape.getUUID("OriginalEscrow").equals(snapshot.getUUID("Transaction"))||com.coraline.feast.GhostEscapeProgress.phase(escape).ordinal()<com.coraline.feast.GhostEscapeProgress.Phase.RESTORING.ordinal())return;
        if(n.getCompound("InventoryReturnWitness").hasUUID("OwnerEscrow"))return;
        var witness=new CompoundTag();witness.putUUID("Session",n.getUUID("Id"));witness.putUUID("Owner",scene.owner);witness.putUUID("Door",scene.door);witness.putUUID("OwnerEscrow",snapshot.getUUID("Transaction"));witness.putLong("At",server.overworld().getGameTime());n.put("InventoryReturnWitness",witness);setDirty();checkpoint(server);
    }
    public record Frame(UUID id,UUID owner,UUID door,UUID phase,long revision,String state,boolean paused,boolean archived){}
    public Frame frame(UUID player){var n=memberOrCurrent(player);if(blocked||n==null||!n.hasUUID("Phase"))return null;return new Frame(n.getUUID("Id"),n.getUUID("Owner"),n.getUUID("Door"),n.getUUID("Phase"),n.getLong("Revision"),n.getCompound("Members").getCompound(player.toString()).getString("State"),n.getBoolean("Paused"),n.getBoolean("Archived"));}
    public boolean hasGuests(UUID owner){var n=current(owner);return !blocked&&n!=null&&n.getCompound("Members").size()>1;}
    /** Capture eligibility at the actual arena entrance, never at payout time. */
    public boolean beginActivity(ServerPlayer owner,VisitData.Scene scene){
        if(!thread(owner.server)||!owner.getUUID().equals(scene.owner)||VisitData.get(owner.server).scenes.get(scene.owner)!=scene)return false;
        if(!observe(owner,scene))return false;var n=current(scene.owner);
        n.putUUID("ActivityId",UUID.randomUUID());n.putInt("ActivityGeneration",scene.loop.generation);n.putString("ActivityKind",scene.loop.activity.name());
        var cohort=new CompoundTag();for(var p:participants(owner.server,scene.owner,false))if(p.serverLevel()==owner.serverLevel())cohort.putBoolean(p.getUUID().toString(),true);cohort.putBoolean(owner.getUUID().toString(),true);n.put("ActivityCohort",cohort);setDirty();return checkpoint(owner.server);
    }
    public boolean groupActivity(VisitData.Scene scene){var n=scene==null?null:current(scene.owner);return !blocked&&n!=null&&n.hasUUID("ActivityId")&&n.getInt("ActivityGeneration")==scene.loop.generation&&n.getString("ActivityKind").equals(scene.loop.activity.name())&&n.getCompound("ActivityCohort").size()>1;}
    public CompoundTag activityProgress(ServerPlayer p,VisitData.Scene scene,String key){
        if(!thread(p.server)||!groupActivity(scene)||!StoryCoop.member(p,scene)||!key.matches("[a-z_]{1,40}"))return null;var n=memberOrCurrent(p.getUUID());var member=n.getCompound("Members").getCompound(p.getUUID().toString());
        if(!member.hasUUID("ProgressActivity")||!member.getUUID("ProgressActivity").equals(n.getUUID("ActivityId"))){member.putUUID("ProgressActivity",n.getUUID("ActivityId"));member.put("ActivityProgress",new CompoundTag());}
        var progress=member.getCompound("ActivityProgress");if(!progress.contains(key)){if(progress.size()>=64)return null;progress.put(key,new CompoundTag());}setDirty();return progress.getCompound(key);
    }
    /** Virtual mall money, stock and purchaser bags share the reward journal's
     * authority. Reattaching after a crash cannot replay a stale scene copy. */
    public boolean bindMallState(ServerPlayer actor,VisitData.Scene scene){
        if(!thread(actor.server)||scene==null||scene.loop.activity!=com.coraline.feast.OtherMotherActivities.Activity.SHOPPING_MALL||!StoryCoop.member(actor,scene))return false;
        var n=current(scene.owner);if(n!=null&&!n.hasUUID("ActivityId")&&scene.loop.phase==com.coraline.feast.OtherMotherLoopState.Phase.ACTIVITY){var actualOwner=actor.server.getPlayerList().getPlayer(scene.owner);if(actualOwner==null||!beginActivity(actualOwner,scene))return false;n=current(scene.owner);}if(n==null||!n.hasUUID("ActivityId")||n.getInt("ActivityGeneration")!=scene.loop.generation||!n.getString("ActivityKind").equals(scene.loop.activity.name()))return false;
        if(!n.hasUUID("MallActivity")||!n.getUUID("MallActivity").equals(n.getUUID("ActivityId"))){n.putUUID("MallActivity",n.getUUID("ActivityId"));n.put("MallState",scene.loop.expedition);setDirty();if(!checkpoint(actor.server))return false;}
        scene.loop.expedition=n.getCompound("MallState");setDirty();return true;
    }
    public boolean queueDirectedReward(ServerPlayer director,VisitData.Scene scene,UUID recipient,String key,List<net.minecraft.world.item.ItemStack> items){
        if(!thread(director.server)||!director.getUUID().equals(scene.owner)||VisitData.get(director.server).scenes.get(scene.owner)!=scene)return false;var n=current(scene.owner);
        if(n==null||!n.hasUUID("ActivityId")||n.getInt("ActivityGeneration")!=scene.loop.generation||!n.getString("ActivityKind").equals(scene.loop.activity.name()))return false;
        if(!directed(n,recipient,key,items))return false;setDirty();boolean saved=checkpoint(director.server);if(saved)for(String id:n.getCompound("Rewards").getAllKeys()){var grant=n.getCompound("Rewards").getCompound(id);if(grant.hasUUID("Id"))durableRewards.add(grant.getUUID("Id"));}return saved;
    }
    private boolean directed(CompoundTag session,UUID recipient,String key,List<net.minecraft.world.item.ItemStack> items){
        if(recipient==null||!key.matches("[a-z_0-9]{1,64}")||items.size()>8192||!session.hasUUID("ActivityId"))return false;if(items.isEmpty())return true;
        if(!session.contains("Rewards"))session.put("Rewards",new CompoundTag());var rewards=session.getCompound("Rewards");String receipt="direct:"+session.getUUID("ActivityId")+":"+recipient+":"+key;if(rewards.contains(receipt))return true;if(rewards.size()>=65536)return false;
        var payload=new ListTag();for(var item:items)if(!item.isEmpty())payload.add(item.copy().save(new CompoundTag()));if(payload.isEmpty())return true;
        var grant=new CompoundTag();grant.putUUID("Id",UUID.randomUUID());grant.putUUID("Player",recipient);grant.putUUID("Session",session.getUUID("Id"));grant.putUUID("Completion",session.getUUID("ActivityId"));grant.putString("State","QUEUED");grant.put("Items",payload);rewards.put(receipt,grant);rewardIndex.computeIfAbsent(recipient,ignored->new ArrayList<>()).add(grant);return true;
    }
    public boolean eliminateActivity(ServerPlayer p,VisitData.Scene scene){
        if(!thread(p.server)||!groupActivity(scene)||!com.coraline.feast.ActivityWorld.isActivity(p.level())||com.coraline.feast.AcceptedButtonEyes.active(scene)||!StoryCoop.member(p,scene))return false;
        var n=memberOrCurrent(p.getUUID());var m=n.getCompound("Members").getCompound(p.getUUID().toString());if(!m.getString("State").equals("ACTIVE")||p.isSpectator())return false;
        m.putInt("SpectatorMode",p.gameMode.getGameModeForPlayer().getId());m.putUUID("SpectatorActivity",n.getUUID("ActivityId"));m.putString("State","SPECTATOR");setDirty();
        if(!checkpoint(p.server)){m.putString("State","ACTIVE");m.remove("SpectatorMode");m.remove("SpectatorActivity");setDirty();return false;}
        p.getPersistentData().putInt("CoralineCoopSpectatorMode",m.getInt("SpectatorMode"));p.getPersistentData().putUUID("CoralineCoopSpectatorSession",n.getUUID("Id"));
        p.closeContainer();p.stopRiding();p.setCamera(p);p.setForcedPose(null);p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);p.fallDistance=0;return true;
    }
    public boolean activitySpectator(ServerPlayer p){var n=memberOrCurrent(p.getUUID());return !blocked&&n!=null&&n.getCompound("Members").getCompound(p.getUUID().toString()).hasUUID("SpectatorActivity");}
    public void restoreActivityMode(ServerPlayer p){
        if(!thread(p.server))return;var n=memberOrCurrent(p.getUUID());var m=n==null?null:n.getCompound("Members").getCompound(p.getUUID().toString());
        if(m==null||!m.hasUUID("SpectatorActivity"))return;int mode=m.getInt("SpectatorMode");if(mode<0||mode>3)return;
        p.setCamera(p);p.setForcedPose(null);p.setGameMode(net.minecraft.world.level.GameType.byId(mode));p.fallDistance=0;
        p.getPersistentData().remove("CoralineCoopSpectatorMode");p.getPersistentData().remove("CoralineCoopSpectatorSession");
        try{p.server.getPlayerList().saveAll();var saved=com.coraline.rebirth.StoryRunStore.read(p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));if(saved.getInt("playerGameType")!=mode||saved.getCompound("ForgeData").hasUUID("CoralineCoopSpectatorSession"))return;
            m.remove("SpectatorActivity");m.remove("SpectatorMode");if(m.getString("State").equals("SPECTATOR"))m.putString("State","ACTIVE");setDirty();checkpoint(p.server);
        }catch(java.io.IOException|RuntimeException unavailable){/* Retain the original mode until native readback succeeds. */}
    }
    boolean queueActivityReward(ServerPlayer actor,VisitData.Scene scene,String key,List<net.minecraft.world.item.ItemStack> items){
        if(!thread(actor.server)||!StoryCoop.member(actor,scene)||!key.matches("[a-z_0-9]{1,64}")||items.isEmpty()||items.size()>2048)return false;
        var n=current(scene.owner);if(n==null||!n.hasUUID("ActivityId")||n.getInt("ActivityGeneration")!=scene.loop.generation||!n.getString("ActivityKind").equals(scene.loop.activity.name()))return false;
        if(!n.contains("Rewards"))n.put("Rewards",new CompoundTag());var rewards=n.getCompound("Rewards");
        var payload=new ListTag();for(var item:items)if(!item.isEmpty())payload.add(item.copy().save(new CompoundTag()));if(payload.isEmpty())return true;
        var cohort=n.getCompound("ActivityCohort");int needed=0;for(String player:cohort.getAllKeys())if(!rewards.contains(n.getUUID("ActivityId")+":"+key+":"+player))needed++;if(rewards.size()+needed>65536)return false;
        for(String player:cohort.getAllKeys()){
            String receipt=n.getUUID("ActivityId")+":"+key+":"+player;if(rewards.contains(receipt))continue;
            var grant=new CompoundTag();grant.putUUID("Id",UUID.randomUUID());grant.putUUID("Player",UUID.fromString(player));grant.putUUID("Session",n.getUUID("Id"));grant.putUUID("Completion",n.getUUID("ActivityId"));grant.putString("State","QUEUED");grant.put("Items",payload.copy());rewards.put(receipt,grant);rewardIndex.computeIfAbsent(UUID.fromString(player),ignored->new ArrayList<>()).add(grant);
        }setDirty();boolean saved=checkpoint(actor.server);if(saved)for(String id:rewards.getAllKeys()){var grant=rewards.getCompound(id);if(grant.hasUUID("Id"))durableRewards.add(grant.getUUID("Id"));}return saved;
    }
    List<CompoundTag> pendingRewards(ServerPlayer player){
        if(!thread(player.server))return List.of();var pending=rewardIndex.getOrDefault(player.getUUID(),List.of()).stream().filter(n->!n.getString("State").equals("DELIVERED")).limit(16).toList();
        if(pending.stream().anyMatch(n->!durableRewards.contains(n.getUUID("Id")))){if(!checkpoint(player.server))return List.of();for(var grant:pending)durableRewards.add(grant.getUUID("Id"));}return pending;
    }
    public VisitData.Scene scene(ServerPlayer p){
        if(!guest(p.getUUID())||memberState(p.getUUID()).equals("PENDING"))return VisitData.get(p.server).scenes.get(p.getUUID());
        if(admittedOwner(p.getUUID()).isEmpty())return null;
        var f=frame(p.getUUID());var scene=f==null?null:VisitData.get(p.server).scenes.get(f.owner());return scene!=null&&scene.door.equals(f.door())?scene:null;
    }
    public String memberState(UUID player){var n=memberSession(player);return n==null?"":n.getCompound("Members").getCompound(player.toString()).getString("State");}
    public Optional<UUID> admittedOwner(UUID player){var n=memberSession(player);if(blocked||n==null||n.getBoolean("Archived")||n.getBoolean("Paused")||!n.getCompound("Permits").getBoolean(player.toString())||!Set.of("ACTIVE","SPECTATOR").contains(memberState(player)))return Optional.empty();return Optional.of(n.getUUID("Owner"));}
    public CompoundTag inventoryLane(ServerPlayer p,VisitData.Scene scene){
        if(!thread(p.server)||scene==null)return null;
        if(scene.owner.equals(p.getUUID()))return com.coraline.feast.AcceptedButtonEyesProgress.data(scene);
        var n=memberSession(p.getUUID());if(n==null||!n.getUUID("Owner").equals(scene.owner)||!n.getUUID("Door").equals(scene.door))return null;
        var m=n.getCompound("Members").getCompound(p.getUUID().toString());if(!m.contains("AcceptedInventory",Tag.TAG_COMPOUND))m.put("AcceptedInventory",new CompoundTag());return m.getCompound("AcceptedInventory");
    }
    public void touch(MinecraftServer server){if(thread(server))setDirty();}
    public List<ServerPlayer> participants(MinecraftServer server,UUID owner,boolean spectators){
        var n=current(owner);if(blocked||n==null||n.getBoolean("Paused"))return List.of();var out=new ArrayList<ServerPlayer>();
        for(String id:n.getCompound("Members").getAllKeys()){String state=n.getCompound("Members").getCompound(id).getString("State");if(!state.equals("ACTIVE")&&!(spectators&&state.equals("SPECTATOR")))continue;if(!id.equals(owner.toString())&&!n.getCompound("Permits").getBoolean(id))continue;var p=server.getPlayerList().getPlayer(UUID.fromString(id));if(p!=null)out.add(p);}return List.copyOf(out);
    }
    public boolean returning(UUID player){var n=memberSession(player);return !blocked&&n!=null&&n.getCompound("Members").getCompound(player.toString()).getString("State").equals("RETURNING");}
    public boolean linkCustody(MinecraftServer server,UUID session,UUID owner,UUID door,UUID holder,UUID receipt){
        if(!thread(server)||blocked)return false;var n=sessions.get(session);
        if(n==null||!owner.equals(n.getUUID("Owner"))||!door.equals(n.getUUID("Door"))||!n.getCompound("Members").contains(holder.toString())&&!knownQuestHolder(n,holder))return false;
        if(!n.contains("Custody"))n.put("Custody",new CompoundTag());var custody=n.getCompound("Custody");
        if(custody.contains(receipt.toString())){var row=custody.getCompound(receipt.toString());return row.hasUUID("Holder")&&holder.equals(row.getUUID("Holder"));}
        if(custody.size()>=64)return false;var row=new CompoundTag();row.putUUID("Holder",holder);row.putUUID("Recovery",receipt);custody.put(receipt.toString(),row);setDirty();return checkpoint(server);
    }
    private static boolean knownQuestHolder(CompoundTag session,UUID holder){for(String key:session.getCompound("Custody").getAllKeys()){var row=session.getCompound("Custody").getCompound(key);if(key.startsWith("item_")&&row.hasUUID("Holder")&&holder.equals(row.getUUID("Holder")))return true;}return false;}
    public boolean provesQuestHolder(UUID owner,UUID door,UUID holder){var n=current(owner);return n!=null&&door.equals(n.getUUID("Door"))&&knownQuestHolder(n,holder);}
    public Set<UUID> questHolders(UUID owner){var n=current(owner);if(n==null)return Set.of();var ids=new HashSet<UUID>();for(String id:n.getCompound("Members").getAllKeys())ids.add(UUID.fromString(id));for(String key:n.getCompound("Custody").getAllKeys()){var row=n.getCompound("Custody").getCompound(key);if(key.startsWith("item_")&&row.hasUUID("Holder"))ids.add(row.getUUID("Holder"));}return Set.copyOf(ids);}
    public CompoundTag questItem(UUID owner,UUID door,String kind){var n=current(owner);return n!=null&&door.equals(n.getUUID("Door"))?n.getCompound("Custody").getCompound("item_"+kind).copy():new CompoundTag();}
    public boolean reserveQuestTool(ServerPlayer actor,VisitData.Scene scene,String kind){
        if(!thread(actor.server)||!StoryCoop.member(actor,scene)||!Set.of("cane","shoe").contains(kind))return false;var n=current(scene.owner);if(n==null)return false;var custody=n.getCompound("Custody");String key="item_"+kind;
        var old=custody.getCompound(key);if(old.hasUUID("Holder"))return actor.getUUID().equals(old.getUUID("Holder"))&&old.getString("State").equals("CLAIMING")&&checkpoint(actor.server);
        if(custody.size()>=64)return false;var row=new CompoundTag();row.putUUID("Holder",actor.getUUID());row.putString("State","CLAIMING");custody.put(key,row);n.put("Custody",custody);setDirty();return checkpoint(actor.server);
    }
    public boolean questHolder(MinecraftServer server,UUID session,UUID holder,UUID receipt,ListTag kinds){
        if(!thread(server)||kinds.size()>7)return false;var n=sessions.get(session);if(n==null)return false;var custody=n.getCompound("Custody");
        for(var raw:kinds){String kind=raw.getAsString();if(!Set.of("key","stone","cane","shoe","eye_0","eye_1","eye_2").contains(kind))return false;String key="item_"+kind;if(!custody.contains(key)&&custody.size()>=64)return false;var row=new CompoundTag();row.putUUID("Holder",holder);row.putUUID("Delivery",receipt);row.putString("State","HELD");custody.put(key,row);}n.put("Custody",custody);setDirty();return checkpoint(server);
    }
    public void finishCustody(MinecraftServer server,UUID session,UUID receipt){if(!thread(server))return;var n=sessions.get(session);if(n==null)return;n.getCompound("Custody").remove(receipt.toString());setDirty();checkpoint(server);}
    public boolean requestReturn(ServerPlayer p){
        if(!thread(p.server))return false;var n=memberSession(p.getUUID());if(n==null)return false;
        if(!QuestItemRecovery.capture(p))return false;
        n.getCompound("Members").getCompound(p.getUUID().toString()).putString("State","RETURNING");setDirty();return checkpoint(p.server);
    }
    /** Idempotent physical/mode restoration. An accepted-branch inventory must
     * finish its own exact-once escrow transaction before this can detach it. */
    public boolean restore(ServerPlayer p){
        if(!thread(p.server)||!returning(p.getUUID()))return false;
        var n=memberSession(p.getUUID());var members=n.getCompound("Members");var member=members.getCompound(p.getUUID().toString());var back=member.getCompound("Return");if(!validReturn(back))return false;
        var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");
        if(!member.getBoolean("ReturnApplied")){
            try{if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;var proof=com.coraline.rebirth.StoryRunStore.read(file).getCompound("ForgeData");if(proof.hasUUID("CoralineCoopReturnedSession")&&proof.getUUID("CoralineCoopReturnedSession").equals(n.getUUID("Id")))member.putBoolean("ReturnApplied",true);}catch(java.io.IOException unavailable){/* A first return may create the native player file. */}
        }
        if(!member.getBoolean("ReturnApplied")){
            var level=com.coraline.parents.FamilyData.level(p.server,back.getString("Dimension"));if(level==null)return false;
            var original=net.minecraft.core.BlockPos.containing(back.getDouble("X"),back.getDouble("Y"),back.getDouble("Z"));if(!level.getWorldBorder().isWithinBounds(original))return false;level.getChunkAt(original);
            var at=com.coraline.tycoon.TycoonWorld.safeFeet(level,original)?original:com.coraline.tycoon.TycoonWorld.landing(level,original);if(at==null)return false;
            p.getPersistentData().putBoolean("CoralineRecovering",true);
            try{p.closeContainer();p.stopRiding();p.stopUsingItem();p.setCamera(p);p.setForcedPose(null);p.setGameMode(net.minecraft.world.level.GameType.byId(back.getInt("Mode")));p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                p.teleportTo(level,at.equals(original)?back.getDouble("X"):at.getX()+.5,at.equals(original)?back.getDouble("Y"):at.getY(),at.equals(original)?back.getDouble("Z"):at.getZ()+.5,back.getFloat("Yaw"),back.getFloat("Pitch"));
                if(member.hasUUID("OriginalDoor"))p.getPersistentData().putUUID("CoralineVisitDoor",member.getUUID("OriginalDoor"));else p.getPersistentData().remove("CoralineVisitDoor");
                com.coraline.feast.CooperativeActivities.cleanPersonal(p);p.getPersistentData().remove("CoralineCoopSpectatorMode");p.getPersistentData().remove("CoralineCoopSpectatorSession");p.getPersistentData().remove("CoralineInTunnel");p.getPersistentData().remove("CoralineReturning");p.getPersistentData().putUUID("CoralineCoopReturnedSession",n.getUUID("Id"));
            }finally{p.getPersistentData().remove("CoralineRecovering");}
            try{p.server.getPlayerList().saveAll();var proof=com.coraline.rebirth.StoryRunStore.read(file).getCompound("ForgeData");if(!proof.hasUUID("CoralineCoopReturnedSession")||!proof.getUUID("CoralineCoopReturnedSession").equals(n.getUUID("Id")))return false;}catch(java.io.IOException|RuntimeException unavailable){return false;}
            member.putBoolean("ReturnApplied",true);setDirty();
        }
        // Releasing controls/position does not bypass the accepted route's
        // inventory condition. The immutable old-session witness can finish
        // this later without teleporting the returned player again.
        if(GroupInventoryEscrow.held(p)&&!GroupInventoryEscrow.restore(p))return false;
        if(GroupInventoryEscrow.held(p))return false;
        members.remove(p.getUUID().toString());membership.remove(p.getUUID());setDirty();return checkpoint(p.server);
    }
    public boolean debugPause(ServerPlayer operator,ServerPlayer owner,boolean pause){
        if(!operator.hasPermissions(2)||operator.server!=owner.server)return false;var n=current(owner.getUUID());var scene=VisitData.get(owner.server).scenes.get(owner.getUUID());if(n==null||scene==null)return false;
        if(pause){n.putBoolean("DebugPaused",true);pause(owner.server,owner.getUUID());return checkpoint(owner.server);}
        n.remove("DebugPaused");setDirty();return observe(owner,scene)&&checkpoint(owner.server);
    }
    public void pause(MinecraftServer server,UUID owner){if(!thread(server))return;var n=current(owner);if(n!=null&&!n.getBoolean("Paused")){n.putBoolean("Paused",true);n.putLong("PausedAt",server.overworld().getGameTime());n.remove("ClockShift");setDirty();}}
    /** Only a committed native True Heart victory may release unfinished story escrows. */
    public boolean prepareCompleteEnding(MinecraftServer server,UUID generation,UUID reset){
        if(!thread(server)||!com.coraline.postgame.TrueHeartBattle.authorizes(server,generation,reset))return false;
        boolean changed=false;for(var n:sessions.values()){
            if(n.hasUUID("CompleteEnding")&&n.getUUID("CompleteEnding").equals(reset))continue;
            var scene=VisitData.get(server).scenes.get(n.getUUID("Owner"));var mall=n.getCompound("MallState");if(mall.isEmpty()&&scene!=null&&scene.door.equals(n.getUUID("Door"))&&scene.loop.activity==com.coraline.feast.OtherMotherActivities.Activity.SHOPPING_MALL&&scene.loop.phase==com.coraline.feast.OtherMotherLoopState.Phase.ACTIVITY)mall=scene.loop.expedition;
            if(!mall.isEmpty()){
                if(!n.hasUUID("ActivityId"))n.putUUID("ActivityId",UUID.randomUUID());var bags=mall.getCompound("BuyerBags");var legacy=mall.getList("ShoppingBag",Tag.TAG_COMPOUND);if(!legacy.isEmpty()){var own=bags.getList(n.getUUID("Owner").toString(),Tag.TAG_COMPOUND);for(var raw:legacy)own.add(raw.copy());bags.put(n.getUUID("Owner").toString(),own);}
                for(String buyer:bags.getAllKeys()){if(!uuidKey(buyer))return false;var items=new ArrayList<net.minecraft.world.item.ItemStack>();for(var raw:bags.getList(buyer,Tag.TAG_COMPOUND))items.add(net.minecraft.world.item.ItemStack.of((CompoundTag)raw));if(!directed(n,UUID.fromString(buyer),"mall_bag",items))return false;}
                mall.remove("ShoppingBag");mall.remove("BuyerBags");mall.putBoolean("Paid",true);n.put("MallState",mall);
            }
            changed=true;n.putUUID("CompleteEnding",reset);n.putBoolean("Paused",true);
            for(String id:n.getCompound("Members").getAllKeys())if(!id.equals(n.getUUID("Owner").toString()))n.getCompound("Members").getCompound(id).putString("State","RETURNING");
        }if(!changed)return true;setDirty();return checkpoint(server);
    }
    public CompoundTag endingReturn(UUID player){var n=memberSession(player);if(n!=null)return n.getCompound("Members").getCompound(player.toString()).getCompound("Return").copy();var own=current(player);var out=new CompoundTag();if(own!=null){var m=own.getCompound("Members").getCompound(player.toString());if(m.hasUUID("SpectatorActivity"))out.putInt("Mode",m.getInt("SpectatorMode"));}return out;}
    public CompoundTag endingEscrow(UUID player){var n=memberSession(player);if(n!=null)return n.getCompound("Members").getCompound(player.toString()).getCompound("AcceptedInventory").getCompound("InventoryEscrow").copy();return new CompoundTag();}
    public boolean finishEndingPlayer(MinecraftServer server,UUID player,UUID generation,UUID reset,CompoundTag nativePlayer){
        if(!thread(server)||!com.coraline.postgame.TrueHeartBattle.authorizes(server,generation,reset)||!nativePlayer.getCompound("ForgeData").hasUUID("CoralineCoopCompleteEnding")||!nativePlayer.getCompound("ForgeData").getUUID("CoralineCoopCompleteEnding").equals(reset)||nativePlayer.getCompound("ForgeData").getCompound("CoralineAcceptedInventoryEscrow").getBoolean("Stored"))return false;
        var n=memberSession(player);if(n!=null){var lane=n.getCompound("Members").getCompound(player.toString()).getCompound("AcceptedInventory");var snapshot=lane.getCompound("InventoryEscrow");var proof=nativePlayer.getCompound("ForgeData").getCompound("CoralineAcceptedInventoryEscrow");if(lane.getBoolean("EscrowStored")&&(!proof.getBoolean("Restored")||!proof.hasUUID("RestoredTransaction")||!snapshot.hasUUID("Transaction")||!snapshot.getUUID("Transaction").equals(proof.getUUID("RestoredTransaction"))||!proof.getCompound("Snapshot").equals(snapshot)))return false;n.getCompound("Members").remove(player.toString());membership.remove(player);}
        var own=current(player);if(own!=null){var m=own.getCompound("Members").getCompound(player.toString());m.remove("SpectatorActivity");m.remove("SpectatorMode");m.putString("State","ACTIVE");own.putBoolean("Archived",true);own.putBoolean("Paused",true);currentSessions.remove(player);}
        setDirty();return checkpoint(server);
    }
    @Override public CompoundTag save(CompoundTag root){root.putInt("Version",2);root.putBoolean("Blocked",blocked);var rows=new ListTag();sessions.values().forEach(n->rows.add(n.copy()));root.put("Sessions",rows);return root;}
}
