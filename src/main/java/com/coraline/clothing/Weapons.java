package com.coraline.clothing;

import com.coraline.registry.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="coraline")
public final class Weapons {
    public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
    private static final UUID MOON_ID=UUID.fromString("f01be0bc-3d6b-4db8-9d5c-871819faf7ad");
    static {
        weapon("needle_rapier",3,3.2F);weapon("moonblade",8,1.6F);weapon("vine_sword",6,1.6F);
        ITEMS.put("button_mace",ModItems.ITEMS.register("button_mace",ButtonMaceItem::new));weapon("spidervision_sword",8,1.4F);weapon("thread_whip",5,1.2F);
        ITEMS.put("midnight_wand",ModItems.ITEMS.register("midnight_wand",MidnightWand::new));
    }
    private static void weapon(String id,int damage,float speed){ITEMS.put(id,ModItems.ITEMS.register(id,()->new StoryWeapon(id,damage,speed)));}
    public static void init(){WhipAction.register();}
    @SubscribeEvent public static void attack(AttackEntityEvent event){
        var p=event.getEntity();
        if(WhipAction.isWhip(p)&&!WhipAction.begin(p,event.getTarget())){event.setCanceled(true);return;}
        if(p.getMainHandItem().getItem() instanceof StoryWeapon)p.getPersistentData().putFloat("CoralineWeaponCharge",p.getAttackStrengthScale(.5F));
    }
    @SubscribeEvent public static void moon(TickEvent.PlayerTickEvent event){
        var p=event.player;if(event.phase!=TickEvent.Phase.END||p.level().isClientSide)return;
        var attribute=p.getAttribute(Attributes.ATTACK_DAMAGE);if(attribute==null)return;
        double bonus=p.isAlive()&&p.getMainHandItem().is(ITEMS.get("moonblade").get())&&ClothingEffects.night(p.level())?7:0;
        var old=attribute.getModifier(MOON_ID);if(old!=null&&old.getAmount()!=bonus)attribute.removeModifier(MOON_ID);
        if(bonus>0&&attribute.getModifier(MOON_ID)==null)attribute.addTransientModifier(new AttributeModifier(MOON_ID,"Coraline moonblade night edge",bonus,AttributeModifier.Operation.ADDITION));
    }
}
