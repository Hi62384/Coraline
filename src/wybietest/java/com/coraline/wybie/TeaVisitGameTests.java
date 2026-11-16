package com.coraline.wybie;
import com.coraline.tea.*;
import com.coraline.bobinsky.*;
import com.coraline.feast.*;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.util.UUID;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TeaVisitGameTests {
    @GameTest(template="empty") public static void houseWaitsUntilPlayerLooksAway(GameTestHelper h){var c=h.absolutePos(new BlockPos(50,100,0));var v=new CompoundTag();v.putLong("House",c.asLong());v.putBoolean("DetailedTeaHouse",true);var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LookQA"));p.setPos(c.getX()+.5,c.getY(),c.getZ()-14);p.setYRot(0);p.setXRot(0);h.assertFalse(BobinskyHouse.unseen(p,v),"House disappears while looking toward it");p.setYRot(180);h.assertTrue(BobinskyHouse.unseen(p,v),"House cannot retire when player looks away");p.setPos(c.getX(),c.getY(),c.getZ());h.assertFalse(BobinskyHouse.unseen(p,v),"House can retire around a player still inside");h.succeed();}
    @GameTest(template="empty") public static void actorsLiftOutOfUnevenGround(GameTestHelper h){
        var c=h.absolutePos(new BlockPos(55,120,0));loadFixtureChunks(h,c);
        // Distant chunks are promoted into the collision cache on a subsequent
        // server tick. A synchronous test otherwise intermittently sees no stone.
        h.runAfterDelay(5,()->{
            // The test grid can place this distant fixture below generated
            // terrain. Test one uneven block, not an arbitrary deep hillside.
            var original=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=8;y++){
                var p=c.offset(x,y,z);original.put(p,h.getLevel().getBlockState(p));h.getLevel().setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            }
            h.getLevel().setBlock(c,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            var e=com.coraline.registry.ModEntities.BOBINSKY_VISITOR.get().create(h.getLevel());e.setPos(c.getX()+.5,c.getY()+.2,c.getZ()+.5);
            h.assertFalse(h.getLevel().noCollision(e,e.getBoundingBox()),"Grounding fixture collision isn't loaded");
            VisitGrounding.lift(e);h.assertTrue(e.getY()>=c.getY()+1&&h.getLevel().noCollision(e,e.getBoundingBox()),"Bobinsky remains buried in terrain at "+c+" entityY="+e.getY());
            e.setNoGravity(true);e.setPos(c.getX()+.5,c.getY()+.2,c.getZ()+.5);VisitGrounding.lift(e);h.assertTrue(e.getY()<c.getY()+.3,"Intentional air choreography was snapped");
            e.discard();for(var entry:original.entrySet())h.getLevel().setBlock(entry.getKey(),entry.getValue(),2);h.succeed();
        });
    }
    private static void loadFixtureChunks(GameTestHelper h,BlockPos c){for(int x=-16;x<=16;x+=16)for(int z=-16;z<=16;z+=16)h.getLevel().getChunkAt(c.offset(x,0,z));}
    // Manual ticks do not publish every native onGround flag transition. Assert
    // actual non-penetrating floor contact: clear at rest, colliding just below.
    @GameTest(template="empty",batch="tea-physics") public static void glassFallsFromCeilingAndBecomesNonItemFragments(GameTestHelper h){
        // This physical fixture is outside the tiny template. Keep it in its
        // own empty high-altitude chunk, away from other batch structures.
        var c=new BlockPos(46000,220,46000);loadFixtureChunks(h,c);var chunk=new net.minecraft.world.level.ChunkPos(c);
        for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++){h.getLevel().setChunkForced(x,z,true);h.getLevel().getChunk(x,z);}
        h.getLevel().setBlock(c.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=7;y++)h.getLevel().setBlock(c.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        h.runAfterDelay(40,()->{h.getLevel().setBlock(c.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);var owner=UUID.randomUUID();var token=UUID.randomUUID();var v=new CompoundTag();v.putUUID("Owner",owner);v.putUUID("Token",token);v.putBoolean("Active",true);var data=BobinskyVisitData.get(h.getLevel().getServer());data.teaVisits.put(owner,v);var e=com.coraline.registry.ModEntities.TEA_GLASS.get().create(h.getLevel());e.getPersistentData().putUUID("TeaOwner",owner);e.getPersistentData().putUUID("TeaToken",token);e.setPos(c.getX()+.5,c.getY()+5.6,c.getZ()+.5);for(int i=0;i<35;i++)e.tick();h.assertTrue(Math.abs(e.getY()-c.getY())<.01&&h.getLevel().noCollision(e,e.getBoundingBox())&&!h.getLevel().noCollision(e,e.getBoundingBox().move(0,-.02,0)),"Glass did not reach ground from roof height: y="+e.getY()+" floor="+h.getLevel().getBlockState(c.below()));e.shatter();h.assertTrue(e.broken()&&e.isNoGravity(),"Glass did not become stationary fragment geometry");e.discard();data.teaVisits.remove(owner);h.getLevel().setBlock(c.below(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++)h.getLevel().setChunkForced(x,z,false);h.succeed();});
    }
    @GameTest(template="empty") public static void detailedTeaHouseRestoresItsEntireFootprint(GameTestHelper h){
        var l=h.getLevel();var c=h.absolutePos(new BlockPos(35,180,0));var v=new CompoundTag();v.putLong("House",c.asLong());v.putBoolean("DetailedTeaHouse",true);v.putBoolean("Tea",true);v.putBoolean("Active",true);v.putUUID("Owner",UUID.randomUUID());
        l.setBlock(c.offset(8,0,9),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),2);v.put("Originals",BobinskyHouse.snapshot(l,c,TeaHouse.RX,TeaHouse.RZ));h.assertTrue(v.getList("Originals",10).size()==17*19*11,"Expanded house is not fully journalled");TeaHouse.build(l,v);h.assertTrue(BobinskyHouse.door(v).equals(c.north(9)),"Door still uses tiny-house dimensions");h.assertTrue(l.getBlockState(c.offset(-7,0,2)).is(net.minecraft.world.level.block.Blocks.SMOKER),"Separate kitchen missing");h.assertTrue(l.getBlockState(c.offset(0,1,1)).is(net.minecraft.world.level.block.Blocks.DARK_OAK_PRESSURE_PLATE),"Tea table missing");BobinskyHouse.restore(l,v);h.assertTrue(l.getBlockState(c.offset(8,0,9)).is(net.minecraft.world.level.block.Blocks.DIRT),"Expanded footprint terrain lost");l.setBlock(c.offset(8,0,9),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
    @GameTest(template="empty") public static void seatLocksDismountButReleasesAfterDrinking(GameTestHelper h){
        var seat=com.coraline.registry.ModEntities.TEA_SEAT.get().create(h.getLevel());var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SeatQA"));
        seat.policy(true,false);var event=new net.minecraftforge.event.entity.EntityMountEvent(p,seat,h.getLevel(),false);TeaSeatEntity.mount(event);h.assertTrue(event.isCanceled(),"Early chair dismount allowed");h.assertFalse(seat.canDrink(),"Tea can animate before prompt");seat.policy(true,true);h.assertTrue(seat.canDrink(),"Drink prompt not enabled");seat.policy(false,false);var release=new net.minecraftforge.event.entity.EntityMountEvent(p,seat,h.getLevel(),false);TeaSeatEntity.mount(release);h.assertFalse(release.isCanceled(),"Chair trapped player after tea");seat.discard();h.succeed();
    }
    @GameTest(template="empty") public static void spinkFacesTheGreetingPlayer(GameTestHelper h){
        var e=com.coraline.registry.ModEntities.SPINK_VISITOR.get().create(h.getLevel());var target=com.coraline.registry.ModEntities.FORCIBLE_VISITOR.get().create(h.getLevel());e.setPos(0,0,0);target.setPos(0,0,-3);TeaVisit.face(e,target);h.assertTrue(Math.abs(e.getYRot()+180)<.01&&Math.abs(e.yBodyRot-e.getYHeadRot())<.01,"Greeting face/body rotation not synchronized");e.discard();target.discard();h.succeed();
    }
    @GameTest(template="empty") public static void cancelledVisitPropsAreRejectedOnChunkReload(GameTestHelper h){
        var owner=UUID.randomUUID();var token=UUID.randomUUID();var v=new CompoundTag();v.putUUID("Owner",owner);v.putUUID("Token",token);v.putBoolean("Active",false);var data=BobinskyVisitData.get(h.getLevel().getServer());data.teaVisits.put(owner,v);
        var item=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),0,100,0,new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.BROKEN_GLASS.get()));item.getPersistentData().putUUID("SceneOwner",owner);item.getPersistentData().putUUID("SceneToken",token);item.getPersistentData().putBoolean("TeaProp",true);
        var stale=new net.minecraftforge.event.entity.EntityJoinLevelEvent(item,h.getLevel(),true);BobinskyHouseProtection.loaded(stale);h.assertTrue(stale.isCanceled(),"Cancelled glass props returned after reload");v.putBoolean("Active",true);var valid=new net.minecraftforge.event.entity.EntityJoinLevelEvent(item,h.getLevel(),true);BobinskyHouseProtection.loaded(valid);h.assertFalse(valid.isCanceled(),"Current visit prop rejected");v.putBoolean("Cancelled",true);var cancelled=new net.minecraftforge.event.entity.EntityJoinLevelEvent(item,h.getLevel(),true);BobinskyHouseProtection.loaded(cancelled);h.assertTrue(cancelled.isCanceled(),"Cancelled visit props reappeared while house waits for look-away");data.teaVisits.remove(owner);h.succeed();
    }
    @GameTest(template="empty") public static void teaCompletionUsesFamilyStoryLifetime(GameTestHelper h){
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();s.family=UUID.randomUUID();s.table=s.piano=BlockPos.ZERO;s.stage=VisitData.Stage.DONE;s.teaVisited=true;var data=VisitData.get(h.getLevel().getServer());data.scenes.put(s.owner,s);h.assertTrue(VisitData.load(data.save(new CompoundTag())).scenes.get(s.owner).teaVisited,"Tea completion lost on restart");FamilyVisitReset.validate(h.getLevel().getServer(),s.owner,UUID.randomUUID());h.assertFalse(data.scenes.containsKey(s.owner),"New parents inherited tea completion");h.succeed();
    }
    @GameTest(template="empty") public static void teaHouseRecoveryAndDoorEntryPolicySurviveSave(GameTestHelper h){
        var d=new BobinskyVisitData();var v=new CompoundTag();var owner=UUID.randomUUID();v.putUUID("Owner",owner);v.putBoolean("Tea",true);v.putBoolean("Active",true);v.putInt("Phase",TeaVisit.KNOCK);v.put("Originals",new ListTag());d.teaVisits.put(owner,v);var copy=BobinskyVisitData.load(d.save(new CompoundTag())).teaVisits.get(owner);h.assertFalse(TeaVisit.canEnter(copy),"Entry allowed before greeting");copy.putInt("Phase",TeaVisit.ENTER);h.assertTrue(TeaVisit.canEnter(copy),"Entry blocked after greeting");h.assertTrue(copy.contains("Originals"),"Tea recovery journal missing");h.succeed();
    }
    @GameTest(template="empty") public static void teaActorsAndSeatAreRegistered(GameTestHelper h){
        var spink=com.coraline.registry.ModEntities.SPINK_VISITOR.get().create(h.getLevel());var forcible=com.coraline.registry.ModEntities.FORCIBLE_VISITOR.get().create(h.getLevel());var seat=com.coraline.registry.ModEntities.TEA_SEAT.get().create(h.getLevel());h.assertTrue(spink.characterId().equals("miss_spink")&&forcible.characterId().equals("miss_forcible"),"Wrong tea models");h.assertFalse(spink.hurt(h.getLevel().damageSources().generic(),999),"Spink can die");h.assertTrue(seat!=null&&seat.getPassengersRidingOffset()<0,"Tea chair missing");spink.discard();forcible.discard();seat.discard();h.succeed();
    }
    @GameTest(template="empty") public static void teaRecordingsAndDrinkableItemAreRegistered(GameTestHelper h){
        StringBuilder lines=new StringBuilder();for(int phase=0;phase<16;phase++){String line=TeaVisit.line(phase);if(!line.isEmpty()){lines.append(line);h.assertTrue(VisitAudio.length("tea_visit_"+line)>0,"Missing tea line");}}h.assertTrue(lines.toString().equals("abcdefghij"),"Tea dialogue order changed");h.assertTrue(VisitAudio.length("tea_visit_music")>0,"Tea music missing");h.assertTrue(com.coraline.registry.ModItems.JASMINE_TEA.get().getUseDuration(new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.JASMINE_TEA.get()))==32,"Tea not drinkable");h.succeed();
    }
}
