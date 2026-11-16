package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The central physical drill owns security, conduits and permanent shutdown.
 * No chunk-load callback awards containment or resets disabled machinery. */
public final class RiftMachineEntity extends BlockEntity implements SiteLockTarget {
    private Tag raw=new CompoundTag();
    private InfiniteStructurePlan.Plan plan;
    private int slot=-1;
    public RiftMachineEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.RIFT_MACHINE.get(),at,state);}
    void configure(InfiniteStructurePlan.Plan plan,int slot){
        if(!(raw instanceof CompoundTag n)||!n.isEmpty()||plan.family()!=InfiniteStructurePlan.Family.RIFT_DRILL_CENTER)return;
        var parts=RiftMachineLayout.parts(plan);if(slot<0||slot>=parts.size()||!parts.get(slot).at().equals(worldPosition))return;
        this.plan=plan;this.slot=slot;raw=RiftMachineLayout.metadata(plan,slot);
        if(slot==0){var progress=new CompoundTag();progress.putInt("Version",1);progress.putBoolean("SecurityOpen",false);progress.putInt("Conduits",0);progress.putBoolean("Disabled",false);((CompoundTag)raw).put("Progress",progress);}setChanged();
    }
    private CompoundTag progress(){return raw instanceof CompoundTag n?n.getCompound("Progress"):new CompoundTag();}
    private boolean validProgress(){
        var n=progress();if(!(raw instanceof CompoundTag root)||!root.contains("Progress",Tag.TAG_COMPOUND)||!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.contains("Conduits",Tag.TAG_INT)||n.getInt("Conduits")<0||n.getInt("Conduits")>3)return false;
        for(String key:List.of("SecurityOpen","Disabled"))if(!n.contains(key,Tag.TAG_BYTE)||n.getByte(key)<0||n.getByte(key)>1)return false;
        if(n.getInt("Conduits")!=0&&!n.getBoolean("SecurityOpen"))return false;
        return n.getBoolean("Disabled")?n.getBoolean("SecurityOpen")&&n.getInt("Conduits")==3&&n.hasUUID("Completion")&&n.contains("CompletedAt",Tag.TAG_LONG)&&n.getLong("CompletedAt")>=0:!n.contains("Completion")&&!n.contains("CompletedAt")&&!n.contains("ContainmentOutcome");
    }
    public boolean isCore(){return slot==0;}
    public boolean disabled(){return isCore()&&validProgress()&&progress().getBoolean("Disabled");}
    UUID completion(){return disabled()?progress().getUUID("Completion"):null;}
    String containmentOutcome(){return disabled()?progress().getString("ContainmentOutcome"):"";}
    private RiftMachineLayout.Kind kind(){return plan==null||slot<0?null:RiftMachineLayout.parts(plan).get(slot).kind();}
    private boolean rightBlock(){var kind=kind();return kind!=null&&getBlockState().is(RiftMachineBlock.block(kind));}
    private RiftMachineEntity root(){
        if(plan==null||level==null)return null;var at=RiftMachineLayout.parts(plan).get(0).at();if(!level.hasChunkAt(at))return null;
        return level.getBlockEntity(at) instanceof RiftMachineEntity core&&core.isCore()&&core.rightBlock()&&core.plan!=null&&core.plan.site().equals(plan.site())&&core.plan.generation().equals(plan.generation())&&core.validProgress()?core:null;
    }
    @Override public boolean authentic(){
        if(plan==null||isRemoved()||!(level instanceof ServerLevel server)||!server.hasChunkAt(worldPosition)||server.getBlockEntity(worldPosition)!=this||!rightBlock()||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;
        try{var generation=generator.structureGeneration();return generation.id().equals(plan.generation())&&generation.seed()==plan.seed()&&root()!=null;}
        catch(IllegalStateException unavailable){return false;}
    }
    @Override public UUID site(){return plan==null?null:plan.site();}
    @Override public int tier(){return 3;}
    @Override public String unlockFeedback(){return "message.coraline.rift_security_open";}
    @Override public boolean locked(){
        if(level!=null&&level.isClientSide)return getBlockState().is(com.coraline.registry.ModBlocks.RIFT_SECURITY_GATE.get())&&!getBlockState().getValue(RiftMachineBlock.OPEN);
        return kind()==RiftMachineLayout.Kind.GATE&&(!authentic()||!root().progress().getBoolean("SecurityOpen"));
    }
    @Override public boolean unlock(UUID site){
        if(kind()!=RiftMachineLayout.Kind.GATE||!authentic()||site==null||!site.equals(site())||!locked())return false;
        var core=root();core.progress().putBoolean("SecurityOpen",true);core.setChanged();core.syncLoaded();
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN,net.minecraft.sounds.SoundSource.BLOCKS,.7F,.8F);return true;
    }
    int sabotageCost(){return isCore()?2:1;}
    boolean sabotage(){return sabotage(null);}
    boolean sabotage(net.minecraft.server.level.ServerPlayer player){
        if(!authentic()||kind()==RiftMachineLayout.Kind.GATE)return false;var core=root();var progress=core.progress();
        if(!progress.getBoolean("SecurityOpen")||progress.getBoolean("Disabled"))return false;
        if(isCore()){
            if(progress.getInt("Conduits")!=3)return false;
            progress.putBoolean("Disabled",true);progress.putUUID("Completion",UUID.randomUUID());progress.putLong("CompletedAt",level.getGameTime());
            // Resolve only at this real successful operation, never on load or
            // later refueling. The global journal also rejects site replays.
            var outcome=BreachWorldData.get(level.getServer()).resolveDrill((ServerLevel)level,core);progress.putString("ContainmentOutcome",outcome.name());PostgameQuests.completed(player,plan);
        }else{
            int bit=slot==1?1:slot==2?2:0;if(bit==0||(progress.getInt("Conduits")&bit)!=0)return false;progress.putInt("Conduits",progress.getInt("Conduits")|bit);
        }
        core.setChanged();core.syncLoaded();return true;
    }
    private void syncLoaded(){
        if(!isCore()||!validProgress()||!(level instanceof ServerLevel server))return;var progress=progress();
        for(var part:RiftMachineLayout.parts(plan))if(server.hasChunkAt(part.at())&&server.getBlockEntity(part.at()) instanceof RiftMachineEntity member&&member.plan!=null&&member.plan.site().equals(plan.site())&&member.rightBlock()){
            var state=member.getBlockState();boolean open=part.kind()==RiftMachineLayout.Kind.GATE&&progress.getBoolean("SecurityOpen");
            boolean active=!progress.getBoolean("Disabled")&&(part.kind()!=RiftMachineLayout.Kind.CONDUIT||(progress.getInt("Conduits")&(member.slot==1?1:2))==0);
            var next=state.setValue(RiftMachineBlock.OPEN,open).setValue(RiftMachineBlock.ACTIVE,active);if(!next.equals(state))server.setBlock(part.at(),next,3);
        }
    }
    static void tick(net.minecraft.world.level.Level level,BlockPos at,BlockState state,RiftMachineEntity entity){if(level.getGameTime()%20==0&&entity.authentic()&&entity.isCore())entity.syncLoaded();}
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide&&authentic()){var core=root();if(core!=null)core.syncLoaded();}}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineRiftMachine",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralineRiftMachine")?n.get("CoralineRiftMachine").copy():new CompoundTag();plan=null;slot=-1;if(raw instanceof CompoundTag metadata)RiftMachineLayout.read(metadata,worldPosition).ifPresent(decoded->{plan=decoded.plan();slot=decoded.slot();});}
    @Override public AABB getRenderBoundingBox(){return getBlockState().is(com.coraline.registry.ModBlocks.RIFT_DRILL_CORE.get())?new AABB(worldPosition).inflate(2,0,2).expandTowards(0,7,0):new AABB(worldPosition);}
}
