import java.util.LinkedList;
import java.util.Queue;

import org.omg.CORBA.RepositoryIdHelper;

import scout.ScoutManager;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class TestBot1{

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;
    
    private Position EnemyBaseLoc = null;
    
    private ScoutManager scoutMan = null;
    
    private boolean haveFactory = false;
    private boolean haveBarracks = false;
    private boolean haveMarine = false;
    private boolean reported = false;
    

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
            	
            	if (unit.getType() == UnitType.Terran_Barracks) {
            		haveBarracks = true;
            	}
            	
            	if (unit.getType() == UnitType.Terran_Factory)
                	haveFactory = true;
            	
            	if (unit.getType() == UnitType.Terran_Marine) {
            		scoutMan = new ScoutManager(unit, game);          		
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
                
                //game.setLocalSpeed(0);
            }

            @Override
            public void onFrame() {
                game.setTextSize(10);
                game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

                StringBuilder units = new StringBuilder("My units:\n");
                
                if (scoutMan != null && EnemyBaseLoc == null) {
                	if (scoutMan.foundEnemy()) {
                		scoutMan.primeScout();
                		EnemyBaseLoc = scoutMan.getEnemyBaseLoc();                		
                	}                
                }
                
                if (EnemyBaseLoc != null && !reported) {
                	if (scoutMan.distToEnemy(15) || scoutMan.getScout().isAttacking() || scoutMan.getScout().isUnderAttack()) {
                		scoutMan.reportEnemy();
                		reported = true;
                	}
                }
                
                /*if (!self.getUnits().contains(UnitType.Terran_Refinery) && self.minerals() >= 100 && !buildings.contains(UnitType.Terran_Refinery)) {
            		for (Unit unit : self.getUnits()) {
                		if (unit.getType().isWorker()) {
                			if (build(unit, UnitType.Terran_Refinery)) {
            					break;
            				}
                		}
                	}
                }*/
                
                if (!haveBarracks && self.minerals() >= 100 && !buildings.contains(UnitType.Terran_Barracks)) {
            		for (Unit unit : self.getUnits()) {
                		if (unit.getType().isWorker()) {
                			if (unit.isCarryingMinerals()) {
                				if (build(unit, UnitType.Terran_Barracks)) {
                					break;
                				}
                			}
                		}
                	}
                }
                
                /*if (!haveFactory && haveBarracks && self.minerals() >= 200 && self.gas() >= 100 && !buildings.contains(UnitType.Terran_Factory)) {
            		for (Unit unit : self.getUnits()) {
                		if (unit.getType().isWorker()) {
                			if (unit.isCarryingMinerals()) {
                				if (build(unit, UnitType.Terran_Factory)) {
                					break;
                				}
                			}
                		}
                	}
                }*/                              
                
                if (self.supplyTotal() - self.supplyUsed() <= 4 && haveBarracks
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
                
                /*for (Unit myUnit : self.getUnits()) {
                	if (myUnit.getType() == UnitType.Terran_Marine) {
                		haveMarine = true;
                		break;
                	}
                	else
                		haveMarine = false;
                }*/
                
                //iterate through my units
                for (Unit myUnit : self.getUnits()) {
                    units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");                                                                               
                    
                    if (myUnit.getType() == UnitType.Terran_Barracks && haveBarracks && !haveMarine && self.minerals() >= 50 && self.supplyTotal() - self.supplyUsed() != 0) {
                    	myUnit.train(UnitType.Terran_Marine);
                    	haveMarine = true;
                    }
                    
                    //if there's enough minerals, train an SCV
                    if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50 && haveMarine) {
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
    	//System.out.println(buildings);
    	TilePosition buildTile = 
				getBuildTile(worker, building, self.getStartLocation());
    	//System.out.println(buildTile.toString());
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
 	
 	if (ret == null) {
 		game.printf("Unable to find suitable build position for "+buildingType.toString());
 		System.out.println("Unable to find suitable build position for "+buildingType.toString());
 	}
 	return ret;
 }

 public static void main(String... args) {
        new TestBot1().run();
    }
}
