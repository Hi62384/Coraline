package com.coraline.postgame;

import com.coraline.feast.VisitData;
import com.coraline.palace.PalaceKeyTransaction;
import com.coraline.palace.PalaceWellProgress;
import com.coraline.rebirth.StoryRebirth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Records only the actual canonical well ending. This does not enable portals
 * into an unimplemented generator or pretend that the expansion is playable. */
@Mod.EventBusSubscriber(modid="coraline")
public final class InfiniteWorldUnlock {
    public static boolean record(ServerPlayer player,VisitData.Scene scene){
        if(scene==null||!player.getUUID().equals(scene.owner)||!StoryRebirth.canonicalCompletion(scene))return false;
        var root=scene.loop.story.getCompound("PinkPalaceJourney");
        var well=PalaceWellProgress.load(scene.owner,scene.door,root.getCompound("Well"));
        var key=PalaceKeyTransaction.load(scene.owner,scene.door,root.getCompound("KeyCustody"));
        if(!GlobalHandEncounter.permits(player.server,scene,well))return false;
        var witness=new InfiniteWorldState.Witness(scene.owner,scene.door,well.session(),key.receipt(),well.originalWybie().orElse(null),well.wybieLife());
        var data=InfiniteWorldData.get(player.server);
        if(data.record(player.server,witness)==InfiniteWorldState.Activation.REFUSED||data.checkpointSeed(player.server).isEmpty())return false;
        GlobalHandEncounter.releaseCompleted(player.server,scene,well);
        InfiniteWorldGeneration.bindAvailable(player.server);return true;
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player)record(player,VisitData.get(player.server).scenes.get(player.getUUID()));
    }
    private InfiniteWorldUnlock(){}
}
