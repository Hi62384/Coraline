package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Additional synchronized kinds on the existing lightweight story-prop entity. */
public final class RescueProps {
    public static final int MANTIS=60,EYE=61,WRAPPER=62,SWARM=63,RAT_BALL=64,CLOTHES=65,FURNACE=66,WEB=67,PURSUING_DOOR=68,CHEESE=69,FREED_GHOST=70,SNOW_GLOBE=71,KEY=72,STONE=73,HAND=74,ROOF_SWING=75;
    public static final int KEY_BUNDLE=76,CONTROL_POINT=91;
    public static final int HAND_WAITING_CUE=2;
    public static final int TONGS=81,ROCK=82,PHOTO=83;
    public static boolean kind(int kind){return kind>=MANTIS&&kind<=KEY_BUNDLE||kind==CONTROL_POINT||kind>=TONGS&&kind<=PHOTO;}
    public static CinematicPropEntity find(ServerPlayer p,VisitData.Scene s,String key){
        var n=ParentRescueStory.scenery(s);if(n.contains("Rescue"+key+"Position")&&n.contains("Rescue"+key+"Dimension")){var l=com.coraline.parents.FamilyData.level(p.server,n.getString("Rescue"+key+"Dimension"));if(l!=null)l.getChunkAt(net.minecraft.core.BlockPos.of(n.getLong("Rescue"+key+"Position")));}
        if(n.hasUUID("Rescue"+key)){
            for(var level:p.server.getAllLevels())if(level.getEntity(n.getUUID("Rescue"+key)) instanceof CinematicPropEntity prop&&prop.owned(s))return prop;
            return null;
        }
        return FirstEscapeScene.prop(p,s,"Rescue"+key);
    }
    public static CinematicPropEntity ensure(ServerPlayer p,VisitData.Scene s,String key,int kind,int slot,Vec3 at){
        if(ParentRescueStory.data(s).contains("Scenery")&&!ParentRescueStory.data(s).contains("Scenery",net.minecraft.nbt.Tag.TAG_COMPOUND))return null;
        var prop=find(p,s,key);if(prop==null){if(ParentRescueStory.scenery(s).hasUUID("Rescue"+key))return null;prop=FirstEscapeScene.ensure(p,s,"Rescue"+key,kind,slot,p.serverLevel(),at);}if(prop==null)return null;
        if(key.equals("SeveredHand")&&(kind!=HAND||slot!=0||prop.kind()!=HAND||prop.slot()!=0))return null;
        if(prop.level()!=p.serverLevel()){
            p.serverLevel().getChunkAt(net.minecraft.core.BlockPos.containing(at));var moved=prop.changeDimension(p.serverLevel(),new net.minecraftforge.common.util.ITeleporter(){@Override public net.minecraft.world.entity.Entity placeEntity(net.minecraft.world.entity.Entity entity,net.minecraft.server.level.ServerLevel current,net.minecraft.server.level.ServerLevel destination,float yaw,java.util.function.Function<Boolean,net.minecraft.world.entity.Entity> reposition){var transferred=reposition.apply(false);if(transferred!=null)transferred.setPos(at);return transferred;}});
            if(!(moved instanceof CinematicPropEntity result)||!result.getUUID().equals(prop.getUUID()))return null;prop=result;
        }
        if(prop.kind()!=kind||prop.slot()!=slot)prop.configure(s,kind,slot);var record=ParentRescueStory.scenery(s);record.putUUID("Rescue"+key,prop.getUUID());record.putLong("Rescue"+key+"Position",prop.blockPosition().asLong());record.putString("Rescue"+key+"Dimension",prop.level().dimension().location().toString());return prop;
    }
    public static void frame(CinematicPropEntity prop,int cue,int time,float value){if(prop!=null&&time%2==0)prop.frame(cue,time,value);}
    public static void hide(ServerPlayer p,VisitData.Scene s,String key){var prop=find(p,s,key);if(prop!=null)prop.setInvisible(true);}
    public static void retire(ServerPlayer p,VisitData.Scene s,String key){if(!p.getUUID().equals(s.owner))return;var prop=find(p,s,key);if(prop==null)return;prop.ejectPassengers();prop.discard();var n=ParentRescueStory.scenery(s);n.remove("Rescue"+key);n.remove("Rescue"+key+"Position");n.remove("Rescue"+key+"Dimension");}
    private RescueProps(){}
}
