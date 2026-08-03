# This creates the Cloud SQL instance (the actual database server)
resource "google_sql_database_instance" "postgres_instance" {
  name             = "parkflow-db-instance"
  database_version = "POSTGRES_15"
  region           = var.region

  settings {
    # The smallest tier available to save costs
    tier = "db-f1-micro"

    ip_configuration {
      ipv4_enabled    = false # NO public IP! Highly secure.
      private_network = var.vpc_network_id # Attach to our private VPC
    }
  }

  # We tell Terraform to wait for the VPC peering connection to finish FIRST
  depends_on = [var.private_vpc_connection_id]
}

# This creates the actual database inside the server
resource "google_sql_database" "parkflow_db" {
  name     = "parkflow"
  instance = google_sql_database_instance.postgres_instance.name
}

# This creates the database user with the password from Secret Manager
resource "google_sql_user" "db_user" {
  name     = "parkflow"
  instance = google_sql_database_instance.postgres_instance.name
  password = var.db_password
}
