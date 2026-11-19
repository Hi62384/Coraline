package com.coraline.feast;

import com.coraline.secrets.SecretsNetwork;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** A small recipient-only presentation snapshot, never a client-written ledger. */
public final class GhostVictimSnapshot {
    public static void send(ServerPlayer p,VisitData.Scene s){
        var n=GhostVictimEntry.data(s);if(!GhostVictimMirrors.participant(p,s,n))return;
        var packet=new CompoundTag();var ghost=new CompoundTag();packet.put("GhostVictim",ghost);
        ghost.putBoolean("Active",true);ghost.putString("Phase",GhostVictimProgress.phase(n).name());ghost.putUUID("Session",n.getUUID("Session"));
        ghost.putBoolean("Imprisoned",GhostVictimProgress.imprisoned(n));
        ghost.putBoolean("Suppressed",GhostVictimProgress.suppressed(n));
        ghost.putBoolean("DebugHideVisuals",n.getBoolean("DebugHideVisuals"));
        ghost.putDouble("Energy",GhostVictimProgress.energy(n));ghost.putDouble("Attention",GhostVictimProgress.attention(n));
        ghost.putBoolean("HasVictim",GhostVictimProgress.hasVictim(n));ghost.putDouble("MotherTrust",GhostVictimProgress.motherTrust(n));ghost.putDouble("GhostTrust",GhostVictimProgress.ghostTrust(n));
        ghost.putDouble("Deterioration",GhostVictimWorld.deterioration(n));ghost.putBoolean("InsideMirror",GhostVictimProgress.mirror(n)&&(!n.hasUUID("ConversationPlayer")||p.getUUID().equals(n.getUUID("ConversationPlayer"))));
        var occupied=n.getCompound("Mirrors").getCompound(n.getString("Mirror"));
        if(GhostVictimProgress.mirror(n)&&occupied.hasUUID("Entity"))ghost.putUUID("MirrorEntity",occupied.getUUID("Entity"));
        var child=GhostVictimActors.find(p,s,n);ghost.putInt("VictimEntity",child==null?-1:child.getId());
        if(n.hasUUID("VictimActor"))ghost.putUUID("VictimActor",n.getUUID("VictimActor"));
        ghost.putString("ChildName",GhostVictimProgress.child(n).getString("Name"));ghost.putString("ChildWords",n.getString("ChildWords"));
        ghost.putLong("Utterance",n.getLong("ChildTalkingUntil"));ghost.putInt("TalkingTicks",(int)Math.max(0,n.getLong("ChildTalkingUntil")-GhostVictimProgress.clock(n)));
        if(GhostEscapeProgress.bodyRestored(n)){
            boolean complete=GhostEscapeProgress.phase(n.getCompound(GhostEscapeProgress.KEY))==GhostEscapeProgress.Phase.COMPLETE;
            ghost.putBoolean("Active",!complete);ghost.putBoolean("BodyRestored",true);ghost.putBoolean("InsideMirror",false);
            ghost.putString("Objective",complete?"":"The child is heading home.");ghost.put("InteractionPoints",new ListTag());SecretsNetwork.send(p,packet);return;
        }
        String objective=GhostVictimProgress.imprisoned(n)?"The Beldam has trapped you. Wait for your release.":switch(GhostVictimProgress.phase(n)){
            case ENTER_MIRROR->"Enter mirror";
            case EMPTY_WORLD->"The house is gone. Something is waiting.";
            case RECONSTRUCTION,NEW_VICTIM_RESET->"Watch the Other World rebuild.";
            case WAITING_FOR_VICTIM->"Go to the Little Door room.";
            case INTRODUCTION->"A new child has arrived.";
            case FIRST_SUPPER->n.getBoolean("SupperPhysicalReady")
                    ?"Manifest an object near the table to warn the child."
                    :"Follow the family to supper.";
            case EXPLORATION->"Find a way to warn the child.";
            case BEDTIME->"This may be your last chance to speak tonight.";
            case NIGHT_PREPARATION->"Explore tomorrow's opportunities.";
            case DAILY_ARRIVAL->"The child is returning through the Little Door.";
            case DAILY_GIFT->"Other Mother has prepared a gift.";
            case ACTIVITY,BETWEEN_ACTIVITIES->"Follow the child. Watch for cracks in the illusion.";
            case VICTIM_CAPTURED->"Another child is being claimed.";
            case VICTIM_ESCAPED->"The child is leaving. You are still trapped.";
        };
        var escapeState=n.getCompound(GhostEscapeProgress.KEY);boolean escaping=GhostEscapeProgress.owns(n,escapeState)&&GhostEscapeProgress.active(escapeState);
        if(escaping&&!GhostVictimProgress.imprisoned(n))objective=GhostEscapeProgress.objective(escapeState);
        if(GhostVictimProgress.suppressed(n))objective="The Beldam is suppressing your manifestations. The child is still in danger.";
        String blocker=escaping?escapeState.getString("Blocker"):n.getString("Blocker");
        if(!blocker.isBlank()&&!GhostVictimProgress.imprisoned(n))objective=blocker;
        ghost.putString("Objective",objective);
        long remaining=GhostVictimProgress.suppressed(n)?n.getLong("SuppressedUntil")-GhostVictimProgress.clock(n):GhostVictimProgress.imprisoned(n)?n.getLong("PunishedUntil")-GhostVictimProgress.clock(n):
            escaping?GhostEscapeProgress.countdown(escapeState,GhostVictimProgress.clock(n)):
            GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.NIGHT_PREPARATION?GhostVictimConfig.PREPARATION_SECONDS.get()*20L-GhostVictimProgress.age(n):
            GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.BEDTIME&&n.contains("BedtimeWindowStarted")?GhostVictimConfig.BEDTIME_SECONDS.get()*20L-(GhostVictimProgress.clock(n)-n.getLong("BedtimeWindowStarted")):0;
        boolean exploring=GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.EXPLORATION&&!escaping&&!GhostVictimProgress.imprisoned(n)&&!GhostVictimProgress.suppressed(n);
        if(exploring)remaining=GhostVictimConfig.EXPLORATION_SECONDS.get()*20L-GhostVictimProgress.age(n);
        ghost.putBoolean("BedtimeCountdown",exploring);
        ghost.putString("FamilyWords",n.getString("FamilyWords"));
        ghost.putInt("FamilyWordsTicks",(int)Math.max(0,n.getLong("FamilyWordsUntil")-GhostVictimProgress.clock(n)));
        ghost.putInt("CountdownTicks",(int)Math.max(0,remaining));var points=new ListTag();
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.ENTER_MIRROR)point(points,GhostVictimEntry.prisonMirror(s).getCenter(),"prison_exit");
        else if(!GhostVictimProgress.imprisoned(n))for(String key:n.getCompound("Mirrors").getAllKeys()){
            boolean preview=GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.NIGHT_PREPARATION;
            if(!(preview?GhostVictimMirrors.previewAvailable(n,key,p.level().dimension().location().toString()):GhostVictimMirrors.available(n,key,p.level().dimension().location().toString())))continue;
            var mirror=n.getCompound("Mirrors").getCompound(key);point(points,GhostVictimMirrors.surface(mirror),(preview?"inspect_mirror:":"mirror:")+key);
            String label=key.equals("bedroom")?"Bedroom mirror":key.replace('_',' ')+" mirror";
            points.getCompound(points.size()-1).putString("Label",preview?"Tomorrow: "+label:
                    label+(GhostVictimMirrors.childPresent(p,n,key,child)?" — child ready":" — waiting for the child"));
        }
        for(String key:n.getCompound("Points").getAllKeys()){
            if(!GhostManifestationTarget.validId(key))continue;
            boolean preview=GhostVictimSchedule.previewAvailable(n,key);
            if(!GhostVictimSchedule.manifestationAvailable(n,key)&&!preview)continue;
            var row=n.getCompound("Points").getCompound(key);if(row.contains("Visible")&&!row.getBoolean("Visible"))continue;
            if(!row.getString("Dimension").isEmpty()&&!row.getString("Dimension").equals(p.level().dimension().location().toString()))continue;
            point(points,GhostVictimManifestations.interactionPoint(row),(preview?"inspect:":"manifest:")+key);
            var point=points.getCompound(points.size()-1);
            var fixture=GhostVictimManifestations.fixture(row);
            if(fixture!=null)point.putLong("Fixture",fixture.asLong());
            point.putString("Label",switch(row.getString("Kind")){
                case "LIGHT"->"Flicker the light";
                case "FOOD"->"Reveal the food";
                case "ILLUSION"->"Reveal the illusion";
                case "KNIFE","OBJECT"->"Disturb the object";
                default->row.getString("Kind").toLowerCase(java.util.Locale.ROOT).replace('_',' ');
            });
            if(!row.getString("Label").isBlank())point.putString("Label",row.getString("Label"));
            var plan=n.getCompound(GhostEscapeProgress.KEY);
            if(key.startsWith("room_light_")&&GhostEscapeProgress.phase(plan)==GhostEscapeProgress.Phase.WAIT_LURE
                    &&plan.contains("AmbushLamp",Tag.TAG_LONG)&&row.getLong("Position")==plan.getLong("AmbushLamp"))
                point.putString("Label","Flicker this light to draw Other Mother away");
            if(preview)point.putString("Label","Tomorrow: "+point.getString("Label"));
            else if(row.getString("Kind").equals("GIFT_SECRET")){point.putString("Alternate","manifest_fright:"+key);point.putString("Label",(row.getString("Label").isBlank()?"Reveal the gift's secret":row.getString("Label"))+"; sneak to move the gift");}
        }
        var outing=GhostVictimActivities.data(n);
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.ACTIVITY&&outing.contains("Center")){
            if(com.coraline.otherworld.OtherWorld.isOther(p.level()))point(points,GhostVictimActivities.housePortal(s).getCenter().add(0,1,0),"follow_activity");
            else if(ActivityWorld.isActivity(p.level()))point(points,GhostVictimActivities.entrance(net.minecraft.core.BlockPos.of(outing.getLong("Center")),outing.getString("Id")).getCenter().add(0,1,0),"return_house");
        }
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.NIGHT_PREPARATION&&!GhostVictimProgress.imprisoned(n)){
            for(Tag entry:n.getList("ScoutSites",Tag.TAG_COMPOUND)){
                var site=(CompoundTag)entry;String id=site.getString("Id");var activity=GhostVictimSchedule.activity(id);if(activity==null)continue;
                if(com.coraline.otherworld.OtherWorld.isOther(p.level())){
                    point(points,net.minecraft.core.BlockPos.of(site.getLong("Entrance")).getCenter().add(0,1,0),"scout:"+id);
                    points.getCompound(points.size()-1).putString("Label","Scout tomorrow's "+activity.title());
                }else if(id.equals(n.getString("ScoutActivity"))){
                    point(points,GhostVictimActivities.entrance(net.minecraft.core.BlockPos.of(site.getLong("Center")),id).getCenter().add(0,1,0),"scout_return");
                    points.getCompound(points.size()-1).putString("Label","Return to the house");
                }
            }
        }
        var escape=n.getCompound(GhostEscapeProgress.KEY);if(GhostEscapeProgress.owns(n,escape)){
            var phase=GhostEscapeProgress.phase(escape);
            if(child!=null&&child.level()==p.level()&&(GhostEscapeProgress.stopSignalAvailable(phase)||phase==GhostEscapeProgress.Phase.HIDDEN)){
                if(phase!=GhostEscapeProgress.Phase.HIDDEN){boolean stopOnly=phase!=GhostEscapeProgress.Phase.WAIT_GO&&phase!=GhostEscapeProgress.Phase.WAIT_LURE;point(points,child.blockPosition().getCenter(),stopOnly?"escape_stop":"escape_go");var signal=points.getCompound(points.size()-1);signal.putString("Alternate","escape_stop");signal.putString("Label",stopOnly?"Signal the child to stop and hide":"Signal Go; sneak to signal Stop");}
                if(phase==GhostEscapeProgress.Phase.HIDDEN){
                    for(Tag raw:escape.getList("HidingNodes",Tag.TAG_COMPOUND)){var node=(CompoundTag)raw;if(node.getBoolean("Compromised")||node.getString("Id").equals(escape.getString("HidingSpot")))continue;
                        point(points,net.minecraft.core.BlockPos.of(node.getLong("Position")).getCenter(),"escape_hide:"+node.getString("Id"));points.getCompound(points.size()-1).putString("Label","Move to "+(node.contains("Label")?node.getString("Label"):GhostEscapeHiding.coverLabel(node.getInt("Room"),"")));}
                    if(escape.contains("FinalDoor")){point(points,net.minecraft.core.BlockPos.of(escape.getLong("FinalDoor")).getCenter(),"escape_door");points.getCompound(points.size()-1).putString("Label","Signal the final run to the Little Door");}
                }
            }
        }
        ghost.put("InteractionPoints",points);SecretsNetwork.send(p,packet);
    }
    private static void point(ListTag all,Vec3 at,String action){var point=new CompoundTag();point.putDouble("X",at.x);point.putDouble("Y",at.y);point.putDouble("Z",at.z);point.putString("Action",action);all.add(point);}
    private GhostVictimSnapshot(){}
}
