
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
         

