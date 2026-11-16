package com.coraline.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;

/** Corresponding body regions on one mesh: geometry and skin change together, never two faded actors. */
final class MotherMorphGeometry {
    private record Pixels(int width,int height,int[] rgba){}
    private static final Map<ResourceLocation,Pixels> PIXELS=new HashMap<>();
    static void clear(){PIXELS.clear();}
    static Vector3f parentPoint(String bone,Vector3f nativePoint,Vector3f min,Vector3f max){
        var point=new Vector3f(nativePoint).sub(new Vector3f(min).add(max).mul(.5F));
        var half=new Vector3f(max).sub(min).mul(.5F);
        point.set(point.x/Math.max(.001F,half.x),point.y/Math.max(.001F,half.y),point.z/Math.max(.001F,half.z));
        // A cube-to-organic deformation, not a uniform stretch of the face.
        float edge=Math.max(Math.abs(point.x),Math.max(Math.abs(point.y),Math.abs(point.z)));
        if(edge>.001F)point.div(edge);
        float side=bone.startsWith("right")?-1:1;
        Vector3f center,size;
        if(bone.equals("head")){center=new Vector3f(0,-.25F,0);size=new Vector3f(.25F,.25F,.25F);}
        else if(bone.endsWith("forearm")){center=new Vector3f(side*.34375F,.6875F,0);size=new Vector3f(.09375F,.1875F,.125F);}
        else if(bone.endsWith("arm")){center=new Vector3f(side*.34375F,.3125F,0);size=new Vector3f(.09375F,.1875F,.125F);}
        // The evil rig's skirt hides its upper legs; its shin surfaces must start
        // as the parent's complete trouser legs rather than leaving a knee gap.
        else if(bone.endsWith("shin")){center=new Vector3f(side*.125F,1.125F,0);size=new Vector3f(.125F,.375F,.125F);}
        else if(bone.endsWith("leg")){center=new Vector3f(side*.125F,.9375F,0);size=new Vector3f(.125F,.1875F,.125F);}
        else{center=new Vector3f(0,.375F,0);size=new Vector3f(.25F,.375F,.125F);}
        return point.mul(size).add(center);
    }
    static Vector3f skin(ResourceLocation skin,String bone,Vector3f point){
        boolean head=bone.equals("head"),arm=bone.contains("arm"),left=bone.startsWith("left");
        float x=point.x,y=point.y,z=point.z;int u,v;
        if(head){u=8+(int)((x+.25F)*16);v=8+(int)((y+.5F)*16);
            if(Math.abs(x)>Math.abs(z)){u=(x<0?0:16)+(int)((z+.25F)*16);}else if(z>0)u=24+(int)((.25F-x)*16);
        }else if(arm){u=(left?36:44)+(int)((x-(left?.25F:-.4375F))*16);v=(left?52:20)+(int)((y-.125F)*16);}
        else if(bone.contains("leg")||bone.contains("shin")){u=(left?20:4)+(int)((x-(left?0:-.25F))*16);v=(left?52:20)+(int)((y-.75F)*16);}
        else{u=20+(int)((x+.25F)*16);v=20+(int)(y*16);if(z>0)u=32+(int)((.25F-x)*16);}
        var rgb=pixel(skin,u/64F,v/64F);
        // Button eyes are an existing render layer, not painted in the parent's skin.
        if(head&&z<-.245F&&Math.abs(y+.26F)<.103F&&Math.abs(Math.abs(x)-.125F)<.09F)rgb.set(.055F,.045F,.045F);
        return rgb;
    }
    static Vector3f pixel(ResourceLocation location,float u,float v){
        var image=PIXELS.computeIfAbsent(location,key->{try(var stream=Minecraft.getInstance().getResourceManager().open(key);var nativeImage=NativeImage.read(stream)){
            int width=nativeImage.getWidth(),height=nativeImage.getHeight();int[] pixels=new int[width*height];for(int y=0;y<height;y++)for(int x=0;x<width;x++)pixels[y*width+x]=nativeImage.getPixelRGBA(x,y);return new Pixels(width,height,pixels);
        }catch(IOException missing){return new Pixels(1,1,new int[]{0xFFFFFFFF});}});
        int x=Math.max(0,Math.min(image.width-1,(int)(u*image.width))),y=Math.max(0,Math.min(image.height-1,(int)(v*image.height)));
        int abgr=image.rgba[y*image.width+x];return new Vector3f((abgr&255)/255F,((abgr>>8)&255)/255F,((abgr>>16)&255)/255F);
    }
    private MotherMorphGeometry(){}
}
