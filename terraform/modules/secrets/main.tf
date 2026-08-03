# --- Database Password Secret ---
resource "google_secret_manager_secret" "db_password" {
  secret_id = "parkflow-db-password"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_password_version" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

# --- JWT Secret ---
resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "parkflow-jwt-secret"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "jwt_secret_version" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = var.jwt_secret
}
