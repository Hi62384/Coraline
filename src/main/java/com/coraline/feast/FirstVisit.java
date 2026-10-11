package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import java.util.*;
import static com.coraline.feast.VisitData.Stage.*;

/** Server-owned, per-player first-visit story. No copied blocks or inventories are replaced. */
@Mod.EventBusSubscriber(modid="coraline")
public final class FirstVisit {
    private static final Set<UUID> listening=new HashSet<>();
    private static final Map<UUID,Long> startupNext=new HashMap<>();
    private static final Map<UUID,String> startupProblems=new HashMap<>();
    public static VisitData.Scene active(ServerPlayer p){var s=VisitData.get(p.server).scenes.get(p.getUUID());var tag=p.getPersistentData();return s!=null&&s.stage!=DONE&&OtherWorld.isOther(p.level())&&!tag.getBoolean("CoralineInTunnel")&&tag.hasUUID("CoralineVisitDoor")&&s.door.equals(tag.getUUID("CoralineVisitDoor"))?s:null;}
    public static boolean locked(ServerPlayer p){return active(p)!=null;}
    public static boolean bedAllowed(ServerPlayer p){var s=active(p);return s==null||s.stage==BED;}
    public static String debugComplete(ServerPlayer p){
        if(!p.isCreative())return "First Visit Complete Debug is Creative-only.";
        var family=com.coraline.parents.ParentAccess.family(p.server,p.getUUID());
        if(family==null||family.deathPhase!=0)return "Claim living parents first.";
        FamilyVisitReset.validate(p.server,p.getUUID(),family.id);
        var data=VisitData.get(p.server);var s=data.scenes.get(p.getUUID());
        var door=s==null?null:DoorData.get(p.server).doors.get(s.door);
        if(door==null||door.revoked||!door.exists||!p.getUUID().equals(door.owner))door=DoorData.get(p.server).doors.values().stream()
            .filter(d->!d.revoked&&d.exists&&p.getUUID().equals(d.owner)).findFirst().orElse(null);
        if(door==null)return "Get your bound Little Door first.";
        if(s!=null&&!s.door.equals(door.id)&&ActivityWorld.protectedWorld(p.level()))return "Return home through your Little Door, then use this item with your current Little Door.";
        if(s==null){
            s=new VisitData.Scene();s.owner=p.getUUID();s.family=family.id;s.door=door.id;
            var realm=RealmData.get(p.server).realms.get(door.id);s.piano=s.table=realm==null?BlockPos.ZERO:realm.doorPos;
            s.stage=DONE;data.scenes.put(s.owner,s);
        }else if(s.stage!=DONE){
            // Change stage before dismounting so the supper-seat lock releases immediately.
            s.stage=DONE;cleanup(p,s);p.stopSleepInBed(true,false);p.getPersistentData().remove("CoralineWakeAt");
            s.elapsed=0;s.supperMusicEndsAt=0;s.welcomeAfter=0;VisitAudio.stop(p);forgetRuntime(p.getUUID());
        }
        // The next entry is a repeat visit and starts the main loop.
        if(!s.door.equals(door.id)){
            s.door=door.id;s.mom=null;s.dad=null;
            var realm=RealmData.get(p.server).realms.get(door.id);s.piano=s.table=realm==null?BlockPos.ZERO:realm.doorPos;
        }
        s.loop.visits=Math.max(1,s.loop.visits);
        s.welcomeAfter=0;
        OtherMotherLoop.resetForDebug(p,s);
        SceneAudio.skipFirstVisit(p,family.id);
        if(OtherWorld.isOther(p.level())){
            // Suppress the automatic same-visit migration. The Little Door remains
            // usable so the next real entry begins the repeat-visit loop.
            p.getPersistentData().putBoolean("CoralineDebugVisitCompleted",true);
        }
        data.setDirty();
        return OtherWorld.isOther(p.level())?
            "First visit marked complete. The Little Door is open for your return; re-enter to start the main loop.":
            "First visit marked complete. Your next Other World entry starts the main loop.";
    }
    public static void entered(ServerPlayer p,RealmData.Realm realm){
        var data=VisitData.get(p.server);
        var family=com.coraline.parents.ParentAccess.family(p.server,p.getUUID());
        FamilyVisitReset.validate(p.server,p.getUUID(),family==null?null:family.id);
        if(data.scenes.containsKey(p.getUUID()))return;
        var door=DoorData.get(p.server).doors.get(realm.door);if(door==null||!p.getUUID().equals(door.owner))return;
        if(!realm.ready||!OtherWorld.isOther(p.level()))return;
        long now=p.server.overworld().getGameTime();if(now<startupNext.getOrDefault(p.getUUID(),0L))return;
        startupNext.put(p.getUUID(),now+VisitConfig.STARTUP_RETRY.get());
        if(realm.bed==null){startupProblem(p,"The welcome is waiting for your real-world respawn bed.");return;}
        hold(p.serverLevel(),realm.doorPos);hold(p.serverLevel(),realm.bed);
        var parents=p.serverLevel().getEntitiesOfClass(ParentEntity.class,new AABB(realm.doorPos).inflate(realm.radius),e->e.isOtherParent()&&realm.door.equals(e.getPersistentData().hasUUID("RealmDoor")?e.getPersistentData().getUUID("RealmDoor"):null));
        var mom=parents.stream().filter(ParentEntity::isMom).findFirst().orElse(null);var dad=parents.stream().filter(e->!e.isMom()).findFirst().orElse(null);if(mom==null||dad==null){startupProblem(p,"Your Other Parents are arriving... the welcome will start shortly.");return;}
        mom.setOnGround(true);dad.setOnGround(true);
        ScenePlacement.prepareWalker(mom);ScenePlacement.prepareWalker(dad);
        BlockPos table=realm.table,piano=realm.piano;
        if(table==null||piano==null){
            var probe=ModEntities.MOM.get().create(p.serverLevel());if(probe==null)return;
            probe.setPos(Vec3.atBottomCenterOf(OtherWorld.safePlayer(p.serverLevel(),realm.doorPos)));probe.setOnGround(true);ScenePlacement.prepareWalker(probe);
            table=ScenePlacement.find(p.serverLevel(),realm.doorPos,3,26,false,probe,null);
            piano=ScenePlacement.find(p.serverLevel(),realm.doorPos,VisitConfig.PIANO_MIN.get(),Math.min(realm.radius-8,VisitConfig.PIANO_MAX.get()),true,probe,table);
            if(table!=null&&piano==null)piano=ScenePlacement.find(p.serverLevel(),realm.doorPos,8,Math.min(realm.radius-8,36),true,probe,table);
            if(table==null||piano==null){var fallback=ScenePlacement.terrace(p.serverLevel(),realm.doorPos,realm.radius);if(fallback!=null){table=fallback[0];piano=fallback[1];}else{startupProblem(p,"The perfected house is still being prepared.");return;}}
        }
        var s=new VisitData.Scene();s.owner=p.getUUID();s.family=family==null?null:family.id;s.door=realm.door;s.mom=mom.getUUID();s.dad=dad.getUUID();s.table=table;s.piano=piano;data.scenes.put(s.owner,s);data.setDirty();
        mom.getPersistentData().putUUID("VisitOwner",s.owner);dad.getPersistentData().putUUID("VisitOwner",s.owner);
        dad.teleportTo(piano.getX()+.5,piano.getY(),piano.getZ()+.5);
        var prop=spawn(p,s,ScenePropEntity.PIANO,piano.getX()+.5,piano.getY(),piano.getZ()+.5,0);dad.startRiding(prop,true);
        // Stage actors while the owner is still in the tunnel. Dialogue starts on actual arrival.
        if(!p.getPersistentData().getBoolean("CoralineInTunnel"))start(p,s,MOTHER_INTRO);
        startupNext.remove(p.getUUID());startupProblems.remove(p.getUUID());
    }
    public static boolean prepareEntrance(ServerPlayer p,RealmData.Realm realm){
        var door=DoorData.get(p.server).doors.get(realm.door);
        if(door==null||!p.getUUID().equals(door.owner))return true;
        entered(p,realm);
        var scene=VisitData.get(p.server).scenes.get(p.getUUID());
        if(scene==null)return false;
        if(scene.stage!=DONE){hold(p.serverLevel(),scene.piano);hold(p.serverLevel(),scene.table);}
        return OtherMotherLoop.prepare(p,realm);
    }
    private static void startupProblem(ServerPlayer p,String message){
        if(!message.equals(startupProblems.put(p.getUUID(),message)))com.mojang.logging.LogUtils.getLogger().warn("First visit for {} pending: {}",p.getGameProfile().getName(),message);
    }
    static ScenePropEntity spawn(ServerPlayer p,VisitData.Scene s,int kind,double x,double y,double z,int slot){
        var prop=ModEntities.SCENE_PROP.get().create(p.serverLevel());prop.owner=s.owner;prop.storyDoor=s.door;prop.kind(kind);prop.slot=slot;prop.setPos(x,y,z);p.serverLevel().getChunkAt(prop.blockPosition());p.serverLevel().addFreshEntity(prop);return prop;
    }
    private static List<ScenePropEntity> props(ServerPlayer p,VisitData.Scene s){return p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(s.table).inflate(VisitConfig.PIANO_MAX.get()+80),e->s.owner.equals(e.owner));}
    private static ParentEntity parent(ServerPlayer p,UUID id){return id!=null&&p.serverLevel().getEntity(id) instanceof ParentEntity e?e:null;}
    private static void hold(ServerLevel level,BlockPos pos){level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(pos),2,pos);level.getChunkAt(pos);}
    private static String audio(VisitData.Stage stage){return switch(stage){case MOTHER_INTRO->"other_mother_intro";case FATHER_ENTER->"other_father_enter_scene";case FATHER_INTRO->"other_father_intro";case SONG->"other_father_song";case DINNER_READY->"other_father_dinners_ready";case SUPPER->"mysterious_supper";case WAITING->"other_parents_waiting";case BED->"other_mother_visit_again";default->null;};}
    static void start(ServerPlayer p,VisitData.Scene s,VisitData.Stage stage){
        s.stage=stage;s.elapsed=0;VisitData.get(p.server).setDirty();listening.add(p.getUUID());String cue=audio(stage);if(cue!=null)VisitAudio.play(p,cue,stage==SONG||stage==SUPPER);
        var realm=RealmData.get(p.server).realms.get(s.door);
        if(realm!=null)OtherWorldEstate.lockStudy(p.serverLevel(),realm.house,
            stage==FATHER_ENTER||stage==FATHER_INTRO||stage==SONG||stage==DINNER_READY);
        if(stage==RETURN_TO_MOTHER){var dad=parent(p,s.dad);if(dad!=null){dad.stopRiding();dad.getNavigation().moveTo(s.table.getX()+2,s.table.getY(),s.table.getZ()+2,1);}}
        if(stage==SUPPER)s.supperMusicEndsAt=p.server.overworld().getGameTime()+VisitAudio.length("mysterious_supper")+10;
        if(stage==CAKE){VisitAudio.stopVoice(p);for(var prop:props(p,s))if(prop.kind()==ScenePropEntity.PLATE||prop.kind()==ScenePropEntity.GLASS)prop.discard();
            spawn(p,s,ScenePropEntity.CAKE,s.table.getX()+.5,s.table.getY()+.88,s.table.getZ()-.65,0);
        }
        if(stage==BED){p.stopRiding();for(var actor:new ParentEntity[]{parent(p,s.mom),parent(p,s.dad)})if(actor!=null)actor.stopRiding();hint(p,"It's time for bed. Follow the lights to your own bed.");}
        if(stage==SUPPER)hint(p,"Help yourself! Right-click plates and glasses to take food.");
    }
    static void table(ServerPlayer p,VisitData.Scene s){
        if(props(p,s).stream().anyMatch(e->e.kind()==ScenePropEntity.TABLE))return;
        spawn(p,s,ScenePropEntity.TABLE,s.table.getX()+.5,s.table.getY(),s.table.getZ()+.5,0);
        ensureChairs(p,s);
        for(int theme=0;theme<4;theme++){
            var plate=spawn(p,s,ScenePropEntity.PLATE,s.table.getX()+.5+(theme%2==0?-.98:.98),s.table.getY()+.88,s.table.getZ()+.5+(theme<2?-.32:.34),theme);
            for(int i=0;i<3;i++)plate.food(i,FeastContent.serving(p.level().random,theme));scheduleRefill(plate);
        }
        for(int i=0;i<3;i++){
            spawn(p,s,ScenePropEntity.PLATE,s.table.getX()+.5+(i==0?0:i==1?-.85:.85),s.table.getY()+.89,s.table.getZ()+.5+(i==0?-.94:.94),4+i);
            var glass=spawn(p,s,ScenePropEntity.GLASS,s.table.getX()+.5+(i==0?.55:i==1?-1.48:1.48),s.table.getY()+.88,s.table.getZ()+.5+(i==0?-.95:.94),i);
            glass.food(0,FeastContent.random(p.level().random,true));scheduleRefill(glass);
        }
    }
    private static void ensureChairs(ServerPlayer p,VisitData.Scene s){
        var existing=props(p,s);
        for(int slot=0;slot<3;slot++){
            final int wanted=slot;
            if(existing.stream().anyMatch(e->e.kind()==ScenePropEntity.CHAIR&&e.slot==wanted))continue;
            double x=s.table.getX()+(slot==0?.5:slot==1?-.4:1.4),z=s.table.getZ()+(slot==0?-1.65:2.15);
            spawn(p,s,ScenePropEntity.CHAIR,x,s.table.getY(),z,slot).setYRot(slot==0?0:180);
        }
    }
    private static void scheduleRefill(ScenePropEntity plate){plate.getPersistentData().putLong("NextServingRefill",plate.level().getGameTime()+VisitConfig.REFILL_TICKS.get()+plate.level().random.nextInt(VisitConfig.REFILL_JITTER.get()+1));}
    private static void refill(ScenePropEntity plate){
        if(plate.kind()==ScenePropEntity.PLATE&&plate.slot>=4)return; // Personal place settings stay empty until used.
        if(plate.level().getGameTime()<plate.getPersistentData().getLong("NextServingRefill"))return;
        for(int i=0;i<(plate.kind()==ScenePropEntity.GLASS?1:3);i++)if(plate.food(i).isEmpty()){
            plate.food(i,plate.kind()==ScenePropEntity.GLASS?FeastContent.random(plate.level().random,true):FeastContent.serving(plate.level().random,plate.slot));break;
        }
        scheduleRefill(plate);
    }
    public static InteractionResult interact(ServerPlayer p,ScenePropEntity prop){
        var s=active(p);if(s==null||!p.getUUID().equals(prop.owner))return InteractionResult.FAIL;
        if(prop.kind()==ScenePropEntity.CHAIR){
            if(prop.slot!=0){hint(p,"That chair is for your Other Parent. Your chair is on the opposite side.");return InteractionResult.CONSUME;}
            if(s.stage==SIT||s.stage==RETURN_TO_MOTHER){
                if(p.getVehicle()==prop||(!prop.isVehicle()&&p.startRiding(prop,true))){start(p,s,SUPPER);return InteractionResult.SUCCESS;}
                hint(p,"Your chair is occupied.");return InteractionResult.FAIL;
            }
        }
        if(prop.kind()==ScenePropEntity.CAKE&&(s.stage==WAITING||s.stage==BED)&&!s.cakeTaken){
            var cake=WelcomeCakeBlock.stack(0);if(!p.getInventory().add(cake)){hint(p,"Make room for your welcome cake.");return InteractionResult.FAIL;}s.cakeTaken=true;VisitData.get(p.server).setDirty();prop.discard();return InteractionResult.SUCCESS;
        }
        if((prop.kind()==ScenePropEntity.PLATE||prop.kind()==ScenePropEntity.GLASS)&&s.stage==SUPPER){
            ItemStack held=p.getMainHandItem();boolean empty=true;for(int i=0;i<3;i++)empty&=prop.food(i).isEmpty();
            if(empty&&held.isEdible()&&FeastContent.drink(held)==(prop.kind()==ScenePropEntity.GLASS)){prop.food(0,held.split(1));return InteractionResult.SUCCESS;}
            for(int i=2;i>=0;i--)if(!prop.food(i).isEmpty()){
                var portion=prop.food(i).copy();portion.setCount(1);if(!p.getInventory().add(portion)){hint(p,"Your inventory is full.");return InteractionResult.FAIL;}
                var remaining=prop.food(i).copy();remaining.shrink(1);prop.food(i,remaining);return InteractionResult.SUCCESS;
            }
        }return InteractionResult.CONSUME;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        if(!p.isAlive())return;
        if(OtherWorld.isOther(p.level())&&!p.getPersistentData().getBoolean("CoralineInTunnel")&&p.getPersistentData().hasUUID("CoralineVisitDoor")&&!VisitData.get(p.server).scenes.containsKey(p.getUUID())){
            var pending=RealmData.get(p.server).realms.get(p.getPersistentData().getUUID("CoralineVisitDoor"));if(pending!=null)entered(p,pending);
        }
        var s=active(p);if(s==null)return;
        if(p.tickCount%40==0)VisitGuide.trail(p,s);
        if(!p.isAlive())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var door=DoorData.get(p.server).doors.get(s.door);
        if(realm==null||door==null||door.revoked||com.coraline.parents.ParentAccess.family(p.server,p.getUUID())==null){abort(p,s,"The welcome has ended. You have returned home.");return;}
        if(realm.bed==null||!(p.serverLevel().getBlockState(realm.bed).getBlock() instanceof net.minecraft.world.level.block.BedBlock)){abort(p,s,"Your copied bed is missing. Returning you home safely.");return;}
        if(p.tickCount%20==0){
            hold(p.serverLevel(),s.piano);hold(p.serverLevel(),s.table);VisitData.get(p.server).setDirty();
            OtherWorldEstate.lockStudy(p.serverLevel(),realm.house,
                s.stage==FATHER_ENTER||s.stage==FATHER_INTRO||s.stage==SONG||s.stage==DINNER_READY);
            if(s.stage.ordinal()>=RETURN_TO_MOTHER.ordinal()&&s.stage.ordinal()<=WAITING.ordinal())ensureChairs(p,s);
        }
        if(!listening.contains(p.getUUID())){
            listening.add(p.getUUID());String cue=audio(s.stage);
            if(cue!=null){VisitAudio.play(p,cue,s.stage==SONG||s.stage==SUPPER);if(s.stage!=SUPPER)s.elapsed=0;else s.supperMusicEndsAt=p.server.overworld().getGameTime()+VisitAudio.length("mysterious_supper")+10;}
        }
        s.elapsed++;
        var mom=parent(p,s.mom);var dad=parent(p,s.dad);
        if(mom==null||dad==null){if(s.elapsed>100)abort(p,s,"A parent is missing from the welcome. Returning you home safely.");return;}
        if(p.tickCount%20==0&&s.stage.ordinal()<RETURN_TO_MOTHER.ordinal()&&!(dad.getVehicle() instanceof ScenePropEntity)){
            var piano=props(p,s).stream().filter(prop->prop.kind()==ScenePropEntity.PIANO).findFirst().orElseGet(()->spawn(p,s,ScenePropEntity.PIANO,s.piano.getX()+.5,s.piano.getY(),s.piano.getZ()+.5,0));dad.startRiding(piano,true);
        }
        mom.setGlowingTag(s.stage==RETURN_TO_MOTHER);dad.setGlowingTag(s.stage==FIND_FATHER);
        if(s.stage==SUPPER||s.stage==CAKE||s.stage==WAITING){
            if(!(p.getVehicle() instanceof ScenePropEntity seat&&seat.kind()==ScenePropEntity.CHAIR&&seat.slot==0&&p.getUUID().equals(seat.owner))){
                var chair=props(p,s).stream().filter(prop->prop.kind()==ScenePropEntity.CHAIR&&prop.slot==0).findFirst().orElseGet(()->spawn(p,s,ScenePropEntity.CHAIR,s.table.getX()+.5,s.table.getY(),s.table.getZ()-1.65,0));p.startRiding(chair,true);
            }
        }
        switch(s.stage){
            case MOTHER_INTRO->{if(s.elapsed>=VisitAudio.length("other_mother_intro"))start(p,s,FIND_FATHER);}
            case FIND_FATHER->{if(p.tickCount%60==0)hint(p,"Find your Other Father — follow his glowing outline.");
                boolean insideStudy=realm.house==null||p.getX()<realm.house.getX()-2&&p.getZ()<realm.house.getZ()-2;
                if(insideStudy&&p.distanceToSqr(s.piano.getCenter())<Math.pow(VisitConfig.PIANO_RANGE.get()-1,2))start(p,s,FATHER_ENTER);}
            case FATHER_ENTER->{if(s.elapsed>=VisitAudio.length("other_father_enter_scene"))start(p,s,FATHER_INTRO);}
            case FATHER_INTRO->{if(s.elapsed>=VisitAudio.length("other_father_intro"))start(p,s,SONG);}
            case SONG->{if(s.elapsed>=VisitAudio.length("other_father_song"))start(p,s,DINNER_READY);}
            case DINNER_READY->{if(s.elapsed>=VisitAudio.length("other_father_dinners_ready")){table(p,s);start(p,s,RETURN_TO_MOTHER);}}
            case RETURN_TO_MOTHER->{if(p.tickCount%60==0)hint(p,"Dinner is ready. Return to your Other Mother.");if(p.distanceToSqr(s.table.getCenter())<64)start(p,s,SIT);}
            case SIT->{if(p.tickCount%60==0){hint(p,"Right-click the chair to sit down for supper.");marker(p,s.table.offset(0,0,-2));}}
            case SUPPER->{
                if(s.elapsed<200&&s.elapsed%20==0)hint(p,"Right-click plates and glasses to take food.");
                if(s.elapsed==200)hint(p,"");
                String cue=SupperDialogue.cueAt(s.elapsed);if(cue!=null)VisitAudio.play(p,cue,false);
                if(s.elapsed%20==0)for(var prop:props(p,s))if(prop.kind()==ScenePropEntity.PLATE||prop.kind()==ScenePropEntity.GLASS)refill(prop);
                if(s.elapsed>=SupperDialogue.endTick())start(p,s,CAKE);
            }
            case CAKE->{if(s.elapsed>=VisitConfig.CAKE_WAIT.get()){start(p,s,WAITING);hint(p,"Welcome home. Right-click your cake to take it.");}}
            case WAITING->{if(s.elapsed>=VisitAudio.length("other_parents_waiting"))start(p,s,BED);}
            case BED->{if(p.tickCount%20==0){marker(p,realm.bed);hint(p,"Sleep in your own bed to return home.");}}
            default->{}
        }
    }
    public static boolean control(ParentEntity parent){
        if(!parent.getPersistentData().hasUUID("VisitOwner"))return false;
        var owner=parent.getServer().getPlayerList().getPlayer(parent.getPersistentData().getUUID("VisitOwner"));
        var s=VisitData.get(parent.getServer()).scenes.get(parent.getPersistentData().getUUID("VisitOwner"));if(s==null||s.stage==DONE){parent.getPersistentData().remove("VisitOwner");parent.setGlowingTag(false);return false;}
        if(owner==null||active(owner)==null){parent.getNavigation().stop();return true;}
        if(s.stage.ordinal()>=RETURN_TO_MOTHER.ordinal()&&s.stage.ordinal()<=WAITING.ordinal()){
            var chair=props(owner,s).stream().filter(e->e.kind()==ScenePropEntity.CHAIR&&e.slot==(parent.isMom()?1:2)).findFirst().orElse(null);
            if(chair!=null){
                SupperSeating.approach(parent,chair);
                parent.lookAtStoryPlayer(owner);return true;
            }
        }
        boolean follow=parent.isMom()&&(s.stage==MOTHER_INTRO||s.stage==FIND_FATHER)||s.stage==BED;
        Vec3 target=follow?owner.position():s.table.getCenter().add(parent.isMom()?-2:2,-.5,1);
        if(!parent.isMom()&&s.stage.ordinal()<RETURN_TO_MOTHER.ordinal()){parent.getNavigation().stop();}
        else if(parent.tickCount%10==0){if(parent.distanceToSqr(target)>4)parent.getNavigation().moveTo(target.x,target.y,target.z,1);else parent.getNavigation().stop();}
        parent.lookAtStoryPlayer(owner);
        if(!parent.isMom()&&parent.getVehicle() instanceof ScenePropEntity prop&&prop.mode()>0){parent.setYHeadRot(prop.getYRot());parent.setXRot(15);}
        return true;
    }
    public static void slept(ServerPlayer p){var s=active(p);if(s==null||s.stage!=BED)return;
        s.stage=DONE;VisitData.get(p.server).setDirty();var d=DoorData.get(p.server).doors.get(s.door);var source=d==null?null:com.coraline.parents.FamilyData.level(p.server,d.dimension);
        if(source!=null){source.setDayTime((Math.floorDiv(source.getDayTime(),24000)+1)*24000);d.activeUntil=0;DoorData.get(p.server).setDirty();var state=source.getBlockState(d.pos);if(state.is(ModBlocks.TINY_DOOR.get()))source.setBlock(d.pos,state.setValue(com.coraline.block.TinyDoorBlock.OPEN,false).setValue(com.coraline.block.TinyDoorBlock.PORTAL,false),3);}
        cleanup(p,s);VisitAudio.stop(p);listening.remove(p.getUUID());
    }
    private static void abort(ServerPlayer p,VisitData.Scene s,String message){s.stage=DONE;cleanup(p,s);VisitData.get(p.server).scenes.remove(p.getUUID());VisitData.get(p.server).setDirty();listening.remove(p.getUUID());VisitAudio.stop(p);OtherWorld.leave(p,true);hint(p,message);}
    /** Escape is a suspension, not completion: keep the quest and earned rewards intact. */
    public static void recoverHome(ServerPlayer p){
        p.getPersistentData().putBoolean("CoralineRecovering",true);
        try{VisitAudio.stop(p);listening.remove(p.getUUID());p.stopRiding();p.stopSleepInBed(true,false);p.getPersistentData().remove("CoralineWakeAt");OtherWorld.leave(p,true);}
        finally{p.getPersistentData().remove("CoralineRecovering");}
        p.sendSystemMessage(Component.literal("Returned home safely. Your first-visit progress is saved."));
    }
    static void forgetRuntime(UUID owner){listening.remove(owner);startupNext.remove(owner);startupProblems.remove(owner);}
    private static void cleanup(ServerPlayer p,VisitData.Scene s){
        var realm=RealmData.get(p.server).realms.get(s.door);
        if(OtherWorld.isOther(p.level())&&realm!=null)OtherWorldEstate.lockStudy(p.serverLevel(),realm.house,false);
        p.stopRiding();for(var prop:props(p,s)){prop.ejectPassengers();prop.discard();}for(var parent:new ParentEntity[]{parent(p,s.mom),parent(p,s.dad)})if(parent!=null){parent.stopRiding();parent.setGlowingTag(false);parent.getPersistentData().remove("VisitOwner");}
    }
    private static void hint(ServerPlayer p,String message){p.displayClientMessage(Component.literal(message),true);}
    private static void marker(ServerPlayer p,BlockPos pos){p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,8,.5,.4,.5,.01);}
    @SubscribeEvent public static void mount(net.minecraftforge.event.entity.EntityMountEvent e){if(e.isDismounting()&&e.getEntityMounting() instanceof ServerPlayer p&&!p.getPersistentData().getBoolean("CoralineRecovering")){var s=active(p);if(s!=null&&(s.stage==SUPPER||s.stage==CAKE||s.stage==WAITING)&&p.isAlive()&&p.getVehicle() instanceof ScenePropEntity seat&&seat.kind()==ScenePropEntity.CHAIR&&seat.slot==0&&p.getUUID().equals(seat.owner))e.setCanceled(true);}}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){listening.remove(e.getEntity().getUUID());startupNext.remove(e.getEntity().getUUID());startupProblems.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void respawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e){listening.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){listening.clear();startupNext.clear();startupProblems.clear();}
}
