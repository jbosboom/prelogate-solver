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
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/27/2015
 */
public interface Device {
	public LaserDirection operate(LaserDirection inputs);

	/**
	 * Returns this device's inputs (directions which influence this device's
	 * output when changed).
	 * @return
	 */
	public default Set<Direction> inputs() {
		return Arrays.stream(Direction.values()).filter(d ->
				LaserDirection.all().anyMatch(l -> !operate(l.set(d, true)).equals(operate(l.set(d, false)))))
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(Direction.class)));
	}
	/**
	 * Returns this device's outputs (directions that change when this device's
	 * inputs are modified).
	 * @return
	 */
	public default Set<Direction> outputs() {
		LaserDirection[] possible = LaserDirection.all().map(this::operate).toArray(LaserDirection[]::new);
		EnumSet<Direction> outputs = EnumSet.noneOf(Direction.class);
		for (Direction d : Direction.values())
			if (Arrays.stream(possible).map(l -> l.get(d)).distinct().count() > 1)
				outputs.add(d);
		return outputs;
	}
}
