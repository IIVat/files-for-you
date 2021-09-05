### FilesForYou App

The application consists of client and server parts.
Client sends dummy requests to server via Websocket protocol:
- client status (healthcheck);
- cpu data

Server:
- receives the requests;
- decodes the messages;
- updates clients statuses;
- aggregates the data;
- emulates a saving of the aggregations to a storage;
- has a dummy api for retrieving aggregated data.

CpuData object contains:
- cid  - client id;
- cpu  - current usage of cpu by the application;
- timestamp - the time and date when the data was red from 
  the system.

Client Status (HealthCheck) contains:
- cid - client id;
- isTurnedOn - checks if the app was turned on by user or not.
- timestamp - time, when the event was generated              

## How to run

```
cd ~/your_folder/files-for-you
```

- run server
```
sbt `;project server; run`
```

- run client
```
sbt `;project client; run`
```