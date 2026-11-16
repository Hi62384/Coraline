package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

/** Registry and denied-context extraction tests only. Successful live pin-game
 * play, cost and renderer acceptance require an actual bound postgame level. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class SiteLockpickGameTests {
    @GameTest(template="empty")
    public static void actualLockpickHasItsIronCraftingRouteAndCannotTargetOrdinaryChests(GameTestHelper h){
        var item=com.coraline.clothing.ArtifactItems.OTHER_IRON_LOCKPICK.get();var stack=new ItemStack(item);
        h.assertTrue(item instanceof OtherIronLockpick&&stack.getMaxDamage()==96,"Real consumable lockpick missing");
        var raw=h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("coraline","other_iron_lockpick")).orElse(null);
        h.assertTrue(raw instanceof ShapedRecipe,"Lockpick has no actual survival recipe");var recipe=(ShapedRecipe)raw;int iron=0,sticks=0;
        for(var ingredient:recipe.getIngredients())if(!ingredient.isEmpty()){
            if(ingredient.test(new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get())))iron++;
            else if(ingredient.test(new ItemStack(Items.STICK)))sticks++;else h.fail("Unexpected lockpick ingredient");
        }
        h.assertTrue(iron==2&&sticks==1&&recipe.getResultItem(h.getLevel().registryAccess()).is(item)&&recipe.getResultItem(h.getLevel().registryAccess()).getCount()==1,"Lockpick lost its intended iron cost/result");
        var at=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlock(at,Blocks.CHEST.defaultBlockState(),2);
        var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LockpickQA"));player.setPos(at.getCenter());player.setItemSlot(EquipmentSlot.MAINHAND,stack);
        var result=item.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter(),net.minecraft.core.Direction.UP,at,false)));
        h.assertTrue(result==net.minecraft.world.InteractionResult.PASS&&stack.getDamageValue()==0,"Lockpick intercepted/charged an unrelated ordinary container");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void cacheDeniesForeignWorldHoppersReadsAndFutureMetadataWithoutRollingLoot(GameTestHelper h){
        var epoch=UUID.fromString("11111111-2222-3333-4444-555555555555");InfiniteStructurePlan.Plan p=null;
        for(int x=-12288;x<=12288&&p==null;x+=768)for(int z=-12288;z<=12288;z+=768){var value=InfiniteStructurePlan.at(epoch,124119,InfiniteStructurePlan.Family.DOLL_WORKSHOP,x,z);if(value.isPresent()){p=value.orElseThrow();break;}}
        h.assertTrue(p!=null,"No real cache receipt fixture");int index=-1;for(int i=0;i<p.modules().size();i++)if(p.modules().get(i).role()==InfiniteStructurePlan.Role.STORAGE){index=i;break;}
        h.assertTrue(index>=0,"No real basic storage lock module");
        var room=p.modules().get(index);var at=new BlockPos(p.x()+room.x()+1,p.y()+room.y()+1,p.z()+room.z()+1);h.getLevel().getChunkAt(at);
        h.getLevel().setBlock(at,com.coraline.registry.ModBlocks.INFINITE_SITE_CACHE.get().defaultBlockState(),2);
        h.assertTrue(h.getLevel().getBlockEntity(at) instanceof SiteCacheBlockEntity,"Custom chest block/entity type failed actual creation");
        var cache=(SiteCacheBlockEntity)h.getLevel().getBlockEntity(at);cache.configure(p,index);cache.setLootTable(ResourceLocation.fromNamespaceAndPath("coraline","postgame/structures/doll_workshop"),123);
        h.assertTrue(cache.saveWithFullMetadata().getCompound("CoralineSiteCache").hasUUID("Site")&&cache.tier()==1,"Actual basic cache was not configured from its structure receipt");
        h.assertTrue(cache.locked()&&!cache.authentic(),"Snapshot alone granted use in a foreign generator");
        h.assertTrue(!cache.getCapability(ForgeCapabilities.ITEM_HANDLER,net.minecraft.core.Direction.DOWN).isPresent()&&cache.getSlotsForFace(net.minecraft.core.Direction.DOWN).length==0,"Hopper/capability exposed a locked inventory");
        cache.unpackLootTable(null);cache.clearContent();cache.setItem(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(cache.getItem(0).isEmpty()&&cache.removeItem(0,64).isEmpty()&&cache.removeItemNoUpdate(0).isEmpty(),"Locked direct container API leaked/accepted items");
        var saved=cache.saveWithFullMetadata();h.assertTrue(saved.contains("LootTable"),"Denied access consumed the once-only loot table");
        var future=saved.getCompound("CoralineSiteCache").copy();future.putInt("Version",999);future.putString("OpaqueState","preserve");saved.put("CoralineSiteCache",future);cache.load(saved);
        h.assertTrue(cache.locked()&&!cache.unlock(p.site())&&cache.saveWithFullMetadata().getCompound("CoralineSiteCache").equals(future),"Future metadata unlocked or was reset");
        h.assertTrue(cache.saveWithFullMetadata().contains("LootTable"),"Future receipt load discarded pending loot");
        saved.putString("CoralineSiteCache","unknown future representation");cache.load(saved);cache.configure(p,index);
        h.assertTrue(cache.locked()&&cache.saveWithFullMetadata().getString("CoralineSiteCache").equals("unknown future representation"),"Unfamiliar non-compound receipt was reset/reconfigured");h.succeed();
    }
}
