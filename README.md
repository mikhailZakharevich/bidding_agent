# Bidding Agent Service

start app:
- sbt run (project directory)

endpoints:
- get all campaigns
  curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://localhost:8080/api/v1/campaigns

- get all stored sites
  curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://localhost:8080/api/v1/sites

- create bid request example
  curl -d '{"id":"bbbbbbb","imp":[{"id":"nnnnnnn","w":160,"h":600,"tagId":"mmmmmmmm"}],"site":{"id":1,"domain":"https://example.com/714"},"user":{"id":"aaaaaa","geo":{"country":"USA"}},"device":{"id":"cccccc","geo":{"country":"Germany"}}}' -H "Content-Type: application/json" -X POST http://localhost:8080/api/v1/bid-request
  or, running from the root folder
  curl -d "@bid-sample.json" -H "Content-Type: application/json" -X POST http://localhost:8080/api/v1/bid-request