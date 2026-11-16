package com.coraline.secrets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.AABB;
import com.coraline.registry.*;
import com.coraline.otherworld.MouseEntity;
import com.coraline.feast.OtherMotherLoop;

public final class SecretSpies {
    public static void suspend(ServerPlayer p){var s=SecretTransactions.state(p);if(s==null)return;for(var tag:s.world.getList("Rats",11)){var id=net.minecraft.nbt.NbtUtils.loadUUID(tag);for(var level:p.server.getAllLevels()){var e=level.getEntity(id);if(e!=null&&e.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("SecretOwner")))e.discard();}}s.world.put("Rats",new net.minecraft.nbt.ListTag());for(var doll:p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,p.getBoundingBox().inflate(64),e->e.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("SecretOwner")))){doll.getPersistentData().putInt("Seen",0);doll.getPersistentData().remove("SeenAt");}}
    static boolean repairDollSeat(ServerPlayer p,net.minecraft.world.entity.Display.ItemDisplay doll){
        var n=doll.getPersistentData();
        var anchor=n.contains("SpySeat")?BlockPos.of(n.getLong("SpySeat")):doll.blockPosition();
        // Displays subtract half a block from the item origin; compensate so
        // the seated model rests above furniture instead of inside it.
        for(int dy=0;dy<=4;dy++){
            var seat=anchor.above(dy);
            var box=new AABB(seat.getX()+.1,seat.getY(),seat.getZ()+.1,seat.getX()+.9,seat.getY()+.75,seat.getZ()+.9);
            if(!p.serverLevel().getBlockState(seat.below()).isFaceSturdy(p.serverLevel(),seat.below(),net.minecraft.core.Direction.UP)
                ||!p.serverLevel().noCollision(box)||p.serverLevel().containsAnyLiquid(box))continue;
            doll.setPos(seat.getX()+.5,seat.getY()+.5,seat.getZ()+.5);
            n.putLong("SpySeat",seat.asLong());n.putBoolean("SpySeatV3",true);return true;
        }
        return false;
    }
    static final int RAT_WATCH_TICKS=60;
    static final double RAT_SPEED=.18;
    static boolean inRatView(MouseEntity rat,ServerPlayer p){var delta=p.position().subtract(rat.position()).multiply(1,0,1);return rat.getLookAngle().multiply(1,0,1).normalize().dot(delta.normalize())>Math.cos(Math.toRadians(60));}
    static boolean inDollView(net.minecraft.world.entity.Display.ItemDisplay doll,net.minecraft.world.phys.Vec3 eye){
        // ItemDisplay's fixed-item transform faces the model opposite its entity look vector.
        return doll.getLookAngle().scale(-1).dot(eye.subtract(doll.position()).normalize())>Math.cos(Math.toRadians(38));
    }
    static int watchTicks(net.minecraft.nbt.CompoundTag n,long now,boolean sees){int ticks=sees?(now-n.getLong("RatSeenAt")==1?n.getInt("Seen"):0)+1:0;n.putLong("RatSeenAt",now);n.putInt("Seen",ticks);return ticks;}
    static boolean ratMayWatch(ServerPlayer p){var r=SecretWorld.realm(p);return r!=null&&r.house!=null&&(com.coraline.otherworld.OtherWorldEstate.outsideDistance(r.house,p.blockPosition())>=40||SecretDialogue.secretArea(p,p.blockPosition()));}
    public static void doll(ServerPlayer p,BlockPos at){doll(p,at,0);}
    /** ItemDisplay's fixed transform reverses the authored room-facing yaw. */
    static float displayYaw(float roomYaw){return net.minecraft.util.Mth.wrapDegrees(roomYaw+180);}
    private static void faceRoom(ServerPlayer p,net.minecraft.world.entity.Display.ItemDisplay doll){
        var s=SecretTransactions.state(p);BlockPos target=null;double nearest=Double.MAX_VALUE;
        var realm=SecretWorld.realm(p);
        if(realm!=null&&realm.house!=null)for(var room:HouseCabinets.ROOMS)if(HouseCabinets.contains(realm.house,room,doll.blockPosition())){
            target=HouseCabinets.at(realm.house,(room.min()[0]+room.max()[0])/2,(room.min()[1]+room.max()[1])/2,(room.min()[2]+room.max()[2])/2);break;
        }
        if(target==null)for(var room:SecretWorld.rooms(s)){var center=BlockPos.of(room.getLong("Pos"));double distance=doll.distanceToSqr(center.getCenter());if(distance<nearest&&distance<15*15){nearest=distance;target=center;}}
        if(target!=null){double dx=target.getX()+.5-doll.getX(),dz=target.getZ()+.5-doll.getZ();doll.setYRot(displayYaw((float)Math.toDegrees(Math.atan2(-dx,dz))));}
        else doll.setYRot(displayYaw(doll.getYRot()));
        doll.getPersistentData().putBoolean("SpyFacingV4",true);
    }
    public static void doll(ServerPlayer p,BlockPos at,float yaw){var e=EntityType.ITEM_DISPLAY.create(p.serverLevel());if(e==null)return;var display=new net.minecraft.nbt.CompoundTag();var item=new net.minecraft.world.item.ItemStack(ModItems.DOLL.get());item.getOrCreateTag().putInt("SecretSpySkin",p.getRandom().nextInt(16));display.put("item",item.save(new net.minecraft.nbt.CompoundTag()));display.putString("item_display","fixed");e.load(display);e.setPos(at.getX()+.5,at.getY()+.5,at.getZ()+.5);e.setYRot(yaw);mark(p,e,"doll");e.getPersistentData().putLong("SpySeat",at.asLong());e.getPersistentData().putBoolean("SpyVisualV2",true);p.serverLevel().addFreshEntity(e);}
    static void mark(ServerPlayer p,Entity e,String kind){e.getPersistentData().putBoolean("CoralineDecoration",true);e.getPersistentData().putUUID("SecretOwner",p.getUUID());e.getPersistentData().putInt("SecretVisit",SecretTransactions.state(p).visit);e.getPersistentData().putString("SpyKind",kind);}
    public static void rat(ServerPlayer p,BlockPos at){var e=ModEntities.MOUSE.get().create(p.serverLevel());if(e==null)return;var s=SecretTransactions.state(p);var ids=s.world.getList("Rats",11);int cap=cap(p);if(ids.size()>=cap)return;ids.add(net.minecraft.nbt.NbtUtils.createUUID(e.getUUID()));s.world.put("Rats",ids);mark(p,e,"rat");e.spying(true);e.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);e.setInvulnerable(false);p.serverLevel().addFreshEntity(e);}
    public static boolean mouse(MouseEntity rat){var n=rat.getPersistentData();if(!n.hasUUID("SecretOwner"))return false;var p=rat.getServer().getPlayerList().getPlayer(n.getUUID("SecretOwner"));if(p!=null&&CatEncounter.active(p)){removeRat(p,rat);rat.discard();return true;}return mouse(rat,p);}
    static boolean mouse(MouseEntity rat,ServerPlayer p){var n=rat.getPersistentData();if(p==null||!SecretsRuntime.active(p)) {rat.getNavigation().stop();return true;}var s=SecretTransactions.state(p);boolean listed=false;for(var id:s.world.getList("Rats",11))if(net.minecraft.nbt.NbtUtils.loadUUID(id).equals(rat.getUUID())){listed=true;break;}if(!listed){rat.discard();return true;}if(!n.getBoolean("Alerted")&&rat.distanceToSqr(p)>64*64){removeRat(p,rat);rat.discard();return true;}if(n.getInt("SecretVisit")!=s.visit){rat.discard();return true;}
        // Actor ticks and delayed reports run independently of the player spy tick.
        // A legitimate attic invitation must suppress all three entry points.
        if(com.coraline.feast.MouseCircusVisit.protectedRoute(p)){
            n.putInt("Seen",0);n.remove("Alerted");n.remove("RunUntil");n.remove("ReportPosition");
            rat.setGlowingTag(false);rat.getNavigation().stop();return true;
        }
        // Existing rats can watch anywhere; spawn locations remain restricted.
        if(!n.getBoolean("Alerted")&&com.coraline.feast.SpecialDelivery.protectedRoute(p)){n.putInt("Seen",0);n.putLong("RatSeenAt",OtherMotherLoop.now(p));return true;}
        rat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(RAT_SPEED);
        if(n.getBoolean("Alerted")){
            if(com.coraline.feast.MotherGoesOut.reportingRat(rat,p))return true;
            rat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(.65);
            if(!n.contains("RunUntil"))n.putLong("RunUntil",OtherMotherLoop.now(p)+20);
            if(rat.tickCount%5==0){var away=rat.position().subtract(p.position()).multiply(1,0,1);if(away.lengthSqr()<.01)away=new net.minecraft.world.phys.Vec3(1,0,0);var target=rat.position().add(away.normalize().scale(12));rat.getNavigation().moveTo(target.x,rat.getY(),target.z,2.8);}
            if(OtherMotherLoop.now(p)>=n.getLong("RunUntil")){var sighting=n.contains("ReportPosition")?BlockPos.of(n.getLong("ReportPosition")):p.blockPosition();if(!com.coraline.feast.MotherGoesOut.spyReport(p,rat,"rat",sighting))report(p,sighting,n.getString("SpyIdentified"));p.displayClientMessage(net.minecraft.network.chat.Component.literal("The rat rushed away to tell Other Mother where it saw you."),true);removeRat(p,rat);rat.discard();}return true;
        }
        if(n.getLong("CommandUntil")>OtherMotherLoop.now(p)){if(rat.tickCount%10==0){var target=BlockPos.of(n.getLong("CommandTarget"));rat.getNavigation().moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,1);}return true;}
        long now=OtherMotherLoop.now(p);
        boolean sees=(!com.coraline.feast.MotherGoesOut.unavailable(p)||com.coraline.feast.MotherGoesOut.suspicious(p))&&s.world.getLong("HijackUntil")<=now&&rat.distanceToSqr(p)<Math.pow(18*(1-.5*s.effective(2)),2)&&inRatView(rat,p)&&rat.hasLineOfSight(p)&&s.invisibleUntil<=now&&!p.isInvisible();
        int ticks=watchTicks(n,now,sees);
        if(sees){
            rat.getNavigation().stop();var delta=p.getEyePosition().subtract(rat.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
            rat.setYRot(net.minecraft.util.Mth.approachDegrees(rat.getYRot(),yaw,12));rat.setYHeadRot(rat.getYRot());rat.setYBodyRot(rat.getYRot());rat.getLookControl().setLookAt(p,30,30);
            if(ticks==1||ticks%20==0)rat.playSound(ModSounds.MOUSE_SQUEAK.get(),2.4F,1.35F);
            if(ticks>=RAT_WATCH_TICKS){n.putString("SpyIdentified",observedEvidence(p));com.coraline.feast.MotherGoesOut.recordSighting(p,n);n.putBoolean("Alerted",true);n.putLong("ReportPosition",p.blockPosition().asLong());n.putLong("RunUntil",now+20);rat.setGlowingTag(true);rat.playSound(ModSounds.MOUSE_SQUEAK.get(),2.4F,.85F);}
            return true;
        }
        if(rat.distanceToSqr(p)<20*20&&Math.floorMod(rat.tickCount+rat.getId()*17,80)==0&&s.world.getLong("RatChirpAfter")<=now){rat.playSound(ModSounds.MOUSE_SQUEAK.get(),.65F,1.05F);s.world.putLong("RatChirpAfter",now+40);}
        if(rat.tickCount%100==0)rat.getNavigation().moveTo(rat.getX()+p.getRandom().nextInt(13)-6,rat.getY(),rat.getZ()+p.getRandom().nextInt(13)-6,.7);return true;}
    /** Only the registry-valid item actually visible when recognition completes becomes report evidence. */
    static String observedEvidence(ServerPlayer p){for(var held:java.util.List.of(p.getMainHandItem(),p.getOffhandItem())){String id=SecretTransactions.evidence(p,held);if(SecretRegistry.ALL.containsKey(id))return id;}return "";}
    static void report(ServerPlayer p){report(p,p.blockPosition());}
    /** Legacy position-only reports confer no item knowledge. */
    static void report(ServerPlayer p,BlockPos sighting){report(p,sighting,"");}
    static void report(ServerPlayer p,BlockPos sighting,String identified){if(com.coraline.feast.MouseCircusVisit.protectedRoute(p))return;var s=SecretTransactions.state(p);if(s.falseUntil>OtherMotherLoop.now(p)){MotherSearch.noise(p,s.target,100,"false report");return;}var m=MotherSearch.mother(p);if(com.coraline.feast.MotherGoesOut.unavailable(p)||Reconciliation.active(p)||m!=null&&s.world.getBoolean("CaughtEncounter")&&m.distanceToSqr(p)<=144)return;
        if(!com.coraline.feast.SpecialDelivery.openedActive(p)&&SecretRegistry.ALL.containsKey(identified))s.exposed.add(identified);
        // An unloaded Mother must not silently consume the rat/doll's report.
        // Persist the last seen location first; her small ticking lease loads her
        // saved chunk so she can investigate without the player approaching her.
        if(m==null){if(s.search!=SecretsState.Search.DIRECT_CHASE){s.search=SecretsState.Search.INVESTIGATING_NOISE;s.target=sighting.immutable();s.world.putLong("SpyReportOrigin",sighting.asLong());}MotherSearch.holdSearch(p);SecretTransactions.checkpoint(p);return;}
        // Opened-delivery reports recover her at the saved sighting, never reveal
        // the payload and never constitute a capture. Normal reports still run.
        if(com.coraline.feast.SpecialDelivery.openedActive(p)&&s.search!=SecretsState.Search.DIRECT_CHASE){
            outer:for(int radius=4;radius<=7;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                for(int dy:new int[]{0,1,-1,2,-2}){
                    var at=sighting.offset(dx,dy,dz);var level=p.serverLevel();
                    var box=new net.minecraft.world.phys.AABB(at.getX()+.15,at.getY(),at.getZ()+.15,at.getX()+.85,at.getY()+2,at.getZ()+.85);
                    if(!level.hasChunkAt(at)||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP)||!level.getFluidState(at).isEmpty()||!level.noCollision(m,box)||p.distanceToSqr(at.getCenter())<9)continue;
                    m.stopRiding();m.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);break outer;
                }
            }
        }
        if(s.search!=SecretsState.Search.DIRECT_CHASE){s.search=SecretsState.Search.INVESTIGATING_NOISE;s.target=sighting.immutable();s.world.putLong("SpyReportOrigin",sighting.asLong());m.setNoAi(false);m.setNoGravity(false);m.stopRiding();m.getNavigation().moveTo(sighting.getX()+.5,sighting.getY(),sighting.getZ()+.5,2.8);}
        SecretTransactions.dirty(p);
    }
    public static void tick(ServerPlayer p){if(CatEncounter.active(p)||com.coraline.feast.MouseCircusVisit.protectedRoute(p)){suspend(p);return;}MotherSearch.holdSearch(p);var s=SecretTransactions.state(p);if(p.tickCount%5!=0)return;
        for(var e:p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,p.getBoundingBox().inflate(24),e->p.getUUID().equals(e.getPersistentData().hasUUID("SecretOwner")?e.getPersistentData().getUUID("SecretOwner"):null)&&e.getPersistentData().getString("SpyKind").equals("doll"))){
            var n=e.getPersistentData();if(!n.getBoolean("SpyVisualV2")){n.putBoolean("SpyVisualV2",true);var saved=e.saveWithoutId(new net.minecraft.nbt.CompoundTag());var item=new net.minecraft.world.item.ItemStack(ModItems.DOLL.get());item.getOrCreateTag().putInt("SecretSpySkin",Math.floorMod(e.getUUID().hashCode(),16));saved.put("item",item.save(new net.minecraft.nbt.CompoundTag()));e.load(saved);var below=e.blockPosition().below();if(p.serverLevel().isEmptyBlock(below))SecretWorld.place(p,below,net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS);e.setYRot(-135F);}
            if(!n.getBoolean("SpySeatV3")&&SecretWorld.pending(s)==0)repairDollSeat(p,e);if(!n.getBoolean("SpyFacingV4"))faceRoom(p,e);if(n.getInt("SecretVisit")!=s.visit){e.discard();continue;}
            var delta=p.getEyePosition().subtract(e.position());boolean visible=!com.coraline.feast.SpecialDelivery.protectedRoute(p)&&(SecretDialogue.secretArea(p,p.blockPosition())||com.coraline.feast.SpecialDelivery.openedActive(p)||com.coraline.feast.MotherGoesOut.unavailable(p)&&com.coraline.feast.MotherGoesOut.suspicious(p))&&s.world.getLong("HijackUntil")<=OtherMotherLoop.now(p)&&delta.lengthSqr()<18*18&&inDollView(e,p.getEyePosition())&&p.hasLineOfSight(e)&&s.invisibleUntil<=OtherMotherLoop.now(p)&&!p.isInvisible();
            long now=OtherMotherLoop.now(p);int seen=visible?(now-n.getLong("SeenAt")<=5?n.getInt("Seen"):0)+5:0;n.putLong("SeenAt",now);n.putInt("Seen",seen);if(seen==5)notice(p,e);if(seen>0&&p.tickCount%20==0)p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,e.getX(),e.getY()+.3,e.getZ(),1,0,0,0,0);if(seen>=60&&n.getLong("ReportAfter")<=now){n.putInt("Seen",0);n.putLong("ReportAfter",now+600);String identified=observedEvidence(p);com.coraline.feast.MotherGoesOut.recordSighting(p,n);if(!com.coraline.feast.MotherGoesOut.spyReport(p,e,"doll",p.blockPosition())){p.displayClientMessage(net.minecraft.network.chat.Component.literal("The doll has told Other Mother where it saw you."),true);report(p,p.blockPosition(),identified);}}
        }
        if(p.tickCount%60==0){var ids=s.world.getList("Rats",11);ids.removeIf(tag->{var entity=p.serverLevel().getEntity(net.minecraft.nbt.NbtUtils.loadUUID(tag));return entity==null||entity.isRemoved();});s.world.put("Rats",ids);int cap=cap(p);if(ratMayWatch(p)&&ids.size()<cap){var at=com.coraline.otherworld.OtherWorld.safePlayer(p.serverLevel(),p.blockPosition().offset(p.getRandom().nextInt(33)-16,0,p.getRandom().nextInt(33)-16));if(at.distSqr(p.blockPosition())>64)rat(p,at);}}
    }
    private static int cap(ServerPlayer p){if(CatEncounter.active(p))return 0;var s=SecretTransactions.state(p);int room=SecretWorld.roomAt(s,p.blockPosition());boolean guarded=room>=9&&(s.exposed.contains("soulglass")||s.exposed.contains("soul_prisons"));return 3+(int)(OtherMotherLoop.scene(p).loop.suspicion*9)+(guarded?2:0);}
    public static void removeRat(ServerPlayer p,Entity rat){var s=SecretTransactions.state(p);if(s==null)return;var ids=s.world.getList("Rats",11);ids.removeIf(tag->net.minecraft.nbt.NbtUtils.loadUUID(tag).equals(rat.getUUID()));s.world.put("Rats",ids);}
    private static void notice(ServerPlayer p,Entity spy){
        var s=SecretTransactions.state(p);long now=OtherMotherLoop.now(p);
        if(s.world.getLong("SpyHintUntil")>now)return;s.world.putLong("SpyHintUntil",now+100);
        spy.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,.5F,1.3F);
        p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,spy.getX(),spy.getY()+.35,spy.getZ(),2,.03,.03,.03,0);
    }
    private SecretSpies(){}
}
