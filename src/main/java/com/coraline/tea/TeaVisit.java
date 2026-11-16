package com.coraline.tea;
import com.coraline.bobinsky.*;
import com.coraline.feast.*;
import com.coraline.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid="coraline")
public final class TeaVisit {
    public static final int KNOCK=-2,OPEN=-1,A=0,ENTER=1,B=2,C=3,SEAT=4,D=5,DRINK=6,E=7,SWIRL=8,F=9,G=10,H=11,GLASS=12,I=13,J=14,LEAVE=15;
    private static net.minecraft.server.MinecraftServer currentServer;
    private static final Set<UUID> recovered=new HashSet<>();
    public static String line(int phase){return switch(phase){case A->"a";case B->"b";case C->"c";case D->"d";case E->"e";case F->"f";case G->"g";case H->"h";case I->"i";case J->"j";default->"";};}
    public static boolean canEnter(CompoundTag v){if(MissingParentsTea.isVisit(v))return MissingParentsTea.canEnter(v);return v.getBoolean("Tea")&&v.getInt("Phase")>=ENTER&&!v.getBoolean("AwaitingRemoval");}
    public static boolean canDrink(ServerPlayer p,ItemStack stack){var v=BobinskyVisitData.get(p.server).teaVisits.get(p.getUUID());return v!=null&&v.getBoolean("Active")&&v.getInt("Phase")==DRINK&&v.getBoolean("Seated")&&p.getVehicle() instanceof TeaSeatEntity&&owned(stack,v);}
    public static boolean owned(ItemStack stack,CompoundTag v){return stack.hasTag()&&stack.getTag().hasUUID("TeaToken")&&v.getUUID("Token").equals(stack.getTag().getUUID("TeaToken"));}
    public static void drunk(ServerPlayer p){var v=BobinskyVisitData.get(p.server).teaVisits.get(p.getUUID());if(v!=null&&v.getInt("Phase")==DRINK){v.putBoolean("Drank",true);if(p.getVehicle() instanceof TeaSeatEntity seat)seat.policy(false,false);begin(p,v,E);}}
    public static void voiceFinished(ServerPlayer p,String sound){var v=BobinskyVisitData.get(p.server).teaVisits.get(p.getUUID());if(v!=null&&v.getBoolean("Active")&&sound.equals("tea_visit_"+line(v.getInt("Phase"))))v.putBoolean("VoiceDone",true);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        if(currentServer!=server){currentServer=server;recovered.clear();}var data=BobinskyVisitData.get(server);long now=server.overworld().getGameTime();
        for(var owner:List.copyOf(data.teaVisits.keySet())){
            var v=data.teaVisits.get(owner);if(!v.getBoolean("Active")||MissingParentsTea.isVisit(v))continue;var p=server.getPlayerList().getPlayer(owner);var s=VisitData.get(server).scenes.get(owner);
            if(FirstEscapeStory.engaged(s))continue;
            if(recovered.add(owner)||s==null||!s.door.equals(v.getUUID("Door"))){cancel(server,v,p);continue;}
            if(now%200==0)BobinskyHouse.hold(server.overworld(),BlockPos.of(v.getLong("House")));
            if(v.getInt("Phase")==KNOCK&&now>=v.getLong("Deadline")){cancel(server,v,p);continue;}
            if(p==null||!p.isAlive()||!p.level().dimension().equals(Level.OVERWORLD)){cancel(server,v,p);continue;}
            perform(p,s,v);if(now%20==0)data.setDirty();
        }
    }
    public static boolean offer(ServerPlayer p,VisitData.Scene s){
        if(BobinskyVisit.pending(p)||s.wybieActor!=null)return false;
        var center=BobinskyHouse.find(p,TeaHouse.RX,TeaHouse.RZ);if(center==null)return false;var v=new CompoundTag();v.putUUID("Owner",p.getUUID());v.putUUID("Door",s.door);v.putUUID("Token",UUID.randomUUID());v.putBoolean("Tea",true);v.putBoolean("DetailedTeaHouse",true);v.putBoolean("Active",true);v.putInt("Phase",KNOCK);v.putLong("House",center.asLong());v.put("Originals",BobinskyHouse.snapshot(p.serverLevel(),center,TeaHouse.RX,TeaHouse.RZ));v.putLong("Deadline",p.server.overworld().getGameTime()+6000);
        var data=BobinskyVisitData.get(p.server);data.teaVisits.put(p.getUUID(),v);recovered.add(p.getUUID());data.setDirty();p.server.overworld().getDataStorage().save();TeaHouse.build(p.serverLevel(),v);
        prompt(p,"Miss Spink and Miss Forcible invite you to tea. Knock at "+BobinskyHouse.door(v).toShortString()+".");return true;
    }
    public static void knock(ServerPlayer p,CompoundTag v){if(v.getInt("Phase")!=KNOCK||!v.getUUID("Owner").equals(p.getUUID())||p.distanceToSqr(Vec3.atCenterOf(BobinskyHouse.door(v)))>25)return;begin(p,v,OPEN);}
    private static void begin(ServerPlayer p,CompoundTag v,int phase){v.putInt("Phase",phase);v.putInt("Elapsed",0);v.putBoolean("VoiceDone",false);if(!line(phase).isEmpty())VisitAudio.play(p,"tea_visit_"+line(phase),false);BobinskyVisitData.get(p.server).setDirty();}
    private static TeaVisitorEntity actor(ServerPlayer p,CompoundTag v,String key){return v.hasUUID(key)&&p.server.overworld().getEntity(v.getUUID(key)) instanceof TeaVisitorEntity e?e:null;}
    private static void spawn(ServerPlayer p,CompoundTag v){var center=BlockPos.of(v.getLong("House"));for(String key:new String[]{"Spink","Forcible"}){var e=(key.equals("Spink")?ModEntities.SPINK_VISITOR:ModEntities.FORCIBLE_VISITOR).get().create(p.serverLevel());var at=key.equals("Spink")?BobinskyHouse.door(v).south():center.offset(v.getBoolean("DetailedTeaHouse")?-6:-3,0,2);e.setPos(Vec3.atBottomCenterOf(at));e.getPersistentData().putUUID("TeaOwner",p.getUUID());e.getPersistentData().putUUID("TeaToken",v.getUUID("Token"));e.setOnGround(true);face(e,p);p.serverLevel().addFreshEntity(e);v.putUUID(key,e.getUUID());}}
    public static void face(TeaVisitorEntity actor,net.minecraft.world.entity.Entity target){actor.getLookControl().setLookAt(target,30,30);float yaw=(float)Math.toDegrees(Math.atan2(target.getZ()-actor.getZ(),target.getX()-actor.getX()))-90;actor.setYRot(yaw);actor.yBodyRot=yaw;actor.setYHeadRot(yaw);actor.yRotO=yaw;actor.yBodyRotO=yaw;actor.yHeadRotO=yaw;}
    private static void walk(TeaVisitorEntity actor,Vec3 target,double speed){var delta=target.subtract(actor.position()).multiply(1,0,1);actor.getNavigation().stop();if(delta.lengthSqr()<.01)return;actor.setYRot((float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90);actor.yBodyRot=actor.getYRot();actor.move(net.minecraft.world.entity.MoverType.SELF,delta.normalize().scale(Math.min(speed,delta.length())));if(!actor.level().noCollision(actor,actor.getBoundingBox().move(0,-.06,0)))actor.setOnGround(true);}
    private static void perform(ServerPlayer p,VisitData.Scene s,CompoundTag v){
        if(v.getBoolean("Cancelled")){if(BobinskyHouse.unseen(p,v)){cleanup(p.server,v,p);v.putLong("RetryAt",p.server.overworld().getGameTime()+1200);}return;}
        int phase=v.getInt("Phase"),time=v.getInt("Elapsed");var center=BlockPos.of(v.getLong("House"));var spink=actor(p,v,"Spink");var forcible=actor(p,v,"Forcible");
        if(phase==OPEN){if(time>=40){setOpen(p,v,true);VisitAudio.play(p,"tea_visit_music",true);spawn(p,v);begin(p,v,A);}else v.putInt("Elapsed",time+1);return;}
        if(phase<A)return;
        if(spink==null||forcible==null){cancel(p.server,v,p);return;}spink.pose(phase,time);forcible.pose(phase,time);
        spink.cup(phase==SWIRL||phase==F||phase==G&&!v.getBoolean("CupTransferred"));forcible.cup(phase==SEAT||phase==G&&v.getBoolean("CupTransferred")||phase==H);
        if(phase==A||phase==ENTER){face(spink,p);face(forcible,spink);}
        if(phase==ENTER){walk(spink,new Vec3(center.getX()-1.5,center.getY(),BobinskyHouse.door(v).getZ()+1.5),.04);face(spink,p);if(BobinskyHouse.bounds(v).deflate(1).contains(p.position())){setOpen(p,v,false);begin(p,v,B);}return;}
        if(phase==DRINK)return;
        if(phase==LEAVE){if(!BobinskyHouse.bounds(v).inflate(.1).contains(p.position())){v.putBoolean("AwaitingRemoval",true);setOpen(p,v,false);VisitAudio.stopMusic(p,"tea_visit_music");if(BobinskyHouse.unseen(p,v)){cleanup(p.server,v,p);s.teaVisited=true;VisitProgression.refresh(p,s);VisitData.get(p.server).setDirty();}}return;}
        if(phase==C){if(time==0)prompt(p,"Follow Miss Spink inside.");var target=new Vec3(center.getX()-1.5,center.getY(),center.getZ()+2.5);if(spink.position().subtract(target).multiply(1,0,1).lengthSqr()>.04)walk(spink,Math.abs(spink.getX()-target.x)>.04?new Vec3(target.x,center.getY(),spink.getZ()):target,.04);else{spink.getNavigation().stop();forcible.getNavigation().stop();face(spink,forcible);face(forcible,spink);}}
        else if(phase==G){int step=v.getInt("CupWalkStep");Vec3 target=step==0?new Vec3(forcible.getX(),center.getY(),center.getZ()+2.6):step==1?new Vec3(spink.getX(),center.getY(),center.getZ()+2.6):spink.position().add(0,0,1.1);walk(forcible,target,.05);if(forcible.position().subtract(target).multiply(1,0,1).lengthSqr()<.04&&step<2)v.putInt("CupWalkStep",step+1);face(spink,forcible);face(forcible,spink);if(time>=10&&forcible.distanceToSqr(spink)<=2.25)v.putBoolean("CupTransferred",true);}
        else if(phase==H){spink.getNavigation().stop();forcible.getNavigation().stop();face(spink,forcible);face(forcible,spink);}
        else if(phase==J||phase==D){face(spink,p);face(forcible,p);}
        if(phase==SEAT){
            var chair=center.offset(1,0,1);var spinkPlace=new Vec3(center.getX()-2.5,center.getY(),center.getZ()+1.5);if(!spink.isPassenger())walk(spink,Math.abs(spink.getX()-spinkPlace.x)>.12?new Vec3(spinkPlace.x,center.getY(),spink.getZ()):spinkPlace,.04);
            if(!v.hasUUID("SpinkSeat")&&spink.position().subtract(spinkPlace).multiply(1,0,1).lengthSqr()<=1){var seat=ModEntities.TEA_SEAT.get().create(p.serverLevel());seat.setPos(spinkPlace.add(0,.6,0));seat.getPersistentData().putUUID("TeaOwner",p.getUUID());p.serverLevel().addFreshEntity(seat);spink.startRiding(seat,true);v.putUUID("SpinkSeat",seat.getUUID());face(spink,p);}
            if(!v.getBoolean("Seated")&&v.hasUUID("SpinkSeat")&&p.distanceToSqr(Vec3.atBottomCenterOf(chair))<=2.25){
                var seat=ModEntities.TEA_SEAT.get().create(p.serverLevel());seat.setPos(Vec3.atBottomCenterOf(chair).add(0,.6,0));seat.getPersistentData().putUUID("TeaOwner",p.getUUID());p.serverLevel().addFreshEntity(seat);p.startRiding(seat,true);v.putUUID("Seat",seat.getUUID());
                v.putBoolean("Seated",true);seat.policy(true,false);v.putInt("ServeElapsed",0);
            }
                if(v.getBoolean("Seated")){int serve=v.getInt("ServeElapsed");Vec3 target=Math.abs(forcible.getX()-p.getX())>.15?new Vec3(p.getX(),center.getY(),center.getZ()+2.5):p.position();walk(forcible,target,.055);face(forcible,p);if(forcible.distanceToSqr(p)<1.2){forcible.getNavigation().stop();var tea=new ItemStack(ModItems.JASMINE_TEA.get());tea.getOrCreateTag().putUUID("TeaToken",v.getUUID("Token"));tea.getOrCreateTag().putUUID("TeaOwner",p.getUUID());if(!p.getInventory().add(tea))p.drop(tea,false);v.putBoolean("TeaGiven",true);begin(p,v,D);}else v.putInt("ServeElapsed",serve+1);}else v.putInt("Elapsed",time+1);return;
        }
        if(phase==D||phase==DRINK||phase==E||phase==SWIRL||phase==F){face(forcible,spink);if(phase==D||phase==E)face(spink,p);}
        if(phase==SWIRL){if(time==0)removeCups(p,v);if(time>=60){begin(p,v,F);return;}v.putInt("Elapsed",time+1);return;}
        if(phase==GLASS){
            if(time==0){var bottle=ModEntities.TEA_GLASS.get().create(p.serverLevel());bottle.setPos(center.getX()-1.9,center.getY()+5.6,center.getZ()+2.9);bottle.getPersistentData().putUUID("TeaOwner",p.getUUID());bottle.getPersistentData().putUUID("TeaToken",v.getUUID("Token"));p.serverLevel().addFreshEntity(bottle);v.putUUID("Glass",bottle.getUUID());}
            var bottle=v.hasUUID("Glass")?p.serverLevel().getEntity(v.getUUID("Glass")):null;
            if(bottle==null||bottle.onGround()){if(bottle instanceof TeaGlassEntity glass)glass.shatter();var at=bottle==null?center.offset(-2,0,3):bottle.blockPosition();p.serverLevel().playSound(null,at,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,1);p.serverLevel().sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,new ItemStack(Items.GLASS)),at.getX()+.5,at.getY()+.15,at.getZ()+.5,30,.5,.05,.5,.03);begin(p,v,I);
            }else v.putInt("Elapsed",time+1);return;
        }
        String sound="tea_visit_"+line(phase);boolean done=(v.getBoolean("VoiceDone")||time>=VisitAudio.length(sound)+60)&&(phase!=G||time>=20);
        if(done){int next=switch(phase){case A->ENTER;case B->C;case C->SEAT;case D->DRINK;case E->SWIRL;case F->G;case G->H;case H->GLASS;case I->J;case J->LEAVE;default->phase+1;};begin(p,v,next);
            if(next==ENTER)prompt(p,"Enter the house.");if(next==SEAT)prompt(p,"Follow Miss Spink to the table and sit in the chair.");if(next==DRINK)prompt(p,"Drink your jasmine tea.");if(next==LEAVE){p.stopRiding();if(v.hasUUID("Seat")&&p.serverLevel().getEntity(v.getUUID("Seat"))!=null)p.serverLevel().getEntity(v.getUUID("Seat")).discard();setOpen(p,v,true);prompt(p,"Leave the house through the front door.");}
        }else v.putInt("Elapsed",time+1);
    }
    private static void setOpen(ServerPlayer p,CompoundTag v,boolean open){for(var pos:new BlockPos[]{BobinskyHouse.door(v),BobinskyHouse.door(v).above()}){var state=p.serverLevel().getBlockState(pos);if(state.hasProperty(DoorBlock.OPEN))p.serverLevel().setBlock(pos,state.setValue(DoorBlock.OPEN,open),2);}}
    private static void prompt(ServerPlayer p,String text){p.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));}
    private static void prop(ItemEntity item,CompoundTag v){item.getPersistentData().putUUID("SceneOwner",v.getUUID("Owner"));item.getPersistentData().putUUID("SceneToken",v.getUUID("Token"));item.getPersistentData().putBoolean("TeaProp",true);}
    private static void removeCups(ServerPlayer p,CompoundTag v){for(var stack:p.getInventory().items)if(owned(stack,v))stack.setCount(0);for(var stack:p.getInventory().offhand)if(owned(stack,v))stack.setCount(0);if(owned(p.containerMenu.getCarried(),v))p.containerMenu.setCarried(ItemStack.EMPTY);for(var item:p.server.overworld().getEntitiesOfClass(ItemEntity.class,BobinskyHouse.bounds(v).inflate(16)))if(owned(item.getItem(),v))item.discard();p.containerMenu.broadcastChanges();}
    public static void cancel(net.minecraft.server.MinecraftServer server,CompoundTag v,ServerPlayer p){boolean defer=BobinskyHouse.deferRestore(p,v);cleanup(server,v,p,!defer);if(defer){v.putBoolean("Active",true);v.putBoolean("Cancelled",true);v.putBoolean("AwaitingRemoval",true);v.putInt("Phase",LEAVE);BobinskyHouse.open(server.overworld(),v,false);}else v.putLong("RetryAt",server.overworld().getGameTime()+1200);BobinskyVisitData.get(server).setDirty();}
    private static void cleanup(net.minecraft.server.MinecraftServer server,CompoundTag v,ServerPlayer p){
        cleanup(server,v,p,true);
    }
    private static void cleanup(net.minecraft.server.MinecraftServer server,CompoundTag v,ServerPlayer p,boolean restore){
        var level=server.overworld();v.putBoolean("Active",false);
        if(p!=null){if(p.getVehicle() instanceof TeaSeatEntity seat)seat.policy(false,false);p.stopRiding();removeCups(p,v);VisitAudio.stopMusic(p,"tea_visit_music");if(!line(v.getInt("Phase")).isEmpty())VisitAudio.stopVoice(p,"tea_visit_"+line(v.getInt("Phase")));if(BobinskyHouse.bounds(v).contains(p.position())){var at=BobinskyHouse.front(v).north();p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);}}
        for(String key:new String[]{"Spink","Forcible","Seat","SpinkSeat","Glass"})if(v.hasUUID(key)&&level.getEntity(v.getUUID(key))!=null)level.getEntity(v.getUUID(key)).discard();for(var tag:v.getList("Shards",10)){var n=(CompoundTag)tag;if(level.getEntity(n.getUUID("Id"))!=null)level.getEntity(n.getUUID("Id")).discard();}
        if(restore)BobinskyHouse.restore(level,v);BobinskyVisitData.get(server).setDirty();
    }
}
