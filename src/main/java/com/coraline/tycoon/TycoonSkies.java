package com.coraline.tycoon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class TycoonSkies {
    public static final String[] NAMES={"Empty White Void","Simple Blue Sky","Beautiful Daytime Sky","Magical Sunset","Enchanted Sky","Spectacular Supernatural Sky"};
    public static String select(ServerPlayer p,int tier){var h=TycoonWorld.home(p);boolean automated=TycoonWorkers.constructing(p);if(tier<0||tier>5||p.serverLevel().dimension()!=TycoonWorld.KEY&&!automated)return "Invalid sky selection.";var unlocked=h.state.getCompound("OwnedSkies").copy();boolean bought=tier==0||unlocked.getBoolean(Integer.toString(tier));unlocked.putBoolean(Integer.toString(tier),true);var patch=new CompoundTag();patch.put("OwnedSkies",unlocked);patch.putInt("SkyTier",tier);int cost=bought?0:TycoonPhaseTwoConfig.SKY[tier].get();String result=automated?TycoonWorkers.payConstruction(p,TycoonWorkers.contextWorker(p),cost,0,patch):TycoonTransactions.purchase(p,cost,ItemStack.EMPTY,patch,null,new CompoundTag());TycoonLandscape.recalculate(h);TycoonPhaseTwo.sync(p);return result;}
    private TycoonSkies(){}
}
