package com.coraline.client.audio;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.BooleanSupplier;

/** Bounded hash check of the actual resource selected by the client resource manager. */
public final class RecordingFingerprint {
    public static void verify(InputStream input, long expectedBytes, String expectedSha256,
                              BooleanSupplier cancelled, long deadlineNanos) throws IOException {
        if (expectedBytes <= 0 || expectedBytes > 64L * 1024 * 1024)
            throw new IOException("Unsupported show recording length");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[16 * 1024];
            long total = 0;
            for (int read; (read = input.read(block)) != -1;) {
                if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()
                    || System.nanoTime() - deadlineNanos >= 0)
                    throw new IOException("Recording identity check cancelled or timed out");
                if (read == 0) throw new IOException("Recording stream made no progress");
                total += read;
                if (total > expectedBytes) throw new IOException("Recording size differs from verified original export");
                digest.update(block, 0, read);
            }
            if (cancelled.getAsBoolean() || System.nanoTime() - deadlineNanos >= 0) throw new IOException("Recording identity check cancelled or timed out");
            if (total != expectedBytes || !HexFormat.of().formatHex(digest.digest()).equals(expectedSha256))
                throw new IOException("Recording differs from verified original export");
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private RecordingFingerprint() {}
}
