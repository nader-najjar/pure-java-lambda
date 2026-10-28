# pure-java-lambda

A small Java service template built with Bazel. Bazel owns Java compilation, dependency resolution, tests, static analysis, coverage, and Lambda ZIP packaging.

## Prerequisites

Install [Bazelisk](https://bazel.build/install/bazelisk). It reads `.bazelversion` and selects the pinned Bazel release. Bazel downloads the pinned Java 25 toolchains; a separate system JDK is not needed for project compilation or tests.

## Standard commands

```sh
bazel build //...
bazel test //...
bazel coverage //:all_tests --combined_report=lcov
bazel build //:lambda_deployment_package
```

`bazel test //...` runs the JUnit suite, Checkstyle, and SpotBugs. Aspect ruleset telemetry is disabled in `.bazelrc`. `bazel coverage` writes the combined LCOV report under Bazel's output tree.

For local lint output, use:

```sh
bazel build --config=lint //:app //:composition //:lambda_entrypoint //:all_tests
```

## IDE setup

Use your usual [IntelliJ IDEA setup guide](https://nader-najjar.notion.site/JetBrains-IDE-Setup-Usage-Guide-d9b0a2b78755822f9d03819f5f02feb2) or [VS Code setup guide](https://nader-najjar.notion.site/Visual-Studio-Code-Setup-Usage-Guide-f4e0a2b7875583be9293817d26459034). The multi-repository workspace and editor readability instructions still apply.

### Get the JDK path selected by Bazel

Run this from the repository root:

```sh
bazel run //tools/ide:print_java_home
```

It prints the JDK home reported by the Java runtime selected for this Bazel target. The configured compiler toolchain and target runtime both use `remotejdk_25`, so this is also the JDK behind this project's Java 25 compilation. Use the printed path as the IntelliJ Project SDK and as VS Code's Java home. Re-run the command after changing the Java toolchain or clearing Bazel's downloaded repositories.

`bazel info java-home` reports the JDK running Bazel itself, which can differ from the project's selected JDK. Bazel's `JavaRuntimeInfo.java_home` identifies the selected runtime inside Bazel's execution root, but on macOS that repository root is a wrapper around the IDE-ready `Contents/Home` directory. Running this tiny target lets the selected JDK report its actual home directly, without depending on Bazel cache layout or platform-specific path rewriting.

### IntelliJ IDEA Ultimate

In the guide's **Project Settings → Project** step, choose **SDK → Add JDK from disk** and use the path printed above. Set **Language Level** to **SDK Default**. Keep the guide's Multi-Project Workspace and per-repository terminal setup. The Gradle JVM step does not apply because this repository builds with Bazel.

### Visual Studio Code

Keep the guide's saved multi-root `.code-workspace`. In the Java folder settings, use the printed path for `java.jdt.ls.java.home` and for the `path` entry in `java.configuration.runtimes` (with `name` set to `JavaSE-25` and `default` set to `true`). This points both the Java language server and project Java runtime at Bazel's JDK. The Maven/Gradle-specific Java-home settings do not apply here.

For example, replace the placeholder with the command's output:

```json
{
  "java.jdt.ls.java.home": "/path/printed/by/command",
  "java.configuration.runtimes": [
    { "name": "JavaSE-25", "path": "/path/printed/by/command", "default": true }
  ],
  "java.test.config": {
    "javaExec": "/path/printed/by/command/bin/java"
  }
}
```

`java.test.config.javaExec` expects the Java executable, so its path ends in `bin/java` on macOS/Linux (or `\bin\java.exe` on Windows). The other two settings expect the JDK home directory.

## Application configuration

The template requires `STAGE`, `AWS_REGION`, `CALCULATION_RESULTS_TABLE_NAME`, `EXAMPLE_STRING_VAR`, `EXAMPLE_INT_VAR`, and `EXAMPLE_BOOLEAN_VAR`. `STAGE` must be exactly `ALPHA`, `BETA`, `GAMMA`, or `PRODUCTION`. `AWS_REGION` must exactly match a region ID known to AWS SDK 2.55.11, such as `us-east-1`. Boolean environment values must be lowercase `true` or `false`. Missing, blank, and invalid values fail validation.

JSON input ignores unknown properties and rejects duplicate keys. All five declared input properties must be present; string, timestamp, and list values must be non-null, and primitive values reject nulls.

AWS SDK versions are managed by the BOM in `MODULE.bazel`; the application uses Regions and the DynamoDB client. The DynamoDB table must exist with a string partition key named `id`, and the deployment needs permission to write items. AWS credentials use the SDK default provider chain. The calculator and persistence are dummy examples; repeated identical calculations overwrite the same item.

## Project layout

- `src/main/java` — sample application and business logic.
- `src/main/resources` — runtime resources.
- `src/test/java` — JUnit 5 tests and test support.
- `tools/lint` — hermetic Checkstyle and SpotBugs launchers and lint aspects.
- `MODULE.bazel` — pinned Bazel modules and Maven coordinates.
- `maven_install.json` — resolved Maven dependency versions, URLs, and SHA-256 checksums.

## Application boundaries

`LambdaHandler` stays at the package root. `composition` holds Guice modules, `environment` groups environment parsing, errors, and models, and `execution` holds the per-input workflow. `shared` retains stateless utilities.

| Target | Responsibility |
| --- | --- |
| `//:app` | Execution, business logic, environment configuration, and shared utilities |
| `//:composition` | Strict Guice wiring and environment/AWS client providers |
| `//:lambda_entrypoint` | AWS Lambda stream handler |
| `//:logging` | Shared JSON logging configuration and runtime |
| `//:lambda_bin` | Handler deploy JAR with runtime dependencies |

Checkstyle and SpotBugs cover every production Java source and the tests. Coverage measures `//:app`, excluding the entrypoint and injection wiring to match the legacy scope. The report measures line coverage and currently has no enforced minimum. The composition test constructs strict wiring with substituted environment and AWS clients; entrypoint integration tests are deferred.

## Dependency updates

1. Change Maven coordinates or BOM versions in `MODULE.bazel`. Use explicit artifact versions unless a declared BOM supplies them.
2. Run `REPIN=1 bazel run @maven//:pin` to update `maven_install.json`.
3. Review both files together and build the affected targets.

Update Bazel in `.bazelversion`. Update the Java toolchain flags in `.bazelrc` when changing the Java major version.

## Outputs

### AWS Lambda (`LambdaHandler`)

* Handler: `io.template.LambdaHandler::handleRequest`, on the `java25` runtime.
* The event JSON reaches `Executor` as its single input argument. The injector is reused across invocations, and logs include the AWS request ID.
* Build `//:lambda_deployment_package` to produce `bazel-bin/lambda-deployment-package.zip`. The ZIP contains `lib/lambda_bin_deploy.jar`, a Bazel deploy JAR bundling the handler, logging configuration, and runtime dependencies. ZIP timestamps are fixed and build stamping is disabled.
* Deploy that ZIP with your infrastructure tool. Lambda clients live for the execution environment's lifetime; there is no process shutdown hook.

## License

All rights reserved. See [LICENSE](LICENSE).
