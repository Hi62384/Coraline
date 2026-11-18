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
    public static boolean participant(ServerPlayer p,VisitData.Scene s,CompoundTag n){return owned(p,s,n)||com.coraline.multiplayer.GhostCooperation.member(p,s,n);}
    public static void register(CompoundTag n,String id,BlockPos at,float yaw){
        register(n,id,at,yaw,com.coraline.otherworld.OtherWorld.KEY.location().toString());
    }
    public static void register(CompoundTag n,String id,BlockPos at,float yaw,String dimension){
        if(id==null||!id.matches("[a-z_]{1,40}")||at==null||!Float.isFinite(yaw))throw new IllegalArgumentException("Invalid authored mirror");
        var all=n.getCompound("Mirrors");var mirror=new CompoundTag();
        mirror.putLong("Position",at.asLong());mirror.putFloat("Yaw",yaw);mirror.putString("Dimension",dimension);
        all.put(id,mirror);n.put("Mirrors",all);
    }
    static Vec3 surface(CompoundTag mirror){
        var center=Vec3.atBottomCenterOf(BlockPos.of(mirror.getLong("Position"))).add(0,1.35,0);
        return mirror.getBoolean("WallMounted")?center.add(normal(mirror).scale(-.29)):center;
    }
    private static Vec3 normal(CompoundTag mirror){return Vec3.directionFromRotation(0,mirror.getFloat("Yaw")).scale(-1);}
    private static boolean hasBacking(net.minecraft.world.level.Level level,CompoundTag mirror){
        if(!mirror.getBoolean("WallMounted"))return true;
        var front=net.minecraft.core.Direction.fromYRot(mirror.getFloat("Yaw")).getOpposite();
        var backing=BlockPos.of(mirror.getLong("Position")).relative(front.getOpposite()).above();
        return level.getBlockState(backing).isFaceSturdy(level,backing,front);
    }
    private static CompoundTag point(CompoundTag n,String id){return n.getCompound("Mirrors").getCompound(id);}
    public static boolean available(CompoundTag n,String id,String dimension){
        if(!GhostVictimProgress.hasVictim(n))return false;
        return inPlan(n,id,dimension,GhostVictimSchedule.today(n));
    }
    public static boolean previewAvailable(CompoundTag n,String id,String dimension){
        if(!GhostVictimProgress.hasVictim(n)||GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.NIGHT_PREPARATION)return false;
        return inPlan(n,id,dimension,GhostVictimSchedule.tomorrow(n));
    }
    private static boolean inPlan(CompoundTag n,String id,String dimension,CompoundTag plan){
        var mirror=point(n,id);if(!mirror.contains("Position"))return false;
        String expected=mirror.getString("Dimension");if(expected.isEmpty())expected=com.coraline.otherworld.OtherWorld.KEY.location().toString();
        if(!expected.equals(dimension)||!GhostVictimProgress.hasVictim(n))return false;
        for(Tag entry:plan.getList("Mirrors",Tag.TAG_STRING))if(entry.getAsString().replace(':','_').equals(id))return true;
        return false;
    }
    public static void inspect(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id){
        if(!participant(p,s,n)||!p.isSpectator()||GhostVictimProgress.imprisoned(n)||!previewAvailable(n,id,p.level().dimension().location().toString()))return;
        var at=surface(point(n,id));
        var delta=at.subtract(p.getEyePosition());if(delta.lengthSqr()>16||delta.lengthSqr()<.001||p.getLookAngle().dot(delta.normalize())<.85)return;
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("This mirror will be available tomorrow. Inspecting uses no energy."),true);
    }
    public static boolean childPresent(ServerPlayer p,CompoundTag n,String id,TycoonNpc child){
        var mirror=point(n,id);
        if(child==null||child.level()!=p.level()||!available(n,id,p.level().dimension().location().toString())
                ||p.distanceToSqr(surface(mirror))>16)return false;
        return childAtMirror(n,id,child,true);
    }
    static boolean childAtMirror(CompoundTag n,String id,TycoonNpc child,boolean requireFacing){
        return child!=null&&child.isAlive()&&!child.isInvisible()&&GhostVictimProgress.hasVictim(n)
                &&child.owner!=null&&n.hasUUID("Owner")&&child.owner.equals(n.getUUID("Owner"))
                &&GhostVictimProgress.child(n).getUUID("Id").equals(child.resident)
                &&n.hasUUID("VictimActor")&&child.getUUID().equals(n.getUUID("VictimActor"))
                &&childCanSeeMirror(n,id,child,child.position())
                &&(!requireFacing||GhostVictimNavigation.view(child).dot(surface(point(n,id)).subtract(child.getEyePosition()).normalize())
                        >=Math.cos(Math.toRadians(65)));
    }
    static boolean childCanSeeMirror(CompoundTag n,String id,net.minecraft.world.entity.LivingEntity child,Vec3 feet){
        var mirror=point(n,id);
        if(!mirror.contains("Position")||!hasBacking(child.level(),mirror))return false;
        var center=surface(mirror);
        var eye=feet.add(0,child.getEyeHeight(),0);
        var delta=center.subtract(eye);
        if(delta.lengthSqr()>25||delta.lengthSqr()<.01||feet.subtract(center).dot(normal(mirror))<.1)return false;
        var end=center.subtract(delta.normalize().scale(.16));
        return child.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,child)).getType()==HitResult.Type.MISS;
    }
    static void faceMirror(CompoundTag n,String id,TycoonNpc child){
        var target=surface(point(n,id));
        child.getLookControl().setLookAt(target.x,target.y,target.z,30,30);
    }
    public static boolean enter(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id){
        if(!participant(p,s,n)||id==null||GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.NIGHT_PREPARATION||!available(n,id,p.level().dimension().location().toString())||!p.isSpectator())return false;
        var mirror=point(n,id);var center=surface(mirror);var delta=center.subtract(p.getEyePosition());
        if(!hasBacking(p.level(),mirror)||delta.lengthSqr()>16||delta.lengthSqr()<.001||p.getLookAngle().dot(delta.normalize())<.8
            ||p.getEyePosition().subtract(center).dot(normal(mirror))<0)return false;
        var end=center.subtract(delta.normalize().scale(.16));
        if(p.level().clip(new ClipContext(p.getEyePosition(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p))
                .getType()!=HitResult.Type.MISS)return false;
        if(GhostVictimProgress.energy(n)<GhostVictimConfig.MIRROR_ENTRY.get()){
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Not enough manifestation energy."),true);return false;
        }
        if(p.getUUID().equals(s.owner)&&GhostVictimProgress.mirror(n)&&n.hasUUID("ConversationPlayer")&&!p.getUUID().equals(n.getUUID("ConversationPlayer"))){
            var previous=p.server.getPlayerList().getPlayer(n.getUUID("ConversationPlayer"));if(previous!=null)close(previous);GhostVictimProgress.leaveMirror(n);
        }
        if(!GhostVictimProgress.enterMirror(n,id,GhostVictimConfig.tuning()))return false;
        n.putUUID("ConversationPlayer",p.getUUID());OtherMotherLoop.changed(p);return true;
    }
    public static boolean open(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child){
        if(!participant(p,s,n)||n.hasUUID("ConversationPlayer")&&!p.getUUID().equals(n.getUUID("ConversationPlayer"))||!GhostVictimProgress.mirror(n)||GhostVictimProgress.imprisoned(n)
            ||!childPresent(p,n,n.getString("Mirror"),child)||!n.hasUUID("Conversation"))return false;
        GhostVictimProgress.remember(n,"met_ghost");screen(p,n,"A pale reflection looks back at "+GhostVictimProgress.child(n).getString("Name")+".");return true;
    }
    public static boolean choice(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,UUID nonce,String action){
        if(!participant(p,s,n)||n.hasUUID("ConversationPlayer")&&!p.getUUID().equals(n.getUUID("ConversationPlayer"))||nonce==null||!n.hasUUID("Conversation")||!nonce.equals(n.getUUID("Conversation")))return false;
        if(action!=null&&action.startsWith("escape:"))return escapeChoice(p,s,n,child,nonce,action.substring(7));
        if("close".equals(action)){GhostVictimProgress.leaveMirror(n);close(p);OtherMotherLoop.changed(p);return true;}
        int selected;try{selected=Integer.parseInt(action);}catch(NumberFormatException invalid){return false;}
        if(selected<0)return false;
        if(selected>=CHOICES.length){var extras=n.getList("EscapeOptions",Tag.TAG_COMPOUND);int extra=selected-CHOICES.length;if(extra>=extras.size())return false;return escapeChoice(p,s,n,child,nonce,extras.getCompound(extra).getString("Action"));}
        var choice=CHOICES[selected];
        if(!childPresent(p,n,n.getString("Mirror"),child))return false;
        if(!GhostVictimProgress.dialogue(n,nonce,choice,true)){
            String feedback=GhostVictimProgress.clock(n)<n.getLong("ConversationAfter")
                    ?"Give me a moment to think.":"We already spoke about that. Show me something else she is hiding.";
            screen(p,n,feedback);
            return false;
        }
        OtherMotherLoop.changed(p);
        if(!GhostVictimProgress.mirror(n)){close(p);return true;}
        String response=switch(CHOICES[selected]){
            case FRIENDLY->"Who are you? Why are you in there?";
            case HELP->"What can I do to help?";
            case TRAPPED->"Trapped? But she said this was a happy place.";
            case EVIDENCE->switch(GhostVictimProgress.child(n).getString("LastEvidenceKind")){
                case "FOOD"->"I saw what was really on the plate. I don't want to eat here again.";
                case "GIFT_SECRET"->"I remember what was inside her gift. Why would she hide that?";
                case "ILLUSION"->"I saw her trick wear off. What else has she changed?";
                default->GhostVictimProgress.child(n).getInt("EvidenceCount")>0
                        ?"I saw it too. Something here is wrong.":"Show me something she is hiding. I need to see it for myself.";
            };
            case ACCUSATION->GhostVictimProgress.child(n).getInt("EvidenceCount")>0?"Maybe you're right. I need to be careful.":"Why should I believe that?";
            case THREAT->"Leave me alone! I'm going to tell her!";
            default->"Goodbye.";
        };
        n.putString("ChildWords",response);n.putLong("ChildTalkingUntil",GhostVictimProgress.clock(n)+Math.max(40,response.length()*2));
        if(GhostVictimProgress.readyToHelp(n)&&!GhostEscapeProgress.active(n.getCompound(GhostEscapeProgress.KEY)))
            response+=" I'll help you. Can we talk in the bedroom tonight?";
        screen(p,n,response);return true;
    }
    private static boolean escapeChoice(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,UUID nonce,String action){
        if(!p.getUUID().equals(s.owner)||!GhostVictimProgress.mirror(n)||!childPresent(p,n,n.getString("Mirror"),child)||action.length()>80)return false;
        String response;
        if(action.equals("begin")){
            if(GhostEscapeProgress.active(n.getCompound(GhostEscapeProgress.KEY)))return false;
            if(!GhostEscapeCovert.tryBegin(p,s,n))return false;
            response="I'll help you. First I need to visit Miss Spink and Miss Forcible for their Seeing Stone.";
        }else{
            var e=n.getCompound(GhostEscapeProgress.KEY);if(!GhostEscapeProgress.owns(n,e))return false;
            if(action.equals("report_stone")){
                if(!GhostEscapeProgress.reportStone(n,true))return false;response="Look for the eyes in one of the locked rooms. I'll wait for your signal.";
            }else if(action.equals("room_report")){
                if(GhostEscapeProgress.phase(e)!=GhostEscapeProgress.Phase.ROOM_REPORT)return false;
                response=e.getBoolean("EyeRoomKnown")?"The eyes are in the room I searched. I need to steal her key before I can retrieve them.":"I haven't found them yet. Tell me which room to search.";
            }else if(action.startsWith("room:")){
                String room=action.substring(5);boolean retrieve=e.getBoolean("KeyStolen")&&e.getBoolean("EyeRoomKnown")&&room.equals(e.getString("EyeRoom"));
                if(!GhostEscapeProgress.assignedRoom(n,room,true,retrieve))return false;
                response=retrieve?"I'll retrieve the eyes from that room.":"I'll search that room for the eyes.";
            }else if(action.equals("ambush")){
                GhostVictimWorld.refreshRoomLights(p,s,n);
                String hide=GhostEscapeAmbush.coverNearLight(n,e,child);
                if(hide.isEmpty()){
                    screen(p,n,"I need a reachable hiding place near a working light.");
                    return false;
                }
                if(!GhostEscapeProgress.assignAmbush(n,hide,true))return false;
                response="I'll hide near a light. Make it flicker, then signal when her back is turned.";
            }else if(action.startsWith("hide:")){
                String hide=action.substring(5);if(!GhostEscapeHiding.node(e,hide).contains("Position")||GhostEscapeHiding.node(e,hide).getBoolean("Compromised"))return false;
                e.putString("TargetHide",hide);response="I'll move to that hiding place.";
            }else return false;
        }
        n.putString("ChildWords",response);n.putLong("ChildTalkingUntil",GhostVictimProgress.clock(n)+Math.max(40,response.length()*2));
        screen(p,n,response);OtherMotherLoop.changed(p);return true;
    }
    public static void close(ServerPlayer p){var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putBoolean("Close",true);SecretsNetwork.send(p,packet);}
    private static void screen(ServerPlayer p,CompoundTag n,String words){
        var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putUUID("Nonce",n.getUUID("Conversation"));
        packet.putString("Speaker",GhostVictimProgress.child(n).getString("Name"));packet.putString("Text",words);
        var options=new ListTag();
        String evidence=GhostVictimProgress.child(n).getString("LastEvidenceKind");
        for(int i=0;i<OPTIONS.size();i++){
            String option=OPTIONS.get(i);
            if(CHOICES[i]==GhostVictimProgress.Dialogue.EVIDENCE)option=switch(evidence){
                case "FOOD"->"What did you see on your plate?";
                case "GIFT_SECRET"->"Let's talk about what was inside her gift.";
                case "ILLUSION"->"Tell me what you saw when her trick wore off.";
                default->option;
            };
            options.add(StringTag.valueOf(option));
        }
        var extraActions=new ListTag();var escape=n.getCompound(GhostEscapeProgress.KEY);
        if(p.getUUID().equals(n.getUUID("Owner"))&&GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.BEDTIME&&!escape.hasUUID("Attempt")&&GhostVictimProgress.readyToHelp(n))
            addOption(options,extraActions,"escape:begin","I believe you. I'll help you escape.");
        if(p.getUUID().equals(n.getUUID("Owner"))&&GhostEscapeProgress.active(escape)){
            switch(GhostEscapeProgress.phase(escape)){
                case STONE_REPORT->addOption(options,extraActions,"escape:report_stone","I have the Seeing Stone. Where should I look?");
                case READY,ROOM_REPORT,KEY_REPORT->{
                    if(GhostEscapeProgress.phase(escape)==GhostEscapeProgress.Phase.ROOM_REPORT)addOption(options,extraActions,"escape:room_report","Tell me what you found.");
                    for(Tag room:escape.getList("Rooms",Tag.TAG_STRING))addOption(options,extraActions,"escape:room:"+room.getAsString(),"Search the "+room.getAsString().replace('_',' ')+".");
                    if(GhostEscapeProgress.phase(escape)==GhostEscapeProgress.Phase.ROOM_REPORT&&!escape.getBoolean("KeyStolen"))addOption(options,extraActions,"escape:ambush","I'll distract her so you can get the key.");
                }
                case HIDDEN->{
                    for(Tag raw:escape.getList("HidingNodes",Tag.TAG_COMPOUND)){var node=(CompoundTag)raw;if(!node.getBoolean("Compromised"))addOption(options,extraActions,"escape:hide:"+node.getString("Id"),"Move to another hiding place.");}
                }
                default->{}
            }
        }
        n.put("EscapeOptions",extraActions);packet.put("Options",options);
        packet.putInt("ReadyTicks",Math.max(0,(int)(n.getLong("ConversationAfter")-GhostVictimProgress.clock(n))));SecretsNetwork.send(p,packet);
    }
    private static void addOption(ListTag visible,ListTag actions,String action,String label){var entry=new CompoundTag();entry.putString("Action",action);actions.add(entry);visible.add(StringTag.valueOf(label));}
    private GhostVictimMirrors(){}
}
