package com.coraline.client.renderer;

import java.util.List;

public final class LittleDoorApertureCheck {
    private static int checks;
    private static void require(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
        checks++;
    }
    private static void near(double actual,double expected,String reason){
        require(Math.abs(actual-expected)<1e-7,reason+": "+actual+" != "+expected);
    }
    private static PortalAperture.Vertex p(double x,double y,double z){
        return new PortalAperture.Vertex(x,y,z,0,0,1);
    }
    public static void main(String[] args){
        for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            int fx=direction[0],fz=direction[1];
            var front=PortalAperture.eye(fx*2-fz*.2,.1,fz*2+fx*.2,fx,fz,DeadTunnelGeometry.MOUTH);
            near(front.x(),.2,"door-local right");
            near(front.y(),.1,"door-local height");
            near(front.z(),-2.5,"door-local approach");
            var within=PortalAperture.eye(-fx*.75-fz*.2,.1,-fz*.75+fx*.2,fx,fz,DeadTunnelGeometry.MOUTH);
            near(within.z(),.25,"door-local crossing");
        }
        var rim=new double[][]{{-.4375,-.5},{.4375,-.5},{.4375,.4375},{-.4375,.4375}};
        for(int i=0;i<rim.length;i++){
            var corner=DeadTunnelGeometry.wall(0,8*i,true);
            near(corner.x(),rim[i][0],"collar corner x");
            near(corner.y(),rim[i][1],"collar corner y");
            near(corner.z(),0,"collar attachment");
        }
        var quad=List.of(p(-2,-2,2),p(2,-2,2),p(2,2,2),p(-2,2,2));
        for(double x:new double[]{-.64,0,.64}){
            var clipped=PortalAperture.clip(quad,x,.12,-1);
            require(clipped.size()>=3,"front approach lost all preview geometry");
            for(var v:clipped){
                double rimX=x+(v.x()-x)/(v.z()+1);
                double rimY=.12+(v.y()-.12)/(v.z()+1);
                require(rimX>=PortalAperture.LEFT-1e-7&&rimX<=PortalAperture.RIGHT+1e-7
                        &&rimY>=PortalAperture.BOTTOM-1e-7&&rimY<=PortalAperture.TOP+1e-7,
                        "clipped fragment exposed outside the frame");
            }
        }
        require(PortalAperture.clip(quad,.6,0,.1).isEmpty(),"external side view leaked geometry");
        require(PortalAperture.clip(quad,0,0,.1).size()==4,"crawl lost the next tunnel segment");
        require(PortalAperture.clip(quad,0,0,2.2).isEmpty(),"rear view leaked geometry");
        for(int side=0;side<DeadTunnelGeometry.SIDES;side++){
            var end=DeadTunnelGeometry.end(23,side,true);
            require(end.size()==3,"no cap segment");
            near(end.get(0).x(),DeadTunnelGeometry.wall(23,side,true).x(),"cap shares wall edge");
            require(end.get(1).shade()<.06&&end.get(1).z()>2.2,"end cap blocks transfer before passage depth");
        }
        require(DeadTunnelGeometry.WEBS.size()>1000&&DeadTunnelGeometry.WEBS.size()<8000,"web geometry vertex budget");
        for(var strand:DeadTunnelGeometry.WEBS)for(var point:strand){
            require(Math.hypot(point.x(),point.y())>.28,"web obstructs the center");
        }
        System.out.println("LITTLE_DOOR_APERTURE_OK "+checks);
    }
}
