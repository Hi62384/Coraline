package com.coraline.tycoon;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.BlockHitResult;

/** Disposable declared owner/visitor/resources checkpoint. Shop, placement,
 * fridge transfers, physical meal route and manual gift use production handlers. */
public final class TycoonFoodVisualCheck {
    private static Minecraft mc;private static volatile int stage,age;private static volatile boolean pending;private static volatile Throwable failure;
    private static UUID resident,actor;private static int ticks;private static double initialTrust,initialSuspicion,mealTrust;private static boolean fetched,seated;
    public static void tick(){mc=Minecraft.getInstance();if(failure!=null)throw new IllegalStateException("Food QA stage="+stage,failure);if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<80)return;
        mc.options.pauseOnLostFocus=false;age++;if(++ticks>6000)throw new IllegalStateException("Food QA timeout stage="+stage);if(ticks%100==0)com.mojang.logging.LogUtils.getLogger().info("TYCOON_FOOD stage={} age={} position={}",stage,age,mc.player.position());
        switch(stage){
            case 0->server(0,p->{setup(p);go(1);});
            case 1->{if(age>50&&personal()&&foodTab())go(2);}
            case 2->{if(age>20){checkLayout();mc.getWindow().setWindowed(1280,960);mc.options.guiScale().set(4);mc.resizeDisplay();checkLayout();shot("food_shop_biscuits");check(press("Buy 1"),"Food purchase button missing");go(3);}}
            case 3->server(3,p->{if(TycoonWorld.home(p).state.getLong("SoulEssence")!=298)return;check(count(p,0)==1,"Real food purchase missing");go(4);});
            case 4->{if(age>20){check(press("Supper Table"),"Table purchase button missing");go(5);}}
            case 5->server(5,p->{if(!has(p,"other_world_supper_table"))return;go(6);});
            case 6->{if(age>20){check(press("Other World Fridge"),"Fridge purchase button missing");go(7);}}
            case 7->server(7,p->{if(!has(p,"other_world_fridge"))return;check(TycoonShop.count(p,TycoonContent.VOID_POWER.get())==42,"Furniture did not debit22 real Power");go(8);});
            case 8->{if(age>20){press("Special Gifts");go(9);}}
            case 9->{if(age>20){shot("special_welcome_cake_shop");check(press("Buy 1"),"Cake purchase missing");go(10);}}
            case 10->server(10,p->{if(TycoonWorld.home(p).state.getLong("SoulEssence")!=218)return;check(count(p,7)==1,"Real purchased cake missing");mc.execute(()->mc.setScreen(null));hold(p,TycoonContent.BLOCKS.get("other_world_supper_table").get().asItem());frame(p,table(p).south());go(11);});
            case 11->{if(age>20){place(table(owner()));go(12);}}
            case 12->server(12,p->{if(!(p.serverLevel().getBlockEntity(table(p)) instanceof TycoonBlockEntity))return;check(!has(p,"other_world_supper_table"),"Table placement did not consume item");hold(p,TycoonContent.BLOCKS.get("other_world_fridge").get().asItem());frame(p,fridge(p).east(2));go(13);});
            case 13->{if(age>20&&mc.level.getEntitiesOfClass(TycoonNpc.class,new net.minecraft.world.phys.AABB(fridge(owner())).inflate(.5)).isEmpty()){place(fridge(owner()));go(14);}}
            case 14->server(14,p->{if(!(p.serverLevel().getBlockEntity(fridge(p)) instanceof TycoonBlockEntity)){if(age>80)throw new IllegalStateException("Fridge placement refused: held="+p.getMainHandItem()+" target="+p.serverLevel().getBlockState(fridge(p))+" support="+p.serverLevel().getBlockState(fridge(p).below())+" nearby="+p.serverLevel().getEntities(null,new net.minecraft.world.phys.AABB(fridge(p))).stream().map(e->e.getType()+"@"+e.position()).toList());return;}check(!has(p,"other_world_fridge"),"Fridge placement did not consume item");hold(p,TycoonFoodContent.ITEMS.get(0).get());go(15);});
            case 15->{if(age>20){use(fridge(owner()));go(16);}}
            case 16->{if(age>20&&mc.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu){var menu=mc.player.containerMenu;int slot=-1;for(int i=9;i<menu.slots.size();i++)if(TycoonFoodContent.index(menu.getSlot(i).getItem())==0){slot=i;break;}check(slot>=0,"Purchased meal missing in real fridge menu");mc.gameMode.handleInventoryMouseClick(menu.containerId,slot,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);go(17);}}
            case 17->{if(age>20){shot("stocked_owned_fridge");mc.player.closeContainer();server(17,p->{var be=(TycoonBlockEntity)p.serverLevel().getBlockEntity(fridge(p));check(be.getItem(0).getCount()==1&&TycoonFoodContent.index(be.getItem(0))==0,"Quick-move did not stock actual fridge");hold(p,TycoonFoodContent.ITEMS.get(7).get());var r=row(p);r.need("Hunger",40);initialTrust=TycoonDialogue.trust(r);initialSuspicion=TycoonDialogue.suspicion(r);mealTrust=TycoonFoodConfig.TRUST[0].get()*TycoonFoodQuality.preference(r,be.getItem(0));frame(p,TycoonWorld.home(p).center().offset(0,0,3));go(18);});}}
            case 18->{if(age==30)shot("eligible_cake_and_live_labels");server(18,p->{var r=row(p);var a=npc(p);var meal=r.n.getCompound("Meal");fetched|=meal.getString("Status").equals("CARRYING");seated|=a.getVehicle() instanceof TycoonSupperSeat;if(!r.n.hasUUID("LastMealReceipt"))return;check(fetched&&seated&&r.need("Hunger")>55,"Meal skipped physical fetch/seating/nutrition");check(((TycoonBlockEntity)p.serverLevel().getBlockEntity(fridge(p))).getItem(0).isEmpty(),"Meal did not debit stock once");check(Math.abs(TycoonDialogue.trust(r)-(initialTrust+mealTrust))<.002,"Legitimate meal trust differs from food preference");frame(p,a.blockPosition().south(2));go(19);});}
            case 19->{if(age>20){shot("meal_completed_live_stats");var a=clientNpc();check(a!=null,"Gift actor unavailable");mc.gameMode.interact(mc.player,a,InteractionHand.MAIN_HAND);go(20);}}
            case 20->server(20,p->{if(!row(p).n.getBoolean("welcome_home_cake_received"))return;check(count(p,7)==0,"Manual cake not consumed once");check(Math.abs(TycoonDialogue.trust(row(p))-(initialTrust+mealTrust+.20))<.003&&Math.abs(TycoonDialogue.suspicion(row(p))-(initialSuspicion-.20))<.003,"Cake did not apply exact independent effects");try{check(TycoonCheckpoints.home(p)&&TycoonCheckpoints.player(p),"Actual meal/gift checkpoint read-back failed");}catch(java.io.IOException e){throw new IllegalStateException(e);}go(21);});
            case 21->{if(age>30){shot("cake_gift_complete");com.mojang.logging.LogUtils.getLogger().info("TYCOON_FOOD_NATIVE_COMPLETE: declared owner/visitor/balances; real desk Soul food/cake and Power furniture purchases, actual block placement and fridge quick-move, physical fetch/carry/seat/eat/one debit, legitimate meal relationship change, eligible HUD/live labels, actual client manual cake handoff/exact effects, saved home/player read-back; not multiplayer or restart/power-loss acceptance");mc.stop();}}
            default->throw new IllegalStateException("Unknown food QA stage");
        }
    }
    private static ServerPlayer owner(){return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
    private static TycoonData.Resident row(ServerPlayer p){return TycoonWorld.home(p).residents.get(resident);}
    private static TycoonNpc npc(ServerPlayer p){return (TycoonNpc)p.serverLevel().getEntity(actor);}
    private static TycoonNpc clientNpc(){for(var e:mc.level.entitiesForRendering())if(e instanceof TycoonNpc a&&a.getUUID().equals(actor))return a;return null;}
    private static BlockPos table(ServerPlayer p){return TycoonWorld.home(p).center().offset(-2,0,2);}
    private static BlockPos fridge(ServerPlayer p){return TycoonWorld.home(p).center().offset(-3,0,-2);}
    private static boolean personal(){return mc.level!=null&&mc.level.dimension()==TycoonWorld.KEY;}
    private static void server(int expected,Consumer<ServerPlayer> action){if(pending)return;pending=true;mc.getSingleplayerServer().execute(()->{try{if(stage==expected)action.accept(owner());}catch(Throwable e){failure=e;}finally{pending=false;}});}
    private static void go(int value){stage=value;age=0;}
    private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static boolean press(String prefix){if(mc.screen==null)return false;for(var child:mc.screen.children())if(child instanceof Button b&&b.active&&b.getMessage().getString().startsWith(prefix)){b.onPress();return true;}return false;}
    private static void checkLayout(){var buttons=new ArrayList<Button>();for(var child:mc.screen.children())if(child instanceof Button b&&b.visible)buttons.add(b);for(int i=0;i<buttons.size();i++){var a=buttons.get(i);check(a.getX()>=0&&a.getY()>=0&&a.getX()+a.getWidth()<=mc.screen.width&&a.getY()+a.getHeight()<=mc.screen.height,"Shop button outside actual screen: "+a.getMessage().getString());for(int j=i+1;j<buttons.size();j++){var b=buttons.get(j);check(!(a.getX()<b.getX()+b.getWidth()&&b.getX()<a.getX()+a.getWidth()&&a.getY()<b.getY()+b.getHeight()&&b.getY()<a.getY()+a.getHeight()),"Actual shop button overlap: "+a.getMessage().getString()+" / "+b.getMessage().getString());}}}
    private static boolean foodTab(){if(mc.screen instanceof com.coraline.client.TycoonWorldScreen s){if(!s.selectedTab().equals("Food"))press("Food");return true;}if(mc.screen instanceof com.coraline.client.TycoonScreen){press("World");press("Dolls / World");}else if(age%20==0)use(TycoonWorld.home(owner()).center().offset(1,0,-1));return false;}
    private static void use(BlockPos at){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter(),Direction.NORTH,at,false));}
    private static void place(BlockPos at){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(at.below().getCenter().add(0,.5,0),Direction.UP,at.below(),false));}
    private static int count(ServerPlayer p,int index){return TycoonShop.count(p,TycoonFoodContent.ITEMS.get(index).get());}
    private static boolean has(ServerPlayer p,String id){return TycoonShop.count(p,TycoonContent.BLOCKS.get(id).get().asItem())>0;}
    private static void hold(ServerPlayer p,Item item){for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item)){var stack=p.getInventory().getItem(i);p.getInventory().setItem(i,p.getMainHandItem());p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.inventoryMenu.broadcastChanges();return;}throw new IllegalStateException("Purchased item unavailable: "+item);}
    private static void frame(ServerPlayer p,BlockPos at){p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,180,10);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,TycoonWorld.home(p).center().getCenter());}
    private static void setup(ServerPlayer p){p.server.getPlayerList().op(p.getGameProfile());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().setItem(10,new ItemStack(TycoonContent.VOID_POWER.get(),64));var h=TycoonWorld.home(p);h.state.putBoolean("Activated",true);h.state.putLong("ActiveTicks",1000);h.state.putLong("SoulEssence",300);h.state.putBoolean("DebugFoodStats",true);check(TycoonWorld.prepare(p),"Declared platform blocked");TycoonWorld.enter(p);
        var r=TycoonCare.generate(UUID.randomUUID());resident=r.id;r.n.putString("Status","ACTIVE");r.n.putBoolean("DebugChild",true);r.n.putInt("VisitCount",1);r.n.putLong("NextRequest",1000000);r.n.putLong("NextIncident",1000000);TycoonDialogue.set(r,.2,.5);var v=new CompoundTag();v.putUUID("Session",UUID.randomUUID());v.putString("Phase","INSIDE");v.putLong("Started",h.clock());v.putLong("EnteredAt",h.clock());v.putLong("LeaveAfter",h.clock()+12000);v.putLong("LastSeen",h.clock());v.putString("LastDimension",TycoonWorld.KEY.location().toString());v.putLong("LastAt",h.center().asLong());r.n.put("Visit",v);r.n.putUUID("FirstVisitSession",v.getUUID("Session"));h.residents.put(r.id,r);var a=TycoonContent.NPC.get().create(p.serverLevel());a.configure(p.getUUID(),r.id,false,r.n.getInt("Appearance"));a.setPos(h.center().offset(-2,0,0).getCenter());actor=a.getUUID();r.n.putUUID("Entity",actor);p.serverLevel().addFreshEntity(a);TycoonVisitTickets.world(p,a.blockPosition());h.state.putBoolean("VisitActive",true);frame(p,h.center().offset(0,0,1));TycoonWorld.dirty(p);
    }
    private static void shot(String name){try{var folder=java.nio.file.Path.of("artwork/tycoon_food_qa").toAbsolutePath();java.nio.file.Files.createDirectories(folder);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(folder.resolve(name+".png"));}}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private TycoonFoodVisualCheck(){}
}
