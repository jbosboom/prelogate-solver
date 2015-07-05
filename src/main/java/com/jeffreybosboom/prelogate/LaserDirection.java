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

import java.util.Arrays;
import java.util.stream.Stream;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/27/2015
 */
public final class LaserDirection {
	private final byte data;
	private static final LaserDirection[] CACHE = new LaserDirection[2*2*2*2];
	static {
		for (byte i = 0; i < CACHE.length; ++i)
			CACHE[i] = new LaserDirection(i);
	}
	private LaserDirection(byte data) {
		this.data = data;
	}
	public static LaserDirection make(boolean up, boolean right, boolean down, boolean left) {
		int i = (up ? 1 : 0) << Direction.UP.ordinal() |
				(right ? 1 : 0) << Direction.RIGHT.ordinal() |
				(down ? 1 : 0) << Direction.DOWN.ordinal() |
				(left ? 1 : 0) << Direction.LEFT.ordinal();
		return CACHE[i];
	}
	public static Stream<LaserDirection> all() {
		return Arrays.stream(CACHE);
	}

	public boolean up() {
		return get(Direction.UP);
	}
	public boolean right() {
		return get(Direction.RIGHT);
	}
	public boolean down() {
		return get(Direction.DOWN);
	}
	public boolean left() {
		return get(Direction.LEFT);
	}
	public boolean get(Direction d) {
		return (data & 1 << d.ordinal()) != 0;
	}

	public LaserDirection set(Direction d, boolean value) {
		//TODO: branchless?
		return value ? set(d) : clear(d);
	}
	public LaserDirection set(Direction d) {
		return CACHE[data | (1 << d.ordinal())];
	}
	public LaserDirection clear(Direction d) {
		return CACHE[data & ~(1 << d.ordinal())];
	}
	public LaserDirection rotateRight() {
		return make(left(), up(), right(), down());
	}
	public LaserDirection rotateRight(int count) {
		LaserDirection r = this;
		for (int i = 0; i < count; ++i)
			r = r.rotateRight();
		return r;
	}
	public LaserDirection rotateLeft() {
		return make(right(), down(), left(), up());
	}
	public LaserDirection rotateLeft(int count) {
		LaserDirection r = this;
		for (int i = 0; i < count; ++i)
			r = r.rotateLeft();
		return r;
	}

	@Override
	public String toString() {
		return (up() ? "U" : "") + (right() ? "R" : "") + (down() ? "D" : "") + (left() ? "L" : "");
	}
}
