package com.jeffreybosboom.prelogate;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/27/2015
 */
public interface Device {
	public LaserDirection operate(LaserDirection inputs);
}
