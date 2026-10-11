package com.coraline.client;

import com.coraline.config.OtherWorldVisualConfig;
import com.coraline.otherworld.RealmVisualState;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Bounded, lazy client-side garden. No world mutations, entities, inventories or chunk requests. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class OtherWorldNature {
    record Anchor(BlockPos pos,boolean water,boolean leaves,long seed){}
    private record Decoration(BlockPos pos,BlockState state,float scale,float dx,float dz,AABB bounds){}
    private static final Map<Long,Anchor> ANCHORS=new LinkedHashMap<>();
    private static final Map<Long,Decoration> DECOR=new LinkedHashMap<>();
    private static final Random RANDOM=new Random(82117);
    private static ClientLevel world;
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(world!=mc.level){world=mc.level;ANCHORS.clear();DECOR.clear();OtherWorldMotes.clear();RealmVisualState.client=null;}
        if(!OtherWorldAtmosphere.active()||mc.player==null){ANCHORS.clear();DECOR.clear();OtherWorldMotes.clear();return;}
        if(mc.isPaused())return;
        var view=RealmVisualState.client;if(view!=null&&view.tunnel()){ANCHORS.clear();DECOR.clear();OtherWorldMotes.clear();return;}
        ticks++;var center=mc.player.blockPosition();int radius=OtherWorldVisualConfig.NATURE_RADIUS.get();
        if(ticks%20==0){
            ANCHORS.values().removeIf(a->a.pos.distSqr(center)>radius*radius*1.5||!world.hasChunkAt(a.pos));
            DECOR.values().removeIf(d->d.pos.distSqr(center)>radius*radius||!world.hasChunkAt(d.pos)||!world.isEmptyBlock(d.pos)||!naturalGround(world.getBlockState(d.pos.below())));
        }
        for(int i=0;i<OtherWorldVisualConfig.SCAN_BUDGET.get();i++){
            int x=center.getX()+RANDOM.nextInt(radius*2+1)-radius,z=center.getZ()+RANDOM.nextInt(radius*2+1)-radius;
            if(!world.hasChunk(x>>4,z>>4))continue;
            var top=world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,new BlockPos(x,0,z));
            if(Math.abs(top.getY()-center.getY())>36)continue;
            var beneath=world.getBlockState(top.below());boolean leaves=beneath.is(BlockTags.LEAVES),water=beneath.getFluidState().is(FluidTags.WATER);
            if(!leaves&&!water&&!naturalGround(beneath)&&!beneath.is(BlockTags.FLOWERS)&&!beneath.is(Blocks.GRASS)&&!beneath.is(Blocks.TALL_GRASS))continue;
            if(!world.isEmptyBlock(top)||!world.canSeeSky(top))continue;
            long seed=Mth.getSeed(top);var anchor=new Anchor(top,water,leaves,seed);
            if(ANCHORS.size()<512)ANCHORS.put(top.asLong(),anchor);
            if(!leaves&&!water&&naturalGround(beneath))decorate(anchor,view);
        }
        OtherWorldMotes.tick(world,mc.player.position(),ANCHORS.values(),mc.options.particles().get());
    }
    static boolean naturalGround(BlockState s){return s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.MOSS_BLOCK)||s.is(Blocks.ROOTED_DIRT);}
    private static void decorate(Anchor a,RealmVisualState.View view){
        if(DECOR.size()>=OtherWorldVisualConfig.DECOR_CAP.get()||DECOR.containsKey(a.pos.asLong()))return;
        long cell=Mth.getSeed(a.pos.getX()>>2,0,a.pos.getZ()>>2);
        double nearHome=view==null?0:Math.max(0,1-Math.sqrt(a.pos.distSqr(view.center()))/48);
        double density=OtherWorldVisualConfig.FLOWERS.get()*(1+nearHome*.65);
        if(Math.floorMod(cell,1000)>=density*1000||Math.floorMod(a.seed,5)>1)return;
        BlockState[] plants={Blocks.PINK_TULIP.defaultBlockState(),Blocks.OXEYE_DAISY.defaultBlockState(),Blocks.CORNFLOWER.defaultBlockState(),Blocks.AZURE_BLUET.defaultBlockState(),Blocks.LILY_OF_THE_VALLEY.defaultBlockState(),Blocks.FERN.defaultBlockState()};
        // Cell-wide species create deliberate little clumps rather than multicolor noise.
        var state=plants[Math.floorMod(cell,plants.length)];float scale=.45F+Math.floorMod(a.seed,25)/100F;
        DECOR.put(a.pos.asLong(),new Decoration(a.pos,state,scale,Math.floorMod(a.seed>>8,21)/100F-.1F,Math.floorMod(a.seed>>16,21)/100F-.1F,new AABB(a.pos)));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!OtherWorldAtmosphere.active())return;
        var mc=Minecraft.getInstance();if(mc.level!=world)return;
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS){
            var pose=event.getPoseStack();var cam=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
            for(var d:DECOR.values()){
                if(!event.getFrustum().isVisible(d.bounds)||d.pos.distToCenterSqr(cam)>OtherWorldVisualConfig.NATURE_RADIUS.get()*OtherWorldVisualConfig.NATURE_RADIUS.get())continue;
                pose.pushPose();pose.translate(d.pos.getX()-cam.x+.5+d.dx,d.pos.getY()-cam.y,d.pos.getZ()-cam.z+.5+d.dz);
                pose.mulPose(Axis.YP.rotationDegrees(Math.floorMod(d.pos.asLong(),4)*90));pose.scale(d.scale,d.scale,d.scale);pose.translate(-.5,0,-.5);
                mc.getBlockRenderer().renderSingleBlock(d.state,pose,buffers,net.minecraft.client.renderer.LevelRenderer.getLightColor(world,d.pos),net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();
            }buffers.endBatch();
        }else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_PARTICLES)OtherWorldMotes.render(event);
    }
}
