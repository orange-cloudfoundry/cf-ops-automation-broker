
### Terraform config file naming

This section details the choice identifying terraform modules using their name as primary key.

Assumptions:
- a service instance maps to a single terraform module invocation, along with one or many outputs configs
- module names need to be unique (constraint imposed by TF)

Pros of this decision:
- limits boilerplate and duplication between name and id
- optimizes performance by reducing full scan on "cf cs" by one
 
Implications/limitations from this decision: 

- module names are also used as a unique id in the terraform repository.
This implies having their name relatively short and without too many special characters that would need file system escapes.
- The repository saves a single module config per file on disk. If multiple modules invocations are needed, wrap them in a single parent module call.
- Only direct lookup by service instanceid is supported, other requires a full directory scan & load.

Preserved use-cases:
- mixing modules from different sources in the same repository


### Completion notification

   * Using git
      * with infra specific format
         * terraform state file
         * terraform-produced file markers
   * Using HTTP notification in concourse
   * Using the messaging (rabbitmq)
   
#### TF completion notifications

   - tf state outputs
      - pros:
         - symetric with terraform_state data source mechanism
         - no changes to cf-ops-automation
         - reliable, versionned and auditeable
         - close to full CRUD support (pending update being adressed)
         - more precise feedback with possibility of crafting detailed user-facing messages for successfull cases
      - cons:
         - extra per instance boilerplate root outputs declaration (along with module)
         - current leaks in tf state https://github.com/hashicorp/terraform/issues/13555 
         - current lack of refresh on updates being addressed
         - imprecise feedback on failure: failed to get conditionals on failed resources. 
   - provisionners and [null_resource](https://www.terraform.io/docs/provisioners/null_resource.html):
     - pros: 
        - faster feedback
        - more precise control over sequencing of notification 
     - cons: 
        - ( failure of notification to broker taints the resource by default )
        - Creation-time provisioners are only run during creation, not during updating or any other lifecycle.
        - destroy failure seem complex to be nofified from: need to set count to 0: "destroy-time provisioners must be used sparingly and with care"
     - variations of provisionners:
         - [local_exec](https://www.terraform.io/docs/provisioners/local-exec.html) triggering 
            - curl to broker
            - rabbitmq to broker
         - [file](https://www.terraform.io/docs/provisioners/file.html)
         
   - [local_file](https://www.terraform.io/docs/providers/local/r/file.html) 
         

