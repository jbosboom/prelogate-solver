/*
 * Copyright 2015 Jeffrey Bosboom.
 * This file is part of prelogate-solver.
 *
 * prelogate-solver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * prelogate-solver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with prelogate-solver.  If not, see <http://www.gnu.org/licenses/>.
 */
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
