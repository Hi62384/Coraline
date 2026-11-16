package com.coraline.feast;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Explicit operator previews use the same owner journal; they never restore
 * confiscated inventory, impersonate another owner or certify a real playthrough. */
public final class GhostVictimDebug {
    public static final String[] ACTIONS={"enter_ghost","exit_prison","other_world","reconstruct","finish_rebuild","energy","attention","punish","release","visuals","new_victim","identity","mother_trust","ghost_trust","capture","child_escape","reset_victim","memories","introduction","supper","exploration","bedtime","preparation","daily_arrival","gift","activity","complete_activity","next_day","state","cooldowns","reload","isolation"};
    public static String run(ServerPlayer p,String request){
        if(!p.hasPermissions(2))return "Operator permission is required.";
        if(request==null||request.length()>200)return "Invalid ghost control.";
        var s=OtherMotherLoop.scene(p);if(s==null||!p.getUUID().equals(s.owner))return "Begin your own Other World story first.";
        var parts=request.split(",",2);String action=parts[0],value=parts.length>1?parts[1]:"0";
        if(action.equals("enter_ghost")&&!GhostVictimDirector.active(s)){
            if(!GhostVictimEntry.begin(p,s))return "Finish the accepted or boss-loss mirror scene first. Original inventory escrow is preserved.";
        }
        if(!GhostVictimDirector.active(s))return "The owner's ghost storyline is not active.";
        var n=GhostVictimEntry.data(s);if(!GhostVictimMirrors.owned(p,s,n))return "This is not your ghost session.";
        n.putBoolean("OperatorPreview",true);
        switch(action){
            case "state"->{return "Ghost "+GhostVictimProgress.phase(n)+"; energy="+GhostVictimProgress.energy(n)+"; attention="+GhostVictimProgress.attention(n)+"; actor="+n.getString("ActorDimension")+"; blocker="+n.getString("LastRouteBlocker");}
            case "identity"->{return GhostVictimProgress.child(n).toString();}
            case "memories"->{return GhostVictimProgress.child(n).getList("Memories",Tag.TAG_STRING).toString();}
            case "energy","attention","mother_trust","ghost_trust"->{
                double number;try{number=Double.parseDouble(value);}catch(NumberFormatException invalid){return "Enter a number from 0 to 1.";}
                if(!Double.isFinite(number)||number<0||number>1)return "Enter a finite number from 0 to 1.";
                if(action.equals("energy"))n.putDouble("Energy",number);
                else if(action.equals("attention"))n.putDouble("Attention",number);
                else GhostVictimProgress.trust(n,action.equals("mother_trust")?number:GhostVictimProgress.motherTrust(n),action.equals("ghost_trust")?number:GhostVictimProgress.ghostTrust(n));
            }
            case "enter_ghost"->{}
            case "exit_prison"->{
                if(GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.ENTER_MIRROR)return "The initial prison has already been left.";
                if(!AcceptedButtonEyesWorld.transfer(p,s,GhostVictimWorld.hall(s)))return "Owner house landing unavailable.";
                GhostVictimProgress.leavePrison(n);GhostVictimEntry.ghostControls(p,s,n);
            }
            case "other_world"->GhostVictimActivities.returnGhost(p,s,n);
            case "visuals"->n.putBoolean("DebugHideVisuals",!n.getBoolean("DebugHideVisuals"));
            case "punish"->n.putDouble("Attention",1);
            case "release"->n.putLong("PunishedUntil",GhostVictimProgress.clock(n));
            case "reconstruct"->GhostVictimProgress.move(n,GhostVictimProgress.Phase.EMPTY_WORLD);
            case "finish_rebuild"->{
                if(!GhostVictimProgress.hasVictim(n)){var id=java.util.UUID.randomUUID();GhostVictimProgress.newVictim(n,id,GhostVictimActors.identity(id));}
                if(!GhostVictimWorld.prepare(p,s,n))return "House is not ready: "+n.getString("Blocker");
                GhostVictimEntry.disguise(p,s,n);GhostVictimSchedule.today(n);GhostVictimProgress.move(n,GhostVictimProgress.Phase.WAITING_FOR_VICTIM);
            }
            case "new_victim","reset_victim"->{
                if(GhostVictimProgress.hasVictim(n))GhostVictimProgress.archiveVictim(n,"ESCAPED");
                else GhostVictimProgress.move(n,GhostVictimProgress.Phase.NEW_VICTIM_RESET);
            }
            case "capture","child_escape"->{
                if(!GhostVictimProgress.hasVictim(n))return "No current child.";
                GhostVictimProgress.trust(n,action.equals("capture")?1:0,GhostVictimProgress.ghostTrust(n));
            }
            case "introduction","supper","exploration","bedtime","preparation","daily_arrival","gift"->{
                if(GhostVictimActors.find(p,s,n)==null)return "The original child must be loaded; no replacement will be spawned.";
                GhostVictimProgress.leaveMirror(n);GhostVictimMirrors.close(p);n.remove("ChildRoutine");
                GhostVictimProgress.move(n,switch(action){case "introduction"->GhostVictimProgress.Phase.INTRODUCTION;case "supper"->GhostVictimProgress.Phase.FIRST_SUPPER;case "exploration"->GhostVictimProgress.Phase.EXPLORATION;case "bedtime"->GhostVictimProgress.Phase.BEDTIME;case "preparation"->GhostVictimProgress.Phase.NIGHT_PREPARATION;case "daily_arrival"->GhostVictimProgress.Phase.DAILY_ARRIVAL;default->GhostVictimProgress.Phase.DAILY_GIFT;});
            }
            case "activity"->{
                if(GhostVictimSchedule.activity(value)==null)return "Enter an existing activity ID.";
                if(!GhostVictimProgress.hasVictim(n)||n.contains("ParticipantActivity"))return "Finish the current outing before selecting another.";
                var activities=new ListTag();activities.add(StringTag.valueOf(value));var plan=GhostVictimSchedule.today(n);plan.put("Activities",activities);plan.putInt("ActivityIndex",0);
                GhostVictimProgress.move(n,GhostVictimProgress.Phase.BETWEEN_ACTIVITIES);n.remove("ChildRoutine");
            }
            case "complete_activity"->{
                if(GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.ACTIVITY)return "No active outing.";
                var a=GhostVictimActivities.data(n);a.putInt("Station",100);a.putLong("Started",GhostVictimProgress.clock(n)-12000);
            }
            case "next_day"->{
                if(GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.NIGHT_PREPARATION)return "Reach nighttime preparation first.";
                n.putLong("PhaseStarted",GhostVictimProgress.clock(n)-GhostVictimConfig.PREPARATION_SECONDS.get()*20L);
            }
            case "cooldowns"->{return "Per-point cooldowns: "+n.getCompound("PointCooldowns")+"; daily uses: "+n.getCompound("PointUses");}
            case "reload"->{try{return n.equals(net.minecraft.nbt.TagParser.parseTag(n.toString()))?"Serialized NBT roundtrip matches; a real reconnect remains a separate test.":"NBT roundtrip failed.";}catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){return "NBT roundtrip failed: "+invalid.getMessage();}}
            case "isolation"->{for(var other:VisitData.get(p.server).scenes.values())if(other!=s&&GhostVictimDirector.active(other)&&GhostVictimEntry.data(other)==n)return "Shared journal detected.";return "Owner journals are distinct; multiple-client verification remains separate.";}
            default->{return "Unknown ghost control.";}
        }
        OtherMotherLoop.changed(p);GhostVictimSnapshot.send(p,s);return "Operator ghost preview applied: "+action+". Not a verified playthrough.";
    }
    private GhostVictimDebug(){}
}
