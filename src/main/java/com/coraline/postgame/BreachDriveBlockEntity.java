package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No fuel inventory is stored here; a copied block cannot copy reserve fuel. */
public final class BreachDriveBlockEntity extends BlockEntity implements MenuProvider {
    private Tag receipt=new CompoundTag();
    public BreachDriveBlockEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.BREACH_DRIVE.get(),at,state);}
    void configure(UUID binding,UUID generation){if(!(receipt instanceof CompoundTag n)||!n.isEmpty())return;n.putInt("Version",1);n.putUUID("Binding",binding);n.putUUID("Generation",generation);setChanged();}
    boolean matches(UUID binding,UUID generation){return receipt instanceof CompoundTag n&&n.contains("Version",Tag.TAG_INT)&&n.getInt("Version")==1&&n.hasUUID("Binding")&&n.hasUUID("Generation")&&n.getUUID("Binding").equals(binding)&&n.getUUID("Generation").equals(generation);}
    public boolean canUse(Player player){
        return level instanceof net.minecraft.server.level.ServerLevel server&&player.level()==level&&player.isAlive()&&!player.isSpectator()&&player.distanceToSqr(worldPosition.getCenter())<=64&&getBlockState().is(com.coraline.registry.ModBlocks.BREACH_DRIVE.get())&&BreachWorldData.get(server.getServer()).matchesDrive(server,worldPosition,this)&&BreachWorldData.get(server.getServer()).view().active();
    }
    @Override public void onLoad(){super.onLoad();if(level instanceof net.minecraft.server.level.ServerLevel server){Runnable cleanup=()->BreachWorldData.get(server.getServer()).retireMarkedDrive(this);if(server.getServer().isSameThread())cleanup.run();else server.getServer().execute(cleanup);}}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineBreachDrive",receipt.copy());}
    @Override public void load(CompoundTag n){super.load(n);receipt=n.contains("CoralineBreachDrive")?n.get("CoralineBreachDrive").copy():new CompoundTag();}
    @Override public Component getDisplayName(){return Component.translatable("block.coraline.breach_drive");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return canUse(player)?new BreachDriveMenu(id,inventory,this):null;}
}
