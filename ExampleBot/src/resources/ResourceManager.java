package resources;

import bwapi.DefaultBWListener;
import bwapi.Game;

public class ResourceManager extends DefaultBWListener {
	private Game game;
	
	
	public ResourceManager(Game game) {
		this.game = game;
	}
	
	@Override
	public void onStart() {
//		game.setTextSize(10);
//		game.drawTextScreen(10, 10, "Resource Manager initialized.");
		System.out.println("Resource Manager Initialized.");
	}
	
	@Override
	public void onFrame() {
//		game.setTextSize(10);
//		game.drawTextScreen(10, 10, "Resource Manager doin work.");
		System.out.println("Resource Manager doin work.");
	}
}
