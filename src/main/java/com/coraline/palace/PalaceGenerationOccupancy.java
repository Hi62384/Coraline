package com.coraline.palace;

/**
 * Bounded site-specific proximity gate for the overworld landmark builder.
 * Verified Palace terrain is not modified by later Well/sign/readback stages.
 * Keep this pure so the critical arrival-while-building case is executable
 * without launching the Minecraft server.
 */
public final class PalaceGenerationOccupancy {
    public static final int WELL_PAUSE_RADIUS=56;
    private static boolean near(double x,double z,int atX,int atZ,double range){
        return Double.isFinite(x)&&Double.isFinite(z)
                &&Math.abs(x-atX)<=range&&Math.abs(z-atZ)<=range;
    }
    public static boolean pauses(String stage,double x,double z,
            boolean palaceClaimed,int palaceX,int palaceZ,
            boolean companyClaimed,boolean companyGenerated,int companyX,int companyZ,
            boolean wellPositioned,int wellX,int wellZ,int viewBlocks,int regionRadius){
        if(stage==null)return true;
        // The final sign stage performs its own 16-block player check. FINISH
        // is exclusively readback and must not wait on anybody at the Palace.
        if(stage.equals("SIGNS")||stage.equals("FINISH")||stage.equals("DONE"))return false;
        if(stage.equals("WELL")||stage.equals("WELL_VERIFY"))
            return wellPositioned&&near(x,z,wellX,wellZ,WELL_PAUSE_RADIUS);
        if(stage.equals("SELECT_COMPANY")||stage.equals("SURVEY_COMPANY")
                ||stage.equals("COMPANY_PREPARE")||stage.equals("COMPANY")||stage.equals("COMPANY_VERIFY"))
            return companyClaimed&&!companyGenerated&&near(x,z,companyX,companyZ,48+Math.max(0,viewBlocks)+64);
        // Terrain, details and Palace house preparation can still change the
        // region. Preserve their original broad player-occupancy protection.
        return palaceClaimed&&near(x,z,palaceX,palaceZ,
                Math.max(0,regionRadius)+Math.max(0,viewBlocks)+64);
    }
    private PalaceGenerationOccupancy(){}
}
