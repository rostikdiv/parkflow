# Compute Engine Virtual Machine
resource "google_compute_instance" "app_server" {
  name         = "parkflow-services-vm"
  machine_type = "e2-micro" # Free Tier
  zone         = "${var.region}-a"
  
  # Use Google's Container-Optimized OS
  boot_disk {
    initialize_params {
      image = "cos-cloud/cos-stable"
    }
  }

  network_interface {
    network    = var.vpc_network_id
    subnetwork = var.vpc_subnetwork_name
    
    # We assign an ephemeral public IP so the VM can pull Docker images from the internet.
    # Access to sensitive ports will be restricted by the firewall.
    access_config {
      network_tier = "STANDARD"
    }
  }

  # Pass our startup script (which will start Redis and RabbitMQ)
  metadata = {
    user-data = file("${path.module}/cloud-init.yaml")
  }
}

# Create a Firewall rule so Cloud Run can connect to Redis (6379) and RabbitMQ (5672)
resource "google_compute_firewall" "allow_internal_services" {
  name    = "parkflow-allow-internal-services"
  network = var.vpc_network_id

  allow {
    protocol = "tcp"
    ports    = ["6379", "5672"]
  }

  # Allow connections only from inside our VPC
  source_ranges = ["10.0.0.0/8"]
}
