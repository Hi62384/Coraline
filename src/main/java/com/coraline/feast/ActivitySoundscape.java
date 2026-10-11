package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Owner-local music and milestone cues; no world-wide music or per-second spam. */
public final class ActivitySoundscape {
    static String track(OtherMotherActivities.Activity activity){return switch(activity){
        case MUSIC_FESTIVAL->"";case OTHER_DESERT_TEMPLE,OTHER_END_CITY,OTHER_MINESHAFT,OTHER_NETHER_FORTRESS,OTHER_WOODLAND_MANSION->"ambient_dream";
        case CARNIVAL->"ambient_carnival";case MAGIC_SHOW->"ambient_theatre";case MICE_CIRCUS->"";
        case SHOPPING_MALL->"ambient_feast";case SNOWBALL_BATTLE,ICE_BOAT_RACE->"ambient_race";case ARCHERY_CONTEST->"ambient_archery";
        case TREASURE_HUNT,GARDEN_MAZE,FISHING_CONTEST->"ambient_garden";case GRAND_FEAST->"ambient_feast";
        case OTHER_OCEAN_MONUMENT->"ambient_dream";
        case DREAM_PARKOUR->"ambient_dream";case MAGIC_GARDEN,CAKE_MAKING_CONTEST->"";default->"ambient_village";};}
    static void tick(ServerPlayer p,VisitData.Scene s){
        if(p.tickCount%10!=0)return;
        var l=s.loop;var data=p.getPersistentData();String id=l.activity.id();
        if(l.activity==OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT&&l.ocean.getInt("Phase")>=2)return;
        long progress=switch(l.activity){
            case OTHER_VILLAGE->20-l.villageTokensRemaining;case FISHING_CONTEST->l.fishingCatches;
            case MAGIC_SHOW->l.magicTokens;case MAGIC_GARDEN->l.flyingGardenHarvestCount;
            case ICE_BOAT_RACE->l.iceLap*100+l.iceCheckpoint;case GRAND_FEAST->l.feastBasketCount;
            case GARDEN_MAZE->Integer.bitCount(l.gardenClaimMask);case MICE_CIRCUS->l.circusAct;
            case CARNIVAL->l.carnivalProgress;case CAKE_MAKING_CONTEST->l.cakeStep;
            default->0;
        };
        boolean entering=!id.equals(data.getString("ActivityAudioId"));
        if(entering){data.putString("ActivityAudioId",id);sound(p,SoundEvents.AMETHYST_BLOCK_CHIME,.4F,.85F);}
        else if(progress>data.getLong("ActivityAudioProgress"))sound(p,
            l.activity==OtherMotherActivities.Activity.FISHING_CONTEST?SoundEvents.FISHING_BOBBER_RETRIEVE:
            l.activity==OtherMotherActivities.Activity.ICE_BOAT_RACE?SoundEvents.NOTE_BLOCK_BELL.value():SoundEvents.EXPERIENCE_ORB_PICKUP,.35F,1.15F);
        data.putLong("ActivityAudioProgress",progress);
        String music=track(l.activity);if(!music.isEmpty()&&p.tickCount%40==0)VisitAudio.play(p,music,true);
        long end=switch(l.activity){case GARDEN_MAZE->l.gardenEndAt;case GRAND_FEAST->l.feastEndAt;
            case FISHING_CONTEST->l.fishingEndAt;case DREAM_PARKOUR->l.parkourEndAt;
            case MAGIC_GARDEN->l.flyingGardenHarvestUntil;case CARNIVAL->l.carnivalGameUntil;default->0;};
        long seconds=(end-OtherMotherLoop.now(p)+19)/20;
        if(seconds!=data.getLong("ActivityAudioSeconds")&&(seconds==30||seconds==10||seconds>0&&seconds<=3))
            sound(p,SoundEvents.NOTE_BLOCK_HAT.value(),.45F,seconds<=3?1.5F:1F);
        data.putLong("ActivityAudioSeconds",seconds);
    }
    static void stop(ServerPlayer p,VisitData.Scene s){
        String track=track(s.loop.activity);if(!track.isEmpty())VisitAudio.stopMusic(p,track);
        if(s.loop.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST)VisitAudio.stopMusic(p,"cake_baking");
        p.getPersistentData().remove("ActivityAudioId");sound(p,SoundEvents.CHORUS_FRUIT_TELEPORT,.45F,1.15F);
    }
    private static void sound(ServerPlayer p,net.minecraft.sounds.SoundEvent sound,float volume,float pitch){p.playNotifySound(sound,SoundSource.PLAYERS,volume,pitch);}
}
