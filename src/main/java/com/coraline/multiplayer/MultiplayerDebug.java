package com.coraline.multiplayer;

import com.coraline.feast.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit operator controls over actual players and the native owner scene.
 * No fake players, fabricated escrow returns, canonical bodies or ending proofs. */
@Mod.EventBusSubscriber(modid="coraline")
public final class MultiplayerDebug {
    public static final String[] ACTIONS={"inspect","create_session","allow_guest","join_guest","leave_guest","checkpoint","pause","resume","owner_disconnect","guest_disconnect","teleport_group","shared_camera","same_position","invisibility","collision","clear_presentation","finish_cutscene","start_carnival","start_temple","start_cake","set_tokens","defeat_owner","defeat_guest","restore_spectator","restore_all","fail_all","guest_victory","boss_phase","enter_ghost","ghost_energy","ghost_attention","ghost_punishment","ghost_state","restore_inventory","quest_recovery","world_state","map_eligibility","recover_map","wybie_state","breach_state","tycoon_panel","replay_status","replay_preview","reset_preview","reset_request"};
    public static String run(ServerPlayer operator,String action,String argument){
        if(!operator.hasPermissions(2))return "Operator permission required.";
        if(argument==null||argument.length()>240)return "Invalid multiplayer control.";
        var fields=argument.split(",",2);ServerPlayer selected=operator;String value=argument;
        if(!fields[0].isBlank())try{selected=operator.server.getPlayerList().getPlayer(java.util.UUID.fromString(fields[0].trim()));value=fields.length==2?fields[1].trim():"";}catch(IllegalArgumentException numberOrAction){/* A value without a player UUID targets the operator. */}
        if(selected==null)return "The selected actual player must be online.";
        var data=StorySessionData.get(operator.server);var ownerId=VisitData.get(operator.server).scenes.containsKey(operator.getUUID())?data.owner(operator.getUUID()).orElse(operator.getUUID()):data.owner(selected.getUUID()).orElse(selected.getUUID());
        var owner=operator.server.getPlayerList().getPlayer(ownerId);var scene=VisitData.get(operator.server).scenes.get(ownerId);
        try{
            if(action.equals("inspect"))return data.debugView(operator.server,selected.getUUID()).toString();
            if(action.equals("world_state"))return "Global Hand complete="+com.coraline.postgame.InfiniteWorldData.get(operator.server).active()+"; canonical outcome="+com.coraline.postgame.InfiniteWorldData.get(operator.server).canonicalCompletion()+"; personal completion owners="+com.coraline.postgame.InfiniteWorldData.get(operator.server).completionOwners().size();
            if(action.equals("breach_state"))return com.coraline.postgame.BreachWorldData.get(operator.server).view().toString();
            if(action.equals("tycoon_panel")){com.coraline.tycoon.TycoonNetwork.open(operator,"debug",null);return "Opened the native Tycoon controls; actual owner/collaborator permissions remain enforced.";}
            if(action.equals("reset_preview"))return "World active="+com.coraline.postgame.InfiniteWorldData.get(operator.server).active()+", final encounter="+com.coraline.postgame.TrueHeartBattle.encounterActive(operator.server)+", reset="+com.coraline.postgame.TrueHeartBattle.resetting(operator.server)+", selected pending="+com.coraline.postgame.TrueHeartBattle.pendingReset(operator.server,selected.getUUID())+". No state changed.";
            if(action.equals("reset_request"))return com.coraline.postgame.TrueHeartBattle.request(operator)==1?"The native final-ending warning is open; its separate consent and genuine victory/core receipts remain required.":"Native final-ending prerequisites refused the request.";
            if(owner==null||scene==null)return "An online native story owner and their existing Little Door scene are required.";
            if(action.equals("create_session"))return data.observe(owner,scene)?"Observed the existing native owner's session. No second personal scene created.":"Session admission refused.";
            if(action.equals("allow_guest"))return data.permit(owner,selected.getUUID(),true)?"Actual guest permission saved.":"Guest permission refused.";
            if(action.equals("join_guest")){
                var door=com.coraline.otherworld.DoorData.get(operator.server).doors.get(scene.door);var level=door==null?null:com.coraline.parents.FamilyData.level(operator.server,door.dimension);
                if(level==null||selected.serverLevel()!=level||selected.distanceToSqr(door.pos.getCenter())>25||!(level.getBlockEntity(door.pos) instanceof com.coraline.otherworld.DoorBlockEntity block))return "The actual guest must stand beside the owner's native Little Door; permit them first.";
                return StoryCoop.touchDoor(selected,block)?"Native permission, return-position and checkpoint admission attempted.":"Native door did not request a guest session.";
            }
            if(action.equals("leave_guest")||action.equals("guest_disconnect")){if(!data.requestReturn(selected))return "Native guest return is not ready.";StoryCoop.clear(selected);data.restore(selected);return "Actual guest return requested; pending original items retain their receipts.";}
            if(action.equals("checkpoint")){VisitData.get(operator.server).setDirty();return data.checkpoint(operator.server)?"Session checkpoint read-back passed.":"Checkpoint remains pending.";}
            if(action.equals("pause")||action.equals("owner_disconnect")||action.equals("resume"))return data.debugPause(operator,owner,!action.equals("resume"))?"Native session pause/resume checkpoint saved.":"Pause/resume is waiting for its native checkpoint.";
            if(action.equals("teleport_group")||action.equals("same_position")){StoryCoop.transferGroup(owner,scene);return "Actual participants transferred using verified native footing.";}
            if(java.util.Set.of("shared_camera","invisibility","collision").contains(action)){owner.getPersistentData().putLong("CoralineCoopDebugUntil",owner.server.overworld().getGameTime()+200);owner.getPersistentData().putUUID("CoralineCoopDebugDoor",scene.door);return "Ten-second shared presentation lease started; the native collision/visibility/control pipeline restores on expiry.";}
            if(action.equals("clear_presentation")){owner.getPersistentData().remove("CoralineCoopDebugUntil");for(var member:data.participants(operator.server,scene.owner,true))StoryCoop.clear(member);return "Debug presentation lease cleared. An actual running cutscene still owns its normal controls.";}
            if(action.equals("finish_cutscene"))return com.coraline.item.StorySkipDebugItem.request(owner,false);
            if(action.startsWith("start_")){var activity=switch(action){case "start_carnival"->OtherMotherActivities.Activity.CARNIVAL;case "start_temple"->OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE;case "start_cake"->OtherMotherActivities.Activity.CAKE_MAKING_CONTEST;default->null;};if(activity!=null)return ActivityWorld.enterDebug(owner,activity);}
            if(action.equals("set_tokens")){int amount=Integer.parseInt(value);if(amount<0||amount>10000||scene.loop.activity!=OtherMotherActivities.Activity.CARNIVAL)return "Enter 0–10000 during a native Carnival visit.";scene.loop.carnivalTokensRemaining=amount;scene.loop.story.putBoolean("OperatorPreview",true);OtherMotherLoop.changed(owner);return "Shared test token balance saved.";}
            if(action.equals("defeat_owner")||action.equals("defeat_guest")){var target=action.equals("defeat_owner")?owner:selected;return data.eliminateActivity(target,scene)||StoryCombatSafety.eliminate(target)?"Actual participant held as a spectator; inventory unchanged.":"The selected player is not an eligible active group combat/activity participant.";}
            if(action.equals("restore_spectator")||action.equals("restore_all")){var targets=action.equals("restore_all")?data.participants(operator.server,scene.owner,true):java.util.List.of(selected);for(var member:targets){data.restoreActivityMode(member);StoryCombatSafety.restore(member);ActivityPlayerState.restore(member);}return "Actual saved participant modes/restoration attempted; intentional story ghosts remain ghosts.";}
            if(action.equals("fail_all")){for(var member:data.participants(operator.server,scene.owner,true))if(!member.isSpectator()){if(ActivityWorld.isActivity(member.level()))data.eliminateActivity(member,scene);else StoryCombatSafety.eliminate(member);}return "Actual participants eliminated through the native group failure path; no completion reward granted.";}
            if(action.equals("guest_victory"))return CooperativeDebugOutcomes.victory(operator,owner,selected);
            if(action.equals("boss_phase"))return CooperativeDebugOutcomes.bossPhase(operator,owner,scene);
            if(action.startsWith("ghost_")||action.equals("enter_ghost")){String control=switch(action){case "ghost_energy"->"energy";case "ghost_attention"->"attention";case "ghost_punishment"->"punish";case "ghost_state"->"state";default->"enter_ghost";};return GhostVictimDebug.run(owner,control+","+(value.isBlank()?"1":value));}
            if(action.equals("restore_inventory"))return GroupInventoryEscrow.restore(selected)?"Actual individual escrow restored with native receipt.":"The original inventory return witness is not ready; nothing fabricated.";
            if(action.equals("quest_recovery"))return QuestItemRecovery.capture(selected)?"Actual holder custody checkpoint attempted; pending items remain recoverable.":"Original quest custody is waiting for its native profile checkpoint.";
            if(action.equals("map_eligibility")){var run=com.coraline.rebirth.StoryRunStore.get(operator.server,scene.owner);return "Personal run="+run.run()+", completed="+run.completed()+", native epilogue ready="+PalaceJourney.prerequisites(scene).ready()+", global Hand complete="+com.coraline.postgame.InfiniteWorldData.get(operator.server).active();}
            if(action.equals("recover_map"))return PalaceJourney.giveMap(owner,scene,true)?"Original eligible vanilla map ID recovered.":"Native map/run prerequisites refused a new map.";
            if(action.equals("wybie_state")){var identity=CanonicalRealWybie.identity(scene);return "Canonical world="+com.coraline.postgame.InfiniteWorldData.get(operator.server).canonicalCompletion()+"; actual personal original="+(identity==null?"unknown, not dead":identity.toString());}
            if(action.equals("replay_status")||action.equals("replay_preview"))return com.coraline.rebirth.StoryRebirth.debug(owner,action.equals("replay_preview")?"preserved":"state");
            return "Unknown multiplayer control.";
        }catch(Exception invalid){return "Control paused: "+(invalid.getMessage()==null?invalid.getClass().getSimpleName():invalid.getMessage());}
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("coraline").then(Commands.literal("multiplayer").requires(s->s.hasPermission(2)).then(Commands.argument("action",StringArgumentType.word()).suggests((c,b)->{for(String action:ACTIONS)b.suggest(action);return b.buildFuture();}).executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(run(p,StringArgumentType.getString(c,"action"),"")));return 1;}).then(Commands.argument("argument",StringArgumentType.greedyString()).executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(run(p,StringArgumentType.getString(c,"action"),StringArgumentType.getString(c,"argument"))));return 1;})))));}
    private MultiplayerDebug(){}
}
