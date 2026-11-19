package com.coraline.palace;

import net.minecraft.nbt.*;

/** Stable schema for a bounded resumable world job, not a completed-site flag. */
public final class PalaceGenerationRules {
    public enum Stage { SELECT_PALACE,SURVEY_REGION,SELECT_WELL,SELECT_COMPANY,SURVEY_COMPANY,TERRAIN,DETAILS,HOUSE_PREPARE,HOUSE,HOUSE_VERIFY,COMPANY_PREPARE,COMPANY,COMPANY_VERIFY,WELL,WELL_VERIFY,SIGNS,FINISH,DONE }
    public static void validate(CompoundTag n){
        if(n.isEmpty())return;
        if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.contains("Stage",Tag.TAG_STRING))throw new IllegalArgumentException("Unknown regional-generation schema");
        var stage=Stage.valueOf(n.getString("Stage"));
        for(String key:new String[]{"Cursor","Subcursor","Verified","ProfileBase","CandidateX","CandidateZ","CompanyX","CompanyZ","CompanyY","ColumnX","ColumnZ","ColumnBottom","ColumnTop","ColumnTarget","Y","SurveyMin","SurveyMax","HouseSurveyMin","HouseSurveyMax","DetailBaseY"})
            if(n.contains(key)&&!n.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Malformed generation "+key);
        for(String key:new String[]{"Cursor","Subcursor","Verified"})if(n.getInt(key)<0||n.getInt(key)>100_000_000)throw new IllegalArgumentException("Unbounded generation "+key);
        for(String key:new String[]{"PalaceClaimed","CompanyClaimed","ColumnActive","ColumnChanged","ProfileSurveyed","CompanyFirst"})if(n.contains(key)&&(!n.contains(key,Tag.TAG_BYTE)||n.getByte(key)<0||n.getByte(key)>1))throw new IllegalArgumentException("Malformed generation "+key);
        boolean early=n.contains("DeferredPalace",Tag.TAG_COMPOUND);
        if(n.contains("DeferredPalace")){
            if(!early||!java.util.EnumSet.of(Stage.SELECT_COMPANY,Stage.SURVEY_COMPANY,Stage.COMPANY_PREPARE,Stage.COMPANY,Stage.COMPANY_VERIFY).contains(stage))throw new IllegalArgumentException("Invalid early Company phase");
            var deferred=n.getCompound("DeferredPalace");
            if(deferred.contains("DeferredPalace"))throw new IllegalArgumentException("Nested early Company phase");
            var resume=stage(deferred);
            if(resume!=Stage.SELECT_PALACE&&resume!=Stage.SURVEY_REGION&&resume!=Stage.SELECT_WELL)throw new IllegalArgumentException("Invalid deferred Palace phase");
        }
        if(!early&&stage.ordinal()>Stage.SELECT_PALACE.ordinal()&&(!n.getBoolean("PalaceClaimed")||!n.contains("CandidateX",Tag.TAG_INT)||!n.contains("CandidateZ",Tag.TAG_INT)||!n.contains("ProfileBase",Tag.TAG_INT)||n.getInt("ProfileBase")<64||n.getInt("ProfileBase")>150))throw new IllegalArgumentException("Missing safe Palace claim/profile");
        if(stage.ordinal()>=Stage.SURVEY_COMPANY.ordinal()&&(!n.getBoolean("CompanyClaimed")||!n.contains("CompanyX",Tag.TAG_INT)||!n.contains("CompanyZ",Tag.TAG_INT)))throw new IllegalArgumentException("Missing safe company claim");
        if(n.getBoolean("ColumnActive")&&(stage!=Stage.TERRAIN&&stage!=Stage.HOUSE_PREPARE&&stage!=Stage.COMPANY_PREPARE))throw new IllegalArgumentException("Column outside its terrain/building phase");
        if(!early&&stage.ordinal()>=Stage.SELECT_WELL.ordinal()&&!n.getBoolean("ProfileSurveyed"))throw new IllegalArgumentException("Missing actual regional survey");
        if(stage==Stage.DETAILS&&n.getInt("Subcursor")>0&&(!n.contains("DetailBaseY",Tag.TAG_INT)||n.getInt("DetailBaseY")< -63||n.getInt("DetailBaseY")>319))throw new IllegalArgumentException("Missing original vegetation/rock base");
        for(String key:new String[]{"PalaceSign","CompanySign"})if(n.contains(key)&&!n.hasUUID(key))throw new IllegalArgumentException("Malformed original shared sign UUID");
        if(n.getBoolean("ColumnActive")){
            for(String key:new String[]{"ColumnX","ColumnZ","ColumnBottom","ColumnTop","ColumnTarget","Y"})if(!n.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Missing active-column "+key);
            if(n.getInt("ColumnBottom")< -64||n.getInt("ColumnTop")>319||n.getInt("ColumnTop")<n.getInt("ColumnBottom")||n.getInt("Y")<n.getInt("ColumnBottom")||n.getInt("Y")>n.getInt("ColumnTop")+1||n.getInt("ColumnTarget")< -64||n.getInt("ColumnTarget")>319)throw new IllegalArgumentException("Active column outside safe build bounds");
            int centerX=n.getInt(stage==Stage.COMPANY_PREPARE?"CompanyX":"CandidateX"),centerZ=n.getInt(stage==Stage.COMPANY_PREPARE?"CompanyZ":"CandidateZ"),radius=stage==Stage.COMPANY_PREPARE?35:PalaceTerrainPlan.EXTENT;
            if(Math.abs((long)n.getInt("ColumnX")-centerX)>radius||Math.abs((long)n.getInt("ColumnZ")-centerZ)>radius)throw new IllegalArgumentException("Active column outside the original claim");
        }
        for(String prefix:new String[]{"Candidate","Company"})if(n.contains(prefix+"X")||n.contains(prefix+"Z")){
            if(!n.contains(prefix+"X",Tag.TAG_INT)||!n.contains(prefix+"Z",Tag.TAG_INT))throw new IllegalArgumentException("Partial claim coordinates");
            new PalaceSitePlan.Point(n.getInt(prefix+"X"),n.getInt(prefix+"Z"));
        }
    }
    public static Stage stage(CompoundTag n){validate(n);return n.isEmpty()?Stage.SELECT_PALACE:Stage.valueOf(n.getString("Stage"));}
    public static void enter(CompoundTag n,Stage stage){n.putInt("Version",1);n.putString("Stage",stage.name());n.putInt("Cursor",0);n.putInt("Subcursor",0);n.putInt("Verified",0);n.remove("ColumnActive");n.remove("ColumnChanged");}
    private PalaceGenerationRules(){}
}
