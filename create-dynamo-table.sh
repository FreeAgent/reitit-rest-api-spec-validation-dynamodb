#!/bin/bash

aws dynamodb --endpoint-url http://localhost:8000 \
    create-table \
    --table-name transactions \
    --attribute-definitions \
        AttributeName=customerid,AttributeType=S \
        AttributeName=timestamp,AttributeType=N \
    --key-schema \
        AttributeName=customerid,KeyType=HASH \
        AttributeName=timestamp,KeyType=RANGE \
--provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5
