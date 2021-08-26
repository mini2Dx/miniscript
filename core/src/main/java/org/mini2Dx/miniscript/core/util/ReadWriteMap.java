/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import org.mini2Dx.lockprovider.ReadWriteLock;
import org.mini2Dx.miniscript.core.GameScriptingEngine;

import java.util.*;

public class ReadWriteMap<K, V> implements Map<K, V> {
	private final ReadWriteLock lock = GameScriptingEngine.LOCK_PROVIDER.newReadWriteLock();
	private final Map<K, V> internalMap = new HashMap<>();

	private final ThreadLocal<Set<K>> keySet = new ThreadLocal<Set<K>>() {
		@Override
		protected Set<K> initialValue() {
			return new HashSet<>();
		}
	};
	private final ThreadLocal<Collection<V>> values = new ThreadLocal<Collection<V>>() {
		@Override
		protected Collection<V> initialValue() {
			return new ArrayList<>();
		}
	};
	private final ThreadLocal<Set<Entry<K, V>>> entrySet = new ThreadLocal<Set<Entry<K, V>>>() {
		@Override
		protected Set<Entry<K, V>> initialValue() {
			return new HashSet<>();
		}
	};

	@Override
	public int size() {
		lock.lockRead();
		final int result = internalMap.size();
		lock.unlockRead();
		return result;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		lock.lockRead();
		final boolean result = internalMap.containsKey(key);
		lock.unlockRead();
		return result;
	}

	@Override
	public boolean containsValue(Object value) {
		lock.lockRead();
		final boolean result = internalMap.containsValue(value);
		lock.unlockRead();
		return result;
	}

	@Override
	public V get(Object key) {
		lock.lockRead();
		final V result = internalMap.get(key);
		lock.unlockRead();
		return result;
	}

	@Override
	public V put(K key, V value) {
		lock.lockWrite();
		final V result = internalMap.put(key, value);
		lock.unlockWrite();
		return result;
	}

	@Override
	public V remove(Object key) {
		lock.lockWrite();
		final V result = internalMap.remove(key);
		lock.unlockWrite();
		return result;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		lock.lockWrite();
		internalMap.putAll(m);
		lock.unlockWrite();
	}

	@Override
	public void clear() {
		lock.lockWrite();
		internalMap.clear();
		lock.unlockWrite();
	}

	@Override
	public Set<K> keySet() {
		lock.lockRead();
		final Set<K> result = keySet.get();
		try {
			result.clear();
			//addAll allocates arrays unnecessarily
			internalMap.keySet().forEach((key) -> {
				result.add(key);
			});
		} finally {
			lock.unlockRead();
		}
		return result;
	}

	@Override
	public Collection<V> values() {
		lock.lockRead();
		final Collection<V> result = values.get();
		try {
			result.clear();
			//addAll allocates arrays unnecessarily
			internalMap.values().forEach((val) -> {
				result.add(val);
			});
		} finally {
			lock.unlockRead();
		}
		return result;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		lock.lockRead();
		final Set<Entry<K, V>> result = entrySet.get();
		try {
			result.clear();
			//addAll allocates arrays unnecessarily
			internalMap.entrySet().forEach((entry) -> {
				result.add(entry);
			});
		} finally {
			lock.unlockRead();
		}
		return result;
	}
}
