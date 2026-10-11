package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.core.Direction;
import java.util.*;

/** Lifecycle adapters for the three expeditions. Rewards still live in the family scene ledger. */
public final class ExpeditionActivities {
    static CompoundTag state(VisitData.Scene s){return s.loop.expedition;}
    public static boolean supports(OtherMotherActivities.Activity a){return a==OtherMotherActivities.Activity.TREASURE_HUNT||a==OtherMotherActivities.Activity.SHOPPING_MALL||a==OtherMotherActivities.Activity.SNOWBALL_BATTLE||a==OtherMotherActivities.Activity.OTHER_MINESHAFT||a==OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS||a==OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION;}
    static boolean owns(ServerPlayer p,VisitData.Scene s){return s!=null&&supports(s.loop.activity)&&s.owner.equals(p.getUUID())&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY&&ActivityWorld.isActivity(p.level());}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return location(VillageActivity.center(p,s),s.loop.activity);}
    static BlockPos location(BlockPos base,OtherMotherActivities.Activity a){return switch(a){case TREASURE_HUNT->base.offset(650,0,1100);case SHOPPING_MALL->base.offset(1100,0,600);case SNOWBALL_BATTLE->base.offset(1500,0,600);case OTHER_MINESHAFT->base.offset(1000,0,1000);case OTHER_NETHER_FORTRESS->base.offset(1300,220,1000);default->base.offset(300,0,1100);};}
    static AABB bounds(BlockPos c,OtherMotherActivities.Activity a){return switch(a){case TREASURE_HUNT->new AABB(c.offset(-74,-10,-149),c.offset(75,22,14));case SHOPPING_MALL->new AABB(c.offset(-46,-3,-137),c.offset(47,21,14));case SNOWBALL_BATTLE->new AABB(c.offset(-40,-3,-78),c.offset(41,14,14));case OTHER_MINESHAFT->new AABB(c.offset(-98,-49,-190),c.offset(99,12,13));case OTHER_NETHER_FORTRESS->new AABB(c.offset(-20,-282,-20),c.offset(41,10,21));default->new AABB(c.offset(-37,-3,-86),c.offset(38,24,16));};}
    static boolean exitReady(VisitData.Scene s){return s.loop.activity==OtherMotherActivities.Activity.SHOPPING_MALL||s.loop.activity==OtherMotherActivities.Activity.TREASURE_HUNT||s.loop.activity==OtherMotherActivities.Activity.OTHER_MINESHAFT?state(s).getInt("Phase")>=1:state(s).getInt("Phase")>=4;}
    static BlockPos exitPos(BlockPos c,VisitData.Scene s){return s.loop.activity==OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS&&state(s).getBoolean("Landed")?c.offset(28,-278,8):c.south(7);}
    static void clearOtherArenas(ServerPlayer p,VisitData.Scene s,ServerLevel l){for(var a:OtherMotherActivities.Activity.values())if(supports(a)){var c=location(VillageActivity.center(p,s),a);ActivityScenery.clean(l,s,bounds(c,a));ActivityScenery.erase(l,bounds(c,a),c.below());}MineshaftActivity.forget(s.owner);MansionActivity.forget(s.owner);}
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        s.loop.expedition=new CompoundTag();var n=state(s);n.putLong("Seed",p.getRandom().nextLong());n.putLong("VoiceUntil",OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(s.loop.activity)));n.putString("Kind",s.loop.activity.name());
        var c=center(p,s);LoopProps.hold(l,c);
        switch(s.loop.activity){case TREASURE_HUNT->TreasureHuntActivity.build(p,s,l,c);case SHOPPING_MALL->ShoppingMallActivity.build(p,s,l,c);case SNOWBALL_BATTLE->SnowballBattleActivity.build(p,s,l,c);case OTHER_MINESHAFT->MineshaftActivity.build(p,s,l,c);case OTHER_NETHER_FORTRESS->FortressActivity.build(p,s,l,c);case OTHER_WOODLAND_MANSION->MansionActivity.build(p,s,l,c);default->{}}
        ActivityScenery.father(p,s,l,c.east(2));OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){LoopProps.hold(l,c);}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(!owns(p,s))return;var n=state(s);long now=OtherMotherLoop.now(p);p.fallDistance=0;p.clearFire();
        if(p.tickCount%20==0){ActivityScenery.clearWeather(p);ExpeditionNetwork.mode(p,s.loop.activity==OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS&&n.getInt("Phase")==2?1:0);}
        if(n.getInt("Phase")==0){
            if(p.distanceToSqr(Vec3.atBottomCenterOf(c))>25)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));
            if(now<n.getLong("VoiceUntil"))return;
            n.putInt("Phase",1);
            switch(s.loop.activity){case TREASURE_HUNT->TreasureHuntActivity.begin(p,s);case SHOPPING_MALL->ShoppingMallActivity.begin(p,s);case SNOWBALL_BATTLE->SnowballBattleActivity.begin(p,s);case OTHER_MINESHAFT->MineshaftActivity.begin(p,s);case OTHER_NETHER_FORTRESS->FortressActivity.begin(p,s);case OTHER_WOODLAND_MANSION->MansionActivity.begin(p,s);default->{}}OtherMotherLoop.changed(p);
        }
        switch(s.loop.activity){case TREASURE_HUNT->TreasureHuntActivity.tick(p,s,c);case SHOPPING_MALL->ShoppingMallActivity.tick(p,s,c);case SNOWBALL_BATTLE->SnowballBattleActivity.tick(p,s,c);case OTHER_MINESHAFT->MineshaftActivity.tick(p,s,c);case OTHER_NETHER_FORTRESS->FortressActivity.tick(p,s,c);case OTHER_WOODLAND_MANSION->MansionActivity.tick(p,s,c);default->{}}
    }
    static void clean(ServerPlayer p,VisitData.Scene s){if(s.loop.activity==OtherMotherActivities.Activity.OTHER_MINESHAFT)MineshaftActivity.settle(p,s);if(s.loop.activity==OtherMotherActivities.Activity.TREASURE_HUNT)TreasureHuntActivity.clear(p,s);if(s.loop.activity==OtherMotherActivities.Activity.SHOPPING_MALL)ShoppingMallActivity.settle(p,s);if(s.loop.activity==OtherMotherActivities.Activity.SNOWBALL_BATTLE){SnowballBattleActivity.clear(p,s);SnowballBattleActivity.restoreBattle(p,s);}restore(p,s);MineshaftActivity.clear(p,s);MansionActivity.forget(s.owner);FortressActivity.restore(p,s);ExpeditionNetwork.mode(p,0);var c=center(p,s);ActivityScenery.clean(p.serverLevel(),s,bounds(c,s.loop.activity));ActivityScenery.erase(p.serverLevel(),bounds(c,s.loop.activity),c.below());}
    public static void abort(ServerPlayer p,VisitData.Scene s){if(!supports(s.loop.activity)||!s.owner.equals(p.getUUID()))return;
        if(s.loop.activity==OtherMotherActivities.Activity.TREASURE_HUNT)TreasureHuntActivity.clear(p,s);
        if(s.loop.activity==OtherMotherActivities.Activity.OTHER_MINESHAFT)MineshaftActivity.finish(p,s,false);
        if(s.loop.activity==OtherMotherActivities.Activity.SHOPPING_MALL)ShoppingMallActivity.settle(p,s);
        if(s.loop.activity==OtherMotherActivities.Activity.SNOWBALL_BATTLE){SnowballBattleActivity.clear(p,s);SnowballBattleActivity.restoreBattle(p,s);}
        restore(p,s);FortressActivity.restore(p,s);ExpeditionNetwork.mode(p,0);state(s).putInt("Phase",5);OtherMotherLoop.changed(p);
        if(ActivityWorld.isActivity(p.level())){ActivityScenery.teleport(p,Vec3.atBottomCenterOf(exitPos(center(p,s),s).north(3)));ActivityWorld.ensureExit(p,s);}
    }
    static void borrow(ServerPlayer p,VisitData.Scene s,Item item){var n=state(s);if(!n.getBoolean("Borrowed")){n.put("Stored",p.getMainHandItem().save(new CompoundTag()));n.putInt("Slot",p.getInventory().selected);n.putBoolean("Borrowed",true);}var stack=new ItemStack(item);stack.getOrCreateTag().putUUID("ExpeditionOwner",s.owner);stack.getOrCreateTag().putInt("Generation",s.loop.generation);p.getInventory().setItem(n.getInt("Slot"),stack);p.getInventory().selected=n.getInt("Slot");p.getInventory().setChanged();}
    static boolean validTool(ServerPlayer p,VisitData.Scene s,Item item){var stack=p.getMainHandItem();return stack.is(item)&&stack.hasTag()&&stack.getTag().hasUUID("ExpeditionOwner")&&stack.getTag().getUUID("ExpeditionOwner").equals(s.owner)&&stack.getTag().getInt("Generation")==s.loop.generation;}
    static void restore(ServerPlayer p,VisitData.Scene s){var n=state(s);for(int i=0;i<p.getInventory().getContainerSize();i++)if(temporary(p.getInventory().getItem(i)))p.getInventory().setItem(i,ItemStack.EMPTY);if(temporary(p.containerMenu.getCarried()))p.containerMenu.setCarried(ItemStack.EMPTY);if(n.getBoolean("Borrowed")){var original=ItemStack.of(n.getCompound("Stored"));n.putBoolean("Borrowed",false);n.remove("Stored");if(!original.isEmpty())ActivityTokens.give(p,original);}OtherMotherLoop.changed(p);}
    public static boolean temporary(ItemStack s){return s.getItem() instanceof com.coraline.item.ExpeditionItem||s.hasTag()&&s.getTag().getBoolean("MineLoan");}
    static void sign(ServerLevel l,BlockPos at,Direction face,String... lines){ActivityBlocks.set(l,at,Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,face),2);if(l.getBlockEntity(at) instanceof SignBlockEntity b){var t=b.getFrontText();for(int i=0;i<Math.min(4,lines.length);i++)t=t.setMessage(i,Component.literal(lines[i]));b.setText(ActivityBlocks.readable(b,t),true);b.setChanged();l.sendBlockUpdated(at,l.getBlockState(at),l.getBlockState(at),3);}}
    static void box(ServerLevel l,BlockPos c,int x,int y,int z,int X,int Y,int Z,Block b){ActivityScenery.box(l,c,x,y,z,X,Y,Z,b);}
    static void put(ServerLevel l,BlockPos c,int x,int y,int z,Block b){ActivityScenery.put(l,c,x,y,z,b);}
    private ExpeditionActivities(){}
}
