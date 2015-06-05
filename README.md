# integrate-security-examples

This repository contains heavily commented sample code for use in clients connecting to Integrate APIs through an
Integrate Security server. This code is intended for use in concert with the API docs. and associated security
documentation (neither of which are provided here). Samples are provided using Spring OAuth and Apache HttpComponents.

All samples were built (and tested) on a machine where:

* The client = the code provided in this repository.
* The auth. server = `http://localhost:8080/openid-connect` - a local implementation of Integrate Security. Note that
this server doesn't not use HTTPS - in production all communications with this server *must* be protected by HTTPS.
* The resource server = `http://localhost:8080/aimple-web-app` - a local test server exposing an API stub. As above,
this doesn't use HTTPS - in production it *must*.

Real life values for these URLs will be provided by Integrate if you are a registered client developer accessing one of
our APIs.

Note to the public: this code may also be useful to people trying to connect to a standards compliant OAuth 2.0
server via the client credentials flow.
