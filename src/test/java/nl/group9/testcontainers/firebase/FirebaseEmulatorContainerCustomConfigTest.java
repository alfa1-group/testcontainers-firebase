package nl.group9.testcontainers.firebase;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class FirebaseEmulatorContainerCustomConfigTest {

    private static final File tempEmulatorDataDir;

    static {
        try {
            // Create a temporary directory for emulator data
            tempEmulatorDataDir = Files.createTempDirectory("firebase-emulator-data").toFile();
            firebaseContainer = TestableFirebaseEmulatorContainer.testBuilder()
                    .withName("FirebaseEmulatorContainerCustomConfigTest")
                    .withEmulatorData(tempEmulatorDataDir.toPath())
                    .readFromFirebaseJson(new File("firebase.json").toPath())
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Container
    private static final FirebaseEmulatorContainer firebaseContainer;

    @Test
    public void testFirestoreRulesAndIndexes() throws InterruptedException, IOException {
        // Verify the firebase.json file exists in the container
        String firebaseJsonCheck = firebaseContainer.execInContainer("cat", "/srv/firebase/firebase.json").getStdout();
        assertTrue(firebaseJsonCheck.contains("\"emulators\""), "Expected firebase.json to be present in the container");

        // Verify the firestore.rules file exists in the container
        String firestoreRulesCheck = firebaseContainer.execInContainer("cat", "/srv/firebase/firestore.rules").getStdout();
        assertTrue(firestoreRulesCheck.contains("service cloud.firestore"),
                "Expected firestore.rules to be present in the container");
    }

    @Test
    public void testStorageRules() throws IOException, InterruptedException {
        // Verify the storage.rules file exists in the container
        String storageRulesCheck = firebaseContainer.execInContainer("cat", "/srv/firebase/storage.rules").getStdout();
        assertTrue(storageRulesCheck.contains("service firebase.storage"),
                "Expected storage.rules to be present in the container");
    }

}
