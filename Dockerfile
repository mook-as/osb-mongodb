FROM opensuse/leap AS builder
RUN zypper --non-interactive addrepo obs://devel:tools:building devel:tools:building
RUN zypper --non-interactive --gpg-auto-import-keys install \
    maven \
    git \
    'java-sdk=1.8.0' \
    which \
    ${NULL}

COPY pom.xml          /opt/osb-mongodb/
COPY osb-backup-core/ /opt/osb-mongodb/osb-backup-core/
COPY osb-core/        /opt/osb-mongodb/osb-core/
COPY osb-dashboard/   /opt/osb-mongodb/osb-dashboard/
COPY osb-deployment/  /opt/osb-mongodb/osb-deployment/
COPY osb-persistence/ /opt/osb-mongodb/osb-persistence/
COPY osb-service/     /opt/osb-mongodb/osb-service/
WORKDIR /opt/osb-mongodb
RUN mvn install
RUN rm -f /root/.m2/repository/de/evoila/cf/broker/cf-service-broker-mongodb/*/cf-service-broker-mongodb-*-sources.jar
RUN rm -f /root/.m2/repository/de/evoila/cf/broker/cf-service-broker-mongodb/*/cf-service-broker-mongodb-*-tests.jar
RUN chmod a+x /root/.m2/repository/de/evoila/cf/broker/cf-service-broker-mongodb/*/cf-service-broker-mongodb-*.jar

FROM opensuse/leap
RUN zypper --non-interactive --gpg-auto-import-keys install \
    file \
    'jre-headless = 1.8.0' \
    ${NULL}
WORKDIR /opt/
COPY --from=builder /root/.m2/repository/de/evoila/cf/broker/cf-service-broker-mongodb/*/cf-service-broker-mongodb-*.jar /opt/cf-service-broker-mongodb.jar
ADD application.yml /opt/
ENTRYPOINT [ "/opt/cf-service-broker-mongodb.jar" ]
