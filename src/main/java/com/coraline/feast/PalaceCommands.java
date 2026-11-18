package com.coraline.feast;

import com.coraline.palace.PalaceSiteData;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registration requires a live desk offer, not operator/debug progression.
 * Status is operator-only. Resume and leave act only on an existing owner-bound ride.
 */
@Mod.EventBusSubscriber(modid="coraline")
public final class PalaceCommands {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("coralinepalace")
            .then(Commands.literal("register").then(Commands.argument("offer",UuidArgument.uuid()).executes(c->PalaceTransport.register(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"offer")))))
            .then(Commands.literal("leave").executes(c->PalaceTransport.leave(c.getSource().getPlayerOrException())))
            .then(Commands.literal("resume").executes(c->PalaceTransport.resume(c.getSource().getPlayerOrException())))
            .then(Commands.literal("well-resume").executes(c->PalaceWellEncounter.resume(c.getSource().getPlayerOrException())))
            .then(Commands.literal("status").requires(c->c.hasPermission(2)).executes(c->{
                var d=PalaceSiteData.get(c.getSource().getServer());var saved=d.save(new net.minecraft.nbt.CompoundTag());var job=saved.getCompound("Generation");
                c.getSource().sendSuccess(()->Component.literal("Palace world job: "+job.getString("Stage")+" / "+job.getInt("Cursor")+". Ready="+d.ready()+". "+d.blocker()),false);
                for(var kind:com.coraline.palace.PalaceSitePlan.Kind.values())c.getSource().sendSuccess(()->Component.literal(kind+": "+d.position(kind).map(net.minecraft.core.BlockPos::toShortString).orElse("not reserved")+"; generated="+d.generated(kind)),false);
                var p=c.getSource().getPlayer();var scene=p==null?null:VisitData.get(p.server).scenes.get(p.getUUID());
                if(scene!=null){
                    var original=CanonicalRealWybie.identity(scene);c.getSource().sendSuccess(()->Component.literal("Original real Wybie: "+(original==null?"no canonical record (not proof of death)":original.blocker().isEmpty()?original.entity()+" / "+original.fate()+" / stored="+original.stored():"incompatible: "+original.blocker())),false);
                    var journey=PalaceJourney.existing(scene);if(journey!=null){
                        var ride=journey.getCompound("Transport");
                        if(!ride.isEmpty())c.getSource().sendSuccess(()->Component.literal("Ride: "+ride.getCompound("Progress").getString("Phase")+". "+ride.getString("Blocker")),false);
                        c.getSource().sendSuccess(()->Component.literal("Well: "+journey.getCompound("Well").getString("Phase")+". "+journey.getString("WellBlocker")+" "+journey.getCompound("WellStage").getString("Blocker")+" "+journey.getString("KeyCustodyBlocker")),false);
                    }
                }
                return 1;
            })));
    }
    private PalaceCommands(){}
}
