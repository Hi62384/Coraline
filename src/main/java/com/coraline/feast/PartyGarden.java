package com.coraline.feast;

import com.google.gson.*;
import com.coraline.otherworld.RealmData;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Uses the very same authored garden terrain and transform as RosewoodGarden.build; never invents a yard. */
public final class PartyGarden {
    public record Cell(int x,int y,int z){}
    private static List<Cell> layout;
    private record Key(long house,long porch){}
    private static final Map<Key,List<PlayerParty.Area>> GROUNDS=new HashMap<>();
    public static synchronized List<Cell> layout(){
        if(layout!=null)return layout;
        try(var raw=PartyGarden.class.getResourceAsStream("/assets/coraline/structures/rosewood_garden.json.gz")){
            if(raw==null)throw new IllegalStateException("Authored Rosewood garden resource is missing");
            try(var in=new InputStreamReader(new GZIPInputStream(raw),StandardCharsets.UTF_8)){
                var json=JsonParser.parseReader(in).getAsJsonObject();var cells=new ArrayList<Cell>();
                for(var e:json.getAsJsonArray("terrain")){var a=e.getAsJsonArray();cells.add(new Cell(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()));}
                if(cells.isEmpty())throw new IllegalStateException("Authored Rosewood garden has no terrain");layout=List.copyOf(cells);return layout;
            }
        }catch(IOException|JsonParseException e){throw new IllegalStateException("Cannot read authored Rosewood garden",e);}
    }
    public static BlockPos feet(BlockPos house,Cell cell){return house.offset(cell.x()-67,cell.y()-2,cell.z()-129);}
    public static List<PlayerParty.Area> grounds(ServerPlayer p,VisitData.Scene s){
        var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null||realm.porch==null)return List.of();var h=realm.house;var key=new Key(h.asLong(),realm.porch.asLong());if(GROUNDS.containsKey(key))return GROUNDS.get(key);String dim=com.coraline.otherworld.OtherWorld.KEY.location().toString();
        // Merge only contiguous terrain cells at the same authored height. Holes and distant land are not permission.
        var out=new ArrayList<PlayerParty.Area>();var tiles=layout().stream().map(c->new PartyLayoutRules.Tile(c.x(),c.y(),c.z())).toList();
        for(var strip:PartyLayoutRules.strips(tiles))strip(out,h,dim,strip.first(),strip.last(),strip.y(),strip.z());
        // Exact front-step approach written by RosewoodGarden.build (z -49..-38, x -1..3).
        out.add(new PlayerParty.Area("porch",dim,new AABB(h.getX()-1,h.getY()-1,h.getZ()-49,h.getX()+4,h.getY()+4,h.getZ()-37)));
        out.add(new PlayerParty.Area("porch",dim,new AABB(realm.porch.offset(-2,0,-2),realm.porch.offset(3,4,3))));var immutable=List.copyOf(out);GROUNDS.put(key,immutable);return immutable;
    }
    private static void strip(List<PlayerParty.Area> out,BlockPos h,String dim,int first,int last,int y,int z){var a=feet(h,new Cell(first,y,z));var b=feet(h,new Cell(last,y,z));out.add(new PlayerParty.Area("backyard",dim,new AABB(a.getX(),a.getY()-.05,a.getZ(),b.getX()+1,b.getY()+4,b.getZ()+1)));}
    private PartyGarden(){}
}
