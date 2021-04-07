[![](https://github.com/wutsi/twitter-server/actions/workflows/master.yml/badge.svg)](https://github.com/wutsi/twitter-server/actions/workflows/master.yml)
[![](https://github.com/wutsi/twitter-server/actions/workflows/pull_request.yml/badge.svg)](https://github.com/wutsi/twitter-server/actions/workflows/pull_request.yml)
[![](https://github.com/wutsi/telegram-server/actions/workflows/scheduled_run.yml/badge.svg)](https://github.com/wutsi/telegram-server/actions/workflows/scheduled_run.yml)

[![JDK](https://img.shields.io/badge/jdk-11-brightgreen.svg)](https://jdk.java.net/11/)
[![](https://img.shields.io/badge/maven-3.6-brightgreen.svg)](https://maven.apache.org/download.cgi)
![](https://img.shields.io/badge/language-kotlin-blue.svg)

Wutsi Plugin for sharing stories on [Twitter](https://www.twitter.com).

This plugin listens to the following events:
  - `urn:event:wutsi:story:published`: When this event is received, this plugin share the story to the site's twitter account.

# Installation Prerequisites
## Database Setup
- Install postgres
- Create account with username/password: `postgres`/`postgres`
- Create a database named `twitter`

## Configure Github
- Generate a Github token for accessing packages from GibHub
  - Goto [https://github.com/settings/tokens](https://github.com/settings/tokens)
  - Click on `Generate New Token`
  - Give a value to your token
  - Select the permissions `read:packages`
  - Generate the token
- Set your GitHub environment variables on your machine:
  - `GITHUB_TOKEN = your-token-value`
  - `GITHUB_USER = your-github-user-name`

## Maven Setup
- Download Instance [Maven 3.6+](https://maven.apache.org/download.cgi)
- Add into `~/m2/settings.xml`
```
    <settings>
        ...
        <servers>
            ...
            <server>
              <id>wutsi-channel-sdk-kotlin</id>
              <username>${env.GITUB_USER}</username>
              <password>${env.GITHUB_TOKEN}</password>
            </server>
            <server>
              <id>wutsi-site-sdk-kotlin</id>
              <username>${env.GITUB_USER}</username>
              <password>${env.GITHUB_TOKEN}</password>
            </server>
            <server>
              <id>wutsi-story-sdk-kotlin</id>
              <username>${env.GITUB_USER}</username>
              <password>${env.GITHUB_TOKEN}</password>
            </server>
            <server>
              <id>wutsi-twitter-sdk-kotlin</id>
              <username>${env.GITUB_USER}</username>
              <password>${env.GITHUB_TOKEN}</password>
            </server>
            <server>
              <id>wutsi-stream-rabbitmq</id>
              <username>${env.GITUB_USER}</username>
              <password>${env.GITHUB_TOKEN}</password>
            </server>
        </servers>
    </settings>
```

## Usage
- Install
```
$ git clone git@github.com:wutsi/twitter-server.git
```

- Build
```
$ cd twitter-server
$ mvn clean install
```

- Launch the API
```
$ mvn spring-boot:run
```

That's it... the API is up and running! Start sending requests :-)

# Links
- [API](https://wutsi.github.io/twitter-server/api/)


#  Configuration Attributes
Here are the Site configuration attributes used by this service:

| Name | Description |
|------|-------------|
| urn:attribute:wutsi:twitter:enabled | If `true`, then the plugin is enabled for the site |
| urn:attribute:wutsi:twitter:client-id | Twitter Application client-ID |
| urn:attribute:wutsi:twitter:client-secret | Twitter Application Client-Secret |
| urn:attribute:wutsi:twitter:user-id | ID of the primary user of the Site. This user will tweet or retweet all stories |
