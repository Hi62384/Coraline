package com.coraline.client;

import net.minecraft.client.gui.GuiGraphics;

/** Curved, asymmetrical lids rather than a rectangular black full-screen wipe. */
public final class StoryEyelids {
    public static void draw(GuiGraphics graphics,int width,int height,float closed,float partial){
        if(closed<=0)return;closed=Math.min(1,closed);
        int step=Math.max(2,width/240);float center=height*.49F;
        for(int x=0;x<width;x+=step){
            double edge=Math.abs((x+step*.5-width*.5)/(width*.5)),curve=Math.pow(edge,1.7)*height*.18;
            float halfOpening=(1-closed)*(height*.56F-(float)curve);
            int top=Math.max(0,Math.round(center-halfOpening)),bottom=Math.min(height,Math.round(center+halfOpening));
            graphics.fill(x,0,Math.min(width,x+step),top,0xFF100C13);
            graphics.fill(x,bottom,Math.min(width,x+step),height,0xFF100C13);
            if(top>0)graphics.fill(x,top,Math.min(width,x+step),Math.min(bottom,top+2),0xA52F202B);
            if(bottom<height)graphics.fill(x,Math.max(top,bottom-2),Math.min(width,x+step),bottom,0xA52F202B);
        }
    }
    private StoryEyelids(){}
}
