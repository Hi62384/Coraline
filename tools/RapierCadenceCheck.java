package com.coraline.clothing;

/** Exercises production admission boundaries, not a simulated damage engine. */
public final class RapierCadenceCheck {
    private static int checks;
    private static void require(boolean valid,String message){checks++;if(!valid)throw new AssertionError(message);}
    public static void main(String[] args) {
        for(int elapsed=-2;elapsed<=24;elapsed++)for(int target=-2;target<=24;target++)for(int window=0;window<=40;window++)
            require(!RapierCadence.ordinaryWindow(false,elapsed,target,window,7),"Foreign/unproven damage window was advanced");
        for(int interval=5;interval<=20;interval++) {
            for(int elapsed=0;elapsed<interval;elapsed++)for(int target=0;target<20;target++)
                require(!RapierCadence.ordinaryWindow(true,elapsed,target,20-target,interval),"World-time cadence admitted a premature strike");
            for(int target=0;target<interval;target++)
                require(!RapierCadence.ordinaryWindow(true,interval,target,20-target,interval),"Frozen/unticked target bypassed cadence");
            for(int window=21;window<=100;window++)
                require(!RapierCadence.ordinaryWindow(true,interval,interval,window,interval),"Custom long protection window was bypassed");
        }
        for(int ticks=7;ticks<10;ticks++) {
            require(RapierCadence.ordinaryWindow(true,ticks,ticks,20-ticks,7),"A proven ordinary rapier window cannot advance");
            require(!RapierCadence.ordinaryWindow(true,ticks,ticks,21-ticks,7),"Modified protection counter was treated as an ordinary window");
        }
        require(!RapierCadence.ordinaryWindow(true,-1,7,13,7),"Clock rollback admitted a strike");
        require(!RapierCadence.ordinaryWindow(true,21,7,13,7),"Expired provenance remained valid");
        require(!RapierCadence.ordinaryWindow(true,7,-1,13,7),"Target clock rollback admitted a strike");
        require(!RapierCadence.ordinaryWindow(true,7,7,13,4),"Unsupported ultra-fast configuration admitted a strike");
        System.out.println("RAPIER_CADENCE_OK checks="+checks);
    }
}
