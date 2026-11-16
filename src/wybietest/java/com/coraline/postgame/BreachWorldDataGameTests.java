package com.coraline.postgame;

import java.nio.file.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.*;

/** Actual global dormant SavedData and wrong-level activation refusal only;
 * no Moonstone transaction, running Drive, real boss or multiplayer claim. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class BreachWorldDataGameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void oneDormantGlobalStoreDoesNotActivateFromOriginalWorldOrReading(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var store=BreachWorldData.get(server);var original=store.view();
        h.assertTrue(!original.active()&&original.blocker().isEmpty()&&original.millionths()==0&&original.fuelTicks()==0&&original.lastGameTick()==-1,"Fresh store is not dormant/empty");
        h.assertTrue(!store.activateFromBoundLevel(h.getLevel())&&!store.advanceFromBoundLevel(h.getLevel())&&store.view().equals(original),"Original-world access activated or advanced Breach");
        var other=server.getLevel(com.coraline.otherworld.OtherWorld.KEY);h.assertTrue(other!=null,"Original Other World fixture absent");
        h.assertTrue(BreachWorldData.get(other.getServer())==store&&!store.activateFromBoundLevel(other)&&store.view().equals(original),"Another original dimension obtained/activated a second store");
        h.assertTrue(!store.isDirty(),"Read-only/denied interaction marked dormant storage for mutation");
        store.setDirty();server.overworld().getDataStorage().save();
        var file=server.getWorldPath(LevelResource.ROOT).resolve("data/coraline_overworld_breach.dat");h.assertTrue(Files.isRegularFile(file),"Actual SavedData was not written under the global Overworld data directory");
        var disk=com.coraline.rebirth.StoryRunStore.read(file);h.assertTrue(disk.contains("data",Tag.TAG_COMPOUND),"Native SavedData root missing");var n=disk.getCompound("data");
        h.assertTrue(n.getInt("Version")==1&&!n.getBoolean("Activated")&&!n.contains("Generation")&&!n.contains("Objectives")&&!n.contains("Fuel"),"Dormant disk state grew live progression from a read/foreign-world access");
        h.assertTrue(!store.isDirty()&&store.view().equals(original)&&BreachWorldData.get(server)==store,"Disk checkpoint created a second or active world-global store");h.succeed();
    }
}
