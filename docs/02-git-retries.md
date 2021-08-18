

Retry logic for unavailable gitlab during integration:
- for clone, retry the whole logic if part of the clone fails
- for push commit, need to refine the logic to be idempotent and robust to intermediate failures:
    - commit is done but push failed and is missing:
       - push systematically only when retrying (avoid pushing when no changes to avoid collapsing gitlab remote)
       - detect commits that need push
          - when the remote branch exists
            - https://stackoverflow.com/questions/2016901/viewing-unpushed-git-commits
            - git log origin/branch..branch
          - when the remote branch does not exist:it always exists because we create it during clone
       
Possible implementations:
- https://stackoverflow.com/questions/11692595/design-pattern-for-retrying-logic-that-failed

   - https://github.com/spring-projects/spring-retry 
      - depends on spring framework 4.3.22-release whereas we're already on 5.0.13
      - not very active
      - nice RetryContext to modify behavior during retries    

   - https://github.com/jhalterman/failsafe
      - no dependencies
      - maintained
      - popular

Steps:
- make commit push idempotent
   - split commitPushRepo() into two idempotent methods: 
   - add a test
        - pushes_pending_commits_when_invoked_during_retry
        - does_not_try_to_push_when_no_pending_commit: how to assert no push was made ?
           - stop the git server
                - verify push did not fail with exceptions
              
- add gitmanager retrier
- instanciate in application with hardcoded retry policy
- make retry policy configureable: wait between retries with random

RetryProperties
- toModel() contructs the RetryPolicy


