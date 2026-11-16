package com.coraline.palace;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** Durable identity, not a spawn recipe. A missing/unloaded entity is never a
 * death or permission to make a replacement. Story death retains the identity
 * and stored original; this chapter has no resurrection transition. */
public final class PalaceWybieIdentity {
    public enum Fate { ALIVE, DEAD, LOST }
    private final UUID owner,door,entity;
    private final CompoundTag original;
    private Fate fate=Fate.ALIVE;
    private boolean stored;
    private String dimension,blocker="";
    private long position;
    private UUID lostWellSession;
    private PalaceWybieIdentity(UUID owner,UUID door,UUID entity,CompoundTag original){this.owner=owner;this.door=door;this.entity=entity;this.original=original;}
    public static PalaceWybieIdentity create(UUID owner,UUID door,UUID entity,String dimension,BlockPos position){
        Objects.requireNonNull(owner);Objects.requireNonNull(door);Objects.requireNonNull(entity);
        var state=new PalaceWybieIdentity(owner,door,entity,null);
        if(!state.remember(entity,dimension,position))throw new IllegalArgumentException("Invalid canonical Wybie location");
        return state;
    }
    public UUID entity(){return entity;}
    public Fate fate(){return fate;}
    public boolean stored(){return stored;}
    public String dimension(){return dimension;}
    public BlockPos position(){return BlockPos.of(position);}
    public String blocker(){return blocker;}
    public boolean matches(UUID id){return blocker.isEmpty()&&entity!=null&&entity.equals(id);}
    public boolean remember(UUID id,String dimension,BlockPos at){
        if(!matches(id)||at==null||dimension==null||ResourceLocation.tryParse(dimension)==null
                ||Math.abs((long)at.getX())>PalaceSitePlan.WORLD_LIMIT||Math.abs((long)at.getZ())>PalaceSitePlan.WORLD_LIMIT
                ||at.getY()< -2048||at.getY()>2047)return false;
        this.dimension=dimension;position=at.asLong();return true;
    }
    public boolean store(UUID id){if(!matches(id))return false;stored=true;return true;}
    public boolean activate(UUID id){if(!matches(id)||fate!=Fate.ALIVE)return false;stored=false;return true;}
    public boolean recordDeath(UUID id){if(!matches(id))return false;fate=Fate.DEAD;stored=true;return true;}
    /** A living original's witnessed fall is not resurrection or a spawn permit. */
    public boolean recordWellFall(UUID id,UUID session){if(!matches(id)||session==null||fate==Fate.DEAD||fate==Fate.LOST&&!session.equals(lostWellSession))return false;fate=Fate.LOST;lostWellSession=session;stored=true;return true;}
    public Optional<UUID> lostWellSession(){return Optional.ofNullable(lostWellSession);}
    public CompoundTag save(){
        if(!blocker.isEmpty())return original.copy();
        var n=original==null?new CompoundTag():original.copy();n.putInt("Version",1);n.putUUID("Owner",owner);n.putUUID("Door",door);n.putUUID("Entity",entity);
        n.putString("Fate",fate.name());n.putBoolean("Stored",stored);n.putString("Dimension",dimension);n.putLong("Position",position);if(lostWellSession!=null)n.putUUID("LostWellSession",lostWellSession);return n;
    }
    public static PalaceWybieIdentity load(UUID owner,UUID door,CompoundTag n){
        var s=new PalaceWybieIdentity(owner,door,n.hasUUID("Entity")?n.getUUID("Entity"):null,n.copy());
        try{
            if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Owner")||!owner.equals(n.getUUID("Owner"))
                    ||!n.hasUUID("Door")||!door.equals(n.getUUID("Door"))||s.entity==null)throw new IllegalArgumentException("Unknown/foreign canonical identity");
            if(!n.contains("Stored",Tag.TAG_BYTE)||n.getByte("Stored")<0||n.getByte("Stored")>1
                    ||!n.contains("Fate",Tag.TAG_STRING)||!n.contains("Dimension",Tag.TAG_STRING)||!n.contains("Position",Tag.TAG_LONG))throw new IllegalArgumentException("Malformed canonical record");
            s.fate=Fate.valueOf(n.getString("Fate"));s.stored=n.getBoolean("Stored");
            if(n.contains("LostWellSession")&&!n.hasUUID("LostWellSession"))throw new IllegalArgumentException("Malformed well-loss identity");
            s.lostWellSession=n.hasUUID("LostWellSession")?n.getUUID("LostWellSession"):null;
            if(s.fate!=Fate.ALIVE&&!s.stored||s.fate==Fate.LOST&&s.lostWellSession==null||!s.remember(s.entity,n.getString("Dimension"),BlockPos.of(n.getLong("Position"))))throw new IllegalArgumentException("Invalid canonical state/location");
        }catch(IllegalArgumentException bad){s.blocker=bad.getMessage();}
        return s;
    }
}
