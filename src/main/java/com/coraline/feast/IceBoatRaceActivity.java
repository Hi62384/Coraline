package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A three-lap frozen festival race. Checkpoints and results belong only to the visiting player. */
public final class IceBoatRaceActivity {
    private static final String OWNER="CoralineIceRaceOwner",GEN="CoralineIceRaceGeneration",SLOT="CoralineIceRaceSlot";
    private static final int RIVALS=6,LAPS=3,GATES=14;
    private static final double TRACK_RADIUS=5.2;
    private record Point(double x,double z){}
    private static final Point[] CONTROL={
        new Point(0,36),new Point(23,37),new Point(40,27),new Point(38,10),
        new Point(25,4),new Point(39,-12),new Point(34,-31),new Point(16,-39),
        new Point(2,-28),new Point(-13,-38),new Point(-34,-33),new Point(-41,-17),
        new Point(-27,-3),new Point(-40,12),new Point(-37,30),new Point(-20,41)
    };
    private static final List<Point> PATH=new ArrayList<>();
    private static final List<Double> DIST=new ArrayList<>();
    private static final double LENGTH;
    static {
        double distance=0;Point previous=null;
        for(int i=0;i<CONTROL.length;i++)for(int step=0;step<18;step++){
            double t=step/18.0;Point a=CONTROL[(i+CONTROL.length-1)%CONTROL.length],b=CONTROL[i],
                c=CONTROL[(i+1)%CONTROL.length],d=CONTROL[(i+2)%CONTROL.length];
            Point point=new Point(cat(a.x,b.x,c.x,d.x,t),cat(a.z,b.z,c.z,d.z,t));
            if(previous!=null)distance+=Math.hypot(point.x-previous.x,point.z-previous.z);
            PATH.add(point);DIST.add(distance);previous=point;
        }
        distance+=Math.hypot(PATH.get(0).x-previous.x,PATH.get(0).z-previous.z);LENGTH=distance;
    }
    private IceBoatRaceActivity(){}
    private static double cat(double a,double b,double c,double d,double t){return .5*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(1020);}
    static boolean exitReady(VisitData.Scene s){return s.loop.iceStage==3&&s.loop.iceResultAt==0;}
    private static void set(ServerLevel level,BlockPos at,Block block){ActivityBlocks.set(level,at,ActivityWorld.persistentLeaves(block),2);}
    private static Point point(double progress){
        double d=progress%LENGTH;if(d<0)d+=LENGTH;
        int lo=0,hi=PATH.size()-1;
        while(lo<hi){int mid=(lo+hi+1)>>>1;if(DIST.get(mid)<=d)lo=mid;else hi=mid-1;}
        int next=(lo+1)%PATH.size();double end=next==0?LENGTH:DIST.get(next);
        double t=Math.min(1,Math.max(0,(d-DIST.get(lo))/(end-DIST.get(lo))));
        Point a=PATH.get(lo),b=PATH.get(next);return new Point(a.x+(b.x-a.x)*t,a.z+(b.z-a.z)*t);
    }
    private static Point lane(double progress,double offset){
        Point at=point(progress),before=point(progress-.35),after=point(progress+.35);
        double dx=after.x-before.x,dz=after.z-before.z,len=Math.hypot(dx,dz);
        return new Point(at.x-dz/len*offset,at.z+dx/len*offset);
    }
    static Vec3 checkpointCenter(BlockPos c,int gate){
        Point at=point(gate*LENGTH/GATES);return new Vec3(c.getX()+.5+at.x,c.getY(),c.getZ()+.5+at.z);
    }
    private static double trackDistance(double x,double z){
        double best=Double.MAX_VALUE;
        for(Point at:PATH){double dx=x-at.x,dz=z-at.z;best=Math.min(best,dx*dx+dz*dz);}
        return Math.sqrt(best);
    }
    private static double trackRadius(double x,double z){
        if(Math.abs(x)<27&&z>28)return 6.6; // Grandstand/start stretch for overtakes.
        if(x>21&&z>-12&&z<11||x<-21&&z>-8&&z<15)return 3.9; // Two technical chicanes.
        if(Math.abs(z)>27)return 5.7;
        return TRACK_RADIUS;
    }
    private static boolean track(int x,int z){return trackDistance(x,z)<=trackRadius(x,z);}
    static void build(ServerLevel level,BlockPos c){
        if(level.getBlockState(c.below(5)).is(Blocks.LIME_GLAZED_TERRACOTTA))return;
        if(level.getBlockState(c.below(5)).is(Blocks.CYAN_GLAZED_TERRACOTTA))removeBuild(level,c);
        RandomSource random=RandomSource.create(0x1CEB0A7L);
        // Solid frozen lake over a sealed snowy island; the curving ice route is deliberately not a simple oval.
        for(int x=-55;x<=55;x++)for(int z=-55;z<=55;z++){
            double distance=trackDistance(x,z),radius=trackRadius(x,z);BlockPos floor=c.offset(x,-1,z);
            set(level,floor.below(),Blocks.STONE);set(level,floor,distance<=radius?
                ((Math.floorMod(x*7+z*13,9)==0)?Blocks.BLUE_ICE:Blocks.PACKED_ICE):
                Math.floorMod(x*3-z*5,19)==0?Blocks.CALCITE:Blocks.SNOW_BLOCK);
            if(distance>radius+.4&&distance<radius+1.6&&Math.floorMod(x+z,5)!=0)set(level,floor.above(),Blocks.PACKED_ICE);
            if(Math.abs(x)==55||Math.abs(z)==55){set(level,floor.above(),Blocks.SPRUCE_FENCE);set(level,floor.above(2),Blocks.SPRUCE_FENCE);}
        }
        shortcut(level,c,3,4);shortcut(level,c,10,11);
        // Start/finish strip, red-gold gantry, illuminated timer posts and a clear return concourse.
        for(int x=-5;x<=5;x++)for(int z=34;z<=38;z++)if(track(x,z))set(level,c.offset(x,-1,z),((x+z)&1)==0?Blocks.WHITE_CONCRETE:Blocks.BLACK_CONCRETE);
        for(int side:new int[]{-1,1}){
            int x=side*9;for(int y=0;y<=6;y++)set(level,c.offset(x,y,36),y%2==0?Blocks.GOLD_BLOCK:Blocks.RED_CONCRETE);
            set(level,c.offset(x,7,36),Blocks.SEA_LANTERN);
        }
        for(int x=-9;x<=9;x++)set(level,c.offset(x,6,36),x%3==0?Blocks.SEA_LANTERN:Blocks.RED_CONCRETE);
        for(int x=-11;x<=11;x++)for(int z=45;z<=54;z++)set(level,c.offset(x,-1,z),Math.floorMod(x+z,7)==0?Blocks.SEA_LANTERN:Blocks.POLISHED_DIORITE);
        for(int x=-12;x<=12;x++)for(int z=47;z<=53;z++)if(Math.abs(x)>=10){
            set(level,c.offset(x,0,z),Blocks.SPRUCE_PLANKS);if(Math.abs(x)>=11)set(level,c.offset(x,1,z),Blocks.RED_CARPET);
        }
        // Bridges over narrow chicanes are decorative overhead crossings, never collision ceilings for a boat.
        bridge(level,c,-31,5,true);bridge(level,c,31,-4,true);bridge(level,c,0,-31,false);
        // A frozen clockwork fountain and ice garden occupy the infield.
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){
            double r=Math.hypot(x,z);if(r<7)set(level,c.offset(x,-1,z),r>5?Blocks.QUARTZ_BLOCK:Blocks.BLUE_ICE);
            if(r>5&&r<6.5)set(level,c.offset(x,0,z),Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        for(int y=0;y<=7;y++)set(level,c.offset(0,y,0),y%2==0?Blocks.PACKED_ICE:Blocks.BLUE_ICE);
        for(int[] arm:new int[][]{{4,0},{-4,0},{0,4},{0,-4}})for(int y=1;y<=3;y++)set(level,c.offset(arm[0],y,arm[1]),y==3?Blocks.SEA_LANTERN:Blocks.PACKED_ICE);
        // Spectator stands and a cluster of brightly lit timber/cherry festival buildings.
        stand(level,c,-18,51);stand(level,c,18,51);
        lodge(level,c,-13,-12,Blocks.PINK_CONCRETE);lodge(level,c,13,-12,Blocks.CYAN_CONCRETE);
        lodge(level,c,-13,14,Blocks.PURPLE_CONCRETE);lodge(level,c,13,14,Blocks.YELLOW_CONCRETE);
        lodge(level,c,-43,-45,Blocks.LIGHT_BLUE_CONCRETE);lodge(level,c,43,-45,Blocks.MAGENTA_CONCRETE);
        lodge(level,c,-43,45,Blocks.CYAN_CONCRETE);lodge(level,c,43,45,Blocks.PINK_CONCRETE);
        kiosk(level,c,0,-15,Blocks.MAGENTA_WOOL,Blocks.CYAN_WOOL);
        kiosk(level,c,0,17,Blocks.LIGHT_BLUE_WOOL,Blocks.YELLOW_WOOL);
        for(int[] tree:new int[][]{{-5,-20},{5,23},{-47,0},{47,0},
            {-49,-30},{-50,-10},{-50,12},{-49,30},{49,-30},{50,-10},{50,12},{49,30},
            {-29,-48},{-11,-49},{11,-49},{29,-48},{-31,48},{31,48}})tree(level,c,tree[0],tree[1]);
        // Festival promenades wind through the infield without touching the driving surface.
        for(int x=-23;x<=23;x++)for(int z=-23;z<=23;z++){
            if(trackDistance(x,z)<8||Math.abs(x)<=8&&Math.abs(z)<=8)continue;
            if(Math.abs(x%12)<=1||Math.abs(z%12)<=1){
                BlockPos at=c.offset(x,-1,z);if(level.isEmptyBlock(at.above()))set(level,at,
                    Math.floorMod(x+z,13)==0?Blocks.SEA_LANTERN:Math.floorMod(x-z,4)==0?Blocks.PINK_CONCRETE:Blocks.LIGHT_BLUE_CONCRETE);
            }
        }
        for(int i=0;i<450;i++){
            int x=random.nextInt(103)-51,z=random.nextInt(103)-51;
            if(trackDistance(x,z)<8||Math.abs(x)<20&&Math.abs(z)<22||Math.abs(z)>44&&Math.abs(x)<25)continue;
            BlockPos at=c.offset(x,0,z);if(!level.isEmptyBlock(at))continue;
            set(level,at,i%11==0?Blocks.SEA_LANTERN:i%7==0?Blocks.PURPLE_CONCRETE:i%5==0?Blocks.CYAN_CONCRETE:i%3==0?Blocks.PINK_CONCRETE:Blocks.SNOW_BLOCK);
            if(i%9==0)set(level,at.above(),Blocks.SNOW);
        }
        // Lantern-lined bends, colored banners and ice sculptures punctuate the whole lap.
        for(int i=0;i<PATH.size();i+=18){
            Point at=lane(DIST.get(i),7.8);int x=(int)Math.round(at.x),z=(int)Math.round(at.z);
            if(Math.abs(x)>51||Math.abs(z)>51||trackDistance(x,z)<6.5)continue;
            BlockPos post=c.offset(x,0,z);if(!level.isEmptyBlock(post))continue;
            for(int y=0;y<3;y++)set(level,post.above(y),Blocks.SPRUCE_FENCE);
            set(level,post.above(3),Blocks.SEA_LANTERN);
            set(level,post.east(),i%3==0?Blocks.PURPLE_BANNER:i%3==1?Blocks.CYAN_BANNER:Blocks.PINK_BANNER);
        }
        for(int[] sculpture:new int[][]{{-48,21},{46,-24},{-6,22},{7,-23}}){
            BlockPos at=c.offset(sculpture[0],0,sculpture[1]);
            for(int y=0;y<4;y++)set(level,at.above(y),y==3?Blocks.SEA_LANTERN:Blocks.BLUE_ICE);
            for(int side:new int[]{-1,1}){set(level,at.offset(side,2,0),Blocks.PACKED_ICE);set(level,at.offset(0,2,side),Blocks.PACKED_ICE);}
        }
        for(int gate:new int[]{2,5,8,11,13})lightArch(level,c,gate);
        // Small drift obelisks force a choice of line while leaving the NPC lanes unobstructed.
        for(int i=0;i<16;i++){
            double along=(i+1.4)*LENGTH/18;Point at=lane(along,i%2==0?3.7:-3.7);
            BlockPos obstacle=c.offset((int)Math.round(at.x),0,(int)Math.round(at.z));
            if(level.isEmptyBlock(obstacle))set(level,obstacle,i%3==0?Blocks.SEA_LANTERN:Blocks.PACKED_ICE);
        }
        set(level,c.below(5),Blocks.LIME_GLAZED_TERRACOTTA);
    }
    private static void shortcut(ServerLevel l,BlockPos c,int from,int to){
        Point a=point(from*LENGTH/GATES),b=point(to*LENGTH/GATES);
        for(int step=0;step<=32;step++){
            double t=step/32.0,x=a.x+(b.x-a.x)*t,z=a.z+(b.z-a.z)*t;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                BlockPos at=c.offset((int)Math.round(x)+dx,-1,(int)Math.round(z)+dz);
                set(l,at,Math.floorMod(step+dx+dz,7)==0?Blocks.SEA_LANTERN:Blocks.BLUE_ICE);
                set(l,at.above(),Blocks.AIR);
            }
        }
        for(Point end:new Point[]{a,b}){
            int x=(int)Math.round(end.x),z=(int)Math.round(end.z);
            set(l,c.offset(x,0,z),Blocks.AIR);
        }
    }
    private static void kiosk(ServerLevel l,BlockPos c,int x,int z,Block a,Block b){
        for(int dx=-4;dx<=4;dx++)for(int dz=-3;dz<=3;dz++){
            BlockPos at=c.offset(x+dx,-1,z+dz);set(l,at,Blocks.SPRUCE_PLANKS);
            if(Math.abs(dx)==4||Math.abs(dz)==3){
                if(dz==3&&Math.abs(dx)<3)set(l,at.above(),Blocks.SPRUCE_SLAB);
                if(Math.abs(dx)==4&&Math.abs(dz)==3)for(int y=0;y<4;y++)set(l,at.above(y),Blocks.STRIPPED_CHERRY_LOG);
            }
            set(l,at.above(4),(dx+dz)%2==0?a:b);
        }
        for(int dx:new int[]{-3,0,3})set(l,c.offset(x+dx,3,z+3),Blocks.SEA_LANTERN);
        for(int dx:new int[]{-2,2})set(l,c.offset(x+dx,0,z+4),Blocks.PINK_TULIP);
    }
    private static void lightArch(ServerLevel l,BlockPos c,int gate){
        double along=gate*LENGTH/GATES;Point left=lane(along,-6.7),right=lane(along,6.7);
        for(int i=0;i<=14;i++){
            double t=i/14.0;int x=(int)Math.round(left.x+(right.x-left.x)*t),z=(int)Math.round(left.z+(right.z-left.z)*t);
            set(l,c.offset(x,6,z),i%3==0?Blocks.SEA_LANTERN:i%2==0?Blocks.MAGENTA_WOOL:Blocks.CYAN_WOOL);
        }
        for(Point end:new Point[]{left,right})for(int y=0;y<6;y++)set(l,c.offset((int)Math.round(end.x),y,(int)Math.round(end.z)),
            y%3==0?Blocks.SEA_LANTERN:Blocks.STRIPPED_CHERRY_LOG);
    }
    private static void bridge(ServerLevel l,BlockPos c,int x,int z,boolean acrossX){
        for(int n=-9;n<=9;n++)for(int w=-1;w<=1;w++){
            BlockPos p=c.offset(x+(acrossX?n:w),5+Math.abs(n)/5,z+(acrossX?w:n));set(l,p,Blocks.SPRUCE_PLANKS);
            if(w==-1||w==1)set(l,p.above(),Blocks.SPRUCE_FENCE);
        }
        for(int n:new int[]{-9,9})for(int y=0;y<5;y++)set(l,c.offset(x+(acrossX?n:0),y,z+(acrossX?0:n)),Blocks.STRIPPED_SPRUCE_LOG);
    }
    private static void stand(ServerLevel l,BlockPos c,int x,int z){
        for(int dx=-5;dx<=5;dx++)for(int dz=-4;dz<=4;dz++){
            BlockPos at=c.offset(x+dx,0,z+dz);set(l,at,Blocks.SPRUCE_PLANKS);
            // Solid terraces rise away from the track (north), not toward it.
            if(dz<=1)for(int y=1;y<=Math.floorDiv(dz+4,2);y++)set(l,at.above(y),Blocks.SPRUCE_PLANKS);
            if(Math.abs(dx)==5||dz==4)set(l,at.above(),Blocks.SPRUCE_FENCE);
        }
        for(int dx:new int[]{-5,5})for(int dz:new int[]{-4,4}){
            for(int y=1;y<=5;y++)set(l,c.offset(x+dx,y,z+dz),Blocks.STRIPPED_SPRUCE_LOG);
            set(l,c.offset(x+dx,6,z+dz),Blocks.SEA_LANTERN);
        }
        for(int dx=-5;dx<=5;dx++)for(int dz=-4;dz<=4;dz++)set(l,c.offset(x+dx,6,z+dz),Math.floorMod(dx+dz,4)==0?Blocks.LIGHT_BLUE_STAINED_GLASS:Blocks.MAGENTA_WOOL);
    }
    private static void lodge(ServerLevel l,BlockPos c,int x,int z,Block wall){
        for(int dx=-5;dx<=5;dx++)for(int dz=-4;dz<=4;dz++){
            BlockPos at=c.offset(x+dx,0,z+dz);set(l,at.below(),Blocks.SPRUCE_PLANKS);
            if(Math.abs(dx)==5||Math.abs(dz)==4)for(int y=0;y<=3;y++)set(l,at.above(y),y==2&&(dx==0||dz==0)?Blocks.LIGHT_BLUE_STAINED_GLASS:wall);
            set(l,at.above(4),Blocks.DARK_OAK_PLANKS);if((dx+dz)%5==0)set(l,at.above(5),Blocks.SNOW_BLOCK);
        }
        set(l,c.offset(x,1,z+4),Blocks.AIR);set(l,c.offset(x,2,z+4),Blocks.AIR);
        for(int dx:new int[]{-4,4}){set(l,c.offset(x+dx,1,z+5),Blocks.SEA_LANTERN);set(l,c.offset(x+dx,2,z+5),Blocks.PINK_BANNER);}
    }
    private static void tree(ServerLevel l,BlockPos c,int x,int z){
        BlockPos root=c.offset(x,0,z);for(int y=0;y<6;y++)set(l,root.above(y),Blocks.SPRUCE_LOG);
        for(int y=2;y<=7;y++){int radius=y<5?3:y<7?2:1;
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)if(Math.abs(dx)+Math.abs(dz)<=radius+1){
                BlockPos at=root.offset(dx,y,dz);set(l,at,y%3==0&&Math.floorMod(dx*3+dz,5)==0?Blocks.SNOW_BLOCK:Blocks.SPRUCE_LEAVES);
            }
        }
        set(l,root.above(8),Blocks.SEA_LANTERN);
    }
    static void removeBuild(ServerLevel level,BlockPos c){
        if(!level.getBlockState(c.below(5)).is(Blocks.CYAN_GLAZED_TERRACOTTA)&&
            !level.getBlockState(c.below(5)).is(Blocks.LIME_GLAZED_TERRACOTTA))return;
        for(int x=-56;x<=56;x++)for(int z=-56;z<=56;z++)for(int y=-5;y<=9;y++){
            BlockPos at=c.offset(x,y,z);if(!level.isEmptyBlock(at))set(level,at,Blocks.AIR);
        }
    }
    static void clean(ServerLevel level,VisitData.Scene s,BlockPos c){
        List<Entity> remove=new ArrayList<>();
        for(Entity e:level.getEntitiesOfClass(Entity.class,new AABB(c).inflate(58,16,58),e->e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))remove.add(e);
        remove.forEach(e->{e.ejectPassengers();e.discard();});
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        BlockPos c=center(p,s);build(level,c);clean(level,s,c);
        var st=s.loop;st.iceStage=0;st.iceLap=0;st.iceCheckpoint=0;st.iceRank=0;st.iceRewarded=false;
        st.iceVoiceUntil=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.ICE_BOAT_RACE));
        st.iceCountdownUntil=0;st.iceResultAt=0;st.iceSeed=p.getRandom().nextLong();
        Arrays.fill(st.iceRivalProgress,0);Arrays.fill(st.iceRivalFinish,0);
        ensureFather(p,s,level,c);ensureSpectators(p,s,level,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        LoopProps.hold(level,c);LoopProps.hold(level,c.south(51));build(level,c);ensureFather(p,s,level,c);ensureSpectators(p,s,level,c);
        if(s.loop.iceStage==1||s.loop.iceStage==2){
            for(int i=0;i<RIVALS;i++)ensureRacer(level,s,c,i);
            if(boat(level,s,c,-1)==null){
                double safe=(s.loop.iceLap*GATES+s.loop.iceCheckpoint)*LENGTH/GATES;
                spawnBoat(level,s,c,-1,safe,-1.2);
            }
        }
        level.setWeatherParameters(120000,0,false,false);level.setRainLevel(0);level.setThunderLevel(0);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0));
    }
    private static void mark(Entity e,VisitData.Scene s,int slot){e.getPersistentData().putUUID(OWNER,s.owner);e.getPersistentData().putInt(GEN,s.loop.generation);e.getPersistentData().putInt(SLOT,slot);e.getPersistentData().putBoolean("CoralineDecoration",true);}
    private static boolean owned(Entity e,VisitData.Scene s){return e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))&&e.getPersistentData().getInt(GEN)==s.loop.generation;}
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        if(!l.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(46)).inflate(6),e->owned(e,s)&&e.getPersistentData().getBoolean("CoralineIceFather")).isEmpty())return;
        var family=ParentAccess.family(p.server,s.owner);var realm=RealmData.get(p.server).realms.get(s.door);
        var father=ModEntities.DAD.get().create(l);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);mark(father,s,-10);
        father.getPersistentData().putBoolean("CoralineIceFather",true);father.getPersistentData().putUUID("RealmDoor",s.door);
        father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+46.5);father.setYRot(90);father.setYBodyRot(90);l.addFreshEntity(father);
    }
    private static void ensureSpectators(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        // Repair existing stands as well as fresh visits.
        for(int side:new int[]{-18,18}){
            for(int x=-4;x<=4;x++)for(int z=-4;z<=1;z++)for(int y=1;y<=4;y++)set(l,c.offset(side+x,y,51+z),Blocks.AIR);
            stand(l,c,side,51);
        }
        int[][] seats={{-21,1,47},{-19,1,47},{-17,1,47},{-15,1,47},{15,1,47},{17,1,47},{19,1,47},{21,1,47},
            {-20,2,49},{-18,2,49},{-16,2,49},{16,2,49},{18,2,49},{20,2,49}};
        for(int i=0;i<seats.length;i++){
            int slot=i;var existing=l.getEntitiesOfClass(Villager.class,new AABB(c).inflate(55,10,55),v->owned(v,s)&&v.getPersistentData().getInt(SLOT)==slot);
            Villager v=existing.isEmpty()?EntityType.VILLAGER.create(l):existing.get(0);if(v==null)continue;
            int[] seat=seats[i];v.setPos(c.getX()+seat[0]+.5,c.getY()+seat[1],c.getZ()+seat[2]+.5);
            v.setNoAi(true);v.setPersistenceRequired();v.setCustomName(Component.literal(VillageActivity.RESIDENT_NAME));v.setCustomNameVisible(false);
            v.setYRot(180);v.setYBodyRot(180);v.setYHeadRot(180);mark(v,s,i);if(existing.isEmpty())l.addFreshEntity(v);
            var chairs=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(55,10,55),e->e.kind()==ScenePropEntity.CHAIR&&e.slot==300+slot&&s.owner.equals(e.owner));
            var chair=chairs.isEmpty()?LoopProps.spawn(l,s,ScenePropEntity.CHAIR,new Vec3(c.getX()+seat[0]+.5,c.getY()+seat[1],c.getZ()+seat[2]+.5),300+slot):chairs.get(0);
            mark(chair,s,300+slot);
            chair.setYRot(180);if(v.getVehicle()!=chair)v.startRiding(chair,true);
        }
    }
    private static Boat boat(ServerLevel level,VisitData.Scene s,BlockPos c,int slot){
        return level.getEntitiesOfClass(Boat.class,new AABB(c).inflate(54,10,54),b->owned(b,s)&&b.getPersistentData().getInt(SLOT)==slot).stream().findFirst().orElse(null);
    }
    private static Boat spawnBoat(ServerLevel l,VisitData.Scene s,BlockPos c,int slot,double progress,double lane){
        Boat b=EntityType.BOAT.create(l);if(b==null)return null;b.setVariant(Boat.Type.SPRUCE);mark(b,s,slot);
        Point at=lane(progress,lane),ahead=lane(progress+1,lane);b.setPos(c.getX()+.5+at.x,c.getY(),c.getZ()+.5+at.z);
        b.setYRot((float)Math.toDegrees(Math.atan2(-(ahead.x-at.x),ahead.z-at.z)));
        if(slot>=0)b.setNoGravity(true); // The player's boat must retain normal ice-boat controls.
        l.addFreshEntity(b);return b;
    }
    private static void ensureRacer(ServerLevel l,VisitData.Scene s,BlockPos c,int i){
        Boat b=boat(l,s,c,i);if(b==null)b=spawnBoat(l,s,c,i,s.loop.iceRivalProgress[i],i%2==0?1.5:-1.5);
        if(b==null)return;
        int slot=i;var riders=l.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(54,10,54),e->owned(e,s)&&e.getPersistentData().getInt(SLOT)==slot);
        var npc=riders.isEmpty()?null:riders.get(0);
        if(npc==null){npc=ModEntities.FISHING_CONTEST_NPC.get().create(l);if(npc==null)return;
            npc.configure(Math.floorMod((int)(s.loop.iceSeed>>(i*7)),64),i%2==0,false,i);npc.setNoAi(true);
            npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            npc.setCustomName(Component.literal("Button-Eyed Racer "+(i+1)));npc.setCustomNameVisible(true);
            mark(npc,s,i);npc.setPos(b.getX(),b.getY(),b.getZ());l.addFreshEntity(npc);
        }
        if(npc.getVehicle()!=b)npc.startRiding(b,true);
    }
    private static void launch(ServerPlayer p,VisitData.Scene s,BlockPos c){
        ServerLevel l=p.serverLevel();Boat player=boat(l,s,c,-1);if(player==null)player=spawnBoat(l,s,c,-1,0,-1.2);
        if(player!=null){
            player.setCustomName(Component.literal("CoralineRaceCountdownLock"));player.setCustomNameVisible(false);
            // The first boat passenger owns the controls. An invisible temporary
            // passenger makes the countdown a real starting lock on both client
            // and server, without teleporting/rotating the player's camera.
            ArmorStand lock=EntityType.ARMOR_STAND.create(l);
            if(lock!=null){lock.setInvisible(true);lock.setNoGravity(true);
                lock.getPersistentData().putBoolean("CoralineIceStartLock",true);mark(lock,s,-1);
                lock.setPos(player.getX(),player.getY(),player.getZ());l.addFreshEntity(lock);lock.startRiding(player,true);
            }
            p.stopRiding();p.teleportTo(l,player.getX(),player.getY()+.2,player.getZ(),player.getYRot(),0);p.startRiding(player,true);
        }
        for(int i=0;i<RIVALS;i++){stagger(s.loop,i);ensureRacer(l,s,c,i);}
        p.sendSystemMessage(Component.literal("Three laps. Follow the glowing bends and pass every checkpoint. Steer your boat to win!"));
    }
    private static void stagger(OtherMotherLoopState state,int slot){state.iceRivalProgress[slot]=-2.5-slot*1.7;}
    private static void releaseStartLock(Boat boat){
        if(boat==null)return;
        boat.setCustomName(null);
        for(Entity rider:List.copyOf(boat.getPassengers()))if(rider.getPersistentData().getBoolean("CoralineIceStartLock")){
            rider.stopRiding();rider.discard();
        }
    }
    static void moveRivals(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var st=s.loop;ServerLevel l=p.serverLevel();int finished=0;
        for(int i=0;i<RIVALS;i++){
            if(st.iceRivalFinish[i]>0){finished++;continue;}
            Boat b=boat(l,s,c,i);if(b==null||b.getPassengers().isEmpty()){ensureRacer(l,s,c,i);b=boat(l,s,c,i);}
            if(b==null)continue;
            double skill=.45+i*.021;double wave=Math.sin((now+i*71)*.035)*.034;
            double mistake=Math.floorMod(now+i*137+(int)(st.iceSeed&255),410)<15?-.19:0;
            double speed=Math.max(.20,skill+wave+mistake);
            st.iceRivalProgress[i]+=speed;
            if(st.iceRivalProgress[i]>=LENGTH*LAPS){
                st.iceRivalProgress[i]=LENGTH*LAPS;st.iceRivalFinish[i]=1+finished;finished++;
                b.setPos(c.getX()+14+i*2,c.getY(),c.getZ()+47);b.setDeltaMovement(Vec3.ZERO);continue;
            }
            double offset=(i%2==0?1:-1)*(1.1+.35*Math.sin(st.iceRivalProgress[i]*.07+i));
            // A faster boat shifts toward a neighboring lane; small independent oscillations prevent a train.
            if(i>=3)offset*=.6;
            Point at=lane(st.iceRivalProgress[i],offset),ahead=lane(st.iceRivalProgress[i]+1,offset);
            b.setPos(c.getX()+.5+at.x,c.getY(),c.getZ()+.5+at.z);
            float yaw=(float)Math.toDegrees(Math.atan2(-(ahead.x-at.x),ahead.z-at.z));b.setYRot(yaw);b.setDeltaMovement(Vec3.ZERO);
            for(Entity passenger:b.getPassengers())if(passenger instanceof FishingContestNpcEntity racer){
                racer.setYRot(yaw);racer.setYBodyRot(yaw);racer.setYHeadRot(yaw);
            }
        }
        if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    private static double nearestProgress(double x,double z){
        double best=Double.MAX_VALUE,progress=0;
        for(int i=0;i<PATH.size();i++){
            int next=(i+1)%PATH.size();Point a=PATH.get(i),b=PATH.get(next);
            double dx=b.x-a.x,dz=b.z-a.z,len=dx*dx+dz*dz;
            double t=Math.max(0,Math.min(1,((x-a.x)*dx+(z-a.z)*dz)/len));
            double ox=x-(a.x+t*dx),oz=z-(a.z+t*dz),d=ox*ox+oz*oz;
            if(d<best){best=d;double end=next==0?LENGTH:DIST.get(next);progress=DIST.get(i)+(end-DIST.get(i))*t;}
        }
        return best>64?-1:progress;
    }
    private static double playerProgress(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var st=s.loop;double here=nearestProgress(p.getX()-c.getX()-.5,p.getZ()-c.getZ()-.5);
        double lower=st.iceCheckpoint*LENGTH/GATES,upper=(st.iceCheckpoint+1)*LENGTH/GATES;
        return st.iceLap*LENGTH+(here<0?lower:Math.max(lower,Math.min(upper,here)));
    }
    private static int position(ServerPlayer p,VisitData.Scene s,BlockPos c){
        double progress=playerProgress(p,s,c);int ahead=0;
        for(int i=0;i<RIVALS;i++)if(s.loop.iceRivalFinish[i]>0||s.loop.iceRivalProgress[i]>progress+.5)ahead++;
        return ahead+1;
    }
    private static void result(ServerPlayer p,VisitData.Scene s,BlockPos c,int rank){
        var st=s.loop;if(st.iceStage==3)return;st.iceStage=3;st.iceRank=rank;st.iceResultAt=OtherMotherLoop.now(p)+70;
        p.stopRiding();p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+48.5,0,0);p.setDeltaMovement(Vec3.ZERO);
        if(rank==1&&!st.iceRewarded){st.iceRewarded=true;
            for(ItemStack reward:new ItemStack[]{new ItemStack(Items.DIAMOND,6),new ItemStack(Items.EMERALD,10),new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)}){
                p.getInventory().add(reward);if(!reward.isEmpty())p.drop(reward,false);
            }
            p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,1,1);
            p.sendSystemMessage(Component.literal("FIRST PLACE! You won the Ice Boat Race and Father's champion prize!"));
        }else p.sendSystemMessage(Component.literal(rank>RIVALS+1?"Race time expired — you did not finish. No winner's prize.":
            "You finished "+rank+" / "+(RIVALS+1)+". Only first place receives the champion prize."));
        OtherMotherLoop.changed(p);
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(!p.getUUID().equals(s.owner))return;
        var st=s.loop;long now=OtherMotherLoop.now(p);ServerLevel l=p.serverLevel();
        if(p.tickCount%20==0)prepare(p,s,l,c);
        if(p.getY()<c.getY()-6||Math.abs(p.getX()-c.getX())>54||Math.abs(p.getZ()-c.getZ())>54){
            p.stopRiding();p.teleportTo(l,c.getX()+.5,c.getY(),c.getZ()+48.5,0,0);p.setDeltaMovement(Vec3.ZERO);
            if(st.iceStage==2)p.sendSystemMessage(Component.literal("You left the frozen course. Return to your race boat at the start."));
        }
        switch(st.iceStage){
            case 0->{if(now>=st.iceVoiceUntil){launch(p,s,c);st.iceStage=1;st.iceCountdownUntil=now+80;OtherMotherLoop.changed(p);}}
            case 1->{
                Boat b=boat(l,s,c,-1);if(b!=null){b.setCustomName(Component.literal("CoralineRaceCountdownLock"));Point at=lane(0,-1.2);b.setPos(c.getX()+.5+at.x,c.getY(),c.getZ()+.5+at.z);b.setDeltaMovement(Vec3.ZERO);}
                if(now>=st.iceCountdownUntil){releaseStartLock(b);st.iceStage=2;OtherMotherLoop.changed(p);p.sendSystemMessage(Component.literal("GO! Three laps around the frozen festival!"));}
                else if((st.iceCountdownUntil-now)%20==0)p.displayClientMessage(Component.literal("Ice Boat Race starts in "+Math.max(1,(st.iceCountdownUntil-now+19)/20)+"…"),true);
            }
            case 2->{
                moveRivals(p,s,c,now);Boat b=boat(l,s,c,-1);
                releaseStartLock(b); // Also recovers a race reloaded just after the countdown.
                if(b!=null&&p.getVehicle()==b){
                    double x=b.getX()-c.getX()-.5,z=b.getZ()-c.getZ()-.5;
                    double next=(st.iceCheckpoint+1)*LENGTH/GATES;
                    Point gate=point(next);double gateDistance=Math.hypot(x-gate.x,z-gate.z);
                    if(gateDistance<6.2&&trackDistance(x,z)<trackRadius(x,z)+.7){
                        st.iceCheckpoint++;
                        if(st.iceCheckpoint==GATES){st.iceCheckpoint=0;st.iceLap++;l.playSound(null,b.blockPosition(),SoundEvents.NOTE_BLOCK_BELL.get(),SoundSource.PLAYERS,1,1.2F);
                            if(st.iceLap>=LAPS){result(p,s,c,1+(int)Arrays.stream(st.iceRivalFinish).filter(n->n>0).count());return;}
                            p.sendSystemMessage(Component.literal("Lap "+st.iceLap+" complete — "+(LAPS-st.iceLap)+" to go!"));
                        }
                        OtherMotherLoop.changed(p);
                    }
                    if(trackDistance(x,z)>12){
                        double safe=st.iceCheckpoint*LENGTH/GATES+2;Point back=lane(safe,-1.2);b.setPos(c.getX()+.5+back.x,c.getY(),c.getZ()+.5+back.z);b.setDeltaMovement(Vec3.ZERO);
                        p.displayClientMessage(Component.literal("Off course — returned to your last checkpoint."),true);
                    }
                }else if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Board your own race boat to continue."),true);
                if(p.tickCount%10==0)p.displayClientMessage(Component.literal("Position: "+position(p,s,c)+" / 7  |  Lap: "+Math.min(LAPS,st.iceLap+1)+" / "+LAPS+"  |  Checkpoint: "+st.iceCheckpoint+" / "+GATES),true);
                if(now>st.iceCountdownUntil+6*60*20)result(p,s,c,8);
            }
            case 3->{if(st.iceResultAt>0&&now>=st.iceResultAt){st.iceResultAt=0;OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);}
                if(st.iceResultAt==0){
                if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Race complete. The return portal is at the glowing start pavilion."),true);
            }}
            default->{}
        }
        if(st.iceStage<3){
            for(ParentEntity father:l.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(46)).inflate(6),e->owned(e,s)&&e.getPersistentData().getBoolean("CoralineIceFather"))){
                double dx=p.getX()-father.getX(),dz=p.getZ()-father.getZ();float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));father.setYRot(yaw);father.setYBodyRot(yaw);father.setYHeadRot(yaw);
            }
        }
    }
}
