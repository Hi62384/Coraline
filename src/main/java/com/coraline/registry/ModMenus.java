package com.coraline.registry;
import com.coraline.parents.ParentsMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"coraline");
    public static final RegistryObject<MenuType<ParentsMenu>> PARENTS=MENUS.register("parents",()->IForgeMenuType.create(ParentsMenu::new));
}
