package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Immutable ownership of ordinary builds after a complete ending. No actors,
 * inventories, stock, adoption records or progression snapshots are replayed. */
public final class TycoonRetiredParcels extends SavedData {
    private static final String NAME="coraline_retired_tycoon_parcels";
    private final Map<Integer,UUID> owners=new TreeMap<>();private boolean blocked;
    private TycoonRetiredParcels(){}
    public static TycoonRetiredParcels get(MinecraftServer server){var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(TycoonRetiredParcels::load,()->{var d=new TycoonRetiredParcels();d.blocked=!java.nio.file.Files.notExists(file,java.nio.file.LinkOption.NOFOLLOW_LINKS);return d;},NAME);}
    private static TycoonRetiredParcels load(CompoundTag root){var d=new TycoonRetiredParcels();if(root.getInt("Version")!=1||root.getBoolean("Blocked")||!root.contains("Parcels",Tag.TAG_LIST)){d.blocked=true;return d;}var rows=root.getList("Parcels",Tag.TAG_COMPOUND);if(rows.size()!=((ListTag)root.get("Parcels")).size()||rows.size()>65536){d.blocked=true;return d;}for(var raw:rows){var n=(CompoundTag)raw;int slot=n.getInt("Slot");if(!n.contains("Slot",Tag.TAG_INT)||slot<0||slot>=65536||!n.hasUUID("Owner")||d.owners.putIfAbsent(slot,n.getUUID("Owner"))!=null){d.blocked=true;d.owners.clear();return d;}}return d;}
    public static BlockPos center(int slot){return new BlockPos((slot%256)*1024,100,(slot/256)*1024);}
    public boolean owns(UUID owner,int slot){return !blocked&&owner!=null&&owner.equals(owners.get(slot));}
    public boolean contains(UUID owner,BlockPos at){if(blocked||owner==null)return false;int x=Math.floorDiv(at.getX()+512,1024),z=Math.floorDiv(at.getZ()+512,1024);if(x<0||x>=256||z<0||z>=256)return false;int slot=x+z*256;var c=center(slot);return owns(owner,slot)&&Math.abs((long)at.getX()-c.getX())<=128&&Math.abs((long)at.getZ()-c.getZ())<=128;}
    public List<Integer> owned(UUID owner){return blocked?List.of():owners.entrySet().stream().filter(e->e.getValue().equals(owner)).map(Map.Entry::getKey).sorted(Comparator.reverseOrder()).toList();}
    public boolean canCapture(Collection<TycoonData.Home> homes){
        if(blocked)return false;int additions=0;for(var h:homes){if(h.slot<0||h.slot>=65536)return false;var prior=owners.get(h.slot);if(prior!=null&&!prior.equals(h.owner))return false;if(prior==null)additions++;}return additions<=65536-owners.size();
    }
    public boolean capture(MinecraftServer server,Collection<TycoonData.Home> homes){
        if(!canCapture(homes)||!server.isSameThread())return false;for(var home:homes){var prior=owners.get(home.slot);if(prior!=null&&!prior.equals(home.owner))return false;}
        for(var home:homes)owners.putIfAbsent(home.slot,home.owner);setDirty();
        try{var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/"+NAME+".dat");if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;server.overworld().getDataStorage().save();return com.coraline.rebirth.StoryRunStore.read(file).getCompound("data").equals(save(new CompoundTag()));}catch(java.io.IOException|RuntimeException failure){return false;}
    }
    public static boolean ordinaryAccess(ServerPlayer p,BlockPos at){return p.serverLevel().dimension()==TycoonWorld.KEY&&get(p.server).contains(p.getUUID(),at)&&!com.coraline.postgame.TrueHeartBattle.resetting(p.server);}
    @Override public CompoundTag save(CompoundTag root){root.putInt("Version",1);root.putBoolean("Blocked",blocked);var list=new ListTag();owners.forEach((slot,owner)->{var n=new CompoundTag();n.putInt("Slot",slot);n.putUUID("Owner",owner);list.add(n);});root.put("Parcels",list);return root;}
}
