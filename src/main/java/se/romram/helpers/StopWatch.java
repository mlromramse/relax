package se.romram.helpers;

/**
 * Created by micke on 2015-02-10.
 */
public class StopWatch {
	long start = 0;
	long lastLap = 0;
	long lap = 0;
	long end = 0;

	public StopWatch start() {
		end = lastLap = lap = start = System.nanoTime();
		return this;
	}

	public StopWatch stop() {
		end = System.nanoTime();
		return this;
	}

	public StopWatch lap() {
		lastLap = lap;
		lap = System.nanoTime();
		return this;
	}

	public StopWatch reset() {
		return start();
	}

	public long getLapTime() {
		return getLapNanoTime() / 1000000;
	}

	public long getLapNanoTime() {
		return lap - lastLap;
	}

	public long getTotalTime() {
		return getTotalNanoTime() / 1000000;
	}

	public long getTotalNanoTime() {
		return end - start;
	}
}
