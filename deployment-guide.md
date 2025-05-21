# Guía Completa para Crear un Pipeline CI/CD en Oracle Cloud Infrastructure (OCI)

Esta guía detallada te mostrará cómo implementar un pipeline de integración continua y despliegue continuo (CI/CD) en Oracle Cloud Infrastructure utilizando OCI DevOps Projects. El pipeline estará integrado con GitHub y desplegará tu aplicación (frontend y backend) en Oracle Kubernetes Engine (OKE) usando Oracle Container Registry (OCIR).

## Índice
1. [Prerequisitos](#prerequisitos)
2. [Configuración de OCI](#configuración-de-oci)
3. [Configuración de OCIR](#configuración-de-ocir)
4. [Configuración de OKE](#configuración-de-oke)
5. [Actualización de Archivos de Configuración](#actualización-de-archivos-de-configuración)
6. [Integración con GitHub](#integración-con-github)
7. [Creación del Pipeline de DevOps](#creación-del-pipeline-de-devops)
8. [Ejecución y Prueba del Pipeline](#ejecución-y-prueba-del-pipeline)
9. [Monitoreo y Solución de Problemas](#monitoreo-y-solución-de-problemas)

## Prerequisitos

Antes de comenzar, asegúrate de tener:

- Una cuenta activa en Oracle Cloud Infrastructure (OCI)
- Git instalado en tu máquina local
- Docker instalado en tu máquina local
- Kubectl instalado en tu máquina local
- Un repositorio GitHub para el proyecto
- OCI CLI configurado en tu entorno local

## Configuración de OCI

### Paso 1: Configurar OCI CLI (Local)

```bash
# Instalar OCI CLI (en Linux/macOS)
bash -c "$(curl -L https://raw.githubusercontent.com/oracle/oci-cli/master/scripts/install/install.sh)"

# Configurar OCI CLI
oci setup config

# Verificar la instalación
oci --version
```

### Paso 2: Crear un Compartment en OCI (Consola OCI)

1. Inicia sesión en la consola de OCI: https://cloud.oracle.com
2. Navega a Identity & Security → Compartments
3. Haz clic en "Create Compartment"
4. Completa los campos:
   - Nombre: {nombre-de-tu-proyecto}-compartment
   - Descripción: Compartment for {nombre-de-tu-proyecto} application
   - Parent Compartment: (selecciona el compartment raíz)
5. Haz clic en "Create Compartment"

### Paso 3: Crear una Dinámica (Dynamic Group) (Consola OCI)

1. Navega a Identity & Security → Dynamic Groups
2. Haz clic en "Create Dynamic Group"
3. Completa los campos:
   - Nombre: {nombre-de-tu-proyecto}-devops-dg
   - Descripción: Dynamic group for DevOps service
   - Reglas de pertenencia: `ALL {resource.type = 'devopsdeploypipeline', resource.compartment.id = 'ocid1.compartment.oc1..aaaaaa...'}` (reemplaza con tu OCID del compartment)
4. Haz clic en "Create"

### Paso 4: Crear una Política (Consola OCI)

1. Navega a Identity & Security → Policies
2. Haz clic en "Create Policy"
3. Completa los campos:
   - Nombre: {nombre-de-tu-proyecto}-devops-policy
   - Descripción: Policy for DevOps service
   - Compartment: {nombre-de-tu-proyecto}-compartment
   - Declaraciones de política:
   ```
   Allow dynamic-group {nombre-de-tu-proyecto}-devops-dg to manage all-resources in compartment {nombre-de-tu-proyecto}-compartment
   Allow service devops to use ons-topics in compartment {nombre-de-tu-proyecto}-compartment
   Allow service devops to read repos in tenancy
   ```
4. Haz clic en "Create"

## Configuración de OCIR

### Paso 1: Crear Repositorios en OCIR (Consola OCI)

1. Navega a Developer Services → Container Registry
2. Asegúrate de estar en el compartment "{nombre-de-tu-proyecto}-compartment"
3. Haz clic en "Create Repository"
4. Crea dos repositorios:
   
   Para el backend:
   - Nombre: {nombre-de-tu-proyecto}-backend
   - Acceso: Public
   
   Para el frontend:
   - Nombre: {nombre-de-tu-proyecto}-frontend
   - Acceso: Public

5. Anota las rutas completas de los repositorios, que tendrán este formato:
   - `<region-code>.ocir.io/<tenancy-namespace>/{nombre-de-tu-proyecto}-backend`
   - `<region-code>.ocir.io/<tenancy-namespace>/{nombre-de-tu-proyecto}-frontend`

### Paso 2: Generar Token de Autenticación (Consola OCI)

1. Haz clic en tu perfil de usuario (esquina superior derecha)
2. Selecciona "My Profile"
3. En el menú izquierdo, haz clic en "Auth Tokens"
4. Haz clic en "Generate Token"
5. Descripción: {nombre-de-tu-proyecto}-ocir-token
6. Haz clic en "Generate Token"
7. **IMPORTANTE**: Copia y guarda este token en un lugar seguro. No podrás verlo de nuevo.

## Configuración de OKE

### Paso 1: Crear un Cluster de Kubernetes (Consola OCI)

1. Navega a Developer Services → Kubernetes Clusters (OKE)
2. Haz clic en "Create Cluster"
3. Elige "Quick Create" para una configuración rápida
4. Completa los campos:
   - Nombre: {nombre-de-tu-proyecto}-cluster
   - Compartment: {nombre-de-tu-proyecto}-compartment
   - Kubernetes Version: (selecciona la más reciente)
   - Visibility: Public
   - Shape: VM.Standard.E3.Flex (o una que se ajuste a tus necesidades)
   - Node Count: 3
5. Haz clic en "Next" y luego en "Create Cluster"

### Paso 2: Configurar kubectl para OKE (Local)

```bash
# Descargar configuración de kubeconfig
oci ce cluster create-kubeconfig --cluster-id ocid1.cluster.oc1.REGION.xxxx --file ~/.kube/config --region REGION --token-version 2.0.0

# Verificar la conexión
kubectl get nodes
```

### Paso 3: Crear Secret para el Docker Registry (Local)

```bash
# Crear un secret para el Docker Registry
kubectl create secret docker-registry ocir-secret \
  --docker-server=<region-code>.ocir.io \
  --docker-username='<tenancy-namespace>/<username>' \
  --docker-password='<auth-token>' \
  --docker-email='<email>'
```

## Actualización de Archivos de Configuración

### Paso 1: Actualizar el Dockerfile de Backend (Local)

Abre el archivo `MtdrSpring/backend/Dockerfile` y actualízalo con lo siguiente:

```dockerfile
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Instalar dependencias primero para aprovechar la caché de Docker
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline

# Copiar código fuente
COPY src ./src

# Construir aplicación
RUN ./mvnw package -DskipTests

# Segunda etapa: imagen de ejecución
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copiar solo el JAR de la etapa de compilación
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle para conectividad a base de datos
COPY src/main/resources/Wallet_javadev /app/src/main/resources/Wallet_javadev

# Variables de entorno para Java
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Paso 2: Actualizar el Dockerfile de Frontend (Local)

Abre el archivo `MtdrSpring/frontend/Dockerfile` y actualízalo con lo siguiente:

```dockerfile
FROM node:18 AS build

WORKDIR /app

# Instalar bun
RUN npm install -g bun

# Instalar dependencias
COPY package.json bun.lockb ./
RUN bun install

# Copiar código fuente
COPY . .

# Construir aplicación
RUN bun run build

# Segunda etapa: imagen de producción
FROM nginx:alpine

# Copiar archivos de configuración para nginx
COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=build /app/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
```

### Paso 3: Crear archivo Nginx Config para Frontend (Local)

Crea un nuevo archivo `MtdrSpring/frontend/nginx.conf` con el siguiente contenido:

```nginx
server {
    listen       3000;
    server_name  localhost;

    #access_log  /var/log/nginx/host.access.log  main;

    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        try_files $uri $uri/ /index.html;
    }

    # Proxy para las peticiones a la API
    location /api/ {
        proxy_pass http://{nombre-de-tu-proyecto}-backend-service:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Proxy para las peticiones de autenticación
    location /auth/ {
        proxy_pass http://{nombre-de-tu-proyecto}-backend-service:8080/auth/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
```

### Paso 4: Actualizar el Archivo Deployment YAML (Local)

Abre el archivo `MtdrSpring/deployment.yaml` y actualízalo para usar las imágenes de OCIR y los secretos:

```yaml
---
apiVersion: v1
kind: Secret
metadata:
  name: {nombre-de-tu-proyecto}-secrets
stringData:
  # Store sensitive information as Kubernetes secrets
  # Replace these placeholder values with actual credentials in a production environment
  # Consider using a secrets management solution in production
  TELEGRAM_BOT_TOKEN: "${TELEGRAM_BOT_TOKEN}" # Replace with actual token when deploying
  DB_PASSWORD: "${DB_PASSWORD}" # Replace with actual password when deploying
  DB_URL: "${DB_URL}" # Database connection string from environment variable
  DB_USERNAME: "${DB_USERNAME}" # Database username from environment variable
  TNS_ADMIN_PATH: "src/main/resources/Wallet_javadev" # Path to wallet
  DRIVER_CLASS_NAME: "oracle.jdbc.OracleDriver" # Database driver class name
type: Opaque
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {nombre-de-tu-proyecto}-backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: {nombre-de-tu-proyecto}-backend
  template:
    metadata:
      labels:
        app: {nombre-de-tu-proyecto}-backend
    spec:
      containers:
      - name: {nombre-de-tu-proyecto}-backend
        image: ${BACKEND_IMAGE}
        ports:
        - containerPort: 8080
        env:
        # Reference secrets securely via secretKeyRef
        - name: TELEGRAM_BOT_TOKEN
          valueFrom:
            secretKeyRef:
              name: {nombre-de-tu-proyecto}-secrets
              key: TELEGRAM_BOT_TOKEN
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: {nombre-de-tu-proyecto}-secrets
              key: DB_PASSWORD
        - name: spring.datasource.url
          valueFrom:
            secretKeyRef:
              name: {nombre-de-tu-proyecto}-secrets
              key: DB_URL
        - name: spring.datasource.username
          valueFrom:
            secretKeyRef:
              name: {nombre-de-tu-proyecto}-secrets
              key: DB_USERNAME
        - name: spring.datasource.driver-class-name
          valueFrom:
            secretKeyRef:
              name: {nombre-de-tu-proyecto}-secrets
              key: DRIVER_CLASS_NAME
        # TNS_ADMIN points to the wallet path INSIDE the container
        - name: TNS_ADMIN
          valueFrom:
            secretKeyRef:
              name: {nombre-de-tu-proyecto}-secrets
              key: TNS_ADMIN_PATH
      imagePullSecrets:
      - name: ocir-secret
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {nombre-de-tu-proyecto}-frontend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: {nombre-de-tu-proyecto}-frontend
  template:
    metadata:
      labels:
        app: {nombre-de-tu-proyecto}-frontend
    spec:
      containers:
      - name: {nombre-de-tu-proyecto}-frontend
        image: ${FRONTEND_IMAGE}
        ports:
        - containerPort: 3000
        env:
        # API base URL for frontend to communicate with backend
        - name: VITE_API_BASE_URL
          value: "/api"
      imagePullSecrets:
      - name: ocir-secret
---
apiVersion: v1
kind: Service
metadata:
  name: {nombre-de-tu-proyecto}-backend-service
spec:
  selector:
    app: {nombre-de-tu-proyecto}-backend
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP # Internal service not exposed directly outside the cluster
---
apiVersion: v1
kind: Service
metadata:
  name: {nombre-de-tu-proyecto}-frontend-service
spec:
  selector:
    app: {nombre-de-tu-proyecto}-frontend
  ports:
  - port: 3000
    targetPort: 3000
  type: ClusterIP # Internal service accessed through Ingress
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {nombre-de-tu-proyecto}-ingress
  annotations:
    # Specifies NGINX as the ingress controller implementation
    kubernetes.io/ingress.class: "nginx"
    # Rewrites request paths for proper routing
    nginx.ingress.kubernetes.io/rewrite-target: /$1
    nginx.ingress.kubernetes.io/use-regex: "true"
spec:
  rules:
  - http:
      paths:
      # Route /api requests to the backend service
      - path: /api(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: {nombre-de-tu-proyecto}-backend-service
            port:
              number: 8080
      # Route /auth requests to the backend service
      - path: /auth(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: {nombre-de-tu-proyecto}-backend-service
            port:
              number: 8080
      # Route all other requests to the frontend service
      - path: /(.*)
        pathType: Prefix
        backend:
          service:
            name: {nombre-de-tu-proyecto}-frontend-service
            port:
              number: 3000
```

### Paso 5: Crear archivo Build Specification (Local)

Crea un nuevo archivo `MtdrSpring/build_spec.yaml` con el siguiente contenido:

```yaml
version: 0.1
component: build
timeoutInSeconds: 1800
shell: bash
env:
  variables:
    # Información del Docker Registry
    DOCKER_REGISTRY: "${region-code}.ocir.io"
    NAMESPACE: "${namespace}"
    
    # Información del repositorio
    BACKEND_REPO: "${namespace}/{nombre-de-tu-proyecto}-backend"
    FRONTEND_REPO: "${namespace}/{nombre-de-tu-proyecto}-frontend"
    
    # Rutas a los Dockerfiles
    BACKEND_DOCKERFILE_PATH: "MtdrSpring/backend/Dockerfile"
    FRONTEND_DOCKERFILE_PATH: "MtdrSpring/frontend/Dockerfile"
    
    # Rutas a los directorios de construcción
    BACKEND_CONTEXT: "MtdrSpring/backend"
    FRONTEND_CONTEXT: "MtdrSpring/frontend"
    
  exportedVariables:
    - BACKEND_IMAGE_TAG
    - FRONTEND_IMAGE_TAG

steps:
  # Paso 1: Iniciar sesión en OCIR
  - type: Command
    name: "Iniciar sesión en OCIR"
    timeoutInSeconds: 60
    command: |
      echo "Iniciando sesión en OCIR..."
      echo "${OCIR_PASSWORD}" | docker login ${DOCKER_REGISTRY} -u "${NAMESPACE}/oracleidentitycloudservice/${OCIR_USER}" --password-stdin
    onFailure:
      - type: Command
        command: |
          echo "Error al iniciar sesión en OCIR"
          exit 1

  # Paso 2: Construir y publicar imagen de Backend
  - type: Command
    name: "Construir y publicar imagen de Backend"
    timeoutInSeconds: 600
    command: |
      echo "Construyendo imagen de backend..."
      cd ${OCI_PRIMARY_SOURCE_DIR}
      
      # Generar tag único para la imagen de backend
      export BACKEND_IMAGE_TAG="${DOCKER_REGISTRY}/${BACKEND_REPO}:${OCI_BUILD_RUN_ID}"
      echo "Tag para la imagen de backend: ${BACKEND_IMAGE_TAG}"
      
      # Construir la imagen
      docker build -t ${BACKEND_IMAGE_TAG} -f ${BACKEND_DOCKERFILE_PATH} ${BACKEND_CONTEXT}
      
      # Publicar la imagen
      docker push ${BACKEND_IMAGE_TAG}
      echo "Imagen de backend publicada: ${BACKEND_IMAGE_TAG}"
    onFailure:
      - type: Command
        command: |
          echo "Error al construir o publicar la imagen de backend"
          exit 1

  # Paso 3: Construir y publicar imagen de Frontend
  - type: Command
    name: "Construir y publicar imagen de Frontend"
    timeoutInSeconds: 600
    command: |
      echo "Construyendo imagen de frontend..."
      cd ${OCI_PRIMARY_SOURCE_DIR}
      
      # Generar tag único para la imagen de frontend
      export FRONTEND_IMAGE_TAG="${DOCKER_REGISTRY}/${FRONTEND_REPO}:${OCI_BUILD_RUN_ID}"
      echo "Tag para la imagen de frontend: ${FRONTEND_IMAGE_TAG}"
      
      # Crear nginx.conf si no existe
      NGINX_CONF_PATH="${FRONTEND_CONTEXT}/nginx.conf"
      if [ ! -f "${NGINX_CONF_PATH}" ]; then
        echo "Creando nginx.conf..."
        cp MtdrSpring/frontend/nginx.conf "${NGINX_CONF_PATH}"
      fi
      
      # Construir la imagen
      docker build -t ${FRONTEND_IMAGE_TAG} -f ${FRONTEND_DOCKERFILE_PATH} ${FRONTEND_CONTEXT}
      
      # Publicar la imagen
      docker push ${FRONTEND_IMAGE_TAG}
      echo "Imagen de frontend publicada: ${FRONTEND_IMAGE_TAG}"
    onFailure:
      - type: Command
        command: |
          echo "Error al construir o publicar la imagen de frontend"
          exit 1

  # Paso 4: Preparar archivo de despliegue de Kubernetes
  - type: Command
    name: "Preparar archivo de despliegue"
    timeoutInSeconds: 300
    command: |
      echo "Preparando archivo de despliegue..."
      cd ${OCI_PRIMARY_SOURCE_DIR}
      
      # Copiar el archivo de despliegue
      cp MtdrSpring/deployment.yaml deployment.yaml
      
      # Reemplazar los placeholders con las imágenes construidas
      sed -i "s|\${BACKEND_IMAGE}|${BACKEND_IMAGE_TAG}|g" deployment.yaml
      sed -i "s|\${FRONTEND_IMAGE}|${FRONTEND_IMAGE_TAG}|g" deployment.yaml
      
      echo "Archivo de despliegue preparado"
    onFailure:
      - type: Command
        command: |
          echo "Error al preparar el archivo de despliegue"
          exit 1

outputArtifacts:
  - name: deployment-manifest
    type: KUBERNETES_MANIFEST
    location: deployment.yaml
```

### Paso 6: Crear archivo Deploy Specification (Local)

Crea un nuevo archivo `MtdrSpring/deploy_spec.yaml` con el siguiente contenido:

```yaml
version: 0.1
component: deploy
timeoutInSeconds: 1200
shell: bash

files:
  - source: ${OCI_KUBERNETES_MANIFEST_FILE}
    name: {nombre-de-tu-proyecto}-manifest

steps:
  # Paso 1: Conectar al cluster de Kubernetes
  - type: Command
    name: "Conectar al cluster de Kubernetes"
    timeoutInSeconds: 120
    command: |
      echo "Conectando al cluster de Kubernetes..."
      mkdir -p $HOME/.kube
      oci ce cluster create-kubeconfig --cluster-id ${OKE_CLUSTER_OCID} --file $HOME/.kube/config --region ${OCI_REGION} --token-version 2.0.0
      export KUBECONFIG=$HOME/.kube/config
      
      # Verificar la conexión
      kubectl get nodes

  # Paso 2: Verificar namespace y crear si no existe
  - type: Command
    name: "Verificar namespace"
    timeoutInSeconds: 30
    command: |
      echo "Verificando namespace..."
      if ! kubectl get namespace ${KUBERNETES_NAMESPACE} > /dev/null 2>&1; then
        echo "Creando namespace ${KUBERNETES_NAMESPACE}..."
        kubectl create namespace ${KUBERNETES_NAMESPACE}
      fi
      kubectl config set-context --current --namespace=${KUBERNETES_NAMESPACE}

  # Paso 3: Crear secretos de Kubernetes
  - type: Command
    name: "Crear secretos para la aplicación"
    timeoutInSeconds: 60
    command: |
      echo "Creando secretos..."
      
      # Crear registry secret si no existe
      if ! kubectl get secret ocir-secret -n ${KUBERNETES_NAMESPACE} > /dev/null 2>&1; then
        kubectl create secret docker-registry ocir-secret \
          --docker-server=${DOCKER_REGISTRY} \
          --docker-username="${NAMESPACE}/oracleidentitycloudservice/${OCIR_USER}" \
          --docker-password="${OCIR_PASSWORD}" \
          --docker-email="${OCIR_EMAIL}" \
          -n ${KUBERNETES_NAMESPACE}
      fi
      
      # Crear secret para las credenciales de la aplicación
      kubectl create secret generic {nombre-de-tu-proyecto}-secrets \
        --from-literal=TELEGRAM_BOT_TOKEN="${TELEGRAM_BOT_TOKEN}" \
        --from-literal=DB_PASSWORD="${DB_PASSWORD}" \
        --from-literal=DB_URL="${DB_URL}" \
        --from-literal=DB_USERNAME="${DB_USERNAME}" \
        --from-literal=TNS_ADMIN_PATH="src/main/resources/Wallet_javadev" \
        --from-literal=DRIVER_CLASS_NAME="oracle.jdbc.OracleDriver" \
        -n ${KUBERNETES_NAMESPACE} \
        --dry-run=client -o yaml | kubectl apply -f -

  # Paso 4: Aplicar el manifiesto de Kubernetes
  - type: Command
    name: "Desplegar la aplicación"
    timeoutInSeconds: 600
    command: |
      echo "Desplegando la aplicación..."
      
      # Aplicar el manifiesto
      kubectl apply -f ${OCI_WORKSPACE_DIR}/{nombre-de-tu-proyecto}-manifest -n ${KUBERNETES_NAMESPACE}
      
      # Esperar a que los pods estén listos
      kubectl rollout status deployment/{nombre-de-tu-proyecto}-backend -n ${KUBERNETES_NAMESPACE} --timeout=300s
      kubectl rollout status deployment/{nombre-de-tu-proyecto}-frontend -n ${KUBERNETES_NAMESPACE} --timeout=300s
      
      echo "Despliegue completado con éxito"

  # Paso 5: Verificar el despliegue
  - type: Command
    name: "Verificar el despliegue"
    timeoutInSeconds: 120
    command: |
      echo "Verificando el despliegue..."
      
      # Verificar los pods
      kubectl get pods -n ${KUBERNETES_NAMESPACE}
      
      # Verificar los servicios
      kubectl get services -n ${KUBERNETES_NAMESPACE}
      
      # Verificar los ingress
      kubectl get ingress -n ${KUBERNETES_NAMESPACE}
      
      # Obtener la URL de acceso
      INGRESS_IP=$(kubectl get ingress {nombre-de-tu-proyecto}-ingress -n ${KUBERNETES_NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
      if [ -n "${INGRESS_IP}" ]; then
        echo "La aplicación está disponible en: http://${INGRESS_IP}"
      else
        echo "La dirección del ingress aún no está disponible, verifica en unos minutos"
      fi
```

## Integración con GitHub

### Paso 1: Subir los Cambios al Repositorio GitHub (Local)

```bash
# Añadir los archivos modificados
git add .

# Crear un commit
git commit -m "Configuración de CI/CD para OCI DevOps"

# Subir los cambios a GitHub
git push origin main
```

### Paso 2: Configurar Token Personal de GitHub (GitHub)

1. Inicia sesión en GitHub
2. Haz clic en tu perfil (esquina superior derecha) → Settings
3. En el menú lateral, haz clic en Developer settings → Personal access tokens → Tokens (classic)
4. Haz clic en "Generate new token" → "Generate new token (classic)"
5. Nombre: "OCI DevOps"
6. Selecciona los siguientes permisos:
   - repo (completo)
   - admin:repo_hook (completo)
7. Haz clic en "Generate token"
8. Copia y guarda el token en un lugar seguro

## Creación del Pipeline de DevOps

### Paso 1: Crear un Proyecto de DevOps (Consola OCI)

1. Navega a Developer Services → DevOps Projects
2. Haz clic en "Create DevOps Project"
3. Completa los campos:
   - Name: {nombre-de-tu-proyecto}-devops
   - Description: DevOps project for {nombre-de-tu-proyecto} application
   - Compartment: {nombre-de-tu-proyecto}-compartment
   - Notifications Topic: Create new topic
   - Topic Name: {nombre-de-tu-proyecto}-devops-topic
   - Description: Topic for {nombre-de-tu-proyecto} DevOps notifications
4. Haz clic en "Create DevOps Project"

### Paso 2: Conectar con GitHub (Consola OCI)

1. En el proyecto DevOps, navega a Code Repository
2. Haz clic en "External Connection" → "Create External Connection"
3. Completa los campos:
   - Name: github-connection
   - Description: Connection to GitHub repository
   - External Repository Type: GitHub
   - GitHub Repository URL: https://github.com/USUARIO/REPO (URL de tu repositorio)
   - GitHub Access Token: (pega el token generado anteriormente)
4. Haz clic en "Create External Connection"

### Paso 3: Crear un Repositorio de Artefactos (Consola OCI)

1. En el proyecto DevOps, navega a Artifacts
2. Haz clic en "Add Artifact"
3. Completa los campos:
   - Name: kubernetes-manifest
   - Type: Kubernetes manifest
   - Description: Kubernetes deployment manifest
   - Artifact Source: Inline
   - Artifact Registry Repository: (deja como NONE)
   - Artifact Path: {nombre-de-tu-proyecto}-manifest
   - Artifact Version: ${LATEST}
   - Verificar la casilla "Allow parameterization"
4. Haz clic en "Add"

### Paso 4: Crear un Entorno de Despliegue (Consola OCI)

1. En el proyecto DevOps, navega a Environments
2. Haz clic en "Create Environment"
3. Completa los campos:
   - Name: oke-environment
   - Description: OKE cluster for deployments
   - Environment Type: Oracle Kubernetes Engine (OKE) Cluster
   - Compartment: {nombre-de-tu-proyecto}-compartment
   - OKE Cluster: {nombre-de-tu-proyecto}-cluster (selecciona tu cluster)
4. Haz clic en "Create Environment"

### Paso 5: Crear el Build Pipeline (Consola OCI)

1. En el proyecto DevOps, navega a Build Pipelines
2. Haz clic en "Create Build Pipeline"
3. Completa los campos:
   - Name: {nombre-de-tu-proyecto}-build-pipeline
   - Description: Build pipeline for {nombre-de-tu-proyecto} application
4. Haz clic en "Create Build Pipeline"
5. Haz clic en "Add Stage"
6. Elige "Manage Build" → "Managed Build"
7. Completa los campos:
   - Stage Name: build-stage
   - Description: Build container images
   - Primary Code Repository: Elegir el repositorio conectado
   - Build Source: Select Branch
   - Branch: main
   - Build Specification: MtdrSpring/build_spec.yaml
8. Haz clic en "Add" para añadir la etapa
9. Haz clic en "Add Stage" nuevamente
10. Elige "Deliver Artifacts"
11. Completa los campos:
    - Stage Name: deliver-artifacts
    - Description: Deliver Kubernetes manifest
    - Build Configuration:
      - Artifact Name: kubernetes-manifest
      - Build Source: Output Artifact Name: deployment-manifest
12. Haz clic en "Add" para añadir la etapa

### Paso 6: Crear el Deploy Pipeline (Consola OCI)

1. En el proyecto DevOps, navega a Deployment Pipelines
2. Haz clic en "Create Deployment Pipeline"
3. Completa los campos:
   - Pipeline Name: {nombre-de-tu-proyecto}-deploy-pipeline
   - Description: Deploy pipeline for {nombre-de-tu-proyecto} application
4. Haz clic en "Create Pipeline"
5. Haz clic en "Add Stage"
6. Elige "Apply Manifest to your Kubernetes Cluster"
7. Completa los campos:
   - Stage Name: deploy-to-kubernetes
   - Description: Deploy to OKE cluster
   - Environment: oke-environment
   - Deployment Specification Source: Inline
   - Deploy Stage Specification Source File: MtdrSpring/deploy_spec.yaml
   - Kubernetes Namespace: {nombre-de-tu-proyecto}-ns
   - Kubernetes Manifest Artifact: kubernetes-manifest
8. Configura las siguientes variables de entorno (continuación):
   - DOCKER_REGISTRY: (tu región).ocir.io
   - NAMESPACE: (tu tenancy namespace)
   - OCIR_USER: (tu username)
   - OCIR_PASSWORD: (tu auth token)
   - OCIR_EMAIL: (tu email)
   - OKE_CLUSTER_OCID: (OCID de tu cluster OKE)
   - KUBERNETES_NAMESPACE: {nombre-de-tu-proyecto}-ns
   - TELEGRAM_BOT_TOKEN: (tu token de Telegram Bot)
   - DB_URL: (URL de conexión a la BD)
   - DB_USERNAME: (usuario de la BD)
   - DB_PASSWORD: (contraseña de la BD)

9. Haz clic en "Add" para añadir la etapa

### Paso 7: Conectar los Pipelines (Consola OCI)

1. En el proyecto DevOps, navega a Build Pipelines
2. Selecciona el pipeline "{nombre-de-tu-proyecto}-build-pipeline"
3. Haz clic en "Add Stage"
4. Elige "Trigger Deployment"
5. Completa los campos:
   - Stage Name: trigger-deployment
   - Description: Trigger deployment pipeline
   - Select Deployment Pipeline: {nombre-de-tu-proyecto}-deploy-pipeline
6. Haz clic en "Add" para añadir la etapa

### Paso 8: Configurar Disparador (Trigger) (Consola OCI)

1. En el proyecto DevOps, navega a Triggers
2. Haz clic en "Create Trigger"
3. Completa los campos:
   - Name: github-trigger
   - Description: Trigger pipeline from GitHub push
   - Source Connection: github-connection
   - Select Build Pipeline: {nombre-de-tu-proyecto}-build-pipeline
   - Actions:
     - Push
     - Pull Request Create
   - Source Branch: main
   - Filter: (deja vacío para incluir todos los cambios)
4. Haz clic en "Create Trigger"

## Ejecución y Prueba del Pipeline

### Paso 1: Ejecutar Manualmente el Pipeline (Consola OCI)

1. En el proyecto DevOps, navega a Build Pipelines
2. Selecciona el pipeline "{nombre-de-tu-proyecto}-build-pipeline"
3. Haz clic en "Start Manual Run"
4. Haz clic en "Start Run" para confirmar

### Paso 2: Monitorear el Progreso (Consola OCI)

1. Observa el progreso del pipeline en la consola
2. Haz clic en cada etapa para ver detalles y logs
3. Resuelve cualquier error que aparezca

### Paso 3: Verificar el Despliegue (Local)

```bash
# Configurar kubectl para conectarse al cluster OKE
oci ce cluster create-kubeconfig --cluster-id <cluster-ocid> --file ~/.kube/config --region <region>

# Cambiar al namespace de la aplicación
kubectl config set-context --current --namespace={nombre-de-tu-proyecto}-ns

# Verificar los pods
kubectl get pods

# Verificar los servicios
kubectl get services

# Verificar el ingress
kubectl get ingress

# Obtener la IP externa del ingress
kubectl get ingress {nombre-de-tu-proyecto}-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
```

## Monitoreo y Solución de Problemas

### Paso 1: Configurar Logging y Monitoreo (Consola OCI)

1. Navega a Observability & Management → Logging
2. Haz clic en "Create Log Group"
3. Completa los campos:
   - Name: {nombre-de-tu-proyecto}-logs
   - Description: Logs for {nombre-de-tu-proyecto} application
   - Compartment: {nombre-de-tu-proyecto}-compartment
4. Haz clic en "Create"
5. Haz clic en "Create Log"
6. Completa los campos:
   - Name: kubernetes-logs
   - Type: Custom Log
   - Enable Log: Yes
   - Log Category: kubernetes
7. Haz clic en "Create"

### Paso 2: Ver Logs de Kubernetes (Consola OCI)

1. Navega a Developer Services → Kubernetes Clusters
2. Selecciona tu cluster "{nombre-de-tu-proyecto}-cluster"
3. En el menú lateral, selecciona "Logs"
4. Haz clic en "View Container Engine Service Logs"
5. Filtra por namespace "{nombre-de-tu-proyecto}-ns"

### Paso 3: Ver Logs de la Aplicación (Local)

```bash
# Ver logs del backend
kubectl logs -f deployment/{nombre-de-tu-proyecto}-backend -n {nombre-de-tu-proyecto}-ns

# Ver logs del frontend
kubectl logs -f deployment/{nombre-de-tu-proyecto}-frontend -n {nombre-de-tu-proyecto}-ns

# Ver eventos del cluster
kubectl get events -n {nombre-de-tu-proyecto}-ns
```

### Paso 4: Solucionar Problemas Comunes

#### Problema: Las imágenes no se pueden descargar
```bash
# Verificar los secrets de Docker Registry
kubectl get secrets -n {nombre-de-tu-proyecto}-ns
kubectl describe secret ocir-secret -n {nombre-de-tu-proyecto}-ns

# Recrear el secret si es necesario
kubectl create secret docker-registry ocir-secret \
  --docker-server=<region-code>.ocir.io \
  --docker-username='<tenancy-namespace>/<username>' \
  --docker-password='<auth-token>' \
  --docker-email='<email>' \
  -n {nombre-de-tu-proyecto}-ns
```

#### Problema: Los pods no arrancan
```bash
# Obtener detalles del pod
kubectl describe pod <pod-name> -n {nombre-de-tu-proyecto}-ns

# Verificar los logs
kubectl logs <pod-name> -n {nombre-de-tu-proyecto}-ns

# Verificar los configmaps y secrets
kubectl get configmaps -n {nombre-de-tu-proyecto}-ns
kubectl get secrets -n {nombre-de-tu-proyecto}-ns
```

#### Problema: No se puede acceder a la aplicación
```bash
# Verificar el ingress
kubectl describe ingress {nombre-de-tu-proyecto}-ingress -n {nombre-de-tu-proyecto}-ns

# Verificar que el servicio ingress-nginx está instalado
kubectl get svc -n ingress-nginx

# Instalar ingress-nginx si no está presente
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

## Ultimo paso

Disfrutar y enviarlo en Canvas
