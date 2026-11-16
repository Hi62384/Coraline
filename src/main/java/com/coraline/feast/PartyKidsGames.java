package com.coraline.feast;
import com.coraline.secrets.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Short physical games with silent Other Wybie, inside the existing party. No stakes or progression rewards. */
public final class PartyKidsGames {
    private static CompoundTag data(VisitData.Scene s){return HouseParty.data(s).getCompound("KidsGame");}
    static boolean active(VisitData.Scene s){return s!=null&&data(s).getBoolean("Active");}
    static boolean start(ServerPlayer p,VisitData.Scene s,CompoundTag guest,boolean tag){
        var w=PartyGuests.entity(p,guest);var realm=SecretWorld.realm(p);
        if(w==null||realm==null||realm.house==null||!guest.getString("Kind").equals("other_wybie")||active(s)||PartyMatches.active(s)||s.loop.phase!=OtherMotherLoopState.Phase.PARTY_SOCIAL||HouseParty.data(s).getBoolean("FeastDue"))return false;
        if(HouseParty.active(p)!=s||w.level()!=p.level()||!w.isAlive()||!s.owner.equals(p.getUUID()))return false;
        Vec3 center=null;String roomName="";
        for(String id:java.util.List.of("front_turret_room","front_west_room","supper_room")){
            var room=PartyRegions.rooms().stream().filter(r->r.id().equals(id)).findFirst().orElse(null);if(room==null)continue;
            int halfX=tag?2:4,halfZ=2;
            for(int x=room.min()[0]+halfX+1;x<=room.max()[0]-halfX&&center==null;x+=2)for(int z=room.min()[2]+halfZ+1;z<=room.max()[2]-halfZ&&center==null;z+=2){
                var candidate=PartyFloor.rectangle(p,com.coraline.secrets.HouseCabinets.at(realm.house,x,room.floor()+1,z),halfX,halfZ);
                if(candidate==null||!p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(candidate,candidate).inflate(halfX+.3,1.5,halfZ+.3),e->e.kind()!=ScenePropEntity.OUTING_SCENERY).isEmpty())continue;
                center=candidate;roomName=room.label();
            }
            if(center!=null)break;
        }
        if(center==null){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Wybie needs a clear patch of floor for this game. Move loose furniture from the front sitting room."),true);return false;}
        w.setNoAi(false);w.setNoGravity(false);w.noPhysics=false;w.getNavigation().stop();
        if(w instanceof com.coraline.entity.CharacterEntity cast){guest.putBoolean("KidsOldDuck",cast.partyDoorDuck());cast.partyDoorDuck(true);}
        if(w.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}
        var route=w.getNavigation().createPath(net.minecraft.core.BlockPos.containing(center),0);
        if(route==null||!route.canReach()){if(w instanceof com.coraline.entity.CharacterEntity cast)cast.partyDoorDuck(guest.getBoolean("KidsOldDuck"));guest.remove("KidsOldDuck");p.displayClientMessage(net.minecraft.network.chat.Component.literal("Wybie cannot reach that play area. Try again from the front sitting room."),true);return false;}
        w.getNavigation().moveTo(route,1.2);
        var n=new CompoundTag();n.putBoolean("Active",true);n.putBoolean("Tag",tag);n.putUUID("Guest",guest.getUUID("Id"));n.putInt("Serial",s.loop.event.getInt("Serial"));n.putDouble("X",center.x);n.putDouble("Y",center.y);n.putDouble("Z",center.z);n.putBoolean("WybieIt",true);n.putInt("Cooldown",60);HouseParty.data(s).put("KidsGame",n);
        n.putString("Room",roomName);guest.putString("State","PLAYING_KIDS_GAME");guest.remove("Target");guest.remove("Group");VisitAudio.chatter(w,false);
        if(!tag){var ball=VisitEvents.prop(p,s,ScenePropEntity.PARTY_BALL,center.add(0,.25,0),3900);ball.getPersistentData().putBoolean("PartyKidsProp",true);n.putUUID("Ball",ball.getUUID());for(int side:new int[]{-1,1})VisitEvents.prop(p,s,ScenePropEntity.PARTY_GOAL,center.add(side*3.5,0,0),3901+(side+1)/2).getPersistentData().putBoolean("PartyKidsProp",true);}
        VisitAudio.caption(p,"Other Wybie",tag?"Tag! Meet Wybie in the "+roomName+". Right-click him when you're 'it'.":"Kickball in the "+roomName+". Right-click the ball to kick. First to three!",140);return true;
    }
    static Vec3 center(CompoundTag n){return new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));}
    static void status(ServerPlayer p,VisitData.Scene s,CompoundTag out){
        if(!active(s))return;var n=data(s);var c=center(n);
        out.putBoolean("KidsMarker",true);out.putLong("KidsWaypoint",net.minecraft.core.BlockPos.containing(c).asLong());
        out.putString("ShowObjective",(n.getBoolean("Tag")?"Tag":"Kickball")+" — "+n.getString("Room")+(n.getBoolean("Started")?" · You "+n.getInt("You")+" — Wybie "+n.getInt("Wybie"):" · Meet Wybie at the glowing ball"));
    }
    static boolean tag(ServerPlayer p,Mob actor){var s=HouseParty.active(p);if(!active(s))return false;var n=data(s);var guest=HousePartyLedger.guest(HouseParty.data(s),n.getUUID("Guest"));if(guest==null||PartyGuests.entity(p,guest)!=actor)return false;
        if(n.getBoolean("Tag")&&!n.getBoolean("WybieIt")&&n.getInt("Cooldown")==0&&p.distanceToSqr(actor)<9&&actor.hasLineOfSight(p)){n.putBoolean("WybieIt",true);n.putInt("Cooldown",50);n.putInt("You",n.getInt("You")+1);PartyGuests.gesture(actor,7,30);VisitAudio.caption(p,"Tag!","You caught Wybie! Run—now he's it.",60);}return true;
    }
    static boolean kick(ServerPlayer p,ScenePropEntity ball){if(ball.kind()!=ScenePropEntity.PARTY_BALL)return false;var s=HouseParty.active(p);if(!active(s))return true;var n=data(s);
        if(!n.hasUUID("Ball")||!ball.getUUID().equals(n.getUUID("Ball"))||!p.getUUID().equals(ball.owner)||ball.distanceToSqr(p)>16||n.getInt("Cooldown")>0)return true;
        var aim=p.getLookAngle().multiply(1,0,1).normalize();n.putDouble("VX",aim.x*.48);n.putDouble("VZ",aim.z*.48);n.putDouble("VY",.12);n.putInt("Cooldown",6);p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);return true;
    }
    static void tick(ServerPlayer p,VisitData.Scene s){
        if(!active(s))return;var n=data(s);var g=HousePartyLedger.guest(HouseParty.data(s),n.getUUID("Guest"));var w=g==null?null:PartyGuests.entity(p,g);var c=center(n);
        if(w==null||n.getInt("Serial")!=s.loop.event.getInt("Serial")||s.loop.phase!=OtherMotherLoopState.Phase.PARTY_SOCIAL||HouseParty.data(s).getBoolean("FeastDue")||s.loop.secrets.searching()||n.getBoolean("Started")&&p.distanceToSqr(c)>900||p.isDeadOrDying()){finish(p,s,"Game paused. You can ask Wybie to play again later.");return;}
        int age=n.getInt("Age")+1;n.putInt("Age",age);if(n.getInt("Cooldown")>0)n.putInt("Cooldown",n.getInt("Cooldown")-1);
        g.putString("State","PLAYING_KIDS_GAME");
        if(w instanceof com.coraline.entity.CharacterEntity cast)cast.partyDoorDuck(true);
        if(!n.getBoolean("Started")&&(p.distanceToSqr(c)>36||w.distanceToSqr(c)>16||Math.abs(w.getY()-c.y)>1.5)){
            if(w.tickCount%20==0){w.getNavigation().moveTo(c.x,c.y,c.z,1.2);p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,c.x,c.y+.5,c.z,3,.35,.25,.35,.005);}
            if(age>1200)finish(p,s,"Wybie couldn't reach the play area. The game is cancelled safely; ask him to try again.");return;
        }
        if(!n.getBoolean("Started")){n.putBoolean("Started",true);n.putInt("Age",0);age=0;VisitAudio.caption(p,"Other Wybie","We're both here. Let's play!",60);}
        w.getLookControl().setLookAt(p,25,20);
        if(n.getBoolean("Tag")){
            if(w.tickCount%12==0){var target=n.getBoolean("WybieIt")?p.position():c.add(Math.sin(age*.025)*1.7,0,Math.cos(age*.025)*1.7);if(PartyRegions.allowed(p,target))w.getNavigation().moveTo(target.x,target.y,target.z,n.getBoolean("WybieIt")?1.05:1.5);}
            if(n.getBoolean("WybieIt")&&n.getInt("Cooldown")==0&&w.distanceToSqr(p)<2.5&&w.hasLineOfSight(p)){n.putBoolean("WybieIt",false);n.putInt("Cooldown",50);n.putInt("Wybie",n.getInt("Wybie")+1);VisitAudio.caption(p,"Tag!","Wybie caught you—you're it! Right-click him to tag him back.",70);}
        }else{
            var entity=p.serverLevel().getEntity(n.getUUID("Ball"));if(!(entity instanceof ScenePropEntity ball)){finish(p,s,"The kickball game ended safely.");return;}
            double vx=n.getDouble("VX"),vz=n.getDouble("VZ"),vy=n.getDouble("VY")-.025;var before=ball.position();ball.move(MoverType.SELF,new Vec3(vx,vy,vz));if(ball.horizontalCollision){vx*=-.7;vz*=-.7;}if(ball.verticalCollision)vy=Math.abs(vy)*.45;n.putDouble("VX",vx*.97);n.putDouble("VZ",vz*.97);n.putDouble("VY",vy);
            if(!PartyRegions.allowed(p,ball.position())||ball.getY()<c.y-1){ball.setPos(c.add(0,.25,0));n.putDouble("VX",0);n.putDouble("VZ",0);n.putDouble("VY",0);}
            if(Math.abs(ball.getX()-c.x)>3.3&&Math.abs(ball.getZ()-c.z)<2.2){String score=ball.getX()>c.x?"You":"Wybie";n.putInt(score,n.getInt(score)+1);ball.setPos(c.add(0,.25,0));n.putDouble("VX",0);n.putDouble("VZ",0);n.putDouble("VY",0);n.putInt("Cooldown",30);VisitAudio.caption(p,"Goal!","You "+n.getInt("You")+" — Wybie "+n.getInt("Wybie"),70);}
            if(w.tickCount%12==0)w.getNavigation().moveTo(ball,1.2);
            if(w.distanceToSqr(ball)<2&&n.getInt("Cooldown")==0){var aim=c.add(-4,0,0).subtract(ball.position()).multiply(1,0,1).normalize();n.putDouble("VX",aim.x*.35);n.putDouble("VZ",aim.z*.35);n.putDouble("VY",.1);n.putInt("Cooldown",20);PartyGuests.gesture(w,6,15);}
        }
        if(age>=1200||n.getInt("You")>=3||n.getInt("Wybie")>=3)finish(p,s,"Good game! You "+n.getInt("You")+" — Wybie "+n.getInt("Wybie")+". Ask him for a rematch anytime before the feast.");
    }
    static void finish(ServerPlayer p,VisitData.Scene s,String message){if(!active(s))return;var n=data(s);n.putBoolean("Active",false);var g=HousePartyLedger.guest(HouseParty.data(s),n.getUUID("Guest"));if(g!=null){var actor=PartyGuests.entity(p,g);if(actor!=null){actor.getNavigation().stop();if(actor instanceof com.coraline.entity.CharacterEntity cast)cast.partyDoorDuck(g.getBoolean("KidsOldDuck"));}g.remove("KidsOldDuck");if(g.getString("State").equals("PLAYING_KIDS_GAME"))g.putString("State","WANDERING");}for(var prop:VisitEvents.props(p,s))if(prop.getPersistentData().getBoolean("PartyKidsProp"))prop.discard();if(!message.isEmpty())VisitAudio.caption(p,"Other Wybie",message,120);}
    private PartyKidsGames(){}
}
