grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.7
grails.project.source.level = 1.7
grails.project.dependency.resolver = "maven" // or ivy

grails.project.dependency.resolution = {
    inherits("global") { }
    log "warn" // 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    repositories {
        inherits true

        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenLocal()
        mavenCentral()
        mavenRepo "https://oss.sonatype.org/content/repositories/snapshots/"
    }

    dependencies {
        runtime 'com.google.guava:guava:14.0.1'
        runtime "org.json:json:20090211"
        runtime "com.google.code.gson:gson:2.2.4"
        runtime "joda-time:joda-time:2.2"
        runtime 'org.apache.commons:commons-io:1.3.2'
        runtime "org.mongodb:mongo-java-driver:2.11.3"
        runtime 'net.sf.saxon:Saxon-HE:9.4'
        runtime('org.codehaus.groovy.modules.http-builder:http-builder:0.5.2') {
            excludes 'groovy'
            excludes 'xml-apis'
            excludes 'xalan'
        }
	runtime 'org.postgresql:postgresql:9.3-1100-jdbc41' 

        //runtime 'xpp3:xpp3:1.1.3.4.O'
        //runtime 'xmlpull:xmlpull:1.1.3.4d_b4_min'
        compile 'me.fhir:fhir-0.12:r2308-SNAPSHOT'
    }

    plugins {
        compile ":rest-client-builder:1.0.2"
    	if (System.getProperty("noTomcat") == null) {
            build ':tomcat:7.0.50'
        }
        runtime ':hibernate:3.6.10.7'
        runtime ":resources:1.2.1"
        runtime ":cors:1.1.2"
        compile (":postgresql-extensions:0.6.4") {excludes 'hibernate'}
    }
}
