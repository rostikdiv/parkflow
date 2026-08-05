# Create a dedicated Service Account for Cloud Run
resource "google_service_account" "cloudrun_sa" {
  account_id   = "parkflow-cloudrun-sa"
  display_name = "ParkFlow Cloud Run Service Account"
}

# Grant Cloud Run access to read the Database Password
resource "google_secret_manager_secret_iam_member" "db_password_access" {
  secret_id = var.db_password_secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_sa.email}"
}

# Grant Cloud Run access to read the JWT Secret
resource "google_secret_manager_secret_iam_member" "jwt_secret_access" {
  secret_id = var.jwt_secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_sa.email}"
}

# Cloud Run Service (V2)
resource "google_cloud_run_v2_service" "backend" {
  name     = "parkflow-backend"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    # Run the container under our dedicated service account
    service_account = google_service_account.cloudrun_sa.email
    
    scaling {
      min_instance_count = 0 # Scale to Zero (Free!)
      max_instance_count = 2 # DDoS protection
    }

    vpc_access {
      # Direct VPC Egress allows Cloud Run to talk to our private VPC
      network_interfaces {
        network    = var.vpc_network_name
        subnetwork = var.vpc_subnetwork_name
      }
      egress = "PRIVATE_RANGES_ONLY"
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/parkflow-repo/parkflow-backend:latest"

      resources {
        limits = {
          cpu    = "1000m"
          memory = "1024Mi"
        }
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "cloud"
      }
      
      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${var.db_private_ip}:5432/parkflow"
      }
      
      env {
        name  = "SPRING_DATASOURCE_USERNAME"
        value = "parkflow"
      }
      
      # Pull password securely from Secret Manager at runtime
      env {
        name = "SPRING_DATASOURCE_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = var.db_password_secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = var.jwt_secret_id
            version = "latest"
          }
        }
      }
      
      env {
        name  = "SPRING_RABBITMQ_HOST"
        value = var.vm_internal_ip
      }
      
      env {
        name  = "SPRING_RABBITMQ_USERNAME"
        value = "parkflow"
      }
      
      env {
        name  = "SPRING_RABBITMQ_PASSWORD"
        value = "parkflow"
      }
      
      env {
        name  = "SPRING_DATA_REDIS_HOST"
        value = var.vm_internal_ip
      }

      env {
        name  = "MANAGEMENT_TRACING_ENABLED"
        value = "false"
      }

      env {
        name  = "MANAGEMENT_HEALTH_MAIL_ENABLED"
        value = "false"
      }

      env {
        name  = "EMULATOR_URL"
        value = "https://parkflow-emulator-258044247462.us-central1.run.app"
      }

      env {
        name  = "SPRING_RABBITMQ_LISTENER_SIMPLE_DEFAULT_REQUEUE_REJECTED"
        value = "false"
      }

      env {
        name  = "FORCE_RESTART"
        value = "2"
      }

      env {
        name  = "SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE"
        value = "2"
      }
    }
  }

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
      template[0].labels,
      client,
      client_version
    ]
  }

  depends_on = [
    google_secret_manager_secret_iam_member.db_password_access,
    google_secret_manager_secret_iam_member.jwt_secret_access
  ]
}

# Make the backend service publicly accessible over the internet
resource "google_cloud_run_v2_service_iam_member" "public_access" {
  project  = google_cloud_run_v2_service.backend.project
  location = google_cloud_run_v2_service.backend.location
  name     = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
