package com.coraline.feast;

import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** Family table, one match and one optional rematch; no separate visit scheduler. */
public final class FamilyGameNight {
    public static OtherMotherDialogue.Pool pool(String category,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/family_game_night/"+category+"/"+SecretDialogue.stateId(state),2);}
    static OtherMotherDialogue.Pool intro(TabletopGames.Game game){return new OtherMotherDialogue.Pool("events/family_game_night/intro/"+TabletopGames.voice(game),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var all=new ArrayList<OtherMotherDialogue.Pool>();for(String category:List.of("invitation","ontime","late","search","finish"))for(var state:OtherMotherWelcome.State.values())all.add(pool(category,state));for(var game:TabletopGames.Game.values())all.add(intro(game));return all;}
    public static List<String> fatherVoices(){var all=new ArrayList<String>(List.of("father_game_night_greeting_1","father_game_night_greeting_2"));for(var game:TabletopGames.Game.values())all.add("father_game_night_"+TabletopGames.voice(game)+"_rules");for(var game:TabletopGames.Game.values())all.add("father_game_night_"+TabletopGames.voice(game)+"_reaction");all.addAll(List.of("father_game_night_finish_1","father_game_night_finish_2"));return all;}
    static TabletopGames.Game game(VisitData.Scene s){return TabletopGames.game(s.loop.event.getCompound("Game"));}
    static Vec3 center(VisitData.Scene s){return Vec3.atBottomCenterOf(s.table);}
    static ScenePropEntity prop(ServerPlayer p,VisitData.Scene s,int kind,int slot){return VisitEvents.props(p,s).stream().filter(e->e.kind()==kind&&e.slot==slot).findFirst().orElse(null);}
    static Vec3 chairPosition(VisitData.Scene s,int slot){return center(s).add(slot==2?-2.35:slot==3?2.35:0,0,slot==0?-2:slot==1?2:0);}
    static float facing(int slot){return slot==0?0:slot==1?180:slot==2?-90:90;}
    static CompoundTag companion(VisitData.Scene s){var n=s.loop.event;if(!n.contains("GameCompanion"))n.put("GameCompanion",new CompoundTag());return n.getCompound("GameCompanion");}
    static CharacterEntity wybie(ServerPlayer p,VisitData.Scene s){var lease=companion(s);var w=WybieOptionalVisits.actor(p,lease);return WybieOptionalVisits.owns(w,p,s,lease)?w:null;}
    /** Only an actually seated, serial-bound invited owner is exempt from house
     * surveillance. Leaving the table or an existing pursuit keeps normal rules. */
    public static boolean seated(ServerPlayer p,VisitData.Scene s){
        return s!=null&&p.getUUID().equals(s.owner)&&VisitEvents.type(s)==VisitEvents.Type.FAMILY_GAME_NIGHT
            &&s.loop.event.getBoolean("GameNightPending")&&!s.loop.secrets.searching()
            &&Set.of(GAME_NIGHT_GREETING,GAME_NIGHT_INSTRUCTIONS,GAME_NIGHT_ACTIVE,GAME_NIGHT_CHOICE,GAME_NIGHT_FINISH).contains(s.loop.phase)
            &&p.getVehicle() instanceof ScenePropEntity chair&&chair.level()==p.level()&&chair.kind()==ScenePropEntity.CHAIR&&chair.slot==0
            &&p.getUUID().equals(chair.owner)&&chair.getPersistentData().getInt("VisitEventSerial")==s.loop.event.getInt("Serial");
    }
    /** Lock the actual optional roster before invitation/play. Never fill an unloaded saved seat with a clone. */
    static void ensureRoster(ServerPlayer p,VisitData.Scene s){
        VisitProgression.refresh(p,s);
        var n=s.loop.event;var lease=companion(s);boolean legacy=n.hasUUID("GameWybie")&&!lease.hasUUID("Actor");
        if(WybieOptionalVisits.allowed(s)&&(!n.getBoolean("GameHasWybie")||!lease.hasUUID("Actor"))&&(!n.contains("Game")||n.getCompound("Game").getBoolean("Done")))n.putBoolean("GameRosterSet",false);
        if(!n.getBoolean("GameRosterSet")){
            CharacterEntity w=null;
            if(WybieOptionalVisits.allowed(s)){
                if(legacy)w=WybieOptionalVisits.adoptGameNight(p,s,lease);
                else if((!n.contains("Game")||n.getCompound("Game").getBoolean("Done"))&&OtherMotherLoop.now(p)>=n.getLong("GameWybieRetry")){
                    n.putLong("GameWybieRetry",OtherMotherLoop.now(p)+40);var realm=SecretWorld.realm(p);
                    if(realm!=null&&realm.porch!=null)for(int distance:new int[]{0,6,12,18}){for(int side:new int[]{0,-6,6}){w=WybieOptionalVisits.acquire(p,s,lease,realm.porch.offset(side,0,-distance));if(w!=null)break;}if(w!=null)break;}
                }
            }
            // Existing four-player saves wait for their exact actor; a new night can honestly have three seats.
            n.putBoolean("GameHasWybie",n.contains("Game")&&!n.getCompound("Game").getBoolean("Done")?TabletopGames.participantCount(n.getCompound("Game"))==4:legacy||WybieOptionalVisits.allowed(s));
            n.putBoolean("GameRosterSet",true);
        }
        if(!n.getBoolean("GameHasWybie")||!WybieOptionalVisits.allowed(s))return;
        var w=wybie(p,s);if(w==null&&legacy)w=WybieOptionalVisits.adoptGameNight(p,s,lease);
        if(w==null&&!lease.hasUUID("Actor")&&!legacy&&OtherMotherLoop.now(p)>=n.getLong("GameWybieRetry")){
            n.putLong("GameWybieRetry",OtherMotherLoop.now(p)+40);var r=SecretWorld.realm(p);if(r!=null&&r.porch!=null){p.serverLevel().getChunkAt(r.porch);w=WybieOptionalVisits.acquire(p,s,lease,r.porch);}
        }
        if(w==null&&lease.contains("ActorPosition")&&p.tickCount%40==0)p.serverLevel().getChunkAt(net.minecraft.core.BlockPos.of(lease.getLong("ActorPosition")));
        if(w!=null){n.putUUID("GameWybie",w.getUUID());w.getPersistentData().putUUID("GameNightOwner",p.getUUID());w.getPersistentData().putString("WybieCompanionMode","GAME_NIGHT");}
    }
    static void ensure(ServerPlayer p,VisitData.Scene s){
        if(!p.getUUID().equals(s.owner))return;ensureRoster(p,s);int count=s.loop.event.getBoolean("GameHasWybie")?4:3;
        var base=center(s);if(prop(p,s,ScenePropEntity.TABLE,0)==null)VisitEvents.prop(p,s,ScenePropEntity.TABLE,base,0);
        for(int i=0;i<count;i++)if(prop(p,s,ScenePropEntity.CHAIR,i)==null){var chair=VisitEvents.prop(p,s,ScenePropEntity.CHAIR,chairPosition(s,i),i);chair.setYRot(facing(i));}
        if(prop(p,s,ScenePropEntity.GAME_BOARD,0)==null)VisitEvents.prop(p,s,ScenePropEntity.GAME_BOARD,base.add(0,.93,0),0);
        if(prop(p,s,ScenePropEntity.GAME_SHELF,0)==null)placeShelf(p,s);
        for(int i=0;i<count;i++)if(prop(p,s,ScenePropEntity.GLASS,i)==null)VisitEvents.prop(p,s,ScenePropEntity.GLASS,base.add(i<2?1.35:-1.35,.91,i%2==0?-.9:.9),i).food(0,new ItemStack(Items.POTION));
    }
    static void placeShelf(ServerPlayer p,VisitData.Scene s){
        long now=OtherMotherLoop.now(p);if(now<s.loop.event.getLong("GameShelfAfter"))return;s.loop.event.putLong("GameShelfAfter",now+400);
        var level=p.serverLevel();for(int radius=4;radius<=10;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;var at=s.table.offset(x,0,z);if(!OutingSites.feet(level,at))continue;
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var wall=at.relative(direction);boolean solid=true;for(int y=0;y<3;y++){var block=level.getBlockState(wall.above(y));if(!block.isSolidRender(level,wall.above(y))||block.hasBlockEntity())solid=false;}if(!solid)continue;
                boolean nearDoor=false;for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(level.getBlockState(at.offset(dx,0,dz)).getBlock() instanceof net.minecraft.world.level.block.DoorBlock)nearDoor=true;if(nearDoor)continue;
                var position=Vec3.atBottomCenterOf(at).add(direction.getStepX()*.18,0,direction.getStepZ()*.18);double hx=direction.getAxis()==net.minecraft.core.Direction.Axis.X?.35:.95,hz=direction.getAxis()==net.minecraft.core.Direction.Axis.Z?.35:.95;
                if(!level.noCollision(new AABB(position.x-hx,position.y,position.z-hz,position.x+hx,position.y+2.2,position.z+hz)))continue;
                var shelf=VisitEvents.prop(p,s,ScenePropEntity.GAME_SHELF,position,0);shelf.setYRot(direction.toYRot());return;
            }
        }
    }
    public static void beginWindow(ServerPlayer p,VisitData.Scene s){var n=s.loop.event;n.putBoolean("GameNightPending",true);n.putInt("ArrivalRemaining",6000);n.putBoolean("GraceRolled",false);n.putBoolean("Searching",false);n.putInt("Matches",0);n.putInt("CompletedMatches",0);n.putBoolean("GameCompleted",false);OtherMotherLoop.phase(p,s,GAME_NIGHT_WAIT);ensure(p,s);}
    static void prepare(ServerPlayer p,VisitData.Scene s){s.loop.event.putBoolean("GameNightPending",true);ensure(p,s);}
    static void parentControl(ParentEntity parent,ServerPlayer p,VisitData.Scene s){FamilyDinner.finishBiteIfDone(parent,p);var chair=prop(p,s,ScenePropEntity.CHAIR,parent.isMom()?1:2);if(chair!=null){SupperSeating.approach(parent,chair);parent.lookAtStoryPlayer(p);}}
    /** The controller tests tickCount % 10. Half-rate goals can otherwise miss
     * every one of those ticks when their scheduler parity is odd. */
    public static final class ControlGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final CharacterEntity actor;
        public ControlGoal(CharacterEntity actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return actor.getPersistentData().hasUUID("GameNightOwner");}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){controlWybie(actor);}
    }
    public static boolean controlWybie(CharacterEntity w){
        var tag=w.getPersistentData();if(!tag.hasUUID("GameNightOwner"))return false;var owner=tag.getUUID("GameNightOwner");var s=VisitData.get(w.getServer()).scenes.get(owner);var p=w.getServer().getPlayerList().getPlayer(owner);
        if(s==null||VisitEvents.type(s)!=VisitEvents.Type.FAMILY_GAME_NIGHT||!s.loop.event.getBoolean("GameNightPending")||!WybieOptionalVisits.allowed(s)){
            if(!tag.hasUUID("WybieCompanionToken")&&tag.hasUUID("RealmDoor"))restoreLegacy(w,owner,tag.getUUID("RealmDoor"));else{tag.remove("GameNightOwner");w.stopRiding();w.getNavigation().stop();}return false;
        }
        if(p==null||!p.isAlive()||p.level()!=w.level()){w.stopRiding();w.getNavigation().stop();return true;}
        var lease=companion(s);if(!WybieOptionalVisits.owns(w,p,s,lease)){w.stopRiding();w.getNavigation().stop();return true;}
        WybieOptionalVisits.prepareNavigation(w);
        lease.putLong("ActorPosition",w.blockPosition().asLong());
        if(!w.isPassenger())WybieOptionalVisits.recover(p,w,lease);
        var chair=prop(p,s,ScenePropEntity.CHAIR,3);if(chair==null)return true;
        if(w.getVehicle()!=chair){
            if(w.distanceToSqr(chair)<4){var at=chair.position().add(0,.1,0);if(w.level().noCollision(w,w.getDimensions(w.getPose()).makeBoundingBox(at))&&!chair.isVehicle())w.startRiding(chair,true);}
            else if(w.tickCount%10==0){if(w.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}w.getNavigation().moveTo(chair,1.5);}
        }else{w.getNavigation().stop();w.setYRot(chair.getYRot());w.setYBodyRot(chair.getYRot());w.getLookControl().setLookAt(p,25,20);}return true;
    }
    static boolean wybieReady(ServerPlayer p,VisitData.Scene s){
        var n=s.loop.event;if(!n.getBoolean("GameHasWybie"))return true;var w=wybie(p,s);var chair=prop(p,s,ScenePropEntity.CHAIR,3);
        if(WybieOptionalVisits.allowed(s)&&w!=null&&w.level()==p.level()&&chair!=null&&w.getVehicle()==chair){n.putInt("GameGuestWait",0);return true;}
        int waiting=n.getInt("GameGuestWait")+1;n.putInt("GameGuestWait",waiting);if(!WybieOptionalVisits.allowed(s)||waiting>=1200)complete(p,s,false);return false;
    }
    static boolean tick(ServerPlayer p,VisitData.Scene s){if(!p.getUUID().equals(s.owner)||!p.isAlive())return true;var n=s.loop.event;int reactionRemaining=n.getInt("GameReactionRemaining");if(reactionRemaining>0)n.putInt("GameReactionRemaining",reactionRemaining-1);if(reactionRemaining>0&&(s.loop.phase==GAME_NIGHT_INSTRUCTIONS||s.loop.phase==GAME_NIGHT_CHOICE||s.loop.phase==GAME_NIGHT_FINISH)){if(s.loop.phase==GAME_NIGHT_CHOICE&&p.tickCount%5==0)send(p,s,false);return true;}if(p.tickCount%20==0)ensure(p,s);
        if(s.loop.phase==GAME_NIGHT_WAIT){if(n.getBoolean("Resuming")){int wait=n.getInt("ResumeWait")+1;n.putInt("ResumeWait",wait);if(wait>12000)complete(p,s,false);return true;}int ticks=n.getInt("ArrivalRemaining");if(ticks>0){n.putInt("ArrivalRemaining",ticks-1);return true;}if(!n.getBoolean("GraceRolled")){n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",FamilyDinner.graceTicks(s.loop.suspicion,p.getRandom().nextDouble()));return true;}int grace=n.getInt("GraceRemaining");if(grace>0){n.putInt("GraceRemaining",grace-1);return true;}if(!n.getBoolean("Searching")&&VisitEvents.speak(p,s,pool("search",SecretDialogue.state(p)))){n.putBoolean("Searching",true);s.loop.secrets.search=SecretsState.Search.SEARCHING;s.loop.secrets.target=p.blockPosition();OtherMotherLoop.changed(p);}return true;}
        if(s.loop.phase==GAME_NIGHT_GREETING){if(!n.getBoolean("MotherGreetingDone")){if(VisitEvents.speak(p,s,pool(n.getBoolean("Late")?"late":"ontime",OtherMotherWelcome.State.fromId(n.getString("ArrivalState")))))n.putBoolean("MotherGreetingDone",true);return true;}if(!greetingFinished(p,s))return true;var m=MotherSearch.mother(p);var d=VisitEvents.father(p,s);if(m==null||d==null||m.getVehicle()!=prop(p,s,ScenePropEntity.CHAIR,1)||d.getVehicle()!=prop(p,s,ScenePropEntity.CHAIR,2)||!wybieReady(p,s))return true;selectGame(p,s,null);return true;}
        if(s.loop.phase==GAME_NIGHT_INSTRUCTIONS){if(!n.getBoolean("GameIntroDone")){if(VisitEvents.speak(p,s,intro(game(s))))n.putBoolean("GameIntroDone",true);return true;}if(!FamilyDinner.fatherSpeech(p,s,"father_game_night_"+TabletopGames.voice(game(s))+"_rules"))return true;OtherMotherLoop.phase(p,s,GAME_NIGHT_ACTIVE);send(p,s,true);return true;}
        if(s.loop.phase==GAME_NIGHT_ACTIVE){if(!wybieReady(p,s))return true;var rules=n.getCompound("Game");if(!(p.getVehicle() instanceof ScenePropEntity seat&&seat.slot==0&&p.getUUID().equals(seat.owner))){int away=n.getInt("GameAway")+1;n.putInt("GameAway",away);if(away>1200)complete(p,s,false);return true;}n.putInt("GameAway",0);if(game(s)==TabletopGames.Game.SILENT_CHARADES)act(p,s,rules);int oldActions=rules.getInt("ActionCount");TabletopGames.tick(rules);if(rules.getInt("ActionCount")!=oldActions)reaction(p,s,rules);if(p.tickCount%5==0){var board=prop(p,s,ScenePropEntity.GAME_BOARD,0);if(board!=null)board.cakeData(TabletopGames.spectatorVisible(rules));send(p,s,false);}if(rules.getBoolean("Done")){restoreActingProps(p,s);n.putInt("Matches",n.getInt("Matches")+1);if(TabletopGames.completedNormally(rules))n.putInt("CompletedMatches",n.getInt("CompletedMatches")+1);n.putInt("ChoiceTicks",0);n.putUUID("GameChoiceNonce",UUID.randomUUID());OtherMotherLoop.phase(p,s,GAME_NIGHT_CHOICE);result(p,s);send(p,s,true);}return true;}
        if(s.loop.phase==GAME_NIGHT_CHOICE){int ticks=n.getInt("ChoiceTicks")+1;n.putInt("ChoiceTicks",ticks);if(p.tickCount%5==0)send(p,s,false);if(ticks>1200||n.getInt("Matches")>=2&&ticks>=100)complete(p,s);return true;}
        if(s.loop.phase==GAME_NIGHT_FINISH){
            if(n.getBoolean("GameWrapupDone")){if(p.tickCount%20==0)VisitEvents.next(p,s,false);return true;}
            if(!n.getBoolean("FatherFinished")){if(FamilyDinner.fatherSpeech(p,s,n.getString("GameEnding")))n.putBoolean("FatherFinished",true);return true;}
            if(!VisitEvents.speak(p,s,pool("finish",SecretDialogue.state(p))))return true;
            if(n.getBoolean("GameCompleted")&&!n.getBoolean("CompletionAwarded")){n.putBoolean("CompletionAwarded",true);OtherMotherLoop.relationship(p,s.loop.trust+VisitConfig.GAME_COMPLETE.get().floatValue(),s.loop.suspicion);if(!n.getBoolean("GamePrize")){n.putBoolean("GamePrize",true);FamilyOuting.give(p,new ItemStack(Items.COOKIE,2));}}
            n.putBoolean("GameWrapupDone",true);VisitEvents.next(p,s,false);return true;
        }return true;
    }
    static boolean greetingFinished(ServerPlayer p,VisitData.Scene s){
        var n=s.loop.event;if(n.getBoolean("GameFatherGreetingDone"))return true;
        if(!FamilyDinner.fatherSpeech(p,s,n.getString("Greeting")))return false;
        n.putBoolean("GameFatherGreetingDone",true);OtherMotherLoop.changed(p);return true;
    }
    static void selectGame(ServerPlayer p,VisitData.Scene s,TabletopGames.Game forced){close(p);restoreActingProps(p,s);var n=s.loop.event;n.remove("PullStarted");var previous=n.contains("Game")?game(s):null;var choices=new ArrayList<>(List.of(TabletopGames.Game.values()));if(previous!=null)choices.remove(previous);var next=forced==null?choices.get(p.getRandom().nextInt(choices.size())):forced;ensureRoster(p,s);boolean withWybie=n.getBoolean("GameHasWybie")&&WybieOptionalVisits.allowed(s)&&wybie(p,s)!=null;if(n.getBoolean("GameHasWybie")&&!withWybie){complete(p,s,false);return;}n.put("Game",withWybie?TabletopGames.start(next,p.getRandom().nextLong()):TabletopGames.start(next,p.getRandom().nextLong(),List.of(new TabletopGames.Participant("owner","You",0),new TabletopGames.Participant("mother","Other Mother",1),new TabletopGames.Participant("father","Other Father",1))));n.putBoolean("GameIntroDone",false);n.putBoolean("GameReactionSpoken",false);n.putUUID("GameMoveNonce",UUID.randomUUID());n.remove("GameChoiceNonce");OtherMotherLoop.phase(p,s,GAME_NIGHT_INSTRUCTIONS);var board=prop(p,s,ScenePropEntity.GAME_BOARD,0);if(board!=null)board.cakeData(TabletopGames.spectatorVisible(n.getCompound("Game")));}
    static void result(ServerPlayer p,VisitData.Scene s){var rules=s.loop.event.getCompound("Game");VisitAudio.caption(p,"Game Night",TabletopGames.resultText(rules),100);var w=wybie(p,s);if(w!=null&&TabletopGames.participantCount(rules)>3)w.gameGesture(TabletopGames.participantWon(rules,3)?7:8,100);}
    static void reaction(ServerPlayer p,VisitData.Scene s,CompoundTag rules){int who=rules.getInt("LastActor");if(!rules.contains("LastActor"))who=Math.floorMod(rules.getInt("Turn")+TabletopGames.participantCount(rules)-1,TabletopGames.participantCount(rules));var actor=who==1?MotherSearch.mother(p):who==2?VisitEvents.father(p,s):who==3?wybie(p,s):p;if(actor!=null)actor.swing(net.minecraft.world.InteractionHand.MAIN_HAND);var w=wybie(p,s);if(w!=null)w.gameGesture(who==3?6:7,45);var n=s.loop.event;if(!n.getBoolean("GameReactionSpoken")&&rules.getInt("ActionCount")>=4){n.putBoolean("GameReactionSpoken",true);String sound="father_game_night_"+TabletopGames.voice(game(s))+"_reaction";VisitAudio.play(p,sound,false);n.putInt("GameReactionRemaining",VisitAudio.length(sound)+4);}}
    static void equipGameProp(LivingEntity actor,ItemStack stack){var data=actor.getPersistentData();if(!data.contains("GameOldHand"))data.put("GameOldHand",actor.getMainHandItem().save(new CompoundTag()));actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);}
    static void restoreActingProps(ServerPlayer p,VisitData.Scene s){
        for(var parent:new ParentEntity[]{MotherSearch.mother(p),VisitEvents.father(p,s)})if(parent!=null){var data=parent.getPersistentData();String saved=data.contains("GameOldHand")?"GameOldHand":VisitEvents.type(s)==VisitEvents.Type.FAMILY_GAME_NIGHT&&data.contains("OutingOldHand")?"OutingOldHand":"";if(!saved.isEmpty()){parent.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.of(data.getCompound(saved)));data.remove(saved);}if(VisitEvents.type(s)==VisitEvents.Type.FAMILY_GAME_NIGHT)parent.familyGesture(0,0);}
        var w=wybie(p,s);if(w!=null){var data=w.getPersistentData();if(data.contains("GameOldHand")){w.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.of(data.getCompound("GameOldHand")));data.remove("GameOldHand");}w.gameGesture(0,0);}
        s.loop.event.remove("GameActingActor");
    }
    static void act(ServerPlayer p,VisitData.Scene s,CompoundTag rules){
        int who=rules.getInt("Actor"),topic=rules.getInt("Topic");if(who<0||who>=TabletopGames.participantCount(rules)||topic<0||topic>=6)return;var n=s.loop.event;
        if(!n.contains("GameActingActor")||n.getInt("GameActingActor")!=who){restoreActingProps(p,s);n.putInt("GameActingActor",who);}
        if(who==0)return;
        int gesture=topic+1;Item[] props={Items.FEATHER,Items.FISHING_ROD,Items.CLOCK,Items.BREAD,Items.STRING,Items.STICK};
        if(who==3){var w=wybie(p,s);if(w!=null){w.gameGesture(gesture,40);equipGameProp(w,new ItemStack(props[topic]));}}
        else{var parent=who==1?MotherSearch.mother(p):VisitEvents.father(p,s);if(parent!=null){parent.familyGesture(gesture,40);equipGameProp(parent,new ItemStack(props[topic]));}}
    }
    static boolean completionEligible(VisitData.Scene s){var n=s.loop.event;return n.getInt("CompletedMatches")>0&&TabletopGames.completedNormally(n.getCompound("Game"));}
    static void complete(ServerPlayer p,VisitData.Scene s){complete(p,s,completionEligible(s));}
    static void complete(ServerPlayer p,VisitData.Scene s,boolean successful){restoreActingProps(p,s);var n=s.loop.event;n.putBoolean("GameCompleted",successful);n.putBoolean("FatherFinished",false);n.putString("GameEnding",p.getRandom().nextBoolean()?"father_game_night_finish_1":"father_game_night_finish_2");OtherMotherLoop.phase(p,s,GAME_NIGHT_FINISH);close(p);}
    public static boolean arrive(ServerPlayer p,VisitData.Scene s,ScenePropEntity chair){var n=s.loop.event;if(VisitEvents.type(s)!=VisitEvents.Type.FAMILY_GAME_NIGHT||s.loop.phase!=GAME_NIGHT_WAIT||!p.getUUID().equals(s.owner)||!p.getUUID().equals(chair.owner)||chair.slot!=0||chair.kind()!=ScenePropEntity.CHAIR||!chair.getPersistentData().contains("VisitEventSerial")||chair.getPersistentData().getInt("VisitEventSerial")!=n.getInt("Serial")||chair.distanceToSqr(p)>9||s.loop.secrets.searching()||n.getBoolean("Searching")||SecretDialogue.busy(p)||chair.isVehicle()&&p.getVehicle()!=chair||!p.startRiding(chair,true))return false;boolean late=n.getInt("ArrivalRemaining")<=0||n.getBoolean("Resuming");n.putBoolean("Late",late);n.putString("ArrivalState",SecretDialogue.state(p).id());n.putString("Greeting",p.getRandom().nextBoolean()?"father_game_night_greeting_1":"father_game_night_greeting_2");n.putBoolean("MotherGreetingDone",false);n.putInt("ArrivalRemaining",0);n.putInt("GraceRemaining",0);if(!n.getBoolean("ArrivalAwarded")&&!n.getBoolean("Resuming"))OtherMotherLoop.relationship(p,s.loop.trust+(late?VisitConfig.GAME_LATE_TRUST:VisitConfig.GAME_ON_TIME).get().floatValue(),s.loop.suspicion+(late?VisitConfig.GAME_LATE_SUSPICION.get().floatValue():0));n.putBoolean("ArrivalAwarded",true);if(n.getBoolean("GameInterrupted")&&n.contains("Game")){n.putBoolean("GameInterrupted",false);OtherMotherLoopState.Phase restored=GAME_NIGHT_ACTIVE;try{var saved=OtherMotherLoopState.Phase.valueOf(n.getString("GameResumePhase"));if(saved==GAME_NIGHT_ACTIVE||saved==GAME_NIGHT_INSTRUCTIONS||saved==GAME_NIGHT_CHOICE||saved==GAME_NIGHT_FINISH)restored=saved;}catch(IllegalArgumentException ignored){}n.remove("GameResumePhase");OtherMotherLoop.phase(p,s,restored);if(restored==GAME_NIGHT_ACTIVE||restored==GAME_NIGHT_CHOICE)send(p,s,true);}else OtherMotherLoop.phase(p,s,GAME_NIGHT_GREETING);return true;}
    public static boolean interact(ServerPlayer p,ScenePropEntity e){var s=OtherMotherLoop.active(p);if(s==null||VisitEvents.type(s)!=VisitEvents.Type.FAMILY_GAME_NIGHT||!e.getPersistentData().contains("VisitEventSerial"))return false;if(!p.getUUID().equals(e.owner)||e.getPersistentData().getInt("VisitEventSerial")!=s.loop.event.getInt("Serial"))return true;if(e.kind()==ScenePropEntity.CHAIR&&e.slot==0){if(!arrive(p,s,e)&&Set.of(GAME_NIGHT_GREETING,GAME_NIGHT_INSTRUCTIONS,GAME_NIGHT_ACTIVE,GAME_NIGHT_CHOICE,GAME_NIGHT_FINISH).contains(s.loop.phase)&&!s.loop.secrets.searching()&&!SecretDialogue.busy(p)&&e.distanceToSqr(p)<9&&!e.isVehicle())p.startRiding(e,true);}else if(e.kind()==ScenePropEntity.GAME_BOARD&&e.distanceToSqr(p)<36&&s.loop.phase==GAME_NIGHT_ACTIVE)send(p,s,true);return true;}
    static void send(ServerPlayer p,VisitData.Scene s,boolean open){var n=s.loop.event;if(!n.contains("Game"))return;var packet=new CompoundTag();packet.putBoolean("FamilyGame",true);packet.putBoolean("Open",open);packet.putString("Phase",s.loop.phase.name());packet.put("Game",TabletopGames.visible(n.getCompound("Game")));packet.putInt("Matches",n.getInt("Matches"));packet.putInt("GameReactionRemaining",n.getInt("GameReactionRemaining"));var board=prop(p,s,ScenePropEntity.GAME_BOARD,0);if(board!=null)packet.putUUID("Board",board.getUUID());packet.putUUID("Nonce",s.loop.phase==GAME_NIGHT_CHOICE?n.getUUID("GameChoiceNonce"):n.getUUID("GameMoveNonce"));SecretsNetwork.send(p,packet);}
    public static boolean choice(ServerPlayer p,UUID nonce,String action){var s=OtherMotherLoop.active(p);if(s==null||VisitEvents.type(s)!=VisitEvents.Type.FAMILY_GAME_NIGHT)return false;var n=s.loop.event;if(!p.getUUID().equals(s.owner)||s.loop.secrets.searching()||SecretDialogue.busy(p)||!(p.getVehicle() instanceof ScenePropEntity seat)||seat.kind()!=ScenePropEntity.CHAIR||seat.slot!=0||!p.getUUID().equals(seat.owner)||!seat.getPersistentData().contains("VisitEventSerial")||seat.getPersistentData().getInt("VisitEventSerial")!=n.getInt("Serial")||p.distanceToSqr(center(s))>36)return true;if(n.getInt("GameReactionRemaining")>0&&s.loop.phase==GAME_NIGHT_CHOICE)return true;if(s.loop.phase==GAME_NIGHT_CHOICE&&n.hasUUID("GameChoiceNonce")&&n.getUUID("GameChoiceNonce").equals(nonce)){if(!action.equals("another")&&!action.equals("finish"))return true;if(action.equals("another")&&n.getInt("Matches")>=2)return true;n.remove("GameChoiceNonce");if(action.equals("another"))selectGame(p,s,null);else complete(p,s);OtherMotherLoop.changed(p);return true;}if(s.loop.phase!=GAME_NIGHT_ACTIVE||!n.hasUUID("GameMoveNonce")||!n.getUUID("GameMoveNonce").equals(nonce))return true;var rules=n.getCompound("Game");if(game(s)!=TabletopGames.Game.SILENT_CHARADES&&rules.getInt("Turn")!=0)return true;if(game(s)==TabletopGames.Game.SILENT_CHARADES&&rules.getInt("TurnTicks")<60)return true;
        if(action.startsWith("pull_begin:")){try{var parts=action.split(":");int row=Integer.parseInt(parts[1]),piece=Integer.parseInt(parts[2]);if(!Double.isFinite(TabletopGames.towerCost(rules.getIntArray("Tower"),row,piece,1)))return true;n.putInt("PullRow",row);n.putInt("PullPiece",piece);n.putLong("PullStarted",OtherMotherLoop.now(p));}catch(RuntimeException ignored){}return true;}
        if(action.equals("pull_release")&&n.contains("PullStarted")){long ticks=OtherMotherLoop.now(p)-n.getLong("PullStarted");action="pull:"+n.getInt("PullRow")+":"+n.getInt("PullPiece")+":"+Math.max(0,1-Math.abs(ticks-40)/40.);n.remove("PullStarted");}else if(action.startsWith("pull:"))return true;
        if(TabletopGames.move(rules,action)){n.putUUID("GameMoveNonce",UUID.randomUUID());reaction(p,s,rules);send(p,s,false);OtherMotherLoop.changed(p);}return true;}
    public static boolean resume(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s==null||VisitEvents.type(s)!=VisitEvents.Type.FAMILY_GAME_NIGHT||!s.loop.event.getBoolean("GameNightPending"))return false;var n=s.loop.event;n.putBoolean("Resuming",true);n.putBoolean("Searching",false);n.putInt("ResumeWait",0);n.putInt("ArrivalRemaining",0);for(String k:List.of("Voice","Sound","IntroElapsed","FatherVoice","FatherVoiceElapsed"))n.remove(k);OtherMotherLoop.phase(p,s,GAME_NIGHT_WAIT);ensure(p,s);return true;}
    static void suspend(ServerPlayer p,VisitData.Scene s){if(VisitEvents.type(s)==VisitEvents.Type.FAMILY_GAME_NIGHT&&s.loop.event.getBoolean("GameNightPending")){if(s.loop.phase==GAME_NIGHT_ACTIVE||s.loop.phase==GAME_NIGHT_INSTRUCTIONS||s.loop.phase==GAME_NIGHT_CHOICE||s.loop.phase==GAME_NIGHT_FINISH){s.loop.event.putBoolean("GameInterrupted",true);s.loop.event.putString("GameResumePhase",s.loop.phase.name());}restoreActingProps(p,s);var w=wybie(p,s);if(w!=null){w.stopRiding();w.getNavigation().stop();}s.loop.event.remove("PullStarted");}close(p);}
    static void close(ServerPlayer p){var n=new CompoundTag();n.putBoolean("FamilyGame",true);n.putBoolean("Close",true);SecretsNetwork.send(p,n);}
    private static void restoreLegacy(CharacterEntity old,UUID owner,UUID door){
        var tag=old.getPersistentData();if(!old.characterId().equals("other_wybie")||!tag.hasUUID("GameNightOwner")||!owner.equals(tag.getUUID("GameNightOwner"))||!tag.hasUUID("RealmDoor")||!door.equals(tag.getUUID("RealmDoor"))||tag.hasUUID("WybieCompanionToken"))return;
        old.stopRiding();old.getNavigation().stop();old.gameGesture(0,0);if(tag.contains("GameOldHand")){old.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.of(tag.getCompound("GameOldHand")));tag.remove("GameOldHand");}if(tag.contains("GameOldInvulnerable"))old.setInvulnerable(tag.getBoolean("GameOldInvulnerable"));tag.remove("GameOldInvulnerable");tag.remove("GameNightOwner");if(tag.getBoolean("GameNightCreated"))old.discard();tag.remove("GameNightCreated");
    }
    static void cleanup(ServerPlayer p,VisitData.Scene s){
        s.loop.event.remove("GameFatherGreetingDone");
        s.loop.event.remove("GameWrapupDone");s.loop.event.remove("GameWybieRetry");
        close(p);restoreActingProps(p,s);var lease=companion(s);var w=WybieOptionalVisits.actor(p,lease);if(w!=null){w.getPersistentData().remove("GameNightOwner");w.gameGesture(0,0);}WybieOptionalVisits.release(p,lease);
        // A legacy save may not yet have been adopted. Only its exact owner/realm-bound actor is ours to release.
        if(s.loop.event.hasUUID("GameWybie"))for(var level:p.server.getAllLevels())if(level.getEntity(s.loop.event.getUUID("GameWybie")) instanceof CharacterEntity old)restoreLegacy(old,p.getUUID(),s.door);
        for(String key:List.of("GameNightPending","Game","GameWybie","GameCompanion","GameRosterSet","GameHasWybie","GameGuestWait","GameMoveNonce","GameChoiceNonce","GameIntroDone","GameReactionSpoken","GameReactionRemaining","GameAway","Matches","ChoiceTicks","GameEnding","GamePrize","GameInterrupted","GameResumePhase","GameActingActor","GameCompleted","CompletedMatches","GameShelfAfter","PullStarted","PullRow","PullPiece"))s.loop.event.remove(key);
    }
    public static void debug(ServerPlayer p,String input){if(!p.hasPermissions(2)||!OtherWorld.isOther(p.level()))return;var s=OtherMotherLoop.active(p);if(s==null)return;String[] parts=input.split(",",2);String arg=parts.length>1?parts[1]:"";if(parts[0].equals("force")){VisitEvents.start(p,s,VisitEvents.Type.FAMILY_GAME_NIGHT);return;}if(VisitEvents.type(s)!=VisitEvents.Type.FAMILY_GAME_NIGHT)return;var n=s.loop.event;try{switch(parts[0]){case "table"->p.teleportTo(p.serverLevel(),s.table.getX()+.5,s.table.getY(),s.table.getZ()-3,p.getYRot(),0);case "window"->beginWindow(p,s);case "time"->n.putInt("ArrivalRemaining",Math.max(0,Math.min(6000,Integer.parseInt(arg))));case "late"->{n.putInt("ArrivalRemaining",0);n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",1200);}case "grace"->{n.putInt("ArrivalRemaining",0);n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",0);}case "resume"->resume(p);case "game"->selectGame(p,s,TabletopGames.Game.valueOf(arg));case "active"->{if(!n.contains("Game"))selectGame(p,s,null);OtherMotherLoop.phase(p,s,GAME_NIGHT_ACTIVE);send(p,s,true);}case "npc"->TabletopGames.move(n.getCompound("Game"),TabletopGames.ai(n.getCompound("Game")));case "win","lose"->{n.getCompound("Game").putBoolean("Done",true);n.getCompound("Game").putInt("Winner",parts[0].equals("win")?0:1);}case "reset"->selectGame(p,s,game(s));case "rematch"->{if(n.getInt("Matches")<2)selectGame(p,s,null);}case "complete"->complete(p,s);case "state"->p.sendSystemMessage(Component.literal(n.toString()));case "voice"->{int index=Integer.parseInt(arg);if(index>=0&&index<62)VisitAudio.play(p,pools().get(index/2).sound(index%2),false);else if(index>=62&&index<78)VisitAudio.play(p,fatherVoices().get(index-62),false);}default->{}}}catch(RuntimeException ignored){}OtherMotherLoop.changed(p);}
    private FamilyGameNight(){}
}
