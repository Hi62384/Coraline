package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Independent of VisitData, family identities and the story-run reset transaction. */
public final class TycoonData extends SavedData {
    public final Map<UUID,Home> homes=new LinkedHashMap<>();
    private int next;
    public static TycoonData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TycoonData::load,TycoonData::new,"coraline_tycoon");}
    public Home home(UUID id){var h=homes.get(id);if(h==null){if(next>=65536)throw new IllegalStateException("Personal world allocation is full");h=new Home(id,next++);homes.put(id,h);setDirty();}return h;}
    public boolean canRetireHomes(Set<UUID> owners){if(owners==null||next<0||next>65536)return false;long required=owners.size();return required<=65536-next;}
    /** Complete-ending adapter. Ordinary replay never calls it. Old parcels
     * remain physically intact but are no longer part of the active simulation.
     * A new slot and capability epoch prevent old items from reopening them. */
    public boolean retireHomes(MinecraftServer server,UUID reset,Set<UUID> owners){
        if(!TycoonReceiptRecovery.readyForGlobalReset(server)||!server.isSameThread()||reset==null||owners==null||owners.size()>4096||owners.stream().anyMatch(java.util.Objects::isNull))return false;
        for(var player:server.getPlayerList().getPlayers())if(owners.contains(player.getUUID())&&player.serverLevel().dimension().equals(TycoonWorld.KEY))return false;
        int required=0;for(var owner:owners){var home=homes.get(owner);if(home==null||!home.state.hasUUID("CompleteReset")||!home.state.getUUID("CompleteReset").equals(reset))required++;}
        if(next<0||required>65536-next)return false;
        var retiring=owners.stream().map(homes::get).filter(java.util.Objects::nonNull).filter(h->!h.state.hasUUID("CompleteReset")||!h.state.getUUID("CompleteReset").equals(reset)).toList();if(!TycoonRetiredParcels.get(server).capture(server,retiring))return false;
        var replacements=new LinkedHashMap<UUID,Home>();int after=next;for(var owner:owners){var old=homes.get(owner);if(old!=null&&old.state.hasUUID("CompleteReset")&&old.state.getUUID("CompleteReset").equals(reset))continue;var home=new Home(owner,after++);long award=old==null?0:old.state.getLong("DoorAwardRun");try{var status=com.coraline.rebirth.StoryRunStore.status(server,owner);award=Math.max(award,status.complete()?status.run():status.run()-1);}catch(java.io.IOException ambiguous){return false;}home.state.putLong("DoorAwardRun",award);home.state.putUUID("CompleteReset",reset);home.state.putUUID("CapabilityEpoch",reset);replacements.put(owner,home);}
        homes.putAll(replacements);next=after;setDirty();
        try{var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/coraline_tycoon.dat");if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;server.overworld().getDataStorage().save();return com.coraline.rebirth.StoryRunStore.read(file).getCompound("data").equals(save(new CompoundTag()));}catch(java.io.IOException|RuntimeException failed){return false;}
    }
    public static final class Home {
        public final UUID owner; public final int slot;
        public CompoundTag state=new CompoundTag();
        public final Map<UUID,Resident> residents=new LinkedHashMap<>();
        public Home(UUID id,int slot){this.owner=id;this.slot=slot;state.putInt("Reputation",50);state.putLong("NextInspection",72000);}
        public BlockPos center(){return new BlockPos((slot%256)*1024,100,(slot/256)*1024);}
        public boolean active(){return state.getBoolean("Activated");}
        public BlockPos entrance(){return state.contains("Entrance")?BlockPos.of(state.getLong("Entrance")):null;}
        public int reputation(){return Math.max(0,Math.min(100,state.getInt("Reputation")));}
        public void reputation(int delta){state.putInt("Reputation",Math.max(0,Math.min(100,reputation()+delta)));}
        public long clock(){return state.getLong("ActiveTicks");}
        public long cooldown(){return Math.max(0,state.getLong("PlacementAfter")-clock());}
        public long activeResidents(){return residents.values().stream().filter(Resident::active).count();}
        public int xp(){return Math.max(0,state.getInt("ProgressXP"));}
        public int level(){return 1+xp()/100;}
        public boolean milestone(String id,int xp){var m=state.getCompound("Milestones");if(m.getBoolean(id))return false;m.putBoolean(id,true);state.put("Milestones",m);state.putInt("ProgressXP",Math.min(1000000,xp()+Math.max(0,xp)));return true;}
        /** Future producers must supply a durable, unique receipt; Phase 1 has no producer. */
        public boolean creditSoul(String receipt,long amount){if(receipt.isBlank()||receipt.length()>100||amount<=0)return false;var receipts=state.getCompound("SoulReceipts");if(receipts.getBoolean(receipt))return false;long before=state.getLong("SoulEssence");if(before<0||amount>Long.MAX_VALUE-before)return false;receipts.putBoolean(receipt,true);state.put("SoulReceipts",receipts);state.putLong("SoulEssence",before+amount);return true;}
        public CompoundTag save(){var n=new CompoundTag();n.putUUID("Owner",owner);n.putInt("Slot",slot);n.put("State",state.copy());var children=new ListTag();residents.values().forEach(r->children.add(r.save()));n.put("Residents",children);return n;}
    }
    public static final class Resident {
        public final UUID id;public CompoundTag n;
        public Resident(UUID id,CompoundTag n){this.id=id;this.n=n;}
        public boolean active(){return n.getString("Status").equals("ACTIVE");}
        public String name(){return n.getString("Name");}
        public double need(String key){double value=n.getDouble(key);return Double.isFinite(value)?Math.max(0,Math.min(100,value)):0;}
        public void need(String key,double value){n.putDouble(key,Double.isFinite(value)?Math.max(0,Math.min(100,value)):0);}
        public double trait(String key){return TycoonPersonality.value(this,key);}
        public BlockPos bed(){return n.contains("Bed")?BlockPos.of(n.getLong("Bed")):null;}
        public CompoundTag save(){var out=n.copy();out.putUUID("Id",id);return out;}
    }
    public static TycoonData load(CompoundTag root){
        if(root.getInt("Version")!=1)throw new IllegalStateException("Unknown tycoon data version; refusing to reset ownership");
        var data=new TycoonData();var slots=new HashSet<Integer>();
        for(var tag:root.getList("Homes",10)){var n=(CompoundTag)tag;if(!n.hasUUID("Owner"))throw new IllegalStateException("Tycoon owner missing");int slot=n.getInt("Slot");if(slot<0||slot>=65536||!slots.add(slot))throw new IllegalStateException("Duplicate/invalid tycoon region");var h=new Home(n.getUUID("Owner"),slot);h.state=n.getCompound("State").copy();for(String key:List.of("CompleteReset","CapabilityEpoch"))if(h.state.contains(key)&&!h.state.hasUUID(key))throw new IllegalStateException("Invalid retired tycoon capability");for(var child:n.getList("Residents",10)){var c=(CompoundTag)child;if(!c.hasUUID("Id")||h.residents.put(c.getUUID("Id"),new Resident(c.getUUID("Id"),c.copy()))!=null)throw new IllegalStateException("Duplicate resident");}if(data.homes.put(h.owner,h)!=null)throw new IllegalStateException("Duplicate tycoon owner");data.next=Math.max(data.next,slot+1);}
        data.next=Math.max(data.next,root.getInt("Next"));return data;
    }
    @Override public CompoundTag save(CompoundTag root){root.putInt("Version",1);root.putInt("Next",next);var list=new ListTag();homes.values().forEach(h->list.add(h.save()));root.put("Homes",list);return root;}
}
