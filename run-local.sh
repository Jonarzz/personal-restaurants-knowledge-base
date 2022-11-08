mvn groovy:execute &
until [ -f .dynamodb-port ]; do sleep 1; done
mvn spring-boot:run -Dspring-boot.run.arguments="--amazon.aws.dynamodb-url=http://localhost:$(cat .dynamodb-port)"