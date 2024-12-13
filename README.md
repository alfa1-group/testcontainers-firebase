# Firebase Emulator TestContainers Integration

This library extends the [TestContainers](https://www.testcontainers.org/) project by providing a custom container for running [Google Firebase Emulators](https://firebase.google.com/docs/emulator-suite) within a Docker environment. It allows seamless integration of Firebase's local emulators into your test setup.

## Features

- Run Firebase emulators in isolated Docker containers.
- Configure individual emulators with custom settings.
- Support for importing/exporting emulator data.
- Easy setup for Firebase Hosting, Firestore, Storage, and more.

The following emulators have been verified to work:

* Firebase Auth
* Firebase Firestore
* Firebase Emulator UI
* Firebase Hosting
* Realtime Database
* PubSub
* Cloud Storage
* Functions

The following emulators are currently unverified:
* EventArc

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>nl.group9.testcontainers</groupId>
    <artifactId>firebase-emulator-container</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

## Usage

### Basic Example

```java
FirebaseEmulatorContainer container = FirebaseEmulatorContainer.builder()
    .withFirebaseVersion("latest")
    .withProjectId("my-test-project")
    .withFirebaseConfig()
        .withEmulators(
            FirebaseEmulatorContainer.Emulator.AUTHENTICATION,
            FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE
        )
        .done()
    .build();

container.start();
```

## Configuration Options

The `FirebaseEmulatorContainer` provides a flexible configuration system for Docker options, Firebase settings, and emulator-specific configurations.

### Docker Configuration

Customize the Docker environment for the emulator container:

```java
.withDockerConfig()
    .withImage("node:20-alpine")  // Base image for the container
    .withUserId(1000)            // User ID inside the container
    .withGroupId(1000)           // Group ID inside the container
    .done()
```

### Firebase Configuration

Configure Firebase-specific settings:

- **Project ID**: Required for the Authentication emulator.
- **Firebase Version**: Specify the version of `firebase-tools`.
- **Token**: Google Cloud CLI token for authentication.

```java
.withFirebaseVersion("latest")
.withProjectId("test-project-id")
.withToken("your-firebase-token")
```

### Emulator Configuration

Enable and configure specific emulators:

```java
.withFirebaseConfig()
    .withEmulators(
        FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE,
        FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE
    )
    .withEmulatorOnFixedPort(FirebaseEmulatorContainer.Emulator.PUB_SUB, 8085)
    .done()
```

### Hosting Configuration

Configure Firebase Hosting:

```java
.withFirebaseConfig()
    .withHostingPath(Paths.get("/path/to/hosting/content"))
    .done()
```

### Functions Configuration

Configure Firebase Functions

```java
.withFirebaseConfig()
    .withFunctionsFromPath(Patahs.get("/path/to/functions"))
    .withFunctionIgnores(new String[]{"node_modules"})
    .done()
```

### Storage Configuration

Set custom rules for Cloud Storage:

```java
.withFirebaseConfig()
    .withStorageRules(Paths.get("/path/to/storage.rules"))
    .done()
```

### Firestore Configuration

Set custom rules and indexes for Firestore:

```java
.withFirebaseConfig()
    .withFirestoreRules(Paths.get("/path/to/firestore.rules"))
    .withFirestoreIndexes(Paths.get("/path/to/firestore.indexes.json"))
    .done()
```

### Data Import/Export

Specify a directory to import/export emulator data:

```java
.withEmulatorData(Paths.get("/path/to/emulator/data"))
```

### Advanced Options

- **Custom `firebase.json`**:
    ```java
    .readFromFirebaseJson(Paths.get("/path/to/firebase.json"))
    ```
  
  When using a custom firebase.json file, please make the following changes to ensure the
  emulator runs as expected. These changes will ensure the emulator behaves the same as
  running the emulator from the normal CLI interface:

  * Add `"host" : "0.0.0.0"` to all emulator entries
  * Add the hub, logging and UI emulators to the emulators configuration
    ```json
    {
    
      "emulators" : {
        "ui": {
          "port": 4000,
          "enabled": true,
          "host": ".0.0.0."
        },  
        "hub": {
          "port": 4400,
          "host": ".0.0.0."
        },     
        "logging": {
          "port": 4500,
          "host": ".0.0.0."
        }     
      }
    }
    ```
  * If you use Firestore, also set the Firestore Websocket port
    ```json
    {
      "emulators" : {
        "firestore": {
          "port": 8080,
          "websocketPort" : 9150,
          "host": "0.0.0.0"
        }
      }
    }
    ```
  * For both entries, you can of course use your own custom ports, where needed.

- **Java Tool Options**:
    ```java
    .withJavaToolOptions("-Xms512m -Xmx1024m")
    ```

## Emulator Overview

| Emulator                  | Default Port | Description                         |
|---------------------------|--------------|-------------------------------------|
| Authentication            | 9099         | Auth emulator                       |
| Emulator Suite UI         | 4000         | Emulator management UI              |
| Emulator Suite UI Hub     | 4400         | Emulator management UI Hub endpoint |
| Emulator Suite UI Logging | 4500         | Emulator management UI Logging      |
| Realtime Database         | 9000         | Database emulator                   |
| Firestore                 | 8080         | Firestore emulator                  |
| Firestore WebSocket       | 9150         | Firestore WS emulator               |
| Cloud Storage             | 9199         | Storage emulator                    |
| Firebase Hosting          | 5000         | Hosting emulator                    |
| Firebase Functions        | 5001         | Functions emulator                  |
| Pub/Sub                   | 8085         | Pub/Sub emulator                    |

## Contributing

We welcome contributions! Here's how you can get started:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Write tests for your changes.
4. Open a pull request with a clear description of the changes.

For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.
```