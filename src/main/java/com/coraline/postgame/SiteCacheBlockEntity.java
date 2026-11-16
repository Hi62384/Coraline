package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

/** Vanilla durable chest inventory/lazy loot, with a separate immutable site
 * receipt. Neither a GUI nonce nor loading unfamiliar metadata unlocks it. */
public final class SiteCacheBlockEntity extends ChestBlockEntity implements WorldlyContainer,SiteLockTarget {
    private static final String KEY="CoralineSiteCache";
    private CompoundTag receipt=new CompoundTag();
    private Tag opaqueReceipt;
    private boolean valid,locked=true;
    private int tier=1;
    public SiteCacheBlockEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.INFINITE_SITE_CACHE.get(),at,state);}
    void configure(InfiniteStructurePlan.Plan plan,int module){
        if(opaqueReceipt!=null||!receipt.isEmpty()||module<0||module>=plan.modules().size())return;
        if(!worldPosition.equals(SiteCacheReceipt.position(plan,module)))return;
        var prepared=SiteCacheReceipt.create(plan,module);if(prepared.isEmpty())return;
        receipt=prepared.orElseThrow();tier=receipt.getInt("Tier");locked=true;valid=true;setChanged();
    }
    public int tier(){return tier;}
    boolean factoryInvestigated(){return receipt.hasUUID("FactoryInvestigator")&&receipt.contains("FactoryInvestigatedAt",Tag.TAG_LONG)&&receipt.getLong("FactoryInvestigatedAt")>=0;}
    void investigateFactory(net.minecraft.server.level.ServerPlayer player){if(factoryInvestigated()||locked()||!authentic()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>36||!receipt.getString("Family").equals(InfiniteStructurePlan.Family.CLOCKWORK_FACTORY.name()))return;var plan=InfiniteStructurePlan.at(receipt.getUUID("Generation"),receipt.getLong("Seed"),InfiniteStructurePlan.Family.CLOCKWORK_FACTORY,receipt.getInt("AnchorX"),receipt.getInt("AnchorZ")).orElse(null);int module=receipt.getInt("Module");if(plan==null||module<0||module>=plan.modules().size()||plan.modules().get(module).role()!=InfiniteStructurePlan.Role.CONTROL)return;receipt.putUUID("FactoryInvestigator",player.getUUID());receipt.putLong("FactoryInvestigatedAt",player.server.overworld().getGameTime());setChanged();PostgameQuests.factoryCompletedIfReady(player,plan);player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.factory_orders"));}
    private boolean objectiveReady(){return FactoryLayout.cacheReady(level,receipt)&&BoundaryDefenseLayout.cacheReady(level,receipt)&&ObservatoryLayout.cacheReady(level,receipt)&&PerformanceLayout.cacheReady(level,receipt)&&CarnivalLayout.cacheReady(level,receipt)&&OrphanageLayout.cacheReady(level,receipt)&&FalsePalaceLayout.cacheReady(level,receipt);}
    String blockedFeedback(){return receipt.getString("Family").equals(InfiniteStructurePlan.Family.CLOCKWORK_FACTORY.name())?"message.coraline.factory_archive_sealed":receipt.getString("Family").equals(InfiniteStructurePlan.Family.FALSE_PINK_PALACE.name())?"message.coraline.false_palace_sealed":receipt.getString("Family").equals(InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE.name())?"message.coraline.orphanage_reward_sealed":receipt.getString("Family").equals(InfiniteStructurePlan.Family.ENDLESS_CARNIVAL.name())?"message.coraline.carnival_reward_sealed":receipt.getString("Family").equals(InfiniteStructurePlan.Family.GRAND_THEATRE.name())?"message.coraline.performance_reward_sealed":receipt.getString("Family").equals(InfiniteStructurePlan.Family.FORGOTTEN_OBSERVATORY.name())?"message.coraline.observatory_reward_sealed":"message.coraline.boundary_reward_sealed";}
    @Override public boolean canPick(){return objectiveReady();}
    public boolean locked(){return level!=null&&level.isClientSide?locked:!valid||locked||level!=null&&(!authentic()||!objectiveReady());}
    public UUID site(){return valid?receipt.getUUID("Site"):null;}
    public boolean authentic(){
        if(!valid||isRemoved()||level==null||!level.hasChunkAt(worldPosition)||!getBlockState().is(com.coraline.registry.ModBlocks.INFINITE_SITE_CACHE.get())||level.getBlockEntity(worldPosition)!=this
            ||!(level.getChunkSource() instanceof net.minecraft.server.level.ServerChunkCache chunks)||!(chunks.getGenerator() instanceof InfiniteChunkGenerator generator))return false;
        try{var generation=generator.structureGeneration();return receipt.getUUID("Generation").equals(generation.id())&&receipt.getLong("Seed")==generation.seed();}
        catch(IllegalStateException unbound){return false;}
    }
    public boolean unlock(UUID site){
        if(!locked()||site==null||!site.equals(site())||!authentic()||!objectiveReady())return false;
        locked=false;receipt.putBoolean("Locked",false);setChanged();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);return true;
    }
    @Override protected void saveAdditional(CompoundTag out){super.saveAdditional(out);out.put(KEY,opaqueReceipt==null?receipt.copy():opaqueReceipt.copy());}
    @Override public void load(CompoundTag saved){
        super.load(saved);var raw=saved.get(KEY);opaqueReceipt=raw!=null&&!(raw instanceof CompoundTag)?raw.copy():null;receipt=saved.getCompound(KEY).copy();valid=false;locked=!receipt.contains("Locked",Tag.TAG_BYTE)||receipt.getBoolean("Locked");tier=Math.max(1,Math.min(4,receipt.getInt("Tier")));
        // Client update packets intentionally contain no generation/loot data.
        if(level!=null&&level.isClientSide){locked=receipt.getBoolean("Locked");return;}
        var parsed=SiteCacheReceipt.read(receipt,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ());
        if(parsed.isPresent()){valid=true;tier=parsed.orElseThrow().tier();locked=parsed.orElseThrow().locked();}
    }
    @Override public CompoundTag getUpdateTag(){var out=new CompoundTag();var state=new CompoundTag();state.putInt("Version",1);state.putInt("Tier",tier);state.putBoolean("Locked",locked());out.put(KEY,state);return out;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public boolean canOpen(net.minecraft.world.entity.player.Player player){return !locked()&&super.canOpen(player);}
    @Override public void unpackLootTable(net.minecraft.world.entity.player.Player player){if(!locked())super.unpackLootTable(player);}
    @Override public ItemStack getItem(int slot){return locked()?ItemStack.EMPTY:super.getItem(slot);}
    @Override public ItemStack removeItem(int slot,int count){return locked()?ItemStack.EMPTY:super.removeItem(slot,count);}
    @Override public ItemStack removeItemNoUpdate(int slot){return locked()?ItemStack.EMPTY:super.removeItemNoUpdate(slot);}
    @Override public void setItem(int slot,ItemStack stack){if(!locked())super.setItem(slot,stack);}
    @Override public void clearContent(){if(!locked())super.clearContent();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return !locked()&&super.canPlaceItem(slot,stack);}
    @Override public int[] getSlotsForFace(Direction side){if(locked())return new int[0];return java.util.stream.IntStream.range(0,getContainerSize()).toArray();}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return !locked();}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return !locked();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){return locked()&&capability==ForgeCapabilities.ITEM_HANDLER?LazyOptional.empty():super.getCapability(capability,side);}
}
