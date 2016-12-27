package org.apache.ctakes.core.pipeline;


import org.apache.ctakes.core.cc.XmiWriterCasConsumerCtakes;
import org.apache.ctakes.core.cr.FileTreeReader;
import org.apache.ctakes.core.util.PropertyAeFactory;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates a pipeline using a small set of simple methods.
 * <p>
 * Some methods are order-specific and calls will directly impact ordering within the pipeline.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2016
 */
final public class PipelineBuilder {

   static private final Logger LOGGER = Logger.getLogger( "PipelineBuilder" );


   private final List<String> _aeNameList;
   private final List<AnalysisEngineDescription> _descList;
   private CollectionReader _reader;

   public PipelineBuilder() {
      _aeNameList = new ArrayList<>();
      _descList = new ArrayList<>();
   }

   /**
    * Use of this method is order-specific
    *
    * @param parameters add ae parameter name value pairs
    * @return this PipelineBuilder
    */
   public PipelineBuilder set( final Object... parameters ) {
      PropertyAeFactory.getInstance().addParameters( parameters );
      return this;
   }

   /**
    * Use of this method is not order-specific
    *
    * @param reader Collection Reader to place at the beginning of the pipeline
    * @return this PipelineBuilder
    */
   public PipelineBuilder reader( final CollectionReader reader ) {
      _reader = reader;
      return this;
   }

   /**
    * Adds a Collection reader to the beginning of the pipeline that will read files in a directory tree.
    * Relies upon {@link FileTreeReader#PARAM_INPUTDIR} having been specified
    * Use of this method is not order-specific.
    *
    * @return this PipelineBuilder
    * @throws UIMAException if the collection reader cannot be created
    */
   public PipelineBuilder readFiles() throws UIMAException {
      _reader = CollectionReaderFactory.createReader( FileTreeReader.class );
      return this;
   }

   /**
    * Adds a Collection reader to the beginning of the pipeline that will read files in a directory tree.
    * Use of this method is not order-specific
    *
    * @param inputDirectory directory with input files
    * @return this PipelineBuilder
    * @throws UIMAException if the collection reader cannot be created
    */
   public PipelineBuilder readFiles( final String inputDirectory ) throws UIMAException {
      _reader = FileTreeReader.createReader( inputDirectory );
      return this;
   }

   /**
    *
    * @return the CollectionReader for the pipeline or null if none has been specified
    */
   public CollectionReader getReader() {
      return _reader;
   }

   /**
    * Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the pipeline
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder add( final Class<? extends AnalysisComponent> component,
                               final Object... parameters ) throws ResourceInitializationException {
      _aeNameList.add( component.getName() );
      _descList.add( PropertyAeFactory.getInstance().createDescription( component, parameters ) );
      return this;
   }

   /**
    * Adds an ae or cc wrapped with "Starting processing" and "Finished processing" log messages
    * Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the pipeline
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder addLogged( final Class<? extends AnalysisComponent> component,
                                     final Object... parameters ) throws ResourceInitializationException {
      _aeNameList.add( component.getName() );
      _descList.add( PropertyAeFactory.getInstance().createLoggedDescription( component, parameters ) );
      return this;
   }

   /**
    * Use of this method is order-specific.
    *
    * @param description ae or cc component class description to add to the pipeline
    * @return this PipelineBuilder
    */
   public PipelineBuilder addDescription( final AnalysisEngineDescription description ) {
      _aeNameList.add( description.getAnnotatorImplementationName() );
      _descList.add( description );
      return this;
   }

   /**
    *
    * @return an ordered list of the annotation engines in the pipeline
    */
   public List<String> getAeNames() {
      return Collections.unmodifiableList( _aeNameList );
   }

   /**
    * Adds ae that maintains CUI information throughout the run.
    * CUI information can later be accessed using the {@link CuiCollector} singleton
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the CuiCollector engine cannot be created
    */
   public PipelineBuilder collectCuis() throws ResourceInitializationException {
      return add( CuiCollector.CuiCollectorEngine.class );
   }

   /**
    * Adds ae that maintains simple Entity information throughout the run.
    * Entity information can later be accessed using the {@link EntityCollector} singleton
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the EntityCollector engine cannot be created
    */
   public PipelineBuilder collectEntities() throws ResourceInitializationException {
      return add( EntityCollector.EntityCollectorEngine.class );
   }

   /**
    * Adds ae that writes an xmi file.
    * Relies upon {@link XmiWriterCasConsumerCtakes#PARAM_OUTPUTDIR} having been specified
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the Xmi writer engine cannot be created
    */
   public PipelineBuilder writeXMIs() throws ResourceInitializationException {
      return add( XmiWriterCasConsumerCtakes.class );
   }

   /**
    * Adds ae that writes an xmi file.
    * Use of this method is order-specific.
    *
    * @param outputDirectory directory in which xmi files should be written
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the Xmi writer engine cannot be created
    */
   public PipelineBuilder writeXMIs( final String outputDirectory ) throws ResourceInitializationException {
      return add( XmiWriterCasConsumerCtakes.class, XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR, outputDirectory );
   }

   /**
    * Run the pipeline using some specified collection reader.
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be run
    * @throws UIMAException if the pipeline could not be run
    */
   public PipelineBuilder run() throws IOException, UIMAException {
      if ( _reader == null ) {
         LOGGER.error( "No Collection Reader specified." );
         return this;
      }
      final AggregateBuilder builder = new AggregateBuilder();
      _descList.forEach( builder::add );
      final AnalysisEngineDescription desc = builder.createAggregateDescription();
      SimplePipeline.runPipeline( _reader, desc );
      return this;
   }

   /**
    * Run the pipeline on the given text.
    * Use of this method is order-specific.
    *
    * @param text text upon which to run this pipeline
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be run
    * @throws UIMAException if the pipeline could not be run
    */
   public PipelineBuilder run( final String text ) throws IOException, UIMAException {
      if ( _reader != null ) {
         LOGGER.error( "Collection Reader specified, ignoring." );
         return this;
      }
      final JCas jcas = JCasFactory.createJCas();
      jcas.setDocumentText( text );
      final AggregateBuilder builder = new AggregateBuilder();
      _descList.forEach( builder::add );
      final AnalysisEngineDescription desc = builder.createAggregateDescription();
      SimplePipeline.runPipeline( jcas, desc );
      return this;
   }


}
