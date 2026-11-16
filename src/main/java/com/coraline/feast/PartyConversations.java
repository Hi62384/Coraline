package com.coraline.feast;

import com.coraline.secrets.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import java.util.*;

/** Owner-only, replay-resistant guest dialogue and explicit item-wager confirmation. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PartyConversations {
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.EntityInteract e){
        if(e.getHand()!=InteractionHand.MAIN_HAND||!(e.getEntity() instanceof ServerPlayer p)||!(e.getTarget() instanceof Mob guest)||!guest.getPersistentData().hasUUID("PartyOwner"))return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.CONSUME);
        open(p,guest);
    }
    public static boolean open(ServerPlayer p,Mob guest){
        if(PartyKidsGames.tag(p,guest))return true;
        if(!guest.getPersistentData().hasUUID("PartyOwner"))return false;
        var s=HouseParty.active(p);if(s==null||!p.getUUID().equals(guest.getPersistentData().getUUID("PartyOwner"))||guest.distanceToSqr(p)>16||!guest.hasLineOfSight(p)||s.loop.secrets.searching()||SecretDialogue.busy(p)||PartyMatches.active(s)||PartySurveillance.warningSpeechActive(s))return true;
        var g=HousePartyLedger.guest(HouseParty.data(s),guest.getPersistentData().getUUID("PartyGuest"));if(g==null||!Set.of("WANDERING","CONVERSING","TALKING_TO_PLAYER").contains(g.getString("State")))return true;
        var prior=HouseParty.data(s).getCompound("Conversation");if(prior.hasUUID("Guest")&&prior.getUUID("Guest").equals(g.getUUID("Id"))&&HouseParty.data(s).getLong("Clock")<prior.getLong("ReadyAt"))return true;
        close(p,s);var session=new CompoundTag();session.putUUID("Guest",g.getUUID("Id"));HouseParty.data(s).put("Conversation",session);g.putString("State","TALKING_TO_PLAYER");root(p,s,g);return true;
    }
    private static String prefix(CompoundTag g){return switch(g.getString("Kind")){case "other_bobinsky"->"bobinsky";case "other_miss_spink"->"spink";case "other_miss_forcible"->"forcible";default->"guest";};}
    private static void root(ServerPlayer p,VisitData.Scene s,CompoundTag g){
        if(g.getString("Kind").equals("other_wybie")){PartyGuests.gesture(PartyGuests.entity(p,g),1,50);show(p,s,g,"root","Wybie smiles and gives a small wave.",List.of("How do you know everyone?","Have you been here before?","Have you noticed anything unusual?","Want to play a game?","Goodbye."),0);return;}
        boolean celebration=PlayerParty.isPlayerParty(s);String who=celebration&&g.getString("Kind").equals("counterpart")?"counterpart":prefix(g);
        String id=who+"_greeting"+(who.equals("guest")||who.equals("counterpart")?"_"+(1+p.getRandom().nextInt(celebration&&who.equals("guest")?4:2)):"");spoken(p,s,g,"root",id,List.of("How do you know Other Mother?","Have you been here before?","Do you know anything interesting?","Want to play a game?","Goodbye."));
    }
    private static void spoken(ServerPlayer p,VisitData.Scene s,CompoundTag g,String node,String id,List<String> choices){String event=PlayerParty.isPlayerParty(s)?"player_party":"house_party";int ticks=NumberedEventDialogue.say(p,event,id);show(p,s,g,node,NumberedEventDialogue.line(event,id).text(),choices,ticks);}
    private static void show(ServerPlayer p,VisitData.Scene s,CompoundTag g,String node,String text,List<String> options,int spokenTicks){
        // Choosing a reply is an explicit advance, not a forced 20-second input lock.
        spokenTicks=Math.min(8,Math.max(0,spokenTicks));
        var n=HouseParty.data(s);var c=n.getCompound("Conversation");c.putUUID("Nonce",UUID.randomUUID());c.putString("Node",node);c.putInt("Options",options.size());c.putLong("ReadyAt",n.getLong("Clock")+spokenTicks);c.putLong("Expires",n.getLong("Clock")+spokenTicks+1200);
        var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putUUID("Nonce",c.getUUID("Nonce"));packet.putString("Speaker",g.getString("Name"));packet.putString("Text",text);packet.put("Options",SecretsState.strings(options));packet.putInt("ReadyTicks",spokenTicks);SecretsNetwork.send(p,packet);OtherMotherLoop.changed(p);
    }
    public static boolean choice(ServerPlayer p,UUID nonce,String action){
        var s=HouseParty.active(p);if(s==null)return false;
        if(PlayerParty.choice(p,nonce,action))return true;
        if(PartyMatches.choice(p,nonce,action))return true;
        var n=HouseParty.data(s);var c=n.getCompound("Conversation");if(!c.hasUUID("Nonce")||!nonce.equals(c.getUUID("Nonce")))return false;
        var g=c.hasUUID("Guest")?HousePartyLedger.guest(n,c.getUUID("Guest")):null;var actor=g==null?null:PartyGuests.entity(p,g);
        if(action.equals("close")){close(p,s);return true;}
        if(actor==null||actor.distanceToSqr(p)>25||!actor.hasLineOfSight(p)||s.loop.secrets.searching()||SecretDialogue.busy(p)||n.getLong("Clock")>c.getLong("Expires")){close(p,s);return true;}
        if(n.getLong("Clock")<c.getLong("ReadyAt"))return true;
        int choice;try{choice=Integer.parseInt(action);}catch(NumberFormatException ignored){return true;}
        if(choice<0||choice>=c.getInt("Options"))return true;c.remove("Nonce");
        switch(c.getString("Node")){
            case "root"->{if(choice==4)close(p,s);else if(choice==3&&g.getString("Kind").equals("other_wybie"))show(p,s,g,"kids","Wybie points to the front sitting room and mimes kicking a ball, then playfully taps his shoulder. What shall we play?",List.of("Kickball — first to three","Tag — one minute","Our usual table games","Back"),0);else if(choice==3)games(p,s,g);else if(choice==2)hint(p,s,g);else social(p,s,g,choice);}
            case "kids"->{if(choice==3)root(p,s,g);else if(choice==2)games(p,s,g);else{boolean started=PartyKidsGames.start(p,s,g,choice==1);close(p,s);if(!started)VisitAudio.caption(p,"Other Wybie","Wybie gestures toward the party. We need the front sitting room free and a little time before the feast. Try again in a moment.",120);}}
            case "social"->{if(choice==0)hint(p,s,g);else root(p,s,g);}
            case "hint"->{if(choice==0)reveal(p,s,g);else root(p,s,g);}
            case "revealed"->root(p,s,g);
            case "games"->{if(choice>=TabletopGames.Game.values().length){root(p,s,g);break;}c.putString("Game",TabletopGames.Game.values()[choice].name());show(p,s,g,"players","How many people shall we invite to the table?",List.of("Just us (2 players)","Three players","Four players","Back"),0);}
            case "players"->{if(choice==3){games(p,s,g);break;}int count=choice+2;var roster=nearby(p,s,g,count);if(roster.size()!=count-1){show(p,s,g,"revealed","There aren't enough available guests nearby for that game. Try a smaller group.",List.of("All right."),0);break;}var ids=new ListTag();for(var guest:roster){var id=new CompoundTag();id.putUUID("Id",guest.getUUID("Id"));ids.add(id);}c.put("Roster",ids);show(p,s,g,"terms",teamTerms(p,roster,TabletopGames.Game.valueOf(c.getString("Game")))+"We can play for fun, or agree on a small item wager before we begin. A draw or interrupted match returns your stake.",List.of("Play for fun","Choose a wager","Back"),0);}
            case "terms"->{if(choice==0)start(p,s,g,null);else if(choice==1)stakes(p,s,g);else games(p,s,g);}
            case "stakes"->{var offers=c.getIntArray("StakeSlots");if(choice>=offers.length){show(p,s,g,"terms","Shall we play just for fun?",List.of("Play for fun","Choose a wager","Back"),0);break;}int slot=offers[choice];var offer=PartyEscrow.offer(p,s.loop.event.getInt("Serial"),slot,1,List.of(new ItemStack(Items.EMERALD,4)));if(offer.isEmpty()){stakes(p,s,g);break;}c.putUUID("EscrowOffer",offer.getUUID("Nonce"));var stake=ItemStack.of(offer.getCompound("Stake"));show(p,s,g,"confirm","YOU RISK: "+stake.getCount()+" "+stake.getHoverName().getString()+"\nYOU CAN WIN: 4 Emeralds, plus your returned stake.\nA loss forfeits only this stake. Draws, departures and interrupted matches refund it.",List.of("Accept this exact wager","Cancel wager"),0);}
            case "confirm"->{if(choice==1){stakes(p,s,g);break;}UUID token=c.hasUUID("EscrowOffer")?PartyEscrow.accept(p,s.loop.event.getInt("Serial"),c.getUUID("EscrowOffer")):null;if(token==null){stakes(p,s,g);break;}start(p,s,g,token);}
            default->close(p,s);
        }return true;
    }
    private static void social(ServerPlayer p,VisitData.Scene s,CompoundTag g,int topic){
        if(g.getString("Kind").equals("other_wybie")){PartyGuests.gesture(PartyGuests.entity(p,g),topic==0?1:2,50);show(p,s,g,"social",topic==0?"Wybie points toward Other Mother, then sweeps a hand around the room. He gives a small, careful shrug.":"Wybie nods, counts several visits on his fingers, then glances toward the little door. His smile fades for a moment.",List.of("Anything unusual?","Back"),0);return;}
        if(PlayerParty.isPlayerParty(s)){show(p,s,g,"social","The guest points out the decorations and the bustle of the celebration, then turns back to listen.",List.of("Have you noticed anything unusual?","Back"),0);return;}
        String reply=socialReply(g,topic);
        show(p,s,g,"social",reply,List.of("Have you noticed anything unusual?","I'd like to ask something else."),0);
    }
    static String socialReply(CompoundTag g,int topic){
        return switch(prefix(g)){
            case "bobinsky"->topic==0?"A good hostess knows a good performance! She invites me, I bring the stories. The mice would come too, but their table manners need work.":"Many evenings! Always a feast, always music. Listen carefully and you may hear a smaller orchestra beneath our feet.";
            case "spink"->topic==0?"Through the theatre, darling. She has such an eye for spectacle. Of course, April and I know a thing or two about making an entrance.":"Oh, yes. We usually sit together. A party is much better when you have someone beside you who remembers how the story really went.";
            case "forcible"->topic==0?"She asked us to perform, and Miriam insisted we stay for supper. I suspect the cake persuaded her more than the applause.":"Often enough to know the best seat and the worst draught. Don't let all this lovely scenery distract you from the people behind it.";
            default->topic==0?switch(Math.floorMod(g.getInt("Order"),3)){case 0->"I met her in the garden. She remembered my favourite cake before I'd told her. Quite a talent, don't you think?";case 1->"She invited my whole street once. The house seemed to have a place ready for everyone. How did you meet her?";default->"A neighbour brought me along. I meant to stay for one cup of tea, and here I am learning everyone's names.";}:"I've visited before, but never with quite this crowd. "+switch(Math.floorMod(g.getInt("Order"),3)){case 0->"Last time we played cards until the candles burned low.";case 1->"I like talking to the guests better than admiring the decorations.";default->"The music is familiar. The company makes each evening different.";};
        };
    }
    private static void hint(ServerPlayer p,VisitData.Scene s,CompoundTag g){
        String prefix=prefix(g);String secret=switch(prefix){case "bobinsky"->"rat_communication";case "spink"->"drawer_marks";case "forcible"->"hidden_passages";default->new String[]{"blind_spots","hollow_trees","gift_preparation"}[Math.floorMod(g.getInt("Order")+s.loop.visits,3)];};
        var c=HouseParty.data(s).getCompound("Conversation");c.putString("Secret",secret);
        String follow=switch(prefix){case "bobinsky"->"Who hears what the messengers say?";case "spink"->"What pattern should I look for?";case "forcible"->"How can I see behind the scenery?";default->"What did you notice, exactly?";};
        if(!prefix.equals("guest"))spoken(p,s,g,"hint",prefix+"_secret_hint",List.of(follow,"Let's talk about something else."));
        else show(p,s,g,"hint",g.getString("Kind").equals("other_wybie")?"Wybie glances toward the windows, then traces a little pattern in the air.":"Some things here look different when you watch them twice. The house has habits, just like its hostess.",List.of(follow,"Let's talk about something else."),0);
    }
    private static void reveal(ServerPlayer p,VisitData.Scene s,CompoundTag g){
        var n=HouseParty.data(s);String id=n.getCompound("Conversation").getString("Secret");var secrets=s.loop.secrets;
        if(!HousePartyLedger.mayClaimSecret(n,g)||!SecretRegistry.ALL.containsKey(id)||secrets.journal.contains(id)||secrets.issued.containsValue(id)){
            show(p,s,g,"revealed","That's all I can show you tonight. You may already have noticed it yourself.",List.of("Thank you."),0);return;
        }
        HousePartyLedger.claimedSecret(n,g,id);SecretTransactions.issue(p,id);
        show(p,s,g,"revealed",g.getString("Kind").equals("other_wybie")?"Wybie quietly passes you a small clue. You can file it in your Secret Journal later.":"Keep this little detail in mind. You can file the clue in your Secret Journal when you have a quiet moment.",List.of("Thank you."),0);
    }
    private static void games(ServerPlayer p,VisitData.Scene s,CompoundTag g){
        if(HouseParty.data(s).getBoolean("FeastDue")||PartyMatches.active(s)){show(p,s,g,"revealed",PlayerParty.isPlayerParty(s)?"We'll have to save the games for another time. Your celebration is about to begin.":"We'll have to save the games for another time. The feast is about to begin.",List.of("All right."),0);return;}
        var options=new ArrayList<String>();for(var game:TabletopGames.Game.values())options.add(switch(game){case BUTTON_CARDS->"Button Cards";case THREAD_FOUR->"Thread Four";case WOBBLY_TOWER->"Wobbly Tower";case MOUSEBOARD_DASH->"Mouseboard Dash";case BUTTON_FLICK->"Button Flick";case SILENT_CHARADES->"Silent Charades";});options.add("Back");show(p,s,g,"games","What would you like to play?",options,0);
    }
    private static List<CompoundTag> nearby(ServerPlayer p,VisitData.Scene s,CompoundTag first,int total){
        var result=new ArrayList<CompoundTag>();var host=PartyGuests.entity(p,first);if(host==null)return result;result.add(first);
        for(var g:PartyGuests.guests(s)){if(result.size()>=total-1)break;var actor=PartyGuests.entity(p,g);if(g==first||actor==null||actor.distanceToSqr(host)>100||!actor.hasLineOfSight(host)||!Set.of("WANDERING","CONVERSING").contains(g.getString("State")))continue;result.add(g);}return result;
    }
    private static String teamTerms(ServerPlayer p,List<CompoundTag> roster,TabletopGames.Game game){
        var people=PartyMatches.participants(p.getUUID(),roster,game);
        if(game!=TabletopGames.Game.THREAD_FOUR)return "Players: "+String.join(", ",people.stream().map(TabletopGames.Participant::name).toList())+".\n";
        return "Team 1: "+String.join(" + ",people.stream().filter(x->x.team()==0).map(TabletopGames.Participant::name).toList())+".\nTeam 2: "+String.join(" + ",people.stream().filter(x->x.team()==1).map(TabletopGames.Participant::name).toList())+".\n";
    }
    private static void stakes(ServerPlayer p,VisitData.Scene s,CompoundTag g){
        var choices=new ArrayList<String>();var slots=new ArrayList<Integer>();for(int i=0;i<p.getInventory().items.size()&&slots.size()<3;i++){var item=p.getInventory().getItem(i);if(!PartyEscrow.safeStake(item))continue;slots.add(i);choices.add("Risk 1 "+item.getHoverName().getString()+" for 4 Emeralds");}
        HouseParty.data(s).getCompound("Conversation").putIntArray("StakeSlots",slots);choices.add("Back");show(p,s,g,"stakes",slots.isEmpty()?"You aren't carrying an eligible ordinary material to wager. Your story items are never eligible.":"Choose the exact item you want to risk. Nothing is taken until you confirm.",choices,0);
    }
    private static void start(ServerPlayer p,VisitData.Scene s,CompoundTag g,UUID token){
        var c=HouseParty.data(s).getCompound("Conversation");var roster=new ArrayList<CompoundTag>();for(var tag:c.getList("Roster",Tag.TAG_COMPOUND)){var id=(CompoundTag)tag;var guest=HousePartyLedger.guest(HouseParty.data(s),id.getUUID("Id"));if(guest!=null)roster.add(guest);}
        TabletopGames.Game game;try{game=TabletopGames.Game.valueOf(c.getString("Game"));}catch(IllegalArgumentException e){if(token!=null){PartyEscrow.settle(p,token,PartyEscrow.Outcome.CANCEL);PartyEscrow.deliverPending(p);}close(p,s);return;}
        boolean started=PartyMatches.start(p,s,roster,game,token);close(p,s);if(!started)VisitAudio.caption(p,PlayerParty.isPlayerParty(s)?"Your Party":"House Party","We need an open game table and nearby guests. Any accepted stake has been returned or saved safely for you.",140);
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){var n=HouseParty.data(s);var c=n.getCompound("Conversation");if(!c.hasUUID("Guest"))return;var g=HousePartyLedger.guest(n,c.getUUID("Guest"));var actor=g==null?null:PartyGuests.entity(p,g);if(actor==null||actor.distanceToSqr(p)>25||!actor.hasLineOfSight(p)||n.getLong("Clock")>c.getLong("Expires")||s.loop.secrets.searching())close(p,s);}
    public static void closeGuest(ServerPlayer p,VisitData.Scene s,UUID id){var c=HouseParty.data(s).getCompound("Conversation");if(c.hasUUID("Guest")&&c.getUUID("Guest").equals(id))close(p,s);}
    public static void close(ServerPlayer p,VisitData.Scene s){var n=HouseParty.data(s);var c=n.getCompound("Conversation");if(c.hasUUID("Guest")){var g=HousePartyLedger.guest(n,c.getUUID("Guest"));if(g!=null&&g.getString("State").equals("TALKING_TO_PLAYER"))g.putString("State","WANDERING");}n.remove("Conversation");var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putBoolean("Close",true);SecretsNetwork.send(p,packet);}
    public static String debugOperator(ServerPlayer p,VisitData.Scene s,String action,String value){
        if(!NumberedDebugActions.operator(p,s)||HouseParty.active(p)!=s)return "No owned operator party.";
        var guests=PartyGuests.guests(s);if(guests.isEmpty())return "No actual guest roster exists.";
        int index=NumberedDebugActions.integer(value,0,guests.size()-1);var g=guests.get(index);var actor=PartyGuests.entity(p,g);
        if(actor==null||actor.level()!=p.level()||actor.distanceToSqr(p)>16||!actor.hasLineOfSight(p)
                ||!Set.of("WANDERING","CONVERSING","TALKING_TO_PLAYER").contains(g.getString("State"))
                ||s.loop.secrets.searching()||SecretDialogue.busy(p)||PartyMatches.active(s)||Reconciliation.active(p))return "Choose a visible, nearby, available guest during the social phase.";
        close(p,s);var session=new CompoundTag();session.putUUID("Guest",g.getUUID("Id"));HouseParty.data(s).put("Conversation",session);g.putString("State","TALKING_TO_PLAYER");
        switch(action){
            case "conversation"->root(p,s,g);
            case "wager"->games(p,s,g); // Normal roster, terms and exact inventory wager confirmation remain mandatory.
            case "secret"->{
                String id=switch(prefix(g)){case "bobinsky"->"rat_communication";case "spink"->"drawer_marks";case "forcible"->"hidden_passages";default->new String[]{"blind_spots","hollow_trees","gift_preparation"}[Math.floorMod(g.getInt("Order")+s.loop.visits,3)];};
                session.putString("Secret",id);reveal(p,s,g); // Existing per-guest, per-party, issued/journal and real-item guards.
            }
            default->{close(p,s);return "Unknown guest conversation control.";}
        }
        OtherMotherLoop.changed(p);return "Opened the actual guest interaction with normal reward/escrow guards.";
    }

    private PartyConversations(){}
}
