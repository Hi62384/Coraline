import com.coraline.client.renderer.PortalAperture;
import java.util.*;

public final class PortalApertureSmoke {
    public static void main(String[] args){
        var random=new Random(1665);int checks=0;
        for(int i=0;i<12000;i++){
            double cx=random.nextDouble()*20-10,cy=random.nextDouble()*8-4,cz=-.05-random.nextDouble()*20,z=.01+random.nextDouble()*3.3;
            var quad=List.of(new PortalAperture.Vertex(-.5,-.5,z,0,0,1),new PortalAperture.Vertex(.5,-.5,z,1,0,1),new PortalAperture.Vertex(.5,.5,z,1,1,1),new PortalAperture.Vertex(-.5,.5,z,0,1,1));
            for(var v:PortalAperture.clip(quad,cx,cy,cz)){double x=(cx*v.z()-cz*v.x())/(v.z()-cz),y=(cy*v.z()-cz*v.y())/(v.z()-cz);if(x<PortalAperture.LEFT-1e-8||x>PortalAperture.RIGHT+1e-8||y<PortalAperture.BOTTOM-1e-8||y>PortalAperture.TOP+1e-8)throw new AssertionError("Tunnel leaks beyond aperture");checks++;}
        }
        var quad=List.of(new PortalAperture.Vertex(-.4,-.4,3,0,0,1),new PortalAperture.Vertex(.4,-.4,3,1,0,1),new PortalAperture.Vertex(.4,.4,3,1,1,1),new PortalAperture.Vertex(-.4,.4,3,0,1,1));
        if(!PortalAperture.clip(quad,2,0,1).isEmpty()||!PortalAperture.clip(quad,0,0,4).isEmpty())throw new AssertionError("Exterior/back view reveals the tunnel");
        if(PortalAperture.clip(quad,0,0,-3).size()!=4)throw new AssertionError("Frontal view was lost");
        for(double crossing:new double[]{-.001,0,.001,.2,.5}){
            var visible=PortalAperture.clip(quad,0,0,crossing);
            if(visible.size()!=4)throw new AssertionError("Tunnel vanishes crossing doorway at "+crossing);
            for(var v:visible)if(v.z()<=crossing)throw new AssertionError("Geometry behind eye survived");
        }
        if(!PortalAperture.clip(List.of(),0,0,0).isEmpty())throw new AssertionError("Empty geometry");
        System.out.println("PORTAL_APERTURE_SMOKE_OK: "+checks+" clipped vertex projections; 12000 viewpoints; exterior/back rejection and frontal visibility");
    }
}
