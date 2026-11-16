package com.coraline.parents;
import com.coraline.feast.*;
import com.coraline.registry.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** One valid family bed identity. Refunds revoke old copies rather than minting usable duplicates. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ParentsBedSafety {
    private static UUID token(MinecraftServer server,FamilyData.Family f){if(!f.bedCustody.hasUUID("Token")){f.bedCustody.putUUID("Token",UUID.randomUUID());FamilyData.get(server).setDirty();}return f.bedCustody.getUUID("Token");}
    public static void stamp(ItemStack stack,UUID family){var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;var f=FamilyData.get(server).families.get(family);if(f!=null)stack.getOrCreateTagElement("BlockEntityTag").putUUID("BedToken",token(server,f));}
    private static boolean matches(ItemStack stack,FamilyData.Family f){var tag=stack.getTagElement("BlockEntityTag");return stack.is(ModItems.PARENTS_BED.get())&&tag!=null&&tag.hasUUID("Family")&&f.id.equals(tag.getUUID("Family"))&&(!tag.hasUUID("BedToken")?f.bedCustody.getInt("Revision")==0:tag.getUUID("BedToken").equals(f.bedCustody.getUUID("Token")));}
    public static boolean validPlaced(MinecraftServer server,UUID owner){
        for(var f:FamilyData.get(server).families.values())if(owner.equals(f.owner)&&f.bed!=null){var level=FamilyData.level(server,f.homeDim);if(level==null)return false;level.getChunkAt(f.bed);if(!(level.getBlockEntity(f.bed) instanceof ParentsBedEntity bed)||!f.id.equals(bed.familyId))return false;
            var state=level.getBlockState(f.bed);for(int part=0;part<4;part++)if(!level.getBlockState(ParentsBedBlock.partPos(f.bed,state.getValue(ParentsBedBlock.FACING),part)).is(ModBlocks.PARENTS_BED.get()))return false;return true;}
        return false;
    }
    public static void retireRevoked(ItemStack stack,ServerLevel level){
        if(!stack.is(ModItems.PARENTS_BED.get()))return;
        var tag=stack.getTagElement("BlockEntityTag");if(tag==null||!tag.hasUUID("Family"))return;var f=FamilyData.get(level.getServer()).families.get(tag.getUUID("Family"));if(f!=null&&f.bedCustody.hasUUID("Token")&&!matches(stack,f))stack.setCount(0);
    }
    public static boolean allowPlacement(ServerPlayer p,ItemStack stack){var tag=stack.getTagElement("BlockEntityTag");if(tag==null||!tag.hasUUID("Family"))return true;var f=FamilyData.get(p.server).families.get(tag.getUUID("Family"));if(f==null)return false;token(p.server,f);
        if(f.bed!=null&&!validPlaced(p.server,f.owner)){f.bedCustody.putLong("LastPlaced",f.bed.asLong());f.bed=null;f.chest=null;FamilyData.get(p.server).setDirty();}
        if(f.bed!=null||!p.getUUID().equals(f.owner)||!matches(stack,f)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("This family's bed is already placed, or this copy has been replaced."),true);return false;}stamp(stack,f.id);return true;
    }
    public static boolean protectedAt(ServerLevel level,BlockPos pos){var state=level.getBlockState(pos);if(!state.is(ModBlocks.PARENTS_BED.get()))return false;var anchor=ParentsBedBlock.anchorPos(pos,state);if(!(level.getBlockEntity(anchor) instanceof ParentsBedEntity bed))return false;var f=bed.family();if(f==null||f.owner==null)return false;
        var scene=VisitData.get(level.getServer()).scenes.get(f.owner);
        if(scene!=null&&(RescueChapter.finished(scene)||scene.loop.story.getBoolean("BeldamDefeatedByGhostEscape")))return false;
        // Lock from the tunnel exit, including the Overworld aftermath before
        // Wybie's visit marks the first escape completely finished.
        if(scene!=null&&(FirstEscapeStory.data(scene).getBoolean("Complete")||FirstEscapeStory.engaged(scene)&&FirstEscapeTimeline.overworld(FirstEscapeStory.phase(scene))))return true;
        var owner=level.getServer().getPlayerList().getPlayer(f.owner);return owner!=null&&com.coraline.otherworld.OtherWorld.isOther(owner.level());
    }
    @SubscribeEvent public static void protect(BlockEvent.BreakEvent e){if(e.getLevel() instanceof ServerLevel level&&protectedAt(level,e.getPos()))e.setCanceled(true);}
    @SubscribeEvent public static void explode(ExplosionEvent.Detonate e){if(e.getLevel() instanceof ServerLevel level)e.getAffectedBlocks().removeIf(pos->protectedAt(level,pos));}
    private static boolean inventoryPresent(ServerPlayer player,FamilyData.Family f){
        for(var slot:player.containerMenu.slots)if(matches(slot.getItem(),f))return true;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(matches(player.getInventory().getItem(i),f))return true;
        for(int i=0;i<player.getEnderChestInventory().getContainerSize();i++)if(matches(player.getEnderChestInventory().getItem(i),f))return true;
        return false;
    }
    private static boolean present(ServerPlayer owner,FamilyData.Family f){
        var server=owner.server;
        if(f.bed!=null){if(validPlaced(server,f.owner))return true;f.bedCustody.putLong("LastPlaced",f.bed.asLong());f.bed=null;f.chest=null;FamilyData.get(server).setDirty();}
        // Login/recovery callers can exist before player-list registration.
        // Check that caller directly as well as all other connected inventories.
        if(inventoryPresent(owner,f))return true;
        for(var player:server.getPlayerList().getPlayers())if(player!=owner&&inventoryPresent(player,f))return true;
        for(var level:server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof ItemEntity item&&!item.isRemoved()&&matches(item.getItem(),f))return true;
        return false;
    }
    public static String recover(ServerPlayer owner,FamilyData.Family f,boolean automatic){
        if(!owner.getUUID().equals(f.owner))return "This bed belongs to another family.";token(owner.server,f);
        if(present(owner,f)){if(automatic){f.bedCustody.remove("LostAt");FamilyData.get(owner.server).setDirty();}return "Your parents' bed still exists; no duplicate was created.";}
        if(owner.getInventory().getFreeSlot()<0)return "Free one inventory slot for your parents' bed.";
        f.bedCustody.putUUID("Token",UUID.randomUUID());f.bedCustody.putInt("Revision",f.bedCustody.getInt("Revision")+1);FamilyData.get(owner.server).setDirty();
        var replacement=ParentsBedItem.forFamily(f.id);if(!owner.getInventory().add(replacement))return "Free one inventory slot for your parents' bed.";
        f.bedCustody.remove("LostAt");FamilyData.get(owner.server).setDirty();owner.getInventory().setChanged();return "Your parents' lost bed has been returned. Older copies cannot create another bed.";
    }
    public static void destroyed(ItemEntity item){if(item.level().isClientSide)return;var server=item.getServer();var tag=item.getItem().getTagElement("BlockEntityTag");if(tag==null||!tag.hasUUID("Family"))return;var f=FamilyData.get(server).families.get(tag.getUUID("Family"));if(f==null||!matches(item.getItem(),f)||f.bed!=null)return;f.bedCustody.putLong("LostAt",server.overworld().getGameTime());FamilyData.get(server).setDirty();}
    @SubscribeEvent public static void expired(ItemExpireEvent e){destroyed(e.getEntity());}
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent e){if(e.getEntity() instanceof ItemEntity item&&item.getRemovalReason()==net.minecraft.world.entity.Entity.RemovalReason.KILLED)destroyed(item);}
    @SubscribeEvent public static void recoverLost(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%100!=0)return;for(var f:FamilyData.get(p.server).families.values())if(p.getUUID().equals(f.owner)&&f.bedCustody.contains("LostAt")&&p.server.overworld().getGameTime()-f.bedCustody.getLong("LostAt")>=40){String message=recover(p,f,true);if(message.startsWith("Your parents' lost bed"))p.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));}}
    private ParentsBedSafety(){}
}
