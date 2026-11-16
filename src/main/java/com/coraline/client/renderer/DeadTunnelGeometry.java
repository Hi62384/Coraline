package com.coraline.client.renderer;

/** Shared, stable geometry for the doorway view and the playable crawl. */
final class DeadTunnelGeometry {
    static PortalAperture.Vertex wall(int ring,int side){
        double z=ring*.125,a=side*Math.PI/24;
        double fold=z*5.2+.5*Math.sin(a*3+z*.7)+.25*Math.sin(z*1.3);
        double radius=(.46+Math.sin(fold)*.025+Math.sin(a*5+z)*.013)*.87;
        return new PortalAperture.Vertex(Math.cos(a)*radius,Math.sin(a)*radius,z-.1875,side/48F+(float)z*.025F,(float)z/4F,(float)(.78+.22*Math.cos(fold-.7)));
    }
    static final java.util.List<PortalAperture.Vertex[]> WEBS=webs();
    private static java.util.List<PortalAperture.Vertex[]> webs(){
        var random=new java.util.Random(0xC0B0EBL);
        var result=new java.util.ArrayList<PortalAperture.Vertex[]>();
        for(int i=0;i<220;i++){
            double a=random.nextDouble()*Math.PI*2,z=.22+random.nextDouble()*21.3;
            double width=.09+random.nextDouble()*.23,length=.14+random.nextDouble()*.48;
            double twist=random.nextDouble()*Math.PI,rad=.32+random.nextDouble()*.045;
            var corners=new PortalAperture.Vertex[4];
            for(int j=0;j<4;j++){
                double u=(j==0||j==3?-.5:.5)*width,v=(j<2?-.5:.5)*length;
                double tangent=u*Math.cos(twist)-v*Math.sin(twist),depth=u*Math.sin(twist)+v*Math.cos(twist);
                corners[j]=new PortalAperture.Vertex(Math.cos(a)*rad-Math.sin(a)*tangent,Math.sin(a)*rad+Math.cos(a)*tangent,z+depth,j==0||j==3?0:1,j<2?0:1,.52F+random.nextFloat()*.18F);
            }
            result.add(corners);
        }
        return java.util.List.copyOf(result);
    }
    private DeadTunnelGeometry(){}
}
