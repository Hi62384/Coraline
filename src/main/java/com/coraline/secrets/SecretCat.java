package com.coraline.secrets;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import com.coraline.feast.OtherMotherLoop;
import com.coraline.feast.VisitAudio;

/** An occasional, non-combat visitor, never a persistent objective arrow. */
public final class SecretCat {
    public static void tick(ServerPlayer p){
        if(p.tickCount%40!=0)return;
        var s=SecretTransactions.state(p);var world=s.world;
        if(!world.getBoolean("CatTried")&&SecretWorld.rooms(s).size()>=3){
            world.putBoolean("CatTried",true);
            if(p.getRandom().nextInt(3)==0){
                var room=SecretWorld.rooms(s).get(p.getRandom().nextInt(3));
                var at=BlockPos.of(room.getLong("Pos")).north(7);
                var cat=EntityType.CAT.create(p.serverLevel());
                if(cat!=null){var appearance=new CompoundTag();appearance.putString("variant","minecraft:all_black");cat.load(appearance);cat.setPersistenceRequired();cat.setInvulnerable(true);cat.getPersistentData().putBoolean("CoralineDecoration",true);cat.getPersistentData().putUUID("SecretOwner",p.getUUID());cat.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);p.serverLevel().addFreshEntity(cat);world.putUUID("Cat",cat.getUUID());world.putLong("CatHome",at.asLong());}
            }
        }
        if(!world.hasUUID("Cat")||!(p.serverLevel().getEntity(world.getUUID("Cat")) instanceof Cat cat))return;
        var at=BlockPos.of(world.getLong("CatHome"));
        if(s.searching()){cat.getNavigation().moveTo(at.getX()+4,at.getY(),at.getZ()-2,1.3);if(cat.distanceToSqr(p)<100)cat.playSound(net.minecraft.sounds.SoundEvents.CAT_HISS,.6F,1);}
        else if(cat.distanceToSqr(p)<144){cat.getLookControl().setLookAt(p,30,30);if(p.tickCount%200==0)cat.getNavigation().moveTo(at.getX()+p.getRandom().nextInt(7)-3,at.getY(),at.getZ()+p.getRandom().nextInt(5),.8);
            if(cat.distanceToSqr(p)<16&&!world.getBoolean("CatHint")){world.putBoolean("CatHint",true);VisitAudio.caption(p,"Cat","A locked door is an invitation. Just not necessarily a friendly one.",130);}}
    }
    private SecretCat(){}
}
