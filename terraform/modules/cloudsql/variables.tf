variable "region" {
  type = string
}

variable "vpc_network_id" {
  type = string
}

variable "private_vpc_connection_id" {
  type = any
}

variable "db_password" {
  type      = string
  sensitive = true
}
