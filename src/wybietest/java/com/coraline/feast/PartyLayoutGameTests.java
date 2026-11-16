package com.coraline.feast;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import static com.coraline.feast.PartyLayoutRules.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PartyLayoutGameTests {
    @GameTest(template="empty") public static void gardenMergesOnlyActualAdjacentTiles(GameTestHelper h){var strips=strips(List.of(new Tile(0,3,0),new Tile(1,3,0),new Tile(3,3,0),new Tile(3,3,0)));h.assertTrue(strips.equals(List.of(new Strip(0,1,3,0),new Strip(3,3,3,0))),"Permission filled a garden hole or duplicated a tile");h.succeed();}
    @GameTest(template="empty") public static void gardenPreservesHeightsRowsAndNegativeCoordinates(GameTestHelper h){var cells=List.of(new Tile(-3,-2,-1),new Tile(-2,-2,-1),new Tile(-1,-1,-1),new Tile(-1,-2,0));var got=new HashSet<>(strips(cells));h.assertTrue(got.equals(Set.of(new Strip(-3,-2,-2,-1),new Strip(-1,-1,-1,-1),new Strip(-1,-1,-2,0))),"Different levels or rows merged into invented grounds");h.succeed();}
    @GameTest(template="empty") public static void gardenRoundtripHasNoExtraPermission(GameTestHelper h){var random=new Random(32);for(int sample=0;sample<100;sample++){var cells=new HashSet<Tile>();for(int i=0;i<200;i++)cells.add(new Tile(random.nextInt(31)-15,random.nextInt(5)-2,random.nextInt(21)-10));var expanded=new HashSet<Tile>();for(var strip:strips(cells))for(int x=strip.first();x<=strip.last();x++)expanded.add(new Tile(x,strip.y(),strip.z()));h.assertTrue(cells.equals(expanded),"Compressed authored footprint gained/lost cells");}h.succeed();}
    @GameTest(template="empty") public static void crowdLeavesSpaceAndNeverOverlapsItself(GameTestHelper h){var offsets=new ArrayList<Offset>();for(int i=0;i<24;i++){var a=crowd(i);h.assertTrue(Math.hypot(a.x(),a.z())>=4.49,"Crowd blocks player center");h.assertTrue(!(Math.abs(a.x())<1.5&&a.z()<0),"Crowd closed the departure gap");for(var b:offsets)h.assertTrue(Math.hypot(a.x()-b.x(),a.z()-b.z())>1.5,"Actual crowd seats overlap");offsets.add(a);}h.succeed();}
    @GameTest(template="empty") public static void cakeSlicesClampAndRemoveExactlyOnePortion(GameTestHelper h){h.assertTrue(cakeSlices(Integer.MIN_VALUE)==0&&cakeSlices(Integer.MAX_VALUE)==12,"Malformed synchronized count escaped cake mesh bounds");for(int i=12;i>0;i--)h.assertTrue(cakeSlices(i)-cakeSlices(i-1)==1,"Cake consumption did not remove one visible portion");h.succeed();}
    @GameTest(template="empty") public static void protectedRoomsDoNotIncludePublicInventory(GameTestHelper h){for(String room:List.of("attic","basement","archive"))h.assertTrue(protectedRoom(room),"Protected room omitted");for(String room:List.of("","kitchen","living_room","player_inventory","theatre_reward"))h.assertFalse(protectedRoom(room),"Unrelated inventory incorrectly protected");h.assertFalse(protectedRoom(null),"Missing room accidentally protects unrelated data");h.succeed();}
    private PartyLayoutGameTests(){}
}
