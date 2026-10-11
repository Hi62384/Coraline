package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.parents.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Story lifetime follows the claimed family, including offline owners and old saves. */
public final class FamilyVisitReset {
    private FamilyVisitReset(){}
    public static void validate(MinecraftServer server,UUID owner,UUID family){
        var data=VisitData.get(server);var scene=data.scenes.get(owner);if(scene==null)return;
        var oldDoor=DoorData.get(server).doors.get(scene.door);
        boolean stale=scene.family!=null?!scene.family.equals(family):oldDoor!=null&&oldDoor.revoked;
        if(stale){forget(server,owner);return;}
        // Legacy saves did not store the pair identity. Attach only when the old
        // door is still valid; a revoked old door must never adopt the new family.
        if(scene.family==null&&family!=null){scene.family=family;data.setDirty();}
    }
    public static void endFamily(MinecraftServer server,FamilyData.Family family){
        if(family.owner==null)return;
        var doors=DoorData.get(server);var ids=new HashSet<UUID>();
        for(var door:doors.doors.values())if(family.owner.equals(door.owner))ids.add(door.id);
        // Bring visitors out while the return address still exists, then retire
        // realm records and jobs. Another player's own quest is untouched.
        for(var p:server.getPlayerList().getPlayers()){
            var tag=p.getPersistentData();boolean visiting=tag.hasUUID("CoralineVisitDoor")&&ids.contains(tag.getUUID("CoralineVisitDoor"));
            if(visiting){
                tag.putBoolean("CoralineRecovering",true);
                try{VisitAudio.stop(p);p.stopRiding();p.stopSleepInBed(true,false);tag.remove("CoralineWakeAt");if(ActivityWorld.protectedWorld(p.level()))OtherWorld.leave(p,false);else{tag.remove("CoralineVisitDoor");tag.remove("CoralineInTunnel");tag.remove("CoralineReturning");}}
                finally{tag.remove("CoralineRecovering");}
                p.sendSystemMessage(Component.literal("These Other Parents are gone. You have returned home safely."));
            }
            if(family.owner.equals(p.getUUID())){tag.remove("CoralineWaitingDoor");SceneAudio.cancelPending(p);}
        }
        forget(server,family.owner);
        SceneAudio.endFamily(server,family.owner,family.id,ids);
        var realms=RealmData.get(server);
        for(var id:ids){OtherWorld.cancelCopy(server,id);realms.realms.remove(id);}
        realms.setDirty();
        var level=server.getLevel(OtherWorld.KEY);
        if(level!=null){var remove=new ArrayList<net.minecraft.world.entity.Entity>();
            for(var e:level.getAllEntities())if(e instanceof ParentEntity parent&&parent.isOtherParent()&&parent.getPersistentData().hasUUID("RealmDoor")&&ids.contains(parent.getPersistentData().getUUID("RealmDoor")))remove.add(e);
            remove.forEach(net.minecraft.world.entity.Entity::discard);
        }
    }
    static void forget(MinecraftServer server,UUID owner){
        OtherMotherLoop.forget(owner);
        var previous=VisitData.get(server).scenes.get(owner);if(previous!=null)LoopProps.clean(server,previous);
        var data=VisitData.get(server);data.scenes.remove(owner);data.setDirty();FirstVisit.forgetRuntime(owner);
        var p=server.getPlayerList().getPlayer(owner);if(p!=null){OtherMotherTutorial.cancel(p);OtherMotherActivities.cancel(p);OtherMotherActivityComplete.cancel(p);VisitAudio.stop(p);}
        var level=server.getLevel(OtherWorld.KEY);if(level==null)return;
        var remove=new ArrayList<ScenePropEntity>();
        for(var e:level.getAllEntities()){
            if(e instanceof ScenePropEntity prop&&owner.equals(prop.owner))remove.add(prop);
            if(e instanceof ParentEntity parent&&parent.getPersistentData().hasUUID("VisitOwner")&&owner.equals(parent.getPersistentData().getUUID("VisitOwner"))){parent.stopRiding();parent.setGlowingTag(false);parent.getPersistentData().remove("VisitOwner");}
        }
        for(var prop:remove){prop.ejectPassengers();prop.discard();}
    }
}
