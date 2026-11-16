package com.coraline.postgame;

/** Seeded, coordinate-only terrain field for the forthcoming chunk adapter.
 * No mutable random cursor, chunk-order dependency or access to live worlds.
 * This sampler alone is not a registered/playable Minecraft dimension. */
public final class InfiniteTerrain {
    public static final int VERSION=1,MIN_Y=-64,HEIGHT=384,SEA_LEVEL=64;
    public enum Biome {
        STITCHED_MEADOWS(.1,.15,.25,.0,.0,8,5),
        BUTTONWOOD_FOREST(-.1,.5,.3,.0,.1,12,8),
        PORCELAIN_PLAINS(-.2,-.45,.35,-.1,.15,8,5),
        CANDY_WONDERLAND(.7,.0,.4,.15,-.1,14,12),
        VELVET_MUSHROOM_FOREST(-.5,.8,.2,.0,.4,16,11),
        CLOCKWORK_HIGHLANDS(.1,-.4,.6,.65,.1,32,26),
        MIRROR_MARSH(.1,.85,.0,-.6,.1,3,4),
        THREADBOUND_MOUNTAINS(-.5,-.15,.7,.85,.3,74,52),
        PAPERWOOD_FOREST(-.1,.1,.4,.1,-.6,13,10),
        FORGOTTEN_GARDENS(.4,.45,.3,-.1,-.4,9,7),
        PALE_WASTELAND(-.1,-.6,.5,.1,.6,15,12),
        OTHERWORLD_BADLANDS(.8,-.75,.6,.45,.2,29,22),
        LANTERNWOOD(-.65,.45,.4,.3,-.2,21,16),
        STITCHED_COAST(.15,.1,-.7,-.2,.0,3,3),
        ROOTBOUND_WILDS(.65,.8,.6,.3,-.1,27,21),
        WHITE_RIFT_WASTES(-.2,-.4,.7,.5,.95,42,29);
        final double heat,wet,land,lift,magic,hills,ridges;
        Biome(double heat,double wet,double land,double lift,double magic,double hills,double ridges){this.heat=heat;this.wet=wet;this.land=land;this.lift=lift;this.magic=magic;this.hills=hills;this.ridges=ridges;}
    }
    public record Column(int surfaceY,Biome biome,double temperature,double moisture,double continentalness,double riverDistance){}
    private static final Biome[] PROFILES=Biome.values();
    private final long seed;
    public InfiniteTerrain(long seed){this.seed=seed;}
    private static long mix(long v){v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;return v^(v>>>31);}
    private static double lattice(long seed,int x,int y,int z){return (mix(seed^(long)x*0x9e3779b97f4a7c15L^(long)y*0x632be59bd9b4e019L^(long)z*0x94d049bb133111ebL)>>>11)*0x1.0p-52-1;}
    private static double fade(double v){return v*v*v*(v*(v*6-15)+10);}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static double noise(long seed,double x,double y,double z){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y),iz=(int)Math.floor(z);double fx=fade(x-ix),fy=fade(y-iy),fz=fade(z-iz);
        double low=lerp(lerp(lattice(seed,ix,iy,iz),lattice(seed,ix+1,iy,iz),fx),lerp(lattice(seed,ix,iy,iz+1),lattice(seed,ix+1,iy,iz+1),fx),fz);
        double high=lerp(lerp(lattice(seed,ix,iy+1,iz),lattice(seed,ix+1,iy+1,iz),fx),lerp(lattice(seed,ix,iy+1,iz+1),lattice(seed,ix+1,iy+1,iz+1),fx),fz);
        return lerp(low,high,fy);
    }
    private double field(long salt,int x,int z,double scale,int octaves){
        double sum=0,total=0,amplitude=1,frequency=1/scale;
        for(int octave=0;octave<octaves;octave++){sum+=noise(seed+salt+octave*0x632be59bd9b4e019L,x*frequency,0,z*frequency)*amplitude;total+=amplitude;amplitude*=.5;frequency*=2;}
        return sum/total;
    }
    private static double sq(double x){return x*x;}
    public Column column(int x,int z){
        double heat=field(17,x,z,1450,3),wet=field(31,x,z,1250,3),land=field(47,x,z,2200,3),lift=field(59,x,z,1050,3),magic=field(79,x,z,1750,3);
        double total=0,hills=0,ridges=0,best=Double.MAX_VALUE;Biome selected=Biome.STITCHED_MEADOWS;
        for(var biome:PROFILES){
            double distance=sq(heat-biome.heat)+sq(wet-biome.wet)+sq((land-biome.land)*1.25)+sq(lift-biome.lift)+sq(magic-biome.magic);
            // Regions have discrete identities, but geometry blends across
            // those boundaries instead of changing height at a biome edge.
            double weight=Math.exp(-distance*4);total+=weight;hills+=weight*biome.hills;ridges+=weight*biome.ridges;
            if(distance<best){best=distance;selected=biome;}
        }
        hills/=total;ridges/=total;
        double regional=field(97,x,z,430,4),ridge=1-Math.abs(field(113,x,z,280,3));ridge=ridge*ridge;
        double elevation=67+land*74+sq(Math.max(0,lift))*120+regional*hills+ridge*ridges*Math.max(0,lift+.2)*2;
        double river=Math.abs(field(139,x,z,820,3));double channel=Math.max(0,1-river/.055);
        // Continuous channels lower toward a shared water plane. A regional
        // ocean field remains below it, producing stable coasts and beaches.
        if(land<-.12)elevation=lerp(elevation,47+land*35,Math.min(1,(-land-.12)*3));
        elevation=lerp(elevation,Math.min(elevation,SEA_LEVEL-3),channel*channel);
        elevation+=field(151,x,z,65,2)*2.5;
        return new Column((int)Math.floor(Math.max(MIN_Y+12,Math.min(MIN_Y+HEIGHT-28,elevation))),selected,heat,wet,land,river);
    }
    public enum Cave { PORCELAIN, SOUL_GLASS, THREADROOT, CLOCKWORK, MIRROR, WHITE_RIFT }
    /** Underground identity has its own depth and coarse 3D regions. Surface
     * climate still determines which environments may occur beneath it. */
    public Cave caveEnvironment(int x,int y,int z,Column column){
        if(y<-24&&(column.biome()==Biome.PORCELAIN_PLAINS||column.biome()==Biome.MIRROR_MARSH||column.biome()==Biome.WHITE_RIFT_WASTES)&&Math.floorMod(mix(seed+853^(long)Math.floorDiv(x,96)*0x9e3779b97f4a7c15L^(long)Math.floorDiv(y,48)*0x632be59bd9b4e019L^(long)Math.floorDiv(z,96)),5)==0)return Cave.SOUL_GLASS;
        return switch(column.biome()){
            case ROOTBOUND_WILDS,BUTTONWOOD_FOREST,LANTERNWOOD,VELVET_MUSHROOM_FOREST->y>-32?Cave.THREADROOT:Cave.PORCELAIN;
            case CLOCKWORK_HIGHLANDS,OTHERWORLD_BADLANDS->Cave.CLOCKWORK;
            case MIRROR_MARSH,STITCHED_COAST->Cave.MIRROR;
            case WHITE_RIFT_WASTES,PALE_WASTELAND->y<32?Cave.WHITE_RIFT:Cave.PORCELAIN;
            default->Cave.PORCELAIN;
        };
    }
    long caveDetail(int x,int y,int z){return mix(seed+877^(long)x*0x9e3779b97f4a7c15L^(long)y*0x632be59bd9b4e019L^(long)z*0x94d049bb133111ebL);}
    /** Pass the already sampled column from the chunk adapter to avoid
     * recalculating climate for each underground voxel. */
    public boolean cave(int x,int y,int z,Column column){
        if(y<=MIN_Y+5||y>column.surfaceY())return false;
        double tubeA=Math.abs(noise(seed+211,x/34.,y/22.,z/34.));
        double tubeB=Math.abs(noise(seed+227,x/29.,y/27.,z/29.));
        boolean tunnels=tubeA<.11&&tubeB<.14;
        double chamber=noise(seed+241,x/57.,y/36.,z/57.);
        boolean entrance=noise(seed+257,x/91.,0,z/91.)>.48;
        if(y>=column.surfaceY()-6&&!entrance)return false;
        if(tunnels||chamber>.59)return true;
        // Additional regional chambers retain the original connected tunnels
        // and surface mouths. No live chunk or saved structure is rewritten.
        if(y>=column.surfaceY()-12)return false;
        return switch(caveEnvironment(x,y,z,column)){
            case PORCELAIN,SOUL_GLASS->y<40&&chamber>.49;
            case WHITE_RIFT->y<48&&Math.abs(noise(seed+269,x/76.,0,z/76.))<.045&&noise(seed+271,x/90.,y/60.,z/90.)>.12;
            case MIRROR->y>4&&y<36&&Math.abs(noise(seed+277,x/54.,0,z/54.))<.09&&Math.abs(y-(16+noise(seed+281,x/90.,0,z/90.)*8))<5;
            case CLOCKWORK->y<32&&chamber>.54;
            case THREADROOT->false;
        };
    }
}
