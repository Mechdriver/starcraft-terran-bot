package attack;

import java.util.ArrayList;
import java.util.HashMap;

import overmind.ControlCenter;
import scout.ScoutManager;
import build.BuildRequest;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;

public class AttackManager extends DefaultBWListener {
	private ControlCenter motherBrain = null;
	private ScoutManager scoutMan;
	private Game myGame;
	private Position enemyBaseLoc = null;
	private BuildRequest squadReq;
	private boolean depot = false;
	private boolean attacking = false;
	private int reqs = 0;
	private ArrayList<Unit> squadList = new ArrayList<Unit>();
	
	private static int maxSize = 5;
	
	public AttackManager(Game game, ScoutManager scoutMan) {
		myGame = game;
		this.scoutMan = scoutMan;
	}
	
	@Override
	public void onStart() {
		System.out.println("Attack Manager initialized.");
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
		
		if (enemyBaseLoc == null) {
			enemyBaseLoc = scoutMan.getEnemyBaseLoc();
		}
		
		if (enemyBaseLoc != null && SquadSize() == maxSize && !attacking) {
			for (Unit joe : squadList) {
				joe.attack(enemyBaseLoc);
			}
			attacking = true;
		}
		
	}
	
	private int SquadSize() {
		return squadList.size();
	}
	
	public void setControlCenter(ControlCenter control) {
		motherBrain = control;
	}
	
}
