package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Entirely fictional server-wide exchange. No external prices or real money. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonMarket extends SavedData {
    public record Company(String id,String name,String industry,long base,double volatility){}
    public static final List<Company> COMPANIES=List.of(new Company("buttons","ButtonWorks Ltd.","Buttons",8_000_000,.035),new Company("moonberry","Moonberry Foods","Food",5_000_000,.025),new Company("entertainment","Otherworld Entertainment","Attractions",12_000_000,.05),new Company("thread","Dream Thread Industries","Textiles",10_000_000,.03),new Company("blackcat","Black Cat Holdings","Diversified",7_000_000,.045),new Company("voidstone","Voidstone Extraction","Materials",9_000_000,.04),new Company("carnival","Grand Carnival Corporation","Performance",15_000_000,.065),new Company("needle","Silver Needle Manufacturing","Machinery",11_000_000,.03));
    private CompoundTag state=new CompoundTag();
    public static Company company(String id){return COMPANIES.stream().filter(c->c.id.equals(id)).findFirst().orElse(null);}
    public static TycoonMarket get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TycoonMarket::load,TycoonMarket::new,"coraline_tycoon_market");}
    public TycoonMarket(){state.putLong("Seed",new java.security.SecureRandom().nextLong());for(var c:COMPANIES){var n=new CompoundTag();n.putLong("Price",c.base);n.putLongArray("History",new long[]{c.base});state.put(c.id,n);}}
    private static TycoonMarket load(CompoundTag n){var result=new TycoonMarket();result.state=n.getCompound("Market").copy();return result;}
    @Override public CompoundTag save(CompoundTag n){n.put("Market",state.copy());return n;}
    public long revision(){return state.getLong("Revision");}
    public long price(String id){return Math.max(250_000,state.getCompound(id).getLong("Price"));}
    public long[] history(String id){return state.getCompound(id).getLongArray("History");}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;var market=get(server);long now=server.overworld().getGameTime();if(!market.state.contains("LastTick")){market.state.putLong("LastTick",now);market.setDirty();return;}if(now-market.state.getLong("LastTick")<1200)return;market.state.putLong("LastTick",now);market.advance();}
    public void advance(){
        long revision=revision()+1;state.putLong("Revision",revision);var random=new Random(state.getLong("Seed")^Long.rotateLeft(revision*0x9E3779B97F4A7C15L,17));double marketMove=random.nextGaussian()*.008;
        for(var c:COMPANIES){var row=state.getCompound(c.id);long old=price(c.id);double trend=row.getDouble("Trend")*.8+random.nextGaussian()*.003,reversion=(c.base-old)/(double)c.base*.008,event=random.nextInt(40)==0?(random.nextBoolean()?1:-1)*(.025+random.nextDouble()*.05):0;double delta=Math.max(-.12,Math.min(.12,marketMove+trend+reversion+random.nextGaussian()*c.volatility+event));long next=Math.max(250_000,Math.min(1_000_000_000L,Math.round(old*(1+delta)/1000)*1000));row.putLong("Price",next);row.putDouble("Trend",trend);row.putString("Event",event>0?"Strong fictional company results":event<0?"Fictional supply disruption":"Routine trading");var before=row.getLongArray("History");var after=new long[Math.min(240,before.length+1)];int keep=after.length-1;System.arraycopy(before,Math.max(0,before.length-keep),after,0,keep);after[keep]=next;row.putLongArray("History",after);state.put(c.id,row);
        }setDirty();
    }
    public static long portfolio(TycoonData.Home h,TycoonMarket market){long total=0;var holdings=h.state.getCompound("Shares");for(var c:COMPANIES)total+=Math.max(0,holdings.getCompound(c.id).getLong("Quantity"))*market.price(c.id);return total;}
    public static String trade(ServerPlayer p,String company,int quantity,boolean buy,long quote,UUID receipt,boolean automated){
        var h=TycoonWorld.home(p);var market=get(p.server);var c=company(company);if(c==null||quantity<1||quantity>10000||quote!=market.revision())return "Invalid order or stale quote. Refresh prices.";
        var receipts=h.state.getLong("TradeReceiptRevision")==quote?h.state.getList("TradeReceipts",8):new ListTag();for(var entry:receipts)if(entry.getAsString().equals(receipt.toString()))return "Order already executed.";if(receipts.size()>=512)return "Order limit for this quote reached. Wait for the next market minute.";
        var holdings=h.state.getCompound("Shares");var position=holdings.getCompound(company);long existing=position.getLong("Quantity"),gross=Math.multiplyExact(market.price(company),(long)quantity),fee=Math.max(1000,gross/100),cost=buy?gross+fee:gross-fee;
        if(buy){
            if(existing>1_000_000-quantity)return "Share holding limit reached.";
            if(automated){long limit=TycoonPolicies.number(h,"investmentLimit")*TycoonSoulLedger.SCALE,portfolio=portfolio(h,market),cash=Math.min(1_000_000_000_000L,h.state.getLong("SoulEssence"))*TycoonSoulLedger.SCALE;long percent=TycoonPolicies.number(h,"investmentPercent"),exposure=TycoonPolicies.number(h,"companyExposurePercent");if(!TycoonPolicies.allowed(h,"investing")||!TycoonPolicies.state(h).getBoolean("allow_company_"+company)||portfolio+cost>limit||portfolio+cost>(cash+portfolio)/100*percent||existing*market.price(company)+cost>Math.max(gross,(cash+portfolio)/100*exposure))return "Investment allocation or company exposure limit reached.";}
            if(!TycoonSoulLedger.debit(h,cost,automated?TycoonPolicies.number(h,"reserve"):0))return "Insufficient available Soul Essence.";position.putLong("Quantity",existing+quantity);position.putLong("CostBasis",position.getLong("CostBasis")+cost);
        }else{if(existing<quantity)return "Not enough owned shares.";if(!TycoonSoulLedger.canCredit(h,cost))return "Soul Essence balance is at its storage limit; no shares sold.";long totalBasis=position.getLong("CostBasis"),basis=totalBasis/existing*quantity+(totalBasis%existing)*quantity/existing;position.putLong("CostBasis",Math.max(0,totalBasis-basis));position.putLong("Quantity",existing-quantity);TycoonSoulLedger.creditMicros(h,cost);h.state.putLong("RealizedProfitMicros",h.state.getLong("RealizedProfitMicros")+cost-basis);}
        holdings.put(company,position);h.state.put("Shares",holdings);receipts.add(StringTag.valueOf(receipt.toString()));h.state.putLong("TradeReceiptRevision",quote);h.state.put("TradeReceipts",receipts);var trades=h.state.getList("TradeHistory",10);var row=new CompoundTag();row.putUUID("Receipt",receipt);row.putString("Company",company);row.putBoolean("Buy",buy);row.putInt("Quantity",quantity);row.putLong("Price",market.price(company));row.putLong("Fee",fee);row.putLong("At",h.clock());trades.add(row);while(trades.size()>128)trades.remove(0);h.state.put("TradeHistory",trades);TycoonFinance.record(h,buy?"shares_bought":"shares_sold",buy?-cost:cost);TycoonWorld.dirty(p);return (buy?"Bought ":"Sold ")+quantity+" shares in "+c.name+". Trading fee paid.";
    }
    public static CompoundTag view(ServerPlayer p){var h=TycoonWorld.home(p);var market=get(p.server);var n=new CompoundTag();n.putLong("Revision",market.revision());n.putString("Disclaimer","Fictional in-game exchange. Prices can fall; no strategy guarantees profits.");var list=new ListTag();for(var c:COMPANIES){var row=new CompoundTag();row.putString("Id",c.id);row.putString("Name",c.name);row.putString("Industry",c.industry);row.putLong("Price",market.price(c.id));row.putDouble("Volatility",c.volatility);row.putLongArray("History",market.history(c.id));row.putString("Event",market.state.getCompound(c.id).getString("Event"));row.putLong("Quantity",h.state.getCompound("Shares").getCompound(c.id).getLong("Quantity"));row.putLong("CostBasis",h.state.getCompound("Shares").getCompound(c.id).getLong("CostBasis"));list.add(row);}n.put("Companies",list);n.putLong("Portfolio",portfolio(h,market));n.putLong("RealizedProfit",h.state.getLong("RealizedProfitMicros"));n.put("Trades",h.state.getList("TradeHistory",10).copy());return n;}
}
