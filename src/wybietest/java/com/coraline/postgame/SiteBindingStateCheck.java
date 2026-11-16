package com.coraline.postgame;

import java.util.*;
import java.nio.file.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Strict production cell identity and compressed NBT; not live cutting,
 * protection, durability, collision, native placement or render acceptance. */
public final class SiteBindingStateCheck {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void reject(CompoundTag original,BlockPos at,Consumer<CompoundTag> corrupt){
        var n=original.copy();corrupt.accept(n);var before=n.copy();
        require(SiteBindingReceipt.read(n,at).isEmpty(),"Malformed/future binding authorized: "+n);require(n.equals(before),"Rejected binding snapshot overwritten");
    }
    public static void main(String[] args)throws Exception{
        var epoch=UUID.fromString("11111111-2222-3333-4444-555555555555");InfiniteStructurePlan.Plan plan=null;
        for(int x=-12288;x<=12288&&plan==null;x+=768)for(int z=-12288;z<=12288;z+=768){var p=InfiniteStructurePlan.at(epoch,124119,InfiniteStructurePlan.Family.DOLL_WORKSHOP,x,z);if(p.isPresent()){plan=p.orElseThrow();break;}}
        require(plan!=null,"No actual workshop fixture");var directory=Files.createTempDirectory(Path.of("build-character-cast"),"binding-state-");int files=0,rooms=0;
        for(int module=0;module<plan.modules().size();module++){
            var cells=SiteBindingReceipt.cells(plan,module);var room=plan.modules().get(module);
            if(room.role()!=InfiniteStructurePlan.Role.WORKROOM){require(cells.isEmpty(),"Binding blocked another room or an exit hall");continue;}
            rooms++;require(cells.size()==24&&new HashSet<>(cells).size()==24,"Doorway cells missing/overlapping");
            for(var at:cells){
                var n=SiteBindingReceipt.create(plan,module,at).orElseThrow();var original=SiteBindingReceipt.read(n,at).orElseThrow();
                require(!original.cut()&&original.site().equals(plan.site())&&original.generation().equals(epoch)&&original.seed()==124119,"Binding lost immutable site receipt");
                require(at.getY()==plan.y()+room.y()+1||at.getY()==plan.y()+room.y()+2,"Binding closed upper room/roof instead of doorway");
                var other=at.getY()==plan.y()+room.y()+1?at.above():at.below();require(cells.contains(other),"Player passage would need missing/non-cuttable second cell");
                n.putBoolean("Cut",true);n.putString("UnknownExtension","preserve");var before=n.copy();
                require(SiteBindingReceipt.read(n,at).orElseThrow().cut()&&n.equals(before),"Cut state lost or read mutated snapshot");
                var path=directory.resolve("binding-"+files+++".dat");NbtIo.writeCompressed(n,path.toFile());var loaded=NbtIo.readCompressed(path.toFile());
                require(loaded.equals(n)&&SiteBindingReceipt.read(loaded,at).orElseThrow().cut(),"Compressed save lost permanent cut state");
                reject(n,at,t->t.putInt("Version",999));reject(n,at,t->t.putString("Version","1"));
                reject(n,at,t->t.putUUID("Site",UUID.randomUUID()));reject(n,at,t->t.putUUID("Generation",UUID.randomUUID()));
                reject(n,at,t->t.putInt("Seed",124119));reject(n,at,t->t.putInt("Module",Integer.MAX_VALUE));
                reject(n,at,t->t.putInt("Cell",-1));reject(n,at,t->t.putString("Cell","0"));
                reject(n,at,t->t.putInt("Cut",1));reject(n,at,t->t.putByte("Cut",(byte)2));
                reject(n,at,t->t.putIntArray("Site",new int[]{1,2,3}));
                require(SiteBindingReceipt.read(n,at.offset(17,0,0)).isEmpty(),"Moved/copied binding retained use authority");
            }
        }
        require(rooms==1&&files==24,"Workshop did not have one real stitched workroom");
        require(SiteBindingReceipt.cells(plan,-1).isEmpty()&&SiteBindingReceipt.cells(plan,99).isEmpty(),"Invalid module generated cells");
        System.out.println("BINDING_FILES="+files+" DIRECTORY="+directory);System.out.println("SITE_BINDING_NBT_OK checks="+checks);
    }
    private SiteBindingStateCheck(){}
}
