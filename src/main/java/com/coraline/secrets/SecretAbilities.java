package com.coraline.secrets;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import com.coraline.feast.OtherMotherLoop;

/** Timed effects are revalidated against the live journal, including during a remote view. */
public final class SecretAbilities {
    private record View(Entity camera,Vec3 body,long until){}
    private static final Map<UUID,View> VIEWS=new HashMap<>();
    public static boolean activate(ServerPlayer p,String id){var s=SecretTransactions.state(p);long now=OtherMotherLoop.now(p);
        switch(id){
            case "hijack"->{var spies=p.serverLevel().getEntitiesOfClass(Entity.class,p.getBoundingBox().inflate(64),e->e.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("SecretOwner"))&&!e.getPersistentData().getString("SpyKind").isEmpty());if(spies.isEmpty())return false;var camera=spies.get(0);VIEWS.put(p.getUUID(),new View(camera,p.position(),now+300));p.setCamera(camera);p.displayClientMessage(net.minecraft.network.chat.Component.literal("Borrowed eyes — sneak to return."),true);return true;}
            case "unweave"->{s.world.putLong("HijackUntil",now+600);return true;}
            case "reveal"->{s.world.putLong("RevealUntil",now+400);return true;}
            default->{return false;}
        }
    }
    public static void tick(ServerPlayer p){var s=SecretTransactions.state(p);if(s==null)return;long now=OtherMotherLoop.now(p);if(!s.ability("invisibility"))s.invisibleUntil=0;if(!s.ability("false_signal"))s.falseUntil=0;if(!s.ability("combat"))s.combatUntil=0;if(!s.ability("unweave"))s.world.putLong("HijackUntil",0);
        var view=VIEWS.get(p.getUUID());if(view!=null){if(!SecretsRuntime.active(p)||!s.ability("hijack")||now>=view.until||!view.camera.isAlive()||p.isShiftKeyDown()){end(p);}else{p.setDeltaMovement(Vec3.ZERO);if(p.position().distanceToSqr(view.body)>.04)p.teleportTo(p.serverLevel(),view.body.x,view.body.y,view.body.z,p.getYRot(),p.getXRot());}}
        if(!SecretsRuntime.active(p))return;
        if(s.invisibleUntil>now&&s.ability("invisibility"))p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,10,0,false,false));
        if(p.tickCount%20!=0)return;boolean debug=p.hasPermissions(2)&&s.world.getLong("DebugRevealUntil")>now;boolean reveal=debug||s.ability("reveal")&&s.world.getLong("RevealUntil")>now;
        for(var node:SecretWorld.nodes(s)){String kind=node.getString("Kind");var at=BlockPos.of(node.getLong("Pos"));boolean show=debug||(kind.equals("loot")&&s.ability("storage"))||((kind.equals("lock")||kind.equals("passage"))&&(s.ability("passages")||reveal))||(kind.equals("soul")&&s.ability("rescue"))||(kind.equals("soulglass")&&s.ability("soulglass"));if(show&&p.distanceToSqr(at.getCenter())<Math.pow(reveal?64:24,2))p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.ENCHANT,true,at.getX()+.5,at.getY()+1.2,at.getZ()+.5,7,.25,.25,.25,.01);}
        if(s.ability("surveillance")||reveal)for(var e:p.serverLevel().getEntitiesOfClass(Entity.class,p.getBoundingBox().inflate(24),e->e.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("SecretOwner"))&&!e.getPersistentData().getString("SpyKind").isEmpty()))p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.WITCH,true,e.getX(),e.getY()+.5,e.getZ(),4,.1,.1,.1,0);
        var m=MotherSearch.mother(p);if(m!=null&&s.ability("vulnerability")&&s.search==SecretsState.Search.RETURNING_HOME&&m.distanceToSqr(p)<24*24)p.displayClientMessage(net.minecraft.network.chat.Component.literal("Her attention has shifted. This is your chance to slip away."),true);
    }
    public static void end(ServerPlayer p){if(VIEWS.remove(p.getUUID())!=null)p.setCamera(p);}
    private SecretAbilities(){}
}
