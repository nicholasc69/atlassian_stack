package com.atlassian.example.servers;

import com.atlassian.example.security.MyEC2Role;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.SecretsManagerSecretOptions;
import software.amazon.awscdk.core.Tag;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.secretsmanager.ISecret;

import java.util.ArrayList;
import java.util.List;

public class AtlassianJumpBoxEC2 {


    public static Instance getAtlassianBitBucketEC2(Construct scope, IVpc vpc, String region,
                                                    SecurityGroup securityGroup) {

        Instance instance;

        //Get EC2Role
        IRole myEC2Role = MyEC2Role.getRole(scope);

        IMachineImage machineImage = AmazonLinuxImage.Builder.create()
                .generation(AmazonLinuxGeneration.AMAZON_LINUX_2)
                .edition(AmazonLinuxEdition.STANDARD)
                .build();

        //Setup Instance for Bitbucket
        instance =  Instance.Builder
                .create(scope, "JumpBox EC2 Instance")
                .instanceName("JumpBox")
                .machineImage(machineImage)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .keyName("nick-" + region)
                .securityGroup(securityGroup)
                .role(myEC2Role)
                .build();
        Tag.add(instance, "Project", "Atlassian");

        return instance;
    }
}
