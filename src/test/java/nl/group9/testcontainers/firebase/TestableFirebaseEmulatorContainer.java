package nl.group9.testcontainers.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.internal.EmulatorCredentials;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Subclass of {@link FirebaseEmulatorContainer} which has some extra facilities to ease testing. Functionally
 * this class is equivalent of its superclass with respect to the testing we need to perform.
 */
public class TestableFirebaseEmulatorContainer extends FirebaseEmulatorContainer {

    private final String name;
    private final Consumer<FirebaseOptions.Builder> options;
    private FirebaseApp app;

    /**
     * Builder for the {@link TestableFirebaseEmulatorContainer}
     */
    public static class Builder extends FirebaseEmulatorContainer.BaseBuilder<TestableFirebaseEmulatorContainer> {

        private Consumer<FirebaseOptions.Builder> options = (options) -> {};
        private String name;

        /**
         * Set the name of the firebase App. This needs to be unique across the JVM
         * @param name The name of the firebase app
         * @return The builder
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set an additional handler for the firebase options builder
         * @param options The handler for the Firebase options builder
         * @return The builder
         */
        public Builder withFirebaseOptions(Consumer<FirebaseOptions.Builder> options) {
            this.options = options;
            return this;
        }

        /**
         * Create the container
         * @return The container.
         */
        @Override
        public TestableFirebaseEmulatorContainer build() {
            return new TestableFirebaseEmulatorContainer(buildConfig(), name, options);
        }
    }

    /**
     * Creates a new Firebase Emulator container
     *
     * @param firebaseConfig The generic configuration of the firebase emulators
     * @param name The name of the firebase app (must be unique across the JVM).
     * @param options Consumer to handle additional changes to the FirebaseOptions.Builder.
     */
    private TestableFirebaseEmulatorContainer(EmulatorConfig firebaseConfig, String name, Consumer<FirebaseOptions.Builder> options) {
        super(firebaseConfig);
        this.name = name;
        this.options = options;
    }

    public static Builder testBuilder() {
        var builder = new Builder();

        /*
         * We determine the current group and user using an env variable. This is set by the GitHub Actions runner.
         * The user and group are used to set the user/group for the user in the docker container run by
         * TestContainers for the Firebase Emulators. This way, the data exported by the Firebase Emulators
         * can be read from the build.
         */
        builder.withDockerConfig()
            .withUserIdFromEnv("CURRENT_USER")
            .withGroupIdFromEnv("CURRENT_GROUP")
            .done();
        builder.withFirebaseVersion("latest");
        builder.withProjectId("demo-test-project");

        return builder;
    }

    @Override
    public void start() {
        super.start();

        var firebaseBuilder = FirebaseOptions.builder()
                .setProjectId("demo-test-project")
                .setCredentials(new EmulatorCredentials());

        if (options != null) {
            options.accept(firebaseBuilder);
        }

        FirebaseOptions options = firebaseBuilder.build();
        app = FirebaseApp.initializeApp(options, name);
    }

    public FirebaseApp getApp() {
        return app;
    }
}
