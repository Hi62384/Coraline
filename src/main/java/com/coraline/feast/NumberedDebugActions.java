package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import com.coraline.secrets.*;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Server-owned dispatcher. Shared-controller hooks below preserve their private ownership/reward invariants. */
public final class NumberedDebugActions {
    private static final Map<String, List<String>> VOICES = voices();
    private static Map<String, List<String>> voices() {
        var result = new LinkedHashMap<String, List<String>>();
        try (var input = NumberedDebugActions.class.getResourceAsStream("/assets/coraline/numbered_event_dialogue.json")) {
            if (input == null) return Map.of();
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                for (var e : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                    int slash = e.getKey().indexOf('/'); if (slash < 1) continue;
                    result.computeIfAbsent(e.getKey().substring(0, slash), ignored -> new ArrayList<>()).add(e.getKey().substring(slash + 1));
                }
            }
        } catch (java.io.IOException | RuntimeException unavailable) { return Map.of(); }
        result.replaceAll((key, value) -> List.copyOf(value)); return Map.copyOf(result);
    }
    static String event(String section) { return switch (section) { case "house" -> "house_party"; case "player" -> "player_party"; case "away" -> "mother_goes_out"; case "theatre" -> "theatre"; default -> "mouse_circus"; }; }
    private static VisitEvents.Type type(String section) { return switch (section) { case "house" -> VisitEvents.Type.HOUSE_PARTY; case "player" -> VisitEvents.Type.PLAYER_PARTY; case "away" -> VisitEvents.Type.OTHER_MOTHER_GOES_OUT; case "theatre" -> VisitEvents.Type.THEATRE; default -> VisitEvents.Type.BOBINSKY_MOUSE_CIRCUS; }; }
    public static int integer(String value, int min, int max) {
        if (!value.matches("[0-9]{1,6}")) throw new IllegalArgumentException("use a whole number from " + min + " to " + max);
        int parsed = Integer.parseInt(value); if (parsed < min || parsed > max) throw new IllegalArgumentException("range " + min + "–" + max); return parsed;
    }
    public static boolean operator(ServerPlayer p, VisitData.Scene s) { return s != null && NumberedDebugServer.authorized(p) == s && p.getUUID().equals(s.owner); }
    public static String run(ServerPlayer p, VisitData.Scene s, String section, String action, String value) {
        if (!operator(p, s)) return "Owner/operator scene validation failed.";
        if(StoryContinuationDebug.section(section))return StoryContinuationDebug.run(p,s,section,action);
        if(section.equals("spink"))return MissingParentsTea.debug(p,s,action);
        if(FirstEscapeStory.debugSection(section))return FirstEscapeStory.debug(p,s,action,value);
        if(section.equals("traditional"))return BlackIsTraditional.debug(p,s,action,value);
        if (action.equals("state")) return "State refreshed. Open Details for clocks, actors, blockers and roster indexes.";
        if (action.equals("voices")) { s.loop.event.putString("DebugVoiceList", event(section)); return "Open Details for all authored voice IDs and recording status."; }
        if (action.equals("voice")) return preview(p, s, section, value);
        if (action.equals("force")) {
            if (s.loop.secrets.searching() || Reconciliation.active(p) || SecretDialogue.busy(p)) return "Finish the active pursuit or dialogue before forcing another event.";
            if (!OtherWorld.isOther(p.level())) return "Return to the real house before starting another event.";
            VisitEvents.start(p, s, type(section)); OtherMotherLoop.changed(p);
            return VisitEvents.type(s) == type(section) ? "Start requested through the visit director. Preparation/recording gates remain active." : "Event start blocked by preparation, eligibility, cleanup or recording gates.";
        }
        if (section.equals("theatre")) { TheatreVisit.debug(p, action); return "Theatre control checked by its owner controller. See Details and chat for any blocker."; }
        if (section.equals("circus")) { MouseCircusVisit.debug(p, action); return "Circus control checked by its owner controller. See Details and chat for any blocker."; }
        if (VisitEvents.type(s) != type(section)) return "Start the selected event first; the current event is " + VisitEvents.type(s) + ".";
        if (section.equals("away")) return MotherGoesOut.debugOperator(p, s, action, value);
        if (HouseParty.active(p) != s) return "No current owned party is active here.";
        var n = HouseParty.data(s);
        if (List.of("guests", "special", "count", "wander", "group").contains(action)) return PartyGuests.debugOperator(p, s, action, value);
        if (List.of("conversation", "secret", "wager").contains(action)) return PartyConversations.debugOperator(p, s, action, value);
        if (List.of("warning", "report", "grace", "search").contains(action)) return PartySurveillance.debugOperator(p, s, action, value);
        if (List.of("win", "loss", "draw").contains(action)) return PartyMatches.debugOperator(p, s, action);
        if (action.equals("cancel_match")) { PartyMatches.suspend(p, s); return "Current match cancelled through its escrow-refund path."; }
        if (action.equals("absence")) {
            int ticks = integer(value, 0, 12000);
            if (PartyRegions.allowed(p, p.position()) || s.loop.secrets.searching()) return "Countdown requires the owner outside permitted grounds and no active search.";
            n.putInt("AbsenceRemaining", ticks); OtherMotherLoop.changed(p); return "Absence countdown set to " + ticks + " ticks.";
        }
        if (action.equals("room")) {
            int index = integer(value, 0, PartyRegions.rooms().size() - 1); var realm = SecretWorld.realm(p);
            if (realm == null || realm.house == null || !OtherWorld.isOther(p.level())) return "Return to your Other House first.";
            return teleport(p, PartyRegions.rooms().get(index).center(realm.house));
        }
        if (action.equals("areas")) return areas(p);
        if (action.equals("outside")) return outside(p);
        if (action.equals("game")) {
            int game = integer(value, 0, TabletopGames.Game.values().length - 1);
            var nearby = PartyGuests.guests(s).stream().filter(g -> { var a = PartyGuests.entity(p, g); return a != null && a.level() == p.level() && a.distanceToSqr(p) <= 100 && a.hasLineOfSight(p) && g.getString("State").equals("WANDERING"); }).limit(1).toList();
            return PartyMatches.start(p, s, nearby, TabletopGames.Game.values()[game], null) ? "Real non-wager match started; use the ordinary game interface." : "Needs an available nearby guest, safe table and chairs, and an uninterrupted social phase.";
        }
        if (action.equals("resume") && (s.loop.secrets.searching() || Reconciliation.active(p) || SecretDialogue.busy(p))) return "Resume is blocked until actual pursuit/reconciliation finishes.";
        if (section.equals("player")) return PlayerParty.debugOperator(p, s, action, value);
        switch (action) {
            case "feast" -> { if (s.loop.phase != OtherMotherLoopState.Phase.PARTY_SOCIAL) return "Feast advance requires the social phase."; HouseParty.debug(p, "feast"); return "Feast is due; active games, announcement and seating still run normally."; }
            case "seats" -> { if (s.loop.phase != OtherMotherLoopState.Phase.PARTY_FEAST_CALL && s.loop.phase != OtherMotherLoopState.Phase.PARTY_FEAST) return "Wait for the feast call before placing banquet seating."; return PartyBanquet.prepare(p, s) ? "Banquet prepared through safe seating checks." : "Safe seating capacity is unavailable."; }
            case "food_warning" -> { return previewPrefix(p, s, "house", "food"); }
            case "finish" -> { if (s.loop.phase != OtherMotherLoopState.Phase.PARTY_FEAST) return "Only an actual active feast can be ended."; HouseParty.debug(p, "finish"); return "Feast ending requested; guest departure still runs normally."; }
            case "resume" -> { HouseParty.resume(p); return "Party resume requested after reconciliation."; }
            case "complete" -> { if (s.loop.phase != OtherMotherLoopState.Phase.PARTY_ENDING || !n.getBoolean("FeastAnnounced") || n.getBoolean("BanquetFailed") || n.getInt("FeastElapsed") < n.getInt("FeastDuration")) return "Completion requires a real completed feast and the ending phase."; HouseParty.tick(p, s); return "Normal ending tick ran; closing dialogue and guest departure remain required."; }
            default -> { return "No supported control for this event."; }
        }
    }
    static String previewPrefix(ServerPlayer p, VisitData.Scene s, String section, String prefix) {
        String state = SecretDialogue.stateId(SecretDialogue.state(p));
        String id = VOICES.getOrDefault(event(section), List.of()).stream().filter(x -> x.startsWith(prefix + "_" + state + "_")).findFirst().orElse("");
        return id.isEmpty() ? "No authored line matches the current state." : preview(p, s, section, id);
    }
    private static String preview(ServerPlayer p, VisitData.Scene s, String section, String id) {
        String event = event(section);
        if (!VOICES.getOrDefault(event, List.of()).contains(id)) return "Unknown authored line ID; use Voice list.";
        if (SecretDialogue.busy(p) || Reconciliation.active(p) || s.loop.secrets.searching()
                || !s.loop.event.getString("Voice").isEmpty()
                || HouseParty.data(s).getLong("Clock") < HouseParty.data(s).getLong("VoiceUntil")
                || HouseParty.data(s).getLong("Clock") < HouseParty.data(s).getLong("HostVoiceUntil")
                || OtherMotherLoop.now(p) < s.loop.event.getLong("DebugVoiceUntil")) return "Wait for the current dialogue or preview to finish.";
        var line = NumberedEventDialogue.line(event, id); int duration = NumberedEventDialogue.say(p, event, id);
        if (duration <= 0) return "Original/generated recording is unavailable: " + line.sound();
        s.loop.event.putLong("DebugVoiceUntil", OtherMotherLoop.now(p) + duration);
        if (HouseParty.active(p) == s) { var party = HouseParty.data(s); party.putLong("VoiceUntil", party.getLong("Clock") + duration); party.putLong("HostVoiceUntil", party.getLong("Clock") + duration); }
        OtherMotherLoop.changed(p);
        return "Preview: " + id + (line.sound().isEmpty() ? " (authored text-only speaker)." : " (registered complete recording).");
    }
    static String teleport(ServerPlayer p, BlockPos at) {
        if (at == null || !p.serverLevel().hasChunkAt(at)) return "Target is unavailable or unloaded; no guessed teleport was used.";
        var safe = Reconciliation.safe(p, at); if (safe == null) return "No safe player landing position exists at the authored target.";
        p.stopRiding(); p.teleportTo(p.serverLevel(), safe.getX() + .5, safe.getY(), safe.getZ() + .5, p.getYRot(), p.getXRot()); p.fallDistance = 0;
        return "Moved to verified safe target " + safe.toShortString() + ".";
    }
    private static String outside(ServerPlayer p) {
        var realm = SecretWorld.realm(p); if (realm == null || realm.house == null || !OtherWorld.isOther(p.level())) return "Return to your house dimension first.";
        for (int distance : new int[]{36, 48, 64, 80}) for (int sign : new int[]{-1, 1}) {
            var target = realm.house.offset(sign * distance, 0, 0); if (!p.serverLevel().hasChunkAt(target)) continue;
            var safe = Reconciliation.safe(p, target); if (safe != null && !PartyRegions.allowed(p, Vec3.atBottomCenterOf(safe))) return teleport(p, safe);
        }
        return "No loaded, safe position just outside the authored grounds was found.";
    }
    private static String areas(ServerPlayer p) {
        var realm = SecretWorld.realm(p); if (realm == null || realm.house == null || !OtherWorld.isOther(p.level())) return "House boundaries are available in your Other House.";
        int count = 0;
        for (var room : PartyRegions.rooms()) {
            var b = room.bounds(realm.house);
            for (int i = 0; i < 8 && count < 192; i++, count++) {
                double x = (i & 1) == 0 ? b.minX : b.maxX, y = (i & 2) == 0 ? b.minY : b.maxY, z = (i & 4) == 0 ? b.minZ : b.maxZ;
                p.serverLevel().sendParticles(p, ParticleTypes.END_ROD, true, x, y, z, 1, 0, 0, 0, 0);
            }
        }
        return "Marked " + count + " authored room corners for this operator only; private rooms remain excluded.";
    }
    public static List<String> inspect(ServerPlayer p, VisitData.Scene s, String section) {
        if(StoryContinuationDebug.section(section))return StoryContinuationDebug.inspect(s,section);
        if(section.equals("spink"))return MissingParentsTea.inspect(s);
        if(FirstEscapeStory.debugSection(section))return FirstEscapeStory.inspect(p,s);
        if(section.equals("traditional")){var n=BlackIsTraditional.data(s);String voice=BlackIsTraditional.sound(BlackIsTraditional.phase(s));return List.of("Story "+n.getString("Phase")+" | started "+n.getBoolean("Started"),"Recording "+voice+" | duration "+ShowRecordings.duration(voice)+" | playback "+ShowPlayback.position(s),"Player seated "+p.isPassenger()+" | sleeping "+p.isSleeping(),"Mother "+n.getLong("MotherPos")+" | Father "+n.getLong("FatherPos"),"Gift open "+n.getBoolean("Opened")+" | needle dropped "+n.getBoolean("NeedleDropped"),"Dark house "+n.getBoolean("Dark")+" | saved lights "+n.getList("Lights",10).size(),"Study locked "+n.getBoolean("StudyLocked")+" | sleep disabled "+n.getBoolean("SleepDisabled"),"Endpoint "+n.getBoolean("StudyRevealComplete")+" | blocker "+n.getString("Blocker"));}
        var out = new ArrayList<String>();
        out.add("Owner " + p.getGameProfile().getName() + " | current event " + VisitEvents.type(s) + " | serial " + s.loop.event.getInt("Serial"));
        out.add("Phase " + s.loop.phase + " | trust " + s.loop.trust + " | suspicion " + s.loop.suspicion);
        out.add("Realm " + s.door + " | dimension " + p.level().dimension().location());
        out.add("Search " + s.loop.secrets.search + " | reconciliation " + Reconciliation.active(p));
        out.add("Selected eligibility " + VisitEvents.eligible(p, s, type(section)) + " | authored audio ready " + NumberedEventDialogue.ready(event(section)));
        if (section.equals("house") || section.equals("player")) {
            var n = HouseParty.data(s);
            out.add("Party active " + (HouseParty.active(p) == s) + " | player phase " + n.getString("PlayerPhase") + " | permitted " + PartyRegions.allowed(p, p.position()));
            out.add("Until 20-minute call " + Math.max(0, 24000 - n.getInt("SocialElapsed")) + " ticks | absence " + (n.contains("AbsenceRemaining") ? n.getInt("AbsenceRemaining") : "not running"));
            out.add("Guest count " + PartyGuests.guests(s).size() + " | capacity " + n.getInt("Capacity") + " | secret claims " + n.getInt("SecretClaims"));
            out.add("Feast " + n.getInt("FeastElapsed") + "/" + n.getInt("FeastDuration") + " | announced " + n.getBoolean("FeastAnnounced") + " | banquet failed " + n.getBoolean("BanquetFailed"));
            var match = n.getCompound("Match"); out.add("Match active " + PartyMatches.active(s) + " | game " + match.getCompound("Game").getString("Game") + " | escrow " + match.hasUUID("Escrow") + " | outcome " + match.getString("Outcome"));
            int i = 0; for (var guest : PartyGuests.guests(s)) {
                if (i >= 32) break; var actor = PartyGuests.entity(p, guest);
                out.add("Guest " + i++ + ": " + guest.getString("Name") + " | " + guest.getString("State") + " | loaded " + (actor != null));
                if (guest.hasUUID("Group")) out.add("  Group " + guest.getUUID("Group") + " until " + guest.getLong("GroupUntil"));
                if (guest.getBoolean("ReportPending") || guest.contains("WarningUntil")) out.add("  Warning until " + guest.getLong("WarningUntil") + " | report " + guest.getBoolean("ReportPending") + " | observed " + guest.getString("ObservedOffense"));
            }
            for (i = 0; i < PartyRegions.rooms().size(); i++) out.add("Room " + i + ": " + PartyRegions.rooms().get(i).label());
            out.add("Blocker: " + n.getString("PreparationBlocker"));
            if (PlayerParty.isPlayerParty(s)) PlayerParty.debugInspect(p, s, out);
        } else if (section.equals("away")) {
            var n = MotherGoesOut.data(s); out.add("Away pending " + MotherGoesOut.pending(s) + " | stage " + n.getString("Stage") + " | offsite " + n.getBoolean("Offsite"));
            out.add("Hidden remaining " + n.getInt("Remaining") + " / selected total " + n.getInt("Duration") + " ticks | early " + n.getBoolean("Early"));
            out.add("Home nearby " + MotherGoesOut.nearHome(p) + " | home checked " + n.getBoolean("HomeChecked") + " | route " + n.getInt("RouteIndex") + " | stall " + n.getInt("RouteStall"));
            out.add("Reports " + n.getList("Reports", Tag.TAG_COMPOUND).size() + " | actionable " + n.getBoolean("ActionableReport"));
            for (var tag : n.getList("Reports", Tag.TAG_COMPOUND)) { var r = (CompoundTag)tag; out.add(r.getString("Source") + ": " + r.getString("Offense") + " at " + BlockPos.of(r.getLong("Position")).toShortString()); }
        } else {
            boolean theatre = section.equals("theatre"); var n = s.loop.event.getCompound(theatre ? "Theatre" : "MouseCircus"); var audio = s.loop.event.getCompound("ShowPlayback");
            out.add("Show phase " + n.getString("Phase") + " | checkpoint " + n.getString("Checkpoint") + " | segment " + n.getInt("Segment"));
            out.add("Timeline cue " + n.getString("Cue") + " | stunt attachment " + s.loop.event.getBoolean("ShowStunt"));
            if (theatre && TheatreVisit.pending(s)) {
                out.add("Reserved seat " + TheatreSite.playerSeat(p, s) + " | saved return " + s.loop.event.contains("TheatreReturn"));
                for (String id : List.of("other_miss_spink", "other_miss_forcible")) { var actor = ShowActors.actor(p, s, id); out.add(id + ": " + (actor == null ? "unloaded/unavailable" : actor.blockPosition().toShortString())); }
            } else if (!theatre && MouseCircusVisit.pending(s) && MouseCircusSite.inside(p, s)) out.add("Active mouse actors " + MouseCircusChoreography.count(p, s, MouseCircusSite.center(p, s)) + " | formation revealed " + n.getBoolean("Revealed"));
            out.add("Arrival " + s.loop.event.getInt("ArrivalRemaining") + " | grace " + s.loop.event.getInt("GraceRemaining") + " | attended " + s.loop.event.getBoolean("ShowAttended"));
            out.add("Audio " + audio.getString("Sound") + " | position " + audio.getInt("Position") + "/" + audio.getInt("Duration") + " ticks");
            out.add("Playback started " + audio.getBoolean("Started") + " | decoded end " + audio.getBoolean("Ended") + " | server finished " + audio.getBoolean("Finished"));
            out.add("Performance done " + n.getBoolean("PerformanceDone") + " | returned " + n.getBoolean("Returned") + " | completed " + s.loop.story.getBoolean(theatre ? "TheatreCompleted" : "MouseCircusCompleted"));
            out.add("Wybie eligible " + WybieOptionalVisits.allowed(s) + " | leased " + n.getCompound("Wybie").hasUUID("Actor"));
            out.add("Debug preview " + n.getString("DebugPreview") + " | paused " + n.getBoolean("DebugPaused") + " | exit-only " + n.getBoolean("DebugExitOnly"));
            out.add("Blocker: " + n.getString("Blocker")); out.add("Audio blocker: " + audio.getString("Blocker"));
            out.add("Seeking DISABLED: native Forge 47.4.23 and real decoded-offset continuity tests are pending.");
            out.add("Visual previews are paused poses, never audio-offset seeks or completion checkpoints.");
        }
        if (s.loop.event.getString("DebugVoiceList").equals(event(section))) {
            for (String id : VOICES.getOrDefault(event(section), List.of())) {
                var line = NumberedEventDialogue.line(event(section), id);
                out.add(id + " | " + (line.sound().isEmpty() ? "authored text only" : NumberedEventDialogue.recordedTicks(line.sound()) > 0 ? "recorded" : "MISSING AUDIO"));
            }
        }
        return out.stream().limit(NumberedDebugNetwork.MAX_ROWS).toList();
    }
    private NumberedDebugActions() {}
}
