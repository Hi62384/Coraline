package com.coraline.feast;

import static com.coraline.feast.CakeKitchenRules.Station.*;

/** Standalone station rules and recoverable saved states. No Minecraft runtime. */
public final class CakeKitchenRulesCheck {
    private static void require(boolean test,String issue){
        if(!test)throw new AssertionError(issue);
    }

    public static void main(String[] args){
        require(workpiece(0)==at(0,0,0),"Bowl prop and supporting table disagree");
        require(workpiece(1)==at(0,0,-2),"Oven prop and supporting block disagree");
        require(workpiece(2)==at(0,0,2),"Stand prop and supporting table disagree");
        require(at(0,0,-22)==JUDGING&&at(1,0,-2)==null&&at(0,1,-2)==null,"Unrelated blocks dispatch a station");
        require(CakeKitchenRules.mode(STAND,2,0)==3&&CakeKitchenRules.mode(OVEN,2,0)==CakeKitchenRules.EMPTY,"Mixed batter is missing from the stand");
        require(CakeKitchenRules.mode(STAND,3,106)==CakeKitchenRules.EMPTY&&CakeKitchenRules.mode(OVEN,3,106)==7,"Saved bake did not restore the pan at the oven");
        require(CakeKitchenRules.mode(STAND,4,166)==CakeKitchenRules.ASSEMBLED&&CakeKitchenRules.mode(OVEN,4,166)==CakeKitchenRules.EMPTY,"Bake removal duplicates the pan");
        for(int layers=1;layers<=3;layers++)require(CakeKitchenRules.layers(layers)==layers,"Saved layer count changed");
        int required=0b11111,wrong=1<<5;
        require(CakeKitchenRules.ingredientEditable(0,0,required),"First ingredient blocked");
        require(!CakeKitchenRules.ingredientEditable(1,0,required),"Ingredient order skipped");
        require(CakeKitchenRules.ingredientEditable(5,required,required),"Wrong ingredient choice unavailable");
        require(CakeKitchenRules.ingredientEditable(5,required|wrong,required),"Wrong ingredient removal blocked");
        require(!CakeKitchenRules.ingredientEditable(0,required|wrong,required),"Recipe ingredient cannot be collected twice");
        require(CakeKitchenRules.decorationEditable(4,0),"Filling or frosting cannot cycle before application");
        require(!CakeKitchenRules.decorationEditable(4,1),"Frosting edits can bypass timed application");
        require(!CakeKitchenRules.actionReady(101,101)&&!CakeKitchenRules.actionReady(102,101),"A second packet can act on the first click");
        require(CakeKitchenRules.actionReady(105,101)&&CakeKitchenRules.actionReady(30,101),"Cooldown cannot resume after reload");
        require(CakeKitchenRules.bakeQuality(39)==0&&CakeKitchenRules.bakeQuality(106)==4&&CakeKitchenRules.bakeQuality(144)==4
            &&CakeKitchenRules.bakeQuality(177)==0,"Bake quality no longer follows the original timing");
        System.out.println("CakeKitchenRulesCheck passed.");
    }
}
