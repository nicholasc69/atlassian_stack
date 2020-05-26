package com.atlassian.example.security;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.ArrayList;
import java.util.List;

public class MyEC2Role {
    private static Role role;

    private static void initRole(Construct scope) {
        role = Role.Builder.create(scope, "MyEC2Role")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .roleName("MyEc2Role")
                .build();
        List<String> actions = new ArrayList<>();
        actions.add("*");

        List<String> resources = new ArrayList<>();
        resources.add("*");

        PolicyStatement statement = PolicyStatement.Builder.create()
                .actions(actions)
                .resources(resources)
                .build();
        role.addToPolicy(statement);
    }

    public static Role getRole(Construct scope) {
        if (role==null)
            initRole(scope);

        return role;
    }
}
