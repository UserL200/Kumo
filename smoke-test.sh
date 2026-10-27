#!/usr/bin/env bash
# Walks the core journey against a running backend:
# landlord signs up, adds a property, invites a tenant, tenant joins,
# landlord records rent, tenant confirms it, budget streak updates.
#
# Usage: ./smoke-test.sh [base_url]
# Needs: curl, jq

BASE="${1:-http://localhost:8080}"
STAMP=$(date +%s)
LL_EMAIL="landlord.$STAMP@example.com"
TN_EMAIL="tenant.$STAMP@example.com"
PASS="TestPass123!"

# ADJUST: must be a value your PropertyType enum really has
PROPERTY_TYPE="ROOM_RENTAL"

step() { printf "\n== %s\n" "$1"; }
post() { # path token json
  curl -s -X POST "$BASE$1" -H "Content-Type: application/json" \
    ${2:+-H "Authorization: Bearer $2"} -d "$3"
}
get() { curl -s "$BASE$1" -H "Authorization: Bearer $2"; }

step "1. Landlord signs up"
LL=$(post /api/v1/auth/signup "" "{\"email\":\"$LL_EMAIL\",\"password\":\"$PASS\",\"firstName\":\"Thandi\",\"lastName\":\"Test\",\"role\":\"LANDLORD\"}")
LL_TOKEN=$(echo "$LL" | jq -r '.accessToken')
echo "$LL" | jq '{role: .user.role, email: .user.email}'
[ "$LL_TOKEN" = "null" ] && { echo "FAILED: no token"; echo "$LL"; exit 1; }

step "2. Landlord adds a property"
PROP=$(post /api/v1/landlord/properties "$LL_TOKEN" "{\"address\":\"12 Test St, Soweto, Room A\",\"propertyType\":\"$PROPERTY_TYPE\",\"monthlyRent\":1200,\"landlordName\":\"Thandi Test\"}")
echo "$PROP" | jq .
PROP_ID=$(echo "$PROP" | jq -r '.id')
[ "$PROP_ID" = "null" ] && { echo "FAILED: property not created"; exit 1; }

step "3. Landlord invites the tenant"
INV=$(post "/api/v1/landlord/properties/$PROP_ID/invites" "$LL_TOKEN" "{\"email\":\"$TN_EMAIL\"}")
echo "$INV" | jq .
INV_TOKEN=$(echo "$INV" | jq -r '.token')
[ "$INV_TOKEN" = "null" ] && { echo "FAILED: no invite"; exit 1; }

step "4. Tenant signs up with the invited email and accepts"
TN=$(post /api/v1/auth/signup "" "{\"email\":\"$TN_EMAIL\",\"password\":\"$PASS\",\"firstName\":\"Sipho\",\"lastName\":\"Test\",\"role\":\"TENANT\"}")
TN_TOKEN=$(echo "$TN" | jq -r '.accessToken')
post "/api/v1/invites/$INV_TOKEN/accept" "$TN_TOKEN" "" | jq .

step "5. A second tenant must NOT be able to use the same invite"
OTHER=$(post /api/v1/auth/signup "" "{\"email\":\"other.$STAMP@example.com\",\"password\":\"$PASS\",\"firstName\":\"Eve\",\"lastName\":\"Test\",\"role\":\"TENANT\"}")
OT_TOKEN=$(echo "$OTHER" | jq -r '.accessToken')
post "/api/v1/invites/$INV_TOKEN/accept" "$OT_TOKEN" "" | jq .

step "6. Landlord records this month's rent (should be PENDING)"
TODAY=$(date +%F)
REC=$(post "/api/v1/landlord/properties/$PROP_ID/payments" "$LL_TOKEN" "{\"propertyId\":$PROP_ID,\"amount\":1200,\"paymentDate\":\"$TODAY\"}")
echo "$REC" | jq '{id, status, source}'
REC_ID=$(echo "$REC" | jq -r '.id')

step "7. Landlord must NOT be able to confirm their own entry"
post "/api/v1/payments/$REC_ID/confirm" "$LL_TOKEN" "" | jq .

step "8. Tenant confirms (should become CONFIRMED)"
post "/api/v1/payments/$REC_ID/confirm" "$TN_TOKEN" "" | jq '{id, status}'

step "9. Tenant budget summary (streak should be 1)"
get /api/v1/budget/summary "$TN_TOKEN" | jq '{monthlyRent, onTimeStreakMonths, remaining}'

step "10. Tenant adds an income and an expense"
curl -s -X PUT "$BASE/api/v1/budget/income" -H "Content-Type: application/json" -H "Authorization: Bearer $TN_TOKEN" -d '{"monthlyIncome":4500}' | jq .
post /api/v1/budget/expenses "$TN_TOKEN" '{"category":"FOOD","amount":900}' | jq .
get /api/v1/budget/summary "$TN_TOKEN" | jq '{rentPercentOfIncome, expensesTotal, remaining}'

printf "\nDone. Steps 5 and 7 should show errors; everything else should succeed.\n"