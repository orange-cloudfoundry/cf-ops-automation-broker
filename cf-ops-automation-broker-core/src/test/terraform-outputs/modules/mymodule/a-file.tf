resource "local_file" "success_file" {
  content  = "foo!"
  filename = "/tmp/writeable/file"
}

resource "local_file" "error_file" {
  content  = "foo!"
  filename = "/tmp/non-writeable/file"
}

output "started" {
  description = "tracked module was invoked"
  value       = "successfully received module invocation"
}

output "completed" {
  description = "provides a completion status of the module to be tracked by the broker"
  value       = "successfully provisionned ${local_file.error_file.filename =="/tmp/writeable/file" ? "yes": "no"} "

  // and ${local_file.error_file.filename}
}
