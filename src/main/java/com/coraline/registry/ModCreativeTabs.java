package com.coraline.registry;

import com.coraline.CoralineMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CoralineMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> CORALINE = TABS.register("coraline", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup.coraline"))
                    .icon(() -> ModItems.DOWSING_ROD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().stream().filter(item->item!=ModItems.MAGIC_BUTTON_TOKEN&&item!=ModItems.CARNIVAL_TOKEN).forEach(item -> output.accept(item.get())))
                    .build());
    private ModCreativeTabs() {}
}
