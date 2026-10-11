package com.coraline.feast;

import static com.coraline.feast.ExpeditionActivities.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.*;
import com.coraline.registry.ModItems;
import java.util.*;

/** Whitelisted deposits; no vanilla block-breaking permission is ever enabled. */
public final class MineshaftActivity {
    public record Room(int x,int y,int z,int tier,int theme){}
    public record Ore(BlockPos offset,int kind,int tier){public int ticks(){return Math.min(20,6+kind*2);}}
    public record Plan(List<Room> rooms,List<Ore> ores){}
    private static final Map<UUID,Map<BlockPos,Ore>> ORES=new HashMap<>();
    private static final Map<UUID,Dig> DIG=new HashMap<>();
    private static final Map<UUID,ServerBossEvent> BARS=new HashMap<>();
    private static final class Dig {BlockPos pos;long heartbeat;int ticks;Dig(BlockPos pos,long t){this.pos=pos;heartbeat=t;}}
    static final Block[] BLOCKS={Blocks.COAL_ORE,Blocks.COPPER_ORE,Blocks.IRON_ORE,Blocks.REDSTONE_ORE,Blocks.LAPIS_ORE,Blocks.GOLD_ORE,Blocks.DIAMOND_ORE,Blocks.EMERALD_ORE,Blocks.BUDDING_AMETHYST,Blocks.AMETHYST_BLOCK,Blocks.DEEPSLATE_DIAMOND_ORE};
    static Item loot(int kind){return switch(kind){case 0->Items.COAL;case 1->Items.RAW_COPPER;case 2->Items.RAW_IRON;case 3->Items.REDSTONE;case 4->Items.LAPIS_LAZULI;case 5->Items.RAW_GOLD;case 6->Items.DIAMOND;case 7->Items.EMERALD;case 8->Items.AMETHYST_SHARD;case 9->com.coraline.clothing.OtherMaterials.MOONSTONE.get();case 10->com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get();default->com.coraline.clothing.OtherMaterials.OTHER_IRON.get();};}
    public static Plan generate(long seed){
        var rng=new Random(seed);var rooms=new ArrayList<Room>();var ores=new ArrayList<Ore>();
        for(var cell:MineMaze.generate(seed).cells()){
            var at=cell.pos();int t=cell.tier();rooms.add(new Room(at.getX(),at.getY(),at.getZ(),t,rng.nextInt(5)));
            for(int side:new int[]{-1,1})for(int z:new int[]{-2,2}){
                int roll=rng.nextInt(100),kind=t==0?(roll<45?0:roll<75?1:2):t==1?(roll<40?2:roll<70?3:roll<90?4:5):t==2?(roll<30?4:roll<65?5:roll<85?6:7):(roll<25?5:roll<50?6:roll<75?7:8+rng.nextInt(3));
                ores.add(new Ore(at.offset(side*4,1,z),kind,t));
            }
        }
        return new Plan(List.copyOf(rooms),List.copyOf(ores));
    }
    static int floor(int n){return -14*Math.min(3,Math.max(0,n/64));}
    static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        MineMaze.build(l,c,MineMaze.generate(state(s).getLong("Seed")));
        var map=new HashMap<BlockPos,Ore>();state(s).putBoolean("MoonstoneUnlocked",knownMaterial(p,"moonstone"));state(s).putBoolean("DiamondUnlocked",knownMaterial(p,"other_diamond"));
        for(var candidate:generate(state(s).getLong("Seed")).ores()){
            var ore=eligible(candidate,state(s));var at=c.offset(ore.offset());put(l,at,0,0,0,BLOCKS[ore.kind()]);map.put(at,ore);
        }
        ORES.put(s.owner,map);
    }
    // No new late-game progression bypass: a rare material appears only after the owner already has it or its equipment.
    static boolean knownMaterial(ServerPlayer p,String prefix){for(int i=0;i<p.getInventory().getContainerSize();i++){var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(p.getInventory().getItem(i).getItem());if(id!=null&&id.getNamespace().equals("coraline")&&(id.getPath().equals(prefix)||id.getPath().startsWith(prefix+"_")))return true;}return false;}
    static Ore eligible(Ore ore,CompoundTag n){return ore.kind==9&&!n.getBoolean("MoonstoneUnlocked")||ore.kind==10&&!n.getBoolean("DiamondUnlocked")?new Ore(ore.offset,8,ore.tier):ore;}
    static void begin(ServerPlayer p,VisitData.Scene s){
        borrow(p,s,ModItems.ACTIVITY_MINING_PICK.get());var n=state(s);
        n.putLong("End",OtherMotherLoop.now(p)+24000);n.putInt("Hearts",5);
        int slot=(p.getInventory().selected+1)%9;
        n.putInt("LanternSlot",slot);n.put("LanternStored",p.getInventory().getItem(slot).save(new CompoundTag()));n.putBoolean("LanternBorrowed",true);
        n.put("MineOffhand",p.getOffhandItem().save(new CompoundTag()));p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
        var lamp=new ItemStack(com.coraline.clothing.UtilityItems.MOONLIT_LANTERN.get());lamp.getOrCreateTag().putBoolean("MineLoan",true);
        p.getInventory().setItem(slot,lamp);p.getInventory().setChanged();
        var extra=new ListTag();
        Item[] tools={ModItems.ACTIVITY_MINING_SHOVEL.get(),ModItems.ACTIVITY_MINE_MAP.get()};
        for(int i=0;i<tools.length;i++){
            int loanSlot=(p.getInventory().selected+2+i)%9;var saved=new CompoundTag();
            saved.putInt("Slot",loanSlot);saved.put("Stack",p.getInventory().getItem(loanSlot).save(new CompoundTag()));extra.add(saved);
            var tool=new ItemStack(tools[i]);tool.getOrCreateTag().putUUID("ExpeditionOwner",s.owner);tool.getOrCreateTag().putInt("Generation",s.loop.generation);
            p.getInventory().setItem(loanSlot,tool);
        }
        n.put("MineExtraStored",extra);p.inventoryMenu.broadcastChanges();
        ExpeditionNetwork.mine(p,5);
        ActivityScenery.title(p,"OTHER MINESHAFT","20 minutes. Five hearts. Return here to keep your haul.");ActivityWorld.ensureExit(p,s);
    }
    public static void hold(ServerPlayer p,BlockPos at,boolean held){var s=OtherMotherLoop.active(p);if(!owns(p,s)||s.loop.activity!=OtherMotherActivities.Activity.OTHER_MINESHAFT)return;if(!held){reset(p);return;}
        if(state(s).getInt("Phase")!=1||OtherMotherLoop.now(p)>=state(s).getLong("End")||!validTool(p,s,ModItems.ACTIVITY_MINING_PICK.get())||p.getEyePosition().distanceToSqr(Vec3.atCenterOf(at))>30)return;
        var hit=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(5.5)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        if(!hit.getBlockPos().equals(at)||!ORES.getOrDefault(s.owner,Map.of()).containsKey(at))return;
        var dig=DIG.get(p.getUUID());if(dig==null||!dig.pos.equals(at)){reset(p);dig=new Dig(at,OtherMotherLoop.now(p));DIG.put(p.getUUID(),dig);}dig.heartbeat=OtherMotherLoop.now(p);
    }
    private static void reset(ServerPlayer p){var d=DIG.remove(p.getUUID());if(d!=null)p.serverLevel().destroyBlockProgress(p.getId(),d.pos,-1);}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){var n=state(s);if(!bounds(c,s.loop.activity).inflate(-1).contains(p.position())){if(n.getInt("Phase")==1){fail(p,s,"You fell outside the mine.");return;}ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));}if(n.getInt("Phase")!=1)return;
        long now=OtherMotherLoop.now(p),left=n.getLong("End")-now;if(left<=0){fail(p,s,"You did not return before the deadline.");return;}
        if(!ORES.containsKey(s.owner)){fail(p,s,"The expedition was interrupted.");return;}
        enforceHands(p,s);
        if(p.tickCount%10==0)ExpeditionNetwork.mine(p,n.getInt("Hearts"));
        MineThreats.tick(p,s,c,now);
        if(n.getInt("Phase")!=1)return;
        var bar=BARS.computeIfAbsent(s.owner,id->new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.PROGRESS));bar.addPlayer(p);bar.setProgress(Math.max(0,Math.min(1,left/24000F)));if(p.tickCount%10==0)bar.setName(Component.literal(String.format("%d:%02d • Bag: %d • Return to the entrance",left/1200,(left/20)%60,n.getInt("Count"))));
        if(left==1200||left==600||left==200){ActivityScenery.title(p,"",left/20+" seconds remaining");p.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.PLAYERS,.6F,1);}
        var d=DIG.get(s.owner);if(d==null)return;var ore=ORES.getOrDefault(s.owner,Map.of()).get(d.pos);
        if(ore==null||now-d.heartbeat>8||!validTool(p,s,ModItems.ACTIVITY_MINING_PICK.get())||p.getEyePosition().distanceToSqr(Vec3.atCenterOf(d.pos))>30||!p.level().getBlockState(d.pos).is(BLOCKS[ore.kind])){reset(p);return;}
        d.ticks++;p.serverLevel().destroyBlockProgress(p.getId(),d.pos,Math.min(9,d.ticks*10/ore.ticks()));
        if(d.ticks%5==0)p.playNotifySound(SoundEvents.STONE_HIT,SoundSource.BLOCKS,.5F,1.2F);
        if(d.ticks>=ore.ticks())harvest(p,s,d.pos,ore);
    }
    static void harvest(ServerPlayer p,VisitData.Scene s,BlockPos at,Ore ore){var map=ORES.get(s.owner);if(map==null||map.remove(at)==null)return;var n=state(s);var bag=n.getList("Bag",10);bag.add(new ItemStack(loot(ore.kind)).save(new CompoundTag()));n.put("Bag",bag);n.putInt("Count",n.getInt("Count")+1);OtherMotherLoop.changed(p);
        reset(p);ActivityBlocks.set(p.serverLevel(),at,(ore.tier<2?Blocks.STONE:Blocks.DEEPSLATE).defaultBlockState(),2);p.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,BLOCKS[ore.kind].defaultBlockState()),at.getX()+.5,at.getY()+.5,at.getZ()+.5,12,.3,.3,.3,.1);p.playNotifySound(ore.kind>=6?SoundEvents.AMETHYST_BLOCK_BREAK:SoundEvents.STONE_BREAK,SoundSource.BLOCKS,.65F,1.1F);if(ore.kind>=6)p.displayClientMessage(Component.literal(loot(ore.kind).getDescription().getString()+" +1"),true);
    }
    /** Successful extraction is allowed only through the entrance portal before time expires. */
    static void settle(ServerPlayer p,VisitData.Scene s){
        var n=state(s);if(n.getBoolean("Paid"))return;
        boolean success=n.getInt("Phase")==1&&n.getInt("Hearts")>0&&OtherMotherLoop.now(p)<n.getLong("End")
            &&p.position().distanceToSqr(Vec3.atBottomCenterOf(center(p,s).south(7)))<36;
        n.putBoolean("Paid",true);var bag=n.getList("Bag",10).copy();n.remove("Bag");
        if(success){var pending=p.getPersistentData().getList("CoralineMineDelivery",10);for(var t:bag)pending.add(t);p.getPersistentData().put("CoralineMineDelivery",pending);}
        n.putInt("Phase",success?4:5);restoreLantern(p,s);OtherMotherLoop.changed(p);
    }
    public static void deliver(ServerPlayer p){var tags=p.getPersistentData().getList("CoralineMineDelivery",10).copy();p.getPersistentData().remove("CoralineMineDelivery");for(var tag:tags)ActivityTokens.give(p,ItemStack.of((CompoundTag)tag));}
    static void restoreLantern(ServerPlayer p,VisitData.Scene s){
        var n=state(s);if(!n.getBoolean("LanternBorrowed"))return;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(ExpeditionActivities.temporary(stack))p.getInventory().setItem(i,ItemStack.EMPTY);}
        for(var entry:n.getList("MineExtraStored",10)){
            var saved=(CompoundTag)entry;var stack=ItemStack.of(saved.getCompound("Stack"));int slot=saved.getInt("Slot");
            if(p.getInventory().getItem(slot).isEmpty())p.getInventory().setItem(slot,stack);else if(!stack.isEmpty())ActivityTokens.give(p,stack);
        }
        n.remove("MineExtraStored");
        n.putBoolean("LanternBorrowed",false);var original=ItemStack.of(n.getCompound("LanternStored"));n.remove("LanternStored");
        if(n.contains("LanternSlot")){int slot=n.getInt("LanternSlot");if(p.getInventory().getItem(slot).isEmpty())p.getInventory().setItem(slot,original);else if(!original.isEmpty())ActivityTokens.give(p,original);}
        else if(!original.isEmpty())ActivityTokens.give(p,original);
        var offhand=ItemStack.of(n.getCompound("MineOffhand"));n.remove("MineOffhand");
        if(p.getOffhandItem().isEmpty())p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,offhand);else if(!offhand.isEmpty())ActivityTokens.give(p,offhand);
        for(var saved:n.getList("MineDisplaced",10)){var stack=ItemStack.of((CompoundTag)saved);if(!ExpeditionActivities.temporary(stack))ActivityTokens.give(p,stack);}n.remove("MineDisplaced");
    }
    static void enforceHands(ServerPlayer p,VisitData.Scene s){
        var off=p.getOffhandItem();
        if(!off.is(com.coraline.clothing.UtilityItems.MOONLIT_LANTERN.get())&&!off.is(ModItems.ACTIVITY_MINING_PICK.get()))return;
        var moved=off.copy();p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
        if(!p.getInventory().add(moved)&&!moved.isEmpty()){
            // No item loss even if an inventory packet fills the vacated slot.
            var n=state(s);var saved=n.getList("MineDisplaced",10);saved.add(moved.save(new CompoundTag()));n.put("MineDisplaced",saved);
        }
        p.inventoryMenu.broadcastChanges();
    }
    static void fail(ServerPlayer p,VisitData.Scene s,String why){
        var n=state(s);if(n.getBoolean("Paid"))return;n.putBoolean("Paid",true);n.putInt("Phase",5);n.putString("Failure",why);n.remove("Bag");n.putInt("Count",0);
        clear(p,s);restoreLantern(p,s);restore(p,s);OtherMotherLoop.changed(p);
        if(ActivityWorld.isActivity(p.level())){ActivityScenery.teleport(p,Vec3.atBottomCenterOf(center(p,s)));ActivityScenery.title(p,"EXPEDITION LOST",why+" Your mining bag is lost.");ActivityWorld.ensureExit(p,s);}
    }
    static void finish(ServerPlayer p,VisitData.Scene s,boolean show){fail(p,s,"Expedition ended.");}
    static void clear(ServerPlayer p,VisitData.Scene s){ExpeditionNetwork.mine(p,-1);reset(p);var b=BARS.remove(s.owner);if(b!=null)b.removeAllPlayers();forget(s.owner);}
    static void forget(UUID owner){ORES.remove(owner);DIG.remove(owner);var b=BARS.remove(owner);if(b!=null)b.removeAllPlayers();}
    public static boolean cart(ServerPlayer p,net.minecraft.world.entity.Entity e){if(!e.getPersistentData().getBoolean("ExpeditionCart"))return false;var s=OtherMotherLoop.active(p);if(owns(p,s)&&s.loop.activity==OtherMotherActivities.Activity.OTHER_MINESHAFT&&state(s).getInt("Phase")==1&&ActivityScenery.owned(e,s)&&e.getPersistentData().getInt("Generation")==s.loop.generation&&p.distanceToSqr(e)<25){if(p.getVehicle()==e)return true;var c=center(p,s);int n=(int)(c.getZ()-e.getZ());e.getPersistentData().putInt("MineStop",Math.min(262,((n+16)/64+1)*64+10));p.startRiding(e,true);e.setDeltaMovement(0,0,-.35);}return true;}
    public static void openMap(ServerPlayer p){
        var s=OtherMotherLoop.active(p);
        if(owns(p,s)&&s.loop.activity==OtherMotherActivities.Activity.OTHER_MINESHAFT&&state(s).getInt("Phase")==1&&validTool(p,s,ModItems.ACTIVITY_MINE_MAP.get()))
            ExpeditionNetwork.map(p,state(s).getLong("Seed"),center(p,s));
    }
    /** Activity enemies are invulnerable to vanilla damage; only this owner/tool rule damages them. */
    public static void attack(ServerPlayer p,net.minecraft.world.entity.Entity target){
        var s=OtherMotherLoop.active(p);
        if(!owns(p,s)||s.loop.activity!=OtherMotherActivities.Activity.OTHER_MINESHAFT||state(s).getInt("Phase")!=1
            ||!validTool(p,s,ModItems.ACTIVITY_MINING_SHOVEL.get())||!target.getPersistentData().getBoolean("MineEnemy")
            ||!ActivityScenery.owned(target,s)||target.getPersistentData().getInt("Generation")!=s.loop.generation||!(target instanceof net.minecraft.world.entity.LivingEntity mob)
            ||!mob.isAlive()||p.distanceToSqr(mob)>12.25||!p.hasLineOfSight(mob))return;
        long now=OtherMotherLoop.now(p);var n=state(s);if(now<n.getLong("MineShovelReady"))return;n.putLong("MineShovelReady",now+10);
        mob.setHealth(Math.max(0,mob.getHealth()-3));
        p.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR,mob.getX(),mob.getY()+1,mob.getZ(),3,.2,.2,.2,.03);
        p.playNotifySound(SoundEvents.PLAYER_ATTACK_STRONG,SoundSource.PLAYERS,.6F,1);
        if(mob.getHealth()<=0)mob.discard();
        else {mob.knockback(.35,p.getX()-mob.getX(),p.getZ()-mob.getZ());p.serverLevel().broadcastEntityEvent(mob,(byte)2);}
    }
    private MineshaftActivity(){}
}
