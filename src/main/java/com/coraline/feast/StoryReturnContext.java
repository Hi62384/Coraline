package com.coraline.feast;

import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

final class StoryReturnContext {
    static boolean entryPhase(String phase){return "A".equals(phase)||"TUNNEL_EXIT".equals(phase);}

    static CompoundTag ticket(CompoundTag prior,UUID owner,UUID door,String phase,long corridor,Direction direction){
        if(!entryPhase(phase)||!direction.getAxis().isHorizontal())return null;
        if(!prior.isEmpty()&&(!matches(prior,owner,door,corridor,direction)||!prior.hasUUID("Session")))return null;
        var ticket=new CompoundTag();
        ticket.putUUID("Owner",owner);
        ticket.putUUID("Door",door);
        ticket.putUUID("Session",prior.hasUUID("Session")?prior.getUUID("Session"):UUID.randomUUID());
        ticket.putString("Phase",phase);
        ticket.putLong("Corridor",corridor);
        ticket.putString("Direction",direction.getName());
        return ticket;
    }

    private static boolean matches(CompoundTag ticket,UUID owner,UUID door,long corridor,Direction direction){
        return ticket.hasUUID("Owner")&&owner.equals(ticket.getUUID("Owner"))
                &&ticket.hasUUID("Door")&&door.equals(ticket.getUUID("Door"))
                &&ticket.contains("Corridor",Tag.TAG_LONG)&&ticket.getLong("Corridor")==corridor
                &&direction.getName().equals(ticket.getString("Direction"))&&entryPhase(ticket.getString("Phase"));
    }

    static boolean inCorridor(Vec3 offset,Direction direction){
        var forward=Vec3.atLowerCornerOf(direction.getNormal());
        double along=offset.dot(forward);
        return along>=0&&along<=23&&Math.abs(offset.y)<1
                &&offset.subtract(forward.scale(along)).horizontalDistanceSqr()<=.50;
    }

    private StoryReturnContext(){}
}
