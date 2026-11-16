package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.DoorBlockEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.FamilyData;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The permanent decayed entrance reuses the saved First Escape corridor, never the friendly loop. */
public final class StoryTunnel {
    public static boolean sealed(MinecraftServer server,java.util.UUID owner){var s=VisitData.get(server).scenes.get(owner);return ParentRescueStory.data(s).getCompound("Continuation").getBoolean("PermanentlySealed");}
    public static boolean permanent(MinecraftServer server,java.util.UUID owner){var s=VisitData.get(server).scenes.get(owner);return MissingParentsCat.tunnelRestored(s)&&!ParentRescueStory.started(s)&&!sealed(server,owner);}
    public static boolean restore(ServerPlayer p,VisitData.Scene s){
        if(s==null||!s.owner.equals(p.getUUID()))return false;
        var d=DoorData.get(p.server).doors.get(s.door);if(d==null||!s.owner.equals(d.owner))return false;
        var source=FamilyData.level(p.server,d.dimension);var other=p.server.getLevel(OtherWorld.KEY);
        if(source==null||other==null)return false;
        source.getChunkAt(d.pos);var state=source.getBlockState(d.pos);
        if(!state.is(ModBlocks.TINY_DOOR.get())||!(source.getBlockEntity(d.pos) instanceof DoorBlockEntity block)
            ||block.doorId!=null&&!s.door.equals(block.doorId))return false;
        var r=RealmData.get(p.server).realms.get(s.door);
        if(r==null||!r.ready||r.house==null||r.doorPos==null){
            OtherWorld.prepareStoryRealm(p,d);
            MissingParentsCat.data(s).putString("Blocker","Preparing your saved Other World passage automatically.");
            return false;
        }
        var n=FirstEscapeStory.data(s);n.putLong("House",r.house.asLong());n.putLong("LittleDoor",r.doorPos.asLong());
        if(!n.contains("Prison"))n.putLong("Prison",r.house.above(80).asLong());
        if(!MissingParentsCat.tunnelRestored(s)||!n.getBoolean("EscapeTunnelBuilt")){n.putBoolean("EscapeTunnelBuilt",false);FirstEscapeStory.ensureDecayedTunnel(p,s);}
        if(!n.getBoolean("EscapeTunnelBuilt"))return false;
        var behind=d.pos.relative(state.getValue(TinyDoorBlock.FACING).getOpposite());
        if(source.getBlockState(behind).is(Blocks.BRICKS)&&n.contains("BrickOriginal"))source.setBlock(behind,NbtUtils.readBlockState(source.holderLookup(net.minecraft.core.registries.Registries.BLOCK),n.getCompound("BrickOriginal")),2);
        n.putBoolean("DecayedTunnel",true);
        block.doorId=s.door;block.storyBricked=false;block.storyDecayed=true;
        MissingParentsCat.data(s).putBoolean("TunnelRestored",true);
        source.setBlock(d.pos,state.setValue(TinyDoorBlock.PORTAL,true).setValue(TinyDoorBlock.OPEN,true),3);
        block.refreshStoryPassageBlock();block.setChanged();source.sendBlockUpdated(d.pos,source.getBlockState(d.pos),source.getBlockState(d.pos),3);
        OtherMotherLoop.changed(p);return true;
    }
    public static void retireCrawl(ServerPlayer p,VisitData.Scene s){
        var original=FirstEscapeStory.data(s);if(!original.contains("EscapeTunnel"))return;
        var level=p.server.getLevel(OtherWorld.KEY);if(level==null)return;
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        for(var scope:java.util.List.of(original,ParentRescueStory.scenery(s))){
            var cells=scope.getCompound("Environment");
            for(int z=0;z<=22;z++)for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++){
                var at=FirstEscapeStory.tunnelCell(s,x,y,z);String key=Long.toString(at.asLong());if(!cells.contains(key))continue;
                level.getChunkAt(at);var saved=cells.getCompound(key);
                if(level.getBlockEntity(at)==null&&level.getBlockState(at).equals(NbtUtils.readBlockState(lookup,saved.getCompound("Placed"))))level.setBlock(at,NbtUtils.readBlockState(lookup,saved.getCompound("Original")),2);
                cells.remove(key);
            }
        }
        original.putBoolean("EscapeTunnelBuilt",false);OtherMotherLoop.changed(p);
    }
    public static boolean enter(ServerPlayer p,DoorBlockEntity door){var s=OtherMotherLoop.scene(p);if(s==null||!MissingParentsCat.tunnelRestored(s)||door.doorId==null||!door.doorId.equals(s.door))return false;if(sealed(p.server,s.owner))return true;if(!MissingParentsCat.data(s).getBoolean("Complete")){
            if(!MissingParentsCat.finishForReturn(p,s)){FirstEscapeStory.hint(p,"The Cat is still revealing the passage.");return true;}
        }if(!ParentRescueStory.started(s))ParentRescueStory.begin(p,s);return true;}
    public static void tickSource(ServerPlayer p,VisitData.Scene s){if(MissingParentsCat.tunnelRestored(s)&&!ParentRescueStory.started(s)&&p.serverLevel()==p.server.overworld())TunnelPassage.tick(p);}
    public static void crawl(ServerPlayer p,VisitData.Scene s,double progress){FirstEscapeStory.ensureDecayedTunnel(p,s);var l=p.server.getLevel(OtherWorld.KEY);var n=FirstEscapeStory.data(s);var origin=BlockPos.of(n.getLong("EscapeTunnel"));var direction=FirstEscapeStory.escapeDirection(s);var at=Vec3.atBottomCenterOf(origin).add(direction.getStepX()*progress,0,direction.getStepZ()*progress);l.getChunkAt(BlockPos.containing(at));p.closeContainer();p.stopRiding();p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);p.teleportTo(l,at.x,at.y,at.z,direction.toYRot(),0);}
    private StoryTunnel(){}
}
