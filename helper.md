# Contracts

## RegistryWithClient
```protobuf
service RegistryWithClient {
  rpc getSvc(GetSvcRequest) returns (stream GetSvcResponse);
}

message GetSvcRequest {

}

message GetSvcResponse {
  shared.ServerInfo svc = 1;
}
```

## ServerWithClient
```protobuf
service ServerWithClient {
  rpc upload(stream UploadRequest) returns (stream UploadResponse);
  rpc download(stream DownloadRequest) returns (stream DownloadResponse);
}


message UploadRequest{
  shared.ImageBlock block = 1;
}

message DownloadRequest{
  string id = 1;
}

message UploadResponse{
  string id = 1;
}

message DownloadResponse{
  shared.ImageBlock blocks = 2;
}
```

## Shared
```protobuf
message ServerInfo {
  string ip = 1;
  uint32 port = 2;
}

message ImageBlock {
  ImageBlockMetadata metadata = 5;
  bytes pixel_data = 6;
}

message ImageBlockMetadata {
  uint32 sequence_number = 1;
  uint32 total_blocks = 2;
}
```

# Demo Setup

## registry:
> In VM 1

```
docker run -d -p 8000:8000 --name registry cdg16/registry 8000 35.241.217.25 demogrp
```

## svc1:
> In VM 2

```
docker run -d -p 9001:9001 --name svc1 -v /var/sharedfiles:/mnt/sharedfs cdg16/svc 9001 35.205.12.238 svc1 demogrp 35.241.217.25 demoex demokey
```

## svc2:
> In VM 2

```
docker run -d -p 9002:9002 --name svc2 -v /var/sharedfiles:/mnt/sharedfs cdg16/svc 9002 35.205.12.238 svc2 demogrp 35.241.217.25 demoex demokey
```

## svc3:
> In VM 3

```
docker run -d -p 9003:9003 --name svc3 -v /var/sharedfiles:/mnt/sharedfs cdg16/svc 9003 35.241.233.104 svc3 demogrp 35.241.217.25 demoex demokey
```

## rabbit config:
> In VM 1

```
docker run -d --name rabbitconfig cdg16/rabbitconfig 35.241.217.25 35.241.217.25 demoex workers demokey
```

## worker1:
> In VM 3

```
java -jar /var/sharedfiles/worker.jar worker1 35.241.233.104 35.241.217.25 workers demogrp
```

## worker2:
> In VM 1

```
java -jar /var/sharedfiles/worker.jar worker2 35.241.217.25 35.241.217.25 workers demogrp
```

## client:
```
java -jar client.jar 8000 35.241.217.25
```
