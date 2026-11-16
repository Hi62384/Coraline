package com.coraline.feast;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** Bounded preferences only. No item custody, reward issuance or claim receipt. */
final class VisitGiftHistory {
    private static final String KEY="VisitGiftHistory";
    private static final int LIMIT=8, ITEMS=16;
    record Bundle(int generation,List<String> ids) {}
    static List<Bundle> read(CompoundTag story) {
        var saved=story.getCompound(KEY).getList("Bundles",Tag.TAG_COMPOUND);
        var result=new ArrayList<Bundle>();
        for(int i=Math.max(0,saved.size()-LIMIT);i<saved.size();i++) {
            var row=saved.getCompound(i);var ids=row.getList("Items",Tag.TAG_STRING);var clean=new LinkedHashSet<String>();
            for(int j=0;j<Math.min(ITEMS,ids.size());j++) {
                String id=ids.getString(j);
                if(id.length()<=128&&ResourceLocation.tryParse(id)!=null)clean.add(id);
            }
            if(row.getInt("Generation")>0&&!clean.isEmpty())result.add(new Bundle(row.getInt("Generation"),List.copyOf(clean)));
        }
        return List.copyOf(result);
    }
    static boolean remember(CompoundTag story,int generation,Collection<String> ids) {
        if(generation<=0)return false;
        var recent=new ArrayList<>(read(story));
        if(recent.stream().anyMatch(bundle->bundle.generation()==generation))return false;
        var clean=new LinkedHashSet<String>();
        for(String id:ids)if(id!=null&&id.length()<=128&&ResourceLocation.tryParse(id)!=null&&clean.size()<ITEMS)clean.add(id);
        if(clean.isEmpty())return false;
        recent.add(new Bundle(generation,List.copyOf(clean)));while(recent.size()>LIMIT)recent.remove(0);
        var saved=new ListTag();
        for(var bundle:recent) {
            var row=new CompoundTag();row.putInt("Generation",bundle.generation());var items=new ListTag();
            bundle.ids().forEach(id->items.add(StringTag.valueOf(id)));row.put("Items",items);saved.add(row);
        }
        var tag=new CompoundTag();tag.put("Bundles",saved);story.put(KEY,tag);return true;
    }
    static int preference(Collection<String> equipment,Predicate<String> owned,List<Bundle> recent) {
        if(equipment.isEmpty())return 1;
        int weight=0;
        for(String id:new LinkedHashSet<>(equipment)) {
            int repeats=(int)recent.stream().filter(bundle->bundle.ids().contains(id)).count();
            weight+=Math.max(1,(owned.test(id)?3:15)/(1+repeats*3));
        }
        return Math.max(1,weight);
    }
    private VisitGiftHistory() {}
}
