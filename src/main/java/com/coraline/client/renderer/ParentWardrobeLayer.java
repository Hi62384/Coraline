package com.coraline.client.renderer;

import com.coraline.parents.ParentEntity;
import com.coraline.client.clothing.TailoredArmorModel;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;

/** Identity-specific hair and clothing silhouettes. Uses existing saved/synchronized skin IDs. */
public final class ParentWardrobeLayer extends RenderLayer<ParentEntity,PlayerModel<ParentEntity>> {
    private final Map<String,HumanoidModel<ParentEntity>> models=new HashMap<>();
    public ParentWardrobeLayer(RenderLayerParent<ParentEntity,PlayerModel<ParentEntity>> renderer){super(renderer);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,ParentEntity p,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(p.isInvisible())return;
        String id=(p.isMom()?"mom_":"dad_")+p.skin();
        var model=models.computeIfAbsent(id,key->new HumanoidModel<>(TailoredArmorModel.geometry("wardrobe/parents/"+key+".json",null).bakeRoot()));
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(getParentModel(),model);
        model.setAllVisible(true);model.hat.visible=false;
        model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("coraline","textures/models/wardrobe/parents/"+id+".png"))),light,LivingEntityRenderer.getOverlayCoords(p,0),1,1,1,1);
    }
}
