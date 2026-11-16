package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.*;

/** Durable house key ledger and owner-scoped, server-validated cabinet inventories. */
public final class HouseCabinets {
    public record Room(String id,String name,int[][] doors,int[] min,int[] max){}
    public record Cabinet(String name,int x,int y,int z,String room,String secret){}
    public static final Room[] ROOMS={
        new Room("storage","upstairs storage room",new int[][]{{36,15,53}},new int[]{37,15,52},new int[]{50,22,67}),
        new Room("attic","attic",new int[][]{{9,24,65}},new int[]{10,24,12},new int[]{61,37,68}),
        new Room("basement","basement",new int[][]{{26,-1,12},{27,-1,12}},new int[]{13,-1,12},new int[]{51,4,68}),
        new Room("archive","basement archive",new int[][]{{31,-1,64}},new int[]{14,-1,55},new int[]{30,4,67})
    };
    public static final Cabinet[] CABINETS={
        new Cabinet("Hall cabinet",35,6,21,"","key_drawer"),
        new Cabinet("Kitchen cabinet",49,6,48,"","house_codes"),
        new Cabinet("Bedroom drawer",40,15,23,"","drawer_marks"),
        new Cabinet("Sitting room cabinet",28,6,49,"","personal_keyring"),
        new Cabinet("Storage cabinet",48,15,64,"storage","attic_plan"),
        new Cabinet("Attic cabinet",48,24,58,"attic","spying_dolls"),
        new Cabinet("Basement cabinet",48,-1,40,"basement","soulglass"),
        new Cabinet("Archive drawer",28,-1,60,"archive","soul_prisons"),
        new Cabinet("Landing cabinet",35,15,49,"","mother_routines")
    };
    private static final int[] SOURCES={0,4,5,6}; // storage -> attic -> basement -> archive; no cycles.
    private static CompoundTag ledger(ServerPlayer p){var s=SecretTransactions.state(p);if(!s.world.contains("HouseCabinets"))s.world.put("HouseCabinets",new CompoundTag());return s.world.getCompound("HouseCabinets");}
    public static BlockPos at(BlockPos house,int x,int y,int z){return house.offset(x-(x>19?2:0)-31,y-5,z-(z>47?2:0)-(z>62?2:0)-41);}
    public static BlockPos cabinetPos(ServerPlayer p,int id){var r=SecretWorld.realm(p);var c=CABINETS[id];return at(r.house,c.x,c.y,c.z);}
    public static boolean inRoom(ServerPlayer p,Room room){var r=SecretWorld.realm(p);return r!=null&&r.house!=null&&contains(r.house,room,p.blockPosition());}
    private static boolean cabinetArea(BlockPos house,int id,BlockPos pos){
        String room=CABINETS[id].room;
        for(var r:ROOMS)if(contains(house,r,pos)){
            if(room.isEmpty())return false;if(room.equals(r.id))return true;
        }
        return room.isEmpty();
    }
    public static boolean contains(BlockPos house,Room room,BlockPos pos){var a=at(house,room.min[0],room.min[1],room.min[2]);var b=at(house,room.max[0],room.max[1],room.max[2]);return pos.getX()>=a.getX()&&pos.getX()<=b.getX()&&pos.getZ()>=a.getZ()&&pos.getZ()<=b.getZ()&&pos.getY()>=a.getY()&&pos.getY()<=b.getY();}
    public static boolean forbidden(SecretsState s,BlockPos pos){if(!s.world.contains("HouseCabinets"))return false;var n=s.world.getCompound("HouseCabinets");if(!n.contains("House"))return false;var house=BlockPos.of(n.getLong("House"));for(var room:ROOMS)if(contains(house,room,pos))return true;return false;}
    public static void prepare(ServerPlayer p){
        var r=SecretWorld.realm(p);if(r==null||r.house==null)return;var n=ledger(p);
        // Upgrade existing houses too: a vanilla wooden door predicts opening
        // locally before our server-side key check can reject the interaction.
        if(n.getInt("DoorBlockVersion")!=1||n.getInt("DoorBlockVisit")!=SecretTransactions.state(p).visit||!n.contains("DoorBlockHouse")||n.getLong("DoorBlockHouse")!=r.house.asLong()){
            for(var room:ROOMS)for(var coordinate:room.doors){var lower=at(r.house,coordinate[0],coordinate[1],coordinate[2]);
                for(var pos:List.of(lower,lower.above())){var old=p.serverLevel().getBlockState(pos);
                    if(!(old.getBlock() instanceof DoorBlock)||old.is(com.coraline.registry.ModBlocks.LOCKED_HOUSE_DOOR.get()))continue;
                    var state=com.coraline.registry.ModBlocks.LOCKED_HOUSE_DOOR.get().defaultBlockState()
                        .setValue(DoorBlock.FACING,old.getValue(DoorBlock.FACING)).setValue(DoorBlock.HALF,old.getValue(DoorBlock.HALF))
                        .setValue(DoorBlock.HINGE,old.getValue(DoorBlock.HINGE)).setValue(DoorBlock.OPEN,old.getValue(DoorBlock.OPEN)).setValue(DoorBlock.POWERED,false);
                    // Both halves must change block type before shape updates:
                    // vanilla otherwise deletes the still-old opposite half.
                    SecretWorld.place(p,pos,com.coraline.registry.ModBlocks.LOCKED_HOUSE_DOOR.get());p.serverLevel().setBlock(pos,state,2|16);
                    SecretTransactions.state(p).world.getCompound("Placed").put(Long.toString(pos.asLong()),NbtUtils.writeBlockState(state));
                }
            }
            n.putInt("DoorBlockVersion",1);n.putInt("DoorBlockVisit",SecretTransactions.state(p).visit);n.putLong("DoorBlockHouse",r.house.asLong());
        }
        if(!n.getBoolean("Initialized")){
            var s=SecretTransactions.state(p);var nodes=new ListTag();
            for(var node:SecretWorld.nodes(s))if(!Set.of("drawer","house_lock").contains(node.getString("Kind")))nodes.add(node);
            s.world.put("Nodes",nodes);s.world.remove("KeyMissing");s.world.remove("AtticOpen");
            var cabinets=new ListTag();for(int i=0;i<CABINETS.length;i++){var c=new CompoundTag();c.putInt("Id",i);var items=new ListTag();
                items.add(new ItemStack(i%2==0?Items.COOKIE:Items.BREAD,2).save(new CompoundTag()));c.put("Items",items);c.putLong("CheckAt",OtherMotherLoop.now(p)+checkInterval(p));cabinets.add(c);}
            n.put("Cabinets",cabinets);var keys=new CompoundTag();
            for(int i=0;i<ROOMS.length;i++){var key=new CompoundTag();key.putInt("Source",SOURCES[i]);key.putInt("Epoch",0);key.putUUID("Token",UUID.randomUUID());keys.put(ROOMS[i].id,key);}
            n.put("Keys",keys);n.putBoolean("Initialized",true);
        }
        if(n.getInt("StorageVersion")==1&&n.getInt("BuiltVisit")==SecretTransactions.state(p).visit&&n.contains("House")&&n.getLong("House")==r.house.asLong())return;
        boolean existingSpies=n.getBoolean("SpiesBuilt")&&n.getInt("BuiltVisit")==SecretTransactions.state(p).visit;
        n.putInt("StorageVersion",1);
        n.putInt("BuiltVisit",SecretTransactions.state(p).visit);n.putLong("House",r.house.asLong());n.putBoolean("SpiesBuilt",false);
        for(var room:ROOMS)relock(p,room);
        for(int i=0;i<CABINETS.length;i++){
            var pos=cabinetPos(p,i);var state=p.serverLevel().getBlockState(pos);
            if((state.isAir()||state.getBlock() instanceof CarpetBlock||storage(state))&&cabinetArea(r.house,i,pos))SecretWorld.place(p,pos,storageBlock(i));
            else { // Use the nearest existing barrel/empty supported wall cell, never cut a wall.
                boolean found=false;for(int radius=1;radius<=3&&!found;radius++)for(int dx=-radius;dx<=radius&&!found;dx++)for(int dz=-radius;dz<=radius&&!found;dz++){
                    var candidate=pos.offset(dx,0,dz);var block=p.serverLevel().getBlockState(candidate);
                    if((block.isAir()||storage(block))&&cabinetArea(r.house,i,candidate)&&p.serverLevel().getBlockState(candidate.below()).isFaceSturdy(p.serverLevel(),candidate.below(),Direction.UP)){pos=candidate;SecretWorld.place(p,pos,storageBlock(i));found=true;}
                }
            }
            cabinet(p,i).putLong("Pos",pos.asLong());cabinet(p,i).putBoolean("Open",false);
            cabinet(p,i).remove("VisualHash");showContents(p,i,null);
            var barrel=p.serverLevel().getBlockState(pos);if(barrel.getBlock() instanceof HouseStorageBlock){
                var facing=state.is(Blocks.BARREL)?state.getValue(BarrelBlock.FACING):state.getBlock() instanceof HouseStorageBlock?state.getValue(HouseStorageBlock.FACING):Direction.WEST;
                if(!facing.getAxis().isHorizontal())facing=Direction.WEST;
                barrel=barrel.setValue(HouseStorageBlock.FACING,facing).setValue(HouseStorageBlock.OPEN,false);p.serverLevel().setBlock(pos,barrel,2);
                SecretTransactions.state(p).world.getCompound("Placed").put(Long.toString(pos.asLong()),NbtUtils.writeBlockState(barrel));
            }
        }
        // House watchers use the same dolls as the abandoned houses.
        if(!existingSpies){
            for(int id:new int[]{4,5,6,7}){var pos=BlockPos.of(cabinet(p,id).getLong("Pos"));SecretSpies.doll(p,pos.above(),180);}
            for(int id:new int[]{5,6}){var pos=BlockPos.of(cabinet(p,id).getLong("Pos"));SecretSpies.rat(p,OtherWorld.safePlayer(p.serverLevel(),pos.east(2)));}
            n.putBoolean("SpiesBuilt",true);
        }
        SecretTransactions.dirty(p);
    }
    private static CompoundTag cabinet(ServerPlayer p,int id){return ledger(p).getList("Cabinets",10).getCompound(id);}
    private static boolean storage(net.minecraft.world.level.block.state.BlockState state){return state.is(Blocks.BARREL)||state.getBlock() instanceof HouseStorageBlock;}
    public static Block storageBlock(int id){return (CABINETS[id].name.contains("drawer")?com.coraline.registry.ModBlocks.ROSEWOOD_DRAWER:com.coraline.registry.ModBlocks.ROSEWOOD_CABINET).get();}
    private static CompoundTag key(ServerPlayer p,String id){return ledger(p).getCompound("Keys").getCompound(id);}
    public static long checkInterval(ServerPlayer p){return Math.round((5-4*OtherMotherLoop.scene(p).loop.suspicion)*60*20);}
    static boolean validKey(ServerPlayer p,ItemStack item,String id){
        if(!item.is(SecretsItems.KEY.get())||!SecretTransactions.owned(p,item)||item.getTag()==null||!id.equals(item.getTag().getString("HouseKey")))return false;
        var k=key(p,id);return k.hasUUID("Token")&&k.getUUID("Token").equals(item.getTag().getUUID("SecretToken"))&&k.getInt("Epoch")==item.getTag().getInt("KeyEpoch");
    }
    /** Visible hands only; witnesses must not scan hidden inventory contents. */
    public static boolean forbiddenKey(ServerPlayer p,ItemStack item){return item.hasTag()&&item.getTag().contains("HouseKey")&&validKey(p,item,item.getTag().getString("HouseKey"));}
    public static boolean canReturn(ServerPlayer p,int cabinet,ItemStack item){
        if(!item.is(SecretsItems.KEY.get()))return true;
        String id=item.getTag()==null?"":item.getTag().getString("HouseKey");
        return validKey(p,item,id)&&key(p,id).getInt("Source")==cabinet&&item.getTag().getInt("KeySource")==cabinet;
    }
    private static ItemStack physicalKey(ServerPlayer p,Room room){var k=key(p,room.id);var item=SecretTransactions.bound(p,SecretsItems.KEY.get(),k.getUUID("Token"));item.getOrCreateTag().putString("HouseKey",room.id);item.getTag().putInt("KeySource",k.getInt("Source"));item.getTag().putInt("KeyEpoch",k.getInt("Epoch"));item.setHoverName(Component.literal("Key: "+room.name));return item;}
    private static SimpleContainer contents(ServerPlayer p,int id){
        var c=cabinet(p,id);var inventory=new SimpleContainer(27);int slot=0;
        for(var entry:c.getList("Items",10)){if(slot<27)inventory.setItem(slot++,ItemStack.of((CompoundTag)entry));}
        for(var room:ROOMS){var k=key(p,room.id);if(k.getInt("Source")==id&&!k.getBoolean("Missing")&&slot<27)inventory.setItem(slot++,physicalKey(p,room));}
        if(!c.getBoolean("SecretTaken")&&SecretRegistry.ALL.containsKey(CABINETS[id].secret)){
            var s=SecretTransactions.state(p);String secret=CABINETS[id].secret;
            boolean stillInCabinet=c.hasUUID("SecretToken")&&secret.equals(s.issued.get(c.getUUID("SecretToken")));
            if(!s.journal.contains(secret)&&(!s.issued.containsValue(secret)||stillInCabinet)&&slot<27){UUID token=c.hasUUID("SecretToken")?c.getUUID("SecretToken"):UUID.randomUUID();c.putUUID("SecretToken",token);s.issued.put(token,secret);inventory.setItem(slot,SecretTransactions.bound(p,SecretsItems.EVIDENCE.get(secret).get(),token));}
        }
        var root=ledger(p);inventory.addListener(changed->{if(SecretTransactions.state(p)!=null&&ledger(p)==root)saveContents(p,id,inventory);});return inventory;
    }
    private static void saveContents(ServerPlayer p,int id,SimpleContainer inventory){
        var c=cabinet(p,id);var items=new ListTag();var present=new HashSet<String>();boolean secretPresent=false;
        for(int i=0;i<inventory.getContainerSize();i++){var item=inventory.getItem(i);if(item.isEmpty())continue;
            if(item.is(SecretsItems.KEY.get())){var n=item.getTag();if(n!=null&&canReturn(p,id,item))present.add(n.getString("HouseKey"));continue;}
            if(c.hasUUID("SecretToken")&&SecretTransactions.owned(p,item)&&c.getUUID("SecretToken").equals(item.getTag().getUUID("SecretToken"))&&CABINETS[id].secret.equals(SecretTransactions.evidence(p,item))){secretPresent=true;continue;}
            items.add(item.save(new CompoundTag()));
        }
        c.put("Items",items);if(c.hasUUID("SecretToken"))c.putBoolean("SecretTaken",!secretPresent);
        for(var room:ROOMS){var k=key(p,room.id);if(k.getInt("Source")==id){boolean returned=k.getBoolean("Missing")&&present.contains(room.id);k.putBoolean("Missing",!present.contains(room.id));if(returned)relock(p,room);}}
        SecretTransactions.dirty(p);
        showContents(p,id,inventory);
    }
    public static boolean interact(ServerPlayer p,BlockPos pos){
        if(!OtherWorld.isOther(p.level()))return false;var r=SecretWorld.realm(p);if(r==null&&p.getPersistentData().hasUUID("CoralineVisitDoor"))r=RealmData.get(p.server).realms.get(p.getPersistentData().getUUID("CoralineVisitDoor"));if(r==null||r.house==null)return false;
        for(var room:ROOMS)for(var coordinate:room.doors){var door=at(r.house,coordinate[0],coordinate[1],coordinate[2]);if(!pos.equals(door)&&!pos.equals(door.above()))continue;
            if(!SecretsRuntime.active(p)){p.displayClientMessage(Component.literal("The "+room.name+" is locked."),true);return true;}
            prepare(p);var k=key(p,room.id);boolean partyDoor=PlayerParty.permittedDoor(p,room.id)||VisitEvents.showDoor(p,room.id);
            var state=p.serverLevel().getBlockState(door);
            if(state.getBlock() instanceof DoorBlock&&!state.getValue(DoorBlock.OPEN)&&!inRoom(p,room)&&!partyDoor){
                InteractionHand hand=validKey(p,p.getMainHandItem(),room.id)?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
                if(!validKey(p,p.getItemInHand(hand),room.id)){p.displayClientMessage(Component.literal("The door is locked. You need the key to the "+room.name+"."),true);return true;}
                if(!p.isUsingItem()){var pending=new CompoundTag();pending.putString("Room",room.id);pending.putLong("Door",door.asLong());pending.putString("Dimension",p.level().dimension().location().toString());pending.putUUID("Token",p.getItemInHand(hand).getTag().getUUID("SecretToken"));p.getPersistentData().put("HouseUnlock",pending);p.startUsingItem(hand);p.displayClientMessage(Component.literal("Hold right-click to turn the key..."),true);}return true;
            }
            if(state.getBlock() instanceof DoorBlock){boolean open=!state.getValue(DoorBlock.OPEN);if(!partyDoor)k.putBoolean("Unlocked",open);
                for(var linked:room.doors)AuthoritativeHouseDoors.setOpen(p.serverLevel(),at(r.house,linked[0],linked[1],linked[2]),open);
            }
            SecretTransactions.dirty(p);return true;
        }
        if(!SecretsRuntime.active(p))return false;prepare(p);
        for(int i=0;i<CABINETS.length;i++)if(cabinet(p,i).getLong("Pos")==pos.asLong()){
            var scene=OtherMotherLoop.scene(p);if(scene!=null&&MouseCircusVisit.owned(p,scene)&&MouseCircusSite.inAttic(p,scene)){p.displayClientMessage(Component.literal("The attic cabinets are locked during the Mouse Circus."),true);return true;}
            var c=cabinet(p,i);boolean open=!c.getBoolean("Open");c.putBoolean("Open",open);var state=p.serverLevel().getBlockState(pos);if(state.getBlock() instanceof HouseStorageBlock){state=state.setValue(HouseStorageBlock.OPEN,open);p.serverLevel().setBlock(pos,state,2);SecretTransactions.state(p).world.getCompound("Placed").put(Long.toString(pos.asLong()),NbtUtils.writeBlockState(state));}
            p.serverLevel().playSound(null,pos,open?net.minecraft.sounds.SoundEvents.BARREL_OPEN:net.minecraft.sounds.SoundEvents.BARREL_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1);
            final int id=i;
            if(open){
                if(p.containerMenu!=p.inventoryMenu)p.closeContainer();
                var sceneForLoot=OtherMotherLoop.scene(p);
                if(sceneForLoot!=null&&p.getUUID().equals(sceneForLoot.owner)&&!PlayerParty.protectedCabinet(p,id)) {
                    long lootSeed=SecretTransactions.state(p).seed^p.getUUID().getMostSignificantBits()^((long)id*0x9e3779b97f4a7c15L);
                    var result=CabinetTreasure.seed(p,c,CabinetTreasure.profile(id,sceneForLoot.loop.visits),lootSeed);
                    if(result==CabinetTreasure.Result.SEEDED||result==CabinetTreasure.Result.PREPARED)SecretTransactions.checkpoint(p);
                    if(result==CabinetTreasure.Result.PREPARED)p.displayClientMessage(Component.literal("Make some space in this cabinet to uncover the rest of its treasures."),true);
                    if(result==CabinetTreasure.Result.MISSING_TABLE||result==CabinetTreasure.Result.INVALID)p.displayClientMessage(Component.literal("The hidden compartment seems jammed. Try again later."),true);
                }
                var inventory=contents(p,id);showContents(p,id,inventory);p.openMenu(new SimpleMenuProvider((sync,inv,player)->new CabinetMenu(sync,inv,inventory,p,id),Component.literal(CABINETS[id].name)));
            }
            else {p.closeContainer();showContents(p,id,null);}SecretTransactions.dirty(p);return true;
        }
        return false;
    }
    private static boolean unlocking(ServerPlayer p,ItemStack key){
        var n=p.getPersistentData().getCompound("HouseUnlock");if(!n.hasUUID("Token")||!SecretsRuntime.active(p)||!p.isAlive()||!p.level().dimension().location().toString().equals(n.getString("Dimension"))||!validKey(p,key,n.getString("Room"))||!n.getUUID("Token").equals(key.getTag().getUUID("SecretToken")))return false;
        var at=BlockPos.of(n.getLong("Door"));if(p.distanceToSqr(at.getCenter())>9)return false;
        var hit=p.pick(4,0,false);if(!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)||!blockHit.getBlockPos().equals(at)&&!blockHit.getBlockPos().equals(at.above()))return false;
        var state=p.serverLevel().getBlockState(at);return state.getBlock() instanceof DoorBlock&&!state.getValue(DoorBlock.OPEN);
    }
    public static void keyTick(ServerPlayer p,ItemStack key,int remaining){if(!unlocking(p,key)){p.getPersistentData().remove("HouseUnlock");p.stopUsingItem();return;}p.getPersistentData().getCompound("HouseUnlock").putInt("Progress",Math.max(0,Math.min(48,49-remaining)));if(remaining%10==0)p.displayClientMessage(Component.literal("Turning key... "+((48-remaining)*100/48)+"%"),true);}
    public static void finishUnlock(ServerPlayer p,ItemStack item){
        if(!unlocking(p,item)||p.getPersistentData().getCompound("HouseUnlock").getInt("Progress")<48){p.getPersistentData().remove("HouseUnlock");return;}
        var n=p.getPersistentData().getCompound("HouseUnlock");var r=SecretWorld.realm(p);if(r!=null&&r.house!=null)for(var room:ROOMS)if(room.id.equals(n.getString("Room"))){key(p,room.id).putBoolean("Unlocked",true);for(var coordinate:room.doors)AuthoritativeHouseDoors.setOpen(p.serverLevel(),at(r.house,coordinate[0],coordinate[1],coordinate[2]),true);p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1);SecretTransactions.dirty(p);}
        p.getPersistentData().remove("HouseUnlock");
    }
    private static final class CabinetMenu extends ChestMenu {
        private final ServerPlayer owner;private final int cabinetId;private final CompoundTag root;
        CabinetMenu(int sync,net.minecraft.world.entity.player.Inventory player,SimpleContainer inventory,ServerPlayer owner,int id){
            super(MenuType.GENERIC_9x3,sync,player,inventory,3);
            this.owner=owner;this.cabinetId=id;this.root=ledger(owner);
            for(int i=0;i<27;i++){var old=slots.get(i);var slot=new Slot(inventory,i,old.x,old.y){@Override public boolean mayPlace(ItemStack item){return !PlayerParty.protectedCabinet(owner,id)&&canReturn(owner,id,item);}@Override public boolean mayPickup(Player player){return player==owner&&!PlayerParty.protectedCabinet(owner,id)&&super.mayPickup(player);}};slot.index=i;slots.set(i,slot);}
        }
        private boolean readOnly(){var scene=OtherMotherLoop.scene(owner);if(scene!=null&&MouseCircusVisit.owned(owner,scene)&&MouseCircusSite.inAttic(owner,scene)){broadcastFullState();return true;}if(!PlayerParty.protectedCabinet(owner,cabinetId))return false;PlayerParty.storageWarning(owner,BlockPos.of(cabinet(owner,cabinetId).getLong("Pos")));broadcastFullState();return true;}
        // Guard the entry point as well as Slot.mayPickup: includes quick craft, hotbar/offhand swap,
        // double-click pickup-all, throwing, and menus already open when the event activates.
        @Override public void clicked(int slot,int button,ClickType type,Player player){if(player!=owner||readOnly())return;super.clicked(slot,button,type,player);}
        @Override public ItemStack quickMoveStack(Player player,int slot){return player!=owner||readOnly()?ItemStack.EMPTY:super.quickMoveStack(player,slot);}
        @Override public boolean stillValid(Player player){return player==owner&&SecretsRuntime.active(owner)&&ledger(owner)==root&&cabinet(owner,cabinetId).getBoolean("Open")&&owner.distanceToSqr(BlockPos.of(cabinet(owner,cabinetId).getLong("Pos")).getCenter())<=64;}
    }
    private static void showContents(ServerPlayer p,int id,SimpleContainer inventory){
        var c=cabinet(p,id);if(!c.contains("Pos"))return;var pos=BlockPos.of(c.getLong("Pos"));
        int hash=c.getBoolean("Open")&&inventory!=null?inventory.createTag().hashCode():0;
        if(c.contains("VisualHash")&&c.getInt("VisualHash")==hash)return;c.putInt("VisualHash",hash);
        for(var entity:p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,new net.minecraft.world.phys.AABB(pos).inflate(2),e->e.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("SecretOwner"))&&e.getPersistentData().contains("CabinetContents")&&e.getPersistentData().getInt("CabinetContents")==id))entity.discard();
        var state=p.serverLevel().getBlockState(pos);if(hash==0||!(state.getBlock() instanceof HouseStorageBlock block))return;
        var facing=state.getValue(HouseStorageBlock.FACING);int shown=0;
        for(int slot=0;slot<inventory.getContainerSize()&&shown<6;slot++){
            var item=inventory.getItem(slot);if(item.isEmpty())continue;
            var display=net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(p.serverLevel());if(display==null)continue;
            var data=new CompoundTag();data.put("item",item.copyWithCount(1).save(new CompoundTag()));data.putString("item_display","fixed");
            var transform=new CompoundTag();transform.put("translation",vector(0,0,0));transform.put("scale",vector(.22F,.22F,.22F));transform.put("left_rotation",vector(0,0,0,1));transform.put("right_rotation",vector(0,0,0,1));data.put("transformation",transform);display.load(data);
            double side=(shown%3-1)*.26,front=block.drawer?.2+(shown/3)*.25:.32,height=block.drawer?.515:.25+(shown/3)*.375;
            var right=facing.getClockWise();display.setPos(pos.getX()+.5+right.getStepX()*side+facing.getStepX()*front,pos.getY()+height,pos.getZ()+.5+right.getStepZ()*side+facing.getStepZ()*front);
            display.setYRot(facing.toYRot());if(block.drawer)display.setXRot(90);
            SecretSpies.mark(p,display,"cabinet_contents");display.getPersistentData().putInt("CabinetContents",id);p.serverLevel().addFreshEntity(display);shown++;
        }
    }
    private static ListTag vector(float... values){var tag=new ListTag();for(float value:values)tag.add(FloatTag.valueOf(value));return tag;}
    private static boolean held(ServerPlayer p,String id){for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(validKey(p,p.getInventory().getItem(slot),id))return true;return validKey(p,p.containerMenu.getCarried(),id);}
    public static void relocate(ServerPlayer p,String id){
        var k=key(p,id);if(!k.hasUUID("Token"))return;int old=k.getInt("Source");
        // Public cabinets are always reachable, even when all room doors are closed.
        var publicSpots=new int[]{0,1,2,3,8};int next=publicSpots[Math.floorMod(k.getInt("Epoch")+Arrays.asList("storage","attic","basement","archive").indexOf(id),publicSpots.length)];if(next==old)next=publicSpots[(Arrays.binarySearch(publicSpots,next)+1)%publicSpots.length];
        k.putInt("Source",next);k.putInt("Epoch",k.getInt("Epoch")+1);k.putUUID("Token",UUID.randomUUID());k.putBoolean("Missing",false);k.remove("Pursue");
        for(var room:ROOMS)if(room.id.equals(id))relock(p,room);
        p.closeContainer();SecretTransactions.dirty(p);
    }
    public static void confiscated(ServerPlayer p,ItemStack item){var n=item.getTag();if(n!=null&&n.contains("HouseKey")&&validKey(p,item,n.getString("HouseKey")))relocate(p,n.getString("HouseKey"));}
    private static void relock(ServerPlayer p,Room room){
        var r=SecretWorld.realm(p);if(r==null||r.house==null)return;key(p,room.id).putBoolean("Unlocked",false);
        for(var coordinate:room.doors){var door=at(r.house,coordinate[0],coordinate[1],coordinate[2]);for(var pos:List.of(door,door.above())){var state=p.serverLevel().getBlockState(pos);if(state.getBlock() instanceof DoorBlock)p.serverLevel().setBlock(pos,state.setValue(DoorBlock.OPEN,false),3);}}
    }
    public static void tick(ServerPlayer p){
        if(!SecretsRuntime.active(p))return;prepare(p);if(p.tickCount%20!=0)return;var n=ledger(p);long now=OtherMotherLoop.now(p);var mother=MotherSearch.mother(p);
        for(int i=0;i<CABINETS.length;i++){
            var c=cabinet(p,i);var pos=BlockPos.of(c.getLong("Pos"));
            showContents(p,i,c.getBoolean("Open")?contents(p,i):null);
            if(c.getBoolean("Open")&&(!PlayerParty.protectedCabinet(p,i)||HouseParty.data(PlayerParty.owned(p)).getLong("StorageAttemptUntil")>HouseParty.data(PlayerParty.owned(p)).getLong("Clock"))&&p.distanceToSqr(pos.getCenter())<25&&mother!=null&&MotherSearch.visible(mother,p,30))scold(p,SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT);
            if(Reconciliation.active(p))return;
            if(MotherGoesOut.unavailable(p)){
                long remaining=c.contains("AwayCheckRemaining")?c.getLong("AwayCheckRemaining"):Math.max(1,c.getLong("CheckAt")-now);
                remaining=Math.min(checkInterval(p),remaining);c.putLong("AwayCheckRemaining",remaining);c.putLong("CheckAt",now+remaining);continue;
            }
            if(c.contains("AwayCheckRemaining")){c.putLong("CheckAt",now+c.getLong("AwayCheckRemaining"));c.remove("AwayCheckRemaining");}
            long interval=checkInterval(p);if(c.getLong("CheckAt")>now+interval)c.putLong("CheckAt",now+interval);if(now<c.getLong("CheckAt"))continue;c.putLong("CheckAt",now+interval);
            for(var room:ROOMS){var k=key(p,room.id);if(k.getInt("Source")!=i||!k.getBoolean("Missing")||k.getBoolean("Pursue"))continue;
                var loop=OtherMotherLoop.scene(p).loop;OtherMotherLoop.relationship(p,loop.trust,loop.suspicion+.06F);
                if((validKey(p,p.getMainHandItem(),room.id)||validKey(p,p.getOffhandItem(),room.id))&&mother!=null&&MotherSearch.visible(mother,p,30)){k.putBoolean("Pursue",true);n.putBoolean("KeyChase",true);n.putBoolean("KeyConfrontation",true);MotherSearch.startChase(p);p.displayClientMessage(Component.literal("Other Mother has noticed the missing key in your hand."),true);}
                else relocate(p,room.id);
            }
        }
        if(n.getBoolean("KeyChase")&&!MotherGoesOut.unavailable(p)){
            for(var room:ROOMS)if(key(p,room.id).getBoolean("Pursue")&&!held(p,room.id))relocate(p,room.id);
            if(mother!=null&&MotherSearch.touching(mother,p))scold(p,SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT);
            else if(!Reconciliation.active(p)){var s=SecretTransactions.state(p);s.search=SecretsState.Search.DIRECT_CHASE;s.target=p.blockPosition();}
        }
        if(SecretDialogue.secretArea(p,p.blockPosition())&&mother!=null&&MotherSearch.visible(mother,p,30))scold(p,SecretDialogue.Event.SECRET_AREA_CAUGHT);
    }
    private static void scold(ServerPlayer p,SecretDialogue.Event event){
        MotherSearch.forbiddenAction(p,event);
    }
    public static boolean keyConfrontation(ServerPlayer p){var s=SecretTransactions.state(p);return s!=null&&s.world.getCompound("HouseCabinets").getBoolean("KeyConfrontation");}
    public static void confronted(ServerPlayer p){var s=SecretTransactions.state(p);if(s!=null){var n=s.world.getCompound("HouseCabinets");n.remove("KeyChase");n.remove("KeyConfrontation");}}
    private HouseCabinets(){}
}
