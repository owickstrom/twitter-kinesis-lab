# Web Visualizer

Shows the most popular hashtags in a nice way.

## Installation

Make sure [Leiningen 2+](http://leiningen.org/) is installed.

Also, a few properties need to be set, containing credentials for the PostgreSQL
RDS database. One way of doing this is to create a file called `credentials`, 
which is "sourced" before the application is started:

	export DB_CLASSNAME=org.postgresql.Driver
	export DB_SUBPROTOCOL=postgresql
	export DB_SUBNAME=//twitter.whatever.us-east-1.rds.amazonaws.com:5432/trendingtweets
	export DB_USER=awsuser
	export DB_PASSWORD=somepwd

## Usage

First, source the credentials file:

	. credentials

To start a web server for the application, run:

    lein ring server
