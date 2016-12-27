package org.apache.ctakes.core.pipeline;


import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/13/2016
 */
public class PiperFileRunner {

   static private final Logger LOGGER = Logger.getLogger( "PiperFileRunner" );

   interface RunOptions {
      @Option(
            shortName = "p",
            longName = "piper",
            description = "path to the piper file containing commands and parameters for pipeline configuration." )
      String getPiperPath();

      @Option(
            shortName = "i",
            longName = "inputDir",
            description = "path to the directory containing the clinical notes to be processed.",
            defaultValue = "" )
      String getInputDirectory();

      @Option(
            shortName = "o",
            longName = "outputDir",
            description = "path to the directory where the output files are to be written.",
            defaultValue = "" )
      String getOutputDirectory();

      @Option(
            longName = "xmiOut",
            description = "path to the directory where xmi files are to be written.  Adds XmiWriter to pipeline.",
            defaultValue = "" )
      String getXmiOutDirectory();

      @Option(
            longName = "user",
            description = "UMLS username.",
            defaultValue = "" )
      String getUmlsUserName();

      @Option(
            longName = "pass",
            description = "UMLS user password.",
            defaultValue = "" )
      String getUmlsPassword();

      @Option(
            shortName = { "?", "h" },
            longName = "help",
            description = "print usage",
            helpRequest = true )
      boolean isHelpWanted();

   }

   /**
    * @param args general run options
    */
   public static void main( final String... args ) {
      final RunOptions options = CliFactory.parseArguments( RunOptions.class, args );
      try {
         final PiperFileReader reader = new PiperFileReader();
         final PipelineBuilder builder = reader.getBuilder();
         // set the input directory parameter if needed
         final String inputDir = options.getInputDirectory();
         if ( !inputDir.isEmpty() ) {
            builder.set( "InputDirectory", inputDir );
         }
         // set the output directory parameter if needed
         final String outputDir = options.getOutputDirectory();
         final String xmiOutDir = options.getXmiOutDirectory();
         if ( !outputDir.isEmpty() ) {
            builder.set( "OutputDirectory", outputDir );
         } else if ( !xmiOutDir.isEmpty() ) {
            builder.set( "OutputDirectory", xmiOutDir );
         }
         // set the umls user and password parameters if needed
         final String umlsUser = options.getUmlsUserName();
         if ( !umlsUser.isEmpty() ) {
            builder.set( "umlsUser", umlsUser );
            builder.set( "ctakes.umlsuser", umlsUser );
         }
         final String umlsPass = options.getUmlsPassword();
         if ( !umlsPass.isEmpty() ) {
            builder.set( "umlsPass", umlsPass );
            builder.set( "ctakes.umlspw", umlsPass );
         }
         // load the piper file
         reader.loadPipelineFile( options.getPiperPath() );
         // if an input directory was specified but the piper didn't add a collection reader, add the default reader
         if ( !inputDir.isEmpty() && builder.getReader() == null ) {
            builder.readFiles( inputDir );
         }
         // if an xmi output directory was specified but the piper didn't add the xmi writer, add the
         if ( !xmiOutDir.isEmpty() ) {
            if ( !builder.getAeNames().stream().map( String::toLowerCase )
                  .anyMatch( n -> n.contains( "xmiwriter" ) ) ) {
               builder.writeXMIs( xmiOutDir );
            }
         }
         // run the pipeline
         builder.run();
      } catch ( UIMAException | IOException multE ) {
         LOGGER.error( multE.getMessage() );
         System.exit( 1 );
      }
   }


}
