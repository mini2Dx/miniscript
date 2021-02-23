/**
 * Copyright 2020 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import java.util.ArrayDeque;

public class ReadWriteBlockingQueue<E> extends AbstractConcurrentBlockingQueue<E> {

	public ReadWriteBlockingQueue(int maxCapacity) {
		super(maxCapacity, new ArrayDeque<>());
	}
}
