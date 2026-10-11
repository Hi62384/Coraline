package com.coraline.feast;

import static com.coraline.feast.ExpeditionActivities.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import com.coraline.registry.ModItems;
import java.util.*;

public final class MansionActivity {
    public static void markRoom(ServerPlayer p,UUID session,int room,boolean excluded){
        var s=OtherMotherLoop.active(p);
        if(!owns(p,s)||s.loop.activity!=OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION||room<0||room>=12)return;
        var n=state(s);if(!n.hasUUID("JournalSession")||!session.equals(n.getUUID("JournalSession")))return;
        int mask=n.getInt("ExcludedRooms");n.putInt("ExcludedRooms",excluded?mask|1<<room:mask&~(1<<room));OtherMotherLoop.changed(p);
    }
    private static final Map<UUID,MansionPuzzle.Puzzle> PUZZLES=new HashMap<>();
    static MansionPuzzle.Puzzle puzzle(VisitData.Scene s){return PUZZLES.computeIfAbsent(s.owner,id->MansionPuzzle.generate(state(s).getLong("Seed")));}
    static void forget(UUID id){PUZZLES.remove(id);}
    static int side(MansionPuzzle.Room r){return r.wing()==0?-1:1;}
    static BlockPos door(BlockPos c,MansionPuzzle.Room r){return c.offset(side(r)*18,r.floor()*9,-18-r.row()*22);}
    static BlockPos witness(BlockPos c,int i){return c.offset(i%2==0?-3:3,(i/3)*9,-12-(i%3)*23);}
    static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){var game=puzzle(s);
        box(l,c,-36,-2,-83,36,-1,13,Blocks.STONE_BRICKS);box(l,c,-35,0,-82,35,19,12,Blocks.DARK_OAK_PLANKS);box(l,c,-34,0,-81,34,17,11,Blocks.AIR);
        for(int y:new int[]{-1,8,18})box(l,c,-34,y,-81,34,y,11,Blocks.DARK_OAK_PLANKS);
        // Long entrance gallery, two room wings and a broad, accessible staircase at the far end.
        for(int floor=0;floor<2;floor++){int y=floor*9;box(l,c,-2,y,-72,2,y,6,Blocks.RED_CARPET);
            for(int z=-72;z<=6;z+=13)for(int x:new int[]{-6,6}){box(l,c,x,y,z,x,y+6,z,Blocks.DARK_OAK_LOG);put(l,c,x,y+5,z+1,Blocks.SHROOMLIGHT);}
            for(int z=-65;z<=0;z+=22){box(l,c,-2,y+6,z,2,y+6,z,Blocks.GOLD_BLOCK);for(int x:new int[]{-2,2})ActivityBlocks.set(l,c.offset(x,y+5,z),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),2);}
        }
        for(int i=0;i<9;i++){box(l,c,-3,i-1,-74-i,3,i-1,-74-i,Blocks.DARK_OAK_PLANKS);for(int x=-3;x<=3;x++)ActivityBlocks.set(l,c.offset(x,i,-74-i),Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH),2);box(l,c,-3,i+1,-74-i,3,i+3,-74-i,Blocks.AIR);}
        // Upper landing returns down a side gallery, rather than requiring a jump off the staircase.
        box(l,c,4,8,-82,6,8,-70,Blocks.DARK_OAK_PLANKS);box(l,c,-6,8,-82,-4,8,-70,Blocks.DARK_OAK_PLANKS);
        box(l,c,-6,8,-83,6,8,-82,Blocks.DARK_OAK_PLANKS);box(l,c,-6,9,-83,6,12,-82,Blocks.AIR);box(l,c,4,9,-82,6,12,-70,Blocks.AIR);box(l,c,-6,9,-82,-4,12,-70,Blocks.AIR);
        // Stair clearance crosses the old rear facade at z=-82. Enclose that
        // landing as a real vestibule rather than leaving a doorway into the void.
        box(l,c,-7,8,-84,7,13,-84,Blocks.DARK_OAK_PLANKS);
        box(l,c,-7,8,-83,-7,13,-82,Blocks.DARK_OAK_PLANKS);
        box(l,c,7,8,-83,7,13,-82,Blocks.DARK_OAK_PLANKS);
        box(l,c,-7,13,-84,7,13,-82,Blocks.DARK_OAK_PLANKS);
        box(l,c,-7,9,-84,7,15,-84,Blocks.DARK_OAK_PLANKS);box(l,c,-4,11,-84,4,13,-84,Blocks.PURPLE_STAINED_GLASS);
        for(var room:game.rooms()){int side=side(room),y=room.floor()*9,z=-18-room.row()*22;int x0=side<0?-34:8,x1=side<0?-8:34;
            box(l,c,x0,y,z-9,x1,y+7,z+9,Blocks.DARK_OAK_PLANKS);box(l,c,x0+1,y,z-8,x1-1,y+6,z+8,Blocks.AIR);box(l,c,side<0?-8:8,y,z-2,side<0?-8:8,y+3,z+2,Blocks.AIR);
            Block carpet=switch(room.carpet()){case 0->Blocks.RED_CARPET;case 1->Blocks.BLUE_CARPET;case 2->Blocks.GREEN_CARPET;default->Blocks.PURPLE_CARPET;};box(l,c,Math.min(side*9,side*17),y,z-6,Math.max(side*9,side*17),y,z+6,carpet);
            box(l,c,side*18,y,z-8,side*18,y+6,z+8,Blocks.DARK_OAK_PLANKS);
            var at=door(c,room);var face=side>0?Direction.WEST:Direction.EAST;Block trim=room.emblem()==0?Blocks.GOLD_BLOCK:room.emblem()==1?Blocks.EMERALD_BLOCK:Blocks.AMETHYST_BLOCK;
            for(int dz:new int[]{-1,1})box(l,c,side*18,y,z+dz,side*18,y+3,z+dz,trim);put(l,c,side*18,y+3,z,trim);
            ActivityBlocks.set(l,at,Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING,face),2);ActivityBlocks.set(l,at.above(),Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING,face).setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
            sign(l,at.relative(face).offset(0,2,3),face,room.name(),room.wing()==0?"West wing":"East wing","One key only","Sneak + use key");
            var b=c.offset(side*13,y,z-4);identity(l,b,room.identity());put(l,c,side*15,y+4,z+4,Blocks.SHROOMLIGHT);
            // Each window is an actual framed window, not a hidden solver-only attribute.
            box(l,c,side*35,y+2,z-3,side*35,y+4,z+3,Blocks.WHITE_STAINED_GLASS);
        }
        box(l,c,-5,0,11,5,5,11,Blocks.CHISELED_STONE_BRICKS);box(l,c,-2,0,11,2,3,11,Blocks.AIR);box(l,c,-7,0,4,-7,3,8,Blocks.BOOKSHELF);box(l,c,7,0,4,7,3,8,Blocks.BOOKSHELF);
        sign(l,c.offset(3,2,10),Direction.NORTH,"MANSION RULES","Allays: truth","Vindicators: lies","Evokers: exactly 1");
        ensureWitnesses(p,s,c);
        for(int i=0;i<6;i++){var at=witness(c,i);
            put(l,at,0,0,2,Blocks.LECTERN);put(l,at,0,3,2,Blocks.CHAIN);
            ActivityBlocks.set(l,at.offset(0,2,2),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),2);
        }
    }
    static void ensureWitnesses(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var l=p.serverLevel();var existing=l.getEntitiesOfClass(Mob.class,bounds(c,s.loop.activity),
                e->ActivityScenery.owned(e,s)&&e.getPersistentData().getInt("MansionClue")>0);
        for(int i=0;i<6;i++){
            final int index=i+1;var at=witness(c,i);LoopProps.hold(l,at);
            var matches=existing.stream().filter(e->e.getPersistentData().getInt("MansionClue")==index).toList();
            Mob mob=matches.isEmpty()?null:matches.get(0);
            for(int duplicate=1;duplicate<matches.size();duplicate++)matches.get(duplicate).discard();
            var clue=puzzle(s).clues().get(i);
            if(mob==null){mob=(Mob)(clue.speaker()==0?EntityType.ALLAY:clue.speaker()==1?EntityType.VINDICATOR:EntityType.EVOKER).create(l);
                if(mob==null)continue;ActivityScenery.mark(mob,s);mob.setNoAi(true);mob.setPersistenceRequired();mob.setNoGravity(true);
                mob.getPersistentData().putInt("MansionClue",index);mob.getPersistentData().putInt("Generation",s.loop.generation);
                mob.setCustomName(Component.literal("Button-Eyed "+(clue.speaker()==0?"Truthful Allay":clue.speaker()==1?"Lying Vindicator":"Riddling Evoker")));
                mob.setCustomNameVisible(true);mob.setPos(Vec3.atBottomCenterOf(at).add(0,clue.speaker()==0?.7:0,0));l.addFreshEntity(mob);
            }
            mob.setNoGravity(true);mob.setDeltaMovement(Vec3.ZERO);mob.setInvisible(false);
            mob.setPos(Vec3.atBottomCenterOf(at).add(0,clue.speaker()==0?.7:0,0));
            mob.setYRot(i%2==0?-90:90);mob.setYBodyRot(mob.getYRot());mob.setYHeadRot(mob.getYRot());
        }
    }
    static void identity(ServerLevel l,BlockPos b,int id){
        switch(id){
            case 0,10->{box(l,b,-2,0,-1,2,2,-1,Blocks.BOOKSHELF);put(l,b,0,0,1,Blocks.LECTERN);}
            case 1->{box(l,b,0,0,0,0,2,0,Blocks.LIGHT_BLUE_WOOL);put(l,b,-1,1,0,Blocks.WHITE_WOOL);put(l,b,1,1,0,Blocks.WHITE_WOOL);put(l,b,0,3,0,Blocks.SEA_LANTERN);}
            case 2->{box(l,b,-2,0,-1,2,0,1,Blocks.CARTOGRAPHY_TABLE);box(l,b,-1,1,-1,1,1,1,Blocks.GREEN_CARPET);}
            case 3->{box(l,b,0,0,0,0,3,0,Blocks.STRIPPED_DARK_OAK_LOG);box(l,b,-1,3,0,1,4,0,Blocks.IRON_BLOCK);}
            case 4,5->{Block wool=id==4?Blocks.BLACK_WOOL:Blocks.WHITE_WOOL;box(l,b,-1,0,0,1,2,1,wool);box(l,b,0,3,0,1,3,1,wool);put(l,b,1,3,-1,id==4?Blocks.PINK_WOOL:Blocks.YELLOW_WOOL);put(l,b,-1,3,0,wool);}
            case 6->{box(l,b,-2,0,0,2,0,0,Blocks.DARK_OAK_FENCE);box(l,b,-2,1,-1,2,1,1,Blocks.DARK_OAK_SLAB);put(l,b,0,2,0,Blocks.CAKE);}
            case 7->{box(l,b,-1,0,-1,1,0,1,Blocks.WHITE_WOOL);box(l,b,-1,1,-1,1,1,-1,Blocks.RED_WOOL);put(l,b,2,0,0,Blocks.BARREL);}
            case 8->{box(l,b,-2,0,-1,2,1,0,Blocks.BARREL);put(l,b,0,2,0,Blocks.HAY_BLOCK);}
            case 9->{box(l,b,-2,0,-1,2,2,-1,Blocks.IRON_BARS);put(l,b,0,0,0,Blocks.CAULDRON);}
            default->{for(int x=-2;x<=2;x+=2){put(l,b,x,0,0,Blocks.QUARTZ_PILLAR);put(l,b,x,1,0,x==0?Blocks.DIAMOND_BLOCK:Blocks.GOLD_BLOCK);}}
        }
    }
    static void treasure(ServerLevel l,BlockPos b){
        for(int x=-5;x<=5;x++)for(int z=-7;z<=7;z++)if(Math.abs(x)==5||Math.abs(z)==7)put(l,b,x,-1,z,Blocks.GOLD_BLOCK);
        for(int x:new int[]{-3,3})for(int z:new int[]{-5,5}){box(l,b,x-1,0,z-1,x+1,0,z+1,Blocks.GOLD_BLOCK);put(l,b,x,1,z,Blocks.GOLD_BLOCK);put(l,b,x,2,z,x<0?Blocks.EMERALD_BLOCK:Blocks.DIAMOND_BLOCK);put(l,b,x,3,z,Blocks.IRON_BARS);put(l,b,x,4,z,Blocks.SEA_LANTERN);}
        box(l,b,-2,0,-6,2,0,-6,Blocks.CHISELED_QUARTZ_BLOCK);put(l,b,-1,1,-6,Blocks.CHEST);put(l,b,1,1,-6,Blocks.CHEST);put(l,b,0,1,-6,Blocks.ENCHANTING_TABLE);
        box(l,b,-1,0,5,1,0,6,Blocks.GOLD_BLOCK);put(l,b,0,1,6,Blocks.AMETHYST_BLOCK);
    }
    static void begin(ServerPlayer p,VisitData.Scene s){borrow(p,s,ModItems.MANSION_KEY.get());ActivityScenery.title(p,"WOODLAND MANSION","One key. Follow the clues.");}
    public static boolean npc(ServerPlayer p,Entity e){int index=e.getPersistentData().getInt("MansionClue")-1;if(index<0)return false;var s=OtherMotherLoop.active(p);if(owns(p,s)&&s.loop.activity==OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION&&state(s).getInt("Phase")==1&&ActivityScenery.owned(e,s)&&p.distanceToSqr(e)<36)discover(p,s,index);return true;}
    static void discover(ServerPlayer p,VisitData.Scene s,int i){if(i<0||i>=6)return;state(s).putInt("Clues",state(s).getInt("Clues")|1<<i);OtherMotherLoop.changed(p);p.playNotifySound(SoundEvents.BOOK_PAGE_TURN,SoundSource.PLAYERS,.6F,1);journal(p,s,i);}
    public static void interactBlock(ServerPlayer p,BlockPos at){var s=OtherMotherLoop.active(p);if(!owns(p,s)||s.loop.activity!=OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION||p.getEyePosition().distanceToSqr(Vec3.atCenterOf(at))>36)return;var c=center(p,s);if(state(s).getInt("Phase")!=1)return;
        for(int i=0;i<6;i++)if(at.equals(witness(c,i).south(2))){discover(p,s,i);return;}
        for(var room:puzzle(s).rooms())if(at.equals(door(c,room))||at.equals(door(c,room).above())){if(!validTool(p,s,ModItems.MANSION_KEY.get()))return;if(!p.isShiftKeyDown()){p.displayClientMessage(Component.literal("Sneak + right-click to spend your only key."),true);return;}choose(p,s,room.id());return;}
    }
    static boolean choose(ServerPlayer p,VisitData.Scene s,int id){if(!owns(p,s)||s.loop.activity!=OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION||state(s).getInt("Phase")!=1||!validTool(p,s,ModItems.MANSION_KEY.get())||id<0||id>=12)return false;var game=puzzle(s);var n=state(s);boolean win=game.jackpot()==id;n.putInt("Phase",win?3:5);n.putInt("Chosen",id);OtherMotherLoop.changed(p);restore(p,s);var c=center(p,s);var room=game.rooms().get(id);var at=door(c,room);ActivityBlocks.set(p.serverLevel(),at,Blocks.AIR.defaultBlockState(),2);ActivityBlocks.set(p.serverLevel(),at.above(),Blocks.AIR.defaultBlockState(),2);
        if(win){var l=p.serverLevel();int x=side(room)*27,y=room.floor()*9,z=-18-room.row()*22;treasure(l,c.offset(x,y,z));ensurePrize(p,s,c);ActivityScenery.title(p,"JACKPOT","Your deduction was correct.");p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,.7F,1);}
        else{put(p.serverLevel(),c,side(room)*27,room.floor()*9,-18-room.row()*22,Blocks.FLOWER_POT);var answer=game.rooms().get(game.jackpot());n.putString("Answer",answer.name());ActivityScenery.title(p,"EMPTY ROOM","Jackpot: "+answer.name());p.sendSystemMessage(Component.literal("The jackpot was in the "+answer.name()+" — "+(answer.floor()==0?"ground":"upper")+" floor, "+(answer.wing()==0?"west":"east")+" wing; "+MansionPuzzle.COLORS[answer.carpet()]+" carpet, "+(answer.emblem()==0?"gold":answer.emblem()==1?"emerald":"amethyst")+" trim. The exit is open.").withStyle(net.minecraft.ChatFormatting.GOLD));p.playNotifySound(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(),SoundSource.PLAYERS,.5F,.6F);ActivityWorld.ensureExit(p,s);}return true;
    }
    public static void openJournal(ServerPlayer p){var s=OtherMotherLoop.active(p);if(owns(p,s)&&s.loop.activity==OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION)journal(p,s,-1);}
    static void journal(ServerPlayer p,VisitData.Scene s,int focus){var game=puzzle(s);var rows=new ArrayList<String>();for(var r:game.rooms())rows.add(r.name()+((state(s).getInt("Survey")&(1<<r.id()))!=0?" | "+MansionPuzzle.COLORS[r.carpet()]+" | "+(r.emblem()==0?"gold":r.emblem()==1?"emerald":"amethyst"):" | not inspected"));var clues=new ArrayList<String>();for(int i=0;i<6;i++)if((state(s).getInt("Clues")&(1<<i))!=0){var cl=game.clues().get(i);clues.add((cl.speaker()==0?"ALLAY (truth): ":cl.speaker()==1?"VINDICATOR (lie): ":"EVOKER (exactly one): ")+cl.text());}if(!state(s).hasUUID("JournalSession")){state(s).putUUID("JournalSession",UUID.randomUUID());OtherMotherLoop.changed(p);}ExpeditionNetwork.journal(p,state(s).getUUID("JournalSession"),state(s).getInt("ExcludedRooms"),rows,clues);}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){var n=state(s);if(!bounds(c,s.loop.activity).contains(p.position())||p.getY()<c.getY()-1)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));int phase=n.getInt("Phase");if(p.tickCount%80==0)ensureWitnesses(p,s,c);if(phase==1){if(p.tickCount%20==0){int seen=n.getInt("Survey");for(var room:puzzle(s).rooms())if(Math.abs(p.getX()-c.getX()-side(room)*13)<5&&Math.abs(p.getY()-c.getY()-room.floor()*9)<2&&Math.abs(p.getZ()-c.getZ()+18+room.row()*22)<9)seen|=1<<room.id();if(seen!=n.getInt("Survey")){n.putInt("Survey",seen);OtherMotherLoop.changed(p);}}if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Key: 1 • Clues: "+Integer.bitCount(n.getInt("Clues"))+" / 6 • Use key in air: journal"),true);
            // Treasure interiors do not exist until the key transaction. Also reject mobility bypasses.
            if(Math.abs(p.getX()-c.getX())>18)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.offset(0,p.getY()>c.getY()+7?9:0,-12)));
        }else if(phase==3)ensurePrize(p,s,c);
    }
    static void ensurePrize(ServerPlayer p,VisitData.Scene s,BlockPos c){var r=puzzle(s).rooms().get(state(s).getInt("Chosen"));var at=c.offset(side(r)*26,r.floor()*9+1,-18-r.row()*22-4);if(LoopProps.nearby(p.serverLevel(),s,at).stream().noneMatch(e->e.slot==750)){put(p.serverLevel(),at,0,-1,0,Blocks.CHISELED_QUARTZ_BLOCK);var prop=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.PRIZE_PLATE,Vec3.atBottomCenterOf(at),750);prop.food(0,new ItemStack(Items.DIAMOND));prop.setCustomName(Component.literal("Claim mansion jackpot"));prop.setCustomNameVisible(true);}}
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){if(prop.slot!=750)return false;if(!owns(p,s)||s.loop.activity!=OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION||state(s).getInt("Phase")!=3)return true;state(s).putInt("Phase",4);OtherMotherLoop.changed(p);prop.discard();DesertTempleActivity.jackpot(p,s);ActivityWorld.ensureExit(p,s);return true;}
    private MansionActivity(){}
}
