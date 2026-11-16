import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.nbt.*;

/** Read-only NBT/Anvil inspection of an explicitly named stopped disposable QA save. */
public final class InspectTycoonCheckpoint {
    static CompoundTag chunk(Path save,int x,int z)throws Exception{
        Path file=save.resolve("dimensions/coraline/personal_other_world/region/r."+Math.floorDiv(x,512)+"."+Math.floorDiv(z,512)+".mca");
        if(!Files.isRegularFile(file))throw new IOException("Missing existing QA region: "+file);
        try(var in=new RandomAccessFile(file.toFile(),"r")){
            int slot=Math.floorMod(x>>4,32)+Math.floorMod(z>>4,32)*32;in.seek(slot*4L);int location=in.readInt();
            if((location>>>8)==0)throw new IOException("Unsaved QA chunk");in.seek((location>>>8)*4096L);int size=in.readInt(),format=in.readUnsignedByte();
            if(size<1||size>4*1024*1024)throw new IOException("Unbounded chunk payload");byte[] payload=new byte[size-1];in.readFully(payload);
            InputStream raw=new ByteArrayInputStream(payload);InputStream decoded=switch(format){case 1->new GZIPInputStream(raw);case 2->new InflaterInputStream(raw);case 3->raw;default->throw new IOException("Unsupported compression");};
            try(var data=new DataInputStream(decoded)){return NbtIo.read(data);}
        }
    }
    static CompoundTag state(CompoundTag chunk,int x,int y,int z){
        for(var entry:chunk.getList("sections",10)){var section=(CompoundTag)entry;if(section.getByte("Y")!=(y>>4))continue;
            var states=section.getCompound("block_states");var palette=states.getList("palette",10);if(palette.isEmpty())return new CompoundTag();if(palette.size()==1)return palette.getCompound(0);
            int bits=Math.max(4,32-Integer.numberOfLeadingZeros(palette.size()-1)),per=64/bits,index=((y&15)*16+(z&15))*16+(x&15);long[] values=states.getLongArray("data");
            return palette.getCompound((int)((values[index/per]>>>((index%per)*bits))&((1L<<bits)-1)));
        }return new CompoundTag();
    }
    static CompoundTag generated(CompoundTag encoded,int x,int y,int z){
        long packed=encoded.getLong("Origin");int ox=(int)(packed>>38),oy=(int)(packed<<52>>52),oz=(int)(packed<<26>>38);
        for(int cell:encoded.getIntArray("Cells"))if(ox+(cell&63)-32==x&&oy+((cell>>>12)&63)-16==y&&oz+((cell>>>6)&63)-32==z)return encoded.getList("Palette",10).getCompound(cell>>>18);
        return new CompoundTag();
    }
    public static void main(String[] args)throws Exception{
        Path save=Path.of(args[0]).toRealPath();if(!save.toString().contains("run-tests-isolated-")||!Files.isRegularFile(save.resolve("level.dat")))throw new IOException("Only an existing disposable QA save is accepted");
        int x=Integer.parseInt(args[1]),y=Integer.parseInt(args[2]),z=Integer.parseInt(args[3]);
        var saved=NbtIo.readCompressed(save.resolve("data/coraline_tycoon.dat").toFile()).getCompound("data");var home=saved.getList("Homes",10).getCompound(0);var s=home.getCompound("State");
        System.out.println("Home tier="+s.getInt("HouseTier")+" terrain="+s.getCompound("Terrain").getAllKeys());
        System.out.println("Actual "+x+","+y+","+z+" = "+state(chunk(save,x,z),x,y,z));
        System.out.println("Recorded house = "+generated(s.getCompound("HouseGenerated"),x,y,z));
        System.out.println("Recorded terrain = "+generated(s.getCompound("Terrain").getCompound(Math.floorDiv(x,16)+","+Math.floorDiv(z,16)),x,y,z));
        long block=((long)x&0x3ffffffL)<<38|((long)z&0x3ffffffL)<<12|((long)y&4095L);System.out.println("Protected edit="+s.getCompound("PlayerEdits").contains(Long.toString(block)));
    }
}
