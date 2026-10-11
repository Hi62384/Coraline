package com.coraline.clothing;

import com.coraline.client.clothing.ClothingLight;
import com.coraline.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in disposable-world render and moving-light check. Not packaged in releases. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ClothingVisualCheck {
    private static boolean started;private static volatile int phase;private static int ticks;
    private static java.util.UUID outlineMob;
    private static boolean previousParentSpawning;
    private static final BlockPos ORIGIN=new BlockPos(32400,100,32400);
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.clothingVisualCheck")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.pauseOnLostFocus=false;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!started){started=true;org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=mc.getSingleplayerServer().overworld();
            p.getPersistentData().remove("CoralineVisitDoor");p.getPersistentData().remove("CoralineInTunnel");p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            previousParentSpawning=com.coraline.config.CoralineConfig.PARENTS_SPAWN.get();
            com.coraline.config.CoralineConfig.PARENTS_SPAWN.set(false);
            for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(ORIGIN).inflate(30)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
            for(int x=-18;x<=18;x++)for(int z=-12;z<=14;z++){
                var pos=ORIGIN.offset(x,-1,z);level.setBlock(pos,Blocks.STONE_BRICKS.defaultBlockState(),2);
                for(int y=1;y<=6;y++)level.setBlock(pos.above(y),Blocks.AIR.defaultBlockState(),2);
            }
            int i=0;
            for(var entry:ModItems.ITEMS.getEntries())if(entry.get() instanceof ClothingItem clothing){
                var stand=new ArmorStand(level,ORIGIN.getX()-6+(i%6)*2.4,ORIGIN.getY()+(i>=6?2.8:0),ORIGIN.getZ());
                stand.setNoGravity(true);stand.setInvulnerable(true);stand.setYRot(0);stand.setYBodyRot(0);stand.setItemSlot(clothing.getEquipmentSlot(),clothing.getDefaultInstance());level.addFreshEntity(stand);i++;
            }
            p.teleportTo(level,ORIGIN.getX(),ORIGIN.getY()+1.0,ORIGIN.getZ()+10,180,0);p.getAbilities().flying=true;p.onUpdateAbilities();phase=Boolean.getBoolean("coraline.referenceArmorOnly")?20:Boolean.getBoolean("coraline.effectsOnly")?16:1;
        });}
        if(phase==16&&ticks==40)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.setItemSlot(EquipmentSlot.MAINHAND,Weapons.ITEMS.get("thread_whip").get().getDefaultInstance());WhipAction.begin(p,null);
        });
        if(phase==16&&ticks==43)try(var shot=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){shot.writeToFile(java.nio.file.Path.of("../artwork/whip_lash_check.png"));}catch(Exception ex){throw new RuntimeException(ex);}
        if(phase<=0||++ticks<100)return;ticks=0;
        int captured=phase;phase=0;
        if(captured==3){
            int lit=net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level,mc.player.blockPosition().above());
            require((lit>>>4&15)>=ClothingConfig.STAR_LIGHT.get()-2,"Star shirt rendering hook is inactive");
            require(mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,mc.player.blockPosition().above())==0,"Visual shirt light altered actual world lighting");
        }
        if(captured==4){
            require((ClothingLight.apply(15<<20,ORIGIN)>>>4&15)>=ClothingConfig.NIGHT_LIGHT.get(),"Coat night light missing");
            require(ClothingLight.apply(0,ORIGIN)==0,"Coat incorrectly illuminates unlit caves");
        }
        if(captured==5)require(ClothingLight.apply(0,mc.player.blockPosition().above())==0,"Light lingered after clothing removal");
        if(captured==11||captured==12){
            net.minecraft.world.entity.Entity target=null;for(var entity:mc.level.entitiesForRendering())if(entity.getUUID().equals(outlineMob))target=entity;
            require(target!=null,"Outline test mob missing on client");
            require(mc.shouldEntityAppearGlowing(target)==(captured==11),"Button-eye outline did not follow head equipment");
            require(!target.isCurrentlyGlowing(),"Button eyes changed the entity's shared glowing state");
            if(captured==12)require(mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Goggles vision did not reach client");
        }
        if(captured==13)require(!mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Goggle vision survived removal");
        if(captured==27){com.coraline.config.CoralineConfig.PARENTS_SPAWN.set(previousParentSpawning);com.mojang.logging.LogUtils.getLogger().info("CORALINE_CLOTHING_VISUAL_AND_LIGHT_CHECK_OK");mc.stop();return;}
        try(var shot=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){shot.writeToFile(java.nio.file.Path.of("../artwork/clothing_check_"+captured+".png"));}catch(Exception ex){throw new RuntimeException(ex);}
        mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            if(captured==1)p.serverLevel().setDayTime(18000);
            if(captured==2)p.setItemSlot(EquipmentSlot.CHEST,ModItems.STAR_SHIRT.get().getDefaultInstance());
            if(captured==3)p.setItemSlot(EquipmentSlot.CHEST,ModItems.MIDNIGHT_COAT.get().getDefaultInstance());
            if(captured==4)p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
            if(captured==10){
                p.setItemSlot(EquipmentSlot.HEAD,ModItems.UNBINDING_BUTTON_EYES.get().getDefaultInstance());
                var mob=net.minecraft.world.entity.EntityType.PIG.create(p.serverLevel());mob.setNoAi(true);mob.setPos(ORIGIN.getX()+1,ORIGIN.getY(),ORIGIN.getZ()+2);outlineMob=mob.getUUID();p.serverLevel().addFreshEntity(mob);
            }
            if(captured==11){p.setItemSlot(EquipmentSlot.HEAD,ModItems.MIDNIGHT_GOGGLES.get().getDefaultInstance());p.serverLevel().setDayTime(18000);}
            if(captured==12)p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            if(captured==13||captured==14){
                var level=p.serverLevel();level.setDayTime(6000);
                for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(ORIGIN).inflate(25)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
                for(int i=0;i<10;i++){
                    var parent=(captured==13?com.coraline.registry.ModEntities.MOM:com.coraline.registry.ModEntities.DAD).get().create(level);
                    var tag=new net.minecraft.nbt.CompoundTag();parent.saveWithoutId(tag);tag.putInt("Skin",i);parent.load(tag);parent.setNoAi(true);parent.setInvulnerable(true);
                    parent.setPos(ORIGIN.getX()-6+(i%5)*3,ORIGIN.getY(),ORIGIN.getZ()-(i/5)*3);parent.setYRot(0);parent.setYBodyRot(0);level.addFreshEntity(parent);
                }
                p.teleportTo(level,ORIGIN.getX(),ORIGIN.getY()+.5,ORIGIN.getZ()+8,180,5);
            }
            if(captured==16){
                var level=mc.getSingleplayerServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);
                for(int x=-10;x<=10;x++)for(int z=-8;z<=8;z++){
                    var floor=ORIGIN.offset(x,-1,z);level.setBlock(floor,Blocks.GRASS_BLOCK.defaultBlockState(),2);
                    for(int y=0;y<5;y++)level.setBlock(floor.above(y+1),Blocks.AIR.defaultBlockState(),2);
                }
                var blocks=new net.minecraft.world.level.block.Block[]{Blocks.RED_WOOL,Blocks.ORANGE_WOOL,Blocks.YELLOW_WOOL,Blocks.LIME_WOOL,Blocks.CYAN_WOOL,Blocks.BLUE_WOOL,Blocks.PURPLE_WOOL};
                for(int x=0;x<blocks.length;x++)for(int y=0;y<3;y++)level.setBlock(ORIGIN.offset(-6+x*2,y,0),blocks[x].defaultBlockState(),2);
                var door=new com.coraline.otherworld.DoorData.Door();door.owner=p.getUUID();door.pos=ORIGIN;door.dimension=net.minecraft.world.level.Level.OVERWORLD.location().toString();door.exists=true;door.placed=true;
                com.coraline.otherworld.DoorData.get(p.server).doors.put(door.id,door);
                var realm=com.coraline.otherworld.RealmData.get(p.server).realm(door);realm.dx=realm.dz=0;realm.doorPos=ORIGIN;realm.radius=64;realm.ready=true;
                p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                p.teleportTo(level,ORIGIN.getX(),ORIGIN.getY()+.2,ORIGIN.getZ()+9,180,0);p.getAbilities().flying=true;p.onUpdateAbilities();
            }
            if(captured==17)mc.execute(()->com.coraline.config.OtherWorldVisualConfig.SATURATION.set(1.0));
            if(captured==18)mc.execute(()->com.coraline.config.OtherWorldVisualConfig.SATURATION.set(2.15));
            if(captured==19){
                var scene=new com.coraline.feast.VisitData.Scene();scene.owner=p.getUUID();scene.door=p.getPersistentData().getUUID("CoralineVisitDoor");scene.table=ORIGIN;scene.piano=ORIGIN.east(20);scene.stage=com.coraline.feast.VisitData.Stage.SUPPER;scene.elapsed=250;
                com.coraline.feast.VisitData.get(p.server).scenes.put(p.getUUID(),scene);
                var seat=com.coraline.registry.ModEntities.SCENE_PROP.get().create(p.serverLevel());seat.owner=p.getUUID();seat.kind(com.coraline.feast.ScenePropEntity.CHAIR);seat.slot=0;seat.setPos(p.position());p.serverLevel().addFreshEntity(seat);p.startRiding(seat,true);
                p.stopRiding();require(p.isPassenger(),"Fixture did not establish locked supper seat");
                com.coraline.feast.FirstVisit.recoverHome(p);
                require(!p.isPassenger()&&!com.coraline.otherworld.OtherWorld.isOther(p.level()),"Emergency exit did not release supper lock and return home");
                require(scene.stage==com.coraline.feast.VisitData.Stage.SUPPER&&scene.elapsed==250,"Escape incorrectly completed or reset quest progress");
                require(!p.getPersistentData().hasUUID("CoralineVisitDoor")&&!p.getPersistentData().getBoolean("CoralineRecovering"),"Recovery left stale portal/override tags");
                com.coraline.feast.VisitData.get(p.server).scenes.remove(p.getUUID());seat.discard();
                org.slf4j.LoggerFactory.getLogger(ClothingVisualCheck.class).info("CORALINE_LOCKED_SEAT_ESCAPE_OK");
            }
            if(captured>=5&&captured<=9){
                var level=p.serverLevel();level.setDayTime(6000);
                for(var parent:level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new net.minecraft.world.phys.AABB(ORIGIN).inflate(30)))parent.discard();
                for(var stand:level.getEntitiesOfClass(ArmorStand.class,new net.minecraft.world.phys.AABB(ORIGIN).inflate(24)))stand.discard();
                for(int i=0;i<4;i++){
                    if((captured-5)*4+i>=ArmorSets.Set.values().length)break;
                    var set=ArmorSets.Set.values()[(captured-5)*4+i];
                    var stand=new ArmorStand(level,ORIGIN.getX()-4.5+i*3,ORIGIN.getY(),ORIGIN.getZ());
                    stand.setNoGravity(true);stand.setInvulnerable(true);stand.setYRot(0);stand.setYBodyRot(0);
                    for(var type:net.minecraft.world.item.ArmorItem.Type.values())stand.setItemSlot(type.getSlot(),ArmorSets.ITEMS.get(set).get(type).get().getDefaultInstance());
                    level.addFreshEntity(stand);
                }
                p.teleportTo(level,ORIGIN.getX(),ORIGIN.getY()+.2,ORIGIN.getZ()+9,180,0);
            }
            if(captured>=20&&captured<=25){
                var level=p.serverLevel();level.setDayTime(6000);
                for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(ORIGIN).inflate(30)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
                for(int i=0;i<3;i++){
                    var stand=new ArmorStand(level,ORIGIN.getX()+(i-1)*1.4,ORIGIN.getY(),ORIGIN.getZ());
                    stand.setNoGravity(true);stand.setInvulnerable(true);stand.setShowArms(true);
                    stand.setYRot(i==1?155:0);stand.setYBodyRot(i==1?155:0);
                    var referenceSets=new ArmorSets.Set[]{ArmorSets.Set.DREAMWEAVE,ArmorSets.Set.COMFY_SILK,ArmorSets.Set.BUTTONGUARD,ArmorSets.Set.GARDEN_VINES,ArmorSets.Set.MIDNIGHT,ArmorSets.Set.MINERS};
                    for(var type:net.minecraft.world.item.ArmorItem.Type.values())stand.setItemSlot(type.getSlot(),ArmorSets.ITEMS.get(referenceSets[captured-20]).get(type).get().getDefaultInstance());
                    if(i==2){
                        stand.setLeftArmPose(new net.minecraft.core.Rotations(-65,0,-15));stand.setRightArmPose(new net.minecraft.core.Rotations(30,0,10));
                        stand.setLeftLegPose(new net.minecraft.core.Rotations(25,0,-5));stand.setRightLegPose(new net.minecraft.core.Rotations(-25,0,5));
                    }
                    level.addFreshEntity(stand);
                }
                p.teleportTo(level,ORIGIN.getX(),ORIGIN.getY()+.1,ORIGIN.getZ()+3.4,180,10);
            }
            phase=captured+1;
        });
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        if(!Boolean.getBoolean("coraline.clothingVisualCheck")||!started)return;
        int i=0;
        for(var entry:ModItems.ITEMS.getEntries())if((phase<=5&&entry.get() instanceof ClothingItem)||(phase>=6&&entry.get() instanceof SetArmorItem armor&&armor.set.ordinal()>=(phase-6)*4&&armor.set.ordinal()<(phase-5)*4)){
            e.getGuiGraphics().renderItem(entry.get().getDefaultInstance(),10+(i%20)*22,10+(i/20)*22);i++;
        }
    }
    private static void require(boolean test,String message){if(!test)throw new IllegalStateException(message);}
}
