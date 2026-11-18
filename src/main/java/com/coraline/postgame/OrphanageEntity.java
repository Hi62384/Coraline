package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Saved evidence and a single actual release. Unknown future data is opaque. */
public final class OrphanageEntity extends BlockEntity {
    private Tag raw=new CompoundTag();private InfiniteStructurePlan.Plan plan;
    public OrphanageEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.ORPHANAGE.get(),at,state);}
    private CompoundTag data(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    void configure(InfiniteStructurePlan.Plan p,int slot){if(plan!=null||!(raw instanceof CompoundTag n)||!n.isEmpty()||!OrphanageLayout.register(p,slot).equals(worldPosition))return;plan=p;raw=OrphanageLayout.metadata(p,slot);setChanged();}
    private boolean authentic(){
        var n=data();if(plan==null||isRemoved()||!(level instanceof ServerLevel s)||s.getBlockEntity(worldPosition)!=this||!(s.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator)||!getBlockState().is(com.coraline.registry.ModBlocks.ORPHANAGE_REGISTER.get())||!n.contains("Slot",Tag.TAG_INT)||n.getInt("Slot")<0||n.getInt("Slot")>3||getBlockState().getValue(OrphanageBlock.SLOT)!=n.getInt("Slot")||!OrphanageLayout.register(plan,n.getInt("Slot")).equals(worldPosition)||!n.contains("Evidence",Tag.TAG_INT)||n.getInt("Evidence")<0||n.getInt("Evidence")>7||!n.contains("Released",Tag.TAG_BYTE)||n.getByte("Released")<0||n.getByte("Released")>1||n.getBoolean("Released")&&n.getInt("Evidence")!=7)return false;
        try{var epoch=generator.structureGeneration();return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed()&&InfiniteWorldData.get(s.getServer()).matchesGeneration(epoch.id());}catch(IllegalStateException unavailable){return false;}
    }
    boolean questAvailable(){return authentic();}
    boolean complete(){return authentic()&&data().getInt("Slot")==3&&data().getBoolean("Released");}
    boolean bindingsCleared(){return authentic()&&data().getInt("Slot")==3&&(data().getBoolean("BindingsCleared")||complete());}
    void read(ServerPlayer p){
        if(!authentic()||!p.isAlive()||p.isCreative()||p.isSpectator()||p.level()!=level||p.distanceToSqr(worldPosition.getCenter())>36||!level.mayInteract(p,worldPosition)||!InfiniteWorldData.get(p.server).accessible())return;
        var at=OrphanageLayout.register(plan,3);if(!level.hasChunkAt(at)||!(level.getBlockEntity(at) instanceof OrphanageEntity root)||!root.authentic()||!root.plan.site().equals(plan.site()))return;int slot=data().getInt("Slot");
        if(slot<3){root.data().putInt("Evidence",root.data().getInt("Evidence")|1<<slot);root.setChanged();p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.orphanage_record_"+slot));return;}
        if(root.data().getInt("Evidence")!=7){p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.orphanage_missing"));return;}
        if(!root.data().hasUUID("Investigator")){
            root.data().putUUID("Investigator",p.getUUID());
            root.setChanged();
        }
        root.release();
        if(root.complete())p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.orphanage_released"));
    }
    private void release(){
        if(!authentic()||data().getInt("Slot")!=3||data().getInt("Evidence")!=7||!data().hasUUID("Investigator"))return;
        boolean bindingsClear=true;
        var binding=com.coraline.registry.ModBlocks.ORPHANAGE_BINDING.get();
        for(var at:OrphanageLayout.bars(plan)){
            if(!level.hasChunkAt(at)){
                bindingsClear=false;
                continue;
            }
            if(level.getBlockState(at).is(binding))level.removeBlock(at,false);
            if(level.getBlockState(at).is(binding))bindingsClear=false;
        }
        if(!bindingsClear)return;
        if(!data().getBoolean("BindingsCleared")){
            data().putBoolean("BindingsCleared",true);
            setChanged();
        }
        boolean vaultsOpen=true;
        for(int i=0;i<plan.modules().size();i++)if(plan.modules().get(i).role()==InfiniteStructurePlan.Role.ORPHAN_VAULT){
            var at=SiteCacheReceipt.position(plan,i);
            if(!level.hasChunkAt(at)||!(level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache)||!plan.site().equals(cache.site())||!cache.authentic()){
                vaultsOpen=false;
                continue;
            }
            cache.unlock(plan.site());
            if(cache.locked())vaultsOpen=false;
        }
        if(!vaultsOpen)return;
        if(!complete()){
            data().putBoolean("Released",true);
            data().putBoolean("CreditPending",true);
            setChanged();
        }
        if(data().getBoolean("CreditPending")&&level instanceof ServerLevel serverLevel){
            var investigator=serverLevel.getServer().getPlayerList().getPlayer(data().getUUID("Investigator"));
            if(investigator!=null&&investigator.level()==level&&PostgameQuests.eligible(investigator)){
                PostgameQuests.completed(investigator,plan);
                data().remove("CreditPending");
                setChanged();
            }
        }
    }
    static void tick(Level l,BlockPos at,BlockState state,OrphanageEntity root){
        if(l instanceof ServerLevel&&l.getGameTime()%20==0&&root.data().hasUUID("Investigator"))root.release();
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineOrphanage",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralineOrphanage")?n.get("CoralineOrphanage").copy():new CompoundTag();plan=null;if(raw instanceof CompoundTag tag)OrphanageLayout.read(tag).ifPresent(p->plan=p);}
}
