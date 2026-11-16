package com.coraline.postgame;

import java.util.*;

/** Runs the production planner, not a reimplementation of terrain or identity. */
public final class InfiniteStructurePlanCheck {
    private static int checks;
    private record Door(int x,int y,int z){}
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static int floor(InfiniteStructurePlan.Module m,int z){return m.y()+(m.role()==InfiniteStructurePlan.Role.STAIRS?18-Math.min(18,z-m.z()):0);}
    private static List<Door> doors(InfiniteStructurePlan.Module m){
        var result=new ArrayList<Door>();
        for(int x=m.x();x<=m.maxX();x++)for(int z=m.z();z<=m.maxZ();z++){
            boolean door=(x==m.x()||x==m.maxX())&&Math.abs(z-(m.z()+m.depth()/2))<=1||(z==m.z()||z==m.maxZ())&&Math.abs(x-(m.x()+m.width()/2))<=1;
            if(door)result.add(new Door(x,floor(m,z),z));
        }return result;
    }
    private static boolean adjacent(InfiniteStructurePlan.Module a,InfiniteStructurePlan.Module b){
        for(var x:doors(a))for(var y:doors(b))if(Math.abs(x.x-y.x)+Math.abs(x.z-y.z)==1&&Math.abs(x.y-y.y)<=1)return true;return false;
    }
    private static void layout(InfiniteStructurePlan.Plan p){
        var modules=p.modules();require(modules.size()<=InfiniteStructurePlan.MAX_PIECES,"Unbounded piece count");
        var seen=new HashSet<Integer>();seen.add(0);boolean changed;
        do{changed=false;for(int i=0;i<modules.size();i++)if(!seen.contains(i)&&modules.get(i).role()!=InfiniteStructurePlan.Role.CHIMNEY)for(int j:new ArrayList<>(seen))if(adjacent(modules.get(i),modules.get(j))){seen.add(i);changed=true;break;}}while(changed);
        for(int i=0;i<modules.size();i++){
            var m=modules.get(i);require(m.role()==InfiniteStructurePlan.Role.CHIMNEY||seen.contains(i),"Disconnected module/entrance: "+p.family()+" / "+m.role());
            require(p.y()+m.y()-8>=InfiniteTerrain.MIN_Y&&p.y()+m.y()+m.height()<InfiniteTerrain.MIN_Y+InfiniteTerrain.HEIGHT,"Module outside build height");
            require(m.width()*m.height()*m.depth()<14000,"One unbounded synchronous piece");
        }
    }
    public static void main(String[] args){
        var epoch=UUID.fromString("11111111-2222-3333-4444-555555555555");
        var counts=new EnumMap<InfiniteStructurePlan.Family,Integer>(InfiniteStructurePlan.Family.class);
        var examples=new EnumMap<InfiniteStructurePlan.Family,InfiniteStructurePlan.Plan>(InfiniteStructurePlan.Family.class);
        int candidates=0;
        for(long seed:new long[]{124119,-742971,0,981377123})for(int x=-12288;x<=12288;x+=1536)for(int z=-12288;z<=12288;z+=1536)for(var family:InfiniteStructurePlan.Family.values()){
            candidates++;var planned=InfiniteStructurePlan.at(epoch,seed,family,x,z);if(planned.isEmpty())continue;
            var p=planned.orElseThrow();counts.merge(family,1,Integer::sum);examples.putIfAbsent(family,p);layout(p);
            require(p.family().biomes.contains(p.biome()),"Wrong climate family");require(p.y()>InfiniteTerrain.SEA_LEVEL+2,"Flooded surface entrance");
        }
        for(var family:InfiniteStructurePlan.Family.values())require(counts.getOrDefault(family,0)>0,"No admissible generated sites across independent seed sample: "+family+" / "+counts);
        for(var p:examples.values()){
            require(InfiniteStructurePlan.at(epoch,p.seed(),p.family(),p.x(),p.z()).orElseThrow().equals(p),"Order/reload changed immutable plan");
            var other=InfiniteStructurePlan.at(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),p.seed(),p.family(),p.x(),p.z()).orElseThrow();
            require(!other.site().equals(p.site())&&other.modules().equals(p.modules()),"Reset epoch reused site identity or changed terrain coordinates");
            boolean immutable=false;try{p.modules().clear();}catch(UnsupportedOperationException expected){immutable=true;}require(immutable,"Mutable shared plan list");
        }
        require(InfiniteStructurePlan.at(epoch,0,InfiniteStructurePlan.Family.DOLL_WORKSHOP,Integer.MAX_VALUE,0).isEmpty(),"Overflowing location accepted");
        System.out.println("STRUCTURE_CANDIDATES="+candidates+" ADMISSIBLE="+counts);
        System.out.println("INFINITE_STRUCTURE_PLAN_OK checks="+checks);
    }
}
