#Money Transfer API

###Problem Statement
Design and implement a RESTful API (including data model and the backing implementation)
for money transfers between accounts.
Explicit requirements:

1. You can use Java, Scala or Kotlin.
2. Keep it simple and to the point (e.g. no need to implement any authentication).
3. Assume the API is invoked by multiple systems and services on behalf of end users.
4. You can use frameworks/libraries if you like (except Spring), but don't forget about
requirement #2 – keep it simple and avoid heavy frameworks.
5. The datastore should run in-memory for the sake of this test.
6. The final result should be executable as a standalone program (should not require
a pre-installed container/server).
7. Demonstrate with tests that the API works as expected.
Implicit requirements:
1. The code produced by you is expected to be of high quality.
2. There are no detailed requirements, use common sense.


### Development notes

First step was developing [contracts](./src/main/resources/contracts.yaml) (OpenAPI contracts can be viewed [here](https://editor.swagger.io) ). 

I tried using KTor generator from swagger, but produced result was mediocre, so I left contract for documentation purposes only.  

Some factors such as server configs, logging,  implementing client, better test/deploy automation or comprehensive mocking are left out of scope.

Due to Jetty running in Debug-level modes, one can see errors there. It's normal behaviour(apparently see https://github.com/eclipse/jetty.project/issues/3529) 
### How to run 

There are two ways to run project: 

- direct
```
./gradlew run
```

or through building a jar
```
./gradlew jar
java -jar builds/libs/transfer-1.0.jar
```