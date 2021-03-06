package fhir
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.hl7.fhir.instance.formats.XmlParser
import org.hl7.fhir.instance.model.Conformance
import org.hl7.fhir.instance.model.DateAndTime
import org.hl7.fhir.instance.model.Profile
import org.hl7.fhir.instance.model.Resource
import org.hl7.fhir.instance.model.Conformance.ConformanceRestComponent
import org.hl7.fhir.instance.model.Conformance.ConformanceRestOperationComponent
import org.hl7.fhir.instance.model.Conformance.ConformanceRestResourceComponent
import org.hl7.fhir.instance.model.Conformance.ConformanceRestResourceOperationComponent
import org.hl7.fhir.instance.model.Conformance.SystemRestfulOperation
import org.hl7.fhir.instance.model.Conformance.TypeRestfulOperation
import org.hl7.fhir.utilities.xhtml.NodeType
import org.hl7.fhir.utilities.xhtml.XhtmlNode

import com.google.common.collect.ImmutableMap

class ConformanceService {

  static XmlService xmlService
  static GrailsApplication grailsApplication
  static Conformance conformance
  static Map<String, String> searchParamXpaths
  static XmlParser parser = new XmlParser()
  static UrlService urlService


  public static ClassLoader getClassLoader(){
    Thread.currentThread().contextClassLoader
  }

  private Resource resourceFromFile(String file) {
    def stream = classLoader.getResourceAsStream(file)
    parser.parse(stream)
  }


  def generateConformance(){
    def xpathFixes = ImmutableMap.<String, String> builder()
    Map spotFixes = grailsApplication.config.fhir.searchParam.spotFixes
    spotFixes.each { uri, xpath ->
      xpathFixes.put(uri, xpath)
    }

    Profile patient = resourceFromFile "resources/patient.profile.xml"

    println("Read patient profile" + patient)
    conformance = resourceFromFile "resources/conformance-base.xml"

    conformance.text.div = new XhtmlNode(NodeType.Element, "div");
    conformance.text.div.addText("Generated Conformance Statement -- see structured representation.")
    conformance.identifierSimple = urlService.fhirBase + '/conformance'
    conformance.publisherSimple = "SMART on FHIR"
    conformance.nameSimple =  "SMART on FHIR Conformance Statement"
    conformance.descriptionSimple = "Describes capabilities of this SMART on FHIR server"
    conformance.telecom[0].valueSimple = urlService.fhirBase

    conformance.dateSimple = DateAndTime.now()

    List supportedOps = [
      TypeRestfulOperation.read,
      TypeRestfulOperation.vread,
      TypeRestfulOperation.update,
      TypeRestfulOperation.searchtype,
      TypeRestfulOperation.create,
      TypeRestfulOperation.historytype,
      TypeRestfulOperation.historyinstance,
      SystemRestfulOperation.transaction,
      SystemRestfulOperation.historysystem
    ]

    conformance.rest.each { ConformanceRestComponent r  ->
      r.operation = r.operation.findAll { ConformanceRestOperationComponent o ->
        o.codeSimple in supportedOps
      }
      r.resource.each { ConformanceRestResourceComponent rc ->

        String resourceName = rc.typeSimple
        String profile = "resources/${resourceName}.profile.xml".toLowerCase()
        Profile p = resourceFromFile(profile) 
        def paramDefs = p.structure.collect{it.searchParam}.flatten()

        rc.searchParam.each { searchParam ->
          String paramName = searchParam.nameSimple
          String key = resourceName + '.' + paramName
          String xpath = paramDefs.find{it.nameSimple == paramName}.xpathSimple

          // If we don't have a spot fix already loaded for this searchParam
          // then use the xapth we discovered in the default Profile (if any)
          if (xpath && !spotFixes[key]) xpathFixes.put(key, xpath)
        }

        rc.operation = rc.operation.findAll { ConformanceRestResourceOperationComponent o ->
          o.codeSimple in supportedOps
        }
      }
    }

    searchParamXpaths = xpathFixes.build()
  }
}
