![Java CI with Maven](https://github.com/dhowe/rita2/workflows/Java%20CI%20with%20Maven/badge.svg)

## RiTa: generative language tools for Java

<a href="http://rednoise.org/rita"><img height="80" src="http://rednoise.org/rita/img/RiTa-logo3.png"/></a>

RiTa is a toolkit for natural language and generative literature. It is implemented in Java and JavaScript, with a common API for both, and it is free/libre/open-source via the GPL license.

###

For JavaScript, see this [repo](https://github.com/dhowe/ritajs) 
or use it with [npm](https://www.npmjs.com/package/rita) 
or [unpkg](https://unpkg.com/rita/) !

### Installation

* Via [github packages](https://github.com/dhowe/rita/packages/)
* Via [maven central](https://search.maven.org/artifact/org.rednoise/rita)
* Or directly in maven:

```xml
<dependency>
  <groupId>org.rednoise</groupId>
  <artifactId>rita</artifactId>
  <version>2.0.2</version>
</dependency>
```


### Building
```sh
$ git clone https://github.com/dhowe/rita.git
$ cd rita
$ mvn install      # when done, you should see "BUILD SUCCESS"
```
The project requires a minimum version of Java 8 and Maven 3.6 to build.

### Eclipse
1. Do steps above under **Building**
2. In eclipse, File->Import->Maven->Existing Maven Projects and select your 'rita' folder
3. Right-click on project, and select 'Run-as' -> 'Maven install' or 'JUnit tests'
