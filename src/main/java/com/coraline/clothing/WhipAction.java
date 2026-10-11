package com.coraline.clothing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-authoritative cooldown, including missed swings. */
public final class WhipAction {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","whip"),()->"1","1"::equals,"1"::equals);
    public record Swing(){}
    public record Lash(int player,Vec3 end){}
    public static void register(){
        CHANNEL.messageBuilder(Swing.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new Swing()).consumerMainThread((m,c)->{
            var p=c.get().getSender();if(p!=null)begin(p,null);
        }).add();
        CHANNEL.messageBuilder(Lash.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeInt(m.player);b.writeDouble(m.end.x);b.writeDouble(m.end.y);b.writeDouble(m.end.z);}).decoder(b->new Lash(b.readInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()))).consumerMainThread((m,c)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.coraline.client.clothing.WhipVisual.receive(m))).add();
    }
    public static boolean isWhip(Player p){return p.getMainHandItem().getItem() instanceof StoryWeapon w&&w.id.equals("thread_whip");}
    public static boolean begin(Player p,Entity target){
        if(!isWhip(p)||p.isSpectator())return false;
        var item=p.getMainHandItem().getItem();if(p.getCooldowns().isOnCooldown(item))return false;
        if(p.level().isClientSide)return true;
        p.getCooldowns().addCooldown(item,WeaponConfig.WHIP_COOLDOWN.get());
        Vec3 end=target==null?p.getEyePosition().add(p.getLookAngle().scale(5.5)):target.getBoundingBox().getCenter();
        if(p instanceof ServerPlayer sp)CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->sp),new Lash(p.getId(),end));
        p.level().playSound(null,p.blockPosition(),com.coraline.registry.ModSounds.THREAD_WHIP.get(),SoundSource.PLAYERS,.85F,.94F+p.getRandom().nextFloat()*.12F);
        return true;
    }
    public static void miss(){CHANNEL.sendToServer(new Swing());}
}
