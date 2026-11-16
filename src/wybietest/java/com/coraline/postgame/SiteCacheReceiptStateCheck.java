package com.coraline.postgame;

import java.util.*;
import java.nio.file.*;
import java.util.function.Consumer;
import net.minecraft.nbt.*;

/** Real production receipt validation and compressed NBT. No Minecraft
 * bootstrap, block entity, live-world authority or item payout is simulated. */
public final class SiteCacheReceiptStateCheck {
    private static int checks;
    private static final UUID EPOCH=UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static InfiniteStructurePlan.Plan plan(InfiniteStructurePlan.Family family){
        for(int x=-12288;x<=12288;x+=768)for(int z=-12288;z<=12288;z+=768){var value=InfiniteStructurePlan.at(EPOCH,124119,family,x,z);if(value.isPresent())return value.orElseThrow();}throw new AssertionError("No valid receipt fixture "+family);
    }
    private static void rejected(CompoundTag original,int x,int y,int z,Consumer<CompoundTag> corrupt,String message){
        var copy=original.copy();corrupt.accept(copy);var before=copy.copy();
        require(SiteCacheReceipt.read(copy,x,y,z).isEmpty(),message);require(copy.equals(before),"Rejected snapshot was reset/rewritten: "+message);
    }
    public static void main(String[] args)throws Exception{
        var directory=Files.createTempDirectory(Path.of("build-character-cast"),"site-cache-receipt-");var tiers=new HashSet<Integer>();int files=0;
        for(var family:InfiniteStructurePlan.Family.values()){
            var p=plan(family);
            for(int i=0;i<p.modules().size();i++){
                var room=p.modules().get(i);var made=SiteCacheReceipt.create(p,i);
                if(room.role()!=InfiniteStructurePlan.Role.STORAGE&&room.role()!=InfiniteStructurePlan.Role.CONTROL&&room.role()!=InfiniteStructurePlan.Role.ORE_VAULT){require(made.isEmpty(),"Ordinary room received a protected-cache receipt");continue;}
                require(made.isPresent(),"Actual guarded room has no receipt");var n=made.orElseThrow();
                int x=p.x()+room.x()+1,y=p.y()+room.y()+1,z=p.z()+room.z()+1;
                var decoded=SiteCacheReceipt.read(n,x,y,z).orElseThrow();
                require(decoded.site().equals(p.site())&&decoded.generation().equals(EPOCH)&&decoded.seed()==124119&&decoded.locked(),"Receipt lost site/generation binding");tiers.add(decoded.tier());
                var extension=new CompoundTag();extension.putUUID("UnrecognizedFutureId",UUID.randomUUID());n.put("Extension",extension);var before=n.copy();
                require(SiteCacheReceipt.read(n,x,y,z).isPresent()&&n.equals(before),"Read mutated preserved extension data");
                var disk=directory.resolve("receipt-"+files+++".dat");NbtIo.writeCompressed(n,disk.toFile());var reloaded=NbtIo.readCompressed(disk.toFile());
                require(reloaded.equals(n)&&SiteCacheReceipt.read(reloaded,x,y,z).orElseThrow().equals(decoded),"Actual compressed NBT roundtrip lost receipt state");
                n.putBoolean("Locked",false);require(!SiteCacheReceipt.read(n,x,y,z).orElseThrow().locked(),"Successful unlock snapshot became locked after decoding");n.putBoolean("Locked",true);
                rejected(n,x,y,z,t->t.putInt("Version",2),"Future version granted permission");
                rejected(n,x,y,z,t->t.putDouble("Version",1),"Coerced non-integer schema version");
                rejected(n,x,y,z,t->t.putUUID("Site",UUID.randomUUID()),"Foreign site accepted");
                rejected(n,x,y,z,t->t.putUUID("Generation",UUID.randomUUID()),"Changed epoch kept old site identity");
                rejected(n,x,y,z,t->t.putString("Module","1"),"String module index accepted");
                rejected(n,x,y,z,t->t.putInt("Module",99),"Out-of-bounds module accepted");
                rejected(n,x,y,z,t->t.putInt("Tier",0),"Zero-tier lock accepted");
                rejected(n,x,y,z,t->t.putInt("Tier",decoded.tier()==4?1:4),"Tier/cost downgrade or mismatch accepted");
                rejected(n,x,y,z,t->t.putByte("Tier",(byte)decoded.tier()),"Coerced wrong-tier numeric representation");
                rejected(n,x,y,z,t->t.putInt("Seed",124119),"Truncated seed type accepted");
                rejected(n,x,y,z,t->t.putByte("Locked",(byte)2),"Unknown Boolean state accepted");
                rejected(n,x,y,z,t->t.putString("Family","FUTURE_FAMILY"),"Unknown family silently reset");
                rejected(n,x,y,z,t->t.putIntArray("Site",new int[]{1,2,3}),"Malformed UUID accepted");
                rejected(n,x,y,z,t->t.putInt("AnchorX",Integer.MAX_VALUE),"Overflowed anchor accepted");
                require(SiteCacheReceipt.read(n,x+1,y,z).isEmpty()&&SiteCacheReceipt.read(n,x,y+1,z).isEmpty()&&SiteCacheReceipt.read(n,x,y,z+1).isEmpty(),"Copied/moved cache retained the old identity");
            }
        }
        require(tiers.equals(Set.of(1,2,3,4)),"Four real structure lock tiers are not all available: "+tiers);
        System.out.println("RECEIPT_FILES="+files+" TIERS="+tiers+" DIRECTORY="+directory);
        System.out.println("SITE_CACHE_RECEIPT_NBT_OK checks="+checks);
    }
    private SiteCacheReceiptStateCheck(){}
}
