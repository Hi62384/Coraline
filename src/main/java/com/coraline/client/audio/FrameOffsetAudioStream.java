package com.coraline.client.audio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/**
 * A real decoded-stream offset: skipped PCM is never submitted to the audio channel.
 * Opens from the beginning for codec history, discards exactly N interleaved frames,
 * and retains the unused suffix of the decoder's last block. No whole-file PCM buffer.
 * This is O(offset) decode work, not compressed-byte seeking and not a clip restart.
 */
public final class FrameOffsetAudioStream implements AudioStream {
    public static final int DECODE_REQUEST_BYTES = 16 * 1024;
    public static final int MAX_DECODE_BLOCK_BYTES = 64 * 1024;
    public static final int MAX_OUTPUT_BYTES = 256 * 1024;
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);
    private final AudioStream source;
    private final AudioFormat format;
    private final BooleanSupplier cancelled;
    private final int frameBytes;
    private final long expectedFrames;
    private ByteBuffer tail = EMPTY;
    private boolean closed, prepared, eof;
    private long skippedFrames, emittedFrames, decodedFrames;

    public FrameOffsetAudioStream(AudioStream source, BooleanSupplier cancelled) throws IOException {
        this(source,cancelled,-1);
    }

    /** A pinned recording must decode to its complete expected frame count, not
     * merely emit an EOF that a client could mistake for a successful ending. */
    public FrameOffsetAudioStream(AudioStream source, BooleanSupplier cancelled, long expectedFrames) throws IOException {
        this.source = Objects.requireNonNull(source);
        this.cancelled = Objects.requireNonNull(cancelled);
        this.expectedFrames = expectedFrames;
        this.format = source.getFormat();
        int channels = format.getChannels();
        this.frameBytes = format.getFrameSize();
        float rate = format.getSampleRate();
        if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
            || format.getSampleSizeInBits() != 16 || channels < 1 || channels > 2
            || frameBytes != channels * 2 || !Float.isFinite(rate) || rate != (int) rate
            || rate < 8000 || rate > 48000 || format.getFrameRate() != rate
            || expectedFrames < -1 || expectedFrames == 0 || expectedFrames > (long) rate * 3600) {
            close();
            throw new IOException("Unsupported decoded show PCM format");
        }
    }

    /** Run on the dedicated bounded seek worker, never on the game or sound thread. */
    public synchronized FrameOffsetAudioStream prepare(long frames, long deadlineNanos) throws IOException {
        try {
            check();
            if (prepared || frames < 0 || frames > (long) format.getSampleRate() * 3600)
                throw new IOException("Invalid or repeated decoded offset preparation");
            long bytes = Math.multiplyExact(frames, frameBytes);
            while (bytes > 0) {
                checkDeadline(deadlineNanos);
                if (!tail.hasRemaining() && !refill()) throw new EOFException("Recording ended before requested offset");
                int discard = (int) Math.min(bytes, tail.remaining());
                tail.position(tail.position() + discard);
                bytes -= discard;
            }
            // Offset-at-EOF must not be accepted as a successful seek or completion.
            checkDeadline(deadlineNanos);
            if (!tail.hasRemaining() && !refill()) throw new EOFException("No decoded frame remains at requested offset");
            skippedFrames = frames;
            prepared = true;
            return this;
        } catch (IOException | RuntimeException error) {
            try { close(); } catch (IOException suppressed) { error.addSuppressed(suppressed); }
            throw error;
        }
    }

    private void checkDeadline(long deadline) throws IOException {
        check();
        if (System.nanoTime() - deadline >= 0) throw new IOException("Decoded offset preparation timed out");
    }
    private void check() throws IOException {
        if (closed || cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new IOException("Decoded show seek cancelled");
    }
    private boolean refill() throws IOException {
        check();
        if (eof) return false;
        ByteBuffer block = source.read(DECODE_REQUEST_BYTES);
        if (block == null) throw new IOException("Decoder returned null PCM");
        if (block.remaining() > MAX_DECODE_BLOCK_BYTES) throw new IOException("Decoder exceeded bounded PCM block");
        if (block.remaining() % frameBytes != 0) throw new IOException("Decoder returned a partial interleaved frame");
        // AudioStream treats an empty buffer as EOF, not a recoverable busy read.
        if (!block.hasRemaining()) {
            if(expectedFrames>=0&&decodedFrames!=expectedFrames)throw new IOException("Decoded show length differs from the pinned recording");
            eof = true; tail = EMPTY; return false;
        }
        decodedFrames += block.remaining() / frameBytes;
        if(expectedFrames>=0&&decodedFrames>expectedFrames)throw new IOException("Decoder exceeded the pinned recording length");
        tail = block;
        return true;
    }

    @Override public AudioFormat getFormat() { return format; }
    @Override public synchronized ByteBuffer read(int requestedBytes) throws IOException {
        check();
        if (!prepared) throw new IOException("Unprepared decoded show stream");
        if (requestedBytes <= 0) throw new IOException("Invalid PCM read size");
        int size = Math.max(frameBytes, Math.min(requestedBytes, MAX_OUTPUT_BYTES));
        size -= size % frameBytes;
        ByteBuffer output = ByteBuffer.allocateDirect(size);
        try {
            while (output.hasRemaining()) {
                if (!tail.hasRemaining() && !refill()) break;
                int count = Math.min(output.remaining(), tail.remaining());
                int limit = tail.limit();
                tail.limit(tail.position() + count);
                output.put(tail);
                tail.limit(limit);
            }
            output.flip();
            emittedFrames += output.remaining() / frameBytes;
            return output;
        } catch (IOException | RuntimeException error) {
            try { close(); } catch (IOException suppressed) { error.addSuppressed(suppressed); }
            throw error;
        }
    }
    public synchronized long skippedFrames() { return skippedFrames; }
    public synchronized long emittedFrames() { return emittedFrames; }
    public synchronized boolean decoderEof() { return eof; }
    @Override public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        tail = EMPTY;
        source.close();
    }
}
