package com.coraline.feast;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** First act only. The existing diving/trapeze/player-stunt indices are unchanged.
 * Every cue advances through the established real-mixer playback gate, never a
 * timer, debug seek, frame preview or client-supplied completion flag.
 */
public final class NauticalOpeningShow {
    public static final int SET_CUE_BASE = 100;

    private static Map<String,Integer> measured() {
        var durations = new HashMap<String,Integer>();
        for (char cue = 'A'; cue <= 'N'; cue++) {
            String sound = NauticalOpeningPlan.sound(cue);
            durations.put(sound, ShowRecordings.duration(sound));
        }
        return durations;
    }
    public static String blocker() {
        if (ShowRecordings.duration(NauticalOpeningPlan.ENTRANCE) <= 0)
            return "Original theatre Entrance recording is unavailable.";
        try { new NauticalOpeningPlan(measured()); return ""; }
        catch (IllegalArgumentException missing) { return missing.getMessage(); }
    }
    static void interrupt(VisitData.Scene s) { TheatreVisit.data(s).putBoolean("OpeningCueStarted", false); TheatreVisit.data(s).remove("NauticalPreview"); }
    static void restart(VisitData.Scene s) {
        var n = TheatreVisit.data(s);
        n.putInt("NauticalVersion", 1); n.putInt("OpeningCue", 0);
        n.putInt("OpeningSeatDelay", 0); n.putBoolean("OpeningCueStarted", false);
    }

    static boolean tick(ServerPlayer player, VisitData.Scene s) {
        if (!TheatreVisit.owned(player, s) || !TheatreSite.inside(player, s)) return false;
        var n = TheatreVisit.data(s);
        if (!n.contains("NauticalVersion")) {
            // An interrupted legacy act-one checkpoint restarts A, not the
            // unrelated old Theatre.mp3 cursor. It never skips the new opening.
            int seated=n.getInt("OpeningSeatDelay");ShowPlayback.stop(player, s); restart(s);n.putInt("OpeningSeatDelay",seated);
        }
        int index = n.getInt("OpeningCue");
        if (!n.contains("NauticalVersion",net.minecraft.nbt.Tag.TAG_INT)||!n.contains("OpeningCue",net.minecraft.nbt.Tag.TAG_INT)
                ||n.getInt("NauticalVersion") != 1 || index < 0 || index > 14) {
            n.putString("Blocker", "Unknown saved nautical opening checkpoint; recovery needs review.");
            return false;
        }
        if (index == 14) return true;
        String problem = blocker();
        if (!problem.isEmpty()) { n.putString("Blocker", problem); return false; }
        char cue = (char) ('A' + index);
        String sound = NauticalOpeningPlan.sound(cue);
        if (!n.getBoolean("OpeningCueStarted")) {
            if (!ShowPlayback.start(player, s, sound, true)) {
                n.putString("Blocker", ShowPlayback.data(s).getString("Blocker")); return false;
            }
            n.putBoolean("OpeningCueStarted", true);
        }
        boolean ended = ShowPlayback.tick(player, s);
        if (!ShowPlayback.data(s).getString("Blocker").isEmpty()) {
            n.putString("Blocker", ShowPlayback.data(s).getString("Blocker")); return false;
        }
        if (!ShowPlayback.started(s)) return false;
        if (!frame(player, s, ShowPlayback.position(s))) return false;
        n.putString("Cue", "NAUTICAL_" + cue);
        if (ended && !ShowPlayback.debugSeeking(s)) {
            if(cue<'N')ShowPlayback.transition(player,s,NauticalOpeningPlan.sound((char)(cue+1)));
            n.putInt("OpeningCue", index + 1); n.putBoolean("OpeningCueStarted", false);
            OtherMotherLoop.changed(player);
            if (cue == 'N') {
                TheatreVisit.curtain(player, s, 0);
                hide(player, s);
                ShowPlayback.stop(player, s);
                return true;
            }
        }
        return false;
    }

    /** Reconstructs only visuals at the heard cursor; no progress/reward writes. */
    static boolean frame(ServerPlayer player, VisitData.Scene s, int tick) {
        return frame(player,s,TheatreVisit.data(s).getInt("OpeningCue"),tick);
    }
    static void preview(ServerPlayer player,VisitData.Scene s){
        if(!TheatreVisit.owned(player,s)||!TheatreSite.inside(player,s))return;
        ShowPlayback.tick(player,s);
        if(ShowPlayback.started(s)&&ShowPlayback.data(s).getString("Blocker").isEmpty())
            frame(player,s,TheatreVisit.data(s).getInt("NauticalPreview"),ShowPlayback.position(s));
    }
    private static boolean frame(ServerPlayer player, VisitData.Scene s, int index, int tick) {
        if (index < 0 || index >= 14) return false;
        char cue = (char) ('A' + index);
        NauticalOpeningPlan plan;
        try { plan = new NauticalOpeningPlan(measured()); }
        catch (IllegalArgumentException missing) { TheatreVisit.data(s).putString("Blocker", missing.getMessage()); return false; }
        var frame = plan.frame(cue, tick);
        var a = ShowActors.actor(player, s, "other_miss_spink");
        var b = ShowActors.actor(player, s, "other_miss_forcible");
        if (a == null || b == null) { TheatreVisit.data(s).putString("Blocker", "Waiting for the original performers."); return false; }
        var center = TheatreSite.center(player, s);
        var set = TheatreSite.ensure(player, s, player.serverLevel(), CinematicPropEntity.OCEAN_SET, center.north(28), 0);
        if (set == null) { TheatreVisit.data(s).putString("Blocker", "Waiting for the owned nautical stage set."); return false; }
        int duration = plan.cues().get(index).duration();
        double u = Math.max(0, Math.min(1, tick / (double) duration));
        set.frame(SET_CUE_BASE + index, tick, duration);
        TheatreVisit.curtain(player, s, (float) frame.curtain());
        var origin = Vec3.atBottomCenterOf(center).add(0, 0, -28);
        double ax = 0, bx = 0, ay = 1.60 + frame.spinkLift(), by = 1.60 + frame.clamLift();
        int poseA = cue == 'D' ? 13 : 10, poseB = cue == 'F' ? frame.finalForciblePose()?17:14 : 10;
        float yawA=0,yawB=(float)frame.forcibleYaw();
        boolean visibleA = cue >= 'C', visibleB = cue >= 'E';
        if (cue == 'C') visibleA = frame.fishHeadLift() >= 1.4;
        if (cue >= 'G' && cue <= 'L') {
            // Alternate real wing-to-stage tracks. Incoming hands meet the
            // outgoing performer before their shared lateral push starts.
            boolean spinkIn = (cue - 'G') % 2 == 0;var push=NauticalOpeningPlan.push(cue,u);
            ax=push.spinkX();bx=push.forcibleX();yawA=push.spinkYaw();yawB=push.forcibleYaw();
            ay = by = 1.60;
            poseA = spinkIn ? 11 : 12; poseB = spinkIn ? 12 : 11;
            visibleA = visibleB = true;
        } else if (cue == 'M') {
            double enter = ScriptedShowTimeline.smooth(u / .20);
            ax = -13 + 12.2 * enter; bx = .8 * enter; ay = by = 1.60;
            poseA = poseB = frame.collapse() > .1 ? 12 : 11;
        } else if (cue == 'N') {
            ax = -.8; bx = .8; ay = by = 1.60; poseA = poseB = 12;
        }
        if(cue=='D')ax+=frame.spinkSway()*3;
        if(cue=='F')bx+=frame.forcibleSway()*2;
        a.setInvisible(!visibleA); b.setInvisible(!visibleB);
        ShowActors.position(a, origin.add(ax, ay, 0), yawA, poseA, tick, 0);
        ShowActors.position(b, origin.add(bx, by, 0), yawB, poseB, tick, 0);
        if (tick % 20 == 0) ShowStageLighting.theatre(player, s, cue >= 'M' ? 5 : cue >= 'E' ? 2 : 0, tick);
        return true;
    }
    static void hide(ServerPlayer p, VisitData.Scene s) {
        for (var prop : p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,
                new net.minecraft.world.phys.AABB(TheatreSite.center(p, s)).inflate(45),
                e -> e.owned(s) && e.kind() == CinematicPropEntity.OCEAN_SET)) prop.frame(-1, 0, 0);
        for (String id : new String[]{"other_miss_spink", "other_miss_forcible"}) {
            var actor = ShowActors.actor(p, s, id); if (actor != null) actor.setInvisible(false);
        }
    }
    private NauticalOpeningShow() {}
}
