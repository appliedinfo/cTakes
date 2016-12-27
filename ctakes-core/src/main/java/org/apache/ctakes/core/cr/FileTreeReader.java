package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.DocumentIdPrefix;
import org.apache.ctakes.typesystem.type.structured.DocumentPath;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Recursively reads a directory tree of files, sorted by level (root first),
 * creating the DocumentID from the file name and the DocumentIdPrefix by the subdirectory path between
 * the root and the leaf file
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/10/2016
 */
final public class FileTreeReader extends CollectionReader_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "FileTreeReader" );

   /**
    * Name of configuration parameter that must be set to the path of
    * a directory containing input files.
    */
   public static final String PARAM_INPUTDIR = "InputDirectory";

   /**
    * Name of configuration parameter that contains the character encoding used
    * by the input files.  If not specified, the default system encoding will
    * be used.
    */
   public static final String PARAM_ENCODING = "Encoding";

   /**
    * Name of optional configuration parameter that specifies the extensions
    * of the files that the collection reader will read.  Values for this
    * parameter should not begin with a dot <code>'.'</code>.
    */
   public static final String PARAM_EXTENSIONS = "Extensions";

   private List<File> _files;
   private String _encoding;
   private Collection<String> _validExtensions;
   private File _rootDir;
   private int _currentIndex;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize() throws ResourceInitializationException {
      try {
         _rootDir = FileLocator.locateFile( (String)getConfigParameterValue( PARAM_INPUTDIR ) );
      } catch ( FileNotFoundException fnfE ) {
         throw new ResourceInitializationException( fnfE );
      }
      _encoding = (String)getConfigParameterValue( PARAM_ENCODING );
      final String[] explicitExtensions = (String[])getConfigParameterValue( PARAM_EXTENSIONS );
      _validExtensions = createValidExtensions( explicitExtensions );

      _currentIndex = 0;
      _files = getDescendentFiles( _rootDir, _validExtensions );
   }

   /**
    * @param explicitExtensions array of file extensions as specified in the uima parameters
    * @return a collection of dot-prefixed extensions or none if {@code explicitExtensions} is null or empty
    */
   static Collection<String> createValidExtensions( final String... explicitExtensions ) {
      if ( explicitExtensions == null || explicitExtensions.length == 0 ) {
         return Collections.emptyList();
      }
      if ( explicitExtensions.length == 1
           && (explicitExtensions[ 0 ].equals( "*" ) || explicitExtensions[ 0 ].equals( ".*" )) ) {
         return Collections.emptyList();
      }
      final Collection<String> validExtensions = new ArrayList<>( explicitExtensions.length );
      for ( String extension : explicitExtensions ) {
         if ( extension.startsWith( "." ) ) {
            validExtensions.add( extension );
         } else {
            validExtensions.add( '.' + extension );
         }
      }
      return validExtensions;
   }

   /**
    * @param parentDir       -
    * @param validExtensions collection of valid extensions or empty collection if all extensions are valid
    * @return List of files descending from the parent directory
    */
   static private List<File> getDescendentFiles( final File parentDir, final Collection<String> validExtensions ) {
      final File[] children = parentDir.listFiles();
      if ( children == null || children.length == 0 ) {
         return Collections.emptyList();
      }
      final Collection<File> childDirs = new ArrayList<>();
      final List<File> descendentFiles = new ArrayList<>();
      for ( File child : children ) {
         if ( child.isDirectory() ) {
            childDirs.add( child );
            continue;
         }
         if ( isExtensionValid( child, validExtensions ) && !child.isHidden() ) {
            descendentFiles.add( child );
         }
      }
      for ( File childDir : childDirs ) {
         descendentFiles.addAll( getDescendentFiles( childDir, validExtensions ) );
      }
      return descendentFiles;
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return true if validExtensions is empty or contains an extension belonging to the given file
    */
   static boolean isExtensionValid( final File file, final Collection<String> validExtensions ) {
      if ( validExtensions.isEmpty() ) {
         return true;
      }
      final String fileName = file.getName();
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) ) {
            if ( fileName.equals( extension ) ) {
               LOGGER.warn( "File " + file.getPath() + " is named as extension " + extension + " ; discarded" );
               return false;
            }
            return true;
         }
      }
      return false;
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return the file name with the longest valid extension removed
    */
   static String createDocumentID( final File file, final Collection<String> validExtensions ) {
      final String fileName = file.getName();
      String maxExtension = "";
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) && extension.length() > maxExtension.length() ) {
            maxExtension = extension;
         }
      }
      int lastDot = fileName.lastIndexOf( '.' );
      if ( !maxExtension.isEmpty() ) {
         lastDot = fileName.length() - maxExtension.length();
      }
      if ( lastDot < 0 ) {
         return fileName;
      }
      return fileName.substring( 0, lastDot );
   }

   /**
    * @param file    -
    * @param rootDir -
    * @return the subdirectory path between the root directory and the file
    */
   static private String createDocumentIdPrefix( final File file, final File rootDir ) {
      final String parentPath = file.getParent();
      final String rootPath = rootDir.getPath();
      if ( parentPath.equals( rootPath ) || !parentPath.startsWith( rootPath ) ) {
         return "";
      }
      return parentPath.substring( rootPath.length() + 1 );
   }

   /**
    * Gets the total number of documents that will be returned by this
    * collection reader.  This is not part of the general collection reader
    * interface.
    *
    * @return the number of documents in the collection
    */
   public int getNumberOfDocuments() {
      return _files.size();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasNext() {
      return _currentIndex < _files.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getNext( final CAS cas ) throws IOException, CollectionException {
      JCas jcas;
      try {
         jcas = cas.getJCas();
      } catch ( CASException casE ) {
         _currentIndex++;
         throw new IOException( casE );
      }
      final File file = _files.get( _currentIndex );
      _currentIndex++;
      // Use 8KB as the default buffer size
      byte[] buffer = new byte[ 8192 ];
      final StringBuilder sb = new StringBuilder();
      try ( final InputStream inputStream = new BufferedInputStream( new FileInputStream( file ), buffer.length ) ) {
         while ( true ) {
            final int length = inputStream.read( buffer );
            if ( length < 0 ) {
               break;
            }
            if ( _encoding != null ) {
               sb.append( new String( buffer, 0, length, _encoding ) );
            } else {
               sb.append( new String( buffer, 0, length ) );
            }
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      // put document text and id annotations in CAS (assume CAS)
      jcas.setDocumentText( sb.toString() );
      final DocumentID documentId = new DocumentID( jcas );
      final String id = createDocumentID( file, _validExtensions );
      documentId.setDocumentID( id );
      documentId.addToIndexes();
      final DocumentIdPrefix documentIdPrefix = new DocumentIdPrefix( jcas );
      final String idPrefix = createDocumentIdPrefix( file, _rootDir );
      documentIdPrefix.setDocumentIdPrefix( idPrefix );
      documentIdPrefix.addToIndexes();
      final DocumentPath documentPath = new DocumentPath( jcas );
      documentPath.setDocumentPath( file.getAbsolutePath() );
      documentPath.addToIndexes();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void close() throws IOException {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      return new Progress[] {
            new ProgressImpl( _currentIndex, _files.size(), Progress.ENTITIES )
      };
   }


   public static CollectionReader createReader( final String inputDirectory ) throws ResourceInitializationException {
      return CollectionReaderFactory.createReader( FileTreeReader.class,
            FilesInDirectoryCollectionReader.PARAM_INPUTDIR,
            inputDirectory );
   }

}
