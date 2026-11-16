package com.coraline.bobinsky;
import com.coraline.feast.*;
import com.coraline.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid="coraline")
public final class BobinskyVisit {
    public static final int CHEESE=-2,KNOCK=-1,A=0,B=1,C=2,WAIT_D=3,D=4,VANISH=5,E=6,F=7,G=8,WAIT_H=9,H=10,I=11,LAND=12,J=13,K=14,WAIT_HIDE=15;
    private static final Set<UUID> recovered=new HashSet<>();
    private static net.minecraft.server.MinecraftServer currentServer;
    public static String line(int phase){return switch(phase){case A->"a";case B->"b";case C->"c";case D->"d";case E->"e";case F->"f";case G->"g";case H->"h";case I->"i";case J->"j";case K->"k";default->"";};}
    public static boolean pending(ServerPlayer p){var d=BobinskyVisitData.get(p.server);var v=d.visits.get(p.getUUID());var tea=d.teaVisits.get(p.getUUID());return v!=null&&v.getBoolean("Active")||tea!=null&&tea.getBoolean("Active");}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        if(currentServer!=server){currentServer=server;recovered.clear();}
        var data=BobinskyVisitData.get(server);long now=server.overworld().getGameTime();
        for(var owner:List.copyOf(data.visits.keySet())){
            var v=data.visits.get(owner);var p=server.getPlayerList().getPlayer(owner);var scene=VisitData.get(server).scenes.get(owner);
            if(FirstEscapeStory.engaged(scene)){v.putBoolean("Paused",true);continue;}
            if(recovered.add(owner)&&v.getBoolean("Active")&&v.contains("Originals")){cancel(server,v,p);continue;}
            if(!v.getBoolean("Active"))continue;
            if(scene==null||!v.getUUID("Door").equals(scene.door)){cancel(server,v,p);continue;}
            if(v.contains("House")&&now%200==0)BobinskyHouse.hold(server.overworld(),BlockPos.of(v.getLong("House")));
            int phase=v.getInt("Phase");
            if(phase==CHEESE&&v.hasUUID("Cheese")&&!v.contains("Parcel")){cancel(server,v,p);continue;}
            if((phase==CHEESE&&!v.getBoolean("ParcelCollected")||phase==KNOCK)&&now>=v.getLong("Deadline")){cancel(server,v,p);continue;}
            if(p==null||!p.isAlive()||!p.level().dimension().equals(Level.OVERWORLD)){
                if(p!=null&&!v.getBoolean("Paused")){VisitAudio.stopMusic(p,"bobinsky_visit_music");if(!line(phase).isEmpty())VisitAudio.stopVoice(p,"bobinsky_visit_"+line(phase));}v.putBoolean("Paused",true);continue;
            }
            if(v.getBoolean("Cancelled")){if(BobinskyHouse.unseen(p,v)){BobinskyHouse.restore(server.overworld(),v);v.putBoolean("Active",false);v.putLong("RetryAt",now+1200);data.setDirty();}continue;}
            if(phase>=A){
                var actor=actor(server,v);if(actor==null){cancel(server,v,p);continue;}
                if(v.getBoolean("Paused")){v.putBoolean("Paused",false);v.putInt("Elapsed",0);v.putBoolean("VoiceDone",false);VisitAudio.play(p,"bobinsky_visit_music",true);if(!line(phase).isEmpty())VisitAudio.play(p,"bobinsky_visit_"+line(phase),false);}
                perform(p,scene,v,actor);
            }
            if(now%20==0)data.setDirty();
        }
    }
    public static void offer(ServerPlayer p,VisitData.Scene scene){
        if(pending(p)||scene.wybieActor!=null)return;
        Vec3 drop=null;double start=Math.toRadians(p.getYRot()+90);
        for(int attempt=0;attempt<32&&drop==null;attempt++){
            double angle=start+attempt*Math.PI/8, radius=2+attempt%3;
            var base=BlockPos.containing(p.getX()+Math.cos(angle)*radius,p.getY(),p.getZ()+Math.sin(angle)*radius);
            for(int dy=2;dy>=-2;dy--){var feet=base.offset(0,dy,0);var level=p.serverLevel();var at=Vec3.atBottomCenterOf(feet);if(!level.hasChunkAt(feet)||!level.getWorldBorder().isWithinBounds(feet)||!level.getBlockState(feet).isAir()||!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)||BobinskyHouseProtection.house(level,feet)!=null||Math.max(Math.abs(at.x-p.getX()),Math.abs(at.z-p.getZ()))<1.7)continue;drop=at.add(0,.05,0);break;}
        }
        if(drop==null)return;
        var data=BobinskyVisitData.get(p.server);var v=new CompoundTag();v.putUUID("Owner",p.getUUID());v.putUUID("Door",scene.door);v.putUUID("Token",UUID.randomUUID());v.putInt("Phase",CHEESE);v.putBoolean("Active",true);v.putLong("Deadline",p.server.overworld().getGameTime()+6000);
        var stack=new ItemStack(ModItems.CHEESE_PACKAGE.get());stack.getOrCreateTag().putUUID("BobinskyOwner",p.getUUID());stack.getOrCreateTag().putUUID("BobinskyToken",v.getUUID("Token"));
        var pos=BlockPos.containing(drop);v.putLong("Parcel",pos.asLong());v.put("ParcelOriginal",net.minecraft.nbt.NbtUtils.writeBlockState(p.serverLevel().getBlockState(pos)));data.visits.put(p.getUUID(),v);recovered.add(p.getUUID());data.setDirty();p.server.overworld().getDataStorage().save();p.serverLevel().setBlock(pos,ModBlocks.CHEESE_PARCEL.get().defaultBlockState(),2);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("A cheese package appeared nearby. Right-click it to pick it up."));
    }
    public static void takeParcel(ServerPlayer p,BlockPos pos){var v=BobinskyVisitData.get(p.server).visits.get(p.getUUID());if(v==null||!v.getBoolean("Active")||v.getInt("Phase")!=CHEESE||!v.contains("Parcel")||v.getLong("Parcel")!=pos.asLong()||p.distanceToSqr(Vec3.atCenterOf(pos))>36||!p.serverLevel().getBlockState(pos).is(ModBlocks.CHEESE_PARCEL.get()))return;
        var stack=new ItemStack(ModItems.CHEESE_PACKAGE.get());stack.getOrCreateTag().putUUID("BobinskyOwner",p.getUUID());stack.getOrCreateTag().putUUID("BobinskyToken",v.getUUID("Token"));
        // Inventory.add consumes its argument. Keep the tagged pickup receipt so
        // quest validation does not depend on an emptied transfer stack.
        var receipt=stack.copy();if(p.getInventory().getFreeSlot()<0||!p.getInventory().add(stack)){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Make room in your inventory to pick up the cheese package."));return;}
        removeParcel(p.server,v);v.putBoolean("ParcelCollected",true);v.putLong("Deadline",p.server.overworld().getGameTime()+6000);BobinskyVisitData.get(p.server).setDirty();p.containerMenu.broadcastChanges();pickedUp(p,receipt);}
    private static void removeParcel(net.minecraft.server.MinecraftServer server,CompoundTag v){if(v.contains("Parcel")){var pos=BlockPos.of(v.getLong("Parcel"));var l=server.overworld();if(l.getBlockState(pos).is(ModBlocks.CHEESE_PARCEL.get()))l.setBlock(pos,v.contains("ParcelOriginal")?net.minecraft.nbt.NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),v.getCompound("ParcelOriginal")):net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);v.remove("Parcel");v.remove("ParcelOriginal");}}
    public static void pickedUp(ServerPlayer p,ItemStack stack){
        var v=BobinskyVisitData.get(p.server).visits.get(p.getUUID());var tag=stack.getTag();
        if(v==null||!v.getBoolean("Active")||tag==null||!tag.hasUUID("BobinskyToken")||!v.getUUID("Token").equals(tag.getUUID("BobinskyToken"))){stack.setCount(0);return;}
        if(v.getInt("Phase")!=CHEESE)return;
        var scene=VisitData.get(p.server).scenes.get(p.getUUID());if(scene==null||scene.wybieActor!=null)return;
        long now=p.server.overworld().getGameTime();if(now<v.getLong("HouseSearchAt"))return;
        var center=BobinskyHouse.find(p);if(center==null){
            // A bedroom, steep terrain or protected builds may have no safe site.
            // Retain the parcel and active offer; inventoryTick retries outdoors.
            v.putLong("HouseSearchAt",now+100);
            if(!v.getBoolean("SiteHintSent")){v.putBoolean("SiteHintSent",true);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("You picked up the cheese package. Carry it outside to open ground nearby so Bobinsky's house can appear."));}
            BobinskyVisitData.get(p.server).setDirty();return;
        }
        v.remove("HouseSearchAt");v.remove("SiteHintSent");
        v.putLong("House",center.asLong());v.put("Originals",BobinskyHouse.snapshot(p.serverLevel(),center));v.putInt("Phase",KNOCK);v.putLong("Deadline",p.server.overworld().getGameTime()+6000);
        var data=BobinskyVisitData.get(p.server);data.setDirty();p.server.overworld().getDataStorage().save();
        BobinskyHouse.build(p.serverLevel(),v);
        var beet=new ItemEntity(p.serverLevel(),center.getX()-1.5,center.getY()+1.2,center.getZ()+2.5,new ItemStack(Items.BEETROOT));beet.getPersistentData().putUUID("SceneOwner",p.getUUID());beet.getPersistentData().putUUID("SceneToken",v.getUUID("Token"));beet.setNoGravity(true);beet.setDeltaMovement(Vec3.ZERO);beet.setPickUpDelay(32767);beet.setInvulnerable(true);if(p.serverLevel().addFreshEntity(beet))v.putUUID("HouseBeet",beet.getUUID());
        data.setDirty();VisitAudio.play(p,"bobinsky_visit_music",true);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Take the cheese to Bobinsky. Knock on his front door at "+BobinskyHouse.door(v).toShortString()+"."));
    }
    public static void knock(ServerPlayer p,CompoundTag v){
        if(v.getInt("Phase")!=KNOCK||!v.getUUID("Owner").equals(p.getUUID())||p.distanceToSqr(Vec3.atCenterOf(BobinskyHouse.door(v)))>25)return;
        var actor=ModEntities.BOBINSKY_VISITOR.get().create(p.serverLevel());if(actor==null)return;
        behind(p,actor);actor.getPersistentData().putUUID("BobinskyOwner",p.getUUID());actor.getPersistentData().putUUID("BobinskyToken",v.getUUID("Token"));
        if(p.serverLevel().addFreshEntity(actor)){v.putUUID("Actor",actor.getUUID());begin(p,v,A);}
    }
    private static void behind(ServerPlayer p,BobinskyVisitorEntity actor){var look=Vec3.directionFromRotation(0,p.getYRot());var at=p.position().subtract(look.scale(2.8));actor.moveTo(at.x,p.getY(),at.z,p.getYRot(),0);actor.setNoGravity(false);com.coraline.wybie.VisitGrounding.lift(actor);actor.setInvisible(false);actor.setOnGround(true);}
    public static BobinskyVisitorEntity actor(net.minecraft.server.MinecraftServer server,CompoundTag v){return v.hasUUID("Actor")&&server.overworld().getEntity(v.getUUID("Actor")) instanceof BobinskyVisitorEntity b?b:null;}
    public static void voiceFinished(ServerPlayer p,String sound){var v=BobinskyVisitData.get(p.server).visits.get(p.getUUID());if(v!=null&&v.getBoolean("Active")&&sound.equals("bobinsky_visit_"+line(v.getInt("Phase"))))v.putBoolean("VoiceDone",true);}
    private static void begin(ServerPlayer p,CompoundTag v,int phase){v.putInt("Phase",phase);v.putInt("Elapsed",0);v.putBoolean("VoiceDone",false);if(phase==G||phase==K){v.putBoolean("Inside",false);v.putBoolean("Entering",false);v.remove("ReturnTicks");v.remove("ReturnStalled");v.remove("ReturnBestDistance");v.remove("ReturnEntering");}if(!line(phase).isEmpty())VisitAudio.play(p,"bobinsky_visit_"+line(phase),false);BobinskyVisitData.get(p.server).setDirty();}
    static void perform(ServerPlayer p,VisitData.Scene scene,CompoundTag v,BobinskyVisitorEntity actor){
        int phase=v.getInt("Phase"),time=v.getInt("Elapsed");actor.pose(phase,time);var front=BobinskyHouse.front(v);var home=BlockPos.of(v.getLong("House"));
        if(phase==WAIT_HIDE){if(BobinskyHouse.unseen(p,v)){actor.discard();removeHouseBeet(p.server,v);BobinskyHouse.restore(p.server.overworld(),v);v.putBoolean("Active",false);scene.bobinskyVisited=true;VisitProgression.refresh(p,scene);VisitData.get(p.server).setDirty();BobinskyVisitData.get(p.server).setDirty();}return;}
        if(phase!=B&&phase!=D&&phase!=H&&phase!=I&&phase!=VANISH){actor.getLookControl().setLookAt(p,30,30);if(actor.getNavigation().isDone()){actor.setYRot((float)Math.toDegrees(Math.atan2(p.getZ()-actor.getZ(),p.getX()-actor.getX()))-90);actor.yBodyRot=actor.getYRot();}}
        if(phase==B){actor.getNavigation().stop();actor.setYHeadRot(actor.getYRot());actor.setXRot(18);}
        if(phase==B&&time==0){removeCheese(p,v);actor.getNavigation().stop();}
        if(phase==D){actor.setNoGravity(true);actor.getNavigation().stop();if(time==0){v.putDouble("AcroX",actor.getX());v.putDouble("AcroY",actor.getY());v.putDouble("AcroZ",actor.getZ());}double progress=Math.min(1,time/(double)Math.max(1,VisitAudio.length("bobinsky_visit_d")));double slide=progress<.2?0:progress<.45?(progress-.2)/.25:progress<.8?1:1-(progress-.8)/.2;actor.setPos(v.getDouble("AcroX")+slide*1.5,v.getDouble("AcroY"),v.getDouble("AcroZ"));}
        if(phase==VANISH){actor.setInvisible(time>=14);actor.setNoGravity(true);}
        if(phase==E&&time==0)behind(p,actor);
        if(phase==F&&time>=20&&!v.getBoolean("BeetGiven")){removeHouseBeet(p.server,v);var beet=new ItemStack(Items.BEETROOT);if(!p.getInventory().add(beet))p.drop(beet,false);v.putBoolean("BeetGiven",true);BobinskyVisitData.get(p.server).setDirty();}
        if((phase==G||phase==K)&&!v.getBoolean("Inside")){
            actor.setNoGravity(false);actor.setXRot(0);if(actor.position().subtract(Vec3.atBottomCenterOf(front)).multiply(1,0,1).lengthSqr()<.04){v.putBoolean("Entering",true);BobinskyHouse.open(p.serverLevel(),v,true);}
            var target=v.getBoolean("Entering")?BobinskyHouse.door(v).south(2):front;var at=Vec3.atBottomCenterOf(target).add(v.getBoolean("Entering")?.5:0,0,0);var delta=at.subtract(actor.position()).multiply(1,0,1);actor.getNavigation().stop();if(delta.lengthSqr()>.01){float yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90;actor.setYRot(yaw);actor.yBodyRot=yaw;actor.setYHeadRot(yaw);actor.move(net.minecraft.world.entity.MoverType.SELF,delta.normalize().scale(Math.min(phase==K?.035:.065,delta.length())));}
            // Track distance toward each waypoint, not movement alone: sliding
            // against a wall or circling must still recover. Persist the budget
            // independently of dialogue time, which is reset after a pause.
            int returning=Math.max(time,v.getInt("ReturnTicks"))+1;
            double distance=actor.position().distanceToSqr(at);
            boolean progress=!v.contains("ReturnBestDistance")||v.getBoolean("ReturnEntering")!=v.getBoolean("Entering")||distance<v.getDouble("ReturnBestDistance")-.01;
            int stalled=progress?0:v.getInt("ReturnStalled")+1;
            if(progress)v.putDouble("ReturnBestDistance",distance);
            v.putBoolean("ReturnEntering",v.getBoolean("Entering"));v.putInt("ReturnTicks",returning);v.putInt("ReturnStalled",stalled);
            if(v.getBoolean("Entering")&&distance<.16||stalled>=100||returning>=600){
                // The interior is authored empty. Hide before relocating so a
                // blocked route cannot strand the visible actor or the story.
                actor.setInvisible(true);actor.getNavigation().stop();actor.setDeltaMovement(Vec3.ZERO);actor.setNoGravity(true);
                actor.setPos(Vec3.atBottomCenterOf(BobinskyHouse.door(v).south(2)).add(.5,0,0));
                v.putBoolean("Inside",true);BobinskyHouse.open(p.serverLevel(),v,false);BobinskyVisitData.get(p.server).setDirty();
            }
        }
        if(phase==WAIT_H){actor.setInvisible(true);actor.getNavigation().stop();}
        if(phase==H&&time==0){actor.setInvisible(false);actor.setNoGravity(true);actor.setPos(p.getX(),Math.min(p.getY()+25,p.level().getMaxBuildHeight()-4),p.getZ());com.coraline.wybie.WybieVisitNetwork.look(p,actor.getId(),VisitAudio.length("bobinsky_visit_h")+VisitAudio.length("bobinsky_visit_i")+100);v.putDouble("SkyY",actor.getY());}
        if(phase==I){actor.setNoGravity(true);double t=Math.min(1,time/(double)Math.max(1,VisitAudio.length("bobinsky_visit_i")));actor.setPos(p.getX(),v.getDouble("SkyY")+(p.getY()-v.getDouble("SkyY"))*t,p.getZ());}
        if(phase==LAND){actor.setPos(p.position());actor.setNoGravity(true);}
        if(phase==J&&time==0){actor.setNoGravity(false);actor.setInvisible(false);var side=Vec3.directionFromRotation(0,p.getYRot());actor.setPos(p.position().add(-side.z*2.5,0,side.x*2.5));com.coraline.wybie.VisitGrounding.lift(actor);com.coraline.wybie.WybieVisitNetwork.look(p,actor.getId(),30);}
        int delay=switch(phase){case WAIT_D->20;case VANISH->54;case WAIT_H->60;case LAND->20;default->0;};
        boolean done=delay>0?time>=delay:(v.getBoolean("VoiceDone")||time>=VisitAudio.length("bobinsky_visit_"+line(phase))+60)&&(phase!=F||time>=20)&&((phase!=G&&phase!=K)||v.getBoolean("Inside"));
        if(done){
            if(phase==I)actor.setPos(p.position());
            if(phase==K){VisitAudio.stopMusic(p,"bobinsky_visit_music");begin(p,v,WAIT_HIDE);return;}
            begin(p,v,phase+1);
        }else v.putInt("Elapsed",time+1);
    }
    public static void cancel(net.minecraft.server.MinecraftServer server,CompoundTag v,ServerPlayer p){
        removeParcel(server,v);
        var level=server.overworld();var actor=actor(server,v);if(actor!=null)actor.discard();if(v.hasUUID("Cheese")&&level.getEntity(v.getUUID("Cheese"))!=null)level.getEntity(v.getUUID("Cheese")).discard();
        if(p!=null){removeCheese(p,v);VisitAudio.stopMusic(p,"bobinsky_visit_music");if(!line(v.getInt("Phase")).isEmpty())VisitAudio.stopVoice(p,"bobinsky_visit_"+line(v.getInt("Phase")));}
        removeHouseBeet(server,v);
        if(BobinskyHouse.deferRestore(p,v)){v.putBoolean("Cancelled",true);v.putInt("Phase",WAIT_HIDE);BobinskyHouse.open(level,v,false);}else{BobinskyHouse.restore(level,v);v.putBoolean("Active",false);v.putLong("RetryAt",level.getGameTime()+1200);}BobinskyVisitData.get(server).setDirty();
    }
    private static void removeCheese(ServerPlayer p,CompoundTag v){for(var stack:p.getInventory().items)if(matches(stack,v))stack.setCount(0);for(var stack:p.getInventory().offhand)if(matches(stack,v))stack.setCount(0);var carried=p.containerMenu.getCarried();if(matches(carried,v))p.containerMenu.setCarried(ItemStack.EMPTY);for(var item:p.server.overworld().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(64)))if(matches(item.getItem(),v))item.discard();p.containerMenu.broadcastChanges();}
    public static boolean matches(ItemStack stack,CompoundTag v){var tag=stack.getTag();return stack.is(ModItems.CHEESE_PACKAGE.get())&&tag!=null&&tag.hasUUID("BobinskyToken")&&v.getUUID("Token").equals(tag.getUUID("BobinskyToken"));}
    private static void removeHouseBeet(net.minecraft.server.MinecraftServer server,CompoundTag v){if(v.hasUUID("HouseBeet")&&server.overworld().getEntity(v.getUUID("HouseBeet"))!=null)server.overworld().getEntity(v.getUUID("HouseBeet")).discard();v.remove("HouseBeet");}
}
