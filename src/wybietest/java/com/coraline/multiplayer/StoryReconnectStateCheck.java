package com.coraline.multiplayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

/** Reconnect journal coverage. This harness runs without a game or server. */
public final class StoryReconnectStateCheck {
    private static int assertions;

    private static void require(boolean value,String reason){
        if(!value)throw new AssertionError(reason);
        assertions++;
    }

    private static CompoundTag session(UUID owner,UUID guest,String state){
        var row=new CompoundTag();
        row.putUUID("Id",UUID.randomUUID());
        row.putUUID("Owner",owner);
        row.putUUID("Door",UUID.randomUUID());
        row.putLong("Revision",3);
        row.putLong("LastDirectorAt",70);
        var members=new CompoundTag();
        var director=new CompoundTag();
        director.putString("State","ACTIVE");
        members.put(owner.toString(),director);
        if(guest!=null){
            var member=new CompoundTag();
            member.putString("State",state);
            var back=new CompoundTag();
            back.putString("Dimension","minecraft:overworld");
            back.putDouble("X",1);back.putDouble("Y",65);back.putDouble("Z",2);
            back.putInt("Mode",1);
            member.put("Return",back);
            members.put(guest.toString(),member);
        }
        row.put("Members",members);
        return row;
    }

    private static StorySessionData reload(CompoundTag... rows)throws Exception{
        return reloadVersion(2,rows);
    }

    private static StorySessionData reloadVersion(int version,CompoundTag... rows)throws Exception{
        var root=new CompoundTag();root.putInt("Version",version);
        var list=new ListTag();for(var row:rows)list.add(row);
        root.put("Sessions",list);
        var bytes=new ByteArrayOutputStream();
        NbtIo.writeCompressed(root,bytes);
        return StorySessionData.load(NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray())));
    }

    public static void main(String[] args)throws Exception{
        for(String state:List.of("ACTIVE","PENDING","SPECTATOR","RETURNING"))
            for(boolean archived:new boolean[]{false,true})
                for(boolean applied:new boolean[]{false,true}){
                    var owner=UUID.randomUUID();var guest=UUID.randomUUID();
                    var old=session(owner,guest,state);old.putBoolean("Archived",archived);
                    old.getCompound("Members").getCompound(guest.toString()).putBoolean("ReturnApplied",applied);
                    var current=archived?session(owner,null,""):old;
                    var journal=archived?reload(old,current):reload(old);
                    require(!journal.blocked(),"Valid session blocked after reload");
                    require(journal.guest(guest)&&journal.owner(guest).orElseThrow().equals(owner),"Guest lost its original owner");
                    require(journal.returning(guest)==(archived||state.equals("RETURNING")),"Reload changed return eligibility");
                    var snapshot=journal.snapshot(owner);
                    require(snapshot.getUUID("Id").equals(current.getUUID("Id")),"Archived session replaced the current session");
                    require(snapshot.getBoolean("Paused")&&snapshot.getLong("PausedAt")==70,"Offline clock was not paused at its original cursor");
                    var savedRows=journal.save(new CompoundTag()).getList("Sessions",10);
                    var savedMember=savedRows.getCompound(0).getCompound("Members").getCompound(guest.toString());
                    require(savedMember.getBoolean("ReturnApplied")==applied,"Partial return receipt changed on replay");
                    snapshot.remove("Members");
                    require(journal.snapshot(owner).contains("Members"),"Snapshot mutated live membership");
                }
        var owner=UUID.randomUUID();var guest=UUID.randomUUID();
        var row=session(owner,guest,"RETURNING");
        var back=row.getCompound("Members").getCompound(guest.toString()).getCompound("Return");
        require(!reloadVersion(1,row).blocked(),"Valid legacy reconnect journal was rejected");
        require(reloadVersion(3,row).blocked(),"Unknown session version granted reconnect access");
        back.remove("Mode");
        require(reload(row).blocked(),"Missing return mode silently became survival");
        back.putLong("Mode",1);
        require(reload(row).blocked(),"Wrongly typed return mode was accepted");
        back.putInt("Mode",4);
        require(reload(row).blocked(),"Out-of-range return mode was accepted");
        back.putInt("Mode",1);back.putDouble("X",Double.POSITIVE_INFINITY);
        require(reload(row).blocked(),"Non-finite return position was accepted");
        back.putDouble("X",1);back.putFloat("Yaw",Float.NaN);
        require(reload(row).blocked(),"Non-finite return rotation was accepted");
        back.remove("Yaw");
        require(reload(row,session(UUID.randomUUID(),guest,"RETURNING")).blocked(),"One guest joined two saved sessions");
        require(reload(row,session(owner,null,"")).blocked(),"Two active saves claimed one director");

        var member=row.getCompound("Members").getCompound(guest.toString());
        var progress=new CompoundTag();
        var arena=new CompoundTag();arena.putLong("Deadline",100);arena.putLong("InvulnerableUntil",110);
        progress.put("arena",arena);member.put("ActivityProgress",progress);
        var shift=UUID.randomUUID();
        StoryDirectorClock.applyMembers(row,shift,30);
        var resumed=reload(row).snapshot(owner);
        StoryDirectorClock.applyMembers(resumed,shift,30);
        var savedArena=resumed.getCompound("Members").getCompound(guest.toString()).getCompound("ActivityProgress").getCompound("arena");
        require(savedArena.getLong("Deadline")==130&&savedArena.getLong("InvulnerableUntil")==140,"Reload replay shifted the guest clock twice");
        StoryDirectorClock.applyMembers(resumed,UUID.randomUUID(),20);
        require(savedArena.getLong("Deadline")==150&&savedArena.getLong("InvulnerableUntil")==160,"A later reconnect could not shift its own clock");
        System.out.println("STORY_RECONNECT_NBT_OK checks="+assertions);
    }
}
