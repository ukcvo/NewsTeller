package edu.kit.anthropomatik.isl.newsTeller.util.propbank;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import weka.core.SerializationHelper;

/**
 * Contains all the Propbank frames and provides access to querying them.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class PropbankFrames {

	public static String SUFFIX_VERB = "-v";
	public static String SUFFIX_NOUN = "-n";
	public static String SUFFIX_ADJ  = "-j";
	
	private static Log log = LogFactory.getLog(PropbankFrames.class);
	
	private static PropbankFrames instance = null;
	
	private String folderName;
	private Map<String, Map<String, PropbankRoleset>> map;
	
	private PropbankFrames(String folderName, boolean forceConstruction) {
		parseAllPropBankFrames(folderName, forceConstruction);
		this.folderName = folderName;
	}
	
	public static PropbankFrames getInstance(String folderName, boolean forceConstruction) {
		if (instance == null || !instance.folderName.equals(folderName)) // only create new object if nonexisting or other folder name
			instance = new PropbankFrames(folderName, forceConstruction);
		return instance;
	}
	
	// region parsing
	private Map<String, PropbankRoleset> parsePropBankFrame(File file) {
		try {
			Map<String, PropbankRoleset> result = new HashMap<String, PropbankRoleset>();
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();

			NodeList rolesets = doc.getElementsByTagName("roleset");
			
			// iterate over "rolesets", i.e. meanings of the word
			for (int i = 0; i < rolesets.getLength(); i++) {
				
				Element roleset = (Element) rolesets.item(i);
				String rolesetName = roleset.getAttribute("id");
				
				PropbankRoleset r = new PropbankRoleset(rolesetName);
				
				// collect arguments from definition 
				Set<PropbankArgument> definitionArguments = new HashSet<PropbankArgument>();
				NodeList roles = roleset.getElementsByTagName("role");
				for (int j = 0; j < roles.getLength(); j++) {
					Element role = (Element) roles.item(j);
					String nString = role.getAttribute("n");
					String fString = role.getAttribute("f");
					definitionArguments.add(new PropbankArgument(nString, fString));
				}
				r.addArgumentSet(definitionArguments);
				
				// iterate over all examples
				NodeList examples = roleset.getElementsByTagName("example");
				for (int j = 0; j < examples.getLength(); j++) {
					Element example = (Element) examples.item(j);

					// look for arguments used in example
					Set<PropbankArgument> exampleArguments = new HashSet<PropbankArgument>();
					NodeList args = example.getElementsByTagName("arg");
					for (int k = 0; k < args.getLength(); k++) {
						Element arg = (Element) args.item(k);
						String nString = arg.getAttribute("n");
						String fString = arg.getAttribute("f");
						exampleArguments.add(new PropbankArgument(nString, fString));
					}
					r.addArgumentSet(exampleArguments);
				}
				
				result.put(rolesetName, r);
			}

			return result;
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("can't process XML file '%s'", file.getName()));
			if (log.isDebugEnabled())
				log.debug("XML error", e);

			return new HashMap<String, PropbankRoleset>();
		}

	}

	@SuppressWarnings("unchecked")
	private void parseAllPropBankFrames(String folderName, boolean forceConstruction) {

		// try to load if requested
		File mapFile = new File(folderName + ".map");
		if (!forceConstruction && mapFile.exists()) {
			try {
				this.map = (Map<String, Map<String, PropbankRoleset>>) SerializationHelper.read(mapFile.getAbsolutePath());
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn("wanted to load map file, but failed... constructing it manually");
				if (log.isDebugEnabled())
					log.debug("failure to read map", e);
			}
		}

		// construct map from scratch
		this.map = new HashMap<String, Map<String, PropbankRoleset>>();
		
		File folder = new File(folderName);

		for (File file : folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}	
			})) 
		{
			String fileName = file.getName();
			String word = fileName.substring(0, fileName.indexOf('.')); // "contradict-v.xml" --> "contradict-v"
			
			Map<String, PropbankRoleset> fileResult = parsePropBankFrame(file);

			this.map.put(word, fileResult);
		}

		// storing the map in a file
		try {
			SerializationHelper.write(folderName + ".map", this.map);
		} catch (Exception e) {
			if (log.isWarnEnabled())
				log.warn("unable to write map file... proceeding w/o storing it");
			if (log.isDebugEnabled())
				log.debug("failure to write map", e);
		}
	}
	// endregion
	
	/**
	 * Checks if the given word of the given wordType is contained in the propbank frames.
	 */
	public boolean containsFrame(String word, String suffix) {
		String fullName = word + suffix;
		return map.containsKey(fullName);
	}
	
	/**
	 * Checks if the given roleset is contained int he propbank frames.
	 */
	public boolean containsRoleset(String word, String suffix, String id) {
		String fullName = word + suffix;
		return containsFrame(word, suffix) && map.get(fullName).containsKey(id);
	}
	
	/**
	 * Retrieves the roleset specified by word, suffix and roleset id. Returns null if not existing.
	 */
	public PropbankRoleset getRoleset(String word, String suffix, String id) {
		PropbankRoleset result = null;
		String fullName = word + suffix;
		
		if (map.containsKey(fullName)) {
			Map<String, PropbankRoleset> frame = map.get(fullName);
			
			if (frame.containsKey(id))
				result = frame.get(id);
		}
		
		return result;
	}
}
