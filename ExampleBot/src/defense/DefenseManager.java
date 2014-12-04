package defense;

import java.util.ArrayList;

import overmind.ControlCenter;
import scout.ScoutManager;
import build.BuildRequest;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Unit;
import bwapi.UnitType;

public class DefenseManager extends DefaultBWListener{
	private Game myGame;
	private ControlCenter motherBrain;
	private boolean depot = false;
	private int reqs = 0;
	private BuildRequest squadReq;
	private ArrayList<Unit> squadList = new ArrayList<Unit>();
	
	private Unit target = null;
	
	private static int maxSize = 20;
	
	public DefenseManager(Game game) {
		myGame = game;
	}
	
	@Override
	public void onStart() {
		System.out.println("Defense Manager initialized.");
	}
	
	@Override
	public void onFrame() {
		if (motherBrain != null && !depot) {
			motherBrain.submitRequest(new BuildRequest(UnitType.Terran_Supply_Depot));
			depot = true;
		}
		
		if (motherBrain != null && reqs < maxSize) {
			squadReq = new BuildRequest(UnitType.Terran_Marine);
			squadReq = squadReq.withUnitOutput(squadList);
			motherBrain.submitRequest(squadReq);
			reqs++;
		}
		
		if (target == null || target.getHitPoints() == 0) {
			if (!myGame.enemy().getUnits().isEmpty()) {
				target = myGame.enemy().getUnits().get(0);
			}
		}
		
		if (!myGame.enemy().getUnits().isEmpty()) {
			
			for (Unit joe : squadList) {
				joe.attack(target);
			}
		}
	}
	
	private int SquadSize() {
		return squadList.size();
	}
	
	public void setControlCenter(ControlCenter control) {
		motherBrain = control;
	}
}
