package com.coraline.registry;

import com.coraline.CoralineMod;
import com.coraline.entity.WybieEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static RegistryObject<EntityType<com.coraline.tea.TeaGlassEntity>> glass(){return ENTITIES.register("tea_glass",()->EntityType.Builder.<com.coraline.tea.TeaGlassEntity>of(com.coraline.tea.TeaGlassEntity::new,MobCategory.MISC).sized(.28F,.35F).clientTrackingRange(16).updateInterval(1).build("tea_glass"));}
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CoralineMod.MOD_ID);
    public static final RegistryObject<EntityType<com.coraline.tea.TeaGlassEntity>> TEA_GLASS=glass();
    public static final RegistryObject<EntityType<com.coraline.postgame.TrueHeartEntity>> TRUE_HEART=ENTITIES.register("true_heart",()->EntityType.Builder.<com.coraline.postgame.TrueHeartEntity>of(com.coraline.postgame.TrueHeartEntity::new,MobCategory.MONSTER).sized(3.2F,6.8F).clientTrackingRange(16).fireImmune().build("coraline:true_heart"));
    public static final RegistryObject<EntityType<com.coraline.postgame.WhiteVoidAvatarEntity>> WHITE_VOID_AVATAR=ENTITIES.register("white_void_avatar",()->EntityType.Builder.<com.coraline.postgame.WhiteVoidAvatarEntity>of(com.coraline.postgame.WhiteVoidAvatarEntity::new,MobCategory.MONSTER).sized(2.4F,5.6F).clientTrackingRange(16).fireImmune().build("coraline:white_void_avatar"));
    public static final RegistryObject<EntityType<com.coraline.postgame.OtherworldResident>> OTHERWORLD_RESIDENT=ENTITIES.register("otherworld_resident",()->EntityType.Builder.<com.coraline.postgame.OtherworldResident>of(com.coraline.postgame.OtherworldResident::new,MobCategory.CREATURE).sized(.7F,1.95F).clientTrackingRange(10).build("coraline:otherworld_resident"));
    public static final RegistryObject<EntityType<com.coraline.postgame.CarnivalTargetEntity>> CARNIVAL_TARGET=ENTITIES.register("carnival_target",()->EntityType.Builder.<com.coraline.postgame.CarnivalTargetEntity>of(com.coraline.postgame.CarnivalTargetEntity::new,MobCategory.MISC).sized(.95F,.95F).clientTrackingRange(10).updateInterval(1).fireImmune().build("coraline:carnival_target"));
    public static final RegistryObject<EntityType<com.coraline.postgame.VoidBoltEntity>> VOID_BOLT=ENTITIES.register("void_bolt",()->EntityType.Builder.<com.coraline.postgame.VoidBoltEntity>of(com.coraline.postgame.VoidBoltEntity::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(10).updateInterval(1).build("coraline:void_bolt"));
    public static final RegistryObject<EntityType<com.coraline.postgame.VoidHazardEntity>> VOID_HAZARD=ENTITIES.register("void_hazard",()->EntityType.Builder.<com.coraline.postgame.VoidHazardEntity>of(com.coraline.postgame.VoidHazardEntity::new,MobCategory.MISC).sized(2.5F,.15F).clientTrackingRange(10).build("coraline:void_hazard"));
    public static final RegistryObject<EntityType<com.coraline.feast.CinematicPropEntity>> CINEMATIC_PROP=ENTITIES.register("cinematic_prop",()->EntityType.Builder.<com.coraline.feast.CinematicPropEntity>of(com.coraline.feast.CinematicPropEntity::new,MobCategory.MISC).sized(.8F,.8F).clientTrackingRange(32).updateInterval(1).build("coraline:cinematic_prop"));
    public static final RegistryObject<EntityType<com.coraline.feast.WybieBicycleEntity>> WYBIE_BICYCLE=ENTITIES.register("wybie_bicycle",()->EntityType.Builder.<com.coraline.feast.WybieBicycleEntity>of(com.coraline.feast.WybieBicycleEntity::new,MobCategory.MISC).sized(.9F,1.15F).clientTrackingRange(14).updateInterval(1).fireImmune().build("coraline:wybie_bicycle"));

    public static final RegistryObject<EntityType<WybieEntity>> WYBIE = ENTITIES.register(
            "wybie",
            () -> EntityType.Builder.of(WybieEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 1.8F)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("wybie")
    );

    private ModEntities() {
    }
    public static final RegistryObject<EntityType<com.coraline.tea.TeaVisitorEntity>> SPINK_VISITOR=ENTITIES.register("spink_visitor",()->EntityType.Builder.<com.coraline.tea.TeaVisitorEntity>of(com.coraline.tea.TeaVisitorEntity::new,MobCategory.CREATURE).sized(.8F,1.75F).clientTrackingRange(16).updateInterval(1).fireImmune().build("spink_visitor"));
    public static final RegistryObject<EntityType<com.coraline.tea.TeaVisitorEntity>> FORCIBLE_VISITOR=ENTITIES.register("forcible_visitor",()->EntityType.Builder.<com.coraline.tea.TeaVisitorEntity>of(com.coraline.tea.TeaVisitorEntity::new,MobCategory.CREATURE).sized(.8F,2F).clientTrackingRange(16).updateInterval(1).fireImmune().build("forcible_visitor"));
    public static final RegistryObject<EntityType<com.coraline.tea.TeaSeatEntity>> TEA_SEAT=ENTITIES.register("tea_seat",()->EntityType.Builder.<com.coraline.tea.TeaSeatEntity>of(com.coraline.tea.TeaSeatEntity::new,MobCategory.MISC).sized(.5F,.3F).clientTrackingRange(12).updateInterval(1).build("tea_seat"));
    public static final RegistryObject<EntityType<com.coraline.wybie.WybieVisitorEntity>> WYBIE_VISITOR=ENTITIES.register("wybie_visitor",()->EntityType.Builder.<com.coraline.wybie.WybieVisitorEntity>of(com.coraline.wybie.WybieVisitorEntity::new,MobCategory.CREATURE).sized(.8F,2.15F).clientTrackingRange(16).updateInterval(1).fireImmune().build("wybie_visitor"));
    public static final RegistryObject<EntityType<com.coraline.bobinsky.BobinskyVisitorEntity>> BOBINSKY_VISITOR=ENTITIES.register("bobinsky_visitor",()->EntityType.Builder.<com.coraline.bobinsky.BobinskyVisitorEntity>of(com.coraline.bobinsky.BobinskyVisitorEntity::new,MobCategory.CREATURE).sized(.8F,2.7F).clientTrackingRange(20).updateInterval(1).fireImmune().build("bobinsky_visitor"));
    public static final RegistryObject<EntityType<com.coraline.clothing.MagicBolt>> MAGIC_BOLT=ENTITIES.register("magic_bolt",()->EntityType.Builder.<com.coraline.clothing.MagicBolt>of(com.coraline.clothing.MagicBolt::new,MobCategory.MISC).sized(.2F,.2F).clientTrackingRange(8).updateInterval(1).build("magic_bolt"));
    public static final RegistryObject<EntityType<com.coraline.feast.ScenePropEntity>> SCENE_PROP=ENTITIES.register("scene_prop",()->EntityType.Builder.<com.coraline.feast.ScenePropEntity>of(com.coraline.feast.ScenePropEntity::new,MobCategory.MISC).sized(.6F,.5F).clientTrackingRange(16).updateInterval(2).build("scene_prop"));
    public static final RegistryObject<EntityType<com.coraline.otherworld.MouseEntity>> MOUSE=ENTITIES.register("mouse",()->EntityType.Builder.of(com.coraline.otherworld.MouseEntity::new,MobCategory.CREATURE).sized(.3F,.25F).clientTrackingRange(12).updateInterval(1).build("mouse"));
    public static final RegistryObject<EntityType<com.coraline.feast.FishingContestNpcEntity>> FISHING_CONTEST_NPC=ENTITIES.register("fishing_contest_npc",()->EntityType.Builder.of(com.coraline.feast.FishingContestNpcEntity::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(12).updateInterval(2).build("fishing_contest_npc"));
    public static final RegistryObject<EntityType<com.coraline.otherworld.DoorItemEntity>> TRACKED_DOOR=ENTITIES.register("tracked_door",()->EntityType.Builder.<com.coraline.otherworld.DoorItemEntity>of(com.coraline.otherworld.DoorItemEntity::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(6).updateInterval(20).build("tracked_door"));
    public static final RegistryObject<EntityType<com.coraline.parents.ParentEntity>> MOM=parent("mom");
    public static final RegistryObject<EntityType<com.coraline.parents.ParentEntity>> DAD=parent("dad");
    public static final RegistryObject<EntityType<com.coraline.doll.BoundDollEntity>> BOUND_DOLL=ENTITIES.register("bound_doll",()->EntityType.Builder.<com.coraline.doll.BoundDollEntity>of(com.coraline.doll.BoundDollEntity::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(6).updateInterval(20).build("bound_doll"));
    private static RegistryObject<EntityType<com.coraline.parents.ParentEntity>> parent(String name){
        return ENTITIES.register(name,()->EntityType.Builder.of(com.coraline.parents.ParentEntity::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(10).updateInterval(2).build(name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
