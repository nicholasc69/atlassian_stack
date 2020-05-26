package com.atlassian.example.db;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.DatabaseSecret;
import software.amazon.awscdk.services.rds.IOptionGroup;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;

import java.util.List;

public class AtlassianRDS {

    public static DatabaseInstance getDatabaseInstance(Construct scope, IVpc vpc, List<ISecurityGroup> securityGroup) {
        //RDS Postgresql DB for Bitbucket and Bamboo
        DatabaseInstance instance;

        String masterUsername = "postgres";

        instance = DatabaseInstance.Builder.create(scope, "AtlassianDatabase")
                .engine(DatabaseInstanceEngine.POSTGRES)
                .engineVersion("9.6.16")
                .instanceClass(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .instanceIdentifier("AtlassianDB")
                .vpc(vpc)
                .vpcPlacement(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .securityGroups(securityGroup)
                .multiAz(false)
                .deletionProtection(false)
                .deleteAutomatedBackups(true)
                .backupRetention(Duration.days(0))
                .removalPolicy(RemovalPolicy.DESTROY)
                .iamAuthentication(true)
                .masterUsername(masterUsername)
                .build();
        Tag.add(instance, "Project", "Atlassian");

        return instance;

    }
}
