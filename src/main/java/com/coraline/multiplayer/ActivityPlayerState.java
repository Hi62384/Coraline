package com.coraline.multiplayer;

import com.coraline.feast.*;
import com.coraline.rebirth.StoryRunStore;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Actual-player restoration survives membership removal and offline return.
 * A guest never reads the director's abilities or potion inventory. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ActivityPlayerState {
    private static final String KEY="CoralineActivityPlayerState";
    public static boolean capture(ServerPlayer p,VisitData.Scene scene){
        var all=p.getPersistentData();var n=all.getCompound(KEY);
        if(!all.contains(KEY)){
            n.putUUID("Player",p.getUUID());n.putUUID("Owner",scene.owner);n.putUUID("Door",scene.door);
            n.putInt("Generation",scene.loop.generation);n.putString("Activity",scene.loop.activity.name());
            n.putBoolean("MayFly",p.getAbilities().mayfly);n.putBoolean("Flying",p.getAbilities().flying);
            n.putLong("StartedAt",p.server.overworld().getGameTime());var effects=new ListTag();
            for(var effect:p.getActiveEffects())effects.add(effect.save(new CompoundTag()));n.put("Effects",effects);all.put(KEY,n);
        }
        if(!bound(p,n,scene))return false;
        if(n.getBoolean("Ready"))return true;
        try{
            p.server.getPlayerList().saveAll();var nativeFile=StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));
            if(!n.equals(nativeFile.getCompound("ForgeData").getCompound(KEY)))return false;
            n.putBoolean("Ready",true);return true;
        }catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    private static boolean bound(ServerPlayer p,CompoundTag n,VisitData.Scene scene){
        return n.hasUUID("Player")&&p.getUUID().equals(n.getUUID("Player"))&&n.hasUUID("Owner")&&scene.owner.equals(n.getUUID("Owner"))
            &&n.hasUUID("Door")&&scene.door.equals(n.getUUID("Door"))&&n.getInt("Generation")==scene.loop.generation&&n.getString("Activity").equals(scene.loop.activity.name());
    }
    public static void restore(ServerPlayer p){
        var n=p.getPersistentData().getCompound(KEY);if(!n.hasUUID("Player")||!p.getUUID().equals(n.getUUID("Player")))return;
        if(!p.isSpectator()){p.getAbilities().mayfly=n.getBoolean("MayFly");p.getAbilities().flying=n.getBoolean("Flying")&&p.getAbilities().mayfly;p.onUpdateAbilities();}
        long elapsed=Math.max(0,p.server.overworld().getGameTime()-n.getLong("StartedAt"));
        var generated=p.getEffect(MobEffects.FIRE_RESISTANCE);
        if(n.getString("Activity").equals("OTHER_NETHER_FORTRESS")&&generated!=null&&generated.getAmplifier()==0&&!generated.isInfiniteDuration()&&generated.getDuration()<=220&&!generated.isVisible()&&!generated.showIcon())p.removeEffect(MobEffects.FIRE_RESISTANCE);
        for(var tag:n.getList("Effects",Tag.TAG_COMPOUND)){
            var old=MobEffectInstance.load((CompoundTag)tag);if(old==null)continue;
            int remaining=old.isInfiniteDuration()?-1:(int)Math.max(0,old.getDuration()-Math.min(Integer.MAX_VALUE,elapsed));if(remaining==0)continue;
            var kept=p.getEffect(old.getEffect());
            if(kept==null||kept.getAmplifier()<old.getAmplifier()||kept.getAmplifier()==old.getAmplifier()&&!kept.isInfiniteDuration()&&(remaining==-1||kept.getDuration()<remaining))
                p.addEffect(new MobEffectInstance(old.getEffect(),remaining,old.getAmplifier(),old.isAmbient(),old.isVisible(),old.showIcon()));
        }
        p.getPersistentData().remove(KEY);
    }
    @SubscribeEvent public static void recover(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0||!p.getPersistentData().contains(KEY))return;
        var scene=OtherMotherLoop.active(p);var n=p.getPersistentData().getCompound(KEY);
        if(scene==null||!bound(p,n,scene)||!ActivityWorld.isActivity(p.level())||p.isSpectator()||scene.loop.expedition.getInt("Phase")>=3)restore(p);
    }
    private ActivityPlayerState(){}
}
