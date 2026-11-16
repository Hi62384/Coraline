package com.coraline.feast;
import com.coraline.registry.ModItems;
import com.coraline.secrets.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.Mob;
import java.util.*;

/** Owner-persisted presents survive the party ending; item NBT never supplies the loot. */
public final class PartyCelebration {
    public static final String BOOK="CoralineHousePartyPresents";
    private static CompoundTag book(ServerPlayer p){if(!p.getPersistentData().contains(BOOK))p.getPersistentData().put(BOOK,new CompoundTag());return p.getPersistentData().getCompound(BOOK);}
    static boolean tick(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);var chair=PartyBanquet.playerChair(p,s);if(chair==null)return false;
        if(!n.getBoolean("CakeAnnounced")){
            n.putBoolean("CakeAnnounced",true);VisitAudio.caption(p,"Other Mother","Our guest of honour! Take your place by the cake, make a wish, and blow out the candles.",140);
            var cake=VisitEvents.prop(p,s,ScenePropEntity.CAKE,PartyBanquet.celebrationCake(p,s),3950);cake.getPersistentData().putBoolean("HousePartyCake",true);cake.setGlowingTag(true);
            for(var g:PartyGuests.guests(s))if(PartyGuests.entity(p,g)!=null&&!Set.of("GONE","LEAVING").contains(g.getString("State")))g.putBoolean("CelebrationAttendee",true);
        }
        for(var g:PartyGuests.guests(s))if(g.getBoolean("CelebrationAttendee")){var actor=PartyGuests.entity(p,g);if(actor!=null){actor.getLookControl().setLookAt(p,30,25);if(actor.tickCount%80==g.getInt("Order")%80)PartyGuests.gesture(actor,7,45);}}
        if(!n.getBoolean("CandlesOut")){if(p.tickCount%80==0)p.displayClientMessage(Component.literal(p.getVehicle()==chair?"Right-click your cake to make a wish, then blow out the candles.":"Take your reserved seat by the cake for your celebration."),true);return false;}
        var pending=PartyGuests.guests(s).stream().filter(g->g.getBoolean("CelebrationAttendee")&&!g.getBoolean("PresentGiven")).findFirst();
        if(pending.isEmpty())return n.getLong("Clock")>=n.getLong("NextPresent")+80;
        if(n.getLong("Clock")<n.getLong("NextPresent"))return false;
        var g=pending.get();UUID id=g.getUUID("Id");
        if(!book(p).contains(id.toString())){var gift=new CompoundTag();gift.putUUID("Owner",p.getUUID());gift.putString("From",g.getString("Name"));Item[] loot={Items.COOKIE,Items.HONEY_BOTTLE,Items.AMETHYST_SHARD,Items.GLOW_BERRIES,Items.IRON_NUGGET};gift.put("Payload",new ItemStack(loot[Math.floorMod(id.hashCode(),loot.length)],1+Math.floorMod(id.hashCode(),3)).save(new CompoundTag()));book(p).put(id.toString(),gift);}
        g.putBoolean("PresentGiven",true);var actor=PartyGuests.entity(p,g);if(actor!=null)PartyGuests.gesture(actor,6,30);
        VisitAudio.caption(p,g.getString("Name"),"I'll leave your present in the supper room when I go. Right-click it to collect it.",60);
        n.putLong("NextPresent",n.getLong("Clock")+16);OtherMotherLoop.changed(p);return false;
    }
    static boolean cake(ServerPlayer p,ScenePropEntity prop){
        if(!prop.getPersistentData().getBoolean("HousePartyCake"))return false;var s=HouseParty.active(p);
        if(s==null||!p.getUUID().equals(prop.owner)||!Objects.equals(s.door,prop.storyDoor)||prop.getPersistentData().getInt("VisitEventSerial")!=s.loop.event.getInt("Serial")||prop.distanceToSqr(p)>16||s.loop.secrets.searching())return true;
        var n=HouseParty.data(s);if(p.getVehicle()!=PartyBanquet.playerChair(p,s)){p.displayClientMessage(Component.literal("Take your seat by the cake first."),true);return true;}
        if(!n.getBoolean("CakeWish")){n.putBoolean("CakeWish",true);VisitAudio.caption(p,"Other Father","Make a wish, kiddo. Ready? Click the cake again and blow!",100);}
        else if(!n.getBoolean("CandlesOut")){n.putBoolean("CandlesOut",true);prop.setGlowingTag(false);p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,prop.getX(),prop.getY()+.5,prop.getZ(),12,.4,.15,.4,.01);VisitAudio.caption(p,"Your guests","Hooray! This evening is for you!",100);for(var g:PartyGuests.guests(s)){var actor=PartyGuests.entity(p,g);if(actor!=null)PartyGuests.gesture(actor,7,90);}}
        OtherMotherLoop.changed(p);return true;
    }
    public static void deliver(ServerPlayer p){
        // Re-create only earned, placed presents in a loaded chunk. Never flood inventory.
        for(String id:book(p).getAllKeys()){
            var receipt=book(p).getCompound(id);if(receipt.getBoolean("Delivered")||receipt.getBoolean("Opened")||!receipt.contains("Placed")||!receipt.hasUUID("Owner")||!p.getUUID().equals(receipt.getUUID("Owner")))continue;
            if(!p.level().dimension().location().toString().equals(receipt.getString("Dimension")))continue;
            var at=net.minecraft.core.BlockPos.of(receipt.getLong("Placed"));if(!p.serverLevel().hasChunkAt(at))continue;
            UUID token;try{token=UUID.fromString(id);}catch(IllegalArgumentException ex){continue;}
            var nearby=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(at).inflate(2),e->e.getPersistentData().hasUUID("HousePartyPresent")&&token.equals(e.getPersistentData().getUUID("HousePartyPresent"))&&p.getUUID().equals(e.owner));
            if(!nearby.isEmpty())continue;
            var prop=com.coraline.registry.ModEntities.SCENE_PROP.get().create(p.serverLevel());if(prop==null)continue;
            prop.owner=p.getUUID();prop.kind(ScenePropEntity.GIFT);prop.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(at));prop.getPersistentData().putUUID("HousePartyPresent",token);
            prop.setCustomName(Component.literal("Present from "+receipt.getString("From")+" — right-click to collect"));prop.setCustomNameVisible(true);prop.setGlowingTag(true);p.serverLevel().addFreshEntity(prop);
        }
    }
    static boolean leavePresent(ServerPlayer p,VisitData.Scene s,CompoundTag guest,Mob actor){
        if(!guest.getBoolean("PresentGiven")||!guest.hasUUID("Id"))return true;
        var receipt=book(p).getCompound(guest.getUUID("Id").toString());
        if(receipt.isEmpty()||receipt.getBoolean("Delivered")||receipt.contains("Placed"))return true;
        var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return false;
        var room=PartyRegions.rooms().stream().filter(r->r.id().equals("supper_room")).findFirst().orElse(null);if(room==null)return false;
        net.minecraft.world.phys.Vec3 spot=null;
        for(int z=room.min()[2]+1;z<room.max()[2]&&spot==null;z+=2)for(int x=room.min()[0]+1;x<room.max()[0]&&spot==null;x+=2){
            var at=HouseCabinets.at(realm.house,x,room.floor()+1,z);var feet=PartyFloor.at(p,at);if(feet==null)continue;
            if(!p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(feet,feet).inflate(.65,1,.65)).isEmpty())continue;
            if(actor!=null){var path=actor.getNavigation().createPath(net.minecraft.core.BlockPos.containing(feet),1);if(path==null||!path.canReach())continue;}
            spot=feet;
        }
        if(spot==null)return false;
        if(actor!=null&&actor.distanceToSqr(spot)>4){
            int waiting=guest.getInt("PresentWalkTicks")+1;guest.putInt("PresentWalkTicks",waiting);
            if(actor.tickCount%20==0)actor.getNavigation().moveTo(spot.x,spot.y,spot.z,.95);
            // A failed route cannot strand the party: retain the earned gift at the safe room spot.
            if(waiting<400)return false;
        }
        receipt.putLong("Placed",net.minecraft.core.BlockPos.containing(spot).asLong());receipt.putString("Dimension",p.level().dimension().location().toString());
        if(actor!=null)PartyGuests.gesture(actor,6,25);deliver(p);OtherMotherLoop.changed(p);return true;
    }
    static boolean presentTick(ScenePropEntity prop){
        if(!prop.getPersistentData().hasUUID("HousePartyPresent"))return false;
        var p=prop.owner==null?null:prop.getServer().getPlayerList().getPlayer(prop.owner);
        if(p!=null){var receipt=book(p).getCompound(prop.getPersistentData().getUUID("HousePartyPresent").toString());if(receipt.getBoolean("Delivered")||receipt.getBoolean("Opened"))prop.discard();}
        return true;
    }
    static boolean pickup(ServerPlayer p,ScenePropEntity prop){
        if(!prop.getPersistentData().hasUUID("HousePartyPresent"))return false;
        if(!p.getUUID().equals(prop.owner)||!PartyMatches.withinReach(p,prop))return true;
        UUID id=prop.getPersistentData().getUUID("HousePartyPresent");var receipt=book(p).getCompound(id.toString());
        if(receipt.isEmpty()||!receipt.hasUUID("Owner")||!p.getUUID().equals(receipt.getUUID("Owner"))||receipt.getBoolean("Delivered")||receipt.getBoolean("Opened"))return true;
        var present=new ItemStack(ModItems.WRAPPED_PARTY_GIFT.get());present.getOrCreateTag().putUUID("Present",id);present.getOrCreateTag().putUUID("Recipient",p.getUUID());present.setHoverName(Component.literal("Wrapped gift from "+receipt.getString("From")));
        p.getInventory().add(present);if(!present.isEmpty()){p.displayClientMessage(Component.literal("Make one inventory space for this present; it will stay here."),true);return true;}
        receipt.putBoolean("Delivered",true);prop.discard();p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();return true;
    }
    public static boolean valid(ServerPlayer p,ItemStack item){if(!item.hasTag()||!item.getTag().hasUUID("Present")||!item.getTag().hasUUID("Recipient")||!p.getUUID().equals(item.getTag().getUUID("Recipient")))return false;var receipt=book(p).getCompound(item.getTag().getUUID("Present").toString());return receipt.getBoolean("Delivered")&&!receipt.getBoolean("Opened")&&p.getUUID().equals(receipt.getUUID("Owner"));}
    public static void unwrap(ServerPlayer p,ItemStack item){
        if(!valid(p,item))return;var receipt=book(p).getCompound(item.getTag().getUUID("Present").toString());var payload=ItemStack.of(receipt.getCompound("Payload"));receipt.putBoolean("Opened",true);item.shrink(1);p.getInventory().add(payload);
        if(!payload.isEmpty()){var drop=p.drop(payload,false);if(drop!=null){drop.setTarget(p.getUUID());drop.getPersistentData().putUUID("CoralineVisitGiftOwner",p.getUUID());}}
        p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE,net.minecraft.sounds.SoundSource.PLAYERS,.7F,1);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
    }
    private PartyCelebration(){}
}
