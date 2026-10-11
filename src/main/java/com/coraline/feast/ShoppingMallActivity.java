package com.coraline.feast;
import static com.coraline.feast.ExpeditionActivities.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.*;

public final class ShoppingMallActivity {
    private static final Map<UUID,ServerBossEvent> BARS=new HashMap<>();
    static boolean active(ServerPlayer p,VisitData.Scene s){return owns(p,s)&&s.loop.activity==OtherMotherActivities.Activity.SHOPPING_MALL&&state(s).getInt("Phase")==1&&!state(s).getBoolean("Paid");}
    static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){var n=state(s);n.putInt("Balance",1000);n.put("Stock",MallStock.generate(n.getLong("Seed"),MineshaftActivity.knownMaterial(p,"moonstone"),MineshaftActivity.knownMaterial(p,"other_diamond")));MallScenery.build(p,s,l,c);refreshDisplays(l,s,c);}
    static void begin(ServerPlayer p,VisitData.Scene s){ActivityScenery.title(p,"SHOPPING MALL","1,000 tokens. Make them count.");ActivityWorld.ensureExit(p,s);}
    static void refreshDisplays(ServerLevel l,VisitData.Scene s,BlockPos c){var list=state(s).getList("Stock",10);for(int i=0;i<list.size();i++){var n=list.getCompound(i);var at=MallScenery.display(c,n.getInt("Store"),n.getInt("Slot"));final int slot=1000+i;var existing=LoopProps.nearby(l,s,at).stream().filter(e->e.slot==slot).findFirst();var prop=existing.orElseGet(()->LoopProps.spawn(l,s,ScenePropEntity.PRIZE_PLATE,Vec3.atBottomCenterOf(at).add(0,.51,0),slot));var stack=ItemStack.of(n.getCompound("Item"));prop.food(0,n.getInt("Stock")>0?stack:ItemStack.EMPTY);var face=MallScenery.side(n.getInt("Store"))<0?Direction.EAST:Direction.WEST;String name=stack.getHoverName().getString();sign(l,at.relative(face).below(),face,name.length()>22?name.substring(0,22):name,n.getInt("Stock")>0?n.getInt("Price")+" Shopping Tokens":"SOLD OUT","Right-click display");}}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){if(!bounds(c,s.loop.activity).inflate(-1).contains(p.position()))ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));if(!active(p,s))return;
        if(p.tickCount%20==0){var bar=BARS.computeIfAbsent(s.owner,k->new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS));bar.addPlayer(p);bar.setProgress(state(s).getInt("Balance")/1000F);bar.setName(Component.literal("Shopping Tokens: "+state(s).getInt("Balance")+"  •  Bag: "+state(s).getInt("Purchases")));}
        if(p.tickCount%10==0){
            var ground=MallScenery.walkTargets(c,0);var upper=MallScenery.walkTargets(c,1);
            for(var npc:p.serverLevel().getEntitiesOfClass(FishingContestNpcEntity.class,bounds(c,s.loop.activity),e->ActivityScenery.owned(e,s)&&e.getPersistentData().getBoolean("MallShopper")))
                wander(npc,c,npc.getPersistentData().getInt("MallFloor")==0?ground:upper);
        }
    }
    static void wander(FishingContestNpcEntity npc,BlockPos c,List<BlockPos> stops){
        long now=npc.level().getGameTime();var data=npc.getPersistentData();
        if(now<data.getLong("MallNextWalk"))return;
        if(!npc.getNavigation().isDone()&&now<data.getLong("MallWalkDeadline"))return;
        // Pause after arriving, rather than resetting every NPC's path at the same instant.
        if(data.getBoolean("MallWalking")){
            data.putBoolean("MallWalking",false);npc.getNavigation().stop();
            data.putLong("MallNextWalk",now+20+npc.getRandom().nextInt(70));return;
        }
        var choices=new ArrayList<BlockPos>();
        for(var at:stops){double distance=npc.distanceToSqr(Vec3.atBottomCenterOf(at));
            if(distance>16&&distance<28*28&&(!data.contains("MallLastStop")||data.getLong("MallLastStop")!=at.asLong()))choices.add(at);
        }
        Collections.shuffle(choices,new Random(npc.getRandom().nextLong()));
        for(var at:choices){
            if(!npc.level().getBlockState(at.below()).isSolidRender(npc.level(),at.below())||!npc.level().isEmptyBlock(at)||!npc.level().isEmptyBlock(at.above()))continue;
            var path=npc.getNavigation().createPath(at,0);
            if(path==null||!path.canReach())continue;
            npc.getNavigation().moveTo(path,.7+npc.getRandom().nextDouble()*.3);
            data.putLong("MallLastStop",at.asLong());data.putLong("MallWalkDeadline",now+400);
            data.putLong("MallNextWalk",now+10);data.putBoolean("MallWalking",true);return;
        }
        data.putLong("MallNextWalk",now+60+npc.getRandom().nextInt(80));
    }
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){if(prop.slot<1000||prop.slot>=1000+state(s).getList("Stock",10).size())return false;if(active(p,s))open(p,s,prop.slot-1000);return true;}
    public static boolean npc(ServerPlayer p,Entity e){if(!e.getPersistentData().getBoolean("MallNpc"))return false;var s=OtherMotherLoop.active(p);if(!active(p,s)||!ActivityScenery.owned(e,s)||p.distanceToSqr(e)>25)return true;int store=e.getPersistentData().getInt("MallStore");if(store<0)return true;var list=state(s).getList("Stock",10);for(int i=0;i<list.size();i++)if(list.getCompound(i).getInt("Store")==store){if(e instanceof LivingEntity living){living.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,p.getEyePosition());}open(p,s,i);break;}return true;}
    static void open(ServerPlayer p,VisitData.Scene s,int index){var n=state(s);var stock=n.getList("Stock",10);if(index<0||index>=stock.size())return;var product=stock.getCompound(index);if(p.distanceToSqr(Vec3.atCenterOf(MallScenery.shop(center(p,s),product.getInt("Store"))))>225)return;n.putUUID("MenuNonce",UUID.randomUUID());n.putInt("MenuStore",product.getInt("Store"));n.putInt("Selected",index);n.putLong("MenuUntil",OtherMotherLoop.now(p)+1200);n.remove("Confirm");send(p,s,false);}
    static void send(ServerPlayer p,VisitData.Scene s,boolean confirm){var n=state(s);var payload=new CompoundTag();payload.putUUID("Nonce",n.getUUID("MenuNonce"));payload.putInt("Selected",n.getInt("Selected"));payload.putInt("Balance",n.getInt("Balance"));payload.putInt("Store",n.getInt("MenuStore"));payload.putBoolean("Confirm",confirm);var list=new ListTag();var stock=n.getList("Stock",10);for(int i=0;i<stock.size();i++)if(stock.getCompound(i).getInt("Store")==n.getInt("MenuStore")){var entry=stock.getCompound(i).copy();entry.putInt("Index",i);list.add(entry);}payload.put("Products",list);ExpeditionNetwork.mall(p,payload);}
    /** Every request is bound to a server-created menu nonce, owner, generation, room and distance. */
    public static void request(ServerPlayer p,UUID nonce,int index,int action){var s=OtherMotherLoop.active(p);if(!active(p,s))return;var n=state(s);if(!n.hasUUID("MenuNonce")||!n.getUUID("MenuNonce").equals(nonce)||OtherMotherLoop.now(p)>n.getLong("MenuUntil"))return;var list=n.getList("Stock",10);if(index<0||index>=list.size())return;var entry=list.getCompound(index);if(entry.getInt("Store")!=n.getInt("MenuStore")||p.distanceToSqr(Vec3.atCenterOf(MallScenery.shop(center(p,s),entry.getInt("Store"))))>225)return;
        if(action==0){n.putInt("Selected",index);n.remove("Confirm");send(p,s,false);return;}
        if(action!=1)return;if(entry.getInt("Price")>=300&&(!n.contains("Confirm")||n.getInt("Confirm")!=index)){n.putInt("Selected",index);n.putInt("Confirm",index);send(p,s,true);return;}
        if(!purchase(s,p.getUUID(),index))return;n.remove("MenuNonce");OtherMotherLoop.changed(p);refreshDisplays(p.serverLevel(),s,center(p,s));p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.PLAYERS,.6F,1.6F);p.displayClientMessage(Component.literal("Purchased — "+n.getInt("Balance")+" Shopping Tokens remaining"),true);open(p,s,index);
    }
    static boolean purchase(VisitData.Scene s,UUID buyer,int index){var n=state(s);var list=n.getList("Stock",10);if(!s.owner.equals(buyer)||n.getInt("Phase")!=1||n.getBoolean("Paid")||index<0||index>=list.size())return false;var entry=list.getCompound(index);int price=entry.getInt("Price");if(price<=0||entry.getInt("Stock")<=0||n.getInt("Balance")<price)return false;var stack=ItemStack.of(entry.getCompound("Item"));if(stack.isEmpty())return false;n.putInt("Balance",n.getInt("Balance")-price);entry.putInt("Stock",entry.getInt("Stock")-1);var bag=n.getList("ShoppingBag",10);bag.add(stack.save(new CompoundTag()));n.put("ShoppingBag",bag);n.putInt("Purchases",n.getInt("Purchases")+1);return true;}
    static void settle(ServerPlayer p,VisitData.Scene s){var n=state(s);clear(p);if(n.getBoolean("Paid"))return;n.putBoolean("Paid",true);n.putInt("Balance",0);n.remove("MenuNonce");var bag=n.getList("ShoppingBag",10).copy();n.remove("ShoppingBag");var pending=p.getPersistentData().getList("CoralineMallDelivery",10);for(var tag:bag)pending.add(tag);p.getPersistentData().put("CoralineMallDelivery",pending);OtherMotherLoop.changed(p);ActivityScenery.title(p,"SHOPPING COMPLETE",n.getInt("Purchases")+" purchases packed");}
    /** Deliver AFTER transport so overflow drops at the destination, not inside an erased mall. */
    public static void deliver(ServerPlayer p){var pending=p.getPersistentData().getList("CoralineMallDelivery",10).copy();p.getPersistentData().remove("CoralineMallDelivery");for(var tag:pending)ActivityTokens.give(p,ItemStack.of((CompoundTag)tag));}
    static void clear(ServerPlayer p){var b=BARS.remove(p.getUUID());if(b!=null)b.removeAllPlayers();ExpeditionNetwork.mall(p,new CompoundTag());}
    private ShoppingMallActivity(){}
}
