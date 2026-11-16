package com.coraline.feast;
import net.minecraft.server.level.ServerPlayer;
/** Migrate only verified durable completion flags. Wybie's spawn-only flag is never accepted. */
public final class VisitProgression {
    public static void refresh(ServerPlayer p,VisitData.Scene s){if(s==null||!p.getUUID().equals(s.owner))return;boolean changed=false;
        // BobinskyVisit assigns this only after successful WAIT_HIDE restoration, never offer/spawn/cancel.
        if(s.bobinskyVisited&&!s.loop.story.getBoolean("BobinskyIntroductionComplete")){s.loop.story.putBoolean("BobinskyIntroductionComplete",true);changed=true;}
        // TeaVisit assigns this only after LEAVE, owner exit and safe unseen house restoration.
        if(s.teaVisited&&!s.loop.story.getBoolean("TeaIntroductionComplete")){s.loop.story.putBoolean("TeaIntroductionComplete",true);changed=true;}
        if(changed)OtherMotherLoop.changed(p);
    }
    private VisitProgression(){}
}
