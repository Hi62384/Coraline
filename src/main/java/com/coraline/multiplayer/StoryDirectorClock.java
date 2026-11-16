package com.coraline.multiplayer;

import java.util.*;
import com.coraline.feast.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Named game clocks only: never shifts seeds, packed positions, inventories,
 * encounter health, scores or completion receipts. */
final class StoryDirectorClock {
    private static long shift(long value,long delta){if(value<=0)return value;try{return Math.addExact(value,delta);}catch(ArithmeticException overflow){return Long.MAX_VALUE;}}
    static void apply(VisitData.Scene scene,UUID receipt,long delta){
        if(scene.loop.story.hasUUID("CoopClockReceipt")&&scene.loop.story.getUUID("CoopClockReceipt").equals(receipt))return;
        var l=scene.loop;
        l.giftRevealUntil=shift(l.giftRevealUntil,delta);
        l.villageGiftAt=shift(l.villageGiftAt,delta);
        l.archeryGiftAt=shift(l.archeryGiftAt,delta);
        l.cakeOvenReadyAt=shift(l.cakeOvenReadyAt,delta);
        l.cakeMixStartedAt=shift(l.cakeMixStartedAt,delta);
        l.cakeOvenStartedAt=shift(l.cakeOvenStartedAt,delta);
        l.cakeVoiceUntil=shift(l.cakeVoiceUntil,delta);
        l.cakeCountdownUntil=shift(l.cakeCountdownUntil,delta);
        l.cakeBakeUntil=shift(l.cakeBakeUntil,delta);
        l.cakeStepAt=shift(l.cakeStepAt,delta);
        l.cakeJudgeUntil=shift(l.cakeJudgeUntil,delta);
        l.cakeAnnounceAt=shift(l.cakeAnnounceAt,delta);
        l.cakeLastActionAt=shift(l.cakeLastActionAt,delta);
        l.carnivalGiftAt=shift(l.carnivalGiftAt,delta);
        l.carnivalGameUntil=shift(l.carnivalGameUntil,delta);
        l.carnivalRoundAt=shift(l.carnivalRoundAt,delta);
        l.carnivalLastActionAt=shift(l.carnivalLastActionAt,delta);
        l.carnivalRideUntil=shift(l.carnivalRideUntil,delta);
        l.parkourVoiceUntil=shift(l.parkourVoiceUntil,delta);
        l.parkourEndAt=shift(l.parkourEndAt,delta);
        l.gardenVoiceUntil=shift(l.gardenVoiceUntil,delta);
        l.gardenEndAt=shift(l.gardenEndAt,delta);
        l.feastVoiceUntil=shift(l.feastVoiceUntil,delta);
        l.feastEndAt=shift(l.feastEndAt,delta);
        for(int i=0;i<l.feastPlateRefillAt.length;i++)l.feastPlateRefillAt[i]=shift(l.feastPlateRefillAt[i],delta);
        l.feastNextWaveAt=shift(l.feastNextWaveAt,delta);
        l.feastCartAt=shift(l.feastCartAt,delta);
        l.feastGuestCanEatAt=shift(l.feastGuestCanEatAt,delta);
        l.fishingVoiceUntil=shift(l.fishingVoiceUntil,delta);
        l.fishingEndAt=shift(l.fishingEndAt,delta);
        l.fishingWalkUntil=shift(l.fishingWalkUntil,delta);
        l.fishingJudgeUntil=shift(l.fishingJudgeUntil,delta);
        l.fishingAnnounceAt=shift(l.fishingAnnounceAt,delta);
        l.fishingLastNpcAt=shift(l.fishingLastNpcAt,delta);
        l.iceVoiceUntil=shift(l.iceVoiceUntil,delta);
        l.iceCountdownUntil=shift(l.iceCountdownUntil,delta);
        l.iceResultAt=shift(l.iceResultAt,delta);
        l.magicVoiceUntil=shift(l.magicVoiceUntil,delta);
        l.magicPhaseUntil=shift(l.magicPhaseUntil,delta);
        l.flyingGardenVoiceUntil=shift(l.flyingGardenVoiceUntil,delta);
        l.flyingGardenPhaseUntil=shift(l.flyingGardenPhaseUntil,delta);
        l.flyingGardenHarvestUntil=shift(l.flyingGardenHarvestUntil,delta);
        for(int i=0;i<l.flyingGardenRegrowAt.length;i++)l.flyingGardenRegrowAt[i]=shift(l.flyingGardenRegrowAt[i],delta);
        l.circusVoiceUntil=shift(l.circusVoiceUntil,delta);
        l.circusActAt=shift(l.circusActAt,delta);
        l.circusPhaseUntil=shift(l.circusPhaseUntil,delta);
        l.deadline=shift(l.deadline,delta);
        l.arrivalDeadline=shift(l.arrivalDeadline,delta);
        scene.supperMusicEndsAt=shift(scene.supperMusicEndsAt,delta);scene.welcomeAfter=shift(scene.welcomeAfter,delta);scene.nextNeighborAttempt=shift(scene.nextNeighborAttempt,delta);
        for(var tag:List.of(l.event,l.story,l.ocean,l.festival,l.temple,l.endCity,l.expedition,l.secrets.dialogue))nested(tag,delta,0);
        for(var tag:List.of(l.temple,l.endCity))for(String key:List.of("Preview","RoomStarted","NextEat","InvulnerableUntil"))if(tag.contains(key,Tag.TAG_LONG))tag.putLong(key,shift(tag.getLong(key),delta));
        if(l.festival.contains("Start",Tag.TAG_LONG))l.festival.putLong("Start",shift(l.festival.getLong("Start"),delta));
        l.story.putUUID("CoopClockReceipt",receipt);
    }
    static void applyMembers(CompoundTag session,UUID receipt,long delta){
        for(String id:session.getCompound("Members").getAllKeys()){var member=session.getCompound("Members").getCompound(id);if(member.hasUUID("ClockReceipt")&&member.getUUID("ClockReceipt").equals(receipt))continue;var progress=member.getCompound("ActivityProgress");nested(progress,delta,0);for(String key:progress.getAllKeys()){var state=progress.getCompound(key);if(state.contains("InvulnerableUntil",Tag.TAG_LONG))state.putLong("InvulnerableUntil",shift(state.getLong("InvulnerableUntil"),delta));}member.putUUID("ClockReceipt",receipt);}
    }
    private static final Set<String> CLOCKS=Set.of("Deadline","PhaseUntil","VoiceUntil","EndAt","NextActionAt","LastActionAt","StartedAt","PhaseStartedAt","NextTick","LastTick","NextAt","GameUntil","RoundAt","CooldownUntil","ReplyUntil","ChoiceUntil","FinishAt","ReturnAt","Started","Until");
    private static void nested(CompoundTag tag,long delta,int depth){if(depth>12)return;for(String key:new ArrayList<>(tag.getAllKeys())){if(CLOCKS.contains(key)&&tag.contains(key,Tag.TAG_LONG))tag.putLong(key,shift(tag.getLong(key),delta));else if(tag.get(key) instanceof CompoundTag child&&!Set.of("InventoryEscrow","InventoryReturnWitness","Rewards","Items","Snapshot","Retained").contains(key))nested(child,delta,depth+1);}}
    static boolean saved(ServerPlayer owner,VisitData.Scene scene,UUID receipt){
        try{var root=com.coraline.rebirth.StoryRunStore.read(owner.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/coraline_first_visits.dat")).getCompound("data");
            for(var raw:root.getList("Scenes",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;if(row.hasUUID("Owner")&&row.getUUID("Owner").equals(scene.owner)&&row.hasUUID("Door")&&row.getUUID("Door").equals(scene.door)){var story=row.getCompound("MainLoop").getCompound("StoryProgression");return story.hasUUID("CoopClockReceipt")&&story.getUUID("CoopClockReceipt").equals(receipt);}}return false;
        }catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    private StoryDirectorClock(){}
}
