package com.coraline.secrets;
import com.coraline.registry.CharacterCast;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CharacterCastGameTests {
    @GameTest(template="empty") public static void castActorsSpawnWithAttributesAndKeepTheirIdentity(GameTestHelper h){
        h.assertTrue(CharacterCast.ENTITIES.size()==15,"Missing cast actor");
        for(var entry:CharacterCast.ENTITIES.entrySet()){
            var actor=entry.getValue().get().create(h.getLevel());
            h.assertTrue(actor!=null&&actor.getMaxHealth()==20,"Actor lacks registered attributes");
            h.assertTrue(actor.characterId().equals(entry.getKey()),"Wrong model selected");
            h.assertTrue(actor.getNavigation()!=null&&actor.getBbHeight()>0,"Actor lacks navigation or bounds");
            var tag=new CompoundTag();h.assertTrue(actor.save(tag),"Actor cannot be saved");
            h.assertTrue(tag.getString("id").equals("coraline:character_"+entry.getKey()),"Saved model identity changed");
            actor.discard();
        }
        h.succeed();
    }
}
