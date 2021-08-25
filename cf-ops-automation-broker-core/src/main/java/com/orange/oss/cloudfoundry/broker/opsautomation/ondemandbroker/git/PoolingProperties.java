package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.time.Duration;

public class PoolingProperties {

	private int minIdle = 1; //eager pooling (prefetching) enabled by default.

	private int secondsBetweenEvictionRuns = 1; //async eager pooling (prefetching) enabled by default. See rationale
	// for default in docs/05-eager-git-clones.md


	public int getMinIdle() {
		return minIdle;
	}

	public int getSecondsBetweenEvictionRuns() {
		return secondsBetweenEvictionRuns;
	}

	/**
	 * Configure interval at which the pool will be checked for eviction extra pooled objects or refilling missing min
	 * idle objects.
	 *
	 * See quoted {@link org.apache.commons.pool2.impl.GenericKeyedObjectPool#setTimeBetweenEvictionRunsMillis(long)}
	 * below:
	 *
	 * Sets the number of milliseconds to sleep between runs of the idle object evictor thread.
	 * <ul>
	 * <li>When positive, the idle object evictor thread starts.</li>
	 * <li>When non-positive, no idle object evictor thread runs.</li>
	 * </ul>
	 */
	public void setSecondsBetweenEvictionRuns(int secondsBetweenEvictionRuns) {
		this.secondsBetweenEvictionRuns = secondsBetweenEvictionRuns;
	}

	/**
	 * See quoted {@link org.apache.commons.pool2.impl.GenericKeyedObjectPool#getMinIdlePerKey()} below:
	 *
	 * Sets the target for the minimum number of idle objects to maintain in
	 * each of the keyed sub-pools. This setting only has an effect if it is
	 * positive and getDurationBetweenEvictionRuns() is greater than
	 * zero. If this is the case, an attempt is made to ensure that each
	 * sub-pool has the required minimum number of instances during idle object
	 * eviction runs.
	 *
	 * Set to zero to disable coab eager pooling (prefetching)
	 */
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}


}
