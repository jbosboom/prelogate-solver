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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.jeffreybosboom.prelogate.Problem.Terminal;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/28/2015
 */
public final class Search {
	private final List<ListMultimap<Integer, ImmutableList<Device>>> materializedRows = new ArrayList<>();
	private final List<int[]> partitions = new ArrayList<>();
	private final ImmutableMap<Coordinate, Terminal> emitters, receivers;
	private final int truthTableRows;
	public Search(Problem problem, int deviceCount) {
		ImmutableMap.Builder<Coordinate, Terminal> eb = ImmutableMap.builder(), rb = ImmutableMap.builder();
		problem.terminals().forEach(t -> (t.isEmitter() ? eb : rb).put(t.coord(), t));
		this.emitters = eb.build();
		this.receivers = rb.build();
		this.truthTableRows = problem.terminals().get(0).values().size();

		Map<Coordinate, Set<Device>> devices = prune(problem.devices());
		devices.forEach((k, v) -> System.out.format("%s: %s%n", k, v));

		//If we have two rows with the same sets of devices, we want to share
		//their materialized rows.
		Map<List<Set<Device>>, ListMultimap<Integer, ImmutableList<Device>>> materializationSharing = new HashMap<>();
		for (List<Set<Device>> row : devicesAsGrid(devices)) {
			ListMultimap<Integer, ImmutableList<Device>> materialization = materializationSharing.get(row);
			if (materialization == null) {
				materialization = Multimaps.newListMultimap(new DenseIntegerMap<>(deviceCount+1), ArrayList::new);
				for (List<Device> instance : Sets.cartesianProduct(row)) {
					if (pruneRow(instance)) continue;
					int devicesInInstance = 0;
					for (Device d : instance)
						if (d != BasicDevice.EMPTY && d != BasicDevice.WALL)
							++devicesInInstance;
					if (devicesInInstance > deviceCount) continue;
					materialization.put(devicesInInstance, ImmutableList.copyOf(instance));
				}
				//TODO: we used ListMultimap because we know there aren't duplicates
				//we could assert that by sorting each of materialization.values()
				//and ensuring all neighbors are distinct
				materializationSharing.put(row, materialization);
			}
			materializedRows.add(materialization);
		}
		buildPartitions(deviceCount, 0, new ArrayDeque<>(materializedRows.size()), partitions);
	}

	private Map<Coordinate, Set<Device>> prune(Map<Coordinate, Set<Device>> input) {
		Map<Coordinate, Set<Device>> devices = new TreeMap<>();
		input.forEach((k, v) -> devices.put(k, new HashSet<>(v)));
		pruneAllOutputsFaceWalls(devices);
		pruneNoInputFromEmitter(devices);
		pruneNoOutputToReceiver(devices);
		return devices;
	}

	private void pruneAllOutputsFaceWalls(Map<Coordinate, Set<Device>> device) {
		List<Device> toBeRemoved = new ArrayList<>();
		Set<BasicDevice> justWall = EnumSet.of(BasicDevice.WALL);
		device.forEach((c, s) -> {
			if (s.contains(BasicDevice.WALL)) return;
			s.forEach(d -> {
				if (d.outputs().stream().anyMatch(d.inputs()::contains)) {
					//empty, mirror, splitter, diffuser, if
					List<Direction> outputsFacingWalls = d.outputs().stream()
						.filter(o -> {
							Coordinate neighbor = c.translate(o);
							//ignore known-empty cells, as they don't affect the beam
							while (EnumSet.of(BasicDevice.EMPTY).equals(device.get(neighbor)))
								neighbor = neighbor.translate(o);
							return device.get(neighbor).equals(justWall)
								&& !receivers.containsKey(neighbor)
								&& !emitters.containsKey(neighbor);
						}).collect(Collectors.toList());
					if ((basedOn(d, BasicDevice.MIRROR) || basedOn(d, BasicDevice.IF)) && outputsFacingWalls.size() >= 1)
						toBeRemoved.add(d);
					else if ((basedOn(d, BasicDevice.SPLITTER) || basedOn(d, BasicDevice.DIFFUSER)) &&
							//three sides, or two non-opposing sides
							(outputsFacingWalls.size() >= 3 || (outputsFacingWalls.size() == 2 && !outputsFacingWalls.get(0).opposite().equals(outputsFacingWalls.get(1)))))
						toBeRemoved.add(d);
					//can't remove empty if we're using device-count-limited search
				} else {
					//and, or, xor
					int outputsFacingWalls = (int)d.outputs().stream()
						.filter(o -> {
							Coordinate neighbor = c.translate(o);
							//ignore known-empty cells, as they don't affect the beam
							while (EnumSet.of(BasicDevice.EMPTY).equals(device.get(neighbor)))
								neighbor = neighbor.translate(o);
							return device.get(neighbor).equals(justWall)
								&& !receivers.containsKey(neighbor);
								//emitters count as walls when inputs and outputs are disjoint
						}).count();
					if (outputsFacingWalls >= 1)
						toBeRemoved.add(d);
				}
			});
			if (!toBeRemoved.isEmpty()) {
				System.out.println("pruneAllOutputsFaceWalls: pruned "+toBeRemoved+" from "+c);
				s.removeAll(toBeRemoved);
				toBeRemoved.clear();
			}
		});
	}

	private void pruneNoOutputToReceiver(Map<Coordinate, Set<Device>> devices) {
		//Devices adjacent to a receiver in the receiver's direction must have
		//at least one of their outputs facing the receiver if the receiver is
		//ever true.
		receivers.forEach((r, t) -> {
			if (!t.values().contains(true)) return;
			Coordinate neighbor = r.translate(t.dir());
			Set<Device> p = devices.get(neighbor);
			if (p == null) return;
			List<Device> toBeRemoved = p.stream().filter(d -> !d.outputs().contains(t.dir().opposite())).collect(Collectors.toList());
			if (!toBeRemoved.isEmpty()) {
				System.out.println("pruneNoOutputToReceiver: pruned "+toBeRemoved+" from "+neighbor);
				p.removeAll(toBeRemoved);
			}
		});
	}

	private void pruneNoInputFromEmitter(Map<Coordinate, Set<Device>> devices) {
		//Devices adjacent to an emitter in the emitter's direction must have
		//at least one of their inputs facing the emitter if there is a truth
		//table row where only that emitter is true and any receiver is true.
		emitters.forEach((r, t) -> {
			if (!emitterMustFlow(t)) return;
			Coordinate neighbor = r.translate(t.dir());
			//ignore known-empty cells, as they don't affect the beam
			while (EnumSet.of(BasicDevice.EMPTY).equals(devices.get(neighbor)))
				neighbor = neighbor.translate(t.dir());
			Set<Device> p = devices.get(neighbor);
			if (p == null) return;
			List<Device> toBeRemoved = p.stream().filter(d -> !d.inputs().contains(t.dir().opposite())).collect(Collectors.toList());
			if (!toBeRemoved.isEmpty()) {
				System.out.println("pruneNoInputFromEmitter: pruned "+toBeRemoved+" from "+neighbor);
				p.removeAll(toBeRemoved);
			}
		});
	}

	private boolean pruneRow(List<Device> row) {
		return pruneRowGatesFacingOutputs(row) ||
				pruneUselessSplitterDiffuser(row);
	}

	private boolean pruneRowGatesFacingOutputs(List<Device> row) {
		for (int i = 0; i < row.size(); ++i) {
			Device first = row.get(i);
			if (!basedOn(first, BasicDevice.AND, BasicDevice.OR, BasicDevice.XOR)) continue;
			for (int j = i+1; j < row.size(); ++j) {
				Device second = row.get(j);
				if (second.equals(BasicDevice.EMPTY)) continue;
				if (basedOn(second, BasicDevice.IF) && second.outputs().contains(Direction.LEFT)) continue; //horizontal if is okay
				if (!basedOn(second, BasicDevice.AND, BasicDevice.OR, BasicDevice.XOR)) break;
				if (first.outputs().equals(second.outputs().stream().map(Direction::opposite).collect(Collectors.toSet())))
					return true;
				break;
			}
		}
		return false;
	}

	private boolean pruneUselessSplitterDiffuser(List<Device> row) {
		//A splitter or diffuser between two opposing devices with no inputs or
		//outputs facing the splitter/diffuser is useless.
		for (int i = 0; i < row.size(); ++i) {
			if (!basedOn(row.get(i), BasicDevice.SPLITTER, BasicDevice.DIFFUSER)) continue;
			int l, r;
			for (l = i-1; l >= 0; --l)
				if (!row.get(l).equals(BasicDevice.EMPTY)) break;
			for (r = i+1; r < row.size(); ++r)
				if (!row.get(r).equals(BasicDevice.EMPTY)) break;
			if (l < 0 || r >= row.size()) continue;
			Device left = row.get(l), right = row.get(r);
			if (!left.inputs().contains(Direction.RIGHT) && !left.outputs().contains(Direction.RIGHT) &&
					!right.inputs().contains(Direction.LEFT) && !right.outputs().contains(Direction.LEFT))
				return true;
		}
		return false;
	}

	private boolean emitterMustFlow(Terminal emitter) {
		for (int ttr = 0; ttr < truthTableRows; ++ttr) {
			int finalttr = ttr;
			if (!emitter.values().get(ttr)) continue;
			if (emitters.values().stream().filter(e -> e != emitter).anyMatch(e -> e.values().get(finalttr))) continue;
			if (receivers.values().stream().noneMatch(r -> r.values().get(finalttr))) continue;
			return true;
		}
		return false;
	}

	private static boolean basedOn(Device d, BasicDevice first) {
		BasicDevice b = d instanceof RotatedDevice ? ((RotatedDevice)d).base() : (BasicDevice)d;
		return b == first;
	}
	private static boolean basedOn(Device d, BasicDevice first, BasicDevice second) {
		BasicDevice b = d instanceof RotatedDevice ? ((RotatedDevice)d).base() : (BasicDevice)d;
		return b == first || b == second;
	}
	private static boolean basedOn(Device d, BasicDevice first, BasicDevice second, BasicDevice third) {
		BasicDevice b = d instanceof RotatedDevice ? ((RotatedDevice)d).base() : (BasicDevice)d;
		return b == first || b == second || b == third;
	}

	private static boolean basedOn(Device d, BasicDevice first, BasicDevice... more) {
		BasicDevice b = d instanceof RotatedDevice ? ((RotatedDevice)d).base() : (BasicDevice)d;
		if (b == first) return true;
		for (BasicDevice i : more)
			if (b == i) return true;
		return false;
	}

	private static List<List<Set<Device>>> devicesAsGrid(Map<Coordinate, Set<Device>> devices) {
		int rows = devices.keySet().stream().mapToInt(Coordinate::row).max().getAsInt() + 1;
		List<List<Set<Device>>> playfield = new ArrayList<>(rows);
		for (int r = 0; r < rows; ++r) {
			int finalr = r;
			int cols = devices.keySet().stream().filter(c -> c.row() == finalr).mapToInt(Coordinate::col).max().getAsInt()+1;
			List<Set<Device>> thisRow = new ArrayList<>(cols);
			for (int c = 0; c < cols; ++c)
				thisRow.add(devices.get(Coordinate.at(r, c)));
			playfield.add(thisRow);
		}
		return playfield;
	}

	private void buildPartitions(int target, int index, ArrayDeque<Integer> current, List<int[]> partitions) {
		if (index == materializedRows.size()) {
			if (target == 0)
				partitions.add(current.stream().mapToInt(Integer::intValue).toArray());
			return;
		}
		for (int k : materializedRows.get(index).keySet())
			if (k <= target) {
				current.addLast(k);
				buildPartitions(target-k, index+1, current, partitions);
				current.removeLast();
			}
	}

	public void search() {
		List<List<ImmutableList<Device>>> solutions = partitions.parallelStream()
				.flatMap(p -> {
					List<List<ImmutableList<Device>>> rowChoices = new ArrayList<>();
					for (int i = 0; i < materializedRows.size(); ++i)
						rowChoices.add(materializedRows.get(i).get(p[i]));
					return Lists.cartesianProduct(rowChoices).stream();
				}).filter(this::evaluate)
				.peek(System.out::println)
				.collect(Collectors.toList());
		System.out.println(solutions.size());
	}

	public long countTrials() {
		long count = partitions.parallelStream()
			.mapToLong(p -> {
				List<List<ImmutableList<Device>>> rowChoices = new ArrayList<>();
				for (int i = 0; i < materializedRows.size(); ++i)
					rowChoices.add(materializedRows.get(i).get(p[i]));
				return Lists.cartesianProduct(rowChoices).size();
			}).sum();
		return count;
	}

	private static final int QUIESCENCE_TICKS = 100;
	private boolean evaluate(List<ImmutableList<Device>> devices) {
		LaserDirection[][] prev = new LaserDirection[devices.size()][], next = new LaserDirection[devices.size()][];
		for (int i = 0; i < prev.length; ++i) {
			prev[i] = new LaserDirection[devices.get(i).size()];
			next[i] = prev[i].clone();
		}

		for (int ttr = 0; ttr < truthTableRows; ++ttr) {
			for (LaserDirection[] n : next)
				Arrays.fill(n, LaserDirection.make(false, false, false, false));
			enforceEmitters(next, ttr);

			int ticks = 0;
			do {
				LaserDirection[][] swaptemp = prev;
				prev = next;
				next = swaptemp;

				for (int r = 0; r < next.length; ++r)
					for (int c = 0; c < next[r].length; ++c) {
						//TODO: we can get rid of the boundary of walls if we're
						//willing to check emitters in the loop.
						LaserDirection input = LaserDirection.make(getInput(prev, r, c, Direction.UP),
								getInput(prev, r, c, Direction.RIGHT),
								getInput(prev, r, c, Direction.DOWN),
								getInput(prev, r, c, Direction.LEFT));
						next[r][c] = devices.get(r).get(c).operate(input);
					}

				enforceEmitters(next, ttr);
			} while (!Arrays.deepEquals(prev, next) && ++ticks < QUIESCENCE_TICKS);
			if (ticks >= QUIESCENCE_TICKS)
				return false; //did not quiesce
			if (!checkReceivers(next, ttr))
				return false;
		}

		return true;
	}

	private static boolean getInput(LaserDirection[][] state, int r, int c, Direction d) {
		switch (d) {
			case UP:
				return (r - 1) < 0 ? false : state[r-1][c].down();
			case RIGHT:
				return (c + 1) >= state[r].length ? false : state[r][c+1].left();
			case DOWN:
				return (r + 1) >= state.length ? false : state[r+1][c].up();
			case LEFT:
				return (c - 1) < 0 ? false : state[r][c-1].right();
		}
		throw new AssertionError("unreachable");
	}

	private void enforceEmitters(LaserDirection[][] state, int ttr) {
		for (Terminal t : emitters.values())
			state[t.row()][t.col()] = state[t.row()][t.col()].set(t.dir(), t.values().get(ttr));
	}

	private boolean checkReceivers(LaserDirection[][] state, int ttr) {
		for (Terminal t : receivers.values())
			if (getInput(state, t.row(), t.col(), t.dir()) != t.values().get(ttr))
				return false;
		return true;
	}

	public static void main(String[] args) throws IOException {
		Problem problem = Problem.fromFile(Paths.get(args[0]));
		Search search = new Search(problem, Integer.valueOf(args[1]));
		System.out.println(search.countTrials()+" states to check");
		search.search();
	}
}
