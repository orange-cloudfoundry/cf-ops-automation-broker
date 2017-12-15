package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

/**
 * Contract supported by the GitProcessor.
 * This Enum is ordered the same as what the processor will handle operations
 */
public enum GitProcessorContext {

	// Precreate

	/**
	 * In: When this key is specified, the gitProcessor will fail of the cloned repo does not contain
	 * <pre>
	 * git branch -rl service-instance-guid  # TODO: and fail if service-instance-guid displays
	 * </pre>
	 */
	failIfRemoteBranchExists,

	/**
	 * In: Name of an expected remote branch to checkout following clone. Defaults to master if missing.
	 * Fails if the remote branch is missing.
	 *
	 * Equivalent of
	 * <pre>
	 * git checkout cassandra # fails if remote cassandra branch does not exist
	 * </pre>
	 */
	checkOutRemoteBranch,

	/**
	 * In: This key represent the name of a branch (e.g. "service-instance-guid".
	 *
	 * If the branch is missing from the clone, then it will be created from the branch checked out (specified in the GitProcessor constructor)
	 *
	 * If the branch is present in the clone, it will simply be used.
	 * equivalent of
	 * <pre>
	 * git branch cassandra #create a local branch
	 * git config branch.cassandra.remote origin; git config branch.cassandra.merge refs/heads/cassandra; #configure branch to push to remote with same name
	 * git checkout cassandra # checkout
	 * </pre>
	 */
	createBranchIfMissing,


	/**
	 * Out: Workdir result of the preCreate activity (a java.nio.file.Path)
	 */
	workDir,

	// Postcreate

	/**
	 * In: Use this key to specify the commit message (usually 1st line \n\nadditional details)
	 */
	commitMessage,


	/**
	 * In: ask to delete the specified remote branch
	 * equivalent of:
	 * <pre>
	 *  git push :service-instance-guid # delete the branch.
	 * </pre>
	 */
	deleteRemoteBranch

}
