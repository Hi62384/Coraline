package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

public final class DesertTempleActivity {
    // 0 introduction, 1 transition, 2 preview, 3 traversal, 4 vault, 5 failed, 6 claimed.
    private static final Map<UUID,List<TemplePath.Tile>> PATHS=new HashMap<>();
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).offset(300,0,600);}
    static CompoundTag state(VisitData.Scene s){return s.loop.temple;}
    static boolean owns(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY&&s.loop.activity==OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE&&ActivityWorld.isActivity(p.level());}
    static boolean exitReady(VisitData.Scene s){return state(s).getInt("Phase")>=5;}
    static BlockPos exitPos(BlockPos c,VisitData.Scene s){return state(s).getInt("Phase")==6?c.north(161):c.south(6);}
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){clean(l,s,center(p,s));s.loop.temple=new CompoundTag();state(s).putLong("Seed",p.getRandom().nextLong());state(s).putLong("VoiceUntil",OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(s.loop.activity)));prepare(p,s,l,center(p,s));OtherMotherLoop.changed(p);}
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){LoopProps.hold(l,c);DesertTempleCourse.build(l,c);ActivityScenery.father(p,s,l,c.east(2));}
    static List<TemplePath.Tile> path(VisitData.Scene s){return PATHS.computeIfAbsent(s.owner,id->TemplePath.generate(state(s).getInt("Room"),state(s).getLong("Seed")+state(s).getInt("Room")*997));}
    static int previewTicks(int room){return new int[]{120,110,100,90,80}[room];}
    private static void beginPreview(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var n=state(s);int room=n.getInt("Room");PATHS.remove(s.owner);path(s);n.putInt("Phase",2);n.putInt("Next",0);n.putBoolean("OnBoard",false);n.putLong("Preview",OtherMotherLoop.now(p));
        DesertTempleCourse.gate(p.serverLevel(),c,room,true);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.offset(0,3,DesertTempleCourse.top(room)+5)));
        p.setYRot(180);p.setXRot(35);p.connection.teleport(p.getX(),p.getY(),p.getZ(),180,35);OtherMotherLoop.changed(p);
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(!owns(p,s))return;var n=state(s);int phase=n.getInt("Phase"),room=n.getInt("Room");long now=OtherMotherLoop.now(p);int top=DesertTempleCourse.top(room),size=TemplePath.size(room);
        if(phase>=5){if(p.getY()<c.getY()-2)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(exitPos(c,s).south(3)));return;}
        if(phase==0){if(now>=n.getLong("VoiceUntil")){n.putInt("Phase",1);ActivityScenery.title(p,"DESERT TEMPLE","Memorize the lights. Walk the path.");OtherMotherLoop.changed(p);}return;}
        if(!DesertTempleCourse.bounds(c).contains(p.position())||p.getY()<c.getY()-.2){fail(p,s,c);return;}
        if(phase==4){ensurePrize(p,s,c);return;}
        if(phase==1){
            // Approaching each viewing balcony starts that room; walking the connecting halls remains free.
            if(p.getZ()<=c.getZ()+top+10)beginPreview(p,s,c);
            else if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Room "+(room+1)+" / 5 • Enter the viewing balcony"),true);
            return;
        }
        if(phase==2){
            var at=Vec3.atBottomCenterOf(c.offset(0,3,top+5));if(p.position().distanceToSqr(at)>1)ActivityScenery.teleport(p,at);
            var route=path(s);long age=now-n.getLong("Preview");int reveal=Math.min(route.size(),1+(int)(age*route.size()/Math.max(1,previewTicks(room)/2)));
            if(p.tickCount%2==0)for(int i=0;i<reveal;i++){var tile=DesertTempleCourse.tile(c,room,route.get(i));ActivityBlocks.set(p.serverLevel(),tile.below(),Blocks.SEA_LANTERN.defaultBlockState(),2);ActivityBlocks.set(p.serverLevel(),tile,Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState(),2);}
            if(age%8==0)p.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.PLAYERS,.18F,1+(float)reveal/route.size());
            if(p.tickCount%10==0)p.displayClientMessage(Component.literal("Room "+(room+1)+" / 5 • Memorize the path"),true);
            if(age>=previewTicks(room)){
                for(var tile:route){var pos=DesertTempleCourse.tile(c,room,tile);ActivityBlocks.set(p.serverLevel(),pos.below(),(Math.abs(pos.getX()-c.getX())%5==0?Blocks.CUT_SANDSTONE:Blocks.SMOOTH_SANDSTONE).defaultBlockState(),2);ActivityBlocks.set(p.serverLevel(),pos,Blocks.STONE_PRESSURE_PLATE.defaultBlockState(),2);}
                n.putInt("Phase",3);DesertTempleCourse.gate(p.serverLevel(),c,room,false);ActivityScenery.title(p,"REMEMBER THE PATH","");p.playNotifySound(SoundEvents.IRON_DOOR_OPEN,SoundSource.BLOCKS,.5F,.8F);OtherMotherLoop.changed(p);
            }return;
        }
        double rx=p.getX()-c.getX(),rz=p.getZ()-c.getZ();int x=(int)Math.floor(rx)+size/2,z=top-(int)Math.floor(rz);
        boolean board=x>=0&&x<size&&z>=0&&z<size;
        if(board){
            if(p.getY()>c.getY()+.3){fail(p,s,c);return;} // no jumping across pressure plates
            n.putBoolean("OnBoard",true);var route=path(s);int previous=n.getInt("Next"),next=TemplePath.step(route,previous,new TemplePath.Tile(x,z));
            if(next<0){fail(p,s,c);return;}if(next!=previous){n.putInt("Next",next);OtherMotherLoop.changed(p);p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.PLAYERS,.12F,.8F+next*.015F);}
            if(next==route.size()){
                DesertTempleCourse.door(p.serverLevel(),c,room,false);PATHS.remove(s.owner);n.putInt("Phase",room==4?4:1);if(room<4)n.putInt("Room",room+1);
                p.playNotifySound(SoundEvents.PLAYER_LEVELUP,SoundSource.PLAYERS,.6F,1.2F);p.serverLevel().sendParticles(ParticleTypes.FIREWORK,p.getX(),p.getY()+1,p.getZ(),16,.8,.8,.8,.04);OtherMotherLoop.changed(p);
            }
        }else if(n.getBoolean("OnBoard")||rz<top-size+1||Math.abs(rx)>size/2.0+1){fail(p,s,c);return;}
        if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Room "+(room+1)+" / 5"),true);
    }
    private static void fail(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(state(s).getInt("Phase")>=5)return;state(s).putInt("Phase",5);PATHS.remove(s.owner);OtherMotherLoop.changed(p);
        ActivityScenery.title(p,"THE TEMPLE REMEMBERS","One wrong step. No jackpot this time.");p.playNotifySound(SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,.6F,.5F);
        p.serverLevel().sendParticles(ParticleTypes.SMOKE,p.getX(),p.getY()+.5,p.getZ(),30,.8,.5,.8,.05);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));ActivityWorld.ensureExit(p,s);
    }
    private static void ensurePrize(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(p.tickCount%20!=0)return;var at=c.offset(0,2,-158);LoopProps.hold(p.serverLevel(),at);
        if(LoopProps.nearby(p.serverLevel(),s,at).stream().noneMatch(e->e.slot==710)){
            var prize=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.PRIZE_PLATE,Vec3.atBottomCenterOf(at),710);prize.food(0,new ItemStack(Items.NETHERITE_INGOT));prize.setCustomName(Component.literal("Claim the temple jackpot"));prize.setCustomNameVisible(true);
        }
    }
    static boolean takePrize(VisitData.Scene s,UUID owner){if(!s.owner.equals(owner)||state(s).getInt("Phase")!=4)return false;state(s).putInt("Phase",6);return true;}
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){if(prop.slot!=710)return false;if(!owns(p,s)||!takePrize(s,p.getUUID()))return true;OtherMotherLoop.changed(p);prop.discard();jackpot(p,s);ActivityWorld.ensureExit(p,s);return true;}
    static void jackpot(ServerPlayer p,VisitData.Scene s){
        // Existing activity top-tier scale, preserving the current visit's reward theme.
        if(s.loop.activityLoot()==OtherMotherDialogue.Loot.FOOD){ActivityTokens.give(p,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));for(int i=0;i<4;i++)ActivityTokens.give(p,FeastContent.serving(p.getRandom(),i));}
        else {ActivityTokens.give(p,new ItemStack(Items.NETHERITE_INGOT));ActivityTokens.give(p,new ItemStack(Items.DIAMOND,4+p.getRandom().nextInt(3)));ActivityTokens.give(p,new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get(),2));}
        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,.7F,1);
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){PATHS.remove(s.owner);ActivityScenery.clean(l,s,DesertTempleCourse.bounds(c).inflate(3));}
    static void removeBuild(ServerLevel l,BlockPos c){ActivityScenery.erase(l,DesertTempleCourse.bounds(c),c.below(2));}
    public static void abort(ServerPlayer p,VisitData.Scene s){if(s.loop.activity==OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE&&s.owner.equals(p.getUUID())){PATHS.remove(s.owner);state(s).putInt("Phase",5);OtherMotherLoop.changed(p);if(ActivityWorld.isActivity(p.level())){ActivityScenery.teleport(p,Vec3.atBottomCenterOf(center(p,s)));ActivityWorld.ensureExit(p,s);}}}
    private DesertTempleActivity(){}
}
