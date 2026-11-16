package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Actual protected-world admission and same-display reuse, not a story outcome. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostManifestationAdmissionGameTests {
    @GameTest(template="empty",timeoutTicks=400) public static void ownedManifestationIsAdmittedWhileUnmarkedVanillaDisplayRemainsBlocked(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());var l=p.server.getLevel(OtherWorld.KEY);
        h.assertTrue(l!=null,"Actual Other World is unavailable");var at=new Vec3(32000.5,120,32000.5);var chunk=new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(at));boolean wasForced=l.getForcedChunks().contains(chunk.toLong());l.setChunkForced(chunk.x,chunk.z,true);l.getChunkAt(net.minecraft.core.BlockPos.containing(at));
        var n=new CompoundTag();GhostVictimProgress.begin(n,"DECLARED_DISPLAY_ADMISSION_FIXTURE");n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putUUID("Session",java.util.UUID.randomUUID());
        int[] waited={0};boolean[] started={false};h.onEachTick(()->{if(started[0])return;if(!l.areEntitiesLoaded(chunk.toLong())){if(++waited[0]>=280){if(!wasForced)l.setChunkForced(chunk.x,chunk.z,false);AcceptedBranchPlayers.remove(p,s);h.fail("Fixture entity section did not load within its bounded setup wait");}return;}started[0]=true;try{
            var unmarked=EntityType.ITEM_DISPLAY.create(l);unmarked.setPos(at);
            h.assertTrue(!l.addFreshEntity(unmarked),"Protected world admitted an unrelated unmarked vanilla display");
            h.assertTrue(GhostVictimManifestations.displayAt(p,s,n,l,"supper_bottle",at,GhostVictimProgress.Disturbance.OBJECT,new ItemStack(Items.GLASS_BOTTLE)),"Authored owned manifestation was rejected by native protection");
            var id=n.getCompound("Points").getCompound("supper_bottle").getUUID("Entity");
            h.assertTrue(l.getEntity(id)!=null,"Authored display was missing immediately after native admission: "+id);
            h.runAfterDelay(3,()->{try{
                var original=l.getEntity(id);h.assertTrue(original!=null,"Native entity sections lost the authored display UUID "+id);h.assertTrue(original.getPersistentData().getBoolean("CoralineDecoration"),"Authored display lost its decoration flag: "+original.getPersistentData());
                h.assertTrue(s.owner.equals(original.getPersistentData().getUUID("GhostManifestationOwner"))&&s.door.equals(original.getPersistentData().getUUID("GhostManifestationDoor"))&&n.getUUID("Session").equals(original.getPersistentData().getUUID("GhostManifestationSession")),"Display lost owner/door/session identity");
                h.assertTrue(GhostVictimManifestations.displayAt(p,s,n,l,"supper_bottle",at,GhostVictimProgress.Disturbance.OBJECT,new ItemStack(Items.GLASS_BOTTLE)),"Existing authored display could not be reused");
                h.assertTrue(id.equals(n.getCompound("Points").getCompound("supper_bottle").getUUID("Entity"))&&n.getCompound("Points").size()==1,"Reconstruction retry duplicated a display");
                h.succeed();
            }finally{var original=l.getEntity(id);if(original!=null)original.discard();if(!wasForced)l.setChunkForced(chunk.x,chunk.z,false);AcceptedBranchPlayers.remove(p,s);}});
        }catch(Throwable failure){if(!wasForced)l.setChunkForced(chunk.x,chunk.z,false);AcceptedBranchPlayers.remove(p,s);throw failure;}});
    }
    private GhostManifestationAdmissionGameTests(){}
}
