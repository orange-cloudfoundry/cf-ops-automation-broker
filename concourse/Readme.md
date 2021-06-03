# COAB dedicated tools

## Cleanup pipeline
It is a pipeline that approve deployment deletion recurrently.

### Load/reload this pipeline
  1. Ensure `reload-coab-cleanup-pipeline.sh` is consistent across environments
  2. You may need to adjust [concourse credentials](credentials-coab-cleanup-pipeline.yml).
  3. Run `reload-coab-cleanup-pipeline.sh`. Please adjust FLY_TARGET and FLY_CMD according to your environment.
