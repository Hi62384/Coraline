package com.coraline.secrets;
import com.coraline.feast.*;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** All mutations are on the server thread; physical evidence is a capability into this ledger. */
public final class SecretTransactions {
    public static SecretsState state(ServerPlayer p){var s=OtherMotherLoop.scene(p);return s==null?null:s.loop.secrets;}
    public static void dirty(ServerPlayer p){var s=state(p);if(s!=null)s.recalculate();OtherMotherLoop.changed(p);}
    /** Commit the spent capability before an inventory payout; stale item copies cannot resurrect it. */
    public static void checkpoint(ServerPlayer p){dirty(p);p.server.overworld().getDataStorage().save();}
    public static void give(ServerPlayer p,ItemStack stack){
        // Inventory.add discards overflow in creative mode. Insert explicitly in both game modes.
        var inventory=p.getInventory();
        for(int pass=0;pass<2&&!stack.isEmpty();pass++)for(int slot=0;slot<36&&!stack.isEmpty();slot++){
            var current=inventory.getItem(slot);
            if(pass==0&&!current.isEmpty()&&ItemStack.isSameItemSameTags(current,stack)){int count=Math.min(stack.getCount(),current.getMaxStackSize()-current.getCount());if(count>0){current.grow(count);stack.shrink(count);}}
            else if(pass==1&&current.isEmpty()){int count=Math.min(stack.getCount(),stack.getMaxStackSize());inventory.setItem(slot,stack.copyWithCount(count));stack.shrink(count);}
        }
        inventory.setChanged();p.containerMenu.broadcastChanges();
        if(!stack.isEmpty()){stack.getOrCreateTag().putUUID("SecretRewardOwner",p.getUUID());var drop=p.drop(stack,false);if(drop!=null)drop.setNoPickUpDelay();}
    }
    static ItemStack bound(ServerPlayer p,Item item,UUID token){var stack=new ItemStack(item);var n=stack.getOrCreateTag();n.putUUID("SecretOwner",p.getUUID());n.putUUID("SecretToken",token);return stack;}
    public static boolean owned(ServerPlayer p,ItemStack stack){var n=stack.getTag();return n!=null&&n.hasUUID("SecretOwner")&&n.getUUID("SecretOwner").equals(p.getUUID())&&n.hasUUID("SecretToken");}
    public static String evidence(ServerPlayer p,ItemStack stack){if(!owned(p,stack)||state(p)==null)return "";String id=state(p).issued.getOrDefault(stack.getTag().getUUID("SecretToken"),"");return SecretRegistry.ALL.containsKey(id)&&stack.is(SecretsItems.EVIDENCE.get(id).get())?id:"";}
    public static void issue(ServerPlayer p,String id){var s=state(p);if(s==null||!SecretRegistry.ALL.containsKey(id)||s.journal.contains(id)||s.issued.containsValue(id))return;UUID token=UUID.randomUUID();s.issued.put(token,id);checkpoint(p);give(p,bound(p,SecretsItems.EVIDENCE.get(id).get(),token));}
    /** Remove only genuine owner-bound forbidden evidence, never ordinary gear or filed secrets. */
    public static void confiscate(ServerPlayer p){
        var s=state(p);if(s==null)return;int removed=0;
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++){
            var item=p.getInventory().getItem(slot);
            if(item.is(SecretsItems.KEY.get())&&owned(p,item)){removed+=item.getCount();p.getInventory().setItem(slot,ItemStack.EMPTY);s.world.putBoolean("KeyMissing",false);s.exposed.add("personal_keyring");continue;}
            String id=evidence(p,item);if(id.isEmpty())continue;
            s.exposed.add(id);s.issued.remove(item.getTag().getUUID("SecretToken"));
            removed+=item.getCount();p.getInventory().setItem(slot,ItemStack.EMPTY);
        }
        if(removed>0){p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();checkpoint(p);p.displayClientMessage(Component.literal("Other Mother confiscated "+removed+" forbidden item"+(removed==1?".":"s.")),true);}
    }
    public static boolean deposit(ServerPlayer p,String id){var s=state(p);if(s==null||s.journal.contains(id))return false;for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(id.equals(evidence(p,item))){s.issued.remove(item.getTag().getUUID("SecretToken"));item.shrink(1);s.journal.add(id);checkpoint(p);p.displayClientMessage(Component.literal("Journal updated: "+SecretRegistry.ALL.get(id).name()),true);return true;}}return false;}
    public static void delete(ServerPlayer p,String id){var s=state(p);if(s!=null&&s.journal.remove(id))checkpoint(p);}
    public static boolean makeBlackmail(ServerPlayer p,Collection<String> selected){var s=state(p);var ids=new LinkedHashSet<>(selected);if(s==null||ids.isEmpty()||!s.journal.containsAll(ids)||s.blackmail.size()>=64)return false;UUID token=UUID.randomUUID();s.journal.removeAll(ids);s.blackmail.put(token,List.copyOf(ids));checkpoint(p);give(p,bound(p,SecretsItems.BLACKMAIL.get(),token));return true;}
    public static boolean resumeReward(ServerPlayer p){var s=state(p);if(s==null||!SecretsRuntime.active(p)||s.pendingReward<0)return false;SecretsNetwork.open(p,false,false);return true;}
    public static boolean offer(ServerPlayer p,ItemStack item,int forced){var s=state(p);if(s==null||!SecretsRuntime.active(p)||SecretDialogue.busy(p)||!item.is(SecretsItems.BLACKMAIL.get())||!owned(p,item)||s.pendingReward>=0)return false;
        UUID token=item.getTag().getUUID("SecretToken");if(!s.blackmail.containsKey(token))return false;
        var n=s.dialogue;n.putUUID("Offer",token);n.putInt("Forced",forced);n.putString("TransactionState",SecretDialogue.state(p).id());
        SecretDialogue.queue(p,SecretDialogue.Event.BLACKMAIL_OFFER,SecretDialogue.state(p));checkpoint(p);return true;
    }
    static ItemStack offeredItem(ServerPlayer p){var s=state(p);if(!s.dialogue.hasUUID("Offer"))return ItemStack.EMPTY;UUID token=s.dialogue.getUUID("Offer");for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.is(SecretsItems.BLACKMAIL.get())&&owned(p,item)&&token.equals(item.getTag().getUUID("SecretToken")))return item;}return ItemStack.EMPTY;}
    public static void resolveOffer(ServerPlayer p){var s=state(p);var n=s.dialogue;if(!n.hasUUID("Offer")||n.getBoolean("Decided")||!n.getBoolean("OfferSpoken")||n.getLong("Until")>OtherMotherLoop.now(p)||!n.getBoolean("Finished")&&OtherMotherLoop.now(p)<n.getLong("Until")+200)return;var item=offeredItem(p);var ids=s.blackmail.get(n.getUUID("Offer"));
        if(item.isEmpty()||ids==null){n.remove("Offer");n.remove("OfferSpoken");checkpoint(p);p.displayClientMessage(Component.literal("The offer was withdrawn. Keep the sealed blackmail with you."),true);return;}
        var scene=OtherMotherLoop.scene(p);double severity=SecretRegistry.severity(ids);int forced=n.getInt("Forced");
        boolean accepted=forced==1||forced!=0&&p.getRandom().nextDouble()<acceptance(severity,scene.loop.trust,scene.loop.suspicion);
        n.putBoolean("Decided",true);n.remove("OfferSpoken");n.putDouble("Severity",severity);s.exposed.addAll(ids);s.pendingReward=accepted?(severity>=.9?3:severity>=.6?2:severity>=.3?1:0):-1;
        if(!accepted){s.blackmail.remove(n.getUUID("Offer"));item.shrink(1);n.remove("Offer");consequences(p,false,severity);SecretDialogue.confront(p,SecretDialogue.Event.BLACKMAIL_REJECTED,OtherMotherWelcome.State.fromId(n.getString("TransactionState")));n.remove("Decided");}
        checkpoint(p);if(accepted)SecretsNetwork.open(p,false,false);
    }
    private static void consequences(ServerPlayer p,boolean accepted,double severity){var l=OtherMotherLoop.scene(p).loop;OtherMotherLoop.relationship(p,l.trust-(float)((accepted?.04:.08)+(accepted?.15:.22)*severity),l.suspicion+(float)((accepted?.04:.10)+(accepted?.15:.32)*severity));}
    public static double acceptance(double severity,double trust,double suspicion){return Math.max(.1,Math.min(.9,.25+.60*severity+.10*trust-.20*suspicion));}
    public static void reward(ServerPlayer p,int choice){var s=state(p);if(s==null||s.pendingReward<0||choice<0||choice>2)return;var n=s.dialogue;
        // Persist the selected payout before attempting delivery. Retry uses this same receipt and choice.
        if(!n.hasUUID("Receipt")){n.putUUID("Receipt",UUID.randomUUID());n.putInt("Choice",choice);checkpoint(p);}
        UUID receipt=n.getUUID("Receipt");boolean delivered=false;
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var tag=p.getInventory().getItem(slot).getTag();if(tag!=null&&tag.hasUUID("SecretReceipt")&&receipt.equals(tag.getUUID("SecretReceipt")))delivered=true;}
        if(p.serverLevel().getEntity(receipt)!=null)delivered=true;
        if(!delivered){int tier=s.pendingReward;choice=n.getInt("Choice");ItemStack reward=choice==0?new ItemStack(Items.EMERALD,4+tier*6):choice==1?new ItemStack(Items.DIAMOND,1+tier*2):new ItemStack(Items.GOLD_INGOT,8+tier*8);reward.getOrCreateTag().putUUID("SecretReceipt",receipt);
            for(int slot=0;slot<36;slot++)if(p.getInventory().getItem(slot).isEmpty()){p.getInventory().setItem(slot,reward);delivered=true;break;}
            if(!delivered){reward.getOrCreateTag().putUUID("SecretRewardOwner",p.getUUID());var drop=new net.minecraft.world.entity.item.ItemEntity(p.serverLevel(),p.getX(),p.getY(),p.getZ(),reward);drop.setUUID(receipt);drop.setNoPickUpDelay();delivered=p.serverLevel().addFreshEntity(drop);}
        }
        if(!delivered){p.displayClientMessage(Component.literal("Your reward could not be delivered. Your choice is saved; try again."),true);return;}
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();p.server.getPlayerList().saveAll();
        if(n.hasUUID("Offer")){var item=offeredItem(p);s.blackmail.remove(n.getUUID("Offer"));if(!item.isEmpty())item.shrink(1);consequences(p,true,n.getDouble("Severity"));}
        s.pendingReward=-1;n.remove("Offer");n.remove("Receipt");n.remove("Decided");
        SecretDialogue.confront(p,SecretDialogue.Event.BLACKMAIL_ACCEPTED,OtherMotherWelcome.State.fromId(n.getString("TransactionState")));checkpoint(p);p.server.getPlayerList().saveAll();
    }
    public static void discoverJournal(ServerPlayer p){var s=state(p);var scene=OtherMotherLoop.scene(p);if(s==null||s.journal.isEmpty())return;double severity=SecretRegistry.severity(s.journal);s.exposed.addAll(s.journal);s.journal.clear();s.invisibleUntil=0;s.falseUntil=0;s.combatUntil=0;OtherMotherLoop.relationship(p,scene.loop.trust-(float)(.08+SecretsConfig.JOURNAL_TRUST.get()*severity),scene.loop.suspicion+(float)(.12+SecretsConfig.JOURNAL_SUSPICION.get()*severity));s.world.putBoolean("Disturbed",true);checkpoint(p);p.displayClientMessage(Component.literal("Your journal has been found. Every page is gone."),true);}
    public static List<ItemStack> gifts(ServerPlayer p,List<ItemStack> original){var s=state(p);if(s==null)return original;if(p.getRandom().nextDouble()<s.counter(5)*.5)return List.of(new ItemStack(new Item[]{Items.ROTTEN_FLESH,Items.BONE,Items.DIRT,Items.DEAD_BUSH}[p.getRandom().nextInt(4)],2+p.getRandom().nextInt(5)));
        var result=new ArrayList<ItemStack>();for(var item:original){var copy=item.copy();if(copy.getMaxStackSize()>1)copy.setCount(Math.min(copy.getMaxStackSize(),copy.getCount()+(int)Math.ceil(copy.getCount()*s.effective(5))));else if(s.effective(5)>p.getRandom().nextDouble()&&copy.isEnchantable())copy=net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(p.getRandom(),copy,5+(int)(15*s.effective(5)),false);result.add(copy);}return result;}
    private SecretTransactions(){}
}
