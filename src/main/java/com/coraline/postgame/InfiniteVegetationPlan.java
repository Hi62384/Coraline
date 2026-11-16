package com.coraline.postgame;

import java.util.*;

/** Seeded tree geometry. Every voxel can be decided without loading neighbors. */
public final class InfiniteVegetationPlan {
    public enum Kind { BUTTONWOOD, PAPERWOOD, LANTERNWOOD, VELVET_MUSHROOM }
    public enum Part { NONE, STEM, CROWN }
    public record Plant(int x,int y,int z,int height,int radius,Kind kind) {}
    private static final int CELL=12, MAX_RADIUS=4;
    private final long seed;
    private final InfiniteTerrain terrain;
    public InfiniteVegetationPlan(long seed) { this.seed=seed; terrain=new InfiniteTerrain(seed); }
    private static long mix(long v) { v=(v^(v>>>30))*0xbf58476d1ce4e5b9L; v=(v^(v>>>27))*0x94d049bb133111ebL; return v^(v>>>31); }
    private long hash(int x,int z) { return mix(seed+911^(long)x*0x9e3779b97f4a7c15L^(long)z*0x632be59bd9b4e019L); }
    private static double unit(long value) { return (value>>>11)*0x1.0p-53; }
    Plant candidate(int cellX,int cellZ) {
        long h=hash(cellX,cellZ);
        int x=cellX*CELL+(int)(unit(mix(h+17))*CELL), z=cellZ*CELL+(int)(unit(mix(h+31))*CELL);
        var column=terrain.column(x,z);
        Kind kind; double probability;
        switch(column.biome()) {
            case BUTTONWOOD_FOREST -> { kind=Kind.BUTTONWOOD; probability=.76; }
            case PAPERWOOD_FOREST -> { kind=Kind.PAPERWOOD; probability=.72; }
            case LANTERNWOOD -> { kind=Kind.LANTERNWOOD; probability=.78; }
            case VELVET_MUSHROOM_FOREST -> { kind=Kind.VELVET_MUSHROOM; probability=.72; }
            case ROOTBOUND_WILDS -> { kind=Kind.BUTTONWOOD; probability=.58; }
            case FORGOTTEN_GARDENS -> { kind=Kind.PAPERWOOD; probability=.24; }
            case STITCHED_MEADOWS -> { kind=Kind.BUTTONWOOD; probability=.065; }
            default -> { return null; }
        }
        if(unit(h)>=probability || column.surfaceY()<=InfiniteTerrain.SEA_LEVEL || terrain.cave(x,column.surfaceY(),z,column)) return null;
        int height=(kind==Kind.PAPERWOOD?7:5)+(int)(unit(mix(h+47))*4);
        int radius=kind==Kind.VELVET_MUSHROOM?4:kind==Kind.PAPERWOOD?2:3;
        if(column.surfaceY()+height+2>=InfiniteTerrain.MIN_Y+InfiniteTerrain.HEIGHT) return null;
        return new Plant(x,column.surfaceY()+1,z,height,radius,kind);
    }
    public List<Plant> forChunk(int chunkX,int chunkZ) {
        int minX=chunkX*16, minZ=chunkZ*16;
        var plants=new ArrayList<Plant>();
        // Candidate roots in the halo allow a canopy to cross a chunk edge.
        // Stable cell ordering also fixes overlap priority independently of
        // the order in which the two chunks generate.
        for(int cx=Math.floorDiv(minX-MAX_RADIUS,CELL);cx<=Math.floorDiv(minX+15+MAX_RADIUS,CELL);cx++)
            for(int cz=Math.floorDiv(minZ-MAX_RADIUS,CELL);cz<=Math.floorDiv(minZ+15+MAX_RADIUS,CELL);cz++) {
                var p=candidate(cx,cz);
                if(p!=null && p.x()+p.radius()>=minX && p.x()-p.radius()<=minX+15 && p.z()+p.radius()>=minZ && p.z()-p.radius()<=minZ+15) plants.add(p);
            }
        return List.copyOf(plants);
    }
    public static Part partAt(Plant p,int x,int y,int z) {
        int dx=x-p.x(), dz=z-p.z(), up=y-p.y();
        if(up<0 || up>p.height()+1 || Math.abs(dx)>p.radius() || Math.abs(dz)>p.radius()) return Part.NONE;
        if(dx==0 && dz==0 && up<p.height()) return Part.STEM;
        int crown=up-p.height();
        boolean leaf=switch(p.kind()) {
            case VELVET_MUSHROOM -> (crown==0 && dx*dx+dz*dz<=p.radius()*p.radius()) || (crown==-1 && Math.max(Math.abs(dx),Math.abs(dz))==p.radius() && dx*dx+dz*dz<=p.radius()*p.radius());
            case PAPERWOOD -> crown>=-3 && crown<=1 && dx*dx+dz*dz<=(crown==1?1:4);
            case BUTTONWOOD, LANTERNWOOD -> crown>=-2 && crown<=1 && dx*dx+dz*dz<=(crown==1?4:10);
        };
        return leaf?Part.CROWN:Part.NONE;
    }
}
