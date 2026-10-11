package com.coraline.parents;
import com.coraline.config.CoralineConfig;
import com.coraline.registry.ModItems;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class ParentTasks {
    public static boolean working(ParentEntity p,FamilyData.Family f){
        return !f.dead&&f.deathPhase==0&&f.owner!=null&&f.bed!=null&&f.chest!=null
                &&p.level().dimension().location().toString().equals(f.homeDim)
                &&(p.isMom()?f.momTask:f.dadTask)!=0;
    }
    public static int nextDelay(ParentEntity p){
        int base=CoralineConfig.TASK_INTERVAL.get();
        double variation=CoralineConfig.TASK_VARIATION.get();
        return Math.max(20,(int)Math.round(base*(1-variation+2*variation*p.getRandom().nextDouble())));
    }
    private static void workWalk(ParentEntity p,FamilyData.Family f){
        if(!p.getNavigation().isDone()||p.tickCount%10!=0)return;
        ServerLevel level=(ServerLevel)p.level();int radius=CoralineConfig.HOME_RADIUS.get();
        for(int attempt=0;attempt<16;attempt++){
            BlockPos candidate=f.bed.offset(p.getRandom().nextInt(radius*2+1)-radius,0,p.getRandom().nextInt(radius*2+1)-radius);
            var target=ParentEvents.safe(level,p,candidate,2);
            if(target==null||target.distSqr(f.bed)>radius*radius||p.distanceToSqr(target.getCenter())<4)continue;
            var path=p.getNavigation().createPath(target,0);
            if(path!=null&&path.canReach()){
                p.getNavigation().moveTo(path,CoralineConfig.TASK_WALK_SPEED.get());return;
            }
        }
    }
    public static void tick(ParentEntity p,FamilyData.Family f){
        if(!(p.level() instanceof ServerLevel l)||!working(p,f))return;
        if(p.distanceToSqr(f.bed.getCenter())>Math.pow(CoralineConfig.HOME_RADIUS.get()+4,2))return;
        long now=l.getGameTime();int task=p.isMom()?f.momTask:f.dadTask;
        if(task==0)return;
        if(now<(p.isMom()?f.momDelivery:f.dadDelivery)){workWalk(p,f);return;}
        if(!(p.isMom()&&task==1)){
            var state=l.getBlockState(f.bed);
            if(!(state.getBlock() instanceof ParentsBedBlock))return;
            var facing=state.getValue(ParentsBedBlock.FACING);
            if(!approachStorage(p,f,facing))return;
            p.getNavigation().stop();
            p.getLookControl().setLookAt(f.bed.getX()+.5,f.bed.getY()+.5,f.bed.getZ()+.5);
        }
        if(p.isMom())f.momDelivery=now+nextDelay(p);else f.dadDelivery=now+nextDelay(p);
        FamilyData.get(l.getServer()).setDirty();
        if(p.isMom()&&task==1){garden(p,f);return;}
        if(!(l.getBlockEntity(f.bed) instanceof ParentsBedEntity chest))return;
        ItemStack reward=reward(p,task);if(reward.isEmpty())return;
        reward.setCount(Math.min(reward.getCount(),reward.getMaxStackSize()));
        // Merge without overwriting occupied slots; a full chest simply pauses that delivery.
        for(int i=0;i<chest.getContainerSize()&&!reward.isEmpty();i++){
            ItemStack old=chest.getItem(i);
            if(old.isEmpty()){chest.setItem(i,reward.copy());reward.setCount(0);}
            else if(ItemStack.isSameItemSameTags(old,reward)){
                int count=Math.min(reward.getCount(),old.getMaxStackSize()-old.getCount());old.grow(count);reward.shrink(count);
            }
        }chest.setChanged();
    }
    public static ItemStack reward(ParentEntity p,int task){
        int food=CoralineConfig.FOOD_COUNT.get(),count=CoralineConfig.RESOURCE_COUNT.get();var random=p.getRandom();
        if(p.isMom())return switch(task){
            case 2 -> new ItemStack(random.nextDouble()<CoralineConfig.WRAP_CHANCE.get()?ModItems.MUSTARD_KETCHUP_SALSA_WRAP.get():new Item[]{Items.APPLE,Items.CARROT,Items.POTATO,Items.BREAD,Items.BEETROOT}[random.nextInt(5)],food);
            case 3 -> armor(p);
            case 4 -> new ItemStack(new Item[]{Items.OAK_LOG,Items.BIRCH_LOG,Items.SPRUCE_LOG}[random.nextInt(3)],count);
            case 5 -> new ItemStack(Items.COBBLESTONE,count);
            default -> ItemStack.EMPTY;
        };
        return switch(task){
            case 1 -> new ItemStack(random.nextDouble()<CoralineConfig.SLIME_CHANCE.get()?ModItems.DADS_SLIME.get():new Item[]{Items.COOKED_BEEF,Items.COOKED_CHICKEN,Items.PUMPKIN_PIE,Items.RABBIT_STEW}[random.nextInt(4)],Math.min(food,16));
            case 2 -> new ItemStack(Items.RAW_IRON,count);
            case 3 -> new ItemStack(Items.COAL,count);
            default -> ItemStack.EMPTY;
        };
    }
    /** Both front positions and the two trunk sides are legitimate close-up delivery spots. */
    private static boolean approachStorage(ParentEntity p,FamilyData.Family f,Direction facing){
        var right=facing.getClockWise();var front=f.bed.relative(facing);
        var spots=java.util.List.of(front.relative(right,p.isMom()?0:1),front.relative(right,p.isMom()?1:0),f.bed.relative(right.getOpposite()),f.bed.relative(right,2));
        var level=(ServerLevel)p.level();
        for(var spot:spots){
            var box=p.getDimensions(p.getPose()).makeBoundingBox(spot.getX()+.5,spot.getY(),spot.getZ()+.5);
            if(!level.getBlockState(spot.below()).isFaceSturdy(level,spot.below(),Direction.UP)||level.getBlockCollisions(p,box).iterator().hasNext())continue;
            if(p.distanceToSqr(spot.getCenter())<=2.25D)return true;
        }
        if(p.tickCount%10!=0)return false;
        for(var spot:spots){var path=p.getNavigation().createPath(spot,0);if(path!=null&&path.canReach()){p.getNavigation().moveTo(path,CoralineConfig.TASK_WALK_SPEED.get());break;}}
        return false;
    }
    private static ItemStack armor(ParentEntity p){
        Item[] items=p.getRandom().nextDouble()<CoralineConfig.GOOD_ARMOR.get()
                ?new Item[]{Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS}
                :new Item[]{Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS,Items.IRON_BOOTS,Items.CHAINMAIL_HELMET};
        return new ItemStack(items[p.getRandom().nextInt(items.length)]);
    }
    private static void garden(ParentEntity p,FamilyData.Family f){
        if(!CoralineConfig.GARDEN_CHANGES.get()||f.gardenPlots>=CoralineConfig.GARDEN_MAX.get())return;
        ServerLevel l=(ServerLevel)p.level();if(!l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING))return;
        int radius=CoralineConfig.GARDEN_RADIUS.get();
        for(int attempt=0;attempt<32;attempt++){
            int x=p.getRandom().nextInt(radius*2+1)-radius,z=p.getRandom().nextInt(radius*2+1)-radius;
            BlockPos soil=f.bed.offset(x,-1,z),crop=soil.above();
            if(Math.abs(x)+Math.abs(z)<3||!l.hasChunkAt(soil)||!l.getBlockState(soil).is(BlockTags.DIRT)||!l.isEmptyBlock(crop)||l.getMaxLocalRawBrightness(crop)<9)continue;
            boolean water=false;
            for(BlockPos check:BlockPos.betweenClosed(soil.offset(-4,0,-4),soil.offset(4,1,4)))if(l.getFluidState(check).is(net.minecraft.tags.FluidTags.WATER)){water=true;break;}
            if(!water){
                BlockPos well=soil.relative(Direction.EAST);
                if(!l.getBlockState(well).is(BlockTags.DIRT)||!l.isEmptyBlock(well.above()))continue;
                l.setBlock(well,Blocks.WATER.defaultBlockState(),3);
            }
            l.setBlock(soil,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
            Block plant=new Block[]{Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES}[p.getRandom().nextInt(3)];
            l.setBlock(crop,plant.defaultBlockState(),3);f.gardenPlots++;FamilyData.get(l.getServer()).setDirty();
            p.getNavigation().moveTo(crop.getX()+.5,crop.getY(),crop.getZ()+.5,.8);return;
        }
    }
}
