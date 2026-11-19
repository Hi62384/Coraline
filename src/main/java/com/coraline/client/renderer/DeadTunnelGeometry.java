package com.coraline.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Door units match tiny_door_portal.json. The rim joins the rear brick reveal. */
final class DeadTunnelGeometry {
    static final int SIDES=32;
    static final double STEP=.125;
    static final double MOUTH=.5;

    static PortalAperture.Vertex wall(int ring,int side,boolean collar){
        double z=ring*STEP;
        int perimeter=Math.floorMod(side,SIDES),edge=perimeter/8;
        double t=(perimeter%8)/8.0;
        double left=PortalAperture.LEFT,right=PortalAperture.RIGHT,bottom=PortalAperture.BOTTOM,top=PortalAperture.TOP;
        double x=switch(edge){case 0->left+(right-left)*t;case 1->right;case 2->right-(right-left)*t;default->left;};
        double y=switch(edge){case 0->bottom;case 1->bottom+(top-bottom)*t;case 2->top;default->top-(top-bottom)*t;};
        double a=Math.atan2(y+.03125,x);
        double fold=z*5.2+.5*Math.sin(a*3+z*.7)+.25*Math.sin(z*1.3);
        double radius=.466+Math.sin(fold)*.019+Math.sin(a*5+z)*.011;
        double blend=collar?Math.max(0,Math.min(1,(z-.125)/.625)):1;
        x+=(Math.cos(a)*radius-x)*blend;
        y+=(Math.sin(a)*radius-y)*blend;
        return new PortalAperture.Vertex(x,y,z,side/8F+(float)z*.1F,(float)z*1.14F,(float)(.78+.22*Math.cos(fold-.7)));
    }

    static List<PortalAperture.Vertex> end(int ring,int side,boolean collar){
        var a=dark(wall(ring,side,collar));
        var b=dark(wall(ring,side+1,collar));
        return List.of(a,new PortalAperture.Vertex(0,0,ring*STEP+.04,.5F,.5F,.04F),b);
    }
    private static PortalAperture.Vertex dark(PortalAperture.Vertex point){
        return new PortalAperture.Vertex(point.x(),point.y(),point.z(),point.u(),point.v(),.047F);
    }

    static final List<PortalAperture.Vertex[]> WEBS=webs();

    private static List<PortalAperture.Vertex[]> webs(){
        var result=new ArrayList<PortalAperture.Vertex[]>();
        var random=new Random(0xC0B0EBL);
        for(int i=0;i<49;i++){
            double z=.55+i*.44,angle=random.nextDouble()*Math.PI*2;
            var hub=point(.35,angle,z-.08);
            var anchors=new PortalAperture.Vertex[6];
            for(int j=0;j<anchors.length;j++){
                double theta=j*Math.PI/3;
                anchors[j]=point(.487,angle+.4*Math.cos(theta),z+.23*Math.sin(theta));
                strand(result,hub,anchors[j],.003+random.nextDouble()*.0025);
            }
            for(int ring=1;ring<=3;ring++){
                for(int j=0;j<anchors.length;j++){
                    var a=hub.mix(anchors[j],ring*.235);
                    var b=hub.mix(anchors[(j+1)%anchors.length],ring*.235);
                    var middle=a.mix(b,.5);
                    middle=new PortalAperture.Vertex(middle.x(),middle.y()-.011,middle.z(),0,0,middle.shade());
                    strand(result,a,middle,.0025);strand(result,middle,b,.0025);
                }
            }
        }
        return List.copyOf(result);
    }

    private static PortalAperture.Vertex point(double radius,double a,double z){
        return new PortalAperture.Vertex(radius*Math.cos(a),radius*Math.sin(a),z,0,0,.67F);
    }

    private static void strand(List<PortalAperture.Vertex[]> out,PortalAperture.Vertex a,PortalAperture.Vertex b,double halfWidth){
        // A three-sided solid filament keeps its silhouette when viewed down the crawl.
        double dx=b.x()-a.x(),dy=b.y()-a.y(),dz=b.z()-a.z();
        double lateral=Math.hypot(dx,dy);
        if(lateral<1e-6)return;
        double ux=dy/lateral,uy=-dx/lateral;
        double length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        double vx=dz*uy/length,vy=-dz*ux/length,vz=(dy*ux-dx*uy)/length;
        for(int face=0;face<3;face++){
            double t=face*Math.PI*2/3,u=(face+1)*Math.PI*2/3;
            double x=(ux*Math.cos(t)+vx*Math.sin(t))*halfWidth,y=(uy*Math.cos(t)+vy*Math.sin(t))*halfWidth,w=vz*Math.sin(t)*halfWidth;
            double xx=(ux*Math.cos(u)+vx*Math.sin(u))*halfWidth,yy=(uy*Math.cos(u)+vy*Math.sin(u))*halfWidth,ww=vz*Math.sin(u)*halfWidth;
            out.add(new PortalAperture.Vertex[]{offset(a,x,y,w),offset(b,x,y,w),offset(b,xx,yy,ww),offset(a,xx,yy,ww)});
        }
    }

    private static PortalAperture.Vertex offset(PortalAperture.Vertex p,double x,double y,double z){
        return new PortalAperture.Vertex(p.x()+x,p.y()+y,p.z()+z,0,0,p.shade());
    }
    private DeadTunnelGeometry(){}
}
