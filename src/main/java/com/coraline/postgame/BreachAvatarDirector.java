package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** At most one saved Avatar lease. Issuance/recovery is admitted only after
 * the leased native entity-storage chunk has actually loaded its entities. */
@Mod.EventBusSubscriber(modid="coraline")
public final class BreachAvatarDirector {
    private static final Map<MinecraftServer,Long> RETRY=new WeakHashMap<>();
    private static boolean space(ServerLevel level,Mob entity,BlockPos at){
        if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))return false;
        var box=entity.getBoundingBox().move(at.getX()+.5-entity.getX(),at.getY()-entity.getY(),at.getZ()+.5-entity.getZ());return level.noCollision(entity,box)&&!level.containsAnyLiquid(box);
    }
    private static boolean protectedBase(ServerLevel level,BlockPos at){
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            var chunk=level.getChunkSource().getChunkNow((at.getX()>>4)+dx,(at.getZ()>>4)+dz);if(chunk==null)continue;
            for(var pos:chunk.getBlockEntities().keySet())if(pos.distSqr(at)<32*32)return true;
        }
        var fake=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        return !level.mayInteract(fake,at.below())||net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,net.minecraft.world.level.GameType.SURVIVAL,fake,at.below())<0;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.getServer().getTickCount()%20!=0)return;var server=event.getServer();var data=BreachWorldData.get(server);var view=data.view();
        if(!view.active()||!view.avatarLocked()||!view.blocker().isEmpty())return;long now=server.overworld().getGameTime();if(now<RETRY.getOrDefault(server,Long.MIN_VALUE))return;var level=server.overworld();
        if(data.avatarId().isPresent()){
            var id=data.avatarId().orElseThrow();for(var world:server.getAllLevels())if(world.getEntity(id)!=null)return;
            var home=data.avatarHome().orElseThrow();if(!level.hasChunkAt(home)||!level.areEntitiesLoaded(ChunkPos.asLong(home.getX()>>4,home.getZ()>>4)))return;
            var restored=EntityType.loadEntityRecursive(data.avatarBody(),level,e->e);
            if(!(restored instanceof WhiteVoidAvatarEntity avatar)||!data.authentic(avatar)||!space(level,avatar,avatar.blockPosition())){RETRY.put(server,now+200);return;}
            if(!level.addFreshEntity(avatar))avatar.discard();RETRY.put(server,now+200);return;
        }
        var players=server.getPlayerList().getPlayers().stream().filter(p->p.serverLevel()==level&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()).toList();if(players.isEmpty())return;
        var target=players.get(Math.floorMod(server.getTickCount()/20,players.size()));
        for(int i=0;i<12;i++){
            double angle=(i+now/24000.)*Math.PI/6;int x=((int)Math.floor(target.getX()+Math.cos(angle)*72)>>4)*16+8,z=((int)Math.floor(target.getZ()+Math.sin(angle)*72)>>4)*16+8;
            var column=new BlockPos(x,64,z);if(!level.hasChunkAt(column)||!level.areEntitiesLoaded(ChunkPos.asLong(x>>4,z>>4)))continue;int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);var at=new BlockPos(x,y,z);
            if(players.stream().anyMatch(p->p.distanceToSqr(at.getCenter())<48*48)||protectedBase(level,at))continue;
            var avatar=com.coraline.registry.ModEntities.WHITE_VOID_AVATAR.get().create(level);if(avatar==null)return;avatar.configure(data.generation().orElseThrow(),UUID.randomUUID(),at);
            if(!space(level,avatar,at)){avatar.discard();continue;}
            if(!data.admitAvatar(avatar)){avatar.discard();return;}
            if(!level.addFreshEntity(avatar))avatar.discard();RETRY.put(server,now+200);
            for(var player:players)player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.avatar_appeared",x,z));return;
        }
        RETRY.put(server,now+200);
    }
    static void summon(WhiteVoidAvatarEntity avatar,int count){
        if(!(avatar.level() instanceof ServerLevel level)||!avatar.authentic())return;var mobs=new ArrayList<net.minecraft.world.entity.monster.Husk>();
        for(int i=0;i<count;i++){
            double angle=(avatar.tickCount+i*8)*.6;int x=avatar.home().getX()+(int)Math.round(Math.cos(angle)*4),z=avatar.home().getZ()+(int)Math.round(Math.sin(angle)*4);var at=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
            var mob=EntityType.HUSK.create(level);if(mob==null||!space(level,mob,at))continue;mob.moveTo(x+.5,at.getY(),z+.5,0,0);mobs.add(mob);
        }
        if(mobs.isEmpty()||!BreachWorldData.get(level.getServer()).admitSummons(avatar,mobs.stream().map(Entity::getUUID).toList()))return;
        for(var mob:mobs){if(!InfectedSummons.configure(mob,avatar)||!level.addFreshEntity(mob)){BreachWorldData.get(level.getServer()).releaseSummon(mob.getUUID(),avatar.getUUID());mob.discard();}}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(event.isCanceled()||!(event.getEntity() instanceof WhiteVoidAvatarEntity avatar)||!(avatar.level() instanceof ServerLevel level))return;var data=BreachWorldData.get(level.getServer());
        if(!data.authentic(avatar)||!data.avatarDefeated(avatar)){avatar.setHealth(1);event.setCanceled(true);return;}
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,avatar.getX(),avatar.getY()+2,avatar.getZ(),100,2,3,2,.06);
        for(var player:level.getServer().getPlayerList().getPlayers())player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.avatar_defeated"));
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){RETRY.remove(event.getServer());}
    private BreachAvatarDirector(){}
}
