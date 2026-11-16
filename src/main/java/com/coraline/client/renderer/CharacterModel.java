package com.coraline.client.renderer;

import com.google.gson.*;
import com.coraline.entity.CharacterEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Cached indexed surfaces and articulated transforms, using native entity lighting. */
public final class CharacterModel extends EntityModel<CharacterEntity> {
    private record Point(float x,float y,float z,float u,float v,float nx,float ny,float nz){}
    private record Surface(String bone,String part,float r,float g,float b,List<Point> points,List<Vector3f> bounds){}
    private record Rig(Map<String,float[]> pivots,Map<String,String> parents,List<Surface> surfaces){}
    private static final Map<String,Rig> CACHE=new HashMap<>();
    private static int cacheRevision;
    private final Map<String,ModelPart> bones=new LinkedHashMap<>();
    private final String id;
    private final int rescueActress;
    private Rig rig;
    private record RigBlend(String sourceBone,Surface target,List<Point> from,Vector3f color,List<Vector3f> targetColors){}
    private List<RigBlend> rigBlend;private String rigBlendSource;private Map<String,Vector3f[]> cageRanges;
    private ResourceLocation morphSkin;private float[] motherMorph;
    private int revision;
    private com.coraline.wybie.WybieVisitorEntity visitor;
    private float animationAge;
    private boolean catBiting;
    private boolean storyHeadOnly,storyWybie,storyRealWybie,storyBlind,wellWybie;
    public void storyBlind(boolean value){storyBlind=value;}
    public void storyHeadOnly(boolean value){storyHeadOnly=value;}
    public void storyGrip(){var arm=bones.get("right_arm");var fore=bones.get("right_forearm");if(arm!=null)arm.xRot=-1.45F;if(fore!=null)fore.xRot=-.55F;}
    public void rescuePose(int gesture,float time){
        var arm=bones.get("right_arm");var fore=bones.get("right_forearm");var left=bones.get("left_arm");var leftFore=bones.get("left_forearm");
        if(arm==null||fore==null||left==null||leftFore==null||gesture<60||gesture>68)return;
        // Presentation proxies never tick on the ground. Do not inherit their
        // airborne inward-arm pose: these long limbs crossed through each other.
        arm.xRot=left.xRot=-.08F;arm.yRot=left.yRot=0;
        arm.zRot=.10F;left.zRot=-.10F;
        fore.xRot=leftFore.xRot=-.22F;fore.yRot=leftFore.yRot=fore.zRot=leftFore.zRot=0;
        switch(gesture){
            case 60->{arm.xRot=-1.15F;arm.yRot=Mth.sin(time*.22F)*.28F;fore.xRot=-.45F;}
            case 61->{arm.xRot=-1.4F;fore.xRot=-.3F;}
            case 62->{arm.xRot=left.xRot=-1.25F;fore.xRot=leftFore.xRot=-.35F;}
            case 63->{
                // Fold the elbow to place the palm on the right button eye.
                // Calibrated against the evil Mother rig's shoulder/elbow/eye pivots.
                arm.xRot=-2.111F;arm.zRot=.363F;fore.xRot=-1.622F;
            }
            case 64->{arm.xRot=-.85F;fore.xRot=-1.35F;arm.yRot=-.20F;arm.zRot=.28F;}
            case 68->{arm.xRot=-1.25F;fore.xRot=-.25F;arm.yRot=-.12F;fore.zRot=Mth.sin(time*.18F)*.28F;}
            case 65->{arm.xRot=-1.95F;fore.xRot=-1.1F;bones.get("head").xRot=-.15F;}
            // One open conversational hand; the other hangs naturally beside
            // the hip instead of meeting it in a symmetrical crossed-arm X.
            case 66->{arm.xRot=-.35F;fore.xRot=-.20F;arm.zRot=.14F;left.xRot=-.10F;leftFore.xRot=-.12F;bones.get("head").xRot=.08F;}
            case 67->{arm.xRot=left.xRot=-.18F;arm.zRot=.40F;left.zRot=-.40F;fore.xRot=leftFore.xRot=-.25F;arm.yRot=-.10F;left.yRot=.10F;}
            default->{}
        }
    }
    private com.coraline.bobinsky.BobinskyVisitorEntity bobVisitor;
    public CharacterModel(String id){this(id,0);}
    public CharacterModel(String id,int rescueActress){this.id=id;this.rescueActress=rescueActress;refresh();}
    private void refresh(){
        rig=CACHE.computeIfAbsent(id,CharacterModel::load);
        if(rescueActress>0)rig=actressRig(rig,rescueActress);
        bones.clear();revision=cacheRevision;motherMorph=null;morphSkin=null;rigBlend=null;rigBlendSource=null;cageRanges=null;
        rig.pivots.forEach((name,p)->{var bone=new ModelPart(List.of(),Map.of());bone.setPos(p[0],p[1],p[2]);bones.put(name,bone);});
    }
    public static void clearCache(){CACHE.clear();MotherMorphGeometry.clear();InsectFurnitureMesh.clear();cacheRevision++;}
    /** Split the existing two-headed evil cocoon rig, retaining each actress's own palette/head. */
    private static Rig actressRig(Rig source,int side){
        var selected=new ArrayList<Surface>();
        for(var surface:source.surfaces){if(side==1&&surface.bone.equals("second_head")||side==2&&surface.bone.equals("head"))continue;
            if(!surface.bone.equals("head")&&!surface.bone.equals("second_head")){
                boolean green=surface.g-surface.r>.055F,pink=surface.r-surface.g>.12F;
                if(side==1&&pink||side==2&&green)continue;
            }selected.add(surface);
        }
        var pivots=new LinkedHashMap<>(source.pivots);if(side==2)pivots.put("second_head",source.pivots.get("head").clone());
        return new Rig(pivots,source.parents,List.copyOf(selected));
    }
    private static Rig load(String id){
        try(var stream=Minecraft.getInstance().getResourceManager().open(ResourceLocation.fromNamespaceAndPath("coraline","characters/"+id+".json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var json=JsonParser.parseReader(reader).getAsJsonObject();var pivots=new LinkedHashMap<String,float[]>();var parents=new HashMap<String,String>();var surfaces=new ArrayList<Surface>();
            for(var element:json.getAsJsonArray("bones")){var b=element.getAsJsonObject();String name=b.get("name").getAsString();var p=b.getAsJsonArray("pivot");pivots.put(name,new float[]{p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat()});if(!b.get("parent").isJsonNull())parents.put(name,b.get("parent").getAsString());}
            for(var element:json.getAsJsonArray("meshes")){
                var m=element.getAsJsonObject();var v=m.getAsJsonArray("vertices");Vector3f[] vertices=new Vector3f[v.size()],normals=new Vector3f[v.size()];
                for(int i=0;i<v.size();i++){var a=v.get(i).getAsJsonArray();vertices[i]=new Vector3f(a.get(0).getAsFloat()/16,a.get(1).getAsFloat()/16,a.get(2).getAsFloat()/16);normals[i]=new Vector3f();}
                for(var f:m.getAsJsonArray("faces")){var a=f.getAsJsonArray();var n=new Vector3f(vertices[a.get(1).getAsInt()]).sub(vertices[a.get(0).getAsInt()]).cross(new Vector3f(vertices[a.get(2).getAsInt()]).sub(vertices[a.get(0).getAsInt()]));for(var ix:a)normals[ix.getAsInt()].add(n);}
                for(var n:normals)if(n.lengthSquared()>1e-12)n.normalize();else n.set(0,0,-1);
                List<Point> points=new ArrayList<>();var uv=m.getAsJsonArray("uv");
                for(var f:m.getAsJsonArray("faces")){var a=f.getAsJsonArray();for(int i=1;i+1<a.size();i++)for(int j:new int[]{0,i,i+1,i+1}){int ix=a.get(j).getAsInt();var p=vertices[ix];var n=normals[ix];var t=uv.get(ix).getAsJsonArray();points.add(new Point(p.x,p.y,p.z,t.get(0).getAsFloat(),t.get(1).getAsFloat(),n.x,n.y,n.z));}}
                var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);for(var vertex:vertices){min.min(vertex);max.max(vertex);}var corners=new ArrayList<Vector3f>();for(float x:new float[]{min.x,max.x})for(float y:new float[]{min.y,max.y})for(float z:new float[]{min.z,max.z})corners.add(new Vector3f(x,y,z));
                int color=Integer.parseInt(m.get("color").getAsString().substring(1),16);surfaces.add(new Surface(m.get("bone").getAsString(),m.has("part")?m.get("part").getAsString():"",((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,List.copyOf(points),List.copyOf(corners)));
            }
            return new Rig(pivots,parents,List.copyOf(surfaces));
        }catch(IOException|RuntimeException ex){throw new IllegalStateException("Cannot load character "+id,ex);}
    }
    @Override public void setupAnim(CharacterEntity actor,float swing,float amount,float age,float yaw,float pitch){
        if(revision!=cacheRevision)refresh();
        visitor=actor instanceof com.coraline.wybie.WybieVisitorEntity v?v:null;animationAge=age;
        wellWybie=id.equals("wybie")&&actor.gameGesture()>=70&&actor.gameGesture()<=77;if(wellWybie)visitor=null;
        storyRealWybie=id.equals("wybie")&&com.coraline.client.FirstEscapeClient.active()&&actor.getId()==com.coraline.client.FirstEscapeClient.realWybie();if(storyRealWybie)visitor=null;
        catBiting=id.equals("cat")&&actor.gameGesture()==20;
        storyWybie=id.equals("other_wybie")&&com.coraline.client.FirstEscapeClient.active()&&actor.getId()==com.coraline.client.FirstEscapeClient.wybie();
        bobVisitor=actor instanceof com.coraline.bobinsky.BobinskyVisitorEntity v?v:null;
        bones.forEach((name,b)->{b.xRot=b.yRot=b.zRot=0;b.xScale=b.yScale=b.zScale=1;var p=rig.pivots.get(name);b.setPos(p[0],p[1],p[2]);});
        var head=bones.get("head");head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        if(bones.containsKey("second_head")){bones.get("second_head").yRot=head.yRot;bones.get("second_head").xRot=head.xRot;}
        bones.get("body").xRot=Mth.sin(age*.055F)*.016F;
        for(String side:List.of("right","left")){
            float phase=swing*.6662F+(side.equals("left")?Mth.PI:0),stride=Mth.cos(phase)*amount;
            bones.get(side+"_leg").xRot=stride*.9F;
            bones.get(side+"_arm").xRot=-stride*.7F;
            bones.get(side+"_arm").zRot=(side.equals("left")?1:-1)*(.035F+Mth.sin(age*.045F)*.012F);
            var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=Math.max(0,-stride)*.9F;
            var forearm=bones.get(side+"_forearm");if(forearm!=null)forearm.xRot=-.08F-Math.abs(stride)*.15F;
            if(actor.isInWater()){bones.get(side+"_arm").xRot=-1.2F+Mth.sin(age*.2F)*.3F;}
            if(!actor.onGround()&&!actor.isInWater()){bones.get(side+"_leg").xRot=.2F;bones.get(side+"_arm").zRot=(side.equals("left")?1:-1)*.3F;}
        }
        bones.get("tail").yRot=Mth.sin(age*.09F)*.15F;
        if(id.equals("evil_other_mother")&&com.coraline.client.FirstEscapeClient.phase().equals("MIRROR_DRAG")){
            for(String side:List.of("right","left")){float stride=Mth.sin(age*.30F+(side.equals("left")?Mth.PI:0))*.45F;bones.get(side+"_leg").xRot=stride;var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=Math.max(0,-stride)*.6F;}
        }
        if(id.equals("cat")&&actor.gameGesture()>=20){head.xRot=actor.gameGesture()==20?.25F+Mth.sin(age*1.2F)*.22F:.17F;head.zRot=actor.gameGesture()==20?Mth.sin(age*.6F)*.08F:0;}
        if(visitor!=null)visitPose(visitor,age);
        if(actor instanceof com.coraline.bobinsky.BobinskyVisitorEntity bob)bobinskyPose(bob,age);
        if(actor instanceof com.coraline.tea.TeaVisitorEntity tea)teaPose(tea);
        if(actor.characterId().equals("other_wybie")){
            if(actor.isPassenger())for(String side:List.of("right","left")){bones.get(side+"_leg").xRot=-1.35F;var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=1.2F;}
            int gesture=actor.gameGesture();float wave=Mth.sin(age*.18F);
            if(gesture==1){bones.get("right_arm").zRot=-1.4F+wave*.4F;bones.get("left_arm").zRot=1.4F-wave*.4F;}
            if(gesture==2||gesture==6){bones.get("right_arm").xRot=-1.2F+wave*.25F;bones.get("left_arm").xRot=-.8F;}
            if(gesture==3){head.zRot=wave*.4F;bones.get("right_arm").zRot=-.9F;}
            if(gesture==4){bones.get("right_arm").xRot=-1.5F;bones.get("left_arm").xRot=-1.5F;head.xRot=.2F;}
            if(gesture==5){bones.get("body").xRot=.22F;head.zRot=wave*.2F;bones.get("right_arm").xRot=-.6F;}
            if(gesture==7){bones.get("right_arm").xRot=-1.2F;bones.get("left_arm").zRot=.6F;head.xRot=wave*.15F;}
            if(gesture==8){bones.get("right_arm").xRot=-2;head.xRot=.5F;}
        }
        if(actor.characterId().equals("other_wybie")){
            int gesture=actor.gameGesture();
            if(gesture==9){head.xRot=Mth.sin(age*.22F)*.14F;}
            if(gesture==10){head.yRot=Mth.sin(age*.08F)*.5F;bones.get("right_arm").xRot=-1.25F;bones.get("right_arm").zRot=-.25F;}
            if(gesture==11){head.zRot=Mth.sin(age*.2F)*.13F;bones.get("right_arm").xRot=-1.4F;bones.get("left_arm").xRot=-.45F;}
            if(gesture==12){head.xRot=.55F;bones.get("body").xRot=.13F;bones.get("right_arm").xRot=.12F;bones.get("left_arm").xRot=.12F;}
            if(gesture==15){bones.get("right_arm").xRot=-2.0F;bones.get("right_arm").zRot=-.2F;var fore=bones.get("right_forearm");if(fore!=null)fore.xRot=-1.3F;}
            if(gesture==16){bones.get("right_arm").xRot=-1.3F;bones.get("right_arm").zRot=-.1F;var fore=bones.get("right_forearm");if(fore!=null)fore.xRot=-.08F;}
            if(actor.getVehicle() instanceof com.coraline.feast.WybieBicycleEntity bike){
                float cycle=net.minecraft.util.Mth.lerp(age-actor.tickCount,bike.previousWheelRotation,bike.wheelRotation)*.5F;
                bones.get("body").xRot=.15F;head.xRot-=.15F;
                for(String side:List.of("right","left")){
                    float pedal=Mth.sin(cycle+(side.equals("left")?Mth.PI:0));
                    bones.get(side+"_leg").xRot=-1.05F+pedal*.35F;
                    var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=1.15F-pedal*.2F;
                    bones.get(side+"_arm").xRot=-1.25F;bones.get(side+"_arm").zRot=side.equals("left")?.08F:-.08F;
                    var forearm=bones.get(side+"_forearm");if(forearm!=null)forearm.xRot=-.3F;
                }
            }
        }
        if(storyWybie){var arm=bones.get("right_arm");var left=bones.get("left_arm");var fore=bones.get("right_forearm");int gesture=actor.gameGesture();
            if(gesture==42){arm.xRot=left.xRot=-2.15F;arm.yRot=-.35F;left.yRot=.35F;}
            if(gesture==43){head.xRot=.15F;arm.xRot=-.6F;}
            if(gesture==44){arm.xRot=-1.9F;fore.xRot=-.8F;}
            if(gesture==45){arm.xRot=-1.8F;fore.xRot=-.7F;left.xRot=-1.15F;}
            if(gesture==46){arm.xRot=left.xRot=-1.6F;bones.get("body").xRot=.25F;}
            if(gesture==47)head.yRot=Mth.sin(age*.2F)*.28F;
            if(gesture==48){arm.xRot=-1.35F;fore.xRot=-.7F;head.xRot=.3F;}
            if(gesture==50){arm.xRot=-1.6F;fore.xRot=-.5F;}
            int knock=com.coraline.client.FirstEscapeClient.knockdown();if(knock>=0&&com.coraline.client.FirstEscapeClient.knockedDown()){float fall=Math.min(1,knock/8F);bones.get("body").zRot=fall*1.3F;bones.get("body").y+=fall*5;head.zRot=-fall*.2F;}
        }
        if(actor.getVehicle() instanceof com.coraline.feast.WybieBicycleEntity bike&&id.equals("wybie")){float cycle=Mth.lerp(age-actor.tickCount,bike.previousWheelRotation,bike.wheelRotation)*.5F;bones.get("body").xRot=.17F;for(String side:List.of("right","left")){bones.get(side+"_arm").xRot=-1.3F;bones.get(side+"_leg").xRot=-1+Mth.sin(cycle+(side.equals("left")?Mth.PI:0))*.35F;bones.get(side+"_shin").xRot=1.1F;}}
        com.coraline.client.ShowActorPoseClient.apply(actor,bones,age);
        if(id.equals("evil_other_father")&&actor.gameGesture()==81){
            var arm=bones.get("right_arm");var fore=bones.get("right_forearm");
            if(arm!=null){arm.xRot=-1.3F;arm.yRot=0;arm.zRot=0;}if(fore!=null)fore.xRot=-.35F;
        }
        if(wellWybie){
            var arm=bones.get("right_arm");var fore=bones.get("right_forearm");var left=bones.get("left_arm");var lf=bones.get("left_forearm");
            arm.yRot=left.yRot=0;arm.zRot=-.12F;left.zRot=.12F;fore.xRot=lf.xRot=-.18F;
            switch(actor.gameGesture()){
                case 70->{arm.xRot=-1.3F;fore.xRot=-.25F;left.xRot=-.6F;}
                case 71->{arm.xRot=-2.8F;left.xRot=-2.65F;fore.xRot=lf.xRot=-.35F;head.xRot=-.35F;bones.get("body").zRot=Mth.sin(age*.15F)*.07F;}
                case 72->{arm.xRot=-2.0F+Mth.sin(age*.18F)*.4F;left.xRot=-2.0F-Mth.sin(age*.18F)*.4F;fore.xRot=lf.xRot=-.5F;head.xRot=-.15F;}
                case 73->{arm.xRot=left.xRot=-2.15F;fore.xRot=lf.xRot=-.65F;}
                case 74->{arm.xRot=-1.15F;fore.xRot=-.8F;left.xRot=-.8F;lf.xRot=-.35F;}
                case 75->{arm.xRot=-1.7F;fore.xRot=-.3F;left.xRot=-.15F;}
                case 76->{arm.xRot=-.2F;left.xRot=-.1F;head.xRot=.10F;}
                case 77->{if(!actor.isPassenger()){arm.xRot=-.15F;left.xRot=-.1F;}head.yRot+=Mth.sin(age*.06F)*.35F;}
            }
        }
        // These rigs have negative-X right limbs and positive-X left limbs. Their old
        // outward gestures used the opposite Z convention, driving hands into torsos.
        if(id.equals("other_wybie")||id.equals("wybie")||id.equals("other_bobinsky")||id.equals("bobinsky"))for(String side:List.of("right","left"))for(String joint:List.of("_arm","_forearm","_leg")){var limb=bones.get(side+joint);if(limb!=null)limb.zRot=-limb.zRot;}
        if(actor.partyDoorDuck()){bones.get("body").xRot=id.contains("bobinsky")?1.15F:.30F;head.xRot=-.2F;}
        // Physical grounding alone cannot protect an inverted head or a rotated boot.
        // Lift the articulated rig relative to its feet, without moving the entity's flight path.
        if(!actor.isPassenger()&&(visitor!=null||bobVisitor!=null||actor instanceof com.coraline.tea.TeaVisitorEntity)){float max=posedMaxY();if(max>1.5F)bones.get("body").y-=(max-1.5F)*16;}
    }
    public float posedMaxY(){var pose=new PoseStack();var point=new Vector3f();float max=Float.NEGATIVE_INFINITY;for(var surface:rig.surfaces){pose.pushPose();transform(surface.bone,pose);for(var corner:surface.bounds){point.set(corner);pose.last().pose().transformPosition(point);max=Math.max(max,point.y);}pose.popPose();}return max;}
    private void teaPose(com.coraline.tea.TeaVisitorEntity tea){
        if(MissingParentsTeaPresentation.pose(tea,bones,animationAge))return;
        boolean spink=tea.characterId().equals("miss_spink");int phase=tea.phase();
        if(tea.isPassenger())for(String side:List.of("right","left")){bones.get(side+"_leg").xRot=-1.25F;var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=1.25F;}
        if(tea.hasCup()||!spink&&phase==10){
            boolean shake=phase==8;float t=tea.elapsed()+animationAge-(int)animationAge;
            float sx=shake?Mth.sin(t*.3F)*.28F:0,sy=shake?Mth.cos(t*.3F)*.15F:0;
            for(String side:List.of("right","left")){boolean right=side.equals("right");var arm=bones.get(side+"_arm");var fore=bones.get(side+"_forearm");arm.xRot=-.65F+sy;arm.zRot=0;arm.yRot=right?-.75F:.75F;arm.z-=2.5F;fore.xRot=-1.05F;fore.zRot=right?-.25F:.25F;arm.x+=sx;fore.y+=sy;}
            if(phase==8||phase==9||phase==10){bones.get("head").xRot=.4F;bones.get("head").yRot=0;}
        }
    }
    private void bobinskyPose(com.coraline.bobinsky.BobinskyVisitorEntity b,float age){
        var body=bones.get("body");var head=bones.get("head");var arm=bones.get("right_arm");var fore=bones.get("right_forearm");int phase=b.phase();
        switch(phase){
            case 1->{head.xRot=.28F;head.yRot=0;head.zRot=.08F;for(String side:List.of("right","left")){var a=bones.get(side+"_arm");var f=bones.get(side+"_forearm");a.xRot=-1.45F;a.zRot=0;a.yRot=side.equals("right")?-.5F:.5F;f.xRot=-1.55F;}}
            case 2->{arm.xRot=-1.25F;fore.xRot=-.4F;arm.zRot=.25F;head.xRot=-.12F;}
            case 4->{float progress=Math.min(1,b.elapsed()/(float)Math.max(1,com.coraline.feast.VisitAudio.length("bobinsky_visit_d")));
                if(progress<.2F){body.xRot=Mth.sin(progress/.2F*Mth.PI)*.65F;arm.zRot=-.4F;bones.get("left_arm").zRot=.4F;}
                else if(progress<.45F){float t=(progress-.2F)/.25F;body.zRot=t*Mth.TWO_PI;body.y-=Mth.sin(t*Mth.PI)*6;arm.zRot=-2.5F;bones.get("left_arm").zRot=2.5F;bones.get("right_leg").zRot=-.6F;bones.get("left_leg").zRot=.6F;}
                else if(progress<.8F){body.zRot=Mth.PI;body.y-=12;arm.zRot=-3.05F;bones.get("left_arm").zRot=3.05F;bones.get("right_leg").zRot=-.3F;bones.get("left_leg").zRot=.3F;}
                else{float t=(progress-.8F)/.2F;body.zRot=Mth.PI*(1-t);body.y-=12*(1-t);arm.zRot=-2.4F*(1-t);bones.get("left_arm").zRot=2.4F*(1-t);}
            }
            case 5->{float t=Math.min(1,b.elapsed()/14F);body.xRot=-t*Mth.TWO_PI;body.y-=Mth.sin(t*Mth.PI)*12;arm.zRot=-1.4F;bones.get("left_arm").zRot=1.4F;}
            case 7->{arm.xRot=-1.3F;fore.xRot=-.35F;head.xRot=.05F;}
            case 10->{body.xRot=-.2F;arm.zRot=-1.3F;bones.get("left_arm").zRot=1.3F;head.xRot=.8F;}
            case 11,12->{body.y-=12;head.xRot=.8F;for(String side:List.of("right","left")){var leg=bones.get(side+"_leg");leg.yScale=1.75F;leg.xRot=0;leg.zRot=side.equals("right")?-.28F:.28F;bones.get(side+"_shin").xRot=0;bones.get(side+"_arm").zRot=side.equals("right")?-1.4F:1.4F;}}
        }
    }
    private void visitPose(com.coraline.wybie.WybieVisitorEntity v,float age){
        var body=bones.get("body");var head=bones.get("head");var arm=bones.get("right_arm");var fore=bones.get("right_forearm");
        float t=Math.min(1,v.elapsed()/12F);int phase=v.phase();
        switch(phase){
            case 2,3->{body.xRot=.35F;head.xRot=.65F;head.yRot=Mth.sin(age*.08F)*.3F;arm.xRot=-.45F;fore.xRot=-.4F;}
            case 4->{arm.xRot=-1.3F*t;arm.zRot=-.38F*t;fore.xRot=-.6F*t;head.xRot=.08F;}
            case 5->{arm.xRot=-1.1F;arm.zRot=-.3F;fore.xRot=-1.15F;head.xRot=.22F;head.yRot=.35F;head.zRot=.18F;}
            case 6->{arm.xRot=-1.25F;arm.zRot=-.35F;fore.xRot=-.7F;bones.get("left_arm").xRot=-1.2F*t;bones.get("left_forearm").xRot=-.35F;}
            case 7->{arm.xRot=-1.5F;arm.zRot=.65F;fore.xRot=-1.4F;head.xRot=.12F;}
            case 8->{arm.xRot=-1.5F;arm.zRot=.75F;fore.xRot=-1.5F;head.xRot=.12F;}
            case 9->{arm.xRot=-2.8F*t;arm.zRot=-.6F;fore.xRot=-.45F;head.xRot=-.55F;head.yRot=-.25F;}
            case 10->{arm.xRot=-1.5F;arm.zRot=.65F;fore.xRot=-1.4F;head.zRot=-.1F;}
            case 11->{arm.xRot=-2.2F*(1-t);arm.zRot=-1.4F*t;fore.xRot=-.8F*(1-t);}
            case 13->{head.yRot=1.05F*t;head.zRot=-.12F;body.xRot=-.1F;}
            case 14->{if(v.biking()){body.y-=3;body.xRot=.22F;head.xRot=-.2F;for(String side:List.of("right","left")){var leg=bones.get(side+"_leg");leg.xRot=-1F+Mth.sin(age*.35F+(side.equals("left")?Mth.PI:0))*.35F;bones.get(side+"_shin").xRot=1.1F;var a=bones.get(side+"_arm");a.xRot=-1.35F;a.zRot=0;bones.get(side+"_forearm").xRot=-.25F;}}}
        }
    }
    public void transformBone(String name,PoseStack pose){transform(name,pose);}
    public void renderMotherMorph(PoseStack pose,VertexConsumer consumer,int light,int overlay,ResourceLocation skin,float progress){
        if(motherMorph==null||!skin.equals(morphSkin))prepareMotherMorph(skin);
        for(int i=0;i<motherMorph.length;i+=16){var v=motherMorph;float t=com.coraline.feast.CocoaBeetlesChoreography.morph(progress,(int)v[i+15]);
            consumer.vertex(pose.last().pose(),Mth.lerp(t,v[i],v[i+3]),Mth.lerp(t,v[i+1],v[i+4]),Mth.lerp(t,v[i+2],v[i+5]))
                .color(Mth.lerp(t,v[i+6],v[i+9]),Mth.lerp(t,v[i+7],v[i+10]),Mth.lerp(t,v[i+8],v[i+11]),1)
                .uv(.5F,.5F).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),v[i+12],v[i+13],v[i+14]).endVertex();
        }
    }
    private void prepareMotherMorph(ResourceLocation skin){
        motherMorph=new float[rig.surfaces.stream().mapToInt(surface->surface.points.size()).sum()*16];morphSkin=skin;int index=0;
        var bounds=new HashMap<String,Vector3f[]>();
        for(var surface:rig.surfaces){var range=bounds.computeIfAbsent(surface.bone,key->new Vector3f[]{new Vector3f(Float.POSITIVE_INFINITY),new Vector3f(Float.NEGATIVE_INFINITY)});for(var corner:surface.bounds){range[0].min(corner);range[1].max(corner);}}
        var atlas=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
        for(var surface:rig.surfaces){var local=new PoseStack();transform(surface.bone,local);var range=bounds.get(surface.bone);
            for(var point:surface.points){var nativePoint=new Vector3f(point.x,point.y,point.z);
                var from=MotherMorphGeometry.parentPoint(surface.bone,nativePoint,range[0],range[1]);
                var to=local.last().pose().transformPosition(new Vector3f(nativePoint));
                var color=MotherMorphGeometry.skin(skin,surface.bone,from);
                var targetColor=MotherMorphGeometry.pixel(atlas,point.u,point.v).mul(surface.r,surface.g,surface.b);
                var normal=local.last().normal().transform(new Vector3f(point.nx,point.ny,point.nz)).normalize();
                for(float value:new float[]{from.x,from.y,from.z,to.x,to.y,to.z,color.x,color.y,color.z,targetColor.x,targetColor.y,targetColor.z,normal.x,normal.y,normal.z,com.coraline.feast.CocoaBeetlesChoreography.morphRegion(surface.bone)})motherMorph[index++]=value;
            }
        }
    }
    public void theatreSet(float tick,float duration){if(revision!=cacheRevision)refresh();for(var entry:bones.entrySet()){var bone=entry.getValue();var pivot=rig.pivots.get(entry.getKey());bone.setPos(pivot[0],pivot[1],pivot[2]);bone.xRot=bone.yRot=bone.zRot=0;}float fall=(float)com.coraline.feast.ScriptedShowTimeline.smooth((tick-Math.max(0,duration-200))/200.0);var body=bones.get("body");if(body!=null){body.xRot=fall*1.34F;body.y+=fall*18;}var boat=bones.get("boat");if(boat!=null){boat.zRot=(float)-com.coraline.feast.TheatreOpeningChoreography.boatTilt(tick/duration)+fall*.38F;boat.y+=fall*12;}for(int i=0;i<3;i++){var wave=bones.get("wave_"+i);if(wave!=null){wave.x=rig.pivots.get("wave_"+i)[0]+(float)Math.sin(tick*.035+i)*3;wave.zRot=(i-1)*fall*.28F;}}}
    private void transform(String name,PoseStack pose){String parent=rig.parents.get(name);if(parent!=null)transform(parent,pose);bones.get(name).translateAndRotate(pose);}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,float r,float g,float b,float alpha){
renderSpeech(pose,consumer,light,overlay,r,g,b,alpha,wellWybie?com.coraline.client.VisitAudioClient.voiceLevel("crawling_hand_"):storyRealWybie?com.coraline.client.VisitAudioClient.voiceLevel("missing_parents_"):id.equals("cat")?catSpeech():visitor!=null?com.coraline.client.VisitAudioClient.voiceLevel("wybie_visit_"):bobVisitor!=null?com.coraline.client.VisitAudioClient.voiceLevel("bobinsky_visit_"):id.equals("evil_other_father")?com.coraline.client.VisitAudioClient.voiceLevel("parent_rescue_f"):id.equals("miss_spink")||id.equals("miss_forcible")?com.coraline.client.VisitAudioClient.voiceLevel("missing_parents_tea_"):0);
    }
    private static float catSpeech(){
        // A/B/D are the player's questions; C is the mirror strike. Keep the
        // Cat listening instead of moving its mouth for those recordings.
        float speech=Math.max(com.coraline.client.VisitAudioClient.voiceLevel("cat_encounter_"),com.coraline.client.VisitAudioClient.voiceLevel("missing_parents_cat_e"));
        int time=com.coraline.client.VisitAudioClient.storyTicks("missing_parents_cat_f");
        if(time>=80&&time<120)speech=Math.max(speech,com.coraline.client.VisitAudioClient.voiceLevel("missing_parents_cat_f"));
        return speech;
    }
    /** Same native mesh path, with a supplied speech sample for isolated render regression checks. */
    public void renderSpeech(PoseStack pose,VertexConsumer consumer,int light,int overlay,float r,float g,float b,float alpha,float speech){
        if(revision!=cacheRevision)refresh();
        if(id.equals("cat")){pose.pushPose();transform("head",pose);float open=catBiting?.5F+.5F*Mth.sin(animationAge*1.2F):speech;
            polygon(pose,consumer,new float[][]{{-1,1.25F},{1,1.25F},{.8F,1.35F+open*.8F},{-.8F,1.35F+open*.8F}},-2.35F,light,overlay,r*.025F,g*.02F,b*.02F,alpha);
            if(open>.2F)for(float x:new float[]{-.8F,.5F})polygon(pose,consumer,new float[][]{{x,1.26F},{x+.3F,1.26F},{x+.15F,1.65F}},-2.36F,light,overlay,r*.9F,g*.88F,b*.8F,alpha);pose.popPose();}
        if(storyWybie){pose.pushPose();transform("head",pose);FirstEscapePresentation.wybieFace(pose,consumer,light);pose.popPose();}
        for(var surface:rig.surfaces){if(storyHeadOnly&&!surface.bone.equals("head"))continue;if(storyBlind&&buttonSurface(surface))continue;if(storyWybie&&!com.coraline.client.FirstEscapeClient.flag("StitchesRemoved")&&surface.part.equals("crooked smile"))continue;if(storyWybie&&com.coraline.client.FirstEscapeClient.flag("HandMissing")&&surface.bone.equals("right_forearm")&&surface.bounds.stream().mapToDouble(v->v.y).min().orElse(0)>.3)continue;pose.pushPose();transform(surface.bone,pose);
            if(bobVisitor!=null&&(surface.part.equals("smiling mouth")||surface.part.equals("small square tooth"))){
                if(surface.part.equals("smiling mouth"))bobinskyMouth(pose,consumer,light,overlay,r,g,b,alpha,speech);
                pose.popPose();continue;
            }
            if((visitor!=null||storyRealWybie||wellWybie)&&surface.part.equals("crooked smile")&&speech>.035F){
                wybieMouth(pose,consumer,light,overlay,r,g,b,alpha,speech);pose.popPose();continue;
            }
            float cx=0,cy=0;boolean face=(visitor!=null||bobVisitor!=null)&&!surface.part.isEmpty();
            if(face){for(var p:surface.points){cx+=p.x;cy+=p.y;}cx/=surface.points.size();cy/=surface.points.size();}
            var matrix=pose.last();for(var p:surface.points){float x=p.x,y=p.y;
                if((id.equals("evil_other_father")||id.equals("evil_other_mother")||id.equals("beldam"))&&surface.bone.equals("head")&&speech>.035F&&((surface.r>.31F&&surface.r<.35F&&surface.g<.2F)||(surface.r>.87F&&surface.g>.82F&&surface.b>.72F)||id.equals("beldam")&&((surface.r<.10F&&surface.g>.13F&&surface.g<.16F)||(surface.r>.80F&&surface.g>.80F))))y=mouthY(y,-3.8F/16F,speech,1.7F);
                if(face&&visitor!=null){int phase=visitor.phase();
                    // The closed smile is rigid. Speech replaces it with a filled cavity,
                    // not a stretched curve whose lower vertices leave the chin.
                    if(surface.part.equals("crooked smile")&&(phase==5||phase==13))x=cx+(x-cx)*.65F;
                    if(surface.part.equals("uneven concerned brow")){float side=p.x<0?-1:1;if(phase==5)y+=side*.025F;if(phase==13)y-=.04F;}
                    if(surface.part.equals("pupil")||surface.part.equals("green hazel iris")||surface.part.equals("eye glint")){if(phase==13)x+=.018F;if(phase==5)y+=.015F;}
                    if(surface.part.equals("eye white")||surface.part.equals("pupil")||surface.part.equals("green hazel iris")||surface.part.equals("eye glint")){if(((int)animationAge)%83<3)y=cy+(y-cy)*.08F;}
                }
                if(face&&bobVisitor!=null){if(surface.part.equals("smiling mouth")){float loudness=com.coraline.client.VisitAudioClient.voiceLevel("bobinsky_visit_");y=mouthY(p.y,cy,loudness,2.8F);}if(surface.part.equals("eye pupil")&&bobVisitor.phase()==com.coraline.bobinsky.BobinskyVisit.B)y+=.015F;}
                float socket=storyBlind&&socketSurface(surface)?.18F:1;consumer.vertex(matrix.pose(),x,y,p.z).color(r*surface.r*socket,g*surface.g*socket,b*surface.b*socket,alpha).uv(p.u,p.v).overlayCoords(overlay).uv2(light).normal(matrix.normal(),p.nx,p.ny,p.nz).endVertex();}pose.popPose();}
    }
    private boolean buttonSurface(Surface surface){if(!surface.bone.equals("head"))return false;return id.equals("evil_other_mother")&&(Math.abs(surface.r-19/255F)<.001F&&Math.abs(surface.g-25/255F)<.001F||Math.abs(surface.r-105/255F)<.001F&&Math.abs(surface.g-113/255F)<.001F)||id.equals("beldam")&&(Math.abs(surface.r-17/255F)<.001F&&Math.abs(surface.g-27/255F)<.001F||Math.abs(surface.r-130/255F)<.001F&&Math.abs(surface.g-152/255F)<.001F);}
    private boolean socketSurface(Surface surface){return surface.bone.equals("head")&&(id.equals("evil_other_mother")&&Math.abs(surface.r-176/255F)<.001F&&Math.abs(surface.g-148/255F)<.001F||id.equals("beldam")&&Math.abs(surface.r-88/255F)<.001F&&Math.abs(surface.g-111/255F)<.001F);}
    /** One remeshed surface moves between corresponding articulated rig regions.
     * Extra spider limbs unfold from the source surface; no second faded actor. */
    public void renderRigTransition(CharacterModel source,PoseStack pose,VertexConsumer out,int light,int overlay,float progress){
        float u=Mth.clamp(progress,0,1);if(u<=0){source.renderSpeech(pose,out,light,overlay,1,1,1,1,0);return;}
        // A closed source envelope bridges the changing target topology. Both
        // are opaque, geometrically moving parts of one skin, not faded actors.
        // Target facets unfold before source facets contract into their mapped
        // target surface; this prevents open strips through the chest/skirt.
        float unfold=Mth.clamp((u-.18F)/.42F,0,1);unfold=unfold*unfold*(3-2*unfold);
        renderRigTransitionSurfaces(source,pose,out,light,overlay,u,unfold);
        if(u<1){float envelope=Mth.clamp((1-u)/.44F,0,1);envelope=envelope*envelope*(3-2*envelope);boolean blind=source.storyBlind;source.storyBlind=storyBlind;try{source.renderRigTransitionSurfaces(this,pose,out,light,overlay,1-u,envelope);}finally{source.storyBlind=blind;}}
    }
    private void renderRigTransitionSurfaces(CharacterModel source,PoseStack pose,VertexConsumer out,int light,int overlay,float progress,float facetScale){
        if(revision!=cacheRevision)refresh();if(source.revision!=cacheRevision)source.refresh();
        if(rigBlend==null||!source.id.equals(rigBlendSource))prepareRigTransition(source);
        float t=Mth.clamp(progress,0,1);t=t*t*(3-2*t);
        var fromTransforms=new HashMap<String,PoseStack>();var toTransforms=new HashMap<String,PoseStack>();var v=new Vector3f();var target=new Vector3f();var normal=new Vector3f();var targetNormal=new Vector3f();var color=new Vector3f();var centroid=new Vector3f();var fromCenter=new Vector3f();var toCenter=new Vector3f();
        for(var blend:rigBlend){var surface=blend.target;if(storyBlind&&buttonSurface(surface))continue;
            var from=fromTransforms.computeIfAbsent(blend.sourceBone,bone->{var stack=new PoseStack();source.transform(bone,stack);return stack;});
            var to=toTransforms.computeIfAbsent(surface.bone,bone->{var stack=new PoseStack();transform(bone,stack);return stack;});
            if(facetScale<1){var a=source.cages().get(blend.sourceBone);var b=cages().get(surface.bone);fromCenter.set(a[0]).add(a[1]).mul(.5F);toCenter.set(b[0]).add(b[1]).mul(.5F);from.last().pose().transformPosition(fromCenter);to.last().pose().transformPosition(toCenter);centroid.set(fromCenter).lerp(toCenter,t);}
            for(int i=0;i<surface.points.size();i++){
                var a=blend.from.get(i);var b=surface.points.get(i);from.last().pose().transformPosition(v.set(a.x,a.y,a.z));to.last().pose().transformPosition(target.set(b.x,b.y,b.z));v.lerp(target,t);if(facetScale<1)v.sub(centroid).mul(facetScale).add(centroid);
                from.last().normal().transform(normal.set(a.nx,a.ny,a.nz));to.last().normal().transform(targetNormal.set(b.nx,b.ny,b.nz));normal.lerp(targetNormal,t);if(normal.lengthSquared()<.00001F)normal.set(0,1,0);else normal.normalize();
                color.set(blend.targetColors.get(i)).lerp(blend.color,1-t);if(storyBlind&&socketSurface(surface))color.mul(.18F);
                out.vertex(pose.last().pose(),v.x,v.y,v.z).color(color.x,color.y,color.z,1).uv(.5F,.5F).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),normal.x,normal.y,normal.z).endVertex();
            }
        }
    }
    private Map<String,Vector3f[]> cages(){if(cageRanges==null)cageRanges=boneRanges(rig);return cageRanges;}
    private static Map<String,Vector3f[]> boneRanges(Rig rig){var result=new HashMap<String,Vector3f[]>();for(var surface:rig.surfaces){var range=result.computeIfAbsent(surface.bone,key->new Vector3f[]{new Vector3f(Float.POSITIVE_INFINITY),new Vector3f(Float.NEGATIVE_INFINITY)});for(var bound:surface.bounds){range[0].min(bound);range[1].max(bound);}}return result;}
    private void prepareRigTransition(CharacterModel source){
        var result=new ArrayList<RigBlend>();var ordinals=new HashMap<String,Integer>();var fromRanges=source.cages();var toRanges=cages();
        for(var target:rig.surfaces){String bone=target.bone;var candidates=source.rig.surfaces.stream().filter(s->s.bone.equals(bone)).toList();if(candidates.isEmpty()){String fallback=bone.endsWith("leg")?bone.replace("leg","shin"):"body";candidates=source.rig.surfaces.stream().filter(s->s.bone.equals(fallback)).toList();}if(candidates.isEmpty())throw new IllegalStateException("No source region for "+bone);
            int ordinal=ordinals.merge(bone,1,Integer::sum)-1;Surface chosen=null;
            if(bone.equals("head")&&candidates.size()==9)chosen=candidates.get(ordinal);
            else{float distance=Float.POSITIVE_INFINITY;for(var candidate:candidates){float d=(target.r-candidate.r)*(target.r-candidate.r)+(target.g-candidate.g)*(target.g-candidate.g)+(target.b-candidate.b)*(target.b-candidate.b);if(d<distance){distance=d;chosen=candidate;}}}
            // Every material on a body region shares one deformation cage.
            // Per-material nearest-surface projections twist adjacent triangles
            // onto different shells and tear an otherwise closed garment.
            var a=fromRanges.get(chosen.bone);var b=toRanges.get(target.bone);var projected=new ArrayList<Point>();
            for(var vertex:target.points){var probe=new Vector3f(a[0].x+(vertex.x-b[0].x)/Math.max(.00001F,b[1].x-b[0].x)*(a[1].x-a[0].x),a[0].y+(vertex.y-b[0].y)/Math.max(.00001F,b[1].y-b[0].y)*(a[1].y-a[0].y),a[0].z+(vertex.z-b[0].z)/Math.max(.00001F,b[1].z-b[0].z)*(a[1].z-a[0].z));
                Point normal=chosen.points.get(0);projected.add(new Point(probe.x,probe.y,probe.z,normal.u,normal.v,normal.nx,normal.ny,normal.nz));
            }
            var first=chosen.points.get(0);var atlas=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");var color=MotherMorphGeometry.pixel(atlas,first.u,first.v).mul(chosen.r,chosen.g,chosen.b);var targetColors=new ArrayList<Vector3f>();for(var point:target.points)targetColors.add(MotherMorphGeometry.pixel(atlas,point.u,point.v).mul(target.r,target.g,target.b));result.add(new RigBlend(chosen.bone,target,List.copyOf(projected),color,List.copyOf(targetColors)));
        }rigBlend=List.copyOf(result);rigBlendSource=source.id;
    }
    /** Keep the upper lip fixed; extend the lower edge in model pixels, not a fraction of a thin smile. */
    public static float mouthY(float y,float center,float loudness,float pixels){return y>center?y+Math.max(0,Math.min(1,loudness))*pixels/16F:y;}
    private static void bobinskyMouth(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float alpha,float speech){
        float open=Math.max(0,Math.min(1,speech)),bottom=-3.65F+open*.9F;
        // The old smile sat at Z=-2.055, behind the face (Z=-2.3).
        // Place the cavity and attached tooth in front of the jaw, never inside it.
        polygon(pose,out,new float[][]{{-1.12F,-3.92F},{1.12F,-3.92F},{.95F,bottom},{-.95F,bottom}},-2.42F,light,overlay,r*.10F,g*.12F,b*.14F,alpha);
        polygon(pose,out,new float[][]{{-.90F,-3.9F},{-.52F,-3.9F},{-.52F,-3.67F},{-.90F,-3.67F}},-2.44F,light,overlay,r*.87F,g*.84F,b*.74F,alpha);
        if(open>.4F)polygon(pose,out,new float[][]{{-.5F,bottom-.15F},{.5F,bottom-.15F},{.6F,bottom-.02F},{-.6F,bottom-.02F}},-2.44F,light,overlay,r*.38F,g*.19F,b*.22F,alpha);
    }
    private static void wybieMouth(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float alpha,float speech){
        float open=Math.max(0,Math.min(1,speech));
        // Head-local pixels: the chin ends at -0.35. Even the widest vowel stays
        // above -0.60, with a fixed upper lip at -2.10 and stationary corners.
        float bottom=-1.76F+1.10F*open;
        float[][] lip={{-1.60F,-1.92F},{-1.10F,-2.18F},{.99F,-2.13F},{1.50F,-1.85F},{.78F,bottom+.07F},{-.95F,bottom+.07F}};
        float[][] cavity={{-1.48F,-1.93F},{-1.05F,-2.08F},{.96F,-2.03F},{1.38F,-1.86F},{.72F,bottom},{-.89F,bottom}};
        polygon(pose,out,lip,-3.19F,light,overlay,r*.46F,g*.31F,b*.24F,alpha);
        polygon(pose,out,cavity,-3.205F,light,overlay,r*.10F,g*.065F,b*.065F,alpha);
        if(open>.18F){
            float[][] teeth={{-1.00F,-2.055F},{.93F,-2.005F},{.88F,-1.85F},{-.96F,-1.90F}};
            polygon(pose,out,teeth,-3.22F,light,overlay,r*.87F,g*.83F,b*.73F,alpha);
        }
        if(open>.45F){
            float[][] tongue={{-.62F,bottom-.18F},{.49F,bottom-.18F},{.64F,bottom-.025F},{-.74F,bottom-.025F}};
            polygon(pose,out,tongue,-3.22F,light,overlay,r*.47F,g*.22F,b*.22F,alpha);
        }
    }
    private static void polygon(PoseStack pose,VertexConsumer out,float[][] points,float z,int light,int overlay,float r,float g,float b,float alpha){
        var matrix=pose.last();
        for(int i=1;i+1<points.length;i++)for(int index:new int[]{0,i,i+1,i+1}){
            float[] p=points[index];out.vertex(matrix.pose(),p[0]/16F,p[1]/16F,z/16F).color(r,g,b,alpha)
                .uv(.00097656F,.99609375F).overlayCoords(overlay).uv2(light).normal(matrix.normal(),0,0,-1).endVertex();
        }
    }
}
