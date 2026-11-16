package com.coraline.feast;

import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** One bounded, owner-authoritative guest match using the shared tabletop rules. */
public final class PartyMatches {
    private static final int TABLE_SLOT=2000,BOARD_SLOT=2001,CHAIR_SLOT=2002;
    private static final int SEAT_GRACE=600,AWAY_GRACE=100,RESULT_GRACE=100,MAX_MATCH_TICKS=4800;
    private static final String HAND="PartyMatchOldHand",HAND_OWNER="PartyMatchHandOwner";
    private static CompoundTag match(VisitData.Scene s){return HouseParty.data(s).getCompound("Match");}
    public static boolean active(VisitData.Scene s){return HouseParty.data(s).contains("Match",Tag.TAG_COMPOUND)&&match(s).getBoolean("Active");}
    private static boolean owned(ServerPlayer p,VisitData.Scene s){return s!=null&&p.getUUID().equals(s.owner)&&HouseParty.active(p)==s;}
    private static CompoundTag guest(VisitData.Scene s,UUID id){return HousePartyLedger.guest(HouseParty.data(s),id);}
    private static boolean available(VisitData.Scene s,CompoundTag g){
        String state=g.getString("State");var conversation=HouseParty.data(s).getCompound("Conversation");
        return !g.getBoolean("ReportPending")&&(state.equals("WANDERING")||state.equals("CONVERSING")
                ||state.equals("TALKING_TO_PLAYER")&&conversation.hasUUID("Guest")&&conversation.getUUID("Guest").equals(g.getUUID("Id")));
    }
    private static boolean actual(ServerPlayer p,VisitData.Scene s,CompoundTag g,Mob actor){
        if(actor==null||!actor.isAlive()||actor.level()!=p.level()||!g.hasUUID("Id")||!g.hasUUID("Entity"))return false;
        var tag=actor.getPersistentData();
        return actor.getUUID().equals(g.getUUID("Entity"))&&tag.hasUUID("PartyOwner")&&p.getUUID().equals(tag.getUUID("PartyOwner"))
                &&tag.hasUUID("PartyGuest")&&g.getUUID("Id").equals(tag.getUUID("PartyGuest"))
                &&tag.getInt("PartySerial")==s.loop.event.getInt("Serial");
    }
    /** Returns false and refunds a newly accepted unused escrow if no safe match can be created. */
    public static boolean start(ServerPlayer p,VisitData.Scene s,List<CompoundTag> requested,TabletopGames.Game game,UUID escrowToken){
        if(!owned(p,s)||active(s)||HouseParty.data(s).getBoolean("FeastDue")||HouseParty.data(s).getBoolean("Suspended")
                ||s.loop.phase!=OtherMotherLoopState.Phase.PARTY_SOCIAL||s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p)
                ||p.isDeadOrDying()||p.isSpectator()||requested==null||requested.size()<1||requested.size()>3||game==null)
            return rejected(p,s,escrowToken);
        int serial=s.loop.event.getInt("Serial");
        if(escrowToken!=null&&!PartyEscrow.held(p,serial,escrowToken))return false;
        var selected=new ArrayList<CompoundTag>();var ids=new HashSet<UUID>();var actors=new HashSet<UUID>();
        for(var request:requested){
            if(request==null||!request.hasUUID("Id"))return rejected(p,s,escrowToken);
            var g=guest(s,request.getUUID("Id"));var actor=g==null?null:PartyGuests.entity(p,g);
            if(g==null||!ids.add(g.getUUID("Id"))||!actual(p,s,g,actor)||!actors.add(actor.getUUID())
                    ||!available(s,g)||actor.isPassenger()||actor.distanceToSqr(p)>144||!actor.hasLineOfSight(p))return rejected(p,s,escrowToken);
            selected.add(g);
        }
        var participants=participants(p.getUUID(),selected,game);CompoundTag rules;
        try{rules=TabletopGames.start(game,p.getRandom().nextLong(),participants);}catch(IllegalArgumentException bad){return rejected(p,s,escrowToken);}
        Vec3 center=PartyGameStations.closest(p,s);
        if(center==null||!PartyRegions.allowed(p,center))return rejected(p,s,escrowToken);
        var table=nearbyProp(p,s,ScenePropEntity.TABLE,center);
        boolean longTable=table!=null&&table.longDining();
        var chairs=new ArrayList<ScenePropEntity>();
        for(int seat=0;seat<participants.size();seat++){
            Vec3 position=seatPosition(center,longTable,seat);var chair=nearbyProp(p,s,ScenePropEntity.CHAIR,position);
            if(chair!=null&&chair.isVehicle()&&!(seat==0&&p.getVehicle()==chair))return rejected(p,s,escrowToken);
            if(!PartyRegions.allowed(p,position)||PartyFloor.at(p,BlockPos.containing(position))==null
                    ||chair==null&&!clear(p,position,.42,2.1,.42))return rejected(p,s,escrowToken);
            if(seat>0){var actor=PartyGuests.entity(p,selected.get(seat-1));var path=actor.getNavigation().createPath(BlockPos.containing(position),0);if(path==null||!path.canReach())return rejected(p,s,escrowToken);}
            chairs.add(chair);
        }
        if(p.isPassenger()&&p.getVehicle()!=chairs.get(0))return rejected(p,s,escrowToken);
        if(table==null&&!clear(p,center,1.8,.9,1.8))return rejected(p,s,escrowToken);
        // Never overlay an existing table game or another owner's furniture.
        if(!p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(center,center).inflate(1,1,1),e->e.kind()==ScenePropEntity.GAME_BOARD).isEmpty())return rejected(p,s,escrowToken);
        var m=new CompoundTag();m.putBoolean("Active",true);m.putUUID("Id",UUID.randomUUID());m.putUUID("Nonce",UUID.randomUUID());
        m.putInt("Serial",serial);m.put("Game",rules);m.putLong("Center",BlockPos.containing(center).asLong());m.putBoolean("LongTable",longTable);
        if(escrowToken!=null)m.putUUID("Escrow",escrowToken);
        m.put("Seats",new ListTag());m.put("CreatedProps",new ListTag());HouseParty.data(s).put("Match",m);
        try{
            if(table==null)table=create(p,s,m,ScenePropEntity.TABLE,center,TABLE_SLOT);
            m.putUUID("Table",table.getUUID());
            for(int seat=0;seat<participants.size();seat++){
                var chair=chairs.get(seat);if(chair==null){chair=create(p,s,m,ScenePropEntity.CHAIR,seatPosition(center,longTable,seat),CHAIR_SLOT+seat);chair.setYRot(facing(seat));}
                var slot=new CompoundTag();slot.putInt("Seat",seat);slot.putUUID("Chair",chair.getUUID());
                if(seat>0){var g=selected.get(seat-1);slot.putUUID("Guest",g.getUUID("Id"));slot.putUUID("Entity",g.getUUID("Entity"));g.putString("State","PLAYING_GAME");g.remove("Group");g.remove("Target");}
                m.getList("Seats",Tag.TAG_COMPOUND).add(slot);
            }
            var board=create(p,s,m,ScenePropEntity.GAME_BOARD,center.add(0,.93,0),BOARD_SLOT);m.putUUID("Board",board.getUUID());board.cakeData(TabletopGames.spectatorVisible(rules));
            var ownerChair=chair(p,m,0);if(ownerChair!=null){ownerChair.setGlowingTag(true);if(p.distanceToSqr(ownerChair)<9&&!p.isPassenger())p.startRiding(ownerChair,true);}
            VisitAudio.caption(p,"House Party","Take your seat at the game table. "+(game==TabletopGames.Game.THREAD_FOUR?TabletopGames.teamName(rules,0)+" vs "+TabletopGames.teamName(rules,1):"Playing with "+String.join(", ",selected.stream().map(g->g.getString("Name")).toList())),160);
            OtherMotherLoop.changed(p);return true;
        }catch(RuntimeException failure){abort(p,s);return false;}
    }
    /** Deterministic teams, disclosed by conversation before any wager: Wybie joins the owner when an opponent remains. */
    static List<TabletopGames.Participant> participants(UUID owner,List<CompoundTag> guests,TabletopGames.Game game){
        var result=new ArrayList<TabletopGames.Participant>();result.add(new TabletopGames.Participant(owner.toString(),"You",game==TabletopGames.Game.THREAD_FOUR?0:-1));
        int teammate=-1;
        if(game==TabletopGames.Game.THREAD_FOUR&&guests.size()>1){for(int i=0;i<guests.size();i++)if(guests.get(i).getString("Kind").equals("other_wybie")){teammate=i;break;}if(teammate<0&&guests.size()==3)teammate=2;}
        for(int i=0;i<guests.size();i++){var g=guests.get(i);result.add(new TabletopGames.Participant(g.getUUID("Id").toString(),g.getString("Name"),game==TabletopGames.Game.THREAD_FOUR?(i==teammate?0:1):-1));}
        return result;
    }
    private static boolean rejected(ServerPlayer p,VisitData.Scene s,UUID token){
        if(s!=null&&p.getUUID().equals(s.owner)&&token!=null&&(!active(s)||!match(s).hasUUID("Escrow")||!token.equals(match(s).getUUID("Escrow")))
                &&PartyEscrow.held(p,s.loop.event.getInt("Serial"),token)){PartyEscrow.settle(p,token,PartyEscrow.Outcome.CANCEL);PartyEscrow.deliverPending(p);}
        return false;
    }
    private static ScenePropEntity nearbyProp(ServerPlayer p,VisitData.Scene s,int kind,Vec3 position){
        return p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(position,position).inflate(.3,1,.3),e->e.kind()==kind&&p.getUUID().equals(e.owner)&&s.door.equals(e.storyDoor)&&e.distanceToSqr(position)<.1).stream().findFirst().orElse(null);
    }
    private static boolean clear(ServerPlayer p,Vec3 at,double x,double y,double z){
        var box=new AABB(at.x-x,at.y+.13,at.z-z,at.x+x,at.y+y,at.z+z);
        return p.serverLevel().noCollision(box)&&!p.serverLevel().containsAnyLiquid(box)
                &&p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,box).isEmpty();
    }
    static Vec3 seatPosition(Vec3 center,boolean longTable,int seat){return center.add(seat==2?-2.35:seat==3?2.35:0,0,seat==0?-(longTable?5.05:2.35):seat==1?(longTable?5.05:2.35):0);}
    static float facing(int seat){return seat==0?0:seat==1?180:seat==2?-90:90;}
    private static ScenePropEntity create(ServerPlayer p,VisitData.Scene s,CompoundTag m,int kind,Vec3 at,int slot){
        var prop=VisitEvents.prop(p,s,kind,at,slot);prop.getPersistentData().putUUID("PartyMatch",m.getUUID("Id"));var entry=new CompoundTag();entry.putUUID("Id",prop.getUUID());m.getList("CreatedProps",Tag.TAG_COMPOUND).add(entry);return prop;
    }
    private static ScenePropEntity prop(ServerPlayer p,CompoundTag m,String key){return m.hasUUID(key)&&p.serverLevel().getEntity(m.getUUID(key)) instanceof ScenePropEntity e?e:null;}
    private static ScenePropEntity chair(ServerPlayer p,CompoundTag m,int index){var seats=m.getList("Seats",Tag.TAG_COMPOUND);return index<0||index>=seats.size()?null:prop(p,seats.getCompound(index),"Chair");}
    private static Mob actor(ServerPlayer p,VisitData.Scene s,CompoundTag seat){
        if(!seat.hasUUID("Guest")||!seat.hasUUID("Entity"))return null;var g=guest(s,seat.getUUID("Guest"));var actor=g==null?null:PartyGuests.entity(p,g);
        return g!=null&&actual(p,s,g,actor)&&actor.getUUID().equals(seat.getUUID("Entity"))?actor:null;
    }
    private static boolean participantsPresent(ServerPlayer p,VisitData.Scene s,CompoundTag m){
        var seats=m.getList("Seats",Tag.TAG_COMPOUND);if(seats.size()!=TabletopGames.participantCount(m.getCompound("Game")))return false;
        for(int i=0;i<seats.size();i++){
            var chair=chair(p,m,i);if(chair==null||!p.getUUID().equals(chair.owner)||!s.door.equals(chair.storyDoor))return false;
            if(i>0){var seat=seats.getCompound(i);var g=seat.hasUUID("Guest")?guest(s,seat.getUUID("Guest")):null;var actor=actor(p,s,seat);
                if(actor==null||g==null||!g.getString("State").equals("PLAYING_GAME")||g.getBoolean("ReportPending"))return false;}
        }
        return prop(p,m,"Board")!=null;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){
        if(s==null||!p.getUUID().equals(s.owner)||!active(s))return;var m=match(s);
        if(!owned(p,s)||m.getInt("Serial")!=s.loop.event.getInt("Serial")||p.isDeadOrDying()||s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p)||HouseParty.data(s).getBoolean("Suspended")){abort(p,s);return;}
        var rules=m.getCompound("Game");if(!TabletopGames.validRoster(rules)||!participantsPresent(p,s,m)){abort(p,s);return;}
        if(rules.getBoolean("Done")){
            settle(p,s);int ticks=m.getInt("ResultTicks")+1;m.putInt("ResultTicks",ticks);
            if(ticks>=RESULT_GRACE){cleanup(p,s);return;}if(p.tickCount%5==0)send(p,s,false);return;
        }
        int age=m.getInt("Age")+1;m.putInt("Age",age);if(age>=MAX_MATCH_TICKS){abort(p,s);return;}
        boolean seated=seatGuests(p,s,m);var ownerChair=chair(p,m,0);boolean ownerSeated=p.getVehicle()==ownerChair;
        if(!m.getBoolean("Started")){
            int waiting=m.getInt("SeatTicks")+1;m.putInt("SeatTicks",waiting);
            if(waiting>SEAT_GRACE||HouseParty.data(s).getBoolean("FeastDue")){abort(p,s);return;}
            if(!seated||!ownerSeated)return;ownerChair.setGlowingTag(false);m.putBoolean("Started",true);send(p,s,true);
        }else if(!seated||!ownerSeated){
            int away=m.getInt("AwayTicks")+1;m.putInt("AwayTicks",away);if(away>AWAY_GRACE)abort(p,s);return;
        }
        m.putInt("AwayTicks",0);
        if(TabletopGames.game(rules)==TabletopGames.Game.SILENT_CHARADES)act(p,s,m);
        int oldActions=rules.getInt("ActionCount");TabletopGames.tick(rules);
        if(rules.getInt("ActionCount")!=oldActions){m.putUUID("Nonce",UUID.randomUUID());reaction(p,s,m);}
        if(rules.getBoolean("Done"))finished(p,s);else if(p.tickCount%5==0)send(p,s,false);
    }
    private static boolean seatGuests(ServerPlayer p,VisitData.Scene s,CompoundTag m){
        boolean seated=true;var seats=m.getList("Seats",Tag.TAG_COMPOUND);
        for(int i=1;i<seats.size();i++){
            var actor=actor(p,s,seats.getCompound(i));var chair=chair(p,m,i);if(actor==null||chair==null)return false;
            if(actor.getVehicle()==chair){actor.getNavigation().stop();actor.getLookControl().setLookAt(p,25,20);continue;}
            seated=false;if(actor.isPassenger()||chair.isVehicle())continue;
            Vec3 at=chair.position().add(0,.1,0);var box=actor.getDimensions(actor.getPose()).makeBoundingBox(at);
            if(actor.distanceToSqr(chair)<2.25&&p.serverLevel().noCollision(actor,box)&&!p.serverLevel().containsAnyLiquid(box)){
                if(actor.startRiding(chair,true)){actor.getNavigation().stop();actor.setDeltaMovement(Vec3.ZERO);}continue;
            }
            if(actor.getNavigation().isDone()||actor.tickCount%40==0)actor.getNavigation().moveTo(chair.getX(),chair.getY(),chair.getZ(),.9);
        }return seated;
    }
    private static void equip(Mob actor,CompoundTag m,Item item){
        var tag=actor.getPersistentData();if(!tag.hasUUID(HAND_OWNER)){tag.putUUID(HAND_OWNER,m.getUUID("Id"));tag.put(HAND,actor.getMainHandItem().save(new CompoundTag()));}
        if(tag.getUUID(HAND_OWNER).equals(m.getUUID("Id")))actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
    }
    private static void restoreHands(ServerPlayer p,VisitData.Scene s,CompoundTag m){
        for(var tag:m.getList("Seats",Tag.TAG_COMPOUND)){var actor=actor(p,s,(CompoundTag)tag);if(actor==null)continue;var saved=actor.getPersistentData();
            if(saved.hasUUID(HAND_OWNER)&&saved.getUUID(HAND_OWNER).equals(m.getUUID("Id"))){if(saved.contains(HAND))actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.of(saved.getCompound(HAND)));saved.remove(HAND);saved.remove(HAND_OWNER);}PartyGuests.gesture(actor,0,0);}
    }
    private static void act(ServerPlayer p,VisitData.Scene s,CompoundTag m){
        var rules=m.getCompound("Game");int who=rules.getInt("Actor"),topic=rules.getInt("Topic");
        if(!m.contains("Acting")||m.getInt("Acting")!=who){restoreHands(p,s,m);m.putInt("Acting",who);}
        if(who<=0||topic<0||topic>=6)return;var actor=actor(p,s,m.getList("Seats",Tag.TAG_COMPOUND).getCompound(who));
        if(actor!=null){Item[] props={Items.FEATHER,Items.FISHING_ROD,Items.CLOCK,Items.BREAD,Items.STRING,Items.STICK};equip(actor,m,props[topic]);PartyGuests.gesture(actor,topic+1,40);}
    }
    private static void reaction(ServerPlayer p,VisitData.Scene s,CompoundTag m){
        var rules=m.getCompound("Game");int who=rules.getInt("LastActor");
        if(who==0){p.swing(InteractionHand.MAIN_HAND);return;}var seats=m.getList("Seats",Tag.TAG_COMPOUND);if(who<0||who>=seats.size())return;
        var actor=actor(p,s,seats.getCompound(who));if(actor==null)return;actor.swing(InteractionHand.MAIN_HAND);PartyGuests.gesture(actor,6,40);
        if(TabletopGames.game(rules)!=TabletopGames.Game.SILENT_CHARADES)equip(actor,m,switch(TabletopGames.game(rules)){case BUTTON_CARDS->Items.PAPER;case THREAD_FOUR,BUTTON_FLICK->Items.STONE_BUTTON;case WOBBLY_TOWER->Items.STICK;default->Items.QUARTZ;});
    }
    static PartyEscrow.Outcome outcome(CompoundTag rules){
        if(!TabletopGames.completedNormally(rules))return PartyEscrow.Outcome.CANCEL;
        if(TabletopGames.participantWon(rules,0))return PartyEscrow.Outcome.WIN;
        return rules.getInt("Winner")<0?PartyEscrow.Outcome.DRAW:PartyEscrow.Outcome.LOSS;
    }
    private static void settle(ServerPlayer p,VisitData.Scene s){
        var m=match(s);if(m.getBoolean("Settled"))return;var rules=m.getCompound("Game");if(!rules.getBoolean("Done"))return;
        var outcome=outcome(rules);if(m.hasUUID("Escrow"))PartyEscrow.settle(p,m.getUUID("Escrow"),outcome);
        m.putBoolean("Settled",true);m.putString("Outcome",outcome.name());PartyEscrow.deliverPending(p);OtherMotherLoop.changed(p);
    }
    private static void finished(ServerPlayer p,VisitData.Scene s){
        var m=match(s);restoreHands(p,s,m);m.remove("PullStarted");m.putUUID("Nonce",UUID.randomUUID());settle(p,s);
        var rules=m.getCompound("Game");VisitAudio.caption(p,"House Party",TabletopGames.resultText(rules),100);
        var seats=m.getList("Seats",Tag.TAG_COMPOUND);for(int i=1;i<seats.size();i++){var actor=actor(p,s,seats.getCompound(i));if(actor!=null)PartyGuests.gesture(actor,TabletopGames.participantWon(rules,i)?7:8,80);}
        send(p,s,true);
    }
    public static boolean choice(ServerPlayer p,UUID nonce,String action){
        var s=HouseParty.active(p);if(s==null||!active(s))return false;var m=match(s);
        if(action==null||!m.hasUUID("Nonce")||!m.getUUID("Nonce").equals(nonce)||m.getInt("Serial")!=s.loop.event.getInt("Serial"))return true;
        var rules=m.getCompound("Game");if(rules.getBoolean("Done")){if(action.equals("finish"))cleanup(p,s);return true;}
        if(!m.getBoolean("Started")||p.isDeadOrDying()||HouseParty.data(s).getBoolean("Suspended")||s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p)||p.getVehicle()!=chair(p,m,0)||!participantsPresent(p,s,m))return true;
        var game=TabletopGames.game(rules);if(game!=TabletopGames.Game.SILENT_CHARADES&&rules.getInt("Turn")!=0||game==TabletopGames.Game.SILENT_CHARADES&&rules.getInt("TurnTicks")<60)return true;
        if(action.startsWith("pull_begin:")){
            if(game!=TabletopGames.Game.WOBBLY_TOWER)return true;
            try{var parts=action.split(":");if(parts.length!=3)return true;int row=Integer.parseInt(parts[1]),piece=Integer.parseInt(parts[2]);if(!Double.isFinite(TabletopGames.towerCost(rules.getIntArray("Tower"),row,piece,1)))return true;
                m.putInt("PullRow",row);m.putInt("PullPiece",piece);m.putLong("PullStarted",OtherMotherLoop.now(p));}
            catch(NumberFormatException ignored){}return true;
        }
        if(action.equals("pull_release")){
            if(game!=TabletopGames.Game.WOBBLY_TOWER||!m.contains("PullStarted"))return true;
            long ticks=OtherMotherLoop.now(p)-m.getLong("PullStarted");m.remove("PullStarted");if(ticks<0||ticks>200)return true;
            action="pull:"+m.getInt("PullRow")+":"+m.getInt("PullPiece")+":"+Math.max(0,1-Math.abs(ticks-40)/40.);
        }else if(action.startsWith("pull:"))return true;
        if(TabletopGames.move(rules,action)){m.putUUID("Nonce",UUID.randomUUID());reaction(p,s,m);if(rules.getBoolean("Done"))finished(p,s);else send(p,s,false);OtherMotherLoop.changed(p);}return true;
    }
    public static boolean interact(ServerPlayer p,ScenePropEntity prop){
        var s=HouseParty.active(p);if(s==null||!active(s))return false;var m=match(s);
        boolean ours=m.hasUUID("Board")&&prop.getUUID().equals(m.getUUID("Board"));
        for(var tag:m.getList("Seats",Tag.TAG_COMPOUND))if(prop.getUUID().equals(((CompoundTag)tag).getUUID("Chair")))ours=true;
        if(!ours)return false;if(!p.getUUID().equals(prop.owner)||!s.door.equals(prop.storyDoor)||!withinReach(p,prop)||s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p))return true;
        if(prop==chair(p,m,0)&&!prop.isVehicle()&&!p.isPassenger())p.startRiding(prop,true);
        if(m.getBoolean("Started")&&p.getVehicle()==chair(p,m,0))send(p,s,true);return true;
    }
    static boolean withinReach(ServerPlayer p,ScenePropEntity prop){
        // The client targets the backrest, not the chair's floor-level origin.
        var eye=p.getEyePosition();var b=prop.getBoundingBox();
        var nearest=new Vec3(net.minecraft.util.Mth.clamp(eye.x,b.minX,b.maxX),net.minecraft.util.Mth.clamp(eye.y,b.minY,b.maxY),net.minecraft.util.Mth.clamp(eye.z,b.minZ,b.maxZ));
        if(eye.distanceToSqr(nearest)>36)return false;
        // Low origins may be behind carpet/table while the backrest/top is visible.
        for(var target:java.util.List.of(nearest,b.getCenter(),new Vec3((b.minX+b.maxX)/2,b.maxY-.001,(b.minZ+b.maxZ)/2))){
            var hit=p.level().clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS)return true;
        }return false;
    }
    private static void send(ServerPlayer p,VisitData.Scene s,boolean open){
        if(!owned(p,s)||!active(s))return;var m=match(s);var rules=m.getCompound("Game");var packet=new CompoundTag();
        packet.putBoolean("FamilyGame",true);packet.putBoolean("PartyGame",true);packet.putBoolean("TabletopActive",true);packet.putBoolean("Open",open);
        packet.putString("Phase",s.loop.phase.name());packet.putUUID("Nonce",m.getUUID("Nonce"));packet.put("Game",TabletopGames.visible(rules));SecretsNetwork.send(p,packet);
        var board=prop(p,m,"Board");if(board!=null)board.cakeData(TabletopGames.spectatorVisible(rules));
    }
    private static void close(ServerPlayer p){var n=new CompoundTag();n.putBoolean("FamilyGame",true);n.putBoolean("PartyGame",true);n.putBoolean("Close",true);SecretsNetwork.send(p,n);}
    private static void abort(ServerPlayer p,VisitData.Scene s){if(s==null||!p.getUUID().equals(s.owner)||!active(s))return;TabletopGames.cancel(match(s).getCompound("Game"));cleanup(p,s);p.displayClientMessage(net.minecraft.network.chat.Component.literal("The game couldn't continue. Any wager was refunded; invite the guest to try again."),true);}
    public static void suspend(ServerPlayer p,VisitData.Scene s){abort(p,s);}
    public static void cleanup(ServerPlayer p,VisitData.Scene s){
        if(s==null||!p.getUUID().equals(s.owner)||!active(s))return;var m=match(s);TabletopGames.cancel(m.getCompound("Game"));settle(p,s);close(p);restoreHands(p,s,m);
        var seats=m.getList("Seats",Tag.TAG_COMPOUND);if(p.getVehicle()==chair(p,m,0))p.stopRiding();
        var ownerChair=chair(p,m,0);if(ownerChair!=null)ownerChair.setGlowingTag(false);
        for(int i=1;i<seats.size();i++){
            var seat=seats.getCompound(i);var actor=actor(p,s,seat);var g=seat.hasUUID("Guest")?guest(s,seat.getUUID("Guest")):null;
            if(actor!=null){if(actor.getVehicle()==chair(p,m,i))actor.stopRiding();actor.getNavigation().stop();PartyGuests.gesture(actor,0,0);}
            if(g!=null&&g.getString("State").equals("PLAYING_GAME")){g.putString("State","WANDERING");g.remove("Target");g.putLong("RestUntil",HouseParty.data(s).getLong("Clock")+80);}
        }
        for(var tag:m.getList("CreatedProps",Tag.TAG_COMPOUND)){var e=prop(p,(CompoundTag)tag,"Id");
            if(e!=null&&p.getUUID().equals(e.owner)&&s.door.equals(e.storyDoor)&&e.getPersistentData().getInt("VisitEventSerial")==m.getInt("Serial")
                    &&e.getPersistentData().hasUUID("PartyMatch")&&m.getUUID("Id").equals(e.getPersistentData().getUUID("PartyMatch"))){e.ejectPassengers();e.discard();}}
        HouseParty.data(s).remove("Match");OtherMotherLoop.changed(p);
    }
    /** Explicit operator testing of the current match; never a normal gameplay packet or client-selected payout. */
    public static String debugOperator(ServerPlayer p,VisitData.Scene s,String action){
        if(!NumberedDebugActions.operator(p,s)||!owned(p,s)||!active(s))return "No owned active operator match.";
        var m=match(s);var rules=m.getCompound("Game");
        if(!m.getBoolean("Started")||rules.getBoolean("Done")||m.getBoolean("Settled")||!participantsPresent(p,s,m)
                ||s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p)||HouseParty.data(s).getBoolean("Suspended"))return "Match must be started, unsettled and have its actual participants present.";
        int winner=switch(action){case "win"->0;case "loss"->1;case "draw"->-1;default->-2;};if(winner==-2)return "Unknown forced test result.";
        m.putBoolean("OperatorForced",true);m.putString("OperatorResult",action);rules.putBoolean("OperatorForced",true);rules.putInt("Physics",0);
        if(TabletopGames.game(rules)==TabletopGames.Game.WOBBLY_TOWER){if(winner<0)rules.remove("Loser");else rules.putInt("Loser",action.equals("win")?1:0);}
        TabletopGames.finish(rules,winner);finished(p,s);OtherMotherLoop.changed(p);
        return "OPERATOR TEST: "+action+" settled once through the current match's real escrow. This was not a normally played result.";
    }

    private PartyMatches(){}
}
