package com.coraline.clothing;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
import static com.coraline.clothing.ClothingItem.Style.*;

@Mod.EventBusSubscriber(modid="coraline")
public final class ClothingEffects {
    public static final UUID SPEED_ID=UUID.fromString("08d0c42b-e98c-4715-9be5-fac9e0fbbe64");
    public static boolean wearing(LivingEntity entity,ClothingItem.Style style){
        return entity.getItemBySlot(style.slot.getSlot()).getItem() instanceof ClothingItem item&&item.style==style;
    }
    public static boolean hasDayCycle(Level level){return level.dimensionType().hasSkyLight()&&level.dimensionType().natural()&&!level.dimensionType().hasFixedTime();}
    public static boolean inWindow(long time,int start,int end){long t=Math.floorMod(time,24000);return start<=end?t>=start&&t<=end:t>=start||t<=end;}
    public static boolean midnight(Level level){return hasDayCycle(level)&&inWindow(level.getDayTime(),ClothingConfig.MIDNIGHT_START.get(),ClothingConfig.MIDNIGHT_END.get());}
    public static boolean night(Level level){return hasDayCycle(level)&&inWindow(level.getDayTime(),13000,23000);}
    /** Lanterns also recognize the Other World's fixed nighttime sky. */
    public static boolean lanternNight(Level level){return night(level)||(com.coraline.feast.ActivityWorld.isActivity(level)||com.coraline.otherworld.OtherWorld.isOther(level))&&level.dimensionType().hasFixedTime();}
    public static double speedBonus(Player player){
        double bonus=ArmorSets.count(player,ArmorSets.Set.DREAMWEAVE)*ClothingConfig.DREAM_SPEED.get();
        if(wearing(player,MIDNIGHT_COAT)&&midnight(player.level()))bonus+=ClothingConfig.MIDNIGHT_SPEED.get();
        if(wearing(player,RAINCOAT)&&hasDayCycle(player.level())&&player.level().isRaining())bonus+=ClothingConfig.RAIN_SPEED.get();
        return bonus;
    }
    public static boolean slowFall(Player p){return wearing(p,FEATHER_BOOTS)||(night(p.level())&&ArmorSets.full(p,ArmorSets.Set.MIDNIGHT));}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Player p=event.player;
        if(!p.level().isClientSide){
            var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);double bonus=p.isAlive()?speedBonus(p):0;
            var previous=speed.getModifier(SPEED_ID);
            if(previous!=null&&previous.getAmount()!=bonus)speed.removeModifier(SPEED_ID);
            if(bonus>0&&speed.getModifier(SPEED_ID)==null)speed.addTransientModifier(new AttributeModifier(SPEED_ID,"Coraline clothing speed",bonus,AttributeModifier.Operation.MULTIPLY_TOTAL));
            var tag=p.getPersistentData();
            if(p.isAlive()&&wearing(p,COMFY_PAJAMAS)){
                int ticks=tag.getInt("CoralinePajamaTicks")+1;
                if(ticks>=ClothingConfig.REGEN_TICKS.get()){p.heal(ClothingConfig.REGEN_HEALTH.get().floatValue());ticks=0;}
                tag.putInt("CoralinePajamaTicks",ticks);
            }else tag.remove("CoralinePajamaTicks");
        }
        // Both sides for prediction. Do not overwrite/remove a potion effect from another source.
        if(p.isAlive()&&slowFall(p)&&!p.onGround()&&!p.isInWaterOrBubble()&&!p.isFallFlying()&&!p.getAbilities().flying&&p.getDeltaMovement().y<0){
            var motion=p.getDeltaMovement();p.setDeltaMovement(motion.x,Math.max(motion.y,-ClothingConfig.FALL_SPEED.get()),motion.z);p.fallDistance=0;
        }
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event){
        if(!(event.getEntity() instanceof Player p))return;
        double extra=wearing(p,CIRCUS_JACKET)?ClothingConfig.CIRCUS_JUMP.get():wearing(p,MIDNIGHT_COAT)&&midnight(p.level())?ClothingConfig.MIDNIGHT_JUMP.get():0;
        if(night(p.level())&&ArmorSets.full(p,ArmorSets.Set.MIDNIGHT))extra+=ClothingConfig.SET_JUMP.get();
        if(extra>0)p.setDeltaMovement(p.getDeltaMovement().add(0,extra,0));
    }
    @SubscribeEvent public static void ate(LivingEntityUseItemEvent.Finish event){
        if(!(event.getEntity() instanceof Player p)||p.level().isClientSide||!wearing(p,GARDEN_OVERALLS))return;
        var food=event.getItem().getFoodProperties(p);
        if(food!=null)p.heal((float)(food.getNutrition()*ClothingConfig.FOOD_HEAL.get()));
    }
    @SubscribeEvent public static void visibility(LivingEvent.LivingVisibilityEvent event){
        if(event.getEntity() instanceof Player p&&wearing(p,SILENT_SLIPPERS)&&(event.getLookingEntity()==null||event.getLookingEntity() instanceof Mob))event.modifyVisibility(ClothingConfig.VISIBILITY.get());
        if(event.getEntity() instanceof Player p&&night(p.level())&&ArmorSets.full(p,ArmorSets.Set.MIDNIGHT_CLOAK)&&(event.getLookingEntity()==null||event.getLookingEntity() instanceof Mob))event.modifyVisibility(ClothingConfig.CLOAK_VISIBILITY.get());
    }
    @SubscribeEvent public static void falling(LivingFallEvent event){
        if(event.getEntity() instanceof Player p&&slowFall(p))event.setDamageMultiplier(0);
    }
}
