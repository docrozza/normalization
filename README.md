# normalization

[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

A Kotlin library for normalising and producing a digest of RDF graphs in Stardog.

## Table of Contents

- [Background](#background)
- [Package](#package)
- [Usage](#usage)
- [Contributing](#contributing)
- [Future Work](#future-work)
- [Maintainers](#maintainers)
- [License](#license)

## Background

RDF is a graph-based model for describing resources via their properties, i.e. the (subject - predicate - value) or triple
that makes up the vertices/edges in the graph. Often it is useful to compare the differences between sets of graphs or
generate short identifiers for graphs via hashing algorithms, ex. to digitally sign a graph. This library provides an
implementation of an algorithm for normalizing RDF datasets such that these operations can be performed. From the
[spec](https://json-ld.github.io/rdf-dataset-canonicalization/spec/) of this algorithm:

> When data scientists discuss canonicalization, they do so in the context of achieving a particular set of goals. Since
> the same information may sometimes be expressed in a variety of different ways, it often becomes necessary to be able
> to transform each of these different ways into a single, standard format. With a standard format, the differences between
> two different sets of data can be easily determined, a cryptographically-strong hash identifier can be generated for a
> particular set of data, and a particular set of data may be digitally-signed for later verification.
> 
> In particular, this specification is about normalizing RDF datasets, which are collections of graphs. Since a directed
> graph can express the same information in more than one way, it requires canonicalization to achieve the aforementioned
> goals and any others that may arise via serendipity.

The algorithm implemented in this library is the _URDNA2015_ detailed in the above specification. 

## Package

```gradle
dependencies {
    compile("io.docrozza:normalization:21.11")
}
```

The project uses [Calendar Versioning](https://calver.org) for version numbers.

## Usage

The JAR file needs to be added to the directory _STARDOG_HOME/server/ext_ or wherever your _STARDOG_EXT_ environment variable
os pointing to. Once installed, the __graphDigest__ function becomes available and can be used as follows:

```sparql
PREFIX gd: <urn:docrozza:stardog:normalization:>
SELECT ?context ?hash FROM NAMED <tag:stardog:api:context:all> WHERE {
    GRAPH ?context {}
    (?context) gd:graphDigest (?hash)
}
```

## Future Work

 * Add extra subject parameter (Boolean) to control whether the context should be included in the normalization routine
 * Add extra subject parameter (String) to select a different digest algorithm from the current SHA-256
 * Have some guards to prevent the loading of huge graphs - this would require high RAM to process using the current algorithm as written
 * Look into some simple shortcuts in normalization, ex. if no BNodes, then the statements can be simply sorted

Someone with in-depth knowledge of Stardog internals might be able to use off-heap processing for the BNode tracking
allowing larger graphs to be normalized but this isn't part of their public API at the moment.

## Contributing

Help appreciated :-) [Open an issue](https://github.com/docrozza/normalization/issues/new) or submit PRs.

NB to use the library or run the tests, a working Stardog installation is required. See
[here](https://docs.stardog.com/get-started/install-stardog/) for more information. To then run the test, the following
gradle project properties need to be set to create the embedded database:
 * stardogHome - the path to the directory to store the database and where the license file is stored
 * stardogLibs - the path to the database library JARs

## Maintainers

[@DocRozza](https://github.com/docrozza)

## License

[MIT](LICENSE) © Rory Steele
