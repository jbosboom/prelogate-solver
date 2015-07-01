package com.jeffreybosboom.prelogate;

import com.google.common.math.IntMath;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/27/2015
 */
public enum Direction {
	UP, RIGHT, DOWN, LEFT;
	public Direction rotateRight() {
		return rotateRight(1);
	}
	public Direction rotateRight(int distance) {
		Direction[] values = Direction.values();
		return values[IntMath.mod(ordinal() + distance, values.length)];
	}
	public Direction rotateLeft() {
		return rotateRight(-1);
	}
	public Direction rotateLeft(int distance) {
		return rotateRight(-distance);
	}
	public Direction opposite() {
		return rotateRight(2);
	}
}
