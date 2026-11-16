import com.coraline.client.audio.FrameOffsetAudioStream;
import com.mojang.blaze3d.audio.OggAudioStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Actual Minecraft/STB decoder, not an OpenAL or cinematic playback acceptance. */
public final class NativeOggContinuitySmoke {
    private static long consume(OggAudioStream stream, MessageDigest digest, long skipBytes) throws Exception {
        long total=0;
        for (;;) {
            ByteBuffer block=stream.read(16384);
            if (!block.hasRemaining()) return total;
            int size=block.remaining();total+=size;
            int skip=(int)Math.min(skipBytes,size);block.position(block.position()+skip);skipBytes-=skip;
            digest.update(block);
        }
    }
    public static void main(String[] args) throws Exception {
        int files=0,checks=0;
        try(var paths=Files.list(Path.of(args[0]))) {
            for(Path file:paths.filter(p->p.toString().endsWith(".ogg")).sorted().toList()) {
                long bytes;int frameSize;
                try(var full=new OggAudioStream(Files.newInputStream(file))) {
                    frameSize=full.getFormat().getFrameSize();
                    bytes=consume(full,MessageDigest.getInstance("SHA-256"),0);
                }
                if(bytes%frameSize!=0||bytes==0)throw new AssertionError("Invalid native PCM length: "+file);
                long frames=bytes/frameSize;
                for(long offset:new long[]{0,1,4095,4096,frames/2,frames-1}) {
                    if(offset>=frames)continue;
                    var expected=MessageDigest.getInstance("SHA-256");
                    try(var full=new OggAudioStream(Files.newInputStream(file))) { consume(full,expected,offset*frameSize); }
                    var actual=MessageDigest.getInstance("SHA-256");long count=0;
                    try(var seek=new FrameOffsetAudioStream(new OggAudioStream(Files.newInputStream(file)),()->false,frames)) {
                        seek.prepare(offset,System.nanoTime()+30_000_000_000L);
                        for(;;){var block=seek.read(16384);if(!block.hasRemaining())break;count+=block.remaining();actual.update(block);}
                        if(!seek.decoderEof()||count!=(frames-offset)*frameSize||!Arrays.equals(expected.digest(),actual.digest()))
                            throw new AssertionError("Native suffix mismatch: "+file+" at "+offset);
                    }
                    checks++;
                }
                for(int scenario=0;scenario<3;scenario++) {
                    var cancelled=new java.util.concurrent.atomic.AtomicBoolean(scenario==0);
                    try(var seek=new FrameOffsetAudioStream(new OggAudioStream(Files.newInputStream(file)),cancelled::get,frames)) {
                        boolean rejected=false;
                        try {
                            seek.prepare(scenario==2?frames:1,System.nanoTime()+30_000_000_000L);
                            if(scenario==1){seek.read(16384);cancelled.set(true);seek.read(16384);}
                        } catch(java.io.IOException expected){rejected=true;}
                        if(!rejected)throw new AssertionError("Native cancellation/EOF accepted: "+file+" scenario "+scenario);
                    }
                    checks++;
                }
                System.out.println(file.getFileName()+" nativeFrames="+frames+" frameBytes="+frameSize);
                files++;
            }
        }
        if(files!=13)throw new AssertionError("Expected all thirteen originals, got "+files);
        System.out.println("NATIVE_OGG_CONTINUITY_OK files="+files+" suffixChecks="+checks+"; NOT OpenAL, actor, camera or multiplayer acceptance.");
    }
}
