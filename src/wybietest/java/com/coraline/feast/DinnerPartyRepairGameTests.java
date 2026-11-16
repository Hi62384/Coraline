package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class DinnerPartyRepairGameTests {
    @GameTest(template="empty") public static void carpetSupportsRealSocialTablesAndBanquet(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new net.minecraft.server.level.ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PartyFloorQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.PARTY_SOCIAL;s.loop.event.putInt("Serial",12);s.loop.event.putString("Type","HOUSE_PARTY");s.loop.event.putBoolean("PartyPending",true);
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.door=s.door;r.house=new BlockPos(107600,180,107600);r.doorPos=HouseCabinets.at(r.house,14,6,40);r.ready=true;RealmData.get(p.server).realms.put(s.door,r);
        var party=new net.minecraft.nbt.CompoundTag();HousePartyLedger.begin(party,s.owner,12,7,24);s.loop.event.put("Party",party);
        try(var raw=DinnerPartyRepairGameTests.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");var zip=new java.util.zip.GZIPInputStream(Objects.requireNonNull(raw));var reader=new java.io.InputStreamReader(zip,java.nio.charset.StandardCharsets.UTF_8)){
            var house=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=house.getAsJsonArray("palette");
            for(var item:house.getAsJsonArray("blocks")){var cell=item.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();if(x<11||x>56||y<5||y>9||z<9||z>68)continue;
                var entry=palette.get(cell.get(3).getAsInt()).getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(entry.get("id").getAsString()));var state=Objects.requireNonNull(block).defaultBlockState();
                for(var property:entry.getAsJsonObject("properties").entrySet())state=property(state,property.getKey(),property.getValue().getAsString());var at=HouseCabinets.at(r.house,x,y,z);l.getChunkAt(at);l.setBlock(at,state,2);
            }
            p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(HouseCabinets.at(r.house,43,6,35)));
            h.assertTrue(PartyGameStations.prepare(p,s),"No actual game table fits the authored carpeted house");
            h.assertTrue(!PartyGameStations.tables(p,s).isEmpty(),"Social invitation has no actual game table");
            h.assertTrue(PartyBanquet.prepare(p,s),"Authored supper-room banquet failed: "+party.getString("BanquetBlocker"));
            h.assertTrue(VisitEvents.props(p,s).stream().anyMatch(e->e.kind()==ScenePropEntity.TABLE&&e.longDining()),"Feast announced without a long dining table");
            h.assertTrue(PartyBanquet.playerChair(p,s)!=null,"Feast lacks the player's actual chair");
            h.assertTrue(!PartyGameStations.tables(p,s).isEmpty(),"Feast preparation deleted social game tables");
            h.succeed();
        }catch(java.io.IOException e){throw new IllegalStateException(e);}finally{for(var prop:VisitEvents.props(p,s))prop.discard();VisitData.get(p.server).scenes.remove(s.owner);RealmData.get(p.server).realms.remove(s.door);}
    }
    @GameTest(template="empty") public static void phaseTimerDoesNotIncludeFutureParts(GameTestHelper h){
        var s=new VisitData.Scene();s.loop.event.put("Party",new net.minecraft.nbt.CompoundTag());var n=HouseParty.data(s);n.putInt("FeastDuration",1200);
        s.loop.phase=OtherMotherLoopState.Phase.PARTY_SOCIAL;n.putInt("SocialElapsed",7200-200);h.assertTrue(HouseParty.remaining(s)==200,"Social clock includes future feast/celebration");
        s.loop.phase=OtherMotherLoopState.Phase.PARTY_FEAST_CALL;h.assertTrue(HouseParty.remaining(s)==0,"A future feast countdown appears frozen while staging");
        s.loop.phase=OtherMotherLoopState.Phase.PARTY_FEAST;n.putInt("FeastElapsed",400);h.assertTrue(HouseParty.remaining(s)==800,"Feast clock is not phase-local");
        n.putInt("CelebrationElapsed",300);h.assertTrue(HouseParty.remaining(s)==600,"Celebration clock includes the feast");h.succeed();
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(net.minecraft.world.level.block.state.BlockState state,String key,String value){
        @SuppressWarnings("unchecked") var property=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);
        return property==null?state:property.getValue(value).map(parsed->state.setValue(property,parsed)).orElse(state);
    }
    private DinnerPartyRepairGameTests(){}
}
