package com.coraline.postgame;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Native slot deltas use the existing durable inventory transaction journal. */
final class QuestWallet {
    static MirrorWallet.Plan donation(ServerPlayer p){var changes=new ArrayList<MirrorWallet.Change>();int glass=4,diamonds=2;for(int slot=0;slot<=40;slot++){if(slot>=36&&slot<40)continue;var before=p.getInventory().getItem(slot);int take=0;if(glass>0&&MirrorWallet.currency(before,com.coraline.registry.ModItems.SOULGLASS.get())){take=Math.min(glass,before.getCount());glass-=take;}else if(diamonds>0&&MirrorWallet.currency(before,com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get())){take=Math.min(diamonds,before.getCount());diamonds-=take;}if(take>0){var after=before.copy();after.shrink(take);changes.add(new MirrorWallet.Change(slot,before.copy(),after));}}return glass==0&&diamonds==0?new MirrorWallet.Plan(List.copyOf(changes)):null;}
    private static List<ItemStack> rewards(net.minecraft.nbt.CompoundTag assignment){
        var glass=com.coraline.registry.ModItems.SOULGLASS.get();var moon=com.coraline.clothing.OtherMaterials.MOONSTONE.get();var iron=com.coraline.clothing.OtherMaterials.OTHER_IRON.get();var diamond=com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get();
        if(assignment.getInt("RewardVersion")==0)return List.of(new ItemStack(glass,2),new ItemStack(moon));
        return switch(PostgameQuests.Mission.valueOf(assignment.getString("Mission"))){
            case TOWER->List.of(new ItemStack(moon),new ItemStack(diamond));
            case FACTORY->List.of(new ItemStack(iron,6),new ItemStack(glass,2));
            case CARNIVAL->List.of(new ItemStack(glass),new ItemStack(iron,2));
            case ORPHANAGE->List.of(new ItemStack(glass,3),new ItemStack(moon));
            case THEATRE->List.of(new ItemStack(glass,2),new ItemStack(diamond));
            case SANCTUARY->List.of(new ItemStack(moon,2),new ItemStack(glass));
            case SEAM->List.of(new ItemStack(glass),new ItemStack(moon));
            case DRILL->List.of(new ItemStack(iron,4),new ItemStack(diamond));
            case BOUNDARY->List.of(new ItemStack(moon,2));
            case OBSERVATORY->List.of(new ItemStack(glass,2),new ItemStack(moon,2));
            case HEART_LOCATION->List.of(new ItemStack(glass));
            case HEART->List.of(new ItemStack(glass,4),new ItemStack(diamond,2),new ItemStack(moon,2));
        };
    }
    static net.minecraft.network.chat.Component description(net.minecraft.nbt.CompoundTag assignment){var text=net.minecraft.network.chat.Component.empty();boolean first=true;for(var output:rewards(assignment)){if(!first)text.append(net.minecraft.network.chat.Component.literal(", "));first=false;text.append(net.minecraft.network.chat.Component.literal(output.getCount()+" × ")).append(output.getHoverName());}return text;}
    static MirrorWallet.Plan reward(ServerPlayer p,net.minecraft.nbt.CompoundTag assignment){var proposed=new LinkedHashMap<Integer,ItemStack>();for(var output:rewards(assignment)){int left=output.getCount();for(int slot=0;slot<36&&left>0;slot++){var stack=proposed.getOrDefault(slot,p.getInventory().getItem(slot));if(!MirrorWallet.currency(stack,output.getItem()))continue;int add=Math.min(left,stack.getMaxStackSize()-stack.getCount());if(add>0){var after=stack.copy();after.grow(add);proposed.put(slot,after);left-=add;}}for(int slot=0;slot<36&&left>0;slot++)if(proposed.getOrDefault(slot,p.getInventory().getItem(slot)).isEmpty()){var after=output.copy();after.setCount(left);proposed.put(slot,after);left=0;}if(left!=0)return null;}var changes=new ArrayList<MirrorWallet.Change>();for(var entry:proposed.entrySet())changes.add(new MirrorWallet.Change(entry.getKey(),p.getInventory().getItem(entry.getKey()).copy(),entry.getValue()));return new MirrorWallet.Plan(List.copyOf(changes));}
    private QuestWallet(){}
}
