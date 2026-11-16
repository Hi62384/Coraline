package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import java.util.*;

/** Owner/serial-bound dinner errands. Progress is saved with the event, never with a GUI. */
public final class DinnerActivities {
    static final int TASKS=7;
    private record Offer(UUID nonce,UUID door,int serial,String node,long expires){}
    private static final Map<UUID,Offer> OFFERS=new HashMap<>();
    private static CompoundTag data(VisitData.Scene s){
        if(!s.loop.event.contains("DinnerActivities"))s.loop.event.put("DinnerActivities",new CompoundTag());
        return s.loop.event.getCompound("DinnerActivities");
    }
    static int task(VisitData.Scene s){return data(s).getInt("Task");}
    static void forget(UUID id){OFFERS.remove(id);}
    static void clear(){OFFERS.clear();}
    static void cleanup(VisitData.Scene s){s.loop.event.remove("DinnerActivities");}
    static void ensureTable(ServerPlayer p,VisitData.Scene s){
        if(s.table==null)return;
        var base=net.minecraft.world.phys.Vec3.atBottomCenterOf(s.table);
        Item[] contents={Items.CAKE,Items.COOKIE,Items.HONEY_BOTTLE,Items.SWEET_BERRIES};
        String[] labels={"Berry cake","Cookies","Cider jug","Recipe bowl"};
        for(int i=0;i<4;i++){
            var prop=FamilyDinner.find(p,s,ScenePropEntity.PLATE,10+i);
            if(i==2&&prop!=null){prop.kind(ScenePropEntity.DINNER_JUG);}
            if(i==2&&prop==null)prop=FamilyDinner.find(p,s,ScenePropEntity.DINNER_JUG,12);
            if(prop==null){prop=VisitEvents.prop(p,s,i==2?ScenePropEntity.DINNER_JUG:ScenePropEntity.PLATE,base.add(i%2==0?-.85:.85,.9,i<2?-3.1:-2.3),10+i);prop.food(0,new ItemStack(contents[i]));}
            if(i==2)prop.setPos(base.add(0,.91,.25));
            prop.setCustomName(Component.literal(labels[i]));prop.setCustomNameVisible(false);
        }
    }
    static void tick(ServerPlayer p,VisitData.Scene s){
        var n=data(s);int elapsed=s.loop.event.getInt("MealElapsed");
        // Also migrate already-running dinners from the old wall of floating labels.
        if(!n.getBoolean("QuietGuidance")){ensureTable(p,s);for(var prop:VisitEvents.props(p,s))prop.setCustomNameVisible(false);n.putBoolean("QuietGuidance",true);}
        if(!n.contains("Recipe"))n.putInt("Recipe",p.getRandom().nextInt(3));
        // Idempotent setup: reconnecting or suspending must not refill/reset an errand.
        if(!n.contains("PreparedTask")||n.getInt("PreparedTask")!=task(s)){
            n.putInt("PreparedTask",task(s));
            for(var prop:VisitEvents.props(p,s))prop.setGlowingTag(false);
            if(task(s)==0){var plate=FamilyDinner.find(p,s,ScenePropEntity.PLATE,4);if(plate!=null)plate.setGlowingTag(true);}
            if(task(s)==1){
                int dish=p.getRandom().nextBoolean()?8:9;n.putInt("Dish",dish);
                var plate=FamilyDinner.find(p,s,ScenePropEntity.PLATE,dish);
                if(plate!=null){plate.food(0,new ItemStack(dish==8?Items.BREAD:Items.BAKED_POTATO,3));plate.setGlowingTag(true);}
            }
            if(task(s)==2)for(int slot=1;slot<=2;slot++){var glass=FamilyDinner.find(p,s,ScenePropEntity.GLASS,slot);if(glass!=null)glass.food(0,ItemStack.EMPTY);}
            if(task(s)==3){var bowl=FamilyDinner.find(p,s,ScenePropEntity.PLATE,13);if(bowl!=null)bowl.setGlowingTag(true);}
            if(task(s)==4)for(int slot=10;slot<=11;slot++){var plate=FamilyDinner.find(p,s,ScenePropEntity.PLATE,slot);if(plate!=null)plate.setGlowingTag(true);}
            if(task(s)==6){var plate=FamilyDinner.find(p,s,ScenePropEntity.PLATE,4);if(plate!=null)plate.setGlowingTag(true);}
        }
        // Only the next physical action glows; picking up the jug changes the target.
        if(task(s)==2){for(var prop:VisitEvents.props(p,s))prop.setGlowingTag(false);
            int filled=n.getInt("Refilled");var target=n.getBoolean("Jug")?FamilyDinner.find(p,s,ScenePropEntity.GLASS,(filled&1)==0?1:2):FamilyDinner.find(p,s,ScenePropEntity.DINNER_JUG,12);
            if(target!=null)target.setGlowingTag(true);
        }
        var offer=OFFERS.get(p.getUUID());
        for(boolean mom:new boolean[]{true,false}){
            var parent=mom?com.coraline.secrets.MotherSearch.mother(p):VisitEvents.father(p,s);
            if(parent!=null)parent.setGlowingTag(task(s)==5&&(n.getInt("Toasts")&(mom?2:1))==0);
        }
        if(offer!=null&&OtherMotherLoop.now(p)>offer.expires)close(p);
    }
    static String hint(VisitData.Scene s){var n=data(s);return switch(task(s)){
        case 0->"Right-click food on your glowing plate to taste it ("+n.getInt("Bites")+"/3).";
        case 1->"Other Father: Please pass the highlighted "+(n.getInt("Dish")==8?"bread":"potatoes")+".";
        case 2->!n.getBoolean("Jug")?"Right-click the glowing cider jug in the middle of the table.":"Right-click "+((n.getInt("Refilled")&1)==0?"Other Mother's":"Other Father's")+" glowing empty glass to pour cider.";
        case 3->n.getBoolean("Sniffed")?"Right-click Other Mother and choose 'Can I learn your recipe?'":"Right-click the glowing recipe bowl to smell it.";
        case 4->n.contains("Serving")?"Right-click "+(n.getInt("Serving")==10?"Other Mother's":"Other Father's")+" plate to serve dessert.":"Right-click the "+((n.getInt("Desserts")&1)==0?"berry cake":"cookies")+" to take a serving.";
        case 5->"Right-click "+((n.getInt("Toasts")&2)==0?"Other Mother":"Other Father")+" and choose 'Make a toast'.";
        case 6->"Clear your own plate to help tidy up.";
        default->"All seven dinner tasks done! Right-click Other Mother or Other Father to finish dinner.";
    };}
    private static boolean seated(ServerPlayer p,VisitData.Scene s){
        return s!=null&&VisitEvents.type(s)==VisitEvents.Type.FAMILY_DINNER&&s.loop.phase==OtherMotherLoopState.Phase.DINNER
            &&s.owner.equals(p.getUUID())&&!s.loop.secrets.searching()&&!SecretDialogue.busy(p)
            &&com.coraline.otherworld.OtherWorld.isOther(p.level())&&s.table!=null&&p.distanceToSqr(s.table.getCenter())<144
            &&(p.getVehicle()==null||p.getVehicle() instanceof ScenePropEntity chair&&chair.kind()==ScenePropEntity.CHAIR&&chair.slot==0
            &&p.getUUID().equals(chair.owner)&&Objects.equals(s.door,chair.storyDoor)
            &&chair.getPersistentData().getInt("VisitEventSerial")==s.loop.event.getInt("Serial"));
    }
    static boolean parent(ServerPlayer p,ParentEntity parent){
        var s=OtherMotherLoop.active(p);
        if(!seated(p,s)||!Objects.equals(parent.getUUID(),parent.isMom()?s.mom:s.dad)||parent.distanceToSqr(p)>100)return false;
        open(p,s,parent.isMom()?"mother":"father");return true;
    }
    static boolean interact(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(!seated(p,s))return false;
        var n=data(s);int stage=task(s);
        if(prop.kind()==ScenePropEntity.TABLE){p.displayClientMessage(Component.literal(hint(s)+" Right-click a parent to talk."),true);return true;}
        if(prop.kind()==ScenePropEntity.GLASS&&prop.slot>0){
            if(stage==2&&n.getBoolean("Jug")){
                int bit=1<<(prop.slot-1);if((n.getInt("Refilled")&bit)==0){prop.food(0,new ItemStack(Items.HONEY_BOTTLE));prop.setGlowingTag(false);n.putInt("Refilled",n.getInt("Refilled")|bit);sound(p,SoundEvents.BOTTLE_EMPTY);
                    if(n.getInt("Refilled")==3)complete(p,s,"Other Mother","There. Now everyone has a drink. What a thoughtful host you're becoming.");else caption(p,"Other Father","That's mine filled. Your Other Mother's glass is still empty.");}
            }else p.displayClientMessage(Component.literal("Use the cider jug when it's time to refill the glasses."),true);
            return true;
        }
        if(prop.kind()!=ScenePropEntity.PLATE&&prop.kind()!=ScenePropEntity.DINNER_JUG)return false;
        if(prop.slot==12){n.putBoolean("Jug",true);caption(p,"Other Mother",stage==2?"Hold it steady, sweetheart. Now click each empty glass.":"That's our cider jug. We'll top up everyone's glasses after the first course.");return true;}
        if(prop.slot==13){n.putBoolean("Sniffed",true);caption(p,"Other Mother",switch(n.getInt("Recipe")){case 0->"The bowl smells of warm honey and summer flowers. What do you think I put in it?";case 1->"A sharp, fruity scent rises from the bowl, with little purple berry stains at the rim.";default->"The sauce smells of rich cocoa. There are tiny chocolate shavings on the spoon.";});return true;}
        if(prop.slot==10||prop.slot==11){
            if(stage==4){n.putInt("Serving",prop.slot);caption(p,"Other Father",prop.slot==10?"One slice of berry cake. Take that over to your Other Mother's plate.":"Those cookies are for me! Click my plate to serve them.");}
            else caption(p,"Other Mother","Save some room! We'll serve dessert after our little recipe game.");return true;
        }
        if(prop.slot==5||prop.slot==6){
            if(stage==4){int required=prop.slot==5?10:11,bit=prop.slot==5?1:2;
                if(n.getInt("Serving")!=required){caption(p,prop.slot==5?"Other Mother":"Other Father",prop.slot==5?"A slice of berry cake for me, please.":"Could I have the cookies, kiddo?");return true;}
                if((n.getInt("Desserts")&bit)==0){prop.food(0,new ItemStack(required==10?Items.CAKE:Items.COOKIE));prop.food(1,ItemStack.EMPTY);prop.food(2,ItemStack.EMPTY);n.putInt("Desserts",n.getInt("Desserts")|bit);n.remove("Serving");sound(p,SoundEvents.GLASS_PLACE);
                    if(n.getInt("Desserts")==3)complete(p,s,"Other Father","Perfect! You remembered both orders. Now, how about a toast?");else caption(p,"Other Mother","Thank you, sweetheart. Don't forget your Other Father.");}
            }return true;
        }
        if(stage==1&&prop.slot==n.getInt("Dish")){
            var plate=FamilyDinner.find(p,s,ScenePropEntity.PLATE,6);if(plate!=null)plate.food(0,prop.food(0).copyWithCount(1));
            prop.food(0,ItemStack.EMPTY);prop.setGlowingTag(false);sound(p,SoundEvents.GLASS_PLACE);
            complete(p,s,"Other Father","Thanks, kiddo! Could you top up our cider next? The jug's in the middle.");return true;
        }
        if(prop.slot==8||prop.slot==9){if(stage==1)caption(p,"Other Father","The other dish, please. I've highlighted the one I meant.");return true;}
        if(stage==6&&prop.slot==4){for(int i=0;i<3;i++)prop.food(i,ItemStack.EMPTY);prop.setGlowingTag(false);complete(p,s,"Other Mother","All tidied away. Stay and chat, or tell us when you're ready to finish.");return true;}
        return false;
    }
    static void ate(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop,boolean hadFood){
        if(!hadFood||task(s)!=0||prop.slot!=4)return;var n=data(s);n.putInt("Bites",n.getInt("Bites")+1);
        if(n.getInt("Bites")>=3)complete(p,s,"Other Father","Three tastes! Which was your favourite? You can tell us at the table. Could you pass me a dish first?");
        else caption(p,"Other Mother",n.getInt("Bites")==1?"How is the first bite? Try the other things on your plate, too.":"One more taste. Every dish has a little story behind it.");
    }
    private static void complete(ServerPlayer p,VisitData.Scene s,String who,String text){
        var n=data(s);if(task(s)>=TASKS)return;n.putInt("Task",task(s)+1);
        OtherMotherLoop.relationship(p,s.loop.trust+.003F,s.loop.suspicion);caption(p,who,text);OtherMotherLoop.changed(p);
    }
    private static void caption(ServerPlayer p,String who,String text){VisitAudio.caption(p,who,text,100);}
    private static void sound(ServerPlayer p,SoundEvent sound){p.level().playSound(null,p.blockPosition(),sound,SoundSource.NEUTRAL,.3F,1);}
    static void status(ServerPlayer p,VisitData.Scene s,CompoundTag packet){
        boolean meal=VisitEvents.type(s)==VisitEvents.Type.FAMILY_DINNER&&!s.loop.secrets.searching()
            &&Set.of(OtherMotherLoopState.Phase.DINNER_GREETING,OtherMotherLoopState.Phase.DINNER,OtherMotherLoopState.Phase.DINNER_FINISH).contains(s.loop.phase);
        packet.putBoolean("DinnerMusic",meal);packet.putLong("DinnerPosition",s.table==null?0:s.table.asLong());
        if(meal&&s.loop.phase==OtherMotherLoopState.Phase.DINNER){packet.putString("ShowObjective","Dinner");packet.putString("DinnerHint",hint(s));packet.putInt("Remaining",0);}
    }
    static boolean readyToFinish(VisitData.Scene s){return task(s)>=TASKS&&data(s).getBoolean("Finish");}
    private static void open(ServerPlayer p,VisitData.Scene s,String node){
        var n=data(s);String speaker="Other Father",text;List<String> options;
        switch(node){
            case "root"->{text=hint(s)+" What would you like to do?";options=List.of("Talk to Other Father","Talk to Other Mother",task(s)>=TASKS?"I'm ready to finish dinner.":"What can I help with?","Back to the meal");}
            case "father"->{text=hint(s)+" Dinner's better when we have something to talk about, kiddo.";options=List.of("Tell me about your music.","What's growing in the garden?",task(s)>=TASKS?"I'm ready to finish dinner.":task(s)==5?"Make a toast with Other Father.":"What was your favourite dish?","Back to the meal");}
            case "mother"->{speaker="Other Mother";text=hint(s)+" Is there anything else you would like, sweetheart?";options=List.of("Can I learn your recipe?","Why is everything here so perfect?",task(s)>=TASKS?"I'm ready to finish dinner.":task(s)==5?"Make a toast with Other Mother.":"What should we do after dinner?","Back to the meal");}
            case "music"->{text="I started with a little tune and let it wander. Sometimes the tune seems to know the house better than I do.";options=List.of("Could we make up a song together?","Does Mother like your music?","Ask about something else");}
            case "song"->{text="You pick the rhythm, I'll find the notes. Tap your spoon: slow, quick, quick. There! That's our dinner song.";options=List.of("Let's play it cheerfully.","Make it a mysterious tune.","Back to the table");}
            case "garden"->{text="The flowers lean toward anyone who listens. If you visit, look for the little paths between the tallest stems.";options=List.of("Do you look after them yourself?","Do the flowers ever stop listening?","Ask about something else");}
            case "recipe"->{speaker="Other Mother";text=task(s)==3?(n.getBoolean("Sniffed")?"You've smelled the sauce. Which ingredient did you recognise?":"Inspect the little recipe bowl first. A good cook uses their nose as well as their eyes."):"Honey for warmth, berries for sharpness, chocolate for a treat. You'll find a recipe bowl on the table when it's time to guess.";options=task(s)==3&&n.getBoolean("Sniffed")?List.of("Honey","Sweet berries","Cocoa","Back to the table"):List.of("How do you get it just right?","Could I help cook next time?","Back to the table");}
            case "toast_father", "toast_mother"->{speaker=node.equals("toast_mother")?"Other Mother":"Other Father";text="What shall we raise our glasses to?";options=List.of("To looking after each other.","To new adventures.","To home, wherever it is.","Not just yet.");}
            default->{speaker=n.getString("ReplySpeaker");text=n.getString("Reply");options=List.of("Ask Other Father something else","Ask Other Mother something else","Back to the meal");}
        }
        String spoken=text.startsWith(hint(s)+" ")?text.substring(hint(s).length()+1):text;
        IncidentalDialogue.say(p,speaker,spoken); // Objective/button text remains on screen, not in the spoken take.
        var offer=new Offer(UUID.randomUUID(),s.door,s.loop.event.getInt("Serial"),node,OtherMotherLoop.now(p)+1200);OFFERS.put(p.getUUID(),offer);
        var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putUUID("Nonce",offer.nonce);packet.putString("Speaker",speaker);packet.putString("Text",text);packet.put("Options",SecretsState.strings(options));packet.putInt("ReadyTicks",0);SecretsNetwork.send(p,packet);
    }
    private static void reply(ServerPlayer p,VisitData.Scene s,String who,String text){data(s).putString("ReplySpeaker",who);data(s).putString("Reply",text);open(p,s,"reply");}
    static boolean choice(ServerPlayer p,UUID nonce,String value){
        var offer=OFFERS.get(p.getUUID());if(offer==null||!offer.nonce.equals(nonce))return false;
        var s=OtherMotherLoop.active(p);
        if(!seated(p,s)||!Objects.equals(offer.door,s.door)||offer.serial!=s.loop.event.getInt("Serial")||OtherMotherLoop.now(p)>offer.expires){close(p);return true;}
        if(value.equals("close")){close(p);return true;}
        int answer;try{answer=Integer.parseInt(value);}catch(NumberFormatException ex){return true;}
        if(answer<0||answer>3)return true;OFFERS.remove(p.getUUID());var n=data(s);
        if((offer.node.equals("father")||offer.node.equals("mother"))&&answer==3){close(p);return true;}
        if((offer.node.equals("father")||offer.node.equals("mother"))&&answer==2&&task(s)>=TASKS){n.putBoolean("Finish",true);close(p);OtherMotherLoop.changed(p);return true;}
        switch(offer.node){
            case "root"->{if(answer<2)open(p,s,answer==0?"father":"mother");else if(answer==2){if(task(s)>=TASKS){n.putBoolean("Finish",true);close(p);}else reply(p,s,"Other Mother",hint(s));}else close(p);}
            case "father"->{if(answer==0)open(p,s,"music");else if(answer==1)open(p,s,"garden");else if(answer==2){if(task(s)==5)open(p,s,"toast_father");else reply(p,s,"Other Father","The roast was lovely, but I always save a little room for cookies. What about you?");}else open(p,s,"root");}
            case "mother"->{if(answer==0)open(p,s,"recipe");else if(answer==1)reply(p,s,"Other Mother","Perfect? Only because I pay attention to what everyone wants. You do tell me what you want, don't you?");else if(answer==2){if(task(s)==5)open(p,s,"toast_mother");else reply(p,s,"Other Mother","Let's enjoy the evening we have first. There is always something else waiting for us.");}else open(p,s,"root");}
            case "music"->{if(answer==0)open(p,s,"song");else if(answer==1)reply(p,s,"Other Father","She likes everything just so. I try to leave a little room for surprises in the music.");else open(p,s,"father");}
            case "song"->{if(answer==2)open(p,s,"root");else{sound(p,answer==0?SoundEvents.NOTE_BLOCK_HARP.get():SoundEvents.NOTE_BLOCK_CHIME.get());reply(p,s,"Other Father",answer==0?"That's it! A bright little chorus. We ought to remember this one.":"A quiet tune for a house full of secrets. You have an ear for atmosphere, kiddo.");}}
            case "garden"->{if(answer==2)open(p,s,"father");else reply(p,s,"Other Father",answer==0?"I do what I can. A little patience, a little water, and knowing when to leave things alone.":"I sometimes wonder that myself. Best not to tell a flower anything you wouldn't sing aloud.");}
            case "recipe"->{
                if(task(s)==3&&n.getBoolean("Sniffed")){if(answer==3)open(p,s,"root");else if(answer==n.getInt("Recipe")){complete(p,s,"Other Mother","Exactly right! A fine nose. Now let's get those desserts served.");reply(p,s,"Other Mother","You found the secret ingredient. Berry cake for me and cookies for your Other Father, please.");}else reply(p,s,"Other Mother","Not quite. Smell the recipe bowl again and look for the clue before trying another ingredient.");}
                else if(answer==2)open(p,s,"root");else reply(p,s,"Other Mother",answer==0?"Taste as you go, and never stop paying attention. The smallest ingredient can change everything.":"Of course. A little help can make a meal feel like something we've made together.");
            }
            case "toast_father", "toast_mother"->{
                if(answer==3){open(p,s,"root");break;}boolean mom=offer.node.equals("toast_mother");int bit=mom?2:1;
                if(task(s)==5){n.putInt("Toasts",n.getInt("Toasts")|bit);sound(p,SoundEvents.GLASS_PLACE);if(n.getInt("Toasts")==3)complete(p,s,"Other Mother","To a lovely evening. Just your plate left to clear, sweetheart.");}
                reply(p,s,mom?"Other Mother":"Other Father",switch(answer){case 0->mom?"To taking very good care of one another. I'll drink to that.":"That's a toast worth keeping, kiddo. Nobody should have to do everything alone.";case 1->mom?"To adventures. Though do remember to come back for supper.":"To all the things we haven't discovered yet!";default->mom?"Home is where you're wanted, sweetheart. Remember that.":"To home. And to finding your own way there.";});
            }
            default->{if(answer<2)open(p,s,answer==0?"father":"mother");else close(p);}
        }
        OtherMotherLoop.changed(p);return true;
    }
    static void close(ServerPlayer p){forget(p.getUUID());var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putBoolean("Close",true);SecretsNetwork.send(p,packet);}
    private DinnerActivities(){}
}
