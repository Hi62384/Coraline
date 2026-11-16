package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;

/** Isolated unrun restoration regression; preserves earlier passing inventory fixtures. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostEscapeRestoredBodyGameTests {
    @GameTest(template="empty") public static void restoredBodyReleasesDoorSealAndLethalDamageProtection(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            var accepted=AcceptedButtonEyesProgress.data(s);AcceptedButtonEyesProgress.begin(accepted,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,0);accepted.putBoolean("SewingCompleted",true);
            var snapshot=new CompoundTag();snapshot.putUUID("Transaction",UUID.randomUUID());accepted.put("InventoryEscrow",snapshot);escapeForRestore(p,s);
            h.assertTrue(AcceptedButtonEyes.sealed(p),"Pending restoration released the accepted-story seal");
            var before=new net.minecraftforge.event.entity.living.LivingDamageEvent(p,p.damageSources().generic(),p.getMaxHealth()+1);AcceptedButtonEyes.damage(before);
            h.assertTrue(before.isCanceled(),"Unrestored story body lost its lethal-cutscene protection");
            var ghost=GhostVictimEntry.data(s);h.assertTrue(GhostEscapeProgress.restorationCommitted(ghost,snapshot.getUUID("Transaction"),true,true),"Fixture could not commit its restoration receipt");
            h.assertFalse(AcceptedButtonEyes.sealed(p),"Restored player remains sealed in the accepted story");
            var after=new net.minecraftforge.event.entity.living.LivingDamageEvent(p,p.damageSources().generic(),p.getMaxHealth()+1);AcceptedButtonEyes.damage(after);
            h.assertFalse(after.isCanceled(),"Restored player retained story immortality");
        }finally{com.coraline.otherworld.DoorData.get(p.server).doors.remove(s.door);AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    private static void escapeForRestore(net.minecraft.server.level.ServerPlayer p,VisitData.Scene s){
        var accepted=AcceptedButtonEyesProgress.data(s);var ghost=new CompoundTag();GhostVictimProgress.begin(ghost,"ACCEPTED_PROPOSAL");ghost.putUUID("Owner",s.owner);ghost.putUUID("Door",s.door);ghost.putUUID("Session",UUID.randomUUID());
        GhostVictimProgress.newVictim(ghost,UUID.randomUUID(),new CompoundTag());accepted.put(GhostVictimProgress.KEY,ghost);
        var escape=GhostEscapeProgress.data(ghost);escape.putUUID("Attempt",UUID.randomUUID());escape.putUUID("Victim",GhostVictimProgress.child(ghost).getUUID("Id"));escape.putUUID("Session",ghost.getUUID("Session"));
        escape.putBoolean("WagerWon",true);escape.putBoolean("PlayerEyesHeld",true);escape.putUUID("OriginalEscrow",accepted.getCompound("InventoryEscrow").getUUID("Transaction"));GhostEscapeProgress.move(escape,GhostEscapeProgress.Phase.RESTORING,0);
        var door=new com.coraline.otherworld.DoorData.Door();door.id=s.door;door.owner=s.owner;door.dimension=p.level().dimension().location().toString();door.pos=p.blockPosition();com.coraline.otherworld.DoorData.get(p.server).doors.put(s.door,door);
    }
    private GhostEscapeRestoredBodyGameTests(){}
}
