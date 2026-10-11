package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;

/** A three-minute vertical course suspended among luminous cloud islands. */
public final class DreamParkourActivity {
    public static final int STAGES=192,DURATION=3*60*20;
    private static final String OWNER="CoralineParkourOwner";
    private static final Block LEGACY_MARKER=Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA;
    private static final Block CURRENT_MARKER=Blocks.CYAN_GLAZED_TERRACOTTA;
    private static final int SEGMENT_LENGTH=8;
    private enum Module { CLOUD_STEPS, CORNER_WRAP, ICE_RUN, SCAFFOLD_CLIMB, HONEY_CONTROL,
        SLIME_HOP, FENCE_PRECISION, TRAPDOOR_CHAIN, HEAD_HITTER, VANISHING, SHORTCUT }
    private static final int[] STAGE_X=new int[STAGES],STAGE_Z=new int[STAGES];
    static {
        // Search forward along a waving spiral for each landing. Quantization
        // to Minecraft blocks used to stack pads vertically or create jumps
        // beyond sprint range; validate the actual integer coordinates.
        double angle=0;STAGE_X[0]=0;STAGE_Z[0]=9;
        for(int i=1;i<STAGES;i++){
            double radius=9+3.0*i/(STAGES-1)+(i>=96?.6*Math.sin(i*.22)+.35*Math.sin(i*.67):.15*Math.sin(i*.17));
            double wanted=(i<48?2.25:i<96?2.5:i<144?2.7:2.9)+
                (i>=64?.23*Math.sin(i*1.21):.12*Math.sin(i*.8));
            double bestScore=Double.POSITIVE_INFINITY,bestAngle=0;
            int bestX=0,bestZ=0;
            for(int turn=12;turn<=42;turn++){
                double candidateAngle=angle+turn/100.0;
                int x=(int)Math.round(Math.sin(candidateAngle)*radius),z=(int)Math.round(Math.cos(candidateAngle)*radius);
                double gap=Math.hypot(x-STAGE_X[i-1],z-STAGE_Z[i-1]);
                if(gap<2||gap>Math.sqrt(10))continue;
                if(i>1&&Math.hypot(x-STAGE_X[i-2],z-STAGE_Z[i-2])<2)continue;
                double score=Math.abs(gap-wanted)+Math.abs(turn/100.0-.23)*.2;
                if(score<bestScore){bestScore=score;bestAngle=candidateAngle;bestX=x;bestZ=z;}
            }
            if(!Double.isFinite(bestScore))throw new IllegalStateException("No reachable dream-parkour landing at stage "+i);
            angle=bestAngle;STAGE_X[i]=bestX;STAGE_Z[i]=bestZ;
        }
    }
    private static final Block[] RAINBOW={Blocks.PINK_STAINED_GLASS,Blocks.MAGENTA_STAINED_GLASS,
        Blocks.PURPLE_STAINED_GLASS,Blocks.LIGHT_BLUE_STAINED_GLASS,Blocks.CYAN_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS,Blocks.ORANGE_STAINED_GLASS};
    private DreamParkourActivity(){}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).west(320);}
    static boolean exitReady(VisitData.Scene s){return s.loop.parkourRewarded;}
    static BlockPos stage(BlockPos c,int i){
        return c.offset(STAGE_X[i],(int)Math.floor(i*.9),STAGE_Z[i]);
    }
    private static int minOffset(int i){return 0;}
    private static int maxOffset(int i){return i<16||i<64&&i%3==0?1:0;}
    private static void put(ServerLevel level,BlockPos at,Block block,boolean remove){
        BlockState state=remove?Blocks.AIR.defaultBlockState():block.defaultBlockState();
        if(!level.getBlockState(at).equals(state))ActivityBlocks.set(level,at,state,2);
    }
    private static void cloud(ServerLevel level,BlockPos at,int rx,int ry,int rz,boolean remove){
        for(int x=-rx;x<=rx;x++)for(int y=-ry;y<=ry;y++)for(int z=-rz;z<=rz;z++){
            double d=(double)x*x/(rx*rx)+(double)y*y/(ry*ry)+(double)z*z/(rz*rz);
            if(d>1.12)continue;
            Block block=d>.65?Blocks.WHITE_STAINED_GLASS:
                Math.floorMod(x*7+y*3-z,29)==0?Blocks.SEA_LANTERN:
                Math.floorMod(x+z,6)==0?Blocks.CALCITE:Blocks.WHITE_WOOL;
            put(level,at.offset(x,y,z),block,remove);
        }
    }
    private static void structure(ServerLevel level,BlockPos c,boolean remove){
        // Wide scalloped cloud floor catches falls without showing a rectangular platform.
        for(int x=-37;x<=37;x++)for(int z=-37;z<=37;z++){
            double edge=32+2.4*Math.sin(x*.27)+2*Math.cos(z*.31);
            if(x*x+z*z>edge*edge)continue;
            var ground=c.offset(x,-1,z);
            Block top=Math.hypot(x,z)>edge-3?Blocks.WHITE_STAINED_GLASS:
                Math.floorMod(x+z,13)==0?Blocks.SEA_LANTERN:
                Math.floorMod(x-z,7)==0?Blocks.LIGHT_BLUE_CONCRETE:Blocks.WHITE_CONCRETE;
            put(level,ground,top,remove);
            put(level,ground.below(),Math.hypot(x,z)>edge-4?Blocks.LIGHT_BLUE_STAINED_GLASS:Blocks.WHITE_WOOL,remove);
        }
        // A clear launch walk and a luminous cloud arch frame the first jump.
        for(int z=9;z<=18;z++)for(int x=-2;x<=2;x++)put(level,c.offset(x,-1,z),x==0&&z%3==0?Blocks.SEA_LANTERN:Blocks.SMOOTH_QUARTZ,remove);
        for(int x=-8;x<=8;x++){
            int y=5+(int)Math.round(4*Math.sqrt(Math.max(0,1-x*x/64.0)));
            put(level,c.offset(x,y,13),RAINBOW[Math.floorMod(x+8,RAINBOW.length)],remove);
            if(Math.abs(x)==8)for(int h=0;h<y;h++)put(level,c.offset(x,h,13),Blocks.QUARTZ_PILLAR,remove);
        }
        for(int side:new int[]{-1,1})for(int z=12;z<=20;z+=2){
            put(level,c.offset(side*5,0,z),Blocks.AMETHYST_BLOCK,remove);
            put(level,c.offset(side*5,1,z),Blocks.SEA_LANTERN,remove);
        }
        // Layered, irregular clouds frame the entrance and continue around the
        // climb. Their transparent rims and illuminated cores read as floating
        // cloud banks rather than another square platform.
        for(int side:new int[]{-1,1}){
            cloud(level,c.offset(side*16,4,5),5,3,4,remove);
            cloud(level,c.offset(side*20,6,11),4,2,5,remove);
            cloud(level,c.offset(side*17,7,-13),5,3,4,remove);
            for(int z=19;z<=25;z+=6){
                var spire=c.offset(side*11,0,z);
                for(int y=0;y<5;y++)put(level,spire.above(y),y%2==0?Blocks.QUARTZ_PILLAR:Blocks.AMETHYST_BLOCK,remove);
                put(level,spire.above(5),Blocks.SEA_LANTERN,remove);
                for(int dx:new int[]{-1,1})put(level,spire.offset(dx,3,0),Blocks.PINK_STAINED_GLASS,remove);
            }
        }
        for(int i=0;i<10;i++){
            double a=i*2.399963;int x=(int)Math.round(Math.cos(a)*21),z=(int)Math.round(Math.sin(a)*21);
            cloud(level,c.offset(x,4+i%4*3,z),3+i%2,2+i%2,3,remove);
        }
        // Cloud gardens spiral upward beside the entire extended climb.
        for(int island=0;island<32;island++){
            double a=island*Math.PI/6;int cx=(int)Math.round(Math.cos(a)*25),cz=(int)Math.round(Math.sin(a)*25);
            int cy=8+island*5,r=5+island%3;
            var hub=c.offset(cx,cy,cz);
            for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
                double d=dx*dx+dz*dz;if(d>r*r+2*Math.sin(dx+dz))continue;
                put(level,hub.offset(dx,0,dz),d>(r-1)*(r-1)?Blocks.WHITE_STAINED_GLASS:
                    (dx+dz)%7==0?Blocks.SEA_LANTERN:Blocks.WHITE_WOOL,remove);
                if(d<(r-2)*(r-2))put(level,hub.offset(dx,-1,dz),Blocks.LIGHT_BLUE_STAINED_GLASS,remove);
                if(d>r*r-5&&Math.floorMod(dx*3+dz,4)==0)put(level,hub.offset(dx,1,dz),Blocks.PINK_STAINED_GLASS,remove);
            }
            for(int y=1;y<=3+island%3;y++)put(level,hub.above(y),island%2==0?Blocks.AMETHYST_BLOCK:Blocks.PURPUR_BLOCK,remove);
            put(level,hub.above(4+island%3),Blocks.SEA_LANTERN,remove);
        }
        // A huge crescent moon hangs beyond the route, surrounded by tiny stars.
        for(int x=-9;x<=9;x++)for(int y=-9;y<=9;y++){
            boolean crescent=x*x+y*y<=81&&(x-3)*(x-3)+(y-2)*(y-2)>54;
            if(crescent)put(level,c.offset(x,126+y,-31),Math.floorMod(x+y,4)==0?Blocks.SEA_LANTERN:Blocks.END_STONE,remove);
        }
        for(int i=0;i<32;i++){
            double a=i*2.399963;int x=(int)Math.round(Math.cos(a)*(18+i%4*6)),z=(int)Math.round(Math.sin(a)*(18+i%4*6));
            int y=12+(i*17)%165;
            put(level,c.offset(x,y,z),i%5==0?Blocks.GLOWSTONE:Blocks.SEA_LANTERN,remove);
        }
        put(level,c.below(),Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,remove);
    }
    private static Module module(long seed,int segment){
        if(segment==0)return Module.CLOUD_STEPS;
        Module[] low={Module.CLOUD_STEPS,Module.CORNER_WRAP,Module.ICE_RUN,Module.SCAFFOLD_CLIMB,Module.SLIME_HOP};
        Module[] mid={Module.CORNER_WRAP,Module.ICE_RUN,Module.HONEY_CONTROL,Module.SLIME_HOP,
            Module.FENCE_PRECISION,Module.TRAPDOOR_CHAIN,Module.HEAD_HITTER,Module.SHORTCUT};
        Module[] high={Module.FENCE_PRECISION,Module.TRAPDOOR_CHAIN,Module.HEAD_HITTER,Module.VANISHING,
            Module.SHORTCUT,Module.HONEY_CONTROL,Module.ICE_RUN,Module.SLIME_HOP};
        Module[] pool=segment<6?low:segment<15?mid:high;
        int index=Math.floorMod((int)(seed>>>(segment%8*7))^segment*17,pool.length);
        Module choice=pool[index],previous=module(seed,segment-1);
        return choice==previous?pool[(index+1)%pool.length]:choice;
    }
    static void validateCourse(long seed){
        for(int i=1;i<STAGES;i++){
            double gap=Math.hypot(STAGE_X[i]-STAGE_X[i-1],STAGE_Z[i]-STAGE_Z[i-1]);
            int rise=(int)Math.floor(i*.9)-(int)Math.floor((i-1)*.9);
            if(gap<2||gap>Math.sqrt(10)||rise<0||rise>1)
                throw new IllegalStateException("Unreachable dream-course transition "+(i-1)+" -> "+i);
            for(int j=Math.max(0,i-5);j<i-1;j++)if(Math.hypot(STAGE_X[i]-STAGE_X[j],STAGE_Z[i]-STAGE_Z[j])<2&&
                (int)Math.floor(i*.9)-(int)Math.floor(j*.9)<4)
                throw new IllegalStateException("Overlapping dream landings at "+j+" and "+i);
        }
        for(int segment=1;segment<(STAGES+SEGMENT_LENGTH-1)/SEGMENT_LENGTH;segment++)
            if(module(seed,segment)==module(seed,segment-1))throw new IllegalStateException("Repeated dream obstacle module at "+segment);
    }
    private static Block padMaterial(Module module,int i){return switch(module){
        case ICE_RUN->i%3==0?Blocks.BLUE_ICE:Blocks.PACKED_ICE;
        case HONEY_CONTROL->i%3==0?Blocks.HONEY_BLOCK:Blocks.PURPUR_BLOCK;
        case SLIME_HOP->i%3==1?Blocks.SLIME_BLOCK:Blocks.LIGHT_BLUE_CONCRETE;
        case SCAFFOLD_CLIMB->Blocks.SCAFFOLDING;
        case FENCE_PRECISION->i%2==0?Blocks.IRON_BARS:Blocks.CHAIN;
        case TRAPDOOR_CHAIN->i%2==0?Blocks.CHERRY_TRAPDOOR:Blocks.WHITE_CONCRETE;
        case VANISHING->Blocks.PINK_STAINED_GLASS;
        case SHORTCUT->Blocks.MAGENTA_CONCRETE;
        case CORNER_WRAP->Blocks.PURPUR_BLOCK;
        case HEAD_HITTER->Blocks.CALCITE;
        default->i%3==0?Blocks.LIGHT_BLUE_CONCRETE:Blocks.WHITE_CONCRETE;};}
    private static void pads(ServerLevel level,BlockPos c,long seed,boolean remove){
        // Each eight-landing module has a coherent movement surface and visual
        // cue. Choice changes with the run seed; no adjacent module repeats.
        if(!remove)for(int i=0;i<STAGES;i++){
            var at=stage(c,i);for(int dx=minOffset(i);dx<=maxOffset(i);dx++)for(int dz=minOffset(i);dz<=maxOffset(i);dz++)
                for(int y=1;y<=2;y++)put(level,at.offset(dx,y,dz),Blocks.AIR,false);
        }
        for(int i=0;i<STAGES;i++){
            Module module=module(seed,i/SEGMENT_LENGTH);int local=i%SEGMENT_LENGTH;
            var at=stage(c,i);int lo=minOffset(i),hi=maxOffset(i);
            Block pad=padMaterial(module,i);
            for(int dx=lo;dx<=hi;dx++)for(int dz=lo;dz<=hi;dz++){
                if(module==Module.TRAPDOOR_CHAIN&&pad==Blocks.CHERRY_TRAPDOOR&&!remove)
                    ActivityBlocks.set(level,at.offset(dx,0,dz),Blocks.CHERRY_TRAPDOOR.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF,net.minecraft.world.level.block.state.properties.Half.TOP),2);
                else put(level,at.offset(dx,0,dz),pad,remove);
                put(level,at.offset(dx,-1,dz),Blocks.WHITE_STAINED_GLASS,remove);
            }
            // A low overhead beam changes jump timing only where the next
            // landing is level and close enough to remain comfortably possible.
            if(module==Module.HEAD_HITTER&&local==2&&i+1<STAGES&&
                stage(c,i+1).getY()==at.getY()&&Math.hypot(STAGE_X[i+1]-STAGE_X[i],STAGE_Z[i+1]-STAGE_Z[i])<=2.5){
                put(level,at.above(3),Blocks.PINK_STAINED_GLASS,remove);
                put(level,at.above(4),Blocks.SEA_LANTERN,remove);
            }
            if(module==Module.CORNER_WRAP&&local==3&&i+1<STAGES){
                int dx=Integer.signum(STAGE_X[i]-STAGE_X[i+1]),dz=Integer.signum(STAGE_Z[i]-STAGE_Z[i+1]);
                // Keep the wall lateral to the line of travel, never inside
                // either landing's headroom.
                var wall=at.offset(dx==0?1:dx,1,dz==0?1:dz);
                if(wall.distManhattan(stage(c,i+1))>2)put(level,wall,Blocks.PURPLE_STAINED_GLASS,remove);
            }
            if(module==Module.SCAFFOLD_CLIMB&&local==3){
                put(level,at.above(),Blocks.SCAFFOLDING,remove);
                put(level,at.above(2),Blocks.SCAFFOLDING,remove);
                put(level,at.above(5),Blocks.SEA_LANTERN,remove);
            }
            if(module==Module.HONEY_CONTROL&&local==3&&i+1<STAGES){
                int sideX=Integer.signum(STAGE_Z[i+1]-STAGE_Z[i]);
                int sideZ=-Integer.signum(STAGE_X[i+1]-STAGE_X[i]);
                var wall=at.offset(sideX,1,sideZ);
                if(wall.distManhattan(stage(c,i+1))>2){
                    put(level,wall,Blocks.HONEY_BLOCK,remove);
                    put(level,wall.above(),Blocks.HONEY_BLOCK,remove);
                }
            }
            if(module==Module.SHORTCUT&&local==1&&i+3<STAGES){
                var from=stage(c,i);var to=stage(c,i+3);
                double gap=Math.hypot(to.getX()-from.getX(),to.getZ()-from.getZ());
                if(gap>=3.5&&gap<=6.2){
                    var midway=new BlockPos((from.getX()+to.getX())/2,(from.getY()+to.getY())/2,(from.getZ()+to.getZ())/2);
                    if(midway.distManhattan(from)>1&&midway.distManhattan(to)>1){
                        put(level,midway,Blocks.GOLD_BLOCK,remove);
                        put(level,midway.below(),Blocks.PINK_STAINED_GLASS,remove);
                    }
                }
            }
            // Broad physical catch-clouds below the route every ~22 blocks.
            // Falling still loses height and time; there is no checkpoint warp.
            if(i>0&&i%24==0)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                if(dx*dx+dz*dz>7)continue;
                put(level,at.offset(dx,-4,dz),Math.floorMod(dx+dz,5)==0?Blocks.SEA_LANTERN:Blocks.WHITE_WOOL,remove);
            }
        }
    }
    private static void pulsePads(ServerLevel level,BlockPos c,long seed,long now){
        for(int i=16;i<STAGES-1;i++){
            if(module(seed,i/SEGMENT_LENGTH)!=Module.VANISHING||i%SEGMENT_LENGTH!=2&&i%SEGMENT_LENGTH!=5)continue;
            var at=stage(c,i);int phase=Math.floorMod((int)(now+i*7),64);
            Block tile=phase<34?Blocks.PINK_STAINED_GLASS:phase<45?Blocks.GOLD_BLOCK:Blocks.AIR;
            put(level,at,tile,false);put(level,at.below(),phase>=45?Blocks.AIR:Blocks.WHITE_STAINED_GLASS,false);
            if(phase==35)level.sendParticles(ParticleTypes.END_ROD,at.getX()+.5,at.getY()+.5,at.getZ()+.5,5,.3,.1,.3,.01);
        }
    }
    private static void clearLegacy(ServerLevel level,BlockPos c){
        // The original arena had no revision marker and a different pad path.
        // Remove the decorative shell by its known geometry, then sweep the
        // narrow climb cylinder. Scanning the entire 85x85x183 bounding box
        // loaded empty chunks and stalled both migration and activity exit.
        structure(level,c,true);
        for(int x=-19;x<=19;x++)for(int z=-19;z<=19;z++){
            if(x*x+z*z>19*19)continue;
            for(int y=0;y<=178;y++){
                var at=c.offset(x,y,z);
                if(!level.isEmptyBlock(at))ActivityBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
            }
        }
    }
    static void build(ServerLevel level,BlockPos c){
        if(level.getBlockState(c.below()).is(LEGACY_MARKER)){
            if(level.getBlockState(c.below(2)).is(CURRENT_MARKER))return;
            clearLegacy(level,c);
        }
        structure(level,c,false);
        put(level,c.below(2),CURRENT_MARKER,false);
    }
    static void removeBuild(ServerLevel level,BlockPos c){
        if(!level.getBlockState(c.below()).is(LEGACY_MARKER))return;
        clearLegacy(level,c);
    }
    static void clean(ServerLevel level,VisitData.Scene s,BlockPos c){
        var remove=new ArrayList<Entity>();
        for(var e:level.getEntitiesOfClass(Entity.class,new AABB(c).inflate(40,100,40),e->e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))remove.add(e);
        remove.forEach(Entity::discard);
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        var c=center(p,s);build(level,c);clean(level,s,c);
        var l=s.loop;l.parkourRewarded=false;l.parkourEndAt=0;
        l.parkourSeed=p.getRandom().nextLong();validateCourse(l.parkourSeed);pads(level,c,l.parkourSeed,true);
        l.parkourVoiceUntil=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.DREAM_PARKOUR));
        ensureFather(p,s,level,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        LoopProps.hold(level,c);LoopProps.hold(level,c.south(16));build(level,c);ensureFather(p,s,level,c);
        if(s.loop.parkourEndAt>0&&!s.loop.parkourRewarded)pads(level,c,s.loop.parkourSeed,false);
        // This is a clear, starry dream even if the shared activity dimension
        // was previously left in rainy weather by another scene.
        level.setWeatherParameters(120000,0,false,false);
        level.setRainLevel(0);level.setThunderLevel(0);
        for(int i=0;i<8;i++){
            double angle=(level.getGameTime()*.018+i*Math.PI/4),radius=6+i%3*3;
            level.sendParticles(ParticleTypes.END_ROD,c.getX()+.5+Math.cos(angle)*radius,c.getY()+1.5+i%4,
                c.getZ()+.5+Math.sin(angle)*radius,2,.25,.3,.25,.004);
        }
        for(int i=0;i<3;i++){
            var at=stage(c,Math.floorMod((int)(level.getGameTime()/20)*3+i*17,STAGES));
            level.sendParticles(ParticleTypes.CLOUD,at.getX()+.5,at.getY()+.15,at.getZ()+.5,3,.5,.08,.5,.005);
        }
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        if(!level.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(16)).inflate(8),e->e.getPersistentData().getBoolean("CoralineParkourFather")&&e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var family=ParentAccess.family(p.server,s.owner);
        var father=ModEntities.DAD.get().create(level);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);
        father.getPersistentData().putBoolean("CoralineParkourFather",true);father.getPersistentData().putUUID(OWNER,s.owner);
        father.getPersistentData().putUUID("RealmDoor",s.door);father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+16.5);father.setYRot(90);father.setYBodyRot(90);level.addFreshEntity(father);
    }
    private static int landedStage(ServerPlayer p,BlockPos c){
        var under=p.blockPosition().below();
        for(int i=STAGES-1;i>=0;i--){var at=stage(c,i);if(under.getY()!=at.getY())continue;
            if(under.getX()>=at.getX()+minOffset(i)&&under.getX()<=at.getX()+maxOffset(i)&&
               under.getZ()>=at.getZ()+minOffset(i)&&under.getZ()<=at.getZ()+maxOffset(i))return i;
        }
        return -1;
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var l=s.loop;long now=OtherMotherLoop.now(p);
        if(p.tickCount%20==0)prepare(p,s,p.serverLevel(),c);
        if(l.parkourEndAt<=0){
            if(now<l.parkourVoiceUntil){
                // Even Creative flyers cannot wait on a future pad for the clock to begin.
                if(p.getY()>c.getY()+2||p.getY()<c.getY()-2||
                    p.distanceToSqr(Vec3.atBottomCenterOf(c.south(16)))>64){
                    p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+16.5,180,0);
                    p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
                }
                if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Wait for Father to finish speaking. The three-minute climb starts next."),true);
                return;
            }
            l.parkourEndAt=now+DURATION;pads(p.serverLevel(),c,l.parkourSeed,false);OtherMotherLoop.changed(p);
            p.sendSystemMessage(Component.literal("Dream Parkour begins! Climb as high as you can in three minutes. There are no checkpoints."));
            p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,1F,1.25F);
        }
        if(l.parkourRewarded)return;
        if(p.tickCount%4==0)pulsePads(p.serverLevel(),c,l.parkourSeed,now);
        if(p.onGround()&&landedStage(p,c)==STAGES-1){finish(p,s,c,true);return;}
        if(now>=l.parkourEndAt){finish(p,s,c,false);return;}
        if(p.tickCount%20==0){int seconds=(int)Math.max(0,(l.parkourEndAt-now+19)/20);
            p.displayClientMessage(Component.literal("Dream climb: "+seconds+"s left  |  Current height: "+height(p,c)+" blocks"),true);}
    }
    private static int height(ServerPlayer p,BlockPos c){return Math.max(0,Math.min(stage(c,STAGES-1).getY()-c.getY()+1,(int)Math.floor(p.getY()-c.getY())));}
    private static void finish(ServerPlayer p,VisitData.Scene s,BlockPos c,boolean summit){
        var l=s.loop;if(l.parkourRewarded)return;
        int currentHeight=summit?stage(c,STAGES-1).getY()-c.getY()+1:height(p,c);
        int tier=currentHeight==0?0:Math.min(5,1+(currentHeight-1)/36);
        var prize=switch(tier){
            case 0->new ItemStack(Items.COOKIE,4);
            case 1->new ItemStack(Items.IRON_INGOT,6);
            case 2->new ItemStack(Items.GOLD_INGOT,8);
            case 3->new ItemStack(Items.EMERALD,10);
            case 4->new ItemStack(Items.DIAMOND,6);
            default->new ItemStack(Items.NETHERITE_INGOT,2);
        };
        String name=prize.getHoverName().getString();p.getInventory().add(prize);if(!prize.isEmpty())p.drop(prize,false);
        l.parkourRewarded=true;OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
        // The player may be seventy blocks up when time expires; bring them
        // back to the launch cloud so the newly opened portal is reachable.
        p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+16.5,180,0);
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,1F,.9F);
        p.sendSystemMessage(Component.literal((summit?"Summit reached!":"Time!")+" Current height: "+currentHeight+" blocks. Tier "+tier+" reward: "+name+". The return portal has appeared at the entrance."));
    }
}
