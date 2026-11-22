package com.coraline.feast;

import static com.coraline.feast.RescueChapter.Phase.*;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.registry.*;
import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.block.*;

/** Rescue encounters using the existing owner-allocated venues and canonical cast leases. */
public final class RescueEncounters {
    public static BlockPos house(VisitData.Scene s){return BlockPos.of(ParentRescueStory.scenery(s).getLong("House"));}
    /** An interrupted collapse may save the phase without its original
     * physically reached start and timing. Requiring all three prevents a
     * phantom (0,0,0) respawn or an endless zero-duration death loop. */
    static boolean collapseCheckpointValid(CompoundTag n){
        double radius=n.getDouble("CollapseInitialRadius");
        return n.contains("CollapseStart",Tag.TAG_LONG)&&n.getLong("CollapseStart")!=0L
                &&n.getInt("CollapseDuration")>0&&Double.isFinite(radius)&&radius>48;
    }
    static boolean restoreIncompleteCollapse(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        if(collapseCheckpointValid(n))return false;
        // Replay the REAL exterior objective. Never grant an eye, Cat rescue,
        // exit receipt or boss completion in lieu of the missing proof.
        RescueChapter.go(p,s,EXTERIOR);
        n.putBoolean("CatRescued",false);
        n.remove("CatCollapsePlaced");n.remove("CatCollapsePosition");
        n.remove("HouseSafe");
        n.putString("Blocker","The collapse checkpoint was incomplete. Go outside to restart the escape.");
        OtherMotherLoop.changed(p);
        return true;
    }
    public static BlockPos garden(ServerPlayer p,VisitData.Scene s){var n=RescueChapter.data(s);if(n.contains("Garden"))return BlockPos.of(n.getLong("Garden"));var at=OtherWorldEstate.rescueGarden(house(s));var l=p.server.getLevel(OtherWorld.KEY);if(l!=null){l.getChunkAt(at);var safe=OtherWorld.safePlayer(l,at);if(safe!=null)at=safe;}n.putLong("Garden",at.asLong());return at;}
    static Vec3 center(ServerPlayer p,VisitData.Scene s){return Vec3.atBottomCenterOf(switch(RescueChapter.phase(s)){case THEATRE_SEARCH,WRAPPER_REACH,THEATRE_GRAB,THEATRE_FIGHT,THEATRE_EYE->TheatreSite.center(p,s).north(28).above();case BOB_E,SWARM_TRANSFORM,SWARM_FIGHT->MouseCircusSite.center(p,s);default->garden(p,s);});}
    /** Match each pre-finale checkpoint to its own dimension, not an identical
     * coordinate in another world. Rat leg 1 is a two-world handoff. */
    static boolean activityCheckpoint(RescueChapter.Phase state,int ratLeg){
        return switch(state){
            case THEATRE_SEARCH,WRAPPER_REACH,THEATRE_GRAB,THEATRE_FIGHT,THEATRE_EYE,
                 BOB_E,SWARM_TRANSFORM,SWARM_FIGHT -> true;
            case RAT_CHASE -> ratLeg<2;
            default -> false;
        };
    }
    static boolean stageDimensionReady(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state){
        if(state==THEATRE_TRAVEL||state==CIRCUS_TRAVEL)return true;
        if(state==RAT_CHASE&&RescueChapter.data(s).getInt("RatLeg")==1)
            return OtherWorld.isOther(p.level())||ActivityWorld.isActivity(p.level());
        var expected=p.server.getLevel(activityCheckpoint(state,RescueChapter.data(s).getInt("RatLeg"))
                ?ActivityWorld.KEY:OtherWorld.KEY);
        return expected!=null&&p.serverLevel()==expected;
    }
    /** A noncombat fall or offworld reload must not refill boss health.
     * Restore the authentic stage without touching eyes, Cat, rat or actor IDs. */
    static boolean recoverNonCombat(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state){
        if(!RescueChapter.owns(p,s)||RescueBosses.fighting(state)||state==RAT_CHASE||state==COLLAPSE)return false;
        var n=RescueChapter.data(s);
        var level=p.server.getLevel(activityCheckpoint(state,n.getInt("RatLeg"))?ActivityWorld.KEY:OtherWorld.KEY);
        if(level==null){n.putString("Blocker","Waiting for the original rescue stage to load.");return false;}
        BlockPos anchor;
        switch(state){
            case THEATRE_SEARCH,WRAPPER_REACH,THEATRE_GRAB,THEATRE_EYE -> {
                if(!TheatreSite.prepare(p,s))return false;
                anchor=TheatreSite.entrance(p,s);
            }
            case BOB_E,SWARM_TRANSFORM -> {
                if(!MouseCircusSite.prepare(p,s))return false;
                anchor=MouseCircusSite.audience(p,s);
            }
            case ATTIC_SEARCH,BOB_A,BOB_B,BOB_C,FURNACE,BOB_D,BOB_TENT,CIRCUS_TRAVEL -> {
                if(!MouseCircusSite.prepareAttic(p,s))return false;
                var tent=MouseCircusSite.tent(p,s);
                if(tent==null){n.putString("Blocker","Waiting for the original attic tent.");return false;}
                anchor=tent.south(4);
            }
            case ATTIC_TRAVEL,CLOTHES -> {
                anchor=MouseCircusSite.atticDoor(p,s);
                if(anchor==null){n.putString("Blocker","Waiting for the original attic doorway.");return false;}
            }
            case EXTERIOR -> anchor=FirstEscapeScene.porch(s).south(8);
            case HOUSE_SAFE -> anchor=FirstEscapeScene.porch(s);
            case ROOM_LOCKED -> anchor=FirstEscapeScene.room(s);
            default -> anchor=garden(p,s);
        }
        if(!RescueChapter.transfer(p,level,anchor)){
            n.putString("Blocker","Clear a safe landing at the original rescue checkpoint; no boss or quest progress was reset.");
            return false;
        }
        p.setHealth(p.getMaxHealth());p.clearFire();p.fallDistance=0;
        n.remove("Blocker");
        RescueChapter.go(p,s,state);
        return true;
    }
    static boolean seeing(ServerPlayer p){var s=OtherMotherLoop.scene(p);return s!=null&&p.isUsingItem()&&p.getUseItem().is(com.coraline.registry.ModItems.SEEING_STONE.get());}
    static boolean gaze(ServerPlayer p,Vec3 at,double range,double dot){var delta=at.subtract(p.getEyePosition());return delta.lengthSqr()<range*range&&delta.lengthSqr()>.01&&p.getLookAngle().dot(delta.normalize())>dot&&p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),at,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    static boolean reveal(ServerPlayer p,VisitData.Scene s,int index,Vec3 at){if(seeing(p)&&gaze(p,at,48,.985)){RescueEyes.eye(s,index).putBoolean("Discovered",true);return true;}return false;}
    static CinematicPropEntity prop(ServerPlayer p,VisitData.Scene s,String key,int kind,int slot,Vec3 at){var e=RescueProps.ensure(p,s,key,kind,slot,at);if(e!=null){e.setInvisible(false);LoopProps.hold(p.serverLevel(),e.blockPosition());}return e;}
    static boolean presentationReady(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase phase){
        if(phase!=BOB_A&&phase!=BOB_B&&phase!=BOB_C&&phase!=BOB_D&&phase!=BOB_E)return true;
        var level=p.server.getLevel(phase==BOB_E?ActivityWorld.KEY:OtherWorld.KEY);
        if(level==null||p.serverLevel()!=level)return false;
        var tent=MouseCircusSite.tent(p,s);
        if(phase!=BOB_E&&tent==null)return false;
        var at=phase==BOB_E?Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)).add(0,2,0):Vec3.atBottomCenterOf(tent.south(6));
        var bob=ShowActors.acquire(p,s,"evil_other_bobinsky",level,at);
        if(bob==null||bob.level()!=level){
            var n=RescueChapter.data(s);
            if(n.getBoolean("Audio")){ShowPlayback.stop(p,s);n.remove("Audio");n.remove("AudioTail");}
            n.putString("Blocker","Waiting for the original Bobinsky before starting his dialogue.");
            return false;
        }
        return true;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state,int t,boolean done){
        var n=RescueChapter.data(s);var mother=ParentRescueStory.mother(p,s);if(mother!=null&&state.ordinal()<HOUSE_SAFE.ordinal()){mother.setInvisible(true);mother.getNavigation().stop();}
        if(n.getBoolean("GardenDead")&&!n.getBoolean("GardenDecayDone")&&p.serverLevel()==p.server.getLevel(OtherWorld.KEY)){int cursor=OtherWorldEstate.decayRescueGarden(p.serverLevel(),house(s),n.getInt("GardenDecayCursor"),192,(at,block)->FirstEscapeScene.place(p,s,at,block));if(cursor<0)n.putBoolean("GardenDecayDone",true);else n.putInt("GardenDecayCursor",cursor);}
        switch(state){
            case GARDEN_SEARCH->{
                RescueChapter.hint(p,p.distanceToSqr(garden(p,s).getCenter())>28*28?"Go to the garden.":"Search the garden through the Seeing Stone for a glowing eye.");
                if(p.level()!=p.server.getLevel(OtherWorld.KEY))return;
                Vec3 at=Vec3.atBottomCenterOf(garden(p,s)).add(4,1.5,0);var eye=prop(p,s,"GardenEye",RescueProps.EYE,0,at);RescueProps.frame(eye,0,t,1.65F);
                if(reveal(p,s,0,at)&&p.distanceToSqr(at)<100){RescueChapter.go(p,s,MANTIS_INTRO);}
            }
            case MANTIS_INTRO->{RescueProps.hide(p,s,"GardenEye");RescueBosses.mantis(p,s,t,0);if(done){RescueBosses.begin(p,s,MANTIS_FIGHT);}}
            case MANTIS_FIGHT,THEATRE_FIGHT,SWARM_FIGHT->RescueBosses.tick(p,s,t);
            case MANTIS_RELEASE->{RescueEyes.eye(s,0).putBoolean("Defeated",true);RescueEyes.eye(s,0).putBoolean("Discovered",true);RescueBosses.mantis(p,s,t,1);if(t>=60){n.putInt("RetrievalLimit",Math.max(240,ShowRecordings.duration("rescue_mantis_take")+80));RescueChapter.go(p,s,MANTIS_TAKE);}}
            case MANTIS_TAKE->{RescueBosses.mantis(p,s,0,2);RescueChapter.hint(p,"Take the ghost eye from Other Father's outstretched hand!");if(done){n.putInt("SinkingElapsed",t);RescueChapter.go(p,s,MANTIS_SINK);}}
            case MANTIS_SINK->{
                boolean claimed=RescueEyes.eye(s,0).getBoolean("Claimed");int sink=n.getInt("SinkingAfterClaim");if(claimed)n.putInt("SinkingAfterClaim",++sink);
                RescueBosses.mantis(p,s,claimed?ShowRecordings.duration("rescue_mantis_take")+sink:0,2);
                if(!claimed)RescueChapter.hint(p,"Right-click the ghost eye in Other Father's outstretched hand.");
                if(claimed&&sink>=80){RescueProps.hide(p,s,"Mantis");RescueProps.hide(p,s,"GardenEye");var dad=ShowActors.actor(p,s,"evil_other_father");if(dad!=null)dad.setInvisible(true);RescueEyes.startDrain(p,s,0);RescueChapter.go(p,s,MANTIS_GONE);}
            }
            case MANTIS_GONE->{if(t>=20){n.putBoolean("GardenDead",true);}if(done){RescueChapter.go(p,s,THEATRE_TRAVEL);}}
            case THEATRE_TRAVEL->{
                RescueChapter.hint(p,"Find the next ghost eye in the basement theatre.");
                ShowHouseAccess.open(p,s,"basement");
                // A completed physical transfer can be saved one tick BEFORE
                // the scene phase. Resume inside the original venue instead of
                // requiring the owner to re-enter from a different dimension.
                if((TheatreSite.inside(p,s)||TheatreSite.atEntrance(p,s))&&TheatreSite.enter(p,s)){
                    abandonTheatre(p,s);RescueChapter.go(p,s,THEATRE_SEARCH);
                }else if(ActivityWorld.isActivity(p.level())&&!TheatreSite.inside(p,s))
                    recoverNonCombat(p,s,THEATRE_TRAVEL);
            }
            case THEATRE_SEARCH->{Vec3 at=Vec3.atBottomCenterOf(TheatreSite.center(p,s).north(28).above());var wrapper=prop(p,s,"Wrapper",RescueProps.WRAPPER,0,at);RescueProps.frame(wrapper,0,t,0);var eye=prop(p,s,"TheatreEye",RescueProps.EYE,1,at.add(0,.7,0));RescueProps.frame(eye,0,t,1.65F);reveal(p,s,1,at.add(0,.7,0));RescueChapter.hint(p,RescueEyes.eye(s,1).getBoolean("Discovered")?"Eye found! Lower the stone and right-click the wrapper to pull it open.":"Use the Seeing Stone to search the empty stage.");}
            case WRAPPER_REACH->{var wrapper=RescueProps.find(p,s,"Wrapper");RescueProps.frame(wrapper,1,t,Math.min(1,t/30F));RescueChapter.hint(p,"Right-click the eye again to pull it free.");}
            case THEATRE_GRAB->{RescueBosses.actresses(p,s,t,true);if(done){p.setDeltaMovement(Vec3.ZERO);RescueBosses.begin(p,s,THEATRE_FIGHT);}}
            case THEATRE_EYE->{RescueBosses.actresses(p,s,t,true);var eye=prop(p,s,"TheatreEye",RescueProps.EYE,1,center(p,s).add(0,1.7,-2.2));RescueProps.frame(eye,1,t,1);RescueChapter.hint(p,"Take the ghost eye, then leave through the theatre entrance.");if(RescueEyes.eye(s,1).getBoolean("Claimed")&&TheatreSite.atExit(p,s)&&TheatreSite.exit(p,s)){RescueChapter.go(p,s,ATTIC_TRAVEL);}}
            case ATTIC_TRAVEL->{
                RescueChapter.hint(p,"Go to the attic.");ShowHouseAccess.open(p,s,"attic");var at=Vec3.atBottomCenterOf(FirstEscapeScene.at(s,8,16,51));var clothes=prop(p,s,"Clothes",RescueProps.CLOTHES,0,at);
                if(!n.getBoolean("ClothesSeen")&&gaze(p,at.add(0,1,0),22,.6)){n.putBoolean("ClothesSeen",true);RescueChapter.go(p,s,CLOTHES);}else if(n.getBoolean("ClothesSeen")&&MouseCircusSite.inAttic(p,s))enterAttic(p,s);
            }
            case CLOTHES->{if(done){if(MouseCircusSite.inAttic(p,s))enterAttic(p,s);else RescueChapter.go(p,s,ATTIC_TRAVEL);}}
            case ATTIC_SEARCH->{RescueChapter.hint(p,"Search the attic for the final ghost eye.");if(t>40)RescueChapter.go(p,s,BOB_A);}
            case BOB_A,BOB_B,BOB_C,FURNACE,BOB_D,BOB_TENT->{if(!bobinsky(p,s,state,t))return;if(state==FURNACE&&t>=60)RescueChapter.go(p,s,BOB_D);else if(state==BOB_TENT&&t>=60)RescueChapter.go(p,s,CIRCUS_TRAVEL);else if(done)RescueChapter.go(p,s,switch(state){case BOB_A->BOB_B;case BOB_B->BOB_C;case BOB_C->FURNACE;case BOB_D->BOB_TENT;default->CIRCUS_TRAVEL;});}
            case CIRCUS_TRAVEL->{RescueChapter.hint(p,"Follow Bobinsky into the circus tent.");var tent=MouseCircusSite.tent(p,s);var activity=p.server.getLevel(ActivityWorld.KEY);boolean alreadyInside=MouseCircusSite.inside(p,s);
                // Interrupted player transfer without a committed circus
                // arrival: recover to the ORIGINAL attic tent, not a bogus
                // forever-pending Activity World checkpoint.
                if(ActivityWorld.isActivity(p.level())&&!alreadyInside){recoverNonCombat(p,s,CIRCUS_TRAVEL);return;}
                if(tent!=null&&activity!=null&&(alreadyInside||MouseCircusSite.inAttic(p,s)&&p.distanceToSqr(tent.getCenter())<9)){
                    // Prepare Bobinsky first. His original actor lease and the
                    // player's dimension entry can then independently retry;
                    // the phase commits only after both are verified present.
                    var bob=ShowActors.acquire(p,s,"evil_other_bobinsky",activity,Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)).add(0,2,0));
                    if(bob==null){n.putString("Blocker","Bobinsky's saved actor is unavailable; the circus entry will retry without creating a duplicate.");return;}
                    if(!alreadyInside&&!MouseCircusSite.enter(p,s)){n.putString("Blocker","The circus transfer is pending; follow the tent and try again.");return;}
                    var actorAt=Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)).add(0,2,0);
                    if(bob.level()==activity)ShowActors.position(bob,actorAt,0,7,0,1);
                    if(MouseCircusSite.inside(p,s)&&bob.level()==activity&&bob.distanceToSqr(actorAt)<.25){prop(p,s,"Cheese",RescueProps.CHEESE,0,Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)));n.remove("Blocker");RescueChapter.go(p,s,BOB_E);}
                    else n.putString("Blocker","Waiting for both Coraline and Bobinsky to reach the circus.");
                }}
            case BOB_E->{
                var bob=ShowActors.actor(p,s,"evil_other_bobinsky");
                if(bob==null||bob.level()!=p.level()||!MouseCircusSite.inside(p,s))return;
                bob.setInvisible(false);
                ShowActors.position(bob,Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)).add(0,2,0),0,7,t,1);
                if(done)RescueChapter.go(p,s,SWARM_TRANSFORM);
            }
            case SWARM_TRANSFORM->{
                var bob=ShowActors.actor(p,s,"evil_other_bobinsky");
                var anchor=center(p,s);
                var swarm=bob==null||bob.level()!=p.level()||!MouseCircusSite.inside(p,s)?null:prop(p,s,"Swarm",RescueProps.SWARM,0,anchor);
                if(swarm==null||swarm.level()!=p.level()){
                    n.putInt("Ticks",0);
                    n.putString("Blocker","Waiting for the original Bobinsky and the Ratmaster swarm.");
                    return;
                }
                swarm.setPos(anchor);
                float reveal=Math.min(1,t/60F);
                RescueProps.frame(swarm,5,t,reveal);
                ShowActors.position(bob,anchor.add(0,2,0),0,80,t,1-reveal);
                if(t>=55)bob.setInvisible(true);
                if(t>=60)RescueBosses.begin(p,s,SWARM_FIGHT);
            }
            case RAT_CHASE->chase(p,s,t);
            case EXTERIOR->{unlockAttic(p,s);RescueChapter.hint(p,"Get outside. Something is happening to the Other World.");if(p.level()==p.server.getLevel(OtherWorld.KEY)&&OtherWorldEstate.outsideHouse(house(s),p.blockPosition())&&RescueEyes.all(s)){n.putLong("CollapseStart",p.blockPosition().asLong());n.putInt("CollapseDuration",1500);n.putInt("CollapseElapsed",0);n.putDouble("CollapseInitialRadius",Math.max(130,p.position().distanceTo(Vec3.atBottomCenterOf(house(s)))+70));RescueChapter.go(p,s,COLLAPSE);}}
            case COLLAPSE->collapse(p,s,t);
            case HOUSE_SAFE->{
                // The room geometry has the same coordinates in every realm.
                // Never move the canonical Cat or lock the room from a stale
                // Activity/Overworld checkpoint after disconnect or death.
                if(!OtherWorld.isOther(p.level())){
                    n.putString("Blocker","Return through your original Little Door to the Other World room.");
                    RescueChapter.hint(p,n.getString("Blocker"));
                    return;
                }
                n.remove("Blocker");
                RescueChapter.hint(p,"Follow the green light to the Little Door room.");greenHouse(p,s);carryCat(p,s);if(FirstEscapeScene.fullyInRoom(s,p.getBoundingBox())&&!p.getBoundingBox().intersects(new AABB(FirstEscapeScene.entrance(s)).inflate(.1,1,.1))){
                    // Do not seal the player into the confrontation until the
                    // original Cat has actually reached the mantel. A failed
                    // entity lease/transfer must remain a recoverable handoff.
                    if(BeldamFinale.catAtMantel(p,s)==null){
                        n.putString("Blocker","Waiting for the original Cat to return before sealing the confrontation room.");
                        RescueChapter.hint(p,n.getString("Blocker"));
                        return;
                    }
                    n.remove("Blocker");
                    closeRoom(p,s);n.putBoolean("RoomLocked",true);RescueChapter.go(p,s,ROOM_LOCKED);
                }}
            case ROOM_LOCKED->{
                // Old saves may already have committed the lock before the
                // original Cat was staged. Never advance that checkpoint blind.
                if(!OtherWorld.isOther(p.level())){
                    n.putString("Blocker","Return to the original Other World confrontation room.");
                    RescueChapter.hint(p,n.getString("Blocker"));
                    return;
                }
                boolean objectives=RescueEyes.all(s)&&n.getBoolean("CatRescued")&&n.getBoolean("HouseSafe");
                var cat=objectives?BeldamFinale.catAtMantel(p,s):null;
                if(!objectives||cat==null){
                    // Reopen the SAME room's real doors, not an invented exit.
                    // Keep the existing milestone, actor UUIDs and eye receipts.
                    n.putBoolean("RoomLocked",false);
                    FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s),true);
                    FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s).south(),true);
                    n.putString("Blocker",objectives
                            ?"Waiting for the original Cat to return before the confrontation."
                            :"The Cat rescue and three original ghost eyes must be restored before the confrontation.");
                    RescueChapter.hint(p,n.getString("Blocker"));
                    return;
                }
                if(!FirstEscapeScene.fullyInRoom(s,p.getBoundingBox())){
                    n.putBoolean("RoomLocked",false);
                    FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s),true);
                    FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s).south(),true);
                    n.putString("Blocker","Enter the Little Door room to resume the confrontation.");
                    RescueChapter.hint(p,n.getString("Blocker"));
                    return;
                }
                n.remove("Blocker");
                closeRoom(p,s);
                n.putBoolean("RoomLocked",true);
                RescueChapter.go(p,s,FINAL_A);
            }
            default->{}
        }
        if(state!=COLLAPSE&&t%20==0&&p.getY()<p.level().getMinBuildHeight()+5)retry(p,s,state);
    }
    private static void abandonTheatre(ServerPlayer p,VisitData.Scene s){for(var e:p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(TheatreSite.center(p,s)).inflate(90),e->e.owned(s))){if(e.kind()==CinematicPropEntity.DOG_ROW)e.frame(0,0,-1);else if(e.kind()==CinematicPropEntity.CURTAIN)e.frame(0,0,1);else if(e.kind()==CinematicPropEntity.OCEAN_SET||e.kind()==CinematicPropEntity.DIVING||e.kind()==CinematicPropEntity.TRAPEZE)e.setInvisible(true);}RescueChapter.data(s).putBoolean("AbandonedTheatre",true);}
    private static void enterAttic(ServerPlayer p,VisitData.Scene s){if(!MouseCircusSite.prepareAttic(p,s)||!MouseCircusSite.inAttic(p,s))return;var room=MouseCircusSite.attic();var min=room.min();var max=room.max();var bounds=new AABB(FirstEscapeScene.at(s,min[0],min[1],min[2]),FirstEscapeScene.at(s,max[0]+1,max[1]+1,max[2]+1));var body=p.getBoundingBox();var door=MouseCircusSite.atticDoor(p,s);if(door!=null&&bounds.contains(new Vec3(body.minX+.001,body.minY+.001,body.minZ+.001))&&bounds.contains(new Vec3(body.maxX-.001,body.maxY-.001,body.maxZ-.001))&&!body.intersects(new AABB(door).inflate(.1,1,.1))){FirstEscapeStory.setDoor(p,door,false);RescueChapter.data(s).putBoolean("AtticLocked",true);RescueChapter.go(p,s,ATTIC_SEARCH);}}
    private static void unlockAttic(ServerPlayer p,VisitData.Scene s){if(p.level()==p.server.getLevel(OtherWorld.KEY)){var door=MouseCircusSite.atticDoor(p,s);if(door!=null)FirstEscapeStory.setDoor(p,door,true);}RescueChapter.data(s).putBoolean("AtticLocked",false);}
    private static boolean bobinsky(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state,int t){
        var tent=MouseCircusSite.tent(p,s);if(tent==null)return false;var base=Vec3.atBottomCenterOf(tent.south(6));var chamber=base.add(5,0,-1);var front=base.subtract(chamber).normalize();var mouth=chamber.add(front.scale(1.4));var furnace=prop(p,s,"Furnace",RescueProps.FURNACE,0,chamber);float open=state==BOB_C?Math.max(0,Math.min(1,(t-ShowRecordings.duration("rescue_bobinsky_c")+30)/20F)):state==FURNACE?1-Math.max(0,Math.min(1,(t-42)/18F)):0;if(furnace!=null){furnace.setYRot((float)Math.toDegrees(Math.atan2(front.x,-front.z)));RescueProps.frame(furnace,state==FURNACE?1:0,t,open);}
        var bob=ShowActors.acquire(p,s,"evil_other_bobinsky",p.serverLevel(),base);if(bob==null||furnace==null)return false;
        double u=Math.min(1,t/(double)Math.max(1,ShowRecordings.duration(RescueChapter.sound(state))));Vec3 at=base.add(Math.sin(t*.06)*.4,0,0);
        if(state==BOB_C)at=base.lerp(mouth,u);if(state==FURNACE){double crawl=Math.max(0,Math.min(1,(t-8)/30.));crawl=crawl*crawl*(3-2*crawl);at=mouth.lerp(chamber.subtract(front.scale(.15)),crawl);}if(state==BOB_D){if(t<160){bob.setInvisible(true);return false;}var anchor=roofAnchor(p,s,bob,base);if(anchor==null){bob.setInvisible(true);RescueChapter.data(s).putString("Blocker","The authored roof beam for Bobinsky's swing is obstructed or missing.");RescueChapter.data(s).putInt("BobSwingTicks",0);return false;}double swing=Math.sin((t-160)*.05)*1.0;at=anchor.add(Math.sin(swing)*3,-Math.cos(swing)*3-2.5,0);var rig=prop(p,s,"BobRoofSwing",RescueProps.ROOF_SWING,0,anchor);if(rig==null)return false;rig.setPos(anchor);rig.frame(0,t-160,1);}else RescueProps.hide(p,s,"BobRoofSwing");if(state==BOB_TENT)at=base.lerp(Vec3.atBottomCenterOf(tent),Math.min(1,t/60.0));
        var saved=RescueChapter.data(s);if(state==BOB_D){saved.putDouble("BobSwingX",at.x);saved.putDouble("BobSwingY",at.y);saved.putDouble("BobSwingZ",at.z);}else if(state==BOB_TENT&&saved.contains("BobSwingX")){var from=new Vec3(saved.getDouble("BobSwingX"),saved.getDouble("BobSwingY"),saved.getDouble("BobSwingZ"));double descend=Math.min(1,t/30.);descend=descend*descend*(3-2*descend);at=t<30?from.lerp(base,descend):base.lerp(Vec3.atBottomCenterOf(tent),Math.min(1,(t-30)/30.));}
        bob.setInvisible(state==FURNACE&&t>=42||state==BOB_TENT&&t>58);ShowActors.position(bob,at,(float)Math.toDegrees(Math.atan2(p.getX()-at.x,at.z-p.getZ())),state==BOB_D?86:state==BOB_A||state==BOB_C||state==FURNACE||state==BOB_TENT?85:state==BOB_B?82:81,state==BOB_D?t-160:t,1);
        var eye=prop(p,s,"BobEye",RescueProps.EYE,2,at.add(0,1.2,-.8));if(eye!=null){eye.setPos(at.add(0,1.2,-.8));eye.setInvisible(state!=BOB_B);eye.frame(1,t,1);}if(state==BOB_B)RescueEyes.eye(s,2).putBoolean("Discovered",true);
        if(state==FURNACE&&t>=24&&t<=50&&t%4==0){var steam=chamber.add(front.scale(.95));p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.CLOUD,true,steam.x,steam.y+1,steam.z,4,.4,.5,.4,.01);if(t==24)p.serverLevel().playSound(null,BlockPos.containing(chamber),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.35F,.65F);}
        if(state!=BOB_D)return true;
        saved.remove("Blocker");
        int swingTicks=Math.min(24,saved.getInt("BobSwingTicks")+1);
        saved.putInt("BobSwingTicks",swingTicks);
        return swingTicks>=24;
    }
    private static Vec3 roofAnchor(ServerPlayer p,VisitData.Scene s,Mob bob,Vec3 base){
        var n=RescueChapter.data(s);
        var level=p.serverLevel();
        var column=BlockPos.containing(base);
        if(n.contains("BobRoofAnchor")){
            var saved=BlockPos.of(n.getLong("BobRoofAnchor"));
            if(roofClear(level,bob,saved))return Vec3.atBottomCenterOf(saved);
        }
        for(int radius=0;radius<=3;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int y=5;y<=15;y++){
                var cell=column.offset(dx,y,dz);
                if(!roofClear(level,bob,cell))continue;
                n.putLong("BobRoofAnchor",cell.asLong());
                n.putInt("BobSwingTicks",0);
                return Vec3.atBottomCenterOf(cell);
            }
        }
        return null;
    }
    private static boolean roofClear(ServerLevel level,Mob bob,BlockPos pos){
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isFaceSturdy(level,pos,Direction.DOWN))return false;
        var anchor=Vec3.atBottomCenterOf(pos);
        for(double angle:new double[]{-1,-.67,-.33,0,.33,.67,1}){
            var at=anchor.add(Math.sin(angle)*3,-Math.cos(angle)*3-2.5,0);
            if(!PalaceWellMotion.clear(level,bob,at))return false;
        }
        return true;
    }
    static void startChase(ServerPlayer p,VisitData.Scene s){var n=RescueChapter.data(s);n.putInt("RatLeg",0);n.putInt("RatNode",0);n.putInt("RatStuck",0);n.remove("RatPathTarget");n.remove("RatPath");n.remove("RatPathNode");n.remove("RatExitWait");RescueChapter.go(p,s,RAT_CHASE);}
private static BlockPos[] chaseHouseRoute(VisitData.Scene s){
        return new BlockPos[]{FirstEscapeScene.at(s,7,24,60),FirstEscapeScene.at(s,7,15,52),
                FirstEscapeScene.at(s,7,15,18),FirstEscapeScene.at(s,20,15,10),
                FirstEscapeScene.at(s,26,6,10),FirstEscapeScene.at(s,26,4,8),
                FirstEscapeScene.at(s,26,2,4),FirstEscapeScene.at(s,26,0,0),
                FirstEscapeScene.at(s,26,-1,11)};
    }
    /** Recover the same saved rat even when older builds saved its pre-transfer
     * circus coordinates after committing the attic return. Only original
     * route chunks are loaded, and an absent UUID is never replaced. */
    static com.coraline.otherworld.MouseEntity chaseRat(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        if(n.hasUUID("ChaseRat")){
            if(n.contains("RatAt")){
                var savedWorld=com.coraline.parents.FamilyData.level(p.server,n.getString("RatDimension"));
                if(savedWorld!=null)savedWorld.getChunkAt(BlockPos.of(n.getLong("RatAt")));
            }
            if(n.getInt("RatLeg")>=2
                    &&!OtherWorld.KEY.location().toString().equals(n.getString("RatDimension"))
                    &&p.tickCount%40==0){
                var savedWorld=p.server.getLevel(OtherWorld.KEY);
                if(savedWorld!=null&&ParentRescueStory.scenery(s).contains("House",net.minecraft.nbt.Tag.TAG_LONG)){
                    var tent=MouseCircusSite.tent(p,s);
                    if(tent!=null)LoopProps.hold(savedWorld,tent.south(3));
                    for(var waypoint:chaseHouseRoute(s))LoopProps.hold(savedWorld,waypoint);
                }
            }
            for(var level:p.server.getAllLevels())
                if(level.getEntity(n.getUUID("ChaseRat")) instanceof com.coraline.otherworld.MouseEntity rat
                        &&rat.getPersistentData().hasUUID("RescueOwner")
                        &&s.owner.equals(rat.getPersistentData().getUUID("RescueOwner")))
                    return rat;
            return null;
        }
        var rat=ModEntities.MOUSE.get().create(p.serverLevel());if(rat==null)return null;rat.doorId=s.door;rat.circusStyle(6);rat.catPose(0,1);rat.spying(true);rat.setInvulnerable(true);rat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(80);rat.getPersistentData().putBoolean("CoralineDecoration",true);rat.getPersistentData().putUUID("RescueOwner",s.owner);rat.setPos(Vec3.atBottomCenterOf(MouseCircusSite.center(p,s).south(10)));if(!p.serverLevel().addFreshEntity(rat))return null;n.putUUID("ChaseRat",rat.getUUID());return rat;}
    static void recordChaseRat(net.minecraft.nbt.CompoundTag state,com.coraline.otherworld.MouseEntity rat){
        state.putLong("RatAt",rat.blockPosition().asLong());
        state.putString("RatDimension",rat.level().dimension().location().toString());
    }
    private static void chase(ServerPlayer p,VisitData.Scene s,int t){
        var n=RescueChapter.data(s);
        if(n.getBoolean("RatResetPending")){resetRatSegment(p,s);return;}
        RescueChapter.hint(p,"Catch the rat on the circus ball! Right-click it to grab the eye.");var rat=chaseRat(p,s);if(rat==null){n.putString("Blocker","Waiting for the saved chase rat; no duplicate created.");return;}int leg=n.getInt("RatLeg");
        if(leg==0){var exit=MouseCircusSite.exit(p,s);rollRat(p,s,rat,exit,t,leg);if(rat.distanceToSqr(exit.getCenter())<9){n.putInt("RatLeg",1);rat.setNoAi(true);}}
        else if(leg==1){if(rat.getVehicle()!=null)rat.getVehicle().setDeltaMovement(Vec3.ZERO);
            // The player and rat use separate reversible travel leases. If the
            // player returns but the mob transfer fails/unloads, retry the mob
            // on later ticks from the attic instead of leaving the chase stuck
            // forever on the circus leg.
            boolean returned=MouseCircusSite.inAttic(p,s);
            if(!returned&&MouseCircusSite.inside(p,s)&&p.distanceToSqr(MouseCircusSite.exit(p,s).getCenter())<16)
                returned=MouseCircusSite.returnToAttic(p,s);
            if(returned){var tent=MouseCircusSite.tent(p,s);if(tent!=null){var moved=ShowTravel.guest(rat,p,s,p.serverLevel(),Vec3.atBottomCenterOf(tent.south(3)),true,"RescueRatReturn");if(moved instanceof com.coraline.otherworld.MouseEntity returnedRat&&rat.getUUID().equals(returnedRat.getUUID())&&returnedRat.level()==p.level()){rat=returnedRat;
                    n.putInt("RatLeg",2);
                    n.putLong("RatHouseStart",moved.blockPosition().asLong());
                    n.remove("RatHouseMoved");n.remove("RatPathTarget");n.remove("Blocker");retireRatBall(p,s,0);unlockAttic(p,s);}else n.putString("Blocker","Rat return is pending; stay in the attic and the transfer will retry.");}}
        }
        else {ShowHouseAccess.open(p,s,"basement");BlockPos[] route=chaseHouseRoute(s);int index=Math.min(route.length-1,n.getInt("RatNode"));rollRat(p,s,rat,route[index],t,leg);if(rat.distanceToSqr(route[index].getCenter())<5){if(index<route.length-1)n.putInt("RatNode",index+1);else{if(rat.getVehicle()!=null)rat.getVehicle().setDeltaMovement(Vec3.ZERO);n.putInt("RatExitWait",n.getInt("RatExitWait")+1);if(n.getInt("RatExitWait")>100){retry(p,s,RAT_CHASE);return;}}}
            if(index<3){var at=Vec3.atBottomCenterOf(route[index]).add(0,0,2);var obstacles=prop(p,s,"ChaseObstacles",RescueProps.SWARM,0,at);if(obstacles!=null){obstacles.setPos(at);obstacles.frame(6,t,1);}if(p.distanceToSqr(at)<4)p.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(),new Vec3(.75,1,.75));}else RescueProps.hide(p,s,"ChaseObstacles");
        }
        recordChaseRat(n,rat);LoopProps.hold((ServerLevel)rat.level(),rat.blockPosition());
        if(leg>=2&&rat.getVehicle() instanceof CinematicPropEntity ball&&ball.kind()==RescueProps.RAT_BALL){
            if(!n.contains("RatHouseStart"))n.putLong("RatHouseStart",ball.blockPosition().asLong());
            if(ball.blockPosition().distSqr(BlockPos.of(n.getLong("RatHouseStart")))>=9)n.putBoolean("RatHouseMoved",true);
        }
    }
    static void rollRat(ServerPlayer p,VisitData.Scene s,com.coraline.otherworld.MouseEntity rat,BlockPos target,int t,int leg){
        var n=RescueChapter.data(s);
        if(rat.level()!=p.level())return;
        var ball=RescueProps.ensure(p,s,"RatBall"+leg,RescueProps.RAT_BALL,0,rat.position());
        if(ball==null){
            if(retireRatBall(p,s,leg))clearRatPath(n);
            else n.putString("Blocker","Waiting for the original circus ball or its recorded chunk.");
            return;
        }
        if(leg>=2&&n.getBoolean("RatHouseMoved")==false&&!n.contains("RatHouseStart"))n.putLong("RatHouseStart",ball.blockPosition().asLong());ball.setInvisible(false);rat.setNoAi(true);rat.setNoGravity(true);rat.noPhysics=false;if(rat.getBbWidth()<.59F)rat.refreshDimensions();
        if(!n.contains("RatPathTarget")||n.getLong("RatPathTarget")!=target.asLong()){
            rat.stopRiding();rat.setNoGravity(false);rat.setOnGround(true);if(rat.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}var path=rat.rescueBallPath(target,ball.position());rat.getNavigation().stop();rat.setNoGravity(true);
            if(path==null||!path.canReach()){n.putString("Blocker","The physical rat-ball route is obstructed at "+target.toShortString()+"; no wall teleport used.");rat.startRiding(ball,true);return;}
            var nodes=new long[path.getNodeCount()];for(int i=0;i<nodes.length;i++){var node=path.getNode(i);nodes[i]=new BlockPos(node.x,node.y,node.z).asLong();}n.putLongArray("RatPath",nodes);n.putInt("RatPathNode",0);n.putLong("RatPathTarget",target.asLong());n.remove("Blocker");
        }
        if(rat.getVehicle()!=ball&&!rat.startRiding(ball,true))return;var route=n.getLongArray("RatPath");int index=n.getInt("RatPathNode");
        if(index>=route.length){ball.setDeltaMovement(0,ball.getDeltaMovement().y,0);return;}
        var next=BlockPos.of(route[index]);var footing=ratFeet(p.serverLevel(),next);if(ball.position().distanceToSqr(footing)<.25){n.putInt("RatPathNode",index+1);return;}
        var block=p.serverLevel().getBlockState(next);if(block.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)FirstEscapeStory.setDoor(p,next,true);
        if(n.contains("RatBallLast")&&n.getLong("RatBallLast")==ball.blockPosition().asLong()
                &&ball.getDeltaMovement().horizontalDistanceSqr()<.003){
            int stalled=n.getInt("RatStuck")+1;
            n.putInt("RatStuck",stalled);
            if(stalled>=20){clearRatPath(n);return;}
        }else n.putInt("RatStuck",0);
        n.putLong("RatBallLast",ball.blockPosition().asLong());
        var direction=footing.subtract(ball.position()).multiply(1,0,1);double speed=leg>=2&&!n.getBoolean("RatHouseMoved")?.19:p.distanceToSqr(rat)>24*24?.075:.145;ball.setDeltaMovement(direction.normalize().scale(speed).add(0,ball.getDeltaMovement().y,0));ball.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));ball.frame(leg,t,1);ParentRescueStory.scenery(s).putLong("RescueRatBall"+leg+"Position",ball.blockPosition().asLong());
    }
    static Vec3 ratFeet(ServerLevel level,BlockPos node){
        // Navigation's integer node can contain a carpet, slab or 3/4-height
        // table. Comparing its integer Y to the physical ball stalls forever
        // above those supports; use the actual collision top, without moving it.
        double top=node.getY();for(var cell:java.util.List.of(node.below(),node))for(var shape:level.getBlockState(cell).getCollisionShape(level,cell).toAabbs()){
            var world=shape.move(cell);if(world.maxX>node.getX()+.2&&world.minX<node.getX()+.8&&world.maxZ>node.getZ()+.2&&world.minZ<node.getZ()+.8&&world.maxY<=node.getY()+1.001)top=Math.max(top,world.maxY);
        }return new Vec3(node.getX()+.5,top,node.getZ()+.5);
    }
    private static void collapse(ServerPlayer p,VisitData.Scene s,int t){
        if(restoreIncompleteCollapse(p,s))return;
        var n=RescueChapter.data(s);
        var level=p.server.getLevel(OtherWorld.KEY);
        if(level==null||p.serverLevel()!=level)return;
        var home=house(s);
        var porch=FirstEscapeScene.porch(s);
        var cat=ShowActors.recoverCat(p,s,level,Vec3.atBottomCenterOf(porch.south(12)));
        if(cat==null){
            n.putString("Blocker","Waiting for the original Cat. The collapse clock is paused.");
            return;
        }
        if(!n.getBoolean("CatRescued")){
            var safe=OtherWorld.safePlayer(level,porch.south(12));
            if(safe==null||!RescueEpilogue.safeReturn(level,p,Pose.STANDING,Vec3.atBottomCenterOf(safe))){
                n.putString("Blocker","Clear the Cat's escape checkpoint outside the porch.");
                return;
            }
            var at=Vec3.atBottomCenterOf(safe);
            // A saved CatCollapsePlaced flag can outlive the Cat's actual
            // position after a chunk/reload transfer. Re-stage that SAME
            // original actor only when lost or obstructed, never after pickup.
            boolean obstructed=!level.noCollision(cat,cat.getBoundingBox());
            if(RescueChallengeMath.restageUnrescuedCat(n.getBoolean("CatCollapsePlaced"),false,
                    cat.distanceToSqr(at),obstructed)){
                cat.stopRiding();
                cat.setDeltaMovement(Vec3.ZERO);
                cat.setPos(at);
                n.putBoolean("CatCollapsePlaced",true);
                n.putLong("CatCollapsePosition",cat.blockPosition().asLong());
                OtherMotherLoop.changed(p);
            }
            cat.setInvisible(false);
            cat.setNoAi(true);
            cat.setNoGravity(false);
            cat.noPhysics=false;
            cat.getLookControl().setLookAt(p,20,20);
            n.putInt("Cat",cat.getId());
            LoopProps.hold(level,cat.blockPosition());
        }
        n.remove("Blocker");
        int elapsed=n.contains("CollapseElapsed")?n.getInt("CollapseElapsed")+1:Math.max(0,t-1);
        n.putInt("CollapseElapsed",elapsed);
        double radius=RescueChallengeMath.collapse(elapsed,n.getInt("CollapseDuration"),n.getDouble("CollapseInitialRadius"),48);
        n.putDouble("CollapseRadius",radius);
        if(n.getBoolean("CatRescued"))carryCat(p,s);
        RescueChapter.hint(p,n.getBoolean("CatRescued")?"RUN TO THE HOUSE!":"RUN TO THE HOUSE — pick up the Cat along the way!");
        var body=p.getBoundingBox();boolean inside=body.minX>=home.getX()-34&&body.maxX<=home.getX()+34&&body.minZ>=home.getZ()-44&&body.maxZ<=home.getZ()+42;
        if(inside&&p.distanceToSqr(porch.getCenter())<196){if(n.getBoolean("CatRescued")){n.putBoolean("HouseSafe",true);n.putDouble("CollapseRadius",48);RescueChapter.go(p,s,HOUSE_SAFE);}else RescueChapter.hint(p,"You can't leave the Cat behind! Right-click the Cat outside.");}
        double distance=Math.hypot(p.getX()-home.getX(),p.getZ()-home.getZ());if(distance>radius+2||elapsed>=n.getInt("CollapseDuration")&&!n.getBoolean("HouseSafe"))retry(p,s,COLLAPSE);
    }
    private static void carryCat(ServerPlayer p,VisitData.Scene s){var cat=ShowActors.actor(p,s,"cat");if(cat==null)return;cat.stopRiding();cat.setNoAi(true);cat.setNoGravity(true);cat.setInvisible(false);cat.setDeltaMovement(Vec3.ZERO);cat.setPos(p.position().add(p.getLookAngle().multiply(.5,0,.5)).add(0,.8,0));RescueChapter.data(s).putInt("Cat",cat.getId());LoopProps.hold(p.serverLevel(),cat.blockPosition());}
    static boolean restoreHouseEntry(boolean savedReady,boolean firstDoorOpen,boolean secondDoorOpen){
        return !savedReady||!firstDoorOpen||!secondDoorOpen;
    }
    private static boolean entryDoorOpen(ServerLevel level,BlockPos pos){
        if(level==null)return false;
        var state=level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.OPEN);
    }
    private static void greenHouse(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);var entrance=FirstEscapeScene.entrance(s);
        var level=p.server.getLevel(OtherWorld.KEY);
        // The GreenHouse save flag is not proof that both original entry
        // doors survived a relog. Reopen only the real doors while HOUSE_SAFE.
        if(restoreHouseEntry(n.getBoolean("GreenHouse"),
                entryDoorOpen(level,entrance),entryDoorOpen(level,entrance.south()))){
            FirstEscapeStory.setDoor(p,entrance,true);
            FirstEscapeStory.setDoor(p,entrance.south(),true);
        }
        if(n.getBoolean("GreenHouse"))return;
        FirstEscapeScene.light(p,s,FirstEscapeScene.room(s).above(3),10);
        var m=ParentRescueStory.mother(p,s);if(m==null)return;
        m.stopRiding();m.setInvisible(false);m.setNoAi(true);
        m.setPos(Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)));
        m.lookAtStoryPlayer(p);n.putBoolean("GreenHouse",true);
    }
    static void closeRoom(ServerPlayer p,VisitData.Scene s){FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s),false);FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s).south(),false);}
    public static boolean takeOfferedEye(ServerPlayer p,VisitData.Scene s){
        var phase=RescueChapter.phase(s);if(phase!=MANTIS_TAKE&&phase!=MANTIS_SINK)return false;
        var offered=RescueProps.find(p,s,"GardenEye");if(offered==null||offered.level()!=p.level()||offered.isInvisible())return false;
        var at=offered.position();
        if(!new AABB(at.add(-.55,-.55,-.55),at.add(.55,.55,.55)).clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(5))).isPresent()||!gaze(p,at,5,.80))return false;
        RescueEyes.eye(s,0).putBoolean("Defeated",true);RescueEyes.eye(s,0).putBoolean("Discovered",true);
        if(RescueEyes.claim(p,s,0)){offered.setInvisible(true);RescueChapter.hint(p,"Garden ghost eye collected.");}return true;
    }
    public static boolean pullTheatreEye(ServerPlayer p,VisitData.Scene s){
        var phase=RescueChapter.phase(s);
        if(phase==THEATRE_EYE){var reward=RescueProps.find(p,s,"TheatreEye");if(reward==null||reward.level()!=p.level()||reward.isInvisible())return false;var at=reward.position();var eye=p.getEyePosition();var box=new AABB(at.add(-.65,-.65,-.65),at.add(.65,.65,.65));if(!box.contains(eye)&&box.clip(eye,eye.add(p.getLookAngle().scale(5))).isEmpty())return false;return RescueEyes.claim(p,s,1);}
        if(phase!=THEATRE_SEARCH&&phase!=WRAPPER_REACH)return false;
        var wrapper=RescueProps.find(p,s,"Wrapper");if(wrapper==null||wrapper.level()!=p.level())return false;
        var box=wrapper.getBoundingBox().inflate(.15);var eye=p.getEyePosition();
        if(!box.contains(eye)&&box.clip(eye,eye.add(p.getLookAngle().scale(4.5))).isEmpty())return false;
        return interact(p,s,wrapper);
    }
    static boolean canClaimRatEye(int ratLeg){return ratLeg>=2;}
    public static boolean interact(ServerPlayer p,VisitData.Scene s,Entity target){var phase=RescueChapter.phase(s);var n=RescueChapter.data(s);
        if(takeOfferedEye(p,s))return true;
        if((phase==MANTIS_TAKE||phase==MANTIS_SINK)&&(target==ShowActors.actor(p,s,"evil_other_father")||target==RescueProps.find(p,s,"Mantis"))){
            var offered=RescueProps.find(p,s,"GardenEye");
            if(offered!=null&&!offered.isInvisible()&&p.getEyePosition().distanceToSqr(offered.position())<25&&gaze(p,offered.position(),5,.8)){
                RescueEyes.eye(s,0).putBoolean("Defeated",true);RescueEyes.eye(s,0).putBoolean("Discovered",true);
                if(RescueEyes.claim(p,s,0))offered.setInvisible(true);return true;
            }
        }
        if(target instanceof CinematicPropEntity prop&&prop.kind()==RescueProps.EYE){int eye=prop.slot();if(eye==0&&(phase==MANTIS_TAKE||phase==MANTIS_SINK)){RescueEyes.eye(s,0).putBoolean("Defeated",true);RescueEyes.eye(s,0).putBoolean("Discovered",true);if(RescueEyes.claim(p,s,0))prop.setInvisible(true);return true;}if(eye==1&&phase==THEATRE_EYE){if(RescueEyes.claim(p,s,1))prop.setInvisible(true);return true;}if(eye==1&&phase==THEATRE_SEARCH&&RescueEyes.eye(s,1).getBoolean("Discovered"))RescueChapter.go(p,s,WRAPPER_REACH);else if(eye==1&&phase==WRAPPER_REACH&&n.getInt("Ticks")>=30)RescueChapter.go(p,s,THEATRE_GRAB);return true;}
        if(target instanceof CinematicPropEntity prop&&prop.kind()==RescueProps.WRAPPER){if(phase==THEATRE_SEARCH&&RescueEyes.eye(s,1).getBoolean("Discovered"))RescueChapter.go(p,s,WRAPPER_REACH);else if(phase==WRAPPER_REACH&&n.getInt("Ticks")>=30)RescueChapter.go(p,s,THEATRE_GRAB);return true;}
        // The ball's physical pick box is 1.35 blocks tall, while its 0.25
        // block-high rat rides only 0.45 above the ball. A normal ray hits the
        // ball first. Resolve that real hit to the SAME saved passenger; the
        // rat's leg, movement and original-owner checks below still apply.
        if(phase==RAT_CHASE&&target instanceof CinematicPropEntity ball
                &&ball.kind()==RescueProps.RAT_BALL&&ball.owned(s)){
            var passenger=ball.getPassengers().stream()
                    .filter(rider->rider instanceof com.coraline.otherworld.MouseEntity
                            &&n.hasUUID("ChaseRat")&&n.getUUID("ChaseRat").equals(rider.getUUID())
                            &&rider.getPersistentData().hasUUID("RescueOwner")
                            &&s.owner.equals(rider.getPersistentData().getUUID("RescueOwner")))
                    .findFirst().orElse(null);
            if(passenger==null){RescueChapter.hint(p,"Follow the original rat on its circus ball.");return true;}
            return interact(p,s,passenger);
        }
        if(phase==RAT_CHASE&&n.hasUUID("ChaseRat")&&n.getUUID("ChaseRat").equals(target.getUUID())&&target.getPersistentData().hasUUID("RescueOwner")&&s.owner.equals(target.getPersistentData().getUUID("RescueOwner"))){if(!canClaimRatEye(n.getInt("RatLeg"))||!n.getBoolean("RatHouseMoved")||n.getBoolean("RatResetPending")
                ||!(target.getVehicle() instanceof CinematicPropEntity ball)||ball.kind()!=RescueProps.RAT_BALL||!ball.owned(s)){
            RescueChapter.hint(p,"Follow the rolling rat into the house and cut it off once it is moving.");
            return true;
        }if(RescueEyes.claim(p,s,2)){target.stopRiding();target.discard();n.remove("ChaseRat");for(int i=0;i<3;i++)retireRatBall(p,s,i);RescueProps.retire(p,s,"ChaseObstacles");RescueChapter.go(p,s,EXTERIOR);}return true;}
        if(phase==COLLAPSE&&n.getBoolean("CatCollapsePlaced")&&target==ShowActors.actor(p,s,"cat")){n.putBoolean("CatRescued",true);return true;}return false;
    }
    public static void retry(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state){
        if(!RescueChapter.owns(p,s))return;var n=RescueChapter.data(s);p.setHealth(p.getMaxHealth());p.clearFire();p.getFoodData().setFoodLevel(20);
        if(state==RAT_CHASE){
            if(!n.getBoolean("RatResetPending")){
                n.putBoolean("RatResetPending",true);
                OtherMotherLoop.changed(p);
                p.server.overworld().getDataStorage().save();
            }
            resetRatSegment(p,s);
        }else if(state==COLLAPSE){if(restoreIncompleteCollapse(p,s))return;FirstEscapeStory.hint(p,"The Other World swallowed you. Retry the escape.");if(!RescueChapter.transfer(p,p.server.getLevel(OtherWorld.KEY),BlockPos.of(n.getLong("CollapseStart"))))return;n.putBoolean("CatRescued",false);n.putInt("CollapseElapsed",0);n.remove("CatCollapsePlaced");n.remove("CatCollapsePosition");n.remove("CatMantel");RescueChapter.go(p,s,COLLAPSE);}
        else if(RescueBosses.fighting(state))RescueBosses.retry(p,s,"Retrying this challenge.");
        else recoverNonCombat(p,s,state);
    }
    private static void clearRatPath(CompoundTag n){
        n.remove("RatPathTarget");
        n.remove("RatPath");
        n.remove("RatPathNode");
        n.remove("RatBallLast");
        n.putInt("RatStuck",0);
    }
    private static void resetRatSegment(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        var rat=chaseRat(p,s);
        if(rat==null){n.putString("Blocker","Waiting for the recorded chase rat before resuming.");return;}
        int leg=n.getInt("RatLeg");
        var level=p.server.getLevel(leg>=2?OtherWorld.KEY:ActivityWorld.KEY);
        var tent=MouseCircusSite.tent(p,s);
        if(level==null||leg>=2&&tent==null)return;
        var playerAt=leg>=2?tent.south(1):MouseCircusSite.center(p,s).south(16);
        var ratAt=leg>=2?tent.south(3):MouseCircusSite.center(p,s).south(10);
        LoopProps.hold(level,ratAt);
        var safe=OtherWorld.safePlayer(level,ratAt);
        if(safe==null)return;
        for(int slot=0;slot<3;slot++)if(!retireRatBall(p,s,slot)){
            n.putString("Blocker","Waiting for the recorded circus ball before resuming the chase.");
            return;
        }
        if(!RescueChapter.transfer(p,level,playerAt))return;
        var moved=ShowTravel.guest(rat,p,s,level,Vec3.atBottomCenterOf(safe),true,"RescueRatReturn");
        if(moved==null||!rat.getUUID().equals(moved.getUUID())||moved.level()!=level)return;
        moved.setPos(Vec3.atBottomCenterOf(safe));
        n.putInt("RatLeg",leg>=2?2:0);
        n.putInt("RatNode",0);
        n.putLong("RatAt",moved.blockPosition().asLong());
        n.putString("RatDimension",moved.level().dimension().location().toString());
        n.putLong("RatHouseStart",moved.blockPosition().asLong());
        n.remove("RatHouseMoved");
        n.remove("RatExitWait");
        n.remove("RatResetPending");
        clearRatPath(n);
        RescueChapter.go(p,s,RAT_CHASE);
    }
    private static boolean retireRatBall(ServerPlayer p,VisitData.Scene s,int leg){
        var scenery=ParentRescueStory.scenery(s);
        String key="RescueRatBall"+leg;
        if(!scenery.contains(key))return !FirstEscapeScene.context(s).contains(key);
        if(!scenery.hasUUID(key))return false;
        var id=scenery.getUUID(key);
        var level=com.coraline.parents.FamilyData.level(p.server,scenery.getString(key+"Dimension"));
        if(level==null||!scenery.contains(key+"Position",Tag.TAG_LONG))return false;
        var at=BlockPos.of(scenery.getLong(key+"Position"));
        LoopProps.hold(level,at);
        Entity original=null;
        for(var world:p.server.getAllLevels()){
            var entity=world.getEntity(id);
            if(entity==null)continue;
            if(original!=null||world!=level)return false;
            original=entity;
        }
        if(original==null&&!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at).toLong()))return false;
        if(original!=null){
            if(!(original instanceof CinematicPropEntity ball)||ball.kind()!=RescueProps.RAT_BALL||ball.slot()!=0||!ball.owned(s))return false;
            var n=RescueChapter.data(s);
            if(ball.getPassengers().stream().anyMatch(rider->!n.hasUUID("ChaseRat")||!rider.getUUID().equals(n.getUUID("ChaseRat"))))return false;
            ball.ejectPassengers();
            ball.discard();
        }
        var n=RescueChapter.data(s);
        var retired=n.getCompound("RetiredRatBalls");
        retired.putBoolean(id.toString(),true);
        n.put("RetiredRatBalls",retired);
        for(var field:new String[]{key,key+"Position",key+"Dimension"})scenery.remove(field);
        OtherMotherLoop.changed(p);
        return true;
    }
    static boolean retiredRatBall(CinematicPropEntity prop,VisitData.Scene scene){
        return prop.kind()==RescueProps.RAT_BALL&&prop.boundTo(scene)
                &&RescueChapter.data(scene).getCompound("RetiredRatBalls").getBoolean(prop.getUUID().toString());
    }
    private RescueEncounters(){}
}
