package com.atlassian.example.network;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Tag;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;

public class AtlassianNetworkController {
    private AtlassianNetworkController() {

    }

    public static SecurityGroup  createSecurityGroup(Construct scope, String id, String description, Vpc vpc) {
        SecurityGroup securityGroup = SecurityGroup.Builder.create(scope, id)
                .securityGroupName(id)
                .allowAllOutbound(true)
                .description(description)
                .vpc(vpc)
                .build();
        Tag.add(securityGroup, "Project", "Atlassian");

        return securityGroup;
    }

    public static Vpc createVpc(Construct scope, String id) {
        Vpc vpc = Vpc.Builder.create(scope, id)
                .cidr("192.168.0.0/16")
                .maxAzs(3)
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .build();
        Tag.add(vpc, "Project", "Atlassian");
        return vpc;
    }
}
