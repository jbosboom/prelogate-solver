package com.jeffreybosboom.prelogate;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/30/2016
 */
public final class DenseIntegerMap<V> extends AbstractMap<Integer, V> {
	private final V[] values;
	@SuppressWarnings("unchecked")
	public DenseIntegerMap(int maxExclusive) {
		this.values = (V[])new Object[maxExclusive];
	}

	@Override
	public V get(Object key) {
		return get(objectToInt(key));
	}

	public V get(int key) {
		return values[key];
	}

	@Override
	public V put(Integer key, V value) {
		return put(objectToInt(key), value);
	}

	public V put(int key, V value) {
		Objects.requireNonNull(value);
		V old = values[key];
		values[key] = value;
		return old;
	}

	@Override
	public V remove(Object key) {
		return remove(objectToInt(key));
	}

	public V remove(int key) {
		V old = values[key];
		values[key] = null;
		return old;
	}

	@Override
	public Set<Entry<Integer, V>> entrySet() {
		return new AbstractSet() {
			@Override
			public int size() {
				//TODO: maybe we need to track size
				int size = 0;
				for (int i = 0; i < values.length; ++i)
					if (values[i] != null)
						++size;
				return size;
			}

			@Override
			public Iterator iterator() {
				int firstNonnullIndex = 0;
				while (firstNonnullIndex < values.length && get(firstNonnullIndex) == null)
					++firstNonnullIndex;
				final int finalFirstNonnullIndex = firstNonnullIndex;
				return new Iterator<V>() {
					private int nextNonnullIndex = finalFirstNonnullIndex;
					private int removableIndex = -1;
					@Override
					public boolean hasNext() {
						return nextNonnullIndex < values.length;
					}

					@Override
					public V next() {
						V ret = get(nextNonnullIndex);
						if (ret == null) throw new ConcurrentModificationException("mapping removed during iteration");
						removableIndex = nextNonnullIndex;
						++nextNonnullIndex;
						while (nextNonnullIndex < values.length && get(nextNonnullIndex) == null)
							++nextNonnullIndex;
						return ret;
					}

					@Override
					public void remove() {
						if (removableIndex < 0) {
							if (removableIndex == -1) throw new IllegalStateException("next() never called");
							if (removableIndex == -2) throw new IllegalStateException("remove() called twice");
						}
						if (get(removableIndex) == null) throw new ConcurrentModificationException("mapping already removed");
						DenseIntegerMap.this.remove(removableIndex);
						removableIndex = -2;
					}
				};
			}
		};
	}

	private static int objectToInt(Object key) {
		//TODO: should we expect Integer?
		return ((Number)key).intValue();
	}
}
