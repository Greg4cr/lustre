#Test Generation for Lustre

This set of tools is based on the [JKind](https://github.com/agacek/jkind) model checker and supports test generation for the [Lustre](https://en.wikipedia.org/wiki/Lustre_(programming_language)) synchronous programming language, including Lustre simulation, coverage obligation generation, coverage measurement, and static program transformations.

Configuration
-------------

1. Java 8.
2. Put jkind.jar under system PATH or create an environment variable JKIND_HOME that points to jkind.jar.
3. Install [Z3](https://github.com/Z3Prover/z3) under system PATH or create an environment variable Z3_HOME that points to Z3. Only necessary for generating tests.
