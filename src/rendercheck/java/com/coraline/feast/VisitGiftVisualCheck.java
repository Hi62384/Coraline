package com.coraline.feast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;
import java.util.*;

/** Development-only physical gift smoke check and complete playback of all 48 new voices. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class VisitGiftVisualCheck {
    private static VisitData.Scene scene;private static volatile boolean busy;private static boolean initialized,preview,done,falling;
    private static int ticks,stage,presentation=-1,clip;private static String pendingClip;
    private static final List<String> clips=new ArrayList<>();
    private static final Set<String> heard=new HashSet<>();
    private static SoundInstance watched;private static String watching;private static long started;private static boolean becameActive;
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/"+name));}}
    @SubscribeEvent public static void sound(PlaySoundEvent e){
        if(!Boolean.getBoolean("coraline.visitGiftVisualCheck")||e.getSound()==null)return;
        String name=e.getSound().getLocation().getPath();
        if(name.startsWith("other_mother_loot_")&&clips.contains(name)){
            check(watched==null,"Overlapping gift recordings");watched=e.getSound();watching=name;started=System.nanoTime();becameActive=false;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(done||!Boolean.getBoolean("coraline.visitGiftVisualCheck")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.pauseOnLostFocus=false;ticks++;
        try {
            if(watched!=null){
                boolean active=mc.getSoundManager().isActive(watched);becameActive|=active;double elapsed=(System.nanoTime()-started)/1e9;
                if(becameActive&&!active){
                    check(elapsed>=VisitAudio.length(watching)/20.0-.20,"Truncated voice "+watching);
                    heard.add(watching);com.mojang.logging.LogUtils.getLogger().info("GIFT_VOICE_PLAYBACK_COMPLETE {} {}s",watching,elapsed);watched=null;watching=null;
                }else check(elapsed<VisitAudio.length(watching)/20.0+6,"Voice failed to load or finish "+watching);
            }
            if(!initialized&&ticks>=40){
                initialized=true;
                for(var kind:OtherMotherDialogue.Loot.values())if(kind.expanded())for(var moment:OtherMotherDialogue.Moment.values())for(int i=0;i<3;i++)clips.add(OtherMotherDialogue.lootPool(kind,moment).sound(i));
                server(mc,()->{
                    var setup=RevisionVisualCheck.class.getDeclaredMethod("setup",Minecraft.class);setup.setAccessible(true);setup.invoke(null,mc);
                    var field=RevisionVisualCheck.class.getDeclaredField("s");field.setAccessible(true);scene=(VisitData.Scene)field.get(null);
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var realm=com.coraline.otherworld.RealmData.get(p.server).realms.get(scene.door);
                    realm.table=realm.doorPos;
                    for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++)p.serverLevel().setBlock(realm.doorPos.offset(x,-1,z),net.minecraft.world.level.block.Blocks.CHERRY_PLANKS.defaultBlockState(),2);
                    start(p,OtherMotherDialogue.Loot.MYSTERY);
                });return;
            }
            if(busy||scene==null||falling||ticks%10!=0)return;
            if(!preview){
                if(scene.loop.phase==OtherMotherLoopState.Phase.GIFTS){
                    int visual=scene.loop.opened?1:0;
                    if(visual!=presentation){presentation=visual;capture(mc,"visit_gift_"+stage+"_"+visual+".png");}
                    var props=mc.level.getEntitiesOfClass(ScenePropEntity.class,new AABB(scene.table).inflate(6),p->p.kind()==ScenePropEntity.GIFT);
                    var prop=props.stream().filter(p->p.kind()==ScenePropEntity.GIFT).findFirst().orElse(null);
                    if(prop!=null&&!scene.loop.opened){
                        var target=prop.position().add(0,.3,0);var delta=target.subtract(mc.player.getEyePosition());
                        mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
                        mc.gameMode.interact(mc.player,prop,net.minecraft.world.InteractionHand.MAIN_HAND);
                    }
                }
                if(scene.loop.phase==OtherMotherLoopState.Phase.WINDOW){
                    check(scene.loop.collected(),"Gift presentation did not award");
                    capture(mc,"visit_gift_"+stage+"_inventory.png");
                    if(stage==0){stage++;presentation=-1;server(mc,()->start(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),OtherMotherDialogue.Loot.CLOTHING));}
                    else {preview=true;falling=true;FortressFallVisualCheck.start(mc,scene);}
                }
            }else if(watched==null){
                if(pendingClip!=null){if(heard.contains(pendingClip))pendingClip=null;else return;}
                while(clip<clips.size()&&heard.contains(clips.get(clip)))clip++;
                if(clip==clips.size()){check(heard.size()==48,"Missing voice coverage");com.mojang.logging.LogUtils.getLogger().info("CORALINE_VISIT_GIFT_CLIENT_CHECK PASSED: 48 complete recordings and physical Mystery/Clothing visit flows");done=true;mc.stop();}
                else {String next=clips.get(clip++);pendingClip=next;server(mc,()->VisitAudio.play(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),next,false));}
            }
        }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_VISIT_GIFT_CLIENT_CHECK FAILED",ex);mc.stop();}
    }
    static void resumePreview(Minecraft mc){server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NewActivitySafety.abort(p,scene);ActivityWorld.exit(p,scene);scene.loop.phase=OtherMotherLoopState.Phase.WINDOW;scene.loop.portal=p.blockPosition().east(12);scene.loop.arrivalDeadline=OtherMotherLoop.now(p)+30000;falling=false;});}
    private interface Work{void run()throws Exception;}
    private static void server(Minecraft mc,Work work){busy=true;mc.getSingleplayerServer().execute(()->{try{work.run();}catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_VISIT_GIFT_CLIENT_CHECK FAILED",ex);mc.execute(mc::stop);}finally{busy=false;}});}
    private static void start(net.minecraft.server.level.ServerPlayer p,OtherMotherDialogue.Loot kind){
        if(scene.loop.active())OtherMotherLoop.resetForDebug(p,scene);
        var realm=com.coraline.otherworld.RealmData.get(p.server).realms.get(scene.door);OtherMotherLoop.entered(p,realm);
        scene.loop.loot=kind;scene.loop.extra=false;scene.loop.visits=2;
        scene.loop.giftBefore=OtherMotherDialogue.lootPool(kind,OtherMotherDialogue.Moment.BEFORE).sound(0);scene.loop.giftAfter=OtherMotherDialogue.lootPool(kind,OtherMotherDialogue.Moment.AFTER).sound(1);
        p.connection.teleport(scene.table.getX()+.5,scene.table.getY(),scene.table.getZ()-2.5,0,0);
        OtherMotherLoop.changed(p);
    }
}
