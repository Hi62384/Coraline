package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;

/** Changes only authored lamps in the allocated venue; never changes household lighting. */
public final class ShowStageLighting {
    public static void theatre(ServerPlayer p,VisitData.Scene s,int act,int tick){var c=TheatreSite.center(p,s);for(int x=-20;x<=20;x+=4){boolean lit=act>=0&&(act>=5||((x/4+tick/40)&3)!=0);lamp(p,c.offset(x,0,-15),lit);}}
    public static void circus(ServerPlayer p,VisitData.Scene s,int act,int tick){var c=MouseCircusSite.center(p,s);for(int i=0;i<24;i++){double a=i*Math.PI/12;boolean lit=act>=0&&(act==4||(i+tick/20)%4!=0);lamp(p,c.offset((int)(Math.cos(a)*20),0,(int)(Math.sin(a)*20)),lit);}}
    private static void lamp(ServerPlayer p,BlockPos at,boolean lit){var state=p.serverLevel().getBlockState(at);if(state.getBlock() instanceof RedstoneLampBlock&&state.getValue(RedstoneLampBlock.LIT)!=lit)p.serverLevel().setBlock(at,state.setValue(RedstoneLampBlock.LIT,lit),2);}
    private ShowStageLighting(){}
}
