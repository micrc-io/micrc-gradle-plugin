# Micrc Plugin for Gradle

## Summary

According to the service metadata (under the schema branch of the current repo, in the module directory of the schema directory), automatically configure the project and generate [CI configuration files](https://www.jenkins.io/doc/book/pipeline/jenkinsfile/), [skaffold configuration files](https://skaffold.dev/docs/references/yaml/), [k8s deployment files](https://kubernetes.io/docs/home/), java interfaces and annotations, with the support of the runtime library(micrc-core), the service can run with one click.

## Getting Start

Configure the plugin in your `build.gradle` file.

```groovy
plugins {
    id 'io.micrc.core.gradle.plugin' version 'v0.0.1'
}
```

Then, place an `intro.json` file in the module directory of the schema directory (schema branch)

```json
{
  "ownerDomain": "demo",
  "contextName": "example",
  "basePackages": "com.org.prod.demo.example",
  "version": "0.0.1",
  "global": {
    "integration": {
      "proxyServerUrl": "http://x.x.x.x:xxxx",
      "proxyRepoUrl": "https://repo.integration.it.org.com/repository/maven-hub/",
      "registry": "registry.integration.it.prod.com",
      "gitopsRepo": "github.com/prod/_gitops"
    },
    "production": {
      "proxyServerUrl": "http://x.x.x.x:xxxx",
      "proxyRepoUrl": "https://repo.production.it.org.com/repository/maven-hub/",
      "registry": "registry.production.it.prod.com",
      "gitopsRepo": "github.com/prod/_gitops"
    }
  },
  "server": {
    "namespace": "prod-demo",
    "middlewares": {
      "database": {
        "enabled": "true",
        "provider": "mysql",
        "profiles": {
          "dev": {
            "host": "sealed with sealedsecret",
            "port": "sealed with sealedsecret",
            "dbname": "sealed with sealedsecret",
            "username": "sealed with sealedsecret",
            "password": "sealed with sealedsecret"
          },
          "ver": {"..."},
          "acc": {"..."}
        }
      },
      "cache": {
        "enabled": "true",
        "provider": "redis",
        "profiles": {
          "dev": {
            "host": "...",
            "port": "...",
            "password": "..."
          },
          "ver": {"..."},
          "acc": {"..."}
        }
      },
      "broker": {
        "enabled": "true",
        "provider": "kafka",
        "profiles": {
          "dev": {
            "host": "...",
            "port": "...",
            "password": "...",
            "topics": "..."
          },
          "ver": {"..."},
          "acc": {"..."}
        }
      },
      "memdb": {
        "enabled": "true",
        "provider": "redis",
        "profiles": {
          "dev": {
            "host": "...",
            "port": "...",
            "password": "..."
          },
          "ver": {"..."},
          "acc": {"..."}
        }
      }
    }
  }
}
```

Reload the project, the plug-in will configure the project according to this file, and generate integration.jenkinsfile, production.jenkinsfile, skaffold.xml in the project root directory, and generate helm and kustomize two directories under build/micrc/manifests, There are k8s deployment files in it. Now, use the vscode and cloud code plugins to run this service in docker-desktop or minikube.

you can also start the `generateManifest` task to produce these files.

```shell
$ ./gradlew generateManifest

BUILD SUCCESSFUL in 4s
```

## Usage

Regarding the generation of java interfaces and annotations and the processing of resources, it will be supplemented later...
