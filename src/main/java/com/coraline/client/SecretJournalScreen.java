package com.coraline.client;

import com.coraline.secrets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Parchment journal and a separate permission-gated operator workspace. */
public final class SecretJournalScreen extends Screen {
    private CompoundTag data;
    private int voiceEvent,voiceState,voiceVariation=-1,scoldCategory,scoldState;
    private String tab="Collection",filter="",selected="",confirmation="";
    private final Set<String> selection=new LinkedHashSet<>();
    private final List<EditBox> values=new ArrayList<>();
    private int page,left,top,w,h,refreshTicks,previewScroll;
    private EditBox search,argument;
    private SecretJournalScreen(CompoundTag n){super(Component.literal(n.getBoolean("Lock")?"Seal Decoder":n.getBoolean("Debug")?"Secrets Debugger":"Secret Journal"));data=n;}
    public static void receive(CompoundTag n){if(n==null)return;var mc=Minecraft.getInstance();if(!n.getBoolean("Debug")&&n.contains("Reward")&&n.getInt("Reward")>=0){mc.setScreen(new BlackmailRewardScreen(n));return;}if(!n.getBoolean("Debug")&&!n.getBoolean("AtJournal")&&!n.getBoolean("Lock"))return;if(mc.screen instanceof SecretJournalScreen screen&&screen.data.getBoolean("Debug")==n.getBoolean("Debug")&&screen.data.getBoolean("Lock")==n.getBoolean("Lock")){screen.data=n;screen.confirmation="";screen.rebuildWidgets();}else mc.setScreen(new SecretJournalScreen(n));}
    @Override public boolean isPauseScreen(){return false;}
    private UUID nonce(){return data.getUUID("Nonce");}
    private void action(String id,String value){SecretsNetwork.request(nonce(),id,value);}
    private void button(String name,int x,int y,int width,Runnable action){
        if(data.getBoolean("Debug")){addRenderableWidget(Button.builder(Component.literal(name),b->action.run()).bounds(x,y,width,18).build());return;}
        addRenderableWidget(new JournalButton(name,x,y,width,()->tab.equals(name),action));
    }
    private static final class JournalButton extends Button {
        private final java.util.function.BooleanSupplier selected;
        JournalButton(String label,int x,int y,int w,java.util.function.BooleanSupplier selected,Runnable action){super(x,y,w,18,Component.literal(label),b->action.run(),DEFAULT_NARRATION);this.selected=selected;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            boolean bright=isHoveredOrFocused()||selected.getAsBoolean();int x=getX(),y=getY();
            g.fill(x,y,x+width,y+height,bright?0xFFAF8854:0xFF806447);
            g.fill(x+1,y+1,x+width-1,y+height-1,bright?0xFF66405B:0xFF382A36);
            if(selected.getAsBoolean())g.fill(x+5,y+height-3,x+width-5,y+height-2,0xFFE9C984);
            g.drawCenteredString(Minecraft.getInstance().font,getMessage(),x+width/2,y+5,active?0xFFFFEAC4:0xFF958579);
        }
    }
    private boolean has(String list,String id){for(var n:data.getList(list,8))if(n.getAsString().equals(id))return true;return false;}
    private String ids(){return String.join(",",selection.isEmpty()&&!selected.isEmpty()?Set.of(selected):selection);}
    @Override protected void init(){w=Math.min(570,width-16);h=Math.min(320,height-16);left=(width-w)/2;top=(height-h)/2;values.clear();
        if(data.getBoolean("Lock")){button("Set pin",left+w/2-55,top+h-44,110,()->action("pin",""));return;}
        String[] tabs=data.getBoolean("Debug")?new String[]{"Collection","Mother","Awareness","Journal","AI / Spies","World","Blackmail","Voice","Scold"}:new String[]{"Collection","Connections","Abilities","Map"};int tw=(w-16)/tabs.length;
        for(int i=0;i<tabs.length;i++){String t=tabs[i];String label=font.width(t)>tw-6?switch(t){case "Collection"->"Collect";case "Awareness"->"Aware";case "AI / Spies"->"Spies";case "Blackmail"->"Mail";default->t;}:t;button(label,left+8+i*tw,top+26,tw-2,()->{tab=t;page=0;confirmation="";rebuildWidgets();});}
        if(data.getInt("Reward")>=0){for(int i=0;i<3;i++){int choice=i;button(new String[]{"Choose emeralds","Choose diamonds","Choose gold"}[i],left+8+i*(w-16)/3,top+h-25,(w-22)/3,()->action("reward",Integer.toString(choice)));}}
        else button("Close",left+w-64,top+h-25,56,this::onClose);
        if(data.getBoolean("AtJournal")&&!data.getBoolean("Debug"))button("Move journal",left+76,top+h-52,92,()->{action("move_journal","");onClose();});
        if(tab.equals("Mother")){motherControls();return;}
        if(tab.equals("Voice")){voiceControls();return;}
        if(tab.equals("Scold")){scoldControls();return;}
        if(tab.equals("AI / Spies")||tab.equals("Journal")||tab.equals("World")||tab.equals("Blackmail")){debugControls();return;}
        if(tab.equals("Collection")||tab.equals("Awareness")){
            selection.retainAll(entries().stream().map(SecretRegistry.Secret::id).toList());if(entries().stream().noneMatch(s->s.id().equals(selected)))selected="";if(selected.isEmpty()&&!entries().isEmpty()){selected=entries().get(0).id();selection.add(selected);}
            search=new EditBox(font,left+10,top+50,w/2-20,18,Component.literal("Search secrets"));search.setHint(Component.literal("Search secrets..."));search.setTooltip(Tooltip.create(Component.literal("Shift-click entries to select several secrets.")));search.setValue(filter);search.setResponder(v->{filter=v;page=0;});addRenderableWidget(search);
            button("<",left+10,top+h-52,26,()->{page=Math.max(0,page-1);rebuildWidgets();});button(">",left+40,top+h-52,26,()->{page++;rebuildWidgets();});
            int bx=left+w/2+6,by=top+h-76,bw=(w/2-20)/2;
            if(data.getBoolean("Debug")){
                if(tab.equals("Awareness")){button("Confirm",bx,by,bw,()->action("expose",ids()));button("Suspect",bx+bw+4,by,bw,()->action("suspect",ids()));button("Unaware",bx,by+21,bw,()->action("unaware",ids()));button("Expose all",bx+bw+4,by+21,bw,()->action("expose_all",""));}
                else {button("Give selected",bx,by,bw,()->action("give",ids()));button("Give all",bx+bw+4,by,bw,()->action("give_all",""));button("Deposit direct",bx,by+21,bw,()->action("deposit_debug",ids()));button("Remove",bx+bw+4,by+21,bw,()->action("remove_debug",ids()));}
            }else if(data.getBoolean("AtJournal")){button("Deposit",bx,by,bw,()->action("deposit",ids()));button("Delete",bx+bw+4,by,bw,()->{previewScroll=0;confirmation="delete";});button("Make blackmail",bx,by+21,bw*2+4,()->{previewScroll=0;confirmation="blackmail";});}
        }
        if(tab.equals("Abilities")){int row=0;for(var n:data.getList("Abilities",8)){String id=n.getAsString();if(!Set.of("invisibility","false_signal","combat","gift","servant","command","reveal","hijack","unweave").contains(id))continue;button(abilityName(id),left+14+(row%2)*(w/2),top+65+(row/2)*23,w/2-28,()->action("ability",id));row++;}}
    }
    private static String abilityName(String id){return switch(id){case "invisibility"->"Shadow veil · 60s";case "false_signal"->"Plant false report · 30s";case "combat"->"Threadbreaker · 30s";case "gift"->"Request secret delivery";case "servant"->"Command one rat";case "command"->"Command nearby rats";case "reveal"->"Reveal illusions · 20s";case "hijack"->"Borrow spy's eyes · 15s";case "unweave"->"Silence spies · 30s";default->id;};}
    private void motherControls(){String[] labels={"Trustness","Suspicion","Code","Treasure","Hidability","Pattern","Weakness","Rewards"};int rowHeight=Math.min(40,(h-104)/4),colWidth=(w-24)/2;
        for(int i=0;i<8;i++){int row=i,colX=left+12+(i/4)*colWidth,rowY=top+52+(i%4)*rowHeight;double v=i==0?data.getDouble("Trust"):i==1?data.getDouble("Suspicion"):data.getDouble("Counter"+(i-2));var box=new EditBox(font,colX+colWidth-58,rowY+11,50,18,Component.literal(labels[i]));box.setValue(String.format(Locale.ROOT,"%.2f",v));addRenderableWidget(box);values.add(box);addRenderableWidget(new AbstractSliderButton(colX,rowY+11,colWidth-64,18,Component.literal(labels[i]),v){@Override protected void updateMessage(){setMessage(Component.literal(labels[row]+": "+String.format(Locale.ROOT,"%.2f",value)));}@Override protected void applyValue(){box.setValue(String.format(Locale.ROOT,"%.2f",value));}});}
        button("Apply overrides",left+12,top+h-48,120,()->action("override",String.join(",",values.stream().map(EditBox::getValue).toList())));button("Clear overrides",left+138,top+h-48,120,()->action("natural",""));button("Reset inputs",left+264,top+h-48,100,this::rebuildWidgets);
    }
    private void scoldControls(){
        String[] states={"Low trust / low suspicion","Mid trust / mid suspicion","High trust / low suspicion","Low trust / high suspicion","High trust / high suspicion"};
        button("Category: "+(scoldCategory==0?"Minor":"Serious"),left+12,top+50,w-24,()->{scoldCategory=1-scoldCategory;rebuildWidgets();});
        button(states[scoldState],left+12,top+71,w-24,()->{scoldState=(scoldState+1)%5;rebuildWidgets();});
        button("Play scolding (preview only)",left+12,top+92,w-24,()->action("scold_preview",scoldCategory+","+scoldState));
        String[] labels={"Test ordinary catch","Test secret area","Test restricted item","Test porch bell","Test accepted blackmail","Test rejected blackmail"};
        for(int i=0;i<labels.length;i++){int event=i;button(labels[i],left+12+(i%2)*(w-24)/2,top+113+(i/2)*21,(w-28)/2,()->action("scold_event",Integer.toString(event)));}
        button("Force next activity proposal",left+12,top+176,w-24,()->action("scold_event","6"));
    }
    private void voiceControls(){
        String[] events={"Ordinary area caught","Secret area caught","Restricted item caught","Blackmail offer","Blackmail accepted","Blackmail rejected"};
        String[] states={"Low trust / low suspicion","Mid trust / mid suspicion","High trust / low suspicion","Low trust / high suspicion","High trust / high suspicion"};
        button(events[voiceEvent],left+16,top+60,w-32,()->{voiceEvent=(voiceEvent+1)%6;rebuildWidgets();});
        button(states[voiceState],left+16,top+83,w-32,()->{voiceState=(voiceState+1)%5;rebuildWidgets();});
        button("Variation: "+(voiceVariation<0?"Random":Integer.toString(voiceVariation+1)),left+16,top+106,w-32,()->{voiceVariation=voiceVariation==1?-1:voiceVariation+1;rebuildWidgets();});
        button("Play voice (no penalties)",left+16,top+132,w-32,()->action("voice_preview",voiceEvent+","+voiceState+","+voiceVariation));
        button("Actual event (real consequences)",left+16,top+155,w-32,()->action("voice_event",Integer.toString(voiceEvent)));
    }
    private void debugControls(){argument=new EditBox(font,left+12,top+52,w-24,18,Component.literal("Value / IDs"));argument.setMaxLength(4096);argument.setValue(tab.equals("Blackmail")?ids():"0");addRenderableWidget(argument);
        String[][] buttons=switch(tab){
            case "Journal"->new String[][]{{"Clear journal","clear"},{"Inspect chosen spot","inspect"},{"Discover journal","discover"},{"Relocate (0–5)","hide"},{"Set inspection progress","inspection_time"},{"Forget knowledge","forget"}};
            case "AI / Spies"->new String[][]{{"Start search","search"},{"Stop search","stop"},{"Missed portal delay","miss"},{"Set delay ticks","delay"},{"Direct chase","chase"},{"Noise at me","noise"},{"Spawn doll","doll"},{"Spawn rat","rat"},{"Spy report","report"},{"Bring Mother","mother"},{"Trigger trap (0–2)","trap"},{"Alert nearest rat","rat_alert"},{"Clear nearby spies","clear_spies"}};
            case "World"->new String[][]{{"Regenerate visit","regenerate"},{"Teleport to room index","teleport"},{"Build through level (0–3)","generate_tier"},{"Unlock chosen room","unlock_debug"},{"Reveal caches / ores","reveal_debug"},{"Refresh positions","refresh"}};
            default->new String[][]{{"Create from selected IDs","blackmail_debug"},{"Force acceptance","accept"},{"Force rejection","reject"},{"Give loop by ID","give_loop"}};
        };
        int columns=tab.equals("AI / Spies")?3:2;int rowHeight=Math.min(23,(h-115)/((buttons.length+columns-1)/columns));for(int i=0;i<buttons.length;i++){String[] pair=buttons[i];button(pair[0],left+12+(i%columns)*(w-24)/columns,top+78+(i/columns)*rowHeight,(w-30)/columns,()->action(pair[1],argument.getValue()));}
    }
    private List<SecretRegistry.Secret> entries(){return SecretRegistry.ALL.values().stream().filter(s->data.getBoolean("Debug")||has("Journal",s.id())||has("Carried",s.id())).filter(s->(s.name()+s.id()).toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))).toList();}
    private int rows(){return Math.max(1,(h-136)/22);}
    @Override public void tick(){super.tick();if(!data.getBoolean("Lock")&&(tab.equals("Map")||tab.equals("Scold"))&&++refreshTicks%20==0)action("refresh","");}
    @Override public boolean mouseClicked(double x,double y,int button){if(!confirmation.isEmpty()){if(y>top+h-53&&y<top+h-30){if(x<left+w/2)action(confirmation,String.join(",",Arrays.stream(ids().split(",")).filter(id->has("Journal",id)).toList()));confirmation="";}return true;}
        if(tab.equals("Connections")){int cx=left+w/3,cy=top+h/2+8,radius=Math.min(w/4-16,h/3-10),i=0;for(var s:SecretRegistry.ALL.values()){double a=i++*Math.PI*2/SecretRegistry.ALL.size();if(Math.abs(x-cx-Math.cos(a)*radius)<8&&Math.abs(y-cy-Math.sin(a)*radius)<8&&has("Journal",s.id())){selected=s.id();selection.clear();selection.add(selected);tab="Collection";rebuildWidgets();return true;}}}
        if((tab.equals("Collection")||tab.equals("Awareness"))&&x>=left+10&&x<left+w/2&&y>=top+76&&y<top+h-60){int index=page*rows()+((int)y-top-76)/22;var entries=entries();if(index<entries.size()){selected=entries.get(index).id();if(hasShiftDown()){if(!selection.add(selected))selection.remove(selected);}else{selection.clear();selection.add(selected);}return true;}}
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);
        g.fill(left-5,top-5,left+w+7,top+h+7,0xAA000000);
        g.fill(left-4,top-4,left+w+4,top+h+4,0xFF362530);
        g.fill(left-2,top-2,left+w+2,top+h+2,0xFF9B794E);
        g.fill(left,top,left+w,top+h,0xFFF0DFC0);
        g.fill(left+4,top+4,left+w-4,top+22,0xFF382A36);
        g.drawCenteredString(font,title,width/2,top+9,0xFFFFE5AF);
        if(!data.getBoolean("Debug")&&!data.getBoolean("Lock")){
            g.fill(left+7,top+48,left+w/2-5,top+h-58,0xFFE5D1AB);
            g.fill(left+w/2+5,top+48,left+w-7,top+h-58,0xFFF7E9CF);
            g.fill(left+w/2-2,top+48,left+w/2+2,top+h-57,0xFFB59A73);
            g.fill(left+8,top+h-32,left+w-72,top+h-31,0xFFB59A73);
            if(tab.equals("Collection")&&entries().isEmpty()){
                int cx=left+3*w/4,cy=top+96;
                g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK),cx-8,cy);
                g.drawCenteredString(font,"YOUR INVESTIGATION",cx,cy+27,0xFF684754);
                g.drawWordWrap(font,Component.literal("Collect evidence. File it here to uncover connections and unlock new abilities."),left+w/2+20,cy+47,w/2-40,0xFF684F3F);
            }
        }
        if(data.getBoolean("Lock")){g.drawCenteredString(font,"Align each pin inside the gold band",width/2,top+60,0xFF483427);int bx=left+30,by=top+h/2,bw=w-60;g.fill(bx,by,bx+bw,by+20,0xFF342B30);double window=data.getDouble("Window");g.fill(bx+(int)(bw*(.5-window/2)),by,bx+(int)(bw*(.5+window/2)),by+20,0xFFBCA352);double t=Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime()+partial-data.getLong("Start");int cursor=bx+(int)(bw*(Math.sin(t*data.getDouble("Speed"))+1)/2);g.fill(cursor-2,by-5,cursor+2,by+25,0xFFF0ECE1);g.drawCenteredString(font,data.getInt("Completed")+" / "+data.getInt("Pins")+" pins",width/2,by+35,0xFF483427);super.render(g,mx,my,partial);return;}
        if(tab.equals("Collection")||tab.equals("Awareness")){var entries=entries();for(int row=0;row<rows();row++){int i=page*rows()+row;if(i>=entries.size())break;var s=entries.get(i);int y=top+76+row*22;if(selection.contains(s.id()))g.fill(left+8,y-2,left+w/2-6,y+18,0xFFBEA77C);g.drawString(font,(has("Journal",s.id())?"• ":"+ ")+s.name(),left+12,y+3,0xFF392A24,false);}
            if(entries.isEmpty())g.drawWordWrap(font,Component.literal("No evidence yet. Search the house's drawers and the buildings beyond the garden."),left+12,top+90,w/2-24,0xFF392A24);
            var s=SecretRegistry.ALL.get(selected);if(s!=null){int x=left+w/2+8,y=top+54;g.drawWordWrap(font,Component.literal(s.name()+"  ·  severity "+String.format(Locale.ROOT,"%.2f",s.severity())),x,y,w/2-22,0xFF392A24);y+=30;for(var line:font.split(Component.literal(s.description()),w/2-22)){g.drawString(font,line,x,y,0xFF493A32,false);y+=10;}y+=5;for(int i=0;i<6;i++)if(s.points()[i]>0){g.drawString(font,SecretRegistry.ATTRIBUTES[i]+" +"+String.format(Locale.ROOT,"%.2f",s.points()[i]/100.),x,y,0xFF5E3929,false);y+=11;}if(!s.ability().isEmpty())g.drawString(font,"Ability: "+s.ability(),x,y,0xFF5E3929,false);if(data.getBoolean("Debug")&&tab.equals("Awareness"))g.drawString(font,has("Exposed",s.id())?"CONFIRMED":has("Suspected",s.id())?"SUSPECTED":"UNKNOWN",x,y+14,0xFF8A292F,false);}
            int[] totals=data.getIntArray("Attributes");for(int i=0;i<Math.min(6,totals.length);i++){int x=left+8+i*(w-80)/6;g.drawString(font,new String[]{"Code","Find","Hide","Pattern","Weak","Gifts"}[i],x,top+h-28,0xFF493A32,false);g.drawString(font,String.format(Locale.ROOT,"%.2f",totals[i]/100.),x,top+h-17,0xFF493A32,false);}
        }else if(tab.equals("Connections")){int cx=left+w/3,cy=top+h/2+8,radius=Math.min(w/4-16,h/3-10);var secrets=new ArrayList<>(SecretRegistry.ALL.values());for(int i=0;i<secrets.size();i++){var s=secrets.get(i);double angle=i*Math.PI*2/secrets.size();int x=cx+(int)(Math.cos(angle)*radius),y=cy+(int)(Math.sin(angle)*radius);for(String id:s.connections()){int j=new ArrayList<>(SecretRegistry.ALL.keySet()).indexOf(id);double a=j*Math.PI*2/secrets.size();line(g,x,y,cx+(int)(Math.cos(a)*radius),cy+(int)(Math.sin(a)*radius),has("Journal",s.id())&&has("Journal",id)?0xFF9B343B:0xFFB6A581);}g.fill(x-3,y-3,x+3,y+3,has("Journal",s.id())?0xFF8A292F:0xFF8B806D);if(Math.abs(mx-x)<6&&Math.abs(my-y)<6)g.renderTooltip(font,Component.literal(has("Journal",s.id())?s.name():"Undiscovered evidence"),mx,my);}int complete=0,loopRow=0;for(var loop:SecretRegistry.LOOPS.values()){boolean done=loop.secrets().stream().allMatch(id->has("Journal",id));if(done)complete++;g.drawString(font,(done?"✓ ":"○ ")+loop.name(),left+w/2+24,top+67+loopRow++*19,done?0xFF8A292F:0xFF70685D,false);}g.drawCenteredString(font,complete+" / 6 loops complete",cx,top+h-38,0xFF493A32);
        }else if(tab.equals("Map")){if(!data.contains("Rooms"))g.drawCenteredString(font,"Structure map unlocks at Pattern Recognition 0.66",width/2,top+h/2,0xFF493A32);else{var player=BlockPos.of(data.getLong("Player"));int cx=left+w/2,cy=top+h/2;for(var tag:data.getList("Rooms",10)){var room=(CompoundTag)tag;var pos=BlockPos.of(room.getLong("Pos"));int x=cx+(pos.getX()-player.getX())/3,y=cy+(pos.getZ()-player.getZ())/3;if(x<left+8||x>left+w-8||y<top+50||y>top+h-30)continue;g.fill(x-3,y-3,x+3,y+3,new int[]{0xFF47724C,0xFF7E7540,0xFF82477E,0xFFA53838}[room.getInt("Tier")]);if(Math.abs(mx-x)<6&&Math.abs(my-y)<6)g.renderTooltip(font,Component.literal(room.getString("Name")+" · level "+room.getInt("Tier")),mx,my);}g.drawCenteredString(font,"You",cx,cy,0xFFFFFFFF);if(data.contains("Mother")){var m=BlockPos.of(data.getLong("Mother"));g.drawCenteredString(font,"M",cx+(m.getX()-player.getX())/3,cy+(m.getZ()-player.getZ())/3,0xFFFF3333);}}}
        else if(tab.equals("Mother")){int rh=Math.min(40,(h-104)/4),cw=(w-24)/2;for(int i=0;i<8;i++)g.drawString(font,i<2?"Relationship":(data.getDouble("Override"+(i-2))>=0?"Override":"Natural")+" · eff "+String.format(Locale.ROOT,"%.2f",data.getDouble("Effective"+(i-2))),left+12+(i/4)*cw,top+52+(i%4)*rh,0xFF493A32,false);}
        else if(tab.equals("Scold"))g.drawString(font,"Stage: "+data.getString("Conversation")+" · Proposal: "+(data.getBoolean("ProposalPending")?"yes":"no"),left+12,top+h-20,0xFF493A32,false);
        else if(tab.equals("AI / Spies"))g.drawString(font,data.getString("Search")+" · sight "+String.format(Locale.ROOT,"%.1f",data.getDouble("Sight")),left+12,top+h-36,0xFF493A32,false);
        super.render(g,mx,my,partial);
        if(!confirmation.isEmpty()){
            g.pose().pushPose();g.pose().translate(0,0,400);
            g.fill(left+12,top+48,left+w-12,top+h-28,0xFF352931);
            g.drawCenteredString(font,confirmation.equals("delete")?"Delete selected secrets?":"Sacrifice selected secrets?",width/2,top+57,0xFFFFD9AB);
            var lines=previewLines();int visible=Math.max(1,(h-148)/11);
            previewScroll=Math.max(0,Math.min(previewScroll,Math.max(0,lines.size()-visible)));
            for(int i=0;i<visible&&i+previewScroll<lines.size();i++)g.drawString(font,lines.get(i+previewScroll),left+24,top+75+i*11,0xFFF0D7B1,false);
            g.drawCenteredString(font,"Scroll to review · "+Math.min(lines.size(),previewScroll+visible)+" / "+lines.size(),width/2,top+h-67,0xFFBAA68F);
            g.drawCenteredString(font,"CONFIRM",left+w/3,top+h-47,0xFFFFB3AA);
            g.drawCenteredString(font,"CANCEL",left+2*w/3,top+h-47,0xFFF0D7B1);g.pose().popPose();
        }
    }
    private List<net.minecraft.util.FormattedCharSequence> previewLines(){
        var before=new LinkedHashSet<String>();for(var n:data.getList("Journal",8))before.add(n.getAsString());
        var chosen=Arrays.stream(ids().split(",")).filter(before::contains).toList();
        var after=new LinkedHashSet<>(before);after.removeAll(chosen);
        var text=new ArrayList<String>();
        text.add("Combined severity: "+String.format(Locale.ROOT,"%.2f",SecretRegistry.severity(chosen)));
        if(chosen.isEmpty())text.add("Select filed evidence first. Carried evidence cannot be sacrificed.");
        for(String id:chosen){var s=SecretRegistry.ALL.get(id);text.add(s.name()+" · severity "+String.format(Locale.ROOT,"%.2f",s.severity()));}
        var a=SecretRegistry.totals(before);var b=SecretRegistry.totals(after);
        for(int i=0;i<6;i++)if(a[i]!=b[i])text.add(SecretRegistry.ATTRIBUTES[i]+": "+String.format(Locale.ROOT,"%.2f → %.2f",a[i]/100.,b[i]/100.));
        var lost=SecretRegistry.abilities(before);lost.removeAll(SecretRegistry.abilities(after));
        for(String id:lost)text.add("Lose ability: "+abilityName(id));
        for(var loop:SecretRegistry.LOOPS.values())if(before.containsAll(loop.secrets())&&!after.containsAll(loop.secrets()))text.add("Break loop: "+loop.name());
        var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();for(String line:text)lines.addAll(font.split(Component.literal(line),w-48));return lines;
    }
    @Override public boolean mouseScrolled(double x,double y,double amount){if(!confirmation.isEmpty()){previewScroll=Math.max(0,previewScroll-(int)Math.signum(amount)*3);return true;}return super.mouseScrolled(x,y,amount);}
    private static void line(GuiGraphics g,int x,int y,int X,int Y,int color){int steps=Math.max(Math.abs(X-x),Math.abs(Y-y));for(int i=0;i<=steps;i++){int a=x+(X-x)*i/Math.max(1,steps),b=y+(Y-y)*i/Math.max(1,steps);g.fill(a,b,a+1,b+1,color);}}
}
