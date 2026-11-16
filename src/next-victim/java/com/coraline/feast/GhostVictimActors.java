package com.coraline.feast;

import java.util.*;
import com.coraline.parents.ParentEntity;
import com.coraline.tycoon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** Physical, persistent child identity. A recorded but unloaded child is never
 * replaced. Ordinary adopted residents do not retain this story's control lease. */
public final class GhostVictimActors {
    public static final String LEASE="GhostVictimLease";
    private static final String[] FIRST={"Robin","Casey","Morgan","Alex","Jamie","Quinn","Avery","Sam","Jordan","Riley","Taylor","Charlie"};
    private static final String[] LAST={"Ellis","Reed","Bennett","Brooks","Patel","Rivera","Chen","Lewis","Hayes","Gray","Park","Clarke"};
    public static CompoundTag identity(UUID id){
        var identity=TycoonCare.generate(id).n.copy();var random=new Random(id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),7));
        identity.putString("Name",FIRST[random.nextInt(FIRST.length)]+" "+LAST[random.nextInt(LAST.length)]);
        identity.putInt("MotherSkin",random.nextInt(ParentEntity.SKIN_COUNT));identity.putInt("FatherSkin",random.nextInt(ParentEntity.SKIN_COUNT));
        TycoonPersonality.ensure(new TycoonData.Resident(id,identity));identity.putBoolean("OriginalGhostHelper",false);return identity;
    }
    public static TycoonNpc find(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!GhostVictimMirrors.owned(p,s,n)||!n.hasUUID("VictimActor"))return null;
        if(n.contains("ActorPosition")&&!n.getString("ActorDimension").isEmpty()){
            var level=com.coraline.parents.FamilyData.level(p.server,n.getString("ActorDimension"));
            if(level!=null)LoopProps.hold(level,BlockPos.of(n.getLong("ActorPosition")));
        }
        for(var level:p.server.getAllLevels())if(level.getEntity(n.getUUID("VictimActor")) instanceof TycoonNpc child){
            if(!owns(child,s,n))return null;remember(n,child);return child;
        }
        return null;
    }
    public static boolean owns(TycoonNpc actor,VisitData.Scene s,CompoundTag n){
        if(actor==null||s==null||!GhostVictimProgress.hasVictim(n)||!n.hasUUID("VictimActor")
            ||!actor.getUUID().equals(n.getUUID("VictimActor"))||!s.owner.equals(actor.owner)
            ||!GhostVictimProgress.child(n).getUUID("Id").equals(actor.resident))return false;
        var lease=actor.getPersistentData().getCompound(LEASE);
        return lease.hasUUID("Owner")&&s.owner.equals(lease.getUUID("Owner"))
            &&lease.hasUUID("Door")&&s.door.equals(lease.getUUID("Door"))
            &&n.hasUUID("Session")&&lease.hasUUID("Session")&&n.getUUID("Session").equals(lease.getUUID("Session"));
    }
    public static TycoonNpc arrive(ServerPlayer p,VisitData.Scene s,CompoundTag n,ServerLevel level,BlockPos feet){
        if(!GhostVictimMirrors.owned(p,s,n)||!GhostVictimProgress.hasVictim(n)||!n.hasUUID("Session")||level==null||feet==null)return null;
        if(n.hasUUID("VictimActor"))return find(p,s,n);
        var child=TycoonContent.NPC.get().create(level);if(child==null)return null;
        // The story child ID is also its original entity UUID. Persisted identity
        // therefore cannot silently become a second actor after a lost callback.
        child.setUUID(GhostVictimProgress.child(n).getUUID("Id"));
        level.getChunkAt(feet);child.setPos(Vec3.atBottomCenterOf(feet));
        if(!GhostVictimNavigation.clear(level,child,feet))return null;
        child.configure(s.owner,GhostVictimProgress.child(n).getUUID("Id"),false,GhostVictimProgress.child(n).getInt("Appearance"));
        child.setCustomName(Component.literal(GhostVictimProgress.child(n).getString("Name")));child.setCustomNameVisible(false);
        var lease=new CompoundTag();lease.putUUID("Owner",s.owner);lease.putUUID("Door",s.door);lease.putUUID("Session",n.getUUID("Session"));
        child.getPersistentData().put(LEASE,lease);
        if(!level.addFreshEntity(child))return null;
        n.putUUID("VictimActor",child.getUUID());remember(n,child);OtherMotherLoop.changed(p);return child;
    }
    public static void remember(CompoundTag n,TycoonNpc actor){
        n.putLong("ActorPosition",actor.blockPosition().asLong());n.putString("ActorDimension",actor.level().dimension().location().toString());
    }
    /** Called before resident care. Only an exact current lease can bypass it. */
    public static boolean leased(TycoonNpc actor){
        if(actor.getServer()==null||!actor.getPersistentData().contains(LEASE))return false;
        var lease=actor.getPersistentData().getCompound(LEASE);if(!lease.hasUUID("Owner"))return false;
        var scene=VisitData.get(actor.getServer()).scenes.get(lease.getUUID("Owner"));if(scene==null)return false;
        var n=AcceptedButtonEyes.data(scene).getCompound(GhostVictimProgress.KEY);
        if(n.hasUUID("Session")&&lease.hasUUID("Session")&&n.getUUID("Session").equals(lease.getUUID("Session"))
            &&lease.hasUUID("Door")&&scene.door.equals(lease.getUUID("Door")))
            for(var entry:n.getList("VictimHistory",net.minecraft.nbt.Tag.TAG_COMPOUND)){
                var retired=(CompoundTag)entry;
                if(retired.hasUUID("RetiredActor")&&actor.getUUID().equals(retired.getUUID("RetiredActor"))){actor.setInvisible(true);actor.discard();return true;}
            }
        if(!n.getBoolean("Started")||!owns(actor,scene,n))return false;
        var player=actor.getServer().getPlayerList().getPlayer(scene.owner);
        if(player==null){actor.getNavigation().stop();actor.setSprinting(false);}else remember(n,actor);
        return true;
    }
    public static void release(TycoonNpc actor){
        actor.getNavigation().stop();actor.setSprinting(false);actor.setPose(Pose.STANDING);actor.refreshDimensions();
        actor.gesture(0,0);actor.getPersistentData().remove(LEASE);actor.setInvisible(false);actor.noPhysics=false;
    }
    private GhostVictimActors(){}
}
