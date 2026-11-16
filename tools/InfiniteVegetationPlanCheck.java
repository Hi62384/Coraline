package com.coraline.postgame;

import java.util.*;

/** Actual planner geometry and halo continuity; no registered block acceptance. */
public final class InfiniteVegetationPlanCheck {
    private static int assertions;
    private static void require(boolean valid,String message) {
        assertions++; if(!valid)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for(var kind:InfiniteVegetationPlan.Kind.values()) {
            var p=new InfiniteVegetationPlan.Plant(-1,80,-16,8,kind==InfiniteVegetationPlan.Kind.VELVET_MUSHROOM?4:kind==InfiniteVegetationPlan.Kind.PAPERWOOD?2:3,kind);
            int stems=0,crown=0;
            for(int x=p.x()-5;x<=p.x()+5;x++) for(int z=p.z()-5;z<=p.z()+5;z++) for(int y=p.y()-1;y<=p.y()+p.height()+2;y++) {
                var part=InfiniteVegetationPlan.partAt(p,x,y,z);
                if(part==InfiniteVegetationPlan.Part.NONE)continue;
                require(Math.abs(x-p.x())<=p.radius()&&Math.abs(z-p.z())<=p.radius()&&y>=p.y()&&y<=p.y()+p.height()+1,"Geometry exceeded halo/build bounds");
                if(part==InfiniteVegetationPlan.Part.STEM)stems++;else crown++;
            }
            require(stems==p.height(),"Missing continuous trunk for "+kind);
            require(crown>10,"Empty or trivial custom canopy for "+kind);
        }
        int total=0,crossing=0,changed=0;
        for(long seed:new long[]{124119,-742971,981377123}) {
            var plan=new InfiniteVegetationPlan(seed);var reload=new InfiniteVegetationPlan(seed);var other=new InfiniteVegetationPlan(seed+1);
            var visits=new ArrayList<int[]>();
            for(int x=-64;x<=64;x+=7)for(int z=-64;z<=64;z+=7)visits.add(new int[]{x,z});
            var expected=new HashMap<String,List<InfiniteVegetationPlan.Plant>>();
            for(var visit:visits)expected.put(visit[0]+":"+visit[1],plan.forChunk(visit[0],visit[1]));
            Collections.shuffle(visits,new Random(17));
            for(var visit:visits) {
                int x=visit[0],z=visit[1];var plants=plan.forChunk(x,z);
                require(plants.equals(expected.get(x+":"+z))&&plants.equals(reload.forChunk(x,z)),"Chunk visitation/reload changed roots");
                if(!plants.equals(other.forChunk(x,z)))changed++;
                for(var plant:plants) {
                    total++;
                    require(plant.y()>InfiniteTerrain.SEA_LEVEL&&plant.y()+plant.height()+1<InfiniteTerrain.MIN_Y+InfiniteTerrain.HEIGHT,"Plant intersects sea or height ceiling");
                    var terrain=new InfiniteTerrain(seed);var column=terrain.column(plant.x(),plant.z());
                    require(plant.y()==column.surfaceY()+1&&!terrain.cave(plant.x(),column.surfaceY(),plant.z(),column),"Root floats above a cave/ground");
                    for(int dx:new int[]{-1,0,1})for(int dz:new int[]{-1,0,1}) {
                        int nx=x+dx,nz=z+dz;
                        boolean intersects=plant.x()+plant.radius()>=nx*16&&plant.x()-plant.radius()<=nx*16+15&&plant.z()+plant.radius()>=nz*16&&plant.z()-plant.radius()<=nz*16+15;
                        if(!intersects)continue;
                        require(plan.forChunk(nx,nz).contains(plant),"Adjacent chunk lost the same cross-border canopy");
                        if(dx!=0||dz!=0)crossing++;
                    }
                }
            }
        }
        require(total>100,"Natural custom vegetation is absent");
        require(crossing>0,"No continuous cross-border canopy was exercised");
        require(changed>0,"Independent seed did not change vegetation");
        System.out.println("Plants sampled="+total+" cross-border references="+crossing+" differing chunks="+changed);
        System.out.println("INFINITE_VEGETATION_PLAN_OK checks="+assertions);
    }
}
