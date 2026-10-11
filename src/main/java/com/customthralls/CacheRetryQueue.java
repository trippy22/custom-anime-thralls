package com.customthralls;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Client-thread-only queue. Only cache readiness is retried; each ready request runs once. */
final class CacheRetryQueue<T>
{
	private static final int MAX_DELAY_TICKS = 8;
	private final BooleanSupplier cacheReady;
	private final Consumer<T> onReady;
	private final Set<T> pending = new LinkedHashSet<>();
	private int delay = 1;
	private int ticksRemaining = 1;

	CacheRetryQueue(BooleanSupplier cacheReady, Consumer<T> onReady)
	{
		this.cacheReady = cacheReady;
		this.onReady = onReady;
	}

	void submit(T request)
	{
		if (pending.isEmpty() && cacheReady.getAsBoolean())
		{
			onReady.accept(request);
		}
		else
		{
			// New arrivals neither probe the cache nor reset an existing backoff.
			pending.add(request);
		}
	}

	void tick()
	{
		if (pending.isEmpty() || --ticksRemaining > 0)
		{
			return;
		}
		if (!cacheReady.getAsBoolean())
		{
			delay = Math.min(delay * 2, MAX_DELAY_TICKS);
			ticksRemaining = delay;
			return;
		}
		ArrayList<T> ready = new ArrayList<>(pending);
		clear();
		ready.forEach(onReady);
	}

	void remove(T request)
	{
		pending.remove(request);
		if (pending.isEmpty())
		{
			clear();
		}
	}

	void clear()
	{
		pending.clear();
		delay = 1;
		ticksRemaining = 1;
	}
}
