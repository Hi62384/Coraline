package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Advances only the owner introduction gate, never a game timer or result. */
public final class ActivityIntro {
    private ActivityIntro(){}
    private static CompoundTag state(OtherMotherLoopState s){return switch(s.activity){
        case MUSIC_FESTIVAL->s.festival;
        case OTHER_DESERT_TEMPLE->s.temple;
        case OTHER_END_CITY->s.endCity;
        case OTHER_OCEAN_MONUMENT->s.ocean;
        default->s.expedition;
    };}
    static long deadline(OtherMotherLoopState s){return switch(s.activity){
        case OTHER_VILLAGE->s.villageIssued?0:s.villageGiftAt;
        case ARCHERY_CONTEST->s.archeryIssued?0:s.archeryGiftAt;
        case CARNIVAL->s.carnivalIssued?0:s.carnivalGiftAt;
        case CAKE_MAKING_CONTEST->s.cakeStage==0?s.cakeVoiceUntil:0;
        case FISHING_CONTEST->s.fishingStage==0?s.fishingVoiceUntil:0;
        case ICE_BOAT_RACE->s.iceStage==0?s.iceVoiceUntil:0;
        case MAGIC_SHOW->s.magicStage==0?s.magicVoiceUntil:0;
        case MAGIC_GARDEN->s.flyingGardenStage==0?s.flyingGardenVoiceUntil:0;
        case MICE_CIRCUS->s.circusPhase==0?s.circusVoiceUntil:0;
        case DREAM_PARKOUR->s.parkourEndAt==0?s.parkourVoiceUntil:0;
        case GARDEN_MAZE->s.gardenEndAt==0?s.gardenVoiceUntil:0;
        case GRAND_FEAST->s.feastEndAt==0?s.feastVoiceUntil:0;
        default->state(s).getInt("Phase")==0?state(s).getLong("VoiceUntil"):0;
    };}
    static boolean finish(OtherMotherLoopState s,long now){
        if(deadline(s)<=now)return false;
        switch(s.activity){
            case OTHER_VILLAGE->s.villageGiftAt=now;
            case ARCHERY_CONTEST->s.archeryGiftAt=now;
            case CARNIVAL->s.carnivalGiftAt=now;
            case CAKE_MAKING_CONTEST->s.cakeVoiceUntil=now;
            case FISHING_CONTEST->s.fishingVoiceUntil=now;
            case ICE_BOAT_RACE->s.iceVoiceUntil=now;
            case MAGIC_SHOW->s.magicVoiceUntil=now;
            case MAGIC_GARDEN->s.flyingGardenVoiceUntil=now;
            case MICE_CIRCUS->s.circusVoiceUntil=now;
            case DREAM_PARKOUR->s.parkourVoiceUntil=now;
            case GARDEN_MAZE->s.gardenVoiceUntil=now;
            case GRAND_FEAST->s.feastVoiceUntil=now;
            default->state(s).putLong("VoiceUntil",now);
        }
        return true;
    }
    static boolean eligible(ServerPlayer p,VisitData.Scene s,String sound,int generation){
        return s!=null&&p.getUUID().equals(s.owner)&&ActivityWorld.isActivity(p.level())
            &&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY&&s.loop.generation==generation
            &&sound.equals(ActivityWorld.introSound(s.loop.activity));
    }
    static void offer(ServerPlayer p,String sound){
        var s=OtherMotherLoop.active(p);
        if(s!=null&&eligible(p,s,sound,s.loop.generation)){
            long left=deadline(s.loop)-OtherMotherLoop.now(p);
            if(left>0)VisitAudio.introOffer(p,new VisitAudio.IntroOffer(sound,s.loop.generation,(int)Math.min(left,12000)));
        }
    }
    static boolean skip(ServerPlayer p,String sound,int generation){
        var s=OtherMotherLoop.active(p);
        if(!eligible(p,s,sound,generation)||!finish(s.loop,OtherMotherLoop.now(p)))return false;
        OtherMotherLoop.changed(p);
        VisitAudio.stopVoice(p,sound);
        VisitAudio.introOffer(p,new VisitAudio.IntroOffer(sound,generation,0));
        return true;
    }
}
