package com.coraline.feast;

import java.util.ArrayList;
import java.util.List;

/** Shared allowlist; labels never become executable commands. All arguments are validated again server-side. */
public final class NumberedDebugControls {
    public record Control(String id, String label, String argument, String blocker) {}
    public static final List<String> SECTIONS = List.of("house", "player", "away", "theatre", "circus", "traditional", "cocoa", "prison", "escape", "missing", "spink", "cat_story", "parent_rescue", "rescue_chapter");
    private static Control c(String id, String label) { return new Control(id, label, "", ""); }
    private static Control a(String id, String label, String argument) { return new Control(id, label, argument, ""); }
    public static String title(String section) {
        return switch (section) {
            case "cat_story" -> "Missing Parents: Cat and Mirror";case "parent_rescue" -> "Return to the Other World";case "rescue_chapter" -> "Ghost Eyes, Beldam and Farewell";
            case "spink" -> "Missing Parents: Spink and Forcible";
            case "cocoa" -> "Cocoa Beetles";case "prison" -> "Mirror Prison";case "escape" -> "First Escape";case "missing" -> "Missing Parents";
            case "house" -> "House Party"; case "player" -> "Player Party";
            case "away" -> "Other Mother Goes Out"; case "theatre" -> "Theatre";
            case "circus" -> "Bobinsky Mouse Circus";case "traditional" -> "Black Is Traditional"; default -> "Numbered events";
        };
    }
    public static List<Control> controls(String section) {
        if(StoryContinuationDebug.section(section))return StoryContinuationDebug.actions(section).entrySet().stream().map(e->c(e.getKey(),e.getValue())).toList();
        if(section.equals("spink"))return MissingParentsTea.debugActions().entrySet().stream().map(e->c(e.getKey(),e.getValue())).toList();
        if(FirstEscapeStory.debugSection(section)){var controls=new ArrayList<Control>(List.of(c("state","Inspect phase, actors, audio and saved flags"),c("begin","Begin after the real rejected-button story endpoint"),c("recover","Replay current recording checkpoint safely"),c("scene","Go to the current story scene"),c("reset","Restore journalled environment and reset this continuation")));for(var entry:FirstEscapeStory.debugActions(section).entrySet())controls.add(c(entry.getKey(),entry.getValue()));for(var phase:FirstEscapeTimeline.Phase.values())if(FirstEscapeStory.debugSection(phase).equals(section))controls.add(c("phase_"+phase.name().toLowerCase(java.util.Locale.ROOT),"Preview "+phase.name().replace('_',' ')));return controls;}
        if(section.equals("traditional")){var controls=new ArrayList<Control>(List.of(c("trust","Set Trustness 1.0 (after current event)"),c("begin","Start at a safe event boundary"),c("state","Inspect story/checkpoints"),c("table","Go to dining room"),c("sit","Seat all three characters"),c("gift","Open gift and preview contents"),c("bed","Go to bedroom"),c("study","Go to study"),c("recover","Replay current safe recording checkpoint"),c("reset","Restore environment and reset story")));for(var phase:BlackIsTraditional.Phase.values())controls.add(c("phase_"+phase.name().toLowerCase(java.util.Locale.ROOT),"Preview "+phase.name().replace('_',' ')));return controls;}
        if (section.equals("theatre") || section.equals("circus")) {
            return (section.equals("theatre") ? ShowDebugControls.theatre() : ShowDebugControls.circus()).stream()
                    .map(x -> new Control(x.id(), x.label(), "", x.requiresVerifiedSeek() && !ShowSeekAvailability.productionReady() ? ShowSeekAvailability.blocker() : "")).toList();
        }
        if (section.equals("away")) return List.of(
                c("force", "Force event"), c("state", "Inspect hidden state"), a("voice", "Preview authored dialogue", "Exact line ID; use Voice list"), c("voices", "List all 64 dialogue IDs"),
                c("departure", "Play departure dialogue"), c("leave", "Start real departure route"), a("time", "Set absence remaining", "Ticks: 0–42000; absent stage only"),
                c("return", "Begin normal return"), c("home", "Go to the real house"), c("spy_report", "Submit actual witness report"), c("doll", "Spawn one test doll"), c("rat", "Spawn one test rat"),
                c("rat_detect", "Run real rat detection"), c("rat_intercept", "Intercept owned reporting rat"), c("early", "Early return from actual report"), c("navigation", "Retry return navigation"),
                c("home_check", "Check actual home presence"), c("missing", "Test missing-player search"), c("search", "Start Mother search"), c("bell", "Use real porch bell"),
                c("catch", "Test contact-guarded capture"), c("confrontation", "Test current confrontation"), c("complete", "Complete at valid checkpoint"));
        if (!section.equals("house") && !section.equals("player")) return List.of();
        var out = new ArrayList<Control>(List.of(c("force", "Force event"), c("state", "Inspect state and roster"), c("guests", "Advance eligible guest arrivals"),
                c("special", "Request eligible special guests"), a("room", "Go to authored party room", "Room index; see state"), c("areas", "Show permitted room boundaries"),
                c("wander", "Resume available guest wandering"), c("group", "Form real conversation group"), c("warning", "Warn for visible offense"), c("report", "Report visible ignored warning"),
                a("grace", "Set warning grace", "Ticks: 100–200"), c("outside", "Go just outside permitted grounds"), a("absence", "Set absence countdown", "Ticks: 0–12000; outside only"),
                c("search", "Start Mother search"), c("resume", "Resume after reconciliation"), a("conversation", "Open nearby guest conversation", "Guest roster index"),
                a("secret", "Test capped guest secret reward", "Guest roster index; genuine clue only"), a("game", "Start nearby guest board game", "Game index 0–5"),
                a("wager", "Open exact wager confirmation", "Guest roster index; inventory untouched until accepted"), c("win", "OP: settle current match as win"),
                c("loss", "OP: settle current match as loss"), c("draw", "OP: settle current match as draw"), c("cancel_match", "Cancel match and refund escrow"),
                c("voices", "List authored dialogue IDs"), a("voice", "Preview authored dialogue", "Exact line ID; use Voice list")));
        if (section.equals("house")) out.addAll(List.of(a("count", "Set House Party guest capacity", "6–20; never removes arrived guests"),
                c("feast", "Advance to the party feast"), c("seats", "Prepare real banquet seating"), c("food_warning", "Preview food warning"), c("finish", "End current feast"), c("complete", "End after required feast")));
        else out.addAll(List.of(c("backyard", "Go to authored backyard"), c("attic", "Go to attic circus"), c("theatre", "Go to real theatre doorway"), c("regenerate", "Validate/recover shared theatre"),
                c("bobinsky", "Request eligible Bobinsky show"), c("spink", "Request eligible theatre show"), c("enter", "Test basement/theatre entry"), c("basement", "Test normal basement fallback"),
                c("storage", "Test protected-storage warning"), c("finale", "Advance to 20-minute finale"), c("assemble", "Assemble actual attendees"), c("cake", "Present cake at valid checkpoint"),
                c("cheer", "Preview guest cheering"), a("gift", "Present one earned guest gift", "Guest roster index; actual attendance required"), c("deliver", "Deliver presented, unclaimed gifts"),
                c("inventory", "Inspect inventory delivery capacity"), c("duplicate", "Audit duplicate-claim protection"), c("complete", "Complete after real celebration")));
        return List.copyOf(out);
    }
    public static Control find(String section, String id) { return controls(section).stream().filter(c -> c.id().equals(id)).findFirst().orElse(null); }
    private NumberedDebugControls() {}
}
