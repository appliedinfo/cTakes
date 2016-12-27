package org.apache.ctakes.core.cc;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.*;

/**
 * Write xmi files in a directory tree mimicking that of the input files
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/12/2016
 */
final public class FileTreeXmiWriter extends AbstractOutputFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "FileTreeXmiWriter" );

   @Override
   public void writeFile( final JCas jCas, final String outputDir,
                          final String documentId, final String fileName ) throws IOException {
      final File xmiFile = new File( outputDir, fileName + ".xmi" );
      try {
         writeXmi( jCas.getCas(), xmiFile );
      } catch ( IOException | SAXException multE ) {
         throw new IOException( multE );
      }
   }

   /**
    * Serialize a CAS to a file in XMI format
    *
    * @param cas  CAS to serialize
    * @param file output file
    * @throws IOException  -
    * @throws SAXException -
    */
   static private void writeXmi( final CAS cas, final File file ) throws IOException, SAXException {
      try ( OutputStream outputStream = new BufferedOutputStream( new FileOutputStream( file ) ) ) {
         XmiCasSerializer casSerializer = new XmiCasSerializer( cas.getTypeSystem() );
         XMISerializer xmiSerializer = new XMISerializer( outputStream );
         casSerializer.serialize( cas, xmiSerializer.getContentHandler() );
      }
   }

   public static AnalysisEngine createEngine( final String outputDirectory ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngine( FileTreeXmiWriter.class, PARAM_OUTPUTDIR, outputDirectory );
   }


}
