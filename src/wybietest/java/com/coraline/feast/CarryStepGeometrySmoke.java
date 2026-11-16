package com.coraline.feast;

import java.util.*;

/** Executes the production footstep calculation without Forge, a server, or a GPU. */
public final class CarryStepGeometrySmoke {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        // The authored hall's first two blocks are bare; its remaining corridor is carpeted.
        var p=new CarryStepGeometry.Point(.5,181,.5);var goal=new CarryStepGeometry.Point(.5,181.0625,9.5);int steps=0;
        while(Math.hypot(p.x()-goal.x(),p.z()-goal.z())>.001&&steps++<200){
            var next=CarryStepGeometry.horizontal(p,goal);var floor=new ArrayList<CarryStepGeometry.FloorBox>();
            for(int z=2;z<=9;z++)floor.add(new CarryStepGeometry.FloorBox(0,181,z,1,181.0625,z+1));
            var at=CarryStepGeometry.floor(next,181,floor);check(at!=null,"Thin carpet blocked the carry");
            check(Math.hypot(at.x()-p.x(),at.z()-p.z())<=CarryStepGeometry.MAX_STEP+.000001,"Carry jumped instead of walking");
            check(Math.abs(at.y()-p.y())<=.062501,"Carpet produced a large step-up");
            if(at.z()+.3>2)check(at.y()>=181.0625,"Footprint clipped into the carpet edge");p=at;
        }
        check(steps<=38&&Math.abs(p.y()-181.0625)<.000001,"Carry is still too slow or did not reach the carpeted mirror approach");
        // A lower open door has a tall leaf at its side, not a block-wide step.
        var openDoor=List.of(new CarryStepGeometry.FloorBox(0,181,0,.1875,182,1));
        check(CarryStepGeometry.floor(new CarryStepGeometry.Point(.5,181,.5),181,openDoor)!=null,"Open door leaf blocked the clear passage");
        check(CarryStepGeometry.floor(new CarryStepGeometry.Point(.25,181,.5),181,openDoor)==null,"Carry penetrated the door leaf");
        var wall=List.of(new CarryStepGeometry.FloorBox(-1,181,0,0,182,1));
        var away=CarryStepGeometry.horizontal(new CarryStepGeometry.Point(.3,181,.5),new CarryStepGeometry.Point(.5,181,.5));
        check(CarryStepGeometry.floor(away,181,wall)!=null,"Actor touching the old wall cannot move away");
        check(CarryStepGeometry.floor(new CarryStepGeometry.Point(.5,181,.5),181,List.of(new CarryStepGeometry.FloorBox(0,181,0,1,182,1)))==null,"Full block was treated as a carpet");
        System.out.println("Carry geometry: carpet transition, speed bound, open door, wall recovery and solid-block guards passed.");
    }
    private CarryStepGeometrySmoke(){}
}
