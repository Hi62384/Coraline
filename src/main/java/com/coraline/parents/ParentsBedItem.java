package com.coraline.parents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import java.util.UUID;

public final class ParentsBedItem extends BlockItem {
    public ParentsBedItem(Block block,Properties p){super(block,p);}
    @Override public net.minecraft.world.InteractionResult place(BlockPlaceContext context){
        // A four-block placement depends on server-owned family data. Do not predict a
        // successful single-block placement (and shrink the item) on the client.
        if(context.getLevel().isClientSide)return net.minecraft.world.InteractionResult.SUCCESS;
        var result=super.place(context);
        if(!result.consumesAction()&&context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)
            player.containerMenu.broadcastFullState();
        return result;
    }
    public static ItemStack forFamily(UUID id){ItemStack s=new ItemStack(com.coraline.registry.ModItems.PARENTS_BED.get());s.getOrCreateTagElement("BlockEntityTag").putUUID("Family",id);return s;}
    @Override protected boolean canPlace(BlockPlaceContext c,BlockState state){
        if(!super.canPlace(c,state))return false;
        if(c.getLevel() instanceof ServerLevel l){
            var tag=c.getItemInHand().getTagElement("BlockEntityTag");
            var f=tag!=null&&tag.hasUUID("Family")?FamilyData.get(l.getServer()).families.get(tag.getUUID("Family")):null;
            if(f==null||f.dead||c.getPlayer()==null||!c.getPlayer().getUUID().equals(f.owner)||f.bed!=null){
                if(c.getPlayer()!=null)c.getPlayer().displayClientMessage(Component.literal("This bed needs its living parents and can only be placed once by their owner."),true);
                return false;
            }
            for(int part=0;part<4;part++){
                var cell=ParentsBedBlock.partPos(c.getClickedPos(),state.getValue(ParentsBedBlock.FACING),part);
                if(!l.hasChunkAt(cell)||!l.getBlockState(cell).canBeReplaced()||!l.getWorldBorder().isWithinBounds(cell)||!l.getBlockState(cell.below()).isFaceSturdy(l,cell.below(),net.minecraft.core.Direction.UP)){
                    c.getPlayer().displayClientMessage(Component.literal("The double bed needs a clear, supported 2 x 2 area."),true);return false;
                }
                if(!l.isUnobstructed(state,cell,net.minecraft.world.phys.shapes.CollisionContext.empty())){
                    c.getPlayer().displayClientMessage(Component.literal("Someone is standing in the bed's 2 x 2 space. Move them aside and try again."),true);return false;
                }
            }
        }return true;
    }
    @Override public void appendHoverText(ItemStack stack,@org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("2 x 2 double bed. Sneak + right-click for built-in storage.").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(stack.getTagElement("BlockEntityTag")==null || !stack.getTagElement("BlockEntityTag").hasUUID("Family"))
            lines.add(Component.literal("Right-click a tamed parent with this bed to assign it.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
