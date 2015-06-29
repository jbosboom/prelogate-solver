package com.jeffreybosboom.prelogate;

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
	},
	WALL {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(false, false, false, false);
		}
	},
	MIRROR {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right(), inputs.up(), false, false);
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
	},
	AND {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right() && inputs.left(), false, false, false);
		}
	},
	OR {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right() || inputs.left(), false, false, false);
		}
	},
	XOR {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			return LaserDirection.make(inputs.right() ^ inputs.left(), false, false, false);
		}
	},
	IF {
		@Override
		public LaserDirection operate(LaserDirection inputs) {
			boolean active = inputs.left() || inputs.right();
			return LaserDirection.make(active && inputs.down(), false, active && inputs.up(), false);
		}
	};
}
