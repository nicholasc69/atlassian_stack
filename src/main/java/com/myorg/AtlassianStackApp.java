package com.myorg;

import software.amazon.awscdk.core.App;

import java.util.Arrays;

public class AtlassianStackApp {
    public static void main(final String[] args) {
        App app = new App();

        new AtlassianStackStack(app, "AtlassianStackStack");

        app.synth();
    }
}
