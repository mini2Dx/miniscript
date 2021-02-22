/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ReadWriteBlockingQueue<E> extends AbstractConcurrentQueue<E> implements BlockingQueue<E> {
	private final int maxCapacity;
	private final Object monitor = new Object();

	public ReadWriteBlockingQueue(int maxCapacity) {
		super(new ArrayDeque<>());
		this.maxCapacity = maxCapacity;
	}

	@Override
	public E poll() {
		final E result = super.poll();
		synchronized(monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	@Override
	public boolean remove(Object o) {
		final boolean result = super.remove(o);
		synchronized(monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		final boolean result = super.removeAll(c);
		synchronized(monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	@Override
	public boolean offer(E e) {
		final boolean result = super.offer(e);
		synchronized(monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	@Override
	public boolean add(E e) {
		final boolean result = super.add(e);
		synchronized(monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		final boolean result = super.addAll(c);
		synchronized(monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	@Override
	public void put(E e) throws InterruptedException {
		lock.lockWrite();
		while(size() >= maxCapacity) {
			lock.unlockWrite();
			synchronized(monitor) {
				monitor.wait();
			}
			lock.lockWrite();
		}
		super.offer(e);
		lock.unlockWrite();

		synchronized(monitor) {
			monitor.notifyAll();
		}
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		lock.lockWrite();
		if(size() >= maxCapacity) {
			lock.unlockWrite();
			synchronized(monitor) {
				monitor.wait(0L, (int) unit.toNanos(timeout));
			}
			lock.lockWrite();
		}
		if(size() >= maxCapacity) {
			lock.unlockWrite();
			return false;
		}
		offer(e);
		lock.unlockWrite();
		return true;
	}

	@Override
	public E take() throws InterruptedException {
		lock.lockWrite();
		while (isEmpty()) {
			lock.unlockWrite();
			synchronized(monitor) {
				monitor.wait();
			}
			lock.lockWrite();
		}
		final E result = super.remove();
		lock.unlockWrite();

		synchronized(monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		lock.lockWrite();
		if (isEmpty()) {
			lock.unlockWrite();
			synchronized(monitor) {
				monitor.wait(0L, (int) unit.toNanos(timeout));
			}
			lock.lockWrite();
		}
		final E result = poll();
		lock.unlockWrite();
		return result;
	}

	@Override
	public int remainingCapacity() {
		return maxCapacity - size();
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		throw new UnsupportedOperationException();
	}
}
