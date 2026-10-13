# OSDU Storage Service for Azure

[![Release](https://img.shields.io/github/v/release/Azure/osdu-spi-storage)](https://github.com/Azure/osdu-spi-storage/releases)
[![Validate](https://github.com/Azure/osdu-spi-storage/actions/workflows/validate.yml/badge.svg?branch=main)](https://github.com/Azure/osdu-spi-storage/actions/workflows/validate.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> [!NOTE]
> Shared service code comes from the [OSDU community upstream](https://community.opengroup.org/osdu/platform/system/storage); the [OSDU documentation](https://osdu.pages.opengroup.org/platform/system/storage/) covers the API.

Storage creates, versions, fetches, and deletes OSDU records, and publishes an event for every change so services such as the indexer can follow along.

## At a glance

| | |
|---|---|
| API base path | `/api/storage/v2/` |
| Swagger UI | `/api/storage/v2/swagger` |
| Health | `:8081/actuator/health` |
| Depends on | Partition, Entitlements, Legal, CRS Conversion |
| Azure resources | Cosmos DB (record metadata, schemas, replay status), Storage (record data), Service Bus (`recordstopic`, `recordstopic-v2`, `legaltags`, `replaytopic`), Redis (caches), Key Vault |
| Deployed by | [OSDU SPI Stack](https://github.com/Azure/osdu-spi-stack) (`software/stacks/osdu/services/storage.yaml`) |

## Repository layout

[CONTRIBUTING.md](CONTRIBUTING.md) explains where each kind of change belongs.

| Path | Owner | Contents |
|---|---|---|
| `storage-core/` | OSDU upstream | Shared service code |
| `provider/storage-azure/` | This repository | Azure provider |
| `storage-acceptance-test/` | OSDU upstream | End-to-end suite run against a deployed environment |
| `testing/storage-test-core/` | OSDU upstream | Shared code for the integration tests |
| `testing/storage-test-azure/` | This repository | Legacy Azure integration tests |
| `.spi/service.yaml` | This repository | How CI deploys and tests the service on SPI Stack |

## Build

Requires Java 25 and Maven 3.6.3+. OSDU dependencies resolve from the public community registry through the settings file in `.mvn`:

```bash
mvn --settings .mvn/community-maven.settings.xml -P core,azure clean install
```

The runnable jar lands at `provider/storage-azure/target/storage-azure-*-spring-boot.jar`.

## Configuration

SPI Stack sets the service's environment from two places: the shared `osdu-config` ConfigMap and the service's own entry in [`services/storage.yaml`](https://github.com/Azure/osdu-spi-stack/blob/main/software/stacks/osdu/services/storage.yaml). Those files are the contract; the tables below list what Storage actually reads from them.

**Shared, from `osdu-config`:**

| Variable | Purpose |
|---|---|
| `AAD_CLIENT_ID` | Application ID that caller tokens are issued for |
| `KEYVAULT_URI` | Central Key Vault |
| `APPINSIGHTS_KEY` | Telemetry |
| `SERVER_PORT` | HTTP port (`8080`) |
| `REDIS_HOSTNAME` | Redis host |

**Specific to Storage**, from `services/storage.yaml`:

| Variable and value on SPI Stack | Purpose |
|---|---|
| `SPRING_APPLICATION_NAME`<br>`storage` | Application name |
| `SERVER_SERVLET_CONTEXTPATH`<br>`/api/storage/v2/` | API base path |
| `AZURE_ISTIOAUTH_ENABLED`<br>`true` | Trust the mesh's token validation |
| `AZURE_PAAS_WORKLOADIDENTITY_ISENABLED`<br>`true` | Authenticate to Azure with workload identity |
| `COSMOSDB_DATABASE`<br>`osdu-db` | Database inside each partition's Cosmos DB account |
| `AZURE_SERVICEBUS_ENABLED`<br>`true` | Publish record change events to Service Bus |
| `AZURE_EVENTGRID_ENABLED`<br>`false` | Event Grid publishing is off on SPI Stack |
| `SERVICEBUS_TOPIC_NAME`<br>`recordstopic` | Record change events |
| `SERVICEBUS_V2_TOPIC_NAME`<br>`recordstopic-v2` | Record change events in the V2 shape, with collaboration context |
| `REDIS_DATABASE`<br>`4` | Redis database for the service's caches |
| `PARTITION_SERVICE_ENDPOINT`<br>`http://partition/api/partition/v1` | Per-partition resource lookup |
| `ENTITLEMENTS_SERVICE_ENDPOINT`<br>`http://entitlements/api/entitlements/v2` | Caller authorization |
| `ENTITLEMENTS_SERVICE_API_KEY`<br>`OBSOLETE` | Legacy key passed to the Entitlements client; the property has no default, so the placeholder must stay set |
| `LEGAL_SERVICE_ENDPOINT`<br>`http://legal/api/legal/v1` | Legal tag validation |
| `AZURE_FEATUREFLAG_LEGALTAGCOMPLIANCEUPDATEENABLED`<br>`true` | Act on legal tag status changes, see [Service notes](#service-notes) |
| `LEGAL_SERVICEBUS_TOPIC_NAME`<br>`legaltags` | Topic Legal publishes tag changes to |
| `LEGAL_SERVICEBUS_TOPIC_SUBSCRIPTION`<br>`legaltagssubscription` | Storage's subscription on that topic |
| `CRS_CONVERSION_SERVICE_ENDPOINT`<br>`http://crs-conversion/api/crs/converter/v2` | Coordinate conversion when records are fetched with a frame of reference |
| `POLICY_SERVICE_ENDPOINT`<br>`http://policy/api/policy/v1` | Policy service client; the property has no default |
| `OPA_ENABLED`<br>`false` | Policy (OPA) authorization is off |

The service authenticates to Azure with workload identity, which injects `AZURE_CLIENT_ID` and a federated token; there are no client secrets. Per-partition resources are resolved at request time through the Partition service. Redis is reached over TLS on port `6380`; the stack sets no Redis host, so the host and password come from the Key Vault secrets `redis-queue-hostname` and `redis-queue-password`.

## Test

| Suite | Where | Runs in CI | Run it yourself |
|---|---|---|---|
| Unit | `storage-core`, `provider/storage-azure` | Pull requests (Java Build) | `mvn ... install` from [Build](#build) |
| Acceptance | [`storage-acceptance-test`](storage-acceptance-test/README.md) | Pull requests, against SPI Stack (Deploy and Test) | `spi test storage` |
| Integration | `testing/storage-test-core`, `testing/storage-test-azure` | No | See below |

CI runs these on pull requests from this repository that change code. Documentation-only changes skip the build, and pull requests from forks build without deploying.

**Acceptance** proves a change on real infrastructure before it merges. It calls the deployed service through the gateway as a privileged test identity, and the bindings in `.spi/service.yaml` supply its inputs. Against an environment you are connected to:

```bash
spi test storage                   # the image and suite the environment is running
spi test storage --source .        # this checkout's suite and descriptor
```

**Integration** is the older Azure suite. It sits outside the root Maven build and CI does not run it. It accepts bearer tokens for the privileged and no-access identities in `INTEGRATION_TESTER_ACCESS_TOKEN` and `NO_DATA_ACCESS_TESTER_ACCESS_TOKEN`, so `spi token` and `spi token --no-access` can supply them, but it has not been proven against SPI Stack.

To call the API by hand, `spi token` mints a bearer token:

```bash
curl -H "Authorization: Bearer $(spi token)" -H "data-partition-id: <partition>" \
  https://<gateway>/api/storage/v2/records/<record-id>
```

## Deploy

For a pull request from this repository that changes code, CI publishes the service image and its test suite image, `osdu-spi-storage-acceptance`, to GHCR, and the Deploy and Test lane borrows an SPI Stack environment, runs the new image there, proves it with the acceptance suite, and restores the environment's own image. When that lane runs and passes, the change is proven on real infrastructure before it merges; the Validation Summary on the pull request shows whether it ran. This repository does not own infrastructure; SPI Stack does.

To try a build by hand on an environment you are connected to, pin it by digest and release the pin when done:

```bash
spi service pin storage --image ghcr.io/azure/osdu-spi-storage@sha256:<digest>
spi service reset storage
```

## Service notes

**Legal tag compliance runs outside a request.** When Legal marks a tag incompliant, Storage marks the records that carry it deleted. The handler is off unless `AZURE_FEATUREFLAG_LEGALTAGCOMPLIANCEUPDATEENABLED` is `true`. SPI Stack turns it on and subscribes it to `legaltags`, the Service Bus topic Legal publishes to; the Event Grid fed topics get no messages while Event Grid is off. Each message arrives on a Service Bus receiver thread, not an HTTP request, so the handler builds the partition, correlation ID, and user headers from the message body in a thread scope. Code on this path cannot use request-scoped beans such as `JaxRsDpsLog`: one that does throws `ScopeNotActiveException`, the message is abandoned, and after repeated deliveries it is dead-lettered.

## License

Copyright © Microsoft Corporation

Licensed under the [Apache License 2.0](LICENSE).
