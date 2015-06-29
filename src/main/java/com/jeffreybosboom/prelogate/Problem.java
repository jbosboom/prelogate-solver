package com.jeffreybosboom.prelogate;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/27/2015
 */
public final class Problem {
	private final List<List<Set<Device>>> devices;
	private final List<Terminal> terminals;

	public Problem(List<List<Set<Device>>> devices, List<Terminal> terminals) {
		this.devices = devices;
		this.terminals = terminals;
	}

	public List<List<Set<Device>>> devices() {
		return devices;
	}

	public List<Terminal> terminals() {
		return terminals;
	}

	public static Problem fromFile(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path);
		Map<Character, Set<Device>> deviceMap = new HashMap<>();
		Map<Character, Direction> emitterDir = new HashMap<>(), receiverDir = new HashMap<>();
		for (String line = lines.get(0); !line.isEmpty(); lines.remove(0), line = lines.get(0)) {
			String[] tokens = line.split(" ");
			assert tokens[0].length() == 1;
			char symbol = tokens[0].charAt(0);
			ImmutableSet.Builder<Device> devices = ImmutableSet.builder();
			if (tokens[1].equalsIgnoreCase("emitter") || tokens[1].equalsIgnoreCase("receiver")) {
				//emitters and receivers count as walls for propagation purposes
				//TODO: dirtyness is different from walls!
				devices.add(BasicDevice.WALL);
				(tokens[1].equalsIgnoreCase("emitter") ? emitterDir : receiverDir)
						.put(symbol, Direction.valueOf(tokens[2].toUpperCase(Locale.ROOT)));
			} else
				Arrays.stream(tokens).skip(1).forEach(t -> devices.addAll(RotatedDevice.from(BasicDevice.valueOf(t.toUpperCase()))));
			deviceMap.put(symbol, devices.build());
		}
		lines.remove(0);

		List<List<Set<Device>>> playfield = new ArrayList<>();
		Map<Character, Integer> rowcols = new HashMap<>();
		int row = 0;
		for (String line = lines.get(0); !line.isEmpty(); lines.remove(0), line = lines.get(0), ++row) {
			List<Set<Device>> lineDevices = new ArrayList<>(line.length());
			for (int col = 0; col < line.length(); ++col) {
				char c = line.charAt(col);
				lineDevices.add(deviceMap.get(c));
				if (emitterDir.containsKey(c) || receiverDir.containsKey(c))
					rowcols.put(c, (row << 16) | col);
			}
			playfield.add(lineDevices);
		}
		lines.remove(0);

		//truth table can contain a space to separate emitters and receivers
		//but we can just ignore the space
		String truthTableHeader = lines.remove(0).replace(" ", "");
		Map<Character, List<Boolean>> values = new HashMap<>();
		for (int i = 0; i < truthTableHeader.length(); ++i)
			values.put(truthTableHeader.charAt(i), new ArrayList<>());
		for (int w = 0; w < lines.size(); ++w) {
			String line = lines.get(w).replace(" ", "");
			for (int i = 0; i < truthTableHeader.length(); ++i)
				values.get(truthTableHeader.charAt(i)).add(line.charAt(i) == '1');
		}
		List<Terminal> terminals = new ArrayList<>(truthTableHeader.length());
		for (int i = 0; i < truthTableHeader.length(); ++i) {
			char c = truthTableHeader.charAt(i);
			terminals.add(new Terminal(emitterDir.containsKey(c), rowcols.get(c),
					emitterDir.containsKey(c) ? emitterDir.get(c) : receiverDir.get(c), values.get(c)));
		}

		return new Problem(playfield, terminals);
	}

	public static final class Terminal {
		private final boolean isEmitter;
		private final int rowcol;
		private final Direction dir;
		private final List<Boolean> values;
		private Terminal(boolean isEmitter, int rowcol, Direction dir, List<Boolean> values) {
			this.isEmitter = isEmitter;
			this.rowcol = rowcol;
			this.dir = dir;
			this.values = values;
		}
		public boolean isEmitter() {
			return isEmitter;
		}
		public boolean isReceiver() {
			return !isEmitter();
		}
		public int row() {
			return rowcol >>> 16;
		}
		public int col() {
			return rowcol & ((1 << 16) - 1);
		}
		public Direction dir() {
			return dir;
		}
		public List<Boolean> values() {
			return values;
		}
	}
}
