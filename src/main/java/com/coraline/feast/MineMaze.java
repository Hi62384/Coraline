package com.coraline.feast;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import static com.coraline.feast.ExpeditionActivities.*;

/** Connected, seeded four-depth maze. Each deeper stratum is wider and has more loops. */
public final class MineMaze {
    public record Cell(int tier,int col,int row){
        public BlockPos pos(){return new BlockPos((col-(7+tier*2)/2)*14,-tier*14,-14-row*14);}
    }
    public record Edge(Cell a,Cell b){}
    public record Layout(List<Cell> cells,List<Edge> edges){}
    public static Layout generate(long seed){
        var rng=new Random(seed);var cells=new ArrayList<Cell>();var edges=new ArrayList<Edge>();
        for(int t=0;t<4;t++){
            int size=7+2*t;var grid=new Cell[size][size];
            for(int z=0;z<size;z++)for(int x=0;x<size;x++){grid[x][z]=new Cell(t,x,z);cells.add(grid[x][z]);}
            var visited=new HashSet<Cell>();var stack=new ArrayDeque<Cell>();stack.push(grid[size/2][0]);visited.add(stack.peek());
            while(!stack.isEmpty()){
                var at=stack.peek();var choices=new ArrayList<Cell>();
                for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=at.col+d[0],z=at.row+d[1];if(x>=0&&x<size&&z>=0&&z<size&&!visited.contains(grid[x][z]))choices.add(grid[x][z]);}
                if(choices.isEmpty()){stack.pop();continue;}var next=choices.get(rng.nextInt(choices.size()));visited.add(next);edges.add(new Edge(at,next));stack.push(next);
            }
            // Loops offer alternate routes instead of a single obvious rail spine.
            for(int z=0;z<size;z++)for(int x=0;x<size-1;x++)if(rng.nextInt(100)<8+t*5){var a=grid[x][z];var b=grid[x+1][z];if(edges.stream().noneMatch(e->e.a.equals(a)&&e.b.equals(b)||e.a.equals(b)&&e.b.equals(a)))edges.add(new Edge(a,b));}
            if(t>0){int old=size-2,col=rng.nextInt(old);edges.add(new Edge(new Cell(t-1,col,old-1),grid[col+1][old]));}
        }
        return new Layout(List.copyOf(cells),List.copyOf(edges));
    }
    public static BlockPos stairStart(Edge edge){return edge.a.pos().east(7);}
    public static BlockPos stairEnd(Edge edge){return edge.b.pos().east(7);}
    static void build(ServerLevel l,BlockPos c,Layout maze){
        for(var cell:maze.cells){var b=c.offset(cell.pos());Block rock=cell.tier<2?Blocks.STONE:Blocks.DEEPSLATE;
            box(l,b,-4,-2,-4,4,5,4,rock);box(l,b,-3,0,-3,3,4,3,Blocks.AIR);
            for(int side:new int[]{-3,3}){box(l,b,side,0,-3,side,3,-3,Blocks.SPRUCE_LOG);box(l,b,side,0,3,side,3,3,Blocks.SPRUCE_LOG);}
            box(l,b,-3,4,0,3,4,0,Blocks.SPRUCE_LOG);
            if(Math.floorMod(cell.col+cell.row,5)==0)put(l,b,3,0,3,Blocks.COBWEB);
        }
        // Flat tunnels first. Sloped connectors must be carved LAST: later
        // room/junction fills used to seal their lower exits and remove treads.
        for(var edge:maze.edges){if(edge.a.tier!=edge.b.tier)continue;var a=edge.a.pos();var b=edge.b.pos();
            int length=Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getZ()-b.getZ()));
            // Seal corridor shells first; then carve. Sloping connectors drop one block per step.
            for(int pass=0;pass<2;pass++)for(int i=0;i<=length;i++){
                int x=a.getX()+(b.getX()-a.getX())*i/length,z=a.getZ()+(b.getZ()-a.getZ())*i/length,y=a.getY()+(b.getY()-a.getY())*i/length;
                boolean alongX=a.getX()!=b.getX();var at=c.offset(x,y,z);
                if(pass==0){if(alongX)box(l,at,0,-2,-2,0,5,2,Blocks.DEEPSLATE);else box(l,at,-2,-2,0,2,5,0,Blocks.DEEPSLATE);}
                else {if(alongX)box(l,at,0,0,-1,0,4,1,Blocks.AIR);else box(l,at,-1,0,0,1,4,0,Blocks.AIR);
                    if(a.getY()!=b.getY())for(int dx=-1;dx<=1;dx++)ActivityBlocks.set(l,at.offset(dx,-1,0),Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,net.minecraft.core.Direction.SOUTH),2);
                }
            }
        }
        // Reopen junction interiors after all tunnel shells, so one branch cannot seal another.
        for(var cell:maze.cells){var b=c.offset(cell.pos());box(l,b,-3,0,-3,3,4,3,Blocks.AIR);box(l,b,-3,4,0,3,4,0,Blocks.SPRUCE_LOG);}
        for(var edge:maze.edges)if(edge.a.tier!=edge.b.tier){
            var a=stairStart(edge);var b=stairEnd(edge);
            // Route shafts beside the rooms; centre shafts used to seal lower-floor branches. Use the open space between them for a
            // full-height descending stair, then re-open both landing approaches.
            for(int i=0;i<=14;i++){
                var at=c.offset(a.getX(),a.getY()-i,a.getZ()-i);
                box(l,at,-2,-2,0,2,5,0,Blocks.DEEPSLATE);
            }
            for(int i=0;i<=14;i++){
                var at=c.offset(a.getX(),a.getY()-i,a.getZ()-i);
                box(l,at,-1,0,0,1,4,0,Blocks.AIR);
                for(int x=-1;x<=1;x++)ActivityBlocks.set(l,at.offset(x,-1,0),Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,net.minecraft.core.Direction.SOUTH),2);
            }
            box(l,c.offset(a),-2,-2,1,2,5,1,Blocks.DEEPSLATE);
            box(l,c.offset(b),-2,-2,-1,2,5,-1,Blocks.DEEPSLATE);
            // Enclosed side entrances connect each landing back to its room.
            // Keep bridge shells outside room centres and stair treads.
            for(var endpoint:new BlockPos[]{a,b}){
                var room=c.offset(endpoint.west(7));
                // Enclose only the outside bridge, never stamp over room-centre branches.
                box(l,room,4,-2,-2,5,5,2,Blocks.DEEPSLATE);
                box(l,room,0,0,-1,5,4,1,Blocks.AIR);
                box(l,c.offset(endpoint),-1,0,0,1,4,0,Blocks.AIR);
            }
        }
        // Sheltered entrance and a single recognizable return gate. No lighting deeper inside.
        box(l,c,-5,-2,-8,5,5,10,Blocks.STONE_BRICKS);box(l,c,-4,0,-7,4,4,9,Blocks.AIR);
        for(int z=-14;z<=-7;z++){box(l,c,-2,-2,z,2,5,z,Blocks.STONE);box(l,c,-1,0,z,1,4,z,Blocks.AIR);}
        for(int x:new int[]{-3,3})put(l,c,x,3,4,Blocks.SHROOMLIGHT);
        sign(l,c.offset(0,2,9),net.minecraft.core.Direction.NORTH,"RETURN GATE","Leave with your bag","20 min / 5 hearts","Deeper = danger");
    }
    private MineMaze(){}
}
