#Test Generation for Lustre

This set of tools is based on the [JKind](https://github.com/agacek/jkind) model checker and supports test generation for the [Lustre](https://en.wikipedia.org/wiki/Lustre_(programming_language)) synchronous programming language, including Lustre simulation, coverage obligation generation, coverage measurement, and static program transformations.

Configuration
-------------

Most features are self-contained, but JKind and Z3 need to be installed for generating tests. Or you can execute JKind on the generated Lustre program with obligations directly and process counterexamples on your own.

1. Put jkind.jar under system PATH or create an environment variable JKIND_HOME that points to jkind.jar.
2. Install [Z3](https://github.com/Z3Prover/z3) under system PATH or create an environment variable Z3_HOME that points to Z3.

Test suite and trace files
--------------------------

Both test suite and trace files are in CSV format. The first line represents variable names and the remaining lines represent values at each step. Each column represents the name and values for one variable. During processing, all spaces are removed.

varname1,varname2,...

value11,value21,... (values at step 1)

...

value1n, value2n,... (values at step n)

Oracle file
-----------

By default, the Lustre simulator produces values for all local and output variables. An optional oracle file can be specified for the Lustre simulator to produce a subset of all the variables. The oracle file contains a set of variables separated by commas. During processing, all spaces are removed.

varname1,varname2,...

