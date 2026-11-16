package com.coraline.bobinsky;

import com.coraline.feast.VisitData;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class BobinskyReturnGameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void blockedReturnsContinueAndFinalHouseRestores(GameTestHelper h){
        var c=h.absolutePos(new BlockPos(1100,240,1100));BobinskyHouse.hold(h.getLevel(),c);
        h.runAfterDelay(5,()->{
            for(int phase:new int[]{BobinskyVisit.G,BobinskyVisit.K}){
                var v=visit(c,phase);var p=player(h,c);var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=UUID.randomUUID();scene.stage=VisitData.Stage.DONE;
                v.put("Originals",BobinskyHouse.snapshot(h.getLevel(),c));BobinskyHouse.build(h.getLevel(),v);
                for(int x=-3;x<=3;x++)for(int y=-1;y<=4;y++)h.getLevel().setBlock(c.offset(x,y,-6),Blocks.STONE.defaultBlockState(),2);
                var actor=ModEntities.BOBINSKY_VISITOR.get().create(h.getLevel());actor.setPos(Vec3.atBottomCenterOf(c.north(8)));
                for(int tick=0;tick<300&&v.getInt("Phase")==phase;tick++)BobinskyVisit.perform(p,scene,v,actor);
                h.assertTrue(v.getInt("Phase")== (phase==BobinskyVisit.G?BobinskyVisit.WAIT_H:BobinskyVisit.WAIT_HIDE),"Blocked return never continued: "+phase);
                h.assertTrue(actor.isInvisible()&&v.getBoolean("Inside"),"Recovered actor still visible outside");
                h.assertFalse(h.getLevel().getBlockState(BobinskyHouse.door(v)).getValue(DoorBlock.OPEN),"Door left open after recovery");
                if(phase==BobinskyVisit.K){
                    // Looking south keeps the house visible; north retires it.
                    p.setPos(Vec3.atBottomCenterOf(c.north(14)));p.setYRot(0);p.setXRot(0);
                    BobinskyVisit.perform(p,scene,v,actor);h.assertTrue(v.contains("Originals"),"Scenery vanished while watched");
                    p.setYRot(180);BobinskyVisit.perform(p,scene,v,actor);
                    h.assertTrue(actor.isRemoved()&&!v.getBoolean("Active")&&scene.bobinskyVisited&&!v.contains("Originals"),"Recovered final visit did not finish and restore");
                }else{actor.discard();BobinskyHouse.restore(h.getLevel(),v);}
                for(int x=-3;x<=3;x++)for(int y=-1;y<=4;y++)h.getLevel().setBlock(c.offset(x,y,-6),Blocks.AIR.defaultBlockState(),2);
            }
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void savedAndLegacyReturnBudgetsCannotRunForever(GameTestHelper h){
        var c=h.absolutePos(new BlockPos(1200,240,1200));var p=player(h,c);
        for(boolean legacy:new boolean[]{false,true}){
            var data=new BobinskyVisitData();var owner=UUID.randomUUID();var v=visit(c,BobinskyVisit.K);
            v.putUUID("Owner",owner);v.putInt(legacy?"Elapsed":"ReturnTicks",legacy?10000:600);data.visits.put(owner,v);
            v=BobinskyVisitData.load(data.save(new CompoundTag())).visits.get(owner);
            var actor=ModEntities.BOBINSKY_VISITOR.get().create(h.getLevel());actor.setPos(Vec3.atBottomCenterOf(c.north(20)));
            BobinskyVisit.perform(p,new VisitData.Scene(),v,actor);
            h.assertTrue(v.getInt("Phase")==BobinskyVisit.WAIT_HIDE&&actor.isInvisible(),"Saved/legacy return did not recover");actor.discard();
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void clearReturnReachesHomeWithoutWaitingForRecovery(GameTestHelper h){
        var c=h.absolutePos(new BlockPos(1300,240,1300));var p=player(h,c);var v=visit(c,BobinskyVisit.K);
        var actor=ModEntities.BOBINSKY_VISITOR.get().create(h.getLevel());actor.setPos(Vec3.atBottomCenterOf(BobinskyHouse.front(v)));
        for(int tick=0;tick<100&&v.getInt("Phase")==BobinskyVisit.K;tick++)BobinskyVisit.perform(p,new VisitData.Scene(),v,actor);
        h.assertTrue(v.getInt("Phase")==BobinskyVisit.WAIT_HIDE&&v.getInt("ReturnTicks")<100,"Clear return relied on timeout");actor.discard();h.succeed();
    }
    private static CompoundTag visit(BlockPos c,int phase){var v=new CompoundTag();v.putLong("House",c.asLong());v.putInt("Phase",phase);v.putBoolean("Active",true);v.putBoolean("VoiceDone",true);return v;}
    private static ServerPlayer player(GameTestHelper h,BlockPos c){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ReturnQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(Vec3.atBottomCenterOf(c.north(14)));return p;
    }
}
