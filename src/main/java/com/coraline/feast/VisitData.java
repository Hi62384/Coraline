package com.coraline.feast;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;
import java.util.*;
public final class VisitData extends SavedData {
    public enum Stage { MOTHER_INTRO,FIND_FATHER,FATHER_ENTER,FATHER_INTRO,SONG,DINNER_READY,RETURN_TO_MOTHER,SIT,SUPPER,CAKE,WAITING,BED,DONE }
    public static final class Scene {
        public UUID owner,door,mom,dad,family;
        public Stage stage=Stage.MOTHER_INTRO;
        public int elapsed;
        public OtherMotherLoopState loop=new OtherMotherLoopState();
        public long supperMusicEndsAt;
        public BlockPos piano,table;
        public boolean cakeTaken;
        public OtherMotherWelcome.State welcomeState=OtherMotherWelcome.State.LOW_TRUST_LOW_SUSPICION;
        public String lastWelcome="";
        public long welcomeAfter;
        public final Map<String,String> lastDialogue=new HashMap<>();
    }
    public final Map<UUID,Scene> scenes=new HashMap<>();
    public static VisitData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(VisitData::load,VisitData::new,"coraline_first_visits");}
    public static VisitData load(CompoundTag root){var data=new VisitData();for(var tag:root.getList("Scenes",10)){
        var n=(CompoundTag)tag;var s=new Scene();s.owner=n.getUUID("Owner");s.door=n.getUUID("Door");s.mom=n.hasUUID("Mom")?n.getUUID("Mom"):null;s.dad=n.hasUUID("Dad")?n.getUUID("Dad"):null;
        try{s.stage=Stage.valueOf(n.getString("Stage"));}catch(IllegalArgumentException e){s.stage=Stage.DONE;}
        s.welcomeState=OtherMotherWelcome.State.fromId(n.getString("WelcomeState"));
        s.loop=OtherMotherLoopState.load(n.getCompound("MainLoop"));
        if(!n.contains("MainLoop"))s.loop.visits=s.stage==Stage.DONE?1:0;
        s.lastWelcome=n.getString("LastWelcome");s.welcomeAfter=n.getLong("WelcomeAfter");
        var history=n.getCompound("OtherMotherDialogue");
        for(var pool:OtherMotherDialogue.pools())if(history.contains(pool.path(),8))s.lastDialogue.put(pool.path(),history.getString(pool.path()));
        s.family=n.hasUUID("Family")?n.getUUID("Family"):null;s.elapsed=n.getInt("Elapsed");s.supperMusicEndsAt=n.getLong("SupperMusicEndsAt");s.piano=BlockPos.of(n.getLong("Piano"));s.table=BlockPos.of(n.getLong("Table"));s.cakeTaken=n.getBoolean("CakeTaken");data.scenes.put(s.owner,s);
    }return data;}
    @Override public CompoundTag save(CompoundTag root){var list=new ListTag();for(var s:scenes.values()){
        var n=new CompoundTag();n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);if(s.mom!=null)n.putUUID("Mom",s.mom);if(s.dad!=null)n.putUUID("Dad",s.dad);n.putString("Stage",s.stage.name());n.putInt("Elapsed",s.elapsed);n.putLong("SupperMusicEndsAt",s.supperMusicEndsAt);n.putLong("Piano",s.piano.asLong());n.putLong("Table",s.table.asLong());n.putBoolean("CakeTaken",s.cakeTaken);list.add(n);
        if(s.family!=null)n.putUUID("Family",s.family);
        n.put("MainLoop",s.loop.save());
        n.putString("WelcomeState",s.welcomeState.id());n.putString("LastWelcome",s.lastWelcome);n.putLong("WelcomeAfter",s.welcomeAfter);
        var history=new CompoundTag();s.lastDialogue.forEach(history::putString);n.put("OtherMotherDialogue",history);
    }root.put("Scenes",list);return root;}
}
