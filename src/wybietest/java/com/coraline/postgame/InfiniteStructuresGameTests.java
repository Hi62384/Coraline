package com.coraline.postgame;

import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraftforge.gametest.*;

/** Real registry/codecs and bounded placement/NBT/loot components. No enabled
 * exploration dimension, custom defenders or complete structure quests. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class InfiniteStructuresGameTests {
    private static final UUID EPOCH=UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final long SEED=124119;
    private static final Map<InfiniteStructurePlan.Family,InfiniteStructurePlan.Plan> PLANS=new EnumMap<>(InfiniteStructurePlan.Family.class);
    private static InfiniteStructurePlan.Plan plan(InfiniteStructurePlan.Family family){
        return PLANS.computeIfAbsent(family,key->{for(int x=-12288;x<=12288;x+=768)for(int z=-12288;z<=12288;z+=768){var p=InfiniteStructurePlan.at(EPOCH,SEED,key,x,z);if(p.isPresent())return p.orElseThrow();}throw new AssertionError("No terrain-compatible fixture: "+family);});
    }
    private static InfiniteChunkGenerator generator(GameTestHelper h)throws Exception{
        return generator(h,EPOCH,SEED);
    }
    private static InfiniteChunkGenerator generator(GameTestHelper h,UUID epoch,long seed)throws Exception{
        var resource=h.getLevel().getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("coraline","postgame/terrain.json")).orElseThrow();
        try(var reader=resource.openAsReader()){
            var raw=ChunkGenerator.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess()),JsonParser.parseReader(reader)).getOrThrow(false,h::fail);
            h.assertTrue(raw instanceof InfiniteChunkGenerator,"Wrong actual generator codec");var g=(InfiniteChunkGenerator)raw;
            h.assertTrue(g.bind(new InfiniteGeneration(epoch,seed)),"Fixture generator binding failed");return g;
        }
    }
    private static IndependentStructureSpread spread(GameTestHelper h){
        var raw=StructurePlacement.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"type\":\"coraline:independent_spread\",\"spacing\":48,\"separation\":24,\"salt\":18237448}")).getOrThrow(false,h::fail);
        h.assertTrue(raw instanceof IndependentStructureSpread,"Placement decoded as vanilla/another type");return (IndependentStructureSpread)raw;
    }
    @GameTest(template="empty")
    public static void threeActualFamiliesAndOwnPlacementSetLoad(GameTestHelper h){
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var set=h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.fromNamespaceAndPath("coraline","infinite_sites"));
        h.assertTrue(set!=null&&set.placement() instanceof IndependentStructureSpread&&set.structures().size()==20,"Full twenty-family independent structure set missing or includes unexpected families");
        for(var family:InfiniteStructurePlan.Family.values()){
            var id=ResourceLocation.fromNamespaceAndPath("coraline",family.id());var value=registry.get(id);
            h.assertTrue(value instanceof InfiniteSiteStructure,"Family failed actual data/type load: "+id);
            h.assertTrue(set.structures().stream().anyMatch(entry->entry.structure().value()==value),"Family absent from real placement set: "+id);
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void independentPlacementIgnoresOverworldSeedAndRejectsRebinding(GameTestHelper h){
        var placement=spread(h);boolean blocked=false;
        try{placement.getPotentialStructureChunk(0,0,0);}catch(IllegalStateException expected){blocked=true;}
        h.assertTrue(blocked,"Unbound placement invented an Overworld seed");
        var generation=new InfiniteGeneration(EPOCH,SEED);h.assertTrue(placement.bind(generation)&&placement.bind(generation),"Same-generation bind was not idempotent");
        var other=spread(h);h.assertTrue(other.bind(new InfiniteGeneration(EPOCH,-742971)),"Separate seed fixture could not bind");int different=0;
        for(int x=-500;x<=500;x+=37)for(int z=-500;z<=500;z+=37){
            var a=placement.getPotentialStructureChunk(1,x,z);var b=placement.getPotentialStructureChunk(999999,x,z);
            h.assertTrue(a.equals(b),"Overworld seed changed an independently seeded candidate");
            if(!a.equals(other.getPotentialStructureChunk(1,x,z)))different++;
            h.assertTrue(Math.floorDiv(a.x,48)==Math.floorDiv(x,48)&&Math.floorDiv(a.z,48)==Math.floorDiv(z,48),"Negative-coordinate placement escaped its cell");
        }
        h.assertTrue(different>500,"Independent seed did not change placement");
        h.assertTrue(!placement.bind(new InfiniteGeneration(UUID.randomUUID(),SEED))&&!placement.bind(new InfiniteGeneration(EPOCH,SEED+1)),"Existing placement silently changed epoch/seed");
        var encoded=StructurePlacement.CODEC.encodeStart(JsonOps.INSTANCE,placement).getOrThrow(false,h::fail).toString();
        h.assertTrue(!encoded.contains(EPOCH.toString())&&!encoded.contains("\"seed\""),"Datapack codec replaced authoritative saved generation binding");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void mapStatesBelongToTheirGeneratorInsteadOfSharedRegistryData(GameTestHelper h)throws Exception{
        var access=h.getLevel().registryAccess();var lookup=access.registryOrThrow(Registries.STRUCTURE_SET).asLookup();
        var holder=access.registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE,ResourceLocation.fromNamespaceAndPath("coraline","doll_workshop")));
        var random=h.getLevel().getChunkSource().randomState();var first=generator(h);
        var oldState=first.createState(lookup,random,900000);var oldList=oldState.getPlacementsForStructure(holder);
        h.assertTrue(oldList.size()==1&&oldList.get(0) instanceof IndependentStructureSpread,"Native map state lost its owned independent spread");
        var old=(IndependentStructureSpread)oldList.get(0);var before=old.getPotentialStructureChunk(0,73,-61);
        var second=generator(h,UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),SEED+1);
        var next=second.createState(lookup,random,-991).getPlacementsForStructure(holder);
        h.assertTrue(next.size()==1&&next.get(0)!=old,"Different generations shared a mutable placement instance");
        h.assertTrue(before.equals(old.getPotentialStructureChunk(99999,73,-61)),"New generation retargeted an old world's placement");
        h.assertTrue(!first.bind(new InfiniteGeneration(UUID.randomUUID(),SEED+1)),"Old generator accepted a reset without retirement");
        var definition=access.registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.fromNamespaceAndPath("coraline","infinite_sites"));boolean unbound=false;
        try{((IndependentStructureSpread)definition.placement()).getPotentialStructureChunk(0,0,0);}catch(IllegalStateException expected){unbound=true;}
        h.assertTrue(unbound,"World binding mutated shared registry data");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void realFamilyLootTablesContainMaterialsWithoutHeartOrStoryCredentials(GameTestHelper h){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SiteLootQA"));
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,player.position()).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,player).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        for(String name:List.of("doll_workshop","clockwork_factory","soul_glass_mine","common_supply","thread_workroom")){
            var table=h.getLevel().getServer().getLootData().getLootTable(ResourceLocation.fromNamespaceAndPath("coraline","postgame/structures/"+name));
            h.assertTrue(table!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Missing actual structure loot: "+name);
            var found=new HashSet<String>();
            for(int seed=1;seed<=1000;seed++){
                var loot=table.getRandomItems(params,seed);h.assertTrue(!loot.isEmpty(),"Empty structure supply roll: "+name);
                for(var stack:loot){
                    var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();found.add(id);
                    h.assertFalse(id.equals("coraline:void_heart")||id.equals("coraline:other_void_sword")||id.equals("coraline:soul_capturer")||id.equals("coraline:ghost_eye")||id.startsWith("coraline:other_world_void_"),"Structure bypassed unique/Heart progression: "+id);
                    var metadata=stack.getTag()==null?new CompoundTag():stack.getTag().copy();if(stack.isDamageableItem())metadata.remove("Damage");h.assertTrue(metadata.isEmpty(),"Ordinary structure loot minted story credentials");
                }
            }
            h.assertTrue(found.contains("coraline:button")&&found.contains("coraline:other_iron_ingot"),"Structure lacks useful recurring materials: "+name);
            if(name.equals("soul_glass_mine"))h.assertTrue(found.contains("coraline:soulglass"),"Soul Glass mine lacks its actual resource payout");
            if(name.equals("common_supply"))h.assertTrue(found.equals(Set.of("coraline:button","coraline:other_iron_ingot","minecraft:string")),"Unprotected entrance cache bypassed guarded treasure");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void actualPieceNbtPreservesIdentityJournalAndUnknownVersions(GameTestHelper h){
        var context=StructurePieceSerializationContext.fromLevel(h.getLevel());
        for(var family:InfiniteStructurePlan.Family.values()){
            var p=plan(family);
            for(int i=0;i<p.modules().size();i++){
                var saved=new InfiniteSitePiece(p,i).createTag(context);saved.putString("ExternalExtension","preserve");
                if(i==0)saved.getCompound("Journal").putUUID("PendingObjective",UUID.randomUUID());
                var copy=new InfiniteSitePiece(saved).createTag(context);
                h.assertTrue(copy.equals(saved),"Piece reload lost identity, extension or root journal: "+family+" / "+i);
                saved.putInt("SiteVersion",999);saved.putString("UnknownFutureState","never reset");
                h.assertTrue(new InfiniteSitePiece(saved).createTag(context).equals(saved),"Future structure metadata was erased/reinterpreted");
            }
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void realStructureStartRequiresItsIndependentGeneratorAndSeed(GameTestHelper h)throws Exception{
        var g=generator(h);var structure=h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.fromNamespaceAndPath("coraline","doll_workshop"));
        h.assertTrue(structure instanceof InfiniteSiteStructure,"Registered workshop structure missing");
        ChunkPos candidate=null;var placement=spread(h);placement.bind(new InfiniteGeneration(EPOCH,SEED));
        for(int x=-8;x<=8&&candidate==null;x++)for(int z=-8;z<=8;z++){
            var chunk=placement.getPotentialStructureChunk(0,x*48,z*48);
            if(InfiniteStructurePlan.at(EPOCH,SEED,InfiniteStructurePlan.Family.DOLL_WORKSHOP,chunk.getMiddleBlockX(),chunk.getMiddleBlockZ()).isPresent()){candidate=chunk;break;}
        }
        h.assertTrue(candidate!=null,"No real eligible random-spread workshop candidate");
        var level=h.getLevel();var height=LevelHeightAccessor.create(InfiniteTerrain.MIN_Y,InfiniteTerrain.HEIGHT);
        var start=structure.generate(level.registryAccess(),g,g.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),SEED,candidate,0,height,structure.biomes()::contains);
        h.assertTrue(start.isValid()&&start.getPieces().size()==7,"Native structure start did not contain the complete saved workshop layout");
        var wrongSeed=structure.generate(level.registryAccess(),g,g.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),SEED+1,candidate,0,height,structure.biomes()::contains);
        h.assertTrue(!wrongSeed.isValid(),"Overworld/foreign seed produced a valid independent structure start");
        var original=level.getChunkSource().getGenerator();
        var wrongWorld=structure.generate(level.registryAccess(),original,original.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),SEED,candidate,0,height,holder->true);
        h.assertTrue(!wrongWorld.isValid(),"Postgame structure leaked into original/vanilla world generation");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void nativeModuleClipsToChunksAndCollectedChestDoesNotRefill(GameTestHelper h)throws Exception{
        var p=plan(InfiniteStructurePlan.Family.DOLL_WORKSHOP);int index=-1;
        for(int i=0;i<p.modules().size();i++)if(p.modules().get(i).role()==InfiniteStructurePlan.Role.HALL){index=i;break;}
        h.assertTrue(index>=0,"Workshop has no actual entrance hall");var module=p.modules().get(index);var piece=new InfiniteSitePiece(p,index);var generator=generator(h);
        var box=piece.getBoundingBox();var chestAt=new BlockPos(p.x()+module.x()+1,p.y()+module.y()+1,p.z()+module.z()+1);
        var clips=new ArrayList<BoundingBox>();
        for(int x=Math.floorDiv(box.minX(),16);x<=Math.floorDiv(box.maxX(),16);x++)for(int z=Math.floorDiv(box.minZ(),16);z<=Math.floorDiv(box.maxZ(),16);z++){
            h.getLevel().getChunk(x,z);clips.add(new BoundingBox(x*16,h.getLevel().getMinBuildHeight(),z*16,x*16+15,h.getLevel().getMaxBuildHeight()-1,z*16+15));
        }
        var sentinel=new BlockPos(box.minX()-1,box.minY(),box.minZ());h.getLevel().setBlock(sentinel,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        for(var clip:clips)piece.postProcess(h.getLevel(),h.getLevel().structureManager(),generator,RandomSource.create(1),clip,new ChunkPos(clip.minX()>>4,clip.minZ()>>4),BlockPos.ZERO);
        h.assertTrue(h.getLevel().getBlockState(sentinel).is(Blocks.DIAMOND_BLOCK),"Piece wrote beyond its bounding box");
        h.assertTrue(h.getLevel().getBlockEntity(chestAt) instanceof ChestBlockEntity,"Actual clipped placement did not create a chest");
        var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(chestAt);var unopened=chest.saveWithFullMetadata();
        h.assertTrue(unopened.contains("LootTable")&&unopened.getString("LootTable").equals("coraline:postgame/structures/common_supply"),"Normal once-only lazy table not assigned");
        boolean any=false;for(int slot=0;slot<chest.getContainerSize();slot++)if(!chest.getItem(slot).isEmpty())any=true;
        h.assertTrue(any,"Actual workshop loot table was empty/missing");chest.clearContent();var collected=chest.saveWithFullMetadata();
        h.assertTrue(!collected.contains("LootTable"),"Collected chest retained a fresh reward table");
        var restored=new InfiniteSitePiece(piece.createTag(StructurePieceSerializationContext.fromLevel(h.getLevel())));Collections.reverse(clips);
        for(var clip:clips)restored.postProcess(h.getLevel(),h.getLevel().structureManager(),generator,RandomSource.create(999),clip,new ChunkPos(clip.minX()>>4,clip.minZ()>>4),BlockPos.ZERO);
        for(var guard:InfiniteSiteGuards.assignments(p,index))h.assertTrue(h.getLevel().getEntity(guard.id())==null,"Caller-supplied generator seeded postgame guards into the original world");
        h.assertTrue(h.getLevel().getBlockEntity(chestAt)==chest&&chest.isEmpty()&&!chest.saveWithFullMetadata().contains("LootTable"),"Piece reload/reversed chunk callbacks refilled or replaced a collected container");
        h.succeed();
    }
}
