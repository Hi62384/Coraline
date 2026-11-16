package com.coraline.client;

import com.coraline.feast.NumberedDebugControls;
import com.coraline.feast.NumberedDebugNetwork;
import com.coraline.feast.NumberedDebugSessions;
import com.coraline.feast.WybieAdventureNetwork;
import com.coraline.feast.ShowDebugControls;
import com.coraline.feast.ShowPlaybackNetwork;
import com.coraline.feast.ShowSeekAvailability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Additive public ScreenEvent integration. Never reads SecretJournalScreen private fields or duplicates Wybie's panel. */
@Mod.EventBusSubscriber(modid = "coraline", value = Dist.CLIENT)
public final class NumberedEventDebugClient {
    @SubscribeEvent public static void journal(ScreenEvent.Init.Post event) {
        var mc = Minecraft.getInstance();
        if (!(event.getScreen() instanceof SecretJournalScreen) || mc.player == null || !mc.player.hasPermissions(2)) return;
        var parent = event.getScreen();
        // Existing Wybie button occupies y=4 at the right edge.
        event.addListener(Button.builder(Component.literal("Numbered events"), button -> mc.setScreen(new Panel(parent)))
                .bounds(Math.max(4, parent.width - 130), 26, 126, 20).build());
    }
    public static void receive(NumberedDebugNetwork.Snapshot data) {
        var mc = Minecraft.getInstance();
        // Late packets must never reopen a closed screen, steal focus, or replace a guest conversation.
        if (mc.screen instanceof Panel panel) panel.receive(data);
    }
    private static final class Panel extends Screen {
        private final Screen parent;
        private NumberedDebugNetwork.Snapshot data;
        private UUID request;
        private String lastCommand = "", section = "house", argument = "", notice = "Opening operator session...";
        private boolean waiting, details;
        private int page, detailPage, ticks, waitingTicks, actionRows;
        private EditBox input;
        private final List<Button> locked = new ArrayList<>();
        Panel(Screen parent) { super(Component.literal("Secrets Debugger · Numbered events")); this.parent = parent; }
        @Override public boolean isPauseScreen() { return false; }
        @Override protected void init() {
            int left = Math.max(4, width / 2 - 215), span = Math.min(430, width - 8), tab = span / 6;
            locked.clear();
            for (int i = 0; i < NumberedDebugControls.SECTIONS.size(); i++) {
                String key = NumberedDebugControls.SECTIONS.get(i);
                String label = switch (key) { case "house" -> "House"; case "player" -> "Player Party"; case "away" -> "Mother Away"; case "theatre" -> "Theatre";case "circus" -> "Circus";case "traditional" -> "Buttons";case "cocoa" -> "Cocoa";case "prison" -> "Prison";case "escape" -> "Escape";case "missing" -> "Missing";default -> key; };
                addLocked(Button.builder(Component.literal(label), b -> { saveArgument(); section = key; page = detailPage = 0; details = false; send("select", ""); })
                        .bounds(left + i % 6 * tab, 24 + i / 6 * 22, tab - 2, 20).build(), true);
            }
            int footer = height - 26;
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(left, footer, 52, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Wybie"), b -> {
                closeSession(); Minecraft.getInstance().setScreen(parent);
                WybieAdventureNetwork.debug(NumberedDebugSessions.NONE, "open", "");
            }).bounds(left + 54, footer, 54, 20).build());
            addLocked(Button.builder(Component.literal(data != null && data.valid() ? "Refresh" : "Reopen"), b -> send(data != null && data.valid() ? "refresh" : "open", ""))
                    .bounds(left + 110, footer, 62, 20).build(), false);
            addRenderableWidget(Button.builder(Component.literal(details ? "Controls" : "Details"), b -> { saveArgument(); details = !details; rebuildWidgets(); })
                    .bounds(left + 174, footer, 62, 20).build());
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { saveArgument(); if (details) detailPage = Math.max(0, detailPage - 1); else page = Math.max(0, page - 1); rebuildWidgets(); })
                    .bounds(left + span - 58, footer, 28, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { saveArgument(); if (details) detailPage = Math.min(detailPages() - 1, detailPage + 1); else page = Math.min(controlPages() - 1, page + 1); rebuildWidgets(); })
                    .bounds(left + span - 28, footer, 28, 20).build());
            actionRows = Math.max(1, Math.min(6, (height - 186) / 22));
            if (!details) {
                input = new EditBox(font, left, 130, span, 20, Component.literal("Control argument")); input.setMaxLength(96); input.setValue(argument); addRenderableWidget(input);
                var controls = NumberedDebugControls.controls(section); int start = page * actionRows * 2;
                for (int i = start; i < Math.min(controls.size(), start + actionRows * 2); i++) {
                    var control = controls.get(i); int local = i - start;
                    String label = control.label();
                    boolean seekControl = (section.equals("theatre") || section.equals("circus")) && ShowDebugControls.needsSeek(section.equals("theatre"), control.id());
                    var displayedSeek = seekControl ? ShowPlaybackClient.seekSnapshot() : null;
                    var button = Button.builder(Component.literal(font.plainSubstrByWidth(label, span / 2 - 14)), b -> { saveArgument(); if (seekControl) sendSeek(control.id(), displayedSeek); else send(control.id(), control.argument().isEmpty() ? "" : argument); })
                            .bounds(left + local % 2 * (span / 2), 154 + local / 2 * 22, span / 2 - 2, 20).build();
                    String tooltip = control.label() + (control.argument().isEmpty() ? "" : "\n" + control.argument()) + (control.blocker().isEmpty() ? "" : "\n" + control.blocker());
                    button.setTooltip(Tooltip.create(Component.literal(tooltip)));
                    if (control.blocker().isEmpty()) addLocked(button, true); else { button.active = false; addRenderableWidget(button); }
                }
            } else input = null;
            if (data == null && request == null) send("open", "");
        }
        private void addLocked(Button button, boolean sessionRequired) {
            button.active = !waiting && (!sessionRequired || data != null && data.valid()); locked.add(button); addRenderableWidget(button);
        }
        private int controlPages() { return Math.max(1, (NumberedDebugControls.controls(section).size() + actionRows * 2 - 1) / (actionRows * 2)); }
        private int detailRows() { return Math.max(1, (height - 134) / 11); }
        private int detailPages() { return data == null ? 1 : Math.max(1, (data.rows().size() + detailRows() - 1) / detailRows()); }
        private void saveArgument() { if (input != null) argument = input.getValue(); }
        private void send(String command, String value) {
            if (waiting || Minecraft.getInstance().player == null || !Minecraft.getInstance().player.hasPermissions(2)) return;
            boolean opening = command.equals("open"); if (!opening && (data == null || !data.valid())) return;
            lastCommand = command; request = UUID.randomUUID(); waiting = true; waitingTicks = 0; for (var b : locked) b.active = false;
            NumberedDebugNetwork.request(new NumberedDebugNetwork.Request(request, opening ? NumberedDebugSessions.NONE : data.nonce(),
                    opening ? 0 : data.sequence(), section, command, value));
        }
        private void sendSeek(String command, ShowPlaybackNetwork.Bind displayed) {
            var player = Minecraft.getInstance().player;
            if (waiting || player == null || !player.hasPermissions(2) || data == null || !data.valid()) return;
            if (!ShowSeekAvailability.productionReady()) { notice = ShowSeekAvailability.blocker(); return; }
            // Never silently replace an old button's recording nonce with a newer binding.
            var current = ShowPlaybackClient.seekSnapshot();
            if (displayed == null || !displayed.equals(current) || !displayed.owner().equals(player.getUUID())) {
                notice = "Recording changed. Refresh the controls before requesting an offset."; return;
            }
            int tick = ShowDebugControls.seekTick(section.equals("theatre"), command, displayed.sound(), displayed.duration());
            if (tick < 0) { notice = "Start the required current recording before using this offset control."; return; }
            ShowPlaybackNetwork.requestSeek(displayed.nonce(), displayed.serial(), displayed.sound(), tick);
            notice = "Offset audition requested. Native stream acknowledgement is still required; story progress stays paused.";
        }
        void receive(NumberedDebugNetwork.Snapshot incoming) {
            if (!waiting || !incoming.request().equals(request) || !incoming.section().equals(section)) return;
            saveArgument(); data = incoming; if (!lastCommand.equals("refresh") || !incoming.valid()) notice = incoming.message(); waiting = false; rebuildWidgets();
        }
        @Override public void tick() {
            if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.hasPermissions(2)) { onClose(); return; }
            if (waiting && ++waitingTicks >= 100) {
                waiting = false; data = null; notice = "No reply. Reopen explicitly; no action is retried automatically."; rebuildWidgets();
            }
            if (++ticks % 40 == 0 && !waiting && data != null && data.valid()) send("refresh", "");
        }
        private void closeSession() {
            if (!waiting && data != null && data.valid()) NumberedDebugNetwork.request(new NumberedDebugNetwork.Request(UUID.randomUUID(), data.nonce(), data.sequence(), section, "close", ""));
            data = null; request = null;
        }
        @Override public void onClose() { closeSession(); Minecraft.getInstance().setScreen(parent); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            renderBackground(graphics); int left = Math.max(4, width / 2 - 215), span = Math.min(430, width - 8);
            graphics.drawString(font, title, left, 9, 0xFFE8CF92);
            String heading = NumberedDebugControls.title(section) + " · OP only" + (details ? " · details " + (detailPage + 1) + "/" + detailPages() : "") + (waiting ? " · waiting" : "");
            graphics.drawString(font, heading, left, 71, 0xFFE8CF92);
            var wrapped = font.split(Component.literal(notice), span);
            for (int i = 0; i < Math.min(3, wrapped.size()); i++) graphics.drawString(font, wrapped.get(i), left, 84 + i * 10, 0xFFFFCCAA);
            if (details && data != null) {
                int start = detailPage * detailRows();
                for (int i = start; i < Math.min(data.rows().size(), start + detailRows()); i++) graphics.drawString(font,
                        font.plainSubstrByWidth(data.rows().get(i), span), left, 120 + (i - start) * 11, 0xFFEDE2DA);
            } else graphics.drawString(font, "Argument (see each control's tooltip) · page " + (page + 1) + "/" + controlPages(), left, 118, 0xFFCCCCCC);
            super.render(graphics, mouseX, mouseY, partial);
        }
    }
    private NumberedEventDebugClient() {}
}
