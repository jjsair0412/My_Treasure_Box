#!/bin/bash
set -euo pipefail

# Kubernetes Cluster Inspection Script
# This script inspects multiple Kubernetes clusters and generates resource usage reports.
# It retrieves CPU, Memory, and Disk usage for Deployments and Nodes using Prometheus metrics.

start_time() {
    date +%s
}

end_time() {
    date +%s
}

inspect_cluster() {
    local CLUSTER_NAME=$1
    DATE_STR=$(date +%Y%m%d_%H%M%S)
    OUTPUT="${CLUSTER_NAME}_report_${DATE_STR}.txt"

    # search Prometheus Pod
    PROM_NS_LIST=("monitoring" "cattle-monitoring-system" "prometheus" "kube-system")
    PROMETHEUS_NAMESPACE=""
    PROMETHEUS_POD=""

    for ns in "${PROM_NS_LIST[@]}"; do
        POD=$(kubectl get pods -n "$ns" -l app.kubernetes.io/name=prometheus -o jsonpath="{.items[0].metadata.name}" 2>/dev/null)
        if [ -n "$POD" ]; then
            PROMETHEUS_NAMESPACE="$ns"
            PROMETHEUS_POD="$POD"
            break
        fi
    done

    if [ -z "$PROMETHEUS_POD" ]; then
        echo "Prometheus Pod not found in $CLUSTER_NAME" >> $OUTPUT
        return
    fi
    
    echo "Found Prometheus Pod: $PROMETHEUS_POD in Namespace: $PROMETHEUS_NAMESPACE" >> $OUTPUT


    echo "$CLUSTER_NAME Cluster Resource Inspection Report" >> $OUTPUT
    echo "target Cluster Name: $CLUSTER_NAME" >> $OUTPUT
    echo "Generated at: $(date)" >> $OUTPUT
    echo "======================================" >> $OUTPUT

    # 1. Deployment inspection
    echo "" >> $OUTPUT
    echo "[Deployment 점검]" >> $OUTPUT
    kubectl get deployment --all-namespaces -o wide >> $OUTPUT

    # Deployment CPU/Memory usage
    DEPLOYMENTS=$(kubectl get deployment --all-namespaces -o jsonpath='{range .items[*]}{.metadata.namespace}{"|"}{.metadata.name}{"\n"}{end}')
    for DEP in $DEPLOYMENTS; do
        NS=$(echo $DEP | cut -d'|' -f1)
        NAME=$(echo $DEP | cut -d'|' -f2)
        echo "Deployment: $NS/$NAME" >> $OUTPUT

        # CPU
        CPU=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
            curl -s "http://localhost:9090/api/v1/query?query=sum(rate(container_cpu_usage_seconds_total{namespace=\"$NS\",pod=~\"$NAME.*\",container!=\"\",container!=\"POD\"}[5m]))")
        echo "$DEP CPU Usage: $CPU" >> $OUTPUT

        # Memory
        MEM=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
            curl -s "http://localhost:9090/api/v1/query?query=sum(container_memory_usage_bytes{namespace=\"$NS\",pod=~\"$NAME.*\",container!=\"\",container!=\"POD\"})")
        echo "$DEP Memory Usage: $MEM" >> $OUTPUT
    done

    # 2. Node inspection
    echo "======================================" >> $OUTPUT
    echo "[Node Inspection]" >> $OUTPUT
    kubectl get nodes -o wide >> $OUTPUT

    NODES=$(kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')
    for NODE in $NODES; do
        echo "start to inspect node: $NODE"
        echo "start to inspect node: $NODE" >> $OUTPUT

        # CPU
        NODE_CPU=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
            curl -s "http://localhost:9090/api/v1/query?query=sum(rate(node_cpu_seconds_total{instance=\"$NODE\",mode!=\"idle\"}[5m]))")
        echo "$NODE CPU Usage : $NODE_CPU" >> $OUTPUT

        # Memory
        NODE_MEM=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
            curl -s "http://localhost:9090/api/v1/query?query=node_memory_MemTotal_bytes{instance=\"$NODE\"} - node_memory_MemAvailable_bytes{instance=\"$NODE\"}")
        echo "$NODE  Memory Usage : $NODE_MEM" >> $OUTPUT

        # Disk
        NODE_DISK=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
            curl -s "http://localhost:9090/api/v1/query?query=node_filesystem_size_bytes{instance=\"$NODE\",mountpoint=\"/\"} - node_filesystem_free_bytes{instance=\"$NODE\",mountpoint=\"/\"}")
        echo "$NODE Disk Usage : $NODE_DISK" >> $OUTPUT

        echo "finished inspecting node: $NODE"
    done
    echo "------------------------" >> $OUTPUT

    # 3. Cluster Inspection (All Node)
    echo "" >> $OUTPUT
    echo "start cluster resource inspection..."
    echo "[Cluster Resource Inspection]" >> $OUTPUT

    # Total CPU
    CLUSTER_CPU=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
        curl -s 'http://localhost:9090/api/v1/query?query=sum(rate(container_cpu_usage_seconds_total{container!="",container!="POD"}[5m]))')
    echo "Cluster CPU Usage: $CLUSTER_CPU" >> $OUTPUT

    # Total Memory
    CLUSTER_MEM=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
        curl -s 'http://localhost:9090/api/v1/query?query=sum(container_memory_usage_bytes{container!="",container!="POD"})')
    echo "Cluster Memory Usage: $CLUSTER_MEM" >> $OUTPUT

    # Total Disk
    CLUSTER_DISK=$(kubectl exec -n $PROMETHEUS_NAMESPACE $PROMETHEUS_POD -- \
        curl -s 'http://localhost:9090/api/v1/query?query=sum(node_filesystem_size_bytes{mountpoint="/"}) - sum(node_filesystem_free_bytes{mountpoint="/"})')
    echo "Cluster Disk Usage: $CLUSTER_DISK" >> $OUTPUT
    
    echo "finished cluster resource inspection."
    echo "------------------------" >> $OUTPUT
    echo "Report saved to $OUTPUT"

}

main() {
    echo "start kubernetes cluster inspection..."
    start=$(start_time)
    CONTEXT=$(kubectl config get-contexts -o name)

    for cluster in $CONTEXT; do
        echo "Switching to context: $cluster"
        kubectl config use-context "$cluster"
        echo "start to inspect cluster: $cluster"
        inspect_cluster "$cluster"
        echo "finished inspecting cluster: $cluster"
    done

    end=$(end_time)
    duration=$((end - start))
    echo "Total execution time: $duration seconds"
}
main