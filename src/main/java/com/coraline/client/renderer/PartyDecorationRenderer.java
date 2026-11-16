package com.coraline.client.renderer;

import com.coraline.feast.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.*;

/** Block-atlas meshes consistent with ScenePropRenderer; synchronized party state, no client reward logic. */
public final class PartyDecorationRenderer {
    private static void box(PoseStack p,MultiBufferSource b,int light,Block block,float x,float y,float z,float w,float h,float d){if(w<=0||h<=0||d<=0)return;p.pushPose();p.translate(x,y,z);p.scale(w,h,d);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);p.popPose();}
    private static Block color(int n){return switch(Math.floorMod(n,4)){case 0->Blocks.PINK_TERRACOTTA;case 1->Blocks.CYAN_TERRACOTTA;case 2->Blocks.WHITE_TERRACOTTA;default->Blocks.PURPUR_BLOCK;};}
    public static boolean render(ScenePropEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var n=e.cakeData();if(!n.getBoolean("PartyDecoration")&&!n.getBoolean("PlayerPartyCake"))return false;pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        if(n.getBoolean("PlayerPartyCake"))cake(n.getInt("Slices"),n.getBoolean("CandlesOut"),pose,buffers,light);
        else {float width=Math.max(.5F,Math.min(9,n.getFloat("Width")));int palette=n.getInt("Palette");switch(n.getInt("Style")){
            case PartyDecorations.GARLAND->garland(pose,buffers,light,width,palette);
            case PartyDecorations.BOUQUET->flowers(pose,buffers,light,0,0,0);
            case PartyDecorations.BALLOONS->balloons(pose,buffers,light,palette,(e.tickCount+partial)*.03F);
            case PartyDecorations.ARCH->{for(float x:new float[]{-width/2,width/2-.14F}){box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,x,0,-.13F,.14F,3.2F,.26F);box(pose,buffers,light,Blocks.GOLD_BLOCK,x-.03F,.08F,-.16F,.20F,.15F,.32F);for(int i=0;i<5;i++)flowers(pose,buffers,light,x+.07F,.45F+i*.55F,0);}box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-width/2,3.15F,-.13F,width,.14F,.26F);pose.pushPose();pose.translate(0,3.08,0);garland(pose,buffers,light,width,palette);pose.popPose();}
            case PartyDecorations.TABLECLOTH->{float depth=n.contains("TableDepth")?n.getFloat("TableDepth"):n.getBoolean("LongTable")?8.8F:3.5F;box(pose,buffers,light,Blocks.WHITE_WOOL,-1.78F,-.015F,-depth/2,3.56F,.025F,depth);for(float x:new float[]{-1.78F,1.75F})box(pose,buffers,light,color(palette),x,-.26F,-depth/2,.03F,.26F,depth);flowers(pose,buffers,light,0,.02F,0);for(float z:new float[]{-.9F,.9F})candle(pose,buffers,light,.7F,.03F,z,true);}
            case PartyDecorations.LANTERNS->{for(int i=0;i<7;i++){float x=-width/2+i*width/6,y=-(float)Math.sin(i*Math.PI/6)*.35F;box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,x,y,-.025F,width/6,.025F,.025F);box(pose,buffers,light,Blocks.COPPER_BLOCK,x-.12F,y-.4F,-.12F,.24F,.05F,.24F);box(pose,buffers,15728880,Blocks.ORANGE_STAINED_GLASS,x-.09F,y-.35F,-.09F,.18F,.20F,.18F);box(pose,buffers,light,Blocks.COPPER_BLOCK,x-.12F,y-.15F,-.12F,.24F,.05F,.24F);}}
            case PartyDecorations.CIRCUS_STAGE->{box(pose,buffers,light,Blocks.DARK_OAK_PLANKS,-4,-.08F,-4,8,.08F,8);for(float x:new float[]{-4,3.88F}){box(pose,buffers,light,Blocks.GOLD_BLOCK,x,0,-3.9F,.12F,3,.12F);for(int i=0;i<4;i++)box(pose,buffers,light,Blocks.RED_TERRACOTTA,x+(x<0?i*.18F:-i*.18F),0,-3.8F+(i%2)*.08F,.18F,2.7F,.18F);}box(pose,buffers,light,Blocks.GOLD_BLOCK,-4,2.9F,-3.9F,8,.1F,.12F);pose.pushPose();pose.translate(0,2.75,-3.7);garland(pose,buffers,light,7.5F,1);pose.popPose();for(float x:new float[]{-2.5F,0,2.5F}){box(pose,buffers,light,color((int)x),x-.3F,.02F,-.3F,.6F,.22F,.6F);for(int i=0;i<12;i++){double a=i*Math.PI*2/12;box(pose,buffers,light,Blocks.GOLD_BLOCK,x+(float)Math.cos(a)*.65F-.06F,.95F+(float)Math.sin(a)*.65F,1.6F,.12F,.12F,.08F);}}}
            default->{}
        }}pose.popPose();return true;
    }
    private static void garland(PoseStack p,MultiBufferSource b,int light,float width,int palette){for(int i=0;i<14;i++){float x=-width/2+i*width/14,y=-(float)Math.sin(i*Math.PI/13)*.28F;box(p,b,light,Blocks.DARK_OAK_PLANKS,x,y,0,width/14,.018F,.018F);float w=Math.min(.32F,width/20);box(p,b,light,color(i+palette),x+.04F,y-.17F,.01F,w,.18F,.02F);box(p,b,light,color(i+palette),x+.04F+w*.2F,y-.26F,.01F,w*.6F,.09F,.02F);if(i%4==1){box(p,b,light,Blocks.GOLD_BLOCK,x+.06F,y-.36F,.02F,.035F,.11F,.025F);box(p,b,light,Blocks.GOLD_BLOCK,x+.02F,y-.32F,.02F,.115F,.03F,.025F);}}}
    private static void flowers(PoseStack p,MultiBufferSource b,int light,float x,float y,float z){box(p,b,light,Blocks.COPPER_BLOCK,x-.10F,y,z-.1F,.2F,.22F,.2F);for(int i=0;i<5;i++){float dx=(float)Math.cos(i*1.256)*.14F,dz=(float)Math.sin(i*1.256)*.14F;box(p,b,light,Blocks.GREEN_TERRACOTTA,x+dx-.015F,y+.2F,z+dz-.015F,.03F,.23F,.03F);box(p,b,light,i%2==0?Blocks.PINK_WOOL:Blocks.WHITE_WOOL,x+dx-.085F,y+.40F,z+dz-.085F,.17F,.09F,.17F);box(p,b,light,Blocks.GOLD_BLOCK,x+dx-.025F,y+.49F,z+dz-.025F,.05F,.025F,.05F);}}
    private static void candle(PoseStack p,MultiBufferSource b,int light,float x,float y,float z,boolean lit){box(p,b,light,Blocks.GOLD_BLOCK,x-.08F,y,z-.08F,.16F,.03F,.16F);box(p,b,light,Blocks.WHITE_TERRACOTTA,x-.035F,y+.03F,z-.035F,.07F,.24F,.07F);box(p,b,light,Blocks.BLACK_CONCRETE,x-.007F,y+.27F,z-.007F,.014F,.035F,.014F);if(lit)box(p,b,15728880,Blocks.ORANGE_STAINED_GLASS,x-.025F,y+.30F,z-.025F,.05F,.085F,.05F);}
    private static void balloons(PoseStack p,MultiBufferSource b,int light,int palette,float time){for(int i=0;i<3;i++){float x=(i-1)*.3F,y=1.5F+(i%2)*.3F+(float)Math.sin(time+i)*.035F;box(p,b,light,Blocks.DARK_OAK_PLANKS,x,0,0,.012F,y,.012F);box(p,b,light,color(i+palette),x-.14F,y,-.14F,.28F,.44F,.28F);box(p,b,light,color(i+palette),x-.21F,y+.08F,-.19F,.42F,.27F,.38F);box(p,b,light,Blocks.WHITE_TERRACOTTA,x-.13F,y+.23F,-.195F,.04F,.11F,.012F);}}
    private static void cake(int slices,boolean out,PoseStack p,MultiBufferSource b,int light){
        int remaining=PartyLayoutRules.cakeSlices(slices);box(p,b,light,Blocks.GOLD_BLOCK,-.48F,0,-.48F,.96F,.045F,.96F);float end=-.43F+.86F*remaining/12;
        for(int i=0;i<remaining;i++){float a=-.43F+i*.86F/12,c=-.43F+(i+1)*.86F/12;box(p,b,light,Blocks.YELLOW_TERRACOTTA,a,.045F,-.40F,c-a,.22F,.8F);box(p,b,light,Blocks.WHITE_CONCRETE,a,.265F,-.43F,c-a,.10F,.86F);float lo=Math.max(a,-.35F),hi=Math.min(c,.35F);if(hi>lo){box(p,b,light,Blocks.PINK_TERRACOTTA,lo,.365F,-.32F,hi-lo,.20F,.64F);box(p,b,light,Blocks.WHITE_CONCRETE,lo,.565F,-.35F,hi-lo,.10F,.7F);}}
        for(float x:new float[]{-.26F,0,.26F})if(x+.04F<end&&remaining>0)for(float z:new float[]{-.21F,.21F})candle(p,b,light,x,.665F,z,!out);
        for(float x:new float[]{-.35F,-.15F,.05F,.25F})if(x+.07F<end){box(p,b,light,Blocks.PINK_WOOL,x,.37F,-.435F,.08F,.055F,.03F);box(p,b,light,Blocks.GOLD_BLOCK,x+.02F,.383F,-.47F,.04F,.027F,.03F);}
    }
    private PartyDecorationRenderer(){}
}
