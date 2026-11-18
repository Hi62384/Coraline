package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Two finite Overworld raids at most, eight bodies per raid, one admission
 * per player per game day. No raid kill grants global containment credit. */
@Mod.EventBusSubscriber(modid="coraline")
public final class BreachRaids {
    private static final String KEY="CoralineBreachInvader";
    private static CompoundTag marker(Entity actor){if(!(actor instanceof OtherWorldHostile)||!actor.getPersistentData().contains(KEY,Tag.TAG_COMPOUND))return null;var n=actor.getPersistentData().getCompound(KEY);return n.contains("Version",Tag.TAG_INT)&&n.getInt("Version")==1&&n.hasUUID("Generation")&&n.hasUUID("Raid")?n:null;}
    public static boolean active(Entity actor){if(actor.getServer()!=null&&TrueHeartBattle.resetting(actor.getServer()))return false;var n=marker(actor);return n!=null&&actor.level() instanceof ServerLevel level&&level==level.getServer().overworld()&&InfiniteWorldData.get(level.getServer()).matchesGeneration(n.getUUID("Generation"))&&BreachWorldData.get(level.getServer()).view().millionths()>=1000000&&BreachRaidData.get(level.getServer(),n.getUUID("Generation")).member(n.getUUID("Raid"),actor.getUUID());}
    private static boolean clear(ServerLevel level,BlockPos at,Mob actor){
        if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!level.canSeeSky(at)||!level.noCollision(actor)||!level.getFluidState(at).isEmpty())return false;var floor=level.getBlockState(at.below());if(!(floor.is(Blocks.GRASS_BLOCK)||floor.is(Blocks.DIRT)||floor.is(Blocks.STONE)||floor.is(Blocks.SAND)||floor.is(Blocks.GRAVEL)))return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=level.getChunkSource().getChunkNow((at.getX()>>4)+x,(at.getZ()>>4)+z);if(chunk==null)return false;for(var pos:chunk.getBlockEntitiesPos())if(pos.distSqr(at)<32*32)return false;}
        var sites=com.coraline.palace.PalaceSiteData.get(level.getServer());for(var kind:com.coraline.palace.PalaceSitePlan.Kind.values()){var site=sites.position(kind);if(site.isPresent()&&site.get().distSqr(at)<(kind==com.coraline.palace.PalaceSitePlan.Kind.PALACE?400*400:96*96))return false;}return true;
    }
    private static void spawn(ServerPlayer p,BreachRaidData data,int severity,long now,InfectionColonies.Source source){var level=p.serverLevel();var mobs=new ArrayList<OtherWorldHostile>();int count=severity>=1500000?8:4;PostgameHostiles.Kind[] choices={PostgameHostiles.Kind.RIFT_SOLDIER,PostgameHostiles.Kind.STITCHED_HOUND,PostgameHostiles.Kind.BUTTON_EYED_WANDERER,PostgameHostiles.Kind.THREADCRAWLER};double angle=level.random.nextDouble()*Math.PI*2;int radius=source==null?56:18;var origin=source==null?p.blockPosition():source.heart();var center=origin.offset((int)(Math.cos(angle)*radius),0,(int)(Math.sin(angle)*radius));if(!level.hasChunkAt(center))return;
        for(int i=0;i<count;i++){var kind=severity>=1500000&&i==count-1?PostgameHostiles.Kind.RIFT_GUARDIAN:choices[i%choices.length];var actor=PostgameHostiles.TYPES.get(kind).get().create(level);if(actor==null)return;var column=center.offset((i%4)*3,0,(i/4)*3);if(!level.hasChunkAt(column))return;var at=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column);actor.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);if(!clear(level,at,actor)||source!=null&&level.players().stream().anyMatch(player->player.distanceToSqr(at.getCenter())<256))return;mobs.add(actor);}
        var raid=UUID.randomUUID();if(!data.admit(p.server,raid,p.getUUID(),mobs,now,source==null?null:source.id(),severity>=1500000?600:1200))return;for(var mob:mobs){var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Generation",data.generation);n.putUUID("Raid",raid);mob.getPersistentData().put(KEY,n);mob.setPersistenceRequired();mob.setTarget(p);if(!level.addFreshEntity(mob)){
                for(var admitted:mobs)if(level.getEntity(admitted.getUUID())==admitted)admitted.discard();
                data.finish(raid);
                return;
            }
        }
        if(!p.server.saveEverything(true,true,true)||!data.activate(p.server,raid,now,severity>=1500000?600:1200)){
            for(var admitted:mobs)admitted.discard();
            data.finish(raid);
            return;
        }
        if(source==null&&level.random.nextInt(severity>=1500000?2:4)==0)InfectionColonies.establish(level,mobs.get(0).blockPosition().west(2),raid);p.sendSystemMessage(net.minecraft.network.chat.Component.translatable(source==null?"message.coraline.breach_raid":"message.coraline.breach_reinforcements"));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){var server=event.getServer();if(event.phase!=TickEvent.Phase.END||server.getTickCount()%200!=0)return;if(TrueHeartBattle.resetting(server))return;var epoch=InfiniteWorldData.get(server).generation();if(epoch.isEmpty())return;var view=BreachWorldData.get(server).view();var data=BreachRaidData.get(server,epoch.orElseThrow());long now=server.overworld().getGameTime();boolean retreat=!view.active()||!view.blocker().isEmpty()||view.millionths()<1000000;data.reconcile(now,retreat);if(retreat||server.overworld().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return;for(var player:server.getPlayerList().getPlayers())if(player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==server.overworld()&&InfiniteWorldData.get(server).accessible()&&data.ready(player.getUUID(),now)){var source=InfectionColonies.sources(server.overworld()).stream().filter(s->player.distanceToSqr(s.heart().getCenter())<=96*96).min(java.util.Comparator.comparingDouble(s->player.distanceToSqr(s.heart().getCenter()))).orElse(null);spawn(player,data,view.millionths(),now,source);break;}}
    @SubscribeEvent public static void living(LivingEvent.LivingTickEvent event){var actor=event.getEntity();if(!(actor.level() instanceof ServerLevel level)||actor.tickCount%20!=0||actor.isDeadOrDying())return;var n=marker(actor);if(n==null)return;if(!active(actor)){level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,actor.getX(),actor.getY()+1,actor.getZ(),12,.3,.6,.3,.02);actor.discard();return;}
        var colony=BreachRaidData.get(level.getServer(),n.getUUID("Generation")).colony(n.getUUID("Raid"));if(colony.isPresent()&&InfectionColonies.sourceAlive(level,colony.get()).filter(alive->!alive).isPresent()){
            weaken(actor,net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,"colony-reinforcement-damage",-.35);
            weaken(actor,net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,"colony-reinforcement-speed",-.2);
        }
    }
    private static void weaken(net.minecraft.world.entity.LivingEntity actor,net.minecraft.world.entity.ai.attributes.Attribute attribute,String key,double amount){var value=actor.getAttribute(attribute);var id=UUID.nameUUIDFromBytes(("coraline:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));if(value!=null&&value.getModifier(id)==null)value.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,key,amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){if(!(event.getLevel() instanceof ServerLevel level)||event.getEntity().getRemovalReason()!=Entity.RemovalReason.KILLED||!(event.getEntity() instanceof LivingEntity mob)||mob.getHealth()>0)return;var n=marker(mob);if(n!=null&&InfiniteWorldData.get(level.getServer()).matchesGeneration(n.getUUID("Generation")))BreachRaidData.get(level.getServer(),n.getUUID("Generation")).killed(n.getUUID("Raid"),mob.getUUID());}
    @SubscribeEvent public static void command(AvatarInfectionPulse event){if(event.avatar.authentic()&&event.level==event.level.getServer().overworld()&&event.phase>=0&&event.phase<=3)InfiniteWorldData.get(event.level.getServer()).generation().ifPresent(id->BreachRaidData.get(event.level.getServer(),id).command(event.level.getGameTime()));}
    private BreachRaids(){}
}
