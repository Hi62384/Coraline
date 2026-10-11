package com.coraline.otherworld;

import com.coraline.registry.ModSounds;
import com.coraline.parents.ParentAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Durable scene milestones, independent of the copied realm and entity lifetime. */
@Mod.EventBusSubscriber(modid="coraline")
public final class SceneAudio extends SavedData {
    private CompoundTag state=new CompoundTag();
    public static SceneAudio get(net.minecraft.server.MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(n->{var d=new SceneAudio();d.state=n;return d;},SceneAudio::new,"coraline_scene_audio");}
    @Override public CompoundTag save(CompoundTag n){return state.copy();}
    public static void play(ServerPlayer p,SoundEvent sound){p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),SoundSource.RECORDS,p.getX(),p.getY(),p.getZ(),com.coraline.config.CoralineConfig.SCENE_VOLUME.get().floatValue(),1,p.getRandom().nextLong()));}
    private static int introTicks(){
        try(var in=SceneAudio.class.getResourceAsStream("/assets/coraline/scene_audio_lengths.json")){
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().get("mice_intro").getAsInt();
        }catch(Exception e){throw new IllegalStateException("Missing scene audio durations",e);}
    }
    public static void begin(ServerPlayer p,DoorData.Door door){
        var family=ParentAccess.family(p.server,p.getUUID());if(family==null)return;
        var data=get(p.server);String key="Intro_"+family.id;
        if(data.state.getBoolean("DebugSkip_"+family.id))return;
        long night=Math.floorDiv(p.serverLevel().getDayTime(),24000);
        String pendingKey="Pending_"+p.getUUID();
        if(data.state.contains(pendingKey)){
            var old=data.state.getCompound(pendingKey);
            if(old.contains("Night")&&old.getLong("Night")==night)return;
            unusedNightEnded(p.server,door);
        }
        if(data.state.getBoolean(key)&&(visited(p.server,p.getUUID(),family.id)||data.state.contains("IntroNight_"+family.id)&&data.state.getLong("IntroNight_"+family.id)==night)){OtherWorld.spawnMice(p.serverLevel(),p,door);return;}
        data.state.putBoolean(key,true);
        data.state.putLong("IntroNight_"+family.id,night);
        var pending=new CompoundTag();pending.putUUID("Door",door.id);pending.putUUID("Family",family.id);
        pending.putLong("Night",night);
        pending.putLong("Due",p.server.overworld().getGameTime()+introTicks()+com.coraline.config.CoralineConfig.MOUSE_INTRO_DELAY.get());
        data.state.put("Pending_"+p.getUUID(),pending);data.setDirty();play(p,ModSounds.MICE_INTRO.get());
    }
    public static void cancelPending(ServerPlayer p){var data=get(p.server);data.state.remove("Pending_"+p.getUUID());data.setDirty();}
    public static void skipFirstVisit(ServerPlayer p,java.util.UUID family){
        var data=get(p.server);data.state.remove("Pending_"+p.getUUID());
        data.state.putBoolean("DebugSkip_"+family,true);data.setDirty();
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(ModSounds.MICE_INTRO.getId(),SoundSource.RECORDS));
    }
    private static boolean visited(net.minecraft.server.MinecraftServer server,java.util.UUID owner,java.util.UUID family){
        var scene=com.coraline.feast.VisitData.get(server).scenes.get(owner);
        return get(server).state.getBoolean("Visited_"+family)||scene!=null&&family.equals(scene.family)&&scene.loop.visits>0;
    }
    public static void otherWorldEntered(ServerPlayer p){var family=ParentAccess.family(p.server,p.getUUID());if(family!=null){var data=get(p.server);data.state.putBoolean("Visited_"+family.id,true);data.setDirty();}}
    public static void unusedNightEnded(net.minecraft.server.MinecraftServer server,DoorData.Door door){
        var data=get(server);var family=ParentAccess.family(server,door.owner);
        if(family!=null&&!visited(server,door.owner,family.id)){data.state.remove("Intro_"+family.id);data.state.remove("IntroNight_"+family.id);}
        String key="Pending_"+door.owner;
        if(data.state.contains(key)&&data.state.getCompound(key).getUUID("Door").equals(door.id)){
            data.state.remove(key);var player=server.getPlayerList().getPlayer(door.owner);
            if(player!=null)player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(ModSounds.MICE_INTRO.getId(),SoundSource.RECORDS));
        }
        data.setDirty();
    }
    public static void endFamily(net.minecraft.server.MinecraftServer server,java.util.UUID owner,java.util.UUID family,java.util.Set<java.util.UUID> doors){
        var data=get(server);data.state.remove("Pending_"+owner);data.state.remove("Intro_"+family);
        data.state.remove("Visited_"+family);data.state.remove("IntroNight_"+family);
        data.state.remove("DebugSkip_"+family);
        for(String key:java.util.Set.copyOf(data.state.getAllKeys()))for(var door:doors)
            if(key.equals("Door_"+door)||(key.startsWith("Tunnel_")&&key.endsWith("_"+door)))data.state.remove(key);
        var player=server.getPlayerList().getPlayer(owner);
        if(player!=null)for(var sound:java.util.List.of(ModSounds.MICE_INTRO,ModSounds.MICE_CHASE,ModSounds.TUNNEL_ENTER,ModSounds.DOOR_PORTAL_OPEN))
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(sound.getId(),SoundSource.RECORDS));
        data.setDirty();
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        var data=get(p.server);String key="Pending_"+p.getUUID();if(!data.state.contains(key))return;
        var pending=data.state.getCompound(key);var family=ParentAccess.family(p.server,p.getUUID());
        var d=DoorData.get(p.server).doors.get(pending.getUUID("Door"));
        if(family==null||!family.id.equals(pending.getUUID("Family"))||d==null||!d.exists||!d.placed){data.state.remove(key);data.setDirty();return;}
        var source=com.coraline.parents.FamilyData.level(p.server,d.dimension);
        if(source!=null&&(!OtherWorld.night(source)||!pending.contains("Night")||pending.getLong("Night")!=Math.floorDiv(source.getDayTime(),24000))){unusedNightEnded(p.server,d);return;}
        if(p.server.overworld().getGameTime()<pending.getLong("Due"))return;
        if(p.tickCount%5!=0||!p.level().dimension().location().toString().equals(d.dimension))return;
        if(OtherWorld.mouseBlocker(p.serverLevel(),p,d)!=null)return;
        if(OtherWorld.spawnMice(p.serverLevel(),p,d)>0){data.state.remove(key);data.setDirty();}
    }
    public static void doorOpened(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){
        if(OtherWorld.isOther(level)||!(level.getBlockEntity(pos) instanceof DoorBlockEntity block))return;
        var d=DoorData.get(level.getServer()).doors.get(block.doorId);if(d==null||!OtherWorld.active(level,d))return;
        var data=get(level.getServer());String key="Door_"+d.id;long night=Math.floorDiv(level.getDayTime(),24000);
        if(data.state.contains(key)&&data.state.getLong(key)==night)return;
        data.state.putLong(key,night);data.setDirty();
        level.playSound(null,pos,ModSounds.DOOR_PORTAL_OPEN.get(),SoundSource.RECORDS,com.coraline.config.CoralineConfig.SCENE_VOLUME.get().floatValue(),1);
    }
    public static void tunnelEntered(ServerPlayer p,DoorData.Door d){
        var source=com.coraline.parents.FamilyData.level(p.server,d.dimension);if(source==null)return;
        var data=get(p.server);String key="Tunnel_"+p.getUUID()+"_"+d.id;long night=Math.floorDiv(source.getDayTime(),24000);
        if(data.state.contains(key)&&data.state.getLong(key)==night)return;
        data.state.putLong(key,night);data.setDirty();play(p,ModSounds.TUNNEL_ENTER.get());
    }
}
