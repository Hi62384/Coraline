package com.coraline.bobinsky;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
/** House recovery remains available even after its family's VisitData scene is deleted. */
public final class BobinskyVisitData extends SavedData {
    public final Map<UUID,CompoundTag> visits=new HashMap<>();
    public final Map<UUID,CompoundTag> teaVisits=new HashMap<>();
    public final Set<Long> playerPlaced=new HashSet<>();
    public static BobinskyVisitData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(BobinskyVisitData::load,BobinskyVisitData::new,"coraline_bobinsky_visits");}
    public static BobinskyVisitData load(CompoundTag root){var d=new BobinskyVisitData();for(var t:root.getList("Visits",10)){var n=(CompoundTag)t;d.visits.put(n.getUUID("Owner"),n.copy());}for(var t:root.getList("TeaVisits",10)){var n=(CompoundTag)t;d.teaVisits.put(n.getUUID("Owner"),n.copy());}for(long pos:root.getLongArray("PlayerPlaced"))d.playerPlaced.add(pos);return d;}
    @Override public CompoundTag save(CompoundTag root){var list=new ListTag();visits.values().forEach(n->list.add(n.copy()));root.put("Visits",list);var tea=new ListTag();teaVisits.values().forEach(n->tea.add(n.copy()));root.put("TeaVisits",tea);root.putLongArray("PlayerPlaced",playerPlaced.stream().mapToLong(Long::longValue).toArray());return root;}
}
