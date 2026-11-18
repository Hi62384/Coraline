package com.coraline.feast;

import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** NPC outings in the existing authored venues. Attending an outing never
 * declares that the ghost won a player minigame or grants player prizes. */
public final class GhostVictimActivities {
    static CompoundTag data(CompoundTag n){if(!n.contains("ParticipantActivity"))n.put("ParticipantActivity",new CompoundTag());return n.getCompound("ParticipantActivity");}
    static BlockPos housePortal(VisitData.Scene s){return GhostVictimWorld.hall(s).south(3);}
    static BlockPos center(ServerPlayer p,VisitData.Scene s,String id){return switch(id){
        case "magic_garden"->MagicGardenActivity.center(p,s);case "mice_circus"->MiceCircusActivity.center(p,s);
        case "magic_show"->MagicShowActivity.center(p,s);case "fishing_contest"->FishingContestActivity.center(p,s);
        case "cake_making_contest"->CakeContestActivity.center(p,s);case "carnival"->CarnivalActivity.center(p,s);
        case "grand_feast"->GrandFeastActivity.center(p,s);case "treasure_hunt"->ExpeditionActivities.location(VillageActivity.center(p,s),OtherMotherActivities.Activity.TREASURE_HUNT);
        case "other_village"->VillageActivity.center(p,s);default->throw new IllegalArgumentException("Unknown authored outing");};}
    static BlockPos entrance(BlockPos c,String id){return c.south(switch(id){case "magic_garden"->43;case "mice_circus"->23;case "fishing_contest"->30;case "grand_feast"->36;default->10;});}
    private static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c,String id,long seed){
        LoopProps.hold(l,c);switch(id){
            case "magic_garden"->MagicGardenActivity.build(l,c);case "mice_circus"->MiceCircusActivity.victimPerformance(p,s,l,c,seed,0);
            case "magic_show"->MagicShowActivity.build(l,c);case "fishing_contest"->{FishingContestActivity.build(l,c);FishingContestActivity.victimAudience(p,s,l,c);}
            case "cake_making_contest"->CakeContestActivity.build(l,c);case "carnival"->CarnivalActivity.build(l,c);
            case "grand_feast"->{GrandFeastActivity.build(l,c);GrandFeastActivity.victimAudience(p,s,l,c);}case "other_village"->{VillageActivity.build(l,c);VillageActivity.spawnVillagers(p,s,l,c);}
            case "treasure_hunt"->TreasureScenery.build(l,c,seed);
            default->throw new IllegalArgumentException("Unsupported outing");}
    }
    public static boolean valid(ScenePropEntity prop,VisitData.Scene s){
        if(!GhostVictimDirector.active(s)||!s.door.equals(prop.storyDoor)||prop.getPersistentData().getInt("LoopGeneration")!=s.loop.generation)return false;
        var n=GhostVictimEntry.data(s);var a=data(n);
        for(Tag entry:n.getList("ScoutSites",Tag.TAG_COMPOUND)){
            var scout=(CompoundTag)entry;
            if(com.coraline.otherworld.OtherWorld.isOther(prop.level())&&prop.kind()==ScenePropEntity.ACTIVITY_PORTAL&&scout.hasUUID("Portal")&&scout.getUUID("Portal").equals(prop.getUUID()))return true;
            if(ActivityWorld.isActivity(prop.level())&&scout.contains("Center")&&prop.distanceToSqr(BlockPos.of(scout.getLong("Center")).getCenter())<200*200)return true;
        }
        if(com.coraline.otherworld.OtherWorld.isOther(prop.level()))return prop.kind()==ScenePropEntity.ACTIVITY_PORTAL&&prop.blockPosition().equals(housePortal(s));
        return ActivityWorld.isActivity(prop.level())&&a.contains("Center")&&prop.distanceToSqr(BlockPos.of(a.getLong("Center")).getCenter())<200*200;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var child=GhostVictimActors.find(p,s,n);if(child==null)return;
        String id=GhostVictimSchedule.currentActivity(n);if(id.isEmpty()){GhostVictimProgress.move(n,GhostVictimProgress.Phase.BEDTIME);n.remove("ChildRoutine");return;}
        var a=data(n);var site=p.server.getLevel(ActivityWorld.KEY);var home=AcceptedButtonEyesWorld.level(p);if(site==null||home==null)return;
        var c=center(p,s,id);var entry=entrance(c,id);long clock=GhostVictimProgress.clock(n);
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.BETWEEN_ACTIVITIES&&GhostVictimProgress.age(n)>=1200){
            // Give the normal pathfinder and its approach recovery a full minute.
            // If the departure route still cannot complete, return the same leased
            // child to the house and mark this outing obstructed without granting
            // its completion trust. This keeps a blocked venue door from freezing
            // every later day of the storyline.
            var returned=child.level()==home?child:ShowTravel.guest(child,p,s,home,Vec3.atBottomCenterOf(housePortal(s)),false,"VictimOutingRecovery");
            if(returned instanceof TycoonNpc same){
                GhostVictimActors.remember(n,same);
                if(!GhostVictimSchedule.skipObstructedDeparture(n,GhostVictimProgress.child(n).getUUID("Id"),id))return;
                recordFailure(n,id+":departure");remove(p,a,"HousePortal");remove(p,a,"ExitPortal");
                n.remove("ParticipantActivity");n.remove("ChildRoutine");OtherMotherLoop.changed(p);
            }
            return;
        }
        if(!a.getString("Id").equals(id)){
            a=new CompoundTag();n.put("ParticipantActivity",a);a.putString("Id",id);a.putLong("Center",c.asLong());a.putLong("Seed",GhostVictimProgress.child(n).getUUID("Id").getLeastSignificantBits()^n.getInt("Day")*7919L^id.hashCode());
            var portal=LoopProps.spawn(home,s,ScenePropEntity.ACTIVITY_PORTAL,Vec3.atBottomCenterOf(housePortal(s)),-501);a.putUUID("HousePortal",portal.getUUID());OtherMotherLoop.changed(p);
        }
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.BETWEEN_ACTIVITIES){
            if(GhostVictimRoutines.travel(p,s,n,child,housePortal(s),false)!=GhostVictimNavigation.Result.ARRIVED)return;
            build(p,s,site,c,id,a.getLong("Seed"));site.getChunkAt(entry);
            var moved=ShowTravel.guest(child,p,s,site,Vec3.atBottomCenterOf(entry),false,"VictimOuting");if(!(moved instanceof TycoonNpc same))return;
            GhostVictimActors.remember(n,same);a.putLong("Started",clock);a.putInt("Station",0);a.putLong("NextStationAt",clock+100);
            GhostVictimProgress.move(n,GhostVictimProgress.Phase.ACTIVITY);n.remove("ChildRoutine");
            var exit=LoopProps.spawn(site,s,ScenePropEntity.ACTIVITY_EXIT,Vec3.atBottomCenterOf(entry),-502);a.putUUID("ExitPortal",exit.getUUID());
            points(p,s,n,site,c,id,a.getLong("Seed"));OtherMotherLoop.changed(p);return;
        }
        if(child.level()!=site)return;int age=(int)(clock-a.getLong("Started"));
        if(GhostVictimProgress.motherTrust(n)<=0||GhostVictimProgress.motherTrust(n)>=1){
            if(!a.getBoolean("Returning")){a.putBoolean("Returning",true);a.putLong("ReturningAt",clock);n.remove("ChildRoutine");}
            a.putBoolean("ExperienceCompleted",false);
            a.putBoolean("TrustInterrupted",true);
        }
        if(p.tickCount%100==0)points(p,s,n,site,c,id,a.getLong("Seed"));
        if(id.equals("mice_circus"))MiceCircusActivity.victimPerformance(p,s,site,c,a.getLong("Seed"),age);
        if(id.equals("magic_show")&&age%10==0)MagicShowActivity.victimPerformance(p,s,site,c,a.getLong("Seed"),age);
        BlockPos[] stations=stations(c,id,a.getLong("Seed"));int step=a.getInt("Station");
        if(!a.getBoolean("Returning")&&step<stations.length&&clock>=a.getLong("NextStationAt")){
            var result=GhostVictimRoutines.travel(p,s,n,child,stations[step],false);
            if(result==GhostVictimNavigation.Result.ARRIVED){
                a.putInt("Station",step+1);a.putLong("NextStationAt",clock+Math.max(100,GhostVictimConfig.ACTIVITY_SECONDS.get()*20L/stations.length));n.remove("ChildRoutine");
                child.gesture(id.equals("grand_feast")?3:2,60);
                if(id.equals("cake_making_contest")){CakeMaking.victimWorkpiece(site,s,c,step);child.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(step<2?Items.WHEAT:step<4?Items.MILK_BUCKET:Items.CAKE));}
                if(id.equals("fishing_contest")){child.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(step==stations.length-1?Items.COD:Items.FISHING_ROD));}
                if(id.equals("treasure_hunt"))GhostVictimDirector.words(n,"I think the next clue leads this way.");
            }else if(result==GhostVictimNavigation.Result.BLOCKED){a.putBoolean("Obstructed",true);}
        }else if(!a.getBoolean("Returning")){
            child.getNavigation().stop();
            if(step==2||step>=stations.length){
                var point=n.getCompound("Points").getCompound("outing_"+id+(step>=stations.length?"_rattle":"_secret"));
                if(point.hasUUID("Entity")) {
                    var focus=GhostVictimManifestations.interactionPoint(point);
                    child.getLookControl().setLookAt(focus.x,focus.y,focus.z,30,30);
                }
            }else GhostVictimRoutines.lookAtMirror(n,child);
        }
        boolean completed=a.getInt("Station")>=stations.length&&age>=GhostVictimConfig.ACTIVITY_SECONDS.get()*20;
        boolean timedOut=age>=12000;
        if(!a.getBoolean("Returning")&&!completed&&!timedOut)return;
        if(!a.getBoolean("Returning")){a.putBoolean("Returning",true);a.putBoolean("ExperienceCompleted",completed);a.putLong("ReturningAt",clock);n.remove("ChildRoutine");}
        if(GhostVictimRoutines.travel(p,s,n,child,entry,false)==GhostVictimNavigation.Result.ARRIVED){
            child.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            var moved=ShowTravel.guest(child,p,s,home,Vec3.atBottomCenterOf(housePortal(s)),false,"VictimOuting");
            if(moved instanceof TycoonNpc same){finishReturn(p,s,n,a,same,id);return;}
        }
        // A failed physical return must not strand the story forever. Only after
        // the normal pathfinder and its recovery attempts have both had a full
        // minute do we use the existing identity-preserving dimension transfer.
        // This is explicitly recorded as a failed outing and grants no trust.
        if(a.getBoolean("Returning")&&(clock-a.getLong("ReturningAt")>=1200||age>=12000)){
            a.putString("ReturnBlocker",n.getString("LastRouteBlocker"));child.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            var moved=ShowTravel.guest(child,p,s,home,Vec3.atBottomCenterOf(housePortal(s)),false,"VictimOutingRecovery");
            if(moved instanceof TycoonNpc same){a.putBoolean("ExperienceCompleted",false);finishReturn(p,s,n,a,same,id);}
        }
    }
    private static void finishReturn(ServerPlayer p,VisitData.Scene s,CompoundTag n,CompoundTag a,TycoonNpc child,String id){
        GhostVictimActors.remember(n,child);
        if(a.getBoolean("ExperienceCompleted"))GhostVictimProgress.trust(n,GhostVictimProgress.motherTrust(n)+.06,GhostVictimProgress.ghostTrust(n));
        else if(a.getBoolean("TrustInterrupted"))recordOuting(n,id,"InterruptedOutings");
        else recordFailure(n,id);
        if(ActivityWorld.isActivity(p.level()))returnGhost(p,s,n);
        remove(p,a,"HousePortal");remove(p,a,"ExitPortal");GhostVictimSchedule.completeActivity(n,GhostVictimProgress.child(n).getUUID("Id"),id);n.remove("ParticipantActivity");n.remove("ChildRoutine");OtherMotherLoop.changed(p);
    }
    private static void points(ServerPlayer p,VisitData.Scene s,CompoundTag n,ServerLevel level,BlockPos c,String id,long seed){
        var opportunity=GhostVictimSchedule.activity(id);String secret="outing_"+id+"_secret",risk="outing_"+id+"_rattle";
        var stops=stations(c,id,seed);var secretStop=stops[Math.min(1,stops.length-1)];var riskStop=stops[stops.length-1];
        ItemStack normal=new ItemStack(switch(id){case "magic_garden"->Items.BLUE_ORCHID;case "mice_circus"->Items.STRING;case "other_village"->Items.EMERALD;case "magic_show"->Items.PAINTING;case "treasure_hunt"->Items.PAPER;case "fishing_contest"->Items.COD;case "cake_making_contest"->Items.SUGAR;case "carnival"->Items.RABBIT_FOOT;default->Items.COOKED_CHICKEN;});
        groundedPoint(p,s,n,level,secret,Vec3.atBottomCenterOf(secretStop).add(.75,0,0),GhostVictimProgress.Disturbance.ILLUSION,normal);
        groundedPoint(p,s,n,level,risk,Vec3.atBottomCenterOf(riskStop).add(-.75,0,0),id.equals("cake_making_contest")||id.equals("grand_feast")?GhostVictimProgress.Disturbance.KNIFE:GhostVictimProgress.Disturbance.OBJECT,new ItemStack(id.equals("cake_making_contest")||id.equals("grand_feast")?Items.IRON_SWORD:Items.GLASS_BOTTLE));
        var row=n.getCompound("Points").getCompound(secret);row.putString("Label","Reveal "+opportunity.useful().replace('_',' '));
        row.put("RevealedItem",new ItemStack(switch(id){case "magic_garden"->Items.DEAD_BUSH;case "mice_circus","magic_show","carnival"->Items.COBWEB;case "other_village"->Items.PAPER;case "treasure_hunt"->Items.SPIDER_EYE;case "fishing_contest"->Items.STRING;default->Items.ROTTEN_FLESH;}).save(new CompoundTag()));
        n.getCompound("Points").getCompound(risk).putString("Label","Disturb "+opportunity.risk().replace('_',' '));
        if(id.equals("magic_garden")){
            var at=c.offset(-3,0,4);var mirror=FirstEscapeScene.ensure(p,s,"VictimGardenMirror",CinematicPropEntity.STORY_MIRROR,120,level,Vec3.atBottomCenterOf(at));
            if(mirror!=null){mirror.setYRot(90);GhostVictimMirrors.register(n,"magic_garden_reflection",at,90,level.dimension().location().toString());n.getCompound("Mirrors").getCompound("magic_garden_reflection").putUUID("Entity",mirror.getUUID());}
        }
    }
    private static void groundedPoint(ServerPlayer p,VisitData.Scene s,CompoundTag n,ServerLevel level,
                                      String id,Vec3 at,GhostVictimProgress.Disturbance kind,ItemStack item){
        level.getChunkAt(BlockPos.containing(at));
        var support=level.clip(new net.minecraft.world.level.ClipContext(at.add(0,1.5,0),at.add(0,-1.5,0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        if(support.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK
                &&support.getDirection()==net.minecraft.core.Direction.UP){
            var surface=support.getLocation();
            var volume=new net.minecraft.world.phys.AABB(surface.x-.15,surface.y+.01,surface.z-.15,
                    surface.x+.15,surface.y+.25,surface.z+.15);
            if(level.noCollision(volume)){
                GhostVictimManifestations.displayAt(p,s,n,level,id,surface,kind,item);
                return;
            }
        }
        GhostVictimManifestations.retireDisplay(p,s,n,id);
    }
    private static BlockPos[] stations(BlockPos c,String id,long seed){return switch(id){
        case "magic_garden"->new BlockPos[]{c.south(30),c.south(15),c.south(4),c.north(20)};
        case "mice_circus"->new BlockPos[]{c.south(20),c.offset(7,0,20),c.south(18)};
        case "magic_show"->new BlockPos[]{c.south(8),c.offset(4,0,8),c.south(6)};
        case "cake_making_contest"->new BlockPos[]{c.east(),c.west(),c.south(),c.north(),c.east(),c.south()};
        case "fishing_contest"->new BlockPos[]{c.south(15),c.offset(8,0,12),c.south(15)};
        case "grand_feast"->new BlockPos[]{c.south(26),c.offset(5,0,20),c.south(16)};
        case "other_village"->new BlockPos[]{c.south(20),c.east(20),c.north(20),c.west(20)};
        case "carnival"->new BlockPos[]{c.south(8),c.east(8),c.north(8),c.west(8)};
        case "treasure_hunt"->{int[] route=TreasureRoute.generate(seed);BlockPos[] result=new BlockPos[route.length];for(int i=0;i<route.length;i++)result[i]=TreasureRoute.LANDMARKS.get(route[i]).note(c).south();yield result;}
        default->throw new IllegalArgumentException("Unsupported itinerary");};}
    public static void follow(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var a=data(n);if(GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.ACTIVITY||p.distanceToSqr(housePortal(s).getCenter())>36||!com.coraline.otherworld.OtherWorld.isOther(p.level()))return;
        var level=p.server.getLevel(ActivityWorld.KEY);if(level==null)return;var c=BlockPos.of(a.getLong("Center"));var at=entrance(c,a.getString("Id"));
        n.putLong("GhostCenter",c.asLong());n.putString("GhostDimension",ActivityWorld.KEY.location().toString());n.putDouble("GhostRadius",190);p.teleportTo(level,at.getX()+.5,at.getY()+1,at.getZ()+.5,p.getYRot(),0);
    }
    public static boolean escort(com.coraline.parents.ParentEntity actor,ServerPlayer p,VisitData.Scene s,CompoundTag n){
        String key=actor.isMom()?"MotherOuting":"FatherOuting";var trip=n.getCompound(key);var current=data(n);
        var site=p.server.getLevel(ActivityWorld.KEY);var home=AcceptedButtonEyesWorld.level(p);if(site==null||home==null)return false;
        boolean active=GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.ACTIVITY&&current.contains("Center");
        if(actor.level()==site&&trip.contains("Center")&&(!active||!trip.getString("Id").equals(current.getString("Id")))){
            var route=trip.getCompound("Route");trip.put("Route",route);var at=entrance(BlockPos.of(trip.getLong("Center")),trip.getString("Id"));
            var result=GhostVictimNavigation.walk(actor,route,at,GhostVictimProgress.clock(n),1,600,door->false);
            if(result==GhostVictimNavigation.Result.ARRIVED||result==GhostVictimNavigation.Result.BLOCKED){
                var moved=ShowTravel.guest(actor,p,s,home,Vec3.atBottomCenterOf(housePortal(s)),false,"VictimParentOuting");
                if(moved!=null){if(result==GhostVictimNavigation.Result.BLOCKED)n.putString("LastParentRouteBlocker","The parent could not leave the venue by foot.");n.remove(key);OtherMotherLoop.changed(p);}
            }
            return true;
        }
        if(active&&actor.level()==home){
            if(!trip.getString("Id").equals(current.getString("Id"))){trip=new CompoundTag();trip.putString("Id",current.getString("Id"));trip.putLong("Center",current.getLong("Center"));n.put(key,trip);}
            var route=trip.getCompound("Route");trip.put("Route",route);
            var result=GhostVictimNavigation.walk(actor,route,housePortal(s),GhostVictimProgress.clock(n),1,600,door->GhostVictimWorld.ordinaryDoor(s,door));
            if(result==GhostVictimNavigation.Result.ARRIVED){
                var at=entrance(BlockPos.of(current.getLong("Center")),current.getString("Id"));
                if(ShowTravel.guest(actor,p,s,site,Vec3.atBottomCenterOf(at),false,"VictimParentOuting")!=null){trip.remove("Route");OtherMotherLoop.changed(p);}
            }else if(result==GhostVictimNavigation.Result.BLOCKED){
                var at=entrance(BlockPos.of(current.getLong("Center")),current.getString("Id"));
                if(ShowTravel.guest(actor,p,s,site,Vec3.atBottomCenterOf(at),false,"VictimParentOuting")!=null){trip.remove("Route");n.putString("LastParentRouteBlocker","The parent could not reach the venue by foot.");OtherMotherLoop.changed(p);}
            }
            return true;
        }
        return false;
    }
    public static void returnGhost(ServerPlayer p,VisitData.Scene s,CompoundTag n){n.remove("GhostCenter");n.remove("GhostDimension");n.remove("GhostRadius");AcceptedButtonEyesWorld.transfer(p,s,housePortal(s));}
    /** Tomorrow's real venues can be scouted while the same child is at home.
     * No child is spawned and no minigame result is awarded by scouting. */
    public static void prepareScouting(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        GhostVictimWorld.prepareGiftPreview(p,s,n);
        if(n.contains("ScoutSites")){
            if(n.getInt("ScoutEntranceVersion")>=1||n.contains("ScoutActivity"))return;
            for(Tag entry:n.getList("ScoutSites",Tag.TAG_COMPOUND))remove(p,(CompoundTag)entry,"Portal");
            n.remove("ScoutSites");
        }
        var level=AcceptedButtonEyesWorld.level(p);if(level==null)return;
        var sites=new ListTag();var planned=GhostVictimSchedule.tomorrow(n).getList("Activities",Tag.TAG_STRING);
        for(int i=0;i<Math.min(1,planned.size());i++){
            String id=planned.getString(i);var row=new CompoundTag();var at=housePortal(s);
            LoopProps.hold(level,at);
            if(!RescueEpilogue.safeReturn(level,p,net.minecraft.world.entity.Pose.STANDING,Vec3.atBottomCenterOf(at)))return;
            row.putString("Id",id);row.putLong("Center",center(p,s,id).asLong());row.putLong("Entrance",at.asLong());
            var portal=LoopProps.spawn(level,s,ScenePropEntity.ACTIVITY_PORTAL,Vec3.atBottomCenterOf(at),-510-i);row.putUUID("Portal",portal.getUUID());sites.add(row);
        }
        n.put("ScoutSites",sites);n.putInt("ScoutEntranceVersion",1);OtherMotherLoop.changed(p);
    }
    public static void scout(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id){
        if(GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.NIGHT_PREPARATION||!com.coraline.otherworld.OtherWorld.isOther(p.level())||GhostVictimProgress.imprisoned(n))return;
        for(Tag entry:n.getList("ScoutSites",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)entry;if(!row.getString("Id").equals(id)||p.distanceToSqr(BlockPos.of(row.getLong("Entrance")).getCenter())>36)continue;
            var level=p.server.getLevel(ActivityWorld.KEY);if(level==null)return;var c=BlockPos.of(row.getLong("Center"));
            long seed=GhostVictimProgress.child(n).getUUID("Id").getLeastSignificantBits()^(n.getInt("Day")+1)*7919L^id.hashCode();
            build(p,s,level,c,id,seed);points(p,s,n,level,c,id,seed);var at=entrance(c,id);
            if(!row.hasUUID("ExitPortal")){var exit=LoopProps.spawn(level,s,ScenePropEntity.ACTIVITY_EXIT,Vec3.atBottomCenterOf(at),-520);row.putUUID("ExitPortal",exit.getUUID());}
            n.putString("ScoutActivity",id);n.putLong("GhostCenter",c.asLong());n.putString("GhostDimension",ActivityWorld.KEY.location().toString());n.putDouble("GhostRadius",190);
            p.teleportTo(level,at.getX()+.5,at.getY()+1,at.getZ()+.5,p.getYRot(),0);OtherMotherLoop.changed(p);return;
        }
    }
    public static void finishScouting(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(n.contains("PreviewGiftPoint")){GhostVictimManifestations.show(p,s,n,n.getString("PreviewGiftPoint"),false);n.remove("PreviewGiftPoint");n.remove("PreviewGiftDay");}
        if(n.contains("ScoutActivity")&&ActivityWorld.isActivity(p.level()))returnGhost(p,s,n);
        for(Tag entry:n.getList("ScoutSites",Tag.TAG_COMPOUND)){var row=(CompoundTag)entry;remove(p,row,"Portal");remove(p,row,"ExitPortal");}
        n.remove("ScoutActivity");n.remove("ScoutSites");
    }
    public static void retireOuting(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(n.contains("ParticipantActivity")){var a=data(n);remove(p,a,"HousePortal");remove(p,a,"ExitPortal");n.remove("ParticipantActivity");}
        finishScouting(p,s,n);
        if(ActivityWorld.isActivity(p.level()))returnGhost(p,s,n);
    }
    private static void remove(ServerPlayer p,CompoundTag a,String key){if(a.hasUUID(key))for(var l:p.server.getAllLevels()){var actor=l.getEntity(a.getUUID(key));if(actor!=null)actor.discard();}}
    private static void recordFailure(CompoundTag n,String id){recordOuting(n,id,"ObstructedOutings");}
    private static void recordOuting(CompoundTag n,String id,String ledger){var records=n.getList(ledger,Tag.TAG_STRING);records.add(StringTag.valueOf(n.getInt("Day")+":"+id));while(records.size()>64)records.remove(0);n.put(ledger,records);}
    private GhostVictimActivities(){}
}
