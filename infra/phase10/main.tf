terraform {
  required_version = ">= 1.6.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.116"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy    = true
      recover_soft_deleted_key_vaults = false
    }
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

data "azurerm_client_config" "current" {}

variable "location" {
  type    = string
  default = "southafricanorth"
}

variable "resource_group_name" {
  type    = string
  default = "rg-snip-phase10-lab"
}

variable "aks_cluster_name" {
  type    = string
  default = "aks-snip-phase10-lab"
}

variable "node_vm_size" {
  type    = string
  default = "Standard_E4bs_v5"
}

variable "node_count" {
  type    = number
  default = 2
}

variable "managed_identity_name" {
  type    = string
  default = "id-snip-connector-phase10"
}

variable "kubernetes_namespace" {
  type    = string
  default = "snip"
}

variable "kubernetes_service_account" {
  type    = string
  default = "snip-connector-runtime"
}

resource "random_string" "suffix" {
  length  = 4
  upper   = false
  special = false
}

resource "azurerm_resource_group" "lab" {
  name     = var.resource_group_name
  location = var.location
  tags = {
    purpose = "snip-phase10-personal-lab"
    phase   = "10"
  }
}

resource "azurerm_container_registry" "lab" {
  name                = "acrsnipp10${random_string.suffix.result}"
  resource_group_name = azurerm_resource_group.lab.name
  location            = azurerm_resource_group.lab.location
  sku                 = "Basic"
  admin_enabled       = false
}

resource "azurerm_user_assigned_identity" "connector_secrets" {
  name                = var.managed_identity_name
  resource_group_name = azurerm_resource_group.lab.name
  location            = azurerm_resource_group.lab.location
}

resource "azurerm_kubernetes_cluster" "lab" {
  name                = var.aks_cluster_name
  location            = azurerm_resource_group.lab.location
  resource_group_name = azurerm_resource_group.lab.name
  dns_prefix          = "snip-p10"
  sku_tier            = "Free"
  kubernetes_version  = null
  oidc_issuer_enabled       = true
  workload_identity_enabled = true

  default_node_pool {
    name                        = "sys"
    vm_size                     = var.node_vm_size
    node_count                  = var.node_count
    os_disk_size_gb             = 64
    os_sku                      = "AzureLinux"
    only_critical_addons_enabled = false
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin      = "azure"
    network_plugin_mode = "overlay"
    network_policy      = "cilium"
    network_data_plane  = "cilium"
    load_balancer_sku   = "standard"
  }
}

resource "azurerm_role_assignment" "acr_pull" {
  scope                = azurerm_container_registry.lab.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_kubernetes_cluster.lab.kubelet_identity[0].object_id
}

resource "azurerm_key_vault" "lab" {
  name                       = "kvsnipp10${random_string.suffix.result}"
  location                   = azurerm_resource_group.lab.location
  resource_group_name        = azurerm_resource_group.lab.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  enable_rbac_authorization  = true
  purge_protection_enabled   = false
  soft_delete_retention_days = 7
  public_network_access_enabled = true
}

resource "azurerm_role_assignment" "bootstrap_secrets_officer" {
  scope                = azurerm_key_vault.lab.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

# Human bootstrap/admin remains Key Vault Secrets Officer on the vault.
# The connector UAMI is Key Vault Secrets User only on the configured SNIP
# secrets (credential + trust). Unrelated secrets and SET/DELETE are denied.
resource "azurerm_role_assignment" "connector_credential_get" {
  scope                = "${azurerm_key_vault.lab.id}/secrets/snip-int-ericsson-inventory-reader"
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.connector_secrets.principal_id
}

resource "azurerm_role_assignment" "connector_trust_get" {
  scope                = "${azurerm_key_vault.lab.id}/secrets/snip-int-ericsson-trust"
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.connector_secrets.principal_id
}

resource "azurerm_federated_identity_credential" "connector_runtime" {
  name                = "snip-connector-runtime"
  resource_group_name = azurerm_resource_group.lab.name
  parent_id           = azurerm_user_assigned_identity.connector_secrets.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = azurerm_kubernetes_cluster.lab.oidc_issuer_url
  subject             = "system:serviceaccount:${var.kubernetes_namespace}:${var.kubernetes_service_account}"
}

output "subscription_id" {
  value = data.azurerm_client_config.current.subscription_id
}

output "tenant_id" {
  value = data.azurerm_client_config.current.tenant_id
}

output "resource_group_name" {
  value = azurerm_resource_group.lab.name
}

output "aks_name" {
  value = azurerm_kubernetes_cluster.lab.name
}

output "oidc_issuer_url" {
  value = azurerm_kubernetes_cluster.lab.oidc_issuer_url
}

output "acr_login_server" {
  value = azurerm_container_registry.lab.login_server
}

output "acr_name" {
  value = azurerm_container_registry.lab.name
}

output "key_vault_name" {
  value = azurerm_key_vault.lab.name
}

output "key_vault_uri" {
  value = azurerm_key_vault.lab.vault_uri
}

output "managed_identity_client_id" {
  value       = azurerm_user_assigned_identity.connector_secrets.client_id
  description = "Bind this value to azure.workload.identity/client-id. Not a secret."
}

output "managed_identity_principal_id" {
  value = azurerm_user_assigned_identity.connector_secrets.principal_id
}

output "managed_identity_name" {
  value = azurerm_user_assigned_identity.connector_secrets.name
}
