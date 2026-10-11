package com.coraline.client.clothing;

import com.coraline.clothing.UtilityItems;
import com.coraline.registry.ModBlocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Sparse palette checks find placed lanterns; never load chunks or scan the whole world. */
final class MoonlitSources {
    private static ClientLevel previous;
    private static final List<BlockPos> placed=new ArrayList<>();
    static List<Vec3> positions(ClientLevel level,Player viewer,int ticks){
        if(previous!=level||ticks%40==0){
            previous=level;placed.clear();int cx=viewer.getBlockX()>>4,cz=viewer.getBlockZ()>>4;
            for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++){
                if(!level.hasChunk(x,z))continue;
                var chunk=level.getChunk(x,z);var sections=chunk.getSections();
                for(int i=0;i<sections.length;i++){
                    var section=sections[i];int y0=level.getMinBuildHeight()+i*16;
                    if(Math.abs(y0-viewer.getY())>48||section.hasOnlyAir()||!section.getStates().maybeHas(s->s.is(ModBlocks.MOONLIT_LANTERN.get())))continue;
                    for(int a=0;a<16;a++)for(int b=0;b<16;b++)for(int d=0;d<16;d++){
                        var state=section.getBlockState(a,b,d);
                        if(state.is(ModBlocks.MOONLIT_LANTERN.get())&&!state.getValue(com.coraline.block.MoonlitLanternBlock.PORTABLE))placed.add(new BlockPos(x*16+a,y0+b,z*16+d));
                    }
                }
            }
        }
        var out=new ArrayList<Vec3>();
        for(var p:level.players())if(p.isAlive()&&p.distanceToSqr(viewer)<48*48&&(p.getMainHandItem().is(UtilityItems.MOONLIT_LANTERN.get())||(!com.coraline.client.MineHud.active()&&p.getOffhandItem().is(UtilityItems.MOONLIT_LANTERN.get()))))out.add(new Vec3(Math.round(p.getX()*16)/16.0,Math.round((p.getY()+1.1)*16)/16.0,Math.round(p.getZ()*16)/16.0));
        for(var p:placed)if(level.getBlockState(p).is(ModBlocks.MOONLIT_LANTERN.get()))out.add(Vec3.atCenterOf(p));
        return out.stream().sorted(Comparator.comparingDouble(p->p.distanceToSqr(viewer.position()))).limit(4).toList();
    }
}
