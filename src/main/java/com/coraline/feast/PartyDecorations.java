package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.secrets.HouseCabinets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Shared lightweight party set dressing; uses existing persistent props and never edits copied house blocks. */
public final class PartyDecorations {
    public static final int GARLAND=0,BOUQUET=1,BALLOONS=2,ARCH=3,TABLECLOTH=4,LANTERNS=5,CIRCUS_STAGE=6;
    private static UUID token(VisitData.Scene s){return UUID.nameUUIDFromBytes((s.owner+":"+s.door+":"+s.loop.event.getInt("Serial")+":party-decoration").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static boolean own(ScenePropEntity e,VisitData.Scene s){return e.getPersistentData().hasUUID("PartyDecoration")&&token(s).equals(e.getPersistentData().getUUID("PartyDecoration"));}
    public static ScenePropEntity furniture(ServerPlayer p,VisitData.Scene s,int kind,Vec3 at,int slot){var old=VisitEvents.props(p,s).stream().filter(e->e.slot==slot&&own(e,s)).findFirst().orElse(null);if(old!=null)return old;var e=VisitEvents.prop(p,s,kind,at,slot);e.getPersistentData().putUUID("PartyDecoration",token(s));return e;}
    public static ScenePropEntity decorate(ServerPlayer p,VisitData.Scene s,int slot,Vec3 at,int style,float width){
        var existing=VisitEvents.props(p,s).stream().filter(e->e.slot==slot&&own(e,s)).findFirst().orElse(null);if(existing!=null)return existing;
        var prop=VisitEvents.prop(p,s,ScenePropEntity.OUTING_SCENERY,at,slot);prop.getPersistentData().putUUID("PartyDecoration",token(s));
        var data=new CompoundTag();data.putBoolean("PartyDecoration",true);data.putInt("Style",style);data.putFloat("Width",Math.max(.5F,Math.min(9,width)));data.putInt("Palette",Math.floorMod(slot,3));prop.cakeData(data);return prop;
    }
    public static boolean prepare(ServerPlayer p,VisitData.Scene s,boolean wholeHouse){
        var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null)return false;int slot=5000;
        for(var room:PartyRegions.rooms()){
            var area=room.bounds(realm.house);double y=Math.min(area.maxY-1,room.center(realm.house).getY()+2.7);float width=(float)Math.min(8,area.maxX-area.minX-1.5);
            if(width>=2){var at=new Vec3((area.minX+area.maxX)/2,y,area.minZ+.65);if(clear(p,new AABB(at.x-width/2,at.y-.6,at.z-.1,at.x+width/2,at.y+.15,at.z+.1)))decorate(p,s,slot,at,GARLAND,width);}slot++;
            // Room-corner flowers/balloons do not occupy doors, stairs or narrow halls.
            if(area.maxX-area.minX>6&&area.maxZ-area.minZ>6){var at=room.center(realm.house).offset((int)(area.maxX-area.minX)/2-2,0,0);if(OutingSites.feet(p.serverLevel(),at)&&clear(p,new AABB(at).inflate(.35,0,.35).expandTowards(0,2,0)))decorate(p,s,slot,Vec3.atBottomCenterOf(at),BALLOONS,1);}slot++;
        }
        if(wholeHouse)for(var room:HouseCabinets.ROOMS)if(room.id().equals("attic")||room.id().equals("basement")){
            var at=HouseCabinets.at(realm.house,room.min()[0]+3,room.min()[1]+3,room.min()[2]+2);if(clear(p,new AABB(at).inflate(2,.6,.2)))decorate(p,s,slot,Vec3.atBottomCenterOf(at),GARLAND,4);slot++;
        }
        decorateTables(p,s);return true;
    }
    public static void decorateTables(ServerPlayer p,VisitData.Scene s){
        int index=0;for(var table:VisitEvents.props(p,s))if(table.kind()==ScenePropEntity.TABLE){
            var prop=VisitEvents.props(p,s).stream().filter(e->own(e,s)&&e.getPersistentData().hasUUID("ClothTable")&&e.getPersistentData().getUUID("ClothTable").equals(table.getUUID())).findFirst().orElse(null);
            if(prop==null){Set<Integer> used=new HashSet<>();for(var e:VisitEvents.props(p,s))if(own(e,s))used.add(e.slot);while(used.contains(5300+index))index++;prop=decorate(p,s,5300+index++,table.position().add(0,.86,0),TABLECLOTH,table.longDining()?8.8F:3.5F);prop.getPersistentData().putUUID("ClothTable",table.getUUID());}
            var data=prop.cakeData().copy();data.putBoolean("LongTable",table.longDining());data.putFloat("TableDepth",table.longDining()?9F*Math.max(1,table.targetScale()):3.5F);prop.cakeData(data);
        }
    }
    public static void garden(ServerPlayer p,VisitData.Scene s,Vec3 center){
        var arch=center.add(0,0,-5);if(clear(p,new AABB(arch.x-3.6,arch.y,arch.z-.3,arch.x+3.6,arch.y+3.4,arch.z+.3)))decorate(p,s,5400,arch,ARCH,7);
        for(int i=0;i<4;i++){double angle=i*Math.PI/2+.4;var at=center.add(Math.cos(angle)*7,0,Math.sin(angle)*7);if(clear(p,new AABB(at.x-.45,at.y,at.z-.45,at.x+.45,at.y+3.5,at.z+.45)))decorate(p,s,5401+i,at,BALLOONS,1);}
        if(clear(p,new AABB(center.x-4,center.y+2.3,center.z-.2,center.x+4.7,center.y+3.3,center.z+.2)))decorate(p,s,5410,center.add(0,3.2,0),LANTERNS,8);
    }
    /** At least one usable station is required; never place food beyond verified public geometry. */
    public static boolean refreshments(ServerPlayer p,VisitData.Scene s,Vec3 center){
        var n=HouseParty.data(s);var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null)return false;
        var areas=new ArrayList<AABB>();for(var room:PartyRegions.rooms())if(room.floor()<10&&!room.id().contains("stair")&&!room.id().contains("hall"))areas.add(room.bounds(realm.house));
        for(var area:PartyGarden.grounds(p,s))areas.add(area.bounds());
        var options=new ArrayList<Vec3>();options.add(center.add(-10,0,-3));options.add(center.add(10,0,-3));
        for(var room:PartyRegions.rooms())if(room.floor()<10&&!room.id().contains("stair")&&!room.id().contains("hall")){var at=Vec3.atBottomCenterOf(room.center(realm.house));options.add(at.add(-2,0,0));options.add(at.add(2,0,0));options.add(at);}
        int count=0;var used=new ArrayList<Vec3>();
        for(int i=0;i<2;i++){
            int slot=5450+i*10;var table=VisitEvents.props(p,s).stream().filter(e->e.slot==slot&&own(e,s)).findFirst().orElse(null);
            Vec3 at=table==null?null:table.position();
            if(at==null)for(var point:options){if(point.distanceToSqr(center)<81||used.stream().anyMatch(v->v.distanceToSqr(point)<36)||!supported(p,areas,point)||!clear(p,new AABB(point.x-1.9,point.y,point.z-1.9,point.x+1.9,point.y+2,point.z+1.9)))continue;at=point;break;}
            if(at==null)continue;used.add(at);count++;if(table==null)table=furniture(p,s,ScenePropEntity.TABLE,at,slot);
            final int plateSlot=slot+1;if(VisitEvents.props(p,s).stream().noneMatch(e->e.slot==plateSlot&&own(e,s))){var plate=furniture(p,s,ScenePropEntity.PLATE,at.add(0,.9,0),plateSlot);plate.getPersistentData().putBoolean("PartyRefreshment",true);plate.getPersistentData().putInt("Servings",12);plate.food(0,new ItemStack(i==0?Items.COOKIE:Items.BREAD,3));plate.food(1,new ItemStack(Items.APPLE,2));}
        }decorateTables(p,s);return count>0;
    }
    private static boolean supported(ServerPlayer p,List<AABB> areas,Vec3 center){for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var point=center.add(x,0,z);if(areas.stream().noneMatch(a->a.contains(point))||!OutingSites.feet(p.serverLevel(),BlockPos.containing(point)))return false;}return true;}
    public static boolean interact(ServerPlayer p,ScenePropEntity prop){
        var s=HouseParty.active(p);if(s==null||!own(prop,s)||!p.getUUID().equals(prop.owner))return false;
        if(prop.getPersistentData().getBoolean("PartyRefreshment")&&prop.distanceToSqr(p)<16&&p.hasLineOfSight(prop)&&!s.loop.secrets.searching()&&p.canEat(false)){
            int left=prop.getPersistentData().getInt("Servings");long clock=HouseParty.data(s).getLong("Clock");if(left>0&&clock>=HouseParty.data(s).getLong("SnackAfter")){prop.getPersistentData().putInt("Servings",left-1);HouseParty.data(s).putLong("SnackAfter",clock+30);p.getFoodData().eat(3,.3F);if(left==1){prop.food(0,ItemStack.EMPTY);prop.food(1,ItemStack.EMPTY);}OtherMotherLoop.changed(p);}
        }return true;
    }
    private static boolean clear(ServerPlayer p,AABB box){return p.serverLevel().noCollision(box);}
    public static void cleanup(ServerPlayer p,VisitData.Scene s){for(var level:p.server.getAllLevels())for(var e:level.getEntitiesOfClass(ScenePropEntity.class,new AABB(s.table).inflate(350),prop->own(prop,s)&&s.owner.equals(prop.owner)&&s.door.equals(prop.storyDoor))){e.ejectPassengers();e.discard();}}
    private PartyDecorations(){}
}
