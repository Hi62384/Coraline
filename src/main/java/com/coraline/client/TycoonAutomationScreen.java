package com.coraline.client;

import java.util.*;
import com.coraline.tycoon.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;

/** Owner's existing desk session, with paginated controls and actual market data. */
public final class TycoonAutomationScreen extends Screen {
    private CompoundTag data;private String tab="Workers";private int page,plotX,plotZ,recipe,range=30,left,top,w,h;private boolean hiring=true,supplies;
    private EditBox value;private TycoonWorker preview;
    private final Map<AbstractWidget,Integer> contentWidgets=new IdentityHashMap<>();private boolean buildingContent;private int scroll,maxScroll;
    public TycoonAutomationScreen(CompoundTag data){super(Component.literal("Tycoon Automation"));this.data=data;}
    public static void receive(CompoundTag data){var mc=Minecraft.getInstance();if(mc.screen instanceof TycoonAutomationScreen s){s.data=data;s.rebuildWidgets();}}
    @Override public boolean isPauseScreen(){return false;}
    private void action(String value){TycoonNetwork.action(data.getUUID("Nonce"),"tycoon2",value);}
    private Button button(String label,int x,int y,int width,Runnable action){var b=Button.builder(Component.literal(label),unused->action.run()).bounds(x,y,width,20).build();addRenderableWidget(b);if(buildingContent)contentWidgets.put(b,y);return b;}
    private void content(AbstractWidget widget){addRenderableWidget(widget);contentWidgets.put(widget,widget.getY());}
    private void layoutScroll(){int bottom=top+300;for(var entry:contentWidgets.entrySet())bottom=Math.max(bottom,entry.getValue()+entry.getKey().getHeight());maxScroll=Math.max(0,bottom-(top+h-56));scroll=Math.max(0,Math.min(maxScroll,scroll));for(var entry:contentWidgets.entrySet()){var widget=entry.getKey();widget.setY(entry.getValue()-scroll);widget.visible=widget.getY()>=top+55&&widget.getY()+widget.getHeight()<=top+h-56;}}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(x>=left&&x<=left+w&&y>=top+55&&y<=top+h-56){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*24)));layoutScroll();return true;}return super.mouseScrolled(x,y,delta);}
    private ListTag rows(String key){return data.getList(key,10);}
    private CompoundTag selected(String key){var list=rows(key);page=Math.min(page,Math.max(0,list.size()-1));return list.isEmpty()?new CompoundTag():list.getCompound(page);}
    private String setting(CompoundTag row,String key,String value){return "worker_set:"+row.getUUID("Id")+","+key+","+value;}
    private void pages(int size){if(size<=1)return;boolean prior=buildingContent;buildingContent=false;button("<",left+w/2-42,top+h-28,30,()->{page=Math.max(0,page-1);preview=null;rebuildWidgets();});button(">",left+w/2+12,top+h-28,30,()->{page=Math.min(size-1,page+1);preview=null;rebuildWidgets();});buildingContent=prior;}
    @Override protected void init(){
        w=Math.min(720,width-16);h=Math.min(420,height-16);left=(width-w)/2;top=(height-h)/2;buildingContent=false;contentWidgets.clear();
        String[] tabs={"Workers","Machines","Resources","Policies","Finance","Market"};for(int i=0;i<tabs.length;i++){String name=tabs[i];String label=w<480&&name.equals("Resources")?"Stock":w<480&&name.equals("Workers")?"Staff":name;button(label,left+8+i*(w-16)/tabs.length,top+27,(w-22)/tabs.length,()->{tab=name;page=0;scroll=0;preview=null;rebuildWidgets();});}
        button("Refresh",left+10,top+h-28,65,()->TycoonNetwork.action(data.getUUID("Nonce"),"refresh",""));button("World Desk",left+w-100,top+h-28,90,()->TycoonWorldScreen.show(data));
        buildingContent=true;if(tab.equals("Workers"))workers();if(tab.equals("Machines"))machines();if(tab.equals("Resources"))resources();if(tab.equals("Policies"))policies();if(tab.equals("Market"))market();buildingContent=false;layoutScroll();
    }
    private void workers(){
        button(hiring?"Manage hired workers":"Hire workers",left+12,top+57,w-24,()->{hiring=!hiring;page=0;preview=null;rebuildWidgets();});var row=selected(hiring?"WorkerCatalog":"Workers");if(row.isEmpty())return;
        if(hiring){button("Hire — "+row.getInt("Price")+" Power (starts on break)",left+12,top+136,w-24,()->action("worker_hire:"+row.getString("Id")));pages(rows("WorkerCatalog").size());return;}
        String id=row.getUUID("Id").toString();int half=(w-32)/2;
        button("Unlock next level — "+(row.getInt("Unlocked")+1)*12+" Power",left+12,top+177,half,()->action("worker_upgrade:"+id));button("Operating Level: "+row.getInt("Operating"),left+20+half,top+177,half,()->action(setting(row,"level",Integer.toString(row.getInt("Operating")%Math.max(1,row.getInt("Unlocked"))+1))));
        button(row.getBoolean("Break")?"Resume Work":"Put on Break",left+12,top+202,half,()->action(setting(row,"break",Boolean.toString(!row.getBoolean("Break")))));button("Set working area where I stand",left+20+half,top+202,half,()->action(setting(row,"area","here")));
        String[] zones={"Central House","Attraction District","Soul Farm District","Restricted Facilities","General Terrain","Orphanage"};button("Zone: "+row.getString("Zone"),left+12,top+227,w-24,()->{int i=Arrays.asList(zones).indexOf(row.getString("Zone"));action(setting(row,"zone",zones[(i+1)%zones.length]));});pages(rows("Workers").size());
        button("Job priority: "+row.getInt("Priority"),left+12,top+252,w-24,()->action(setting(row,"priority",Integer.toString((row.getInt("Priority")+10)%110))));
    }
    private void machines(){
        int half=(w-32)/2;button("Build Converter — 32 Power",left+12,top+59,half,()->action("machine_build:converter,"+plotX+","+plotZ));button("Build Extractor — 40 Power",left+20+half,top+59,half,()->action("machine_build:extractor,"+plotX+","+plotZ));
        button("Plot X: "+plotX,left+12,top+84,half,()->{plotX=plotX==7?-8:plotX+1;rebuildWidgets();});button("Plot Z: "+plotZ,left+20+half,top+84,half,()->{plotZ=plotZ==7?-8:plotZ+1;rebuildWidgets();});
        var row=selected("Machines");if(row.isEmpty())return;String id=row.getUUID("Id").toString();
        button("Unlock next level — "+(row.getInt("Unlocked")+1)*12+" Power",left+12,top+182,half,()->action("machine_upgrade:"+id));button("Operating Level: "+row.getInt("Operating"),left+20+half,top+182,half,()->action("machine_set:"+id+",level,"+(row.getInt("Operating")%Math.max(1,row.getInt("Unlocked"))+1)));
        button(row.getBoolean("Break")?"Resume machine":"Put on Break",left+12,top+207,w-24,()->action("machine_set:"+id+",break,"+!row.getBoolean("Break")));
        button("Repair missing machine blocks",left+12,top+282,w-24,()->action("machine_repair:"+id));
        var recipes=rows("ConverterRecipes");if(row.getString("Type").equals("converter")&&!recipes.isEmpty()){recipe=Math.min(recipe,recipes.size()-1);button("Select output: "+recipes.getCompound(recipe).getString("Id"),left+12,top+232,w-24,()->{recipe=(recipe+1)%recipes.size();rebuildWidgets();});button("Apply recipe",left+12,top+257,w-24,()->action("machine_set:"+id+",recipe,"+recipes.getCompound(recipe).getString("Id")));}pages(rows("Machines").size());
    }
    private void resources(){
        button(supplies?"Produced Resources":"Automation Supplies",left+12,top+59,w-24,()->{supplies=!supplies;page=0;rebuildWidgets();});var row=selected(supplies?"Warehouse":"ProducedResources");int half=(w-32)/2;
        if(supplies){button("Deposit held stack",left+12,top+154,half,()->action("supply_deposit:64"));button("Expand storage",left+20+half,top+154,half,()->action("storage_upgrade:"));}
        if(!row.isEmpty())button("Collect stack that fits",left+12,top+184,w-24,()->action((supplies?"supply_collect:":"resource_collect:")+row.getString("Id")));pages(rows(supplies?"Warehouse":"ProducedResources").size());
        if(!row.isEmpty())button("Collect selected — all that fits",left+12,top+209,w-24,()->action((supplies?"supply_collect_many:":"resource_collect_many:")+row.getString("Id")));
        button("Collect all resources that fit",left+12,top+234,w-24,()->action(supplies?"supply_collect_many:":"resource_collect_many:"));
    }
    private void policies(){
        var list=rows("PolicyControls");var row=selected("PolicyControls");if(row.isEmpty())return;String key=row.getString("Key");var policies=data.getCompound("Policies");
        if(row.getBoolean("Flag"))button(policies.getBoolean(key)?"Enabled — click to disable":"Disabled — click to authorize",left+12,top+134,w-24,()->action("policy:"+key+"="+!policies.getBoolean(key)));
        else if(row.contains("Choices")){var choices=row.getList("Choices",8);button(policies.getString(key),left+12,top+134,w-24,()->{int selected=0;for(int i=0;i<choices.size();i++)if(choices.getString(i).equals(policies.getString(key)))selected=i;action("policy:"+key+"="+choices.getString((selected+1)%choices.size()));});}
        else{value=new EditBox(font,left+12,top+134,(w-32)/2,20,Component.literal("Policy value"));value.setMaxLength(12);value.setValue(Long.toString(policies.getLong(key)));content(value);button("Save value",left+20+(w-32)/2,top+134,(w-32)/2,()->action("policy:"+key+"="+value.getValue()));}
        String[] modes={"Maximum Profit","Maximum Growth","Maximum Safety","Balanced Operation","Custom"};button("Master strategy: "+policies.getString("Strategy"),left+12,top+164,w-24,()->action("policy:Strategy="+modes[(Arrays.asList(modes).indexOf(policies.getString("Strategy"))+1)%modes.length]));
        String[] risks={"Conservative","Balanced","Aggressive","Custom"};button("Investment strategy: "+policies.getString("Risk"),left+12,top+189,w-24,()->action("policy:Risk="+risks[(Arrays.asList(risks).indexOf(policies.getString("Risk"))+1)%risks.length]));
        button("Reserved plot X: "+plotX,left+12,top+214,(w-32)/2,()->{plotX=plotX==7?-8:plotX+1;rebuildWidgets();});button("Z: "+plotZ,left+20+(w-32)/2,top+214,(w-32)/2,()->{plotZ=plotZ==7?-8:plotZ+1;rebuildWidgets();});String plot="reserve_tile_"+plotX+","+plotZ;button(policies.getBoolean(plot)?"Unreserve this terrain tile":"Reserve this tile from automated construction",left+12,top+239,w-24,()->action("policy:"+plot+"="+!policies.getBoolean(plot)));pages(list.size());
    }
    private void market(){
        var market=data.getCompound("Market");var list=market.getList("Companies",10);if(list.isEmpty())return;page=Math.min(page,list.size()-1);var row=list.getCompound(page);
        button("Chart range: "+range+" minutes",left+12,top+247,w-24,()->{range=range==30?120:range==120?240:30;rebuildWidgets();});value=new EditBox(font,left+12,top+272,80,20,Component.literal("Share quantity"));value.setValue("1");value.setMaxLength(5);content(value);
        button("Buy shares",left+100,top+272,(w-124)/2,()->trade(row,"buy"));button("Sell shares",left+108+(w-124)/2,top+272,(w-124)/2,()->trade(row,"sell"));pages(list.size());
    }
    private void trade(CompoundTag company,String side){action("trade:"+company.getString("Id")+","+value.getValue()+","+side+","+data.getCompound("Market").getLong("Revision")+","+UUID.randomUUID());}
    private void line(GuiGraphics g,String text,int y,int color){int at=top+y-scroll;if(at>=top+55&&at+font.lineHeight<=top+h-56)g.drawString(font,font.plainSubstrByWidth(text,w-24),left+12,at,color);}
    private static String money(long micros){return String.format(Locale.ROOT,"%.3f",micros/1_000_000.);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(left,top,left+w,top+h,0xF0181424);g.drawCenteredString(font,title,left+w/2,top+9,0xF1D9AC);
        if(tab.equals("Workers")){var row=selected(hiring?"WorkerCatalog":"Workers");if(row.isEmpty())line(g,"No hired workers yet.",91,0xD8CBDD);else if(hiring){line(g,row.getString("Name"),91,0xF1D9AC);line(g,"Hire once, unlock five levels. Work starts only after Resume Work.",108,0xD8CBDD);}else{line(g,TycoonWorkerType.parse(row.getString("Type")).label+" — unlocked "+row.getInt("Unlocked")+", operating "+row.getInt("Operating"),89,0xF1D9AC);line(g,"Task: "+(row.contains("TaskLabel")?row.getString("TaskLabel"):row.getString("Task").replace('_',' ').toLowerCase(java.util.Locale.ROOT)),106,0xD8CBDD);line(g,"Salary "+money(row.getLong("SalaryPerMinute"))+" Essence/min | Capacity "+row.getInt("Capacity")+" | Completed "+row.getLong("CompletedJobs"),123,0xD8CBDD);line(g,"Blocker: "+row.getString("Blocker"),140,0xFFB8AA);line(g,"Work area "+net.minecraft.core.BlockPos.of(row.getLong("Area")).toShortString()+" | Paid: "+row.getBoolean("Paid"),157,0xC5B2D8);}}
        if(tab.equals("Machines")){var row=selected("Machines");if(row.isEmpty())line(g,"Select a supported owned plot, clear the footprint, then build.",122,0xD8CBDD);else{line(g,row.getString("Type")+" — "+row.getString("Status"),120,0xF1D9AC);line(g,"Unlocked "+row.getInt("Unlocked")+" | operating "+row.getInt("Operating")+" | progress "+row.getInt("Progress")+"/1200",137,0xD8CBDD);line(g,"Output "+row.getString("Recipe")+" | produced "+row.getLong("Produced"),154,0xC5B2D8);}}
        if(tab.equals("Resources")){var row=selected(supplies?"Warehouse":"ProducedResources");line(g,(supplies?"Supplies "+data.getLong("SupplyUsed"):"Produced "+data.getLong("ProducedUsed"))+" / "+data.getInt("SupplyCapacity")+" items",91,0xF1D9AC);if(!row.isEmpty()){line(g,row.getString("Id"),111,0xD8CBDD);line(g,"Stored: "+row.getLong("Count"),128,0xC5B2D8);var id=net.minecraft.resources.ResourceLocation.tryParse(row.getString("Id"));var item=id==null?null:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);if(item!=null){g.enableScissor(left+8,top+55,left+w-8,top+h-56);g.renderItem(new net.minecraft.world.item.ItemStack(item),left+w-44,top+111-scroll);g.disableScissor();}}else line(g,"No stored items. Nothing is produced or supplied for free.",111,0xD8CBDD);}
        if(tab.equals("Policies")){var row=selected("PolicyControls");if(!row.isEmpty()){line(g,row.getString("Key").replaceAll("([a-z])([A-Z])","$1 $2").replace('_',' '),91,0xF1D9AC);line(g,row.getBoolean("Flag")?"Permission is opt-in. Enabling this allows its normal authorized gameplay.":row.contains("Choices")?"Choose an explicit policy; only permitted resources and known information are used.":"Allowed range: "+row.getLong("Min")+"–"+row.getLong("Max"),110,0xD8CBDD);}}
        if(tab.equals("Finance")){var f=data.getCompound("Finances");line(g,"Balance: "+f.getString("Balance")+" Essence | Reserve: "+f.getLong("Reserve"),67,0xF1D9AC);line(g,"Projected farm income: "+money(f.getLong("ProjectedFarmIncomeMinute"))+" / minute",89,0xB9E7FF);line(g,"Last completed active minute — measured",119,0xF1D9AC);line(g,"Income: "+money(f.getLong("MeasuredIncomeMinute")),139,0xD8CBDD);line(g,"Expenses: "+money(f.getLong("MeasuredExpensesMinute")),159,0xD8CBDD);line(g,"Net: "+money(f.getLong("MeasuredNetMinute")),179,0xD8CBDD);line(g,"Fictional stock portfolio: "+money(data.getCompound("Market").getLong("Portfolio")),209,0xC5B2D8);line(g,"Source: actual committed cash flows, not guaranteed future profits.",231,0xC5B2D8);}
        if(tab.equals("Market"))chart(g,mx,my);
        if(tab.equals("Finance")){var f=data.getCompound("Finances");line(g,"Workers active / paused: "+f.getInt("ActiveWorkers")+" / "+f.getInt("PausedWorkers")+" | Machines: "+f.getInt("ActiveMachines")+" / "+f.getInt("PausedMachines"),253,0xC5B2D8);line(g,"Measured payroll "+money(f.getLong("MeasuredPayroll"))+" | converters "+money(f.getLong("MeasuredConverters"))+" | food/gifts "+money(f.getLong("MeasuredFood")),273,0xC5B2D8);}
        if(tab.equals("Workers")&&minecraft.level!=null&&minecraft.player!=null){var row=selected(hiring?"WorkerCatalog":"Workers");if(!row.isEmpty()){
            var type=TycoonWorkerType.parse(row.getString(hiring?"Id":"Type"));int rank=hiring?1:row.getInt("Operating");
            if(preview==null||preview.role()!=type){preview=TycoonAutomationContent.WORKER.get().create(minecraft.level);if(preview!=null)preview.configure(minecraft.player.getUUID(),hiring?UUID.nameUUIDFromBytes(type.id().getBytes(java.nio.charset.StandardCharsets.UTF_8)):row.getUUID("Id"),type,rank);}
            if(preview!=null){preview.presentation(rank,0);g.enableScissor(left+8,top+55,left+w-8,top+h-56);net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g,left+w-50,top+(hiring?134:174)-scroll,hiring?24:18,left+w-50-mx,top+110-my-scroll,preview);g.disableScissor();}
        }}
        if(maxScroll>0){int area=h-111;int size=Math.max(8,area*area/(area+maxScroll));int y=top+55+(area-size)*scroll/Math.max(1,maxScroll);g.fill(left+w-5,y,left+w-3,y+size,0xFFB1A1C2);}
        if(!data.getString("Result").isBlank())g.drawString(font,font.plainSubstrByWidth(data.getString("Result"),w-24),left+12,top+h-51,0xE8C581);super.render(g,mx,my,partial);
    }
    private void chart(GuiGraphics g,int mx,int my){
        var list=data.getCompound("Market").getList("Companies",10);if(list.isEmpty())return;var row=list.getCompound(Math.min(page,list.size()-1));line(g,row.getString("Name")+" — "+row.getString("Industry"),60,0xF1D9AC);line(g,"Price "+money(row.getLong("Price"))+" Essence | Owned "+row.getLong("Quantity")+" | 1% fee",77,0xD8CBDD);line(g,"Fictional in-game exchange. Prices can fall.",94,0xFFB8AA);
        var prices=row.getLongArray("History");if(prices.length==0)return;int start=Math.max(0,prices.length-range),length=prices.length-start,x0=left+16,y0=top+119-scroll,cw=w-32,ch=91;long min=Long.MAX_VALUE,max=0;for(int i=start;i<prices.length;i++){min=Math.min(min,prices[i]);max=Math.max(max,prices[i]);}g.enableScissor(left+8,top+55,left+w-8,top+h-56);g.fill(x0,y0,x0+cw,y0+ch,0xFF100E18);long span=Math.max(1,max-min);int previousY=0;
        for(int i=0;i<length;i++){int x=x0+(length<=1?cw/2:i*(cw-1)/(length-1)),y=y0+ch-3-(int)((prices[start+i]-min)*(ch-6)/span);if(i>0){int previousX=x0+(i-1)*(cw-1)/Math.max(1,length-1);g.fill(previousX,previousY,x+1,previousY+1,0xFF7CD8BB);g.fill(x,Math.min(y,previousY),x+1,Math.max(y,previousY)+1,0xFF7CD8BB);}else g.fill(x,y,x+2,y+2,0xFF7CD8BB);previousY=y;}
        if(mx>=x0&&mx<x0+cw&&my>=y0&&my<y0+ch){int index=Math.max(0,Math.min(length-1,(mx-x0)*Math.max(1,length-1)/cw));g.fill(mx,y0,mx+1,y0+ch,0xFF8D859E);line(g,"Recorded price "+money(prices[start+index])+" — "+(length-index-1)+" active market minutes ago",216,0xE8C581);}else line(g,row.getString("Event"),216,0xC5B2D8);g.disableScissor();
        long first=prices[start];line(g,String.format(Locale.ROOT,"Range change %+.2f%%",100*(row.getLong("Price")-first)/(double)Math.max(1,first)),232,0xC5B2D8);
    }
}
