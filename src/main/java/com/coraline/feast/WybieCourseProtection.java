package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Helpers cannot dismantle or obstruct another owner's live scoring course. */
@Mod.EventBusSubscriber(modid="coraline")
public final class WybieCourseProtection {
    @SubscribeEvent public static void breakCourse(BlockEvent.BreakEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!OtherWorld.isOther(level))return;
        if(temporary(level,event.getPos()))event.setCanceled(true);
    }
    @SubscribeEvent public static void placeObstacle(BlockEvent.EntityPlaceEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof ServerPlayer player)||!OtherWorld.isOther(level))return;
        for(var scene:VisitData.get(level.getServer()).scenes.values()){
            if(!scene.loop.event.getBoolean("WybiePending")||scene.owner.equals(player.getUUID()))continue;
            var course=WybieCourse.get(WybieAdventure.data(scene));if(course==null)continue;
            if(course.distanceSquared(event.getPos().getCenter())<=9){event.setCanceled(true);return;}
            if(event instanceof BlockEvent.EntityMultiPlaceEvent multi)for(var replaced:multi.getReplacedBlockSnapshots())if(course.distanceSquared(replaced.getPos().getCenter())<=9){event.setCanceled(true);return;}
        }
    }
    private static boolean temporary(ServerLevel level,BlockPos at){
        String key=Long.toString(at.asLong());for(var scene:VisitData.get(level.getServer()).scenes.values())if(scene.loop.event.getBoolean("WybiePending")&&WybieAdventure.data(scene).getCompound("CourseBlocks").contains(key))return true;return false;
    }
    private WybieCourseProtection(){}
}
