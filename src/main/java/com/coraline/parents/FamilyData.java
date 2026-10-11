package com.coraline.parents;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent pair identity is independent of whether either parent is currently loaded. */
public final class FamilyData extends SavedData {
    public final Map<UUID,Family> families=new HashMap<>();
    public static FamilyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FamilyData::load,FamilyData::new,"coraline_families");
    }
    public static final class Family {
        public UUID id=UUID.randomUUID(),mom,dad,owner;
        public boolean dead;
        public int deathPhase;
        public boolean firstDeathMom;
        public long deathStartedAt;
        // Runtime audio reservation; recordings stop on restart, so this is not saved.
        public long dialogueUntil;
        public BlockPos bed,chest,momPos=BlockPos.ZERO,dadPos=BlockPos.ZERO;
        public String homeDim="minecraft:overworld",momDim="minecraft:overworld",dadDim="minecraft:overworld";
        public int momTask,dadTask,gardenPlots;
        public int momSkin=-1,dadSkin=-1;
        public long momDelivery,dadDelivery,lastBedClick;
        public boolean busy=true;
        public long availabilityChangesAt;
        public Family() { lastBedClick=-100000; }
        public boolean isAvailable(MinecraftServer server){
            long now=server.overworld().getGameTime();boolean changed=false;
            if(availabilityChangesAt==0){availabilityChangesAt=now+phaseDuration(server);changed=true;}
            // Use game ticks, not time of day: sleeping or /time cannot skip a busy period.
            while(now>=availabilityChangesAt){busy=!busy;availabilityChangesAt+=phaseDuration(server);changed=true;}
            if(changed)FamilyData.get(server).setDirty();
            return !dead&&!busy;
        }
        private long phaseDuration(MinecraftServer server){
            int min=busy?com.coraline.config.CoralineConfig.BUSY_MIN.get():com.coraline.config.CoralineConfig.AVAILABLE_MIN.get();
            int max=Math.max(min,busy?com.coraline.config.CoralineConfig.BUSY_MAX.get():com.coraline.config.CoralineConfig.AVAILABLE_MAX.get());
            return 1200L*(min+server.overworld().random.nextInt(max-min+1));
        }
        public CompoundTag save() {
            CompoundTag n=new CompoundTag(); n.putUUID("Id",id);
            if(mom!=null)n.putUUID("Mom",mom); if(dad!=null)n.putUUID("Dad",dad); if(owner!=null)n.putUUID("Owner",owner);
            n.putBoolean("Dead",dead); if(bed!=null)n.putLong("Bed",bed.asLong()); if(chest!=null)n.putLong("Chest",chest.asLong());
            n.putInt("DeathPhase",deathPhase);n.putBoolean("FirstDeathMom",firstDeathMom);n.putLong("DeathStartedAt",deathStartedAt);
            n.putString("HomeDimension",homeDim); n.putString("MomDimension",momDim); n.putString("DadDimension",dadDim);
            n.putLong("MomPosition",momPos.asLong());n.putLong("DadPosition",dadPos.asLong());
            n.putInt("MomTask",momTask);n.putInt("DadTask",dadTask);n.putInt("GardenPlots",gardenPlots);
            n.putInt("MomSkin",momSkin);n.putInt("DadSkin",dadSkin);
            n.putLong("MomDelivery",momDelivery);n.putLong("DadDelivery",dadDelivery);
            n.putBoolean("Busy",busy);n.putLong("AvailabilityChangesAt",availabilityChangesAt);return n;
        }
        public static Family load(CompoundTag n) {
            Family f=new Family(); f.id=n.getUUID("Id");if(n.hasUUID("Mom"))f.mom=n.getUUID("Mom");if(n.hasUUID("Dad"))f.dad=n.getUUID("Dad");if(n.hasUUID("Owner"))f.owner=n.getUUID("Owner");
            f.dead=n.getBoolean("Dead");if(n.contains("Bed"))f.bed=BlockPos.of(n.getLong("Bed"));if(n.contains("Chest"))f.chest=BlockPos.of(n.getLong("Chest"));
            f.deathPhase=n.getInt("DeathPhase");f.firstDeathMom=n.getBoolean("FirstDeathMom");f.deathStartedAt=n.getLong("DeathStartedAt");
            f.homeDim=n.getString("HomeDimension");f.momDim=n.getString("MomDimension");f.dadDim=n.getString("DadDimension");
            f.momPos=BlockPos.of(n.getLong("MomPosition"));f.dadPos=BlockPos.of(n.getLong("DadPosition"));
            f.momTask=n.getInt("MomTask");f.dadTask=n.getInt("DadTask");f.gardenPlots=n.getInt("GardenPlots");
            f.momSkin=n.contains("MomSkin")?n.getInt("MomSkin"):-1;f.dadSkin=n.contains("DadSkin")?n.getInt("DadSkin"):-1;
            f.momDelivery=n.getLong("MomDelivery");f.dadDelivery=n.getLong("DadDelivery");
            f.busy=!n.contains("Busy")||n.getBoolean("Busy");f.availabilityChangesAt=n.getLong("AvailabilityChangesAt");return f;
        }
    }
    public static ServerLevel level(MinecraftServer s,String id) {
        ResourceLocation key=ResourceLocation.tryParse(id);
        return key==null?null:s.getLevel(ResourceKey.create(Registries.DIMENSION,key));
    }
    public static FamilyData load(CompoundTag n) {
        FamilyData d=new FamilyData();for(Tag t:n.getList("Families",10)){Family f=Family.load((CompoundTag)t);d.families.put(f.id,f);}return d;
    }
    @Override public CompoundTag save(CompoundTag n) {
        ListTag list=new ListTag();families.values().forEach(f->list.add(f.save()));n.put("Families",list);return n;
    }
}
