package com.coraline.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/** Offscreen rendering must not leak clear/fog/depth/viewport state into sky or hands. */
final class SeeingStoneRenderState implements AutoCloseable {
    private final int[] viewport=new int[4],scissorBox=new int[4];
    private final float[] clear=new float[4],color=RenderSystem.getShaderColor().clone(),fog=RenderSystem.getShaderFogColor().clone();
    private final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),write=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),active=GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
    private final int[] textures=new int[12];
    private final float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
    private final com.mojang.blaze3d.shaders.FogShape shape=RenderSystem.getShaderFogShape();
    private final net.minecraft.client.renderer.ShaderInstance shader=RenderSystem.getShader();
    private final org.joml.Matrix4f projection=new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
    private final com.mojang.blaze3d.vertex.VertexSorting sorting=RenderSystem.getVertexSorting();
    SeeingStoneRenderState(){
        GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissorBox);GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        for(int i=0;i<textures.length;i++)textures[i]=RenderSystem.getShaderTexture(i);
        RenderSystem.getModelViewStack().pushPose();RenderSystem.disableScissor();
    }
    @Override public void close(){
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
        RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);
        RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.getModelViewStack().popPose();RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);RenderSystem.setShaderFogColor(fog[0],fog[1],fog[2],fog[3]);RenderSystem.setShaderFogShape(shape);
        RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.setShader(()->shader);
        for(int i=0;i<textures.length;i++)RenderSystem.setShaderTexture(i,textures[i]);RenderSystem.activeTexture(active);
        RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
        RenderSystem.depthFunc(depthFunc);RenderSystem.depthMask(write);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
        if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
        if(scissor)RenderSystem.enableScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);else RenderSystem.disableScissor();
    }
}
