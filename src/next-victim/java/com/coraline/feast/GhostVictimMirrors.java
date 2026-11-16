package com.coraline.feast;

import java.util.List;
import java.util.UUID;
import com.coraline.secrets.SecretsNetwork;
import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative adapter for the existing conversation screen. Mirrors
 * must have been physically placed by the owner's director; a client cannot
 * supply coordinates, summon a distant child or reuse a stale conversation. */
public final class GhostVictimMirrors {
    private static final List<String> OPTIONS=List.of("Hello. You can trust me.",
        "I am trapped here.","Please help me.","Think about what you saw.",
        "She is hiding something from you.","You should be afraid of me.","Leave the mirror");
    private static final GhostVictimProgress.Dialogue[] CHOICES={
        GhostVictimProgress.Dialogue.FRIENDLY,GhostVictimProgress.Dialogue.TRAPPED,
        GhostVictimProgress.Dialogue.HELP,GhostVictimProgress.Dialogue.EVIDENCE,
        GhostVictimProgress.Dialogue.ACCUSATION,GhostVictimProgress.Dialogue.THREAT,
        GhostVictimProgress.Dialogue.LEAVE};
    public static boolean owned(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        return s!=null&&s.owner.equals(p.getUUID())&&n.getBoolean("Started")&&n.hasUUID("Session")
            &&n.hasUUID("Owner")&&s.owner.equals(n.getUUID("Owner"))
            &&n.hasUUID("Door")&&s.door.equals(n.getUUID("Door"));
    }
    public static void register(CompoundTag n,String id,BlockPos at,float yaw){
        if(id==null||!id.matches("[a-z_]{1,40}")||at==null||!Float.isFinite(yaw))throw new IllegalArgumentException("Invalid authored mirror");
        var all=n.getCompound("Mirrors");var mirror=new CompoundTag();
        mirror.putLong("Position",at.asLong());mirror.putFloat("Yaw",yaw);
        all.put(id,mirror);n.put("Mirrors",all);
    }
    private static CompoundTag point(CompoundTag n,String id){return n.getCompound("Mirrors").getCompound(id);}
    public static boolean childPresent(ServerPlayer p,CompoundTag n,String id,TycoonNpc child){
        var mirror=point(n,id);
        if(!com.coraline.otherworld.OtherWorld.isOther(p.level())||child==null||!child.isAlive()||child.isInvisible()||child.level()!=p.level()
            ||!GhostVictimProgress.hasVictim(n)||child.owner==null||!child.owner.equals(p.getUUID())
            ||!GhostVictimProgress.child(n).getUUID("Id").equals(child.resident)
            ||!n.hasUUID("VictimActor")||!child.getUUID().equals(n.getUUID("VictimActor"))
            ||!mirror.contains("Position"))return false;
        Vec3 center=Vec3.atBottomCenterOf(BlockPos.of(mirror.getLong("Position"))).add(0,1.35,0);
        Vec3 delta=center.subtract(child.getEyePosition());
        // STORY_MIRROR's authored face is local -Z, rotated by -entity yaw.
        Vec3 normal=Vec3.directionFromRotation(0,mirror.getFloat("Yaw")).scale(-1);
        if(delta.lengthSqr()>25||delta.lengthSqr()<.01||p.distanceToSqr(center)>16
            ||child.position().subtract(center).dot(normal)<.1)return false;
        if(child.getLookAngle().dot(delta.normalize())<Math.cos(Math.toRadians(65)))return false;
        // Ray ends just in front of the surface, so the authored backing wall
        // does not make the mirror itself count as an obstruction.
        Vec3 surface=center.subtract(delta.normalize().scale(.16));
        return p.level().clip(new ClipContext(child.getEyePosition(),surface,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,child)).getType()==HitResult.Type.MISS;
    }
    public static boolean enter(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id){
        if(!owned(p,s,n)||!com.coraline.otherworld.OtherWorld.isOther(p.level())||!p.isSpectator()||id==null||!point(n,id).contains("Position"))return false;
        var at=BlockPos.of(point(n,id).getLong("Position"));
        if(p.distanceToSqr(at.getCenter())>16||!GhostVictimProgress.enterMirror(n,id,GhostVictimConfig.tuning()))return false;
        OtherMotherLoop.changed(p);return true;
    }
    public static boolean open(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child){
        if(!owned(p,s,n)||!GhostVictimProgress.mirror(n)||GhostVictimProgress.imprisoned(n)
            ||!childPresent(p,n,n.getString("Mirror"),child)||!n.hasUUID("Conversation"))return false;
        screen(p,n,"A pale reflection looks back at "+GhostVictimProgress.child(n).getString("Name")+".");return true;
    }
    public static boolean choice(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,UUID nonce,String action){
        if(!owned(p,s,n)||nonce==null||!n.hasUUID("Conversation")||!nonce.equals(n.getUUID("Conversation")))return false;
        if("close".equals(action)){GhostVictimProgress.leaveMirror(n);close(p);OtherMotherLoop.changed(p);return true;}
        int selected;try{selected=Integer.parseInt(action);}catch(NumberFormatException invalid){return false;}
        if(selected<0||selected>=CHOICES.length)return false;
        if(!GhostVictimProgress.dialogue(n,nonce,CHOICES[selected],childPresent(p,n,n.getString("Mirror"),child)))return false;
        OtherMotherLoop.changed(p);
        if(!GhostVictimProgress.mirror(n)){close(p);return true;}
        String response=switch(CHOICES[selected]){
            case FRIENDLY->"Who are you? Why are you in there?";
            case HELP->"What can I do to help?";
            case TRAPPED->"Trapped? But she said this was a happy place.";
            case EVIDENCE->GhostVictimProgress.child(n).getInt("EvidenceCount")>0?"I saw it too. Something here is wrong.":"I haven't seen anything like that.";
            case ACCUSATION->GhostVictimProgress.child(n).getInt("EvidenceCount")>0?"Maybe you're right. I need to be careful.":"Why should I believe that?";
            case THREAT->"Leave me alone! I'm going to tell her!";
            default->"Goodbye.";
        };
        n.putString("ChildWords",response);n.putLong("ChildTalkingUntil",GhostVictimProgress.clock(n)+Math.max(40,response.length()*2));
        screen(p,n,response);return true;
    }
    public static void close(ServerPlayer p){var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putBoolean("Close",true);SecretsNetwork.send(p,packet);}
    private static void screen(ServerPlayer p,CompoundTag n,String words){
        var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putUUID("Nonce",n.getUUID("Conversation"));
        packet.putString("Speaker",GhostVictimProgress.child(n).getString("Name"));packet.putString("Text",words);
        var options=new ListTag();for(String option:OPTIONS)options.add(StringTag.valueOf(option));packet.put("Options",options);
        packet.putInt("ReadyTicks",Math.max(0,(int)(n.getLong("ConversationAfter")-GhostVictimProgress.clock(n))));SecretsNetwork.send(p,packet);
    }
    private GhostVictimMirrors(){}
}
