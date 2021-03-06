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
	private final Map<Coordinate, Set<Device>> devices;
	private final List<Terminal> terminals;

	public Problem(Map<Coordinate, Set<Device>> devices, List<Terminal> terminals) {
		this.devices = devices;
		this.terminals = terminals;
	}

	public Map<Coordinate, Set<Device>> devices() {
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

		Map<Coordinate, Set<Device>> playfield = new HashMap<>();
		Map<Character, Integer> rowcols = new HashMap<>();
		int row = 0;
		for (String line = lines.get(0); !line.isEmpty(); lines.remove(0), line = lines.get(0), ++row) {
			for (int col = 0; col < line.length(); ++col) {
				char c = line.charAt(col);
				playfield.put(Coordinate.at(row, col), deviceMap.get(c));
				if (emitterDir.containsKey(c) || receiverDir.containsKey(c))
					rowcols.put(c, (row << 16) | col);
			}
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
		public Coordinate coord() {
			return Coordinate.at(row(), col());
		}
		public Direction dir() {
			return dir;
		}
		public List<Boolean> values() {
			return values;
		}
	}
}
