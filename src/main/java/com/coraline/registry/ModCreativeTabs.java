package com.coraline.registry;

import com.coraline.CoralineMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import java.util.Comparator;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CoralineMod.MOD_ID);
    private enum Shelf { STORY, WARDROBE, BUILDING, DEBUG }
    private static boolean hidden(RegistryObject<Item> item){return item==ModItems.MAGIC_BUTTON_TOKEN||item==ModItems.CARNIVAL_TOKEN||item==ModItems.CIRCUS_COTTON_CANDY||item==ModItems.CIRCUS_POPCORN;}
    private static Shelf shelf(RegistryObject<Item> item){
        String id=item.getId().getPath();
        if(id.contains("debug")||id.contains("debugger"))return Shelf.DEBUG;
        if(item.get() instanceof com.coraline.clothing.ClothingItem)return Shelf.WARDROBE;
        if(item.get() instanceof BlockItem)return Shelf.BUILDING;
        return Shelf.STORY;
    }
    private static void items(Shelf shelf,CreativeModeTab.Output output){
        for(var item:ModItems.ITEMS.getEntries().stream().filter(entry->!hidden(entry)&&shelf(entry)==shelf)
                .sorted(Comparator.comparing(entry->entry.getId().getPath())).toList()){
            if(item==ModItems.GHOST_EYE)for(int index=0;index<3;index++){
                var sample=new ItemStack(item.get());sample.getOrCreateTag().putInt("GhostEye",index);output.accept(sample);
            }else output.accept(item.get());
        }
    }
    private static CreativeModeTab tab(String title,RegistryObject<Item> icon,Shelf shelf){return CreativeModeTab.builder()
            .title(Component.literal(title)).icon(()->icon.get().getDefaultInstance())
            .displayItems((parameters,output)->items(shelf,output)).build();}
    public static final RegistryObject<CreativeModeTab> CORALINE=TABS.register("coraline",()->tab("Coraline | Story",ModItems.DOWSING_ROD,Shelf.STORY));
    public static final RegistryObject<CreativeModeTab> WARDROBE=TABS.register("wardrobe",()->tab("Coraline | Wardrobe",ModItems.MIDNIGHT_COAT,Shelf.WARDROBE));
    public static final RegistryObject<CreativeModeTab> BUILDING=TABS.register("building",()->tab("Coraline | Building",ModItems.PARENTS_BED,Shelf.BUILDING));
    public static final RegistryObject<CreativeModeTab> DEBUG=TABS.register("debug",()->tab("Coraline | Debug",ModItems.STORY_SKIP_DEBUG,Shelf.DEBUG));
    private ModCreativeTabs() {}
}
