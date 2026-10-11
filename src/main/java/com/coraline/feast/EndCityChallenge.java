package com.coraline.feast;

import java.util.*;
import net.minecraft.core.BlockPos;

/** Pure, replayable rules for the moving rune challenge. */
public final class EndCityChallenge {
    public static final int DURATION = 20 * 300;
    public static final int HEARTS = 3;
    private static final int[] CYCLES={40,32,25,19,14};
    public static List<BlockPos> route(long seed, int room, int stage) {
        int r = EndCityCourse.radius(room) - 2;
        var corners = new ArrayList<>(List.of(new BlockPos(-r,0,-r), new BlockPos(r,0,-r),
                new BlockPos(r,0,r), new BlockPos(-r,0,r)));
        Collections.shuffle(corners, new Random(seed * 0x9E3779B97F4A7C15L ^ (room * 8191L) ^ (stage * 127L)));
        return List.copyOf(corners.subList(0, room < 2 ? 3 : 4));
    }
    public static int cycle(int room) { return CYCLES[Math.max(0,Math.min(4,room))]; }
    public record Beam(net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to,double halfWidth){
        public boolean intersects(net.minecraft.world.phys.AABB box){
            if(box.maxY<=from.y||box.minY>=from.y+2)return false;
            var centre=from.add(to).scale(.5);var u=to.subtract(from).normalize();double length=from.distanceTo(to)*.5;
            double dx=(box.minX+box.maxX)*.5-centre.x,dz=(box.minZ+box.maxZ)*.5-centre.z;
            double hx=(box.maxX-box.minX)*.5,hz=(box.maxZ-box.minZ)*.5,vx=-u.z,vz=u.x;
            return Math.abs(dx*u.x+dz*u.z)<=length+hx*Math.abs(u.x)+hz*Math.abs(u.z)
                &&Math.abs(dx*vx+dz*vz)<=halfWidth+hx*Math.abs(vx)+hz*Math.abs(vz)
                &&Math.abs(dx)<=hx+length*Math.abs(u.x)+halfWidth*Math.abs(vx)
                &&Math.abs(dz)<=hz+length*Math.abs(u.z)+halfWidth*Math.abs(vz);
        }
    }
    private static long mix(long value){value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;value=(value^(value>>>27))*0x94D049BB133111EBL;return value^(value>>>31);}
    private static int shuffledTile(long cue,int count,long seed){
        int mask=Integer.highestOneBit(count-1)*2-1;int tile=(int)Math.floorMod(cue,count);
        do{tile=(tile+(int)seed)&mask;tile=(tile^(tile>>>3))&mask;tile=(tile*0x9E3779B1)&mask;tile=(tile^(tile>>>5))&mask;}while(tile>=count);
        return tile;
    }
    /** Each cue picks an arena-wide tile and fresh angle. The tile permutation
     * visits every tile without using a predictable row/column sweep. */
    public static Beam geometry(BlockPos b,long elapsed,int room){
        int r=EndCityCourse.radius(room),side=2*r+1,count=side*side;long cue=Math.max(0,elapsed)/cycle(room);
        long seed=mix(b.asLong()+room*8191L);int tile=shuffledTile(cue,count,seed);
        double px=tile%side-r,pz=tile/side-r;
        double angle=Math.floorMod(mix(seed+cue*0x9E3779B97F4A7C15L),100000)*Math.PI/100000;
        double ux=Math.cos(angle),uz=Math.sin(angle),low=-r-.5,high=r+.5,t0=-1e9,t1=1e9;
        if(Math.abs(ux)>1e-8){double a=(low-px)/ux,c=(high-px)/ux;t0=Math.max(t0,Math.min(a,c));t1=Math.min(t1,Math.max(a,c));}
        if(Math.abs(uz)>1e-8){double a=(low-pz)/uz,c=(high-pz)/uz;t0=Math.max(t0,Math.min(a,c));t1=Math.min(t1,Math.max(a,c));}
        return new Beam(new net.minecraft.world.phys.Vec3(b.getX()+.5+px+ux*t0,b.getY(),b.getZ()+.5+pz+uz*t0),
            new net.minecraft.world.phys.Vec3(b.getX()+.5+px+ux*t1,b.getY(),b.getZ()+.5+pz+uz*t1),.65);
    }
    public static int beam(long elapsed, int room) {
        int r = EndCityCourse.radius(room);
        return Math.floorMod((int)(elapsed / cycle(room)), 2*r+1) - r;
    }
    public static boolean firing(long elapsed, int room) {
        return Math.floorMod(elapsed, cycle(room)) >= cycle(room)-6;
    }
    public static net.minecraft.world.phys.AABB beamBox(BlockPos b,long elapsed,int room){
        var beam=geometry(b,elapsed,room);return new net.minecraft.world.phys.AABB(beam.from,beam.to).inflate(beam.halfWidth,0,beam.halfWidth).expandTowards(0,2,0);
    }
    private EndCityChallenge() {}
}
