package com.coraline.client.renderer;

import java.util.*;

/** Clips real geometry to rays through the door, not a screen-aligned rectangle. */
public final class PortalAperture {
    public record Vertex(double x,double y,double z,float u,float v,float shade){
        Vertex mix(Vertex other,double t){return new Vertex(x+(other.x-x)*t,y+(other.y-y)*t,z+(other.z-z)*t,(float)(u+(other.u-u)*t),(float)(v+(other.v-v)*t),(float)(shade+(other.shade-shade)*t));}
    }
    public static final double LEFT=-.4375,RIGHT=.4375,BOTTOM=-.5,TOP=.4375;
    public static List<Vertex> clip(List<Vertex> polygon,double cx,double cy,double cz){
        // Outside the opening, clip rays to the frame. Once the eye crosses
        // the frame, retain the tunnel ahead until the traversal handoff.
        if(polygon.isEmpty())return List.of();
        if(cz>=-.001){
            if(cx<LEFT||cx>RIGHT||cy<BOTTOM||cy>TOP)return List.of();
            var inside=new ArrayList<Vertex>();var previous=polygon.get(polygon.size()-1);double before=previous.z-cz-.01;
            for(var current:polygon){double after=current.z-cz-.01;if((before>=0)!=(after>=0))inside.add(previous.mix(current,before/(before-after)));if(after>=0)inside.add(current);previous=current;before=after;}
            return inside;
        }
        var result=polygon;
        for(int plane=0;plane<4;plane++){
            if(result.isEmpty())return result;var next=new ArrayList<Vertex>();var previous=result.get(result.size()-1);double before=distance(previous,plane,cx,cy,cz);
            for(var current:result){double after=distance(current,plane,cx,cy,cz);if((before>=0)!=(after>=0))next.add(previous.mix(current,before/(before-after)));if(after>=0)next.add(current);previous=current;before=after;}result=next;
        }return result;
    }
    private static double distance(Vertex p,int plane,double cx,double cy,double cz){return switch(plane){case 0->(cx-LEFT)*p.z-cz*p.x+LEFT*cz;case 1->-((cx-RIGHT)*p.z-cz*p.x+RIGHT*cz);case 2->(cy-BOTTOM)*p.z-cz*p.y+BOTTOM*cz;default->-((cy-TOP)*p.z-cz*p.y+TOP*cz);};}
    private PortalAperture(){}
}
