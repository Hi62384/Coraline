package com.coraline.client.renderer;

import com.coraline.client.FirstEscapeClient;
import com.coraline.entity.CharacterEntity;
import com.coraline.feast.CinematicPropEntity;
import com.coraline.feast.RescueChallengeMath;
import com.coraline.parents.ParentEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Native articulated insect furniture; no billboard substitutes for story actors. */
public final class FirstEscapePresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png"),WALL=ResourceLocation.fromNamespaceAndPath("coraline","textures/block/insect_parlor_wallpaper.png"),ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    private static CharacterModel evil;private static CharacterEntity proxy;
    private static final int[][] QUAD={{0,0},{1,0},{1,1},{0,1}};
    // The same 128 quads and normals as the authored procedural ellipsoid.
    // Cache only unit coordinates, so animation/scale/color remain unchanged.
    private static final double[][] SPHERE_UNIT=sphereUnit();
    private static double[][] sphereUnit(){var unit=new double[8*16*4][3];int i=0;for(int ring=0;ring<8;ring++)for(int side=0;side<16;side++)for(int[] point:QUAD){double a=-Math.PI/2+(ring+point[0])*Math.PI/8,t=(side+point[1])*Math.PI/8;unit[i++]=new double[]{Math.cos(a)*Math.cos(t),Math.sin(a),Math.cos(a)*Math.sin(t)};}return unit;}
    private static VertexConsumer solid(MultiBufferSource b){return b.getBuffer(RenderType.entityCutoutNoCull(WHITE));}
    private static void box(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double w,double h,double d,int color){CinematicPropRenderer.box(p,b,l,x,y,z,w,h,d,color);}
    private static void vertex(PoseStack p,VertexConsumer b,int light,double x,double y,double z,double nx,double ny,double nz,int color,float alpha,float u,float v){b.vertex(p.last().pose(),(float)x,(float)y,(float)z).color(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,alpha).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),(float)nx,(float)ny,(float)nz).endVertex();}
    static void sphere(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double rx,double ry,double rz,int color,float alpha){
        for(var point:SPHERE_UNIT){double nx=point[0],ny=point[1],nz=point[2];vertex(p,b,l,x+nx*rx,y+ny*ry,z+nz*rz,nx,ny,nz,color,alpha,.5F,.5F);}
    }
    private static void limb(PoseStack p,VertexConsumer b,int l,Vec3 a,Vec3 c,double width,int color){var d=c.subtract(a);if(d.lengthSqr()<.00001)return;p.pushPose();p.translate(a.x,a.y,a.z);p.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),new org.joml.Vector3f((float)d.x,(float)d.y,(float)d.z).normalize()));box(p,b,l,0,0,0,width,d.length(),width,color);p.popPose();}
    private static void legs(PoseStack p,VertexConsumer b,int l,double time,int count,double radius,double baseY,double stride){for(int side:new int[]{-1,1})for(int i=0;i<count;i++){double z=(i-(count-1)*.5)*.6,swing=Math.sin(time*.2+i*2.2+side)*stride;var a=new Vec3(side*radius,baseY,z);var joint=new Vec3(side*(radius+.38),baseY*.55+.12,z+swing);var toe=new Vec3(side*(radius+.65),.035,z+.15-swing);limb(p,b,l,a,joint,.065,0x301E2A);limb(p,b,l,joint,toe,.055,0x251721);sphere(p,b,l,joint.x,joint.y,joint.z,.08,.08,.08,0x70443B,1);}}
    public static void companionEffects(CharacterEntity actor,PoseStack pose,MultiBufferSource buffers,int light,float partial){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        if(actor.getId()==FirstEscapeClient.realWybie()&&FirstEscapeClient.phase().equals("MISSING_F")){
            pose.pushPose();pose.translate(0,actor.getBbHeight()+.5+Math.sin((actor.tickCount+partial)*.09)*.08,0);pose.mulPose(Axis.YP.rotationDegrees((actor.tickCount+partial)*2));pose.scale(.38F,.38F,.38F);pose.translate(-.5,0,-.5);
            mc.getBlockRenderer().renderSingleBlock(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(),pose,buffers,15728880,OverlayTexture.NO_OVERLAY);pose.popPose();
        }
        if(actor.getId()!=FirstEscapeClient.wybie()||!(mc.player.getVehicle() instanceof CinematicPropEntity grip)||grip.kind()!=CinematicPropEntity.GRAB_CARRIER||grip.cue()!=2)return;
        var held=mc.player.position().subtract(actor.position()).add(0,1.1,0);
        if(held.lengthSqr()>9)return;
        var sleeve=solid(buffers);double yaw=Math.toRadians(actor.getYRot());
        var elbow=new Vec3(Math.cos(yaw)*.25,1.15,Math.sin(yaw)*.25);
        var wrist=held.add(0,0,-.08);
        limb(pose,sleeve,light,elbow,wrist,.075,0x314964);
        sphere(pose,sleeve,light,wrist.x,wrist.y,wrist.z,.10,.07,.12,0xB49470,1);
    }
    public static boolean render(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light,float partial){if(e.kind()<CinematicPropEntity.STORY_CANE)return false;var b=solid(buffers);double time=e.visualTime(partial);int glow=light;p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        String mesh=switch(e.kind()){case CinematicPropEntity.ROACH_WARDROBE->"wardrobe";case CinematicPropEntity.BUG_CHAIR->"chair";case CinematicPropEntity.BUG_COUCH->"couch";case CinematicPropEntity.BUG_TABLE->"table";case CinematicPropEntity.BUG_LAMP->"lamp";case CinematicPropEntity.BUG_CHANDELIER->"chandelier";default->"";};
        if(!mesh.isEmpty()){
            p.pushPose();if(e.kind()==CinematicPropEntity.ROACH_WARDROBE&&e.cue()==3){p.translate(0,.08,.6);p.mulPose(Axis.XP.rotationDegrees(e.value()*-87));p.translate(0,0,-.6);}
            if(e.kind()==CinematicPropEntity.BUG_LAMP&&e.cue()==4){p.translate(0,.10,0);p.mulPose(Axis.ZP.rotationDegrees(-23*com.coraline.feast.CocoaBeetlesChoreography.lampLean(time)));p.translate(0,-.10,0);}
            boolean rendered=InsectFurnitureMesh.render(mesh,p,buffers,light,time,e.kind()==CinematicPropEntity.BUG_CHAIR&&e.cue()==2||e.kind()==CinematicPropEntity.ROACH_WARDROBE&&e.cue()==1?.09F:.008F,e.value());p.popPose();
            if(rendered){p.popPose();return true;}
        }
        switch(e.kind()){
            case CinematicPropEntity.STORY_CANE->{p.translate(0,.03,0);p.mulPose(Axis.ZP.rotationDegrees(90));box(p,b,light,0,0,0,.055,1.15,.055,0x573E2C);for(int i=0;i<11;i++){double a=i*Math.PI/10; sphere(p,b,light,.12-Math.cos(a)*.12,1.15+Math.sin(a)*.12,0,.034,.034,.034,0x664B33,1);}box(p,b,light,0,-.03,0,.065,.07,.065,0x1B1920);}
            case CinematicPropEntity.ROACH_WARDROBE->wardrobe(e,p,b,buffers,light,time);
            case CinematicPropEntity.WATER_BUCKET->CinematicPropRenderer.waterBucket(p,buffers,light,time);
            case CinematicPropEntity.BUG_CHAIR->{legs(p,b,light,time,2,.35,.5,e.cue()==2?.13:.015);sphere(p,b,light,0,.48,0,.52,.18,.53,0x82334B,1);for(int i=0;i<4;i++)sphere(p,b,glow,0,.75+i*.28,.36,.48-i*.055,.2,.22,0x903C91+i*0x090103,1);for(int side:new int[]{-1,1})sphere(p,b,glow,side*.18,1.72,.23,.12,.09,.09,0xD9B851,1);}
            case CinematicPropEntity.BUG_COUCH->{legs(p,b,light,time,3,1.2,.6,.02);sphere(p,b,light,0,.52,0,1.5,.28,.7,0xC32930,1);for(int i=-3;i<=3;i++){sphere(p,b,glow,i*.4,1.15,.46,.36,.92,.4,0xAA1D38+Math.abs(i)*0x040201,1);sphere(p,b,glow,i*.4,1.98,.48,.2,.2,.18,0x27758A,1);}for(int side:new int[]{-1,1})sphere(p,b,light,side*1.45,.85,0,.35,.6,.68,0xAD2832,1);}
            case CinematicPropEntity.BUG_TABLE->{legs(p,b,light,time,3,.65,.6,.018);sphere(p,b,glow,0,.65,0,1.05,.2,.55,0x277F72,1);for(int i=-2;i<=2;i++)sphere(p,b,glow,i*.32,.83,0,.16,.025,.3,0xB7A63B,1);sphere(p,b,glow,0,.82,-.28,.26,.04,.15,0xDDC957,1);}
            case CinematicPropEntity.BUG_LAMP->{sphere(p,b,light,0,.1,0,.45,.1,.32,0x221B24,1);var a=new Vec3(0,.15,0);var c=new Vec3(.1,1.5,0);limb(p,b,light,a,c,.08,0x234642);limb(p,b,light,c,new Vec3(.55,2.1,0),.07,0x1D5A68);sphere(p,b,glow,.65,2.05,0,.32,.15,.14,0x43C8D3,1);for(int side:new int[]{-1,1})limb(p,b,glow,new Vec3(.65,2,0),new Vec3(.5,1.7,side*.5),.04,0xDC9C49);}
            case CinematicPropEntity.BUG_CHANDELIER->{limb(p,b,light,new Vec3(0,.25,0),new Vec3(0,1.3,0),.055,0x30232D);sphere(p,b,glow,0,.18,0,.3,.45,.3,0x77632B,1);for(int i=0;i<7;i++){double a=i*Math.PI*2/7,x=Math.cos(a)*(1+i%2*.25),z=Math.sin(a)*(1+i%2*.25);limb(p,b,light,new Vec3(0,0,0),new Vec3(x,-.75,z),.035,0x35252D);sphere(p,b,glow,x,-.87,z,.12,.22,.12,i%2==0?0xB4D53B:0xE7C451,1);}}
            case CinematicPropEntity.BEETLE_BOX->beetleTray(e,p,b,light);
            case CinematicPropEntity.INSECT_PARLOR->parlor(e,p,buffers,glow);
            case CinematicPropEntity.PRISON_BED->{for(int side:new int[]{-1,1})for(int end:new int[]{-1,1})box(p,b,light,side*.65,.04,end*.95,.09,.6,.09,0x35363C);box(p,b,light,0,.28,0,1.4,.15,2.2,0x39353B);sphere(p,b,e.value()>0?15728880:light,0,.54,0,.68,.15,1.0,e.value()>0?0x346B44:0x55575D,1);box(p,b,light,0,.52,.75,1.15,.16,.4,0x76776B);}
            case CinematicPropEntity.GHOST_CHILD->{if(e.value()>0||e.cue()==3)ghost(e,p,buffers,partial);}
            case CinematicPropEntity.PRISON_HEAD->{if(e.value()>0){p.translate(0,-2.0,com.coraline.feast.FirstEscapeVisualRules.prisonHeadOffset(e.value()));evil(p,buffers,15728880,1,true,0,time,0,net.minecraft.world.item.ItemStack.EMPTY);}}
            case CinematicPropEntity.STORY_MIRROR->{if(e.slot()<4)p.scale(.64F,.72F,1);box(p,b,light,0,.1,0,2.1,3.5,.12,0x5B3337);for(int side:new int[]{-1,1})box(p,b,light,side*1.02,.15,-.08,.12,3.4,.1,0xB28E56);box(p,b,light,0,3.5,-.08,2.2,.18,.15,0xB28E56);box(p,b,glow,0,.25,-.09,1.85,3.1,.01,e.value()>0?0x447A73:0x4F5361);}
            case CinematicPropEntity.VICTIM_SUPPER_TABLE->{
                box(p,b,light,0,.74,0,2.9,.16,2.8,0x664027);
                box(p,b,light,0,.90,0,.62,.018,2.76,0x36624F);
                for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})
                    box(p,b,light,x*1.21,0,z*1.17,.17,.75,.17,0x432C20);
            }
            case CinematicPropEntity.STORY_SHOE->{if(e.cue()!=1)p.mulPose(Axis.XP.rotationDegrees((float)(e.time()*13)));Minecraft.getInstance().getItemRenderer().renderStatic(new net.minecraft.world.item.ItemStack(com.coraline.feast.FirstEscapeItems.SHOE.get()),net.minecraft.world.item.ItemDisplayContext.GROUND,light,OverlayTexture.NO_OVERLAY,p,buffers,e.level(),e.getId());}
            case CinematicPropEntity.GRAB_CARRIER->{if(com.coraline.feast.FirstEscapeVisualRules.rescueHand(FirstEscapeClient.phase(),FirstEscapeClient.flag("Rescued"),e.cue())){limb(p,b,light,new Vec3(.8,1.15,-.7),new Vec3(.15,1.1,-.18),.13,0x314964);sphere(p,b,light,.12,1.1,-.15,.13,.08,.17,0xB49470,1);for(int i=0;i<4;i++)sphere(p,b,light,.03+i*.055,1.09,-.02,.035,.035,.09,0xB49470,1);}}
            default->{}
        }
        if(e.kind()==CinematicPropEntity.BUG_CHANDELIER)for(int i=0;i<7;i++){double angle=i*Math.PI*2/7;bulb(p,buffers,Math.cos(angle)*(1+i%2*.25),-.87,Math.sin(angle)*(1+i%2*.25),.125,.225,.125,i%2==0?0xCEEF61:0xFFE487,e.value());}
        if(e.kind()==CinematicPropEntity.BUG_TABLE)for(int i=-2;i<=2;i++)bulb(p,buffers,i*.32,.856,0,.115,.018,.24,0xD2EA63,e.value());
        if(e.kind()==CinematicPropEntity.BUG_LAMP)bulb(p,buffers,.65,2.05,0,.325,.155,.145,0x83EEEF,e.value());
        if(e.kind()==CinematicPropEntity.BUG_CHAIR){
            for(int side:new int[]{-1,1}){
                sphere(p,b,light,side*.18,1.72,.14,.082,.073,.025,0x231C29,1);
                for(int hole=0;hole<4;hole++)bulb(p,buffers,side*.18+(hole%2==0?-.023:.023),1.72+(hole<2?-.022:.022),.112,.009,.01,.006,0xEED78F,e.value());
                b=solid(buffers); // Glow changed the shared fallback buffer's render type.
                limb(p,b,light,new Vec3(side*.15,1.82,.35),new Vec3(side*.35,2.03,.35),.018,0x382338);
                sphere(p,b,light,side*.47,.68,.08,.13,.10,.43,0xB34C85,1);
            }
        }
        if(e.kind()==CinematicPropEntity.BUG_COUCH){
            limb(p,b,light,new Vec3(-1.3,.72,-.46),new Vec3(1.3,.72,-.46),.035,0xE0A25F);
            limb(p,b,light,new Vec3(-1.35,.32,0),new Vec3(1.35,.32,0),.09,0x492432);
        }
        if(e.kind()==CinematicPropEntity.BUG_TABLE){
            for(int side:new int[]{-1,1})for(int vein=0;vein<3;vein++)limb(p,b,light,new Vec3(0,.852,vein*.14-.18),new Vec3(side*.68,.79,vein*.14-.22),.014,0x365A51);
            sphere(p,b,light,0,.72,-.49,.27,.13,.21,0x9EA54C,1);
            for(int side:new int[]{-1,1})sphere(p,b,light,side*.105,.77,-.65,.042,.045,.028,0x211C29,1);
        }
        p.popPose();return true;
    }
    private static void bulb(PoseStack p,MultiBufferSource buffers,double x,double y,double z,double rx,double ry,double rz,int color,float reveal){
        if(reveal<=0)return;float brightness=.12F+.88F*Math.min(1,reveal);
        int toned=((int)(((color>>16)&255)*brightness)<<16)|((int)(((color>>8)&255)*brightness)<<8)|(int)((color&255)*brightness);
        sphere(p,buffers.getBuffer(RenderType.eyes(WHITE)),15728880,x,y,z,rx,ry,rz,toned,1);
    }
    private static void wardrobe(CinematicPropEntity e,PoseStack p,VertexConsumer b,MultiBufferSource buffers,int light,double time){
        if(e.cue()==3){p.translate(0,.08,.6);p.mulPose(Axis.XP.rotationDegrees(e.value()*-87));p.translate(0,0,-.6);}
        // A continuous tapered carapace, not a pile of disconnected sausages.
        for(int ring=0;ring<16;ring++)for(int side=0;side<24;side++)for(int[] point:QUAD){
            double y=.30+(ring+point[0])*2.85/16,t=(side+point[1])*Math.PI/12;
            double profile=Math.pow(Math.max(.015,Math.sin((y-.22)/3.05*Math.PI)),.38),nx=Math.cos(t),nz=Math.sin(t);
            vertex(p,b,light,nx*profile*1.02,y,nz*profile*.64,nx,0,nz,0xA85D4D,1,.5F,.5F);
        }
        // Paired cupboard doors, thin drawer reveals, and brass-black knobs.
        for(int i=0;i<5;i++){double y=.55+i*.49,width=.46+Math.sin((y-.22)/3.05*Math.PI)*.39;
            limb(p,b,light,new Vec3(-width,y,-.565),new Vec3(width,y,-.565),.021,0x583239);
            for(int side:new int[]{-1,1})sphere(p,b,light,side*(i>=3?.12:0),y+.14,-.645,.045,.045,.034,0x251C25,1);
        }
        limb(p,b,light,new Vec3(0,2.04,-.65),new Vec3(0,2.99,-.49),.019,0x583239);
        legs(p,b,light,time,3,.77,.86,e.cue()==1?.12:0);
        for(int side:new int[]{-1,1})for(int i=0;i<3;i++){double z=(i-1)*.6;
            limb(p,b,light,new Vec3(side*1.42,.035,z+.15),new Vec3(side*1.52,.035,z+.31),.027,0x211722);
            limb(p,b,light,new Vec3(side*1.42,.035,z+.15),new Vec3(side*1.55,.035,z+.09),.027,0x211722);
        }
        sphere(p,b,light,0,3.28,-.05,.49,.30,.39,0xBE963F,1);
        for(int side:new int[]{-1,1}){
            sphere(p,b,light,side*.285,3.30,-.39,.205,.22,.10,0xD2AB51,1);
            sphere(p,b,light,side*.285,3.30,-.478,.145,.16,.033,0x191A23,1);
            for(int hole=0;hole<4;hole++)bulb(p,buffers,side*.285+(hole%2==0?-.045:.045),3.30+(hole<2?-.05:.05),-.515,.019,.021,.009,0xDCF18E,e.value());
            var previous=new Vec3(side*.20,3.51,-.10);
            for(int segment=1;segment<=7;segment++){double a=segment/7.;var next=new Vec3(side*(.20+.34*a),3.51+.7*a,-.10-.12*Math.sin(a*Math.PI));limb(p,b,light,previous,next,.026,0x30222E);previous=next;}
        }
    }
    private static void beetle(PoseStack p,VertexConsumer b,int light,double t){sphere(p,b,light,0,.25,0,.35,.2,.5,0x593D2D,1);for(int i=0;i<4;i++)box(p,b,light,0,.4,-.3+i*.2,.025,.025,.025,0x2B201C);sphere(p,b,light,0,.28,-.5,.23,.18,.2,0x35291F,1);legs(p,b,light,t,3,.25,.25,.07);for(int s:new int[]{-1,1})limb(p,b,light,new Vec3(s*.12,.3,-.6),new Vec3(s*.2+Math.sin(t*.2)*.04,.45,-.9),.02,0x302520);}
    private static void beetleTray(CinematicPropEntity e,PoseStack p,VertexConsumer b,int light){
        box(p,b,light,0,0,0,.90,.045,.72,0x342339);box(p,b,light,0,.045,0,.85,.014,.67,0xD2B78F);
        for(int side:new int[]{-1,1}){box(p,b,light,side*.44,.045,0,.025,.13,.72,0x79513B);box(p,b,light,0,.045,side*.35,.90,.13,.025,0x79513B);}
        float open=e.cue()>=2?e.value():0;p.pushPose();p.translate(0,.18,.36);p.mulPose(Axis.XP.rotationDegrees(open*112));box(p,b,light,0,0,-.36,.94,.035,.75,0x33223A);box(p,b,light,0,.036,-.36,.88,.008,.69,0x704C3B);sphere(p,b,light,0,.049,-.36,.12,.008,.065,0xCAA965,1);p.popPose();
        if(open<.20F)return;
        for(int i=0;i<9;i++){
            p.pushPose();p.translate((i%3-1)*.275,.065,(i/3-1)*.225);
            // Fluted cream paper cups remain after a beetle is taken.
            for(int fold=0;fold<12;fold++){double a=fold*Math.PI/6;box(p,b,light,Math.cos(a)*.105,.015,Math.sin(a)*.09,.028,.055,.028,fold%2==0?0xE1CCAA:0xF0DFBD);}
            if(e.cue()<3||i!=4){p.pushPose();p.scale(.13F,.13F,.13F);beetle(p,b,light,0);limb(p,b,light,new Vec3(0,.44,-.33),new Vec3(0,.44,.32),.022,0xB38B42);p.popPose();}p.popPose();
        }
    }
    private static void parlor(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light){var b=solid(buffers);int wood=0x39202D,gold=0x987244;
        for(int index=0;index<2;index++){
            int side=index==0?-1:1;double x=side*8.30,z=-3.1;
            limb(p,b,light,new Vec3(x,.08,z),new Vec3(x,2.8,z),.034,0x292031);
            sphere(p,b,light,x,2.84,z,.12,.35,.17,0x6C2744,1);
            for(int wing=0;wing<3;wing++){
                double a=(wing-1)*.45;Vec3 base=new Vec3(x,2.78,z),tip=new Vec3(x-side*(.3+wing*.08),3.72-Math.abs(a)*.35,z+Math.sin(a)*.55);
                limb(p,b,light,base,tip,.025,0x6E2737);sphere(p,b,light,(base.x+tip.x)*.5,(base.y+tip.y)*.5,(base.z+tip.z)*.5,.065,.31,.10,0xA13C45,1);
            }
            bulb(p,buffers,x-side*.08,3.12,z-.015,.11,.25,.12,0xFFAE39,com.coraline.feast.CocoaBeetlesChoreography.sconce(e.visualTime(0),index));
            b=solid(buffers); // Never emit the remaining room into the additive eye pass.
        }
        // One tessellated rug surface above both planks and vanilla carpet. Adjacent
        // rings share an edge, not overlapping quads or intersecting raised rods.
        double[] radii={0,3.2,3.26,3.40,3.46,3.58,3.64};
        for(int ring=0;ring<radii.length-1;ring++)for(int i=0;i<64;i++){
            double a=i*Math.PI/32,c=(i+1)*Math.PI/32,inner=radii[ring],outer=radii[ring+1];
            int color=ring==0?(i%2==0?0xA96532:0x68382A):ring%2==1?gold:wood;
            for(double[] point:new double[][]{{inner,a},{outer,a},{outer,c},{inner,c}})
                vertex(p,b,light,Math.cos(point[1])*point[0],.085,Math.sin(point[1])*point[0],0,1,0,color,1,.5F,.5F);
        }
        for(int x:new int[]{-4,0,4})curtain(p,b,light,x);
        // Repeating burgundy cornice ties the three bays together.
        box(p,b,light,0,5.64,5.36,15,.20,.24,0x302033);box(p,b,light,0,5.54,5.18,15,.035,.045,0x82404C);
        for(int i=-14;i<=14;i++)sphere(p,b,light,i*.5,5.62,5.18,.16,.065,.022,0x713443,1);
        // Recessed warm hearth, iron tendrils and oval insect ornament.
        box(p,b,light,-2,0,5.1,2.1,.25,.75,wood);box(p,b,light,-2,.25,5.3,1.8,1.45,.25,0x211820);box(p,b,light,-2,1.7,5.1,2.2,.18,.75,wood);sphere(p,b,light,-2,3.2,5.2,.5,.75,.07,gold,1);sphere(p,b,light,-2,3.2,5.1,.41,.62,.05,0x4E2E42,1);for(int i=0;i<5;i++){double flicker=.3+Math.sin((e.tickCount+i*7)*.17)*.1;sphere(p,b,e.value()>.1?15728880:light,-2+(i-2)*.22,.5+flicker,4.98,.12,flicker,.12,0xE57A2D,1);limb(p,b,light,new Vec3(-2+(i-2)*.3,.3,4.8),new Vec3(-2+(i-2)*.12,1.4,4.8),.035,0x39202D);}
        for(int side:new int[]{-1,1})for(int leg=0;leg<4;leg++){double y=2.8+leg*.22;limb(p,b,light,new Vec3(-2+side*.39,y,5.15),new Vec3(-2+side*(.68+leg*.07),y+(leg-1.5)*.24,5.14),.025,wood);limb(p,b,light,new Vec3(-2+side*(.68+leg*.07),y+(leg-1.5)*.24,5.14),new Vec3(-2+side*.61,y+(leg-1.5)*.48,5.14),.022,wood);}
    }
    private static double curtainOpening(double t){return .29+.19*Math.cos(t*Math.PI*3)+.12*t;}
    private static void curtain(PoseStack p,VertexConsumer b,int light,double x){
        box(p,b,light,x,1.2,5.48,1.85,3.85,.12,0x251927);
        for(int pleat=0;pleat<12;pleat++)box(p,b,light,x-.82+pleat*.15,1.22,5.37,.065,3.8,.075,pleat%2==0?0x39202D:0x251A28);
        for(int side:new int[]{-1,1})for(int segment=0;segment<24;segment++){
            double t=segment/24.,u=(segment+1)/24.,y=1.2+t*3.65,top=1.2+u*3.65,a=curtainOpening(t),c=curtainOpening(u);
            for(double[] v:new double[][]{{x+side*a,y},{x+side*.96,y},{x+side*.96,top},{x+side*c,top}})vertex(p,b,light,v[0],v[1],5.27,0,0,-1,segment%6<3?0x7F303F:0x742B3D,1,.5F,.5F);
            limb(p,b,light,new Vec3(x+side*a,y,5.245),new Vec3(x+side*c,top,5.245),.022,0xC3A15B);
        }
        box(p,b,light,x,4.85,5.22,2.10,.26,.25,0x693143);box(p,b,light,x,5.13,5.23,2.24,.065,.29,0xAE7955);
        for(int segment=0;segment<24;segment++){
            double a=-1+segment/12.,c=-1+(segment+1)/12.,ya=4.68+.14*Math.cos(a*Math.PI*2),yc=4.68+.14*Math.cos(c*Math.PI*2);
            for(double[] v:new double[][]{{x+a,ya},{x+a,4.88},{x+c,4.88},{x+c,yc}})vertex(p,b,light,v[0],v[1],5.06,0,0,-1,0x693143,1,.5F,.5F);
            limb(p,b,light,new Vec3(x+a,ya,5.04),new Vec3(x+c,yc,5.04),.018,0xC3A15B);
        }
    }
    private static void ghost(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,float partial){
        var b=buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));double t=e.visualTime(partial);boolean freed=e.cue()>=3,sad=com.coraline.feast.FirstEscapeVisualRules.ghostSad(e.cue(),com.coraline.client.RescueClient.phase().equals("FAREWELL_C"),com.coraline.client.RescueClient.time()),headOnly=e.cue()==2;
        int color=freed?(e.slot()==0?0xFFE9A8:e.slot()==1?0xF5D68A:0xFDEBC3):e.slot()==0?0x64FFA0:e.slot()==1?0x7CFFAE:0x56F5BA,hair=freed?0xCBA85D:e.slot()==0?0x60946B:e.slot()==1?0x608E71:0x5B967A;
        float alpha=freed?.54F:.72F;if(e.cue()==3){float u=e.value();sphere(p,b,15728880,0,.9,0,.24*(1-u)+.03,.24*(1-u)+.03,.24*(1-u)+.03,0xFFE9A8,1-u);if(u<.12F)return;alpha*=u;}
        if(!freed){
            // The eye pass adds RGB directly, regardless of vertex alpha.
            // Keep its RGB dim and reacquire the body buffer after switching
            // passes; otherwise a shared BufferBuilder draws the face as eyes.
            var halo=buffers.getBuffer(RenderType.eyes(WHITE));
            for(int layer=0;layer<3;layer++)sphere(p,halo,15728880,0,.85,0,.38+layer*.08,.78+layer*.06,.26+layer*.07,0x041B0B,1);
            b=buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        }
        p.translate(0,Math.sin(t*.04+e.slot())*.06,0);var player=Minecraft.getInstance().player;if(player!=null){var d=player.position().subtract(e.position());p.mulPose(Axis.YP.rotationDegrees(180-(float)Math.toDegrees(Math.atan2(-d.x,d.z))));}
        if(!headOnly){sphere(p,b,15728880,0,.6,0,.25,.43,.16,color,alpha);for(int side:new int[]{-1,1}){double lift=Math.sin(t*.035+e.slot()*2+side)*.18;sphere(p,b,15728880,side*.30,.70+lift*.5,-.07,.07,.25,.07,color,alpha);sphere(p,b,15728880,side*.12,.14,0,.08,.25,.09,color,alpha);}if(freed&&e.slot()==1)sphere(p,b,15728880,0,.36,0,.35,.22,.23,color,alpha);}
        sphere(p,b,15728880,0,1.25,0,.24,.29,.21,color,alpha);int ink=freed?0x856A3C:0x183B2D;
        for(int side:new int[]{-1,1}){sphere(p,b,15728880,side*.09,1.28,-.19,.04,.05,.018,ink,.85F);limb(p,b,15728880,new Vec3(side*.04,1.38+(sad?.035:0),-.2),new Vec3(side*.15,1.36,-.2),.02,freed?0xC9A764:0x3F7052);}
        limb(p,b,15728880,new Vec3(-.08,1.1+(sad?-.025:.025),-.205),new Vec3(0,1.08+(sad?.025:0),-.217),.014,ink);limb(p,b,15728880,new Vec3(0,1.08+(sad?.025:0),-.217),new Vec3(.08,1.1+(sad?-.025:.025),-.205),.014,ink);
        if(e.slot()==0)for(int i=-2;i<=2;i++)sphere(p,b,15728880,i*.09,1.48,-.01,.1,.1,.2,hair,.65F);else if(e.slot()==1)for(int side:new int[]{-1,1})sphere(p,b,15728880,side*.25,1.32,0,.10,.17,.1,hair,.65F);else sphere(p,b,15728880,0,1.47,.02,.25,.12,.22,hair,.65F);
        if(freed)for(int i=0;i<40;i++){double a=i*Math.PI/20,next=(i+1)*Math.PI/20;limb(p,b,15728880,new Vec3(Math.cos(a)*.38,1.72+Math.sin(a)*.10,.02),new Vec3(Math.cos(next)*.38,1.72+Math.sin(next)*.10,.02),.012,0xFFE9A8);}
    }
    private static void evil(PoseStack p,MultiBufferSource buffers,int light,float amount,boolean headOnly,float yaw,double time,int gesture,net.minecraft.world.item.ItemStack held){
        var mc=Minecraft.getInstance();if(mc.level==null)return;if(evil==null)evil=new CharacterModel("evil_other_mother");
        if(proxy==null||proxy.level()!=mc.level)proxy=com.coraline.registry.CharacterCast.ENTITIES.get("evil_other_mother").get().create(mc.level);if(proxy==null)return;
        boolean finale=com.coraline.client.RescueClient.active(),rescue=finale||com.coraline.client.ParentRescueClient.active();
        proxy.gameGesture(0,0);evil.setupAnim(proxy,0,0,(float)time,0,0);if(rescue)evil.rescuePose(gesture,(float)time);
        if(!rescue&&!headOnly&&(FirstEscapeClient.phase().equals("MIRROR_DRAG")||FirstEscapeClient.phase().equals("COCOA_G")||FirstEscapeClient.phase().equals("MIRROR_THROW")))evil.storyGrip();
        p.pushPose();p.scale(1,headOnly?1:1.28F,1);p.translate(0,1.501,0);p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.scale(-1,-1,1);
        evil.storyHeadOnly(headOnly);evil.storyBlind(finale&&com.coraline.client.RescueClient.flag("BeldamBlind"));
        evil.renderSpeech(p,buffers.getBuffer(RenderType.entityTranslucent(ATLAS)),light,OverlayTexture.NO_OVERLAY,1,1,1,amount,com.coraline.client.VisitAudioClient.voiceLevel(finale?"finale_":rescue?"parent_rescue_":headOnly?"mirror_prison_":"cocoa_beetles_"));
        evil.storyHeadOnly(false);evil.storyBlind(false);
        if(rescue){p.pushPose();evil.transformBone("right_forearm",p);
            if(!held.isEmpty()){p.translate(0,.28,0);p.scale(.80F,.80F,.80F);mc.getItemRenderer().renderStatic(held,net.minecraft.world.item.ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,p,buffers,mc.level,0);}
            else if(gesture==60){box(p,solid(buffers),light,0,.12,0,.025,.55,.025,0x9D7853);sphere(p,solid(buffers),light,0,.69,0,.07,.09,.045,0xAC895F,1);}
            else if(gesture==61)for(int finger=0;finger<5;finger++){double lift=Math.max(0,Math.sin(time*.65-finger*.9))*.055;box(p,solid(buffers),light,(finger-2)*.045,.28,-.015-lift,.033,.17-finger*.008,.035,0xCFB7A0);}
            p.popPose();
        }p.popPose();
    }
    public static boolean mother(ParentEntity actor,float yaw,float partial,PoseStack p,MultiBufferSource buffers,int light){
        float morph=Math.max(Math.max(FirstEscapeClient.morph(actor.getId(),partial),com.coraline.client.ParentRescueClient.morph(actor.getId())),actor.getId()==com.coraline.client.RescueClient.mother()?1:0);if(morph<=0||actor.isInvisible())return false;
        if(morph>=.999F){evil(p,buffers,light,1,false,yaw,actor.tickCount+partial,actor.familyGesture(),actor.getMainHandItem());return true;}
        var mc=Minecraft.getInstance();if(mc.level==null)return false;
        if(evil==null)evil=new CharacterModel("evil_other_mother");
        if(proxy==null||proxy.level()!=mc.level)proxy=com.coraline.registry.CharacterCast.ENTITIES.get("evil_other_mother").get().create(mc.level);
        if(proxy==null)return false;evil.setupAnim(proxy,0,0,actor.tickCount+partial,0,0);evil.storyHeadOnly(false);
        p.pushPose();p.scale(1,1+.28F*morph,1);p.translate(0,1.501,0);p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.scale(-1,-1,1);
        evil.renderMotherMorph(p,solid(buffers),light,OverlayTexture.NO_OVERLAY,ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/mom_"+actor.skin()+".png"),morph);
        p.popPose();return true;
    }
    public static void heldBeetle(ParentEntity actor,net.minecraft.client.model.PlayerModel<ParentEntity> model,PoseStack p,MultiBufferSource buffers,int light){
        if(actor.getId()!=FirstEscapeClient.mother()||!FirstEscapeClient.phase().startsWith("COCOA_"))return;
        boolean holding=actor.familyGesture()==40&&!FirstEscapeClient.flag("BeetleEaten"),presenting=actor.familyGesture()==66;
        if(!holding&&!presenting)return;
        var skin=MotherMorphGeometry.pixel(ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/mom_"+actor.skin()+".png"),44/64F,26/64F);
        int skinColor=((int)(skin.x*255)<<16)|((int)(skin.y*255)<<8)|(int)(skin.z*255);var b=solid(buffers);
        p.pushPose();model.rightArm.translateAndRotate(p);fingers(p,b,light,skinColor,holding,1);
        if(holding){float remaining=com.coraline.feast.CocoaBeetlesChoreography.beetleRemaining(FirstEscapeClient.phase(),FirstEscapeClient.time());if(remaining>0){p.pushPose();p.translate(-.035,.70,-.14);p.scale(.22F*remaining,.22F*remaining,.22F*remaining);beetle(p,b,light,actor.tickCount);p.popPose();}}
        p.popPose();
        if(presenting){p.pushPose();model.leftArm.translateAndRotate(p);fingers(p,b,light,skinColor,false,-1);p.popPose();}
    }
    /** Three joint segments per finger; opposed thumb, knuckles and red nails. */
    private static void fingers(PoseStack p,VertexConsumer b,int light,int skin,boolean pinch,int side){
        p.pushPose();p.translate(-.035*side,.58,-.035);sphere(p,b,light,0,.045,0,.085,.10,.06,skin,1);
        for(int finger=0;finger<4;finger++){
            double x=(finger-1.5)*.043*side,length=(finger==0||finger==3?.040:.047),curl=pinch?(finger==0?.40:.95):.22;
            Vec3 last=new Vec3(x,.10,-.012);
            for(int segment=0;segment<3;segment++){
                double angle=curl*(segment+1);var next=last.add(0,Math.cos(angle)*length,-Math.sin(angle)*length);
                limb(p,b,light,last,next,.030,skin);sphere(p,b,light,next.x,next.y,next.z,.018,.018,.017,skin,1);
                if(segment==2)sphere(p,b,light,next.x,next.y,next.z-.015,.012,.020,.006,0x9B253A,1);last=next;
            }
        }
        var thumb=new Vec3(-side*.075,.04,-.005);var joint=thumb.add(-side*.032,.04,-.035);var tip=pinch?new Vec3(-side*.055,.125,-.085):joint.add(-side*.02,.035,-.025);
        limb(p,b,light,thumb,joint,.037,skin);limb(p,b,light,joint,tip,.031,skin);sphere(p,b,light,tip.x,tip.y,tip.z-.012,.018,.025,.007,0xA8293C,1);p.popPose();
    }
    public static void angryFace(ParentEntity actor,net.minecraft.client.model.PlayerModel<ParentEntity> model,PoseStack p,MultiBufferSource buffers,int light){
        if(!FirstEscapeClient.angry(actor.getId())||actor.isInvisible())return;
        p.pushPose();model.head.translateAndRotate(p);var b=solid(buffers);
        var skin=MotherMorphGeometry.pixel(ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/mom_"+actor.skin()+".png"),12/64F,9/64F);
        int skinColor=((int)(skin.x*255)<<16)|((int)(skin.y*255)<<8)|(int)(skin.z*255);
        for(int side:new int[]{-1,1}){
            // The button-eye layer is at Z=-.29: lids must be in front of it.
            for(double[] point:new double[][]{{side*.24,-.50},{side*.045,-.50},{side*.045,-.347},{side*.24,-.45}})vertex(p,b,light,point[0],point[1],-.298,0,0,-1,skinColor,1,.5F,.5F);
            limb(p,b,light,new Vec3(side*.235,-.45,-.307),new Vec3(side*.047,-.347,-.307),.030,0x241820);
            limb(p,b,light,new Vec3(side*.052,-.095,-.303),new Vec3(side*.10,-.083,-.303),.016,0x3F2224);
        }
        limb(p,b,light,new Vec3(-.052,-.095,-.303),new Vec3(.052,-.095,-.303),.019,0x3F2224);p.popPose();
    }
    public static void wybieFace(PoseStack p,VertexConsumer b,int light){boolean mask=!FirstEscapeClient.flag("MaskRemoved");double lift=FirstEscapeClient.phase().equals("MASK_ANIMATION")?Math.min(1,FirstEscapeClient.time()/25.)*.7:0;if(mask){p.pushPose();p.translate(0,-lift,-lift*.3);sphere(p,b,light,0,-.25,-.23,.32,.36,.15,0xE2C89C,1);for(int side:new int[]{-1,1})sphere(p,b,light,side*.13,-.32,-.37,.08,.10,.03,0x261B21,1);sphere(p,b,light,0,-.15,-.44,.17,.08,.2,0xD7A33C,1);sphere(p,b,light,0,-.6,-.22,.08,.12,.1,0xA93636,1);p.popPose();}
        if(!FirstEscapeClient.flag("StitchesRemoved")){
            // The jaw is at Z=-3.08/16, not -.28. Replace the relaxed crooked
            // mouth with one curved seam, then anchor each stitch across it.
            for(int i=0;i<12;i++){double x=-.105+i*.21/12,next=x+.21/12;limb(p,b,light,new Vec3(x,com.coraline.feast.FirstEscapeVisualRules.stitchedSmileY(x),-.202),new Vec3(next,com.coraline.feast.FirstEscapeVisualRules.stitchedSmileY(next),-.202),.009,0x5B3028);}
            double progress=FirstEscapeClient.phase().equals("STITCH_ANIMATION")?Math.min(1,FirstEscapeClient.time()/35.):0;
            for(int i=0;i<7;i++){double x=(i-3)*.029,y=com.coraline.feast.FirstEscapeVisualRules.stitchedSmileY(x),z=-.209-progress*.16;
                limb(p,b,light,new Vec3(x-.008,y-.018,z),new Vec3(x+.008,y+.018,z),.007,0x20151D);
                sphere(p,b,light,x,y,z-.003,.006,.006,.004,0x20151D,(float)(1-progress));
            }
        }
    }
    private FirstEscapePresentation(){}
}
