package com.coraline.parents;
import com.coraline.config.CoralineConfig;
import com.coraline.registry.ModEntities;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline")
public final class ParentEvents {
    public static BlockPos safe(ServerLevel level,ParentEntity p,BlockPos near,int radius){
        for(int r=0;r<=radius;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
            for(int y=2;y>=-3;y--){BlockPos b=near.offset(x,y,z);
                if(!level.hasChunkAt(b)||!level.getWorldBorder().isWithinBounds(b))continue;
                var box=p.getDimensions(p.getPose()).makeBoundingBox(b.getX()+.5,b.getY(),b.getZ()+.5);
                if(level.getBlockState(b.below()).isFaceSturdy(level,b.below(),Direction.UP)&&level.noCollision(p,box)&&!level.containsAnyLiquid(box))return b;
            }
        }return null;
    }
    public static boolean spawnPair(ServerLevel level,BlockPos near){
        return spawnPair(level,near,null);
    }
    public static boolean spawnClaimedPair(ServerPlayer owner,BlockPos near){
        if(ParentAccess.family(owner.server,owner.getUUID())!=null)return false;
        return spawnPair(owner.serverLevel(),near,owner.getUUID());
    }
    private static boolean spawnPair(ServerLevel level,BlockPos near,java.util.UUID owner){
        ParentEntity mom=ModEntities.MOM.get().create(level),dad=ModEntities.DAD.get().create(level);
        if(mom==null||dad==null)return false;
        BlockPos a=safe(level,mom,near,4);if(a==null)return false;
        mom.setPos(a.getX()+.5,a.getY(),a.getZ()+.5);
        BlockPos b=safe(level,dad,a.offset(2,0,0),4);if(b==null||a.equals(b))return false;
        dad.setPos(b.getX()+.5,b.getY(),b.getZ()+.5);
        FamilyData data=FamilyData.get(level.getServer());FamilyData.Family f=new FamilyData.Family();
        f.owner=owner;
        f.mom=mom.getUUID();f.dad=dad.getUUID();f.momPos=a;f.dadPos=b;f.momDim=f.dadDim=level.dimension().location().toString();
        mom.familyId=dad.familyId=f.id;mom.randomizeSkin();dad.randomizeSkin();
        if(owner!=null){mom.markClaimedForSetup();dad.markClaimedForSetup();}
        f.momSkin=mom.skin();f.dadSkin=dad.skin();
        for(ParentEntity p:new ParentEntity[]{mom,dad}){p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(CoralineConfig.HEALTH.get());p.setHealth(p.getMaxHealth());}
        data.families.put(f.id,f);
        if(!level.addFreshEntity(mom)||!level.addFreshEntity(dad)){mom.discard();dad.discard();data.families.remove(f.id);return false;}
        data.setDirty();return true;
    }
    public static void transferNear(ParentEntity p,ServerLevel target,BlockPos near){
        BlockPos safe=safe(target,p,near,5);if(safe==null)return;
        if(p.level()!=target){
            p.changeDimension(target,new net.minecraftforge.common.util.ITeleporter(){
                @Override public net.minecraft.world.entity.Entity placeEntity(net.minecraft.world.entity.Entity entity,ServerLevel current,ServerLevel dest,float yaw,java.util.function.Function<Boolean,net.minecraft.world.entity.Entity> reposition){
                    var moved=reposition.apply(false);moved.moveTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5,yaw,0);return moved;
                }
            });
        }else{p.getNavigation().stop();p.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent e){
        if(!e.getLevel().isClientSide && e.getEntity() instanceof Monster monster){
            monster.targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(monster,ParentEntity.class,true));
        }
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        ServerLevel l=p.serverLevel();
        if(com.coraline.otherworld.OtherWorld.isOther(l))return;
        if(p.tickCount%40==0){
            var data=FamilyData.get(p.server);
            for(var f:data.families.values())if(!f.dead&&p.getUUID().equals(f.owner)&&f.bed==null){
                // Load only each owned parent's last known chunk so following resumes across unloading/dimensions.
                for(boolean mom:new boolean[]{true,false}){
                    ServerLevel old=FamilyData.level(p.server,mom?f.momDim:f.dadDim);if(old==null)continue;
                    var id=mom?f.mom:f.dad;var entity=old.getEntity(id);
                    if(entity==null)old.getChunkAt(mom?f.momPos:f.dadPos);
                    else if(entity instanceof ParentEntity parent && (old!=l||parent.distanceToSqr(p)>Math.pow(CoralineConfig.TELEPORT_DISTANCE.get(),2)))transferNear(parent,l,p.blockPosition());
                }
            }
        }
        if(!CoralineConfig.PARENTS_SPAWN.get()||l.dimension()!=Level.OVERWORLD||p.isSpectator()||p.tickCount%CoralineConfig.SPAWN_INTERVAL.get()!=0)return;
        if(l.random.nextDouble()>=CoralineConfig.SPAWN_CHANCE.get())return;
        if(l.getEntitiesOfClass(ParentEntity.class,p.getBoundingBox().inflate(96)).size()>=2*CoralineConfig.MAX_NEARBY.get())return;
        double angle=l.random.nextDouble()*Math.PI*2;int min=CoralineConfig.SPAWN_MIN.get(),max=Math.max(min,CoralineConfig.SPAWN_MAX.get());
        double distance=min+l.random.nextInt(max-min+1);int x=(int)Math.floor(p.getX()+Math.cos(angle)*distance),z=(int)Math.floor(p.getZ()+Math.sin(angle)*distance);
        if(!l.hasChunkAt(new BlockPos(x,0,z)))return;
        BlockPos pos=l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));
        if(l.getMaxLocalRawBrightness(pos)<8)return;
        spawnPair(l,pos);
    }
}
