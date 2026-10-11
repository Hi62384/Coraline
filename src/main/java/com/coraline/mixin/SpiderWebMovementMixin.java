package com.coraline.mixin;

import com.coraline.clothing.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Entity.class)
public abstract class SpiderWebMovementMixin {
    @ModifyVariable(method="makeStuckInBlock",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Vec3 coraline$web(Vec3 multiplier,BlockState state,Vec3 original){
        if((Object)this instanceof Player p&&state.getBlock() instanceof WebBlock&&SpiderArmor.hasWebBonus(p)){
            double speed=ClothingConfig.WEB_SPEED.get();return new Vec3(speed,.9,speed);
        }
        return multiplier;
    }
}
