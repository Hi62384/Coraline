package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Physical dial alignment; loading and later refuelling never resolve credit. */
public final class SeamAnchorEntity extends BlockEntity {
    private Tag raw=new CompoundTag();private InfiniteStructurePlan.Plan plan;private int slot=-1;
    public SeamAnchorEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.SEAM_ANCHOR.get(),at,state);}
    void configure(InfiniteStructurePlan.Plan plan,int slot){if(this.plan!=null||!SeamLayout.family(plan.family())||!(raw instanceof CompoundTag n)||!n.isEmpty()||slot<0||slot>3||!SeamLayout.parts(plan).get(slot).equals(worldPosition))return;this.plan=plan;this.slot=slot;raw=SeamLayout.metadata(plan,slot);setChanged();}
    private CompoundTag data(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    private boolean self(){
        if(plan==null||slot<0||slot>3||isRemoved()||!(level instanceof ServerLevel server)||!server.hasChunkAt(worldPosition)||server.getBlockEntity(worldPosition)!=this||!getBlockState().is((slot==0?com.coraline.registry.ModBlocks.SEAM_CORE:com.coraline.registry.ModBlocks.SEAM_DIAL).get())||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;
        var n=data();if(!n.contains("Rotation",Tag.TAG_INT)||n.getInt("Rotation")<0||n.getInt("Rotation")>3)return false;
        if(slot==0){if(!n.contains("Closed",Tag.TAG_BYTE)||n.getByte("Closed")<0||n.getByte("Closed")>1)return false;
            if(n.getBoolean("Closed")){if(!n.hasUUID("Completion")||!n.contains("Outcome",Tag.TAG_STRING))return false;try{BreachState.Resolution.valueOf(n.getString("Outcome"));}catch(IllegalArgumentException bad){return false;}}
            else if(n.contains("Completion")||n.contains("Outcome"))return false;
        }
        try{var epoch=generator.structureGeneration();return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed()&&InfiniteWorldData.get(server.getServer()).matchesGeneration(epoch.id());}catch(IllegalStateException unavailable){return false;}
    }
    private SeamAnchorEntity root(){if(!self())return null;var at=SeamLayout.parts(plan).get(0);if(!level.hasChunkAt(at))return null;return level.getBlockEntity(at) instanceof SeamAnchorEntity root&&root.slot==0&&root.self()&&root.plan.site().equals(plan.site())?root:null;}
    boolean authentic(){return root()!=null;}
    UUID site(){return plan==null?null:plan.site();}
    UUID completion(){return data().hasUUID("Completion")?data().getUUID("Completion"):null;}
    boolean closed(){return slot==0&&data().getBoolean("Closed");}
    void interact(ServerPlayer player){
        if(player.isCreative()||player.isSpectator()||!player.isAlive()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>36||!level.mayInteract(player,worldPosition)||!InfiniteWorldData.get(player.server).accessible())return;
        var core=root();if(core==null){say(player,"seam_unavailable");return;}if(core.closed()){say(player,plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE?"false_palace_opened":"seam_closed");return;}
        if(slot>0){data().putInt("Rotation",(data().getInt("Rotation")+1)&3);setChanged();core.sync();level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_STEP,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1+data().getInt("Rotation")*.12F);return;}
        var parts=SeamLayout.parts(plan);for(int i=1;i<parts.size();i++){
            var at=parts.get(i);if(!level.hasChunkAt(at)||!(level.getBlockEntity(at) instanceof SeamAnchorEntity node)||!node.self()||node.slot!=i||!node.plan.site().equals(plan.site())){say(player,"seam_unavailable");return;}
            if(node.data().getInt("Rotation")!=SeamLayout.expected(plan,i)){say(player,"seam_alignment");return;}
        }
        if(plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE){data().putBoolean("Closed",true);data().putUUID("Completion",UUID.randomUUID());data().putString("Outcome",BreachState.Resolution.ZERO.name());setChanged();sync();FalsePalaceLayout.release(level,plan);say(player,"false_palace_opened");return;}
        var breach=BreachWorldData.get(player.server).view();if(!breach.active()||!breach.blocker().isEmpty()){say(player,"seam_unavailable");return;}
        data().putBoolean("Closed",true);data().putUUID("Completion",UUID.randomUUID());
        // Temporary REFUSED keeps the record structurally valid while the
        // physical completion is attested. This method is the only resolver.
        data().putString("Outcome",BreachState.Resolution.REFUSED.name());
        var outcome=BreachWorldData.get(player.server).resolveSeam((ServerLevel)level,this);data().putString("Outcome",outcome.name());setChanged();sync();PostgameQuests.completed(player,plan);
        ((ServerLevel)level).sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,45,1,1,1,.04);
        say(player,outcome==BreachState.Resolution.REDUCED?"seam_contained":"seam_closed_no_credit");
    }
    private static void say(ServerPlayer p,String message){p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline."+message),true);}
    private void sync(){if(slot!=0||!self())return;for(var at:SeamLayout.parts(plan))if(level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof SeamAnchorEntity node&&node.self()&&node.plan.site().equals(plan.site())){var state=node.getBlockState();var next=state.setValue(SeamAnchorBlock.ROTATION,node.data().getInt("Rotation")).setValue(SeamAnchorBlock.ACTIVE,!closed());if(!state.equals(next))level.setBlock(at,next,3);}}
    static void tick(net.minecraft.world.level.Level level,BlockPos at,BlockState state,SeamAnchorEntity entity){if(level.getGameTime()%20==0&&entity.slot==0){entity.sync();if(entity.authentic()&&entity.closed()&&entity.plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE)FalsePalaceLayout.release(level,entity.plan);}}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineSeam",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralineSeam")?n.get("CoralineSeam").copy():new CompoundTag();plan=null;slot=-1;if(raw instanceof CompoundTag tag)SeamLayout.read(tag,worldPosition).ifPresent(decoded->{plan=decoded.plan();slot=decoded.slot();});}
}
