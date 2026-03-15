#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080/api/webhooks"
# Update this with a message ID that exists in your tbWhatsAppLog table
TEST_MESSAGE_ID="CHANGE_OR_USE_ACTUAL_ID"

echo "--- Testing Infobip Webhook (DELIVERED) ---"
curl -X POST "$BASE_URL/infobip" \
     -H "Content-Type: application/json" \
     -d '{
       "results": [
         {
           "messageId": "'"$TEST_MESSAGE_ID"'",
           "status": {
             "id": 5,
             "groupId": 3,
             "groupName": "DELIVERED",
             "name": "DELIVERED_TO_HANDSET",
             "description": "Message delivered to handset"
           }
         }
       ]
     }'
echo -e "\n"

echo "--- Testing Infobip Webhook (FAILED) ---"
curl -X POST "$BASE_URL/infobip" \
     -H "Content-Type: application/json" \
     -d '{
       "results": [
         {
           "messageId": "'"$TEST_MESSAGE_ID"'",
           "status": {
             "name": "REJECTED"
           },
           "error": {
             "id": 2,
             "name": "NO_WHATSAPP_ACCOUNT",
             "description": "The number does not have a WhatsApp account"
           }
         }
       ]
     }'
echo -e "\n"

echo "--- Testing 360Dialog Webhook (READ) ---"
curl -X POST "$BASE_URL/360dialog" \
     -H "Content-Type: application/json" \
     -d '{
       "statuses": [
         {
           "id": "'"$TEST_MESSAGE_ID"'",
           "status": "read",
           "timestamp": "1614850020",
           "recipient_id": "911234567890"
         }
       ]
     }'
echo -e "\n"

echo "--- Testing 360Dialog Webhook (FAILED) ---"
curl -X POST "$BASE_URL/360dialog" \
     -H "Content-Type: application/json" \
     -d '{
       "statuses": [
         {
           "id": "'"$TEST_MESSAGE_ID"'",
           "status": "failed",
           "errors": [
             {
               "code": 131030,
               "title": "Recipient unavailable",
               "details": "The recipient is not available on WhatsApp."
             }
           ]
         }
       ]
     }'
echo -e "\n"

echo "Check your 'tbWhatsAppLog' and 'tbWhatsAppLogDetail' tables to verify the updates!"
