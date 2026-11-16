package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A real finite assembly batch; disabling three clutches physically stops
 * future products. Merely reading a journal or killing mobs cannot stop it. */
public final class FactoryControlEntity extends BlockEntity {
    private Tag raw=new CompoundTag();private InfiniteStructurePlan.Plan plan;private int slot;
    public FactoryControlEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.FACTORY_CONTROL.get(),at,state);}
    private CompoundTag data(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    void configure(InfiniteStructurePlan.Plan plan,int slot){if(this.plan!=null||!(raw instanceof CompoundTag n)||!n.isEmpty()||!FactoryLayout.part(plan,slot).equals(worldPosition))return;this.plan=plan;this.slot=slot;raw=FactoryLayout.metadata(plan,slot);setChanged();}
    UUID site(){return plan.site();}
    boolean authentic(){if(plan==null||isRemoved()||!(level instanceof ServerLevel server)||!server.hasChunkAt(worldPosition)||server.getBlockEntity(worldPosition)!=this||!getBlockState().is(com.coraline.registry.ModBlocks.FACTORY_CONTROL.get())||getBlockState().getValue(FactoryControlBlock.PART)!=slot||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;try{var bound=generator.structureGeneration();return bound.id().equals(plan.generation())&&bound.seed()==plan.seed()&&InfiniteWorldData.get(server.getServer()).matchesGeneration(bound.id());}catch(IllegalStateException unbound){return false;}}
    boolean complete(){return authentic()&&FactoryBatchData.get(level.getServer(),plan).stopped(plan.site());}
    void operate(ServerPlayer player){
        if(!authentic()||!player.isAlive()||player.isCreative()||player.isSpectator()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>25||!level.mayInteract(player,worldPosition)||TrueHeartBattle.recoveryPending(player)||!InfiniteWorldData.get(player.server).accessible())return;
        var core=FactoryLayout.core(level,plan);if(core==null)return;var batch=FactoryBatchData.get(player.server,plan);if(!batch.ready()){tell(player,"factory_save_paused");return;}if(core.complete()){tell(player,"factory_stopped");return;}
        if(slot==0){if(core.data().getInt("Clutches")!=7){tell(player,"factory_instructions");return;}if(batch.stop(player.server,plan,player.getUUID())){core.sync();level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.PISTON_CONTRACT,net.minecraft.sounds.SoundSource.BLOCKS,1,.6F);tell(player,"factory_stopped");PostgameQuests.factoryCompletedIfReady(player,plan);}else tell(player,"factory_save_paused");return;}
        int mask=core.data().getInt("Clutches");if((mask&(1<<(slot-1)))!=0){tell(player,"factory_clutch_off");return;}int expected=Integer.bitCount(mask)+1;if(slot!=expected){tell(player,"factory_order");return;}
        core.data().putInt("Clutches",mask|(1<<(slot-1)));core.setChanged();core.sync();level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.LEVER_CLICK,net.minecraft.sounds.SoundSource.BLOCKS,1,.8F);tell(player,slot==3?"factory_return_press":"factory_clutch_off");
    }
    private static void tell(ServerPlayer p,String key){p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline."+key));}
    private void sync(){if(!authentic())return;boolean stopped=complete();int mask=data().getInt("Clutches");for(int i=0;i<4;i++){var at=FactoryLayout.part(plan,i);if(!level.hasChunkAt(at)||!(level.getBlockEntity(at) instanceof FactoryControlEntity part)||!part.authentic()||!part.site().equals(plan.site()))continue;boolean active=!stopped&&(i==0||(mask&(1<<(i-1)))==0);var state=part.getBlockState();if(state.getValue(FactoryControlBlock.ACTIVE)!=active)level.setBlock(at,state.setValue(FactoryControlBlock.ACTIVE,active),3);}}
    static void tick(Level level,BlockPos at,BlockState state,FactoryControlEntity core){
        if(level.getGameTime()%20!=0||!core.authentic()||TrueHeartBattle.resetting(level.getServer()))return;core.sync();if(state.getValue(FactoryControlBlock.PULSE)&&level.getGameTime()>=core.data().getLong("PulseUntil"))level.setBlock(at,core.getBlockState().setValue(FactoryControlBlock.PULSE,false),3);if(core.complete())return;
        var server=(ServerLevel)level;boolean nearby=server.players().stream().anyMatch(p->p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&p.distanceToSqr(at.getCenter())<=1024&&!TrueHeartBattle.recoveryPending(p));if(!nearby)return;
        if(core.data().getLong("NextBuild")>level.getGameTime())return;core.data().putLong("NextBuild",level.getGameTime()+100);core.setChanged();var batch=FactoryBatchData.get(server.getServer(),core.plan);if(!batch.ready())return;int index=batch.issued(core.site());if(index>=3)return;
        var feet=at.east(3);if(!level.hasChunkAt(feet))return;var worker=PostgameHostiles.TYPES.get(PostgameHostiles.Kind.BUTTON_EYED_WANDERER).get().create(level);if(worker==null)return;worker.setUUID(FactoryBatchData.worker(core.site(),index));worker.moveTo(feet.getX()+.5,feet.getY(),feet.getZ()+.5,90,0);if(!level.noCollision(worker,worker.getBoundingBox())||level.containsAnyLiquid(worker.getBoundingBox())||level.getBlockState(feet.below()).getCollisionShape(level,feet.below()).isEmpty())return;
        if(!batch.issue(server.getServer(),core.plan,index))return;worker.setPersistenceRequired();worker.restrictTo(at,24);if(level.addFreshEntity(worker)){core.data().putLong("PulseUntil",level.getGameTime()+20);core.setChanged();level.setBlock(at,core.getBlockState().setValue(FactoryControlBlock.PULSE,true),3);server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,feet.getX()+.5,feet.getY()+1,feet.getZ()+.5,20,.4,.6,.4,.02);level.playSound(null,at,net.minecraft.sounds.SoundEvents.PISTON_EXTEND,net.minecraft.sounds.SoundSource.BLOCKS,.8F,.7F);}
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("CoralineFactory",raw.copy());}
    @Override public void load(CompoundTag tag){super.load(tag);raw=tag.contains("CoralineFactory")?tag.get("CoralineFactory").copy():new CompoundTag();plan=null;if(raw instanceof CompoundTag n)FactoryLayout.read(n,worldPosition).ifPresent(p->{plan=p.plan();slot=p.slot();});}
}
