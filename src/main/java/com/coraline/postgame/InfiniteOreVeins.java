package com.coraline.postgame;

/** Natural supernatural veins in absolute coordinates, including ordinary
 * resource equivalents with independent heights, sizes and regional weights.
 * Sampling never depends on Breach fuel, traversal order or a player RNG. */
public final class InfiniteOreVeins {
    public enum Ore {
        NONE(1,1,0,0,0,0,0),
        OTHER_IRON(14,12,-48,112,1.8,.30,311),
        OTHER_DIAMOND(30,16,-58,4,1.9,.20,337),
        MOONSTONE(32,18,-58,32,1.75,.14,383),
        PERFECT(40,20,-56,12,1.6,.16,401),
        SOUL_GLASS(40,20,-56,12,1.6,.16,359),
        OTHER_COAL(12,12,-16,160,2.5,.46,419),
        OTHER_COPPER(16,14,-16,112,2.5,.38,431),
        OTHER_GOLD(24,16,-56,40,1.9,.22,443),
        OTHER_REDSTONE(20,14,-60,16,2.1,.28,457),
        OTHER_LAPIS(26,18,-48,32,1.8,.24,463),
        OTHER_EMERALD(38,22,-32,96,1.25,.16,479),
        OTHER_QUARTZ(18,16,-48,80,2.2,.32,491);
        final int horizontal,vertical,minY,maxY,salt;final double radius,chance;
        Ore(int horizontal,int vertical,int minY,int maxY,double radius,double chance,int salt){this.horizontal=horizontal;this.vertical=vertical;this.minY=minY;this.maxY=maxY;this.radius=radius;this.chance=chance;this.salt=salt;}
    }
    record Point(int x,int y,int z){}
    private static final Ore[] VEINS={Ore.SOUL_GLASS,Ore.PERFECT,Ore.MOONSTONE,Ore.OTHER_DIAMOND,Ore.OTHER_IRON,Ore.OTHER_EMERALD,Ore.OTHER_GOLD,Ore.OTHER_LAPIS,Ore.OTHER_REDSTONE,Ore.OTHER_QUARTZ,Ore.OTHER_COPPER,Ore.OTHER_COAL};
    private final long seed;
    public InfiniteOreVeins(long seed){this.seed=seed;}
    private static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    private static double unit(long value){return (value>>>11)*0x1.0p-53;}
    private long hash(int x,int y,int z,int salt){return mix(seed+salt^(long)x*0x9e3779b97f4a7c15L^(long)y*0x632be59bd9b4e019L^(long)z*0x94d049bb133111ebL);}
    private static double chance(Ore ore,InfiniteTerrain.Biome biome){
        boolean rareRegion=biome==InfiniteTerrain.Biome.MIRROR_MARSH||biome==InfiniteTerrain.Biome.PORCELAIN_PLAINS||biome==InfiniteTerrain.Biome.WHITE_RIFT_WASTES;
        if((ore==Ore.SOUL_GLASS||ore==Ore.PERFECT)&&rareRegion)return ore.chance*1.25;
        boolean regional=switch(ore){
            case OTHER_COPPER->biome==InfiniteTerrain.Biome.MIRROR_MARSH;
            case OTHER_QUARTZ->biome==InfiniteTerrain.Biome.PORCELAIN_PLAINS;
            case OTHER_GOLD,OTHER_REDSTONE->biome==InfiniteTerrain.Biome.OTHERWORLD_BADLANDS;
            case OTHER_EMERALD->biome==InfiniteTerrain.Biome.WHITE_RIFT_WASTES;
            default->false;
        };
        return regional?ore.chance*1.45:ore.chance;
    }
    Point candidate(Ore ore,int cellX,int cellY,int cellZ,InfiniteTerrain.Biome biome){
        if(ore==Ore.NONE||biome==null)return null;long hash=hash(cellX,cellY,cellZ,ore.salt);if(unit(hash)>=chance(ore,biome))return null;
        int pad=(int)Math.ceil(ore.radius)+1;
        int x=cellX*ore.horizontal+pad+(int)(unit(mix(hash+17))*(ore.horizontal-pad*2));
        int y=cellY*ore.vertical+pad+(int)(unit(mix(hash+31))*(ore.vertical-pad*2));
        int z=cellZ*ore.horizontal+pad+(int)(unit(mix(hash+47))*(ore.horizontal-pad*2));
        return y<ore.minY||y>ore.maxY?null:new Point(x,y,z);
    }
    public Ore sample(int x,int y,int z,InfiniteTerrain.Biome biome){
        if(biome==null)return Ore.NONE;
        for(var ore:VEINS){
            if(y<ore.minY||y>ore.maxY)continue;
            var center=candidate(ore,Math.floorDiv(x,ore.horizontal),Math.floorDiv(y,ore.vertical),Math.floorDiv(z,ore.horizontal),biome);if(center==null)continue;
            double dx=x-center.x(),dy=y-center.y(),dz=z-center.z();
            double irregularity=.75+.5*unit(hash(x,y,z,ore.salt+71));
            if(dx*dx+dy*dy+dz*dz<=ore.radius*ore.radius*irregularity)return ore;
        }
        return Ore.NONE;
    }
}
