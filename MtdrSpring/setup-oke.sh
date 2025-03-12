#!/bin/bash
set -e

# Variables (ajustar según tu configuración)
CLUSTER_NAME="dashmaster-cluster"
COMPARTMENT_ID="ocid1.compartment.oc1..aaaaaaaa6ysydltnc7kqg3vi2mtzmwnqphnnrlz7qt5uc5zdv7lu4ivm4uxq"
CLUSTER_SHAPE="VM.Standard.A1.Flex"
REGION="mx-queretaro-1"
NODE_POOL_SIZE=2
VCN_NAME="dashmaster-vcn"
VCN_DNS_LABEL="dashmastervcn"  # Modificado: sin guiones
K8S_VERSION="v1.26.2"

# Configurar región de OCI CLI
oci setup repair-file-permissions --file ~/.oci/config
oci setup repair-file-permissions --file ~/.oci/oci_api_key.pem
export OCI_CLI_REGION=$REGION

echo "Creando VCN para el cluster..."
VCN_OCID=$(oci network vcn create \
  --compartment-id $COMPARTMENT_ID \
  --display-name $VCN_NAME \
  --dns-label $VCN_DNS_LABEL \
  --cidr-block "10.0.0.0/16" \
  --query 'data.id' \
  --raw-output)

echo "Configurando reglas de seguridad para el cluster..."
oci network security-list create \
  --compartment-id $COMPARTMENT_ID \
  --vcn-id $VCN_OCID \
  --display-name "${VCN_NAME}-seclist" \
  --egress-security-rules '[{"destination": "0.0.0.0/0", "protocol": "all", "is-stateless": false}]' \
  --ingress-security-rules '[{"source": "0.0.0.0/0", "protocol": "6", "is-stateless": false, "tcp-options": {"destination-port-range": {"min": 80, "max": 80}}}, {"source": "0.0.0.0/0", "protocol": "6", "is-stateless": false, "tcp-options": {"destination-port-range": {"min": 443, "max": 443}}}]'

echo "Creando subredes..."
# Crear subnet para nodos
SUBNET_OCID=$(oci network subnet create \
  --compartment-id $COMPARTMENT_ID \
  --vcn-id $VCN_OCID \
  --display-name "${VCN_NAME}-nodes" \
  --dns-label "nodes" \
  --cidr-block "10.0.10.0/24" \
  --query 'data.id' \
  --raw-output)

# Crear subnet para balanceadores de carga
LB_SUBNET_OCID=$(oci network subnet create \
  --compartment-id $COMPARTMENT_ID \
  --vcn-id $VCN_OCID \
  --display-name "${VCN_NAME}-lb" \
  --dns-label "lb" \
  --cidr-block "10.0.20.0/24" \
  --query 'data.id' \
  --raw-output)

echo "Creando cluster OKE..."
CLUSTER_OCID=$(oci ce cluster create \
  --compartment-id $COMPARTMENT_ID \
  --name $CLUSTER_NAME \
  --vcn-id $VCN_OCID \
  --kubernetes-version $K8S_VERSION \
  --options '{"service_lb_subnet_ids": ["'$LB_SUBNET_OCID'"], "add_ons": {"is_kubernetes_dashboard_enabled": true, "is_tiller_enabled": false}}' \
  --query 'data.id' \
  --raw-output)

# Obtener el ID de imagen más reciente para Oracle Linux 7
echo "Obteniendo imagen de Oracle Linux..."
IMAGE_ID=$(oci compute image list \
  --compartment-id $COMPARTMENT_ID \
  --operating-system "Oracle Linux" \
  --operating-system-version "7.9" \
  --shape $CLUSTER_SHAPE \
  --sort-by TIMECREATED \
  --sort-order DESC \
  --query 'data[0].id' \
  --raw-output)

echo "Utilizando imagen: $IMAGE_ID"

echo "Creando node pool..."
NODE_POOL_OCID=$(oci ce node-pool create \
  --compartment-id $COMPARTMENT_ID \
  --cluster-id $CLUSTER_OCID \
  --name "${CLUSTER_NAME}-pool1" \
  --node-shape $CLUSTER_SHAPE \
  --node-shape-config '{"ocpus": 2, "memory_in_gbs": 12}' \
  --node-source-details '{"source_type": "IMAGE", "image_id": "'$IMAGE_ID'"}' \
  --size $NODE_POOL_SIZE \
  --subnet-ids '["'$SUBNET_OCID'"]' \
  --kubernetes-version $K8S_VERSION \
  --query 'data.id' \
  --raw-output)

echo "Descargando el kubeconfig..."
mkdir -p ~/.kube
oci ce cluster create-kubeconfig \
  --cluster-id $CLUSTER_OCID \
  --file ~/.kube/config \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT

chmod 600 ~/.kube/config

echo "Configuración completa!"
echo "CLUSTER_ID: $CLUSTER_OCID"
echo "NODE_POOL_ID: $NODE_POOL_OCID"
echo "Para verificar el acceso al cluster, ejecute: kubectl get nodes"