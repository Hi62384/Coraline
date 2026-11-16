package com.coraline.feast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread only. A permission check and a current binding are required on every request. */
public final class NumberedDebugSessions {
    public static final UUID NONE = new UUID(0, 0);
    public static final long LIFETIME_TICKS = 2400, IDLE_TICKS = 1200, ACTION_GAP = 4, OPEN_GAP = 20;
    public record Binding(UUID owner, UUID door, int serial, String event, String dimension) {}
    public record Ticket(UUID nonce, long sequence, long expires) {}
    private record Session(Binding binding, Ticket ticket, long last) {}
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> opened = new HashMap<>();

    public Ticket open(UUID owner, Binding binding, long now) {
        if (binding == null || !owner.equals(binding.owner())) return null;
        Long prior = opened.get(owner);
        if (prior != null && now >= prior && now - prior < OPEN_GAP) return null;
        opened.put(owner, now);
        var ticket = new Ticket(UUID.randomUUID(), 0, now + LIFETIME_TICKS);
        sessions.put(owner, new Session(binding, ticket, now - ACTION_GAP));
        return ticket;
    }

    /** Consumes a nonce before any side effect. No duplicate packet can execute twice. */
    public Ticket consume(UUID owner, Binding binding, UUID nonce, long sequence, long now) {
        Session old = sessions.get(owner);
        if (old == null) return null;
        if (!old.binding().equals(binding) || now < old.last() || now >= old.ticket().expires()
                || now - old.last() >= IDLE_TICKS) {
            sessions.remove(owner);
            return null;
        }
        if (!old.ticket().nonce().equals(nonce) || old.ticket().sequence() != sequence
                || now - old.last() < ACTION_GAP) return null;
        var next = new Ticket(UUID.randomUUID(), sequence + 1, old.ticket().expires());
        sessions.put(owner, new Session(binding, next, now));
        return next;
    }

    /** Authorized transitions may move the owner/change serial; unrelated changes invalidate the session. */
    public void rebind(UUID owner, Binding binding) {
        Session old = sessions.get(owner);
        if (old == null) return;
        if (binding == null || !owner.equals(binding.owner()) || !old.binding().door().equals(binding.door())) {
            sessions.remove(owner);
        } else sessions.put(owner, new Session(binding, old.ticket(), old.last()));
    }
    public Ticket ticket(UUID owner) { var s = sessions.get(owner); return s == null ? null : s.ticket(); }
    public void close(UUID owner) { sessions.remove(owner); }
    public void forget(UUID owner) { sessions.remove(owner); opened.remove(owner); }
    public void clear() { sessions.clear(); opened.clear(); }
}
