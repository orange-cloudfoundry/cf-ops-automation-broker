module "serviceinstance4567" {
  source = "modules/mymodule"
}

output "4567.started" {
  value = "${module.serviceinstance4567.started}"
}

output "4567.completed" {
  value = "${module.serviceinstance4567.completed}"
}

//"started": "${module.cloudflare-route-ondemandroute5-duplicated.started}",
//"completed": "${module.cloudflare-route-ondemandroute5-duplicated.completed}"

