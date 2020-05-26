package com.atlassian.example;

import com.atlassian.example.db.AtlassianRDS;
import com.atlassian.example.network.AtlassianNetworkController;
import com.atlassian.example.servers.AtlassianBitBucketEC2;
import com.atlassian.example.servers.AtlassianJumpBoxEC2;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.Tag;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.DatabaseInstance;

import java.util.LinkedList;
import java.util.List;

public class AtlassianStack extends Stack {
    public AtlassianStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AtlassianStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        //Setup VPC and Security Group for DB
        Vpc atlassianVpc = AtlassianNetworkController.createVpc(this, "AtalssianVPC");

        //Set Security Group for EC2
        SecurityGroup atlassianServerSecurityGroup = AtlassianNetworkController.createSecurityGroup(this,
                "AtlassianServerSecurityGroup", "Atlassian Serer Security Group", atlassianVpc);
        atlassianServerSecurityGroup.addIngressRule(Peer.ipv4("0.0.0.0/0"), Port.tcp(80));//HTTP
        atlassianServerSecurityGroup.addIngressRule(Peer.ipv4("0.0.0.0/0"), Port.tcp(22));//SSH
        atlassianServerSecurityGroup.addIngressRule(Peer.ipv4("0.0.0.0/0"), Port.tcp(7990));//Bitbucket

        //Set Security Group for RDS
        SecurityGroup atlassianDBSecurityGroup = AtlassianNetworkController.createSecurityGroup(this,
                "AtlassianRDSSecurityGroup", "Atlassian RDSSecurity Group", atlassianVpc);
        atlassianDBSecurityGroup.addIngressRule(atlassianServerSecurityGroup, Port.tcp(5432)); //EC2 Access
        atlassianDBSecurityGroup.addIngressRule(Peer.ipv4("0.0.0.0/0"), Port.tcp(5432));
        List <ISecurityGroup> sgList = new LinkedList<>();
        sgList.add(atlassianDBSecurityGroup);

        //JumpBox
        Instance jumpBoxInstance = AtlassianJumpBoxEC2.getAtlassianBitBucketEC2(this, atlassianVpc,
                this.getRegion(),
                atlassianServerSecurityGroup);

        //Atlassian RDS Database Instance
        DatabaseInstance atlassianRDSInstance = AtlassianRDS.getDatabaseInstance(this, atlassianVpc, sgList);

        //Atlassian Bitbucket EC2 Instance
        Instance bitbucketInstance = AtlassianBitBucketEC2.getAtlassianBitBucketEC2(this, atlassianVpc,
                this.getRegion(),
                atlassianServerSecurityGroup,
                atlassianRDSInstance);

    }
}
