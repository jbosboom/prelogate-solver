package com.jeffreybosboom.prelogate;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.jeffreybosboom.prelogate.Problem.Terminal;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 6/28/2015
 */
public final class Search {
	private final Problem problem;
	private final List<SetMultimap<Integer, List<Device>>> materializedRows = new ArrayList<>();
	private final List<int[]> partitions = new ArrayList<>();
	public Search(Problem problem, int deviceCount) {
		this.problem = problem;
		Map<List<Set<Device>>, SetMultimap<Integer, List<Device>>> materializationSharing = new HashMap<>();
		for (List<Set<Device>> row : problem.devices()) {
			SetMultimap<Integer, List<Device>> materialization = materializationSharing.get(row);
			if (materialization == null) {
				materialization = ImmutableSetMultimap.copyOf(Multimaps.index(Sets.cartesianProduct(row),
					r -> (int)r.stream().filter(x -> x != BasicDevice.EMPTY && x != BasicDevice.WALL).count()));
				materializationSharing.put(row, materialization);
			}
			materializedRows.add(materialization);
		}
		buildPartitions(deviceCount, 0, new ArrayDeque<>(materializedRows.size()), partitions);
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
		partitions.parallelStream().flatMap(p -> {
			List<Set<List<Device>>> rowChoices = new ArrayList<>();
			for (int i = 0; i < materializedRows.size(); ++i)
				rowChoices.add(materializedRows.get(i).get(p[i]));
			return Sets.cartesianProduct(rowChoices).stream();
		}).filter(this::evaluate).forEach(System.out::println);
	}

	private static final int QUIESCENCE_TICKS = 100;
	private boolean evaluate(List<List<Device>> devices) {
		ArrayList<ArrayList<LaserDirection>> prev = new ArrayList<>(), next = new ArrayList<>();
		for (List<Device> d : devices) {
			prev.add(new ArrayList<>(Collections.nCopies(d.size(), LaserDirection.make(false, false, false, false))));
			next.add(new ArrayList<>(Collections.nCopies(d.size(), LaserDirection.make(false, false, false, false))));
		}

		for (int ttr = 0; ttr < problem.terminals().get(0).values().size(); ++ttr) {
			for (ArrayList<LaserDirection> n : next)
				Collections.copy(n, Collections.nCopies(n.size(), LaserDirection.make(false, false, false, false)));
			enforceEmitters(next, ttr);

			int ticks = 0;
			do {
				ArrayList<ArrayList<LaserDirection>> swaptemp = prev;
				prev = next;
				next = swaptemp;

				for (int r = 0; r < next.size(); ++r)
					for (int c = 0; c < next.get(r).size(); ++c) {
						//TODO: we can get rid of the boundary of walls if we're
						//willing to check emitters in the loop.
						LaserDirection input = LaserDirection.make(getInput(prev, r, c, Direction.UP),
								getInput(prev, r, c, Direction.RIGHT),
								getInput(prev, r, c, Direction.DOWN),
								getInput(prev, r, c, Direction.LEFT));
						next.get(r).set(c, devices.get(r).get(c).operate(input));
					}

				enforceEmitters(next, ttr);
			} while (!prev.equals(next) && ++ticks < QUIESCENCE_TICKS);
			if (ticks >= QUIESCENCE_TICKS)
				return false; //did not quiesce
			if (!checkReceivers(next, ttr))
				return false;
		}

		return true;
	}

	private static boolean getInput(ArrayList<ArrayList<LaserDirection>> state, int r, int c, Direction d) {
		switch (d) {
			case UP:
				return (r - 1) < 0 ? false : state.get(r-1).get(c).down();
			case RIGHT:
				return (c + 1) >= state.get(r).size() ? false : state.get(r).get(c+1).left();
			case DOWN:
				return (r + 1) >= state.size() ? false : state.get(r+1).get(c).up();
			case LEFT:
				return (c - 1) < 0 ? false : state.get(r).get(c-1).right();
		}
		throw new AssertionError("unreachable");
	}

	private void enforceEmitters(ArrayList<ArrayList<LaserDirection>> state, int ttr) {
		for (Terminal t : problem.terminals())
			if (t.isEmitter())
				state.get(t.row()).set(t.col(), state.get(t.row()).get(t.col()).set(t.dir(), t.values().get(ttr)));
	}

	private boolean checkReceivers(ArrayList<ArrayList<LaserDirection>> state, int ttr) {
		for (Terminal t : problem.terminals())
			if (t.isReceiver())
				if (getInput(state, t.row(), t.col(), t.dir()) != t.values().get(ttr))
					return false;
		return true;
	}

	public static void main(String[] args) throws IOException {
		Problem problem = Problem.fromFile(Paths.get("Conclusion - Turn.txt"));
		Search search = new Search(problem, 7);
		search.search();
	}
}
