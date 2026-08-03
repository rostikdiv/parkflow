# Creates a Virtual Private Cloud (VPC) network
resource "google_compute_network" "main_vpc" {
  name                    = "parkflow-vpc"
  auto_create_subnetworks = false # We want to create our own custom subnets
}

# Creates a subnet specifically for our region
resource "google_compute_subnetwork" "main_subnet" {
  name          = "parkflow-subnet"
  ip_cidr_range = "10.0.0.0/24" # Expanded range (256 IPs) required by Cloud Run Direct VPC Egress
  region        = var.region
  network       = google_compute_network.main_vpc.id
}

# Cloud SQL requires a special "Private Services Access" connection to live inside our VPC.
# First, we allocate an IP range for Google's managed services (like Cloud SQL).
resource "google_compute_global_address" "private_ip_address" {
  name          = "google-managed-services-range"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 24
  network       = google_compute_network.main_vpc.id
}

# Second, we establish a peering connection between our VPC and Google's internal network.
resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.main_vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_address.name]
}
