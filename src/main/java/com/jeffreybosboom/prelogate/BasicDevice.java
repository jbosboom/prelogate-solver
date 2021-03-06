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

import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/27/2015
 */
public enum BasicDevice implements Device {
	EMPTY {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.down(), inputs.left(), inputs.up(), inputs.right());
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(EnumSet.allOf(Direction.class));
		}
		@Override
		public Set<Direction> outputs() {
			return inputs();
		}
	},
	WALL {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(false, false, false, false);
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(EnumSet.noneOf(Direction.class));
		}
		@Override
		public Set<Direction> outputs() {
			return Sets.immutableEnumSet(EnumSet.noneOf(Direction.class));
		}
	},
	MIRROR {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right(), inputs.up(), false, false);
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(Direction.UP, Direction.RIGHT);
		}
		@Override
		public Set<Direction> outputs() {
			return Sets.immutableEnumSet(Direction.UP, Direction.RIGHT);
		}
	},
	SPLITTER {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.down() || inputs.right(),
					inputs.left() || inputs.up(),
					inputs.up() || inputs.left(),
					inputs.right() || inputs.down());
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(EnumSet.allOf(Direction.class));
		}
		@Override
		public Set<Direction> outputs() {
			return inputs();
		}
	},
//	MIXER {
//		@Override
//		public LaserDirection operate(LaserDirection inputs) {
//			return LaserDirection.make(inputs.right() || inputs.left(), false, false, false);
//		}
//	},
	DIFFUSER {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right() || inputs.down() || inputs.left(),
					inputs.up() || inputs.down() || inputs.left(),
					inputs.up() || inputs.right() || inputs.left(),
					inputs.up() || inputs.right() || inputs.down());
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(EnumSet.allOf(Direction.class));
		}
		@Override
		public Set<Direction> outputs() {
			return inputs();
		}
	},
	AND {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right() && inputs.left(), false, false, false);
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(Direction.LEFT, Direction.RIGHT);
		}
		@Override
		public Set<Direction> outputs() {
			return Sets.immutableEnumSet(Direction.UP);
		}
	},
	OR {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right() || inputs.left(), false, false, false);
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(Direction.LEFT, Direction.RIGHT);
		}
		@Override
		public Set<Direction> outputs() {
			return Sets.immutableEnumSet(Direction.UP);
		}
	},
	XOR {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right() ^ inputs.left(), false, false, false);
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(Direction.LEFT, Direction.RIGHT);
		}
		@Override
		public Set<Direction> outputs() {
			return Sets.immutableEnumSet(Direction.UP);
		}
	},
	IF {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			boolean active = inputs.left() || inputs.right();
			return LaserDirection.make(active && inputs.down(), false, active && inputs.up(), false);
		}
		@Override
		public Set<Direction> inputs() {
			return Sets.immutableEnumSet(EnumSet.allOf(Direction.class));
		}
		@Override
		public Set<Direction> outputs() {
			return Sets.immutableEnumSet(Direction.UP, Direction.DOWN);
		}
	};
}
