package com.coraline.feast;

import com.coraline.entity.CharacterEntity;
import com.coraline.registry.*;
import com.coraline.secrets.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Persisted guest identities, physical arrivals, social groups and normal navigation. */
public final class PartyGuests {
    public static List<CompoundTag> guests(VisitData.Scene s){return HouseParty.data(s).getList("Guests",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).toList();}
    public static Mob entity(ServerPlayer p,CompoundTag g){if(!g.hasUUID("Entity")||!g.hasUUID("Id"))return null;for(var level:p.server.getAllLevels())if(level.getEntity(g.getUUID("Entity")) instanceof Mob m){var n=m.getPersistentData();if(n.hasUUID("PartyOwner")&&p.getUUID().equals(n.getUUID("PartyOwner"))&&n.hasUUID("PartyGuest")&&g.getUUID("Id").equals(n.getUUID("PartyGuest")))return m;}return null;}
    public static void roster(ServerPlayer p,VisitData.Scene s){
        VisitProgression.refresh(p,s);
        var n=HouseParty.data(s);int order=0;
        if(s.loop.story.getBoolean("BobinskyIntroductionComplete"))add(n,"other_bobinsky","Other Bobinsky",null,order++);
        if(s.loop.story.getBoolean("TeaIntroductionComplete")){add(n,"other_miss_spink","Other Spink",null,order++);add(n,"other_miss_forcible","Other Forcible",null,order++);}
        if(WybieOptionalVisits.allowed(s))add(n,"other_wybie","Other Wybie",null,order++);
        var others=new ArrayList<>(p.server.getPlayerList().getPlayers());others.sort(Comparator.comparing(x->x.getUUID().toString()));
        for(var other:others)if(!other.getUUID().equals(p.getUUID())){
            var g=add(n,"counterpart","Other "+other.getGameProfile().getName(),other.getUUID(),order++);
            // Only the public display identity and texture property are copied.
            var profile=new GameProfile(other.getUUID(),other.getGameProfile().getName());profile.getProperties().putAll("textures",other.getGameProfile().getProperties().get("textures"));
            g.put("Profile",NbtUtils.writeGameProfile(new CompoundTag(),profile));
        }
        int generics=Math.max(6,n.getInt("Capacity")-order);
        for(int i=0;i<generics;i++)add(n,"resident","Guest "+(i+1),null,order++);
        n.putInt("RosterRevision",2);
    }
    /** Expand old saved parties without replacing actors, clocks or guest identities. */
    public static void upgradeRoster(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);VisitProgression.refresh(p,s);boolean expand=n.getInt("RosterRevision")<2;
        if(expand)n.putInt("Capacity",Math.min(40,Math.max(24,n.getInt("Capacity")*2)));int order=guests(s).stream().mapToInt(g->g.getInt("Order")).max().orElse(-1)+1;
        var kinds=new HashSet<>(guests(s).stream().map(g->g.getString("Kind")).toList());
        if(s.loop.story.getBoolean("BobinskyIntroductionComplete")&&!kinds.contains("other_bobinsky"))add(n,"other_bobinsky","Other Bobinsky",null,order++);
        if(s.loop.story.getBoolean("TeaIntroductionComplete")){if(!kinds.contains("other_miss_spink"))add(n,"other_miss_spink","Other Spink",null,order++);if(!kinds.contains("other_miss_forcible"))add(n,"other_miss_forcible","Other Forcible",null,order++);}
        if(WybieOptionalVisits.allowed(s)&&!kinds.contains("other_wybie"))add(n,"other_wybie","Other Wybie",null,order++);
        while(guests(s).size()<n.getInt("Capacity"))add(n,"resident","Guest "+(order+1),null,order++);
        n.putInt("RosterRevision",2);if(expand||order>guests(s).size())OtherMotherLoop.changed(p);
    }
    private static CompoundTag add(CompoundTag n,String kind,String name,UUID counterpart,int order){var g=HousePartyLedger.addGuest(n,kind,counterpart,order);g.putString("Name",name);g.putInt("Skin",Math.floorMod((int)(n.getLong("Seed")+order*17),64));g.putBoolean("Female",order%2==0);return g;}
    public static void tick(ServerPlayer p,VisitData.Scene s){
        if(p.tickCount%20!=0)return;var n=HouseParty.data(s);long now=n.getLong("Clock");
        if(!PlayerParty.isPlayerParty(s)){arrivalWave(p,s);PartyEntrance.tick(p,s);}
        int active=(int)guests(s).stream().filter(g->entity(p,g)!=null&&!g.getString("State").equals("GONE")).count();
        boolean queued=guests(s).stream().anyMatch(g->g.getString("State").equals("QUEUED"));
        for(var g:guests(s)){
            String state=g.getString("State");var m=entity(p,g);
            if(PlayerParty.isPlayerParty(s)&&state.equals("QUEUED")&&p.level().dimension().location().toString().equals(n.getString("HouseDimension"))&&active<n.getInt("Capacity")&&now>=g.getInt("ArrivalAt")&&!n.getBoolean("FeastDue")&&now>=n.getLong("NextArrival")){
                if(spawn(p,s,g)){active++;n.putLong("NextArrival",now+60+p.getRandom().nextInt(41));}
            }else if(m!=null&&queued&&g.getString("Kind").equals("counterpart")&&now-g.getLong("ArrivedAt")>3600&&state.equals("WANDERING"))g.putString("State","LEAVING");
            // Missing actors never create an identical replacement at a watched location.
            else if(m==null&&!state.equals("QUEUED")&&!state.equals("GONE")&&!state.equals("LEAVING")){
                g.putInt("MissingTicks",g.getInt("MissingTicks")+20);
                if(g.getInt("MissingTicks")>=200)g.putString("State","GONE");
            }else if(m!=null)g.remove("MissingTicks");
        }
        if(!n.getBoolean("FeastDue")&&now>=n.getLong("NextGroup")){if(PartyGuestHouseAI.away(p,s))PartyGuestHouseAI.formGroup(p,s);else formGroup(p,s);n.putLong("NextGroup",now+140+p.getRandom().nextInt(301));}
    }
    static int arrivalGroupSize(net.minecraft.util.RandomSource random,int available){return Math.min(Math.max(0,available),1+random.nextInt(6));}
    static int arrivalDelay(net.minecraft.util.RandomSource random){return 60+random.nextInt(141);}
    private static void arrivalWave(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);long now=n.getLong("Clock");if(n.getBoolean("FeastDue")||now<n.getLong("NextArrival"))return;
        var all=guests(s);
        if(n.getInt("WaveRemaining")==0&&all.stream().anyMatch(g->Set.of("ARRIVING","WAITING_AT_DOOR","ENTERING").contains(g.getString("State"))&&entity(p,g)!=null))return;
        int active=(int)all.stream().filter(g->entity(p,g)!=null&&!g.getString("State").equals("GONE")).count();
        var queued=all.stream().filter(g->g.getString("State").equals("QUEUED")).sorted(Comparator.comparingInt(g->g.getString("Kind").startsWith("other_")?0:1)).toList();
        if(queued.isEmpty()||active>=n.getInt("Capacity"))return;
        if(n.getInt("WaveRemaining")==0){
            if(queued.stream().mapToInt(g->g.getInt("ArrivalAt")).min().orElse(0)>now)return;
            boolean named=queued.get(0).getString("Kind").startsWith("other_");
            n.putBoolean("WaveNamed",named);
            boolean pair=queued.get(0).getString("Kind").equals("other_miss_spink")||queued.get(0).getString("Kind").equals("other_miss_forcible");n.putBoolean("WaveSpinkPair",pair);
            n.putInt("WaveRemaining",named?(pair?Math.min(2,n.getInt("Capacity")-active):1):arrivalGroupSize(p.getRandom(),Math.min(queued.size(),n.getInt("Capacity")-active)));
            n.putInt("ArrivalWave",n.getInt("ArrivalWave")+1);
        }
        for(var g:queued){
            if(n.getInt("WaveRemaining")==0||active>=n.getInt("Capacity"))break;
            if(g.getString("Kind").startsWith("other_")!=n.getBoolean("WaveNamed"))continue;
            if(n.getBoolean("WaveSpinkPair")&&!Set.of("other_miss_spink","other_miss_forcible").contains(g.getString("Kind")))continue;
            if(spawn(p,s,g)){g.putInt("ArrivalWave",n.getInt("ArrivalWave"));n.putInt("WaveRemaining",n.getInt("WaveRemaining")-1);active++;}
        }
        n.putLong("NextArrival",now+(n.getInt("WaveRemaining")>0?40:arrivalDelay(p.getRandom())));
        OtherMotherLoop.changed(p);
    }
    private static boolean watched(ServerPlayer p,Vec3 at){return p.distanceToSqr(at)<144&&p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),at.add(0,1,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    static boolean spawn(ServerPlayer p,VisitData.Scene s,CompoundTag g){
        var realm=SecretWorld.realm(p);if(realm==null||realm.porch==null)return false;Mob actor=null;
        String kind=g.getString("Kind");boolean named=CharacterCast.ENTITIES.containsKey(kind);
        if(named){
            var found=p.serverLevel().getEntitiesOfClass(CharacterEntity.class,new AABB(realm.house).inflate(150),e->e.characterId().equals(kind)&&e.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(e.getPersistentData().getUUID("RealmDoor"))&&!e.getPersistentData().hasUUID("GameNightOwner")&&!e.getPersistentData().hasUUID("PartyOwner")&&!e.getPersistentData().hasUUID("ShowActorOwner")&&!e.getPersistentData().hasUUID("WybieCompanionOwner"));
            if(!found.isEmpty())actor=found.get(0);
        }
        boolean created=actor==null;
        if(created){
            actor=named?CharacterCast.ENTITIES.get(kind).get().create(p.serverLevel()):ModEntities.FISHING_CONTEST_NPC.get().create(p.serverLevel());if(actor==null)return false;
            BlockPos at=null;
            for(int distance=6;distance<=48&&at==null;distance+=3)for(int side=-12;side<=12;side+=6){
                var tryAt=realm.porch.offset(side,0,-distance);var feet=Reconciliation.safe(p,tryAt);if(feet==null)continue;
                var point=Vec3.atBottomCenterOf(feet);if(PartyRegions.allowed(p,point)||watched(p,point)||!p.serverLevel().noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(point)))continue;
                at=feet;break;
            }
            if(at==null)return false;actor.setPos(Vec3.atBottomCenterOf(at));
        }
        var tag=actor.getPersistentData();tag.putUUID("PartyOwner",p.getUUID());tag.putUUID("PartyGuest",g.getUUID("Id"));tag.putInt("PartySerial",s.loop.event.getInt("Serial"));tag.putBoolean("PartyCreated",created);
        tag.putBoolean("PartyOldInvulnerable",actor.isInvulnerable());tag.put("PartyOldHand",actor.getMainHandItem().save(new CompoundTag()));tag.putLong("PartyOldPosition",actor.blockPosition().asLong());
        tag.putBoolean("PartyOldNoAI",actor.isNoAi());tag.putBoolean("PartyOldNoGravity",actor.isNoGravity());tag.putBoolean("PartyOldPhysics",actor.noPhysics);tag.putUUID("RealmDoor",s.door);actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;actor.setPersistenceRequired();actor.setInvulnerable(true);actor.setMaxUpStep(1.05F);
        var range=actor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);if(range!=null){tag.putDouble("PartyOldFollowRange",range.getBaseValue());range.setBaseValue(Math.max(96,range.getBaseValue()));}actor.getNavigation().setMaxVisitedNodesMultiplier(4);
        if(actor instanceof FishingContestNpcEntity npc){npc.configure(g.getInt("Skin"),g.getBoolean("Female"),false,g.getInt("Order"));npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);if(g.contains("Profile"))npc.partyProfile(g.getCompound("Profile"));npc.setCustomName(Component.literal(g.getString("Name")));}
        if(created){actor.getPersistentData().putBoolean("CoralineDecoration",true);if(!p.serverLevel().addFreshEntity(actor))return false;}
        g.putUUID("Entity",actor.getUUID());g.putString("State","ARRIVING");g.putLong("ArrivedAt",HouseParty.data(s).getLong("Clock"));g.putLong("MoveStarted",HouseParty.data(s).getLong("Clock"));return true;
    }
    public static boolean control(Mob m){
        var tag=m.getPersistentData();if(!tag.hasUUID("PartyOwner"))return false;
        var p=m.getServer().getPlayerList().getPlayer(tag.getUUID("PartyOwner"));
        var s=VisitData.get(m.getServer()).scenes.get(tag.getUUID("PartyOwner"));
        if(s==null||!s.loop.event.getBoolean("PartyPending")||tag.getInt("PartySerial")!=s.loop.event.getInt("Serial")){release(m);return false;}
        if(p==null){m.getNavigation().stop();return true;}
        var g=HousePartyLedger.guest(HouseParty.data(s),tag.getUUID("PartyGuest"));if(g==null||!g.hasUUID("Entity")||!g.getUUID("Entity").equals(m.getUUID())||g.getString("State").equals("GONE")){release(m);return false;}
        long now=HouseParty.data(s).getLong("Clock");String state=g.getString("State");
        if(!PlayerParty.isPlayerParty(s)&&m instanceof CharacterEntity&&!HouseParty.insideHouse(p,m.position())&&!Set.of("LEAVING","GONE","QUEUED","PERFORMING","AUDIENCE").contains(state)&&!g.getBoolean("Admitted")){g.putString("State","ARRIVING");state="ARRIVING";}
        if(Set.of("ARRIVING","WAITING_AT_DOOR","ENTERING").contains(state)){m.setNoAi(false);m.setNoGravity(false);m.noPhysics=false;}
        if(!state.equals("CONVERSING"))VisitAudio.chatter(m,false);
        if(PlayerParty.isPlayerParty(s)&&Set.of("PERFORMING","AUDIENCE").contains(state))return true; // Canonical show owns these actors, including another dimension.
        if(PlayerParty.isPlayerParty(s)&&state.equals("APPROACHING_CIRCUS")){if(g.contains("CircusTarget")&&m.tickCount%40==0){var at=BlockPos.of(g.getLong("CircusTarget"));m.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.9);}return true;}
        if(PlayerParty.isPlayerParty(s)&&state.equals("APPROACHING_THEATRE")){PlayerParty.control(m,p,s,g);return true;}
        if(p.level()!=m.level()){if(PartyGuestHouseAI.control(m,p,s,g))return true;m.getNavigation().stop();return true;}
        if(PartyEntrance.keepInside(m,p,s,g))return true;
        if(PlayerParty.isPlayerParty(s)&&Set.of("CELEBRATING","PRESENTING_GIFT").contains(state)){PlayerParty.control(m,p,s,g);return true;}
        if(state.equals("REPORTING")){PartySurveillance.report(m,p,s,g);return true;}
        if(state.equals("WARNING")){m.getLookControl().setLookAt(p,30,20);if(m.distanceToSqr(p)>9){if(m.tickCount%40==0)m.getNavigation().moveTo(p,.9);}else m.getNavigation().stop();return true;}
        if(state.equals("PLAYING_GAME"))return true;
        if(state.equals("PLAYING_KIDS_GAME"))return true;
        if(state.equals("TALKING_TO_PLAYER")){m.getNavigation().stop();m.getLookControl().setLookAt(p,25,20);return true;}
        if(state.equals("MOVING_TO_FEAST")||state.equals("EATING")){PartyBanquet.control(m,p,s,g);return true;}
        if(state.equals("LEAVING")){leave(m,p,s,g);return true;}
        if(Reconciliation.active(p)){m.getNavigation().stop();return true;}
        if(!PlayerParty.isPlayerParty(s)&&Set.of("ARRIVING","WAITING_AT_DOOR","ENTERING").contains(state)){PartyEntrance.arrive(m,p,s,g);return true;}
        if(state.equals("ARRIVING")){
            var realm=SecretWorld.realm(p);if(PartyRegions.allowed(p,m.position())){g.putString("State","WANDERING");g.remove("Target");gesture(m,1,40);return true;}
            if(realm!=null&&realm.porch!=null&&m.tickCount%40==0)move(m,realm.porch.south(5),.9);return true;
        }
        if(state.equals("CONVERSING")){
            if(now>=g.getLong("GroupUntil")){g.putString("State","WANDERING");g.remove("Group");g.putLong("RestUntil",now+40+p.getRandom().nextInt(101));return true;}
            m.getNavigation().stop();var group=guests(s).stream().filter(x->x.getString("State").equals("CONVERSING")&&x.hasUUID("Group")&&g.hasUUID("Group")&&x.getUUID("Group").equals(g.getUUID("Group"))&&entity(p,x)!=null).toList();
            var speakers=group.stream().filter(x->!x.getString("Kind").equals("other_wybie")).toList();
            var speaker=speakers.isEmpty()?null:entity(p,speakers.get((int)(now/100)%speakers.size()));
            if(speaker!=null)m.getLookControl().setLookAt(speaker,20,15);boolean close=group.stream().map(x->entity(p,x)).filter(Objects::nonNull).anyMatch(other->other!=m&&Math.abs(other.getY()-m.getY())<2&&other.distanceToSqr(m)<36&&m.hasLineOfSight(other));
            VisitAudio.chatter(m,speaker==m&&close&&!SecretDialogue.busy(p)&&!PartySurveillance.warningSpeechActive(s));if(speaker==m&&m.tickCount%100==0){gesture(m,1+p.getRandom().nextInt(3),50);ambientConversation(p,s,g,m,now);}
            return true;
        }
        if(state.equals("APPROACHING_GROUP")){
            if(now>g.getLong("GroupUntil")){g.putString("State","WANDERING");g.remove("Target");return true;}
            if(g.contains("Target")&&m.distanceToSqr(Vec3.atBottomCenterOf(BlockPos.of(g.getLong("Target"))))<1.5){g.putString("State","CONVERSING");m.getNavigation().stop();return true;}
        }
        if(now<g.getLong("RestUntil")){m.getNavigation().stop();if(m.tickCount%80==0)m.setYHeadRot(m.getYRot()+p.getRandom().nextInt(91)-45);return true;}
        if(g.contains("Target")){
            var at=BlockPos.of(g.getLong("Target"));
            if(m.distanceToSqr(Vec3.atBottomCenterOf(at))<1.8||now-g.getLong("MoveStarted")>600){g.remove("Target");g.putLong("RestUntil",now+80+p.getRandom().nextInt(241));m.getNavigation().stop();return true;}
            if(m.tickCount%40==0)move(m,at,.65+p.getRandom().nextDouble()*.25);return true;
        }
        if(m.tickCount%40==0){
            // Only a changing subset of rooms is populated; private opportunities remain.
            int room=PlayerParty.isPlayerParty(s)?p.getRandom().nextInt(32):p.getRandom().nextInt(PartyRegions.rooms().size());
            // Deliberately distribute a third of the crowd around the public
            // upper gallery/landing/library, rather than relying on a slow
            // modulo cycle through the downstairs rooms first.
            if(!PlayerParty.isPlayerParty(s)&&g.getInt("Order")%3==0)room=10+p.getRandom().nextInt(3);
            PartyRegions.destination(p,m,room,HouseParty.data(s).getLong("Seed")+now+g.getInt("Order")*31L).ifPresent(at->{g.putLong("Target",at.asLong());g.putLong("MoveStarted",now);move(m,at,.8);});
        }return true;
    }
    private static void move(Mob m,BlockPos at,double speed){
        if(m.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}
        var path=m.getNavigation().createPath(at,0);if(path!=null&&path.canReach())m.getNavigation().moveTo(path,speed);
    }
    private static void formGroup(ServerPlayer p,VisitData.Scene s){
        var available=guests(s).stream().filter(g->g.getString("State").equals("WANDERING")&&entity(p,g)!=null).toList();if(available.size()<2)return;
        var leader=available.get(p.getRandom().nextInt(available.size()));var anchor=entity(p,leader);UUID id=UUID.randomUUID();long until=HouseParty.data(s).getLong("Clock")+300+p.getRandom().nextInt(901);int count=0,target=2+p.getRandom().nextInt(3);
        for(var g:available){var m=entity(p,g);if(m.distanceToSqr(anchor)>100||count>=target)continue;
            double angle=.6+count*1.9;var at=BlockPos.containing(anchor.position().add(Math.cos(angle)*1.7,0,Math.sin(angle)*1.7));
            var standing=PartyFloor.at(p,at);if(standing==null||!PartyRegions.allowed(p,standing)||!p.serverLevel().noCollision(m,m.getDimensions(m.getPose()).makeBoundingBox(standing)))continue;
            var path=m.getNavigation().createPath(at,0);if(path==null||!path.canReach())continue;
            g.putUUID("Group",id);g.putLong("GroupUntil",until);g.putLong("Target",at.asLong());g.putLong("MoveStarted",HouseParty.data(s).getLong("Clock"));g.putString("State","APPROACHING_GROUP");m.getNavigation().moveTo(path,.8);count++;
        }
    }
    private static void ambientConversation(ServerPlayer p,VisitData.Scene s,CompoundTag g,Mob actor,long now){
        if(g.getString("Kind").equals("other_wybie"))return; // Other Wybie is silent.
        var n=HouseParty.data(s);if(now<n.getLong("NextAmbientCaption")||actor.distanceToSqr(p)>100||!actor.hasLineOfSight(p)||SecretDialogue.busy(p)||PartySurveillance.warningSpeechActive(s))return;
        int topic=g.hasUUID("Group")?Math.floorMod(g.getUUID("Group").hashCode(),4):0;
        String[] remarks={"Have you seen the view from the upstairs gallery?","The music carries beautifully through this house.","I wonder what our hostess has prepared for supper.","It is good to see so many neighbors together."};
        String[] answers={"Yes, the garden looks quite different from up there.","It does. I could listen to that piano all evening.","Something extravagant, knowing her.","Indeed. Come and join our conversation."};
        VisitAudio.caption(p,g.getString("Name"),((now/100)&1)==0?remarks[topic]:answers[topic],90);n.putLong("NextAmbientCaption",now+120);
    }
    public static void gesture(Mob m,int gesture,int ticks){if(m instanceof CharacterEntity c)c.gameGesture(gesture,ticks);else if(m instanceof FishingContestNpcEntity f)f.partyGesture(gesture,ticks);}
    public static void depart(ServerPlayer p,VisitData.Scene s){HouseParty.data(s).putInt("DepartureTicks",0);for(var g:guests(s)){if(g.getString("State").equals("QUEUED")){g.putString("State","GONE");continue;}g.putString("State","LEAVING");g.putLong("LeaveAfter",HouseParty.data(s).getLong("Clock")+Math.min(160,g.getInt("Order")*4L));var m=entity(p,g);if(m!=null){m.stopRiding();VisitAudio.chatter(m,false);}}}
    private static void leave(Mob m,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        if(HouseParty.data(s).getLong("Clock")<g.getLong("LeaveAfter")){m.getNavigation().stop();return;}
        if(!PlayerParty.isPlayerParty(s)&&!PartyCelebration.leavePresent(p,s,g,m))return;
        var realm=SecretWorld.realm(p);if(realm==null||realm.porch==null)return;
        var out=realm.porch.north(12);if(m.tickCount%10==0)move(m,out,1.25);
        boolean outside=!PartyRegions.allowed(p,m.position())&&m.distanceToSqr(realm.porch.getCenter())>36;
        boolean stalled=HouseParty.data(s).getLong("Clock")-g.getLong("LeaveAfter")>600&&m.getNavigation().isDone();
        if((outside||stalled)&&!watched(p,m.position())){g.putString("State","GONE");release(m);}
    }
    public static boolean departed(ServerPlayer p,VisitData.Scene s){return guests(s).stream().allMatch(g->g.getString("State").equals("GONE")||entity(p,g)==null);}
    public static void cleanup(ServerPlayer p,VisitData.Scene s){for(var g:guests(s)){var m=entity(p,g);if(m!=null)release(m);}}
    static void release(Mob m){
        if(m instanceof CharacterEntity cast)cast.partyDoorDuck(false);
        VisitAudio.chatter(m,false);
        var tag=m.getPersistentData();m.stopRiding();m.getNavigation().stop();gesture(m,0,0);
        if(tag.contains("PartyOldFollowRange")){var range=m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);if(range!=null)range.setBaseValue(tag.getDouble("PartyOldFollowRange"));tag.remove("PartyOldFollowRange");}m.getNavigation().setMaxVisitedNodesMultiplier(1);
        if(tag.getBoolean("PartyCreated")){m.discard();return;}
        if(tag.contains("PartyOldInvulnerable"))m.setInvulnerable(tag.getBoolean("PartyOldInvulnerable"));
        if(tag.contains("PartyOldNoAI")){m.setNoAi(tag.getBoolean("PartyOldNoAI"));m.setNoGravity(tag.getBoolean("PartyOldNoGravity"));m.noPhysics=tag.getBoolean("PartyOldPhysics");tag.remove("PartyOldNoAI");tag.remove("PartyOldNoGravity");tag.remove("PartyOldPhysics");}
        if(tag.contains("PartyOldHand"))m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.of(tag.getCompound("PartyOldHand")));
        for(String key:List.of("PartyOwner","PartyGuest","PartySerial","PartyCreated","PartyOldInvulnerable","PartyOldHand","PartyOldPosition","PartyMatchOldHand","PartyMatchHandOwner"))tag.remove(key);
    }
    /** Operator-only diagnostics use the same actors, path checks and normal attendance accounting. */
    public static String debugOperator(ServerPlayer p,VisitData.Scene s,String action,String value){
        if(!NumberedDebugActions.operator(p,s)||HouseParty.active(p)!=s)return "No owned operator party.";
        var n=HouseParty.data(s);
        if(s.loop.secrets.searching()||Reconciliation.active(p)||SecretDialogue.busy(p)||n.getBoolean("FeastDue"))return "Guest controls require an uninterrupted social phase.";
        switch(action){
            case "guests","special"->{
                int scheduled=0;
                for(var g:guests(s))if(g.getString("State").equals("QUEUED")&&(action.equals("guests")||g.getString("Kind").startsWith("other_"))){
                    if(g.getString("Kind").equals("other_wybie")&&!WybieOptionalVisits.allowed(s))continue;
                    g.putInt("ArrivalAt",0);scheduled++;
                }
                n.putLong("NextArrival",n.getLong("Clock"));OtherMotherLoop.changed(p);
                return "Scheduled "+scheduled+" eligible queued guests through physical arrival. Missing progression/banned Wybie are not fabricated.";
            }
            case "count"->{
                if(PlayerParty.isPlayerParty(s))return "Player Party roster is frozen with its per-guest gift book.";
                int count=NumberedDebugActions.integer(value,6,40);
                long present=guests(s).stream().filter(g->entity(p,g)!=null&&!g.getString("State").equals("GONE")).count();
                if(count<present)return "Capacity cannot be below the actual arrived guest count.";
                int order=guests(s).stream().mapToInt(g->g.getInt("Order")).max().orElse(-1)+1;
                while(guests(s).size()<count){add(n,"resident","Guest "+(order+1),null,order++);}
                n.putInt("Capacity",count);OtherMotherLoop.changed(p);return "Capacity set to "+count+"; existing actors and attendance retained.";
            }
            case "wander"->{
                for(var g:guests(s))if(Set.of("WANDERING","CONVERSING","APPROACHING_GROUP").contains(g.getString("State"))){g.putString("State","WANDERING");g.remove("Group");g.remove("Target");g.putLong("RestUntil",0);}
                OtherMotherLoop.changed(p);return "Available social guests will choose new valid room destinations.";
            }
            case "group"->{formGroup(p,s);OtherMotherLoop.changed(p);return "Conversation group requested using real nearby guests and reachable spots.";}
            default->{return "Unknown guest control.";}
        }
    }

    private PartyGuests(){}
}
