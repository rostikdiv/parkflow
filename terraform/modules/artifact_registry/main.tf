# This file defines the Artifact Registry resource.
# Artifact Registry is a private Docker image repository provided by Google Cloud.

resource "google_artifact_registry_repository" "parkflow_repo" {
  provider      = google
  project       = var.project_id
  location      = var.region
  
  repository_id = "parkflow-repo"
  description   = "Docker repository for ParkFlow backend images"
  format        = "DOCKER" # Specify that this repository will store Docker images
}
