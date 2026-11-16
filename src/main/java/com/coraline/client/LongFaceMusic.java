package com.coraline.client;
import com.coraline.feast.LongFaceMusicPlan;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class LongFaceMusic {
    static void update(){
        int stage=LongFaceMusicPlan.stage(TraditionalClient.phase(),FirstEscapeClient.phase());
        ScenePreludeMusic.setGain(LongFaceMusicPlan.SOUND,stage>0,LongFaceMusicPlan.gain(stage));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        update();
    }
    private LongFaceMusic(){}
}
