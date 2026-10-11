package com.coraline.feast;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class MineRevisionGameTests {
 @GameTest(template="empty") public static void mineMazeConnectivity(GameTestHelper h){
  var variants=new HashSet<Integer>();
  for(int seed=0;seed<1000;seed++){
   var maze=MineMaze.generate(seed);variants.add(maze.edges().hashCode());
   h.assertTrue(maze.cells().size()==420&&maze.edges().size()>419,"Missing branches/loops");
   var graph=new HashMap<MineMaze.Cell,List<MineMaze.Cell>>();
   for(var edge:maze.edges()){graph.computeIfAbsent(edge.a(),k->new ArrayList<>()).add(edge.b());graph.computeIfAbsent(edge.b(),k->new ArrayList<>()).add(edge.a());
    var a=edge.a().pos();var b=edge.b().pos();h.assertTrue(Math.abs(a.getX()-b.getX())+Math.abs(a.getZ()-b.getZ())==14,"Nonadjacent tunnel");h.assertTrue(Math.abs(a.getY()-b.getY())<=14,"Unwalkable stairs");}
   var seen=new HashSet<MineMaze.Cell>();var queue=new ArrayDeque<MineMaze.Cell>();queue.add(maze.cells().get(0));
   while(!queue.isEmpty()){var at=queue.remove();if(seen.add(at))queue.addAll(graph.getOrDefault(at,List.of()));}
   h.assertTrue(seen.size()==420,"Disconnected maze");
  }
  h.assertTrue(variants.size()>990,"Repeated maze");h.succeed();
 }
 @GameTest(template="empty") public static void mineTimeoutLosesHaul(GameTestHelper h){
  var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_MINESHAFT);var n=f.s().loop.expedition;
  n.putInt("Phase",1);MineshaftActivity.begin(f.p(),f.s());h.assertTrue(ActivityWorld.exitReady(f.s()),"No default exit");
  h.assertTrue(n.getInt("Hearts")==5&&n.getLong("End")-OtherMotherLoop.now(f.p())==24000,"Wrong stakes");
  n.putLong("End",OtherMotherLoop.now(f.p()));var bag=new net.minecraft.nbt.ListTag();bag.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND).save(new net.minecraft.nbt.CompoundTag()));n.put("Bag",bag);
  f.p().setPos(Vec3.atBottomCenterOf(f.c().south(7)));MineshaftActivity.settle(f.p(),f.s());
  h.assertTrue(n.getInt("Phase")==5&&!n.contains("Bag")&&f.p().getPersistentData().getList("CoralineMineDelivery",10).isEmpty(),"Timeout paid loot");h.succeed();
 }
}
