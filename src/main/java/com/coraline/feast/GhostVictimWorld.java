package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.registry.ModBlocks;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Reuses the actual owner house, furniture and Little Door. The client reverses
 * deterioration gradually; this class never replaces the estate with a copy. */
public final class GhostVictimWorld {
    public static BlockPos at(VisitData.Scene s,int x,int y,int z){return HouseCabinets.at(AcceptedButtonEyesWorld.house(s),x,y,z);}
    public static BlockPos hall(VisitData.Scene s){return at(s,34,6,44);}
    public static BlockPos supper(VisitData.Scene s){return at(s,44,6,37);}
    public static BlockPos kitchen(VisitData.Scene s){return at(s,47,6,47);}
    public static BlockPos bedroom(VisitData.Scene s){return at(s,44,15,23);}
    /** A walkable shared position where the child is close enough to the
     * bedroom mirror for its real conversation validator. */
    static BlockPos bedtimeConversationPosition(VisitData.Scene s,ServerLevel level,net.minecraft.world.entity.PathfinderMob child,CompoundTag n){
        var mirror=n.getCompound("Mirrors").getCompound("bedroom");if(!mirror.contains("Position",Tag.TAG_LONG))return null;
        var stored=GhostVictimNavigationGoal(n);if(stored!=null&&GhostVictimNavigation.clear(level,child,stored)){
            var eye=Vec3.atBottomCenterOf(stored).add(0,child.getEyeHeight(),0);
            if(eye.distanceTo(GhostVictimMirrors.surface(mirror))<4.95)return stored;
        }
        var anchor=BlockPos.of(mirror.getLong("Position"));BlockPos best=null;double bestDistance=Double.MAX_VALUE;
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++){
            var candidate=anchor.offset(dx,0,dz);if(OtherWorldEstate.outsideHouse(AcceptedButtonEyesWorld.house(s),candidate))continue;
            if(!level.hasChunkAt(candidate)||!GhostVictimNavigation.clear(level,child,candidate))continue;
            var eye=Vec3.atBottomCenterOf(candidate).add(0,child.getEyeHeight(),0);double distance=eye.distanceTo(GhostVictimMirrors.surface(mirror));if(distance>=4.95)continue;
            var path=child.getNavigation().createPath(candidate,0);if(path==null||!path.canReach())continue;
            double score=distance+eye.distanceToSqr(child.getEyePosition())*.01;if(score<bestDistance){best=candidate;bestDistance=score;}
        }
        if(best!=null)n.putLong("BedtimeConversationTarget",best.asLong());return best;
    }
    private static BlockPos GhostVictimNavigationGoal(CompoundTag n){return n.contains("BedtimeConversationTarget",Tag.TAG_LONG)?BlockPos.of(n.getLong("BedtimeConversationTarget")):null;}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!GhostVictimMirrors.owned(p,s,n))return false;
        var level=AcceptedButtonEyesWorld.level(p);var realm=AcceptedButtonEyesWorld.realm(p,s);
        if(level==null||realm==null||realm.house==null||realm.doorPos==null)return false;
        level.getChunkAt(realm.doorPos);
        var accepted=AcceptedButtonEyes.data(s);var old=level.getBlockState(realm.doorPos);
        if(!old.is(ModBlocks.TINY_DOOR.get())){n.putString("Blocker","The owner's Little Door is missing.");return false;}
        var open=old.setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true);
        level.setBlock(realm.doorPos,open,3);
        if(level.getBlockEntity(realm.doorPos) instanceof DoorBlockEntity door){
            door.storyBricked=false;door.setChanged();level.sendBlockUpdated(realm.doorPos,open,open,3);
            var backing=realm.doorPos.relative(open.getValue(TinyDoorBlock.FACING).getOpposite());
            if(level.getBlockState(backing).is(Blocks.BRICKS))FirstEscapeScene.place(p,s,backing,Blocks.AIR.defaultBlockState());
            TunnelPassage.ensure(level,door);
        }
        // Reverse only the captured decorative bed geometry. Each cell has one
        // admission; an obstructed partner must never replay another cell's
        // saved block entity or recreate a bed subsequently removed by a player.
        if(!n.getBoolean("ChildBedRestored")){
            var saved=accepted.getList("OriginalBed",Tag.TAG_COMPOUND);
            var handled=n.getCompound("ChildBedHandled");boolean complete=!saved.isEmpty();
            for(Tag entry:saved){var cell=(CompoundTag)entry;
                if(!cell.contains("Position",Tag.TAG_LONG)||!cell.contains("State",Tag.TAG_COMPOUND)){complete=false;continue;}
                var pos=BlockPos.of(cell.getLong("Position"));
                var state=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),cell.getCompound("State"));
                if(!(state.getBlock() instanceof net.minecraft.world.level.block.BedBlock)||realm.bed==null||pos.distSqr(realm.bed)>1){complete=false;continue;}
                String key=Long.toString(pos.asLong());
                if(handled.contains(key,Tag.TAG_BYTE))continue;
                if(!level.hasChunkAt(pos)){complete=false;continue;}
                var current=level.getBlockState(pos);
                if(current.isAir())level.setBlock(pos,state,2|16);
                if(!level.getBlockState(pos).equals(state)){complete=false;continue;}
                // Apply and verify the geometry first. If persistence is
                // interrupted, the next call can safely observe the restored
                // bed and commit this admission without duplicating blocks.
                handled.putBoolean(key,true);n.put("ChildBedHandled",handled);OtherMotherLoop.changed(p);
                p.server.overworld().getDataStorage().save();
                try{var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/coraline_first_visits.dat");if(!com.coraline.rebirth.StoryRunStore.read(file).getCompound("data").equals(VisitData.get(p.server).save(new CompoundTag())))return false;}catch(java.io.IOException|RuntimeException failed){return false;}
            }
            if(complete){n.putBoolean("ChildBedRestored",true);OtherMotherLoop.changed(p);}
        }
        if(!ensureHouseProps(p,s,n)||!points(p,s,n))return false;n.remove("Blocker");OtherMotherLoop.changed(p);return true;
    }
    static BlockPos diningTable(ServerPlayer p,VisitData.Scene s){
        var realm=AcceptedButtonEyesWorld.realm(p,s);
        return realm!=null&&realm.table!=null?realm.table:s.table!=null?s.table:supper(s);
    }
    static boolean retiredSupperTable(CinematicPropEntity prop,VisitData.Scene s){
        return prop.kind()==CinematicPropEntity.VICTIM_SUPPER_TABLE&&!GhostVictimDirector.active(s);
    }
    static boolean ensureHouseProps(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!n.getBoolean("WallMirrorsPrepared")){
            if(!mirrors(p,s,n))return false;
            n.putBoolean("WallMirrorsPrepared",true);
        }
        if(n.getBoolean("ExtraHouseObjectsPrepared"))return true;
        if(!object(p,s,n,"house_hall_vase",hall(s).east(),"Rattle the vase",Items.FLOWER_POT)
            ||!object(p,s,n,"house_hall_book",hall(s).north(2),"Lift the storybook",Items.BOOK)
            ||!object(p,s,n,"house_kitchen_cup",kitchen(s).west(2),"Rattle the cup",Items.GLASS_BOTTLE)
            ||!object(p,s,n,"house_bedroom_toy",bedroom(s).south(2),"Move the toy",com.coraline.tycoon.TycoonContent.BALL.get())
            ||!object(p,s,n,"house_bedroom_clock",bedroom(s).west(2),"Shake the clock",Items.CLOCK))return false;
        n.putBoolean("ExtraHouseObjectsPrepared",true);
        OtherMotherLoop.changed(p);return true;
    }
    private static boolean object(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,BlockPos pos,String label,Item item){
        var level=AcceptedButtonEyesWorld.level(p);level.getChunkAt(pos);
        var at=Vec3.atBottomCenterOf(pos).add(0,level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()?.48:1.12,0);
        if(!GhostVictimManifestations.display(p,s,n,id,at,GhostVictimProgress.Disturbance.OBJECT,new ItemStack(item)))return false;
        n.getCompound("Points").getCompound(id).putString("Label",label);
        return true;
    }
    private static boolean points(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(n.getBoolean("HousePointsPrepared"))return true;var table=diningTable(p,s);
        if(!GhostVictimManifestations.display(p,s,n,"supper_bottle",Vec3.atBottomCenterOf(table).add(.7,1,.2),GhostVictimProgress.Disturbance.OBJECT,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)))return false;
        if(!ensureSupperPoints(p,s,n))return false;
        if(!GhostVictimManifestations.display(p,s,n,"kitchen_knife",Vec3.atBottomCenterOf(kitchen(s)).add(.5,1,.2),GhostVictimProgress.Disturbance.KNIFE,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD)))return false;
        var level=AcceptedButtonEyesWorld.level(p);BlockPos[] rooms={hall(s),supper(s),bedroom(s)};
        for(int room=0;room<rooms.length;room++){
            boolean found=false;
            for(int y=0;y<=7&&!found;y++)for(int x=-6;x<=6&&!found;x++)for(int z=-6;z<=6;z++){
                var at=rooms[room].offset(x,y,z);if(level.hasChunkAt(at)&&GhostVictimManifestations.lightSource(level.getBlockState(at))&&GhostVictimManifestations.light(p,s,n,"room_light_"+room,at)){found=true;break;}
            }
        }
        n.putBoolean("HousePointsPrepared",true);return true;
    }
    /** Also called at supper so an already-prepared saved house receives the
     * new interaction points without rebuilding rooms or duplicating props. */
    public static boolean ensureSupperPoints(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var table=diningTable(p,s);
        var level=AcceptedButtonEyesWorld.level(p);
        level.getChunkAt(table);
        var scenery=FirstEscapeScene.context(s);
        if(scenery.hasUUID("VictimSupperTable")&&FirstEscapeScene.prop(p,s,"VictimSupperTable")==null){
            // The saved visual can be lost after a reload. Its missing UUID
            // otherwise prevents ensure() from ever restoring the table.
            scenery.remove("VictimSupperTable");
            scenery.remove("VictimSupperTablePosition");
            OtherMotherLoop.changed(p);
        }
        if(FirstEscapeScene.ensure(p,s,"VictimSupperTable",CinematicPropEntity.VICTIM_SUPPER_TABLE,0,level,Vec3.atBottomCenterOf(table))==null)return false;
        if(n.getInt("SupperObjectsRevision")>=1)return true;
        if(!GhostVictimManifestations.display(p,s,n,"supper_silverware",Vec3.atBottomCenterOf(table).add(-.5,1,.5),GhostVictimProgress.Disturbance.OBJECT,new net.minecraft.world.item.ItemStack(FeastContent.STORY_SPOON.get())))return false;
        if(!GhostVictimManifestations.display(p,s,n,"supper_knife",Vec3.atBottomCenterOf(table).add(.4,1,.6),GhostVictimProgress.Disturbance.KNIFE,new net.minecraft.world.item.ItemStack(FeastContent.STORY_TABLE_KNIFE.get())))return false;
        var base=Vec3.atBottomCenterOf(table);
        if(!GhostVictimManifestations.display(p,s,n,"supper_bread",base.add(-.95,1.08,-.55),GhostVictimProgress.Disturbance.FOOD,new ItemStack(Items.BREAD))
            ||!GhostVictimManifestations.display(p,s,n,"supper_dessert",base.add(.92,1.08,-.72),GhostVictimProgress.Disturbance.ILLUSION,new ItemStack(Items.PUMPKIN_PIE))
            ||!GhostVictimManifestations.display(p,s,n,"supper_cup",base.add(.92,1.08,.82),GhostVictimProgress.Disturbance.OBJECT,new ItemStack(Items.GLASS_BOTTLE)))return false;
        n.putBoolean("SupperObjectsPrepared",true);n.putInt("SupperObjectsRevision",1);OtherMotherLoop.changed(p);return true;
    }
    public static void configureGiftSecret(CompoundTag n,String gift){
        var row=n.getCompound("Points").getCompound("gift_"+gift);if(!row.getString("Kind").equals("GIFT_SECRET")||row.getInt("GiftRevealVersion")>=1)return;
        var item=switch(gift){
            case "cake"->net.minecraft.world.item.Items.SPIDER_EYE;
            case "flower"->net.minecraft.world.item.Items.DEAD_BUSH;
            case "music_box","gadget"->net.minecraft.world.item.Items.REDSTONE;
            case "toy"->net.minecraft.world.item.Items.TRIPWIRE_HOOK;
            case "personal_present"->net.minecraft.world.item.Items.PAPER;
            default->net.minecraft.world.item.Items.STRING;
        };
        String detail=switch(gift){case "cake"->"the cake's ingredients";case "flower"->"the flower's true appearance";
            case "music_box","gadget"->"the hidden mechanism";case "toy","doll"->"the concealed strings";
            case "personal_present"->"the hidden message";default->"the trinket's hidden threads";};
        row.put("RevealedItem",new net.minecraft.world.item.ItemStack(item).save(new CompoundTag()));row.putString("Label","Reveal "+detail);
        row.putInt("GiftRevealVersion",1);
    }
    public static void prepareGiftPreview(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!GhostVictimProgress.hasVictim(n)||GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.NIGHT_PREPARATION)return;
        var plan=GhostVictimSchedule.tomorrow(n);String gift=plan.getString("Gift"),point="gift_"+gift;
        if(n.getInt("PreviewGiftDay")==plan.getInt("Day")&&point.equals(n.getString("PreviewGiftPoint")))return;
        if(!GhostVictimManifestations.display(p,s,n,point,Vec3.atBottomCenterOf(hall(s)).add(0,1,0),GhostVictimProgress.Disturbance.GIFT_SECRET,GhostVictimDirector.gift(gift)))return;
        configureGiftSecret(n,gift);GhostVictimManifestations.show(p,s,n,point,true);
        n.putInt("PreviewGiftDay",plan.getInt("Day"));n.putString("PreviewGiftPoint",point);OtherMotherLoop.changed(p);
    }
    private static boolean mirrors(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        String[] ids={"main_hall","living_room","bedroom","upstairs_corridor"};
        int[][] cells={{35,6,31},{28,6,51},{50,15,23},{35,15,49}};float[] yaw={270,0,270,270};
        var level=AcceptedButtonEyesWorld.level(p);
        for(int i=0;i<ids.length;i++){
            var position=at(s,cells[i][0],cells[i][1],cells[i][2]);
            var prop=FirstEscapeScene.ensure(p,s,"VictimMirror_"+ids[i],CinematicPropEntity.STORY_MIRROR,i,level,Vec3.atBottomCenterOf(position));
            if(prop==null)return false;
            // These anchors are the walkable cells immediately in front of
            // the backing walls in the compact Rosewood blueprint.
            var front=Direction.fromYRot(yaw[i]).getOpposite();
            prop.setPos(Vec3.atBottomCenterOf(position).add(front.getStepX()*-.39,0,front.getStepZ()*-.39));
            prop.setYRot(yaw[i]);prop.frame(0,0,0);
            FirstEscapeScene.context(s).putLong("VictimMirror_"+ids[i]+"Position",position.asLong());
            GhostVictimMirrors.register(n,ids[i],position,yaw[i]);
            var mirror=n.getCompound("Mirrors").getCompound(ids[i]);
            mirror.putUUID("Entity",prop.getUUID());mirror.putBoolean("WallMounted",true);
        }
        return true;
    }
    public static double deterioration(CompoundTag n){
        return switch(GhostVictimProgress.phase(n)){
            case ENTER_MIRROR,EMPTY_WORLD->5;
            case RECONSTRUCTION->5*(1-Math.min(1,GhostVictimProgress.age(n)/(20.*GhostVictimConfig.REBUILD_SECONDS.get())));
            case NEW_VICTIM_RESET->{double duration=20.*GhostVictimConfig.REBUILD_SECONDS.get(),age=GhostVictimProgress.age(n);
                yield age<duration/2?5*Math.min(1,age/(duration/2)):5*(1-Math.min(1,(age-duration/2)/duration));}
            default->0;
        };
    }
    public static boolean ordinaryDoor(VisitData.Scene s,BlockPos position){
        for(var room:HouseCabinets.ROOMS)for(int[] door:room.doors()){
            var at=at(s,door[0],door[1],door[2]);if(at.equals(position)||at.above().equals(position))return GhostEscapeRooms.permittedDoor(s,position);
        }
        return !OtherWorldEstate.outsideHouse(AcceptedButtonEyesWorld.house(s),position);
    }
    private GhostVictimWorld(){}
}
