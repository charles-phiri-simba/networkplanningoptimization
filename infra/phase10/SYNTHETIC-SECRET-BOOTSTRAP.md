# Synthetic connector secrets must never enter Terraform state.

# Required environment (values are not committed):
#   SNIP_AZURE_KEY_VAULT_NAME
#   SNIP_SYNTHETIC_ERICSSON_USERNAME
#   SNIP_SYNTHETIC_ERICSSON_PASSWORD
#   SNIP_SYNTHETIC_NOKIA_USERNAME
#   SNIP_SYNTHETIC_NOKIA_PASSWORD

# Example (run from a protected bootstrap identity, not GitHub Actions for the pod):
#
#   az keyvault secret set \
#     --vault-name "$SNIP_AZURE_KEY_VAULT_NAME" \
#     --name snip-int-ericsson-inventory-reader \
#     --value "{\"username\":\"$SNIP_SYNTHETIC_ERICSSON_USERNAME\",\"password\":\"$SNIP_SYNTHETIC_ERICSSON_PASSWORD\"}"
#
# GitHub Actions may orchestrate the AKS job. It must not `az keyvault secret show` the connector secret
# for the application pod. The pod retrieves the secret through Workload Identity.

Do not place username, password, token, PKCS12, or private-key values in `*.tf`, `terraform.tfvars`, or remote state.
