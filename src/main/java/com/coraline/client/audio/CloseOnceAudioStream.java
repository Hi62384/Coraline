package com.coraline.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/** The engine, failed future and cancelled preparation may converge on the same native decoder. */
public final class CloseOnceAudioStream implements AudioStream {
    private final AudioStream delegate;
    private final AtomicBoolean closed = new AtomicBoolean();
    public CloseOnceAudioStream(AudioStream delegate) { this.delegate = delegate; }
    @Override public AudioFormat getFormat() { return delegate.getFormat(); }
    @Override public ByteBuffer read(int bytes) throws IOException {
        if (closed.get()) throw new IOException("Decoded show stream already closed");
        return delegate.read(bytes);
    }
    @Override public void close() throws IOException { if (closed.compareAndSet(false, true)) delegate.close(); }
}
