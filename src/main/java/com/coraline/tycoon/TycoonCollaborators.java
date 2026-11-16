package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The actual root owner controls grants. Permission never transfers ownership,
 * creates a home, changes story progress, or authorizes an operator implicitly. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonCollaborators {
    private static final Map<MinecraftServer,Set<UUID>> BLOCKED=new WeakHashMap<>();
    public static boolean allowed(ServerPlayer actor,UUID owner){
        if(owner==null||!actor.server.isSameThread()||com.coraline.postgame.TrueHeartBattle.recoveryPending(actor)||com.coraline.postgame.TrueHeartBattle.resetting(actor.server))return false;
        if(owner.equals(actor.getUUID()))return true;
        if(BLOCKED.getOrDefault(actor.server,Set.of()).contains(owner))return false;
        var h=TycoonData.get(actor.server).homes.get(owner);if(h==null||!h.active())return false;
        var permits=h.state.getCompound("Collaborators");return permits.contains(actor.getUUID().toString(),Tag.TAG_BYTE)&&permits.getBoolean(actor.getUUID().toString());
    }
    public static boolean storageAllowed(ServerPlayer actor,TycoonBlockEntity block){
        if(!storage(block.kind())||!allowed(actor,block.owner))return false;
        if(block.owner.equals(actor.getUUID()))return true;
        var h=TycoonData.get(actor.server).homes.get(block.owner);if(h==null)return false;
        return h.state.hasUUID("CapabilityEpoch")?h.state.getUUID("CapabilityEpoch").equals(block.epoch):block.epoch==null;
    }
    public static boolean storage(String kind){return Set.of("orphan_fridge","other_world_fridge","storage_cabinet","toy_box").contains(kind);}
    private static boolean checkpoint(ServerPlayer owner,TycoonData.Home home){
        try{var file=owner.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/coraline_tycoon.dat");if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;
            var data=TycoonData.get(owner.server);data.setDirty();owner.server.overworld().getDataStorage().save();var saved=com.coraline.rebirth.StoryRunStore.read(file).getCompound("data");
            for(var raw:saved.getList("Homes",Tag.TAG_COMPOUND)){var n=(CompoundTag)raw;if(n.hasUUID("Owner")&&n.getUUID("Owner").equals(owner.getUUID()))return n.equals(home.save());}
        }catch(java.io.IOException|RuntimeException failure){return false;}return false;
    }
    public static int permit(ServerPlayer owner,UUID guest,boolean allow){
        if(!owner.server.isSameThread()||guest==null||owner.getUUID().equals(guest)||com.coraline.postgame.TrueHeartBattle.recoveryPending(owner))return 0;
        var data=TycoonData.get(owner.server);var home=data.homes.get(owner.getUUID());if(home==null||!home.active())return 0;
        if(!home.state.contains("Collaborators"))home.state.put("Collaborators",new CompoundTag());var grants=home.state.getCompound("Collaborators");
        if(allow){if(!grants.contains(guest.toString())&&grants.size()>=64){owner.sendSystemMessage(Component.literal("This household already has 64 collaborators."));return 0;}grants.putBoolean(guest.toString(),true);}else grants.remove(guest.toString());
        var audit=home.state.getList("AccessAudit",Tag.TAG_COMPOUND);var row=new CompoundTag();row.putUUID("Actor",owner.getUUID());row.putUUID("Target",guest);row.putString("Action",allow?"GRANT":"REVOKE");row.putLong("At",owner.server.overworld().getGameTime());if(audit.size()>=256)audit.remove(0);audit.add(row);home.state.put("AccessAudit",audit);data.setDirty();
        if(!checkpoint(owner,home)){BLOCKED.computeIfAbsent(owner.server,s->new HashSet<>()).add(owner.getUUID());owner.sendSystemMessage(Component.literal("Access checkpoint paused. Shared access is blocked until the owner saves the permission again."));return 0;}
        BLOCKED.computeIfAbsent(owner.server,s->new HashSet<>()).remove(owner.getUUID());
        if(!allow){var player=owner.server.getPlayerList().getPlayer(guest);if(player!=null){player.closeContainer();TycoonNetwork.clear(guest);}}
        owner.sendSystemMessage(Component.literal(allow?"Household collaboration access granted.":"Collaboration permission revoked."));return 1;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        var root=Commands.literal("coraline");var access=Commands.literal("tycoon_access");
        for(boolean grant:new boolean[]{true,false})access.then(Commands.literal(grant?"grant":"revoke").then(Commands.argument("player",UuidArgument.uuid()).executes(c->permit(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"player"),grant))));
        access.then(Commands.literal("leave").executes(c->TycoonCollaborationVisits.leave(c.getSource().getPlayerOrException())?1:0));
        access.then(Commands.literal("past_builds").executes(c->{var p=c.getSource().getPlayerOrException();var slots=TycoonRetiredParcels.get(p.server).owned(p.getUUID());p.sendSystemMessage(Component.literal(slots.isEmpty()?"No preserved past parcels are recorded.":"Your preserved parcel slots: "+slots.stream().limit(16).toList()+". Visit with /coraline tycoon_access visit_builds <slot>."));return 1;}));
        access.then(Commands.literal("visit_builds").then(Commands.argument("slot",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,65535)).executes(c->TycoonCollaborationVisits.visitPast(c.getSource().getPlayerOrException(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"slot")))));
        event.getDispatcher().register(root.then(access));
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){BLOCKED.remove(event.getServer());}
    private TycoonCollaborators(){}
}
