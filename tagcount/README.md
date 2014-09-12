# tagcount

Provides a worker that receives tweet tag records from Amazon Kinesis. The
number of occurrences of each tag in a sliding window is counted and stored in
a PostgreSQL database. The production database is a PostgreSQL running in
Amazon RDS, and may be a local database for development.

## Installation

Make sure [Leiningen 2+](http://leiningen.org/) is installed.

## Usage

A few properties need to be set, containing an AWS access key with permissions
for Amazon Kinesis and the database settings and credentials:

* AWS access key, eg `AKIAJQSAKJ4HJEXAMPLE`
* AWS secret key, eg `somesecretawskey`
* AWS region, eg `eu-west-1`
* JDBC driver classname, eg `org.postgresql.Driver`
* JDBC subprotocol, eg `postgresql`
* JDBC subname, eg `//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets`
* Database username, eg `awsuser`
* Database password, eg `somepwd`

We're using a utility called [environ][1] to be able to handle properties in the
same way, code-wise, whether we are in development, test, or production.

[1]: https://github.com/weavejester/environ

### Development

During development, the application is run from Leiningen:

    $ lein run

In order to have the necessary properties available during development, place
them in a file called `profiles.clj`, which should look like:

    {:dev {:env {:aws-access-key-id "AKIAJQSAKJ4HJEXAMPLE"
                 :aws-secret-key "somesecretawskey"
                 :aws-region "eu-west-1"
                 :db-classname "org.postgresql.Driver"
                 :db-subprotocol "postgresql"
                 :db-subname "//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets"
                 :db-user "awsuser"
                 :db-password "somepwd"}}}

NOTE: Make sure you don't accidentally add `profiles.clj` to source control.

The `:dev` profile will be used when running `lein run` or `lein repl`.
LightTable or other IDEs that have Leiningen integration can also be used.

If a `:test` environment is available, it will be used when running `lein test`
(should there be any tests that require any of these properties):

    {:dev {:env {:aws-access-key-id "AKIAJQSAKJ4HJEXAMPLE"
                 :aws-secret-key "somesecretawskey"
                 :aws-region "eu-west-1"
                 :db-classname "org.postgresql.Driver"
                 :db-subprotocol "postgresql"
                 :db-subname "//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets"
                 :db-user "awsuser"
                 :db-password "somepwd"}}
     :test {:env {:aws-access-key-id "AKIAJQSAKJ4HJEXAMPLE"
                  :aws-secret-key "somesecretawskey"
                  :aws-region "eu-west-1"
                  :db-classname "org.postgresql.Driver"
                  :db-subprotocol "postgresql"
                  :db-subname "//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets"
                  :db-user "awsuser"
                  :db-password "somepwd"}}}

### Production

Build a stand-alone executable jar like this:

    $ lein uberjar

In production, the settings need to be available through other means than the
`profiles.clj` file. One way is environment variables:

    $ AWS_ACCESS_KEY_ID=AKIAJQSAKJ4HJEXAMPLE ... java -jar tagcount-<VERSION>-standalone.jar

Another way is Java system properties:

    $ java -Daws.access.key.id=AKIAJQSAKJ4HJEXAMPLE ... -jar tagcount-<VERSION>-standalone.jar

Of course, the variables can also be placed in a file which is sourced before
running:

    $ cat credentials
    export AWS_ACCESS_KEY_ID=AKIAJQSAKJ4HJEXAMPLE
    export AWS_SECRET_KEY=somesecretawskey
    export AWS_REGION=eu-west-1
    export DB_CLASSNAME=org.postgresql.Driver
    export DB_SUBPROTOCOL=postgresql
    export DB_SUBNAME=//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets
    export DB_USER=awsuser
    export DB_PASSWORD=somepwd

    $ . credentials

    $ java -jar tagcount-<VERSION>-standalone.jar
