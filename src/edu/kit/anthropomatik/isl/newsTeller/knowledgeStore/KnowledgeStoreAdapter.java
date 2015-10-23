package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.query.BindingSet;

import eu.fbk.knowledgestore.KnowledgeStore;
import eu.fbk.knowledgestore.OperationException;
import eu.fbk.knowledgestore.Session;
import eu.fbk.knowledgestore.client.Client;
import eu.fbk.knowledgestore.data.Stream;

/**
 * Adapter class to facilitate KnowledgeStore access.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KnowledgeStoreAdapter {
	
	private static Log log = LogFactory.getLog(KnowledgeStoreAdapter.class);
	
	public static void main(String[] args) {
		String serverURL = "http://knowledgestore2.fbk.eu/nwr/wikinews";
		int timeoutSec = 10;
		String query = "SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10";
		
		KnowledgeStore ks = Client.builder(serverURL).compressionEnabled(true).maxConnections(2).validateServer(false)
				.connectionTimeout(timeoutSec*1000).build();
		if(log.isTraceEnabled())
			log.trace("created KS instance");
		Session session = ks.newSession();
		
		try {
			Stream<BindingSet> stream = session.sparql(query).timeout((long) (timeoutSec*1000)).execTuples();
			
			List<BindingSet> tuples = stream.toList();
			for (BindingSet tuple : tuples) {
				System.out.println(tuple.toString());
			}

			System.out.println(tuples.size());
			
			@SuppressWarnings("unchecked")
			List<String> variables = stream.getProperty("variables", List.class);
			System.out.println(variables);
			stream.close();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		session.close();
		ks.close();
	}
}
