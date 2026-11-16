package com.coraline.client;

import com.coraline.feast.TabletopGames;
import com.coraline.secrets.SecretsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Optional tabletop view. Owner packets contain no opponent hands or charade answers. */
public final class TabletopGameScreen extends Screen {
    private CompoundTag packet,rules;private int left,top,w,h,pullTicks;private boolean pulling,flicking;private int wildColor,clueProp,clueGesture;private double angle,power=.4;
    private static final int[] COLORS={0xFFBA4B5D,0xFF457CB7,0xFFD8AF45,0xFF539B79};
    TabletopGameScreen(CompoundTag n){super(Component.literal(n.getBoolean("PartyGame")?"House Party Game":"Family Game Night"));update(n);}
    static void receive(CompoundTag n){if(n.getBoolean("FamilyGame")){WorldTabletop.receive(n);return;}var mc=Minecraft.getInstance();if(n.getBoolean("Close")){if(mc.screen instanceof TabletopGameScreen)mc.setScreen(null);return;}if(mc.screen instanceof TabletopGameScreen s){String before=s.rules.getString("Game");s.update(n);if(!before.equals(s.rules.getString("Game"))||s.packet.getBoolean("Open"))s.rebuildWidgets();}else if(n.getBoolean("Open"))mc.setScreen(new TabletopGameScreen(n));}
    void update(CompoundTag n){
        boolean newMatch=packet!=null&&(!Objects.equals(packet.getUUID("Nonce"),n.getUUID("Nonce"))
                ||!rules.getString("Game").equals(n.getCompound("Game").getString("Game")));
        packet=n.copy();rules=packet.getCompound("Game");
        if(newMatch||!playerTurn()||rules.getInt("Physics")>0){pulling=false;flicking=false;pullTicks=0;}
    }
    private boolean partyGame(){return packet.getBoolean("PartyGame");}
    private boolean resultScreen(){return partyGame()?rules.getBoolean("Done"):packet.getString("Phase").equals("GAME_NIGHT_CHOICE");}
    private String name(int who){return TabletopGames.participantName(rules,who);}
    private String shortName(int who,int pixels){return font.plainSubstrByWidth(name(who),Math.max(12,pixels));}
    @Override public boolean isPauseScreen(){return false;}
    private void action(String text){SecretsNetwork.request(packet.getUUID("Nonce"),"visit_choice",text);}
    @Override protected void init(){w=Math.min(440,width-20);h=Math.min(295,height-20);left=(width-w)/2;top=(height-h)/2;addRenderableWidget(Button.builder(Component.literal("Look around"),b->onClose()).bounds(left+w-100,top+h-23,90,18).build());}
    @Override public void tick(){if(pulling)pullTicks++;if(partyGame()?!packet.getBoolean("TabletopActive"):!packet.getString("Phase").equals("GAME_NIGHT_ACTIVE")&&!packet.getString("Phase").equals("GAME_NIGHT_CHOICE"))onClose();}
    private boolean playerTurn(){return !rules.getBoolean("Done")&&(rules.getInt("Turn")==0||TabletopGames.game(rules)==TabletopGames.Game.SILENT_CHARADES);}
    private boolean canRematch(){return !partyGame()&&packet.getInt("Matches")<2;}
    private boolean waitingForVoice(){return packet.getInt("GameReactionRemaining")>0;}
    private static boolean inside(double x,double y,int left,int top,int width,int height){return x>=left&&x<left+width&&y>=top&&y<top+height;}
    private void line(GuiGraphics g,int x0,int y0,int x1,int y1,int color){int steps=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));for(int i=0;i<=steps;i++){float t=steps==0?0:(float)i/steps;int x=Math.round(x0+(x1-x0)*t),y=Math.round(y0+(y1-y0)*t);g.fill(x-1,y-1,x+1,y+1,color);}}
    private int clueStep(){return Math.max(10,Math.min(24,(h-155)/4));}
    private int clueStart(){return top+78;}
    private int clueAt(double x,double y,int y0,int step,int cardHeight){int cell=(w-24)/3;for(int i=0;i<6;i++)if(inside(x,y,left+12+i%3*cell,y0+i/3*step,(w-30)/3,cardHeight))return i;return -1;}
    private void card(GuiGraphics g,int x,int y,int width,int height,int color,String label,boolean glow){g.fill(x-1,y-1,x+width+1,y+height+1,glow?0xFFFFDF94:0xFF382C32);g.fill(x,y,x+width,y+height,0xFFF7E6C6);g.fill(x+4,y+4,x+width-4,y+height-4,color);g.drawCenteredString(font,label,x+width/2,y+height/2-3,0xFFF8EBDD);}
    private static String symbol(int c){return new String[]{"1","2","3","4","Skip","Rev","+1","Wild"}[Math.floorMod(c,8)];}
    private void circle(GuiGraphics g,int x,int y,int r,int color){for(int dy=-r;dy<=r;dy++){int dx=(int)Math.sqrt(r*r-dy*dy);g.fill(x-dx,y+dy,x+dx+1,y+dy+1,color);}}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){g.fill(left-2,top-2,left+w+2,top+h+2,0xFFBC925F);g.fill(left,top,left+w,top+h,0xEE302630);g.drawString(font,rules.getString("Game").replace('_',' '),left+12,top+10,0xFFF5D1A0);String turn=resultScreen()?(partyGame()?"Match finished":waitingForVoice()?"Listening to Father...":canRematch()?"One more game, or finish?":"Tonight's games are finished"):TabletopGames.game(rules)==TabletopGames.Game.SILENT_CHARADES?name(rules.getInt("Actor"))+" is acting":(playerTurn()?"YOUR TURN":name(rules.getInt("Turn"))+"'s turn");g.drawString(font,turn,left+12,top+25,0xFFEDDCA8);
        int cx=left+w/2,cy=top+h/2;
        if(resultScreen()){g.drawWordWrap(font,Component.literal(TabletopGames.resultText(rules)),left+20,cy-55,w-40,0xFFFFD68A);if(canRematch())card(g,cx-145,cy,130,40,waitingForVoice()?0xFF56515C:0xFF547C66,"Another game",false);card(g,canRematch()?cx+15:cx-65,cy,130,40,waitingForVoice()?0xFF56515C:0xFF815A68,partyGame()?"Back to party":"Finish tonight",false);super.render(g,mx,my,partial);return;}
        switch(TabletopGames.game(rules)){
            case BUTTON_CARDS->{int topCard=rules.getInt("Top");card(g,cx-28,top+66,56,68,COLORS[rules.getInt("Color")],symbol(topCard),true);card(g,cx-112,top+66,56,68,0xFF645A86,"Draw",false);int[] hand=rules.getIntArray("Hand0");int cw=Math.min(48,(w-24)/Math.max(1,hand.length));for(int i=0;i<hand.length;i++){boolean legal=TabletopGames.legalCard(hand[i],topCard,rules.getInt("Color"));card(g,left+12+i*cw,top+h-93,cw-3,57,COLORS[hand[i]/8],symbol(hand[i]),legal&&playerTurn());}for(int i=0;i<4;i++){g.fill(cx+65+i*23,top+74,cx+84+i*23,top+93,COLORS[i]);if(i==wildColor)g.fill(cx+65+i*23,top+94,cx+84+i*23,top+96,0xFFFDF1BA);}g.drawString(font,"Wild color",cx+65,top+100,0xFFE7D3BB);int seats=TabletopGames.participantCount(rules)-1,space=(w-24)/seats;for(int i=1;i<=seats;i++)g.drawString(font,shortName(i,space-48)+": "+rules.getInt("HandCount"+i)+" cards",left+12+(i-1)*space,top+44,0xFFD6C6BD);}
            case THREAD_FOUR->{int cell=Math.min(27,(h-110)/6),x0=cx-cell*7/2,y0=top+52;var board=rules.getIntArray("Board");for(int row=0;row<6;row++)for(int col=0;col<7;col++){int x=x0+col*cell,y=y0+(5-row)*cell;g.fill(x,y,x+cell,y+cell,0xFF755780);circle(g,x+cell/2,y+cell/2,cell/2-3,board[row*7+col]<0?0xFF251F2C:board[row*7+col]==0?COLORS[1]:COLORS[0]);}g.drawCenteredString(font,font.plainSubstrByWidth("Choose a column · "+TabletopGames.teamName(rules,0)+" vs "+TabletopGames.teamName(rules,1),w-24),cx,top+h-42,0xFFE9D4B8);}
            case WOBBLY_TOWER->{var rows=rules.getIntArray("Tower");int y0=top+45,rowH=Math.min(13,(h-105)/12);for(int row=11;row>=0;row--)for(int piece=0;piece<3;piece++)if((rows[row]&(1<<piece))!=0){int x=cx-75+piece*50,y=y0+(11-row)*rowH;g.fill(x,y,x+48,y+rowH-2,piece==1?0xFFB18351:0xFFD0A56D);g.hLine(x+3,x+43,y+rowH/2,0xFF8B623E);}int bar=(int)(Math.max(0,rules.getDouble("Stability"))*160);g.fill(cx-80,top+h-65,cx+80,top+h-59,0xFF512E37);g.fill(cx-80,top+h-65,cx-80+bar,top+h-59,0xFF86AE7E);g.drawCenteredString(font,pulling?"Release near the green marker":"Hold a block, release after two seconds",cx,top+h-48,0xFFE9D4B8);if(pulling){g.fill(cx-80,top+h-82,cx+80,top+h-76,0xFF786452);g.fill(cx-3,top+h-84,cx+4,top+h-74,0xFF91D3A3);int pos=cx-80+Math.min(160,pullTicks*2);g.fill(pos,top+h-85,pos+2,top+h-74,0xFFFFE5A3);}}
            case MOUSEBOARD_DASH->{int cellW=Math.min(43,(w-44)/8),cellH=Math.min(33,(h-125)/4);for(int i=0;i<31;i++){int x=left+22+i%8*cellW,y=top+55+i/8*cellH;g.fill(x,y,x+cellW-8,y+cellH-6,i%7==3?0xFFB29A54:i%9==5?0xFF6B586E:0xFF638169);g.drawCenteredString(font,Integer.toString(i),x+(cellW-8)/2,y+cellH-15,0xFFEFE4CB);}int[] mice=rules.getIntArray("Mouse");for(int i=0;i<TabletopGames.participantCount(rules);i++){int index=Math.min(30,mice[i]),x=left+22+index%8*cellW+i*7,y=top+57+index/8*cellH;circle(g,x+5,y+6,5,COLORS[i]);circle(g,x+2,y+1,2,COLORS[i]);circle(g,x+8,y+1,2,COLORS[i]);}card(g,cx-135,top+h-65,120,30,0xFF547C66,"Roll · Safe",false);card(g,cx+15,top+h-65,120,30,0xFF876459,"Roll · Shortcut",false);g.drawString(font,"Die: "+rules.getInt("Die"),left+15,top+42,0xFFFFE3A6);}
            case BUTTON_FLICK->{int size=Math.min(w-80,h-105),x0=cx-size/2,y0=top+48;g.fill(x0,y0,x0+size,y0+size,0xFFBA925D);for(int r:new int[]{(int)(size*.3),(int)(size*.19),(int)(size*.1),(int)(size*.04)})circle(g,cx,y0+(int)(size*.325),r,r>size*.2?0xFF80614D:r>size*.1?0xFFC6AB69:0xFF739485);for(var tag:rules.getList("Pucks",10)){var puck=(CompoundTag)tag;int x=cx+(int)(puck.getDouble("X")*size/2),y=y0+(int)((1-puck.getDouble("Z"))*size/2);circle(g,x,y,5,COLORS[puck.getInt("Owner")]);g.fill(x-1,y-1,x+1,y+1,0xFF292432);}if(rules.getInt("Physics")==0){int startY=y0+(int)(size*.925);circle(g,cx,startY,6,COLORS[rules.getInt("Turn")]);g.fill(cx-1,startY-1,cx+1,startY+1,0xFF292432);}g.drawCenteredString(font,"Drag from the bottom puck to aim and set strength",cx,top+h-36,0xFFEAD7BC);if(flicking){int startY=y0+(int)(size*.925);double radians=Math.toRadians(angle),length=Math.max(12,power*size*.7);length=Math.min(length,Math.min((size/2.-3)/Math.max(.0001,Math.abs(Math.sin(radians))),(startY-y0-3)/Math.max(.0001,Math.cos(radians))));int aimX=cx+(int)(Math.sin(radians)*length),aimY=startY-(int)(Math.cos(radians)*length);line(g,cx,startY,aimX,aimY,0xFFFFE6A6);circle(g,aimX,aimY,3,0xFFFFE6A6);g.drawCenteredString(font,"Strength "+Math.round(power*100)+"% · Aim "+Math.round(angle)+"°",cx,top+42,0xFFFFE6A6);}}
            case SILENT_CHARADES->{int actor=rules.getInt("Actor");if(actor==0){g.drawCenteredString(font,"Your secret: "+TabletopGames.TOPICS[rules.getInt("Topic")],cx,top+48,0xFFFFE4A5);g.drawCenteredString(font,"Choose a prop and a gesture, then act",cx,top+63,0xFFD2C4BA);for(int i=0;i<6;i++){int x=left+12+i%3*(w-24)/3,y=clueStart()+i/3*clueStep();card(g,x,y,(w-30)/3,clueStep()-4,i==clueProp?0xFF6B987D:0xFF6A576F,TabletopGames.TOPICS[i],false);int gy=clueStart()+clueStep()*2+8+i/3*clueStep();card(g,x,gy,(w-30)/3,clueStep()-4,i==clueGesture?0xFF9A7B54:0xFF554D69,new String[]{"Flap","Cast","Tick-tock","Eat","Paws","Row"}[i],false);}card(g,cx-45,top+h-44,90,20,0xFF547C66,"ACT",false);}
                else{g.drawCenteredString(font,"Watch the actor at the table, then guess",cx,top+55,0xFFEBD3AD);for(int i=0;i<6;i++)card(g,left+12+i%3*(w-24)/3,top+88+i/3*50,(w-30)/3,40,0xFF6C5A7D,TabletopGames.TOPICS[i],false);}}
        }
        int[] scores=rules.getIntArray("Scores");if(scores.length==TabletopGames.participantCount(rules)){int space=Math.max(20,(w-122)/scores.length);for(int i=0;i<scores.length;i++)g.drawString(font,shortName(i,space-20)+" "+scores[i],left+12+i*space,top+h-18,0xFFCBBAA4);}super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){if(super.mouseClicked(mx,my,button))return true;if(button!=0)return false;int cx=left+w/2;if(resultScreen()){if(waitingForVoice())return true;if(canRematch()&&inside(mx,my,cx-145,top+h/2,130,40))action("another");else if(inside(mx,my,canRematch()?cx+15:cx-65,top+h/2,130,40))action("finish");return true;}if(!playerTurn())return false;
        switch(TabletopGames.game(rules)){
            case BUTTON_CARDS->{var hand=rules.getIntArray("Hand0");int cw=Math.min(48,(w-24)/Math.max(1,hand.length));if(my>=top+h-93&&my<=top+h-36){int index=(int)Math.floor((mx-left-12)/cw);if(index>=0&&index<hand.length)action("card:"+index+":"+wildColor);}else if(mx>=cx-112&&mx<=cx-56&&my>=top+66&&my<=top+134)action("draw");else if(my>=top+74&&my<=top+93&&mx>=cx+65&&mx<cx+157)wildColor=(int)(mx-cx-65)/23;}
            case THREAD_FOUR->{int cell=Math.min(27,(h-110)/6),col=(int)Math.floor((mx-(cx-cell*7/2))/cell);if(col>=0&&col<7&&my>=top+52&&my<top+52+cell*6)action("column:"+col);}
            case WOBBLY_TOWER->{int rowH=Math.min(13,(h-105)/12),row=11-(int)Math.floor((my-top-45)/rowH),piece=(int)Math.floor((mx-cx+75)/50);if(row>=0&&row<10&&piece>=0&&piece<3){action("pull_begin:"+row+":"+piece);pulling=true;pullTicks=0;}}
            case MOUSEBOARD_DASH->{if(inside(mx,my,cx-135,top+h-65,120,30))action("roll:safe");else if(inside(mx,my,cx+15,top+h-65,120,30))action("roll:risky");}
            case BUTTON_FLICK->{int size=Math.min(w-80,h-105),startY=top+48+(int)(size*.925);if(rules.getInt("Physics")==0&&Math.hypot(mx-cx,my-startY)<=14){flicking=true;aim(mx,my);}}
            case SILENT_CHARADES->{if(rules.getInt("Actor")==0){if(inside(mx,my,cx-45,top+h-44,90,20))action("clue:"+clueProp+":"+clueGesture);else{int prop=clueAt(mx,my,clueStart(),clueStep(),clueStep()-4),gesture=clueAt(mx,my,clueStart()+clueStep()*2+8,clueStep(),clueStep()-4);if(prop>=0)clueProp=prop;else if(gesture>=0)clueGesture=gesture;}}else{int guess=clueAt(mx,my,top+88,50,40);if(guess>=0)action("guess:"+guess);}}
        }return true;
    }
    void aim(double mx,double my){int cx=left+w/2,size=Math.min(w-80,h-105);double dx=mx-cx,dz=top+48+size*.925-my;angle=Math.max(-70,Math.min(70,Math.toDegrees(Math.atan2(dx,Math.max(1,dz)))));power=Math.max(0,Math.min(1,Math.hypot(dx,dz)/Math.max(1,size)));}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(flicking){aim(x,y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(pulling){pulling=false;if(playerTurn())action("pull_release");return true;}if(flicking){flicking=false;if(playerTurn()&&rules.getInt("Physics")==0)action("flick:"+angle+":"+power);return true;}return super.mouseReleased(x,y,button);}
}
