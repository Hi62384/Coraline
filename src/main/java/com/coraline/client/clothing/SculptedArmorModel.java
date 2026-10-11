package com.coraline.client.clothing;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Indexed cloth meshes share the vanilla skeleton, armor pass, light and enchantment glint.
 * Geometry is loaded once per item and invalidated by resource pack reloads.
 */
public final class SculptedArmorModel extends HumanoidModel<LivingEntity> {
    private static final Map<String,SculptedArmorModel> CACHE=new HashMap<>();
    private final List<Panel> panels=new ArrayList<>();
    private double viewDistance;
    private final ModelPart cape=new ModelPart(List.of(),Map.of());
    private record Point(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    private record Panel(ModelPart bone,float r,float g,float b,int detail,boolean emissive,List<Point> vertices) {}

    private SculptedArmorModel(String id) {
        super(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());
        var resource=new ResourceLocation("coraline","sculpted/"+id+".json");
        try(var stream=Minecraft.getInstance().getResourceManager().open(resource);
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            var data=JsonParser.parseReader(reader).getAsJsonObject();
            var materials=data.getAsJsonObject("materials");
            for(var entry:data.getAsJsonArray("meshes")) {
                var mesh=entry.getAsJsonObject();
                ModelPart bone=switch(mesh.get("bone").getAsString()) {
                    case "head"->head;case "body"->body;case "left_arm"->leftArm;
                    case "right_arm"->rightArm;case "left_leg"->leftLeg;case "right_leg"->rightLeg;case "cape"->cape;
                    default->throw new IOException("Unknown costume bone");
                };
                var xyz=mesh.getAsJsonArray("vertices");var uv=mesh.getAsJsonArray("uv");
                Vector3f[] positions=new Vector3f[xyz.size()],normals=new Vector3f[xyz.size()];
                for(int i=0;i<xyz.size();i++){var a=xyz.get(i).getAsJsonArray();positions[i]=new Vector3f(a.get(0).getAsFloat()/16,a.get(1).getAsFloat()/16,a.get(2).getAsFloat()/16);normals[i]=new Vector3f();}
                for(var f:mesh.getAsJsonArray("faces")) {
                    var face=f.getAsJsonArray();int a=face.get(0).getAsInt(),b=face.get(1).getAsInt(),c=face.get(2).getAsInt();
                    var n=new Vector3f(positions[b]).sub(positions[a]).cross(new Vector3f(positions[c]).sub(positions[a]));
                    for(var index:face)normals[index.getAsInt()].add(n);
                }
                for(var n:normals)if(n.lengthSquared()>1e-12)n.normalize();else n.set(0,0,-1);
                if(mesh.has("normals")){
                    var authored=mesh.getAsJsonArray("normals");
                    if(authored.size()!=normals.length)throw new IOException("Mismatched authored normals");
                    for(int i=0;i<normals.length;i++){var n=authored.get(i).getAsJsonArray();normals[i].set(n.get(0).getAsFloat(),n.get(1).getAsFloat(),n.get(2).getAsFloat());}
                }
                int color=Integer.parseInt(materials.get(mesh.get("material").getAsString()).getAsString().substring(1),16);
                List<Point> vertices=new ArrayList<>();
                for(var f:mesh.getAsJsonArray("faces")) {
                    var face=f.getAsJsonArray();
                    // Native armor VertexConsumer is QUADS. Triangle fans repeat the final corner.
                    if(face.size()==4){for(var ix:face)add(vertices,ix.getAsInt(),positions,normals,uv);}
                    else for(int i=1;i+1<face.size();i++)for(int j:new int[]{0,i,i+1,i+1})add(vertices,face.get(j).getAsInt(),positions,normals,uv);
                }
                String name=mesh.get("name").getAsString().toLowerCase(Locale.ROOT);
                int detail=name.contains("stitch")||name.contains("thread")||name.contains("hole")?2:name.contains("embroid")||name.contains("star")||name.contains("vein")?1:0;
                panels.add(new Panel(bone,((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,detail,mesh.has("emissive")&&mesh.get("emissive").getAsBoolean(),vertices));
            }
            // One transform per bone/material/detail class, not per individual stitch.
            Map<String,Panel> batches=new LinkedHashMap<>();
            for(var panel:panels){
                String key=System.identityHashCode(panel.bone)+":"+panel.r+":"+panel.g+":"+panel.b+":"+panel.detail+":"+panel.emissive;
                var batch=batches.get(key);
                if(batch==null)batches.put(key,panel);else batch.vertices.addAll(panel.vertices);
            }
            panels.clear();panels.addAll(batches.values());
        } catch(IOException|RuntimeException ex) {throw new IllegalStateException("Unable to load costume "+id,ex);}
    }
    private static void add(List<Point> out,int i,Vector3f[] p,Vector3f[] n,JsonArray uv){
        var t=uv.get(i).getAsJsonArray();out.add(new Point(p[i].x,p[i].y,p[i].z,t.get(0).getAsFloat(),t.get(1).getAsFloat(),n[i].x,n[i].y,n[i].z));
    }
    public static HumanoidModel<?> forItem(String id,HumanoidModel<?> original,LivingEntity entity){
        var model=CACHE.computeIfAbsent(id,SculptedArmorModel::new);
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,model);
        // File already contains precisely the relevant piece (including coat tails/overall legs).
        model.setAllVisible(true);
        model.cape.copyFrom(model.body);
        model.cape.xRot-=.035f*(float)Math.sin(entity.tickCount*.045);
        model.viewDistance=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(entity.position());
        return model;
    }
    public static void clearCache(){CACHE.clear();}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,float r,float g,float b,float alpha){
        for(var panel:panels){
            if(panel.detail==2&&viewDistance>16*16||panel.detail==1&&viewDistance>32*32)continue;
            pose.pushPose();
            if(young){
                if(panel.bone==head){pose.scale(.75f,.75f,.75f);pose.translate(0,1,0);}
                else{pose.scale(.5f,.5f,.5f);pose.translate(0,1.5,0);}
            }
            panel.bone.translateAndRotate(pose);
            var transform=pose.last();
            for(var v:panel.vertices)consumer.vertex(transform.pose(),v.x,v.y,v.z)
                .color(r*panel.r,g*panel.g,b*panel.b,alpha).uv(v.u,v.v).overlayCoords(overlay).uv2(panel.emissive?net.minecraft.client.renderer.LightTexture.FULL_BRIGHT:light)
                .normal(transform.normal(),v.nx,v.ny,v.nz).endVertex();
            pose.popPose();
        }
    }
}
