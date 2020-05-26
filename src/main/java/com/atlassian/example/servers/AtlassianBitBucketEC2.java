package com.atlassian.example.servers;

import com.atlassian.example.security.MyEC2Role;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.SecretsManagerSecretOptions;
import software.amazon.awscdk.core.Tag;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.Grant;
import software.amazon.awscdk.services.iam.IGrantable;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseSecret;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AtlassianBitBucketEC2 {


    public static Instance getAtlassianBitBucketEC2(Construct scope, IVpc vpc, String region,
                                                    SecurityGroup securityGroup,
                                                    DatabaseInstance database) {

        Instance instance;

        //Get EC2Role
        IRole myEC2Role = MyEC2Role.getRole(scope);

        IMachineImage machineImage = AmazonLinuxImage.Builder.create()
                .generation(AmazonLinuxGeneration.AMAZON_LINUX_2)
                .edition(AmazonLinuxEdition.STANDARD)
                .build();

        //Setup Bitbucket EC2 Instance
        //////////////////////////////
        //Setup EBS volume for Bitbucket Home
        BlockDevice bitbucketHomeDevice = BlockDevice.builder()
                .deviceName("/dev/sdf")
                .volume(BlockDeviceVolume.ebs(100))
                .build();
        List<BlockDevice> devices = new ArrayList<BlockDevice>();
        devices.add(bitbucketHomeDevice);

        //Setup User Data
        UserData bitBucketUserData = UserData.forLinux();
        //AWS and Yum Updates
        bitBucketUserData.addCommands("yum update -y\n");
        bitBucketUserData.addCommands("yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/" +
                "latest/linux_amd64/amazon-ssm-agent.rpm\n");
        //Setup WebServer for Health Checks
        bitBucketUserData.addCommands("yum install httpd -y\n");
        bitBucketUserData.addCommands("service httpd start\n");
        bitBucketUserData.addCommands("chkconfig httpd on\n");

        //Create FS and mount bitbucket home drive
        bitBucketUserData.addCommands("mkdir -p /bitbucket/home \n");
        bitBucketUserData.addCommands("chown -R ec2-user:ec2-user /bitbucket \n");
        bitBucketUserData.addCommands("mkfs -t ext4 /dev/sdf \n");
        bitBucketUserData.addCommands("mount /dev/sdf /bitbucket/home \n");
        bitBucketUserData.addCommands("echo '/dev/sdf    /bitbucket/home ext4    defaults    0   0' >> /etc/fstab \n");

        //Configure Bitbucket DB
        bitBucketUserData.addCommands("yum install postgresql jq -y");
        bitBucketUserData.addCommands("export RDSHOST=" + database.getDbInstanceEndpointAddress() +"\n" +
                "export PGUSER=postgres\n" +
                "export PGPASSWORD=\"$(aws secretsmanager get-secret-value  --region us-east-1 " +
                "--secret-id " + database.getSecret().getSecretArn() + " | jq --raw-output .SecretString | jq -r .\"password\")\"\n");

        bitBucketUserData.addCommands("echo \"CREATE DATABASE bitbucket; CREATE USER bitbucket WITH PASSWORD 'bitbucket'; " +
                "GRANT ALL PRIVILEGES ON DATABASE bitbucket to bitbucket;\" >> /tmp/db.sql\n");
        bitBucketUserData.addCommands("psql -U $PGUSER -h $RDSHOST -f /tmp/db.sql");
        bitBucketUserData.addCommands("rm /tmp/db.sql");

        //Download BitBucket
        bitBucketUserData.addCommands("yum remove java* -y \n");
        bitBucketUserData.addCommands("yum install java-1.8.0-openjdk-devel git jq -y \n");

        bitBucketUserData.addCommands("wget https://product-downloads.atlassian.com/software/stash/downloads/atlassian-bitbucket-7.1.3-x64.bin " +
                "-P /tmp\n");
        bitBucketUserData.addCommands("chmod +x /tmp/atlassian-bitbucket-7.1.3-x64.bin \n");

        //Bitbucket Installer Response File
        bitBucketUserData.addCommands("echo 'app.install.service$Boolean=true\n" +
                "portChoice=custom\n" +
                "httpPort=7990\n" +
                "serverPort=8006\n" +
                "app.bitbucketHome=/bitbucket/home\n" +
                "app.defaultInstallDir=/opt/atlassian/bitbucket/7.1.3' >> /tmp/bitbucket.installer");

        //Bitbucket Configuration File
        bitBucketUserData.addCommands("echo 'setup.displayName=Nicks Bitbucket\n" +
        "setup.license=AAABYQ0ODAoPeNqNkUFrwkAQhe/7KxZ6aQ+RTWolCoHGJKWCVTFWKPQyiWNdjGuY3djaX981WWh7K\n" +
                "PS4b/bN9x5zlYPhcU3cD7kYjPrBSAx5kq54IALBZs2hQJpvnzWSjgIhBEuOykBpZnDAqACic+9dV\n" +
                "pWEg77XBtQGaFOA2vfKY+8TmJX0rjeVJSqNq3ONrS/N1tl0vsiWbNFQuQONKRi0+/2hJ0JPBMw5s\n" +
                "o9a0tkNA+GGLkL2BLL6R4Yc6YQ0SaNxPHz0Hm7jpXcXL1+8ySR5cAEtARJUBiky1CDLm0KXJGsjj\n" +
                "6pTLMuOFajyj1DdIhtLnrCzuA4WPJ2keTbzpn5fhP7AHzD7in4rc3oDJTW0xNyV4GPbgiWErfyD2\n" +
                "Pf80BHtX7rk3kKlkVUdc23PdVkUsBS/i4ylKZpyj4ZfXwrzrvENv9yVtyd+HfEUT1gdayS+QjpwV\n" +
                "4JlJ6iaLl1H+gKq4b2QMCwCFFW0KcjbeDAY9VLQCX/X/IesVmekAhRptlwiKVIbtDZxoRfWvXdXu\n" +
                "N+Xfw==X02hd\n" +
        "setup.sysadmin.username=nick\n" +
        "setup.sysadmin.password=password123\n" +
        "setup.sysadmin.displayName=Nick\n" +
        "setup.sysadmin.emailAddress=nicholas@synthesis.co.za\n" +
        "jdbc.driver=org.postgresql.Driver\n" +
        "jdbc.url=jdbc:postgresql://" + database.getDbInstanceEndpointAddress()
                + ":5432/bitbucket\n" +
        "jdbc.driver=org.postgresql.Driver\n" +
        "jdbc.user=bitbucket\n" +
        "jdbc.password=bitbucket\n" +
        "plugin.mirroring.upstream.url=https://bitbucket.company.com' >> /tmp/bitbucket.properties");

        //Install Bitbucket
        bitBucketUserData.addCommands("/tmp/atlassian-bitbucket-7.1.3-x64.bin -q /tmp/bitbucket.installer \n");

        //Start Bitbucket
        bitBucketUserData.addCommands("cp /tmp/bitbucket.properties /bitbucket/home/shared/ \n");
        bitBucketUserData.addCommands("service atlbitbucket start\n");



        //Setup Instance for Bitbucket
        instance =  Instance.Builder
                .create(scope, "Bitbucket EC2 Instance")
                .instanceName("Bitbucket")
                .machineImage(machineImage)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MEDIUM))
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .keyName("nick-" + region)
                .securityGroup(securityGroup)
                .role(myEC2Role)
                .blockDevices(devices)
                .userData(bitBucketUserData)
                .build();
        Tag.add(instance, "Project", "Atlassian");

        return instance;
    }
}
