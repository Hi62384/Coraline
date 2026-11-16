package com.coraline.multiplayer;

import com.coraline.feast.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline")
public final class StoryActivitySafety {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void fatal(LivingDamageEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getAmount()>=p.getHealth()&&StorySessionData.get(p.server).eliminateActivity(p,OtherMotherLoop.scene(p)))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void death(LivingDeathEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&StorySessionData.get(p.server).eliminateActivity(p,OtherMotherLoop.scene(p))){e.setCanceled(true);if(p.getHealth()<=0)p.setHealth(1);}
    }
    public static boolean beforeDirectorTick(ServerPlayer owner,VisitData.Scene scene){
        var data=StorySessionData.get(owner.server);if(!data.groupActivity(scene)||AcceptedButtonEyes.active(scene))return false;
        if(CooperativeActivities.playing(owner,scene).isEmpty()){ActivityWorld.cooperativeFailure(owner,scene);return true;}
        for(var p:data.participants(owner.server,scene.owner,true))if(data.activitySpectator(p)&&p.serverLevel()==owner.serverLevel()){
            if(!p.isSpectator())p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            var target=CooperativeActivities.playing(owner,scene).stream().findFirst().orElse(null);if(target!=null)p.setCamera(target);
        }return false;
    }
    public static void restore(ServerPlayer owner,VisitData.Scene scene){var data=StorySessionData.get(owner.server);for(var p:data.participants(owner.server,scene.owner,true))data.restoreActivityMode(p);}
    private StoryActivitySafety(){}
}
