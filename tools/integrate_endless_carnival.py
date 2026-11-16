"""Connect the native carnival activity to registries and generation."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def replace(path,old,new):
    p=ROOT/path;s=p.read_text(encoding='utf-8')
    if new in s:return
    assert old in s,(path,old)
    p.write_text(s.replace(old,new),encoding='utf-8')
def main():
    base='src/main/java/com/coraline/'
    replace(base+'registry/ModBlocks.java','    public static final RegistryObject<Block> PERFORMANCE_CONTROL=', '    public static final RegistryObject<Block> CARNIVAL_CONTROL=BLOCKS.register("carnival_control",com.coraline.postgame.CarnivalBlock::new);\n    public static final RegistryObject<Block> PERFORMANCE_CONTROL=')
    replace(base+'registry/ModBlockEntities.java','    public static final RegistryObject<BlockEntityType<com.coraline.postgame.PerformanceEntity>> PERFORMANCE=', '    public static final RegistryObject<BlockEntityType<com.coraline.postgame.CarnivalEntity>> CARNIVAL=BLOCK_ENTITIES.register("carnival",()->BlockEntityType.Builder.of(com.coraline.postgame.CarnivalEntity::new,ModBlocks.CARNIVAL_CONTROL.get()).build(null));\n    public static final RegistryObject<BlockEntityType<com.coraline.postgame.PerformanceEntity>> PERFORMANCE=')
    replace(base+'registry/ModEntities.java','    public static final RegistryObject<EntityType<com.coraline.postgame.VoidBoltEntity>> VOID_BOLT=', '    public static final RegistryObject<EntityType<com.coraline.postgame.CarnivalTargetEntity>> CARNIVAL_TARGET=ENTITIES.register("carnival_target",()->EntityType.Builder.<com.coraline.postgame.CarnivalTargetEntity>of(com.coraline.postgame.CarnivalTargetEntity::new,MobCategory.MISC).sized(.95F,.95F).clientTrackingRange(10).updateInterval(1).fireImmune().build("coraline:carnival_target"));\n    public static final RegistryObject<EntityType<com.coraline.postgame.VoidBoltEntity>> VOID_BOLT=')
    replace(base+'client/ClientModEvents.java','        event.registerEntityRenderer(ModEntities.OTHERWORLD_RESIDENT.get(),','        event.registerEntityRenderer(ModEntities.CARNIVAL_TARGET.get(),com.coraline.client.renderer.CarnivalTargetRenderer::new);\n        event.registerEntityRenderer(ModEntities.OTHERWORLD_RESIDENT.get(),')
    p='postgame/InfiniteSitePiece.java'
    replace(base+p,'        PerformanceLayout.place(level,clip,plan,index);','        PerformanceLayout.place(level,clip,plan,index);\n        CarnivalLayout.place(level,clip,plan,index);')
    replace(base+p,'        if(role==InfiniteStructurePlan.Role.RESIDENCE&&plan.family()==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY)name="resistance_sanctuary_supplies";','        if(role==InfiniteStructurePlan.Role.RESIDENCE&&plan.family()==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY)name="resistance_sanctuary_supplies";\n        if(role==InfiniteStructurePlan.Role.CARNIVAL_SUPPLIES)name="endless_carnival_supplies";')
    replace(base+p,'if(role==InfiniteStructurePlan.Role.HALL||role==InfiniteStructurePlan.Role.STORAGE||','if(role==InfiniteStructurePlan.Role.CARNIVAL_SUPPLIES||role==InfiniteStructurePlan.Role.CARNIVAL_VAULT||role==InfiniteStructurePlan.Role.HALL||role==InfiniteStructurePlan.Role.STORAGE||')
    replace(base+p,'boolean guarded=role!=InfiniteStructurePlan.Role.HALL','boolean guarded=role!=InfiniteStructurePlan.Role.CARNIVAL_SUPPLIES&&role!=InfiniteStructurePlan.Role.HALL')
    replace(base+p,'        if(plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE)wall=','        if(plan.family()==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL)wall=Blocks.PINK_TERRACOTTA.defaultBlockState();\n        if(plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE)wall=')
    replace(base+'postgame/InfiniteSiteGuards.java','            case GRAND_THEATRE->','            case ENDLESS_CARNIVAL->{/* Physical shooting gallery and troupe. */}\n            case GRAND_THEATRE->')
    p='postgame/InfiniteSiteResidents.java'
    replace(base+p,'return plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE?4:','return (plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE||plan.family()==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL)?4:')
    replace(base+p,'||family==InfiniteStructurePlan.Family.GRAND_THEATRE;','||family==InfiniteStructurePlan.Family.GRAND_THEATRE||family==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL;')
    replace(base+p,'||plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE&&plan.modules()','||(plan.family()==InfiniteStructurePlan.Family.GRAND_THEATRE||plan.family()==InfiniteStructurePlan.Family.ENDLESS_CARNIVAL)&&plan.modules()')
    p='postgame/OtherworldResident.java'
    replace(base+p,'if(!released()&&PerformanceLayout.freed(level(),residence()))recordRelease();','if(isAlive()&&!released()&&(residence().getString("Family").equals(InfiniteStructurePlan.Family.ENDLESS_CARNIVAL.name())?CarnivalLayout.freed(level(),residence()):PerformanceLayout.freed(level(),residence())))recordRelease();')
    replace(base+p,':"message.coraline.performer_loop"',':residence().getString("Family").equals(InfiniteStructurePlan.Family.ENDLESS_CARNIVAL.name())?"message.coraline.carnival_performer_loop":"message.coraline.performer_loop"')
    replace(base+p,'if(role()==0){offer(buttons(2),','if(role()==0){offer(buttons(3),new ItemStack(Items.ARROW,16),8);offer(buttons(2),')
    replace(base+p,'else if(role()==1){offer(buttons(6),','else if(role()==1){offer(buttons(8),new ItemStack(Items.BOW),4);offer(buttons(6),')
    p='postgame/SiteCacheBlockEntity.java'
    replace(base+p,'&&PerformanceLayout.cacheReady(level,receipt);','&&PerformanceLayout.cacheReady(level,receipt)&&CarnivalLayout.cacheReady(level,receipt);')
    replace(base+p,'String blockedFeedback(){return receipt.getString("Family")','String blockedFeedback(){return receipt.getString("Family").equals(InfiniteStructurePlan.Family.ENDLESS_CARNIVAL.name())?"message.coraline.carnival_reward_sealed":receipt.getString("Family")')
if __name__=='__main__':main()
