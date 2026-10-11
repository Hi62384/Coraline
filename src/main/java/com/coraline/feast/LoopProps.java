package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** Bounded, owner-only decorative displays. The saved reward ledger prevents reload duplication. */
public final class LoopProps {
    private LoopProps(){}
    public static boolean owned(ScenePropEntity prop){return prop.getPersistentData().contains("LoopGeneration");}
    public static boolean valid(ScenePropEntity prop){
        var s=prop.owner==null?null:VisitData.get(prop.getServer()).scenes.get(prop.owner);
        if(s!=null&&prop.kind()==ScenePropEntity.ACTIVITY_PORTAL&&(!(s.loop.phase==WINDOW||s.loop.phase==ARRIVAL||s.loop.phase==READY||s.loop.phase==ACTIVITY)||!prop.blockPosition().equals(s.loop.portal)))return false;
        if(s!=null&&prop.kind()==ScenePropEntity.ACTIVITY_EXIT&&!ActivityWorld.exitReady(s))return false;
        return s!=null&&s.loop.active()&&Objects.equals(s.door,prop.storyDoor)&&prop.getPersistentData().getInt("LoopGeneration")==s.loop.generation
            &&(OtherWorld.isOther(prop.level())||ActivityWorld.isActivity(prop.level())&&s.loop.phase==ACTIVITY);
    }
    static ScenePropEntity spawn(ServerLevel level,VisitData.Scene s,int kind,Vec3 pos,int slot){
        var e=ModEntities.SCENE_PROP.get().create(level);e.owner=s.owner;e.storyDoor=s.door;e.kind(kind);e.slot=slot;
        e.getPersistentData().putInt("LoopGeneration",s.loop.generation);e.setPos(pos);level.getChunkAt(e.blockPosition());level.addFreshEntity(e);return e;
    }
    static List<ScenePropEntity> nearby(ServerLevel level,VisitData.Scene s,BlockPos at){
        return level.getEntitiesOfClass(ScenePropEntity.class,new AABB(at).inflate(8),e->owned(e)&&s.owner.equals(e.owner)&&s.door.equals(e.storyDoor));
    }
    static void hold(ServerLevel level,BlockPos pos){level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(pos),2,pos);level.getChunkAt(pos);}
    static boolean prepareGifts(ServerPlayer p,VisitData.Scene s){
        if(s.loop.giftPrepared)return true;
        var r=RealmData.get(p.server).realms.get(s.door);if(r==null)return false;
        if(r.table!=null)s.table=r.table;
        else {
            var probe=ModEntities.MOM.get().create(p.serverLevel());if(probe==null)return false;
            probe.setPos(p.position());probe.setOnGround(true);ScenePlacement.prepareWalker(probe);
            var table=ScenePlacement.find(p.serverLevel(),p.blockPosition(),3,24,false,probe,null);
            if(table==null){var terrace=ScenePlacement.terrace(p.serverLevel(),r.doorPos,r.radius);if(terrace==null)return false;table=terrace[0];}
            s.table=table;
        }
        s.loop.gifts.clear();
        s.loop.gifts.addAll(com.coraline.secrets.SecretTransactions.gifts(p,VisitGiftPools.generate(s.loop.loot,p.getRandom(),VisitGiftPools.progress(p,s))));
        s.loop.giftPrepared=true;
        OtherMotherLoop.changed(p);return true;
    }
    static boolean choosePortal(ServerPlayer p,VisitData.Scene s){
        var r=RealmData.get(p.server).realms.get(s.door);if(r==null||r.house==null)return false;
        BlockPos next=null;var level=p.serverLevel();
        // A portal needs a clear standing space, not the old piano-sized footprint
        // and a path from a temporary NPC standing at the player's location.
        for(int distance=55;distance<=85&&next==null;distance+=2)
            for(int side:new int[]{-1,1})for(int z=-24;z<=24&&next==null;z+=3){
                var column=r.house.offset(side*distance,0,z);
                if(!level.hasChunkAt(column))continue;
                var at=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column);
                if(portalFits(level,at))next=at;
            }
        if(next==null){
            OtherWorldEstate.lockStudy(level,r.house,false);
            for(int x=-13;x<=-8&&next==null;x++)for(int z=-5;z<=-3&&next==null;z++){
                var at=r.house.offset(x,1,z);if(portalFits(level,at))next=at;
            }
            // The study entrance is a reserved, unfurnished passage in the estate.
            // An entity portal does not replace blocks or destroy stored items.
            if(next==null)next=r.house.offset(-11,1,-3);
        }
        removePortals(p.server,s);s.loop.portal=next;OtherMotherLoop.changed(p);return true;
    }
    static boolean portalFits(ServerLevel level,BlockPos at){
        var box=new AABB(at.getX()+.05,at.getY(),at.getZ()+.05,at.getX()+.95,at.getY()+2.8,at.getZ()+.95);
        return level.getWorldBorder().isWithinBounds(box)
            &&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP)
            &&!level.getBlockCollisions(null,box).iterator().hasNext()&&!level.containsAnyLiquid(box);
    }
    /** Prefer distant side lawns; the study is the explicit emergency fallback. */
    public static boolean portalSite(BlockPos house,BlockPos at){
        int x=Math.abs(at.getX()-house.getX()),z=at.getZ()-house.getZ();
        return x>=55&&x<=85&&z>=-24&&z<=24||at.getX()>=house.getX()-13&&at.getX()<=house.getX()-8
            &&z>=-5&&z<=-3&&at.getY()==house.getY()+1;
    }
    static void ensure(ServerPlayer p,VisitData.Scene s){
        var level=p.serverLevel();var l=s.loop;
        if(l.phase==GIFTS){
            hold(level,s.table);var props=nearby(level,s,s.table);Vec3 base=Vec3.atBottomCenterOf(s.table);
            if(props.stream().noneMatch(e->e.kind()==ScenePropEntity.TABLE))spawn(level,s,ScenePropEntity.TABLE,base,-1);
            if(l.loot!=OtherMotherDialogue.Loot.FOOD){
                var box=props.stream().filter(e->e.kind()==ScenePropEntity.GIFT).findFirst().orElseGet(()->spawn(level,s,ScenePropEntity.GIFT,base.add(0,.89,0),-1));
                box.mode(l.opened?1:0);box.treasure(l.loot==OtherMotherDialogue.Loot.MYSTERY&&!l.opened?List.of():l.gifts);
                if(l.loot.expanded()){box.setCustomName(net.minecraft.network.chat.Component.literal(l.opened?"Your "+l.loot.displayName+" present":"Right-click: "+(l.loot==OtherMotherDialogue.Loot.MYSTERY?"Open your surprise":"Open "+l.loot.displayName+" present")));box.setCustomNameVisible(true);}
                props.stream().filter(e->e.kind()==ScenePropEntity.PLATE||e.kind()==ScenePropEntity.GLASS).forEach(ScenePropEntity::discard);
                return;
            }
            // Food alone uses four themed platters and three individual glasses.
            int count=7;
            for(int slot=0;slot<count;slot++){
                int key=slot;boolean drink=l.loot==OtherMotherDialogue.Loot.FOOD&&slot>=4;
                var prop=props.stream().filter(e->e.slot==key&&(e.kind()==ScenePropEntity.PLATE||e.kind()==ScenePropEntity.GLASS)).findFirst().orElse(null);
                if(prop==null){double x=drink?(slot-5)*1.1:(slot%2==0?-.98:.98);double z=drink?.99:(slot<2?-.48:.4);prop=spawn(level,s,drink?ScenePropEntity.GLASS:ScenePropEntity.PLATE,base.add(x,.9,z),slot);}
                int offset=drink?12+slot-4:slot*3;
                for(int i=0;i<3;i++)prop.food(i,(!drink||i==0)&&offset+i<l.gifts.size()?l.gifts.get(offset+i):ItemStack.EMPTY);
            }
        }
        if(l.phase==WINDOW||l.phase==ARRIVAL||l.phase==READY){
            var realm=RealmData.get(p.server).realms.get(s.door);
            if(realm==null||realm.house==null)return;
            if(!portalSite(realm.house,l.portal)&&!choosePortal(p,s))return;
            hold(level,l.portal);
            if(nearby(level,s,l.portal).stream().noneMatch(e->e.kind()==ScenePropEntity.ACTIVITY_PORTAL))spawn(level,s,ScenePropEntity.ACTIVITY_PORTAL,Vec3.atBottomCenterOf(l.portal),-1).setGlowingTag(true);
            level.sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,l.portal.getX()+.5,l.portal.getY()+1.5,l.portal.getZ()+.5,5,.7,1,.12,.01);
        }
    }
    public static InteractionResult interact(ServerPlayer p,ScenePropEntity prop){
        var s=OtherMotherLoop.active(p);if(s==null||!valid(prop)||!p.getUUID().equals(prop.owner)||p.distanceToSqr(prop)>36)return InteractionResult.FAIL;
        var l=s.loop;
        if(prop.kind()==ScenePropEntity.ACTIVITY_PORTAL){if(l.phase==READY)ActivityWorld.enter(p,s);return InteractionResult.CONSUME;}
        if(prop.kind()==ScenePropEntity.ACTIVITY_EXIT){if(l.phase==ACTIVITY)ActivityWorld.exit(p,s);return InteractionResult.CONSUME;}
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.SHOPPING_MALL&&ShoppingMallActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.SNOWBALL_BATTLE&&SnowballBattleActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.TREASURE_HUNT&&TreasureHuntActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(prop.kind()==ScenePropEntity.CHAIR&&l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST){
            CakeContestActivity.sit(p,s,prop);return InteractionResult.CONSUME;
        }
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.GARDEN_MAZE&&GardenMazeActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.GRAND_FEAST&&GrandFeastActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.MAGIC_SHOW&&MagicShowActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.MAGIC_GARDEN&&MagicGardenActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.CARNIVAL&&CarnivalActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST&&CakeMaking.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.MICE_CIRCUS&&MiceCircusActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT&&OceanMonumentActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.MUSIC_FESTIVAL&&MusicFestivalActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE&&DesertTempleActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.OTHER_END_CITY&&EndCityActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS&&FortressActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase==ACTIVITY&&l.activity==OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION&&MansionActivity.interactProp(p,s,prop))return InteractionResult.CONSUME;
        if(l.phase!=GIFTS)return InteractionResult.CONSUME;
        if(prop.kind()==ScenePropEntity.GIFT){
            if(!l.opened){
                l.opened=true;prop.mode(1);
                if(l.loot.expanded()){
                    l.giftRevealUntil=OtherMotherLoop.now(p)+30;
                    p.playNotifySound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.65F,1.1F);
                    p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.ENCHANT,true,prop.getX(),prop.getY()+.5,prop.getZ(),18,.45,.3,.45,.05);
                }
                OtherMotherLoop.changed(p);ensure(p,s);return InteractionResult.SUCCESS;
            }
            if(l.loot.expanded()){finishGiftReveal(p,s);return InteractionResult.SUCCESS;}
            for(int i=0;i<l.gifts.size();i++)if(!l.gifts.get(i).isEmpty()){
                var remainder=l.gifts.get(i).copy();int before=remainder.getCount();p.getInventory().add(remainder);
                l.gifts.set(i,remainder);OtherMotherLoop.changed(p);ensure(p,s);
                if(remainder.getCount()==before)p.displayClientMessage(net.minecraft.network.chat.Component.literal("Make room in your inventory for your gifts."),true);
                break;
            }
            return InteractionResult.SUCCESS;
        }
        if(prop.kind()!=ScenePropEntity.PLATE&&prop.kind()!=ScenePropEntity.GLASS)return InteractionResult.CONSUME;
        if(l.loot!=OtherMotherDialogue.Loot.FOOD)return InteractionResult.FAIL;
        int start=prop.kind()==ScenePropEntity.GLASS?12+prop.slot-4:prop.slot*3;
        for(int i=start;i<Math.min(l.gifts.size(),start+(prop.kind()==ScenePropEntity.GLASS?1:3));i++){
            if(i<0||l.gifts.get(i).isEmpty())continue;
            // Inventory.add can partially accept a stack; retain its exact remainder.
            var remainder=l.gifts.get(i).copy();int before=remainder.getCount();p.getInventory().add(remainder);
            l.gifts.set(i,remainder);OtherMotherLoop.changed(p);ensure(p,s);
            if(remainder.getCount()==before)p.displayClientMessage(net.minecraft.network.chat.Component.literal("Make room in your inventory for your gifts."),true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
    static void finishGiftReveal(ServerPlayer p,VisitData.Scene s){
        var l=s.loop;if(l.phase!=GIFTS||!l.loot.expanded()||!l.opened||OtherMotherLoop.now(p)<l.giftRevealUntil||!s.owner.equals(p.getUUID())||OtherMotherLoop.active(p)!=s)return;
        for(int i=0;i<l.gifts.size();i++)if(!l.gifts.get(i).isEmpty()){
            var awarded=l.gifts.get(i).copy();var overflow=ActivityTokens.giveTracked(p,awarded);if(overflow!=null)overflow.getPersistentData().putUUID("CoralineVisitGiftOwner",p.getUUID());l.gifts.set(i,overflow!=null&&overflow.getPersistentData().getBoolean("CoralineAwardFailed")?overflow.getItem().copy():ItemStack.EMPTY);OtherMotherLoop.changed(p);
        }
        ensure(p,s);
    }
    static void removePortals(MinecraftServer server,VisitData.Scene s){remove(server,s,true);}
    static void clean(MinecraftServer server,VisitData.Scene s){remove(server,s,false);}
    private static void remove(MinecraftServer server,VisitData.Scene s,boolean portalsOnly){
        for(var key:List.of(OtherWorld.KEY,ActivityWorld.KEY)){var level=server.getLevel(key);if(level==null)continue;
            var found=new ArrayList<ScenePropEntity>();for(var e:level.getAllEntities())if(e instanceof ScenePropEntity prop&&owned(prop)&&s.owner.equals(prop.owner)&&(!portalsOnly||prop.kind()==ScenePropEntity.ACTIVITY_PORTAL||prop.kind()==ScenePropEntity.ACTIVITY_EXIT))found.add(prop);
            found.forEach(ScenePropEntity::discard);
        }
    }
}
