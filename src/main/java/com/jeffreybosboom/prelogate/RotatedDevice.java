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

import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/27/2015
 */
public final class RotatedDevice implements Device {
	private final Device device;
	private final byte rotationCount;
	public RotatedDevice(Device device, byte rotationCount) {
		if (device instanceof RotatedDevice)
			throw new IllegalArgumentException("rotating a rotated device: "+device);
		if (rotationCount <= 0 || rotationCount >= 4)
			throw new IllegalArgumentException("bad rotation count: "+rotationCount);
		this.device = device;
		this.rotationCount = rotationCount;
	}

	public static ImmutableSet<Device> from(BasicDevice base) {
		if (base == BasicDevice.EMPTY) return ImmutableSet.of(base);
		Map<List<LaserDirection>, Device> map = new LinkedHashMap<>();
		map.put(LaserDirection.all().map(base::operate).collect(Collectors.toList()), base);
		for (byte i = 1; i <= 3; ++i) {
			RotatedDevice d = new RotatedDevice(base, i);
			map.putIfAbsent(LaserDirection.all().map(d::operate).collect(Collectors.toList()), d);
		}
		return ImmutableSet.copyOf(map.values());
	}

	public Device base() {
		return device;
	}

	@Override
	public LaserDirection operate(LaserDirection inputs) {
		return device.operate(inputs.rotateLeft(rotationCount)).rotateRight(rotationCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final RotatedDevice other = (RotatedDevice)obj;
		if (!Objects.equals(this.device, other.device))
			return false;
		if (this.rotationCount != other.rotationCount)
			return false;
		return true;
	}
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + Objects.hashCode(this.device);
		hash = 83 * hash + this.rotationCount;
		return hash;
	}
	@Override
	public String toString() {
		return String.format("%sr%d", device, rotationCount);
	}
}
