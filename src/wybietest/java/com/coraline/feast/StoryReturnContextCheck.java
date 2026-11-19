package com.coraline.feast;

import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.phys.Vec3;

public final class StoryReturnContextCheck {
    private static void check(boolean yes,String explanation){if(!yes)throw new AssertionError(explanation);}
    public static void main(String[] args)throws Exception{
        var owner=UUID.randomUUID();
        var door=UUID.randomUUID();
        for(var direction:Direction.Plane.HORIZONTAL){
            var first=StoryReturnContext.ticket(new CompoundTag(),owner,door,"A",123,direction);
            check(first!=null,"Legacy return could not establish a checkpoint");
            var reload=TagParser.parseTag(first.toString());
            var recovered=StoryReturnContext.ticket(reload,owner,door,"TUNNEL_EXIT",123,direction);
            check(recovered!=null&&recovered.getUUID("Session").equals(first.getUUID("Session")),"Reload changed the story session");
            check(StoryReturnContext.ticket(reload,UUID.randomUUID(),door,"A",123,direction)==null,"Foreign actor reused the checkpoint");
            check(StoryReturnContext.ticket(reload,owner,door,"A",124,direction)==null,"Different corridor reused the checkpoint");
            check(StoryReturnContext.ticket(reload,owner,door,"B",123,direction)==null,"Completed crawl was repeated");
            var forward=Vec3.atLowerCornerOf(direction.getNormal());
            check(StoryReturnContext.inCorridor(forward.scale(20),direction),"Correct exit was rejected");
            check(!StoryReturnContext.inCorridor(forward.scale(30),direction),"Unrelated position was treated as the exit");
        }
        System.out.println("StoryReturnContextCheck passed");
    }
}
