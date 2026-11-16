package com.coraline.client;

import com.coraline.client.audio.FrameOffsetAudioStream;
import com.coraline.client.audio.CloseOnceAudioStream;
import com.coraline.client.audio.RecordingFingerprint;
import com.coraline.feast.ShowPlaybackNetwork;
import com.coraline.feast.ShowSeekAvailability;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client lifecycle of one server-issued offset audition. No client cursor controls story state. */
@Mod.EventBusSubscriber(modid = "coraline", value = Dist.CLIENT)
public final class ShowSeekClient {
    private static final ThreadPoolExecutor PREPARE = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1), runnable -> {
            Thread thread = new Thread(runnable, "Coraline decoded show seek");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    private static Token current;

    public interface Streamed { Token seekToken(); }
    public static final class Token {
        final ShowPlaybackNetwork.SeekStart command;
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean failed = new AtomicBoolean();
        volatile FrameOffsetAudioStream stream;
        SoundInstance sound;
        boolean started, ended;
        Token(ShowPlaybackNetwork.SeekStart command) { this.command = command; }
        public boolean cancelled() { return cancelled.get(); }
    }

    static void start(ShowPlaybackNetwork.SeekStart command) {
        cancelAll();
        if (!ShowSeekAvailability.productionReady()) { status(command, 2); return; }
        Token token = new Token(command);
        current = token;
        token.sound = VisitAudioClient.playSeek(command, token);
        if (token.sound == null) failed(token);
    }

    static boolean accepts(Token token,Sound sound) {
        var media=ShowSeekAvailability.media(token.command.sound());
        return media!=null&&sound!=null&&sound.shouldStream()&&sound.getPath().toString().equals(media.resource());
    }
    static void rejected(Token token){failed(token);}
    /** Forge SoundInstance#getStream extension; SoundEngine calls this only for streamed entries. */
    static CompletableFuture<AudioStream> open(Token token, SoundBufferLibrary library, Sound sound, boolean looping) {
        ShowSeekAvailability.Media media = ShowSeekAvailability.media(token.command.sound());
        if (media == null || looping || !sound.shouldStream() || !sound.getPath().toString().equals(media.resource())) {
            failed(token);
            return CompletableFuture.failedFuture(new IOException("Seek requires the fixed verified streaming recording"));
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        // Pin the actual selected resource bytes before reopening through vanilla's decoder.
        // A reload invalidates this token via SoundEngineLoadEvent; no pack override is trusted.
        CompletableFuture<Void> fingerprint = submit(() -> {
            try (var input = Minecraft.getInstance().getResourceManager().open(sound.getPath())) {
                RecordingFingerprint.verify(input, media.compressedBytes(), media.runtimeSha256(), token::cancelled, deadline);
            }
            return null;
        });
        CompletableFuture<AudioStream> result = fingerprint.thenCompose(ignored -> {
            if (token.cancelled()) return CompletableFuture.failedFuture(new IOException("Seek cancelled before decoder open"));
            // Never cancel this future: if opening completes late, the callback must close its stream.
            return library.getStream(sound.getPath(), false).thenCompose(rawSource -> {
                AudioStream source = new CloseOnceAudioStream(rawSource);
                CompletableFuture<AudioStream> prepared = submit(() -> {
                    FrameOffsetAudioStream offset = null;
                    try {
                        offset = new FrameOffsetAudioStream(source, token::cancelled, media.decodedFrames());
                        var format = offset.getFormat();
                        if ((int) format.getSampleRate() != media.sampleRate() || format.getChannels() != media.channels())
                            throw new IOException("Decoded format differs from verified original export");
                        offset.prepare(media.frameAtTick(token.command.offsetTick()), deadline);
                        if (token.cancelled()) throw new IOException("Seek cancelled after preparation");
                        token.stream = offset;
                        if (token.cancelled()) throw new IOException("Seek cancelled as preparation completed");
                        return offset;
                    } catch (IOException | RuntimeException error) {
                        try { if (offset != null) offset.close(); else source.close(); }
                        catch (IOException suppressed) { error.addSuppressed(suppressed); }
                        throw error;
                    }
                });
                // Queue rejection must close a source that vanilla already opened.
                prepared.whenComplete((ready, error) -> {
                    if (error != null) try { source.close(); } catch (IOException ignoredClose) {}
                });
                return prepared;
            });
        });
        result.whenComplete((stream, error) -> {
            if (error != null) failed(token);
            if (stream != null && token.cancelled()) try { stream.close(); } catch (IOException ignored) {}
        });
        return result;
    }

    private static <T> CompletableFuture<T> submit(Callable<T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try { PREPARE.execute(() -> {
            try { result.complete(action.call()); }
            catch (Exception error) { result.completeExceptionally(error); }
        }); }
        catch (RejectedExecutionException busy) { result.completeExceptionally(busy); }
        return result;
    }

    @SubscribeEvent public static void sourceStarted(PlayStreamingSourceEvent event) {
        if (!(event.getSound() instanceof Streamed seek)) return;
        Token token = seek.seekToken();
        Minecraft.getInstance().execute(() -> {
            if (token != current || token.cancelled() || token.failed.get() || token.stream == null || token.started) return;
            token.started = true;
            VisitAudioClient.seekStarted(token.sound,token.command.offsetTick());
            status(token.command, 0); // The engine has attached the offset stream and called channel.play().
        });
    }
    static boolean pending(SoundInstance sound) {
        Token token=current;
        return token!=null&&token.sound==sound&&!token.cancelled()&&!token.failed.get()&&!token.started;
    }
    static boolean actuallyStarted(SoundInstance sound) {
        return !(sound instanceof Streamed seek)||seek.seekToken().started;
    }
    static void observed(SoundInstance sound, boolean active) {
        Token token = current;
        if (token == null || token.sound != sound || token.cancelled() || token.ended || !token.started) return;
        if (!active) {
            token.ended = true;
            if (token.stream != null && token.stream.decoderEof()) status(token.command, 1); else failed(token);
        }
    }
    private static void failed(Token token) {
        if (!token.failed.compareAndSet(false, true)) return;
        Minecraft.getInstance().execute(() -> {
            if (token != current || token.cancelled()) return;
            status(token.command, 2);
            cancel(token.command.nonce());
        });
    }
    static void stopping(String sound, boolean music) {
        Token token = current;
        if (token != null && token.command.music() == music && (sound.isEmpty() || token.command.sound().equals(sound)))
            cancel(token.command.nonce());
    }
    public static void cancel(UUID nonce) {
        Token token = current;
        if (token == null || !token.command.nonce().equals(nonce)) return;
        current = null;
        token.cancelled.set(true);
        if (token.sound != null) Minecraft.getInstance().getSoundManager().stop(token.sound);
        // Do not close on the game thread while the decoder is executing. It sees the token
        // between <=16 KiB decode requests; queued/opening streams close on their completion path.
        if (token.stream != null) {
            try { PREPARE.execute(() -> { try { token.stream.close(); } catch (IOException ignored) {} }); }
            catch (RejectedExecutionException busy) { /* SoundEngine owns the attached stream and closes it on stop. */ }
        }
    }
    public static void cancelAll() { if (current != null) cancel(current.command.nonce()); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { cancelAll(); ShowPlaybackClient.clear(); }
    @SubscribeEvent public static void reload(SoundEngineLoadEvent event) {
        Minecraft.getInstance().execute(() -> { if (current != null) failed(current); });
    }
    private static void status(ShowPlaybackNetwork.SeekStart command, int status) {
        if(Minecraft.getInstance().player==null)return;
        ShowPlaybackNetwork.seekSignal(command.nonce(), command.serial(), command.sound(), status);
    }
    private ShowSeekClient() {}
}
