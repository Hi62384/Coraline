package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Deterministic owner-local scenery and compact generated-cell provenance.
 * Chunk dimensions remain vanilla 16x16; the special core straddles four cells.
 */
public final class TycoonLandscape {
    public static final String[] TIERS={"Plain Foundation","Grassland","Meadow","Garden","Candy Landscape","Enchanted Landscape"};
    public static String key(int x,int z){return x+","+z;}
    public static boolean inBounds(int x,int z){return x>=-8&&x<8&&z>=-8&&z<8;}
    public static int tileX(TycoonData.Home h,BlockPos at){return Math.floorDiv(at.getX()-h.center().getX(),16);}
    public static int tileZ(TycoonData.Home h,BlockPos at){return Math.floorDiv(at.getZ()-h.center().getZ(),16);}
    public static boolean core(TycoonData.Home h,BlockPos at){int x=at.getX()-h.center().getX(),z=at.getZ()-h.center().getZ();return x>=-4&&x<4&&z>=-4&&z<4;}
    public static boolean owned(TycoonData.Home h,BlockPos at){int x=tileX(h,at),z=tileZ(h,at);return inBounds(x,z)&&(core(h,at)||h.state.getCompound("Terrain").contains(key(x,z)));}
    public static boolean adjacent(TycoonData.Home h,int x,int z){if(!inBounds(x,z))return false;if((x==-1||x==0)&&(z==-1||z==0))return true;var tiles=h.state.getCompound("Terrain");return tiles.contains(key(x-1,z))||tiles.contains(key(x+1,z))||tiles.contains(key(x,z-1))||tiles.contains(key(x,z+1));}
    public static long seed(TycoonData.Home h){return h.owner.getMostSignificantBits()^h.owner.getLeastSignificantBits()^0x71EADE5L;}
    public static int height(TycoonData.Home h,int x,int z){double phase=Math.floorMod(seed(h),10000)/1000.;double noise=Math.sin(x*.105+phase)*1.35+Math.cos(z*.083-phase)*1.15+Math.sin((x+z)*.15+phase)*.65;double fade=Math.min(1,Math.max(0,(Math.max(Math.abs(x),Math.abs(z))-5)/12.));return h.center().getY()-1+(int)Math.round(noise*fade);}
    private static long hash(long seed,int x,int z){long v=seed+x*341873128712L+z*132897987541L;v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;return v^(v>>>27);}
    private static void put(Map<BlockPos,BlockState> out,BlockPos at,Block b){out.put(at,b.defaultBlockState());}

    public static Map<BlockPos,BlockState> terrain(TycoonData.Home h,int tx,int tz,int tier){
        var out=new LinkedHashMap<BlockPos,BlockState>();var c=h.center();long seed=seed(h);
        for(int x=tx*16;x<tx*16+16;x++)for(int z=tz*16;z<tz*16+16;z++){
            var at=new BlockPos(c.getX()+x,height(h,x,z),c.getZ()+z);if(core(h,at))continue;
            long detail=hash(seed,x,z);int style=tier;
            // Deliberate two-block material transition; surface height is global
            // noise independent of tier, so unlike neighbors share the same edge.
            int lx=Math.floorMod(x,16),lz=Math.floorMod(z,16);int nx=lx<2?tx-1:lx>13?tx+1:tx,nz=lz<2?tz-1:lz>13?tz+1:tz;
            var neighbor=h.state.getCompound("Terrain").getCompound(key(nx,nz));if((nx!=tx||nz!=tz)&&!neighbor.isEmpty()&&(detail&3)==0)style=neighbor.getInt("Tier");
            for(int y=c.getY()-7;y<at.getY();y++)put(out,new BlockPos(at.getX(),y,at.getZ()),y<at.getY()-2?Blocks.STONE:tier>=4?Blocks.WHITE_TERRACOTTA:Blocks.DIRT);
            put(out,at,switch(style){case 0->Blocks.SMOOTH_STONE;case 4->(detail&3)==0?Blocks.PINK_CONCRETE:(detail&3)==1?Blocks.MAGENTA_TERRACOTTA:Blocks.WHITE_TERRACOTTA;case 5->(detail&7)==0?Blocks.MOSS_BLOCK:Blocks.GRASS_BLOCK;default->Blocks.GRASS_BLOCK;});
            if(tier==1&&Math.floorMod(detail,19)==0)put(out,at.above(),Blocks.GRASS);
            if(tier>=2&&Math.floorMod(detail,11)==0)put(out,at.above(),switch(tier){case 2->(detail&1)==0?Blocks.CORNFLOWER:Blocks.OXEYE_DAISY;case 3->(detail&1)==0?Blocks.ALLIUM:Blocks.AZURE_BLUET;case 4->Blocks.PINK_CANDLE;default->Blocks.FLOWERING_AZALEA;});
            if(tier==3&&Math.floorMod(x+z,13)==0)put(out,at,Blocks.MOSSY_COBBLESTONE);
            if(tier>=3&&Math.floorMod(detail,137)==0&&lx>=2&&lx<=13&&lz>=2&&lz<=13){
                int trunk=tier>=4?3:2;for(int y=1;y<=trunk;y++)put(out,at.above(y),tier==4?(y%2==0?Blocks.WHITE_CONCRETE:Blocks.RED_CONCRETE):tier==5?Blocks.STRIPPED_CRIMSON_STEM:Blocks.OAK_LOG);
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=0;dy<=2;dy++)if(dx*dx+dz*dz+dy*dy<=5){var leaf=at.offset(dx,trunk+dy,dz);if(tier==3)out.put(leaf,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT,true));else put(out,leaf,tier==4?(Math.floorMod(dx+dz,2)==0?Blocks.PINK_STAINED_GLASS:Blocks.PINK_CONCRETE):dy==1&&dx==0&&dz==0?Blocks.SEA_LANTERN:Blocks.WARPED_WART_BLOCK);}
            }
            if(tier==5&&Math.floorMod(detail,61)==0)put(out,at.above(),Blocks.SHROOMLIGHT);
        }
        return out;
    }

    /** Four bytes/cell rather than a huge compound per generated block. */
    public static CompoundTag encode(Map<BlockPos,BlockState> cells,BlockPos origin){
        var palette=new ArrayList<BlockState>();var values=new int[cells.size()];int i=0;
        for(var e:cells.entrySet()){int x=e.getKey().getX()-origin.getX()+32,z=e.getKey().getZ()-origin.getZ()+32,y=e.getKey().getY()-origin.getY()+16;if(x<0||x>=64||z<0||z>=64||y<0||y>=64)throw new IllegalArgumentException("Scenery plan exceeds local codec");int state=palette.indexOf(e.getValue());if(state<0){state=palette.size();palette.add(e.getValue());}if(state>=8192)throw new IllegalArgumentException("Scenery palette too large");values[i++]=x|(z<<6)|(y<<12)|(state<<18);}
        var n=new CompoundTag();n.putLong("Origin",origin.asLong());n.putIntArray("Cells",values);var tags=new ListTag();palette.forEach(s->tags.add(NbtUtils.writeBlockState(s)));n.put("Palette",tags);return n;
    }
    public static Map<BlockPos,BlockState> decode(ServerLevel l,CompoundTag n){var out=new LinkedHashMap<BlockPos,BlockState>();if(!n.contains("Origin"))return out;var origin=BlockPos.of(n.getLong("Origin"));var palette=n.getList("Palette",10);for(int value:n.getIntArray("Cells")){int index=value>>>18;if(index>=palette.size())throw new IllegalStateException("Corrupt generated-cell palette");var at=origin.offset((value&63)-32,((value>>>12)&63)-16,((value>>>6)&63)-32);out.put(at,NbtUtils.readBlockState(l.holderLookup(Registries.BLOCK),palette.getCompound(index)));}return out;}

    public static String purchase(ServerPlayer p,int x,int z,int tier){
        var h=TycoonWorld.home(p);if(p.serverLevel().dimension()!=TycoonWorld.KEY&&!TycoonWorkers.constructing(p)||!h.active()||tier<0||tier>5||!inBounds(x,z))return "Invalid terrain selection.";
        var old=h.state.getCompound("Terrain").getCompound(key(x,z));boolean upgrade=!old.isEmpty();if(!upgrade&&!adjacent(h,x,z))return "Select a chunk touching the core or purchased land.";if(upgrade&&tier<=old.getInt("Tier"))return "Choose a higher replacement tier.";
        int cost=TycoonPhaseTwoConfig.TERRAIN[tier].get();if(upgrade)cost=Math.max(1,(cost-TycoonPhaseTwoConfig.TERRAIN[old.getInt("Tier")].get())*3/4);
        var next=terrain(h,x,z,tier);var record=encode(next,h.center().offset(x*16,0,z*16));record.putInt("Tier",tier);record.putLong("Seed",seed(h));var owned=h.state.getCompound("Terrain").copy();owned.put(key(x,z),record);var patch=new CompoundTag();patch.put("Terrain",owned);
        return TycoonBuildJob.begin(p,"terrain "+x+","+z,old,next,record.getLong("Origin"),patch,cost);
    }

    public record Goodness(double terrain,double house,double sky){public double total(){return Math.min(100,terrain+house+sky);}}
    private static Goodness calculate(TycoonData.Home h){var all=h.state.getCompound("Terrain");double sum=0;var tiers=new HashSet<Integer>();for(String key:all.getAllKeys()){int t=Math.max(0,Math.min(5,all.getCompound(key).getInt("Tier")));sum+=new double[]{.015,.18,.36,.57,.79,1}[t];tiers.add(t);}double coverage=1-Math.exp(-sum/20.);double quality=all.isEmpty()?0:sum/all.size();double diversity=1+Math.min(.12,Math.max(0,tiers.size()-1)*.025);double land=Math.min(55,55*coverage*(.25+.75*quality)*diversity);land=Math.max(0,land-Math.min(land*.5,h.state.getCompound("SceneryDamage").size()*.025));return new Goodness(land,Math.max(0,Math.min(5,h.state.getInt("HouseTier")))*6,Math.max(0,Math.min(5,h.state.getInt("SkyTier")))*3);}
    public static Goodness goodness(TycoonData.Home h){return h.state.contains("WorldGoodness")?new Goodness(h.state.getDouble("TerrainGoodness"),h.state.getDouble("HouseGoodness"),h.state.getDouble("SkyGoodness")):calculate(h);}
    public static void recalculate(TycoonData.Home h){var g=calculate(h);h.state.putDouble("WorldGoodness",g.total());h.state.putDouble("TerrainGoodness",g.terrain);h.state.putDouble("HouseGoodness",g.house);h.state.putDouble("SkyGoodness",g.sky);}
    private TycoonLandscape(){}
}
