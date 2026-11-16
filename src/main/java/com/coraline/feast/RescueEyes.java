package com.coraline.feast;

import java.util.UUID;
import com.coraline.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RescueEyes {
    public static CompoundTag ledger(VisitData.Scene s){var root=ParentRescueStory.data(s);if(!root.contains("GhostEyes"))root.put("GhostEyes",new CompoundTag());return root.getCompound("GhostEyes");}
    public static CompoundTag eye(VisitData.Scene s,int index){if(index<0||index>2)throw new IllegalArgumentException("Eye identity");var all=ledger(s);String key="Eye"+index;if(!all.contains(key)){var n=new CompoundTag();n.putUUID("Identity",UUID.randomUUID());all.put(key,n);}return all.getCompound(key);}
    public static boolean all(VisitData.Scene s){return all(ledger(s));}
    static boolean all(CompoundTag eyes){if(eyes.getBoolean("Released"))return true;for(int i=0;i<3;i++)if(!eyes.getCompound("Eye"+i).getBoolean("Claimed"))return false;return true;}
    public static boolean authentic(ItemStack stack,VisitData.Scene s,int index){if(!stack.is(ModItems.GHOST_EYE.get())||!stack.hasTag())return false;var n=stack.getTag();return !ledger(s).getBoolean("Released")&&n.getInt("GhostEye")==index&&n.hasUUID("EyeOwner")&&s.owner.equals(n.getUUID("EyeOwner"))&&n.hasUUID("EyeRealm")&&s.door.equals(n.getUUID("EyeRealm"))&&n.hasUUID("EyeIdentity")&&eye(s,index).getUUID("Identity").equals(n.getUUID("EyeIdentity"));}
    public static boolean claim(ServerPlayer p,VisitData.Scene s,int index){
        if(!p.getUUID().equals(s.owner)||ledger(s).getBoolean("Released"))return false;
        var n=eye(s,index);if(n.getBoolean("Claimed"))return true;if(!n.getBoolean("Defeated")||!n.getBoolean("Discovered"))return false;
        if(p.getInventory().getFreeSlot()<0){FirstEscapeStory.hint(p,"Make one inventory space for the ghost eye.");return false;}
        var stack=new ItemStack(ModItems.GHOST_EYE.get());var tag=stack.getOrCreateTag();tag.putInt("GhostEye",index);tag.putUUID("EyeOwner",s.owner);tag.putUUID("EyeRealm",s.door);tag.putUUID("EyeIdentity",n.getUUID("Identity"));
        if(!p.getInventory().add(stack))return false;n.putBoolean("Claimed",true);n.putBoolean("Awarded",true);if(com.coraline.otherworld.OtherWorld.isOther(p.level())){n.putLong("DrainOrigin",p.blockPosition().asLong());if(index!=0)n.putLong("DrainStarted",p.serverLevel().getGameTime());n.putString("DrainDimension",p.level().dimension().location().toString());}p.getInventory().setChanged();OtherMotherLoop.changed(p);return true;
    }
    public static void startDrain(ServerPlayer p,VisitData.Scene s,int index){var n=eye(s,index);if(n.getBoolean("Claimed")&&n.contains("DrainOrigin")&&!n.contains("DrainStarted")&&com.coraline.otherworld.OtherWorld.isOther(p.level())){n.putLong("DrainStarted",p.serverLevel().getGameTime());OtherMotherLoop.changed(p);}}
    public static boolean release(ServerPlayer p,VisitData.Scene s){if(!p.getUUID().equals(s.owner)||!all(s))return false;if(ledger(s).getBoolean("Released"))return true;
        // Retire the actual capabilities, including a recorded storage location; never unrelated green items.
        for(int i=0;i<3;i++){final int index=i;if(!StoryItemCustody.capture(p,s,"RetiredEye"+i,stack->authentic(stack,s,index))){FirstEscapeStory.hint(p,"Retrieve the original ghost eyes from storage to finish their farewell.");return false;}}
        for(int i=0;i<3;i++)StoryItemCustody.destroy(s,"RetiredEye"+i);
        ledger(s).putBoolean("Released",true);s.loop.story.putBoolean("GhostChildrenFreed",true);p.getInventory().setChanged();OtherMotherLoop.changed(p);
        return true;
    }
    private RescueEyes(){}
}
