package com.coraline.client;

import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** The apparently endless white boundary dissolves and rematerializes the estate. */
public final class OtherWorldWhiteout {
    private OtherWorldWhiteout(){}
    public static float amount(float partial){
        if(RescueClient.active())return 0; // The rescue collapse is depth-aware, not the legacy full-frame border fade.
        var mc=Minecraft.getInstance();var view=RealmVisualState.client;
        if(mc.level==null||mc.player==null||view==null||view.tunnel()||!OtherWorld.isOther(mc.level))return 0;
        double distance=Math.hypot(mc.player.getX()-view.center().getX(),mc.player.getZ()-view.center().getZ());
        float edge=Mth.clamp((float)((distance-(view.radius()-12))/54.0),0,1);
        float sequence=0;
        float catFade=CatEncounterClient.fade(partial);if(catFade>=0)return catFade;
        if(view.whiteoutAge()>=0){float age=view.whiteoutAge()+partial;sequence=age<=200?1:Mth.clamp((260-age)/60F,0,1);}
        return Math.max(edge,sequence);
    }
}
