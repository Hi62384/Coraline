package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import java.util.Locale;
import java.util.Objects;

/** Audio presentation only: callers supply a state, not trust/suspicion thresholds. */
public final class OtherMotherWelcome {
    private OtherMotherWelcome() {}

    public enum State {
        LOW_TRUST_LOW_SUSPICION,
        MID_TRUST_MID_SUSPICION,
        HIGH_TRUST_LOW_SUSPICION,
        HIGH_SUSPICION_LOW_TRUST,
        HIGH_TRUST_HIGH_SUSPICION;

        public String id() { return name().toLowerCase(Locale.ROOT); }
        public String sound(int index) {
            if (index < 0 || index >= 3) throw new IllegalArgumentException("Welcome index must be 0..2");
            return "other_mother_welcome_" + id() + "_0" + (index + 1);
        }
        public static State fromId(String id) {
            for (var state : values()) if (state.id().equals(id)) return state;
            return LOW_TRUST_LOW_SUSPICION;
        }
    }

    /** Uniform choice from this state, excluding the immediately previous recording. */
    public static String choose(State state, String previous, RandomSource random) {
        int excluded = -1;
        for (int i = 0; i < 3; i++) if (state.sound(i).equals(previous)) excluded = i;
        int choice = random.nextInt(excluded < 0 ? 3 : 2);
        if (excluded >= 0 && choice >= excluded) choice++;
        return state.sound(choice);
    }

    /** Integration point for the future/external relationship system. No progression is invented here. */
    public static boolean setState(ServerPlayer player, State state) {
        var data = VisitData.get(player.server);
        var scene = data.scenes.get(player.getUUID());
        if (scene == null) return false;
        scene.welcomeState = Objects.requireNonNull(state);
        scene.loop.trust=switch(state){case HIGH_TRUST_LOW_SUSPICION,HIGH_TRUST_HIGH_SUSPICION->.8F;case MID_TRUST_MID_SUSPICION->.5F;default->.2F;};
        scene.loop.suspicion=switch(state){case HIGH_SUSPICION_LOW_TRUST,HIGH_TRUST_HIGH_SUSPICION->.8F;case MID_TRUST_MID_SUSPICION->.5F;default->.2F;};
        data.setDirty();
        return true;
    }

    public static boolean eligible(VisitData.Scene scene, java.util.UUID door, long now) {
        return scene != null && scene.stage == VisitData.Stage.DONE
                && Objects.equals(scene.door, door) && now >= scene.welcomeAfter;
    }

    /** Called once on actual tunnel exit, never during staging or the first-visit dialogue. */
    public static void entered(ServerPlayer player, RealmData.Realm realm) {
        if (!OtherWorld.isOther(player.level()) || player.getPersistentData().getBoolean("CoralineInTunnel")) return;
        var data = VisitData.get(player.server);
        var scene = data.scenes.get(player.getUUID());
        long now = player.server.overworld().getGameTime();
        if (!eligible(scene, realm.door, now)) return;
        if (scene.mom == null || !(player.serverLevel().getEntity(scene.mom) instanceof ParentEntity mother)
                || !mother.isAlive() || !mother.isMom() || !mother.isOtherParent()) return;
        String sound = choose(scene.welcomeState, scene.lastWelcome, player.getRandom());
        VisitAudio.play(player, sound, false);
        scene.lastWelcome = sound;
        scene.welcomeAfter = now + VisitAudio.length(sound) + 20;
        data.setDirty();
    }
}
