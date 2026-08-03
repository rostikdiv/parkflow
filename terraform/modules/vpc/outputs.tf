# We need to export the VPC ID so other resources (like Cloud SQL and Cloud Run) can attach to it.
output "network_id" {
  value = google_compute_network.main_vpc.id
}

output "network_name" {
  value = google_compute_network.main_vpc.name
}

output "subnetwork_name" {
  value = google_compute_subnetwork.main_subnet.name
}

# We also export the peering connection to ensure Cloud SQL waits for it to be created.
output "private_vpc_connection_id" {
  value = google_service_networking_connection.private_vpc_connection.id
}
