java -jar ../wutsi-codegen/target/wutsi-codegen-0.0.24.jar server \
    -in https://wutsi-openapi.s3.amazonaws.com/twitter_api.yaml \
    -out . \
    -name twitter \
    -package com.wutsi.twitter \
    -jdk 11 \
    -github_user wutsi \
    -github_project twitter-server \
    -heroku wutsi-twitter \
    -service_database \
    -service_logger \
    -service_mqueue

if [ $? -eq 0 ]
then
    echo Code Cleanup...
    mvn antrun:run@ktlint-format
    mvn antrun:run@ktlint-format

else
    echo "FAILED"
    exit -1
fi
