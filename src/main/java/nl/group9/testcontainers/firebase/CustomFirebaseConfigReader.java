package nl.group9.testcontainers.firebase;

import nl.group9.testcontainers.firebase.json.Emulators;
import nl.group9.testcontainers.firebase.json.FirebaseConfig;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Reader for the firebase.json file to convert it to the {@link nl.group9.testcontainers.firebase.FirebaseEmulatorContainer.FirebaseConfig}
 */
class CustomFirebaseConfigReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Read the firebase config from a firebase.json file
     * @param customFirebaseJson The path to the file
     * @return The configuration
     * @throws IOException In case the file could not be read
     */
    public FirebaseEmulatorContainer.FirebaseConfig readFromFirebase(Path customFirebaseJson) throws IOException {
        var root = readCustomFirebaseJson(customFirebaseJson);

        return new FirebaseEmulatorContainer.FirebaseConfig(
                readHosting(root.getHosting()),
                readStorage(root.getStorage()),
                readFirestore(root.getFirestore()),
                readFunctions(root.getFunctions()),
                readEmulators(root.getEmulators()));
    }

    private record EmulatorMergeStrategy<T>(
            FirebaseEmulatorContainer.Emulator emulator,
            Supplier<T> configObjectSupplier,
            Function<T, Supplier<Integer>> portSupplier) {
    }

    private Map<FirebaseEmulatorContainer.Emulator, FirebaseEmulatorContainer.ExposedPort> readEmulators(Emulators em) {
        var mergeStrategies = new EmulatorMergeStrategy<?>[] {
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.AUTHENTICATION,
                        em::getAuth,
                        a -> a::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI,
                        em::getUi,
                        u -> u::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.EMULATOR_HUB,
                        em::getHub,
                        h -> h::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.LOGGING,
                        em::getLogging,
                        l -> l::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS,
                        em::getFunctions,
                        f -> f::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.EVENT_ARC,
                        em::getEventarc,
                        e -> e::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE,
                        em::getDatabase,
                        d -> d::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE,
                        em::getFirestore,
                        d -> d::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE,
                        em::getStorage,
                        s -> s::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING,
                        em::getHosting,
                        h -> h::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.PUB_SUB,
                        em::getPubsub,
                        h -> h::getPort
                )
        };

        var map = Arrays.stream(mergeStrategies)
                .map(this::mergeEmulator)
                .filter(e -> !Objects.isNull(e))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (em.getFirestore() != null && em.getFirestore().getWebsocketPort() != null) {
            map = new HashMap<>(map);

            map.put(
                    FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE_WS,
                    new FirebaseEmulatorContainer.ExposedPort(em.getFirestore().getWebsocketPort()));

            map = Map.copyOf(map);
        }

        return map;
    }

    private <T> Map.Entry<FirebaseEmulatorContainer.Emulator, FirebaseEmulatorContainer.ExposedPort> mergeEmulator(
            EmulatorMergeStrategy<T> emulatorMergeStrategy) {

        var configObject = emulatorMergeStrategy.configObjectSupplier.get();
        if (configObject != null) {
            var port = emulatorMergeStrategy.portSupplier.apply(configObject).get();
            return Map.entry(emulatorMergeStrategy.emulator, new FirebaseEmulatorContainer.ExposedPort(port));
        } else {
            return null;
        }
    }

    private FirebaseEmulatorContainer.FirestoreConfig readFirestore(Object firestore) {
        if (firestore instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> firestoreMap = (Map<String, String>) firestore;

            var rulesFile = Optional
                    .ofNullable(firestoreMap.get("rules"))
                    .map(this::resolvePath);
            var indexesFile = Optional
                    .ofNullable(firestoreMap.get("indexes"))
                    .map(this::resolvePath);

            return new FirebaseEmulatorContainer.FirestoreConfig(
                    rulesFile,
                    indexesFile);
        } else {
            return FirebaseEmulatorContainer.FirestoreConfig.DEFAULT;
        }
    }

    private FirebaseEmulatorContainer.HostingConfig readHosting(Object hosting) {
        if (hosting instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> hostingMap = (Map<String, String>) hosting;

            var publicDir = Optional
                    .ofNullable(hostingMap.get("public"))
                    .map(this::resolvePath);

            return new FirebaseEmulatorContainer.HostingConfig(
                    publicDir);
        } else {
            return FirebaseEmulatorContainer.HostingConfig.DEFAULT;
        }
    }

    private FirebaseEmulatorContainer.StorageConfig readStorage(Object storage) {
        if (storage instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> storageMap = (Map<String, String>) storage;

            var rulesFile = Optional
                    .ofNullable(storageMap.get("rules"))
                    .map(this::resolvePath);

            return new FirebaseEmulatorContainer.StorageConfig(
                    rulesFile);
        } else {
            return FirebaseEmulatorContainer.StorageConfig.DEFAULT;
        }
    }

    private FirebaseEmulatorContainer.FunctionsConfig readFunctions(Object functions) {
        if (functions instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> functionsMap = (Map<String, Object>) functions;

            var functionsPath = Optional
                    .ofNullable(functionsMap.get("source"))
                    .map(String.class::cast)
                    .map(this::resolvePath);

            var ignores = Optional
                    .ofNullable(functionsMap.get("ignores"))
                    .map(String[].class::cast)
                    .orElse(new String[0]);

            return new FirebaseEmulatorContainer.FunctionsConfig(
                    functionsPath,
                    ignores
            );
        } else {
            return FirebaseEmulatorContainer.FunctionsConfig.DEFAULT;
        }
    }

    private Path resolvePath(String filename) {
        return new File(filename).toPath();
    }

    private FirebaseConfig readCustomFirebaseJson(Path customFirebaseJson) throws IOException {
        var customFirebaseFile = customFirebaseJson.toFile();
        var customFirebaseStream = new BufferedInputStream(new FileInputStream(customFirebaseFile));

        return objectMapper.readerFor(FirebaseConfig.class)
                .readValue(customFirebaseStream);
    }

}
