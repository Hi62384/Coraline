package com.coraline.palace;

import com.coraline.feast.CinematicPropEntity;
import com.coraline.otherworld.RosewoodHouse;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;

/** Compile-only until native verification is authorized. Not a generated-world,
 * chunk-restart, terrain/lighting, structure-preservation or visual certificate.
 */
@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class PalaceGenerationGameTests {
    @GameTest(template="empty") public static void originalVictorianMasterIsFurnishedUniqueAndPortalFree(GameTestHelper h){
        var cells=RosewoodHouse.palaceCells();var unique=new java.util.HashSet<net.minecraft.core.BlockPos>();int fixtures=0,roof=0;
        for(var c:cells){h.assertTrue(unique.add(c.offset()),"Compacted house export repeats a final block position");
            h.assertFalse(c.state().is(com.coraline.registry.ModBlocks.TINY_DOOR.get()),"Overworld import fabricated an active Little Door");
            if(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(c.state().getBlock()).getPath().startsWith("rosewood_"))fixtures++;
            if(c.state().is(net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES))roof++;
        }
        h.assertTrue(cells.size()>25_000&&fixtures>50&&roof>50,"Original furnished Victorian master was replaced by a generic shell");h.succeed();
    }
    @GameTest(template="empty") public static void sharedSignPersistsWithoutAnotherOwnerOrScene(GameTestHelper h){
        var a=CinematicPropEntity.TYPE.get().create(h.getLevel());var b=CinematicPropEntity.TYPE.get().create(h.getLevel());
        h.assertTrue(a!=null&&b!=null,"Existing scenery entity type missing");a.worldSign(1);var data=a.saveWithoutId(new CompoundTag());b.load(data);
        h.assertTrue(b.kind()==CinematicPropEntity.WORLD_SIGN&&b.slot()==1&&b.owner==null&&b.door==null,"Shared company sign became an owned quest actor on reload");
        h.assertTrue(b.getPersistentData().getBoolean("CoralineWorldLandmark"),"Shared sign provenance lost");h.succeed();
    }
    @GameTest(template="empty") public static void incompleteOrFutureWorldJobsCannotDeliverAMap(GameTestHelper h){
        var d=new PalaceSiteData();d.initialize(42,new net.minecraft.core.BlockPos(0,70,0));
        h.assertFalse(d.ready(),"Empty world treated as completed generation");
        var saved=d.save(new CompoundTag());var future=new CompoundTag();future.putInt("Version",2);future.putString("Stage","DONE");saved.put("Generation",future);
        var loaded=PalaceSiteData.load(saved);h.assertTrue(!loaded.blocker().isEmpty()&&!loaded.ready(),"Future job bypassed generation receipt gates");
        h.assertTrue(loaded.save(new CompoundTag()).getCompound("Generation").equals(future),"Future job evidence discarded");h.succeed();
    }
    @GameTest(template="empty") public static void legacyPrecommitPauseReleasesClaimWithoutResettingCandidateSearch(GameTestHelper h){
        var sites=new PalaceSiteData();
        sites.initialize(42,new net.minecraft.core.BlockPos(0,70,0));
        var kind=PalaceSitePlan.Kind.PALACE;
        var original=sites.proposal(kind).orElseThrow();
        var checkpoint=sites.generation();
        checkpoint.putBoolean("PalaceClaimed",true);
        checkpoint.putInt("CandidateX",original.x());
        checkpoint.putInt("CandidateZ",original.z());
        checkpoint.putInt("ProfileBase",100);
        PalaceGenerationRules.enter(checkpoint,PalaceGenerationRules.Stage.SURVEY_REGION);
        sites.stopGeneration("Player exploration reached the pending region; job stopped to preserve explored terrain/builds. No replacement Palace allocated.");
        var reload=PalaceSiteData.load(sites.save(new CompoundTag()));
        reload.retryLegacyPause();
        h.assertTrue(reload.blocker().isEmpty()&&reload.generation().isEmpty(),"World could not continue after legacy pause");
        h.assertTrue(reload.proposal(kind).filter(original::equals).isEmpty(),"Generation returned to rejected terrain");
        h.succeed();
    }
    @GameTest(template="empty") public static void terrainCursorCoversEveryColumnInChunkOrder(GameTestHelper h){
        int side=PalaceTerrainPlan.EXTENT*2+1,minX=-119,minZ=27;
        var covered=new java.util.BitSet(side*side);
        var visitedChunks=new java.util.HashSet<Long>();
        long previous=Long.MIN_VALUE;
        for(int cursor=0;cursor<side*side;cursor++){
            var at=PalaceTerrainPlan.column(minX,minZ,cursor);
            int x=at.x()-minX,z=at.z()-minZ;
            if(x<0||z<0||x>=side||z>=side||covered.get(z*side+x))throw new AssertionError("Invalid terrain cursor "+cursor);
            covered.set(z*side+x);
            long chunk=net.minecraft.world.level.ChunkPos.asLong(at.x()>>4,at.z()>>4);
            if(chunk!=previous&&!visitedChunks.add(chunk))throw new AssertionError("Returned to an earlier chunk");
            previous=chunk;
        }
        h.assertTrue(covered.cardinality()==side*side,"Chunk order omitted terrain columns");
        h.succeed();
    }
}
