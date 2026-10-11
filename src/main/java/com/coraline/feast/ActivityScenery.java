package com.coraline.feast;

import com.coraline.registry.ModEntities;
import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;

/** Small construction helpers, using the existing owned props and safe block placement. */
final class ActivityScenery {
    static final String OWNER="CoralineNewActivityOwner";
    static void clearWeather(ServerPlayer p){
        p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0));
        p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,0));
    }
    static void put(ServerLevel l,BlockPos c,int x,int y,int z,Block b){ActivityBlocks.set(l,c.offset(x,y,z),ActivityWorld.persistentLeaves(b),2);}
    static void box(ServerLevel l,BlockPos c,int x0,int y0,int z0,int x1,int y1,int z1,Block b){
        for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++)put(l,c,x,y,z,b);
    }
    static void mark(Entity e,VisitData.Scene s){e.getPersistentData().putUUID(OWNER,s.owner);e.getPersistentData().putBoolean("CoralineDecoration",true);e.setInvulnerable(true);}
    static boolean owned(Entity e,VisitData.Scene s){return e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))||e instanceof ScenePropEntity p&&s.owner.equals(p.owner)&&s.door.equals(p.storyDoor);}
    static void clean(ServerLevel l,VisitData.Scene s,AABB bounds){for(var e:l.getEntitiesOfClass(Entity.class,bounds,e->owned(e,s)))e.discard();}
    static void erase(ServerLevel l,AABB b,BlockPos marker){
        if(l.isEmptyBlock(marker))return;
        // Top-down, suppressing drops and neighbor cascades. Only the activity's private footprint.
        for(int y=(int)b.maxY;y>=(int)b.minY;y--)for(int z=(int)b.minZ;z<=(int)b.maxZ;z++)for(int x=(int)b.minX;x<=(int)b.maxX;x++){
            var p=new BlockPos(x,y,z);if(!l.isEmptyBlock(p))ActivityBlocks.set(l,p,Blocks.AIR.defaultBlockState(),2);
        }
    }
    static void father(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos at){
        LoopProps.hold(l,at);
        if(!l.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(at).inflate(8),e->owned(e,s)).isEmpty())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var dad=ModEntities.DAD.get().create(l);if(dad==null)return;
        var family=ParentAccess.family(p.server,s.owner);dad.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));dad.setNoAi(true);mark(dad,s);
        // An activity host is scoped to its owner/visit, not the house parent's generation.
        dad.getPersistentData().putUUID("ActivityHostDoor",s.door);
        dad.getPersistentData().putInt("ActivityHostGeneration",s.loop.generation);
        dad.setPersistenceRequired();dad.setNoGravity(true);dad.setInvisible(false);
        dad.setPos(Vec3.atBottomCenterOf(at));dad.setYRot(90);dad.setYBodyRot(90);dad.setYHeadRot(90);l.addFreshEntity(dad);
    }
    static void title(ServerPlayer p,String main,String sub){p.connection.send(new ClientboundSetTitlesAnimationPacket(5,50,15));p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(main)));p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));}
    static void teleport(ServerPlayer p,Vec3 at){p.stopRiding();p.teleportTo(p.serverLevel(),at.x,at.y,at.z,p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);}
    static void lamp(ServerLevel l,BlockPos c,int x,int z){box(l,c,x,0,z,x,3,z,Blocks.DARK_OAK_FENCE);put(l,c,x,4,z,Blocks.SEA_LANTERN);}
    static void tree(ServerLevel l,BlockPos c,int x,int z){box(l,c,x,0,z,x,5,z,Blocks.CHERRY_LOG);for(int y=4;y<=6;y++)for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(dx*dx+dz*dz<12)put(l,c,x+dx,y,z+dz,Blocks.CHERRY_LEAVES);put(l,c,x,3,z+1,Blocks.SHROOMLIGHT);}
    static void fountain(ServerLevel l,BlockPos c,int x,int z){
        box(l,c,x-3,-1,z-3,x+3,-1,z+3,Blocks.QUARTZ_BRICKS);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)put(l,c,x+dx,0,z+dz,Math.abs(dx)==3||Math.abs(dz)==3?Blocks.SMOOTH_QUARTZ:Blocks.WATER);
        box(l,c,x,0,z,x,2,z,Blocks.QUARTZ_PILLAR);put(l,c,x,3,z,Blocks.SEA_LANTERN);
    }
    private ActivityScenery(){}
}
