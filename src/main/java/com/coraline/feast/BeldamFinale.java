package com.coraline.feast;

import static com.coraline.feast.RescueChapter.Phase.*;
import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Audio-ended gates, physical Cat/Stone arcs, hearing-only web and manual final lock. */
public final class BeldamFinale {
    /** Direct operator checkpoint, including the original exit/reunion dialogue. */
    public static String debugAfterFinalBoss(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);
        if(!p.hasPermissions(2))return "Operator permission required. In singleplayer, enable cheats before using this debug item.";
        if(s==null){String initialized=FirstVisit.debugComplete(p);if(OtherMotherLoop.scene(p)==null)return initialized;return debugAfterFinalBoss(p);}
        if(!p.getUUID().equals(s.owner))return "This is another player's story session. Return to your own story before using this item.";
        if(p.serverLevel()!=p.server.overworld())return "Use this item in the Overworld.";
        if(AcceptedButtonEyes.active(s))return "Finish or reset the active button-eye storyline before changing branches.";
        if(RescueChapter.finished(s)||RescueChapter.phase(s).ordinal()>=REUNION_A.ordinal())return "The post-boss storyline has already started; no items duplicated.";
        var d=DoorData.get(p.server).doors.get(s.door);var realm=RealmData.get(p.server).realms.get(s.door);
        if(d==null||!s.owner.equals(d.owner))return "Your story has no matching owned Little Door. Use the door originally given by your parents.";
        if(realm==null||!realm.ready||realm.house==null){
            if(!OtherWorld.prepareDebugStory(p,d)){p.getPersistentData().putBoolean("CoralineDebugAfterFinalPending",true);return "Preparing your story world automatically. The post-boss checkpoint will start when it is ready.";}
        }
        p.serverLevel().getChunkAt(d.pos);var door=p.level().getBlockState(d.pos);
        if(!door.is(ModBlocks.TINY_DOOR.get()))return "Restore the original Little Door before using this checkpoint.";
        if(p.getInventory().getFreeSlot()<0)return "Make space for the recovered key and ghost eyes first.";
        boolean key=false;for(var stack:p.getInventory().items)key|=ParentRescueStory.key(stack,s);
        if(!key){
            if(!StoryItemCustody.held(s,"Key").isEmpty()){if(!StoryItemCustody.giveBack(p,s,"Key"))return "Free a slot for your original key.";}
            else if(s.loop.story.getBoolean("LittleDoorKeyIssued")){
                if(!StoryItemCustody.capture(p,s,"Key",stack->ParentRescueStory.key(stack,s))&&!StoryItemCustody.recoverSwallowedKey(p,s))return "Retrieve your existing Little Door Key; a duplicate will not be created.";
                if(!StoryItemCustody.giveBack(p,s,"Key"))return "Free a slot for your original key.";
            }else if(!ParentRescueStory.grantKey(p,s))return "Free a slot for the Little Door Key.";
        }
        for(int i=0;i<3;i++){var eye=RescueEyes.eye(s,i);eye.putBoolean("Discovered",true);eye.putBoolean("Defeated",true);if(!RescueEyes.claim(p,s,i))return "Free inventory space for the three ghost eyes, then use again.";}
        if(!StoryItemCustody.held(s,"Stone").isEmpty()&&!StoryItemCustody.giveBack(p,s,"Stone"))return "Free a slot for your Seeing Stone.";
        if(!RescueChapter.transfer(p,p.server.overworld(),d.pos))return "Clear a safe landing beside your Little Door.";
        ShowPlayback.stop(p,s);VisitAudio.stop(p);RescueBosses.hideBar(p);p.stopUsingItem();
        s.stage=VisitData.Stage.DONE;var first=FirstEscapeStory.data(s);first.putBoolean("Started",true);first.putBoolean("Complete",true);first.putString("Phase",FirstEscapeTimeline.Phase.ESCAPE_COMPLETE.name());first.remove("Recover");MissingParentsCat.data(s).putBoolean("Complete",true);MissingParentsCat.data(s).putBoolean("TunnelRestored",true);
        var rescue=ParentRescueStory.data(s);rescue.putBoolean("Started",true);rescue.putBoolean("Complete",true);rescue.putBoolean("Seated",false);
        var scenery=ParentRescueStory.scenery(s);scenery.putLong("House",realm.house.asLong());scenery.putLong("LittleDoor",realm.doorPos.asLong());scenery.putLong("Prison",realm.house.above(80).asLong());
        var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,Vec3.atBottomCenterOf(d.pos).add(0,.2,1));
        if(hand==null)return "The original Hand could not be staged; retry the checkpoint before the farewell.";
        hand.frame(1,0,1);
        var n=RescueChapter.data(s);n.putBoolean("Started",true);n.putBoolean("DebugFinalFightSkipped",true);n.putBoolean("KeyRecovered",true);n.putBoolean("CrawlReachedOverworld",true);n.putBoolean("PermanentlySealed",true);n.putBoolean("BangingDone",true);n.remove("Recover");
        n.putBoolean("HandSevered",true);
        if(!p.serverLevel().setBlock(d.pos,door.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false),3))return "The Little Door could not be sealed; retry the checkpoint.";
        rescue.putBoolean("DoorLocked",true);d.activeUntil=0;DoorData.get(p.server).setDirty();
        if(!RescueEpilogue.begin(p,s)){RescueChapter.go(p,s,BANGING);n.putInt("Ticks",120);}
        OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();return "Final boss completed. The Little Door exit and reunion dialogue is starting.";
    }
    /** Explicit operator shortcut: skip the fight, not the original key/door or epilogue. */
    public static String debugSkipFight(ServerPlayer p,VisitData.Scene s){
        if(!p.hasPermissions(2)||s==null||!p.getUUID().equals(s.owner))return "Owner/operator authorization required.";
        if(!RescueChapter.owns(p,s)||RescueChapter.phase(s).ordinal()<FINAL_A.ordinal()){
            s.loop.story.putBoolean("DebugSkipFinalFightPending",true);OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();
            return "Boss skip armed. Continue the remaining cutscenes and eye quests; the final fight will be skipped automatically when it begins.";
        }
        var state=RescueChapter.phase(s);
        if(state.ordinal()<FINAL_A.ordinal())return "Reach the final Other Mother encounter first; the three eye quests are not skipped.";
        if(state.ordinal()>=LOCK_DOOR.ordinal())return "The final fight is already over. No progress or items duplicated.";
        if(!RescueEyes.all(s))return "Collect your three original ghost eyes before skipping the final encounter.";
        var d=DoorData.get(p.server).doors.get(s.door);var level=d==null?null:FamilyData.level(p.server,d.dimension);
        if(d==null||!s.owner.equals(d.owner)||level==null)return "Your original Little Door is unavailable; nothing skipped.";
        level.getChunkAt(d.pos);if(!level.getBlockState(d.pos).is(ModBlocks.TINY_DOOR.get()))return "Restore your original Little Door before skipping.";
        boolean key=false;for(int i=0;i<p.getInventory().getContainerSize();i++)key|=ParentRescueStory.key(p.getInventory().getItem(i),s);
        if(!key){if(!StoryItemCustody.recoverSwallowedKey(p,s)||!StoryItemCustody.giveBack(p,s,"Key"))return "Retrieve the original key or free an inventory slot; no duplicate key created.";}
        if(!StoryItemCustody.held(s,"Stone").isEmpty()&&!StoryItemCustody.giveBack(p,s,"Stone"))return "Free an inventory slot for the original Seeing Stone before skipping.";
        if(!RescueChapter.transfer(p,level,d.pos))return "Clear a safe landing beside the original Little Door, then retry.";
        ShowPlayback.stop(p,s);VisitAudio.stop(p);p.stopUsingItem();RescueBosses.hideBar(p);
        // The skipped door struggle normally severs the original Hand. Keep
        // that physical actor and its saved identity for the later well chase.
        var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,net.minecraft.world.phys.Vec3.atBottomCenterOf(d.pos).add(0,.2,1));
        if(hand==null)return "The original Hand could not be staged; the skip has not completed.";
        var n=RescueChapter.data(s);s.loop.story.remove("DebugSkipFinalFightPending");n.putBoolean("DebugFinalFightSkipped",true);n.putBoolean("KeyRecovered",true);n.putBoolean("CrawlReachedOverworld",true);n.remove("CatHeld");n.remove("CatFlying");n.remove("Recover");n.remove("Press");
        hand.frame(1,0,1);n.putBoolean("HandSevered",true);
        s.loop.story.putString("LittleDoorKeyHolder","PLAYER");
        var mother=ParentRescueStory.mother(p,s);if(mother!=null){mother.getNavigation().stop();mother.setDeltaMovement(Vec3.ZERO);mother.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);mother.setInvisible(true);}
        RescueChapter.go(p,s,LOCK_DOOR);RescueSiteCleanup.tick(p,s);OtherMotherLoop.changed(p);
        return "Final encounter skipped. Use your recovered key to lock the Little Door; the reunion continues normally.";
    }
    private static BlockPos little(VisitData.Scene s){return BlockPos.of(ParentRescueStory.scenery(s).getLong("LittleDoor"));}
    private static Vec3 hearth(VisitData.Scene s){return Vec3.atBottomCenterOf(FirstEscapeScene.room(s)).add(-2,.7,5);}
    static float face(Vec3 from,Vec3 to){return (float)Math.toDegrees(Math.atan2(from.x-to.x,to.z-from.z));}
    /** Exported rig button line: 1.501 - (9 - 19 - 7) / 16, not vanilla parent's chest-height eyes. */
    public static Vec3 facePoint(Entity mother){double yaw=Math.toRadians(mother.getYRot());return mother.position().add(-Math.sin(yaw)*.14,2.5635,Math.cos(yaw)*.14);}
    public static AABB faceBox(Entity mother){var at=facePoint(mother);return new AABB(at.x-.27,at.y-.29,at.z-.27,at.x+.27,at.y+.29,at.z+.27);}
    public static boolean catHitBeforeBlock(Entity mother,Vec3 from,Vec3 to,HitResult block){var hit=faceBox(mother).clip(from,to);return hit.isPresent()&&(block.getType()==HitResult.Type.MISS||from.distanceToSqr(hit.get())<from.distanceToSqr(block.getLocation()));}
    public static Vec3 catThrowVelocity(Vec3 from,Vec3 aim){var delta=aim.subtract(from);double ticks=Math.max(1,delta.horizontalDistance()/1.2);return new Vec3(delta.x/ticks,delta.y/ticks+.045*(ticks-1)/2,delta.z/ticks);}
    static Vec3 catThrowOrigin(ServerPlayer p){var look=p.getLookAngle().normalize();var eye=p.getEyePosition();var hand=eye.add(look.scale(.65)).add(0,-.45,0);var trace=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(eye,hand,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));return trace.getType()==HitResult.Type.MISS?hand:trace.getLocation().subtract(look.scale(.08));}
    private static void motherAt(ParentEntity m,Vec3 at,ServerPlayer p,int gesture){if(m==null)return;m.stopRiding();m.getNavigation().stop();m.setNoAi(true);m.setNoGravity(true);m.noPhysics=true;m.setInvisible(false);m.setPos(at);m.setYRot(face(at,p.position()));m.setYBodyRot(m.getYRot());m.setYHeadRot(m.getYRot());m.familyGesture(gesture,5);}
    public static boolean preflight(ServerPlayer p,VisitData.Scene s){
        var state=RescueChapter.phase(s);var n=RescueChapter.data(s);
        if(state.ordinal()>=FINAL_I.ordinal()&&state.ordinal()<=FINAL_N.ordinal()&&!n.getBoolean("KeyRecovered")&&!StoryItemCustody.recoverSwallowedKey(p,s)){n.putString("Blocker","The swallowed key cannot be recovered: restore its saved custody record or bring the surviving owner-bound original out of storage.");return false;}
        if(state==FINAL_E&&!n.getBoolean("StoneTaken")){if(!StoryItemCustody.capture(p,s,"Stone",stack->StoryItemCustody.stone(stack,s))){n.putString("Blocker","Bring your original Seeing Stone out of storage. Unrelated stones are not substitutes.");return false;}p.stopUsingItem();n.putBoolean("StoneTaken",true);}
        if(state==TUNNEL_CHASE&&!n.getBoolean("PassageEntered")&&!RescuePassage.enter(p,s))return false;
        n.remove("Blocker");return true;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state,int t,boolean done){
        var n=RescueChapter.data(s);var m=ParentRescueStory.mother(p,s);
        if(state.ordinal()<=FINAL_N.ordinal()&&m==null){n.putString("Blocker","Waiting for the saved Other Mother, not creating another Beldam.");return;}
        if(state.ordinal()<=FINAL_N.ordinal()){m.setInvisible(false);m.setNoAi(true);m.getNavigation().stop();m.lookAtStoryPlayer(p);}
        if(state.ordinal()<=FINAL_O.ordinal())RescueWebSite.prepare(p,s);
        switch(state){
            case FINAL_A->{motherAt(m,Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)),p,66);if(done)RescueChapter.go(p,s,FINAL_B);}
            case FINAL_B->{m.familyGesture(67,5);if(done)RescueChapter.go(p,s,FINAL_C);}
            case FINAL_C->{m.familyGesture(66,5);if(done)RescueChapter.go(p,s,SHOW_EYES);}
            case SHOW_EYES->RescueChapter.hint(p,"Show the three ghost eyes.");
            case FINAL_D->{showEyes(p,s,t);m.familyGesture(66,5);if(done)RescueChapter.go(p,s,FINAL_E);}
            case FINAL_E->{m.familyGesture(64,5);m.setItemSlot(EquipmentSlot.MAINHAND,StoryItemCustody.held(s,"Stone"));if(done)RescueChapter.go(p,s,FINAL_F);}
            case FINAL_F->{
                m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);var target=hearth(s);var start=m.position().add(0,1.65,-.4);double u=Math.min(1,t/40.0);var point=start.lerp(target,u).add(0,Math.sin(u*Math.PI)*1.3,0);var stone=RescueProps.ensure(p,s,"ThrownStone",RescueProps.STONE,0,point);if(stone!=null){stone.setPos(point);stone.frame(0,t,1);}
                if(u>=1&&!n.getBoolean("StoneDestroyed")){StoryItemCustody.destroy(s,"Stone");n.putBoolean("StoneDestroyed",true);RescueProps.hide(p,s,"ThrownStone");p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,target.x,target.y,target.z,18,.2,.3,.2,.025);OtherMotherLoop.changed(p);}
                if(done&&n.getBoolean("StoneDestroyed"))RescueChapter.go(p,s,FINAL_G);
            }
            case FINAL_G->{m.familyGesture(66,5);if(done)RescueChapter.go(p,s,HIDING_CHOICE);}
            case HIDING_CHOICE->RescueChapter.hint(p,"Where have you hidden them?");
            case FINAL_H,WARDROBE_ASIDE->{
                if(t>=40||state==WARDROBE_ASIDE)moveWardrobe(p,s,m);
                if((done||state==WARDROBE_ASIDE)&&n.getBoolean("WardrobeClear"))RescueChapter.go(p,s,FINAL_I);
                else if(done)RescueChapter.go(p,s,WARDROBE_ASIDE);
            }
            case FINAL_I->{m.familyGesture(65,5);if(t>=20){var key=StoryItemCustody.held(s,"Key");if(key.isEmpty())return;m.setItemSlot(EquipmentSlot.MAINHAND,key);n.putBoolean("KeyCoughed",true);}if(done&&n.getBoolean("KeyCoughed"))RescueChapter.go(p,s,FINAL_J);}
            case FINAL_J->{m.familyGesture(64,5);m.setItemSlot(EquipmentSlot.MAINHAND,StoryItemCustody.held(s,"Key"));if(done)RescueChapter.go(p,s,FINAL_K);}
            case FINAL_K->{
                var at=Vec3.atBottomCenterOf(little(s).east());moveMother(m,at,.045,p,64);
                if(t>=Math.max(20,ShowRecordings.duration("finale_k")-45)&&m.distanceToSqr(at)<1){var frame=p.serverLevel().getBlockState(little(s));if(frame.is(ModBlocks.TINY_DOOR.get())&&!StoryItemCustody.held(s,"Key").isEmpty()){p.serverLevel().setBlock(little(s),frame.setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,false),3);n.putBoolean("DoorUnlocked",true);ParentRescueStory.data(s).putBoolean("DoorLocked",false);}}
                if(done&&n.getBoolean("DoorUnlocked"))RescueChapter.go(p,s,FIGHT_CHOICE);
            }
            case FIGHT_CHOICE->RescueChapter.hint(p,"Fight back.");
            case FINAL_M->{catAtMantel(p,s);if(done)RescueChapter.go(p,s,CAT_THROW);}
            case CAT_THROW->{
                var cat=catAtMantel(p,s);if(cat==null)return;moveMother(m,p.position(),.018,p,67);
                if(n.getBoolean("CatFlying"))catFlight(p,s,cat,m);else if(n.getBoolean("CatHeld")){cat.setPos(p.getEyePosition().add(p.getLookAngle().scale(.65)).add(0,-.45,0));RescueChapter.hint(p,"Aim at Other Mother's face and left-click to throw the Cat.");}else RescueChapter.hint(p,"Right-click the Cat on the mantel, then aim and throw.");
                if(!n.getBoolean("CatFlying")&&m.distanceToSqr(p)<1.8)AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS);
            }
            case FINAL_N->{
                int duration=ShowRecordings.duration("finale_n");m.familyGesture(67,5);var cat=ShowActors.actor(p,s,"cat");
                if(RescueWebLayout.blindAt(t,duration))n.putBoolean("BeldamBlind",true);
                if(RescueWebLayout.swatAt(t,duration)&&cat!=null){if(!n.contains("CatSwatX")){n.putDouble("CatSwatX",cat.getX());n.putDouble("CatSwatY",cat.getY());n.putDouble("CatSwatZ",cat.getZ());}double u=Math.min(1,(t-(duration-40))/25.0);var from=new Vec3(n.getDouble("CatSwatX"),n.getDouble("CatSwatY"),n.getDouble("CatSwatZ"));var land=Vec3.atBottomCenterOf(FirstEscapeScene.entrance(s).west(3));cat.setPos(from.lerp(land,u).add(0,Math.sin(u*Math.PI)*1.3,0));ShowPlaybackNetwork.pose(cat,34,t,1);if(u>=1)n.putBoolean("CatEscaped",true);}
                if(t>=duration-50&&!n.getBoolean("KeyRecovered")){m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);if(StoryItemCustody.giveBack(p,s,"Key")){n.putBoolean("KeyRecovered",true);s.loop.story.putString("LittleDoorKeyHolder","PLAYER");}else RescueChapter.hint(p,"Make an inventory space to recover your original key.");}
                if(done&&n.getBoolean("CatEscaped")&&n.getBoolean("KeyRecovered"))RescueChapter.go(p,s,FINAL_O);
            }
            case FINAL_O->{
                if(RescueWebSite.prepare(p,s)&&t>=30&&!n.getBoolean("FloorOpened")){for(int x=14;x<=30;x++)for(int z=37;z<=51;z++)FirstEscapeScene.place(p,s,FirstEscapeScene.at(s,x,5,z),Blocks.AIR.defaultBlockState());n.putBoolean("FloorOpened",true);}
                if(n.getBoolean("FloorOpened")&&p.getY()<FirstEscapeScene.room(s).getY()-2&&!n.getBoolean("WebEntered")){enterWeb(p,s,m);}
                if(done&&n.getBoolean("WebEntered"))RescueChapter.go(p,s,FINAL_P);
            }
            case FINAL_P->{n.putFloat("BeldamMorph",Math.min(1,t/(float)Math.max(1,ShowRecordings.duration("finale_p"))));if(done)RescueChapter.go(p,s,FINAL_Q);}
            case FINAL_Q->{if(m!=null){m.setInvisible(t<120);motherAt(m,Vec3.atBottomCenterOf(RescueWebSite.center(p,s)),p,67);m.setInvisible(t<120);}if(done){n.remove("NoiseX");rememberPosition(p,n);RescueChapter.go(p,s,WEB_STEALTH);}}
            case WEB_STEALTH->web(p,s,m,t);
            case DOOR_KICK->{RescueChapter.hint(p,"Kick the door open!");if(t==1)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,net.minecraft.sounds.SoundSource.BLOCKS,.8F,.8F);if(t>=20){n.putFloat("Struggle",0);RescueChapter.go(p,s,DOOR_STRUGGLE);}}
            case DOOR_STRUGGLE->{
                float progress=RescueChallengeMath.struggle(n.getFloat("Struggle"),n.getBoolean("Press")?1:0,n.getBoolean("AccessibleStruggle"));n.remove("Press");n.putFloat("Struggle",progress);RescueChapter.hint(p,"Rapidly click to pull the door shut — "+Math.round(progress*100)+"%");
                if(RescueWebLayout.struggleFinished(t,progress)){n.putBoolean("HandSevered",true);var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,RescueWebSite.at(p,s,1,1).add(0,.15,-.7));if(hand!=null)hand.frame(1,0,1);RescueChapter.go(p,s,HAND_FALL);}else if(t>=240)retry(p,s,DOOR_STRUGGLE,"You lost your grip. Try closing the door again.");
            }
            case HAND_FALL->{RescuePassage.prepare(p,s);var hand=RescueProps.find(p,s,"SeveredHand");if(hand!=null){hand.frame(1,t,1);if(t<20)hand.setPos(hand.position().add(0,-.015,0));}if(t>=60&&RescuePassage.prepare(p,s)){n.remove("PassageEntered");RescueChapter.go(p,s,TUNNEL_CHASE);}}
            case TUNNEL_CHASE->tunnel(p,s,t,done);
            case LOCK_DOOR->RescueChapter.hint(p,"Use your recovered key to lock the Little Door.");
            case BANGING->{if(t<=120&&t%20==0)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,net.minecraft.sounds.SoundSource.BLOCKS,(1-t/140F)*.8F,.7F);if(t>=120){n.putBoolean("BangingDone",true);RescueEpilogue.begin(p,s);}}
            default->{}
        }
        if(n.getBoolean("WebEntered")&&state.ordinal()>=FINAL_O.ordinal()&&state.ordinal()<=WEB_STEALTH.ordinal()&&p.getY()<RescueWebSite.center(p,s).getY()-6)retry(p,s,WEB_STEALTH,"You slipped off the safe strands. Retry from the web entrance.");
    }
    private static void showEyes(ServerPlayer p,VisitData.Scene s,int t){for(int i=0;i<3;i++){var at=p.position().add(p.getLookAngle().multiply(.8,0,.8)).add((i-1)*.45,1.1,0);var e=RescueProps.ensure(p,s,"PresentedEye"+i,RescueProps.EYE,i,at);if(e!=null){e.setPos(at);e.frame(1,t,1);}}}
    private static boolean physicalEyes(ServerPlayer p,VisitData.Scene s){for(int i=0;i<3;i++){boolean found=false;for(int slot=0;slot<p.getInventory().getContainerSize();slot++)found|=RescueEyes.authentic(p.getInventory().getItem(slot),s,i);if(!found)return false;}return RescueEyes.all(s);}
    private static void moveMother(ParentEntity m,Vec3 target,double speed,ServerPlayer p,int gesture){if(m==null)return;var delta=target.subtract(m.position()).multiply(1,0,1);var next=m.position().add(delta.length()<=speed?delta:delta.normalize().scale(speed));motherAt(m,next,p,gesture);}
    private static void moveWardrobe(ServerPlayer p,VisitData.Scene s,ParentEntity m){
        var n=RescueChapter.data(s);var wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");if(wardrobe==null)return;var door=little(s);var goal=Vec3.atBottomCenterOf(door).add(0,0,4);moveMother(m,wardrobe.position().add(1.5,0,0),.04,p,64);if(m.distanceToSqr(wardrobe)>12)return;
        var delta=goal.subtract(wardrobe.position());wardrobe.setPos(wardrobe.position().add(delta.length()<.055?delta:delta.normalize().scale(.055)));wardrobe.frame(1,n.getInt("Ticks"),1);
        var portal=new AABB(door).inflate(.3,.1,.3);if(!wardrobe.getBoundingBox().intersects(portal)&&wardrobe.distanceToSqr(goal)<.1){n.putBoolean("WardrobeClear",true);ParentRescueStory.data(s).putBoolean("WardrobeBlocking",false);}
    }
    static Mob catAtMantel(ServerPlayer p,VisitData.Scene s){var n=RescueChapter.data(s);var at=hearth(s).add(0,.7,0);var cat=ShowActors.acquire(p,s,"cat",p.serverLevel(),at);if(cat==null)return null;if(!n.getBoolean("CatMantel")){cat.stopRiding();cat.setNoAi(true);cat.setNoGravity(true);cat.setDeltaMovement(Vec3.ZERO);cat.setPos(at);n.putBoolean("CatMantel",true);}cat.setInvisible(false);n.putInt("Cat",cat.getId());LoopProps.hold(p.serverLevel(),cat.blockPosition());return cat;}
    private static void catFlight(ServerPlayer p,VisitData.Scene s,Mob cat,ParentEntity mother){var n=RescueChapter.data(s);int t=n.getInt("CatFlightTick")+1;n.putInt("CatFlightTick",t);var from=cat.position();var v=new Vec3(n.getDouble("CatVX"),n.getDouble("CatVY"),n.getDouble("CatVZ"));var next=from.add(v);var trace=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(from,next,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,cat));
        if(catHitBeforeBlock(mother,from,next,trace)){cat.setPos(facePoint(mother));n.remove("CatFlying");n.remove("CatHeld");n.putBoolean("CatHit",true);RescueChapter.go(p,s,FINAL_N);return;}
        if(trace.getType()!=HitResult.Type.MISS||t>45){cat.setPos(trace.getType()==HitResult.Type.MISS?next:trace.getLocation());retry(p,s,CAT_THROW,"The Cat missed. Pick him up and aim at Other Mother's face.");return;}cat.setPos(next);n.putDouble("CatVY",v.y-.045);ShowPlaybackNetwork.pose(cat,34,t,1);
    }
    private static void enterWeb(ServerPlayer p,VisitData.Scene s,ParentEntity mother){var n=RescueChapter.data(s);var l=p.server.getLevel(ActivityWorld.KEY);var at=RescueWebSite.at(p,s,1,0);l.getChunkAt(BlockPos.containing(at));p.stopRiding();p.setForcedPose(null);p.setPose(Pose.STANDING);p.teleportTo(l,at.x,at.y+1,at.z,90,0);p.fallDistance=0;n.putBoolean("WebEntered",true);if(mother!=null){var moved=ShowTravel.guest(mother,p,s,l,Vec3.atBottomCenterOf(RescueWebSite.center(p,s)),true,"BeldamFinaleReturn");if(moved!=null&&s.mom.equals(moved.getUUID()))n.putLong("BeldamAt",moved.blockPosition().asLong());}var web=RescueProps.ensure(p,s,"Web",RescueProps.WEB,0,Vec3.atLowerCornerOf(RescueWebSite.center(p,s)));if(web!=null)web.frame(0,0,1);}
    private static void rememberPosition(ServerPlayer p,CompoundTag n){n.putDouble("PreviousX",p.getX());n.putDouble("PreviousY",p.getY());n.putDouble("PreviousZ",p.getZ());n.putBoolean("PreviousGround",p.onGround());}
    private static void web(ServerPlayer p,VisitData.Scene s,ParentEntity mother,int t){
        var n=RescueChapter.data(s);var center=RescueWebSite.center(p,s);double moved=Math.hypot(p.getX()-n.getDouble("PreviousX"),p.getZ()-n.getDouble("PreviousZ"));boolean sticky=false;double best=3;for(int lane=0;lane<3;lane++)for(int sample=0;sample<=RescueWebLayout.SAMPLES;sample+=4){var point=RescueWebSite.at(p,s,lane,sample/(double)RescueWebLayout.SAMPLES);double d=p.position().distanceToSqr(point);if(d<best){best=d;sticky=RescueWebLayout.sticky(lane,sample);}}
        if(sticky&&p.onGround())p.makeStuckInBlock(ModBlocks.STORY_WEB.get().defaultBlockState(),new Vec3(.65,1,.65));
        boolean landed=RescueWebLayout.audibleLanding(n.getDouble("PreviousY"),p.getY(),p.onGround()&&!n.getBoolean("PreviousGround"));
        if(t<=1){for(int i=0;i<5;i++)n.remove("WebTravel"+i);n.remove("WebTravelSamples");n.putInt("HearingNoiseEvents",0);}
        var movement=RescueWebHearing.consume(p,moved);double step=movement.loudest(),travel=movement.distance();int samples=movement.packets();
        boolean noise=RescueChallengeMath.hears(step,p.isCrouching(),p.isSprinting(),landed,sticky);n.putDouble("LastWebTravel",travel);n.putDouble("LastWebStep",step);n.putInt("LastWebPackets",samples);
        n.putDouble("LastWebMoved",moved);n.putBoolean("LastWebSticky",sticky);n.putBoolean("LastWebLanded",landed);
        if(noise){n.putString("LastNoiseReason",landed?"LANDING":sticky?"STICKY":p.isSprinting()?"SPRINT":p.isCrouching()?"FAST_CROUCH":"WALK");n.putDouble("LastNoiseMoved",step);n.putDouble("LastNoiseTravel",travel);n.putInt("LastNoiseSamples",samples);int events=n.getInt("HearingNoiseEvents");n.putInt("HearingNoiseEvents",events+1);if(events<12)org.slf4j.LoggerFactory.getLogger(BeldamFinale.class).debug("Web noise t={} displacement={} loudestStep={} acceptedTravel={} packets={} crouching={} sprint={} landing={} sticky={}",t,moved,step,travel,samples,p.isCrouching(),p.isSprinting(),landed,sticky);n.putDouble("NoiseX",p.getX());n.putDouble("NoiseY",p.getY());n.putDouble("NoiseZ",p.getZ());n.putInt("NoiseAge",0);if(t%6==0)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SPIDER_STEP,net.minecraft.sounds.SoundSource.BLOCKS,.3F,1.4F);}else n.putInt("NoiseAge",n.getInt("NoiseAge")+1);
        // Pursuit target is only the most recently heard position, never p.position().
        if(mother!=null){mother.setInvisible(false);mother.setNoAi(true);mother.setNoGravity(true);mother.noPhysics=true;if(n.contains("NoiseX")&&n.getInt("NoiseAge")<160){var heard=new Vec3(n.getDouble("NoiseX"),n.getDouble("NoiseY")-.5,n.getDouble("NoiseZ"));var delta=heard.subtract(mother.position());mother.setPos(mother.position().add(delta.length()<.095?delta:delta.normalize().scale(.095)));mother.setYRot(face(mother.position(),heard));mother.setYBodyRot(mother.getYRot());}n.putLong("BeldamAt",mother.blockPosition().asLong());if(mother.getBoundingBox().inflate(.35).intersects(p.getBoundingBox())){AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS);return;}}
        rememberPosition(p,n);var end=RescueWebSite.at(p,s,1,1);var door=RescueProps.ensure(p,s,"WebDoor",RescueProps.PURSUING_DOOR,0,end.add(0,0,-1));if(door!=null)door.frame(0,t,1);RescueChapter.hint(p,sticky?"Sticky strand — keep still to stop making noise.":"Crouch and follow the thick strands. She follows sound, not sight.");if(p.distanceToSqr(end)<4&&p.getY()>end.y-.6)RescueChapter.go(p,s,DOOR_KICK);
    }
    private static void tunnel(ServerPlayer p,VisitData.Scene s,int t,boolean done){
        var n=RescueChapter.data(s);p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);p.fallDistance=0;
        double progress=RescuePassage.progress(p,s),pursuit=RescueWebLayout.crawlPursuit(t);var origin=RescuePassage.origin(p,s);
        var door=RescueProps.ensure(p,s,"PursuingDoor",RescueProps.PURSUING_DOOR,0,Vec3.atBottomCenterOf(origin).add(pursuit,0,0));
        if(door!=null){door.setPos(Vec3.atBottomCenterOf(origin).add(pursuit,0,0));door.setYRot(90);door.frame(2,t,1);}
        RescueChapter.hint(p,"CRAWL! Dodge the rubble and reach the real door.");
        if(progress<pursuit+1.1||progress<-.5||Math.abs(p.getZ()-origin.getZ()-.5)>2){retry(p,s,TUNNEL_CHASE,"The door caught up. Retry the crawl with your recovered key.");return;}
        if(progress<RescuePassage.LENGTH||!done)return;
        var source=DoorData.get(p.server).doors.get(s.door);if(source==null||!s.owner.equals(source.owner))return;
        var level=FamilyData.level(p.server,source.dimension);if(level==null||level==p.server.getLevel(ActivityWorld.KEY))return;
        if(!RescueChapter.transfer(p,level,source.pos))return;
        n.putBoolean("CrawlReachedOverworld",true);RescueChapter.go(p,s,LOCK_DOOR);
    }
    public static void input(ServerPlayer p,VisitData.Scene s,String action){
        var state=RescueChapter.phase(s);var n=RescueChapter.data(s);
        if(state==SHOW_EYES&&action.equals("show_eyes")&&physicalEyes(p,s))RescueChapter.go(p,s,FINAL_D);
        else if(state==HIDING_CHOICE&&action.equals("behind_little_door"))RescueChapter.go(p,s,FINAL_H);
        else if(state==FIGHT_CHOICE&&action.equals("fight"))RescueChapter.go(p,s,FINAL_M);
        else if(state==DOOR_STRUGGLE&&action.equals("struggle"))n.putBoolean("Press",true);
        else if(state==CAT_THROW&&action.equals("throw_cat")&&n.getBoolean("CatHeld")&&!n.getBoolean("CatFlying")){
            var cat=ShowActors.actor(p,s,"cat");if(cat==null)return;
            var look=p.getLookAngle().normalize();var from=catThrowOrigin(p);var v=look.scale(1.25).add(0,.18,0);var mother=ParentRescueStory.mother(p,s);
            if(mother!=null){var hit=faceBox(mother).clip(p.getEyePosition(),p.getEyePosition().add(look.scale(24)));if(hit.isPresent())v=catThrowVelocity(from,hit.get());}
            cat.setPos(from);n.putDouble("CatVX",v.x);n.putDouble("CatVY",v.y);n.putDouble("CatVZ",v.z);n.putInt("CatFlightTick",0);n.putBoolean("CatFlying",true);
        }
    }
    public static boolean interact(ServerPlayer p,VisitData.Scene s,Entity target){var n=RescueChapter.data(s);if(RescueChapter.phase(s)==CAT_THROW&&target==ShowActors.actor(p,s,"cat")&&!n.getBoolean("CatFlying")){n.putBoolean("CatHeld",true);return true;}return false;}
    public static boolean block(ServerPlayer p,BlockPos at){var s=OtherMotherLoop.scene(p);if(!RescueChapter.owns(p,s)||RescueChapter.phase(s)!=LOCK_DOOR)return false;var n=RescueChapter.data(s);var d=DoorData.get(p.server).doors.get(s.door);if(d==null||!s.owner.equals(d.owner)||p.serverLevel()!=FamilyData.level(p.server,d.dimension)||!at.equals(d.pos)||p.distanceToSqr(at.getCenter())>16)return false;boolean key=false;for(int i=0;i<p.getInventory().getContainerSize();i++)key|=ParentRescueStory.key(p.getInventory().getItem(i),s);if(!key||!n.getBoolean("CrawlReachedOverworld")||!n.getBoolean("KeyRecovered"))return true;var frame=p.serverLevel().getBlockState(at);if(!frame.is(ModBlocks.TINY_DOOR.get()))return true;p.serverLevel().setBlock(at,frame.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false),3);n.putBoolean("PermanentlySealed",true);ParentRescueStory.data(s).putBoolean("DoorLocked",true);d.activeUntil=0;DoorData.get(p.server).setDirty();RescueChapter.go(p,s,BANGING);return true;}
    public static void retry(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase state,String reason){
        var n=RescueChapter.data(s);n.putString("LastRetryReason",reason);n.putString("LastRetryPhase",state.name());n.putInt("RetryCount",n.getInt("RetryCount")+1);org.slf4j.LoggerFactory.getLogger(BeldamFinale.class).debug("Rescue retry phase={} reason={} feet={} crouching={} moved={} sticky={} landed={} noiseAge={} lastNoise={} lastNoiseMoved={}",state,reason,p.position(),p.isCrouching(),n.getDouble("LastWebMoved"),n.getBoolean("LastWebSticky"),n.getBoolean("LastWebLanded"),n.getInt("NoiseAge"),n.getString("LastNoiseReason"),n.getDouble("LastNoiseMoved"));RescueChapter.hint(p,reason);p.setHealth(p.getMaxHealth());p.clearFire();p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        if(state==CAT_THROW){
            if(!RescueChapter.transfer(p,p.server.getLevel(OtherWorld.KEY),FirstEscapeScene.room(s)))return;
            n.remove("CatFlying");n.remove("CatHeld");n.remove("CatMantel");var mother=ParentRescueStory.mother(p,s);if(mother!=null)motherAt(mother,Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)),p,67);catAtMantel(p,s);
        }else if(state==WEB_STEALTH){
            var at=RescueWebSite.at(p,s,1,0);if(!RescueChapter.transfer(p,p.server.getLevel(ActivityWorld.KEY),BlockPos.containing(at).above()))return;
            var mother=ParentRescueStory.mother(p,s);if(mother!=null)mother.setPos(Vec3.atBottomCenterOf(RescueWebSite.center(p,s)));n.remove("NoiseX");rememberPosition(p,n);
        }else if(state==DOOR_STRUGGLE){
            var at=RescueWebSite.at(p,s,1,1);if(!RescueChapter.transfer(p,p.server.getLevel(ActivityWorld.KEY),BlockPos.containing(at).above()))return;n.putFloat("Struggle",0);n.remove("Press");
        }else if(state==TUNNEL_CHASE){n.remove("PassageEntered");if(!RescuePassage.enter(p,s))return;}
        RescueChapter.go(p,s,state);
    }
    private BeldamFinale(){}
}
