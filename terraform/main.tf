# Main Terraform configuration file.
# Specifies required providers (plugins) like the Google Cloud provider.

terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0" # Use version 5.x of the provider
    }
  }
}

# Configure the provider: specifies the target project and region for the resources.
provider "google" {
  project = var.project_id
  region  = var.region
}

# Load the Artifact Registry module from the local modules/ directory
module "artifact_registry" {
  source     = "./modules/artifact_registry"
  project_id = var.project_id
  region     = var.region
}

# Load the VPC module
module "vpc" {
  source = "./modules/vpc"
  region = var.region
}

# Load the Secret Manager module
module "secrets" {
  source      = "./modules/secrets"
  db_password = var.db_password
  jwt_secret  = var.jwt_secret
}

# Load the Cloud SQL module
module "cloudsql" {
  source                    = "./modules/cloudsql"
  region                    = var.region
  vpc_network_id            = module.vpc.network_id
  private_vpc_connection_id = module.vpc.private_vpc_connection_id
  db_password               = var.db_password
}

# Load the Compute module for Redis and RabbitMQ
module "compute" {
  source              = "./modules/compute"
  region              = var.region
  vpc_network_id      = module.vpc.network_id
  vpc_subnetwork_name = module.vpc.subnetwork_name
}

# Load the Cloud Run module for the Spring Boot application
module "cloudrun" {
  source                = "./modules/cloudrun"
  region                = var.region
  project_id            = var.project_id
  vpc_network_name      = module.vpc.network_name
  vpc_subnetwork_name   = module.vpc.subnetwork_name
  db_private_ip         = module.cloudsql.private_ip_address
  vm_internal_ip        = module.compute.internal_ip
  db_password_secret_id = module.secrets.db_password_secret_id
  jwt_secret_id         = module.secrets.jwt_secret_id
}
