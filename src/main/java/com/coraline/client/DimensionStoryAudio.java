package com.coraline.client;

import net.minecraft.client.resources.sounds.SoundInstance;

/** Only the screen update inside an in-session level handoff may keep these
 * listener-relative story streams. Disconnect/reload/explicit stops retain
 * vanilla cleanup, and the original OpenAL source is never restarted. */
public final class DimensionStoryAudio {
    private static final ThreadLocal<Boolean> HANDOFF=ThreadLocal.withInitial(()->false);
    public static boolean handoff(){return HANDOFF.get();}
    public static boolean preserves(SoundInstance sound){return VisitAudioClient.handoffVoice(sound)||AcceptedSewingMusic.owns(sound)||OtherMotherGameProposalMusic.owns(sound)||ParentsRescueArrivalMusic.owns(sound)||FirstEscapeMusic.owns(sound)||MissingParentsMusic.owns(sound);}
    public static void duringHandoff(Runnable screenUpdate){
        boolean previous=HANDOFF.get();HANDOFF.set(true);
        try{screenUpdate.run();}finally{HANDOFF.set(previous);}
    }
    private DimensionStoryAudio(){}
}
