package com.coraline.feast;

import java.util.function.Predicate;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Saved original-item custody, with one explicitly receipted legacy swallowed-key migration. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class StoryItemCustody {
    public static boolean stone(ItemStack stack,VisitData.Scene s){return stack.is(com.coraline.registry.ModItems.SEEING_STONE.get())&&stack.hasTag()&&stack.getTag().hasUUID("StoryOwner")&&s.owner.equals(stack.getTag().getUUID("StoryOwner"))&&stack.getTag().hasUUID("StoryDoor")&&s.door.equals(stack.getTag().getUUID("StoryDoor"));}
    public static boolean capture(ServerPlayer p,VisitData.Scene s,String key,Predicate<ItemStack> matches){
        if(!s.owner.equals(p.getUUID()))return false;
        String sharedKind=key.equals("Key")?"key":key.equals("Stone")?"stone":key.startsWith("RetiredEye")?"eye_"+key.substring(10):"";
        if(!sharedKind.isEmpty()&&!com.coraline.multiplayer.QuestItemRecovery.recall(p,s,sharedKind))return false;
        var root=ParentRescueStory.data(s);var custody=root.getCompound("Custody");root.put("Custody",custody);
        if(custody.getCompound(key).getBoolean("Returned")||custody.getCompound(key).getBoolean("Destroyed"))return false;
        take(p.getInventory(),custody,key,matches);take(p.getEnderChestInventory(),custody,key,matches);
        var location=root.getCompound("ItemLocations").getCompound(key);if(location.contains("Position")){var level=com.coraline.parents.FamilyData.level(p.server,location.getString("Dimension"));if(level!=null){var pos=net.minecraft.core.BlockPos.of(location.getLong("Position"));level.getChunkAt(pos);if(level.getBlockEntity(pos) instanceof Container container)take(container,custody,key,matches);}}
        var cursor=p.containerMenu.getCarried();if(remove(cursor,custody,key,matches,0))p.containerMenu.setCarried(cursor);
        // The currently opened chest and nested shulker/bundle inventories are explicit locations.
        for(var slot:p.containerMenu.slots){var stack=slot.getItem();if(remove(stack,custody,key,matches,0)){slot.set(stack);slot.setChanged();}}
        // The authored secret cabinets store their real inventory in the owner's
        // SavedData, rather than in a vanilla block entity. Inspect only item lists.
        for(var entry:s.loop.secrets.world.getCompound("HouseCabinets").getList("Cabinets",Tag.TAG_COMPOUND)){
            var items=((CompoundTag)entry).getList("Items",Tag.TAG_COMPOUND);for(int i=items.size()-1;i>=0;i--){var saved=items.getCompound(i);var stack=ItemStack.of(saved);if(remove(stack,custody,key,matches,0)){if(stack.isEmpty())items.remove(i);else stack.save(saved);}}
        }
        for(var level:p.server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof ItemEntity dropped&&remove(dropped.getItem(),custody,key,matches,0)){if(dropped.getItem().isEmpty())dropped.discard();}
        if(key.equals("Key")&&custody.getCompound(key).contains("Stack"))root.putInt("KeyCustodyVersion",1);
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();OtherMotherLoop.changed(p);return custody.getCompound(key).contains("Stack");
    }
    public static boolean recoverSwallowedKey(ServerPlayer p,VisitData.Scene s){
        if(s==null||!s.owner.equals(p.getUUID()))return false;
        var root=ParentRescueStory.data(s);var record=root.getCompound("Custody").getCompound("Key");
        if(record.getBoolean("Returned")||record.getBoolean("Destroyed"))return false;
        if(!held(s,"Key").isEmpty())return true;
        // An existing (even damaged) receipt is not permission to manufacture another item.
        if(record.contains("Stack")||root.hasUUID("LegacyKeyReplacementReceipt"))return false;
        var story=s.loop.story;
        if(!ParentRescueStory.started(s)||!root.getBoolean("KeyTaken")||!root.getBoolean("KeySwallowed")||!story.getBoolean("LittleDoorKeyIssued")||!story.getString("LittleDoorKeyHolder").equals("EVIL_OTHER_MOTHER"))return false;
        if(capture(p,s,"Key",stack->ParentRescueStory.key(stack,s)))return true;
        var mother=ParentRescueStory.mother(p,s);
        if(mother!=null)for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.MAINHAND,net.minecraft.world.entity.EquipmentSlot.OFFHAND}){
            var stack=mother.getItemBySlot(slot);if(ParentRescueStory.key(stack,s)){record.put("Stack",stack.copyWithCount(1).save(new CompoundTag()));mother.setItemSlot(slot,ItemStack.EMPTY);root.getCompound("Custody").put("Key",record);root.putInt("KeyCustodyVersion",1);OtherMotherLoop.changed(p);return true;}
        }
        if(root.getInt("KeyCustodyVersion")>0||story.getInt("LittleDoorKeyCustodyVersion")>0)return false;
        var receipt=java.util.UUID.randomUUID();var replacement=ParentRescueStory.boundKey(s);var tag=replacement.getOrCreateTag();
        tag.putBoolean("LegacyReplacement",true);tag.putUUID("LegacyReplacementReceipt",receipt);
        record.put("Stack",replacement.save(new CompoundTag()));record.putUUID("LegacyReplacementReceipt",receipt);
        root.getCompound("Custody").put("Key",record);root.putUUID("LegacyKeyReplacementReceipt",receipt);root.putInt("KeyCustodyVersion",1);
        OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();return true;
    }
    private static void take(Container container,CompoundTag root,String key,Predicate<ItemStack> matches){for(int i=0;i<container.getContainerSize();i++){var stack=container.getItem(i);if(remove(stack,root,key,matches,0))container.setItem(i,stack);}container.setChanged();}
    private static boolean remove(ItemStack stack,CompoundTag root,String key,Predicate<ItemStack> matches,int depth){
        if(stack.isEmpty()||depth>8)return false;
        if(matches.test(stack)){var record=root.getCompound(key);if(!record.contains("Stack")){record.put("Stack",stack.copyWithCount(1).save(new CompoundTag()));root.put(key,record);}stack.setCount(0);return true;}
        boolean changed=false;if(!stack.hasTag())return false;
        for(var tag:new CompoundTag[]{stack.getTag(),stack.getTagElement("BlockEntityTag")})if(tag!=null){var items=tag.getList("Items",Tag.TAG_COMPOUND);for(int i=items.size()-1;i>=0;i--){var entry=items.getCompound(i);var nested=ItemStack.of(entry);if(remove(nested,root,key,matches,depth+1)){changed=true;if(nested.isEmpty())items.remove(i);else nested.save(entry);}}}
        return changed;
    }
    public static ItemStack held(VisitData.Scene s,String key){var record=ParentRescueStory.data(s).getCompound("Custody").getCompound(key);return record.getBoolean("Destroyed")||record.getBoolean("Returned")?ItemStack.EMPTY:ItemStack.of(record.getCompound("Stack"));}
    public static boolean giveBack(ServerPlayer p,VisitData.Scene s,String key){
        if(!p.getUUID().equals(s.owner))return false;var record=ParentRescueStory.data(s).getCompound("Custody").getCompound(key);if(record.getBoolean("Returned"))return true;
        var item=held(s,key);if(item.isEmpty()||p.getInventory().getFreeSlot()<0)return false;
        if(!p.getInventory().add(item))return false;record.putBoolean("Returned",true);p.getInventory().setChanged();OtherMotherLoop.changed(p);return true;
    }
    /** A returned authentic key remains a saved custody receipt even if the
     * physical inventory item was later destroyed. This grants no new item. */
    public static boolean returnedOriginalKey(VisitData.Scene s){
        if(s==null)return false;
        var record=ParentRescueStory.data(s).getCompound("Custody").getCompound("Key");
        return record.getBoolean("Returned")&&!record.getBoolean("Destroyed")
                &&ParentRescueStory.key(ItemStack.of(record.getCompound("Stack")),s);
    }
    public static void destroy(VisitData.Scene s,String key){var record=ParentRescueStory.data(s).getCompound("Custody").getCompound(key);if(record.contains("Stack")&&!record.getBoolean("Returned"))record.putBoolean("Destroyed",true);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void stored(net.minecraftforge.event.entity.player.PlayerContainerEvent.Close event){if(!(event.getEntity() instanceof ServerPlayer player))return;var visited=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Container,Boolean>());for(var slot:event.getContainer().slots)trackContainer(player,slot.container,visited,0);}
    private static void trackContainer(ServerPlayer player,Container container,java.util.Set<Container> visited,int depth){if(depth>8||!visited.add(container))return;if(container instanceof PartyStorageGuard.CompoundParts pair){trackContainer(player,pair.coraline$first(),visited,depth+1);trackContainer(player,pair.coraline$second(),visited,depth+1);}else if(container instanceof net.minecraft.world.level.block.entity.BlockEntity block)for(int i=0;i<container.getContainerSize();i++)track(player,container.getItem(i),block,0);}
    private static void track(ServerPlayer player,ItemStack stack,net.minecraft.world.level.block.entity.BlockEntity block,int depth){
        if(depth>8||!stack.hasTag())return;var tag=stack.getTag();java.util.UUID owner=tag.hasUUID("EyeOwner")?tag.getUUID("EyeOwner"):tag.hasUUID("StoryOwner")?tag.getUUID("StoryOwner"):tag.hasUUID("StoryKeyOwner")?tag.getUUID("StoryKeyOwner"):null;var s=owner==null?null:VisitData.get(player.server).scenes.get(owner);
        if(s!=null){String key=ParentRescueStory.key(stack,s)?"Key":stone(stack,s)?"Stone":"";for(int i=0;i<3&&key.isEmpty();i++)if(RescueEyes.authentic(stack,s,i))key="RetiredEye"+i;if(!key.isEmpty()&&block.getLevel()!=null){var root=ParentRescueStory.data(s);var locations=root.getCompound("ItemLocations");var location=new CompoundTag();location.putString("Dimension",block.getLevel().dimension().location().toString());location.putLong("Position",block.getBlockPos().asLong());locations.put(key,location);root.put("ItemLocations",locations);VisitData.get(player.server).setDirty();}}
        for(var nestedTag:new CompoundTag[]{tag,stack.getTagElement("BlockEntityTag")})if(nestedTag!=null)for(var entry:nestedTag.getList("Items",Tag.TAG_COMPOUND))track(player,ItemStack.of((CompoundTag)entry),block,depth+1);
    }
    private StoryItemCustody(){}
}
