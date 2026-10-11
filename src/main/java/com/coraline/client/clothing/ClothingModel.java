package com.coraline.client.clothing;

import com.coraline.clothing.ClothingItem.Style;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import java.util.EnumMap;

/** Proper moving humanoid limbs, with tailored hems, slipper soles, boot wings and a tall hat. */
public final class ClothingModel extends HumanoidModel<LivingEntity> {
    private static final EnumMap<Style,ClothingModel> MODELS=new EnumMap<>(Style.class);
    private ClothingModel(ModelPart root){super(root);}
    public static HumanoidModel<?> forStyle(Style style,HumanoidModel<?> original){
        var m=MODELS.computeIfAbsent(style,s->new ClothingModel(layer(s).bakeRoot()));
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,m);
        m.setAllVisible(false);
        switch(style){
            case TOP_HAT,MIDNIGHT_GOGGLES,UNBINDING_BUTTON_EYES->m.head.visible=true;
            case FEATHER_BOOTS,SILENT_SLIPPERS->m.rightLeg.visible=m.leftLeg.visible=true;
            default->{
                m.body.visible=m.rightArm.visible=m.leftArm.visible=true;
                m.rightLeg.visible=m.leftLeg.visible=style==Style.COMFY_PAJAMAS||style==Style.GARDEN_OVERALLS||style==Style.MIDNIGHT_COAT||style==Style.CIRCUS_JACKET;
            }
        }
        return m;
    }
    private static LayerDefinition layer(Style style){
        var mesh=HumanoidModel.createMesh(new CubeDeformation(.4F),0);var root=mesh.getRoot();
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        if(style==Style.UNBINDING_BUTTON_EYES){
            var buttons=CubeListBuilder.create();
            for(float x:new float[]{-2.1F,2.1F}){
                buttons.texOffs(0,0).addBox(x-1.5F,-5.6F,-4.6F,3,3,.5F);
                buttons.texOffs(0,8).addBox(x-1.8F,-5.2F,-4.6F,3.6F,2.2F,.5F);
                for(float dx:new float[]{-.5F,.5F})for(float y:new float[]{-4.7F,-3.7F})buttons.texOffs(16,0).addBox(x+dx-.2F,y,-4.7F,.4F,.4F,.15F);
            }
            root.addOrReplaceChild("head",buttons,PartPose.ZERO);
        }
        if(style==Style.MIDNIGHT_GOGGLES){
            root.addOrReplaceChild("head",CubeListBuilder.create()
                .texOffs(0,0).addBox(-4.3F,-5.5F,-4.3F,8.6F,2,8.6F)
                .texOffs(0,16).addBox(-4,-6,-5,3.5F,3.5F,1)
                .texOffs(12,16).addBox(.5F,-6,-5,3.5F,3.5F,1)
                .texOffs(24,16).addBox(-.5F,-4.7F,-5,1,1,1),PartPose.ZERO);
        }
        if(style==Style.TOP_HAT){
            root.addOrReplaceChild("head",CubeListBuilder.create()
                .texOffs(0,0).addBox(-6,-8.8F,-6,12,1,12)
                .texOffs(0,16).addBox(-4.25F,-17,-4.25F,8.5F,8.5F,8.5F)
                .texOffs(36,18).addBox(-1,-11,-4.4F,2,2,.3F),PartPose.ZERO);
        }
        boolean boots=style==Style.FEATHER_BOOTS||style==Style.SILENT_SLIPPERS;
        boolean coat=style==Style.MIDNIGHT_COAT||style==Style.CIRCUS_JACKET;
        for(boolean left:new boolean[]{false,true}){
            float origin=left?1.9F:-1.9F;
            var leg=CubeListBuilder.create().texOffs(0,32);
            if(boots){
                float start=style==Style.SILENT_SLIPPERS?9:6;
                leg.addBox(-2,start,-2.4F,4,12-start,4.8F,new CubeDeformation(.45F));
                if(style==Style.FEATHER_BOOTS){
                    float x=left?2.2F:-3.2F;
                    leg.texOffs(20,38).addBox(x,5,-.5F,1,5,4)
                       .texOffs(32,38).addBox(x,3,1.2F,1,4,1.8F);
                }
            }else leg.addBox(-2,0,-2,4,coat?5:12,4,new CubeDeformation(coat||style==Style.GARDEN_OVERALLS?.55F:.35F));
            root.addOrReplaceChild(left?"left_leg":"right_leg",leg,PartPose.offset(origin,12,0));
            if(!boots){
                float x=left?-1:-3;
                var arm=CubeListBuilder.create().texOffs(40,left?40:16).addBox(x,-2,-2,4,style==Style.STAR_SHIRT?5:10,4,new CubeDeformation(.4F));
                if(style==Style.CIRCUS_JACKET)arm.texOffs(40,0).addBox(x-.2F,-2.8F,-2.2F,4.4F,1.2F,4.4F);
                root.addOrReplaceChild(left?"left_arm":"right_arm",arm,PartPose.offset(left?5:-5,2,0));
            }
        }
        if(style==Style.RAINCOAT)root.getChild("body").addOrReplaceChild("folded_hood",CubeListBuilder.create().texOffs(0,0).addBox(-4,-1,1.8F,8,4,3),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64);
    }
}
