package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Separate owner-allocated circus destination using the theatre's reversible travel primitive. */
public final class MouseCircusSite {
    public static BlockPos center(ServerPlayer p,VisitData.Scene s){return ShowSiteData.get(p.server).center(s.owner,true);}
    public static BlockPos audience(ServerPlayer p,VisitData.Scene s){return center(p,s).south(24);}
    public static BlockPos meeting(ServerPlayer p,VisitData.Scene s){var r=RealmData.get(p.server).realms.get(s.door);return r==null||r.house==null?null:r.porch!=null?r.porch:HouseCabinets.at(r.house,34,6,10);}
    public static BlockPos exit(ServerPlayer p,VisitData.Scene s){return center(p,s).south(37);}
    public static HouseCabinets.Room attic(){return Arrays.stream(HouseCabinets.ROOMS).filter(r->r.id().equals("attic")).findFirst().orElseThrow();}
    public static boolean inAttic(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&OtherWorld.isOther(p.level())&&HouseCabinets.inRoom(p,attic());}
    public static BlockPos atticDoor(ServerPlayer p,VisitData.Scene s){var r=RealmData.get(p.server).realms.get(s.door);if(r==null||r.house==null)return null;var d=attic().doors()[0];return HouseCabinets.at(r.house,d[0],d[1],d[2]);}
    public static BlockPos tent(ServerPlayer p,VisitData.Scene s){var n=MouseCircusVisit.data(s);return n.contains("Tent")?BlockPos.of(n.getLong("Tent")):null;}
    public static boolean inside(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&ActivityWorld.isActivity(p.level())&&new AABB(center(p,s).offset(-42,-2,-42),center(p,s).offset(43,46,43)).contains(p.position());}
    public static boolean prepareAttic(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return false;
        var realm=RealmData.get(p.server).realms.get(s.door);var level=p.server.getLevel(OtherWorld.KEY);if(realm==null||realm.house==null||level==null)return false;var n=MouseCircusVisit.data(s);BlockPos at=tent(p,s);
        if(n.getInt("TentPlacementVersion")<2){
            // Only remove the known authored concrete tent, never arbitrary furniture/roof blocks.
            for(int y=24;y<=30;y++)for(int x=29;x<=35;x++)for(int z=17;z<=23;z++){
                boolean wall=y<=26&&(x==29||x==35||z==17||z==23)&&!(z==23&&x>=31&&x<=33&&y<=25);
                int radius=30-y;boolean roof=y>=27&&Math.abs(x-32)<=radius&&Math.abs(z-20)<=radius;
                if(!wall&&!roof)continue;var cell=HouseCabinets.at(realm.house,x,y,z);var expected=wall?((x-29)/2%2==0?Blocks.RED_CONCRETE:Blocks.WHITE_CONCRETE):((x+z)%2!=0?Blocks.RED_CONCRETE:Blocks.WHITE_CONCRETE);
                if(level.getBlockState(cell).is(expected)&&level.getBlockEntity(cell)==null)ActivityBlocks.set(level,cell,Blocks.AIR.defaultBlockState(),2);
            }
            var target=HouseCabinets.at(realm.house,32,24,20);
            for(var cell:BlockPos.betweenClosed(target.offset(-2,0,-2),target.offset(2,3,2)))if(!level.getBlockState(cell).isAir()&&!level.getBlockState(cell).is(Blocks.RED_CARPET)){n.putString("Blocker","The far-end tent footprint is obstructed; preserving unrelated blocks.");return false;}
            if(!level.getBlockState(target.below()).isFaceSturdy(level,target.below(),Direction.UP))return false;
            for(var prop:level.getEntitiesOfClass(CinematicPropEntity.class,new AABB(target).inflate(70),e->e.owned(s)&&(e.kind()==CinematicPropEntity.TENT||e.kind()==CinematicPropEntity.FERRIS||e.kind()==CinematicPropEntity.CANNON)))prop.discard();
            at=target;n.putLong("Tent",at.asLong());n.putInt("TentPlacementVersion",2);OtherMotherLoop.changed(p);
        }
        TheatreSite.ensure(p,s,level,CinematicPropEntity.TENT,at,0);TheatreSite.ensure(p,s,level,CinematicPropEntity.FERRIS,at.west(3),0);TheatreSite.ensure(p,s,level,CinematicPropEntity.CANNON,at.east(3),0);n.remove("Blocker");return true;
    }
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return false;
        var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return false;var c=center(p,s);var data=ShowSiteData.get(p.server);var n=data.owner(s.owner);
        if(!n.getBoolean("CircusBuilt")){
            if(!VenueBuildRecovery.begin(p,l,data,n,"Circus",
                    c.offset(-41,-1,-41),c.offset(41,44,41),
                    Set.of(Blocks.SMOOTH_SANDSTONE,Blocks.CUT_RED_SANDSTONE,
                            Blocks.RED_TERRACOTTA,Blocks.WHITE_TERRACOTTA,Blocks.DARK_OAK_PLANKS,
                            Blocks.RED_WOOL,Blocks.WHITE_WOOL,Blocks.DARK_OAK_FENCE,
                            Blocks.CHISELED_RED_SANDSTONE,Blocks.SHROOMLIGHT,
                            Blocks.REDSTONE_LAMP)))return false;
            for(int x=-41;x<=41;x++)for(int z=-41;z<=41;z++){double r=Math.hypot(x,z);if(r>41)continue;double angle=Math.atan2(z,x);put(l,c.offset(x,-1,z),r<19?Blocks.SMOOTH_SANDSTONE:r>38?Blocks.CUT_RED_SANDSTONE:Math.abs(x)<3&&z>18?Blocks.RED_TERRACOTTA:Blocks.DARK_OAK_PLANKS);
                if(r>39)for(int y=0;y<17;y++)if(!(z>38&&Math.abs(x)<3&&y<5))put(l,c.offset(x,y,z),(int)Math.floor((angle+Math.PI)*8/Math.PI)%2==0?Blocks.RED_TERRACOTTA:Blocks.WHITE_TERRACOTTA);
                int roof=17+(int)((41-r)*.64);put(l,c.offset(x,roof,z),(int)Math.floor((angle+Math.PI)*8/Math.PI)%2==0?Blocks.RED_WOOL:Blocks.WHITE_WOOL);
                if(r>18&&r<19.5&&! (z>0&&Math.abs(x)<3))put(l,c.offset(x,0,z),Blocks.DARK_OAK_FENCE);
            }
            for(int i=0;i<16;i++){double a=i*Math.PI/8;var column=c.offset((int)(Math.cos(a)*35),0,(int)(Math.sin(a)*35));for(int y=0;y<26;y++)put(l,column.above(y),y%6==0?Blocks.CHISELED_RED_SANDSTONE:Blocks.CUT_RED_SANDSTONE);put(l,column.above(26),Blocks.SHROOMLIGHT);}
            for(int i=0;i<24;i++){double a=i*Math.PI/12;put(l,c.offset((int)(Math.cos(a)*20),0,(int)(Math.sin(a)*20)),Blocks.REDSTONE_LAMP);}VenueBuildRecovery.complete(data,n,"Circus");p.server.overworld().getDataStorage().save();
        }
        ShowStageLease.hold(p,s,true);
        // The rear wall is a real wall. Exit is the carpet trigger inside it, not a tiny tent/void hole.
        if(!n.getBoolean("CircusRearSealed")){for(int x=-2;x<=2;x++)for(int z=40;z<=41;z++)if(Math.hypot(x,z)<=41)for(int y=0;y<5;y++)if(l.getBlockState(c.offset(x,y,z)).isAir())put(l,c.offset(x,y,z),Blocks.RED_TERRACOTTA);n.putBoolean("CircusRearSealed",true);data.setDirty();}
        for(var prop:l.getEntitiesOfClass(CinematicPropEntity.class,new AABB(exit(p,s)).inflate(5),e->e.owned(s)&&e.kind()==CinematicPropEntity.TENT&&e.slot()==99))prop.discard();
        TheatreSite.ensure(p,s,l,CinematicPropEntity.SEAT,audience(p,s),0);TheatreSite.ensure(p,s,l,CinematicPropEntity.SEAT,audience(p,s).west(2),1);return true;
    }
    public static boolean enter(ServerPlayer p,VisitData.Scene s){var at=tent(p,s);if(!inAttic(p,s)||at==null||p.distanceToSqr(at.getCenter())>16||!prepare(p,s))return false;return ShowTravel.enter(p,s,"CircusReturn",audience(p,s));}
    public static boolean returnToAttic(ServerPlayer p,VisitData.Scene s){return ShowTravel.exit(p,s,"CircusReturn")&&inAttic(p,s);}
    public static Mob enterGuest(Mob m,ServerPlayer p,VisitData.Scene s){return ShowTravel.guest(m,p,s,p.server.getLevel(ActivityWorld.KEY),Vec3.atBottomCenterOf(audience(p,s).west(2)),true,"CircusGuestReturn");}
    public static Mob exitGuest(Mob m,ServerPlayer p,VisitData.Scene s){return ShowTravel.returnGuest(m,p,s,"CircusGuestReturn");}
    public static boolean release(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID())||inside(p,s)||!ShowTravel.returnAll(p,s,"CircusGuestReturn"))return false;for(var l:p.server.getAllLevels())for(var prop:l.getEntitiesOfClass(CinematicPropEntity.class,new AABB(l==p.server.getLevel(ActivityWorld.KEY)?center(p,s):Objects.requireNonNullElse(tent(p,s),p.blockPosition())).inflate(70),e->e.owned(s))){prop.ejectPassengers();prop.discard();}ShowStageLease.release(p,s,true);return true;}
    private static void put(ServerLevel level,BlockPos at,Block block){ActivityBlocks.set(level,at,block.defaultBlockState(),2);}
    private MouseCircusSite(){}
}
