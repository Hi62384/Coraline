package com.coraline.palace;

/** Deterministic bounded valley, overlook and WALKABLE trail. Values are shared
 * by real terrain placement and the later physical encounter/transport driver.
 * This class does not load a chunk, modify time/weather or install a global
 * biome sampler. Native terrain/LOS/navigation review is still required.
 */
public final class PalaceTerrainPlan {
    public static final int EXTENT=384;
    public record Sample(int surface,double weight,boolean trail,boolean clearing){}
    private static double smooth(double x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    public static long hash(long seed,int x,int z){long v=seed^((long)x*0x9e3779b97f4a7c15L)^((long)z*0xc2b2ae3d27d4eb4fL);v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;return v^(v>>>31);}
    public static double unit(long seed,int x,int z){return (hash(seed,x,z)>>>11)*0x1.0p-53;}
    /** Anchor is the Palace foundation elevation, not world zero. */
    public static Sample sample(long seed,PalaceSitePlan.Point palace,PalaceSitePlan.Point well,int anchor,int x,int z,int natural){
        if(palace==null||well==null||anchor<64||anchor>150||natural< -64||natural>319)throw new IllegalArgumentException("Unsupported valley profile");
        double dx=(double)x-palace.x(),dz=(double)z-palace.z(),r=Math.hypot(dx,dz);
        double weight=PalaceSitePlan.regionWeight(seed,palace,x,z);
        double wellDistance=Math.hypot((double)x-well.x(),(double)z-well.z());
        weight=Math.max(weight,1-smooth((wellDistance-10)/16));
        if(weight==0)return new Sample(natural,0,false,false);
        double wx=(double)well.x()-palace.x(),wz=(double)well.z()-palace.z(),length=Math.hypot(wx,wz);
        if(length<220||length>450)throw new IllegalArgumentException("Well outside scenic valley band");
        double u=Math.max(0,Math.min(1,(dx*wx+dz*wz)/(length*length))),side=(dx*(-wz)+dz*wx)/length;
        double winding=4*Math.sin(u*Math.PI*3)*Math.sin(u*Math.PI);
        double routeDistance=Math.abs(side-winding);
        if(u>0&&u<1)weight=Math.max(weight,1-smooth((routeDistance-3)/7));
        double uneven=5*Math.sin(dx*.041)*Math.cos(dz*.033)+2*Math.sin((dx+dz)*.097);
        double ring=smooth((r-80)/180),ridge=(17+42*Math.pow(Math.sin(Math.atan2(dz,dx)*2.5+.7),2))*ring;
        double shaped=anchor-7+ridge+uneven*smooth((r-58)/35);
        // Distinct exposed rock peaks beyond the inhabited valley floor.
        shaped+=36*Math.exp(-((dx+220)*(dx+220)+(dz-115)*(dz-115))/5200);
        shaped+=49*Math.exp(-((dx-160)*(dx-160)+(dz+210)*(dz+210))/4700);
        double lineHeight=anchor+36*u;
        // A wide open sightline keeps intervening hills BELOW the house roof
        // sightline. The trail uses the same smooth 36 m rise over >220 m.
        if(u>0&&u<1&&Math.abs(side)<16)shaped=Math.min(shaped,lineHeight-3);
        if(u>0&&u<1){double pathBlend=smooth((routeDistance-3)/7);shaped=lineHeight+(shaped-lineHeight)*pathBlend;}
        boolean clearing=r<61||Math.hypot((double)x-well.x(),(double)z-well.z())<13;
        shaped=anchor-1+(shaped-(anchor-1))*smooth((r-55)/35);
        shaped=anchor+35+(shaped-(anchor+35))*smooth((wellDistance-10)/16);
        int surface=(int)Math.round(natural+(shaped-natural)*weight);
        return new Sample(Math.max(-64,Math.min(319,surface)),weight,routeDistance<=2.6&&u>0&&u<1,clearing);
    }
    public static boolean vegetation(long seed,PalaceSitePlan.Point palace,PalaceSitePlan.Point well,int x,int z){
        double r=Math.hypot((double)x-palace.x(),(double)z-palace.z());
        if(r<67||Math.hypot((double)x-well.x(),(double)z-well.z())<18)return false;
        double wx=(double)well.x()-palace.x(),wz=(double)well.z()-palace.z(),len=Math.hypot(wx,wz);
        double u=((x-palace.x())*wx+(z-palace.z())*wz)/(len*len),side=((x-palace.x())*(-wz)+(z-palace.z())*wx)/len;
        return !(u>0&&u<1&&Math.abs(side)<18)&&unit(seed,x,z)<PalaceSitePlan.regionWeight(seed,palace,x,z)*.5;
    }
    private PalaceTerrainPlan(){}
}
