import com.coraline.palace.PalaceGenerationOccupancy;

/** Actual pure generation-site occupancy logic, without Minecraft world IO. */
public final class PalaceGenerationOccupancyCheck {
    private static void require(boolean actual,boolean expected,String message){
        if(actual!=expected)throw new AssertionError(message+": expected "+expected+" got "+actual);
    }
    private static boolean check(String stage,double x,double z,boolean companyGenerated){
        return PalaceGenerationOccupancy.pauses(stage,x,z,
                true,1000,1000,true,companyGenerated,5000,5000,
                true,1250,1000,160,384);
    }
    public static void main(String[] args){
        require(check("TERRAIN",1000,1000,true),true,"Palace terrain work must protect nearby players");
        require(check("HOUSE",1000,1000,true),true,"Palace house work must protect nearby players");
        require(check("WELL",1000,1000,true),false,"Visitor at verified Palace must not block the distant Well");
        require(check("WELL_VERIFY",1000,1000,true),false,"Well readback must not pause for Palace visitor");
        require(check("WELL",1250,1000,true),true,"Physical visitor at unbuilt Well must be protected");
        require(check("WELL",1310,1000,true),false,"Visitor beyond Well build footprint may continue");
        require(check("SIGNS",1000,1000,true),false,"Shared sign has its own local guard");
        require(check("FINISH",1000,1000,true),false,"Read-only final sightline must not pause for Palace visitor");
        require(check("COMPANY_PREPARE",5000,5000,false),true,"Unbuilt Company protects local visitors");
        require(check("COMPANY_VERIFY",5000,5000,true),false,"Verified Company cannot block unrelated work");
        require(check("TERRAIN",9000,9000,true),false,"Far-away visitors must not block Palace terrain");
        System.out.println("PalaceGenerationOccupancyCheck: 11 scenarios passed.");
    }
}
