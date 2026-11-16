package com.coraline.postgame;

import java.util.*;
import com.coraline.registry.ModEntities;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/** Real custom monster types. The remaining roster is not silently
 * substituted with renamed vanilla entities. */
@Mod.EventBusSubscriber(modid="coraline",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class PostgameHostiles {
    public enum Kind {
        BUTTON_EYED_WANDERER(.6F,1.85F,24,4,.23,0),
        CLOCKWORK_WATCHER(.7F,1.5F,32,6,.17,6),
        RIFT_SOLDIER(.65F,1.95F,30,5,.24,3),
        THREADCRAWLER(1.1F,.65F,20,4,.31,1),
        STITCHED_HOUND(.95F,.95F,26,5,.32,2),
        PORCELAIN_SENTINEL(1.4F,2.75F,70,12,.15,10),
        PAPER_WRAITH(.6F,1.7F,16,5,.27,0),
        ROOTBOUND_HORROR(1.1F,2.55F,42,8,.2,5),
        VOID_STALKER(.65F,2.2F,40,8,.29,4),
        FALSE_VILLAGER(.65F,1.95F,28,5,.25,2),
        THREAD_SPINNER(.9F,1.65F,28,4,.22,2),
        RIFT_GUARDIAN(1.6F,3.2F,160,10,.2,12);
        public final float width,height;public final double health,damage,speed,armor;
        Kind(float width,float height,double health,double damage,double speed,double armor){this.width=width;this.height=height;this.health=health;this.damage=damage;this.speed=speed;this.armor=armor;}
        public String id(){return name().toLowerCase(Locale.ROOT);}
    }
    public static final Map<Kind,RegistryObject<EntityType<OtherWorldHostile>>> TYPES=new EnumMap<>(Kind.class);
    static{for(var kind:Kind.values())TYPES.put(kind,ModEntities.ENTITIES.register(kind.id(),()->EntityType.Builder.<OtherWorldHostile>of(OtherWorldHostile::new,MobCategory.MONSTER).sized(kind.width,kind.height).clientTrackingRange(10).build("coraline:"+kind.id())));}
    public static Kind kind(EntityType<?> type){for(var entry:TYPES.entrySet())if(entry.getValue().get()==type)return entry.getKey();throw new IllegalArgumentException("Unknown postgame hostile type");}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){for(var entry:TYPES.entrySet())event.put(entry.getValue().get(),OtherWorldHostile.attributes(entry.getKey()).build());event.put(ModEntities.WHITE_VOID_AVATAR.get(),WhiteVoidAvatarEntity.attributes().build());event.put(ModEntities.TRUE_HEART.get(),TrueHeartEntity.attributes().build());event.put(ModEntities.OTHERWORLD_RESIDENT.get(),OtherworldResident.attributes().build());}
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event){event.enqueueWork(()->{
        for(var type:TYPES.values())SpawnPlacements.register(type.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,OtherWorldHostile::naturalSpawn);
    });}
    public static void init(){}
    private PostgameHostiles(){}
}
