package lib;

import lib.jog.graphics;

/**
 * Class for a visual representation text. It has word wrap enabled 
 * and prints out orders character by character in a retro style. It
 * also has support for delays between lines.
 * @author Huw Taylor
 */
public class TextBox {
	
	protected final int LINES;
	protected final char SEPARATOR = '|';
	public static final char DELAY_START = '{';
	public static final char DELAY_END = '}';
	
	protected double typing_wait; // Time between 'typing' characters
	protected int x, y, width, height;
	protected String[] orders;
	protected int current_order;
	protected double timer, delay_timer;
	protected boolean is_delaying, is_typing;
	protected String buffer;

	/**
	 * Constructor of a TextBox.
	 * @param x the x coordinate to display the box.
	 * @param y the y coordinate to display the box.
	 * @param width the width the box wrap to.
	 * @param height the height of the box.
	 * @param lines the maximum amount of lines to display at a time.
	 */
	public TextBox(int x, int y, int width, int height, int lines) {
		LINES = lines;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		typing_wait = 0.01; // Default 0.01 seconds
		orders = new String[LINES];
		current_order = 0;
		for (int i = 0; i < LINES; i ++) {
			orders[i] = "";
		}
		timer = 0;
		delay_timer = 0;
		is_delaying = false;
		is_typing = false;
		buffer = "";
	}
	
	/**
	 * Changes the speed at which text is 'typed'.
	 * @param delay the new wait in seconds between each character.
	 */
	public void setSpeed(double delay) {
		typing_wait = delay;
	}
	
	/**
	 * Adds a line of text to be displayed.
	 * @param the text to be written.
	 */
	public void addText(String order) {
		// Word Wrap
		if (order.length()*8 > width) {
			String wrappedOrder = order.substring(0, (width/8)-1);
			while (wrappedOrder.charAt(wrappedOrder.length()-1) != ' ') {
				wrappedOrder = wrappedOrder.substring(0, wrappedOrder.length()-1);
			}
			buffer += wrappedOrder + SEPARATOR;
			addText(order.substring(wrappedOrder.length()));
		} else {
			buffer += order + SEPARATOR;
		}
		is_typing = true;
	}

	/**
	 * Adds a delay, making the textbox wait before continuing.
	 * @param duration the length of the delay in seconds.
	 */
	public void delay(double duration) {
		buffer += DELAY_START + String.valueOf(duration) + DELAY_END;
	}
	
	/**
	 * Adds a newline to the textbox.
	 */
	public void newline() {
		newlines(1);
//		_buffer += SEPARATOR;
	}
	
	public void newlines(int number) {
		for (int i = 0; i < number; i++)
			addText(" ");
	}
	
	/**
	 * Accesses how many lines we are currently using.
	 * @return the amount of used lines.
	 */
	protected int linesBeingUsed() {
		for (int i = 0; i < LINES; i ++) {
			if (orders[i] == null || orders[i] == "") {
				return i;
			}
		}
		return LINES;
	}

	/**
	 * Whether we have stopped typing and have no orders queued up.
	 * @return whether the TextBox is up to date.
	 */
	public boolean isUpToDate() {
		return !is_typing;
	}
	
	/**
	 * Updates the timer of the TextBox and contents of box.
	 * @param time_difference time since the last update call.
	 */
	public void update(double time_difference) {
		// Update delay
		if (is_delaying) {
			if (delay_timer <= 0) {
				is_delaying = false;
			} else {
				delay_timer = Math.max(0, delay_timer - time_difference);
				return;
			}
		}
		// Update timer
		timer += time_difference;
		if (timer >= typing_wait) {
			timer -= typing_wait;
			// Finished
			if (buffer.isEmpty()) {
				is_typing = false;
			// Delay
			} else if (buffer.charAt(0) == DELAY_START) {
				buffer = buffer.substring(1);
				is_delaying = true;
				String delay = "";
				while (buffer.charAt(0) != DELAY_END) {
					delay += buffer.charAt(0);
					buffer = buffer.substring(1);
				}
				buffer = buffer.substring(1);
				delay_timer = Double.parseDouble(delay);
			// New Line
			} else if (buffer.charAt(0) == SEPARATOR) {
				current_order += 1;
				buffer = buffer.substring(1);
			} else {
				// Too many lines
				if (current_order >= LINES) {
					ripple();
				}
				orders[current_order] += buffer.substring(0, 1);
				buffer = buffer.substring(1);
			}
		}
	}
	
	/**
	 * Cycle through the lines, removing the first, and moving the rest up by one.
	 */
	protected void ripple() {
		for (int i = 0; i < LINES-1; i ++) {
			orders[i] = orders[i+1];
		}
		orders[LINES-1] = "";
		current_order = Math.max(0, current_order - 1);
	}
	
	/**
	 * Prints the currently available characters of the TextBox.
	 */
	public void draw() {
		graphics.setColour(graphics.green);
		for (int i = 0; i < linesBeingUsed(); i ++) {
			graphics.print(orders[i], x + 4, y + 4 + (i * (height-8) / LINES));
		}
	}

}