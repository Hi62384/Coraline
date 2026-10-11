package com.coraline.client;

import com.coraline.otherworld.*;
import com.coraline.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One listener-relative loop, cleaned up on exit, death, disconnect or level change. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class TunnelAudio {
    private static Hum hum;
    private static boolean inside(){var mc=Minecraft.getInstance();return mc.level!=null&&OtherWorld.isOther(mc.level)&&mc.player!=null&&mc.player.isAlive()&&RealmVisualState.client!=null&&RealmVisualState.client.tunnel();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var manager=Minecraft.getInstance().getSoundManager();
        if(!inside()){if(hum!=null){manager.stop(hum);hum=null;}return;}
        if(hum==null){hum=new Hum();manager.play(hum);}
    }
    private static final class Hum extends AbstractTickableSoundInstance {
        Hum(){super(ModSounds.TUNNEL_HUM.get(),SoundSource.AMBIENT,RandomSource.create());looping=true;delay=0;relative=true;attenuation=Attenuation.NONE;volume=com.coraline.config.CoralineConfig.TUNNEL_HUM_VOLUME.get().floatValue();}
        @Override public void tick(){if(!inside())stop();}
    }
}
