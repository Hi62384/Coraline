package com.coraline.secrets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;

/** A carried journal is presentation state, never a duplicate inventory item. */
public final class JournalPlacement {
    public static boolean carrying(ServerPlayer p){var s=SecretTransactions.state(p);return s!=null&&s.world.getBoolean("MovingJournal");}
    public static boolean bedroom(ServerPlayer p){var r=SecretWorld.realm(p);if(r==null||r.house==null||!com.coraline.otherworld.OtherWorld.isOther(p.level()))return false;var c=r.house;return p.getX()>=c.getX()+4&&p.getX()<c.getX()+18&&p.getZ()>=c.getZ()-28&&p.getZ()<c.getZ()-9&&p.getY()>=c.getY()+9&&p.getY()<c.getY()+15;}
    public static void begin(ServerPlayer p){if(!SecretsRuntime.atJournal(p)||!bedroom(p)||carrying(p))return;var s=SecretTransactions.state(p);s.world.putBoolean("MovingJournal",true);var token=java.util.UUID.randomUUID();s.world.putUUID("CarriedJournalToken",token);var book=SecretTransactions.bound(p,SecretsItems.JOURNAL.get(),token);if(p.getOffhandItem().isEmpty())p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,book);else SecretTransactions.give(p,book);SecretTransactions.checkpoint(p);}
    public static void cancel(ServerPlayer p){var s=SecretTransactions.state(p);if(s==null)return;s.world.remove("MovingJournal");if(s.world.hasUUID("CarriedJournalToken")){var token=s.world.getUUID("CarriedJournalToken");for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.is(SecretsItems.JOURNAL.get())&&SecretTransactions.owned(p,item)&&token.equals(item.getTag().getUUID("SecretToken")))p.getInventory().setItem(i,net.minecraft.world.item.ItemStack.EMPTY);}s.world.remove("CarriedJournalToken");}var at=SecretWorld.hiding(p,s.hiding);var level=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);if(at!=null&&level!=null&&s.world.hasUUID("JournalProp")){var prop=level.getEntity(s.world.getUUID("JournalProp"));if(prop!=null)prop.setPos(SecretWorld.journalDisplay(p,at));}SecretTransactions.dirty(p);}
    public static boolean place(ServerPlayer p,int slot){if(!carrying(p)||!bedroom(p)||slot<0||slot>=SecretWorld.HIDING.length)return false;var at=SecretWorld.hiding(p,slot);if(at==null||p.distanceToSqr(at.getCenter())>25)return false;var s=SecretTransactions.state(p);s.hiding=slot;cancel(p);SecretTransactions.checkpoint(p);return true;}
    public static boolean interact(ServerPlayer p,Entity e){var s=SecretTransactions.state(p);if(s==null||!SecretsRuntime.active(p)||!s.world.hasUUID("JournalHit")||!s.world.getUUID("JournalHit").equals(e.getUUID())||p.distanceToSqr(e)>25)return false;if(p.isShiftKeyDown())begin(p);else SecretsNetwork.open(p,false,true);return true;}
    public static void tick(ServerPlayer p){
        var s=SecretTransactions.state(p);if(s==null)return;
        if(carrying(p)&&(!bedroom(p)||!SecretsRuntime.active(p)||!p.isAlive())){cancel(p);p.displayClientMessage(Component.literal("Journal returned to its previous hiding place."),true);}
        if(!SecretsRuntime.active(p))return;
        var at=SecretWorld.hiding(p,s.hiding);if(at==null)return;
        Entity hit=s.world.hasUUID("JournalHit")?p.serverLevel().getEntity(s.world.getUUID("JournalHit")):null;
        if(carrying(p)){
            if(hit!=null)hit.discard();s.world.remove("JournalHit");
            if(s.world.hasUUID("JournalProp")){var prop=p.serverLevel().getEntity(s.world.getUUID("JournalProp"));if(prop!=null)prop.setPos(p.getEyePosition().add(p.getLookAngle().scale(.7)).add(0,-.35,0));}
            if(p.tickCount%8==0){for(int i=0;i<SecretWorld.HIDING.length;i++){var spot=SecretWorld.hiding(p,i);if(spot==null)continue;var ground=SecretWorld.journalGround(p,spot);for(int edge=0;edge<8;edge++){double angle=edge*Math.PI/4;p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,ground.x+Math.cos(angle)*.4,ground.y+.04,ground.z+Math.sin(angle)*.4,1,0,0,0,0);}}p.displayClientMessage(Component.literal("Place journal: right-click a highlighted spot • Stay in your bedroom"),true);}return;
        }
        if(hit==null){var e=net.minecraft.world.entity.EntityType.INTERACTION.create(p.serverLevel());if(e==null)return;var n=new net.minecraft.nbt.CompoundTag();n.putFloat("width",.85F);n.putFloat("height",.3F);n.putBoolean("response",true);e.load(n);e.getPersistentData().putBoolean("CoralineDecoration",true);e.getPersistentData().putUUID("SecretOwner",p.getUUID());e.setPos(SecretWorld.journalGround(p,at));p.serverLevel().addFreshEntity(e);s.world.putUUID("JournalHit",e.getUUID());hit=e;}
        hit.setPos(SecretWorld.journalGround(p,at));
    }
    private JournalPlacement(){}
}
