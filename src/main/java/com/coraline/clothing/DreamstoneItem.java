package com.coraline.clothing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The real player remains vulnerable while an invisible, colliding projection moves nearby. */
@Mod.EventBusSubscriber(modid="coraline")
public final class DreamstoneItem extends Item {
    private record Dream(ServerLevel level,Vec3 body,float yaw,float pitch,UUID projection){}
    private static final Map<UUID,Dream> DREAMS=new HashMap<>();
    public DreamstoneItem(){super(new Item.Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext context){return context.getPlayer()==null?InteractionResult.FAIL:use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){return use(player.level(),player,hand).getResult();}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){if(active(server))stop(server);else start(server);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public static boolean active(ServerPlayer player){return DREAMS.containsKey(player.getUUID());}
    private static void start(ServerPlayer player){
        if(active(player)||player.isSpectator())return;
        SoulCapturerItem.exit(player);
        ArmorStand projection=new ArmorStand(player.level(),player.getX(),player.getY(),player.getZ());
        projection.setInvisible(true);projection.setSilent(true);projection.setNoGravity(true);projection.setInvulnerable(true);
        projection.setYRot(player.getYRot());projection.setXRot(player.getXRot());
        projection.getPersistentData().putBoolean("CoralineDecoration",true);
        if(!player.level().addFreshEntity(projection))return;
        DREAMS.put(player.getUUID(),new Dream(player.serverLevel(),player.position(),player.getYRot(),player.getXRot(),projection.getUUID()));
        // ServerPlayer follows a non-self camera every tick. Only switch the client
        // camera: the vulnerable real body must remain at its original location.
        ArtifactNetwork.dreamState(player,true,projection.getId());
    }
    public static void move(ServerPlayer player,float strafe,float forward,boolean rise,boolean descend,float yaw,float pitch){
        Dream dream=DREAMS.get(player.getUUID());if(dream==null)return;
        if(!Float.isFinite(strafe)||!Float.isFinite(forward)||!Float.isFinite(yaw)||!Float.isFinite(pitch))return;
        var entity=dream.level.getEntity(dream.projection);if(!(entity instanceof ArmorStand projection)){stop(player);return;}
        projection.setYRot(yaw);projection.setXRot(pitch);
        Vec3 facing=Vec3.directionFromRotation(0,yaw),side=new Vec3(facing.z,0,-facing.x);
        Vec3 motion=facing.scale(net.minecraft.util.Mth.clamp(forward,-1,1)*.35).add(side.scale(net.minecraft.util.Mth.clamp(strafe,-1,1)*.35)).add(0,(rise?1:0)*.35-(descend?1:0)*.35,0);
        if(motion.lengthSqr()>.35*.35)motion=motion.normalize().scale(.35);
        if(projection.position().add(motion).distanceToSqr(dream.body)>10000)return;
        projection.move(MoverType.SELF,motion);projection.setDeltaMovement(Vec3.ZERO);projection.hurtMarked=true;
    }
    public static void stop(ServerPlayer player){
        Dream dream=DREAMS.remove(player.getUUID());if(dream==null)return;
        var projection=dream.level.getEntity(dream.projection);if(projection!=null)projection.discard();
        player.setCamera(player);player.teleportTo(dream.level,dream.body.x,dream.body.y,dream.body.z,dream.yaw,dream.pitch);
        ArtifactNetwork.dreamState(player,false,-1);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;
        Dream dream=DREAMS.get(player.getUUID());if(dream==null)return;
        if(!player.isAlive()||player.serverLevel()!=dream.level){stop(player);return;}
        if(!(dream.level.getEntity(dream.projection) instanceof ArmorStand)){stop(player);return;}
        if(player.position().distanceToSqr(dream.body)>.01)player.connection.teleport(dream.body.x,dream.body.y,dream.body.z,dream.yaw,dream.pitch);
        player.setYRot(dream.yaw);player.setXRot(dream.pitch);player.setYHeadRot(dream.yaw);player.setDeltaMovement(Vec3.ZERO);
    }
    @SubscribeEvent public static void blockAttack(AttackEntityEvent event){if(event.getEntity() instanceof ServerPlayer player&&active(player))event.setCanceled(true);}
    @SubscribeEvent public static void blockUse(PlayerInteractEvent.RightClickBlock event){if(event.getEntity() instanceof ServerPlayer player&&active(player))event.setCanceled(true);}
    @SubscribeEvent public static void blockItem(PlayerInteractEvent.RightClickItem event){if(event.getEntity() instanceof ServerPlayer player&&active(player))event.setCanceled(true);}
    @SubscribeEvent public static void blockEntity(PlayerInteractEvent.EntityInteract event){if(event.getEntity() instanceof ServerPlayer player&&active(player))event.setCanceled(true);}
    @SubscribeEvent public static void blockDig(PlayerInteractEvent.LeftClickBlock event){if(event.getEntity() instanceof ServerPlayer player&&active(player))event.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)stop(player);}
}
