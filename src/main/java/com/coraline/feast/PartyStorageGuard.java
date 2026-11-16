package com.coraline.feast;

import com.coraline.secrets.HouseCabinets;
import com.coraline.secrets.HouseStorageBlock;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server inventory-layer protection, independent of witnesses and of whether the owner is online. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PartyStorageGuard {
    public interface GuardedContainer {}
    public interface GuardedCapabilities {}
    public interface GuardedHandler {}
    public interface GuardedPackets {}
    public interface GuardedWorld {}
    // Match the accessor's exact return descriptor. A covariant override would
    // make javac emit an annotated default bridge that Mixin cannot apply.
    public interface SidedInventory {WorldlyContainer coraline$inventory();}
    public interface CompoundParts {Container coraline$first();Container coraline$second();}
    private static final Map<Object,WeakReference<BlockEntity>> HANDLERS=Collections.synchronizedMap(new IdentityHashMap<>());
    public static boolean protectedAt(Level level,BlockPos pos){
        if(!(level instanceof ServerLevel server))return false;
        for(var s:VisitData.get(server.getServer()).scenes.values()){
            var n=HouseParty.data(s);if(!PlayerParty.isPlayerParty(s)||!n.getBoolean("Pending")||!n.contains("House")||!level.dimension().location().toString().equals(n.getString("HouseDimension")))continue;
            var house=BlockPos.of(n.getLong("House"));for(var room:HouseCabinets.ROOMS)if(PartyLayoutRules.protectedRoom(room.id())&&HouseCabinets.contains(house,room,pos))return true;
        }return false;
    }
    public static boolean protects(Object container){
        if(container instanceof BlockEntity be)return be.getLevel()!=null&&protectedAt(be.getLevel(),be.getBlockPos());
        if(container instanceof net.minecraftforge.items.wrapper.InvWrapper wrapper)return protects(wrapper.getInv());
        if(container instanceof SidedInventory wrapper)return protects(wrapper.coraline$inventory());
        if(container instanceof CompoundParts pair)return protects(pair.coraline$first())||protects(pair.coraline$second());
        var ref=HANDLERS.get(container);var be=ref==null?null:ref.get();return be!=null&&!be.isRemoved()&&protects(be);
    }
    public static void track(BlockEntity be,IItemHandler handler){if(handler!=null&&be.getLevel()!=null&&protectedAt(be.getLevel(),be.getBlockPos()))HANDLERS.put(handler,new WeakReference<>(be));}
    public static boolean blocksMenu(ServerPlayer p,AbstractContainerMenu menu){
        if(menu==p.inventoryMenu)return false;
        for(var slot:menu.slots)if(protects(slot.container)){warning(p,slot.container);return true;}
        return false;
    }
    private static void warning(ServerPlayer p,Object container){
        if(container instanceof BlockEntity be){PlayerParty.storageWarning(p,be.getBlockPos());return;}
        if(container instanceof CompoundParts pair){warning(p,protects(pair.coraline$first())?pair.coraline$first():pair.coraline$second());return;}
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Other Mother asked you not to take anything from here."),true);
    }
    public static boolean protectedBlock(Level level,BlockPos at){return protectedAt(level,at)&&(level.getBlockEntity(at)!=null||level.getBlockState(at).getBlock() instanceof HouseStorageBlock);}
    /** Read-only audit primes already-cached capability objects and refuses uninstrumented providers. */
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!((Object)p.connection instanceof GuardedPackets)||!((Object)p.serverLevel() instanceof GuardedWorld)){HouseParty.data(s).putString("StorageBlocker","Required party inventory mixins are not installed");return false;}
        var n=HouseParty.data(s);var house=BlockPos.of(n.getLong("House"));
        for(var room:HouseCabinets.ROOMS)if(PartyLayoutRules.protectedRoom(room.id())){
            var a=HouseCabinets.at(house,room.min()[0],room.min()[1],room.min()[2]);var b=HouseCabinets.at(house,room.max()[0],room.max()[1],room.max()[2]);
            for(var at:BlockPos.betweenClosed(a,b)){
                if(!p.serverLevel().hasChunkAt(at)){n.putString("StorageBlocker","Protected storage chunk is not loaded");return false;}
                var be=p.serverLevel().getBlockEntity(at);if(be==null)continue;
                if(be instanceof Container&&(!(be instanceof GuardedContainer)||!be.getClass().getName().startsWith("net.minecraft.world.level.block.entity."))){n.putString("StorageBlocker","Uninstrumented container: "+be.getClass().getName());return false;}
                for(int side=-1;side<6;side++){var cap=be.getCapability(ForgeCapabilities.ITEM_HANDLER,side<0?null:Direction.values()[side]);var handler=cap.orElse(null);if(handler!=null){track(be,handler);if(!(be instanceof GuardedCapabilities)||!(handler instanceof GuardedHandler)||!supportedHandler(handler)){n.putString("StorageBlocker","Uninstrumented item handler: "+handler.getClass().getName());return false;}}}
            }
        }
        for(var player:p.server.getPlayerList().getPlayers())if(blocksMenu(player,player.containerMenu))player.closeContainer();
        n.remove("StorageBlocker");return true;
    }
    /** Subclasses can override injected methods. Unknown providers must be reviewed before allowing this event. */
    private static boolean supportedHandler(IItemHandler handler){return Set.of("net.minecraftforge.items.wrapper.InvWrapper","net.minecraftforge.items.wrapper.SidedInvWrapper","net.minecraftforge.items.ItemStackHandler").contains(handler.getClass().getName());}
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!protectedBlock(p.level(),e.getPos())||p.level().getBlockState(e.getPos()).getBlock() instanceof HouseStorageBlock)return;
        // Custom cupboards retain visual/read-only inspection; other containers cannot open extraction UIs.
        PlayerParty.storageWarning(p,e.getPos());e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
    }
    @SubscribeEvent public static void placeContainer(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof Level level&&protectedAt(level,e.getPos())&&e.getPlacedBlock().hasBlockEntity())e.setCanceled(true);}
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent e){if(e.getLevel() instanceof Level level&&protectedBlock(level,e.getPos())){e.setCanceled(true);if(e.getPlayer() instanceof ServerPlayer p)PlayerParty.storageWarning(p,e.getPos());}}
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e){e.getAffectedBlocks().removeIf(at->protectedBlock(e.getLevel(),at));}
    @SubscribeEvent public static void piston(PistonEvent.Pre e){if(!(e.getLevel() instanceof Level level))return;var resolver=e.getStructureHelper();if(resolver!=null&&resolver.resolve()&&(resolver.getToPush().stream().anyMatch(at->protectedBlock(level,at))||resolver.getToDestroy().stream().anyMatch(at->protectedBlock(level,at))))e.setCanceled(true);}
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load e){if(!(e.getLevel() instanceof ServerLevel level)||!(e.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk))return;for(var be:chunk.getBlockEntities().values())if(protectedAt(level,be.getBlockPos()))for(int side=-1;side<6;side++)be.getCapability(ForgeCapabilities.ITEM_HANDLER,side<0?null:Direction.values()[side]).ifPresent(handler->track(be,handler));}
    public static void release(){HANDLERS.entrySet().removeIf(e->{var be=e.getValue().get();return be==null||be.isRemoved()||!protects(be);});}
    private PartyStorageGuard(){}
}
