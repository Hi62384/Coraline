package com.coraline.block.entity;

import com.coraline.item.DollItem;
import com.coraline.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class DollBlockEntity extends BlockEntity {
    private UUID ownerId;
    private String ownerName = "Player";
    private CompoundTag profile = new CompoundTag();
    private int bondGeneration;
    private long invalidHomeSince=-1;
    private int movementCountdown=100;
    public boolean moving;
    public static void serverTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,DollBlockEntity doll){
        if(!(level instanceof net.minecraft.server.level.ServerLevel sl)||doll.ownerId==null)return;
        if(com.coraline.otherworld.OtherWorld.isOther(level))return;
        if(level.getGameTime()%20==0){
            var data=com.coraline.doll.DollBondData.get(sl.getServer());var stack=doll.createDollStack();
            if(!data.current(stack)){doll.moving=true;level.removeBlock(pos,false);return;}
            data.placed(stack,sl,pos);
            if(sl.getServer().getPlayerList().getPlayer(doll.ownerId)==null)return;
            if(com.coraline.doll.DollHome.nearBed(sl,pos,doll.ownerId)){
                doll.invalidHomeSince=-1;data.satisfied(stack,sl.getServer().overworld().getGameTime(),false);
            }else{
                if(doll.invalidHomeSince<0)doll.invalidHomeSince=level.getGameTime();
                if(level.getGameTime()-doll.invalidHomeSince>=com.coraline.config.CoralineConfig.DOLL_INVALID_PLACEMENT_TICKS.get()){
                    doll.moving=true;level.removeBlock(pos,false);data.recall(data.bonds.get(doll.ownerId),sl);return;
                }
            }
        }
        int interval=com.coraline.config.CoralineConfig.DOLL_MOVE_INTERVAL.get();
        // Retry an eligible move while nobody is watching, rather than missing a whole minute's window.
        if(--doll.movementCountdown<=0){
            doll.movementCountdown=com.coraline.config.CoralineConfig.DOLL_MOVE_RETRY.get();
            if(level.random.nextDouble()<com.coraline.config.CoralineConfig.DOLL_MOVE_CHANCE.get()){
                doll.movementCountdown=interval;
                com.coraline.doll.DollMovement.tryMove(sl,pos,doll);
                if(!doll.moving)doll.movementCountdown=com.coraline.config.CoralineConfig.DOLL_MOVE_RETRY.get();
            }
        }
    }

    public com.mojang.authlib.GameProfile getProfile() {
        com.mojang.authlib.GameProfile saved = net.minecraft.nbt.NbtUtils.readGameProfile(profile);
        return saved != null ? saved : new com.mojang.authlib.GameProfile(ownerId, ownerName);
    }

    public DollBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOLL.get(), pos, state);
    }

    public void setOwner(UUID ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public ItemStack createDollStack() {
        ItemStack stack = new ItemStack(com.coraline.registry.ModItems.DOLL.get());
        CompoundTag owner = new CompoundTag();
        if (this.ownerId != null) {
            owner.putUUID(DollItem.OWNER_ID, this.ownerId);
        }
        owner.putString(DollItem.OWNER_NAME, this.ownerName);
        owner.put("Profile", profile.copy());
        owner.putInt("BondGeneration",bondGeneration);
        stack.addTagElement("BlockEntityTag", owner);
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.ownerId != null) {
            tag.putUUID(DollItem.OWNER_ID, this.ownerId);
        }
        tag.putString(DollItem.OWNER_NAME, this.ownerName);
        tag.put("Profile", profile.copy());
        tag.putInt("BondGeneration",bondGeneration);
        tag.putInt("MovementCountdown",movementCountdown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.profile = tag.getCompound("Profile").copy();
        this.bondGeneration=tag.getInt("BondGeneration");
        this.movementCountdown=tag.contains("MovementCountdown")?tag.getInt("MovementCountdown"):100;
        this.ownerId = tag.hasUUID(DollItem.OWNER_ID) ? tag.getUUID(DollItem.OWNER_ID) : null;
        this.ownerName = tag.contains(DollItem.OWNER_NAME) ? tag.getString(DollItem.OWNER_NAME) : "Player";
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
