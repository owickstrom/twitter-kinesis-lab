# Web Visualizer

Shows the most popular hashtags in a nice tag cloud. The data is read from
a PostgreSQL database. The production database is a PostgreSQL running in
Amazon RDS, and may be a local database for development.

## Installation

Make sure [Leiningen 2+](http://leiningen.org/) is installed.

## Usage

A few properties need to be set, containing the database settings and credentials:

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

    $ lein ring server

If you prefer to not have abrowser open, use:

    $ lein ring server-headless

In order to have the necessary properties available during development, place
them in a file called `profiles.clj`, which should look like:

    {:dev {:env {:db-classname "org.postgresql.Driver"
                 :db-subprotocol "postgresql"
                 :db-subname "//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets"
                 :db-user "awsuser"
                 :db-password "somepwd"}}}

NOTE: Make sure you don't accidentally add `profiles.clj` to source control.

The `:dev` profile will be used when running `lein ring server` or `lein repl`.
LightTable or other IDEs that have Leiningen integration can also be used.

If a `:test` environment is available, it will be used when running `lein test`
(should there be any tests that require any of these properties):

    {:dev {:env {:db-classname "org.postgresql.Driver"
                 :db-subprotocol "postgresql"
                 :db-subname "//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets"
                 :db-user "awsuser"
                 :db-password "somepwd"}}
     :test {:env {:db-classname "org.postgresql.Driver"
                  :db-subprotocol "postgresql"
                  :db-subname "//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets"
                  :db-user "awsuser"
                  :db-password "somepwd"}}}

### Production

Build a stand-alone executable jar like this:

    $ lein ring uberwar

In production, the settings need to be available through other means than the
`profiles.clj` file. One way is environment variables:

    $ DB_CLASSNAME=org.postgresql.Driver ... java -jar twitter-hashtags-visualizer-<VERSION>-standalone.war

Another way is Java system properties:

    $ java -Ddb.classname=org.postgresql.Driver ... -jar twitter-hashtags-visualizer-<VERSION>-standalone.war

Of course, the variables can also be placed in a file which is sourced before
running:

    $ cat credentials
    export DB_CLASSNAME=org.postgresql.Driver
    export DB_SUBPROTOCOL=postgresql
    export DB_SUBNAME=//twitter-kinesis-lab.whatever.eu-west-1.rds.amazonaws.com:5432/trendingtweets
    export DB_USER=awsuser
    export DB_PASSWORD=somepwd

    $ . credentials

    $ java -jar twitter-hashtags-visualizer-<VERSION>-standalone.war
