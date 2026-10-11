package com.coraline.client.renderer;
import com.coraline.feast.ScenePropEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Compact block-textured furniture meshes, using resource-pack materials. */
public final class ScenePropRenderer extends EntityRenderer<ScenePropEntity> {
    public ScenePropRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.4F;}
    @Override public ResourceLocation getTextureLocation(ScenePropEntity p){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
    private static void box(PoseStack pose,MultiBufferSource buffers,int light,Block material,float x,float y,float z,float w,float h,float d){
        pose.pushPose();pose.translate(x,y,z);pose.scale(w,h,d);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(material.defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);pose.popPose();
    }
    private static void curtainBox(PoseStack pose,MultiBufferSource buffers,String texture,float x,float y,float z,float w,float h,float d){
        // Stage-lit cloth: emissive shader avoids camera-dependent entity diffuse
        // lighting on the extremely non-uniformly scaled old block meshes.
        // Vanilla translucent-emissive writes COLOR ONLY: later props/particles draw through it.
        var out=buffers.getBuffer(StageCurtainRenderType.cloth(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/"+texture+".png")));
        float[][] corners={{x,y,z},{x+w,y,z},{x+w,y+h,z},{x,y+h,z},{x,y,z+d},{x+w,y,z+d},{x+w,y+h,z+d},{x,y+h,z+d}};
        int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{0,1,5,4},{3,7,6,2}};
        for(var face:faces)for(int i=0;i<4;i++){
            var v=corners[face[i]];
            out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(210,210,210,255).uv(i==0||i==3?0:1,i<2?1:0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pose.last().normal(),0,0,1).endVertex();
        }
    }
    @Override public void render(ScenePropEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var client=Minecraft.getInstance();
        if(e.kind()==ScenePropEntity.CARNIVAL_GONDOLA&&client.options.getCameraType().isFirstPerson()&&client.player!=null&&client.player.getVehicle()==e)return;
        boolean mantisCockpit=e.kind()==ScenePropEntity.GARDEN_MANTIS&&client.options.getCameraType().isFirstPerson()&&client.player!=null&&client.player.getVehicle()==e;
        pose.pushPose();
        switch(e.kind()){
            case ScenePropEntity.FESTIVAL_CONSOLE->{
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-1.5F,.78F,-.5F,3,.16F,1);
                for(float x:new float[]{-1.35F,1.15F})box(pose,buffers,light,Blocks.GOLD_BLOCK,x,0,-.4F,.18F,.78F,.8F);
                for(int i=0;i<4;i++){
                    Block color=switch(i){case 0->Blocks.ORANGE_TERRACOTTA;case 1->Blocks.WARPED_PLANKS;case 2->Blocks.PURPUR_BLOCK;default->Blocks.PINK_TERRACOTTA;};
                    boolean pressed=e.mode()==i+1&&e.modeAge()<5;float x=-1.3F+i*.65F;
                    box(pose,buffers,light,color,x,.94F-(pressed?.05F:0),-.44F,.59F,.10F,.72F);
                    box(pose,buffers,pressed?15728880:light,Blocks.SEA_LANTERN,x+.18F,1.045F-(pressed?.05F:0),-.35F,.21F,.025F,.15F);
                }
                // A mounted microphone and back panel frame the playable keyboard.
                box(pose,buffers,light,Blocks.BLACK_CONCRETE,1.7F,0,-.4F,.07F,1.8F,.07F);
                box(pose,buffers,light,Blocks.IRON_BLOCK,1.5F,1.72F,-.45F,.28F,.12F,.15F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-1.5F,1.02F,-.5F,3,.08F,.06F);
            }
            case ScenePropEntity.FESTIVAL_TIP->{
                pose.scale(.35F,.35F,.35F);pose.mulPose(Axis.YP.rotationDegrees((e.tickCount+partial)*12));
                Minecraft.getInstance().getItemRenderer().renderStatic(e.food(0),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId());
            }
            case ScenePropEntity.CIRCUS_CHOICE,ScenePropEntity.CIRCUS_BET->{
                pose.mulPose(Axis.YP.rotationDegrees(180));
                boolean choice=e.kind()==ScenePropEntity.CIRCUS_CHOICE;
                Block coat=choice?switch(e.mode()){case 0->Blocks.WHITE_WOOL;case 1->Blocks.BROWN_WOOL;case 2->Blocks.ORANGE_WOOL;
                    case 3->Blocks.LIGHT_GRAY_WOOL;case 4->Blocks.BLACK_WOOL;default->Blocks.PURPLE_WOOL;}:e.mode()==0?Blocks.LIME_WOOL:Blocks.RED_WOOL;
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-.40F,0,-.40F,.80F,.75F,.80F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.43F,.72F,-.43F,.86F,.10F,.86F);
                if(choice){
                    box(pose,buffers,light,coat,-.20F,.83F,-.08F,.40F,.24F,.29F);
                    box(pose,buffers,light,coat,-.15F,1.00F,-.24F,.30F,.20F,.24F);
                    for(float x:new float[]{-.19F,.11F})box(pose,buffers,light,coat,x,1.13F,-.19F,.09F,.16F,.06F);
                    for(float x:new float[]{-.10F,.07F})box(pose,buffers,light,Blocks.BLACK_CONCRETE,x,1.11F,-.245F,.035F,.045F,.015F);
                    box(pose,buffers,light,Blocks.PINK_WOOL,-.04F,1.04F,-.275F,.08F,.06F,.04F);
                }else{
                    box(pose,buffers,light,coat,-.25F,.86F,-.25F,.50F,.13F,.50F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.05F,.99F,-.05F,.10F,.11F,.10F);
                }
            }
            case ScenePropEntity.CARNIVAL_PEG->{
                Block glass=e.slot==1?Blocks.CYAN_STAINED_GLASS:e.slot==0?Blocks.ORANGE_STAINED_GLASS:Blocks.MAGENTA_STAINED_GLASS;
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.21F,0,-.21F,.42F,.07F,.42F);
                box(pose,buffers,light,glass,-.16F,.07F,-.16F,.32F,.47F,.32F);
                box(pose,buffers,light,Blocks.WHITE_STAINED_GLASS,-.13F,.16F,-.169F,.07F,.27F,.018F);
                box(pose,buffers,light,glass,-.085F,.54F,-.085F,.17F,.22F,.17F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.11F,.75F,-.11F,.22F,.065F,.22F);
            }
            case ScenePropEntity.CARNIVAL_FERRIS_WHEEL->{
                pose.mulPose(Axis.ZP.rotationDegrees(e.mode()>0?(e.modeAge()+partial)*1.031F:0));
                for(int i=0;i<24;i++){
                    pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(i*15F));
                    for(float z:new float[]{-.42F,.31F})
                        box(pose,buffers,light,i%2==0?Blocks.COPPER_BLOCK:Blocks.GOLD_BLOCK,-2.12F,15.85F,z,4.24F,.30F,.11F);
                    if(i%2==0){
                        box(pose,buffers,15728880,Blocks.SEA_LANTERN,-.15F,16.11F,-.48F,.30F,.32F,.94F);
                        box(pose,buffers,light,Blocks.ORANGE_CONCRETE,-.11F,0,-.26F,.22F,15.9F,.52F);
                    }
                    pose.popPose();
                }
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-1.1F,-1.1F,-.64F,2.2F,2.2F,1.28F);
                box(pose,buffers,15728880,Blocks.SEA_LANTERN,-.36F,-.36F,-.67F,.72F,.72F,.16F);
            }
            case ScenePropEntity.CARNIVAL_GONDOLA->{
                boolean rider=client.options.getCameraType().isFirstPerson()&&client.player!=null&&client.player.getVehicle()==e;
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-1.05F,-.10F,-.88F,2.10F,.15F,1.76F);
                box(pose,buffers,light,Blocks.CHERRY_PLANKS,-.93F,.05F,-.76F,1.86F,.10F,1.52F);
                for(float x:new float[]{-1.05F,.93F}){
                    box(pose,buffers,light,Blocks.PINK_TERRACOTTA,x,.10F,-.88F,.12F,1.10F,1.76F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,x,1.17F,-.88F,.12F,.10F,1.76F);
                }
                for(float z:new float[]{-.88F,.76F}){
                    box(pose,buffers,light,Blocks.PINK_TERRACOTTA,-.95F,.10F,z,1.90F,.70F,.12F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.95F,.78F,z,1.90F,.11F,.12F);
                }
                if(!rider){
                    for(float x:new float[]{-1.05F,.93F})box(pose,buffers,light,Blocks.GOLD_BLOCK,x,1.27F,-.88F,.12F,1.10F,1.76F);
                    box(pose,buffers,light,Blocks.YELLOW_WOOL,-1.10F,2.34F,-.94F,2.20F,.18F,1.88F);
                    box(pose,buffers,light,Blocks.SEA_LANTERN,-.19F,2.52F,-.15F,.38F,.12F,.30F);
                }
            }
            case ScenePropEntity.CARNIVAL_MOUNT->{
                boolean rider=client.options.getCameraType().isFirstPerson()&&client.player!=null&&client.player.getVehicle()==e;
                pose.mulPose(Axis.YP.rotationDegrees(180-e.getYRot()));
                int creature=Math.floorMod(e.slot-71,4);
                Block body=switch(creature){case 0->Blocks.WHITE_TERRACOTTA;case 1->Blocks.PINK_TERRACOTTA;
                    case 2->Blocks.CYAN_TERRACOTTA;default->Blocks.ORANGE_TERRACOTTA;};
                if(!rider)box(pose,buffers,light,Blocks.GOLD_BLOCK,-.035F,-.05F,-.035F,.07F,4.8F,.07F);
                box(pose,buffers,light,body,-.38F,.68F,-.63F,.76F,.63F,1.25F);
                if(!rider){
                    box(pose,buffers,light,body,-.28F,1.12F,-.83F,.56F,.57F,.52F);
                    box(pose,buffers,light,body,-.30F,1.40F,-.99F,.60F,.37F,.46F);
                    for(float x:new float[]{-.27F,.20F})box(pose,buffers,light,Blocks.BLACK_CONCRETE,x,1.58F,-1.0F,.07F,.09F,.035F);
                    for(float x:new float[]{-.29F,.16F})box(pose,buffers,light,body,x,.08F,-.42F,.13F,.68F,.15F);
                    for(float x:new float[]{-.29F,.16F})box(pose,buffers,light,body,x,.08F,.28F,.13F,.68F,.15F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.42F,1.12F,-.12F,.84F,.08F,.20F);
                    // Different ride animals need readable silhouettes, not only
                    // recolored copies of the same horse-shaped seat.
                    switch(creature){
                        case 0->{ // rabbit: tall ears and a cotton tail
                            for(float x:new float[]{-.23F,.13F}){
                                box(pose,buffers,light,body,x,1.72F,-.76F,.11F,.42F,.13F);
                                box(pose,buffers,light,Blocks.PINK_TERRACOTTA,x+.025F,1.79F,-.775F,.06F,.29F,.018F);
                            }
                            box(pose,buffers,light,Blocks.WHITE_WOOL,-.14F,.70F,.54F,.28F,.28F,.24F);
                        }
                        case 1->{ // pig: broad snout and short square ears
                            box(pose,buffers,light,Blocks.PINK_CONCRETE,-.26F,1.41F,-1.20F,.52F,.25F,.25F);
                            for(float x:new float[]{-.19F,.11F})box(pose,buffers,light,Blocks.BROWN_TERRACOTTA,x,1.48F,-1.205F,.07F,.07F,.012F);
                            for(float x:new float[]{-.28F,.16F})box(pose,buffers,light,body,x,1.70F,-.76F,.12F,.20F,.15F);
                        }
                        case 2->{ // moonlit mouse: round ears and a slender tail
                            for(float x:new float[]{-.40F,.23F})box(pose,buffers,light,body,x,1.53F,-.72F,.17F,.35F,.13F);
                            box(pose,buffers,light,Blocks.PINK_TERRACOTTA,-.09F,1.43F,-1.06F,.18F,.10F,.10F);
                            box(pose,buffers,light,Blocks.CYAN_TERRACOTTA,-.035F,.82F,.53F,.07F,.08F,.57F);
                        }
                        default->{ // fox: pointed ears, white muzzle and bushy tail
                            for(float x:new float[]{-.27F,.16F})box(pose,buffers,light,body,x,1.72F,-.79F,.11F,.28F,.12F);
                            box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,-.22F,1.38F,-1.13F,.44F,.18F,.18F);
                            box(pose,buffers,light,body,-.19F,.79F,.53F,.38F,.23F,.57F);
                            box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,-.17F,.81F,1.04F,.34F,.18F,.22F);
                        }
                    }
                }
            }
            case ScenePropEntity.CARNIVAL_FOOD->{
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.34F,0,-.34F,.68F,.07F,.68F);
                box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,-.28F,.07F,-.28F,.56F,.035F,.56F);
                if(!e.food(0).isEmpty()){
                    pose.pushPose();pose.translate(0,.30,0);pose.scale(.48F,.48F,.48F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(e.food(0),ItemDisplayContext.FIXED,15728880,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId());pose.popPose();
                }
                for(float x:new float[]{-.33F,.31F})box(pose,buffers,light,Blocks.WHITE_STAINED_GLASS,x,.08F,-.33F,.025F,.49F,.66F);
                box(pose,buffers,light,Blocks.WHITE_STAINED_GLASS,-.33F,.08F,-.33F,.66F,.49F,.025F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.34F,.57F,-.34F,.68F,.04F,.68F);
            }
            case ScenePropEntity.CARNIVAL_STRIKER_METER->{
                // Eye-level horizontal meter next to the strike plate.
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-1.2F,1.15F,-.1F,2.4F,.25F,.18F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,.85F,1.12F,-.13F,.35F,.31F,.23F);
                float phase=(e.modeAge()+partial)%40F;
                float position=e.mode()>0?2F*(1F-Math.abs(20F-phase)/20F)-1F:-1F;
                box(pose,buffers,15728880,Blocks.SEA_LANTERN,position,1.1F,-.27F,.18F,.35F,.15F);
            }
            case ScenePropEntity.CARNIVAL_BALLOON->{
                Block color=switch(Math.floorMod(e.slot,6)){case 0->Blocks.RED_CONCRETE;case 1->Blocks.YELLOW_CONCRETE;
                    case 2->Blocks.CYAN_CONCRETE;case 3->Blocks.LIME_CONCRETE;case 4->Blocks.MAGENTA_CONCRETE;default->Blocks.ORANGE_CONCRETE;};
                float bob=(float)Math.sin((e.tickCount+partial)*.10F+e.slot)*.055F;
                box(pose,buffers,light,Blocks.LIGHT_GRAY_CONCRETE,-.012F,.02F,-.012F,.024F,.38F,.024F);
                box(pose,buffers,light,color,-.06F,.38F+bob,-.06F,.12F,.09F,.12F);
                box(pose,buffers,light,color,-.22F,.46F+bob,-.20F,.44F,.15F,.40F);
                box(pose,buffers,light,color,-.29F,.61F+bob,-.25F,.58F,.31F,.50F);
                box(pose,buffers,light,color,-.23F,.92F+bob,-.21F,.46F,.15F,.42F);
                box(pose,buffers,15728880,Blocks.WHITE_STAINED_GLASS,-.18F,.78F+bob,-.254F,.11F,.13F,.02F);
            }
            case ScenePropEntity.CARNIVAL_DUCK->{
                float bob=(float)Math.sin((e.tickCount+partial)*.15F+e.slot)*.035F;
                pose.translate(0,bob,0);
                box(pose,buffers,light,Blocks.YELLOW_CONCRETE,-.27F,.15F,-.34F,.54F,.32F,.68F);
                box(pose,buffers,light,Blocks.YELLOW_CONCRETE,-.19F,.37F,-.32F,.38F,.28F,.33F);
                box(pose,buffers,light,Blocks.ORANGE_CONCRETE,-.14F,.37F,-.43F,.28F,.10F,.13F);
                for(float x:new float[]{-.18F,.12F})box(pose,buffers,light,Blocks.BLACK_CONCRETE,x,.51F,-.337F,.06F,.07F,.035F);
                box(pose,buffers,light,Blocks.YELLOW_CONCRETE,-.19F,.39F,.27F,.38F,.13F,.16F);
                for(float x:new float[]{-.33F,.26F})box(pose,buffers,light,Blocks.GOLD_BLOCK,x,.29F,-.1F,.07F,.11F,.23F);
            }
            case ScenePropEntity.CARNIVAL_WHEEL->{
                pose.mulPose(Axis.YP.rotationDegrees(180-e.getYRot()));
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-2.44F,-.14F,.17F,4.88F,4.88F,.22F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.11F,4.38F,-.52F,.22F,.51F,.17F);
                pose.pushPose();pose.translate(0,2.3F,.02F);
                float u=Math.min(1F,(e.modeAge()+partial)/65F);
                float ease=1F-(1F-u)*(1F-u)*(1F-u);
                float angle=e.mode()==1?(1080F-e.cakeStyle()*45F)*ease:e.mode()==2?-e.cakeStyle()*45F:0;
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                Block[] wedge={Blocks.RED_CONCRETE,Blocks.YELLOW_CONCRETE,Blocks.CYAN_CONCRETE,Blocks.LIME_CONCRETE,
                    Blocks.MAGENTA_CONCRETE,Blocks.ORANGE_CONCRETE,Blocks.BLUE_CONCRETE,Blocks.PINK_CONCRETE};
                Item[] prizes={Items.BREAD,Items.EMERALD,Items.DIAMOND,Items.GOLDEN_APPLE,
                    Items.AMETHYST_SHARD,Items.ENCHANTED_BOOK,Items.GOLD_INGOT,Items.NETHERITE_SCRAP};
                for(int i=0;i<8;i++){
                    pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(i*45F));
                    box(pose,buffers,light,wedge[i],-.35F,.90F,-.08F,.70F,1.15F,.12F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.035F,.23F,-.10F,.07F,1.91F,.15F);
                    box(pose,buffers,15728880,Blocks.SEA_LANTERN,-.08F,1.95F,-.13F,.16F,.16F,.18F);
                    pose.pushPose();pose.translate(0,1.14,-.18);pose.scale(.7F,.7F,.7F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(prizes[i]),ItemDisplayContext.FIXED,15728880,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId()+i);
                    pose.popPose();
                    pose.popPose();
                }
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.36F,-.36F,-.19F,.72F,.72F,.30F);
                pose.popPose();
            }
            case ScenePropEntity.CAKE_KITCHEN->{
                int id=e.mode();boolean recipe=id>=100&&id<107;
                // Both side counters present their fronts toward the baker in
                // the center aisle, never along the row toward the next booth.
                pose.mulPose(Axis.YP.rotationDegrees(id<12?-90:90));
                if(id<12)pose.scale(.72F,.85F,.72F);
                if(recipe){
                    box(pose,buffers,light,Blocks.CHERRY_PLANKS,-.30F,0,-.06F,.60F,.58F,.12F);
                    box(pose,buffers,light,Blocks.WHITE_CONCRETE,-.25F,.07F,-.075F,.50F,.45F,.025F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.28F,.54F,-.085F,.56F,.035F,.16F);
                }else{
                    // Open pantry trays and wall-backed ingredient plaques
                    // replace glass cubes with gold caps (the old floating jars).
                    boolean upper=false;
                    if(upper){
                        box(pose,buffers,light,Blocks.CHERRY_PLANKS,-.25F,0,-.06F,.50F,.46F,.10F);
                        box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,-.22F,.04F,-.075F,.44F,.37F,.025F);
                    }else{
                        box(pose,buffers,light,Blocks.CHERRY_PLANKS,-.29F,0,-.25F,.58F,.08F,.50F);
                        box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,-.25F,.07F,-.21F,.50F,.025F,.42F);
                        for(float x:new float[]{-.27F,.23F})box(pose,buffers,light,Blocks.CHERRY_PLANKS,x,.09F,-.21F,.04F,.10F,.42F);
                    }
                }
                if(!e.food(0).isEmpty()){
                    pose.pushPose();pose.translate(0,recipe?.30:.23,-.18);
                    pose.scale(recipe?.30F:.24F,recipe?.30F:.24F,recipe?.30F:.24F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(e.food(0),ItemDisplayContext.FIXED,15728880,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId());pose.popPose();
                }
            }
            case ScenePropEntity.GARDEN_MANTIS->{
                // Minecraft yaw 180 faces north; the model's forward axis is -Z.
                pose.mulPose(Axis.YP.rotationDegrees(180-e.getYRot()));
                if(mantisCockpit){
                    // The rider sees a slim skiff rim and peripheral controls.
                    // The sculpted thorax, pilot rig and rotor are intentionally
                    // hidden only in first person so the garden owns the center.
                    box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-1.25F,.75F,-1.18F,2.50F,.10F,.17F);
                    for(float x:new float[]{-1.28F,1.20F}){
                        box(pose,buffers,light,Blocks.GOLD_BLOCK,x,.82F,-1.16F,.08F,.55F,1.45F);
                        box(pose,buffers,light,Blocks.COPPER_BLOCK,x,1.38F,-.34F,.08F,.23F,.08F);
                    }
                    box(pose,buffers,15728880,Blocks.SEA_LANTERN,-.10F,.83F,-1.19F,.20F,.08F,.08F);
                }else{
                float flutter=(float)Math.sin((e.tickCount+partial)*.32)*9F;
                MantisMesh.render(pose,buffers,light);
                // Sculpted body, face, limbs and button eyes come from the Blender mesh.
                // The open skiff and animated wings remain separate, camera-safe pieces.
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-1.18F,.72F,-.63F,2.36F,.13F,1.55F);
                for(int side:new int[]{-1,1}){
                    float x=side<0?-1.16F:1.03F;
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,x,.78F,-.65F,.13F,1.18F,1.39F);
                    box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,x,.76F,-.65F,.15F,.13F,1.43F);
                    pose.pushPose();pose.translate(side*1.1,1.23F,.18F);pose.mulPose(Axis.ZP.rotationDegrees(side*flutter));
                    box(pose,buffers,light,Blocks.CYAN_STAINED_GLASS,side<0?-2.55F:0,0,-.65F,2.55F,.045F,1.30F);
                    for(float rib=.35F;rib<2.55F;rib+=.55F)box(pose,buffers,light,Blocks.GOLD_BLOCK,side<0?-rib:rib,0,-.68F,.045F,.07F,1.36F);
                    pose.popPose();
                }
                // A readable dashboard stays below the eye line in first person.
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-1.01F,1.72F,-.77F,2.02F,.12F,.20F);
                for(float x:new float[]{-.80F,.74F}){
                    box(pose,buffers,light,Blocks.COPPER_BLOCK,x,1.83F,-.68F,.07F,.38F,.07F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,x-.04F,2.18F,-.72F,.15F,.12F,.15F);
                }
                box(pose,buffers,15728880,Blocks.SEA_LANTERN,-.08F,1.85F,-.78F,.16F,.10F,.08F);
                // Rotor blades near eye height are suppressed only for the rider;
                // the head, rails, instruments, legs and wings remain visible.
                if(!mantisCockpit){
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.055F,1.21F,.52F,.11F,2.12F,.11F);
                    pose.pushPose();pose.translate(0,3.35F,.58F);pose.mulPose(Axis.YP.rotationDegrees((e.tickCount+partial)*22));
                    box(pose,buffers,light,Blocks.CUT_COPPER,-2.25F,-.025F,-.09F,4.5F,.05F,.18F);
                    box(pose,buffers,light,Blocks.CUT_COPPER,-.09F,-.025F,-2.25F,.18F,.05F,4.5F);pose.popPose();
                }
                }
            }
            case ScenePropEntity.GARDEN_PLANT->{
                Block petal=Blocks.PINK_TERRACOTTA;
                box(pose,buffers,light,Blocks.MOSS_BLOCK,-.36F,0,-.36F,.72F,.16F,.72F);
                box(pose,buffers,light,Blocks.GREEN_TERRACOTTA,-.06F,.13F,-.06F,.12F,.56F,.12F);
                for(int i=0;i<5;i++){
                    pose.pushPose();pose.translate(0,.59F,0);pose.mulPose(Axis.YP.rotationDegrees(i*72F+(e.tickCount+partial)*.35F));
                    box(pose,buffers,light,petal,-.10F,-.07F,-.38F,.20F,.13F,.33F);pose.popPose();
                }
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.13F,.54F,-.13F,.26F,.25F,.26F);
            }
            case ScenePropEntity.MAGIC_CURTAIN->{
                float openness=e.mode()==2?1:e.mode()==1?Math.min(1,(e.modeAge()+partial)/16F):e.mode()==3?Math.max(0,1-(e.modeAge()+partial)/16F):0;
                openness=openness*openness*(3-2*openness);
                float width=Math.max(.16F,9F*(1-openness));
                for(int side:new int[]{-1,1}){
                    float x=side<0?-9F:9F-width;
                    curtainBox(pose,buffers,"purple_wool",x,0,-.16F,width,7.7F,.32F);
                    for(float fold=.8F;fold<width-.24F;fold+=1.4F)curtainBox(pose,buffers,"red_wool",x+fold,0,-.2F,.24F,7.7F,.37F);
                    curtainBox(pose,buffers,"gold_block",x,6.45F,-.25F,width,.13F,.43F);
                    curtainBox(pose,buffers,"magenta_wool",x,7.7F,-.2F,width,.75F,.4F);
                    curtainBox(pose,buffers,"gold_block",x,8.35F,-.24F,width,.13F,.48F);
                }
            }
            case ScenePropEntity.MAGIC_SPOT,ScenePropEntity.MAGIC_PRIZE,ScenePropEntity.MAGIC_FINISH->{
                boolean prize=e.kind()!=ScenePropEntity.MAGIC_SPOT;
                box(pose,buffers,light,prize?Blocks.POLISHED_BLACKSTONE:Blocks.PURPUR_BLOCK,-.48F,0,-.48F,.96F,.43F,.96F);
                box(pose,buffers,light,e.mode()==1?Blocks.LIME_CONCRETE:Blocks.GOLD_BLOCK,-.5F,.43F,-.5F,1F,.08F,1F);
                for(float x:new float[]{-.48F,.42F})for(float z:new float[]{-.48F,.42F})
                    box(pose,buffers,light,Blocks.SEA_LANTERN,x,.48F,z,.06F,.08F,.06F);
                var item=e.food(0);
                if(!item.isEmpty()){
                    pose.pushPose();pose.translate(0,.98+Math.sin((e.tickCount+partial)*.07)*.035,0);
                    pose.mulPose(Axis.YP.rotationDegrees((e.tickCount+partial)*1.5F));
                    pose.scale(prize?.78F:1.05F,prize?.78F:1.05F,prize?.78F:1.05F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(item,ItemDisplayContext.FIXED,15728880,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId());pose.popPose();
                }
            }
            case ScenePropEntity.RESCUE_DOOR->{
                pose.translate(-.5,0,-.5);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.PORTAL,true),pose,buffers,15728880,OverlayTexture.NO_OVERLAY);
            }
            case ScenePropEntity.ACTIVITY_PORTAL,ScenePropEntity.ACTIVITY_EXIT->{
                ActivityPortalVisual.render(e.tickCount+partial,pose,buffers);
            }
            case ScenePropEntity.TARGET_STATIC,ScenePropEntity.TARGET_SLIDER,ScenePropEntity.TARGET_BOBBER,
                 ScenePropEntity.TARGET_POPUP,ScenePropEntity.TARGET_SPINNER,ScenePropEntity.TARGET_GOLD->target(e,partial,pose,buffers,light);
            case ScenePropEntity.ARCHERY_PLATFORM->{
                box(pose,buffers,light,Blocks.PURPUR_BLOCK,-1.5F,-.15F,-1.5F,3F,.3F,3F);
                for(float x:new float[]{-1.5F,1.35F})box(pose,buffers,light,Blocks.GOLD_BLOCK,x,.16F,-1.5F,.15F,.08F,3F);
                for(float z:new float[]{-1.5F,1.35F})box(pose,buffers,light,Blocks.GOLD_BLOCK,-1.5F,.16F,z,3F,.08F,.15F);
            }
            case ScenePropEntity.CAKE_MIXER->{
                box(pose,buffers,light,Blocks.IRON_BLOCK,-.34F,0,-.34F,.68F,.09F,.68F);
                box(pose,buffers,light,Blocks.PINK_TERRACOTTA,-.29F,.08F,-.29F,.58F,.2F,.58F);
                box(pose,buffers,light,Blocks.YELLOW_TERRACOTTA,-.21F,.28F,-.21F,.42F,.025F,.42F);
                pose.pushPose();
                if(e.mode()>0)pose.mulPose(Axis.YP.rotationDegrees((e.tickCount+partial)*42));
                box(pose,buffers,light,Blocks.IRON_BLOCK,-.025F,.27F,-.025F,.05F,.3F,.05F);
                box(pose,buffers,light,Blocks.IRON_BLOCK,-.11F,.27F,-.08F,.22F,.04F,.16F);
                box(pose,buffers,light,Blocks.CHERRY_PLANKS,-.03F,.52F,-.03F,.06F,.17F,.06F);
                pose.popPose();
            }
            case ScenePropEntity.CAKE_WORKPIECE->cakeWorkpiece(e,pose,buffers,light);
            case ScenePropEntity.GIFT->{
                // Hollow lined box: the rewards sit inside its walls, never on supper plates.
                box(pose,buffers,light,Blocks.RED_WOOL,-.55F,0,-.55F,1.1F,.08F,1.1F);
                for(float x:new float[]{-.55F,.47F})box(pose,buffers,light,Blocks.RED_WOOL,x,0,-.55F,.08F,.45F,1.1F);
                for(float z:new float[]{-.55F,.47F}){
                    box(pose,buffers,light,Blocks.RED_WOOL,-.55F,0,z,1.1F,.45F,.08F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.06F,0,z-.005F,.12F,.45F,.09F);
                }
                box(pose,buffers,light,Blocks.YELLOW_TERRACOTTA,-.47F,.08F,-.47F,.94F,.015F,.94F);
                if(e.mode()>0){
                    var items=e.treasure();
                    for(int i=0;i<items.size();i++)if(!items.get(i).isEmpty())for(int layer=0;layer<Math.min(3,items.get(i).getCount());layer++){
                        pose.pushPose();pose.translate((i%3-1)*.28,.23+(i/9)*.16+layer*.025,((i/3)%3-1)*.28);
                        pose.mulPose(Axis.XP.rotationDegrees(75));pose.mulPose(Axis.ZP.rotationDegrees((i%3-1)*14));pose.scale(.29F,.29F,.29F);
                        Minecraft.getInstance().getItemRenderer().renderStatic(items.get(i),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId()+i);pose.popPose();
                    }
                }
                pose.pushPose();pose.translate(0,.45,.55);if(e.mode()>0)pose.mulPose(Axis.XP.rotationDegrees(110));
                box(pose,buffers,light,Blocks.RED_TERRACOTTA,-.58F,0,-1.16F,1.16F,.09F,1.16F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.06F,.09F,-1.16F,.12F,.025F,1.16F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.58F,.09F,-.64F,1.16F,.025F,.12F);pose.popPose();
            }
            case ScenePropEntity.PIANO->piano(e,partial,pose,buffers,light);
            case ScenePropEntity.TABLE->{
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-1.8F,.7F,-1.3F,3.6F,.16F,2.6F);
                box(pose,buffers,light,Blocks.SMOOTH_SANDSTONE,-1.77F,.86F,-1.27F,3.54F,.02F,2.54F);
                for(float x:new float[]{-1.77F,1.74F})box(pose,buffers,light,Blocks.SMOOTH_SANDSTONE,x,.55F,-1.27F,.03F,.32F,2.54F);
                box(pose,buffers,light,Blocks.GREEN_TERRACOTTA,-.27F,.884F,-1.22F,.54F,.008F,2.44F);
                for(float x:new float[]{-1.65F,1.45F})for(float z:new float[]{-1.15F,.95F})box(pose,buffers,light,Blocks.DARK_OAK_LOG,x,0,z,.18F,.72F,.18F);
                for(float z:new float[]{-1.2F,1.17F})box(pose,buffers,light,Blocks.GOLD_BLOCK,-1.7F,.883F,z,3.4F,.01F,.025F);
                for(int setting=0;setting<3;setting++){
                    float x=setting==0?0:setting==1?-.85F:.85F,z=setting==0?-.94F:.94F;
                    box(pose,buffers,light,Blocks.IRON_BLOCK,x-.33F,.895F,z-.16F,.025F,.018F,.32F);
                    box(pose,buffers,light,Blocks.IRON_BLOCK,x+.3F,.895F,z-.16F,.025F,.018F,.32F);
                    box(pose,buffers,light,Blocks.WHITE_WOOL,x-.21F,.925F,z-.14F,.15F,.015F,.26F);
                }
                candles(e,partial,pose,buffers,light,-.64F);candles(e,partial,pose,buffers,light,.64F);
            }
            case ScenePropEntity.CHAIR->chair(e,pose,buffers,light);
            case ScenePropEntity.PLATE->{
                int rarity=e.cakeStyle();
                if(e.mode()==1){dish(pose,buffers,light,.38F);
                    box(pose,buffers,light,Blocks.IRON_BLOCK,-.31F,.07F,-.31F,.62F,.16F,.62F);
                    box(pose,buffers,light,Blocks.IRON_BLOCK,-.22F,.23F,-.22F,.44F,.09F,.44F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,-.04F,.32F,-.04F,.08F,.07F,.08F);break;}
                if(rarity>=3){box(pose,buffers,light,rarity==4?Blocks.GOLD_BLOCK:Blocks.IRON_BLOCK,-.39F,-.04F,-.39F,.78F,.05F,.78F);
                    for(float x:new float[]{-.34F,.3F})for(float z:new float[]{-.34F,.3F})
                        box(pose,buffers,light,rarity==4?Blocks.SEA_LANTERN:Blocks.AMETHYST_BLOCK,x,.01F,z,.04F,.06F,.04F);}
                dish(pose,buffers,light,rarity>=3?.38F:.3F);food(e,pose,buffers,light,false);
            }
            case ScenePropEntity.PRIZE_PLATE->{
                dish(pose,buffers,light,.43F);
                if(!e.food(0).isEmpty()){
                    pose.pushPose();pose.translate(0,.26,0);pose.mulPose(Axis.YP.rotationDegrees((e.tickCount+partial)*1.8F));
                    pose.scale(.68F,.68F,.68F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(e.food(0),ItemDisplayContext.FIXED,
                        15728880,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId());pose.popPose();
                }
            }
            case ScenePropEntity.FEAST_CART->{if(e.mode()!=0){
                box(pose,buffers,light,Blocks.CHERRY_PLANKS,-.75F,.25F,-.5F,1.5F,.17F,1F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,-.7F,.42F,-.45F,1.4F,.04F,.9F);
                for(float x:new float[]{-.62F,.46F})for(float z:new float[]{-.38F,.24F})
                    box(pose,buffers,light,Blocks.BLACK_CONCRETE,x,0,z,.16F,.3F,.16F);
                for(float x:new float[]{-.55F,0,.45F}){
                    box(pose,buffers,light,Blocks.IRON_BLOCK,x-.16F,.46F,-.17F,.32F,.10F,.34F);
                    box(pose,buffers,light,Blocks.GOLD_BLOCK,x-.035F,.56F,-.035F,.07F,.08F,.07F);
                }
                for(float x:new float[]{-.7F,.65F})box(pose,buffers,light,Blocks.CHERRY_FENCE,x,.42F,.42F,.05F,.65F,.05F);
            }}
            case ScenePropEntity.GLASS->{if(e.food(0).isEmpty()){box(pose,buffers,light,Blocks.GLASS,-.12F,0,-.12F,.24F,.35F,.24F);}else food(e,pose,buffers,light,true);}
            case ScenePropEntity.CAKE->{pose.translate(0,0,Math.max(0,1-(e.tickCount+partial)/60F));pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate(-.5,0,-.5);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(com.coraline.registry.ModBlocks.WELCOME_CAKE.get().defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);}
            case ScenePropEntity.CONTEST_CAKE->contestCake(e,pose,buffers,light);
        }
        pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
    }
    private static void contestCake(ScenePropEntity e,PoseStack pose,MultiBufferSource buffers,int light){
        if(e.cakeData().contains("Base")){CakeMesh.render(e.cakeData(),pose,buffers,light);return;}
        int style=e.cakeStyle(),flavor=style&3,icing=(style>>>2)&3,topper=(style>>>4)&3;
        Block sponge=flavor==1?Blocks.BROWN_TERRACOTTA:flavor==2?Blocks.PINK_TERRACOTTA:Blocks.YELLOW_TERRACOTTA;
        Block cream=icing==1?Blocks.LIGHT_BLUE_CONCRETE:icing==2?Blocks.MAGENTA_CONCRETE:Blocks.WHITE_CONCRETE;
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-.48F,0,-.48F,.96F,.045F,.96F);
        box(pose,buffers,light,sponge,-.40F,.045F,-.40F,.80F,.23F,.80F);
        box(pose,buffers,light,cream,-.43F,.27F,-.43F,.86F,.10F,.86F);
        box(pose,buffers,light,sponge,-.32F,.37F,-.32F,.64F,.20F,.64F);
        box(pose,buffers,light,cream,-.35F,.56F,-.35F,.70F,.10F,.70F);
        for(float x:new float[]{-.29F,.21F})for(float z:new float[]{-.29F,.21F}){
            box(pose,buffers,light,cream,x,.66F,z,.08F,.10F,.08F);
            box(pose,buffers,light,topper==1?Blocks.GOLD_BLOCK:topper==2?Blocks.PINK_WOOL:Blocks.RED_WOOL,x+.01F,.77F,z+.01F,.06F,.07F,.06F);
        }
        if(topper==2)box(pose,buffers,light,Blocks.FLOWERING_AZALEA_LEAVES,-.08F,.66F,-.08F,.16F,.20F,.16F);
        else if(topper==1)box(pose,buffers,light,Blocks.GOLD_BLOCK,-.10F,.67F,-.10F,.20F,.12F,.20F);
        else box(pose,buffers,light,Blocks.RED_WOOL,-.08F,.66F,-.08F,.16F,.16F,.16F);
    }
    private static Block sponge(int index){return switch(index){
        case 1->Blocks.BROWN_TERRACOTTA;case 2->Blocks.PINK_TERRACOTTA;case 3->Blocks.RED_TERRACOTTA;
        case 4->Blocks.ORANGE_TERRACOTTA;case 5->Blocks.YELLOW_TERRACOTTA;case 6->Blocks.WHITE_TERRACOTTA;
        default->Blocks.SMOOTH_SANDSTONE;};}
    private static Block filling(int index){return switch(index){
        case 1->Blocks.BROWN_CONCRETE_POWDER;case 2->Blocks.PINK_CONCRETE_POWDER;case 3->Blocks.MAGENTA_CONCRETE_POWDER;
        case 4->Blocks.HONEYCOMB_BLOCK;case 5->Blocks.YELLOW_CONCRETE_POWDER;default->Blocks.WHITE_CONCRETE_POWDER;};}
    private static Block frosting(int index){return switch(index){
        case 1->Blocks.BROWN_GLAZED_TERRACOTTA;case 2->Blocks.PINK_GLAZED_TERRACOTTA;case 3->Blocks.BLUE_GLAZED_TERRACOTTA;
        case 4->Blocks.ORANGE_GLAZED_TERRACOTTA;case 5->Blocks.CALCITE;case 6->Blocks.BLACK_GLAZED_TERRACOTTA;
        default->Blocks.QUARTZ_BLOCK;};}
    /** Modular, block-atlas cake: visible sponge, filling, frosting, drizzle and toppings. */
    private static void modularCake(net.minecraft.nbt.CompoundTag d,PoseStack pose,MultiBufferSource buffers,int light){
        int base=d.getInt("Base"),layers=Math.max(1,Math.min(3,d.getInt("Layers"))),fill=d.getInt("Filling");
        int frost=d.getInt("Frosting"),piping=d.getInt("Piping"),topper=d.getInt("Topper"),toppings=d.getInt("Toppings");
        Block sponge=sponge(base),cream=frosting(frost),jam=filling(fill),edge=frost==1||frost==6?Blocks.BLACK_TERRACOTTA:cream;
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-.49F,0,-.49F,.98F,.045F,.98F);
        float y=.05F,layerH=layers==1?.40F:layers==2?.25F:.19F;
        for(int layer=0;layer<layers;layer++){
            // Offset four corners to soften the silhouette without a giant cube.
            box(pose,buffers,light,sponge,-.40F,y,-.34F,.8F,layerH,.68F);
            box(pose,buffers,light,sponge,-.34F,y,-.40F,.68F,layerH,.8F);
            if(base==6)for(float x:new float[]{-.25F,.16F})for(float z:new float[]{-.29F,.22F})
                box(pose,buffers,light,(layer+x>0?Blocks.RED_CONCRETE:Blocks.BLUE_CONCRETE),x,y+.10F,z,.07F,.06F,.07F);
            if(base==3)for(float x:new float[]{-.3F,.27F})box(pose,buffers,light,Blocks.RED_CONCRETE,x,y+.04F,-.405F,.035F,.07F,.05F);
            y+=layerH;
            if(layer<layers-1){
                box(pose,buffers,light,jam,-.43F,y,-.36F,.86F,.065F,.72F);
                box(pose,buffers,light,jam,-.36F,y,-.43F,.72F,.065F,.86F);
                if(fill==2||fill==3)for(float x:new float[]{-.30F,0,.3F})
                    box(pose,buffers,light,fill==2?Blocks.RED_WOOL:Blocks.PURPLE_WOOL,x,y+.015F,-.445F,.075F,.045F,.07F);
                y+=.065F;
            }
        }
        // Thick top coat, visible side ribbons and larger readable drip shapes.
        box(pose,buffers,light,cream,-.45F,y,-.37F,.9F,.095F,.74F);
        box(pose,buffers,light,cream,-.37F,y,-.45F,.74F,.095F,.9F);
        for(float x:new float[]{-.38F,.34F}){
            box(pose,buffers,light,edge,x,y-.13F,-.31F,.055F,.18F,.62F);
            if(piping==3||frost==1||frost==6)for(float z:new float[]{-.25F,.12F})
                box(pose,buffers,light,edge,x,y-.23F,z,.07F,.28F,.10F);
        }
        for(float z:new float[]{-.38F,.34F})box(pose,buffers,light,edge,-.31F,y-.12F,z,.62F,.17F,.055F);
        y+=.095F;
        if(piping==1||piping==2||piping==4)for(float x:new float[]{-.31F,0,.31F})for(float z:new float[]{-.31F,.31F})
            box(pose,buffers,light,piping==4?Blocks.PINK_WOOL:Blocks.WHITE_WOOL,x-.05F,y,z-.05F,.10F,.10F,.10F);
        if(piping==2)box(pose,buffers,light,cream,-.16F,y,-.16F,.32F,.08F,.32F);
        if((toppings&(1<<6))!=0||(toppings&(1<<7))!=0){
            Block drizzle=(toppings&(1<<7))!=0?Blocks.BROWN_CONCRETE:Blocks.HONEY_BLOCK;
            for(float x:new float[]{-.26F,.04F,.28F})box(pose,buffers,light,drizzle,x,y,-.33F,.055F,.025F,.66F);
            y+=.025F;
        }
        int count=0;for(int type=0;type<10;type++)if((toppings&(1<<type))!=0){
            Block garnish=switch(type){case 0,2->Blocks.RED_WOOL;case 1->Blocks.BLUE_WOOL;case 3,4,7->Blocks.BROWN_WOOL;
                case 5,9->Blocks.MAGENTA_WOOL;case 6->Blocks.HONEY_BLOCK;default->Blocks.WHITE_WOOL;};
            float angle=(float)(count*2.1+.4);float x=(float)Math.sin(angle)*.23F,z=(float)Math.cos(angle)*.23F;
            for(int piece=0;piece<3;piece++){float px=x+(piece-1)*.12F,pz=z+(piece%2)*.08F;
                box(pose,buffers,light,garnish,px-.065F,y,pz-.065F,.13F,type==4?.07F:.12F,.13F);}
            count++;
        }
        if(topper>=0){Block material=switch(topper){case 0->Blocks.END_STONE;case 1->Blocks.GOLD_BLOCK;case 2->Blocks.FLOWERING_AZALEA_LEAVES;
            case 3->Blocks.PINK_WOOL;case 4->Blocks.WHITE_CANDLE;case 5->Blocks.BLACK_CONCRETE;default->Blocks.GOLD_BLOCK;};
            box(pose,buffers,light,material,-.10F,y+.07F,-.04F,.20F,.25F,.08F);
            if(topper==0)box(pose,buffers,light,Blocks.PURPLE_WOOL,-.025F,y+.14F,-.06F,.13F,.15F,.1F);
            if(topper==1){box(pose,buffers,light,material,-.18F,y+.16F,-.04F,.36F,.08F,.08F);
                box(pose,buffers,light,material,-.04F,y+.04F,-.04F,.08F,.38F,.08F);}
        }
    }
    private static void cakeWorkpiece(ScenePropEntity e,PoseStack pose,MultiBufferSource buffers,int light){
        int mode=e.mode();if(mode==10){if(e.slot==2)CakeMesh.stand(pose,buffers,light);return;}
        var d=e.cakeData();Block batter=sponge(d.getInt("Base"));
        if(mode<=2){
            box(pose,buffers,light,Blocks.IRON_BLOCK,-.34F,0,-.34F,.68F,.09F,.68F);
            box(pose,buffers,light,Blocks.PINK_TERRACOTTA,-.31F,.08F,-.31F,.62F,.17F,.62F);
            box(pose,buffers,light,mode==0?Blocks.WHITE_CONCRETE_POWDER:batter,-.25F,.25F,-.25F,.50F,.05F,.50F);
            if(mode==0)for(int i=0;i<5;i++)box(pose,buffers,light,i%2==0?Blocks.YELLOW_WOOL:Blocks.BROWN_WOOL,
                -.22F+i*.08F,.30F,-.11F+(i%2)*.12F,.08F,.04F,.07F);
            if(mode==1)for(int i=0;i<4;i++)box(pose,buffers,light,Blocks.WHITE_WOOL,-.18F+i*.1F,.30F,-.2F+(i%2)*.22F,.07F,.04F,.07F);
        }else if(mode==11){CakeMesh.render(d,pose,buffers,light);}
        else {
            box(pose,buffers,light,Blocks.IRON_BLOCK,-.45F,0,-.45F,.90F,.055F,.90F);
            Block color=mode>=9?Blocks.BLACK_TERRACOTTA:mode>=8?Blocks.BROWN_TERRACOTTA:batter;
            float height=mode==3||mode==4?.15F:mode==5?.21F:.28F;
            box(pose,buffers,light,color,-.37F,.055F,-.37F,.74F,height,.74F);
            if(mode>=6&&mode<=7)box(pose,buffers,light,Blocks.YELLOW_TERRACOTTA,-.29F,.055F+height,-.29F,.58F,.025F,.58F);
        }
    }
    private static void dish(PoseStack pose,MultiBufferSource buffers,int light,float r){
        // Stepped porcelain rim, approximating an octagonal plate rather than a square tile.
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-r,0,-r*.65F,r*2,.025F,r*1.3F);
        for(float z:new float[]{-r,r*.65F})box(pose,buffers,light,Blocks.GOLD_BLOCK,-r*.65F,0,z,r*1.3F,.025F,r*.35F);
        box(pose,buffers,light,Blocks.SMOOTH_QUARTZ,-r+.025F,.026F,-r*.65F+.015F,r*2-.05F,.014F,r*1.3F-.03F);
        for(float z:new float[]{-r+.025F,r*.65F-.015F})box(pose,buffers,light,Blocks.SMOOTH_QUARTZ,-r*.65F+.015F,.026F,z,r*1.3F-.03F,.014F,r*.35F-.01F);
    }
    private static void target(ScenePropEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(e.kind()==ScenePropEntity.TARGET_POPUP&&e.mode()==0)return;
        pose.scale(e.targetScale(),e.targetScale(),e.targetScale());
        if(e.kind()==ScenePropEntity.TARGET_SPINNER)pose.mulPose(Axis.YP.rotationDegrees((e.tickCount+partial)*14));
        Block frame=e.kind()==ScenePropEntity.TARGET_GOLD?Blocks.GOLD_BLOCK:Blocks.DARK_OAK_PLANKS;
        Block middle=switch(e.kind()){
            case ScenePropEntity.TARGET_SLIDER->Blocks.PURPLE_CONCRETE;
            case ScenePropEntity.TARGET_BOBBER->Blocks.LIME_CONCRETE;
            case ScenePropEntity.TARGET_POPUP->Blocks.ORANGE_CONCRETE;
            case ScenePropEntity.TARGET_SPINNER->Blocks.LIGHT_BLUE_CONCRETE;
            default->Blocks.CYAN_CONCRETE;
        };
        box(pose,buffers,light,Blocks.STRIPPED_DARK_OAK_LOG,-.07F,-1.05F,-.08F,.14F,1.12F,.14F);
        box(pose,buffers,light,frame,-.58F,-.02F,-.10F,1.16F,1.16F,.10F);
        box(pose,buffers,light,Blocks.WHITE_CONCRETE,-.51F,.05F,.005F,1.02F,1.02F,.025F);
        box(pose,buffers,light,middle,-.39F,.17F,.032F,.78F,.78F,.025F);
        box(pose,buffers,light,Blocks.RED_CONCRETE,-.27F,.29F,.059F,.54F,.54F,.025F);
        box(pose,buffers,light,e.kind()==ScenePropEntity.TARGET_GOLD?Blocks.GOLD_BLOCK:Blocks.YELLOW_CONCRETE,-.12F,.44F,.086F,.24F,.24F,.027F);
    }
    private static void candles(ScenePropEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,float z){
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-.19F,.9F,z-.12F,.38F,.04F,.24F);
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-.025F,.94F,z-.025F,.05F,.22F,.05F);
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-.18F,1.1F,z-.025F,.36F,.035F,.05F);
        for(int i=-1;i<=1;i++){
            float x=i*.15F,height=i==0?.43F:.32F;
            box(pose,buffers,light,Blocks.SMOOTH_SANDSTONE,x-.025F,1.13F,z-.025F,.05F,height,.05F);
            float flicker=.028F+(float)Math.sin((e.tickCount+partial)*.2+i)*.005F;
            box(pose,buffers,LightTexture.FULL_BRIGHT,Blocks.YELLOW_CONCRETE,x-.018F,1.13F+height,z-.018F,.036F,flicker*2,.036F);
        }
    }
    private static void chair(ScenePropEntity e,PoseStack pose,MultiBufferSource buffers,int light){
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        var wood=Blocks.STRIPPED_MANGROVE_WOOD;
        box(pose,buffers,light,wood,-.38F,.66F,-.38F,.76F,.12F,.76F);
        box(pose,buffers,light,Blocks.RED_WOOL,-.32F,.78F,-.32F,.64F,.09F,.64F);
        box(pose,buffers,light,wood,-.38F,.78F,-.38F,.76F,1.05F,.1F);
        box(pose,buffers,light,Blocks.RED_WOOL,-.27F,.94F,-.27F,.54F,.72F,.025F);
        for(float x:new float[]{-.36F,.29F}){
            box(pose,buffers,light,Blocks.GOLD_BLOCK,x,.85F,-.28F,.065F,.94F,.035F);
            box(pose,buffers,light,wood,x,1.08F,-.25F,.08F,.09F,.62F);
            for(float z:new float[]{-.32F,.24F}){
                box(pose,buffers,light,wood,x,0,z,.09F,.7F,.09F);
                box(pose,buffers,light,Blocks.GOLD_BLOCK,x,.02F,z,.095F,.1F,.095F);
            }
        }
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-.4F,1.8F,-.39F,.8F,.07F,.13F);
        box(pose,buffers,light,wood,-.24F,1.87F,-.38F,.48F,.1F,.1F);
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-.07F,1.96F,-.39F,.14F,.1F,.12F);
    }
    private static void food(ScenePropEntity e,PoseStack pose,MultiBufferSource buffers,int light,boolean drink){
        for(int i=0;i<3;i++)if(!e.food(i).isEmpty()){
            pose.pushPose();pose.translate(drink?0:(i-1)*.15,drink?.19:.06+i*.008,drink?0:(i==1?.1:-.06));
            if(!drink)pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(drink?.5F:.28F,drink?.5F:.28F,drink?.5F:.28F);
            Minecraft.getInstance().getItemRenderer().renderStatic(e.food(i),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId()+i);pose.popPose();
        }
    }
    private static void piano(ScenePropEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()-(e.mode()==2?partial*2.5F:0)));
        var black=Blocks.BLACK_CONCRETE;
        var wood=Blocks.STRIPPED_MANGROVE_WOOD;
        box(pose,buffers,light,wood,-1.2F,.7F,-.2F,2.4F,.42F,1.45F);
        box(pose,buffers,light,wood,-1.0F,.74F,1.25F,1.7F,.3F,.6F);
        box(pose,buffers,light,Blocks.GOLD_BLOCK,-1.18F,1.11F,-.19F,2.36F,.025F,1.42F);
        for(int string=0;string<16;string++)box(pose,buffers,light,Blocks.IRON_BLOCK,-1.05F+string*.135F,1.137F,-.08F,.012F,.008F,1.15F);
        pose.pushPose();pose.translate(-1.2,1.14,-.2);pose.mulPose(Axis.ZP.rotationDegrees(18));box(pose,buffers,light,wood,0,0,0,2.4F,.075F,1.6F);pose.popPose();
        box(pose,buffers,light,wood,-1.22F,.75F,-.55F,2.44F,.13F,.37F);
        for(int i=0;i<22;i++){
            float x=-1.12F+i*.103F;float press=e.mode()>0?(float)Math.max(0,Math.sin((e.tickCount+partial)*.45+i*1.7))*.025F:0;
            box(pose,buffers,light,Blocks.QUARTZ_BLOCK,x,.89F-press,-.53F,.097F,.04F,.31F);
            if(i%7!=2&&i%7!=6)box(pose,buffers,light,black,x+.07F,.94F-press,-.35F,.045F,.055F,.14F);
        }
        for(float x:new float[]{-1.02F,.9F})box(pose,buffers,light,wood,x,0,.95F,.13F,.75F,.13F);
        box(pose,buffers,light,wood,-.1F,0,-.2F,.18F,.75F,.18F);
        box(pose,buffers,light,wood,-.42F,.34F,-1.15F,.84F,.13F,.48F);
        for(float x:new float[]{-.37F,.28F})box(pose,buffers,light,wood,x,0,-1.08F,.08F,.35F,.08F);
        if(e.mode()==2)for(int side:new int[]{-1,1}){
            float bounce=(float)Math.sin((e.tickCount+partial)*.5+side)*.07F+Math.max(0,1-(e.modeAge()+partial)/20F)*.7F;
            box(pose,buffers,light,Blocks.IRON_BLOCK,side*1.04F-.06F,.95F,0,.12F,.8F,.12F);
            box(pose,buffers,light,Blocks.IRON_BLOCK,side<0?-1.04F:.28F,1.65F+bounce,-.62F,.76F,.09F,.09F);
            box(pose,buffers,light,Blocks.IRON_BLOCK,side*.33F-.055F,1.06F+bounce,-.62F,.11F,.65F,.11F);
            for(int finger=0;finger<3;finger++)box(pose,buffers,light,Blocks.IRON_BLOCK,side*.33F-.07F+finger*.06F,1.02F+bounce,-.68F,.035F,.09F,.17F);
        }
    }
}
