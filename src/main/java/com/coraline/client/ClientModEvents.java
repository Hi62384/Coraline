package com.coraline.client;

import com.coraline.CoralineMod;
import com.coraline.client.renderer.DollRenderer;
import com.coraline.client.renderer.WybieRenderer;
import com.coraline.registry.ModBlockEntities;
import com.coraline.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CoralineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->com.coraline.client.renderer.CharacterModel.clearCache());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(OtherWorldColorGrade::reset));
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(SeeingStoneClient::reset));
    }
    @SubscribeEvent public static void dimensionEffects(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event){
        event.register(com.coraline.otherworld.OtherWorld.KEY.location(),new OtherWorldSky());
        event.register(com.coraline.tycoon.TycoonWorld.KEY.location(),new TycoonSky());
        event.register(com.coraline.postgame.InfiniteRealm.TYPE,new OtherWorldSky());
        event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","true_heart"),new TrueHeartSky());
    }

    @SubscribeEvent
    public static void itemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? 0x655080 : 0xd2b58c,
                com.coraline.registry.ModItems.PARENTS_SPAWN_EGG.get());
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.coraline.client.renderer.OtherworldResidentModel.LAYER,com.coraline.client.renderer.OtherworldResidentModel::layer);
        event.registerLayerDefinition(com.coraline.client.renderer.WhiteVoidAvatarModel.LAYER,com.coraline.client.renderer.WhiteVoidAvatarModel::layer);
        event.registerLayerDefinition(com.coraline.client.renderer.PostgameHostileModel.LAYER,com.coraline.client.renderer.PostgameHostileModel::layer);
        event.registerLayerDefinition(com.coraline.client.renderer.WybieModel.LAYER, com.coraline.client.renderer.WybieModel::createLayer);
        event.registerLayerDefinition(com.coraline.client.renderer.MouseRenderer.LAYER,com.coraline.client.renderer.MouseRenderer::layer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CARNIVAL_TARGET.get(),com.coraline.client.renderer.CarnivalTargetRenderer::new);
        event.registerEntityRenderer(ModEntities.OTHERWORLD_RESIDENT.get(),com.coraline.client.renderer.OtherworldResidentRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_VOID_AVATAR.get(),com.coraline.client.renderer.WhiteVoidAvatarRenderer::new);
        event.registerEntityRenderer(ModEntities.TRUE_HEART.get(),com.coraline.client.renderer.TrueHeartRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_BOLT.get(),context->new com.coraline.client.renderer.VoidEffectRenderer<>(context,false));
        event.registerEntityRenderer(ModEntities.VOID_HAZARD.get(),context->new com.coraline.client.renderer.VoidEffectRenderer<>(context,true));
        for(var type:com.coraline.postgame.PostgameHostiles.TYPES.values())event.registerEntityRenderer(type.get(),com.coraline.client.renderer.PostgameHostileRenderer::new);
        event.registerEntityRenderer(com.coraline.tycoon.TycoonContent.NPC.get(),com.coraline.client.renderer.TycoonNpcRenderer::new);
        event.registerEntityRenderer(com.coraline.tycoon.TycoonAutomationContent.WORKER.get(),com.coraline.client.renderer.TycoonWorkerRenderer::new);
        event.registerEntityRenderer(com.coraline.tycoon.TycoonFoodContent.SEAT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(com.coraline.tycoon.TycoonPhaseTwoContent.DOLL_ACTOR.get(),com.coraline.client.renderer.TycoonDollRenderer::new);
        event.registerEntityRenderer(ModEntities.TEA_GLASS.get(),com.coraline.client.renderer.TeaGlassRenderer::new);
        com.coraline.registry.CharacterCast.ENTITIES.forEach((id,type)->event.registerEntityRenderer(type.get(),context->new com.coraline.client.renderer.CharacterRenderer(context,id)));
        event.registerEntityRenderer(ModEntities.MAGIC_BOLT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.SCENE_PROP.get(),com.coraline.client.renderer.ScenePropRenderer::new);
        event.registerEntityRenderer(ModEntities.WYBIE.get(), WybieRenderer::new);
        event.registerEntityRenderer(ModEntities.WYBIE_VISITOR.get(),com.coraline.client.renderer.WybieVisitorRenderer::new);
        event.registerEntityRenderer(ModEntities.BOBINSKY_VISITOR.get(),com.coraline.client.renderer.BobinskyVisitorRenderer::new);
        event.registerEntityRenderer(ModEntities.SPINK_VISITOR.get(),c->new com.coraline.client.renderer.TeaVisitorRenderer(c,"miss_spink"));event.registerEntityRenderer(ModEntities.FORCIBLE_VISITOR.get(),c->new com.coraline.client.renderer.TeaVisitorRenderer(c,"miss_forcible"));event.registerEntityRenderer(ModEntities.TEA_SEAT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DOLL.get(), DollRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INFINITE_SITE_CACHE.get(),net.minecraft.client.renderer.blockentity.ChestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BREACH_DRIVE.get(),com.coraline.client.renderer.BreachDriveRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MAGIC_MIRROR.get(),com.coraline.client.renderer.MagicMirrorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RIFT_MACHINE.get(),com.coraline.client.renderer.RiftMachineRenderer::new);
        event.registerEntityRenderer(ModEntities.MOM.get(),c->new com.coraline.client.renderer.ParentRenderer(c,true));
        event.registerEntityRenderer(ModEntities.DAD.get(),c->new com.coraline.client.renderer.ParentRenderer(c,false));
        event.registerEntityRenderer(ModEntities.BOUND_DOLL.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.TRACKED_DOOR.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MOUSE.get(),com.coraline.client.renderer.MouseRenderer::new);
        event.registerEntityRenderer(ModEntities.FISHING_CONTEST_NPC.get(),com.coraline.client.renderer.FishingContestNpcRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TINY_DOOR.get(),com.coraline.client.renderer.DoorPortalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TUNNEL_CORE.get(),com.coraline.client.renderer.TunnelRenderer::new);
    }
    @SubscribeEvent public static void addLayers(EntityRenderersEvent.AddLayers event){
        for(var type:new net.minecraft.world.entity.EntityType[]{net.minecraft.world.entity.EntityType.ZOMBIE,net.minecraft.world.entity.EntityType.HUSK,net.minecraft.world.entity.EntityType.SKELETON,net.minecraft.world.entity.EntityType.STRAY,net.minecraft.world.entity.EntityType.WITHER_SKELETON,net.minecraft.world.entity.EntityType.SPIDER,net.minecraft.world.entity.EntityType.CAVE_SPIDER,net.minecraft.world.entity.EntityType.ENDERMAN,net.minecraft.world.entity.EntityType.CREEPER}){
            var r=event.getRenderer(type);if(r!=null)r.addLayer(new com.coraline.client.renderer.MineEyes(r));
        }
        var renderer=event.getRenderer(net.minecraft.world.entity.EntityType.VILLAGER);
        if(renderer instanceof net.minecraft.client.renderer.entity.VillagerRenderer villager)
            villager.addLayer(new com.coraline.client.renderer.VillageVillagerEyes(villager));
        var vindicator=event.getRenderer(net.minecraft.world.entity.EntityType.VINDICATOR);
        if(vindicator instanceof net.minecraft.client.renderer.entity.VindicatorRenderer r)
            r.addLayer(new com.coraline.client.renderer.MansionWitnessEyes<>(r,m->((net.minecraft.client.model.IllagerModel<?>)m).getHead(),false));
        var evoker=event.getRenderer(net.minecraft.world.entity.EntityType.EVOKER);
        if(evoker instanceof net.minecraft.client.renderer.entity.EvokerRenderer r)
            r.addLayer(new com.coraline.client.renderer.MansionWitnessEyes<>(r,m->((net.minecraft.client.model.IllagerModel<?>)m).getHead(),false));
        var allay=event.getRenderer(net.minecraft.world.entity.EntityType.ALLAY);
        if(allay instanceof net.minecraft.client.renderer.entity.AllayRenderer r)
            r.addLayer(new com.coraline.client.renderer.MansionWitnessEyes<>(r,m->{var root=m.root();return root.hasChild("head")?root.getChild("head"):root.getChild("root").getChild("head");},true));
    }
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){
        event.enqueueWork(()->net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
            com.coraline.postgame.InfiniteContent.ILLUSION_MIRROR.get(),net.minecraft.client.renderer.RenderType.translucent()));
        event.enqueueWork(()->net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
            com.coraline.registry.ModBlocks.LOCKED_HOUSE_DOOR.get(),net.minecraft.client.renderer.RenderType.cutout()));
        event.enqueueWork(()->net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
            com.coraline.registry.ModBlocks.SOULGLASS_BLOCK.get(),net.minecraft.client.renderer.RenderType.translucent()));
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(com.coraline.food.TreatFoods.ITEMS.get("cotton_candy").get(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","bites"),(stack,level,entity,seed)->com.coraline.food.ReusableTreat.bites(stack)));
        event.enqueueWork(()->{
            var bow=com.coraline.registry.ModItems.ARCHERY_BOW.get();
            net.minecraft.client.renderer.item.ItemProperties.register(bow,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","pulling"),
                (stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1:0);
            net.minecraft.client.renderer.item.ItemProperties.register(bow,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","pull"),
                (stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?
                    (stack.getUseDuration()-entity.getUseItemRemainingTicks())/20.0F:0);
            net.minecraft.client.renderer.item.ItemProperties.register(com.coraline.registry.ModItems.FISHING_CONTEST_ROD.get(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","cast"),
                (stack,level,entity,seed)->entity instanceof net.minecraft.world.entity.player.Player player&&player.fishing!=null&&
                    (player.getMainHandItem()==stack||player.getOffhandItem()==stack)?1:0);
        });
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(com.coraline.registry.ModMenus.PARENTS.get(),com.coraline.client.ParentsScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(com.coraline.registry.ModMenus.BREACH_DRIVE.get(),BreachDriveScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(com.coraline.registry.ModMenus.MAGIC_MIRROR.get(),MagicMirrorScreen::new));
    }
}
