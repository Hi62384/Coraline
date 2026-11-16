package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

/** Real item escrow. Gift acceptance is signed and independent of food care. */
public final class TycoonGifts {
    public static String interest(ItemStack item){
        if(item.isEmpty()||TycoonFoodContent.index(item)==7)return "";
        if(item.is(Items.COMPASS)||item.is(Items.MAP)||item.is(Items.FILLED_MAP)||item.is(Items.SPYGLASS))return "Explorativeness";
        if(item.is(Items.DIAMOND)||item.is(Items.AMETHYST_SHARD)||item.is(Items.EMERALD))return "Aweness";
        if(item.is(Items.PAINTING)||item.is(Items.GOAT_HORN)||item.is(Items.NOTE_BLOCK))return "Artisticness";
        if(item.is(TycoonContent.BALL.get())||item.is(Items.SNOWBALL)||item.is(Items.SLIME_BALL))return "Playfulness";
        if(item.is(Items.BOOK)||item.is(Items.WRITABLE_BOOK))return "Socialness";
        if(TycoonFoodContent.meal(item)||item.is(Items.COOKIE)||item.is(Items.PUMPKIN_PIE))return "FoodInterest";
        // Pack makers can add category tags without editing the Java registry.
        for(String category:List.of("Explorativeness","Aweness","Artisticness","Playfulness","Socialness","FoodInterest"))if(item.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","tycoon_gifts/"+category.toLowerCase(Locale.ROOT)))))return category;
        return "";
    }
    public static boolean manual(ServerPlayer p,TycoonNpc actor){
        var r=TycoonCare.resident(actor);var item=p.getMainHandItem();String category=interest(item);
        if(category.isEmpty()||r==null||!r.active()||!TycoonVisits.inside(r)||!TycoonWorld.owner(p).equals(actor.owner)||p.serverLevel().dimension()!=TycoonWorld.KEY||p.distanceToSqr(actor)>25)return false;
        var h=TycoonWorld.home(p);if(r.n.contains("Gift")||h.clock()<r.n.getLong("GiftAfter")){TycoonWorld.message(p,r.name()+": Please give me a little time between presents.");return true;}
        if(r.trait("Giftableness")<0){
            int refusals=r.n.getInt("GiftRefusals");r.n.putInt("GiftRefusals",refusals+1);r.n.putLong("GiftAfter",h.clock()+200);
            if(refusals>0)TycoonDialogue.set(r,TycoonDialogue.trust(r),TycoonDialogue.suspicion(r)+.008*Math.abs(r.trait("Giftableness")));
            TycoonDollAnalysis.observe(r,"gift-refusal:"+h.clock(),"They declined an unsolicited present",100,h.clock());TycoonWorld.message(p,r.name()+": No thank you. Please don't keep offering me things.");TycoonWorld.dirty(p);return true;
        }
        var gift=escrow(item,category);var patch=new CompoundTag();patch.put("Gift",gift);
        TycoonWorld.message(p,TycoonTransactions.consumeHeld(p,item.copyWithCount(1),new CompoundTag(),r.id,patch));return true;
    }
    private static CompoundTag escrow(ItemStack item,String category){var n=new CompoundTag();n.putUUID("Id",UUID.randomUUID());n.put("Item",item.copyWithCount(1).save(new CompoundTag()));n.putString("Category",category);n.putInt("Progress",0);return n;}
    public static boolean tick(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        if(!r.n.contains("Gift"))return false;var n=r.n.getCompound("Gift");
        if(n.getBoolean("Reserving")&&!debit(p,n))return false;
        actor.getNavigation().stop();actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.of(n.getCompound("Item")));actor.gesture(8,25);int progress=n.getInt("Progress")+20;n.putInt("Progress",progress);
        if(progress<80){TycoonWorld.dirty(p);return true;}
        var h=TycoonWorld.home(p);var history=r.n.getList("GiftMemories",10);int repetitions=0;for(var entry:history)if(((CompoundTag)entry).getString("Category").equals(n.getString("Category")))repetitions++;
        double giftableness=r.trait("Giftableness"),preference=r.trait(n.getString("Category")),gain=Math.max(0,giftableness)*(.003+.022*preference)/(1+repetitions*.6);
        // The receipt and reward retire in the same resident SavedData snapshot.
        TycoonDialogue.set(r,TycoonDialogue.trust(r)+gain,TycoonDialogue.suspicion(r)-gain*.25);
        var memory=new CompoundTag();memory.putUUID("Receipt",n.getUUID("Id"));memory.putString("Category",n.getString("Category"));memory.put("Item",n.getCompound("Item").copy());memory.putLong("At",h.clock());history.add(memory);while(history.size()>32)history.remove(0);r.n.put("GiftMemories",history);
        TycoonDollAnalysis.observe(r,"gift:"+n.getUUID("Id"),giftableness==0?"They were indifferent to a present":preference>.65?"They particularly liked this present":"They accepted this present",180,h.clock());r.n.putLong("GiftAfter",h.clock()+TycoonAttractionConfig.GIFT_COOLDOWN.get());r.n.remove("Gift");actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();
        if(n.getBoolean("FromStorage")){var l=p.server.getLevel(TycoonWorld.KEY);if(l!=null&&l.getBlockEntity(BlockPos.of(n.getLong("Source"))) instanceof TycoonBlockEntity be){be.giftDebits.remove(n.getUUID("Id").toString());be.setChanged();}}
        TycoonWorld.message(p,r.name()+": "+(giftableness==0?"Oh. Thank you, I suppose.":preference>.65?"You remembered something I really like!":"Thank you for thinking of me."));return true;
    }
    public static boolean storage(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor,CompoundTag structure){
        var h=TycoonWorld.home(p);var activity=r.n.getCompound("Activity");if(r.trait("Giftableness")<=0||h.clock()<r.n.getLong("GiftAfter")||activity.getBoolean("GiftTaken")||r.n.contains("Gift"))return false;
        var source=TycoonAttractions.local(structure,5,1,-5);if(!(actor.level().getBlockEntity(source) instanceof TycoonBlockEntity be)||!TycoonWorld.owner(p).equals(be.owner))return false;
        int slot=-1;double best=.35;for(int i=0;i<be.getContainerSize();i++){String category=interest(be.getItem(i));if(category.isEmpty())continue;double score=r.trait(category);if(score>best){slot=i;best=score;}}if(slot<0)return false;
        var at=TycoonMeals.approach(p,actor,source);if(at==null)return false;if(!TycoonCare.move(actor,at,.75)){r.n.putString("Task","BROWSING_GIFT_CABINET");return true;}
        if(!be.giftDebits.isEmpty())return false; // One durable inventory mutation at a time.
        var item=be.getItem(slot);var n=escrow(item,interest(item));n.putBoolean("FromStorage",true);n.putBoolean("Reserving",true);n.putLong("Source",source.asLong());n.putInt("Slot",slot);n.put("Before",item.save(new CompoundTag()));var after=item.copy();after.shrink(1);n.put("After",after.save(new CompoundTag()));r.n.put("Gift",n);activity.putBoolean("GiftTaken",true);TycoonWorld.dirty(p);
        try{if(!TycoonCheckpoints.home(p)){r.n.remove("Gift");activity.remove("GiftTaken");return false;}}catch(java.io.IOException failed){r.n.remove("Gift");activity.remove("GiftTaken");return false;}
        return debit(p,n);
    }
    private static boolean debit(ServerPlayer p,CompoundTag n){
        var l=p.server.getLevel(TycoonWorld.KEY);if(l==null)return false;var source=BlockPos.of(n.getLong("Source"));l.getChunkAt(source);
        if(!(l.getBlockEntity(source) instanceof TycoonBlockEntity be)||!TycoonWorld.owner(p).equals(be.owner))return false;
        String receipt=n.getUUID("Id").toString();if(be.giftDebits.getBoolean(receipt)){n.putBoolean("Reserving",false);return true;}
        var before=ItemStack.of(n.getCompound("Before"));int slot=n.getInt("Slot");if(slot<0||slot>=be.getContainerSize())return false;var actual=be.getItem(slot);
        if(!ItemStack.isSameItemSameTags(actual,before)||actual.getCount()!=before.getCount())return false;
        be.setItem(slot,ItemStack.of(n.getCompound("After")));be.giftDebits.putBoolean(receipt,true);be.setChanged();p.server.saveEverything(true,true,true);n.putBoolean("Reserving",false);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();return true;
    }
    private TycoonGifts(){}
}
