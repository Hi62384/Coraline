package com.coraline.feast;

import static com.coraline.feast.RescueChapter.Phase.*;
import java.util.*;
import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;

/** Saved combat; only the owner can reveal or strike the actual exposed weakness. */
public final class RescueBosses {
    private static final Map<UUID,ServerBossEvent> BARS=new HashMap<>();
    public static boolean nearArena(ServerPlayer p,VisitData.Scene scene){return scene!=null&&RescueChapter.started(scene)&&fighting(RescueChapter.phase(scene))&&p.position().distanceToSqr(RescueEncounters.center(p,scene))<64*64;}
    private static java.util.List<ServerPlayer> players(ServerPlayer director,VisitData.Scene scene){
        var list=com.coraline.multiplayer.StorySessionData.get(director.server).participants(director.server,scene.owner,true);
        if(list.isEmpty())return director.isAlive()&&!director.isSpectator()?java.util.List.of(director):java.util.List.of();
        return list.stream().filter(p->p.isAlive()&&!p.isSpectator()&&p.serverLevel()==director.serverLevel()).toList();
    }
    public static boolean fighting(RescueChapter.Phase state){return state==MANTIS_FIGHT||state==THEATRE_FIGHT||state==SWARM_FIGHT;}
    public static void hideBar(ServerPlayer p){var bar=BARS.remove(p.getUUID());if(bar!=null)bar.removeAllPlayers();}
    public static void begin(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state){var n=RescueChapter.data(s);n.putFloat("BossHealth",state==MANTIS_FIGHT?48:state==THEATRE_FIGHT?240:216);n.putFloat("BossMaximum",state==MANTIS_FIGHT?48:state==THEATRE_FIGHT?240:216);n.putFloat("SpinkHealth",120);n.putFloat("ForcibleHealth",120);n.remove("WeakUntil");n.remove("HitAt");n.remove("SocketCycle");n.remove("SocketTier");n.remove("MantisAttemptSpent");n.remove("TheatreCycle");n.putInt("BossCycleStart",0);n.putInt("BossLastTier",0);n.putInt("WeakCycle",-1);RescueChapter.go(p,s,state);}
    public static void tick(ServerPlayer p,VisitData.Scene s,int time){
        var n=RescueChapter.data(s);var state=RescueChapter.phase(s);
        if(state==MANTIS_FIGHT&&n.getFloat("BossMaximum")>48){float fraction=n.getFloat("BossHealth")/n.getFloat("BossMaximum");n.putFloat("BossMaximum",48);n.putFloat("BossHealth",48*fraction);}
        float hp=n.getFloat("BossHealth"),maximum=n.getFloat("BossMaximum");int tier=RescueChallengeMath.tier(hp,maximum);
        // Entering a faster health tier starts a fresh telegraph, not the middle
        // of an attack computed with a suddenly shorter modulo period.
        if(n.getInt("BossLastTier")!=tier){n.putInt("BossLastTier",tier);n.putInt("BossCycleStart",time);if(state!=MANTIS_FIGHT)n.remove("WeakUntil");}int clock=Math.max(0,time-n.getInt("BossCycleStart")),beat=RescueChallengeMath.beat(clock,tier),pattern=RescueChallengeMath.attack(clock,tier,state==MANTIS_FIGHT?4:5),cycle=clock/(state==MANTIS_FIGHT?240:180-tier*20);
        if(state==MANTIS_FIGHT){beat=clock%240;pattern=(clock/240)%4;}
        if(state==THEATRE_FIGHT){if(n.getFloat(n.getInt("WeakActress")==0?"SpinkHealth":"ForcibleHealth")<=0)n.putInt("WeakActress",1-n.getInt("WeakActress"));cycle=clock/300;beat=clock%300;pattern=cycle%5;
            if(!n.contains("TheatreCycle")||n.getInt("TheatreCycle")!=cycle){n.putInt("TheatreCycle",cycle);n.putInt("WeakActress",n.getFloat("SpinkHealth")<=0?1:n.getFloat("ForcibleHealth")<=0?0:p.getRandom().nextInt(2));n.putInt("TheatreSocket",p.getRandom().nextInt(4));}
            if(beat>=200)n.putLong("WeakUntil",p.serverLevel().getGameTime()+300-beat);else n.remove("WeakUntil");
        }
        if(state==MANTIS_FIGHT){
            // Every recovery grants one four-second attempt, independently of
            // whether the player has already located the invisible socket.
            if(!n.contains("SocketCycle")||n.getInt("SocketCycle")!=cycle||n.getInt("SocketTier")!=tier){
                n.putInt("WeakSocket",Math.floorMod(n.getInt("WeakSocket")+1+p.getRandom().nextInt(3),4));
                n.putInt("SocketCycle",cycle);n.putInt("SocketTier",tier);n.remove("WeakUntil");n.putBoolean("MantisAttemptSpent",false);
            }
            if(beat>=100&&beat<180&&!n.getBoolean("MantisAttemptSpent")&&!n.contains("WeakUntil"))n.putLong("WeakUntil",p.serverLevel().getGameTime()+180-beat);
            if(beat>=180)n.remove("WeakUntil");
        }
        n.putInt("BossTier",tier);n.putInt("BossBeat",beat);n.putInt("BossPattern",pattern);n.putBoolean("Vulnerable",state==MANTIS_FIGHT?n.contains("WeakUntil")&&n.getLong("WeakUntil")>=p.serverLevel().getGameTime():state==THEATRE_FIGHT?beat>=200:RescueChallengeMath.vulnerable(clock,tier));
        String name=state==MANTIS_FIGHT?"The Mechanical Mantis":state==THEATRE_FIGHT?"The Other Actresses":"The Ratmaster";
        var bar=BARS.computeIfAbsent(p.getUUID(),id->{var b=new ServerBossEvent(Component.literal(name),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);b.addPlayer(p);return b;});bar.setName(Component.literal(name+" · "+(tier+1)+"/3"));bar.setProgress(Math.max(0,Math.min(1,hp/maximum)));
        Vec3 center=RescueEncounters.center(p,s);boolean group=com.coraline.multiplayer.StorySessionData.get(p.server).hasGuests(s.owner);
        for(var member:players(p,s)){bar.addPlayer(member);var delta=member.position().subtract(center);if(delta.horizontalDistanceSqr()>40*40||member.getY()<center.y-8){if(group)com.coraline.multiplayer.StoryCombatSafety.eliminate(member);else{lose(p,s);return;}}}
        var participants=players(p,s);if(participants.isEmpty()){com.coraline.multiplayer.StoryCombatSafety.restoreGroup(p,s);lose(p,s);return;}
        var aim=participants.get(Math.floorMod(cycle,participants.size()));var toPlayer=aim.position().subtract(center);
        if(beat<=1||state==MANTIS_FIGHT&&beat==35){n.putDouble("AttackX",aim.getX());n.putDouble("AttackZ",aim.getZ());n.putDouble("AttackAngle",Math.atan2(toPlayer.z,toPlayer.x));}
        var locked=new Vec3(n.getDouble("AttackX"),aim.getY(),n.getDouble("AttackZ"));
        Vec3 weak=state==MANTIS_FIGHT?mantis(p,s,time,3):state==THEATRE_FIGHT?actresses(p,s,time,false):swarm(p,s,time,pattern,beat);
        if(weak!=null){n.putDouble("WeakX",weak.x);n.putDouble("WeakY",weak.y);n.putDouble("WeakZ",weak.z);
            var eye=RescueEncounters.prop(p,s,"WeakPoint",state==MANTIS_FIGHT||state==THEATRE_FIGHT?RescueProps.CONTROL_POINT:RescueProps.EYE,state==MANTIS_FIGHT?0:state==THEATRE_FIGHT?1:2,weak);if(eye!=null){eye.setPos(weak);eye.frame(n.getBoolean("Vulnerable")&&n.getLong("WeakUntil")>=p.serverLevel().getGameTime()?1:0,time,0);}
            for(var member:participants)if(state==SWARM_FIGHT&&RescueEncounters.seeing(member)&&RescueEncounters.gaze(member,weak,30,.975)){n.putLong("WeakUntil",p.serverLevel().getGameTime()+180);n.putInt("WeakCycle",cycle);}
        }
        if(state==MANTIS_FIGHT&&(beat==0||beat==40||beat==60)&&(!n.contains("MantisSoundClock")||n.getInt("MantisSoundClock")!=clock)){
            n.putInt("MantisSoundClock",clock);var sound=beat==0?net.minecraft.sounds.SoundEvents.IRON_GOLEM_STEP:beat==40?net.minecraft.sounds.SoundEvents.IRON_GOLEM_ATTACK:net.minecraft.sounds.SoundEvents.ANVIL_LAND;
            p.serverLevel().playSound(null,BlockPos.containing(center),sound,net.minecraft.sounds.SoundSource.HOSTILE,.75F,beat==60?.8F:1.1F);
        }
        if(beat<40){if(state!=MANTIS_FIGHT)RescueChapter.hint(p,warning(state,pattern));if(time%5==0)telegraph(p,locked,pattern,center,n.getDouble("AttackAngle"));}
        if(state==THEATRE_FIGHT?beat<200&&beat%10==0:beat==60||tier==2&&beat==82)for(var member:participants){boolean hit=state==THEATRE_FIGHT?actressContact(p,s,member):hazard(member,state,pattern,center,locked,n.getDouble("AttackAngle"));if(hit&&member.invulnerableTime<=0&&RescueCombat.bossHarm(member,pattern==1?10:8)){member.setDeltaMovement(member.getDeltaMovement().add(member.position().subtract(center).multiply(1,0,1).normalize().scale(.18)).add(0,.12,0));}}
        if(state==MANTIS_FIGHT&&beat>=100)RescueChapter.hint(p,n.getBoolean("Vulnerable")?"ATTACK NOW — one weapon strike! Find the yellow socket with the Seeing Stone.":"Opening closed. Dodge the next attack, then try the yellow socket again.");
        else if(beat>=100)RescueChapter.hint(p,n.getLong("WeakUntil")>=p.serverLevel().getGameTime()?"Strike the revealed weak point while it is exposed!":"Raise the Seeing Stone to find the exposed control point.");
        if(hp<=0){com.coraline.multiplayer.StoryCombatSafety.restoreGroup(p,s);hideBar(p);RescueProps.hide(p,s,"WeakPoint");if(state==MANTIS_FIGHT){RescueEyes.eye(s,0).putBoolean("Defeated",true);RescueChapter.go(p,s,MANTIS_RELEASE);}else if(state==THEATRE_FIGHT){RescueEyes.eye(s,1).putBoolean("Defeated",true);n.putBoolean("TheatreDead",true);actresses(p,s,time,true);RescueChapter.go(p,s,THEATRE_EYE);}else{RescueEyes.eye(s,2).putBoolean("Defeated",true);RescueProps.hide(p,s,"Swarm");RescueEncounters.startChase(p,s);}}
    }
    private static String warning(RescueChapter.Phase phase,int attack){if(phase==MANTIS_FIGHT)return new String[]{"Forearm rising — dodge sideways!","Mantis lowering — clear its charge!","Legs rising — jump the shockwave!","Restraints reaching — move away from the marked spot!"}[attack];if(phase==THEATRE_FIGHT)return new String[]{"Spink reaches — dodge the long hands!","Wrapper opening beneath you — move!","Both actresses rush — find the open side!","Illusions gathering — inspect with the Seeing Stone!","Stage lights converge — leave the marked spot!"}[attack];return new String[]{"Rat wave — jump or move aside!","Rat tower tilting — clear its fall!","Circus balls incoming — dodge!","Swarm closing — find the spectral gap!","False eye carriers — use the Seeing Stone!"}[attack];}
    private static boolean hazard(ServerPlayer p,RescueChapter.Phase phase,int attack,Vec3 c,Vec3 locked,double angle){double distance=p.position().distanceTo(locked),r=p.position().subtract(c).horizontalDistance();double current=Math.atan2(p.getZ()-c.z,p.getX()-c.x),difference=Math.atan2(Math.sin(current-angle),Math.cos(current-angle));return switch(attack){case 0->phase==SWARM_FIGHT?p.onGround()&&Math.abs(p.getZ()-locked.z)<1.8:distance<3.4;case 1->Math.abs(difference)<.30&&r<24;case 2->phase==MANTIS_FIGHT?p.onGround()&&r<20:distance<2.1;case 3->phase==SWARM_FIGHT?r<7&&Math.abs(difference)>.5:distance<3.2;default->distance<3.2;};}
    private static void telegraph(ServerPlayer p,Vec3 locked,int attack,Vec3 center,double angle){for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 q=attack==1?center.add(Math.cos(angle)*i,.15,Math.sin(angle)*i):locked.add(Math.cos(a)*2,.15,Math.sin(a)*2);p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,q.x,q.y,q.z,1,0,0,0,0);}}
    public static boolean hit(ServerPlayer p,Entity target){var s=OtherMotherLoop.scene(p);if(s==null||!RescueChapter.started(s)||!com.coraline.multiplayer.StoryCoop.canAct(p,s)||!fighting(RescueChapter.phase(s)))return false;
        var n=RescueChapter.data(s);boolean ours=target instanceof CinematicPropEntity prop&&prop.owned(s)&&(prop.kind()==RescueProps.MANTIS||prop.kind()==RescueProps.SWARM||prop.kind()==RescueProps.CONTROL_POINT||prop.kind()==RescueProps.EYE)||RescueChapter.phase(s)==MANTIS_FIGHT&&target==ShowActors.actor(p,s,"evil_other_father")||target==ShowActors.actor(p,s,"other_miss_spink")||target==ShowActors.actor(p,s,"other_miss_forcible");if(!ours)return false;
        long now=p.serverLevel().getGameTime();if(p.level()!=target.level()||!withinMeleeReach(p,target.getBoundingBox())||n.contains("HitAt")&&now-n.getLong("HitAt")<15)return true;
        if(!n.getBoolean("Vulnerable")||n.getLong("WeakUntil")<now){RescueChapter.hint(p,"find weak spot with seeing stone");return true;}
        var weak=new Vec3(n.getDouble("WeakX"),n.getDouble("WeakY"),n.getDouble("WeakZ"));
        boolean mantis=RescueChapter.phase(s)==MANTIS_FIGHT;
        if(mantis){n.putBoolean("MantisAttemptSpent",true);n.putBoolean("Vulnerable",false);n.remove("WeakUntil");var point=RescueProps.find(p,s,"WeakPoint");if(point!=null)point.frame(0,point.time(),0);OtherMotherLoop.changed(p);}
        var contact=weakContact(p.getEyePosition(),p.getLookAngle(),weak);
        if(contact.isEmpty()||p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),contact.get(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS){
            if(mantis)RescueChapter.hint(p,"Missed the socket — opening closed. Locate it with the Seeing Stone before your next strike.");return true;
        }
        if(p.getMainHandItem().isEmpty()||p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)<=1.0||p.getAttackStrengthScale(.5F)<.8F){RescueChapter.hint(p,mantis?"find weak spot with seeing stone":"attack now!");return true;}
        n.putLong("HitAt",now);float damage=(float)Math.max(RescueChapter.phase(s)==MANTIS_FIGHT?6:3,Math.min(RescueChapter.phase(s)==MANTIS_FIGHT?20:8,p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)));int tier=RescueChallengeMath.tier(n.getFloat("BossHealth"),n.getFloat("BossMaximum"));
        if(RescueChapter.phase(s)==THEATRE_FIGHT){String key=n.getInt("WeakActress")==1?"ForcibleHealth":"SpinkHealth";n.putFloat(key,Math.max(0,n.getFloat(key)-damage));n.putFloat("BossHealth",n.getFloat("SpinkHealth")+n.getFloat("ForcibleHealth"));}else n.putFloat("BossHealth",Math.max(0,n.getFloat("BossHealth")-damage));
        int nextTier=RescueChallengeMath.tier(n.getFloat("BossHealth"),n.getFloat("BossMaximum"));if(nextTier!=tier&&RescueChapter.phase(s)!=MANTIS_FIGHT)n.remove("WeakUntil");
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,weak.x,weak.y,weak.z,12,.22,.22,.22,.04);p.serverLevel().playSound(null,BlockPos.containing(weak),net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT,net.minecraft.sounds.SoundSource.HOSTILE,.9F,1.2F);p.displayClientMessage(Component.literal("Weak point hit! Health: "+Math.round(n.getFloat("BossHealth"))+" / "+Math.round(n.getFloat("BossMaximum"))),true);OtherMotherLoop.changed(p);return true;
    }
    /** Match ordinary survival melee reach to the surface, not the entity origin. */
    static Optional<Vec3> weakContact(Vec3 eye,Vec3 look,Vec3 weak){
        var box=new AABB(weak.add(-.45,-.45,-.45),weak.add(.45,.45,.45));
        return box.contains(eye)?Optional.of(eye):box.clip(eye,eye.add(look.normalize().scale(3.0)));
    }
    private static boolean withinMeleeReach(ServerPlayer p,AABB box){
        var eye=p.getEyePosition();return eye.distanceToSqr(new Vec3(net.minecraft.util.Mth.clamp(eye.x,box.minX,box.maxX),net.minecraft.util.Mth.clamp(eye.y,box.minY,box.maxY),net.minecraft.util.Mth.clamp(eye.z,box.minZ,box.maxZ)))<=9;
    }
    /** Probe actual collision tops, including slab edges, and skim water surfaces. */
    static Vec3 mantisSurface(ServerLevel level,Vec3 next,double previousY){
        double top=Double.NEGATIVE_INFINITY;
        for(int x=(int)Math.floor(next.x-.3);x<=(int)Math.floor(next.x+.3);x++)
        for(int z=(int)Math.floor(next.z-.3);z<=(int)Math.floor(next.z+.3);z++)
        for(int y=(int)Math.floor(previousY)-2;y<=(int)Math.floor(previousY)+1;y++){
            var cell=new BlockPos(x,y,z);if(!level.hasChunkAt(cell))continue;
            var state=level.getBlockState(cell);var shape=state.getCollisionShape(level,cell);
            double height=shape.isEmpty()?Double.NEGATIVE_INFINITY:y+shape.max(Direction.Axis.Y);
            var fluid=state.getFluidState();if(fluid.is(net.minecraft.tags.FluidTags.WATER))height=Math.max(height,y+fluid.getHeight(level,cell));
            if(height<=previousY+1.05&&height>=previousY-1.05)top=Math.max(top,height);
        }
        return Double.isFinite(top)?new Vec3(next.x,top,next.z):null;
    }
    public static Vec3 mantis(ServerPlayer p,VisitData.Scene s,int time,int action){var n=RescueChapter.data(s);var base=Vec3.atBottomCenterOf(RescueEncounters.garden(p,s));var machine=RescueEncounters.prop(p,s,"Mantis",RescueProps.MANTIS,0,base);if(machine==null)return null;
        Vec3 at=machine.position();if(action==0||action==3&&!n.getBoolean("Vulnerable")){
            var delta=p.position().subtract(at).multiply(1,0,1);double range=delta.length();
            boolean charge=action==3&&n.getInt("BossBeat")>=40&&n.getInt("BossBeat")<70&&n.getInt("BossPattern")==1;
            double speed=action==0?.045:charge?.48:.22;
            var step=delta.normalize().scale(range>(charge?1.5:3.5)?speed:0);
            // The decorative legs span flower beds and kerbs; they must not turn
            // the whole machine into an immovable four-block collision probe.
            for(double turn:new double[]{0,.65,-.65,1.3,-1.3,1.57,-1.57,2.1,-2.1}){
                var next=at.add(step.yRot((float)turn));var cell=BlockPos.containing(next);
                var feet=mantisSurface(p.serverLevel(),next,at.y);if(feet==null)continue;var box=new AABB(next.x-.3,feet.y+.01,next.z-.3,next.x+.3,feet.y+1.7,next.z+.3);
                if(Math.abs(feet.y-at.y)<=1.05&&!p.serverLevel().getBlockCollisions(machine,box).iterator().hasNext()){
                    machine.setPos(next.x,feet.y,next.z);break;
                }
            }
        }
        if(action==2)machine.setPos(machine.getX(),base.y-Math.max(0,time-ShowRecordings.duration("rescue_mantis_take"))/(double)Math.max(1,n.getInt("RetrievalLimit")-ShowRecordings.duration("rescue_mantis_take"))*4,machine.getZ());
        if(action==0||action==3&&!n.getBoolean("Vulnerable")){n.remove("OfferYaw");machine.setYRot((float)Math.toDegrees(Math.atan2(p.getX()-machine.getX(),machine.getZ()-p.getZ())));}
        else{if(!n.contains("OfferYaw"))n.putFloat("OfferYaw",machine.getYRot());machine.setYRot(n.getFloat("OfferYaw"));}RescueProps.frame(machine,action,time,n.getFloat("BossHealth")/Math.max(1,n.getFloat("BossMaximum")));
        var father=ShowActors.acquire(p,s,"evil_other_father",p.serverLevel(),machine.position().add(0,1.1,0));if(father!=null){father.setInvisible(action==2&&time>n.getInt("RetrievalLimit")-20);ShowActors.position(father,machine.position().add(0,1.1,0),machine.getYRot()+180,action==2?81:80,time,1);}
        double yaw=Math.toRadians(machine.getYRot());int socket=n.getInt("WeakSocket");
        double angle=yaw+(socket-1.5)*.8;
        Vec3 weak=action==2?machine.position().add(Math.sin(yaw)*1.15,1.8,-Math.cos(yaw)*1.15):machine.position().add(Math.sin(angle)*1.65,1.25+(socket%2)*.35,-Math.cos(angle)*1.65);if(action==2){var eye=RescueEncounters.prop(p,s,"GardenEye",RescueProps.EYE,0,weak);if(eye!=null){eye.setPos(weak);eye.frame(1,time,1);eye.setInvisible(RescueEyes.eye(s,0).getBoolean("Claimed"));}}return weak;
    }
    public static Vec3 actresses(ServerPlayer p,VisitData.Scene s,int time,boolean scripted){
        var n=RescueChapter.data(s);var center=RescueEncounters.center(p,s);Vec3 weak=null;int pattern=n.getInt("BossPattern");
        for(int side:new int[]{-1,1}){
            String id=side<0?"other_miss_spink":"other_miss_forcible",hp=side<0?"SpinkHealth":"ForcibleHealth";
            var actor=ShowActors.acquire(p,s,id,p.serverLevel(),center.add(side*4,0,0));if(actor==null)continue;
            boolean exposed=n.getBoolean("Vulnerable");
            double angle=time*(exposed?.24:.13)+side*Math.PI+Math.sin(time*.031+side)*.8,radius=exposed?3.2:4+Math.sin(time*.061+side)*2.5;
            var target=p.position().subtract(center).multiply(1,0,1);if(target.length()>16)target=target.normalize().scale(16);
            var orbit=scripted?center:center.add(target);
            var at=orbit.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);if(scripted)at=center.add(side*1.1,0,0);
            var safe=OtherWorld.safePlayer(p.serverLevel(),BlockPos.containing(at));if(safe!=null)at=new Vec3(at.x,safe.getY(),at.z);
            boolean down=n.contains(hp)&&n.getFloat(hp)<=0;
            if(down){at=actor.position();}
            ShowActors.position(actor,at,down?actor.getYRot():scripted?(float)Math.toDegrees(Math.atan2(p.getX()-at.x,at.z-p.getZ())):(float)(time*32+side*90),down?84:83,time,1);
            if(scripted&&!down)actor.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,p.getEyePosition());
            if(!down&&n.getInt("WeakActress")== (side<0?0:1)){double socketAngle=Math.toRadians(actor.getYRot())+n.getInt("TheatreSocket")*Math.PI/2;weak=at.add(Math.sin(socketAngle)*.65,1.1,-Math.cos(socketAngle)*.65);}
        }
        if(scripted&&RescueChapter.phase(s)==THEATRE_GRAB&&time<35){var delta=center.subtract(p.position()).multiply(1,0,1);if(delta.lengthSqr()>4){p.setDeltaMovement(delta.normalize().scale(.06));p.hurtMarked=true;}}
        return weak;
    }
    private static boolean actressContact(ServerPlayer director,VisitData.Scene s,ServerPlayer target){
        for(String id:new String[]{"other_miss_spink","other_miss_forcible"}){var actor=ShowActors.actor(director,s,id);String hp=id.equals("other_miss_spink")?"SpinkHealth":"ForcibleHealth";if(actor!=null&&RescueChapter.data(s).getFloat(hp)>0&&actor.position().distanceToSqr(target.position())<2.8*2.8)return true;}return false;
    }
    private static Vec3 swarm(ServerPlayer p,VisitData.Scene s,int time,int attack,int beat){var center=RescueEncounters.center(p,s);var prop=RescueEncounters.prop(p,s,"Swarm",RescueProps.SWARM,0,center);RescueProps.frame(prop,attack,time,beat/180F);double angle=time*.016;var weak=center.add(Math.cos(angle)*4,.6,Math.sin(angle)*4);if(prop!=null)prop.grabTarget(p.position().subtract(center));return weak;}
    public static void lose(ServerPlayer p,VisitData.Scene s){
        hideBar(p);com.coraline.multiplayer.StoryCombatSafety.restoreGroup(p,s);
        if(!AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS)){
            RescueChapter.data(s).putBoolean("BossLossPending",true);OtherMotherLoop.changed(p);
            FirstEscapeStory.hint(p,"Other Mother is preparing the button-eye procedure.");
        }else RescueChapter.data(s).remove("BossLossPending");
    }
    public static void retry(ServerPlayer p,VisitData.Scene s,String reason){hideBar(p);FirstEscapeStory.hint(p,reason);var state=RescueChapter.phase(s);p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);p.clearFire();var at=BlockPos.containing(RescueEncounters.center(p,s)).south(10);if(RescueChapter.transfer(p,p.serverLevel(),at))begin(p,s,state);}
    private RescueBosses(){}
}
