package com.coraline.item;

import com.coraline.block.DollBlock;
import com.coraline.block.TinyDoorBlock;
import com.coraline.doll.DollBondData;
import com.coraline.otherworld.DoorBlockEntity;
import com.coraline.otherworld.DoorData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEvents;
import com.coraline.parents.ParentsBedBlock;
import com.coraline.parents.ParentsBedItem;
import com.coraline.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import java.util.List;

/** One-use-per-family Creative shortcut for the complete normal-world test setup. */
public final class FamilySetupDebugItem extends Item {
    public FamilySetupDebugItem(){super(new Properties().stacksTo(1));}

    public static String setup(ServerPlayer player){
        if(!player.isCreative())return "Family Setup Debug is Creative-only.";
        ServerLevel level=player.serverLevel();
        if(com.coraline.feast.ActivityWorld.protectedWorld(level))return "Use this in a normal world, not the Other World or an activity.";
        var family=ParentAccess.family(player.server,player.getUUID());
        if(family!=null&&family.bed!=null)return "Your parents already have a bed. This shortcut will not duplicate an existing setup.";
        var bonds=DollBondData.get(player.server);
        if(bonds.bonds.containsKey(player.getUUID()))return "You already have a bound doll. This shortcut will not create another.";
        var doors=DoorData.get(player.server);
        if(doors.hasDoor(player.getUUID()))return "You already have a Little Door. This shortcut will not create another.";
        BlockPos origin=findSite(player);
        if(origin==null)return "No safe spot nearby: a player or block entity would be overwritten. Move a little and try again; nothing was changed.";

        // Level the area before asking the normal parent spawner for safe feet.
        // Uneven terrain can otherwise make that spawner reject a valid site.
        flatten(level,origin);
        if(family==null){
            if(!ParentEvents.spawnClaimedPair(player,origin.south(4)))return "The yard was levelled, but the parents could not spawn. Move to another spot and try again.";
            family=ParentAccess.family(player.server,player.getUUID());
            if(family==null)return "The yard was levelled, but the parent pair did not register. Please report this setup error.";
        }

        // Shape a small, level family yard out of the nearby terrain. This is a
        // Creative debug shortcut, but it never replaces another block entity.
        // All cells were preflighted before spawning. The normal block hooks establish
        // the family-bed and doll-bond links; the door uses the same durable record.
        var parents=ModBlocks.PARENTS_BED.get();
        var parentState=parents.defaultBlockState().setValue(ParentsBedBlock.FACING,Direction.NORTH);
        level.setBlock(origin,parentState,2);
        parents.setPlacedBy(level,origin,parentState,player,ParentsBedItem.forFamily(family.id));

        BlockPos ownBed=origin.offset(3,0,0);
        var foot=Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT);
        level.setBlock(ownBed,foot,2);
        level.setBlock(ownBed.north(),foot.setValue(BedBlock.PART,BedPart.HEAD),3);
        player.setRespawnPosition(level.dimension(),ownBed,0,true,false);

        var dollStack=bonds.claim(player);
        if(dollStack.isEmpty())return "Parents and beds were placed, but the doll could not be bound. Please report this setup error.";
        BlockPos dollPos=origin.offset(4,0,0);
        var dollState=ModBlocks.DOLL.get().defaultBlockState().setValue(DollBlock.FACING,Direction.SOUTH);
        level.setBlock(dollPos,dollState,3);
        ModBlocks.DOLL.get().setPlacedBy(level,dollPos,dollState,player,dollStack);

        var doorStack=doors.gift(player.getUUID());
        if(doorStack.isEmpty())return "The doll was placed, but the Little Door record could not be created. Please report this setup error.";
        var door=doors.doors.get(DoorData.id(doorStack));
        BlockPos doorPos=origin.offset(5,0,0);
        var doorState=ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,Direction.SOUTH);
        level.setBlock(doorPos,doorState,3);
        if(level.getBlockEntity(doorPos) instanceof DoorBlockEntity block){
            block.doorId=door.id;block.setChanged();
            door.placed=true;door.dimension=level.dimension().location().toString();door.pos=doorPos.immutable();
            doors.setDirty();
        }
        return "Test setup ready on a new level yard nearby: claimed Mom and Dad, their double bed, your blue respawn bed, a bound doll, and your Little Door.";
    }

    private static BlockPos findSite(ServerPlayer player){
        var level=player.serverLevel();var ahead=player.blockPosition().relative(player.getDirection(),5);
        for(int radius=0;radius<=12;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int dy:new int[]{0,1,-1,2,-2}){var origin=ahead.offset(dx,dy,dz);
                if(canShape(level,origin))return origin;
            }
        }
        return null;
    }
    private static boolean canShape(ServerLevel level,BlockPos origin){
        if(!level.getEntitiesOfClass(Entity.class,new AABB(origin.offset(0,0,-1),origin.offset(6,3,5)),Entity::isAlive).isEmpty())return false;
        for(int x=0;x<=5;x++)for(int z=-1;z<=4;z++){
            var pos=origin.offset(x,0,z);
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
            for(int y=-1;y<=3;y++)if(level.getBlockEntity(pos.above(y))!=null)return false;
        }
        return true;
    }
    private static void flatten(ServerLevel level,BlockPos origin){
        for(int x=0;x<=5;x++)for(int z=-1;z<=4;z++){
            var pos=origin.offset(x,0,z);
            for(int y=0;y<=3;y++)level.setBlock(pos.above(y),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos.below(),Blocks.CHERRY_PLANKS.defaultBlockState(),3);
            for(int depth=2;depth<=8;depth++){
                var support=pos.below(depth);
                if(!level.getBlockState(support).canBeReplaced())break;
                level.setBlock(support,Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),3);
            }
        }
    }

    private void activate(Player player){if(player instanceof ServerPlayer p){
        p.sendSystemMessage(Component.literal(setup(p)));
        p.getCooldowns().addCooldown(this,20);
    }}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        activate(player);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getPlayer()!=null)activate(context.getPlayer());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Creative: build a complete first-visit test setup nearby.").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Flattens nearby terrain; protects chests and other block entities.").withStyle(ChatFormatting.GRAY));
    }
}
