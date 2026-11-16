package com.coraline.tycoon;

import net.minecraft.nbt.*;

/** Bounded measurements from real committed ledger events, not invented profit. */
public final class TycoonFinance {
    public static void record(TycoonData.Home h,String category,long micros){
        if(micros==0)return;long minute=h.clock()/1200;var history=h.state.getList("FinancialHistory",10);CompoundTag bucket;
        if(history.isEmpty()||history.getCompound(history.size()-1).getLong("Minute")!=minute){bucket=new CompoundTag();bucket.putLong("Minute",minute);history.add(bucket);}else bucket=history.getCompound(history.size()-1);
        bucket.putLong(category,bucket.getLong(category)+micros);bucket.putLong(micros>0?"Income":"Expenses",bucket.getLong(micros>0?"Income":"Expenses")+Math.abs(micros));while(history.size()>120)history.remove(0);h.state.put("FinancialHistory",history);
    }
    public static CompoundTag view(TycoonData.Home h){
        var n=new CompoundTag();n.putString("Balance",TycoonSoulLedger.display(h));long rate=0;var farms=TycoonSoulFarms.farms(h);for(String key:farms.getAllKeys())rate+=farms.getCompound(key).getLong("RateMicros");n.putLong("ProjectedFarmIncomeMinute",rate*60);
        var history=h.state.getList("FinancialHistory",10);for(var value:history){var row=(CompoundTag)value;if(row.getLong("Minute")==h.clock()/1200-1){n.putLong("MeasuredIncomeMinute",row.getLong("Income"));n.putLong("MeasuredExpensesMinute",row.getLong("Expenses"));n.putLong("MeasuredNetMinute",row.getLong("Income")-row.getLong("Expenses"));n.putLong("MeasuredPayroll",-row.getLong("payroll"));n.putLong("MeasuredConverters",-row.getLong("converter"));n.putLong("MeasuredFood",-row.getLong("food")-row.getLong("cake"));}}
        int active=0,paused=0;long payroll=0;for(String id:TycoonWorkers.workers(h).getAllKeys()){var worker=TycoonWorkers.workers(h).getCompound(id);if(worker.getBoolean("Break")||!worker.getBoolean("Paid"))paused++;else{active++;payroll+=TycoonWorkers.salaryMicros(worker);}}n.putInt("ActiveWorkers",active);n.putInt("PausedWorkers",paused);n.putLong("ProjectedPayrollMinute",payroll);
        int machines=0,stopped=0;for(String id:TycoonMachines.machines(h).getAllKeys()){var row=TycoonMachines.machines(h).getCompound(id);if(row.getBoolean("Break")||!row.getString("Status").equals("WORKING"))stopped++;else machines++;}n.putInt("ActiveMachines",machines);n.putInt("PausedMachines",stopped);n.put("History",history.copy());n.putLong("Reserve",TycoonPolicies.number(h,"reserve"));return n;
    }
    private TycoonFinance(){}
}
