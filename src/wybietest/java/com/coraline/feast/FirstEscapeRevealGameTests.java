package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FirstEscapeRevealGameTests {
    private static ServerPlayer owner(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);
        var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DoorRevealQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=new BlockPos(129200,180,129200);s.loop.event.putInt("Serial",2);
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);p.setPos(s.table.getCenter());
        var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putUUID("Token",UUID.randomUUID());n.putUUID("Choice",UUID.randomUUID());n.putLong("House",s.table.asLong());n.putLong("LittleDoor",FirstEscapeScene.at(s,14,6,38).asLong());n.putLong("Prison",s.table.above(40).asLong());n.putLong("SafePlayer",p.blockPosition().asLong());return p;
    }
    @GameTest(template="empty",batch="sealed-room-reveal")
    public static void roomIsPreparedWhileDoorsAreStillClosed(GameTestHelper h){
        var p=owner(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var n=FirstEscapeStory.data(s);
        try{
            for(int z:new int[]{41,42}){var at=FirstEscapeScene.at(s,31,6,z);l.getChunkAt(at);l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);var door=Blocks.OAK_DOOR.defaultBlockState();l.setBlock(at,door,2);l.setBlock(at.above(),door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);}
            var wall=FirstEscapeScene.at(s,20,8,52);l.getChunkAt(wall);l.setBlock(wall,Blocks.WHITE_CONCRETE.defaultBlockState(),2);
            FirstEscapeStory.go(p,s,FirstEscapeTimeline.Phase.PRY_DOOR);
            h.assertTrue(n.getBoolean("RoomBuilt")&&l.getBlockState(wall).is(ModBlocks.INSECT_PARLOR_WALLPAPER.get()),"Room not prepared during the closed-door cane animation");
            var little=BlockPos.of(n.getLong("LittleDoor"));h.assertTrue(l.getBlockState(little).getValue(TinyDoorBlock.OPEN)&&l.getBlockState(little).getValue(TinyDoorBlock.PORTAL),"Cane reveal prepares a closed Little Door instead of its portal");
            for(int z:new int[]{41,42})h.assertFalse(l.getBlockState(FirstEscapeScene.at(s,31,6,z)).getValue(DoorBlock.OPEN),"Room preparation exposed the new interior");
            var environment=n.getCompound("Environment").copy();FirstEscapeStory.go(p,s,FirstEscapeTimeline.Phase.ROOM_DARK);
            h.assertTrue(environment.equals(n.getCompound("Environment")),"Visible reveal rewrote the prepared room geometry");
        }finally{FirstEscapeScene.restore(p,s);VisitData.get(p.server).scenes.remove(s.owner);}
        h.succeed();
    }
    @GameTest(template="empty")
    public static void revealKeepsHouseLightingAndPrisonHeadProjectsBeyondWall(GameTestHelper h){
        for(String phase:new String[]{"FIND_CANE","CANE_COLLECTED","PRY_DOOR","ROOM_DARK","ROOM_TRANSFORMING","COCOA_A","COCOA_G"})h.assertTrue(FirstEscapeVisualRules.inheritedDarkHouse(phase),"Opening changed the inherited ambient lighting: "+phase);
        h.assertFalse(FirstEscapeVisualRules.inheritedDarkHouse("MIRROR_A")||FirstEscapeVisualRules.inheritedDarkHouse("ESCAPE_A"),"Prison/rescue inherited the house darkening");
        h.assertTrue(FirstEscapeVisualRules.prisonHeadOffset(1)>.5,"Revealed head remains buried in the wall");
        h.assertTrue(FirstEscapeVisualRules.prisonHeadOffset(0)<0,"Retracted head no longer hides in the wall");h.succeed();
    }
    @GameTest(template="empty",batch="washed-story-door",timeoutTicks=300)
    public static void washedDoorRecoversIdentityAndTheRealOpenInteraction(GameTestHelper h){
        var p=owner(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var n=FirstEscapeStory.data(s);var at=BlockPos.of(n.getLong("LittleDoor"));
        try{
            LoopProps.hold(l,at);l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);l.setBlock(at,Blocks.WATER.defaultBlockState(),2);n.putString("LittleDoorFacing","east");n.putString("Phase","OPEN_LITTLE_DOOR");n.putUUID("Choice",UUID.randomUUID());p.setPos(at.getCenter().add(1,0,0));
            var matching=new ItemStack(ModItems.TINY_DOOR.get());matching.getOrCreateTag().putUUID("CoralineDoor",s.door);
            var unrelated=new ItemStack(ModItems.TINY_DOOR.get());unrelated.getOrCreateTag().putUUID("CoralineDoor",UUID.randomUUID());
            var chunk=new net.minecraft.world.level.ChunkPos(at);l.setChunkForced(chunk.x,chunk.z,true);boolean[] ran={false};int[] waiting={0};
            h.onEachTick(()->{if(ran[0])return;if(!l.areEntitiesLoaded(chunk.toLong())){if(++waiting[0]>=280){l.setChunkForced(chunk.x,chunk.z,false);VisitData.get(p.server).scenes.remove(s.owner);h.fail("Fixture entity section did not load within its bounded setup wait");}return;}ran[0]=true;
                // Forge replaces vanilla ItemEntity door drops with the real
                // tracked type on join. Assert that type, not discarded wrappers.
                var stale=ModEntities.TRACKED_DOOR.get().create(l);var keep=ModEntities.TRACKED_DOOR.get().create(l);stale.setPos(at.getCenter());keep.setPos(at.getCenter());stale.setItem(matching);keep.setItem(unrelated);l.addFreshEntity(stale);l.addFreshEntity(keep);try{
                h.assertTrue(FirstEscapeStory.repairLittleDoor(p,s),"Legacy water-damaged door did not recover");
                var door=(DoorBlockEntity)l.getBlockEntity(at);h.assertTrue(s.door.equals(door.doorId)&&!door.getBlockState().getValue(TinyDoorBlock.OPEN),"Recovery changed identity or skipped the closed-door interaction");
                h.assertTrue(stale.isRemoved()&&!keep.isRemoved(),"Recovery kept the exact stale copy or deleted unrelated drops");
                h.assertTrue(FirstEscapeStory.block(p,at)&&FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.ESCAPE_E&&door.passageBuilt,"Recovered door cannot form its passage and continue the quest");
                keep.discard();TunnelPassage.restore(l,door);l.setBlock(at,Blocks.GOLD_BLOCK.defaultBlockState(),2);
                h.assertFalse(FirstEscapeStory.repairLittleDoor(p,s),"Recovery overwrote a solid player block");
            }finally{FirstEscapeScene.restore(p,s);l.setChunkForced(chunk.x,chunk.z,false);VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();});
        }catch(RuntimeException error){VisitData.get(p.server).scenes.remove(s.owner);throw error;}
    }
    @GameTest(template="empty",batch="mirror-throw-tail")
    public static void throwStartsFortyTicksBeforeEndWithoutStoppingTheVoice(GameTestHelper h){
        var p=owner(h);var s=OtherMotherLoop.scene(p);var n=FirstEscapeStory.data(s);n.putString("Phase","COCOA_G");n.putBoolean("AtMirror",true);n.putBoolean("Grabbed",true);
        try{
            h.assertTrue(ShowPlayback.start(p,s,"cocoa_beetles_g",false),"Original G recording unavailable");var audio=ShowPlayback.data(s);var nonce=audio.getUUID("Nonce");ShowPlayback.signal(p,nonce,"cocoa_beetles_g",0);
            int duration=ShowRecordings.duration("cocoa_beetles_g"),cue=FirstEscapeVisualRules.mirrorThrowCue(duration);h.assertTrue(cue==duration-40,"Throw is not two seconds before the original line ends");
            FirstEscapeStory.timedMirrorThrow(p,s,cue-1);h.assertFalse(n.contains("MirrorThrowAt"),"Throw started early");
            FirstEscapeStory.timedMirrorThrow(p,s,cue);h.assertTrue(n.getInt("MirrorThrowAt")==cue&&n.getInt("MirrorThrowFrame")==1,"Throw did not start at the exact cue");
            FirstEscapeStory.timedMirrorThrow(p,s,cue+29);h.assertTrue(n.getBoolean("MirrorPrisonEntered")&&p.distanceToSqr(FirstEscapeScene.prison(s).getCenter())<16,"Player was not in the prison before G ended");
            h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.COCOA_G&&nonce.equals(audio.getUUID("Nonce"))&&audio.getString("Sound").equals("cocoa_beetles_g")&&!audio.getBoolean("Finished"),"Throw cut off/replaced the original voice tail");
        }finally{ShowPlayback.stop(p,s);FirstEscapeScene.restore(p,s);VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
}
