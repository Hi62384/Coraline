package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.*;
import com.coraline.secrets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Real online owner, authored first-floor slice, normal NPC goals and legal moves.
 * The checkpoint begins after the invitation; it is not whole-story acceptance.
 * Never seats NPCs, teleports them after acquisition, edits winners, or ACKs audio.
 */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FamilyGameNightNaturalVisualCheck {
    private static boolean requested;
    private static volatile boolean ready,stopping;
    private static int ticks,serial,acceptedMoves,lastTurn=-1,pullStarted=-1;
    private static java.util.UUID companion,mother,father;
    private static boolean sawSeats,sawMatch,sawResult;
    private static float arrivalTrust;
    private static String phase="";
    private static volatile String capture;

    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.familyGameNightNaturalCheck")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!requested){requested=true;mc.getSingleplayerServer().execute(()->{
            try{stage(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));ready=true;}
            catch(Throwable error){fail("stage",error);}
        });}
        var shot=capture;if(shot!=null){capture=null;try{
            var out=java.nio.file.Path.of("../artwork/family_natural_qa");java.nio.file.Files.createDirectories(out);
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(shot+".png"));}
        }catch(Exception error){fail("capture",error);}}
    }

    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void server(TickEvent.PlayerTickEvent event){
        if(!Boolean.getBoolean("coraline.familyGameNightNaturalCheck")||!ready||stopping
                ||event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        try{observe(p);}catch(Throwable error){fail("flow",error);}
    }
    private static void observe(ServerPlayer p){
        ticks++;var s=OtherMotherLoop.scene(p);require(s!=null,"Lost original scene");var n=s.loop.event;
        var w=p.serverLevel().getEntity(companion);require(w!=null&&!w.isRemoved(),"Lost original borrowed Wybie");
        if(n.getInt("Serial")!=serial){
            require(sawSeats&&sawMatch&&sawResult&&acceptedMoves>0,"Event advanced without natural seating and real match/result");
            require(p.getInventory().countItem(net.minecraft.world.item.Items.COOKIE)==2&&s.loop.trust>arrivalTrust,"Normal completed-match prize/trust not awarded exactly once");
            require(!w.getPersistentData().hasUUID("GameNightOwner")&&!w.getPersistentData().hasUUID("WybieCompanionToken"),"Completed game retained companion lease");
            require(w instanceof Mob actor&&actor.isNoAi()&&actor.isNoGravity()&&actor.noPhysics,"Borrowed actor flags not restored");
            System.out.println("FAMILY_GAME_NIGHT_NATURAL_NATIVE_COMPLETE natural original NPC seating, ordinary online owner, legal nonce moves="+acceptedMoves+", normally completed match, reward and lease cleanup; post-invitation checkpoint only");
            stopping=true;Minecraft.getInstance().execute(Minecraft.getInstance()::stop);return;
        }
        require(VisitEvents.type(s)==VisitEvents.Type.FAMILY_GAME_NIGHT,"Changed event without serial advance");
        String current=s.loop.phase.name();if(!phase.equals(current)){phase=current;System.out.println("FAMILY_GAME_NIGHT_NATURAL_PROGRESS phase="+phase+" ticks="+ticks+" Wybie="+w.position()+" vehicle="+w.getVehicle());}
        var wm=FamilyGameNight.wybie(p,s);require(wm==w,"Controller substituted original actor");
        var mom=MotherSearch.mother(p);var dad=VisitEvents.father(p,s);
        require(mom!=null&&dad!=null&&mom.getUUID().equals(mother)&&dad.getUUID().equals(father),"Parent identity changed");
        if(!sawSeats&&mom.getVehicle()==FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,1)
                &&dad.getVehicle()==FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,2)
                &&w.getVehicle()==FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,3)){
            sawSeats=true;capture="natural_seats";System.out.println("FAMILY_GAME_NIGHT_NATURAL_SEATS original parents and Wybie arrived through production goals");
        }
        if(ticks%100==0)System.out.println("FAMILY_GAME_NIGHT_NATURAL_NAV tick="+ticks+" at="+w.position()+" pathDone="+wm.getNavigation().isDone()+" collision="+w.horizontalCollision+" noAi="+wm.isNoAi()+" seat="+FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,3).position());
        if(ticks==650&&!sawSeats){
            var seat=FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,3);var range=wm.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);double original=range.getBaseValue();boolean duck=wm.partyDoorDuck();
            try{for(int reach:new int[]{16,64})for(boolean lowered:new boolean[]{false,true}){
                wm.getNavigation().stop();range.setBaseValue(reach);wm.partyDoorDuck(lowered);var path=wm.getNavigation().createPath(seat,0);
                System.out.println("FAMILY_GAME_NIGHT_FAILED_PATH_PROBE range="+reach+" duck="+lowered+" height="+wm.getBbHeight()+" canReach="+(path!=null&&path.canReach())+" end="+(path==null?"null":path.getEndNode())+" nodes="+(path==null?0:path.getNodeCount()));
            }}finally{range.setBaseValue(original);wm.partyDoorDuck(duck);wm.getNavigation().stop();}
            throw new IllegalStateException("Natural arrival stalled; failed-run path probes logged, no completion accepted. event="+n+" search="+s.loop.secrets.save());
        }
        if(s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_ACTIVE){
            require(sawSeats,"Match started before actual NPC seating");var rules=n.getCompound("Game");
            require(TabletopGames.participantCount(rules)==4,"Lost real fourth participant");
            if(!sawMatch){sawMatch=true;capture="active_"+TabletopGames.voice(TabletopGames.game(rules));}
            if(!rules.getBoolean("Done")&&rules.getInt("Physics")==0&&(rules.getInt("Turn")==0||TabletopGames.game(rules)==TabletopGames.Game.SILENT_CHARADES)&&ticks%5==0){
                int before=rules.getInt("ActionCount");String action=TabletopGames.ai(rules);
                if(action.startsWith("pull:")){
                    if(pullStarted<0){var parts=action.split(":");FamilyGameNight.choice(p,n.getUUID("GameMoveNonce"),"pull_begin:"+parts[1]+":"+parts[2]);pullStarted=ticks;}
                    else if(ticks-pullStarted>=40){FamilyGameNight.choice(p,n.getUUID("GameMoveNonce"),"pull_release");pullStarted=-1;}
                }else FamilyGameNight.choice(p,n.getUUID("GameMoveNonce"),action);
                if(rules.getInt("ActionCount")>before)acceptedMoves++;
            }
            if(rules.getInt("Turn")!=lastTurn){lastTurn=rules.getInt("Turn");System.out.println("FAMILY_GAME_NIGHT_NATURAL_TURN actor="+lastTurn+" actions="+rules.getInt("ActionCount"));}
        }
        if(s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_CHOICE){
            require(TabletopGames.completedNormally(n.getCompound("Game"))&&n.getInt("CompletedMatches")==1,"Timed-out/fabricated result");
            sawResult=true;FamilyGameNight.choice(p,n.getUUID("GameChoiceNonce"),"finish");
        }
        if(s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_FINISH)require(n.getBoolean("GameCompleted"),"Ordinary finished match lost completion");
        require(ticks<9000,"Checkpoint timed out without gameplay completion: "+phase);
    }

    private static void stage(ServerPlayer p)throws java.io.IOException{
        var level=p.server.getLevel(OtherWorld.KEY);var house=new BlockPos(35400,160,35400);
        // Import original compact mapping and palette; no invented corridors,
        // open-door override, cleared furniture, or replacement collision shell.
        try(var raw=FamilyGameNightNaturalVisualCheck.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");
            var zip=new java.util.zip.GZIPInputStream(java.util.Objects.requireNonNull(raw));
            var reader=new java.io.InputStreamReader(zip,java.nio.charset.StandardCharsets.UTF_8)){
            var authored=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=authored.getAsJsonArray("palette");
            for(var item:authored.getAsJsonArray("blocks")){
                var cell=item.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();if(y<4||y>13)continue;
                var entry=palette.get(cell.get(3).getAsInt()).getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(entry.get("id").getAsString()));
                var state=java.util.Objects.requireNonNull(block).defaultBlockState();for(var property:entry.getAsJsonObject("properties").entrySet())state=property(state,property.getKey(),property.getValue().getAsString());
                var at=house.offset(x-(x>18?1:0)-(x>19?1:0)-31,y-5,z-(z>46?1:0)-(z>47?1:0)-(z>61?1:0)-(z>62?1:0)-41);level.getChunkAt(at);level.setBlock(at,state,2);
            }
        }
        var door=new DoorData.Door();door.owner=p.getUUID();door.placed=true;door.pos=house;door.activeUntil=Long.MAX_VALUE;DoorData.get(p.server).doors.put(door.id,door);
        var realm=new RealmData.Realm();realm.door=door.id;realm.doorPos=HouseCabinets.at(house,14,6,40);realm.sourceDoor=house;realm.house=house;realm.porch=HouseCabinets.at(house,34,6,10);realm.table=HouseCabinets.at(house,43,6,36);realm.piano=HouseCabinets.at(house,20,6,60);realm.ready=realm.islandComplete=true;realm.radius=100;realm.parentGeneration=1;RealmData.get(p.server).realms.put(door.id,realm);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.stage=VisitData.Stage.DONE;s.table=realm.table;s.piano=realm.piano;s.loop.visits=2;s.loop.secrets.visit=2;s.loop.trust=.5F;s.loop.suspicion=.1F;s.loop.story.putBoolean("WybieIntroductionComplete",true);VisitData.get(p.server).scenes.put(s.owner,s);
        for(String key:java.util.List.of("CoralineInTunnel","CoralineReturning","CoralineWhiteoutStart","CoralineWhiteoutMoved"))p.getPersistentData().remove(key);
        p.getInventory().clearContent();p.getPersistentData().putUUID("CoralineVisitDoor",s.door);p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.teleportTo(level,realm.table.getX()+.5,realm.table.getY(),realm.table.getZ()-2.5,0,0);
        var mom=ModEntities.MOM.get().create(level);var dad=ModEntities.DAD.get().create(level);
        for(var actor:java.util.List.of(mom,dad)){actor.makeOtherParent(0);actor.getPersistentData().putUUID("RealmDoor",s.door);actor.getPersistentData().putInt("RealmGeneration",1);actor.setPos(Vec3.atBottomCenterOf(HouseCabinets.at(house,33,6,26)));require(level.addFreshEntity(actor),"Parent spawn refused");}
        mom.getPersistentData().putUUID("LoopOwner",p.getUUID());s.mom=mother=mom.getUUID();s.dad=father=dad.getUUID();
        var w=CharacterCast.ENTITIES.get("other_wybie").get().create(level);w.getPersistentData().putUUID("RealmDoor",s.door);w.getPersistentData().putBoolean("CoralineDecoration",true);w.setNoAi(true);w.setNoGravity(true);w.noPhysics=true;w.setPos(Vec3.atBottomCenterOf(realm.porch));require(level.addFreshEntity(w),"Borrowed Wybie spawn refused");companion=w.getUUID();
        VisitEvents.start(p,s,VisitEvents.Type.FAMILY_GAME_NIGHT);FamilyGameNight.beginWindow(p,s);serial=s.loop.event.getInt("Serial");
        var chair=FamilyGameNight.prop(p,s,ScenePropEntity.CHAIR,0);require(chair!=null&&FamilyGameNight.arrive(p,s,chair),"Owner normal arrival rejected");
        arrivalTrust=s.loop.trust;
        System.out.println("FAMILY_GAME_NIGHT_NATURAL_STARTED post-invitation original first-floor slice; original NPC goals and actual owner connection");
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(net.minecraft.world.level.block.state.BlockState state,String key,String value){
        @SuppressWarnings("unchecked") var property=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);
        return property==null?state:property.getValue(value).map(parsed->state.setValue(property,parsed)).orElse(state);
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static void fail(String stage,Throwable error){if(stopping)return;stopping=true;com.mojang.logging.LogUtils.getLogger().error("FAMILY_GAME_NIGHT_NATURAL_NATIVE_FAILED "+stage,error);Minecraft.getInstance().execute(Minecraft.getInstance()::stop);}
    private FamilyGameNightNaturalVisualCheck(){}
}
