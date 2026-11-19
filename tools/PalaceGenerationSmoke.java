import com.coraline.palace.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import net.minecraft.nbt.*;

/** Real Anvil-header IO in an owned fixture directory and pure plans/NBT only.
 * Does NOT launch a server, build terrain or certify visual/native generation.
 */
public final class PalaceGenerationSmoke {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static void invalid(CompoundTag n){try{PalaceGenerationRules.validate(n);throw new AssertionError("Malformed generation checkpoint accepted");}catch(IllegalArgumentException expected){checks++;}}
    public static int run(Path fixture)throws Exception{
        var palace=new PalaceSitePlan.Point(170_000,-230_000);int anchor=94;
        for(long seed=0;seed<24;seed++){
            var well=PalaceSitePlan.candidate(seed,new PalaceSitePlan.Point(0,0),PalaceSitePlan.Kind.WELL,0,palace).orElseThrow();
            var center=PalaceTerrainPlan.sample(seed,palace,well,anchor,palace.x(),palace.z(),77);
            check(center.surface()==anchor-1&&center.clearing(),"Palace foundation not grounded on its planned plateau");
            var rim=PalaceTerrainPlan.sample(seed,palace,well,anchor,well.x(),well.z(),76);
            check(rim.surface()==anchor+35&&rim.weight()==1&&rim.clearing(),"Well hill fails to support real ring at the reserved elevation");
            for(int x=-400;x<=400;x+=13)for(int z=-400;z<=400;z+=13){
                var a=PalaceTerrainPlan.sample(seed,palace,well,anchor,palace.x()+x,palace.z()+z,80);
                check(a.surface()>=-58&&a.surface()<=285&&Double.isFinite(a.weight())&&a.weight()>=0&&a.weight()<=1,"Unbounded valley height/weight");
                check(a.equals(PalaceTerrainPlan.sample(seed,palace,well,anchor,palace.x()+x,palace.z()+z,80)),"Reload changes valley geometry");
                if(Math.hypot(x,z)>385)check(a.surface()==80&&a.weight()==0,"Terrain changes leak beyond claimed region");
            }
            double wx=well.x()-palace.x(),wz=well.z()-palace.z(),length=Math.hypot(wx,wz);int last=anchor-1;
            for(int step=0;step<=Math.ceil(length);step++){
                double u=Math.min(1,step/length),winding=4*Math.sin(u*Math.PI*3)*Math.sin(u*Math.PI);
                int x=(int)Math.round(palace.x()+wx*u-wz/length*winding),z=(int)Math.round(palace.z()+wz*u+wx/length*winding);
                var at=PalaceTerrainPlan.sample(seed,palace,well,anchor,x,z,80);
                check(Math.abs(at.surface()-last)<=1,"Trail has an unwalkable >one-block step at "+step+" of "+length+": "+last+" -> "+at.surface());last=at.surface();
                check(!PalaceTerrainPlan.vegetation(seed,palace,well,x,z),"Conifer obstructs well trail/Palace sightline");
            }
        }
        for(var bounds:new PalaceLandmarkPlan.Bounds[]{PalaceLandmarkPlan.COMPANY,PalaceLandmarkPlan.WELL}){
            var unique=new java.util.HashSet<String>();
            for(int i=0;i<bounds.size();i++){var c=bounds.cell(i);check(c[0]>=bounds.minX()&&c[0]<=bounds.maxX()&&c[1]>=bounds.minY()&&c[1]<=bounds.maxY()&&c[2]>=bounds.minZ()&&c[2]<=bounds.maxZ(),"Landmark cursor escaped footprint");unique.add(java.util.Arrays.toString(c));}
            check(unique.size()==bounds.size(),"Streamed blueprint skips/duplicates positions");
        }
        for(int y=-31;y<=0;y++)check(PalaceLandmarkPlan.well(0,y,0)==PalaceLandmarkPlan.Material.AIR,"Disposal shaft is obstructed");
        check(PalaceLandmarkPlan.well(0,-32,0)==PalaceLandmarkPlan.Material.STONE,"Shaft lacks physical bottom");
        var root=new CompoundTag();PalaceGenerationRules.enter(root,PalaceGenerationRules.Stage.SELECT_PALACE);PalaceGenerationRules.validate(root);
        var bad=root.copy();bad.putInt("Version",2);invalid(bad);bad=root.copy();bad.putInt("Cursor",-1);invalid(bad);bad=root.copy();bad.putString("Stage","future stage");invalid(bad);
        root.putBoolean("PalaceClaimed",true);root.putBoolean("ProfileSurveyed",true);root.putInt("CandidateX",palace.x());root.putInt("CandidateZ",palace.z());root.putInt("ProfileBase",anchor);root.putBoolean("CompanyClaimed",true);root.putInt("CompanyX",1800);root.putInt("CompanyZ",2400);
        for(var stage:PalaceGenerationRules.Stage.values()){PalaceGenerationRules.enter(root,stage);PalaceGenerationRules.validate(root);check(PalaceGenerationRules.stage(root.copy())==stage,"Generation stage lost on NBT reload");}
        PalaceGenerationRules.enter(root,PalaceGenerationRules.Stage.TERRAIN);root.putBoolean("ColumnActive",true);root.putInt("ColumnX",palace.x());root.putInt("ColumnZ",palace.z());root.putInt("ColumnBottom",70);root.putInt("ColumnTop",100);root.putInt("ColumnTarget",90);root.putInt("Y",82);PalaceGenerationRules.validate(root);
        bad=root.copy();bad.putInt("ColumnX",palace.x()+385);invalid(bad);bad=root.copy();bad.putInt("Y",102);invalid(bad);bad=root.copy();bad.remove("ColumnBottom");invalid(bad);
        var deferred=new CompoundTag();
        PalaceGenerationRules.enter(deferred,PalaceGenerationRules.Stage.SURVEY_REGION);
        deferred.putBoolean("PalaceClaimed",true);
        deferred.putInt("CandidateX",palace.x());
        deferred.putInt("CandidateZ",palace.z());
        deferred.putInt("ProfileBase",94);
        deferred.putInt("Cursor",681);
        var company=new CompoundTag();
        company.put("DeferredPalace",deferred.copy());
        PalaceGenerationRules.enter(company,PalaceGenerationRules.Stage.SELECT_COMPANY);
        var reloaded=TagParser.parseTag(company.toString());
        check(PalaceGenerationRules.stage(reloaded)==PalaceGenerationRules.Stage.SELECT_COMPANY,"Company could not resume before Palace verification");
        check(reloaded.getCompound("DeferredPalace").getInt("Cursor")==681,"Early Company lost the deferred survey cursor");
        bad=reloaded.copy();PalaceGenerationRules.enter(bad,PalaceGenerationRules.Stage.TERRAIN);invalid(bad);
        earlyCompanyRegistry();
        anvil(fixture);return checks;
    }
    private static void earlyCompanyRegistry() throws Exception {
        var kind=PalaceSitePlan.Kind.COMPANY;
        var data=new PalaceSiteData();
        data.initialize(123,net.minecraft.core.BlockPos.ZERO);
        var prioritize=PalaceSiteData.class.getDeclaredMethod("prioritizeCompany");
        var generation=PalaceSiteData.class.getDeclaredMethod("generation");
        var reserve=PalaceSiteData.class.getDeclaredMethod("reserveGenerationPlan",PalaceSitePlan.Kind.class,net.minecraft.core.BlockPos.class);
        var verify=PalaceSiteData.class.getDeclaredMethod("generationVerified",PalaceSitePlan.Kind.class,net.minecraft.core.BlockPos.class,int.class,int.class);
        var resume=PalaceSiteData.class.getDeclaredMethod("resumeAfterEarlyCompany");
        for(var method:new java.lang.reflect.Method[]{prioritize,generation,reserve,verify,resume})method.setAccessible(true);
        check((Boolean)prioritize.invoke(data),"Required Company could not start on a fresh registry");
        var point=data.proposal(kind).orElseThrow();
        data=PalaceSiteData.load(TagParser.parseTag(data.save(new CompoundTag()).toString()));
        check(data.blocker().isEmpty()&&data.proposal(kind).orElseThrow().equals(point),"Company proposal changed across reload without a Palace");
        var work=(CompoundTag)generation.invoke(data);
        PalaceGenerationRules.enter(work,PalaceGenerationRules.Stage.SURVEY_COMPANY);
        work.putBoolean("CompanyClaimed",true);
        work.putInt("CompanyX",point.x());
        work.putInt("CompanyZ",point.z());
        work.putInt("CompanyY",82);
        work.putInt("Cursor",49);
        var address=new net.minecraft.core.BlockPos(point.x(),82,point.z());
        check((Boolean)reserve.invoke(data,kind,address),"Bounded Company survey could not reserve its original address");
        data=PalaceSiteData.load(TagParser.parseTag(data.save(new CompoundTag()).toString()));
        check(data.blocker().isEmpty()&&!data.companyMapReady()&&data.position(kind).orElseThrow().equals(address),"Reserved Company unlocked the map before readback");
        work=(CompoundTag)generation.invoke(data);
        PalaceGenerationRules.enter(work,PalaceGenerationRules.Stage.COMPANY_VERIFY);
        check(!(Boolean)verify.invoke(data,kind,address,49,50),"Partial readback unlocked the map");
        check((Boolean)verify.invoke(data,kind,address,50,50),"Complete readback did not unlock the map");
        resume.invoke(data);
        data=PalaceSiteData.load(TagParser.parseTag(data.save(new CompoundTag()).toString()));
        check(data.companyMapReady()&&!data.ready()&&data.blocker().isEmpty(),"Early Company verification was lost when the Palace survey resumed");
    }

    private static void anvil(Path owned)throws Exception{
        var directory=Files.createDirectory(owned.resolve("virgin-region-fixtures"));
        check(PalaceVirginTerrain.untouched(directory,-40,-40,40,40,(x,z)->false),"Missing regions are not virgin");
        check(!PalaceVirginTerrain.untouched(directory,-1,-1,1,1,(x,z)->x==0&&z==0),"Loaded but unsaved chunk accepted as virgin");
        var region=directory.resolve("r.-1.-1.mca");var header=ByteBuffer.allocate(8192).order(ByteOrder.BIG_ENDIAN);
        // Chunk (-1,-1) is index 1023 in region (-1,-1), NOT index zero.
        header.putInt(1023*4,0x00000201);try(var out=FileChannel.open(region,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){while(header.hasRemaining())out.write(header);}
        check(!PalaceVirginTerrain.untouched(directory,-1,-1,-1,-1,(x,z)->false),"Negative-coordinate saved chunk not detected");
        check(PalaceVirginTerrain.untouched(directory,-32,-32,-32,-32,(x,z)->false),"Untouched neighbour confused with occupied negative chunk");
        var before=Files.readAllBytes(region);PalaceVirginTerrain.untouched(directory,-1,-1,-1,-1,(x,z)->false);check(java.util.Arrays.equals(before,Files.readAllBytes(region)),"Virgin survey wrote to Anvil source");
        Files.createFile(directory.resolve("r.0.0.mca"));try{PalaceVirginTerrain.untouched(directory,0,0,0,0,(x,z)->false);throw new AssertionError("Truncated region interpreted as empty");}catch(java.io.IOException expected){checks++;}
        try{PalaceVirginTerrain.untouched(directory,0,0,129,0,(x,z)->false);throw new AssertionError("Unbounded header scan accepted");}catch(IllegalArgumentException expected){checks++;}
    }
}
