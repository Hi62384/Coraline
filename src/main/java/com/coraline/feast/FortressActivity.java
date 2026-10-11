package com.coraline.feast;

import static com.coraline.feast.ExpeditionActivities.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Five Nether-themed old-school droppers with shrinking lava targets. */
public final class FortressActivity {
    public record Gate(int y,int x,int z,int radius){}
    public static final int LEVELS=5;
    static final String[] LEVEL_NAMES={"Fortress Beams","Basalt Cavern","Gilded Furnace","Crimson Canopy","Royal Inferno"};
    public static List<Gate> generate(long seed){return generate(seed,0);}
    public static List<Gate> generate(long seed,int level){
        if(level<0||level>=LEVELS)throw new IllegalArgumentException("Invalid fortress level");
        var random=new Random(seed+104729L*level);var gates=new ArrayList<Gate>();
        int count=7+level*2;
        boolean swap=random.nextBoolean();int direction=random.nextBoolean()?1:-1;
        double cross=random.nextBoolean()?-3:3;int turn=43+random.nextInt(4);
        var heights=new int[count];for(int i=0;i<count;i++)heights[i]=-30-(int)Math.round(i*224.0/(count-1))+(i==0||i==count-1?0:random.nextInt(9)-4);
        // Derive the route from a normal-gravity, normal-air-acceleration sweep.
        // It crosses the shaft rather than preserving a shared straight corridor.
        double x=.5,z=-.5,y=-.5,vx=0,vz=0,vy=0;int next=0;
        for(int tick=0;tick<200&&next<count;tick++){
            double along=(tick<turn?-9:9)*direction;
            double side=cross+(level>=2&&tick>=65?-cross*2:0);
            double tx=swap?side:along,tz=swap?along:side;
            double ax=Math.max(-1,Math.min(1,(tx-x)*.7-vx*3)),az=Math.max(-1,Math.min(1,(tz-z)*.7-vz*3));
            double length=Math.max(1,Math.hypot(ax,az));vx+=ax/length*.0196;vz+=az/length*.0196;
            double old=y;x+=vx;z+=vz;y+=vy;vx*=.91;vz*=.91;vy=(vy-.08)*.98;
            while(next<count&&y<=heights[next]+1&&old>heights[next]+1){
                gates.add(new Gate(heights[next],(int)Math.floor(x),(int)Math.floor(z),3));next++;
            }
        }
        if(next!=count)throw new IllegalStateException("Incomplete dropper route");
        return List.copyOf(gates);
    }
    public static boolean inside(Gate g,double x,double z){return x>g.x-g.radius+.31&&x<g.x+g.radius+.69&&z>g.z-g.radius+.31&&z<g.z+g.radius+.69;}
    static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        // Solid outer shaft and decorative bands. All floor openings are built from the same validated data used by collision checks.
        for(int y=-280;y<=7;y++)for(int k=-18;k<=18;k++)for(int side:new int[]{-18,18}){put(l,c,side,y,k,y%12==0?Blocks.RED_NETHER_BRICKS:Blocks.NETHER_BRICKS);put(l,c,k,y,side,y%12==0?Blocks.RED_NETHER_BRICKS:Blocks.NETHER_BRICKS);}
        box(l,c,-8,-2,0,8,-1,11,Blocks.POLISHED_BLACKSTONE_BRICKS);box(l,c,-8,0,10,8,2,10,Blocks.NETHER_BRICK_FENCE);box(l,c,-8,0,0,-8,2,10,Blocks.NETHER_BRICK_FENCE);box(l,c,8,0,0,8,2,10,Blocks.NETHER_BRICK_FENCE);
        for(int x:new int[]{-6,6}){box(l,c,x,0,3,x,6,3,Blocks.CHISELED_POLISHED_BLACKSTONE);put(l,c,x,7,3,Blocks.SHROOMLIGHT);}
        sign(l,c.offset(0,2,9),Direction.NORTH,"FORTRESS DESCENT","Dodge every obstacle","Land in the lava","Lava = safe finish");
        buildGates(l,c,state(s).getLong("Seed"),0);
        box(l,c,-17,-281,-17,17,-280,17,Blocks.BLACKSTONE);buildPool(l,c,state(s).getLong("Seed"),0);
        box(l,c,19,-280,-10,38,-279,13,Blocks.POLISHED_BLACKSTONE_BRICKS);box(l,c,19,-278,-10,38,-267,13,Blocks.NETHER_BRICKS);box(l,c,20,-278,-9,37,-268,12,Blocks.AIR);
        box(l,c,21,-279,-8,36,-279,11,Blocks.GOLD_BLOCK);box(l,c,24,-279,-5,33,-279,9,Blocks.RED_NETHER_BRICKS);
        for(int x:new int[]{22,35})for(int z:new int[]{-6,9}){box(l,c,x,-278,z,x,-271,z,Blocks.GILDED_BLACKSTONE);put(l,c,x,-270,z,Blocks.SHROOMLIGHT);}
        box(l,c,24,-278,-7,32,-277,-6,Blocks.GOLD_BLOCK);put(l,c,25,-276,-6,Blocks.DIAMOND_BLOCK);put(l,c,31,-276,-6,Blocks.EMERALD_BLOCK);
        put(l,c,28,-278,-5,Blocks.GILDED_BLACKSTONE);
    }
    static void buildGates(ServerLevel l,BlockPos c,long seed,int level){
        for(var obstacle:FortressDropper.obstacles(seed,level))box(l,c,obstacle.x(),obstacle.y(),obstacle.z(),obstacle.X(),obstacle.Y(),obstacle.Z(),obstacle.material());
        // Wall-mounted furnace windows give each course a readable Nether silhouette.
        for(int y=-20;y>=-270;y-=24)for(int x:new int[]{-17,17})box(l,c,x,y,-12,x,y+7,-10,level%2==0?Blocks.SHROOMLIGHT:Blocks.ORANGE_STAINED_GLASS);
    }
    static void buildPool(ServerLevel l,BlockPos c,long seed,int level){
        box(l,c,-17,-279,-17,17,-279,17,Blocks.BLACKSTONE);
        var pool=FortressDropper.pool(seed,level);int r=pool.size()/2;
        box(l,c,pool.x()-r-1,-279,pool.z()-r-1,pool.x()+r+1,-279,pool.z()+r+1,Blocks.CHISELED_POLISHED_BLACKSTONE);
        box(l,c,pool.x()-r,-279,pool.z()-r,pool.x()+r,-279,pool.z()+r,Blocks.LAVA);
    }
    static void nextLevel(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var n=state(s);int oldLevel=n.getInt("FortressLevel");
        box(p.serverLevel(),c,-17,-277,-17,17,-3,17,Blocks.AIR);
        int level=oldLevel+1;n.putInt("FortressLevel",level);n.putInt("Passed",0);n.putInt("Phase",1);
        n.putBoolean("FortressBoarded",false);n.putLong("FortressResetUntil",OtherMotherLoop.now(p)+20);
        n.remove("PreviousX");n.remove("PreviousY");n.remove("PreviousZ");n.remove("Failure");n.remove("DropAt");
        buildGates(p.serverLevel(),c,n.getLong("Seed"),level);buildPool(p.serverLevel(),c,n.getLong("Seed"),level);
        ExpeditionNetwork.mode(p,0);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.south(4)));p.setOnGround(false);
        ActivityScenery.title(p,"LEVEL "+level+" CLEARED","Level "+(level+1)+" / 5 — "+FortressDropper.pool(n.getLong("Seed"),level).size()+"×"+FortressDropper.pool(n.getLong("Seed"),level).size()+" lava target.");
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP,SoundSource.PLAYERS,.6F,1.1F);OtherMotherLoop.changed(p);
    }
    static void begin(ServerPlayer p,VisitData.Scene s){var n=state(s);n.putBoolean("MayFly",p.getAbilities().mayfly);n.putBoolean("Flying",p.getAbilities().flying);n.putBoolean("AbilitiesStored",true);n.putLong("EffectsAt",OtherMotherLoop.now(p));
        for(var effect:new MobEffect[]{MobEffects.SLOW_FALLING,MobEffects.LEVITATION})if(p.hasEffect(effect))n.put(effect==MobEffects.SLOW_FALLING?"Slow":"Levitation",p.getEffect(effect).save(new CompoundTag()));
        if(p.hasEffect(MobEffects.FIRE_RESISTANCE))n.put("Fire",p.getEffect(MobEffects.FIRE_RESISTANCE).save(new CompoundTag()));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,2000,0,false,false));
        n.putInt("FortressLevel",0);n.putBoolean("FortressBoarded",true);ActivityScenery.title(p,"LEVEL 1 / 5","Dodge obstacles. Land in lava.");
    }
    static void restrict(ServerPlayer p){p.stopFallFlying();p.removeEffect(MobEffects.SLOW_FALLING);p.removeEffect(MobEffects.LEVITATION);if(p.getAbilities().mayfly||p.getAbilities().flying){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();}}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){var n=state(s);int phase=n.getInt("Phase");
        if(phase>=3){boolean landed=n.getBoolean("Landed");if(!landed&&p.getY()<c.getY()-3||landed&&(p.getY()<c.getY()-279||p.getX()<c.getX()+19||p.getX()>c.getX()+38||Math.abs(p.getZ()-c.getZ())>12))ActivityScenery.teleport(p,Vec3.atBottomCenterOf(exitPos(c,s).north(4)));if(phase==3)ensurePrize(p,s,c);return;}
        restrict(p);if(!p.hasEffect(MobEffects.FIRE_RESISTANCE)||p.getEffect(MobEffects.FIRE_RESISTANCE).getDuration()<40)p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,200,0,false,false));var local=p.position().subtract(Vec3.atLowerCornerOf(c));
        if(phase==1){
            if(!n.getBoolean("FortressBoarded")&&n.contains("FortressResetUntil")){
                boolean safe=Math.abs(local.x)<8&&local.z>=1&&local.z<=9&&local.y>=-.2&&local.y<=2;
                if(!safe){ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.south(4)));p.setOnGround(false);}
                else if(OtherMotherLoop.now(p)>=n.getLong("FortressResetUntil"))n.putBoolean("FortressBoarded",true);
                p.fallDistance=0;return;
            }
            if(local.y<-.5){
                n.putInt("Phase",2);n.putInt("Passed",0);n.putLong("DropAt",OtherMotherLoop.now(p));
                n.putDouble("PreviousY",0);n.putDouble("PreviousX",local.x);n.putDouble("PreviousZ",local.z);
                ExpeditionNetwork.mode(p,1);p.playNotifySound(SoundEvents.ELYTRA_FLYING,SoundSource.PLAYERS,.35F,.7F);
            }else{
                if(Math.abs(local.x)>9||local.z>12)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));
                if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Jump into the shaft • Dodge obstacles • Land in LAVA to clear the level"),true);
                return;
            }
        }
        // Movement packets may arrive in batches. A normal fall can therefore
        // advance more than four blocks between server observations.
        if(Math.abs(local.x)>17||Math.abs(local.z)>17||local.y>2){n.putString("Failure","You left the falling course.");fail(p,s,c);return;}
        if(OtherMotherLoop.now(p)-n.getLong("DropAt")>1600){n.putString("Failure","The descent timed out.");fail(p,s,c);return;}
        var gates=generate(n.getLong("Seed"),n.getInt("FortressLevel"));int passed=n.getInt("Passed");
        var previous=new Vec3(n.contains("PreviousX")?n.getDouble("PreviousX"):local.x,n.getDouble("PreviousY"),n.contains("PreviousZ")?n.getDouble("PreviousZ"):local.z);
        if(local.y-previous.y>2){n.putString("Failure","The descent was interrupted by upward movement.");fail(p,s,c);return;}
        if(FortressDropper.hit(FortressDropper.obstacles(n.getLong("Seed"),n.getInt("FortressLevel")),previous,local)){n.putString("Failure","You struck a beam or pillar. Dodge the solid obstacles.");fail(p,s,c);return;}
        int crossed=passed;while(crossed<gates.size()&&local.y<=gates.get(crossed).y()+1)crossed++;
        if(crossed>passed){passed=crossed;n.putInt("Passed",passed);p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.PLAYERS,.4F,.8F+passed*.1F);}
        // Evaluate the destination before solid collision: at full falling
        // speed a player can reach the pool floor in the same tick as the lava.
        if(p.onGround()&&local.y< -3&&local.y> -278&&!p.serverLevel().noCollision(p,p.getBoundingBox().move(0,-.08,0))){n.putString("Failure","You landed on an obstacle. The lava pool is the finish.");fail(p,s,c);return;}
        n.putDouble("PreviousY",local.y);n.putDouble("PreviousX",local.x);n.putDouble("PreviousZ",local.z);
        if(local.y<=-278){if(!FortressDropper.pool(n.getLong("Seed"),n.getInt("FortressLevel")).contains(local.x,local.z)){n.putString("Failure","You missed the lava target. The blackstone floor is not safe.");fail(p,s,c);return;}if(n.getInt("FortressLevel")<LEVELS-1){nextLevel(p,s,c);return;}n.putInt("Phase",3);n.putBoolean("Landed",true);restore(p,s);ExpeditionNetwork.mode(p,0);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.offset(28,-278,3)));ActivityScenery.title(p,"PERFECT LANDING","All five descents cleared. The vault is yours.");p.playNotifySound(SoundEvents.GENERIC_EXTINGUISH_FIRE,SoundSource.PLAYERS,.8F,.6F);OtherMotherLoop.changed(p);ensurePrize(p,s,c);return;}
        if(p.tickCount%10==0)p.displayClientMessage(Component.literal("Level "+(n.getInt("FortressLevel")+1)+" / 5 • "+passed+" / "+gates.size()+" depth markers • Aim for the small LAVA target"),true);
    }
    /** Count every crossed plane at its interpolated crossing position, not
     * the later packet position. Negative result identifies the blocked gate. */
    static int crossedGates(List<Gate> gates,int passed,Vec3 from,Vec3 to){
        if(to.y>=from.y)return passed;
        while(passed<gates.size()){
            var gate=gates.get(passed);double plane=gate.y+1.0;
            if(to.y>plane)break;
            if(from.y<plane)return -(passed+1);
            double t=(from.y-plane)/(from.y-to.y);
            var crossing=from.lerp(to,t);
            if(!inside(gate,crossing.x,crossing.z))return -(passed+1);
            passed++;
        }
        return passed;
    }
    static void fail(ServerPlayer p,VisitData.Scene s,BlockPos c){state(s).putInt("Phase",5);restore(p,s);ExpeditionNetwork.mode(p,0);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));ActivityScenery.title(p,"FORTRESS TRAP",state(s).getString("Failure").isEmpty()?"Dodge every obstacle, then land in lava.":state(s).getString("Failure"));p.playNotifySound(SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,.45F,.6F);OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);}
    static void restore(ServerPlayer p,VisitData.Scene s){var n=state(s);if(!n.getBoolean("AbilitiesStored"))return;n.putBoolean("AbilitiesStored",false);p.getAbilities().mayfly=n.getBoolean("MayFly");p.getAbilities().flying=n.getBoolean("Flying");p.onUpdateAbilities();
        p.removeEffect(MobEffects.FIRE_RESISTANCE);
        int elapsed=(int)Math.max(0,OtherMotherLoop.now(p)-n.getLong("EffectsAt"));for(String key:new String[]{"Slow","Levitation","Fire"})if(n.contains(key,10)){var e=MobEffectInstance.load(n.getCompound(key));n.remove(key);if(e!=null&&(e.isInfiniteDuration()||e.getDuration()>elapsed))p.addEffect(new MobEffectInstance(e.getEffect(),e.isInfiniteDuration()?-1:e.getDuration()-elapsed,e.getAmplifier(),e.isAmbient(),e.isVisible(),e.showIcon()));}
    }
    static void ensurePrize(ServerPlayer p,VisitData.Scene s,BlockPos c){var at=c.offset(28,-277,-5);if(LoopProps.nearby(p.serverLevel(),s,at).stream().noneMatch(e->e.slot==740)){var prop=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.PRIZE_PLATE,Vec3.atBottomCenterOf(at),740);prop.food(0,new ItemStack(Items.NETHERITE_INGOT));prop.setCustomName(Component.literal("Claim fortress jackpot"));prop.setCustomNameVisible(true);}}
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){if(prop.slot!=740)return false;if(!owns(p,s)||s.loop.activity!=OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS||state(s).getInt("Phase")!=3)return true;state(s).putInt("Phase",4);OtherMotherLoop.changed(p);prop.discard();DesertTempleActivity.jackpot(p,s);ActivityWorld.ensureExit(p,s);return true;}
    private FortressActivity(){}
}
