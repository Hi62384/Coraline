package com.coraline.client.renderer;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Low-poly brass-and-enamel mantis geometry authored in Blender. */
final class MantisMesh {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private static volatile float[][] triangles;
    private MantisMesh(){}

    private static float[][] load(){
        if(triangles!=null)return triangles;
        try(var stream=MantisMesh.class.getResourceAsStream("/assets/coraline/models/entity/garden_mantis_mesh.json")){
            if(stream==null)throw new IllegalStateException("Magic Garden mantis mesh is missing");
            var raw=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("triangles");
            float[][] result=new float[raw.size()][12];
            for(int i=0;i<raw.size();i++)for(int j=0;j<12;j++)result[i][j]=raw.get(i).getAsJsonArray().get(j).getAsFloat();
            return triangles=result;
        }catch(Exception ex){throw new IllegalStateException("Cannot load Magic Garden mantis mesh",ex);}
    }

    static void render(PoseStack pose,MultiBufferSource buffers,int light){
        var last=pose.last();var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for(float[] tri:load()){
            Vector3f ab=new Vector3f(tri[3]-tri[0],tri[4]-tri[1],tri[5]-tri[2]);
            Vector3f ac=new Vector3f(tri[6]-tri[0],tri[7]-tri[1],tri[8]-tri[2]);
            Vector3f normal=ab.cross(ac).normalize();
            // Blender stores these material colors in linear space; convert to
            // display-space vertex tint before Minecraft multiplies the texture.
            int r=tint(tri[9]),g=tint(tri[10]),b=tint(tri[11]);
            for(int v=0;v<3;v++)consumer.vertex(last.pose(),tri[v*3],tri[v*3+1],tri[v*3+2])
                .color(r,g,b,255).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(last.normal(),normal.x,normal.y,normal.z).endVertex();
        }
    }
    private static int tint(float linear){return Math.min(255,Math.max(0,Math.round((float)Math.pow(linear,1/2.2)*255)));}
}
