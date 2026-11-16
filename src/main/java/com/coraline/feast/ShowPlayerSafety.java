package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Safety is scoped to the owner's actual stunt seat; never grants permanent invulnerability. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ShowPlayerSafety {
    public static boolean attach(ServerPlayer p,VisitData.Scene s,CinematicPropEntity seat){
        if(!s.owner.equals(p.getUUID())||seat==null||!seat.isAlive()||seat.level()!=p.level()||!seat.owned(s)||seat.kind()!=CinematicPropEntity.SEAT||seat.slot()!=0||!TheatreSite.inside(p,s))return false;
        // Mount callbacks may reject the attachment. Do not advertise stunt protection
        // until the owner actually occupies the correct live seat.
        s.loop.event.putBoolean("ShowStunt",false);
        var carrier=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.STUNT_CARRIER,p.blockPosition(),0);if(carrier==null)return false;
        carrier.setPos(p.position());s.loop.event.putBoolean("ShowSeatRelease",true);
        try{if(p.getVehicle()!=carrier){p.stopRiding();if(!p.startRiding(carrier,true))return false;}}finally{s.loop.event.remove("ShowSeatRelease");}
        s.loop.event.putBoolean("ShowStunt",true);p.fallDistance=0;OtherMotherLoop.changed(p);VisitEvents.sendStatus(p,s);return true;
    }
    public static boolean active(ServerPlayer p){var s=OtherMotherLoop.scene(p);return s!=null&&s.owner.equals(p.getUUID())&&s.loop.event.getBoolean("ShowStunt")&&TheatreSite.inside(p,s)&&p.getVehicle() instanceof CinematicPropEntity seat&&seat.isAlive()&&seat.owned(s)&&seat.kind()==CinematicPropEntity.STUNT_CARRIER&&seat.slot()==0;}
    public static boolean seatLocked(ServerPlayer p){var s=OtherMotherLoop.scene(p);return p.isAlive()&&s!=null&&!s.loop.event.getBoolean("ShowSeatRelease")&&TheatreVisit.owned(p,s)&&TheatreVisit.data(s).getString("Phase").equals("PERFORMING")&&!TheatreVisit.data(s).getBoolean("NeedsRecovery")&&TheatreSite.inside(p,s)&&p.getVehicle() instanceof CinematicPropEntity seat&&seat.owned(s)&&seat.kind()==CinematicPropEntity.SEAT&&seat.slot()==0;}
    public static boolean move(ServerPlayer p,VisitData.Scene s,ScriptedShowTimeline.Point path){if(!active(p))return false;var c=TheatreSite.center(p,s);var seat=(CinematicPropEntity)p.getVehicle();seat.setPos(c.getX()+.5+path.x(),c.getY()+path.y(),c.getZ()+.5+path.z());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);return true;}
    public static void release(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID()))return;s.loop.event.putBoolean("ShowStunt",false);s.loop.event.putBoolean("ShowSeatRelease",true);try{var vehicle=p.getVehicle();if(vehicle instanceof CinematicPropEntity seat&&seat.owned(s)&&(seat.kind()==CinematicPropEntity.SEAT||seat.kind()==CinematicPropEntity.STUNT_CARRIER)&&seat.slot()==0){p.stopRiding();if(seat.kind()==CinematicPropEntity.STUNT_CARRIER)seat.discard();else if(TheatreSite.inside(p,s))seat.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(TheatreSite.playerSeat(p,s)));p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);}}finally{s.loop.event.remove("ShowSeatRelease");}}
    @SubscribeEvent public static void damage(net.minecraftforge.event.entity.living.LivingHurtEvent e){if(e.getEntity() instanceof ServerPlayer p&&active(p))e.setCanceled(true);}
    @SubscribeEvent public static void dismount(net.minecraftforge.event.entity.EntityMountEvent e){if(e.isDismounting()&&e.getEntityMounting() instanceof ServerPlayer p&&(active(p)||seatLocked(p)))e.setCanceled(true);}
    private ShowPlayerSafety(){}
}
