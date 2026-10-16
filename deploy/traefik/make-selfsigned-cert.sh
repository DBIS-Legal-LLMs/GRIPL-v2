#!/usr/bin/env bash
# Create a small private CA and a server certificate for the base domain.
#
#   ./make-selfsigned-cert.sh <base-domain> [server-ip] [output-dir]
#   ./make-selfsigned-cert.sh 134.60.71.167.nip.io 134.60.71.167
#
# Writes into <output-dir> (default ./certs, next to the compose file):
#   tls.crt / tls.key   what Traefik serves (covers <base-domain>, *.<base-domain>, the IP)
#   ca.crt              hand this to people; installing it as a trusted CA in the
#                       OS/browser removes the warning on their machine
#   ca.key              keep it on the server, never share it
# Re-running keeps the CA (so installed copies stay valid) and issues a fresh
# server certificate; do that before the 13 months are up.
set -euo pipefail

base=${1:?usage: make-selfsigned-cert.sh <base-domain> [server-ip] [output-dir]}
ip=${2:-}
out=${3:-certs}
days=397   # the longest lifetime browsers and Apple accept for a server certificate

umask 077
mkdir -p "$out"
cd "$out"

if [ ! -f ca.key ]; then
  openssl ecparam -name prime256v1 -genkey -noout -out ca.key
  openssl req -x509 -new -key ca.key -sha256 -days 1825 \
    -subj "/O=GRIPL server/CN=GRIPL server CA" -out ca.crt
  echo "created a new CA (ca.crt)"
else
  echo "keeping the existing CA"
fi

san="DNS:$base,DNS:*.$base"
[ -n "$ip" ] && san="$san,IP:$ip"

openssl ecparam -name prime256v1 -genkey -noout -out tls.key
openssl req -new -key tls.key -subj "/O=GRIPL server/CN=$base" -out tls.csr
printf 'subjectAltName=%s\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth\n' "$san" > tls.ext
openssl x509 -req -in tls.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -days "$days" -sha256 -extfile tls.ext -out tls.crt 2>/dev/null
rm -f tls.csr tls.ext ca.srl
chmod 644 tls.crt ca.crt
chmod 600 tls.key ca.key

echo "certificate for: $san"
openssl x509 -in tls.crt -noout -enddate
echo "files in $(pwd): ca.crt (give to users) tls.crt tls.key"
