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
    public static ParentEntity mother(ServerPlayer p){var scene=OtherMotherLoop.scene(p);return scene!=null&&scene.mom!=null&&p.serverLevel().getEntity(scene.mom) instanceof ParentEntity m?m:null;}
    public static void missed(ServerPlayer p){var s=SecretTransactions.state(p);s.search=SecretsState.Search.MISSED_ACTIVITY_PENDING;s.pendingTicks=1200+p.getRandom().nextInt(10801);SecretTransactions.dirty(p);}
    public static void attendanceExpired(ServerPlayer p){var s=SecretTransactions.state(p);if(s.world.getBoolean("LateSearchScheduled"))return;s.world.putBoolean("LateSearchScheduled",true);if(!s.searching())missed(p);SecretTransactions.dirty(p);}
    public static void attended(ServerPlayer p){var s=SecretTransactions.state(p);s.search=SecretsState.Search.NORMAL;s.pendingTicks=0;s.searchTicks=0;s.world.remove("LateSearchScheduled");var m=mother(p);if(m!=null)m.getNavigation().stop();SecretTransactions.dirty(p);}
    public static void noise(ServerPlayer p,BlockPos at,double range,String source){if(Reconciliation.active(p))return;var s=SecretTransactions.state(p);var m=mother(p);if(s==null||m==null)return;p.serverLevel().playSound(null,at,net.minecraft.sounds.SoundEvents.BELL_BLOCK,net.minecraft.sounds.SoundSource.BLOCKS,2,.65F);
        boolean blocked=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(m.getEyePosition(),at.getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,m)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS;
        if(m.distanceToSqr(at.getCenter())>Math.pow(range*(blocked?.65:1),2))return;s.search=SecretsState.Search.INVESTIGATING_NOISE;s.target=at.immutable();s.world.putString("NoiseSource",source);s.world.putLong("NoiseTime",OtherMotherLoop.now(p));s.world.putDouble("NoiseRange",range);s.world.putLong("NoisePosition",at.asLong());SecretTransactions.dirty(p);
    }
    public static void observed(ServerPlayer p,String id,BlockPos at){if(Reconciliation.active(p))return;var s=SecretTransactions.state(p);var m=mother(p);if(s==null||m==null||s.invisibleUntil>OtherMotherLoop.now(p)||p.isInvisible())return;double sight=30*(1-.5*s.effective(2))*(1-s.soulWeakening());if(m.distanceToSqr(p)>sight*sight||!m.hasLineOfSight(p))return;if(SecretRegistry.ALL.containsKey(id))s.exposed.add(id);catchSeenPlayer(p);SecretTransactions.checkpoint(p);}
    public static void bell(ServerPlayer p){var s=SecretTransactions.state(p);if(s==null||!s.searching()||SecretDialogue.busy(p)||!Reconciliation.permitted(p))return;var state=SecretDialogue.state(p);var scene=OtherMotherLoop.scene(p);OtherMotherLoop.relationship(p,scene.loop.trust-.025F,scene.loop.suspicion);Reconciliation.begin(p,Reconciliation.Category.MINOR,state,true);}
    public static void catchPlayer(ServerPlayer p){catchPlayer(p,false);}
    public static void catchSeenPlayer(ServerPlayer p){
        var s=SecretTransactions.state(p);if(s==null)return;
        var m=mother(p);var r=SecretWorld.realm(p);
        boolean forbidden=r!=null&&r.house!=null&&(com.coraline.otherworld.OtherWorldEstate.outsideDistance(r.house,p.blockPosition())>=40||SecretDialogue.secretArea(s,p.blockPosition()));
        if(!s.searching()&&forbidden){if(m!=null&&visible(m,p,96))startChase(p);return;}
        if(s.searching()){if(m!=null&&visible(m,p,30)){remember(p);if(touching(m,p))catchPlayer(p,false);}return;}
        catchPlayer(p,true);
    }
    private static void catchPlayer(ServerPlayer p,boolean seen){var s=SecretTransactions.state(p);var scene=OtherMotherLoop.scene(p);var r=SecretWorld.realm(p);if(s==null||r==null||Reconciliation.active(p)||s.world.getLong("CatchUntil")>OtherMotherLoop.now(p))return;var mother=mother(p);if(mother==null||mother.distanceToSqr(p)>(seen?96*96:36)||!mother.hasLineOfSight(p)||s.searching()&&!touching(mother,p))return;if(s.world.getBoolean("CaughtEncounter"))return;var dialogueState=SecretDialogue.state(p);MotherInterrupts.interrupt(p);s.world.putBoolean("CaughtEncounter",true);
        var evidence=new java.util.HashSet<String>();for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);String id=SecretTransactions.evidence(p,item);if(!id.isEmpty())evidence.add(id);if(item.is(SecretsItems.KEY.get())&&SecretTransactions.owned(p,item))evidence.add("personal_keyring");}double severity=SecretRegistry.severity(evidence);s.exposed.addAll(evidence);int room=SecretWorld.roomAt(s,p.blockPosition());double area=room<0?0:(SecretWorld.rooms(s).get(room).getInt("Tier")+1)*.1;
        OtherMotherLoop.relationship(p,scene.loop.trust-(float)(.025+Math.min(.18,Math.sqrt(p.distanceToSqr(r.house.getCenter()))/1000)+severity*.15+area*.15),scene.loop.suspicion+(float)(severity*SecretsConfig.CATCH_SEVERITY.get()+area*.2));s.search=SecretsState.Search.NORMAL;s.world.putLong("CatchUntil",OtherMotherLoop.now(p)+200);s.target=r.porch;
        SecretDialogue.confront(p,SecretDialogue.caught(!evidence.isEmpty(),SecretDialogue.secretArea(s,p.blockPosition())),dialogueState);SecretTransactions.dirty(p);
    }
    public static void tick(ServerPlayer p){var s=SecretTransactions.state(p);if(s.world.getBoolean("CaughtEncounter")){var watcher=mother(p);int away=watcher==null||watcher.distanceToSqr(p)>144||!watcher.hasLineOfSight(p)?s.world.getInt("EncounterAway")+1:0;s.world.putInt("EncounterAway",away);if(away>=60){s.world.remove("CaughtEncounter");s.world.remove("EncounterAway");}}if(p.tickCount%200==0&&!SecretDialogue.busy(p)&&!s.searching()&&!s.exposed.isEmpty()&&s.world.getLong("RemarkAfter")<OtherMotherLoop.now(p)){var m=mother(p);if(m!=null&&m.distanceToSqr(p)<64){String remark=s.exposed.contains("soul_prisons")?"Some doors are closed for their own good. Leave them closed.":s.exposed.contains("soulglass")?"There are things here too fragile for curious hands.":s.exposed.contains("spying_dolls")?"You have been watching everything so carefully. So have I.":s.counter(5)>0?"Perhaps my presents have spoiled you.":s.counter(1)>0?"Is there something missing from what I have given you?":"You needn't concern yourself with every locked door.";VisitAudio.caption(p,"Other Mother",remark,120);s.world.putLong("RemarkAfter",OtherMotherLoop.now(p)+2400);}}if(s.search==SecretsState.Search.MISSED_ACTIVITY_PENDING&&--s.pendingTicks<=0){s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);}
        if(s.searching()&&++s.searchTicks%400==0){var l=OtherMotherLoop.scene(p).loop;OtherMotherLoop.relationship(p,l.trust,l.suspicion+.01F);}if(p.tickCount%20==0)OtherMotherLoop.changed(p);}
    public static boolean control(ParentEntity m,ServerPlayer p){if(!SecretsRuntime.active(p))return false;var s=SecretTransactions.state(p);if(Reconciliation.control(m,p))return true;if(SecretDialogue.busy(p)&&!s.searching()){m.getNavigation().stop();m.getLookControl().setLookAt(p,30,30);return true;}if(!s.searching()&&s.search!=SecretsState.Search.RETURNING_HOME)return false;
        double effective=s.effective(2),range=30*(1-.5*effective)*(1-s.soulWeakening());boolean visible=visible(m,p,range);
        if(s.searching()&&touching(m,p)){catchPlayer(p);if(Reconciliation.active(p))return true;}
        if(visible&&s.searching()){remember(p);m.getLookControl().setLookAt(p,30,30);}
        if(s.search==SecretsState.Search.DIRECT_CHASE&&!visible){
            if(!s.world.contains("ChaseSeenAt"))s.world.putLong("ChaseSeenAt",OtherMotherLoop.now(p));
            if(OtherMotherLoop.now(p)-s.world.getLong("ChaseSeenAt")>=80){s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);s.world.remove("ChaseSeenAt");}
        }
        // Retired dialogue poses and seats must never disable an active pursuit.
        if((s.search==SecretsState.Search.SEARCHING||s.search==SecretsState.Search.INVESTIGATING_NOISE)&&!inSearchRadius(p.blockPosition(),s.target)){
            s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);m.getNavigation().stop();
        }
        m.setNoAi(false);m.setNoGravity(false);m.stopRiding();
        if(visible&&s.search==SecretsState.Search.DIRECT_CHASE&&m.distanceToSqr(p)<9)m.getMoveControl().setWantedPosition(p.getX(),p.getY(),p.getZ(),2.8*(1-.35*effective));
        if(m.tickCount%10==0){if(m.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}var destination=s.target;var delta=destination.getCenter().subtract(m.position());if(delta.horizontalDistance()>18){var step=m.position().add(new Vec3(delta.x,0,delta.z).normalize().scale(16));var safe=Reconciliation.safe(p,BlockPos.containing(step));if(safe!=null)destination=safe;}m.getNavigation().moveTo(destination.getX()+.5,destination.getY(),destination.getZ()+.5,(s.search==SecretsState.Search.RETURNING_HOME?1.2:2.8)*(1-.35*effective));recoverPursuit(m,p,destination);}
        if(s.search!=SecretsState.Search.DIRECT_CHASE&&(m.distanceToSqr(s.target.getCenter())<5||m.tickCount%240==0&&m.getNavigation().isDone())){
            if(s.search==SecretsState.Search.RETURNING_HOME){if(m.distanceToSqr(s.target.getCenter())<5)s.search=SecretsState.Search.NORMAL;return true;}
            s.search=SecretsState.Search.SEARCHING;s.target=patrolTarget(p);
        }return true;
    }
    static boolean touching(ParentEntity m,ServerPlayer p){return m.getBoundingBox().inflate(.08,0,.08).intersects(p.getBoundingBox())&&m.hasLineOfSight(p);}
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
    private static void remember(ServerPlayer p){var s=SecretTransactions.state(p);s.search=SecretsState.Search.DIRECT_CHASE;s.target=p.blockPosition();s.world.putLong("ChaseSeenAt",OtherMotherLoop.now(p));}
    static BlockPos patrolTarget(ServerPlayer p){
        var s=SecretTransactions.state(p);var r=SecretWorld.realm(p);var rooms=SecretWorld.rooms(s);
        int patrol=s.world.getInt("PatrolIndex");s.world.putInt("PatrolIndex",patrol+1);
        // Search a moving local area, without targeting the player's exact hiding place.
        for(int attempt=0;attempt<12;attempt++){
            BlockPos candidate;
            if(attempt==0&&!rooms.isEmpty()&&patrol%2==0)candidate=BlockPos.of(rooms.get(Math.floorMod(patrol/2,rooms.size())).getLong("Pos")).north(7);
            else {double angle=(patrol%8)*Math.PI/4+(p.getRandom().nextDouble()-.5)*Math.PI/4;double radius=16+103*p.getRandom().nextDouble();
                int x=p.blockPosition().getX()+(int)(Math.cos(angle)*radius),z=p.blockPosition().getZ()+(int)(Math.sin(angle)*radius);
                if(!p.serverLevel().hasChunkAt(new BlockPos(x,p.getBlockY(),z)))continue;
                int y=SecretWorld.ground(p.serverLevel(),x,z);if(y==Integer.MIN_VALUE)continue;candidate=new BlockPos(x,y,z);
            }
            if(!inSearchRadius(p.blockPosition(),candidate)||!p.serverLevel().hasChunkAt(candidate)||!SecretWorld.terrainCopied(r,candidate))continue;var safe=Reconciliation.safe(p,candidate);if(safe!=null&&inSearchRadius(p.blockPosition(),safe))return safe;
        }
        var m=mother(p);
        if(m!=null&&inSearchRadius(p.blockPosition(),m.blockPosition()))return m.blockPosition();
        // A nearby waypoint lets navigation approach the search area; do not
        // send her to the distant house or reveal the player's precise position.
        return p.blockPosition().offset(8,0,8);
    }
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
