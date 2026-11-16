package com.coraline.tycoon;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class TycoonWelcomeCake {
    public static boolean eligible(ServerPlayer p,TycoonData.Resident r){
        var visit=r.n.getCompound("Visit");if(!r.active()||r.n.getInt("VisitCount")!=1||r.n.getBoolean("FirstVisitEnded")||r.n.getBoolean("welcome_home_cake_received")||r.n.contains("CakeGift")||r.n.contains("Meal")||!visit.getString("Phase").equals("INSIDE")||!visit.hasUUID("Session")||!r.n.hasUUID("Entity"))return false;
        if(r.n.hasUUID("FirstVisitSession")&&!visit.getUUID("Session").equals(r.n.getUUID("FirstVisitSession")))return false;
        var level=p.server.getLevel(TycoonWorld.KEY);return level!=null&&level.getEntity(r.n.getUUID("Entity")) instanceof TycoonNpc a&&TycoonWorld.owner(p).equals(a.owner)&&a.resident.equals(r.id);
    }
    public static boolean give(ServerPlayer p,TycoonNpc actor){if(TycoonFoodContent.index(p.getMainHandItem())!=7)return false;var r=TycoonCare.resident(actor);
        if(r==null||!TycoonWorld.owner(p).equals(actor.owner)||!r.n.hasUUID("Entity")||!r.n.getUUID("Entity").equals(actor.getUUID())||p.level()!=actor.level()||p.serverLevel().dimension()!=TycoonWorld.KEY||p.distanceToSqr(actor)>16||!TycoonWorld.owned(p,p.getMainHandItem())||!eligible(p,r)){TycoonWorld.message(p,"Only your currently present first-time visitors who have not received cake can accept it. Cake was not consumed.");return true;}
        var visit=r.n.getCompound("Visit");var gift=new CompoundTag();gift.putUUID("Receipt",java.util.UUID.randomUUID());gift.putUUID("Session",visit.getUUID("Session"));gift.putInt("Progress",0);gift.put("Item",p.getMainHandItem().copyWithCount(1).save(new CompoundTag()));var patch=new CompoundTag();patch.put("CakeGift",gift);patch.putUUID("FirstVisitSession",visit.getUUID("Session"));
        String result=TycoonTransactions.consumeHeld(p,p.getMainHandItem().copyWithCount(1),new CompoundTag(),r.id,patch);if(!r.n.contains("CakeGift")){TycoonWorld.message(p,result);return true;}
        TycoonWorld.message(p,r.name()+": "+(r.trait("Sensitivity")>.7?"Oh... is that really for me?":r.trait("Activity")>.65?"Whoa! That's amazing!":r.trait("Suspiciousness")>.6?"You made this for me?":"Thank you! It looks delicious!"));TycoonFoodSync.send(p,true);return true;
    }
    public static boolean tick(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){if(!r.n.contains("CakeGift"))return false;var gift=r.n.getCompound("CakeGift");int ticks=gift.getInt("Progress")+20;gift.putInt("Progress",ticks);actor.getNavigation().stop();actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.of(gift.getCompound("Item")));actor.gesture(ticks<40?8:1,25);
        if(ticks<120){TycoonWorld.dirty(p);return true;}
        if(!r.n.getBoolean("welcome_home_cake_received")){TycoonDialogue.set(r,TycoonDialogue.trust(r)+.20,TycoonDialogue.suspicion(r)-.20);r.n.putBoolean("welcome_home_cake_received",true);r.n.putUUID("CakeGiftReceipt",gift.getUUID("Receipt"));}
        r.n.remove("CakeGift");actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);actor.gesture(0,0);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();TycoonFoodSync.send(p,true);return true;
    }
    private TycoonWelcomeCake(){}
}
