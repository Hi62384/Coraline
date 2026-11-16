package com.coraline.client.renderer;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Cached, indexed background-Blender exports. No per-frame JSON or model rebuilds. */
final class InsectFurnitureMesh {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private record Surface(String part,int color,float[][] vertices,int[][] faces,float[][] normals){}
    private static final Map<String,List<Surface>> CACHE=new HashMap<>();
    private static List<Surface> load(String name){
        try(var stream=Minecraft.getInstance().getResourceManager().open(ResourceLocation.fromNamespaceAndPath("coraline","insect_furniture/"+name+".json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var doc=JsonParser.parseReader(reader).getAsJsonObject();var surfaces=new ArrayList<Surface>();
            for(var raw:doc.getAsJsonArray("meshes")){
                var m=raw.getAsJsonObject();var points=m.getAsJsonArray("vertices");float[][] vertices=new float[points.size()][3];
                for(int i=0;i<vertices.length;i++)for(int j=0;j<3;j++)vertices[i][j]=points.get(i).getAsJsonArray().get(j).getAsFloat();
                var triangles=m.getAsJsonArray("faces");int[][] faces=new int[triangles.size()][3];
                for(int i=0;i<faces.length;i++)for(int j=0;j<3;j++)faces[i][j]=triangles.get(i).getAsJsonArray().get(j).getAsInt();
                float[][] normals=new float[faces.length][3];for(int i=0;i<faces.length;i++){var a=vertices[faces[i][0]];var c=vertices[faces[i][1]];var d=vertices[faces[i][2]];var normal=new org.joml.Vector3f(c[0]-a[0],c[1]-a[1],c[2]-a[2]).cross(new org.joml.Vector3f(d[0]-a[0],d[1]-a[1],d[2]-a[2]));if(normal.lengthSquared()>1e-12F)normal.normalize();normals[i]=new float[]{normal.x,normal.y,normal.z};}
                surfaces.add(new Surface(m.get("part").getAsString(),Integer.parseInt(m.get("color").getAsString().substring(1),16),vertices,faces,normals));
            }
            return List.copyOf(surfaces);
        }catch(IOException|RuntimeException bad){org.slf4j.LoggerFactory.getLogger(InsectFurnitureMesh.class).error("Insect furniture export unavailable: "+name,bad);return List.of();}
    }
    static void clear(){CACHE.clear();}
    static boolean render(String name,PoseStack pose,MultiBufferSource buffers,int light,double time,float walking,float glow){
        var surfaces=CACHE.computeIfAbsent(name,InsectFurnitureMesh::load);if(surfaces.isEmpty())return false;
        for(var surface:surfaces){
            if(surface.part.equals("glow")&&glow<=0)continue;
            var out=buffers.getBuffer(surface.part.equals("glow")?RenderType.eyes(WHITE):RenderType.entityCutoutNoCull(WHITE));
            pose.pushPose();
            if(surface.part.startsWith("leg_")){int side=surface.part.contains("-1")?-1:1;int index=Integer.parseInt(surface.part.substring(surface.part.lastIndexOf('_')+1));float angle=(float)Math.sin(time*.20+index*2.1+side)*walking;pose.mulPose(Axis.YP.rotation(angle));}
            else if(surface.part.startsWith("antenna_"))pose.mulPose(Axis.ZP.rotation((float)Math.sin(time*.045)*.018F));
            float brightness=surface.part.equals("glow")?.15F+.85F*Math.min(1,glow):1;
            float r=((surface.color>>16)&255)/255F*brightness,g=((surface.color>>8)&255)/255F*brightness,b=(surface.color&255)/255F*brightness;
            for(int i=0;i<surface.faces.length;i++){
                var face=surface.faces[i];var normal=surface.normals[i];
                for(int j=0;j<4;j++){var v=surface.vertices[face[Math.min(j,2)]];out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(r,g,b,1).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),normal[0],normal[1],normal[2]).endVertex();}
            }
            pose.popPose();
        }
        return true;
    }
    private InsectFurnitureMesh(){}
}
