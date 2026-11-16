package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** An off-site interval of the existing visit, with physical routes and evidence-limited reports. */
public final class MotherGoesOut {
    public static CompoundTag data(VisitData.Scene s){return s.loop.event.getCompound("MotherAway");}
    public static boolean pending(VisitData.Scene s){return s!=null&&VisitEvents.type(s)==VisitEvents.Type.OTHER_MOTHER_GOES_OUT&&s.loop.event.getBoolean("MotherAwayPending")&&MotherAwayLedger.owned(data(s),s.owner,s.loop.event.getInt("Serial"));}
    public static boolean unavailable(ServerPlayer p){var s=OtherMotherLoop.scene(p);return pending(s)&&data(s).getBoolean("Offsite");}
    public static boolean fatherAvailable(ServerPlayer p){var s=OtherMotherLoop.active(p);return pending(s)&&data(s).getString("Stage").equals("ABSENT");}
    public static OtherMotherDialogue.Pool pool(String category,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/mother_goes_out/"+category+"/"+SecretDialogue.stateId(state),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var all=new ArrayList<OtherMotherDialogue.Pool>();for(String category:List.of("departure","return","missing","alert","caught_missing","caught_spy"))for(var state:OtherMotherWelcome.State.values())all.add(pool(category,state));return all;}
    private static ParentEntity raw(ServerPlayer p,VisitData.Scene s){
        if(s.mom==null)return null;var n=data(s);ServerLevel level=p.serverLevel();
        if(n.contains("Dimension")){var id=net.minecraft.resources.ResourceLocation.tryParse(n.getString("Dimension"));if(id!=null){var saved=p.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id));if(saved!=null)level=saved;}}
        return level.getEntity(s.mom) instanceof ParentEntity m&&m.isMom()&&m.isOtherParent()&&m.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(m.getPersistentData().getUUID("RealmDoor"))&&m.getPersistentData().hasUUID("LoopOwner")&&s.owner.equals(m.getPersistentData().getUUID("LoopOwner"))?m:null;
    }
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!NumberedEventDialogue.ready("mother_goes_out")||!recoverDoors(p,s))return false;
        var m=MotherSearch.mother(p);var realm=SecretWorld.realm(p);if(m==null||realm==null||realm.porch==null||realm.house==null)return false;
        var n=new CompoundTag();MotherAwayLedger.prepare(n,p.getUUID(),s.loop.event.getInt("Serial"),p.getRandom().nextLong());n.putString("Dimension",p.level().dimension().location().toString());n.putLong("LastActorPosition",m.blockPosition().asLong());
        n.putBoolean("OldInvisible",m.isInvisible());n.putBoolean("OldNoAi",m.isNoAi());n.putBoolean("OldNoGravity",m.isNoGravity());n.putBoolean("OldInvulnerable",m.isInvulnerable());n.putBoolean("OldSilent",m.isSilent());
        prepareNavigation(m);
        var outward=realm.porch.getCenter().subtract(realm.house.getCenter()).multiply(1,0,1).normalize();if(outward.lengthSqr()<.1)return false;
        var route=new ListTag();for(int distance:new int[]{0,10,24,42}){
            var target=realm.porch.getCenter().add(outward.scale(distance));BlockPos at=findRoutePoint(p,m,BlockPos.containing(target));if(at==null)return false;route.add(LongTag.valueOf(at.asLong()));
        }
        n.put("Route",route);n.putLong("Home",realm.porch.asLong());n.putLong("Relay",((LongTag)route.get(1)).getAsLong());
        if(!includeEntrance(p,m,n))return false;
        var restoration=new CompoundTag();for(String key:List.of("OldInvisible","OldNoAi","OldNoGravity","OldInvulnerable","OldSilent"))restoration.putBoolean(key,n.getBoolean(key));
        restoration.putInt("Serial",s.loop.event.getInt("Serial"));restoration.putUUID("Owner",p.getUUID());restoration.putUUID("Door",s.door);m.getPersistentData().put("MotherAwayRestore",restoration);
        s.loop.event.put("MotherAway",n);s.loop.event.putBoolean("MotherAwayPending",true);return true;
    }
    private static BlockPos findRoutePoint(ServerPlayer p,ParentEntity m,BlockPos target){
        for(int radius=0;radius<=5;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;int x=target.getX()+dx,z=target.getZ()+dz;
            if(!p.serverLevel().hasChunkAt(new BlockPos(x,target.getY(),z)))continue;
            int y=radius==0?target.getY():p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            var at=Reconciliation.safe(p,new BlockPos(x,y,z));if(at==null||!p.serverLevel().noCollision(m,m.getDimensions(m.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(at))))continue;
            var path=m.getNavigation().createPath(at,0);if(path!=null&&path.canReach())return at;
        }return null;
    }
    private static void prepareNavigation(ParentEntity m){if(m.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}}
    /** Resolve the generated front door once; the return checkpoint must be inside it. */
    private static boolean includeEntrance(ServerPlayer p,ParentEntity m,CompoundTag n){
        if(n.getBoolean("EntranceRoute"))return true;
        var realm=SecretWorld.realm(p);if(realm==null||realm.house==null||realm.porch==null)return false;
        BlockPos door=null;double distance=Double.MAX_VALUE;
        for(var at:BlockPos.betweenClosed(realm.porch.offset(-6,-2,-6),realm.porch.offset(6,4,8))){
            if(!p.serverLevel().hasChunkAt(at))continue;var state=p.serverLevel().getBlockState(at);
            if(!(state.getBlock() instanceof DoorBlock)||state.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER)continue;
            if(Arrays.stream(HouseCabinets.ROOMS).anyMatch(room->HouseCabinets.contains(realm.house,room,at)))continue;
            double d=at.distSqr(realm.porch);if(d<distance){door=at.immutable();distance=d;}
        }
        if(door==null)return false;
        var state=p.serverLevel().getBlockState(door);Direction inward=state.getValue(DoorBlock.FACING);
        if(door.relative(inward).distSqr(realm.house)>door.relative(inward.getOpposite()).distSqr(realm.house))inward=inward.getOpposite();
        BlockPos inside=null;
        for(int depth=3;depth<=4&&inside==null;depth++)for(int side:new int[]{0,1,-1}){
            var candidate=door.relative(inward,depth).relative(inward.getClockWise(),side);if(!p.serverLevel().hasChunkAt(candidate))continue;var safe=Reconciliation.safe(p,candidate);
            if(safe==null||safe.getX()!=candidate.getX()||safe.getZ()!=candidate.getZ()||Math.abs(safe.getY()-door.getY())>1||!p.serverLevel().noCollision(m,m.getDimensions(m.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(safe))))continue;
            if(Arrays.stream(HouseCabinets.ROOMS).anyMatch(room->HouseCabinets.contains(realm.house,room,safe)))continue;
            inside=safe;break;
        }
        if(inside==null)return false;
        var old=n.getList("Route",Tag.TAG_LONG);if(old.isEmpty())return false;
        var route=new ListTag();route.add(LongTag.valueOf(inside.asLong()));for(var point:old)route.add(point.copy());
        n.put("Route",route);n.putLong("Home",inside.asLong());n.putLong("FrontDoor",door.asLong());n.putString("FrontDoorBlock",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());n.putBoolean("EntranceRoute",true);
        // Existing saved journeys keep the same physical waypoint, selected duration and reports.
        if(n.contains("RouteIndex"))n.putInt("RouteIndex",n.getInt("RouteIndex")+1);
        return true;
    }
    /** Open only the saved front door; retain both expected halves until safe restoration. */
    private static void frontDoor(ParentEntity m,VisitData.Scene s,CompoundTag n,boolean crossing){
        if(!n.getBoolean("EntranceRoute"))return;var level=(ServerLevel)m.level();var at=BlockPos.of(n.getLong("FrontDoor"));if(!level.hasChunkAt(at)||!level.hasChunkAt(at.above()))return;
        var state=level.getBlockState(at);
        if(crossing&&m.distanceToSqr(Vec3.atBottomCenterOf(at))<=16){
            if(!(state.getBlock() instanceof DoorBlock door)||state.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER||!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(n.getString("FrontDoorBlock")))return;
            if(!state.getValue(DoorBlock.OPEN)){
                var upper=level.getBlockState(at.above());
                if(!state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER).equals(upper))return;
                var lease=new CompoundTag();lease.putUUID("Owner",s.owner);lease.putUUID("Door",s.door);lease.putInt("Serial",s.loop.event.getInt("Serial"));
                lease.putString("Dimension",level.dimension().location().toString());lease.putLong("Position",at.asLong());
                lease.put("Lower",NbtUtils.writeBlockState(state.setValue(DoorBlock.OPEN,true)));lease.put("Upper",NbtUtils.writeBlockState(upper.setValue(DoorBlock.OPEN,true)));
                n.put("FrontDoorLease",lease);n.putBoolean("FrontDoorOpened",true);VisitData.get(m.getServer()).setDirty();
                door.setOpen(m,level,state,at,true);
            }
        }else if(n.getBoolean("FrontDoorOpened")&&m.distanceToSqr(Vec3.atBottomCenterOf(at))>4){
            // Old saves lack an expected-state snapshot. Do not guess and close a later edit.
            if(!n.contains("FrontDoorLease")||restoreDoor(m.getServer(),n.getCompound("FrontDoorLease"))){n.remove("FrontDoorLease");n.remove("FrontDoorOpened");VisitData.get(m.getServer()).setDirty();}
        }
    }
    /** A resolved lease is restored or superseded by a later edit; unloaded/occupied doors wait. */
    private static boolean restoreDoor(net.minecraft.server.MinecraftServer server,CompoundTag lease){
        if(!lease.contains("Position")||!lease.contains("Lower")||!lease.contains("Upper"))return true;
        var id=net.minecraft.resources.ResourceLocation.tryParse(lease.getString("Dimension"));if(id==null)return true;
        var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id));if(level==null)return false;
        var at=BlockPos.of(lease.getLong("Position"));if(!level.hasChunkAt(at)||!level.hasChunkAt(at.above()))return false;
        var state=level.getBlockState(at);var upper=level.getBlockState(at.above());
        if(!NbtUtils.writeBlockState(state).equals(lease.getCompound("Lower"))||!NbtUtils.writeBlockState(upper).equals(lease.getCompound("Upper")))return true;
        if(!(state.getBlock() instanceof DoorBlock door)||state.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER||!state.getValue(DoorBlock.OPEN))return true;
        if(!level.getEntitiesOfClass(LivingEntity.class,new AABB(at).expandTowards(0,1,0).inflate(1,1,1)).isEmpty())return false;
        door.setOpen(null,level,state,at,false);
        return state.setValue(DoorBlock.OPEN,false).equals(level.getBlockState(at))&&upper.setValue(DoorBlock.OPEN,false).equals(level.getBlockState(at.above()));
    }
    /** Independent of the current event/actor: aborted crossings retain their saved cleanup. */
    public static boolean recoverDoors(ServerPlayer p,VisitData.Scene s){
        if(s==null||!p.getUUID().equals(s.owner))return false;
        var leases=s.loop.story.getList("MotherAwayDoorCleanup",Tag.TAG_COMPOUND);boolean changed=false;
        for(int i=leases.size()-1;i>=0;i--){var lease=leases.getCompound(i);
            if(!lease.hasUUID("Owner")||!s.owner.equals(lease.getUUID("Owner"))||!lease.hasUUID("Door")||!s.door.equals(lease.getUUID("Door")))continue;
            if(restoreDoor(p.server,lease)){leases.remove(i);changed=true;}
        }
        if(leases.isEmpty()&&s.loop.story.contains("MotherAwayDoorCleanup")){s.loop.story.remove("MotherAwayDoorCleanup");changed=true;}
        if(changed)OtherMotherLoop.changed(p);return leases.isEmpty();
    }
    public static void begin(ServerPlayer p,VisitData.Scene s){data(s).putInt("RouteIndex",0);data(s).putLong("RouteStarted",data(s).getLong("Clock"));OtherMotherLoop.phase(p,s,MOTHER_LEAVING);}
    /** Never create a replacement while the existing actor may merely be unloaded. */
    public static boolean preserveActor(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(raw(p,s)!=null){n.remove("MissingActorTicks");return true;}
        if(n.contains("LastActorPosition")){var at=BlockPos.of(n.getLong("LastActorPosition"));LoopProps.hold(p.serverLevel(),at);p.serverLevel().getChunkAt(at);}
        if(raw(p,s)!=null){n.remove("MissingActorTicks");return true;}
        int missing=n.getInt("MissingActorTicks")+20;n.putInt("MissingActorTicks",missing);
        // The existing visit-failure path returns the owner home without rewards or penalties.
        // A subsequently loaded actor restores its saved flags through recoverActor().
        return missing<600;
    }
    /** One active-owner tick. Exact timer data stays entirely server-side. */
    public static void backgroundTick(ServerPlayer p,VisitData.Scene s){
        if(!pending(s))return;var n=data(s);
        if(s.loop.suspicion>=1||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP){cleanup(p,s);return;}
        if(s.loop.phase==EVENT_INTRO||p.isSleeping()||raw(p,s)==null)return;
        if(!n.getBoolean("Offsite")&&s.loop.secrets.searching())MotherAwayLedger.searching(n);
        String before=n.getString("Stage");
        MotherAwayLedger.tick(n,Reconciliation.active(p)||SecretDialogue.busy(p));
        if(n.getString("Stage").equals("RETURNING")&&!before.equals("RETURNING"))n.putLong("RouteStarted",n.getLong("Clock"));
        if(n.getString("Stage").equals("RETURNING")&&s.loop.phase==MOTHER_ABSENT)OtherMotherLoop.phase(p,s,MOTHER_RETURNING);
        if((n.getString("Stage").equals("LEAVING")||n.getString("Stage").equals("RETURNING"))&&n.getLong("Clock")-n.getLong("RouteStarted")>=2400){
            if(n.getString("Stage").equals("LEAVING")){cleanup(p,s);VisitEvents.next(p,s,false);}
            else OtherMotherLoop.fail(p,"Mother's return route is blocked. You have returned home safely.");
            return;
        }
        if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        var n=data(s);var m=raw(p,s);if(m==null)return true;
        if(n.getString("Stage").equals("HOME_CHECK")){
            frontDoor(m,s,n,false);
            if(!n.getBoolean("ReturnLineDone")){
                String category=n.getBoolean("Early")?"alert":n.getBoolean("NearbyAtReturn")?"return":"missing";
                VisitEvents.stopConversation(p);
                // Owner-directed lines never play from across the realm. Missing-player search
                // can start without broadcasting a private monologue to a distant player.
                // The persisted voice identity lets an already-started line finish if the owner
                // steps away, without authorizing a new recording to start remotely.
                boolean speaking=s.loop.event.getString("Voice").startsWith("events/mother_goes_out/"+category+"/");
                if(speaking||m.distanceToSqr(p)<=144&&m.hasLineOfSight(p)){if(!VisitEvents.speak(p,s,pool(category,SecretDialogue.state(p))))return true;}
                n.putBoolean("ReturnLineDone",true);
            }
            if(MotherAwayLedger.safeReturn(n)){cleanup(p,s);VisitEvents.next(p,s,false);return true;}
            startSearch(p,s);return true;
        }
        return true;
    }
    public static boolean control(ParentEntity m,ServerPlayer p,VisitData.Scene s){
        if(!pending(s)||!m.getUUID().equals(s.mom)||s.loop.phase==EVENT_INTRO)return false;var n=data(s);String stage=n.getString("Stage");
        if(m.tickCount%20==0)n.putLong("LastActorPosition",m.blockPosition().asLong());
        if(Reconciliation.active(p))return false;
        if(stage.equals("SEARCHING")||!n.getBoolean("Offsite")&&s.loop.secrets.searching()){MotherAwayLedger.searching(n);return false;}
        if(SecretDialogue.busy(p)||p.isSleeping()){m.getNavigation().stop();return true;}
        if((stage.equals("LEAVING")||stage.equals("RETURNING"))&&!n.getBoolean("EntranceRoute")){
            prepareNavigation(m);if(m.tickCount%20!=0||!includeEntrance(p,m,n)){m.getNavigation().stop();return true;}
        }
        if(n.getBoolean("Offsite")&&!stage.equals("RETURNING")){m.getNavigation().stop();m.setNoAi(true);m.setNoGravity(true);m.setDeltaMovement(Vec3.ZERO);return true;}
        if(stage.equals("RETURNING")&&n.getBoolean("Offsite")){
            var returnRoute=n.getList("Route",Tag.TAG_LONG);int returnIndex=Math.max(0,returnRoute.size()-2);
            if(watched(p,m.position())){
                // Recover only on the outdoor route, never inside the house or on its porch.
                if(m.tickCount%40==0)for(int i=returnRoute.size()-1;i>=2;i--){var at=BlockPos.of(((LongTag)returnRoute.get(i)).getAsLong());if(p.serverLevel().hasChunkAt(at)&&!watched(p,Vec3.atBottomCenterOf(at))&&p.distanceToSqr(at.getCenter())>64&&m.level().noCollision(m,m.getDimensions(m.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(at)))){m.setPos(Vec3.atBottomCenterOf(at));returnIndex=i-1;break;}}
                if(watched(p,m.position()))return true;
            }
            restore(m,n);n.putBoolean("Offsite",false);n.putInt("RouteIndex",returnIndex);
        }
        if(stage.equals("HOME_CHECK")){frontDoor(m,s,n,false);m.getNavigation().stop();if(m.distanceToSqr(p)<144)m.lookAtStoryPlayer(p);return true;}
        if(!stage.equals("LEAVING")&&!stage.equals("RETURNING"))return true;
        m.stopRiding();m.setNoAi(false);m.setNoGravity(false);m.setMaxUpStep(1.05F);
        prepareNavigation(m);
        var route=n.getList("Route",Tag.TAG_LONG);int index=n.getInt("RouteIndex");if(index<0||index>=route.size())return true;
        frontDoor(m,s,n,index<=1);
        var target=BlockPos.of(((LongTag)route.get(index)).getAsLong());
        if(m.distanceToSqr(Vec3.atBottomCenterOf(target))<(index<=1?1:4)){
            if(stage.equals("LEAVING")){
                if(index<route.size()-1){n.putInt("RouteIndex",index+1);n.putInt("RouteStall",0);}
                else if(!watched(p,m.position())||m.distanceToSqr(p)>96*96){
                    m.getNavigation().stop();m.setInvisible(true);m.setSilent(true);m.setInvulnerable(true);m.setNoAi(true);m.setNoGravity(true);MotherAwayLedger.departed(n);OtherMotherLoop.phase(p,s,MOTHER_ABSENT);
                }
            }else if(index>0){n.putInt("RouteIndex",index-1);n.putInt("RouteStall",0);}
            else {MotherAwayLedger.home(n,nearHome(p));OtherMotherLoop.phase(p,s,MOTHER_HOME_CHECK);m.getNavigation().stop();}
            return true;
        }
        if(m.tickCount%20==0){
            var path=m.getNavigation().createPath(target,0);if(path!=null&&path.canReach())m.getNavigation().moveTo(path,1.05);
            var before=n.contains("RouteLast")?BlockPos.of(n.getLong("RouteLast")):m.blockPosition();int stalled=before.distSqr(m.blockPosition())<1?n.getInt("RouteStall")+20:0;n.putInt("RouteStall",stalled);n.putLong("RouteLast",m.blockPosition().asLong());
            // Doorway and interior checkpoints must be walked; a blocked crossing uses the
            // existing bounded route failure instead of teleporting through the front door.
            if(index>1&&stalled>=100&&!watched(p,m.position())){
                var direction=target.getCenter().subtract(m.position());var step=BlockPos.containing(m.position().add(direction.normalize().scale(Math.min(6,direction.length()))));var safe=Reconciliation.safe(p,step);
                if(safe!=null&&m.level().noCollision(m,m.getDimensions(m.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(safe)))&&!watched(p,Vec3.atBottomCenterOf(safe))&&p.distanceToSqr(safe.getCenter())>16){m.setPos(Vec3.atBottomCenterOf(safe));n.putInt("RouteStall",0);}
            }
            if(stalled>=1200&&stage.equals("LEAVING")){cleanup(p,s);VisitEvents.next(p,s,false);}
        }return true;
    }
    private static boolean watched(ServerPlayer p,Vec3 at){
        var delta=at.add(0,1,0).subtract(p.getEyePosition());if(delta.lengthSqr()>96*96)return false;
        if(delta.normalize().dot(p.getLookAngle())<.12)return false;
        return p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),at.add(0,1,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;
    }
    public static boolean nearHome(ServerPlayer p){var r=SecretWorld.realm(p);return r!=null&&r.house!=null&&OtherWorldEstate.outsideDistance(r.house,p.blockPosition())<=8&&p.getY()>=r.house.getY()-24&&p.getY()<=r.house.getY()+50;}
    public static String observedOffense(ServerPlayer p){
        var s=SecretTransactions.state(p);if(s==null)return "";if(!SecretTransactions.evidence(p,p.getMainHandItem()).isEmpty())return "restricted_item";if(SecretDialogue.secretArea(p,p.blockPosition()))return "forbidden";
        if(p.containerMenu!=p.inventoryMenu)for(var tag:s.world.getCompound("HouseCabinets").getList("Cabinets",Tag.TAG_COMPOUND)){var n=(CompoundTag)tag;if(n.getBoolean("Open")&&p.distanceToSqr(BlockPos.of(n.getLong("Pos")).getCenter())<25)return "drawer";}
        return nearHome(p)?"":"location";
    }
    public static boolean suspicious(ServerPlayer p){return !observedOffense(p).isEmpty();}
    public static void recordSighting(ServerPlayer p,CompoundTag witness){if(!pending(OtherMotherLoop.scene(p)))return;witness.putString("AwayOffense",observedOffense(p));witness.putString("AwayIdentified",SecretTransactions.evidence(p,p.getMainHandItem()));witness.putLong("AwayObservedAt",OtherMotherLoop.now(p));witness.putLong("ReportPosition",p.blockPosition().asLong());}
    /** Returns true only when this active event consumed the legitimate report. */
    public static boolean spyReport(ServerPlayer p,Entity witness,String kind,BlockPos sighting){
        var s=OtherMotherLoop.active(p);if(!pending(s)||data(s).getString("Stage").equals("LEAVING"))return false;var observed=witness.getPersistentData();String offense=observed.getString("AwayOffense");if(offense.isEmpty())return true;
        var n=data(s);String identified=observed.getString("AwayIdentified");
        // Preserve the existing false-signal ability: a diverted report carries only its decoy location.
        if(s.loop.secrets.falseUntil>OtherMotherLoop.now(p)){offense="location";identified="";sighting=s.loop.secrets.target;}
        boolean accepted=MotherAwayLedger.report(n,witness.getUUID(),kind,offense,sighting.asLong(),identified,observed.getLong("AwayObservedAt"));
        if(accepted){String id=identified;if(SecretRegistry.ALL.containsKey(id))s.loop.secrets.exposed.add(id);if(n.getString("Stage").equals("SEARCHING")&&s.loop.secrets.search!=SecretsState.Search.DIRECT_CHASE){s.loop.secrets.target=sighting;s.loop.secrets.world.putLong("SpyReportOrigin",sighting.asLong());}SecretTransactions.checkpoint(p);}return true;
    }
    /** Rats must physically reach a relay while Mother is away, not merely exhaust a timer. */
    public static boolean reportingRat(com.coraline.otherworld.MouseEntity rat,ServerPlayer p){
        var s=OtherMotherLoop.active(p);if(!pending(s)||data(s).getString("Stage").equals("LEAVING"))return false;var n=data(s);var relay=BlockPos.of(n.getLong("Relay"));
        rat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(.65);
        if(rat.tickCount%10==0)rat.getNavigation().moveTo(relay.getX()+.5,relay.getY(),relay.getZ()+.5,2.8);
        if(rat.distanceToSqr(relay.getCenter())<=4&&p.serverLevel().clip(new net.minecraft.world.level.ClipContext(rat.getEyePosition(),relay.getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,rat)).getType()==HitResult.Type.MISS){
            var sighting=BlockPos.of(rat.getPersistentData().getLong("ReportPosition"));spyReport(p,rat,"rat",sighting);SecretSpies.removeRat(p,rat);rat.discard();
        }return true;
    }
    private static void startSearch(ServerPlayer p,VisitData.Scene s){var n=data(s);MotherAwayLedger.searching(n);MotherInterrupts.interrupt(p);s.loop.secrets.search=SecretsState.Search.SEARCHING;var origin=n.contains("LastReportedPosition")?BlockPos.of(n.getLong("LastReportedPosition")):BlockPos.of(n.getLong("Home"));s.loop.secrets.target=origin;s.loop.secrets.world.putLong("SpyReportOrigin",origin.asLong());SecretTransactions.checkpoint(p);}
    /** Queue adapter: called only from the existing contact-guarded caught dialogue path. */
    public static OtherMotherDialogue.Pool contactPool(ServerPlayer p,SecretDialogue.Event event,OtherMotherWelcome.State state){
        var s=OtherMotherLoop.scene(p);if(!pending(s)||!data(s).getBoolean("SearchBegun")||data(s).getBoolean("CaughtLineQueued")||!Set.of(SecretDialogue.Event.ORDINARY_AREA_CAUGHT,SecretDialogue.Event.SECRET_AREA_CAUGHT,SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT).contains(event))return null;
        var m=raw(p,s);if(m==null||!m.getBoundingBox().inflate(.08,0,.08).intersects(p.getBoundingBox())||!m.hasLineOfSight(p))return null;
        data(s).putBoolean("CaughtLineQueued",true);return pool(data(s).getBoolean("ActionableReport")?"caught_spy":"caught_missing",state);
    }
    public static OtherMotherDialogue.Pool queuedPool(String path){return pools().stream().filter(p->p.path().equals(path)).findFirst().orElse(null);}
    public static void suspend(ServerPlayer p,VisitData.Scene s){if(pending(s))VisitEvents.stopConversation(p);}
    public static boolean resume(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(!pending(s)||!data(s).getBoolean("SearchBegun"))return false;if(s.loop.suspicion>=1)return false;cleanup(p,s);VisitEvents.next(p,s,false);return true;}
    public static void reconnect(ServerPlayer p,VisitData.Scene s){if(!pending(s))return;VisitEvents.stopConversation(p);var m=raw(p,s);if(m!=null&&data(s).getBoolean("Offsite")){m.setInvisible(true);m.setSilent(true);m.setInvulnerable(true);m.setNoAi(true);m.setNoGravity(true);}}
    private static void restore(ParentEntity m,CompoundTag n){m.setInvisible(n.getBoolean("OldInvisible"));m.setNoAi(n.getBoolean("OldNoAi"));m.setNoGravity(n.getBoolean("OldNoGravity"));m.setInvulnerable(n.getBoolean("OldInvulnerable"));m.setSilent(n.getBoolean("OldSilent"));}
    /** Loaded actors repair their own saved flags after an event ended in another dimension. */
    public static void recoverActor(ParentEntity m,VisitData.Scene s){var tag=m.getPersistentData();if(!tag.contains("MotherAwayRestore"))return;var saved=tag.getCompound("MotherAwayRestore");if(!saved.hasUUID("Owner")||!saved.getUUID("Owner").equals(s.owner)||!saved.hasUUID("Door")||!saved.getUUID("Door").equals(s.door)||!m.getUUID().equals(s.mom))return;if(!pending(s)||saved.getInt("Serial")!=s.loop.event.getInt("Serial")){restore(m,saved);tag.remove("MotherAwayRestore");}}
    public static void cleanup(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return;
        if(!s.loop.event.contains("MotherAway")){recoverDoors(p,s);return;}
        var n=data(s);var m=raw(p,s);
        if(n.contains("FrontDoorLease")){
            var leases=s.loop.story.getList("MotherAwayDoorCleanup",Tag.TAG_COMPOUND);leases.add(n.getCompound("FrontDoorLease").copy());s.loop.story.put("MotherAwayDoorCleanup",leases);
        }
        if(m!=null){restore(m,n);m.getPersistentData().remove("MotherAwayRestore");m.getNavigation().stop();}
        MotherAwayLedger.completed(n);s.loop.event.remove("MotherAwayPending");s.loop.event.remove("MotherAway");
        recoverDoors(p,s);OtherMotherLoop.changed(p);
    }
    public static void debug(ServerPlayer p,String input){if(!p.hasPermissions(2)||!OtherWorld.isOther(p.level()))return;var s=OtherMotherLoop.active(p);if(s==null)return;String[] args=input.split(",",2);if(args[0].equals("force")){VisitEvents.start(p,s,VisitEvents.Type.OTHER_MOTHER_GOES_OUT);return;}if(!pending(s))return;var n=data(s);try{switch(args[0]){case "time"->{if(args.length==2)n.putInt("Remaining",Math.max(0,Math.min(42000,Integer.parseInt(args[1]))));}case "return"->n.putInt("Remaining",0);case "state"->p.sendSystemMessage(net.minecraft.network.chat.Component.literal(n.toString()));default->{}}}catch(NumberFormatException ignored){}OtherMotherLoop.changed(p);}
    public static String debugOperator(ServerPlayer p,VisitData.Scene s,String action,String value){
        if(!NumberedDebugActions.operator(p,s)||!pending(s))return "No owned Mother Away event is active.";
        var n=data(s);String stage=n.getString("Stage");var mother=raw(p,s);
        if(action.equals("home_check"))return "Actual near-home check: "+nearHome(p)+"; no presence flag was fabricated.";
        if(action.equals("departure"))return NumberedDebugActions.previewPrefix(p,s,"away","departure");
        if(action.equals("time")){
            int ticks=NumberedDebugActions.integer(value,0,42000);if(!stage.equals("ABSENT"))return "Remaining time can be set only after Mother physically left.";
            n.putInt("Remaining",ticks);OtherMotherLoop.changed(p);return "Hidden absence remaining set to "+ticks+" ticks; selected original duration retained.";
        }
        if(action.equals("home")){var realm=SecretWorld.realm(p);return realm==null?"No owner realm is available.":NumberedDebugActions.teleport(p,realm.porch);}
        if(action.equals("rat")||action.equals("doll")){
            String counter=action.equals("rat")?"DebugRatsCreated":"DebugDollsCreated";
            if(n.getInt(counter)>=2)return "This event already created its two bounded test "+action+" targets.";
            var at=Reconciliation.safe(p,p.blockPosition().offset(3,0,0));if(at==null)return "No safe nearby spy position.";
            if(action.equals("rat"))SecretSpies.rat(p,at);else SecretSpies.doll(p,at,p.getYRot());
            n.putInt(counter,n.getInt(counter)+1);OtherMotherLoop.changed(p);return "Requested one real owned "+action+" through existing spy setup; normal perception/reporting remains required.";
        }
        if(action.equals("rat_detect")||action.equals("rat_intercept")||action.equals("spy_report")||action.equals("early")){
            var rats=p.serverLevel().getEntitiesOfClass(com.coraline.otherworld.MouseEntity.class,p.getBoundingBox().inflate(48),rat->{var tag=rat.getPersistentData();return tag.hasUUID("SecretOwner")&&p.getUUID().equals(tag.getUUID("SecretOwner"))&&tag.getInt("SecretVisit")==s.loop.secrets.visit;});
            for(var rat:rats){
                var tag=rat.getPersistentData();
                if(action.equals("rat_detect")){SecretSpies.mouse(rat);return "Ran one real perception step; line of sight, recognition time and successful delivery are still required.";}
                if(!tag.getBoolean("Alerted"))continue;
                if(action.equals("rat_intercept")){SecretSpies.removeRat(p,rat);rat.discard();OtherMotherLoop.changed(p);return "Intercepted this owner's actual reporting rat before delivery. Already delivered reports remain valid.";}
                if(!tag.getString("AwayOffense").isEmpty()&&n.contains("Relay")){
                    int before=n.getList("Reports",Tag.TAG_COMPOUND).size();reportingRat(rat,p);
                    return n.getList("Reports",Tag.TAG_COMPOUND).size()>before?"Actual witness report delivered; normal delayed return applies.":"Reporting rat is still travelling to the real relay; no report or early return was fabricated.";
                }
            }
            return "No qualifying owned rat observation/report is available. Dolls continue through their normal detection/report path.";
        }
        if(Reconciliation.active(p)||SecretDialogue.busy(p))return "An actual reconciliation/dialogue is still running.";
        switch(action){
            case "leave"->{if(!stage.equals("LEAVING")||s.loop.phase==EVENT_INTRO)return "Allow the complete departure recording to finish before the real route starts.";if(mother==null)return "Original Mother actor is unavailable.";control(mother,p,s);return "Original Mother's departure navigation ran; absence starts only after she actually leaves.";}
            case "return"->{if(!stage.equals("ABSENT"))return "Normal return can be requested only from the actual absent stage.";n.putInt("Remaining",0);OtherMotherLoop.changed(p);return "Absence will expire on the next tick; real travel delay/doorway return remains required.";}
            case "navigation"->{if(!stage.equals("RETURNING")||mother==null)return "Requires the original Mother's returning stage.";control(mother,p,s);return "Retried the original actor's current safe return route.";}
            case "missing","search"->{if(n.getBoolean("Offsite")||!stage.equals("HOME_CHECK")||mother==null)return "Mother must actually return and reach her home-check checkpoint first.";if(action.equals("missing")&&nearHome(p))return "The player is really near home; no missing-player flag was fabricated.";startSearch(p,s);return "Existing Mother search started; contact capture remains required.";}
            case "bell"->{var realm=SecretWorld.realm(p);if(realm==null||realm.porch==null||p.distanceToSqr(realm.porch.getCenter())>25)return "Stand at your actual porch bell.";MotherSearch.bell(p);return "Existing porch-bell behavior checked its actual search/reconciliation guards.";}
            case "catch","confrontation"->{if(mother==null||n.getBoolean("Offsite")||!MotherSearch.touching(mother,p))return "Mother must physically touch the owner with line of sight; no capture was injected.";MotherSearch.catchPlayer(p);return "Existing contact-guarded capture path ran once.";}
            case "complete"->{if(MotherAwayLedger.safeReturn(n)){tick(p,s);return "Normal safe-home return tick ran; required greeting completes before cleanup.";}return "Complete the real safe return or contact/reconciliation checkpoint. No ordinary completion was fabricated.";}
            default->{return "Unknown Mother Away control.";}
        }
    }

    private MotherGoesOut(){}
}
