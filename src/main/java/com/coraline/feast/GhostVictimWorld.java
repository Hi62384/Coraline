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
            var path=child.getNavigation().createPath(stored,0);
            if(GhostVictimMirrors.childCanSeeMirror(n,"bedroom",child,Vec3.atBottomCenterOf(stored))
                    &&(child.blockPosition().equals(stored)||path!=null&&path.canReach()))return stored;
        }
        var anchor=BlockPos.of(mirror.getLong("Position"));BlockPos best=null;double bestDistance=Double.MAX_VALUE;
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++){
            var candidate=anchor.offset(dx,0,dz);if(OtherWorldEstate.outsideHouse(AcceptedButtonEyesWorld.house(s),candidate))continue;
            if(!level.hasChunkAt(candidate)||!GhostVictimNavigation.clear(level,child,candidate))continue;
            var eye=Vec3.atBottomCenterOf(candidate).add(0,child.getEyeHeight(),0);double distance=eye.distanceTo(GhostVictimMirrors.surface(mirror));if(distance>=4.95||!GhostVictimMirrors.childCanSeeMirror(n,"bedroom",child,Vec3.atBottomCenterOf(candidate)))continue;
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
        var level=AcceptedButtonEyesWorld.level(p);
        if(level==null)return false;
        // Active saves may already have a prepared house but no numbered lights.
        if(p.tickCount%40==0)refreshRoomLights(p,s,n);
        var bedroomMirror=n.getCompound("Mirrors").getCompound("bedroom");
        if(!n.getBoolean("WallMirrorsPrepared")||!bedroomMirror.contains("Position",Tag.TAG_LONG)
                ||!GhostVictimMirrors.hasBacking(level,bedroomMirror)){
            if(!mirrors(p,s,n))return false;
            n.putBoolean("WallMirrorsPrepared",true);
        }
        for(String id:new String[]{"house_hall_vase","house_hall_book","house_kitchen_cup","house_bedroom_toy",
                "house_bedroom_clock","supper_bottle","kitchen_knife","supper_cup"})
            GhostVictimManifestations.hideLegacyHouseholdDisplay(p,s,n,id);
        return true;
    }
    private static boolean points(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!n.getBoolean("HousePointsPrepared")&&!ensureSupperPoints(p,s,n))return false;
        refreshRoomLights(p,s,n);
        n.putBoolean("HousePointsPrepared",true);
        return true;
    }

    static void refreshRoomLights(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var level=AcceptedButtonEyesWorld.level(p);
        if(level==null)return;
        BlockPos[] rooms={hall(s),supper(s),bedroom(s),kitchen(s),at(s,28,6,51),at(s,35,15,48)};
        var points=n.getCompound("Points");
        for(int room=0;room<rooms.length;room++){
            String id="room_light_"+room;
            var previous=points.getCompound(id);
            if(previous.contains("EffectUntil"))continue;
            if(previous.contains("Position")){
                var at=BlockPos.of(previous.getLong("Position"));
                if(!level.hasChunkAt(at))continue;
                if(GhostVictimManifestations.lightSource(level.getBlockState(at))
                        &&!level.getBlockState(at).is(Blocks.LIGHT))continue;
            }
            var reserved=new java.util.HashSet<Long>();
            for(String key:points.getAllKeys()){
                var row=points.getCompound(key);
                if(!id.equals(key)&&row.getString("Kind").equals("LIGHT")&&row.contains("Position",Tag.TAG_LONG))
                    reserved.add(row.getLong("Position"));
            }
            var center=rooms[room];
            BlockPos best=null;
            double nearest=Double.MAX_VALUE;
            for(var cell:BlockPos.betweenClosed(center.offset(-6,0,-6),center.offset(6,7,6))){
                double distance=cell.distSqr(center);
                if(distance>=nearest||reserved.contains(cell.asLong())||!level.hasChunkAt(cell))continue;
                var state=level.getBlockState(cell);
                if(state.is(Blocks.LIGHT)||!GhostVictimManifestations.lightSource(state)||level.getBlockEntity(cell)!=null)continue;
                best=cell.immutable();
                nearest=distance;
            }
            if(best!=null){
                if(GhostVictimManifestations.light(p,s,n,id,best))OtherMotherLoop.changed(p);
            }
        }
    }
    /** Also called at supper so an already-prepared saved house receives the
     * new interaction points without rebuilding rooms or duplicating props. */
    public static boolean ensureSupperPoints(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var table=diningTable(p,s);
        var level=AcceptedButtonEyesWorld.level(p);
        level.getChunkAt(table);
        var prop=ensureSupperProp(p,s,"VictimSupperTable",CinematicPropEntity.VICTIM_SUPPER_TABLE,0,level,Vec3.atBottomCenterOf(table));
        if(prop==null||prop.level()!=level||prop.kind()!=CinematicPropEntity.VICTIM_SUPPER_TABLE)return false;
        if(!GhostVictimManifestations.display(p,s,n,"supper_silverware",Vec3.atBottomCenterOf(table).add(-.5,1,.5),GhostVictimProgress.Disturbance.OBJECT,new net.minecraft.world.item.ItemStack(FeastContent.STORY_SPOON.get())))return false;
        if(!GhostVictimManifestations.display(p,s,n,"supper_knife",Vec3.atBottomCenterOf(table).add(.4,1,.6),GhostVictimProgress.Disturbance.KNIFE,new net.minecraft.world.item.ItemStack(FeastContent.STORY_TABLE_KNIFE.get())))return false;
        var base=Vec3.atBottomCenterOf(table);
        if(!GhostVictimManifestations.display(p,s,n,"supper_bread",base.add(-.95,1.08,-.55),GhostVictimProgress.Disturbance.FOOD,new ItemStack(Items.BREAD))
            ||!GhostVictimManifestations.display(p,s,n,"supper_dessert",base.add(.92,1.08,-.72),GhostVictimProgress.Disturbance.ILLUSION,new ItemStack(Items.PUMPKIN_PIE)))return false;
        n.putBoolean("SupperObjectsPrepared",true);n.putInt("SupperObjectsRevision",1);OtherMotherLoop.changed(p);return true;
    }
    static CinematicPropEntity ensureSupperProp(ServerPlayer p,VisitData.Scene s,String key,int kind,int slot,ServerLevel level,Vec3 target){
        var scenery=FirstEscapeScene.context(s);
        if(!scenery.hasUUID(key))return FirstEscapeScene.ensure(p,s,key,kind,slot,level,target);
        var existing=FirstEscapeScene.prop(p,s,key);
        if(existing!=null)return existing.level()==level&&existing.kind()==kind&&existing.slot()==slot?existing:null;
        if(scenery.contains(key+"Position",Tag.TAG_LONG)){
            var at=BlockPos.of(scenery.getLong(key+"Position"));
            level.getChunkAt(at);
            if(!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at).toLong()))return null;
        }
        var id=scenery.getUUID(key);
        if(com.coraline.rebirth.LegacyActorRecovery.check(p.server,id)
                !=com.coraline.rebirth.LegacyActorRecovery.Result.ABSENT)return null;
        var replacement=CinematicPropEntity.TYPE.get().create(level);
        if(replacement==null)return null;
        replacement.configure(s,kind,slot);replacement.setUUID(id);replacement.setPos(target);
        if(!level.addFreshEntity(replacement))return null;
        scenery.putLong(key+"Position",replacement.blockPosition().asLong());
        OtherMotherLoop.changed(p);
        return replacement;
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
    record GiftSurface(Vec3 display,BlockPos approach){}
    static GiftSurface giftSurface(ServerPlayer p,VisitData.Scene s,net.minecraft.world.entity.PathfinderMob child){
        var level=AcceptedButtonEyesWorld.level(p);
        if(level==null)return null;
        for(var center:java.util.List.of(hall(s),diningTable(p,s),at(s,28,6,51))){
            for(var pos:BlockPos.betweenClosed(center.offset(-5,0,-5),center.offset(5,1,5))){
                if(!level.hasChunkAt(pos))continue;
                var state=level.getBlockState(pos);
                if(!state.is(ModBlocks.ROSEWOOD_TABLE.get())&&!state.is(ModBlocks.ROSEWOOD_CABINET.get())
                        &&!state.is(ModBlocks.ROSEWOOD_DRAWER.get()))continue;
                var shape=state.getCollisionShape(level,pos);
                if(shape.isEmpty())continue;
                var top=Vec3.atBottomCenterOf(pos).add(0,shape.max(Direction.Axis.Y),0);
                var space=new net.minecraft.world.phys.AABB(top.x-.2,top.y+.01,top.z-.2,top.x+.2,top.y+.32,top.z+.2);
                if(!level.noCollision(space)||level.containsAnyLiquid(space))continue;
                var approach=child==null?null:GhostVictimNavigation.approach(child,pos.immutable());
                if(child==null||approach!=null)return new GiftSurface(top,approach);
            }
        }
        return null;
    }
    public static void prepareGiftPreview(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!GhostVictimProgress.hasVictim(n)||GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.NIGHT_PREPARATION)return;
        var plan=GhostVictimSchedule.tomorrow(n);String gift=plan.getString("Gift"),point="gift_"+gift;
        var surface=giftSurface(p,s,null);
        if(surface==null){GhostVictimManifestations.show(p,s,n,point,false);return;}
        if(!GhostVictimManifestations.display(p,s,n,point,surface.display(),GhostVictimProgress.Disturbance.GIFT_SECRET,GhostVictimDirector.gift(gift)))return;
        configureGiftSecret(n,gift);GhostVictimManifestations.show(p,s,n,point,true);
        n.putInt("PreviewGiftDay",plan.getInt("Day"));n.putString("PreviewGiftPoint",point);OtherMotherLoop.changed(p);
    }
    private static boolean mirrors(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        String[] ids={"main_hall","living_room","bedroom","upstairs_corridor"};
        int[][] cells={{35,6,31},{28,6,51},{50,15,23},{35,15,49}};float[] yaw={270,0,270,270};
        var level=AcceptedButtonEyesWorld.level(p);
        for(int i=0;i<ids.length;i++){
            var anchor=at(s,cells[i][0],cells[i][1],cells[i][2]);
            var position=mirrorPosition(p,s,level,anchor,yaw[i]);
            if(position==null){
                n.getCompound("Mirrors").remove(ids[i]);
                if(ids[i].equals("bedroom")){
                    n.putString("Blocker","The bedroom mirror needs a clear wall and a walkable space in front of it.");
                    return false;
                }
                continue;
            }
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
    private static BlockPos mirrorPosition(ServerPlayer player,VisitData.Scene scene,ServerLevel level,BlockPos anchor,float yaw){
        var front=Direction.fromYRot(yaw).getOpposite();
        var side=front.getClockWise();
        for(int offset:new int[]{0,-1,1,-2,2,-3,3}){
            var at=anchor.relative(side,offset);
            var stand=at.relative(front);
            if(OtherWorldEstate.outsideHouse(AcceptedButtonEyesWorld.house(scene),stand)
                    ||!level.hasChunkAt(at)||!level.hasChunkAt(stand))continue;
            var trial=new CompoundTag();
            trial.putLong("Position",at.asLong());trial.putFloat("Yaw",yaw);trial.putBoolean("WallMounted",true);
            if(!GhostVictimMirrors.hasBacking(level,trial)
                    ||!level.getBlockState(stand.below()).isFaceSturdy(level,stand.below(),Direction.UP))continue;
            var surface=GhostVictimMirrors.surface(trial);
            var area=new net.minecraft.world.phys.AABB(
                    Vec3.atBottomCenterOf(stand).add(-.26,.13,-.26),Vec3.atBottomCenterOf(stand).add(.26,1.75,.26));
            if(!level.noCollision(area)||level.containsAnyLiquid(area))continue;
            var eye=Vec3.atBottomCenterOf(stand).add(0,1.45,0);
            var end=surface.add(Vec3.atBottomCenterOf(stand).subtract(surface).normalize().scale(.16));
            if(level.clip(new net.minecraft.world.level.ClipContext(eye,end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,player)).getType()
                    ==net.minecraft.world.phys.HitResult.Type.MISS)return at;
        }
        return null;
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
