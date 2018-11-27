
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
