package com.atmire.import_citations;

import com.atmire.import_citations.configuration.Query;
import com.atmire.import_citations.configuration.SourceException;
import com.atmire.import_citations.configuration.metadatamapping.MetadataContributor;
import com.atmire.import_citations.configuration.metadatamapping.MetadataFieldMapping;
import com.atmire.import_citations.datamodel.Record;
import com.atmire.wadl.IndexScopusResource;
import com.atmire.wadl.ScopusidResource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.jaxen.JaxenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * @author lotte.hofstede at atmire.com
 */
public class ScopusSource extends AbstractImportSource<OMElement> {
    private static Logger log = Logger.getLogger(ScopusSource.class);

    protected String baseAddress;
    protected String view;

    private GenerateQueryForItem_Scopus generateQueryForItem = null;

    @Autowired(required = false)
    public void setView(String view) {
        this.view = view;
    }

    @Autowired(required = false)
    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    @Autowired
    public void setGenerateQueryForItem(GenerateQueryForItem_Scopus generateQueryForItem) {
        this.generateQueryForItem = generateQueryForItem;
    }

    @Override
    public int getNbRecords(String query) throws SourceException {
        return retry(new GetNbRecords(query));
    }

    @Override
    public int getNbRecords(Query query) throws SourceException {
        return retry(new GetNbRecords(query));
    }

    @Override
    public Collection<Record> getRecords(String query, int start, int count) throws SourceException {
        return retry(new GetRecords(query, start, count));
    }

    @Override
    public Collection<Record> getRecords(Query query) throws SourceException {
        return retry(new GetRecords(query));
    }

    @Override
    public Record getRecord(String id) throws SourceException {
        return retry(new GetRecord(id));
    }

    @Override
    public Record getRecord(Query query) throws SourceException {
        return retry(new GetRecord(query));
    }

    @Override
    public String getImportSource() {
        return baseAddress;
    }

    @Override
    public Collection<Record> findMatchingRecords(Item item) throws SourceException {
        return null;
    }

    @Override
    public Collection<Record> findMatchingRecords(Query q) throws SourceException {
        return null;
    }

    @Override
    public void init() throws Exception {

    }


    private class GetNbRecords extends AbstractScopusCallable<Integer> {

        private GetNbRecords(String queryString) {
            query = new Query();
            query.addParameter("query", queryString);
        }

        private Query query;

        public GetNbRecords(Query query) {
            this.query = query;
        }

        @Override
        public Integer call() throws Exception {
            Response simple = getSearchResponse(query.getParameterAsClass("query", String.class), null, 0, 0);
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new SourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }

            String responseString = simple.readEntity(String.class);

            String count = getSingleElementValue(responseString, "opensearch:totalResults");

            try {
                return Integer.parseInt(count);
            } catch (NumberFormatException e) {
                log.error(e.getMessage(), e);
                return 0;
            }
        }
    }

    private class GetRecords extends AbstractScopusCallable<Collection<Record>> {

        private Query query;

        private GetRecords(String queryString, int start, int count) {
            query = new Query();
            query.addParameter("query", queryString);
            query.addParameter("start", start);
            query.addParameter("count", count);
        }

        private GetRecords(Query q) {
            this.query = q;
        }

        @Override
        public Collection<Record> call() throws Exception {
            List<Record> records = new ArrayList<Record>();

            Response simple = getSearchResponse(query.getParameterAsClass("query", String.class), null,
                    query.getParameterAsClass("start", Integer.class),
                    query.getParameterAsClass("count", Integer.class));
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new SourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }

            List<OMElement> omElements = splitToRecords(simple.readEntity(String.class));

            for (OMElement record : omElements) {
                records.add(new Record(new LinkedList<>(getMetadataFieldMapping().resultToDCValueMapping(record))));
            }

            return records;
        }
    }

    private class GetRecord extends AbstractScopusCallable<Record> {

        private Query query;

        private GetRecord(String queryString) {
            query = new Query();
            query.addParameter("id", queryString);
        }

        public GetRecord(Query q) {
            query = q;
        }


        @Override
        public Record call() throws Exception {
            Record record = null;
            Response simple = getSearchResponse(query.getParameterAsClass("id", String.class), null);
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new SourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }

            OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(simple.readEntity(String.class)));
            OMElement element = (OMElement) records.getDocumentElement().getChildrenWithLocalName("entry").next();
            if (element != null) {
                element.declareNamespace("http://www.w3.org/2005/Atom", "a");
                element.declareNamespace("http://purl.org/dc/elements/1.1/", "dc");
                record = new Record(new LinkedList<>(getMetadataFieldMapping().resultToDCValueMapping(element)));
            }

            return record;
        }
    }

    private class FindMatchingRecords extends AbstractScopusCallable<Collection<Record>> {

        private Item item;

        private FindMatchingRecords(Item item) {
            this.item = item;
        }

        public FindMatchingRecords(Query q) {
            item = q.getParameterAsClass("item", Item.class);
        }

        @Override
        public Collection<Record> call() throws Exception {

            String query = generateQueryForItem.generateQueryForItem(item);
            List<Record> records = performCall(query);

            if (records.size() == 0) {
                String fallbackQuery = generateQueryForItem.generateFallbackQueryForItem(item);
                records = performCall(fallbackQuery);
            }

            return records;
        }

        public List<Record> performCall(String query) throws Exception {
            List<Record> records = new LinkedList<Record>();
            if (query == null) {
                return records;
            }
            Response simple = getSearchResponse(query, "identifier");
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new SourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }
            OMElement element = getDocumentElement(simple);

            AXIOMXPath xpath = new AXIOMXPath("/a:search-results/a:entry/dc:identifier");
            xpath.addNamespace("a", "http://www.w3.org/2005/Atom");
            xpath.addNamespace("dc", "http://purl.org/dc/elements/1.1/");
            List<OMElement> recordsList = xpath.selectNodes(element);

            for (OMElement omElement : recordsList) {
                String text = omElement.getText();
                String scopus_id = "SCOPUS_ID:";
                if (text.startsWith(scopus_id)) {
                    String id = text.substring(scopus_id.length());
                    Metadatum dcValue = new Metadatum();
                    dcValue.schema = "scopus";
                    dcValue.element = "id";
                    dcValue.value = id;
                    Record record = new Record(Collections.singletonList(dcValue));
                    records.add(record);
                }
            }

            if (records.size() == 0) {
                String fallbackQuery = generateQueryForItem.generateFallbackQueryForItem(item);
            }

            return records;
        }
    }

    private abstract class AbstractScopusCallable<T> implements Callable<T> {


        protected Response getSearchResponse(String query, String fields) {
            IndexScopusResource scopusResource = JAXRSClientFactory.create(baseAddress, IndexScopusResource.class);
            //      http://api.elsevier.com/content/search/index:SCOPUS?query=DOI(10.1007/s10439-010-0201-5)&field=citedby-count&apiKey=7f8c024a802ae228658bb08c974dbefb
            return scopusResource.simple("application/xml", null, null, null, null, null, null, getApiKey(), null, null, query, view, fields,
                    null, null, null, null, null, null, null, null);
        }

        protected Response getSearchResponse(String query, String fields, int start, int count) {
            IndexScopusResource scopusResource = JAXRSClientFactory.create(baseAddress, IndexScopusResource.class);
            //      http://api.elsevier.com/content/search/index:SCOPUS?query=DOI(10.1007/s10439-010-0201-5)&field=citedby-count&apiKey=7f8c024a802ae228658bb08c974dbefb
            return scopusResource.simple("application/xml", null, null, null, null, null, null, getApiKey(), null, null, query, view, fields,
                    null, Integer.toString(start), Integer.toString(count), null, null, null, null, null);
        }

        protected OMElement getDocumentElement(Response simple) {
            InputStream inputStream = simple.readEntity(InputStream.class);
            OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(inputStream);
            return records.getDocumentElement();
        }
    }

}