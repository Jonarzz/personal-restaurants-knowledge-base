dynamodbPortFile=.dynamodb-port
rm $dynamodbPortFile
mvn groovy:execute &
until [ -f $dynamodbPortFile ]; do sleep 1; done
mvn spring-boot:run -Dspring-boot.run.arguments="--amazon.aws.dynamodb-url=http://localhost:$(cat $dynamodbPortFile)"