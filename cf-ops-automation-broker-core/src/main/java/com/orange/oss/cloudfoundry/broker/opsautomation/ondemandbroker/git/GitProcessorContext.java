package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

/**
 * Contract supported by the SimpleGitManager.
 * This Enum is ordered the same as what the processor will handle operations
 */
public enum GitProcessorContext {

	// Precreate

	/**
	 * This key should map to an array of String specifying the submodule path specs
	 * (e.g."master-depls/bosh-expe/template/bosh-deployment" ) to fetch/update.
	 * If this key is missing or empty, no submodule is fetched by default.
	 */
	submoduleListToFetch(false, true),

	/**
	 * When this key is set a boolean true value, then all submodules are fetched without requiring
	 * to individually list them. This overrides the behavior or submoduleListToFetch key
	 */
	fetchAllSubModules(false, true),

	/**
	 * In: When this key is specified, the gitProcessor will fail if the cloned repo does contain
	 * <pre>
	 * git branch -rl service-instance-guid  # TODO: and fail if service-instance-guid displays
	 * </pre>
	 */
	failIfRemoteBranchExists(true, false),

	/**
	 * In: Name of an expected remote branch to checkout following clone. Defaults to master if missing.
	 * Fails if the remote branch is missing.
	 *
	 * Equivalent of:
	 * <pre>
	 * git checkout cassandra # fails if remote cassandra branch does not exist
	 * </pre>
	 */
	checkOutRemoteBranch(true, false),

	/**
	 * In: This key represents the name of a branch to checkout (e.g. "service-instance-guid").
	 *
	 * If the branch is missing from the clone, then it will be created from the branch checked out (specified in the checkOutRemoteBranch key)
	 *
	 * If the branch is present in the clone, it will simply be used.
	 *
	 * Equivalent of:
	 * <pre>
	 * git branch cassandra #create a local branch
	 * git config branch.cassandra.remote origin; git config branch.cassandra.merge refs/heads/cassandra; #configure branch to push to remote with same name
	 * git checkout cassandra # checkout
	 * </pre>
	 */
	createBranchIfMissing(true, false),


	/**
	 * Out: Workdir result of the preCreate activity (a java.nio.file.Path)
	 */
	workDir(false, false),

	// Postcreate

	/**
	 * In: Use this key to specify the commit message (usually 1st line \n\nadditional details)
	 */
	commitMessage(false, false),


	/**
	 * In: Use this key to not perform a clone and clone clean up in the step associated with this context. Value can contain non null java.lang.Object
	 */
	ignoreStep(false, false),

	/**
	 * In: ask to delete the specified remote branch
	 *
	 * Equivalent of:
	 * <pre>
	 *  git push :service-instance-guid # delete the branch.
	 * </pre>
	 */
	deleteRemoteBranch(false, false);

	private final boolean rejectWhenPooled;
	private final boolean poolable;

	GitProcessorContext(boolean poolable, boolean rejectWhenPooled) {
		this.poolable = poolable;
		this.rejectWhenPooled = rejectWhenPooled;
	}

	/**
	 * Indicates whether the request field is a discrimant key when pooling the request (i.e. is discrimant
	 */
	boolean isPoolable() {
		return poolable;
	}

	/**
	 * Indicates whether the request field is not supported when using pooling.
	 */
	boolean isRejectWhenPooled() {
		return rejectWhenPooled;
	}
}
