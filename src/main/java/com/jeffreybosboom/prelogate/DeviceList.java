package com.jeffreybosboom.prelogate;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/31/2016
 */
public final class DeviceList extends AbstractList<Device> {
	private static final IdentityHashMap<Device, Byte> MAP = new IdentityHashMap<>(32);
	private static final Device[] TABLE = new Device[32];
	static {
		byte b = 0;
		for (BasicDevice d : BasicDevice.values()) {
			TABLE[b] = d;
			MAP.put(d, b++);
			for (Device r : RotatedDevice.from(d)) {
				TABLE[b] = r;
				MAP.put(r, b++);
			}
		}
	}

	private final byte[] data;
	public DeviceList(List<Device> devices) {
		this.data = new byte[devices.size()];
		for (int i = 0; i < data.length; ++i)
			data[i] = MAP.get(devices.get(i));
	}
	@Override
	public Device get(int index) {
		return TABLE[data[index]];
	}
	@Override
	public int size() {
		return data.length;
	}
	public int internalHashcode() {
		return Arrays.hashCode(data);
	}
}
