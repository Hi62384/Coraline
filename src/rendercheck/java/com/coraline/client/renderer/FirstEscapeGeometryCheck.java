package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.*;

/** Exact native vertex-stream regression, not an FPS or whole-scene claim. */
public final class FirstEscapeGeometryCheck {
    public static void verify(){
        var points=new java.util.ArrayList<float[]>();var out=new VertexConsumer(){
            public VertexConsumer vertex(double x,double y,double z){points.add(new float[]{(float)x,(float)y,(float)z});return this;}
            public VertexConsumer color(int r,int g,int b,int a){return this;}
            public VertexConsumer uv(float u,float v){return this;}
            public VertexConsumer overlayCoords(int u,int v){return this;}
            public VertexConsumer uv2(int u,int v){return this;}
            public VertexConsumer normal(float x,float y,float z){var p=points.get(points.size()-1);points.set(points.size()-1,new float[]{p[0],p[1],p[2],x,y,z});return this;}
            public void endVertex(){}
            public void defaultColor(int r,int g,int b,int a){}
            public void unsetDefaultColor(){}
        };
        int checked=0;for(double[] scale:new double[][]{{1,1,1},{.12,.035,.7},{.41,.62,.05},{1.5,.28,.7}}){
            points.clear();FirstEscapePresentation.sphere(new PoseStack(),out,15728880,.25,1.125,-.625,scale[0],scale[1],scale[2],0xA13C45,.6F);int index=0;
            for(int ring=0;ring<8;ring++)for(int side=0;side<16;side++)for(int[] corner:new int[][]{{0,0},{1,0},{1,1},{0,1}}){double a=-Math.PI/2+(ring+corner[0])*Math.PI/8,t=(side+corner[1])*Math.PI/8,nx=Math.cos(a)*Math.cos(t),ny=Math.sin(a),nz=Math.cos(a)*Math.sin(t);var actual=points.get(index++);float[] expected={(float)(.25+nx*scale[0]),(float)(1.125+ny*scale[1]),(float)(-.625+nz*scale[2]),(float)nx,(float)ny,(float)nz};for(int axis=0;axis<6;axis++){if(actual[axis]!=expected[axis])throw new AssertionError("Cached ellipsoid changed vertex/normal "+index+" axis "+axis);checked++;}}
            if(points.size()!=512)throw new AssertionError("Ellipsoid topology changed");
        }
        System.out.println("FIRST_ESCAPE_GEOMETRY_NATIVE_VERIFIED exact position/normal checks="+checked+"; same topology, no detail removed");
    }
    private FirstEscapeGeometryCheck(){}
}
