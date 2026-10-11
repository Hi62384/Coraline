package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import com.coraline.clothing.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class VisitGiftGameTests {
    private static int index;
    private record Fixture(ServerPlayer p,VisitData.Scene s){}
    private static Fixture fixture(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);var pos=new BlockPos(92000+(index++)*192,100,92000);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)l.setChunkForced((pos.getX()>>4)+dx,(pos.getZ()>>4)+dz,true);
        l.getChunkAt(pos);for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++)l.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        var p=FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VisitGiftQA"));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var d=new DoorData.Door();d.owner=p.getUUID();d.pos=pos;DoorData.get(p.server).doors.put(d.id,d);
        var realm=RealmData.get(p.server).realm(d);realm.ready=true;realm.parentGeneration=1;realm.table=pos;realm.house=pos.north(40);realm.porch=pos.north(18);realm.radius=80;
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=d.id;s.stage=VisitData.Stage.DONE;s.table=pos;s.piano=pos;s.loop.phase=OtherMotherLoopState.Phase.WELCOME;s.loop.generation=1;s.loop.visits=2;
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var mom=ModEntities.MOM.get().create(l);mom.makeOtherParent(0);mom.setNoAi(true);mom.setPersistenceRequired();mom.setPos(p.position().add(2,0,0));mom.getPersistentData().putUUID("RealmDoor",s.door);mom.getPersistentData().putInt("RealmGeneration",1);l.addFreshEntity(mom);s.mom=mom.getUUID();
        return new Fixture(p,s);
    }
    private static void tick(ServerPlayer p){p.tickCount++;OtherMotherLoop.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));}
    private static void finish(GameTestHelper h,Fixture f,OtherMotherLoopState.Phase phase){
        h.assertTrue(f.s.loop.phase==phase,"Wrong voice phase "+phase);
        tick(f.p);h.assertTrue(f.s.loop.phase==phase&&f.s.loop.deadline>OtherMotherLoop.now(f.p),"Voice did not hold "+phase);
        tick(f.p);h.assertTrue(f.s.loop.phase==phase,"Voice overlapped next phase "+phase);
        f.s.loop.deadline=OtherMotherLoop.now(f.p);f.s.welcomeAfter=0;tick(f.p);
    }
    @GameTest(template="empty") public static void weightedCategoriesAndProgression(GameTestHelper h){
        var progress=new VisitGiftPools.Progress(0,false,false,false);var counts=new EnumMap<OtherMotherDialogue.Loot,Integer>(OtherMotherDialogue.Loot.class);var r=RandomSource.create(9371);
        for(int i=0;i<20000;i++)counts.merge(VisitGiftPools.chooseConfigured(r,progress,c->c.kind()==OtherMotherDialogue.Loot.MYSTERY?5:15,false),1,Integer::sum);
        h.assertTrue(counts.size()==10,"Selector omitted categories");h.assertTrue(counts.get(OtherMotherDialogue.Loot.MYSTERY)<counts.get(OtherMotherDialogue.Loot.FOOD),"Mystery weight ignored");
        for(var kind:OtherMotherDialogue.Loot.values())for(int seed=0;seed<300;seed++){
            var items=VisitGiftPools.generate(kind,RandomSource.create(seed),progress);h.assertTrue(!items.isEmpty(),"Empty reward "+kind);
            for(var stack:items){var item=stack.getItem();h.assertTrue(!ExpeditionActivities.temporary(stack),"Temporary activity equipment in gift");
                if(kind.expanded())h.assertFalse(item instanceof SetArmorItem armor&&(armor.set==ArmorSets.Set.MOONSTONE||armor.set==ArmorSets.Set.PERFECT||armor.set==ArmorSets.Set.OTHER_DIAMOND),"Early advanced armor");
                if(kind==OtherMotherDialogue.Loot.CLOTHING)h.assertTrue(item instanceof ClothingItem,"Clothing is not wearable clothing");
                if(kind==OtherMotherDialogue.Loot.ARMOR)h.assertTrue(item instanceof ArmorItem&&!(item instanceof ClothingItem),"Armor/clothing mismatch");
                if(kind==OtherMotherDialogue.Loot.WEAPONS)h.assertTrue(item instanceof SwordItem||item instanceof BowItem||item instanceof CrossbowItem||item==Items.ARROW,"Early weapons mismatch");
                if(kind==OtherMotherDialogue.Loot.TOOLS)h.assertTrue(item instanceof DiggerItem||item instanceof FishingRodItem||item instanceof ShearsItem||item instanceof BrushItem,"Tools mismatch");
                if(kind==OtherMotherDialogue.Loot.MUSIC)h.assertTrue(item instanceof RecordItem||item==Items.JUKEBOX||item==Items.NOTE_BLOCK||item==Items.BELL,"Music mismatch");
            }
            if(kind==OtherMotherDialogue.Loot.BUILDING_MATERIALS||kind==OtherMotherDialogue.Loot.REDSTONE)h.assertTrue(items.size()>=5&&items.stream().mapToInt(ItemStack::getCount).sum()>48,"Bundle too small");
        }
        for(var kind:OtherMotherDialogue.Loot.values())h.assertTrue(VisitGiftPools.choose(r,progress,c->c.kind()==kind?1:0)==kind,"Forced weight ignored");
        h.assertTrue(!VisitGiftPools.choose(r,progress,c->0).expanded(),"Zero weight fallback failed");h.succeed();
    }
    @GameTest(template="empty") public static void equalCategoryOddsIgnoreLegacyWeightsAndPoolSizes(GameTestHelper h){
        var progress=new VisitGiftPools.Progress(0,false,false,false);var r=RandomSource.create(91743);var uniform=RandomSource.create(91743);
        int[] legacy={15,15,10,10,10,10,10,8,7,5};var counts=new EnumMap<OtherMotherDialogue.Loot,Integer>(OtherMotherDialogue.Loot.class);
        for(int i=0;i<100000;i++){
            var selected=VisitGiftPools.chooseConfigured(r,progress,c->legacy[c.kind().ordinal()],true);
            h.assertTrue(selected==VisitGiftPools.choose(uniform,progress,c->1),"Saved weights biased equal mode");counts.merge(selected,1,Integer::sum);
        }
        h.assertTrue(counts.size()==10,"Category missing from equal selection");
        for(var kind:OtherMotherDialogue.Loot.values()){
            h.assertTrue(kind.defaultWeight==10,"Unequal new-install default");
            h.assertTrue(Math.abs(counts.get(kind)-10000)<500,"Category distribution outside tolerance: "+kind);
            h.assertTrue(VisitGiftPools.chooseConfigured(r,progress,c->c.kind()==kind?100:0,true)==kind,"Disabled categories not respected");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void advancedGearRequiresPriorMaterial(GameTestHelper h){
        var late=new VisitGiftPools.Progress(2,false,false,false);
        for(var kind:List.of(OtherMotherDialogue.Loot.ARMOR,OtherMotherDialogue.Loot.TOOLS,OtherMotherDialogue.Loot.WEAPONS,OtherMotherDialogue.Loot.MYSTERY))for(int seed=0;seed<1000;seed++)for(var stack:VisitGiftPools.generate(kind,RandomSource.create(seed),late)){
            var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
            h.assertFalse(id.startsWith("moonstone_")||id.startsWith("other_diamond_")||id.startsWith("perfect_"),"Late visit bypassed prior-material gate "+id);
        }
        var f=fixture(h);f.s.loop.visits=20;f.p.getInventory().setItem(0,new ItemStack(com.coraline.food.TreatFoods.ITEMS.get("perfect_roast_dinner").get()));
        h.assertFalse(VisitGiftPools.progress(f.p,f.s).perfect(),"Food unlocked perfect gear");
        f.p.getInventory().setItem(0,new ItemStack(com.coraline.registry.ModItems.PERFECT_INGOT.get()));h.assertTrue(VisitGiftPools.progress(f.p,f.s).perfect(),"Perfect material did not unlock gear");h.succeed();
    }
    @GameTest(template="empty") public static void originalFoodAndTreasurePoolsPreserved(GameTestHelper h){
        for(int seed=0;seed<100;seed++)for(var kind:List.of(OtherMotherDialogue.Loot.FOOD,OtherMotherDialogue.Loot.TREASURE)){
            var r=RandomSource.create(seed);var expected=new ArrayList<ItemStack>();
            if(kind==OtherMotherDialogue.Loot.FOOD){for(int theme=0;theme<4;theme++)for(int i=0;i<3;i++)expected.add(FeastContent.serving(r,theme));for(int i=0;i<3;i++)expected.add(FeastContent.random(r,true));}
            else for(var item:new Item[]{Items.IRON_INGOT,Items.GOLD_INGOT,Items.COPPER_INGOT,Items.COAL,Items.LAPIS_LAZULI,Items.REDSTONE,Items.EMERALD,Items.DIAMOND,OtherMaterials.OTHER_IRON.get(),OtherMaterials.OTHER_DIAMOND.get()})expected.add(new ItemStack(item,1+r.nextInt(item==Items.DIAMOND||item==OtherMaterials.OTHER_DIAMOND.get()?3:6)));
            var actual=VisitGiftPools.generate(kind,RandomSource.create(seed),new VisitGiftPools.Progress(0,false,false,false));
            h.assertTrue(actual.size()==expected.size(),"Original pool size changed");for(int i=0;i<actual.size();i++)h.assertTrue(ItemStack.matches(actual.get(i),expected.get(i)),"Original pool changed at "+kind+"/"+i);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=500) public static void everyCategoryAndVoiceChoiceContinuesVisit(GameTestHelper h){
        var fixtures=new ArrayList<Fixture>();for(var kind:OtherMotherDialogue.Loot.values())for(int line=0;line<3;line++){
            var f=fixture(h);f.s.loop.loot=kind;f.s.loop.giftBefore=OtherMotherDialogue.lootPool(kind,OtherMotherDialogue.Moment.BEFORE).sound(line);f.s.loop.giftAfter=OtherMotherDialogue.lootPool(kind,OtherMotherDialogue.Moment.AFTER).sound((line+1)%3);fixtures.add(f);
        }
        h.runAfterDelay(5,()->{
            for(var f:fixtures){
                var l=f.s.loop;String before=l.giftBefore,after=l.giftAfter;
                finish(h,f,OtherMotherLoopState.Phase.WELCOME);h.assertTrue(l.phase==OtherMotherLoopState.Phase.BEFORE&&l.gifts.isEmpty(),"Gift preceded welcome");
                finish(h,f,OtherMotherLoopState.Phase.BEFORE);h.assertTrue(l.phase==OtherMotherLoopState.Phase.GIFTS&&l.giftPrepared&&!l.gifts.isEmpty(),"Before did not prepare gift");
                h.assertTrue(before.equals(f.s.lastDialogue.get(OtherMotherDialogue.lootPool(l.loot,OtherMotherDialogue.Moment.BEFORE).path())),"Wrong before voice");
                int amount=l.gifts.stream().mapToInt(ItemStack::getCount).sum();
                f.p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(f.s.table));LoopProps.ensure(f.p,f.s);var props=LoopProps.nearby(f.p.serverLevel(),f.s,f.s.table);
                if(l.loot==OtherMotherDialogue.Loot.FOOD){for(var prop:props)if(prop.kind()==ScenePropEntity.PLATE||prop.kind()==ScenePropEntity.GLASS)for(int i=0;i<3;i++)LoopProps.interact(f.p,prop);}
                else {var box=props.stream().filter(prop->prop.kind()==ScenePropEntity.GIFT).findFirst().orElseThrow();
                    if(l.loot==OtherMotherDialogue.Loot.MYSTERY)h.assertTrue(box.treasure().isEmpty(),"Mystery contents revealed before opening");
                    LoopProps.interact(f.p,box);
                    if(l.loot.expanded()){h.assertTrue(f.p.getInventory().isEmpty(),"Reward preceded reveal");LoopProps.finishGiftReveal(f.p,f.s);h.assertTrue(!l.collected(),"Reveal ended too early");l.giftRevealUntil=OtherMotherLoop.now(f.p);LoopProps.finishGiftReveal(f.p,f.s);}
                    else for(int i=0;i<l.gifts.size();i++)LoopProps.interact(f.p,box);
                }
                h.assertTrue(l.collected(),"Gift not collected: "+l.loot+" remaining="+l.gifts.stream().filter(stack->!stack.isEmpty()).count()+" props="+props.size());h.assertTrue(f.p.getInventory().items.stream().mapToInt(ItemStack::getCount).sum()==amount,"Reward amount differs: "+l.loot);
                tick(f.p);finish(h,f,OtherMotherLoopState.Phase.AFTER);h.assertTrue(after.equals(f.s.lastDialogue.get(OtherMotherDialogue.lootPool(l.loot,OtherMotherDialogue.Moment.AFTER).path())),"Wrong after voice");
                finish(h,f,OtherMotherLoopState.Phase.TUTORIAL);h.assertTrue(l.phase==OtherMotherLoopState.Phase.PROPOSAL,"Tutorial/proposal interrupted");
                finish(h,f,OtherMotherLoopState.Phase.PROPOSAL);finish(h,f,OtherMotherLoopState.Phase.WARNING);
                h.assertTrue(l.phase==OtherMotherLoopState.Phase.WINDOW&&l.arrivalDeadline-OtherMotherLoop.now(f.p)==6000,"Missing five-minute relaxation");
                h.assertTrue(l.giftBefore.equals(before)&&l.giftAfter.equals(after),"Selected lines rerolled");
                OtherMotherLoop.forget(f.p.getUUID());
            }h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100) public static void fullInventoryOwnerAndSavedReveal(GameTestHelper h){
        var f=fixture(h);var p=f.p;var s=f.s;s.loop.loot=OtherMotherDialogue.Loot.MYSTERY;s.loop.phase=OtherMotherLoopState.Phase.GIFTS;
        s.loop.giftPrepared=true;s.loop.gifts.add(new ItemStack(Items.DIAMOND,2));s.loop.giftBefore="other_mother_loot_mystery_before_02";s.loop.giftAfter="other_mother_loot_mystery_after_03";
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));LoopProps.ensure(p,s);
        h.runAfterDelay(3,()->{
            var box=LoopProps.nearby(p.serverLevel(),s,s.table).stream().filter(e->e.kind()==ScenePropEntity.GIFT).findFirst().orElseThrow();
            var helper=FakePlayerFactory.get(p.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GiftHelper"));helper.setPos(p.position());LoopProps.interact(helper,box);
            h.assertTrue(!s.loop.opened,"Helper opened present");LoopProps.interact(p,box);
            s.loop.giftRevealUntil=OtherMotherLoop.now(p);var copy=OtherMotherLoopState.load(s.loop.save());s.loop=copy;
            LoopProps.finishGiftReveal(p,s);LoopProps.finishGiftReveal(p,s);LoopProps.interact(p,box);
            var drops=p.serverLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4),e->e.getItem().is(Items.DIAMOND));
            h.assertTrue(drops.size()==1&&drops.get(0).getItem().getCount()==2&&p.getUUID().equals(drops.get(0).getPersistentData().getUUID("CoralineAwardOwner")),"Overflow lost or duplicated");
            var pickup=new net.minecraftforge.event.entity.player.EntityItemPickupEvent(helper,drops.get(0));VisitGiftOwnership.pickup(pickup);h.assertTrue(pickup.isCanceled(),"Helper stole overflow");
            h.assertTrue(copy.collected()&&copy.giftPrepared&&copy.loot==OtherMotherDialogue.Loot.MYSTERY&&copy.giftAfter.endsWith("_03"),"Saved visit changed");
            var old=copy.gifts;h.assertTrue(LoopProps.prepareGifts(p,s)&&old==copy.gifts&&copy.collected(),"Prepared gift rerolled");h.succeed();
        });
    }
}
