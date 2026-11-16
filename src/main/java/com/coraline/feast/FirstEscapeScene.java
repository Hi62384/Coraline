package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.*;

/** The parlor occupies the actual Little Door room; all non-BE edits are journalled. */
public final class FirstEscapeScene {
    static CompoundTag context(VisitData.Scene s){return AcceptedButtonEyesProgress.active(s)?AcceptedButtonEyesProgress.scenery(s):ParentRescueStory.started(s)?ParentRescueStory.scenery(s):FirstEscapeStory.data(s);}
    public static BlockPos at(VisitData.Scene s,int x,int y,int z){var n=context(s);return HouseCabinets.at(BlockPos.of(n.getLong("House")),x,y,z);}
    public static BlockPos room(VisitData.Scene s){return at(s,22,6,44);}
    public static BlockPos entrance(VisitData.Scene s){return at(s,31,6,41);}
    public static BlockPos mirror(VisitData.Scene s){return at(s,33,6,53);}
    public static BlockPos porch(VisitData.Scene s){return at(s,34,6,10);}
    // Include the authored inset return at z=32..36, not only the cleared prop area.
    public static BlockPos roomMin(VisitData.Scene s){return at(s,14,6,32);}
    public static BlockPos roomMax(VisitData.Scene s){return at(s,31,13,52);}
    public static boolean inRoom(VisitData.Scene s,BlockPos p){return new AABB(roomMin(s),roomMax(s)).contains(p.getCenter());}
    public static boolean fullyInRoom(VisitData.Scene s,AABB body){var bounds=new AABB(roomMin(s),roomMax(s));return bounds.contains(new Vec3(body.minX+.0001,body.minY+.0001,body.minZ+.0001))&&bounds.contains(new Vec3(body.maxX-.0001,body.maxY-.0001,body.maxZ-.0001));}
    public static boolean inHouseOrPorch(VisitData.Scene s,BlockPos p){var house=BlockPos.of(context(s).getLong("House"));if(!OtherWorldEstate.outsideHouse(house,p))return true;var front=porch(s);return Math.abs(p.getX()-front.getX())<5&&Math.abs(p.getZ()-front.getZ())<5&&Math.abs(p.getY()-front.getY())<3;}
    private static ServerLevel level(ServerPlayer p){return p.server.getLevel(OtherWorld.KEY);}
    static void place(ServerPlayer p,VisitData.Scene s,BlockPos at,BlockState state){var l=level(p);if(l==null||l.getBlockEntity(at)!=null)return;var n=context(s);var cells=n.getCompound("Environment");String key=Long.toString(at.asLong());if(!cells.contains(key)){var cell=new CompoundTag();cell.put("Original",NbtUtils.writeBlockState(l.getBlockState(at)));cells.put(key,cell);}cells.getCompound(key).put("Placed",NbtUtils.writeBlockState(state));n.put("Environment",cells);l.setBlock(at,state,2);}
    public static void prepareRoom(ServerPlayer p,VisitData.Scene s){var n=context(s);if(n.getBoolean("RoomBuilt"))return;var c=room(s);var l=level(p);l.getChunkAt(c);
        for(int x=14;x<=30;x++)for(int z=37;z<=51;z++)for(int y=6;y<=12;y++){
            var pos=at(s,x,y,z);if(l.getBlockEntity(pos)!=null)continue;
            place(p,s,pos,Blocks.AIR.defaultBlockState());
        }
        // Replace the existing plaster, never build a second wall inside the room.
        // Glass, timber framing, doorways and the lower panelling remain intact.
        for(int x=13;x<=31;x++)for(int z=32;z<=52;z++)for(int y=6;y<=12;y++){
            var pos=at(s,x,y,z);var old=l.getBlockState(pos);
            if(plaster(old))place(p,s,pos,com.coraline.registry.ModBlocks.INSECT_PARLOR_WALLPAPER.get().defaultBlockState());
        }
        n.putBoolean("RoomBuilt",true);OtherMotherLoop.changed(p);
        var hidden=new ListTag();for(var display:l.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,new AABB(c).inflate(12),e->inRoom(s,e.blockPosition())&&e.getPersistentData().hasUUID("SecretOwner")&&s.owner.equals(e.getPersistentData().getUUID("SecretOwner")))){var saved=new CompoundTag();saved.putUUID("Id",display.getUUID());saved.putBoolean("Invisible",display.isInvisible());hidden.add(saved);display.setInvisible(true);}n.put("HiddenSpies",hidden);
    }
    public static BlockPos prison(VisitData.Scene s){return BlockPos.of(context(s).getLong("Prison"));}
    /** Rescue stays on the mirror's authored hall floor, never a basement/roof fallback. */
    static Vec3 hallLanding(ServerPlayer p,VisitData.Scene s,Vec3 avoid){
        var center=mirror(s).north(2);var l=level(p);if(l==null)return null;
        for(int radius=0;radius<=3;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;var at=center.offset(x,0,z);l.getChunkAt(at);
            if(!FirstEscapeCarry.fits(p,at))continue;var feet=FirstEscapeCarry.feet(p,at);
            if(avoid!=null&&feet.distanceToSqr(avoid)<1.44)continue;
            return feet;
        }
        return null;
    }
    private static boolean plaster(BlockState state){return state.is(Blocks.PINK_CONCRETE)||state.is(Blocks.WHITE_CONCRETE)||state.is(Blocks.WHITE_TERRACOTTA);}
    public static void light(ServerPlayer p,VisitData.Scene s,BlockPos at,int brightness){var l=level(p);if(l==null)return;var old=l.getBlockState(at);var next=Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,brightness);if(!old.equals(next)&&(old.isAir()||old.is(Blocks.LIGHT)))place(p,s,at,next);}
    public static void followLight(ServerPlayer p,VisitData.Scene s,String key,Vec3 source,int brightness){
        var n=context(s);var at=BlockPos.containing(source);String saved=key+"LightPosition";
        if(n.contains(saved)&&n.getLong(saved)!=at.asLong())light(p,s,BlockPos.of(n.getLong(saved)),0);
        light(p,s,at,brightness);n.putLong(saved,at.asLong());
    }
    public static void preparePrison(ServerPlayer p,VisitData.Scene s){var n=context(s);if(n.getBoolean("PrisonBuilt"))return;var c=prison(s);var l=level(p);l.getChunkAt(c);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=5;y++){boolean shell=Math.abs(x)==4||Math.abs(z)==4||y==-1||y==5;place(p,s,c.offset(x,y,z),(shell?Blocks.DEEPSLATE_BRICKS:Blocks.AIR).defaultBlockState());}place(p,s,c.north(4).above(),Blocks.PACKED_ICE.defaultBlockState());place(p,s,c.north(4).above(2),Blocks.PACKED_ICE.defaultBlockState());n.putBoolean("PrisonBuilt",true);}
    public static CinematicPropEntity prop(ServerPlayer p,VisitData.Scene s,String key){var n=context(s);if(!n.hasUUID(key))return null;for(var l:p.server.getAllLevels())if(l.getEntity(n.getUUID(key)) instanceof CinematicPropEntity e&&e.owned(s))return e;return null;}
    static void reconcileOriginalGhostReferences(ServerPlayer p,VisitData.Scene s){
        var original=FirstEscapeStory.data(s);var current=context(s);boolean changed=false;
        for(int slot=0;slot<3;slot++){
            String key="Ghost"+slot;if(!original.hasUUID(key))continue;var id=original.getUUID(key);
            if(current.hasUUID(key)&&id.equals(current.getUUID(key)))continue;
            if(current.hasUUID(key)){var retired=s.loop.story.getCompound("RetiredCanonicalGhosts");retired.putInt(current.getUUID(key).toString(),slot);s.loop.story.put("RetiredCanonicalGhosts",retired);}
            current.putUUID(key,id);if(original.contains(key+"Position"))current.putLong(key+"Position",original.getLong(key+"Position"));changed=true;
        }
        if(changed)OtherMotherLoop.changed(p);
    }
    public static CinematicPropEntity ensure(ServerPlayer p,VisitData.Scene s,String key,int kind,int slot,ServerLevel l,Vec3 at){var n=context(s);
        if(kind==CinematicPropEntity.GHOST_CHILD&&key.equals("Ghost"+slot)){
            if(RescueEyes.ledger(s).getBoolean("Released"))return null;
            var original=FirstEscapeStory.data(s);var id=RescueEpilogue.savedGhostReference(original,n,slot);
            if(id!=null&&(!n.hasUUID(key)||!id.equals(n.getUUID(key)))){
                // A boss-loss branch reuses the three original prison actors.
                // Keep a tombstone for an older branch's recorded duplicate.
                if(n.hasUUID(key)){var retired=s.loop.story.getCompound("RetiredCanonicalGhosts");retired.putInt(n.getUUID(key).toString(),slot);s.loop.story.put("RetiredCanonicalGhosts",retired);}
                n.putUUID(key,id);if(original.contains(key+"Position"))n.putLong(key+"Position",original.getLong(key+"Position"));OtherMotherLoop.changed(p);
            }
        }
        var e=prop(p,s,key);if(e!=null){n.putLong(key+"Position",e.blockPosition().asLong());return e;}if(n.hasUUID(key)){if(n.contains(key+"Position"))l.getChunkAt(BlockPos.of(n.getLong(key+"Position")));// An unloaded UUID is not permission to clone.
            return null;}
        l.getChunkAt(BlockPos.containing(at));e=CinematicPropEntity.TYPE.get().create(l);if(e==null)return null;e.configure(s,kind,slot);e.setPos(at);if(!l.addFreshEntity(e))return null;n.putUUID(key,e.getUUID());n.putLong(key+"Position",e.blockPosition().asLong());OtherMotherLoop.changed(p);return e;}
    public static void roomProps(ServerPlayer p,VisitData.Scene s){var l=level(p);var c=Vec3.atBottomCenterOf(room(s));var n=context(s);
        if(n.getBoolean("RoomBuilt")&&n.getInt("WallpaperRevision")<2){
            // The authored room has inset returns, not four straight walls. Its
            // east wall is terracotta; treating only concrete left it bare.
            for(int x=13;x<=31;x++)for(int z=32;z<=52;z++)for(int y=6;y<=12;y++){
                var pos=at(s,x,y,z);var old=l.getBlockState(pos);
                if(plaster(old))place(p,s,pos,com.coraline.registry.ModBlocks.INSECT_PARLOR_WALLPAPER.get().defaultBlockState());
            }
            n.putBoolean("WallpaperApplied",true);
            n.putInt("WallpaperRevision",2);
        }
        ensure(p,s,"Parlor",CinematicPropEntity.INSECT_PARLOR,0,l,c);
        var door=BlockPos.of(n.getLong("LittleDoor"));var wardrobe=ensure(p,s,"Wardrobe",CinematicPropEntity.ROACH_WARDROBE,0,l,Vec3.atBottomCenterOf(door).add(2,0,3));if(wardrobe!=null)wardrobe.setYRot(90);
        var couch=ensure(p,s,"BugCouch",CinematicPropEntity.BUG_COUCH,1,l,c.add(3,0,3));
        if(couch!=null){var toTable=c.add(1,0,2).subtract(couch.position());couch.setYRot((float)Math.toDegrees(Math.atan2(toTable.x,-toTable.z)));}
        ensure(p,s,"BugChair",CinematicPropEntity.BUG_CHAIR,0,l,c.add(4,0,-3));
        ensure(p,s,"BugTable",CinematicPropEntity.BUG_TABLE,0,l,c.add(1,0,2));ensure(p,s,"BeetleBox",CinematicPropEntity.BEETLE_BOX,0,l,c.add(1,.75,2));
        ensure(p,s,"BugLamp",CinematicPropEntity.BUG_LAMP,0,l,c.add(-5,0,-3));ensure(p,s,"Chandelier",CinematicPropEntity.BUG_CHANDELIER,0,l,c.add(0,4.8,0));
        removeHallMirror(p,s);
    }
    public static void removeHallMirror(ServerPlayer p,VisitData.Scene s){
        // The house's existing packed-ice wall is the mirror. Retire only our
        // redundant freestanding prop, not any authored wall or player block.
        var mirror=prop(p,s,"HallMirror");if(mirror!=null)mirror.discard();
        context(s).remove("HallMirror");context(s).remove("HallMirrorPosition");
    }
    public static void retirePrison(ServerPlayer p,VisitData.Scene s){
        var l=level(p);if(l==null)return;var c=prison(s);var cells=context(s).getCompound("Environment");var lookup=l.holderLookup(Registries.BLOCK);
        for(var at:BlockPos.betweenClosed(c.offset(-4,-1,-4),c.offset(4,5,4))){
            String key=Long.toString(at.asLong());if(!cells.contains(key))continue;var cell=cells.getCompound(key);l.getChunkAt(at);
            if(l.getBlockEntity(at)==null&&l.getBlockState(at).equals(NbtUtils.readBlockState(lookup,cell.getCompound("Placed")))){l.setBlock(at,NbtUtils.readBlockState(lookup,cell.getCompound("Original")),2);cells.remove(key);}
        }
        for(var prop:l.getEntitiesOfClass(CinematicPropEntity.class,new AABB(c.offset(-5,-2,-5),c.offset(5,7,5)),e->e.owned(s)&&!e.canonicalGhost(s)))prop.discard();
        context(s).remove("PrisonBuilt");OtherMotherLoop.changed(p);
    }
    public static void restore(ServerPlayer p,VisitData.Scene s){var l=level(p);if(l==null)return;for(var tag:context(s).getList("HiddenSpies",10)){var saved=(CompoundTag)tag;var entity=l.getEntity(saved.getUUID("Id"));if(entity!=null)entity.setInvisible(saved.getBoolean("Invisible"));}var cells=context(s).getCompound("Environment");var lookup=l.holderLookup(Registries.BLOCK);for(String key:cells.getAllKeys()){var at=BlockPos.of(Long.parseLong(key));var cell=cells.getCompound(key);if(l.getBlockEntity(at)==null&&l.getBlockState(at).equals(NbtUtils.readBlockState(lookup,cell.getCompound("Placed"))))l.setBlock(at,NbtUtils.readBlockState(lookup,cell.getCompound("Original")),2);} }
    private FirstEscapeScene(){}
}
