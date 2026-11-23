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
# server certificate; do that before the 13 months are up. To start over with a new
# CA, delete the whole output directory first (everyone has to install the new ca.crt).
set -euo pipefail

base=${1:?usage: make-selfsigned-cert.sh <base-domain> [server-ip] [output-dir]}
ip=${2:-}
out=${3:-certs}
days=397   # the longest lifetime browsers and Apple accept for a server certificate

umask 077
mkdir -p "$out"
cd "$out"

if [ ! -f ca.key ]; then
  # The CA is limited to this server's own names (X.509 name constraints), so that
  # people who install ca.crt do not end up trusting a CA that could vouch for any
  # website if its key ever leaked. Compliant clients reject anything outside them.
  constraints="permitted;DNS:$base"
  [ -n "$ip" ] && constraints="$constraints,permitted;IP:$ip/255.255.255.255"
  openssl ecparam -name prime256v1 -genkey -noout -out ca.key
  openssl req -x509 -new -key ca.key -sha256 -days 1825 \
    -subj "/O=GRIPL server/CN=GRIPL server CA" \
    -addext "basicConstraints=critical,CA:TRUE,pathlen:0" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -addext "nameConstraints=critical,$constraints" \
    -out ca.crt
  echo "created a new CA (ca.crt), restricted to: $constraints"
else
  echo "keeping the existing CA"
fi

san="DNS:$base,DNS:*.$base"
[ -n "$ip" ] && san="$san,IP:$ip"

# issued under temporary names: the files Traefik serves are only replaced once the result is valid
openssl ecparam -name prime256v1 -genkey -noout -out tls.key.new
openssl req -new -key tls.key.new -subj "/O=GRIPL server/CN=$base" -out tls.csr
printf 'subjectAltName=%s\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth\n' "$san" > tls.ext
openssl x509 -req -in tls.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -days "$days" -sha256 -extfile tls.ext -out tls.crt.new 2>/dev/null
rm -f tls.csr tls.ext ca.srl
# an existing CA only vouches for the names it was created for (a changed IP or domain needs a new CA)
openssl verify -CAfile ca.crt -purpose sslserver tls.crt.new >/dev/null 2>&1 || {
  rm -f tls.key.new tls.crt.new
  echo "error: the existing CA does not accept this certificate (other name, or the CA expired)." >&2
  echo "       Delete $(pwd) and run this again to create a new CA; everyone has to install the new ca.crt." >&2
  exit 1
}
mv tls.key.new tls.key
mv tls.crt.new tls.crt
chmod 644 tls.crt ca.crt
chmod 600 tls.key ca.key

echo "certificate for: $san"
openssl x509 -in tls.crt -noout -enddate
echo "files in $(pwd): ca.crt (give to users) tls.crt tls.key"
