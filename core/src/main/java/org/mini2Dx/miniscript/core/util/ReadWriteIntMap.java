/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import org.mini2Dx.lockprovider.ReadWriteLock;
import org.mini2Dx.miniscript.core.GameScriptingEngine;

public class ReadWriteIntMap<T> extends IntMap<T> {
	protected ReadWriteLock lock = GameScriptingEngine.LOCK_PROVIDER.newReadWriteLock();

	private final ThreadLocal<Keys> keys = new ThreadLocal<Keys>() {
		@Override
		protected Keys initialValue() {
			return new Keys(ReadWriteIntMap.this);
		}
	};
	private final ThreadLocal<Values> values = new ThreadLocal<Values>() {
		@Override
		protected Values initialValue() {
			return new Values(ReadWriteIntMap.this);
		}
	};
	private final ThreadLocal<Entries> entries = new ThreadLocal<Entries>() {
		@Override
		protected Entries initialValue() {
			return new Entries(ReadWriteIntMap.this);
		}
	};

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public ReadWriteIntMap() {
		super();
	}

	/**
	 * Creates a new map with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ReadWriteIntMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor
	 */
	public ReadWriteIntMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * NOTE: read access to the other map is not thread-safe
	 *
	 * @param map
	 */
	public ReadWriteIntMap(IntMap<? extends T> map) {
		super(map);
	}

	/**
	 * Returns the size in a thread-safe manner
	 * @return 0 if empty
	 */
	public int size() {
		lock.lockRead();
		final int result = super.size;
		lock.unlockRead();
		return result;
	}

	@Override
	public T put(int key, T value) {
		lock.lockWrite();
		T t = super.put(key, value);
		lock.unlockWrite();
		return t;
	}

	/**
	 * Puts a key/value if the key is not already present
	 * @param key The key to put if absent
	 * @param value The value to put if absent
	 * @return True if the value was put
	 */
	public boolean putIfAbsent(int key, T value) {
		boolean result = false;
		lock.lockWrite();
		if(!super.containsKey(key)) {
			super.put(key, value);
			result = true;
		}
		lock.unlockWrite();
		return result;
	}

	/**
	 * Puts a key/value if the key is already present
	 * @param key The key to put if present
	 * @param value The value to put if present
	 * @return True if the value was put
	 */
	public boolean putIfPresent(int key, T value) {
		boolean result = false;
		lock.lockWrite();
		if(super.containsKey(key)) {
			super.put(key, value);
			result = true;
		}
		lock.unlockWrite();
		return result;
	}

	@Override
	public void putAll(IntMap<? extends  T> map) {
		boolean isOtherConcurrent = map instanceof ReadWriteIntMap;
		if (isOtherConcurrent){
			((ReadWriteIntMap) map).getLock().lockRead();
		}
		lock.lockWrite();
		super.putAll(map);
		lock.unlockWrite();
		if (isOtherConcurrent){
			((ReadWriteIntMap) map).getLock().unlockRead();
		}
	}

	/**
	 * @param key
	 * @param defaultValue Returned if the key was not associated with a value.
	 */
	@Override
	public T get(int key, T defaultValue) {
		lock.lockRead();
		T t = super.get(key, defaultValue);
		lock.unlockRead();
		return t;
	}

	@Override
	public T get(int key) {
		lock.lockRead();
		T t = super.get(key);
		lock.unlockRead();
		return t;
	}

	@Override
	public T remove(int key) {
		lock.lockWrite();
		T t = super.remove(key);
		lock.unlockWrite();
		return t;
	}

	/**
	 * Returns true if the map has one or more items.
	 */
	@Override
	public boolean notEmpty() {
		lock.lockRead();
		boolean b = super.notEmpty();
		lock.unlockRead();
		return b;
	}

	/**
	 * Returns true if the map is empty.
	 */
	@Override
	public boolean isEmpty() {
		lock.lockRead();
		boolean b = super.isEmpty();
		lock.unlockRead();
		return b;
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
	 * done. If the map contains more items than the specified capacity, the next highest power of two capacity is used instead.
	 *
	 * @param maximumCapacity
	 */
	@Override
	public void shrink(int maximumCapacity) {
		lock.lockWrite();
		super.shrink(maximumCapacity);
		lock.unlockWrite();
	}

	/**
	 * Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger.
	 *
	 * @param maximumCapacity
	 */
	@Override
	public void clear(int maximumCapacity) {
		lock.lockWrite();
		super.clear(maximumCapacity);
		lock.unlockWrite();
	}

	@Override
	public void clear() {
		lock.lockWrite();
		super.clear();
		lock.unlockWrite();
	}

	@Override
	public boolean containsKey(int key) {
		lock.lockRead();
		boolean b = super.containsKey(key);
		lock.unlockRead();
		return b;
	}

	/**
	 * Returns the key for the specified value, or <tt>notFound</tt> if it is not in the map. Note this traverses the entire map
	 * and compares every value, which may be an expensive operation.
	 *
	 * @param value
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 * @param notFound
	 */
	@Override
	public int findKey(Object value, boolean identity, int notFound) {
		lock.lockRead();
		int i = super.findKey(value, identity, notFound);
		lock.unlockRead();
		return i;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity
	 */
	@Override
	public void ensureCapacity(int additionalCapacity) {
		lock.lockWrite();
		super.ensureCapacity(additionalCapacity);
		lock.unlockWrite();
	}

	@Override
	public int hashCode() {
		lock.lockRead();
		int i = super.hashCode();
		lock.unlockRead();
		return i;
	}

	@Override
	public boolean equals(Object obj) {
		boolean isOtherConcurrent = obj instanceof ReadWriteIntMap;
		if (isOtherConcurrent){
			((ReadWriteIntMap) obj).getLock().lockRead();
		}
		lock.lockRead();
		boolean b = super.equals(obj);
		lock.unlockRead();
		if (isOtherConcurrent){
			((ReadWriteIntMap) obj).getLock().unlockRead();
		}
		return b;
	}

	@Override
	public String toString() {
		lock.lockRead();
		String s = super.toString();
		lock.unlockRead();
		return s;
	}

	@Override
	public Entries<T> entries() {
		final Entries<T> entries = this.entries.get();
		lock.lockRead();
		entries.reset();
		lock.unlockRead();
		return entries;
	}

	@Override
	public Values<T> values() {
		final Values<T> values = this.values.get();
		lock.lockRead();
		values.reset();
		lock.unlockRead();
		return values;
	}

	@Override
	public Keys keys() {
		final Keys keys = this.keys.get();
		lock.lockRead();
		keys.reset();
		lock.unlockRead();
		return keys;
	}

	public ReadWriteLock getLock() {
		return lock;
	}
}