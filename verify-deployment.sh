#!/bin/bash

echo "🔍 DashMaster Deployment Verification Script"
echo "============================================="

# Función para mostrar estado con colores - esto hace el output más claro
show_status() {
    if [ $2 -eq 0 ]; then
        echo "✅ $1"
    else
        echo "❌ $1"
    fi
}

# 1. Verificar estado de pods
echo -e "\n1️⃣ Verificando estado de pods..."
kubectl get pods -n default -l app=dashmaster-backend
kubectl get pods -n default -l app=dashmaster-frontend

# 2. Verificar servicios y sus endpoints
echo -e "\n2️⃣ Verificando servicios..."
kubectl get services -n default | grep dashmaster

# 3. Verificar logs del backend para errores críticos
echo -e "\n3️⃣ Últimos logs del backend..."
BACKEND_POD=$(kubectl get pods -n default -l app=dashmaster-backend -o jsonpath='{.items[0].metadata.name}')
echo "Backend pod: $BACKEND_POD"
kubectl logs $BACKEND_POD -n default --tail=10 | grep -E "(Started|ERROR|WARN)"

# 4. Verificar logs del frontend para configuración de nginx
echo -e "\n4️⃣ Últimos logs del frontend..."
FRONTEND_POD=$(kubectl get pods -n default -l app=dashmaster-frontend -o jsonpath='{.items[0].metadata.name}')
echo "Frontend pod: $FRONTEND_POD"
kubectl logs $FRONTEND_POD -n default --tail=10

# 5. Verificar que nginx tenga configuración válida
echo -e "\n5️⃣ Verificando configuración de nginx..."
kubectl exec $FRONTEND_POD -n default -- nginx -t
show_status "Nginx configuration test" $?

# 6. Verificar health checks internos de ambos servicios
echo -e "\n6️⃣ Verificando health checks..."

# Backend health check - nota el context path /api
echo "Verificando backend health..."
kubectl exec $BACKEND_POD -n default -- curl -s http://localhost:8080/api/actuator/health > /dev/null
show_status "Backend internal health check" $?

# Frontend health check - nuestro endpoint personalizado
echo "Verificando frontend health..."
kubectl exec $FRONTEND_POD -n default -- curl -s http://localhost:80/health > /dev/null
show_status "Frontend internal health check" $?

# 7. Verificar conectividad crítica entre servicios
echo -e "\n7️⃣ Verificando conectividad entre servicios..."
kubectl exec $FRONTEND_POD -n default -- curl -s http://dashmaster-backend-service:8080/api/actuator/health > /dev/null
show_status "Frontend -> Backend connectivity" $?

# 8. Obtener información de acceso externo
echo -e "\n8️⃣ Información de acceso externo..."
EXTERNAL_IP=$(kubectl get service dashmaster-frontend-service -n default -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "🌐 IP Externa: $EXTERNAL_IP"
echo "🔗 URL de la aplicación: http://$EXTERNAL_IP"

# 9. Prueba real del endpoint problemático
echo -e "\n9️⃣ Verificando endpoint de login desde externa..."
if [ ! -z "$EXTERNAL_IP" ]; then
    # Probar con datos de prueba para ver si el endpoint responde
    curl -s -X POST "http://$EXTERNAL_IP/auth/login" \
         -H "Content-Type: application/json" \
         -d '{"username":"test","password":"test"}' \
         -w "HTTP Status: %{http_code}\n" \
         -o /dev/null
    show_status "External login endpoint accessible" $?
    
    echo "Verificando headers de respuesta..."
    curl -s -I "http://$EXTERNAL_IP/auth/login" | head -5
else
    echo "⚠️ No se pudo obtener la IP externa - el LoadBalancer puede estar iniciando"
fi

# 10. Información detallada para debugging
echo -e "\n🔧 Información adicional para debugging..."
echo "Describe del servicio frontend:"
kubectl describe service dashmaster-frontend-service -n default | grep -E "(Endpoints|Port|Type)"

echo -e "\nEstado del pod frontend:"
kubectl describe pod $FRONTEND_POD -n default | grep -E "(Status|Ready|Restart)"

echo -e "\nEstado del pod backend:"
kubectl describe pod $BACKEND_POD -n default | grep -E "(Status|Ready|Restart)"

echo -e "\n🏁 Verificación completada!"
echo "Si hay errores, revisa los logs detallados con:"
echo "kubectl logs $FRONTEND_POD -n default -f"
echo "kubectl logs $BACKEND_POD -n default -f"