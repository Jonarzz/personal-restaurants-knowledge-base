mvn spring-boot:run \
 -Dspring-boot.run.profiles=local \
 -Dspring-boot.run.arguments="--amazon.aws.dynamodb-url=http://localhost:$(cat .dynamodb-port)"