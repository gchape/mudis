# Mudis

A high-performance, Redis-like key-value store with pub/sub capabilities built on Netty and Spring.

## Architecture Overview

### System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        CLI[CLI Client]
        APP[Application Client]
    end
    
    subgraph "Network Layer"
        CC[MudisClientCodec]
        SC[MudisServerCodec]
    end
    
    subgraph "Handler Layer"
        CH[MudisClientHandler]
        SH[MudisServerHandler]
    end
    
    subgraph "Core Layer"
        SERVER[MudisServer]
        MSG[Message Types]
        OPS[Operations]
    end
    
    subgraph "Storage Layer"
        KV[Key-Value Store]
        PS[Pub/Sub Engine]
        DS[Data Structures]
    end
    
    CLI --> CC
    APP --> CC
    CC --> CH
    CH --> SC
    SC --> SH
    SH --> SERVER
    SERVER --> MSG
    MSG --> OPS
    OPS --> KV
    OPS --> PS
    OPS --> DS
```

### Communication Flow

```mermaid
sequenceDiagram
    participant Client
    participant ClientCodec
    participant Network
    participant ServerCodec
    participant ServerHandler
    participant Server

    Client->>ClientCodec: sendMessage("SUBSCRIBE ch")
    ClientCodec->>ClientCodec: encode()
    Note over ClientCodec: [0][12][SUBSCRIBE ch]
    ClientCodec->>Network: Send bytes
    Network->>ServerCodec: Receive bytes
    ServerCodec->>ServerCodec: decode()
    Note over ServerCodec: Message.Subscribe("ch")
    ServerCodec->>ServerHandler: channelRead0(msg)
    ServerHandler->>ServerHandler: handleSubscribe()
    ServerHandler->>ServerCodec: writeAndFlush(msg)
    ServerCodec->>ServerCodec: encode()
    Note over ServerCodec: [19][SUBSCRIBED:ch]
    ServerCodec->>Network: Send response
    Network->>ClientCodec: Receive response
    ClientCodec->>ClientCodec: decode()
    Note over ClientCodec: "SUBSCRIBED:ch"
    ClientCodec->>Client: Display response
```

### Protocol Format

```mermaid
graph LR
    subgraph "Request Format"
        OP[Op Ordinal4 bytes] --> SIZE[Args Size4 bytes]
        SIZE --> ARGS[ArgumentsN bytes]
    end
    
    subgraph "Response Format"
        RSIZE[Response Size4 bytes] --> RDATA[Response DataN bytes]
    end
```

## Features

- **Custom Binary Protocol**: Efficient binary protocol with operation codes and length-prefixed arguments
- **Pub/Sub**: Redis-like publish/subscribe messaging
- **Data Persistence**: Optional storage in data structures (Lists, Hashes)
- **High Performance**: Built on Netty with optimized channel options
- **Spring Integration**: Managed lifecycle with Spring Boot
- **Scalable**: Configurable thread pools for boss and worker groups

## Supported Operations

### SUBSCRIBE
Subscribe to a channel to receive published messages.

**Format**: `SUBSCRIBE <channel>`

**Example**: `SUBSCRIBE news`

**Response**: `SUBSCRIBED:news`

### PUBLISH
Publish a message to a channel with optional data structure storage.

**Format**: `PUBLISH <channel> <message> [data_structure]`

**Data Structures**:
- `[]` - Append to list
- `#{}` - Store in hash
- (empty) - Just pub/sub, no persistence

**Examples**:
```
PUBLISH news "Breaking news"
PUBLISH logs "Error occurred" []
PUBLISH metrics "cpu:95" #{}
```

**Response**: `PUBLISHED:news`
