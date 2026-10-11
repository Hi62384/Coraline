package com.coraline.secrets;

import java.util.*;
import com.coraline.feast.*;
import com.coraline.otherworld.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;

/** Metadata, not block scans, drives interactions and access protection. */
public final class SecretWorld {
    public static final String[] HIDING={"Writing desk","Closet shelf","Bedside chest","Dresser","Loose floorboard","Bookcase compartment"};
    private static final String[] NAMES={"Abandoned cottage","Garden pavilion","Old watchtower","Doll workshop","Hidden library","Clockwork storehouse","Thread observatory","Other mine","Illusion chamber","Soulglass laboratory","Soul prison","World anchor"};
    public static RealmData.Realm realm(ServerPlayer p){var scene=OtherMotherLoop.scene(p);return scene==null?null:RealmData.get(p.server).realms.get(scene.door);}
    public static BlockPos hiding(ServerPlayer p,int slot){var r=realm(p);return r==null||r.bed==null?null:r.bed.offset(new int[]{3,-3,2,-2,3,-3}[slot],slot==4?-1:0,new int[]{0,-2,2,2,-2,1}[slot]);}
    public static List<CompoundTag> rooms(SecretsState s){var out=new ArrayList<CompoundTag>();for(var tag:s.world.getList("Rooms",10))out.add((CompoundTag)tag);return out;}
    public static List<CompoundTag> nodes(SecretsState s){var out=new ArrayList<CompoundTag>();for(var tag:s.world.getList("Nodes",10))out.add((CompoundTag)tag);return out;}
    static void node(SecretsState s,BlockPos p,String kind,String id,int room){var list=s.world.getList("Nodes",10);var n=new CompoundTag();n.putLong("Pos",p.asLong());n.putString("Kind",kind);n.putString("Id",id);n.putInt("Room",room);list.add(n);s.world.put("Nodes",list);}
    private static final Set<UUID> QUEUING=new HashSet<>();
    public static int pending(SecretsState s){return Math.max(0,s.world.getList("BuildQueue",10).size()-s.world.getInt("BuildCursor"));}
    public static void drain(ServerPlayer p,int budget){var s=SecretTransactions.state(p);var queue=s.world.getList("BuildQueue",10);int cursor=s.world.getInt("BuildCursor");for(int n=0;n<budget&&cursor<queue.size();n++){var entry=queue.getCompound(cursor++);var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.tryParse(entry.getString("Block")));place(p,BlockPos.of(entry.getLong("Pos")),block);}if(cursor>=queue.size()){s.world.remove("BuildQueue");s.world.remove("BuildCursor");}else s.world.putInt("BuildCursor",cursor);}
    static void place(ServerPlayer p,BlockPos at,Block block){var s=SecretTransactions.state(p);
        if(QUEUING.contains(p.getUUID())){var queue=s.world.getList("BuildQueue",10);var entry=new CompoundTag();entry.putLong("Pos",at.asLong());entry.putString("Block",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString());queue.add(entry);s.world.put("BuildQueue",queue);return;}var originals=s.world.getCompound("Originals");String key=Long.toString(at.asLong());if(!originals.contains(key))originals.put(key,NbtUtils.writeBlockState(p.serverLevel().getBlockState(at)));s.world.put("Originals",originals);var placed=s.world.getCompound("Placed");placed.put(key,NbtUtils.writeBlockState(block.defaultBlockState()));s.world.put("Placed",placed);p.serverLevel().setBlock(at,block.defaultBlockState(),2|16);}
    public static void begin(ServerPlayer p){var s=SecretTransactions.state(p);var r=realm(p);if(s==null||r==null||r.bed==null||!r.ready)return;
        if(s.visit!=OtherMotherLoop.scene(p).loop.visits){
            // Wait while another player occupies generated content; never rebuild underneath helpers.
            for(var room:rooms(s)){var box=new AABB(BlockPos.of(room.getLong("Pos"))).inflate(9);if(p.serverLevel().players().stream().anyMatch(v->v!=p&&box.intersects(v.getBoundingBox())))return;}
            var old=s.world.getCompound("Originals");var placed=s.world.getCompound("Placed");for(String key:old.getAllKeys()){var at=BlockPos.of(Long.parseLong(key));var lookup=p.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK);if(placed.contains(key)&&p.serverLevel().getBlockState(at).equals(NbtUtils.readBlockState(lookup,placed.getCompound(key))))p.serverLevel().setBlock(at,NbtUtils.readBlockState(lookup,old.getCompound(key)),2|16);}
            if(s.world.hasUUID("JournalProp")){var oldProp=p.serverLevel().getEntity(s.world.getUUID("JournalProp"));if(oldProp!=null)oldProp.discard();}
            for(var entity:p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(r.house).inflate(r.radius),entity->entity.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(entity.getPersistentData().getUUID("SecretOwner"))))entity.discard();
            s.issued.entrySet().removeIf(entry->{for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var item=p.getInventory().getItem(slot);if(SecretTransactions.owned(p,item)&&entry.getKey().equals(item.getTag().getUUID("SecretToken")))return false;}return true;});
            s.world=new CompoundTag();s.claimed.clear();s.unlocked.clear();s.seed=p.getRandom().nextLong();s.visit=OtherMotherLoop.scene(p).loop.visits;s.search=SecretsState.Search.NORMAL;s.pendingTicks=0;
            // Only decorate already-empty bedroom cells, retaining the actual house furnishings.
            for(int i=0;i<6;i++){var at=hiding(p,i);if(p.serverLevel().isEmptyBlock(at))place(p,at,new Block[]{Blocks.LECTERN,Blocks.BOOKSHELF,Blocks.CHEST,Blocks.BARREL,Blocks.CHISELED_BOOKSHELF,Blocks.CHISELED_BOOKSHELF}[i]);node(s,at,"hiding",Integer.toString(i),-1);}
            var attic=com.coraline.otherworld.OtherWorldEstate.atticDoor(r.house);node(s,attic,"house_lock","attic",-1);node(s,attic.above(),"house_lock","attic",-1);var evidence=r.house.offset(15,16,7);if(p.serverLevel().isEmptyBlock(evidence))place(p,evidence,Blocks.CHISELED_BOOKSHELF);node(s,evidence,"secret","attic_plan",-1);
            var bell=r.porch.east(2);if(p.serverLevel().isEmptyBlock(bell)){place(p,bell.below(),Blocks.STONE_BRICKS);place(p,bell,Blocks.BELL);}node(s,bell,"bell","bell",-1);
            for(int i=0;i<3;i++){var drawer=r.bed.offset(-4+i*2,0,3);if(p.serverLevel().isEmptyBlock(drawer))place(p,drawer,Blocks.BARREL);node(s,drawer,"drawer",Integer.toString(i),-1);}
            s.world.putInt("KeyDrawer",new Random(s.seed).nextInt(3));
            if(!p.getInventory().contains(new ItemStack(SecretsItems.GUIDE.get())))SecretTransactions.give(p,new ItemStack(SecretsItems.GUIDE.get()));
            p.displayClientMessage(Component.literal("Search drawers and bookshelves. Bring evidence to your bedroom journal."),true);
            SecretTransactions.dirty(p);
        }
        drain(p,192);
        if(p.tickCount%20==0)WildOres.tick(p);
        if(p.tickCount%20==0&&pending(s)==0&&rooms(s).size()<40)buildNext(p);
        if(p.tickCount%40==0)journalProp(p);
    }
    private static void journalProp(ServerPlayer p){var s=SecretTransactions.state(p);var at=hiding(p,s.hiding);if(at==null)return;var tag=s.world;
        var existing=tag.hasUUID("JournalProp")?p.serverLevel().getEntity(tag.getUUID("JournalProp")):null;
        if(JournalPlacement.carrying(p))return;
        if(existing!=null){existing.setPos(at.getX()+.5,at.getY()+1.03,at.getZ()+.5);return;}
        var e=net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(p.serverLevel());if(e==null)return;var display=new CompoundTag();display.put("item",new ItemStack(SecretsItems.JOURNAL.get()).save(new CompoundTag()));display.putString("item_display","ground");e.load(display);e.setPos(at.getX()+.5,at.getY()+1.03,at.getZ()+.5);e.getPersistentData().putBoolean("CoralineDecoration",true);e.getPersistentData().putUUID("SecretOwner",p.getUUID());p.serverLevel().addFreshEntity(e);tag.putUUID("JournalProp",e.getUUID());
    }
    public static int distanceTier(double distance){return distance<110?0:distance<180?1:distance<250?2:3;}
    static boolean terrainCopied(RealmData.Realm r,BlockPos at){
        if(r.islandComplete)return true;
        if(r.preservedRadius>0&&r.sourceDoor!=null){double x=Math.abs(at.getX()-r.dx-r.sourceDoor.getX())+9,z=Math.abs(at.getZ()-r.dz-r.sourceDoor.getZ())+9;if(x*x+z*z<=r.preservedRadius*r.preservedRadius)return true;}
        for(int x=(at.getX()-r.dx-6)>>4;x<=(at.getX()-r.dx+6)>>4;x++)
            for(int z=(at.getZ()-r.dz-9)>>4;z<=(at.getZ()-r.dz+6)>>4;z++)
                if(!r.copiedChunks.contains(net.minecraft.world.level.ChunkPos.asLong(x,z)))return false;
        return true;
    }
    private static boolean natural(net.minecraft.world.level.block.state.BlockState state){return state.isAir()||state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)||state.is(net.minecraft.tags.BlockTags.DIRT)||state.is(net.minecraft.tags.BlockTags.LEAVES)||state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.FLOWERS)||state.is(net.minecraftforge.common.Tags.Blocks.ORES)||state.is(Blocks.SAND)||state.is(Blocks.RED_SAND)||state.is(Blocks.SANDSTONE)||state.is(Blocks.CLAY)||state.is(Blocks.SNOW_BLOCK)||state.is(Blocks.GRAVEL)||state.is(Blocks.GRASS)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.FERN)||state.is(Blocks.LARGE_FERN)||state.is(Blocks.SNOW);}
    /** Read earth, not the heightmap's tree trunk/canopy. Never flatten built blocks. */
    static int ground(net.minecraft.server.level.ServerLevel level,int x,int z){
        int top=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
        for(int y=top-1;y>=Math.max(level.getMinBuildHeight(),top-96);y--){
            var at=new BlockPos(x,y,z);var state=level.getBlockState(at);
            if(state.isAir()||state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.LEAVES)||state.getCollisionShape(level,at).isEmpty()&&state.getFluidState().isEmpty())continue;
            return natural(state)&&state.getFluidState().isEmpty()?y+1:Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }
    static BlockPos site(net.minecraft.server.level.ServerLevel level,BlockPos candidate){
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(int dx=-6;dx<=6;dx++)for(int dz=-9;dz<=6;dz++){
            int y=ground(level,candidate.getX()+dx,candidate.getZ()+dz);
            if(y==Integer.MIN_VALUE||y<level.getMinBuildHeight()+10||y>level.getMaxBuildHeight()-15)return null;
            low=Math.min(low,y);high=Math.max(high,y);if(high-low>6)return null;
        }
        var center=new BlockPos(candidate.getX(),(low+high)/2,candidate.getZ());
        for(int dx=-6;dx<=6;dx++)for(int dz=-9;dz<=6;dz++)for(int dy=-3;dy<=13;dy++)
            if(!natural(level.getBlockState(center.offset(dx,dy,dz))))return null;
        return center;
    }
    public static void buildNext(ServerPlayer p){var s=SecretTransactions.state(p);var r=realm(p);int index=rooms(s).size();var random=new Random(s.seed+index*733L+s.world.getInt("Attempts")*31L);BlockPos center=null;
        for(int attempt=0;attempt<24;attempt++){
            int[] counts=new int[4];for(var room:rooms(s))counts[room.getInt("Tier")]++;
            int desired=0;for(int t=1;t<4;t++)if(counts[t]<counts[desired])desired=t;
            double angle=random.nextDouble()*Math.PI*2;
            // Sample the wanted estate-distance band directly instead of rejecting almost
            // every point in a 560-wide square. Fall back if that tier has no usable land.
            int tier=attempt<12?desired:random.nextInt(4);if(counts[tier]>=10)continue;
            double distance=new int[]{54,110,180,250}[tier]+random.nextDouble()*new int[]{55,69,69,145}[tier];
            double vx=Math.cos(angle),vz=Math.sin(angle);
            int x=(int)Math.round(vx*distance)+(vx<0?-43:43),z=(int)Math.round(vz*distance)+(vz<0?-20:87);
            var candidate=r.house.offset(x,0,z);
            if(!terrainCopied(r,candidate))continue;
            double rx=candidate.getX()-r.sourceDoor.getX()-r.dx,rz=candidate.getZ()-r.sourceDoor.getZ()-r.dz;
            if(rx*rx+rz*rz>Math.pow(r.radius-14,2))continue;
            final BlockPos probe=candidate;if(rooms(s).stream().anyMatch(n->{var at=BlockPos.of(n.getLong("Pos"));double dx=at.getX()-probe.getX(),dz=at.getZ()-probe.getZ();return dx*dx+dz*dz<64*64;}))continue;
            center=site(p.serverLevel(),candidate);if(center!=null)break;
        }
        if(center==null){s.world.putInt("Attempts",s.world.getInt("Attempts")+1);return;}
        s.world.putInt("Attempts",0);
        int tier=distanceTier(OtherWorldEstate.outsideDistance(r.house,center));final int selectedTier=tier;
        int style=tier*3+(int)(rooms(s).stream().filter(n->n.getInt("Tier")==selectedTier).count()%3);var room=new CompoundTag();room.putLong("Pos",center.asLong());room.putInt("Tier",tier);room.putString("Name",NAMES[style]+new String[]{" — ruined"," — abandoned"," — forsaken"," — haunted"}[(index/12)%4]);var list=s.world.getList("Rooms",10);list.add(room);s.world.put("Rooms",list);
        QUEUING.add(p.getUUID());try{
        Block wall=tier==0?Blocks.SPRUCE_PLANKS:tier==1?Blocks.BRICKS:tier==2?Blocks.DEEPSLATE_BRICKS:Blocks.POLISHED_BLACKSTONE_BRICKS;
        int roof=index%3==2?8:6;
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            for(int y=-3;y<0;y++)place(p,center.offset(x,y,z),Blocks.STONE_BRICKS);
            for(int y=0;y<=roof;y++){boolean edge=Math.abs(x)==5||Math.abs(z)==5;Block block=y==roof?Blocks.DARK_OAK_SLAB:edge?wall:Blocks.AIR;
                if(edge&&y>=2&&y<=3&&(Math.abs(x)==5?z%3==0:x%3==0))block=Blocks.PURPLE_STAINED_GLASS;
                if(x==0&&z==-5&&y<3)block=tier==0?Blocks.AIR:Blocks.IRON_BARS;
                place(p,center.offset(x,y,z),block);
            }
        }
        // Each module has a different furnished silhouette and distinct internal focal point.
        for(int side:new int[]{-4,4})for(int z=-3;z<=3;z+=3){place(p,center.offset(side,0,z),index%3==0?Blocks.BOOKSHELF:index%3==1?Blocks.BARREL:Blocks.CHISELED_DEEPSLATE);place(p,center.offset(side,1,z),index%3==2?Blocks.AMETHYST_CLUSTER:Blocks.LANTERN);}
        for(int z=-4;z<=3;z++)place(p,center.offset(0,0,z),Blocks.PURPLE_CARPET);
        place(p,center.offset(0,0,3),tier==3?Blocks.ENCHANTING_TABLE:Blocks.LECTERN);
        SecretScenery.decorate(p,center,style,roof);SecretScenery.exterior(p,center,index,roof);
        var lock=center.offset(0,1,-5);node(s,lock,"lock",Integer.toString(index),index);
        int n=0;for(var secret:SecretRegistry.ALL.values())if(secret.tier()==tier&&Math.floorMod(new ArrayList<>(SecretRegistry.ALL.keySet()).indexOf(secret.id())/6,3)==index%3){var at=center.offset(-3+(n%4)*2,0,1+(n/4)*2);place(p,at,Blocks.CHISELED_BOOKSHELF);node(s,at,"secret",secret.id(),index);n++;}
        var loot=center.offset(3,0,-2);place(p,loot,Blocks.BARREL);node(s,loot,"loot","loot"+index,index);
        var cache=center.offset(-3,0,-2);place(p,cache,Blocks.CHISELED_BOOKSHELF);node(s,cache,"cache","cache"+index,index);
        if(style==7||tier==3){for(int ore=0;ore<3;ore++){var at=center.offset(-2+ore*2,-1,2);place(p,at,new Block[]{Blocks.IRON_ORE,Blocks.DIAMOND_ORE,Blocks.AMETHYST_BLOCK}[ore]);node(s,at,"ore","ore"+index+"_"+ore,index);}}
        if(style==9)node(s,center.offset(0,0,3),"soulglass","soulglass",index);
        if(style==10){node(s,center.offset(0,0,3),"soul","soul",index);var captive=net.minecraft.world.entity.EntityType.ALLAY.create(p.serverLevel());if(captive!=null){captive.setNoAi(true);captive.setNoGravity(true);captive.setInvulnerable(true);captive.setPersistenceRequired();captive.getPersistentData().putBoolean("CoralineDecoration",true);captive.getPersistentData().putUUID("SecretOwner",p.getUUID());captive.setPos(center.getX()+.5,center.getY()+2,center.getZ()+3.5);p.serverLevel().addFreshEntity(captive);room.putUUID("SoulCaptive",captive.getUUID());s.world.putUUID("SoulCaptive",captive.getUUID());}}
        if(style==8){for(int z=2;z<=4;z++)for(int y=0;y<3;y++)place(p,center.offset(2,y,z),Blocks.BOOKSHELF);node(s,center.offset(2,1,3),"passage","illusion_passage",index);var hidden=center.offset(3,0,3);place(p,hidden,Blocks.BARREL);node(s,hidden,"loot","passage_loot",index);}
        if(tier>0){int traps=1+(int)Math.floor(s.counter(3)*3);for(int t=0;t<traps;t++){var trap=center.offset(t%2==0?2:-2,0,-3+t);place(p,trap,index%3==0?Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE:index%3==1?Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE:Blocks.STONE_PRESSURE_PLATE);node(s,trap,"trap",Integer.toString(index%3),index);}place(p,center.offset(-3,0,3),Blocks.SPRUCE_PLANKS);SecretSpies.doll(p,center.offset(-3,1,3),-135F);}
        SecretScenery.ruin(p,center,index,roof,tier);
        // Walkable approach: terrain validation bounds the rise to three blocks.
        int approach=ground(p.serverLevel(),center.getX(),center.getZ()-9)-center.getY();
        for(int z=-8;z<-5;z++){int rise=(int)Math.round(approach*(-z-6)/3.0);for(int x=-1;x<=1;x++){var step=center.offset(x,rise,z);place(p,step.below(),Blocks.STONE_BRICKS);place(p,step,Blocks.AIR);place(p,step.above(),Blocks.AIR);}}
        }finally{QUEUING.remove(p.getUUID());}
        SecretTransactions.dirty(p);
    }
    public static int roomAt(SecretsState s,BlockPos at){var rooms=rooms(s);for(int i=0;i<rooms.size();i++){var c=BlockPos.of(rooms.get(i).getLong("Pos"));if(Math.abs(at.getX()-c.getX())<=5&&Math.abs(at.getZ()-c.getZ())<=5&&at.getY()>=c.getY()-1&&at.getY()<c.getY()+10)return i;}return -1;}
    public static boolean interact(ServerPlayer p,BlockPos at){if(!SecretsRuntime.active(p))return false;var s=SecretTransactions.state(p);for(var n:nodes(s))if(BlockPos.of(n.getLong("Pos")).equals(at)||(n.getString("Kind").equals("lock")&&BlockPos.of(n.getLong("Pos")).distSqr(at)<=1)){String kind=n.getString("Kind"),id=n.getString("Id");int room=n.getInt("Room");
            if(kind.equals("house_lock")){if(p.getMainHandItem().is(SecretsItems.KEY.get())&&SecretTransactions.owned(p,p.getMainHandItem())){var state=p.serverLevel().getBlockState(at);if(state.hasProperty(DoorBlock.OPEN)){boolean open=!state.getValue(DoorBlock.OPEN);p.serverLevel().setBlock(at,state.setValue(DoorBlock.OPEN,open),3);s.world.putBoolean("AtticOpen",open);}}else p.displayClientMessage(Component.literal("A borrowed house key fits this lock."),true);return true;}
            if(kind.equals("bell")){MotherSearch.bell(p);return true;}
            if(kind.equals("hiding")){int hiding=Integer.parseInt(id);if(JournalPlacement.place(p,hiding)){journalProp(p);return true;}if(s.hiding==hiding){if(p.isShiftKeyDown())JournalPlacement.begin(p);else SecretsNetwork.open(p,false,true);}else p.displayClientMessage(Component.literal("Move your journal here using its Move journal button."),true);return true;}
            if(kind.equals("drawer")){int drawer=Integer.parseInt(id);if(drawer==s.world.getInt("KeyDrawer")){if(s.world.getBoolean("KeyMissing")){if(p.getMainHandItem().is(SecretsItems.KEY.get())&&SecretTransactions.owned(p,p.getMainHandItem())){p.getMainHandItem().shrink(1);s.world.putBoolean("KeyMissing",false);}}else {s.world.putBoolean("KeyMissing",true);MotherSearch.observed(p,"personal_keyring",at);SecretTransactions.give(p,SecretTransactions.bound(p,SecretsItems.KEY.get(),UUID.randomUUID()));}}else SecretTransactions.issue(p,drawer==0?"key_drawer":"house_codes");SecretTransactions.dirty(p);return true;}
            if(room>=0&&room==rooms(s).size()-1&&pending(s)>0){p.displayClientMessage(Component.literal("The threads of this building are still settling."),true);return true;}
            if(kind.equals("lock")){SecretLocks.open(p,room);return true;}
            if(room>=0&&rooms(s).get(room).getInt("Tier")>0&&!s.unlocked.contains(room))return true;
            if(kind.equals("passage")){if(s.ability("passages")||s.ability("reveal")){place(p,at.below(),Blocks.AIR);place(p,at,Blocks.AIR);p.playNotifySound(net.minecraft.sounds.SoundEvents.PISTON_CONTRACT,net.minecraft.sounds.SoundSource.BLOCKS,.5F,.7F);}return true;}
            if(kind.equals("secret")){SecretTransactions.issue(p,id);MotherSearch.observed(p,id,at);p.displayClientMessage(Component.literal("Evidence: "+SecretRegistry.ALL.get(id).name()+" — deposit it in your bedroom journal."),true);return true;}
            if(kind.equals("cache")&&s.value(1)<100)return true;
            if(kind.equals("loot")||kind.equals("cache")){if(s.claimed.add(id)){MotherSearch.observed(p,kind.equals("cache")?"hidden_vaults":"secret_storage",at);if(kind.equals("cache")&&p.getRandom().nextDouble()<s.counter(1)*.75)MotherSearch.noise(p,at,90,"cache");int tier=rooms(s).get(room).getInt("Tier")+(kind.equals("cache")?1:0);SecretTransactions.checkpoint(p);SecretTransactions.give(p,new ItemStack(tier>=3?Items.DIAMOND:tier>=1?Items.EMERALD:Items.IRON_INGOT,2+p.getRandom().nextInt(3+tier*2)));SecretTransactions.dirty(p);}return true;}
            if(kind.equals("soulglass")&&s.ability("soulglass")&&s.claimed.add(id)){SecretTransactions.checkpoint(p);SecretTransactions.give(p,new ItemStack(SecretsItems.SOULGLASS.get()));SecretTransactions.dirty(p);return true;}
            if(kind.equals("soul")&&s.ability("rescue")&&s.claimed.add(id)){MotherSearch.observed(p,"soul_prisons",at);s.rescued++;if(s.world.hasUUID("SoulCaptive")){var captive=p.serverLevel().getEntity(s.world.getUUID("SoulCaptive"));if(captive!=null)captive.discard();}p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,at.getX()+.5,at.getY()+2,at.getZ()+.5,35,.6,1,.6,.05);SecretTransactions.checkpoint(p);SecretTransactions.give(p,new ItemStack(Items.EXPERIENCE_BOTTLE,s.ability("liberation")?12:4));p.displayClientMessage(Component.literal("A captive soul is free. Her pursuit and binding power weaken slightly."),true);SecretTransactions.dirty(p);return true;}
            return true;
        }return false;
    }
    public static boolean mine(ServerPlayer p,BlockPos at){if(!SecretsRuntime.active(p)||!(p.getMainHandItem().getItem() instanceof PickaxeItem))return false;var s=SecretTransactions.state(p);for(var node:nodes(s))if(node.getString("Kind").equals("ore")&&BlockPos.of(node.getLong("Pos")).equals(at)){int room=node.getInt("Room");if(!s.unlocked.contains(room)||!s.claimed.add(node.getString("Id")))return false;int ore=Integer.parseInt(node.getString("Id").substring(node.getString("Id").length()-1));var item=ore==0?com.coraline.clothing.OtherMaterials.OTHER_IRON.get():ore==1?com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get():com.coraline.clothing.OtherMaterials.MOONSTONE.get();place(p,at,Blocks.STONE);SecretTransactions.checkpoint(p);SecretTransactions.give(p,new ItemStack(item));SecretTransactions.dirty(p);return true;}return false;}
    private SecretWorld(){}
}
