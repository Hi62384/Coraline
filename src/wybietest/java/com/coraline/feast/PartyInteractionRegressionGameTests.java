package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.HouseCabinets;
import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PartyInteractionRegressionGameTests {
    private record Fixture(ServerPlayer p,VisitData.Scene s,RealmData.Realm realm) implements AutoCloseable {
        public void close(){p.stopRiding();p.stopSleepInBed(true,false);for(var e:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(realm.house).inflate(100),e->p.getUUID().equals(e.owner)))e.discard();VisitData.get(p.server).scenes.remove(s.owner);RealmData.get(p.server).realms.remove(s.door);}
    }
    private static Fixture fixture(GameTestHelper h,int offset){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PartyInteractionQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.PARTY_SOCIAL;s.loop.event.putInt("Serial",44);s.loop.event.putString("Type","HOUSE_PARTY");s.loop.event.putBoolean("PartyPending",true);
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);var r=new RealmData.Realm();r.door=s.door;r.house=new BlockPos(113400+offset,180,113400);r.ready=true;r.bed=HouseCabinets.at(r.house,41,15,24);r.table=HouseCabinets.at(r.house,43,6,36);r.piano=HouseCabinets.at(r.house,20,6,60);s.table=r.table;s.piano=r.piano;RealmData.get(p.server).realms.put(s.door,r);
        var party=new CompoundTag();HousePartyLedger.begin(party,s.owner,44,1,12);s.loop.event.put("Party",party);
        try(var raw=PartyInteractionRegressionGameTests.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");var zip=new java.util.zip.GZIPInputStream(Objects.requireNonNull(raw));var reader=new java.io.InputStreamReader(zip,java.nio.charset.StandardCharsets.UTF_8)){
            var doc=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=doc.getAsJsonArray("palette");for(var element:doc.getAsJsonArray("blocks")){var a=element.getAsJsonArray();int x=a.get(0).getAsInt(),y=a.get(1).getAsInt(),z=a.get(2).getAsInt();if(x<32||x>53||y<5||y>23||z<12||z>52)continue;var spec=palette.get(a.get(3).getAsInt()).getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(spec.get("id").getAsString()));var state=Objects.requireNonNull(block).defaultBlockState();for(var entry:spec.getAsJsonObject("properties").entrySet())state=property(state,entry.getKey(),entry.getValue().getAsString());var at=HouseCabinets.at(r.house,x,y,z);l.getChunkAt(at);l.setBlock(at,state,2|16|32);}
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}return new Fixture(p,s,r);
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(net.minecraft.world.level.block.state.BlockState state,String key,String value){@SuppressWarnings("unchecked") var prop=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);return prop==null?state:prop.getValue(value).map(v->state.setValue(prop,v)).orElse(state);}
    @GameTest(template="empty",timeoutTicks=240) public static void physicalPresentsSurviveFullInventoryAndOpenOnlyOnce(GameTestHelper h){
        try(var f=fixture(h,0)){var p=f.p;var s=f.s;UUID id=UUID.randomUUID();var receipt=new CompoundTag();receipt.putUUID("Owner",s.owner);receipt.putString("From","Guest QA");receipt.put("Payload",new ItemStack(Items.COOKIE,3).save(new CompoundTag()));var book=new CompoundTag();book.put(id.toString(),receipt);p.getPersistentData().put(PartyCelebration.BOOK,book);
            var guest=new CompoundTag();guest.putUUID("Id",id);guest.putBoolean("PresentGiven",true);h.assertTrue(PartyCelebration.leavePresent(p,s,guest,null),"No safe present spot in authored supper room");
            var prop=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(f.realm.house).inflate(80),e->e.getPersistentData().hasUUID("HousePartyPresent")).stream().findFirst().orElseThrow();
            h.assertTrue(!prop.getPersistentData().contains("VisitEventSerial")&&PartyCelebration.presentTick(prop),"Present still belongs to temporary event cleanup");
            var standing=PartyFloor.at(p,prop.blockPosition().east());h.assertTrue(standing!=null,"Fixture pickup stance is blocked");p.setPos(standing);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));PartyCelebration.pickup(p,prop);
            h.assertFalse(prop.isRemoved()||receipt.getBoolean("Delivered"),"Full inventory deleted the physical present");p.getInventory().setItem(0,ItemStack.EMPTY);PartyCelebration.pickup(p,prop);
            h.assertTrue(prop.isRemoved()&&receipt.getBoolean("Delivered"),"Normal right-click did not collect the present: reach="+PartyMatches.withinReach(p,prop)+" owner="+p.position()+" gift="+prop.position()+" bounds="+prop.getBoundingBox()+" receipt="+receipt);var wrapped=p.getInventory().getItem(0);var duplicate=wrapped.copy();
            var use=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,net.minecraft.world.InteractionHand.MAIN_HAND);RealmProtection.useItem(use);h.assertFalse(use.isCanceled(),"Realm protection still blocks unwrapping");
            p.getInventory().selected=0;ModItems.WRAPPED_PARTY_GIFT.get().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(receipt.getBoolean("Opened")&&p.getInventory().getItem(0).is(Items.COOKIE)&&p.getInventory().getItem(0).getCount()==3,"Collected present did not unwrap into its recorded payload");PartyCelebration.unwrap(p,duplicate);h.assertTrue(p.getInventory().getItem(0).getCount()==3,"Duplicate token paid a second time");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240) public static void bedroomFurnitureGatesSleepAndIsRemovedAfterWaking(GameTestHelper h){
        try(var f=fixture(h,256)){var p=f.p;var s=f.s;s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;var n=BlackIsTraditional.data(s);n.putBoolean("Started",true);n.putString("Phase","M");n.putLong("Bed",f.realm.bed.asLong());n.putUUID("Choice",UUID.randomUUID());p.setPos(Vec3.atBottomCenterOf(f.realm.bed.east(2)));BedroomBarricade.tick(p,s);
            var pieces=VisitEvents.props(p,s).stream().filter(e->e.getPersistentData().getBoolean("BedroomBarricade")).toList();h.assertTrue(pieces.size()==3,"Authored bedroom cannot stage three pushable pieces: "+n.getCompound("Barricade").getString("Blocker"));
            for(int a=0;a<pieces.size();a++)for(int b=a+1;b<pieces.size();b++)h.assertTrue(pieces.get(a).distanceToSqr(pieces.get(b))>=9,"Barricade furniture still starts clustered together");
            n.putString("Phase","GO_TO_BED");BlackIsTraditional.block(p,f.realm.bed);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.GO_TO_BED&&!p.isSleeping(),"Bed bypassed the incomplete barricade");
            for(var piece:pieces)for(int i=0;i<256&&!piece.getPersistentData().getBoolean("BarricadePlaced");i++){
                boolean reached=false;var room=PartyRegions.rooms().stream().filter(r->r.id().equals("child_bedroom")).findFirst().orElseThrow();
                for(double radius:new double[]{1,1.5,2})for(int turn=0;turn<8&&!reached;turn++){
                    double angle=turn*Math.PI/4;var at=piece.position().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);var floor=PartyFloor.at(p,net.minecraft.core.BlockPos.containing(at));if(floor==null)continue;at=new Vec3(at.x,floor.y,at.z);
                    if(!room.bounds(f.realm.house).contains(at)||p.level().getBlockCollisions(p,p.getDimensions(p.getPose()).makeBoundingBox(at)).iterator().hasNext())continue;
                    p.setPos(at);reached=PartyMatches.withinReach(p,piece);
                }
                h.assertTrue(reached,"No unobstructed player approach to barricade piece "+piece.position());
                n.getCompound("Barricade").putLong("NextPush",0);BedroomBarricade.interact(p,s,piece);
            }
            p.setPos(Vec3.atBottomCenterOf(f.realm.bed.east(2)));BedroomBarricade.tick(p,s);h.assertTrue(BedroomBarricade.done(s),"Furniture pushes did not finish the barricade");
            BlackIsTraditional.go(p,s,BlackIsTraditional.Phase.DARK_HOUSE);h.assertTrue(VisitEvents.props(p,s).stream().noneMatch(e->e.getPersistentData().getBoolean("BedroomBarricade")),"Barricades survived waking and block downstairs access");
        }h.succeed();
    }
}
