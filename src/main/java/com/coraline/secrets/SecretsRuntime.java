package com.coraline.secrets;
import com.coraline.feast.*;
import com.coraline.otherworld.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid="coraline")
public final class SecretsRuntime {
    public static boolean active(ServerPlayer p){var s=OtherMotherLoop.active(p);return s!=null&&OtherWorld.isOther(p.level())&&!p.getPersistentData().getBoolean("CoralineInTunnel")&&(s.loop.phase==OtherMotherLoopState.Phase.WINDOW||s.loop.phase==OtherMotherLoopState.Phase.EXPLORING);}
    public static boolean exitBlocked(ServerPlayer p){var s=SecretTransactions.state(p);if(s!=null&&active(p)&&s.searching()){p.displayClientMessage(Component.literal("Other Mother is looking for you. Ring the bell on the front porch to let her know where you are."),true);return true;}return false;}
    public static boolean atJournal(ServerPlayer p){var s=SecretTransactions.state(p);var at=s==null?null:SecretWorld.hiding(p,s.hiding);return active(p)&&at!=null&&p.distanceToSqr(at.getCenter())<36;}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent e){if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(e.player instanceof ServerPlayer p) )return;if(p.tickCount%20==0)for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var tag=p.getInventory().getItem(slot).getTag();if(tag!=null&&tag.hasUUID("SecretRewardOwner")&&p.getUUID().equals(tag.getUUID("SecretRewardOwner")))tag.remove("SecretRewardOwner");}SecretAbilities.tick(p);JournalPlacement.tick(p);Reconciliation.hold(p);if(!active(p))return;var s=SecretTransactions.state(p);SecretWorld.begin(p);SecretDialogue.tick(p);if(Reconciliation.active(p))return;MotherSearch.tick(p);SecretSpies.tick(p);SecretCat.tick(p);
        s.inspectionProgress+=1/(SecretsState.inspectionMinutes(OtherMotherLoop.scene(p).loop.suspicion)*1200);
        if(s.inspectionProgress>=1&&!s.searching()&&!SecretDialogue.busy(p)){s.inspectionProgress=0;inspect(p,-1);}
        if(p.tickCount%5==0){
            int room=SecretWorld.roomAt(s,p.blockPosition());if(room>=0&&SecretWorld.rooms(s).get(room).getInt("Tier")>0&&!s.unlocked.contains(room)){var at=BlockPos.of(SecretWorld.rooms(s).get(room).getLong("Pos")).north(7);p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());p.displayClientMessage(Component.literal("The seal holds. Decode the lock at the entrance."),true);}
            for(var node:SecretWorld.nodes(s)){var at=BlockPos.of(node.getLong("Pos"));String kind=node.getString("Kind");double dist=p.distanceToSqr(at.getCenter());
                boolean show=kind.equals("cache")&&s.value(1)==100||kind.equals("drawer")&&s.value(3)>=33&&node.getString("Id").equals(Integer.toString(s.world.getInt("KeyDrawer")))||kind.equals("lock")&&s.ability("passages");
                if(show&&dist<256)p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,at.getX()+.5,at.getY()+1,at.getZ()+.5,1,.1,.1,.1,0);
                if(kind.equals("trap")&&dist<1.8&&p.getRandom().nextDouble()<.02+s.counter(3)*.15&&s.world.getLong("TrapUntil")<OtherMotherLoop.now(p)){trap(p,at,Integer.parseInt(node.getString("Id")));}
            }
            if(s.world.getLong("SnaredUntil")>OtherMotherLoop.now(p)){p.setDeltaMovement(0,p.getDeltaMovement().y,0);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,10,5,false,false));if(p.isShiftKeyDown())s.world.putLong("SnaredUntil",Math.max(0,s.world.getLong("SnaredUntil")-10));}
        }
        if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    public static void inspect(ServerPlayer p,int forced){var s=SecretTransactions.state(p);int spot=forced>=0?forced:p.getRandom().nextInt(8);if(spot>=6)spot=0; // Open desk is checked three times as often.
        if(spot==s.hiding)SecretTransactions.discoverJournal(p);if(s.world.getBoolean("KeyMissing")||s.world.getBoolean("AtticOpen")){var l=OtherMotherLoop.scene(p).loop;OtherMotherLoop.relationship(p,l.trust,l.suspicion+.025F);s.suspected.add("personal_keyring");}SecretTransactions.dirty(p);
    }
    public static void trap(ServerPlayer p,BlockPos at,int type){if(Reconciliation.active(p))return;var s=SecretTransactions.state(p);s.world.putLong("TrapUntil",OtherMotherLoop.now(p)+200);if(type==0){s.world.putLong("SnaredUntil",OtherMotherLoop.now(p)+80);p.displayClientMessage(Component.literal("Caught in a snare — hold Sneak to free yourself."),true);}else if(type==2){SecretSpies.rat(p,at.east(2));SecretSpies.rat(p,at.west(2));}MotherSearch.noise(p,at,70,"trap");SecretTransactions.dirty(p);}
    public static void ability(ServerPlayer p,String id){var s=SecretTransactions.state(p);long now=OtherMotherLoop.now(p);if(!active(p)||!s.ability(id)||s.cooldowns.getOrDefault(id,0L)>now)return;
        boolean success=true;switch(id){
            case "invisibility"->s.invisibleUntil=now+1200;
            case "false_signal"->{s.falseUntil=now+600;s.target=p.blockPosition();}
            case "combat"->s.combatUntil=now+600;
            case "gift"->{String claim="loop_gift";if(!s.claimed.add(claim))return;SecretTransactions.checkpoint(p);SecretTransactions.give(p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,6));}
            case "servant","command"->{var target=BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(10)));int count=0;for(var rat:p.serverLevel().getEntitiesOfClass(MouseEntity.class,p.getBoundingBox().inflate(24),r->r.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(r.getPersistentData().getUUID("SecretOwner")))){rat.getPersistentData().putLong("CommandUntil",now+300);rat.getPersistentData().putLong("CommandTarget",target.asLong());rat.getPersistentData().putBoolean("Alerted",false);if(++count>=(id.equals("command")?4:1))break;}success=count>0;}
            case "reveal","hijack","unweave"->success=SecretAbilities.activate(p,id);
            default->success=false;
        }if(success){s.cooldowns.put(id,now+(id.equals("invisibility")?4800:1200));p.playNotifySound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.6F,1.2F);SecretTransactions.dirty(p);}
    }
    @SubscribeEvent public static void pickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent e){var tag=e.getItem().getItem().getTag();if(tag!=null&&tag.hasUUID("SecretOwner")&&!e.getEntity().getUUID().equals(tag.getUUID("SecretOwner")))e.setCanceled(true);if(tag!=null&&tag.hasUUID("SecretRewardOwner")){if(!e.getEntity().getUUID().equals(tag.getUUID("SecretRewardOwner")))e.setCanceled(true);}}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){SecretAbilities.end(p);JournalPlacement.cancel(p);Reconciliation.suspend(p);}SecretLocks.clear(e.getEntity().getUUID());SecretsNetwork.clear(e.getEntity().getUUID());}
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e){if(e.getSource().getEntity() instanceof ServerPlayer p&&e.getEntity() instanceof com.coraline.parents.ParentEntity m&&m.isOtherParent()&&m.isMom()){var s=SecretTransactions.state(p);if(s!=null)e.setAmount(e.getAmount()*(float)(1+.5*s.effective(4)+(s.ability("combat")&&s.combatUntil>OtherMotherLoop.now(p)?.35:0)));}if(e.getEntity() instanceof ServerPlayer p&&e.getSource().getEntity() instanceof com.coraline.parents.ParentEntity m&&m.isOtherParent()&&m.isMom()){var s=SecretTransactions.state(p);if(s!=null)e.setAmount(e.getAmount()*(float)(1-.35*s.effective(4)-s.soulWeakening()));}}
    private SecretsRuntime(){}
}
