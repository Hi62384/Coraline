package com.coraline.palace;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Read-only Anvil LOCATION headers. Zero is the only proof of no saved chunk;
 * malformed/truncated/unreadable files reject a candidate, never mean virgin.
 * Never opens a region file for writing or instantiates a world/chunk.
 */
public final class PalaceVirginTerrain {
    @FunctionalInterface public interface Loaded { boolean contains(int chunkX,int chunkZ); }
    public static boolean untouched(Path regions,int minX,int minZ,int maxX,int maxZ,Loaded loaded)throws IOException{
        if(minX>maxX||minZ>maxZ||(long)maxX-minX>128||(long)maxZ-minZ>128)throw new IllegalArgumentException("Unbounded virgin terrain survey");
        var headers=new HashMap<Long,int[]>();
        for(int z=minZ;z<=maxZ;z++)for(int x=minX;x<=maxX;x++){
            if(loaded.contains(x,z))return false;
            int rx=Math.floorDiv(x,32),rz=Math.floorDiv(z,32);long id=((long)rx<<32)^(rz&0xffffffffL);var offsets=headers.get(id);
            if(offsets==null){
                offsets=new int[1024];var path=regions.resolve("r."+rx+"."+rz+".mca");
                java.nio.file.attribute.BasicFileAttributes attributes=null;
                try{attributes=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);}catch(NoSuchFileException absent){/* Only an actual missing file is absence. */}
                if(attributes!=null){
                    if(!attributes.isRegularFile()||attributes.size()<8192)throw new IOException("Malformed region location table");
                    var data=ByteBuffer.allocate(4096).order(ByteOrder.BIG_ENDIAN);
                    try(var channel=FileChannel.open(path,StandardOpenOption.READ)){while(data.hasRemaining())if(channel.read(data)<=0)throw new IOException("Truncated/unreadable location table");}
                    data.flip();data.asIntBuffer().get(offsets);
                }
                headers.put(id,offsets);
            }
            if(offsets[Math.floorMod(x,32)+Math.floorMod(z,32)*32]!=0)return false;
        }
        return true;
    }
    private PalaceVirginTerrain(){}
}
