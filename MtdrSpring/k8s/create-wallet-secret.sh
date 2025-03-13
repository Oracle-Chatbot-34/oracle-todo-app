#!/bin/bash

# Variables
WALLET_DIR="MtdrSpring/backend/src/main/resources/Wallet_javadev"
SECRET_NAME="oracle-wallet"
NAMESPACE="default"

# Create a temporary directory
TMP_DIR=$(mktemp -d)

# Copy wallet files to the temporary directory
cp "$WALLET_DIR"/* "$TMP_DIR"/

# Create the secret
kubectl create secret generic $SECRET_NAME \
  --from-file="$TMP_DIR/cwallet.sso" \
  --from-file="$TMP_DIR/ewallet.p12" \
  --from-file="$TMP_DIR/keystore.jks" \
  --from-file="$TMP_DIR/ojdbc.properties" \
  --from-file="$TMP_DIR/sqlnet.ora" \
  --from-file="$TMP_DIR/tnsnames.ora" \
  --from-file="$TMP_DIR/truststore.jks" \
  --namespace=$NAMESPACE

# Clean up
rm -rf "$TMP_DIR"

echo "Oracle Wallet secret '$SECRET_NAME' created successfully"