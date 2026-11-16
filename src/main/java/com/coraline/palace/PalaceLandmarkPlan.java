package com.coraline.palace;

/** Small streamed secondary landmarks. The Palace itself reuses the existing
 * furnished Victorian master; no second generic house/bitmap export is made.
 */
public final class PalaceLandmarkPlan {
    public enum Material { KEEP,AIR,STONE,BRICK,PINK,CREAM,DARK,PLANKS,GLASS,FENCE,CRATE,LIGHT,STAIR_N,STAIR_S,COUNTER,CHAIR,ROPE }
    public record Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ){
        public int size(){return (maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);}
        public int[] cell(int index){if(index<0||index>=size())throw new IllegalArgumentException("Outside landmark cursor");int depth=maxZ-minZ+1,width=maxX-minX+1;return new int[]{minX+index%width,minY+index/(width*depth),minZ+(index/width)%depth};}
    }
    public static final Bounds COMPANY=new Bounds(-17,-3,-17,23,15,16),WELL=new Bounds(-8,-32,-8,8,6,8);
    public static Material company(int x,int y,int z){
        boolean building=Math.abs(x)<=11&&Math.abs(z)<=9;
        if(y<0)return building||x>=12&&x<=22&&Math.abs(z)<=12?Material.STONE:Material.KEEP;
        if(building){
            if(y==0||y==5)return Material.PLANKS;
            if(y>=10&&y<=14&&Math.abs(x)<=14-(y-10)*3&&Math.abs(z)<=11)return Material.DARK;
            boolean wall=Math.abs(x)==11||Math.abs(z)==9;
            if(y>0&&y<10&&wall){
                if(z==-9&&Math.abs(x)<=2&&y<=3||x==11&&z>=-4&&z<=4&&y<=4)return Material.AIR;
                if(y>=2&&y<=3&&(Math.abs(x)%6<=1&&Math.abs(z)==9||Math.abs(z)%6<=1&&Math.abs(x)==11))return Material.GLASS;
                return y==1||y==4||y==9?Material.CREAM:Material.BRICK;
            }
            if(y==1&&z>=5&&Math.abs(x)>=5&&Math.abs(x)<=9)return Material.CRATE;
            if(y==1&&x>=-6&&x<=-3&&z==-4)return Material.COUNTER;
            if(y==1&&x==-5&&z==-6)return Material.CHAIR;
            if(y>=1&&y<=3&&x>=-9&&x<=-7&&z==3)return Material.CRATE;
            if(y>=2&&y<=4&&x==6&&z==-7)return Material.ROPE;
            if(y==3&&x>=5&&x<=9&&z==6)return Material.PLANKS;
            if(y==6&&z>=5&&x%3==0)return Material.CRATE;
            if(y==4&&z==0&&Math.abs(x)==5)return Material.LIGHT;
            if(x>=-9&&x<=-7&&z>=-5&&z<=3&&y==(z+5)/2+1)return Material.STAIR_S;
            return Material.AIR;
        }
        if(x>=12&&x<=22&&Math.abs(z)<=12&&y==0)return Material.PLANKS;
        if(x==22&&z%5==0&&Math.abs(z)<=10&&y>=1&&y<=4)return Material.FENCE;
        if(z==13&&x>=-10&&x<=7&&y==1&&x%3!=0)return Material.CRATE;
        if(y>=0&&y<=14)return Material.AIR;
        return Material.KEEP;
    }
    public static Material well(int x,int y,int z){
        double r=Math.hypot(x,z);
        if(y<0){if(r<1.8)return y==-32?Material.STONE:Material.AIR;if(r<3.2)return Material.STONE;return Material.KEEP;}
        if(y<=1&&r>=1.8&&r<3.2)return Material.STONE;
        if(y==0&&r<1.8)return Material.AIR;
        // Broken weathered railing leaves an actual reachable hanging ledge.
        if(y==2&&r>=2.3&&r<3.2&&z>=0)return Material.FENCE;
        if(y==0&&r>=3.2&&r<=7.5)return Material.STONE;
        return Material.AIR;
    }
    private PalaceLandmarkPlan(){}
}
