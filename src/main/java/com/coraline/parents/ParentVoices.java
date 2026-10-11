package com.coraline.parents;

import com.coraline.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import java.util.*;

public final class ParentVoices {
    private static final Map<String,Integer> LENGTHS=load();
    private static final Map<String,List<String>> GROUPS=loadGroups();
    private static Map<String,List<String>> loadGroups(){
        var result=new HashMap<String,List<String>>();
        try(var in=ParentVoices.class.getResourceAsStream("/assets/coraline/parent_voice_groups.json")){
            if(in!=null)for(var group:com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().entrySet()){
                var clips=new ArrayList<String>();for(var clip:group.getValue().getAsJsonArray())clips.add(clip.getAsString());result.put(group.getKey(),clips);
            }
        }catch(Exception e){throw new IllegalStateException("Cannot read parent dialogue rotations",e);}return result;
    }
    private static Map<String,Integer> load(){
        var result=new HashMap<String,Integer>();
        try(var stream=ParentVoices.class.getResourceAsStream("/assets/coraline/parent_voice_lengths.json")){
            if(stream!=null)try(var reader=new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){
                com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsInt()));
            }
        }catch(Exception e){com.mojang.logging.LogUtils.getLogger().warn("Cannot read parent voice durations",e);}return result;
    }
    public static int length(SoundEvent sound){return LENGTHS.getOrDefault(sound.getLocation().getPath(),200);}
    public static SoundEvent select(boolean mom,boolean taskMode,boolean raining){
        if(mom)return taskMode?ModSounds.MOM_TASK.get():ModSounds.MOM_NEGLECT.get();
        return taskMode?ModSounds.DAD_TASK.get():raining?ModSounds.DAD_NEGLECT_RAIN.get():ModSounds.DAD_NEGLECT.get();
    }
    public static FamilyData.Family family(ParentEntity parent){
        var family=parent.family();
        if(family==null&&parent.isOtherParent()&&parent.getPersistentData().hasUUID("OriginalFamily"))family=FamilyData.get(parent.getServer()).families.get(parent.getPersistentData().getUUID("OriginalFamily"));
        return family;
    }
    public static SoundEvent dialogue(ParentEntity parent){
        var family=family(parent);
        var group=select(parent.isMom(),family!=null&&family.isAvailable(parent.getServer()),parent.level().isRaining());
        var options=GROUPS.get(group.getLocation().getPath());if(options==null||options.isEmpty())return group;
        var data=parent.getPersistentData();String key="VoiceRotation_"+group.getLocation().getPath();
        var remaining=data.getList(key,8);var valid=new net.minecraft.nbt.ListTag();
        for(var clip:remaining)if(options.contains(clip.getAsString()))valid.add(clip.copy());
        if(valid.isEmpty())for(String clip:options)valid.add(net.minecraft.nbt.StringTag.valueOf(clip));
        String choice=valid.remove(parent.getRandom().nextInt(valid.size())).getAsString();data.put(key,valid);
        var registered=ModSounds.PARENT_CLIPS.get(choice);return registered==null?group:registered.get();
    }
}
