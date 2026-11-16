package com.coraline.secrets;
import com.coraline.feast.*;
import com.coraline.parents.ParentEntity;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** One pursuit controller arbitrates visual evidence, sounds, reporting spies and scripted dialogue. */
public final class MotherSearch {
    public static ParentEntity mother(ServerPlayer p){if(CatEncounter.active(p)||MotherGoesOut.unavailable(p))return null;var scene=OtherMotherLoop.scene(p);return scene!=null&&scene.mom!=null&&p.serverLevel().getEntity(scene.mom) instanceof ParentEntity m?m:null;}
    public static void missed(ServerPlayer p){var s=SecretTransactions.state(p);s.world.remove("SpyReportOrigin");s.search=SecretsState.Search.MISSED_ACTIVITY_PENDING;s.pendingTicks=FamilyDinner.graceTicks(OtherMotherLoop.scene(p).loop.suspicion,p.getRandom().nextDouble());SecretTransactions.dirty(p);}
    public static void attendanceExpired(ServerPlayer p){var s=SecretTransactions.state(p);if(s.world.getBoolean("LateSearchScheduled"))return;s.world.putBoolean("LateSearchScheduled",true);if(!s.searching())missed(p);SecretTransactions.dirty(p);}
    public static void attended(ServerPlayer p){var s=SecretTransactions.state(p);s.world.remove("SpyReportOrigin");s.search=SecretsState.Search.NORMAL;s.pendingTicks=0;s.searchTicks=0;s.world.remove("LateSearchScheduled");var m=mother(p);if(m!=null)m.getNavigation().stop();SecretTransactions.dirty(p);}
    public static void noise(ServerPlayer p,BlockPos at,double range,String source){if(Reconciliation.active(p))return;var s=SecretTransactions.state(p);var m=mother(p);if(s==null||m==null)return;p.serverLevel().playSound(null,at,net.minecraft.sounds.SoundEvents.BELL_BLOCK,net.minecraft.sounds.SoundSource.BLOCKS,2,.65F);
        if(s.search==SecretsState.Search.DIRECT_CHASE)return; // A distraction cannot replace a witnessed pursuit.
        boolean blocked=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(m.getEyePosition(),at.getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,m)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS;
        if(m.distanceToSqr(at.getCenter())>Math.pow(range*(blocked?.65:1),2))return;s.search=SecretsState.Search.INVESTIGATING_NOISE;s.target=at.immutable();s.world.remove("SpyReportOrigin");s.world.putString("NoiseSource",source);s.world.putLong("NoiseTime",OtherMotherLoop.now(p));s.world.putDouble("NoiseRange",range);s.world.putLong("NoisePosition",at.asLong());SecretTransactions.dirty(p);
    }
    public static void observed(ServerPlayer p,String id,BlockPos at){if(Reconciliation.active(p))return;if(!id.equals(SecretTransactions.evidence(p,p.getMainHandItem()))&&!id.equals(SecretTransactions.evidence(p,p.getOffhandItem())))return;var s=SecretTransactions.state(p);var m=mother(p);if(s==null||m==null||s.invisibleUntil>OtherMotherLoop.now(p)||p.isInvisible())return;double sight=30*(1-.5*s.effective(2))*(1-s.soulWeakening());if(!visible(m,p,sight))return;if(SecretRegistry.ALL.containsKey(id))s.exposed.add(id);forbiddenAction(p,SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT);SecretTransactions.checkpoint(p);}
    public static void bell(ServerPlayer p){var s=SecretTransactions.state(p);if(s==null||!s.searching()||SecretDialogue.busy(p)||!Reconciliation.permitted(p))return;if(SpecialDelivery.openedActive(p)){var m=mother(p);if(m!=null&&touching(m,p))catchPlayer(p);else if(m!=null){s.search=SecretsState.Search.INVESTIGATING_NOISE;s.target=p.blockPosition();}return;}var state=SecretDialogue.state(p);var scene=OtherMotherLoop.scene(p);OtherMotherLoop.relationship(p,scene.loop.trust-.025F,scene.loop.suspicion);Reconciliation.begin(p,Reconciliation.Category.MINOR,state,true);}
    public static void catchPlayer(ServerPlayer p){catchPlayer(p,false);}
    public static void catchSeenPlayer(ServerPlayer p){
        var s=SecretTransactions.state(p);var m=mother(p);if(s==null||m==null||!visible(m,p,96))return;
        if(!s.searching())startChase(p);else remember(p);
    }
    public static void forbiddenAction(ServerPlayer p,SecretDialogue.Event event){if(MotherGoesOut.unavailable(p))return;
        var s=SecretTransactions.state(p);if(s==null||Reconciliation.active(p)||s.world.getBoolean("CaughtEncounter")||s.world.getLong("CatchUntil")>OtherMotherLoop.now(p))return;
        // Remember the witnessed offense even if the item is put away or the room is left.
        if(!s.world.getString("ChaseOffense").equals(SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT.name()))s.world.putString("ChaseOffense",event.name());
        if(s.search!=SecretsState.Search.DIRECT_CHASE)startChase(p);
    }
    private static void catchPlayer(ServerPlayer p,boolean seen){var s=SecretTransactions.state(p);var scene=OtherMotherLoop.scene(p);var r=SecretWorld.realm(p);if(s==null||r==null||Reconciliation.active(p)||s.world.getLong("CatchUntil")>OtherMotherLoop.now(p))return;var mother=mother(p);if(mother==null||!touching(mother,p))return;if(s.world.getBoolean("CaughtEncounter"))return;var dialogueState=SecretDialogue.state(p);MotherInterrupts.interrupt(p);s.world.putBoolean("CaughtEncounter",true);
        var evidence=new java.util.HashSet<String>();for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);String id=SecretTransactions.evidence(p,item);if(!id.isEmpty())evidence.add(id);if(item.is(SecretsItems.KEY.get())&&SecretTransactions.owned(p,item))evidence.add("personal_keyring");}double severity=SecretRegistry.severity(evidence);s.exposed.addAll(evidence);int room=SecretWorld.roomAt(s,p.blockPosition());double area=room<0?0:(SecretWorld.rooms(s).get(room).getInt("Tier")+1)*.1;
        if(SpecialDelivery.caught(p,dialogueState,evidence))return;
        WybieAdventure.caught(p,dialogueState,evidence,SecretDialogue.secretArea(p,p.blockPosition()));
        OtherMotherLoop.relationship(p,scene.loop.trust-(float)(.025+Math.min(.18,Math.sqrt(p.distanceToSqr(r.house.getCenter()))/1000)+severity*.15+area*.15),scene.loop.suspicion+(float)(severity*SecretsConfig.CATCH_SEVERITY.get()+area*.2));s.search=SecretsState.Search.NORMAL;s.world.putLong("CatchUntil",OtherMotherLoop.now(p)+200);s.target=r.porch;
        var event=HouseCabinets.keyConfrontation(p)?SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT:SecretDialogue.caught(!evidence.isEmpty(),SecretDialogue.secretArea(p,p.blockPosition()));
        if(!s.world.getString("ChaseOffense").isEmpty())event=SecretDialogue.Event.valueOf(s.world.getString("ChaseOffense"));s.world.remove("ChaseOffense");HouseCabinets.confronted(p);SecretDialogue.confront(p,event,dialogueState);SecretTransactions.dirty(p);
    }
    public static void tick(ServerPlayer p){var s=SecretTransactions.state(p);if(s.world.getBoolean("CaughtEncounter")){var watcher=mother(p);int away=watcher==null||watcher.distanceToSqr(p)>144||!watcher.hasLineOfSight(p)?s.world.getInt("EncounterAway")+1:0;s.world.putInt("EncounterAway",away);if(away>=60){s.world.remove("CaughtEncounter");s.world.remove("EncounterAway");}}if(p.tickCount%200==0&&!SecretDialogue.busy(p)&&!s.searching()&&!s.exposed.isEmpty()&&s.world.getLong("RemarkAfter")<OtherMotherLoop.now(p)){var m=mother(p);if(m!=null&&m.distanceToSqr(p)<64){String remark=s.exposed.contains("soul_prisons")?"Some doors are closed for their own good. Leave them closed.":s.exposed.contains("soulglass")?"There are things here too fragile for curious hands.":s.exposed.contains("spying_dolls")?"You have been watching everything so carefully. So have I.":s.counter(5)>0?"Perhaps my presents have spoiled you.":s.counter(1)>0?"Is there something missing from what I have given you?":"You needn't concern yourself with every locked door.";VisitAudio.caption(p,"Other Mother",remark,120);s.world.putLong("RemarkAfter",OtherMotherLoop.now(p)+2400);}}if(s.search==SecretsState.Search.MISSED_ACTIVITY_PENDING&&--s.pendingTicks<=0){s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);}
        if(s.searching()&&++s.searchTicks%400==0){var l=OtherMotherLoop.scene(p).loop;OtherMotherLoop.relationship(p,l.trust,l.suspicion+.01F);}if(p.tickCount%20==0)OtherMotherLoop.changed(p);}
    public static boolean control(ParentEntity m,ServerPlayer p){m.getPersistentData().putBoolean("PursuitDriving",false);m.setMaxUpStep(.6F);if(!SecretsRuntime.active(p))return false;var s=SecretTransactions.state(p);if(Reconciliation.control(m,p))return true;if(SecretDialogue.busy(p)&&!s.searching()){m.getNavigation().stop();m.getLookControl().setLookAt(p,30,30);return true;}if(!s.searching()&&s.search!=SecretsState.Search.RETURNING_HOME)return false;
        double effective=s.effective(2),range=30*(1-.5*effective)*(1-s.soulWeakening());boolean visible=visible(m,p,range);
        if(s.searching()&&touching(m,p)){catchPlayer(p);if(Reconciliation.active(p))return true;}
        if(visible&&s.searching()){remember(p);m.getLookControl().setLookAt(p,30,30);}
        if(s.search==SecretsState.Search.DIRECT_CHASE&&!visible){
            if(!s.world.contains("ChaseSeenAt"))s.world.putLong("ChaseSeenAt",OtherMotherLoop.now(p));
            if(OtherMotherLoop.now(p)-s.world.getLong("ChaseSeenAt")>=80){s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);s.world.remove("ChaseSeenAt");}
        }
        // Retired dialogue poses and seats must never disable an active pursuit.
        if((s.search==SecretsState.Search.SEARCHING||s.search==SecretsState.Search.INVESTIGATING_NOISE)&&!inSearchRadius(searchCenter(p),s.target)){
            s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);m.getNavigation().stop();
        }
        m.setNoAi(false);m.setNoGravity(false);m.stopRiding();
        m.setMaxUpStep(1.05F);m.getPersistentData().putBoolean("PursuitDriving",true);
        double speed=(s.search==SecretsState.Search.RETURNING_HOME?1.2:2.8)*(1-.35*effective);
        // Only the final, unobstructed contact uses precise player coordinates.
        // Path nodes use block centers, which can otherwise stop just short of a corner.
        if(visible&&s.search==SecretsState.Search.DIRECT_CHASE&&m.getNavigation().isDone()&&m.distanceToSqr(p)<2&&Math.abs(m.getY()-p.getY())<.5)
            m.getMoveControl().setWantedPosition(p.getX(),p.getY(),p.getZ(),speed);
        if(m.tickCount%10==0){
            if(m.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}
            m.getNavigation().setMaxVisitedNodesMultiplier(4);
            m.getNavigation().setSpeedModifier(speed);
            var destination=s.target;var path=m.getNavigation().getPath();
            // Keep the path around corners; do not steer through walls toward a nearby player.
            // A higher step height lets her traverse full blocks without repeated jump stalls.
            var memory=m.getPersistentData();
            if(path==null||path.isDone()||!memory.contains("PursuitGoal")||BlockPos.of(memory.getLong("PursuitGoal")).distSqr(destination)>4){
                var waypoint=destination;
                if(m.distanceToSqr(destination.getCenter())>32*32){
                    var step=m.position().add(destination.getCenter().subtract(m.position()).normalize().scale(20));
                    var nearby=Reconciliation.safe(p,BlockPos.containing(step));waypoint=nearby==null?BlockPos.containing(step):nearby;
                }
                m.getNavigation().moveTo(waypoint.getX()+.5,waypoint.getY(),waypoint.getZ()+.5,speed);memory.putLong("PursuitGoal",destination.asLong());
            }
            recoverPursuit(m,p,destination);
        }
        if(s.search!=SecretsState.Search.DIRECT_CHASE&&(m.distanceToSqr(s.target.getCenter())<5||m.tickCount%240==0&&m.getNavigation().isDone())){
            if(s.search==SecretsState.Search.RETURNING_HOME){if(m.distanceToSqr(s.target.getCenter())<5)s.search=SecretsState.Search.NORMAL;return true;}
            s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);
        }return true;
    }
    /** Refresh only a small expiring entity-ticking lease, never the whole route/world. */
    public static void holdSearch(ServerPlayer p){
        if(p.tickCount%20!=0)return;var s=SecretTransactions.state(p);var scene=OtherMotherLoop.scene(p);if(s==null||scene==null)return;
        var level=p.serverLevel();var old=s.world.contains("PursuitTicket")?BlockPos.of(s.world.getLong("PursuitTicket")):null;
        if(!s.searching()||MotherGoesOut.unavailable(p)){
            if(old!=null){level.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(old),2,old);s.world.remove("PursuitTicket");}return;
        }
        var m=mother(p);var at=m==null?scene.loop.motherPos:m.blockPosition();if(at==null||m==null&&(scene.mom==null||at.equals(BlockPos.ZERO)))return;
        var chunk=new net.minecraft.world.level.ChunkPos(at);var key=new BlockPos(chunk.getMiddleBlockX(),0,chunk.getMiddleBlockZ());
        if(old!=null&&!old.equals(key))level.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(old),2,old);
        level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,chunk,2,key);level.getChunk(chunk.x,chunk.z);s.world.putLong("PursuitTicket",key.asLong());
    }
    public static boolean touching(ParentEntity m,ServerPlayer p){return m.getBoundingBox().inflate(.08,0,.08).intersects(p.getBoundingBox())&&m.hasLineOfSight(p);}
    static boolean visible(ParentEntity m,ServerPlayer p,double range){
        var s=SecretTransactions.state(p);if(s==null||s.invisibleUntil>OtherMotherLoop.now(p)||p.isInvisible()||m.distanceToSqr(p)>range*range||!m.hasLineOfSight(p))return false;
        var delta=p.getEyePosition().subtract(m.getEyePosition());double length=delta.horizontalDistance();if(length<.01)return true;
        double yaw=Math.toRadians(m.getYHeadRot());
        return (-Math.sin(yaw)*delta.x+Math.cos(yaw)*delta.z)/length>=.5; // 120-degree forward cone.
    }
    public static void startChase(ServerPlayer p){
        MotherInterrupts.interrupt(p);
        remember(p);
        var m=mother(p);if(m!=null){m.setNoAi(false);m.setNoGravity(false);m.stopRiding();m.getNavigation().moveTo(p,2.8);}
        SecretTransactions.checkpoint(p);
    }
    private static void remember(ServerPlayer p){var s=SecretTransactions.state(p);s.world.remove("SpyReportOrigin");s.search=SecretsState.Search.DIRECT_CHASE;s.target=p.blockPosition();s.world.putLong("ChaseSeenAt",OtherMotherLoop.now(p));}
    static BlockPos patrolTarget(ServerPlayer p){
        var s=SecretTransactions.state(p);var r=SecretWorld.realm(p);var rooms=SecretWorld.rooms(s);
        var center=searchCenter(p);
        int patrol=s.world.getInt("PatrolIndex");s.world.putInt("PatrolIndex",patrol+1);
        // Search a moving local area, without targeting the player's exact hiding place.
        for(int attempt=0;attempt<12;attempt++){
            BlockPos candidate;
            if(attempt==0&&!rooms.isEmpty()&&patrol%2==0)candidate=BlockPos.of(rooms.get(Math.floorMod(patrol/2,rooms.size())).getLong("Pos")).north(7);
            else {double angle=(patrol%8)*Math.PI/4+(p.getRandom().nextDouble()-.5)*Math.PI/4;double radius=16+103*p.getRandom().nextDouble();
                int x=center.getX()+(int)(Math.cos(angle)*radius),z=center.getZ()+(int)(Math.sin(angle)*radius);
                if(!p.serverLevel().hasChunkAt(new BlockPos(x,p.getBlockY(),z)))continue;
                int y=SecretWorld.ground(p.serverLevel(),x,z);if(y==Integer.MIN_VALUE)continue;candidate=new BlockPos(x,y,z);
            }
            if(!inSearchRadius(center,candidate)||!p.serverLevel().hasChunkAt(candidate)||!SecretWorld.terrainCopied(r,candidate))continue;var safe=Reconciliation.safe(p,candidate);if(safe!=null&&inSearchRadius(center,safe))return safe;
        }
        var m=mother(p);
        if(m!=null&&inSearchRadius(center,m.blockPosition()))return m.blockPosition();
        // A nearby waypoint lets navigation approach the search area; do not
        // send her to the distant house or reveal the player's precise position.
        return center.offset(8,0,8);
    }
    static BlockPos searchCenter(ServerPlayer p){var s=SecretTransactions.state(p);return s.world.contains("SpyReportOrigin")&&(s.search==SecretsState.Search.SEARCHING||s.search==SecretsState.Search.INVESTIGATING_NOISE)?BlockPos.of(s.world.getLong("SpyReportOrigin")):p.blockPosition();}
    static boolean inSearchRadius(BlockPos center,BlockPos target){long dx=(long)target.getX()-center.getX(),dz=(long)target.getZ()-center.getZ();return dx*dx+dz*dz<=120L*120;}
    static void recoverPursuit(ParentEntity m,ServerPlayer p,BlockPos destination){
        if(SecretTransactions.state(p).search==SecretsState.Search.DIRECT_CHASE)return;
        var n=m.getPersistentData();long now=OtherMotherLoop.now(p);
        if(!n.contains("PursuitX")||now-n.getLong("PursuitCheck")>40){
            n.putDouble("PursuitX",m.getX());n.putDouble("PursuitY",m.getY());n.putDouble("PursuitZ",m.getZ());n.putInt("PursuitStuck",0);
        }
        double moved=m.distanceToSqr(n.getDouble("PursuitX"),n.getDouble("PursuitY"),n.getDouble("PursuitZ"));
        if(moved>.25||m.distanceToSqr(destination.getCenter())<5){
            n.putDouble("PursuitX",m.getX());n.putDouble("PursuitY",m.getY());n.putDouble("PursuitZ",m.getZ());n.putInt("PursuitStuck",0);
        }else n.putInt("PursuitStuck",n.getInt("PursuitStuck")+10);
        n.putLong("PursuitCheck",now);
        if(n.getInt("PursuitStuck")<100)return;
        // Bounded recovery only after five seconds without progress; no excavation,
        // player teleport, or forced catch. Validate solid ground and empty space.
        var delta=destination.getCenter().subtract(m.position());var step=m.position().add(delta.normalize().scale(Math.min(6,delta.length())));
        var safe=Reconciliation.safe(p,BlockPos.containing(step));
        if(safe!=null&&m.distanceToSqr(safe.getCenter())>1&&p.distanceToSqr(safe.getCenter())>16){m.getNavigation().stop();m.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}
        n.putInt("PursuitStuck",0);
    }
    private MotherSearch(){}
}
