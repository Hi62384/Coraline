package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import com.coraline.secrets.Reconciliation;
import com.coraline.secrets.SecretsState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = "coraline")
public final class NumberedDebugServer {
    private static final NumberedDebugSessions SESSIONS = new NumberedDebugSessions();
    /** No target UUID is accepted from the client. This owner must also occupy their actual realm or owned venue. */
    static VisitData.Scene authorized(ServerPlayer p) {
        if (!p.hasPermissions(2) || p.isDeadOrDying()) return null;
        var story=OtherMotherLoop.scene(p);
        if(RescueChapter.owns(p,story)){if(OtherWorld.isOther(p.level())||TheatreSite.inside(p,story)||MouseCircusSite.inside(p,story))return story;var c=ShowSiteData.get(p.server).center(story.owner,false);var v=p.position();if(p.level().dimension().equals(ActivityWorld.KEY)&&v.x>=c.getX()+80&&v.x<=c.getX()+200&&v.z>=c.getZ()+40&&v.z<=c.getZ()+140&&v.y>=c.getY()-75&&v.y<=c.getY()+100)return story;var door=com.coraline.otherworld.DoorData.get(p.server).doors.get(story.door);if(door!=null&&com.coraline.parents.FamilyData.level(p.server,door.dimension)==p.serverLevel())return story;}
        if(FirstEscapeStory.engaged(story)&&p.getUUID().equals(story.owner)&&FirstEscapeTimeline.overworld(FirstEscapeStory.phase(story))){var door=com.coraline.otherworld.DoorData.get(p.server).doors.get(story.door);if(door!=null&&com.coraline.parents.FamilyData.level(p.server,door.dimension)==p.serverLevel())return story;}
        if(ParentRescueStory.started(story)&&p.getUUID().equals(story.owner)&&OtherWorld.isOther(p.level()))return story;
        var s = OtherMotherLoop.active(p);
        if (s == null || !p.getUUID().equals(s.owner)) return null;
        if (OtherWorld.isOther(p.level())) return s;
        if (TheatreVisit.pending(s) && TheatreSite.inside(p, s)
                || MouseCircusVisit.pending(s) && MouseCircusSite.inside(p, s)
                || PlayerParty.active(p) == s) return s;
        return null;
    }
    private static NumberedDebugSessions.Binding binding(ServerPlayer p, VisitData.Scene s) {
        return s == null ? null : new NumberedDebugSessions.Binding(p.getUUID(), s.door, s.loop.event.getInt("Serial"),
                s.loop.event.getString("Type"), p.level().dimension().location().toString());
    }
    public static void receive(ServerPlayer p, NumberedDebugNetwork.Request request) {
        if (!p.hasPermissions(2)) { SESSIONS.forget(p.getUUID()); return; }
        if (!NumberedDebugControls.SECTIONS.contains(request.section()) || request.argument().length() > 96) return;
        var s = authorized(p); long now = OtherMotherLoop.now(p);
        if (s == null) { SESSIONS.close(p.getUUID()); failure(p, request, "Requires your active Other World visit or your owned event venue."); return; }
        NumberedDebugSessions.Ticket ticket;
        if (request.command().equals("open")) {
            if (!request.nonce().equals(NumberedDebugSessions.NONE) || request.sequence() != 0 || !request.argument().isEmpty()) return;
            ticket = SESSIONS.open(p.getUUID(), binding(p, s), now);
        } else ticket = SESSIONS.consume(p.getUUID(), binding(p, s), request.nonce(), request.sequence(), now);
        if (ticket == null) { failure(p, request, "Session expired, changed, replayed or rate limited. Reopen the debugger."); return; }
        if (request.command().equals("close")) { SESSIONS.close(p.getUUID()); return; }
        String message = "Operator-only controls. Changes affect your current storyline.";
        if (!List.of("open", "refresh", "select").contains(request.command())) {
            var control = NumberedDebugControls.find(request.section(), request.command());
            if (control == null) message = "Unknown control; no changes made.";
            else if (!control.blocker().isEmpty()) message = control.blocker();
            else if (control.argument().isEmpty() && !request.argument().isEmpty()) message = "This control takes no argument.";
            else if (!FirstEscapeStory.engaged(s)&&(s.loop.suspicion >= 1 || s.loop.secrets.search == SecretsState.Search.SCRIPTED_TRAP)) message = "The major trapping storyline has priority. Mutation controls are locked.";
            else {
                try { message = NumberedDebugActions.run(p, s, request.section(), request.command(), request.argument()); }
                catch (IllegalArgumentException bad) { message = "Invalid control argument: " + bad.getMessage(); }
                catch (RuntimeException failure) {
                    System.getLogger("coraline.numbered_debug").log(System.Logger.Level.ERROR, "Operator control failed: " + request.section() + "/" + request.command(), failure);
                    message = "Control failed; no automatic retry. Inspect current state and the server log before continuing.";
                }
            }
        }
        var current = authorized(p);
        SESSIONS.rebind(p.getUUID(), binding(p, current)); ticket = SESSIONS.ticket(p.getUUID());
        if (ticket == null || current == null) { failure(p, request, message + " Session ended with the visit transition."); return; }
        NumberedDebugNetwork.send(p, new NumberedDebugNetwork.Snapshot(request.request(), ticket.nonce(), ticket.sequence(),
                (int)Math.max(0, ticket.expires() - now), true, request.section(), message,
                NumberedDebugActions.inspect(p, current, request.section())));
    }
    private static void failure(ServerPlayer p, NumberedDebugNetwork.Request request, String message) {
        NumberedDebugNetwork.send(p, new NumberedDebugNetwork.Snapshot(request.request(), NumberedDebugSessions.NONE, 0, 0,
                false, request.section(), message, List.of()));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.forget(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
    private NumberedDebugServer() {}
}
