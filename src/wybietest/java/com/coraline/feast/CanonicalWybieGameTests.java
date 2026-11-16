package com.coraline.feast;

import java.util.UUID;
import com.coraline.palace.PalaceWybieIdentity;
import com.coraline.registry.CharacterCast;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.gametest.*;

/** Compile-only under the low-RAM workflow. Real native entity/remove/save
 * fixtures must still run before claiming preservation works in Minecraft. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CanonicalWybieGameTests {
    @GameTest(template="empty") public static void cleanupRetainsOriginalEntityAndUuid(GameTestHelper h){
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();
        var actor=CharacterCast.ENTITIES.get("wybie").get().create(h.getLevel());
        h.assertTrue(actor!=null,"Original cast unavailable");actor.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));
        var id=actor.getUUID();actor.getPersistentData().putUUID("CanonicalRealWybieOwner",s.owner);actor.getPersistentData().putUUID("CanonicalRealWybieDoor",s.door);
        s.loop.story.put("CanonicalRealWybie",PalaceWybieIdentity.create(s.owner,s.door,id,h.getLevel().dimension().location().toString(),actor.blockPosition()).save());
        VisitData.get(h.getLevel().getServer()).scenes.put(s.owner,s);h.getLevel().addFreshEntity(actor);
        actor.discard();h.assertFalse(actor.isRemoved(),"Legacy scene cleanup erased canonical Wybie");
        h.assertTrue(h.getLevel().getEntity(id)==actor&&CanonicalRealWybie.identity(s).stored(),"Stored identity is not the same entity");
        var n=new CompoundTag();h.assertTrue(actor.save(n),"Canonical entity did not serialize");
        var restored=net.minecraft.world.entity.EntityType.loadEntityRecursive(n,h.getLevel(),e->e);
        h.assertTrue(restored!=null&&restored.getUUID().equals(id),"Native entity NBT changed UUID");
        // Tear down only this fixture: remove its canonical tag first.
        actor.getPersistentData().remove("CanonicalRealWybieOwner");actor.discard();VisitData.get(h.getLevel().getServer()).scenes.remove(s.owner);h.succeed();
    }
    @GameTest(template="empty") public static void storyDeathArchivesRatherThanDeletes(GameTestHelper h){
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();
        var actor=CharacterCast.ENTITIES.get("wybie").get().create(h.getLevel());h.assertTrue(actor!=null,"Original unavailable");actor.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));
        actor.getPersistentData().putUUID("CanonicalRealWybieOwner",s.owner);actor.getPersistentData().putUUID("CanonicalRealWybieDoor",s.door);
        s.loop.story.put("CanonicalRealWybie",PalaceWybieIdentity.create(s.owner,s.door,actor.getUUID(),h.getLevel().dimension().location().toString(),actor.blockPosition()).save());
        VisitData.get(h.getLevel().getServer()).scenes.put(s.owner,s);h.getLevel().addFreshEntity(actor);
        actor.remove(Entity.RemovalReason.KILLED);
        var saved=CanonicalRealWybie.identity(s);h.assertFalse(actor.isRemoved(),"Death deleted original");
        h.assertTrue(saved.fate()==PalaceWybieIdentity.Fate.DEAD&&saved.stored()&&!saved.activate(actor.getUUID()),"Death lost tombstone or permitted resurrection");
        h.assertTrue(s.loop.story.getCompound("CanonicalRealWybieSnapshot").hasUUID("UUID"),"Original snapshot missing");
        actor.getPersistentData().remove("CanonicalRealWybieOwner");actor.discard();VisitData.get(h.getLevel().getServer()).scenes.remove(s.owner);h.succeed();
    }
}
