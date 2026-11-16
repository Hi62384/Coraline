package com.coraline.client.renderer;

import java.util.*;
import com.coraline.feast.CinematicPropEntity;
import com.coraline.feast.NauticalOpeningPlan;
import com.coraline.feast.NauticalOpeningShow;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Bounded code-native scenic meshes, not furniture blocks or extra mob actors.
 * Physical blocking and the heard cue are server-owned. Reference/Blender and
 * native visual QA are still required before calling the rebuilt act accepted.
 */
public final class NauticalStageRenderer {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private record Mesh(float[] points,int color){
        Mesh(double[][] vertices,int[][] faces,int color){this(bake(vertices,faces),color);}
    }
    private static final List<Mesh> BOAT=boat(),CLAM=clam(),HEAD=fish(),LOWER=lowerFish(),HOOK=hook();
    private static final List<List<Mesh>> WAVES=List.of(wave(0),wave(1),wave(2));
    private static NauticalOpeningPlan cached;
    private static int cachedCue=-1,cachedDuration=-1;

    public static boolean render(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light,float partial){
        int index=e.cue()-NauticalOpeningShow.SET_CUE_BASE;
        if(e.kind()!=CinematicPropEntity.OCEAN_SET||index<0||index>=14)return false;
        char cue=(char)('A'+index);int duration=(int)e.value();if(duration<=0)return true;
        if(cached==null||cachedCue!=index||cachedDuration!=duration){
            var lengths=new HashMap<String,Integer>();for(char c='A';c<='N';c++)lengths.put(NauticalOpeningPlan.sound(c),100);
            lengths.put(NauticalOpeningPlan.sound(cue),duration);
            try{cached=new NauticalOpeningPlan(lengths);}catch(IllegalArgumentException invalid){return true;}
            cachedCue=index;cachedDuration=duration;
        }
        double tick=Math.min(duration,e.visualTime(partial)),u=tick/duration;
        var f=cached.frame(cue,tick);var out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        // Shared slotted stage wagon: actor feet remain supported while the
        // boat and clam scenery swap on separate upstage tracks.
        quad(p,out,light,new double[][]{{-15,1.6,-2},{15,1.6,-2},{15,1.6,1.9},{-15,1.6,1.9}},0x956E46);
        quad(p,out,light,new double[][]{{-15,1,1.9},{15,1,1.9},{15,1.6,1.9},{-15,1.6,1.9}},0x59422E);
        // Painted flat background panels have real independent falling hinges.
        for(int panel=0;panel<5;panel++){
            p.pushPose();p.translate((panel-2)*8,0,-4.5);
            double fall=Math.max(0,Math.min(1,(f.collapse()-.13*panel)/.48));
            p.mulPose(Axis.XP.rotationDegrees((float)(-fall*(58+panel*4))));
            quad(p,out,light,new double[][]{{-4,0,0},{4,0,0},{4,12,0},{-4,12,0}},0x98B3A8);
            quad(p,out,light,new double[][]{{-4,0,.015},{4,0,.015},{4,3,.015},{-4,3,.015}},0x365D61);
            p.popPose();
        }
        double swap=cue>='G'&&cue<='L'?1-f.boatScene():cue=='M'?1-f.boatScene():0;
        p.pushPose();p.translate(cue>='G'?swap*25:0,cue=='E'?f.spinkLift():cue<'E'?0:cue<'G'?-9:0,-.6);
        p.mulPose(Axis.ZP.rotationDegrees((float)(f.collapse()*18)));
        p.mulPose(Axis.XP.rotationDegrees((float)(f.collapse()*-9)));
        draw(BOAT,p,out,light);p.popPose();
        p.pushPose();p.translate(cue>='G'?-f.boatScene()*25:0,cue=='E'?f.clamLift():cue<'E'?-9:0,0);
        p.mulPose(Axis.ZP.rotationDegrees((float)(-f.collapse()*10)));
        draw(CLAM,p,out,light);p.popPose();
        if(cue<='D'){
            if(cue<'C'||cue=='C'&&f.fishHeadLift()<1.4)draw(LOWER,p,out,light);
            p.pushPose();p.translate(0,f.fishHeadLift(),0);draw(HEAD,p,out,light);p.popPose();
        }
        if(cue>='B'&&cue<='D'){
            double y=cue=='B'?10.7-8*u:2.7+f.fishHeadLift();
            p.pushPose();p.translate(0,y,.98);draw(HOOK,p,out,light);p.popPose();
            line(p,out,light,0,14,.98,0,y+.8,.98,.022,0x4D3E2B);
        }
        for(int layer=0;layer<3;layer++){
            p.pushPose();p.translate(Math.sin(tick*.06+layer)*.28,Math.sin(tick*.08+layer)*.05,2.5+layer*1.25);
            p.mulPose(Axis.ZP.rotationDegrees((float)((layer-1)*f.collapse()*11)));
            draw(WAVES.get(layer),p,out,light);p.popPose();
        }
        // Jammed rails and loose battens visibly rotate, not explosion particles.
        if(cue>='M')for(int side:new int[]{-1,1}){
            p.pushPose();p.translate(side*11,1.6,-.9);p.mulPose(Axis.ZP.rotationDegrees((float)(side*f.collapse()*48)));
            line(p,out,light,0,0,0,0,7,0,.055,0x5C4330);p.popPose();
        }
        // Real opaque wing flats conceal the track endpoints. Performers walk
        // behind these surfaces rather than toggling invisible during pushes.
        for(int side:new int[]{-1,1}){
            double x0=side*12.4,x1=side*26;
            quad(p,out,light,new double[][]{{x0,1.6,2.1},{x1,1.6,2.1},{x1,14,2.1},{x0,14,2.1}},0x29252A);
            line(p,out,light,x0,1.6,2.12,x0,14,2.12,.07,0xB38D4A);
        }
        return true;
    }

    private static List<Mesh> boat(){
        var result=new ArrayList<Mesh>();int sections=20,bands=10;double[][] vertices=new double[(sections+1)*(bands+1)][3];
        for(int i=0;i<=sections;i++)for(int j=0;j<=bands;j++){
            double x=-10.5+21*i/sections,t=Math.PI*j/bands,width=.18+2.5*Math.sin(Math.PI*i/sections);
            vertices[i*(bands+1)+j]=new double[]{x,1.6-1.9*Math.sin(t),Math.cos(t)*width};
        }
        for(int i=0;i<sections;i++)for(int j=0;j<bands;j++)result.add(new Mesh(vertices,new int[][]{{i*(bands+1)+j,(i+1)*(bands+1)+j,(i+1)*(bands+1)+j+1,i*(bands+1)+j+1}},(i+j)%3==0?0x80583B:0x916344));
        result.add(ellipse(0,1.6,0,10.5,2.6,0xB99469));
        for(int i=0;i<21;i++){
            double x=-10+i,width=.18+2.5*Math.sin(Math.PI*(x+10.5)/21);
            result.add(tube(x,1.7,-width,x,1.7,width,.035,0x5C3B28));
        }
        result.add(tube(5,1.6,-1.2,5,12,-1.2,.10,0x86603C));
        // Curved brown sail: a faceted cloth surface with separate bent ribs.
        for(int i=0;i<16;i++)for(int j=0;j<8;j++){
            double[][] v={sail(i/16.0,j/8.0),sail((i+1)/16.0,j/8.0),sail((i+1)/16.0,(j+1)/8.0),sail(i/16.0,(j+1)/8.0)};
            result.add(new Mesh(v,new int[][]{{0,1,2,3}},new int[]{0x7D573F,0x916B4D,0xA27653,0x8B6348}[(i+j)%4]));
        }
        for(int i=0;i<=8;i++)for(int j=0;j<8;j++){
            var a=sail(i/8.0,j/8.0);var b=sail(i/8.0,(j+1)/8.0);
            result.add(tube(a[0],a[1],a[2]+.015,b[0],b[1],b[2]+.015,.025,0xBA9369));
        }
        for(int side:new int[]{-1,1})result.add(tube(side*10,1.65,-1,5,11.8,-1.2,.025,0xC7AF82));
        // Relief-painted sailors belong to the boat mesh, not a crowd of AI.
        int[] shirts={0xB69A73,0x6B8580,0xC0A888,0x8D6755,0x8D9C8E,0xAF8C63};
        for(int i=0;i<6;i++){
            double x=-8+i*3.1,y=2.1+(i%2)*.12;
            result.add(oval(x,y+.25,-.3,.7,.85,.12,shirts[i]));
            result.add(oval(x,y+1.35,-.18,.38,.48,.12,0xB18C68));
            result.add(tube(x-.65,y+.45,-.12,x-1.0,y+.95,-.08,.15,0xBD9670));
            result.add(tube(x+.65,y+.45,-.12,x+1.1,y+.05,0,.15,0xBD9670));
            result.add(tube(x-1.1,1.6,.15,x+1.3,3.2,.15,.045,0x705039));
            result.add(oval(x,y+1.62,-.12,.42,.20,.14,0x65503B));
        }
        return List.copyOf(result);
    }
    private static double[] sail(double u,double v){double x=-10+20*u;return new double[]{x,11.8-2.7*Math.pow(1-u,2)-v*(3.2+3.7*Math.sin(u*Math.PI)), -1.5+1.2*Math.sin(u*Math.PI)*Math.sin(v*Math.PI)};}
    private static List<Mesh> clam(){
        var result=new ArrayList<Mesh>();
        for(int i=0;i<22;i++){
            double a=Math.PI*i/22,b=Math.PI*(i+1)/22;
            int color=new int[]{0xD6C59B,0xEEE2BD,0xB59872,0xD7AC8E}[i%4];
            result.add(new Mesh(new double[][]{{0,1.2,-.5},{Math.cos(a)*3.5,1.2+Math.sin(a)*4.5,-.8},{Math.cos(b)*3.5,1.2+Math.sin(b)*4.5,-.8}},new int[][]{{0,1,2}},color));
            result.add(tube(0,1.2,-.45,Math.cos(a)*3.5,1.2+Math.sin(a)*4.5,-.74,.035,0xC38E73));
            double c=Math.PI*2*i/22,d=Math.PI*2*(i+1)/22;
            result.add(new Mesh(new double[][]{{0,1.55,0},{Math.cos(c)*3.0,.65,Math.sin(c)*1.8},{Math.cos(d)*3.0,.65,Math.sin(d)*1.8}},new int[][]{{0,1,2}},color));
        }
        result.add(ellipse(0,1.6,0,.8,.7,0xD6CBA7));
        return List.copyOf(result);
    }
    private static List<Mesh> fish(){
        var result=new ArrayList<Mesh>();result.add(oval(0,3.25,0,.88,1.12,.62,0x638479));
        result.add(oval(0,2.9,.52,.55,.16,.16,0x172D2A));
        for(int side:new int[]{-1,1}){
            result.add(ring(side*.40,3.65,.58,.24,.15,0xBBAF76));
            result.add(ring(side*.40,3.65,.603,.15,.08,0x122625));
            for(int i=0;i<4;i++){double a=i*Math.PI/2;result.add(ring(side*.40+Math.cos(a)*.10,3.65+Math.sin(a)*.10,.61,.035,.019,0x657D66));}
        }
        result.add(ring(0,2.77,.76,.16,.115,0xC9B376));
        result.add(new Mesh(new double[][]{{-.65,3.7,-.1},{-1.18,3.95,-.1},{-.7,3.0,-.1}},new int[][]{{0,1,2}},0x87996D));
        return List.copyOf(result);
    }
    private static List<Mesh> lowerFish(){
        var result=new ArrayList<Mesh>();result.add(oval(0,2.15,0,.65,.65,.48,0x63836B));
        for(int row=0;row<6;row++)for(int col=0;col<7;col++){
            double x=(col-3)*.16+(row%2)*.07,y=1.65+row*.14;
            result.add(oval(x,y,.43,.11,.10,.018,(row+col)%3==0?0xB3BA82:0x779872));
        }
        result.add(new Mesh(new double[][]{{-.4,1.8,0},{-1.5,1.9,0},{-1.8,2.6,0},{-.8,2.1,0}},new int[][]{{0,1,2,3}},0x789E79));
        return List.copyOf(result);
    }
    private static List<Mesh> hook(){
        var result=new ArrayList<Mesh>();result.add(tube(0,.8,0,0,.15,0,.035,0xC9BF98));
        for(int i=0;i<12;i++){double a=Math.PI*i/12,b=Math.PI*(i+1)/12;result.add(tube(.18-.18*Math.cos(a),.15-.18*Math.sin(a),0,.18-.18*Math.cos(b),.15-.18*Math.sin(b),0,.035,0xC9BF98));}
        return List.copyOf(result);
    }
    private static List<Mesh> wave(int layer){
        var result=new ArrayList<Mesh>();for(int i=0;i<32;i++){
            double x=-21+i*1.32,h=.7+Math.sin(i*.62+layer)*.20;
            result.add(new Mesh(new double[][]{{x,0,0},{x+1.32,0,0},{x+1.32,h,.12},{x+.75,h+.48,-.16},{x,h+.13,.12}},new int[][]{{0,1,2,3,4}},new int[]{0x487E79,0x729A6C,0x88A07B}[layer]));
        }return List.copyOf(result);
    }
    private static Mesh oval(double x,double y,double z,double rx,double ry,double rz,int color){
        int rows=8,columns=12;var v=new double[(rows+1)*columns][3];var faces=new ArrayList<int[]>();
        for(int row=0;row<=rows;row++)for(int col=0;col<columns;col++){double a=Math.PI*row/rows,b=col*Math.PI*2/columns;v[row*columns+col]=new double[]{x+rx*Math.sin(a)*Math.cos(b),y+ry*Math.cos(a),z+rz*Math.sin(a)*Math.sin(b)};}
        for(int row=0;row<rows;row++)for(int col=0;col<columns;col++)faces.add(new int[]{row*columns+col,row*columns+(col+1)%columns,(row+1)*columns+(col+1)%columns,(row+1)*columns+col});
        return new Mesh(v,faces.toArray(int[][]::new),color);
    }
    private static Mesh ellipse(double x,double y,double z,double rx,double rz,int color){var v=new double[25][3];v[0]=new double[]{x,y,z};var faces=new int[24][3];for(int i=0;i<24;i++){double a=i*Math.PI/12;v[i+1]=new double[]{x+rx*Math.cos(a),y,z+rz*Math.sin(a)};faces[i]=new int[]{0,i+1,(i+1)%24+1};}return new Mesh(v,faces,color);}
    private static Mesh ring(double x,double y,double z,double outer,double inner,int color){var v=new double[48][3];var faces=new int[24][4];for(int i=0;i<24;i++){double a=i*Math.PI/12;v[i]=new double[]{x+outer*Math.cos(a),y+outer*Math.sin(a),z};v[i+24]=new double[]{x+inner*Math.cos(a),y+inner*Math.sin(a),z};faces[i]=new int[]{i,(i+1)%24,(i+1)%24+24,i+24};}return new Mesh(v,faces,color);}
    private static Mesh tube(double x,double y,double z,double tx,double ty,double tz,double radius,int color){
        var axis=new org.joml.Vector3d(tx-x,ty-y,tz-z);if(axis.lengthSquared()<1e-12)throw new IllegalArgumentException("Empty scenic rod");axis.normalize();
        var across=axis.cross(Math.abs(axis.y)<.9?new org.joml.Vector3d(0,1,0):new org.joml.Vector3d(1,0,0),new org.joml.Vector3d()).normalize().mul(radius);
        var up=axis.cross(across,new org.joml.Vector3d()).normalize().mul(radius);var v=new double[12][3];var faces=new int[6][4];
        for(int end=0;end<2;end++)for(int i=0;i<6;i++){double a=i*Math.PI/3;v[end*6+i]=new double[]{(end==0?x:tx)+across.x*Math.cos(a)+up.x*Math.sin(a),(end==0?y:ty)+across.y*Math.cos(a)+up.y*Math.sin(a),(end==0?z:tz)+across.z*Math.cos(a)+up.z*Math.sin(a)};}
        for(int i=0;i<6;i++)faces[i]=new int[]{i,(i+1)%6,(i+1)%6+6,i+6};return new Mesh(v,faces,color);
    }
    private static void line(PoseStack p,VertexConsumer out,int light,double x,double y,double z,double tx,double ty,double tz,double radius,int color){draw(List.of(tube(x,y,z,tx,ty,tz,radius,color)),p,out,light);}
    /** Pre-bake triangulation and flat normals once, not per frame. */
    private static float[] bake(double[][] vertices,int[][] faces){
        int capacity=0;for(var face:faces)capacity+=(face.length-2)*24;var points=new float[capacity];int used=0;
        for(var face:faces)for(int i=1;i<face.length-1;i++){
            var a=vertices[face[0]];var b=vertices[face[i]];var c=vertices[face[i+1]];
            double x=b[0]-a[0],y=b[1]-a[1],z=b[2]-a[2],tx=c[0]-a[0],ty=c[1]-a[1],tz=c[2]-a[2];
            double nx=y*tz-z*ty,ny=z*tx-x*tz,nz=x*ty-y*tx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);if(length<1e-8)continue;
            for(int j:new int[]{face[0],face[i],face[i+1],face[i+1]}){
                for(double coordinate:vertices[j])points[used++]=(float)coordinate;
                points[used++]=(float)(nx/length);points[used++]=(float)(ny/length);points[used++]=(float)(nz/length);
            }
        }return Arrays.copyOf(points,used);
    }
    private static void draw(List<Mesh> meshes,PoseStack p,VertexConsumer out,int light){
        var matrix=p.last().pose();var normal=p.last().normal();
        for(var mesh:meshes){var points=mesh.points;int color=mesh.color;for(int i=0;i<points.length;i+=6)
            out.vertex(matrix,points[i],points[i+1],points[i+2]).color(color>>16&255,color>>8&255,color&255,255).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,points[i+3],points[i+4],points[i+5]).endVertex();}
    }
    /** CPU-only validation entry: no Minecraft instance, GL, sound or world. */
    public static int validateGeometry(){
        int vertices=0;var all=new ArrayList<Mesh>();all.addAll(BOAT);all.addAll(CLAM);all.addAll(HEAD);all.addAll(LOWER);all.addAll(HOOK);for(var wave:WAVES)all.addAll(wave);
        for(var mesh:all){if(mesh.points.length==0||mesh.points.length%24!=0)throw new IllegalStateException("Invalid scenic primitive");
            for(int i=0;i<mesh.points.length;i+=6){for(int j=0;j<6;j++)if(!Float.isFinite(mesh.points[i+j]))throw new IllegalStateException("Non-finite scenic mesh");
                double length=0;for(int j=3;j<6;j++)length+=mesh.points[i+j]*mesh.points[i+j];if(Math.abs(length-1)>1e-5)throw new IllegalStateException("Invalid scenic normal");
                for(int j=0;j<3;j++)if(Math.abs(mesh.points[i+j])>25)throw new IllegalStateException("Scenic mesh outside bounded stage");vertices++;}
        }if(vertices>100000)throw new IllegalStateException("Unbounded scenic vertex budget");return vertices;
    }
    private static void quad(PoseStack p,VertexConsumer out,int light,double[][] v,int color){
        var a=new org.joml.Vector3d(v[1]).sub(new org.joml.Vector3d(v[0]));var b=new org.joml.Vector3d(v[2]).sub(new org.joml.Vector3d(v[0]));a.cross(b);
        if(a.lengthSquared()<1e-16)return;a.normalize();for(var point:v)out.vertex(p.last().pose(),(float)point[0],(float)point[1],(float)point[2]).color(color>>16&255,color>>8&255,color&255,255).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),(float)a.x,(float)a.y,(float)a.z).endVertex();
    }
    private NauticalStageRenderer(){}
}
