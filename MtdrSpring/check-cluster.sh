#!/bin/bash

echo "==== Estado de los nodos del clúster ===="
kubectl get nodes -o wide

echo
echo "==== Estado de los pods ===="
kubectl get pods --all-namespaces

echo
echo "==== Estado de los servicios ===="
kubectl get services --all-namespaces

echo
echo "==== IP pública del servicio ===="
kubectl get service dashmaster-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}'

echo
echo "==== Verificando la conexión a la base de datos ===="
POD_NAME=$(kubectl get pods -l app=dashmaster -o jsonpath='{.items[0].metadata.name}')
kubectl exec $POD_NAME -- curl -s http://localhost:8080/actuator/health | grep database
