package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Temporary collision-checked tables with real seats and rotating dining groups. */
public final class PartyBanquet {
    private static final int FIRST=3000,LIMIT=3500;
    private static ScenePropEntity prop(ServerPlayer p,VisitData.Scene s,int slot){return VisitEvents.props(p,s).stream().filter(e->e.slot==slot&&e.getPersistentData().getBoolean("PartyBanquet")).findFirst().orElse(null);}
    private static ScenePropEntity create(ServerPlayer p,VisitData.Scene s,int kind,Vec3 at,int slot){var e=VisitEvents.prop(p,s,kind,at,slot);e.getPersistentData().putBoolean("PartyBanquet",true);return e;}
    private static Vec3 seat(Vec3 center,int side){return center.add(side==2?-2.35:side==3?2.35:0,0,side==0?-2.35:side==1?2.35:0);}
    private static float yaw(int side){return side==0?0:side==1?180:side==2?-90:90;}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);if(n.getBoolean("BanquetReady"))return true;
        var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return false;
        if(!PlayerParty.isPlayerParty(s))return prepareLongTable(p,s,realm.house);
        int guests=(int)PartyGuests.guests(s).stream().filter(g->PartyGuests.entity(p,g)!=null&&!g.getString("State").equals("LEAVING")).count();int wanted=Math.max(1,(guests+6)/4);
        var centers=new ArrayList<BlockPos>();
        for(var room:PartyRegions.rooms()){
            if(room.floor()!=5)continue;
            for(int x=room.min()[0]+3;x<=room.max()[0]-3&&centers.size()<wanted;x+=3)for(int z=room.min()[2]+3;z<=room.max()[2]-3&&centers.size()<wanted;z+=3){
                var at=HouseCabinets.at(realm.house,x,room.floor()+1,z);var center=Vec3.atBottomCenterOf(at);
                if(centers.stream().anyMatch(other->other.distSqr(at)<64)||!clear(p,center))continue;centers.add(at);
            }
        }
        if(centers.isEmpty())return false;var saved=new ListTag();int index=0;
        for(var at:centers){var tableTag=new CompoundTag();tableTag.putLong("Position",at.asLong());saved.add(tableTag);buildTable(p,s,at,index++);}
        n.put("BanquetTables",saved);n.putInt("BanquetSeats",centers.size()*4);n.putBoolean("BanquetReady",true);assign(p,s,0);PartyDecorations.decorateTables(p,s);return true;
    }
    private static boolean prepareLongTable(ServerPlayer p,VisitData.Scene s,BlockPos house){
        var n=HouseParty.data(s);var center=PartyFloor.rectangle(p,HouseCabinets.at(house,43,6,36),2,12);if(center==null){n.putString("BanquetBlocker","The supper-room floor or table space is obstructed.");return false;}
        int count=(int)PartyGuests.guests(s).stream().filter(g->!Set.of("GONE","LEAVING").contains(g.getString("State"))).count()+3;
        count=Math.max(6,count);double depth=12;
        if(count>46||!PartyFloor.clear(p,new AABB(center.x-1.8,center.y+.02,center.z-depth,center.x+1.8,center.y+.9,center.z+depth)))return false;
        for(int i=0;i<count;i++){var at=longSeat(center,depth,count,i);if(!HouseParty.insideHouse(p,at)||PartyFloor.at(p,BlockPos.containing(at))==null||!PartyFloor.clear(p,new AABB(at.x-.43,at.y+.01,at.z-.43,at.x+.43,at.y+2.1,at.z+.43))){n.putString("BanquetBlocker","A supper-room chair position is obstructed.");return false;}}
        // Only our own temporary party furniture may be replaced. Never edit the house blocks.
        for(var e:VisitEvents.props(p,s))if(!e.getPersistentData().getBoolean("PartyGameStation")&&(e.kind()==ScenePropEntity.TABLE||e.kind()==ScenePropEntity.CHAIR||e.kind()==ScenePropEntity.PLATE||e.kind()==ScenePropEntity.GLASS)){e.ejectPassengers();e.discard();}
        n.putBoolean("LongBanquet",true);n.putDouble("BanquetDepth",depth);n.putInt("BanquetSeats",count);
        n.putDouble("BanquetFloor",center.y);n.remove("BanquetBlocker");
        var saved=new ListTag();var entry=new CompoundTag();entry.putLong("Position",BlockPos.containing(center).asLong());saved.add(entry);n.put("BanquetTables",saved);
        buildLongTable(p,s,center,count,depth);n.putBoolean("BanquetReady",true);assign(p,s,0);PartyDecorations.decorateTables(p,s);return true;
    }
    static Vec3 longSeat(Vec3 center,double depth,int count,int index){
        if(index<2)return center.add(0,0,index==0?-depth-.8:depth+.8);
        int rows=(count-2+1)/2,row=(index-2)/2;double z=rows<=1?0:-(depth-.7)+row*(2*(depth-.7)/(rows-1));
        return center.add(index%2==0?-2.45:2.45,0,z);
    }
    private static void buildLongTable(ServerPlayer p,VisitData.Scene s,Vec3 center,int count,double depth){
        if(prop(p,s,FIRST)==null){var table=create(p,s,ScenePropEntity.TABLE,center,FIRST);table.longDining(true);table.targetScale((float)(depth/4.5));}
        for(int i=0;i<count;i++){
            var at=longSeat(center,depth,count,i);if(prop(p,s,chairSlot(i))==null){var chair=create(p,s,ScenePropEntity.CHAIR,at,chairSlot(i));chair.setYRot(i<2?yaw(i):i%2==0?-90:90);}
            var place=i<2?at.add(0,.9,i==0?.7:-.7):at.add(i%2==0?.8:-.8,.9,0);
            if(prop(p,s,plateSlot(i))==null){var plate=create(p,s,ScenePropEntity.PLATE,place,plateSlot(i));for(int f=0;f<3;f++)plate.food(f,FeastContent.serving(p.getRandom(),i%4));}
        }
        for(int i=0;i<3;i++)if(prop(p,s,FIRST+13+i)==null){var plate=create(p,s,ScenePropEntity.PLATE,center.add(0,.9,(i-1)*6),FIRST+13+i);plate.getPersistentData().putBoolean("PartyServing",true);for(int f=0;f<3;f++)plate.food(f,FeastContent.serving(p.getRandom(),i));}
    }
    static ScenePropEntity playerChair(ServerPlayer p,VisitData.Scene s){return prop(p,s,chairSlot(0));}
    static Vec3 celebrationCake(ServerPlayer p,VisitData.Scene s){var chair=playerChair(p,s);return chair==null?Vec3.atBottomCenterOf(s.table).add(0,.92,0):chair.position().add(0,.92,1.25);}
    private static boolean clear(ServerPlayer p,Vec3 center){
        if(!PartyRegions.allowed(p,center)||!p.serverLevel().noCollision(new AABB(center.x-1.85,center.y+.01,center.z-1.85,center.x+1.85,center.y+.95,center.z+1.85)))return false;
        for(int side=0;side<4;side++){var point=seat(center,side);var at=BlockPos.containing(point);if(!PartyRegions.allowed(p,point)||!OutingSites.feet(p.serverLevel(),at)||!p.serverLevel().noCollision(new AABB(point.x-.5,point.y+.01,point.z-.5,point.x+.5,point.y+2.5,point.z+.5)))return false;}
        return true;
    }
    private static void buildTable(ServerPlayer p,VisitData.Scene s,BlockPos at,int index){
        var n=HouseParty.data(s);if(n.getBoolean("LongBanquet")){buildLongTable(p,s,new Vec3(at.getX()+.5,n.contains("BanquetFloor")?n.getDouble("BanquetFloor"):at.getY()+.126,at.getZ()+.5),n.getInt("BanquetSeats"),n.getDouble("BanquetDepth"));return;}
        int base=FIRST+index*40;Vec3 center=Vec3.atBottomCenterOf(at);if(prop(p,s,base)==null)create(p,s,ScenePropEntity.TABLE,center,base);
        for(int side=0;side<4;side++){
            var chair=prop(p,s,base+1+side);if(chair==null){chair=create(p,s,ScenePropEntity.CHAIR,seat(center,side),base+1+side);chair.setYRot(yaw(side));}
            var point=seat(center,side).subtract(center).scale(.54).add(center).add(0,.9,0);
            if(prop(p,s,base+5+side)==null){var plate=create(p,s,ScenePropEntity.PLATE,point,base+5+side);for(int food=0;food<3;food++)plate.food(food,FeastContent.serving(p.getRandom(),Math.floorMod(index+side,4)));}
            if(prop(p,s,base+9+side)==null)create(p,s,ScenePropEntity.GLASS,point.add(.32,.01,.18),base+9+side).food(0,FeastContent.random(p.getRandom(),true));
        }
        for(int dish=0;dish<3;dish++)if(prop(p,s,base+13+dish)==null){var plate=create(p,s,ScenePropEntity.PLATE,center.add((dish-1)*.65,.91,0),base+13+dish);plate.getPersistentData().putBoolean("PartyServing",true);for(int f=0;f<3;f++)plate.food(f,FeastContent.serving(p.getRandom(),Math.floorMod(index+dish,4)));}
        if(prop(p,s,base+16)==null)create(p,s,ScenePropEntity.CAKE,center.add(.45,.92,.6),base+16).food(0,new ItemStack(Items.CAKE));
    }
    private static int chairSlot(int seat){return FIRST+(seat/4)*40+1+seat%4;}
    private static int plateSlot(int seat){return FIRST+(seat/4)*40+5+seat%4;}
    private static void assign(ServerPlayer p,VisitData.Scene s,int wave){
        var n=HouseParty.data(s);int seats=n.getInt("BanquetSeats"),guestSeats=Math.max(1,seats-3);var list=PartyGuests.guests(s).stream().filter(g->PartyGuests.entity(p,g)!=null&&!Set.of("LEAVING","GONE","QUEUED").contains(g.getString("State"))).toList();
        for(int i=0;i<list.size();i++){var g=list.get(i);var m=PartyGuests.entity(p,g);m.stopRiding();finishBite(m);
            if(n.getBoolean("LongBanquet")||i/guestSeats==wave){g.putInt("FeastSeat",3+(n.getBoolean("LongBanquet")?i:i%guestSeats));g.putString("State","MOVING_TO_FEAST");g.putLong("NextBite",n.getLong("Clock")+100+p.getRandom().nextInt(241));VisitAudio.chatter(m,false);}
            else {g.remove("FeastSeat");g.putString("State","WANDERING");}
        }n.putInt("FeastWave",wave);
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);if(p.tickCount%20!=0)return;
        var tables=n.getList("BanquetTables",Tag.TAG_COMPOUND);for(int i=0;i<tables.size();i++)buildTable(p,s,BlockPos.of(tables.getCompound(i).getLong("Position")),i);
        int count=(int)PartyGuests.guests(s).stream().filter(g->PartyGuests.entity(p,g)!=null).count();int waves=Math.max(1,(count+Math.max(1,n.getInt("BanquetSeats")-3)-1)/Math.max(1,n.getInt("BanquetSeats")-3));int wave=Math.min(waves-1,n.getInt("FeastElapsed")/Math.max(1,n.getInt("FeastDuration")/waves));
        if(wave!=n.getInt("FeastWave"))assign(p,s,wave);
        if(!n.getBoolean("DessertServed")&&n.getInt("FeastElapsed")>=n.getInt("FeastDuration")*2/3){n.putBoolean("DessertServed",true);for(var e:VisitEvents.props(p,s))if(e.getPersistentData().getBoolean("PartyBanquet")&&e.kind()==ScenePropEntity.PLATE){e.food(0,new ItemStack(Items.COOKIE,3));e.food(1,new ItemStack(Items.PUMPKIN_PIE));e.food(2,new ItemStack(Items.SWEET_BERRIES,2));}}
    }
    public static void control(Mob m,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        finishBiteIfDue(m,p);if(!g.contains("FeastSeat")){g.putString("State","WANDERING");return;}int seat=g.getInt("FeastSeat");var chair=prop(p,s,chairSlot(seat));if(chair==null)return;
        if(m.getVehicle()!=chair){m.setMaxUpStep(1.05F);if(m.distanceToSqr(chair)<2.25&&!chair.isVehicle()&&m.hasLineOfSight(chair))m.startRiding(chair,true);else if(m.tickCount%40==0)m.getNavigation().moveTo(chair,1);return;}
        g.putString("State","EATING");m.getNavigation().stop();m.setYRot(chair.getYRot());m.setYBodyRot(chair.getYRot());
        long now=HouseParty.data(s).getLong("Clock");if(now>=g.getLong("NextBite")){var plate=prop(p,s,plateSlot(seat));if(plate!=null)bite(p,plate,m);g.putLong("NextBite",now+160+p.getRandom().nextInt(301));}
        if(m.tickCount%100==Math.floorMod(g.getInt("Order")*7,100))PartyGuests.gesture(m,2,35);
    }
    public static boolean seatHost(ParentEntity parent,ServerPlayer p,VisitData.Scene s){
        int seat=parent.isMom()?1:2;var chair=prop(p,s,chairSlot(seat));if(chair==null)return false;FamilyDinner.finishBiteIfDone(parent,p);SupperSeating.approach(parent,chair);parent.lookAtStoryPlayer(p);
        if(parent.getVehicle()==chair&&parent.tickCount%260==(parent.isMom()?0:80)){var plate=prop(p,s,plateSlot(seat));if(plate!=null)FamilyDinner.bite(p,plate,parent);}return true;
    }
    private static void bite(ServerPlayer p,ScenePropEntity plate,Mob m){
        for(int i=2;i>=0;i--)if(!plate.food(i).isEmpty()){
            finishBite(m);var old=m.getMainHandItem();m.getPersistentData().put("PartyBiteHand",old.save(new CompoundTag()));m.getPersistentData().putLong("PartyBiteUntil",HouseParty.data(OtherMotherLoop.scene(p)).getLong("Clock")+32);
            var food=plate.food(i).copy();m.setItemSlot(EquipmentSlot.MAINHAND,food.copyWithCount(1));food.shrink(1);plate.food(i,food);m.startUsingItem(InteractionHand.MAIN_HAND);PartyGuests.gesture(m,6,32);m.swing(InteractionHand.MAIN_HAND);return;
        }
    }
    private static void finishBiteIfDue(Mob m,ServerPlayer p){if(m.getPersistentData().contains("PartyBiteHand")&&HouseParty.data(OtherMotherLoop.scene(p)).getLong("Clock")>=m.getPersistentData().getLong("PartyBiteUntil"))finishBite(m);}
    static void finishBite(Mob m){if(!m.getPersistentData().contains("PartyBiteHand"))return;m.stopUsingItem();m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.of(m.getPersistentData().getCompound("PartyBiteHand")));m.getPersistentData().remove("PartyBiteHand");m.getPersistentData().remove("PartyBiteUntil");}
    public static boolean interact(ServerPlayer p,ScenePropEntity e){
        if(PartyCelebration.cake(p,e))return true;
        if(!e.getPersistentData().getBoolean("PartyBanquet"))return false;var s=HouseParty.active(p);if(s==null||!p.getUUID().equals(e.owner)||e.getPersistentData().getInt("VisitEventSerial")!=s.loop.event.getInt("Serial")||e.distanceToSqr(p)>16||s.loop.secrets.searching()||SecretDialogue.busy(p))return true;
        if(e.kind()==ScenePropEntity.CHAIR){if(e.slot==chairSlot(0)&&!e.isVehicle())p.startRiding(e,true);return true;}
        if(s.loop.phase!=OtherMotherLoopState.Phase.PARTY_FEAST||e.kind()!=ScenePropEntity.PLATE)return true;
        var n=HouseParty.data(s);long now=n.getLong("Clock");if(now<n.getLong("FoodClickAfter"))return true;
        n.putLong("FoodClickAfter",now+20);
        if(!e.getPersistentData().getBoolean("PartyServing")){if(e.slot==plateSlot(0)&&p.getFoodData().needsFood())FamilyDinner.bite(p,e,p);return true;}
        int source=-1;for(int i=0;i<3;i++)if(!e.food(i).isEmpty()){source=i;break;}if(source<0)return true;
        var candidate=e.food(source);boolean room=false;for(int i=0;i<36;i++){var existing=p.getInventory().getItem(i);if(existing.isEmpty()||ItemStack.isSameItemSameTags(existing,candidate)&&existing.getCount()<existing.getMaxStackSize()){room=true;break;}}if(!room)return true;
        var mother=MotherSearch.mother(p);boolean observed=mother!=null&&PartySurveillance.sees(mother,p,12);
        var notice=HousePartyLedger.foodTaking(n,now,observed);
        if(notice==HousePartyLedger.FoodNotice.WARN)NumberedEventDialogue.mother(p,"house_party","food");
        if(notice==HousePartyLedger.FoodNotice.BLOCK)return true;
        for(int i=0;i<3;i++)if(!e.food(i).isEmpty()){
            var serving=e.food(i).copyWithCount(1);var remainder=serving.copy();PartyEscrow.insert(p,remainder);if(!remainder.isEmpty())return true;
            var food=e.food(i).copy();food.shrink(1);e.food(i,food);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();return true;
        }return true;
    }
    public static void cleanup(ServerPlayer p,VisitData.Scene s){for(var g:PartyGuests.guests(s)){var m=PartyGuests.entity(p,g);if(m!=null)finishBite(m);}for(var e:VisitEvents.props(p,s))if(e.slot>=FIRST&&e.slot<LIMIT&&e.getPersistentData().getBoolean("PartyBanquet")){e.ejectPassengers();e.discard();}}
    private PartyBanquet(){}
}
