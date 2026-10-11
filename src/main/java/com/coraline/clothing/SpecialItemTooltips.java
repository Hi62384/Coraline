package com.coraline.clothing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Explicit ability summaries for gameplay-changing equipment and artifacts. */
@Mod.EventBusSubscriber(modid="coraline")
public final class SpecialItemTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        var key=ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());if(key==null||!key.getNamespace().equals("coraline"))return;
        String ability=switch(key.getPath()){
            case "button_mace"->"Hits also strike nearby mobs.";
            case "moonstone_pickaxe"->"At night, nearby ores glow through walls.";
            case "moonstone_axe"->"Strikes harder and mines much faster at night.";
            case "moonstone_shovel"->"At night, gains Silk Touch and mines much faster.";
            case "moonstone_hoe"->"At night, mature crops yield extra produce and may drop a golden carrot.";
            case "perfect_pickaxe"->"Mined blocks yield extra drops, as with Fortune III.";
            case "perfect_hoe"->"Tills special farmland that speeds crop growth. Crops harvested with this hoe yield extra produce.";
            case "beldam_sewing_needle"->"Consumes string to repair tools or armor. Can capture weak mobs with a Soul Capturer offhand.";
            case "moonlit_lantern"->"Moonlight amplifies its glow tenfold: 3-block daylight reach, 30-block night reach.";
            case "perfect_excavating_tool"->"Mines basic ground and stone in a 3×3 area, but cannot mine valuable ores.";
            case "builders_dream"->"Hold in your offhand to place and break blocks from farther away.";
            case "magic_umbrella"->"Hold under open sky in rain to heal; slows falls and prevents fall damage.";
            case "garden_mantis_arm"->"Right-click to harvest nearby mature crops in front of you and replant them if you have seeds.";
            case "soul_capturer"->"Offhand: kill a weak mob with the sewing needle to bind it. Right-click to command captured servants.";
            case "dream_staff"->"Right-click to fire a dream pulse that freezes a mob.";
            case "dreamstone"->"Right-click to project into a non-interacting flying dream form; right-click again to return to your body.";
            case "perfect_tree_cutter"->"Break a log to fell its connected, leaf-bearing tree.";
            case "potent_other_flint_and_steel"->"Right-click to briefly create lava; use again to remove it early.";
            case "midnight_crossbow"->"Reloads much faster at night and emits moonlit glints.";
            case "bottomless_lava_bucket"->"Places lava sources without consuming the bucket; each use wears it down.";
            case "bottomless_water_bucket"->"Places water sources without consuming the bucket; each use wears it down.";
            default->null;
        };
        if(ability!=null)event.getToolTip().add(Component.literal(ability).withStyle(ChatFormatting.AQUA));
    }
}
