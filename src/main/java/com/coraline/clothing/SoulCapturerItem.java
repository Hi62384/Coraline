package com.coraline.clothing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Captured souls belong to the item, not its original owner. All commands are server-validated. */
@Mod.EventBusSubscriber(modid="coraline")
public final class SoulCapturerItem extends Item {
    private static final String OWNER="CoralineSoulOwner",LIST="CoralineCaptured",CAPTURER="CoralineCapturerId";
    private static final int MAX_SOULS=10;
    private static final Map<UUID,View> VIEWS=new HashMap<>();
    private static final Map<UUID,Double> VERTICAL=new HashMap<>();
    private static final Map<UUID,Mark> MARKS=new HashMap<>();
    private record View(UUID mob,Vec3 body,UUID capturer,boolean control,boolean hadNoAi,boolean hadNoGravity,ServerLevel level,float yaw,float pitch){}
    private record Mark(UUID target,long until){}
    public SoulCapturerItem(){super(new Item.Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext context){return context.getPlayer()==null?InteractionResult.FAIL:use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){return use(player.level(),player,hand).getResult();}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){if(VIEWS.containsKey(server.getUUID()))exit(server);else ArtifactNetwork.openSouls(server,stack);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    static UUID id(ItemStack stack){
        var tag=stack.getOrCreateTag();if(!tag.hasUUID(CAPTURER))tag.putUUID(CAPTURER,UUID.randomUUID());return tag.getUUID(CAPTURER);
    }
    private static boolean same(ItemStack stack,UUID id){return stack.is(ArtifactItems.SOUL_CAPTURER.get())&&stack.hasTag()&&stack.getTag().hasUUID(CAPTURER)&&id.equals(stack.getTag().getUUID(CAPTURER));}
    private static ItemStack heldCapturer(Player player,UUID capturer){
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);if(same(stack,capturer))return stack;
        }return ItemStack.EMPTY;
    }
    private static ItemStack find(Player player){
        if(player.getOffhandItem().is(ArtifactItems.SOUL_CAPTURER.get()))return player.getOffhandItem();
        if(player.getMainHandItem().is(ArtifactItems.SOUL_CAPTURER.get()))return player.getMainHandItem();
        for(var stack:player.getInventory().items)if(stack.is(ArtifactItems.SOUL_CAPTURER.get()))return stack;
        return ItemStack.EMPTY;
    }
    private static ItemStack find(ServerLevel level,UUID capturer){
        for(ServerPlayer player:level.getServer().getPlayerList().getPlayers()){
            for(int i=0;i<player.getInventory().getContainerSize();i++){
                ItemStack stack=player.getInventory().getItem(i);if(same(stack,capturer))return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    private static ServerPlayer holder(ServerLevel level,UUID capturer){
        for(ServerPlayer player:level.getServer().getPlayerList().getPlayers())if(player.serverLevel()==level&&same(player.getOffhandItem(),capturer))return player;
        return null;
    }
    static CompoundTag record(ItemStack stack,UUID mobId){
        ListTag list=stack.getOrCreateTag().getList(LIST,Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)if(list.getCompound(i).hasUUID("Id")&&list.getCompound(i).getUUID("Id").equals(mobId))return list.getCompound(i);
        return null;
    }
    @SubscribeEvent public static void capture(LivingDeathEvent event){
        if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel)||!(event.getSource().getEntity() instanceof ServerPlayer player))return;
        if(mob.getMaxHealth()>=40||!player.getMainHandItem().is(UtilityItems.SEWING_NEEDLE.get())||!player.getOffhandItem().is(ArtifactItems.SOUL_CAPTURER.get())||mob.getPersistentData().hasUUID(CAPTURER))return;
        ItemStack capturer=player.getOffhandItem();ListTag list=capturer.getOrCreateTag().getList(LIST,Tag.TAG_COMPOUND);
        if(list.size()>=MAX_SOULS){player.sendSystemMessage(Component.literal("This Soul Capturer already holds ten souls."));return;}
        UUID capturerId=id(capturer);event.setCanceled(true);mob.setHealth(Math.max(2,mob.getMaxHealth()/2));mob.deathTime=0;
        CompoundTag data=mob.getPersistentData();data.putUUID(OWNER,player.getUUID());data.putUUID(CAPTURER,capturerId);
        data.putInt("CoralineSoulFollow",0);data.putInt("CoralineSoulTarget",0);data.putInt("CoralineSoulTask",0);
        data.putDouble("CoralineSoulHomeX",mob.getX());data.putDouble("CoralineSoulHomeY",mob.getY());data.putDouble("CoralineSoulHomeZ",mob.getZ());
        if(mob.getCustomName()!=null)data.putString("CoralineSoulOriginalName",Component.Serializer.toJson(mob.getCustomName()));
        data.putBoolean("CoralineSoulOriginalNoAi",mob.isNoAi());data.put("CoralineSoulOriginalHand",mob.getMainHandItem().save(new CompoundTag()));
        String name=mob.getName().getString();mob.setCustomName(Component.literal("◉ ◉ "+name));mob.setCustomNameVisible(true);mob.setPersistenceRequired();
        CompoundTag entry=new CompoundTag();entry.putUUID("Id",mob.getUUID());entry.putString("Name",name);entry.putInt("Target",0);entry.putInt("Follow",0);entry.putInt("Task",0);entry.putInt("Selected",0);
        list.add(entry);capturer.getOrCreateTag().put(LIST,list);mob.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        player.sendSystemMessage(Component.literal(name+" now serves this Soul Capturer."));
    }
    private static void releaseMob(Mob mob){
        CompoundTag data=mob.getPersistentData();
        if(data.contains("CoralineSoulOriginalName"))mob.setCustomName(Component.Serializer.fromJson(data.getString("CoralineSoulOriginalName")));else mob.setCustomName(null);
        mob.setCustomNameVisible(false);mob.setNoAi(data.getBoolean("CoralineSoulOriginalNoAi"));
        if(data.contains("CoralineSoulOriginalHand"))mob.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.of(data.getCompound("CoralineSoulOriginalHand")));
        for(String key:new String[]{OWNER,CAPTURER,"CoralineSoulFollow","CoralineSoulTarget","CoralineSoulTask","CoralineSoulHomeX","CoralineSoulHomeY","CoralineSoulHomeZ","CoralineSoulOriginalName","CoralineSoulOriginalNoAi","CoralineSoulOriginalHand","CoralineSoulLastTask"})data.remove(key);
        mob.setTarget(null);mob.getNavigation().stop();
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof Mob mob&&SoulReleaseData.get(level.getServer()).consume(mob.getUUID()))releaseMob(mob);
    }
    @SubscribeEvent public static void protectHolder(LivingAttackEvent event){
        if(!(event.getEntity().level() instanceof ServerLevel level))return;
        if(event.getSource().getEntity() instanceof Mob attacker&&attacker.getPersistentData().hasUUID(CAPTURER)){
            UUID capturer=attacker.getPersistentData().getUUID(CAPTURER);ServerPlayer holder=holder(level,capturer);
            if(event.getEntity()==holder||event.getEntity() instanceof Mob other&&other.getPersistentData().hasUUID(CAPTURER)&&capturer.equals(other.getPersistentData().getUUID(CAPTURER))){event.setCanceled(true);return;}
        }
        if(event.getEntity() instanceof ServerPlayer victim&&event.getSource().getEntity() instanceof LivingEntity attacker&&victim.getOffhandItem().is(ArtifactItems.SOUL_CAPTURER.get()))
            MARKS.put(id(victim.getOffhandItem()),new Mark(attacker.getUUID(),level.getGameTime()+200));
    }
    @SubscribeEvent public static void holderAttack(AttackEntityEvent event){
        if(event.getEntity() instanceof ServerPlayer player&&event.getTarget() instanceof LivingEntity target&&player.getOffhandItem().is(ArtifactItems.SOUL_CAPTURER.get()))
            MARKS.put(id(player.getOffhandItem()),new Mark(target.getUUID(),player.serverLevel().getGameTime()+200));
    }
    private static void applyMode(Mob mob,CompoundTag entry){
        if(mob==null)return;mob.getPersistentData().putInt("CoralineSoulTarget",entry.getInt("Target"));mob.getPersistentData().putInt("CoralineSoulFollow",entry.getInt("Follow"));mob.getPersistentData().putInt("CoralineSoulTask",entry.getInt("Task"));
    }
    private static void release(ServerPlayer player,ItemStack stack,ListTag list,int index){
        CompoundTag entry=list.getCompound(index);UUID mobId=entry.getUUID("Id");
        Entity entity=player.serverLevel().getEntity(mobId);
        if(entity instanceof Mob mob)releaseMob(mob);else SoulReleaseData.get(player.server).release(mobId);
        list.remove(index);stack.getOrCreateTag().put(LIST,list);
    }
    public static void action(ServerPlayer player,UUID mobId,int action){
        if(action==5){exit(player);return;}
        ItemStack stack=find(player);if(stack.isEmpty())return;
        UUID capturer=id(stack);ListTag list=stack.getOrCreateTag().getList(LIST,Tag.TAG_COMPOUND);
        if(action==10||action==11||action==12){
            if(action==12){for(int i=list.size()-1;i>=0;i--)release(player,stack,list,i);}
            else {
                String key=action==10?"Target":"Follow",global=action==10?"SoulGlobalTarget":"SoulGlobalFollow";
                int mode=(stack.getOrCreateTag().getInt(global)+1)%3;stack.getOrCreateTag().putInt(global,mode);
                for(int i=0;i<list.size();i++){
                    CompoundTag entry=list.getCompound(i);entry.putInt(key,mode);
                    Entity entity=player.serverLevel().getEntity(entry.getUUID("Id"));if(entity instanceof Mob mob)applyMode(mob,entry);
                }
                stack.getOrCreateTag().put(LIST,list);
            }
            ArtifactNetwork.openSouls(player,stack);return;
        }
        int index=-1;for(int i=0;i<list.size();i++)if(list.getCompound(i).hasUUID("Id")&&list.getCompound(i).getUUID("Id").equals(mobId)){index=i;break;}
        if(index<0)return;CompoundTag entry=list.getCompound(index);
        Entity entity=player.serverLevel().getEntity(mobId);Mob mob=entity instanceof Mob m?m:null;
        if(action==4){release(player,stack,list,index);ArtifactNetwork.openSouls(player,stack);return;}
        if(action==6){
            SoulInventory inventory=new SoulInventory(stack,entry);
            NetworkHooks.openScreen(player,new SimpleMenuProvider((window,items,owner)->new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1,window,items,inventory,1),Component.literal(entry.getString("Name")+"'s hotbar")));
            return;
        }
        if(action==0||action==1){
            if(mob==null||!mob.isAlive()){player.displayClientMessage(Component.literal("That soul is not loaded nearby."),true);return;}
            if(mob.distanceToSqr(player)>64*64){player.displayClientMessage(Component.literal("Move within 64 blocks of that soul to view or control it."),true);return;}
            DreamstoneItem.stop(player);exit(player);
            VIEWS.put(player.getUUID(),new View(mobId,player.position(),capturer,action==1,mob.isNoAi(),mob.isNoGravity(),player.serverLevel(),player.getYRot(),player.getXRot()));
            ArtifactNetwork.soulView(player,action==1?2:1,mob.getId());
            ArtifactNetwork.soulHotbar(player,entry);
            if(action==1)mob.setNoAi(true);
            return;
        }
        if(action==2)entry.putInt("Target",(entry.getInt("Target")+1)%3);
        else if(action==3)entry.putInt("Follow",(entry.getInt("Follow")+1)%3);
        else if(action==7)entry.putInt("Task",(entry.getInt("Task")+1)%5);
        else if(action>=20&&action<29)entry.putInt("Selected",action-20);
        else return;
        applyMode(mob,entry);stack.getOrCreateTag().put(LIST,list);
        if(mob!=null)syncHand(mob,entry);
        if(action>=20&&action<29){ArtifactNetwork.soulHotbar(player,entry);return;}
        ArtifactNetwork.openSouls(player,stack);
    }
    private static boolean flying(Mob mob){return mob instanceof net.minecraft.world.entity.FlyingMob||mob.getMoveControl() instanceof FlyingMoveControl;}
    public static void move(ServerPlayer player,float strafe,float forward,boolean jump,boolean crouch,float yaw,float pitch){
        View view=VIEWS.get(player.getUUID());if(view==null||!view.control)return;
        if(heldCapturer(player,view.capturer).isEmpty()){exit(player);return;}
        if(!Float.isFinite(strafe)||!Float.isFinite(forward)||!Float.isFinite(yaw)||!Float.isFinite(pitch))return;
        Entity entity=player.serverLevel().getEntity(view.mob);if(!(entity instanceof Mob mob)){exit(player);return;}
        mob.setYRot(yaw);mob.setXRot(pitch);mob.setYHeadRot(yaw);mob.setYBodyRot(yaw);
        Vec3 look=Vec3.directionFromRotation(0,yaw),side=new Vec3(look.z,0,-look.x);
        double speed=crouch?.10:.24;Vec3 motion=look.scale(net.minecraft.util.Mth.clamp(forward,-1,1)*speed).add(side.scale(net.minecraft.util.Mth.clamp(strafe,-1,1)*speed));
        if(motion.lengthSqr()>speed*speed)motion=motion.normalize().scale(speed);
        if(flying(mob)){
            motion=motion.add(0,(jump?speed:0)-(crouch?speed:0),0);mob.setNoGravity(true);
            mob.move(MoverType.SELF,motion);mob.setDeltaMovement(Vec3.ZERO);
        }else{
            // No-AI mobs do not run ordinary travel physics. Keep a separate velocity;
            // vanilla AI ticks can clear deltaMovement before the next input packet.
            double vertical=VERTICAL.getOrDefault(player.getUUID(),0.0);
            boolean grounded=mob.onGround()||!mob.level().noCollision(mob,mob.getBoundingBox().move(0,-.04,0));
            if(grounded&&vertical<=0)vertical=jump?.42:0;
            else if(mob.isInWater()&&jump)vertical=.18;
            mob.move(MoverType.SELF,new Vec3(motion.x,vertical,motion.z));
            VERTICAL.put(player.getUUID(),mob.verticalCollision?-.08:(vertical-.08)*.98);
            mob.setDeltaMovement(Vec3.ZERO);
        }
        mob.hurtMarked=true;
    }
    private static Mob controlled(ServerPlayer player){
        View view=VIEWS.get(player.getUUID());if(view==null||!view.control||heldCapturer(player,view.capturer).isEmpty())return null;
        Entity entity=player.serverLevel().getEntity(view.mob);return entity instanceof Mob mob&&mob.isAlive()?mob:null;
    }
    private static CompoundTag controlledRecord(ServerPlayer player,Mob mob){var view=VIEWS.get(player.getUUID());return view==null?null:record(heldCapturer(player,view.capturer),mob.getUUID());}
    public static void attack(ServerPlayer player,int entityId){
        Mob mob=controlled(player);if(mob==null)return;Entity entity=player.serverLevel().getEntity(entityId);
        if(!(entity instanceof LivingEntity target)||!target.isAlive()||target==player||target.distanceToSqr(mob)>16||!mob.hasLineOfSight(target))return;
        if(target instanceof Mob other&&other.getPersistentData().hasUUID(CAPTURER)&&other.getPersistentData().getUUID(CAPTURER).equals(mob.getPersistentData().getUUID(CAPTURER)))return;
        long now=player.serverLevel().getGameTime();if(now-mob.getPersistentData().getLong("CoralineSoulLastAttack")<10)return;
        mob.getPersistentData().putLong("CoralineSoulLastAttack",now);
        CompoundTag entry=controlledRecord(player,mob);if(entry==null)return;
        int slot=entry.getInt("Selected");ItemStack tool=SoulInventory.get(entry,slot);
        if(!tool.isEmpty()){
            double amount=1+tool.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum();
            target.hurt(mob.damageSources().mobAttack(mob),(float)Math.max(1,amount));
            if(tool.isDamageableItem()){tool.hurtAndBreak(1,mob,e->{});SoulInventory.set(entry,slot,tool);syncHand(mob,entry);ArtifactNetwork.soulHotbar(player,entry);}
        }else if(mob instanceof RangedAttackMob ranged&&target.distanceToSqr(mob)>9)ranged.performRangedAttack(target,1F);
        else if(mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null)mob.doHurtTarget(target);
        else target.hurt(mob.damageSources().mobAttack(mob),1F);
    }
    public static void ability(ServerPlayer player,int entityId){
        Mob mob=controlled(player);if(mob==null)return;
        long now=player.serverLevel().getGameTime();if(now-mob.getPersistentData().getLong("CoralineSoulLastAbility")<40)return;
        Entity entity=player.serverLevel().getEntity(entityId);
        LivingEntity target=entity instanceof LivingEntity living&&living!=player&&living.distanceToSqr(mob)<256&&mob.hasLineOfSight(living)?living:null;
        boolean used=false;
        if(mob instanceof Creeper){player.serverLevel().explode(mob,mob.getX(),mob.getY(),mob.getZ(),2F,Level.ExplosionInteraction.NONE);used=true;}
        else if(mob instanceof EnderMan){Vec3 previous=mob.position(),next=previous.add(mob.getLookAngle().scale(6));mob.teleportTo(next.x,next.y,next.z);if(!player.serverLevel().noCollision(mob))mob.teleportTo(previous.x,previous.y,previous.z);else used=true;}
        else if(mob instanceof Spider){BlockPos pos=mob.blockPosition().relative(mob.getDirection(),2);if(player.serverLevel().isEmptyBlock(pos)){player.serverLevel().setBlockAndUpdate(pos,Blocks.COBWEB.defaultBlockState());used=true;}}
        else if(mob instanceof Blaze&&target!=null){target.setSecondsOnFire(5);target.hurt(mob.damageSources().mobAttack(mob),4F);used=true;}
        else if(mob instanceof Witch&&target!=null){target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100),mob);used=true;}
        else if(mob instanceof RangedAttackMob ranged&&target!=null){ranged.performRangedAttack(target,1F);used=true;}
        if(used)mob.getPersistentData().putLong("CoralineSoulLastAbility",now);
    }
    public static void block(ServerPlayer player,BlockPos pos,Direction face,boolean place){
        Mob mob=controlled(player);if(mob==null||!player.serverLevel().hasChunkAt(pos)||mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>36)return;
        CompoundTag entry=controlledRecord(player,mob);if(entry==null)return;
        int slot=entry.getInt("Selected");ItemStack tool=SoulInventory.get(entry,slot);ServerLevel level=player.serverLevel();
        if(place){
            if(!(tool.getItem() instanceof BlockItem block))return;
            ItemStack original=player.getMainHandItem();player.setItemInHand(InteractionHand.MAIN_HAND,tool.copy());
            try{
                Vec3 hit=Vec3.atCenterOf(pos).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5);
                block.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(hit,face,pos,false)));
                SoulInventory.set(entry,slot,player.getMainHandItem().copy());
                ArtifactNetwork.soulHotbar(player,entry);
            }finally{player.setItemInHand(InteractionHand.MAIN_HAND,original);}
        }else{
            var state=level.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(level,pos)<0||!level.mayInteract(player,pos)||!canMine(tool,state))return;
            var event=new BlockEvent.BreakEvent(level,pos,state,player);if(MinecraftForge.EVENT_BUS.post(event))return;
            Block.dropResources(state,level,pos,level.getBlockEntity(pos),mob,tool);
            level.removeBlock(pos,false);level.levelEvent(2001,pos,Block.getId(state));
            if(tool.isDamageableItem()){tool.hurtAndBreak(1,mob,e->{});SoulInventory.set(entry,slot,tool);syncHand(mob,entry);ArtifactNetwork.soulHotbar(player,entry);}
        }
    }
    public static boolean canMine(ItemStack tool,net.minecraft.world.level.block.state.BlockState state){
        if(tool.isEmpty())return false;
        boolean appropriate=(tool.getItem() instanceof net.minecraft.world.item.ShovelItem&&state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL))
            ||(tool.getItem() instanceof net.minecraft.world.item.PickaxeItem&&state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE))
            ||(tool.getItem() instanceof net.minecraft.world.item.AxeItem&&state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE))
            ||(tool.getItem() instanceof net.minecraft.world.item.HoeItem&&state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE))
            ||(tool.getItem() instanceof net.minecraft.world.item.ShearsItem&&tool.isCorrectToolForDrops(state));
        return appropriate&&(!state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state));
    }
    private static void syncHand(Mob mob,CompoundTag entry){
        ItemStack selected=SoulInventory.get(entry,entry.getInt("Selected"));
        if(!ItemStack.matches(mob.getMainHandItem(),selected))mob.setItemSlot(EquipmentSlot.MAINHAND,selected.copy());
        mob.setDropChance(EquipmentSlot.MAINHAND,0);
    }
    public static void exit(ServerPlayer player){
        VERTICAL.remove(player.getUUID());
        View view=VIEWS.remove(player.getUUID());if(view==null)return;
        Entity entity=view.level.getEntity(view.mob);
        if(entity instanceof Mob mob&&view.control){mob.setNoAi(view.hadNoAi);mob.setNoGravity(view.hadNoGravity);}
        player.setCamera(player);player.teleportTo(view.level,view.body.x,view.body.y,view.body.z,view.yaw,view.pitch);ArtifactNetwork.soulView(player,0,-1);
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;
        View view=VIEWS.get(player.getUUID());if(view==null)return;
        Entity entity=player.serverLevel().getEntity(view.mob);
        if(player.serverLevel()!=view.level||!player.isAlive()||!(entity instanceof Mob mob)||!mob.isAlive()||mob.position().distanceToSqr(view.body)>64*64||heldCapturer(player,view.capturer).isEmpty()){exit(player);return;}
        if(player.position().distanceToSqr(view.body)>.01)player.connection.teleport(view.body.x,view.body.y,view.body.z,view.yaw,view.pitch);
        player.setYRot(view.yaw);player.setXRot(view.pitch);player.setYHeadRot(view.yaw);player.setDeltaMovement(Vec3.ZERO);
    }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)||level.getGameTime()%10!=0)return;
        for(Entity entity:level.getAllEntities())if(entity instanceof Mob mob&&mob.isAlive()&&mob.getPersistentData().hasUUID(CAPTURER)){
            CompoundTag data=mob.getPersistentData();UUID capturer=data.getUUID(CAPTURER);
            ItemStack stack=find(level,capturer);CompoundTag entry=stack.isEmpty()?null:record(stack,mob.getUUID());
            if(entry!=null){applyMode(mob,entry);syncHand(mob,entry);taskLoot(level,mob,entry,stack);}
            ServerPlayer holder=holder(level,capturer);
            if(VIEWS.values().stream().anyMatch(v->v.mob.equals(mob.getUUID())&&v.control))continue;
            LivingEntity target=target(level,mob,holder,capturer,data.getInt("CoralineSoulTarget"));mob.setTarget(target);
            if(target!=null){mob.getNavigation().moveTo(target,1.2);if(mob.getAttribute(Attributes.ATTACK_DAMAGE)==null&&mob.distanceToSqr(target)<3&&mob.tickCount%20==0)target.hurt(mob.damageSources().mobAttack(mob),1F);continue;}
            int follow=data.getInt("CoralineSoulFollow");
            if(follow==0&&holder!=null&&mob.distanceToSqr(holder)>9)mob.getNavigation().moveTo(holder,1.15);
            else if(follow==1&&mob.getNavigation().isDone())mob.getNavigation().moveTo(data.getDouble("CoralineSoulHomeX")+(mob.getRandom().nextDouble()-.5)*12,data.getDouble("CoralineSoulHomeY"),data.getDouble("CoralineSoulHomeZ")+(mob.getRandom().nextDouble()-.5)*12,1);
            else if(follow==2)mob.getNavigation().stop();
        }
    }
    private static LivingEntity target(ServerLevel level,Mob mob,ServerPlayer holder,UUID capturer,int mode){
        if(mode==0)return null;
        if(mode==2){Mark mark=MARKS.get(capturer);if(holder==null||mark==null||mark.until<level.getGameTime())return null;Entity entity=level.getEntity(mark.target);return entity instanceof LivingEntity target&&target.isAlive()&&target.distanceToSqr(mob)<256?target:null;}
        LivingEntity nearest=null;double distance=144;
        for(LivingEntity candidate:level.getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(12),other->other!=mob&&other.isAlive()&&other!=holder)){
            if(candidate instanceof Mob other&&other.getPersistentData().hasUUID(CAPTURER)&&capturer.equals(other.getPersistentData().getUUID(CAPTURER)))continue;
            double d=candidate.distanceToSqr(mob);if(d<distance&&mob.hasLineOfSight(candidate)){nearest=candidate;distance=d;}
        }
        return nearest;
    }
    private static void taskLoot(ServerLevel level,Mob mob,CompoundTag entry,ItemStack capturer){
        int task=entry.getInt("Task");if(task==0)return;
        CompoundTag data=mob.getPersistentData();long now=level.getGameTime(),last=data.getLong("CoralineSoulLastTask");
        if(last==0){data.putLong("CoralineSoulLastTask",now);return;}
        double attack=mob.getAttribute(Attributes.ATTACK_DAMAGE)==null?0:mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed=mob.getAttribute(Attributes.MOVEMENT_SPEED)==null?0:mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        int strength=net.minecraft.util.Mth.clamp((int)Math.round(mob.getMaxHealth()/20.0+attack/6.0+speed*2),1,5);
        if(now-last<1200/strength)return;data.putLong("CoralineSoulLastTask",now);
        Item[] pool=switch(task){
            case 1->new Item[]{Items.WHEAT_SEEDS,Items.APPLE,Items.CARROT,Items.OAK_LOG};
            case 2->new Item[]{Items.COBBLESTONE,Items.COAL,Items.RAW_IRON,Items.REDSTONE};
            case 3->new Item[]{Items.LEATHER,Items.BONE,Items.STRING,Items.COOKED_BEEF};
            default->new Item[]{Items.IRON_NUGGET,Items.FLINT,Items.ROTTEN_FLESH,Items.GOLD_NUGGET};
        };
        Item loot=pool[level.random.nextInt(Math.min(pool.length,2+(strength>2?1:0)+(strength>4?1:0)))];
        if(SoulInventory.insert(entry,new ItemStack(loot,1+strength/2)))capturer.getOrCreateTag().put(LIST,capturer.getOrCreateTag().getList(LIST,Tag.TAG_COMPOUND));
    }
    @SubscribeEvent public static void blockAttack(AttackEntityEvent event){if(event.getEntity() instanceof ServerPlayer player&&VIEWS.containsKey(player.getUUID()))event.setCanceled(true);}
    @SubscribeEvent public static void blockUse(PlayerInteractEvent.RightClickBlock event){if(event.getEntity() instanceof ServerPlayer player&&VIEWS.containsKey(player.getUUID()))event.setCanceled(true);}
    @SubscribeEvent public static void blockItem(PlayerInteractEvent.RightClickItem event){if(event.getEntity() instanceof ServerPlayer player&&VIEWS.containsKey(player.getUUID()))event.setCanceled(true);}
    @SubscribeEvent public static void blockEntity(PlayerInteractEvent.EntityInteract event){if(event.getEntity() instanceof ServerPlayer player&&VIEWS.containsKey(player.getUUID()))event.setCanceled(true);}
    @SubscribeEvent public static void blockDig(PlayerInteractEvent.LeftClickBlock event){if(event.getEntity() instanceof ServerPlayer player&&VIEWS.containsKey(player.getUUID()))event.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)exit(player);}
}
