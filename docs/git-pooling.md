
Context:

(Processor) Context <---> GitContext <---> (SimpleGitManager) Context

Responsibities:
- PooledGitManager: interacts with pool, and maps (Processor) Context in/out of GitContext, and pooled Context
- PooledGitFactory: maintains pooled Context, including reset


gitFetchAndReset()      
        - git fetch +refs/heads/develop:refs/remotes/origin/develop
        - git reset 'origin/master' --hard 


