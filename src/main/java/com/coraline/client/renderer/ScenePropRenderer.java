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
        if(e.kind()==ScenePropEntity.CHAIR&&e.visualSlot()>=10000)return;
        if(e.kind()==ScenePropEntity.DINNER_JUG){
            var b=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png")));
            FirstEscapePresentation.sphere(pose,b,light,0,.27,0,.24,.27,.24,0xCBA65E,1);
            FirstEscapePresentation.sphere(pose,b,light,0,.50,0,.13,.13,.13,0xF4DDB0,1);
            box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-.10F,.59F,-.10F,.20F,.035F,.20F);
            box(pose,buffers,light,Blocks.YELLOW_TERRACOTTA,-.09F,.47F,-.30F,.18F,.10F,.24F);
            for(float y:new float[]{.18F,.40F})box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,.18F,y,-.04F,.20F,.055F,.08F);
            box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,.33F,.18F,-.04F,.055F,.27F,.08F);
            return;
        }
        if(e.kind()==ScenePropEntity.PARTY_BALL){var b=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png")));FirstEscapePresentation.sphere(pose,b,light,0,.25,0,.25,.25,.25,0xDF3E56,1);return;}
        if(e.kind()==ScenePropEntity.PARTY_GOAL){for(float z:new float[]{-2,2})box(pose,buffers,light,Blocks.YELLOW_WOOL,-.06F,0,z-.06F,.12F,1.1F,.12F);box(pose,buffers,light,Blocks.WHITE_WOOL,-.06F,1.1F,-2,.12F,.12F,4);return;}
        if(PartyDecorationRenderer.render(e,partial,pose,buffers,light)){super.render(e,yaw,partial,pose,buffers,light);return;}
        var client=Minecraft.getInstance();
        if(e.kind()==ScenePropEntity.CARNIVAL_GONDOLA&&client.options.getCameraType().isFirstPerson()&&client.player!=null&&client.player.getVehicle()==e)return;
        boolean mantisCockpit=e.kind()==ScenePropEntity.GARDEN_MANTIS&&client.options.getCameraType().isFirstPerson()&&client.player!=null&&client.player.getVehicle()==e;
        pose.pushPose();
        switch(e.kind()){
            case ScenePropEntity.GAME_BOARD->tabletop(e,partial,pose,buffers,light);
            case ScenePropEntity.GAME_SHELF->{
                pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
                for(int i=0;i<4;i++){box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-.9F,i*.55F,-.3F,1.8F,.09F,.6F);for(int j=0;j<3;j++){box(pose,buffers,light,new Block[]{Blocks.BLUE_TERRACOTTA,Blocks.PINK_TERRACOTTA,Blocks.GREEN_TERRACOTTA}[j],-.8F+j*.55F,i*.55F+.1F,-.24F,.48F,.32F,.45F);box(pose,buffers,light,Blocks.GOLD_BLOCK,-.65F+j*.55F,i*.55F+.2F,-.255F,.18F,.03F,.03F);}}
                for(float x:new float[]{-.95F,.87F})box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,x,0,-.3F,.08F,2.1F,.6F);
            }
            case ScenePropEntity.OUTING_SCENERY->outingScenery(e,pose,buffers,light);
            case ScenePropEntity.PICNIC_BASKET->{
                box(pose,buffers,light,Blocks.BAMBOO_PLANKS,-.5F,0,-.32F,1,.07F,.64F);
                for(float z:new float[]{-.32F,.26F})box(pose,buffers,light,Blocks.BAMBOO_PLANKS,-.5F,.07F,z,1,.35F,.06F);
                for(float x:new float[]{-.5F,.44F})box(pose,buffers,light,Blocks.BAMBOO_PLANKS,x,.07F,-.26F,.06F,.35F,.52F);
                for(int i=0;i<7;i++)box(pose,buffers,light,Blocks.STRIPPED_OAK_WOOD,-.49F+i*.15F,.03F,-.335F,.045F,.34F,.67F);
                for(float x:new float[]{-.45F,.37F})box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,x,.42F,-.05F,.08F,.38F,.1F);
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-.45F,.76F,-.05F,.9F,.08F,.1F);
                pose.pushPose();pose.translate(0,.42,-.32);pose.mulPose(Axis.XP.rotationDegrees(e.mode()>0?-Math.min(95,e.modeAge()*4):0));box(pose,buffers,light,Blocks.BAMBOO_PLANKS,-.5F,0,0,1,.055F,.64F);pose.popPose();
                if(e.mode()>0){box(pose,buffers,light,Blocks.WHITE_WOOL,-.36F,.09F,-.2F,.72F,.03F,.4F);}
            }
            case ScenePropEntity.OUTING_TELESCOPE->{
                for(int i=0;i<3;i++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(i*120));pose.mulPose(Axis.ZP.rotationDegrees(22));box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-.05F,0,-.05F,.1F,1.05F,.1F);pose.popPose();}
                box(pose,buffers,light,Blocks.COPPER_BLOCK,-.15F,.92F,-.15F,.3F,.18F,.3F);pose.pushPose();pose.translate(0,1.1,0);pose.mulPose(Axis.XP.rotationDegrees(-35));box(pose,buffers,light,Blocks.COPPER_BLOCK,-.15F,-.15F,-.7F,.3F,.3F,1.4F);box(pose,buffers,light,Blocks.GOLD_BLOCK,-.19F,-.19F,.6F,.38F,.38F,.14F);box(pose,buffers,15728880,Blocks.LIGHT_BLUE_STAINED_GLASS,-.145F,-.145F,.742F,.29F,.29F,.025F);box(pose,buffers,light,Blocks.BLACK_CONCRETE,-.09F,-.09F,-.79F,.18F,.18F,.1F);pose.popPose();
            }
            case ScenePropEntity.OUTING_TACKLE->{box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-.5F,0,-.35F,1,.45F,.7F);box(pose,buffers,light,Blocks.IRON_BLOCK,-.52F,.42F,-.37F,1.04F,.05F,.74F);box(pose,buffers,light,Blocks.IRON_BLOCK,-.14F,.48F,-.025F,.28F,.15F,.05F);food(e,pose,buffers,light,false);}
            case ScenePropEntity.OUTING_LANDMARK->{switch(e.visualSlot()>=100?2:Math.floorMod(e.visualSlot(),4)){
                case 0->{box(pose,buffers,light,Blocks.MOSSY_STONE_BRICKS,-.4F,0,-.4F,.8F,.22F,.8F);box(pose,buffers,light,Blocks.CHISELED_STONE_BRICKS,-.2F,.22F,-.2F,.4F,.7F,.4F);box(pose,buffers,15728880,Blocks.AMETHYST_BLOCK,-.12F,.92F,-.12F,.24F,.25F,.24F);}
                case 1->{for(float x:new float[]{-.7F,.55F})box(pose,buffers,light,Blocks.SPRUCE_LOG,x,0,-.3F,.15F,.4F,.6F);box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-.8F,.4F,-.35F,1.6F,.12F,.7F);box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-.8F,.52F,.2F,1.6F,.5F,.1F);}
                case 2->{for(int i=0;i<(e.visualSlot()>=100&&e.mode()>0?0:5);i++)box(pose,buffers,light,i%2==0?Blocks.ALLIUM:Blocks.BLUE_ORCHID,-.5F+i*.25F,0,(i%2)*.2F,.5F,.65F,.5F);}
                default->{box(pose,buffers,light,Blocks.MOSS_BLOCK,-.55F,0,-.45F,1.1F,.08F,.9F);for(int i=0;i<3;i++){box(pose,buffers,light,Blocks.MUSHROOM_STEM,-.45F+i*.4F,.08F,0,.12F,.2F+i*.08F,.12F);box(pose,buffers,light,i%2==0?Blocks.RED_MUSHROOM_BLOCK:Blocks.BROWN_MUSHROOM_BLOCK,-.57F+i*.4F,.26F+i*.08F,-.1F,.35F,.12F,.35F);}}
            }}
            case ScenePropEntity.OUTING_BOBBER->{if(e.mode()==0){fishingLine(e,partial,pose,buffers);box(pose,buffers,light,Blocks.WHITE_CONCRETE,-.06F,0,-.06F,.12F,.10F,.12F);box(pose,buffers,light,Blocks.RED_CONCRETE,-.06F,.10F,-.06F,.12F,.07F,.12F);}}
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
                Block glass=e.visualSlot()==1?Blocks.CYAN_STAINED_GLASS:e.visualSlot()==0?Blocks.ORANGE_STAINED_GLASS:Blocks.MAGENTA_STAINED_GLASS;
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
                int creature=Math.floorMod(e.visualSlot()-71,4);
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
                Block color=switch(Math.floorMod(e.visualSlot(),6)){case 0->Blocks.RED_CONCRETE;case 1->Blocks.YELLOW_CONCRETE;
                    case 2->Blocks.CYAN_CONCRETE;case 3->Blocks.LIME_CONCRETE;case 4->Blocks.MAGENTA_CONCRETE;default->Blocks.ORANGE_CONCRETE;};
                float bob=(float)Math.sin((e.tickCount+partial)*.10F+e.visualSlot())*.055F;
                box(pose,buffers,light,Blocks.LIGHT_GRAY_CONCRETE,-.012F,.02F,-.012F,.024F,.38F,.024F);
                box(pose,buffers,light,color,-.06F,.38F+bob,-.06F,.12F,.09F,.12F);
                box(pose,buffers,light,color,-.22F,.46F+bob,-.20F,.44F,.15F,.40F);
                box(pose,buffers,light,color,-.29F,.61F+bob,-.25F,.58F,.31F,.50F);
                box(pose,buffers,light,color,-.23F,.92F+bob,-.21F,.46F,.15F,.42F);
                box(pose,buffers,15728880,Blocks.WHITE_STAINED_GLASS,-.18F,.78F+bob,-.254F,.11F,.13F,.02F);
            }
            case ScenePropEntity.CARNIVAL_DUCK->{
                float bob=(float)Math.sin((e.tickCount+partial)*.15F+e.visualSlot())*.035F;
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
                box(pose,buffers,light,e.mode()==1?sponge(e.cakeData().getInt("Base")):Blocks.YELLOW_TERRACOTTA,-.21F,.28F,-.21F,.42F,.025F,.42F);
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
                float depth=e.longDining()?4.5F*Math.max(1,e.targetScale()):1.3F;
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-1.8F,.7F,-depth,3.6F,.16F,depth*2);
                box(pose,buffers,light,Blocks.SMOOTH_SANDSTONE,-1.77F,.86F,-depth+.03F,3.54F,.02F,depth*2-.06F);
                for(float x:new float[]{-1.77F,1.74F})box(pose,buffers,light,Blocks.SMOOTH_SANDSTONE,x,.55F,-depth+.03F,.03F,.32F,depth*2-.06F);
                box(pose,buffers,light,Blocks.GREEN_TERRACOTTA,-.27F,.884F,-depth+.08F,.54F,.008F,depth*2-.16F);
                for(float x:new float[]{-1.65F,1.45F})for(float z:new float[]{-depth+.15F,depth-.35F})box(pose,buffers,light,Blocks.DARK_OAK_LOG,x,0,z,.18F,.72F,.18F);
                for(float z:new float[]{-depth+.1F,depth-.13F})box(pose,buffers,light,Blocks.GOLD_BLOCK,-1.7F,.883F,z,3.4F,.01F,.025F);
                for(int setting=0;setting<3;setting++){
                    float x=setting==0?0:setting==1?-.85F:.85F,z=setting==0?-(depth-.36F):depth-.36F;
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
        int mode=e.mode();
        if(e.visualSlot()==0)return;
        var design=e.cakeData();
        if(mode==com.coraline.feast.CakeKitchenRules.ASSEMBLED)CakeMesh.render(design,pose,buffers,light);
        else if(mode==com.coraline.feast.CakeKitchenRules.EMPTY){
            if(e.visualSlot()==2)CakeMesh.stand(pose,buffers,light);
        }else CakeMesh.pan(design,mode,pose,buffers,light);
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
    private static Block gameColor(int who){return new Block[]{Blocks.RED_TERRACOTTA,Blocks.BLUE_TERRACOTTA,Blocks.YELLOW_TERRACOTTA,Blocks.GREEN_TERRACOTTA}[Math.floorMod(who,4)];}
    private static void buttonPiece(PoseStack pose,MultiBufferSource buffers,int light,Block color,float x,float y,float z,float r){
        // A twelve-sided button puck with real holes, rather than a wool cube.
        for(int side=0;side<12;side++){pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.YP.rotationDegrees(side*30));box(pose,buffers,light,color,-r*.26F,0,-r,r*.52F,.025F,r);pose.popPose();}
        for(float dx:new float[]{-.025F,.017F})for(float dz:new float[]{-.025F,.017F})box(pose,buffers,light,Blocks.BLACK_CONCRETE,x+dx,y+.026F,z+dz,.01F,.002F,.01F);
    }
    private static void tabletop(ScenePropEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var n=com.coraline.client.WorldTabletop.visible(e,e.cakeData());if(!n.contains("Game"))return;var game=com.coraline.feast.TabletopGames.Game.valueOf(n.getString("Game"));
        com.coraline.client.WorldTabletop.render(e,pose,buffers,light);
        box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-.95F,0,-.8F,1.9F,.045F,1.6F);box(pose,buffers,light,Blocks.GOLD_BLOCK,-.95F,.045F,-.8F,1.9F,.01F,.02F);
        switch(game){
            case BUTTON_CARDS->{
                for(int i=0;i<6;i++)box(pose,buffers,light,Blocks.BLUE_TERRACOTTA,-.45F,.05F+i*.012F,-.15F,.32F,.01F,.45F);
                float deal=n.contains("LastActor")?Math.min(1,(n.getInt("TurnTicks")+partial)/20F):1;float angle=n.getInt("LastActor")*(float)Math.PI/2;
                float cx=.05F+(float)Math.sin(angle)*(1-deal)*.6F,cz=-.15F-(float)Math.cos(angle)*(1-deal)*.5F;
                box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,cx-.01F,.059F,cz-.01F,.34F,.02F,.47F);box(pose,buffers,light,gameColor(n.getInt("Color")),cx,.06F,cz,.32F,.02F,.45F);cardMark(pose,buffers,light,n.getInt("Top")%8,cx+.11F,.081F,cz+.15F,.032F);
                int[] hand=n.getIntArray("Hand0");for(int i=0;i<Math.min(8,hand.length);i++){float hx=-.75F+i*.19F;box(pose,buffers,light,gameColor(hand[i]/8),hx,.08F,-.67F,.16F,.014F,.26F);cardMark(pose,buffers,light,hand[i]%8,hx+.05F,.095F,-.59F,.019F);}
                for(int seat=1;seat<4;seat++)for(int i=0;i<Math.min(7,n.getInt("HandCount"+seat));i++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(seat*90));box(pose,buffers,light,Blocks.BLUE_TERRACOTTA,-.6F+i*.18F,.08F,.5F,.16F,.014F,.25F);box(pose,buffers,light,Blocks.GOLD_BLOCK,-.56F+i*.18F,.094F,.57F,.07F,.002F,.1F);pose.popPose();}
            }
            case THREAD_FOUR->{
                var board=n.getIntArray("Board");box(pose,buffers,light,Blocks.PURPUR_BLOCK,-.65F,.05F,.05F,1.3F,.85F,.08F);
                for(int row=0;row<6;row++)for(int col=0;col<7;col++){pose.pushPose();pose.translate(-.54F+col*.18F,.12F+row*.13F,.04F);pose.mulPose(Axis.XP.rotationDegrees(90));int team=board.length>row*7+col?board[row*7+col]:-1;float fall=col==n.getInt("LastColumn")&&row==n.getInt("LastRow")?Math.max(0,1-(n.getInt("TurnTicks")+partial)/16F)*.5F:0;pose.translate(0,0,fall);buttonPiece(pose,buffers,light,team<0?Blocks.BLACK_CONCRETE:gameColor(team==0?1:0),0,0,0,.055F);pose.popPose();}
                for(float x:new float[]{-.65F,.55F})box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,x,.05F,-.1F,.1F,.1F,.5F);
            }
            case WOBBLY_TOWER->{
                var rows=n.getIntArray("Tower");float stability=(float)Math.max(0,n.getDouble("Stability"));boolean collapse=n.getBoolean("Done")&&stability<=0;
                for(int row=0;row<rows.length;row++)for(int piece=0;piece<3;piece++)if((rows[row]&(1<<piece))!=0){pose.pushPose();pose.translate(0,.05F+row*.075F,0);float wobble=(float)Math.sin((e.tickCount+partial)*.18)*(1-stability)*3;pose.mulPose(Axis.ZP.rotationDegrees(wobble));pose.mulPose(Axis.YP.rotationDegrees(row%2*90));if(collapse){float t=Math.min(1,(n.getInt("TurnTicks")+partial)/30F);pose.translate((piece-1)*t*.6,-row*.07*t,t*.3);pose.mulPose(Axis.ZP.rotationDegrees(t*(piece-1)*80));}box(pose,buffers,light,Blocks.STRIPPED_OAK_WOOD,-.21F+piece*.14F,0,-.21F,.135F,.07F,.42F);pose.popPose();}
            }
            case MOUSEBOARD_DASH->{
                for(int i=0;i<=30;i++){float x=-.8F+i%8*.21F,z=-.65F+i/8*.36F;box(pose,buffers,light,i%7==3?Blocks.YELLOW_TERRACOTTA:i%9==5?Blocks.PURPLE_TERRACOTTA:Blocks.GREEN_TERRACOTTA,x,.05F,z,.18F,.012F,.27F);if(i==7||i==16){box(pose,buffers,light,Blocks.SPRUCE_PLANKS,x,.07F,z,.2F,.02F,.3F);}}
                var mice=n.getIntArray("Mouse");for(int who=0;who<Math.min(4,mice.length);who++){float progress=mice[who];if(who==n.getInt("LastActor")){float t=Math.min(1,(n.getInt("TurnTicks")+partial)/30F);progress=n.getInt("LastFrom")+(mice[who]-n.getInt("LastFrom"))*t;}int cell=Math.min(30,(int)progress);float x=-.73F+cell%8*.21F+who*.018F,z=-.55F+cell/8*.36F;box(pose,buffers,light,gameColor(who),x,.075F,z,.07F,.055F,.12F);box(pose,buffers,light,gameColor(who),x+.02F,.09F,z-.02F,.055F,.045F,.06F);for(float ear:new float[]{-.01F,.045F})box(pose,buffers,light,gameColor(who),x+ear,.13F,z,.025F,.025F,.025F);box(pose,buffers,light,Blocks.BLACK_CONCRETE,x+.055F,.12F,z-.026F,.013F,.014F,.014F);}
                box(pose,buffers,light,Blocks.QUARTZ_BLOCK,-.08F,.065F,-.08F,.16F,.16F,.16F);for(int pip=0;pip<Math.min(4,n.getInt("Die"));pip++)box(pose,buffers,light,Blocks.BLACK_CONCRETE,-.045F+(pip%2)*.07F,.226F,-.045F+(pip/2)*.07F,.025F,.001F,.025F);
            }
            case BUTTON_FLICK->{
                // Uniform .7 world scale matches both axes and every scoring radius in TabletopGames.
                box(pose,buffers,light,Blocks.STRIPPED_OAK_WOOD,-.75F,.045F,-.75F,1.5F,.025F,1.5F);
                for(float radius:new float[]{.6F,.38F,.2F,.08F})for(int i=0;i<40;i++){float a=i*(float)Math.PI/20;box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,(float)Math.cos(a)*radius*.7F-.01F,.073F,.245F+(float)Math.sin(a)*radius*.7F-.01F,.02F,.001F,.02F);}
                for(var tag:n.getList("Pucks",10)){var puck=(net.minecraft.nbt.CompoundTag)tag;buttonPiece(pose,buffers,light,gameColor(puck.getInt("Owner")),(float)puck.getDouble("X")*.7F,.075F,(float)puck.getDouble("Z")*.7F,.035F);}
                if(n.getInt("Physics")==0&&!n.getBoolean("Done"))buttonPiece(pose,buffers,light,gameColor(n.getInt("Turn")),0,.075F,-.595F,.035F);
                for(float edge:new float[]{-.79F,.75F}){box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,edge,.05F,-.79F,.04F,.09F,1.58F);box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-.79F,.05F,edge,1.58F,.09F,.04F);}
            }
            case SILENT_CHARADES->{
                box(pose,buffers,light,Blocks.WHITE_TERRACOTTA,-.42F,.055F,-.28F,.84F,.015F,.56F);for(int i=0;i<4;i++)box(pose,buffers,light,gameColor(i),-.32F+i*.18F,.072F,-.25F,.015F,.025F,.48F);
                box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,.5F,.06F,-.2F,.32F,.04F,.4F);box(pose,buffers,light,Blocks.BLUE_TERRACOTTA,.52F,.1F,-.18F,.28F,.01F,.36F);
            }
        }
    }
    private static void cardMark(PoseStack pose,MultiBufferSource buffers,int light,int symbol,float x,float y,float z,float pixel){
        String[] glyphs={"010110010010111","110001010100111","110001010001110","101101111001001","011100010001110","110101110101101","010111010000010","101101101111101"};String glyph=glyphs[Math.floorMod(symbol,8)];
        for(int row=0;row<5;row++)for(int col=0;col<3;col++)if(glyph.charAt(row*3+col)=='1')box(pose,buffers,light,Blocks.WHITE_CONCRETE,x+col*pixel,y,z+row*pixel,pixel*.8F,.002F,pixel*.8F);
    }
    private static void fishingLine(ScenePropEntity bobber,float partial,PoseStack pose,MultiBufferSource buffers){
        var data=bobber.cakeData();var entity=bobber.level().getEntity(data.getInt("AnglerId"));
        if(!(entity instanceof com.coraline.parents.ParentEntity parent)||!data.hasUUID("AnglerUUID")||!parent.getUUID().equals(data.getUUID("AnglerUUID"))||!parent.getMainHandItem().is(Items.FISHING_ROD)||parent.distanceToSqr(bobber)>1600)return;
        double yaw=Math.toRadians(parent.yBodyRot),side=parent.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
        var start=new net.minecraft.world.phys.Vec3(net.minecraft.util.Mth.lerp(partial,bobber.xo,bobber.getX()),net.minecraft.util.Mth.lerp(partial,bobber.yo,bobber.getY())+.12,net.minecraft.util.Mth.lerp(partial,bobber.zo,bobber.getZ()));
        var end=new net.minecraft.world.phys.Vec3(net.minecraft.util.Mth.lerp(partial,parent.xo,parent.getX())+Math.cos(yaw)*.35*side-Math.sin(yaw)*.5,net.minecraft.util.Mth.lerp(partial,parent.yo,parent.getY())+parent.getBbHeight()*.72,net.minecraft.util.Mth.lerp(partial,parent.zo,parent.getZ())+Math.sin(yaw)*.35*side+Math.cos(yaw)*.5);
        var delta=end.subtract(start);var out=buffers.getBuffer(RenderType.lines());
        for(int i=0;i<24;i++){double a=i/24.0,b=(i+1)/24.0;float x0=(float)(delta.x*a),y0=(float)(.12+delta.y*a-.35*4*a*(1-a)),z0=(float)(delta.z*a),x1=(float)(delta.x*b),y1=(float)(.12+delta.y*b-.35*4*b*(1-b)),z1=(float)(delta.z*b);float nx=x1-x0,ny=y1-y0,nz=z1-z0,length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(length==0)continue;
            out.vertex(pose.last().pose(),x0,y0,z0).color(35,29,25,255).normal(pose.last().normal(),nx/length,ny/length,nz/length).endVertex();
            out.vertex(pose.last().pose(),x1,y1,z1).color(35,29,25,255).normal(pose.last().normal(),nx/length,ny/length,nz/length).endVertex();
        }
    }
    private static void outingScenery(ScenePropEntity e,PoseStack pose,MultiBufferSource buffers,int light){
        boolean water=e.visualSlot()==1||e.visualSlot()==4;
        if(e.visualSlot()==0||e.visualSlot()==2){for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)box(pose,buffers,light,(x+z)%2==0?Blocks.PINK_WOOL:Blocks.WHITE_WOOL,x,.015F,z,1,.035F,1);}
        if(e.visualSlot()==2){for(int i=0;i<4;i++){box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-4+i*2,.15F,4,1.8F,.1F,1);box(pose,buffers,light,Blocks.BLUE_WOOL,-3.8F+i*2,.25F,4.15F,1.4F,.04F,.7F);}}
        if(water){for(int i=0;i<4;i++){box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-2+i,.025F,-2,1,.12F,2);box(pose,buffers,light,Blocks.SPRUCE_LOG,-2+i,-.05F,-1.9F,.12F,1,.12F);}box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-2,.2F,2,4,.08F,1);for(float x:new float[]{-2,1.8F})box(pose,buffers,light,Blocks.SPRUCE_PLANKS,x,0,2,.15F,.3F,.9F);
            box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-4,0,3,2,2.3F,.13F);box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-4,0,3,.13F,2.3F,2);box(pose,buffers,light,Blocks.SPRUCE_PLANKS,-2.13F,0,3,.13F,2.3F,2);for(int i=0;i<4;i++)box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-4.25F,2.3F+i*.16F,3+i*.3F,2.5F,.16F,2.5F-i*.6F);
            for(int i=0;i<3;i++)box(pose,buffers,light,Blocks.BARREL,-3.7F+i*.7F,.05F,2.05F,.5F,.6F,.5F);
        }
        for(int i=0;i<8;i++){float a=i*(float)Math.PI/4,x=(float)Math.cos(a)*5,z=(float)Math.sin(a)*5;box(pose,buffers,light,Blocks.DARK_OAK_LOG,x,0,z,.16F,1.2F,.16F);box(pose,buffers,15728880,Blocks.LANTERN,x-.12F,1.2F,z-.12F,.4F,.45F,.4F);
            for(int j=0;j<3;j++)box(pose,buffers,light,j%2==0?Blocks.ALLIUM:Blocks.PINK_PETALS,x+.4F+j*.3F,.02F,z+.2F,.3F,.5F,.3F);}
        // Trees are real, reversible world blocks; never non-colliding decorative trunks.
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
            float x=-1.12F+i*.103F;float press=e.mode()==3?(i==11?(float)Math.max(0,Math.sin(Math.min(12,com.coraline.client.TraditionalClient.time()%60)*Math.PI/12))*.025F:0):e.mode()>0?(float)Math.max(0,Math.sin((e.tickCount+partial)*.45+i*1.7))*.025F:0;
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
