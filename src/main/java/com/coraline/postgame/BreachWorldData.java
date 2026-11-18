package com.coraline.postgame;

import java.nio.file.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** One global fuel bank and Breach journal. Slot removal and fuel credit share
 * one SavedData snapshot; there is no per-block/per-player reserve to copy.
 * Reading this store cannot activate Breach or charge a player's inventory. */
public final class BreachWorldData extends SavedData {
    private static final String NAME="coraline_overworld_breach";
    private BreachState state;
    private boolean unconfirmed;
    private Tag drive,inputOpaque,avatar;
    private boolean loading;
    private final SimpleContainer input=new SimpleContainer(1){@Override public void setChanged(){super.setChanged();if(!loading)BreachWorldData.this.setDirty();}};
    public record View(boolean active,int millionths,long fuelTicks,long lastGameTick,boolean avatarLocked,boolean containmentEnabled,int dailyMillionths,String blocker){}
    private BreachWorldData(){state=BreachState.fresh();}
    private BreachWorldData(CompoundTag n){
        state=BreachState.load(n);drive=n.contains("Drive")?n.get("Drive").copy():null;avatar=n.contains("PhysicalAvatar")?n.get("PhysicalAvatar").copy():null;loading=true;
        if(n.contains("DriveInput")){
            var raw=n.get("DriveInput");
            if(raw instanceof CompoundTag saved&&saved.isEmpty()){}
            else if(raw instanceof CompoundTag saved){var stack=ItemStack.of(saved);if(acceptsFuel(stack)&&stack.getCount()<=64)input.setItem(0,stack);else inputOpaque=raw.copy();}
            else inputOpaque=raw.copy();
        }
        loading=false;
    }
    private static Path file(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");}
    public static BreachWorldData get(MinecraftServer server){
        if(!server.isSameThread())throw new IllegalStateException("Shared Breach storage must be accessed on the server thread");
        return server.overworld().getDataStorage().computeIfAbsent(BreachWorldData::new,()->Files.notExists(file(server),LinkOption.NOFOLLOW_LINKS)?new BreachWorldData():new BreachWorldData(new CompoundTag()),NAME);
    }
    public View view(){
        String blocker=problem();boolean active=blocker.isEmpty()&&state.active();
        return new View(active,active?state.value():0,active?state.fuel():0,active?state.lastTick():-1,active&&state.avatarLocked(),active&&state.containmentEnabled(),active?(state.powered()?state.rules().poweredRate():state.rules().unpoweredRate()):0,blocker);
    }
    private String problem(){
        if(unconfirmed)return "Breach persistence checkpoint is unconfirmed";if(!state.blocker().isEmpty())return state.blocker();if(inputOpaque!=null)return "Unrecognized Moonstone bank data";
        if(drive!=null&&!validDrive())return "Unrecognized Breach Drive binding";if(state.active()&&drive==null)return "Active Breach has no authoritative Drive binding";
        if(avatar!=null&&!validAvatar())return "Unrecognized White Void Avatar encounter";if(state.avatarEntity().isPresent()&&avatar==null)return "Avatar identity has no physical encounter record";return "";
    }
    private boolean validDrive(){
        if(!(drive instanceof CompoundTag n)||!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Binding")||!n.hasUUID("Generation")||!n.contains("Position",Tag.TAG_LONG)||!n.contains("Dimension",Tag.TAG_STRING)||!n.getString("Dimension").equals("minecraft:overworld"))return false;
        var at=BlockPos.of(n.getLong("Position"));return Math.abs((long)at.getX())<=29999984&&Math.abs((long)at.getZ())<=29999984&&(!state.active()||state.generation().orElseThrow().equals(n.getUUID("Generation")));
    }
    static boolean acceptsFuel(ItemStack stack){return !stack.isEmpty()&&stack.is(com.coraline.clothing.OtherMaterials.MOONSTONE.get())&&(!stack.hasTag()||stack.getTag().isEmpty());}
    net.minecraft.world.Container input(){return input;}
    boolean hasDriveReceipt(){return drive!=null;}
    Optional<BlockPos> drivePosition(){return validDrive()?Optional.of(BlockPos.of(((CompoundTag)drive).getLong("Position"))):Optional.empty();}
    Optional<UUID> driveId(){return validDrive()?Optional.of(((CompoundTag)drive).getUUID("Binding")):Optional.empty();}
    Optional<UUID> driveGeneration(){return validDrive()?Optional.of(((CompoundTag)drive).getUUID("Generation")):Optional.empty();}
    boolean matchesDrive(ServerLevel level,BlockPos at,BreachDriveBlockEntity entity){
        if(level!=level.getServer().overworld()||!problem().isEmpty()||!validDrive()||!drivePosition().orElseThrow().equals(at)||level.getBlockEntity(at)!=entity)return false;
        var epoch=InfiniteWorldData.get(level.getServer()).state().generation();
        return epoch.isPresent()&&epoch.orElseThrow().equals(driveGeneration().orElseThrow())&&entity.matches(driveId().orElseThrow(),epoch.orElseThrow());
    }
    private boolean owns(ServerLevel level){
        if(!problem().isEmpty()||!level.getServer().isSameThread()||!(level.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;
        try{
            var bound=generator.structureGeneration();var canonical=InfiniteWorldData.get(level.getServer()).state();
            return canonical.active()&&canonical.generation().orElseThrow().equals(bound.id())&&canonical.seed().orElseThrow()==bound.seed()&&(!state.active()||state.generation().orElseThrow().equals(bound.id()));
        }catch(IllegalStateException unavailable){return false;}
    }
    /** Future physical activation must attest a real durably bound level.
     * Neither the existing main-story ending nor a debug command calls this. */
    boolean activateFromBoundLevel(ServerLevel level){
        if(!owns(level)||!state.blocker().isEmpty())return false;var server=level.getServer();
        var durable=InfiniteGeneration.checkpoint(server);if(durable.isEmpty())return false;
        var expected=((InfiniteChunkGenerator)level.getChunkSource().getGenerator()).structureGeneration();
        if(!durable.orElseThrow().id().equals(expected.id())||durable.orElseThrow().seed()!=expected.seed()||driveGeneration().isEmpty()||!driveGeneration().orElseThrow().equals(expected.id()))return false;
        var at=drivePosition().orElseThrow();var overworld=server.overworld();if(!overworld.hasChunkAt(at)||!(overworld.getBlockEntity(at) instanceof BreachDriveBlockEntity device)||!matchesDrive(overworld,at,device))return false;
        if(state.active())return true;
        var candidate=BreachState.load(state.save());
        if(!candidate.activate(expected.id(),server.overworld().getGameTime(),BreachConfig.rules()))return false;
        state=candidate;return checkpoint(server);
    }
    /** Persist the single location before placing anything. */
    boolean claimDrive(ServerLevel bound,BlockPos at){
        if(!owns(bound)||drive!=null||state.active()||!bound.getServer().overworld().getWorldBorder().isWithinBounds(at))return false;
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Binding",UUID.randomUUID());n.putUUID("Generation",((InfiniteChunkGenerator)bound.getChunkSource().getGenerator()).structureGeneration().id());n.putLong("Position",at.asLong());n.putString("Dimension","minecraft:overworld");drive=n;return checkpoint(bound.getServer());
    }
    boolean relocateUninstalledDrive(ServerLevel bound,BlockPos old,BlockPos replacement){
        var level=bound.getServer().overworld();
        if(!owns(bound)||state.active()||!drivePosition().filter(old::equals).isPresent()
                ||!BreachDriveController.obstructed(level,old)||!BreachDriveController.clear(level,replacement))return false;
        var record=((CompoundTag)drive).copy();
        record.putLong("Position",replacement.asLong());
        drive=record;
        return checkpoint(bound.getServer());
    }
    /** Vanilla menu transfers put real items in this one shared slot. The
     * slot and its resulting fuel are persisted together, never in a chunk. */
    boolean consumeInput(ServerLevel bound){
        if(!owns(bound)||!state.active()||!acceptsFuel(input.getItem(0)))return false;var stack=input.getItem(0);
        var next=BreachState.load(state.save());if(!next.addFuel(stack.getCount(),bound.getServer().overworld().getGameTime()))return false;
        state=next;input.setItem(0,ItemStack.EMPTY);setDirty();return true;
    }
    BreachState.Resolution resolveDrill(ServerLevel bound,RiftMachineEntity drill){
        if(!owns(bound)||!state.active()||drill.getLevel()!=bound||!drill.isCore()||!drill.disabled()||!drill.authentic()||drill.completion()==null)return BreachState.Resolution.REFUSED;
        advanceFromBoundLevel(bound);consumeInput(bound);
        var outcome=state.complete(BreachState.Kind.RIFT_DRILL,drill.site(),drill.completion(),100000,bound.getServer().overworld().getGameTime());
        if(outcome!=BreachState.Resolution.REFUSED&&outcome!=BreachState.Resolution.ALREADY_RESOLVED&&outcome!=BreachState.Resolution.CONFLICT)setDirty();return outcome;
    }
    BreachState.Resolution resolveHeart(ServerLevel bound,BoundaryDefenseEntity heart){
        if(!owns(bound)||!state.active()||heart.getLevel()!=bound||!heart.majorHeart()||!heart.complete()||heart.site()==null||heart.completion()==null)return BreachState.Resolution.REFUSED;
        advanceFromBoundLevel(bound);consumeInput(bound);
        var outcome=state.complete(BreachState.Kind.HEART_CHAMBER,heart.site(),heart.completion(),250000,bound.getServer().overworld().getGameTime());
        if(outcome!=BreachState.Resolution.REFUSED&&outcome!=BreachState.Resolution.ALREADY_RESOLVED&&outcome!=BreachState.Resolution.CONFLICT)setDirty();return outcome;
    }
    BreachState.Resolution resolveQuest(ServerLevel bound,ServerPlayer owner){
        var proof=PostgameQuests.handedIn(owner);if(!owns(bound)||!state.active()||proof==null)return BreachState.Resolution.REFUSED;
        advanceFromBoundLevel(bound);consumeInput(bound);var kind=proof.getString("Giver").equals("coraline")?BreachState.Kind.CORALINE_QUEST:BreachState.Kind.WYBIE_QUEST;
        var outcome=state.complete(kind,proof.getUUID("Objective"),proof.getUUID("Claim"),100000,bound.getServer().overworld().getGameTime());if(outcome!=BreachState.Resolution.REFUSED&&outcome!=BreachState.Resolution.ALREADY_RESOLVED&&outcome!=BreachState.Resolution.CONFLICT)setDirty();return outcome;
    }
    BreachState.Resolution resolveWybieRescue(ServerLevel bound,ServerPlayer owner){
        var receipt=WybieRescue.completion(owner);var objective=WybieRescue.objective(owner);
        if(!owns(bound)||!state.active()||receipt.isEmpty()||objective==null)return BreachState.Resolution.REFUSED;
        advanceFromBoundLevel(bound);consumeInput(bound);
        var outcome=state.complete(BreachState.Kind.WYBIE_QUEST,objective,receipt.orElseThrow(),250000,bound.getServer().overworld().getGameTime());
        if(outcome!=BreachState.Resolution.REFUSED&&outcome!=BreachState.Resolution.ALREADY_RESOLVED&&outcome!=BreachState.Resolution.CONFLICT)setDirty();return outcome;
    }
    BreachState.Resolution resolveSeam(ServerLevel bound,SeamAnchorEntity seam){
        if(!owns(bound)||!state.active()||seam.getLevel()!=bound||!seam.closed()||!seam.authentic()||seam.site()==null||seam.completion()==null)return BreachState.Resolution.REFUSED;
        advanceFromBoundLevel(bound);consumeInput(bound);
        var outcome=state.complete(BreachState.Kind.DIMENSIONAL_SEAM,seam.site(),seam.completion(),150000,bound.getServer().overworld().getGameTime());
        if(outcome!=BreachState.Resolution.REFUSED&&outcome!=BreachState.Resolution.ALREADY_RESOLVED&&outcome!=BreachState.Resolution.CONFLICT)setDirty();return outcome;
    }
    /** In-memory game-tick advancement only; normal SavedData autosave handles
     * this store. No wall time and no per-tick forced disk writes. */
    boolean advanceFromBoundLevel(ServerLevel level){
        if(!owns(level)||!state.active())return false;long now=level.getServer().overworld().getGameTime();
        if(now<state.lastTick())return false;
        var rules=BreachConfig.rules();boolean changed=now!=state.lastTick()||!state.rules().equals(rules);
        if(!state.configure(rules,now))return false;if(changed)setDirty();return true;
    }
    private boolean checkpoint(MinecraftServer server){
        try{
            if(Files.isSymbolicLink(file(server))||Files.isSymbolicLink(file(server).getParent()))throw new java.io.IOException("Ambiguous Breach storage path");
            var expected=save(new CompoundTag());setDirty();server.overworld().getDataStorage().save();
            var disk=com.coraline.rebirth.StoryRunStore.read(file(server));
            if(!disk.contains("data",net.minecraft.nbt.Tag.TAG_COMPOUND)||!disk.getCompound("data").equals(expected)||!BreachState.load(disk.getCompound("data")).blocker().isEmpty())throw new java.io.IOException("Breach checkpoint did not match");
            return true;
        }catch(java.io.IOException|RuntimeException failed){unconfirmed=true;return false;}
    }
    boolean retireFromEnding(MinecraftServer server,UUID generation,UUID reset){
        if(!TrueHeartBattle.authorizes(server,generation,reset)||!problem().isEmpty())return false;var current=state.save();if(current.hasUUID("CompleteReset")&&reset.equals(current.getUUID("CompleteReset"))){if(!checkpoint(server))return false;cleanupRetiredDrives(server);return true;}if(!state.active()||state.generation().filter(generation::equals).isEmpty())return false;
        if(current.getList("RetiredDrives",net.minecraft.nbt.Tag.TAG_COMPOUND).size()>=1024)return false;var position=drivePosition().orElse(null);var binding=driveId().orElse(null);loading=true;input.clearContent();loading=false;inputOpaque=null;avatar=null;drive=null;var fresh=BreachState.fresh().save();var retired=current.getList("RetiredDrives",net.minecraft.nbt.Tag.TAG_COMPOUND).copy();if(retired.size()>=1024)return false;if(position!=null&&binding!=null){var device=new CompoundTag();device.putUUID("Generation",generation);device.putUUID("Binding",binding);device.putUUID("Reset",reset);device.putLong("Position",position.asLong());retired.add(device);}fresh.put("RetiredDrives",retired);fresh.putUUID("CompleteReset",reset);state=BreachState.load(fresh);if(!checkpoint(server))return false;
        if(position!=null&&binding!=null&&server.overworld().hasChunkAt(position)&&server.overworld().getBlockEntity(position) instanceof BreachDriveBlockEntity device&&device.matches(binding,generation))server.overworld().removeBlock(position,false);return true;
    }
    private void cleanupRetiredDrives(MinecraftServer server){for(var tag:state.save().getList("RetiredDrives",net.minecraft.nbt.Tag.TAG_COMPOUND)){var row=(CompoundTag)tag;var at=BlockPos.of(row.getLong("Position"));if(server.overworld().hasChunkAt(at)&&server.overworld().getBlockEntity(at) instanceof BreachDriveBlockEntity device)retireMarkedDrive(device);}}
    void retireMarkedDrive(BreachDriveBlockEntity device){if(!(device.getLevel() instanceof ServerLevel level)||level!=level.getServer().overworld()||!problem().isEmpty())return;for(var tag:state.save().getList("RetiredDrives",net.minecraft.nbt.Tag.TAG_COMPOUND)){var row=(CompoundTag)tag;if(!row.hasUUID("Generation")||!row.hasUUID("Binding")||!row.hasUUID("Reset")||!row.contains("Position",net.minecraft.nbt.Tag.TAG_LONG))return;if(row.getLong("Position")==device.getBlockPos().asLong()&&device.matches(row.getUUID("Binding"),row.getUUID("Generation"))&&(InfiniteWorldData.get(level.getServer()).state().retired(row.getUUID("Generation"))||TrueHeartBattle.authorizes(level.getServer(),row.getUUID("Generation"),row.getUUID("Reset")))){level.removeBlock(device.getBlockPos(),false);return;}}}
    private boolean validAvatar(){
        if(!(avatar instanceof CompoundTag n)||!state.active()||!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Generation")||!n.getUUID("Generation").equals(state.generation().orElseThrow())||!n.hasUUID("Entity")||!n.hasUUID("Seal")||!n.hasUUID("Victory")||!n.contains("Home",Tag.TAG_LONG)||!n.contains("Body",Tag.TAG_COMPOUND)||!n.contains("Summons",Tag.TAG_LIST)||!n.contains("TotalSummons",Tag.TAG_INT)||n.getInt("TotalSummons")<0||n.getInt("TotalSummons")>24)return false;
        var home=BlockPos.of(n.getLong("Home"));if(Math.abs((long)home.getX())>29999980||Math.abs((long)home.getZ())>29999980||home.getY()< -60||home.getY()>310)return false;
        var body=n.getCompound("Body");if(!body.getString("id").equals("coraline:white_void_avatar")||!body.hasUUID("UUID")||!body.getUUID("UUID").equals(n.getUUID("Entity")))return false;
        var list=n.getList("Summons",Tag.TAG_COMPOUND);if(list.size()>24||list.size()!=((net.minecraft.nbt.ListTag)n.get("Summons")).size())return false;var seen=new HashSet<UUID>();
        for(var raw:list){var row=(CompoundTag)raw;if(!row.hasUUID("Entity")||!seen.add(row.getUUID("Entity"))||!row.contains("Released",Tag.TAG_BYTE)||row.getByte("Released")<0||row.getByte("Released")>1)return false;}
        return n.getString("Phase").equals("ACTIVE")?state.avatarLocked()&&state.avatarEntity().filter(n.getUUID("Entity")::equals).isPresent():n.getString("Phase").equals("DEFEATED")&&state.avatarVictory(n.getUUID("Entity"),n.getUUID("Victory"));
    }
    Optional<UUID> avatarId(){return problem().isEmpty()&&avatar instanceof CompoundTag n&&n.getString("Phase").equals("ACTIVE")?Optional.of(n.getUUID("Entity")):Optional.empty();}
    Optional<UUID> generation(){return problem().isEmpty()?state.generation():Optional.empty();}
    Optional<BlockPos> avatarHome(){return avatarId().isPresent()?Optional.of(BlockPos.of(((CompoundTag)avatar).getLong("Home"))):Optional.empty();}
    CompoundTag avatarBody(){return avatarId().isPresent()?((CompoundTag)avatar).getCompound("Body").copy():new CompoundTag();}
    boolean admitAvatar(WhiteVoidAvatarEntity entity){
        if(!problem().isEmpty()||!state.avatarLocked()||state.avatarEntity().isPresent()||entity.getServer()==null||entity.generation()==null||entity.seal()==null||entity.level()!=entity.getServer().overworld()||!state.generation().filter(entity.generation()::equals).isPresent())return false;
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Generation",entity.generation());n.putUUID("Entity",entity.getUUID());n.putUUID("Seal",entity.seal());n.putUUID("Victory",UUID.randomUUID());n.putLong("Home",entity.home().asLong());n.putString("Phase","ACTIVE");var body=new CompoundTag();if(!entity.save(body))return false;n.put("Body",body);n.put("Summons",new net.minecraft.nbt.ListTag());n.putInt("TotalSummons",0);
        var next=BreachState.load(state.save());if(!next.bindAvatar(entity.getUUID(),entity.getServer().overworld().getGameTime()))return false;state=next;avatar=n;return checkpoint(entity.getServer());
    }
    boolean authentic(WhiteVoidAvatarEntity entity){
        return !entity.level().isClientSide&&entity.getServer()!=null&&!TrueHeartBattle.resetting(entity.getServer())&&entity.level()==entity.getServer().overworld()&&avatarId().filter(entity.getUUID()::equals).isPresent()&&entity.generation()!=null&&entity.generation().equals(((CompoundTag)avatar).getUUID("Generation"))&&entity.seal()!=null&&entity.seal().equals(((CompoundTag)avatar).getUUID("Seal"))&&entity.home().asLong()==((CompoundTag)avatar).getLong("Home");
    }
    boolean defeated(WhiteVoidAvatarEntity entity){return problem().isEmpty()&&avatar instanceof CompoundTag n&&n.getString("Phase").equals("DEFEATED")&&n.getUUID("Entity").equals(entity.getUUID())&&entity.seal()!=null&&n.getUUID("Seal").equals(entity.seal())&&state.avatarVictory(entity.getUUID(),n.getUUID("Victory"));}
    void rememberAvatar(WhiteVoidAvatarEntity entity){if(authentic(entity)){var body=new CompoundTag();if(entity.save(body)){((CompoundTag)avatar).put("Body",body);setDirty();}}}
    boolean avatarDefeated(WhiteVoidAvatarEntity entity){
        if(!authentic(entity)||entity.getHealth()>0)return false;var n=((CompoundTag)avatar).copy();var next=BreachState.load(state.save());
        if(!next.avatarDefeated(entity.getUUID(),n.getUUID("Victory"),entity.getServer().overworld().getGameTime()))return false;
        n.putString("Phase","DEFEATED");state=next;avatar=n;return checkpoint(entity.getServer());
    }
    boolean admitSummons(WhiteVoidAvatarEntity owner,List<UUID> ids){
        if(!authentic(owner)||ids.isEmpty()||ids.size()>3)return false;var n=(CompoundTag)avatar;var list=n.getList("Summons",Tag.TAG_COMPOUND);int active=0;var known=new HashSet<UUID>();for(var raw:list){var row=(CompoundTag)raw;known.add(row.getUUID("Entity"));if(!row.getBoolean("Released"))active++;}
        if(active+ids.size()>8||n.getInt("TotalSummons")+ids.size()>24||new HashSet<>(ids).size()!=ids.size()||ids.stream().anyMatch(known::contains))return false;
        for(var id:ids){var row=new CompoundTag();row.putUUID("Entity",id);row.putBoolean("Released",false);list.add(row);}n.putInt("TotalSummons",n.getInt("TotalSummons")+ids.size());return checkpoint(owner.getServer());
    }
    boolean ownsSummon(UUID entity,UUID owner,UUID generation){
        if(!problem().isEmpty()||avatarId().filter(owner::equals).isEmpty()||!((CompoundTag)avatar).getUUID("Generation").equals(generation))return false;
        for(var raw:((CompoundTag)avatar).getList("Summons",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;if(row.getUUID("Entity").equals(entity)&&!row.getBoolean("Released"))return true;}return false;
    }
    void releaseSummon(UUID entity,UUID owner){if(avatar instanceof CompoundTag n&&n.hasUUID("Entity")&&n.getUUID("Entity").equals(owner))for(var raw:n.getList("Summons",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;if(row.getUUID("Entity").equals(entity)){row.putBoolean("Released",true);setDirty();}}}
    @Override public CompoundTag save(CompoundTag target){target.merge(state.save());if(drive!=null)target.put("Drive",drive.copy());if(avatar!=null)target.put("PhysicalAvatar",avatar.copy());target.put("DriveInput",inputOpaque!=null?inputOpaque.copy():input.getItem(0).isEmpty()?new CompoundTag():input.getItem(0).save(new CompoundTag()));return target;}
}
