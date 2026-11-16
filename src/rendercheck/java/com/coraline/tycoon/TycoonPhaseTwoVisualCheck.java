package com.coraline.tycoon;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Disposable declared housing/resident/resource checkpoints, followed by real
 * desk packets, doll handoffs, mouse navigation, group portals and purchases.
 * Not a full survival playthrough, hour-long placement wait or8–15minute soak.
 */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class TycoonPhaseTwoVisualCheck {
    private static final BlockPos HOUSE=new BlockPos(118000,180,118000);
    private static final UUID[] RESIDENTS=new UUID[2],ACTORS=new UUID[2];
    private static final int[][] TILES={{-1,0},{0,-1},{-1,-1},{1,0},{0,1},{1,1},{-1,1},{1,-1}};
    private static volatile int stage,age;private static volatile boolean pending;private static volatile Throwable failed;
    private static int ticks,tileIndex,houseTier=2,skyTier;private static boolean clicked,sawMice,sawFollowing;
    private static int beforePower;private static double savedTrust,savedSuspicion;private static UUID question;
    private static Minecraft mc;
    private static net.minecraft.client.gui.screens.Screen readyScreen;private static String readyTab;private static int readyAt;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.tycoonPhaseTwoCheck")||event.phase!=TickEvent.Phase.END)return;
        if(Boolean.getBoolean("coraline.tycoonExpansionCheck")){TycoonExpansionVisualCheck.tick();return;}
        if(Boolean.getBoolean("coraline.tycoonFoodCheck")){TycoonFoodVisualCheck.tick();return;}
        mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(failed!=null)throw new IllegalStateException("Tycoon Phase2 stage="+stage,failed);
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<80)return;
        mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);age++;if(++ticks>22000)throw new IllegalStateException("Tycoon Phase2 timed out at stage="+stage);
        if(ticks%100==0)com.mojang.logging.LogUtils.getLogger().info("TYCOON_TWO stage={} age={} position={} screen={}",stage,age,mc.player.position(),mc.screen==null?"none":mc.screen.getClass().getSimpleName());
        switch(stage){
            case 0->server(0,p->{if(Boolean.getBoolean("coraline.tycoonPhaseTwoResume")){resume(p);go(90);}else{setup(p);go(1);}});
            case 1->{if(age>60&&personal()&&worldTab("Dolls"))go(2);}
            case 2->{if(age>30){shot("doll_workshop");press("Create —");go(3);}}
            case 3->server(3,p->{if(!row(p,0).n.getCompound("Doll").hasUUID("Id"))return;check(TycoonShop.count(p,TycoonContent.VOID_POWER.get())==1016,"First real doll fee isn't8");go(4);});
            case 4->{if(age>20){press(">");go(5);}}
            case 5->{if(age>15){press("Create —");go(6);}}
            case 6->server(6,p->{if(!row(p,1).n.getCompound("Doll").hasUUID("Id"))return;check(TycoonShop.count(p,TycoonContent.VOID_POWER.get())==1008,"Second doll fee/delivery wrong");TycoonWorld.leave(p);go(7);});
            case 7->{mc.setScreen(null);if(age>40&&!personal())server(7,p->{holdDoll(p,0);frameChild(p,1);go(8);});}
            case 8->{if(age>12&&interact(1))go(9);}
            case 9->server(9,p->{check(p.getMainHandItem().is(TycoonPhaseTwoContent.DOLL.get())&&p.getMainHandItem().getCount()==1&&!row(p,1).n.getCompound("Doll").getBoolean("Delivered"),"Wrong resident consumed a doll");frameChild(p,0);go(10);});
            case 10->{if(age>12&&interact(0))go(11);}
            case 11->{if(age>25)server(11,p->{check(TycoonDolls.valid(p,row(p,0)),"Matching handoff didn't produce canonical bedside doll");holdDoll(p,1);frameChild(p,1);go(12);});}
            case 12->{if(age>12){shot("first_bedside_doll");if(interact(1))go(13);}}
            case 13->server(13,p->{check(TycoonDolls.valid(p,row(p,1)),"Second doll isn't physically present");p.server.overworld().setDayTime(6000);emptyHand(p);TycoonWorld.enter(p);go(14);});
            case 14->{if(age>40&&personal()&&worldTab("Visits"))server(14,p->{check(!TycoonVisits.nighttime(p)&&TycoonVisits.invite(p).equals("The mice only come at night.")&&!TycoonWorld.home(p).state.getBoolean("VisitActive"),"Daytime invitation bypassed");p.server.overworld().setDayTime(13000);go(15);});}
            case 15->{if(age>35){press("Refresh");go(16);}}
            case 16->{if(age>30&&worldTab("Visits")){shot("nighttime_eligible_group");press("Preview eligible");go(17);}}
            case 17->{if(age>25){press("Confirm Spawn Mice");mc.setScreen(null);go(18);}}
            case 18->server(18,p->{var h=TycoonWorld.home(p);for(int i=0;i<2;i++){var r=row(p,i);sawMice|=r.n.getCompound("Visit").hasUUID("Mouse");sawFollowing|=r.n.getCompound("Visit").getString("Phase").equals("FOLLOWING");}if(TycoonVisits.count(h)!=2||!bothInWorld(p))return;check(sawMice&&sawFollowing,"Group teleported without physical mice/following phase");for(int i=0;i<2;i++){check(p.server.overworld().getEntity(ACTORS[i])==null&&p.server.getLevel(TycoonWorld.KEY).getEntity(ACTORS[i]) instanceof TycoonNpc,"Crossing duplicated/lost NPC identity");check(row(p,i).n.getBoolean("RelationshipInitialized"),"Visitor relationship not initialized");}frameDesk(p);go(19);});
            case 19->{if(age>25){shot("visitors_arrived_same_identity");use(TycoonWorld.home(owner()).center().offset(-1,0,-1));go(20);}}
            case 20->{if(age>25)server(20,p->{check(p.serverLevel().dimension()==TycoonWorld.KEY&&TycoonVisits.count(TycoonWorld.home(p))==2,"Ordinary return bypassed active visitors");frameChild(p,0);go(21);});}
            case 21->{if(age>12&&interact(0))go(22);}
            case 22->{if(age>30){check(mc.screen instanceof com.coraline.client.TycoonConversationScreen,"Visitor click didn't open conversation");shot("visitor_contextual_dialogue");server(22,p->{question=row(p,0).n.getUUID("Question");go(23);});}}
            case 23->{if(age>10){press("I'm still building");go(24);}}
            case 24->{if(age>30){shot("visitor_honest_response");server(24,p->{var r=row(p,0);check(r.n.getList("DialogueHistory",10).size()==1&&TycoonDialogue.trust(r)>.1,"Real dialogue response didn't apply");double t=TycoonDialogue.trust(r),s=TycoonDialogue.suspicion(r);TycoonDialogue.respond(p,r,question,0);check(t==TycoonDialogue.trust(r)&&s==TycoonDialogue.suspicion(r),"Response replay altered relationship");frameDesk(p);go(25);});mc.setScreen(null);}}
            case 25->{if(age>30&&worldTab("Visits")){press("Send All Visitors Home");mc.setScreen(null);go(26);}}
            case 26->server(26,p->{if(TycoonVisits.count(TycoonWorld.home(p))!=0||TycoonWorld.home(p).residents.values().stream().anyMatch(TycoonVisits::visiting))return;for(int i=0;i<2;i++)check(p.server.overworld().getEntity(ACTORS[i]) instanceof TycoonNpc&&TycoonDolls.valid(p,row(p,i)),"Normal return lost NPC/doll");frameDesk(p);go(27);});
            case 27->{if(age>25&&worldTab("Terrain")){beforePower=-1;server(27,p->{beforePower=TycoonShop.count(p,TycoonContent.VOID_POWER.get());go(28);});}}
            case 28->{if(age>10){selectTile(0,0);press("Confirm —");go(29);}}
            case 29->server(29,p->{var h=TycoonWorld.home(p);if(h.state.contains("BuildJob")||!h.state.getCompound("Terrain").contains("0,0"))return;check(TycoonShop.count(p,TycoonContent.VOID_POWER.get())==beforePower-16,"Terrain fee wasn't real");mc.execute(()->mc.setScreen(null));frameOverview(p,0);go(30);});
            case 30->{if(age>35){shot("grassland_expansion");server(30,p->{frameDesk(p);var h=TycoonWorld.home(p);var at=h.center().offset(10,TycoonLandscape.height(h,10,10)-h.center().getY(),10);var old=p.serverLevel().getBlockState(at);p.serverLevel().setBlock(at,Blocks.GOLD_BLOCK.defaultBlockState(),2);TycoonPhaseTwo.edit(p,at,Blocks.GOLD_BLOCK.defaultBlockState());int power=TycoonShop.count(p,TycoonContent.VOID_POWER.get());String refusal=TycoonLandscape.purchase(p,0,0,3);check(refusal.contains("conflicts")&&!h.state.contains("BuildJob")&&TycoonShop.count(p,TycoonContent.VOID_POWER.get())==power&&p.serverLevel().getBlockState(at).is(Blocks.GOLD_BLOCK),"Protected edit overwritten/spent: "+refusal);p.serverLevel().setBlock(at,old,2);TycoonPhaseTwo.edit(p,at,old);go(31);});}}
            case 31->{if(age>25&&worldTab("Terrain")){selectTile(0,0);press(">");press(">");press("Confirm —");go(32);}}
            case 32->server(32,p->{var h=TycoonWorld.home(p);if(h.state.contains("BuildJob")||h.state.getCompound("Terrain").getCompound("0,0").getInt("Tier")!=3)return;frameDesk(p);go(33);});
            case 33->{if(age>25&&worldTab("House")){press("Build / upgrade");go(34);}}
            case 34->server(34,p->{var h=TycoonWorld.home(p);if(h.state.contains("BuildJob")||h.state.getInt("HouseTier")!=1)return;check(p.serverLevel().getBlockState(h.center().offset(1,0,-1)).is(TycoonContent.DESK.get())&&p.serverLevel().getBlockState(h.center().offset(-1,0,-1)).is(TycoonContent.DOOR.get()),"Cottage buried/destroyed core");frameOverview(p,1);mc.execute(()->mc.setScreen(null));go(35);});
            case 35->{if(age>35){shot("garden_cottage_white_sky");server(35,p->{frameDesk(p);go(36);});}}
            case 36->{if(age>25&&worldTab("Sky")){press(">");press(">");press(">");press("Select / purchase");go(37);}}
            case 37->server(37,p->{if(TycoonWorld.home(p).state.getInt("SkyTier")!=3)return;frameOverview(p,1);mc.execute(()->mc.setScreen(null));go(38);});
            case 38->{if(age>40){shot("magical_sunset_cottage");server(38,p->{frameDesk(p);go(39);});}}
            case 39->{if(age>25&&worldTab("Terrain")){if(tileIndex>=TILES.length){server(39,p->go(41));break;}selectTile(TILES[tileIndex][0],TILES[tileIndex][1]);press("Confirm —");go(40);}}
            case 40->server(40,p->{var h=TycoonWorld.home(p);if(h.state.contains("BuildJob")||!h.state.getCompound("Terrain").contains(TycoonLandscape.key(TILES[tileIndex][0],TILES[tileIndex][1])))return;tileIndex++;go(39);});
            case 41->server(41,p->{frameDesk(p);String result=TycoonLandscape.purchase(p,0,0,5);check(result.startsWith("Building"),"Enchanted replacement rejected: "+result);go(42);});
            case 42->server(42,p->{var h=TycoonWorld.home(p);if(h.state.contains("BuildJob")||h.state.getCompound("Terrain").getCompound("0,0").getInt("Tier")!=5)return;go(43);});
            case 43->server(43,p->{frameDesk(p);String result=TycoonHouses.purchase(p,houseTier);check(result.startsWith("Building"),"House tier"+houseTier+" refused: "+result);go(44);});
            case 44->server(44,p->{var h=TycoonWorld.home(p);if(h.state.contains("BuildJob")||h.state.getInt("HouseTier")!=houseTier)return;if(++houseTier<=5)go(43);else{frameOverview(p,5);mc.execute(()->mc.setScreen(null));go(45);}});
            case 45->{if(age>40){shot("mansion_enchanted_terrain");server(45,p->{frameDesk(p);go(46);});}}
            case 46->server(46,p->{frameDesk(p);String result=TycoonSkies.select(p,skyTier);check(result.startsWith("Purchased"),"Sky purchase/select refused: "+result);frameOverview(p,5);go(47);});
            case 47->{if(age>35){shot("sky_tier_"+skyTier);if(++skyTier<=5)go(46);else server(47,p->{frameDesk(p);p.server.overworld().setDayTime(13000);row(p,0).need("Neglect",60);row(p,1).need("Neglect",10);savedTrust=TycoonDialogue.trust(row(p,0));savedSuspicion=TycoonDialogue.suspicion(row(p,0));go(48);});}}
            case 48->{if(age>25&&worldTab("Visits")){press("Refresh");go(49);}}
            case 49->{if(age>30){press("Preview eligible");go(50);}}
            case 50->{if(age>25){press("Confirm Spawn Mice");mc.setScreen(null);go(51);}}
            case 51->server(51,p->{if(!row(p,0).n.getCompound("Visit").getString("Phase").equals("INSIDE"))return;check(TycoonVisits.count(TycoonWorld.home(p))==1&&row(p,1).n.getInt("VisitCount")==1&&row(p,0).n.getInt("VisitCount")==2,"Second invitation selected ineligible resident");check(Math.abs(TycoonDialogue.trust(row(p,0))-savedTrust)<.005&&Math.abs(TycoonDialogue.suspicion(row(p,0))-savedSuspicion)<.005,"Second visit reset hidden relationship");frameDesk(p);go(52);});
            case 52->{if(age>25&&worldTab("Visits")){press("Send All Visitors Home");go(53);}}
            case 53->server(53,p->{var n=row(p,0).n.getCompound("Visit");if(!n.getString("Phase").equals("LEAVING"))return;n.putLong("LeavingAt",TycoonWorld.home(p).clock()-1300);go(54);});
            case 54->server(54,p->{if(TycoonVisits.visiting(row(p,0)))return;check(p.server.overworld().getEntity(ACTORS[0]) instanceof TycoonNpc,"Bounded return recovery lost original identity");frameDesk(p);go(55);});
            case 55->{mc.setScreen(null);if(age>25){use(TycoonWorld.home(owner()).center().offset(-1,0,-1));go(56);}}
            case 56->{if(age>35&&!personal())server(56,p->{saveProof(p);go(57);});}
            case 57->{if(age>20){com.mojang.logging.LogUtils.getLogger().info("TYCOON_PHASE_TWO_NATIVE_COMPLETE: declared housing/residents/resources; real doll workshop purchases, wrong/right handoffs and bedside actors, day rejection, physical mice/group same-UUID crossings, hidden contextual dialogue and replay rejection, protected terrain refusal, nine connected chunks/procedural upgrade, five house tiers/core preservation, six actual sky selections, second-visit relationships/eligibility, physical normal returns and declared timeout recovery, disk checkpoint; not a whole survival playthrough or duration soak");mc.stop();}}
            case 90->{if(age>50)server(90,p->{checkProof(p);TycoonWorld.enter(p);go(91);});}
            case 91->{if(age>80&&personal()){shot("resumed_mansion_sky_and_world");server(91,p->{checkProof(p);go(92);});}}
            case 92->{if(age>20){com.mojang.logging.LogUtils.getLogger().info("TYCOON_PHASE_TWO_RESUME_COMPLETE: restarted tool-owned disposable world retained doll/resident UUIDs, relationships/history, purchased terrain, house, sky and real return links; no user save involved");mc.stop();}}
            default->throw new IllegalStateException("Unknown Phase2 QA stage");
        }
    }
    private static void server(int expected,Consumer<ServerPlayer> action){if(pending)return;pending=true;mc.getSingleplayerServer().execute(()->{try{if(stage==expected)action.accept(owner());}catch(Throwable failure){failed=failure;}finally{pending=false;}});}
    private static void go(int next){stage=next;age=0;clicked=false;}
    private static ServerPlayer owner(){return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
    private static TycoonData.Resident row(ServerPlayer p,int i){return TycoonWorld.home(p).residents.get(RESIDENTS[i]);}
    private static boolean personal(){return mc.level!=null&&mc.level.dimension()==TycoonWorld.KEY;}
    private static boolean bothInWorld(ServerPlayer p){return row(p,0).n.getCompound("Visit").getString("Phase").equals("INSIDE")&&row(p,1).n.getCompound("Visit").getString("Phase").equals("INSIDE");}
    private static void check(boolean condition,String error){if(!condition)throw new IllegalStateException(error);}
    private static void use(BlockPos at){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter(),Direction.NORTH,at,false));}
    private static boolean press(String prefix){if(mc.screen==null)return false;for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith(prefix)&&b.active){b.onPress();return true;}return false;}
    private static boolean worldTab(String tab){if(mc.screen instanceof com.coraline.client.TycoonWorldScreen screen){if(readyScreen!=screen||!tab.equals(readyTab)){readyScreen=screen;readyTab=tab;readyAt=ticks+6;}if(!screen.selectedTab().equals(tab)){press(tab);readyAt=ticks+6;return false;}return ticks>=readyAt;}if(mc.screen instanceof com.coraline.client.TycoonScreen){press("World");press("Dolls / World");}else if(age%20==0)use(TycoonWorld.home(owner()).center().offset(1,0,-1));return false;}
    private static void selectTile(int x,int z){var screen=mc.screen;check(screen instanceof com.coraline.client.TycoonWorldScreen,"No real terrain map");int w=Math.min(680,screen.width-16),h=Math.min(360,screen.height-16),cell=Math.max(4,Math.min(12,(h-106)/16));screen.mouseClicked((screen.width-w)/2+12+(x+8)*cell+cell/2.,(screen.height-h)/2+56+(z+8)*cell+cell/2.,0);}
    private static boolean interact(int index){if(mc.level==null)return false;for(var entity:mc.level.entitiesForRendering())if(entity instanceof TycoonNpc a&&a.getUUID().equals(ACTORS[index])&&mc.player.distanceToSqr(a)<25){mc.gameMode.interact(mc.player,a,InteractionHand.MAIN_HAND);return true;}return false;}
    private static TycoonNpc actor(ServerPlayer p,int index){for(var level:p.server.getAllLevels())if(level.getEntity(ACTORS[index]) instanceof TycoonNpc a)return a;return null;}
    private static void frameChild(ServerPlayer p,int index){var a=actor(p,index);check(a!=null,"Canonical actor unavailable");var l=(ServerLevel)a.level();var at=TycoonWorld.landing(l,a.blockPosition().south(2));check(at!=null,"No safe observer position");p.setNoGravity(false);p.teleportTo(l,at.getX()+.5,at.getY(),at.getZ()+.5,180,0);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,a.getEyePosition());}
    private static void frameDesk(ServerPlayer p){var h=TycoonWorld.home(p);p.setNoGravity(false);p.teleportTo(p.server.getLevel(TycoonWorld.KEY),h.center().getX()+.5,h.center().getY(),h.center().getZ()+1.5,180,10);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,h.center().offset(1,0,-1).getCenter());}
    private static void frameOverview(ServerPlayer p,int house){var h=TycoonWorld.home(p);p.setNoGravity(true);int distance=house>=5?32:house>0?11:8;p.teleportTo(p.server.getLevel(TycoonWorld.KEY),h.center().getX()+distance,h.center().getY()+distance*.45,h.center().getZ()+distance,135,20);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,h.center().above(house>=5?4:1).getCenter());}
    private static void emptyHand(ServerPlayer p){var held=p.getMainHandItem();if(!held.isEmpty()){int slot=p.getInventory().getFreeSlot();check(slot>=0,"Fixture inventory full");p.getInventory().setItem(slot,held);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}p.inventoryMenu.broadcastChanges();}
    private static void holdDoll(ServerPlayer p,int index){emptyHand(p);for(int slot=0;slot<36;slot++){var item=p.getInventory().getItem(slot);if(item.is(TycoonPhaseTwoContent.DOLL.get())&&item.hasTag()&&item.getTag().getUUID("Resident").equals(RESIDENTS[index])){p.getInventory().setItem(slot,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,item);p.inventoryMenu.broadcastChanges();return;}}throw new IllegalStateException("Crafted real doll missing from inventory");}
    private static void fixtureBlock(ServerPlayer p,String id,BlockPos at){TycoonWorld.placeOwned(p.server.overworld(),at,TycoonContent.BLOCKS.get(id).get(),p.getUUID());var n=TycoonWorld.home(p).state.getCompound("Furniture");n.putString(Long.toString(at.asLong()),id);TycoonWorld.home(p).state.put("Furniture",n);}
    private static void setup(ServerPlayer p){
        p.server.getPlayerList().op(p.getGameProfile());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var l=p.server.overworld();p.getInventory().clearContent();for(int i=10;i<26;i++)p.getInventory().setItem(i,new ItemStack(TycoonContent.VOID_POWER.get(),64));
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){var at=HOUSE.offset(x,0,z);l.getChunkAt(at);l.setBlock(at,Blocks.OAK_PLANKS.defaultBlockState(),2);for(int y=1;y<=6;y++)l.setBlock(at.above(y),(y==5&&Math.abs(x)<=6&&Math.abs(z)<=6||y<5&&(Math.abs(x)==6||Math.abs(z)==6)&&Math.abs(x)<=6&&Math.abs(z)<=6)?Blocks.OAK_PLANKS.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
        for(int y=1;y<=2;y++)l.setBlock(HOUSE.offset(0,y,6),Blocks.AIR.defaultBlockState(),2);var h=TycoonWorld.home(p);h.state.putBoolean("Activated",true);h.state.putLong("ActiveTicks",10000);h.state.putLong("Entrance",HOUSE.offset(0,1,2).asLong());TycoonWorld.placeOwned(l,h.entrance(),TycoonContent.DOOR.get(),p.getUUID());var bed=HOUSE.offset(3,1,3);l.setBlock(bed,Blocks.BLUE_BED.defaultBlockState(),2);p.setRespawnPosition(l.dimension(),bed,0,false,false);
        fixtureBlock(p,"orphan_bed",HOUSE.offset(-3,1,-2));fixtureBlock(p,"orphan_bed",HOUSE.offset(3,1,-2));fixtureBlock(p,"orphan_food_table",HOUSE.offset(0,1,-2));fixtureBlock(p,"orphan_fridge",HOUSE.offset(-3,1,2));fixtureBlock(p,"water_station",HOUSE.offset(3,1,1));fixtureBlock(p,"orphan_lamp",HOUSE.offset(-1,1,3));fixtureBlock(p,"toy_box",HOUSE.offset(-3,1,3));((TycoonBlockEntity)l.getBlockEntity(HOUSE.offset(-3,1,2))).setItem(0,new ItemStack(Items.BREAD,32));((TycoonBlockEntity)l.getBlockEntity(HOUSE.offset(3,1,1))).water=32;
        for(int i=0;i<2;i++){var r=TycoonCare.generate(UUID.randomUUID());RESIDENTS[i]=r.id;r.n.putString("Status","ACTIVE");r.n.putBoolean("DebugChild",true);r.need("Neglect",60);r.n.putLong("Bed",HOUSE.offset(i==0?-3:3,1,-2).asLong());r.n.putLong("NextRequest",1000000);r.n.putLong("NextIncident",1000000);var a=TycoonContent.NPC.get().create(l);a.configure(p.getUUID(),r.id,false,r.n.getInt("Appearance"));a.setPos(HOUSE.offset(i==0?-2:2,1,-1).getCenter());l.addFreshEntity(a);ACTORS[i]=a.getUUID();r.n.putUUID("Entity",a.getUUID());h.residents.put(r.id,r);}
        p.teleportTo(l,HOUSE.getX()+.5,HOUSE.getY()+1,HOUSE.getZ()+4.5,180,0);TycoonWorld.prepare(p);TycoonWorld.enter(p);TycoonWorld.dirty(p);
    }
    private static void saveProof(ServerPlayer p){var h=TycoonWorld.home(p);var proof=new CompoundTag();var kids=new ListTag();for(int i=0;i<2;i++){var r=row(p,i);var n=new CompoundTag();n.putUUID("Resident",r.id);n.putUUID("Entity",ACTORS[i]);n.putUUID("Doll",r.n.getCompound("Doll").getUUID("Id"));n.putDouble("Trustness",TycoonDialogue.trust(r));n.putDouble("Suspicion",TycoonDialogue.suspicion(r));n.putInt("Visits",r.n.getInt("VisitCount"));kids.add(n);}proof.put("Residents",kids);proof.put("Terrain",h.state.getCompound("Terrain").copy());proof.putInt("HouseTier",h.state.getInt("HouseTier"));proof.putInt("SkyTier",h.state.getInt("SkyTier"));proof.putLong("Entrance",h.entrance().asLong());h.state.put("PhaseTwoQAProof",proof);p.getPersistentData().putBoolean("TycoonTwoQAReady",true);p.server.saveEverything(true,true,true);try{check(TycoonCheckpoints.home(p)&&TycoonCheckpoints.player(p),"Final disk read-back didn't match");}catch(java.io.IOException failure){throw new IllegalStateException(failure);} }
    private static void resume(ServerPlayer p){check(p.getPersistentData().getBoolean("TycoonTwoQAReady"),"Resume opened an unrelated/new save");var h=TycoonWorld.home(p);var proof=h.state.getCompound("PhaseTwoQAProof");check(!proof.isEmpty(),"Resume lacks proof");var kids=proof.getList("Residents",10);for(int i=0;i<2;i++){RESIDENTS[i]=kids.getCompound(i).getUUID("Resident");ACTORS[i]=kids.getCompound(i).getUUID("Entity");}checkProof(p);}
    private static void checkProof(ServerPlayer p){var h=TycoonWorld.home(p);var proof=h.state.getCompound("PhaseTwoQAProof");check(h.state.getCompound("Terrain").equals(proof.getCompound("Terrain"))&&h.state.getInt("HouseTier")==proof.getInt("HouseTier")&&h.state.getInt("SkyTier")==proof.getInt("SkyTier")&&h.entrance().asLong()==proof.getLong("Entrance"),"Restart lost purchased world/portal link");for(int i=0;i<2;i++){var r=row(p,i);var saved=proof.getList("Residents",10).getCompound(i);check(r!=null&&r.n.getUUID("Entity").equals(ACTORS[i])&&r.n.getCompound("Doll").getUUID("Id").equals(saved.getUUID("Doll"))&&TycoonDialogue.trust(r)==saved.getDouble("Trustness")&&TycoonDialogue.suspicion(r)==saved.getDouble("Suspicion")&&r.n.getInt("VisitCount")==saved.getInt("Visits"),"Restart reset resident/doll/relationship history");}}
    private static void shot(String name){try{var folder=java.nio.file.Path.of("artwork/tycoon_phase2_qa").toAbsolutePath();java.nio.file.Files.createDirectories(folder);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(folder.resolve(name+".png"));}}catch(java.io.IOException failure){throw new IllegalStateException(failure);}}
}
