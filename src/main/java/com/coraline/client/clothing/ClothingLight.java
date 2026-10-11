package com.coraline.client.clothing;

import com.coraline.clothing.*;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.coraline.clothing.ClothingItem.Style.*;

/** Cached coarse occlusion + analytical fractional light. Never rewrites world blocks. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ClothingLight {
    private record Key(long origin,int intensity){}
    private record Field(BlockPos origin,int intensity,Long2ByteOpenHashMap values,Set<Long> sections){}
    private record Emitter(Vec3 pos,Field field,int scale){}
    private record Snapshot(List<Emitter> emitters,int moon){}
    private static volatile Snapshot snapshot=new Snapshot(List.of(),0);
    private static final LinkedHashMap<Key,Field> fields=new LinkedHashMap<>(16,.75F,true);
    private static final Set<Long> pending=new LinkedHashSet<>();
    private static Object lastLevel;
    private static int ticks;
    private static List<Vec3> baked=List.of();
    public static long lastWorkNanos;public static int lastVisited,lastRebuilt;
    public static int apply(int packed,BlockPos pos){
        var s=snapshot;int sky=packed>>>20&15,block=packed&0xFFFF,boost=s.moon*16*sky/15;
        for(var emitter:s.emitters){
            var f=emitter.field;double distance=Math.abs(pos.getX()+.5-emitter.pos.x)+Math.abs(pos.getY()+.5-emitter.pos.y)+Math.abs(pos.getZ()+.5-emitter.pos.z);
            if(distance>=f.intensity)continue;
            int dx=pos.getX()-f.origin.getX(),dy=pos.getY()-f.origin.getY(),dz=pos.getZ()-f.origin.getZ();
            int x=Math.floorDiv(dx,2)*2,y=Math.floorDiv(dy,2)*2,z=Math.floorDiv(dz,2)*2;
            double value=0;
            for(int a=0;a<2;a++)for(int b=0;b<2;b++)for(int c=0;c<2;c++){
                double weight=(a==0?1-(dx-x)*.5:(dx-x)*.5)*(b==0?1-(dy-y)*.5:(dy-y)*.5)*(c==0?1-(dz-z)*.5:(dz-z)*.5);
                if(weight==0)continue;
                value+=weight*f.values.get(BlockPos.asLong(f.origin.getX()+x+a*2,f.origin.getY()+y+b*2,f.origin.getZ()+z+c*2));
            }
            double fraction=distance-pos.distManhattan(f.origin);
            boost=Math.max(boost,(int)Math.round(Math.max(0,Math.min(f.intensity,value-fraction))*emitter.scale));
        }
        return boost>block?(packed&0xFFFF0000)|boost:packed;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;long started=System.nanoTime();lastVisited=lastRebuilt=0;
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null||mc.player==null){snapshot=new Snapshot(List.of(),0);lastLevel=null;pending.clear();return;}
        if(lastLevel!=level){lastLevel=level;fields.clear();pending.clear();baked=List.of();ticks=0;}
        ticks++;
        int moon=ClothingEffects.wearing(mc.player,MIDNIGHT_COAT)&&ClothingEffects.night(level)?ClothingConfig.NIGHT_LIGHT.get():0;
        var positions=new ArrayList<Vec3>();
        level.players().stream().filter(p->p.isAlive()&&p.distanceToSqr(mc.player)<48*48&&ClothingEffects.wearing(p,STAR_SHIRT))
            .sorted(Comparator.comparingDouble(p->p.distanceToSqr(mc.player))).limit(8).forEach(p->positions.add(p.position().add(0,1,0)));
        int stars=positions.size();positions.addAll(MoonlitSources.positions(level,mc.player,ticks));
        var emitters=new ArrayList<Emitter>();boolean night=ClothingEffects.lanternNight(level);
        // Refresh occlusion occasionally, not every second or every sub-block move.
        if(ticks%100==0)fields.clear();
        for(int i=0;i<positions.size();i++){
            var source=positions.get(i);boolean lantern=i>=stars;int intensity=lantern?(night?30:3):ClothingConfig.STAR_LIGHT.get();
            if(intensity<=0)continue;var origin=BlockPos.containing(source);if(origin.getY()<level.getMinBuildHeight()||origin.getY()>=level.getMaxBuildHeight())continue;var key=new Key(origin.asLong(),intensity);var field=fields.get(key);
            if(field==null){
                var values=new Long2ByteOpenHashMap();var sections=new HashSet<Long>();var queue=new ArrayDeque<BlockPos>();
                values.put(origin.asLong(),(byte)intensity);queue.add(origin);
                while(!queue.isEmpty()){
                    var p=queue.removeFirst();lastVisited++;int value=values.get(p.asLong());
                    var chunk=level.getChunkAt(p);var section=chunk.getSection(chunk.getSectionIndex(p.getY()));
                    if(!section.hasOnlyAir())sections.add(SectionPos.of(p).asLong());
                    if(value<=2||(!p.equals(origin)&&level.getBlockState(p).isSolidRender(level,p)))continue;
                    for(var direction:Direction.values()){
                        var mid=p.relative(direction);var next=p.relative(direction,2);
                        if(next.getY()<level.getMinBuildHeight()||next.getY()>=level.getMaxBuildHeight()||!level.hasChunkAt(next)||values.get(next.asLong())>=value-2)continue;
                        // A one-block wall must not disappear between coarse samples.
                        if(level.getBlockState(mid).isSolidRender(level,mid))continue;
                        values.put(next.asLong(),(byte)(value-2));queue.add(next);
                    }
                }
                field=new Field(origin,intensity,values,Set.copyOf(sections));fields.put(key,field);
                while(fields.size()>16)fields.remove(fields.keySet().iterator().next());
            }
            emitters.add(new Emitter(source,field,lantern?8:16));
        }
        var previous=snapshot;snapshot=new Snapshot(List.copyOf(emitters),moon);
        boolean moved=positions.size()!=baked.size()||previous.emitters.size()!=emitters.size();
        if(!moved)for(int i=0;i<emitters.size();i++)if(previous.emitters.get(i).field.intensity!=emitters.get(i).field.intensity||previous.emitters.get(i).scale!=emitters.get(i).scale){moved=true;break;}
        if(!moved)for(int i=0;i<positions.size();i++)if(positions.get(i).distanceToSqr(baked.get(i))>=.75*.75){moved=true;break;}
        // Entities see each fractional update. Baked block meshes use a bounded
        // movement threshold and queue; empty sections never get rebuilt for light.
        if(moved||ticks%100==0||previous.moon!=moon){
            for(var e:previous.emitters)pending.addAll(e.field.sections);
            for(var e:emitters)pending.addAll(e.field.sections);
            baked=List.copyOf(positions);
        }
        if(previous.moon!=moon)mc.levelRenderer.allChanged();
        for(var iterator=pending.iterator();iterator.hasNext()&&lastRebuilt<2;){
            long key=iterator.next();iterator.remove();var section=SectionPos.of(key);
            mc.levelRenderer.setSectionDirty(section.x(),section.y(),section.z());lastRebuilt++;
        }
        lastWorkNanos=System.nanoTime()-started;
    }
}

