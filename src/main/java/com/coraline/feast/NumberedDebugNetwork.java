package com.coraline.feast;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Separate operator-only channel; ordinary Secrets snapshots never contain hidden event timers. */
@Mod.EventBusSubscriber(modid = "coraline", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NumberedDebugNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("coraline", "numbered_event_debug"), () -> "1", "1"::equals, "1"::equals);
    public static final int MAX_ROWS = 256, MAX_TEXT = 256;
    public record Request(UUID request, UUID nonce, long sequence, String section, String command, String argument) {}
    public record Snapshot(UUID request, UUID nonce, long sequence, int remainingTicks, boolean valid,
                           String section, String message, List<String> rows) {
        public Snapshot { rows = List.copyOf(rows); }
    }
    private static boolean registered;
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) { event.enqueueWork(NumberedDebugNetwork::register); }
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> { b.writeUUID(m.request()); b.writeUUID(m.nonce()); b.writeLong(m.sequence()); b.writeUtf(m.section(), 16); b.writeUtf(m.command(), 32); b.writeUtf(m.argument(), 96); })
                .decoder(b -> new Request(b.readUUID(), b.readUUID(), b.readLong(), b.readUtf(16), b.readUtf(32), b.readUtf(96)))
                .consumerMainThread((m, c) -> { var p = c.get().getSender(); if (p != null) NumberedDebugServer.receive(p, m); }).add();
        CHANNEL.messageBuilder(Snapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NumberedDebugNetwork::encode).decoder(NumberedDebugNetwork::decode)
                .consumerMainThread((m, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.coraline.client.NumberedEventDebugClient.receive(m))).add();
    }
    private static void encode(Snapshot m, FriendlyByteBuf b) {
        b.writeUUID(m.request()); b.writeUUID(m.nonce()); b.writeLong(m.sequence()); b.writeInt(m.remainingTicks()); b.writeBoolean(m.valid());
        b.writeUtf(m.section(), 16); b.writeUtf(bounded(m.message()), MAX_TEXT);
        int count = Math.min(MAX_ROWS, m.rows().size()); b.writeVarInt(count);
        for (int i = 0; i < count; i++) b.writeUtf(bounded(m.rows().get(i)), MAX_TEXT);
    }
    private static Snapshot decode(FriendlyByteBuf b) {
        UUID request = b.readUUID(), nonce = b.readUUID(); long sequence = b.readLong(); int ticks = b.readInt(); boolean valid = b.readBoolean();
        String section = b.readUtf(16), message = b.readUtf(MAX_TEXT); int count = b.readVarInt();
        if (count < 0 || count > MAX_ROWS) throw new IllegalArgumentException("Unbounded debugger snapshot");
        var rows = new ArrayList<String>(count); for (int i = 0; i < count; i++) rows.add(b.readUtf(MAX_TEXT));
        return new Snapshot(request, nonce, sequence, ticks, valid, section, message, rows);
    }
    private static String bounded(String text) { return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT - 3) + "..."; }
    public static void request(Request request) { CHANNEL.sendToServer(request); }
    static void send(ServerPlayer player, Snapshot snapshot) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot); }
    private NumberedDebugNetwork() {}
}
