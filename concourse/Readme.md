# COAB dedicated tools

## Cleanup pipeline
It is a pipeline that approve deployment deletion recurrently.

### Load/reload this pipeline
  1. Ensure `reload-coab-cleanup-pipeline.sh` is consistent across environments
  2. You may need to adjust [concourse credentials](credentials-coab-cleanup-pipeline.yml).
  3. Run `reload-coab-cleanup-pipeline.sh`. Please adjust FLY_TARGET and FLY_CMD according to your environment.

### Auto-install this pipeline
**pre-requisite**:
  - fly cli available in local environment
  - curl and wget

From secrets root directory:
```bash
mkdir -p coab-tools
cd coab-tools
bash <(curl -SsL https://raw.githubusercontent.com/orange-cloudfoundry/cf-ops-automation-broker/develop/concourse/reload-coab-cleanup-pipeline.sh)
```
