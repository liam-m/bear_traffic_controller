package scn;

import java.io.File;
import java.util.Random;

import lib.jog.graphics;
import lib.jog.input;
import lib.jog.window;

import cls.Aircraft;
import cls.Waypoint;

import btc.Main;

public class Demo extends Scene {
	
	private final cls.Vector[] _locationPoints = new cls.Vector[] {
		new cls.Vector(-64, 32, 9000),
		new cls.Vector(-64, 64, 9000),
		new cls.Vector(window.width() + 32, 32, 9000),
		new cls.Vector(window.width() + 32, 64, 9000),
	};
	private final String[] _locationNames = new String[] {
		"Top Left",
		"Bottom Left",
		"Top Right",
		"Bottom Right",
	};
	
	public static Waypoint[] _locationWayPoints = new Waypoint[] {
		new Waypoint(-64, 32, true), //top left
		new Waypoint(-64, window.height(), true), //bottom left
		new Waypoint(window.width() + 32, 32, true), // top right
		new Waypoint(window.width() + 32, window.height(), true), //bottom right
	};

	public static Waypoint[] _waypoints = new Waypoint[] {
		
		/*
		new Waypoint(0, 128),
		new Waypoint(0, 256),
		new Waypoint(window.width() - 32, 128),
		new Waypoint(window.width() - 32, 256),
		new Waypoint(window.width() - 32 / 4, 64),
		
		new Waypoint(128, 256),
		new Waypoint(416, 64),
		new Waypoint(344, 192),
		new Waypoint(256, 256),
		new Waypoint(128, 128),
		*/
		
		//airspace waypoints
		new Waypoint(300, 180, false),
		new Waypoint(110, 200, false),
		new Waypoint(125, 70, false),
		new Waypoint(500, 100, false),
		new Waypoint(470, 300, false),
		
		//destination/origin waypoints - present in this list for pathfinding.
		_locationWayPoints[0],
		_locationWayPoints[1],
		_locationWayPoints[2],
		_locationWayPoints[3],
	};
	
	
	
	private lib.OrdersBox _ordersBox;
	private double _timer;
	private Aircraft _selectedAircraft;
	private Waypoint _selectedWaypoint;
	private int _selectedPathpoint;
	private java.util.ArrayList<Aircraft> _aircraft;
	private graphics.Image _airplaneImage;
	private lib.ButtonText _manualOverrideButton;
	private lib.Altimeter _altimeter;
	private static double flightGenerationInterval = 12;
	private double flightGenerationTimer = 0;
	private int maxAircraft = 4;
	
	public Main main() {
		return _main;
	}
	
	public java.util.ArrayList<Aircraft> aircraftList() {
		return _aircraft;
	}

	public Demo(Main main) {
		super(main);
	}

	@Override
	public void start() {
		_ordersBox = new lib.OrdersBox(4, window.height() - 120, window.width()-8, 116, 6);
		_aircraft = new java.util.ArrayList<Aircraft>();
		_airplaneImage = graphics.newImage("gfx" + File.separator + "plane.png");
		lib.ButtonText.Action manual = new lib.ButtonText.Action() {
			@Override
			public void action() {
				toggleManualControl();
			}
		};
		_manualOverrideButton = new lib.ButtonText(" Take Control", manual, (window.width() - 128) / 2, 32, 128, 32, 8, 4);
		_altimeter = new lib.Altimeter(8, 248, 64, 96);
		_timer = 0;
		deselectAircraft();
	}
	
	private void toggleManualControl() {
		if (_selectedAircraft == null) return;
		_selectedAircraft.toggleManualControl();
		_manualOverrideButton.setText( (_selectedAircraft.isManuallyControlled() ? "Remove" : " Take") + " Control");
	}
	
	private void deselectAircraft() {
		if (_selectedAircraft != null && _selectedAircraft.isManuallyControlled()) {
			_selectedAircraft.toggleManualControl();
		}
		_selectedAircraft = null;
		_selectedWaypoint = null; 
		_selectedPathpoint = -1;
		_altimeter.hide();
	}

	@Override
	public void update(double dt) {
		if (_aircraft.size() > 0){
			_main.score().addTime(dt); 
		}
		_ordersBox.update(dt);
		_timer += dt;
		for (Aircraft plane : _aircraft) {
			plane.update(dt);
		}
		checkCollisions(dt);
		for (int i = _aircraft.size()-1; i >=0; i --) {
			if (_aircraft.get(i).isFinished()) {
				if (_aircraft.get(i) == _selectedAircraft) {
					deselectAircraft();
				}
				_aircraft.remove(i);
				_main.score().addFlight();
			}
		}
		_altimeter.update(dt);
		if (_selectedAircraft != null && input.isKeyDown(input.KEY_W)) {
			_selectedAircraft.update(-dt);
		}
		
		flightGenerationTimer += dt;
		if(flightGenerationTimer >= flightGenerationInterval){
			//zero interval to begin next count
			flightGenerationTimer -= flightGenerationInterval;
			if (_aircraft.size() < maxAircraft){
				generateFlight();
			}
			
		}
	}
	
	private void checkCollisions(double dt) {
		for (Aircraft plane : _aircraft) {
			plane.updateCollisions(dt, this);
		}
	}
	
	public void gameOver(Aircraft plane1, Aircraft plane2) {
		_main.closeScene();
		_main.setScene(new GameOver(_main, plane1, plane2));
		_main.score().addGameOver();
	}

	@Override
	public void mousePressed(int key, int x, int y) {
		if (key == input.MOUSE_LEFT) {
			for (Aircraft a : _aircraft) {
				if (a.isMouseOver(x-16, y-16)) {
					_selectedAircraft = a;
					_altimeter.show(_selectedAircraft);
				}
			}
			for (Waypoint w : _waypoints) {
				if (_selectedAircraft != null){
					if (w.isMouseOver(x-16, y-16) && _selectedAircraft.flightPathContains(w) > -1) {
						_selectedWaypoint = w;
						_selectedPathpoint = _selectedAircraft.flightPathContains(w);
					}
				}
			}
		}
		if (key == input.MOUSE_RIGHT) deselectAircraft();
		_altimeter.mousePressed(key, x, y);
	}

	@Override
	public void mouseReleased(int key, int x, int y) {
		if (_selectedAircraft != null && _manualOverrideButton.isMouseOver(x, y)) _manualOverrideButton.act();
		if (key == input.MOUSE_LEFT && _selectedWaypoint != null) {
			for (Waypoint w : _waypoints) {
				if (w.isMouseOver(x-16, y-16)) {
					_selectedAircraft.alterPath(_selectedPathpoint, w);
					_selectedPathpoint = -1;
					_selectedWaypoint = null;
				}
			}
		}
		_altimeter.mouseReleased(key, x, y);
	}

	@Override
	public void keyPressed(int key) {
		
	}

	@Override
	public void keyReleased(int key) {
		switch (key) {
		
			case input.KEY_SPACE :
				toggleManualControl();
			break;
			
			case input.KEY_LCRTL :
				generateFlight();
			break;
			
			case input.KEY_ESCAPE :
				_main.closeScene();
			break;
			
		}
	}

	@Override
	public void draw() {
		graphics.setColour(0, 128, 0);
		graphics.rectangle(false, 16, 16, window.width() - 32, window.height() - 144);
		_ordersBox.draw();
		graphics.setViewport(16, 16, window.width() - 32, window.height() - 144);
		for (Waypoint waypoint : _waypoints) {
			waypoint.draw();
		}
		graphics.setColour(255, 255, 255);
		for (Aircraft aircraft : _aircraft) {
			aircraft.draw();
		}
		
		if (_selectedAircraft != null) {
			// Flight Path
			_selectedAircraft.drawFlightPath();
			graphics.setColour(0, 128, 0);
			// Override Button
			graphics.setColour(0, 0, 0);
			graphics.rectangle(true, (window.width() - 128) / 2, 16, 128, 32);
			graphics.setColour(0, 128, 0);
			graphics.rectangle(false, (window.width() - 128) / 2, 16, 128, 32);
			_manualOverrideButton.draw();
			// Change Altitude
		}
		
		graphics.setViewport();
		graphics.setColour(0, 128, 0);
		
		_altimeter.draw();
		
		int hours = (int)(_timer / (60 * 60));
		int minutes = (int)(_timer / 60);
		minutes %= 60;
		double seconds = _timer % 60;
		java.text.DecimalFormat df = new java.text.DecimalFormat("00.00");
		String timePlayed = String.format("%d:%02d:", hours, minutes) + df.format(seconds); 
		graphics.print(timePlayed, window.width() - (timePlayed.length() * 8), 0);
		int planes = _aircraft.size();
		graphics.print(String.valueOf(_aircraft.size()) + " plane" + (planes == 1 ? "" : "s") + " in the sky.", 256, 0);
		graphics.print("Score: " + _main.score().calculate(), 0, 0);
	}
	
	private void generateFlight() {
		// Origin and Destination
		int o = randInt(0, _locationWayPoints.length);
		int d = randInt(0, _locationWayPoints.length);
		while (d == o){
			d = randInt(0, _locationWayPoints.length);
		}
		String originName = _locationNames[o];
		String destinationName = _locationNames[d];
		
/*		int side = randInt(0,1);
		switch (side){
		case 0://enter from left, leave from right
			break;
		case 1://enter from right, leave from left
			break;
		}*/
		
		Waypoint originPoint = _locationWayPoints[o];
		Waypoint destinationPoint = _locationWayPoints[d];
		

		// Name
		String name = "";
		boolean nameTaken = true;
		while (nameTaken) {
			name = "Flight " + (int)(900 * Math.random() + 100);
			nameTaken = false;
			for (Aircraft a : _aircraft) {
				if (a.name() == name) nameTaken = true;
			}
		}
		// Add to world
		_ordersBox.addOrder("<<< " + name + " incoming from " + originName + " heading towards " + destinationName + ".");
		System.out.println("<<< " + name + " incoming from " + originName + " heading towards " + destinationName + ".");
		Aircraft a = new Aircraft(name, originName, destinationName, originPoint, destinationPoint, _airplaneImage, 32, _waypoints);
		_aircraft.add(a);
	}
	
	private int randInt(int min, int max){
		// generates a random integer between min and max
		// not inclusive of max
		Random rand = new Random();
		return rand.nextInt((max - min)) + min;
	}

	@Override
	public void close() {
		
	}

}