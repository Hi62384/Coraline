package com.coraline.otherworld;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid="coraline")
public final class RealmProtection {
    @SubscribeEvent public static void entityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent e){
        if(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level&&com.coraline.feast.ActivityWorld.protectedWorld(level)&&e.getEntity() instanceof com.coraline.parents.ParentEntity p&&p.isOtherParent()&&p.getPersistentData().hasUUID("RealmDoor")){
            var r=RealmData.get(level.getServer()).realms.get(p.getPersistentData().getUUID("RealmDoor"));
            if(r==null||p.getPersistentData().getInt("RealmGeneration")!=r.parentGeneration){e.setCanceled(true);return;}
        }
        if(!e.getLevel().isClientSide&&com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel())&&!e.getEntity().getPersistentData().getBoolean("CoralineDecoration")&&!(e.getEntity() instanceof ServerPlayer)&&!(e.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity)&&!(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getEntity() instanceof net.minecraft.world.entity.projectile.FishingHook)&&!(e.getEntity() instanceof com.coraline.parents.ParentEntity p&&p.isOtherParent()))e.setCanceled(true);
    }
    @SubscribeEvent public static void tunnelDamage(net.minecraftforge.event.entity.living.LivingAttackEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.feast.ActivityWorld.protectedWorld(p.level())){p.fallDistance=0;e.setCanceled(true);}
    }
    @SubscribeEvent public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.feast.ActivityWorld.protectedWorld(p.level())){
            e.setCanceled(true);p.setHealth(p.getMaxHealth());p.clearFire();p.fallDistance=0;
            if(OtherWorld.isOther(p.level()))com.coraline.feast.QuestRecovery.recoverToPorch(p);
        }
    }
    @SubscribeEvent public static void attack(net.minecraftforge.event.entity.player.AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&e.getTarget().getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getTarget().getPersistentData().getUUID("SecretOwner"))&&e.getTarget() instanceof com.coraline.otherworld.MouseEntity){com.coraline.secrets.SecretSpies.removeRat(p,e.getTarget());e.getTarget().discard();e.setCanceled(true);return;}if(com.coraline.feast.ActivityWorld.protectedWorld(e.getEntity().level())&&(e.getTarget().getPersistentData().getBoolean("CoralineDecoration")||e.getTarget() instanceof com.coraline.parents.ParentEntity)){if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)com.coraline.feast.MineshaftActivity.attack(p,e.getTarget());e.setCanceled(true);}}
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getTarget()==com.coraline.secrets.MotherSearch.mother(p)&&com.coraline.secrets.MotherInterrupts.blackmail(p)){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.secrets.JournalPlacement.interact(p,e.getTarget())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.secrets.SecretsRuntime.active(p)&&e.getTarget()==com.coraline.secrets.MotherSearch.mother(p)&&(com.coraline.secrets.SecretTransactions.resumeReward(p)||com.coraline.secrets.SecretDialogue.busy(p)||com.coraline.secrets.SecretTransactions.offer(p,e.getItemStack(),-1))){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getItemStack().is(com.coraline.registry.ModItems.SNOWBALL_POUCH.get())){if(e.getEntity() instanceof ServerPlayer p)com.coraline.feast.SnowballBattleActivity.throwBall(p);e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.feast.ShoppingMallActivity.npc(p,e.getTarget())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(e.getEntity() instanceof ServerPlayer p&&(com.coraline.feast.MineshaftActivity.cart(p,e.getTarget())||com.coraline.feast.MansionActivity.npc(p,e.getTarget()))){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getTarget() instanceof com.coraline.feast.FishingContestNpcEntity bookie&&bookie.getPersistentData().getBoolean("CoralineCircusBookie")){
            if(e.getEntity() instanceof ServerPlayer p&&e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)com.coraline.feast.MiceCircusActivity.interactBookie(p,bookie);
            e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&com.coraline.feast.CarnivalActivity.isCarnivalNpc(e.getTarget())){if(e.getEntity() instanceof ServerPlayer p)com.coraline.feast.CarnivalActivity.interactNpc(p,e.getTarget());e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&com.coraline.feast.CakeContestActivity.isContestNpc(e.getTarget())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getTarget() instanceof com.coraline.feast.FishingContestNpcEntity){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&com.coraline.feast.VillageActivity.isVillageVillager(e.getTarget())){
            if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof net.minecraft.world.entity.npc.Villager v){com.coraline.feast.VillageActivity.trade(p,v,e.getHand());e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}
            return;
        }
        if(com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel())&&!(e.getTarget() instanceof com.coraline.parents.ParentEntity)&&!(e.getTarget() instanceof com.coraline.feast.ScenePropEntity))e.setCanceled(true);
    }
    @SubscribeEvent public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getTarget()==com.coraline.secrets.MotherSearch.mother(p)&&com.coraline.secrets.MotherInterrupts.blackmail(p)){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.secrets.JournalPlacement.interact(p,e.getTarget())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getItemStack().is(com.coraline.registry.ModItems.SNOWBALL_POUCH.get())){if(e.getEntity() instanceof ServerPlayer p)com.coraline.feast.SnowballBattleActivity.throwBall(p);e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.feast.ShoppingMallActivity.npc(p,e.getTarget())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(e.getEntity() instanceof ServerPlayer p&&(com.coraline.feast.MineshaftActivity.cart(p,e.getTarget())||com.coraline.feast.MansionActivity.npc(p,e.getTarget()))){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getTarget() instanceof com.coraline.feast.FishingContestNpcEntity bookie&&bookie.getPersistentData().getBoolean("CoralineCircusBookie")){
            if(e.getEntity() instanceof ServerPlayer p&&e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)com.coraline.feast.MiceCircusActivity.interactBookie(p,bookie);
            e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&com.coraline.feast.CarnivalActivity.isCarnivalNpc(e.getTarget())){if(e.getEntity() instanceof ServerPlayer p)com.coraline.feast.CarnivalActivity.interactNpc(p,e.getTarget());e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&com.coraline.feast.CakeContestActivity.isContestNpc(e.getTarget())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getTarget() instanceof com.coraline.feast.FishingContestNpcEntity){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&com.coraline.feast.VillageActivity.isVillageVillager(e.getTarget())){
            if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof net.minecraft.world.entity.npc.Villager v){com.coraline.feast.VillageActivity.trade(p,v,e.getHand());e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}
            return;
        }
        if(com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel())&&!(e.getTarget() instanceof com.coraline.feast.ScenePropEntity))e.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&com.coraline.secrets.WildOres.canMine(p,e.getPos()))return;if(e.getPlayer() instanceof ServerPlayer p&&com.coraline.secrets.SecretWorld.mine(p,e.getPos())){e.setCanceled(true);return;}if(e.getLevel() instanceof net.minecraft.world.level.Level level&&com.coraline.feast.ActivityWorld.protectedWorld(level))e.setCanceled(true);}
    @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof net.minecraft.world.level.Level level&&com.coraline.feast.ActivityWorld.protectedWorld(level))e.setCanceled(true);}
    private static boolean creativeDebug(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack){
        if((stack.is(com.coraline.secrets.SecretsItems.DEBUG.get())||stack.is(com.coraline.secrets.SecretsItems.JOURNAL_DISCOVERY_DEBUG.get()))&&player.hasPermissions(2))return true;
        if(!player.isCreative())return false;
        var item=stack.getItem();
        return item instanceof com.coraline.item.FirstVisitDebugItem||item instanceof com.coraline.item.FamilySetupDebugItem||
            item instanceof com.coraline.item.RelationshipDebugItem||item instanceof com.coraline.item.VillageActivityDebugItem||
            item instanceof com.coraline.item.ArcheryActivityDebugItem||item instanceof com.coraline.item.CakeActivityDebugItem||
            item instanceof com.coraline.item.CarnivalActivityDebugItem||item instanceof com.coraline.item.DreamParkourActivityDebugItem||
            item instanceof com.coraline.item.FishingActivityDebugItem||item instanceof com.coraline.item.GardenMazeActivityDebugItem||
            item instanceof com.coraline.item.GrandFeastActivityDebugItem||item instanceof com.coraline.item.IceBoatRaceActivityDebugItem||
            item instanceof com.coraline.item.MagicActivityDebugItem;
    }
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem e){
        if(creativeDebug(e.getEntity(),e.getItemStack())||e.getItemStack().is(com.coraline.secrets.SecretsItems.GUIDE.get()))return;
        if(e.getEntity() instanceof ServerPlayer loopPlayer&&com.coraline.feast.OtherMotherLoop.portalOnly(loopPlayer)){e.setCanceled(true);return;}
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.feast.FirstVisit.active(p)!=null&&com.coraline.feast.FirstVisit.active(p).stage==com.coraline.feast.VisitData.Stage.BED){e.setCanceled(true);return;}
        if(!com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel()))return;var item=e.getItemStack().getItem();
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&item==com.coraline.registry.ModItems.ACTIVITY_MINE_MAP.get())return;
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&item==com.coraline.registry.ModItems.MANSION_KEY.get())return;
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&item==com.coraline.registry.ModItems.SNOWBALL_POUCH.get())return;
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&item==com.coraline.registry.ModItems.TREASURE_NOTE.get())return;
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&item==com.coraline.registry.ModItems.ARCHERY_BOW.get())return;
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&item==com.coraline.registry.ModItems.FISHING_CONTEST_ROD.get())return;
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&(item==com.coraline.registry.ModItems.CARNIVAL_RING.get()||item==com.coraline.registry.ModItems.CARNIVAL_DART.get()))return;
        if(!e.getItemStack().isEdible()&&!(item instanceof net.minecraft.world.item.PotionItem)&&!(item instanceof net.minecraft.world.item.MilkBucketItem)&&!(item instanceof net.minecraft.world.item.ArmorItem)&&!(item instanceof net.minecraft.world.item.ShieldItem)&&item!=net.minecraft.world.item.Items.SPYGLASS)e.setCanceled(true);
    }
    @SubscribeEvent public static void useBlock(PlayerInteractEvent.RightClickBlock e){
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())&&e.getItemStack().is(com.coraline.registry.ModItems.ACTIVITY_MINE_MAP.get())){if(e.getEntity() instanceof ServerPlayer p)com.coraline.feast.MineshaftActivity.openMap(p);e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(creativeDebug(e.getEntity(),e.getItemStack())){
            e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);e.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);return;
        }
        if(!com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel()))return;
        if(e.getEntity() instanceof ServerPlayer secretPlayer&&com.coraline.secrets.SecretWorld.interact(secretPlayer,e.getPos())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        var state=e.getLevel().getBlockState(e.getPos());
        if(e.getEntity() instanceof ServerPlayer visitor&&state.is(Blocks.SPRUCE_DOOR)){
            var tag=visitor.getPersistentData();
            var realm=tag.hasUUID("CoralineVisitDoor")?RealmData.get(visitor.server).realms.get(tag.getUUID("CoralineVisitDoor")):null;
            var attic=realm==null||realm.house==null?null:OtherWorldEstate.atticDoor(realm.house);
            if(attic!=null&&(e.getPos().equals(attic)||e.getPos().equals(attic.above()))){
                visitor.displayClientMessage(Component.literal("The attic door is locked."),true);
                e.setCanceled(true);return;
            }
        }
        if(e.getEntity() instanceof ServerPlayer visitor){
            var scene=com.coraline.feast.FirstVisit.active(visitor);
            if(scene!=null&&state.is(Blocks.DARK_OAK_DOOR)){
                var realm=RealmData.get(visitor.server).realms.get(scene.door);
                var study=realm==null||realm.house==null?null:OtherWorldEstate.studyDoor(realm.house);
                boolean isStudy=study!=null&&(e.getPos().equals(study)||e.getPos().equals(study.above()));
                boolean locked=scene.stage==com.coraline.feast.VisitData.Stage.FATHER_ENTER||
                    scene.stage==com.coraline.feast.VisitData.Stage.FATHER_INTRO||
                    scene.stage==com.coraline.feast.VisitData.Stage.SONG||
                    scene.stage==com.coraline.feast.VisitData.Stage.DINNER_READY;
                if(isStudy&&locked){visitor.displayClientMessage(Component.literal("The study door is locked while Father plays."),true);e.setCanceled(true);return;}
            }
        }
        // Opening/closing the Little Door is cosmetic; OtherWorld.enter enforces travel locks.
        if(state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))return;
        if(e.getEntity() instanceof ServerPlayer loopPlayer&&com.coraline.feast.OtherMotherLoop.portalOnly(loopPlayer)){e.setCanceled(true);return;}
        if(com.coraline.feast.ActivityWorld.isActivity(e.getLevel())){
            if(e.getItemStack().is(com.coraline.registry.ModItems.SNOWBALL_POUCH.get())){e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);e.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);return;}
            if(e.getItemStack().is(com.coraline.registry.ModItems.ACTIVITY_CHORUS_FRUIT.get())){
                e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);e.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);return;
            }
            if(e.getItemStack().is(com.coraline.registry.ModItems.FISHING_CONTEST_ROD.get())||e.getItemStack().is(com.coraline.registry.ModItems.CARNIVAL_RING.get())||e.getItemStack().is(com.coraline.registry.ModItems.CARNIVAL_DART.get())){
                e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
                e.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                return;
            }
            if(e.getEntity() instanceof ServerPlayer visitor){
                com.coraline.feast.CakeContestActivity.interactBlock(visitor,e.getPos());
                com.coraline.feast.CarnivalActivity.interactBlock(visitor,e.getPos());
                com.coraline.feast.GardenMazeActivity.interactBlock(visitor,e.getPos());
                com.coraline.feast.MansionActivity.interactBlock(visitor,e.getPos());
                com.coraline.feast.TreasureHuntActivity.interactBlock(visitor,e.getPos());
            }
            e.setCanceled(true);return;
        }
        if(e.getEntity() instanceof ServerPlayer leaving&&state.getBlock() instanceof BedBlock&&com.coraline.secrets.SecretsRuntime.exitBlocked(leaving)){e.setCanceled(true);return;}
        if(e.getEntity() instanceof ServerPlayer loopPlayer&&com.coraline.feast.OtherMotherLoop.locked(loopPlayer)
            &&(state.getBlock() instanceof BedBlock||state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))){
            loopPlayer.displayClientMessage(Component.literal("Your Other Mother hasn't finished with you yet."),true);e.setCanceled(true);return;
        }
        if(e.getEntity() instanceof ServerPlayer p&&com.coraline.feast.FirstVisit.active(p)!=null){
            var scene=com.coraline.feast.FirstVisit.active(p);
            boolean passage=state.getBlock() instanceof DoorBlock||state.getBlock() instanceof TrapDoorBlock||state.getBlock() instanceof FenceGateBlock;
            if((state.getBlock() instanceof BedBlock&&!com.coraline.feast.FirstVisit.bedAllowed(p))||(scene.stage==com.coraline.feast.VisitData.Stage.BED&&!(state.getBlock() instanceof BedBlock)&&!passage)){p.displayClientMessage(Component.literal(scene.stage==com.coraline.feast.VisitData.Stage.BED?"It's time for bed.":"Your Other Mother is expecting you for dinner first."),true);e.setCanceled(true);return;}
        }
        if(state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))return;
        if(state.getBlock() instanceof DoorBlock||state.getBlock() instanceof TrapDoorBlock||state.getBlock() instanceof FenceGateBlock)return;
        if(state.getBlock() instanceof BedBlock&&e.getEntity() instanceof ServerPlayer player){
            var tag=player.getPersistentData();var r=tag.hasUUID("CoralineVisitDoor")?RealmData.get(player.server).realms.get(tag.getUUID("CoralineVisitDoor")):null;
            var foot=e.getPos();var head=state.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD?foot:foot.relative(state.getValue(BedBlock.FACING));
            if(r!=null&&r.bed!=null&&(r.bed.equals(foot)||r.bed.equals(head)||r.bed.south().equals(foot)||r.bed.south().equals(head))){
                if(!tag.contains("CoralineWakeAt")){
                    player.startSleeping(head);tag.putLong("CoralineWakeAt",player.server.overworld().getGameTime()+Math.max(60,com.coraline.feast.VisitAudio.length("see_you_soon")+10));
                    var scene=com.coraline.feast.FirstVisit.active(player);
                    if(scene!=null)tag.putLong("CoralineWakeAt",Math.max(tag.getLong("CoralineWakeAt"),scene.supperMusicEndsAt));
                    com.coraline.feast.VisitAudio.play(player,"see_you_soon",false);
                }
            }else player.displayClientMessage(Component.literal("This isn't the bedroom prepared for you."),true);
        }
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
    }
    @SubscribeEvent public static void explosion(net.minecraftforge.event.level.ExplosionEvent.Detonate e){if(com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel())){e.getAffectedBlocks().clear();e.getAffectedEntities().clear();}}
    @SubscribeEvent public static void bucket(net.minecraftforge.event.entity.player.FillBucketEvent e){if(com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel()))e.setCanceled(true);}
    @SubscribeEvent public static void spawn(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn e){if(com.coraline.feast.ActivityWorld.protectedWorld(e.getLevel().getLevel()))e.setSpawnCancelled(true);}
    @SubscribeEvent public static void tokenToss(net.minecraftforge.event.entity.item.ItemTossEvent e){
        if(e.getPlayer() instanceof ServerPlayer guest&&com.coraline.feast.ActivityWorld.isActivity(guest.level())&&
            (e.getEntity().getItem().is(com.coraline.registry.ModItems.CARNIVAL_TOKEN.get())||e.getEntity().getItem().is(com.coraline.registry.ModItems.VILLAGE_TOKEN.get()))&&
            e.getEntity().getItem().hasTag()&&e.getEntity().getItem().getTag().hasUUID("CoralineCarnivalOwner")){
            var stack=e.getEntity().getItem().copy();e.setCanceled(true);guest.getInventory().add(stack);
            guest.displayClientMessage(Component.literal("Keep your carnival tokens until the attractions are finished."),true);return;
        }
        if(e.getPlayer() instanceof ServerPlayer baker&&com.coraline.feast.ActivityWorld.isActivity(baker.level())&&
            e.getEntity().getItem().is(com.coraline.registry.ModItems.CONTEST_CAKE.get())&&
            e.getEntity().getItem().hasTag()&&e.getEntity().getItem().getTag().hasUUID("CoralineCakeOwner")){
            var stack=e.getEntity().getItem().copy();e.setCanceled(true);
            baker.getInventory().add(stack);
            baker.displayClientMessage(Component.literal("Keep your contest cake until the judging panel."),true);return;
        }
        if(e.getPlayer() instanceof ServerPlayer archer&&com.coraline.feast.ActivityWorld.isActivity(archer.level())&&
            (e.getEntity().getItem().is(com.coraline.registry.ModItems.ARCHERY_ARROW.get())||e.getEntity().getItem().is(com.coraline.registry.ModItems.ARCHERY_BOW.get()))&&
            e.getEntity().getItem().hasTag()&&e.getEntity().getItem().getTag().getBoolean("CoralineArcheryIssued")){
            var stack=e.getEntity().getItem().copy();e.setCanceled(true);
            archer.getInventory().add(stack); // A missing remainder is restored by the contest ledger next tick.
            archer.displayClientMessage(Component.literal("Keep Father's contest equipment until the round ends."),true);return;
        }
        if(!(e.getPlayer() instanceof ServerPlayer p)||!com.coraline.feast.ActivityWorld.isActivity(p.level())||!e.getEntity().getItem().is(com.coraline.registry.ModItems.VILLAGE_TOKEN.get()))return;
        var stack=e.getEntity().getItem().copy();e.setCanceled(true);
        p.getInventory().add(stack);if(!stack.isEmpty())p.drop(stack,false);
        p.displayClientMessage(Component.literal("Keep your button tokens until the village trading is finished."),true);
    }
}
