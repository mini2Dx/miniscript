/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Thomas Cashman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.mini2Dx.miniscript.core.util;

import org.mini2Dx.lockprovider.ReadWriteLock;
import org.mini2Dx.miniscript.core.GameScriptingEngine;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public abstract class AbstractConcurrentQueue<E> implements Queue<E> {
	protected final ReadWriteLock lock = GameScriptingEngine.LOCK_PROVIDER.newReadWriteLock();

	private final Queue<E> internalQueue;

	public AbstractConcurrentQueue(Queue<E> internalQueue) {
		this.internalQueue = internalQueue;
	}

	@Override
	public int size() {
		lock.lockRead();
		final int result = internalQueue.size();
		lock.unlockRead();
		return result;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		lock.lockRead();
		final boolean result = internalQueue.contains(o);
		lock.unlockRead();
		return result;
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		lock.lockRead();
		try {
			return internalQueue.toArray();
		} finally {
			lock.unlockRead();
		}
	}

	@Override
	public <T> T[] toArray(T[] a) {
		lock.lockRead();
		try {
			return internalQueue.toArray(a);
		} finally {
			lock.unlockRead();
		}
	}

	@Override
	public boolean add(E e) {
		lock.lockWrite();
		final boolean result = internalQueue.add(e);
		lock.unlockWrite();
		return result;
	}

	@Override
	public boolean remove(Object o) {
		lock.lockWrite();
		final boolean result = internalQueue.remove(o);
		lock.unlockWrite();
		return result;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		lock.lockRead();
		final boolean result = internalQueue.containsAll(c);
		lock.unlockRead();
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		lock.lockWrite();
		final boolean result = internalQueue.addAll(c);
		lock.unlockWrite();
		return result;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		lock.lockWrite();
		final boolean result = internalQueue.removeAll(c);
		lock.unlockWrite();
		return result;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		lock.lockWrite();
		final boolean result = internalQueue.retainAll(c);
		lock.unlockWrite();
		return result;
	}

	@Override
	public void clear() {
		lock.lockWrite();
		internalQueue.clear();
		lock.unlockWrite();
	}

	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public E remove() {
		lock.lockWrite();
		try {
			return internalQueue.remove();
		} finally {
			lock.unlockWrite();
		}
	}

	@Override
	public E poll() {
		lock.lockWrite();
		try {
			return internalQueue.poll();
		} finally {
			lock.unlockWrite();
		}
	}

	@Override
	public E element() {
		lock.lockRead();
		try {
			return internalQueue.element();
		} finally {
			lock.unlockRead();
		}
	}

	@Override
	public E peek() {
		lock.lockRead();
		try {
			return internalQueue.peek();
		} finally {
			lock.unlockRead();
		}
	}
}
