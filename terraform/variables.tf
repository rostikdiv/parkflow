# Variables in Terraform allow us to avoid hardcoding values.
# They act like function arguments for our infrastructure.

variable "project_id" {
  description = "The ID of the GCP project"
  type        = string
}

variable "region" {
  description = "The region to deploy resources to"
  type        = string
  default     = "us-central1"
}

variable "db_password" {
  description = "Password for the Cloud SQL database"
  type        = string
  sensitive   = true
  default     = "parkflow_db_password_2026_secure"
}

variable "jwt_secret" {
  description = "Secret key for JWT authentication"
  type        = string
  sensitive   = true
  default     = "ParkflowSuperSecretKeyThatIsAtLeast32CharactersLong2026"
}
