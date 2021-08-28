/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import java.util.Collection;
import java.util.function.Consumer;

public class ToCollectionConsumer<T> implements Consumer<T> {
	private Collection<T> collection;

	@Override
	public void accept(T t) {
		collection.add(t);
	}

	public void setCollection(Collection<T> list) {
		this.collection = list;
	}
}
