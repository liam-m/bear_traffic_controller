package cls;

import java.io.File;
import java.util.ArrayList;

import scn.Demo;
import lib.RandomNumber;
import lib.jog.audio;
import lib.jog.graphics;
import lib.jog.input;
import lib.jog.window;

/**
 * <h1>Aircraft</h1>
 * <p>Represents an aircraft. Calculates velocity, route-following, etc.</p>
 */
public class Aircraft {

	public final static int RADIUS = 16; // The physical size of the plane in pixels. This determines crashes.
	public final static int MOUSE_LENIANCY = 32;  // How far away (in pixels) the mouse can be from the plane but still select it.
	public final static int COMPASS_RADIUS = 64; // How large to draw the bearing circle.
	public static int separationRule = 64; // How much the plane can turn per second - in radians.
	private double turnSpeed;
	private Vector position;
	private Vector velocity;
	private boolean isManuallyControlled;
	private String flightName; // Unique and generated randomly
	public Vector currentTarget; // The position the plane is currently flying towards (if not manually controlled).
	private double manualBearingTarget;

	private int currentRouteStage;
	private graphics.Image image; // The plane image
	private boolean hasFinished; // If destination is airport, must be given a land command bnefore it returns True //#Should this be non-inline?
	public boolean is_waiting_to_land;// If the destination is the airport, True until land() is called. //#Should this be non-inline?
	private double currentlyTurningBy;// In radians
	/**
	 * Holds a list of planes currently in violation of separation rules with this plane
	 */
	private java.util.ArrayList<Aircraft> planesTooNear = new java.util.ArrayList<Aircraft>();
	private int altitudeState;// Whether the plane is climbing or falling
	private int altitudeChangeSpeed; // The speed to climb or fall by. Set in switch statement below.
	private double timeOfCreation;// Used to calculate how long an aircraft spent in the airspace
	/**
	 * Used to get (system) time when an aircraft was created.
	 * @return Time when aircraft was created.
	 */
	public double getTimeOfCreation() {
		return timeOfCreation;
	}
	/**
	 * Used when calculating when a label representing the score a particular plane scored should disappear
	 */
	private double timeOfDeparture;

	/**
	 * Used to get (system) time when an aircraft successfully departed.
	 * @return Time when aircraft departed.
	 */
	public double getTimeOfDeparture() {
		return timeOfDeparture;
	}
	/**
	 * Used outside of Aircraft class to assign a (system) time to a plane that successfully left airspace
	 * @param departureTime (system time when a plane departed)
	 */
	public void setTimeOfDeparture(double departureTime) {
		timeOfDeparture = departureTime;
	}

	/**
	 * Optimal time a plane needs to reach its exit point
	 */
	private double optimalTime;
	/**
	 * Getter for optimal time.
	 * @return Optimal Optimal time for an aircraft to complete its path.
	 */
	public double getOptimalTime() {
		return optimalTime;
	}
	/**
	 * Static ints for use where altitude state is to be changed.
	 */
	public static final int ALTITUDE_CLIMB = 1;
	public static final int ALTITUDE_FALL = -1;
	public static final int ALTITUDE_LEVEL = 0;

	/**
	 * This method returns multiplier bonus to reward players for fast and efficient management of planes
	 * @param optimalTime - Ideal time - Ambitious goal   
	 * @param timeTaken - Total time aircraft has spent in the airspace. 
	 * @return 2 for very efficient, alternatively 1.5 
	 */
	private boolean collisionWarningSoundFlag = false;

	private final static audio.Sound WARNING_SOUND = audio.newSoundEffect("sfx" + File.separator + "beep.ogg");// Used during separation violation 
	
	/**
	 * Each plane has its own base score that user improves their score by when
	 * a plane successfully leaves the airspace.
	 */
	private int baseScore;
	private int individualScore;

	/**
	 * This variable increases the multiplierVariable when a plane successfully leaves the airspace.
	 */
	private int additionToMultiplier = 1;

	/**
	 * Used to get a base score per plane outside of Aircraft class.
	 * @return baseScore
	 */
	public int getBaseScore() {
		return baseScore;
	}

	/**
	 * Gets the score for a specific aircraft.
	 */
	public int getScore() {
		return individualScore;
	}

	/**
	 * Sets the score for a specific aircraft.
	 */
	public void setScore(int score) {
		individualScore = score;
	}

	/**
	 * Used to get a additionToMultiplier outside of Aircraft class.
	 * @return additionToMultiplier
	 */
	public int getAdditionToMultiplier() {
		return additionToMultiplier;
	}

	/**
	 * Used to set additionToMultiplier outside of Aircraft class.
	 * @param number
	 */
	public void setAdditionToMultiplier(int multiplierLevel) {
		switch (multiplierLevel) {
		case 1:
			additionToMultiplier = 64;
			break;
		case 2:
			additionToMultiplier = 32;
			break;
		case 3:
			additionToMultiplier = 32;
			break;
		case 4:
			additionToMultiplier = 16;
			break;
		case 5:
			additionToMultiplier = 8;
			break;
		}
	}
	
	private FlightPlan flight_plan;

	/**
	 * Constructor for an aircraft.
	 * @param name the name of the flight.
	 * @param nameOrigin the name of the location from which the plane hails.
	 * @param nameDestination the name of the location to which the plane is going.
	 * @param originPoint the point to initialise the plane.
	 * @param destinationPoint the end point of the plane's route.
	 * @param img the image to represent the plane.
	 * @param speed the speed the plane will travel at.
	 * @param sceneWaypoints the waypoints on the map.
	 * @param difficulty the difficulty the game is set to
	 */
	public Aircraft(String name, String nameDestination, String nameOrigin, Waypoint destinationPoint, Waypoint originPoint, graphics.Image img, double speed, Waypoint[] sceneWaypoints, int difficulty) {
		flightName = name;
		
		flight_plan = new FlightPlan(findGreedyRoute(originPoint, destinationPoint, sceneWaypoints), nameOrigin, nameDestination, originPoint, destinationPoint);
		
		image = img;
		timeOfCreation = System.currentTimeMillis() / 1000; // System time when aircraft was created in seconds.

		position = originPoint.getLocation();
		
		if (originPoint.getLocation() == Demo.airport.getLocation()) {
			position = position.add(new Vector(-100, -70, 0)); // Start at departures
		}
		int altitudeOffset = RandomNumber.randInclusiveInt(0, 1) == 0 ? 28000 : 30000;
		position = position.add(new Vector(0, 0, altitudeOffset));

		// Calculate initial velocity (direction)
		currentTarget = flight_plan.getRoute()[0].getLocation();
		double x = currentTarget.getX() - position.getX();
		double y = currentTarget.getY() - position.getY();
		velocity = new Vector(x, y, 0).normalise().scaleBy(speed);

		isManuallyControlled = false;
		hasFinished = false;
		is_waiting_to_land = flight_plan.getDestination().equals(Demo.airport.getLocation());
		currentRouteStage = 0;
		currentlyTurningBy = 0;
		manualBearingTarget = Double.NaN;

		// Speed up plane for higher difficulties
		switch (difficulty) {
		// Adjust the aircraft's attributes according to the difficulty of the parent scene
		// 0 has the easiest attributes (slower aircraft, more forgiving separation rules)
		// 2 has the hardest attributes (faster aircraft, least forgiving separation rules)
		case Demo.DIFFICULTY_EASY:
			separationRule = 64;
			turnSpeed = Math.PI / 4;
			altitudeChangeSpeed = 500;
			baseScore = 60;
			optimalTime = flight_plan.getTotalDistance() / speed;
		break;

		case Demo.DIFFICULTY_MEDIUM:
			separationRule = 96;
			velocity = velocity.scaleBy(2);
			turnSpeed = Math.PI / 3;
			altitudeChangeSpeed = 300;
			baseScore = 150;
			optimalTime = flight_plan.getTotalDistance() / (speed * 2);
		break;
			
		case Demo.DIFFICULTY_HARD:
			separationRule = 128;
			velocity = velocity.scaleBy(3);
			// At high velocities, the aircraft is allowed to turn faster - this helps keep the aircraft on track.
			turnSpeed = Math.PI / 2;
			altitudeChangeSpeed = 200;
			baseScore = 300;
			additionToMultiplier = 3;
			optimalTime = flight_plan.getTotalDistance() / (speed * 3);
		break;

		default:
			Exception e = new Exception("Invalid Difficulty : " + difficulty + ".");
			e.printStackTrace();
		}
	}

	public Vector position() {
		return position;
	}

	public String getName() {
		return flightName;
	}

	public boolean isFinished() { // Returns whether the plane has reached its destination
		return hasFinished;
	}

	public boolean isManuallyControlled() {
		return isManuallyControlled;
	}

	public int getAltitudeState() {
		return altitudeState;
	}

	/**
	 * Calculates the angle from the plane's position, to its current target.
	 * @return an angle in radians to the plane's current target.
	 */
	private double angleToTarget() {
		if (isManuallyControlled) {
			return (manualBearingTarget == Double.NaN) ? getBearing(): manualBearingTarget;
		} else {
			return Math.atan2(currentTarget.getY() - position.getY(), currentTarget.getX() - position.getX());
		}
	}

	//#Needed? - seems obvious
	/**
	 * Checks whether the plane lies outside of the airspace.
	 * @return true, if the plane is out of the airspace. False, otherwise.
	 */
	public boolean isOutOfBounds() {
		double x = position.getX();
		double y = position.getY();
		return (x < RADIUS || x > window.width() + RADIUS - 32 || y < RADIUS || y > window.height() + RADIUS - 176);
	}

	public double getBearing() {
		return Math.atan2(velocity.getY(), velocity.getX());
	}

	public double getSpeed() {
		return velocity.magnitude();
	}

	public boolean isAt(Vector point) {
		double dy = point.getY() - position.getY();
		double dx = point.getX() - position.getX();
		return dy * dy + dx * dx < 4 * 4;
	}

	public boolean isTurningLeft() {
		return currentlyTurningBy < 0;
	}

	public boolean isTurningRight() {
		return currentlyTurningBy > 0;
	}

	public int flightPathContains(Waypoint waypoint) {
		int index = -1;
		for (int i = 0; i < flight_plan.getRoute().length; i++) {
			if (flight_plan.getRoute()[i] == waypoint) index = i;
		}
		return index;
	}
	
	/**
	 * Edits the plane's path by changing the waypoint it will go to at a certain stage in its route.
	 * @param routeStage the stage at which the new waypoint will replace the old.
	 * @param newWaypoint the new waypoint to travel to.
	 */
	public void alterPath(int routeStage, Waypoint newWaypoint) {
		if (routeStage > -1) {
			flight_plan.alterPath(routeStage, newWaypoint);
			if (!isManuallyControlled)
				resetBearing();
			if (routeStage == currentRouteStage) {
				currentTarget = newWaypoint.getLocation();
				turnTowardsTarget(0);
			}
		}
	}

	public boolean isMouseOver(int mx, int my) {
		double dx = position.getX() - mx;
		double dy = position.getY() - my;
		return dx * dx + dy * dy < MOUSE_LENIANCY * MOUSE_LENIANCY;
	}

	/**
	 * Calls {@link isMouseOver()} using {@link input.mouseX()} and {@link input.mouseY()} as the arguments.
	 * @return true, if the mouse is close enough to this plane. False, otherwise.
	 */
	public boolean isMouseOver() {
		return isMouseOver(input.mouseX() - Demo.airspace_view_offset_x, input.mouseY() - Demo.airspace_view_offset_y);
	}

	private boolean is_landing = false;
	
	public boolean isAtDestination() {
		if (flight_plan.getDestination().equals(Demo.airport.getLocation())) {
			return Demo.airport.isWithinArrivals(position, false);
		} else {
			return isAt(flight_plan.getDestination());
		}
	}

	/**
	 * Updates the plane's position and bearing, the stage of its route, and whether it has finished its flight.
	 * @param time_difference
	 */
	public void update(double time_difference) {
		if (hasFinished) return;
		if (is_landing) {
			if (position.getZ() > 100) {
				position.setZ(position.getZ() - 2529 * time_difference);
			} else {
				Demo.airport.is_active = false;
				hasFinished = true;
			}
		} else {
			switch (altitudeState) {
			case -1:
				fall();
				break;
			case 0:
				break;
			case 1:
				climb();
				break;
			}
		}

		// Update position
		Vector dv = velocity.scaleBy(time_difference);
		position = position.add(dv);

		currentlyTurningBy = 0;

		// Update target
		
		if (currentTarget.equals(flight_plan.getDestination()) && isAtDestination()) { // At finishing point
			if (!is_waiting_to_land) { // Ready to land
				hasFinished = true;
				if (flight_plan.getDestination().equals(Demo.airport.getLocation())) { // Landed at airport
					Demo.airport.is_active = false;
				}
			}
		} else if (isAt(currentTarget)) {
			currentRouteStage++;
			// Next target is the destination if you're at the end of the plan, otherwise it's the next waypoint
			currentTarget = currentRouteStage >= flight_plan.getRoute().length ? flight_plan.getDestination() : flight_plan.getRoute()[currentRouteStage].getLocation();
		}

		// Update bearing
		if (Math.abs(angleToTarget() - getBearing()) > 0.01) {
			turnTowardsTarget(time_difference);
		}
	}

	public void turnLeft(double time_difference) {
		turnBy(time_difference * -turnSpeed);
		manualBearingTarget = Double.NaN;
	}

	public void turnRight(double time_difference) {
		turnBy(time_difference * turnSpeed);
		manualBearingTarget = Double.NaN;
	}

	/**
	 * Turns the plane by a certain angle (in radians). Positive angles turn the plane clockwise.
	 * @param angle the angle by which to turn.
	 */
	private void turnBy(double angle) {
		currentlyTurningBy = angle;
		double cosA = Math.cos(angle);
		double sinA = Math.sin(angle);
		double x = velocity.getX();
		double y = velocity.getY();
		velocity = new Vector(x*cosA - y*sinA, y*cosA + x*sinA, velocity.getZ());
	}

	private void turnTowardsTarget(double time_difference) {
		// Get difference in angle
		double angleDifference = (angleToTarget() % (2 * Math.PI)) - (getBearing() % (2 * Math.PI));
		boolean crossesPositiveNegativeDivide = angleDifference < -Math.PI * 7 / 8;
		// Correct difference
		angleDifference += Math.PI;
		angleDifference %= (2 * Math.PI);
		angleDifference -= Math.PI;
		// Get which way to turn.
		int angleDirection = (int) (angleDifference /= Math.abs(angleDifference));
		if (crossesPositiveNegativeDivide)
			angleDirection *= -1;
		double angleMagnitude = Math.min(Math.abs((time_difference * turnSpeed)), Math.abs(angleDifference));
		turnBy(angleMagnitude * angleDirection);
	}

	/**
	 * Draws the plane and any warning circles if necessary.
	 */
	public void draw(int highlightedAltitude) {
		double alpha;
		if (position.getZ() >= 28000 && position.getZ() <= 28500) {
			alpha = highlightedAltitude == 28000 ? 255 : 128;
		} else if (position.getZ() <= 30000 && position.getZ() >= 28500) {
			alpha = highlightedAltitude == 30000 ? 255 : 128;
		} else {
			alpha = 128;
		}
		double scale = 2*(position.getZ()/30000);
		graphics.setColour(128, 128, 128, alpha);
		graphics.draw(image, scale, position.getX()-image.width()/2, position.getY()-image.height()/2, getBearing(), 8, 8);
		graphics.setColour(128, 128, 128, alpha/2.5);
		graphics.print(String.format("%.0f", position.getZ()) + "�", position.getX()+8, position.getY()-8);
		drawWarningCircles();
	}

	/**
	 * Draws the compass around this plane - Used for manual control //#Do we still have manual control 
	 */
	public void drawCompass() {
		graphics.setColour(graphics.green);
		Double xpos = position.getX()-image.width()/2; // Centre position of aircraft
		Double ypos = position.getY()-image.height()/2;
		graphics.circle(false, xpos + Demo.airspace_view_offset_x, ypos + Demo.airspace_view_offset_y, COMPASS_RADIUS, 30);
		for (int i = 0; i < 360; i += 60) {
			double r = Math.toRadians(i - 90);
			double x = xpos + Demo.airspace_view_offset_x + (1.1 * COMPASS_RADIUS * Math.cos(r));
			double y = ypos + Demo.airspace_view_offset_y - 2 + (1.1 * COMPASS_RADIUS * Math.sin(r));
			if (i > 170) x -= 24;
			if (i == 180) x += 12;
			graphics.print(String.valueOf(i), x, y);
		}
		double x, y;
		if (isManuallyControlled && input.isMouseDown(input.MOUSE_RIGHT)) {
			// Draw new bearing
			graphics.setColour(graphics.green_transp);
			double r = Math.atan2(input.mouseY() - position.getY(), input.mouseX() - position.getX());
			x = Demo.airspace_view_offset_x + xpos + (COMPASS_RADIUS * Math.cos(r));
			y = Demo.airspace_view_offset_y + ypos + (COMPASS_RADIUS * Math.sin(r));
			graphics.line(xpos + Demo.airspace_view_offset_x, ypos + Demo.airspace_view_offset_y, x, y);
			graphics.line(xpos + Demo.airspace_view_offset_x - 1, ypos + Demo.airspace_view_offset_y, x, y);
			graphics.line(xpos + Demo.airspace_view_offset_x, ypos + Demo.airspace_view_offset_y - 1, x, y);
			graphics.line(xpos + Demo.airspace_view_offset_x + 1, ypos + Demo.airspace_view_offset_y, x, y);
			graphics.line(xpos + Demo.airspace_view_offset_x + 1, ypos + Demo.airspace_view_offset_y + 1, x, y);
			graphics.setColour(0, 128, 0, 16);
		}
		x = Demo.airspace_view_offset_x + xpos + (COMPASS_RADIUS * Math.cos(getBearing()));
		y = Demo.airspace_view_offset_y + ypos + (COMPASS_RADIUS * Math.sin(getBearing()));
		// Draw current bearing
		graphics.line(xpos + Demo.airspace_view_offset_x, ypos + Demo.airspace_view_offset_y, x, y);
		graphics.line(xpos + Demo.airspace_view_offset_x - 1, ypos + Demo.airspace_view_offset_y, x, y);
		graphics.line(xpos + Demo.airspace_view_offset_x, ypos + Demo.airspace_view_offset_y - 1, x, y);
		graphics.line(xpos + Demo.airspace_view_offset_x + 1, ypos + Demo.airspace_view_offset_y, x, y);
		graphics.line(xpos + Demo.airspace_view_offset_x + 1, ypos + Demo.airspace_view_offset_y + 1, x, y);

	}

	/**
	 * Draws warning circles around this plane and any others that are too near.
	 */
	private void drawWarningCircles() {
		for (Aircraft plane : planesTooNear) {
			Vector midPoint = position.add(plane.position).scaleBy(0.5);
			double radius = position.sub(midPoint).magnitude() * 2;
			graphics.setColour(graphics.red);
			graphics.circle(false, midPoint.getX(), midPoint.getY(), radius);
		}
	}

	/**
	 * Draws lines starting from the plane, along its flight path to its destination.
	 */
	public void drawFlightPath(boolean is_selected) {
		if (is_selected) {
			graphics.setColour(0, 128, 128);
		} else {
			graphics.setColour(0, 128, 128, 128);
		}

		Waypoint[] route = flight_plan.getRoute();
		Vector destination = flight_plan.getDestination();
		if (currentTarget != destination) {
			graphics.line(position.getX()-image.width()/2, position.getY()-image.height()/2, route[currentRouteStage].getLocation().getX(), route[currentRouteStage].getLocation().getY());
		}
		for (int i = currentRouteStage; i < route.length-1; i++) {
			graphics.line(route[i].getLocation().getX(), route[i].getLocation().getY(), route[i+1].getLocation().getX(), route[i+1].getLocation().getY());	
		}
		if (currentTarget == destination) {
			graphics.line(position.getX()-image.width()/2, position.getY()-image.height()/2, destination.getX(), destination.getY());
		} else {
			graphics.line(route[route.length-1].getLocation().getX(), route[route.length-1].getLocation().getY(), destination.getX(), destination.getY());
		}
	}

	/**
	 * Visually represents the waypoint being moved.
	 * @param mouseX current position of mouse
	 * @param mouseY current position of mouse
	 */
	public void drawModifiedPath(int modified, double mouseX, double mouseY) {
		graphics.setColour(0, 128, 128, 128);
		Waypoint[] route = flight_plan.getRoute();
		Vector destination = flight_plan.getDestination();
		if (currentRouteStage > modified - 1) {
			graphics.line(position().getX(), position().getY(), mouseX, mouseY);
		} else {
			graphics.line(route[modified-1].getLocation().getX(), route[modified-1].getLocation().getY(), mouseX, mouseY);
		}
		if (currentTarget == destination) {
			graphics.line(mouseX, mouseY, destination.getX(), destination.getY());
		} else {
			int index = modified + 1;
			if (index == route.length) { // Modifying final waypoint in route
				// Line drawn to final waypoint
				graphics.line(mouseX, mouseY, destination.getX(), destination.getY());
			} else {
				graphics.line(mouseX, mouseY, route[index].getLocation().getX(), route[index].getLocation().getY());
			}
		}
	}

	/**
	 * Creates a sensible route from an origin to a destination from an array of
	 * waypoints. Waypoint costs are considered according to distance from
	 * current aircraft location Costs are further weighted by distance from
	 * waypoint to destination.
	 * @param origin the waypoint from which to begin.
	 * @param destination the waypoint at which to end.
	 * @param waypoints the waypoints to be used.
	 * @return a sensible route between the origin and the destination, using a sensible amount of waypoint.
	 */
	public Waypoint[] findGreedyRoute(Waypoint origin, Waypoint destination, Waypoint[] waypoints) {
		// To hold the route as we generate it.
		ArrayList<Waypoint> selectedWaypoints = new ArrayList<Waypoint>();
		// Initialise the origin as the first point in the route.
		// SelectedWaypoints.add(origin);
		// To track our position as we generate the route. Initialise to the start of the route
		Waypoint currentPos = origin;

		// To track the closest next waypoint
		double cost = Double.MAX_VALUE;
		Waypoint cheapest = null;
		// To track if the route is complete
		boolean atDestination = false;

		while (!atDestination) {
			for (Waypoint point : waypoints) {
				boolean skip = false;

				for (Waypoint routePoints : selectedWaypoints) {
					// Check we have not already selected the waypoint
					// If we have, skip evaluating the point
					// This protects the aircraft from getting stuck looping between points
					if (routePoints.getLocation().equals(point.getLocation())) {
						skip = true; // Flag to skip
						break; // No need to check rest of list, already found a match.
					}
				}
				// Do not consider the waypoint we are currently at or the origin
				// Do not consider offscreen waypoints which are not the destination
				// Also skip if flagged as a previously selected waypoint
				if (skip | point.getLocation().equals(currentPos.getLocation()) | point.getLocation().equals(origin.getLocation())
						| (point.isEntryOrExit() && !(point.getLocation().equals(destination.getLocation())))) {
					skip = false; // Reset flag
					continue;
				} else {
					/*
					 * Get cost of visiting waypoint 
					 * Compare cost vs current cheapest 
					 * If smaller, replace
					 */
					if (point.getCost(currentPos) + 0.5 * Waypoint.getCostBetween(point, destination) < cost) {
						// Cheaper route found, update
						cheapest = point;
						cost = point.getCost(currentPos) + 0.5 * Waypoint.getCostBetween(point, destination);
					}
				}

			} // End for - evaluated all waypoints
			// The cheapest waypoint must have been found
			assert cheapest != null : "The cheapest waypoint was not found";

			if (cheapest.getLocation().equals(destination.getLocation())) {
				/*
				 * Route has reached destination, break out of while loop
				 */
				atDestination = true;
			}
			// Update the selected route
			// Consider further points in route from the position of the selected point
			selectedWaypoints.add(cheapest);
			currentPos = cheapest;
			// Resaturate cost for next loop
			cost = Double.MAX_VALUE;

		} // End while
		// Create a Waypoint[] to hold the new route
		Waypoint[] route = new Waypoint[selectedWaypoints.size()];
		// Fill route with the selected waypoints
		for (int i = 0; i < selectedWaypoints.size(); i++) {
			route[i] = selectedWaypoints.get(i);
		}
		return route;
	}

	/**
	 * Updates the amount of planes that are too close, violating the separation rules, and also checks for crashes.
	 * @param time_difference the time elapsed since the last frame.
	 * @param scene the game scene object.
	 * @return 0 if no collisions, 1 if separation violation, 2 if crash //#What does -1 do
	 */
	public int updateCollisions(double time_difference,	ArrayList<Aircraft> aircraftList, Score score) {
		planesTooNear.clear();
		for (int i = 0; i < aircraftList.size(); i++) {
			Aircraft plane = aircraftList.get(i);
			if (plane != this && isWithin(plane, RADIUS)) {
				hasFinished = true;
				return i;
			} else if (plane != this && isWithin(plane, separationRule)) {
				planesTooNear.add(plane);
				score.increaseMeterFill(-1); // Punishment for breaching separation rules (applies to all aircraft involved - usually 2)
				if (collisionWarningSoundFlag == false) {
					collisionWarningSoundFlag = true;
					WARNING_SOUND.play();
				}
			}
		}
		if (planesTooNear.isEmpty()) {
			collisionWarningSoundFlag = false;
		}
		return -1;//#What does -1 do
	}

	/**
	 * Checks whether an aircraft is within a certain distance from this one.
	 * @param aircraft the aircraft to check.
	 * @param distance the distance within which to care about.
	 * @return true, if the aircraft is within the distance. False, otherwise.
	 */
	private boolean isWithin(Aircraft aircraft, int distance) {
		double dx = aircraft.position().getX() - position.getX();
		double dy = aircraft.position().getY() - position.getY();
		double dz = aircraft.position().getZ() - position.getZ();
		return dx * dx + dy * dy + dz * dz < distance * distance;
	}

	public void toggleManualControl() {
		isManuallyControlled = !isManuallyControlled;
		if (isManuallyControlled) {
			setBearing(getBearing());
		} 
		else {
			resetBearing();
		}
	}

	public void setBearing(double newHeading) {
		manualBearingTarget = newHeading;
	}

	private void resetBearing() {
		if (currentRouteStage < flight_plan.getRoute().length & flight_plan.getRoute()[currentRouteStage] != null) {
			currentTarget = flight_plan.getRoute()[currentRouteStage].getLocation();
		}
		turnTowardsTarget(0);
	}


	public void climb() {
		if (position.getZ() < 30000 && altitudeState == ALTITUDE_CLIMB)
			changeAltitude(altitudeChangeSpeed);
		if (position.getZ() >= 30000) {
			changeAltitude(0);
			altitudeState = ALTITUDE_LEVEL;
			position = new Vector(position.getX(), position.getY(), 30000);
		}
	}

	public void fall() {
		if (position.getZ() > 28000 && altitudeState == ALTITUDE_FALL)
			changeAltitude(-altitudeChangeSpeed);
		if (position.getZ() <= 28000) {
			changeAltitude(0);
			altitudeState = ALTITUDE_LEVEL;
			position = new Vector(position.getX(), position.getY(), 28000);
		}
	}

	public void land() {
		is_waiting_to_land = false;
		is_landing = true;
		Demo.airport.is_active = true;
	}

	public void takeOff() {
		Demo.airport.is_active = true;
		Demo.takeOffSequence(this);
	}

	private void changeAltitude(int height) {
		// velocity = velocity.add(new Vector(0,0, height));
		velocity.setZ(height);
	}

	
	public void setAltitudeState(int state) {
		this.altitudeState = state;// Either climbing or falling
	}

	/**
	 * Checks if an aircraft is close to an its parameter (entry point).
	 * @param position of a waypoint
	 * @return True it if it close
	 */
	public boolean isCloseToEntry(Vector position) {
		double x = this.position().getX() - position.getX();
		double y = this.position().getY() - position.getY();
		return ((x*x + y*y) <= (300 * 300));
	}
	
	public FlightPlan getFlightPlan() {
		return flight_plan;
	}

}
