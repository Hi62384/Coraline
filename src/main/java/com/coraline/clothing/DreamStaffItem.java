package com.coraline.clothing;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** A visible dream pulse that holds a struck mob in place for two seconds. */
@Mod.EventBusSubscriber(modid="coraline")
public final class DreamStaffItem extends Item {
    private static final DustParticleOptions SPARK=new DustParticleOptions(new Vector3f(.65F,.44F,1F),.55F);
    public DreamStaffItem(){super(new Item.Properties().durability(320));}
    @Override public InteractionResult useOn(UseOnContext context){return context.getPlayer()==null?InteractionResult.FAIL:use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){return use(player.level(),player,hand).getResult();}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
        if(world instanceof ServerLevel level){
            Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(24));
            var block=world.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(block.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)end=block.getLocation();
            var hit=ProjectileUtil.getEntityHitResult(world,player,start,end,player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),e->e instanceof Mob&&e.isPickable());
            if(hit!=null&&hit.getEntity() instanceof Mob mob){
                if(!mob.getPersistentData().contains("CoralineDreamStaffUntil"))mob.getPersistentData().putBoolean("CoralineDreamStaffHadNoAi",mob.isNoAi());
                mob.getPersistentData().putLong("CoralineDreamStaffUntil",level.getGameTime()+40);mob.setNoAi(true);
                end=hit.getLocation();
            }
            Vec3 delta=end.subtract(start);int steps=Math.max(1,(int)(delta.length()*5));
            for(int i=0;i<=steps;i++){Vec3 pos=start.add(delta.scale(i/(double)steps));level.sendParticles(SPARK,pos.x,pos.y,pos.z,1,0,0,0,0);}
            if(!player.getAbilities().instabuild)stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this,100);player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
    @SubscribeEvent public static void unstun(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide)return;
        var data=mob.getPersistentData();
        if(data.contains("CoralineDreamStaffUntil")&&mob.level().getGameTime()>=data.getLong("CoralineDreamStaffUntil")){
            mob.setNoAi(data.getBoolean("CoralineDreamStaffHadNoAi"));
            data.remove("CoralineDreamStaffUntil");data.remove("CoralineDreamStaffHadNoAi");
        }
    }
}
