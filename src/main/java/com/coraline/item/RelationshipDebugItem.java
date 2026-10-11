package com.coraline.item;

import com.coraline.feast.OtherMotherLoop;
import com.coraline.feast.OtherMotherLoopState;
import com.coraline.feast.OtherMotherWelcome;
import com.coraline.feast.VisitAudio;
import com.coraline.feast.VisitData;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Locale;

/** Creative-only relationship controls; no ordinary item can change story state. */
public final class RelationshipDebugItem extends Item {
    private final boolean trust;
    private final int step;
    public RelationshipDebugItem(boolean trust,int step){super(new Properties().stacksTo(1));this.trust=trust;this.step=step;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p){
            if(!p.isCreative())p.sendSystemMessage(Component.literal("Creative mode is required for relationship debug items."));
            else {
                var scene=VisitData.get(p.server).scenes.get(p.getUUID());
                if(scene==null)p.sendSystemMessage(Component.literal("No Other Mother visit exists yet. Claim parents and complete the first visit first."));
                else {
                    float value=trust?scene.loop.trust:scene.loop.suspicion;
                    float next=Math.max(0,Math.min(1,(Math.round(value*4)+step)/4F));
                    OtherMotherLoop.relationship(p,trust?next:scene.loop.trust,trust?scene.loop.suspicion:next);
                    var state=OtherMotherLoopState.classify(scene.loop.trust,scene.loop.suspicion);
                    p.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "Other Mother: trust %.2f, suspicion %.2f — %s",scene.loop.trust,scene.loop.suspicion,state.id().replace('_',' '))));
                    // Sneak-use is an immediate voice preview; ordinary use changes
                    // the values for the next story line without interrupting it.
                    if(p.isShiftKeyDown()&&OtherWorld.isOther(p.level())){
                        String cue=OtherMotherWelcome.choose(state,scene.lastWelcome,p.getRandom());
                        VisitAudio.play(p,cue,false);scene.lastWelcome=cue;VisitData.get(p.server).setDirty();
                    }
                }
            }
            p.getCooldowns().addCooldown(this,5);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Creative: "+(step>0?"raise ":"lower ")+(trust?"trust":"suspicion")+" by 0.25.").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Sneak-use in the Other World to preview a matching welcome line.").withStyle(ChatFormatting.GRAY));
    }
}
