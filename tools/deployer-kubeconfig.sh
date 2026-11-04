#!/usr/bin/env bash
# Baut die kubeconfig für das GitHub-Secret KUBECONFIG aus dem ServiceAccount github-deployer
# (k8s/setup/deployer.yaml). Sie darf nur im Namespace altsetter deployen.
#
#   tools/deployer-kubeconfig.sh https://<api-server>:6443 | gh secret set KUBECONFIG
#
# Die Ausgabe enthält ein Token: nicht in Dateien speichern, direkt weiterreichen.
set -euo pipefail

server="${1:?Adresse des Kubernetes-API-Servers angeben, z. B. https://203.0.113.10:6443}"
namespace=altsetter

ca="$(kubectl -n "$namespace" get secret github-deployer-token -o jsonpath='{.data.ca\.crt}')"
token="$(kubectl -n "$namespace" get secret github-deployer-token -o jsonpath='{.data.token}' | base64 -d)"

cat <<EOF
apiVersion: v1
kind: Config
clusters:
  - name: altsetter
    cluster:
      server: ${server}
      certificate-authority-data: ${ca}
users:
  - name: github-deployer
    user:
      token: ${token}
contexts:
  - name: altsetter
    context:
      cluster: altsetter
      user: github-deployer
      namespace: ${namespace}
current-context: altsetter
EOF
