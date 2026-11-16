package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Explicit persisted permissions; hiring is not permission for discretionary spending. */
public final class TycoonPolicies {
    public static final Map<String,long[]> NUMBERS=new LinkedHashMap<>();
    public static final Map<String,List<String>> CHOICES=Map.of("CandidatePriority",List.of("Balanced selection","Highest Worker Aptitude","Most promising personality","Lowest placement fee","Best expected Soul Production","Random selection"),"PreferredBranch",List.of("Balanced","First","Second","Third"),"PreferredSoulSpecialization",List.of("Any","Explorer","Social","Artistic","Spectacle","Playful","Culinary","Independent","Restless"));
    public static final Set<String> FLAGS=new LinkedHashSet<>(List.of("placements","dolls","invitations","conversations","assignments","gifts","cakes","proposals","farmPurchases","farmUpgrades","construction","attractionUpgrades","houseUpgrades","skyUpgrades","terrainUpgrades","repairs","autoRelease","keepRareSouls","investing","useProducedSupplies","salaryDowngrade","reinvest"));
    static{NUMBERS.put("minimumBaseProductionMilli",new long[]{0,0,1000});NUMBERS.put("minimumStabilityPercent",new long[]{0,0,100});NUMBERS.put("preferredFoodQuality",new long[]{4,0,6});NUMBERS.put("maximumVisitorSuspicionPercent",new long[]{90,0,100});NUMBERS.put("expansionDirection",new long[]{0,0,4});}
    static{NUMBERS.put("neglectMin",new long[]{50,0,74});NUMBERS.put("neglectMax",new long[]{70,0,74});NUMBERS.put("minHunger",new long[]{45,25,90});NUMBERS.put("minThirst",new long[]{45,25,90});NUMBERS.put("maxResidents",new long[]{8,1,64});NUMBERS.put("maxVisitors",new long[]{6,1,64});NUMBERS.put("reserve",new long[]{10,0,100000});NUMBERS.put("payrollPerMinute",new long[]{30,0,10000});NUMBERS.put("foodBudgetHour",new long[]{0,0,10000});NUMBERS.put("giftBudgetHour",new long[]{0,0,10000});NUMBERS.put("constructionPowerHour",new long[]{0,0,10000});NUMBERS.put("farmSoulBudgetHour",new long[]{0,0,10000});NUMBERS.put("converterBudgetHour",new long[]{60,0,10000});NUMBERS.put("investmentLimit",new long[]{0,0,100000});NUMBERS.put("investmentPercent",new long[]{10,0,80});NUMBERS.put("companyExposurePercent",new long[]{25,1,100});NUMBERS.put("tradeCooldown",new long[]{1200,200,24000});NUMBERS.put("stopLossPercent",new long[]{15,1,90});NUMBERS.put("minSoulQualityPercent",new long[]{0,0,100});NUMBERS.put("maxRestlessnessPercent",new long[]{100,0,100});NUMBERS.put("desiredGoodness",new long[]{60,0,100});}
    public static CompoundTag state(TycoonData.Home h){var n=h.state.getCompound("AutomationPolicies");for(var e:NUMBERS.entrySet())if(!n.contains(e.getKey()))n.putLong(e.getKey(),e.getValue()[0]);for(var e:CHOICES.entrySet())if(!n.contains(e.getKey()))n.putString(e.getKey(),e.getValue().get(0));if(!n.contains("Strategy"))n.putString("Strategy","Balanced Operation");if(!n.contains("Risk"))n.putString("Risk","Conservative");h.state.put("AutomationPolicies",n);return n;}
    public static boolean allowed(TycoonData.Home h,String key){return state(h).getBoolean(key);}
    public static long number(TycoonData.Home h,String key){return state(h).getLong(key);}
    public static String set(ServerPlayer p,String key,String value){
        var h=TycoonWorld.home(p);var n=state(h);
        if(FLAGS.contains(key))n.putBoolean(key,Boolean.parseBoolean(value));
        else if(CHOICES.containsKey(key)){if(!CHOICES.get(key).contains(value))return "Unknown policy choice.";n.putString(key,value);}
        else if(NUMBERS.containsKey(key)){long v=Long.parseLong(value);var bounds=NUMBERS.get(key);if(v<bounds[1]||v>bounds[2])return "Policy value outside allowed range.";if(key.equals("neglectMin")&&v>n.getLong("neglectMax")||key.equals("neglectMax")&&v<n.getLong("neglectMin"))return "Minimum Neglect must not exceed maximum.";n.putLong(key,v);}
        else if(key.equals("Strategy")&&List.of("Maximum Profit","Maximum Growth","Maximum Safety","Balanced Operation","Custom").contains(value))n.putString(key,value);
        else if(key.equals("Risk")&&List.of("Conservative","Balanced","Aggressive","Custom").contains(value))n.putString(key,value);
        else if(key.startsWith("allow_structure_")){TycoonAttractionType.parse(key.substring(16));n.putBoolean(key,Boolean.parseBoolean(value));}
        else if(key.startsWith("reserve_tile_")){String[] parts=key.substring(13).split(",");if(parts.length!=2||!TycoonLandscape.inBounds(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])))return "Invalid reserved terrain.";n.putBoolean(key,Boolean.parseBoolean(value));}
        else if(key.startsWith("allow_company_")&&TycoonMarket.company(key.substring(14))!=null)n.putBoolean(key,Boolean.parseBoolean(value));
        else return "Unknown policy.";
        TycoonWorld.dirty(p);return "Policy saved: "+key+" = "+value;
    }
    public static boolean budget(TycoonData.Home h,String category,long amount,long limit){var budget=h.state.getCompound("AutomationSpending");var n=budget.getCompound(category);return amount>=0&&(h.clock()/72000!=n.getLong("Hour")?0:n.getLong("Spent"))<=limit-amount;}
    public static void spent(TycoonData.Home h,String category,long amount){var budgets=h.state.getCompound("AutomationSpending");var n=budgets.getCompound(category);if(n.getLong("Hour")!=h.clock()/72000){n.putLong("Hour",h.clock()/72000);n.putLong("Spent",0);}n.putLong("Spent",n.getLong("Spent")+amount);budgets.put(category,n);h.state.put("AutomationSpending",budgets);}
    private TycoonPolicies(){}
}
