package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** Repeatable dinner, independent of the first-visit supper and Grand Feast. */
public final class FamilyDinner {
    public static final String[] FATHER_VOICES={"father_dinner_greeting_1","father_dinner_greeting_2","father_pass_bread","father_pass_potatoes","father_dinner_comment_1","father_dinner_comment_2","father_dinner_question","father_dinner_finish"};
    public static OtherMotherDialogue.Pool pool(String category,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/family_dinner/"+category+"/"+SecretDialogue.stateId(state),2);}
    public static int graceTicks(float suspicion,double u){return (int)Math.round(1200*(1+9*Math.pow(Math.max(0,Math.min(1,u)),.4+6*OtherMotherLoopState.clamp(suspicion))));}
    static void finishBiteIfDone(ParentEntity parent,ServerPlayer p){if(parent.getPersistentData().contains("DinnerBiteEnd")&&OtherMotherLoop.now(p)>=parent.getPersistentData().getLong("DinnerBiteEnd"))finishBite(parent);}
    public static void beginWindow(ServerPlayer p,VisitData.Scene s){s.loop.event.remove("DinnerResumePhase");s.loop.event.putBoolean("DinnerPending",true);s.loop.event.putInt("ArrivalRemaining",6000);s.loop.event.putBoolean("GraceRolled",false);s.loop.event.putBoolean("Searching",false);OtherMotherLoop.phase(p,s,DINNER_WAIT);ensureTable(p,s);}
    static ScenePropEntity find(ServerPlayer p,VisitData.Scene s,int kind,int slot){return VisitEvents.props(p,s).stream().filter(e->e.kind()==kind&&e.slot==slot).findFirst().orElse(null);}
    static void ensureTable(ServerPlayer p,VisitData.Scene s){
        if(s.table==null)return;var base=Vec3.atBottomCenterOf(s.table);
        if(find(p,s,ScenePropEntity.TABLE,0)==null)VisitEvents.prop(p,s,ScenePropEntity.TABLE,base,0).longDining(true);
        for(int i=0;i<3;i++)if(find(p,s,ScenePropEntity.CHAIR,i)==null){var chair=VisitEvents.prop(p,s,ScenePropEntity.CHAIR,SupperSeating.position(s.table,i),i);chair.setYRot(SupperSeating.facing(i));}
        for(int i=0;i<4;i++)if(find(p,s,ScenePropEntity.PLATE,i)==null){var plate=VisitEvents.prop(p,s,ScenePropEntity.PLATE,base.add(i%2==0?-.98:.98,.89,i<2?-1.9:1.9),i);for(int f=0;f<3;f++)plate.food(f,FeastContent.serving(p.getRandom(),i));}
        // Two explicitly selectable serving dishes for the one passing interaction.
        if(find(p,s,ScenePropEntity.PLATE,8)==null){var bread=VisitEvents.prop(p,s,ScenePropEntity.PLATE,base.add(-.8,.9,-2.5),8);bread.food(0,new ItemStack(Items.BREAD,3));}
        if(find(p,s,ScenePropEntity.PLATE,9)==null){var potatoes=VisitEvents.prop(p,s,ScenePropEntity.PLATE,base.add(.8,.9,-2.5),9);potatoes.food(0,new ItemStack(Items.BAKED_POTATO,3));}
        for(int i=0;i<3;i++){
            double x=i==0?0:i==1?-.85:.85,z=i==0?-4.1:4.1;
            if(find(p,s,ScenePropEntity.PLATE,4+i)==null){var plate=VisitEvents.prop(p,s,ScenePropEntity.PLATE,base.add(x,.89,z),4+i);for(int f=0;f<3;f++)plate.food(f,FeastContent.serving(p.getRandom(),p.getRandom().nextInt(4)));}
            if(find(p,s,ScenePropEntity.GLASS,i)==null)VisitEvents.prop(p,s,ScenePropEntity.GLASS,base.add(x+.5,.9,z),i).food(0,FeastContent.random(p.getRandom(),true));
        }
        DinnerActivities.ensureTable(p,s);
    }
    static void seatParent(ParentEntity parent,ServerPlayer p,VisitData.Scene s){
        if(parent.getPersistentData().contains("DinnerBiteEnd")&&OtherMotherLoop.now(p)>=parent.getPersistentData().getLong("DinnerBiteEnd"))finishBite(parent);
        var chair=find(p,s,ScenePropEntity.CHAIR,parent.isMom()?1:2);if(chair==null)return;
        if(s.loop.secrets.searching()&&parent.isMom()){parent.stopRiding();return;}
        SupperSeating.approach(parent,chair);
        parent.lookAtStoryPlayer(p);
        if(s.loop.phase==DINNER&&parent.tickCount%260==0){var plate=find(p,s,ScenePropEntity.PLATE,parent.isMom()?5:6);if(plate!=null)bite(p,plate,parent);}
        if(s.loop.phase==DINNER&&s.loop.event.getInt("MealElapsed")>=1800&&parent.getPersistentData().getInt("DinnerDrinkSerial")!=s.loop.event.getInt("Serial")){var glass=find(p,s,ScenePropEntity.GLASS,parent.isMom()?1:2);if(glass!=null&&!glass.food(0).isEmpty()){parent.getPersistentData().putInt("DinnerDrinkSerial",s.loop.event.getInt("Serial"));drink(p,glass,parent);}}
    }
    static boolean fatherSpeech(ServerPlayer p,VisitData.Scene s,String name){
        var n=s.loop.event;
        if(!n.getString("FatherVoice").equals(name)){VisitAudio.play(p,name,false);n.putString("FatherVoice",name);n.putInt("FatherVoiceElapsed",0);n.putInt("FatherVoiceLength",VisitAudio.length(name));n.putBoolean("FatherVoiceFinished",false);return false;}
        int elapsed=n.getInt("FatherVoiceElapsed")+1;n.putInt("FatherVoiceElapsed",elapsed);
        if(elapsed<n.getInt("FatherVoiceLength")||!n.getBoolean("FatherVoiceFinished")&&elapsed<n.getInt("FatherVoiceLength")+40)return false;n.remove("FatherVoice");return true;
    }
    static boolean tick(ServerPlayer p,VisitData.Scene s){
        var n=s.loop.event;
        if(s.loop.phase==DINNER_WAIT){
            int remaining=n.getInt("ArrivalRemaining");if(remaining>0){n.putInt("ArrivalRemaining",remaining-1);return true;}
            if(!n.getBoolean("GraceRolled")){n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",graceTicks(s.loop.suspicion,p.getRandom().nextDouble()));OtherMotherLoop.changed(p);return true;}
            int grace=n.getInt("GraceRemaining");if(grace>0){n.putInt("GraceRemaining",grace-1);return true;}
            if(!n.getBoolean("Searching")){
                // The calling voice is queued once, before pursuit owns the actor.
                var pool=pool("search",SecretDialogue.state(p));int ticks=OtherMotherDialogue.playActivityArrival(p,pool);if(ticks==0)return true;
                n.putBoolean("Searching",true);n.putBoolean("Resuming",false);s.loop.secrets.search=SecretsState.Search.SEARCHING;s.loop.secrets.target=p.blockPosition();OtherMotherLoop.changed(p);
            }return true;
        }
        if(s.loop.phase==DINNER_GREETING){
            if(!n.getBoolean("MotherGreetingDone")){if(VisitEvents.speak(p,s,pool(n.getBoolean("Late")?"late":"ontime",OtherMotherWelcome.State.fromId(n.getString("ArrivalState")))))n.putBoolean("MotherGreetingDone",true);return true;}
            if(!fatherSpeech(p,s,n.getString("Greeting")))return true;
            String interrupted=n.getString("DinnerResumePhase");n.remove("DinnerResumePhase");
            if(interrupted.equals(DINNER_FINISH.name())){OtherMotherLoop.phase(p,s,DINNER_FINISH);return true;}
            OtherMotherLoop.phase(p,s,DINNER);
            if(!interrupted.equals(DINNER.name())){n.putInt("MealElapsed",0);n.putInt("RequestDish",p.getRandom().nextBoolean()?8:9);}
            return true;
        }
        if(s.loop.phase==DINNER){
            int elapsed=n.getInt("MealElapsed")+1;n.putInt("MealElapsed",elapsed);
            DinnerActivities.tick(p,s);
            if(elapsed>=3000||DinnerActivities.readyToFinish(s)){DinnerActivities.close(p);for(var plate:VisitEvents.props(p,s))if(plate.kind()==ScenePropEntity.PLATE)for(int i=0;i<3;i++)plate.food(i,ItemStack.EMPTY);OtherMotherLoop.phase(p,s,DINNER_FINISH);n.putBoolean("FatherFinished",false);}return true;
        }
        if(s.loop.phase==DINNER_FINISH){
            if(!n.getBoolean("FatherFinished")){if(fatherSpeech(p,s,"father_dinner_finish"))n.putBoolean("FatherFinished",true);return true;}
            if(!VisitEvents.speak(p,s,pool("finish",SecretDialogue.state(p))))return true;
            if(!n.getBoolean("CompletionAwarded")){n.putBoolean("CompletionAwarded",true);OtherMotherLoop.relationship(p,s.loop.trust+VisitConfig.DINNER_COMPLETE.get().floatValue(),s.loop.suspicion);}
            n.putBoolean("DinnerPending",false);if(p.getVehicle() instanceof ScenePropEntity prop&&Objects.equals(prop.owner,p.getUUID()))p.stopRiding();VisitEvents.next(p,s,false);return true;
        }return true;
    }
    public static boolean arrive(ServerPlayer p,VisitData.Scene s,ScenePropEntity chair){
        var n=s.loop.event;if(s.loop.phase!=DINNER_WAIT||s.loop.secrets.searching()||n.getBoolean("Searching")&&!n.getBoolean("Resuming")||SecretDialogue.busy(p)||chair.slot!=0||chair.kind()!=ScenePropEntity.CHAIR||chair.distanceToSqr(p)>9||!p.getUUID().equals(chair.owner))return false;
        if(chair.isVehicle()&&p.getVehicle()!=chair||!p.startRiding(chair,true))return false;
        p.level().playSound(null,chair.blockPosition(),net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.NEUTRAL,.2F,.75F);
        boolean late=n.getInt("ArrivalRemaining")<=0||n.getBoolean("Resuming");n.putBoolean("Late",late);n.putInt("ArrivalRemaining",0);n.putInt("GraceRemaining",0);n.putBoolean("Searching",false);n.putBoolean("MotherGreetingDone",false);
        n.putString("ArrivalState",SecretDialogue.state(p).id());n.putString("Greeting",p.getRandom().nextBoolean()?FATHER_VOICES[0]:FATHER_VOICES[1]);
        if(!n.getBoolean("ArrivalAwarded")&&!n.getBoolean("Resuming"))OtherMotherLoop.relationship(p,s.loop.trust+(late?VisitConfig.DINNER_LATE_TRUST:VisitConfig.DINNER_ON_TIME).get().floatValue(),s.loop.suspicion+(late?VisitConfig.DINNER_LATE_SUSPICION.get().floatValue():0));n.putBoolean("ArrivalAwarded",true);OtherMotherLoop.phase(p,s,DINNER_GREETING);return true;
    }
    public static boolean interact(ServerPlayer p,ScenePropEntity prop){
        if(!prop.getPersistentData().contains("VisitEventSerial"))return false;var s=OtherMotherLoop.active(p);
        if(s==null||VisitEvents.type(s)!=VisitEvents.Type.FAMILY_DINNER||!Objects.equals(prop.owner,p.getUUID())||prop.getPersistentData().getInt("VisitEventSerial")!=s.loop.event.getInt("Serial")||prop.distanceToSqr(p)>100)return true;
        if(prop.kind()==ScenePropEntity.CHAIR){if(prop.slot==0&&!arrive(p,s,prop)&&Set.of(DINNER_GREETING,DINNER,DINNER_FINISH).contains(s.loop.phase)&&!s.loop.secrets.searching()&&!SecretDialogue.busy(p)&&prop.distanceToSqr(p)<9&&!prop.isVehicle())p.startRiding(prop,true);return true;}
        if(s.loop.phase!=DINNER)return true;
        if(DinnerActivities.interact(p,s,prop))return true;
        if(prop.kind()==ScenePropEntity.GLASS&&prop.slot==0&&!prop.food(0).isEmpty()){drink(p,prop,p);return true;}
        if(prop.kind()==ScenePropEntity.PLATE){
            if(s.loop.event.getInt("PassingUntil")>0&&prop.slot==s.loop.event.getInt("RequestDish"))return true;
            var n=s.loop.event;if(prop.slot==n.getInt("RequestDish")&&prop.isCurrentlyGlowing()&&!n.getBoolean("Passed")){n.putBoolean("Passed",true);n.putInt("PassingUntil",n.getInt("MealElapsed")+30);prop.setGlowingTag(false);p.level().playSound(null,prop.blockPosition(),net.minecraft.sounds.SoundEvents.GLASS_PLACE,net.minecraft.sounds.SoundSource.NEUTRAL,.2F,1.4F);OtherMotherLoop.relationship(p,s.loop.trust+VisitConfig.DINNER_PASS.get().floatValue(),s.loop.suspicion);VisitAudio.caption(p,"Other Father","Thank you, kiddo!",60);return true;}
            if(prop.slot==5||prop.slot==6)return true;long now=OtherMotherLoop.now(p);if(now<n.getLong("BiteAfter"))return true;n.putLong("BiteAfter",now+30);boolean hadFood=!prop.food(0).isEmpty()||!prop.food(1).isEmpty()||!prop.food(2).isEmpty();bite(p,prop,p);DinnerActivities.ate(p,s,prop,hadFood);
        }return true;
    }
    static void bite(ServerPlayer p,ScenePropEntity prop,net.minecraft.world.entity.LivingEntity who){
        for(int i=2;i>=0;i--)if(!prop.food(i).isEmpty()){var mouth=prop.food(i).copyWithCount(1);var food=prop.food(i).copy();food.shrink(1);prop.food(i,food);who.swing(net.minecraft.world.InteractionHand.MAIN_HAND);who.level().playSound(null,who.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EAT,net.minecraft.sounds.SoundSource.NEUTRAL,.35F,1);
            if(who instanceof ParentEntity parent){finishBite(parent);parent.getPersistentData().put("DinnerOldHand",parent.getMainHandItem().save(new CompoundTag()));parent.getPersistentData().putLong("DinnerBiteEnd",OtherMotherLoop.now(p)+36);parent.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,mouth);parent.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);}
            if(who==p)p.getFoodData().eat(2,.3F);if(who.level() instanceof net.minecraft.server.level.ServerLevel level)level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,prop.food(i).isEmpty()?new ItemStack(Items.BREAD):prop.food(i)),who.getX(),who.getEyeY()-.2,who.getZ(),4,.1,.1,.1,.01);return;}
    }
    static void finishBite(ParentEntity parent){var n=parent.getPersistentData();if(!n.contains("DinnerOldHand"))return;parent.stopUsingItem();parent.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.of(n.getCompound("DinnerOldHand")));n.remove("DinnerOldHand");n.remove("DinnerBiteEnd");}
    static void drink(ServerPlayer p,ScenePropEntity glass,net.minecraft.world.entity.LivingEntity who){var item=glass.food(0).copyWithCount(1);glass.food(0,ItemStack.EMPTY);who.swing(net.minecraft.world.InteractionHand.MAIN_HAND);who.level().playSound(null,who.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_DRINK,net.minecraft.sounds.SoundSource.NEUTRAL,.3F,1);if(who==p)p.getFoodData().eat(1,.15F);else if(who instanceof ParentEntity parent){finishBite(parent);parent.getPersistentData().put("DinnerOldHand",parent.getMainHandItem().save(new CompoundTag()));parent.getPersistentData().putLong("DinnerBiteEnd",OtherMotherLoop.now(p)+36);parent.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item);parent.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);}}
    private static final Map<UUID,UUID> QUESTIONS=new HashMap<>();
    static void forget(UUID id){QUESTIONS.remove(id);DinnerActivities.forget(id);}
    static void suspend(ServerPlayer p,VisitData.Scene s){
        if(VisitEvents.type(s)!=VisitEvents.Type.FAMILY_DINNER||!s.loop.event.getBoolean("DinnerPending"))return;
        if(s.loop.phase==DINNER||s.loop.phase==DINNER_FINISH)s.loop.event.putString("DinnerResumePhase",s.loop.phase.name());
        reconnect(p,s);
    }
    /** A disconnected/closed question cannot retain an actionable nonce, but its unanswered offer survives. */
    static void reconnect(ServerPlayer p,VisitData.Scene s){
        forget(p.getUUID());
        if(VisitEvents.type(s)==VisitEvents.Type.FAMILY_DINNER&&!s.loop.event.getBoolean("Answered")&&s.loop.event.getInt("QuestionAfter")>0)s.loop.event.putBoolean("QuestionOffered",false);
    }
    static void clearQuestions(){QUESTIONS.clear();DinnerActivities.clear();}
    static void question(ServerPlayer p,VisitData.Scene s){
        var nonce=UUID.randomUUID();QUESTIONS.put(p.getUUID(),nonce);var n=new CompoundTag();
        n.putBoolean("VisitConversation",true);n.putUUID("Nonce",nonce);n.putString("Speaker","Other Father");
        int round=s.loop.event.getInt("QuestionRound");
        String text=round==1?"What have you enjoyed most about the house?":round==2?"Could you give me a hand with the table?":"So, what have you been getting up to today?";
        n.putString("Text",text);IncidentalDialogue.say(p,"Other Father",text);
        n.put("Options",SecretsState.strings(round==1?List.of("Your piano music.","Tell me about the garden.","Finding a little quiet time."):round==2?List.of("I'll pass the bread.","I'll pass the potatoes.","I'm still enjoying my dinner."):List.of("It's been wonderful.","I've been looking around.","I think I prefer home.")));
        SecretsNetwork.send(p,n);
    }
    public static boolean choice(ServerPlayer p,UUID nonce,String answer){if(DinnerActivities.choice(p,nonce,answer))return true;var key=QUESTIONS.get(p.getUUID());if(key==null||!key.equals(nonce))return false;if(!Set.of("0","1","2","close").contains(answer))return true;var s=OtherMotherLoop.active(p);if(s==null||s.loop.phase!=DINNER||s.loop.secrets.searching()||SecretDialogue.busy(p)||p.distanceToSqr(s.table.getCenter())>144||!(p.getVehicle() instanceof ScenePropEntity chair)||chair.slot!=0||!Objects.equals(chair.owner,p.getUUID()))return true;QUESTIONS.remove(p.getUUID());
        int round=s.loop.event.getInt("QuestionRound");
        if(round>0&&!s.loop.event.getBoolean("Answered")){
            s.loop.event.putBoolean("Answered",true);
            if(!answer.equals("close")){
                if(round==2&&(answer.equals("0")||answer.equals("1"))){var n=s.loop.event;int dish=answer.equals("0")?8:9;n.putInt("RequestDish",dish);n.putBoolean("Passed",false);var plate=find(p,s,ScenePropEntity.PLATE,dish);if(plate!=null){if(plate.food(0).isEmpty())plate.food(0,new ItemStack(dish==8?net.minecraft.world.item.Items.BREAD:net.minecraft.world.item.Items.BAKED_POTATO));plate.setGlowingTag(true);}}
                VisitAudio.caption(p,"Other Father",round==1?switch(answer){case "0"->"Music makes a house feel like home, doesn't it? I keep finding new melodies here.";case "1"->"The garden has a way of surprising me too. Even the flowers seem to be listening.";default->"Fair enough, kiddo. We all need a quiet moment sometimes.";}:answer.equals("2")?"Of course. Take your time and enjoy it.":"Thank you! Right-click the highlighted dish when you're ready to pass it.",140);
                if(!answer.equals("2"))OtherMotherLoop.relationship(p,s.loop.trust+.005F,s.loop.suspicion);
            }
        }else if(!s.loop.event.getBoolean("Answered")){s.loop.event.putBoolean("Answered",true);if(answer.equals("0"))OtherMotherLoop.relationship(p,s.loop.trust+VisitConfig.DINNER_FRIENDLY.get().floatValue(),s.loop.suspicion);else if(answer.equals("2"))OtherMotherLoop.relationship(p,s.loop.trust-VisitConfig.DINNER_HOME.get().floatValue(),s.loop.suspicion);}
        var n=new CompoundTag();n.putBoolean("VisitConversation",true);n.putBoolean("Close",true);SecretsNetwork.send(p,n);return true;}
    private FamilyDinner(){}
}
