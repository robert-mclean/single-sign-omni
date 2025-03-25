import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as elbv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import * as ecrAssets from "aws-cdk-lib/aws-ecr-assets";
import * as route53 from "aws-cdk-lib/aws-route53";
import * as targets from "aws-cdk-lib/aws-route53-targets";
import * as certificatemanager from "aws-cdk-lib/aws-certificatemanager";
import * as ecs_patterns from "aws-cdk-lib/aws-ecs-patterns";
import * as s3 from "aws-cdk-lib/aws-s3";
import * as s3deploy from "aws-cdk-lib/aws-s3-deployment";
import * as cloudfront from "aws-cdk-lib/aws-cloudfront";
import * as cloudfrontOrigins from "aws-cdk-lib/aws-cloudfront-origins";
import * as route53_targets from "aws-cdk-lib/aws-route53-targets";

export class SingleSignOmniStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create a VPC (with default settings, max 2 AZs)
    const vpc = new ec2.Vpc(this, "SingleSignOmniVpc", {
      maxAzs: 2,
    });

    // Create an ECS Cluster
    const apiCluster = new ecs.Cluster(this, "SingleSignOmniApiCluster", {
      vpc,
    });

    // Build a Docker image from the directory containing your Spring Boot project.
    // Make sure your Dockerfile is present in the given directory.
    const apiDockerImageAsset = new ecrAssets.DockerImageAsset(
      this,
      "SingleSignOmniApi",
      {
        directory: "./api",
      }
    );

    // Define a Fargate Task Definition
    const apiTaskDefinition = new ecs.FargateTaskDefinition(
      this,
      "SingleSignOmniApiTaskDef",
      {
        memoryLimitMiB: 1024,
        cpu: 512,
      }
    );

    // // Add the container to the task definition
    apiTaskDefinition.addContainer("SingleSignOmniApiContainer", {
      image: ecs.ContainerImage.fromDockerImageAsset(apiDockerImageAsset),
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: "SamlIdp" }),
      portMappings: [{ containerPort: 8080 }],
    });

    // // Create a Fargate Service
    // const apiFargateService = new ecs.FargateService(
    //   this,
    //   "SingleSignOmniApiService",
    //   {
    //     cluster: apiCluster,
    //     taskDefinition: apiTaskDefinition,
    //     desiredCount: 1,
    //   }
    // );
    // ECS Fargate Service with ALB

    const domainName = "singlesignomni.com";

    const hostedZone = route53.HostedZone.fromHostedZoneAttributes(
      this,
      "HostedZone",
      {
        hostedZoneId: "Z03967301PZR2GOEIVEB7",
        zoneName: "singlesignomni.com",
      }
    );
    // const hostedZone = route53.HostedZone.fromLookup(this, "HostedZone", {
    //   domainName,
    // });

    const certificate = certificatemanager.Certificate.fromCertificateArn(
      this,
      "Certificate",
      "arn:aws:acm:us-east-1:876683053476:certificate/8011f84f-0e06-4a4e-9a3a-ca5f6b4f5a51"
    );

    const fargateService =
      new ecs_patterns.ApplicationLoadBalancedFargateService(
        this,
        "SpringBootService",
        {
          cluster: apiCluster,
          memoryLimitMiB: 1024,
          cpu: 512,
          desiredCount: 1,
          taskDefinition: apiTaskDefinition,
          publicLoadBalancer: true,
          certificate,
          domainName: "api.singlesignomni.com",
          domainZone: hostedZone,
        }
      );

    const websiteBucket = new s3.Bucket(this, "ReactFrontendBucket", {
      websiteIndexDocument: "index.html",
      websiteErrorDocument: "index.html",
      publicReadAccess: false,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
    });

    // CloudFront distribution
    const distribution = new cloudfront.Distribution(
      this,
      "ReactFrontendDistribution",
      {
        defaultBehavior: {
          origin: new cloudfrontOrigins.S3StaticWebsiteOrigin(websiteBucket),
          viewerProtocolPolicy:
            cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
        },
        domainNames: ["www.singlesignomni.com"],
        certificate,
      }
    );

    // Deploy the React build folder to S3
    new s3deploy.BucketDeployment(this, "DeployReactApp", {
      sources: [s3deploy.Source.asset("./frontend/dist")], // Replace with your React build path
      destinationBucket: websiteBucket,
      distribution,
      distributionPaths: ["/*"],
    });

    // Route53 alias record for frontend (www)
    // new route53.ARecord(this, "FrontendAliasRecord", {
    //   zone: hostedZone,
    //   recordName: "www",
    //   target: route53.RecordTarget.fromAlias(
    //     new route53_targets.CloudFrontTarget(distribution)
    //   ),
    // });

    // // Route53 alias record for backend (api)
    // new route53.ARecord(this, "BackendAliasRecord", {
    //   zone: hostedZone,
    //   recordName: "api",
    //   target: route53.RecordTarget.fromAlias(
    //     new route53_targets.LoadBalancerTarget(fargateService.loadBalancer)
    //   ),
    // });
  }
}

const app = new cdk.App();
new SingleSignOmniStack(app, "SingleSignOmniStack");
app.synth();
