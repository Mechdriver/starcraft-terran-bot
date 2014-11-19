package overmind;

import bwapi.DefaultBWListener;
import bwapi.Game;

public class ControlCenter extends DefaultBWListener {
	private Game game;
	
	
	public ControlCenter(Game game) {
		this.game = game;
	}
	
	@Override
	public void onStart() {
//		game.setTextSize(10);
//		game.drawTextScreen(10, 10, "Control Center initialized.");
		System.out.println("Control Center initialized.");
	}
	
	@Override
	public void onFrame() {
//		game.setTextSize(10);
//		game.drawTextScreen(10, 10, "Control Center doin work.");
		System.out.println("Control Center doin work.");
	}
}
