output "instance_connection_name" {
  value = google_sql_database_instance.postgres_instance.connection_name
}

output "private_ip_address" {
  value = google_sql_database_instance.postgres_instance.private_ip_address
}
