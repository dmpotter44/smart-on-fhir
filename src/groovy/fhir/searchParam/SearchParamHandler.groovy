package fhir.searchParam

import groovy.util.logging.Log4j

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import org.hl7.fhir.instance.formats.XmlComposer
import org.hl7.fhir.instance.formats.XmlParser
import org.hl7.fhir.instance.model.Resource
import org.hl7.fhir.instance.model.Conformance.SearchParamType
import org.springframework.util.xml.SimpleNamespaceContext
import org.w3c.dom.Node
import org.xml.sax.InputSource

import com.mongodb.BasicDBObject
import com.mongodb.DBObject

// TODO implement generic logic for extracting References
public class ReferenceSearchParamHandler extends SearchParamHandler { }

// TODO extract Integer into its own class. Need clarification on
// how this is different from other numerical types (double, say).
public class IntegerSearchParamHandler extends StringSearchParamHandler { }


/**
 * @author jmandel
 *
 * Instances of this class generate database-ready index terms for a given
 * FHIR resource, based on the declared "searchParam" support in our
 * server-wide conformance profile.
 */
@Log4j
public abstract class SearchParamHandler {

	static XmlParser parser = new XmlParser()
	static XmlComposer composer = new XmlComposer()
	static SimpleNamespaceContext nsContext
	
	String fieldName;
	SearchParamType fieldType;
	String xpath;

	public static SearchParamHandler create(String fieldName, SearchParamType fieldType, String xpath) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		println("forfield $fieldType got $fieldName to $xpath")
		
		String ft = fieldType.toString().capitalize();
		String className = SearchParamHandler.class.canonicalName.replace(
			"SearchParamHandler", ft + "SearchParamHandler")
		
		SearchParamHandler.log.debug("$ft --> $className")
		Class c = Class.forName(className,
								true, 
								Thread.currentThread().contextClassLoader);
		
							
		SearchParamHandler ret =  c.newInstance(
				fieldName: fieldName,
				fieldType: fieldType,
				xpath: xpath
		);
		ret.init();
		return ret;
	}

	// TODO: dependency injection here.
	static void injectGrailsApplication(def grailsApplication){
		nsContext = new SimpleNamespaceContext();		
		grailsApplication.config.fhir.namespaces.each {
			prefix, uri -> nsContext.bindNamespaceUri(prefix, uri)
		}
	}

	protected void init(){}

	public String XmlForResource(Resource r) throws Exception {
		OutputStream o = new ByteArrayOutputStream();
		composer.compose(o, r, false);
		return o.toString();
	}

	public org.w3c.dom.Document fromResource(Resource r) throws IOException, Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		org.w3c.dom.Document d = builder.parse(new InputSource(new StringReader(XmlForResource(r))));
		return d;
	}

	protected void processXpathNodes(List<Node> nodes, List<SearchParamValue> index) throws Exception{

	}

	protected String paramXpath() {
		return "//" + xpath + "/@value";
	}

	protected void setMissing(boolean missing, List<SearchParamValue> index){
		index.add(value(
				":missing",
				missing ? true : false));
	}

	List<Node> selectNodes(String path, Node node) {
		
		def xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(nsContext);
		
		// collect to take NodeList --> List<Node>
		log.debug path+this.fieldName+this.fieldType+this.class
		
		
		xpath.evaluate(path, node, XPathConstants.NODESET).collect {
			it
		}
	}

	List<Node> query(String xpath, Node n){
		selectNodes(xpath, n)
	}

	public SearchParamValue value(String modifier, Object v){
		new SearchParamValue(
			paramName: fieldName + (modifier ?:""),
			paramType: fieldType,
			paramValue: v
		);
	}

	public SearchParamValue value(Object v){
		value(null,v);
	}

	public String queryString(String xpath, Node n){
		query(xpath, n).collect {
			it.nodeValue
		}.join " "
	}

	public List<SearchParamValue> execute(Resource r) throws Exception {
		List<SearchParamValue> index = []
		List<Node> nodes = query(paramXpath(), fromResource(r))
		processXpathNodes(nodes, index);
		return index;
	}
	
	String stripQuotes(def searchedFor){
		def val = searchedFor.value =~ /^"(.*)"$/
		if (!val.matches()){
			throw new RuntimeException("search strings must be in double quotes: " + searchedFor)
		}
		val[0][1]
	}
	
	static BasicDBObject match(def params){
		return [ searchTerms:[
			$elemMatch: [
				k: params.k,
				v: params.v
			]]]
	}
	
	static BasicDBObject and(List<DBObject> clauses){
		return [$and: clauses]
	}

	static BasicDBObject and(DBObject... clauses){
		return and(clauses)
	}
	
	static BasicDBObject or(DBObject... clauses){
		return or(clauses)
	}
	
	static BasicDBObject or(List<DBObject> clauses){
		return [$or: clauses]
	}

	BasicDBObject searchClause(def searchedFor){
		return [searchTerms:['$elemMatch': [k: fieldName,v: searchedFor.value]]]
	}
}