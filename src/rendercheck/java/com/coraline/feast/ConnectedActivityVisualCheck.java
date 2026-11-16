package com.coraline.feast;

import com.coraline.multiplayer.*;
import com.coraline.otherworld.OtherWorld;
import com.coraline.tycoon.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Disposable checkpoints using two actual TCP-connected players. The fixture
 * moves participants through native temple tiles and invokes normal validated
 * server interaction handlers. It never edits outcomes, completion clocks,
 * payout journals or ending witnesses. It is not a full campaign or a claim
 * that hardware input was exercised on the guest for every interaction. */
public final class ConnectedActivityVisualCheck {
    private static int stage,age,total,ownerMetal,guestMetal,ownerMall,guestMall,price,quantity,bedBefore,rootBedBefore;
    private static Item mallItem;
    private static ListTag rootInventory;
    private static ServerPlayer oldRoot;
    private static java.util.UUID rootId;
    private static volatile String capture;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void capture(Minecraft mc){var name=capture;if(name==null)return;capture=null;
        try{var folder=java.nio.file.Path.of("../artwork/connected_transactions_qa");java.nio.file.Files.createDirectories(folder);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(folder.resolve(name+".png"));}}
        catch(java.io.IOException error){throw new IllegalStateException(error);}
    }
    private static void move(ServerPlayer p,Vec3 at){p.teleportTo(p.serverLevel(),at.x,at.y,at.z,p.getYRot(),p.getXRot());p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}
    private static void invite(ServerPlayer owner,VisitData.Scene s,OtherMotherActivities.Activity activity){
        // Explicit invitation checkpoint; no first-entry/mother-reaction or
        // story completion credit is inferred from this staging operation.
        s.loop.generation++;s.loop.activity=activity;s.loop.loot=OtherMotherDialogue.Loot.TREASURE;s.loop.phase=OtherMotherLoopState.Phase.READY;
        s.loop.portal=owner.blockPosition();ActivityWorld.enter(owner,s);
        require(ActivityWorld.isActivity(owner.level()),"Native activity entrance did not transport the director");
    }
    public static void begin(ServerPlayer owner,ServerPlayer guest,VisitData.Scene s){
        require(owner!=guest&&owner.server.getPlayerList().getPlayer(owner.getUUID())==owner&&owner.server.getPlayerList().getPlayer(guest.getUUID())==guest,"Two actual connected participants required");
        ownerMetal=owner.getInventory().countItem(Items.NETHERITE_INGOT);guestMetal=guest.getInventory().countItem(Items.NETHERITE_INGOT);
        invite(owner,s,OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE);stage=0;age=total=0;
        System.out.println("CONNECTED_TRANSACTIONS_STARTED: actual two-client native temple, mall and household transactions; explicit invitation/developed-household checkpoints");
    }
    public static boolean tick(ServerPlayer owner,ServerPlayer guest,VisitData.Scene s){
        require(++total<20000,"Connected transactions exceeded bounded runtime at stage "+stage);age++;
        var sessions=StorySessionData.get(owner.server);
        if(stage<6)require(guest!=null,"Guest disconnected before offline-household checkpoint");
        if(stage==0){
            if(guest.serverLevel()!=owner.serverLevel()){require(age<200,"Guest did not follow native arena transport");return false;}
            require(sessions.groupActivity(s),"Actual participant cohort was not saved at entrance");
            owner.invulnerableTime=0;owner.hurt(owner.damageSources().generic(),owner.getHealth()+20);
            require(owner.isAlive()&&owner.isSpectator()&&sessions.activitySpectator(owner)&&!guest.isSpectator(),"Fatal director damage did not preserve a living spectator and playing guest");
            capture="temple_spectator";stage=1;age=0;
            System.out.println("CONNECTED_TEMPLE_DEATH_OK: actual damage event, director spectates and guest continues");
        }else if(stage==1){
            require(owner.isAlive()&&owner.isSpectator()&&!guest.isSpectator(),"Temple participant modes changed before completion");
            var n=DesertTempleActivity.state(s);int phase=n.getInt("Phase"),room=n.getInt("Room");var c=DesertTempleActivity.center(owner,s);
            require(phase!=5,"Native temple failed while following its generated path");
            if(phase==1)move(guest,Vec3.atBottomCenterOf(c.offset(0,3,DesertTempleCourse.top(room)+8)));
            else if(phase==3&&age%3==0){
                var trial=sessions.activityProgress(guest,s,"temple");int next=trial!=null&&trial.contains("Room")&&trial.getInt("Room")==room?trial.getInt("Next"):0;
                var path=DesertTempleActivity.path(s);require(next<path.size(),"Completed path did not advance its native room");
                move(guest,Vec3.atBottomCenterOf(DesertTempleCourse.tile(c,room,path.get(next))));
            }else if(phase==4){
                var prize=owner.serverLevel().getEntitiesOfClass(ScenePropEntity.class,DesertTempleCourse.bounds(c),e->e.slot==710&&ActivityScenery.owned(e,s)).stream().findFirst().orElse(null);
                if(prize!=null){move(guest,prize.position().add(0,0,1));LoopProps.interact(guest,prize);require(n.getInt("Phase")==6,"Guest prize interaction did not commit native temple victory");
                    capture="temple_guest_victory";ActivityWorld.exit(owner,s);stage=2;age=0;}
            }
        }else if(stage==2){
            if(!OtherWorld.isOther(owner.level())||guest.serverLevel()!=owner.serverLevel()||owner.isSpectator()||guest.isSpectator()){require(age<200,"Native temple return failed to restore both modes/realms");return false;}
            if(owner.getInventory().countItem(Items.NETHERITE_INGOT)!=ownerMetal+1||guest.getInventory().countItem(Items.NETHERITE_INGOT)!=guestMetal+1){require(age<200,"Both temple winners did not receive their own actual jackpot");return false;}
            require(owner.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&guest.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Eliminated participant original mode not restored");
            require(!sessions.activitySpectator(owner)&&!sessions.activitySpectator(guest),"Completed activity retained a spectator lease");
            System.out.println("CONNECTED_TEMPLE_WIN_OK: guest completed all five real paths; both native inventories received jackpot; original modes restored");
            capture="temple_return_and_rewards";invite(owner,s,OtherMotherActivities.Activity.SHOPPING_MALL);stage=3;age=0;
        }else if(stage==3){
            var n=ExpeditionActivities.state(s);if(n.getInt("Phase")==0)return false;
            require(n.getInt("Phase")==1&&guest.serverLevel()==owner.serverLevel(),"Mall did not enter native shopping phase");
            var products=n.getList("Stock",Tag.TAG_COMPOUND);int selected=-1;
            for(int i=0;i<products.size();i++){var product=products.getCompound(i);if(!ItemStack.of(product.getCompound("Item")).is(Items.EMERALD)&&product.getInt("Stock")>0&&product.getInt("Price")>0&&(selected<0||product.getInt("Price")<products.getCompound(selected).getInt("Price")))selected=i;}
            require(selected>=0,"Actual mall generated no purchasable product");var product=products.getCompound(selected);var stack=ItemStack.of(product.getCompound("Item"));mallItem=stack.getItem();quantity=stack.getCount();price=product.getInt("Price");
            ownerMall=owner.getInventory().countItem(mallItem);guestMall=guest.getInventory().countItem(mallItem);int balance=n.getInt("Balance"),stock=product.getInt("Stock");
            move(guest,Vec3.atBottomCenterOf(MallScenery.shop(ExpeditionActivities.center(owner,s),product.getInt("Store"))));ShoppingMallActivity.open(guest,s,selected);
            var menu=sessions.activityProgress(guest,s,"mall_menu");require(menu!=null&&menu.hasUUID("MenuNonce"),"Guest did not receive its own native shop nonce");var nonce=menu.getUUID("MenuNonce");
            ShoppingMallActivity.request(guest,java.util.UUID.randomUUID(),selected,1);require(n.getInt("Balance")==balance,"Foreign shop nonce spent shared currency");
            ShoppingMallActivity.request(guest,nonce,selected,1);if(price>=300)ShoppingMallActivity.request(guest,nonce,selected,1);
            require(n.getInt("Balance")==balance-price&&product.getInt("Stock")==stock-1,"Guest purchase did not debit exact shared balance and stock");
            require(n.getCompound("BuyerBags").getList(guest.getUUID().toString(),Tag.TAG_COMPOUND).size()==1&&n.getCompound("BuyerBags").getList(owner.getUUID().toString(),Tag.TAG_COMPOUND).isEmpty(),"Guest item was assigned to the director's bag");
            ShoppingMallActivity.request(guest,nonce,selected,1);require(n.getInt("Balance")==balance-price,"Stale purchase nonce charged twice");
            ActivityWorld.exit(owner,s);stage=4;age=0;
        }else if(stage==4){
            if(!OtherWorld.isOther(owner.level())||guest.serverLevel()!=owner.serverLevel()||guest.getInventory().countItem(mallItem)!=guestMall+quantity){require(age<200,"Native purchaser delivery did not reach the guest after return");return false;}
            require(owner.getInventory().countItem(mallItem)==ownerMall,"Guest purchase copied to director inventory");
            System.out.println("CONNECTED_MALL_PURCHASE_OK: real generated stock, per-player nonce, shared exact debit, stale/foreign denial and purchaser-only native delivery");capture="mall_return";
            var h=TycoonWorld.home(guest);rootId=guest.getUUID();h.state.putBoolean("Activated",true);
            System.out.println("CONNECTED_TYCOON_DEVELOPED_CHECKPOINT: disposable activated guest-owned home and 24 actual fixture Void Power; not a claimed canonical unlock");
            int slot=-1;for(int i=0;i<9;i++)if(guest.getInventory().getItem(i).isEmpty()){slot=i;break;}require(slot>=0,"Fixture needs one empty hand slot for native deposit");
            guest.getInventory().setItem(slot,new ItemStack(TycoonContent.VOID_POWER.get(),24));guest.getInventory().selected=slot;
            require(TycoonWarehouse.deposit(guest,24).startsWith("Purchased")&&TycoonWarehouse.count(h,TycoonContent.VOID_POWER.get())==24,"Actual household supply deposit failed");
            String denied=TycoonScope.call(owner,rootId,()->TycoonShop.buy(owner,"orphan_bed"),"denied");require(denied.equals("denied"),"Unpermitted collaborator spent household funds");
            require(TycoonCollaborators.permit(guest,owner.getUUID(),true)==1,"Actual owner could not checkpoint collaborator permission");
            bedBefore=owner.getInventory().countItem(TycoonContent.BLOCKS.get("orphan_bed").get().asItem());rootBedBefore=guest.getInventory().countItem(TycoonContent.BLOCKS.get("orphan_bed").get().asItem());
            purchaseBed(owner,h,1);require(guest.getInventory().countItem(TycoonContent.BLOCKS.get("orphan_bed").get().asItem())==rootBedBefore,"Collaborator delivery went to household owner");
            rootInventory=guest.getInventory().save(new ListTag());oldRoot=guest;guest.connection.disconnect(Component.literal("Disposable offline-household QA checkpoint"));stage=6;age=0;
            System.out.println("CONNECTED_TRANSACTIONS_RECONNECT_REQUIRED: restart the same CoralineQAGuest after the offline purchase marker");
        }else if(stage==6){
            if(guest==oldRoot){require(age<200,"Guest did not disconnect for offline-household check");return false;}
            require(owner.server.getPlayerList().getPlayer(rootId)==null,"Household owner still online during offline purchase");
            purchaseBed(owner,TycoonData.get(owner.server).homes.get(rootId),2);stage=7;age=0;
            System.out.println("CONNECTED_TYCOON_OFFLINE_PURCHASE_OK: actual connected collaborator spent exact shared stock while root owner disconnected; restart CoralineQAGuest now");
        }else if(stage==7){
            if(guest==null||guest==oldRoot){require(age<18000,"Household owner did not reconnect");return false;}
            require(guest.getUUID().equals(rootId)&&rootInventory.equals(guest.getInventory().save(new ListTag())),"Offline collaborator purchase changed root owner's original inventory");
            require(sessions.scene(guest)==s&&!VisitData.get(owner.server).scenes.containsKey(guest.getUUID()),"Second reconnect changed personal story ownership");
            var h=TycoonData.get(owner.server).homes.get(rootId);long funds=TycoonWarehouse.count(h,TycoonContent.VOID_POWER.get());
            require(TycoonCollaborators.permit(guest,owner.getUUID(),false)==1,"Root owner could not revoke collaboration");
            require(TycoonScope.call(owner,rootId,()->TycoonShop.buy(owner,"orphan_bed"),"denied").equals("denied")&&TycoonWarehouse.count(h,TycoonContent.VOID_POWER.get())==funds,"Revoked collaboration still spent shared stock");
            require(sessions.checkpoint(owner.server),"Final two-client transaction checkpoint failed");capture="collaboration_receipts";
            System.out.println("CONNECTED_TRANSACTIONS_NATIVE_COMPLETE: two real TCP profiles; native five-room temple guest victory after director death, both jackpot deliveries and mode restoration; guest-only mall purchase; saved online/offline collaborator payments and revoke; original root inventory survived new-process reconnect. Explicit checkpoints, not full ghost/ending/reset acceptance.");stage=8;return true;
        }
        return false;
    }
    private static void purchaseBed(ServerPlayer actor,TycoonData.Home home,int count){
        int cost=TycoonConfig.PRICES.get("orphan_bed").get();String result=TycoonScope.call(actor,rootId,()->TycoonShop.buy(actor,"orphan_bed"),"denied");
        require(result.startsWith("Purchased"),"Native collaborator purchase failed: "+result);
        require(TycoonWarehouse.count(home,TycoonContent.VOID_POWER.get())==24L-count*cost,"Collaborator did not debit exact household supply stock");
        require(actor.getInventory().countItem(TycoonContent.BLOCKS.get("orphan_bed").get().asItem())==bedBefore+count,"Actual collaborator did not receive its own item");
        require(TycoonScope.call(actor,rootId,()->actor.getInventory().items.stream().anyMatch(item->item.is(TycoonContent.BLOCKS.get("orphan_bed").get().asItem())&&TycoonWorld.owned(actor,item)),false),"Collaborator item lost actual household capability binding");
    }
    private ConnectedActivityVisualCheck(){}
}
