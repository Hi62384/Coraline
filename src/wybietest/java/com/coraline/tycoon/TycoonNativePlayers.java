package com.coraline.tycoon;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

/** GameTest-only saved-player fixture. Forge exposes an unmodifiable players
 * view. Register solely in saveAll's backing list, not the online UUID map:
 * these are not connected clients and must not drive the ambient care AI.
 * This does not replace the separate real connected-client acceptance test. */
final class TycoonNativePlayers {
    @SuppressWarnings("unchecked") private static List<ServerPlayer> savedPlayers(ServerPlayer p){try{var field=PlayerList.class.getDeclaredField("players");field.setAccessible(true);return (List<ServerPlayer>)field.get(p.server.getPlayerList());}catch(ReflectiveOperationException e){throw new IllegalStateException("GameTest saved-player fixture unavailable",e);}}
    static void add(ServerPlayer p){var list=savedPlayers(p);if(!list.contains(p))list.add(p);}
    static void remove(ServerPlayer p){savedPlayers(p).remove(p);}
    private TycoonNativePlayers(){}
}
