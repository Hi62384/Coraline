package com.coraline.feast;

import java.util.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Sparse three-dimensional dropper hazards, not stacked solid floors. */
public final class FortressDropper {
    public record Obstacle(int x,int y,int z,int X,int Y,int Z,Block material){
        public AABB bounds(){return new AABB(x,y,z,X+1,Y+1,Z+1);}
    }
    public record Pool(int x,int z,int size){
        public boolean contains(double px,double pz){int r=size/2;return px>x-r+.31&&px<x+r+.69&&pz>z-r+.31&&pz<z+r+.69;}
    }
    public static Pool pool(long seed,int level){var guides=FortressActivity.generate(seed,level);var last=guides.get(guides.size()-1);return new Pool(last.x(),last.z(),13-level*2);}
    private record Key(long seed,int level){}
    private static final LinkedHashMap<Key,List<Obstacle>> CACHE=new LinkedHashMap<>(64,.75F,true);
    public static synchronized List<Obstacle> obstacles(long seed,int level){
        var key=new Key(seed,level);var cached=CACHE.get(key);if(cached!=null)return cached;
        var random=new Random(seed^0x51DF009AL^(level*104729L));var out=new ArrayList<Obstacle>();int index=0;
        var path=FortressActivity.generate(seed,level);
        var footprints=new HashSet<String>();
        for(var original:path){
            var before=index==0?new FortressActivity.Gate(0,0,-1,3):path.get(index-1);
            int gx=Math.round((original.x()+before.x())/2F),gz=Math.round((original.z()+before.z())/2F);
            Block block=switch(level){case 0->Blocks.NETHER_BRICKS;case 1->Blocks.POLISHED_BASALT;case 2->Blocks.GILDED_BLACKSTONE;case 3->Blocks.CRIMSON_STEM;default->Blocks.RED_NETHER_BRICKS;};
            // Independent small props, never mirrored wall-to-wall frames.
            // Keep a broad steering allowance, including body and arrival lag.
            for(int piece=0;piece<1+level/2;piece++)for(int attempt=0;attempt<100;attempt++){
                int x=random.nextInt(29)-14,z=random.nextInt(29)-14,y=original.y()+random.nextInt(5)-2;
                int shape=random.nextInt(4),length=2+random.nextInt(3);boolean axis=random.nextBoolean();
                var cluster=new ArrayList<Obstacle>();
                for(int part=0;part<3;part++){
                    if(shape==0){ // Three irregular sections of a broken bridge.
                        int dx=axis?part*(length+1):0,dz=axis?0:part*(length+1);
                        cluster.add(new Obstacle(x+dx,y-part%2,z+dz,x+dx+(axis?length-1:0),y-part%2,z+dz+(axis?0:length-1),block));
                    }else if(shape==1){ // Stepped diagonal fragments, with real empty gaps.
                        cluster.add(new Obstacle(x+part*2,y-part,z+part*2,x+part*2+1,y-part,z+part*2+1,part==1?Blocks.MAGMA_BLOCK:block));
                    }else if(shape==2){ // A compact layered floating rock island.
                        int r=part==0?2:1;cluster.add(new Obstacle(x-r,y+part,z-r,x+r,y+part,z+r,part==2?block:Blocks.BLACKSTONE));
                    }else{ // Short hanging pillar, rather than long aligned shafts.
                        cluster.add(new Obstacle(x,y-part*2,z,x,y-part*2+1,z,part==1?Blocks.NETHER_BRICK_FENCE:Blocks.BASALT));
                    }
                }
                boolean clear=true;for(var o:cluster)if(o.x()<-16||o.X()>16||o.z()<-16||o.Z()>16||
                    (o.X()>=gx-5&&o.x()<=gx+5&&o.Z()>=gz-5&&o.z()<=gz+5)){clear=false;break;}
                String footprint=x+":"+z;
                if(!clear||!footprints.add(footprint))continue;
                out.addAll(cluster);break;
            }
            index++;
        }
        // Offset bridge fragments block straight-down lava landings, but are
        // spread over depth instead of forming a dense, nearly solid curtain.
        var pool=pool(seed,level);int r=pool.size()/2;
        for(int row=-r;row<=r;row++){
            int cut=pool.x()+random.nextInt(2*(r/2)+1)-r/2;
            int left=-78-random.nextInt(23),right=-78-random.nextInt(23);
            if(Math.abs(left-right)<4)right=-78-Math.floorMod(-right-78+8,23);
            out.add(new Obstacle(pool.x()-r,left,pool.z()+row,cut,left,pool.z()+row,Blocks.POLISHED_BLACKSTONE_BRICKS));
            out.add(new Obstacle(cut+1,right,pool.z()+row,pool.x()+r,right,pool.z()+row,Blocks.GILDED_BLACKSTONE));
        }
        var result=List.copyOf(out);CACHE.put(key,result);while(CACHE.size()>64)CACHE.remove(CACHE.keySet().iterator().next());return result;
    }
    public static boolean hit(List<Obstacle> obstacles,Vec3 from,Vec3 to){
        for(var obstacle:obstacles){var b=obstacle.bounds();var swept=new AABB(b.minX-.3,b.minY-1.8,b.minZ-.3,b.maxX+.3,b.maxY,b.maxZ+.3);
            if(swept.contains(from)||swept.contains(to)||swept.clip(from,to).isPresent())return true;
        }return false;
    }
    private FortressDropper(){}
}
