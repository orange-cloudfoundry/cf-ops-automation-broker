package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

public enum GitProcessorContext {
	workDir,

	commitMessage,
	/**
	 * This key represent the name of a branch (e.g. "service-instance-guid".
	 *
	 * If the branch is missing from the clone, then it will be created from the branch checked out (specified in the GitProcessor constructor)
	 *
	 * If the branch is present in the clone, it will simply be used.
	 */
	createBranchIfMissing
}
