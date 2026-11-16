package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FirstEscapeCarryGameTests {
    @GameTest(template="empty",timeoutTicks=900) public static void authoredParlorAndCarpetedHallHaveARealCarryRoute(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var p=new net.minecraft.server.level.ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HouseCarryQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",2);
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putLong("House",new BlockPos(107360,180,107360).asLong());
        try(var raw=FirstEscapeCarryGameTests.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");var zipped=new java.util.zip.GZIPInputStream(java.util.Objects.requireNonNull(raw));var reader=new java.io.InputStreamReader(zipped,java.nio.charset.StandardCharsets.UTF_8)){
            var source=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=source.getAsJsonArray("palette");
            for(var element:source.getAsJsonArray("blocks")){var cell=element.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();if(x<13||x>36||y<5||y>9||z<32||z>54)continue;
                var entry=palette.get(cell.get(3).getAsInt()).getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(entry.get("id").getAsString()));var state=java.util.Objects.requireNonNull(block).defaultBlockState();
                for(var property:entry.getAsJsonObject("properties").entrySet())state=property(state,property.getKey(),property.getValue().getAsString());
                var at=FirstEscapeScene.at(s,x,y,z);l.getChunkAt(at);l.setBlock(at,state,2);
            }
            var eastPlaster=FirstEscapeScene.at(s,31,9,44);var insetPlaster=FirstEscapeScene.at(s,27,9,36);
            l.setBlock(eastPlaster,Blocks.WHITE_TERRACOTTA.defaultBlockState(),2);l.setBlock(insetPlaster,Blocks.WHITE_CONCRETE.defaultBlockState(),2);
            var glazing=FirstEscapeScene.at(s,13,9,40);l.setBlock(glazing,Blocks.GLASS.defaultBlockState(),2);
            FirstEscapeScene.prepareRoom(p,s);FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s),true);FirstEscapeStory.setDoor(p,FirstEscapeScene.at(s,31,6,42),true);
            var start=FirstEscapeScene.room(s).offset(-1,0,2);var hall=FirstEscapeScene.entrance(s).east(2);var mirror=FirstEscapeScene.mirror(s).north(1);
            h.assertTrue(!FirstEscapeCarry.route(p,start,hall).isEmpty(),"The real parlor doorway is not traversable with the captive");
            h.assertTrue(!FirstEscapeCarry.route(p,hall,mirror).isEmpty(),"The actual carpeted hall cannot reach the mirror");
            var mother=com.coraline.registry.ModEntities.MOM.get().create(l);h.assertTrue(mother!=null,"Carry actor could not be created");mother.setPos(FirstEscapeCarry.feet(p,start));mother.setOnGround(true);
            for(var goal:java.util.List.of(hall,mirror)){boolean arrived=false;for(int step=0;step<800&&!arrived;step++)arrived=FirstEscapeCarry.move(p,s,mother,goal);h.assertTrue(arrived,"Actual carried-route movement stalled before "+goal+" at "+mother.position()+" / node "+n.getInt("CarryNode")+" / route "+n.getList("CarryRoute",4)+" / "+n.getString("Blocker"));}
            h.assertTrue(l.getBlockState(FirstEscapeScene.at(s,13,9,38)).is(com.coraline.registry.ModBlocks.INSECT_PARLOR_WALLPAPER.get()),"Existing room plaster was not replaced");
            h.assertTrue(l.getBlockState(eastPlaster).is(com.coraline.registry.ModBlocks.INSECT_PARLOR_WALLPAPER.get()),"Terracotta east wall was missed");
            h.assertTrue(l.getBlockState(insetPlaster).is(com.coraline.registry.ModBlocks.INSECT_PARLOR_WALLPAPER.get()),"Inset return was missed");
            h.assertTrue(l.getBlockState(glazing).is(Blocks.GLASS),"Wallpaper covered existing glazing");
            // Reproduce an older, partly wallpapered save and check migration.
            l.setBlock(eastPlaster,Blocks.WHITE_TERRACOTTA.defaultBlockState(),2);n.putBoolean("WallpaperApplied",true);n.putInt("WallpaperRevision",1);
            n.putLong("LittleDoor",FirstEscapeScene.at(s,13,6,44).asLong());FirstEscapeScene.roomProps(p,s);
            h.assertTrue(l.getBlockState(eastPlaster).is(com.coraline.registry.ModBlocks.INSECT_PARLOR_WALLPAPER.get()),"Existing save did not repair wallpaper coverage");
            // Use the real grip carrier and mounted player, not only an actor path.
            mother.makeOtherParent(0);mother.setPos(FirstEscapeCarry.feet(p,start));h.assertTrue(l.addFreshEntity(mother),"Canonical Other Mother fixture was not admitted to the realm");s.mom=mother.getUUID();
            FirstEscapeScene.prop(p,s,"BugChair").setPos(mother.position());
            p.setPos(mother.position().add(.8,0,0));n.remove("DragInHall");n.remove("AtMirror");FirstEscapeCarry.clear(n);
            FirstEscapeStory.go(p,s,FirstEscapeTimeline.Phase.COCOA_G);
            h.assertTrue(n.getBoolean("Grabbed")&&p.getVehicle() instanceof CinematicPropEntity,"Transformation ending did not mount the captive immediately");
            var carrier=p.getVehicle();var before=carrier.position();
            // Entity-section visibility changes on actual server ticks. A tight
            // synchronous loop cannot exercise the mounted chunk handoff.
            var steps=new int[]{0};var trace=new java.util.ArrayDeque<String>();
            h.onEachTick(()->{FirstEscapeStory.drag(p,s);var captiveAt=carrier.position().add(0,carrier.getPassengersRidingOffset(),0);h.assertTrue(!l.getBlockCollisions(p,p.getDimensions(net.minecraft.world.entity.Pose.SWIMMING).makeBoundingBox(captiveAt)).iterator().hasNext(),"Captive grip entered a wall");h.assertTrue(carrier.position().subtract(mother.position()).horizontalDistanceSqr()>=.75*.75,"Captive camera entered Mother's torso");if(++steps[0]%10==0){if(trace.size()==24)trace.removeFirst();trace.addLast(steps[0]+":"+mother.position()+" node="+n.getInt("CarryNode")+" loaded="+(l.getEntity(s.mom)==mother));}
                if(n.getBoolean("AtMirror")){
                    h.assertTrue(p.getVehicle()==carrier&&carrier.position().distanceToSqr(before)>16,"Mother did not carry the mounted player through the hallway");
                    n.putBoolean("InternalTransfer",true);p.stopRiding();p.setForcedPose(null);carrier.discard();mother.discard();FirstEscapeScene.restore(p,s);VisitData.get(p.server).scenes.remove(s.owner);h.succeed();
                }else if(steps[0]>=800){VisitData.get(p.server).scenes.remove(s.owner);h.fail("Mounted captive stalled before the actual mirror: mother="+mother.position()+" hall="+n.getBoolean("DragInHall")+" node="+n.getInt("CarryNode")+" blocker="+n.getString("Blocker")+" trace="+trace);}
            });
        }catch(java.io.IOException error){VisitData.get(p.server).scenes.remove(s.owner);throw new IllegalStateException(error);}
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(net.minecraft.world.level.block.state.BlockState state,String key,String value){
        @SuppressWarnings("unchecked") var property=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);
        return property==null?state:property.getValue(value).map(parsed->state.setValue(property,parsed)).orElse(state);
    }
    @GameTest(template="empty") public static void carryDetoursAroundWallAndAcceptsCarpet(GameTestHelper h){
        var p=player(h);var l=p.serverLevel();var c=h.absolutePos(new BlockPos(2,2,2));l.getChunkAt(c);
        for(int x=0;x<7;x++)for(int z=0;z<5;z++){l.setBlock(c.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)l.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(c.offset(x,0,z),Blocks.BROWN_CARPET.defaultBlockState(),2);}
        for(int z=0;z<4;z++)for(int y=0;y<3;y++)l.setBlock(c.offset(3,y,z),Blocks.STONE.defaultBlockState(),2);
        var route=FirstEscapeCarry.route(p,c.offset(1,0,1),c.offset(5,0,1));
        h.assertTrue(!route.isEmpty(),"Carpet was incorrectly treated as an impassable wall");
        h.assertTrue(route.stream().anyMatch(cell->cell.getZ()==c.getZ()+4),"Carry path cut through the wall instead of using the gap");
        h.assertTrue(route.stream().allMatch(cell->FirstEscapeCarry.fits(p,cell)),"A route node lacks captive head clearance or floor support");
        h.assertTrue(Math.abs(FirstEscapeCarry.feet(p,c).y-c.getY()-.0625)<.001,"Carry route sinks into the carpet");h.succeed();
    }
    @GameTest(template="empty") public static void sealedDoorwayIsNotReportedAsArrival(GameTestHelper h){
        var p=player(h);var l=p.serverLevel();var c=h.absolutePos(new BlockPos(2,2,2));l.getChunkAt(c);
        for(int x=0;x<5;x++)for(int z=0;z<5;z++){l.setBlock(c.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)l.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int y=0;y<3;y++)l.setBlock(c.relative(direction).above(y),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(FirstEscapeCarry.route(p,c,c.east(3)).isEmpty(),"A failed path was allowed through a sealed wall");h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){return new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CarryQA"));}
    private FirstEscapeCarryGameTests(){}
}
