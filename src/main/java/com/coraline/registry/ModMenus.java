package com.coraline.registry;
import com.coraline.parents.ParentsMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"coraline");
    public static final RegistryObject<MenuType<ParentsMenu>> PARENTS=MENUS.register("parents",()->IForgeMenuType.create(ParentsMenu::new));
    public static final RegistryObject<MenuType<com.coraline.postgame.BreachDriveMenu>> BREACH_DRIVE=MENUS.register("breach_drive",()->IForgeMenuType.create(com.coraline.postgame.BreachDriveMenu::new));
    public static final RegistryObject<MenuType<com.coraline.postgame.MagicMirrorMenu>> MAGIC_MIRROR=MENUS.register("magic_mirror",()->IForgeMenuType.create(com.coraline.postgame.MagicMirrorMenu::new));
}
