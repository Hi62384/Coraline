package com.coraline.feast;

/** Pure carry-footstep math, also checked without starting a Minecraft VM. */
final class CarryStepGeometry {
    static final double MAX_STEP=.24;
    record Point(double x,double y,double z){}
    record FloorBox(double minX,double minY,double minZ,double maxX,double maxY,double maxZ){}
    static Point horizontal(Point from,Point target){
        double dx=target.x-from.x,dz=target.z-from.z,length=Math.hypot(dx,dz),scale=length<.000001?0:Math.min(MAX_STEP,length)/length;
        return new Point(from.x+dx*scale,from.y,from.z+dz*scale);
    }
    static Point floor(Point next,int baseY,Iterable<FloorBox> shapes){
        double height=baseY;
        for(var box:shapes){
            if(box.maxX<=next.x-.3||box.minX>=next.x+.3||box.maxZ<=next.z-.3||box.minZ>=next.z+.3)continue;
            double covering=box.maxY-baseY;if(covering>.125)return null;
            height=Math.max(height,box.maxY);
        }
        return new Point(next.x,height,next.z);
    }
    private CarryStepGeometry(){}
}
