# Money Transfer API
[![CircleCI](https://circleci.com/gh/Long-shot/Money-Transfers.svg?style=svg)](https://circleci.com/gh/Long-shot/Money-Transfers)
[![codecov](https://codecov.io/gh/Long-shot/Money-Transfers/branch/master/graph/badge.svg)](https://codecov.io/gh/Long-shot/Money-Transfers)
<img src="http://online.swagger.io/validator?url=http://online.swagger.io/validator?url=https://github.com/Long-shot/Money-Transfers/blob/master/src/main/resources/contracts.yaml">
### Problem Statement
Design and implement a RESTful API (including data model and the backing implementation)
for money transfers between accounts.

### Development notes

First step was developing [contracts](./src/main/resources/contracts.yaml) (OpenAPI contracts can be viewed [here](https://editor.swagger.io) ). 

I tried using KTor generator from swagger, but produced result was mediocre, so I left contract for documentation purposes only.  

Some factors such as server configs, logging,  implementing client, better test/deploy automation or comprehensive mocking are left out of scope.

Due to Jetty running in Debug-level modes, one can see errors there. It's normal behaviour(apparently see https://github.com/eclipse/jetty.project/issues/3529)

Unfortunately, Jacoco fails to correctly check some functions, but this close enough. 
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

Note: for running tests, there is some problem with JDK11/UUID/Jacoco. Java 8 recommended
to run tests:
```
./gradlew test
```