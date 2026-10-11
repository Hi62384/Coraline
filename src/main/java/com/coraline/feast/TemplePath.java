package com.coraline.feast;

import java.util.*;

/** Orthogonal trails with a blank row between sweeps and no self-touching branches. */
public final class TemplePath {
    public record Tile(int x,int z){}
    public static int size(int room){return 5+room;}
    public static int target(int room){return new int[]{9,13,18,23,29}[room];}
    public static List<Tile> generate(int room,long seed){
        int n=size(room),min=target(room);var rng=new Random(seed);var route=new ArrayList<Tile>();
        // Alternating rows are connectors only: parallel sweeps cannot merge into gold blobs.
        for(int attempt=0;attempt<10000;attempt++){
            route.clear();int x=rng.nextInt(n);route.add(new Tile(x,0));
            for(int z=0;z<n;z++){
                int dest=z>0&&z<n-1&&z%2==1?rng.nextInt(n):x;
                while(x!=dest){x+=Integer.signum(dest-x);route.add(new Tile(x,z));}
                if(z<n-1)route.add(new Tile(x,z+1));
            }
            if(route.size()>=min&&route.size()<=min+2&&valid(room,route))return List.copyOf(route);
        }
        throw new IllegalStateException("Unable to generate temple route: "+room);
    }
    public static boolean valid(int room,List<Tile> route){
        int n=size(room);if(route.isEmpty()||route.get(0).z!=0||route.get(route.size()-1).z!=n-1)return false;
        if(route.stream().filter(t->t.z==0).count()!=1||route.stream().filter(t->t.z==n-1).count()!=1)return false;
        var seen=new HashSet<Tile>();Tile previous=null;
        for(var t:route){if(t.x<0||t.x>=n||t.z<0||t.z>=n||!seen.add(t))return false;
            if(previous!=null&&Math.abs(t.x-previous.x)+Math.abs(t.z-previous.z)!=1)return false;
            // Each new tile may touch its predecessor, but no earlier stretch of the trail.
            for(var offset:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                var neighbor=new Tile(t.x+offset[0],t.z+offset[1]);
                if(seen.contains(neighbor)&&!neighbor.equals(previous))return false;
            }
            previous=t;}
        return true;
    }
    /** Staying on the accepted tile is harmless; revisiting or skipping another tile is not. */
    public static int step(List<Tile> route,int next,Tile tile){
        if(next>0&&tile.equals(route.get(next-1)))return next;
        return next<route.size()&&tile.equals(route.get(next))?next+1:-1;
    }
    private TemplePath(){}
}
