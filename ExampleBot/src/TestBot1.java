import java.util.LinkedList;
import java.util.Queue;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class TestBot1{

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    public void run() {
        mirror.getModule().setEventListener(new DefaultBWListener() {
            @Override
            public void onUnitCreate(Unit unit) {
                System.out.println("New unit " + unit.getType());
            }
            
            @Override
            public void onUnitComplete(Unit unit) {
            	if (unit.getType().isBuilding() && buildings.contains(unit.getType())) {
            		buildings.remove(unit.getType());
            	}
            }
            
            @Override
            public void onStart() {
                game = mirror.getGame();
                self = game.self();

                //Use BWTA to analyze map
                //This may take a few minutes if the map is processed first time!
                System.out.println("Analyzing map...");
                BWTA.readMap();
                BWTA.analyze();
                System.out.println("Map data ready");
                
            }

            @Override
            public void onFrame() {
                game.setTextSize(10);
                game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

                StringBuilder units = new StringBuilder("My units:\n");

                if (self.supplyTotal() - self.supplyUsed() <= 4 
                		&& self.minerals() >= 100 
                		&& !buildings.contains(UnitType.Terran_Supply_Depot)) {
                	for (Unit unit : self.getUnits()) {
                		if (unit.getType().isWorker()) {
                			if (!unit.isCarryingMinerals() && !unit.isCarryingGas()) {
                				if (build(unit, UnitType.Terran_Supply_Depot)) {
                					break;
                				}
                			}
                		}
                	}
                }
                //iterate through my units
                for (Unit myUnit : self.getUnits()) {
                    units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

                    //if there's enough minerals, train an SCV
                    if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50) {
                        myUnit.train(UnitType.Terran_SCV);
                    }

                    //if it's a drone and it's idle, send it to the closest mineral patch
                    if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                        Unit closestMineral = null;

                        //find the closest mineral
                        for (Unit neutralUnit : game.neutral().getUnits()) {
                            if (neutralUnit.getType().isMineralField()) {
                                if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                                    closestMineral = neutralUnit;
                                }
                            }
                        }

                        //if a mineral patch was found, send the drone to gather it
                        if (closestMineral != null) {
                            myUnit.gather(closestMineral, false);
                        }
                    }
                }

                //draw my units on screen
                game.drawTextScreen(10, 25, units.toString());
            }
        });

        mirror.startGame();
    }
    
    private Queue<UnitType> buildings = new LinkedList<UnitType>();
    private boolean enqueueBuilding(UnitType building) {
    	if (!building.isBuilding()) {
    		return false;
    	}
    	return buildings.offer(building);
    }

    private boolean build(Unit worker, UnitType building) {
    	System.out.println(buildings);
    	TilePosition buildTile = 
				getBuildTile(worker, UnitType.Terran_Supply_Depot, self.getStartLocation());
    	if (buildTile != null) {
    		if (worker.build(buildTile, building)) {
    	    	buildings.add(building);
    	    	return true;
    		}
    	}
    	return false;
    }
 // Returns a suitable TilePosition to build a given building type near 
 // specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
 public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
 	TilePosition ret = null;
 	int maxDist = 3;
 	int stopDist = 40;
 	
 	// Refinery, Assimilator, Extractor
 	if (buildingType.isRefinery()) {
 		for (Unit n : game.neutral().getUnits()) {
 			if ((n.getType() == UnitType.Resource_Vespene_Geyser) && 
 					( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
 					( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
 					) return n.getTilePosition();
 		}
 	}
 	
 	while ((maxDist < stopDist) && (ret == null)) {
 		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
 			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
 				if (game.canBuildHere(builder, new TilePosition(i,j), buildingType, false)) {
 					// units that are blocking the tile
 					boolean unitsInWay = false;
 					for (Unit u : game.getAllUnits()) {
 						if (u.getID() == builder.getID()) continue;
 						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
 					}
 					if (!unitsInWay) {
 						return new TilePosition(i, j);
 					}
 				}
 			}
 		}
 		maxDist += 2;
 	}
 	
 	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
 	return ret;
 }

 public static void main(String... args) {
        new TestBot1().run();
    }
}
