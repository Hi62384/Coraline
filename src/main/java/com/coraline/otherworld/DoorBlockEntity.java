package com.coraline.otherworld;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public final class DoorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public UUID doorId;
    public boolean passageBuilt;
    public boolean storyDecayed;
    public boolean storyBricked;
    /** Derived from the owner's current story phase; synced, never saved as a permanent seal. */
    public boolean storyPassageBlocked;
    public void refreshStoryPassageBlock(){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server))return;
        boolean blocked=false;
        if(doorId!=null&&com.coraline.otherworld.OtherWorld.isOther(server)&&!hasPostgameBinding()){
            var record=DoorData.get(server.getServer()).doors.get(doorId);
            var scene=record==null?null:com.coraline.feast.VisitData.get(server.getServer()).scenes.get(record.owner);
            blocked=com.coraline.feast.FirstEscapeStory.engaged(scene)&&scene.door.equals(doorId)
                &&com.coraline.feast.FirstEscapeStory.data(scene).contains("LittleDoor")
                &&worldPosition.asLong()==com.coraline.feast.FirstEscapeStory.data(scene).getLong("LittleDoor")
                &&com.coraline.feast.FirstEscapeTimeline.wardrobeBlocksPassage(com.coraline.feast.FirstEscapeStory.phase(scene));
        }
        if(doorId!=null&&!com.coraline.otherworld.OtherWorld.isOther(server)&&!hasPostgameBinding()){
            var record=DoorData.get(server.getServer()).doors.get(doorId);var scene=record==null?null:com.coraline.feast.VisitData.get(server.getServer()).scenes.get(record.owner);
            blocked=scene!=null&&record.pos.equals(worldPosition)&&com.coraline.feast.FirstEscapeStory.sealed(server.getServer(),record.owner)&&!com.coraline.feast.MissingParentsCat.tunnelRestored(scene);
            if(blocked&&getBlockState().getValue(com.coraline.block.TinyDoorBlock.OPEN))server.setBlock(worldPosition,getBlockState().setValue(com.coraline.block.TinyDoorBlock.OPEN,false),3);
        }
        if(storyPassageBlocked==blocked)return;
        storyPassageBlocked=blocked;setChanged();server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    private net.minecraft.nbt.Tag postgameBinding;
    public boolean hasPostgameBinding(){return postgameBinding!=null;}
    public UUID postgameGeneration(){try{return postgameBinding==null?null:net.minecraft.nbt.NbtUtils.loadUUID(postgameBinding);}catch(IllegalArgumentException invalid){return null;}}
    public boolean bindPostgame(UUID generation){if(postgameBinding!=null){if(generation.equals(postgameGeneration()))return true;if(!(level instanceof net.minecraft.server.level.ServerLevel server)||doorId!=null||!com.coraline.postgame.InfiniteWorldData.get(server.getServer()).state().retired(postgameGeneration())||!com.coraline.postgame.InfiniteWorldData.get(server.getServer()).matchesGeneration(generation))return false;}if(doorId!=null)return false;postgameBinding=net.minecraft.nbt.NbtUtils.createUUID(generation);setChanged();return true;}
    public void extinguishRetiredPostgame(){if(!(level instanceof net.minecraft.server.level.ServerLevel server)||postgameGeneration()==null||!com.coraline.postgame.InfiniteWorldData.get(server.getServer()).state().retired(postgameGeneration()))return;var state=getBlockState();if(state.hasProperty(com.coraline.block.TinyDoorBlock.PORTAL)){var next=state.setValue(com.coraline.block.TinyDoorBlock.PORTAL,false).setValue(com.coraline.block.TinyDoorBlock.OPEN,false);if(!state.equals(next))server.setBlock(worldPosition,next,3);}}
    @Override public void onLoad(){super.onLoad();if(level instanceof net.minecraft.server.level.ServerLevel server){Runnable refresh=()->{extinguishRetiredPostgame();refreshStoryPassageBlock();};if(server.getServer().isSameThread())refresh.run();else server.getServer().execute(refresh);}}
    /** Operator appearance preview only; deliberately absent from saved chunk data. */
    public int tunnelPreview;
    public int passageVersion;
    public net.minecraft.nbt.ListTag passageOriginals=new net.minecraft.nbt.ListTag();
    public DoorBlockEntity(BlockPos pos,BlockState state){super(com.coraline.registry.ModBlockEntities.TINY_DOOR.get(),pos,state);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(doorId!=null)tag.putUUID("Door",doorId);if(postgameBinding!=null)tag.put("PostgameGeneration",postgameBinding.copy());tag.putBoolean("PassageBuilt",passageBuilt);tag.putBoolean("StoryDecayed",storyDecayed);tag.putBoolean("StoryBricked",storyBricked);tag.putInt("PassageVersion",passageVersion);tag.put("PassageOriginals",passageOriginals.copy());}
@Override public void load(CompoundTag tag){super.load(tag);postgameBinding=tag.contains("PostgameGeneration")?tag.get("PostgameGeneration").copy():null;tunnelPreview=tag.getInt("TunnelPreview");doorId=tag.hasUUID("Door")?tag.getUUID("Door"):null;passageBuilt=tag.getBoolean("PassageBuilt");storyDecayed=tag.getBoolean("StoryDecayed");storyBricked=tag.getBoolean("StoryBricked");storyPassageBlocked=tag.getBoolean("StoryPassageBlocked");passageVersion=tag.getInt("PassageVersion");passageOriginals=tag.getList("PassageOriginals",10).copy();}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).inflate(4);}
    @Override public CompoundTag getUpdateTag(){var n=saveWithoutMetadata();n.putInt("TunnelPreview",tunnelPreview);n.putBoolean("StoryPassageBlocked",storyPassageBlocked);return n;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
