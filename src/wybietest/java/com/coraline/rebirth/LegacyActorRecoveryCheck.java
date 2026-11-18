package com.coraline.rebirth;

import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import net.minecraft.nbt.*;

public final class LegacyActorRecoveryCheck {
    private static void require(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }

    private static void writeRegion(Path region,UUID body,UUID passenger)throws Exception{
        var chunk=new CompoundTag();
        chunk.putIntArray("Position",new int[]{-32,64});
        var entities=new ListTag();
        var entity=new CompoundTag();
        entity.putUUID("UUID",body);
        var passengers=new ListTag();
        var child=new CompoundTag();
        child.putUUID("UUID",passenger);
        passengers.add(child);
        entity.put("Passengers",passengers);
        entities.add(entity);
        chunk.put("Entities",entities);
        var bytes=new ByteArrayOutputStream();
        try(var output=new DataOutputStream(new DeflaterOutputStream(bytes))){NbtIo.write(chunk,output);}
        try(var file=new RandomAccessFile(region.toFile(),"rw")){
            file.setLength(12288);
            file.writeInt((2<<8)|1);
            file.seek(8192);
            file.writeInt(bytes.size()+1);
            file.writeByte(2);
            file.write(bytes.toByteArray());
        }
    }

    public static void main(String[] args)throws Exception{
        var original=UUID.randomUUID();
        var foreign=UUID.randomUUID();
        var root=Files.createTempDirectory("coraline-native-custody-");
        var directory=Files.createDirectories(root.resolve("dimensions/example/deep/realm/entities"));
        var region=directory.resolve("r.-1.2.mca");
        writeRegion(region,foreign,original);
        require(LegacyActorRecovery.inspect(root,original)==LegacyActorRecovery.Result.PRESENT,"Unloaded original passenger was overlooked");
        require(LegacyActorRecovery.inspect(root,foreign)==LegacyActorRecovery.Result.PRESENT,"Saved actor was overlooked");
        require(LegacyActorRecovery.inspect(root,UUID.randomUUID())==LegacyActorRecovery.Result.ABSENT,"Missing UUID failed verified scan");
        var parked=UUID.randomUUID();
        var vehicle=new CompoundTag();
        vehicle.putUUID("UUID",parked);
        var rootVehicle=new CompoundTag();
        rootVehicle.put("Entity",vehicle);
        var playerData=new CompoundTag();
        playerData.put("RootVehicle",rootVehicle);
        var file=Files.createDirectories(root.resolve("playerdata")).resolve(UUID.randomUUID()+".dat");
        NbtIo.writeCompressed(playerData,file.toFile());
        require(LegacyActorRecovery.inspect(root,parked)==LegacyActorRecovery.Result.PRESENT,"Disconnected player's native vehicle was overlooked");
        var reconnect=new LegacyActorRecovery.Observation(original);
        reconnect.joined(foreign);
        require(reconnect.decide(LegacyActorRecovery.Result.ABSENT)==LegacyActorRecovery.Result.ABSENT,"Unrelated reconnect blocked recovery");
        reconnect.joined(original);
        require(reconnect.decide(LegacyActorRecovery.Result.ABSENT)==LegacyActorRecovery.Result.PRESENT,"Rejoined original lost to an older disk result");
        try(var damaged=new RandomAccessFile(region.toFile(),"rw")){damaged.setLength(4096);}
        require(LegacyActorRecovery.inspect(root,original)==LegacyActorRecovery.Result.UNVERIFIED,"Damaged native data was treated as absence");
        System.out.println("LegacyActorRecoveryCheck passed");
    }
}
