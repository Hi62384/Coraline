package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Authored room envelopes; never substitutes a whole-floor height test for house permissions. */
public final class PartyRegions {
    public record Room(String id,String label,int[] min,int[] max,int floor) {
        AABB bounds(BlockPos house){var a=HouseCabinets.at(house,min[0],min[1],min[2]);var b=HouseCabinets.at(house,max[0],max[1],max[2]);return new AABB(a.getX(),a.getY()-.05,a.getZ(),b.getX()+1,b.getY()+1,b.getZ()+1).inflate(.55,0,.55);}
        BlockPos center(BlockPos house){return HouseCabinets.at(house,(min[0]+max[0])/2,floor+1,(min[2]+max[2])/2);}
    }
    private static final List<Room> ROOMS=load();
    private static List<Room> load(){
        try(var in=Objects.requireNonNull(PartyRegions.class.getResourceAsStream("/assets/coraline/party_public_rooms.json"));var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
            var result=new ArrayList<Room>();
            for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("rooms")){
                var n=element.getAsJsonObject();int[] min=new int[3],max=new int[3];
                for(int i=0;i<3;i++){min[i]=n.getAsJsonArray("min").get(i).getAsInt();max[i]=n.getAsJsonArray("max").get(i).getAsInt();}
                result.add(new Room(n.get("id").getAsString(),n.get("label").getAsString(),min,max,n.get("floorBlockY").getAsInt()));
            }
            return List.copyOf(result);
        }catch(IOException e){throw new IllegalStateException("Cannot read authored party rooms",e);}
    }
    public static List<Room> rooms(){return ROOMS;}
    public static boolean allowed(ServerPlayer owner,Vec3 feet){
        if(PlayerParty.active(owner)!=null)return PlayerParty.allowed(owner,feet);
        var realm=SecretWorld.realm(owner);if(realm==null||realm.house==null||!OtherWorld.isOther(owner.level()))return false;
        var at=BlockPos.containing(feet);var secrets=SecretTransactions.state(owner);
        if(secrets==null||SecretDialogue.secretArea(secrets,at))return false;
        return ROOMS.stream().anyMatch(room->room.bounds(realm.house).contains(feet));
    }
    public static Optional<BlockPos> destination(ServerPlayer owner,Mob guest,int roomIndex,long seed){
        if(PlayerParty.active(owner)!=null)return PlayerParty.destination(owner,guest,roomIndex,seed);
        var realm=SecretWorld.realm(owner);if(realm==null||realm.house==null)return Optional.empty();
        var room=ROOMS.get(Math.floorMod(roomIndex,ROOMS.size()));var random=new Random(seed);
        for(int attempt=0;attempt<24;attempt++){
            int x=room.min[0]+random.nextInt(room.max[0]-room.min[0]+1),z=room.min[2]+random.nextInt(room.max[2]-room.min[2]+1);
            var at=HouseCabinets.at(realm.house,x,room.floor+1,z);var position=PartyFloor.at(owner,at);
            if(position==null||!allowed(owner,position)||!owner.serverLevel().noCollision(guest,guest.getDimensions(guest.getPose()).makeBoundingBox(position)))continue;
            if(owner.serverLevel().getEntities(guest,new AABB(position,position).inflate(.9,1,.9)).stream().anyMatch(e->e instanceof net.minecraft.world.entity.LivingEntity))continue;
            var path=guest.getNavigation().createPath(at,0);
            if(path!=null&&path.canReach())return Optional.of(at.immutable());
        }
        return Optional.empty();
    }
    /** Conservative population budget; runtime seating/navigation still constrains placement. */
    public static int capacity(ServerPlayer owner){
        var realm=SecretWorld.realm(owner);if(realm==null||realm.house==null)return 0;int free=0;
        for(var room:ROOMS)for(int x=room.min[0];x<=room.max[0];x+=2)for(int z=room.min[2];z<=room.max[2];z+=2){
            var at=HouseCabinets.at(realm.house,x,room.floor+1,z);
            if(allowed(owner,Vec3.atBottomCenterOf(at))&&PartyFloor.at(owner,at)!=null)free++;
        }
        return free<24?0:Math.max(6,Math.min(20,free/12));
    }
    private PartyRegions(){}
}
