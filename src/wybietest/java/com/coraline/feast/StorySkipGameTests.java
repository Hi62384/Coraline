package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class StorySkipGameTests {
    @GameTest(template="empty") public static void firstEscapeSkipConfirmsPreservesVisibleWybieAndAwardsNothing(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var at=h.absolutePos(new BlockPos(2,2,2));com.coraline.entity.CharacterEntity actor=null;
        // GameTestServer's default operator level is zero; use a real level-2
        // entry rather than op(), which would not exercise an operator request.
        p.server.getPlayerList().getOps().add(new net.minecraft.server.players.ServerOpListEntry(p.getGameProfile(),2,false));
        h.assertTrue(p.hasPermissions(2),"Operator fixture does not have level-2 permission");
        try{
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var cell=at.offset(x,0,z);l.getChunkAt(cell);l.setBlock(cell.below(),Blocks.STONE.defaultBlockState(),2);l.setBlock(cell,Blocks.AIR.defaultBlockState(),2);l.setBlock(cell.above(),Blocks.AIR.defaultBlockState(),2);}
            l.setBlock(at,ModBlocks.TINY_DOOR.get().defaultBlockState(),2);var d=new DoorData.Door();d.id=s.door;d.owner=s.owner;d.pos=at;d.dimension=l.dimension().location().toString();DoorData.get(p.server).doors.put(d.id,d);
            var family=new com.coraline.parents.FamilyData.Family();family.owner=s.owner;com.coraline.parents.FamilyData.get(p.server).families.put(family.id,family);
            p.setPos(at.getX()+.5,at.getY(),at.getZ()+3.5);p.setYRot(180);p.setXRot(0);
            actor=com.coraline.registry.CharacterCast.ENTITIES.get("wybie").get().create(l);actor.setPos(at.getX()+.5,at.getY(),at.getZ()+1.5);h.assertTrue(l.addFreshEntity(actor),"Original visible Wybie fixture rejected");h.assertTrue(CanonicalRealWybie.bind(p,s,actor),"Original actor not bound");
            var record=new CompoundTag();record.putUUID("Entity",actor.getUUID());record.putLong("Position",actor.blockPosition().asLong());record.putString("Dimension",l.dimension().location().toString());var actors=new CompoundTag();actors.put("wybie",record);s.loop.event.put("ShowActors",actors);
            h.assertTrue(ShowActors.visibleToAnyone(actor,p),"Fixture did not exercise explicit visible-actor operator cleanup");
            h.assertTrue(ShowActors.actor(p,s,"wybie")==actor,"Original actor fixture not available by its persisted lease");
            h.assertTrue(com.coraline.item.StorySkipDebugItem.request(p,false).startsWith("Confirm:"),"First Escape shortcut executed without confirmation");h.assertFalse(FirstEscapeStory.engaged(s),"Confirmation completed story");
            String result=com.coraline.item.StorySkipDebugItem.request(p,false);h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.MISSING_A&&!FirstEscapeStory.data(s).getBoolean("Complete")&&FirstEscapeStory.data(s).getBoolean("DebugAftermathStarted"),"Confirmed First Escape shortcut bypassed required aftermath: "+result+"; state="+FirstEscapeStory.data(s)+"; door="+DoorData.get(p.server).doors.get(s.door)+"; canonical="+CanonicalRealWybie.identity(s).save());
            h.assertTrue(!actor.isRemoved()&&CanonicalRealWybie.dormant(actor)&&CanonicalRealWybie.find(p,s)==actor,"Debug skip erased or cloned visible original Wybie");
            h.assertFalse(MissingParentsTea.eligible(s),"Tea bypassed the preserved Overworld Wybie cutscenes");FirstEscapeStory.data(s).putInt("Ticks",21);com.coraline.item.StorySkipDebugItem.request(p,false);com.coraline.item.StorySkipDebugItem.request(p,false);h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.MISSING_A&&FirstEscapeStory.data(s).getInt("Ticks")==21,"Repeated skip reset the active aftermath");h.assertFalse(p.getInventory().contains(new ItemStack(Items.DIAMOND_BLOCK)),"Debug shortcut generated chase reward");
        }finally{if(actor!=null)actor.discard();p.server.getPlayerList().deop(p.getGameProfile());MissingParentsTeaGameTests.remove(p,s);}
        h.succeed();
    }
    @GameTest(template="empty") public static void operatorFinaleSkipConfirmsReturnsExactKeyAndKeepsManualLock(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var at=new BlockPos(115200,180,115200);
        p.server.getPlayerList().getOps().add(new net.minecraft.server.players.ServerOpListEntry(p.getGameProfile(),2,false));
        h.assertTrue(p.hasPermissions(2),"Operator fixture does not have level-2 permission");
        try{
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var cell=at.offset(x,0,z);l.getChunkAt(cell);l.setBlock(cell.below(),Blocks.STONE.defaultBlockState(),2);l.setBlock(cell,Blocks.AIR.defaultBlockState(),2);l.setBlock(cell.above(),Blocks.AIR.defaultBlockState(),2);}
            l.setBlock(at,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.OPEN,true),2);
            var d=new DoorData.Door();d.id=s.door;d.owner=s.owner;d.pos=at;d.dimension=l.dimension().location().toString();DoorData.get(p.server).doors.put(d.id,d);
            ParentRescueStory.data(s).putBoolean("Started",true);var n=RescueChapter.data(s);n.putBoolean("Started",true);n.putString("Phase","CAT_THROW");
            for(int i=0;i<3;i++){var eye=RescueEyes.eye(s,i);eye.putBoolean("Discovered",true);eye.putBoolean("Defeated",true);h.assertTrue(RescueEyes.claim(p,s,i),"Original eye fixture not claimed");}
            var original=ParentRescueStory.boundKey(s);original.getOrCreateTag().putString("OriginalKeepsake","same physical key");p.getInventory().add(original.copy());
            h.assertTrue(StoryItemCustody.capture(p,s,"Key",stack->ParentRescueStory.key(stack,s)),"Canonical key was not put in custody");
            for(int i=3;i<p.getInventory().items.size();i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            h.assertTrue(com.coraline.item.StorySkipDebugItem.request(p,true).startsWith("Confirm:"),"Skip executed without confirmation");
            h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.CAT_THROW,"Confirmation already advanced phase");
            h.assertTrue(com.coraline.item.StorySkipDebugItem.request(p,true).contains("inventory slot"),"Full inventory did not block exact-key return");
            h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.CAT_THROW&&!n.getBoolean("DebugFinalFightSkipped"),"Failed return committed the skip");
            p.getInventory().setItem(5,ItemStack.EMPTY);com.coraline.item.StorySkipDebugItem.request(p,true);
            String result=com.coraline.item.StorySkipDebugItem.request(p,true);h.assertTrue(result.startsWith("Final encounter skipped"),"Confirmed operator shortcut failed: "+result);
            h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.LOCK_DOOR&&n.getBoolean("DebugFinalFightSkipped")&&n.getBoolean("CrawlReachedOverworld"),"Skip did not resume at manual lock");
            h.assertFalse(n.getBoolean("PermanentlySealed"),"Skip automatically locked the actual door");
            int count=0;for(var stack:p.getInventory().items)if(ParentRescueStory.key(stack,s)){count+=stack.getCount();h.assertTrue(ItemStack.isSameItemSameTags(original,stack),"Skip replaced original key metadata");}h.assertTrue(count==1,"Original key duplicated");
            com.coraline.item.StorySkipDebugItem.request(p,true);com.coraline.item.StorySkipDebugItem.request(p,true);
            h.assertTrue(p.getInventory().items.stream().filter(stack->ParentRescueStory.key(stack,s)).mapToInt(ItemStack::getCount).sum()==1,"Repeated skip duplicated the key");
            h.assertTrue(BeldamFinale.block(p,at)&&RescueChapter.phase(s)==RescueChapter.Phase.BANGING,"Manual owner/key lock failed after debug skip");
        }finally{p.server.getPlayerList().deop(p.getGameProfile());MissingParentsTeaGameTests.remove(p,s);}
        h.succeed();
    }
}
