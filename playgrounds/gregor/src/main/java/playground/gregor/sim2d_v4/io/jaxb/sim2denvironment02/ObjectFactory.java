//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.18 at 02:24:41 PM CET 
//


package playground.gregor.sim2d_v4.io.jaxb.sim2denvironment02;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the playground.gregor.sim2d_v4.io.jaxb.sim2denvironment02 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Sim2DEnvironmentSection_QNAME = new QName("http://www.matsim.org/files/dtd", "sim2dEnvironmentSection");
    private final static QName _Sim2DEnvironment_QNAME = new QName("http://www.matsim.org/files/dtd", "sim2DEnvironment");
    private final static QName _XMLSectionPropertyTypeOpenings_QNAME = new QName("http://www.matsim.org/files/dtd", "openings");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: playground.gregor.sim2d_v4.io.jaxb.sim2denvironment02
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link XMLOpeningsType }
     * 
     */
    public XMLOpeningsType createXMLOpeningsType() {
        return new XMLOpeningsType();
    }

    /**
     * Create an instance of {@link XMLFeatureCollectionType }
     * 
     */
    public XMLFeatureCollectionType createXMLFeatureCollectionType() {
        return new XMLFeatureCollectionType();
    }

    /**
     * Create an instance of {@link XMLSectionPropertyType }
     * 
     */
    public XMLSectionPropertyType createXMLSectionPropertyType() {
        return new XMLSectionPropertyType();
    }

    /**
     * Create an instance of {@link XMLNeighborsType }
     * 
     */
    public XMLNeighborsType createXMLNeighborsType() {
        return new XMLNeighborsType();
    }

    /**
     * Create an instance of {@link XMLSim2DEnvironmentSectionType }
     * 
     */
    public XMLSim2DEnvironmentSectionType createXMLSim2DEnvironmentSectionType() {
        return new XMLSim2DEnvironmentSectionType();
    }

    /**
     * Create an instance of {@link XMLRelatedLinksRefIdsType }
     * 
     */
    public XMLRelatedLinksRefIdsType createXMLRelatedLinksRefIdsType() {
        return new XMLRelatedLinksRefIdsType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLSim2DEnvironmentSectionType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.matsim.org/files/dtd", name = "sim2dEnvironmentSection", substitutionHeadNamespace = "http://www.opengis.net/gml", substitutionHeadName = "_Feature")
    public JAXBElement<XMLSim2DEnvironmentSectionType> createSim2DEnvironmentSection(XMLSim2DEnvironmentSectionType value) {
        return new JAXBElement<XMLSim2DEnvironmentSectionType>(_Sim2DEnvironmentSection_QNAME, XMLSim2DEnvironmentSectionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLFeatureCollectionType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.matsim.org/files/dtd", name = "sim2DEnvironment", substitutionHeadNamespace = "http://www.opengis.net/gml", substitutionHeadName = "_FeatureCollection")
    public JAXBElement<XMLFeatureCollectionType> createSim2DEnvironment(XMLFeatureCollectionType value) {
        return new JAXBElement<XMLFeatureCollectionType>(_Sim2DEnvironment_QNAME, XMLFeatureCollectionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLOpeningsType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.matsim.org/files/dtd", name = "openings", scope = XMLSectionPropertyType.class)
    public JAXBElement<XMLOpeningsType> createXMLSectionPropertyTypeOpenings(XMLOpeningsType value) {
        return new JAXBElement<XMLOpeningsType>(_XMLSectionPropertyTypeOpenings_QNAME, XMLOpeningsType.class, XMLSectionPropertyType.class, value);
    }

}
