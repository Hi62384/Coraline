package com.coraline.client.clothing;

import com.coraline.clothing.ArmorSets;
import com.google.gson.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Same authored cuboids and palettes used by the Blender inventory renders. */
public final class TailoredArmorModel extends HumanoidModel<LivingEntity> {
    private static final Map<String,TailoredArmorModel> CACHE=new HashMap<>();
    private TailoredArmorModel(ModelPart root){super(root);}
    public static HumanoidModel<?> forSet(ArmorSets.Set set,EquipmentSlot slot,HumanoidModel<?> original){
        var model=CACHE.computeIfAbsent(set.id()+slot,key->new TailoredArmorModel(layer(set.id(),slot).bakeRoot()));
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,model);
        model.setAllVisible(false);
        switch(slot){
            case HEAD->model.head.visible=true;
            case CHEST->{model.body.visible=model.rightArm.visible=model.leftArm.visible=true;model.rightLeg.visible=model.leftLeg.visible=true;}
            case LEGS->{model.body.visible=model.rightLeg.visible=model.leftLeg.visible=true;}
            case FEET->model.rightLeg.visible=model.leftLeg.visible=true;
            default->{}
        }
        return model;
    }
    private static LayerDefinition layer(String id,EquipmentSlot slot){
        String piece=switch(slot){case HEAD->"helmet";case CHEST->"chestplate";case LEGS->"leggings";default->"boots";};
        return geometry("wardrobe/"+id+".json",piece);
    }
    public static LayerDefinition geometry(String resource,String piece){
        var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();
        Map<String,PartDefinition> parts=new HashMap<>();
        for(String name:List.of("head","hat","body","right_arm","left_arm","right_leg","left_leg")){
            PartPose pose=switch(name){case "right_arm"->PartPose.offset(-5,2,0);case "left_arm"->PartPose.offset(5,2,0);case "right_leg"->PartPose.offset(-1.9F,12,0);case "left_leg"->PartPose.offset(1.9F,12,0);default->PartPose.ZERO;};
            parts.put(name,root.addOrReplaceChild(name,CubeListBuilder.create(),pose));
        }
        try(var stream=TailoredArmorModel.class.getResourceAsStream("/assets/coraline/"+resource)){
            if(stream==null)throw new IOException("Missing wardrobe "+resource);
            var boxes=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("boxes");int index=0;
            for(var entry:boxes){var b=entry.getAsJsonObject();if(piece!=null&&!piece.equals(b.get("piece").getAsString()))continue;
                var a=b.getAsJsonArray("box");int material=b.get("material").getAsInt();
                var cube=CubeListBuilder.create().texOffs((material%4)*64,(material/4)*64).addBox(0,0,0,a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat());
                var r=b.getAsJsonArray("rotation");
                parts.get(b.get("part").getAsString()).addOrReplaceChild("detail_"+(index++),cube,PartPose.offsetAndRotation(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),r.get(0).getAsFloat(),r.get(1).getAsFloat(),r.get(2).getAsFloat()));
            }
        }catch(IOException e){throw new IllegalStateException(e);}
        return LayerDefinition.create(mesh,256,128);
    }
}
