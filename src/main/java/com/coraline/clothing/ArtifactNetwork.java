package com.coraline.clothing;

import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Small, server-authoritative action channel for the capturer and projection controls. */
public final class ArtifactNetwork {
    public record SoulEntry(UUID id,String name,boolean loaded,int target,int follow,int task,int selected){}
    public record SoulMenu(List<SoulEntry> entries,int target,int follow){}
    public record SoulAction(UUID id,int action){}
    public record SoulMove(float strafe,float forward,boolean jump,boolean crouch,float yaw,float pitch){}
    public record SoulAttack(int entityId){}
    public record SoulAbility(int entityId){}
    public record SoulBlock(BlockPos pos,Direction face,boolean place){}
    public record SoulHotbar(List<ItemStack> slots,int selected){}
    public record ViewState(int mode,int camera){}
    public record DreamState(boolean active,int camera){}
    public record DreamExit(){}
    public record NeedleMenu(){}
    public record NeedleRepair(int slot){}
    public record DreamMove(float strafe,float forward,boolean rise,boolean descend,float yaw,float pitch){}
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","artifacts"),()->"4","4"::equals,"4"::equals);
    private ArtifactNetwork(){}
    public static void register(){
        CHANNEL.messageBuilder(SoulMenu.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.entries.size());for(var e:m.entries){b.writeUUID(e.id);b.writeUtf(e.name);b.writeBoolean(e.loaded);b.writeVarInt(e.target);b.writeVarInt(e.follow);b.writeVarInt(e.task);b.writeVarInt(e.selected);}b.writeVarInt(m.target);b.writeVarInt(m.follow);})
            .decoder(b->{int n=Math.min(10,b.readVarInt());List<SoulEntry> entries=new ArrayList<>();for(int i=0;i<n;i++)entries.add(new SoulEntry(b.readUUID(),b.readUtf(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt()));return new SoulMenu(entries,b.readVarInt(),b.readVarInt());})
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.SoulCapturerScreen.receive(m))).add();
        CHANNEL.messageBuilder(SoulAction.class,1,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeUUID(m.id);b.writeVarInt(m.action);}).decoder(b->new SoulAction(b.readUUID(),b.readVarInt()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)SoulCapturerItem.action(p,m.id,m.action);}).add();
        CHANNEL.messageBuilder(SoulMove.class,2,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeFloat(m.strafe);b.writeFloat(m.forward);b.writeBoolean(m.jump);b.writeBoolean(m.crouch);b.writeFloat(m.yaw);b.writeFloat(m.pitch);}).decoder(b->new SoulMove(b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean(),b.readFloat(),b.readFloat()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)SoulCapturerItem.move(p,m.strafe,m.forward,m.jump,m.crouch,m.yaw,m.pitch);}).add();
        CHANNEL.messageBuilder(ViewState.class,3,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.mode);b.writeVarInt(m.camera);}).decoder(b->new ViewState(b.readVarInt(),b.readVarInt()))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.ArtifactClientControls.soulMode(m.mode,m.camera))).add();
        CHANNEL.messageBuilder(DreamState.class,4,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeBoolean(m.active);b.writeVarInt(m.camera);}).decoder(b->new DreamState(b.readBoolean(),b.readVarInt()))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.ArtifactClientControls.dreamMode(m.active,m.camera))).add();
        CHANNEL.messageBuilder(DreamExit.class,5,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{}).decoder(b->new DreamExit())
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)DreamstoneItem.stop(p);}).add();
        CHANNEL.messageBuilder(NeedleMenu.class,6,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{}).decoder(b->new NeedleMenu())
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.NeedleRepairScreen.open())).add();
        CHANNEL.messageBuilder(NeedleRepair.class,7,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeVarInt(m.slot)).decoder(b->new NeedleRepair(b.readVarInt()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)BeldamSewingNeedle.repair(p,m.slot);}).add();
        CHANNEL.messageBuilder(DreamMove.class,8,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeFloat(m.strafe);b.writeFloat(m.forward);b.writeBoolean(m.rise);b.writeBoolean(m.descend);b.writeFloat(m.yaw);b.writeFloat(m.pitch);})
            .decoder(b->new DreamMove(b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean(),b.readFloat(),b.readFloat()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)DreamstoneItem.move(p,m.strafe,m.forward,m.rise,m.descend,m.yaw,m.pitch);}).add();
        CHANNEL.messageBuilder(SoulAttack.class,9,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeVarInt(m.entityId)).decoder(b->new SoulAttack(b.readVarInt()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)SoulCapturerItem.attack(p,m.entityId);}).add();
        CHANNEL.messageBuilder(SoulAbility.class,10,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeVarInt(m.entityId)).decoder(b->new SoulAbility(b.readVarInt()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)SoulCapturerItem.ability(p,m.entityId);}).add();
        CHANNEL.messageBuilder(SoulBlock.class,11,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeBlockPos(m.pos);b.writeEnum(m.face);b.writeBoolean(m.place);})
            .decoder(b->new SoulBlock(b.readBlockPos(),b.readEnum(Direction.class),b.readBoolean()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)SoulCapturerItem.block(p,m.pos,m.face,m.place);}).add();
        CHANNEL.messageBuilder(SoulHotbar.class,12,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.slots.size());for(ItemStack item:m.slots)b.writeItem(item);b.writeVarInt(m.selected);})
            .decoder(b->{int count=Math.min(9,b.readVarInt());List<ItemStack> slots=new ArrayList<>();for(int i=0;i<count;i++)slots.add(b.readItem());return new SoulHotbar(slots,b.readVarInt());})
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.ArtifactClientControls.hotbar(m.slots,m.selected))).add();
    }
    public static void openSouls(ServerPlayer player,ItemStack stack){
        var list=stack.getOrCreateTag().getList("CoralineCaptured",Tag.TAG_COMPOUND);var entries=new ArrayList<SoulEntry>();
        for(int i=0;i<list.size();i++){
            var record=list.getCompound(i);if(!record.hasUUID("Id"))continue;
            UUID id=record.getUUID("Id");var entity=player.serverLevel().getEntity(id);Mob mob=entity instanceof Mob m?m:null;
            entries.add(new SoulEntry(id,record.getString("Name"),mob!=null,record.getInt("Target"),record.getInt("Follow"),record.getInt("Task"),record.getInt("Selected")));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new SoulMenu(entries,stack.getOrCreateTag().getInt("SoulGlobalTarget"),stack.getOrCreateTag().getInt("SoulGlobalFollow")));
    }
    public static void soulView(ServerPlayer player,int mode,int camera){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ViewState(mode,camera));}
    public static void soulHotbar(ServerPlayer player,net.minecraft.nbt.CompoundTag record){
        List<ItemStack> slots=new ArrayList<>();for(int i=0;i<9;i++)slots.add(SoulInventory.get(record,i));
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new SoulHotbar(slots,record.getInt("Selected")));
    }
    public static void dreamState(ServerPlayer player,boolean active,int camera){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new DreamState(active,camera));}
    public static void action(UUID id,int action){CHANNEL.sendToServer(new SoulAction(id,action));}
    public static void move(float strafe,float forward,boolean jump,boolean crouch,float yaw,float pitch){CHANNEL.sendToServer(new SoulMove(strafe,forward,jump,crouch,yaw,pitch));}
    public static void soulAttack(int entityId){CHANNEL.sendToServer(new SoulAttack(entityId));}
    public static void soulAbility(int entityId){CHANNEL.sendToServer(new SoulAbility(entityId));}
    public static void soulBlock(BlockPos pos,Direction face,boolean place){CHANNEL.sendToServer(new SoulBlock(pos,face,place));}
    public static void exitDream(){CHANNEL.sendToServer(new DreamExit());}
    public static void openNeedle(ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new NeedleMenu());}
    public static void repairNeedle(int slot){CHANNEL.sendToServer(new NeedleRepair(slot));}
    public static void moveDream(float strafe,float forward,boolean rise,boolean descend,float yaw,float pitch){CHANNEL.sendToServer(new DreamMove(strafe,forward,rise,descend,yaw,pitch));}
}
