#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <bearer_token> <listing_id> <file_path> [base_url]"
  echo "  base_url defaults to http://localhost:8080"
  exit 1
}

[ $# -lt 3 ] && usage

TOKEN="$1"
LISTING_ID="$2"
FILE="$3"
BASE_URL="${4:-http://localhost:8080}"

if [ ! -f "$FILE" ]; then
  echo "Error: file not found: $FILE" >&2
  exit 1
fi

# Detect content type from extension
case "${FILE##*.}" in
  jpg|jpeg) CONTENT_TYPE="image/jpeg" ;;
  png)      CONTENT_TYPE="image/png" ;;
  webp)     CONTENT_TYPE="image/webp" ;;
  *)
    echo "Error: unsupported file type. Supported: jpg, jpeg, png, webp" >&2
    exit 1
    ;;
esac

# Compute base64-encoded binary MD5 (required by S3)
MD5=$(openssl dgst -md5 -binary "$FILE" | base64)
CONTENT_LENGTH=$(wc -c < "$FILE" | tr -d ' ')

echo "File:           $FILE"
echo "Content-Type:   $CONTENT_TYPE"
echo "Content-Length: $CONTENT_LENGTH"
echo "MD5 (base64):   $MD5"
echo ""

# Step 1: get the presigned URL
echo ">>> Requesting signed URL..."
SIGN_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/upload/signUrl" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"listingId\":$LISTING_ID,\"contentLength\":$CONTENT_LENGTH,\"contentType\":\"$CONTENT_TYPE\",\"md5\":\"$MD5\"}")

echo "Response: $SIGN_RESPONSE"
echo ""

UPLOAD_URL=$(echo "$SIGN_RESPONSE" | jq -r '.uploadUrl')
FILE_KEY=$(echo "$SIGN_RESPONSE" | jq -r '.fileKey')
FILE_URL=$(echo "$SIGN_RESPONSE" | jq -r '.fileUrl // empty')

if [ -z "$UPLOAD_URL" ] || [ "$UPLOAD_URL" = "null" ]; then
  echo "Error: failed to get upload URL" >&2
  exit 1
fi

# Step 2: build headers from uploadHeaders map (each key -> array of values)
HEADER_ARGS=()
HEADER_ARGS+=(-H "Content-Type: $CONTENT_TYPE")
HEADER_ARGS+=(-H "Content-MD5: $MD5")

# Add any extra headers returned by the server (cache-control, x-amz-acl, etc.)
while IFS= read -r header; do
  HEADER_ARGS+=(-H "$header")
done < <(echo "$SIGN_RESPONSE" | jq -r '
  .uploadHeaders // {} |
  to_entries[] |
  # skip headers curl sets automatically
  select(.key | ascii_downcase | IN("content-type","content-md5") | not) |
  "\(.key): \(.value | join(", "))"
')

echo ">>> Uploading to S3..."
UPLOAD_RESPONSE=$(curl -sS -w "\n%{http_code}" -X PUT "$UPLOAD_URL" \
  "${HEADER_ARGS[@]}" \
  --data-binary "@$FILE" 2>&1) || { echo "Error: curl failed: $UPLOAD_RESPONSE" >&2; exit 1; }

HTTP_STATUS=$(echo "$UPLOAD_RESPONSE" | tail -n1)
UPLOAD_BODY=$(echo "$UPLOAD_RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ]; then
  echo "Upload successful (HTTP $HTTP_STATUS)"
  echo ""
  echo "File key: $FILE_KEY"
  [ -n "$FILE_URL" ] && echo "File URL: $FILE_URL"
else
  echo "Upload failed with HTTP $HTTP_STATUS" >&2
  [ -n "$UPLOAD_BODY" ] && echo "$UPLOAD_BODY" >&2
  exit 1
fi
