
## Current design

Context:

(Processor) Context <---> GitPoolKey <---> (SimpleGitManager) Context

Responsibities:
- PooledGitManager: interacts with pool, and maps (Processor) Context in/out of GitPoolKey, and pooled Context
- PooledGitFactory: maintains pooled Context, including reset


gitFetchAndReset()      
        - git fetch +refs/heads/develop:refs/remotes/origin/develop
        - git reset 'origin/master' --hard 

Q: how do we deal with multiple gitprocessor instances sharing the same context with different aliases ?
A: each PooledGitManager uses its own repo alias to filter request and provide responses out.


Q: how tag/prefix keys to ease their filtering ?
A: a pooleable flag on the enum. 


Q: why do we user a KeyedPool instead of plain pool ?
A: we do need inputs in makeObject() to create initial clone: createBranchIfMissing,checkOutRemoteBranch 

```
    @Override
    public PooledObject<Git> makeObject(Context key) {
    }
```


Q: can we have GitManager expose strong method prototypes and not deal with Context anymore ?
- extract methods with inputs, context outputs and state (Git, submodules)
    - not clear where to store state
- update GitManager to this contract
- inline wrapping calls in GitProcessor: Context contract moves to GitProcessor
- rename SimpleGitManagerTest into GitProcessorTest

Benefits:
- strong typing on the GitManager: explicit arguments made visible

A: expensive refactoring, only worth if more git related work/complexity, and associated Test cleanup.
=> delay it


## Future improvements


Pooling design is made complex/fragile by 
- the handling of the Context object mapping 
- overdesign since we need a single GitManager interface impl
- YAGNI smells related to GitContext keys + submodule features


How can we make it simpler ? 

Option 1: Reconsider adding pool right into original GitProcessor 

GitProcessor

Pool: GitPoolKey, PooledGit (workDir, Git)
AnonymousFactory inner class

- preCreate: 
	getGitClone()

	- makePoolKey(Context): GitPoolKey
	- pooledGit = pool.borrow(kep)
	   - factory.makeObject()
	       - git = this.cloneRepo()
	       - return new PooledGit(git, workDir)
	   - factory.validateObject()
	   		- this.gitFetchReset()
    - context.setContextKey(PRIVATE_GIT, git)
    - context.setWorkDir(pooledGit.workDir)


- postCreate: 
	getGitClone()

	- makePoolKey(Context): GitPoolKey
	- pooledGit = pool.borrow(kep)
	   - factory.makeObject()
	       - git = this.cloneRepo()
	       - return new PooledGit(git, workDir)
    - context.setContextKey(PRIVATE_GIT, git)
    - context.setWorkDir(pooledGit.workDir)

    commitAndPush()


How to test it ?
- hard to unit test Factory 
- hard to unit test getGitClone()


Option 2: similar to option 1 , but extract GitCloner collaborator which does manipulate GitContext enum, but plain POJO with arguments (lots of test refactoring, see initial POC, because coupled to Context key sharing request/response/state )

=> implies refactoring GitProcessorTest:
	- move/delete test of non public methods to GitClonerTest
	- rewrite test logic as this is currently highly coupled to Context key sharing request/response/state
	   + use JGit api for setUps and asserts.

Option 3: reimplement Git support completely


=> Option 2 seems better long term. Estimate 2-4 MD