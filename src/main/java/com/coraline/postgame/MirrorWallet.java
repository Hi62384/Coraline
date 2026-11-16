package com.coraline.postgame;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Plans changes only to actual payment/output slots. Unrelated equipment or
 * capabilities are neither cloned nor rewritten during a transaction. */
final class MirrorWallet {
    record Change(int slot,ItemStack before,ItemStack after){}
    record Plan(List<Change> changes){}
    enum Failure { FUNDS, SPACE }
    record Result(Plan plan,Failure failure){}
    static boolean currency(ItemStack stack,Item item){
        if(stack.isEmpty()||!stack.is(item)||stack.getCount()>stack.getMaxStackSize()||stack.hasTag()&&!stack.getTag().isEmpty())return false;
        var n=stack.serializeNBT();return !n.contains("ForgeCaps")||n.contains("ForgeCaps",net.minecraft.nbt.Tag.TAG_COMPOUND)&&n.getCompound("ForgeCaps").isEmpty();
    }
    static long count(ServerPlayer player,Item item){long amount=0;for(int i=0;i<36;i++)if(currency(player.getInventory().getItem(i),item))amount+=player.getInventory().getItem(i).getCount();if(currency(player.getOffhandItem(),item))amount+=player.getOffhandItem().getCount();return amount;}
    private static boolean debit(ServerPlayer player,Item item,int count,Map<Integer,ItemStack> proposed){
        for(int slot=0;slot<=40&&count>0;slot++){
            if(slot>=36&&slot<40)continue;var stack=proposed.getOrDefault(slot,player.getInventory().getItem(slot));if(!currency(stack,item))continue;
            int take=Math.min(stack.getCount(),count);var after=stack.copy();after.shrink(take);proposed.put(slot,after);count-=take;
        }return count==0;
    }
    static Result plan(ServerPlayer player,ItemStack output,int diamonds){
        if(diamonds<1||output.isEmpty()||output.getCount()>64)return new Result(null,Failure.FUNDS);
        if(count(player,com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get())<diamonds||count(player,com.coraline.clothing.OtherMaterials.MOONSTONE.get())<1)return new Result(null,Failure.FUNDS);
        var proposed=new LinkedHashMap<Integer,ItemStack>();
        if(!debit(player,com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get(),diamonds,proposed)||!debit(player,com.coraline.clothing.OtherMaterials.MOONSTONE.get(),1,proposed))return new Result(null,Failure.FUNDS);
        int remaining=output.getCount();
        for(int slot=0;slot<36&&remaining>0;slot++){
            var stack=proposed.getOrDefault(slot,player.getInventory().getItem(slot));
            if(stack.isEmpty()||!ItemStack.isSameItemSameTags(stack,output)||MirrorPricing.eligibility(player.serverLevel(),stack)!=MirrorPricing.Problem.NONE)continue;
            int add=Math.min(remaining,Math.max(0,stack.getMaxStackSize()-stack.getCount()));if(add==0)continue;var after=stack.copy();after.grow(add);proposed.put(slot,after);remaining-=add;
        }
        for(int slot=0;slot<36&&remaining>0;slot++)if(proposed.getOrDefault(slot,player.getInventory().getItem(slot)).isEmpty()){
            int add=Math.min(remaining,output.getMaxStackSize());proposed.put(slot,output.copyWithCount(add));remaining-=add;
        }
        if(remaining>0)return new Result(null,Failure.SPACE);
        var changes=new ArrayList<Change>();for(var entry:proposed.entrySet())changes.add(new Change(entry.getKey(),player.getInventory().getItem(entry.getKey()).copy(),entry.getValue().copy()));
        return new Result(new Plan(List.copyOf(changes)),null);
    }
    static boolean matches(ServerPlayer player,Plan plan,boolean after){for(var change:plan.changes)if(!ItemStack.matches(player.getInventory().getItem(change.slot),after?change.after:change.before))return false;return true;}
    static void apply(ServerPlayer player,Plan plan,boolean after){for(var change:plan.changes)player.getInventory().setItem(change.slot,(after?change.after:change.before).copy());player.getInventory().setChanged();}
    private MirrorWallet(){}
}
