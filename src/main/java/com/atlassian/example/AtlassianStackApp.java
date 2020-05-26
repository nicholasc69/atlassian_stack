package com.atlassian.example;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class AtlassianStackApp {

    // Helper method to build an environment
    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }

    public static void main(final String[] args) {
        App app = new App();
        Environment synthesisEnv = makeEnv("296274010522", "us-east-1");
        new AtlassianStack(app, "atlassian-stack", StackProps.builder()
                                                            .stackName("AtlassianStack")
                                                            .env(synthesisEnv)
                                                            .build());
        app.synth();
    }
}
