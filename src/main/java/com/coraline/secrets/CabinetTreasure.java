package com.coraline.secrets;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Real loot tables fill the existing durable cabinet inventory once. No
 * physical key/evidence/quest item is created by these ordinary loot pools. */
final class CabinetTreasure {
    enum Result { SEEDED, PREPARED, UNCHANGED, MISSING_TABLE, INVALID }
    private static final String VERSION="TreasureVersion", PENDING="PreparedTreasure";
    private static final Set<String> ALLOWED=Set.of("button","other_iron_ingot","other_diamond","moonstone","perfect_ingot","soulglass",
        "midnight_coat","star_shirt","raincoat","comfy_pajamas","garden_overalls","circus_jacket","silent_slippers","feather_boots","top_hat","midnight_goggles");
    static String profile(int cabinet,int visits) {
        return switch(cabinet) {
            case 0 -> "hall";case 1 -> "kitchen";case 2 -> "bedroom";case 3 -> "sitting_room";
            case 4 -> "storage";case 5 -> "attic";case 6 -> "basement";
            case 7 -> visits>=9?"archive_late":"archive";case 8 -> "landing";
            default -> throw new IllegalArgumentException("Unknown authored cabinet");
        };
    }
    private static boolean valid(ItemStack stack) {
        if(stack.isEmpty()||stack.getCount()>stack.getMaxStackSize())return false;
        // Damageable ItemStack construction adds its ordinary Damage:0 tag.
        // Permit that default only; damaged gear, enchantments and story or
        // ownership credentials still cannot enter ordinary prepared loot.
        var tag=stack.getTag();
        if(tag!=null&&!tag.isEmpty()&&!(stack.isDamageableItem()&&tag.size()==1&&tag.contains("Damage",Tag.TAG_INT)&&tag.getInt("Damage")==0))return false;
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id!=null&&(id.getNamespace().equals("coraline")&&ALLOWED.contains(id.getPath())
            ||id.getNamespace().equals("minecraft")&&Set.of("string","cookie","bread","amethyst_shard").contains(id.getPath()));
    }
    static Result seed(ServerPlayer player,CompoundTag cabinet,String profile,long seed) {
        if(cabinet.contains(VERSION))return cabinet.contains(VERSION,Tag.TAG_INT)&&cabinet.getInt(VERSION)==1?Result.UNCHANGED:Result.INVALID;
        if(cabinet.contains("Items")&&(!(cabinet.get("Items") instanceof ListTag stored)||!stored.isEmpty()&&stored.getElementType()!=Tag.TAG_COMPOUND))return Result.INVALID;
        ListTag prepared;
        boolean justPrepared=false;
        if(cabinet.contains(PENDING)) {
            if(!(cabinet.get(PENDING) instanceof ListTag list)||list.isEmpty()||list.getElementType()!=Tag.TAG_COMPOUND||cabinet.getInt("PreparedTreasureVersion")!=1)return Result.INVALID;
            prepared=list;
        } else {
            if(!profile.matches("[a-z_]+"))return Result.INVALID;
            var id=ResourceLocation.fromNamespaceAndPath("coraline","secrets/cabinets/"+profile);
            var table=player.server.getLootData().getLootTable(id);
            if(table==LootTable.EMPTY)return Result.MISSING_TABLE;
            var params=new LootParams.Builder(player.serverLevel()).withParameter(LootContextParams.ORIGIN,player.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY,player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
            var generated=table.getRandomItems(params,seed==0?1:seed);
            if(generated.isEmpty()||generated.size()>8||generated.stream().anyMatch(stack->!valid(stack)))return Result.INVALID;
            prepared=new ListTag();for(var stack:generated)prepared.add(stack.save(new CompoundTag()));
            cabinet.put(PENDING,prepared);cabinet.putInt("PreparedTreasureVersion",1);cabinet.putString("PreparedTreasureTable",id.toString());justPrepared=true;
        }
        if(prepared.size()>8)return Result.INVALID;
        for(var entry:prepared)if(!valid(ItemStack.of((CompoundTag)entry)))return Result.INVALID;
        var stored=cabinet.getList("Items",Tag.TAG_COMPOUND);
        // Reserve space for the cabinet's existing real key and evidence slot.
        // A full cabinet retains the exact prepared roll until the owner clears
        // room; opening it again or progressing a visit cannot reroll it.
        if(stored.size()+prepared.size()>25)return justPrepared?Result.PREPARED:Result.UNCHANGED;
        var merged=stored.copy();for(var entry:prepared)merged.add(entry.copy());
        cabinet.put("Items",merged);cabinet.putInt(VERSION,1);
        cabinet.remove(PENDING);cabinet.remove("PreparedTreasureVersion");cabinet.remove("PreparedTreasureTable");return Result.SEEDED;
    }
    private CabinetTreasure() {}
}
