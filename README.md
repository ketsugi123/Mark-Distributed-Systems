# Mark Distributed Systems

## Overview
The goal is to create a distributed system allowing multiple users to tag photos using cloud resources. The system uses multiple virtual machines (VMs) on Google Cloud Platform (GCP) with GlusterFS for distributed file storage. Clients upload photos in streaming to a server and later download tagged photos from any server.

## Tools Used
- Java 11
- Spread Toolkit
- gRPC
- RabbitMQ
- Docker

### 1. Image Upload
1. The client contacts the Registry Server to obtain the IP of an available server.
2. The client sends the image to the designated server.
3. The server:
    - Notifies the Spread group about the client's status;
    - Generates a unique ID for the image;
    - Stores the image in GlusterFS;
    - Publishes a message on the RabbitMQ exchange with the image ID and keywords.
4. The server returns the image ID to the client.

### 2. Image Processing
1. The message published on the RabbitMQ exchange is consumed by a Worker.
2. The Worker:
    - Retrieves the image corresponding to the ID from GlusterFS;
    - Processes the image (tags it with keywords);
    - Stores the processed image in GlusterFS;
    - Notifies the Spread group about the completion of processing.

### 3. Image Download
1. The client contacts the Registry Server to obtain the IP of an available server.
2. The client requests the server for the image corresponding to a given ID.
3. The server retrieves the image from GlusterFS and sends it to the client.

# Installation Manual

### Pull Docker Images
Execute the following commands to get the necessary Docker images:
```bash
# Registry Server
docker pull cdg16/registry

# Server svc
docker pull cdg16/svc

# RabbitMQ Configurator
docker pull cdg16/rabbitconfig
```

### Run Containers
1. **Registry Server**
```bash
docker run -d -p <port>:<port> --name registry cdg16/registry <port> <daemonIP> <spreadGroup>
```
- `port`: Server port (e.g., 8000);
- `daemonIP`: IP address of the Spread daemon;
- `spreadGroup`: Name of the Spread group.

2. **Server svc**
```bash
docker run -d -p <svcPort>:<svcPort> --name <svcName> -v /var/sharedfiles:/mnt/sharedfs cdg16/svc <svcPort> <daemonIP> <spreadUserName> <spreadGroup> <IP_BROKER> <exchangeName> <routingKey>
```
- `svcPort`: Server port (e.g., 9001);
- `daemonIP`: IP address of the Spread daemon;
- `spreadUserName`: Username for the Spread group. Note: All usernames must start with "svc" due to the JSON parsing logic;
- `spreadGroup`: Name of the Spread group;
- `IP_BROKER`: IP address of RabbitMQ;
- `exchangeName`: Name of the RabbitMQ exchange;
- `routingKey`: Routing key for the RabbitMQ queue.

3. **RabbitMQ Configurator**
```bash
docker run -d --name rabbitconfig cdg16/rabbitconfig <IP_BROKER> <RABBIT_HOST> <EXCHANGE_NAME> <QUEUE_NAME> <ROUTING_KEY>
```
- `IP_BROKER`: IP address of RabbitMQ;
- `RABBIT_HOST`: RabbitMQ host address;
- `EXCHANGE_NAME`: Name of the RabbitMQ exchange;
- `QUEUE_NAME`: Name of the RabbitMQ queue;
- `ROUTING_KEY`: Routing key for the RabbitMQ queue.

4. **Worker**
```bash
java -jar /var/sharedfiles/worker.jar <WORKER_NAME> <daemonIP> <IP_BROKER> <QUEUE_NAME> <spreadGroup>
```
- `WORKER_NAME`: Username for the Spread group;
- `daemonIP`: IP address of the Spread daemon;
- `IP_BROKER`: IP address of RabbitMQ;
- `QUEUE_NAME`: Name of the RabbitMQ queue;
- `spreadGroup`: Name of the Spread group.

*Note: The Worker application is expected to run inside a Google Cloud Console VM with access to the shared GlusterFS directory.*

5. **Client**
```bash
java -jar client.jar <registryPort> <registryAddress>
```
- `registryPort`: Registry server port;
- `registryAddress`: IP address of the Registry server.
