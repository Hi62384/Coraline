package com.coraline.client;

import java.util.*;
import com.coraline.tycoon.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class TycoonFoodOverlay {
    private static CompoundTag data=new CompoundTag();
    public static void receive(CompoundTag n){data=n.copy();}
    public static int color(double value,boolean highGood){boolean good=highGood?value>=.65:value<=.35,bad=highGood?value<.35:value>.65;return good?0x79E595:bad?0xFF7D78:0xFFE28B;}
    public static boolean labels(TycoonNpc actor,PoseStack pose,MultiBufferSource buffers,int light){var mc=Minecraft.getInstance();if(!data.getBoolean("Debug")||mc.player==null||!mc.player.hasPermissions(2)||mc.player.distanceToSqr(actor)>32*32)return false;
        CompoundTag row=null;for(var e:data.getList("Rows",10)){var n=(CompoundTag)e;if(n.getInt("Entity")==actor.getId()&&n.getUUID("UUID").equals(actor.getUUID())){row=n;break;}}if(row==null)return false;
        boolean personal=actor.level().dimension()==TycoonWorld.KEY;if(personal!=row.getBoolean("Personal"))return false;
        String[] lines={row.getString("Name"),personal?String.format(Locale.ROOT,"Trustness: %.3f",row.getDouble("Trustness")):String.format(Locale.ROOT,"Hunger: %.1f / 100",row.getDouble("Hunger")),personal?String.format(Locale.ROOT,"Suspicion: %.3f",row.getDouble("Suspicion")):String.format(Locale.ROOT,"Neglect: %.1f / 100",row.getDouble("Neglect"))};
        pose.pushPose();pose.translate(0,actor.getBbHeight()+.55,0);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(-.023F,-.023F,.023F);
        for(int i=0;i<3;i++){double value=i==1?(personal?row.getDouble("Trustness"):row.getDouble("Hunger")/100):(personal?row.getDouble("Suspicion"):row.getDouble("Neglect")/100);int color=i==0?0xFFF1D9AC:0xFF000000|color(value,i==1);mc.font.drawInBatch(lines[i],-mc.font.width(lines[i])/2F,i*10,color,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0x800D1020,15728880);}
        pose.popPose();return true;
    }
    @SubscribeEvent public static void hud(net.minecraftforge.client.event.RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){data=new CompoundTag();return;}if(!data.getBoolean("CakeHeld")||TycoonFoodContent.index(mc.player.getMainHandItem())!=7||mc.level.dimension()!=TycoonWorld.KEY)return;var names=new ArrayList<String>();for(var name:data.getList("CakeNames",8))names.add(name.getAsString());
        int y=8;var g=e.getGuiGraphics();String title="Welcome Home Cake — Eligible Visitors";int width=Math.min(310,mc.getWindow().getGuiScaledWidth()-16);g.fill(5,5,8+width,31+Math.min(9,Math.max(1,names.size()))*11,0xBB181424);g.drawString(mc.font,title,9,y,0xFFE8CF91);y+=16;
        if(names.isEmpty())g.drawString(mc.font,"No first-time visitors can receive this cake.",9,y,0xFFE1D8CE);else for(int i=0;i<Math.min(8,names.size());i++){g.drawString(mc.font,names.get(i),9,y,0xFFD5F0DE);y+=11;}if(names.size()>8)g.drawString(mc.font,"+ "+(names.size()-8)+" more eligible visitors",9,y,0xFFD5F0DE);
    }
    private TycoonFoodOverlay(){}
}
