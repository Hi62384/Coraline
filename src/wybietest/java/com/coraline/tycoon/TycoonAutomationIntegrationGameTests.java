package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Real stock, salary, inventory receipts and navigation; declared funded setup,
 * not a claim of a full placement-to-conversion survival playthrough. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TycoonAutomationIntegrationGameTests {
    private static ServerPlayer player(GameTestHelper t){var server=t.getLevel().getServer();var p=new ServerPlayer(server,server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AutomationQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);var h=TycoonWorld.home(p);h.state.putBoolean("Activated",true);h.state.putLong("ActiveTicks",1000);h.state.putLong("SoulEssence",1000);p.setPos(t.absolutePos(new BlockPos(2,3,2)).getCenter());return p;}
    private static void clean(ServerPlayer p){TycoonNativePlayers.remove(p);TycoonData.get(p.server).homes.remove(p.getUUID());TycoonNetwork.clear(p.getUUID());}
    @GameTest(template="empty",batch="automation-inventory") public static void batchCollectionFitsRealInventoryAndKeepsOverflow(GameTestHelper t){
        var p=player(t);TycoonNativePlayers.add(p);try{var h=TycoonWorld.home(p);var stock=new CompoundTag();stock.putLong("minecraft:iron_ingot",70);stock.putLong("minecraft:coal",70);stock.putLong("minecraft:oak_log",70);h.state.put("ProducedResources",stock);
            for(int i=0;i<33;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(32,new ItemStack(Items.IRON_INGOT,60));
            String result=TycoonWarehouse.collectMany(p,"",true);long stored=TycoonMachines.producedCount(h);long collected=TycoonShop.count(p,Items.IRON_INGOT)-60+TycoonShop.count(p,Items.COAL)+TycoonShop.count(p,Items.OAK_LOG);
            t.assertTrue(result.startsWith("Purchased")&&collected>64&&stored>0&&stored+collected==210,"Collection duplicated/lost resources or refused a valid partial batch: "+result);
            long before=stored;for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));t.assertTrue(TycoonWarehouse.collectMany(p,"",true).contains("Nothing fits")&&TycoonMachines.producedCount(h)==before,"Full inventory deleted overflow");
            t.assertTrue(!h.state.contains("PendingTransaction"),"Batch left an unreconciled transaction");
        }finally{clean(p);}t.succeed();
    }
    @GameTest(template="empty") public static void quoteReplayCannotDoubleDebitAndSalesPayFees(GameTestHelper t){var p=player(t);try{var h=TycoonWorld.home(p);var market=TycoonMarket.get(p.server);var id=UUID.randomUUID();String result=TycoonMarket.trade(p,"buttons",2,true,market.revision(),id,false);t.assertTrue(result.startsWith("Bought"),"Actual buy refused: "+result);var snapshot=h.state.copy();t.assertTrue(TycoonMarket.trade(p,"buttons",2,true,market.revision(),id,false).equals("Order already executed.")&&h.state.equals(snapshot),"Replayed buy changed funds/holdings");result=TycoonMarket.trade(p,"buttons",2,false,market.revision(),UUID.randomUUID(),false);t.assertTrue(result.startsWith("Sold")&&h.state.getCompound("Shares").getCompound("buttons").getLong("Quantity")==0&&h.state.getLong("SoulEssence")<1000,"Sale created free fee-less profit or retained shares");}finally{clean(p);}t.succeed();}
    @GameTest(template="empty",batch="automation-real-play",timeoutTicks=1800) public static void maidUsesOneRealReusableBallForActualCatch(GameTestHelper t){
        var p=player(t);var l=p.serverLevel();var h=TycoonWorld.home(p);var c=t.absolutePos(new BlockPos(3,3,3));
        // A batch can straddle a chunk border. Fake saved players are not
        // connected chunk watchers, so keep the whole real ball flight loaded.
        var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=(c.getX()-8)>>4;x<=(c.getX()+8)>>4;x++)for(int z=(c.getZ()-8)>>4;z<=(c.getZ()+8)>>4;z++){
            var chunk=new net.minecraft.world.level.ChunkPos(x,z);if(!l.getForcedChunks().contains(chunk.toLong())){l.setChunkForced(x,z,true);forced.add(chunk);}
        }
        Runnable releaseChunks=()->{for(var chunk:forced)l.setChunkForced(chunk.x,chunk.z,false);};
        // Seal this underground fixture too: cave water must not move the
        // paid ball or actors while this dry-floor play test is running.
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-1;y<=5;y++)
            if(y==-1||y==5||x==-8||x==8||z==-8||z==8)l.setBlock(c.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=0;y<5;y++)l.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        p.setPos(Vec3.atBottomCenterOf(c.south(4)));TycoonNativePlayers.add(p);
        var r=TycoonCare.generate(UUID.randomUUID());r.n.putString("Status","ACTIVE");r.need("Neglect",60);r.n.putString("Request","CATCH");r.n.putLong("RequestExpires",h.clock()+5000);r.n.putLong("NextRequest",1000000);r.n.putLong("NextIncident",1000000);h.residents.put(r.id,r);
        var child=TycoonContent.NPC.get().create(l);child.configure(h.owner,r.id,false,1);child.setPos(Vec3.atBottomCenterOf(c.east(2)));r.n.putUUID("Entity",child.getUUID());l.addFreshEntity(child);
        var worker=worker(p,TycoonWorkerType.OVERWORLD_MAID,l,c.west(2));var row=TycoonWorkers.workers(h).getCompound(worker.worker.toString());TycoonWarehouse.add(h,TycoonContent.BALL.get(),1);int[] age={0};boolean[] flight={false};
        t.onEachTick(()->{try{age[0]++;h.state.putLong("ActiveTicks",1000+age[0]);if(age[0]%20==0){TycoonWorkers.tick(p);TycoonStaffPlay.childTick(p,r,child);}if(row.hasUUID("PlayBall")&&l.getEntity(row.getUUID("PlayBall")) instanceof net.minecraft.world.entity.item.ItemEntity ball)flight[0]|=!ball.isInvisible()&&ball.getDeltaMovement().lengthSqr()>.001;
            if(r.n.getInt("LastCatchPasses")>=3){t.assertTrue(flight[0]&&r.n.getString("Request").isEmpty()&&r.need("Neglect")<56,"Play completed without actual thrown/caught ball or care effect");t.assertTrue(TycoonWarehouse.count(h,TycoonContent.BALL.get())==0&&row.getBoolean("BallHeld")&&l.getEntity(row.getUUID("PlayBall")) instanceof net.minecraft.world.entity.item.ItemEntity,"Reusable paid ball lost or duplicated");l.getEntity(row.getUUID("PlayBall")).discard();worker.discard();child.discard();clean(p);releaseChunks.run();t.succeed();}
            else if(age[0]>=1750){var ball=row.hasUUID("PlayBall")?l.getEntity(row.getUUID("PlayBall")):null;String trace=ball==null?"missing":ball.position()+" velocity="+ball.getDeltaMovement()+" ticks="+ball.tickCount+" loaded="+l.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(ball.blockPosition()).toLong());worker.discard();child.discard();if(ball!=null)ball.discard();clean(p);releaseChunks.run();t.fail("Real catch blocked: "+row.getString("Task")+" play="+r.n.getCompound("StaffPlay")+" request="+r.n.getString("Request")+" neglect="+r.need("Neglect")+" worker="+worker.position()+" child="+child.position()+" ball="+trace+" blocker="+row.getString("Blocker"));}
        }catch(Throwable failure){worker.discard();child.discard();clean(p);releaseChunks.run();throw failure;}});
    }
    private static TycoonWorker worker(ServerPlayer p,TycoonWorkerType type,ServerLevel l,BlockPos at){var h=TycoonWorld.home(p);var id=UUID.randomUUID();var actor=TycoonAutomationContent.WORKER.get().create(l);actor.configure(h.owner,id,type,5);actor.setPos(Vec3.atBottomCenterOf(at));var row=new CompoundTag();row.putUUID("Id",id);row.putUUID("Owner",h.owner);row.putUUID("Actor",actor.getUUID());row.putString("Type",type.id());row.putInt("Unlocked",5);row.putInt("Operating",5);row.putLong("Area",at.asLong());row.putLong("LastAt",at.asLong());var all=TycoonWorkers.workers(h);all.put(id.toString(),row);h.state.put("Workers",all);l.addFreshEntity(actor);return actor;}
    @GameTest(template="empty",batch="automation-physical-chef",timeoutTicks=700) public static void paidChefActuallyWalksCarriesAndRestocksOneRealBatch(GameTestHelper t){
        var p=player(t);var l=p.serverLevel();var h=TycoonWorld.home(p);var c=t.absolutePos(new BlockPos(3,3,3));
        // The GameTest origin is underground. A floor alone let cave water
        // flood this batch and FloatGoal carried the chef 32 blocks upward.
        // Seal a real room first, then clear its interior; do not disable the
        // worker's collision, floating, navigation or physical cargo delivery.
        for(int x=-3;x<=6;x++)for(int z=-3;z<=4;z++)for(int y=-1;y<=3;y++)
            if(y==-1||y==3||x==-3||x==6||z==-3||z==4)l.setBlock(c.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
        for(int x=-2;x<=5;x++)for(int z=-2;z<=3;z++)for(int y=0;y<3;y++)l.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        p.setPos(Vec3.atBottomCenterOf(c.south(3)));TycoonNativePlayers.add(p);
        var source=c.east(4);TycoonWorld.placeOwned(l,source,TycoonContent.BLOCKS.get("orphan_fridge").get(),p.getUUID());var furniture=new CompoundTag();furniture.putString(Long.toString(source.asLong()),"orphan_fridge");h.state.put("Furniture",furniture);var be=(TycoonBlockEntity)l.getBlockEntity(source);TycoonWarehouse.add(h,Items.BREAD,8);
        var id=UUID.randomUUID();var worker=TycoonAutomationContent.WORKER.get().create(l);worker.configure(h.owner,id,TycoonWorkerType.OVERWORLD_CHEF,5);worker.setPos(Vec3.atBottomCenterOf(c.north()));var row=new CompoundTag();row.putUUID("Id",id);row.putUUID("Owner",h.owner);row.putUUID("Actor",worker.getUUID());row.putString("Type","overworld_chef");row.putInt("Unlocked",5);row.putInt("Operating",5);row.putLong("Area",c.asLong());row.putLong("LastAt",worker.blockPosition().asLong());var all=new CompoundTag();all.put(id.toString(),row);h.state.put("Workers",all);l.addFreshEntity(worker);var start=worker.position();int[] age={0};boolean[] carried={false};
        t.onEachTick(()->{try{age[0]++;h.state.putLong("ActiveTicks",1000+age[0]);if(age[0]%20==0)TycoonWorkers.tick(p);carried[0]|=row.contains("Carry")&&!worker.getMainHandItem().isEmpty();
            if(be.getItem(0).getCount()==8){t.assertTrue(carried[0]&&worker.distanceToSqr(start)>1,"Chef was decorative or delivery teleported without physical movement/carry");t.assertTrue(TycoonWarehouse.count(h,Items.BREAD)==0&&!row.contains("Carry")&&row.getLong("CompletedJobs")==1,"Stock debit/delivery repeated or remained reserved");t.assertTrue(row.getLong("SalaryPaid")>0&&h.state.getLong("SoulEssence")<1000,"Worker ran without actual salary");worker.discard();clean(p);t.succeed();}
            else if(age[0]>=650){worker.discard();clean(p);t.fail("Chef blocked at "+worker.position()+" task="+row.getString("Task")+" blocker="+row.getString("Blocker")+" carry="+row.getCompound("Carry"));}
        }catch(Throwable failure){worker.discard();clean(p);throw failure;}});
    }
    private TycoonAutomationIntegrationGameTests(){}
}
