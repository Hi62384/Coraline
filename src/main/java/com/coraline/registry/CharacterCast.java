package com.coraline.registry;

import com.coraline.entity.CharacterEntity;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

public final class CharacterCast {
    public static final List<String> IDS=List.of("wybie","other_wybie","bobinsky","other_bobinsky","miss_spink","miss_forcible","other_miss_spink","other_miss_forcible","evil_other_father","evil_other_bobinsky","evil_other_spink_forcible","evil_other_mother","beldam","coraline","cat");
    public static final Map<String,RegistryObject<EntityType<CharacterEntity>>> ENTITIES=new LinkedHashMap<>();
    public static void init(){
        if(!ENTITIES.isEmpty())return;
        for(String id:IDS){
            float height=id.equals("cat")?1:id.equals("beldam")?3.2F:id.equals("evil_other_mother")?3.1F:id.equals("evil_other_father")?2.45F:id.contains("bobinsky")?(id.equals("other_bobinsky")?3.4F:2.7F):2.0F;
            var type=ModEntities.ENTITIES.register("character_"+id,()->EntityType.Builder.<CharacterEntity>of(CharacterEntity::new,MobCategory.CREATURE).sized(id.equals("cat")?.5F:.7F,height).clientTrackingRange(10).updateInterval(2).build("character_"+id));
            ENTITIES.put(id,type);
            ModItems.ITEMS.register("character_"+id+"_spawn_egg",()->new ForgeSpawnEggItem(type,0x292D39,id.contains("other")?0xB78A4D:0x879BA3,new Item.Properties()));
        }
    }
    private CharacterCast(){}
}
