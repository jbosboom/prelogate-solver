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
