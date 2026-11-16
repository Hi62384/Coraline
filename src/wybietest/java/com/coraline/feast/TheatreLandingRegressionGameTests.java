package com.coraline.feast;
import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TheatreLandingRegressionGameTests {
    @GameTest(template="empty",timeoutTicks=120) public static void actualParentsWaitOnTheExteriorCellarStairExit(GameTestHelper h){
        var server=h.getLevel().getServer();var l=server.getLevel(OtherWorld.KEY);var p=new ServerPlayer(server,server.getLevel(ActivityWorld.KEY),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TheatreLandingQA"));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",99);s.loop.event.putBoolean("TheatrePending",true);VisitData.get(server).scenes.put(s.owner,s);
        var r=new RealmData.Realm();r.door=s.door;r.house=new BlockPos(114000,180,114000);r.ready=true;RealmData.get(server).realms.put(s.door,r);
        try(var raw=TheatreLandingRegressionGameTests.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");var zip=new java.util.zip.GZIPInputStream(Objects.requireNonNull(raw));var reader=new java.io.InputStreamReader(zip,java.nio.charset.StandardCharsets.UTF_8)){
            var doc=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=doc.getAsJsonArray("palette");for(var element:doc.getAsJsonArray("blocks")){var a=element.getAsJsonArray();int x=a.get(0).getAsInt(),y=a.get(1).getAsInt(),z=a.get(2).getAsInt();if(x<24||x>29||y>9||z< -2||z>16)continue;var spec=palette.get(a.get(3).getAsInt()).getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(spec.get("id").getAsString()));var state=Objects.requireNonNull(block).defaultBlockState();for(var entry:spec.getAsJsonObject("properties").entrySet())state=property(state,entry.getKey(),entry.getValue().getAsString());var at=HouseCabinets.at(r.house,x,y,z);l.getChunkAt(at);l.setBlock(at,state,2|16|32);}
            var mom=ModEntities.MOM.get().create(l);var dad=ModEntities.DAD.get().create(l);
            for(var actor:List.of(mom,dad)){actor.makeOtherParent(0);actor.getPersistentData().putUUID("RealmDoor",s.door);actor.setPos(Vec3.atBottomCenterOf(HouseCabinets.at(r.house,26,-1,8)));h.assertTrue(l.addFreshEntity(actor),"Fixture parent rejected");}s.mom=mom.getUUID();s.dad=dad.getUUID();
            TheatreVisit.stageReunionParents(p,s);var top=Vec3.atBottomCenterOf(HouseCabinets.at(r.house,26,4,-1));
            h.assertTrue(TheatreVisit.data(s).getBoolean("LandingParentsPlaced"),"Strict landing lookup cannot place both parents");
            h.assertTrue(mom.distanceToSqr(top)<5&&dad.distanceToSqr(top)<5&&mom.getZ()<r.house.getZ()-40&&dad.getZ()<r.house.getZ()-40,"Parents still wait by the front-room windows instead of the cellar exit");
            h.assertTrue(mom.isNoAi()&&dad.isNoAi()&&mom.getNavigation().isDone()&&dad.getNavigation().isDone(),"Parents wander away while the reunion recording plays");mom.discard();dad.discard();h.succeed();
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}finally{VisitData.get(server).scenes.remove(s.owner);RealmData.get(server).realms.remove(s.door);}
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(net.minecraft.world.level.block.state.BlockState state,String key,String value){@SuppressWarnings("unchecked") var prop=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);return prop==null?state:prop.getValue(value).map(v->state.setValue(prop,v)).orElse(state);}
}
