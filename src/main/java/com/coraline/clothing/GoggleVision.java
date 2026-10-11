package com.coraline.clothing;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Track only our own hidden effect. Longer/stronger potion effects retain ownership. */
@Mod.EventBusSubscriber(modid="coraline")
public final class GoggleVision {
    private static final Map<Player,MobEffectInstance> OWNED=new WeakHashMap<>();
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        Player p=event.player;
        if(event.phase!=TickEvent.Phase.END||p.level().isClientSide)return;
        var effect=p.getEffect(MobEffects.NIGHT_VISION);
        boolean ours=effect!=null&&effect==OWNED.get(p)&&effect.getDuration()<=240&&effect.getAmplifier()==0&&effect.isAmbient()&&!effect.isVisible()&&!effect.showIcon();
        if(!ours)OWNED.remove(p);
        if(p.isAlive()&&ClothingEffects.wearing(p,ClothingItem.Style.MIDNIGHT_GOGGLES)){
            if(effect==null||(ours&&effect.getDuration()<230)){
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0,true,false,false));
                OWNED.put(p,p.getEffect(MobEffects.NIGHT_VISION));
            }
        }else if(ours){p.removeEffect(MobEffects.NIGHT_VISION);OWNED.remove(p);}
    }
}
