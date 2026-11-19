package com.coraline.client.renderer;

import com.coraline.client.RescueClient;
import com.coraline.feast.*;
import com.coraline.parents.ParentEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Bounded native geometry: one mesh batch for a swarm, not a mob per visible rat. */
public final class RescuePresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png"),ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    private static CharacterModel beldam,transitionSource;private static com.coraline.entity.CharacterEntity proxy;
    private record WebCell(int x,int y,int z,double top,boolean sticky){}
    private static java.util.UUID webId;private static long webRefresh;private static java.util.List<WebCell> webCells=java.util.List.of();
    private static void box(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double w,double h,double d,int color){CinematicPropRenderer.box(p,b,l,x,y,z,w,h,d,color);}
    private static void oval(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double rx,double ry,double rz,int color){FirstEscapePresentation.sphere(p,b,l,x,y,z,rx,ry,rz,color,1);}
    private static void bar(PoseStack p,VertexConsumer b,int l,Vec3 a,Vec3 end,double w,int color){var d=end.subtract(a);if(d.lengthSqr()<.00001)return;p.pushPose();p.translate(a.x,a.y,a.z);p.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),new org.joml.Vector3f((float)d.x,(float)d.y,(float)d.z).normalize()));box(p,b,l,0,0,0,w,d.length(),w,color);p.popPose();}
    private static void quad(PoseStack p,VertexConsumer b,int l,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color,float alpha){for(var v:new Vec3[]{a,c,d,e})b.vertex(p.last().pose(),(float)v.x,(float)v.y,(float)v.z).color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l).normal(p.last().normal(),0,1,0).endVertex();}
    public static boolean prop(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light,float partial){
        if(!RescueProps.kind(e.kind()))return false;var mc=Minecraft.getInstance();if(mc.player==null||!mc.player.getUUID().equals(e.owner)||e.isInvisible())return true;
        var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));double t=e.visualTime(partial);p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        switch(e.kind()){
            case RescueProps.MANTIS->mantis(e,p,b,light,t);
            case RescueProps.WRAPPER->{for(int i=0;i<5;i++)box(p,b,light,0,.12+i*.14,0,1.6-i*.16,.15,1.7-i*.08,0x74846A);var paper=buffers.getBuffer(RenderType.entityTranslucent(WHITE));double open=e.cue()>0?Math.min(1,t/30):0;for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*.25,.3,0);p.mulPose(Axis.ZP.rotationDegrees((float)(side*open*50)));for(int fold=0;fold<8;fold++)box(p,paper,light,side*fold*.13,.03*Math.sin(fold*1.4),0,.16,.04,2.2,fold%2==0?0x8F9E86:0xBCC0A6);p.popPose();}}
            case RescueProps.SWARM->{int count=e.cue()==6?6:96;for(int i=0;i<count;i++){double a=i*2.399963+t*.018,r=e.cue()==6?Math.sqrt((i+.5)/count)*.8:e.cue()==3?3+(t%140)*.025:Math.sqrt((i+.5)/count)*6.5,x=Math.cos(a)*r,z=Math.sin(a)*r,y=e.cue()==1?(i%12)*.22:0;if(e.cue()==2){x+=(t%80)*.07;z=Math.sin(i+t*.03)*5;}
                if(e.cue()==5){x*=Math.max(.18,e.value());z*=Math.max(.18,e.value());y=(i%10)*.21*(1-e.value());}
                if(e.cue()==7){double burst=Math.sin(Math.PI*Math.min(1,e.value()*2));x+=(i%2==0?1:-1)*burst*6;z+=(i%4<2?1:-1)*burst*2.5;y=(i%7)*burst*.15;}p.pushPose();p.translate(x,y,z);p.mulPose(Axis.YP.rotation((float)(-a+t*.03)));rat(p,b,light,t+i*3);p.popPose();}}
            case RescueProps.RAT_BALL->{p.pushPose();p.translate(0,.28,0);p.mulPose(Axis.ZP.rotationDegrees((float)(t*6)));for(int i=0;i<10;i++){p.pushPose();p.mulPose(Axis.YP.rotationDegrees(i*36));box(p,b,light,0,-.25,0,.11,.5,.53,i%2==0?0xAF3045:0xDAC08A);p.popPose();}p.popPose();}
            case RescueProps.CLOTHES->{bar(p,b,light,new Vec3(-1.1,2.2,0),new Vec3(1.1,2.2,0),.04,0x685444);for(int side:new int[]{-1,1}){box(p,b,light,side*.55,.5,0,.65,1.5,.15,side<0?0x86695B:0x52686B);bar(p,b,light,new Vec3(side*.55,1.9,0),new Vec3(side*.95,1.4,0),.18,side<0?0x86695B:0x52686B);}}
            case RescueProps.FURNACE->furnace(e,p,b,light);
            case RescueProps.ROOF_SWING->{double angle=Math.sin(t*.05);var end=new Vec3(Math.sin(angle)*3,-Math.cos(angle)*3,0);box(p,b,light,0,-.08,0,.38,.08,.38,0x554237);bar(p,b,light,Vec3.ZERO,end,.035,0x8D7A5B);oval(p,b,light,end.x,end.y,end.z,.12,.12,.045,0xAB9061);}
            case RescueProps.WEB->web(e,p,buffers,light);
            case RescueProps.PURSUING_DOOR->{for(int side:new int[]{-1,1})box(p,b,light,side*.87,0,0,.16,2.4,.3,0x4E3545);box(p,b,light,0,2.35,0,1.9,.15,.3,0x4E3545);box(p,b,light,0,.05,0,1.6,2.25,.18,0x7D664E);for(int y=0;y<3;y++)box(p,b,light,0,.24+y*.64,-.10,1.35,.50,.04,0x625044);oval(p,b,light,.57,1,-.2,.055,.055,.055,0xB89B58);if(e.cue()==2)for(int side:new int[]{-1,1}){var a=new Vec3(side*.9,.7,.1);var joint=new Vec3(side*1.4,.3,.4);bar(p,b,light,a,joint,.08,0x6A7067);bar(p,b,light,joint,new Vec3(side*1.3,0,.7),.07,0x727B6E);}}
            case RescueProps.CHEESE->{for(int i=0;i<5;i++)box(p,b,light,0,i*.12,0,.8-i*.12,.13,.7,0xDEBE64);}
            case RescueProps.SNOW_GLOBE->{float melt=e.value();oval(p,b,light,0,.12,0,.7,.12,.7,0x8E6852);for(int i=0;i<44;i++){double a=i*2.39,r=(i%7)/10.0,y=.3+(i%11)*.09*(1-melt);box(p,b,15728880,Math.cos(a)*r,y,Math.sin(a)*r,.025,.025,.025,0xFAF3E1);}var glass=buffers.getBuffer(RenderType.entityTranslucent(WHITE));FirstEscapePresentation.sphere(p,glass,light,0,.75,0,.7,.7,.7,0xBDE7F1,.19F);}
            case RescueProps.KEY,RescueProps.STONE->{p.translate(0,.25,0);p.mulPose(Axis.YP.rotationDegrees((float)(t*9)));p.scale(.6F,.6F,.6F);mc.getItemRenderer().renderStatic(new net.minecraft.world.item.ItemStack(e.kind()==RescueProps.KEY?com.coraline.registry.ModItems.LITTLE_DOOR_STORY_KEY.get():com.coraline.registry.ModItems.SEEING_STONE.get()),net.minecraft.world.item.ItemDisplayContext.GROUND,light,OverlayTexture.NO_OVERLAY,p,buffers,e.level(),e.getId());}
            case RescueProps.HAND->{if(e.cue()!=RescueProps.HAND_WAITING_CUE)crawlingHand(e,p,b,light,t);}
            case RescueProps.TONGS->{
                for(int side:new int[]{-1,1}){bar(p,b,light,new Vec3(0,0,.35),new Vec3(side*.07,0,-.35),.035,0x8D9491);bar(p,b,light,new Vec3(side*.07,0,-.35),new Vec3(side*.13,-.04,-.62),.045,0xB2B7AA);}
                oval(p,b,light,0,0,.35,.10,.025,.08,0x363C39);
            }
            case RescueProps.ROCK->{oval(p,b,light,0,.18,0,.33,.25,.28,0x6E7068);box(p,b,light,-.08,.28,-.02,.23,.11,.25,0x85877D);}
            case RescueProps.PHOTO->{
                box(p,b,light,0,0,0,.76,.53,.035,0xDDD0AD);box(p,b,light,0,.035,-.023,.67,.43,.012,0x716F56);
                // Exactly two depicted figures: one human girl and her matching
                // smaller button-eyed doll. No second human/third face/crowd.
                for(int figure=0;figure<2;figure++){
                    double x=figure==0?-.16:.18,scale=figure==0?1:.73,y=.07;
                    box(p,b,light,x,y,-.036,.19*scale,.20*scale,.012,0x7F856B);
                    oval(p,b,light,x,y+.285*scale,-.045,.083*scale,.095*scale,.012,0x3A3127);
                    oval(p,b,light,x,y+.27*scale,-.06,.063*scale,.071*scale,.008,0xCAB58C);
                    for(int side:new int[]{-1,1}){box(p,b,light,x+side*.105*scale,y+.09*scale,-.044,.035*scale,.16*scale,.012,0xCAB58C);box(p,b,light,x+side*.045*scale,y-.055*scale,-.044,.04*scale,.09*scale,.012,0x3D3B30);oval(p,b,light,x+side*.027*scale,y+.28*scale,-.073,(figure==0?.007:.015)*scale,.011*scale,.004,0x24241F);}
                }
            }
            case RescueProps.KEY_BUNDLE->{
                double fold=e.cue()==0?Math.min(1,t/60):1;
                // A folded cloth around the actual rock/hand-fragment bundle.
                for(int side:new int[]{-1,1}){box(p,b,light,side*.31,.03,0,.34,.045,.72,0x9F947F);box(p,b,light,0,.03,side*.31,.72,.045,.34,0xB0A48E);}
                oval(p,b,light,0,.22,0,.28,.21,.25,0x666960);
                for(int fragment=0;fragment<5;fragment++){double a=fragment*2.4;box(p,b,light,Math.cos(a)*.22,.10,Math.sin(a)*.22,.13,.065,.07,0x9E8A78);}
                for(int side:new int[]{-1,1}){bar(p,b,light,new Vec3(side*.42,.06,0),new Vec3(side*.42*(1-fold),.06+.43*fold,0),.13,0xB0A48E);bar(p,b,light,new Vec3(0,.06,side*.42),new Vec3(0,.06+.43*fold,side*.42*(1-fold)),.13,0x9F947F);}
                if(fold>.9){double knot=(fold-.9)*10;oval(p,b,light,0,.48,0,.095*knot,.07*knot,.075*knot,0x8E806A);}
                if(e.value()>0){oval(p,b,light,.17,.43,-.16,.09,.09,.025,0x29272B);bar(p,b,light,new Vec3(.17,.39,-.16),new Vec3(.17,.18,-.16),.035,0xA5946D);}
            }
            default->{}
        }p.popPose();return true;
    }
    private static void crawlingHand(CinematicPropEntity e,PoseStack p,VertexConsumer b,int light,double t){
        boolean grip=e.cue()==11,recover=e.cue()>=34&&(e.cue()-34)%4==2,warning=e.cue()>=34&&(e.cue()-34)%4==0;
        if(e.cue()==20){p.translate(Math.sin(t*1.7)*.06,Math.abs(Math.sin(t*1.3))*.05,Math.cos(t*1.1)*.04);p.mulPose(Axis.ZP.rotationDegrees((float)(Math.sin(t*1.4)*18)));}
        if(warning)p.translate(0,.08+Math.sin(t*.3)*.03,0);
        oval(p,b,light,0,.15,0,.24,.10,.21,recover?0xAD9682:0x9E8A78);
        for(int finger=0;finger<4;finger++){
            double x=(finger-1.5)*.12,lift=grip?.14:Math.max(0,Math.sin(t*.28+finger*1.7))*.12;
            var root=new Vec3(x,.15,-.12);var knuckle=new Vec3(x*1.10,.11+lift,-.32);var joint=new Vec3(x*1.24,.08+lift,-.48);
            var tip=new Vec3(x*1.40,grip?.20:.015,grip?-.40:-.62+Math.abs(finger-1.5)*.04);
            bar(p,b,light,root,knuckle,.065,0x9E8A78);oval(p,b,light,knuckle.x,knuckle.y,knuckle.z,.047,.044,.045,0x756259);
            bar(p,b,light,knuckle,joint,.052,0x9E8A78);bar(p,b,light,joint,tip,.042,0x887165);oval(p,b,light,tip.x,tip.y+.01,tip.z,.025,.012,.043,0x303C36);
        }
        bar(p,b,light,new Vec3(-.18,.16,.06),new Vec3(-.36,.08,-.07),.085,0x887165);bar(p,b,light,new Vec3(-.36,.08,-.07),new Vec3(-.42,.02,-.22),.065,0x9E8A78);
        for(int wire=0;wire<3;wire++)bar(p,b,light,new Vec3((wire-1)*.07,.14,.12),new Vec3((wire-1)*.10,.14+Math.sin(t*.2+wire)*.025,.34),.018,0x766B58);
    }
    private static void furnace(CinematicPropEntity e,PoseStack p,VertexConsumer b,int light){
        // A real open chamber, not a black rectangle on a solid ellipsoid.
        box(p,b,light,0,0,0,2.2,.18,1.8,0x24252D);box(p,b,light,0,.18,.81,2.2,2.05,.18,0x25252D);
        for(int side:new int[]{-1,1}){box(p,b,light,side, .18,0,.2,2.05,1.8,0x383940);for(int rivet=0;rivet<6;rivet++)oval(p,b,light,side*1.01,.3+rivet*.34,-.92,.045,.045,.035,0x776954);bar(p,b,light,new Vec3(side*.75,.1,.5),new Vec3(side*.98,-.15,.65),.15,0x333039);}
        box(p,b,light,0,2.23,0,2.2,.22,1.8,0x45434A);box(p,b,light,0,2.45,.35,.42,1.8,.42,0x302E36);box(p,b,15728880,0,.20,.50,1.4,.04,.32,0x584A22);
        p.pushPose();p.translate(-.90,0,-.94);p.mulPose(Axis.YP.rotationDegrees(Math.max(0,Math.min(1,e.value()))*108));box(p,b,light,.90,.18,0,1.8,2.05,.12,0x36353C);box(p,b,light,.90,.35,-.075,1.48,1.67,.04,0x1D1D25);oval(p,b,light,1.56,1.08,-.15,.07,.07,.07,0xA28B5E);p.popPose();
    }
    private static void rat(PoseStack p,VertexConsumer b,int l,double t){box(p,b,l,0,.06,0,.23,.17,.40,0x38343B);box(p,b,l,0,.12,-.22,.19,.16,.18,0x46414A);for(int side:new int[]{-1,1}){box(p,b,l,side*.10,.22,-.19,.09,.10,.05,0x71616C);box(p,b,15728880,side*.085,.17,-.31,.025,.025,.025,0xBB493E);for(int end:new int[]{-1,1})box(p,b,l,side*.10,.02+Math.max(0,Math.sin(t*.3+side+end))*.04,end*.14,.06,.05,.10,0x7B626A);}bar(p,b,l,new Vec3(0,.12,.18),new Vec3(Math.sin(t*.04)*.1,.06,.55),.025,0x8E737A);}
    private static void mantis(CinematicPropEntity e,PoseStack p,VertexConsumer b,int light,double t){
        p.pushPose();double beat=RescueClient.value("BossBeat");int pattern=(int)RescueClient.value("BossPattern");
        double strike=e.cue()==3?Math.sin(Math.PI*Math.max(0,Math.min(1,(beat-40)/35.0))):0;
        if(e.cue()==3){p.translate(0,pattern==2?strike*.35:0,0);p.mulPose(Axis.XP.rotationDegrees((float)(pattern==1?strike*18:pattern==0?-strike*10:0)));}
        oval(p,b,light,0,1,0,.65,.40,1.3,0x315F57);oval(p,b,light,0,1.3,-1,.5,.32,.45,0x7A9165);for(int side:new int[]{-1,1}){oval(p,b,15728880,side*.32,1.4,-1.3,.13,.13,.10,0xC1DB76);for(int leg=0;leg<3;leg++){double z=(leg-1)*.85,step=Math.sin(t*.17+leg*2+side)*.22;var root=new Vec3(side*.45,1,z);var joint=new Vec3(side*1.5,.6,z+step);var foot=new Vec3(side*2.1,.03,z-.2-step);bar(p,b,light,root,joint,.12,0x4E7163);bar(p,b,light,joint,foot,.075,0x292F36);oval(p,b,light,joint.x,joint.y,joint.z,.16,.16,.16,0xB58F51);}
            double reach=e.cue()==3&&(pattern==0||pattern==3)?strike*1.25:0;var elbow=new Vec3(side*1.2,1.9,-1.6-reach);bar(p,b,light,new Vec3(side*.4,1.35,-.8),elbow,.16,0x44796A);bar(p,b,light,elbow,new Vec3(side*.55,1.1,-2.4-reach),.11,0x97A476);for(int tooth=0;tooth<5;tooth++)bar(p,b,light,new Vec3(side*(.58+tooth*.10),1.15+tooth*.12,-2.3+tooth*.13-reach),new Vec3(side*.35,1+tooth*.12,-2.3+tooth*.13-reach),.035,0xD2C491);
            for(int wire=0;wire<2;wire++)bar(p,b,light,new Vec3(side*.6,1.3,wire*.7),new Vec3(side*.35,2.0,-.1+wire*.3),.035,0x776653);
        }box(p,b,light,0,1.35,.2,.65,.12,.8,0x846C4B);p.popPose();
    }
    private static void web(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light){var b=buffers.getBuffer(RenderType.entityTranslucent(WHITE));var l=e.level();var c=e.blockPosition();
        // Draw the actual collision tops, so every thick strand seen is one that can hold weight.
        if(!e.getUUID().equals(webId)||l.getGameTime()-webRefresh>=5){
            var positions=new java.util.HashSet<net.minecraft.core.BlockPos>();var sticky=new java.util.HashSet<net.minecraft.core.BlockPos>();
            for(int lane=0;lane<3;lane++)for(int sample=0;sample<=RescueWebLayout.SAMPLES;sample++){
                double u=sample/(double)RescueWebLayout.SAMPLES;var q=RescueWebLayout.point(lane,u);var cell=net.minecraft.core.BlockPos.containing(q.x(),q.y()-.001,q.z());positions.add(cell);if(RescueWebLayout.sticky(lane,sample))sticky.add(cell);
                if(sample%150==0&&sample>0&&sample<900&&lane<2){var other=RescueWebLayout.point(lane+1,u);for(int j=0;j<=12;j++){double k=j/12.;positions.add(net.minecraft.core.BlockPos.containing(q.x()+(other.x()-q.x())*k,q.y()-.001,q.z()+(other.z()-q.z())*k));}}
            }
            for(double u:new double[]{0,1}){var q=RescueWebLayout.point(1,u);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)positions.add(net.minecraft.core.BlockPos.containing(q.x()+x,q.y()-.001,q.z()+z));}
            var cells=new java.util.ArrayList<WebCell>();for(var local:positions){var state=l.getBlockState(c.offset(local));if(state.is(com.coraline.registry.ModBlocks.STORY_WEB.get()))cells.add(new WebCell(local.getX(),local.getY(),local.getZ(),local.getY()+state.getValue(com.coraline.block.StoryWebBlock.HEIGHT)/16.0,sticky.contains(local)));}webCells=java.util.List.copyOf(cells);webId=e.getUUID();webRefresh=l.getGameTime();
        }
        for(var cell:webCells){int x=cell.x,z=cell.z;double top=cell.top;quad(p,b,15728880,new Vec3(x,top,z),new Vec3(x+1,top,z),new Vec3(x+1,top,z+1),new Vec3(x,top,z+1),cell.sticky?0x596A69:0xB8C6C5,.60F);for(double side:new double[]{.08,.40,.72})bar(p,b,light,new Vec3(x+side,top+.005,z),new Vec3(x+side+.15,top+.005,z+1),cell.sticky?.029:.018,cell.sticky?0xAD8B53:0xCFD9D4);}
        // Thin radial filaments are visibly unsafe: no collision blocks below them.
        for(int ray=0;ray<24;ray++){double a=ray*Math.PI/12;for(int seg=0;seg<10;seg++){double r=seg*3.3,next=r+3.3;bar(p,b,light,new Vec3(Math.cos(a)*r,7+seg*.7,Math.sin(a)*r),new Vec3(Math.cos(a)*next,7+(seg+1)*.7,Math.sin(a)*next),.022,0x55616A);}}
    }
    public static boolean mother(ParentEntity actor,float yaw,float partial,PoseStack p,MultiBufferSource buffers,int light){
        boolean accepted=com.coraline.client.AcceptedButtonEyesClient.beldam(actor.getId());
        if(!accepted&&actor.getId()!=RescueClient.mother()||actor.isInvisible())return false;float morph=accepted?1:RescueClient.value("BeldamMorph");if(RescueClient.phase().equals("FINAL_P"))morph=Math.max(morph,Math.min(1,(RescueClient.time()+partial)/Math.max(1,RescueClient.duration())));if(morph<=0)return false;
        var mc=Minecraft.getInstance();if(mc.level==null)return false;if(beldam==null)beldam=new CharacterModel("beldam");if(proxy==null||proxy.level()!=mc.level)proxy=com.coraline.registry.CharacterCast.ENTITIES.get("beldam").get().create(mc.level);if(proxy==null)return false;
        beldam.setupAnim(proxy,0,0,actor.tickCount+partial,0,0);beldam.rescuePose(actor.familyGesture(),actor.tickCount+partial);beldam.storyBlind(RescueClient.flag("BeldamBlind"));p.pushPose();p.translate(0,1.501,0);p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.scale(-1,-1,1);
        if(morph<1){if(transitionSource==null)transitionSource=new CharacterModel("evil_other_mother");transitionSource.setupAnim(proxy,0,0,actor.tickCount+partial,0,0);transitionSource.rescuePose(actor.familyGesture(),actor.tickCount+partial);beldam.renderRigTransition(transitionSource,p,buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)),light,OverlayTexture.NO_OVERLAY,morph);}
        else beldam.renderSpeech(p,buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS)),light,OverlayTexture.NO_OVERLAY,1,1,1,1,com.coraline.client.VisitAudioClient.voiceLevel(accepted?"other_mother_incidental_":"finale_"));
        beldam.storyBlind(false);p.popPose();return true;
    }
    private RescuePresentation(){}
}
