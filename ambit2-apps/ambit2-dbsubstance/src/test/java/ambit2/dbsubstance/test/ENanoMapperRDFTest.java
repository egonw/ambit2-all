package ambit2.dbsubstance.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import ambit2.base.data.SubstanceRecord;
import ambit2.base.interfaces.IStructureRecord;
import ambit2.base.io.DownloadTool;
import ambit2.rest.substance.SubstanceRDFReporter;
import junit.framework.Assert;
import net.idea.loom.nm.nanowiki.ENanoMapperRDFReader;

/**
 * These are tests reading eNanoMapper RDF and writing to database
 * @author egonw
 *
 */
public class ENanoMapperRDFTest {

	public static File getRDFFile() throws Exception {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		File file = new File(baseDir, "datamodel.ttl");
		if (!file.exists()) {
			URL url = new URL("https://gist.githubusercontent.com/egonw/07e5fc9b1be7f9ece44834a169ddc155/raw/datamodel.ttl");
			DownloadTool.download(url, file);
		}
		return file;
	}

	@Test
	public void testRead() throws Exception {
		ENanoMapperRDFReader reader = null;
		try {
			reader = new ENanoMapperRDFReader(
				new InputStreamReader(new FileInputStream(getRDFFile())), "DEMO"
			);
			while (reader.hasNext()) {
				IStructureRecord record = reader.nextRecord();
				Assert.assertTrue(record instanceof SubstanceRecord);
				System.out.println("Substance: " + ((SubstanceRecord) record).toJSON(""));
			}
		} finally {
			if (reader != null) reader.close();
		}
	}

	@Test
	public void testRoundtrip() throws Exception {
		ENanoMapperRDFReader reader = null;
		Request hack = new Request();
		hack.setRootRef(new Reference("http://localhost/ambit2"));
		SubstanceRDFReporter r = new SubstanceRDFReporter(hack,
				MediaType.TEXT_RDF_N3);
		Model model = ModelFactory.createDefaultModel();
		r.header(model, null);
		r.setOutput(model);
		try {
			reader = new ENanoMapperRDFReader(
				new InputStreamReader(new FileInputStream(getRDFFile())), "DEMO"
			);
			while (reader.hasNext()) {
				IStructureRecord record = reader.nextRecord();
				Assert.assertTrue(record instanceof SubstanceRecord);
				r.processItem((SubstanceRecord) record);
			}
			r.footer(model, null);
			File output = new File(System.getProperty("java.io.tmpdir") + "/"
					+ "enmrdf_export.ttl");
			System.out.println("Exported to " + output.getAbsolutePath());
			OutputStream writer = new FileOutputStream(output);

			RDFDataMgr.write(writer, model, RDFFormat.TURTLE);
		} finally {
			if (reader != null) reader.close();
		}
	}

}
