package com.coraline.client.renderer;

import com.coraline.tycoon.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

/** Vanilla humanoid animation and held-item layers, using authorized skin data. */
public final class TycoonWorkerRenderer extends MobRenderer<TycoonWorker,PlayerModel<TycoonWorker>> {
    public TycoonWorkerRenderer(EntityRendererProvider.Context context){super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),.4F);addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));}
    @Override public ResourceLocation getTextureLocation(TycoonWorker worker){
        if(worker.rank()>=4&&worker.role()==TycoonWorkerType.OVERWORLD_MAID&&worker.owner()!=null){var connection=Minecraft.getInstance().getConnection();var profile=connection==null?null:connection.getPlayerInfo(worker.owner());if(profile!=null)return profile.getSkinLocation();return DefaultPlayerSkin.getDefaultSkin(worker.owner());}
        return DefaultPlayerSkin.getDefaultSkin(java.util.UUID.nameUUIDFromBytes((worker.role().id()+worker.rank()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Override protected void scale(TycoonWorker worker,PoseStack pose,float partial){float size=worker.rank()==5?1:.82F+worker.rank()*.035F;pose.scale(size,size,size);}
}
