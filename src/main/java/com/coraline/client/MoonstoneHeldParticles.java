package com.coraline.client;

import com.coraline.clothing.ClothingEffects;
import com.coraline.clothing.OtherMaterials;
import com.coraline.clothing.Weapons;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** Tiny moonlit glints sit beside the first-person hand instead of surrounding the player's body. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MoonstoneHeldParticles {
    private static final DustParticleOptions GLINT=new DustParticleOptions(new Vector3f(.32F,.79F,1F),.18F);
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var player=mc.player;
        if(player==null||mc.isPaused()||!ClothingEffects.night(player.level())||player.tickCount%4!=0)return;
        boolean main=isMoonstone(player.getMainHandItem());
        boolean off=isMoonstone(player.getOffhandItem());
        if(!main&&!off)return;
        var look=player.getLookAngle();var right=look.cross(new Vec3(0,1,0)).normalize();
        var eye=player.getEyePosition();
        if(main)particle(player.level(),eye.add(look.scale(.7)).add(right.scale(.38)).add(0,-.29,0),player.tickCount);
        if(off)particle(player.level(),eye.add(look.scale(.7)).add(right.scale(-.38)).add(0,-.29,0),player.tickCount+7);
    }
    private static boolean isMoonstone(net.minecraft.world.item.ItemStack stack){
        if(stack.is(Weapons.ITEMS.get("moonblade").get()))return true;
        if(stack.is(com.coraline.clothing.ArtifactItems.MIDNIGHT_CROSSBOW.get()))return true;
        for(var entry:OtherMaterials.TOOLS.entrySet())if(entry.getKey().startsWith("moonstone_")&&stack.is(entry.getValue().get()))return true;
        return false;
    }
    private static void particle(net.minecraft.world.level.Level level,Vec3 center,int age){
        double wiggle=Math.sin(age*.6)*.08;
        level.addParticle(GLINT,center.x+wiggle,center.y+Math.cos(age*.4)*.045,center.z,0,.005,0);
    }
}
