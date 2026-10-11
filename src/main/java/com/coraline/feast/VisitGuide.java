package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner-only, bounded route guidance with an explicit non-destructive rescue. */
@Mod.EventBusSubscriber(modid="coraline")
public final class VisitGuide {
    private VisitGuide(){}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("coraline")
            .then(Commands.literal("guide").executes(c->guide(c.getSource().getPlayerOrException()))));
    }
    public static void instructions(ServerPlayer p){
        // Recovery is automatic and intentionally silent; story presentation stays out of chat.
    }
    static BlockPos target(ServerPlayer p,VisitData.Scene s){
        if(s.loop.active())return OtherMotherLoop.target(p,s);
        var r=RealmData.get(p.server).realms.get(s.door);if(r==null)return null;
        return switch(s.stage){
            case FIND_FATHER->s.piano.north(2);
            case RETURN_TO_MOTHER,SIT->s.table.north(2);
            case BED->r.bed;
            default->null;
        };
    }
    static Path route(ServerPlayer p,BlockPos goal){
        if(goal==null||p.distanceToSqr(goal.getCenter())>128*128)return null;
        var probe=ModEntities.MOM.get().create(p.serverLevel());if(probe==null)return null;
        probe.setPos(p.position());probe.setOnGround(true);ScenePlacement.prepareWalker(probe);
        return probe.getNavigation().createPath(goal,1);
    }
    public static void trail(ServerPlayer p,VisitData.Scene s){
        var goal=target(p,s);if(goal==null)return;var path=route(p,goal);
        if(path==null||!path.canReach())return;
        for(int i=0;i<Math.min(path.getNodeCount(),36);i+=2){var node=path.getNode(i);
            p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,node.x+.5,node.y+.18,node.z+.5,1,.04,.02,.04,0);
        }
    }
    public static int guide(ServerPlayer p){
        var s=FirstVisit.active(p);if(s==null)s=OtherMotherLoop.active(p);if(s==null||ActivityWorld.isActivity(p.level())){p.sendSystemMessage(Component.literal("No active quest destination."));return 0;}
        var goal=target(p,s);if(goal==null){p.sendSystemMessage(Component.literal("This scene is still playing. The guide becomes available when you have somewhere to go."));return 0;}
        // Explicit opt-in recovery never advances the story, generates loot or changes blocks.
        if(!relocate(p,goal)){p.sendSystemMessage(Component.literal("That destination is obstructed. Waiting for a safe landing spot."));return 0;}
        p.sendSystemMessage(Component.literal("The guide brought you to the next destination."));return 1;
    }
    static boolean relocate(ServerPlayer p,BlockPos goal){
        var level=p.serverLevel();level.getChunkAt(goal);
        for(int radius=0;radius<=4;radius++)for(int dy:new int[]{0,1,-1,2,-2,3,-3})for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
            var at=goal.offset(x,dy,z);if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at))continue;
            var floor=level.getBlockState(at.below());
            if(!floor.isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP)||floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)||floor.is(net.minecraft.world.level.block.Blocks.CACTUS)||floor.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock)continue;
            var box=net.minecraft.world.entity.EntityDimensions.fixed(.6F,1.8F).makeBoundingBox(at.getX()+.5,at.getY(),at.getZ()+.5);
            if(!level.noCollision(p,box)||level.containsAnyLiquid(box))continue;
            var feet=net.minecraft.world.phys.Vec3.atBottomCenterOf(at);
            var sight=level.clip(new net.minecraft.world.level.ClipContext(feet.add(0,1.6,0),goal.getCenter().add(0,1,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(sight.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&sight.getBlockPos().distSqr(goal)>2)continue;
            p.stopRiding();p.setForcedPose(null);p.teleportTo(level,feet.x,feet.y,feet.z,p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return true;
        }
        return false;
    }
}
