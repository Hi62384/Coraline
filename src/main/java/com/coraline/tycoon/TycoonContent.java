package com.coraline.tycoon;
import com.coraline.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** Uses the mod's existing registries; no second registration bus or parallel mod. */
public final class TycoonContent {
    public static final Map<String,RegistryObject<Block>> BLOCKS=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
    public static final RegistryObject<Item> WEAVER=item("world_weaver",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public boolean isFoil(ItemStack s){return true;}
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){if(c.getPlayer() instanceof ServerPlayer p&&c.getHand()==InteractionHand.MAIN_HAND)return TycoonWorld.activate(p,c.getClickedPos())?InteractionResult.SUCCESS:InteractionResult.FAIL;return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
    });
    public static final RegistryObject<Item> VOID_POWER=item("void_power",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> SOUL_ESSENCE=item("soul_essence",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> PHONE=item("orphanage_phone",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public InteractionResultHolder<ItemStack> use(Level l,net.minecraft.world.entity.player.Player player,InteractionHand hand){if(player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND){var stack=p.getItemInHand(hand);var owner=stack.hasTag()&&stack.getTag().hasUUID("TycoonOwner")?stack.getTag().getUUID("TycoonOwner"):p.getUUID();TycoonScope.run(p,owner,()->{if(TycoonWorld.validPhone(p,stack)){TycoonAgency.contact(p);TycoonNetwork.open(p,"phone",null);}});}return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),l.isClientSide);}
    });
    public static final RegistryObject<Item> BALL=item("orphan_play_ball",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public InteractionResultHolder<ItemStack> use(Level l,net.minecraft.world.entity.player.Player player,InteractionHand hand){if(player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND){var item=p.getItemInHand(hand);var owner=item.hasTag()&&item.getTag().hasUUID("TycoonOwner")?item.getTag().getUUID("TycoonOwner"):p.getUUID();TycoonScope.run(p,owner,()->TycoonCare.throwBall(p));}return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),l.isClientSide);}
    });
    public static final RegistryObject<Block> FOUNDATION=block("void_foundation");
    public static final RegistryObject<Block> DOOR=block("special_dimension_little_door");
    public static final RegistryObject<Block> DESK=block("world_work_desk");
    public static final RegistryObject<BlockEntityType<TycoonBlockEntity>> STORAGE;
    public static final RegistryObject<net.minecraft.world.entity.EntityType<TycoonNpc>> NPC;
    static{
        for(String id:List.of("orphan_bed","orphan_food_table","orphan_fridge","water_station","orphan_rug","orphan_lamp","orphan_plant","toy_box","storage_cabinet","other_world_supper_table","other_world_fridge"))block(id);
        STORAGE=ModBlockEntities.BLOCK_ENTITIES.register("tycoon_furniture",()->BlockEntityType.Builder.of(TycoonBlockEntity::new,BLOCKS.values().stream().map(RegistryObject::get).filter(b->b!=FOUNDATION.get()).toArray(Block[]::new)).build(null));
        NPC=ModEntities.ENTITIES.register("tycoon_resident",()->net.minecraft.world.entity.EntityType.Builder.<TycoonNpc>of(TycoonNpc::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.55F,1.5F).clientTrackingRange(12).updateInterval(2).build("coraline:tycoon_resident"));
    }
    private static RegistryObject<Item> item(String id,java.util.function.Supplier<Item> supplier){var r=ModItems.ITEMS.register(id,supplier);ITEMS.put(id,r);return r;}
    private static RegistryObject<Block> block(String id){RegistryObject<Block> r=ModBlocks.BLOCKS.register(id,()->new TycoonBlock(id));BLOCKS.put(id,r);ITEMS.put(id,ModItems.ITEMS.register(id,()->new BlockItem(r.get(),new Item.Properties()){
        @Override public InteractionResult place(BlockPlaceContext c){if(!(c.getPlayer() instanceof ServerPlayer p))return c.getLevel().isClientSide?InteractionResult.SUCCESS:InteractionResult.FAIL;var item=c.getItemInHand();if(!item.hasTag()||!item.getTag().hasUUID("TycoonOwner"))return InteractionResult.FAIL;return TycoonScope.call(p,item.getTag().getUUID("TycoonOwner"),()->{if(!TycoonWorld.canPlace(p,id,c.getClickedPos(),item))return InteractionResult.FAIL;return super.place(c);},InteractionResult.FAIL);}
    }));return r;}
    public static ItemStack stack(String id){return new ItemStack(ITEMS.get(id).get());}
    public static String label(String id){var out=new StringBuilder();for(String part:id.split("_")){if(!out.isEmpty())out.append(' ');out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}return out.toString();}
    public static void init(){}
}
