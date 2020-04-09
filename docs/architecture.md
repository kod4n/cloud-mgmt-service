# Overview
The `cloud-mgmt-service` will be in charge of provisioning and monitoring all cloud resources, both on-premise and public cloud deployments. The service uses Terraform to provision all infrastructure needed to bootstrap a Kubernetes cluster with RKE. Terraform state must be persisted in a durable network location.

The MVP will target AWS and expose a cloud-agnostic, asynchronous interface for provisioning infrastructure. Authentication will be set at runtime using a shared token, to be superseded by more robust authentication in the future.

# Components
The following architecture is comprised of multiple components.

## cloud-mgmt-service
The primary service in this architecture. See [Overview](#overview) for roles and responsibilities.

## Terraform
[Terraform](https://www.terraform.io/) will be used to create and delete all cloud resources. It was chosen for its ease of use and ability to interface with multiple providers. Terraform state and Terraform template files must be persisted.

## policy-mgmt-service
The `policy-mgmt-service` will provide compliance validation for all infrastructure provisioned through platform services. It is external to the `cloud-mgmt-service` and will check a request against a set of policy rules. For example, if a user requests additional storage, `cloud-mgmt-service` would use `policy-mgmt-service` to determine if any security policies are in place and use that information to generate a compliant Terraform template.

# Diagrams

## Component
![Component](https://www.plantuml.com/plantuml/img/VP4n2y8m48Nt_8gZanswE3f8fJWAGaK7xN2cfmIRf2PfGQN_tQPLrA9281ovzzrxbzWwDAwI1IkHd_1842I5hd9Oe6ehISf1IgOMd0AuCO3jYep1WpOoWoYQljKxbJfqbC44hDE6hE_c7XR9etIHxJ53cUyNgjQdNZ34poHUjO8DxPy-h5UKHjV22gt-JrdgGl3Bbp2USQx8Y6vgUt9qk4VRvXy6wdCth87NZdvo7qigk_TFHnxG6ONWIwXUE9eduz3VwsKIieR5fFBsS-u0)
<details><summary>Show UML Code</summary>
<p>

```
@startuml
package "Cloud Management Service" {
  [Terraform] --> [Terraform\nState] : stores
  [cloud-mgmt-service] --> [Terraform] : invokes
  [Terraform] --> [Amazon Web Services] : provisions
  [cloud-mgmt-service] --> [Policy Management Service] : queries
  database "Terraform\nState" {
  }
}
package "Policy Management Service" {
  [policy-mgmt-service]
}
cloud "Amazon Web Services" {
  (EC2)
  (VPC)
}
@enduml
```

</p>
</details>

## Physical
![Component](https://www.plantuml.com/plantuml/img/jP9FIyD04CNl_HHZJWgc0K_YeTZ6Wc0L8OA7uc6o6UlI_HDtPrj4-jtTJQtMje8Nvv30PDvxlnbowHaT1wrwHz885Hg25-RMNnI5msL_9labrC6J4zOm6UuBgmjSUeMrJ-zSnWy-VxOkRnMQv5Hez4okQAJdOlXObLPhQ_hj_uNS4I-jcqip6vgcN7jSaP8BSc_5U38QjgsI0bbQCx7OZ32Q279hf9vGDZIGZSkp3Dvx4EjtoJ6g-ZHB7TYqijFiOLB1s62jgCbEEWTZuuxPpRc7UuKSmJMKAYJYJKQgQ0PvQFC1JCiEuXODpaX7VAZ8BQIW9xW0PmEJ_rArAEPn2hBVDDiHgQRTXhcUQSxznZPFgG_a72Qw52FDmiwtSZhfriHF1LICtE5vjexyHl4bN-q3tzjuqxU_4BVTv4_aRsVOjnwFAfXOWGgkhO--aIdsTtqRPApHn_cTFNO7Lpjh73xXu31elCbh82S_RxbsM3zMlKioJVpjFW00)
<details><summary>Show UML Code</summary>
<p>

```
@startuml
!include https://raw.githubusercontent.com/awslabs/aws-icons-for-plantuml/master/dist/AWSCommon.puml
!include https://raw.githubusercontent.com/awslabs/aws-icons-for-plantuml/master/dist/NetworkingAndContentDelivery/ELBApplicationLoadBalancer.puml
cloud "EC2" {
    ELBApplicationLoadBalancer(alb,"Load Balancer","TLS Enabled")
    node "K8s Platform Cluster" {
        alb -up-> [Cloud Management Service] : routes
        package "Cloud Management Service" {
            [cloud-mgmt-service] --> [Terraform] : invokes
            [Terraform] --> [Terraform State] : stores        
        }
        package "Policy Management Service" {
            [policy-mgmt-service]
        }
        package "EBS Local Host Storage" {
            database "Terraform State" {
            }
        }
    [cloud-mgmt-service] -> [policy-mgmt-service] : queries
    }
}
@enduml
```

</p>
</details>

## Security
![Component](https://www.plantuml.com/plantuml/img/ZP5F2u8m68VlVeeNJmdkqYaYmf1E4GX17KfYpMNH_eXxJLByxaqbAeZglOnjptuyFnOOf3aMKZXA9npSnSX092WUTIuX58L1dhjGEs0de-n2Kmk5GXS9BAcoEyaLmehdrpFZ3x2TBMiX8bE9nFJNErMZld2rfmMUgYO6akXIaIbiPv9MMi667nvH9eOnr5dPiSm_8MQciR5TVlEk4sbnPhTVFbb7VHLw2Vy7XmbfvVyeQgiy3vWFVHsdSRgtu7xetxiFvk7GQzTr57gM3xXAx6VT0G00)
<details><summary>Show UML Code</summary>
<p>

```
@startuml
node "K8s Platform Cluster" {
    package "Cloud Management Service" {
        [cloud-mgmt-service\n{token_authz}] -down-> [Amazon Web Services] : {pw_authc,https}   
    }
    package "Policy Management Service" {
        [policy-mgmt-service\n{token_authz}]
    }
    [policy-mgmt-service\n{token_authz}] <--> [cloud-mgmt-service\n{token_authz}] : {token_authc, https}
}
cloud "Amazon Web Services" {
  (EC2)
  (VPC)
}
@enduml
```

</p>
</details>
