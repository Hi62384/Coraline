package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original flags live on the real actor as well as the scene, so unloading never destroys rollback data. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PartyShowRestoration {
    static final String KEY="PartyShowOriginal";
    private static boolean same(CompoundTag n,VisitData.Scene s,String show){return n.hasUUID("Owner")&&s.owner.equals(n.getUUID("Owner"))&&n.hasUUID("Door")&&s.door.equals(n.getUUID("Door"))&&n.getInt("Serial")==s.loop.event.getInt("Serial")&&show.equals(n.getString("Show"));}
    static boolean capture(ServerPlayer p,VisitData.Scene s,String show,Mob actor){
        var tag=actor.getPersistentData();if(!s.owner.equals(p.getUUID())||!tag.hasUUID("PartyOwner")||!s.owner.equals(tag.getUUID("PartyOwner")))return false;
        if(tag.contains(KEY))return same(tag.getCompound(KEY),s,show);
        var n=new CompoundTag();n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putInt("Serial",s.loop.event.getInt("Serial"));n.putString("Show",show);n.putUUID("Entity",actor.getUUID());n.putBoolean("NoAI",actor.isNoAi());n.putBoolean("Gravity",actor.isNoGravity());n.putBoolean("Physics",actor.noPhysics);tag.put(KEY,n);return true;
    }
    static boolean restore(ServerPlayer p,VisitData.Scene s,String show,Mob actor,CompoundTag legacy){
        if(!s.owner.equals(p.getUUID()))return false;var tag=actor.getPersistentData();
        if(tag.hasUUID("PartyOwner")&&!s.owner.equals(tag.getUUID("PartyOwner")))return false;
        if(tag.contains(KEY)){if(!same(tag.getCompound(KEY),s,show))return false;apply(actor,tag.getCompound(KEY));tag.remove(KEY);return true;}
        // Backward-compatible recovery of an older staged save which only has the scene's actor ledger.
        apply(actor,legacy);return true;
    }
    private static void apply(Mob actor,CompoundTag n){actor.setNoAi(n.getBoolean("NoAI"));actor.setNoGravity(n.getBoolean("Gravity"));actor.noPhysics=n.getBoolean("Physics");ShowPlaybackNetwork.pose(actor,0,0,1);}
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof Mob actor)||!actor.getPersistentData().contains(KEY))return;
        var tag=actor.getPersistentData();var n=tag.getCompound(KEY);if(!n.hasUUID("Owner")){return;}var server=actor.getServer();if(server==null)return;var s=VisitData.get(server).scenes.get(n.getUUID("Owner"));
        // A legitimate vanilla dimension transfer may replace the runtime entity binding. Keep the rollback ledger exact.
        if(s!=null&&same(n,s,n.getString("Show"))&&n.hasUUID("Entity"))for(var value:s.loop.event.getCompound(n.getString("Show")).getList("Actors",10)){var entry=(CompoundTag)value;if(entry.hasUUID("Entity")&&entry.getUUID("Entity").equals(n.getUUID("Entity")))entry.putUUID("Entity",actor.getUUID());}
        n.putUUID("Entity",actor.getUUID());var owner=s==null?null:server.getPlayerList().getPlayer(s.owner);
        if(s!=null&&owner!=null&&OtherMotherLoop.active(owner)==s&&same(n,s,n.getString("Show"))&&HouseParty.data(s).getBoolean("Pending")&&!HouseParty.data(s).getBoolean("Suspended")&&!s.loop.event.getBoolean("ShowCleanupDeferred")){
            var show=s.loop.event.getCompound(n.getString("Show"));if(show.getBoolean("Playing")&&!show.getBoolean("Resume")&&!show.getBoolean("RestorePending"))return;
        }
        apply(actor,n);tag.remove(KEY);
    }
    private PartyShowRestoration(){}
}
