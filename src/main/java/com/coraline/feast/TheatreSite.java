package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** The single shared Player Party / special Theatre venue, reached through the real cellar doors. */
public final class TheatreSite {
    public static final int WIDTH=60,DEPTH=88,HEIGHT=38,ROWS=14,SEATS_PER_ROW=20;
    public static final int RESERVED_ROW=7,PLAYER_COLUMN=9,COMPANION_COLUMN=8;
    public static boolean reserved(int row,int column){return false;} // Owner-requested clear-view aisle seats.
    public static BlockPos center(ServerPlayer p,VisitData.Scene s){return ShowSiteData.get(p.server).center(s.owner,false);}
    private static boolean stepped(ServerPlayer p,VisitData.Scene s){return ShowSiteData.get(p.server).owner(s.owner).getBoolean("TheatreSteppedSeating");}
    public static BlockPos entrance(ServerPlayer p,VisitData.Scene s){return center(p,s).offset(0,stepped(p,s)?4:0,40);}
    public static BlockPos seat(ServerPlayer p,VisitData.Scene s,int index){int row=Math.floorDiv(index,SEATS_PER_ROW),col=Math.floorMod(index,SEATS_PER_ROW);int x=col<10?-24+col*2:6+(col-10)*2;return center(p,s).offset(x,stepped(p,s)?row/3:0,-3+row*3);}
    public static BlockPos playerSeat(ServerPlayer p,VisitData.Scene s){return center(p,s).offset(-2,stepped(p,s)?2:0,18);}
    public static BlockPos companionSeat(ServerPlayer p,VisitData.Scene s){return center(p,s).offset(0,stepped(p,s)?2:0,18);}
    public static BlockPos houseDoor(ServerPlayer p,VisitData.Scene s){var r=RealmData.get(p.server).realms.get(s.door);if(r==null||r.house==null)return null;var room=Arrays.stream(HouseCabinets.ROOMS).filter(x->x.id().equals("basement")).findFirst().orElseThrow();var at=room.doors()[0];return HouseCabinets.at(r.house,at[0],at[1],at[2]);}
    public static boolean atEntrance(ServerPlayer p,VisitData.Scene s){var at=houseDoor(p,s);return s.owner.equals(p.getUUID())&&OtherWorld.isOther(p.level())&&at!=null&&p.distanceToSqr(Vec3.atBottomCenterOf(at))<9;}
    public static boolean inside(ServerPlayer p,VisitData.Scene s){return allowed(p,s,p.level().dimension(),p.position());}
    public static boolean allowed(ServerPlayer p,VisitData.Scene s,ResourceKey<Level> dimension,Vec3 at){var c=center(p,s);return s.owner.equals(p.getUUID())&&dimension.equals(ActivityWorld.KEY)&&new AABB(c.offset(-30,-2,-44),c.offset(31,40,45)).contains(at);}
    public static boolean atExit(ServerPlayer p,VisitData.Scene s){return inside(p,s)&&p.distanceToSqr(Vec3.atBottomCenterOf(entrance(p,s)))<9;}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return false;
        var level=p.server.getLevel(ActivityWorld.KEY);if(level==null||houseDoor(p,s)==null)return false;var c=center(p,s);var data=ShowSiteData.get(p.server);var n=data.owner(s.owner);level.getChunkAt(c);
        boolean newlyBuilt=!n.getBoolean("TheatreBuilt");
        if(newlyBuilt){
            // Allocation is unique; still refuse existing blocks rather than carving anyone's build.
            for(var at:BlockPos.betweenClosed(c.offset(-30,-2,-44),c.offset(30,38,44)))if(!level.getBlockState(at).isAir())return false;
            for(int x=-30;x<=30;x++)for(int z=-44;z<=44;z++){
                put(level,c.offset(x,-1,z),Math.abs(x)<3?Blocks.RED_TERRACOTTA:Blocks.DARK_OAK_PLANKS);put(level,c.offset(x,38,z),Blocks.BLACK_TERRACOTTA);
                if(Math.abs(x)==30||Math.abs(z)==44)for(int y=0;y<38;y++)if(!(z==44&&Math.abs(x)<3&&y<5))put(level,c.offset(x,y,z),y%9==0?Blocks.GOLD_BLOCK:Blocks.RED_TERRACOTTA);
            }
            // Raised deep stage, walkable side ramps, grand fluted columns, ornate balcony tiers.
            for(int x=-25;x<=25;x++)for(int z=-43;z<=-16;z++)put(level,c.offset(x,0,z),Blocks.DARK_OAK_PLANKS);
            for(int x:new int[]{-27,-22,22,27})for(int y=0;y<30;y++)for(int z=-18;z<=-16;z++)put(level,c.offset(x,y,z),y<2||y>27?Blocks.CHISELED_RED_SANDSTONE:Blocks.CUT_RED_SANDSTONE);
            for(int side:new int[]{-1,1})for(int tier=0;tier<3;tier++)for(int z=-10;z<=34;z++){
                int y=8+tier*8;for(int x=24;x<30;x++)put(level,c.offset(side*x,y,z),Blocks.DARK_OAK_PLANKS);put(level,c.offset(side*24,y+1,z),Blocks.DARK_OAK_FENCE);
                if(z%6==0){put(level,c.offset(side*28,y+4,z),Blocks.SHROOMLIGHT);put(level,c.offset(side*27,y+3,z),Blocks.CHISELED_RED_SANDSTONE);}
            }
            for(int x=-20;x<=20;x+=4)put(level,c.offset(x,0,-15),Blocks.REDSTONE_LAMP);
            for(int x:new int[]{-28,28})for(int z=-8;z<=35;z+=7)put(level,c.offset(x,5,z),Blocks.SHROOMLIGHT);
            referenceDetails(level,c);
            n.putBoolean("TheatreBuilt",true);data.setDirty();
        }
        // Existing occupied saves retain their old coordinates until an empty, unobstructed venue
        // can receive the risers. Never build new flooring through a returning player or actor.
        prepareSteppedSeating(level,c,n,data);
        if(newlyBuilt)for(int z=0;z<=39;z+=3)for(int x:new int[]{-3,3}){
            int height=stepped(p,s)&&z>=5?Math.min(4,(z+4)/9):0;
            put(level,c.offset(x,height-1,z),Blocks.SHROOMLIGHT);
        }
        ShowStageLease.hold(p,s,false);
        migrateReservedSeats(p,s,level);
        ensure(p,s,level,CinematicPropEntity.CURTAIN,c.north(16),0);
        ensure(p,s,level,CinematicPropEntity.SEAT,playerSeat(p,s),0);ensure(p,s,level,CinematicPropEntity.SEAT,companionSeat(p,s),1);
        for(int row=0;row<ROWS;row++){var seats=ensure(p,s,level,CinematicPropEntity.DOG_ROW,c.offset(0,stepped(p,s)?row/3:0,-3+row*3),row);if(seats!=null&&VisitEvents.type(s)==VisitEvents.Type.PLAYER_PARTY)seats.frame(0,0,-1);}
        return true;
    }
    /** Move only our known row seats, including their saved riders.
     * Never rebuild the venue or relocate arbitrary seats/player edits. */
    private static void migrateReservedSeats(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        var c=center(p,s);
        for(int slot=0;slot<2;slot++){
            int assigned=slot;var old=Vec3.atBottomCenterOf(seat(p,s,RESERVED_ROW*SEATS_PER_ROW+(slot==0?PLAYER_COLUMN:COMPANION_COLUMN)));
            var target=Vec3.atBottomCenterOf(slot==0?playerSeat(p,s):companionSeat(p,s));
            var bounds=new AABB(target.x-.4,target.y+.05,target.z-.4,target.x+.4,target.y+1.9,target.z+.4);
            if(!level.noCollision(bounds)||level.containsAnyLiquid(bounds))continue;
            for(var seat:level.getEntitiesOfClass(CinematicPropEntity.class,new AABB(old,old).inflate(.2),e->e.owned(s)&&e.kind()==CinematicPropEntity.SEAT&&e.slot()==assigned))seat.setPos(target);
        }
    }
    /** Reference-image ornament for newly allocated venues; existing saved builds are not overwritten. */
    private static void referenceDetails(ServerLevel level,BlockPos c){
        // Tall gold-orange flutes at the proscenium and the audience entrance.
        for(int side:new int[]{-1,1})for(int z:new int[]{-15,39}){
            int x=side*(z<0?23:28);
            for(int y=0;y<=30;y++)for(int flute=-1;flute<=1;flute++)
                put(level,c.offset(x+flute,y,z),y<2||y>28?Blocks.CHISELED_RED_SANDSTONE:flute==0?Blocks.WAXED_CUT_COPPER:Blocks.CUT_RED_SANDSTONE);
        }
        // Curved high arch encloses the red upper valance, rather than a flat lintel.
        for(int x=-22;x<=22;x++){
            int arch=28+(int)Math.round(6*Math.sqrt(Math.max(0,1-x*x/484.0)));
            for(int y=arch;y<=arch+1;y++)put(level,c.offset(x,y,-15),Blocks.CUT_RED_SANDSTONE);
        }
        // Individual stacked boxes: curved stepped gold fronts and red back panels.
        for(int side:new int[]{-1,1})for(int tier=0;tier<3;tier++)for(int box=0;box<4;box++){
            int y=8+tier*8,z=-6+box*11;
            for(int dz=-3;dz<=3;dz++){
                int inset=Math.abs(dz)==3?1:0;
                put(level,c.offset(side*(24+inset),y,z+dz),Blocks.CUT_RED_SANDSTONE);
                put(level,c.offset(side*(25+inset),y-1,z+dz),Blocks.CHISELED_RED_SANDSTONE);
                put(level,c.offset(side*29,y+2,z+dz),Blocks.RED_WOOL);
            }
            for(int dy=1;dy<=6;dy++)put(level,c.offset(side*28,y+dy,z+5),Blocks.CUT_RED_SANDSTONE);
        }
    }
    private static void prepareSteppedSeating(ServerLevel level,BlockPos c,CompoundTag n,ShowSiteData data){
        if(n.getBoolean("TheatreSteppedSeating")||!level.getEntitiesOfClass(LivingEntity.class,new AABB(c.offset(-30,-2,-44),c.offset(31,40,45))).isEmpty())return;
        var floor=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int z=5;z<=43;z++){
            int height=Math.min(4,(z+4)/9);boolean riser=(z+4)%9==0;
            for(int x=-25;x<=25;x++)for(int y=0;y<height;y++){
                var state=Math.abs(x)<3&&y==height-1?(riser?Blocks.RED_NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH):Blocks.RED_TERRACOTTA.defaultBlockState()):Blocks.DARK_OAK_PLANKS.defaultBlockState();
                var at=c.offset(x,y,z);var old=level.getBlockState(at);
                if(!old.isAir()&&!old.equals(state))return; // Preserve later scenery/player edits.
                floor.put(at,state);
            }
        }
        for(var entry:floor.entrySet())if(!level.getBlockState(entry.getKey()).equals(entry.getValue()))ActivityBlocks.set(level,entry.getKey(),entry.getValue(),2);
        n.putBoolean("TheatreSteppedSeating",true);data.setDirty();
    }
    public static void audience(ServerPlayer p,VisitData.Scene s){var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return;for(int row=0;row<ROWS;row++){var audience=ensure(p,s,l,CinematicPropEntity.DOG_ROW,center(p,s).offset(0,stepped(p,s)?row/3:0,-3+row*3),row);if(audience!=null)audience.frame(0,0,0);}}
    public static CinematicPropEntity ensure(ServerPlayer p,VisitData.Scene s,ServerLevel level,int kind,BlockPos at,int slot){
        var found=level.getEntitiesOfClass(CinematicPropEntity.class,new AABB(at).inflate(kind==CinematicPropEntity.SEAT||kind==CinematicPropEntity.STUNT_CARRIER?96:kind==CinematicPropEntity.DOG_ROW?8:2),e->e.owned(s)&&e.kind()==kind&&e.slot()==slot);if(!found.isEmpty()){found.sort(Comparator.comparing(e->!e.isVehicle()));for(int i=1;i<found.size();i++)if(!found.get(i).isVehicle())found.get(i).discard();var prop=found.get(0);if((kind==CinematicPropEntity.DOG_ROW||kind==CinematicPropEntity.SEAT)&&!prop.isVehicle()&&prop.distanceToSqr(Vec3.atBottomCenterOf(at))>.0001)prop.setPos(Vec3.atBottomCenterOf(at));return prop;}
        var prop=CinematicPropEntity.TYPE.get().create(level);if(prop==null)return null;prop.configure(s,kind,slot);prop.setPos(Vec3.atBottomCenterOf(at));return level.addFreshEntity(prop)?prop:null;
    }
    public static boolean enter(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID())||!prepare(p,s))return false;if(inside(p,s))return true;if(!atEntrance(p,s))return false;return ShowTravel.enter(p,s,"TheatreReturn",entrance(p,s));}
    public static boolean exit(ServerPlayer p,VisitData.Scene s){return ShowTravel.exit(p,s,"TheatreReturn");}
    public static Mob enterGuest(Mob guest,ServerPlayer p,VisitData.Scene s){return ShowTravel.guest(guest,p,s,p.server.getLevel(ActivityWorld.KEY),Vec3.atBottomCenterOf(entrance(p,s)),true,"TheatreGuestReturn");}
    public static Mob exitGuest(Mob guest,ServerPlayer p,VisitData.Scene s){return ShowTravel.returnGuest(guest,p,s,"TheatreGuestReturn");}
    public static boolean seatGuest(Mob guest,ServerPlayer p,VisitData.Scene s,int index){if(guest==null||index<0||index>=ROWS*SEATS_PER_ROW||reserved(index/SEATS_PER_ROW,index%SEATS_PER_ROW)||guest.level()!=p.level()||!inside(p,s))return false;var at=seat(p,s,index);var prop=ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,at,index+2);if(prop==null)return false;prop.frame(-1,0,0);if(guest.getVehicle()==prop)return true;if(guest.distanceToSqr(prop)>4){TheatreVisit.navigate(guest,at,p,false);return false;}return guest.startRiding(prop,true);}
    public static void unseatGuest(Mob guest,ServerPlayer p,VisitData.Scene s){if(guest!=null&&guest.getVehicle() instanceof CinematicPropEntity prop&&prop.owned(s))guest.stopRiding();}
    public static boolean returnAll(ServerPlayer p,VisitData.Scene s){return ShowTravel.returnAll(p,s,"TheatreGuestReturn");}
    public static boolean release(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID())||inside(p,s)||!returnAll(p,s))return false;var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return false;for(var prop:l.getEntitiesOfClass(CinematicPropEntity.class,new AABB(center(p,s)).inflate(100),e->e.owned(s))){prop.ejectPassengers();prop.discard();}ShowStageLease.release(p,s,false);return true;}
    private static void put(ServerLevel level,BlockPos at,Block block){ActivityBlocks.set(level,at,block.defaultBlockState(),2);}
    private TheatreSite(){}
}
