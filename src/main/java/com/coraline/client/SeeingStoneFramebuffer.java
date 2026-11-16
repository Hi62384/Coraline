package com.coraline.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Color-only replacement: never clears world depth or changes its sky clear color. */
public final class SeeingStoneFramebuffer {
    public static void copyColor(RenderTarget from,RenderTarget to){
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try{
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,from.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,to.frameBufferId);
            GL30.glBlitFramebuffer(0,0,from.width,from.height,0,0,to.width,to.height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
        }finally{
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
        }
    }
    private SeeingStoneFramebuffer(){}
}
