package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner/operator scoped, changed-value sync. No hidden numeric state is sent
 * to ordinary players. Never creates entities for labels. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonFoodSync {
    private static final Map<UUID,CompoundTag> LAST=new HashMap<>();
    @SubscribeEvent public static void player(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p)send(p,false);}
    public static void send(ServerPlayer p,boolean force){var visit=TycoonCollaborationVisits.visitingOwner(p);if(visit.isPresent()&&!visit.get().equals(TycoonWorld.owner(p))){TycoonScope.run(p,visit.get(),()->send(p,force));return;}var h=TycoonData.get(p.server).homes.get(TycoonWorld.owner(p));if(h==null||!h.active()||p.connection==null)return;
        boolean cake=TycoonFoodContent.index(p.getMainHandItem())==7&&TycoonWorld.owned(p,p.getMainHandItem())&&p.serverLevel().dimension()==TycoonWorld.KEY;
        boolean debug=p.hasPermissions(2)&&h.state.getBoolean("DebugFoodStats");if(!cake&&!debug&&!LAST.containsKey(p.getUUID()))return;
        var n=new CompoundTag();n.putString("Mode","food_state");n.putBoolean("CakeHeld",cake);n.putBoolean("Debug",debug);var rows=new ListTag();var names=new ListTag();
        int count=0;for(var r:h.residents.values())if(r.active()&&r.n.hasUUID("Entity")&&p.serverLevel().getEntity(r.n.getUUID("Entity")) instanceof TycoonNpc a&&TycoonWorld.owner(p).equals(a.owner)){boolean eligible=cake&&TycoonWelcomeCake.eligible(p,r);if(eligible)names.add(StringTag.valueOf(r.name()));if(p.distanceToSqr(a)>=48*48||count++>=48||!eligible&&!debug)continue;var row=new CompoundTag();row.putInt("Entity",a.getId());row.putUUID("UUID",a.getUUID());row.putString("Name",r.name());row.putBoolean("Eligible",eligible);row.putBoolean("Personal",a.level().dimension()==TycoonWorld.KEY);
            if(debug){row.putDouble("Hunger",r.need("Hunger"));row.putDouble("Neglect",r.need("Neglect"));row.putDouble("Trustness",TycoonDialogue.trust(r));row.putDouble("Suspicion",TycoonDialogue.suspicion(r));}rows.add(row);
        }n.put("Rows",rows);n.put("CakeNames",names);if(force||!n.equals(LAST.get(p.getUUID()))){TycoonNetwork.snapshot(p,n);if(cake||debug)LAST.put(p.getUUID(),n.copy());else LAST.remove(p.getUUID());}
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){LAST.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){LAST.clear();}
    private TycoonFoodSync(){}
}
