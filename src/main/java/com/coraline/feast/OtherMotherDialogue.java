package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import com.coraline.parents.ParentEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;
import java.util.List;

/** Shared audio-only pools; gameplay owners control rewards and phase transitions. */
public final class OtherMotherDialogue {
    private OtherMotherDialogue() {}
    public enum Loot {
        FOOD("Food",10), TREASURE("Treasure",10), WEAPONS("Weapons",10), ARMOR("Armor",10), TOOLS("Tools",10),
        BUILDING_MATERIALS("Building Materials",10), REDSTONE("Redstone & Machinery",10), CLOTHING("Clothing",10), MUSIC("Music",10), MYSTERY("Mystery Gifts",10);
        public final String displayName;public final int defaultWeight;
        Loot(String displayName,int weight){this.displayName=displayName;this.defaultWeight=weight;}
        public String id(){return name().toLowerCase(java.util.Locale.ROOT);}
        public boolean expanded(){return this!=FOOD&&this!=TREASURE;}
    }
    public enum Moment { BEFORE, AFTER }
    public record Pool(String path, int size, String filePrefix) {
        public Pool(String path, int size) { this(path, size, "line"); }
        public String fileName(int index) { return filePrefix + "_0" + (index + 1) + ".ogg"; }
        public String sound(int index) {
            if (index < 0 || index >= size) throw new IllegalArgumentException("Invalid dialogue index");
            return "other_mother_" + path.replace('/', '_') + "_0" + (index + 1);
        }
    }
    public static Pool lootPool(Loot loot, Moment moment) {
        return new Pool("loot/" + loot.name().toLowerCase(java.util.Locale.ROOT) + "/"
                + moment.name().toLowerCase(java.util.Locale.ROOT), 3);
    }
    public static Pool extraPool(OtherMotherWelcome.State state) {
        return new Pool("state_extra/" + state.id(), 2);
    }
    public static List<Pool> pools() {
        var pools = new ArrayList<Pool>();
        for (var loot : Loot.values()) for (var moment : Moment.values()) pools.add(lootPool(loot, moment));
        for (var state : OtherMotherWelcome.State.values()) pools.add(extraPool(state));
        pools.add(OtherMotherTutorial.POOL);
        for (var activity : OtherMotherActivities.Activity.values()) pools.add(activity.pool());
        for (var state : OtherMotherWelcome.State.values()) pools.add(OtherMotherActivities.waitPool(state));
        for (var state : OtherMotherWelcome.State.values()) for (var arrival : OtherMotherActivities.Arrival.values())
            pools.add(OtherMotherActivities.arrivalPool(state, arrival));
        for (var state : OtherMotherWelcome.State.values()) pools.add(OtherMotherActivityComplete.pool(state));
        pools.addAll(com.coraline.secrets.SecretDialogue.pools());
        pools.addAll(com.coraline.secrets.Reconciliation.pools());
        return List.copyOf(pools);
    }
    public static String choose(Pool pool, String previous, RandomSource random) {
        // Scolding has exactly one authored recording per category/state.
        if (pool.size() == 1) return pool.sound(0);
        int excluded = -1;
        for (int i = 0; i < pool.size(); i++) if (pool.sound(i).equals(previous)) excluded = i;
        int choice = random.nextInt(pool.size() - (excluded < 0 ? 0 : 1));
        if (excluded >= 0 && choice >= excluded) choice++;
        return pool.sound(choice);
    }
    /** Returns the playback duration in ticks, or zero when busy/ineligible.
     * The caller must wait for BEFORE to finish, grant its reward, then request AFTER.
     * No rewards, timers, or relationship progression are invented by this audio layer. */
    public static int playLoot(ServerPlayer player, Loot loot, Moment moment) {
        return play(player, lootPool(loot, moment));
    }
    /** Optional follow-up, using exactly the existing externally selected welcome state. */
    public static int playStateExtra(ServerPlayer player) {
        var scene = VisitData.get(player.server).scenes.get(player.getUUID());
        return scene == null ? 0 : play(player, extraPool(scene.welcomeState));
    }
    static int play(ServerPlayer player, Pool pool) {
        return play(player, pool, false);
    }
    static int playActivityContinuation(ServerPlayer player, Pool pool) {
        return play(player, pool, true);
    }
    /** Activity controller has already validated ownership and its active attendance sequence.
     * The portal can be outside the mother's loaded chunk; audio remains player-relative. */
    static int playActivityArrival(ServerPlayer player, Pool pool) {
        return play(player, pool, true, true);
    }
    static int playActivityComplete(ServerPlayer player, Pool pool) {
        return play(player, pool, false, true);
    }
    private static int play(ServerPlayer player, Pool pool, boolean activityContinuation) {
        return play(player, pool, activityContinuation, false);
    }
    public static int playSecretSelected(ServerPlayer player,Pool pool,String selected){return play(player,pool,false,true,selected);}
    static int playSelected(ServerPlayer player,Pool pool,String selected){return play(player,pool,true,true,selected);}
    private static int play(ServerPlayer player, Pool pool, boolean activityContinuation, boolean allowUnloadedMother) {return play(player,pool,activityContinuation,allowUnloadedMother,null);}
    private static int play(ServerPlayer player, Pool pool, boolean activityContinuation, boolean allowUnloadedMother,String selected) {
        var data = VisitData.get(player.server);
        var scene = data.scenes.get(player.getUUID());
        long now = player.server.overworld().getGameTime();
        if (!OtherWorld.isOther(player.level()) || player.getPersistentData().getBoolean("CoralineInTunnel")
                || scene == null || scene.stage != VisitData.Stage.DONE || now < scene.welcomeAfter
                || OtherMotherTutorial.pending(player)
                || OtherMotherActivityComplete.pending(player)
                || (!activityContinuation && OtherMotherActivities.speaking(player))
                || !player.getPersistentData().hasUUID("CoralineVisitDoor")
                || !scene.door.equals(player.getPersistentData().getUUID("CoralineVisitDoor"))
                || scene.mom == null) return 0;
        var entity = player.serverLevel().getEntity(scene.mom);
        if (entity == null ? !allowUnloadedMother : !(entity instanceof ParentEntity mother)
                || !mother.isAlive() || !mother.isMom() || !mother.isOtherParent()) return 0;
        String sound = selected==null?choose(pool, scene.lastDialogue.getOrDefault(pool.path(), ""), player.getRandom()):selected;
        if(java.util.stream.IntStream.range(0,pool.size()).noneMatch(i->pool.sound(i).equals(sound)))return 0;
        int duration = VisitAudio.length(sound);
        scene.loop.secrets.dialogue.putString("VoiceClockSound",sound);
        scene.loop.secrets.dialogue.putLong("VoiceClockEnd",System.currentTimeMillis()+duration*50L);
        VisitAudio.play(player, sound, false);
        scene.lastDialogue.put(pool.path(), sound);
        scene.welcomeAfter = now + duration;
        data.setDirty();
        return duration;
    }
}
