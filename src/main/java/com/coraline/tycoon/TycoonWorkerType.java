package com.coraline.tycoon;

import java.util.Locale;

public enum TycoonWorkerType {
    OVERWORLD_MAID("Overworld Maid",true,20,1),OVERWORLD_CHEF("Overworld Chef",true,24,1),ORPHAN_BUYER("Orphan Buyer",true,28,.8),DOLL_MAKER("Doll Maker",true,24,1),COMPLIANCE("Agency Compliance Manager",true,24,.8),
    OTHER_MANAGER("Other World Manager Maid",false,32,1.2),OTHER_CHEF("Other World Chef",false,24,1),GUARD("World Guard",false,28,1.1),GIFT_MANAGER("Gift Manager",false,24,.8),VISIT_COORDINATOR("Visit Coordinator",false,24,.8),
    SOUL_MANAGER("Soul Manager",false,32,1.1),STRUCTURE_MANAGER("Structure Manager",false,32,1),TECHNICIAN("Maintenance Technician",false,28,1),VOID_OPERATOR("Void Power Operator",false,24,.8),ACCOUNTANT("Accountant",false,40,1.2),MASTER("Master Automation Manager",false,64,1.5);
    public final String label;public final boolean overworld;public final int price;public final double salaryScale;
    TycoonWorkerType(String label,boolean overworld,int price,double salaryScale){this.label=label;this.overworld=overworld;this.price=price;this.salaryScale=salaryScale;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public static TycoonWorkerType parse(String id){return valueOf(id.toUpperCase(Locale.ROOT));}
}
