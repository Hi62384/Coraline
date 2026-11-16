import java.util.*;
import com.coraline.palace.PalaceWybieIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Real NBT/lifecycle checks only; does not instantiate a game/server/entity. */
public final class PalaceWybieSmoke {
    private static int checks;
    private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static int run(){
        for(int i=0;i<128;i++){
            var owner=UUID.randomUUID();var door=UUID.randomUUID();var id=UUID.randomUUID();var foreign=UUID.randomUUID();
            var state=PalaceWybieIdentity.create(owner,door,id,"minecraft:overworld",new BlockPos(1700+i,91,-150));
            check(state.matches(id)&&state.fate()==PalaceWybieIdentity.Fate.ALIVE&&!state.stored(),"Initial identity");
            check(!state.store(foreign)&&!state.recordDeath(foreign)&&!state.activate(foreign),"Foreign actor mutated identity");
            check(!state.remember(id,null,BlockPos.ZERO)&&!state.remember(id,"bad dimension",BlockPos.ZERO),"Invalid dimension");
            for(int scene=0;scene<24;scene++){
                check(state.store(id),"Original cannot be stored");
                var saved=state.save();state=PalaceWybieIdentity.load(owner,door,saved);
                check(state.stored()&&state.matches(id)&&state.blocker().isEmpty(),"Departure/reload lost original");
                check(state.activate(id)&&!state.stored(),"Living original cannot return");
                check(state.remember(id,"minecraft:overworld",new BlockPos(-2200+scene,96+i%4,710)),"Return location");
                saved.putUUID("Entity",foreign);check(state.entity().equals(id),"Save leaked mutable identity");
            }
            check(state.recordDeath(id)&&state.stored(),"Death must preserve dormant original");
            for(int reload=0;reload<32;reload++){
                state=PalaceWybieIdentity.load(owner,door,state.save());
                check(state.matches(id)&&state.fate()==PalaceWybieIdentity.Fate.DEAD&&state.stored(),"Death erased identity");
                check(!state.activate(id)&&state.store(id),"Retry resurrected Wybie");
            }
            var saved=state.save();
            saved.putString("AdditionalHistory","preserve future chapter metadata");state=PalaceWybieIdentity.load(owner,door,saved);
            check(state.save().getString("AdditionalHistory").equals("preserve future chapter metadata"),"Valid identity dropped chapter metadata");
            for(int kind=0;kind<7;kind++){
                var bad=saved.copy();bad.putString("FutureEvidence","keep this");
                switch(kind){case 0->bad.putInt("Version",99);case 1->bad.putUUID("Owner",foreign);case 2->bad.putUUID("Door",foreign);case 3->bad.putString("Entity","not UUID");case 4->bad.putBoolean("Stored",false);case 5->bad.putString("Dimension","bad dimension");case 6->bad.putString("Position","not position");}
                var blocked=PalaceWybieIdentity.load(owner,door,bad);
                check(!blocked.blocker().isEmpty()&&!blocked.matches(id)&&!blocked.activate(id)&&!blocked.recordDeath(id),"Invalid identity mutated");
                check(blocked.save().equals(bad)&&PalaceWybieIdentity.load(owner,door,blocked.save()).save().equals(bad),"Quarantine lost original evidence");
            }
        }
        return checks;
    }
}
